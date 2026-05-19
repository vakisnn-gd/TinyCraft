import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

final class WorldGenerator {
    static final int CHUNK_HEIGHT = GameConfig.WORLD_HEIGHT;
    private static final int COLUMN_AREA = GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE;
    private static final Biome[] BIOMES = Biome.values();
    private static final double CONTINENT_SCALE = 0.0028;
    private static final double CONTINENTALNESS_SCALE = 0.00135;
    private static final double EROSION_SCALE = 0.0042;
    private static final double RIDGE_SCALE = 0.0105;
    private static final double DETAIL_SCALE = 0.036;
    private static final double RIVER_FIELD_SCALE = 0.0027;
    private static final double TEMPERATURE_SCALE = 0.0024;
    private static final double HUMIDITY_SCALE = 0.0026;
    private static final double CAVE_TUNNEL_THRESHOLD = 0.480;
    private static final double CAVE_ROOM_THRESHOLD = 0.755;
    private static final double CAVE_FREQUENCY = 0.022;
    private static final double CAVE_ROOM_FREQUENCY = 0.0062;
    private static final int CAVE_MIN_DEPTH_BELOW_SURFACE = 6;
    private static final double CANYON_SCALE = 0.0068;
    private static final double OCEAN_COASTLINE = -0.05;
    private static final double COASTLINE_WIDTH = 0.085;
    private static final int BEACH_MAX_HEIGHT_ABOVE_SEA = 1;
    private static final int OCEAN_SAND_DEPTH = 2;
    private static final double RIVER_MIN_WIDTH_BLOCKS = 2.6;
    private static final double RIVER_MAX_WIDTH_BLOCKS = 4.2;
    private static final double RIVER_MIN_BANK_BLOCKS = 4.8;
    private static final double RIVER_MAX_BANK_BLOCKS = 7.2;
    private static final int RIVER_WATER_LEVEL = GameConfig.SEA_LEVEL;
    private static final double LAKE_WIDTH = 0.066;
    private static final double LAKE_BANK_WIDTH = 0.112;
    private static final int VILLAGE_CELL_SIZE_CHUNKS = 7;
    private static final double VILLAGE_CHANCE = 0.42;
    private static final int VILLAGE_FOUNDATION_DEPTH = 14;
    private static final int VILLAGE_ROAD_FOUNDATION_DEPTH = 8;
    private static final int VILLAGE_STRUCTURE_PROTECTION_DEPTH = 36;
    private static final int WET_RIVER_CAVE_PROTECTION_DEPTH = 28;
    private static final int VILLAGE_ROAD_HALF_WIDTH = 1;
    private static final byte GENMASK_STRUCTURE = 1;
    private static final byte GENMASK_ROAD = 1 << 1;
    private static final byte GENMASK_WET_RIVER = 1 << 2;
    private static final int MINESHAFT_CELL_SIZE_CHUNKS = 7;
    private static final double MINESHAFT_CHANCE = 0.24;
    private static final int CAVE_SYSTEM_SIZE = 64;
    private static final byte TERRAIN_OCEAN = 1;
    private static final byte TERRAIN_BEACH = 1 << 2;
    private static final byte TERRAIN_LAKE = 1 << 3;
    private static final ThreadLocal<GenerationScratch> SCRATCH = ThreadLocal.withInitial(GenerationScratch::new);

    private final long seed;
    private final TerrainPreset terrainPreset;
    private final int dimensionId;
    private final TerrainProfile terrain;
    private final int[] permutation = new int[512];
    private final DensitySampler densitySampler = new DensitySampler();
    private final ConcurrentHashMap<Long, VillagePlan> villagePlanCache = new ConcurrentHashMap<>();

    WorldGenerator(long seed) {
        this(seed, TerrainPreset.LEGACY, GameConfig.DIMENSION_OVERWORLD);
    }

    WorldGenerator(long seed, TerrainPreset terrainPreset) {
        this(seed, terrainPreset, GameConfig.DIMENSION_OVERWORLD);
    }

    WorldGenerator(long seed, TerrainPreset terrainPreset, int dimensionId) {
        this.seed = seed;
        this.terrainPreset = terrainPreset == null ? TerrainPreset.LEGACY : terrainPreset;
        this.dimensionId = dimensionId;
        this.terrain = TerrainProfile.forPreset(this.terrainPreset);
        initializePermutation();
    }

    GeneratedChunkColumn generateChunk(int chunkX, int chunkZ) {
        if (isParadiseChunk(chunkX, chunkZ)) {
            return generateParadiseChunk(chunkX, chunkZ);
        }
        GeneratedChunkColumn column = new GeneratedChunkColumn(chunkX, chunkZ);
        int startX = chunkX * GameConfig.CHUNK_SIZE;
        int startZ = chunkZ * GameConfig.CHUNK_SIZE;
        GenerationScratch scratch = SCRATCH.get();
        boolean profile = shouldProfileGeneration();
        long stageStart = profile ? System.nanoTime() : 0L;
        generateBiomes(column, startX, startZ, scratch);
        stageStart = logGenerationStage("BIOMES", chunkX, chunkZ, stageStart, profile);
        generateNoiseTerrain(column, startX, startZ, scratch);
        stageStart = logGenerationStage("NOISE", chunkX, chunkZ, stageStart, profile);
        applySurface(column, startX, startZ, scratch);
        stageStart = logGenerationStage("SURFACE", chunkX, chunkZ, stageStart, profile);
        carveTerrain(column, startX, startZ, scratch);
        stageStart = logGenerationStage("CARVERS", chunkX, chunkZ, stageStart, profile);
        fillFluids(column, startX, startZ, scratch);
        stageStart = logGenerationStage("FLUIDS", chunkX, chunkZ, stageStart, profile);
        placeFeatures(column, startX, startZ, scratch);
        logGenerationStage("FEATURES", chunkX, chunkZ, stageStart, profile);
        return column;
    }

    private boolean shouldProfileGeneration() {
        return GameConfig.ENABLE_FRAME_PROFILING || GameConfig.ENABLE_DEBUG_LOGS;
    }

    private long logGenerationStage(String stage, int chunkX, int chunkZ, long stageStart, boolean profile) {
        if (!profile) {
            return 0L;
        }
        long now = System.nanoTime();
        return now;
    }

    GeneratedChunkColumn generateTerrainColumn(int chunkX, int chunkZ) {
        GeneratedChunkColumn column = new GeneratedChunkColumn(chunkX, chunkZ);
        int startX = chunkX * GameConfig.CHUNK_SIZE;
        int startZ = chunkZ * GameConfig.CHUNK_SIZE;
        GenerationScratch scratch = SCRATCH.get();
        generateBiomes(column, startX, startZ, scratch);
        generateNoiseTerrain(column, startX, startZ, scratch);
        applySurface(column, startX, startZ, scratch);
        return column;
    }

    private void generateBiomes(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                scratch.biomeOrdinals[index] = (byte) sampleBiome(worldX, worldZ).ordinal();
            }
        }
        column.setStatus(ChunkGenerationStatus.BIOMES);
    }

    private void generateNoiseTerrain(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        cacheTerrainMetadata(startX, startZ, scratch, false);
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                TerrainSample sample = scratch.samples[index];
                Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                RiverSample river = scratch.riverSamples[index];
                boolean riverWet = river != null && river.wet && river.mask > 0.18;
                int bedrockTop = bedrockTopY(worldX, worldZ);
                int topY = Math.min(GameConfig.WORLD_MAX_Y, sample.approximateSurfaceHeight + 36);
                for (int worldY = GameConfig.WORLD_MIN_Y; worldY <= bedrockTop; worldY++) {
                    column.setBlock(worldX, worldY, worldZ, GameConfig.BEDROCK);
                }
                int solidTopY = Math.min(topY, sample.approximateSurfaceHeight - caveRoofDepth(biome, sample.terrainFlags, riverWet));
                for (int worldY = bedrockTop + 1; worldY <= solidTopY; worldY++) {
                    column.setBlock(worldX, worldY, worldZ, baseStoneForY(worldY));
                }
                int noiseStartY = Math.max(bedrockTop + 1, solidTopY + 1);
                for (int worldY = noiseStartY; worldY <= topY; worldY++) {
                    if (worldY <= bedrockTop) {
                        column.setBlock(worldX, worldY, worldZ, GameConfig.BEDROCK);
                        continue;
                    }
                    boolean protectedCarve = shouldProtectCarve(scratch, startX, startZ, worldX, worldY, worldZ);
                    double density = densitySampler.sampleTerrainDensity(worldX, worldY, worldZ, sample, protectedCarve);
                    if (density > 0.0) {
                        column.setBlock(worldX, worldY, worldZ, baseStoneForY(worldY));
                    }
                }
            }
        }
        column.setStatus(ChunkGenerationStatus.NOISE);
    }

    private void applySurface(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                scratch.surfaceHeights[index] = applySurfaceColumn(column, worldX, worldZ, scratch.dirtDepths[index], biome, scratch.terrainFlags[index]);
            }
        }
        column.setStatus(ChunkGenerationStatus.SURFACE);
    }

    void postProcessChunk(GeneratedChunkColumn column) {
        if (column == null) {
            return;
        }
        int startX = column.chunkX * GameConfig.CHUNK_SIZE;
        int startZ = column.chunkZ * GameConfig.CHUNK_SIZE;
        GenerationScratch scratch = SCRATCH.get();
        cacheTerrainMetadata(startX, startZ, scratch, true);
        carveTerrain(column, startX, startZ, scratch);
        fillFluids(column, startX, startZ, scratch);
        placeFeatures(column, startX, startZ, scratch);
    }

    private void carveTerrain(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        carveCaves(column, startX, startZ, scratch);
        carveCaveNetworks(column, startX, startZ, scratch);
        carveSurfaceCaveEntrances(column, startX, startZ, scratch);
        carveSurfaceFissures(column, startX, startZ, scratch);
        column.setStatus(ChunkGenerationStatus.CARVERS);
    }

    private void fillFluids(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        populateKarstLakes(column, startX, startZ, scratch);
        populateCaveFluids(column, startX, startZ, scratch);
        populateCavePatches(column, startX, startZ, scratch);
        stabilizeGeneratedSand(column, startX, startZ);
        floodBelowSeaLevel(column, startX, startZ, scratch);
        populateUnderwaterSedimentVeins(column, startX, startZ, scratch);
        populateDeepLavaPockets(column, startX, startZ);
        column.setStatus(ChunkGenerationStatus.FLUIDS);
    }

    private void placeFeatures(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        populateAquaticPlants(column, startX, startZ, scratch);
        populateSmallSurfacePools(column, startX, startZ, scratch);
        populateOreClusters(column, startX, startZ, scratch);
        populateCaveWallOres(column, startX, startZ, scratch);
        populateDesertOases(column, startX, startZ);
        populateSurface(column, startX, startZ, scratch);
        populateStructures(column, startX, startZ, scratch);
        enforceShoreSand(column, startX, startZ, scratch);
        stabilizeGeneratedSand(column, startX, startZ);
        stabilizeRiverBanks(column, startX, startZ, scratch);
        smoothSnowCoverage(column, startX, startZ, scratch);
        removeSubmergedPlantsAndSnow(column, startX, startZ);
        recalculateSurfaceHeights(column, startX, startZ);
        column.setStatus(ChunkGenerationStatus.FULL);
    }

    private void populateStructures(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        populateVillagePieces(column, startX, startZ, scratch);
        populateMineshaftPieces(column, startX, startZ);
    }

    private void populateVillagePieces(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        int minCellX = Math.floorDiv(column.chunkX - 4, VILLAGE_CELL_SIZE_CHUNKS);
        int maxCellX = Math.floorDiv(column.chunkX + 4, VILLAGE_CELL_SIZE_CHUNKS);
        int minCellZ = Math.floorDiv(column.chunkZ - 4, VILLAGE_CELL_SIZE_CHUNKS);
        int maxCellZ = Math.floorDiv(column.chunkZ + 4, VILLAGE_CELL_SIZE_CHUNKS);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                VillagePlan plan = sampleVillagePlan(cellX, cellZ);
                if (plan == null
                    || Math.abs(column.chunkX - plan.centerChunkX) > 4
                    || Math.abs(column.chunkZ - plan.centerChunkZ) > 4) {
                    continue;
                }
                placeVillagePlan(column, plan);
            }
        }
    }

    private VillagePlan sampleVillagePlan(int cellX, int cellZ) {
        long cacheKey = packVillageCell(cellX, cellZ);
        VillagePlan cached = villagePlanCache.get(cacheKey);
        if (cached != null) {
            return cached == VillagePlan.NONE ? null : cached;
        }
        VillagePlan plan = createVillagePlan(cellX, cellZ);
        VillagePlan stored = plan == null ? VillagePlan.NONE : plan;
        VillagePlan existing = villagePlanCache.putIfAbsent(cacheKey, stored);
        VillagePlan result = existing == null ? stored : existing;
        return result == VillagePlan.NONE ? null : result;
    }

    private VillagePlan createVillagePlan(int cellX, int cellZ) {
        long cellSeed = mix64(seed ^ ((long) cellX * 918273645L) ^ ((long) cellZ * 192837465L));
        if (randomUnit(cellSeed) > VILLAGE_CHANCE) {
            return null;
        }
        int centerChunkX = cellX * VILLAGE_CELL_SIZE_CHUNKS + 2 + (int) (randomUnit(cellSeed ^ 0xA53A9E1BL) * 3.0);
        int centerChunkZ = cellZ * VILLAGE_CELL_SIZE_CHUNKS + 2 + (int) (randomUnit(cellSeed ^ 0xC13FA9A9L) * 3.0);
        int centerX = centerChunkX * GameConfig.CHUNK_SIZE + 8;
        int centerZ = centerChunkZ * GameConfig.CHUNK_SIZE + 8;
        int baseY = averageTerrainHeight(centerX - 22, centerZ - 22, 44, 44);
        if (!isVillageSiteSuitable(centerX, centerZ, baseY)) {
            return null;
        }
        return new VillagePlan(centerChunkX, centerChunkZ, centerX, centerZ, baseY, cellSeed);
    }

    private long packVillageCell(int cellX, int cellZ) {
        return (((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL);
    }

    private boolean isVillageSiteSuitable(int centerX, int centerZ, int baseY) {
        if (baseY <= GameConfig.SEA_LEVEL + 1 || baseY > GameConfig.SEA_LEVEL + 34) {
            return false;
        }
        if (!isNaturalVillageClearing(centerX, centerZ)) {
            return false;
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int samples = 0;
        for (int dx = -28; dx <= 28; dx += 7) {
            for (int dz = -28; dz <= 28; dz += 7) {
                int x = centerX + dx;
                int z = centerZ + dz;
                TerrainSample sample = terrainSampleAt(x, z);
                RiverSample river = sampleRiver(x, z, sample.approximateSurfaceHeight, sampleContinentalness(x, z), sampleLakeBasin(x, z));
                if (isAquaticTerrain(sample.terrainFlags)
                    || (sample.terrainFlags & TERRAIN_BEACH) != 0
                    || (river != null && river.wet && river.mask > 0.30)) {
                    return false;
                }
                minY = Math.min(minY, sample.approximateSurfaceHeight);
                maxY = Math.max(maxY, sample.approximateSurfaceHeight);
                samples++;
            }
        }
        return samples > 0 && maxY - minY <= 3 && Math.abs(baseY - ((minY + maxY) / 2)) <= 2;
    }

    private boolean isNaturalVillageClearing(int centerX, int centerZ) {
        int openSamples = 0;
        int samples = 0;
        for (int dx = -30; dx <= 30; dx += 10) {
            for (int dz = -30; dz <= 30; dz += 10) {
                Biome biome = sampleBiome(centerX + dx, centerZ + dz);
                if (!biomeCanHostVillageClearing(biome)) {
                    return false;
                }
                double clearing = climate01(fractalNoise((centerX + dx + 3800.0) * 0.018, (centerZ + dz - 4100.0) * 0.018, 2, 0.56));
                double treePressure = climate01(fractalNoise((centerX + dx - 900.0) * 0.040, (centerZ + dz + 760.0) * 0.040, 2, 0.54));
                if (clearing > 0.38 && treePressure < 0.72) {
                    openSamples++;
                }
                samples++;
            }
        }
        return samples > 0 && openSamples >= samples / 2;
    }

    private boolean biomeCanHostVillageClearing(Biome biome) {
        return biome == Biome.PLAINS
            || biome == Biome.MEADOW
            || biome == Biome.SAVANNA
            || biome == Biome.SHRUBLAND
            || biome == Biome.SEASONAL_FOREST
            || biome == Biome.FOREST
            || biome == Biome.RAINFOREST;
    }

    private void placeVillagePlan(GeneratedChunkColumn column, VillagePlan plan) {
        int buildY = plan.baseY + 1;
        softenVillageClearing(column, plan.centerX, plan.centerZ, plan.baseY, 38);
        levelVillageArea(column, plan.centerX - 5, plan.centerZ - 5, 11, 11, plan.baseY, 5);
        clearVillageCanopy(column, plan.centerX, plan.centerZ, plan.baseY, 42);
        levelVillageArea(column, plan.centerX - 5, plan.centerZ - 5, 11, 11, plan.baseY, 5);
        int wellVariant = (int) Math.floor(randomUnit(plan.seed ^ 0x7777L) * 3.0);
        StructureTemplates.place(column, "village_plaza", plan.centerX - 3, buildY, plan.centerZ - 3, wellVariant);
        placeVillagePath(column, plan.centerX - 35, plan.centerZ, plan.centerX + 35, plan.centerZ, plan.baseY);
        placeVillagePath(column, plan.centerX, plan.centerZ - 32, plan.centerX, plan.centerZ + 32, plan.baseY);

        int[][] houseSlots = {
            {-28, -16, -21, 0, 13, 12},
            {15, -18, 18, 0, 13, 12},
            {-31, 10, -24, 0, 15, 14},
            {16, 12, 19, 0, 13, 13},
            {-7, -31, 0, -24, 13, 12},
            {6, 22, 0, 24, 13, 12},
            {-35, -2, -29, 0, 13, 13},
            {25, -3, 29, 0, 13, 13}
        };
        int houseCount = 3 + (int) Math.floor(randomUnit(plan.seed ^ 0x1234L) * 6.0);
        for (int i = 0; i < houseSlots.length && i < houseCount; i++) {
            int[] slot = houseSlots[i];
            int originX = plan.centerX + slot[0];
            int originZ = plan.centerZ + slot[1];
            levelVillageArea(column, originX - 2, originZ - 2, slot[4], slot[5], plan.baseY, 6);
            String template = i == 2 || i == 5 ? "village_house_large" : smallHouseFor(plan.seed, i + 1);
            StructureTemplates.place(column, template, originX, buildY, originZ, rotationFor(plan.seed, i + 1));
            placeVillagePath(column, plan.centerX + slot[2], plan.centerZ + slot[3], originX + 4, originZ + 4, plan.baseY);
        }

        int[][] farmSlots = {
            {24, 22, 0, 24},
            {-40, 20, -32, 0},
            {26, -30, 28, 0}
        };
        int farmCount = 1 + (int) Math.floor(randomUnit(plan.seed ^ 0xBABE5L) * 3.0);
        for (int i = 0; i < farmSlots.length && i < farmCount; i++) {
            int[] slot = farmSlots[i];
            int originX = plan.centerX + slot[0];
            int originZ = plan.centerZ + slot[1];
            levelVillageArea(column, originX - 1, originZ - 1, 17, 15, plan.baseY, 4);
            StructureTemplates.place(column, "village_farm", originX, buildY, originZ, rotationFor(plan.seed, 20 + i));
            int endZ = slot[1] >= 0 ? originZ - 1 : originZ + 15;
            placeVillagePath(column, plan.centerX + slot[2], plan.centerZ + slot[3], originX + 8, endZ, plan.baseY);
        }
    }

    private String smallHouseFor(long planSeed, int salt) {
        return randomUnit(planSeed ^ (salt * 0x9E3779B97F4A7C15L)) < 0.5 ? "village_house_small_1" : "village_house_small_2";
    }

    private int rotationFor(long planSeed, int salt) {
        return (int) Math.floor(randomUnit(planSeed ^ (salt * 0x632BE59BD9B4E019L)) * 4.0) & 3;
    }

    private void softenVillageClearing(GeneratedChunkColumn column, int centerX, int centerZ, int baseY, int radius) {
        int radiusSq = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                removeTreeColumn(column, x, z, baseY);
                int surfaceY = findSurface(column, x, z);
                if (Math.abs(surfaceY - baseY) > 7) {
                    continue;
                }
                byte above = column.getBlock(x, surfaceY + 1, z);
                if (isPlant(above) || above == GameConfig.SNOW_LAYER) {
                    setStructureBlock(column, x, surfaceY + 1, z, GameConfig.AIR);
                }
                if (surfaceY < baseY - 3) {
                    int softenedY = Math.min(baseY - 2, surfaceY + 2);
                    stabilizeVillageColumn(column, x, z, softenedY, 5, GameConfig.GRASS, 2);
                }
            }
        }
    }

    private void clearVillageCanopy(GeneratedChunkColumn column, int centerX, int centerZ, int baseY, int radius) {
        int radiusSq = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                int dx = x - centerX;
                int dz = z - centerZ;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                removeTreeColumn(column, x, z, baseY);
                int surfaceY = findSurface(column, x, z);
                if (Math.abs(surfaceY - baseY) > 6) {
                    continue;
                }
                byte surface = column.getBlock(x, surfaceY, z);
                if (surface == GameConfig.OAK_LOG || surface == GameConfig.PINE_LOG || surface == GameConfig.BIRCH_LOG) {
                    setStructureBlock(column, x, surfaceY, z, GameConfig.GRASS);
                }
                for (int y = Math.max(GameConfig.WORLD_MIN_Y + 1, baseY - 2); y <= Math.min(GameConfig.WORLD_MAX_Y, baseY + 28); y++) {
                    byte block = column.getBlock(x, y, z);
                    if (block == GameConfig.OAK_LOG
                        || block == GameConfig.OAK_LEAVES
                        || block == GameConfig.PINE_LOG
                        || block == GameConfig.PINE_LEAVES
                        || block == GameConfig.BIRCH_LOG
                        || block == GameConfig.BIRCH_LEAVES
                        || block == GameConfig.TALL_GRASS
                        || block == GameConfig.RED_FLOWER
                        || block == GameConfig.YELLOW_FLOWER
                        || block == GameConfig.SNOW_LAYER) {
                        setStructureBlock(column, x, y, z, GameConfig.AIR);
                    }
                }
            }
        }
    }

    private void removeTreeColumn(GeneratedChunkColumn column, int x, int z, int baseY) {
        for (int y = Math.max(GameConfig.WORLD_MIN_Y + 1, baseY - 3); y <= Math.min(GameConfig.WORLD_MAX_Y, baseY + 28); y++) {
            byte block = column.getBlock(x, y, z);
            if (block == GameConfig.OAK_LOG
                || block == GameConfig.OAK_LEAVES
                || block == GameConfig.PINE_LOG
                || block == GameConfig.PINE_LEAVES
                || block == GameConfig.BIRCH_LOG
                || block == GameConfig.BIRCH_LEAVES) {
                setStructureBlock(column, x, y, z, GameConfig.AIR);
            }
        }
    }

    private void placeVillagePath(GeneratedChunkColumn column, int fromX, int fromZ, int toX, int toZ, int baseY) {
        int stepX = Integer.compare(toX, fromX);
        for (int x = fromX; x != toX + stepX; x += stepX == 0 ? 1 : stepX) {
            placeWidePathBlock(column, x, fromZ, baseY);
            if (stepX == 0) {
                break;
            }
        }
        int stepZ = Integer.compare(toZ, fromZ);
        for (int z = fromZ; z != toZ + stepZ; z += stepZ == 0 ? 1 : stepZ) {
            placeWidePathBlock(column, toX, z, baseY);
            if (stepZ == 0) {
                break;
            }
        }
    }

    private void placeWidePathBlock(GeneratedChunkColumn column, int x, int z, int baseY) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 2) {
                    continue;
                }
                placePathBlock(column, x + dx, z + dz, baseY, 6, villageRoadSurfaceBlock(x + dx, z + dz, Math.abs(dx) + Math.abs(dz)));
            }
        }
    }

    private void placePathBlock(GeneratedChunkColumn column, int x, int z, int baseY, int maxDelta, byte surfaceBlock) {
        if (!isInColumn(column, x, z)) {
            return;
        }
        int surfaceY = findSurface(column, x, z);
        if (surfaceY <= GameConfig.WORLD_MIN_Y) {
            return;
        }
        int roadY = villageRoadY(x, z, surfaceY, baseY);
        if (hasVillageStructureAboveRoad(column, x, z, roadY)) {
            return;
        }
        stabilizeVillageColumn(column, x, z, roadY, VILLAGE_ROAD_FOUNDATION_DEPTH, surfaceBlock, 3);
    }

    private boolean hasVillageStructureAboveRoad(GeneratedChunkColumn column, int x, int z, int roadY) {
        if (column.getBlock(x, roadY, z) == InventoryItems.OAK_PLANKS) {
            return true;
        }
        int top = Math.min(GameConfig.WORLD_MAX_Y, roadY + 6);
        for (int y = roadY + 1; y <= top; y++) {
            if (isVillageStructureBlock(column.getBlock(x, y, z))) {
                return true;
            }
        }
        return false;
    }

    private boolean isVillageStructureBlock(byte block) {
        switch (block) {
            case GameConfig.OAK_LOG:
            case GameConfig.OAK_FENCE:
            case GameConfig.OAK_DOOR:
            case GameConfig.CHEST:
            case GameConfig.CRAFTING_TABLE:
            case GameConfig.FURNACE:
            case GameConfig.GLASS:
            case GameConfig.TORCH:
            case GameConfig.RED_BED:
                return true;
            default:
                return block == InventoryItems.OAK_PLANKS;
        }
    }

    private int villageRoadY(int worldX, int worldZ, int surfaceY, int baseY) {
        double gradeNoise = fractalNoise((worldX + 910.0) * 0.035, (worldZ - 410.0) * 0.035, 2, 0.50) * 0.45;
        int target = (int) Math.round(lerp(surfaceY, baseY + gradeNoise, 0.58));
        return clamp(target, baseY - 2, baseY + 2);
    }

    private byte villageRoadSurfaceBlock(int worldX, int worldZ, int edgeDistance) {
        if (edgeDistance >= 2) {
            double edgeNoise = randomUnit(mix64(seed ^ ((long) worldX * 73428767L) ^ ((long) worldZ * 912931L)));
            if (edgeNoise < 0.18) {
                return GameConfig.DIRT;
            }
            if (edgeNoise > 0.86) {
                return GameConfig.COBBLESTONE;
            }
        }
        return GameConfig.GRAVEL;
    }

    private void levelVillageArea(GeneratedChunkColumn column, int originX, int originZ, int width, int depth, int groundY, int clearHeight) {
        for (int x = originX; x < originX + width; x++) {
            for (int z = originZ; z < originZ + depth; z++) {
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                int surfaceY = findSurface(column, x, z);
                if (surfaceY <= GameConfig.WORLD_MIN_Y) {
                    continue;
                }
                stabilizeVillageColumn(column, x, z, groundY, VILLAGE_FOUNDATION_DEPTH, GameConfig.GRASS, clearHeight);
            }
        }
    }

    private void stabilizeVillageColumn(GeneratedChunkColumn column, int x, int z, int groundY, int foundationDepth, byte surfaceBlock, int clearHeight) {
        int bottomY = villagePlinthBottom(column, x, z, groundY, foundationDepth);
        int surfaceY = findSurface(column, x, z);
        for (int y = bottomY; y < groundY; y++) {
            if (column.getBlock(x, y, z) != GameConfig.BEDROCK) {
                setStructureBlock(column, x, y, z, villageFoundationBlock(y, groundY));
            }
        }
        setStructureBlock(column, x, groundY, z, surfaceBlock);
        int clearTop = Math.max(groundY + clearHeight, surfaceY + clearHeight);
        for (int y = groundY + 1; y <= Math.min(GameConfig.WORLD_MAX_Y, clearTop); y++) {
            setStructureBlock(column, x, y, z, GameConfig.AIR);
        }
    }

    private int villagePlinthBottom(GeneratedChunkColumn column, int x, int z, int groundY, int minimumDepth) {
        int minimumBottom = Math.max(GameConfig.WORLD_MIN_Y + 1, groundY - minimumDepth);
        int y = groundY - 1;
        while (y > GameConfig.WORLD_MIN_Y + 1) {
            byte block = column.getBlock(x, y, z);
            if (block == GameConfig.BEDROCK) {
                return y + 1;
            }
            if (y <= minimumBottom) {
                return shouldReplaceVillageFoundation(block) ? minimumBottom : y + 1;
            }
            y--;
        }
        return GameConfig.WORLD_MIN_Y + 1;
    }

    private boolean shouldReplaceVillageFoundation(byte block) {
        return block == GameConfig.AIR
            || GameConfig.isLiquidBlock(block)
            || isPlant(block)
            || block == GameConfig.OAK_LOG
            || block == GameConfig.PINE_LOG
            || block == GameConfig.BIRCH_LOG
            || block == GameConfig.OAK_LEAVES
            || block == GameConfig.PINE_LEAVES
            || block == GameConfig.BIRCH_LEAVES;
    }

    private byte villageFoundationBlock(int y, int groundY) {
        return y >= groundY - 2 ? GameConfig.DIRT : GameConfig.COBBLESTONE;
    }

    private int averageTerrainHeight(int originX, int originZ, int width, int depth) {
        int total = 0;
        int count = 0;
        for (int x = originX; x < originX + width; x += Math.max(1, width / 3)) {
            for (int z = originZ; z < originZ + depth; z += Math.max(1, depth / 3)) {
                total += estimateSurfaceHeight(x, z);
                count++;
            }
        }
        return count == 0 ? GameConfig.SEA_LEVEL : Math.round((float) total / count);
    }

    private void populateMineshaftPieces(GeneratedChunkColumn column, int startX, int startZ) {
        int minCellX = Math.floorDiv(column.chunkX - 5, MINESHAFT_CELL_SIZE_CHUNKS);
        int maxCellX = Math.floorDiv(column.chunkX + 5, MINESHAFT_CELL_SIZE_CHUNKS);
        int minCellZ = Math.floorDiv(column.chunkZ - 5, MINESHAFT_CELL_SIZE_CHUNKS);
        int maxCellZ = Math.floorDiv(column.chunkZ + 5, MINESHAFT_CELL_SIZE_CHUNKS);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                MineshaftPlan plan = sampleMineshaftPlan(cellX, cellZ);
                if (plan == null
                    || Math.abs(column.chunkX - plan.centerChunkX) > 5
                    || Math.abs(column.chunkZ - plan.centerChunkZ) > 5) {
                    continue;
                }
                placeMineshaftNetwork(column, plan);
            }
        }
    }

    private MineshaftPlan sampleMineshaftPlan(int cellX, int cellZ) {
        long cellSeed = mix64(seed ^ ((long) cellX * 73428767L) ^ ((long) cellZ * 912931L));
        if (randomUnit(cellSeed) > MINESHAFT_CHANCE) {
            return null;
        }
        int centerChunkX = cellX * MINESHAFT_CELL_SIZE_CHUNKS + 2 + (int) (randomUnit(cellSeed ^ 0x31337L) * 3.0);
        int centerChunkZ = cellZ * MINESHAFT_CELL_SIZE_CHUNKS + 2 + (int) (randomUnit(cellSeed ^ 0x51515L) * 3.0);
        int y = GameConfig.SEA_LEVEL - 26 - (int) Math.round(randomUnit(cellSeed ^ 0x51EEDL) * 42.0);
        y = clamp(y, GameConfig.WORLD_MIN_Y + 12, GameConfig.SEA_LEVEL - 12);
        return new MineshaftPlan(centerChunkX, centerChunkZ, centerChunkX * GameConfig.CHUNK_SIZE + 8, centerChunkZ * GameConfig.CHUNK_SIZE + 8, y, cellSeed);
    }

    private void placeMineshaftNetwork(GeneratedChunkColumn column, MineshaftPlan plan) {
        StructureTemplates.place(column, "mineshaft_room", plan.centerX, plan.y, plan.centerZ, 0);
        for (int direction = 0; direction < 4; direction++) {
            int length = 28 + (int) (randomUnit(plan.seed ^ (direction * 0x9E3779B97F4A7C15L)) * 26.0);
            carveMineshaftTunnel(column, plan.seed, plan.centerX, plan.y, plan.centerZ, direction, length, direction, true);
        }
    }

    private void carveMineshaftTunnel(GeneratedChunkColumn column, long seedValue, int startX, int startY, int startZ,
                                      int direction, int length, int pathId, boolean branchable) {
        int dx = direction == 1 ? 1 : (direction == 3 ? -1 : 0);
        int dz = direction == 2 ? 1 : (direction == 0 ? -1 : 0);
        int x = startX;
        int z = startZ;
        int y = startY;
        boolean railRun = randomUnit(seedValue ^ (pathId * 0xABC98388FB8FAC03L)) < 0.64;
        for (int step = 1; step <= length; step++) {
            x += dx;
            z += dz;
            if (step % 13 == 0) {
                y += randomUnit(seedValue ^ (pathId * 7919L) ^ step) > 0.5 ? 1 : -1;
                y = clamp(y, GameConfig.WORLD_MIN_Y + 10, GameConfig.SEA_LEVEL - 8);
            }
            if (step > 10 && randomUnit(seedValue ^ (pathId * 4177L) ^ (step * 131L)) < 0.035) {
                placeMineshaftDamageMarker(column, x, y, z, dx != 0);
                step += 3;
                continue;
            }
            carveMineshaftCell(column, x, y, z);
            if (step % 5 == 0) {
                StructureTemplates.place(column, "mineshaft_support", x, y, z, dx != 0 ? 0 : 1);
            }
            if (step % 11 == 0 && randomUnit(seedValue ^ (pathId * 973L) ^ step) < 0.45) {
                setStructureBlock(column, x, y + 2, z, GameConfig.TORCH);
            }
            if (railRun && (step & 1) == 0 && randomUnit(seedValue ^ (step * 31L) ^ (pathId * 17L)) > 0.22) {
                setStructureBlock(column, x, y, z, GameConfig.RAIL);
            }
            if (step > 7 && randomUnit(seedValue ^ (pathId * 1193L) ^ (step * 23L)) < 0.055) {
                placeMineshaftDamageMarker(column, x, y, z, dx != 0);
            }
            if (branchable && step > 8 && step % 14 == 0) {
                carveMineshaftIntersection(column, x, y, z);
                int side = randomUnit(seedValue ^ (step * 0x5DEECE66DL) ^ pathId) < 0.5 ? turnLeft(direction) : turnRight(direction);
                int branchLength = 10 + (int) (randomUnit(seedValue ^ (step * 0xBEEFL) ^ pathId) * 18.0);
                carveMineshaftTunnel(column, seedValue ^ (step * 0x632BE59BD9B4E019L), x, y, z, side, branchLength, pathId * 10 + step, false);
            }
            if (branchable && step > 16 && step % 23 == 0 && randomUnit(seedValue ^ (step * 0x7777L)) < 0.48) {
                carveMineshaftRoom(column, x, y, z, 3, 2, 3);
                if (randomUnit(seedValue ^ (step * 0x9999L)) < 0.55) {
                    setStructureBlock(column, x + (dx == 0 ? 1 : 0), y, z + (dz == 0 ? 1 : 0), GameConfig.CHEST);
                }
            }
        }
    }

    private int turnLeft(int direction) {
        return (direction + 3) & 3;
    }

    private int turnRight(int direction) {
        return (direction + 1) & 3;
    }

    private void carveMineshaftCell(GeneratedChunkColumn column, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    setStructureBlock(column, x + dx, y + dy, z + dz, GameConfig.AIR);
                }
                setStructureBlock(column, x + dx, y - 1, z + dz, InventoryItems.OAK_PLANKS);
            }
        }
    }

    private void carveMineshaftIntersection(GeneratedChunkColumn column, int x, int y, int z) {
        StructureTemplates.place(column, "mineshaft_crossing", x, y, z, 0);
    }

    private void carveMineshaftRoom(GeneratedChunkColumn column, int centerX, int y, int centerZ, int radiusX, int radiusY, int radiusZ) {
        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                for (int dy = -1; dy <= radiusY + 1; dy++) {
                    boolean shell = Math.abs(dx) == radiusX || Math.abs(dz) == radiusZ || dy == -1;
                    setStructureBlock(column, centerX + dx, y + dy, centerZ + dz, shell && dy == -1 ? InventoryItems.OAK_PLANKS : GameConfig.AIR);
                }
            }
        }
        setStructureBlock(column, centerX - radiusX + 1, y, centerZ - radiusZ + 1, GameConfig.CHEST);
        setStructureBlock(column, centerX + radiusX - 1, y, centerZ + radiusZ - 1, GameConfig.TORCH);
    }

    private void placeMineshaftDamageMarker(GeneratedChunkColumn column, int x, int y, int z, boolean alongX) {
        int sideX = alongX ? 0 : 1;
        int sideZ = alongX ? 1 : 0;
        setStructureBlock(column, x, y, z, GameConfig.AIR);
        setStructureBlock(column, x, y - 1, z, GameConfig.GRAVEL);
        setStructureBlock(column, x + sideX, y - 1, z + sideZ, GameConfig.COBBLESTONE);
        setStructureBlock(column, x - sideX, y, z - sideZ, GameConfig.AIR);
        setStructureBlock(column, x + sideX, y + 1, z + sideZ, GameConfig.AIR);
        if (randomUnit(mix64(seed ^ ((long) x * 31L) ^ ((long) z * 17L) ^ y)) < 0.45) {
            setStructureBlock(column, x + sideX, y, z + sideZ, GameConfig.OAK_FENCE);
        }
    }

    private boolean isInColumn(GeneratedChunkColumn column, int worldX, int worldZ) {
        return Math.floorDiv(worldX, GameConfig.CHUNK_SIZE) == column.chunkX
            && Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE) == column.chunkZ;
    }

    private boolean isParadiseChunk(int chunkX, int chunkZ) {
        if (!GameConfig.isParadiseDimension(dimensionId)) {
            return false;
        }
        int minX = chunkX * GameConfig.CHUNK_SIZE;
        int minZ = chunkZ * GameConfig.CHUNK_SIZE;
        int maxX = minX + GameConfig.CHUNK_SIZE - 1;
        int maxZ = minZ + GameConfig.CHUNK_SIZE - 1;
        return maxX >= GameConfig.PARADISE_ORIGIN_X - GameConfig.PARADISE_RADIUS_BLOCKS
            && minX <= GameConfig.PARADISE_ORIGIN_X + GameConfig.PARADISE_RADIUS_BLOCKS
            && maxZ >= GameConfig.PARADISE_ORIGIN_Z - GameConfig.PARADISE_RADIUS_BLOCKS
            && minZ <= GameConfig.PARADISE_ORIGIN_Z + GameConfig.PARADISE_RADIUS_BLOCKS;
    }

    private GeneratedChunkColumn generateParadiseChunk(int chunkX, int chunkZ) {
        GeneratedChunkColumn column = new GeneratedChunkColumn(chunkX, chunkZ);
        int startX = chunkX * GameConfig.CHUNK_SIZE;
        int startZ = chunkZ * GameConfig.CHUNK_SIZE;
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = paradiseSurfaceHeight(worldX, worldZ);
                column.setSurfaceHeight(localX, localZ, surfaceY);
                if (surfaceY <= GameConfig.WORLD_MIN_Y) {
                    continue;
                }
                int thickness = paradiseIslandThickness(worldX, worldZ, surfaceY);
                int bottomY = Math.max(GameConfig.WORLD_MIN_Y + 4, surfaceY - thickness);
                for (int y = bottomY; y <= surfaceY; y++) {
                    byte block = y == surfaceY
                        ? GameConfig.GRASS
                        : y >= surfaceY - 3 ? GameConfig.DIRT : (y < surfaceY - thickness + 2 ? GameConfig.DEEPSLATE : GameConfig.STONE);
                    column.setBlock(worldX, y, worldZ, block);
                }
            }
        }
        populateParadiseFeatures(column, startX, startZ);
        placeParadiseReturnPortal(column);
        column.setStatus(ChunkGenerationStatus.FULL);
        return column;
    }

    private int paradiseSurfaceHeight(int worldX, int worldZ) {
        double dx = worldX - GameConfig.PARADISE_ORIGIN_X;
        double dz = worldZ - GameConfig.PARADISE_ORIGIN_Z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double edgeNoise = fractalNoise((worldX + 17.0) * 0.045, (worldZ - 31.0) * 0.045, 3, 0.55) * 3.0;
        if (distance <= 36.0 + edgeNoise) {
            if (distance <= 10.0) {
                return GameConfig.PARADISE_SURFACE_Y;
            }
            double heightNoise = fractalNoise((worldX - 240.0) * 0.060, (worldZ + 180.0) * 0.060, 2, 0.54);
            return GameConfig.PARADISE_SURFACE_Y + (int) Math.round(heightNoise * 3.0 - Math.max(0.0, distance - 24.0) * 0.10);
        }

        int best = GameConfig.WORLD_MIN_Y;
        int cellX = Math.floorDiv(worldX - GameConfig.PARADISE_ORIGIN_X, 56);
        int cellZ = Math.floorDiv(worldZ - GameConfig.PARADISE_ORIGIN_Z, 56);
        for (int oz = -1; oz <= 1; oz++) {
            for (int ox = -1; ox <= 1; ox++) {
                int islandCellX = cellX + ox;
                int islandCellZ = cellZ + oz;
                long islandSeed = mix64(seed ^ 0x51A7E11L ^ ((long) islandCellX * 73428767L) ^ ((long) islandCellZ * 912931L));
                if (randomUnit(islandSeed) > 0.58) {
                    continue;
                }
                int centerX = GameConfig.PARADISE_ORIGIN_X + islandCellX * 56 + (int) Math.round((randomUnit(islandSeed ^ 0x31L) - 0.5) * 20.0);
                int centerZ = GameConfig.PARADISE_ORIGIN_Z + islandCellZ * 56 + (int) Math.round((randomUnit(islandSeed ^ 0x57L) - 0.5) * 20.0);
                double centerDistance = Math.hypot(centerX - GameConfig.PARADISE_ORIGIN_X, centerZ - GameConfig.PARADISE_ORIGIN_Z);
                if (centerDistance > GameConfig.PARADISE_RADIUS_BLOCKS - 28.0) {
                    continue;
                }
                double radius = 10.0 + randomUnit(islandSeed ^ 0x71L) * 12.0;
                double islandDistance = Math.hypot(worldX - centerX, worldZ - centerZ);
                if (islandDistance > radius) {
                    continue;
                }
                double heightNoise = fractalNoise((worldX + islandCellX * 11.0) * 0.070, (worldZ - islandCellZ * 13.0) * 0.070, 2, 0.52);
                int height = GameConfig.PARADISE_SURFACE_Y + 10 + (int) Math.round(randomUnit(islandSeed ^ 0x99L) * 18.0 + heightNoise * 3.0);
                best = Math.max(best, height);
            }
        }
        return best;
    }

    private int paradiseIslandThickness(int worldX, int worldZ, int surfaceY) {
        double dx = worldX - GameConfig.PARADISE_ORIGIN_X;
        double dz = worldZ - GameConfig.PARADISE_ORIGIN_Z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 40.0) {
            return 7 + (int) Math.max(0.0, (36.0 - distance) * 0.28);
        }
        double noise = climate01(fractalNoise((worldX + surfaceY) * 0.09, (worldZ - surfaceY) * 0.09, 2, 0.55));
        return 5 + (int) Math.round(noise * 4.0);
    }

    private void populateParadiseFeatures(GeneratedChunkColumn column, int startX, int startZ) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = column.getSurfaceHeight(localX, localZ);
                if (surfaceY <= GameConfig.WORLD_MIN_Y || surfaceY + 6 >= GameConfig.WORLD_MAX_Y) {
                    continue;
                }
                if (Math.abs(worldX - GameConfig.PARADISE_ORIGIN_X) <= 5
                    && Math.abs(worldZ - GameConfig.PARADISE_ORIGIN_Z) <= 8) {
                    continue;
                }
                long featureSeed = mix64(seed ^ 0x9ADE5EEDL ^ (((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL));
                double roll = randomUnit(featureSeed);
                if (roll < 0.028) {
                    placeParadiseTree(column, worldX, surfaceY + 1, worldZ, featureSeed);
                } else if (roll < 0.22 && column.getBlock(worldX, surfaceY + 1, worldZ) == GameConfig.AIR) {
                    column.setBlock(worldX, surfaceY + 1, worldZ,
                        ((featureSeed >>> 8) & 1L) == 0L ? GameConfig.YELLOW_FLOWER : GameConfig.RED_FLOWER);
                } else if (roll < 0.44 && column.getBlock(worldX, surfaceY + 1, worldZ) == GameConfig.AIR) {
                    column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.TALL_GRASS);
                }
            }
        }
    }

    private void placeParadiseTree(GeneratedChunkColumn column, int x, int y, int z, long featureSeed) {
        int height = 4 + (int) Math.round(randomUnit(featureSeed ^ 0xC0DEL) * 2.0);
        for (int dy = 0; dy < height; dy++) {
            column.setBlock(x, y + dy, z, GameConfig.BIRCH_LOG);
        }
        int leafBase = y + height - 2;
        for (int dy = 0; dy <= 3; dy++) {
            int radius = dy == 3 ? 1 : 2;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && dy < 2) {
                        continue;
                    }
                    column.setBlock(x + dx, leafBase + dy, z + dz, GameConfig.BIRCH_LEAVES);
                }
            }
        }
    }

    private void placeParadiseReturnPortal(GeneratedChunkColumn column) {
        int originX = GameConfig.PARADISE_ORIGIN_X - 1;
        int originY = GameConfig.PARADISE_SURFACE_Y + 1;
        int originZ = GameConfig.PARADISE_ORIGIN_Z + 2;
        for (int width = 0; width < 4; width++) {
            column.setBlock(originX + width, originY, originZ, GameConfig.GLASS);
            column.setBlock(originX + width, originY + 4, originZ, GameConfig.GLASS);
        }
        for (int height = 1; height <= 3; height++) {
            column.setBlock(originX, originY + height, originZ, GameConfig.GLASS);
            column.setBlock(originX + 3, originY + height, originZ, GameConfig.GLASS);
            column.setBlock(originX + 1, originY + height, originZ, GameConfig.PARADISE_PORTAL);
            column.setBlock(originX + 2, originY + height, originZ, GameConfig.PARADISE_PORTAL);
        }
    }

    private void setStructureBlock(GeneratedChunkColumn column, int worldX, int worldY, int worldZ, byte block) {
        if (Math.floorDiv(worldX, GameConfig.CHUNK_SIZE) != column.chunkX
            || Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE) != column.chunkZ) {
            return;
        }
        column.setBlock(worldX, worldY, worldZ, block);
    }

    int estimateSurfaceHeight(int worldX, int worldZ) {
        if (GameConfig.isParadiseArea(worldX, worldZ)) {
            int paradiseHeight = paradiseSurfaceHeight(worldX, worldZ);
            if (paradiseHeight > GameConfig.WORLD_MIN_Y) {
                return paradiseHeight;
            }
        }
        return sampleSurfaceHeight(worldX, worldZ, sampleBiome(worldX, worldZ));
    }

    int debugVillageCellSizeChunks() {
        return VILLAGE_CELL_SIZE_CHUNKS;
    }

    int[] debugVillageCenterForCell(int cellX, int cellZ) {
        VillagePlan plan = sampleVillagePlan(cellX, cellZ);
        if (plan == null) {
            return null;
        }
        return new int[]{plan.centerX, plan.baseY, plan.centerZ, plan.centerChunkX, plan.centerChunkZ};
    }

    boolean isFaceVisible(GeneratedChunkColumn column, int worldX, int worldY, int worldZ, Face face) {
        byte source = column.getBlock(worldX, worldY, worldZ);
        if (source == GameConfig.AIR) {
            return false;
        }

        int neighborX = worldX;
        int neighborY = worldY;
        int neighborZ = worldZ;
        switch (face) {
            case WEST:
                neighborX--;
                break;
            case EAST:
                neighborX++;
                break;
            case TOP:
                neighborY++;
                break;
            case BOTTOM:
                neighborY--;
                break;
            case NORTH:
                neighborZ--;
                break;
            case SOUTH:
                neighborZ++;
                break;
            default:
                break;
        }

        byte neighbor = column.getBlock(neighborX, neighborY, neighborZ);
        return !isOpaque(neighbor);
    }

    private int applySurfaceColumn(GeneratedChunkColumn column, int worldX, int worldZ, int dirtDepth, Biome biome, byte terrainFlags) {
        int surfaceHeight = findSurface(column, worldX, worldZ);
        if (surfaceHeight <= GameConfig.WORLD_MIN_Y) {
            return surfaceHeight;
        }

        boolean oceanFloorSurface = surfaceHeight < GameConfig.SEA_LEVEL;
        boolean beachTerrain = isBeachSurface(surfaceHeight, terrainFlags);
        int fillDepth = oceanFloorSurface ? Math.max(dirtDepth, OCEAN_SAND_DEPTH + 1) : (beachTerrain ? OCEAN_SAND_DEPTH : dirtDepth);
        int depth = 0;
        for (int worldY = surfaceHeight; worldY >= GameConfig.WORLD_MIN_Y; worldY--) {
            byte block = column.getBlock(worldX, worldY, worldZ);
            if (!isTerrainBaseBlock(block)) {
                if (block != GameConfig.AIR && !GameConfig.isLiquidBlock(block)) {
                    depth++;
                }
                continue;
            }

            if (worldY < -8) {
                break;
            }

            if ((beachTerrain && depth <= fillDepth)
                || (oceanFloorSurface && depth < OCEAN_SAND_DEPTH)) {
                column.setBlock(worldX, worldY, worldZ, GameConfig.SAND);
            } else if (depth == 0) {
                column.setBlock(worldX, worldY, worldZ, topBlockForTerrain(biome, terrainFlags, surfaceHeight, worldX, worldZ));
            } else if (depth <= fillDepth) {
                column.setBlock(worldX, worldY, worldZ, fillerBlockForTerrain(biome, terrainFlags, surfaceHeight, worldX, worldZ));
            } else {
                break;
            }
            depth++;
        }
        return surfaceHeight;
    }

    private boolean isTerrainBaseBlock(byte block) {
        return block == GameConfig.STONE || block == GameConfig.DEEPSLATE || block == GameConfig.COBBLESTONE;
    }

    private int bedrockTopY(int worldX, int worldZ) {
        double noise = climate01(fractalNoise((worldX + 31.0) * 0.23, (worldZ - 17.0) * 0.23, 2, 0.55));
        return GameConfig.WORLD_MIN_Y + 3 + (noise > 0.58 ? 1 : 0);
    }

    private byte baseStoneForY(int worldY) {
        return worldY < 0 ? GameConfig.DEEPSLATE : GameConfig.STONE;
    }

    private void cacheTerrainMetadata(int startX, int startZ, GenerationScratch scratch, boolean updateBiomes) {
        Arrays.fill(scratch.generationMasks, (byte) 0);
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                double continentalness = densitySampler.sampleContinentalness(worldX, worldZ);
                double lakeBasin = sampleLakeBasin(worldX, worldZ);
                Biome biome = updateBiomes ? sampleBiome(worldX, worldZ) : BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                int surfaceHeight = sampleSurfaceHeight(worldX, worldZ, biome, continentalness, lakeBasin);
                RiverSample river = sampleRiver(worldX, worldZ, surfaceHeight, continentalness, lakeBasin);
                byte terrainFlags = sampleTerrainFlags(worldX, worldZ, continentalness, lakeBasin, surfaceHeight);
                int dirtDepth = sampleDirtDepth(worldX, worldZ, biome);
                scratch.biomeOrdinals[index] = (byte) biome.ordinal();
                scratch.surfaceHeights[index] = surfaceHeight;
                scratch.dirtDepths[index] = (byte) dirtDepth;
                scratch.terrainFlags[index] = terrainFlags;
                scratch.continentalness[index] = continentalness;
                scratch.lakeBasins[index] = lakeBasin;
                scratch.riverSamples[index] = river;
                if (river != null && river.wet && river.mask > 0.18) {
                    scratch.generationMasks[index] |= GENMASK_WET_RIVER;
                }
                scratch.samples[index].set(surfaceHeight, terrainFlags);
            }
        }
        cacheVillageGenerationMasks(startX, startZ, scratch);
    }

    private void cacheVillageGenerationMasks(int startX, int startZ, GenerationScratch scratch) {
        int chunkX = Math.floorDiv(startX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(startZ, GameConfig.CHUNK_SIZE);
        int minCellX = Math.floorDiv(chunkX - 4, VILLAGE_CELL_SIZE_CHUNKS);
        int maxCellX = Math.floorDiv(chunkX + 4, VILLAGE_CELL_SIZE_CHUNKS);
        int minCellZ = Math.floorDiv(chunkZ - 4, VILLAGE_CELL_SIZE_CHUNKS);
        int maxCellZ = Math.floorDiv(chunkZ + 4, VILLAGE_CELL_SIZE_CHUNKS);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                VillagePlan plan = sampleVillagePlan(cellX, cellZ);
                if (plan == null
                    || Math.abs(chunkX - plan.centerChunkX) > 4
                    || Math.abs(chunkZ - plan.centerChunkZ) > 4) {
                    continue;
                }
                markVillageGenerationMask(startX, startZ, scratch, plan);
            }
        }
    }

    private void markVillageGenerationMask(int startX, int startZ, GenerationScratch scratch, VillagePlan plan) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int index = columnIndex(localX, localZ);
                if (isVillageRoadColumn(plan, worldX, worldZ)) {
                    scratch.generationMasks[index] |= GENMASK_STRUCTURE | GENMASK_ROAD;
                } else if (isVillageStructureColumn(plan, worldX, worldZ)) {
                    scratch.generationMasks[index] |= GENMASK_STRUCTURE;
                }
            }
        }
    }

    private boolean isVillageStructureColumn(VillagePlan plan, int worldX, int worldZ) {
        if (inRect(worldX, worldZ, plan.centerX - 7, plan.centerZ - 7, 15, 15)) {
            return true;
        }
        if (inRect(worldX, worldZ, plan.centerX - 27, plan.centerZ - 21, 13, 12)) {
            return true;
        }
        if (inRect(worldX, worldZ, plan.centerX + 10, plan.centerZ - 22, 13, 13)) {
            return true;
        }
        if (inRect(worldX, worldZ, plan.centerX - 30, plan.centerZ + 10, 15, 14)) {
            return true;
        }
        if (inRect(worldX, worldZ, plan.centerX + 8, plan.centerZ + 8, 17, 15)) {
            return true;
        }
        if (randomUnit(plan.seed ^ 0x4040L) < 0.48
            && inRect(worldX, worldZ, plan.centerX + 21, plan.centerZ - 10, 17, 15)) {
            return true;
        }
        return randomUnit(plan.seed ^ 0x5050L) < 0.38
            && inRect(worldX, worldZ, plan.centerX + 13, plan.centerZ + 18, 13, 13);
    }

    private boolean isVillageRoadColumn(VillagePlan plan, int worldX, int worldZ) {
        if (isPathCorridor(worldX, worldZ, plan.centerX - 30, plan.centerZ, plan.centerX + 30, plan.centerZ, VILLAGE_ROAD_HALF_WIDTH)
            || isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ - 26, plan.centerX, plan.centerZ + 27, VILLAGE_ROAD_HALF_WIDTH)
            || isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ, plan.centerX - 21, plan.centerZ - 13, VILLAGE_ROAD_HALF_WIDTH)
            || isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ, plan.centerX + 16, plan.centerZ - 13, VILLAGE_ROAD_HALF_WIDTH)
            || isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ, plan.centerX - 23, plan.centerZ + 13, VILLAGE_ROAD_HALF_WIDTH)
            || isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ, plan.centerX + 17, plan.centerZ + 15, VILLAGE_ROAD_HALF_WIDTH)) {
            return true;
        }
        if (randomUnit(plan.seed ^ 0x4040L) < 0.48
            && isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ, plan.centerX + 29, plan.centerZ - 3, VILLAGE_ROAD_HALF_WIDTH)) {
            return true;
        }
        return randomUnit(plan.seed ^ 0x5050L) < 0.38
            && isPathCorridor(worldX, worldZ, plan.centerX, plan.centerZ, plan.centerX + 19, plan.centerZ + 21, VILLAGE_ROAD_HALF_WIDTH);
    }

    private boolean isPathCorridor(int worldX, int worldZ, int fromX, int fromZ, int toX, int toZ, int halfWidth) {
        return (between(worldX, fromX, toX) && Math.abs(worldZ - fromZ) <= halfWidth)
            || (between(worldZ, fromZ, toZ) && Math.abs(worldX - toX) <= halfWidth);
    }

    private boolean between(int value, int a, int b) {
        return value >= Math.min(a, b) && value <= Math.max(a, b);
    }

    private boolean inRect(int worldX, int worldZ, int originX, int originZ, int width, int depth) {
        return worldX >= originX
            && worldX < originX + width
            && worldZ >= originZ
            && worldZ < originZ + depth;
    }

    private boolean hasGenerationMask(GenerationScratch scratch, int startX, int startZ, int worldX, int worldZ, byte mask) {
        int index = localColumnIndex(startX, startZ, worldX, worldZ);
        return index >= 0 && (scratch.generationMasks[index] & mask) != 0;
    }

    private boolean shouldProtectCarve(GenerationScratch scratch, int startX, int startZ, int worldX, int worldY, int worldZ) {
        int index = localColumnIndex(startX, startZ, worldX, worldZ);
        if (index < 0) {
            return false;
        }
        byte mask = scratch.generationMasks[index];
        if (mask == 0) {
            return false;
        }
        int surfaceY = scratch.surfaceHeights[index];
        if ((mask & GENMASK_STRUCTURE) != 0 && worldY >= surfaceY - VILLAGE_STRUCTURE_PROTECTION_DEPTH) {
            return true;
        }
        return (mask & GENMASK_WET_RIVER) != 0 && worldY >= surfaceY - WET_RIVER_CAVE_PROTECTION_DEPTH;
    }

    private boolean shouldProtectSurfaceRoof(GenerationScratch scratch, int startX, int startZ, int worldX, int worldY, int worldZ) {
        int index = localColumnIndex(startX, startZ, worldX, worldZ);
        if (index < 0) {
            return false;
        }
        int surfaceY = scratch.surfaceHeights[index];
        int depth = surfaceY - worldY;
        if (depth < 0) {
            return false;
        }
        Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
        byte terrainFlags = scratch.terrainFlags[index];
        RiverSample river = scratch.riverSamples[index];
        boolean riverWet = river != null && river.wet && river.mask > 0.18;
        return depth < caveRoofDepth(biome, terrainFlags, riverWet);
    }

    private int localColumnIndex(int startX, int startZ, int worldX, int worldZ) {
        int localX = worldX - startX;
        int localZ = worldZ - startZ;
        if (localX < 0 || localX >= GameConfig.CHUNK_SIZE || localZ < 0 || localZ >= GameConfig.CHUNK_SIZE) {
            return -1;
        }
        return columnIndex(localX, localZ);
    }

    private void carveCaves(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                int surfaceHeight = scratch.surfaceHeights[index];
                byte terrainFlags = scratch.terrainFlags[index];
                RiverSample river = scratch.riverSamples[index];
                boolean riverWet = river != null && river.wet && river.mask > 0.18;
                boolean aquatic = isAquaticTerrain(terrainFlags) || riverWet;
                int roofDepth = caveRoofDepth(biome, terrainFlags, riverWet);
                boolean entranceColumn = false;
                int caveCeiling = Math.min(surfaceHeight - roofDepth, GameConfig.WORLD_MAX_Y - 1);
                for (int worldY = GameConfig.WORLD_MIN_Y + 5; worldY < caveCeiling; worldY++) {
                    if (shouldProtectCarve(scratch, startX, startZ, worldX, worldY, worldZ)) {
                        continue;
                    }
                    byte block = column.getBlock(worldX, worldY, worldZ);
                    if (!canCarveCaveBlock(block) && !canCarveCaveEntranceBlock(block, entranceColumn)) {
                        continue;
                    }

                    int depth = surfaceHeight - worldY;
                    if (aquatic && depth < roofDepth + 6) {
                        continue;
                    }

                    boolean stoneLike = canCarveCaveBlock(block);
                    double caveDensity = stoneLike ? densitySampler.sampleCaveDensity(worldX, worldY, worldZ) : 0.0;
                    boolean protectedRoof = depth < roofDepth + (aquatic ? 10 : 2);
                    boolean carveTunnel = stoneLike
                        && !protectedRoof
                        && depth >= roofDepth + 2
                        && caveDensity > (worldY < 0 ? CAVE_TUNNEL_THRESHOLD - 0.035 : CAVE_TUNNEL_THRESHOLD + 0.035);
                    boolean carveRoom = stoneLike
                        && !protectedRoof
                        && worldY < 28
                        && depth >= roofDepth + 18
                        && caveDensity > CAVE_ROOM_THRESHOLD + 0.045;
                    boolean carveEntrance = false;
                    boolean carveCanyon = stoneLike
                        && !riverWet
                        && depth >= roofDepth + 10
                        && shouldCarveCanyon(worldX, worldY, worldZ, surfaceHeight, biome, terrainFlags);

                    if (carveTunnel || carveRoom || carveEntrance || carveCanyon) {
                        column.setBlock(worldX, worldY, worldZ, GameConfig.AIR);
                    }
                }
            }
        }
    }

    private void carveCaveNetworks(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        int minCellX = Math.floorDiv(startX - 72, CAVE_SYSTEM_SIZE);
        int maxCellX = Math.floorDiv(startX + GameConfig.CHUNK_SIZE + 72, CAVE_SYSTEM_SIZE);
        int minCellZ = Math.floorDiv(startZ - 72, CAVE_SYSTEM_SIZE);
        int maxCellZ = Math.floorDiv(startZ + GameConfig.CHUNK_SIZE + 72, CAVE_SYSTEM_SIZE);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                long caveSeed = mix64(seed ^ ((long) cellX * 544910567L) ^ ((long) cellZ * 1301081L));
                if (randomUnit(caveSeed) > 0.76) {
                    continue;
                }
                int centerX = cellX * CAVE_SYSTEM_SIZE + 14 + (int) (randomUnit(caveSeed ^ 0xCAFE1L) * 36.0);
                int centerZ = cellZ * CAVE_SYSTEM_SIZE + 14 + (int) (randomUnit(caveSeed ^ 0xCAFE2L) * 36.0);
                TerrainSample sample = terrainSampleAt(centerX, centerZ);
                if (isAquaticTerrain(sample.terrainFlags)) {
                    continue;
                }
                if (hasGenerationMask(scratch, startX, startZ, centerX, centerZ, (byte) (GENMASK_STRUCTURE | GENMASK_WET_RIVER))) {
                    continue;
                }
                double layerRoll = randomUnit(caveSeed ^ 0xCAFE3L);
                int centerY;
                if (layerRoll < 0.58) {
                    centerY = sample.approximateSurfaceHeight - 18 - (int) Math.round(randomUnit(caveSeed ^ 0xCAFE4L) * 34.0);
                } else {
                    centerY = GameConfig.SEA_LEVEL - 26 - (int) Math.round(randomUnit(caveSeed ^ 0xCAFE5L) * 54.0);
                }
                centerY = clamp(centerY, GameConfig.WORLD_MIN_Y + 13, Math.min(sample.approximateSurfaceHeight - 12, GameConfig.SEA_LEVEL + 4));
                boolean deepSystem = centerY < 8;
                double roomRadius = deepSystem
                    ? 2.7 + randomUnit(caveSeed ^ 0x11L) * 1.8
                    : 2.4 + randomUnit(caveSeed ^ 0x11L) * 1.2;
                carveCaveRoom(column, scratch, startX, startZ, centerX, centerY, centerZ, roomRadius, deepSystem ? 1.55 : 1.45, true);
                if (randomUnit(caveSeed ^ 0x1212L) < 0.55) {
                    carveCavePillar(column, centerX, centerY, centerZ, roomRadius * 0.55);
                }
                int tunnels = 3 + (int) (randomUnit(caveSeed ^ 0x22L) * 2.0);
                for (int tunnel = 0; tunnel < tunnels; tunnel++) {
                    carveCaveTunnel(column, scratch, startX, startZ, caveSeed, centerX, centerY, centerZ, tunnel);
                }
                if (randomUnit(caveSeed ^ 0x33L) < 0.58) {
                    carveCaveEntrance(column, scratch, startX, startZ, caveSeed, centerX, centerY, centerZ, sample.approximateSurfaceHeight);
                }
                if (randomUnit(caveSeed ^ 0x44L) < 0.24) {
                    carveVerticalCaveLink(column, scratch, startX, startZ, caveSeed, centerX, centerY, centerZ);
                }
                if (deepSystem && randomUnit(caveSeed ^ 0x5A5A5A5AL) < 0.12) {
                    carveKarstCavern(column, scratch, startX, startZ, caveSeed, centerX, centerY, centerZ);
                }
            }
        }
    }

    private void carveKarstCavern(GeneratedChunkColumn column, GenerationScratch scratch, int startX, int startZ,
                                  long caveSeed, int centerX, int centerY, int centerZ) {
        boolean lavaCavern = randomUnit(caveSeed ^ 0xDADAL) < 0.42;
        int cavernY = lavaCavern
            ? clamp(centerY - 12 - (int) (randomUnit(caveSeed ^ 0xBEEFL) * 20.0), GameConfig.WORLD_MIN_Y + 13, 8)
            : clamp(centerY - 4 + (int) Math.round((randomUnit(caveSeed ^ 0xFACE1L) - 0.5) * 16.0), 14, GameConfig.SEA_LEVEL - 12);
        double radius = lavaCavern
            ? 5.6 + randomUnit(caveSeed ^ 0xFACE2L) * 3.4
            : 6.2 + randomUnit(caveSeed ^ 0xFACE3L) * 4.2;
        carveCaveEllipsoid(column, scratch, startX, startZ, centerX, cavernY + 2, centerZ, radius, 2.15 + radius * 0.22, radius * 0.86, false);
        carveCaveEllipsoid(column, scratch, startX, startZ, centerX, cavernY - 1, centerZ, radius * 0.72, 1.25, radius * 0.62, false);
    }

    private void carveCaveTunnel(GeneratedChunkColumn column, GenerationScratch scratch, int columnStartX, int columnStartZ,
                                 long caveSeed, int startX, int startY, int startZ, int tunnelId) {
        double angle = (Math.PI * 0.5 * tunnelId) + (randomUnit(caveSeed ^ (tunnelId * 0x9E3779B97F4A7C15L)) - 0.5) * 0.42;
        double x = startX;
        double y = startY;
        double z = startZ;
        boolean deepTunnel = startY < -12;
        int length = 34 + (int) (randomUnit(caveSeed ^ (tunnelId * 0x12345L)) * (deepTunnel ? 34.0 : 28.0));
        double radius = (deepTunnel ? 1.35 : 1.42) + randomUnit(caveSeed ^ (tunnelId * 0x54321L)) * (deepTunnel ? 0.54 : 0.48);
        for (int step = 0; step < length; step++) {
            angle += fractalNoise((startX + tunnelId * 97.0 + step) * 0.025, (startZ - tunnelId * 53.0 + step) * 0.025, 1, 0.50) * (deepTunnel ? 0.052 : 0.075);
            x += Math.cos(angle);
            z += Math.sin(angle);
            y += fractalNoise((startX + step * 3.0) * 0.020, (startZ - step * 2.0) * 0.020, 1, 0.50) * (deepTunnel ? 0.10 : 0.18);
            y = clamp(y, GameConfig.WORLD_MIN_Y + 9.0, GameConfig.SEA_LEVEL + 18.0);
            double choke = climate01(fractalNoise3((x + tunnelId * 171.0) * 0.086, (y - 44.0) * 0.071, (z - tunnelId * 91.0) * 0.086, 1, 0.50));
            double stepRadius = radius * lerp(0.86, 1.22, choke) + Math.sin(step * 0.23) * 0.13;
            double verticalRadius = Math.max(deepTunnel ? 1.18 : 1.36, stepRadius * (deepTunnel ? 0.78 : 0.88));
            carveCaveEllipsoid(column, scratch, columnStartX, columnStartZ, (int) Math.round(x), (int) Math.round(y), (int) Math.round(z), stepRadius, verticalRadius, stepRadius, false);
            if (step > 12 && step % 22 == 0 && randomUnit(caveSeed ^ (tunnelId * 1777L) ^ step) < (deepTunnel ? 0.18 : 0.12)) {
                carveCaveRoom(column, scratch, columnStartX, columnStartZ, (int) Math.round(x), (int) Math.round(y), (int) Math.round(z),
                    (deepTunnel ? 2.0 : 2.2) + randomUnit(caveSeed ^ step) * (deepTunnel ? 0.9 : 0.8),
                    deepTunnel ? 1.20 : 1.25,
                    false);
            }
        }
    }

    private void carveCaveEntrance(GeneratedChunkColumn column, GenerationScratch scratch, int startX, int startZ,
                                   long caveSeed, int targetX, int targetY, int targetZ, int surfaceY) {
        int mouthX = targetX + (int) Math.round((randomUnit(caveSeed ^ 0xE17A9CEL) - 0.5) * 18.0);
        int mouthZ = targetZ + (int) Math.round((randomUnit(caveSeed ^ 0xE17A9CFL) - 0.5) * 18.0);
        TerrainSample mouthSample = terrainSampleAt(mouthX, mouthZ);
        if (!isDryCaveMouth(mouthX, mouthZ, mouthSample)) {
            return;
        }
        int steps = Math.max(18, Math.abs(mouthX - targetX) + Math.abs(mouthZ - targetZ) + Math.max(8, mouthSample.approximateSurfaceHeight - targetY));
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = (int) Math.round(lerp(mouthX, targetX, t));
            int z = (int) Math.round(lerp(mouthZ, targetZ, t));
            int y = (int) Math.round(lerp(mouthSample.approximateSurfaceHeight - 1, targetY, t));
            double radius = lerp(1.95, 1.34, t);
            carveCaveEllipsoid(column, scratch, startX, startZ, x, y, z, radius, 1.46, radius, true);
        }
    }

    private void carveSurfaceCaveEntrances(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        int minCellX = Math.floorDiv(startX - 32, 48);
        int maxCellX = Math.floorDiv(startX + GameConfig.CHUNK_SIZE + 32, 48);
        int minCellZ = Math.floorDiv(startZ - 32, 48);
        int maxCellZ = Math.floorDiv(startZ + GameConfig.CHUNK_SIZE + 32, 48);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                long entranceSeed = mix64(seed ^ ((long) cellX * 0x9E3779B97F4A7C15L) ^ ((long) cellZ * 0xC2B2AE3D27D4EB4FL));
                if (randomUnit(entranceSeed) > 0.16) {
                    continue;
                }
                int mouthX = cellX * 48 + 8 + (int) (randomUnit(entranceSeed ^ 0xE11L) * 32.0);
                int mouthZ = cellZ * 48 + 8 + (int) (randomUnit(entranceSeed ^ 0xE22L) * 32.0);
                int index = localColumnIndex(startX, startZ, mouthX, mouthZ);
                if (index < 0) {
                    continue;
                }
                TerrainSample mouthSample = terrainSampleAt(mouthX, mouthZ);
                RiverSample river = scratch.riverSamples[index];
                if (!isDryCaveMouth(mouthX, mouthZ, mouthSample)
                    || (river != null && river.mask > 0.14)
                    || hasGenerationMask(scratch, startX, startZ, mouthX, mouthZ, (byte) (GENMASK_STRUCTURE | GENMASK_ROAD | GENMASK_WET_RIVER))) {
                    continue;
                }
                int slope = terrainHeightDeltaAround(mouthX, mouthZ, 6, sampleBiome(mouthX, mouthZ));
                if (slope < 3 || slope > 22) {
                    continue;
                }

                double angle = randomUnit(entranceSeed ^ 0xE33L) * Math.PI * 2.0;
                int steps = 18 + (int) (randomUnit(entranceSeed ^ 0xE44L) * 14.0);
                double x = mouthX;
                double z = mouthZ;
                double y = mouthSample.approximateSurfaceHeight - 0.4;
                int endX = (int) Math.round(mouthX + Math.cos(angle) * 0.95 * steps);
                int endZ = (int) Math.round(mouthZ + Math.sin(angle) * 0.95 * steps);
                int endY = (int) Math.round(y - steps * 0.36);
                if (!hasNearbyCaveAir(column, endX, endY, endZ, 5)) {
                    continue;
                }
                for (int step = 0; step <= steps; step++) {
                    double t = step / (double) steps;
                    double radius = lerp(2.8, 1.55, t);
                    double verticalRadius = lerp(1.55, 1.10, t);
                    carveCaveEllipsoid(column, scratch, startX, startZ,
                        (int) Math.round(x), (int) Math.round(y), (int) Math.round(z),
                        radius, verticalRadius, radius * 0.86, true);
                    x += Math.cos(angle) * 0.95;
                    z += Math.sin(angle) * 0.95;
                    y -= lerp(0.18, 0.54, t);
                }
            }
        }
    }

    private boolean hasNearbyCaveAir(GeneratedChunkColumn column, int centerX, int centerY, int centerZ, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                for (int y = centerY - radius; y <= centerY + radius; y++) {
                    if (!GameConfig.isWorldYInside(y)) {
                        continue;
                    }
                    double dx = x - centerX;
                    double dy = (y - centerY) * 1.35;
                    double dz = z - centerZ;
                    if (dx * dx + dy * dy + dz * dz <= radius * radius && column.getBlock(x, y, z) == GameConfig.AIR) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void carveSurfaceFissures(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        long fissureSeed = mix64(seed ^ ((long) column.chunkX * 0x632BE59BD9B4E019L) ^ ((long) column.chunkZ * 0x85157AF5L));
        if (randomUnit(fissureSeed) > 0.035) {
            return;
        }

        int originX = startX + 3 + (int) (randomUnit(fissureSeed ^ 0xA11L) * 10.0);
        int originZ = startZ + 3 + (int) (randomUnit(fissureSeed ^ 0xB22L) * 10.0);
        int originIndex = localColumnIndex(startX, startZ, originX, originZ);
        if (originIndex < 0) {
            return;
        }
        Biome originBiome = BIOMES[scratch.biomeOrdinals[originIndex] & 0xFF];
        RiverSample originRiver = scratch.riverSamples[originIndex];
        if (isAquaticTerrain(scratch.terrainFlags[originIndex])
            || (originRiver != null && originRiver.mask > 0.10)
            || isForestLikeBiome(originBiome)
            || hasGenerationMask(scratch, startX, startZ, originX, originZ, (byte) (GENMASK_STRUCTURE | GENMASK_ROAD | GENMASK_WET_RIVER))) {
            return;
        }

        double angle = randomUnit(fissureSeed ^ 0xC33L) * Math.PI * 2.0;
        int length = 10 + (int) (randomUnit(fissureSeed ^ 0xD44L) * 14.0);
        int depth = 7 + (int) (randomUnit(fissureSeed ^ 0xE55L) * 12.0);
        for (int step = -length / 2; step <= length / 2; step++) {
            double centerX = originX + Math.cos(angle) * step;
            double centerZ = originZ + Math.sin(angle) * step;
            double width = 0.50 + randomUnit(fissureSeed ^ (step * 0x9E3779B97F4A7C15L)) * 0.65;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int worldX = (int) Math.round(centerX + dx);
                    int worldZ = (int) Math.round(centerZ + dz);
                    int index = localColumnIndex(startX, startZ, worldX, worldZ);
                    if (index < 0 || shouldProtectCarve(scratch, startX, startZ, worldX, scratch.surfaceHeights[index], worldZ)) {
                        continue;
                    }
                    RiverSample river = scratch.riverSamples[index];
                    if (isAquaticTerrain(scratch.terrainFlags[index]) || (river != null && river.mask > 0.16)) {
                        continue;
                    }
                    double alongNormal = Math.abs(dx * Math.sin(angle) - dz * Math.cos(angle));
                    if (alongNormal > width) {
                        continue;
                    }
                    int surfaceY = findSurface(column, worldX, worldZ);
                    int bottomY = Math.max(GameConfig.WORLD_MIN_Y + 6, surfaceY - depth);
                    for (int y = surfaceY; y >= bottomY; y--) {
                        if (surfaceY - y < 3 && alongNormal > width * 0.55) {
                            continue;
                        }
                        byte block = column.getBlock(worldX, y, worldZ);
                        if (canCarveCaveBlock(block) || canCarveCaveEntranceBlock(block, true)) {
                            column.setBlock(worldX, y, worldZ, GameConfig.AIR);
                        }
                    }
                }
            }
        }
    }

    private boolean isDryCaveMouth(int worldX, int worldZ, TerrainSample sample) {
        if (isAquaticTerrain(sample.terrainFlags) || sample.approximateSurfaceHeight <= GameConfig.SEA_LEVEL + 2) {
            return false;
        }
        double continentalness = sampleContinentalness(worldX, worldZ);
        RiverSample river = sampleRiver(worldX, worldZ, sample.approximateSurfaceHeight, continentalness, sampleLakeBasin(worldX, worldZ));
        return river == null || !river.active || river.mask < 0.20;
    }

    private void carveCavePillar(GeneratedChunkColumn column, int centerX, int centerY, int centerZ, double caveRadius) {
        int x = centerX + (int) Math.round(caveRadius * 0.45);
        int z = centerZ - (int) Math.round(caveRadius * 0.28);
        int bottom = Math.max(GameConfig.WORLD_MIN_Y + 5, centerY - 5);
        int top = Math.min(GameConfig.WORLD_MAX_Y - 3, centerY + 6);
        for (int y = bottom; y <= top; y++) {
            double radius = 0.72 + 0.18 * Math.sin(y * 0.41);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 0.25 || !isInColumn(column, x + dx, z + dz)) {
                        continue;
                    }
                    if (column.getBlock(x + dx, y, z + dz) == GameConfig.AIR) {
                        column.setBlock(x + dx, y, z + dz, baseStoneForY(y));
                    }
                }
            }
        }
    }

    private void carveVerticalCaveLink(GeneratedChunkColumn column, GenerationScratch scratch, int startX, int startZ,
                                       long caveSeed, int centerX, int centerY, int centerZ) {
        int topY = clamp(centerY + 10 + (int) (randomUnit(caveSeed ^ 0x7171L) * 14.0), centerY + 5, GameConfig.SEA_LEVEL - 6);
        int bottomY = clamp(centerY - 10 - (int) (randomUnit(caveSeed ^ 0x8181L) * 26.0), GameConfig.WORLD_MIN_Y + 8, centerY - 5);
        for (int y = bottomY; y <= topY; y++) {
            double radius = 0.82 + 0.22 * Math.sin(y * 0.31);
            carveCaveEllipsoid(column, scratch, startX, startZ, centerX, y, centerZ, radius, 0.72, radius, false);
        }
    }

    private void carveCaveRoom(GeneratedChunkColumn column, GenerationScratch scratch, int startX, int startZ,
                               int centerX, int centerY, int centerZ, double radius, double verticalRadius, boolean allowLarge) {
        double rx = radius;
        double rz = radius * (allowLarge ? 1.08 : 1.0);
        double ry = verticalRadius + radius * 0.16;
        carveCaveEllipsoid(column, scratch, startX, startZ, centerX, centerY, centerZ, rx, ry, rz, false);
    }

    private void carveCaveEllipsoid(GeneratedChunkColumn column, GenerationScratch scratch, int startX, int startZ,
                                    int centerX, int centerY, int centerZ,
                                    double radiusX, double radiusY, double radiusZ, boolean allowSurfaceBlocks) {
        int minX = (int) Math.floor(centerX - radiusX - 1.0);
        int maxX = (int) Math.ceil(centerX + radiusX + 1.0);
        int minY = (int) Math.floor(centerY - radiusY - 1.0);
        int maxY = (int) Math.ceil(centerY + radiusY + 1.0);
        int minZ = (int) Math.floor(centerZ - radiusZ - 1.0);
        int maxZ = (int) Math.ceil(centerZ + radiusZ + 1.0);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    if (!GameConfig.isWorldYInside(y) || y <= GameConfig.WORLD_MIN_Y + 4) {
                        continue;
                    }
                    double nx = (x - centerX) / radiusX;
                    double ny = (y - centerY) / radiusY;
                    double nz = (z - centerZ) / radiusZ;
                    if (nx * nx + ny * ny + nz * nz > 1.0) {
                        continue;
                    }
                    if (shouldProtectCarve(scratch, startX, startZ, x, y, z)) {
                        continue;
                    }
                    if ((!allowSurfaceBlocks || shouldProtectAquaticSurfaceEntrance(scratch, startX, startZ, x, y, z))
                        && shouldProtectSurfaceRoof(scratch, startX, startZ, x, y, z)) {
                        continue;
                    }
                    byte block = column.getBlock(x, y, z);
                    if (GameConfig.isLiquidBlock(block)) {
                        continue;
                    }
                    if (canCarveCaveBlock(block) || canCarveCaveEntranceBlock(block, allowSurfaceBlocks)) {
                        column.setBlock(x, y, z, GameConfig.AIR);
                    }
                }
            }
        }
    }

    private void populateAquaticPlants(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                byte terrainFlags = scratch.terrainFlags[index];
                RiverSample river = scratch.riverSamples[index];
                boolean riverWater = river != null && river.wet && river.mask > 0.48;
                if ((terrainFlags & (TERRAIN_OCEAN | TERRAIN_LAKE)) == 0 && !riverWater) {
                    continue;
                }

                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int floorY = findSurface(column, worldX, worldZ);
                int waterLevel = riverWater ? river.waterLevel : waterLevelForTerrain(scratch.surfaceHeights[index], terrainFlags);
                if (floorY < 2 || floorY + 2 > GameConfig.WORLD_MAX_Y || waterLevel - floorY < 2) {
                    continue;
                }

                byte floorBlock = column.getBlock(worldX, floorY, worldZ);
                if (!isAquaticPlantFloor(floorBlock)
                    || !GameConfig.isWaterBlock(column.getBlock(worldX, floorY + 1, worldZ))
                    || !GameConfig.isWaterBlock(column.getBlock(worldX, floorY + 2, worldZ))) {
                    continue;
                }

                double patch = climate01(fractalNoise((worldX + 1720.0) * 0.070, (worldZ - 1330.0) * 0.070, 2, 0.54));
                double detail = climate01(fractalNoise((worldX - 420.0) * 0.190, (worldZ + 860.0) * 0.190, 1, 0.50));
                boolean ocean = (terrainFlags & TERRAIN_OCEAN) != 0;
                double density = riverWater ? 0.84 : (ocean ? 0.48 : 0.66);
                if (ocean && waterLevel - floorY >= 5 && patch > 0.64 && detail > 0.38) {
                    int kelpHeight = 2 + (int) (randomUnit(mix64(seed ^ (worldX * 341873128712L) ^ (worldZ * 132897987541L))) * 4.0);
                    int topY = Math.min(waterLevel - 2, floorY + kelpHeight);
                    for (int y = floorY + 1; y <= topY; y++) {
                        if (GameConfig.isWaterBlock(column.getBlock(worldX, y, worldZ))) {
                            int segment = y == topY ? 2 : (y == floorY + 1 ? 0 : 1);
                            column.setTemplateBlock(worldX, y, worldZ, Blocks.withData(GameConfig.KELP, segment));
                        }
                    }
                    if (topY >= floorY + 1 && column.getBlock(worldX, topY + 1, worldZ) == GameConfig.AIR) {
                        column.setBlock(worldX, topY + 1, worldZ, GameConfig.WATER_SOURCE);
                    }
                } else if (patch > density && detail > 0.30) {
                    if (GameConfig.isWaterBlock(column.getBlock(worldX, floorY + 2, worldZ))) {
                        column.setBlock(worldX, floorY + 1, worldZ, GameConfig.SEAGRASS);
                    } else if (column.getBlock(worldX, floorY + 2, worldZ) == GameConfig.AIR && floorY + 2 <= waterLevel) {
                        column.setBlock(worldX, floorY + 1, worldZ, GameConfig.SEAGRASS);
                        column.setBlock(worldX, floorY + 2, worldZ, GameConfig.WATER_SOURCE);
                    }
                }
            }
        }
    }

    private void populateKarstLakes(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        int minCellX = Math.floorDiv(startX - 72, CAVE_SYSTEM_SIZE);
        int maxCellX = Math.floorDiv(startX + GameConfig.CHUNK_SIZE + 72, CAVE_SYSTEM_SIZE);
        int minCellZ = Math.floorDiv(startZ - 72, CAVE_SYSTEM_SIZE);
        int maxCellZ = Math.floorDiv(startZ + GameConfig.CHUNK_SIZE + 72, CAVE_SYSTEM_SIZE);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                long caveSeed = mix64(seed ^ ((long) cellX * 544910567L) ^ ((long) cellZ * 1301081L));
                if (randomUnit(caveSeed) > 0.66 || randomUnit(caveSeed ^ 0x5A5A5A5AL) >= 0.12) {
                    continue;
                }
                int centerX = cellX * CAVE_SYSTEM_SIZE + 14 + (int) (randomUnit(caveSeed ^ 0xCAFE1L) * 36.0);
                int centerZ = cellZ * CAVE_SYSTEM_SIZE + 14 + (int) (randomUnit(caveSeed ^ 0xCAFE2L) * 36.0);
                TerrainSample sample = terrainSampleAt(centerX, centerZ);
                if (isAquaticTerrain(sample.terrainFlags)) {
                    continue;
                }
                double layerRoll = randomUnit(caveSeed ^ 0xCAFE3L);
                int centerY = layerRoll < 0.58
                    ? sample.approximateSurfaceHeight - 10 - (int) Math.round(randomUnit(caveSeed ^ 0xCAFE4L) * 28.0)
                    : GameConfig.SEA_LEVEL - 24 - (int) Math.round(randomUnit(caveSeed ^ 0xCAFE5L) * 58.0);
                centerY = clamp(centerY, GameConfig.WORLD_MIN_Y + 13, Math.min(sample.approximateSurfaceHeight - 5, GameConfig.SEA_LEVEL + 16));

                boolean lavaLake = randomUnit(caveSeed ^ 0xDADAL) < 0.42;
                int lakeY = lavaLake
                    ? clamp(centerY - 12 - (int) (randomUnit(caveSeed ^ 0xBEEFL) * 20.0), GameConfig.WORLD_MIN_Y + 13, 8)
                    : clamp(centerY - 4 + (int) Math.round((randomUnit(caveSeed ^ 0xFACE1L) - 0.5) * 16.0), 14, GameConfig.SEA_LEVEL - 12);
                double radius = lavaLake
                    ? 3.8 + randomUnit(caveSeed ^ 0xFACE2L) * 2.4
                    : 4.4 + randomUnit(caveSeed ^ 0xFACE3L) * 3.0;
                placeKarstLake(column, startX, startZ, scratch, centerX, lakeY, centerZ, radius, lavaLake ? GameConfig.LAVA_SOURCE : GameConfig.WATER_SOURCE);
            }
        }
    }

    private void placeKarstLake(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch,
                                int centerX, int lakeY, int centerZ, double radius, byte fluidBlock) {
        int minX = (int) Math.floor(centerX - radius - 1.0);
        int maxX = (int) Math.ceil(centerX + radius + 1.0);
        int minZ = (int) Math.floor(centerZ - radius - 1.0);
        int maxZ = (int) Math.ceil(centerZ + radius + 1.0);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                double nx = (x - centerX) / radius;
                double nz = (z - centerZ) / (radius * 0.82);
                double shape = nx * nx + nz * nz;
                if (shape > 1.0) {
                    continue;
                }
                if (shouldProtectCarve(scratch, startX, startZ, x, lakeY, z)) {
                    continue;
                }
                int floorY = lakeY - 1;
                byte floor = column.getBlock(x, floorY, z);
                if (canReplaceCavePatchBlock(floor)) {
                    column.setBlock(x, floorY, z, fluidBlock == GameConfig.LAVA_SOURCE ? GameConfig.COBBLESTONE : GameConfig.GRAVEL);
                }
                for (int y = lakeY; y <= lakeY + 1; y++) {
                    if (!GameConfig.isWorldYInside(y)) {
                        continue;
                    }
                    byte block = column.getBlock(x, y, z);
                    if (block == GameConfig.AIR || canCarveCaveBlock(block)) {
                        column.setNaturalFluid(x, y, z, fluidBlock);
                    }
                }
            }
        }
    }

    private void populateCaveFluids(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                byte terrainFlags = scratch.terrainFlags[index];
                if (isAquaticTerrain(terrainFlags)) {
                    continue;
                }

                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceHeight = scratch.surfaceHeights[index];
                int topY = Math.min(surfaceHeight - 8, GameConfig.SEA_LEVEL - 4);
                for (int worldY = 6; worldY <= topY; worldY++) {
                    if (shouldProtectCarve(scratch, startX, startZ, worldX, worldY, worldZ)) {
                        continue;
                    }
                    if (column.getBlock(worldX, worldY, worldZ) != GameConfig.AIR
                        || !isSolidCaveFloor(column.getBlock(worldX, worldY - 1, worldZ))
                        || !hasCaveAirSpace(column, worldX, worldY, worldZ)) {
                        continue;
                    }

                    int lavaY = 10 + (int) Math.round(climate01(fractalNoise((worldX - 730.0) * 0.010, (worldZ + 410.0) * 0.010, 2, 0.54)) * 8.0);
                    double lavaPool = climate01(fractalNoise((worldX + 180.0) * 0.055, (worldZ - 960.0) * 0.055, 2, 0.52));
                    if (worldY <= 22 && Math.abs(worldY - lavaY) <= 1 && lavaPool > 0.84) {
                        column.setNaturalFluid(worldX, worldY, worldZ, GameConfig.LAVA_SOURCE);
                        continue;
                    }

                    int waterY = GameConfig.SEA_LEVEL - 22
                        + (int) Math.round(fractalNoise((worldX + 1240.0) * 0.011, (worldZ - 640.0) * 0.011, 2, 0.55) * 5.0);
                    double waterPool = climate01(fractalNoise((worldX - 520.0) * 0.047, (worldZ + 880.0) * 0.047, 2, 0.56));
                    if (worldY >= 20 && Math.abs(worldY - waterY) <= 1 && waterPool > 0.88) {
                        column.setNaturalFluid(worldX, worldY, worldZ, GameConfig.WATER_SOURCE);
                        continue;
                    }

                    if (hasCaveWall(column, worldX, worldY, worldZ)) {
                        double sourceNoise = climate01(fractalNoise3((worldX + 2600.0) * 0.061, (worldY - 35.0) * 0.043, (worldZ - 2200.0) * 0.061, 2, 0.52));
                        if (worldY > 24 && worldY < GameConfig.SEA_LEVEL - 4 && sourceNoise > 0.991) {
                            column.setNaturalFluid(worldX, worldY, worldZ, GameConfig.WATER_SOURCE);
                        } else if (worldY < 16 && sourceNoise > 0.995) {
                            column.setNaturalFluid(worldX, worldY, worldZ, GameConfig.LAVA_SOURCE);
                        }
                    }
                }
            }
        }
    }

    private boolean hasCaveWall(GeneratedChunkColumn column, int worldX, int worldY, int worldZ) {
        return isSolidCaveFloor(column.getBlock(worldX + 1, worldY, worldZ))
            || isSolidCaveFloor(column.getBlock(worldX - 1, worldY, worldZ))
            || isSolidCaveFloor(column.getBlock(worldX, worldY, worldZ + 1))
            || isSolidCaveFloor(column.getBlock(worldX, worldY, worldZ - 1));
    }

    private void populateCavePatches(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int topY = Math.min(scratch.surfaceHeights[index] - 4, GameConfig.SEA_LEVEL + 6);
                for (int worldY = GameConfig.WORLD_MIN_Y + 8; worldY <= topY; worldY++) {
                    if (shouldProtectCarve(scratch, startX, startZ, worldX, worldY, worldZ)) {
                        continue;
                    }
                    if (column.getBlock(worldX, worldY, worldZ) != GameConfig.AIR) {
                        continue;
                    }
                    byte floor = column.getBlock(worldX, worldY - 1, worldZ);
                    if (!canReplaceCavePatchBlock(floor) || !hasCaveAirSpace(column, worldX, worldY, worldZ)) {
                        continue;
                    }
                    double patchNoise = climate01(fractalNoise3((worldX + 3320.0) * 0.045, (worldY - 18.0) * 0.052, (worldZ - 2810.0) * 0.045, 2, 0.54));
                    if (patchNoise > 0.88) {
                        byte patchBlock = worldY < GameConfig.SEA_LEVEL - 34 && patchNoise > 0.965
                            ? GameConfig.CLAY
                            : (patchNoise > 0.93 ? GameConfig.DIRT : GameConfig.GRAVEL);
                        column.setBlock(worldX, worldY - 1, worldZ, patchBlock);
                    }
                }
            }
        }
    }

    private boolean canReplaceCavePatchBlock(byte block) {
        return block == GameConfig.STONE
            || block == GameConfig.COBBLESTONE
            || block == GameConfig.DEEPSLATE;
    }

    private void floodBelowSeaLevel(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceHeight = findSurface(column, worldX, worldZ);
                byte terrainFlags = scratch.terrainFlags[index];
                RiverSample river = scratch.riverSamples[index];
                if (river != null && river.wet) {
                    fillRiverWaterColumn(column, worldX, worldZ, surfaceHeight, river);
                    continue;
                }

                int waterLevel = waterLevelForTerrain(surfaceHeight, terrainFlags);
                if (!shouldFillWithSeaWater(surfaceHeight, waterLevel, terrainFlags)) {
                    continue;
                }

                for (int worldY = Math.min(waterLevel, GameConfig.WORLD_MAX_Y); worldY > GameConfig.WORLD_MIN_Y; worldY--) {
                    byte block = column.getBlock(worldX, worldY, worldZ);
                    if (block == GameConfig.AIR) {
                        column.setNaturalFluid(worldX, worldY, worldZ, GameConfig.WATER_SOURCE);
                        continue;
                    }
                    break;
                }
            }
        }
    }

    private boolean isBeachSurface(int surfaceHeight, byte terrainFlags) {
        return (terrainFlags & TERRAIN_BEACH) != 0;
    }

    private void populateUnderwaterSedimentVeins(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        populateUnderwaterSedimentVein(column, startX, startZ, scratch, GameConfig.SAND, 0x5A4D51EEL, 0.031, 0.55, 2);
        populateUnderwaterSedimentVein(column, startX, startZ, scratch, GameConfig.GRAVEL, 0x6A7E11E7L, 0.036, 0.62, 2);
        populateUnderwaterSedimentVein(column, startX, startZ, scratch, GameConfig.CLAY, 0xC1A7B10BL, 0.041, 0.68, 1);
    }

    private void populateUnderwaterSedimentVein(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch,
                                                byte depositBlock, long salt, double scale, double threshold, int depth) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                byte terrainFlags = scratch.terrainFlags[index];
                if ((terrainFlags & (TERRAIN_OCEAN | TERRAIN_LAKE)) == 0 || isBeachSurface(scratch.surfaceHeights[index], terrainFlags)) {
                    continue;
                }

                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int floorY = findSurface(column, worldX, worldZ);
                int waterDepth = GameConfig.SEA_LEVEL - floorY;
                if (waterDepth < 3
                    || floorY <= GameConfig.WORLD_MIN_Y
                    || floorY + 1 > GameConfig.WORLD_MAX_Y
                    || !isUnderwaterDepositBase(column.getBlock(worldX, floorY, worldZ))
                    || !GameConfig.isWaterBlock(column.getBlock(worldX, floorY + 1, worldZ))) {
                    continue;
                }

                double vein = climate01(fractalNoise((worldX + (salt & 1023L)) * scale, (worldZ - ((salt >>> 10) & 1023L)) * scale, 3, 0.56));
                if (vein < threshold) {
                    continue;
                }

                int bottomY = Math.max(GameConfig.WORLD_MIN_Y, floorY - depth + 1);
                for (int y = floorY; y >= bottomY; y--) {
                    if (isUnderwaterDepositBase(column.getBlock(worldX, y, worldZ))) {
                        column.setBlock(worldX, y, worldZ, depositBlock);
                    }
                }
            }
        }
    }

    private boolean isUnderwaterDepositBase(byte block) {
        return block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.CLAY
            || block == GameConfig.DIRT
            || block == GameConfig.STONE
            || block == GameConfig.COBBLESTONE;
    }

    private void fillRiverWaterColumn(GeneratedChunkColumn column, int worldX, int worldZ, int surfaceHeight, RiverSample river) {
        if (!isRiverWaterCore(river)) {
            return;
        }

        int waterTop = river.waterLevel;
        int bedY = Math.min(river.bedLevel, waterTop - 1);
        if (surfaceHeight - waterTop > 2 || bedY <= GameConfig.WORLD_MIN_Y || waterTop > GameConfig.WORLD_MAX_Y) {
            return;
        }
        if (!isRiverWaterColumnOpen(column, worldX, worldZ, surfaceHeight, waterTop)) {
            return;
        }
        for (int y = waterTop + 1; y <= surfaceHeight; y++) {
            byte block = column.getBlock(worldX, y, worldZ);
            if (block != GameConfig.AIR && !GameConfig.isWaterBlock(block)) {
                column.setBlock(worldX, y, worldZ, GameConfig.AIR);
            }
        }
        column.setBlock(worldX, bedY, worldZ, riverBedBlock(worldX, worldZ, river));
        for (int y = bedY + 1; y <= waterTop; y++) {
            column.setNaturalFluid(worldX, y, worldZ, GameConfig.WATER_SOURCE);
        }
    }

    private boolean isRiverWaterCore(RiverSample river) {
        return river != null
            && river.wet
            && river.mask >= 0.42
            && river.distanceToCenter <= river.width * 0.88;
    }

    private byte riverBedBlock(int worldX, int worldZ, RiverSample river) {
        double center = 1.0 - smoothstep(0.0, river.width, river.distanceToCenter);
        double mix = climate01(fractalNoise((worldX + 430.0) * 0.18, (worldZ - 770.0) * 0.18, 2, 0.50));
        if (center > 0.72) {
            if (mix < 0.42) {
                return GameConfig.GRAVEL;
            }
            if (mix < 0.78) {
                return GameConfig.SAND;
            }
            return GameConfig.CLAY;
        }
        if (mix < 0.35) {
            return GameConfig.GRAVEL;
        }
        if (mix < 0.70) {
            return GameConfig.SAND;
        }
        return GameConfig.DIRT;
    }

    private boolean isRiverWaterColumnOpen(GeneratedChunkColumn column, int worldX, int worldZ, int surfaceHeight, int waterLevel) {
        for (int y = surfaceHeight + 1; y <= Math.min(waterLevel + 1, GameConfig.WORLD_MAX_Y); y++) {
            byte block = column.getBlock(worldX, y, worldZ);
            if (block != GameConfig.AIR && !isPlant(block) && !GameConfig.isWaterBlock(block)) {
                return false;
            }
        }
        return true;
    }

    private void populateDeepLavaPockets(GeneratedChunkColumn column, int startX, int startZ) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                for (int worldY = GameConfig.WORLD_MIN_Y + 6; worldY <= -48; worldY++) {
                    if (column.getBlock(worldX, worldY, worldZ) != GameConfig.AIR
                        || !isSolidCaveFloor(column.getBlock(worldX, worldY - 1, worldZ))
                        || !hasCaveAirSpace(column, worldX, worldY, worldZ)) {
                        continue;
                    }
                    double lavaNoise = climate01(fractalNoise3((worldX + 650.0) * 0.045, (worldY - 20.0) * 0.060, (worldZ - 250.0) * 0.045, 2, 0.55));
                    if (lavaNoise > 0.935) {
                        column.setNaturalFluid(worldX, worldY, worldZ, GameConfig.LAVA_SOURCE);
                    }
                }
            }
        }
    }

    private void populateSmallSurfacePools(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        long chunkSeed = mix64(seed ^ ((long) column.chunkX * 341873128712L) ^ ((long) column.chunkZ * 132897987541L));
        if (randomUnit(chunkSeed) < 0.045) {
            tryPlaceSurfacePool(column, startX, startZ, scratch, chunkSeed, GameConfig.WATER_SOURCE);
        }
        long lavaSeed = mix64(chunkSeed ^ 0x6A09E667F3BCC909L);
        if (randomUnit(lavaSeed) < 0.006) {
            tryPlaceSurfacePool(column, startX, startZ, scratch, lavaSeed, GameConfig.LAVA_SOURCE);
        }
    }

    private void tryPlaceSurfacePool(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch,
                                     long noiseSeed, byte fluidBlock) {
        int centerLocalX = 4 + (int) ((noiseSeed >>> 8) & 7L);
        int centerLocalZ = 4 + (int) ((noiseSeed >>> 13) & 7L);
        int index = columnIndex(centerLocalX, centerLocalZ);
        Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
        byte terrainFlags = scratch.terrainFlags[index];
        int centerSurfaceY = findSurface(column, startX + centerLocalX, startZ + centerLocalZ);

        if (centerSurfaceY <= GameConfig.SEA_LEVEL + 2
            || centerSurfaceY + 2 > GameConfig.WORLD_MAX_Y
            || isAquaticTerrain(terrainFlags)
            || (scratch.generationMasks[index] & (GENMASK_STRUCTURE | GENMASK_WET_RIVER)) != 0
            || (scratch.riverSamples[index] != null && scratch.riverSamples[index].active)
            || shouldPlaceSnow(startX + centerLocalX, startZ + centerLocalZ, biome)) {
            return;
        }
        if (fluidBlock == GameConfig.LAVA_SOURCE && (biome == Biome.RAINFOREST || biome == Biome.SWAMPLAND || biome.snowCovered)) {
            return;
        }

        int radius = fluidBlock == GameConfig.LAVA_SOURCE ? 3 : 4 + (int) ((noiseSeed >>> 19) & 1L);
        int fluidY = centerSurfaceY - 1;
        int floorY = fluidY - (fluidBlock == GameConfig.LAVA_SOURCE ? 1 : 2);
        byte floorBlock = fluidBlock == GameConfig.LAVA_SOURCE
            ? GameConfig.COBBLESTONE
            : aquaticFloorBlock(startX + centerLocalX, startZ + centerLocalZ, (byte) (TERRAIN_LAKE | TERRAIN_BEACH));
        if (!isSurfacePoolFootprintAllowed(column, startX, startZ, scratch, centerLocalX, centerLocalZ, centerSurfaceY, radius, fluidBlock)) {
            return;
        }

        for (int offsetX = -radius - 1; offsetX <= radius + 1; offsetX++) {
            for (int offsetZ = -radius - 1; offsetZ <= radius + 1; offsetZ++) {
                int localX = centerLocalX + offsetX;
                int localZ = centerLocalZ + offsetZ;
                if (localX < 1 || localX >= GameConfig.CHUNK_SIZE - 1 || localZ < 1 || localZ >= GameConfig.CHUNK_SIZE - 1) {
                    continue;
                }
                double shapeNoise = randomUnit(mix64(noiseSeed ^ ((long) offsetX * 73428767L) ^ ((long) offsetZ * 912931L))) * 0.45;
                double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ) + shapeNoise;
                if (distance > radius + 0.45) {
                    continue;
                }

                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                if (hasGenerationMask(scratch, startX, startZ, worldX, worldZ, (byte) (GENMASK_STRUCTURE | GENMASK_WET_RIVER))) {
                    continue;
                }
                int surfaceY = findSurface(column, worldX, worldZ);
                if (surfaceY < fluidY || surfaceY > centerSurfaceY + 2) {
                    continue;
                }

                for (int y = floorY; y <= surfaceY + 1; y++) {
                    if (y < floorY || y > GameConfig.WORLD_MAX_Y) {
                        continue;
                    }
                    if (y < fluidY) {
                        column.setBlock(worldX, y, worldZ, floorBlock);
                    } else if (y == fluidY) {
                        column.setNaturalFluid(worldX, y, worldZ, fluidBlock);
                    } else {
                        column.setBlock(worldX, y, worldZ, GameConfig.AIR);
                    }
                }
            }
        }
    }

    private boolean isSurfacePoolFootprintAllowed(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch,
                                                  int centerLocalX, int centerLocalZ, int centerSurfaceY, int radius, byte fluidBlock) {
        int minHeight = centerSurfaceY;
        int maxHeight = centerSurfaceY;
        for (int offsetX = -radius - 2; offsetX <= radius + 2; offsetX++) {
            for (int offsetZ = -radius - 2; offsetZ <= radius + 2; offsetZ++) {
                int localX = centerLocalX + offsetX;
                int localZ = centerLocalZ + offsetZ;
                if (localX < 1 || localX >= GameConfig.CHUNK_SIZE - 1 || localZ < 1 || localZ >= GameConfig.CHUNK_SIZE - 1) {
                    return false;
                }
                double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
                if (distance > radius + 2.0) {
                    continue;
                }
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                Biome footprintBiome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                if (isMountainBiome(footprintBiome) || isAquaticTerrain(scratch.terrainFlags[index])) {
                    return false;
                }
                if (hasGenerationMask(scratch, startX, startZ, worldX, worldZ, (byte) (GENMASK_STRUCTURE | GENMASK_WET_RIVER))) {
                    return false;
                }
                int surfaceY = findSurface(column, worldX, worldZ);
                minHeight = Math.min(minHeight, surfaceY);
                maxHeight = Math.max(maxHeight, surfaceY);
                if (surfaceY > GameConfig.SEA_LEVEL + (fluidBlock == GameConfig.LAVA_SOURCE ? 4 : 6)) {
                    return false;
                }
            }
        }
        int maxDelta = fluidBlock == GameConfig.LAVA_SOURCE ? 3 : 4;
        return maxHeight - minHeight <= maxDelta;
    }

    private void populateOreClusters(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceHeight = scratch.surfaceHeights[index];
                for (int worldY = GameConfig.WORLD_MIN_Y + 6; worldY < Math.min(surfaceHeight, GameConfig.WORLD_MAX_Y - 1); worldY += 2) {
                    byte oreHost = column.getBlock(worldX, worldY, worldZ);
                    if (oreHost != GameConfig.STONE && oreHost != GameConfig.DEEPSLATE && oreHost != GameConfig.COBBLESTONE) {
                        continue;
                    }

                    int depthBelowSurface = surfaceHeight - worldY;
                    if (depthBelowSurface > 10) {
                        double patchNoise = climate01(fractalNoise3((worldX + 1550.0) * 0.046, (worldY + 40.0) * 0.030, (worldZ - 920.0) * 0.046, 2, 0.54));
                        if (patchNoise > 0.982) {
                            column.setBlock(worldX, worldY, worldZ, worldY < 0 && patchNoise > 0.994 ? GameConfig.CLAY : GameConfig.GRAVEL);
                            continue;
                        }
                        if (worldY > -18 && patchNoise < 0.012) {
                            column.setBlock(worldX, worldY, worldZ, GameConfig.DIRT);
                            continue;
                        }
                    }

                    double coalCluster = Math.abs(fractalNoise3((worldX - 90.0) * 0.115, worldY * 0.105, (worldZ + 150.0) * 0.115, 2, 0.58));
                    double ironCluster = Math.abs(fractalNoise3((worldX + 40.0) * 0.138, (worldY - 18.0) * 0.126, (worldZ - 65.0) * 0.138, 2, 0.55));
                    double diamondCluster = Math.abs(fractalNoise3((worldX - 170.0) * 0.185, (worldY + 11.0) * 0.192, (worldZ + 210.0) * 0.185, 2, 0.53));

                    if (worldY < 18 && diamondCluster > (worldY < -32 ? 0.755 : 0.805)) {
                        column.setBlock(worldX, worldY, worldZ, oreForHost(oreHost, GameConfig.DIAMOND_ORE));
                    } else if (worldY < 54 && ironCluster > (worldY < 0 ? 0.735 : 0.785)) {
                        column.setBlock(worldX, worldY, worldZ, oreForHost(oreHost, GameConfig.IRON_ORE));
                    } else if (worldY < 72 && coalCluster > 0.710) {
                        column.setBlock(worldX, worldY, worldZ, oreForHost(oreHost, GameConfig.COAL_ORE));
                    }
                }
            }
        }
    }

    private void populateCaveWallOres(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceHeight = scratch.surfaceHeights[index];
                int maxY = Math.min(surfaceHeight - 5, GameConfig.SEA_LEVEL + 10);
                for (int worldY = GameConfig.WORLD_MIN_Y + 8; worldY <= maxY; worldY += 2) {
                    byte host = column.getBlock(worldX, worldY, worldZ);
                    if (host != GameConfig.STONE && host != GameConfig.DEEPSLATE && host != GameConfig.COBBLESTONE) {
                        continue;
                    }
                    if (!hasAdjacentCaveAir(column, worldX, worldY, worldZ)) {
                        continue;
                    }
                    double seam = climate01(fractalNoise3((worldX + 3100.0) * 0.165, (worldY - 25.0) * 0.122, (worldZ - 2700.0) * 0.165, 2, 0.55));
                    if (worldY < 12 && seam > 0.895) {
                        column.setBlock(worldX, worldY, worldZ, oreForHost(host, GameConfig.DIAMOND_ORE));
                    } else if (worldY < 56 && seam > 0.760) {
                        column.setBlock(worldX, worldY, worldZ, oreForHost(host, GameConfig.IRON_ORE));
                    } else if (worldY < 82 && seam < 0.245) {
                        column.setBlock(worldX, worldY, worldZ, oreForHost(host, GameConfig.COAL_ORE));
                    }
                }
            }
        }
    }

    private byte oreForHost(byte host, byte normalOre) {
        if (host != GameConfig.DEEPSLATE) {
            return normalOre;
        }
        if (normalOre == GameConfig.IRON_ORE) {
            return GameConfig.DEEPSLATE_IRON_ORE;
        }
        if (normalOre == GameConfig.DIAMOND_ORE) {
            return GameConfig.DEEPSLATE_DIAMOND_ORE;
        }
        if (normalOre == GameConfig.COAL_ORE) {
            return GameConfig.DEEPSLATE_COAL_ORE;
        }
        return normalOre;
    }

    private boolean hasAdjacentCaveAir(GeneratedChunkColumn column, int worldX, int worldY, int worldZ) {
        return column.getBlock(worldX + 1, worldY, worldZ) == GameConfig.AIR
            || column.getBlock(worldX - 1, worldY, worldZ) == GameConfig.AIR
            || column.getBlock(worldX, worldY + 1, worldZ) == GameConfig.AIR
            || column.getBlock(worldX, worldY - 1, worldZ) == GameConfig.AIR
            || column.getBlock(worldX, worldY, worldZ + 1) == GameConfig.AIR
            || column.getBlock(worldX, worldY, worldZ - 1) == GameConfig.AIR;
    }

    private void populateSurface(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                byte terrainFlags = scratch.terrainFlags[index];
                RiverSample river = scratch.riverSamples[index];
                int surfaceY = findSurface(column, worldX, worldZ);
                if (surfaceY <= GameConfig.WORLD_MIN_Y || surfaceY + 1 > GameConfig.WORLD_MAX_Y) {
                    continue;
                }
                byte surfaceBlock = column.getBlock(worldX, surfaceY, worldZ);
                if (column.getBlock(worldX, surfaceY + 1, worldZ) != GameConfig.AIR
                    || isAquaticTerrain(terrainFlags)
                    || (river != null && river.active && river.mask > 0.30)
                    || surfaceY <= GameConfig.SEA_LEVEL) {
                    continue;
                }

                long noiseSeed = mix64(seed ^ (((long) worldX) << 32) ^ (worldZ & 0xFFFFFFFFL));
                if (biome == Biome.DESERT && surfaceBlock == GameConfig.SAND) {
                    if (randomUnit(noiseSeed) < 0.052) {
                        tryPlaceCactus(column, worldX, surfaceY + 1, worldZ, noiseSeed);
                    } else if (randomUnit(noiseSeed ^ 0x5151L) < 0.070) {
                        column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.DEAD_BUSH);
                    }
                    continue;
                }

                if (surfaceBlock != GameConfig.GRASS) {
                    if (shouldPlaceSnow(worldX, worldZ, biome) && canSupportSnow(surfaceBlock)) {
                        column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.SNOW_LAYER);
                    }
                    continue;
                }

                double treeRoll = randomUnit(noiseSeed);
                if (biomeSupportsTrees(biome) && treeRoll < treeChanceForBiome(biome)) {
                    if (biome == Biome.BIRCH_FOREST) {
                        tryPlaceBirchTree(column, worldX, worldZ, startX, startZ, noiseSeed);
                    } else if (biome.pineTrees) {
                        tryPlacePineTree(column, worldX, worldZ, startX, startZ, noiseSeed);
                    } else {
                        tryPlaceTree(column, worldX, worldZ, startX, startZ, noiseSeed);
                    }
                    continue;
                }

                if (shouldPlaceSnow(worldX, worldZ, biome)) {
                    column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.SNOW_LAYER);
                    continue;
                }

                double floraNoise = fractalNoise((worldX + 120.0) * 0.120, (worldZ - 340.0) * 0.120, 2, 0.55);
                if ((biome == Biome.FOREST
                    || biome == Biome.BIRCH_FOREST
                    || biome == Biome.DARK_FOREST
                    || biome == Biome.JUNGLE
                    || biome == Biome.RAINFOREST
                    || biome == Biome.SEASONAL_FOREST
                    || biome == Biome.WOODED_HILLS) && floraNoise > 0.38) {
                    column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.TALL_GRASS);
                } else if ((biome == Biome.PLAINS || biome == Biome.SAVANNA || biome == Biome.MEADOW) && floraNoise > 0.20) {
                    column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.TALL_GRASS);
                } else if ((biome == Biome.MEADOW && floraNoise < -0.28) || floraNoise < -0.46) {
                    column.setBlock(worldX, surfaceY + 1, worldZ,
                        ((noiseSeed >>> 5) & 1L) == 0L ? GameConfig.RED_FLOWER : GameConfig.YELLOW_FLOWER);
                }
            }
        }
    }

    private void populateDesertOases(GeneratedChunkColumn column, int startX, int startZ) {
        long chunkSeed = mix64(seed ^ 0x0A51E5L ^ ((long) column.chunkX * 91815541L) ^ ((long) column.chunkZ * 19281131L));
        if (randomUnit(chunkSeed) > 0.035) {
            return;
        }
        int centerX = startX + 4 + (int) (randomUnit(chunkSeed ^ 0x45L) * 8.0);
        int centerZ = startZ + 4 + (int) (randomUnit(chunkSeed ^ 0x91L) * 8.0);
        if (sampleBiome(centerX, centerZ) != Biome.DESERT) {
            return;
        }
        int centerY = findSurface(column, centerX, centerZ);
        if (centerY <= GameConfig.SEA_LEVEL || centerY >= GameConfig.WORLD_MAX_Y - 3) {
            return;
        }
        int radius = 4 + (int) (randomUnit(chunkSeed ^ 0xD3L) * 2.0);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (!isInColumn(column, x, z)) {
                    continue;
                }
                double dist = Math.sqrt(dx * dx + dz * dz);
                double edgeNoise = fractalNoise((x + 200.0) * 0.24, (z - 130.0) * 0.24, 2, 0.55);
                if (dist > radius + edgeNoise * 0.8) {
                    continue;
                }
                int y = findSurface(column, x, z);
                if (dist < radius * 0.45) {
                    column.setBlock(x, y, z, GameConfig.SAND);
                    column.setBlock(x, y + 1, z, GameConfig.WATER_SOURCE);
                } else if (dist < radius * 0.75) {
                    column.setBlock(x, y, z, GameConfig.GRASS);
                    if (column.getBlock(x, y + 1, z) == GameConfig.AIR && randomUnit(chunkSeed ^ ((long) dx << 32) ^ dz) < 0.40) {
                        column.setBlock(x, y + 1, z, GameConfig.TALL_GRASS);
                    }
                } else {
                    column.setBlock(x, y, z, GameConfig.SAND);
                    if (column.getBlock(x, y + 1, z) == GameConfig.AIR && randomUnit(chunkSeed ^ ((long) x * 31L) ^ z) < 0.18) {
                        column.setBlock(x, y + 1, z, GameConfig.DEAD_BUSH);
                    }
                }
            }
        }
    }

    private void enforceShoreSand(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = findSurface(column, worldX, worldZ);
                if (!isBeachSurface(surfaceY, scratch.terrainFlags[index])) {
                    continue;
                }

                int beachSurfaceY = Math.max(surfaceY, GameConfig.SEA_LEVEL);
                if (surfaceY < beachSurfaceY) {
                    for (int y = surfaceY + 1; y <= beachSurfaceY; y++) {
                        byte block = column.getBlock(worldX, y, worldZ);
                        if (block == GameConfig.AIR || GameConfig.isLiquidBlock(block) || isPlant(block)) {
                            column.setBlock(worldX, y, worldZ, GameConfig.SAND);
                        }
                    }
                }
                int bottomY = Math.max(GameConfig.WORLD_MIN_Y, beachSurfaceY - OCEAN_SAND_DEPTH);
                for (int y = beachSurfaceY; y >= bottomY; y--) {
                    byte block = column.getBlock(worldX, y, worldZ);
                    if (isNaturalSurfaceBlock(block)) {
                        column.setBlock(worldX, y, worldZ, GameConfig.SAND);
                    }
                }
            }
        }
    }

    private boolean isNaturalSurfaceBlock(byte block) {
        return block == GameConfig.GRASS
            || block == GameConfig.DIRT
            || block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.CLAY
            || block == GameConfig.STONE
            || block == GameConfig.COBBLESTONE;
    }

    private boolean tryPlaceTree(GeneratedChunkColumn column, int worldX, int worldZ, int startX, int startZ, long noiseSeed) {
        if (worldX < startX + 2 || worldX > startX + GameConfig.CHUNK_SIZE - 3
            || worldZ < startZ + 2 || worldZ > startZ + GameConfig.CHUNK_SIZE - 3) {
            return false;
        }

        int surfaceY = findSurface(column, worldX, worldZ);
        if (surfaceY < GameConfig.SEA_LEVEL + 1 || surfaceY + 7 > GameConfig.WORLD_MAX_Y) {
            return false;
        }
        if (column.getBlock(worldX, surfaceY, worldZ) != GameConfig.GRASS) {
            return false;
        }

        int trunkBaseY = surfaceY + 1;
        int trunkHeight = 4 + (int) ((noiseSeed >>> 8) & 1L);
        for (int y = trunkBaseY; y <= trunkBaseY + trunkHeight + 2; y++) {
            int radius = y >= trunkBaseY + trunkHeight - 1 ? 2 : 0;
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    byte existing = column.getBlock(worldX + offsetX, y, worldZ + offsetZ);
                    if (existing != GameConfig.AIR && !isPlant(existing)) {
                        return false;
                    }
                }
            }
        }

        for (int y = 0; y < trunkHeight; y++) {
            column.setBlock(worldX, trunkBaseY + y, worldZ, GameConfig.OAK_LOG);
        }
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 1, worldZ, 2, false, GameConfig.OAK_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight, worldZ, 2, true, GameConfig.OAK_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight + 1, worldZ, 1, true, GameConfig.OAK_LEAVES);
        column.setBlock(worldX, trunkBaseY + trunkHeight + 1, worldZ, GameConfig.OAK_LEAVES);
        return true;
    }

    private boolean tryPlacePineTree(GeneratedChunkColumn column, int worldX, int worldZ, int startX, int startZ, long noiseSeed) {
        if (worldX < startX + 2 || worldX > startX + GameConfig.CHUNK_SIZE - 3
            || worldZ < startZ + 2 || worldZ > startZ + GameConfig.CHUNK_SIZE - 3) {
            return false;
        }

        int surfaceY = findSurface(column, worldX, worldZ);
        if (surfaceY < GameConfig.SEA_LEVEL + 1 || surfaceY + 9 > GameConfig.WORLD_MAX_Y) {
            return false;
        }
        if (column.getBlock(worldX, surfaceY, worldZ) != GameConfig.GRASS) {
            return false;
        }

        int trunkBaseY = surfaceY + 1;
        int trunkHeight = 6 + (int) ((noiseSeed >>> 8) & 1L);
        for (int y = trunkBaseY; y <= trunkBaseY + trunkHeight + 1; y++) {
            int radius = y >= trunkBaseY + 2 ? 2 : 0;
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    byte existing = column.getBlock(worldX + offsetX, y, worldZ + offsetZ);
                    if (existing != GameConfig.AIR && !isPlant(existing)) {
                        return false;
                    }
                }
            }
        }

        for (int y = 0; y < trunkHeight; y++) {
            column.setBlock(worldX, trunkBaseY + y, worldZ, GameConfig.PINE_LOG);
        }
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 4, worldZ, 2, true, GameConfig.PINE_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 3, worldZ, 1, false, GameConfig.PINE_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 2, worldZ, 2, true, GameConfig.PINE_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 1, worldZ, 1, false, GameConfig.PINE_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight, worldZ, 1, true, GameConfig.PINE_LEAVES);
        column.setBlock(worldX, trunkBaseY + trunkHeight + 1, worldZ, GameConfig.PINE_LEAVES);
        return true;
    }

    private boolean tryPlaceBirchTree(GeneratedChunkColumn column, int worldX, int worldZ, int startX, int startZ, long noiseSeed) {
        if (worldX < startX + 2 || worldX > startX + GameConfig.CHUNK_SIZE - 3
            || worldZ < startZ + 2 || worldZ > startZ + GameConfig.CHUNK_SIZE - 3) {
            return false;
        }

        int surfaceY = findSurface(column, worldX, worldZ);
        if (surfaceY < GameConfig.SEA_LEVEL + 1 || surfaceY + 8 > GameConfig.WORLD_MAX_Y) {
            return false;
        }
        if (column.getBlock(worldX, surfaceY, worldZ) != GameConfig.GRASS) {
            return false;
        }

        int trunkBaseY = surfaceY + 1;
        int trunkHeight = 5 + (int) ((noiseSeed >>> 8) & 1L);
        for (int y = trunkBaseY; y <= trunkBaseY + trunkHeight + 2; y++) {
            int radius = y >= trunkBaseY + trunkHeight - 2 ? 2 : 0;
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    byte existing = column.getBlock(worldX + offsetX, y, worldZ + offsetZ);
                    if (existing != GameConfig.AIR && !isPlant(existing)) {
                        return false;
                    }
                }
            }
        }

        for (int y = 0; y < trunkHeight; y++) {
            column.setBlock(worldX, trunkBaseY + y, worldZ, GameConfig.BIRCH_LOG);
        }
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 2, worldZ, 2, true, GameConfig.BIRCH_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight - 1, worldZ, 2, false, GameConfig.BIRCH_LEAVES);
        placeLeavesLayer(column, worldX, trunkBaseY + trunkHeight, worldZ, 1, true, GameConfig.BIRCH_LEAVES);
        column.setBlock(worldX, trunkBaseY + trunkHeight + 1, worldZ, GameConfig.BIRCH_LEAVES);
        return true;
    }

    private boolean tryPlaceCactus(GeneratedChunkColumn column, int worldX, int baseY, int worldZ, long noiseSeed) {
        int height = 2 + (int) ((noiseSeed >>> 9) & 1L);
        if (((noiseSeed >>> 13) & 3L) == 0L) {
            height++;
        }
        if (baseY + height > GameConfig.WORLD_MAX_Y) {
            return false;
        }
        if (column.getBlock(worldX, baseY - 1, worldZ) != GameConfig.SAND) {
            return false;
        }
        for (int y = baseY; y < baseY + height; y++) {
            if (column.getBlock(worldX, y, worldZ) != GameConfig.AIR
                || column.getBlock(worldX + 1, y, worldZ) != GameConfig.AIR
                || column.getBlock(worldX - 1, y, worldZ) != GameConfig.AIR
                || column.getBlock(worldX, y, worldZ + 1) != GameConfig.AIR
                || column.getBlock(worldX, y, worldZ - 1) != GameConfig.AIR) {
                return false;
            }
        }
        for (int y = baseY; y < baseY + height; y++) {
            column.setBlock(worldX, y, worldZ, GameConfig.CACTUS);
        }
        return true;
    }

    private void placeLeavesLayer(GeneratedChunkColumn column, int centerX, int y, int centerZ, int radius, boolean rounded, byte leafBlock) {
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                if (rounded && Math.abs(offsetX) == radius && Math.abs(offsetZ) == radius) {
                    continue;
                }
                byte existing = column.getBlock(centerX + offsetX, y, centerZ + offsetZ);
                if (existing == GameConfig.AIR || isPlant(existing)) {
                    column.setBlock(centerX + offsetX, y, centerZ + offsetZ, leafBlock);
                }
            }
        }
    }

    private void recalculateSurfaceHeights(GeneratedChunkColumn column, int startX, int startZ) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                column.setSurfaceHeight(localX, localZ, findSurface(column, worldX, worldZ));
            }
        }
    }

    private int sampleSurfaceHeight(int worldX, int worldZ, Biome biome) {
        return sampleSurfaceHeight(worldX, worldZ, biome, sampleContinentalness(worldX, worldZ), sampleLakeBasin(worldX, worldZ));
    }

    private int sampleSurfaceHeight(int worldX, int worldZ, Biome biome, double continentalness, double lakeBasin) {
        double continentRelief = fractalNoise(worldX * terrain.continentScale, worldZ * terrain.continentScale, 3, 0.53);
        double erosion = fractalNoise((worldX + 240.0) * terrain.erosionScale, (worldZ - 510.0) * terrain.erosionScale, 3, 0.58);
        double ridges = Math.abs(fractalNoise((worldX - 680.0) * terrain.ridgeScale, (worldZ + 420.0) * terrain.ridgeScale, 3, 0.56));
        double detail = fractalNoise((worldX + 120.0) * terrain.detailScale, (worldZ - 290.0) * terrain.detailScale, 3, 0.46);

        double landBlend = smoothstep(terrain.oceanCoastline - terrain.coastlineWidth, terrain.oceanCoastline + terrain.coastlineWidth, continentalness);
        double inland = smoothstep(terrain.oceanCoastline + 0.10, 0.42, continentalness);
        double oceanDepth = smoothstep(terrain.oceanCoastline - 0.12, -0.72, continentalness);
        double coastalShelf = smoothstep(terrain.oceanCoastline - 0.20, terrain.oceanCoastline - 0.04, continentalness)
            * (1.0 - smoothstep(terrain.oceanCoastline + 0.02, terrain.oceanCoastline + 0.12, continentalness));

        double oceanHeight = GameConfig.SEA_LEVEL - 12.0
            - oceanDepth * terrain.oceanDepth
            + coastalShelf * 3.0
            + erosion * 1.5
            + detail * 0.7;
        double shoreLift = smoothstep(terrain.oceanCoastline + 0.02, terrain.oceanCoastline + 0.26, continentalness) * 1.8;
        double mountainFactor = sampleMountainFactor(worldX, worldZ, continentalness);
        double landHeight = GameConfig.SEA_LEVEL + 4.0
            + shoreLift
            + inland * biome.continentAmplitude * 0.48 * terrain.landRelief
            + continentRelief * biome.erosionAmplitude * (0.34 + inland * 0.11) * terrain.landRelief
            + erosion * biome.erosionAmplitude * (0.30 + inland * 0.12) * terrain.landRelief
            + ridges * ridges * biome.ridgeAmplitude * (0.16 + inland * 0.34) * terrain.ridgeRelief
            + detail * biome.detailAmplitude * (0.36 + inland * 0.19) * terrain.detailRelief;
        if (mountainFactor > 0.0) {
            double peak = smoothstep(0.08, 0.54, ridges) * mountainFactor;
            landHeight += mountainFactor * terrain.mountainBaseLift + peak * terrain.mountainPeakLift;
        }
        if (biome == Biome.WOODED_HILLS || biome == Biome.GROVE) {
            double hillPeak = smoothstep(0.22, 0.78, ridges) * smoothstep(terrain.oceanCoastline + 0.18, 0.58, continentalness);
            landHeight += hillPeak * terrain.hillPeakLift;
        }

        double baseHeight = lerp(oceanHeight, landHeight, landBlend);
        if (continentalness > terrain.oceanCoastline + 0.26
            && lakeBasin < terrain.lakeBankWidth
            && isLakeAllowed(biome, (int) Math.round(baseHeight), worldX, worldZ)) {
            double lakeBlend = smoothstep(terrain.lakeWidth, terrain.lakeBankWidth, lakeBasin);
            double lakeFloor = GameConfig.SEA_LEVEL - 7.0 + detail * 0.25;
            baseHeight = lerp(lakeFloor, baseHeight, lakeBlend);
        }

        RiverShape riverShape = sampleRiverShape(worldX, worldZ, continentalness, lakeBasin);
        if (riverShape.active) {
            double center = 1.0 - smoothstep(0.0, riverShape.width, riverShape.distanceToCenter);
            double valley = 1.0 - smoothstep(riverShape.width * 0.22, riverShape.bankWidth, riverShape.distanceToCenter);
            double lowlandWetness = 1.0 - smoothstep(GameConfig.SEA_LEVEL + 3.0, GameConfig.SEA_LEVEL + 14.0, baseHeight);
            double riverGrade = fractalNoise((worldX + 1230.0) * terrain.riverFieldScale * 0.32, (worldZ - 1880.0) * terrain.riverFieldScale * 0.32, 2, 0.56);
            double targetWater = GameConfig.SEA_LEVEL + riverGrade * 1.6;
            double wetFloor = Math.min(baseHeight - lerp(0.10, 0.36, center), targetWater - lerp(0.28, 1.15, center));
            double dryFloor = baseHeight - lerp(0.08, 0.34, center);
            double targetFloor = lerp(dryFloor, wetFloor, lowlandWetness);
            baseHeight = lerp(baseHeight, targetFloor, valley * terrain.riverValleyStrength);
        }

        if (biome == Biome.DESERT) {
            double dunes = Math.abs(fractalNoise((worldX + 430.0) * 0.016, (worldZ - 720.0) * 0.016, 3, 0.52));
            double duneRipples = fractalNoise((worldX - 170.0) * 0.055, (worldZ + 260.0) * 0.055, 2, 0.50);
            baseHeight += Math.max(0.0, dunes - 0.30) * 8.0 + duneRipples * 1.2;
        }

        return clamp((int) Math.round(baseHeight), 8, GameConfig.WORLD_MAX_Y - 9);
    }

    private int sampleDirtDepth(int worldX, int worldZ, Biome biome) {
        double soilNoise = fractalNoise((worldX - 80.0) * 0.041, (worldZ + 60.0) * 0.041, 2, 0.55);
        int variation = soilNoise > 0.35 ? 2 : (soilNoise > -0.15 ? 1 : 0);
        return clamp(biome.baseSoilDepth + variation, 3, 5);
    }

    private int waterLevelForTerrain(int surfaceHeight, byte terrainFlags) {
        if ((terrainFlags & TERRAIN_OCEAN) != 0) {
            return GameConfig.SEA_LEVEL;
        }
        if ((terrainFlags & TERRAIN_LAKE) != 0) {
            return GameConfig.SEA_LEVEL - 2;
        }
        return surfaceHeight;
    }

    private boolean shouldFillWithSeaWater(int surfaceHeight, int waterLevel, byte terrainFlags) {
        if ((terrainFlags & TERRAIN_OCEAN) != 0) {
            return surfaceHeight < GameConfig.SEA_LEVEL;
        }
        if (surfaceHeight >= waterLevel) {
            return false;
        }
        return (terrainFlags & TERRAIN_LAKE) != 0;
    }

    private byte sampleTerrainFlags(int worldX, int worldZ, double continentalness, double lakeBasin, int surfaceHeight) {
        byte flags = 0;
        Biome biome = sampleBiome(worldX, worldZ);
        boolean lake = continentalness > terrain.oceanCoastline + 0.26
            && lakeBasin < terrain.lakeWidth
            && isLakeAllowed(biome, surfaceHeight, worldX, worldZ);
        if (lake) {
            return TERRAIN_LAKE;
        }
        if (continentalness < terrain.oceanCoastline) {
            return TERRAIN_OCEAN;
        }

        boolean shorelineHeight = surfaceHeight >= GameConfig.SEA_LEVEL - 2
            && surfaceHeight <= GameConfig.SEA_LEVEL + BEACH_MAX_HEIGHT_ABOVE_SEA;
        boolean oceanCoastline = continentalness >= terrain.oceanCoastline
            && continentalness <= terrain.oceanCoastline + terrain.coastlineWidth
            && lakeBasin >= terrain.lakeBankWidth;
        if (shorelineHeight && oceanCoastline) {
            return TERRAIN_BEACH;
        }

        return flags;
    }

    private boolean isLakeAllowed(Biome biome, int surfaceHeight, int worldX, int worldZ) {
        if (biome == Biome.MOUNTAINS || biome == Biome.GROVE || biome == Biome.WOODED_HILLS) {
            return false;
        }
        if (surfaceHeight > GameConfig.SEA_LEVEL + 5) {
            return false;
        }
        int minHeight = surfaceHeight;
        int maxHeight = surfaceHeight;
        int[][] samples = {
            {10, 0}, {-10, 0}, {0, 10}, {0, -10},
            {7, 7}, {-7, 7}, {7, -7}, {-7, -7},
            {14, 0}, {-14, 0}, {0, 14}, {0, -14}
        };
        for (int[] sample : samples) {
            int sx = worldX + sample[0];
            int sz = worldZ + sample[1];
            Biome nearbyBiome = sampleBiome(sx, sz);
            if (isMountainBiome(nearbyBiome)) {
                return false;
            }
            int height = sampleSurfaceHeightNoLake(sx, sz, nearbyBiome);
            minHeight = Math.min(minHeight, height);
            maxHeight = Math.max(maxHeight, height);
            if (height > GameConfig.SEA_LEVEL + 8) {
                return false;
            }
        }
        if (maxHeight - minHeight > 5) {
            return false;
        }
        double roughness = Math.abs(fractalNoise((worldX - 930.0) * terrain.ridgeScale, (worldZ + 510.0) * terrain.ridgeScale, 2, 0.55));
        return roughness < 0.28;
    }

    private int terrainHeightDeltaAround(int worldX, int worldZ, int radius, Biome centerBiome) {
        int center = sampleSurfaceHeightNoLake(worldX, worldZ, centerBiome);
        int min = center;
        int max = center;
        int north = sampleSurfaceHeightNoLake(worldX, worldZ - radius, sampleBiome(worldX, worldZ - radius));
        int south = sampleSurfaceHeightNoLake(worldX, worldZ + radius, sampleBiome(worldX, worldZ + radius));
        int west = sampleSurfaceHeightNoLake(worldX - radius, worldZ, sampleBiome(worldX - radius, worldZ));
        int east = sampleSurfaceHeightNoLake(worldX + radius, worldZ, sampleBiome(worldX + radius, worldZ));
        min = Math.min(min, Math.min(Math.min(north, south), Math.min(west, east)));
        max = Math.max(max, Math.max(Math.max(north, south), Math.max(west, east)));
        return max - min;
    }

    private int sampleSurfaceHeightNoLake(int worldX, int worldZ, Biome biome) {
        return sampleSurfaceHeight(worldX, worldZ, biome, sampleContinentalness(worldX, worldZ), 1.0);
    }

    private double sampleContinentalness(int worldX, int worldZ) {
        double primary = fractalNoise((worldX + 1800.0) * terrain.continentalnessScale, (worldZ - 900.0) * terrain.continentalnessScale, 5, 0.54);
        double broad = fractalNoise((worldX - 2400.0) * terrain.continentalnessScale * 0.45, (worldZ + 1600.0) * terrain.continentalnessScale * 0.45, 3, 0.56);
        return clamp(primary * 0.72 + broad * 0.28, -1.0, 1.0);
    }

    private RiverSample sampleRiver(int worldX, int worldZ, int approximateSurfaceHeight, double continentalness, double lakeBasin) {
        RiverShape shape = sampleRiverShape(worldX, worldZ, continentalness, lakeBasin);
        if (!shape.active) {
            return RiverSample.INACTIVE;
        }

        double centerMask = 1.0 - smoothstep(0.0, shape.width, shape.distanceToCenter);
        boolean wet = approximateSurfaceHeight <= GameConfig.SEA_LEVEL + terrain.riverWetHeight && centerMask > terrain.riverWetMask;
        double riverGrade = fractalNoise((worldX + 1230.0) * terrain.riverFieldScale * 0.32, (worldZ - 1880.0) * terrain.riverFieldScale * 0.32, 2, 0.56);
        int waterLevel = clamp(
            Math.min(approximateSurfaceHeight - 1, GameConfig.SEA_LEVEL + 2 + (int) Math.round(riverGrade * 1.5)),
            GameConfig.SEA_LEVEL,
            GameConfig.SEA_LEVEL + 5
        );
        if (wet) {
            double bedNoise = fractalNoise((worldX - 910.0) * 0.050, (worldZ + 310.0) * 0.050, 2, 0.50);
            int depth = clamp(terrain.riverMinDepth + (int) Math.round(centerMask * terrain.riverDepthRange + bedNoise * 0.35), terrain.riverMinDepth, terrain.riverMaxDepth);
            int bedLevel = waterLevel - depth;
            return new RiverSample(true, true, shape.distanceToCenter, shape.width, shape.bankWidth, shape.mask, waterLevel, bedLevel);
        }

        int dryCut = (int) Math.round(smoothstep(0.62, 1.0, shape.mask) * 1.0);
        return new RiverSample(true, false, shape.distanceToCenter, shape.width, shape.bankWidth, shape.mask, waterLevel, approximateSurfaceHeight - dryCut);
    }

    private RiverShape sampleRiverShape(int worldX, int worldZ, double continentalness, double lakeBasin) {
        if (continentalness <= terrain.oceanCoastline + terrain.riverActivationOffset || lakeBasin < terrain.lakeBankWidth) {
            return RiverShape.INACTIVE;
        }

        double line = sampleRiverField(worldX, worldZ);
        double dx = sampleRiverField(worldX + 4, worldZ) - sampleRiverField(worldX - 4, worldZ);
        double dz = sampleRiverField(worldX, worldZ + 4) - sampleRiverField(worldX, worldZ - 4);
        double slope = Math.max(0.0012, (Math.abs(dx) + Math.abs(dz)) / 8.0);
        double distance = Math.abs(line) / slope;

        double widthNoise = climate01(fractalNoise((worldX + 1480.0) * terrain.riverWidthScale, (worldZ - 820.0) * terrain.riverWidthScale, 2, 0.54));
        double width = lerp(terrain.riverMinWidth, terrain.riverMaxWidth, widthNoise);
        double bankWidth = width + lerp(terrain.riverMinBank, terrain.riverMaxBank, widthNoise);
        double mask = 1.0 - smoothstep(width, bankWidth, distance);
        if (mask <= 0.0) {
            return RiverShape.INACTIVE;
        }
        return new RiverShape(true, distance, width, bankWidth, mask);
    }

    private double sampleRiverField(int worldX, int worldZ) {
        double primary = fractalNoise((worldX + 760.0) * terrain.riverFieldScale, (worldZ - 1130.0) * terrain.riverFieldScale, 3, 0.52);
        double broad = fractalNoise((worldX - 340.0) * terrain.riverFieldScale * 0.48, (worldZ + 910.0) * terrain.riverFieldScale * 0.48, 2, 0.55);
        return primary * 0.82 + broad * 0.18;
    }

    private double sampleLakeBasin(int worldX, int worldZ) {
        double basin = climate01(fractalNoise((worldX - 2200.0) * 0.0048, (worldZ + 1770.0) * 0.0048, 2, 0.55));
        double breakup = climate01(fractalNoise((worldX + 640.0) * 0.014, (worldZ - 480.0) * 0.014, 2, 0.52));
        return 1.0 - (basin * 0.82 + breakup * 0.18);
    }

    private Biome sampleBiome(int worldX, int worldZ) {
        double temperature = smoothClimate(
            worldX,
            worldZ,
            terrain.temperatureScale,
            -380.0,
            140.0,
            4,
            0.56
        );
        double humidity = smoothClimate(
            worldX,
            worldZ,
            terrain.humidityScale,
            510.0,
            -260.0,
            4,
            0.58
        );
        double continentalness = sampleContinentalness(worldX, worldZ);
        double highland = sampleHighland(worldX, worldZ);
        double weirdness = sampleWeirdness(worldX, worldZ);
        double mountainFactor = sampleMountainFactor(worldX, worldZ, continentalness);
        if (mountainFactor > 0.18 && temperature < 0.82 && humidity > 0.16) {
            return Biome.MOUNTAINS;
        }
        if (highland > terrain.groveThreshold && temperature < 0.50 && humidity > 0.48) {
            return Biome.GROVE;
        }
        if (highland > terrain.woodedHillsThreshold && humidity > 0.42 && weirdness > terrain.woodedHillsWeirdness) {
            return Biome.WOODED_HILLS;
        }
        if (highland > terrain.meadowThreshold && temperature > 0.42 && temperature < 0.78 && humidity > 0.30 && humidity < 0.70) {
            return Biome.MEADOW;
        }
        return betaBiomeFromClimate(temperature, humidity, weirdness);
    }

    private double sampleHighland(int worldX, int worldZ) {
        return smoothClimate(worldX, worldZ, terrain.highlandScale, -2140.0, 990.0, 5, 0.56);
    }

    private double sampleRidgeGate(int worldX, int worldZ) {
        double ridges = Math.abs(fractalNoise((worldX + 750.0) * terrain.ridgeGateScale, (worldZ - 1180.0) * terrain.ridgeGateScale, 3, 0.55));
        return smoothstep(terrain.mountainRidgeThreshold - 0.10, terrain.mountainRidgeThreshold + 0.18, ridges);
    }

    private double sampleWeirdness(int worldX, int worldZ) {
        return smoothClimate(worldX, worldZ, terrain.weirdnessScale, 150.0, -2060.0, 4, 0.57);
    }

    private double sampleMountainFactor(int worldX, int worldZ, double continentalness) {
        if (terrain.mountainPeakLift <= 0.0) {
            return 0.0;
        }
        double highland = sampleHighland(worldX, worldZ);
        double highlandMask = smoothstep(terrain.mountainThreshold - 0.16, terrain.mountainThreshold + 0.04, highland);
        double inland = smoothstep(terrain.oceanCoastline + 0.05, 0.32, continentalness);
        double ridgeMask = 0.65 + sampleRidgeGate(worldX, worldZ) * 0.35;
        return clamp(highlandMask * ridgeMask * inland, 0.0, 1.0);
    }

    String debugBiomeName(int worldX, int worldZ) {
        double continentalness = sampleContinentalness(worldX, worldZ);
        double lakeBasin = sampleLakeBasin(worldX, worldZ);
        int surfaceHeight = sampleSurfaceHeight(worldX, worldZ, sampleBiome(worldX, worldZ), continentalness, lakeBasin);
        RiverSample river = sampleRiver(worldX, worldZ, surfaceHeight, continentalness, lakeBasin);
        byte terrainFlags = sampleTerrainFlags(worldX, worldZ, continentalness, lakeBasin, surfaceHeight);
        if ((terrainFlags & TERRAIN_OCEAN) != 0) {
            return surfaceHeight < GameConfig.SEA_LEVEL - 10 ? "Deep Ocean" : "Ocean";
        }
        if (river.wet && river.mask >= 0.42) {
            return "River";
        }
        if ((terrainFlags & TERRAIN_LAKE) != 0) {
            return "Lake";
        }
        if ((terrainFlags & TERRAIN_BEACH) != 0) {
            return "Beach";
        }
        return formatBiomeName(sampleBiome(worldX, worldZ));
    }

    boolean isLikelyDrySpawnColumn(int worldX, int worldZ) {
        double continentalness = sampleContinentalness(worldX, worldZ);
        double lakeBasin = sampleLakeBasin(worldX, worldZ);
        Biome biome = sampleBiome(worldX, worldZ);
        int surfaceHeight = sampleSurfaceHeight(worldX, worldZ, biome, continentalness, lakeBasin);
        byte terrainFlags = sampleTerrainFlags(worldX, worldZ, continentalness, lakeBasin, surfaceHeight);
        if (surfaceHeight <= GameConfig.SEA_LEVEL || (terrainFlags & (TERRAIN_OCEAN | TERRAIN_LAKE)) != 0) {
            return false;
        }
        RiverSample river = sampleRiver(worldX, worldZ, surfaceHeight, continentalness, lakeBasin);
        return river == null || !river.wet || river.mask <= 0.30;
    }

    int debugSurfaceHeight(int worldX, int worldZ) {
        double continentalness = sampleContinentalness(worldX, worldZ);
        return sampleSurfaceHeight(worldX, worldZ, sampleBiome(worldX, worldZ), continentalness, sampleLakeBasin(worldX, worldZ));
    }

    String debugTerrainInfo(int worldX, int worldZ) {
        double continentalness = sampleContinentalness(worldX, worldZ);
        double lakeBasin = sampleLakeBasin(worldX, worldZ);
        Biome biome = sampleBiome(worldX, worldZ);
        int surfaceHeight = sampleSurfaceHeight(worldX, worldZ, biome, continentalness, lakeBasin);
        RiverSample river = sampleRiver(worldX, worldZ, surfaceHeight, continentalness, lakeBasin);
        return "river active=" + river.active
            + " wet=" + river.wet
            + " riverDistance=" + formatDebug(river.distanceToCenter)
            + " riverWidth=" + formatDebug(river.width)
            + " riverBank=" + formatDebug(river.bankWidth)
            + " riverMask=" + formatDebug(river.mask)
            + " riverWaterLevel=" + river.waterLevel
            + " riverBedLevel=" + river.bedLevel;
    }

    String debugDensityInfo(int worldX, int worldY, int worldZ) {
        TerrainSample sample = terrainSampleAt(worldX, worldZ);
        double density = densitySampler.sampleTerrainDensity(worldX, worldY, worldZ, sample);
        double cave = densitySampler.sampleCaveDensity(worldX, worldY, worldZ);
        int depthBelowSurface = sample.approximateSurfaceHeight - worldY;
        double openThreshold = CAVE_TUNNEL_THRESHOLD + smoothstep(-4.0, GameConfig.WORLD_MIN_Y + 18.0, worldY) * 0.065;
        return "density=" + formatDebug(density)
            + " approx=" + sample.approximateSurfaceHeight
            + " caveDensity=" + formatDebug(cave)
            + " caveOpen=" + (depthBelowSurface >= CAVE_MIN_DEPTH_BELOW_SURFACE && cave > openThreshold)
            + " depthBelowSurface=" + depthBelowSurface;
    }

    private TerrainSample terrainSampleAt(int worldX, int worldZ) {
        double continentalness = densitySampler.sampleContinentalness(worldX, worldZ);
        double lakeBasin = sampleLakeBasin(worldX, worldZ);
        Biome biome = sampleBiome(worldX, worldZ);
        int surfaceHeight = sampleSurfaceHeight(worldX, worldZ, biome, continentalness, lakeBasin);
        byte terrainFlags = sampleTerrainFlags(worldX, worldZ, continentalness, lakeBasin, surfaceHeight);
        return new TerrainSample(surfaceHeight, terrainFlags);
    }

    private String formatDebug(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private String formatBiomeName(Biome biome) {
        String[] parts = biome.name().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private double smoothClimate(int worldX, int worldZ, double scale, double offsetX, double offsetZ, int octaves, double persistence) {
        double center = climate01(fractalNoise((worldX + offsetX) * scale, (worldZ + offsetZ) * scale, octaves, persistence));
        double broad = 0.0;
        int samples = 0;
        for (int offsetSampleX = -1; offsetSampleX <= 1; offsetSampleX++) {
            for (int offsetSampleZ = -1; offsetSampleZ <= 1; offsetSampleZ++) {
                int sampleX = worldX + offsetSampleX * GameConfig.CHUNK_SIZE * 2;
                int sampleZ = worldZ + offsetSampleZ * GameConfig.CHUNK_SIZE * 2;
                broad += climate01(fractalNoise((sampleX + offsetX) * scale, (sampleZ + offsetZ) * scale, octaves, persistence));
                samples++;
            }
        }
        return clamp(lerp(center, broad / samples, 0.42), 0.0, 1.0);
    }

    private boolean biomeSupportsTrees(Biome biome) {
        return biome == Biome.RAINFOREST
            || biome == Biome.JUNGLE
            || biome == Biome.SEASONAL_FOREST
            || biome == Biome.FOREST
            || biome == Biome.BIRCH_FOREST
            || biome == Biome.DARK_FOREST
            || biome == Biome.SWAMPLAND
            || biome == Biome.TAIGA
            || biome == Biome.GROVE
            || biome == Biome.WOODED_HILLS
            || biome == Biome.MOUNTAINS
            || biome == Biome.MEADOW
            || biome == Biome.PLAINS;
    }

    private boolean shouldPlaceSnow(int worldX, int worldZ, Biome biome) {
        if (biome.snowCovered) {
            return true;
        }
        return (biome == Biome.MOUNTAINS || biome == Biome.GROVE)
            && sampleSurfaceHeight(worldX, worldZ, biome) > GameConfig.SEA_LEVEL + 54;
    }

    private boolean canSupportSnow(byte block) {
        return block == GameConfig.GRASS
            || block == GameConfig.DIRT
            || block == GameConfig.COBBLESTONE
            || block == GameConfig.STONE
            || block == GameConfig.SNOW_BLOCK
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.SAND;
    }

    private boolean isAquaticPlantFloor(byte block) {
        return block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.CLAY
            || block == GameConfig.DIRT;
    }

    private void smoothSnowCoverage(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                Biome biome = BIOMES[scratch.biomeOrdinals[index] & 0xFF];
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = findSurface(column, worldX, worldZ);
                if (!shouldPlaceSnow(worldX, worldZ, biome)
                    || surfaceY <= GameConfig.SEA_LEVEL
                    || surfaceY + 1 > GameConfig.WORLD_MAX_Y
                    || isAquaticTerrain(scratch.terrainFlags[index])
                    || (scratch.riverSamples[index] != null && scratch.riverSamples[index].active && scratch.riverSamples[index].mask > 0.24)) {
                    removeUnsupportedSnow(column, worldX, surfaceY + 1, worldZ);
                    continue;
                }

                byte surfaceBlock = column.getBlock(worldX, surfaceY, worldZ);
                byte above = column.getBlock(worldX, surfaceY + 1, worldZ);
                if (canSupportSnow(surfaceBlock) && above == GameConfig.AIR) {
                    column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.SNOW_LAYER);
                } else if (above == GameConfig.SNOW_LAYER && !canSupportSnow(surfaceBlock)) {
                    column.setBlock(worldX, surfaceY + 1, worldZ, GameConfig.AIR);
                }
            }
        }
    }

    private void removeUnsupportedSnow(GeneratedChunkColumn column, int worldX, int worldY, int worldZ) {
        if (GameConfig.isWorldYInside(worldY) && column.getBlock(worldX, worldY, worldZ) == GameConfig.SNOW_LAYER) {
            column.setBlock(worldX, worldY, worldZ, GameConfig.AIR);
        }
    }

    private void removeSubmergedPlantsAndSnow(GeneratedChunkColumn column, int startX, int startZ) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                for (int worldY = GameConfig.WORLD_MIN_Y + 1; worldY < GameConfig.WORLD_MAX_Y; worldY++) {
                    byte block = column.getBlock(worldX, worldY, worldZ);
                    if (!isPlant(block) || block == GameConfig.SEAGRASS || block == GameConfig.KELP) {
                        continue;
                    }
                    if (GameConfig.isWaterBlock(column.getBlock(worldX, worldY - 1, worldZ))
                        || GameConfig.isWaterBlock(column.getBlock(worldX, worldY + 1, worldZ))
                        || hasAdjacentWater(column, worldX, worldY, worldZ)) {
                        column.setBlock(worldX, worldY, worldZ, GameConfig.AIR);
                    }
                }
            }
        }
    }

    private boolean hasAdjacentWater(GeneratedChunkColumn column, int worldX, int worldY, int worldZ) {
        return GameConfig.isWaterBlock(column.getBlock(worldX + 1, worldY, worldZ))
            || GameConfig.isWaterBlock(column.getBlock(worldX - 1, worldY, worldZ))
            || GameConfig.isWaterBlock(column.getBlock(worldX, worldY, worldZ + 1))
            || GameConfig.isWaterBlock(column.getBlock(worldX, worldY, worldZ - 1));
    }

    private double treeChanceForBiome(Biome biome) {
        switch (biome) {
            case JUNGLE:
                return 0.175;
            case RAINFOREST:
                return 0.145;
            case DARK_FOREST:
                return 0.155;
            case FOREST:
            case SEASONAL_FOREST:
                return 0.110;
            case BIRCH_FOREST:
                return 0.082;
            case SWAMPLAND:
                return 0.060;
            case GROVE:
            case TAIGA:
                return 0.085;
            case WOODED_HILLS:
                return 0.070;
            case MOUNTAINS:
                return 0.020;
            case MEADOW:
                return 0.018;
            case PLAINS:
                return 0.012;
            default:
                return 0.0;
        }
    }

    private Biome betaBiomeFromClimate(double temperature, double humidity, double weirdness) {
        double adjustedHumidity = humidity * temperature;
        if (temperature < 0.12) {
            return humidity < 0.10 ? Biome.ICE_DESERT : Biome.TUNDRA;
        }
        if (temperature < 0.34 && humidity > 0.46 && weirdness > 0.56) {
            return Biome.GROVE;
        }
        if (temperature > Math.min(terrain.desertTemperature, 0.70) && humidity < Math.max(terrain.desertHumidity, 0.38)) {
            return weirdness > 0.70 ? Biome.BADLANDS : Biome.DESERT;
        }
        if (humidity < 0.24) {
            return temperature > 0.58 ? Biome.SAVANNA : Biome.SHRUBLAND;
        }
        if (temperature > 0.82 && humidity > 0.78) {
            return weirdness > 0.42 ? Biome.JUNGLE : Biome.RAINFOREST;
        }
        if (adjustedHumidity > 0.52 && temperature < 0.68) {
            return Biome.SWAMPLAND;
        }
        if (temperature < 0.38 || (temperature < 0.44 && humidity > 0.66 && weirdness > 0.54)) {
            return Biome.TAIGA;
        }
        if (temperature < 0.82) {
            if (humidity > 0.62 && weirdness > 0.55) {
                return Biome.DARK_FOREST;
            }
            if (humidity > terrain.birchMinHumidity && humidity < terrain.birchMaxHumidity && weirdness < terrain.birchWeirdness) {
                return Biome.BIRCH_FOREST;
            }
            if (humidity < 0.38) {
                return Biome.SHRUBLAND;
            }
            return Biome.FOREST;
        }
        if (humidity < 0.44) {
            return Biome.PLAINS;
        }
        if (humidity < 0.82) {
            return Biome.SEASONAL_FOREST;
        }
        return Biome.RAINFOREST;
    }

    private double climate01(double value) {
        return clamp(value * 0.5 + 0.5, 0.0, 1.0);
    }

    private boolean isAquaticTerrain(byte terrainFlags) {
        return (terrainFlags & (TERRAIN_OCEAN | TERRAIN_BEACH | TERRAIN_LAKE)) != 0;
    }

    private boolean shouldProtectAquaticSurfaceEntrance(GenerationScratch scratch, int startX, int startZ, int worldX, int worldY, int worldZ) {
        int localX = worldX - startX;
        int localZ = worldZ - startZ;
        if (localX < 0 || localX >= GameConfig.CHUNK_SIZE || localZ < 0 || localZ >= GameConfig.CHUNK_SIZE) {
            return false;
        }
        int index = columnIndex(localX, localZ);
        byte terrainFlags = scratch.terrainFlags[index];
        if (!isAquaticTerrain(terrainFlags) && (scratch.riverSamples[index] == null || !scratch.riverSamples[index].wet)) {
            return false;
        }
        int protectedDepth = caveRoofDepth(BIOMES[scratch.biomeOrdinals[index] & 0xFF], terrainFlags, scratch.riverSamples[index] != null && scratch.riverSamples[index].wet) + 8;
        return scratch.surfaceHeights[index] - worldY < protectedDepth;
    }

    private int caveRoofDepth(byte terrainFlags) {
        if ((terrainFlags & TERRAIN_OCEAN) != 0) {
            return 28;
        }
        if ((terrainFlags & TERRAIN_LAKE) != 0) {
            return 24;
        }
        if ((terrainFlags & TERRAIN_BEACH) != 0) {
            return 20;
        }
        return 8;
    }

    private int caveRoofDepth(Biome biome, byte terrainFlags, boolean riverWet) {
        int roofDepth = caveRoofDepth(terrainFlags);
        if (riverWet) {
            roofDepth = Math.max(roofDepth, 26);
        }
        if (isForestLikeBiome(biome)) {
            roofDepth = Math.max(roofDepth, 12);
        }
        if (biome == Biome.MOUNTAINS) {
            roofDepth = Math.max(roofDepth, 12);
        }
        return roofDepth;
    }

    private boolean isForestLikeBiome(Biome biome) {
        return biome == Biome.FOREST
            || biome == Biome.BIRCH_FOREST
            || biome == Biome.DARK_FOREST
            || biome == Biome.JUNGLE
            || biome == Biome.RAINFOREST
            || biome == Biome.SEASONAL_FOREST
            || biome == Biome.TAIGA
            || biome == Biome.GROVE
            || biome == Biome.WOODED_HILLS
            || biome == Biome.SWAMPLAND;
    }

    private boolean canCarveCaveBlock(byte block) {
        return block == GameConfig.COBBLESTONE
            || block == GameConfig.STONE
            || block == GameConfig.DEEPSLATE;
    }

    private boolean canCarveCaveEntranceBlock(byte block, boolean entranceColumn) {
        if (!entranceColumn) {
            return false;
        }
        return block == GameConfig.COBBLESTONE
            || block == GameConfig.STONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.DIRT
            || block == GameConfig.GRASS
            || block == GameConfig.SAND
            || block == GameConfig.GRAVEL;
    }

    private boolean shouldCarveCanyon(int worldX, int worldY, int worldZ, int surfaceHeight, Biome biome, byte terrainFlags) {
        if (isAquaticTerrain(terrainFlags)
            || biome.canyonStrength < 0.13
            || worldY < GameConfig.SEA_LEVEL - 42
            || worldY > Math.min(surfaceHeight - 30, GameConfig.SEA_LEVEL - 18)) {
            return false;
        }

        double canyonNoise = Math.abs(fractalNoise(worldX * CANYON_SCALE, worldZ * CANYON_SCALE, 2, 0.58));
        if (canyonNoise < 0.993) {
            return false;
        }
        double canyonFloor = GameConfig.SEA_LEVEL - 34.0
            + fractalNoise((worldX + 540.0) * 0.011, (worldZ - 410.0) * 0.011, 2, 0.60) * 4.0;
        double sideBreakup = climate01(fractalNoise3((worldX - 400.0) * 0.020, (worldY + 30.0) * 0.030, (worldZ + 520.0) * 0.020, 2, 0.52));
        return worldY > canyonFloor && sideBreakup > 0.68;
    }

    private boolean isSolidCaveFloor(byte block) {
        return isOpaque(block) && block != GameConfig.BEDROCK;
    }

    private boolean hasCaveAirSpace(GeneratedChunkColumn column, int worldX, int worldY, int worldZ) {
        int air = 0;
        if (column.getBlock(worldX + 1, worldY, worldZ) == GameConfig.AIR) {
            air++;
        }
        if (column.getBlock(worldX - 1, worldY, worldZ) == GameConfig.AIR) {
            air++;
        }
        if (column.getBlock(worldX, worldY, worldZ + 1) == GameConfig.AIR) {
            air++;
        }
        if (column.getBlock(worldX, worldY, worldZ - 1) == GameConfig.AIR) {
            air++;
        }
        if (column.getBlock(worldX, worldY + 1, worldZ) == GameConfig.AIR) {
            air++;
        }
        return air >= 3;
    }

    private byte topBlockForTerrain(Biome biome, byte terrainFlags, int surfaceHeight, int worldX, int worldZ) {
        if (isBeachSurface(surfaceHeight, terrainFlags)) {
            return GameConfig.SAND;
        }
        if ((terrainFlags & TERRAIN_OCEAN) != 0 && surfaceHeight < GameConfig.SEA_LEVEL - 8) {
            return aquaticFloorBlock(worldX, worldZ, terrainFlags);
        }
        if ((terrainFlags & TERRAIN_LAKE) != 0) {
            return aquaticFloorBlock(worldX, worldZ, terrainFlags);
        }
        if ((terrainFlags & TERRAIN_OCEAN) != 0) {
            return aquaticFloorBlock(worldX, worldZ, terrainFlags);
        }
        if (isMountainBiome(biome) && surfaceHeight > GameConfig.SEA_LEVEL + 28) {
            double rockNoise = climate01(fractalNoise((worldX - 1520.0) * 0.064, (worldZ + 830.0) * 0.064, 2, 0.54));
            if (surfaceHeight > GameConfig.SEA_LEVEL + 72 && rockNoise > 0.35) {
                return GameConfig.SNOW_BLOCK;
            }
            if (surfaceHeight > GameConfig.SEA_LEVEL + 56 || rockNoise > 0.62) {
                return GameConfig.STONE;
            }
        }
        return biome.topBlock;
    }

    private byte fillerBlockForTerrain(Biome biome, byte terrainFlags, int surfaceHeight, int worldX, int worldZ) {
        if ((terrainFlags & TERRAIN_OCEAN) != 0) {
            return GameConfig.SAND;
        }
        if ((terrainFlags & TERRAIN_BEACH) != 0) {
            return GameConfig.SAND;
        }
        if ((terrainFlags & TERRAIN_LAKE) != 0) {
            return GameConfig.DIRT;
        }
        if (isMountainBiome(biome) && surfaceHeight > GameConfig.SEA_LEVEL + 40) {
            double rockNoise = climate01(fractalNoise((worldX + 330.0) * 0.052, (worldZ - 1740.0) * 0.052, 2, 0.56));
            if (rockNoise > 0.36) {
                return GameConfig.STONE;
            }
        }
        return biome.fillerBlock;
    }

    private boolean isMountainBiome(Biome biome) {
        return biome == Biome.MOUNTAINS || biome == Biome.GROVE || biome == Biome.WOODED_HILLS;
    }

    private byte aquaticFloorBlock(int worldX, int worldZ, byte terrainFlags) {
        if ((terrainFlags & TERRAIN_BEACH) != 0) {
            return GameConfig.SAND;
        }
        if ((terrainFlags & TERRAIN_LAKE) != 0) {
            double floorNoise = fractalNoise((worldX + 930.0) * 0.070, (worldZ - 1210.0) * 0.070, 2, 0.52);
            return floorNoise > 0.30 ? GameConfig.GRAVEL : (floorNoise < -0.35 ? GameConfig.SAND : GameConfig.DIRT);
        }
        double floorNoise = fractalNoise((worldX + 930.0) * 0.060, (worldZ - 1210.0) * 0.060, 2, 0.52);
        if (floorNoise > -0.02) {
            return GameConfig.GRAVEL;
        }
        if (floorNoise < -0.48) {
            return GameConfig.CLAY;
        }
        return GameConfig.SAND;
    }

    private void stabilizeGeneratedSand(GeneratedChunkColumn column, int startX, int startZ) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                for (int worldY = GameConfig.WORLD_MIN_Y + 1; worldY <= GameConfig.WORLD_MAX_Y; worldY++) {
                    if (column.getBlock(worldX, worldY, worldZ) != GameConfig.SAND) {
                        continue;
                    }

                    byte support = column.getBlock(worldX, worldY - 1, worldZ);
                    if (isGeneratedSandSupport(support)) {
                        continue;
                    }
                    column.setBlock(worldX, worldY, worldZ, GameConfig.AIR);
                }
            }
        }
    }

    private boolean isGeneratedSandSupport(byte block) {
        return block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.COBBLESTONE
            || block == GameConfig.DIRT
            || block == GameConfig.GRASS
            || block == GameConfig.STONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.CLAY;
    }

    private void stabilizeRiverBanks(GeneratedChunkColumn column, int startX, int startZ, GenerationScratch scratch) {
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int index = columnIndex(localX, localZ);
                RiverSample river = scratch.riverSamples[index];
                if (river == null || !river.active || river.mask < 0.28) {
                    continue;
                }
                if ((scratch.generationMasks[index] & (GENMASK_STRUCTURE | GENMASK_ROAD)) != 0) {
                    continue;
                }

                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = findSurface(column, worldX, worldZ);
                int bottomY = Math.max(GameConfig.WORLD_MIN_Y + 1, surfaceY - 7);
                for (int y = surfaceY; y >= bottomY; y--) {
                    byte block = column.getBlock(worldX, y, worldZ);
                    if (!isNaturalSurfaceBlock(block) && !isTerrainBaseBlock(block)) {
                        continue;
                    }
                    byte below = column.getBlock(worldX, y - 1, worldZ);
                    if (below != GameConfig.AIR && !GameConfig.isLiquidBlock(below)) {
                        continue;
                    }
                    if (river.wet && y <= river.waterLevel + 1) {
                        column.setBlock(worldX, y, worldZ, riverBedBlock(worldX, worldZ, river));
                        if (y + 1 <= river.waterLevel) {
                            column.setNaturalFluid(worldX, y + 1, worldZ, GameConfig.WATER_SOURCE);
                        }
                    } else {
                        column.setBlock(worldX, y, worldZ, GameConfig.AIR);
                    }
                }
            }
        }
    }

    private int findSurface(GeneratedChunkColumn column, int worldX, int worldZ) {
        return column.findSurface(worldX, worldZ);
    }

    private boolean isOpaque(byte block) {
        return block != GameConfig.AIR
            && !GameConfig.isLiquidBlock(block)
            && !isPlant(block);
    }

    private boolean isPlant(byte block) {
        return block == GameConfig.TALL_GRASS
            || block == GameConfig.SEAGRASS
            || block == GameConfig.KELP
            || block == GameConfig.RED_FLOWER
            || block == GameConfig.YELLOW_FLOWER
            || block == GameConfig.SNOW_LAYER
            || block == GameConfig.DEAD_BUSH
            || block == GameConfig.WHEAT_CROP
            || block == GameConfig.CARROT_CROP
            || block == GameConfig.POTATO_CROP
            || block == GameConfig.RAIL
            || block == GameConfig.TORCH
            || block == GameConfig.OAK_DOOR;
    }

    private void initializePermutation() {
        int[] values = new int[256];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }

        Random random = new Random(seed);
        for (int i = values.length - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            int temporary = values[i];
            values[i] = values[swapIndex];
            values[swapIndex] = temporary;
        }

        for (int i = 0; i < permutation.length; i++) {
            permutation[i] = values[i & 255];
        }
    }

    private int columnIndex(int localX, int localZ) {
        return localZ * GameConfig.CHUNK_SIZE + localX;
    }

    private long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private double randomUnit(long bits) {
        return ((bits >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    private double fractalNoise(double x, double z, int octaves, double persistence) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double amplitudeSum = 0.0;

        for (int octave = 0; octave < octaves; octave++) {
            total += perlin(x * frequency, z * frequency) * amplitude;
            amplitudeSum += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }

        return total / amplitudeSum;
    }

    private double fractalNoise3(double x, double y, double z, int octaves, double persistence) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double amplitudeSum = 0.0;

        for (int octave = 0; octave < octaves; octave++) {
            total += perlin3(x * frequency, y * frequency, z * frequency) * amplitude;
            amplitudeSum += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }

        return total / amplitudeSum;
    }

    private double perlin(double x, double y) {
        int baseX = (int) Math.floor(x) & 255;
        int baseY = (int) Math.floor(y) & 255;

        double localX = x - Math.floor(x);
        double localY = y - Math.floor(y);

        double fadeX = fade(localX);
        double fadeY = fade(localY);

        int aa = permutation[permutation[baseX] + baseY];
        int ab = permutation[permutation[baseX] + baseY + 1];
        int ba = permutation[permutation[baseX + 1] + baseY];
        int bb = permutation[permutation[baseX + 1] + baseY + 1];

        double blendTop = lerp(grad(aa, localX, localY), grad(ba, localX - 1.0, localY), fadeX);
        double blendBottom = lerp(grad(ab, localX, localY - 1.0), grad(bb, localX - 1.0, localY - 1.0), fadeX);
        return lerp(blendTop, blendBottom, fadeY);
    }

    private double perlin3(double x, double y, double z) {
        int baseX = (int) Math.floor(x) & 255;
        int baseY = (int) Math.floor(y) & 255;
        int baseZ = (int) Math.floor(z) & 255;

        double localX = x - Math.floor(x);
        double localY = y - Math.floor(y);
        double localZ = z - Math.floor(z);

        double fadeX = fade(localX);
        double fadeY = fade(localY);
        double fadeZ = fade(localZ);

        int a = permutation[baseX] + baseY;
        int aa = permutation[a] + baseZ;
        int ab = permutation[a + 1] + baseZ;
        int b = permutation[baseX + 1] + baseY;
        int ba = permutation[b] + baseZ;
        int bb = permutation[b + 1] + baseZ;

        double x1 = lerp(grad3(permutation[aa], localX, localY, localZ), grad3(permutation[ba], localX - 1.0, localY, localZ), fadeX);
        double x2 = lerp(grad3(permutation[ab], localX, localY - 1.0, localZ), grad3(permutation[bb], localX - 1.0, localY - 1.0, localZ), fadeX);
        double y1 = lerp(x1, x2, fadeY);

        double x3 = lerp(grad3(permutation[aa + 1], localX, localY, localZ - 1.0), grad3(permutation[ba + 1], localX - 1.0, localY, localZ - 1.0), fadeX);
        double x4 = lerp(grad3(permutation[ab + 1], localX, localY - 1.0, localZ - 1.0), grad3(permutation[bb + 1], localX - 1.0, localY - 1.0, localZ - 1.0), fadeX);
        double y2 = lerp(x3, x4, fadeY);

        return lerp(y1, y2, fadeZ);
    }

    private double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private double grad(int hash, double x, double y) {
        switch (hash & 7) {
            case 0:
                return x + y;
            case 1:
                return -x + y;
            case 2:
                return x - y;
            case 3:
                return -x - y;
            case 4:
                return x;
            case 5:
                return -x;
            case 6:
                return y;
            default:
                return -y;
        }
    }

    private double grad3(int hash, double x, double y, double z) {
        switch (hash & 15) {
            case 0:
                return x + y;
            case 1:
                return -x + y;
            case 2:
                return x - y;
            case 3:
                return -x - y;
            case 4:
                return x + z;
            case 5:
                return -x + z;
            case 6:
                return x - z;
            case 7:
                return -x - z;
            case 8:
                return y + z;
            case 9:
                return -y + z;
            case 10:
                return y - z;
            case 11:
                return -y - z;
            case 12:
                return x + y;
            case 13:
                return -x + y;
            case 14:
                return -y + z;
            default:
                return y - z;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class TerrainProfile {
        final double continentScale;
        final double continentalnessScale;
        final double erosionScale;
        final double ridgeScale;
        final double detailScale;
        final double riverFieldScale;
        final double riverWidthScale;
        final double temperatureScale;
        final double humidityScale;
        final double highlandScale;
        final double ridgeGateScale;
        final double weirdnessScale;
        final double oceanCoastline;
        final double coastlineWidth;
        final double oceanDepth;
        final double landRelief;
        final double ridgeRelief;
        final double detailRelief;
        final double mountainBaseLift;
        final double mountainPeakLift;
        final double hillPeakLift;
        final double riverMinWidth;
        final double riverMaxWidth;
        final double riverMinBank;
        final double riverMaxBank;
        final double riverActivationOffset;
        final double riverValleyStrength;
        final int riverWetHeight;
        final double riverWetMask;
        final int riverMinDepth;
        final int riverMaxDepth;
        final double riverDepthRange;
        final double lakeWidth;
        final double lakeBankWidth;
        final double mountainThreshold;
        final double mountainRidgeThreshold;
        final double groveThreshold;
        final double woodedHillsThreshold;
        final double woodedHillsWeirdness;
        final double meadowThreshold;
        final double desertTemperature;
        final double desertHumidity;
        final double birchMinHumidity;
        final double birchMaxHumidity;
        final double birchWeirdness;

        TerrainProfile(
            double continentScale,
            double continentalnessScale,
            double erosionScale,
            double ridgeScale,
            double detailScale,
            double riverFieldScale,
            double riverWidthScale,
            double temperatureScale,
            double humidityScale,
            double highlandScale,
            double ridgeGateScale,
            double weirdnessScale,
            double oceanCoastline,
            double coastlineWidth,
            double oceanDepth,
            double landRelief,
            double ridgeRelief,
            double detailRelief,
            double mountainBaseLift,
            double mountainPeakLift,
            double hillPeakLift,
            double riverMinWidth,
            double riverMaxWidth,
            double riverMinBank,
            double riverMaxBank,
            double riverActivationOffset,
            double riverValleyStrength,
            int riverWetHeight,
            double riverWetMask,
            int riverMinDepth,
            int riverMaxDepth,
            double riverDepthRange,
            double lakeWidth,
            double lakeBankWidth,
            double mountainThreshold,
            double mountainRidgeThreshold,
            double groveThreshold,
            double woodedHillsThreshold,
            double woodedHillsWeirdness,
            double meadowThreshold,
            double desertTemperature,
            double desertHumidity,
            double birchMinHumidity,
            double birchMaxHumidity,
            double birchWeirdness
        ) {
            this.continentScale = continentScale;
            this.continentalnessScale = continentalnessScale;
            this.erosionScale = erosionScale;
            this.ridgeScale = ridgeScale;
            this.detailScale = detailScale;
            this.riverFieldScale = riverFieldScale;
            this.riverWidthScale = riverWidthScale;
            this.temperatureScale = temperatureScale;
            this.humidityScale = humidityScale;
            this.highlandScale = highlandScale;
            this.ridgeGateScale = ridgeGateScale;
            this.weirdnessScale = weirdnessScale;
            this.oceanCoastline = oceanCoastline;
            this.coastlineWidth = coastlineWidth;
            this.oceanDepth = oceanDepth;
            this.landRelief = landRelief;
            this.ridgeRelief = ridgeRelief;
            this.detailRelief = detailRelief;
            this.mountainBaseLift = mountainBaseLift;
            this.mountainPeakLift = mountainPeakLift;
            this.hillPeakLift = hillPeakLift;
            this.riverMinWidth = riverMinWidth;
            this.riverMaxWidth = riverMaxWidth;
            this.riverMinBank = riverMinBank;
            this.riverMaxBank = riverMaxBank;
            this.riverActivationOffset = riverActivationOffset;
            this.riverValleyStrength = riverValleyStrength;
            this.riverWetHeight = riverWetHeight;
            this.riverWetMask = riverWetMask;
            this.riverMinDepth = riverMinDepth;
            this.riverMaxDepth = riverMaxDepth;
            this.riverDepthRange = riverDepthRange;
            this.lakeWidth = lakeWidth;
            this.lakeBankWidth = lakeBankWidth;
            this.mountainThreshold = mountainThreshold;
            this.mountainRidgeThreshold = mountainRidgeThreshold;
            this.groveThreshold = groveThreshold;
            this.woodedHillsThreshold = woodedHillsThreshold;
            this.woodedHillsWeirdness = woodedHillsWeirdness;
            this.meadowThreshold = meadowThreshold;
            this.desertTemperature = desertTemperature;
            this.desertHumidity = desertHumidity;
            this.birchMinHumidity = birchMinHumidity;
            this.birchMaxHumidity = birchMaxHumidity;
            this.birchWeirdness = birchWeirdness;
        }

        static TerrainProfile forPreset(TerrainPreset preset) {
            if (preset == TerrainPreset.LARGE_BIOMES) {
                return new TerrainProfile(
                    0.00235, 0.00072, 0.0032, 0.0082, 0.029,
                    0.00092, 0.00095, 0.00095, 0.00105, 0.00082, 0.00180, 0.00135,
                    0.005, 0.035, 38.0, 1.36, 1.55, 1.12, 48.0, 150.0, 8.0,
                    7.2, 13.2, 12.0, 19.0, 0.12, 0.74, 13, 0.14, 2, 4, 2.05,
                    LAKE_WIDTH, LAKE_BANK_WIDTH, 0.38, 0.08, 0.62, 0.61, 0.49, 0.58,
                    0.76, 0.30, 0.32, 0.72, 0.44
                );
            }
            if (preset == TerrainPreset.DEFAULT) {
                return new TerrainProfile(
                    0.00315, 0.00105, 0.0048, 0.0125, 0.041,
                    0.00145, 0.00155, 0.00215, 0.00230, 0.00145, 0.00305, 0.00255,
                    0.010, 0.052, 36.0, 1.44, 1.66, 1.16, 54.0, 164.0, 9.0,
                    6.4, 11.4, 10.5, 16.5, 0.13, 0.72, 12, 0.14, 2, 4, 2.00,
                    LAKE_WIDTH, LAKE_BANK_WIDTH, 0.38, 0.08, 0.62, 0.61, 0.49, 0.58,
                    0.76, 0.30, 0.32, 0.72, 0.44
                );
            }
            return new TerrainProfile(
                CONTINENT_SCALE, CONTINENTALNESS_SCALE, EROSION_SCALE, RIDGE_SCALE, DETAIL_SCALE,
                RIVER_FIELD_SCALE, 0.0022, TEMPERATURE_SCALE, HUMIDITY_SCALE, 0.0018, 0.0048, 0.0034,
                OCEAN_COASTLINE, COASTLINE_WIDTH, 30.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0,
                RIVER_MIN_WIDTH_BLOCKS, RIVER_MAX_WIDTH_BLOCKS, RIVER_MIN_BANK_BLOCKS, RIVER_MAX_BANK_BLOCKS,
                0.20, 0.26, 5, 0.24, 1, 2, 0.65,
                LAKE_WIDTH, LAKE_BANK_WIDTH, 0.79, 0.22, 0.70, 0.68, 0.62, 0.66,
                0.92, 0.12, 0.36, 0.66, 0.30
            );
        }
    }

    private enum Biome {
        RAINFOREST(GameConfig.GRASS, GameConfig.DIRT, 5, 18.0, 8.0, 9.0, 4.2, 0.10, false, false),
        JUNGLE(GameConfig.GRASS, GameConfig.DIRT, 5, 20.0, 9.0, 12.0, 4.8, 0.11, false, false),
        SWAMPLAND(GameConfig.GRASS, GameConfig.DIRT, 5, 13.0, 5.0, 4.0, 2.0, 0.04, false, false),
        SEASONAL_FOREST(GameConfig.GRASS, GameConfig.DIRT, 5, 17.0, 7.0, 8.0, 3.5, 0.10, false, false),
        FOREST(GameConfig.GRASS, GameConfig.DIRT, 5, 18.0, 8.0, 10.0, 4.0, 0.12, false, false),
        BIRCH_FOREST(GameConfig.GRASS, GameConfig.DIRT, 5, 16.0, 6.0, 7.0, 3.0, 0.08, false, false),
        DARK_FOREST(GameConfig.GRASS, GameConfig.DIRT, 5, 17.0, 7.5, 8.0, 2.7, 0.06, false, false),
        SAVANNA(GameConfig.GRASS, GameConfig.DIRT, 4, 16.0, 6.0, 4.0, 2.4, 0.05, false, false),
        SHRUBLAND(GameConfig.GRASS, GameConfig.DIRT, 4, 15.0, 6.0, 5.0, 2.6, 0.06, false, false),
        TAIGA(GameConfig.GRASS, GameConfig.DIRT, 5, 17.0, 7.0, 11.0, 3.4, 0.14, true, true),
        GROVE(GameConfig.GRASS, GameConfig.DIRT, 5, 18.0, 7.0, 12.0, 3.0, 0.10, true, true),
        MOUNTAINS(GameConfig.GRASS, GameConfig.DIRT, 4, 34.0, 16.0, 36.0, 7.0, 0.12, false, true),
        WOODED_HILLS(GameConfig.GRASS, GameConfig.DIRT, 4, 25.0, 11.0, 22.0, 5.2, 0.13, false, false),
        MEADOW(GameConfig.GRASS, GameConfig.DIRT, 4, 19.0, 5.0, 8.0, 2.8, 0.04, false, false),
        DESERT(GameConfig.SAND, GameConfig.SAND, 4, 16.0, 7.0, 5.0, 2.8, 0.05, false, false),
        BADLANDS(GameConfig.SAND, GameConfig.SAND, 3, 22.0, 10.0, 18.0, 4.5, 0.16, false, false),
        PLAINS(GameConfig.GRASS, GameConfig.DIRT, 4, 16.0, 7.0, 8.0, 3.0, 0.18, false, false),
        ICE_DESERT(GameConfig.SAND, GameConfig.SAND, 4, 12.0, 4.0, 3.0, 1.8, 0.04, true, false),
        TUNDRA(GameConfig.GRASS, GameConfig.DIRT, 4, 14.0, 5.0, 4.0, 2.0, 0.05, true, false);

        final byte topBlock;
        final byte fillerBlock;
        final int baseSoilDepth;
        final double continentAmplitude;
        final double erosionAmplitude;
        final double ridgeAmplitude;
        final double detailAmplitude;
        final double canyonStrength;
        final boolean snowCovered;
        final boolean pineTrees;

        Biome(byte topBlock, byte fillerBlock, int baseSoilDepth, double continentAmplitude, double erosionAmplitude,
              double ridgeAmplitude, double detailAmplitude, double canyonStrength,
              boolean snowCovered, boolean pineTrees) {
            this.topBlock = topBlock;
            this.fillerBlock = fillerBlock;
            this.baseSoilDepth = baseSoilDepth;
            this.continentAmplitude = continentAmplitude;
            this.erosionAmplitude = erosionAmplitude;
            this.ridgeAmplitude = ridgeAmplitude;
            this.detailAmplitude = detailAmplitude;
            this.canyonStrength = canyonStrength;
            this.snowCovered = snowCovered;
            this.pineTrees = pineTrees;
        }
    }

    private final class DensitySampler {
        double sampleTerrainDensity(int worldX, int worldY, int worldZ, TerrainSample sample) {
            return sampleTerrainDensity(worldX, worldY, worldZ, sample, false);
        }

        double sampleTerrainDensity(int worldX, int worldY, int worldZ, TerrainSample sample, boolean protectCaves) {
            if (worldY > sample.approximateSurfaceHeight + 36) {
                return -64.0;
            }
            double terrainDensity = sample.approximateSurfaceHeight - worldY;
            double surfaceBand = 1.0 - smoothstep(18.0, 72.0, Math.abs(sample.approximateSurfaceHeight - worldY));
            double terrainNoise = surfaceBand > 0.001
                ? fractalNoise3((worldX + 310.0) * 0.018, (worldY - 24.0) * 0.016, (worldZ - 190.0) * 0.018, 3, 0.53) * 4.2 * surfaceBand
                : 0.0;
            double coarseLift = Math.abs(sample.approximateSurfaceHeight - worldY) < 96.0
                ? fractalNoise3((worldX - 820.0) * 0.006, (worldY + 48.0) * 0.010, (worldZ + 470.0) * 0.006, 2, 0.58) * 2.4
                : 0.0;
            double density = terrainDensity + terrainNoise + coarseLift;

            double surfaceDepth = sample.approximateSurfaceHeight - worldY;
            int naturalRoofDepth = caveRoofDepth(sample.terrainFlags);
            if (protectCaves || surfaceDepth < naturalRoofDepth) {
                return density;
            }
            double cave = sampleCaveDensity(worldX, worldY, worldZ);
            boolean protectedAquaticRoof = isAquaticTerrain(sample.terrainFlags) && surfaceDepth < naturalRoofDepth + 8;
            double deepTightening = smoothstep(-4.0, GameConfig.WORLD_MIN_Y + 18.0, worldY) * 0.030;
            double deepOpening = smoothstep(-2.0, GameConfig.WORLD_MIN_Y + 18.0, worldY) * 0.060;
            double openThreshold = CAVE_TUNNEL_THRESHOLD + deepTightening - deepOpening;
            if (!protectedAquaticRoof && cave > openThreshold && worldY > GameConfig.WORLD_MIN_Y + 7) {
                density = Math.min(density, -4.0 - cave * 4.0);
            } else if (!protectedAquaticRoof) {
                density -= cave * 4.2;
            }
            return density;
        }

        double sampleContinentalness(int x, int z) {
            return WorldGenerator.this.sampleContinentalness(x, z);
        }

        double sampleCaveDensity(int x, int y, int z) {
            double midBand = smoothstep(GameConfig.SEA_LEVEL + 18.0, GameConfig.SEA_LEVEL - 26.0, y);
            double deepBand = smoothstep(-10.0, GameConfig.WORLD_MIN_Y + 14.0, y);
            double bedrockFade = 1.0 - smoothstep(GameConfig.WORLD_MIN_Y + 10.0, GameConfig.WORLD_MIN_Y + 22.0, y);
            double depthFactor = clamp(0.26 + midBand * 0.54 + deepBand * 0.30 - bedrockFade * 0.12, 0.0, 1.0);
            double upperCaveBonus = smoothstep(GameConfig.SEA_LEVEL + 12.0, GameConfig.SEA_LEVEL - 12.0, y) * 0.035;
            double tunnelA = Math.abs(fractalNoise3((x + 120.0) * CAVE_FREQUENCY, (y - 18.0) * (CAVE_FREQUENCY * 1.28), (z - 240.0) * CAVE_FREQUENCY, 2, 0.56));
            double tunnelB = Math.abs(fractalNoise3((x - 620.0) * (CAVE_FREQUENCY * 0.58), (y + 80.0) * (CAVE_FREQUENCY * 0.92), (z + 410.0) * (CAVE_FREQUENCY * 0.58), 2, 0.54));
            double tunnelOpen = Math.max(
                1.0 - smoothstep(0.014, 0.074, tunnelA),
                (1.0 - smoothstep(0.010, 0.056, tunnelB)) * 0.78
            );
            double gate = climate01(fractalNoise3((x + 710.0) * 0.0085, (y - 90.0) * 0.010, (z - 330.0) * 0.0085, 2, 0.52));
            double choke = climate01(fractalNoise3((x - 1330.0) * 0.018, (y + 240.0) * 0.022, (z + 970.0) * 0.018, 2, 0.52));
            tunnelOpen *= smoothstep(0.34, 0.62, gate) * lerp(0.62, 1.0, choke);

            double roomNoise = climate01(fractalNoise3((x - 940.0) * CAVE_ROOM_FREQUENCY, (y + 70.0) * (CAVE_ROOM_FREQUENCY * 1.36), (z + 610.0) * CAVE_ROOM_FREQUENCY, 3, 0.56));
            double roomBand = smoothstep(GameConfig.SEA_LEVEL + 2.0, GameConfig.SEA_LEVEL - 46.0, y)
                * (1.0 - smoothstep(GameConfig.WORLD_MIN_Y + 18.0, GameConfig.WORLD_MIN_Y + 8.0, y));
            double roomOpen = smoothstep(CAVE_ROOM_THRESHOLD - 0.01, 0.98, roomNoise) * roomBand * 0.20;
            return clamp(Math.max(tunnelOpen * depthFactor * 1.08, roomOpen) + upperCaveBonus * 0.65, 0.0, 1.0);
        }
    }

    private static final class TerrainSample {
        int approximateSurfaceHeight;
        byte terrainFlags;

        TerrainSample(int approximateSurfaceHeight, byte terrainFlags) {
            set(approximateSurfaceHeight, terrainFlags);
        }

        void set(int approximateSurfaceHeight, byte terrainFlags) {
            this.approximateSurfaceHeight = approximateSurfaceHeight;
            this.terrainFlags = terrainFlags;
        }
    }

    private static final class VillagePlan {
        static final VillagePlan NONE = new VillagePlan(0, 0, 0, 0, 0, 0L);

        final int centerChunkX;
        final int centerChunkZ;
        final int centerX;
        final int centerZ;
        final int baseY;
        final long seed;

        VillagePlan(int centerChunkX, int centerChunkZ, int centerX, int centerZ, int baseY, long seed) {
            this.centerChunkX = centerChunkX;
            this.centerChunkZ = centerChunkZ;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.baseY = baseY;
            this.seed = seed;
        }
    }

    private static final class MineshaftPlan {
        final int centerChunkX;
        final int centerChunkZ;
        final int centerX;
        final int centerZ;
        final int y;
        final long seed;

        MineshaftPlan(int centerChunkX, int centerChunkZ, int centerX, int centerZ, int y, long seed) {
            this.centerChunkX = centerChunkX;
            this.centerChunkZ = centerChunkZ;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.y = y;
            this.seed = seed;
        }
    }

    private static final class RiverShape {
        static final RiverShape INACTIVE = new RiverShape(false, Double.POSITIVE_INFINITY, 0.0, 0.0, 0.0);

        final boolean active;
        final double distanceToCenter;
        final double width;
        final double bankWidth;
        final double mask;

        RiverShape(boolean active, double distanceToCenter, double width, double bankWidth, double mask) {
            this.active = active;
            this.distanceToCenter = distanceToCenter;
            this.width = width;
            this.bankWidth = bankWidth;
            this.mask = mask;
        }
    }

    private static final class RiverSample {
        static final RiverSample INACTIVE = new RiverSample(false, false, Double.POSITIVE_INFINITY, 0.0, 0.0, 0.0, RIVER_WATER_LEVEL, GameConfig.SEA_LEVEL);

        final boolean active;
        final boolean wet;
        final double distanceToCenter;
        final double width;
        final double bankWidth;
        final double mask;
        final int waterLevel;
        final int bedLevel;

        RiverSample(boolean active, boolean wet, double distanceToCenter, double width, double bankWidth, double mask, int waterLevel, int bedLevel) {
            this.active = active;
            this.wet = wet;
            this.distanceToCenter = distanceToCenter;
            this.width = width;
            this.bankWidth = bankWidth;
            this.mask = mask;
            this.waterLevel = waterLevel;
            this.bedLevel = bedLevel;
        }
    }

    private static final class GenerationScratch {
        final int[] surfaceHeights = new int[COLUMN_AREA];
        final byte[] biomeOrdinals = new byte[COLUMN_AREA];
        final byte[] dirtDepths = new byte[COLUMN_AREA];
        final byte[] terrainFlags = new byte[COLUMN_AREA];
        final byte[] generationMasks = new byte[COLUMN_AREA];
        final double[] continentalness = new double[COLUMN_AREA];
        final double[] lakeBasins = new double[COLUMN_AREA];
        final RiverSample[] riverSamples = new RiverSample[COLUMN_AREA];
        final TerrainSample[] samples = new TerrainSample[COLUMN_AREA];

        GenerationScratch() {
            for (int i = 0; i < samples.length; i++) {
                samples[i] = new TerrainSample(GameConfig.SEA_LEVEL, (byte) 0);
            }
        }
    }
}

final class GeneratedChunkColumn implements StructureTemplates.Target {
    final int chunkX;
    final int chunkZ;
    private final Chunk[] sections = new Chunk[GameConfig.SECTION_COUNT];
    private final short[] surfaceHeights = new short[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE];
    private ChunkGenerationStatus status = ChunkGenerationStatus.EMPTY;

    GeneratedChunkColumn(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        for (int chunkY = 0; chunkY < sections.length; chunkY++) {
            sections[chunkY] = new Chunk(chunkX, chunkY, chunkZ);
        }
    }

    Chunk[] sections() {
        return sections;
    }

    ChunkGenerationStatus status() {
        return status;
    }

    void setStatus(ChunkGenerationStatus status) {
        this.status = status == null ? ChunkGenerationStatus.EMPTY : status;
    }

    byte getBlock(int worldX, int worldY, int worldZ) {
        if (!GameConfig.isWorldYInside(worldY)
            || Math.floorDiv(worldX, GameConfig.CHUNK_SIZE) != chunkX
            || Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE) != chunkZ) {
            return GameConfig.AIR;
        }
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localY = GameConfig.localYForWorldY(worldY);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(worldY);
        return sections[chunkY].getBlockLocal(localX, localY, localZ);
    }

    void setBlock(int worldX, int worldY, int worldZ, byte block) {
        if (!GameConfig.isWorldYInside(worldY)
            || Math.floorDiv(worldX, GameConfig.CHUNK_SIZE) != chunkX
            || Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE) != chunkZ) {
            return;
        }
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localY = GameConfig.localYForWorldY(worldY);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(worldY);
        Chunk chunk = sections[chunkY];
        chunk.setBlockLocal(localX, localY, localZ, block);
        if (GameConfig.isLiquidBlock(block)) {
            chunk.setFluidDistanceLocal(localX, localY, localZ, 0);
        }
    }

    public void setTemplateBlock(int worldX, int worldY, int worldZ, BlockState state) {
        if (Math.floorDiv(worldX, GameConfig.CHUNK_SIZE) != chunkX
            || Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE) != chunkZ
            || !GameConfig.isWorldYInside(worldY)) {
            return;
        }
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localY = GameConfig.localYForWorldY(worldY);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(worldY);
        sections[chunkY].setBlockStateLocal(localX, localY, localZ, state);
        if (state != null && state.type.isLiquid()) {
            sections[chunkY].setFluidDistanceLocal(localX, localY, localZ, 0);
        }
    }

    public BlockState getTemplateBlock(int worldX, int worldY, int worldZ) {
        return getBlockState(worldX, worldY, worldZ);
    }

    BlockState getBlockState(int worldX, int worldY, int worldZ) {
        if (!GameConfig.isWorldYInside(worldY)
            || Math.floorDiv(worldX, GameConfig.CHUNK_SIZE) != chunkX
            || Math.floorDiv(worldZ, GameConfig.CHUNK_SIZE) != chunkZ) {
            return Blocks.stateFromLegacyId(GameConfig.AIR);
        }
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localY = GameConfig.localYForWorldY(worldY);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(worldY);
        return sections[chunkY].getBlockStateLocal(localX, localY, localZ);
    }

    void setNaturalFluid(int worldX, int worldY, int worldZ, byte block) {
        setBlock(worldX, worldY, worldZ, block);
        if (!GameConfig.isWorldYInside(worldY) || !GameConfig.isLiquidBlock(block)) {
            return;
        }
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localY = GameConfig.localYForWorldY(worldY);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(worldY);
        sections[chunkY].setFluidDistanceLocal(localX, localY, localZ, GameConfig.NATURAL_FLUID_DISTANCE);
    }

    void setSurfaceHeight(int localX, int localZ, int height) {
        surfaceHeights[localZ * GameConfig.CHUNK_SIZE + localX] = (short) height;
    }

    int getSurfaceHeight(int localX, int localZ) {
        return surfaceHeights[localZ * GameConfig.CHUNK_SIZE + localX];
    }

    int findSurface(int worldX, int worldZ) {
        int localX = Math.floorMod(worldX, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(worldZ, GameConfig.CHUNK_SIZE);
        for (int chunkY = sections.length - 1; chunkY >= 0; chunkY--) {
            Chunk chunk = sections[chunkY];
            if (chunk == null || chunk.isEmpty()) {
                continue;
            }
            int sectionBaseY = GameConfig.sectionYForIndex(chunkY);
            for (int localY = GameConfig.CHUNK_SIZE - 1; localY >= 0; localY--) {
                int worldY = sectionBaseY + localY;
                if (!GameConfig.isWorldYInside(worldY)) {
                    continue;
                }
                byte block = chunk.getBlockLocal(localX, localY, localZ);
                if (block != GameConfig.AIR && !GameConfig.isLiquidBlock(block) && !isIgnoredSurfacePlant(block)) {
                    return worldY;
                }
            }
        }
        return GameConfig.WORLD_MIN_Y;
    }

    private static boolean isIgnoredSurfacePlant(byte block) {
        return block == GameConfig.TALL_GRASS
            || block == GameConfig.SEAGRASS
            || block == GameConfig.RED_FLOWER
            || block == GameConfig.YELLOW_FLOWER
            || block == GameConfig.SNOW_LAYER
            || block == GameConfig.DEAD_BUSH
            || block == GameConfig.WHEAT_CROP
            || block == GameConfig.RAIL
            || block == GameConfig.TORCH
            || block == GameConfig.OAK_DOOR;
    }
}
