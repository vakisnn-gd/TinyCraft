import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

final class BlockType {
    final int numericId;
    final String namespacedId;
    final String displayName;
    final boolean solid;
    final boolean opaque;
    final boolean liquid;
    final boolean replaceable;
    final boolean plant;
    final boolean gravityAffected;
    final int lightEmission;

    BlockType(int numericId, String namespacedId, String displayName,
              boolean solid, boolean opaque, boolean liquid, boolean replaceable,
              boolean plant, boolean gravityAffected, int lightEmission) {
        this.numericId = numericId;
        this.namespacedId = namespacedId;
        this.displayName = displayName;
        this.solid = solid;
        this.opaque = opaque;
        this.liquid = liquid;
        this.replaceable = replaceable;
        this.plant = plant;
        this.gravityAffected = gravityAffected;
        this.lightEmission = lightEmission;
    }

    boolean isAir() {
        return numericId == GameConfig.AIR;
    }

    boolean isSolid() {
        return solid;
    }

    boolean isOpaque() {
        return opaque;
    }

    boolean isLiquid() {
        return liquid;
    }

    boolean isReplaceable() {
        return replaceable;
    }

    boolean isPlant() {
        return plant;
    }

    boolean hasGravity() {
        return gravityAffected;
    }
}

final class BlockState {
    final BlockType type;
    final int data;

    BlockState(BlockType type) {
        this(type, 0);
    }

    BlockState(BlockType type, int data) {
        this.type = type == null ? BlockRegistry.air() : type;
        this.data = data;
    }

    BlockType type() {
        return type;
    }

    int data() {
        return data;
    }
}

final class BlockRegistry {
    private static final HashMap<Integer, BlockType> TYPES_BY_NUMERIC_ID = new HashMap<>();
    private static final HashMap<String, BlockType> TYPES_BY_NAME = new HashMap<>();
    private static final HashMap<Integer, BlockState> STATES_BY_NUMERIC_ID = new HashMap<>();

    static {
        register(GameConfig.AIR, "minecraft:air", "Air", false, false, false, true, false, false, 0);
        register(GameConfig.GRASS, "minecraft:grass_block", "Grass Block", true, true, false, false, false, false, 0);
        register(GameConfig.DIRT, "minecraft:dirt", "Dirt", true, true, false, false, false, false, 0);
        register(GameConfig.COBBLESTONE, "minecraft:cobblestone", "Cobblestone", true, true, false, false, false, false, 0);
        register(GameConfig.BEDROCK, "minecraft:bedrock", "Bedrock", true, true, false, false, false, false, 0);
        register(GameConfig.IRON_ORE, "minecraft:iron_ore", "Iron Ore", true, true, false, false, false, false, 0);
        register(GameConfig.DIAMOND_ORE, "minecraft:diamond_ore", "Diamond Ore", true, true, false, false, false, false, 0);
        register(GameConfig.COAL_ORE, "minecraft:coal_ore", "Coal Ore", true, true, false, false, false, false, 0);
        register(GameConfig.ZOMBIE_SKIN, "tiny:zombie_skin", "Zombie Skin", true, true, false, false, false, false, 0);
        register(GameConfig.ZOMBIE_SHIRT, "tiny:zombie_shirt", "Zombie Shirt", true, true, false, false, false, false, 0);
        register(GameConfig.ZOMBIE_PANTS, "tiny:zombie_pants", "Zombie Pants", true, true, false, false, false, false, 0);
        register(GameConfig.ZOMBIE_EYE, "tiny:zombie_eye", "Zombie Eye", true, true, false, false, false, false, 0);
        register(GameConfig.WATER, "minecraft:water", "Water", false, false, true, false, false, false, 0);
        register(GameConfig.LAVA, "minecraft:lava", "Lava", false, false, true, false, false, false, 15);
        register(GameConfig.SAND, "minecraft:sand", "Sand", true, true, false, false, false, true, 0);
        register(GameConfig.OAK_LOG, "minecraft:oak_log", "Oak Log", true, true, false, false, false, false, 0);
        register(GameConfig.OAK_LEAVES, "minecraft:oak_leaves", "Oak Leaves", true, true, false, false, false, false, 0);
        register(GameConfig.TALL_GRASS, "minecraft:tall_grass", "Tall Grass", false, false, false, true, true, false, 0);
        register(GameConfig.RED_FLOWER, "minecraft:red_flower", "Red Flower", false, false, false, true, true, false, 0);
        register(GameConfig.YELLOW_FLOWER, "minecraft:yellow_flower", "Yellow Flower", false, false, false, true, true, false, 0);
        register(GameConfig.STONE, "minecraft:stone", "Stone", true, true, false, false, false, false, 0);
        register(GameConfig.OBSIDIAN, "minecraft:obsidian", "Obsidian", true, true, false, false, false, false, 0);
        register(GameConfig.WATER_SOURCE, "minecraft:water_source", "Water Source", false, false, true, false, false, false, 0);
        register(GameConfig.WATER_FLOWING, "minecraft:water_flowing", "Flowing Water", false, false, true, false, false, false, 0);
        register(GameConfig.LAVA_SOURCE, "minecraft:lava_source", "Lava Source", false, false, true, false, false, false, 15);
        register(GameConfig.LAVA_FLOWING, "minecraft:lava_flowing", "Flowing Lava", false, false, true, false, false, false, 15);
        register(InventoryItems.OAK_PLANKS, "minecraft:oak_planks", "Oak Planks", true, true, false, false, false, false, 0);
        register(GameConfig.CACTUS, "minecraft:cactus", "Cactus", true, true, false, false, false, false, 0);
        register(GameConfig.PINE_LOG, "minecraft:pine_log", "Pine Log", true, true, false, false, false, false, 0);
        register(GameConfig.PINE_LEAVES, "minecraft:pine_leaves", "Pine Leaves", true, true, false, false, false, false, 0);
        register(GameConfig.SNOW_LAYER, "minecraft:snow_layer", "Snow Layer", false, false, false, true, false, false, 0);
        register(GameConfig.BIRCH_LOG, "minecraft:birch_log", "Birch Log", true, true, false, false, false, false, 0);
        register(GameConfig.BIRCH_LEAVES, "minecraft:birch_leaves", "Birch Leaves", true, true, false, false, false, false, 0);
        register(GameConfig.SNOW_BLOCK, "minecraft:snow_block", "Snow Block", true, true, false, false, false, false, 0);
        register(GameConfig.DEAD_BUSH, "minecraft:dead_bush", "Dead Bush", false, false, false, true, true, false, 0);
        register(GameConfig.PINE_PLANKS, "minecraft:spruce_planks", "Spruce Planks", true, true, false, false, false, false, 0);
        register(GameConfig.BIRCH_PLANKS, "minecraft:birch_planks", "Birch Planks", true, true, false, false, false, false, 0);
        register(GameConfig.OAK_STAIRS, "minecraft:oak_stairs", "Oak Stairs", true, false, false, false, false, false, 0);
        register(GameConfig.PINE_STAIRS, "minecraft:spruce_stairs", "Spruce Stairs", true, false, false, false, false, false, 0);
        register(GameConfig.BIRCH_STAIRS, "minecraft:birch_stairs", "Birch Stairs", true, false, false, false, false, false, 0);
        register(GameConfig.STONE_STAIRS, "minecraft:stone_stairs", "Stone Stairs", true, false, false, false, false, false, 0);
        register(GameConfig.COBBLESTONE_STAIRS, "minecraft:cobblestone_stairs", "Cobblestone Stairs", true, false, false, false, false, false, 0);
        register(GameConfig.GRAVEL, "minecraft:gravel", "Gravel", true, true, false, false, false, true, 0);
        register(GameConfig.SEAGRASS, "minecraft:seagrass", "Seagrass", false, false, false, true, true, false, 0);
        register(GameConfig.KELP, "minecraft:kelp", "Kelp", false, false, false, true, true, false, 0);
        register(GameConfig.DEEPSLATE, "minecraft:deepslate", "Deepslate", true, true, false, false, false, false, 0);
        register(GameConfig.CLAY, "minecraft:clay", "Clay", true, true, false, false, false, false, 0);
        register(GameConfig.OAK_FENCE, "minecraft:oak_fence", "Oak Fence", true, false, false, false, false, false, 0);
        register(GameConfig.CHEST, "minecraft:chest", "Chest", true, true, false, false, false, false, 0);
        register(GameConfig.WHEAT_CROP, "minecraft:wheat", "Wheat", false, false, false, true, true, false, 0);
        register(GameConfig.CARROT_CROP, "minecraft:carrots", "Carrots", false, false, false, true, true, false, 0);
        register(GameConfig.POTATO_CROP, "minecraft:potatoes", "Potatoes", false, false, false, true, true, false, 0);
        register(GameConfig.RAIL, "minecraft:rail", "Rail", false, false, false, true, false, false, 0);
        register(GameConfig.OAK_DOOR, "minecraft:oak_door", "Oak Door", false, false, false, false, false, false, 0);
        register(GameConfig.FARMLAND, "minecraft:farmland", "Farmland", true, true, false, false, false, false, 0);
        register(GameConfig.TORCH, "minecraft:torch", "Torch", false, false, false, true, true, false, 14);
        register(GameConfig.STRUCTURE_BLOCK, "minecraft:structure_block", "Structure Block", true, true, false, false, false, false, 0);
        register(GameConfig.CRAFTING_TABLE, "minecraft:crafting_table", "Crafting Table", true, true, false, false, false, false, 0);
        register(GameConfig.FURNACE, "minecraft:furnace", "Furnace", true, true, false, false, false, false, 0);
        register(GameConfig.GLASS, "minecraft:glass", "Glass", true, false, false, false, false, false, 0);
        register(GameConfig.DEEPSLATE_IRON_ORE, "minecraft:deepslate_iron_ore", "Deepslate Iron Ore", true, true, false, false, false, false, 0);
        register(GameConfig.DEEPSLATE_DIAMOND_ORE, "minecraft:deepslate_diamond_ore", "Deepslate Diamond Ore", true, true, false, false, false, false, 0);
        register(GameConfig.DEEPSLATE_COAL_ORE, "minecraft:deepslate_coal_ore", "Deepslate Coal Ore", true, true, false, false, false, false, 0);
        register(GameConfig.RED_BED, "minecraft:red_bed", "Red Bed", true, false, false, false, false, false, 0);
        register(GameConfig.OAK_FENCE_GATE, "minecraft:oak_fence_gate", "Oak Fence Gate", true, false, false, false, false, false, 0);
        register(GameConfig.PARADISE_PORTAL, "tiny:paradise_portal", "Paradise Portal", false, false, false, false, false, false, 12);
    }

    private BlockRegistry() {
    }

    private static void register(byte numericId, String namespacedId, String displayName,
                                 boolean solid, boolean opaque, boolean liquid, boolean replaceable,
                                 boolean plant, boolean gravityAffected, int lightEmission) {
        BlockType type = new BlockType(
            numericId & 0xFF,
            namespacedId,
            displayName,
            solid,
            opaque,
            liquid,
            replaceable,
            plant,
            gravityAffected,
            lightEmission
        );
        TYPES_BY_NUMERIC_ID.put(numericId & 0xFF, type);
        TYPES_BY_NAME.put(namespacedId, type);
        STATES_BY_NUMERIC_ID.put(numericId & 0xFF, new BlockState(type));
    }

    static BlockType air() {
        return TYPES_BY_NUMERIC_ID.get(GameConfig.AIR & 0xFF);
    }

    static BlockType typeByNumericId(byte legacyId) {
        BlockType type = TYPES_BY_NUMERIC_ID.get(legacyId & 0xFF);
        return type == null ? air() : type;
    }

    static BlockState stateByNumericId(byte legacyId) {
        BlockState state = STATES_BY_NUMERIC_ID.get(legacyId & 0xFF);
        return state == null ? STATES_BY_NUMERIC_ID.get(GameConfig.AIR & 0xFF) : state;
    }

    static BlockState stateByName(String namespacedId) {
        if (namespacedId != null) {
            int stateSeparator = namespacedId.indexOf('#');
            if (stateSeparator > 0) {
                String baseName = namespacedId.substring(0, stateSeparator);
                int data = 0;
                try {
                    data = Integer.parseInt(namespacedId.substring(stateSeparator + 1));
                } catch (NumberFormatException ignored) {
                    data = 0;
                }
                BlockType type = TYPES_BY_NAME.get(baseName);
                return type == null ? STATES_BY_NUMERIC_ID.get(GameConfig.AIR & 0xFF) : new BlockState(type, data);
            }
        }
        BlockType type = TYPES_BY_NAME.get(namespacedId);
        if (type == null) {
            return STATES_BY_NUMERIC_ID.get(GameConfig.AIR & 0xFF);
        }
        BlockState state = STATES_BY_NUMERIC_ID.get(type.numericId);
        return state == null ? STATES_BY_NUMERIC_ID.get(GameConfig.AIR & 0xFF) : state;
    }

    static BlockType typeByName(String namespacedId) {
        return TYPES_BY_NAME.get(namespacedId);
    }

    static boolean hasLegacyId(byte legacyId) {
        return TYPES_BY_NUMERIC_ID.containsKey(legacyId & 0xFF);
    }

    static Collection<BlockType> registeredTypes() {
        return Collections.unmodifiableCollection(TYPES_BY_NUMERIC_ID.values());
    }
}

final class LegacyBlockIds {
    private LegacyBlockIds() {
    }

    static BlockState stateFromLegacyId(byte id) {
        return BlockRegistry.stateByNumericId(id);
    }

    static byte legacyIdFromState(BlockState state) {
        if (state == null || state.type == null) {
            return GameConfig.AIR;
        }
        int numericId = state.type.numericId;
        return numericId >= 0 && numericId <= 255 ? (byte) numericId : GameConfig.AIR;
    }

    static BlockType typeFromLegacyId(byte id) {
        return BlockRegistry.typeByNumericId(id);
    }

    static boolean isKnownLegacyId(byte id) {
        return BlockRegistry.hasLegacyId(id);
    }
}

final class Blocks {
    private Blocks() {
    }

    static BlockState stateFromLegacyId(byte legacyId) {
        return LegacyBlockIds.stateFromLegacyId(legacyId);
    }

    static byte legacyIdFromState(BlockState state) {
        return LegacyBlockIds.legacyIdFromState(state);
    }

    static BlockState stateFromNamespacedId(String namespacedId) {
        return BlockRegistry.stateByName(namespacedId);
    }

    static String serializedId(BlockState state) {
        if (state == null || state.type == null) {
            return "minecraft:air";
        }
        return state.data == 0 ? state.type.namespacedId : state.type.namespacedId + "#" + state.data;
    }

    static BlockState withData(byte legacyId, int data) {
        return new BlockState(typeFromLegacyId(legacyId), data);
    }

    static BlockState doorState(boolean open, boolean upper, int facing) {
        int data = (open ? 1 : 0) | (upper ? 2 : 0) | ((facing & 3) << 2);
        return withData(GameConfig.OAK_DOOR, data);
    }

    static BlockState bedState(boolean head, int facing) {
        int data = (head ? 1 : 0) | ((facing & 3) << 1);
        return withData(GameConfig.RED_BED, data);
    }

    static boolean isDoorOpen(BlockState state) {
        return state != null && state.type.numericId == (GameConfig.OAK_DOOR & 0xFF) && (state.data & 1) != 0;
    }

    static boolean isDoorUpper(BlockState state) {
        return state != null && state.type.numericId == (GameConfig.OAK_DOOR & 0xFF) && (state.data & 2) != 0;
    }

    static int doorFacing(BlockState state) {
        return state == null ? 0 : ((state.data >>> 2) & 3);
    }

    static BlockState gateState(boolean open, int facing) {
        int data = (open ? 1 : 0) | ((facing & 3) << 1);
        return withData(GameConfig.OAK_FENCE_GATE, data);
    }

    static BlockState stairState(byte legacyId, int facing) {
        return withData(legacyId, facing & 3);
    }

    static boolean isGateOpen(BlockState state) {
        return state != null && state.type.numericId == (GameConfig.OAK_FENCE_GATE & 0xFF) && (state.data & 1) != 0;
    }

    static int gateFacing(BlockState state) {
        return state == null ? 0 : ((state.data >>> 1) & 3);
    }

    static int stairFacing(BlockState state) {
        return state == null ? 0 : (state.data & 3);
    }

    static boolean isBedHead(BlockState state) {
        return state != null && state.type.numericId == (GameConfig.RED_BED & 0xFF) && (state.data & 1) != 0;
    }

    static int bedFacing(BlockState state) {
        return state == null ? 1 : ((state.data >>> 1) & 3);
    }

    static BlockType typeFromLegacyId(byte legacyId) {
        return LegacyBlockIds.typeFromLegacyId(legacyId);
    }

    static boolean isKnownLegacyId(byte legacyId) {
        return LegacyBlockIds.isKnownLegacyId(legacyId);
    }

    static boolean isSolid(byte legacyId) {
        return typeFromLegacyId(legacyId).isSolid();
    }

    static boolean isOpaque(byte legacyId) {
        return typeFromLegacyId(legacyId).isOpaque();
    }

    static boolean isLiquid(byte legacyId) {
        return typeFromLegacyId(legacyId).isLiquid();
    }

    static boolean isPlant(byte legacyId) {
        return typeFromLegacyId(legacyId).isPlant();
    }

    static boolean isReplaceable(byte legacyId) {
        return typeFromLegacyId(legacyId).isReplaceable();
    }

    static boolean isGravityAffected(byte legacyId) {
        return typeFromLegacyId(legacyId).hasGravity();
    }
}
