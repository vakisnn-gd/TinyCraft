import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OceanCoastGenerationTest {
    private static final long SCREENSHOT_SEED = Long.parseUnsignedLong("be8d8974b9cdb026", 16);
    private static final long LOW_FOREST_COAST_SEED = Long.parseUnsignedLong("a8be75bd862989bc", 16);

    @Test
    public void lowBeachFromScreenshotIsFilledToSeaLevel() {
        int worldX = 45;
        int worldZ = -7;
        WorldGenerator generator = new WorldGenerator(SCREENSHOT_SEED, TerrainPreset.DEFAULT);

        assertEquals("Beach", generator.debugBiomeName(worldX, worldZ));
        assertTrue(generator.debugSurfaceHeight(worldX, worldZ) < GameConfig.SEA_LEVEL);

        GeneratedChunkColumn column = generator.generateChunk(
            Math.floorDiv(worldX, GameConfig.CHUNK_SIZE),
            Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE)
        );

        assertEquals(GameConfig.WATER_SOURCE, column.getBlock(worldX, GameConfig.SEA_LEVEL, worldZ));
        assertEquals(GameConfig.AIR, column.getBlock(worldX, GameConfig.SEA_LEVEL + 1, worldZ));
        Chunk waterSection = column.sections()[GameConfig.sectionIndexForY(GameConfig.SEA_LEVEL)];
        assertEquals(
            GameConfig.NATURAL_FLUID_DISTANCE,
            waterSection.getFluidDistanceLocal(
                Math.floorMod(worldX, GameConfig.CHUNK_SIZE),
                GameConfig.localYForWorldY(GameConfig.SEA_LEVEL),
                Math.floorMod(worldZ, GameConfig.CHUNK_SIZE)
            )
        );
    }

    @Test
    public void lowForestCoastFromScreenshotBecomesOceanAndFillsToSeaLevel() {
        int worldX = 215;
        int worldZ = -143;
        WorldGenerator generator = new WorldGenerator(LOW_FOREST_COAST_SEED, TerrainPreset.DEFAULT);

        assertEquals("Ocean", generator.debugBiomeName(worldX, worldZ));
        assertTrue(generator.debugSurfaceHeight(worldX, worldZ) < GameConfig.SEA_LEVEL - 2);

        GeneratedChunkColumn column = generator.generateChunk(
            Math.floorDiv(worldX, GameConfig.CHUNK_SIZE),
            Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE)
        );

        assertEquals(GameConfig.WATER_SOURCE, column.getBlock(worldX, GameConfig.SEA_LEVEL, worldZ));
        assertEquals(GameConfig.AIR, column.getBlock(worldX, GameConfig.SEA_LEVEL + 1, worldZ));
    }
}
