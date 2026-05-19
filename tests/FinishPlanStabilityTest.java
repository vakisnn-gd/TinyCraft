import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

public class FinishPlanStabilityTest {
    @Test
    public void chatGiveUsageMentionsTinyCraftNamespace() throws Exception {
        ChatSystem chat = new ChatSystem();
        Method executeCommand = ChatSystem.class.getDeclaredMethod("executeCommand", String.class, ChatSystem.CommandTarget.class);
        executeCommand.setAccessible(true);

        executeCommand.invoke(chat, "/give", new DummyCommandTarget());

        List<String> messages = chat.visibleMessages();
        assertFalse(messages.isEmpty());
        String last = messages.get(messages.size() - 1);
        assertTrue(last.contains("tinycraft:name"));
        assertFalse(last.contains("minecraft:name"));
    }

    @Test
    public void giveResolverAcceptsBareAndTinyCraftNames() throws Exception {
        ChatSystem chat = new ChatSystem();
        Method resolveGiveItem = ChatSystem.class.getDeclaredMethod("resolveGiveItem", String.class);
        resolveGiveItem.setAccessible(true);

        assertEquals(Byte.valueOf(GameConfig.DIRT), resolveGiveItem.invoke(chat, "dirt"));
        assertEquals(Byte.valueOf(GameConfig.DIRT), resolveGiveItem.invoke(chat, "tinycraft:dirt"));
        assertEquals(Byte.valueOf(InventoryItems.DIAMOND_ITEM), resolveGiveItem.invoke(chat, "diamond"));
    }

    @Test
    public void newPlayerSpawnStaysNearOriginAndOnSafeSurface() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-spawn");
        VoxelWorld world = new VoxelWorld(1234L);
        world.configureWorld(directory, 1234L, TerrainPreset.DEFAULT);

        PlayerState player = new PlayerState();
        world.placePlayerAtSpawn(player);

        assertTrue(Math.abs(player.x) <= 80.0);
        assertTrue(Math.abs(player.z) <= 80.0);
        assertEquals(world.safeStandingYAt(player.x, player.z), player.y, 0.0001);
    }

    @Test
    public void creativeClickCreatesStackOnlyForCreativePlayers() {
        InventorySlotRef creativeSlot = new InventorySlotRef(InventorySlotGroup.CREATIVE, 0);
        byte expectedItem = InventoryItems.CREATIVE_ITEMS[0];

        PlayerInventory survivalInventory = new PlayerInventory();
        assertFalse(survivalInventory.handleClick(creativeSlot, false, false, false, false, null, null));
        assertTrue(survivalInventory.getCursorStack().isEmpty());

        PlayerInventory creativeInventory = new PlayerInventory();
        assertTrue(creativeInventory.handleClick(creativeSlot, true, false, false, false, null, null));
        assertEquals(expectedItem, creativeInventory.getCursorStack().itemId);
        assertEquals(1, creativeInventory.getCursorStack().count);
    }

    @Test
    public void validNearbyBlockBreakRemovesBlockAndMarksNetworkDirty() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-break");
        VoxelWorld world = new VoxelWorld(5678L);
        world.configureWorld(directory, 5678L, TerrainPreset.DEFAULT);
        int x = 0;
        int z = 0;
        int y = (int) Math.floor(world.safeStandingYAt(x + 0.5, z + 0.5)) - 1;

        world.setBlockState(x, y, z, Blocks.stateFromLegacyId(GameConfig.DIRT));
        assertTrue(world.breakBlock(new RayHit(x, y, z, x, y, z)));

        assertEquals(GameConfig.AIR, world.getBlock(x, y, z));
        assertNotNull(world.drainNetworkDirtyBlocks());
    }

    private static final class DummyCommandTarget implements ChatSystem.CommandTarget {
        @Override public void teleportPlayer(double x, double y, double z) {}
        @Override public void setWorldTime(double worldTime) {}
        @Override public void setGameMode(String mode) {}
        @Override public void clearInventory() {}
        @Override public boolean giveItem(byte itemId, int amount) { return true; }
        @Override public void spawnZombieAtPlayer() {}
        @Override public String currentSeed() { return "0"; }
        @Override public String locateBiome(String biomeName) { return ""; }
        @Override public String locateStructure(String structureName) { return ""; }
        @Override public String placeStructure(String structureName, int rotation) { return ""; }
        @Override public String listStructures() { return ""; }
        @Override public String currentDebugLocation() { return ""; }
        @Override public String terrainDebugAt(int x, int z) { return ""; }
        @Override public String heightTest() { return ""; }
        @Override public String blockInfo() { return ""; }
        @Override public void sendChat(String message) {}
    }
}
