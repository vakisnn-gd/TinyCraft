import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChunkCompactStorageTest {
    @Test
    public void normalSectionUsesOneBytePaletteIndices() {
        Chunk chunk = new Chunk(0, 0, 0);

        chunk.setBlockStateAtIndex(0, Blocks.stateFromLegacyId(GameConfig.STONE));
        chunk.setBlockStateAtIndex(1, Blocks.stateFromLegacyId(GameConfig.DIRT));

        assertEquals(Chunk.VOLUME, chunk.blockStateIndexStorageBytesForDebug());
        assertEquals(GameConfig.STONE, chunk.getBlockAtIndex(0));
        assertEquals(GameConfig.DIRT, chunk.getBlockAtIndex(1));
    }

    @Test
    public void sectionWidensWithoutLosingPaletteValues() {
        Chunk chunk = new Chunk(0, 0, 0);

        for (int index = 0; index < 300; index++) {
            chunk.setBlockStateAtIndex(index, Blocks.withData(GameConfig.STONE, index + 1));
        }

        assertEquals(Chunk.VOLUME * Short.BYTES, chunk.blockStateIndexStorageBytesForDebug());
        assertEquals(1, chunk.getBlockStateAtIndex(0).data);
        assertEquals(256, chunk.getBlockStateAtIndex(255).data);
        assertEquals(300, chunk.getBlockStateAtIndex(299).data);

        ChunkSectionSnapshot snapshot = chunk.snapshot();
        assertEquals(300, snapshot.getBlockStateLocal(11, 1, 2).data);
    }

    @Test
    public void fluidDistancesAllocateOnlyForSectionsWithFluid() {
        Chunk chunk = new Chunk(0, 0, 0);

        assertEquals(0, chunk.fluidDistanceStorageBytesForDebug());
        assertEquals(-1, chunk.getFluidDistanceAtIndex(0));

        chunk.setBlockStateAtIndex(0, Blocks.stateFromLegacyId(GameConfig.WATER_SOURCE));
        chunk.setFluidDistanceLocal(0, 0, 0, GameConfig.NATURAL_FLUID_DISTANCE);

        assertEquals(Chunk.VOLUME, chunk.fluidDistanceStorageBytesForDebug());
        assertEquals(GameConfig.NATURAL_FLUID_DISTANCE, chunk.getFluidDistanceAtIndex(0));
        assertEquals(GameConfig.NATURAL_FLUID_DISTANCE, chunk.snapshot().getFluidDistanceLocal(0, 0, 0));

        chunk.setBlockStateAtIndex(0, Blocks.stateFromLegacyId(GameConfig.AIR));

        assertEquals(0, chunk.fluidDistanceStorageBytesForDebug());
        assertEquals(-1, chunk.getFluidDistanceAtIndex(0));
        assertEquals(-1, chunk.snapshot().getFluidDistanceLocal(0, 0, 0));
    }
}
