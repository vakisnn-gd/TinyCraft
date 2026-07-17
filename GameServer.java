import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

final class GameServer implements MultiplayerManager.Listener, MultiplayerManager.ServerCommandDelegate {
    private static final double AUTOSAVE_SECONDS = 30.0;

    private final ServerProperties properties;
    private final ServerAccessList accessList = new ServerAccessList();
    private final PlayerState simulationPlayer = new PlayerState();

    private VoxelWorld world;
    private MultiplayerManager multiplayer;
    private Thread tickThread;
    private volatile boolean running;
    private volatile boolean stopped = true;
    private long startMillis;
    private double autosaveTimer;

    GameServer(ServerProperties properties) {
        this.properties = properties;
    }

    boolean start() {
        try {
            accessList.loadOrCreate();
            Files.createDirectories(RuntimePaths.resolve("backups"));
            ResolvedWorld resolved = resolveWorld();
            world = new VoxelWorld(resolved.seed);
            world.setRenderDistanceChunks(properties.viewDistance);
            world.configureWorld(resolved.directory, resolved.seed, resolved.terrainPreset);
            world.initializeNoise();
            world.generateWorld();
            world.placePlayerAtSpawn(simulationPlayer);
            world.prepareForPlayer(simulationPlayer);
            world.primeStreamingAround(simulationPlayer);

            multiplayer = new MultiplayerManager(null, this);
            multiplayer.setServerCommandDelegate(this);
            if (!multiplayer.startDedicatedHost(world, properties.port, properties.maxPlayers, properties.allowPvp)) {
                return false;
            }
            running = true;
            stopped = false;
            startMillis = System.currentTimeMillis();
            tickThread = new Thread(this::runTickLoop, "TinyCraft dedicated tick");
            tickThread.setDaemon(false);
            tickThread.start();
            System.out.println("TinyCraft v0.2.1 dedicated server");
            System.out.println("World: " + resolved.directory);
            System.out.println("Seed: " + Long.toUnsignedString(resolved.seed, 16));
            System.out.println("MOTD: " + properties.motd);
            System.out.println("Type 'help' for commands.");
            return true;
        } catch (IOException exception) {
            System.out.println("Server start failed: " + exception.getMessage());
            return false;
        }
    }

    boolean isRunning() {
        return running;
    }

    void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        running = false;
        if (tickThread != null && Thread.currentThread() != tickThread) {
            try {
                tickThread.join(2000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            tickThread = null;
        }
        boolean saved = false;
        if (multiplayer != null) {
            multiplayer.broadcastServerMessage("Server stopping.");
            save();
            saved = true;
            multiplayer.stop();
            multiplayer = null;
        }
        if (!saved) {
            save();
        }
        if (world != null) {
            world.cleanup();
            world = null;
        }
        System.out.println("Server stopped.");
    }

    void handleConsoleCommand(String raw) {
        MultiplayerManager currentMultiplayer = multiplayer;
        if (currentMultiplayer != null && running && Thread.currentThread() != tickThread) {
            currentMultiplayer.queueServerAction(() -> handleConsoleCommand(raw));
            return;
        }
        String result = executeServerCommand(null, "Console", raw, true);
        if (result == null) {
            System.out.println("Unknown command. Type 'help'.");
        } else if (!result.trim().isEmpty()) {
            System.out.println(result);
        }
    }

    @Override
    public String joinRejectionReason(UUID uuid, String name) {
        if (accessList.isBanned(uuid, name)) {
            return "You are banned from this server.";
        }
        if (properties.whitelist && !accessList.isWhitelisted(uuid, name) && !accessList.isOperator(uuid, name)) {
            return "You are not whitelisted on this server.";
        }
        return null;
    }

    @Override
    public String executeServerCommand(UUID senderUuid, String senderName, String commandLine) {
        return executeServerCommand(senderUuid, senderName, commandLine, false);
    }

    private String executeServerCommand(UUID senderUuid, String senderName, String commandLine, boolean console) {
        String line = commandLine == null ? "" : commandLine.trim();
        if (line.startsWith("/")) {
            line = line.substring(1).trim();
        }
        if (line.isEmpty()) {
            return "";
        }
        String[] parts = line.split("\\s+", 4);
        String command = parts[0].toLowerCase(Locale.ROOT);
        if ("gm".equals(command)) {
            line = "gamemode" + (line.length() > 2 ? line.substring(2) : "");
            parts = line.split("\\s+", 4);
            command = "gamemode";
        } else if ("gms".equals(command) || "gmc".equals(command) || "gmsp".equals(command)) {
            String mode = "gmc".equals(command) ? "creative" : ("gmsp".equals(command) ? "spectator" : "survival");
            String target = parts.length >= 2 ? " " + parts[1] : "";
            line = "gamemode " + mode + target;
            parts = line.split("\\s+", 4);
            command = "gamemode";
        }
        if (!isDedicatedServerCommand(command)) {
            return null;
        }
        if (senderUuid != null && !accessList.isOperator(senderUuid, senderName)
            && !isPublicPlayerCommand(command)
            && (!properties.allowCheats || !isPlayerCheatCommand(command))) {
            return "You do not have permission to use /" + command + ".";
        }
        try {
            if ("help".equals(command)) {
                return "Commands: help, status, list, ping, msg <player> <message>, say <message>, kick <player> [reason], save, save-all, stop, op, deop, whitelist, ban, pardon, tp, kill, summon, setblock, fill, gamemode, give, clear";
            }
            if ("status".equals(command)) {
                long uptimeSeconds = Math.max(0L, (System.currentTimeMillis() - startMillis) / 1000L);
                return "Status: running, uptime=" + uptimeSeconds + "s, players="
                    + multiplayer.connectedPlayerCount() + "/" + properties.maxPlayers
                    + ", whitelist=" + properties.whitelist;
            }
            if ("list".equals(command)) {
                return "Players: " + multiplayer.connectedPlayerNames();
            }
            if ("say".equals(command)) {
                if (parts.length < 2 || line.length() <= 4) {
                    return "Usage: say <message>";
                }
                String message = line.substring(4).trim();
                multiplayer.broadcastServerMessage(message);
                System.out.println("[Server] " + message);
                return console ? "" : "[Server] " + message;
            }
            if ("kick".equals(command)) {
                if (parts.length < 2) {
                    return "Usage: kick <player> [reason]";
                }
                String[] kickParts = line.split("\\s+", 3);
                String reason = kickParts.length >= 3 ? kickParts[2] : "Kicked by server.";
                multiplayer.kickByName(kickParts[1], reason);
                return "Kicked " + kickParts[1] + ".";
            }
            if ("save".equals(command) || "save-all".equals(command)) {
                save();
                return console ? "" : "Saved world.";
            }
            if ("stop".equals(command)) {
                if (!console) {
                    multiplayer.broadcastServerMessage("Server stop requested by " + safeSenderName(senderName) + ".");
                }
                stop();
                return console ? "" : "Stopping server.";
            }
            if ("op".equals(command)) {
                if (parts.length < 2) {
                    return "Usage: op <player|uuid>";
                }
                ResolvedPlayer target = resolvePlayer(parts[1]);
                accessList.addOperator(parts[1], target.uuid, target.name);
                return "Opped " + target.display(parts[1]) + ".";
            }
            if ("deop".equals(command)) {
                if (parts.length < 2) {
                    return "Usage: deop <player|uuid>";
                }
                ResolvedPlayer target = resolvePlayer(parts[1]);
                accessList.removeOperator(parts[1], target.uuid, target.name);
                return "Deopped " + target.display(parts[1]) + ".";
            }
            if ("ban".equals(command)) {
                if (parts.length < 2) {
                    return "Usage: ban <player|uuid> [reason]";
                }
                ResolvedPlayer target = resolvePlayer(parts[1]);
                accessList.addBan(parts[1], target.uuid, target.name);
                String reason = line.split("\\s+", 3).length >= 3 ? line.split("\\s+", 3)[2] : "Banned by server.";
                multiplayer.kickByName(parts[1], reason);
                return "Banned " + target.display(parts[1]) + ".";
            }
            if ("pardon".equals(command)) {
                if (parts.length < 2) {
                    return "Usage: pardon <player|uuid>";
                }
                ResolvedPlayer target = resolvePlayer(parts[1]);
                accessList.removeBan(parts[1], target.uuid, target.name);
                return "Pardoned " + target.display(parts[1]) + ".";
            }
            if ("whitelist".equals(command)) {
                return handleWhitelistCommand(parts);
            }
            if ("tp".equals(command)) {
                String[] teleportParts = line.split("\\s+");
                boolean selfTarget = senderUuid != null && teleportParts.length == 4;
                boolean explicitTarget = teleportParts.length == 5;
                if (!selfTarget && !explicitTarget) {
                    return senderUuid == null ? "Usage: tp <player> <x> <y> <z>" : "Usage: /tp <x> <y> <z>";
                }
                String targetName = selfTarget ? senderName : teleportParts[1];
                if (!selfTarget && senderUuid != null && !accessList.isOperator(senderUuid, senderName)) {
                    return "You do not have permission to teleport another player.";
                }
                int xIndex = selfTarget ? 1 : 2;
                double x = Double.parseDouble(teleportParts[xIndex]);
                double y = Double.parseDouble(teleportParts[xIndex + 1]);
                double z = Double.parseDouble(teleportParts[xIndex + 2]);
                return multiplayer.teleportPlayerByName(targetName, x, y, z)
                    ? "Teleported " + targetName + "."
                    : "Player not found: " + targetName;
            }
            if ("kill".equals(command)) {
                boolean selfTarget = senderUuid != null && parts.length == 1;
                if (!selfTarget && parts.length != 2) {
                    return senderUuid == null ? "Usage: kill <player>" : "Usage: /kill [player]";
                }
                String targetName = selfTarget ? senderName : parts[1];
                if (senderUuid != null && !targetName.equalsIgnoreCase(senderName)
                    && !accessList.isOperator(senderUuid, senderName)) {
                    return "You do not have permission to kill another player.";
                }
                return multiplayer.killPlayerByName(targetName)
                    ? "Killed " + targetName + "."
                    : "Player not found: " + targetName;
            }
            if ("summon".equals(command)) {
                if (parts.length < 2 || parts.length > 3) {
                    return senderUuid == null ? "Usage: summon <entity> <player>" : "Usage: /summon <entity>";
                }
                MobKind kind = ChatSystem.resolveMobKind(parts[1]);
                if (kind == null) {
                    return "Unknown entity: " + parts[1];
                }
                boolean selfTarget = senderUuid != null && parts.length == 2;
                if (!selfTarget && parts.length != 3) {
                    return "Usage: summon <entity> <player>";
                }
                String targetName = selfTarget ? senderName : parts[2];
                if (senderUuid != null && !targetName.equalsIgnoreCase(senderName)
                    && !accessList.isOperator(senderUuid, senderName)) {
                    return "You do not have permission to summon at another player.";
                }
                return multiplayer.summonMobAtPlayerByName(targetName, kind)
                    ? "Summoned " + parts[1] + "."
                    : "Player not found: " + targetName;
            }
            if ("setblock".equals(command)) {
                String[] blockParts = line.split("\\s+");
                if (blockParts.length != 5) {
                    return console
                        ? "Usage: setblock <x> <y> <z> <block>"
                        : "Usage: /setblock <x> <y> <z> <block>";
                }
                BlockState state = ChatSystem.resolveCommandBlockState(blockParts[4]);
                if (state == null) {
                    return "Unknown block: " + blockParts[4];
                }
                int changed = multiplayer.setBlockByCommand(
                    senderUuid == null ? null : senderName,
                    Integer.parseInt(blockParts[1]),
                    Integer.parseInt(blockParts[2]),
                    Integer.parseInt(blockParts[3]),
                    state
                );
                return commandBlockFeedback(changed, false);
            }
            if ("fill".equals(command)) {
                String[] fillParts = line.split("\\s+");
                if (fillParts.length != 8) {
                    return console
                        ? "Usage: fill <x1> <y1> <z1> <x2> <y2> <z2> <block>"
                        : "Usage: /fill <x1> <y1> <z1> <x2> <y2> <z2> <block>";
                }
                BlockState state = ChatSystem.resolveCommandBlockState(fillParts[7]);
                if (state == null) {
                    return "Unknown block: " + fillParts[7];
                }
                int x1 = Integer.parseInt(fillParts[1]);
                int y1 = Integer.parseInt(fillParts[2]);
                int z1 = Integer.parseInt(fillParts[3]);
                int x2 = Integer.parseInt(fillParts[4]);
                int y2 = Integer.parseInt(fillParts[5]);
                int z2 = Integer.parseInt(fillParts[6]);
                int minX = Math.min(x1, x2);
                int minY = Math.min(y1, y2);
                int minZ = Math.min(z1, z2);
                int maxX = Math.max(x1, x2);
                int maxY = Math.max(y1, y2);
                int maxZ = Math.max(z1, z2);
                if (ChatSystem.fillBlockCount(minX, minY, minZ, maxX, maxY, maxZ) > ChatSystem.MAX_FILL_BLOCKS) {
                    return "Too many blocks. Maximum: " + ChatSystem.MAX_FILL_BLOCKS + ".";
                }
                int changed = multiplayer.fillBlocksByCommand(
                    senderUuid == null ? null : senderName,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    state
                );
                return commandBlockFeedback(changed, true);
            }
            if ("gamemode".equals(command)) {
                boolean selfTarget = senderUuid != null && parts.length == 2;
                if (parts.length < 3 && !selfTarget) {
                    return senderUuid == null
                        ? "Usage: gamemode <survival|creative|spectator> <player>"
                        : "Usage: /gamemode <survival|creative|spectator>";
                }
                String mode = parts[1].toLowerCase(Locale.ROOT);
                if (!"survival".equals(mode) && !"creative".equals(mode) && !"spectator".equals(mode)
                    && !"0".equals(mode) && !"1".equals(mode) && !"3".equals(mode)) {
                    return "Unknown gamemode: " + parts[1];
                }
                String targetName = selfTarget ? senderName : parts[2];
                return multiplayer.setPlayerGameModeByName(targetName, mode)
                    ? "Set " + targetName + " to " + mode + "."
                    : "Player not found: " + targetName;
            }
            if ("clear".equals(command)) {
                boolean selfTarget = senderUuid != null && parts.length == 1;
                if (parts.length < 2 && !selfTarget) {
                    return senderUuid == null ? "Usage: clear <player>" : "Usage: /clear";
                }
                String targetName = selfTarget ? senderName : parts[1];
                return multiplayer.clearPlayerInventoryByName(targetName)
                    ? "Cleared " + targetName + "."
                    : "Player not found: " + targetName;
            }
            if ("give".equals(command)) {
                boolean selfTarget = senderUuid != null && parts.length == 3;
                if (parts.length < 4 && !selfTarget) {
                    return senderUuid == null
                        ? "Usage: give <player> <id|tinycraft:name> <amount>"
                        : "Usage: /give <id|tinycraft:name> <amount>";
                }
                String targetName = selfTarget ? senderName : parts[1];
                String itemToken = selfTarget ? parts[1] : parts[2];
                String amountToken = selfTarget ? parts[2] : parts[3];
                Byte itemId = resolveGiveItem(itemToken);
                int amount = Integer.parseInt(amountToken);
                if (itemId == null || amount <= 0) {
                    return "Cannot give item " + itemToken + ".";
                }
                return multiplayer.givePlayerItemByName(targetName, itemId, amount)
                    ? "Gave " + amount + " item(s) to " + targetName + "."
                    : "Player not found or inventory full: " + targetName;
            }
            return null;
        } catch (IOException exception) {
            return "Command failed: " + exception.getMessage();
        } catch (NumberFormatException exception) {
            return "Command failed: expected a number.";
        }
    }

    private String commandBlockFeedback(int changed, boolean fill) {
        if (changed == -1) {
            return "Coordinates are outside the world.";
        }
        if (changed < 0) {
            return "World or player is not available.";
        }
        if (fill) {
            return "Filled " + changed + " block(s).";
        }
        return changed == 0 ? "No blocks were changed." : "Changed the block.";
    }

    private String handleWhitelistCommand(String[] parts) throws IOException {
        if (parts.length < 2) {
            return "Whitelist is " + (properties.whitelist ? "on" : "off") + ". Entries: " + accessList.describeWhitelist();
        }
        String action = parts[1].toLowerCase(Locale.ROOT);
        if ("on".equals(action)) {
            properties.whitelist = true;
            properties.save();
            return "Whitelist enabled.";
        }
        if ("off".equals(action)) {
            properties.whitelist = false;
            properties.save();
            return "Whitelist disabled.";
        }
        if ("list".equals(action)) {
            return "Whitelist: " + accessList.describeWhitelist();
        }
        if (parts.length < 3) {
            return "Usage: whitelist <on|off|list|add|remove> [player|uuid]";
        }
        ResolvedPlayer target = resolvePlayer(parts[2]);
        if ("add".equals(action)) {
            accessList.addWhitelist(parts[2], target.uuid, target.name);
            return "Whitelisted " + target.display(parts[2]) + ".";
        }
        if ("remove".equals(action)) {
            accessList.removeWhitelist(parts[2], target.uuid, target.name);
            return "Removed " + target.display(parts[2]) + " from whitelist.";
        }
        return "Usage: whitelist <on|off|list|add|remove> [player|uuid]";
    }

    private boolean isDedicatedServerCommand(String command) {
        return "help".equals(command)
            || "status".equals(command)
            || "list".equals(command)
            || "say".equals(command)
            || "kick".equals(command)
            || "save".equals(command)
            || "save-all".equals(command)
            || "stop".equals(command)
            || "op".equals(command)
            || "deop".equals(command)
            || "whitelist".equals(command)
            || "ban".equals(command)
            || "pardon".equals(command)
            || "tp".equals(command)
            || "kill".equals(command)
            || "summon".equals(command)
            || "setblock".equals(command)
            || "fill".equals(command)
            || "gamemode".equals(command)
            || "give".equals(command)
            || "clear".equals(command);
    }

    private boolean isPlayerCheatCommand(String command) {
        return "tp".equals(command)
            || "kill".equals(command)
            || "summon".equals(command)
            || "setblock".equals(command)
            || "fill".equals(command)
            || "gamemode".equals(command)
            || "give".equals(command)
            || "clear".equals(command);
    }

    private boolean isPublicPlayerCommand(String command) {
        return "help".equals(command) || "list".equals(command);
    }

    private Byte resolveGiveItem(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String query = raw.trim().toLowerCase(Locale.ROOT);
        try {
            int value = Integer.parseInt(query);
            if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                return (byte) value;
            }
        } catch (NumberFormatException exception) {
            System.err.println("Failed to parse block ID from server command '" + query + "': " + exception);
        }
        String localName = query.startsWith("tinycraft:") ? query.substring("tinycraft:".length()) : query;
        String namespaced = localName.startsWith("minecraft:") ? localName : "minecraft:" + localName;
        BlockType block = BlockRegistry.typeByName(namespaced);
        if (block != null && InventoryItems.isCollectible((byte) block.numericId)) {
            return (byte) block.numericId;
        }
        if ("stick".equals(localName)) {
            return InventoryItems.STICK;
        }
        if ("coal".equals(localName)) {
            return InventoryItems.COAL_ITEM;
        }
        if ("iron_ingot".equals(localName)) {
            return InventoryItems.IRON_INGOT;
        }
        if ("diamond".equals(localName)) {
            return InventoryItems.DIAMOND_ITEM;
        }
        return null;
    }

    private ResolvedPlayer resolvePlayer(String token) {
        UUID uuid = multiplayer == null ? null : multiplayer.connectedPlayerUuid(token);
        String name = multiplayer == null ? null : multiplayer.connectedPlayerDisplayName(token);
        if (uuid == null) {
            try {
                uuid = UUID.fromString(token);
            } catch (IllegalArgumentException exception) {
                System.err.println("Failed to parse player UUID from server command '" + token + "': " + exception);
            }
        }
        if (name == null && uuid == null) {
            name = token;
        }
        return new ResolvedPlayer(uuid, name);
    }

    private String safeSenderName(String senderName) {
        return senderName == null || senderName.trim().isEmpty() ? "Console" : senderName.trim();
    }

    @Override
    public void onMultiplayerStatus(String status) {
        System.out.println(status);
    }

    @Override
    public void onMultiplayerChat(String message) {
        System.out.println(message);
    }

    @Override
    public void onClientWelcome(long seed, TerrainPreset terrainPreset, int dimensionId, double x, double y, double z, double worldTime) {
    }

    @Override
    public void onClientChunkColumn(int chunkX, int chunkZ) {
    }

    @Override
    public void onClientBlockUpdate(int x, int y, int z) {
    }

    @Override
    public void onClientDisconnected(String reason) {
    }

    @Override
    public void onClientHealth(double health) {
    }

    @Override
    public void onClientInventoryAdd(byte itemId, int count, int durabilityDamage) {
    }

    @Override
    public void onClientServerPlayerState(int dimensionId, double x, double y, double z, double yaw, double pitch, boolean creativeMode, boolean spectatorMode, double health) {
    }

    @Override
    public void onClientInventorySync(PlayerInventory authoritativeInventory) {
    }

    @Override
    public void onClientContainerOpen(int screenMode, int x, int y, int z, int windowId, ContainerInventory chest, FurnaceBlockEntity furnace) {
    }

    @Override
    public void onClientContainerUpdate(int screenMode, int x, int y, int z, int windowId, ContainerInventory chest, FurnaceBlockEntity furnace) {
    }

    @Override
    public void onClientContainerClose(int windowId) {
    }

    private void runTickLoop() {
        long lastNs = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            double delta = Math.min(0.2, (now - lastNs) / 1_000_000_000.0);
            lastNs = now;
            tick(delta);
            long sleepMillis = Math.max(1L, (long) (GameConfig.GAME_TICK_SECONDS * 1000.0));
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void tick(double delta) {
        if (world == null || multiplayer == null) {
            return;
        }
        multiplayer.drainEvents();
        if (!running || world == null || multiplayer == null) {
            return;
        }
        PlayerState activePlayer = multiplayer.firstConnectedPlayer();
        PlayerState serverPlayer = activePlayer == null ? simulationPlayer : activePlayer;
        world.setRenderDistanceChunks(properties.viewDistance);
        world.advanceWorldTime(delta);
        world.prepareForPlayer(serverPlayer);
        world.updateDroppedItems(serverPlayer, null, delta);
        world.updateMobs(serverPlayer, delta);
        ColumnUpdateList dirtyFromTicks = world.updateWorldTicks(serverPlayer, delta);
        multiplayer.broadcastBlockUpdates(world, dirtyFromTicks);
        multiplayer.broadcastBlockUpdates(world, world.drainNetworkDirtyBlocks());
        multiplayer.tickDedicated(world, serverPlayer, delta);
        multiplayer.broadcastBlockUpdates(world, world.drainNetworkDirtyBlocks());
        autosaveTimer += delta;
        if (autosaveTimer >= AUTOSAVE_SECONDS) {
            autosaveTimer = 0.0;
            save();
        }
    }

    private void save() {
        if (world == null) {
            return;
        }
        if (multiplayer != null) {
            multiplayer.saveConnectedPlayers(world);
        }
        world.saveAllLoadedColumns();
        System.out.println("Saved world.");
    }

    private ResolvedWorld resolveWorld() throws IOException {
        Path directory = properties.worldDirectory();
        Files.createDirectories(directory);
        WorldMetadata metadata = readWorldMetadataIfPresent(directory);
        if (metadata != null) {
            return new ResolvedWorld(directory, metadata.seed, metadata.terrainPreset);
        }

        TerrainPreset terrainPreset = properties.terrainPreset();
        Long explicitSeed = properties.explicitSeed();
        long seed = explicitSeed == null ? properties.randomSeed() : explicitSeed.longValue();
        writeWorldMetadata(directory, seed, 0, properties.allowCheats, 2, terrainPreset);
        return new ResolvedWorld(directory, seed, terrainPreset);
    }

    private WorldMetadata readWorldMetadataIfPresent(Path directory) throws IOException {
        Path levelPath = directory.resolve(GameConfig.SAVE_LEVEL_FILE);
        if (Files.isRegularFile(levelPath)) {
            String json = readUtf8(levelPath);
            long seed = parseStoredSeed(jsonValue(json, "seed"));
            TerrainPreset terrainPreset = TerrainPreset.fromMetadata(jsonValue(json, "worldType"));
            if (terrainPreset == TerrainPreset.LEGACY) {
                terrainPreset = properties.terrainPreset();
            }
            return new WorldMetadata(seed, terrainPreset);
        }

        Path metadataPath = directory.resolve(GameConfig.SAVE_METADATA_FILE);
        if (!Files.isRegularFile(metadataPath)) {
            return null;
        }
        String[] lines = readUtf8(metadataPath).split("\\R");
        long seed = lines.length == 0 ? 0L : parseStoredSeed(lines[0]);
        TerrainPreset terrainPreset = properties.terrainPreset();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("worldType=")) {
                terrainPreset = TerrainPreset.fromMetadata(line.substring(10));
            } else if (line.startsWith("terrainPreset=")) {
                terrainPreset = TerrainPreset.fromMetadata(line.substring(14));
            }
        }
        if (terrainPreset == TerrainPreset.LEGACY) {
            terrainPreset = properties.terrainPreset();
        }
        return new WorldMetadata(seed, terrainPreset);
    }

    private void writeWorldMetadata(Path directory, long seed, int gameMode, boolean allowCheats, int difficulty, TerrainPreset terrainPreset) throws IOException {
        Files.createDirectories(directory);
        Files.createDirectories(directory.resolve(GameConfig.SAVE_REGION_DIRECTORY));
        TerrainPreset storedPreset = terrainPreset == null ? TerrainPreset.DEFAULT : terrainPreset;
        String seedHex = Long.toUnsignedString(seed, 16);
        String levelJson = "{"
            + System.lineSeparator() + "  \"version\": 1,"
            + System.lineSeparator() + "  \"seed\": \"" + jsonEscape(seedHex) + "\","
            + System.lineSeparator() + "  \"gameMode\": " + gameMode + ","
            + System.lineSeparator() + "  \"allowCheats\": " + allowCheats + ","
            + System.lineSeparator() + "  \"difficulty\": " + difficulty + ","
            + System.lineSeparator() + "  \"worldType\": \"" + jsonEscape(storedPreset.metadataId()) + "\","
            + System.lineSeparator() + "  \"minY\": " + GameConfig.WORLD_MIN_Y + ","
            + System.lineSeparator() + "  \"height\": " + GameConfig.WORLD_HEIGHT + ","
            + System.lineSeparator() + "  \"seaLevel\": " + GameConfig.SEA_LEVEL + ","
            + System.lineSeparator() + "  \"createdWith\": \"TinyCraft server mcrx-1\""
            + System.lineSeparator() + "}"
            + System.lineSeparator();
        AtomicFiles.writeUtf8(directory.resolve(GameConfig.SAVE_LEVEL_FILE), levelJson);

        String metadata = seedHex
            + System.lineSeparator() + "mode=" + gameMode
            + System.lineSeparator() + "allowCheats=" + allowCheats
            + System.lineSeparator() + "difficulty=" + difficulty
            + System.lineSeparator() + "worldType=" + storedPreset.metadataId()
            + System.lineSeparator();
        AtomicFiles.writeUtf8(directory.resolve(GameConfig.SAVE_METADATA_FILE), metadata);
    }

    private String readUtf8(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private long parseStoredSeed(String seedText) {
        if (seedText == null || seedText.trim().isEmpty()) {
            return 0L;
        }
        String trimmed = seedText.trim();
        try {
            return Long.parseUnsignedLong(trimmed, 16);
        } catch (NumberFormatException ignored) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignoredAgain) {
                return ServerProperties.parseSeed(trimmed);
            }
        }
    }

    private String jsonValue(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) {
            return null;
        }
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            return valueEnd < 0 ? null : json.substring(valueStart + 1, valueEnd);
        }
        int valueEnd = valueStart;
        while (valueEnd < json.length() && ",}\r\n".indexOf(json.charAt(valueEnd)) < 0) {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd).trim();
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class ResolvedPlayer {
        final UUID uuid;
        final String name;

        ResolvedPlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        String display(String fallback) {
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
            if (uuid != null) {
                return uuid.toString();
            }
            return fallback;
        }
    }

    private static final class WorldMetadata {
        final long seed;
        final TerrainPreset terrainPreset;

        WorldMetadata(long seed, TerrainPreset terrainPreset) {
            this.seed = seed;
            this.terrainPreset = terrainPreset == null ? TerrainPreset.DEFAULT : terrainPreset;
        }
    }

    private static final class ResolvedWorld {
        final Path directory;
        final long seed;
        final TerrainPreset terrainPreset;

        ResolvedWorld(Path directory, long seed, TerrainPreset terrainPreset) {
            this.directory = directory;
            this.seed = seed;
            this.terrainPreset = terrainPreset;
        }
    }
}
