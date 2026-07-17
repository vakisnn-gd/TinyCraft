import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlockDropRulesTest {
    @Test
    public void wheatDropsRespectGrowthStage() {
        BlockDropRules.DropPlan immature = BlockDropRules.forBrokenBlock(
            GameConfig.WHEAT_CROP, 6, GameConfig.AIR, false);
        BlockDropRules.DropPlan mature = BlockDropRules.forBrokenBlock(
            GameConfig.WHEAT_CROP, 7, GameConfig.AIR, false);

        assertDrop(immature, 0, InventoryItems.WHEAT_SEEDS, 1, 1, 0.2);
        assertEquals(1, immature.size());
        assertDrop(mature, 0, GameConfig.WHEAT_CROP, 1, 1, 0.2);
        assertDrop(mature, 1, InventoryItems.WHEAT_SEEDS, 1, 3, 0.3);
        assertEquals(2, mature.size());
    }

    @Test
    public void carrotAndPotatoDropsUseItemsAndGrowthStage() {
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.CARROT_CROP, 6, GameConfig.AIR, false),
            0, InventoryItems.CARROT, 1, 1, 0.2);
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.CARROT_CROP, 7, GameConfig.AIR, false),
            0, InventoryItems.CARROT, 2, 4, 0.2);
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.POTATO_CROP, 6, GameConfig.AIR, false),
            0, InventoryItems.POTATO, 1, 1, 0.2);
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.POTATO_CROP, 7, GameConfig.AIR, false),
            0, InventoryItems.POTATO, 2, 4, 0.2);
    }

    @Test
    public void aquaticPlantsDropThemselvesButRealLiquidsDoNot() {
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.SEAGRASS, 0, GameConfig.AIR, false),
            0, GameConfig.SEAGRASS, 1, 1, 0.2);
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.KELP, 0, GameConfig.AIR, false),
            0, GameConfig.KELP, 1, 1, 0.2);
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.WATER, 0, GameConfig.AIR, false).isEmpty());
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.LAVA, 0, GameConfig.AIR, false).isEmpty());
    }

    @Test
    public void creativeNeverCreatesABlockItemDrop() {
        byte[] blocks = {
            GameConfig.DIRT, GameConfig.STONE, GameConfig.DIAMOND_ORE,
            GameConfig.WHEAT_CROP, GameConfig.SEAGRASS
        };
        for (byte block : blocks) {
            assertTrue(BlockDropRules.forBrokenBlock(block, 7, InventoryItems.NETHERITE_PICKAXE, true).isEmpty());
        }
    }

    @Test
    public void standardMappingsAndHarvestTiersStayAuthoritative() {
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.STONE, 0, InventoryItems.WOODEN_PICKAXE, false),
            0, GameConfig.COBBLESTONE, 1, 1, 0.2);
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.COAL_ORE, 0, InventoryItems.WOODEN_PICKAXE, false),
            0, InventoryItems.COAL_ITEM, 1, 1, 0.2);
        assertDrop(BlockDropRules.forBrokenBlock(GameConfig.DIAMOND_ORE, 0, InventoryItems.IRON_PICKAXE, false),
            0, InventoryItems.DIAMOND_ITEM, 1, 1, 0.2);

        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.STONE, 0, GameConfig.AIR, false).isEmpty());
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.IRON_ORE, 0, InventoryItems.WOODEN_PICKAXE, false).isEmpty());
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.DIAMOND_ORE, 0, InventoryItems.STONE_PICKAXE, false).isEmpty());
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.OBSIDIAN, 0, InventoryItems.IRON_PICKAXE, false).isEmpty());
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.OAK_LEAVES, 0, GameConfig.AIR, false).isEmpty());
        assertTrue(BlockDropRules.forBrokenBlock(GameConfig.PARADISE_PORTAL, 0, GameConfig.AIR, false).isEmpty());
    }

    private void assertDrop(BlockDropRules.DropPlan plan, int index, byte itemId,
                            int minCount, int maxCount, double yOffset) {
        BlockDropRules.DropSpec drop = plan.get(index);
        assertEquals(itemId, drop.itemId);
        assertEquals(minCount, drop.minCount);
        assertEquals(maxCount, drop.maxCount);
        assertEquals(yOffset, drop.yOffset, 0.0001);
    }
}
