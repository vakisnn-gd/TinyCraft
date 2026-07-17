final class BlockDropRules {
    private static final DropPlan EMPTY = new DropPlan();

    private BlockDropRules() {
    }

    static DropPlan forBrokenBlock(byte block, int stateData, byte heldItem, boolean creativeMode) {
        if (creativeMode || !canHarvestBlock(block, heldItem)) {
            return EMPTY;
        }
        if (block == GameConfig.WHEAT_CROP) {
            if (stateData >= 7) {
                return new DropPlan(
                    new DropSpec(GameConfig.WHEAT_CROP, 1, 1, 0.2),
                    new DropSpec(InventoryItems.WHEAT_SEEDS, 1, 3, 0.3)
                );
            }
            return one(InventoryItems.WHEAT_SEEDS, 1, 1);
        }
        if (block == GameConfig.CARROT_CROP) {
            return one(InventoryItems.CARROT, stateData >= 7 ? 2 : 1, stateData >= 7 ? 4 : 1);
        }
        if (block == GameConfig.POTATO_CROP) {
            return one(InventoryItems.POTATO, stateData >= 7 ? 2 : 1, stateData >= 7 ? 4 : 1);
        }
        if (!InventoryItems.isCollectible(block)
            || block == GameConfig.OAK_LEAVES
            || block == GameConfig.PINE_LEAVES
            || block == GameConfig.BIRCH_LEAVES
            || block == GameConfig.PARADISE_PORTAL
            || (GameConfig.isLiquidBlock(block) && block != GameConfig.SEAGRASS && block != GameConfig.KELP)) {
            return EMPTY;
        }
        if (block == GameConfig.COAL_ORE || block == GameConfig.DEEPSLATE_COAL_ORE) {
            return one(InventoryItems.COAL_ITEM, 1, 1);
        }
        if (block == GameConfig.DIAMOND_ORE || block == GameConfig.DEEPSLATE_DIAMOND_ORE) {
            return one(InventoryItems.DIAMOND_ITEM, 1, 1);
        }
        if (block == GameConfig.STONE) {
            return one(GameConfig.COBBLESTONE, 1, 1);
        }
        return one(block, 1, 1);
    }

    static boolean canHarvestBlock(byte block, byte heldItem) {
        int pickaxeTier = pickaxeTier(heldItem);
        if (block == GameConfig.OBSIDIAN) {
            return pickaxeTier >= 4;
        }
        if (block == GameConfig.DIAMOND_ORE || block == GameConfig.DEEPSLATE_DIAMOND_ORE) {
            return pickaxeTier >= 3;
        }
        if (block == GameConfig.IRON_ORE || block == GameConfig.DEEPSLATE_IRON_ORE) {
            return pickaxeTier >= 2;
        }
        if (block == GameConfig.STONE
            || block == GameConfig.COBBLESTONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.COAL_ORE
            || block == GameConfig.DEEPSLATE_COAL_ORE) {
            return pickaxeTier >= 1;
        }
        return true;
    }

    private static DropPlan one(byte itemId, int minCount, int maxCount) {
        return new DropPlan(new DropSpec(itemId, minCount, maxCount, 0.2));
    }

    static final class DropPlan {
        private final DropSpec[] drops;

        private DropPlan(DropSpec... drops) {
            this.drops = drops;
        }

        int size() {
            return drops.length;
        }

        DropSpec get(int index) {
            return drops[index];
        }

        boolean isEmpty() {
            return drops.length == 0;
        }
    }

    static final class DropSpec {
        final byte itemId;
        final int minCount;
        final int maxCount;
        final double yOffset;

        private DropSpec(byte itemId, int minCount, int maxCount, double yOffset) {
            this.itemId = itemId;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.yOffset = yOffset;
        }
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
}
