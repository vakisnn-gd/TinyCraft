import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

final class ChatSystem {
    interface CommandTarget {
        void teleportPlayer(double x, double y, double z);

        void setWorldTime(double worldTime);

        void setGameMode(String mode);

        void clearInventory();

        boolean giveItem(byte itemId, int amount);

        void killPlayer();

        boolean summonMob(MobKind kind);

        int setBlock(int x, int y, int z, BlockState state);

        int fillBlocks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state);

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

        default boolean canUseCheatCommands() {
            return true;
        }

        default boolean shouldHandleCommandRemotely(String command) {
            return false;
        }
    }

    static final class VisibleMessage {
        private final String text;
        private final float alpha;

        private VisibleMessage(String text, float alpha) {
            this.text = text;
            this.alpha = alpha;
        }

        String text() {
            return text;
        }

        float alpha() {
            return alpha;
        }
    }

    static final class CommandSuggestion {
        private final String text;
        private final boolean selected;

        private CommandSuggestion(String text, boolean selected) {
            this.text = text;
            this.selected = selected;
        }

        String text() {
            return text;
        }

        boolean selected() {
            return selected;
        }
    }

    private static final class CommandSpec {
        private final String name;
        private final String syntax;
        private final String description;

        private CommandSpec(String name, String syntax, String description) {
            this.name = name;
            this.syntax = syntax;
            this.description = description;
        }

        private String suggestionText() {
            StringBuilder text = new StringBuilder("/").append(name);
            if (!syntax.isEmpty()) {
                text.append(' ').append(syntax);
            }
            if (!description.isEmpty()) {
                text.append("  ").append(description);
            }
            return text.toString();
        }
    }

    private static final class CompletionCandidate {
        private final String replacement;
        private final String displayText;

        private CompletionCandidate(String replacement, String displayText) {
            this.replacement = replacement;
            this.displayText = displayText;
        }
    }

    private static final class StoredMessage {
        private final String text;
        private final long createdAtNanos;

        private StoredMessage(String text, long createdAtNanos) {
            this.text = text;
            this.createdAtNanos = createdAtNanos;
        }
    }

    private static final int MAX_MESSAGES = 100;
    private static final int CLOSED_MESSAGE_LIMIT = 5;
    private static final long CLOSED_VISIBLE_NANOS = 8_000_000_000L;
    private static final long CLOSED_FADE_NANOS = 2_000_000_000L;
    private static final int MAX_INPUT_LENGTH = 96;
    private static final int MAX_INPUT_HISTORY = 100;
    private static final int MAX_VISIBLE_SUGGESTIONS = 6;
    private static final int SCROLL_LINES_PER_NOTCH = 3;
    static final int MAX_FILL_BLOCKS = 32768;
    private static final CommandSpec[] COMMAND_SPECS = {
        new CommandSpec("blockinfo", "", "Selected block details"),
        new CommandSpec("clear", "", "Clear inventory"),
        new CommandSpec("fill", "<from> <to> <block>", "Fill an area with blocks"),
        new CommandSpec("gamemode", "<creative|survival|spectator>", "Change game mode"),
        new CommandSpec("give", "<id|tinycraft:name> <amount>", "Give an item"),
        new CommandSpec("heighttest", "", "Check world height"),
        new CommandSpec("help", "[command]", "Show available commands"),
        new CommandSpec("kick", "<player> [reason]", "Disconnect a player"),
        new CommandSpec("kill", "", "Kill yourself"),
        new CommandSpec("list", "", "List online players"),
        new CommandSpec("locate", "<biome|structure> <name>", "Find a place"),
        new CommandSpec("locatebiome", "<name>", "Find a biome"),
        new CommandSpec("msg", "<player> <message>", "Send a private message"),
        new CommandSpec("ping", "", "Show connection latency"),
        new CommandSpec("place", "structure <name|list> [rotation]", "Place a structure"),
        new CommandSpec("probe", "<x> <z>", "Inspect terrain generation"),
        new CommandSpec("say", "<message>", "Broadcast a message"),
        new CommandSpec("seed", "", "Show the world seed"),
        new CommandSpec("setblock", "<x> <y> <z> <block>", "Set one block"),
        new CommandSpec("spawnzombie", "", "Spawn a zombie"),
        new CommandSpec("summon", "<entity>", "Summon a mob"),
        new CommandSpec("time", "set <day|night>", "Change world time"),
        new CommandSpec("tp", "<x> <y> <z>", "Teleport"),
        new CommandSpec("whereami", "", "Show debug location")
    };
    private static final List<String> BIOME_SUGGESTIONS = Arrays.asList(
        "badlands", "birch_forest", "dark_forest", "desert", "forest", "grove", "ice_desert",
        "jungle", "meadow", "mountains", "plains", "rainforest", "savanna", "seasonal_forest",
        "shrubland", "swampland", "taiga", "tundra", "wooded_hills"
    );
    private static final List<String> MOB_SUGGESTIONS = Arrays.asList(
        "cow", "herring", "pig", "salmon", "sheep", "skeleton", "villager", "zombie"
    );
    private static final List<String> BLOCK_SUGGESTIONS = buildBlockSuggestions();
    private static final List<String> GIVE_ITEM_SUGGESTIONS = buildGiveItemSuggestions();
    private final ArrayList<StoredMessage> messages = new ArrayList<>();
    private final ArrayList<String> inputHistory = new ArrayList<>();
    private final StringBuilder input = new StringBuilder(MAX_INPUT_LENGTH);
    private final LongSupplier nanoTime;
    private List<CompletionCandidate> completionCandidates = Collections.emptyList();
    private int completionIndex = -1;
    private int historyCursor;
    private int scrollOffset;
    private String historyDraft = "";
    private boolean cheatSuggestionsEnabled = true;
    private boolean active;
    private boolean suppressNextCharacter;

    ChatSystem() {
        this(System::nanoTime);
    }

    ChatSystem(LongSupplier nanoTime) {
        this.nanoTime = nanoTime == null ? System::nanoTime : nanoTime;
    }

    boolean isActive() {
        return active;
    }

    void setCheatSuggestionsEnabled(boolean enabled) {
        cheatSuggestionsEnabled = enabled;
        resetCompletion();
    }

    void open() {
        active = true;
        suppressNextCharacter = true;
        scrollOffset = 0;
        input.setLength(0);
        resetHistoryNavigation();
        resetCompletion();
    }

    void close() {
        active = false;
        suppressNextCharacter = false;
        scrollOffset = 0;
        input.setLength(0);
        resetHistoryNavigation();
        resetCompletion();
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
        inputEdited();
    }

    void backspace() {
        if (active && input.length() > 0) {
            input.deleteCharAt(input.length() - 1);
            inputEdited();
        }
    }

    void previousInput() {
        if (!active || inputHistory.isEmpty() || historyCursor <= 0) {
            return;
        }
        if (historyCursor == inputHistory.size()) {
            historyDraft = input.toString();
        }
        historyCursor--;
        replaceInput(inputHistory.get(historyCursor));
        resetCompletion();
    }

    void nextInput() {
        if (!active || historyCursor >= inputHistory.size()) {
            return;
        }
        historyCursor++;
        replaceInput(historyCursor < inputHistory.size() ? inputHistory.get(historyCursor) : historyDraft);
        resetCompletion();
    }

    void cycleSuggestion(boolean reverse) {
        if (!active) {
            return;
        }
        if (completionCandidates.isEmpty()) {
            completionCandidates = buildCompletionCandidates(input.toString());
            if (completionCandidates.isEmpty()) {
                return;
            }
            completionIndex = reverse ? completionCandidates.size() - 1 : 0;
        } else {
            int direction = reverse ? -1 : 1;
            completionIndex = Math.floorMod(completionIndex + direction, completionCandidates.size());
        }
        replaceInput(completionCandidates.get(completionIndex).replacement);
        historyCursor = inputHistory.size();
        historyDraft = input.toString();
    }

    List<CommandSuggestion> commandSuggestions() {
        if (!active) {
            return Collections.emptyList();
        }
        List<CompletionCandidate> candidates = completionCandidates.isEmpty()
            ? buildCompletionCandidates(input.toString())
            : completionCandidates;
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        int selectedIndex = completionIndex >= 0 ? completionIndex : 0;
        int maxStart = Math.max(0, candidates.size() - MAX_VISIBLE_SUGGESTIONS);
        int start = Math.max(0, Math.min(maxStart, selectedIndex - MAX_VISIBLE_SUGGESTIONS / 2));
        int end = Math.min(candidates.size(), start + MAX_VISIBLE_SUGGESTIONS);
        ArrayList<CommandSuggestion> visible = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            visible.add(new CommandSuggestion(candidates.get(i).displayText, i == selectedIndex));
        }
        return Collections.unmodifiableList(visible);
    }

    void submit(CommandTarget target) {
        if (!active) {
            return;
        }
        String submitted = input.toString().trim();
        rememberInput(submitted);
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

    void scroll(int direction) {
        if (!active || direction == 0) {
            return;
        }
        scrollOffset = Math.max(0, scrollOffset + Integer.signum(direction) * SCROLL_LINES_PER_NOTCH);
    }

    int scrollOffset() {
        return scrollOffset;
    }

    private void rememberInput(String submitted) {
        if (submitted.isEmpty()) {
            return;
        }
        if (inputHistory.isEmpty() || !submitted.equals(inputHistory.get(inputHistory.size() - 1))) {
            inputHistory.add(submitted);
            while (inputHistory.size() > MAX_INPUT_HISTORY) {
                inputHistory.remove(0);
            }
        }
    }

    private void inputEdited() {
        historyCursor = inputHistory.size();
        historyDraft = input.toString();
        resetCompletion();
    }

    private void resetHistoryNavigation() {
        historyCursor = inputHistory.size();
        historyDraft = "";
    }

    private void resetCompletion() {
        completionCandidates = Collections.emptyList();
        completionIndex = -1;
    }

    private void replaceInput(String replacement) {
        input.setLength(0);
        if (replacement == null || replacement.isEmpty()) {
            return;
        }
        input.append(replacement, 0, Math.min(MAX_INPUT_LENGTH, replacement.length()));
    }

    private List<CompletionCandidate> buildCompletionCandidates(String value) {
        if (value == null || !value.startsWith("/")) {
            return Collections.emptyList();
        }

        int whitespace = firstWhitespaceIndex(value, 1);
        if (whitespace < 0) {
            String prefix = value.substring(1).toLowerCase(Locale.ROOT);
            ArrayList<CompletionCandidate> candidates = new ArrayList<>();
            for (CommandSpec spec : COMMAND_SPECS) {
                if (spec.name.startsWith(prefix) && (cheatSuggestionsEnabled || !isCheatCommand(spec.name))) {
                    candidates.add(new CompletionCandidate("/" + spec.name, spec.suggestionText()));
                }
            }
            return candidates;
        }

        int tokenStart = currentTokenStart(value);
        String completedText = value.substring(1, tokenStart).trim();
        if (completedText.isEmpty()) {
            return Collections.emptyList();
        }
        String[] completed = completedText.split("\\s+");
        String command = completed[0].toLowerCase(Locale.ROOT);
        if (!cheatSuggestionsEnabled && isCheatCommand(command)) {
            return Collections.emptyList();
        }
        String prefix = value.substring(tokenStart).toLowerCase(Locale.ROOT);
        List<String> options = argumentOptions(command, completed.length, completed);
        if (options.isEmpty()) {
            return Collections.emptyList();
        }

        String replacementPrefix = value.substring(0, tokenStart);
        ArrayList<CompletionCandidate> candidates = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                candidates.add(new CompletionCandidate(replacementPrefix + option, option));
            }
        }
        return candidates;
    }

    private static List<String> argumentOptions(String command, int argumentIndex, String[] completed) {
        if ("gamemode".equals(command) && argumentIndex == 1) {
            return Arrays.asList("creative", "survival", "spectator");
        }
        if ("time".equals(command)) {
            if (argumentIndex == 1) {
                return Collections.singletonList("set");
            }
            if (argumentIndex == 2 && "set".equalsIgnoreCase(completed[1])) {
                return Arrays.asList("day", "night");
            }
        }
        if ("give".equals(command) && argumentIndex == 1) {
            return GIVE_ITEM_SUGGESTIONS;
        }
        if ("summon".equals(command) && argumentIndex == 1) {
            return MOB_SUGGESTIONS;
        }
        if (("setblock".equals(command) && argumentIndex == 4)
            || ("fill".equals(command) && argumentIndex == 7)) {
            return BLOCK_SUGGESTIONS;
        }
        if ("locatebiome".equals(command) && argumentIndex == 1) {
            return BIOME_SUGGESTIONS;
        }
        if ("locate".equals(command)) {
            if (argumentIndex == 1) {
                return Arrays.asList("biome", "structure", "village", "mineshaft");
            }
            if (argumentIndex == 2 && "biome".equalsIgnoreCase(completed[1])) {
                return BIOME_SUGGESTIONS;
            }
            if (argumentIndex == 2 && "structure".equalsIgnoreCase(completed[1])) {
                return Arrays.asList("village", "mineshaft");
            }
        }
        if ("place".equals(command)) {
            if (argumentIndex == 1) {
                return Collections.singletonList("structure");
            }
            if (argumentIndex == 2 && "structure".equalsIgnoreCase(completed[1])) {
                ArrayList<String> structures = new ArrayList<>(StructureTemplates.NAMES.size() + 1);
                structures.add("list");
                structures.addAll(StructureTemplates.NAMES);
                return structures;
            }
            if (argumentIndex == 3 && "structure".equalsIgnoreCase(completed[1])) {
                return Arrays.asList("0", "1", "2", "3");
            }
        }
        return Collections.emptyList();
    }

    private static int firstWhitespaceIndex(String value, int start) {
        for (int i = Math.max(0, start); i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int currentTokenStart(String value) {
        int index = value.length();
        while (index > 0 && !Character.isWhitespace(value.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    private static List<String> buildGiveItemSuggestions() {
        ArrayList<String> names = new ArrayList<>();
        String[] inventoryNames = {
            "stick", "coal", "iron_ingot", "diamond", "wheat_seeds", "carrot", "potato",
            "raw_herring", "raw_salmon", "cooked_herring", "cooked_salmon",
            "herring_spawn_egg", "salmon_spawn_egg"
        };
        for (String name : inventoryNames) {
            names.add("tinycraft:" + name);
        }
        for (int numericId = 0; numericId <= 255; numericId++) {
            byte itemId = (byte) numericId;
            if (!Blocks.isKnownLegacyId(itemId) || isHiddenGiveItem(itemId) || itemId == GameConfig.AIR) {
                continue;
            }
            String namespacedId = Blocks.typeFromLegacyId(itemId).namespacedId;
            String suggestion = namespacedId.startsWith("minecraft:")
                ? "tinycraft:" + namespacedId.substring("minecraft:".length())
                : namespacedId;
            if (!names.contains(suggestion)) {
                names.add(suggestion);
            }
        }
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    private static List<String> buildBlockSuggestions() {
        ArrayList<String> names = new ArrayList<>();
        for (BlockType type : BlockRegistry.registeredTypes()) {
            if (type.namespacedId.startsWith("minecraft:")) {
                names.add(type.namespacedId);
            }
        }
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    List<String> visibleMessages() {
        ArrayList<String> visible = new ArrayList<>(messages.size());
        for (StoredMessage message : messages) {
            visible.add(message.text);
        }
        return Collections.unmodifiableList(visible);
    }

    List<VisibleMessage> renderMessages() {
        long nowNanos = nanoTime.getAsLong();
        int first = active ? 0 : Math.max(0, messages.size() - CLOSED_MESSAGE_LIMIT);
        ArrayList<VisibleMessage> visible = new ArrayList<>(messages.size() - first);
        for (int i = first; i < messages.size(); i++) {
            StoredMessage message = messages.get(i);
            float alpha = active ? 1.0f : closedMessageAlpha(nowNanos - message.createdAtNanos);
            if (alpha > 0.0f) {
                visible.add(new VisibleMessage(message.text, alpha));
            }
        }
        return Collections.unmodifiableList(visible);
    }

    private static float closedMessageAlpha(long ageNanos) {
        if (ageNanos < 0L || ageNanos <= CLOSED_VISIBLE_NANOS - CLOSED_FADE_NANOS) {
            return 1.0f;
        }
        if (ageNanos >= CLOSED_VISIBLE_NANOS) {
            return 0.0f;
        }
        return (float) (CLOSED_VISIBLE_NANOS - ageNanos) / (float) CLOSED_FADE_NANOS;
    }

    void addMessage(String message) {
        if (message == null) {
            return;
        }
        String[] lines = message.split("\\R", -1);
        long createdAtNanos = nanoTime.getAsLong();
        for (String line : lines) {
            messages.add(new StoredMessage(line, createdAtNanos));
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
        if (target.shouldHandleCommandRemotely(submitted)) {
            target.sendChat(submitted);
            return;
        }
        if ("help".equals(command)) {
            executeHelp(parts, target);
            return;
        }
        if ("list".equals(command) || "ping".equals(command) || "msg".equals(command) || "kick".equals(command)) {
            target.sendChat(submitted);
            return;
        }
        if (isCheatCommand(command) && !target.canUseCheatCommands()) {
            addMessage("Cheats are not enabled in this world.");
            return;
        }
        if ("tp".equals(command)) {
            executeTeleport(parts, target);
            return;
        }
        if ("kill".equals(command)) {
            executeKill(parts, target);
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
        if ("setblock".equals(command)) {
            executeSetBlock(parts, target);
            return;
        }
        if ("fill".equals(command)) {
            executeFill(parts, target);
            return;
        }
        if ("spawnzombie".equals(command)) {
            executeSpawnZombie(parts, target);
            return;
        }
        if ("summon".equals(command)) {
            executeSummon(parts, target);
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

    private void executeHelp(String[] parts, CommandTarget target) {
        if (parts.length > 2) {
            addMessage("Usage: /help [command]");
            return;
        }
        if (parts.length == 2) {
            String requested = parts[1].toLowerCase(Locale.ROOT);
            for (CommandSpec spec : COMMAND_SPECS) {
                if (spec.name.equals(requested)) {
                    addMessage(spec.suggestionText());
                    return;
                }
            }
            addMessage("Unknown command: /" + parts[1]);
            return;
        }

        addMessage("Commands: /help, /list, /ping, /msg");
        if (target.canUseCheatCommands()) {
            addMessage("Cheats: /gamemode, /give, /tp, /kill, /summon, /setblock, /fill, /time, /clear, /locate, /place and debug commands");
        }
    }

    void clearMessages() {
        messages.clear();
        scrollOffset = 0;
    }

    private static boolean isCheatCommand(String command) {
        return "tp".equals(command)
            || "kill".equals(command)
            || "time".equals(command)
            || "gamemode".equals(command)
            || "clear".equals(command)
            || "say".equals(command)
            || "give".equals(command)
            || "setblock".equals(command)
            || "fill".equals(command)
            || "spawnzombie".equals(command)
            || "summon".equals(command)
            || "seed".equals(command)
            || "locate".equals(command)
            || "locatebiome".equals(command)
            || "place".equals(command)
            || "whereami".equals(command)
            || "locatebug".equals(command)
            || "probe".equals(command)
            || "terrain".equals(command)
            || "heighttest".equals(command)
            || "blockinfo".equals(command);
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

    private void executeKill(String[] parts, CommandTarget target) {
        if (parts.length != 1) {
            addMessage("Usage: /kill");
            return;
        }
        target.killPlayer();
        addMessage("Killed player.");
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

    private void executeSetBlock(String[] parts, CommandTarget target) {
        if (parts.length != 5) {
            addMessage("Usage: /setblock <x> <y> <z> <block>");
            return;
        }
        BlockState state = resolveCommandBlockState(parts[4]);
        if (state == null) {
            addMessage("Unknown block: " + parts[4]);
            return;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            int changed = target.setBlock(x, y, z, state);
            if (changed < 0) {
                addMessage("Coordinates are outside the world.");
                return;
            }
            addMessage(changed == 0 ? "No blocks were changed." : "Changed the block at " + x + " " + y + " " + z + ".");
        } catch (NumberFormatException exception) {
            addMessage("Coordinates must be whole numbers.");
        }
    }

    private void executeFill(String[] parts, CommandTarget target) {
        if (parts.length != 8) {
            addMessage("Usage: /fill <x1> <y1> <z1> <x2> <y2> <z2> <block>");
            return;
        }
        BlockState state = resolveCommandBlockState(parts[7]);
        if (state == null) {
            addMessage("Unknown block: " + parts[7]);
            return;
        }
        try {
            int x1 = Integer.parseInt(parts[1]);
            int y1 = Integer.parseInt(parts[2]);
            int z1 = Integer.parseInt(parts[3]);
            int x2 = Integer.parseInt(parts[4]);
            int y2 = Integer.parseInt(parts[5]);
            int z2 = Integer.parseInt(parts[6]);
            int minX = Math.min(x1, x2);
            int minY = Math.min(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxX = Math.max(x1, x2);
            int maxY = Math.max(y1, y2);
            int maxZ = Math.max(z1, z2);
            if (fillBlockCount(minX, minY, minZ, maxX, maxY, maxZ) > MAX_FILL_BLOCKS) {
                addMessage("Too many blocks. Maximum: " + MAX_FILL_BLOCKS + ".");
                return;
            }
            int changed = target.fillBlocks(minX, minY, minZ, maxX, maxY, maxZ, state);
            if (changed < 0) {
                addMessage("Coordinates are outside the world.");
                return;
            }
            addMessage("Filled " + changed + " block(s).");
        } catch (NumberFormatException exception) {
            addMessage("Coordinates must be whole numbers.");
        }
    }

    static BlockState resolveCommandBlockState(String raw) {
        String query = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return null;
        }
        if (query.startsWith("tinycraft:")) {
            query = "minecraft:" + query.substring("tinycraft:".length());
        } else if (query.indexOf(':') < 0) {
            query = "minecraft:" + query;
        } else if (!query.startsWith("minecraft:")) {
            return null;
        }
        BlockType type = BlockRegistry.typeByName(query);
        return type == null ? null : new BlockState(type);
    }

    static long fillBlockCount(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        long sizeX = (long) maxX - minX + 1L;
        long sizeY = (long) maxY - minY + 1L;
        long sizeZ = (long) maxZ - minZ + 1L;
        if (sizeX <= 0L || sizeY <= 0L || sizeZ <= 0L
            || sizeX > MAX_FILL_BLOCKS || sizeY > MAX_FILL_BLOCKS || sizeZ > MAX_FILL_BLOCKS) {
            return MAX_FILL_BLOCKS + 1L;
        }
        long area = sizeX * sizeY;
        if (area > MAX_FILL_BLOCKS) {
            return MAX_FILL_BLOCKS + 1L;
        }
        long volume = area * sizeZ;
        return volume > MAX_FILL_BLOCKS ? MAX_FILL_BLOCKS + 1L : volume;
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

    private static boolean isHiddenGiveItem(byte item) {
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
        target.summonMob(MobKind.ZOMBIE);
        addMessage("Spawned zombie.");
    }

    private void executeSummon(String[] parts, CommandTarget target) {
        if (parts.length != 2) {
            addMessage("Usage: /summon <entity>");
            return;
        }
        MobKind kind = resolveMobKind(parts[1]);
        if (kind == null || !target.summonMob(kind)) {
            addMessage("Unknown entity: " + parts[1]);
            return;
        }
        addMessage("Summoned " + parts[1] + ".");
    }

    static MobKind resolveMobKind(String raw) {
        String name = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        int namespace = name.indexOf(':');
        if (namespace >= 0) {
            String prefix = name.substring(0, namespace);
            if (!"minecraft".equals(prefix) && !"tinycraft".equals(prefix)) {
                return null;
            }
            name = name.substring(namespace + 1);
        }
        switch (name) {
            case "zombie": return MobKind.ZOMBIE;
            case "skeleton": return MobKind.SKELETON;
            case "pig": return MobKind.PIG;
            case "sheep": return MobKind.SHEEP;
            case "cow": return MobKind.COW;
            case "villager": return MobKind.VILLAGER;
            case "herring": return MobKind.HERRING;
            case "salmon": return MobKind.SALMON;
            default: return null;
        }
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
        return messages.get(messages.size() - 1).text;
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
