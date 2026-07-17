import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

public class FinishPlanStabilityTest {
    @Test
    public void oldWorldsDeriveMissingAllowCheatsFromTheirGameMode() {
        assertFalse(WorldAccessRules.allowCheatsFromMetadata(null, 0));
        assertTrue(WorldAccessRules.allowCheatsFromMetadata(null, 1));
        assertTrue(WorldAccessRules.allowCheatsFromMetadata(null, 2));
        assertFalse(WorldAccessRules.allowCheatsFromMetadata("false", 1));
        assertTrue(WorldAccessRules.allowCheatsFromMetadata("true", 0));
    }

    @Test
    public void playerGameModeSurvivesSaveAndLoad() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-player-mode");
        VoxelWorld world = new VoxelWorld(77L);
        try {
            world.configureWorld(directory, 77L, TerrainPreset.DEFAULT);
            world.initializeNoise();
            PlayerState saved = new PlayerState();
            saved.x = 0.5;
            saved.y = 100.0;
            saved.z = 0.5;
            saved.creativeMode = true;
            saved.spectatorMode = false;
            world.savePlayerState(saved);

            PlayerState restored = new PlayerState();
            assertTrue(world.loadPlayerState(restored));
            assertTrue(restored.creativeMode);
            assertFalse(restored.spectatorMode);
        } finally {
            world.cleanup();
        }
    }

    @Test
    public void folderWithoutWorldMetadataIsNotListedAsWorld() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-multiplayer-cache");

        assertFalse(TinyCraft.hasWorldMetadata(directory));
        Files.write(directory.resolve(GameConfig.SAVE_METADATA_FILE), "0".getBytes(StandardCharsets.UTF_8));
        assertTrue(TinyCraft.hasWorldMetadata(directory));
    }

    @Test
    public void windowsReleaseUsesBundledJavaRuntime() throws Exception {
        String runScript = new String(Files.readAllBytes(Paths.get("run-game.bat")), StandardCharsets.UTF_8);
        String developmentScript = new String(Files.readAllBytes(Paths.get("run-opengl.bat")), StandardCharsets.UTF_8);
        String releaseScript = new String(Files.readAllBytes(Paths.get("build-release.bat")), StandardCharsets.UTF_8);

        assertTrue(runScript.contains("runtime\\bin\\java.exe"));
        assertTrue(developmentScript.startsWith("@echo off"));
        assertTrue(developmentScript.contains("jdk-21*"));
        assertTrue(developmentScript.contains("TINYCRAFT_JDK_HOME"));
        assertTrue(releaseScript.contains("--add-modules java.base,java.desktop,jdk.unsupported"));
        assertTrue(releaseScript.contains("%DEST%\\runtime"));
    }

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

    @Test
    public void commandFillChangesRequestedBlocksAndRejectsInvalidHeight() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-command-fill");
        VoxelWorld world = new VoxelWorld(9012L);
        try {
            world.configureWorld(directory, 9012L, TerrainPreset.DEFAULT);
            world.initializeNoise();
            BlockState stone = Blocks.stateFromLegacyId(GameConfig.STONE);

            assertEquals(4, world.fillBlockStateForCommand(0, 70, 0, 1, 70, 1, stone));
            assertEquals(GameConfig.STONE, world.getBlock(0, 70, 0));
            assertEquals(GameConfig.STONE, world.getBlock(1, 70, 1));
            assertEquals(-1, world.setBlockStateForCommand(0, GameConfig.WORLD_MAX_Y + 1, 0, stone));
        } finally {
            world.cleanup();
        }
    }

    private static final class DummyCommandTarget implements ChatSystem.CommandTarget {
        @Override public void teleportPlayer(double x, double y, double z) {}
        @Override public void setWorldTime(double worldTime) {}
        @Override public void setGameMode(String mode) {}
        @Override public void clearInventory() {}
        @Override public boolean giveItem(byte itemId, int amount) { return true; }
        @Override public void killPlayer() {}
        @Override public boolean summonMob(MobKind kind) { return true; }
        @Override public int setBlock(int x, int y, int z, BlockState state) { return 0; }
        @Override public int fillBlocks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) { return 0; }
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
