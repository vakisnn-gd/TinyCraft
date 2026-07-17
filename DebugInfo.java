final class DebugInfo {
    static final long SLOW_REFRESH_NANOS = 1_000_000_000L;
    static final double LOW_FPS_THRESHOLD = 30.0;
    static final double HIGH_MEMORY_RATIO = 0.85;

    double fps;
    double frameTimeMs;
    double x;
    double y;
    double z;
    double yawDegrees;
    double pitchDegrees;
    int blockX;
    int blockY;
    int blockZ;
    int chunkX;
    int chunkY;
    int chunkZ;
    int localX;
    int localY;
    int localZ;
    int dimensionId;
    int lightLevel;
    boolean skyVisible;
    String facing = "unknown";
    String gameMode = "survival";
    String biome = "Unknown";
    String chunkStatus = "EMPTY";
    String region = "unknown";
    String seed = "unknown";
    int generatedSurface;
    int actualSurface;
    boolean hasTargetBlock;
    int targetX;
    int targetY;
    int targetZ;
    String targetName = "none";
    String targetId = "minecraft:air";
    int targetStateData;
    String heldItemName = "none";
    long usedMemoryMiB;
    long allocatedMemoryMiB;
    long maxMemoryMiB;
    String osDescription = "unknown";
    String javaDescription = "unknown";
    String cpuDescription = "unknown";
    int logicalProcessors;
    String gpuDescription = "unknown";
    String openGlDescription = "unknown";

    private long lastSlowRefreshNanos = Long.MIN_VALUE;
    private int cachedChunkX = Integer.MIN_VALUE;
    private int cachedChunkY = Integer.MIN_VALUE;
    private int cachedChunkZ = Integer.MIN_VALUE;
    private boolean systemInfoReady;

    boolean shouldRefreshSlow(long nowNanos, int currentChunkX, int currentChunkY, int currentChunkZ) {
        boolean moved = currentChunkX != cachedChunkX
            || currentChunkY != cachedChunkY
            || currentChunkZ != cachedChunkZ;
        boolean expired = lastSlowRefreshNanos == Long.MIN_VALUE
            || nowNanos < lastSlowRefreshNanos
            || nowNanos - lastSlowRefreshNanos >= SLOW_REFRESH_NANOS;
        if (!moved && !expired) {
            return false;
        }
        cachedChunkX = currentChunkX;
        cachedChunkY = currentChunkY;
        cachedChunkZ = currentChunkZ;
        lastSlowRefreshNanos = nowNanos;
        return true;
    }

    void refreshMemory(Runtime runtime) {
        long divisor = 1024L * 1024L;
        long total = Math.max(0L, runtime.totalMemory());
        long free = Math.max(0L, runtime.freeMemory());
        long max = Math.max(total, runtime.maxMemory());
        usedMemoryMiB = Math.max(0L, total - free) / divisor;
        allocatedMemoryMiB = total / divisor;
        maxMemoryMiB = max / divisor;
    }

    void refreshSystemInfo(Runtime runtime, String gpuVendor, String gpuRenderer, String openGlVersion) {
        if (systemInfoReady) {
            return;
        }
        String osName = systemProperty("os.name", "Unknown OS");
        String osVersion = systemProperty("os.version", "");
        String osArch = systemProperty("os.arch", "unknown");
        osDescription = joinNonEmpty(osName, osVersion) + " (" + osArch + ")";

        String javaVersion = systemProperty("java.version", "unknown");
        String dataModel = systemProperty("sun.arch.data.model", osArch.contains("64") ? "64" : "32");
        javaDescription = javaVersion + " (" + dataModel + "-bit)";

        String processor = System.getenv("PROCESSOR_IDENTIFIER");
        cpuDescription = processor == null || processor.trim().isEmpty() ? osArch : processor.trim();
        logicalProcessors = Math.max(1, runtime.availableProcessors());
        gpuDescription = joinNonEmpty(gpuVendor, gpuRenderer);
        openGlDescription = openGlVersion == null || openGlVersion.trim().isEmpty()
            ? "unknown"
            : openGlVersion.trim();
        systemInfoReady = true;
    }

    boolean hasLowFps() {
        return fps > 0.0 && fps < LOW_FPS_THRESHOLD;
    }

    boolean hasHighMemoryUsage() {
        return maxMemoryMiB > 0L && usedMemoryMiB / (double) maxMemoryMiB >= HIGH_MEMORY_RATIO;
    }

    static double frameTimeMs(double fps) {
        return fps > 0.0 ? 1000.0 / fps : 0.0;
    }

    static int approximateLightLevel(float daylight, boolean skyVisible) {
        int skyLevel = (int) Math.round(Math.max(0.0, Math.min(1.0, daylight)) * 15.0);
        return skyVisible ? skyLevel : Math.max(0, Math.min(15, (int) Math.round(skyLevel * 0.20)));
    }

    static float overlayScale(float inventoryScale, int framebufferWidth) {
        float desired = Math.max(0.72f, inventoryScale * 0.88f);
        float widthLimit = Math.max(0.72f, Math.max(1, framebufferWidth) / 820.0f);
        return Math.min(desired, widthLimit);
    }

    static float alignedTextX(float panelX, float panelWidth, float padding, float textWidth,
            boolean rightAligned) {
        return rightAligned ? panelX + panelWidth - padding - textWidth : panelX + padding;
    }

    private static String systemProperty(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String joinNonEmpty(String first, String second) {
        String left = first == null ? "" : first.trim();
        String right = second == null ? "" : second.trim();
        if (left.isEmpty()) {
            return right.isEmpty() ? "unknown" : right;
        }
        return right.isEmpty() || left.equalsIgnoreCase(right) ? left : left + " " + right;
    }
}
