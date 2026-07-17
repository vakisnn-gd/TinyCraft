import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ParadisePortalTest {
    @Test
    public void paradisePortalBlockIsRegisteredAsGlowingTransparentBlock() {
        BlockType portal = BlockRegistry.typeByName("tiny:paradise_portal");

        assertNotNull(portal);
        assertEquals(GameConfig.PARADISE_PORTAL & 0xFF, portal.numericId);
        assertFalse(portal.isSolid());
        assertFalse(portal.isOpaque());
        assertTrue(portal.lightEmission > 0);
    }

    @Test
    public void torchActivatesGlassFramePortal() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-portal-activate");
        buildGlassFrame(world, 0, 70, 0, 0);

        assertTrue(world.activateParadisePortalAtFrame(0, 70, 0));

        assertPortalInterior(world, 0, 70, 0, 0, GameConfig.PARADISE_PORTAL);
    }

    @Test
    public void invalidFrameClearsPortalInterior() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-portal-clear");
        buildGlassFrame(world, 0, 70, 0, 0);
        assertTrue(world.activateParadisePortalAtFrame(0, 70, 0));

        assertTrue(world.breakBlock(new RayHit(0, 70, 0, 0, 70, 0)));

        assertPortalInterior(world, 0, 70, 0, 0, GameConfig.AIR);
    }

    @Test
    public void paradiseSpawnIsInParadiseAreaAndSafe() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-paradise-spawn");
        PlayerState player = new PlayerState();

        world.placePlayerAtParadiseSpawn(player);

        assertTrue(world.isParadiseArea(player.x, player.z));
        assertEquals(world.safeStandingYAt(player.x, player.z), player.y, 0.0001);
        assertEquals(GameConfig.GRASS, world.getBlock((int) Math.floor(player.x), (int) Math.floor(player.y) - 1, (int) Math.floor(player.z)));
    }

    @Test
    public void paradiseColumnsUseSeparateDimensionStorage() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-paradise-dimension");
        int chunkX = Math.floorDiv(GameConfig.PARADISE_ORIGIN_X, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(GameConfig.PARADISE_ORIGIN_Z, GameConfig.CHUNK_SIZE);

        world.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        assertTrue(world.isParadiseColumn(chunkX, chunkZ));
        world.placePlayerAtParadiseSpawn(new PlayerState());
        world.setBlockState(GameConfig.PARADISE_ORIGIN_X, GameConfig.PARADISE_SURFACE_Y + 8, GameConfig.PARADISE_ORIGIN_Z,
            Blocks.stateFromLegacyId(GameConfig.GLASS));
        world.saveAllLoadedColumns();

        Path paradiseRegion = world.paradiseDimensionDirectory()
            .resolve(GameConfig.SAVE_REGION_DIRECTORY)
            .resolve(RegionStorage.regionFileNameForChunk(chunkX, chunkZ));
        Path overworldRegion = world.overworldDimensionDirectory()
            .resolve(GameConfig.SAVE_REGION_DIRECTORY)
            .resolve(RegionStorage.regionFileNameForChunk(chunkX, chunkZ));
        assertEquals(
            world.overworldDimensionDirectory().resolveSibling(
                world.overworldDimensionDirectory().getFileName().toString() + "-paradize"
            ),
            world.paradiseDimensionDirectory()
        );
        assertTrue(Files.isRegularFile(paradiseRegion));
        assertFalse(Files.exists(overworldRegion));
    }

    @Test
    public void legacyParadiseStorageIsReadAndMigratedToSeparateWorldDirectory() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-paradise-legacy");
        Path legacyDirectory = directory
            .resolve(GameConfig.SAVE_DIMENSIONS_DIRECTORY)
            .resolve(GameConfig.PARADISE_DIMENSION_DIRECTORY);
        RegionStorage legacyStorage = new RegionStorage(legacyDirectory);
        VoxelWorld.ChunkColumn legacyColumn = new VoxelWorld.ChunkColumn(0, 0);
        int worldY = GameConfig.PARADISE_SURFACE_Y + 6;
        Chunk legacySection = legacyColumn.section(GameConfig.sectionIndexForY(worldY));
        legacySection.setBlockLocal(0, GameConfig.localYForWorldY(worldY), 0, GameConfig.GLASS);
        legacyStorage.saveColumn(legacyColumn);

        VoxelWorld world = new VoxelWorld(424242L);
        world.configureWorld(directory, 424242L, TerrainPreset.DEFAULT);
        world.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        world.setBlockState(1, worldY, 1, Blocks.stateFromLegacyId(GameConfig.DIRT));

        assertEquals(GameConfig.GLASS, world.getBlock(0, worldY, 0));
        world.saveAllLoadedColumns();
        Path migratedRegion = world.paradiseDimensionDirectory()
            .resolve(GameConfig.SAVE_REGION_DIRECTORY)
            .resolve(RegionStorage.regionFileNameForChunk(0, 0));
        assertTrue(Files.isRegularFile(migratedRegion));
    }

    @Test
    public void activeDimensionSwitchKeepsIndependentLoadedColumns() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-paradise-runtime");
        world.setBlockState(0, GameConfig.SURFACE_Y + 3, 0, Blocks.stateFromLegacyId(GameConfig.GLASS));

        world.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        assertFalse(world.isChunkLoaded(0, 0));
        world.setBlockState(0, GameConfig.PARADISE_SURFACE_Y + 3, 0, Blocks.stateFromLegacyId(GameConfig.GLASS));
        assertTrue(world.isChunkLoaded(0, 0));

        world.setActiveDimension(GameConfig.DIMENSION_OVERWORLD);
        assertTrue(world.isChunkLoaded(0, 0));
        assertEquals(GameConfig.GLASS, world.getBlock(0, GameConfig.SURFACE_Y + 3, 0));
    }

    @Test
    public void dimensionSimulationKeepsIndependentTickTimers() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-paradise-ticks");
        PlayerState overworldPlayer = new PlayerState();
        PlayerState paradisePlayer = new PlayerState();
        paradisePlayer.dimensionId = GameConfig.DIMENSION_PARADISE;

        world.updateWorldTicks(overworldPlayer, GameConfig.WORLD_TICK_INTERVAL);
        assertEquals(1L, world.activeWorldTickCounter());

        world.updateWorldTicks(paradisePlayer, GameConfig.WORLD_TICK_INTERVAL);
        assertEquals(1L, world.activeWorldTickCounter());

        world.setActiveDimensionForSimulation(overworldPlayer);
        assertEquals(1L, world.activeWorldTickCounter());
    }

    @Test
    public void dimensionSwitchPreservesPendingNetworkBlockUpdates() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-paradise-network-dirty");
        world.clearNetworkDirtyBlocks();
        world.setBlockState(2, GameConfig.SURFACE_Y + 4, 2, Blocks.stateFromLegacyId(GameConfig.GLASS));

        world.setActiveDimensionForSimulation(GameConfig.DIMENSION_PARADISE);
        world.clearNetworkDirtyBlocks();
        world.setBlockState(3, GameConfig.PARADISE_SURFACE_Y + 4, 3, Blocks.stateFromLegacyId(GameConfig.DIRT));
        assertEquals(1, world.drainNetworkDirtyBlocks().size());

        world.setActiveDimensionForSimulation(GameConfig.DIMENSION_OVERWORLD);
        assertEquals(1, world.drainNetworkDirtyBlocks().size());
    }

    @Test
    public void mobsAndContainersPersistSeparatelyForBothWorlds() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-paradise-data");
        VoxelWorld world = new VoxelWorld(424242L);
        world.configureWorld(directory, 424242L, TerrainPreset.DEFAULT);
        world.chestContainerAt(7, 70, 7).getStack(0).set(GameConfig.DIRT, 5);
        world.spawnMobAt(MobKind.COW, 7.5, 70.0, 7.5);

        world.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        world.chestContainerAt(7, 70, 7).getStack(0).set(GameConfig.GLASS, 3);
        world.spawnMobAt(MobKind.PIG, 7.5, 70.0, 7.5);
        world.saveAllLoadedColumns();

        assertTrue(Files.isRegularFile(directory.resolve("containers.dat")));
        assertTrue(Files.isRegularFile(directory.resolve("mobs.dat")));
        assertTrue(Files.isRegularFile(world.paradiseDimensionDirectory().resolve("containers.dat")));
        assertTrue(Files.isRegularFile(world.paradiseDimensionDirectory().resolve("mobs.dat")));

        VoxelWorld reloaded = new VoxelWorld(424242L);
        reloaded.configureWorld(directory, 424242L, TerrainPreset.DEFAULT);
        assertEquals(GameConfig.DIRT, reloaded.chestContainerAt(7, 70, 7).getStack(0).itemId);
        assertEquals(MobKind.COW, reloaded.getMobs().get(0).kind);

        reloaded.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        assertEquals(GameConfig.GLASS, reloaded.chestContainerAt(7, 70, 7).getStack(0).itemId);
        assertEquals(MobKind.PIG, reloaded.getMobs().get(0).kind);
    }

    @Test
    public void legacyParadiseEntityDataMigratesToSeparateWorldDirectory() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-paradise-legacy-data");
        VoxelWorld writer = new VoxelWorld(424242L);
        writer.configureWorld(directory, 424242L, TerrainPreset.DEFAULT);
        writer.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        writer.chestContainerAt(9, 70, 9).getStack(0).set(GameConfig.GLASS, 2);
        writer.spawnMobAt(MobKind.SHEEP, 9.5, 70.0, 9.5);
        writer.saveAllLoadedColumns();

        Path legacyDirectory = directory
            .resolve(GameConfig.SAVE_DIMENSIONS_DIRECTORY)
            .resolve(GameConfig.PARADISE_DIMENSION_DIRECTORY);
        Files.createDirectories(legacyDirectory);
        Files.move(writer.paradiseDimensionDirectory().resolve("containers.dat"), legacyDirectory.resolve("containers.dat"));
        Files.move(writer.paradiseDimensionDirectory().resolve("mobs.dat"), legacyDirectory.resolve("mobs.dat"));

        VoxelWorld reader = new VoxelWorld(424242L);
        reader.configureWorld(directory, 424242L, TerrainPreset.DEFAULT);
        reader.setActiveDimension(GameConfig.DIMENSION_PARADISE);
        assertEquals(GameConfig.GLASS, reader.chestContainerAt(9, 70, 9).getStack(0).itemId);
        assertEquals(MobKind.SHEEP, reader.getMobs().get(0).kind);

        reader.saveAllLoadedColumns();
        assertTrue(Files.isRegularFile(reader.paradiseDimensionDirectory().resolve("containers.dat")));
        assertTrue(Files.isRegularFile(reader.paradiseDimensionDirectory().resolve("mobs.dat")));
    }

    @Test
    public void portalTeleportsToParadiseAndBackToSavedReturnPoint() throws Exception {
        VoxelWorld world = newTestWorld("tinycraft-paradise-roundtrip");
        buildGlassFrame(world, 0, 70, 0, 0);
        assertTrue(world.activateParadisePortalAtFrame(0, 70, 0));
        PlayerState player = new PlayerState();
        player.setPosition(1.5, 71.1, 0.5);
        double returnX = player.x;
        double returnY = player.y;
        double returnZ = player.z;

        assertTrue(world.updateParadisePortalTravel(player, GameConfig.PARADISE_PORTAL_SECONDS + 0.1));
        assertTrue(world.isParadiseArea(player.x, player.z));
        assertTrue(player.hasParadiseReturn);
        assertEquals(returnX, player.paradiseReturnX, 0.0001);

        player.paradisePortalCooldown = 0.0;
        player.setPosition(GameConfig.PARADISE_ORIGIN_X + 0.5, GameConfig.PARADISE_SURFACE_Y + 2.1, GameConfig.PARADISE_ORIGIN_Z + 2.5);

        assertTrue(world.updateParadisePortalTravel(player, GameConfig.PARADISE_PORTAL_SECONDS + 0.1));
        assertFalse(world.isParadiseArea(player.x, player.z));
        assertEquals(returnX, player.x, 0.0001);
        assertEquals(returnY, player.y, 0.0001);
        assertEquals(returnZ, player.z, 0.0001);
    }

    private VoxelWorld newTestWorld(String prefix) throws Exception {
        Path directory = Files.createTempDirectory(prefix);
        VoxelWorld world = new VoxelWorld(424242L);
        world.configureWorld(directory, 424242L, TerrainPreset.DEFAULT);
        world.initializeNoise();
        return world;
    }

    private void buildGlassFrame(VoxelWorld world, int originX, int originY, int originZ, int axis) {
        for (int width = 0; width < 4; width++) {
            setFrameBlock(world, originX, originY, originZ, axis, width, 0);
            setFrameBlock(world, originX, originY, originZ, axis, width, 4);
        }
        for (int height = 1; height <= 3; height++) {
            setFrameBlock(world, originX, originY, originZ, axis, 0, height);
            setFrameBlock(world, originX, originY, originZ, axis, 3, height);
        }
    }

    private void setFrameBlock(VoxelWorld world, int originX, int originY, int originZ, int axis, int width, int height) {
        int x = axis == 0 ? originX + width : originX;
        int z = axis == 0 ? originZ : originZ + width;
        world.setBlockState(x, originY + height, z, Blocks.stateFromLegacyId(GameConfig.GLASS));
    }

    private void assertPortalInterior(VoxelWorld world, int originX, int originY, int originZ, int axis, byte expectedBlock) {
        for (int width = 1; width <= 2; width++) {
            for (int height = 1; height <= 3; height++) {
                int x = axis == 0 ? originX + width : originX;
                int z = axis == 0 ? originZ : originZ + width;
                assertEquals(expectedBlock, world.getBlock(x, originY + height, z));
            }
        }
    }
}
