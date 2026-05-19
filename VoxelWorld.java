import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;

final class VoxelWorld implements StructureTemplates.Target {
    private static final int SAVE_FORMAT_VERSION = 5;
    private static final int LEGACY_BYTE_BLOCK_SAVE_FORMAT_VERSION = 4;
    private static final int CONTAINER_SAVE_MAGIC = 0x544D4354;
    private static final int CONTAINER_SAVE_VERSION = 3;
    private static final int MOB_SAVE_MAGIC = 0x544D4F42;
    private static final int MOB_SAVE_VERSION = 3;
    private static final double PASSIVE_BABY_GROW_SECONDS = 600.0;
    private static final int PLAYER_INVENTORY_SAVE_MAGIC = 0x544D5049;
    private static final int PLAYER_INVENTORY_SAVE_VERSION = 1;
    private static final int NETWORK_PLAYER_INVENTORY_SAVE_MAGIC = 0x544D4E49;
    private static final int NETWORK_PLAYER_INVENTORY_SAVE_VERSION = 1;
    private static final int PARADISE_PLAYER_STATE_MAGIC = 0x54435052;
    private static final int PARADISE_PLAYER_STATE_VERSION = 2;
    private static final double ZOMBIE_GROWL_DISTANCE_SQUARED = 11.0 * 11.0;
    private static final double COLLISION_EPSILON = 1.0e-7;
    private static final int HOSTILE_MOB_TARGET_COUNT = 18;
    private static final int FISH_MOB_TARGET_COUNT = 64;
    private static final int MAX_ASYNC_COLUMN_SUBMISSIONS_PER_TICK = Math.max(192, GameConfig.CHUNK_GENERATION_THREADS * 48);
    private static final int MAX_INITIAL_COLUMN_SUBMISSIONS = Math.max(768, GameConfig.CHUNK_GENERATION_THREADS * 192);
    private static final int MAX_PENDING_COLUMN_TASKS = Math.max(8192, GameConfig.CHUNK_GENERATION_THREADS * 1024);
    private static final int MAX_COLUMN_INTEGRATIONS_PER_FRAME = Math.max(16, GameConfig.CHUNK_GENERATION_THREADS * 5);
    private static final long COLUMN_INTEGRATION_BUDGET_NS = 5_000_000L;
    private static final int MAX_WATER_CELLS_PER_WORLD_TICK = 128;
    private static final int MAX_LAVA_CELLS_PER_WORLD_TICK = 32;
    private static final int MAX_SAND_CELLS_PER_WORLD_TICK = 96;
    private static final int MAX_MOB_AI_UPDATES_PER_FRAME = 32;
    private static final int SPAWN_LAND_SEARCH_RADIUS = 64;
    private static final int SPAWN_LAND_SEARCH_STEP = 16;

    static final class ChunkColumn {
        final int chunkX;
        final int chunkZ;
        final Chunk[] sections = new Chunk[GameConfig.SECTION_COUNT];
        final short[] surfaceHeights = new short[Chunk.SIZE * Chunk.SIZE];
        ChunkGenerationStatus status = ChunkGenerationStatus.EMPTY;
        boolean dirty;
        boolean generated;
        boolean naturalTerrain;

        ChunkColumn(int chunkX, int chunkZ) {
            this(chunkX, chunkZ, true);
        }

        ChunkColumn(int chunkX, int chunkZ, boolean allocateSections) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            if (allocateSections) {
                for (int chunkY = 0; chunkY < sections.length; chunkY++) {
                    sections[chunkY] = new Chunk(chunkX, chunkY, chunkZ);
                }
            }
        }

        Chunk section(int chunkY) {
            if (!GameConfig.isChunkCoordinateInside(chunkX, chunkY, chunkZ)) {
                return null;
            }
            return sections[chunkY];
        }

        int getSurfaceHeightLocal(int localX, int localZ) {
            return surfaceHeights[indexSurface(localX, localZ)];
        }

        void setSurfaceHeightLocal(int localX, int localZ, int height) {
            surfaceHeights[indexSurface(localX, localZ)] = (short) height;
        }

        private static int indexSurface(int localX, int localZ) {
            return localZ * Chunk.SIZE + localX;
        }
    }

    private static final class PendingBlockChange {
        final int x;
        final int y;
        final int z;
        final byte block;
        final int distance;

        PendingBlockChange(int x, int y, int z, byte block, int distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.distance = distance;
        }
    }

    private static final class CachedFluidFlow {
        final double x;
        final double y;
        final double z;

        CachedFluidFlow(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class DimensionRuntimeState {
        final ConcurrentHashMap<Long, ChunkColumn> loadedColumns = new ConcurrentHashMap<>();
        final ConcurrentHashMap<Long, Future<ChunkColumn>> pendingColumns = new ConcurrentHashMap<>();
        final ArrayList<MobEntity> mobs = new ArrayList<>();
        final HashSet<Long> populatedVillageCells = new HashSet<>();
        final ArrayList<DroppedItem> droppedItems = new ArrayList<>();
        final ArrayList<FallingBlock> fallingBlocks = new ArrayList<>();
        final HashMap<Long, ContainerInventory> chestContainers = new HashMap<>();
        final HashMap<Long, FurnaceBlockEntity> furnaces = new HashMap<>();
        final HashSet<Long> activeWaterCells = new HashSet<>();
        final HashSet<Long> activeLavaCells = new HashSet<>();
        final HashSet<Long> activeSandCells = new HashSet<>();
        final HashSet<Long> queuedWaterCells = new HashSet<>();
        final HashSet<Long> queuedLavaCells = new HashSet<>();
        final HashSet<Long> queuedSandCells = new HashSet<>();
        final ArrayDeque<Long> activeWaterQueue = new ArrayDeque<>();
        final ArrayDeque<Long> activeLavaQueue = new ArrayDeque<>();
        final ArrayDeque<Long> activeSandQueue = new ArrayDeque<>();
        final HashMap<Long, Double> leafDecayTimers = new HashMap<>();
        final HashSet<Long> fallingBlockSources = new HashSet<>();
        final HashSet<Long> dirtyChunkSections = new HashSet<>();
        final HashSet<Long> pendingMeshDirtySections = new HashSet<>();
        final HashMap<Long, CachedFluidFlow> waterFlowCache = new HashMap<>();
        int streamingWarmupFrames;
        int lastStreamingChunkX = Integer.MIN_VALUE;
        int lastStreamingChunkZ = Integer.MIN_VALUE;
    }

    private long seed;
    private TerrainPreset terrainPreset = TerrainPreset.LEGACY;
    private Path worldDirectory;
    private Path saveDirectory;
    private RegionStorage regionStorage;
    private RegionStorage paradiseRegionStorage;
    private final ConcurrentHashMap<Long, ChunkColumn> loadedColumns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Future<ChunkColumn>> pendingColumns = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor generationExecutor = createGenerationExecutor();
    private final BackgroundSaveService backgroundSaveService = new BackgroundSaveService();
    private final int[] permutation = new int[512];
    private final ArrayList<MobEntity> mobs = new ArrayList<>();
    private final HashMap<UUID, RemotePlayerState> remotePlayers = new HashMap<>();
    private final HashSet<Long> populatedVillageCells = new HashSet<>();
    private final ArrayList<DroppedItem> droppedItems = new ArrayList<>();
    private final ArrayList<FallingBlock> fallingBlocks = new ArrayList<>();
    private final HashMap<Long, ContainerInventory> chestContainers = new HashMap<>();
    private final HashMap<Long, FurnaceBlockEntity> furnaces = new HashMap<>();
    private final HashSet<Long> activeWaterCells = new HashSet<>();
    private final HashSet<Long> activeLavaCells = new HashSet<>();
    private final HashSet<Long> activeSandCells = new HashSet<>();
    private final HashSet<Long> queuedWaterCells = new HashSet<>();
    private final HashSet<Long> queuedLavaCells = new HashSet<>();
    private final HashSet<Long> queuedSandCells = new HashSet<>();
    private final ArrayDeque<Long> activeWaterQueue = new ArrayDeque<>();
    private final ArrayDeque<Long> activeLavaQueue = new ArrayDeque<>();
    private final ArrayDeque<Long> activeSandQueue = new ArrayDeque<>();
    private final HashMap<Long, Double> leafDecayTimers = new HashMap<>();
    private final HashSet<Long> fallingBlockSources = new HashSet<>();
    private final HashSet<Long> dirtyChunkSections = new HashSet<>();
    private final HashSet<Long> pendingMeshDirtySections = new HashSet<>();
    private final ColumnUpdateList simulationDirtyColumns = new ColumnUpdateList(64);
    private final ColumnUpdateList networkDirtyBlocks = new ColumnUpdateList(128);
    private final ArrayList<Long> readyColumnKeys = new ArrayList<>();
    private final MutableVec2 mobSteering = new MutableVec2();
    private final MutableVec2 zombieSeparation = new MutableVec2();
    private final MutableVec3 sampledFluidFlow = new MutableVec3();
    private final MutableVec3 blockFluidFlow = new MutableVec3();
    private final HashMap<Long, CachedFluidFlow> waterFlowCache = new HashMap<>();
    private Random worldRandom;
    private WorldGenerator worldGenerator;
    private final HashMap<Integer, DimensionRuntimeState> dimensionRuntimeStates = new HashMap<>();
    private boolean networkMirrorMode;
    private int activeDimensionId = GameConfig.DIMENSION_OVERWORLD;
    private double simulationAccumulator;
    private double hostileMobSpawnCooldown;
    private double fishSpawnCooldown;
    private double worldTime = 0.30;
    private long worldTickCounter;
    private int renderDistanceChunks = GameConfig.CHUNK_RENDER_DISTANCE;
    private int streamingWarmupFrames;
    private int lastStreamingChunkX = Integer.MIN_VALUE;
    private int lastStreamingChunkZ = Integer.MIN_VALUE;
    private long nextColumnTaskSequence;
    private long mobUpdateCounter;
    private long droppedItemUpdateCounter;
    private long streamingProfileLastLogNs;
    private long streamingProfilePrepareNs;
    private long streamingProfileDrainNs;
    private long streamingProfileEnsureNs;
    private long streamingProfileUnloadNs;
    private long generationProfileNs;
    private int streamingProfileFrames;
    private int streamingProfileSubmittedColumns;
    private int streamingProfileIntegratedColumns;
    private int generationProfileColumns;

    private static ThreadPoolExecutor createGenerationExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            GameConfig.CHUNK_GENERATION_THREADS,
            GameConfig.CHUNK_GENERATION_THREADS,
            0L,
            TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<>()
        );
        return executor;
    }

    private static final class PrioritizedColumnTask extends FutureTask<ChunkColumn> implements Comparable<PrioritizedColumnTask> {
        final int priority;
        final long sequence;

        PrioritizedColumnTask(Callable<ChunkColumn> callable, int priority, long sequence) {
            super(callable);
            this.priority = priority;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(PrioritizedColumnTask other) {
            int priorityCompare = Integer.compare(priority, other.priority);
            return priorityCompare != 0 ? priorityCompare : Long.compare(sequence, other.sequence);
        }
    }

    VoxelWorld(long seed) {
        configureWorld(RuntimePaths.resolve(GameConfig.SAVE_ROOT_DIRECTORY, "World 1"), seed);
    }

    long getSeed() {
        return seed;
    }

    TerrainPreset getTerrainPreset() {
        return terrainPreset;
    }

    void configureWorld(Path worldDirectory, long seed) {
        configureWorld(worldDirectory, seed, TerrainPreset.LEGACY);
    }

    void configureWorld(Path worldDirectory, long seed, TerrainPreset terrainPreset) {
        networkMirrorMode = false;
        if (this.worldDirectory != null && !this.worldDirectory.equals(worldDirectory)) {
            flushLoadedColumns();
            saveMobs();
            for (Future<ChunkColumn> future : pendingColumns.values()) {
                future.cancel(true);
            }
            pendingColumns.clear();
            loadedColumns.clear();
            activeWaterCells.clear();
            activeLavaCells.clear();
            activeSandCells.clear();
            clearDynamicBlockQueues();
            leafDecayTimers.clear();
            fallingBlockSources.clear();
            mobs.clear();
            droppedItems.clear();
            fallingBlocks.clear();
            saveContainers();
            chestContainers.clear();
            furnaces.clear();
        }
        this.seed = seed;
        this.terrainPreset = terrainPreset == null ? TerrainPreset.LEGACY : terrainPreset;
        this.worldDirectory = worldDirectory;
        this.saveDirectory = worldDirectory.resolve(GameConfig.SAVE_CHUNKS_DIRECTORY);
        this.regionStorage = new RegionStorage(worldDirectory);
        this.paradiseRegionStorage = new RegionStorage(paradiseDimensionDirectory(worldDirectory));
        this.worldRandom = new Random(seed ^ 0x7A21AF13D54E3B21L);
        this.activeDimensionId = GameConfig.DIMENSION_OVERWORLD;
        this.worldGenerator = new WorldGenerator(seed, this.terrainPreset, activeDimensionId);
        this.worldTime = 0.30;
        this.streamingWarmupFrames = 0;
        this.lastStreamingChunkX = Integer.MIN_VALUE;
        this.lastStreamingChunkZ = Integer.MIN_VALUE;
        this.mobUpdateCounter = 0L;
        this.droppedItemUpdateCounter = 0L;
        waterFlowCache.clear();
        leafDecayTimers.clear();
        loadContainers();
        loadMobs();
        dimensionRuntimeStates.clear();
        saveActiveDimensionRuntime();
    }

    void setNetworkMirrorMode(boolean networkMirrorMode) {
        this.networkMirrorMode = networkMirrorMode;
        if (networkMirrorMode) {
            for (Future<ChunkColumn> future : pendingColumns.values()) {
                future.cancel(true);
            }
            pendingColumns.clear();
            generationExecutor.purge();
        }
    }

    boolean isNetworkMirrorMode() {
        return networkMirrorMode;
    }

    List<MobEntity> getMobs() {
        return mobs;
    }

    List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }

    List<RemotePlayerState> getRemotePlayers() {
        return new ArrayList<>(remotePlayers.values());
    }

    void updateRemotePlayer(UUID uuid, String name, double x, double y, double z, double yaw, double pitch,
                            byte heldItem, boolean sneaking, boolean spectatorMode) {
        if (uuid == null) {
            return;
        }
        RemotePlayerState remote = remotePlayers.get(uuid);
        if (remote == null) {
            remote = new RemotePlayerState(uuid, name);
            remotePlayers.put(uuid, remote);
        }
        remote.capturePreviousPosition();
        remote.x = x;
        remote.y = y;
        remote.z = z;
        remote.yaw = yaw;
        remote.pitch = pitch;
        remote.heldItem = heldItem;
        remote.sneaking = sneaking;
        remote.spectatorMode = spectatorMode;
        remote.name = name == null ? remote.name : name;
        remote.lastUpdateMillis = System.currentTimeMillis();
    }

    void removeRemotePlayer(UUID uuid) {
        if (uuid != null) {
            remotePlayers.remove(uuid);
        }
    }

    void clearRemotePlayers() {
        remotePlayers.clear();
    }

    boolean hasLoadedColumnAt(double x, double z) {
        return loadedColumns.containsKey(columnKey(worldToChunk((int) Math.floor(x)), worldToChunk((int) Math.floor(z))));
    }

    List<FallingBlock> getFallingBlocks() {
        return fallingBlocks;
    }

    void spawnZombieAt(double x, double y, double z) {
        double spawnY = Math.max(GameConfig.WORLD_MIN_Y + 1.0, Math.min(GameConfig.WORLD_MAX_Y - GameConfig.ZOMBIE_HEIGHT, y));
        mobs.add(new MobEntity(x, spawnY, z, x, z, worldRandom));
    }

    void spawnMobAt(MobKind kind, double x, double y, double z) {
        double spawnY = Math.max(GameConfig.WORLD_MIN_Y + 1.0, Math.min(GameConfig.WORLD_MAX_Y - GameConfig.ZOMBIE_HEIGHT, y));
        mobs.add(new MobEntity(kind, x, spawnY, z, x, z, worldRandom));
    }

    private void spawnBabyMobAt(MobKind kind, double x, double y, double z) {
        double spawnY = Math.max(GameConfig.WORLD_MIN_Y + 1.0, Math.min(GameConfig.WORLD_MAX_Y - GameConfig.ZOMBIE_HEIGHT, y));
        MobEntity baby = new MobEntity(kind, x, spawnY, z, x, z, worldRandom);
        baby.babyAge = PASSIVE_BABY_GROW_SECONDS;
        baby.breedCooldown = PASSIVE_BABY_GROW_SECONDS;
        mobs.add(baby);
    }

    void setRenderDistanceChunks(int renderDistanceChunks) {
        this.renderDistanceChunks = clamp(renderDistanceChunks, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
    }

    Chunk getChunk(int chunkX, int chunkY, int chunkZ) {
        ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
        if (column == null) {
            return null;
        }
        return column.section(chunkY);
    }

    ChunkSectionSnapshot getChunkSnapshot(int chunkX, int chunkY, int chunkZ) {
        Chunk chunk = getChunk(chunkX, chunkY, chunkZ);
        return chunk == null ? null : chunk.snapshot();
    }

    boolean isChunkEmpty(int chunkX, int chunkY, int chunkZ) {
        Chunk chunk = getChunk(chunkX, chunkY, chunkZ);
        return chunk == null || chunk.isEmpty();
    }

    boolean isChunkLoaded(int chunkX, int chunkZ) {
        ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
        return column != null && column.generated && column.status == ChunkGenerationStatus.FULL;
    }

    boolean isBlockLoaded(int x, int y, int z) {
        if (!GameConfig.isWorldCoordinateInside(x, y, z)) {
            return false;
        }
        return isChunkLoaded(worldToChunk(x), worldToChunk(z));
    }

    void fillLoadedChunksSnapshot(List<Chunk> chunks) {
        chunks.clear();
        if (chunks instanceof ArrayList) {
            ((ArrayList<Chunk>) chunks).ensureCapacity(loadedColumns.size() * GameConfig.SECTION_COUNT);
        }
        for (ChunkColumn column : loadedColumns.values()) {
            if (column == null || !column.generated || column.status != ChunkGenerationStatus.FULL) {
                continue;
            }
            for (Chunk chunk : column.sections) {
                chunks.add(chunk);
            }
        }
    }

    void initializeNoise() {
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
        worldGenerator = new WorldGenerator(seed, terrainPreset, activeDimensionId);
    }

    int activeDimensionId() {
        return activeDimensionId;
    }

    private int normalizeDimensionId(int dimensionId) {
        return GameConfig.isParadiseDimension(dimensionId)
            ? GameConfig.DIMENSION_PARADISE
            : GameConfig.DIMENSION_OVERWORLD;
    }

    private DimensionRuntimeState runtimeStateFor(int dimensionId) {
        int normalized = normalizeDimensionId(dimensionId);
        DimensionRuntimeState state = dimensionRuntimeStates.get(normalized);
        if (state == null) {
            state = new DimensionRuntimeState();
            dimensionRuntimeStates.put(normalized, state);
        }
        return state;
    }

    private void saveActiveDimensionRuntime() {
        DimensionRuntimeState state = runtimeStateFor(activeDimensionId);
        state.loadedColumns.clear();
        state.loadedColumns.putAll(loadedColumns);
        state.pendingColumns.clear();
        state.pendingColumns.putAll(pendingColumns);
        state.mobs.clear();
        state.mobs.addAll(mobs);
        state.populatedVillageCells.clear();
        state.populatedVillageCells.addAll(populatedVillageCells);
        state.droppedItems.clear();
        state.droppedItems.addAll(droppedItems);
        state.fallingBlocks.clear();
        state.fallingBlocks.addAll(fallingBlocks);
        state.chestContainers.clear();
        state.chestContainers.putAll(chestContainers);
        state.furnaces.clear();
        state.furnaces.putAll(furnaces);
        state.activeWaterCells.clear();
        state.activeWaterCells.addAll(activeWaterCells);
        state.activeLavaCells.clear();
        state.activeLavaCells.addAll(activeLavaCells);
        state.activeSandCells.clear();
        state.activeSandCells.addAll(activeSandCells);
        state.queuedWaterCells.clear();
        state.queuedWaterCells.addAll(queuedWaterCells);
        state.queuedLavaCells.clear();
        state.queuedLavaCells.addAll(queuedLavaCells);
        state.queuedSandCells.clear();
        state.queuedSandCells.addAll(queuedSandCells);
        state.activeWaterQueue.clear();
        state.activeWaterQueue.addAll(activeWaterQueue);
        state.activeLavaQueue.clear();
        state.activeLavaQueue.addAll(activeLavaQueue);
        state.activeSandQueue.clear();
        state.activeSandQueue.addAll(activeSandQueue);
        state.leafDecayTimers.clear();
        state.leafDecayTimers.putAll(leafDecayTimers);
        state.fallingBlockSources.clear();
        state.fallingBlockSources.addAll(fallingBlockSources);
        state.dirtyChunkSections.clear();
        state.dirtyChunkSections.addAll(dirtyChunkSections);
        state.pendingMeshDirtySections.clear();
        state.pendingMeshDirtySections.addAll(pendingMeshDirtySections);
        state.waterFlowCache.clear();
        state.waterFlowCache.putAll(waterFlowCache);
        state.streamingWarmupFrames = streamingWarmupFrames;
        state.lastStreamingChunkX = lastStreamingChunkX;
        state.lastStreamingChunkZ = lastStreamingChunkZ;
    }

    private void restoreActiveDimensionRuntime(int dimensionId) {
        DimensionRuntimeState state = runtimeStateFor(dimensionId);
        loadedColumns.clear();
        loadedColumns.putAll(state.loadedColumns);
        pendingColumns.clear();
        pendingColumns.putAll(state.pendingColumns);
        mobs.clear();
        mobs.addAll(state.mobs);
        populatedVillageCells.clear();
        populatedVillageCells.addAll(state.populatedVillageCells);
        droppedItems.clear();
        droppedItems.addAll(state.droppedItems);
        fallingBlocks.clear();
        fallingBlocks.addAll(state.fallingBlocks);
        chestContainers.clear();
        chestContainers.putAll(state.chestContainers);
        furnaces.clear();
        furnaces.putAll(state.furnaces);
        activeWaterCells.clear();
        activeWaterCells.addAll(state.activeWaterCells);
        activeLavaCells.clear();
        activeLavaCells.addAll(state.activeLavaCells);
        activeSandCells.clear();
        activeSandCells.addAll(state.activeSandCells);
        queuedWaterCells.clear();
        queuedWaterCells.addAll(state.queuedWaterCells);
        queuedLavaCells.clear();
        queuedLavaCells.addAll(state.queuedLavaCells);
        queuedSandCells.clear();
        queuedSandCells.addAll(state.queuedSandCells);
        activeWaterQueue.clear();
        activeWaterQueue.addAll(state.activeWaterQueue);
        activeLavaQueue.clear();
        activeLavaQueue.addAll(state.activeLavaQueue);
        activeSandQueue.clear();
        activeSandQueue.addAll(state.activeSandQueue);
        leafDecayTimers.clear();
        leafDecayTimers.putAll(state.leafDecayTimers);
        fallingBlockSources.clear();
        fallingBlockSources.addAll(state.fallingBlockSources);
        dirtyChunkSections.clear();
        dirtyChunkSections.addAll(state.dirtyChunkSections);
        pendingMeshDirtySections.clear();
        pendingMeshDirtySections.addAll(state.pendingMeshDirtySections);
        simulationDirtyColumns.clear();
        networkDirtyBlocks.clear();
        waterFlowCache.clear();
        waterFlowCache.putAll(state.waterFlowCache);
        streamingWarmupFrames = state.streamingWarmupFrames;
        lastStreamingChunkX = state.lastStreamingChunkX;
        lastStreamingChunkZ = state.lastStreamingChunkZ;
    }

    void setActiveDimension(int dimensionId) {
        int normalized = normalizeDimensionId(dimensionId);
        if (activeDimensionId == normalized) {
            return;
        }
        flushLoadedColumns();
        backgroundSaveService.flush();
        saveActiveDimensionRuntime();
        activeDimensionId = normalized;
        restoreActiveDimensionRuntime(activeDimensionId);
        worldGenerator = new WorldGenerator(seed, terrainPreset, activeDimensionId);
    }

    void setActiveDimensionFor(PlayerState player) {
        if (player != null) {
            setActiveDimension(player.dimensionId);
        }
    }

    void saveAllLoadedColumns() {
        flushLoadedColumns();
        backgroundSaveService.flush();
        saveContainers();
        saveMobs();
    }

    void discardLoadedWorld() {
        for (Future<ChunkColumn> future : pendingColumns.values()) {
            future.cancel(true);
        }
        pendingColumns.clear();
        flushLoadedColumns();
        backgroundSaveService.flush();
        saveMobs();
        loadedColumns.clear();
        activeWaterCells.clear();
        activeLavaCells.clear();
        activeSandCells.clear();
        clearDynamicBlockQueues();
        leafDecayTimers.clear();
        fallingBlockSources.clear();
        dirtyChunkSections.clear();
        pendingMeshDirtySections.clear();
        simulationDirtyColumns.clear();
        networkDirtyBlocks.clear();
        waterFlowCache.clear();
        mobs.clear();
        populatedVillageCells.clear();
        droppedItems.clear();
        fallingBlocks.clear();
        saveContainers();
        chestContainers.clear();
        furnaces.clear();
        remotePlayers.clear();
        dimensionRuntimeStates.clear();
        worldDirectory = null;
        saveDirectory = null;
        regionStorage = null;
        paradiseRegionStorage = null;
        networkMirrorMode = false;
    }

    void clearRuntimeWorld() {
        for (Future<ChunkColumn> future : pendingColumns.values()) {
            future.cancel(true);
        }
        pendingColumns.clear();
        loadedColumns.clear();
        activeWaterCells.clear();
        activeLavaCells.clear();
        activeSandCells.clear();
        clearDynamicBlockQueues();
        leafDecayTimers.clear();
        fallingBlockSources.clear();
        dirtyChunkSections.clear();
        pendingMeshDirtySections.clear();
        simulationDirtyColumns.clear();
        networkDirtyBlocks.clear();
        waterFlowCache.clear();
        mobs.clear();
        populatedVillageCells.clear();
        droppedItems.clear();
        fallingBlocks.clear();
        chestContainers.clear();
        furnaces.clear();
        remotePlayers.clear();
        dimensionRuntimeStates.clear();
        simulationAccumulator = 0.0;
        hostileMobSpawnCooldown = 20.0;
        worldTickCounter = 0L;
        worldTime = 0.30;
        mobUpdateCounter = 0L;
        droppedItemUpdateCounter = 0L;
        streamingWarmupFrames = 0;
        lastStreamingChunkX = Integer.MIN_VALUE;
        lastStreamingChunkZ = Integer.MIN_VALUE;
        streamingProfileLastLogNs = 0L;
        streamingProfileFrames = 0;
        streamingProfileSubmittedColumns = 0;
        streamingProfileIntegratedColumns = 0;
        streamingProfilePrepareNs = 0L;
        streamingProfileDrainNs = 0L;
        streamingProfileEnsureNs = 0L;
        streamingProfileUnloadNs = 0L;
        generationProfileColumns = 0;
        generationProfileNs = 0L;
        worldDirectory = null;
        saveDirectory = null;
        regionStorage = null;
        paradiseRegionStorage = null;
        networkMirrorMode = false;
    }

    void generateWorld() {
        flushLoadedColumns();
        backgroundSaveService.flush();
        loadedColumns.clear();
        pendingColumns.clear();
        activeWaterCells.clear();
        activeLavaCells.clear();
        activeSandCells.clear();
        clearDynamicBlockQueues();
        leafDecayTimers.clear();
        fallingBlockSources.clear();
        waterFlowCache.clear();
        dirtyChunkSections.clear();
        pendingMeshDirtySections.clear();
        simulationDirtyColumns.clear();
        networkDirtyBlocks.clear();
        mobs.clear();
        remotePlayers.clear();
        populatedVillageCells.clear();
        droppedItems.clear();
        fallingBlocks.clear();
        chestContainers.clear();
        furnaces.clear();
        dimensionRuntimeStates.clear();
        simulationAccumulator = 0.0;
        hostileMobSpawnCooldown = 20.0;
        worldTickCounter = 0L;
        worldTime = 0.30;
        mobUpdateCounter = 0L;
        droppedItemUpdateCounter = 0L;
        streamingWarmupFrames = 0;
        lastStreamingChunkX = Integer.MIN_VALUE;
        lastStreamingChunkZ = Integer.MIN_VALUE;

        try {
            Files.createDirectories(worldDirectory.resolve(GameConfig.SAVE_REGION_DIRECTORY));
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to create save directory: " + exception.getMessage());
            }
        }

        ensureColumnsAround(0, 0, GameConfig.INITIAL_CHUNK_SYNC_RADIUS, true);
    }

    void prepareForPlayer(PlayerState player) {
        if (player == null) {
            return;
        }
        setActiveDimensionFor(player);
        int playerChunkX = worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = worldToChunk((int) Math.floor(player.z));
        if (playerChunkX != lastStreamingChunkX || playerChunkZ != lastStreamingChunkZ) {
            lastStreamingChunkX = playerChunkX;
            lastStreamingChunkZ = playerChunkZ;
            streamingWarmupFrames = Math.min(streamingWarmupFrames, 80);
        }
        streamingWarmupFrames = Math.min(600, streamingWarmupFrames + 1);
        boolean profile = GameConfig.ENABLE_FRAME_PROFILING;
        long prepareStartNs = profile ? System.nanoTime() : 0L;
        int streamingRadius = getStreamingRadius(player);
        cancelFarPendingColumns(playerChunkX, playerChunkZ, streamingRadius + 4);
        long drainStartNs = profile ? System.nanoTime() : 0L;
        int integratedColumns = networkMirrorMode ? 0 : drainGeneratedColumns();
        long ensureStartNs = profile ? System.nanoTime() : 0L;
        int submittedColumns = networkMirrorMode ? 0 : ensureColumnsAround(playerChunkX, playerChunkZ, streamingRadius, false);
        long unloadStartNs = profile ? System.nanoTime() : 0L;
        unloadFarColumns(playerChunkX, playerChunkZ, streamingRadius + 2);
        cancelFarPendingColumns(playerChunkX, playerChunkZ, streamingRadius + 4);
        if (profile) {
            long endNs = System.nanoTime();
            logStreamingProfile(
                submittedColumns,
                integratedColumns,
                drainStartNs - prepareStartNs,
                ensureStartNs - drainStartNs,
                unloadStartNs - ensureStartNs,
                endNs - unloadStartNs
            );
        }
    }

    void primeStreamingAround(PlayerState player) {
        if (player == null) {
            return;
        }
        setActiveDimensionFor(player);
        if (networkMirrorMode) {
            return;
        }
        int playerChunkX = worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = worldToChunk((int) Math.floor(player.z));
        lastStreamingChunkX = playerChunkX;
        lastStreamingChunkZ = playerChunkZ;
        streamingWarmupFrames = Math.min(600, streamingWarmupFrames + 120);
        int streamingRadius = getStreamingRadius(player);
        drainGeneratedColumns(MAX_COLUMN_INTEGRATIONS_PER_FRAME * 2, COLUMN_INTEGRATION_BUDGET_NS * 2);
        ensureColumnsAround(playerChunkX, playerChunkZ, streamingRadius, false, MAX_INITIAL_COLUMN_SUBMISSIONS);
    }

    int loadedColumnCountAround(PlayerState player, int radius) {
        if (player == null) {
            return 0;
        }
        setActiveDimensionFor(player);
        int playerChunkX = worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = worldToChunk((int) Math.floor(player.z));
        int loaded = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (loadedColumns.containsKey(columnKey(playerChunkX + dx, playerChunkZ + dz))) {
                    loaded++;
                }
            }
        }
        return loaded;
    }

    int targetColumnCountForRadius(int radius) {
        int diameter = radius * 2 + 1;
        return diameter * diameter;
    }

    int pendingColumnCount() {
        return pendingColumns.size();
    }

    ColumnUpdateList updateWorldTicks(PlayerState player, double deltaTime) {
        setActiveDimensionFor(player);
        simulationDirtyColumns.clear();
        dirtyChunkSections.clear();
        drainGeneratedColumns();
        drainPendingMeshDirtySections();

        updateFallingBlocks(deltaTime);
        tickLeafDecay(deltaTime);
        updateFurnaces(deltaTime);

        simulationAccumulator += deltaTime;
        while (simulationAccumulator >= GameConfig.WORLD_TICK_INTERVAL) {
            simulationAccumulator -= GameConfig.WORLD_TICK_INTERVAL;
            tickDynamicBlocks(player);
            worldTickCounter++;
        }

        return simulationDirtyColumns;
    }

    ColumnUpdateList drainNetworkDirtyBlocks() {
        ColumnUpdateList drained = new ColumnUpdateList(Math.max(1, networkDirtyBlocks.size()));
        for (int i = 0; i < networkDirtyBlocks.size(); i++) {
            drained.add(networkDirtyBlocks.xAt(i), networkDirtyBlocks.yAt(i), networkDirtyBlocks.zAt(i));
        }
        networkDirtyBlocks.clear();
        return drained;
    }

    void clearNetworkDirtyBlocks() {
        networkDirtyBlocks.clear();
    }

    double getWorldTime() {
        return worldTime;
    }

    void setWorldTime(double worldTime) {
        this.worldTime = normalizeWorldTime(worldTime);
    }

    void advanceWorldTime(double deltaTime) {
        setWorldTime(worldTime + deltaTime / GameConfig.DAY_LENGTH_SECONDS);
    }

    void placePlayerAtSpawn(PlayerState player) {
        player.dimensionId = GameConfig.DIMENSION_OVERWORLD;
        setActiveDimension(player.dimensionId);
        int bestX = 0;
        int bestZ = 0;
        int bestY = Integer.MIN_VALUE;

        for (int radius = 0; radius <= SPAWN_LAND_SEARCH_RADIUS; radius += SPAWN_LAND_SEARCH_STEP) {
            boolean found = false;
            for (int offsetX = -radius; offsetX <= radius && !found; offsetX += SPAWN_LAND_SEARCH_STEP) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ += SPAWN_LAND_SEARCH_STEP) {
                    if (Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) {
                        continue;
                    }
                    int x = offsetX;
                    int z = offsetZ;
                    if (!worldGenerator.isLikelyDrySpawnColumn(x, z)) {
                        continue;
                    }
                    int[] spawn = findExactSpawnNear(x, z);
                    if (spawn != null) {
                        bestX = spawn[0];
                        bestY = spawn[1];
                        bestZ = spawn[2];
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                break;
            }
        }

        if (bestY == Integer.MIN_VALUE) {
            bestX = 0;
            bestZ = 0;
            ensureColumnGeneratedSync(0, 0);
            bestY = Math.max(GameConfig.WORLD_MIN_Y + 2, getSurfaceHeight(0, 0));
        }

        player.x = bestX + 0.5;
        player.z = bestZ + 0.5;
        player.y = getStandingY(player.x, player.z, bestY + 1.0);
    }

    private int[] findExactSpawnNear(int centerX, int centerZ) {
        for (int radius = 0; radius <= SPAWN_LAND_SEARCH_STEP; radius += 4) {
            for (int offsetX = -radius; offsetX <= radius; offsetX += 4) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ += 4) {
                    if (Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) {
                        continue;
                    }
                    int x = centerX + offsetX;
                    int z = centerZ + offsetZ;
                    int y = findExactSpawnAt(x, z);
                    if (y != Integer.MIN_VALUE) {
                        return new int[]{x, y, z};
                    }
                }
            }
        }
        return null;
    }

    private int findExactSpawnAt(int x, int z) {
        ChunkColumn spawnColumn = ensureColumnGeneratedSync(worldToChunk(x), worldToChunk(z));
        if (spawnColumn == null || !spawnColumn.generated) {
            return Integer.MIN_VALUE;
        }
        int surfaceY = findSpawnGroundY(x, z);
        byte ground = getBlock(x, surfaceY, z);
        if (surfaceY <= GameConfig.WORLD_MIN_Y
            || !isSpawnGroundBlock(ground)
            || GameConfig.isLiquidBlock(ground)
            || getBlock(x, surfaceY + 1, z) != GameConfig.AIR
            || getBlock(x, surfaceY + 2, z) != GameConfig.AIR) {
            return Integer.MIN_VALUE;
        }
        return surfaceY;
    }

    double safeStandingYAt(double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int groundY = findExactSpawnAt(blockX, blockZ);
        if (groundY == Integer.MIN_VALUE) {
            groundY = getSurfaceHeight(blockX, blockZ);
        }
        return getStandingY(x, z, groundY + 1.0);
    }

    boolean isParadiseArea(double x, double z) {
        return GameConfig.isParadiseDimension(activeDimensionId);
    }

    Path paradiseDimensionDirectory() {
        return worldDirectory == null ? null : paradiseDimensionDirectory(worldDirectory);
    }

    private static Path paradiseDimensionDirectory(Path rootDirectory) {
        return rootDirectory.resolve(GameConfig.SAVE_DIMENSIONS_DIRECTORY)
            .resolve(GameConfig.PARADISE_DIMENSION_DIRECTORY);
    }

    boolean isParadiseColumn(int chunkX, int chunkZ) {
        if (!GameConfig.isParadiseDimension(activeDimensionId)) {
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

    void placePlayerAtParadiseSpawn(PlayerState player) {
        if (player == null) {
            return;
        }
        int spawnX = GameConfig.PARADISE_ORIGIN_X;
        int spawnZ = GameConfig.PARADISE_ORIGIN_Z - 4;
        player.dimensionId = GameConfig.DIMENSION_PARADISE;
        setActiveDimension(player.dimensionId);
        ensureColumnGeneratedSync(worldToChunk(spawnX), worldToChunk(spawnZ));
        ensureColumnGeneratedSync(worldToChunk(GameConfig.PARADISE_ORIGIN_X), worldToChunk(GameConfig.PARADISE_ORIGIN_Z));
        player.x = spawnX + 0.5;
        player.z = spawnZ + 0.5;
        player.y = safeStandingYAt(player.x, player.z);
        player.verticalVelocity = 0.0;
        player.isGrounded = true;
    }

    boolean updateParadisePortalTravel(PlayerState player, double deltaTime) {
        if (player == null || player.spectatorMode) {
            return false;
        }
        player.paradisePortalCooldown = Math.max(0.0, player.paradisePortalCooldown - Math.max(0.0, deltaTime));
        if (player.paradisePortalCooldown > 0.0 || !isPlayerInsideParadisePortal(player)) {
            player.paradisePortalTimer = 0.0;
            return false;
        }
        player.paradisePortalTimer += Math.max(0.0, deltaTime);
        if (player.paradisePortalTimer < GameConfig.PARADISE_PORTAL_SECONDS) {
            return false;
        }
        if (GameConfig.isParadiseDimension(player.dimensionId)) {
            teleportPlayerFromParadise(player);
        } else {
            teleportPlayerToParadise(player);
        }
        player.paradisePortalTimer = 0.0;
        player.paradisePortalCooldown = GameConfig.PARADISE_PORTAL_COOLDOWN_SECONDS;
        player.capturePreviousPosition();
        return true;
    }

    private boolean isPlayerInsideParadisePortal(PlayerState player) {
        int blockX = (int) Math.floor(player.x);
        int blockZ = (int) Math.floor(player.z);
        int feetY = (int) Math.floor(player.y + 0.08);
        int headY = (int) Math.floor(player.y + player.eyeHeight());
        return getBlock(blockX, feetY, blockZ) == GameConfig.PARADISE_PORTAL
            || getBlock(blockX, headY, blockZ) == GameConfig.PARADISE_PORTAL;
    }

    private void teleportPlayerToParadise(PlayerState player) {
        player.hasParadiseReturn = true;
        player.paradiseReturnX = player.x;
        player.paradiseReturnY = player.y;
        player.paradiseReturnZ = player.z;
        player.paradiseReturnDimensionId = player.dimensionId;
        placePlayerAtParadiseSpawn(player);
    }

    private void teleportPlayerFromParadise(PlayerState player) {
        player.dimensionId = player.paradiseReturnDimensionId;
        if (GameConfig.isParadiseDimension(player.dimensionId)) {
            player.dimensionId = GameConfig.DIMENSION_OVERWORLD;
        }
        setActiveDimension(player.dimensionId);
        if (player.hasParadiseReturn
            && Double.isFinite(player.paradiseReturnX)
            && Double.isFinite(player.paradiseReturnY)
            && Double.isFinite(player.paradiseReturnZ)
            && isInside((int) Math.floor(player.paradiseReturnX), (int) Math.floor(player.paradiseReturnY), (int) Math.floor(player.paradiseReturnZ))) {
            ensureColumnGeneratedSync(worldToChunk((int) Math.floor(player.paradiseReturnX)), worldToChunk((int) Math.floor(player.paradiseReturnZ)));
            player.setPosition(player.paradiseReturnX, player.paradiseReturnY, player.paradiseReturnZ);
        } else {
            placePlayerAtSpawn(player);
        }
        player.verticalVelocity = 0.0;
        player.isGrounded = true;
    }

    private int findSpawnGroundY(int x, int z) {
        for (int y = GameConfig.WORLD_MAX_Y; y >= GameConfig.WORLD_MIN_Y + 1; y--) {
            byte block = getBlock(x, y, z);
            if (GameConfig.isLiquidBlock(block)) {
                continue;
            }
            if (isSpawnGroundBlock(block)) {
                return y;
            }
        }
        return GameConfig.WORLD_MIN_Y;
    }

    private boolean isSpawnGroundBlock(byte block) {
        return block == GameConfig.GRASS
            || block == GameConfig.SAND
            || block == GameConfig.STONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.COBBLESTONE;
    }

    boolean loadPlayerState(PlayerState player) {
        return loadPlayerState(player, null);
    }

    boolean loadPlayerState(PlayerState player, PlayerInventory inventory) {
        if (player == null || worldDirectory == null) {
            return false;
        }
        Path playerPath = worldDirectory.resolve(GameConfig.SAVE_PLAYER_FILE);
        if (!Files.isRegularFile(playerPath)) {
            return false;
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(playerPath)))) {
            double savedX = input.readDouble();
            double savedY = input.readDouble();
            double savedZ = input.readDouble();
            double savedYaw = input.readDouble();
            double savedPitch = input.readDouble();
            if (!Double.isFinite(savedX) || !Double.isFinite(savedY) || !Double.isFinite(savedZ)
                || !Double.isFinite(savedYaw) || !Double.isFinite(savedPitch)) {
                return false;
            }
            player.x = savedX;
            player.y = savedY;
            player.z = savedZ;
            player.yaw = savedYaw;
            player.pitch = savedPitch;
            if (input.available() >= 2) {
                int savedMode = input.readUnsignedByte();
                boolean savedFlightEnabled = input.readBoolean();
                player.creativeMode = savedMode == 1;
                player.spectatorMode = savedMode == 2;
                player.flightEnabled = savedFlightEnabled;
            } else {
                player.creativeMode = false;
                player.spectatorMode = false;
                player.flightEnabled = false;
            }
            if (input.available() >= 41) {
                player.health = clamp(input.readDouble(), 0.0, GameConfig.MAX_HEALTH);
                player.hunger = clamp(input.readDouble(), 0.0, GameConfig.MAX_HUNGER);
            } else {
                if (input.available() >= 4) {
                    player.health = clamp(input.readInt(), 0.0, GameConfig.MAX_HEALTH);
                }
                if (input.available() >= 4) {
                    player.hunger = clamp(input.readInt(), 0.0, GameConfig.MAX_HUNGER);
                }
            }
            if (input.available() >= 25) {
                player.hasCustomSpawn = input.readBoolean();
                player.spawnX = input.readDouble();
                player.spawnY = input.readDouble();
                player.spawnZ = input.readDouble();
            }
            while (input.available() >= 8) {
                int magic = input.readInt();
                int version = input.readInt();
                if (magic == PLAYER_INVENTORY_SAVE_MAGIC && version >= 1 && version <= PLAYER_INVENTORY_SAVE_VERSION) {
                    if (inventory != null) {
                        readPlayerInventory(input, inventory);
                    } else {
                        break;
                    }
                } else if (magic == PARADISE_PLAYER_STATE_MAGIC && version >= 1 && version <= PARADISE_PLAYER_STATE_VERSION) {
                    readParadisePlayerState(input, player, version);
                } else {
                    break;
                }
            }
            return true;
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to load player state: " + exception.getMessage());
            }
            return false;
        }
    }

    void savePlayerState(PlayerState player) {
        savePlayerState(player, null);
    }

    void savePlayerState(PlayerState player, PlayerInventory inventory) {
        if (player == null || worldDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(worldDirectory);
            Path playerPath = worldDirectory.resolve(GameConfig.SAVE_PLAYER_FILE);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(playerPath)))) {
                output.writeDouble(player.x);
                output.writeDouble(player.y);
                output.writeDouble(player.z);
                output.writeDouble(player.yaw);
                output.writeDouble(player.pitch);
                output.writeByte(player.spectatorMode ? 2 : (player.creativeMode ? 1 : 0));
                output.writeBoolean(player.flightEnabled);
                output.writeDouble(clamp(player.health, 0.0, GameConfig.MAX_HEALTH));
                output.writeDouble(clamp(player.hunger, 0.0, GameConfig.MAX_HUNGER));
                output.writeBoolean(player.hasCustomSpawn);
                output.writeDouble(player.spawnX);
                output.writeDouble(player.spawnY);
                output.writeDouble(player.spawnZ);
                writeParadisePlayerState(output, player);
                if (inventory != null) {
                    output.writeInt(PLAYER_INVENTORY_SAVE_MAGIC);
                    output.writeInt(PLAYER_INVENTORY_SAVE_VERSION);
                    writePlayerInventory(output, inventory);
                }
            }
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to save player state: " + exception.getMessage());
            }
        }
    }

    boolean loadNetworkPlayerState(UUID uuid, PlayerState player) {
        return loadNetworkPlayerState(uuid, player, null);
    }

    boolean loadNetworkPlayerState(UUID uuid, PlayerState player, PlayerInventory inventory) {
        if (uuid == null || player == null || worldDirectory == null) {
            return false;
        }
        Path playerPath = networkPlayerPath(uuid);
        if (!Files.isRegularFile(playerPath)) {
            return false;
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(playerPath)))) {
            double savedX = input.readDouble();
            double savedY = input.readDouble();
            double savedZ = input.readDouble();
            double savedYaw = input.readDouble();
            double savedPitch = input.readDouble();
            if (!Double.isFinite(savedX) || !Double.isFinite(savedY) || !Double.isFinite(savedZ)
                || !Double.isFinite(savedYaw) || !Double.isFinite(savedPitch)) {
                return false;
            }
            player.setPosition(savedX, savedY, savedZ);
            player.yaw = savedYaw;
            player.pitch = savedPitch;
            if (input.available() >= 2) {
                int savedMode = input.readUnsignedByte();
                player.creativeMode = savedMode == 1;
                player.spectatorMode = savedMode == 2;
                player.flightEnabled = input.readBoolean();
            }
            if (input.available() >= 16) {
                player.health = clamp(input.readDouble(), 0.0, GameConfig.MAX_HEALTH);
                player.hunger = clamp(input.readDouble(), 0.0, GameConfig.MAX_HUNGER);
            }
            while (input.available() >= 8) {
                int magic = input.readInt();
                int version = input.readInt();
                if (magic == NETWORK_PLAYER_INVENTORY_SAVE_MAGIC && version >= 1 && version <= NETWORK_PLAYER_INVENTORY_SAVE_VERSION) {
                    if (inventory != null) {
                        inventory.readFrom(input);
                    } else {
                        break;
                    }
                } else if (magic == PARADISE_PLAYER_STATE_MAGIC && version >= 1 && version <= PARADISE_PLAYER_STATE_VERSION) {
                    readParadisePlayerState(input, player, version);
                } else {
                    break;
                }
            }
            return true;
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to load network player state: " + exception.getMessage());
            }
            return false;
        }
    }

    void saveNetworkPlayerState(UUID uuid, PlayerState player) {
        saveNetworkPlayerState(uuid, player, null);
    }

    void saveNetworkPlayerState(UUID uuid, PlayerState player, PlayerInventory inventory) {
        if (uuid == null || player == null || worldDirectory == null) {
            return;
        }
        try {
            Path playersDirectory = worldDirectory.resolve("players");
            Files.createDirectories(playersDirectory);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(networkPlayerPath(uuid))))) {
                output.writeDouble(player.x);
                output.writeDouble(player.y);
                output.writeDouble(player.z);
                output.writeDouble(player.yaw);
                output.writeDouble(player.pitch);
                output.writeByte(player.spectatorMode ? 2 : (player.creativeMode ? 1 : 0));
                output.writeBoolean(player.flightEnabled);
                output.writeDouble(clamp(player.health, 0.0, GameConfig.MAX_HEALTH));
                output.writeDouble(clamp(player.hunger, 0.0, GameConfig.MAX_HUNGER));
                writeParadisePlayerState(output, player);
                if (inventory != null) {
                    output.writeInt(NETWORK_PLAYER_INVENTORY_SAVE_MAGIC);
                    output.writeInt(NETWORK_PLAYER_INVENTORY_SAVE_VERSION);
                    inventory.writeTo(output);
                }
            }
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to save network player state: " + exception.getMessage());
            }
        }
    }

    private Path networkPlayerPath(UUID uuid) {
        return worldDirectory.resolve("players").resolve(uuid.toString() + ".dat");
    }

    private void writeParadisePlayerState(DataOutputStream output, PlayerState player) throws IOException {
        output.writeInt(PARADISE_PLAYER_STATE_MAGIC);
        output.writeInt(PARADISE_PLAYER_STATE_VERSION);
        output.writeInt(player.dimensionId);
        output.writeBoolean(player.hasParadiseReturn);
        output.writeDouble(player.paradiseReturnX);
        output.writeDouble(player.paradiseReturnY);
        output.writeDouble(player.paradiseReturnZ);
        output.writeInt(player.paradiseReturnDimensionId);
    }

    private void readParadisePlayerState(DataInputStream input, PlayerState player, int version) throws IOException {
        player.dimensionId = version >= 2 ? input.readInt() : GameConfig.DIMENSION_OVERWORLD;
        player.hasParadiseReturn = input.readBoolean();
        player.paradiseReturnX = input.readDouble();
        player.paradiseReturnY = input.readDouble();
        player.paradiseReturnZ = input.readDouble();
        player.paradiseReturnDimensionId = input.available() >= 4 ? input.readInt() : GameConfig.DIMENSION_OVERWORLD;
    }

    private void loadContainers() {
        chestContainers.clear();
        furnaces.clear();
        if (worldDirectory == null) {
            return;
        }
        Path path = worldDirectory.resolve("containers.dat");
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int header = input.readInt();
            int version = header == CONTAINER_SAVE_MAGIC ? input.readInt() : header;
            if (version < 1 || version > CONTAINER_SAVE_VERSION) {
                return;
            }
            int chestCount = input.readInt();
            for (int i = 0; i < chestCount; i++) {
                long key = input.readLong();
                ContainerInventory container = new ContainerInventory(27);
                for (ItemStack stack : container.slots) {
                    readStack(input, stack, version);
                }
                chestContainers.put(key, container);
            }
            int furnaceCount = input.readInt();
            for (int i = 0; i < furnaceCount; i++) {
                long key = input.readLong();
                FurnaceBlockEntity furnace = new FurnaceBlockEntity();
                readStack(input, furnace.input, version);
                readStack(input, furnace.fuel, version);
                readStack(input, furnace.output, version);
                furnace.burnRemaining = input.readDouble();
                furnace.burnTotal = input.readDouble();
                furnace.cookProgress = input.readDouble();
                furnace.cookTotal = input.readDouble();
                furnaces.put(key, furnace);
            }
        } catch (IOException ignored) {
            chestContainers.clear();
            furnaces.clear();
        }
    }

    private void saveContainers() {
        if (worldDirectory == null) {
            return;
        }
        try {
            Files.createDirectories(worldDirectory);
            Path path = worldDirectory.resolve("containers.dat");
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
                output.writeInt(CONTAINER_SAVE_MAGIC);
                output.writeInt(CONTAINER_SAVE_VERSION);
                ArrayList<Map.Entry<Long, ContainerInventory>> nonEmptyChests = new ArrayList<>();
                for (Map.Entry<Long, ContainerInventory> entry : chestContainers.entrySet()) {
                    if (!isContainerEmpty(entry.getValue())) {
                        nonEmptyChests.add(entry);
                    }
                }
                ArrayList<Map.Entry<Long, FurnaceBlockEntity>> nonEmptyFurnaces = new ArrayList<>();
                for (Map.Entry<Long, FurnaceBlockEntity> entry : furnaces.entrySet()) {
                    if (!isFurnaceEmpty(entry.getValue())) {
                        nonEmptyFurnaces.add(entry);
                    }
                }
                output.writeInt(nonEmptyChests.size());
                for (Map.Entry<Long, ContainerInventory> entry : nonEmptyChests) {
                    output.writeLong(entry.getKey());
                    for (ItemStack stack : entry.getValue().slots) {
                        writeStack(output, stack);
                    }
                }
                output.writeInt(nonEmptyFurnaces.size());
                for (Map.Entry<Long, FurnaceBlockEntity> entry : nonEmptyFurnaces) {
                    FurnaceBlockEntity furnace = entry.getValue();
                    output.writeLong(entry.getKey());
                    writeStack(output, furnace.input);
                    writeStack(output, furnace.fuel);
                    writeStack(output, furnace.output);
                    output.writeDouble(furnace.burnRemaining);
                    output.writeDouble(furnace.burnTotal);
                    output.writeDouble(furnace.cookProgress);
                    output.writeDouble(furnace.cookTotal);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void writeStack(DataOutputStream output, ItemStack stack) throws IOException {
        output.writeByte(stack == null || stack.isEmpty() ? GameConfig.AIR : stack.itemId);
        output.writeInt(stack == null || stack.isEmpty() ? 0 : stack.count);
        output.writeInt(stack == null || stack.isEmpty() ? 0 : stack.durabilityDamage);
    }

    private boolean isContainerEmpty(ContainerInventory container) {
        if (container == null) {
            return true;
        }
        for (ItemStack stack : container.slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isFurnaceEmpty(FurnaceBlockEntity furnace) {
        return furnace == null
            || (furnace.input.isEmpty()
            && furnace.fuel.isEmpty()
            && furnace.output.isEmpty()
            && furnace.burnRemaining <= 0.0
            && furnace.cookProgress <= 0.0);
    }

    private void readStack(DataInputStream input, ItemStack stack, int version) throws IOException {
        byte item = input.readByte();
        int count = input.readInt();
        stack.set(item, count);
        if (version >= 3) {
            int damage = input.readInt();
            if (InventoryItems.isDurableItem(item)) {
                stack.durabilityDamage = Math.max(0, Math.min(damage, InventoryItems.maxDurability(item) - 1));
            } else {
                stack.durabilityDamage = 0;
            }
        }
    }

    private void loadMobs() {
        mobs.clear();
        if (worldDirectory == null) {
            return;
        }
        Path path = worldDirectory.resolve("mobs.dat");
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = input.readInt();
            if (magic != MOB_SAVE_MAGIC) {
                return;
            }
            int version = input.readInt();
            if (version < 1 || version > MOB_SAVE_VERSION) {
                return;
            }
            int count = Math.max(0, Math.min(input.readInt(), 512));
            for (int i = 0; i < count; i++) {
                MobKind kind = mobKindFromOrdinal(input.readInt());
                double x = input.readDouble();
                double y = input.readDouble();
                double z = input.readDouble();
                double homeX = input.readDouble();
                double homeZ = input.readDouble();
                MobEntity mob = new MobEntity(kind, x, y, z, homeX, homeZ, worldRandom);
                mob.health = Math.max(0.0, Math.min(input.readDouble(), maxMobHealth(kind)));
                mob.velocityX = input.readDouble();
                mob.velocityZ = input.readDouble();
                mob.verticalVelocity = input.readDouble();
                mob.isGrounded = input.readBoolean();
                mob.bodyYaw = input.readDouble();
                mob.targetBodyYaw = input.readDouble();
                mob.wanderDirection = input.readDouble();
                mob.wanderTime = input.readDouble();
                mob.directionIndex = input.readInt();
                mob.attackCooldown = input.readDouble();
                mob.hurtCooldown = input.readDouble();
                mob.fleeTimer = input.readDouble();
                mob.loveTimer = input.readDouble();
                mob.breedCooldown = input.readDouble();
                if (version >= 2) {
                    mob.walkCycle = input.readDouble();
                    mob.resting = input.readBoolean();
                    mob.fireTimer = input.readDouble();
                    mob.fireDamageTimer = input.readDouble();
                    mob.wasInWater = input.readBoolean();
                    mob.aiTickOffset = input.readInt();
                }
                if (version >= 3) {
                    mob.babyAge = input.readDouble();
                }
                if (mob.health > 0.0 && isInside((int) Math.floor(mob.x), (int) Math.floor(mob.y), (int) Math.floor(mob.z))) {
                    mobs.add(mob);
                }
            }
        } catch (IOException ignored) {
            mobs.clear();
        }
    }

    private void saveMobs() {
        if (worldDirectory == null) {
            return;
        }
        try {
            Files.createDirectories(worldDirectory);
            Path path = worldDirectory.resolve("mobs.dat");
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
                output.writeInt(MOB_SAVE_MAGIC);
                output.writeInt(MOB_SAVE_VERSION);
                int count = 0;
                for (MobEntity mob : mobs) {
                    if (mob != null && mob.health > 0.0) {
                        count++;
                    }
                }
                output.writeInt(count);
                for (MobEntity mob : mobs) {
                    if (mob == null || mob.health <= 0.0) {
                        continue;
                    }
                    output.writeInt(mob.kind.ordinal());
                    output.writeDouble(mob.x);
                    output.writeDouble(mob.y);
                    output.writeDouble(mob.z);
                    output.writeDouble(mob.homeX);
                    output.writeDouble(mob.homeZ);
                    output.writeDouble(mob.health);
                    output.writeDouble(mob.velocityX);
                    output.writeDouble(mob.velocityZ);
                    output.writeDouble(mob.verticalVelocity);
                    output.writeBoolean(mob.isGrounded);
                    output.writeDouble(mob.bodyYaw);
                    output.writeDouble(mob.targetBodyYaw);
                    output.writeDouble(mob.wanderDirection);
                    output.writeDouble(mob.wanderTime);
                    output.writeInt(mob.directionIndex);
                    output.writeDouble(mob.attackCooldown);
                    output.writeDouble(mob.hurtCooldown);
                    output.writeDouble(mob.fleeTimer);
                    output.writeDouble(mob.loveTimer);
                    output.writeDouble(mob.breedCooldown);
                    output.writeDouble(mob.walkCycle);
                    output.writeBoolean(mob.resting);
                    output.writeDouble(mob.fireTimer);
                    output.writeDouble(mob.fireDamageTimer);
                    output.writeBoolean(mob.wasInWater);
                    output.writeInt(mob.aiTickOffset);
                    output.writeDouble(mob.babyAge);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private MobKind mobKindFromOrdinal(int ordinal) {
        MobKind[] values = MobKind.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MobKind.ZOMBIE;
    }

    private double maxMobHealth(MobKind kind) {
        if (kind == MobKind.HERRING || kind == MobKind.SALMON) {
            return 4.0;
        }
        return kind == MobKind.COW || kind == MobKind.SHEEP || kind == MobKind.PIG ? 10.0 : 20.0;
    }

    void updateMobs(PlayerState player, double deltaTime) {
        if (player == null) {
            return;
        }
        setActiveDimensionFor(player);

        mobUpdateCounter++;
        hostileMobSpawnCooldown -= deltaTime;
        fishSpawnCooldown -= deltaTime;
        populateVillageResidentsNear(player);
        maybeSpawnZombieNearPlayer(player);
        maybeSpawnFishNearPlayer(player);

        int aiUpdates = 0;
        for (int i = mobs.size() - 1; i >= 0; i--) {
            MobEntity mob = mobs.get(i);
            double despawnDistance = mob.kind == MobKind.VILLAGER ? 220.0 : (isFishMob(mob) ? 80.0 : 96.0);
            double playerDistanceSquared = distanceSquared(mob.x, mob.z, player.x, player.z);
            if (mob.health <= 0) {
                dropMobLoot(mob);
                mobs.remove(i);
                continue;
            }
            if (playerDistanceSquared > despawnDistance * despawnDistance) {
                mobs.remove(i);
                continue;
            }

            boolean inWater = intersectsFluid(
                mob.x, mob.y, mob.z,
                mob.radius(), mob.height(), GameConfig.WATER
            );
            boolean inLava = intersectsFluid(
                mob.x, mob.y, mob.z,
                mob.radius(), mob.height(), GameConfig.LAVA
            );
            if (inWater != mob.wasInWater) {
                mob.splashQueued = true;
            }
            mob.wasInWater = inWater;

            if (inLava) {
                mob.fireTimer = Math.max(mob.fireTimer, 3.0);
                mob.fireDamageTimer -= deltaTime;
                if (mob.fireDamageTimer <= 0.0) {
                    mob.health -= GameConfig.LAVA_DAMAGE;
                    mob.fireDamageTimer = GameConfig.LAVA_DAMAGE_INTERVAL;
                }
            } else if (isFishMob(mob) && !inWater) {
                mob.fireDamageTimer -= deltaTime;
                if (mob.fireDamageTimer <= 0.0) {
                    mob.health -= 1.0;
                    mob.fireDamageTimer = 0.75;
                }
            } else if (isSunBurningMob(mob, inWater)) {
                mob.fireTimer = Math.max(mob.fireTimer, 1.2);
                mob.fireDamageTimer -= deltaTime;
                if (mob.fireDamageTimer <= 0.0) {
                    mob.health -= GameConfig.FIRE_DAMAGE;
                    mob.fireDamageTimer = GameConfig.FIRE_DAMAGE_INTERVAL;
                }
            } else {
                mob.fireTimer = Math.max(0.0, mob.fireTimer - deltaTime);
            }
            mob.hurtCooldown = Math.max(0.0, mob.hurtCooldown - deltaTime);
            if (touchesBlock(mob.x, mob.y, mob.z, mob.radius() + 0.06, mob.height(), GameConfig.CACTUS) && mob.hurtCooldown <= 0.0) {
                mob.health -= 1.0;
                mob.hurtCooldown = 0.65;
            }
            mob.fleeTimer = Math.max(0.0, mob.fleeTimer - deltaTime);
            mob.loveTimer = Math.max(0.0, mob.loveTimer - deltaTime);
            mob.breedCooldown = Math.max(0.0, mob.breedCooldown - deltaTime);
            mob.babyAge = Math.max(0.0, mob.babyAge - deltaTime);

            int aiInterval = zombieAiInterval(playerDistanceSquared);
            boolean nearMob = aiInterval <= GameConfig.ZOMBIE_NEAR_AI_TICKS;
            boolean scheduledAi = Math.floorMod(mobUpdateCounter + mob.aiTickOffset, aiInterval) == 0;
            if (nearMob || (scheduledAi && aiUpdates < MAX_MOB_AI_UPDATES_PER_FRAME)) {
                updateMobAi(mob, player, deltaTime * aiInterval);
                aiUpdates++;
            }
            updateMobMovement(mob, inWater, deltaTime);
            if (isHostileMob(mob)) {
                maybeQueueHostileMobGrowl(mob, player, deltaTime);
                tryHostileMobAttack(mob, player);
            } else {
                maybeQueuePassiveMobSound(mob, deltaTime);
            }
        }
    }

    private boolean isHostileMob(MobEntity mob) {
        return mob.kind == MobKind.ZOMBIE || mob.kind == MobKind.SKELETON;
    }

    private boolean isPassiveMob(MobEntity mob) {
        return mob != null && (mob.kind == MobKind.PIG || mob.kind == MobKind.SHEEP || mob.kind == MobKind.COW);
    }

    private boolean isFishMob(MobEntity mob) {
        return mob != null && (mob.kind == MobKind.HERRING || mob.kind == MobKind.SALMON);
    }

    private int zombieAiInterval(double playerDistanceSquared) {
        if (playerDistanceSquared <= GameConfig.ZOMBIE_NEAR_AI_DISTANCE * GameConfig.ZOMBIE_NEAR_AI_DISTANCE) {
            return GameConfig.ZOMBIE_NEAR_AI_TICKS;
        }
        if (playerDistanceSquared <= GameConfig.ZOMBIE_MID_AI_DISTANCE * GameConfig.ZOMBIE_MID_AI_DISTANCE) {
            return GameConfig.ZOMBIE_MID_AI_TICKS;
        }
        return GameConfig.ZOMBIE_FAR_AI_TICKS;
    }

    private void dropMobLoot(MobEntity mob) {
        if (mob == null || mob.kind == MobKind.VILLAGER) {
            return;
        }
        switch (mob.kind) {
            case PIG:
                spawnDroppedItem(InventoryItems.RAW_PORK, 1 + worldRandom.nextInt(2), mob.x, mob.y + 0.45, mob.z);
                break;
            case COW:
                spawnDroppedItem(InventoryItems.RAW_BEEF, 1 + worldRandom.nextInt(2), mob.x, mob.y + 0.45, mob.z);
                if (worldRandom.nextDouble() < 0.55) {
                    spawnDroppedItem(InventoryItems.LEATHER, 1, mob.x, mob.y + 0.45, mob.z);
                }
                break;
            case SHEEP:
                spawnDroppedItem(InventoryItems.WOOL, 1, mob.x, mob.y + 0.45, mob.z);
                if (worldRandom.nextDouble() < 0.65) {
                    spawnDroppedItem(InventoryItems.RAW_MUTTON, 1, mob.x, mob.y + 0.45, mob.z);
                }
                break;
            case HERRING:
                spawnDroppedItem(InventoryItems.RAW_HERRING, 1, mob.x, mob.y + 0.18, mob.z);
                break;
            case SALMON:
                spawnDroppedItem(InventoryItems.RAW_SALMON, 1, mob.x, mob.y + 0.18, mob.z);
                break;
            case SKELETON:
                spawnDroppedItem(InventoryItems.BONE, 1 + worldRandom.nextInt(2), mob.x, mob.y + 0.45, mob.z);
                break;
            case ZOMBIE:
                if (worldRandom.nextDouble() < 0.75) {
                    spawnDroppedItem(InventoryItems.ROTTEN_FLESH, 1, mob.x, mob.y + 0.45, mob.z);
                }
                break;
            default:
                break;
        }
    }

    private void populateVillageResidentsNear(PlayerState player) {
        if (player == null || mobs.size() > 96) {
            return;
        }
        int villageCellSize = getVillageCellSizeChunks();
        int playerChunkX = worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = worldToChunk((int) Math.floor(player.z));
        int playerCellX = Math.floorDiv(playerChunkX, villageCellSize);
        int playerCellZ = Math.floorDiv(playerChunkZ, villageCellSize);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int cellX = playerCellX + dx;
                int cellZ = playerCellZ + dz;
                long key = (((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL);
                if (populatedVillageCells.contains(key)) {
                    continue;
                }
                int[] center = getVillageCenterForCell(cellX, cellZ);
                if (center == null) {
                    continue;
                }
                if (distanceSquared(player.x, player.z, center[0], center[2]) > 180.0 * 180.0) {
                    continue;
                }
                int spawned = spawnVillageResidents(center[0], center[1], center[2], mix64(seed ^ key));
                if (spawned > 0) {
                    populatedVillageCells.add(key);
                }
            }
        }
    }

    private int spawnVillageResidents(int centerX, int centerY, int centerZ, long villageSeed) {
        int count = 2 + (int) Math.round(randomUnit(villageSeed ^ 0xA11CE5L) * 3.0);
        int spawned = 0;
        int[][] offsets = {
            {0, 0}, {-8, -6}, {9, -5}, {-10, 8}, {8, 9}, {14, 2}, {-14, -1}
        };
        for (int attempt = 0; attempt < offsets.length && spawned < count; attempt++) {
            int x = centerX + offsets[attempt][0] + (int) Math.round((randomUnit(villageSeed ^ (attempt * 31L)) - 0.5) * 4.0);
            int z = centerZ + offsets[attempt][1] + (int) Math.round((randomUnit(villageSeed ^ (attempt * 57L)) - 0.5) * 4.0);
            ensureColumnLoadedSync(worldToChunk(x), worldToChunk(z));
            int surfaceY = getSurfaceHeight(x, z);
            if (Math.abs(surfaceY - centerY) > 6
                || GameConfig.isLiquidBlock(getBlock(x, surfaceY, z))
                || getBlock(x, surfaceY + 1, z) != GameConfig.AIR
                || getBlock(x, surfaceY + 2, z) != GameConfig.AIR
                || collides(x + 0.5, surfaceY + 1.01, z + 0.5, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT)) {
                continue;
            }
            mobs.add(new MobEntity(MobKind.VILLAGER, x + 0.5, surfaceY + 1.01, z + 0.5, centerX + 0.5, centerZ + 0.5, new Random(villageSeed ^ attempt)));
            spawned++;
        }
        return spawned;
    }

    private double randomUnit(long bits) {
        long mantissa = (bits >>> 11) & ((1L << 53) - 1L);
        return mantissa / (double) (1L << 53);
    }

    private boolean isSunBurningMob(MobEntity mob, boolean inWater) {
        return isHostileMob(mob)
            && isDaytime()
            && !inWater
            && isExposedToSky((int) Math.floor(mob.x), (int) Math.floor(mob.y + mob.height()), (int) Math.floor(mob.z));
    }

    private boolean isDaytime() {
        return worldTime > 0.18 && worldTime < 0.78;
    }

    private boolean isExposedToSky(int x, int y, int z) {
        for (int checkY = Math.max(y, GameConfig.WORLD_MIN_Y); checkY <= GameConfig.WORLD_MAX_Y; checkY++) {
            byte block = getBlock(x, checkY, z);
            if (Blocks.isOpaque(block)
                && block != GameConfig.OAK_LEAVES
                && block != GameConfig.PINE_LEAVES
                && block != GameConfig.BIRCH_LEAVES) {
                return false;
            }
        }
        return true;
    }

    boolean breakBlock(RayHit hit) {
        if (hit == null) {
            return false;
        }
        byte targetBlock = getBlock(hit.x, hit.y, hit.z);
        if ((GameConfig.isLiquidBlock(targetBlock) && !isAquaticPlantBlock(targetBlock))
            || targetBlock == GameConfig.BEDROCK
            || targetBlock == GameConfig.AIR) {
            return false;
        }

        if (targetBlock == GameConfig.OAK_DOOR) {
            removeDoorAt(hit.x, hit.y, hit.z);
        } else if (targetBlock == GameConfig.RED_BED) {
            removeBedAt(hit.x, hit.y, hit.z);
        } else {
            if (targetBlock == GameConfig.CHEST) {
                dropChestContents(hit.x, hit.y, hit.z);
            } else if (targetBlock == GameConfig.FURNACE) {
                dropFurnaceContents(hit.x, hit.y, hit.z);
            }
            setBlockState(hit.x, hit.y, hit.z, GameConfig.AIR, -1);
        }
        if (targetBlock == GameConfig.OAK_LOG || targetBlock == GameConfig.PINE_LOG || targetBlock == GameConfig.BIRCH_LOG) {
            scheduleUnsupportedLeavesAround(hit.x, hit.y, hit.z);
        }
        if (targetBlock == GameConfig.GLASS || targetBlock == GameConfig.PARADISE_PORTAL) {
            refreshParadisePortalsNear(hit.x, hit.y, hit.z);
        }
        updatePlantSupportAt(hit.x, hit.y + 1, hit.z);
        updateSnowSupportAt(hit.x, hit.y + 1, hit.z);
        updateDoorSupportAt(hit.x, hit.y + 1, hit.z);
        refreshDynamicCellsAround(hit.x, hit.y, hit.z);
        markDirtyBlock(hit.x, hit.y, hit.z);
        refreshSurfaceHeight(hit.x, hit.z);
        return true;
    }

    private void scheduleUnsupportedLeavesAround(int x, int y, int z) {
        int radius = 6;
        for (int leafY = y - radius; leafY <= y + radius; leafY++) {
            for (int leafZ = z - radius; leafZ <= z + radius; leafZ++) {
                for (int leafX = x - radius; leafX <= x + radius; leafX++) {
                    byte block = getBlock(leafX, leafY, leafZ);
                    if (!isLeafBlock(block) || hasNearbyLog(leafX, leafY, leafZ, 4)) {
                        continue;
                    }
                    double distance = Math.sqrt(square(leafX - x) + square(leafY - y) + square(leafZ - z));
                    leafDecayTimers.put(packBlock(leafX, leafY, leafZ), 4.0 + distance * 0.9 + worldRandom.nextDouble() * 8.0);
                }
            }
        }
    }

    private void writePlayerInventory(DataOutputStream output, PlayerInventory inventory) throws IOException {
        for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
            writeStack(output, inventory.getHotbarStack(i));
        }
        for (int i = 0; i < PlayerInventory.STORAGE_SIZE; i++) {
            writeStack(output, inventory.getStorageStack(i));
        }
        for (int i = 0; i < PlayerInventory.ARMOR_SIZE; i++) {
            writeStack(output, inventory.getArmorStack(i));
        }
        writeStack(output, inventory.getOffhandStack());
    }

    private void readPlayerInventory(DataInputStream input, PlayerInventory inventory) throws IOException {
        inventory.clearAll();
        for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
            readStack(input, inventory.getHotbarStack(i), CONTAINER_SAVE_VERSION);
        }
        for (int i = 0; i < PlayerInventory.STORAGE_SIZE; i++) {
            readStack(input, inventory.getStorageStack(i), CONTAINER_SAVE_VERSION);
        }
        for (int i = 0; i < PlayerInventory.ARMOR_SIZE; i++) {
            readStack(input, inventory.getArmorStack(i), CONTAINER_SAVE_VERSION);
        }
        readStack(input, inventory.getOffhandStack(), CONTAINER_SAVE_VERSION);
    }

    private void tickLeafDecay(double deltaTime) {
        if (leafDecayTimers.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, Double>> iterator = leafDecayTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Double> entry = iterator.next();
            long blockKey = entry.getKey();
            int x = unpackBlockX(blockKey);
            int y = unpackBlockY(blockKey);
            int z = unpackBlockZ(blockKey);
            if (!isLeafBlock(getBlock(x, y, z)) || hasNearbyLog(x, y, z, 4)) {
                iterator.remove();
                continue;
            }
            double remaining = entry.getValue() - deltaTime;
            if (remaining > 0.0) {
                entry.setValue(remaining);
                continue;
            }
            setBlockState(x, y, z, GameConfig.AIR, -1);
            updatePlantSupportAt(x, y + 1, z);
            refreshDynamicCellsAround(x, y, z);
            markDirtyBlock(x, y, z);
            refreshSurfaceHeight(x, z);
            iterator.remove();
        }
    }

    private int square(int value) {
        return value * value;
    }

    private boolean hasNearbyLog(int x, int y, int z, int radius) {
        for (int logY = y - radius; logY <= y + radius; logY++) {
            for (int logZ = z - radius; logZ <= z + radius; logZ++) {
                for (int logX = x - radius; logX <= x + radius; logX++) {
                    if (isLogBlock(getBlock(logX, logY, logZ))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isLeafBlock(byte block) {
        return block == GameConfig.OAK_LEAVES || block == GameConfig.PINE_LEAVES || block == GameConfig.BIRCH_LEAVES;
    }

    private boolean isLogBlock(byte block) {
        return block == GameConfig.OAK_LOG || block == GameConfig.PINE_LOG || block == GameConfig.BIRCH_LOG;
    }

    boolean interactBlock(RayHit hit, PlayerState player, boolean shiftDown) {
        if (hit == null) {
            return false;
        }
        byte block = getBlock(hit.x, hit.y, hit.z);
        if (block == GameConfig.OAK_DOOR) {
            toggleDoorAt(hit.x, hit.y, hit.z);
            return true;
        }
        if (block == GameConfig.OAK_FENCE_GATE) {
            toggleFenceGateAt(hit.x, hit.y, hit.z);
            return true;
        }
        if (block == GameConfig.RED_BED) {
            BlockState bed = getBlockState(hit.x, hit.y, hit.z);
            int facing = Blocks.bedFacing(bed);
            int footX = Blocks.isBedHead(bed) ? hit.x - facingDx(facing) : hit.x;
            int footZ = Blocks.isBedHead(bed) ? hit.z - facingDz(facing) : hit.z;
            player.hasCustomSpawn = true;
            player.spawnX = footX + 0.5;
            player.spawnY = hit.y + 1.01;
            player.spawnZ = footZ + 0.5;
            if (!isDaytime()) {
                setWorldTime(0.30);
            }
            return true;
        }
        if (block == GameConfig.STRUCTURE_BLOCK) {
            return interactStructureBlock(hit, player, shiftDown);
        }
        return false;
    }

    ContainerInventory chestContainerAt(int x, int y, int z) {
        long key = packBlock(x, y, z);
        ContainerInventory container = chestContainers.get(key);
        if (container == null) {
            container = new ContainerInventory(27);
            fillGeneratedChestLoot(container, x, y, z);
            chestContainers.put(key, container);
        }
        return container;
    }

    FurnaceBlockEntity furnaceAt(int x, int y, int z) {
        long key = packBlock(x, y, z);
        FurnaceBlockEntity furnace = furnaces.get(key);
        if (furnace == null) {
            furnace = new FurnaceBlockEntity();
            furnaces.put(key, furnace);
        }
        return furnace;
    }

    private void dropChestContents(int x, int y, int z) {
        ContainerInventory container = chestContainerAt(x, y, z);
        for (ItemStack stack : container.slots) {
            if (!stack.isEmpty()) {
                spawnDroppedItem(stack.itemId, stack.count, stack.durabilityDamage, x + 0.5, y + 0.65, z + 0.5);
                stack.clear();
            }
        }
        chestContainers.remove(packBlock(x, y, z));
    }

    private void dropFurnaceContents(int x, int y, int z) {
        FurnaceBlockEntity furnace = furnaces.remove(packBlock(x, y, z));
        if (furnace == null) {
            return;
        }
        dropStack(furnace.input, x, y, z);
        dropStack(furnace.fuel, x, y, z);
        dropStack(furnace.output, x, y, z);
    }

    private void dropStack(ItemStack stack, int x, int y, int z) {
        if (stack != null && !stack.isEmpty()) {
            spawnDroppedItem(stack.itemId, stack.count, stack.durabilityDamage, x + 0.5, y + 0.65, z + 0.5);
            stack.clear();
        }
    }

    private boolean interactStructureBlock(RayHit hit, PlayerState player, boolean shiftDown) {
        int x = hit.x;
        int y = hit.y;
        int z = hit.z;
        BlockState state = getBlockState(x, y, z);
        int templateIndex = state.data & 0xFF;
        int rotation = (state.data >>> 8) & 3;
        boolean topClick = hit.previousY > y;
        if (topClick) {
            String template = StructureTemplates.nameAt(templateIndex);
            int placeX = x + 2;
            int placeY = Math.max(GameConfig.WORLD_MIN_Y + 1, y);
            int placeZ = z;
            StructureTemplates.place(this, template, placeX, placeY, placeZ, rotation);
        } else if (shiftDown) {
            rotation = (rotation + 1) & 3;
        } else {
            templateIndex = (templateIndex + 1) % StructureTemplates.NAMES.size();
        }
        setBlockState(x, y, z, new BlockState(Blocks.typeFromLegacyId(GameConfig.STRUCTURE_BLOCK), templateIndex | (rotation << 8)));
        markDirtyBlock(x, y, z);
        refreshDynamicCellsAround(x, y, z);
        return true;
    }

    String placeStructureTemplate(String name, int originX, int originY, int originZ, int rotation) {
        String template = StructureTemplates.normalize(name);
        if (!StructureTemplates.exists(template)) {
            return "Unknown structure '" + name + "'. Available: " + StructureTemplates.listNames();
        }
        if (!isInside(originX, originY, originZ)) {
            return "Cannot place structure outside world.";
        }
        StructureTemplates.place(this, template, originX, originY, originZ, rotation);
        return "Placed structure " + template + " rotation=" + (((rotation % 4) + 4) & 3) + ".";
    }

    String structureTemplateList() {
        return StructureTemplates.listNames();
    }

    boolean activateParadisePortalAtFrame(int x, int y, int z) {
        for (int axis = 0; axis < 2; axis++) {
            for (int widthOffset = 0; widthOffset < 4; widthOffset++) {
                for (int heightOffset = 0; heightOffset < 5; heightOffset++) {
                    int originX = axis == 0 ? x - widthOffset : x;
                    int originZ = axis == 0 ? z : z - widthOffset;
                    int originY = y - heightOffset;
                    if (!isValidParadisePortalFrame(originX, originY, originZ, axis, true)) {
                        continue;
                    }
                    if (isCompleteParadisePortal(originX, originY, originZ, axis)) {
                        return false;
                    }
                    fillParadisePortalInterior(originX, originY, originZ, axis);
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshParadisePortalsNear(int x, int y, int z) {
        for (int axis = 0; axis < 2; axis++) {
            for (int originY = y - 4; originY <= y; originY++) {
                for (int widthOffset = -1; widthOffset <= 3; widthOffset++) {
                    for (int depthOffset = -1; depthOffset <= 1; depthOffset++) {
                        int originX = axis == 0 ? x - widthOffset : x + depthOffset;
                        int originZ = axis == 0 ? z + depthOffset : z - widthOffset;
                        if (!portalInteriorContainsPortal(originX, originY, originZ, axis)) {
                            continue;
                        }
                        if (!isCompleteParadisePortal(originX, originY, originZ, axis)) {
                            clearParadisePortalInterior(originX, originY, originZ, axis);
                        }
                    }
                }
            }
        }
    }

    private boolean isCompleteParadisePortal(int originX, int originY, int originZ, int axis) {
        if (!isValidParadisePortalFrame(originX, originY, originZ, axis, true)) {
            return false;
        }
        for (int width = 1; width <= 2; width++) {
            for (int height = 1; height <= 3; height++) {
                int cellX = axis == 0 ? originX + width : originX;
                int cellZ = axis == 0 ? originZ : originZ + width;
                if (getBlock(cellX, originY + height, cellZ) != GameConfig.PARADISE_PORTAL) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean portalInteriorContainsPortal(int originX, int originY, int originZ, int axis) {
        for (int width = 1; width <= 2; width++) {
            for (int height = 1; height <= 3; height++) {
                int cellX = axis == 0 ? originX + width : originX;
                int cellZ = axis == 0 ? originZ : originZ + width;
                if (getBlock(cellX, originY + height, cellZ) == GameConfig.PARADISE_PORTAL) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValidParadisePortalFrame(int originX, int originY, int originZ, int axis, boolean allowPortalInterior) {
        for (int width = 0; width < 4; width++) {
            int cellX = axis == 0 ? originX + width : originX;
            int cellZ = axis == 0 ? originZ : originZ + width;
            if (getBlock(cellX, originY, cellZ) != GameConfig.GLASS
                || getBlock(cellX, originY + 4, cellZ) != GameConfig.GLASS) {
                return false;
            }
        }
        for (int height = 1; height <= 3; height++) {
            int rightX = axis == 0 ? originX + 3 : originX;
            int rightZ = axis == 0 ? originZ : originZ + 3;
            if (getBlock(originX, originY + height, originZ) != GameConfig.GLASS
                || getBlock(rightX, originY + height, rightZ) != GameConfig.GLASS) {
                return false;
            }
        }
        for (int width = 1; width <= 2; width++) {
            for (int height = 1; height <= 3; height++) {
                int cellX = axis == 0 ? originX + width : originX;
                int cellZ = axis == 0 ? originZ : originZ + width;
                byte block = getBlock(cellX, originY + height, cellZ);
                if (block != GameConfig.AIR && (!allowPortalInterior || block != GameConfig.PARADISE_PORTAL)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void fillParadisePortalInterior(int originX, int originY, int originZ, int axis) {
        for (int width = 1; width <= 2; width++) {
            for (int height = 1; height <= 3; height++) {
                int cellX = axis == 0 ? originX + width : originX;
                int cellZ = axis == 0 ? originZ : originZ + width;
                setBlockState(cellX, originY + height, cellZ, GameConfig.PARADISE_PORTAL, -1);
                refreshDynamicCellsAround(cellX, originY + height, cellZ);
                refreshSurfaceHeight(cellX, cellZ);
            }
        }
    }

    private void clearParadisePortalInterior(int originX, int originY, int originZ, int axis) {
        for (int width = 1; width <= 2; width++) {
            for (int height = 1; height <= 3; height++) {
                int cellX = axis == 0 ? originX + width : originX;
                int cellZ = axis == 0 ? originZ : originZ + width;
                if (getBlock(cellX, originY + height, cellZ) == GameConfig.PARADISE_PORTAL) {
                    setBlockState(cellX, originY + height, cellZ, GameConfig.AIR, -1);
                    refreshDynamicCellsAround(cellX, originY + height, cellZ);
                    refreshSurfaceHeight(cellX, cellZ);
                }
            }
        }
    }

    void spawnDroppedItem(byte itemId, int count, double x, double y, double z) {
        spawnDroppedItem(itemId, count, 0, x, y, z);
    }

    void spawnDroppedItem(byte itemId, int count, int durabilityDamage, double x, double y, double z) {
        if (!InventoryItems.isCollectible(itemId) || count <= 0) {
            return;
        }
        if (droppedItems.size() >= 256) {
            return;
        }
        DroppedItem droppedItem = new DroppedItem(itemId, count, x, y, z);
        droppedItem.durabilityDamage = InventoryItems.isDurableItem(itemId)
            ? Math.max(0, Math.min(durabilityDamage, InventoryItems.maxDurability(itemId) - 1))
            : 0;
        droppedItem.velocityX = (worldRandom.nextDouble() - 0.5) * 1.2;
        droppedItem.velocityZ = (worldRandom.nextDouble() - 0.5) * 1.2;
        droppedItem.verticalVelocity = 1.8 + worldRandom.nextDouble() * 1.0;
        droppedItem.spinDegrees = worldRandom.nextDouble() * 360.0;
        droppedItem.physicsTickOffset = worldRandom.nextInt(8);
        droppedItems.add(droppedItem);
    }

    void spawnThrownItem(byte itemId, int count, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        spawnThrownItem(itemId, count, 0, x, y, z, velocityX, velocityY, velocityZ);
    }

    void spawnThrownItem(byte itemId, int count, int durabilityDamage, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        if (!InventoryItems.isCollectible(itemId) || count <= 0 || droppedItems.size() >= 256) {
            return;
        }
        DroppedItem droppedItem = new DroppedItem(itemId, count, x, y, z);
        droppedItem.durabilityDamage = InventoryItems.isDurableItem(itemId)
            ? Math.max(0, Math.min(durabilityDamage, InventoryItems.maxDurability(itemId) - 1))
            : 0;
        droppedItem.velocityX = velocityX;
        droppedItem.velocityZ = velocityZ;
        droppedItem.verticalVelocity = velocityY;
        droppedItem.spinDegrees = worldRandom.nextDouble() * 360.0;
        droppedItem.physicsTickOffset = worldRandom.nextInt(8);
        droppedItems.add(droppedItem);
    }

    void spawnChestLootAt(int blockX, int blockY, int blockZ) {
        ContainerInventory container = new ContainerInventory(27);
        fillGeneratedChestLoot(container, blockX, blockY, blockZ);
        for (ItemStack stack : container.slots) {
            if (!stack.isEmpty()) {
                spawnDroppedItem(stack.itemId, stack.count, blockX + 0.5, blockY + 0.8, blockZ + 0.5);
            }
        }
    }

    private void fillGeneratedChestLoot(ContainerInventory container, int blockX, int blockY, int blockZ) {
        long lootSeed = seed ^ mix64((((long) blockX) << 38) ^ (((long) blockZ) << 12) ^ blockY);
        Random lootRandom = new Random(lootSeed);
        byte[] commonLoot = {InventoryItems.POTATO, InventoryItems.CARROT, InventoryItems.WHEAT_SEEDS, InventoryItems.BREAD};
        byte[] mineLoot = {
            InventoryItems.COAL_ITEM, InventoryItems.COAL_ITEM, InventoryItems.COAL_ITEM,
            GameConfig.IRON_ORE, GameConfig.IRON_ORE, InventoryItems.IRON_INGOT,
            InventoryItems.BREAD, GameConfig.RAIL, GameConfig.RAIL,
            InventoryItems.DIAMOND_ITEM
        };
        byte[] loot = blockY < GameConfig.SEA_LEVEL - 12 ? mineLoot : commonLoot;
        int rolls = 2 + lootRandom.nextInt(3);
        for (int roll = 0; roll < rolls; roll++) {
            byte item = loot[lootRandom.nextInt(loot.length)];
            int count = item == InventoryItems.DIAMOND_ITEM ? 1 : 1 + lootRandom.nextInt(item == GameConfig.RAIL ? 10 : 4);
            int slot = lootRandom.nextInt(container.slots.length);
            ItemStack stack = container.slots[slot];
            if (stack.isEmpty()) {
                stack.set(item, count);
            } else if (stack.itemId == item) {
                stack.count = Math.min(InventoryItems.maxStackSize(item), stack.count + count);
            }
        }
    }

    private long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    void updateDroppedItems(PlayerState player, PlayerInventory inventory, double deltaTime) {
        setActiveDimensionFor(player);
        droppedItemUpdateCounter++;
        for (int i = droppedItems.size() - 1; i >= 0; i--) {
            DroppedItem droppedItem = droppedItems.get(i);
            droppedItem.ageSeconds += deltaTime;
            droppedItem.pickupDelaySeconds = Math.max(0.0, droppedItem.pickupDelaySeconds - deltaTime);
            droppedItem.spinDegrees = (droppedItem.spinDegrees + deltaTime * 180.0) % 360.0;
            if (droppedItem.ageSeconds >= GameConfig.DROPPED_ITEM_DESPAWN_SECONDS || droppedItem.count <= 0) {
                droppedItems.remove(i);
                continue;
            }

            double dx = player == null ? 0.0 : droppedItem.x - player.x;
            double dy = player == null ? 0.0 : (droppedItem.y + droppedItem.height() * 0.5) - (player.y + GameConfig.PLAYER_HEIGHT * 0.5);
            double dz = player == null ? 0.0 : droppedItem.z - player.z;
            double distanceSquared = player == null ? Double.POSITIVE_INFINITY : dx * dx + dy * dy + dz * dz;
            int physicsInterval = droppedItemPhysicsInterval(distanceSquared, droppedItem);
            if (physicsInterval <= 1 || Math.floorMod(droppedItemUpdateCounter + droppedItem.physicsTickOffset, physicsInterval) == 0) {
                updateDroppedItemPhysics(droppedItem, Math.min(deltaTime * physicsInterval, 0.12));
            }

            if (player == null || inventory == null || droppedItem.pickupDelaySeconds > 0.0) {
                continue;
            }

            double pickupDistance = GameConfig.DROPPED_ITEM_PICKUP_RADIUS;
            if (distanceSquared <= pickupDistance * pickupDistance
                && inventory.addItem(droppedItem.itemId, droppedItem.count, droppedItem.durabilityDamage)) {
                droppedItems.remove(i);
            }
        }
    }

    private int droppedItemPhysicsInterval(double distanceSquared, DroppedItem droppedItem) {
        if (distanceSquared <= 24.0 * 24.0) {
            return 1;
        }
        if (distanceSquared <= 64.0 * 64.0) {
            return 3;
        }
        return droppedItem.isGrounded ? 12 : 6;
    }

    boolean placeBlock(RayHit hit, byte block, PlayerState player) {
        if (hit == null || (player != null && player.spectatorMode)) {
            return false;
        }

        if (block == GameConfig.TORCH && activateParadisePortalAtFrame(hit.x, hit.y, hit.z)) {
            return true;
        }

        byte placedBlock = GameConfig.placedBlockForItem(block);
        int placeX = hit.previousX;
        int placeY = hit.previousY;
        int placeZ = hit.previousZ;
        if (GameConfig.isLiquidBlock(placedBlock)
            && GameConfig.fluidItemForBlock(getBlock(hit.x, hit.y, hit.z)) == GameConfig.fluidItemForBlock(placedBlock)) {
            placeX = hit.x;
            placeY = hit.y;
            placeZ = hit.z;
        }

        if (!isInside(placeX, placeY, placeZ)) {
            return false;
        }

        if (isCropBlock(placedBlock)) {
            return placeCrop(placeX, placeY, placeZ, placedBlock);
        }
        if (isAquaticPlantBlock(placedBlock)) {
            return placeAquaticPlant(placeX, placeY, placeZ, placedBlock);
        }
        if (placedBlock == GameConfig.OAK_DOOR) {
            return placeDoor(placeX, placeY, placeZ, player);
        }
        if (placedBlock == GameConfig.OAK_FENCE_GATE) {
            return placeFenceGate(placeX, placeY, placeZ, player);
        }
        if (placedBlock == GameConfig.RED_BED) {
            return placeBed(placeX, placeY, placeZ, player);
        }
        if (isStairBlock(placedBlock)) {
            return placeStair(placeX, placeY, placeZ, placedBlock, player);
        }

        byte targetBlock = getBlock(placeX, placeY, placeZ);
        boolean canReplaceTarget = Blocks.isReplaceable(targetBlock)
            || GameConfig.isLiquidBlock(targetBlock);
        if (!canReplaceTarget) {
            return false;
        }

        boolean solidPlacement = isSolidBlock(placedBlock);
        if (solidPlacement && blockIntersectsEntityHitboxes(placeX, placeY, placeZ, placedBlock, player)) {
            return false;
        }

        if (GameConfig.isLiquidBlock(placedBlock)) {
            setBlockState(placeX, placeY, placeZ, placedBlock, 0);
        } else {
            setBlockState(placeX, placeY, placeZ, placedBlock, -1);
        }
        if (placedBlock == GameConfig.CHEST) {
            chestContainers.put(packBlock(placeX, placeY, placeZ), new ContainerInventory(27));
        } else if (placedBlock == GameConfig.FURNACE) {
            furnaces.put(packBlock(placeX, placeY, placeZ), new FurnaceBlockEntity());
        }
        updatePlantSupportAt(placeX, placeY + 1, placeZ);
        updateDoorSupportAt(placeX, placeY + 1, placeZ);
        refreshDynamicCellsAround(placeX, placeY, placeZ);
        refreshParadisePortalsNear(placeX, placeY, placeZ);
        markDirtyBlock(placeX, placeY, placeZ);
        refreshSurfaceHeight(placeX, placeZ);
        return true;
    }

    private boolean placeCrop(int x, int y, int z, byte cropBlock) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y || getBlock(x, y - 1, z) != GameConfig.FARMLAND) {
            return false;
        }
        if (!isReplaceableForPlacement(getBlock(x, y, z))) {
            return false;
        }
        setBlockState(x, y, z, Blocks.withData(cropBlock, 0));
        updatePlantSupportAt(x, y + 1, z);
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
        return true;
    }

    private boolean placeAquaticPlant(int x, int y, int z, byte plantBlock) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y || y + 1 > GameConfig.WORLD_MAX_Y) {
            return false;
        }
        byte current = getBlock(x, y, z);
        byte support = getBlock(x, y - 1, z);
        if (!GameConfig.isWaterBlock(current) || !GameConfig.isWaterBlock(getBlock(x, y + 1, z))) {
            return false;
        }
        if (support != GameConfig.SAND
            && support != GameConfig.GRAVEL
            && support != GameConfig.CLAY
            && support != GameConfig.DIRT
            && support != GameConfig.KELP) {
            return false;
        }
        if (plantBlock == GameConfig.KELP) {
            int segment = support == GameConfig.KELP ? 2 : 0;
            setBlockState(x, y, z, Blocks.withData(plantBlock, segment));
            if (support == GameConfig.KELP) {
                setBlockState(x, y - 1, z, Blocks.withData(GameConfig.KELP, 1));
            }
        } else {
            setBlockState(x, y, z, plantBlock, 0);
        }
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        return true;
    }

    byte scoopLiquid(RayHit hit) {
        if (hit == null || !isInside(hit.x, hit.y, hit.z)) {
            return GameConfig.AIR;
        }
        byte block = getBlock(hit.x, hit.y, hit.z);
        byte fluid = GameConfig.fluidItemForBlock(block);
        if (fluid == GameConfig.WATER) {
            removeLiquidAt(hit.x, hit.y, hit.z);
            return InventoryItems.ITEM_WATER_BUCKET;
        }
        if (fluid == GameConfig.LAVA) {
            removeLiquidAt(hit.x, hit.y, hit.z);
            return InventoryItems.ITEM_LAVA_BUCKET;
        }
        return GameConfig.AIR;
    }

    private void removeLiquidAt(int x, int y, int z) {
        setBlockState(x, y, z, GameConfig.AIR, -1);
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
    }

    private boolean placeDoor(int x, int y, int z, PlayerState player) {
        if (!isInside(x, y, z) || !isInside(x, y + 1, z) || y <= GameConfig.WORLD_MIN_Y) {
            return false;
        }
        if (!isReplaceableForPlacement(getBlock(x, y, z)) || !isReplaceableForPlacement(getBlock(x, y + 1, z))) {
            return false;
        }
        if (!isSolidBlock(getBlock(x, y - 1, z))) {
            return false;
        }
        if (blockIntersectsEntityHitboxes(x, y, z, GameConfig.OAK_DOOR, player)
            || blockIntersectsEntityHitboxes(x, y + 1, z, GameConfig.OAK_DOOR, player)) {
            return false;
        }
        int facing = player == null ? 2 : facingFromYaw(player.yaw);
        setBlockState(x, y, z, Blocks.doorState(false, false, facing));
        setBlockState(x, y + 1, z, Blocks.doorState(false, true, facing));
        refreshDynamicCellsAround(x, y, z);
        refreshDynamicCellsAround(x, y + 1, z);
        markDirtyBlock(x, y, z);
        markDirtyBlock(x, y + 1, z);
        refreshSurfaceHeight(x, z);
        return true;
    }

    private boolean placeStair(int x, int y, int z, byte block, PlayerState player) {
        byte targetBlock = getBlock(x, y, z);
        if (!Blocks.isReplaceable(targetBlock) && !GameConfig.isLiquidBlock(targetBlock)) {
            return false;
        }
        if (blockIntersectsEntityHitboxes(x, y, z, block, player)) {
            return false;
        }
        int facing = player == null ? 0 : facingFromYaw(player.yaw);
        setBlockState(x, y, z, Blocks.stairState(block, facing));
        updatePlantSupportAt(x, y + 1, z);
        updateDoorSupportAt(x, y + 1, z);
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
        return true;
    }

    private boolean placeFenceGate(int x, int y, int z, PlayerState player) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y || !isReplaceableForPlacement(getBlock(x, y, z))) {
            return false;
        }
        if (!isSolidBlock(getBlock(x, y - 1, z)) || blockIntersectsEntityHitboxes(x, y, z, GameConfig.OAK_FENCE_GATE, player)) {
            return false;
        }
        int facing = player == null ? 2 : facingFromYaw(player.yaw);
        setBlockState(x, y, z, Blocks.gateState(false, facing));
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
        return true;
    }

    private boolean placeBed(int x, int y, int z, PlayerState player) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y) {
            return false;
        }
        int facing = player == null ? 1 : facingFromYaw(player.yaw);
        int headX = x + facingDx(facing);
        int headZ = z + facingDz(facing);
        if (!isInside(headX, y, headZ)) {
            return false;
        }
        if (!isReplaceableForPlacement(getBlock(x, y, z)) || !isReplaceableForPlacement(getBlock(headX, y, headZ))) {
            return false;
        }
        if (!isSolidBlock(getBlock(x, y - 1, z)) || !isSolidBlock(getBlock(headX, y - 1, headZ))) {
            return false;
        }
        if (blockIntersectsEntityHitboxes(x, y, z, GameConfig.RED_BED, player)
            || blockIntersectsEntityHitboxes(headX, y, headZ, GameConfig.RED_BED, player)) {
            return false;
        }
        setBlockState(x, y, z, Blocks.bedState(false, facing));
        setBlockState(headX, y, headZ, Blocks.bedState(true, facing));
        refreshDynamicCellsAround(x, y, z);
        refreshDynamicCellsAround(headX, y, headZ);
        markDirtyBlock(x, y, z);
        markDirtyBlock(headX, y, headZ);
        refreshSurfaceHeight(x, z);
        refreshSurfaceHeight(headX, headZ);
        return true;
    }

    private void removeBedAt(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        if (state.type.numericId != (GameConfig.RED_BED & 0xFF)) {
            return;
        }
        int facing = Blocks.bedFacing(state);
        int footX = Blocks.isBedHead(state) ? x - facingDx(facing) : x;
        int footZ = Blocks.isBedHead(state) ? z - facingDz(facing) : z;
        int headX = footX + facingDx(facing);
        int headZ = footZ + facingDz(facing);
        if (getBlock(footX, y, footZ) == GameConfig.RED_BED) {
            setBlockState(footX, y, footZ, GameConfig.AIR, -1);
            refreshDynamicCellsAround(footX, y, footZ);
            markDirtyBlock(footX, y, footZ);
            refreshSurfaceHeight(footX, footZ);
        }
        if (getBlock(headX, y, headZ) == GameConfig.RED_BED) {
            setBlockState(headX, y, headZ, GameConfig.AIR, -1);
            refreshDynamicCellsAround(headX, y, headZ);
            markDirtyBlock(headX, y, headZ);
            refreshSurfaceHeight(headX, headZ);
        }
    }

    private int facingDx(int facing) {
        return facing == 1 ? 1 : (facing == 3 ? -1 : 0);
    }

    private int facingDz(int facing) {
        return facing == 2 ? 1 : (facing == 0 ? -1 : 0);
    }

    private boolean isReplaceableForPlacement(byte block) {
        return Blocks.isReplaceable(block) || GameConfig.isLiquidBlock(block);
    }

    private boolean isCropBlock(byte block) {
        return block == GameConfig.WHEAT_CROP
            || block == GameConfig.CARROT_CROP
            || block == GameConfig.POTATO_CROP;
    }

    private boolean isAquaticPlantBlock(byte block) {
        return block == GameConfig.SEAGRASS || block == GameConfig.KELP;
    }

    private int facingFromYaw(double yaw) {
        double normalized = yaw % (Math.PI * 2.0);
        if (normalized < 0.0) {
            normalized += Math.PI * 2.0;
        }
        int quadrant = (int) Math.floor((normalized + Math.PI * 0.25) / (Math.PI * 0.5)) & 3;
        switch (quadrant) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    private void toggleDoorAt(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        if (state.type.numericId != (GameConfig.OAK_DOOR & 0xFF)) {
            return;
        }
        int lowerY = doorLowerY(x, y, z, state);
        BlockState lower = getBlockState(x, lowerY, z);
        if (lower.type.numericId != (GameConfig.OAK_DOOR & 0xFF)) {
            return;
        }
        int facing = Blocks.doorFacing(lower);
        boolean open = !Blocks.isDoorOpen(lower);
        setBlockState(x, lowerY, z, Blocks.doorState(open, false, facing));
        if (getBlock(x, lowerY + 1, z) == GameConfig.OAK_DOOR) {
            setBlockState(x, lowerY + 1, z, Blocks.doorState(open, true, facing));
        }
        refreshDynamicCellsAround(x, lowerY, z);
        refreshDynamicCellsAround(x, lowerY + 1, z);
        markDirtyBlock(x, lowerY, z);
        markDirtyBlock(x, lowerY + 1, z);
    }

    private void toggleFenceGateAt(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        if (state.type.numericId != (GameConfig.OAK_FENCE_GATE & 0xFF)) {
            return;
        }
        setBlockState(x, y, z, Blocks.gateState(!Blocks.isGateOpen(state), Blocks.gateFacing(state)));
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
    }

    private void removeDoorAt(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        int lowerY = doorLowerY(x, y, z, state);
        if (getBlock(x, lowerY, z) == GameConfig.OAK_DOOR) {
            setBlockState(x, lowerY, z, GameConfig.AIR, -1);
        }
        if (getBlock(x, lowerY + 1, z) == GameConfig.OAK_DOOR) {
            setBlockState(x, lowerY + 1, z, GameConfig.AIR, -1);
        }
    }

    private void updateDoorSupportAt(int x, int y, int z) {
        if (!isInside(x, y, z) || getBlock(x, y, z) != GameConfig.OAK_DOOR) {
            return;
        }
        BlockState state = getBlockState(x, y, z);
        int lowerY = doorLowerY(x, y, z, state);
        if (getBlock(x, lowerY, z) != GameConfig.OAK_DOOR
            || getBlock(x, lowerY + 1, z) != GameConfig.OAK_DOOR
            || !isSolidBlock(getBlock(x, lowerY - 1, z))) {
            removeDoorAt(x, y, z);
            markDirtyBlock(x, y, z);
            refreshSurfaceHeight(x, z);
        }
    }

    private int doorLowerY(int x, int y, int z, BlockState state) {
        if (Blocks.isDoorUpper(state)) {
            return y - 1;
        }
        if (getBlock(x, y - 1, z) == GameConfig.OAK_DOOR && getBlock(x, y + 1, z) != GameConfig.OAK_DOOR) {
            return y - 1;
        }
        return y;
    }

    RayHit raycastBlock(PlayerState player) {
        double originX = player.x;
        double originY = player.y + player.eyeHeight();
        double originZ = player.z;

        double horizontalLength = Math.cos(player.pitch);
        double dirX = Math.cos(player.yaw) * horizontalLength;
        double dirY = Math.sin(player.pitch);
        double dirZ = Math.sin(player.yaw) * horizontalLength;

        int blockX = (int) Math.floor(originX);
        int blockY = (int) Math.floor(originY);
        int blockZ = (int) Math.floor(originZ);
        int previousX = blockX;
        int previousY = blockY;
        int previousZ = blockZ;

        int stepX = dirX > 0.0 ? 1 : (dirX < 0.0 ? -1 : 0);
        int stepY = dirY > 0.0 ? 1 : (dirY < 0.0 ? -1 : 0);
        int stepZ = dirZ > 0.0 ? 1 : (dirZ < 0.0 ? -1 : 0);

        double nextBoundaryX = stepX > 0 ? blockX + 1.0 : blockX;
        double nextBoundaryY = stepY > 0 ? blockY + 1.0 : blockY;
        double nextBoundaryZ = stepZ > 0 ? blockZ + 1.0 : blockZ;

        double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY : (nextBoundaryX - originX) / dirX;
        double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY : (nextBoundaryY - originY) / dirY;
        double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : (nextBoundaryZ - originZ) / dirZ;

        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dirX);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dirY);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dirZ);

        double maxDistance = GameConfig.REACH_DISTANCE;
        double traveled = 0.0;

        while (traveled <= maxDistance) {
            if (isInside(blockX, blockY, blockZ)) {
                byte block = getBlock(blockX, blockY, blockZ);
                if (block != GameConfig.AIR && (!GameConfig.isLiquidBlock(block) || isAquaticPlantBlock(block))) {
                    double[] bounds = selectionBounds(block, getBlockState(blockX, blockY, blockZ));
                    if (rayIntersectsAabb(originX, originY, originZ, dirX, dirY, dirZ,
                        blockX + bounds[0], blockY + bounds[1], blockZ + bounds[2],
                        blockX + bounds[3], blockY + bounds[4], blockZ + bounds[5],
                        maxDistance)) {
                        return new RayHit(blockX, blockY, blockZ, previousX, previousY, previousZ);
                    }
                }
            }

            previousX = blockX;
            previousY = blockY;
            previousZ = blockZ;

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                blockX += stepX;
                traveled = tMaxX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                blockY += stepY;
                traveled = tMaxY;
                tMaxY += tDeltaY;
            } else {
                blockZ += stepZ;
                traveled = tMaxZ;
                tMaxZ += tDeltaZ;
            }
        }

        return null;
    }

    private double[] selectionBounds(byte block, BlockState state) {
        switch (block) {
            case GameConfig.SNOW_LAYER:
                return new double[]{0.0, 0.0, 0.0, 1.0, 0.125, 1.0};
            case GameConfig.FARMLAND:
                return new double[]{0.0, 0.0, 0.0, 1.0, 0.9375, 1.0};
            case GameConfig.WHEAT_CROP:
            case GameConfig.CARROT_CROP:
            case GameConfig.POTATO_CROP:
            case GameConfig.TALL_GRASS:
            case GameConfig.RED_FLOWER:
            case GameConfig.YELLOW_FLOWER:
            case GameConfig.SEAGRASS:
            case GameConfig.KELP:
            case GameConfig.DEAD_BUSH:
                return new double[]{0.18, 0.0, 0.18, 0.82, 0.86, 0.82};
            case GameConfig.RAIL:
                return new double[]{0.0, 0.0, 0.0, 1.0, 0.075, 1.0};
            case GameConfig.TORCH:
                return new double[]{0.40, 0.0, 0.40, 0.60, 0.82, 0.60};
            case GameConfig.PARADISE_PORTAL:
                double inset = (1.0 - GameConfig.PARADISE_PORTAL_RENDER_THICKNESS) * 0.5;
                return new double[]{inset, 0.0, inset, 1.0 - inset, 1.0, 1.0 - inset};
            case GameConfig.OAK_FENCE:
                return new double[]{0.375, 0.0, 0.375, 0.625, 1.5, 0.625};
            case GameConfig.OAK_FENCE_GATE:
                return fenceGateSelectionBounds(state);
            case GameConfig.CHEST:
                return new double[]{0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375};
            case GameConfig.RED_BED:
                return new double[]{0.0, 0.0, 0.0, 1.0, 0.56, 1.0};
            case GameConfig.OAK_DOOR:
                return doorSelectionBounds(state);
            default:
                return new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
        }
    }

    private double[] doorSelectionBounds(BlockState state) {
        int facing = Blocks.doorFacing(state);
        boolean open = Blocks.isDoorOpen(state);
        double t = 0.16;
        int visualFacing = open ? ((facing + 1) & 3) : facing;
        switch (visualFacing) {
            case 0:
                return new double[]{0.0, 0.0, 0.0, 1.0, 1.0, t};
            case 1:
                return new double[]{1.0 - t, 0.0, 0.0, 1.0, 1.0, 1.0};
            case 2:
                return new double[]{0.0, 0.0, 1.0 - t, 1.0, 1.0, 1.0};
            default:
                return new double[]{0.0, 0.0, 0.0, t, 1.0, 1.0};
        }
    }

    private boolean rayIntersectsAabb(double ox, double oy, double oz, double dx, double dy, double dz,
                                      double minX, double minY, double minZ,
                                      double maxX, double maxY, double maxZ,
                                      double maxDistance) {
        double tMin = 0.0;
        double tMax = maxDistance;
        if (Math.abs(dx) < 1.0e-9) {
            if (ox < minX || ox > maxX) return false;
        } else {
            double inv = 1.0 / dx;
            double t1 = (minX - ox) * inv;
            double t2 = (maxX - ox) * inv;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }
        if (Math.abs(dy) < 1.0e-9) {
            if (oy < minY || oy > maxY) return false;
        } else {
            double inv = 1.0 / dy;
            double t1 = (minY - oy) * inv;
            double t2 = (maxY - oy) * inv;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }
        if (Math.abs(dz) < 1.0e-9) {
            if (oz < minZ || oz > maxZ) return false;
        } else {
            double inv = 1.0 / dz;
            double t1 = (minZ - oz) * inv;
            double t2 = (maxZ - oz) * inv;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }
        return tMax >= tMin && tMax >= 0.0;
    }

    boolean attackMobInReach(PlayerState player, int damage, double knockback) {
        if (player == null || damage <= 0) {
            return false;
        }
        double originX = player.x;
        double originY = player.y + player.eyeHeight();
        double originZ = player.z;
        double horizontalLength = Math.cos(player.pitch);
        double dirX = Math.cos(player.yaw) * horizontalLength;
        double dirY = Math.sin(player.pitch);
        double dirZ = Math.sin(player.yaw) * horizontalLength;
        double reach = 3.4;
        MobEntity best = null;
        double bestProjection = Double.POSITIVE_INFINITY;
        for (MobEntity mob : mobs) {
            if (mob.health <= 0) {
                continue;
            }
            double centerX = mob.x;
            double centerY = mob.y + GameConfig.ZOMBIE_HEIGHT * 0.52;
            double centerZ = mob.z;
            double toX = centerX - originX;
            double toY = centerY - originY;
            double toZ = centerZ - originZ;
            double projection = toX * dirX + toY * dirY + toZ * dirZ;
            if (projection < 0.0 || projection > reach || projection >= bestProjection) {
                continue;
            }
            double closestX = originX + dirX * projection;
            double closestY = originY + dirY * projection;
            double closestZ = originZ + dirZ * projection;
            double dx = centerX - closestX;
            double dy = centerY - closestY;
            double dz = centerZ - closestZ;
            double hitRadius = mob.kind == MobKind.VILLAGER ? 0.72 : 0.62;
            if (dx * dx + dy * dy + dz * dz <= hitRadius * hitRadius) {
                best = mob;
                bestProjection = projection;
            }
        }
        if (best == null) {
            return false;
        }
        if (best.hurtCooldown > 0.0) {
            return true;
        }
        best.health -= damage;
        best.fireTimer = 0.0;
        best.growlQueued = true;
        best.hurtCooldown = 0.28;
        if (isPassiveMob(best)) {
            best.fleeTimer = 4.0 + best.random.nextDouble() * 2.0;
        }
        double appliedKnockback = knockback * (isPassiveMob(best) ? 1.35 : 1.12);
        best.velocityX += dirX * appliedKnockback;
        best.velocityZ += dirZ * appliedKnockback;
        best.verticalVelocity = Math.max(best.verticalVelocity, 0.20 + appliedKnockback * 0.18);
        return true;
    }

    boolean feedPassiveMobInReach(PlayerState player, byte itemId) {
        if (player == null || !isBreedingFood(itemId)) {
            return false;
        }
        MobEntity fed = findFeedTarget(player, itemId);
        if (fed == null) {
            return false;
        }
        if (fed.isBaby()) {
            fed.babyAge = Math.max(0.0, fed.babyAge - 60.0);
            fed.growlQueued = true;
            return true;
        }
        fed.loveTimer = 8.0;
        fed.breedCooldown = Math.max(fed.breedCooldown, 1.0);
        fed.growlQueued = true;
        MobEntity mate = findMateFor(fed);
        if (mate != null) {
            spawnBabyMobAt(fed.kind, (fed.x + mate.x) * 0.5, Math.max(fed.y, mate.y), (fed.z + mate.z) * 0.5);
            fed.loveTimer = 0.0;
            mate.loveTimer = 0.0;
            fed.breedCooldown = 35.0;
            mate.breedCooldown = 35.0;
        }
        return true;
    }

    private MobEntity findFeedTarget(PlayerState player, byte itemId) {
        MobEntity best = null;
        double bestDistance = 3.2 * 3.2;
        for (MobEntity mob : mobs) {
            if (!canFeedMob(mob, itemId)) {
                continue;
            }
            double dx = mob.x - player.x;
            double dy = (mob.y + 0.7) - (player.y + player.eyeHeight());
            double dz = mob.z - player.z;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDistance) {
                bestDistance = dist;
                best = mob;
            }
        }
        return best;
    }

    private MobEntity findMateFor(MobEntity fed) {
        for (MobEntity other : mobs) {
            if (other == fed || other.kind != fed.kind || other.health <= 0 || other.isBaby()
                || other.breedCooldown > 0.0 || other.loveTimer <= 0.0) {
                continue;
            }
            if (distanceSquared(fed.x, fed.z, other.x, other.z) <= 6.0 * 6.0 && Math.abs(fed.y - other.y) <= 2.0) {
                return other;
            }
        }
        return null;
    }

    private boolean canFeedMob(MobEntity mob, byte itemId) {
        if (mob == null || mob.health <= 0) {
            return false;
        }
        boolean correctFood = (itemId == GameConfig.WHEAT_CROP && (mob.kind == MobKind.COW || mob.kind == MobKind.SHEEP))
            || (itemId == InventoryItems.CARROT && mob.kind == MobKind.PIG);
        return correctFood
            && (mob.isBaby() || mob.breedCooldown <= 0.0);
    }

    private boolean isBreedingFood(byte itemId) {
        return itemId == GameConfig.WHEAT_CROP || itemId == InventoryItems.CARROT;
    }

    boolean collides(double x, double y, double z, double radius, double height) {
        int minX = (int) Math.floor(x - radius + COLLISION_EPSILON);
        int maxX = (int) Math.floor(x + radius - COLLISION_EPSILON);
        int minY = (int) Math.floor(y + COLLISION_EPSILON);
        int maxY = (int) Math.floor(y + height - COLLISION_EPSILON);
        int minZ = (int) Math.floor(z - radius + COLLISION_EPSILON);
        int maxZ = (int) Math.floor(z + radius - COLLISION_EPSILON);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockY = minY; blockY <= maxY; blockY++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    BlockState state = getBlockState(blockX, blockY, blockZ);
                    if (!isCollisionSolid(state)) {
                        continue;
                    }
                    if (state.type.numericId == (GameConfig.OAK_FENCE & 0xFF)) {
                        double[] fenceBounds = fenceCollisionBounds(blockX, blockY, blockZ);
                        if (intersectsAabb(
                            x - radius, y, z - radius,
                            x + radius, y + height, z + radius,
                            blockX + fenceBounds[0], blockY + fenceBounds[1], blockZ + fenceBounds[2],
                            blockX + fenceBounds[3], blockY + fenceBounds[4], blockZ + fenceBounds[5])) {
                            return true;
                        }
                        continue;
                    }
                    double[] bounds = selectionBounds((byte) state.type.numericId, state);
                    if (intersectsAabb(
                        x - radius, y, z - radius,
                        x + radius, y + height, z + radius,
                        blockX + bounds[0], blockY + bounds[1], blockZ + bounds[2],
                        blockX + bounds[3], blockY + bounds[4], blockZ + bounds[5])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double[] fenceCollisionBounds(int x, int y, int z) {
        double minX = 0.375;
        double maxX = 0.625;
        double minZ = 0.375;
        double maxZ = 0.625;
        if (fenceConnectsTo(x - 1, y, z)) {
            minX = 0.0;
        }
        if (fenceConnectsTo(x + 1, y, z)) {
            maxX = 1.0;
        }
        if (fenceConnectsTo(x, y, z - 1)) {
            minZ = 0.0;
        }
        if (fenceConnectsTo(x, y, z + 1)) {
            maxZ = 1.0;
        }
        return new double[]{minX, 0.0, minZ, maxX, 1.5, maxZ};
    }

    private boolean fenceConnectsTo(int x, int y, int z) {
        byte block = getBlock(x, y, z);
        return block == GameConfig.OAK_FENCE
            || block == GameConfig.OAK_FENCE_GATE
            || (isSolidBlock(block) && isCubeBlock(block) && !isTransparentBlock(block));
    }

    boolean touchesBlock(double x, double y, double z, double radius, double height, byte targetBlock) {
        int minX = (int) Math.floor(x - radius + COLLISION_EPSILON);
        int maxX = (int) Math.floor(x + radius - COLLISION_EPSILON);
        int minY = (int) Math.floor(y + COLLISION_EPSILON);
        int maxY = (int) Math.floor(y + height - COLLISION_EPSILON);
        int minZ = (int) Math.floor(z - radius + COLLISION_EPSILON);
        int maxZ = (int) Math.floor(z + radius - COLLISION_EPSILON);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockY = minY; blockY <= maxY; blockY++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    if (getBlock(blockX, blockY, blockZ) == targetBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    boolean intersectsFluid(double x, double y, double z, double radius, double height, byte fluidItem) {
        int minX = (int) Math.floor(x - radius + COLLISION_EPSILON);
        int maxX = (int) Math.floor(x + radius - COLLISION_EPSILON);
        int minY = (int) Math.floor(y + COLLISION_EPSILON);
        int maxY = (int) Math.floor(y + height - COLLISION_EPSILON);
        int minZ = (int) Math.floor(z - radius + COLLISION_EPSILON);
        int maxZ = (int) Math.floor(z + radius - COLLISION_EPSILON);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockY = minY; blockY <= maxY; blockY++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    byte block = getBlock(blockX, blockY, blockZ);
                    if (GameConfig.fluidItemForBlock(block) != fluidItem) {
                        continue;
                    }
                    double fluidTop = blockY + getFluidSurfaceHeight(blockX, blockY, blockZ);
                    if (fluidTop > y + COLLISION_EPSILON) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    boolean isPointInsideFluid(double x, double y, double z, byte fluidItem) {
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        byte block = getBlock(blockX, blockY, blockZ);
        if (GameConfig.fluidItemForBlock(block) != fluidItem) {
            return false;
        }
        return y < blockY + getFluidSurfaceHeight(blockX, blockY, blockZ) - 1.0e-4;
    }

    boolean canStandOnFluid(double x, double y, double z, double radius, byte fluidBlock) {
        int minX = (int) Math.floor(x - radius + COLLISION_EPSILON);
        int maxX = (int) Math.floor(x + radius - COLLISION_EPSILON);
        int minZ = (int) Math.floor(z - radius + COLLISION_EPSILON);
        int maxZ = (int) Math.floor(z + radius - COLLISION_EPSILON);
        int blockY = (int) Math.floor(y - 0.08);

        boolean foundFlowingFluid = false;
        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                byte block = getBlock(blockX, blockY, blockZ);
                if (block == fluidBlock) {
                    foundFlowingFluid = true;
                } else if (isSolidBlock(block)) {
                    return false;
                }
            }
        }
        return foundFlowingFluid;
    }

    boolean hasNearbyWater(double x, double y, double z, int radius) {
        int centerX = (int) Math.floor(x);
        int centerY = (int) Math.floor(y);
        int centerZ = (int) Math.floor(z);
        for (int blockX = centerX - radius; blockX <= centerX + radius; blockX++) {
            for (int blockY = Math.max(GameConfig.WORLD_MIN_Y, centerY - 1); blockY <= Math.min(GameConfig.WORLD_MAX_Y, centerY + 1); blockY++) {
                for (int blockZ = centerZ - radius; blockZ <= centerZ + radius; blockZ++) {
                    if (GameConfig.isWaterBlock(getBlock(blockX, blockY, blockZ))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    double getFluidSurfaceHeight(int x, int y, int z) {
        byte block = getBlock(x, y, z);
        if (!GameConfig.isLiquidBlock(block)) {
            return 0.0;
        }
        if (GameConfig.isFluidSourceBlock(block)) {
            return 1.0;
        }
        byte above = getBlock(x, y + 1, z);
        if (GameConfig.fluidItemForBlock(above) == GameConfig.fluidItemForBlock(block)) {
            return 1.0;
        }
        int distance = Math.max(0, getFluidDistance(x, y, z));
        int maxDistance = GameConfig.fluidSpreadDistance(block);
        double step = GameConfig.isWaterBlock(block) ? 0.11 : 0.15;
        double minimum = GameConfig.isWaterBlock(block) ? 0.16 : 0.24;
        double maximum = GameConfig.isWaterBlock(block) ? 0.98 : 0.96;
        return Math.max(minimum, Math.min(maximum, maximum - Math.min(distance, maxDistance) * step));
    }

    void sampleFluidFlow(double x, double y, double z, double radius, double height, byte fluidItem, MutableVec3 output) {
        double minX = x - radius;
        double maxX = x + radius;
        double minY = y;
        double maxY = y + height;
        double minZ = z - radius;
        double maxZ = z + radius;

        int startX = (int) Math.floor(minX + COLLISION_EPSILON);
        int endX = (int) Math.floor(maxX - COLLISION_EPSILON);
        int startY = (int) Math.floor(minY + COLLISION_EPSILON);
        int endY = (int) Math.floor(maxY - COLLISION_EPSILON);
        int startZ = (int) Math.floor(minZ + COLLISION_EPSILON);
        int endZ = (int) Math.floor(maxZ - COLLISION_EPSILON);

        double flowX = 0.0;
        double flowY = 0.0;
        double flowZ = 0.0;
        int samples = 0;

        for (int blockX = startX; blockX <= endX; blockX++) {
            for (int blockY = startY; blockY <= endY; blockY++) {
                for (int blockZ = startZ; blockZ <= endZ; blockZ++) {
                    byte block = getBlock(blockX, blockY, blockZ);
                    if (GameConfig.fluidItemForBlock(block) != fluidItem) {
                        continue;
                    }
                    double fluidTop = blockY + getFluidSurfaceHeight(blockX, blockY, blockZ);
                    if (fluidTop <= minY + COLLISION_EPSILON) {
                        continue;
                    }

                    getFluidFlowVector(blockX, blockY, blockZ, blockFluidFlow);
                    flowX += blockFluidFlow.x;
                    flowY += blockFluidFlow.y;
                    flowZ += blockFluidFlow.z;
                    samples++;
                }
            }
        }

        if (samples == 0) {
            output.set(0.0, 0.0, 0.0);
            return;
        }
        output.set(flowX / samples, flowY / samples, flowZ / samples);
    }

    double getStandingY(double x, double z, double fallbackY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int topSolid = getSurfaceHeight(blockX, blockZ);
        int standY = topSolid + 1;
        if (standY >= GameConfig.WORLD_MAX_Y) {
            return fallbackY;
        }
        if (isSolidBlock(getBlock(blockX, standY, blockZ)) || isSolidBlock(getBlock(blockX, standY + 1, blockZ))) {
            return fallbackY;
        }
        return standY + 0.01;
    }

    boolean isInside(int x, int y, int z) {
        return GameConfig.isWorldCoordinateInside(x, y, z);
    }

    byte getBlock(int x, int y, int z) {
        Chunk chunk = getChunkForBlock(x, y, z);
        if (chunk == null) {
            return GameConfig.AIR;
        }
        return chunk.getBlockLocal(localBlockCoordinate(x), GameConfig.localYForWorldY(y), localBlockCoordinate(z));
    }

    BlockState getBlockState(int x, int y, int z) {
        Chunk chunk = getChunkForBlock(x, y, z);
        if (chunk == null) {
            return Blocks.stateFromLegacyId(GameConfig.AIR);
        }
        return chunk.getBlockStateLocal(localBlockCoordinate(x), GameConfig.localYForWorldY(y), localBlockCoordinate(z));
    }

    void applyNetworkBlockState(int x, int y, int z, BlockState state, int fluidDistance) {
        if (!isInside(x, y, z)) {
            return;
        }
        ensureColumnLoadedSync(worldToChunk(x), worldToChunk(z));
        Chunk chunk = getChunkForBlock(x, y, z);
        if (chunk == null) {
            return;
        }
        int localX = localBlockCoordinate(x);
        int localY = GameConfig.localYForWorldY(y);
        int localZ = localBlockCoordinate(z);
        chunk.setSerializedCellState((localY * Chunk.SIZE + localZ) * Chunk.SIZE + localX, state, fluidDistance);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
        refreshDynamicCellsAround(x, y, z);
    }

    void writeNetworkColumn(int chunkX, int chunkZ, DataOutputStream output) throws IOException {
        ensureColumnLoadedSync(chunkX, chunkZ);
        ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
        output.writeInt(chunkX);
        output.writeInt(chunkZ);
        output.writeInt(activeDimensionId);
        if (column == null) {
            output.writeBoolean(false);
            return;
        }
        output.writeBoolean(true);
        output.writeInt(column.status.ordinal());
        for (int i = 0; i < column.surfaceHeights.length; i++) {
            output.writeShort(column.surfaceHeights[i]);
        }
        output.writeInt(GameConfig.SECTION_COUNT);
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            ChunkSectionSnapshot snapshot = column.sections[chunkY].snapshot();
            ByteArrayOutputStream sectionBytes = new ByteArrayOutputStream();
            DataOutputStream sectionOutput = new DataOutputStream(sectionBytes);
            int runs = 0;
            int index = 0;
            while (index < Chunk.VOLUME) {
                int localX = index & (Chunk.SIZE - 1);
                int localY = index / (Chunk.SIZE * Chunk.SIZE);
                int localZ = (index / Chunk.SIZE) & (Chunk.SIZE - 1);
                BlockState state = snapshot.getBlockStateLocal(localX, localY, localZ);
                String id = Blocks.serializedId(state);
                int distance = snapshot.getFluidDistanceLocal(localX, localY, localZ);
                int count = 1;
                while (index + count < Chunk.VOLUME) {
                    int nextIndex = index + count;
                    int nextLocalX = nextIndex & (Chunk.SIZE - 1);
                    int nextLocalY = nextIndex / (Chunk.SIZE * Chunk.SIZE);
                    int nextLocalZ = (nextIndex / Chunk.SIZE) & (Chunk.SIZE - 1);
                    if (distance != snapshot.getFluidDistanceLocal(nextLocalX, nextLocalY, nextLocalZ)
                        || !id.equals(Blocks.serializedId(snapshot.getBlockStateLocal(nextLocalX, nextLocalY, nextLocalZ)))) {
                        break;
                    }
                    count++;
                }
                sectionOutput.writeInt(count);
                sectionOutput.writeUTF(id);
                sectionOutput.writeByte(distance);
                runs++;
                index += count;
            }
            sectionOutput.flush();
            output.writeInt(chunkY);
            output.writeInt(runs);
            output.writeInt(sectionBytes.size());
            output.write(sectionBytes.toByteArray());
        }
    }

    long readNetworkColumn(DataInputStream input) throws IOException {
        int chunkX = input.readInt();
        int chunkZ = input.readInt();
        int dimensionId = input.readInt();
        setActiveDimension(dimensionId);
        boolean present = input.readBoolean();
        if (!present) {
            return columnKey(chunkX, chunkZ);
        }
        ChunkColumn column = new ChunkColumn(chunkX, chunkZ);
        column.status = ChunkGenerationStatus.fromOrdinal(input.readInt());
        for (int i = 0; i < column.surfaceHeights.length; i++) {
            column.surfaceHeights[i] = input.readShort();
        }
        int sectionCount = input.readInt();
        if (sectionCount != GameConfig.SECTION_COUNT) {
            throw new IOException("invalid network chunk section count");
        }
        boolean[] seenSections = new boolean[GameConfig.SECTION_COUNT];
        for (int section = 0; section < sectionCount; section++) {
            int chunkY = input.readInt();
            int runs = input.readInt();
            int sectionBytes = input.readInt();
            if (chunkY < 0 || chunkY >= GameConfig.SECTION_COUNT || seenSections[chunkY]
                || runs <= 0 || sectionBytes < 0 || sectionBytes > MultiplayerProtocol.MAX_PACKET_BYTES) {
                throw new IOException("invalid network chunk section");
            }
            seenSections[chunkY] = true;
            byte[] payload = new byte[sectionBytes];
            input.readFully(payload);
            DataInputStream sectionInput = new DataInputStream(new ByteArrayInputStream(payload));
            Chunk chunk = column.sections[chunkY];
            int index = 0;
            for (int run = 0; run < runs; run++) {
                int count = sectionInput.readInt();
                if (count <= 0 || index + count > Chunk.VOLUME) {
                    throw new IOException("invalid network chunk run");
                }
                String id = sectionInput.readUTF();
                int distance = sectionInput.readByte();
                BlockState state = Blocks.stateFromNamespacedId(id);
                for (int i = 0; i < count; i++) {
                    chunk.setSerializedCellState(index++, state, distance);
                }
            }
            if (index != Chunk.VOLUME || sectionInput.available() != 0) {
                throw new IOException("invalid network chunk run length");
            }
        }
        column.generated = true;
        if (column.status != ChunkGenerationStatus.FULL) {
            column.status = ChunkGenerationStatus.FULL;
        }
        ChunkColumn previous = loadedColumns.put(columnKey(chunkX, chunkZ), column);
        if (previous != null) {
            removeColumnDynamicBlocks(previous);
        }
        registerColumnDynamicBlocks(column);
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            markDirtySection(chunkX, chunkY, chunkZ);
        }
        markColumnAndLoadedNeighborsDirty(chunkX, chunkZ);
        return columnKey(chunkX, chunkZ);
    }

    public BlockState getTemplateBlock(int x, int y, int z) {
        return getBlockState(x, y, z);
    }

    public void setTemplateBlock(int x, int y, int z, BlockState state) {
        setBlockState(x, y, z, state);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
        refreshDynamicCellsAround(x, y, z);
    }

    int getSurfaceHeight(int x, int z) {
        ChunkColumn column = loadedColumns.get(columnKey(worldToChunk(x), worldToChunk(z)));
        if (column == null) {
            return worldGenerator != null ? worldGenerator.estimateSurfaceHeight(x, z) : calculateTerrainHeight(x, z);
        }
        return column.getSurfaceHeightLocal(localBlockCoordinate(x), localBlockCoordinate(z));
    }

    float getAmbientShade(int x, int y, int z) {
        int surfaceY = getSurfaceHeight(x, z);
        int depth = surfaceY - y;
        float shade = 1.0f;
        if (depth > 0) {
            shade -= Math.min(0.50f, depth * 0.030f);
        }
        if (y < 8) {
            shade -= 0.04f;
        }
        if (isLiquidBlock(getBlock(x, y, z))) {
            shade = Math.max(shade, 0.55f);
        }
        if (depth > 2) {
            float torchShade = nearbyTorchShade(x, y, z);
            if (torchShade > shade) {
                shade = torchShade;
            }
        }
        return Math.max(0.42f, Math.min(1.0f, shade));
    }

    private float nearbyTorchShade(int x, int y, int z) {
        int radius = 2;
        int bestDistanceSquared = Integer.MAX_VALUE;
        for (int offsetY = -radius; offsetY <= radius; offsetY++) {
            int checkY = y + offsetY;
            if (!GameConfig.isWorldYInside(checkY)) {
                continue;
            }
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                    int distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
                    if (distanceSquared >= bestDistanceSquared || distanceSquared > radius * radius) {
                        continue;
                    }
                    if (getBlock(x + offsetX, checkY, z + offsetZ) == GameConfig.TORCH) {
                        bestDistanceSquared = distanceSquared;
                    }
                }
            }
        }
        if (bestDistanceSquared == Integer.MAX_VALUE) {
            return 0.0f;
        }
        return 0.56f + (1.0f - bestDistanceSquared / (float) (radius * radius)) * 0.38f;
    }

    float getColdSurfaceTint(int x, int y, int z) {
        if (getBlock(x, y + 1, z) == GameConfig.SNOW_LAYER
            || getBlock(x, y, z) == GameConfig.SNOW_LAYER
            || getBlock(x, y + 2, z) == GameConfig.SNOW_LAYER) {
            return 1.0f;
        }

        double temperature = biomeTemperature(x, z);
        double altitudeCold = y > GameConfig.SEA_LEVEL + 18 ? Math.min(0.35, (y - GameConfig.SEA_LEVEL - 18) / 72.0) : 0.0;
        double cold = Math.max(0.0, (-temperature - 0.08) * 2.0 + altitudeCold);
        return (float) Math.max(0.0, Math.min(1.0, cold));
    }

    float getTaigaSurfaceTint(int x, int z) {
        return getBiomeName(x, z).contains("Taiga") ? 1.0f : 0.0f;
    }

    String getBiomeName(int x, int z) {
        return worldGenerator != null ? worldGenerator.debugBiomeName(x, z) : "Unknown";
    }

    int getGeneratedSurfaceHeight(int x, int z) {
        return worldGenerator != null ? worldGenerator.debugSurfaceHeight(x, z) : calculateTerrainHeight(x, z);
    }

    int getVillageCellSizeChunks() {
        if (worldGenerator == null) {
            worldGenerator = new WorldGenerator(seed, terrainPreset);
        }
        return worldGenerator.debugVillageCellSizeChunks();
    }

    int[] getVillageCenterForCell(int cellX, int cellZ) {
        if (worldGenerator == null) {
            worldGenerator = new WorldGenerator(seed, terrainPreset);
        }
        return worldGenerator.debugVillageCenterForCell(cellX, cellZ);
    }

    int getActualSurfaceHeight(int x, int z) {
        ChunkColumn column = loadedColumns.get(columnKey(worldToChunk(x), worldToChunk(z)));
        if (column == null) {
            return getGeneratedSurfaceHeight(x, z);
        }
        return column.getSurfaceHeightLocal(localBlockCoordinate(x), localBlockCoordinate(z));
    }

    String getTerrainDebugInfo(int x, int z) {
        return worldGenerator != null ? worldGenerator.debugTerrainInfo(x, z) : "terrain=unknown";
    }

    String getDensityDebugInfo(int x, int y, int z) {
        return worldGenerator != null ? worldGenerator.debugDensityInfo(x, y, z) : "density=unknown";
    }

    ChunkGenerationStatus getChunkStatus(int chunkX, int chunkZ) {
        ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
        return column == null ? ChunkGenerationStatus.EMPTY : column.status;
    }

    String getRegionFileName(int chunkX, int chunkZ) {
        return RegionStorage.regionFileNameForChunk(chunkX, chunkZ);
    }

    boolean isFaceVisible(byte sourceBlock, int x, int y, int z) {
        if (!isInside(x, y, z)) {
            return true;
        }

        byte neighbor = getBlock(x, y, z);
        if (neighbor == GameConfig.AIR) {
            return true;
        }
        if (isLiquidBlock(sourceBlock)) {
            return GameConfig.fluidItemForBlock(neighbor) != GameConfig.fluidItemForBlock(sourceBlock);
        }
        if (sourceBlock == GameConfig.SNOW_LAYER) {
            return neighbor != GameConfig.SNOW_LAYER
                && (isTransparentBlock(neighbor) || !isSolidBlock(neighbor));
        }
        if (sourceBlock == GameConfig.OAK_LEAVES || sourceBlock == GameConfig.PINE_LEAVES || sourceBlock == GameConfig.BIRCH_LEAVES) {
            return neighbor != sourceBlock
                && neighbor != GameConfig.OAK_LOG
                && neighbor != GameConfig.PINE_LOG
                && neighbor != GameConfig.BIRCH_LOG;
        }
        return isTransparentBlock(neighbor) || !isSolidBlock(neighbor);
    }

    void cleanup() {
        flushLoadedColumns();
        backgroundSaveService.flush();
        saveContainers();
        for (Future<ChunkColumn> future : pendingColumns.values()) {
            future.cancel(true);
        }
        pendingColumns.clear();
        generationExecutor.shutdownNow();
        try {
            generationExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        backgroundSaveService.shutdown();
    }

    private double[] fenceGateSelectionBounds(BlockState state) {
        if (Blocks.isGateOpen(state)) {
            return new double[]{0.375, 0.0, 0.375, 0.625, 1.5, 0.625};
        }
        int facing = Blocks.gateFacing(state);
        if (facing == 0 || facing == 2) {
            return new double[]{0.0, 0.0, 0.375, 1.0, 1.5, 0.625};
        }
        return new double[]{0.375, 0.0, 0.0, 0.625, 1.5, 1.0};
    }

    boolean isTransparentBlock(byte block) {
        return block != GameConfig.AIR && !Blocks.isOpaque(block);
    }

    boolean isSolidBlock(byte block) {
        return Blocks.isSolid(block);
    }

    private boolean isCollisionSolid(BlockState state) {
        if (state == null || state.type == null) {
            return false;
        }
        if (state.type.numericId == (GameConfig.OAK_DOOR & 0xFF)) {
            return !Blocks.isDoorOpen(state);
        }
        if (state.type.numericId == (GameConfig.OAK_FENCE_GATE & 0xFF)) {
            return !Blocks.isGateOpen(state);
        }
        return state.type.isSolid();
    }

    private boolean intersectsAabb(double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ,
                                   double otherMinX, double otherMinY, double otherMinZ,
                                   double otherMaxX, double otherMaxY, double otherMaxZ) {
        return maxX > otherMinX + COLLISION_EPSILON && minX < otherMaxX - COLLISION_EPSILON
            && maxY > otherMinY + COLLISION_EPSILON && minY < otherMaxY - COLLISION_EPSILON
            && maxZ > otherMinZ + COLLISION_EPSILON && minZ < otherMaxZ - COLLISION_EPSILON;
    }

    boolean isLiquidBlock(byte block) {
        return Blocks.isLiquid(block);
    }

    boolean isCrossPlant(byte block) {
        return block == GameConfig.TALL_GRASS
            || block == GameConfig.SEAGRASS
            || block == GameConfig.KELP
            || block == GameConfig.RED_FLOWER
            || block == GameConfig.YELLOW_FLOWER
            || block == GameConfig.WHEAT_CROP
            || block == GameConfig.CARROT_CROP
            || block == GameConfig.POTATO_CROP
            || block == GameConfig.DEAD_BUSH
            || block == GameConfig.TORCH;
    }

    boolean isPlantBlock(byte block) {
        return Blocks.isPlant(block);
    }

    boolean isCubeBlock(byte block) {
        return block != GameConfig.AIR
            && block != GameConfig.OAK_FENCE
            && block != GameConfig.OAK_FENCE_GATE
            && block != GameConfig.RED_BED
            && block != GameConfig.OAK_DOOR
            && !isStairBlock(block)
            && !isCrossPlant(block);
    }

    boolean isStairBlock(byte block) {
        return block == GameConfig.OAK_STAIRS
            || block == GameConfig.PINE_STAIRS
            || block == GameConfig.BIRCH_STAIRS
            || block == GameConfig.STONE_STAIRS
            || block == GameConfig.COBBLESTONE_STAIRS;
    }

    int getFluidDistance(int x, int y, int z) {
        Chunk chunk = getChunkForBlock(x, y, z);
        if (chunk == null) {
            return -1;
        }
        return chunk.getFluidDistanceLocal(localBlockCoordinate(x), GameConfig.localYForWorldY(y), localBlockCoordinate(z));
    }

    private void tickDynamicBlocks(PlayerState player) {
        int playerChunkX = player == null ? 0 : worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = player == null ? 0 : worldToChunk((int) Math.floor(player.z));

        HashSet<Long> reactionCandidates = new HashSet<>();
        tickFluidSet(activeWaterCells, queuedWaterCells, activeWaterQueue, GameConfig.WATER, playerChunkX, playerChunkZ, reactionCandidates, MAX_WATER_CELLS_PER_WORLD_TICK);
        if (worldTickCounter % GameConfig.LAVA_FLOW_STEP_INTERVAL == 0) {
            tickFluidSet(activeLavaCells, queuedLavaCells, activeLavaQueue, GameConfig.LAVA, playerChunkX, playerChunkZ, reactionCandidates, MAX_LAVA_CELLS_PER_WORLD_TICK);
        }
        resolveFluidReactions(reactionCandidates);
        tickSand(playerChunkX, playerChunkZ);
        if ((worldTickCounter & 31L) == 0L) {
            tickGrassSpread(playerChunkX, playerChunkZ);
        }
        if ((worldTickCounter & 15L) == 0L) {
            tickCropGrowth(playerChunkX, playerChunkZ);
        }
    }

    private void tickCropGrowth(int playerChunkX, int playerChunkZ) {
        for (int attempt = 0; attempt < 48; attempt++) {
            int chunkX = playerChunkX + worldRandom.nextInt(7) - 3;
            int chunkZ = playerChunkZ + worldRandom.nextInt(7) - 3;
            ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
            if (column == null) {
                continue;
            }
            int x = chunkX * GameConfig.CHUNK_SIZE + worldRandom.nextInt(GameConfig.CHUNK_SIZE);
            int z = chunkZ * GameConfig.CHUNK_SIZE + worldRandom.nextInt(GameConfig.CHUNK_SIZE);
            int surfaceY = column.getSurfaceHeightLocal(localBlockCoordinate(x), localBlockCoordinate(z));
            int minY = Math.max(GameConfig.WORLD_MIN_Y + 1, surfaceY - 2);
            int maxY = Math.min(GameConfig.WORLD_MAX_Y, surfaceY + 2);
            for (int y = minY; y <= maxY; y++) {
                byte block = getBlock(x, y, z);
                if (!isCropBlock(block) || !canPlantStay(x, y, z)) {
                    continue;
                }
                BlockState state = getBlockState(x, y, z);
                int stage = state == null ? 0 : state.data;
                if (stage >= 7 || worldRandom.nextDouble() > 0.34) {
                    continue;
                }
                setBlockState(x, y, z, Blocks.withData(block, stage + 1));
                markDirtyBlock(x, y, z);
                break;
            }
        }
    }

    private void tickGrassSpread(int playerChunkX, int playerChunkZ) {
        for (int attempt = 0; attempt < 36; attempt++) {
            int chunkX = playerChunkX + worldRandom.nextInt(9) - 4;
            int chunkZ = playerChunkZ + worldRandom.nextInt(9) - 4;
            ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
            if (column == null) {
                continue;
            }
            int x = chunkX * GameConfig.CHUNK_SIZE + worldRandom.nextInt(GameConfig.CHUNK_SIZE);
            int z = chunkZ * GameConfig.CHUNK_SIZE + worldRandom.nextInt(GameConfig.CHUNK_SIZE);
            int y = column.getSurfaceHeightLocal(localBlockCoordinate(x), localBlockCoordinate(z));
            if (getBlock(x, y, z) != GameConfig.DIRT || getBlock(x, y + 1, z) != GameConfig.AIR) {
                continue;
            }
            if (!hasAdjacentGrass(x, y, z)) {
                continue;
            }
            setBlockState(x, y, z, GameConfig.GRASS, -1);
            markDirtyBlock(x, y, z);
        }
    }

    private boolean hasAdjacentGrass(int x, int y, int z) {
        return getBlock(x + 1, y, z) == GameConfig.GRASS
            || getBlock(x - 1, y, z) == GameConfig.GRASS
            || getBlock(x, y, z + 1) == GameConfig.GRASS
            || getBlock(x, y, z - 1) == GameConfig.GRASS
            || getBlock(x + 1, y + 1, z) == GameConfig.GRASS
            || getBlock(x - 1, y + 1, z) == GameConfig.GRASS
            || getBlock(x, y + 1, z + 1) == GameConfig.GRASS
            || getBlock(x, y + 1, z - 1) == GameConfig.GRASS;
    }

    private void tickFluidSet(HashSet<Long> activeCells, HashSet<Long> queuedCells, ArrayDeque<Long> queue, byte fluidItem, int playerChunkX, int playerChunkZ, HashSet<Long> reactionCandidates, int maxCells) {
        if (activeCells.isEmpty()) {
            return;
        }

        if (queue.isEmpty()) {
            refillDynamicQueue(activeCells, queuedCells, queue, maxCells * 4);
        }
        HashMap<Long, PendingBlockChange> pendingChanges = new HashMap<>();
        ArrayList<Long> processedKeys = new ArrayList<>();
        int processed = 0;
        int attempts = 0;
        int maxAttempts = Math.max(maxCells * 8, maxCells);
        while (processed < maxCells && attempts < maxAttempts && !queue.isEmpty()) {
            long blockKey = queue.removeFirst();
            if (!queuedCells.remove(blockKey)) {
                continue;
            }
            attempts++;
            if (!activeCells.contains(blockKey)) {
                continue;
            }
            int x = unpackBlockX(blockKey);
            int y = unpackBlockY(blockKey);
            int z = unpackBlockZ(blockKey);
            byte block = getBlock(x, y, z);
            if (GameConfig.fluidItemForBlock(block) != fluidItem) {
                activeCells.remove(blockKey);
                continue;
            }
            if (!isChunkWithinFluidSimulationDistance(worldToChunk(x), worldToChunk(z), playerChunkX, playerChunkZ)) {
                enqueueDynamicCell(activeCells, queuedCells, queue, blockKey);
                continue;
            }
            if (!shouldTickDynamicCell(blockKey, x, z, playerChunkX, playerChunkZ)) {
                enqueueDynamicCell(activeCells, queuedCells, queue, blockKey);
                continue;
            }
            processed++;
            processedKeys.add(blockKey);
            simulateFluidCell(x, y, z, fluidItem, pendingChanges);
        }

        if (pendingChanges.isEmpty()) {
            refreshProcessedFluidActiveStates(processedKeys);
            return;
        }

        ArrayList<PendingBlockChange> orderedChanges = new ArrayList<>(pendingChanges.values());
        orderedChanges.sort((left, right) -> {
            if (left.y != right.y) {
                return Integer.compare(left.y, right.y);
            }
            if (left.x != right.x) {
                return Integer.compare(left.x, right.x);
            }
            return Integer.compare(left.z, right.z);
        });
        for (PendingBlockChange change : orderedChanges) {
            byte current = getBlock(change.x, change.y, change.z);
            if (current == change.block
                && (!GameConfig.isLiquidBlock(change.block) || getFluidDistance(change.x, change.y, change.z) == change.distance)) {
                continue;
            }
            if (change.block == GameConfig.AIR) {
                if (GameConfig.fluidItemForBlock(current) != fluidItem) {
                    continue;
                }
            } else if (!isReplaceableForFluid(current) && GameConfig.fluidItemForBlock(current) != fluidItem) {
                continue;
            }

            setBlockState(change.x, change.y, change.z, change.block, change.distance);
            refreshDynamicCellsAround(change.x, change.y, change.z);
            markDirtyBlock(change.x, change.y, change.z);
            reactionCandidates.add(packBlock(change.x, change.y, change.z));
        }
        refreshProcessedFluidActiveStates(processedKeys);
    }

    private void refreshProcessedFluidActiveStates(ArrayList<Long> processedKeys) {
        for (long blockKey : processedKeys) {
            updateActiveStateAt(unpackBlockX(blockKey), unpackBlockY(blockKey), unpackBlockZ(blockKey));
        }
    }

    private void simulateFluidCell(int x, int y, int z, byte fluidItem, HashMap<Long, PendingBlockChange> pendingChanges) {
        byte block = getBlock(x, y, z);
        byte sourceBlock = GameConfig.sourceBlockForFluid(fluidItem);
        byte flowingBlock = GameConfig.flowingBlockForFluid(fluidItem);
        int maxDistance = GameConfig.fluidSpreadDistance(fluidItem);

        if (block != sourceBlock && block != flowingBlock) {
            return;
        }

        boolean fallingColumn = GameConfig.fluidItemForBlock(getBlock(x, y + 1, z)) == fluidItem;
        int currentDistance = block == sourceBlock || fallingColumn ? 0 : getFluidDistance(x, y, z);
        if (currentDistance < 0) {
            currentDistance = block == sourceBlock ? 0 : 1;
        }

        if (block == flowingBlock && !fallingColumn && !hasSimpleFluidSupport(x, y, z, fluidItem, currentDistance)) {
            scheduleBlockChange(pendingChanges, x, y, z, GameConfig.AIR, -1);
            return;
        }

        if (y > GameConfig.WORLD_MIN_Y && canIntroduceSimpleFluid(x, y - 1, z, fluidItem, 0)) {
            scheduleBlockChange(pendingChanges, x, y - 1, z, flowingBlock, 0);
            return;
        }

        if (!hasFluidHorizontalFloor(x, y, z, fluidItem)) {
            return;
        }

        int nextDistance = block == sourceBlock || fallingColumn ? 1 : currentDistance + 1;
        if (nextDistance > maxDistance) {
            return;
        }

        tryScheduleSimpleHorizontalFlow(pendingChanges, x + 1, y, z, fluidItem, nextDistance);
        tryScheduleSimpleHorizontalFlow(pendingChanges, x - 1, y, z, fluidItem, nextDistance);
        tryScheduleSimpleHorizontalFlow(pendingChanges, x, y, z + 1, fluidItem, nextDistance);
        tryScheduleSimpleHorizontalFlow(pendingChanges, x, y, z - 1, fluidItem, nextDistance);
    }

    private void tryScheduleSimpleHorizontalFlow(HashMap<Long, PendingBlockChange> pendingChanges, int x, int y, int z, byte fluidItem, int distance) {
        if (!canIntroduceSimpleFluid(x, y, z, fluidItem, distance)) {
            return;
        }
        scheduleBlockChange(pendingChanges, x, y, z, GameConfig.flowingBlockForFluid(fluidItem), distance);
    }

    private boolean canIntroduceSimpleFluid(int x, int y, int z, byte fluidItem, int distance) {
        if (!isInside(x, y, z)) {
            return false;
        }
        byte current = getBlock(x, y, z);
        if (current == GameConfig.sourceBlockForFluid(fluidItem)) {
            return false;
        }
        if (GameConfig.fluidItemForBlock(current) == fluidItem) {
            int currentDistance = getFluidDistance(x, y, z);
            return currentDistance < 0 || distance < currentDistance;
        }
        return isReplaceableForFluid(current);
    }

    private boolean hasSimpleFluidSupport(int x, int y, int z, byte fluidItem, int currentDistance) {
        if (GameConfig.fluidItemForBlock(getBlock(x, y + 1, z)) == fluidItem) {
            return true;
        }
        return hasSimpleFluidNeighborSupport(x + 1, y, z, fluidItem, currentDistance)
            || hasSimpleFluidNeighborSupport(x - 1, y, z, fluidItem, currentDistance)
            || hasSimpleFluidNeighborSupport(x, y, z + 1, fluidItem, currentDistance)
            || hasSimpleFluidNeighborSupport(x, y, z - 1, fluidItem, currentDistance);
    }

    private boolean hasSimpleFluidNeighborSupport(int x, int y, int z, byte fluidItem, int currentDistance) {
        byte neighbor = getBlock(x, y, z);
        if (neighbor == GameConfig.sourceBlockForFluid(fluidItem)) {
            return hasFluidHorizontalFloor(x, y, z, fluidItem);
        }
        if (GameConfig.fluidItemForBlock(neighbor) != fluidItem) {
            return false;
        }
        if (!hasFluidHorizontalFloor(x, y, z, fluidItem)) {
            return false;
        }
        int neighborDistance = getFluidDistance(x, y, z);
        return neighborDistance >= 0 && neighborDistance < currentDistance;
    }

    private boolean hasFluidHorizontalFloor(int x, int y, int z, byte fluidItem) {
        if (y <= GameConfig.WORLD_MIN_Y) {
            return true;
        }
        byte below = getBlock(x, y - 1, z);
        return below != GameConfig.AIR
            && GameConfig.fluidItemForBlock(below) != fluidItem
            && !isReplaceableForFluid(below);
    }

    private void tickSand(int playerChunkX, int playerChunkZ) {
        if (activeSandCells.isEmpty()) {
            return;
        }

        if (activeSandQueue.isEmpty()) {
            refillDynamicQueue(activeSandCells, queuedSandCells, activeSandQueue, MAX_SAND_CELLS_PER_WORLD_TICK * 4);
        }
        int processed = 0;
        int attempts = 0;
        int maxAttempts = Math.max(MAX_SAND_CELLS_PER_WORLD_TICK * 8, MAX_SAND_CELLS_PER_WORLD_TICK);
        while (processed < MAX_SAND_CELLS_PER_WORLD_TICK && attempts < maxAttempts && !activeSandQueue.isEmpty()) {
            long blockKey = activeSandQueue.removeFirst();
            if (!queuedSandCells.remove(blockKey)) {
                continue;
            }
            attempts++;
            if (!activeSandCells.contains(blockKey)) {
                continue;
            }
            int x = unpackBlockX(blockKey);
            int y = unpackBlockY(blockKey);
            int z = unpackBlockZ(blockKey);
            byte block = getBlock(x, y, z);
            if (!isFallingTerrainBlock(block)) {
                activeSandCells.remove(blockKey);
                continue;
            }
            if (!isChunkWithinFluidSimulationDistance(worldToChunk(x), worldToChunk(z), playerChunkX, playerChunkZ)) {
                enqueueDynamicCell(activeSandCells, queuedSandCells, activeSandQueue, blockKey);
                continue;
            }
            if (!shouldTickDynamicCell(blockKey, x, z, playerChunkX, playerChunkZ)) {
                enqueueDynamicCell(activeSandCells, queuedSandCells, activeSandQueue, blockKey);
                continue;
            }
            processed++;
            byte below = getBlock(x, y - 1, z);
            if (!isReplaceableForFallingBlock(below)) {
                refreshDynamicCellsAround(x, y, z);
                continue;
            }

            spawnFallingBlock(block, x, y, z);
        }
    }

    private void spawnFallingBlock(byte block, int x, int y, int z) {
        long blockKey = packBlock(x, y, z);
        if (fallingBlockSources.contains(blockKey) || getBlock(x, y, z) != block) {
            return;
        }

        fallingBlockSources.add(blockKey);
        activeSandCells.remove(blockKey);
        setBlockState(x, y, z, GameConfig.AIR, -1);
        updatePlantSupportAt(x, y + 1, z);
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
        fallingBlocks.add(new FallingBlock(block, x, y, z));
    }

    private void updateFallingBlocks(double deltaTime) {
        if (fallingBlocks.isEmpty()) {
            return;
        }

        for (int i = fallingBlocks.size() - 1; i >= 0; i--) {
            FallingBlock fallingBlock = fallingBlocks.get(i);
            fallingBlock.verticalVelocity = Math.max(
                fallingBlock.verticalVelocity - GameConfig.GRAVITY * deltaTime,
                -GameConfig.TERMINAL_VELOCITY
            );
            double previousY = fallingBlock.y;
            fallingBlock.y += fallingBlock.verticalVelocity * deltaTime;

            int blockX = (int) Math.floor(fallingBlock.x);
            int blockZ = (int) Math.floor(fallingBlock.z);
            if (!isChunkLoaded(worldToChunk(blockX), worldToChunk(blockZ))) {
                continue;
            }

            int landingY = findFallingBlockLandingY(blockX, (int) Math.floor(previousY), (int) Math.floor(fallingBlock.y), blockZ);
            if (fallingBlock.y <= landingY + COLLISION_EPSILON || landingY >= GameConfig.WORLD_MAX_Y) {
                bakeFallingBlock(fallingBlock, blockX, landingY, blockZ);
                fallingBlocks.remove(i);
            }
        }
    }

    private int findFallingBlockLandingY(int x, int previousY, int currentY, int z) {
        int startY = clamp(Math.max(previousY, currentY), GameConfig.WORLD_MIN_Y + 1, GameConfig.WORLD_MAX_Y);
        int endY = clamp(Math.min(previousY, currentY), GameConfig.WORLD_MIN_Y, GameConfig.WORLD_MAX_Y - 1);
        for (int y = startY - 1; y >= endY; y--) {
            if (!isReplaceableForFallingBlock(getBlock(x, y, z))) {
                return Math.min(y + 1, GameConfig.WORLD_MAX_Y);
            }
        }
        return findFallingBlockLandingY(x, currentY, z);
    }

    private int findFallingBlockLandingY(int x, int currentY, int z) {
        int startY = clamp(currentY, GameConfig.WORLD_MIN_Y + 1, GameConfig.WORLD_MAX_Y);
        for (int y = startY - 1; y >= GameConfig.WORLD_MIN_Y; y--) {
            if (!isReplaceableForFallingBlock(getBlock(x, y, z))) {
                return Math.min(y + 1, GameConfig.WORLD_MAX_Y);
            }
        }
        return GameConfig.WORLD_MIN_Y;
    }

    private void bakeFallingBlock(FallingBlock fallingBlock, int x, int y, int z) {
        fallingBlockSources.remove(packBlock(fallingBlock.blockX, fallingBlock.blockY, fallingBlock.blockZ));

        int placeY = clamp(y, GameConfig.WORLD_MIN_Y, GameConfig.WORLD_MAX_Y);
        while (placeY < GameConfig.WORLD_MAX_Y && !isReplaceableForFallingBlock(getBlock(x, placeY, z))) {
            placeY++;
        }

        setBlockState(x, placeY, z, fallingBlock.blockId, -1);
        updatePlantSupportAt(x, placeY + 1, z);
        refreshDynamicCellsAround(x, placeY, z);
        markDirtyBlock(x, placeY, z);
        refreshSurfaceHeight(x, z);
    }

    private void updateFurnaces(double deltaTime) {
        if (furnaces.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, FurnaceBlockEntity>> iterator = furnaces.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, FurnaceBlockEntity> entry = iterator.next();
            long key = entry.getKey();
            int x = unpackBlockX(key);
            int y = unpackBlockY(key);
            int z = unpackBlockZ(key);
            if (getBlock(x, y, z) != GameConfig.FURNACE) {
                iterator.remove();
                continue;
            }
            if (!isChunkLoaded(worldToChunk(x), worldToChunk(z))) {
                continue;
            }
            entry.getValue().tick(deltaTime);
        }
    }

    private void resolveFluidReactions(HashSet<Long> reactionCandidates) {
        for (long blockKey : reactionCandidates) {
            int x = unpackBlockX(blockKey);
            int y = unpackBlockY(blockKey);
            int z = unpackBlockZ(blockKey);
            resolveFluidReactionAt(x, y, z);
            resolveFluidReactionAt(x + 1, y, z);
            resolveFluidReactionAt(x - 1, y, z);
            resolveFluidReactionAt(x, y + 1, z);
            resolveFluidReactionAt(x, y - 1, z);
            resolveFluidReactionAt(x, y, z + 1);
            resolveFluidReactionAt(x, y, z - 1);
        }
    }

    private void resolveFluidReactionAt(int x, int y, int z) {
        byte lavaBlock = getBlock(x, y, z);
        if (!GameConfig.isLavaBlock(lavaBlock)) {
            return;
        }

        byte waterBlock = GameConfig.AIR;
        if (GameConfig.isWaterBlock(getBlock(x + 1, y, z))) {
            waterBlock = getBlock(x + 1, y, z);
        } else if (GameConfig.isWaterBlock(getBlock(x - 1, y, z))) {
            waterBlock = getBlock(x - 1, y, z);
        } else if (GameConfig.isWaterBlock(getBlock(x, y + 1, z))) {
            waterBlock = getBlock(x, y + 1, z);
        } else if (GameConfig.isWaterBlock(getBlock(x, y - 1, z))) {
            waterBlock = getBlock(x, y - 1, z);
        } else if (GameConfig.isWaterBlock(getBlock(x, y, z + 1))) {
            waterBlock = getBlock(x, y, z + 1);
        } else if (GameConfig.isWaterBlock(getBlock(x, y, z - 1))) {
            waterBlock = getBlock(x, y, z - 1);
        }

        if (waterBlock == GameConfig.AIR) {
            return;
        }

        setBlockState(x, y, z, getFluidReactionBlock(waterBlock, lavaBlock), -1);
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
        refreshSurfaceHeight(x, z);
    }

    private void scheduleBlockChange(HashMap<Long, PendingBlockChange> pendingChanges, int x, int y, int z, byte block, int distance) {
        if (!isInside(x, y, z)) {
            return;
        }
        byte currentBlock = getBlock(x, y, z);
        byte currentFluid = GameConfig.fluidItemForBlock(currentBlock);
        byte changeFluid = GameConfig.fluidItemForBlock(block);
        if (currentFluid != GameConfig.AIR && currentBlock == GameConfig.sourceBlockForFluid(currentFluid)) {
            if (block == GameConfig.AIR || block == GameConfig.flowingBlockForFluid(currentFluid)) {
                return;
            }
        }

        long key = packBlock(x, y, z);
        PendingBlockChange existing = pendingChanges.get(key);
        if (existing != null) {
            byte existingFluid = GameConfig.fluidItemForBlock(existing.block);
            if (existingFluid != GameConfig.AIR && existing.block == GameConfig.sourceBlockForFluid(existingFluid)
                && block != existing.block) {
                return;
            }
            if (changeFluid != GameConfig.AIR && block == GameConfig.sourceBlockForFluid(changeFluid)) {
                pendingChanges.put(key, new PendingBlockChange(x, y, z, block, distance));
                return;
            }
        }
        pendingChanges.put(key, new PendingBlockChange(x, y, z, block, distance));
    }

    private int countCurrentAdjacentWaterSources(int x, int y, int z) {
        int neighboringSources = 0;
        if (getBlock(x + 1, y, z) == GameConfig.WATER_SOURCE) {
            neighboringSources++;
        }
        if (getBlock(x - 1, y, z) == GameConfig.WATER_SOURCE) {
            neighboringSources++;
        }
        if (getBlock(x, y, z + 1) == GameConfig.WATER_SOURCE) {
            neighboringSources++;
        }
        if (getBlock(x, y, z - 1) == GameConfig.WATER_SOURCE) {
            neighboringSources++;
        }
        return neighboringSources;
    }

    private void getFluidFlowVector(int x, int y, int z, MutableVec3 output) {
        byte block = getBlock(x, y, z);
        if (!GameConfig.isWaterBlock(block)) {
            output.set(0.0, 0.0, 0.0);
            return;
        }

        long cacheKey = packBlock(x, y, z);
        CachedFluidFlow cachedFlow = waterFlowCache.get(cacheKey);
        if (cachedFlow != null) {
            output.set(cachedFlow.x, cachedFlow.y, cachedFlow.z);
            return;
        }

        int currentDistance = Math.max(0, getFluidDistance(x, y, z));
        double currentHeight = getFluidSurfaceHeight(x, y, z);
        double flowX = 0.0;
        double flowY = 0.0;
        double flowZ = 0.0;

        flowX = accumulateFlow(flowX, currentDistance, currentHeight, x + 1, y, z, 1, 0);
        flowX = accumulateFlow(flowX, currentDistance, currentHeight, x - 1, y, z, -1, 0);
        flowZ = accumulateFlow(flowZ, currentDistance, currentHeight, x, y, z + 1, 1, 0, true);
        flowZ = accumulateFlow(flowZ, currentDistance, currentHeight, x, y, z - 1, -1, 0, true);

        if (isWaterFallEdge(x, y, z + 1)) {
            flowZ += 1.8;
            flowY -= 0.8;
        }
        if (isWaterFallEdge(x, y, z - 1)) {
            flowZ -= 1.8;
            flowY -= 0.8;
        }
        if (isWaterFallEdge(x + 1, y, z)) {
            flowX += 1.8;
            flowY -= 0.8;
        }
        if (isWaterFallEdge(x - 1, y, z)) {
            flowX -= 1.8;
            flowY -= 0.8;
        }
        if (y > GameConfig.WORLD_MIN_Y && canFluidOccupyBlock(getBlock(x, y - 1, z), GameConfig.WATER)) {
            flowY -= 1.2;
        }

        double magnitude = Math.sqrt(flowX * flowX + flowY * flowY + flowZ * flowZ);
        if (magnitude < 1.0e-8) {
            waterFlowCache.put(cacheKey, new CachedFluidFlow(0.0, 0.0, 0.0));
            output.set(0.0, 0.0, 0.0);
            return;
        }
        double normalizedX = flowX / magnitude;
        double normalizedY = flowY / magnitude;
        double normalizedZ = flowZ / magnitude;
        waterFlowCache.put(cacheKey, new CachedFluidFlow(normalizedX, normalizedY, normalizedZ));
        output.set(normalizedX, normalizedY, normalizedZ);
    }

    private double accumulateFlow(double current, int currentDistance, double currentHeight,
                                  int neighborX, int neighborY, int neighborZ,
                                  int direction, int unused) {
        return accumulateFlow(current, currentDistance, currentHeight, neighborX, neighborY, neighborZ, direction, unused, false);
    }

    private double accumulateFlow(double current, int currentDistance, double currentHeight,
                                  int neighborX, int neighborY, int neighborZ,
                                  int direction, int unused, boolean zAxis) {
        byte neighbor = getBlock(neighborX, neighborY, neighborZ);
        if (GameConfig.isWaterBlock(neighbor)) {
            int neighborDistance = Math.max(0, getFluidDistance(neighborX, neighborY, neighborZ));
            double neighborHeight = getFluidSurfaceHeight(neighborX, neighborY, neighborZ);
            double weight = Math.max(0.0, neighborDistance - currentDistance)
                + Math.max(0.0, currentHeight - neighborHeight) * 3.0;
            if (weight > 0.0) {
                current += direction * weight;
            }
        } else if (isReplaceableForFluid(neighbor)) {
            current += direction * 0.25;
        }
        return current;
    }

    private boolean isWaterFallEdge(int x, int y, int z) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y) {
            return false;
        }
        byte sameLevel = getBlock(x, y, z);
        if (!canFluidOccupyBlock(sameLevel, GameConfig.WATER) && !GameConfig.isWaterBlock(sameLevel)) {
            return false;
        }
        byte below = getBlock(x, y - 1, z);
        return canFluidOccupyBlock(below, GameConfig.WATER) && !GameConfig.isWaterBlock(below);
    }

    private boolean canFluidOccupyBlock(byte block, byte fluidItem) {
        return isReplaceableForFluid(block) || GameConfig.fluidItemForBlock(block) == fluidItem;
    }

    private void updateMobAi(MobEntity mob, PlayerState player, double deltaTime) {
        if (isFishMob(mob)) {
            updateFishAi(mob, deltaTime);
            return;
        }
        if (mob.kind == MobKind.VILLAGER) {
            updateVillagerAi(mob, deltaTime);
            return;
        }
        mob.wanderTime -= deltaTime;
        if (mob.wanderTime <= 0.0) {
            if (mob.resting) {
                mob.resting = false;
                chooseMobDirection(mob, true);
                mob.wanderTime = 1.4 + mob.random.nextDouble() * 1.8;
            } else {
                mob.resting = true;
                mob.wanderTime = 0.8 + mob.random.nextDouble() * 1.2;
            }
        }

        double targetVelocityX = 0.0;
        double targetVelocityZ = 0.0;
        boolean pursuingPlayer = isHostileMob(mob) && canHostileMobPursuePlayer(mob, player);
        if (isPassiveMob(mob) && mob.fleeTimer > 0.0 && player != null) {
            double deltaX = mob.x - player.x;
            double deltaZ = mob.z - player.z;
            double horizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (horizontalDistanceSquared > 1.0e-8) {
                double horizontalDistance = Math.sqrt(horizontalDistanceSquared);
                double fleeSpeed = GameConfig.ZOMBIE_WANDER_SPEED * 1.65;
                targetVelocityX = deltaX / horizontalDistance * fleeSpeed;
                targetVelocityZ = deltaZ / horizontalDistance * fleeSpeed;
                mob.targetBodyYaw = Math.atan2(deltaZ, deltaX);
                mob.resting = false;
            }
        } else if (pursuingPlayer) {
            mob.resting = false;
            double deltaX = player.x - mob.x;
            double deltaZ = player.z - mob.z;
            double horizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (horizontalDistanceSquared > 1.0e-8) {
                double horizontalDistance = Math.sqrt(horizontalDistanceSquared);
                targetVelocityX = deltaX / horizontalDistance * GameConfig.ZOMBIE_CHASE_SPEED;
                targetVelocityZ = deltaZ / horizontalDistance * GameConfig.ZOMBIE_CHASE_SPEED;
                steerMobAroundObstacle(mob, targetVelocityX, targetVelocityZ, mobSteering);
                targetVelocityX = mobSteering.x;
                targetVelocityZ = mobSteering.z;
                mob.targetBodyYaw = Math.atan2(deltaZ, deltaX);
            }
        } else if (!mob.resting) {
            double wanderSpeed = isHostileMob(mob) ? GameConfig.ZOMBIE_WANDER_SPEED : GameConfig.ZOMBIE_WANDER_SPEED * 0.62;
            targetVelocityX = directionMoveX(mob.directionIndex) * wanderSpeed;
            targetVelocityZ = directionMoveZ(mob.directionIndex) * wanderSpeed;
            mob.targetBodyYaw = angleForDirectionIndex(mob.directionIndex);
        } else {
            mob.targetBodyYaw = mob.bodyYaw;
        }

        calculateMobSeparation(mob, zombieSeparation);
        targetVelocityX += zombieSeparation.x;
        targetVelocityZ += zombieSeparation.z;

        double targetSpeedSquared = targetVelocityX * targetVelocityX + targetVelocityZ * targetVelocityZ;
        double maxSpeed = isHostileMob(mob)
            ? GameConfig.ZOMBIE_CHASE_SPEED
            : GameConfig.ZOMBIE_WANDER_SPEED * (mob.fleeTimer > 0.0 ? 1.75 : 0.72);
        if (targetSpeedSquared > maxSpeed * maxSpeed) {
            double scale = maxSpeed / Math.sqrt(targetSpeedSquared);
            targetVelocityX *= scale;
            targetVelocityZ *= scale;
        }

        double blend = Math.min(1.0, deltaTime * 8.0);
        mob.velocityX += (targetVelocityX - mob.velocityX) * blend;
        mob.velocityZ += (targetVelocityZ - mob.velocityZ) * blend;
    }

    private void updateFishAi(MobEntity fish, double deltaTime) {
        fish.wanderTime -= deltaTime;
        if (fish.wanderTime <= 0.0) {
            chooseMobDirection(fish, false);
            fish.wanderTime = 0.9 + fish.random.nextDouble() * 1.6;
            fish.wanderDirection = fish.random.nextDouble() - 0.5;
        }
        double speed = fish.kind == MobKind.SALMON ? 1.25 : 1.05;
        double targetVelocityX = directionMoveX(fish.directionIndex) * speed;
        double targetVelocityZ = directionMoveZ(fish.directionIndex) * speed;
        fish.targetBodyYaw = angleForDirectionIndex(fish.directionIndex);
        if (!GameConfig.isWaterBlock(getBlock((int) Math.floor(fish.x), (int) Math.floor(fish.y), (int) Math.floor(fish.z)))) {
            fish.wanderDirection = -0.8;
        }
        double targetVelocityY = clamp(fish.wanderDirection * 0.85, -0.85, 0.85);
        double blend = Math.min(1.0, deltaTime * 5.0);
        fish.velocityX += (targetVelocityX - fish.velocityX) * blend;
        fish.velocityZ += (targetVelocityZ - fish.velocityZ) * blend;
        fish.verticalVelocity += (targetVelocityY - fish.verticalVelocity) * blend;
    }

    private void updateVillagerAi(MobEntity villager, double deltaTime) {
        villager.wanderTime -= deltaTime;
        double homeDistance = Math.sqrt(distanceSquared(villager.x, villager.z, villager.homeX, villager.homeZ));
        boolean night = !isDaytime();
        if (homeDistance > 24.0) {
            double dx = villager.homeX - villager.x;
            double dz = villager.homeZ - villager.z;
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            villager.velocityX += (dx / len * 0.85 - villager.velocityX) * Math.min(1.0, deltaTime * 5.0);
            villager.velocityZ += (dz / len * 0.85 - villager.velocityZ) * Math.min(1.0, deltaTime * 5.0);
            villager.targetBodyYaw = Math.atan2(dz, dx);
            return;
        }
        if (villager.wanderTime <= 0.0) {
            villager.resting = villager.random.nextDouble() < (night ? 0.78 : 0.34);
            chooseMobDirection(villager, true);
            villager.wanderTime = night
                ? 2.4 + villager.random.nextDouble() * 3.2
                : 1.2 + villager.random.nextDouble() * 2.4;
        }
        double targetVelocityX = 0.0;
        double targetVelocityZ = 0.0;
        if (!villager.resting) {
            double speed = night ? 0.26 : 0.56;
            targetVelocityX = directionMoveX(villager.directionIndex) * speed;
            targetVelocityZ = directionMoveZ(villager.directionIndex) * speed;
            villager.targetBodyYaw = angleForDirectionIndex(villager.directionIndex);
        }
        calculateMobSeparation(villager, zombieSeparation);
        targetVelocityX += zombieSeparation.x * 0.5;
        targetVelocityZ += zombieSeparation.z * 0.5;
        double blend = Math.min(1.0, deltaTime * 5.0);
        villager.velocityX += (targetVelocityX - villager.velocityX) * blend;
        villager.velocityZ += (targetVelocityZ - villager.velocityZ) * blend;
    }

    private void updateMobMovement(MobEntity mob, boolean inWater, double deltaTime) {
        if (inWater) {
            sampleFluidFlow(mob.x, mob.y, mob.z, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT, GameConfig.WATER, sampledFluidFlow);
            mob.velocityX += sampledFluidFlow.x * GameConfig.WATER_ENTITY_FLOW_PUSH * deltaTime;
            mob.velocityZ += sampledFluidFlow.z * GameConfig.WATER_ENTITY_FLOW_PUSH * deltaTime;
            mob.verticalVelocity += sampledFluidFlow.y * GameConfig.WATER_VERTICAL_FLOW_PUSH * deltaTime;
            mob.velocityX *= 0.82;
            mob.velocityZ *= 0.82;
            mob.verticalVelocity -= GameConfig.WATER_SINK_ACCELERATION * deltaTime;
            double tickScale = deltaTime / GameConfig.PHYSICS_TICK_SECONDS;
            mob.verticalVelocity *= Math.pow(GameConfig.WATER_DRAG_PER_TICK, tickScale);
        }

        double moveX = mob.velocityX * deltaTime;
        double moveZ = mob.velocityZ * deltaTime;
        boolean movedHorizontally = moveMobHorizontal(mob, moveX, moveZ, deltaTime);
        if (!movedHorizontally && (Math.abs(moveX) > 1.0e-8 || Math.abs(moveZ) > 1.0e-8)) {
            mob.velocityX *= 0.25;
            mob.velocityZ *= 0.25;
            redirectMobAfterCollision(mob);
        }

        if (Double.isNaN(mob.stepTargetY)) {
            if (!inWater) {
                mob.verticalVelocity = Math.max(mob.verticalVelocity - GameConfig.GRAVITY * deltaTime, -GameConfig.TERMINAL_VELOCITY);
            }
            moveMobVertical(mob, mob.verticalVelocity * deltaTime);
            mob.isGrounded = isMobStandingOnGround(mob);
        }
        mob.bodyYaw = turnTowardAngle(mob.bodyYaw, mob.targetBodyYaw, GameConfig.ZOMBIE_TURN_SPEED * deltaTime);

        double horizontalSpeed = Math.sqrt(mob.velocityX * mob.velocityX + mob.velocityZ * mob.velocityZ);
        if (movedHorizontally && horizontalSpeed > 0.02) {
            mob.walkCycle += horizontalSpeed * deltaTime * (mob.isGrounded ? 8.0 : 5.0);
        } else {
            mob.walkCycle += (0.0 - mob.walkCycle) * Math.min(1.0, deltaTime * 7.0);
        }
    }

    private boolean moveMobHorizontal(MobEntity mob, double moveX, double moveZ, double deltaTime) {
        if (Math.abs(moveX) < 1e-8 && Math.abs(moveZ) < 1e-8) {
            return false;
        }

        double nextX = mob.x + moveX;
        double nextZ = mob.z + moveZ;
        if (canZombieOccupy(nextX, mob.y, nextZ)) {
            mob.x = nextX;
            mob.z = nextZ;
            mob.stepTargetY = Double.NaN;
            return true;
        }
        if ((mob.isGrounded || !Double.isNaN(mob.stepTargetY)) && canMobStepUp(mob, moveX, moveZ)) {
            if (Double.isNaN(mob.stepTargetY) || mob.stepTargetY < mob.y + 0.5) {
                mob.stepTargetY = mob.y + 1.0;
            }
            double raisedY = Math.min(mob.stepTargetY, mob.y + GameConfig.ZOMBIE_STEP_SPEED * deltaTime);
            if (canZombieOccupy(mob.x, raisedY, mob.z)) {
                mob.y = raisedY;
            }
            mob.verticalVelocity = 0.0;
            mob.isGrounded = false;
            if (mob.y + 1.0e-4 >= mob.stepTargetY && canZombieOccupy(nextX, mob.y, nextZ)) {
                mob.x = nextX;
                mob.z = nextZ;
                mob.stepTargetY = Double.NaN;
            }
            return true;
        }

        mob.stepTargetY = Double.NaN;
        return false;
    }

    private void moveMobVertical(MobEntity mob, double moveY) {
        if (Math.abs(moveY) < 1e-8) {
            return;
        }

        double nextY = mob.y + moveY;
        if (!collides(mob.x, nextY, mob.z, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT)) {
            mob.y = nextY;
            return;
        }

        if (moveY > 0.0) {
            int guard = 0;
            while (collides(mob.x, nextY, mob.z, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT) && guard++ < 256) {
                nextY -= 0.01;
            }
        } else {
            int guard = 0;
            while (collides(mob.x, nextY, mob.z, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT) && guard++ < 256) {
                nextY += 0.01;
            }
            mob.isGrounded = true;
        }

        mob.y = nextY;
        mob.verticalVelocity = 0.0;
    }

    private boolean canMobStepUp(MobEntity mob, double moveX, double moveZ) {
        if (Math.abs(moveX) < 1e-8 && Math.abs(moveZ) < 1e-8) {
            return false;
        }

        double nextX = mob.x + moveX;
        double nextZ = mob.z + moveZ;
        if (!collides(nextX, mob.y, nextZ, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT)) {
            return false;
        }
        return canZombieOccupy(nextX, mob.y + 1.0, nextZ);
    }

    private boolean isMobStandingOnGround(MobEntity mob) {
        return collides(mob.x, mob.y - 0.05, mob.z, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT);
    }

    private boolean canZombieOccupy(double x, double y, double z) {
        int chunkX = worldToChunk((int) Math.floor(x));
        int chunkZ = worldToChunk((int) Math.floor(z));
        if (!isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        return !collides(x, y, z, GameConfig.ZOMBIE_RADIUS, GameConfig.ZOMBIE_HEIGHT);
    }

    private void chooseMobDirection(MobEntity mob, boolean preferHomeAxis) {
        int directionIndex;
        if (preferHomeAxis && distanceSquared(mob.x, mob.z, mob.homeX, mob.homeZ) > 4.0) {
            double deltaX = mob.homeX - mob.x;
            double deltaZ = mob.homeZ - mob.z;
            if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
                directionIndex = deltaX >= 0.0 ? 0 : 2;
            } else {
                directionIndex = deltaZ >= 0.0 ? 1 : 3;
            }
        } else {
            directionIndex = mob.random.nextInt(4);
        }

        mob.directionIndex = directionIndex;
        mob.wanderDirection = angleForDirectionIndex(directionIndex);
        mob.targetBodyYaw = mob.wanderDirection;
    }

    private void redirectMobAfterCollision(MobEntity mob) {
        if (tryRedirectMob(mob, (mob.directionIndex + 2) & 3)
            || tryRedirectMob(mob, (mob.directionIndex + 1) & 3)
            || tryRedirectMob(mob, (mob.directionIndex + 3) & 3)) {
            return;
        }

        mob.resting = true;
        mob.wanderTime = 0.8 + mob.random.nextDouble() * 1.2;
        mob.targetBodyYaw = angleForDirectionIndex(mob.random.nextInt(4));
    }

    private boolean tryRedirectMob(MobEntity mob, int candidate) {
        double moveX = directionMoveX(candidate) * 0.25;
        double moveZ = directionMoveZ(candidate) * 0.25;
        boolean canStep = mob.isGrounded && canMobStepUp(mob, moveX, moveZ);
        if (!canZombieOccupy(mob.x + moveX, mob.y, mob.z + moveZ) && !canStep) {
            return false;
        }

        mob.directionIndex = candidate;
        mob.wanderDirection = angleForDirectionIndex(candidate);
        mob.targetBodyYaw = mob.wanderDirection;
        mob.resting = false;
        mob.wanderTime = 1.5 + mob.random.nextDouble() * 1.5;
        return true;
    }

    private double directionMoveX(int directionIndex) {
        switch (directionIndex & 3) {
            case 0:
                return 1.0;
            case 2:
                return -1.0;
            default:
                return 0.0;
        }
    }

    private double directionMoveZ(int directionIndex) {
        switch (directionIndex & 3) {
            case 1:
                return 1.0;
            case 3:
                return -1.0;
            default:
                return 0.0;
        }
    }

    private double angleForDirectionIndex(int directionIndex) {
        return (directionIndex & 3) * Math.PI * 0.5;
    }

    private double turnTowardAngle(double current, double target, double maxStep) {
        double delta = wrapAngle(target - current);
        if (delta > maxStep) {
            delta = maxStep;
        } else if (delta < -maxStep) {
            delta = -maxStep;
        }
        return wrapAngle(current + delta);
    }

    private double wrapAngle(double angle) {
        while (angle <= -Math.PI) {
            angle += Math.PI * 2.0;
        }
        while (angle > Math.PI) {
            angle -= Math.PI * 2.0;
        }
        return angle;
    }

    private void steerMobAroundObstacle(MobEntity mob, double velocityX, double velocityZ, MutableVec2 output) {
        double speedSquared = velocityX * velocityX + velocityZ * velocityZ;
        if (speedSquared < 1.0e-8) {
            output.set(0.0, 0.0);
            return;
        }

        double speed = Math.sqrt(speedSquared);
        double probeScale = 0.35 / speed;
        double probeX = velocityX * probeScale;
        double probeZ = velocityZ * probeScale;
        if (canZombieOccupy(mob.x + probeX, mob.y, mob.z + probeZ)
            || (mob.isGrounded && canMobStepUp(mob, probeX, probeZ))) {
            output.set(velocityX, velocityZ);
            return;
        }

        double signX = Math.signum(velocityX);
        double signZ = Math.signum(velocityZ);
        if (Math.abs(velocityX) >= Math.abs(velocityZ)) {
            if (signX != 0.0 && canZombieOccupy(mob.x + signX * 0.35, mob.y, mob.z)) {
                output.set(signX * speed, 0.0);
                return;
            }
            if (signZ != 0.0 && canZombieOccupy(mob.x, mob.y, mob.z + signZ * 0.35)) {
                output.set(0.0, signZ * speed);
                return;
            }
        } else {
            if (signZ != 0.0 && canZombieOccupy(mob.x, mob.y, mob.z + signZ * 0.35)) {
                output.set(0.0, signZ * speed);
                return;
            }
            if (signX != 0.0 && canZombieOccupy(mob.x + signX * 0.35, mob.y, mob.z)) {
                output.set(signX * speed, 0.0);
                return;
            }
        }

        double leftX = -velocityZ / speed * speed;
        double leftZ = velocityX / speed * speed;
        if (canZombieOccupy(mob.x + leftX / speed * 0.35, mob.y, mob.z + leftZ / speed * 0.35)) {
            output.set(leftX, leftZ);
            return;
        }
        if (canZombieOccupy(mob.x - leftX / speed * 0.35, mob.y, mob.z - leftZ / speed * 0.35)) {
            output.set(-leftX, -leftZ);
            return;
        }
        output.set(0.0, 0.0);
    }

    private void calculateMobSeparation(MobEntity mob, MutableVec2 output) {
        double separationX = 0.0;
        double separationZ = 0.0;
        double separationDistanceSquared = GameConfig.ZOMBIE_SEPARATION_DISTANCE * GameConfig.ZOMBIE_SEPARATION_DISTANCE;
        for (MobEntity other : mobs) {
            if (other == mob) {
                continue;
            }
            double deltaX = mob.x - other.x;
            double deltaZ = mob.z - other.z;
            double distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
            if (distanceSquared < 1.0e-6 || distanceSquared > separationDistanceSquared) {
                continue;
            }
            double distance = Math.sqrt(distanceSquared);
            double strength = (GameConfig.ZOMBIE_SEPARATION_DISTANCE - distance) / GameConfig.ZOMBIE_SEPARATION_DISTANCE;
            separationX += deltaX / distance * strength * 0.85;
            separationZ += deltaZ / distance * strength * 0.85;
        }
        output.set(separationX, separationZ);
    }

    private boolean canHostileMobPursuePlayer(MobEntity mob, PlayerState player) {
        if (player == null || player.creativeMode || player.spectatorMode || player.health <= 0) {
            return false;
        }
        if (Math.abs(player.y - mob.y) > GameConfig.ZOMBIE_AGGRO_Y_LIMIT) {
            return false;
        }
        return distanceSquared(mob.x, mob.z, player.x, player.z) <= GameConfig.ZOMBIE_VIEW_DISTANCE * GameConfig.ZOMBIE_VIEW_DISTANCE;
    }

    private void maybeQueueHostileMobGrowl(MobEntity mob, PlayerState player, double deltaTime) {
        if (!canHostileMobPursuePlayer(mob, player) || mob.growlCooldown > 0.0) {
            mob.growlCooldown = Math.max(0.0, mob.growlCooldown - deltaTime);
            return;
        }
        double distanceToPlayerSquared = distanceSquared(mob.x, mob.z, player.x, player.z);
        if (distanceToPlayerSquared < ZOMBIE_GROWL_DISTANCE_SQUARED
            && mob.random.nextDouble() < deltaTime * 0.45) {
            mob.growlCooldown = 4.0 + mob.random.nextDouble() * 5.0;
            mob.growlQueued = true;
        }
    }

    private void maybeQueuePassiveMobSound(MobEntity mob, double deltaTime) {
        mob.growlCooldown = Math.max(0.0, mob.growlCooldown - deltaTime);
        if (mob.growlCooldown > 0.0) {
            return;
        }
        if (mob.random.nextDouble() < deltaTime * 0.10) {
            mob.growlCooldown = 5.0 + mob.random.nextDouble() * 7.0;
            mob.growlQueued = true;
        }
    }

    private void tryHostileMobAttack(MobEntity mob, PlayerState player) {
        mob.attackCooldown = Math.max(0.0, mob.attackCooldown - GameConfig.PHYSICS_TICK_SECONDS);
        if (!canHostileMobPursuePlayer(mob, player) || mob.attackCooldown > 0.0 || player.health <= 0) {
            return;
        }
        double distanceToPlayerSquared = distanceSquared(mob.x, mob.z, player.x, player.z);
        if (distanceToPlayerSquared <= GameConfig.ZOMBIE_ATTACK_RANGE * GameConfig.ZOMBIE_ATTACK_RANGE
            && Math.abs((mob.y + 0.9) - (player.y + 0.9)) <= 1.35) {
            applyPlayerDamage(player, GameConfig.ZOMBIE_ATTACK_DAMAGE);
            mob.attackCooldown = GameConfig.ZOMBIE_ATTACK_COOLDOWN;
            mob.growlQueued = true;
        }
    }

    private void applyPlayerDamage(PlayerState player, double amount) {
        if (player == null || player.health <= 0.0 || player.creativeMode || player.spectatorMode) {
            if (player != null && player.health <= 0.0) {
                player.health = 0.0;
            }
            return;
        }
        int armor = Math.max(0, Math.min(20, player.armorProtection));
        double protectedDamage = Math.ceil(amount * (1.0 - armor * 0.04) * 2.0) / 2.0;
        player.health = Math.max(0.0, player.health - Math.max(0.5, protectedDamage));
    }

    private void maybeSpawnZombieNearPlayer(PlayerState player) {
        if (player == null || player.spectatorMode || mobs.size() >= HOSTILE_MOB_TARGET_COUNT || hostileMobSpawnCooldown > 0.0) {
            return;
        }

        for (int attempt = 0; attempt < 6; attempt++) {
            int offsetX = worldRandom.nextInt(36) - 18;
            int offsetZ = worldRandom.nextInt(36) - 18;
            if (Math.abs(offsetX) < 8 && Math.abs(offsetZ) < 8) {
                continue;
            }
            int blockX = (int) Math.floor(player.x) + offsetX;
            int blockZ = (int) Math.floor(player.z) + offsetZ;
            int chunkX = worldToChunk(blockX);
            int chunkZ = worldToChunk(blockZ);
            if (!isChunkLoaded(chunkX, chunkZ)) {
                submitColumnTask(chunkX, chunkZ);
                continue;
            }
            int surfaceY = getSurfaceHeight(blockX, blockZ);
            if (surfaceY <= GameConfig.WORLD_MIN_Y || surfaceY + 2 > GameConfig.WORLD_MAX_Y) {
                continue;
            }
            if (getBlock(blockX, surfaceY + 1, blockZ) != GameConfig.AIR || getBlock(blockX, surfaceY + 2, blockZ) != GameConfig.AIR) {
                continue;
            }
            if (GameConfig.isLiquidBlock(getBlock(blockX, surfaceY, blockZ))) {
                continue;
            }
            boolean day = isDaytime();
            if (day && !isExposedToSky(blockX, surfaceY + 2, blockZ) && worldRandom.nextDouble() < 0.85) {
                continue;
            }
            if (!day && isExposedToSky(blockX, surfaceY + 2, blockZ) && worldRandom.nextDouble() < 0.45) {
                continue;
            }
            MobKind kind = chooseSpawnMobKind(day, blockX, surfaceY, blockZ);
            if ((kind == MobKind.PIG || kind == MobKind.SHEEP || kind == MobKind.COW)
                && !isValidPassiveSpawnSurface(blockX, surfaceY, blockZ)) {
                continue;
            }
            double mobX = blockX + 0.5;
            double mobY = surfaceY + 1.01;
            double mobZ = blockZ + 0.5;
            mobs.add(new MobEntity(kind, mobX, mobY, mobZ, mobX, mobZ, worldRandom));
            hostileMobSpawnCooldown = day ? 2.0 + worldRandom.nextDouble() * 2.4 : 1.5 + worldRandom.nextDouble() * 1.5;
            return;
        }

        hostileMobSpawnCooldown = 0.8;
    }

    private void maybeSpawnFishNearPlayer(PlayerState player) {
        if (player == null || player.spectatorMode || fishSpawnCooldown > 0.0 || countFishMobs() >= FISH_MOB_TARGET_COUNT) {
            return;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            int offsetX = worldRandom.nextInt(56) - 28;
            int offsetZ = worldRandom.nextInt(56) - 28;
            if (Math.abs(offsetX) < 10 && Math.abs(offsetZ) < 10) {
                continue;
            }
            int blockX = (int) Math.floor(player.x) + offsetX;
            int blockZ = (int) Math.floor(player.z) + offsetZ;
            int chunkX = worldToChunk(blockX);
            int chunkZ = worldToChunk(blockZ);
            if (!isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }
            int surfaceY = getSurfaceHeight(blockX, blockZ);
            if (surfaceY >= GameConfig.SEA_LEVEL - 2 || surfaceY <= GameConfig.WORLD_MIN_Y) {
                continue;
            }
            int spawnY = clamp(surfaceY + 2 + worldRandom.nextInt(Math.max(1, GameConfig.SEA_LEVEL - surfaceY - 2)), surfaceY + 1, GameConfig.SEA_LEVEL - 1);
            if (!GameConfig.isWaterBlock(getBlock(blockX, spawnY, blockZ)) || !GameConfig.isWaterBlock(getBlock(blockX, spawnY + 1, blockZ))) {
                continue;
            }
            MobKind kind = worldRandom.nextDouble() < 0.72 ? MobKind.HERRING : MobKind.SALMON;
            int group = kind == MobKind.HERRING ? 3 + worldRandom.nextInt(4) : 1 + worldRandom.nextInt(3);
            for (int i = 0; i < group && countFishMobs() < FISH_MOB_TARGET_COUNT; i++) {
                double mobX = blockX + 0.25 + worldRandom.nextDouble() * 0.5 + (worldRandom.nextDouble() - 0.5) * 2.2;
                double mobY = spawnY + (worldRandom.nextDouble() - 0.5) * 1.2;
                double mobZ = blockZ + 0.25 + worldRandom.nextDouble() * 0.5 + (worldRandom.nextDouble() - 0.5) * 2.2;
                if (GameConfig.isWaterBlock(getBlock((int) Math.floor(mobX), (int) Math.floor(mobY), (int) Math.floor(mobZ)))) {
                    mobs.add(new MobEntity(kind, mobX, mobY, mobZ, mobX, mobZ, worldRandom));
                }
            }
            fishSpawnCooldown = 1.4 + worldRandom.nextDouble() * 1.8;
            return;
        }
        fishSpawnCooldown = 0.8;
    }

    private int countFishMobs() {
        int count = 0;
        for (MobEntity mob : mobs) {
            if (isFishMob(mob)) {
                count++;
            }
        }
        return count;
    }

    private MobKind chooseSpawnMobKind(boolean day, int blockX, int surfaceY, int blockZ) {
        if (!day) {
            return worldRandom.nextBoolean() ? MobKind.ZOMBIE : MobKind.SKELETON;
        }
        double roll = worldRandom.nextDouble();
        if (roll < 0.34) {
            return MobKind.PIG;
        }
        if (roll < 0.67) {
            return MobKind.SHEEP;
        }
        return MobKind.COW;
    }

    private boolean isValidPassiveSpawnSurface(int blockX, int surfaceY, int blockZ) {
        byte surface = getBlock(blockX, surfaceY, blockZ);
        if (surface != GameConfig.GRASS && surface != GameConfig.SAND) {
            return false;
        }
        byte support = getBlock(blockX, surfaceY - 1, blockZ);
        return Blocks.isSolid(support)
            && getBlock(blockX, surfaceY + 1, blockZ) == GameConfig.AIR
            && getBlock(blockX, surfaceY + 2, blockZ) == GameConfig.AIR;
    }

    private boolean blockIntersectsPlayerHitbox(int blockX, int blockY, int blockZ, byte block, PlayerState player) {
        if (player == null || player.spectatorMode) {
            return false;
        }
        double[] bounds = blockBounds(blockX, blockY, blockZ, block);
        return intersectsEntityBox(
            bounds,
            player.x - GameConfig.PLAYER_RADIUS,
            player.y,
            player.z - GameConfig.PLAYER_RADIUS,
            player.x + GameConfig.PLAYER_RADIUS,
            player.y + player.height(),
            player.z + GameConfig.PLAYER_RADIUS
        );
    }

    private boolean blockIntersectsEntityHitboxes(int blockX, int blockY, int blockZ, byte block, PlayerState player) {
        if (blockIntersectsPlayerHitbox(blockX, blockY, blockZ, block, player)) {
            return true;
        }
        double[] bounds = blockBounds(blockX, blockY, blockZ, block);
        for (MobEntity mob : mobs) {
            double radius = mob.radius();
            if (intersectsEntityBox(
                bounds,
                mob.x - radius,
                mob.y,
                mob.z - radius,
                mob.x + radius,
                mob.y + mob.height(),
                mob.z + radius
            )) {
                return true;
            }
        }
        return false;
    }

    private double[] blockBounds(int blockX, int blockY, int blockZ, byte block) {
        BlockState state = new BlockState(Blocks.typeFromLegacyId(block));
        double[] bounds = selectionBounds(block, state);
        return new double[]{
            blockX + bounds[0],
            blockY + bounds[1],
            blockZ + bounds[2],
            blockX + bounds[3],
            blockY + bounds[4],
            blockZ + bounds[5]
        };
    }

    private boolean intersectsEntityBox(double[] bounds,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        return maxX > bounds[0] && minX < bounds[3]
            && maxY > bounds[1] && minY < bounds[4]
            && maxZ > bounds[2] && minZ < bounds[5];
    }

    private int ensureColumnsAround(int centerChunkX, int centerChunkZ, int chunkRadius, boolean synchronousOnly) {
        return ensureColumnsAround(centerChunkX, centerChunkZ, chunkRadius, synchronousOnly, MAX_ASYNC_COLUMN_SUBMISSIONS_PER_TICK);
    }

    private int ensureColumnsAround(int centerChunkX, int centerChunkZ, int chunkRadius, boolean synchronousOnly, int asyncSubmissionLimit) {
        int syncRadius = synchronousOnly ? chunkRadius : 0;
        int submittedAsync = 0;
        for (int radius = 0; radius <= chunkRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                submittedAsync = ensureColumnAtRadius(centerChunkX + dx, centerChunkZ - radius, radius, syncRadius, synchronousOnly, submittedAsync, asyncSubmissionLimit);
                if (radius != 0) {
                    submittedAsync = ensureColumnAtRadius(centerChunkX + dx, centerChunkZ + radius, radius, syncRadius, synchronousOnly, submittedAsync, asyncSubmissionLimit);
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                submittedAsync = ensureColumnAtRadius(centerChunkX - radius, centerChunkZ + dz, radius, syncRadius, synchronousOnly, submittedAsync, asyncSubmissionLimit);
                if (radius != 0) {
                    submittedAsync = ensureColumnAtRadius(centerChunkX + radius, centerChunkZ + dz, radius, syncRadius, synchronousOnly, submittedAsync, asyncSubmissionLimit);
                }
            }
        }
        return submittedAsync;
    }

    private int ensureColumnAtRadius(int chunkX, int chunkZ, int radius, int syncRadius, boolean synchronousOnly, int submittedAsync, int asyncSubmissionLimit) {
        if (radius <= syncRadius || synchronousOnly) {
            ensureColumnLoadedSync(chunkX, chunkZ);
        } else if (submittedAsync < asyncSubmissionLimit) {
            if (submitColumnTask(chunkX, chunkZ)) {
                submittedAsync++;
            }
        }
        return submittedAsync;
    }

    private void unloadFarColumns(int playerChunkX, int playerChunkZ, int unloadRadius) {
        Iterator<Map.Entry<Long, ChunkColumn>> iterator = loadedColumns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkColumn> entry = iterator.next();
            ChunkColumn column = entry.getValue();
            int horizontalDistance = Math.max(Math.abs(column.chunkX - playerChunkX), Math.abs(column.chunkZ - playerChunkZ));
            if (horizontalDistance <= unloadRadius) {
                continue;
            }
            saveColumnIfDirty(column);
            removeColumnDynamicBlocks(column);
            iterator.remove();
        }
    }

    private int drainGeneratedColumns() {
        return drainGeneratedColumns(MAX_COLUMN_INTEGRATIONS_PER_FRAME, COLUMN_INTEGRATION_BUDGET_NS);
    }

    private int drainGeneratedColumns(int maxColumns, long budgetNs) {
        readyColumnKeys.clear();
        for (Map.Entry<Long, Future<ChunkColumn>> entry : pendingColumns.entrySet()) {
            if (entry.getValue().isDone()) {
                readyColumnKeys.add(entry.getKey());
            }
        }
        if (readyColumnKeys.isEmpty()) {
            return 0;
        }

        readyColumnKeys.sort((left, right) -> Integer.compare(
            chunkPriority(unpackColumnX(left), unpackColumnZ(left)),
            chunkPriority(unpackColumnX(right), unpackColumnZ(right))
        ));

        long startNs = System.nanoTime();
        int integrated = 0;
        for (long key : readyColumnKeys) {
            if (integrated >= maxColumns) {
                break;
            }
            if (integrated > 0 && System.nanoTime() - startNs >= budgetNs) {
                break;
            }
            Future<ChunkColumn> future = pendingColumns.get(key);
            if (future == null || !future.isDone()) {
                continue;
            }
            try {
                ChunkColumn column = future.get();
                integrateColumn(column);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException exception) {
                if (GameConfig.ENABLE_DEBUG_LOGS) {
                    System.out.println("VoxelWorld: column task failed: " + exception.getMessage());
                }
            }
            pendingColumns.remove(key, future);
            integrated++;
        }
        readyColumnKeys.clear();
        return integrated;
    }

    private void ensureColumnLoadedSync(int chunkX, int chunkZ) {
        long key = columnKey(chunkX, chunkZ);
        ChunkColumn loaded = loadedColumns.get(key);
        if (loaded != null) {
            return;
        }
        if (networkMirrorMode) {
            return;
        }

        Future<ChunkColumn> future = pendingColumns.get(key);
        if (future != null) {
            try {
                integrateColumn(future.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException exception) {
                if (GameConfig.ENABLE_DEBUG_LOGS) {
                    System.out.println("VoxelWorld: failed to wait for chunk column: " + exception.getMessage());
                }
            }
            pendingColumns.remove(key, future);
            return;
        }

        integrateColumn(loadOrGenerateColumn(chunkX, chunkZ));
    }

    private ChunkColumn ensureColumnGeneratedSync(int chunkX, int chunkZ) {
        ensureColumnLoadedSync(chunkX, chunkZ);
        ChunkColumn column = loadedColumns.get(columnKey(chunkX, chunkZ));
        return column != null && column.generated ? column : null;
    }

    private boolean submitColumnTask(int chunkX, int chunkZ) {
        if (networkMirrorMode) {
            return false;
        }
        long key = columnKey(chunkX, chunkZ);
        if (loadedColumns.containsKey(key) || pendingColumns.containsKey(key)) {
            return false;
        }
        int priority = chunkPriority(chunkX, chunkZ);
        if (pendingColumns.size() >= MAX_PENDING_COLUMN_TASKS && priority > 3) {
            return false;
        }
        PrioritizedColumnTask task = new PrioritizedColumnTask(
            () -> loadOrGenerateColumn(chunkX, chunkZ),
            priority,
            nextColumnTaskSequence++
        );
        Future<ChunkColumn> previous = pendingColumns.putIfAbsent(key, task);
        if (previous != null) {
            return false;
        }
        generationExecutor.execute(task);
        return true;
    }

    UUID findRemotePlayerInReach(PlayerState player) {
        if (player == null) {
            return null;
        }
        double originX = player.x;
        double originY = player.y + player.eyeHeight();
        double originZ = player.z;
        double horizontalLength = Math.cos(player.pitch);
        double dirX = Math.cos(player.yaw) * horizontalLength;
        double dirY = Math.sin(player.pitch);
        double dirZ = Math.sin(player.yaw) * horizontalLength;
        double reach = 3.4;
        RemotePlayerState best = null;
        double bestProjection = Double.POSITIVE_INFINITY;
        for (RemotePlayerState remote : remotePlayers.values()) {
            if (remote.health <= 0.0 || remote.spectatorMode) {
                continue;
            }
            double centerX = remote.x;
            double centerY = remote.y + remote.height() * 0.55;
            double centerZ = remote.z;
            double toX = centerX - originX;
            double toY = centerY - originY;
            double toZ = centerZ - originZ;
            double projection = toX * dirX + toY * dirY + toZ * dirZ;
            if (projection < 0.0 || projection > reach || projection >= bestProjection) {
                continue;
            }
            double closestX = originX + dirX * projection;
            double closestY = originY + dirY * projection;
            double closestZ = originZ + dirZ * projection;
            double dx = centerX - closestX;
            double dy = centerY - closestY;
            double dz = centerZ - closestZ;
            double hitRadius = remote.radius() + 0.34;
            if (dx * dx + dy * dy + dz * dz <= hitRadius * hitRadius) {
                best = remote;
                bestProjection = projection;
            }
        }
        return best == null ? null : best.uuid;
    }

    private int chunkPriority(int chunkX, int chunkZ) {
        if (lastStreamingChunkX == Integer.MIN_VALUE || lastStreamingChunkZ == Integer.MIN_VALUE) {
            return 0;
        }
        int dx = Math.abs(chunkX - lastStreamingChunkX);
        int dz = Math.abs(chunkZ - lastStreamingChunkZ);
        return Math.max(dx, dz) * 256 + dx * dx + dz * dz;
    }

    private void cancelFarPendingColumns(int playerChunkX, int playerChunkZ, int keepRadius) {
        for (Map.Entry<Long, Future<ChunkColumn>> entry : pendingColumns.entrySet()) {
            int chunkX = unpackColumnX(entry.getKey());
            int chunkZ = unpackColumnZ(entry.getKey());
            int horizontalDistance = Math.max(Math.abs(chunkX - playerChunkX), Math.abs(chunkZ - playerChunkZ));
            if (horizontalDistance <= keepRadius) {
                continue;
            }
            Future<ChunkColumn> future = entry.getValue();
            if (pendingColumns.remove(entry.getKey(), future)) {
                future.cancel(true);
            }
        }
        generationExecutor.purge();
    }

    private void integrateColumn(ChunkColumn column) {
        if (column == null) {
            return;
        }
        column.generated = true;
        ChunkColumn previous = loadedColumns.putIfAbsent(columnKey(column.chunkX, column.chunkZ), column);
        if (previous == null) {
            registerColumnDynamicBlocks(column);
            markColumnAndLoadedNeighborsDirty(column.chunkX, column.chunkZ);
        }
    }

    private void markColumnAndLoadedNeighborsDirty(int chunkX, int chunkZ) {
        markLoadedNeighborColumnDirty(chunkX - 1, chunkZ);
        markLoadedNeighborColumnDirty(chunkX + 1, chunkZ);
        markLoadedNeighborColumnDirty(chunkX, chunkZ - 1);
        markLoadedNeighborColumnDirty(chunkX, chunkZ + 1);
    }

    private void markLoadedNeighborColumnDirty(int chunkX, int chunkZ) {
        if (!loadedColumns.containsKey(columnKey(chunkX, chunkZ))) {
            return;
        }
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            markPendingMeshDirtySection(chunkX, chunkY, chunkZ);
        }
    }

    private void markPendingMeshDirtySection(int chunkX, int chunkY, int chunkZ) {
        if (chunkY < 0 || chunkY >= GameConfig.WORLD_CHUNKS_Y) {
            return;
        }
        pendingMeshDirtySections.add(chunkSectionKey(chunkX, chunkY, chunkZ));
    }

    private void drainPendingMeshDirtySections() {
        if (pendingMeshDirtySections.isEmpty()) {
            return;
        }
        for (long sectionKey : pendingMeshDirtySections) {
            markDirtySection(unpackChunkSectionX(sectionKey), unpackChunkSectionY(sectionKey), unpackChunkSectionZ(sectionKey));
        }
        pendingMeshDirtySections.clear();
    }

    private ChunkColumn loadOrGenerateColumn(int chunkX, int chunkZ) {
        if (networkMirrorMode) {
            return null;
        }
        RegionStorage storage = storageForColumn(chunkX, chunkZ);
        if (storage != null) {
            ChunkColumn loaded = storage.loadColumn(chunkX, chunkZ);
            if (loaded != null) {
                return loaded;
            }
        }
        if (saveDirectory != null) {
            Path chunkPath = chunkFilePath(chunkX, chunkZ);
            if (Files.isRegularFile(chunkPath)) {
                ChunkColumn loaded = loadColumnFromDisk(chunkPath, chunkX, chunkZ);
                if (loaded != null) {
                    return loaded;
                }
            }
        }
        ChunkColumn generated = generateColumn(chunkX, chunkZ);
        return generated;
    }

    private ChunkColumn loadColumnFromDisk(Path chunkPath, int chunkX, int chunkZ) {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(chunkPath)))) {
            int version = input.readInt();
            if (version != SAVE_FORMAT_VERSION && version != LEGACY_BYTE_BLOCK_SAVE_FORMAT_VERSION) {
                return null;
            }
            int storedChunkX = input.readInt();
            int storedChunkZ = input.readInt();
            if (storedChunkX != chunkX || storedChunkZ != chunkZ) {
                return null;
            }

            ChunkColumn column = new ChunkColumn(chunkX, chunkZ);
            int sectionMask = input.readInt();
            for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
                if ((sectionMask & (1 << chunkY)) == 0) {
                    continue;
                }
                Chunk chunk = column.section(chunkY);
                int index = 0;
                while (index < Chunk.VOLUME) {
                    int runLength = input.readUnsignedShort();
                    BlockState state;
                    if (version >= SAVE_FORMAT_VERSION) {
                        state = Blocks.stateFromNamespacedId(input.readUTF());
                    } else {
                        state = Blocks.stateFromLegacyId(input.readByte());
                    }
                    int distance = input.readByte();
                    for (int run = 0; run < runLength && index < Chunk.VOLUME; run++, index++) {
                        chunk.setSerializedCellState(index, state, distance);
                    }
                }
            }
            recalculateColumnSurfaceHeights(column);
            column.status = ChunkGenerationStatus.FULL;
            column.dirty = false;
            return column;
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to load chunk " + chunkPath + ": " + exception.getMessage());
            }
            return null;
        }
    }

    private ChunkColumn generateColumn(int chunkX, int chunkZ) {
        boolean profile = GameConfig.ENABLE_FRAME_PROFILING;
        long startNs = profile ? System.nanoTime() : 0L;
        if (worldGenerator == null) {
            worldGenerator = new WorldGenerator(seed, terrainPreset);
        }
        GeneratedChunkColumn generated = worldGenerator.generateChunk(chunkX, chunkZ);
        ChunkColumn column = new ChunkColumn(chunkX, chunkZ, false);

        Chunk[] generatedSections = generated.sections();
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            column.sections[chunkY] = generatedSections[chunkY];
        }
        column.status = generated.status();

        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                column.setSurfaceHeightLocal(localX, localZ, generated.getSurfaceHeight(localX, localZ));
            }
        }
        column.dirty = false;
        column.naturalTerrain = true;
        if (profile) {
            logGenerationProfile(System.nanoTime() - startNs);
        }
        return column;
    }

    private RegionStorage storageForColumn(int chunkX, int chunkZ) {
        if (GameConfig.isParadiseDimension(activeDimensionId) && paradiseRegionStorage != null) {
            return paradiseRegionStorage;
        }
        return regionStorage;
    }

    private void logStreamingProfile(
        int submittedColumns,
        int integratedColumns,
        long prepareNs,
        long drainNs,
        long ensureNs,
        long unloadNs
    ) {
        streamingProfileFrames++;
        streamingProfileSubmittedColumns += submittedColumns;
        streamingProfileIntegratedColumns += integratedColumns;
        streamingProfilePrepareNs += prepareNs;
        streamingProfileDrainNs += drainNs;
        streamingProfileEnsureNs += ensureNs;
        streamingProfileUnloadNs += unloadNs;
        long nowNs = System.nanoTime();
        if (streamingProfileLastLogNs == 0L) {
            streamingProfileLastLogNs = nowNs;
            return;
        }
        long intervalNs = nowNs - streamingProfileLastLogNs;
        if (intervalNs < (long) (GameConfig.FRAME_PROFILE_LOG_INTERVAL_SECONDS * 1_000_000_000.0)) {
            return;
        }

        double frames = Math.max(1, streamingProfileFrames);
        double generationAverageMs = generationProfileColumns == 0
            ? 0.0
            : generationProfileNs / 1_000_000.0 / generationProfileColumns;
        System.out.printf(
            "VoxelWorld profile: frames=%d loaded=%d pending=%d submitted=%d integrated=%d prepare=%.3fms drain=%.3fms ensure=%.3fms unload=%.3fms genColumns=%d genAvg=%.3fms%n",
            streamingProfileFrames,
            loadedColumns.size(),
            pendingColumns.size(),
            streamingProfileSubmittedColumns,
            streamingProfileIntegratedColumns,
            streamingProfilePrepareNs / 1_000_000.0 / frames,
            streamingProfileDrainNs / 1_000_000.0 / frames,
            streamingProfileEnsureNs / 1_000_000.0 / frames,
            streamingProfileUnloadNs / 1_000_000.0 / frames,
            generationProfileColumns,
            generationAverageMs
        );
        streamingProfileLastLogNs = nowNs;
        streamingProfileFrames = 0;
        streamingProfileSubmittedColumns = 0;
        streamingProfileIntegratedColumns = 0;
        streamingProfilePrepareNs = 0L;
        streamingProfileDrainNs = 0L;
        streamingProfileEnsureNs = 0L;
        streamingProfileUnloadNs = 0L;
        generationProfileColumns = 0;
        generationProfileNs = 0L;
    }

    private void logGenerationProfile(long elapsedNs) {
        generationProfileColumns++;
        generationProfileNs += elapsedNs;
    }

    private void recalculateColumnSurfaceHeights(ChunkColumn column) {
        int startX = column.chunkX * GameConfig.CHUNK_SIZE;
        int startZ = column.chunkZ * GameConfig.CHUNK_SIZE;
        for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                column.setSurfaceHeightLocal(localX, localZ, findTopSolidBlock(worldX, worldZ));
            }
        }
    }

    private void registerColumnDynamicBlocks(ChunkColumn column) {
        int startX = column.chunkX * GameConfig.CHUNK_SIZE;
        int startZ = column.chunkZ * GameConfig.CHUNK_SIZE;
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            Chunk chunk = column.section(chunkY);
            int startY = GameConfig.sectionYForIndex(chunkY);
            for (int localY = 0; localY < GameConfig.CHUNK_SIZE; localY++) {
                for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                    for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
                        int worldX = startX + localX;
                        int worldY = startY + localY;
                        int worldZ = startZ + localZ;
                        if (column.naturalTerrain
                            && isFallingTerrainBlock(chunk.getBlockLocal(localX, localY, localZ))
                            && isStableGeneratedSand(worldX, worldY, worldZ)) {
                            continue;
                        }
                        updateActiveStateAt(worldX, worldY, worldZ);
                    }
                }
            }
        }
    }

    private void removeColumnDynamicBlocks(ChunkColumn column) {
        int startX = column.chunkX * GameConfig.CHUNK_SIZE;
        int startZ = column.chunkZ * GameConfig.CHUNK_SIZE;
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            int startY = GameConfig.sectionYForIndex(chunkY);
            for (int localY = 0; localY < GameConfig.CHUNK_SIZE; localY++) {
                for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                    for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
                        long blockKey = packBlock(startX + localX, startY + localY, startZ + localZ);
                        activeWaterCells.remove(blockKey);
                        activeLavaCells.remove(blockKey);
                        activeSandCells.remove(blockKey);
                        queuedWaterCells.remove(blockKey);
                        queuedLavaCells.remove(blockKey);
                        queuedSandCells.remove(blockKey);
                        waterFlowCache.remove(blockKey);
                    }
                }
            }
        }
    }

    private void clearDynamicBlockQueues() {
        queuedWaterCells.clear();
        queuedLavaCells.clear();
        queuedSandCells.clear();
        activeWaterQueue.clear();
        activeLavaQueue.clear();
        activeSandQueue.clear();
    }

    private void updateActiveStateAt(int x, int y, int z) {
        if (!isInside(x, y, z)) {
            return;
        }

        long blockKey = packBlock(x, y, z);
        byte block = getBlock(x, y, z);
        if (GameConfig.isWaterBlock(block) && isFluidActiveCandidate(x, y, z, GameConfig.WATER)) {
            activeWaterCells.add(blockKey);
            enqueueDynamicCell(activeWaterCells, queuedWaterCells, activeWaterQueue, blockKey);
        } else {
            activeWaterCells.remove(blockKey);
            queuedWaterCells.remove(blockKey);
        }

        if (GameConfig.isLavaBlock(block) && isFluidActiveCandidate(x, y, z, GameConfig.LAVA)) {
            activeLavaCells.add(blockKey);
            enqueueDynamicCell(activeLavaCells, queuedLavaCells, activeLavaQueue, blockKey);
        } else {
            activeLavaCells.remove(blockKey);
            queuedLavaCells.remove(blockKey);
        }

        if (isFallingTerrainBlock(block) && isSandActiveCandidate(x, y, z)) {
            activeSandCells.add(blockKey);
            enqueueDynamicCell(activeSandCells, queuedSandCells, activeSandQueue, blockKey);
        } else {
            activeSandCells.remove(blockKey);
            queuedSandCells.remove(blockKey);
        }
    }

    private void enqueueDynamicCell(HashSet<Long> activeCells, HashSet<Long> queuedCells, ArrayDeque<Long> queue, long blockKey) {
        if (activeCells.contains(blockKey) && queuedCells.add(blockKey)) {
            queue.addLast(blockKey);
        }
    }

    private void refillDynamicQueue(HashSet<Long> activeCells, HashSet<Long> queuedCells, ArrayDeque<Long> queue, int limit) {
        int queued = 0;
        for (long blockKey : activeCells) {
            if (queuedCells.add(blockKey)) {
                queue.addLast(blockKey);
                queued++;
                if (queued >= limit) {
                    return;
                }
            }
        }
    }

    private boolean isFallingTerrainBlock(byte block) {
        return Blocks.isGravityAffected(block);
    }

    private boolean isFluidActiveCandidate(int x, int y, int z, byte fluidItem) {
        byte block = getBlock(x, y, z);
        if (block == GameConfig.SEAGRASS) {
            return false;
        }
        if (GameConfig.fluidItemForBlock(block) != fluidItem) {
            return false;
        }
        if (GameConfig.isFluidSourceBlock(block) && getFluidDistance(x, y, z) == GameConfig.NATURAL_FLUID_DISTANCE) {
            return (y > GameConfig.WORLD_MIN_Y && canFluidOccupyBlock(getBlock(x, y - 1, z), fluidItem))
                || hasLoadedNaturalFluidExpansionOpportunity(x, y, z, fluidItem);
        }
        if (GameConfig.isFluidFlowingBlock(block)) {
            int distance = getFluidDistance(x, y, z);
            if (distance < 0) {
                return true;
            }
            if (!hasSimpleFluidSupport(x, y, z, fluidItem, distance)) {
                return true;
            }
            return hasSimpleFluidExpansionOpportunity(x, y, z, fluidItem, distance);
        }
        if (fluidItem == GameConfig.WATER && isStableNaturalWaterSource(x, y, z)) {
            return hasFluidExpansionOpportunity(x, y, z);
        }
        return hasSimpleFluidExpansionOpportunity(x, y, z, fluidItem, 0);
    }

    private boolean hasFluidExpansionOpportunity(int x, int y, int z) {
        return (y > GameConfig.WORLD_MIN_Y && isReplaceableForFluid(getBlock(x, y - 1, z)))
            || isReplaceableForFluid(getBlock(x + 1, y, z))
            || isReplaceableForFluid(getBlock(x - 1, y, z))
            || isReplaceableForFluid(getBlock(x, y, z + 1))
            || isReplaceableForFluid(getBlock(x, y, z - 1));
    }

    private boolean hasLoadedNaturalFluidExpansionOpportunity(int x, int y, int z, byte fluidItem) {
        if (!hasFluidHorizontalFloor(x, y, z, fluidItem)) {
            return false;
        }
        return canIntroduceLoadedFluid(x + 1, y, z, fluidItem, 1)
            || canIntroduceLoadedFluid(x - 1, y, z, fluidItem, 1)
            || canIntroduceLoadedFluid(x, y, z + 1, fluidItem, 1)
            || canIntroduceLoadedFluid(x, y, z - 1, fluidItem, 1);
    }

    private boolean canIntroduceLoadedFluid(int x, int y, int z, byte fluidItem, int distance) {
        return isBlockLoaded(x, y, z) && canIntroduceSimpleFluid(x, y, z, fluidItem, distance);
    }

    private boolean hasSimpleFluidExpansionOpportunity(int x, int y, int z, byte fluidItem, int currentDistance) {
        if (y > GameConfig.WORLD_MIN_Y && canIntroduceSimpleFluid(x, y - 1, z, fluidItem, 0)) {
            return true;
        }
        if (!hasFluidHorizontalFloor(x, y, z, fluidItem)) {
            return false;
        }
        int nextDistance = currentDistance + 1;
        if (nextDistance > GameConfig.fluidSpreadDistance(fluidItem)) {
            return false;
        }
        return canIntroduceSimpleFluid(x + 1, y, z, fluidItem, nextDistance)
            || canIntroduceSimpleFluid(x - 1, y, z, fluidItem, nextDistance)
            || canIntroduceSimpleFluid(x, y, z + 1, fluidItem, nextDistance)
            || canIntroduceSimpleFluid(x, y, z - 1, fluidItem, nextDistance);
    }

    private boolean isStableNaturalWaterSource(int x, int y, int z) {
        if (getBlock(x, y, z) != GameConfig.WATER_SOURCE || y > GameConfig.SEA_LEVEL) {
            return false;
        }
        if (getFluidDistance(x, y, z) == GameConfig.NATURAL_FLUID_DISTANCE) {
            return true;
        }
        byte below = getBlock(x, y - 1, z);
        if (isReplaceableForFluid(below) || GameConfig.isLavaBlock(below)) {
            return false;
        }
        return GameConfig.isWaterBlock(below)
            || GameConfig.isWaterBlock(getBlock(x, y + 1, z))
            || countCurrentAdjacentWaterSources(x, y, z) >= 1;
    }

    private boolean isSandActiveCandidate(int x, int y, int z) {
        return y > GameConfig.WORLD_MIN_Y && isReplaceableForFallingBlock(getBlock(x, y - 1, z));
    }

    private boolean isStableGeneratedSand(int x, int y, int z) {
        return y <= GameConfig.SEA_LEVEL
            && (GameConfig.isWaterBlock(getBlock(x, y + 1, z))
                || GameConfig.isWaterBlock(getBlock(x, y + 2, z)));
    }

    private void refreshDynamicCellsAround(int x, int y, int z) {
        invalidateFluidFlowCacheAround(x, y, z);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                    updateActiveStateAt(x + offsetX, y + offsetY, z + offsetZ);
                }
            }
        }
    }

    private void setBlockState(int x, int y, int z, byte block, int fluidDistance) {
        ChunkColumn column = ensureLoadedColumnForBlock(x, z);
        if (column == null) {
            return;
        }
        Chunk chunk = column.section(GameConfig.sectionIndexForY(y));
        if (chunk == null) {
            return;
        }

        int localX = localBlockCoordinate(x);
        int localY = GameConfig.localYForWorldY(y);
        int localZ = localBlockCoordinate(z);
        byte previous = chunk.getBlockLocal(localX, localY, localZ);
        int previousDistance = chunk.getFluidDistanceLocal(localX, localY, localZ);
        if (previous == block && (!GameConfig.isLiquidBlock(block) || previousDistance == fluidDistance)) {
            return;
        }

        chunk.setBlockLocal(localX, localY, localZ, block);
        if (GameConfig.isLiquidBlock(block)) {
            chunk.setFluidDistanceLocal(localX, localY, localZ, Math.max(0, fluidDistance));
        }
        column.dirty = true;
        invalidateFluidFlowCacheAround(x, y, z);
        markDirtyBlock(x, y, z);
    }

    void setBlockState(int x, int y, int z, BlockState state) {
        BlockState normalized = state == null ? Blocks.stateFromLegacyId(GameConfig.AIR) : state;
        int fluidDistance = normalized.type.isLiquid() ? 0 : -1;
        ChunkColumn column = ensureLoadedColumnForBlock(x, z);
        if (column == null) {
            return;
        }
        Chunk chunk = column.section(GameConfig.sectionIndexForY(y));
        if (chunk == null) {
            return;
        }
        int localX = localBlockCoordinate(x);
        int localY = GameConfig.localYForWorldY(y);
        int localZ = localBlockCoordinate(z);
        BlockState previous = chunk.getBlockStateLocal(localX, localY, localZ);
        if (previous.type.numericId == normalized.type.numericId && previous.data == normalized.data) {
            return;
        }
        chunk.setBlockStateLocal(localX, localY, localZ, normalized);
        if (normalized.type.isLiquid()) {
            chunk.setFluidDistanceLocal(localX, localY, localZ, Math.max(0, fluidDistance));
        }
        column.dirty = true;
        invalidateFluidFlowCacheAround(x, y, z);
        markDirtyBlock(x, y, z);
    }

    private void invalidateFluidFlowCacheAround(int x, int y, int z) {
        for (int offsetX = -2; offsetX <= 2; offsetX++) {
            for (int offsetY = -2; offsetY <= 2; offsetY++) {
                for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                    waterFlowCache.remove(packBlock(x + offsetX, y + offsetY, z + offsetZ));
                }
            }
        }
    }

    private ChunkColumn ensureLoadedColumnForBlock(int x, int z) {
        int chunkX = worldToChunk(x);
        int chunkZ = worldToChunk(z);
        ensureColumnLoadedSync(chunkX, chunkZ);
        return loadedColumns.get(columnKey(chunkX, chunkZ));
    }

    private void updatePlantSupportAt(int x, int y, int z) {
        if (!isInside(x, y, z)) {
            return;
        }
        byte block = getBlock(x, y, z);
        if (!isPlantBlock(block) || canPlantStay(x, y, z)) {
            return;
        }
        setBlockState(x, y, z, GameConfig.AIR, -1);
        refreshDynamicCellsAround(x, y, z);
    }

    private void updateSnowSupportAt(int x, int y, int z) {
        if (!isInside(x, y, z) || getBlock(x, y, z) != GameConfig.SNOW_LAYER || canSnowStay(x, y, z)) {
            return;
        }
        setBlockState(x, y, z, GameConfig.AIR, -1);
        refreshDynamicCellsAround(x, y, z);
        markDirtyBlock(x, y, z);
    }

    private boolean canSnowStay(int x, int y, int z) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y) {
            return false;
        }
        byte supportBlock = getBlock(x, y - 1, z);
        return supportBlock == GameConfig.GRASS
            || supportBlock == GameConfig.DIRT
            || supportBlock == GameConfig.COBBLESTONE
            || supportBlock == GameConfig.STONE
            || supportBlock == GameConfig.SNOW_BLOCK
            || supportBlock == GameConfig.DEEPSLATE
            || supportBlock == GameConfig.SAND
            || supportBlock == GameConfig.GRAVEL;
    }

    private boolean canPlantStay(int x, int y, int z) {
        if (!isInside(x, y, z) || y <= GameConfig.WORLD_MIN_Y) {
            return false;
        }
        byte plant = getBlock(x, y, z);
        if (isCropBlock(plant)) {
            return getBlock(x, y - 1, z) == GameConfig.FARMLAND;
        }
        if (plant == GameConfig.SEAGRASS || plant == GameConfig.KELP) {
            byte support = getBlock(x, y - 1, z);
            return GameConfig.isWaterBlock(getBlock(x, y + 1, z))
                && (support == GameConfig.SAND
                    || support == GameConfig.GRAVEL
                    || support == GameConfig.CLAY
                    || support == GameConfig.DIRT
                    || support == GameConfig.KELP);
        }
        byte supportBlock = getBlock(x, y - 1, z);
        return Blocks.isSolid(supportBlock)
            && (supportBlock == GameConfig.GRASS
                || supportBlock == GameConfig.DIRT
                || supportBlock == GameConfig.SAND
                || supportBlock == GameConfig.FARMLAND
                || supportBlock == InventoryItems.OAK_PLANKS
                || supportBlock == GameConfig.PINE_PLANKS
                || supportBlock == GameConfig.BIRCH_PLANKS
                || supportBlock == GameConfig.COBBLESTONE
                || supportBlock == GameConfig.STONE
                || supportBlock == GameConfig.DEEPSLATE
                || supportBlock == GameConfig.GRAVEL);
    }

    private void refreshSurfaceHeight(int x, int z) {
        ChunkColumn column = loadedColumns.get(columnKey(worldToChunk(x), worldToChunk(z)));
        if (column == null) {
            return;
        }
        column.setSurfaceHeightLocal(localBlockCoordinate(x), localBlockCoordinate(z), findTopSolidBlock(x, z));
    }

    private int findTopSolidBlock(int x, int z) {
        for (int y = GameConfig.WORLD_MAX_Y; y >= GameConfig.WORLD_MIN_Y; y--) {
            if (blocksSky(getBlock(x, y, z))) {
                return y;
            }
        }
        return GameConfig.WORLD_MIN_Y;
    }

    private boolean blocksSky(byte block) {
        return Blocks.isSolid(block)
            && block != GameConfig.OAK_LEAVES
            && block != GameConfig.PINE_LEAVES
            && block != GameConfig.BIRCH_LEAVES;
    }

    private boolean isReplaceableForFluid(byte block) {
        return Blocks.isReplaceable(block) || block == GameConfig.CACTUS;
    }

    private boolean isReplaceableForFallingBlock(byte block) {
        return Blocks.isReplaceable(block) || Blocks.isLiquid(block);
    }

    private byte getFluidReactionBlock(byte waterBlock, byte lavaBlock) {
        boolean waterSource = waterBlock == GameConfig.WATER_SOURCE;
        boolean lavaSource = lavaBlock == GameConfig.LAVA_SOURCE;
        if (waterSource && lavaSource) {
            return GameConfig.OBSIDIAN;
        }
        if (waterSource) {
            return GameConfig.COBBLESTONE;
        }
        if (lavaSource) {
            return GameConfig.STONE;
        }
        return GameConfig.COBBLESTONE;
    }

    private boolean isChunkWithinFluidSimulationDistance(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        return Math.abs(chunkX - playerChunkX) <= GameConfig.FLUID_SIMULATION_CHUNK_DISTANCE
            && Math.abs(chunkZ - playerChunkZ) <= GameConfig.FLUID_SIMULATION_CHUNK_DISTANCE;
    }

    private boolean shouldTickDynamicCell(long blockKey, int x, int z, int playerChunkX, int playerChunkZ) {
        int distance = Math.max(Math.abs(worldToChunk(x) - playerChunkX), Math.abs(worldToChunk(z) - playerChunkZ));
        int interval = distance <= 1 ? 1 : (distance == 2 ? 2 : 4);
        if (interval <= 1) {
            return true;
        }
        long phase = worldTickCounter + blockKey;
        return Math.floorMod(phase, interval) == 0;
    }

    private int getStreamingRadius(PlayerState player) {
        int renderRadius = player != null && player.spectatorMode
            ? Math.max(renderDistanceChunks, GameConfig.SPECTATOR_CHUNK_RENDER_DISTANCE)
            : renderDistanceChunks;
        return Math.max(renderRadius + GameConfig.RENDER_CHUNK_UNLOAD_PADDING + 2, GameConfig.ACTIVE_SIMULATION_CHUNK_DISTANCE + 2);
    }

    private void markDirtyBlock(int x, int y, int z) {
        if (!GameConfig.isWorldYInside(y)) {
            return;
        }
        networkDirtyBlocks.add(x, y, z);
        int chunkX = worldToChunk(x);
        int chunkY = GameConfig.sectionIndexForY(y);
        int chunkZ = worldToChunk(z);
        markDirtySection(chunkX, chunkY, chunkZ);
        markDirtySection(chunkX + 1, chunkY, chunkZ);
        markDirtySection(chunkX - 1, chunkY, chunkZ);
        markDirtySection(chunkX, chunkY, chunkZ + 1);
        markDirtySection(chunkX, chunkY, chunkZ - 1);
        markDirtySection(chunkX, chunkY + 1, chunkZ);
        markDirtySection(chunkX, chunkY - 1, chunkZ);

        int localX = localBlockCoordinate(x);
        int localY = GameConfig.localYForWorldY(y);
        int localZ = localBlockCoordinate(z);
        if (localX == 0) {
            markDirtySection(chunkX - 1, chunkY, chunkZ);
        } else if (localX == GameConfig.CHUNK_SIZE - 1) {
            markDirtySection(chunkX + 1, chunkY, chunkZ);
        }
        if (localZ == 0) {
            markDirtySection(chunkX, chunkY, chunkZ - 1);
        } else if (localZ == GameConfig.CHUNK_SIZE - 1) {
            markDirtySection(chunkX, chunkY, chunkZ + 1);
        }
        if (localY == 0) {
            markDirtySection(chunkX, chunkY - 1, chunkZ);
        } else if (localY == GameConfig.CHUNK_SIZE - 1) {
            markDirtySection(chunkX, chunkY + 1, chunkZ);
        }
    }

    private void markDirtySection(int chunkX, int chunkY, int chunkZ) {
        if (chunkY < 0 || chunkY >= GameConfig.WORLD_CHUNKS_Y) {
            return;
        }
        long dirtyKey = chunkSectionKey(chunkX, chunkY, chunkZ);
        if (dirtyChunkSections.add(dirtyKey)) {
            int blockX = chunkX * GameConfig.CHUNK_SIZE + GameConfig.CHUNK_SIZE / 2;
            int blockY = GameConfig.sectionYForIndex(chunkY) + GameConfig.CHUNK_SIZE / 2;
            int blockZ = chunkZ * GameConfig.CHUNK_SIZE + GameConfig.CHUNK_SIZE / 2;
            simulationDirtyColumns.add(blockX, blockY, blockZ);
        }
    }

    private long chunkSectionKey(int chunkX, int chunkY, int chunkZ) {
        long packedX = ((long) chunkX) & 0x3FFFFFFL;
        long packedZ = ((long) chunkZ) & 0x3FFFFFFL;
        long packedY = chunkY & 0xFFFL;
        return (packedX << 38) | (packedZ << 12) | packedY;
    }

    private int unpackChunkSectionX(long sectionKey) {
        int value = (int) ((sectionKey >>> 38) & 0x3FFFFFFL);
        return value >= 0x2000000 ? value - 0x4000000 : value;
    }

    private int unpackChunkSectionY(long sectionKey) {
        return (int) (sectionKey & 0xFFFL);
    }

    private int unpackChunkSectionZ(long sectionKey) {
        int value = (int) ((sectionKey >>> 12) & 0x3FFFFFFL);
        return value >= 0x2000000 ? value - 0x4000000 : value;
    }

    private void updateDroppedItemPhysics(DroppedItem droppedItem, double deltaTime) {
        boolean inWater = intersectsFluid(
            droppedItem.x, droppedItem.y, droppedItem.z,
            droppedItem.radius(), droppedItem.height(), GameConfig.WATER
        );

        if (inWater) {
            sampleFluidFlow(
                droppedItem.x, droppedItem.y, droppedItem.z,
                droppedItem.radius(), droppedItem.height(), GameConfig.WATER, sampledFluidFlow
            );
            droppedItem.velocityX += sampledFluidFlow.x * GameConfig.WATER_ENTITY_FLOW_PUSH * deltaTime;
            droppedItem.velocityZ += sampledFluidFlow.z * GameConfig.WATER_ENTITY_FLOW_PUSH * deltaTime;
            droppedItem.verticalVelocity += sampledFluidFlow.y * GameConfig.WATER_VERTICAL_FLOW_PUSH * deltaTime;
            droppedItem.verticalVelocity -= GameConfig.DROPPED_ITEM_GRAVITY * 0.25 * deltaTime;
        } else {
            droppedItem.verticalVelocity -= GameConfig.DROPPED_ITEM_GRAVITY * deltaTime;
        }

        droppedItem.verticalVelocity = Math.max(-GameConfig.TERMINAL_VELOCITY * 0.5, droppedItem.verticalVelocity);
        moveDroppedItemHorizontal(droppedItem, droppedItem.velocityX * deltaTime, droppedItem.velocityZ * deltaTime);
        moveDroppedItemVertical(droppedItem, droppedItem.verticalVelocity * deltaTime);

        double drag = inWater ? GameConfig.DROPPED_ITEM_WATER_DRAG : GameConfig.DROPPED_ITEM_DRAG;
        droppedItem.velocityX *= drag;
        droppedItem.velocityZ *= drag;
        if (droppedItem.isGrounded) {
            droppedItem.velocityX *= 0.72;
            droppedItem.velocityZ *= 0.72;
        }
    }

    private void moveDroppedItemHorizontal(DroppedItem droppedItem, double moveX, double moveZ) {
        if (Math.abs(moveX) > 1.0e-5) {
            double nextX = droppedItem.x + moveX;
            if (!collides(nextX, droppedItem.y, droppedItem.z, droppedItem.radius(), droppedItem.height())) {
                droppedItem.x = nextX;
            } else {
                droppedItem.velocityX = 0.0;
            }
        }
        if (Math.abs(moveZ) > 1.0e-5) {
            double nextZ = droppedItem.z + moveZ;
            if (!collides(droppedItem.x, droppedItem.y, nextZ, droppedItem.radius(), droppedItem.height())) {
                droppedItem.z = nextZ;
            } else {
                droppedItem.velocityZ = 0.0;
            }
        }
    }

    private void moveDroppedItemVertical(DroppedItem droppedItem, double moveY) {
        if (Math.abs(moveY) < 1.0e-5) {
            return;
        }

        double nextY = droppedItem.y + moveY;
        if (!collides(droppedItem.x, nextY, droppedItem.z, droppedItem.radius(), droppedItem.height())) {
            droppedItem.y = nextY;
            droppedItem.isGrounded = false;
            return;
        }

        if (moveY < 0.0) {
            droppedItem.isGrounded = true;
            droppedItem.verticalVelocity = -droppedItem.verticalVelocity * GameConfig.DROPPED_ITEM_BOUNCE;
            if (Math.abs(droppedItem.verticalVelocity) < 0.18) {
                droppedItem.verticalVelocity = 0.0;
            }
        } else {
            droppedItem.verticalVelocity = 0.0;
        }
    }

    private void saveColumnIfDirty(ChunkColumn column) {
        if (column == null || !column.dirty) {
            return;
        }

        RegionStorage storage = storageForColumn(column.chunkX, column.chunkZ);
        if (storage != null) {
            backgroundSaveService.enqueue(storage, snapshotColumnForSave(column));
            column.dirty = false;
            return;
        }

        if (saveDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(saveDirectory);
            Path chunkPath = chunkFilePath(column.chunkX, column.chunkZ);
            int sectionMask = 0;
            for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
                Chunk chunk = column.section(chunkY);
                if (chunk != null && !chunk.isEmpty()) {
                    sectionMask |= 1 << chunkY;
                }
            }
            if (sectionMask == 0) {
                Files.deleteIfExists(chunkPath);
                column.dirty = false;
                return;
            }
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(chunkPath)))) {
                output.writeInt(SAVE_FORMAT_VERSION);
                output.writeInt(column.chunkX);
                output.writeInt(column.chunkZ);
                output.writeInt(sectionMask);
                for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
                    Chunk chunk = column.section(chunkY);
                    if (chunk == null || chunk.isEmpty()) {
                        continue;
                    }
                    int index = 0;
                    while (index < Chunk.VOLUME) {
                        BlockState state = chunk.getBlockStateAtIndex(index);
                        int distance = chunk.getFluidDistanceAtIndex(index);
                        int runLength = 1;
                        while (index + runLength < Chunk.VOLUME && runLength < 0xFFFF
                            && sameBlockState(chunk.getBlockStateAtIndex(index + runLength), state)
                            && chunk.getFluidDistanceAtIndex(index + runLength) == distance) {
                            runLength++;
                        }
                        output.writeShort(runLength);
                        output.writeUTF(Blocks.serializedId(state));
                        output.writeByte(distance);
                        index += runLength;
                    }
                }
            }
            column.dirty = false;
        } catch (IOException exception) {
            if (GameConfig.ENABLE_DEBUG_LOGS) {
                System.out.println("VoxelWorld: failed to save column " + column.chunkX + "," + column.chunkZ + ": " + exception.getMessage());
            }
        }
    }

    private void flushLoadedColumns() {
        drainGeneratedColumns();
        for (ChunkColumn column : loadedColumns.values()) {
            saveColumnIfDirty(column);
        }
    }

    private ChunkColumn snapshotColumnForSave(ChunkColumn source) {
        ChunkColumn copy = new ChunkColumn(source.chunkX, source.chunkZ, false);
        copy.status = source.status;
        copy.generated = source.generated;
        copy.naturalTerrain = source.naturalTerrain;
        copy.dirty = false;
        System.arraycopy(source.surfaceHeights, 0, copy.surfaceHeights, 0, source.surfaceHeights.length);
        for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
            Chunk chunk = source.section(chunkY);
            if (chunk == null || chunk.isEmpty()) {
                continue;
            }
            ChunkSectionSnapshot snapshot = chunk.snapshot();
            Chunk copyChunk = new Chunk(source.chunkX, chunkY, source.chunkZ);
            int index = 0;
            for (int localY = 0; localY < GameConfig.CHUNK_SIZE; localY++) {
                for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                    for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
                        copyChunk.setSerializedCellState(
                            index++,
                            snapshot.getBlockStateLocal(localX, localY, localZ),
                            snapshot.getFluidDistanceLocal(localX, localY, localZ)
                        );
                    }
                }
            }
            copy.sections[chunkY] = copyChunk;
        }
        return copy;
    }

    private boolean sameBlockState(BlockState left, BlockState right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.type.numericId == right.type.numericId && left.data == right.data;
    }

    private Path chunkFilePath(int chunkX, int chunkZ) {
        return saveDirectory.resolve("c." + chunkX + "." + chunkZ + ".bin");
    }

    private Chunk getChunkForBlock(int x, int y, int z) {
        if (!isInside(x, y, z)) {
            return null;
        }
        ChunkColumn column = loadedColumns.get(columnKey(worldToChunk(x), worldToChunk(z)));
        if (column == null) {
            return null;
        }
        return column.section(GameConfig.sectionIndexForY(y));
    }

    private double biomeTemperature(int x, int z) {
        return octaveNoise((x - 380.0) * 0.0036, (z + 120.0) * 0.0036, 3, 0.55);
    }

    private double biomeHumidity(int x, int z) {
        return octaveNoise((x + 540.0) * 0.0042, (z - 260.0) * 0.0042, 3, 0.58);
    }

    private int calculateTerrainHeight(int x, int z) {
        double continent = octaveNoise(x * 0.0035, z * 0.0035, 4, 0.53);
        double mountainMask = Math.max(0.0, octaveNoise((x + 460.0) * 0.0056, (z - 700.0) * 0.0056, 3, 0.56));
        double broadHills = octaveNoise(x * 0.012, z * 0.012, 4, 0.52);
        double detail = octaveNoise((x + 200.0) * 0.042, (z - 300.0) * 0.042, 3, 0.48);
        double ridges = Math.abs(octaveNoise((x - 600.0) * 0.020, (z + 450.0) * 0.020, 3, 0.58));
        double canyonField = octaveNoise((x + 900.0) * 0.0072, (z - 410.0) * 0.0072, 2, 0.60);
        double temperature = biomeTemperature(x, z);
        double humidity = biomeHumidity(x, z);
        double mountainBoost = ridges * ridges * (8.0 + mountainMask * 22.0);
        double biomeLift = temperature < -0.10 ? 4.5 : (humidity > 0.40 ? -2.0 : 0.0);
        double baseHeight = GameConfig.SEA_LEVEL
            + continent * 18.0
            + broadHills * 10.0
            + detail * 4.0
            + mountainBoost
            + biomeLift;
        if (canyonField > 0.68) {
            baseHeight -= 12.0 + (canyonField - 0.68) * 34.0;
        }
        return clamp((int) Math.round(baseHeight), GameConfig.WORLD_MIN_Y + 8, GameConfig.WORLD_MAX_Y - 9);
    }

    private int worldToChunk(int coordinate) {
        return Math.floorDiv(coordinate, GameConfig.CHUNK_SIZE);
    }

    private int localBlockCoordinate(int coordinate) {
        return Math.floorMod(coordinate, GameConfig.CHUNK_SIZE);
    }

    private long columnKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (((long) chunkZ) & 0xFFFFFFFFL);
    }

    private int unpackColumnX(long columnKey) {
        return (int) (columnKey >> 32);
    }

    private int unpackColumnZ(long columnKey) {
        return (int) columnKey;
    }

    private long packBlock(int x, int y, int z) {
        long packedX = ((long) x) & 0x3FFFFFFL;
        long packedZ = ((long) z) & 0x3FFFFFFL;
        long packedY = (long) (y - GameConfig.WORLD_MIN_Y) & 0xFFFL;
        return (packedX << 38) | (packedZ << 12) | packedY;
    }

    private int unpackBlockX(long blockKey) {
        int value = (int) ((blockKey >>> 38) & 0x3FFFFFFL);
        return value >= 0x2000000 ? value - 0x4000000 : value;
    }

    private int unpackBlockY(long blockKey) {
        return (int) (blockKey & 0xFFFL) + GameConfig.WORLD_MIN_Y;
    }

    private int unpackBlockZ(long blockKey) {
        int value = (int) ((blockKey >>> 12) & 0x3FFFFFFL);
        return value >= 0x2000000 ? value - 0x4000000 : value;
    }

    private double distanceSquared(double x1, double z1, double x2, double z2) {
        double deltaX = x1 - x2;
        double deltaZ = z1 - z2;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double normalizeWorldTime(double value) {
        double normalized = value % 1.0;
        return normalized < 0.0 ? normalized + 1.0 : normalized;
    }

    private double octaveNoise(double x, double z, int octaves, double persistence) {
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

    private double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
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
}
