import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

final class MultiplayerManager {
    interface Listener {
        void onMultiplayerStatus(String status);
        void onMultiplayerChat(String message);
        void onClientWelcome(long seed, TerrainPreset terrainPreset, int dimensionId, double x, double y, double z, double worldTime);
        void onClientChunkColumn(int chunkX, int chunkZ);
        void onClientBlockUpdate(int x, int y, int z);
        void onClientDisconnected(String reason);
        void onClientHealth(double health);
        void onClientInventoryAdd(byte itemId, int count, int durabilityDamage);
        void onClientServerPlayerState(int dimensionId, double x, double y, double z, double yaw, double pitch, boolean creativeMode, boolean spectatorMode, double health);
        void onClientInventorySync(PlayerInventory authoritativeInventory);
        void onClientContainerOpen(int screenMode, int x, int y, int z, int windowId, ContainerInventory chest, FurnaceBlockEntity furnace);
        void onClientContainerUpdate(int screenMode, int x, int y, int z, int windowId, ContainerInventory chest, FurnaceBlockEntity furnace);
        void onClientContainerClose(int windowId);
    }

    interface ServerCommandDelegate {
        String joinRejectionReason(UUID uuid, String name);
        String executeServerCommand(UUID senderUuid, String senderName, String commandLine);
    }

    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final double PLAYER_SEND_INTERVAL = 0.05;
    private static final double HOST_BROADCAST_INTERVAL = 0.08;
    private static final double ENTITY_SNAPSHOT_INTERVAL = 0.05;
    private static final double PLAYER_LIST_INTERVAL = 1.0;
    private static final double PING_INTERVAL = 1.0;
    private static final double PING_TIMEOUT_SECONDS = 5.0;
    private static final double CLIENT_CHUNK_REQUEST_INTERVAL = 0.05;
    private static final int CLIENT_CHUNK_REQUESTS_PER_TICK = 32;
    private static final int INITIAL_CHUNK_SYNC_RADIUS = 8;
    private static final int INITIAL_CHUNK_SEND_BUDGET = 289;
    private static final double SERVER_REACH_TOLERANCE = 1.25;
    private static final int MAX_TEXT_CHARS = 512;
    private static final int RATE_WINDOW_MS = 1000;
    private static final int PLAYER_STATE_RATE = 240;
    private static final int CHUNK_REQUEST_RATE = 512;
    private static final int BLOCK_ACTION_RATE = 20;
    private static final int ATTACK_RATE = 20;
    private static final int CHAT_COMMAND_RATE = 8;
    private static final int CONTAINER_CLICK_RATE = 30;

    private final LocalProfile profile;
    private final Listener listener;
    private final ConcurrentLinkedQueue<Runnable> mainThreadEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BlockAction> pendingBlockActions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ChunkRequest> pendingChunkRequests = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, ServerClient> serverClients = new ConcurrentHashMap<>();
    private final HashSet<Long> requestedClientColumns = new HashSet<>();
    private final ArrayDeque<Long> clientColumnQueue = new ArrayDeque<>();
    private final GameClientSession clientSession = new GameClientSession();
    private final ArrayList<PlayerListEntry> playerListSnapshot = new ArrayList<>();

    private volatile boolean hostRunning;
    private volatile boolean clientRunning;
    private volatile VoxelWorld activeWorld;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Connection clientConnection;
    private PlayerState activeHostPlayer;
    private ServerCommandDelegate serverCommandDelegate;
    private int maxPlayers = 8;
    private boolean allowPvp = true;
    private int dedicatedChunkLogBudget = 8;
    private int dedicatedBlockActionLogBudget = 12;
    private String status = "Offline";
    private double clientPlayerSendTimer;
    private double hostBroadcastTimer;
    private double containerUpdateTimer;
    private double entitySnapshotTimer;
    private double playerListTimer;
    private double pingTimer;
    private double clientChunkRequestTimer;
    private long pendingClientPingTime = -1L;
    private long lastClientPongMillis;
    private volatile boolean clientDisconnectEventQueued;
    private int clientWindowId;
    private int clientDimensionId = GameConfig.DIMENSION_OVERWORLD;

    MultiplayerManager(LocalProfile profile, Listener listener) {
        this.profile = profile;
        this.listener = listener;
    }

    void setServerCommandDelegate(ServerCommandDelegate serverCommandDelegate) {
        this.serverCommandDelegate = serverCommandDelegate;
    }

    boolean isHosting() {
        return hostRunning;
    }

    boolean isClient() {
        return clientRunning;
    }

    boolean isMultiplayerActive() {
        return hostRunning || clientRunning;
    }

    String status() {
        return status;
    }

    void drainEvents() {
        Runnable event;
        while ((event = mainThreadEvents.poll()) != null) {
            event.run();
        }
    }

    boolean startHost(final VoxelWorld world, final PlayerState hostPlayer, int port) {
        stop();
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(port));
        } catch (IOException exception) {
            setStatus("Host failed: " + exception.getMessage());
            return false;
        }
        activeWorld = world;
        activeHostPlayer = hostPlayer;
        maxPlayers = 8;
        allowPvp = true;
        playerListSnapshot.clear();
        hostRunning = true;
        setStatus("Hosting on port " + port);
        acceptThread = new Thread(() -> acceptLoop(world, hostPlayer), "TinyCraft LAN host");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    boolean startDedicatedHost(final VoxelWorld world, int port, int maxPlayers, boolean allowPvp) {
        stop();
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        } catch (IOException exception) {
            setStatus("Dedicated host failed: " + exception.getMessage());
            return false;
        }
        activeWorld = world;
        activeHostPlayer = null;
        this.maxPlayers = Math.max(1, maxPlayers);
        this.allowPvp = allowPvp;
        dedicatedChunkLogBudget = 8;
        playerListSnapshot.clear();
        hostRunning = true;
        setStatus("Listening on 0.0.0.0:" + port);
        acceptThread = new Thread(() -> acceptLoop(world, null), "TinyCraft dedicated listener");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    boolean connect(final String host, final int port) {
        stop();
        clientRunning = true;
        clientDisconnectEventQueued = false;
        requestedClientColumns.clear();
        clientColumnQueue.clear();
        playerListSnapshot.clear();
        clientSession.replacePlayerList(playerListSnapshot);
        pendingClientPingTime = -1L;
        lastClientPongMillis = 0L;
        setStatus("Connecting to " + host + ":" + port);
        Thread thread = new Thread(() -> clientConnectLoop(host, port), "TinyCraft LAN client");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    void stop() {
        boolean hadNetworkState = hostRunning
            || clientRunning
            || clientConnection != null
            || serverSocket != null
            || !serverClients.isEmpty();
        hostRunning = false;
        clientRunning = false;
        closeQuietly(clientConnection);
        clientConnection = null;
        for (ServerClient client : serverClients.values()) {
            closeQuietly(client.connection);
        }
        serverClients.clear();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        pendingBlockActions.clear();
        pendingChunkRequests.clear();
        requestedClientColumns.clear();
        clientColumnQueue.clear();
        playerListSnapshot.clear();
        clientSession.replacePlayerList(playerListSnapshot);
        if (hadNetworkState || !"Offline".equals(status)) {
            setStatus("Offline");
        }
    }

    void tickHost(VoxelWorld world, PlayerState hostPlayer, byte hostHeldItem, double deltaTime) {
        if (!hostRunning) {
            return;
        }
        activeWorld = world;
        activeHostPlayer = hostPlayer;
        processHostRequests(world);
        processParadisePortalTravel(world, deltaTime);
        processRemoteClientItemPickups(world);
        hostBroadcastTimer += deltaTime;
        entitySnapshotTimer += deltaTime;
        playerListTimer += deltaTime;
        pingTimer += deltaTime;
        if (hostBroadcastTimer >= HOST_BROADCAST_INTERVAL) {
            hostBroadcastTimer = 0.0;
            broadcastPlayerState(profile.uuid, profile.name, hostPlayer, hostHeldItem);
            broadcastWorldTime(world.getWorldTime());
            for (ServerClient client : serverClients.values()) {
                broadcastPlayerState(client.uuid, client.name, client.player, client.heldItem);
            }
        }
        sendOpenFurnaceUpdates(world, deltaTime);
        if (entitySnapshotTimer >= ENTITY_SNAPSHOT_INTERVAL) {
            entitySnapshotTimer = 0.0;
            broadcastMobSnapshot(world);
            broadcastDroppedItemSnapshot(world);
        }
        if (pingTimer >= PING_INTERVAL) {
            pingTimer = 0.0;
            pingServerClients();
        }
        if (playerListTimer >= PLAYER_LIST_INTERVAL) {
            playerListTimer = 0.0;
            broadcastPlayerList(hostPlayer, hostHeldItem);
        }
    }

    void tickDedicated(VoxelWorld world, double deltaTime) {
        if (!hostRunning) {
            return;
        }
        activeWorld = world;
        processHostRequests(world);
        processParadisePortalTravel(world, deltaTime);
        processRemoteClientItemPickups(world);
        hostBroadcastTimer += deltaTime;
        entitySnapshotTimer += deltaTime;
        playerListTimer += deltaTime;
        pingTimer += deltaTime;
        if (hostBroadcastTimer >= HOST_BROADCAST_INTERVAL) {
            hostBroadcastTimer = 0.0;
            broadcastWorldTime(world.getWorldTime());
            for (ServerClient client : serverClients.values()) {
                broadcastPlayerState(client.uuid, client.name, client.player, client.heldItem);
            }
        }
        sendOpenFurnaceUpdates(world, deltaTime);
        if (entitySnapshotTimer >= ENTITY_SNAPSHOT_INTERVAL) {
            entitySnapshotTimer = 0.0;
            broadcastMobSnapshot(world);
            broadcastDroppedItemSnapshot(world);
        }
        if (pingTimer >= PING_INTERVAL) {
            pingTimer = 0.0;
            pingServerClients();
        }
        if (playerListTimer >= PLAYER_LIST_INTERVAL) {
            playerListTimer = 0.0;
            broadcastPlayerList(null, GameConfig.AIR);
        }
    }

    void tickClient(VoxelWorld world, PlayerState player, byte heldItem, int selectedHotbarSlot, int renderDistanceChunks, double deltaTime) {
        if (!clientRunning || clientConnection == null) {
            return;
        }
        activeWorld = world;
        clientDimensionId = player.dimensionId;
        clientPlayerSendTimer += deltaTime;
        clientChunkRequestTimer += deltaTime;
        pingTimer += deltaTime;
        if (clientPlayerSendTimer >= PLAYER_SEND_INTERVAL) {
            clientPlayerSendTimer = 0.0;
            sendPlayerState(clientConnection, profile.uuid, profile.name, player, heldItem, selectedHotbarSlot);
        }
        if (clientChunkRequestTimer >= CLIENT_CHUNK_REQUEST_INTERVAL) {
            clientChunkRequestTimer = 0.0;
            queueClientChunkRequests(player, renderDistanceChunks);
            for (int i = 0; i < CLIENT_CHUNK_REQUESTS_PER_TICK && !clientColumnQueue.isEmpty(); i++) {
                long key = clientColumnQueue.removeFirst();
                sendChunkRequest(clientConnection, unpackDimension(key), unpackColumnX(key), unpackColumnZ(key));
            }
        }
        if (pingTimer >= PING_INTERVAL) {
            pingTimer = 0.0;
            sendClientPing();
        }
        if (lastClientPongMillis > 0L && System.currentTimeMillis() - lastClientPongMillis > (long) (PING_TIMEOUT_SECONDS * 1000.0)) {
            clientSession.setPingMs(-1);
        }
    }

    void requestInitialClientChunks(int dimensionId, double x, double z) {
        if (!clientRunning || clientConnection == null) {
            return;
        }
        int chunkX = Math.floorDiv((int) Math.floor(x), GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv((int) Math.floor(z), GameConfig.CHUNK_SIZE);
        queueClientChunkRequests(dimensionId, chunkX, chunkZ, INITIAL_CHUNK_SYNC_RADIUS);
        int sent = 0;
        while (sent < INITIAL_CHUNK_SEND_BUDGET && !clientColumnQueue.isEmpty()) {
            long key = clientColumnQueue.removeFirst();
            sendChunkRequest(clientConnection, unpackDimension(key), unpackColumnX(key), unpackColumnZ(key));
            sent++;
        }
    }

    void sendChat(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (hostRunning) {
            String senderName = profile == null ? "Server" : profile.name;
            String formatted = "<" + senderName + "> " + message;
            emitChat(formatted);
            broadcastChat(formatted);
        } else if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.CHAT, output -> output.writeUTF(message));
        }
    }

    void sendCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return;
        }
        if (hostRunning) {
            handleServerCommand(profile == null ? null : profile.uuid, profile == null ? "Server" : profile.name, commandLine);
        } else if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.COMMAND, output -> output.writeUTF(commandLine));
        }
    }

    void requestContainerOpen(int screenMode, int x, int y, int z) {
        if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.CONTAINER_OPEN_REQUEST, output -> {
                output.writeInt(screenMode);
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(z);
            });
        }
    }

    void sendContainerClick(InventorySlotRef ref, boolean rightClick, boolean shiftDown, boolean middleClick) {
        if (!clientRunning || clientConnection == null || ref == null) {
            return;
        }
        send(clientConnection, MultiplayerProtocol.CONTAINER_CLICK, output -> {
            output.writeInt(clientWindowId);
            output.writeInt(ref.group.ordinal());
            output.writeInt(ref.index);
            output.writeBoolean(rightClick);
            output.writeBoolean(shiftDown);
            output.writeBoolean(middleClick);
        });
    }

    void sendContainerClose() {
        if (!clientRunning || clientConnection == null) {
            return;
        }
        final int windowId = clientWindowId;
        send(clientConnection, MultiplayerProtocol.CONTAINER_CLOSE, output -> output.writeInt(windowId));
        clientWindowId = 0;
    }

    void sendDropSelectedHotbarItem(int selectedHotbarSlot) {
        if (!clientRunning || clientConnection == null) {
            return;
        }
        send(clientConnection, MultiplayerProtocol.ITEM_DROP, output -> {
            output.writeInt(clientPlayerDimension());
            output.writeInt(selectedHotbarSlot);
            output.writeBoolean(false);
        });
    }

    List<PlayerListEntry> playerListSnapshot() {
        ArrayList<PlayerListEntry> copy = new ArrayList<>(playerListSnapshot);
        final UUID localUuid = profile == null ? null : profile.uuid;
        Collections.sort(copy, new Comparator<PlayerListEntry>() {
            @Override
            public int compare(PlayerListEntry a, PlayerListEntry b) {
                boolean aLocal = localUuid != null && localUuid.equals(a.uuid);
                boolean bLocal = localUuid != null && localUuid.equals(b.uuid);
                if (aLocal != bLocal) {
                    return aLocal ? -1 : 1;
                }
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return Collections.unmodifiableList(copy);
    }

    int pingMs() {
        return clientSession.pingMs();
    }

    String playerListText() {
        List<PlayerListEntry> entries = playerListSnapshot();
        if (entries.isEmpty()) {
            return "Players: (none)";
        }
        StringBuilder builder = new StringBuilder("Players: ");
        for (int i = 0; i < entries.size(); i++) {
            PlayerListEntry entry = entries.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(entry.name).append(" (").append(formatPing(entry.pingMs)).append(")");
        }
        return builder.toString();
    }

    String pingText() {
        return "Ping: " + formatPing(clientSession.pingMs());
    }

    private String formatPing(int pingMs) {
        return pingMs < 0 ? "? ms" : pingMs + " ms";
    }

    private void handleServerCommand(UUID senderUuid, String senderName, String commandLine) {
        String raw = commandLine == null ? "" : commandLine.trim();
        if (raw.startsWith("/")) {
            raw = raw.substring(1).trim();
        }
        if (raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split("\\s+", 3);
        String command = parts[0].toLowerCase(Locale.ROOT);
        if ("gm".equals(command)) {
            raw = "gamemode" + (raw.length() > 2 ? raw.substring(2) : "");
            parts = raw.split("\\s+", 3);
            command = "gamemode";
        } else if ("gms".equals(command) || "gmc".equals(command) || "gmsp".equals(command)) {
            String mode = "gmc".equals(command) ? "creative" : ("gmsp".equals(command) ? "spectator" : "survival");
            String target = parts.length >= 2 ? " " + parts[1] : "";
            raw = "gamemode " + mode + target;
            parts = raw.split("\\s+", 3);
            command = "gamemode";
        }
        if ("list".equals(command)) {
            sendCommandFeedback(senderUuid, playerListText());
        } else if ("ping".equals(command)) {
            sendCommandFeedback(senderUuid, "Ping: " + pingFor(senderUuid));
        } else if ("msg".equals(command)) {
            if (parts.length < 3) {
                sendCommandFeedback(senderUuid, "Usage: /msg <player> <message>");
            } else {
                sendPrivateMessage(senderUuid, senderName, parts[1], parts[2]);
            }
        } else if ("kick".equals(command)) {
            if (runDelegatedServerCommand(senderUuid, senderName, raw)) {
                return;
            }
            if (profile != null && (senderUuid == null || !profile.uuid.equals(senderUuid))) {
                sendCommandFeedback(senderUuid, "Only the host can use /kick.");
                return;
            }
            if (parts.length < 2) {
                sendCommandFeedback(senderUuid, "Usage: /kick <player> [reason]");
            } else {
                String[] kickParts = raw.split("\\s+", 3);
                kickPlayer(kickParts[1], kickParts.length >= 3 ? kickParts[2] : "Kicked by host.");
            }
        } else if ("clear".equals(command)) {
            if (runDelegatedServerCommand(senderUuid, senderName, raw)) {
                return;
            }
            ServerClient client = senderUuid == null ? null : serverClients.get(senderUuid);
            if (client == null) {
                sendCommandFeedback(senderUuid, "Only connected clients can use /clear in multiplayer.");
                return;
            }
            client.inventory.clearAll();
            sendInventorySync(client);
            sendCommandFeedback(senderUuid, "Inventory cleared.");
        } else if ("give".equals(command)) {
            if (runDelegatedServerCommand(senderUuid, senderName, raw)) {
                return;
            }
            ServerClient client = senderUuid == null ? null : serverClients.get(senderUuid);
            if (client == null || parts.length < 3) {
                sendCommandFeedback(senderUuid, "Usage: /give <id|tinycraft:name> <amount>");
                return;
            }
            Byte itemId = resolveGiveItem(parts[1]);
            int amount;
            try {
                amount = Integer.parseInt(parts[2]);
            } catch (NumberFormatException exception) {
                sendCommandFeedback(senderUuid, "Usage: /give <id|tinycraft:name> <amount>");
                return;
            }
            if (itemId == null || amount <= 0 || !client.inventory.addItem(itemId, Math.min(amount, 999))) {
                sendCommandFeedback(senderUuid, "Cannot give item " + parts[1] + ".");
                return;
            }
            sendInventorySync(client);
            sendCommandFeedback(senderUuid, "Gave " + amount + " item(s).");
        } else if ("gamemode".equals(command)) {
            if (runDelegatedServerCommand(senderUuid, senderName, raw)) {
                return;
            }
            if (profile != null && (senderUuid == null || !profile.uuid.equals(senderUuid))) {
                sendCommandFeedback(senderUuid, "Only the host can use /gamemode on this LAN world.");
                return;
            }
            if (parts.length < 2) {
                sendCommandFeedback(senderUuid, "Usage: /gamemode <survival|creative|spectator>");
                return;
            }
            String mode = parts[1].toLowerCase(Locale.ROOT);
            String targetName = parts.length >= 3 ? parts[2] : senderName;
            if (!isValidGameModeName(mode)) {
                sendCommandFeedback(senderUuid, "Unknown gamemode: " + parts[1]);
                return;
            }
            if (setPlayerGameModeByName(targetName, mode)) {
                sendCommandFeedback(senderUuid, "Set " + targetName + " to " + normalizedGameModeName(mode) + ".");
            } else {
                sendCommandFeedback(senderUuid, "Player not found: " + targetName);
            }
        } else {
            if (!runDelegatedServerCommand(senderUuid, senderName, raw)) {
                sendCommandFeedback(senderUuid, "Unknown multiplayer command: /" + parts[0]);
            }
        }
    }

    private boolean runDelegatedServerCommand(UUID senderUuid, String senderName, String raw) {
        if (serverCommandDelegate == null) {
            return false;
        }
        String result = serverCommandDelegate.executeServerCommand(senderUuid, senderName, raw);
        if (result == null) {
            return false;
        }
        if (!result.trim().isEmpty()) {
            for (String line : result.split("\\R")) {
                sendCommandFeedback(senderUuid, line);
            }
        }
        return true;
    }

    private String pingFor(UUID uuid) {
        if (uuid != null && profile != null && profile.uuid.equals(uuid)) {
            return "0 ms";
        }
        ServerClient client = uuid == null ? null : serverClients.get(uuid);
        return client == null ? "? ms" : formatPing(client.pingMs);
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
        } catch (NumberFormatException ignored) {
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

    private void sendPrivateMessage(UUID senderUuid, String senderName, String targetName, String message) {
        ServerClient target = findClientByName(targetName);
        if (target == null) {
            sendCommandFeedback(senderUuid, "Player not found: " + targetName);
            return;
        }
        String from = senderName == null || senderName.trim().isEmpty() ? "Server" : senderName;
        String formatted = "[PM] <" + from + "> " + message;
        send(target.connection, MultiplayerProtocol.CHAT, output -> output.writeUTF(formatted));
        sendCommandFeedback(senderUuid, "[PM to " + target.name + "] " + message);
    }

    private void kickPlayer(String targetName, String reason) {
        ServerClient target = findClientByName(targetName);
        if (target == null) {
            emitChat("Player not found: " + targetName);
            return;
        }
        sendDisconnect(target.connection, reason == null || reason.trim().isEmpty() ? "Kicked." : reason);
        closeQuietly(target.connection);
    }

    private ServerClient findClientByName(String name) {
        if (name == null) {
            return null;
        }
        for (ServerClient client : serverClients.values()) {
            if (client.name.equalsIgnoreCase(name)) {
                return client;
            }
        }
        return null;
    }

    private void sendCommandFeedback(UUID targetUuid, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        if (targetUuid == null || (profile != null && profile.uuid.equals(targetUuid))) {
            emitChat(message);
            return;
        }
        ServerClient client = serverClients.get(targetUuid);
        if (client != null) {
            send(client.connection, MultiplayerProtocol.CHAT, output -> output.writeUTF(message));
        }
    }

    void sendBlockBreak(int x, int y, int z, byte heldItem, int selectedHotbarSlot) {
        if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.BLOCK_ACTION, output -> {
                output.writeByte(MultiplayerProtocol.BLOCK_BREAK_FINISH);
                output.writeInt(clientPlayerDimension());
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(z);
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(z);
                output.writeByte(heldItem);
                output.writeInt(selectedHotbarSlot);
            });
        }
    }

    void sendBlockBreakStart(int x, int y, int z, byte heldItem, int selectedHotbarSlot) {
        if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.BLOCK_ACTION, output -> {
                output.writeByte(MultiplayerProtocol.BLOCK_BREAK_START);
                output.writeInt(clientPlayerDimension());
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(z);
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(z);
                output.writeByte(heldItem);
                output.writeInt(selectedHotbarSlot);
            });
        }
    }

    void sendBlockPlace(RayHit hit, byte heldItem, int selectedHotbarSlot) {
        if (hit == null || !clientRunning || clientConnection == null) {
            return;
        }
        send(clientConnection, MultiplayerProtocol.BLOCK_ACTION, output -> {
            output.writeByte(MultiplayerProtocol.BLOCK_PLACE);
            output.writeInt(clientPlayerDimension());
            output.writeInt(hit.x);
            output.writeInt(hit.y);
            output.writeInt(hit.z);
            output.writeInt(hit.previousX);
            output.writeInt(hit.previousY);
            output.writeInt(hit.previousZ);
            output.writeByte(heldItem);
            output.writeInt(selectedHotbarSlot);
        });
    }

    private int clientPlayerDimension() {
        return clientDimensionId;
    }

    void sendPlayerAttack(UUID targetUuid, int damage) {
        if (targetUuid == null || damage <= 0) {
            return;
        }
        if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.PLAYER_ATTACK, output -> {
                MultiplayerProtocol.writeUuid(output, targetUuid);
                output.writeInt(damage);
            });
        } else if (hostRunning) {
            if (profile != null) {
                applyPlayerAttack(profile.uuid, targetUuid, damage);
            }
        }
    }

    void sendMobAttack(int damage, double knockback) {
        if (damage <= 0) {
            return;
        }
        if (clientRunning && clientConnection != null) {
            send(clientConnection, MultiplayerProtocol.MOB_ATTACK, output -> {
                output.writeInt(damage);
                output.writeDouble(knockback);
            });
        }
    }

    void broadcastServerMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String formatted = "[Server] " + message.trim();
        emitChat(formatted);
        broadcastChat(formatted);
    }

    int connectedPlayerCount() {
        return serverClients.size();
    }

    String connectedPlayerNames() {
        StringBuilder builder = new StringBuilder();
        for (ServerClient client : serverClients.values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(client.name);
        }
        return builder.length() == 0 ? "(none)" : builder.toString();
    }

    UUID connectedPlayerUuid(String targetName) {
        ServerClient client = findClientByName(targetName);
        return client == null ? null : client.uuid;
    }

    String connectedPlayerDisplayName(String targetName) {
        ServerClient client = findClientByName(targetName);
        return client == null ? null : client.name;
    }

    boolean teleportPlayerByName(String targetName, double x, double y, double z) {
        ServerClient client = findClientByName(targetName);
        VoxelWorld world = activeWorld;
        if (client == null || !client.connection.open || world == null || !isFinite(x) || !isFinite(y) || !isFinite(z)) {
            return false;
        }
        if (!world.isInside((int) Math.floor(x), (int) Math.floor(Math.max(GameConfig.WORLD_MIN_Y, Math.min(GameConfig.WORLD_MAX_Y, y))), (int) Math.floor(z))) {
            return false;
        }
        y = world.safeStandingYAt(x, z);
        client.player.setPosition(x, y, z);
        client.player.capturePreviousPosition();
        client.teleportGraceUntilMillis = System.currentTimeMillis() + 1500L;
        sendServerPlayerState(client);
        broadcastPlayerState(client.uuid, client.name, client.player, client.heldItem);
        return true;
    }

    boolean setPlayerGameModeByName(String targetName, String mode) {
        ServerClient client = findClientByName(targetName);
        if (client == null || !client.connection.open) {
            return false;
        }
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        client.player.creativeMode = "creative".equals(normalized) || "1".equals(normalized);
        client.player.spectatorMode = "spectator".equals(normalized) || "3".equals(normalized);
        client.player.flightEnabled = client.player.creativeMode || client.player.spectatorMode;
        if (!client.player.spectatorMode) {
            client.player.sneaking = false;
        }
        sendServerPlayerState(client);
        sendInventorySync(client);
        broadcastPlayerState(client.uuid, client.name, client.player, client.heldItem);
        broadcastPlayerList(null, GameConfig.AIR);
        VoxelWorld world = activeWorld;
        if (world != null) {
            world.saveNetworkPlayerState(client.uuid, client.player, client.inventory);
        }
        return true;
    }

    private boolean isValidGameModeName(String mode) {
        return "survival".equals(mode) || "creative".equals(mode) || "spectator".equals(mode)
            || "0".equals(mode) || "1".equals(mode) || "3".equals(mode);
    }

    private String normalizedGameModeName(String mode) {
        if ("1".equals(mode) || "creative".equals(mode)) {
            return "creative";
        }
        if ("3".equals(mode) || "spectator".equals(mode)) {
            return "spectator";
        }
        return "survival";
    }

    boolean clearPlayerInventoryByName(String targetName) {
        ServerClient client = findClientByName(targetName);
        if (client == null || !client.connection.open) {
            return false;
        }
        client.inventory.clearAll();
        sendInventorySync(client);
        return true;
    }

    boolean givePlayerItemByName(String targetName, byte itemId, int amount) {
        ServerClient client = findClientByName(targetName);
        if (client == null || !client.connection.open || amount <= 0) {
            return false;
        }
        boolean added = client.inventory.addItem(itemId, Math.min(amount, 999));
        sendInventorySync(client);
        return added;
    }

    private void sendServerPlayerState(ServerClient client) {
        if (client == null || !client.connection.open) {
            return;
        }
        send(client.connection, MultiplayerProtocol.SERVER_PLAYER_STATE, output -> {
            output.writeInt(client.player.dimensionId);
            output.writeDouble(client.player.x);
            output.writeDouble(client.player.y);
            output.writeDouble(client.player.z);
            output.writeDouble(client.player.yaw);
            output.writeDouble(client.player.pitch);
            output.writeBoolean(client.player.creativeMode);
            output.writeBoolean(client.player.spectatorMode);
            output.writeDouble(client.player.health);
        });
    }

    void saveConnectedPlayers(VoxelWorld world) {
        if (world == null) {
            return;
        }
        for (ServerClient client : serverClients.values()) {
            world.saveNetworkPlayerState(client.uuid, client.player, client.inventory);
        }
    }

    void kickByName(String targetName, String reason) {
        kickPlayer(targetName, reason);
    }

    PlayerState firstConnectedPlayer() {
        for (ServerClient client : serverClients.values()) {
            return client.player;
        }
        return activeHostPlayer;
    }

    void broadcastBlockNeighborhood(VoxelWorld world, int x, int y, int z) {
        if (!hostRunning) {
            return;
        }
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    broadcastBlockState(world, x + dx, y + dy, z + dz);
                }
            }
        }
    }

    void broadcastBlockUpdates(VoxelWorld world, ColumnUpdateList blocks) {
        if (!hostRunning || world == null || blocks == null) {
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            broadcastBlockState(world, blocks.xAt(i), blocks.yAt(i), blocks.zAt(i));
        }
    }

    private void acceptLoop(VoxelWorld world, PlayerState hostPlayer) {
        while (hostRunning && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Connection connection = new Connection(socket);
                Thread thread = new Thread(() -> handleServerClient(connection, world, hostPlayer), "TinyCraft LAN peer");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException exception) {
                if (hostRunning) {
                    setStatus("Host accept failed: " + exception.getMessage());
                }
            }
        }
    }

    private void handleServerClient(Connection connection, VoxelWorld world, PlayerState hostPlayer) {
        UUID uuid = null;
        ServerClient acceptedClient = null;
        try {
            MultiplayerProtocol.Packet hello = MultiplayerProtocol.readPacket(connection.input);
            if (hello == null || hello.type != MultiplayerProtocol.HELLO) {
                throw new IOException("missing HELLO");
            }
            DataInputStream input = hello.input;
            int magic = input.readInt();
            int version = input.readInt();
            uuid = MultiplayerProtocol.readUuid(input);
            String name = LocalProfile.sanitizeName(input.readUTF(), "Player");
            if (magic != MultiplayerProtocol.MAGIC || version != MultiplayerProtocol.VERSION) {
                sendDisconnect(connection, "Incompatible multiplayer protocol.");
                return;
            }
            if (serverCommandDelegate != null) {
                String rejection = serverCommandDelegate.joinRejectionReason(uuid, name);
                if (rejection != null && !rejection.trim().isEmpty()) {
                    sendDisconnect(connection, rejection);
                    return;
                }
            }
            if (profile != null && profile.uuid.equals(uuid)) {
                sendDisconnect(connection, "Duplicate player uuid.");
                return;
            }
            if (serverClients.size() >= maxPlayers) {
                sendDisconnect(connection, "Server is full.");
                return;
            }
            ServerClient client = new ServerClient(uuid, name, connection);
            if (world.loadNetworkPlayerState(uuid, client.player, client.inventory)) {
                world.setActiveDimensionFor(client.player);
                repairUnsafeJoinPosition(world, client.player);
                client.player.capturePreviousPosition();
            } else if (hostPlayer != null) {
                client.player.dimensionId = hostPlayer.dimensionId;
                world.setActiveDimensionFor(hostPlayer);
                client.player.setPosition(hostPlayer.x + 1.5, hostPlayer.y, hostPlayer.z + 1.5);
                client.player.y = world.safeStandingYAt(client.player.x, client.player.z);
            } else {
                world.placePlayerAtSpawn(client.player);
            }
            client.teleportGraceUntilMillis = System.currentTimeMillis() + 2500L;
            if (serverClients.putIfAbsent(uuid, client) != null) {
                sendDisconnect(connection, "Duplicate player uuid.");
                return;
            }
            acceptedClient = client;
            if (profile == null) {
                System.out.println("Join: " + client.name + " " + client.uuid);
            }
            mainThreadEvents.add(() -> world.updateRemotePlayer(
                client.uuid,
                client.name,
                client.player.x,
                client.player.y,
                client.player.z,
                client.player.yaw,
                client.player.pitch,
                client.heldItem,
                client.player.sneaking,
                client.player.spectatorMode
            ));
            send(connection, MultiplayerProtocol.WELCOME, output -> {
                output.writeLong(world.getSeed());
                output.writeUTF(world.getTerrainPreset().metadataId());
                output.writeInt(client.player.dimensionId);
                output.writeDouble(client.player.x);
                output.writeDouble(client.player.y);
                output.writeDouble(client.player.z);
                output.writeDouble(world.getWorldTime());
            });
            sendInventorySync(client);
            sendServerPlayerState(client);
            broadcastPlayerState(client.uuid, client.name, client.player, client.heldItem);
            broadcastPlayerList(null, GameConfig.AIR);
            emitChat(name + " joined the world.");
            broadcastChat(name + " joined the world.");
            while (hostRunning && connection.open) {
                MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(connection.input);
                if (packet == null) {
                    break;
                }
                handleServerPacket(client, packet);
            }
        } catch (IOException exception) {
            if (hostRunning) {
                setStatus("Client disconnected: " + exception.getMessage());
                if (profile == null) {
                    System.out.println("Client error: " + exception.getMessage());
                }
            }
        } finally {
            closeQuietly(connection);
            if (uuid != null) {
                ServerClient removed = acceptedClient != null && serverClients.remove(uuid, acceptedClient) ? acceptedClient : null;
                if (removed != null) {
                    world.saveNetworkPlayerState(removed.uuid, removed.player, removed.inventory);
                    mainThreadEvents.add(() -> world.removeRemotePlayer(removed.uuid));
                    broadcastDespawn(uuid);
                    emitChat(removed.name + " left the world.");
                    if (profile == null) {
                        System.out.println("Leave: " + removed.name + " " + removed.uuid);
                    }
                }
            }
        }
    }

    private void handleServerPacket(ServerClient client, MultiplayerProtocol.Packet packet) throws IOException {
        DataInputStream input = packet.input;
        if (packet.type == MultiplayerProtocol.PLAYER_STATE) {
            requireRate(client.playerStateRate, PLAYER_STATE_RATE, client, "Too many player state packets.");
            requirePayloadLimit(packet, 256);
            readPlayerStateInto(client, input);
        } else if (packet.type == MultiplayerProtocol.CHAT) {
            requireRate(client.chatCommandRate, CHAT_COMMAND_RATE, client, "Too many chat packets.");
            requirePayloadLimit(packet, MultiplayerProtocol.MAX_TEXT_BYTES);
            String message = input.readUTF();
            if (message.length() > MAX_TEXT_CHARS) {
                throw new IOException("chat message too long");
            }
            String formatted = "<" + client.name + "> " + message;
            emitChat(formatted);
            broadcastChat(formatted);
        } else if (packet.type == MultiplayerProtocol.CHUNK_REQUEST) {
            requireRate(client.chunkRequestRate, CHUNK_REQUEST_RATE, client, "Too many chunk requests.");
            requirePayloadLimit(packet, 12);
            pendingChunkRequests.add(new ChunkRequest(client, input.readInt(), input.readInt(), input.readInt()));
        } else if (packet.type == MultiplayerProtocol.BLOCK_ACTION) {
            requireRate(client.blockActionRate, BLOCK_ACTION_RATE, client, "Too many block actions.");
            requirePayloadLimit(packet, 34);
            BlockAction action = BlockAction.read(client, input);
            if (profile == null && dedicatedBlockActionLogBudget > 0) {
                dedicatedBlockActionLogBudget--;
                System.out.println("Block action received: " + client.name + " kind=" + action.kind
                    + " at " + action.x + "," + action.y + "," + action.z);
                if (dedicatedBlockActionLogBudget == 0) {
                    System.out.println("Block action logging muted after initial burst.");
                }
            }
            pendingBlockActions.add(action);
        } else if (packet.type == MultiplayerProtocol.PLAYER_ATTACK) {
            requireRate(client.attackRate, ATTACK_RATE, client, "Too many attack packets.");
            requirePayloadLimit(packet, 20);
            UUID targetUuid = MultiplayerProtocol.readUuid(input);
            int damage = input.readInt();
            applyPlayerAttack(client.uuid, targetUuid, damage);
        } else if (packet.type == MultiplayerProtocol.MOB_ATTACK) {
            requireRate(client.attackRate, ATTACK_RATE, client, "Too many attack packets.");
            requirePayloadLimit(packet, 12);
            int damage = input.readInt();
            double knockback = input.readDouble();
            applyMobAttack(client, damage, knockback);
        } else if (packet.type == MultiplayerProtocol.PING) {
            requirePayloadLimit(packet, 8);
            long timestamp = input.readLong();
            send(client.connection, MultiplayerProtocol.PONG, output -> output.writeLong(timestamp));
        } else if (packet.type == MultiplayerProtocol.PONG) {
            requirePayloadLimit(packet, 8);
            long timestamp = input.readLong();
            if (client.pendingPingTime == timestamp) {
                client.pingMs = (int) Math.max(0L, Math.min(9999L, System.currentTimeMillis() - timestamp));
                client.pendingPingTime = -1L;
            }
        } else if (packet.type == MultiplayerProtocol.COMMAND) {
            requireRate(client.chatCommandRate, CHAT_COMMAND_RATE, client, "Too many command packets.");
            requirePayloadLimit(packet, MultiplayerProtocol.MAX_TEXT_BYTES);
            handleServerCommand(client.uuid, client.name, input.readUTF());
        } else if (packet.type == MultiplayerProtocol.CONTAINER_OPEN_REQUEST) {
            requireRate(client.containerClickRate, CONTAINER_CLICK_RATE, client, "Too many container packets.");
            requirePayloadLimit(packet, 16);
            handleContainerOpenRequest(client, input);
        } else if (packet.type == MultiplayerProtocol.CONTAINER_CLICK) {
            requireRate(client.containerClickRate, CONTAINER_CLICK_RATE, client, "Too many container clicks.");
            requirePayloadLimit(packet, 18);
            handleContainerClick(client, input);
        } else if (packet.type == MultiplayerProtocol.CONTAINER_CLOSE) {
            requirePayloadLimit(packet, 4);
            int windowId = input.readInt();
            if (client.activeWindowId == windowId) {
                closeServerContainer(client);
            }
        } else if (packet.type == MultiplayerProtocol.ITEM_DROP) {
            requireRate(client.containerClickRate, CONTAINER_CLICK_RATE, client, "Too many item drops.");
            requirePayloadLimit(packet, 9);
            handleItemDrop(client, input);
        } else if (packet.type == MultiplayerProtocol.DISCONNECT) {
            closeQuietly(client.connection);
        }
    }

    private void readPlayerStateInto(ServerClient client, DataInputStream input) throws IOException {
        UUID packetUuid = MultiplayerProtocol.readUuid(input);
        String packetName = LocalProfile.sanitizeName(input.readUTF(), client.name);
        if (!client.uuid.equals(packetUuid)) {
            throw new IOException("player uuid mismatch");
        }
        client.name = packetName;
        client.player.capturePreviousPosition();
        int packetDimensionId = input.readInt();
        double x = input.readDouble();
        double y = input.readDouble();
        double z = input.readDouble();
        double yaw = input.readDouble();
        double pitch = input.readDouble();
        input.readByte();
        client.player.sneaking = input.readBoolean();
        input.readBoolean();
        double packetHealth = input.readDouble();
        int selectedHotbarSlot = input.available() >= 4 ? input.readInt() : 0;
        if (!isFinite(x) || !isFinite(y) || !isFinite(z) || !isFinite(yaw) || !isFinite(pitch) || !isFinite(packetHealth)) {
            throw new IOException("invalid player state");
        }
        if (packetDimensionId != client.player.dimensionId) {
            if (client.teleportGraceUntilMillis <= System.currentTimeMillis()) {
                throw new IOException("player dimension mismatch");
            }
            client.player.dimensionId = packetDimensionId;
        }
        if (client.teleportGraceUntilMillis <= System.currentTimeMillis()) {
            double dx = x - client.player.x;
            double dy = y - client.player.y;
            double dz = z - client.player.z;
            if (dx * dx + dy * dy + dz * dz > 48.0 * 48.0) {
                throw new IOException("player moved too far");
            }
        }
        client.player.x = x;
        client.player.y = y;
        client.player.z = z;
        client.player.yaw = yaw;
        client.player.pitch = Math.max(-GameConfig.MAX_PITCH, Math.min(GameConfig.MAX_PITCH, pitch));
        client.selectedHotbarSlot = clampHotbarSlot(selectedHotbarSlot);
        client.heldItem = authoritativeHeldItem(client);
        mainThreadEvents.add(() -> client.worldUpdate(packetName));
    }

    private void processHostRequests(VoxelWorld world) {
        ChunkRequest chunkRequest;
        int chunks = 0;
        while (chunks < 64 && (chunkRequest = pendingChunkRequests.poll()) != null) {
            final ChunkRequest request = chunkRequest;
            ServerClient client = request.client;
            if (client.connection.open) {
                if (request.dimensionId != client.player.dimensionId) {
                    continue;
                }
                world.setActiveDimension(request.dimensionId);
                if (profile == null && dedicatedChunkLogBudget > 0) {
                    dedicatedChunkLogBudget--;
                    System.out.println("Chunk request: " + client.name + " " + request.chunkX + "," + request.chunkZ);
                    if (dedicatedChunkLogBudget == 0) {
                        System.out.println("Chunk request logging muted after initial burst.");
                    }
                }
                send(client.connection, MultiplayerProtocol.CHUNK_DATA, output -> world.writeNetworkColumn(request.chunkX, request.chunkZ, output));
            }
            chunks++;
        }

        BlockAction action;
        while ((action = pendingBlockActions.poll()) != null) {
            ServerClient actorClient = action.client;
            if (!actorClient.connection.open || actorClient.player.health <= 0.0 || actorClient.player.spectatorMode) {
                continue;
            }
            if (action.dimensionId != actorClient.player.dimensionId) {
                sendCommandFeedback(actorClient.uuid, "Rejected block action: wrong dimension.");
                continue;
            }
            world.setActiveDimension(actorClient.player.dimensionId);
            if (!isValidBlockAction(world, action)) {
                sendCommandFeedback(actorClient.uuid, "Rejected block action: too far or invalid target.");
                continue;
            }
            boolean changed = false;
            if (action.kind == MultiplayerProtocol.BLOCK_BREAK_START) {
                actorClient.selectedHotbarSlot = clampHotbarSlot(action.selectedHotbarSlot);
                actorClient.breakingDimensionId = action.dimensionId;
                actorClient.breakingX = action.x;
                actorClient.breakingY = action.y;
                actorClient.breakingZ = action.z;
                actorClient.breakingHeldItem = authoritativeHeldItem(actorClient);
                actorClient.breakingHotbarSlot = actorClient.selectedHotbarSlot;
                actorClient.breakingStartedMillis = System.currentTimeMillis();
                continue;
            }
            if (action.kind == MultiplayerProtocol.BLOCK_BREAK || action.kind == MultiplayerProtocol.BLOCK_BREAK_FINISH) {
                actorClient.selectedHotbarSlot = clampHotbarSlot(action.selectedHotbarSlot);
                byte authoritativeHeldItem = authoritativeHeldItem(actorClient);
                byte targetBlock = world.getBlock(action.x, action.y, action.z);
                if (!canStartBreakBlockServer(targetBlock, authoritativeHeldItem, actorClient.player.creativeMode)) {
                    sendCommandFeedback(actorClient.uuid, "Rejected block action: cannot break with selected item.");
                    continue;
                }
                if (!actorClient.player.creativeMode) {
                    double requiredMillis = breakDurationSeconds(targetBlock, authoritativeHeldItem, false) * 1000.0;
                    boolean started = matchesStoredBreak(actorClient, action, authoritativeHeldItem);
                    double elapsedMillis = started
                        ? System.currentTimeMillis() - actorClient.breakingStartedMillis
                        : requiredMillis;
                    if (elapsedMillis + 125.0 < requiredMillis) {
                        sendCommandFeedback(actorClient.uuid, "Rejected block action: block is not broken yet.");
                        continue;
                    }
                }
                changed = world.breakBlock(new RayHit(action.x, action.y, action.z, action.x, action.y, action.z));
                if (changed) {
                    byte droppedItem = droppedItemForBrokenBlock(targetBlock);
                    if (droppedItem != GameConfig.AIR && canHarvestBlock(targetBlock, authoritativeHeldItem)) {
                        world.spawnDroppedItem(droppedItem, 1, action.x + 0.5, action.y + 0.2, action.z + 0.5);
                    }
                    if (!actorClient.player.creativeMode && shouldDamageToolForBlock(authoritativeHeldItem, targetBlock)) {
                        actorClient.inventory.damageSelectedItem(actorClient.selectedHotbarSlot, 1);
                        sendInventorySync(actorClient);
                    }
                    clearStoredBreak(actorClient);
                }
            } else if (action.kind == MultiplayerProtocol.BLOCK_PLACE) {
                PlayerState actor = actorClient.player;
                actorClient.selectedHotbarSlot = clampHotbarSlot(action.selectedHotbarSlot);
                byte authoritativeHeldItem = authoritativeHeldItem(actorClient);
                if (authoritativeHeldItem != action.heldItem) {
                    sendCommandFeedback(actorClient.uuid, "Rejected block action: selected item mismatch.");
                    continue;
                }
                if (authoritativeHeldItem != GameConfig.AIR) {
                    changed = world.placeBlock(new RayHit(action.x, action.y, action.z, action.previousX, action.previousY, action.previousZ), authoritativeHeldItem, actor);
                    if (changed && !actor.creativeMode) {
                        consumeSelectedInventoryItem(actorClient.inventory, actorClient.selectedHotbarSlot, authoritativeHeldItem);
                        sendInventorySync(actorClient);
                    }
                }
            }
            if (changed) {
                if (profile == null) {
                    System.out.println("Block action: " + actorClient.name + " kind=" + action.kind
                        + " at " + action.x + "," + action.y + "," + action.z);
                }
                broadcastBlockNeighborhood(world, action.x, action.y, action.z);
                broadcastBlockNeighborhood(world, action.previousX, action.previousY, action.previousZ);
                closeInvalidatedContainers(world, action.x, action.y, action.z);
                closeInvalidatedContainers(world, action.previousX, action.previousY, action.previousZ);
            }
        }
    }

    private boolean isValidBlockAction(VoxelWorld world, BlockAction action) {
        if (world == null || action == null || !world.isInside(action.x, action.y, action.z)
            || !world.isBlockLoaded(action.x, action.y, action.z)) {
            return false;
        }
        if (action.kind == MultiplayerProtocol.BLOCK_PLACE && !world.isInside(action.previousX, action.previousY, action.previousZ)) {
            return false;
        }
        if ((action.kind == MultiplayerProtocol.BLOCK_BREAK
            || action.kind == MultiplayerProtocol.BLOCK_BREAK_START
            || action.kind == MultiplayerProtocol.BLOCK_BREAK_FINISH)
            && world.getBlock(action.x, action.y, action.z) == GameConfig.BEDROCK) {
            return false;
        }
        PlayerState player = action.client.player;
        double targetX = (action.kind == MultiplayerProtocol.BLOCK_PLACE ? action.previousX : action.x) + 0.5;
        double targetY = (action.kind == MultiplayerProtocol.BLOCK_PLACE ? action.previousY : action.y) + 0.5;
        double targetZ = (action.kind == MultiplayerProtocol.BLOCK_PLACE ? action.previousZ : action.z) + 0.5;
        double eyeX = player.x;
        double eyeY = player.y + player.eyeHeight();
        double eyeZ = player.z;
        double dx = targetX - eyeX;
        double dy = targetY - eyeY;
        double dz = targetZ - eyeZ;
        double maxReach = GameConfig.MAX_REACH + SERVER_REACH_TOLERANCE;
        if (dx * dx + dy * dy + dz * dz > maxReach * maxReach) {
            return false;
        }
        return action.kind == MultiplayerProtocol.BLOCK_BREAK
            || action.kind == MultiplayerProtocol.BLOCK_BREAK_START
            || action.kind == MultiplayerProtocol.BLOCK_BREAK_FINISH
            || action.kind == MultiplayerProtocol.BLOCK_PLACE;
    }

    private boolean matchesStoredBreak(ServerClient client, BlockAction action, byte authoritativeHeldItem) {
        return client.breakingStartedMillis > 0L
            && client.breakingDimensionId == action.dimensionId
            && client.breakingX == action.x
            && client.breakingY == action.y
            && client.breakingZ == action.z
            && client.breakingHotbarSlot == client.selectedHotbarSlot
            && client.breakingHeldItem == authoritativeHeldItem;
    }

    private void clearStoredBreak(ServerClient client) {
        client.breakingStartedMillis = 0L;
        client.breakingDimensionId = GameConfig.DIMENSION_OVERWORLD;
        client.breakingX = 0;
        client.breakingY = 0;
        client.breakingZ = 0;
        client.breakingHeldItem = GameConfig.AIR;
        client.breakingHotbarSlot = 0;
    }

    private int clampHotbarSlot(int selectedHotbarSlot) {
        if (selectedHotbarSlot < 0 || selectedHotbarSlot >= PlayerInventory.HOTBAR_SIZE) {
            return 0;
        }
        return selectedHotbarSlot;
    }

    private byte authoritativeHeldItem(ServerClient client) {
        if (client == null || client.inventory == null) {
            return GameConfig.AIR;
        }
        ItemStack stack = client.inventory.getHotbarStack(clampHotbarSlot(client.selectedHotbarSlot));
        return stack == null || stack.isEmpty() ? GameConfig.AIR : stack.itemId;
    }

    private void handleItemDrop(ServerClient client, DataInputStream input) throws IOException {
        int dimensionId = input.readInt();
        int selectedHotbarSlot = clampHotbarSlot(input.readInt());
        boolean dropStack = input.readBoolean();
        if (activeWorld == null || client == null || !client.connection.open
            || client.player.health <= 0.0 || client.player.spectatorMode) {
            return;
        }
        if (dimensionId != client.player.dimensionId) {
            sendCommandFeedback(client.uuid, "Rejected item drop: wrong dimension.");
            return;
        }
        client.selectedHotbarSlot = selectedHotbarSlot;
        ItemStack stack = client.inventory.getHotbarStack(selectedHotbarSlot);
        if (stack == null || stack.isEmpty()) {
            sendInventorySync(client);
            return;
        }
        int count = dropStack ? stack.count : 1;
        if (count <= 0) {
            return;
        }
        PlayerState player = client.player;
        double yaw = player.yaw;
        double pitch = player.pitch;
        double forwardX = Math.cos(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.sin(yaw) * Math.cos(pitch);
        double spawnX = player.x + forwardX * 0.72;
        double spawnY = player.y + player.eyeHeight() * 0.78;
        double spawnZ = player.z + forwardZ * 0.72;
        activeWorld.setActiveDimension(dimensionId);
        activeWorld.spawnThrownItem(stack.itemId, count, stack.durabilityDamage,
            spawnX, spawnY, spawnZ, forwardX * 4.2, forwardY * 4.2 + 1.0, forwardZ * 4.2);
        if (!player.creativeMode) {
            stack.count -= count;
            if (stack.count <= 0) {
                stack.clear();
            }
        }
        sendInventorySync(client);
        broadcastDroppedItemSnapshot(activeWorld);
    }

    private void repairUnsafeJoinPosition(VoxelWorld world, PlayerState player) {
        if (world == null || player == null || player.spectatorMode) {
            return;
        }
        if (world.isParadiseArea(player.x, player.z)) {
            double safeY = world.safeStandingYAt(player.x, player.z);
            if (player.y < GameConfig.WORLD_MIN_Y + 8.0 || player.y < safeY - 16.0 || player.y > safeY + 12.0) {
                player.y = safeY;
                player.verticalVelocity = 0.0;
            }
            return;
        }
        double distanceFromOriginSquared = player.x * player.x + player.z * player.z;
        if (distanceFromOriginSquared > 192.0 * 192.0) {
            world.placePlayerAtSpawn(player);
            player.verticalVelocity = 0.0;
            return;
        }
        double safeY = world.safeStandingYAt(player.x, player.z);
        if (player.y < GameConfig.WORLD_MIN_Y + 8.0 || player.y < safeY - 16.0 || player.y > safeY + 8.0) {
            player.y = safeY;
            player.verticalVelocity = 0.0;
        }
    }

    private boolean consumeSelectedInventoryItem(PlayerInventory inventory, int selectedHotbarSlot, byte itemId) {
        if (inventory == null || itemId == GameConfig.AIR) {
            return false;
        }
        return consumeOneFromStack(inventory.getHotbarStack(clampHotbarSlot(selectedHotbarSlot)), itemId);
    }

    private boolean consumeOneFromStack(ItemStack stack, byte itemId) {
        if (stack == null || stack.isEmpty() || stack.itemId != itemId) {
            return false;
        }
        stack.count--;
        if (stack.count <= 0) {
            stack.clear();
        }
        return true;
    }

    private void handleContainerOpenRequest(ServerClient client, DataInputStream input) throws IOException {
        int screenMode = input.readInt();
        int x = input.readInt();
        int y = input.readInt();
        int z = input.readInt();
        if (!client.connection.open || client.player.health <= 0.0) {
            sendCommandFeedback(client.uuid, "Cannot open container right now.");
            return;
        }
        VoxelWorld world = activeWorld;
        if (world == null || !isValidContainerTarget(world, client, screenMode, x, y, z)) {
            sendCommandFeedback(client.uuid, "Cannot open container.");
            return;
        }
        client.activeScreenMode = screenMode;
        client.activeContainerX = x;
        client.activeContainerY = y;
        client.activeContainerZ = z;
        client.activeWindowId++;
        if (client.activeWindowId <= 0) {
            client.activeWindowId = 1;
        }
        sendContainerOpen(client, world);
        sendInventorySync(client);
    }

    private boolean isValidContainerTarget(VoxelWorld world, ServerClient client, int screenMode, int x, int y, int z) {
        if (screenMode == GameConfig.INVENTORY_SCREEN_PLAYER) {
            return true;
        }
        if (client.player.spectatorMode || !world.isInside(x, y, z)) {
            return false;
        }
        if (!isWithinServerReach(client.player, x, y, z)) {
            return false;
        }
        return isContainerBlockAllowed(screenMode, world.getBlock(x, y, z));
    }

    private void handleContainerClick(ServerClient client, DataInputStream input) throws IOException {
        int windowId = input.readInt();
        int groupOrdinal = input.readInt();
        int slotIndex = input.readInt();
        boolean rightClick = input.readBoolean();
        boolean shiftDown = input.readBoolean();
        boolean middleClick = input.readBoolean();
        if (windowId != client.activeWindowId || client.activeWindowId <= 0) {
            sendCommandFeedback(client.uuid, "Container action rejected: stale window.");
            return;
        }
        InventorySlotGroup[] groups = InventorySlotGroup.values();
        if (groupOrdinal < 0 || groupOrdinal >= groups.length) {
            throw new IOException("invalid inventory slot group");
        }
        InventorySlotRef ref = new InventorySlotRef(groups[groupOrdinal], slotIndex);
        if (!isValidSlotRefForScreen(ref, client.activeScreenMode)) {
            throw new IOException("invalid inventory slot index");
        }
        VoxelWorld world = activeWorld;
        if (world == null || !isValidContainerTarget(world, client, client.activeScreenMode, client.activeContainerX, client.activeContainerY, client.activeContainerZ)) {
            sendCommandFeedback(client.uuid, "Container action rejected: unavailable container.");
            closeServerContainer(client);
            send(client.connection, MultiplayerProtocol.CONTAINER_CLOSE, output -> output.writeInt(windowId));
            return;
        }
        ContainerInventory chest = activeChestFor(client, world);
        FurnaceBlockEntity furnace = activeFurnaceFor(client, world);
        boolean creativeInventory = client.player.creativeMode && client.activeScreenMode == GameConfig.INVENTORY_SCREEN_PLAYER;
        if (client.inventory.handleClick(ref, creativeInventory, rightClick, shiftDown, middleClick, chest, furnace)) {
            sendInventorySync(client);
            sendContainerUpdate(client, world);
        }
    }

    static boolean isValidSlotRefForScreen(InventorySlotRef ref, int screenMode) {
        if (ref == null || ref.index < 0) {
            return false;
        }
        switch (ref.group) {
            case STORAGE:
                return ref.index < PlayerInventory.STORAGE_SIZE;
            case HOTBAR:
                return ref.index < PlayerInventory.HOTBAR_SIZE;
            case ARMOR:
                return ref.index < PlayerInventory.ARMOR_SIZE;
            case OFFHAND:
                return ref.index == 0;
            case CRAFT:
                return screenMode == GameConfig.INVENTORY_SCREEN_PLAYER && ref.index < PlayerInventory.CRAFT_SIZE;
            case CRAFT_RESULT:
                return screenMode == GameConfig.INVENTORY_SCREEN_PLAYER && ref.index == 0;
            case CRAFT_3X3:
                return screenMode == GameConfig.INVENTORY_SCREEN_WORKBENCH && ref.index < PlayerInventory.WORKBENCH_CRAFT_SIZE;
            case CRAFT_3X3_RESULT:
                return screenMode == GameConfig.INVENTORY_SCREEN_WORKBENCH && ref.index == 0;
            case CHEST_CONTAINER:
                return screenMode == GameConfig.INVENTORY_SCREEN_CHEST && ref.index < 27;
            case FURNACE_INPUT:
            case FURNACE_FUEL:
            case FURNACE_OUTPUT:
                return screenMode == GameConfig.INVENTORY_SCREEN_FURNACE && ref.index == 0;
            case CREATIVE:
                return screenMode == GameConfig.INVENTORY_SCREEN_PLAYER && ref.index < InventoryItems.CREATIVE_ITEMS.length;
            case TRASH:
                return ref.index == 0;
            default:
                return false;
        }
    }

    static boolean isContainerBlockAllowed(int screenMode, byte block) {
        if (screenMode == GameConfig.INVENTORY_SCREEN_CHEST) {
            return block == GameConfig.CHEST;
        }
        if (screenMode == GameConfig.INVENTORY_SCREEN_FURNACE) {
            return block == GameConfig.FURNACE;
        }
        if (screenMode == GameConfig.INVENTORY_SCREEN_WORKBENCH) {
            return block == GameConfig.CRAFTING_TABLE;
        }
        return screenMode == GameConfig.INVENTORY_SCREEN_PLAYER;
    }

    static boolean isWithinServerReach(PlayerState player, int x, int y, int z) {
        if (player == null) {
            return false;
        }
        double dx = x + 0.5 - player.x;
        double dy = y + 0.5 - (player.y + player.eyeHeight());
        double dz = z + 0.5 - player.z;
        double maxReach = GameConfig.MAX_REACH + SERVER_REACH_TOLERANCE;
        return dx * dx + dy * dy + dz * dz <= maxReach * maxReach;
    }

    private ContainerInventory activeChestFor(ServerClient client, VoxelWorld world) {
        if (client.activeScreenMode != GameConfig.INVENTORY_SCREEN_CHEST) {
            return null;
        }
        return world.chestContainerAt(client.activeContainerX, client.activeContainerY, client.activeContainerZ);
    }

    private FurnaceBlockEntity activeFurnaceFor(ServerClient client, VoxelWorld world) {
        if (client.activeScreenMode != GameConfig.INVENTORY_SCREEN_FURNACE) {
            return null;
        }
        return world.furnaceAt(client.activeContainerX, client.activeContainerY, client.activeContainerZ);
    }

    private void closeServerContainer(ServerClient client) {
        client.inventory.returnTransientCraftingToInventory();
        ItemStack cursor = client.inventory.getCursorStack();
        if (cursor != null && !cursor.isEmpty()) {
            client.inventory.addItem(cursor.itemId, cursor.count, cursor.durabilityDamage);
            cursor.clear();
        }
        client.activeWindowId = 0;
        client.activeScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
        client.activeContainerX = 0;
        client.activeContainerY = 0;
        client.activeContainerZ = 0;
        sendInventorySync(client);
    }

    private void closeInvalidatedContainers(VoxelWorld world, int x, int y, int z) {
        if (world == null) {
            return;
        }
        for (ServerClient client : serverClients.values()) {
            if (client.activeWindowId <= 0
                || client.activeContainerX != x
                || client.activeContainerY != y
                || client.activeContainerZ != z) {
                continue;
            }
            if (isValidContainerTarget(world, client, client.activeScreenMode, x, y, z)) {
                continue;
            }
            int windowId = client.activeWindowId;
            closeServerContainer(client);
            send(client.connection, MultiplayerProtocol.CONTAINER_CLOSE, output -> output.writeInt(windowId));
            sendCommandFeedback(client.uuid, "Container closed: block changed.");
        }
    }

    private void sendInventorySync(ServerClient client) {
        if (client == null || !client.connection.open) {
            return;
        }
        send(client.connection, MultiplayerProtocol.INVENTORY_SYNC, output -> client.inventory.writeTo(output));
    }

    private void sendContainerOpen(ServerClient client, VoxelWorld world) {
        sendContainerState(client, world, MultiplayerProtocol.CONTAINER_OPEN);
    }

    private void sendContainerUpdate(ServerClient client, VoxelWorld world) {
        sendContainerState(client, world, MultiplayerProtocol.CONTAINER_UPDATE);
    }

    private void sendContainerState(ServerClient client, VoxelWorld world, byte packetType) {
        if (client == null || world == null || !client.connection.open) {
            return;
        }
        send(client.connection, packetType, output -> {
            output.writeInt(client.activeWindowId);
            output.writeInt(client.activeScreenMode);
            output.writeInt(client.activeContainerX);
            output.writeInt(client.activeContainerY);
            output.writeInt(client.activeContainerZ);
            if (client.activeScreenMode == GameConfig.INVENTORY_SCREEN_CHEST) {
                output.writeBoolean(true);
                world.chestContainerAt(client.activeContainerX, client.activeContainerY, client.activeContainerZ).writeTo(output);
            } else {
                output.writeBoolean(false);
            }
            if (client.activeScreenMode == GameConfig.INVENTORY_SCREEN_FURNACE) {
                output.writeBoolean(true);
                world.furnaceAt(client.activeContainerX, client.activeContainerY, client.activeContainerZ).writeTo(output);
            } else {
                output.writeBoolean(false);
            }
        });
    }

    private void sendOpenFurnaceUpdates(VoxelWorld world, double deltaTime) {
        if (world == null || serverClients.isEmpty()) {
            return;
        }
        containerUpdateTimer += deltaTime;
        if (containerUpdateTimer < 0.10) {
            return;
        }
        containerUpdateTimer = 0.0;
        for (ServerClient client : serverClients.values()) {
            if (!client.connection.open
                || client.activeWindowId <= 0
                || client.activeScreenMode != GameConfig.INVENTORY_SCREEN_FURNACE) {
                continue;
            }
            world.setActiveDimensionFor(client.player);
            if (!isValidContainerTarget(world, client, client.activeScreenMode, client.activeContainerX, client.activeContainerY, client.activeContainerZ)) {
                continue;
            }
            sendContainerUpdate(client, world);
        }
    }

    private void clientConnectLoop(String host, int port) {
        String disconnectReason = "Connection Lost";
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            Connection connection = new Connection(socket);
            clientConnection = connection;
            send(connection, MultiplayerProtocol.HELLO, output -> {
                output.writeInt(MultiplayerProtocol.MAGIC);
                output.writeInt(MultiplayerProtocol.VERSION);
                MultiplayerProtocol.writeUuid(output, profile.uuid);
                output.writeUTF(profile.name);
            });
            while (clientRunning && connection.open) {
                MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(connection.input);
                if (packet == null) {
                    disconnectReason = "Connection Lost";
                    break;
                }
                handleClientPacket(packet);
            }
        } catch (IOException exception) {
            if (clientRunning) {
                disconnectReason = exception.getMessage() == null ? "Connection Lost" : "Connection Lost: " + exception.getMessage();
                setStatus(disconnectReason);
                queueClientDisconnected(disconnectReason);
            }
        } finally {
            if (clientRunning) {
                queueClientDisconnected(disconnectReason);
            }
            clientRunning = false;
            closeQuietly(clientConnection);
            clientConnection = null;
            setStatus("Offline");
        }
    }

    private void handleClientPacket(MultiplayerProtocol.Packet packet) throws IOException {
        DataInputStream input = packet.input;
        if (packet.type == MultiplayerProtocol.WELCOME) {
            long seed = input.readLong();
            TerrainPreset preset = TerrainPreset.fromMetadata(input.readUTF());
            int dimensionId = input.readInt();
            double x = input.readDouble();
            double y = input.readDouble();
            double z = input.readDouble();
            double worldTime = input.readDouble();
            mainThreadEvents.add(() -> listener.onClientWelcome(seed, preset, dimensionId, x, y, z, worldTime));
            setStatus("Connected");
        } else if (packet.type == MultiplayerProtocol.PLAYER_STATE) {
            UUID uuid = MultiplayerProtocol.readUuid(input);
            String name = input.readUTF();
            int dimensionId = input.readInt();
            double x = input.readDouble();
            double y = input.readDouble();
            double z = input.readDouble();
            double yaw = input.readDouble();
            double pitch = input.readDouble();
            byte heldItem = input.readByte();
            boolean sneaking = input.readBoolean();
            boolean spectator = input.readBoolean();
            double health = input.readDouble();
            if (!profile.uuid.equals(uuid) && dimensionId == clientDimensionId) {
                mainThreadEvents.add(() -> {
                    VoxelWorld world = activeWorld;
                    if (world != null) {
                        world.updateRemotePlayer(uuid, name, x, y, z, yaw, pitch, heldItem, sneaking, spectator);
                        RemotePlayerState remote = null;
                        for (RemotePlayerState candidate : world.getRemotePlayers()) {
                            if (candidate.uuid.equals(uuid)) {
                                remote = candidate;
                                break;
                            }
                        }
                        if (remote != null) {
                            remote.health = health;
                        }
                    }
                });
            }
        } else if (packet.type == MultiplayerProtocol.PLAYER_DESPAWN) {
            UUID uuid = MultiplayerProtocol.readUuid(input);
            mainThreadEvents.add(() -> {
                VoxelWorld world = activeWorld;
                if (world != null) {
                    world.removeRemotePlayer(uuid);
                }
            });
        } else if (packet.type == MultiplayerProtocol.CHAT) {
            String message = input.readUTF();
            emitChat(message);
        } else if (packet.type == MultiplayerProtocol.CHUNK_DATA) {
            byte[] remaining = readRemaining(input);
            DataInputStream headerInput = new DataInputStream(new ByteArrayInputStream(remaining));
            headerInput.readInt();
            headerInput.readInt();
            int packetDimensionId = headerInput.readInt();
            mainThreadEvents.add(() -> {
                try {
                    VoxelWorld world = activeWorld;
                    if (world != null && packetDimensionId == clientDimensionId) {
                        long key = world.readNetworkColumn(new DataInputStream(new ByteArrayInputStream(remaining)));
                        listener.onClientChunkColumn(unpackColumnX(key), unpackColumnZ(key));
                    }
                } catch (IOException exception) {
                    setStatus("Chunk sync failed: " + exception.getMessage());
                }
            });
        } else if (packet.type == MultiplayerProtocol.BLOCK_UPDATE) {
            int dimensionId = input.readInt();
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            String id = input.readUTF();
            int distance = input.readByte();
            mainThreadEvents.add(() -> {
                VoxelWorld world = activeWorld;
                if (world != null && dimensionId == clientDimensionId) {
                    world.setActiveDimension(dimensionId);
                    world.applyNetworkBlockState(x, y, z, Blocks.stateFromNamespacedId(id), distance);
                    listener.onClientBlockUpdate(x, y, z);
                }
            });
        } else if (packet.type == MultiplayerProtocol.WORLD_TIME) {
            double worldTime = input.readDouble();
            mainThreadEvents.add(() -> {
                VoxelWorld world = activeWorld;
                if (world != null) {
                    world.setWorldTime(worldTime);
                }
            });
        } else if (packet.type == MultiplayerProtocol.MOB_SNAPSHOT) {
            byte[] remaining = readRemaining(input);
            mainThreadEvents.add(() -> applyMobSnapshot(remaining));
        } else if (packet.type == MultiplayerProtocol.DROPPED_ITEM_SNAPSHOT) {
            byte[] remaining = readRemaining(input);
            mainThreadEvents.add(() -> applyDroppedItemSnapshot(remaining));
        } else if (packet.type == MultiplayerProtocol.DISCONNECT) {
            String reason = input.readUTF();
            setStatus("Disconnected: " + reason);
            queueClientDisconnected(reason == null || reason.trim().isEmpty() ? "Connection Lost" : reason);
            clientRunning = false;
        } else if (packet.type == MultiplayerProtocol.PLAYER_HEALTH) {
            double health = input.readDouble();
            mainThreadEvents.add(() -> listener.onClientHealth(health));
        } else if (packet.type == MultiplayerProtocol.INVENTORY_ADD) {
            byte itemId = input.readByte();
            int count = input.readInt();
            int durabilityDamage = input.readInt();
            mainThreadEvents.add(() -> listener.onClientInventoryAdd(itemId, count, durabilityDamage));
        } else if (packet.type == MultiplayerProtocol.SERVER_PLAYER_STATE) {
            int dimensionId = input.readInt();
            double x = input.readDouble();
            double y = input.readDouble();
            double z = input.readDouble();
            double yaw = input.readDouble();
            double pitch = input.readDouble();
            boolean creativeMode = input.readBoolean();
            boolean spectatorMode = input.readBoolean();
            double health = input.readDouble();
            mainThreadEvents.add(() -> listener.onClientServerPlayerState(dimensionId, x, y, z, yaw, pitch, creativeMode, spectatorMode, health));
        } else if (packet.type == MultiplayerProtocol.INVENTORY_SYNC) {
            PlayerInventory snapshot = new PlayerInventory();
            snapshot.readFrom(input);
            mainThreadEvents.add(() -> listener.onClientInventorySync(snapshot));
        } else if (packet.type == MultiplayerProtocol.CONTAINER_OPEN || packet.type == MultiplayerProtocol.CONTAINER_UPDATE) {
            ContainerState state = readContainerState(input);
            clientWindowId = state.windowId;
            if (packet.type == MultiplayerProtocol.CONTAINER_OPEN) {
                mainThreadEvents.add(() -> listener.onClientContainerOpen(state.screenMode, state.x, state.y, state.z, state.windowId, state.chest, state.furnace));
            } else {
                mainThreadEvents.add(() -> listener.onClientContainerUpdate(state.screenMode, state.x, state.y, state.z, state.windowId, state.chest, state.furnace));
            }
        } else if (packet.type == MultiplayerProtocol.CONTAINER_CLOSE) {
            int windowId = input.readInt();
            if (clientWindowId == windowId) {
                clientWindowId = 0;
            }
            mainThreadEvents.add(() -> listener.onClientContainerClose(windowId));
        } else if (packet.type == MultiplayerProtocol.PING) {
            long timestamp = input.readLong();
            send(clientConnection, MultiplayerProtocol.PONG, output -> output.writeLong(timestamp));
        } else if (packet.type == MultiplayerProtocol.PONG) {
            long timestamp = input.readLong();
            if (pendingClientPingTime == timestamp) {
                int ping = (int) Math.max(0L, Math.min(9999L, System.currentTimeMillis() - timestamp));
                pendingClientPingTime = -1L;
                lastClientPongMillis = System.currentTimeMillis();
                clientSession.setPingMs(ping);
            }
        } else if (packet.type == MultiplayerProtocol.PLAYER_LIST) {
            int count = input.readInt();
            ArrayList<PlayerListEntry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                UUID uuid = MultiplayerProtocol.readUuid(input);
                String name = input.readUTF();
                int pingMs = input.readInt();
                double health = input.readDouble();
                String gameMode = input.readUTF();
                boolean connected = input.readBoolean();
                if (profile != null && profile.uuid.equals(uuid) && clientSession.pingMs() >= 0) {
                    pingMs = clientSession.pingMs();
                }
                entries.add(new PlayerListEntry(uuid, name, pingMs, health, gameMode, connected));
            }
            mainThreadEvents.add(() -> {
                playerListSnapshot.clear();
                playerListSnapshot.addAll(entries);
                clientSession.replacePlayerList(entries);
            });
        }
    }

    private ContainerState readContainerState(DataInputStream input) throws IOException {
        int windowId = input.readInt();
        int screenMode = input.readInt();
        int x = input.readInt();
        int y = input.readInt();
        int z = input.readInt();
        ContainerInventory chest = null;
        FurnaceBlockEntity furnace = null;
        if (input.readBoolean()) {
            chest = ContainerInventory.readFrom(input, 54);
        }
        if (input.readBoolean()) {
            furnace = new FurnaceBlockEntity();
            furnace.readFrom(input);
        }
        return new ContainerState(windowId, screenMode, x, y, z, chest, furnace);
    }

    private void queueClientDisconnected(String reason) {
        if (clientDisconnectEventQueued) {
            return;
        }
        clientDisconnectEventQueued = true;
        String message = reason == null || reason.trim().isEmpty() ? "Connection Lost" : reason;
        mainThreadEvents.add(() -> listener.onClientDisconnected(message));
    }

    private void queueClientChunkRequests(PlayerState player, int radius) {
        int playerChunkX = Math.floorDiv((int) Math.floor(player.x), GameConfig.CHUNK_SIZE);
        int playerChunkZ = Math.floorDiv((int) Math.floor(player.z), GameConfig.CHUNK_SIZE);
        queueClientChunkRequests(player.dimensionId, playerChunkX, playerChunkZ, radius);
    }

    private void queueClientChunkRequests(int dimensionId, int playerChunkX, int playerChunkZ, int radius) {
        int requestRadius = Math.max(2, Math.min(radius, 8));
        for (int dz = -requestRadius; dz <= requestRadius; dz++) {
            for (int dx = -requestRadius; dx <= requestRadius; dx++) {
                long key = columnKey(dimensionId, playerChunkX + dx, playerChunkZ + dz);
                if (requestedClientColumns.add(key)) {
                    clientColumnQueue.addLast(key);
                }
            }
        }
    }

    private void sendPlayerState(Connection connection, UUID uuid, String name, PlayerState player, byte heldItem, int selectedHotbarSlot) {
        send(connection, MultiplayerProtocol.PLAYER_STATE, output -> {
            MultiplayerProtocol.writeUuid(output, uuid);
            output.writeUTF(name);
            writePlayerFields(output, player, heldItem);
            output.writeInt(selectedHotbarSlot);
        });
    }

    private void sendClientPing() {
        if (clientConnection == null || !clientConnection.open || pendingClientPingTime >= 0L) {
            return;
        }
        pendingClientPingTime = System.currentTimeMillis();
        send(clientConnection, MultiplayerProtocol.PING, output -> output.writeLong(pendingClientPingTime));
    }

    private void pingServerClients() {
        long now = System.currentTimeMillis();
        for (ServerClient client : serverClients.values()) {
            if (client.pendingPingTime >= 0L && now - client.pendingPingTime > (long) (PING_TIMEOUT_SECONDS * 1000.0)) {
                client.pingMs = -1;
                client.pendingPingTime = -1L;
            }
            if (client.pendingPingTime < 0L && client.connection.open) {
                client.pendingPingTime = now;
                send(client.connection, MultiplayerProtocol.PING, output -> output.writeLong(now));
            }
        }
    }

    private void broadcastPlayerList(PlayerState hostPlayer, byte hostHeldItem) {
        ArrayList<PlayerListEntry> entries = buildPlayerList(hostPlayer, hostHeldItem);
        playerListSnapshot.clear();
        playerListSnapshot.addAll(entries);
        for (ServerClient client : serverClients.values()) {
            send(client.connection, MultiplayerProtocol.PLAYER_LIST, output -> writePlayerList(output, entries));
        }
    }

    private ArrayList<PlayerListEntry> buildPlayerList(PlayerState hostPlayer, byte hostHeldItem) {
        ArrayList<PlayerListEntry> entries = new ArrayList<>();
        if (profile != null && hostPlayer != null) {
            entries.add(new PlayerListEntry(profile.uuid, profile.name, 0, hostPlayer.health, gameModeName(hostPlayer), true));
        }
        for (ServerClient client : serverClients.values()) {
            entries.add(new PlayerListEntry(client.uuid, client.name, client.pingMs, client.player.health, gameModeName(client.player), client.connection.open));
        }
        Collections.sort(entries, new Comparator<PlayerListEntry>() {
            @Override
            public int compare(PlayerListEntry a, PlayerListEntry b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return entries;
    }

    private void writePlayerList(DataOutputStream output, List<PlayerListEntry> entries) throws IOException {
        output.writeInt(entries.size());
        for (PlayerListEntry entry : entries) {
            MultiplayerProtocol.writeUuid(output, entry.uuid);
            output.writeUTF(entry.name);
            output.writeInt(entry.pingMs);
            output.writeDouble(entry.health);
            output.writeUTF(entry.gameMode);
            output.writeBoolean(entry.connected);
        }
    }

    private String gameModeName(PlayerState player) {
        if (player == null) {
            return "survival";
        }
        if (player.spectatorMode) {
            return "spectator";
        }
        if (player.creativeMode) {
            return "creative";
        }
        return "survival";
    }

    private void broadcastPlayerState(UUID uuid, String name, PlayerState player, byte heldItem) {
        for (ServerClient client : serverClients.values()) {
            if (client.player.dimensionId != player.dimensionId) {
                send(client.connection, MultiplayerProtocol.PLAYER_DESPAWN, output -> MultiplayerProtocol.writeUuid(output, uuid));
                continue;
            }
            send(client.connection, MultiplayerProtocol.PLAYER_STATE, output -> {
                MultiplayerProtocol.writeUuid(output, uuid);
                output.writeUTF(name);
                writePlayerFields(output, player, heldItem);
            });
        }
    }

    private void writePlayerFields(DataOutputStream output, PlayerState player, byte heldItem) throws IOException {
        output.writeInt(player.dimensionId);
        output.writeDouble(player.x);
        output.writeDouble(player.y);
        output.writeDouble(player.z);
        output.writeDouble(player.yaw);
        output.writeDouble(player.pitch);
        output.writeByte(heldItem);
        output.writeBoolean(player.sneaking);
        output.writeBoolean(player.spectatorMode);
        output.writeDouble(player.health);
    }

    private void applyPlayerAttack(UUID attackerUuid, UUID targetUuid, int damage) {
        if (!hostRunning || attackerUuid == null || targetUuid == null || damage <= 0) {
            return;
        }
        PlayerState attacker = profile != null && profile.uuid.equals(attackerUuid)
            ? activeHostPlayer
            : serverClients.containsKey(attackerUuid) ? serverClients.get(attackerUuid).player : null;
        PlayerState target = profile != null && profile.uuid.equals(targetUuid)
            ? activeHostPlayer
            : serverClients.containsKey(targetUuid) ? serverClients.get(targetUuid).player : null;
        if (!allowPvp && serverClients.containsKey(targetUuid)) {
            return;
        }
        if (attacker == null || target == null || target.spectatorMode || target.health <= 0.0) {
            return;
        }
        double dx = target.x - attacker.x;
        double dy = (target.y + target.height() * 0.5) - (attacker.y + attacker.eyeHeight());
        double dz = target.z - attacker.z;
        if (dx * dx + dy * dy + dz * dz > 3.8 * 3.8) {
            return;
        }
        double protectedDamage = Math.max(0.5, damage);
        target.health = Math.max(0.0, target.health - protectedDamage);
        if (profile == null || !profile.uuid.equals(targetUuid)) {
            ServerClient targetClient = serverClients.get(targetUuid);
            if (targetClient != null) {
                send(targetClient.connection, MultiplayerProtocol.PLAYER_HEALTH, output -> output.writeDouble(target.health));
            }
        }
        if (profile != null && profile.uuid.equals(targetUuid)) {
            broadcastPlayerState(targetUuid, profile.name, target, GameConfig.AIR);
        } else {
            ServerClient targetClient = serverClients.get(targetUuid);
            if (targetClient != null) {
                broadcastPlayerState(targetUuid, targetClient.name, target, targetClient.heldItem);
            }
        }
    }

    private void applyMobAttack(ServerClient client, int damage, double knockback) {
        VoxelWorld world = activeWorld;
        if (!hostRunning || world == null || client == null || !client.connection.open || damage <= 0) {
            return;
        }
        if (world.attackMobInReach(client.player, damage, knockback)) {
            broadcastMobSnapshot(world);
        }
    }

    private void broadcastChat(String message) {
        for (ServerClient client : serverClients.values()) {
            send(client.connection, MultiplayerProtocol.CHAT, output -> output.writeUTF(message));
        }
    }

    private void broadcastDespawn(UUID uuid) {
        for (ServerClient client : serverClients.values()) {
            send(client.connection, MultiplayerProtocol.PLAYER_DESPAWN, output -> MultiplayerProtocol.writeUuid(output, uuid));
        }
    }

    private void broadcastWorldTime(double worldTime) {
        for (ServerClient client : serverClients.values()) {
            send(client.connection, MultiplayerProtocol.WORLD_TIME, output -> output.writeDouble(worldTime));
        }
    }

    private void broadcastBlockState(VoxelWorld world, int x, int y, int z) {
        if (!world.isInside(x, y, z)) {
            return;
        }
        BlockState state = world.getBlockState(x, y, z);
        int distance = world.getFluidDistance(x, y, z);
        int dimensionId = world.activeDimensionId();
        for (ServerClient client : serverClients.values()) {
            if (client.player.dimensionId != dimensionId) {
                continue;
            }
            send(client.connection, MultiplayerProtocol.BLOCK_UPDATE, output -> {
                output.writeInt(dimensionId);
                output.writeInt(x);
                output.writeInt(y);
                output.writeInt(z);
                output.writeUTF(Blocks.serializedId(state));
                output.writeByte(distance);
            });
        }
    }

    private void broadcastMobSnapshot(VoxelWorld world) {
        List<MobEntity> mobs = world.getMobs();
        for (ServerClient client : serverClients.values()) {
            send(client.connection, MultiplayerProtocol.MOB_SNAPSHOT, output -> {
                output.writeInt(Math.min(mobs.size(), 128));
                for (int i = 0; i < mobs.size() && i < 128; i++) {
                    MobEntity mob = mobs.get(i);
                    output.writeByte(mob.kind.ordinal());
                    output.writeDouble(mob.x);
                    output.writeDouble(mob.y);
                    output.writeDouble(mob.z);
                    output.writeDouble(mob.bodyYaw);
                    output.writeDouble(mob.health);
                    output.writeDouble(mob.babyAge);
                }
            });
        }
    }

    private void broadcastDroppedItemSnapshot(VoxelWorld world) {
        List<DroppedItem> items = world.getDroppedItems();
        for (ServerClient client : serverClients.values()) {
            send(client.connection, MultiplayerProtocol.DROPPED_ITEM_SNAPSHOT, output -> {
                output.writeInt(Math.min(items.size(), 128));
                for (int i = 0; i < items.size() && i < 128; i++) {
                    DroppedItem item = items.get(i);
                    output.writeByte(item.itemId);
                    output.writeInt(item.count);
                    output.writeInt(item.durabilityDamage);
                    output.writeDouble(item.x);
                    output.writeDouble(item.y);
                    output.writeDouble(item.z);
                }
            });
        }
    }

    private void processRemoteClientItemPickups(VoxelWorld world) {
        if (world == null || serverClients.isEmpty()) {
            return;
        }
        List<DroppedItem> items = world.getDroppedItems();
        double pickupDistance = GameConfig.DROPPED_ITEM_PICKUP_RADIUS;
        double pickupDistanceSquared = pickupDistance * pickupDistance;
        for (int i = items.size() - 1; i >= 0; i--) {
            DroppedItem item = items.get(i);
            if (item.pickupDelaySeconds > 0.0 || item.count <= 0) {
                continue;
            }
            for (ServerClient client : serverClients.values()) {
                if (!client.connection.open || client.player.health <= 0.0) {
                    continue;
                }
                double dx = item.x - client.player.x;
                double dy = (item.y + item.height() * 0.5) - (client.player.y + GameConfig.PLAYER_HEIGHT * 0.5);
                double dz = item.z - client.player.z;
                if (dx * dx + dy * dy + dz * dz <= pickupDistanceSquared) {
                    final byte itemId = item.itemId;
                    final int count = item.count;
                    final int durabilityDamage = item.durabilityDamage;
                    if (client.inventory.addItem(itemId, count, durabilityDamage)) {
                        send(client.connection, MultiplayerProtocol.INVENTORY_ADD, output -> {
                            output.writeByte(itemId);
                            output.writeInt(count);
                            output.writeInt(durabilityDamage);
                        });
                        sendInventorySync(client);
                        items.remove(i);
                    }
                    break;
                }
            }
        }
    }

    private void processParadisePortalTravel(VoxelWorld world, double deltaTime) {
        if (world == null || serverClients.isEmpty()) {
            return;
        }
        for (ServerClient client : serverClients.values()) {
            if (!client.connection.open || client.player.health <= 0.0) {
                continue;
            }
            world.setActiveDimensionFor(client.player);
            if (!world.updateParadisePortalTravel(client.player, deltaTime)) {
                continue;
            }
            if (client.activeWindowId > 0) {
                int windowId = client.activeWindowId;
                closeServerContainer(client);
                send(client.connection, MultiplayerProtocol.CONTAINER_CLOSE, output -> output.writeInt(windowId));
            }
            client.teleportGraceUntilMillis = System.currentTimeMillis() + 1500L;
            sendServerPlayerState(client);
            broadcastPlayerState(client.uuid, client.name, client.player, client.heldItem);
        }
    }

    private byte droppedItemForBrokenBlock(byte block) {
        if (!InventoryItems.isCollectible(block)
            || block == GameConfig.OAK_LEAVES
            || block == GameConfig.PINE_LEAVES
            || block == GameConfig.BIRCH_LEAVES
            || block == GameConfig.PARADISE_PORTAL
            || GameConfig.isLiquidBlock(block)) {
            return GameConfig.AIR;
        }
        if (block == GameConfig.COAL_ORE || block == GameConfig.DEEPSLATE_COAL_ORE) {
            return InventoryItems.COAL_ITEM;
        }
        if (block == GameConfig.DIAMOND_ORE || block == GameConfig.DEEPSLATE_DIAMOND_ORE) {
            return InventoryItems.DIAMOND_ITEM;
        }
        if (block == GameConfig.STONE) {
            return GameConfig.COBBLESTONE;
        }
        return block;
    }

    static boolean canBreakBlockServer(byte block, byte heldItem, boolean creativeMode) {
        return canStartBreakBlockServer(block, heldItem, creativeMode) && (creativeMode || canHarvestBlock(block, heldItem));
    }

    static boolean canStartBreakBlockServer(byte block, byte heldItem, boolean creativeMode) {
        if (block == GameConfig.AIR
            || block == GameConfig.BEDROCK
            || (GameConfig.isLiquidBlock(block) && block != GameConfig.SEAGRASS && block != GameConfig.KELP)) {
            return false;
        }
        if (block == GameConfig.OBSIDIAN) {
            return creativeMode || pickaxeTier(heldItem) >= 4;
        }
        return true;
    }

    static double breakDurationSeconds(byte block, byte heldItem, boolean creativeMode) {
        if (creativeMode || isInstantBreakBlock(block)) {
            return 0.0;
        }
        double baseDuration;
        switch (block) {
            case GameConfig.GRASS:
            case GameConfig.DIRT:
            case GameConfig.FARMLAND:
                baseDuration = 0.75;
                break;
            case GameConfig.SAND:
            case GameConfig.GRAVEL:
            case GameConfig.CLAY:
                baseDuration = 0.85;
                break;
            case GameConfig.OAK_LOG:
            case GameConfig.PINE_LOG:
            case GameConfig.BIRCH_LOG:
            case InventoryItems.OAK_PLANKS:
            case GameConfig.PINE_PLANKS:
            case GameConfig.BIRCH_PLANKS:
            case GameConfig.CHEST:
            case GameConfig.CRAFTING_TABLE:
                baseDuration = 1.25;
                break;
            case GameConfig.COBBLESTONE:
            case GameConfig.STONE:
                baseDuration = 1.35;
                break;
            case GameConfig.DEEPSLATE:
                baseDuration = 1.9;
                break;
            case GameConfig.COAL_ORE:
            case GameConfig.IRON_ORE:
            case GameConfig.DIAMOND_ORE:
                baseDuration = 1.8;
                break;
            case GameConfig.DEEPSLATE_COAL_ORE:
            case GameConfig.DEEPSLATE_IRON_ORE:
            case GameConfig.DEEPSLATE_DIAMOND_ORE:
                baseDuration = 2.25;
                break;
            case GameConfig.OBSIDIAN:
                baseDuration = 8.5;
                break;
            default:
                baseDuration = 1.4;
                break;
        }
        return Math.max(0.05, baseDuration / breakSpeedMultiplier(heldItem, block));
    }

    private static boolean isInstantBreakBlock(byte block) {
        return block == GameConfig.TORCH
            || block == GameConfig.WHEAT_CROP
            || block == GameConfig.CARROT_CROP
            || block == GameConfig.POTATO_CROP
            || block == GameConfig.TALL_GRASS
            || block == GameConfig.SEAGRASS
            || block == GameConfig.KELP
            || block == GameConfig.RED_FLOWER
            || block == GameConfig.YELLOW_FLOWER
            || block == GameConfig.RAIL;
    }

    private static double breakSpeedMultiplier(byte heldItem, byte block) {
        if (isPickaxe(heldItem) && isStoneHarvestBlock(block)) {
            switch (heldItem) {
                case InventoryItems.NETHERITE_PICKAXE: return 7.0;
                case InventoryItems.DIAMOND_PICKAXE: return 6.2;
                case InventoryItems.IRON_PICKAXE: return 5.2;
                case InventoryItems.STONE_PICKAXE: return 3.8;
                case InventoryItems.WOODEN_PICKAXE: return 2.4;
                default: return 1.0;
            }
        }
        if (isShovel(heldItem) && isDirtLikeBlock(block)) {
            switch (heldItem) {
                case InventoryItems.NETHERITE_SHOVEL: return 6.0;
                case InventoryItems.DIAMOND_SHOVEL: return 5.0;
                case InventoryItems.IRON_SHOVEL: return 4.2;
                case InventoryItems.STONE_SHOVEL: return 3.0;
                case InventoryItems.WOODEN_SHOVEL: return 1.8;
                default: return 1.0;
            }
        }
        if (isAxe(heldItem) && isWoodLikeBlock(block)) {
            switch (heldItem) {
                case InventoryItems.NETHERITE_AXE: return 6.0;
                case InventoryItems.DIAMOND_AXE: return 5.0;
                case InventoryItems.IRON_AXE: return 4.2;
                case InventoryItems.STONE_AXE: return 3.0;
                case InventoryItems.WOODEN_AXE: return 1.8;
                default: return 1.0;
            }
        }
        return 1.0;
    }

    private static boolean shouldDamageToolForBlock(byte heldItem, byte block) {
        return InventoryItems.isDurableItem(heldItem)
            && ((isPickaxe(heldItem) && isStoneHarvestBlock(block))
                || (isShovel(heldItem) && isDirtLikeBlock(block))
                || (isAxe(heldItem) && isWoodLikeBlock(block)));
    }

    private static boolean canHarvestBlock(byte block, byte heldItem) {
        if (block == GameConfig.OBSIDIAN) {
            return pickaxeTier(heldItem) >= 4;
        }
        if (block == GameConfig.DIAMOND_ORE || block == GameConfig.DEEPSLATE_DIAMOND_ORE) {
            return pickaxeTier(heldItem) >= 3;
        }
        if (block == GameConfig.IRON_ORE || block == GameConfig.DEEPSLATE_IRON_ORE) {
            return pickaxeTier(heldItem) >= 2;
        }
        if (isStoneHarvestBlock(block)) {
            return pickaxeTier(heldItem) >= 1;
        }
        return true;
    }

    private static boolean isStoneHarvestBlock(byte block) {
        return block == GameConfig.STONE
            || block == GameConfig.COBBLESTONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.COAL_ORE
            || block == GameConfig.DEEPSLATE_COAL_ORE
            || block == GameConfig.IRON_ORE
            || block == GameConfig.DEEPSLATE_IRON_ORE
            || block == GameConfig.DIAMOND_ORE
            || block == GameConfig.DEEPSLATE_DIAMOND_ORE
            || block == GameConfig.OBSIDIAN;
    }

    private static boolean isDirtLikeBlock(byte block) {
        return block == GameConfig.GRASS
            || block == GameConfig.DIRT
            || block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.CLAY
            || block == GameConfig.FARMLAND
            || block == GameConfig.SNOW_BLOCK
            || block == GameConfig.SNOW_LAYER;
    }

    private static boolean isWoodLikeBlock(byte block) {
        return block == GameConfig.OAK_LOG
            || block == GameConfig.PINE_LOG
            || block == GameConfig.BIRCH_LOG
            || block == InventoryItems.OAK_PLANKS
            || block == GameConfig.PINE_PLANKS
            || block == GameConfig.BIRCH_PLANKS
            || block == GameConfig.OAK_STAIRS
            || block == GameConfig.PINE_STAIRS
            || block == GameConfig.BIRCH_STAIRS
            || block == GameConfig.CHEST
            || block == GameConfig.CRAFTING_TABLE
            || block == GameConfig.OAK_FENCE
            || block == GameConfig.OAK_FENCE_GATE
            || block == GameConfig.OAK_DOOR;
    }

    private static boolean isPickaxe(byte item) {
        return item == InventoryItems.WOODEN_PICKAXE || item == InventoryItems.STONE_PICKAXE
            || item == InventoryItems.IRON_PICKAXE || item == InventoryItems.DIAMOND_PICKAXE
            || item == InventoryItems.NETHERITE_PICKAXE;
    }

    private static boolean isShovel(byte item) {
        return item == InventoryItems.WOODEN_SHOVEL || item == InventoryItems.STONE_SHOVEL
            || item == InventoryItems.IRON_SHOVEL || item == InventoryItems.DIAMOND_SHOVEL
            || item == InventoryItems.NETHERITE_SHOVEL;
    }

    private static boolean isAxe(byte item) {
        return item == InventoryItems.WOODEN_AXE || item == InventoryItems.STONE_AXE
            || item == InventoryItems.IRON_AXE || item == InventoryItems.DIAMOND_AXE
            || item == InventoryItems.NETHERITE_AXE;
    }

    private static int pickaxeTier(byte itemId) {
        switch (itemId) {
            case InventoryItems.WOODEN_PICKAXE:
                return 1;
            case InventoryItems.STONE_PICKAXE:
                return 2;
            case InventoryItems.IRON_PICKAXE:
                return 3;
            case InventoryItems.DIAMOND_PICKAXE:
                return 4;
            case InventoryItems.NETHERITE_PICKAXE:
                return 5;
            default:
                return 0;
        }
    }

    private void applyMobSnapshot(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            VoxelWorld world = activeWorld;
            if (world == null) {
                return;
            }
            List<MobEntity> mobs = world.getMobs();
            ArrayList<MobEntity> previousMobs = new ArrayList<>(mobs);
            boolean[] reused = new boolean[previousMobs.size()];
            mobs.clear();
            int count = input.readInt();
            for (int i = 0; i < count; i++) {
                int ordinal = input.readUnsignedByte();
                MobKind[] kinds = MobKind.values();
                MobKind kind = ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal] : MobKind.ZOMBIE;
                double x = input.readDouble();
                double y = input.readDouble();
                double z = input.readDouble();
                double bodyYaw = input.readDouble();
                MobEntity mob = reuseMob(previousMobs, reused, kind, x, y, z);
                double oldX = mob.x;
                double oldY = mob.y;
                double oldZ = mob.z;
                mob.capturePreviousPosition();
                mob.x = x;
                mob.y = y;
                mob.z = z;
                double snapshotSeconds = Math.max(0.001, ENTITY_SNAPSHOT_INTERVAL);
                mob.velocityX = (x - oldX) / snapshotSeconds;
                mob.velocityZ = (z - oldZ) / snapshotSeconds;
                mob.verticalVelocity = (y - oldY) / snapshotSeconds;
                mob.bodyYaw = bodyYaw;
                mob.targetBodyYaw = mob.bodyYaw;
                mob.health = input.readDouble();
                mob.babyAge = input.readDouble();
                double horizontalSpeed = Math.sqrt(mob.velocityX * mob.velocityX + mob.velocityZ * mob.velocityZ);
                if (horizontalSpeed > 0.02) {
                    mob.walkCycle += horizontalSpeed * snapshotSeconds * (mob.isGrounded ? 8.0 : 5.0);
                }
                mobs.add(mob);
            }
        } catch (IOException exception) {
            setStatus("Mob sync failed: " + exception.getMessage());
        }
    }

    private MobEntity reuseMob(List<MobEntity> previousMobs, boolean[] reused, MobKind kind, double x, double y, double z) {
        int bestIndex = -1;
        double bestDistanceSquared = 16.0;
        for (int i = 0; i < previousMobs.size(); i++) {
            if (reused[i]) {
                continue;
            }
            MobEntity candidate = previousMobs.get(i);
            if (candidate.kind != kind) {
                continue;
            }
            double dx = candidate.x - x;
            double dy = candidate.y - y;
            double dz = candidate.z - z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) {
            reused[bestIndex] = true;
            return previousMobs.get(bestIndex);
        }
        return new MobEntity(kind, x, y, z, 0.0, 0.0, new java.util.Random(1L));
    }

    private void applyDroppedItemSnapshot(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            VoxelWorld world = activeWorld;
            if (world == null) {
                return;
            }
            List<DroppedItem> items = world.getDroppedItems();
            items.clear();
            int count = input.readInt();
            for (int i = 0; i < count; i++) {
                byte itemId = input.readByte();
                int itemCount = input.readInt();
                int durabilityDamage = input.readInt();
                DroppedItem item = new DroppedItem(itemId, itemCount, input.readDouble(), input.readDouble(), input.readDouble());
                item.durabilityDamage = durabilityDamage;
                items.add(item);
            }
        } catch (IOException exception) {
            setStatus("Item sync failed: " + exception.getMessage());
        }
    }

    private void sendChunkRequest(Connection connection, int dimensionId, int chunkX, int chunkZ) {
        send(connection, MultiplayerProtocol.CHUNK_REQUEST, output -> {
            output.writeInt(dimensionId);
            output.writeInt(chunkX);
            output.writeInt(chunkZ);
        });
    }

    private void sendDisconnect(Connection connection, String reason) {
        send(connection, MultiplayerProtocol.DISCONNECT, output -> output.writeUTF(reason));
    }

    private void send(Connection connection, byte type, MultiplayerProtocol.PacketWriter writer) {
        if (connection == null || !connection.open) {
            return;
        }
        synchronized (connection.output) {
            try {
                MultiplayerProtocol.writePacket(connection.output, type, writer);
            } catch (IOException exception) {
                closeQuietly(connection);
            }
        }
    }

    private void emitChat(String message) {
        if (listener != null) {
            mainThreadEvents.add(() -> listener.onMultiplayerChat(message));
        }
    }

    private void requirePayloadLimit(MultiplayerProtocol.Packet packet, int maxBytes) throws IOException {
        if (packet.payloadLength > maxBytes) {
            throw new IOException("packet payload too large for type " + packet.type);
        }
    }

    private void requireRate(RateCounter counter, int maxEvents, ServerClient client, String message) throws IOException {
        if (!counter.allow(maxEvents)) {
            sendDisconnect(client.connection, message);
            closeQuietly(client.connection);
            throw new IOException(message);
        }
    }

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private void setStatus(String nextStatus) {
        status = nextStatus == null ? "Offline" : nextStatus;
        if (listener != null) {
            final String deliveredStatus = status;
            mainThreadEvents.add(() -> listener.onMultiplayerStatus(deliveredStatus));
        }
    }

    private byte[] readRemaining(DataInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (input.available() > 0) {
            int read = input.read(buffer, 0, Math.min(buffer.length, input.available()));
            if (read <= 0) {
                break;
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            connection.open = false;
            try {
                connection.socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private long columnKey(int dimensionId, int chunkX, int chunkZ) {
        long dimension = (dimensionId & 0xFFL) << 56;
        long x = (chunkX & 0x0FFFFFFFL) << 28;
        long z = chunkZ & 0x0FFFFFFFL;
        return dimension | x | z;
    }

    private int unpackColumnX(long key) {
        int value = (int) ((key >>> 28) & 0x0FFFFFFFL);
        return (value & 0x08000000) != 0 ? value | 0xF0000000 : value;
    }

    private int unpackColumnZ(long key) {
        int value = (int) (key & 0x0FFFFFFFL);
        return (value & 0x08000000) != 0 ? value | 0xF0000000 : value;
    }

    private int unpackDimension(long key) {
        return (int) ((key >>> 56) & 0xFFL);
    }

    private static final class Connection {
        final Socket socket;
        final DataInputStream input;
        final DataOutputStream output;
        volatile boolean open = true;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.socket.setTcpNoDelay(true);
            this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            this.output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }
    }

    private final class ServerClient {
        final UUID uuid;
        String name;
        final Connection connection;
        final PlayerState player = new PlayerState();
        final PlayerInventory inventory = new PlayerInventory();
        final RateCounter playerStateRate = new RateCounter();
        final RateCounter chunkRequestRate = new RateCounter();
        final RateCounter blockActionRate = new RateCounter();
        final RateCounter attackRate = new RateCounter();
        final RateCounter chatCommandRate = new RateCounter();
        final RateCounter containerClickRate = new RateCounter();
        byte heldItem = GameConfig.AIR;
        int selectedHotbarSlot;
        int pingMs = -1;
        long pendingPingTime = -1L;
        long teleportGraceUntilMillis = -1L;
        int activeWindowId;
        int activeScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
        int activeContainerX;
        int activeContainerY;
        int activeContainerZ;
        long breakingStartedMillis;
        int breakingDimensionId = GameConfig.DIMENSION_OVERWORLD;
        int breakingX;
        int breakingY;
        int breakingZ;
        byte breakingHeldItem = GameConfig.AIR;
        int breakingHotbarSlot;

        ServerClient(UUID uuid, String name, Connection connection) {
            this.uuid = uuid;
            this.name = name;
            this.connection = connection;
        }

        void worldUpdate(String displayName) {
            VoxelWorld world = activeWorld;
            if (world != null) {
                world.updateRemotePlayer(uuid, displayName, player.x, player.y, player.z, player.yaw, player.pitch, heldItem, player.sneaking, player.spectatorMode);
            }
        }
    }

    static final class RateCounter {
        long windowStartedMillis;
        int count;

        boolean allow(int maxEvents) {
            long now = System.currentTimeMillis();
            if (now - windowStartedMillis >= RATE_WINDOW_MS) {
                windowStartedMillis = now;
                count = 0;
            }
            count++;
            return count <= maxEvents;
        }
    }

    private static final class ContainerState {
        final int windowId;
        final int screenMode;
        final int x;
        final int y;
        final int z;
        final ContainerInventory chest;
        final FurnaceBlockEntity furnace;

        ContainerState(int windowId, int screenMode, int x, int y, int z, ContainerInventory chest, FurnaceBlockEntity furnace) {
            this.windowId = windowId;
            this.screenMode = screenMode;
            this.x = x;
            this.y = y;
            this.z = z;
            this.chest = chest;
            this.furnace = furnace;
        }
    }

    private static final class BlockAction {
        final ServerClient client;
        final byte kind;
        final int dimensionId;
        final int x;
        final int y;
        final int z;
        final int previousX;
        final int previousY;
        final int previousZ;
        final byte heldItem;
        final int selectedHotbarSlot;

        BlockAction(ServerClient client, byte kind, int dimensionId, int x, int y, int z, int previousX, int previousY, int previousZ, byte heldItem, int selectedHotbarSlot) {
            this.client = client;
            this.kind = kind;
            this.dimensionId = dimensionId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.previousX = previousX;
            this.previousY = previousY;
            this.previousZ = previousZ;
            this.heldItem = heldItem;
            this.selectedHotbarSlot = selectedHotbarSlot;
        }

        static BlockAction read(ServerClient client, DataInputStream input) throws IOException {
            byte kind = input.readByte();
            int dimensionId = input.readInt();
            int x = input.readInt();
            int y = input.readInt();
            int z = input.readInt();
            int previousX = input.readInt();
            int previousY = input.readInt();
            int previousZ = input.readInt();
            byte heldItem = input.readByte();
            int selectedHotbarSlot = input.available() >= 4 ? input.readInt() : 0;
            return new BlockAction(
                client,
                kind,
                dimensionId,
                x,
                y,
                z,
                previousX,
                previousY,
                previousZ,
                heldItem,
                selectedHotbarSlot
            );
        }
    }

    private static final class ChunkRequest {
        final ServerClient client;
        final int dimensionId;
        final int chunkX;
        final int chunkZ;

        ChunkRequest(ServerClient client, int dimensionId, int chunkX, int chunkZ) {
            this.client = client;
            this.dimensionId = dimensionId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }
}
