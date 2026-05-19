import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

final class BackgroundSaveService {
    private static final long POISON = Long.MIN_VALUE;

    private final ConcurrentHashMap<Long, SaveJob> pendingJobs = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<Long> readyKeys = new LinkedBlockingQueue<>();
    private final AtomicInteger activeJobs = new AtomicInteger();
    private final Thread worker;
    private volatile boolean running = true;

    BackgroundSaveService() {
        worker = new Thread(this::runWorker, "tinycraft-save-worker");
        worker.setDaemon(true);
        worker.start();
    }

    void enqueue(RegionStorage storage, VoxelWorld.ChunkColumn column) {
        if (storage == null || column == null) {
            return;
        }
        long key = columnKey(column.chunkX, column.chunkZ);
        SaveJob job = new SaveJob(storage, column);
        if (pendingJobs.put(key, job) == null) {
            readyKeys.offer(key);
        }
    }

    void flush() {
        while (!pendingJobs.isEmpty() || activeJobs.get() > 0) {
            try {
                Thread.sleep(2L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    void shutdown() {
        flush();
        running = false;
        readyKeys.offer(POISON);
        try {
            worker.join(1500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void runWorker() {
        while (running) {
            try {
                long key = readyKeys.take();
                if (key == POISON) {
                    continue;
                }
                SaveJob job = pendingJobs.get(key);
                if (job == null) {
                    continue;
                }
                activeJobs.incrementAndGet();
                try {
                    pendingJobs.remove(key, job);
                    job.storage.saveColumn(job.column);
                } finally {
                    activeJobs.decrementAndGet();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException exception) {
                if (GameConfig.ENABLE_DEBUG_LOGS) {
                    System.out.println("BackgroundSaveService: failed to save column: " + exception.getMessage());
                }
            }
        }
    }

    private static long columnKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private static final class SaveJob {
        final RegionStorage storage;
        final VoxelWorld.ChunkColumn column;

        SaveJob(RegionStorage storage, VoxelWorld.ChunkColumn column) {
            this.storage = storage;
            this.column = column;
        }
    }
}
