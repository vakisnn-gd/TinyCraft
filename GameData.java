import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

final class GameConfig {
    static final int WINDOW_WIDTH = 1280;
    static final int WINDOW_HEIGHT = 720;

    static final int CHUNK_SIZE = 16;
    static final int WORLD_MIN_Y = -64;
    static final int WORLD_HEIGHT = 384;
    static final int WORLD_MAX_Y = WORLD_MIN_Y + WORLD_HEIGHT - 1;
    static final int SECTION_HEIGHT = 16;
    static final int SECTION_COUNT = WORLD_HEIGHT / SECTION_HEIGHT;
    static final int WORLD_CHUNKS_X = 16;
    static final int WORLD_CHUNKS_Y = SECTION_COUNT;
    static final int WORLD_CHUNKS_Z = 16;
    static final int CHUNK_RENDER_DISTANCE = 32;
    static final int SPECTATOR_CHUNK_RENDER_DISTANCE = 12;
    static final int MIN_RENDER_DISTANCE = 2;
    static final int MAX_RENDER_DISTANCE_CHUNKS = 32;
    static final int WORLD_WIDTH = CHUNK_SIZE * WORLD_CHUNKS_X;
    static final int WORLD_DEPTH = CHUNK_SIZE * WORLD_CHUNKS_Z;
    static final int SURFACE_COLUMN_COUNT = WORLD_WIDTH * WORLD_DEPTH;
    static final String SAVE_ROOT_DIRECTORY = "saves";
    static final String SAVE_METADATA_FILE = "seed.txt";
    static final String SAVE_LEVEL_FILE = "level.json";
    static final String SAVE_CHUNKS_DIRECTORY = "chunks";
    static final String SAVE_REGION_DIRECTORY = "region";
    static final String SAVE_DIMENSIONS_DIRECTORY = "dimensions";
    static final String PARADISE_DIMENSION_DIRECTORY = "paradise";
    static final String SAVE_PLAYER_FILE = "player.dat";
    static final int DIMENSION_OVERWORLD = 0;
    static final int DIMENSION_PARADISE = 1;
    static final int REGION_SIZE_CHUNKS = 32;
    static final int REGION_CHUNK_COUNT = REGION_SIZE_CHUNKS * REGION_SIZE_CHUNKS;
    static final String REGION_FILE_EXTENSION = ".mcrx";
    static final int CHUNK_GENERATION_THREADS = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    static final int INITIAL_CHUNK_SYNC_RADIUS = 1;
    static final int SEA_LEVEL = 63;
    static final int SURFACE_Y = SEA_LEVEL;

    static final byte AIR = 0;
    static final byte GRASS = 1;
    static final byte DIRT = 2;
    static final byte COBBLESTONE = 3;
    static final byte BEDROCK = 4;
    static final byte IRON_ORE = 5;
    static final byte DIAMOND_ORE = 6;
    static final byte COAL_ORE = 7;
    static final byte ZOMBIE_SKIN = 8;
    static final byte ZOMBIE_SHIRT = 9;
    static final byte ZOMBIE_PANTS = 10;
    static final byte ZOMBIE_EYE = 11;
    static final byte WATER = 12;
    static final byte LAVA = 13;
    static final byte SAND = 14;
    static final byte OAK_LOG = 15;
    static final byte OAK_LEAVES = 16;
    static final byte TALL_GRASS = 17;
    static final byte RED_FLOWER = 18;
    static final byte YELLOW_FLOWER = 19;
    static final byte STONE = 20;
    static final byte OBSIDIAN = 21;
    static final byte WATER_SOURCE = 22;
    static final byte WATER_FLOWING = 23;
    static final byte LAVA_SOURCE = 24;
    static final byte LAVA_FLOWING = 25;
    static final byte CACTUS = 37;
    static final byte PINE_LOG = 38;
    static final byte PINE_LEAVES = 39;
    static final byte SNOW_LAYER = 40;
    static final byte GRAVEL = 41;
    static final byte SEAGRASS = 42;
    static final byte DEEPSLATE = 43;
    static final byte CLAY = 44;
    static final byte OAK_FENCE = 45;
    static final byte CHEST = 46;
    static final byte WHEAT_CROP = 47;
    static final byte RAIL = 48;
    static final byte OAK_DOOR = 53;
    static final byte FARMLAND = 54;
    static final byte TORCH = 55;
    static final byte STRUCTURE_BLOCK = 56;
    static final byte CRAFTING_TABLE = 57;
    static final byte FURNACE = 58;
    static final byte GLASS = 59;
    static final byte DEEPSLATE_IRON_ORE = 60;
    static final byte DEEPSLATE_DIAMOND_ORE = 61;
    static final byte DEEPSLATE_COAL_ORE = 62;
    static final byte RED_BED = 71;
    static final byte OAK_FENCE_GATE = 117;
    static final byte BIRCH_LOG = 118;
    static final byte BIRCH_LEAVES = 119;
    static final byte SNOW_BLOCK = 120;
    static final byte DEAD_BUSH = 121;
    static final byte PINE_PLANKS = 122;
    static final byte BIRCH_PLANKS = 123;
    static final byte OAK_STAIRS = 124;
    static final byte PINE_STAIRS = 125;
    static final byte BIRCH_STAIRS = 126;
    static final byte STONE_STAIRS = 127;
    static final byte COBBLESTONE_STAIRS = (byte) 128;
    static final byte KELP = (byte) 129;
    static final byte CARROT_CROP = (byte) 130;
    static final byte POTATO_CROP = (byte) 131;
    static final byte PARADISE_PORTAL = (byte) 138;

    static final int PARADISE_ORIGIN_X = 0;
    static final int PARADISE_ORIGIN_Z = 0;
    static final int PARADISE_SURFACE_Y = 128;
    static final int PARADISE_RADIUS_BLOCKS = 224;
    static final double PARADISE_PORTAL_SECONDS = 0.75;
    static final double PARADISE_PORTAL_COOLDOWN_SECONDS = 2.0;
    static final double PARADISE_PORTAL_RENDER_THICKNESS = 0.125;

    static final double PLAYER_RADIUS = 0.30;
    static final double PLAYER_HEIGHT = 1.80;
    static final double EYE_HEIGHT = 1.62;
    static final double PLAYER_SNEAK_HEIGHT = 1.50;
    static final double PLAYER_SNEAK_EYE_HEIGHT = 1.32;
    static final double SNEAK_SPEED_FACTOR = 0.34;
    static final double MAX_REACH = 4.8;
    static final double MAX_RENDER_DISTANCE = Math.hypot(MAX_RENDER_DISTANCE_CHUNKS * CHUNK_SIZE, WORLD_HEIGHT * 0.5);

    static final double WALK_SPEED = 6.45;
    static final double SPRINT_SPEED = 9.66;
    static final double CREATIVE_FLY_SPEED = 10.2;
    static final double JUMP_SPEED = 7.25;
    static final double GRAVITY = 18.0;
    static final double TERMINAL_VELOCITY = 24.0;
    static final double GAME_TICK_SECONDS = 0.05;
    static final double PHYSICS_TICK_SECONDS = GAME_TICK_SECONDS;
    static final double WATER_MOVE_FACTOR = 0.60;
    static final double WATER_SWIM_ACCELERATION = 7.20;
    static final double WATER_SINK_ACCELERATION = 4.70;
    static final double WATER_DESCEND_ACCELERATION = 3.50;
    static final double WATER_DRAG_PER_TICK = 0.98;
    static final double WATER_FLOW_PUSH = 1.70;
    static final double WATER_ENTITY_FLOW_PUSH = 1.10;
    static final double WATER_VERTICAL_FLOW_PUSH = 0.85;
    static final double MAX_PITCH = Math.toRadians(89.0);
    static final double CAMERA_STEP_HEIGHT = 0.65;
    static final double MAX_COLLISION_STEP = 0.45;
    static final double FOV_DEGREES = 70.0;
    static final double SPRINT_FOV_BOOST = 4.0;
    static final double DAY_LENGTH_SECONDS = 360.0;
    static final int RENDER_CHUNK_UNLOAD_PADDING = 2;
    static final int STAR_COUNT = 160;
    static final double STAR_RADIUS = MAX_RENDER_DISTANCE + 120.0;
    static final double STAR_BASE_SIZE = 0.55;
    static final double CLOUD_LAYER_BASE_HEIGHT = 250.0;
    static final double CLOUD_CELL_SIZE = 34.0;
    static final int CLOUD_RENDER_RADIUS = 6;
    static final double CLOUD_DRIFT_SPEED = 0.26;
    static final boolean ENABLE_DEBUG_LOGS = Boolean.getBoolean("tinycraft.debug");
    static final boolean ENABLE_FRAME_PROFILING = Boolean.getBoolean("tinycraft.profile");
    static final double FRAME_PROFILE_LOG_INTERVAL_SECONDS = 2.0;

    static final double ZOMBIE_RADIUS = 0.28;
    static final double ZOMBIE_HEIGHT = 1.80;
    static final double ZOMBIE_VIEW_DISTANCE = 20.0;
    static final double ZOMBIE_CHASE_SPEED = 1.9;
    static final double ZOMBIE_WANDER_SPEED = 0.8;
    static final double ZOMBIE_SIMPLE_CHASE_SPEED = 1.15;
    static final double ZOMBIE_TURN_SPEED = 7.5;
    static final double ZOMBIE_STEP_SPEED = 5.0;
    static final double ZOMBIE_ATTACK_RANGE = 1.35;
    static final double ZOMBIE_ATTACK_COOLDOWN = 1.15;
    static final double ZOMBIE_NEAR_AI_DISTANCE = 20.0;
    static final double ZOMBIE_MID_AI_DISTANCE = 50.0;
    static final double ZOMBIE_AGGRO_Y_LIMIT = 6.0;
    static final double ZOMBIE_RENDER_DISTANCE = 56.0;
    static final double ZOMBIE_SEPARATION_DISTANCE = 0.82;
    static final int ZOMBIE_ATTACK_DAMAGE = 1;
    static final int ZOMBIE_NEAR_AI_TICKS = 1;
    static final int ZOMBIE_MID_AI_TICKS = 5;
    static final int ZOMBIE_FAR_AI_TICKS = 20;
    static final double SUFFOCATION_INTERVAL = 1.0;
    static final int SUFFOCATION_DAMAGE = 1;
    static final int MAX_AIR_UNITS = 10;
    static final double AIR_UNIT_INTERVAL = 0.5;
    static final double AIR_RECOVERY_INTERVAL = 0.2;
    static final double DROWNING_DAMAGE_INTERVAL = 1.0;
    static final int DROWNING_DAMAGE = 1;
    static final double LAVA_DAMAGE_INTERVAL = 0.55;
    static final int LAVA_DAMAGE = 1;
    static final double FIRE_DAMAGE_INTERVAL = 1.0;
    static final int FIRE_DAMAGE = 1;
    // Вода может растекаться по горизонтали до 7 блоков от источника.
    static final int WATER_SPREAD_DISTANCE = 8;
    // Лава растекается медленнее и ближе: только до 4 блоков.
    static final int LAVA_SPREAD_DISTANCE = 4;
    static final int WATER_FLOW_TICKS = 1;
    static final int LAVA_FLOW_TICKS = 8;
    static final double WORLD_TICK_INTERVAL = GAME_TICK_SECONDS * WATER_FLOW_TICKS;
    static final int LAVA_FLOW_STEP_INTERVAL = Math.max(1, LAVA_FLOW_TICKS / WATER_FLOW_TICKS);
    static final int NATURAL_FLUID_DISTANCE = -2;
    static final int FLUID_SIMULATION_MAX_Y = WORLD_MAX_Y - 1;
    static final int ACTIVE_SIMULATION_CHUNK_DISTANCE = 3;
    static final int FLUID_SIMULATION_CHUNK_DISTANCE = 3;
    static final int MAX_HEALTH = 20;
    static final int MAX_HUNGER = 20;
    static final double HUNGER_SPRINT_DRAIN_SECONDS = 4.0;
    static final double HUNGER_STARVE_DAMAGE_INTERVAL = 4.0;
    static final double DROPPED_ITEM_RADIUS = 0.16;
    static final double DROPPED_ITEM_HEIGHT = 0.24;
    static final double DROPPED_ITEM_PICKUP_RADIUS = 1.35;
    static final double DROPPED_ITEM_GRAVITY = 13.0;
    static final double DROPPED_ITEM_DRAG = 0.92;
    static final double DROPPED_ITEM_WATER_DRAG = 0.86;
    static final double DROPPED_ITEM_BOUNCE = 0.18;
    static final double DROPPED_ITEM_DESPAWN_SECONDS = 300.0;
    static final double DROPPED_ITEM_PICKUP_DELAY_SECONDS = 0.75;
    static final double WALK_STEP_INTERVAL = 0.46;
    static final double SPRINT_STEP_INTERVAL = 0.34;
    static final double WATER_AMBIENT_MIN_INTERVAL = 2.2;
    static final double WATER_AMBIENT_MAX_INTERVAL = 5.8;
    static final int SPAWN_SEARCH_RADIUS = 16;
    static final int WORLD_MENU_VISIBLE_ROWS = 7;
    static final int WORLD_MENU_NAME_LIMIT = 24;
    static final int MENU_SCREEN_MAIN = 0;
    static final int MENU_SCREEN_SINGLEPLAYER = 1;
    static final int MENU_SCREEN_OPTIONS = 2;
    static final int MENU_SCREEN_CREATE_WORLD = 3;
    static final int MENU_SCREEN_RENAME_WORLD = 4;
    static final int MENU_SCREEN_MULTIPLAYER = 5;
    static final int MENU_SCREEN_OPEN_LAN = 6;
    static final int MENU_SCREEN_DISCONNECTED = 7;
    static final int INVENTORY_SCREEN_PLAYER = 0;
    static final int INVENTORY_SCREEN_WORKBENCH = 1;
    static final int INVENTORY_SCREEN_CHEST = 2;
    static final int INVENTORY_SCREEN_FURNACE = 3;

    static final String[] PAUSE_OPTIONS = {"Вернуться в игру", "Настройки...", "Открыть для LAN", "Сохранить и выйти в меню"};
    static final String[] DEATH_OPTIONS = {"Возродиться", "Выйти в меню"};
    static final String[] WORLD_MENU_ACTIONS = {"Одиночная игра", "Мультиплеер", "Настройки", "Выйти"};
    static final String[] SINGLEPLAYER_ACTIONS = {"Играть", "Создать", "Переимен.", "Удалить", "Назад"};
    static final String[] MULTIPLAYER_ACTIONS = {"Открыть мир", "Подключиться", "Назад"};
    static final String[] LAN_ACTIONS = {"Открыть мир для LAN", "Отмена"};
    static final String[] CREATE_WORLD_ACTIONS = {"Создать мир", "Отмена"};
    static final String[] RENAME_WORLD_ACTIONS = {"Переименовать", "Отмена"};
    static final String[] CREATIVE_TABS = {"Блоки", "Природа", "Инструменты", "Жидкости"};
    static final String[] GAME_MODE_OPTIONS = {"Выживание", "Творческий", "Наблюдатель"};
    static final String[] DIFFICULTY_OPTIONS = {"Мирная", "Легкая", "Обычная", "Сложная"};
    static final String[] PAUSE_OPTIONS_EN = {"Back to Game", "Options...", "Open to LAN", "Save and Quit to Title"};
    static final String[] DEATH_OPTIONS_EN = {"Respawn", "Exit to Menu"};
    static final String[] WORLD_MENU_ACTIONS_EN = {"Singleplayer", "Multiplayer", "Options", "Quit"};
    static final String[] SINGLEPLAYER_ACTIONS_EN = {"Play", "Create New", "Rename", "Delete", "Back"};
    static final String[] MULTIPLAYER_ACTIONS_EN = {"Open World", "Connect", "Back"};
    static final String[] LAN_ACTIONS_EN = {"Start LAN World", "Cancel"};
    static final String[] DISCONNECTED_ACTIONS = {"Back"};
    static final String[] DISCONNECTED_ACTIONS_EN = {"Back"};
    static final String[] CREATE_WORLD_ACTIONS_EN = {"Create New World", "Cancel"};
    static final String[] RENAME_WORLD_ACTIONS_EN = {"Rename", "Cancel"};
    static final String[] CREATIVE_TABS_EN = {"Blocks", "Nature", "Tools", "Fluids"};
    static final String[] GAME_MODE_OPTIONS_EN = {"Survival", "Creative", "Spectator"};
    static final String[] DIFFICULTY_OPTIONS_EN = {"Peaceful", "Easy", "Normal", "Hard"};
    static final String[] TERRAIN_PRESET_OPTIONS = {"\u041e\u0431\u044b\u0447\u043d\u044b\u0439", "\u0411\u043e\u043b\u044c\u0448\u0438\u0435 \u0431\u0438\u043e\u043c\u044b"};
    static final String[] TERRAIN_PRESET_OPTIONS_EN = {"Default", "Large Biomes"};
    public static final double REACH_DISTANCE = MAX_REACH;

    private GameConfig() {
    }

    static boolean isWorldCoordinateInside(int x, int y, int z) {
        return isWorldYInside(y);
    }

    static boolean isWorldYInside(int y) {
        return y >= WORLD_MIN_Y && y <= WORLD_MAX_Y;
    }

    static int sectionIndexForY(int worldY) {
        return Math.floorDiv(worldY - WORLD_MIN_Y, SECTION_HEIGHT);
    }

    static int sectionYForIndex(int sectionIndex) {
        return WORLD_MIN_Y + sectionIndex * SECTION_HEIGHT;
    }

    static int localYForWorldY(int worldY) {
        return Math.floorMod(worldY - WORLD_MIN_Y, SECTION_HEIGHT);
    }

    static boolean isChunkCoordinateInside(int chunkX, int chunkY, int chunkZ) {
        return chunkY >= 0 && chunkY < WORLD_CHUNKS_Y;
    }

    static boolean isChunkLocalCoordinateInside(int localX, int localY, int localZ) {
        return localX >= 0 && localX < CHUNK_SIZE
            && localY >= 0 && localY < CHUNK_SIZE
            && localZ >= 0 && localZ < CHUNK_SIZE;
    }

    static boolean isWaterBlock(byte block) {
        return block == WATER_SOURCE || block == WATER_FLOWING || block == SEAGRASS || block == KELP;
    }

    static boolean isLavaBlock(byte block) {
        return block == LAVA_SOURCE || block == LAVA_FLOWING;
    }

    static boolean isLiquidBlock(byte block) {
        return isWaterBlock(block) || isLavaBlock(block) || (Blocks.isLiquid(block) && block != WATER && block != LAVA);
    }

    static boolean isFluidSourceBlock(byte block) {
        return block == WATER_SOURCE || block == LAVA_SOURCE;
    }

    static boolean isFluidFlowingBlock(byte block) {
        return block == WATER_FLOWING || block == LAVA_FLOWING;
    }

    static byte placedBlockForItem(byte itemId) {
        if (itemId == InventoryItems.WHEAT_SEEDS) {
            return WHEAT_CROP;
        }
        if (itemId == InventoryItems.CARROT) {
            return CARROT_CROP;
        }
        if (itemId == InventoryItems.POTATO) {
            return POTATO_CROP;
        }
        if (itemId == InventoryItems.ITEM_WATER_BUCKET) {
            return WATER_SOURCE;
        }
        if (itemId == InventoryItems.ITEM_LAVA_BUCKET) {
            return LAVA_SOURCE;
        }
        if (itemId == WATER) {
            return WATER_SOURCE;
        }
        if (itemId == LAVA) {
            return LAVA_SOURCE;
        }
        return itemId;
    }

    static byte sourceBlockForFluid(byte blockOrItem) {
        if (blockOrItem == WATER || isWaterBlock(blockOrItem)) {
            return WATER_SOURCE;
        }
        if (blockOrItem == LAVA || isLavaBlock(blockOrItem)) {
            return LAVA_SOURCE;
        }
        return AIR;
    }

    static byte flowingBlockForFluid(byte blockOrItem) {
        if (blockOrItem == WATER || isWaterBlock(blockOrItem)) {
            return WATER_FLOWING;
        }
        if (blockOrItem == LAVA || isLavaBlock(blockOrItem)) {
            return LAVA_FLOWING;
        }
        return AIR;
    }

    static byte fluidItemForBlock(byte block) {
        if (block == SEAGRASS || block == KELP) {
            return WATER;
        }
        if (isWaterBlock(block)) {
            return WATER;
        }
        if (isLavaBlock(block)) {
            return LAVA;
        }
        return AIR;
    }

    static int fluidSpreadDistance(byte blockOrItem) {
        return (blockOrItem == WATER || isWaterBlock(blockOrItem)) ? WATER_SPREAD_DISTANCE : LAVA_SPREAD_DISTANCE;
    }

    static boolean isParadiseArea(double x, double z) {
        return Math.abs(x - PARADISE_ORIGIN_X) <= PARADISE_RADIUS_BLOCKS
            && Math.abs(z - PARADISE_ORIGIN_Z) <= PARADISE_RADIUS_BLOCKS;
    }

    static boolean isParadiseDimension(int dimensionId) {
        return dimensionId == DIMENSION_PARADISE;
    }

    static String[] pauseOptions() {
        return Settings.isRussian() ? PAUSE_OPTIONS : PAUSE_OPTIONS_EN;
    }

    static String[] deathOptions() {
        return Settings.isRussian() ? DEATH_OPTIONS : DEATH_OPTIONS_EN;
    }

    static String[] worldMenuActions() {
        return Settings.isRussian() ? WORLD_MENU_ACTIONS : WORLD_MENU_ACTIONS_EN;
    }

    static String[] singleplayerActions() {
        return Settings.isRussian() ? SINGLEPLAYER_ACTIONS : SINGLEPLAYER_ACTIONS_EN;
    }

    static String[] multiplayerActions() {
        return Settings.isRussian() ? MULTIPLAYER_ACTIONS : MULTIPLAYER_ACTIONS_EN;
    }

    static String[] lanActions() {
        return Settings.isRussian() ? LAN_ACTIONS : LAN_ACTIONS_EN;
    }

    static String[] disconnectedActions() {
        return Settings.isRussian() ? DISCONNECTED_ACTIONS : DISCONNECTED_ACTIONS_EN;
    }

    static String[] createWorldActions() {
        return Settings.isRussian() ? CREATE_WORLD_ACTIONS : CREATE_WORLD_ACTIONS_EN;
    }

    static String[] renameWorldActions() {
        return Settings.isRussian() ? RENAME_WORLD_ACTIONS : RENAME_WORLD_ACTIONS_EN;
    }

    static String[] creativeTabs() {
        return Settings.isRussian() ? CREATIVE_TABS : CREATIVE_TABS_EN;
    }

    static String[] gameModeOptions() {
        return Settings.isRussian() ? GAME_MODE_OPTIONS : GAME_MODE_OPTIONS_EN;
    }

    static String[] difficultyOptions() {
        return Settings.isRussian() ? DIFFICULTY_OPTIONS : DIFFICULTY_OPTIONS_EN;
    }

    static String[] terrainPresetOptions() {
        return Settings.isRussian() ? TERRAIN_PRESET_OPTIONS : TERRAIN_PRESET_OPTIONS_EN;
    }

}

enum TerrainPreset {
    LEGACY("legacy", 0),
    DEFAULT("default", 0),
    LARGE_BIOMES("large_biomes", 1);

    static final TerrainPreset NEW_WORLD_DEFAULT = DEFAULT;

    private final String metadataId;
    private final int createWorldOptionIndex;

    TerrainPreset(String metadataId, int createWorldOptionIndex) {
        this.metadataId = metadataId;
        this.createWorldOptionIndex = createWorldOptionIndex;
    }

    String metadataId() {
        return metadataId;
    }

    int createWorldOptionIndex() {
        return createWorldOptionIndex;
    }

    static TerrainPreset fromCreateWorldOption(int index) {
        return index == LARGE_BIOMES.createWorldOptionIndex ? LARGE_BIOMES : DEFAULT;
    }

    static TerrainPreset fromMetadata(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LEGACY;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (TerrainPreset preset : values()) {
            if (preset.metadataId.equals(normalized) || preset.name().toLowerCase(java.util.Locale.ROOT).equals(normalized)) {
                return preset;
            }
        }
        return LEGACY;
    }
}

enum ChunkGenerationStatus {
    EMPTY,
    BIOMES,
    NOISE,
    SURFACE,
    CARVERS,
    FLUIDS,
    FEATURES,
    FULL;

    static ChunkGenerationStatus fromOrdinal(int ordinal) {
        ChunkGenerationStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FULL;
    }
}

final class WorldInfo {
    final String name;
    final Path directory;
    final long seed;
    final long lastModifiedMillis;
    final int gameMode;
    final int difficulty;
    final TerrainPreset terrainPreset;

    WorldInfo(String name, Path directory, long seed, long lastModifiedMillis, int gameMode, int difficulty, TerrainPreset terrainPreset) {
        this.name = name;
        this.directory = directory;
        this.seed = seed;
        this.lastModifiedMillis = lastModifiedMillis;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.terrainPreset = terrainPreset == null ? TerrainPreset.LEGACY : terrainPreset;
    }
}

final class RuntimePaths {
    static final Path PROJECT_ROOT = detectProjectRoot();

    private RuntimePaths() {
    }

    static Path resolve(String first, String... more) {
        return PROJECT_ROOT.resolve(Paths.get(first, more)).normalize();
    }

    private static Path detectProjectRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path cwdRoot = projectRootFrom(cwd);
        if (cwdRoot != null) {
            return cwdRoot;
        }

        try {
            Path classPath = Paths.get(RuntimePaths.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path classRoot = projectRootFrom(classPath);
            if (classRoot != null) {
                return classRoot;
            }
        } catch (Exception ignored) {
        }

        return cwd;
    }

    private static Path projectRootFrom(Path path) {
        if (looksLikeProjectRoot(path)) {
            return path;
        }
        Path fileName = path.getFileName();
        Path parent = path.getParent();
        if (fileName != null && parent != null && "out".equalsIgnoreCase(fileName.toString()) && looksLikeProjectRoot(parent)) {
            return parent;
        }
        return null;
    }

    private static boolean looksLikeProjectRoot(Path path) {
        return java.nio.file.Files.isRegularFile(path.resolve("TinyCraft.java"))
            && java.nio.file.Files.isDirectory(path.resolve("lib"));
    }
}

final class Settings {
    static double mouseSensitivity = 0.00081;
    static double mouseVerticalFactor = 0.7407407407;
    static int inventoryUiSize = 2;
    static int graphicsQuality = 0;
    static int brightness = 50;
    static int masterVolume = 80;
    static int savedRenderDistance = GameConfig.CHUNK_RENDER_DISTANCE;
    static int savedFovDegrees = (int) GameConfig.FOV_DEGREES;
    static int savedMaxFps = 144;
    static String language = "ru";

    private Settings() {
    }

    static float inventoryUiScale() {
        return 0.86f + clampInventoryUiSize(inventoryUiSize) * 0.16f;
    }

    static void load() {
        java.nio.file.Path path = RuntimePaths.resolve("options.txt");
        if (!java.nio.file.Files.isRegularFile(path)) {
            return;
        }
        try {
            for (String line : java.nio.file.Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("inventoryUiSize=")) {
                    inventoryUiSize = clampInventoryUiSize(Integer.parseInt(trimmed.substring(16)));
                } else if (trimmed.startsWith("graphicsQuality=")) {
                    graphicsQuality = clampGraphicsQuality(Integer.parseInt(trimmed.substring(16)));
                } else if (trimmed.startsWith("brightness=")) {
                    brightness = clampPercent(Integer.parseInt(trimmed.substring(11)));
                } else if (trimmed.startsWith("masterVolume=")) {
                    masterVolume = clampPercent(Integer.parseInt(trimmed.substring(13)));
                } else if (trimmed.startsWith("renderDistance=")) {
                    savedRenderDistance = clamp(Integer.parseInt(trimmed.substring(15)), GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
                } else if (trimmed.startsWith("fov=")) {
                    savedFovDegrees = clamp(Integer.parseInt(trimmed.substring(4)), 55, 100);
                } else if (trimmed.startsWith("maxFps=")) {
                    savedMaxFps = clamp(Integer.parseInt(trimmed.substring(7)), 30, 240);
                } else if (trimmed.startsWith("language=")) {
                    String value = trimmed.substring(9).trim().toLowerCase(java.util.Locale.ROOT);
                    language = "en".equals(value) ? "en" : "ru";
                }
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            inventoryUiSize = clampInventoryUiSize(inventoryUiSize);
            graphicsQuality = clampGraphicsQuality(graphicsQuality);
            brightness = clampPercent(brightness);
            masterVolume = clampPercent(masterVolume);
            savedRenderDistance = clamp(savedRenderDistance, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
            savedFovDegrees = clamp(savedFovDegrees, 55, 100);
            savedMaxFps = clamp(savedMaxFps, 30, 240);
        }
    }

    static void save(int renderDistance, int fovDegrees) {
        save(renderDistance, fovDegrees, savedMaxFps);
    }

    static void save(int renderDistance, int fovDegrees, int maxFps) {
        savedRenderDistance = clamp(renderDistance, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
        savedFovDegrees = clamp(fovDegrees, 55, 100);
        savedMaxFps = clamp(maxFps, 30, 240);
        inventoryUiSize = clampInventoryUiSize(inventoryUiSize);
        graphicsQuality = clampGraphicsQuality(graphicsQuality);
        brightness = clampPercent(brightness);
        masterVolume = clampPercent(masterVolume);
        String text = "renderDistance=" + savedRenderDistance + System.lineSeparator()
            + "fov=" + savedFovDegrees + System.lineSeparator()
            + "maxFps=" + savedMaxFps + System.lineSeparator()
            + "inventoryUiSize=" + inventoryUiSize + System.lineSeparator()
            + "graphicsQuality=" + graphicsQuality + System.lineSeparator()
            + "brightness=" + brightness + System.lineSeparator()
            + "masterVolume=" + masterVolume + System.lineSeparator()
            + "language=" + language + System.lineSeparator();
        try {
            java.nio.file.Files.write(RuntimePaths.resolve("options.txt"), text.getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
        }
    }

    private static int clampInventoryUiSize(int value) {
        return clamp(value, 1, 4);
    }

    private static int clampGraphicsQuality(int value) {
        return clamp(value, 0, 1);
    }

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    static boolean goodGraphics() {
        return graphicsQuality >= 1;
    }

    static float brightnessMultiplier() {
        return 0.72f + clampPercent(brightness) * 0.0076f;
    }

    static float masterVolumeScale() {
        return clampPercent(masterVolume) / 100.0f;
    }

    static boolean isRussian() {
        return !"en".equals(language);
    }

    static void toggleLanguage() {
        language = isRussian() ? "en" : "ru";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

enum Face {
    WEST(0.80f),
    EAST(0.80f),
    TOP(1.00f),
    BOTTOM(0.56f),
    NORTH(0.72f),
    SOUTH(0.72f);

    final float brightness;

    Face(float brightness) {
        this.brightness = brightness;
    }
}

final class HotbarConfig {
    static final byte[] SLOT_BLOCKS = {
        GameConfig.GRASS,
        GameConfig.COBBLESTONE,
        GameConfig.DEEPSLATE,
        GameConfig.DIRT,
        GameConfig.SAND,
        GameConfig.GRAVEL,
        GameConfig.CLAY,
        GameConfig.OAK_LOG,
        GameConfig.CRAFTING_TABLE,
        GameConfig.CHEST,
        GameConfig.FURNACE,
        GameConfig.RED_BED,
        GameConfig.GLASS,
        GameConfig.DEEPSLATE_COAL_ORE,
        GameConfig.DEEPSLATE_IRON_ORE,
        GameConfig.DEEPSLATE_DIAMOND_ORE,
        GameConfig.OAK_LEAVES,
        GameConfig.PINE_LOG,
        GameConfig.PINE_LEAVES,
        GameConfig.BIRCH_LOG,
        GameConfig.BIRCH_LEAVES,
        GameConfig.SNOW_BLOCK,
        GameConfig.CACTUS,
        GameConfig.SNOW_LAYER,
        GameConfig.DEAD_BUSH,
        GameConfig.SEAGRASS,
        GameConfig.KELP,
        GameConfig.PINE_PLANKS,
        GameConfig.BIRCH_PLANKS,
        GameConfig.OAK_STAIRS,
        GameConfig.PINE_STAIRS,
        GameConfig.BIRCH_STAIRS,
        GameConfig.STONE_STAIRS,
        GameConfig.COBBLESTONE_STAIRS,
        InventoryItems.ITEM_WATER_BUCKET,
        InventoryItems.ITEM_LAVA_BUCKET,
        GameConfig.DIAMOND_ORE
    };

    private HotbarConfig() {
    }
}

final class PlayerState extends Entity {
    static final double DEFAULT_YAW = 0.0;
    static final double DEFAULT_PITCH = -0.10;

    double yaw = DEFAULT_YAW;
    double pitch = DEFAULT_PITCH;
    double cameraBobPhase = 0.0;
    double cameraBobAmount = 0.0;
    double handSwingTimer = 0.0;
    double suffocationTimer = 0.0;
    double hungerDrainTimer = 0.0;
    double hungerDamageTimer = 0.0;
    double hungerRegenTimer = 0.0;
    double hunger = GameConfig.MAX_HUNGER;
    int armorProtection = 0;
    boolean headInWater;
    boolean wasInLiquid;
    boolean creativeMode;
    boolean spectatorMode;
    boolean flightEnabled;
    boolean sneaking;
    boolean hasCustomSpawn;
    double spawnX;
    double spawnY;
    double spawnZ;
    boolean hasParadiseReturn;
    double paradiseReturnX;
    double paradiseReturnY;
    double paradiseReturnZ;
    int paradiseReturnDimensionId;
    double paradisePortalTimer;
    double paradisePortalCooldown;
    int dimensionId;

    PlayerState() {
        super(0.5, GameConfig.SURFACE_Y + 1.0, 0.5, GameConfig.MAX_HEALTH);
    }

    @Override
    double radius() {
        return GameConfig.PLAYER_RADIUS;
    }

    @Override
    double height() {
        return sneaking ? GameConfig.PLAYER_SNEAK_HEIGHT : GameConfig.PLAYER_HEIGHT;
    }

    double eyeHeight() {
        return sneaking ? GameConfig.PLAYER_SNEAK_EYE_HEIGHT : GameConfig.EYE_HEIGHT;
    }
}

final class RemotePlayerState extends Entity {
    final java.util.UUID uuid;
    String name;
    double yaw = PlayerState.DEFAULT_YAW;
    double pitch = PlayerState.DEFAULT_PITCH;
    byte heldItem = GameConfig.AIR;
    boolean sneaking;
    boolean spectatorMode;
    long lastUpdateMillis;

    RemotePlayerState(java.util.UUID uuid, String name) {
        super(0.5, GameConfig.SURFACE_Y + 1.0, 0.5, GameConfig.MAX_HEALTH);
        this.uuid = uuid;
        this.name = name == null ? "Player" : name;
        this.lastUpdateMillis = System.currentTimeMillis();
    }

    @Override
    double radius() {
        return GameConfig.PLAYER_RADIUS;
    }

    @Override
    double height() {
        return sneaking ? GameConfig.PLAYER_SNEAK_HEIGHT : GameConfig.PLAYER_HEIGHT;
    }
}

final class Chunk {
    static final int SIZE = GameConfig.CHUNK_SIZE;
    static final int VOLUME = SIZE * SIZE * SIZE;

    final int chunkX;
    final int chunkY;
    final int chunkZ;
    private final ArrayList<BlockState> palette = new ArrayList<>();
    private final int[] blockStateIndices = new int[VOLUME];
    private final byte[] fluidDistance = new byte[VOLUME];
    private int nonAirBlockCount;
    private int version;
    private int cachedSnapshotVersion = -1;
    private ChunkSectionSnapshot cachedSnapshot;

    Chunk(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        palette.add(Blocks.stateFromLegacyId(GameConfig.AIR));
        Arrays.fill(fluidDistance, (byte) -1);
    }

    synchronized ChunkSectionSnapshot snapshot() {
        if (cachedSnapshot != null && cachedSnapshotVersion == version) {
            return cachedSnapshot;
        }
        BlockState[] states = new BlockState[VOLUME];
        byte[] blocks = new byte[VOLUME];
        byte[] distances = new byte[VOLUME];
        for (int index = 0; index < VOLUME; index++) {
            BlockState state = blockStateAtIndexUnchecked(index);
            states[index] = state;
            blocks[index] = Blocks.legacyIdFromState(state);
            distances[index] = fluidDistance[index];
        }
        cachedSnapshot = new ChunkSectionSnapshot(chunkX, chunkY, chunkZ, blocks, states, distances, nonAirBlockCount, version);
        cachedSnapshotVersion = version;
        return cachedSnapshot;
    }

    synchronized byte getBlockLocal(int localX, int localY, int localZ) {
        return Blocks.legacyIdFromState(getBlockStateLocal(localX, localY, localZ));
    }

    synchronized void setBlockLocal(int localX, int localY, int localZ, byte block) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return;
        }
        setBlockStateAtIndex(index(localX, localY, localZ), Blocks.stateFromLegacyId(block));
    }

    synchronized BlockState getBlockStateLocal(int localX, int localY, int localZ) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return Blocks.stateFromLegacyId(GameConfig.AIR);
        }
        return getBlockStateAtIndex(index(localX, localY, localZ));
    }

    synchronized void setBlockStateLocal(int localX, int localY, int localZ, BlockState state) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return;
        }
        setBlockStateAtIndex(index(localX, localY, localZ), state);
    }

    synchronized int getFluidDistanceLocal(int localX, int localY, int localZ) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return -1;
        }
        return fluidDistance[index(localX, localY, localZ)];
    }

    synchronized void setFluidDistanceLocal(int localX, int localY, int localZ, int distance) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return;
        }
        int index = index(localX, localY, localZ);
        byte next = (byte) distance;
        if (fluidDistance[index] != next) {
            fluidDistance[index] = next;
            version++;
        }
    }

    synchronized boolean isEmpty() {
        return nonAirBlockCount == 0;
    }

    synchronized byte getBlockAtIndex(int index) {
        return Blocks.legacyIdFromState(getBlockStateAtIndex(index));
    }

    synchronized BlockState getBlockStateAtIndex(int index) {
        if (index < 0 || index >= VOLUME) {
            return Blocks.stateFromLegacyId(GameConfig.AIR);
        }
        return blockStateAtIndexUnchecked(index);
    }

    synchronized int getFluidDistanceAtIndex(int index) {
        if (index < 0 || index >= VOLUME) {
            return -1;
        }
        return fluidDistance[index];
    }

    synchronized void setSerializedCell(int index, byte block, int distance) {
        setSerializedCellState(index, Blocks.stateFromLegacyId(block), distance);
    }

    synchronized void setSerializedCellState(int index, BlockState state, int distance) {
        if (index < 0 || index >= VOLUME) {
            return;
        }

        int previousVersion = version;
        setBlockStateAtIndex(index, state);
        byte nextDistance = state.type.isLiquid() ? (byte) distance : (byte) -1;
        if (fluidDistance[index] != nextDistance) {
            fluidDistance[index] = nextDistance;
            if (version == previousVersion) {
                version++;
            }
        }
    }

    synchronized void setBlockStateAtIndex(int index, BlockState state) {
        if (index < 0 || index >= VOLUME) {
            return;
        }
        BlockState normalized = state == null ? Blocks.stateFromLegacyId(GameConfig.AIR) : state;
        BlockState previous = getBlockStateAtIndex(index);
        if (sameBlockState(previous, normalized)) {
            return;
        }

        boolean previousAir = previous.type.isAir();
        boolean nextAir = normalized.type.isAir();
        if (previousAir && !nextAir) {
            nonAirBlockCount++;
        } else if (!previousAir && nextAir) {
            nonAirBlockCount--;
        }

        blockStateIndices[index] = paletteIndexFor(normalized);
        if (!normalized.type.isLiquid()) {
            fluidDistance[index] = -1;
        }
        version++;
    }

    private BlockState blockStateAtIndexUnchecked(int index) {
        int paletteIndex = blockStateIndices[index];
        if (paletteIndex < 0 || paletteIndex >= palette.size()) {
            return Blocks.stateFromLegacyId(GameConfig.AIR);
        }
        return palette.get(paletteIndex);
    }

    static boolean validatePaletteForDebug() {
        Chunk chunk = new Chunk(0, 0, 0);
        if (!chunk.isEmpty()) {
            return false;
        }
        chunk.setBlockStateAtIndex(0, Blocks.stateFromLegacyId(GameConfig.STONE));
        if (chunk.isEmpty() || chunk.getBlockLocal(0, 0, 0) != GameConfig.STONE) {
            return false;
        }
        BlockState state = chunk.getBlockStateLocal(0, 0, 0);
        if (!"minecraft:stone".equals(state.type.namespacedId)) {
            return false;
        }
        chunk.setBlockStateAtIndex(0, Blocks.stateFromLegacyId(GameConfig.STONE));
        if (chunk.nonAirBlockCount != 1) {
            return false;
        }
        chunk.setBlockLocal(0, 0, 0, GameConfig.AIR);
        return chunk.isEmpty() && chunk.nonAirBlockCount == 0;
    }

    private int paletteIndexFor(BlockState state) {
        for (int paletteIndex = 0; paletteIndex < palette.size(); paletteIndex++) {
            if (sameBlockState(palette.get(paletteIndex), state)) {
                return paletteIndex;
            }
        }
        palette.add(state);
        return palette.size() - 1;
    }

    private boolean sameBlockState(BlockState left, BlockState right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.type.numericId == right.type.numericId && left.data == right.data;
    }

    private int index(int localX, int localY, int localZ) {
        return (localY * SIZE + localZ) * SIZE + localX;
    }
}

final class ChunkSectionSnapshot {
    final int chunkX;
    final int chunkY;
    final int chunkZ;
    private final byte[] blocks;
    private final BlockState[] states;
    private final byte[] fluidDistance;
    private final int nonAirBlockCount;
    final int version;

    ChunkSectionSnapshot(int chunkX, int chunkY, int chunkZ, byte[] blocks, BlockState[] states, byte[] fluidDistance, int nonAirBlockCount, int version) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
        this.blocks = blocks;
        this.states = states;
        this.fluidDistance = fluidDistance;
        this.nonAirBlockCount = nonAirBlockCount;
        this.version = version;
    }

    boolean isEmpty() {
        return nonAirBlockCount == 0;
    }

    byte getBlockLocal(int localX, int localY, int localZ) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return GameConfig.AIR;
        }
        return blocks[index(localX, localY, localZ)];
    }

    BlockState getBlockStateLocal(int localX, int localY, int localZ) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return Blocks.stateFromLegacyId(GameConfig.AIR);
        }
        BlockState state = states[index(localX, localY, localZ)];
        return state == null ? Blocks.stateFromLegacyId(GameConfig.AIR) : state;
    }

    int getFluidDistanceLocal(int localX, int localY, int localZ) {
        if (!GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)) {
            return -1;
        }
        return fluidDistance[index(localX, localY, localZ)];
    }

    private int index(int localX, int localY, int localZ) {
        return (localY * Chunk.SIZE + localZ) * Chunk.SIZE + localX;
    }
}

final class ChunkMesh {
    final int chunkX;
    final int chunkY;
    final int chunkZ;
    int opaqueVaoId;
    int opaqueVboId;
    int opaqueVertexCount;
    int transparentVaoId;
    int transparentVboId;
    int transparentVertexCount;
    boolean hasOpaqueGeometry;
    boolean hasTransparentGeometry;
    boolean resident;
    long revealStartNanos;

    ChunkMesh(int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;
    }
}

final class RayHit {
    final int x;
    final int y;
    final int z;
    final int previousX;
    final int previousY;
    final int previousZ;

    RayHit(int x, int y, int z, int previousX, int previousY, int previousZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.previousX = previousX;
        this.previousY = previousY;
        this.previousZ = previousZ;
    }
}

enum MobKind {
    ZOMBIE,
    SKELETON,
    PIG,
    SHEEP,
    COW,
    VILLAGER,
    HERRING,
    SALMON
}

final class MobEntity extends Entity {
    final MobKind kind;
    final double homeX;
    final double homeZ;
    double bodyYaw;
    double targetBodyYaw;
    double wanderDirection;
    double wanderTime;
    boolean resting;
    int directionIndex;
    double walkCycle;
    double growlCooldown = 3.0;
    double attackCooldown = 0.0;
    double hurtCooldown = 0.0;
    double fleeTimer = 0.0;
    double loveTimer = 0.0;
    double breedCooldown = 0.0;
    double babyAge = 0.0;
    int aiTickOffset;
    double stepTargetY = Double.NaN;
    boolean growlQueued;
    boolean splashQueued;
    final Random random;

    MobEntity(double x, double y, double z, double homeX, double homeZ, Random seedRandom) {
        this(MobKind.ZOMBIE, x, y, z, homeX, homeZ, seedRandom);
    }

    MobEntity(MobKind kind, double x, double y, double z, double homeX, double homeZ, Random seedRandom) {
        super(x, y, z, maxHealthFor(kind));
        this.kind = kind == null ? MobKind.ZOMBIE : kind;
        this.homeX = homeX;
        this.homeZ = homeZ;
        this.random = new Random(seedRandom.nextLong());
        this.directionIndex = this.random.nextInt(4);
        this.bodyYaw = directionIndex * Math.PI * 0.5;
        this.targetBodyYaw = bodyYaw;
        this.wanderDirection = bodyYaw;
        this.wanderTime = 2.0 + this.random.nextDouble() * 2.0;
        this.resting = false;
        this.aiTickOffset = this.random.nextInt(Math.max(1, GameConfig.ZOMBIE_FAR_AI_TICKS));
        this.isGrounded = true;
    }

    private static int maxHealthFor(MobKind kind) {
        if (kind == MobKind.HERRING || kind == MobKind.SALMON) {
            return 4;
        }
        if (kind == MobKind.COW || kind == MobKind.SHEEP || kind == MobKind.PIG) {
            return 10;
        }
        return 20;
    }

    @Override
    double radius() {
        if (kind == MobKind.HERRING) {
            return 0.18;
        }
        if (kind == MobKind.SALMON) {
            return 0.24;
        }
        if (isPassiveBaby()) {
            return GameConfig.ZOMBIE_RADIUS * 0.58;
        }
        return GameConfig.ZOMBIE_RADIUS;
    }

    @Override
    double height() {
        if (kind == MobKind.HERRING) {
            return 0.24;
        }
        if (kind == MobKind.SALMON) {
            return 0.32;
        }
        if (isPassiveBaby()) {
            return GameConfig.ZOMBIE_HEIGHT * 0.58;
        }
        return GameConfig.ZOMBIE_HEIGHT;
    }

    boolean isBaby() {
        return babyAge > 0.0;
    }

    private boolean isPassiveBaby() {
        return isBaby() && (kind == MobKind.COW || kind == MobKind.SHEEP || kind == MobKind.PIG);
    }
}

final class DroppedItem extends Entity {
    final byte itemId;
    int count;
    int durabilityDamage;
    double ageSeconds;
    double pickupDelaySeconds = GameConfig.DROPPED_ITEM_PICKUP_DELAY_SECONDS;
    double spinDegrees;
    int physicsTickOffset;

    DroppedItem(byte itemId, int count, double x, double y, double z) {
        super(x, y, z, 1);
        this.itemId = itemId;
        this.count = count;
        this.durabilityDamage = 0;
    }

    @Override
    double radius() {
        return GameConfig.DROPPED_ITEM_RADIUS;
    }

    @Override
    double height() {
        return GameConfig.DROPPED_ITEM_HEIGHT;
    }
}

final class FallingBlock extends Entity {
    final byte blockId;
    final int blockX;
    final int blockY;
    final int blockZ;

    FallingBlock(byte blockId, int blockX, int blockY, int blockZ) {
        super(blockX + 0.5, blockY, blockZ + 0.5, 1);
        this.blockId = blockId;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    @Override
    double radius() {
        return 0.49;
    }

    @Override
    double height() {
        return 1.0;
    }
}
