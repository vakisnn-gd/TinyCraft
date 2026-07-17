final class ChunkSectionInvalidation {
    interface Target {
        void mark(int chunkX, int chunkY, int chunkZ);
    }

    private ChunkSectionInvalidation() {
    }

    static void aroundBlock(int blockX, int blockY, int blockZ, Target target) {
        if (target == null || !GameConfig.isWorldYInside(blockY)) {
            return;
        }
        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(blockY);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_SIZE);
        int localX = Math.floorMod(blockX, GameConfig.CHUNK_SIZE);
        int localY = GameConfig.localYForWorldY(blockY);
        int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_SIZE);
        int minOffsetX = localX == 0 ? -1 : 0;
        int maxOffsetX = localX == GameConfig.CHUNK_SIZE - 1 ? 1 : 0;
        int minOffsetY = localY == 0 ? -1 : 0;
        int maxOffsetY = localY == GameConfig.CHUNK_SIZE - 1 ? 1 : 0;
        int minOffsetZ = localZ == 0 ? -1 : 0;
        int maxOffsetZ = localZ == GameConfig.CHUNK_SIZE - 1 ? 1 : 0;
        for (int offsetX = minOffsetX; offsetX <= maxOffsetX; offsetX++) {
            for (int offsetY = minOffsetY; offsetY <= maxOffsetY; offsetY++) {
                int affectedChunkY = chunkY + offsetY;
                if (affectedChunkY < 0 || affectedChunkY >= GameConfig.WORLD_CHUNKS_Y) {
                    continue;
                }
                for (int offsetZ = minOffsetZ; offsetZ <= maxOffsetZ; offsetZ++) {
                    target.mark(chunkX + offsetX, affectedChunkY, chunkZ + offsetZ);
                }
            }
        }
    }
}
