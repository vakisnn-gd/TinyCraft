import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class NaturalFluidSimulationTest {
    @Test
    public void exposedNaturalWaterSpreadsAfterColumnLoad() throws Exception {
        Path worldDirectory = Files.createTempDirectory("tinycraft-natural-water");
        VoxelWorld world = new VoxelWorld(123L);
        try {
            world.configureWorld(worldDirectory, 123L, TerrainPreset.DEFAULT);

            int worldX = 8;
            int worldY = 70;
            int worldZ = 8;
            VoxelWorld.ChunkColumn column = new VoxelWorld.ChunkColumn(0, 0);
            column.naturalTerrain = true;
            column.status = ChunkGenerationStatus.FULL;
            Chunk section = column.section(GameConfig.sectionIndexForY(worldY));
            int localY = GameConfig.localYForWorldY(worldY);
            section.setBlockLocal(worldX, localY - 1, worldZ, GameConfig.STONE);
            section.setBlockLocal(worldX, localY, worldZ, GameConfig.WATER_SOURCE);
            section.setFluidDistanceLocal(worldX, localY, worldZ, GameConfig.NATURAL_FLUID_DISTANCE);

            Method integrateColumn = VoxelWorld.class.getDeclaredMethod("integrateColumn", VoxelWorld.ChunkColumn.class);
            integrateColumn.setAccessible(true);
            integrateColumn.invoke(world, column);

            PlayerState player = new PlayerState();
            player.x = worldX + 0.5;
            player.y = worldY + 1.0;
            player.z = worldZ + 0.5;
            world.updateWorldTicks(player, GameConfig.WORLD_TICK_INTERVAL);

            assertEquals(GameConfig.WATER_SOURCE, world.getBlock(worldX, worldY, worldZ));
            assertEquals(GameConfig.WATER_FLOWING, world.getBlock(worldX + 1, worldY, worldZ));
            assertEquals(1, world.getFluidDistance(worldX + 1, worldY, worldZ));
        } finally {
            world.cleanup();
        }
    }
}
