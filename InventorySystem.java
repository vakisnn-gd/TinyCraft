import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

final class InventoryItems {
    static final byte OAK_PLANKS = 26;
    static final byte STICK = 27;
    static final byte IRON_HELMET = 28;
    static final byte IRON_CHESTPLATE = 29;
    static final byte IRON_LEGGINGS = 30;
    static final byte IRON_BOOTS = 31;
    static final byte SHIELD = 32;
    static final byte TOTEM = 33;
    static final byte IRON_INGOT = 34;
    static final byte DIAMOND_PICKAXE = 35;
    static final byte NETHERITE_PICKAXE = 36;
    static final byte DIAMOND_SWORD = 63;
    static final byte DIAMOND_AXE = 64;
    static final byte DIAMOND_SHOVEL = 65;
    static final byte DIAMOND_HOE = 66;
    static final byte NETHERITE_SWORD = 67;
    static final byte NETHERITE_AXE = 68;
    static final byte NETHERITE_SHOVEL = 69;
    static final byte NETHERITE_HOE = 70;
    static final byte WOODEN_PICKAXE = 72;
    static final byte WOODEN_SWORD = 73;
    static final byte WOODEN_AXE = 74;
    static final byte WOODEN_SHOVEL = 75;
    static final byte WOODEN_HOE = 76;
    static final byte STONE_PICKAXE = 77;
    static final byte STONE_SWORD = 78;
    static final byte STONE_AXE = 79;
    static final byte STONE_SHOVEL = 80;
    static final byte STONE_HOE = 81;
    static final byte IRON_PICKAXE = 82;
    static final byte IRON_SWORD = 83;
    static final byte IRON_AXE = 84;
    static final byte IRON_SHOVEL = 85;
    static final byte IRON_HOE = 86;
    static final byte ZOMBIE_SPAWN_EGG = 87;
    static final byte SKELETON_SPAWN_EGG = 88;
    static final byte PIG_SPAWN_EGG = 89;
    static final byte SHEEP_SPAWN_EGG = 90;
    static final byte COW_SPAWN_EGG = 91;
    static final byte VILLAGER_SPAWN_EGG = 92;
    static final byte RAW_PORK = 93;
    static final byte RAW_BEEF = 94;
    static final byte RAW_MUTTON = 95;
    static final byte LEATHER = 96;
    static final byte WOOL = 97;
    static final byte ROTTEN_FLESH = 98;
    static final byte BONE = 99;
    static final byte COOKED_PORK = 100;
    static final byte COOKED_BEEF = 101;
    static final byte COOKED_MUTTON = 102;
    static final byte BAKED_POTATO = 103;
    static final byte RAW_HERRING = (byte) 132;
    static final byte RAW_SALMON = (byte) 133;
    static final byte COOKED_HERRING = (byte) 134;
    static final byte COOKED_SALMON = (byte) 135;
    static final byte HERRING_SPAWN_EGG = (byte) 136;
    static final byte SALMON_SPAWN_EGG = (byte) 137;
    static final byte DIAMOND_HELMET = 104;
    static final byte DIAMOND_CHESTPLATE = 105;
    static final byte DIAMOND_LEGGINGS = 106;
    static final byte DIAMOND_BOOTS = 107;
    static final byte NETHERITE_HELMET = 108;
    static final byte NETHERITE_CHESTPLATE = 109;
    static final byte NETHERITE_LEGGINGS = 110;
    static final byte NETHERITE_BOOTS = 111;
    static final byte COAL_ITEM = 112;
    static final byte DIAMOND_ITEM = 113;
    static final byte ITEM_BUCKET = 114;
    static final byte ITEM_WATER_BUCKET = 115;
    static final byte ITEM_LAVA_BUCKET = 116;
    static final byte BREAD = 49;
    static final byte POTATO = 50;
    static final byte CARROT = 51;
    static final byte WHEAT_SEEDS = 52;

    static final byte[] CREATIVE_ITEMS = {
        GameConfig.GRASS,
        GameConfig.DIRT,
        GameConfig.COBBLESTONE,
        GameConfig.BEDROCK,
        GameConfig.IRON_ORE,
        GameConfig.DIAMOND_ORE,
        GameConfig.COAL_ORE,
        GameConfig.DEEPSLATE_IRON_ORE,
        GameConfig.DEEPSLATE_DIAMOND_ORE,
        GameConfig.DEEPSLATE_COAL_ORE,
        GameConfig.ZOMBIE_SKIN,
        GameConfig.ZOMBIE_SHIRT,
        GameConfig.ZOMBIE_PANTS,
        GameConfig.ZOMBIE_EYE,
        GameConfig.SAND,
        GameConfig.GRAVEL,
        GameConfig.CLAY,
        GameConfig.OAK_LOG,
        GameConfig.OAK_LEAVES,
        GameConfig.PINE_LOG,
        GameConfig.PINE_LEAVES,
        GameConfig.BIRCH_LOG,
        GameConfig.BIRCH_LEAVES,
        GameConfig.CACTUS,
        GameConfig.DEAD_BUSH,
        GameConfig.SNOW_BLOCK,
        GameConfig.SNOW_LAYER,
        GameConfig.SEAGRASS,
        GameConfig.KELP,
        GameConfig.TALL_GRASS,
        GameConfig.RED_FLOWER,
        GameConfig.YELLOW_FLOWER,
        GameConfig.STONE,
        GameConfig.DEEPSLATE,
        GameConfig.OBSIDIAN,
        OAK_PLANKS,
        GameConfig.PINE_PLANKS,
        GameConfig.BIRCH_PLANKS,
        GameConfig.OAK_STAIRS,
        GameConfig.PINE_STAIRS,
        GameConfig.BIRCH_STAIRS,
        GameConfig.STONE_STAIRS,
        GameConfig.COBBLESTONE_STAIRS,
        GameConfig.OAK_FENCE,
        GameConfig.OAK_FENCE_GATE,
        GameConfig.CHEST,
        GameConfig.CRAFTING_TABLE,
        GameConfig.FURNACE,
        GameConfig.GLASS,
        GameConfig.PARADISE_PORTAL,
        GameConfig.RED_BED,
        GameConfig.WHEAT_CROP,
        GameConfig.RAIL,
        GameConfig.OAK_DOOR,
        GameConfig.FARMLAND,
        GameConfig.TORCH,
        GameConfig.STRUCTURE_BLOCK,
        STICK,
        BREAD,
        POTATO,
        CARROT,
        WHEAT_SEEDS,
        IRON_INGOT,
        COAL_ITEM,
        DIAMOND_ITEM,
        ITEM_BUCKET,
        ITEM_WATER_BUCKET,
        ITEM_LAVA_BUCKET,
        WOODEN_PICKAXE,
        WOODEN_SWORD,
        WOODEN_AXE,
        WOODEN_SHOVEL,
        WOODEN_HOE,
        STONE_PICKAXE,
        STONE_SWORD,
        STONE_AXE,
        STONE_SHOVEL,
        STONE_HOE,
        IRON_PICKAXE,
        IRON_SWORD,
        IRON_AXE,
        IRON_SHOVEL,
        IRON_HOE,
        DIAMOND_PICKAXE,
        NETHERITE_PICKAXE,
        DIAMOND_SWORD,
        DIAMOND_AXE,
        DIAMOND_SHOVEL,
        DIAMOND_HOE,
        NETHERITE_SWORD,
        NETHERITE_AXE,
        NETHERITE_SHOVEL,
        NETHERITE_HOE,
        IRON_HELMET,
        IRON_CHESTPLATE,
        IRON_LEGGINGS,
        IRON_BOOTS,
        DIAMOND_HELMET,
        DIAMOND_CHESTPLATE,
        DIAMOND_LEGGINGS,
        DIAMOND_BOOTS,
        NETHERITE_HELMET,
        NETHERITE_CHESTPLATE,
        NETHERITE_LEGGINGS,
        NETHERITE_BOOTS,
        SHIELD,
        TOTEM,
        ZOMBIE_SPAWN_EGG,
        SKELETON_SPAWN_EGG,
        PIG_SPAWN_EGG,
        SHEEP_SPAWN_EGG,
        COW_SPAWN_EGG,
        VILLAGER_SPAWN_EGG,
        HERRING_SPAWN_EGG,
        SALMON_SPAWN_EGG,
        RAW_PORK,
        RAW_BEEF,
        RAW_MUTTON,
        RAW_HERRING,
        RAW_SALMON,
        COOKED_PORK,
        COOKED_BEEF,
        COOKED_MUTTON,
        COOKED_HERRING,
        COOKED_SALMON,
        BAKED_POTATO,
        LEATHER,
        WOOL,
        ROTTEN_FLESH,
        BONE
    };

    static final int[][] CREATIVE_TAB_INDICES = buildCreativeTabIndices();

    private InventoryItems() {
    }

    private static int[][] buildCreativeTabIndices() {
        return new int[][]{
            indicesFor(
                GameConfig.GRASS,
                GameConfig.DIRT,
                GameConfig.COBBLESTONE,
                GameConfig.BEDROCK,
                GameConfig.STONE,
                GameConfig.DEEPSLATE,
                GameConfig.OBSIDIAN,
                GameConfig.SAND,
                GameConfig.GRAVEL,
                GameConfig.CLAY,
                GameConfig.OAK_LOG,
                GameConfig.PINE_LOG,
                GameConfig.BIRCH_LOG,
                InventoryItems.OAK_PLANKS,
                GameConfig.PINE_PLANKS,
                GameConfig.BIRCH_PLANKS,
                GameConfig.OAK_STAIRS,
                GameConfig.PINE_STAIRS,
                GameConfig.BIRCH_STAIRS,
                GameConfig.STONE_STAIRS,
                GameConfig.COBBLESTONE_STAIRS,
                GameConfig.OAK_FENCE,
                GameConfig.OAK_FENCE_GATE,
                GameConfig.CHEST,
                GameConfig.CRAFTING_TABLE,
                GameConfig.FURNACE,
                GameConfig.GLASS,
                GameConfig.PARADISE_PORTAL,
                GameConfig.RED_BED,
                GameConfig.RAIL,
                GameConfig.OAK_DOOR,
                GameConfig.FARMLAND,
                GameConfig.TORCH,
                GameConfig.STRUCTURE_BLOCK,
                GameConfig.IRON_ORE,
                GameConfig.COAL_ORE,
                GameConfig.DIAMOND_ORE,
                GameConfig.DEEPSLATE_IRON_ORE,
                GameConfig.DEEPSLATE_COAL_ORE,
                GameConfig.DEEPSLATE_DIAMOND_ORE
            ),
            indicesFor(
                GameConfig.OAK_LEAVES,
                GameConfig.PINE_LOG,
                GameConfig.PINE_LEAVES,
                GameConfig.BIRCH_LOG,
                GameConfig.BIRCH_LEAVES,
                GameConfig.CACTUS,
                GameConfig.DEAD_BUSH,
                GameConfig.SNOW_BLOCK,
                GameConfig.SNOW_LAYER,
                GameConfig.SEAGRASS,
                GameConfig.KELP,
                GameConfig.TALL_GRASS,
                GameConfig.RED_FLOWER,
                GameConfig.YELLOW_FLOWER,
                GameConfig.WHEAT_CROP,
                GameConfig.FARMLAND,
                GameConfig.TORCH,
                InventoryItems.BREAD,
                InventoryItems.POTATO,
                InventoryItems.CARROT,
                InventoryItems.WHEAT_SEEDS,
                InventoryItems.RAW_PORK,
                InventoryItems.RAW_BEEF,
                InventoryItems.RAW_MUTTON,
                InventoryItems.RAW_HERRING,
                InventoryItems.RAW_SALMON,
                InventoryItems.COOKED_PORK,
                InventoryItems.COOKED_BEEF,
                InventoryItems.COOKED_MUTTON,
                InventoryItems.COOKED_HERRING,
                InventoryItems.COOKED_SALMON,
                InventoryItems.BAKED_POTATO,
                InventoryItems.LEATHER,
                InventoryItems.WOOL,
                InventoryItems.ROTTEN_FLESH,
                InventoryItems.BONE,
                InventoryItems.ZOMBIE_SPAWN_EGG,
                InventoryItems.SKELETON_SPAWN_EGG,
                InventoryItems.PIG_SPAWN_EGG,
                InventoryItems.SHEEP_SPAWN_EGG,
                InventoryItems.COW_SPAWN_EGG,
                InventoryItems.VILLAGER_SPAWN_EGG,
                InventoryItems.HERRING_SPAWN_EGG,
                InventoryItems.SALMON_SPAWN_EGG
            ),
            indicesFor(
                InventoryItems.WOODEN_PICKAXE,
                InventoryItems.WOODEN_SWORD,
                InventoryItems.WOODEN_AXE,
                InventoryItems.WOODEN_SHOVEL,
                InventoryItems.WOODEN_HOE,
                InventoryItems.STONE_PICKAXE,
                InventoryItems.STONE_SWORD,
                InventoryItems.STONE_AXE,
                InventoryItems.STONE_SHOVEL,
                InventoryItems.STONE_HOE,
                InventoryItems.IRON_PICKAXE,
                InventoryItems.IRON_SWORD,
                InventoryItems.IRON_AXE,
                InventoryItems.IRON_SHOVEL,
                InventoryItems.IRON_HOE,
                InventoryItems.DIAMOND_PICKAXE,
                InventoryItems.NETHERITE_PICKAXE,
                InventoryItems.DIAMOND_SWORD,
                InventoryItems.DIAMOND_AXE,
                InventoryItems.DIAMOND_SHOVEL,
                InventoryItems.DIAMOND_HOE,
                InventoryItems.NETHERITE_SWORD,
                InventoryItems.NETHERITE_AXE,
                InventoryItems.NETHERITE_SHOVEL,
                InventoryItems.NETHERITE_HOE,
                InventoryItems.IRON_HELMET,
                InventoryItems.IRON_CHESTPLATE,
                InventoryItems.IRON_LEGGINGS,
                InventoryItems.IRON_BOOTS,
                InventoryItems.DIAMOND_HELMET,
                InventoryItems.DIAMOND_CHESTPLATE,
                InventoryItems.DIAMOND_LEGGINGS,
                InventoryItems.DIAMOND_BOOTS,
                InventoryItems.NETHERITE_HELMET,
                InventoryItems.NETHERITE_CHESTPLATE,
                InventoryItems.NETHERITE_LEGGINGS,
                InventoryItems.NETHERITE_BOOTS,
                InventoryItems.TOTEM,
                InventoryItems.SHIELD,
                InventoryItems.STICK,
                InventoryItems.IRON_INGOT,
                InventoryItems.COAL_ITEM,
                InventoryItems.DIAMOND_ITEM
            ),
            indicesFor(
                InventoryItems.ITEM_BUCKET,
                InventoryItems.ITEM_WATER_BUCKET,
                InventoryItems.ITEM_LAVA_BUCKET,
                GameConfig.ZOMBIE_SKIN,
                GameConfig.ZOMBIE_SHIRT,
                GameConfig.ZOMBIE_PANTS,
                GameConfig.ZOMBIE_EYE
            )
        };
    }

    private static int[] indicesFor(byte... itemIds) {
        int[] indices = new int[itemIds.length];
        int count = 0;
        for (byte itemId : itemIds) {
            int index = creativeIndexOf(itemId);
            if (index >= 0) {
                indices[count++] = index;
            }
        }
        return Arrays.copyOf(indices, count);
    }

    private static int creativeIndexOf(byte itemId) {
        for (int i = 0; i < CREATIVE_ITEMS.length; i++) {
            if (CREATIVE_ITEMS[i] == itemId) {
                return i;
            }
        }
        return -1;
    }

    static String name(byte itemId) {
        if (Settings.isRussian()) {
            String translated = russianName(itemId);
            if (translated != null) {
                return translated;
            }
        }
        if (Blocks.isKnownLegacyId(itemId)) {
            return Blocks.typeFromLegacyId(itemId).displayName;
        }
        switch (itemId) {
            case GameConfig.GRASS:
                return "Grass Block";
            case GameConfig.DIRT:
                return "Dirt";
            case GameConfig.COBBLESTONE:
                return "Cobblestone";
            case GameConfig.BEDROCK:
                return "Bedrock";
            case GameConfig.IRON_ORE:
                return "Iron Ore";
            case GameConfig.DIAMOND_ORE:
                return "Diamond Ore";
            case GameConfig.COAL_ORE:
                return "Coal Ore";
            case GameConfig.ZOMBIE_SKIN:
                return "Zombie Skin";
            case GameConfig.ZOMBIE_SHIRT:
                return "Zombie Shirt";
            case GameConfig.ZOMBIE_PANTS:
                return "Zombie Pants";
            case GameConfig.ZOMBIE_EYE:
                return "Zombie Eye";
            case GameConfig.WATER:
                return "Water";
            case GameConfig.LAVA:
                return "Lava";
            case GameConfig.SAND:
                return "Sand";
            case GameConfig.GRAVEL:
                return "Gravel";
            case GameConfig.CLAY:
                return "Clay";
            case GameConfig.OAK_LOG:
                return "Oak Log";
            case GameConfig.OAK_LEAVES:
                return "Oak Leaves";
            case GameConfig.PINE_LOG:
                return "Pine Log";
            case GameConfig.PINE_LEAVES:
                return "Pine Leaves";
            case GameConfig.BIRCH_LOG:
                return "Birch Log";
            case GameConfig.BIRCH_LEAVES:
                return "Birch Leaves";
            case GameConfig.CACTUS:
                return "Cactus";
            case GameConfig.DEAD_BUSH:
                return "Dead Bush";
            case GameConfig.SNOW_BLOCK:
                return "Snow Block";
            case GameConfig.SNOW_LAYER:
                return "Snow Layer";
            case GameConfig.SEAGRASS:
                return "Seagrass";
            case GameConfig.KELP:
                return "Kelp";
            case GameConfig.TALL_GRASS:
                return "Tall Grass";
            case GameConfig.RED_FLOWER:
                return "Red Flower";
            case GameConfig.YELLOW_FLOWER:
                return "Yellow Flower";
            case GameConfig.STONE:
                return "Stone";
            case GameConfig.DEEPSLATE:
                return "Deepslate";
            case GameConfig.OBSIDIAN:
                return "Obsidian";
            case OAK_PLANKS:
                return "Oak Planks";
            case GameConfig.PINE_PLANKS:
                return "Spruce Planks";
            case GameConfig.BIRCH_PLANKS:
                return "Birch Planks";
            case GameConfig.OAK_STAIRS:
                return "Oak Stairs";
            case GameConfig.PINE_STAIRS:
                return "Spruce Stairs";
            case GameConfig.BIRCH_STAIRS:
                return "Birch Stairs";
            case GameConfig.STONE_STAIRS:
                return "Stone Stairs";
            case GameConfig.COBBLESTONE_STAIRS:
                return "Cobblestone Stairs";
            case GameConfig.OAK_FENCE:
                return "Oak Fence";
            case GameConfig.OAK_FENCE_GATE:
                return "Oak Fence Gate";
            case GameConfig.CHEST:
                return "Chest";
            case GameConfig.CRAFTING_TABLE:
                return "Crafting Table";
            case GameConfig.FURNACE:
                return "Furnace";
            case GameConfig.GLASS:
                return "Glass";
            case GameConfig.WHEAT_CROP:
                return "Wheat";
            case GameConfig.CARROT_CROP:
                return "Carrots";
            case GameConfig.POTATO_CROP:
                return "Potatoes";
            case GameConfig.RAIL:
                return "Rail";
            case GameConfig.OAK_DOOR:
                return "Oak Door";
            case GameConfig.FARMLAND:
                return "Farmland";
            case GameConfig.TORCH:
                return "Torch";
            case GameConfig.STRUCTURE_BLOCK:
                return "Structure Block";
            case GameConfig.RED_BED:
                return "Red Bed";
            case STICK:
                return "Stick";
            case BREAD:
                return "Bread";
            case POTATO:
                return "Potato";
            case CARROT:
                return "Carrot";
            case WHEAT_SEEDS:
                return "Seeds";
            case RAW_HERRING:
                return "Raw Herring";
            case RAW_SALMON:
                return "Raw Salmon";
            case COOKED_HERRING:
                return "Cooked Herring";
            case COOKED_SALMON:
                return "Cooked Salmon";
            case COAL_ITEM:
                return "Coal";
            case DIAMOND_ITEM:
                return "Diamond";
            case ITEM_BUCKET:
                return "Bucket";
            case ITEM_WATER_BUCKET:
                return "Water Bucket";
            case ITEM_LAVA_BUCKET:
                return "Lava Bucket";
            case IRON_HELMET:
                return "Iron Helmet";
            case IRON_CHESTPLATE:
                return "Iron Chestplate";
            case IRON_LEGGINGS:
                return "Iron Leggings";
            case IRON_BOOTS:
                return "Iron Boots";
            case DIAMOND_HELMET:
                return "Diamond Helmet";
            case DIAMOND_CHESTPLATE:
                return "Diamond Chestplate";
            case DIAMOND_LEGGINGS:
                return "Diamond Leggings";
            case DIAMOND_BOOTS:
                return "Diamond Boots";
            case NETHERITE_HELMET:
                return "Netherite Helmet";
            case NETHERITE_CHESTPLATE:
                return "Netherite Chestplate";
            case NETHERITE_LEGGINGS:
                return "Netherite Leggings";
            case NETHERITE_BOOTS:
                return "Netherite Boots";
            case SHIELD:
                return "Shield";
            case TOTEM:
                return "Totem";
            case IRON_INGOT:
                return "Iron Ingot";
            case WOODEN_PICKAXE:
                return "Wooden Pickaxe";
            case WOODEN_SWORD:
                return "Wooden Sword";
            case WOODEN_AXE:
                return "Wooden Axe";
            case WOODEN_SHOVEL:
                return "Wooden Shovel";
            case WOODEN_HOE:
                return "Wooden Hoe";
            case STONE_PICKAXE:
                return "Stone Pickaxe";
            case STONE_SWORD:
                return "Stone Sword";
            case STONE_AXE:
                return "Stone Axe";
            case STONE_SHOVEL:
                return "Stone Shovel";
            case STONE_HOE:
                return "Stone Hoe";
            case IRON_PICKAXE:
                return "Iron Pickaxe";
            case IRON_SWORD:
                return "Iron Sword";
            case IRON_AXE:
                return "Iron Axe";
            case IRON_SHOVEL:
                return "Iron Shovel";
            case IRON_HOE:
                return "Iron Hoe";
            case DIAMOND_PICKAXE:
                return "Diamond Pickaxe";
            case NETHERITE_PICKAXE:
                return "Netherite Pickaxe";
            case DIAMOND_SWORD:
                return "Diamond Sword";
            case DIAMOND_AXE:
                return "Diamond Axe";
            case DIAMOND_SHOVEL:
                return "Diamond Shovel";
            case DIAMOND_HOE:
                return "Diamond Hoe";
            case NETHERITE_SWORD:
                return "Netherite Sword";
            case NETHERITE_AXE:
                return "Netherite Axe";
            case NETHERITE_SHOVEL:
                return "Netherite Shovel";
            case NETHERITE_HOE:
                return "Netherite Hoe";
            case ZOMBIE_SPAWN_EGG:
                return "Zombie Spawn Egg";
            case SKELETON_SPAWN_EGG:
                return "Skeleton Spawn Egg";
            case PIG_SPAWN_EGG:
                return "Pig Spawn Egg";
            case SHEEP_SPAWN_EGG:
                return "Sheep Spawn Egg";
            case COW_SPAWN_EGG:
                return "Cow Spawn Egg";
            case VILLAGER_SPAWN_EGG:
                return "Villager Spawn Egg";
            case RAW_PORK:
                return "Raw Pork";
            case RAW_BEEF:
                return "Raw Beef";
            case RAW_MUTTON:
                return "Raw Mutton";
            case COOKED_PORK:
                return "Cooked Pork";
            case COOKED_BEEF:
                return "Steak";
            case COOKED_MUTTON:
                return "Cooked Mutton";
            case BAKED_POTATO:
                return "Baked Potato";
            case LEATHER:
                return "Leather";
            case WOOL:
                return "Wool";
            case ROTTEN_FLESH:
                return "Rotten Flesh";
            case BONE:
                return "Bone";
            default:
                return "Item " + itemId;
        }
    }

    private static String russianName(byte itemId) {
        if (itemId == GameConfig.KELP) {
            return "\u041b\u0430\u043c\u0438\u043d\u0430\u0440\u0438\u044f";
        }
        if (itemId == GameConfig.CARROT_CROP) {
            return "\u041c\u043e\u0440\u043a\u043e\u0432\u044c";
        }
        if (itemId == GameConfig.POTATO_CROP) {
            return "\u041a\u0430\u0440\u0442\u043e\u0444\u0435\u043b\u044c";
        }
        if (itemId == RAW_HERRING) {
            return "\u0421\u044b\u0440\u0430\u044f \u0441\u0435\u043b\u0435\u0434\u043a\u0430";
        }
        if (itemId == RAW_SALMON) {
            return "\u0421\u044b\u0440\u043e\u0439 \u043b\u043e\u0441\u043e\u0441\u044c";
        }
        if (itemId == COOKED_HERRING) {
            return "\u0416\u0430\u0440\u0435\u043d\u0430\u044f \u0441\u0435\u043b\u0435\u0434\u043a\u0430";
        }
        if (itemId == COOKED_SALMON) {
            return "\u0416\u0430\u0440\u0435\u043d\u044b\u0439 \u043b\u043e\u0441\u043e\u0441\u044c";
        }
        if (itemId == HERRING_SPAWN_EGG) {
            return "\u042f\u0439\u0446\u043e \u043f\u0440\u0438\u0437\u044b\u0432\u0430 \u0441\u0435\u043b\u0435\u0434\u043a\u0438";
        }
        if (itemId == SALMON_SPAWN_EGG) {
            return "\u042f\u0439\u0446\u043e \u043f\u0440\u0438\u0437\u044b\u0432\u0430 \u043b\u043e\u0441\u043e\u0441\u044f";
        }
        switch (itemId) {
            case GameConfig.GRASS: return "Блок травы";
            case GameConfig.DIRT: return "Земля";
            case GameConfig.COBBLESTONE: return "Булыжник";
            case GameConfig.BEDROCK: return "Бедрок";
            case GameConfig.IRON_ORE: return "Железная руда";
            case GameConfig.DIAMOND_ORE: return "Алмазная руда";
            case GameConfig.COAL_ORE: return "Угольная руда";
            case GameConfig.ZOMBIE_SKIN: return "Кожа зомби";
            case GameConfig.ZOMBIE_SHIRT: return "Рубашка зомби";
            case GameConfig.ZOMBIE_PANTS: return "Штаны зомби";
            case GameConfig.ZOMBIE_EYE: return "Глаз зомби";
            case GameConfig.WATER: return "Вода";
            case GameConfig.LAVA: return "Лава";
            case GameConfig.SAND: return "Песок";
            case GameConfig.GRAVEL: return "Гравий";
            case GameConfig.CLAY: return "Глина";
            case GameConfig.OAK_LOG: return "Дубовое бревно";
            case GameConfig.OAK_LEAVES: return "Дубовая листва";
            case GameConfig.PINE_LOG: return "Сосновое бревно";
            case GameConfig.PINE_LEAVES: return "Сосновая листва";
            case GameConfig.BIRCH_LOG: return "Березовое бревно";
            case GameConfig.BIRCH_LEAVES: return "Березовая листва";
            case GameConfig.CACTUS: return "Кактус";
            case GameConfig.DEAD_BUSH: return "Мертвый куст";
            case GameConfig.SNOW_BLOCK: return "Блок снега";
            case GameConfig.SNOW_LAYER: return "Слой снега";
            case GameConfig.SEAGRASS: return "Водоросли";
            case GameConfig.TALL_GRASS: return "Высокая трава";
            case GameConfig.RED_FLOWER: return "Красный цветок";
            case GameConfig.YELLOW_FLOWER: return "Желтый цветок";
            case GameConfig.STONE: return "Камень";
            case GameConfig.DEEPSLATE: return "Глубинный сланец";
            case GameConfig.DEEPSLATE_IRON_ORE: return "Железная руда в глубинном сланце";
            case GameConfig.DEEPSLATE_DIAMOND_ORE: return "Алмазная руда в глубинном сланце";
            case GameConfig.DEEPSLATE_COAL_ORE: return "Угольная руда в глубинном сланце";
            case GameConfig.OBSIDIAN: return "Обсидиан";
            case OAK_PLANKS: return "Дубовые доски";
            case GameConfig.PINE_PLANKS: return "Сосновые доски";
            case GameConfig.BIRCH_PLANKS: return "Березовые доски";
            case GameConfig.OAK_STAIRS: return "Дубовые ступени";
            case GameConfig.PINE_STAIRS: return "Сосновые ступени";
            case GameConfig.BIRCH_STAIRS: return "Березовые ступени";
            case GameConfig.STONE_STAIRS: return "Каменные ступени";
            case GameConfig.COBBLESTONE_STAIRS: return "Булыжниковые ступени";
            case GameConfig.OAK_FENCE: return "Дубовый забор";
            case GameConfig.CHEST: return "Сундук";
            case GameConfig.CRAFTING_TABLE: return "Верстак";
            case GameConfig.FURNACE: return "Печь";
            case GameConfig.GLASS: return "Стекло";
            case GameConfig.WHEAT_CROP: return "Пшеница";
            case GameConfig.RAIL: return "Рельсы";
            case GameConfig.OAK_DOOR: return "Дубовая дверь";
            case GameConfig.FARMLAND: return "Грядка";
            case GameConfig.TORCH: return "Факел";
            case GameConfig.STRUCTURE_BLOCK: return "Структурный блок";
            case GameConfig.RED_BED: return "Красная кровать";
            case STICK: return "Палка";
            case BREAD: return "Хлеб";
            case POTATO: return "Картофель";
            case CARROT: return "Морковь";
            case WHEAT_SEEDS: return "Семена";
            case IRON_HELMET: return "Железный шлем";
            case IRON_CHESTPLATE: return "Железный нагрудник";
            case IRON_LEGGINGS: return "Железные поножи";
            case IRON_BOOTS: return "Железные ботинки";
            case SHIELD: return "Щит";
            case TOTEM: return "Тотем";
            case IRON_INGOT: return "Железный слиток";
            case WOODEN_PICKAXE: return "Деревянная кирка";
            case WOODEN_SWORD: return "Деревянный меч";
            case WOODEN_AXE: return "Деревянный топор";
            case WOODEN_SHOVEL: return "Деревянная лопата";
            case WOODEN_HOE: return "Деревянная мотыга";
            case STONE_PICKAXE: return "Каменная кирка";
            case STONE_SWORD: return "Каменный меч";
            case STONE_AXE: return "Каменный топор";
            case STONE_SHOVEL: return "Каменная лопата";
            case STONE_HOE: return "Каменная мотыга";
            case IRON_PICKAXE: return "Железная кирка";
            case IRON_SWORD: return "Железный меч";
            case IRON_AXE: return "Железный топор";
            case IRON_SHOVEL: return "Железная лопата";
            case IRON_HOE: return "Железная мотыга";
            case DIAMOND_PICKAXE: return "Алмазная кирка";
            case NETHERITE_PICKAXE: return "Незеритовая кирка";
            case DIAMOND_SWORD: return "Алмазный меч";
            case DIAMOND_AXE: return "Алмазный топор";
            case DIAMOND_SHOVEL: return "Алмазная лопата";
            case DIAMOND_HOE: return "Алмазная мотыга";
            case NETHERITE_SWORD: return "Незеритовый меч";
            case NETHERITE_AXE: return "Незеритовый топор";
            case NETHERITE_SHOVEL: return "Незеритовая лопата";
            case NETHERITE_HOE: return "Незеритовая мотыга";
            case ZOMBIE_SPAWN_EGG: return "Яйцо призыва зомби";
            case SKELETON_SPAWN_EGG: return "Яйцо призыва скелета";
            case PIG_SPAWN_EGG: return "Яйцо призыва свиньи";
            case SHEEP_SPAWN_EGG: return "Яйцо призыва овцы";
            case COW_SPAWN_EGG: return "Яйцо призыва коровы";
            case VILLAGER_SPAWN_EGG: return "Яйцо призыва жителя";
            case RAW_PORK: return "Сырая свинина";
            case RAW_BEEF: return "Сырая говядина";
            case RAW_MUTTON: return "Сырая баранина";
            case COOKED_PORK: return "Жареная свинина";
            case COOKED_BEEF: return "Стейк";
            case COOKED_MUTTON: return "Жареная баранина";
            case BAKED_POTATO: return "Печеный картофель";
            case LEATHER: return "Кожа";
            case WOOL: return "Шерсть";
            case ROTTEN_FLESH: return "Гнилая плоть";
            case BONE: return "Кость";
            case COAL_ITEM: return "\u0423\u0433\u043e\u043b\u044c";
            case DIAMOND_ITEM: return "\u0410\u043b\u043c\u0430\u0437";
            case DIAMOND_HELMET: return "\u0410\u043b\u043c\u0430\u0437\u043d\u044b\u0439 \u0448\u043b\u0435\u043c";
            case DIAMOND_CHESTPLATE: return "\u0410\u043b\u043c\u0430\u0437\u043d\u044b\u0439 \u043d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a";
            case DIAMOND_LEGGINGS: return "\u0410\u043b\u043c\u0430\u0437\u043d\u044b\u0435 \u043f\u043e\u043d\u043e\u0436\u0438";
            case DIAMOND_BOOTS: return "\u0410\u043b\u043c\u0430\u0437\u043d\u044b\u0435 \u0431\u043e\u0442\u0438\u043d\u043a\u0438";
            case NETHERITE_HELMET: return "\u041d\u0435\u0437\u0435\u0440\u0438\u0442\u043e\u0432\u044b\u0439 \u0448\u043b\u0435\u043c";
            case NETHERITE_CHESTPLATE: return "\u041d\u0435\u0437\u0435\u0440\u0438\u0442\u043e\u0432\u044b\u0439 \u043d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a";
            case NETHERITE_LEGGINGS: return "\u041d\u0435\u0437\u0435\u0440\u0438\u0442\u043e\u0432\u044b\u0435 \u043f\u043e\u043d\u043e\u0436\u0438";
            case NETHERITE_BOOTS: return "\u041d\u0435\u0437\u0435\u0440\u0438\u0442\u043e\u0432\u044b\u0435 \u0431\u043e\u0442\u0438\u043d\u043a\u0438";
            case ITEM_BUCKET: return "\u0412\u0435\u0434\u0440\u043e";
            case ITEM_WATER_BUCKET: return "\u0412\u0435\u0434\u0440\u043e \u0432\u043e\u0434\u044b";
            case ITEM_LAVA_BUCKET: return "\u0412\u0435\u0434\u0440\u043e \u043b\u0430\u0432\u044b";
            default: return null;
        }
    }

    static boolean isPlaceable(byte itemId) {
        return itemId == WHEAT_SEEDS
            || itemId == CARROT
            || itemId == POTATO
            || itemId == ITEM_WATER_BUCKET
            || itemId == ITEM_LAVA_BUCKET
            || (Blocks.isKnownLegacyId(itemId)
                && !Blocks.typeFromLegacyId(itemId).isAir()
                && !isCropBlock(itemId));
    }

    static boolean isCropBlock(byte itemId) {
        return itemId == GameConfig.WHEAT_CROP
            || itemId == GameConfig.CARROT_CROP
            || itemId == GameConfig.POTATO_CROP;
    }

    static boolean isCollectible(byte itemId) {
        return itemId != GameConfig.AIR
            && itemId != GameConfig.BEDROCK
            && itemId != GameConfig.WATER
            && itemId != GameConfig.LAVA
            && (!Blocks.isLiquid(itemId) || itemId == GameConfig.SEAGRASS || itemId == GameConfig.KELP);
    }

    static int maxStackSize(byte itemId) {
        switch (itemId) {
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case NETHERITE_HELMET:
            case NETHERITE_CHESTPLATE:
            case NETHERITE_LEGGINGS:
            case NETHERITE_BOOTS:
            case ITEM_BUCKET:
            case ITEM_WATER_BUCKET:
            case ITEM_LAVA_BUCKET:
            case SHIELD:
            case TOTEM:
            case WOODEN_PICKAXE:
            case WOODEN_SWORD:
            case WOODEN_AXE:
            case WOODEN_SHOVEL:
            case WOODEN_HOE:
            case STONE_PICKAXE:
            case STONE_SWORD:
            case STONE_AXE:
            case STONE_SHOVEL:
            case STONE_HOE:
            case IRON_PICKAXE:
            case IRON_SWORD:
            case IRON_AXE:
            case IRON_SHOVEL:
            case IRON_HOE:
            case DIAMOND_PICKAXE:
            case NETHERITE_PICKAXE:
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_HOE:
            case NETHERITE_SWORD:
            case NETHERITE_AXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_HOE:
                return 1;
            default:
                return 64;
        }
    }

    static boolean isArmor(byte itemId) {
        return armorSlotIndex(itemId) >= 0;
    }

    static boolean isDurableItem(byte itemId) {
        return maxDurability(itemId) > 0;
    }

    static int maxDurability(byte itemId) {
        switch (itemId) {
            case WOODEN_PICKAXE:
            case WOODEN_SWORD:
            case WOODEN_AXE:
            case WOODEN_SHOVEL:
            case WOODEN_HOE:
                return 59;
            case STONE_PICKAXE:
            case STONE_SWORD:
            case STONE_AXE:
            case STONE_SHOVEL:
            case STONE_HOE:
                return 131;
            case IRON_PICKAXE:
            case IRON_SWORD:
            case IRON_AXE:
            case IRON_SHOVEL:
            case IRON_HOE:
                return 250;
            case DIAMOND_PICKAXE:
            case DIAMOND_SWORD:
            case DIAMOND_AXE:
            case DIAMOND_SHOVEL:
            case DIAMOND_HOE:
                return 1561;
            case NETHERITE_PICKAXE:
            case NETHERITE_SWORD:
            case NETHERITE_AXE:
            case NETHERITE_SHOVEL:
            case NETHERITE_HOE:
                return 2031;
            case IRON_HELMET:
                return 165;
            case IRON_CHESTPLATE:
                return 240;
            case IRON_LEGGINGS:
                return 225;
            case IRON_BOOTS:
                return 195;
            case DIAMOND_HELMET:
                return 363;
            case DIAMOND_CHESTPLATE:
                return 528;
            case DIAMOND_LEGGINGS:
                return 495;
            case DIAMOND_BOOTS:
                return 429;
            case NETHERITE_HELMET:
                return 407;
            case NETHERITE_CHESTPLATE:
                return 592;
            case NETHERITE_LEGGINGS:
                return 555;
            case NETHERITE_BOOTS:
                return 481;
            case SHIELD:
                return 336;
            default:
                return 0;
        }
    }

    static int armorSlotIndex(byte itemId) {
        switch (itemId) {
            case IRON_HELMET:
            case DIAMOND_HELMET:
            case NETHERITE_HELMET:
                return 0;
            case IRON_CHESTPLATE:
            case DIAMOND_CHESTPLATE:
            case NETHERITE_CHESTPLATE:
                return 1;
            case IRON_LEGGINGS:
            case DIAMOND_LEGGINGS:
            case NETHERITE_LEGGINGS:
                return 2;
            case IRON_BOOTS:
            case DIAMOND_BOOTS:
            case NETHERITE_BOOTS:
                return 3;
            default:
                return -1;
        }
    }

    static boolean isOffhandPreferred(byte itemId) {
        return itemId == SHIELD || itemId == TOTEM;
    }

    static boolean isSpawnEgg(byte itemId) {
        return mobKindForSpawnEgg(itemId) != null;
    }

    static MobKind mobKindForSpawnEgg(byte itemId) {
        switch (itemId) {
            case ZOMBIE_SPAWN_EGG:
                return MobKind.ZOMBIE;
            case SKELETON_SPAWN_EGG:
                return MobKind.SKELETON;
            case PIG_SPAWN_EGG:
                return MobKind.PIG;
            case SHEEP_SPAWN_EGG:
                return MobKind.SHEEP;
            case COW_SPAWN_EGG:
                return MobKind.COW;
            case VILLAGER_SPAWN_EGG:
                return MobKind.VILLAGER;
            case HERRING_SPAWN_EGG:
                return MobKind.HERRING;
            case SALMON_SPAWN_EGG:
                return MobKind.SALMON;
            default:
                return null;
        }
    }

    static int foodValue(byte itemId) {
        switch (itemId) {
            case BREAD:
                return 5;
            case CARROT:
                return 3;
            case POTATO:
                return 2;
            case RAW_PORK:
            case RAW_BEEF:
            case RAW_MUTTON:
            case RAW_HERRING:
            case RAW_SALMON:
                return 3;
            case COOKED_PORK:
            case COOKED_BEEF:
                return 8;
            case COOKED_MUTTON:
            case COOKED_HERRING:
            case COOKED_SALMON:
                return 6;
            case BAKED_POTATO:
                return 5;
            case ROTTEN_FLESH:
                return 2;
            default:
                return 0;
        }
    }
}

final class ItemStack {
    byte itemId;
    int count;
    int durabilityDamage;

    ItemStack() {
        clear();
    }

    ItemStack(byte itemId, int count) {
        set(itemId, count);
    }

    boolean isEmpty() {
        return itemId == GameConfig.AIR || count <= 0;
    }

    void set(byte itemId, int count) {
        if (itemId == GameConfig.AIR || count <= 0) {
            clear();
            return;
        }
        this.itemId = itemId;
        this.count = count;
        this.durabilityDamage = 0;
    }

    void clear() {
        itemId = GameConfig.AIR;
        count = 0;
        durabilityDamage = 0;
    }

    void copyFrom(ItemStack other) {
        if (other == null || other.isEmpty()) {
            clear();
        } else {
            set(other.itemId, other.count);
            durabilityDamage = InventoryItems.isDurableItem(itemId)
                ? Math.max(0, Math.min(other.durabilityDamage, InventoryItems.maxDurability(itemId) - 1))
                : 0;
        }
    }

    ItemStack copy() {
        ItemStack stack = new ItemStack(itemId, count);
        stack.durabilityDamage = durabilityDamage;
        return stack;
    }

    int remainingDurability() {
        int max = InventoryItems.maxDurability(itemId);
        return max <= 0 ? 0 : Math.max(0, max - durabilityDamage);
    }

    void damage(int amount) {
        int max = InventoryItems.maxDurability(itemId);
        if (max <= 0 || amount <= 0 || isEmpty()) {
            return;
        }
        durabilityDamage += amount;
        if (durabilityDamage >= max) {
            clear();
        }
    }
}

enum InventorySlotGroup {
    STORAGE,
    HOTBAR,
    ARMOR,
    OFFHAND,
    CRAFT,
    CRAFT_RESULT,
    CRAFT_3X3,
    CRAFT_3X3_RESULT,
    CHEST_CONTAINER,
    FURNACE_INPUT,
    FURNACE_FUEL,
    FURNACE_OUTPUT,
    CREATIVE,
    TRASH
}

final class InventorySlotRef {
    final InventorySlotGroup group;
    final int index;

    InventorySlotRef(InventorySlotGroup group, int index) {
        this.group = group;
        this.index = index;
    }
}

final class CraftingRecipe {
    final int width;
    final int height;
    final byte[] pattern;
    final byte[] shapelessIngredients;
    final byte resultItem;
    final int resultCount;

    private CraftingRecipe(int width, int height, byte[] pattern, byte[] shapelessIngredients, byte resultItem, int resultCount) {
        this.width = width;
        this.height = height;
        this.pattern = pattern;
        this.shapelessIngredients = shapelessIngredients;
        this.resultItem = resultItem;
        this.resultCount = resultCount;
    }

    static CraftingRecipe shaped(int width, int height, byte resultItem, int resultCount, byte... pattern) {
        return new CraftingRecipe(width, height, pattern, null, resultItem, resultCount);
    }

    static CraftingRecipe shapeless(byte resultItem, int resultCount, byte... ingredients) {
        return new CraftingRecipe(0, 0, null, ingredients, resultItem, resultCount);
    }

    boolean shapeless() {
        return shapelessIngredients != null;
    }
}

final class CraftingRecipes {
    private static final byte ANY_PLANKS = (byte) 0xF1;

    private static final CraftingRecipe[] RECIPES = {
        CraftingRecipe.shapeless(InventoryItems.OAK_PLANKS, 4, GameConfig.OAK_LOG),
        CraftingRecipe.shapeless(GameConfig.PINE_PLANKS, 4, GameConfig.PINE_LOG),
        CraftingRecipe.shapeless(GameConfig.BIRCH_PLANKS, 4, GameConfig.BIRCH_LOG),
        CraftingRecipe.shaped(1, 2, InventoryItems.STICK, 4,
            InventoryItems.OAK_PLANKS,
            InventoryItems.OAK_PLANKS),
        CraftingRecipe.shaped(1, 2, InventoryItems.STICK, 4,
            GameConfig.PINE_PLANKS,
            GameConfig.PINE_PLANKS),
        CraftingRecipe.shaped(1, 2, InventoryItems.STICK, 4,
            GameConfig.BIRCH_PLANKS,
            GameConfig.BIRCH_PLANKS),
        CraftingRecipe.shaped(3, 2, GameConfig.OAK_STAIRS, 4,
            InventoryItems.OAK_PLANKS, GameConfig.AIR, GameConfig.AIR,
            InventoryItems.OAK_PLANKS, InventoryItems.OAK_PLANKS, InventoryItems.OAK_PLANKS),
        CraftingRecipe.shaped(3, 2, GameConfig.PINE_STAIRS, 4,
            GameConfig.PINE_PLANKS, GameConfig.AIR, GameConfig.AIR,
            GameConfig.PINE_PLANKS, GameConfig.PINE_PLANKS, GameConfig.PINE_PLANKS),
        CraftingRecipe.shaped(3, 2, GameConfig.BIRCH_STAIRS, 4,
            GameConfig.BIRCH_PLANKS, GameConfig.AIR, GameConfig.AIR,
            GameConfig.BIRCH_PLANKS, GameConfig.BIRCH_PLANKS, GameConfig.BIRCH_PLANKS),
        CraftingRecipe.shaped(3, 2, GameConfig.STONE_STAIRS, 4,
            GameConfig.STONE, GameConfig.AIR, GameConfig.AIR,
            GameConfig.STONE, GameConfig.STONE, GameConfig.STONE),
        CraftingRecipe.shaped(3, 2, GameConfig.COBBLESTONE_STAIRS, 4,
            GameConfig.COBBLESTONE, GameConfig.AIR, GameConfig.AIR,
            GameConfig.COBBLESTONE, GameConfig.COBBLESTONE, GameConfig.COBBLESTONE),
        CraftingRecipe.shaped(2, 2, GameConfig.CRAFTING_TABLE, 1,
            ANY_PLANKS, ANY_PLANKS,
            ANY_PLANKS, ANY_PLANKS),
        CraftingRecipe.shaped(3, 3, GameConfig.CHEST, 1,
            ANY_PLANKS, ANY_PLANKS, ANY_PLANKS,
            ANY_PLANKS, GameConfig.AIR, ANY_PLANKS,
            ANY_PLANKS, ANY_PLANKS, ANY_PLANKS),
        CraftingRecipe.shaped(3, 3, GameConfig.FURNACE, 1,
            GameConfig.COBBLESTONE, GameConfig.COBBLESTONE, GameConfig.COBBLESTONE,
            GameConfig.COBBLESTONE, GameConfig.AIR, GameConfig.COBBLESTONE,
            GameConfig.COBBLESTONE, GameConfig.COBBLESTONE, GameConfig.COBBLESTONE),
        CraftingRecipe.shaped(1, 2, GameConfig.TORCH, 4,
            InventoryItems.COAL_ITEM,
            InventoryItems.STICK),
        CraftingRecipe.shapeless(InventoryItems.COAL_ITEM, 1, GameConfig.COAL_ORE),
        CraftingRecipe.shapeless(InventoryItems.COAL_ITEM, 1, GameConfig.DEEPSLATE_COAL_ORE),
        CraftingRecipe.shapeless(InventoryItems.DIAMOND_ITEM, 1, GameConfig.DIAMOND_ORE),
        CraftingRecipe.shapeless(InventoryItems.DIAMOND_ITEM, 1, GameConfig.DEEPSLATE_DIAMOND_ORE),
        CraftingRecipe.shaped(2, 3, GameConfig.OAK_DOOR, 3,
            ANY_PLANKS, ANY_PLANKS,
            ANY_PLANKS, ANY_PLANKS,
            ANY_PLANKS, ANY_PLANKS),
        CraftingRecipe.shaped(3, 3, GameConfig.RAIL, 16,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, InventoryItems.STICK, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT),
        CraftingRecipe.shaped(3, 1, InventoryItems.BREAD, 1,
            GameConfig.WHEAT_CROP, GameConfig.WHEAT_CROP, GameConfig.WHEAT_CROP),
        CraftingRecipe.shaped(3, 2, GameConfig.OAK_FENCE, 6,
            ANY_PLANKS, InventoryItems.STICK, ANY_PLANKS,
            ANY_PLANKS, InventoryItems.STICK, ANY_PLANKS),
        CraftingRecipe.shaped(3, 2, GameConfig.OAK_FENCE_GATE, 1,
            InventoryItems.STICK, ANY_PLANKS, InventoryItems.STICK,
            InventoryItems.STICK, ANY_PLANKS, InventoryItems.STICK),
        CraftingRecipe.shapeless(GameConfig.GRAVEL, 1, GameConfig.COBBLESTONE, GameConfig.DIRT),
        CraftingRecipe.shapeless(GameConfig.CLAY, 1, GameConfig.SAND, GameConfig.DIRT),
        CraftingRecipe.shaped(3, 3, InventoryItems.WOODEN_PICKAXE, 1,
            ANY_PLANKS, ANY_PLANKS, ANY_PLANKS,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR),
        CraftingRecipe.shaped(1, 3, InventoryItems.WOODEN_SWORD, 1,
            ANY_PLANKS,
            ANY_PLANKS,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 3, InventoryItems.WOODEN_AXE, 1,
            ANY_PLANKS, ANY_PLANKS,
            ANY_PLANKS, InventoryItems.STICK,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(1, 3, InventoryItems.WOODEN_SHOVEL, 1,
            ANY_PLANKS,
            InventoryItems.STICK,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 2, InventoryItems.WOODEN_HOE, 1,
            ANY_PLANKS, ANY_PLANKS,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(3, 3, InventoryItems.STONE_PICKAXE, 1,
            GameConfig.COBBLESTONE, GameConfig.COBBLESTONE, GameConfig.COBBLESTONE,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR),
        CraftingRecipe.shaped(1, 3, InventoryItems.STONE_SWORD, 1,
            GameConfig.COBBLESTONE,
            GameConfig.COBBLESTONE,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 3, InventoryItems.STONE_AXE, 1,
            GameConfig.COBBLESTONE, GameConfig.COBBLESTONE,
            GameConfig.COBBLESTONE, InventoryItems.STICK,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(1, 3, InventoryItems.STONE_SHOVEL, 1,
            GameConfig.COBBLESTONE,
            InventoryItems.STICK,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 2, InventoryItems.STONE_HOE, 1,
            GameConfig.COBBLESTONE, GameConfig.COBBLESTONE,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(3, 3, InventoryItems.IRON_PICKAXE, 1,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR),
        CraftingRecipe.shaped(1, 3, InventoryItems.IRON_SWORD, 1,
            InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 3, InventoryItems.IRON_AXE, 1,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, InventoryItems.STICK,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(1, 3, InventoryItems.IRON_SHOVEL, 1,
            InventoryItems.IRON_INGOT,
            InventoryItems.STICK,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 2, InventoryItems.IRON_HOE, 1,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(3, 2, InventoryItems.ITEM_BUCKET, 1,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT,
            GameConfig.AIR, InventoryItems.IRON_INGOT, GameConfig.AIR),
        CraftingRecipe.shaped(3, 3, InventoryItems.SHIELD, 1,
            ANY_PLANKS, InventoryItems.IRON_INGOT, ANY_PLANKS,
            ANY_PLANKS, ANY_PLANKS, ANY_PLANKS,
            GameConfig.AIR, ANY_PLANKS, GameConfig.AIR),
        CraftingRecipe.shaped(3, 2, GameConfig.RED_BED, 1,
            InventoryItems.WOOL, InventoryItems.WOOL, InventoryItems.WOOL,
            ANY_PLANKS, ANY_PLANKS, ANY_PLANKS),
        CraftingRecipe.shaped(3, 2, InventoryItems.DIAMOND_PICKAXE, 1,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM,
            GameConfig.AIR, InventoryItems.STICK, GameConfig.AIR),
        CraftingRecipe.shaped(1, 3, InventoryItems.DIAMOND_SWORD, 1,
            InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 3, InventoryItems.DIAMOND_AXE, 1,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, InventoryItems.STICK,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shaped(1, 3, InventoryItems.DIAMOND_SHOVEL, 1,
            InventoryItems.DIAMOND_ITEM,
            InventoryItems.STICK,
            InventoryItems.STICK),
        CraftingRecipe.shaped(2, 2, InventoryItems.DIAMOND_HOE, 1,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM,
            GameConfig.AIR, InventoryItems.STICK),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_PICKAXE, 1, InventoryItems.DIAMOND_PICKAXE, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_SWORD, 1, InventoryItems.DIAMOND_SWORD, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_AXE, 1, InventoryItems.DIAMOND_AXE, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_SHOVEL, 1, InventoryItems.DIAMOND_SHOVEL, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_HOE, 1, InventoryItems.DIAMOND_HOE, GameConfig.OBSIDIAN),
        CraftingRecipe.shaped(3, 2, InventoryItems.IRON_HELMET, 1,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT),
        CraftingRecipe.shaped(3, 3, InventoryItems.IRON_CHESTPLATE, 1,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT),
        CraftingRecipe.shaped(3, 3, InventoryItems.IRON_LEGGINGS, 1,
            InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT),
        CraftingRecipe.shaped(3, 2, InventoryItems.IRON_BOOTS, 1,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT,
            InventoryItems.IRON_INGOT, GameConfig.AIR, InventoryItems.IRON_INGOT),
        CraftingRecipe.shaped(3, 2, InventoryItems.DIAMOND_HELMET, 1,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, GameConfig.AIR, InventoryItems.DIAMOND_ITEM),
        CraftingRecipe.shaped(3, 3, InventoryItems.DIAMOND_CHESTPLATE, 1,
            InventoryItems.DIAMOND_ITEM, GameConfig.AIR, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM),
        CraftingRecipe.shaped(3, 3, InventoryItems.DIAMOND_LEGGINGS, 1,
            InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, GameConfig.AIR, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, GameConfig.AIR, InventoryItems.DIAMOND_ITEM),
        CraftingRecipe.shaped(3, 2, InventoryItems.DIAMOND_BOOTS, 1,
            InventoryItems.DIAMOND_ITEM, GameConfig.AIR, InventoryItems.DIAMOND_ITEM,
            InventoryItems.DIAMOND_ITEM, GameConfig.AIR, InventoryItems.DIAMOND_ITEM),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_HELMET, 1, InventoryItems.DIAMOND_HELMET, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_CHESTPLATE, 1, InventoryItems.DIAMOND_CHESTPLATE, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_LEGGINGS, 1, InventoryItems.DIAMOND_LEGGINGS, GameConfig.OBSIDIAN),
        CraftingRecipe.shapeless(InventoryItems.NETHERITE_BOOTS, 1, InventoryItems.DIAMOND_BOOTS, GameConfig.OBSIDIAN)
    };

    private CraftingRecipes() {
    }

    static CraftingRecipe findMatch(ItemStack[] craftGrid) {
        return findMatch(craftGrid, 2, 2);
    }

    static CraftingRecipe findMatch(ItemStack[] craftGrid, int gridWidth, int gridHeight) {
        CraftingRecipe shapeless = findShapelessMatch(craftGrid);
        if (shapeless != null) {
            return shapeless;
        }
        int minX = gridWidth;
        int minY = gridHeight;
        int maxX = -1;
        int maxY = -1;
        for (int index = 0; index < craftGrid.length; index++) {
            if (craftGrid[index].isEmpty()) {
                continue;
            }
            int x = index % gridWidth;
            int y = index / gridWidth;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        if (maxX < minX || maxY < minY) {
            return null;
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        for (CraftingRecipe recipe : RECIPES) {
            if (recipe.shapeless()) {
                continue;
            }
            if (recipe.width != width || recipe.height != height) {
                continue;
            }

            boolean matches = true;
            for (int y = 0; y < height && matches; y++) {
                for (int x = 0; x < width; x++) {
                    byte actual = craftGrid[(minY + y) * gridWidth + (minX + x)].isEmpty()
                        ? GameConfig.AIR
                        : craftGrid[(minY + y) * gridWidth + (minX + x)].itemId;
                    byte expected = recipe.pattern[y * recipe.width + x];
                    if (!matchesIngredient(expected, actual)) {
                        matches = false;
                        break;
                    }
                }
            }
            if (matches) {
                return recipe;
            }
        }
        return null;
    }

    private static CraftingRecipe findShapelessMatch(ItemStack[] craftGrid) {
        byte[] actual = new byte[craftGrid.length];
        int actualCount = 0;
        for (ItemStack stack : craftGrid) {
            if (!stack.isEmpty()) {
                actual[actualCount++] = stack.itemId;
            }
        }
        if (actualCount == 0) {
            return null;
        }
        for (CraftingRecipe recipe : RECIPES) {
            if (!recipe.shapeless() || recipe.shapelessIngredients.length != actualCount) {
                continue;
            }
            boolean[] used = new boolean[actualCount];
            boolean matches = true;
            for (byte expected : recipe.shapelessIngredients) {
                boolean found = false;
                for (int i = 0; i < actualCount; i++) {
                    if (!used[i] && matchesIngredient(expected, actual[i])) {
                        used[i] = true;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matchesIngredient(byte expected, byte actual) {
        if (expected == ANY_PLANKS) {
            return actual == InventoryItems.OAK_PLANKS
                || actual == GameConfig.PINE_PLANKS
                || actual == GameConfig.BIRCH_PLANKS;
        }
        return actual == expected;
    }
}

final class FurnaceRecipe {
    final byte input;
    final byte output;
    final int outputCount;
    final double cookSeconds;

    FurnaceRecipe(byte input, byte output, int outputCount, double cookSeconds) {
        this.input = input;
        this.output = output;
        this.outputCount = outputCount;
        this.cookSeconds = cookSeconds;
    }
}

final class FurnaceRecipes {
    private static final FurnaceRecipe[] RECIPES = {
        new FurnaceRecipe(GameConfig.IRON_ORE, InventoryItems.IRON_INGOT, 1, 6.0),
        new FurnaceRecipe(GameConfig.DEEPSLATE_IRON_ORE, InventoryItems.IRON_INGOT, 1, 6.0),
        new FurnaceRecipe(GameConfig.SAND, GameConfig.GLASS, 1, 5.0),
        new FurnaceRecipe(GameConfig.COBBLESTONE, GameConfig.STONE, 1, 5.0),
        new FurnaceRecipe(InventoryItems.RAW_PORK, InventoryItems.COOKED_PORK, 1, 5.0),
        new FurnaceRecipe(InventoryItems.RAW_BEEF, InventoryItems.COOKED_BEEF, 1, 5.0),
        new FurnaceRecipe(InventoryItems.RAW_MUTTON, InventoryItems.COOKED_MUTTON, 1, 5.0),
        new FurnaceRecipe(InventoryItems.RAW_HERRING, InventoryItems.COOKED_HERRING, 1, 4.0),
        new FurnaceRecipe(InventoryItems.RAW_SALMON, InventoryItems.COOKED_SALMON, 1, 4.0),
        new FurnaceRecipe(InventoryItems.POTATO, InventoryItems.BAKED_POTATO, 1, 4.0)
    };

    private FurnaceRecipes() {
    }

    static FurnaceRecipe find(byte input) {
        for (FurnaceRecipe recipe : RECIPES) {
            if (recipe.input == input) {
                return recipe;
            }
        }
        return null;
    }

    static double fuelSeconds(byte item) {
        switch (item) {
            case InventoryItems.COAL_ITEM:
            case GameConfig.COAL_ORE:
            case GameConfig.DEEPSLATE_COAL_ORE:
                return 80.0;
            case GameConfig.OAK_LOG:
            case GameConfig.PINE_LOG:
                return 15.0;
            case InventoryItems.OAK_PLANKS:
                return 10.0;
            case InventoryItems.STICK:
                return 4.0;
            default:
                return 0.0;
        }
    }
}

final class ContainerInventory {
    final ItemStack[] slots;

    ContainerInventory(int size) {
        slots = new ItemStack[size];
        Arrays.setAll(slots, index -> new ItemStack());
    }

    ItemStack getStack(int index) {
        return slots[index];
    }

    void writeTo(DataOutputStream output) throws IOException {
        output.writeInt(slots.length);
        for (ItemStack stack : slots) {
            InventoryCodecs.writeStack(output, stack);
        }
    }

    static ContainerInventory readFrom(DataInputStream input, int maxSlots) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > maxSlots) {
            throw new IOException("invalid container size: " + size);
        }
        ContainerInventory container = new ContainerInventory(size);
        for (ItemStack stack : container.slots) {
            InventoryCodecs.readStack(input, stack);
        }
        return container;
    }

    void copyFrom(ContainerInventory source) {
        if (source == null || source.slots.length != slots.length) {
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            slots[i].copyFrom(source.slots[i]);
        }
    }
}

final class FurnaceBlockEntity {
    final ItemStack input = new ItemStack();
    final ItemStack fuel = new ItemStack();
    final ItemStack output = new ItemStack();
    double burnRemaining;
    double burnTotal;
    double cookProgress;
    double cookTotal;

    void tick(double deltaTime) {
        FurnaceRecipe recipe = input.isEmpty() ? null : FurnaceRecipes.find(input.itemId);
        if (recipe == null || !canOutput(recipe)) {
            cookProgress = 0.0;
            cookTotal = 0.0;
            burnRemaining = Math.max(0.0, burnRemaining - deltaTime);
            return;
        }

        if (burnRemaining <= 0.0) {
            double fuelSeconds = fuel.isEmpty() ? 0.0 : FurnaceRecipes.fuelSeconds(fuel.itemId);
            if (fuelSeconds <= 0.0) {
                cookProgress = 0.0;
                cookTotal = recipe.cookSeconds;
                return;
            }
            fuel.count--;
            if (fuel.count <= 0) {
                fuel.clear();
            }
            burnRemaining = fuelSeconds;
            burnTotal = fuelSeconds;
        }

        burnRemaining = Math.max(0.0, burnRemaining - deltaTime);
        cookTotal = recipe.cookSeconds;
        cookProgress += deltaTime;
        if (cookProgress >= recipe.cookSeconds) {
            input.count--;
            if (input.count <= 0) {
                input.clear();
            }
            if (output.isEmpty()) {
                output.set(recipe.output, recipe.outputCount);
            } else {
                output.count += recipe.outputCount;
            }
            cookProgress = 0.0;
        }
    }

    private boolean canOutput(FurnaceRecipe recipe) {
        if (output.isEmpty()) {
            return true;
        }
        return output.itemId == recipe.output
            && output.count + recipe.outputCount <= InventoryItems.maxStackSize(recipe.output);
    }

    void writeTo(DataOutputStream outputStream) throws IOException {
        InventoryCodecs.writeStack(outputStream, input);
        InventoryCodecs.writeStack(outputStream, fuel);
        InventoryCodecs.writeStack(outputStream, output);
        outputStream.writeDouble(burnRemaining);
        outputStream.writeDouble(burnTotal);
        outputStream.writeDouble(cookProgress);
        outputStream.writeDouble(cookTotal);
    }

    void readFrom(DataInputStream inputStream) throws IOException {
        InventoryCodecs.readStack(inputStream, input);
        InventoryCodecs.readStack(inputStream, fuel);
        InventoryCodecs.readStack(inputStream, output);
        burnRemaining = inputStream.readDouble();
        burnTotal = inputStream.readDouble();
        cookProgress = inputStream.readDouble();
        cookTotal = inputStream.readDouble();
    }

    void copyFrom(FurnaceBlockEntity source) {
        if (source == null) {
            return;
        }
        input.copyFrom(source.input);
        fuel.copyFrom(source.fuel);
        output.copyFrom(source.output);
        burnRemaining = source.burnRemaining;
        burnTotal = source.burnTotal;
        cookProgress = source.cookProgress;
        cookTotal = source.cookTotal;
    }
}

final class PlayerInventory {
    static final int STORAGE_SIZE = 36;
    static final int HOTBAR_SIZE = 9;
    static final int ARMOR_SIZE = 4;
    static final int CRAFT_SIZE = 4;
    static final int WORKBENCH_CRAFT_SIZE = 9;

    private final ItemStack[] storage = createSlots(STORAGE_SIZE);
    private final ItemStack[] hotbar = createSlots(HOTBAR_SIZE);
    private final ItemStack[] armor = createSlots(ARMOR_SIZE);
    private final ItemStack[] craftGrid = createSlots(CRAFT_SIZE);
    private final ItemStack[] workbenchGrid = createSlots(WORKBENCH_CRAFT_SIZE);
    private final ItemStack offhand = new ItemStack();
    private final ItemStack cursor = new ItemStack();
    private final ItemStack craftResult = new ItemStack();
    private final ItemStack workbenchResult = new ItemStack();
    private boolean craftDirty = true;
    private boolean workbenchCraftDirty = true;

    PlayerInventory() {
    }

    ItemStack getStorageStack(int index) {
        return storage[index];
    }

    ItemStack getHotbarStack(int index) {
        return hotbar[index];
    }

    ItemStack getArmorStack(int index) {
        return armor[index];
    }

    ItemStack getCraftStack(int index) {
        return craftGrid[index];
    }

    ItemStack getWorkbenchCraftStack(int index) {
        return workbenchGrid[index];
    }

    ItemStack getOffhandStack() {
        return offhand;
    }

    int getArmorProtection() {
        int protection = 0;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) {
                continue;
            }
            switch (stack.itemId) {
                case InventoryItems.IRON_HELMET:
                    protection += 2;
                    break;
                case InventoryItems.IRON_CHESTPLATE:
                    protection += 6;
                    break;
                case InventoryItems.IRON_LEGGINGS:
                    protection += 5;
                    break;
                case InventoryItems.IRON_BOOTS:
                    protection += 2;
                    break;
                case InventoryItems.DIAMOND_HELMET:
                case InventoryItems.NETHERITE_HELMET:
                    protection += 3;
                    break;
                case InventoryItems.DIAMOND_CHESTPLATE:
                case InventoryItems.NETHERITE_CHESTPLATE:
                    protection += 8;
                    break;
                case InventoryItems.DIAMOND_LEGGINGS:
                case InventoryItems.NETHERITE_LEGGINGS:
                    protection += 6;
                    break;
                case InventoryItems.DIAMOND_BOOTS:
                case InventoryItems.NETHERITE_BOOTS:
                    protection += 3;
                    break;
                default:
                    break;
            }
        }
        return protection;
    }

    ItemStack getCursorStack() {
        return cursor;
    }

    ItemStack getCraftResultStack() {
        refreshCraftResult();
        return craftResult;
    }

    ItemStack getWorkbenchCraftResultStack() {
        refreshWorkbenchCraftResult();
        return workbenchResult;
    }

    byte getSelectedItemId(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= hotbar.length || hotbar[hotbarSlot].isEmpty()) {
            return GameConfig.AIR;
        }
        return hotbar[hotbarSlot].itemId;
    }

    boolean damageSelectedItem(int hotbarSlot, int amount) {
        if (hotbarSlot < 0 || hotbarSlot >= hotbar.length) {
            return false;
        }
        ItemStack stack = hotbar[hotbarSlot];
        if (stack.isEmpty() || !InventoryItems.isDurableItem(stack.itemId)) {
            return false;
        }
        stack.damage(amount);
        return true;
    }

    boolean consumeSelectedItem(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= hotbar.length) {
            return false;
        }
        ItemStack stack = hotbar[hotbarSlot];
        if (stack.isEmpty()) {
            return false;
        }
        stack.count--;
        if (stack.count <= 0) {
            stack.clear();
        }
        return true;
    }

    void clearCursor() {
        cursor.clear();
    }

    void clearAll() {
        clearSlots(storage);
        clearSlots(hotbar);
        clearSlots(armor);
        clearSlots(craftGrid);
        clearSlots(workbenchGrid);
        offhand.clear();
        cursor.clear();
        craftResult.clear();
        workbenchResult.clear();
        markCraftDirty();
        markWorkbenchCraftDirty();
    }

    void writeTo(DataOutputStream output) throws IOException {
        writeSlots(output, hotbar);
        writeSlots(output, storage);
        writeSlots(output, armor);
        writeSlots(output, craftGrid);
        writeSlots(output, workbenchGrid);
        InventoryCodecs.writeStack(output, offhand);
        InventoryCodecs.writeStack(output, cursor);
    }

    void readFrom(DataInputStream input) throws IOException {
        readSlots(input, hotbar, HOTBAR_SIZE);
        readSlots(input, storage, STORAGE_SIZE);
        readSlots(input, armor, ARMOR_SIZE);
        readSlots(input, craftGrid, CRAFT_SIZE);
        readSlots(input, workbenchGrid, WORKBENCH_CRAFT_SIZE);
        InventoryCodecs.readStack(input, offhand);
        InventoryCodecs.readStack(input, cursor);
        craftResult.clear();
        workbenchResult.clear();
        markCraftDirty();
        markWorkbenchCraftDirty();
    }

    void copyFrom(PlayerInventory source) {
        if (source == null) {
            return;
        }
        copySlots(hotbar, source.hotbar);
        copySlots(storage, source.storage);
        copySlots(armor, source.armor);
        copySlots(craftGrid, source.craftGrid);
        copySlots(workbenchGrid, source.workbenchGrid);
        offhand.copyFrom(source.offhand);
        cursor.copyFrom(source.cursor);
        craftResult.clear();
        workbenchResult.clear();
        markCraftDirty();
        markWorkbenchCraftDirty();
    }

    ItemStack peekSlot(InventorySlotRef ref, boolean creativeMode) {
        return peekSlot(ref, creativeMode, null, null);
    }

    ItemStack peekSlot(InventorySlotRef ref, boolean creativeMode, ContainerInventory chest, FurnaceBlockEntity furnace) {
        if (ref == null) {
            return null;
        }
        switch (ref.group) {
            case STORAGE:
                return storage[ref.index];
            case HOTBAR:
                return hotbar[ref.index];
            case ARMOR:
                return armor[ref.index];
            case OFFHAND:
                return offhand;
            case CRAFT:
                return craftGrid[ref.index];
            case CRAFT_RESULT:
                refreshCraftResult();
                return craftResult;
            case CRAFT_3X3:
                return workbenchGrid[ref.index];
            case CRAFT_3X3_RESULT:
                refreshWorkbenchCraftResult();
                return workbenchResult;
            case CHEST_CONTAINER:
                return chest == null ? null : chest.getStack(ref.index);
            case FURNACE_INPUT:
                return furnace == null ? null : furnace.input;
            case FURNACE_FUEL:
                return furnace == null ? null : furnace.fuel;
            case FURNACE_OUTPUT:
                return furnace == null ? null : furnace.output;
            case CREATIVE:
                if (!creativeMode || ref.index < 0 || ref.index >= InventoryItems.CREATIVE_ITEMS.length) {
                    return null;
                }
                byte creativeItem = InventoryItems.CREATIVE_ITEMS[ref.index];
                return new ItemStack(creativeItem, 1);
            case TRASH:
                return null;
            default:
                return null;
        }
    }

    boolean isSlotAccepting(InventorySlotRef ref, ItemStack stack) {
        if (ref == null || stack == null || stack.isEmpty()) {
            return false;
        }
        switch (ref.group) {
            case STORAGE:
            case HOTBAR:
            case CRAFT:
            case CRAFT_3X3:
            case CHEST_CONTAINER:
            case FURNACE_INPUT:
                return true;
            case FURNACE_FUEL:
                return FurnaceRecipes.fuelSeconds(stack.itemId) > 0.0;
            case ARMOR:
                return InventoryItems.armorSlotIndex(stack.itemId) == ref.index;
            case OFFHAND:
                return true;
            default:
                return false;
        }
    }

    boolean handleClick(InventorySlotRef ref, boolean creativeMode, boolean rightClick, boolean shiftDown) {
        return handleClick(ref, creativeMode, rightClick, shiftDown, false, null, null);
    }

    boolean handleClick(InventorySlotRef ref, boolean creativeMode, boolean rightClick, boolean shiftDown,
                        ContainerInventory chest, FurnaceBlockEntity furnace) {
        return handleClick(ref, creativeMode, rightClick, shiftDown, false, chest, furnace);
    }

    boolean handleClick(InventorySlotRef ref, boolean creativeMode, boolean rightClick, boolean shiftDown,
                        boolean creativeStackClick, ContainerInventory chest, FurnaceBlockEntity furnace) {
        refreshCraftResult();
        refreshWorkbenchCraftResult();
        if (ref == null) {
            cursor.clear();
            return true;
        }
        if (ref.group == InventorySlotGroup.TRASH) {
            if (shiftDown && creativeMode) {
                clearAll();
            } else {
                cursor.clear();
            }
            return true;
        }
        if (shiftDown && !rightClick) {
            return quickMove(ref, creativeMode, chest, furnace);
        }
        if (ref.group == InventorySlotGroup.CREATIVE) {
            return takeFromCreative(ref, creativeMode, creativeStackClick);
        }
        if (ref.group == InventorySlotGroup.CRAFT_RESULT) {
            return takeCraftResult(rightClick);
        }
        if (ref.group == InventorySlotGroup.CRAFT_3X3_RESULT) {
            return takeWorkbenchCraftResult(rightClick);
        }
        if (ref.group == InventorySlotGroup.FURNACE_OUTPUT && furnace != null) {
            return rightClick ? handleRightClick(ref, chest, furnace) : handleLeftClick(ref, chest, furnace);
        }
        return rightClick ? handleRightClick(ref, chest, furnace) : handleLeftClick(ref, chest, furnace);
    }

    boolean addItem(byte itemId, int count) {
        return addItem(itemId, count, 0);
    }

    boolean addItem(byte itemId, int count, int durabilityDamage) {
        if (itemId == GameConfig.AIR || count <= 0) {
            return true;
        }

        int damage = normalizedDurabilityDamage(itemId, durabilityDamage);
        int remaining = mergeIntoExisting(hotbar, itemId, count, damage, null);
        remaining = mergeIntoExisting(storage, itemId, remaining, damage, null);
        remaining = mergeIntoEmpty(hotbar, itemId, remaining, damage, null);
        remaining = mergeIntoEmpty(storage, itemId, remaining, damage, null);
        return remaining == 0;
    }

    void markCraftDirty() {
        craftDirty = true;
    }

    void markWorkbenchCraftDirty() {
        workbenchCraftDirty = true;
    }

    void returnTransientCraftingToInventory() {
        returnSlotsToInventory(craftGrid);
        returnSlotsToInventory(workbenchGrid);
        craftResult.clear();
        workbenchResult.clear();
        markCraftDirty();
        markWorkbenchCraftDirty();
    }

    private boolean takeFromCreative(InventorySlotRef ref, boolean creativeMode, boolean stackClick) {
        if (!creativeMode || ref.index < 0 || ref.index >= InventoryItems.CREATIVE_ITEMS.length) {
            return false;
        }
        byte itemId = InventoryItems.CREATIVE_ITEMS[ref.index];
        int count = stackClick ? InventoryItems.maxStackSize(itemId) : 1;
        cursor.set(itemId, count);
        return true;
    }

    private boolean takeCraftResult(boolean rightClick) {
        refreshCraftResult();
        if (craftResult.isEmpty()) {
            return false;
        }

        int amount = rightClick ? 1 : craftResult.count;
        if (cursor.isEmpty()) {
            cursor.set(craftResult.itemId, amount);
        } else if (cursor.itemId == craftResult.itemId
            && cursor.count + amount <= InventoryItems.maxStackSize(cursor.itemId)) {
            cursor.count += amount;
        } else {
            return false;
        }

        consumeCraftIngredients();
        refreshCraftResult();
        return true;
    }

    private boolean takeWorkbenchCraftResult(boolean rightClick) {
        refreshWorkbenchCraftResult();
        if (workbenchResult.isEmpty()) {
            return false;
        }

        int amount = rightClick ? 1 : workbenchResult.count;
        if (cursor.isEmpty()) {
            cursor.set(workbenchResult.itemId, amount);
        } else if (cursor.itemId == workbenchResult.itemId
            && cursor.count + amount <= InventoryItems.maxStackSize(cursor.itemId)) {
            cursor.count += amount;
        } else {
            return false;
        }

        consumeWorkbenchCraftIngredients();
        refreshWorkbenchCraftResult();
        return true;
    }

    private boolean handleLeftClick(InventorySlotRef ref, ContainerInventory chest, FurnaceBlockEntity furnace) {
        ItemStack slot = mutableSlot(ref, chest, furnace);
        if (slot == null) {
            return false;
        }

        if (cursor.isEmpty()) {
            if (slot.isEmpty()) {
                return false;
            }
            cursor.copyFrom(slot);
            slot.clear();
            onSlotChanged(ref);
            return true;
        }

        if (slot.isEmpty()) {
            if (!isSlotAccepting(ref, cursor)) {
                return false;
            }
            slot.copyFrom(cursor);
            cursor.clear();
            onSlotChanged(ref);
            return true;
        }

        int maxStack = InventoryItems.maxStackSize(slot.itemId);
        if (slot.itemId == cursor.itemId
            && slot.durabilityDamage == cursor.durabilityDamage
            && slot.count < maxStack
            && isSlotAccepting(ref, cursor)) {
            int transfer = Math.min(maxStack - slot.count, cursor.count);
            if (transfer <= 0) {
                return false;
            }
            slot.count += transfer;
            cursor.count -= transfer;
            if (cursor.count <= 0) {
                cursor.clear();
            }
            onSlotChanged(ref);
            return true;
        }

        if (!isSlotAccepting(ref, cursor)) {
            return false;
        }

        ItemStack temp = slot.copy();
        slot.copyFrom(cursor);
        cursor.copyFrom(temp);
        onSlotChanged(ref);
        return true;
    }

    private boolean handleRightClick(InventorySlotRef ref, ContainerInventory chest, FurnaceBlockEntity furnace) {
        ItemStack slot = mutableSlot(ref, chest, furnace);
        if (slot == null) {
            return false;
        }

        if (cursor.isEmpty()) {
            if (slot.isEmpty()) {
                return false;
            }
            int amount = (slot.count + 1) / 2;
            cursor.set(slot.itemId, amount);
            cursor.durabilityDamage = slot.durabilityDamage;
            slot.count -= amount;
            if (slot.count <= 0) {
                slot.clear();
            }
            onSlotChanged(ref);
            return true;
        }

        if (!isSlotAccepting(ref, cursor)) {
            return false;
        }

        if (slot.isEmpty()) {
            slot.set(cursor.itemId, 1);
            slot.durabilityDamage = cursor.durabilityDamage;
            cursor.count--;
            if (cursor.count <= 0) {
                cursor.clear();
            }
            onSlotChanged(ref);
            return true;
        }

        if (slot.itemId == cursor.itemId
            && slot.durabilityDamage == cursor.durabilityDamage
            && slot.count < InventoryItems.maxStackSize(slot.itemId)) {
            slot.count++;
            cursor.count--;
            if (cursor.count <= 0) {
                cursor.clear();
            }
            onSlotChanged(ref);
            return true;
        }

        return false;
    }

    private boolean quickMove(InventorySlotRef ref, boolean creativeMode, ContainerInventory chest, FurnaceBlockEntity furnace) {
        if (ref == null) {
            return false;
        }
        if (ref.group == InventorySlotGroup.CREATIVE) {
            if (!creativeMode || ref.index < 0 || ref.index >= InventoryItems.CREATIVE_ITEMS.length) {
                return false;
            }
            byte itemId = InventoryItems.CREATIVE_ITEMS[ref.index];
            return addItem(itemId, 1);
        }
        if (ref.group == InventorySlotGroup.CRAFT_RESULT) {
            boolean changed = false;
            refreshCraftResult();
            while (!craftResult.isEmpty() && addItem(craftResult.itemId, craftResult.count)) {
                consumeCraftIngredients();
                refreshCraftResult();
                changed = true;
            }
            return changed;
        }
        if (ref.group == InventorySlotGroup.CRAFT_3X3_RESULT) {
            boolean changed = false;
            refreshWorkbenchCraftResult();
            while (!workbenchResult.isEmpty() && addItem(workbenchResult.itemId, workbenchResult.count)) {
                consumeWorkbenchCraftIngredients();
                refreshWorkbenchCraftResult();
                changed = true;
            }
            return changed;
        }

        ItemStack slot = mutableSlot(ref, chest, furnace);
        if (slot == null || slot.isEmpty()) {
            return false;
        }

        if (ref.group == InventorySlotGroup.CHEST_CONTAINER
            || ref.group == InventorySlotGroup.FURNACE_INPUT
            || ref.group == InventorySlotGroup.FURNACE_FUEL
            || ref.group == InventorySlotGroup.FURNACE_OUTPUT) {
            boolean moved = moveStackToArray(slot, hotbar);
            moved = moveStackToArray(slot, storage) || moved;
            onSlotChanged(ref);
            return moved;
        }

        if (ref.group == InventorySlotGroup.HOTBAR || ref.group == InventorySlotGroup.STORAGE) {
            if (chest != null) {
                boolean movedToChest = moveStackToArray(slot, chest.slots);
                onSlotChanged(ref);
                return movedToChest;
            }
            if (furnace != null) {
                boolean movedToFurnace = moveStackToFurnace(slot, furnace);
                onSlotChanged(ref);
                return movedToFurnace;
            }
            if (InventoryItems.isArmor(slot.itemId)) {
                int armorIndex = InventoryItems.armorSlotIndex(slot.itemId);
                if (armorIndex >= 0 && armor[armorIndex].isEmpty()) {
                    armor[armorIndex].copyFrom(slot);
                    slot.clear();
                    onSlotChanged(ref);
                    return true;
                }
            }
            if (InventoryItems.isOffhandPreferred(slot.itemId) && offhand.isEmpty()) {
                offhand.copyFrom(slot);
                slot.clear();
                onSlotChanged(ref);
                return true;
            }

            ItemStack[] primaryTarget = ref.group == InventorySlotGroup.HOTBAR ? storage : hotbar;
            boolean moved = moveStackToArray(slot, primaryTarget);
            if (!slot.isEmpty()) {
                moved = moveStackToArray(slot, ref.group == InventorySlotGroup.HOTBAR ? hotbar : storage) || moved;
            }
            onSlotChanged(ref);
            return moved;
        }

        if (ref.group == InventorySlotGroup.ARMOR || ref.group == InventorySlotGroup.OFFHAND || ref.group == InventorySlotGroup.CRAFT || ref.group == InventorySlotGroup.CRAFT_3X3) {
            boolean moved = moveStackToArray(slot, hotbar);
            moved = moveStackToArray(slot, storage) || moved;
            onSlotChanged(ref);
            return moved;
        }

        return false;
    }

    private boolean moveStackToFurnace(ItemStack source, FurnaceBlockEntity furnace) {
        if (source.isEmpty()) {
            return false;
        }
        int originalCount = source.count;
        if (FurnaceRecipes.find(source.itemId) != null) {
            mergeIntoSingleSlot(source, furnace.input);
        }
        if (!source.isEmpty() && FurnaceRecipes.fuelSeconds(source.itemId) > 0.0) {
            mergeIntoSingleSlot(source, furnace.fuel);
        }
        return source.count != originalCount;
    }

    private void mergeIntoSingleSlot(ItemStack source, ItemStack target) {
        if (source.isEmpty()) {
            return;
        }
        int maxStack = InventoryItems.maxStackSize(source.itemId);
        if (target.isEmpty()) {
            int transfer = Math.min(maxStack, source.count);
            target.set(source.itemId, transfer);
            source.count -= transfer;
        } else if (target.itemId == source.itemId && target.durabilityDamage == source.durabilityDamage && target.count < maxStack) {
            int transfer = Math.min(maxStack - target.count, source.count);
            target.count += transfer;
            source.count -= transfer;
        }
        if (source.count <= 0) {
            source.clear();
        }
    }

    private boolean moveStackToArray(ItemStack source, ItemStack[] targetSlots) {
        if (source.isEmpty()) {
            return false;
        }
        int originalCount = source.count;
        source.count = mergeIntoExisting(targetSlots, source.itemId, source.count, source.durabilityDamage, source);
        source.count = mergeIntoEmpty(targetSlots, source.itemId, source.count, source.durabilityDamage, source);
        if (source.count <= 0) {
            source.clear();
        }
        return source.count != originalCount;
    }

    private void refreshCraftResult() {
        if (!craftDirty) {
            return;
        }
        craftDirty = false;
        CraftingRecipe recipe = CraftingRecipes.findMatch(craftGrid);
        if (recipe == null) {
            craftResult.clear();
        } else {
            craftResult.set(recipe.resultItem, recipe.resultCount);
        }
    }

    private void refreshWorkbenchCraftResult() {
        if (!workbenchCraftDirty) {
            return;
        }
        workbenchCraftDirty = false;
        CraftingRecipe recipe = CraftingRecipes.findMatch(workbenchGrid, 3, 3);
        if (recipe == null) {
            workbenchResult.clear();
        } else {
            workbenchResult.set(recipe.resultItem, recipe.resultCount);
        }
    }

    private void consumeCraftIngredients() {
        for (ItemStack stack : craftGrid) {
            if (stack.isEmpty()) {
                continue;
            }
            stack.count--;
            if (stack.count <= 0) {
                stack.clear();
            }
        }
        markCraftDirty();
    }

    private void consumeWorkbenchCraftIngredients() {
        for (ItemStack stack : workbenchGrid) {
            if (stack.isEmpty()) {
                continue;
            }
            stack.count--;
            if (stack.count <= 0) {
                stack.clear();
            }
        }
        markWorkbenchCraftDirty();
    }

    private ItemStack mutableSlot(InventorySlotRef ref, ContainerInventory chest, FurnaceBlockEntity furnace) {
        switch (ref.group) {
            case STORAGE:
                return storage[ref.index];
            case HOTBAR:
                return hotbar[ref.index];
            case ARMOR:
                return armor[ref.index];
            case OFFHAND:
                return offhand;
            case CRAFT:
                return craftGrid[ref.index];
            case CRAFT_3X3:
                return workbenchGrid[ref.index];
            case CHEST_CONTAINER:
                return chest == null ? null : chest.getStack(ref.index);
            case FURNACE_INPUT:
                return furnace == null ? null : furnace.input;
            case FURNACE_FUEL:
                return furnace == null ? null : furnace.fuel;
            case FURNACE_OUTPUT:
                return furnace == null ? null : furnace.output;
            default:
                return null;
        }
    }

    private void onSlotChanged(InventorySlotRef ref) {
        if (ref.group == InventorySlotGroup.CRAFT) {
            markCraftDirty();
        } else if (ref.group == InventorySlotGroup.CRAFT_3X3) {
            markWorkbenchCraftDirty();
        }
    }

    private void returnSlotsToInventory(ItemStack[] slots) {
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) {
                continue;
            }
            moveStackToArray(stack, hotbar);
            moveStackToArray(stack, storage);
        }
    }

    private int mergeIntoExisting(ItemStack[] slots, byte itemId, int remaining, int durabilityDamage, ItemStack excluded) {
        int maxStack = InventoryItems.maxStackSize(itemId);
        int damage = normalizedDurabilityDamage(itemId, durabilityDamage);
        for (ItemStack slot : slots) {
            if (remaining <= 0) {
                return 0;
            }
            if (slot == excluded || slot.isEmpty() || slot.itemId != itemId || slot.durabilityDamage != damage || slot.count >= maxStack) {
                continue;
            }
            int transfer = Math.min(maxStack - slot.count, remaining);
            slot.count += transfer;
            remaining -= transfer;
        }
        return remaining;
    }

    private int mergeIntoEmpty(ItemStack[] slots, byte itemId, int remaining, int durabilityDamage, ItemStack excluded) {
        int maxStack = InventoryItems.maxStackSize(itemId);
        int damage = normalizedDurabilityDamage(itemId, durabilityDamage);
        for (ItemStack slot : slots) {
            if (remaining <= 0) {
                return 0;
            }
            if (slot == excluded || !slot.isEmpty()) {
                continue;
            }
            int transfer = Math.min(maxStack, remaining);
            slot.set(itemId, transfer);
            slot.durabilityDamage = damage;
            remaining -= transfer;
        }
        return remaining;
    }

    private int normalizedDurabilityDamage(byte itemId, int durabilityDamage) {
        if (!InventoryItems.isDurableItem(itemId)) {
            return 0;
        }
        return Math.max(0, Math.min(durabilityDamage, InventoryItems.maxDurability(itemId) - 1));
    }

    private ItemStack[] createSlots(int size) {
        ItemStack[] slots = new ItemStack[size];
        Arrays.setAll(slots, index -> new ItemStack());
        return slots;
    }

    private void clearSlots(ItemStack[] slots) {
        for (ItemStack slot : slots) {
            slot.clear();
        }
    }

    private void writeSlots(DataOutputStream output, ItemStack[] slots) throws IOException {
        output.writeInt(slots.length);
        for (ItemStack stack : slots) {
            InventoryCodecs.writeStack(output, stack);
        }
    }

    private void readSlots(DataInputStream input, ItemStack[] slots, int expectedSize) throws IOException {
        int size = input.readInt();
        if (size != expectedSize) {
            throw new IOException("invalid inventory section size: " + size);
        }
        for (ItemStack stack : slots) {
            InventoryCodecs.readStack(input, stack);
        }
    }

    private void copySlots(ItemStack[] target, ItemStack[] source) {
        for (int i = 0; i < target.length && i < source.length; i++) {
            target[i].copyFrom(source[i]);
        }
    }
}

final class InventoryCodecs {
    private InventoryCodecs() {
    }

    static void writeStack(DataOutputStream output, ItemStack stack) throws IOException {
        if (stack == null || stack.isEmpty()) {
            output.writeByte(GameConfig.AIR);
            output.writeInt(0);
            output.writeInt(0);
            return;
        }
        output.writeByte(stack.itemId);
        output.writeInt(stack.count);
        output.writeInt(stack.durabilityDamage);
    }

    static void readStack(DataInputStream input, ItemStack stack) throws IOException {
        byte itemId = input.readByte();
        int count = input.readInt();
        int durabilityDamage = input.readInt();
        if (itemId == GameConfig.AIR || count <= 0) {
            stack.clear();
            return;
        }
        int maxStack = InventoryItems.maxStackSize(itemId);
        stack.set(itemId, Math.max(1, Math.min(count, maxStack)));
        if (InventoryItems.isDurableItem(itemId)) {
            stack.durabilityDamage = Math.max(0, Math.min(durabilityDamage, InventoryItems.maxDurability(itemId) - 1));
        } else {
            stack.durabilityDamage = 0;
        }
    }
}
