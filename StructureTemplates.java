import java.util.Arrays;
import java.util.List;

final class StructureTemplates {
    interface Target {
        void setTemplateBlock(int worldX, int worldY, int worldZ, BlockState state);
        BlockState getTemplateBlock(int worldX, int worldY, int worldZ);
    }

    static final List<String> NAMES = Arrays.asList(
        "village_house_small_1",
        "village_house_small_2",
        "village_house_large",
        "village_farm",
        "village_plaza",
        "village_path_segment",
        "mineshaft_corridor",
        "mineshaft_crossing",
        "mineshaft_room",
        "mineshaft_support"
    );

    private StructureTemplates() {
    }

    static boolean exists(String name) {
        return NAMES.contains(normalize(name));
    }

    static String nameAt(int index) {
        if (NAMES.isEmpty()) {
            return "";
        }
        return NAMES.get(Math.floorMod(index, NAMES.size()));
    }

    static int indexOf(String name) {
        return NAMES.indexOf(normalize(name));
    }

    static String listNames() {
        return String.join(", ", NAMES);
    }

    static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase().replace('-', '_');
    }

    static boolean place(Target target, String name, int originX, int originY, int originZ, int rotation) {
        String template = normalize(name);
        int rot = ((rotation % 4) + 4) & 3;
        switch (template) {
            case "village_house_small_1":
                placeHouse(target, originX, originY, originZ, 7, 6, 3, rot, GameConfig.OAK_LOG, InventoryItems.OAK_PLANKS);
                return true;
            case "village_house_small_2":
                placeHouse(target, originX, originY, originZ, 6, 7, 3, rot, InventoryItems.OAK_PLANKS, InventoryItems.OAK_PLANKS);
                return true;
            case "village_house_large":
                placeHouse(target, originX, originY, originZ, 9, 8, 4, rot, GameConfig.COBBLESTONE, InventoryItems.OAK_PLANKS);
                return true;
            case "village_farm":
                placeFarm(target, originX, originY, originZ, 12, 9, rot);
                return true;
            case "village_plaza":
                placePlaza(target, originX, originY, originZ, rot);
                return true;
            case "village_path_segment":
                placePathSegment(target, originX, originY, originZ, 13, rot);
                return true;
            case "mineshaft_corridor":
                placeMineshaftCorridor(target, originX, originY, originZ, 16, rot);
                return true;
            case "mineshaft_crossing":
                placeMineshaftCrossing(target, originX, originY, originZ, rot);
                return true;
            case "mineshaft_room":
                placeMineshaftRoom(target, originX, originY, originZ, rot);
                return true;
            case "mineshaft_support":
                placeMineshaftSupport(target, originX, originY, originZ, rot);
                return true;
            default:
                return false;
        }
    }

    private static void placeHouse(Target target, int ox, int oy, int oz, int width, int depth, int wallHeight, int rot, byte wallBlock, byte roofBlock) {
        clearBox(target, ox - 1, oy + 1, oz - 1, width + 2, wallHeight + 4, depth + 2, rot);
        fillBox(target, ox - 1, oy - 1, oz - 1, width + 2, 1, depth + 2, rot, GameConfig.GRASS);
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                set(target, ox, oy, oz, x, 0, z, rot, InventoryItems.OAK_PLANKS);
                for (int y = 1; y <= wallHeight; y++) {
                    boolean edge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                    boolean corner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
                    boolean sideWindow = y == 2 && !corner && edge
                        && ((x == 0 || x == width - 1) ? z == depth / 2 : x == width / 2);
                    byte block = sideWindow ? GameConfig.GLASS : (edge ? (corner ? GameConfig.OAK_LOG : wallBlock) : GameConfig.AIR);
                    set(target, ox, oy, oz, x, y, z, rot, block);
                }
            }
        }
        int doorX = width / 2;
        set(target, ox, oy, oz, doorX, 1, 0, rot, Blocks.doorState(false, false, rot));
        set(target, ox, oy, oz, doorX, 2, 0, rot, Blocks.doorState(false, true, rot));
        set(target, ox, oy, oz, width - 2, 1, depth - 2, rot, GameConfig.CHEST);
        set(target, ox, oy, oz, 1, 1, depth - 2, rot, GameConfig.CRAFTING_TABLE);
        set(target, ox, oy, oz, 1, 2, depth - 2, rot, GameConfig.TORCH);
        int bedFacing = (1 + rot) & 3;
        int bedX = 1;
        int bedZ = Math.max(1, depth - 4);
        set(target, ox, oy, oz, bedX, 1, bedZ, rot, Blocks.bedState(false, bedFacing));
        set(target, ox, oy, oz, bedX + 1, 1, bedZ, rot, Blocks.bedState(true, bedFacing));
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                set(target, ox, oy, oz, x, wallHeight + 1, z, rot, roofBlock);
            }
        }
        for (int x = 0; x < width; x++) {
            set(target, ox, oy, oz, x, wallHeight + 2, depth / 2, rot, GameConfig.OAK_LOG);
        }
    }

    private static void placeFarm(Target target, int ox, int oy, int oz, int width, int depth, int rot) {
        clearBox(target, ox, oy + 1, oz, width, 2, depth, rot);
        int waterX = width / 2;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean edge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                if (edge) {
                    set(target, ox, oy, oz, x, 0, z, rot, InventoryItems.OAK_PLANKS);
                } else if (x == waterX) {
                    set(target, ox, oy, oz, x, 0, z, rot, GameConfig.WATER_SOURCE);
                } else {
                    set(target, ox, oy, oz, x, 0, z, rot, GameConfig.FARMLAND);
                    byte crop = farmCropFor(ox, oz, x, z);
                    int stage = 4 + Math.floorMod(ox * 31 + oz * 17 + x * 7 + z * 11, 4);
                    set(target, ox, oy, oz, x, 1, z, rot, Blocks.withData(crop, stage));
                }
            }
        }
    }

    private static byte farmCropFor(int ox, int oz, int x, int z) {
        int roll = Math.floorMod(ox * 13 + oz * 19 + x * 5 + z * 3, 3);
        if (roll == 1) {
            return GameConfig.CARROT_CROP;
        }
        if (roll == 2) {
            return GameConfig.POTATO_CROP;
        }
        return GameConfig.WHEAT_CROP;
    }

    private static void placePlaza(Target target, int ox, int oy, int oz, int rot) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (Math.abs(x - 3) + Math.abs(z - 3) <= 5) {
                    set(target, ox, oy, oz, x, 0, z, rot, GameConfig.GRAVEL);
                    set(target, ox, oy, oz, x, 1, z, rot, GameConfig.AIR);
                }
            }
        }
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                set(target, ox, oy, oz, x, 0, z, rot, x == 3 && z == 3 ? GameConfig.WATER_SOURCE : GameConfig.COBBLESTONE);
            }
        }
        set(target, ox, oy, oz, 2, 1, 2, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 4, 1, 2, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 2, 1, 4, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 4, 1, 4, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 2, 2, 2, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 4, 2, 2, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 2, 2, 4, rot, GameConfig.OAK_FENCE);
        set(target, ox, oy, oz, 4, 2, 4, rot, GameConfig.OAK_FENCE);
        byte roof = rot % 3 == 1 ? GameConfig.BIRCH_PLANKS : (rot % 3 == 2 ? GameConfig.PINE_PLANKS : InventoryItems.OAK_PLANKS);
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                if (Math.abs(x - 3) + Math.abs(z - 3) <= 3) {
                    set(target, ox, oy, oz, x, 3, z, rot, roof);
                }
            }
        }
        set(target, ox, oy, oz, 3, 4, 3, rot, GameConfig.TORCH);
    }

    private static void placePathSegment(Target target, int ox, int oy, int oz, int length, int rot) {
        for (int i = 0; i < length; i++) {
            for (int w = -1; w <= 1; w++) {
                set(target, ox, oy, oz, i, 0, w, rot, GameConfig.GRAVEL);
                set(target, ox, oy, oz, i, 1, w, rot, GameConfig.AIR);
                set(target, ox, oy, oz, i, 2, w, rot, GameConfig.AIR);
            }
        }
    }

    private static void placeMineshaftCorridor(Target target, int ox, int oy, int oz, int length, int rot) {
        for (int i = 0; i < length; i++) {
            carveShaftCell(target, ox, oy, oz, i, 0, rot);
            if ((i & 3) == 0) {
                placeMineshaftSupport(target, ox + i, oy, oz, rot);
            }
            if ((i & 1) == 0) {
                set(target, ox, oy, oz, i, 0, 0, rot, GameConfig.RAIL);
            }
        }
    }

    private static void placeMineshaftCrossing(Target target, int ox, int oy, int oz, int rot) {
        fillBox(target, ox - 2, oy - 1, oz - 2, 5, 1, 5, rot, InventoryItems.OAK_PLANKS);
        clearBox(target, ox - 2, oy, oz - 2, 5, 3, 5, rot);
        for (int i = -2; i <= 2; i++) {
            set(target, ox, oy, oz, i, 3, 0, rot, InventoryItems.OAK_PLANKS);
            set(target, ox, oy, oz, 0, 3, i, rot, InventoryItems.OAK_PLANKS);
        }
    }

    private static void placeMineshaftRoom(Target target, int ox, int oy, int oz, int rot) {
        fillBox(target, ox - 4, oy - 1, oz - 4, 9, 1, 9, rot, InventoryItems.OAK_PLANKS);
        clearBox(target, ox - 4, oy, oz - 4, 9, 4, 9, rot);
        set(target, ox, oy, oz, -3, 0, -3, rot, GameConfig.CHEST);
        set(target, ox, oy, oz, 3, 2, 3, rot, GameConfig.TORCH);
    }

    private static void placeMineshaftSupport(Target target, int ox, int oy, int oz, int rot) {
        for (int side = -1; side <= 1; side += 2) {
            for (int y = 0; y <= 2; y++) {
                set(target, ox, oy, oz, 0, y, side, rot, GameConfig.OAK_FENCE);
            }
        }
        for (int z = -1; z <= 1; z++) {
            set(target, ox, oy, oz, 0, 3, z, rot, InventoryItems.OAK_PLANKS);
        }
    }

    private static void carveShaftCell(Target target, int ox, int oy, int oz, int i, int centerZ, int rot) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(target, ox, oy, oz, i + dx, -1, centerZ + dz, rot, InventoryItems.OAK_PLANKS);
                for (int dy = 0; dy <= 2; dy++) {
                    set(target, ox, oy, oz, i + dx, dy, centerZ + dz, rot, GameConfig.AIR);
                }
            }
        }
    }

    private static void fillBox(Target target, int ox, int oy, int oz, int width, int height, int depth, int rot, byte block) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    set(target, ox, oy, oz, x, y, z, rot, block);
                }
            }
        }
    }

    private static void clearBox(Target target, int ox, int oy, int oz, int width, int height, int depth, int rot) {
        fillBox(target, ox, oy, oz, width, height, depth, rot, GameConfig.AIR);
    }

    private static void set(Target target, int ox, int oy, int oz, int x, int y, int z, int rot, byte block) {
        set(target, ox, oy, oz, x, y, z, rot, Blocks.stateFromLegacyId(block));
    }

    private static void set(Target target, int ox, int oy, int oz, int x, int y, int z, int rot, BlockState state) {
        int[] rotated = rotate(x, z, rot);
        target.setTemplateBlock(ox + rotated[0], oy + y, oz + rotated[1], state);
    }

    private static int[] rotate(int x, int z, int rot) {
        switch (rot & 3) {
            case 1:
                return new int[]{-z, x};
            case 2:
                return new int[]{-x, -z};
            case 3:
                return new int[]{z, -x};
            default:
                return new int[]{x, z};
        }
    }
}
