import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class ChatSystem {
    interface CommandTarget {
        void teleportPlayer(double x, double y, double z);

        void setWorldTime(double worldTime);

        void setGameMode(String mode);

        void clearInventory();

        boolean giveItem(byte itemId, int amount);

        void spawnZombieAtPlayer();

        String currentSeed();

        String locateBiome(String biomeName);

        String locateStructure(String structureName);

        String placeStructure(String structureName, int rotation);

        String listStructures();

        String currentDebugLocation();

        String terrainDebugAt(int x, int z);

        String heightTest();

        String blockInfo();

        void sendChat(String message);
    }

    private static final int MAX_MESSAGES = 8;
    private static final int MAX_INPUT_LENGTH = 96;
    private final ArrayList<String> messages = new ArrayList<>();
    private final StringBuilder input = new StringBuilder(MAX_INPUT_LENGTH);
    private boolean active;
    private boolean suppressNextCharacter;

    boolean isActive() {
        return active;
    }

    void open() {
        active = true;
        suppressNextCharacter = true;
        input.setLength(0);
    }

    void close() {
        active = false;
        suppressNextCharacter = false;
        input.setLength(0);
    }

    void appendCharacter(int codepoint) {
        if (!active) {
            return;
        }
        if (suppressNextCharacter) {
            suppressNextCharacter = false;
            if (codepoint == 't' || codepoint == 'T') {
                return;
            }
        }
        if (Character.isISOControl(codepoint) || !Character.isValidCodePoint(codepoint) || input.length() >= MAX_INPUT_LENGTH) {
            return;
        }
        input.appendCodePoint(codepoint);
    }

    void backspace() {
        if (active && input.length() > 0) {
            input.deleteCharAt(input.length() - 1);
        }
    }

    void submit(CommandTarget target) {
        if (!active) {
            return;
        }
        String submitted = input.toString().trim();
        close();
        if (submitted.isEmpty()) {
            return;
        }
        if (submitted.charAt(0) == '/') {
            executeCommand(submitted, target);
        } else {
            target.sendChat(submitted);
        }
    }

    String inputText() {
        return input.toString();
    }

    List<String> visibleMessages() {
        return Collections.unmodifiableList(messages);
    }

    void addMessage(String message) {
        if (message == null) {
            return;
        }
        String[] lines = message.split("\\R", -1);
        for (String line : lines) {
            messages.add(line);
            while (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }
    }

    private void executeCommand(String submitted, CommandTarget target) {
        String[] parts = submitted.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            addMessage("Usage: /tp <x> <y> <z> or /time set <day|night>");
            return;
        }

        String command = parts[0].toLowerCase(Locale.ROOT);
        if ("list".equals(command) || "ping".equals(command) || "msg".equals(command) || "kick".equals(command)) {
            target.sendChat(submitted);
            return;
        }
        if ("tp".equals(command)) {
            executeTeleport(parts, target);
            return;
        }
        if ("time".equals(command)) {
            executeTime(parts, target);
            return;
        }
        if ("gamemode".equals(command)) {
            executeGameMode(parts, target);
            return;
        }
        if ("clear".equals(command)) {
            executeClear(parts, target);
            return;
        }
        if ("say".equals(command)) {
            executeSay(submitted);
            return;
        }
        if ("give".equals(command)) {
            executeGive(parts, target);
            return;
        }
        if ("spawnzombie".equals(command)) {
            executeSpawnZombie(parts, target);
            return;
        }
        if ("seed".equals(command)) {
            executeSeed(parts, target);
            return;
        }
        if ("locate".equals(command)) {
            executeLocate(parts, target);
            return;
        }
        if ("locatebiome".equals(command) || "locateBiome".equals(command)) {
            executeLocateBiomeAlias(parts, target);
            return;
        }
        if ("place".equals(command)) {
            executePlace(parts, target);
            return;
        }
        if ("whereami".equals(command) || "locatebug".equals(command)) {
            executeWhereAmI(parts, target);
            return;
        }
        if ("probe".equals(command) || "terrain".equals(command)) {
            executeProbe(parts, target);
            return;
        }
        if ("heighttest".equals(command)) {
            executeHeightTest(parts, target);
            return;
        }
        if ("blockinfo".equals(command)) {
            executeBlockInfo(parts, target);
            return;
        }
        addMessage("Unknown command: /" + parts[0]);
    }

    private void executeLocateBiomeAlias(String[] parts, CommandTarget target) {
        if (parts.length < 2) {
            addMessage("Usage: /locatebiome <name>");
            return;
        }
        StringBuilder biomeName = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (biomeName.length() > 0) {
                biomeName.append(' ');
            }
            biomeName.append(parts[i]);
        }
        String result = target.locateBiome(biomeName.toString());
        addMessage(isBlank(result) ? "Biome not found." : result);
    }

    private void executeTeleport(String[] parts, CommandTarget target) {
        if (parts.length != 4) {
            addMessage("Usage: /tp <x> <y> <z>");
            return;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                addMessage("Coordinates must be finite numbers.");
                return;
            }
            target.teleportPlayer(x, y, z);
            addMessage(String.format(Locale.ROOT, "Teleported to %.1f %.1f %.1f", x, y, z));
        } catch (NumberFormatException exception) {
            addMessage("Coordinates must be numbers.");
        }
    }

    private void executeTime(String[] parts, CommandTarget target) {
        if (parts.length != 3 || !"set".equalsIgnoreCase(parts[1])) {
            addMessage("Usage: /time set <day|night>");
            return;
        }
        String value = parts[2].toLowerCase(Locale.ROOT);
        if ("day".equals(value)) {
            target.setWorldTime(0.30);
            addMessage("Time set to day.");
        } else if ("night".equals(value)) {
            target.setWorldTime(0.80);
            addMessage("Time set to night.");
        } else {
            addMessage("Usage: /time set <day|night>");
        }
    }

    private void executeGameMode(String[] parts, CommandTarget target) {
        if (parts.length != 2) {
            addMessage("Usage: /gamemode <creative|survival|spectator>");
            return;
        }
        String mode = parts[1].toLowerCase(Locale.ROOT);
        if (!"creative".equals(mode) && !"survival".equals(mode) && !"spectator".equals(mode)) {
            addMessage("Usage: /gamemode <creative|survival|spectator>");
            return;
        }
        target.setGameMode(mode);
        addMessage("Game mode set to " + mode + ".");
    }

    private void executeClear(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /clear");
            return;
        }
        target.clearInventory();
        addMessage("Inventory cleared.");
    }

    private void executeSay(String submitted) {
        String message = submitted.length() <= 4 ? "" : submitted.substring(5).trim();
        if (message.isEmpty()) {
            addMessage("Usage: /say <message>");
            return;
        }
        addMessage("[Server] " + message);
    }

    private void executeGive(String[] parts, CommandTarget target) {
        if (parts.length != 3) {
            addMessage("Usage: /give <id|tinycraft:name> <amount>");
            return;
        }
        try {
            int amount = Integer.parseInt(parts[2]);
            if (amount <= 0 || amount > 4096) {
                addMessage("Amount must be between 1 and 4096.");
                return;
            }

            Byte resolved = resolveGiveItem(parts[1]);
            if (resolved == null) {
                addMessage("Unknown item or block '" + parts[1] + "'.");
                return;
            }
            byte itemId = resolved.byteValue();
            if (target.giveItem(itemId, amount)) {
                addMessage("Gave " + InventoryItems.name(itemId) + " x" + amount + ".");
            } else {
                addMessage("Cannot give item " + parts[1] + ".");
            }
        } catch (NumberFormatException exception) {
            addMessage("Usage: /give <id|tinycraft:name> <amount>");
        }
    }

    private Byte resolveGiveItem(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String query = raw.trim().toLowerCase(Locale.ROOT);
        Byte numeric = resolveNumericGiveItem(query);
        if (numeric != null) {
            return numeric;
        }
        Byte namedItem = resolveNamedInventoryItem(query);
        if (namedItem != null) {
            return namedItem;
        }
        Byte block = resolveBlockName(query);
        if (block != null) {
            return block;
        }
        return resolveBlockName(toInternalBlockId(query));
    }

    private Byte resolveNumericGiveItem(String query) {
        try {
            int rawId = Integer.parseInt(query);
            if (rawId >= 0 && rawId <= 255) {
                byte item = (byte) rawId;
                return isHiddenGiveItem(item) ? null : item;
            }
            if (rawId >= Byte.MIN_VALUE && rawId <= Byte.MAX_VALUE) {
                byte item = (byte) rawId;
                return isHiddenGiveItem(item) ? null : item;
            }
        } catch (NumberFormatException exception) {
            System.err.println("Failed to parse item ID from chat command '" + query + "': " + exception);
        }
        return null;
    }

    private boolean isHiddenGiveItem(byte item) {
        return item == GameConfig.CARROT_CROP || item == GameConfig.POTATO_CROP;
    }

    private Byte resolveBlockName(String namespacedId) {
        BlockState state = Blocks.stateFromNamespacedId(namespacedId);
        if (state == null || state.type == null) {
            return null;
        }
        if (state.type.numericId == (GameConfig.AIR & 0xFF) && !"minecraft:air".equals(namespacedId)) {
            return null;
        }
        if (state.type.numericId == (GameConfig.CARROT_CROP & 0xFF)
            || state.type.numericId == (GameConfig.POTATO_CROP & 0xFF)) {
            return null;
        }
        return Blocks.legacyIdFromState(state);
    }

    private Byte resolveNamedInventoryItem(String query) {
        switch (stripTinyCraftNamespace(query)) {
            case "stick":
                return InventoryItems.STICK;
            case "coal":
                return InventoryItems.COAL_ITEM;
            case "iron_ingot":
                return InventoryItems.IRON_INGOT;
            case "diamond":
                return InventoryItems.DIAMOND_ITEM;
            case "wheat_seeds":
            case "seeds":
                return InventoryItems.WHEAT_SEEDS;
            case "carrot":
            case "carrots":
                return InventoryItems.CARROT;
            case "potato":
            case "potatoes":
                return InventoryItems.POTATO;
            case "raw_herring":
            case "herring":
                return InventoryItems.RAW_HERRING;
            case "raw_salmon":
            case "salmon":
                return InventoryItems.RAW_SALMON;
            case "cooked_herring":
                return InventoryItems.COOKED_HERRING;
            case "cooked_salmon":
                return InventoryItems.COOKED_SALMON;
            case "herring_spawn_egg":
                return InventoryItems.HERRING_SPAWN_EGG;
            case "salmon_spawn_egg":
                return InventoryItems.SALMON_SPAWN_EGG;
            default:
                return null;
        }
    }

    private String toInternalBlockId(String query) {
        String localName = stripTinyCraftNamespace(query);
        if (localName.startsWith("minecraft:")) {
            return localName;
        }
        return "minecraft:" + localName;
    }

    private String stripTinyCraftNamespace(String query) {
        return query != null && query.startsWith("tinycraft:") ? query.substring("tinycraft:".length()) : query;
    }

    private void executeSpawnZombie(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /spawnzombie");
            return;
        }
        target.spawnZombieAtPlayer();
        addMessage("Spawned zombie.");
    }

    private void executeSeed(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /seed");
            return;
        }
        addMessage("Seed: " + target.currentSeed());
    }

    private void executeLocate(String[] parts, CommandTarget target) {
        if (parts.length == 2) {
            String direct = parts[1];
            if ("village".equalsIgnoreCase(direct)
                || "mineshaft".equalsIgnoreCase(direct)
                || "shaft".equalsIgnoreCase(direct)) {
                String result = target.locateStructure(direct);
                addMessage(isBlank(result) ? "Structure not found." : result);
                return;
            }
        }

        if (parts.length < 3) {
            addMessage("Usage: /locate biome <name> or /locate structure <village|mineshaft>");
            return;
        }

        if ("biome".equalsIgnoreCase(parts[1]) || "biom".equalsIgnoreCase(parts[1])) {
            StringBuilder biomeName = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                if (biomeName.length() > 0) {
                    biomeName.append(' ');
                }
                biomeName.append(parts[i]);
            }
            String result = target.locateBiome(biomeName.toString());
            addMessage(isBlank(result) ? "Biome not found." : result);
            return;
        }

        if ("structure".equalsIgnoreCase(parts[1]) || "struct".equalsIgnoreCase(parts[1])) {
            StringBuilder structureName = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                if (structureName.length() > 0) {
                    structureName.append(' ');
                }
                structureName.append(parts[i]);
            }
            String result = target.locateStructure(structureName.toString());
            addMessage(isBlank(result) ? "Structure not found." : result);
            return;
        }

        addMessage("Usage: /locate biome <name> or /locate structure <village|mineshaft>");
    }

    String copyText() {
        if (active && input.length() > 0) {
            return input.toString();
        }
        if (messages.isEmpty()) {
            return "";
        }
        return messages.get(messages.size() - 1);
    }

    private void executePlace(String[] parts, CommandTarget target) {
        if (parts.length >= 3 && "structure".equalsIgnoreCase(parts[1])) {
            if ("list".equalsIgnoreCase(parts[2])) {
                addMessage("Structures: " + target.listStructures());
                return;
            }
            String name = parts[2];
            int rotation = 0;
            if (parts.length >= 4) {
                try {
                    rotation = Integer.parseInt(parts[3]);
                } catch (NumberFormatException exception) {
                    addMessage("Rotation must be 0, 1, 2, or 3.");
                    return;
                }
            }
            addMessage(target.placeStructure(name, rotation));
            return;
        }
        addMessage("Usage: /place structure <name|list> [rotation]");
    }

    private void executeWhereAmI(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /whereami");
            return;
        }
        addMessage(target.currentDebugLocation());
    }

    private void executeProbe(String[] parts, CommandTarget target) {
        if (parts.length != 3) {
            addMessage("Usage: /probe <x> <z>");
            return;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            addMessage(target.terrainDebugAt(x, z));
        } catch (NumberFormatException exception) {
            addMessage("Usage: /probe <x> <z>");
        }
    }

    private void executeHeightTest(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /heighttest");
            return;
        }
        addMessage(target.heightTest());
    }

    private void executeBlockInfo(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /blockinfo");
            return;
        }
        addMessage(target.blockInfo());
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
