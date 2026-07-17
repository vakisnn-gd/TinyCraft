import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FOG;
import static org.lwjgl.opengl.GL11.GL_FOG_COLOR;
import static org.lwjgl.opengl.GL11.GL_FOG_END;
import static org.lwjgl.opengl.GL11.GL_FOG_MODE;
import static org.lwjgl.opengl.GL11.GL_FOG_START;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_PROJECTION_MATRIX;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearDepth;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCopyTexSubImage2D;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFrustum;
import static org.lwjgl.opengl.GL11.glFogf;
import static org.lwjgl.opengl.GL11.glFogfv;
import static org.lwjgl.opengl.GL11.glFogi;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glRotated;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glShadeModel;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTranslated;
import static org.lwjgl.opengl.GL11.glVertex3d;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glGetFloatv;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;

final class OpenGlRenderer {
    private static final int INTS_PER_VERTEX = 2;
    private static final int GPU_INTS_PER_VERTEX = 3;
    private static final int GPU_BYTES_PER_VERTEX = GPU_INTS_PER_VERTEX * Integer.BYTES;
    private static final int VERTICES_PER_QUAD = 6;
    private static final int AO_FULL_BRIGHT_PACKED = 0xFF;
    private static final int AO_FLIP_DIAGONAL = 1 << 8;
    private static final int VERTEX_FLAG_FOLIAGE = 1;
    private static final int VERTEX_FLAG_UPPER = 2;
    private static final int TRANSLUCENT_SHADER_ALPHA_BASE = 0xF8;
    private static final int OPAQUE_SHADER_ALPHA_BASE = 0xFC;
    private static final long CHUNK_MESH_REBUILD_COOLDOWN_NANOS = 50_000_000L;
    private static final double PACKED_VERTEX_BIAS = 0.25;
    private static final double PACKED_VERTEX_SCALE = 32.0;
    private static final int PACKED_VERTEX_MAX = 1023;
    private static final int PACKED_REGION_COORD_MAX = 8191;
    private static final int PACKED_REGION_Z_LOW_MAX = 511;
    private static final int MIN_CHUNK_UPLOADS_PER_FRAME = 1;
    private static final int MAX_CHUNK_UPLOADS_PER_FRAME = 16;
    private static final int MAX_CATCH_UP_CHUNK_UPLOADS_PER_FRAME = 12;
    private static final int OPAQUE_RENDER_REGION_SIZE_CHUNKS = 8;
    private static final int MAX_OPAQUE_REGION_UPLOADS_PER_FRAME = 1;
    private static final long OPAQUE_REGION_REVEAL_NANOS = 850_000_000L;
    private static final int MAX_IMMEDIATE_CHUNK_UPLOADS_PER_FRAME = 8;
    private static final int MAX_BACKLOG_CHUNK_UPLOADS_PER_FRAME = 24;
    private static final int MESH_THREAD_COUNT = GameConfig.CHUNK_GENERATION_THREADS;
    private static final int MAX_MESH_BUILDS_IN_FLIGHT = MESH_THREAD_COUNT * 2;
    private static final int MAX_CHUNK_DISCOVERY_PER_FRAME = 512;
    private static final int MAX_DIRTY_CANDIDATES_PER_FRAME = 512;
    private static final int INITIAL_MESH_BUILD_RADIUS = 12;
    private static final int MAX_PENDING_DIRTY_MESH_BUILDS = 2048;
    private static final int MAX_QUEUED_MESH_BUILD_CANDIDATES = 512;
    private static final long NORMAL_MESH_UPLOAD_BUDGET_NANOS = 2_000_000L;
    private static final long LOW_FPS_MESH_UPLOAD_BUDGET_NANOS = 1_500_000L;
    private static final long BACKLOG_MESH_UPLOAD_BUDGET_NANOS = 4_000_000L;
    private static final double LIQUID_SURFACE_Z_OFFSET = 0.01;
    private static final double CAMERA_NEAR_PLANE = 0.08;
    private static final double CAMERA_FAR_PADDING = 64.0;
    private static final int RENDER_EDGE_PADDING_CHUNKS = 2;
    private static final String UI_FONT_FAMILY = "SansSerif";
    private static final int MAX_TEXT_TEXTURE_CACHE_SIZE = 512;
    private static final int ATLAS_TILE_SIZE = 16;
    private static final int ATLAS_COLUMNS = 4;
    private static final int ATLAS_ROWS = 4;
    private static final int MENU_PANORAMA_FACE_COUNT = 6;
    private static final int MENU_PANORAMA_CAPTURE_SIZE = 1024;
    private static final DateTimeFormatter SCREENSHOT_TIME =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);

    private final VoxelWorld world;
    private final SkyRenderer skyRenderer;
    private final WaterRenderer waterRenderer;
    private final UiRenderer uiRenderer;
    private final HashMap<Long, ChunkMesh> chunkMeshes = new HashMap<>();
    private final HashMap<Long, OpaqueRenderRegion> opaqueRenderRegions = new HashMap<>();
    private final ArrayDeque<Long> pendingOpaqueRegionBuilds = new ArrayDeque<>();
    private final HashSet<Long> pendingOpaqueRegionBuildKeys = new HashSet<>();
    private final HashSet<Long> dirtyChunkMeshes = new HashSet<>();
    private final HashSet<Long> immediateChunkMeshes = new HashSet<>();
    private final ArrayList<ChunkMesh> transparentChunkPass = new ArrayList<>();
    private final ArrayList<Long> staleMeshKeys = new ArrayList<>();
    private final ArrayList<Chunk> loadedChunkSnapshot = new ArrayList<>();
    private final ArrayDeque<Long> pendingDirtyMeshBuilds = new ArrayDeque<>();
    private final HashSet<Long> pendingDirtyMeshBuildKeys = new HashSet<>();
    private final PriorityQueue<MeshBuildCandidate> meshBuildQueue = new PriorityQueue<>();
    private final HashSet<Long> queuedMeshBuildKeys = new HashSet<>();
    private final MeshBuildRevisionTracker meshBuildRevisions = new MeshBuildRevisionTracker();
    private final MeshBuildRadiusController meshBuildRadiusController = new MeshBuildRadiusController(INITIAL_MESH_BUILD_RADIUS);
    // Value is the world/mesh epoch that owns the task. This prevents an old
    // worker from clearing a newer task with the same chunk key after a world change.
    private final ConcurrentHashMap<Long, Integer> meshBuildsInFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> meshBuildsReady = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> meshBuildCooldownUntilNanos = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<MeshBuildResult> completedImmediateMeshBuilds = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MeshBuildResult> completedMeshBuilds = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OpaqueRegionBuildResult> completedOpaqueRegionBuilds = new ConcurrentLinkedQueue<>();
    private final ExecutorService meshExecutor = createMeshExecutor();
    private final ExecutorService opaqueRegionExecutor = createOpaqueRegionExecutor();
    private final LinkedHashMap<String, TextTexture> textTextures = new LinkedHashMap<>(128, 0.75f, true);
    private final DebugInfo debugInfo = new DebugInfo();
    private final Frustum chunkFrustum = new Frustum();
    private final FloatBuffer fogColorScratch = BufferUtils.createFloatBuffer(4);

    private int framebufferWidth = GameConfig.WINDOW_WIDTH;
    private int framebufferHeight = GameConfig.WINDOW_HEIGHT;
    private int terrainTextureId;
    private final int[] menuPanoramaTextureIds = new int[MENU_PANORAMA_FACE_COUNT];
    private boolean menuPanoramaReady;
    private int blurredWorldTextureId;
    private int blurredWorldTextureWidth;
    private int blurredWorldTextureHeight;
    private int uploadProbeVboId;
    private int chunkShaderProgram;
    private int chunkShaderViewProjectionLocation = -1;
    private int chunkShaderOriginLocation = -1;
    private int chunkShaderCameraPositionLocation = -1;
    private int chunkShaderTimeLocation = -1;
    private int chunkShaderTransparentPassLocation = -1;
    private int chunkShaderDaylightLocation = -1;
    private int chunkShaderFancyGraphicsLocation = -1;
    private int chunkShaderFogColorLocation = -1;
    private int chunkShaderFogDensityLocation = -1;
    private int chunkShaderBrightnessLocation = -1;
    private int chunkShaderRevealLocation = -1;
    private int chunkShaderRegionMeshLocation = -1;
    private final FloatBuffer matrixScratch = BufferUtils.createFloatBuffer(16);
    private final float[] modelViewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private double currentFovDegrees = GameConfig.FOV_DEGREES;
    private int currentRenderDistanceChunks = GameConfig.CHUNK_RENDER_DISTANCE;
    private float currentDaylight = 1.0f;
    private float currentSceneBrightness = 1.0f;
    private float currentSkyRed = 0.50f;
    private float currentSkyGreen = 0.68f;
    private float currentSkyBlue = 0.92f;
    private boolean resourcesReady;
    private boolean vaoSupported;
    private double debugFps;
    private double fpsSampleTime;
    private int fpsSampleFrames;
    private int loadedChunkDiscoveryCursor;
    private int lastMeshPriorityChunkX = Integer.MIN_VALUE;
    private int lastMeshPriorityChunkZ = Integer.MIN_VALUE;
    private long debugMeshUploadNanosLastFrame;
    private int debugOpaqueRegionDrawCallsLastFrame;
    private int debugTransparentChunkDrawCallsLastFrame;
    private int opaqueRegionBuildsInFlight;
    private double renderCameraX;
    private double renderCameraY;
    private double renderCameraZ;
    private double renderCameraYaw;
    private double renderCameraPitch;
    private int cameraBlockX;
    private int cameraBlockY;
    private int cameraBlockZ;
    private double cameraForwardX = 1.0;
    private double cameraForwardY;
    private double cameraForwardZ;
    private int lastCameraSolidBlockX = Integer.MIN_VALUE;
    private int lastCameraSolidBlockY = Integer.MIN_VALUE;
    private int lastCameraSolidBlockZ = Integer.MIN_VALUE;
    private boolean lastCameraInsideSolidBlock;
    private boolean spectatorInsideBlock;
    private boolean lastSpectatorInsideBlock;
    private boolean chunkBordersEnabled;
    private long lastMeshProfileLogNanos;
    private volatile int meshBuildEpoch;
    private double menuPanoramaYaw;
    private double currentPartialTicks = 1.0;

    OpenGlRenderer(VoxelWorld world) {
        this.world = world;
        this.skyRenderer = new SkyRenderer();
        this.waterRenderer = new WaterRenderer(world);
        this.uiRenderer = new UiRenderer();
    }

    private static ExecutorService createMeshExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "tinycraft-mesh-worker");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(MESH_THREAD_COUNT, factory);
    }

    private static ExecutorService createOpaqueRegionExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "tinycraft-opaque-region-worker");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadExecutor(factory);
    }

    void init() {
        GL.createCapabilities();
        verifyOpenGl("GL.createCapabilities");
        vaoSupported = GL.getCapabilities().OpenGL30 || GL.getCapabilities().GL_ARB_vertex_array_object;
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glClearDepth(1.0);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glShadeModel(GL_SMOOTH);
        glClearColor(0.53f, 0.72f, 0.94f, 1.0f);
        verifyOpenGl("OpenGL state init");

        terrainTextureId = loadTerrainTexture();
        menuPanoramaReady = loadMenuPanoramaTextures();
        uploadProbeVboId = createUploadProbeVbo();
        chunkShaderProgram = createChunkShaderProgram();
        resourcesReady = terrainTextureId != 0 && uploadProbeVboId != 0 && chunkShaderProgram != 0;
        if (!resourcesReady) {
            throw new IllegalStateException("Renderer resources were not created.");
        }

        verifyOpenGl("chunk mesh init");
    }

    void updateFramebufferSize(long window) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            glfwGetFramebufferSize(window, widthBuffer, heightBuffer);
            framebufferWidth = Math.max(1, widthBuffer.get(0));
            framebufferHeight = Math.max(1, heightBuffer.get(0));
        }
    }

    int getPauseOptionAt(double cursorX, double cursorY) {
        float uiScale = Math.max(1.0f, getUiScale());
        float wideWidth = 398.0f * uiScale;
        float buttonHeight = 38.0f * uiScale;
        float centerX = framebufferWidth * 0.5f;
        float y = 142.0f * uiScale;
        for (int i = 0; i < GameConfig.pauseOptions().length; i++) {
            float buttonY = y + i * 52.0f * uiScale;
            if (cursorX >= centerX - wideWidth * 0.5f && cursorX <= centerX + wideWidth * 0.5f
                && cursorY >= buttonY && cursorY <= buttonY + buttonHeight) {
                return i;
            }
        }
        return -1;
    }

    int getOptionsSliderAt(double cursorX, double cursorY, int menuScreen) {
        if (menuScreen != GameConfig.MENU_SCREEN_OPTIONS) {
            return -1;
        }
        float uiScale = Math.max(1.0f, getUiScale());
        if (isPointInsideSettingsSlider(cursorX, cursorY, 0, optionsRenderDistanceSliderY(uiScale), uiScale)) {
            return 0;
        }
        if (isPointInsideSettingsSlider(cursorX, cursorY, 1, optionsFovSliderY(uiScale), uiScale)) {
            return 1;
        }
        if (isPointInsideSettingsSlider(cursorX, cursorY, 2, optionsBrightnessSliderY(uiScale), uiScale)) {
            return 2;
        }
        if (isPointInsideSettingsSlider(cursorX, cursorY, 3, optionsVolumeSliderY(uiScale), uiScale)) {
            return 3;
        }
        if (isPointInsideSettingsSlider(cursorX, cursorY, 4, optionsFpsSliderY(uiScale), uiScale)) {
            return 4;
        }
        if (isPointInsideSettingsSlider(cursorX, cursorY, 5, optionsGuiScaleSliderY(uiScale), uiScale)) {
            return 5;
        }
        return -1;
    }

    double sliderPercentAt(double cursorX, int sliderIndex) {
        float uiScale = Math.max(1.0f, getUiScale());
        float sliderWidth = optionsControlWidth(uiScale);
        float sliderX = optionsControlX(sliderIndex, uiScale);
        return clamp((cursorX - sliderX) / sliderWidth, 0.0, 1.0);
    }

    private boolean isPointInsideSettingsSlider(double cursorX, double cursorY, int optionIndex, float y, float uiScale) {
        float sliderWidth = optionsControlWidth(uiScale);
        float sliderX = optionsControlX(optionIndex, uiScale);
        return cursorX >= sliderX - 10.0f * uiScale
            && cursorX <= sliderX + sliderWidth + 10.0f * uiScale
            && cursorY >= y - 16.0f * uiScale
            && cursorY <= y + 22.0f * uiScale;
    }

    private float optionsControlWidth(float uiScale) {
        return Math.min(360.0f * uiScale, Math.max(220.0f * uiScale, (framebufferWidth - 180.0f * uiScale) * 0.5f));
    }

    private float optionsColumnGap(float uiScale) {
        return 44.0f * uiScale;
    }

    private float optionsControlX(int optionIndex, float uiScale) {
        float controlWidth = optionsControlWidth(uiScale);
        float gap = optionsColumnGap(uiScale);
        boolean rightColumn = optionIndex >= 3 && optionIndex <= 7 && optionIndex != 6;
        if (rightColumn) {
            return framebufferWidth * 0.5f + gap * 0.5f;
        }
        return framebufferWidth * 0.5f - controlWidth - gap * 0.5f;
    }

    private float optionsRowY(int row, float uiScale) {
        return Math.max(110.0f * uiScale, framebufferHeight * 0.18f) + row * 76.0f * uiScale;
    }

    private float optionsRenderDistanceSliderY(float uiScale) {
        return optionsRowY(0, uiScale);
    }

    private float optionsFovSliderY(float uiScale) {
        return optionsRowY(1, uiScale);
    }

    private float optionsBrightnessSliderY(float uiScale) {
        return optionsRowY(2, uiScale);
    }

    private float optionsVolumeSliderY(float uiScale) {
        return optionsRowY(0, uiScale);
    }

    private float optionsFpsSliderY(float uiScale) {
        return optionsRowY(1, uiScale);
    }

    private float optionsGuiScaleSliderY(float uiScale) {
        return optionsRowY(2, uiScale);
    }

    private float optionsGraphicsButtonY(float uiScale) {
        return optionsRowY(3, uiScale);
    }

    private float optionsLanguageButtonY(float uiScale) {
        return optionsRowY(3, uiScale);
    }

    private float optionsBackButtonY(float uiScale) {
        return optionsRowY(4, uiScale);
    }

    int getDeathOptionAt(double cursorX, double cursorY) {
        return uiRenderer.getDeathOptionAt(cursorX, cursorY, framebufferWidth, Math.max(1.0f, getUiScale()));
    }

    int getMainMenuOptionAt(double cursorX, double cursorY, int menuScreen) {
        float uiScale = Math.max(1.0f, getUiScale());
        if (menuScreen == GameConfig.MENU_SCREEN_OPTIONS) {
            float actionWidth = optionsControlWidth(uiScale);
            float actionHeight = 46.0f * uiScale;
            float graphicsX = optionsControlX(6, uiScale);
            float graphicsY = optionsGraphicsButtonY(uiScale);
            if (cursorX >= graphicsX && cursorX <= graphicsX + actionWidth && cursorY >= graphicsY && cursorY <= graphicsY + actionHeight) {
                return 6;
            }
            float languageX = optionsControlX(7, uiScale);
            float languageY = optionsLanguageButtonY(uiScale);
            if (cursorX >= languageX && cursorX <= languageX + actionWidth && cursorY >= languageY && cursorY <= languageY + actionHeight) {
                return 7;
            }
            float x = framebufferWidth * 0.5f - actionWidth * 0.5f;
            float y = optionsBackButtonY(uiScale);
            return cursorX >= x && cursorX <= x + actionWidth && cursorY >= y && cursorY <= y + actionHeight ? 8 : -1;
        }
        String[] actions = menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER ? GameConfig.singleplayerActions()
            : (menuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD ? GameConfig.createWorldActions()
            : (menuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER ? GameConfig.multiplayerActions()
            : (menuScreen == GameConfig.MENU_SCREEN_OPEN_LAN ? GameConfig.lanActions()
            : (menuScreen == GameConfig.MENU_SCREEN_DISCONNECTED ? GameConfig.disconnectedActions()
            : (menuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD ? GameConfig.renameWorldActions()
            : GameConfig.worldMenuActions())))));
        float actionWidth = menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER ? 118.0f * uiScale
            : (menuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD || menuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD || menuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER || menuScreen == GameConfig.MENU_SCREEN_OPEN_LAN || menuScreen == GameConfig.MENU_SCREEN_DISCONNECTED ? 240.0f * uiScale : 280.0f * uiScale);
        float actionHeight = menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER ? 38.0f * uiScale : 46.0f * uiScale;
        float gap = menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER ? 10.0f * uiScale : 12.0f * uiScale;
        boolean horizontal = menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER || menuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD || menuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD || menuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER || menuScreen == GameConfig.MENU_SCREEN_OPEN_LAN || menuScreen == GameConfig.MENU_SCREEN_DISCONNECTED;
        float actionsX = horizontal
            ? framebufferWidth * 0.5f - (actionWidth * actions.length + gap * (actions.length - 1)) * 0.5f
            : framebufferWidth * 0.5f - actionWidth * 0.5f;
        float firstY = menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER ? framebufferHeight - 58.0f * uiScale
            : (menuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD || menuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD || menuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER || menuScreen == GameConfig.MENU_SCREEN_OPEN_LAN || menuScreen == GameConfig.MENU_SCREEN_DISCONNECTED ? framebufferHeight - 78.0f * uiScale : framebufferHeight * 0.5f - 18.0f * uiScale);
        for (int i = 0; i < actions.length; i++) {
            float boxX = horizontal ? actionsX + i * (actionWidth + gap) : actionsX;
            float boxY = horizontal ? firstY : firstY + i * 58.0f * uiScale;
            if (cursorX >= boxX && cursorX <= boxX + actionWidth && cursorY >= boxY && cursorY <= boxY + actionHeight) {
                return i;
            }
        }
        return -1;
    }

    int getMainMenuWorldAt(double cursorX, double cursorY, int menuScreen, int worldCount, int scrollOffset) {
        if (menuScreen != GameConfig.MENU_SCREEN_SINGLEPLAYER) {
            return -1;
        }
        float uiScale = Math.max(1.0f, getUiScale());
        float panelWidth = framebufferWidth - 160.0f * uiScale;
        float panelX = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float listX = panelX + 28.0f * uiScale;
        float listY = 96.0f * uiScale;
        float listWidth = panelWidth - 56.0f * uiScale;
        float rowHeight = 36.0f * uiScale;
        int visibleRows = Math.min(GameConfig.WORLD_MENU_VISIBLE_ROWS, Math.max(0, worldCount - scrollOffset));
        for (int row = 0; row < visibleRows; row++) {
            float rowY = listY + row * (rowHeight + 8.0f * uiScale);
            if (cursorX >= listX && cursorX <= listX + listWidth && cursorY >= rowY && cursorY <= rowY + rowHeight) {
                return scrollOffset + row;
            }
        }
        return -1;
    }

    InventorySlotRef getInventorySlotAt(PlayerInventory inventory, boolean creativeMode, int creativeTab, int creativeScrollOffset, int inventoryScreenMode, double mouseX, double mouseY) {
        UiRenderer.InventoryUiLayout layout = buildInventoryLayout(creativeMode, creativeTab, creativeScrollOffset, inventoryScreenMode);
        UiRenderer.SlotBox slot = layout.hitTest(mouseX, mouseY);
        if (slot == null) {
            return null;
        }
        if (slot.ref.group == InventorySlotGroup.CREATIVE && slot.ref.index >= InventoryItems.CREATIVE_ITEMS.length) {
            return null;
        }
        return slot.ref;
    }

    boolean isInventoryPointInside(boolean creativeMode, int creativeTab, int inventoryScreenMode, double mouseX, double mouseY) {
        return buildInventoryLayout(creativeMode, creativeTab, inventoryScreenMode).contains(mouseX, mouseY);
    }

    int getCreativeTabAt(double mouseX, double mouseY, int creativeTab) {
        UiRenderer.InventoryUiLayout layout = buildInventoryLayout(true, creativeTab);
        float uiScale = getInventoryUiScale();
        float gap = 4.0f * uiScale;
        String[] tabLabels = GameConfig.creativeTabs();
        float tabWidth = (layout.panelWidth - 32.0f * uiScale - gap * (tabLabels.length - 1)) / tabLabels.length;
        float tabHeight = 22.0f * uiScale;
        float totalWidth = tabWidth * tabLabels.length + gap * (tabLabels.length - 1);
        float startX = layout.panelX + layout.panelWidth * 0.5f - totalWidth * 0.5f;
        float y = layout.panelY + 10.0f * uiScale;
        for (int i = 0; i < tabLabels.length; i++) {
            float x = startX + i * (tabWidth + gap);
            if (mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight) {
                return i;
            }
        }
        return -1;
    }

    int getCreateWorldFieldAt(double mouseX, double mouseY) {
        float uiScale = Math.max(1.0f, getUiScale());
        float panelWidth = Math.min(720.0f * uiScale, framebufferWidth - 120.0f * uiScale);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float nameY = 126.0f * uiScale;
        float seedY = 386.0f * uiScale;
        float fieldHeight = 36.0f * uiScale;
        if (mouseX >= x && mouseX <= x + panelWidth && mouseY >= nameY && mouseY <= nameY + fieldHeight) {
            return 0;
        }
        if (mouseX >= x && mouseX <= x + panelWidth && mouseY >= seedY && mouseY <= seedY + fieldHeight) {
            return 1;
        }
        return -1;
    }

    int getCreateWorldToggleAt(double mouseX, double mouseY) {
        float uiScale = Math.max(1.0f, getUiScale());
        float buttonWidth = 340.0f * uiScale;
        float buttonHeight = 44.0f * uiScale;
        float gap = 18.0f * uiScale;
        float startX = framebufferWidth * 0.5f - (buttonWidth * 2.0f + gap) * 0.5f;
        float firstRowY = 224.0f * uiScale;
        for (int i = 0; i < 2; i++) {
            float x = startX + i * (buttonWidth + gap);
            if (mouseX >= x && mouseX <= x + buttonWidth && mouseY >= firstRowY && mouseY <= firstRowY + buttonHeight) {
                return i;
            }
        }
        float secondRowY = 282.0f * uiScale;
        for (int i = 0; i < 2; i++) {
            float x = startX + i * (buttonWidth + gap);
            if (mouseX >= x && mouseX <= x + buttonWidth && mouseY >= secondRowY && mouseY <= secondRowY + buttonHeight) {
                return i + 2;
            }
        }
        return -1;
    }

    int getMultiplayerFieldAt(double mouseX, double mouseY) {
        float uiScale = Math.max(1.0f, getUiScale());
        float panelWidth = Math.min(720.0f * uiScale, framebufferWidth - 120.0f * uiScale);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float fieldHeight = 36.0f * uiScale;
        float nameY = 142.0f * uiScale;
        float hostY = 232.0f * uiScale;
        float portY = 322.0f * uiScale;
        if (mouseX >= x && mouseX <= x + panelWidth && mouseY >= nameY && mouseY <= nameY + fieldHeight) {
            return 0;
        }
        if (mouseX >= x && mouseX <= x + panelWidth && mouseY >= hostY && mouseY <= hostY + fieldHeight) {
            return 1;
        }
        if (mouseX >= x && mouseX <= x + panelWidth && mouseY >= portY && mouseY <= portY + fieldHeight) {
            return 2;
        }
        return -1;
    }

    int getLanToggleAt(double mouseX, double mouseY) {
        float uiScale = Math.max(1.0f, getUiScale());
        float buttonWidth = 240.0f * uiScale;
        float buttonHeight = 32.0f * uiScale;
        float gap = 12.0f * uiScale;
        float startX = framebufferWidth * 0.5f - (buttonWidth * 2.0f + gap) * 0.5f;
        float y = 170.0f * uiScale;
        if (mouseY >= y && mouseY <= y + buttonHeight) {
            if (mouseX >= startX && mouseX <= startX + buttonWidth) {
                return 0;
            }
            float cheatsX = startX + buttonWidth + gap;
            if (mouseX >= cheatsX && mouseX <= cheatsX + buttonWidth) {
                return 1;
            }
        }
        return -1;
    }

    int getRenameWorldFieldAt(double mouseX, double mouseY) {
        float uiScale = Math.max(1.0f, getUiScale());
        float panelWidth = Math.min(720.0f * uiScale, framebufferWidth - 120.0f * uiScale);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float y = 246.0f * uiScale;
        float fieldHeight = 36.0f * uiScale;
        return mouseX >= x && mouseX <= x + panelWidth && mouseY >= y && mouseY <= y + fieldHeight ? 0 : -1;
    }

    void renderLoadingScreen(String message, String detail) {
        if (!resourcesReady || framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        if (menuPanoramaReady) {
            glDisable(GL_CULL_FACE);
            setupProjection();
            setupMenuPanoramaSkyboxCamera(1.0 / 60.0);
            renderMenuPanoramaSkybox();
            renderBlurredWorldBackground();
        }
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, framebufferWidth, framebufferHeight, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        if (menuPanoramaReady) {
            drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.02f, 0.025f, 0.03f, 0.48f);
        } else {
            drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.20f, 0.28f, 0.38f, 1.0f);
            drawRect(0.0f, framebufferHeight * 0.56f, framebufferWidth, framebufferHeight * 0.44f, 0.08f, 0.16f, 0.10f, 0.40f);
        }
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(framebufferHeight * 0.45f, uiScale * 1.15f, message, 0.96f, 0.96f, 0.96f);
        if (detail != null && !detail.isEmpty()) {
            drawCenteredShadowText(framebufferHeight * 0.45f + 34.0f * uiScale, uiScale * 0.78f, detail, 0.72f, 0.74f, 0.78f);
        }

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_DEPTH_TEST);
    }

    void buildAllChunkMeshes() {
        markAllChunksDirty();
    }

    void clearWorldMeshes() {
        meshBuildEpoch++;
        for (ChunkMesh mesh : chunkMeshes.values()) {
            unloadChunkMesh(mesh);
        }
        chunkMeshes.clear();
        clearOpaqueRenderRegions();
        dirtyChunkMeshes.clear();
        meshBuildRevisions.clear();
        immediateChunkMeshes.clear();
        pendingDirtyMeshBuilds.clear();
        pendingDirtyMeshBuildKeys.clear();
        meshBuildQueue.clear();
        queuedMeshBuildKeys.clear();
        meshBuildsInFlight.clear();
        meshBuildsReady.clear();
        meshBuildCooldownUntilNanos.clear();
        completedImmediateMeshBuilds.clear();
        completedMeshBuilds.clear();
        completedOpaqueRegionBuilds.clear();
        staleMeshKeys.clear();
        transparentChunkPass.clear();
        loadedChunkDiscoveryCursor = 0;
        lastMeshPriorityChunkX = Integer.MIN_VALUE;
        lastMeshPriorityChunkZ = Integer.MIN_VALUE;
        meshBuildRadiusController.reset();
    }

    void rebuildChunksAroundBlock(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_SIZE);
        markChunkColumnDirty(chunkX, chunkZ);

        if (Math.floorMod(blockX, GameConfig.CHUNK_SIZE) == 0) {
            markChunkColumnDirty(chunkX - 1, chunkZ);
        }
        if (Math.floorMod(blockX, GameConfig.CHUNK_SIZE) == GameConfig.CHUNK_SIZE - 1) {
            markChunkColumnDirty(chunkX + 1, chunkZ);
        }
        if (Math.floorMod(blockZ, GameConfig.CHUNK_SIZE) == 0) {
            markChunkColumnDirty(chunkX, chunkZ - 1);
        }
        if (Math.floorMod(blockZ, GameConfig.CHUNK_SIZE) == GameConfig.CHUNK_SIZE - 1) {
            markChunkColumnDirty(chunkX, chunkZ + 1);
        }
    }

    void rebuildChunkSectionAroundBlock(int blockX, int blockY, int blockZ) {
        forceRebuildBlockEdit(blockX, blockY, blockZ);
    }

    void forceRebuildBlockEdit(int blockX, int blockY, int blockZ) {
        rebuildChunkSectionAroundBlock(blockX, blockY, blockZ, true);
    }

    void rebuildChunkSectionAroundBlockThrottled(int blockX, int blockY, int blockZ) {
        rebuildChunkSectionAroundBlock(blockX, blockY, blockZ, false);
    }

    private void rebuildChunkSectionAroundBlock(int blockX, int blockY, int blockZ, boolean immediate) {
        if (!GameConfig.isWorldYInside(blockY)) {
            rebuildChunksAroundBlock(blockX, blockZ);
            return;
        }
        ChunkSectionInvalidation.aroundBlock(
            blockX,
            blockY,
            blockZ,
            (chunkX, chunkY, chunkZ) -> markChunkDirty(chunkX, chunkY, chunkZ, immediate)
        );
    }

    void setChunkBordersEnabled(boolean chunkBordersEnabled) {
        this.chunkBordersEnabled = chunkBordersEnabled;
    }

    void markAllChunksDirty() {
        dirtyChunkMeshes.clear();
        pendingDirtyMeshBuilds.clear();
        pendingDirtyMeshBuildKeys.clear();
        meshBuildQueue.clear();
        queuedMeshBuildKeys.clear();
        world.fillLoadedChunksSnapshot(loadedChunkSnapshot);
        for (Chunk chunk : loadedChunkSnapshot) {
            if (!chunk.isEmpty()) {
                long meshKey = chunkKey(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
                meshBuildRevisions.invalidate(meshKey);
                dirtyChunkMeshes.add(meshKey);
            }
        }
        loadedChunkDiscoveryCursor = 0;
    }

    void render(PlayerState player, PlayerInventory inventory, RayHit hoveredBlock, RayHit breakingBlock, double breakingProgress, boolean paused, boolean inventoryOpen, int inventoryScreenMode, ContainerInventory chestContainer, FurnaceBlockEntity furnace, boolean deathScreenActive, int deathSelection, boolean mainMenuActive, int mainMenuScreen, boolean optionsOpenedFromPause, int mainMenuSelection, boolean mainMenuWorldActionsEnabled, String createWorldName, String createWorldSeed, int createWorldGameMode, boolean createWorldAllowCheats, int createWorldDifficulty, int createWorldTerrainPreset, int activeMenuTextField, String renameWorldName, String multiplayerName, String multiplayerHost, String multiplayerPort, String multiplayerStatus, int lanGameMode, boolean lanAllowCheats, List<WorldInfo> worlds, int selectedWorldIndex, int mainMenuScrollOffset, String loadedWorldName, boolean showDebugInfo, boolean hideHud, int pauseSelection, boolean gameModeSwitcherActive, int gameModeSelection, byte selectedBlock, int selectedSlot, int creativeTab, int creativeScrollOffset, boolean creativeMode, boolean thirdPersonView, boolean frontThirdPersonView, boolean sprinting, int renderDistanceChunks, int fovDegrees, double timeOfDay, double mouseX, double mouseY, double deltaTime, double partialTicks, ChatSystem chat, boolean showPlayerList, List<PlayerListEntry> playerList) {
        if (!resourcesReady || framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }

        currentRenderDistanceChunks = clamp(renderDistanceChunks, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
        currentPartialTicks = clamp(partialTicks, 0.0, 1.0);
        updateFpsCounter(deltaTime);
        world.fillLoadedChunksSnapshot(loadedChunkSnapshot);
        updateSkyColor(player, timeOfDay);
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDisable(GL_CULL_FACE);

        updateCameraEffects(sprinting, fovDegrees, deltaTime);
        boolean blurWorldBackground = shouldBlurWorldBackground(paused, mainMenuActive, mainMenuScreen, optionsOpenedFromPause);
        boolean menuPanorama = shouldRenderMenuPanorama(mainMenuActive, mainMenuScreen, optionsOpenedFromPause);
        if (menuPanorama) {
            if (menuPanoramaReady) {
                setupProjection();
                setupMenuPanoramaSkyboxCamera(deltaTime);
                renderMenuPanoramaSkybox();
            }
            renderOverlay(player, inventory, hoveredBlock, paused, inventoryOpen, inventoryScreenMode, chestContainer, furnace, deathScreenActive, deathSelection, mainMenuActive, mainMenuScreen, optionsOpenedFromPause, mainMenuSelection, mainMenuWorldActionsEnabled, createWorldName, createWorldSeed, createWorldGameMode, createWorldAllowCheats, createWorldDifficulty, createWorldTerrainPreset, activeMenuTextField, renameWorldName, multiplayerName, multiplayerHost, multiplayerPort, multiplayerStatus, lanGameMode, lanAllowCheats, worlds, selectedWorldIndex, mainMenuScrollOffset, loadedWorldName, showDebugInfo, hideHud, pauseSelection, gameModeSwitcherActive, gameModeSelection, selectedBlock, selectedSlot, creativeTab, creativeScrollOffset, creativeMode, thirdPersonView, renderDistanceChunks, fovDegrees, timeOfDay, mouseX, mouseY, chat, showPlayerList, playerList);
            logOpenGlError("render menu panorama");
            return;
        }
        setupProjection();
        setupCamera(player, thirdPersonView, frontThirdPersonView);
        updateChunkFrustum();
        if (shouldPrepareWorldMeshes(mainMenuActive, optionsOpenedFromPause)) {
            ensureChunkMeshesAroundPlayer(player.x, player.y, player.z, getMeshBuildRadius(player));
        }
        renderAtmosphere(player, timeOfDay, deltaTime);
        configureFog(player, timeOfDay, renderDistanceChunks);
        renderChunks(player, false);
        renderFallingBlocks(player);
        renderZombies(player);
        renderRemotePlayers(player);
        renderDroppedItems(player, timeOfDay);
        renderPlayerModel(player, inventory, selectedBlock, thirdPersonView, frontThirdPersonView);
        renderChunks(player, true);
        if (!mainMenuActive && !thirdPersonView && !frontThirdPersonView && !hideHud && !player.spectatorMode) {
            renderFirstPersonHand3d(player, selectedBlock, breakingBlock != null && breakingProgress > 0.0);
        }
        glDisable(GL_FOG);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (!mainMenuActive || optionsOpenedFromPause) {
            renderChunkBorders(player);
            renderHoveredOutline(hoveredBlock);
            renderBreakingOverlay(breakingBlock, breakingProgress);
            renderWorldTint(player);
        }
        if (shouldUsePanoramaFallbackForWorldBlur(
            blurWorldBackground, hasResidentChunkMesh(), menuPanoramaReady)) {
            setupProjection();
            setupMenuPanoramaSkyboxCamera(deltaTime);
            renderMenuPanoramaSkybox();
        }
        if (blurWorldBackground) {
            renderBlurredWorldBackground();
        }
        renderOverlay(player, inventory, hoveredBlock, paused, inventoryOpen, inventoryScreenMode, chestContainer, furnace, deathScreenActive, deathSelection, mainMenuActive, mainMenuScreen, optionsOpenedFromPause, mainMenuSelection, mainMenuWorldActionsEnabled, createWorldName, createWorldSeed, createWorldGameMode, createWorldAllowCheats, createWorldDifficulty, createWorldTerrainPreset, activeMenuTextField, renameWorldName, multiplayerName, multiplayerHost, multiplayerPort, multiplayerStatus, lanGameMode, lanAllowCheats, worlds, selectedWorldIndex, mainMenuScrollOffset, loadedWorldName, showDebugInfo, hideHud, pauseSelection, gameModeSwitcherActive, gameModeSelection, selectedBlock, selectedSlot, creativeTab, creativeScrollOffset, creativeMode, thirdPersonView, renderDistanceChunks, fovDegrees, timeOfDay, mouseX, mouseY, chat, showPlayerList, playerList);
        logOpenGlError("render");
    }

    Path captureScreenshot() throws IOException {
        if (!resourcesReady || framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw new IOException("Renderer is not ready for screenshot capture.");
        }
        Path screenshotDirectory = RuntimePaths.resolve("screenshots");
        Files.createDirectories(screenshotDirectory);
        Path output = screenshotDirectory.resolve(
            "screenshot-" + SCREENSHOT_TIME.format(LocalDateTime.now()) + ".png"
        );
        ByteBuffer rgba = BufferUtils.createByteBuffer(framebufferWidth * framebufferHeight * 4);
        glReadPixels(0, 0, framebufferWidth, framebufferHeight, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        BufferedImage image = imageFromRgba(rgba, framebufferWidth, framebufferHeight);
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("PNG writer is not available for " + output);
        }
        verifyOpenGl("capture screenshot");
        return output;
    }

    Path captureMenuPanorama(PlayerState player, int renderDistanceChunks, double timeOfDay) throws IOException {
        if (!resourcesReady || player == null || framebufferWidth <= 0 || framebufferHeight <= 0) {
            throw new IOException("Renderer is not ready for panorama capture.");
        }

        int renderSize = Math.max(1,
            Math.min(MENU_PANORAMA_CAPTURE_SIZE, Math.min(framebufferWidth, framebufferHeight)));
        int viewportX = (framebufferWidth - renderSize) / 2;
        int viewportY = (framebufferHeight - renderSize) / 2;
        Path captureDirectory = RuntimePaths.resolve(
            "screenshots",
            "panorama-" + SCREENSHOT_TIME.format(LocalDateTime.now())
        );
        Files.createDirectories(captureDirectory);

        double savedYaw = player.yaw;
        double savedPitch = player.pitch;
        double savedFov = currentFovDegrees;
        double savedPartialTicks = currentPartialTicks;
        int savedRenderDistance = currentRenderDistanceChunks;

        try {
            currentFovDegrees = 90.0;
            currentPartialTicks = 1.0;
            currentRenderDistanceChunks = clamp(
                renderDistanceChunks,
                GameConfig.MIN_RENDER_DISTANCE,
                GameConfig.MAX_RENDER_DISTANCE_CHUNKS
            );
            world.fillLoadedChunksSnapshot(loadedChunkSnapshot);
            updateSkyColor(player, timeOfDay);

            for (int face = 0; face < MENU_PANORAMA_FACE_COUNT; face++) {
                player.yaw = panoramaFaceYawRadians(face);
                player.pitch = panoramaFacePitchRadians(face);
                ByteBuffer rgba = renderPanoramaCaptureFace(
                    player,
                    timeOfDay,
                    currentRenderDistanceChunks,
                    viewportX,
                    viewportY,
                    renderSize
                );
                BufferedImage image = panoramaImageFromRgba(rgba, renderSize);
                image = scalePanoramaImage(image, MENU_PANORAMA_CAPTURE_SIZE);
                Path output = captureDirectory.resolve(panoramaFaceFileName(face));
                if (!ImageIO.write(image, "png", output.toFile())) {
                    throw new IOException("PNG writer is not available for " + output);
                }
            }
        } finally {
            player.yaw = savedYaw;
            player.pitch = savedPitch;
            currentFovDegrees = savedFov;
            currentPartialTicks = savedPartialTicks;
            currentRenderDistanceChunks = savedRenderDistance;
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        }

        return captureDirectory;
    }

    private ByteBuffer renderPanoramaCaptureFace(PlayerState player, double timeOfDay, int renderDistanceChunks,
                                                  int viewportX, int viewportY, int renderSize) {
        glViewport(viewportX, viewportY, renderSize, renderSize);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDisable(GL_CULL_FACE);
        setupProjection(90.0, 1.0);
        setupCamera(player, false, false);
        updateChunkFrustum(90.0, 1.0);
        renderAtmosphere(player, timeOfDay, 0.0);
        configureFog(player, timeOfDay, renderDistanceChunks);
        renderChunks(player, false);
        renderFallingBlocks(player);
        renderZombies(player);
        renderRemotePlayers(player);
        renderDroppedItems(player, timeOfDay);
        renderChunks(player, true);
        glDisable(GL_FOG);

        ByteBuffer rgba = BufferUtils.createByteBuffer(renderSize * renderSize * 4);
        glReadPixels(viewportX, viewportY, renderSize, renderSize, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        verifyOpenGl("capture menu panorama face");
        return rgba;
    }

    static double panoramaFaceYawRadians(int face) {
        switch (face) {
            case 0: return Math.PI;
            case 1: return 0.0;
            case 2:
            case 3:
            case 4: return -Math.PI * 0.5;
            case 5: return Math.PI * 0.5;
            default: throw new IllegalArgumentException("Unknown panorama face: " + face);
        }
    }

    static double panoramaFacePitchRadians(int face) {
        switch (face) {
            case 0:
            case 1:
            case 4:
            case 5: return 0.0;
            case 2: return Math.PI * 0.5;
            case 3: return -Math.PI * 0.5;
            default: throw new IllegalArgumentException("Unknown panorama face: " + face);
        }
    }

    static String panoramaFaceFileName(int face) {
        if (face < 0 || face >= MENU_PANORAMA_FACE_COUNT) {
            throw new IllegalArgumentException("Unknown panorama face: " + face);
        }
        return "panorama_" + face + ".png";
    }

    static BufferedImage panoramaImageFromRgba(ByteBuffer rgba, int size) {
        return imageFromRgba(rgba, size, size);
    }

    static BufferedImage imageFromRgba(ByteBuffer rgba, int width, int height) {
        if (rgba == null || width <= 0 || height <= 0 || rgba.capacity() < width * height * 4) {
            throw new IllegalArgumentException("Invalid screenshot pixel buffer.");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            int sourceY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int offset = (sourceY * width + x) * 4;
                int red = rgba.get(offset) & 0xFF;
                int green = rgba.get(offset + 1) & 0xFF;
                int blue = rgba.get(offset + 2) & 0xFF;
                image.setRGB(x, y, (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }

    private static BufferedImage scalePanoramaImage(BufferedImage source, int size) {
        if (source.getWidth() == size && source.getHeight() == size) {
            return source;
        }
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, size, size, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    void cleanup() {
        for (ChunkMesh mesh : chunkMeshes.values()) {
            unloadChunkMesh(mesh);
        }
        chunkMeshes.clear();
        clearOpaqueRenderRegions();
        dirtyChunkMeshes.clear();
        meshBuildRevisions.clear();
        immediateChunkMeshes.clear();
        pendingDirtyMeshBuilds.clear();
        pendingDirtyMeshBuildKeys.clear();
        meshBuildQueue.clear();
        queuedMeshBuildKeys.clear();
        meshBuildsInFlight.clear();
        meshBuildsReady.clear();
        meshBuildCooldownUntilNanos.clear();
        completedImmediateMeshBuilds.clear();
        completedMeshBuilds.clear();
        completedOpaqueRegionBuilds.clear();
        meshBuildRadiusController.reset();
        meshExecutor.shutdownNow();
        opaqueRegionExecutor.shutdownNow();
        if (terrainTextureId != 0) {
            glDeleteTextures(terrainTextureId);
            terrainTextureId = 0;
        }
        for (int i = 0; i < menuPanoramaTextureIds.length; i++) {
            if (menuPanoramaTextureIds[i] != 0) {
                glDeleteTextures(menuPanoramaTextureIds[i]);
                menuPanoramaTextureIds[i] = 0;
            }
        }
        menuPanoramaReady = false;
        if (blurredWorldTextureId != 0) {
            glDeleteTextures(blurredWorldTextureId);
            blurredWorldTextureId = 0;
        }
        blurredWorldTextureWidth = 0;
        blurredWorldTextureHeight = 0;
        for (TextTexture texture : textTextures.values()) {
            glDeleteTextures(texture.textureId);
        }
        textTextures.clear();
        if (uploadProbeVboId != 0) {
            glDeleteBuffers(uploadProbeVboId);
            uploadProbeVboId = 0;
        }
        if (chunkShaderProgram != 0) {
            glDeleteProgram(chunkShaderProgram);
            chunkShaderProgram = 0;
        }
        resourcesReady = false;
    }

    private int createChunkShaderProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, readShaderSource("ChunkShader.vsh"));
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, readShaderSource("ChunkShader.fsh"));
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(program);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            throw new IllegalStateException("Failed to link chunk shader: " + log);
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        chunkShaderViewProjectionLocation = glGetUniformLocation(program, "uViewProjection");
        chunkShaderOriginLocation = glGetUniformLocation(program, "uChunkOrigin");
        chunkShaderCameraPositionLocation = glGetUniformLocation(program, "uCameraPosition");
        chunkShaderTimeLocation = glGetUniformLocation(program, "uTime");
        chunkShaderTransparentPassLocation = glGetUniformLocation(program, "uTransparentPass");
        chunkShaderDaylightLocation = glGetUniformLocation(program, "uDaylight");
        chunkShaderFancyGraphicsLocation = glGetUniformLocation(program, "u_FancyGraphics");
        chunkShaderFogColorLocation = glGetUniformLocation(program, "uFogColor");
        chunkShaderFogDensityLocation = glGetUniformLocation(program, "uFogDensity");
        chunkShaderBrightnessLocation = glGetUniformLocation(program, "uBrightness");
        chunkShaderRevealLocation = glGetUniformLocation(program, "uReveal");
        chunkShaderRegionMeshLocation = glGetUniformLocation(program, "uRegionMesh");
        return program;
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile chunk shader: " + log);
        }
        return shader;
    }

    private String readShaderSource(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read shader " + filename, exception);
        }
    }

    private void updateSkyColor(PlayerState player, double timeOfDay) {
        double sun = Math.sin(timeOfDay * Math.PI * 2.0 - Math.PI * 0.5);
        float daylight = clampColor((float) ((sun + 0.18) / 1.18));
        daylight = Math.max(0.10f, daylight);
        float duskGlow = (float) Math.pow(Math.max(0.0, 1.0 - Math.abs(sun) * 1.5), 2.0);

        float skyRed = clampColor(0.03f + daylight * 0.47f + duskGlow * 0.18f);
        float skyGreen = clampColor(0.05f + daylight * 0.63f + duskGlow * 0.06f);
        float skyBlue = clampColor(0.12f + daylight * 0.80f);
        if (player != null && world.isParadiseArea(player.x, player.z)) {
            daylight = 1.0f;
            skyRed = 0.78f;
            skyGreen = 0.90f;
            skyBlue = 1.0f;
        }

        currentDaylight = daylight;
        currentSceneBrightness = (0.55f + currentDaylight * 0.45f) * Settings.brightnessMultiplier();
        currentSkyRed = skyRed;
        currentSkyGreen = skyGreen;
        currentSkyBlue = skyBlue;
        glClearColor(skyRed, skyGreen, skyBlue, 1.0f);
    }

    private void setupProjection() {
        setupProjection(currentFovDegrees, (double) framebufferWidth / framebufferHeight);
    }

    private void setupProjection(double fovDegrees, double aspect) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        double nearPlane = CAMERA_NEAR_PLANE;
        double farPlane = cameraFarPlane();
        double top = Math.tan(Math.toRadians(fovDegrees) * 0.5) * nearPlane;
        double rightPlane = top * aspect;
        glFrustum(-rightPlane, rightPlane, -top, top, nearPlane, farPlane);
    }

    private void setupCamera(PlayerState player, boolean thirdPersonView, boolean frontThirdPersonView) {
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        double pitchDegrees = Math.toDegrees(player.pitch);
        double yawDegrees = Math.toDegrees(player.yaw);
        double bobOffsetY = 0.0;
        double bobRoll = 0.0;

        double targetX = player.x;
        double targetY = thirdPersonView
            ? player.y + (player.sneaking ? 1.08 : 1.28) + bobOffsetY
            : player.y + player.eyeHeight() + bobOffsetY;
        double targetZ = player.z;

        double cameraX = targetX;
        double cameraY = targetY;
        double cameraZ = targetZ;

        if (thirdPersonView) {
            double lookX = Math.cos(player.yaw) * Math.cos(player.pitch);
            double lookY = Math.sin(player.pitch);
            double lookZ = Math.sin(player.yaw) * Math.cos(player.pitch);
            double desiredDistance = frontThirdPersonView ? 3.8 : 4.4;
            double cameraDirection = frontThirdPersonView ? 1.0 : -1.0;
            double safeDistance = resolveThirdPersonCameraDistance(
                targetX,
                targetY,
                targetZ,
                lookX,
                lookY,
                lookZ,
                cameraDirection,
                desiredDistance
            );
            cameraX = targetX + lookX * safeDistance * cameraDirection;
            cameraY = targetY + lookY * safeDistance * cameraDirection + 0.18;
            cameraZ = targetZ + lookZ * safeDistance * cameraDirection;
        }

        renderCameraX = cameraX;
        renderCameraY = cameraY;
        renderCameraZ = cameraZ;
        double viewYaw = frontThirdPersonView ? player.yaw + Math.PI : player.yaw;
        double viewPitch = frontThirdPersonView ? -player.pitch : player.pitch;
        renderCameraYaw = viewYaw;
        renderCameraPitch = viewPitch;
        cameraForwardX = Math.cos(viewYaw) * Math.cos(viewPitch);
        cameraForwardY = Math.sin(viewPitch);
        cameraForwardZ = Math.sin(viewYaw) * Math.cos(viewPitch);
        cameraBlockX = (int) Math.floor(renderCameraX);
        cameraBlockY = (int) Math.floor(renderCameraY);
        cameraBlockZ = (int) Math.floor(renderCameraZ);
        waterRenderer.setCameraBlock(cameraBlockX, cameraBlockY, cameraBlockZ);
        spectatorInsideBlock = player.spectatorMode && isCameraInsideSolidBlock();
        refreshCameraInsideBlockMesh();

        double viewPitchDegrees = frontThirdPersonView ? -pitchDegrees : pitchDegrees;
        double viewYawDegrees = frontThirdPersonView ? yawDegrees + 180.0 : yawDegrees;
        glRotatef((float) -viewPitchDegrees, 1.0f, 0.0f, 0.0f);
        glRotatef((float) (viewYawDegrees + 90.0), 0.0f, 1.0f, 0.0f);
        glRotatef((float) -bobRoll, 0.0f, 0.0f, 1.0f);
    }

    private void updateChunkFrustum() {
        updateChunkFrustum(currentFovDegrees,
            (double) framebufferWidth / Math.max(1, framebufferHeight));
    }

    private void updateChunkFrustum(double fovDegrees, double aspect) {
        chunkFrustum.set(
            0.0,
            0.0,
            0.0,
            cameraForwardX,
            cameraForwardY,
            cameraForwardZ,
            fovDegrees,
            aspect,
            CAMERA_NEAR_PLANE,
            cameraFarPlane()
        );
    }

    private double resolveThirdPersonCameraDistance(double targetX, double targetY, double targetZ,
                                                    double lookX, double lookY, double lookZ,
                                                    double cameraDirection, double desiredDistance) {
        double minDistance = 0.72;
        for (double distance = desiredDistance; distance >= minDistance; distance -= 0.16) {
            double cameraX = targetX + lookX * distance * cameraDirection;
            double cameraY = targetY + lookY * distance * cameraDirection + 0.18;
            double cameraZ = targetZ + lookZ * distance * cameraDirection;
            if (isThirdPersonCameraClear(cameraX, cameraY, cameraZ)) {
                return distance;
            }
        }
        return minDistance;
    }

    private boolean isThirdPersonCameraClear(double cameraX, double cameraY, double cameraZ) {
        if (world.collides(cameraX, cameraY - 0.20, cameraZ, 0.32, 0.40)) {
            return false;
        }
        int blockX = (int) Math.floor(cameraX);
        int blockY = (int) Math.floor(cameraY);
        int blockZ = (int) Math.floor(cameraZ);
        return !world.isSolidBlock(world.getBlock(blockX, blockY, blockZ));
    }

    static boolean shouldBlurWorldBackground(boolean paused, boolean mainMenuActive,
                                             int mainMenuScreen, boolean optionsOpenedFromPause) {
        return paused || (mainMenuActive
            && optionsOpenedFromPause
            && (mainMenuScreen == GameConfig.MENU_SCREEN_OPTIONS
                || mainMenuScreen == GameConfig.MENU_SCREEN_OPEN_LAN));
    }

    static boolean shouldRenderMenuPanorama(boolean mainMenuActive, int mainMenuScreen, boolean optionsOpenedFromPause) {
        return mainMenuActive
            && (!optionsOpenedFromPause
                || (mainMenuScreen != GameConfig.MENU_SCREEN_OPTIONS
                    && mainMenuScreen != GameConfig.MENU_SCREEN_OPEN_LAN));
    }

    static boolean shouldPrepareWorldMeshes(boolean mainMenuActive, boolean menuOpenedFromWorld) {
        return !mainMenuActive || menuOpenedFromWorld;
    }

    static boolean shouldUsePanoramaFallbackForWorldBlur(boolean blurWorldBackground,
                                                          boolean worldGeometryReady,
                                                          boolean menuPanoramaReady) {
        return blurWorldBackground && !worldGeometryReady && menuPanoramaReady;
    }

    private void renderBlurredWorldBackground() {
        ensureBlurredWorldTexture();
        glBindTexture(GL_TEXTURE_2D, blurredWorldTextureId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, framebufferWidth, framebufferHeight);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, framebufferWidth, framebufferHeight, 0.0, -1.0, 1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
        glBegin(GL_QUADS);
        glVertex3d(0.0, 0.0, 0.0);
        glVertex3d(framebufferWidth, 0.0, 0.0);
        glVertex3d(framebufferWidth, framebufferHeight, 0.0);
        glVertex3d(0.0, framebufferHeight, 0.0);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, blurredWorldTextureId);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);
        float[] offsets = {-2.0f, 0.0f, 2.0f};
        float sampleWeight = 1.0f / (offsets.length * offsets.length);
        glColor4f(sampleWeight, sampleWeight, sampleWeight, sampleWeight);
        for (float offsetY : offsets) {
            for (float offsetX : offsets) {
                drawBlurredWorldSample(offsetX / framebufferWidth, offsetY / framebufferHeight);
            }
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void ensureBlurredWorldTexture() {
        if (blurredWorldTextureId == 0) {
            blurredWorldTextureId = glGenTextures();
        }
        glBindTexture(GL_TEXTURE_2D, blurredWorldTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        if (blurredWorldTextureWidth != framebufferWidth || blurredWorldTextureHeight != framebufferHeight) {
            blurredWorldTextureWidth = framebufferWidth;
            blurredWorldTextureHeight = framebufferHeight;
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, blurredWorldTextureWidth, blurredWorldTextureHeight,
                0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }
    }

    private void drawBlurredWorldSample(float uOffset, float vOffset) {
        glBegin(GL_QUADS);
        glTexCoord2f(uOffset, 1.0f + vOffset);
        glVertex3d(0.0, 0.0, 0.0);
        glTexCoord2f(1.0f + uOffset, 1.0f + vOffset);
        glVertex3d(framebufferWidth, 0.0, 0.0);
        glTexCoord2f(1.0f + uOffset, vOffset);
        glVertex3d(framebufferWidth, framebufferHeight, 0.0);
        glTexCoord2f(uOffset, vOffset);
        glVertex3d(0.0, framebufferHeight, 0.0);
        glEnd();
    }

    private void setupMenuPanoramaSkyboxCamera(double deltaTime) {
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        double step = Math.max(0.0, Math.min(deltaTime, 0.05));
        menuPanoramaYaw += step * 0.055;
        if (menuPanoramaYaw > Math.PI * 2.0) {
            menuPanoramaYaw -= Math.PI * 2.0;
        }

        double pitch = Math.toRadians(-8.0);
        glRotatef((float) -Math.toDegrees(pitch), 1.0f, 0.0f, 0.0f);
        glRotatef((float) (Math.toDegrees(menuPanoramaYaw) + 90.0), 0.0f, 1.0f, 0.0f);
    }

    private void renderMenuPanoramaSkybox() {
        glDisable(GL_FOG);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glClear(GL_DEPTH_BUFFER_BIT);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        double size = 96.0;
        drawPanoramaFace(0, -size, size, size, -size, size, -size, -size, -size, -size, -size, -size, size);
        drawPanoramaFace(1, size, size, -size, size, size, size, size, -size, size, size, -size, -size);
        drawPanoramaFace(2, -size, size, size, size, size, size, size, size, -size, -size, size, -size);
        drawPanoramaFace(3, -size, -size, -size, size, -size, -size, size, -size, size, -size, -size, size);
        drawPanoramaFace(4, -size, size, -size, size, size, -size, size, -size, -size, -size, -size, -size);
        drawPanoramaFace(5, size, size, size, -size, size, size, -size, -size, size, size, -size, size);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void drawPanoramaFace(int face, double x1, double y1, double z1, double x2, double y2, double z2,
                                  double x3, double y3, double z3, double x4, double y4, double z4) {
        glBindTexture(GL_TEXTURE_2D, menuPanoramaTextureIds[face]);
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(x1, y1, z1);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(x2, y2, z2);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(x3, y3, z3);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(x4, y4, z4);
        glEnd();
    }

    private void renderAtmosphere(PlayerState player, double timeOfDay, double deltaTime) {
        skyRenderer.render(player, timeOfDay, deltaTime, currentDaylight, renderCameraX, renderCameraY, renderCameraZ);
    }

    private void renderWorldTint(PlayerState player) {
        float nightAlpha = (1.0f - currentDaylight) * 0.42f;
        float underwaterAlpha = player != null && player.headInWater ? 0.32f : 0.0f;
        if (nightAlpha <= 0.01f && underwaterAlpha <= 0.01f) {
            return;
        }

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, framebufferWidth, framebufferHeight, 0.0, -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
        if (nightAlpha > 0.01f) {
            drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.02f, 0.03f, 0.10f, nightAlpha);
        }
        if (underwaterAlpha > 0.01f) {
            drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.07f, 0.22f, 0.38f, underwaterAlpha);
        }
        glEnable(GL_DEPTH_TEST);
        glPopMatrix();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void renderChunks(PlayerState player, boolean transparentPass) {
        int playerChunkX = worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = worldToChunk((int) Math.floor(player.z));
        int chunkRenderRadius = getChunkRenderRadius(player);

        if (transparentPass) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false);
            glDisable(GL_CULL_FACE);
        } else {
            glDisable(GL_BLEND);
            glDepthMask(true);
            // Existing chunk faces do not all use one consistent winding order.
            // Back-face culling therefore removes valid terrain surfaces.
            glDisable(GL_CULL_FACE);
        }

        bindChunkShader(transparentPass);
        long nowNanos = System.nanoTime();
        if (!transparentPass) {
            debugOpaqueRegionDrawCallsLastFrame = 0;
            glUniform1i(chunkShaderRegionMeshLocation, 1);
            renderOpaqueRegions(playerChunkX, playerChunkZ, chunkRenderRadius, nowNanos);
            glUniform1i(chunkShaderRegionMeshLocation, 0);
        } else {
            transparentChunkPass.clear();
        }
        for (ChunkMesh mesh : chunkMeshes.values()) {
            if (mesh == null || !mesh.resident) {
                continue;
            }
            int chunkX = mesh.chunkX;
            int chunkY = mesh.chunkY;
            int chunkZ = mesh.chunkZ;
            if (Math.max(Math.abs(chunkX - playerChunkX), Math.abs(chunkZ - playerChunkZ)) > chunkRenderRadius) {
                continue;
            }
            if (!shouldRenderChunk(playerChunkX, playerChunkZ, chunkRenderRadius, chunkX, chunkY, chunkZ)) {
                continue;
            }
            if (!transparentPass && isOpaqueRegionRenderable(mesh)) {
                continue;
            }
            int vertexCount = transparentPass ? mesh.transparentVertexCount : mesh.opaqueVertexCount;
            int vaoId = transparentPass ? mesh.transparentVaoId : mesh.opaqueVaoId;
            int vboId = transparentPass ? mesh.transparentVboId : mesh.opaqueVboId;
            if (vertexCount > 0 && vboId != 0) {
                if (transparentPass) {
                    transparentChunkPass.add(mesh);
                } else {
                    setChunkReveal(mesh, nowNanos);
                    drawChunkBuffer(mesh, vaoId, vboId, vertexCount);
                    debugOpaqueRegionDrawCallsLastFrame++;
                }
            }
        }

        if (transparentPass) {
            if (transparentChunkPass.size() > 1) {
                transparentChunkPass.sort((a, b) -> Double.compare(
                    chunkDistanceSquaredToCamera(b),
                    chunkDistanceSquaredToCamera(a)
                ));
            }
            debugTransparentChunkDrawCallsLastFrame = 0;
            for (ChunkMesh mesh : transparentChunkPass) {
                setChunkReveal(mesh, nowNanos);
                drawChunkBuffer(mesh, mesh.transparentVaoId, mesh.transparentVboId, mesh.transparentVertexCount);
                debugTransparentChunkDrawCallsLastFrame++;
            }
        }
        glUseProgram(0);

        glDepthMask(true);
        if (transparentPass) {
            glDisable(GL_BLEND);
        }
    }

    private double chunkDistanceSquaredToCamera(ChunkMesh mesh) {
        double centerX = (mesh.chunkX + 0.5) * GameConfig.CHUNK_SIZE;
        double centerY = GameConfig.sectionYForIndex(mesh.chunkY) + GameConfig.CHUNK_SIZE * 0.5;
        double centerZ = (mesh.chunkZ + 0.5) * GameConfig.CHUNK_SIZE;
        double dx = centerX - renderCameraX;
        double dy = centerY - renderCameraY;
        double dz = centerZ - renderCameraZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private void bindChunkShader(boolean transparentPass) {
        glUseProgram(chunkShaderProgram);
        updateViewProjectionMatrixUniform();
        glUniform1f(chunkShaderTimeLocation, (float) (System.nanoTime() * 1.0e-9));
        glUniform1f(chunkShaderDaylightLocation, currentDaylight);
        glUniform1i(chunkShaderTransparentPassLocation, transparentPass ? 1 : 0);
        glUniform1i(chunkShaderFancyGraphicsLocation, 0);
        glUniform3f(chunkShaderCameraPositionLocation, (float) renderCameraX, (float) renderCameraY, (float) renderCameraZ);
        glUniform3f(chunkShaderFogColorLocation, currentSkyRed, currentSkyGreen, currentSkyBlue);
        float visibleBlocks = Math.max(GameConfig.CHUNK_SIZE * GameConfig.MIN_RENDER_DISTANCE, currentRenderDistanceChunks * GameConfig.CHUNK_SIZE);
        glUniform1f(chunkShaderFogDensityLocation, 1.75f / visibleBlocks);
        glUniform1f(chunkShaderBrightnessLocation, Settings.brightnessMultiplier());
        glUniform1f(chunkShaderRevealLocation, 1.0f);
        glUniform1i(chunkShaderRegionMeshLocation, 0);
    }

    private void setChunkReveal(ChunkMesh mesh, long nowNanos) {
        if (chunkShaderRevealLocation < 0) {
            return;
        }
        if (mesh.revealStartNanos <= 0L) {
            glUniform1f(chunkShaderRevealLocation, 1.0f);
            return;
        }
        float reveal = (float) clamp((nowNanos - mesh.revealStartNanos) / 650_000_000.0, 0.0, 1.0);
        glUniform1f(chunkShaderRevealLocation, reveal);
    }

    private void setOpaqueRegionReveal(OpaqueRenderRegion region, long nowNanos) {
        if (chunkShaderRevealLocation < 0 || region.revealStartNanos <= 0L) {
            glUniform1f(chunkShaderRevealLocation, 1.0f);
            return;
        }
        float reveal = (float) clamp((nowNanos - region.revealStartNanos) / (double) OPAQUE_REGION_REVEAL_NANOS, 0.0, 1.0);
        glUniform1f(chunkShaderRevealLocation, reveal);
    }

    private void updateViewProjectionMatrixUniform() {
        matrixScratch.clear();
        glGetFloatv(GL_MODELVIEW_MATRIX, matrixScratch);
        matrixScratch.get(modelViewMatrix);
        matrixScratch.clear();
        glGetFloatv(GL_PROJECTION_MATRIX, matrixScratch);
        matrixScratch.get(projectionMatrix);
        multiplyColumnMajor(projectionMatrix, modelViewMatrix, viewProjectionMatrix);
        matrixScratch.clear();
        matrixScratch.put(viewProjectionMatrix);
        matrixScratch.flip();
        glUniformMatrix4fv(chunkShaderViewProjectionLocation, false, matrixScratch);
    }

    private void multiplyColumnMajor(float[] left, float[] right, float[] out) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                out[column * 4 + row] =
                    left[0 * 4 + row] * right[column * 4 + 0]
                    + left[1 * 4 + row] * right[column * 4 + 1]
                    + left[2 * 4 + row] * right[column * 4 + 2]
                    + left[3 * 4 + row] * right[column * 4 + 3];
            }
        }
    }

    private double chunkDistanceSquaredToPoint(int chunkX, int chunkY, int chunkZ, double x, double y, double z) {
        double centerX = (chunkX + 0.5) * GameConfig.CHUNK_SIZE;
        double centerY = GameConfig.sectionYForIndex(chunkY) + GameConfig.CHUNK_SIZE * 0.5;
        double centerZ = (chunkZ + 0.5) * GameConfig.CHUNK_SIZE;
        double dx = centerX - x;
        double dy = centerY - y;
        double dz = centerZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean shouldRenderChunk(int playerChunkX, int playerChunkZ, int chunkRenderRadius, int chunkX, int chunkY, int chunkZ) {
        if (Math.max(Math.abs(chunkX - playerChunkX), Math.abs(chunkZ - playerChunkZ)) > chunkRenderRadius) {
            return false;
        }
        if (!isChunkInFrustum(chunkX, chunkY, chunkZ)) {
            return false;
        }

        return true;
    }

    private boolean isChunkInFrustum(int chunkX, int chunkY, int chunkZ) {
        double minX = cameraRelativeChunkOrigin(chunkX, renderCameraX);
        double minY = GameConfig.sectionYForIndex(chunkY) - renderCameraY;
        double minZ = cameraRelativeChunkOrigin(chunkZ, renderCameraZ);
        double maxX = minX + GameConfig.CHUNK_SIZE;
        double maxY = minY + GameConfig.CHUNK_SIZE;
        double maxZ = minZ + GameConfig.CHUNK_SIZE;
        return chunkFrustum.intersectsAabb(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private double cameraFarPlane() {
        double horizontalDistance = (currentRenderDistanceChunks + RENDER_EDGE_PADDING_CHUNKS) * GameConfig.CHUNK_SIZE;
        return Math.max(Math.hypot(horizontalDistance, GameConfig.WORLD_HEIGHT * 0.5) + CAMERA_FAR_PADDING, GameConfig.WORLD_HEIGHT * 1.45);
    }

    private void configureFog(PlayerState player, double timeOfDay, int renderDistanceChunks) {
        float daylight = currentDaylight;
        float night = 1.0f - daylight;
        float red = clampColor(0.05f + daylight * 0.50f + night * 0.03f);
        float green = clampColor(0.07f + daylight * 0.64f + night * 0.04f);
        float blue = clampColor(0.11f + daylight * 0.82f + night * 0.10f);
        if (player != null && world.isParadiseArea(player.x, player.z)) {
            red = 0.70f;
            green = 0.86f;
            blue = 0.96f;
        }
        fogColorScratch.clear();
        fogColorScratch.put(red).put(green).put(blue).put(1.0f).flip();

        float visibleBlocks = Math.max(GameConfig.CHUNK_SIZE * GameConfig.MIN_RENDER_DISTANCE, renderDistanceChunks * GameConfig.CHUNK_SIZE);
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogfv(GL_FOG_COLOR, fogColorScratch);
        glFogf(GL_FOG_START, visibleBlocks * 0.36f);
        glFogf(GL_FOG_END, visibleBlocks * 0.86f);
    }

    private boolean shouldRenderMob(PlayerState player, MobEntity mob) {
        return isSphereVisible(
            player,
            mob.x,
            mob.y + GameConfig.ZOMBIE_HEIGHT * 0.5,
            mob.z,
            0.9,
            Math.min(GameConfig.MAX_RENDER_DISTANCE, GameConfig.ZOMBIE_RENDER_DISTANCE)
        );
    }

    private boolean isSphereVisible(PlayerState player, double centerX, double centerY, double centerZ, double radius, double maxDistance) {
        double eyeX = player.x;
        double eyeY = player.y + GameConfig.EYE_HEIGHT;
        double eyeZ = player.z;
        double dx = centerX - eyeX;
        double dy = centerY - eyeY;
        double dz = centerZ - eyeZ;
        double maxVisibleDistance = maxDistance + radius;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > maxVisibleDistance * maxVisibleDistance) {
            return false;
        }

        double horizontal = Math.cos(player.pitch);
        double forwardX = Math.cos(player.yaw) * horizontal;
        double forwardY = Math.sin(player.pitch);
        double forwardZ = Math.sin(player.yaw) * horizontal;
        double forwardProjection = dx * forwardX + dy * forwardY + dz * forwardZ;
        if (forwardProjection < -radius * 1.35) {
            return false;
        }

        double lateralSquared = Math.max(0.0, distanceSquared - forwardProjection * forwardProjection);
        double effectiveProjection = Math.max(0.0, forwardProjection);
        double halfFov = Math.toRadians(currentFovDegrees * 0.5);
        double lateralAllowance = Math.tan(halfFov) * effectiveProjection + radius * 1.7 + 2.5;
        return forwardProjection > 0.0 || distanceSquared <= (radius + 3.0) * (radius + 3.0) || lateralSquared <= lateralAllowance * lateralAllowance;
    }

    private boolean submitChunkMeshBuild(MeshBuildCandidate candidate, double sortCameraX, double sortCameraY, double sortCameraZ) {
        if (meshBuildsReady.containsKey(candidate.meshKey)) {
            return false;
        }
        boolean immediate = candidate.immediate || immediateChunkMeshes.contains(candidate.meshKey);
        long now = System.nanoTime();
        Long cooldownUntil = meshBuildCooldownUntilNanos.get(candidate.meshKey);
        if (!immediate && cooldownUntil != null && now < cooldownUntil) {
            return false;
        }
        int buildEpoch = meshBuildEpoch;
        int buildRevision = meshBuildRevisions.current(candidate.meshKey);
        if (meshBuildsInFlight.putIfAbsent(candidate.meshKey, buildEpoch) != null) {
            return false;
        }
        if (immediate) {
            immediateChunkMeshes.remove(candidate.meshKey);
        }
        meshBuildCooldownUntilNanos.put(candidate.meshKey, now + CHUNK_MESH_REBUILD_COOLDOWN_NANOS);
        MeshBuildContext context = new MeshBuildContext(
            cameraBlockX,
            cameraBlockY,
            cameraBlockZ,
            spectatorInsideBlock,
            sortCameraX,
            sortCameraY,
            sortCameraZ,
            buildEpoch
        );
        meshExecutor.execute(() -> {
            try {
                MeshBuildResult result = buildChunkMeshResult(candidate.chunkX, candidate.chunkY, candidate.chunkZ, candidate.meshKey, context, buildRevision);
                if (result.epoch == meshBuildEpoch) {
                    meshBuildsReady.put(candidate.meshKey, Boolean.TRUE);
                    if (immediate) {
                        completedImmediateMeshBuilds.add(result);
                    } else {
                        completedMeshBuilds.add(result);
                    }
                }
            } finally {
                meshBuildsInFlight.remove(candidate.meshKey, buildEpoch);
            }
        });
        return true;
    }

    private MeshBuildResult buildChunkMeshResult(int chunkX, int chunkY, int chunkZ, long meshKey, MeshBuildContext context, int buildRevision) {
        if (chunkY < 0 || chunkY >= GameConfig.WORLD_CHUNKS_Y) {
            return MeshBuildResult.empty(chunkX, chunkY, chunkZ, meshKey, -1, buildRevision, true, context.epoch);
        }

        ChunkSectionSnapshot sourceChunk = world.getChunkSnapshot(chunkX, chunkY, chunkZ);
        if (sourceChunk == null || sourceChunk.isEmpty()) {
            int version = sourceChunk == null ? -1 : sourceChunk.version;
            return MeshBuildResult.empty(chunkX, chunkY, chunkZ, meshKey, version, buildRevision, true, context.epoch);
        }

        int startX = chunkX * GameConfig.CHUNK_SIZE;
        int startY = GameConfig.sectionYForIndex(chunkY);
        int startZ = chunkZ * GameConfig.CHUNK_SIZE;
        int endX = startX + GameConfig.CHUNK_SIZE;
        int endY = startY + GameConfig.CHUNK_SIZE;
        int endZ = startZ + GameConfig.CHUNK_SIZE;

        IntVertexBuilder opaqueBuilder = new IntVertexBuilder(4096);
        IntVertexBuilder transparentBuilder = new IntVertexBuilder(2048);
        long[] greedyMask = new long[GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE];
        buildChunkDisplayList(sourceChunk, opaqueBuilder, transparentBuilder, startX, startY, startZ, endX, endY, endZ, context, greedyMask);
        transparentBuilder.sortQuadsFarToNear(context.sortCameraX - startX, context.sortCameraY - startY, context.sortCameraZ - startZ);
        return new MeshBuildResult(
            chunkX,
            chunkY,
            chunkZ,
            meshKey,
            sourceChunk.version,
            buildRevision,
            opaqueBuilder.toArray(),
            opaqueBuilder.intCount(),
            transparentBuilder.toArray(),
            transparentBuilder.intCount(),
            true,
            context.epoch
        );
    }

    private void buildChunkDisplayList(ChunkSectionSnapshot sourceChunk, IntVertexBuilder opaqueBuilder, IntVertexBuilder transparentBuilder, int startX, int startY, int startZ, int endX, int endY, int endZ, MeshBuildContext context, long[] greedyMask) {
        emitGreedyCubeFaces(sourceChunk, opaqueBuilder, startX, startY, startZ, context, greedyMask);

        for (int localX = 0, x = startX; x < endX; x++, localX++) {
            for (int localY = 0, y = startY; y < endY; y++, localY++) {
                for (int localZ = 0, z = startZ; z < endZ; z++, localZ++) {
                    byte block = sourceChunk.getBlockLocal(localX, localY, localZ);
                    if (block == GameConfig.AIR) {
                        continue;
                    }
                    BlockState blockState = sourceChunk.getBlockStateLocal(localX, localY, localZ);
                    boolean cameraInsideSolidBlock = x == context.cameraBlockX
                    && y == context.cameraBlockY
                    && z == context.cameraBlockZ
                    && world.isSolidBlock(block);
                    if (cameraInsideSolidBlock && !context.spectatorInsideBlock) {
                        continue;
                    }
                    if (context.spectatorInsideBlock && cameraInsideSolidBlock) {
                        continue;
                    }
                    if (context.spectatorInsideBlock && world.isSolidBlock(block) && isNearSpectatorCutoutBlock(x, y, z, context)) {
                        continue;
                    }

                    if (isGreedyMeshBlock(block, blockState, x, y, z, context)) {
                        continue;
                    }

                    boolean stableOpaqueBlock = block == GameConfig.SNOW_LAYER
                        || block == GameConfig.OAK_FENCE
                        || block == GameConfig.OAK_FENCE_GATE
                        || block == GameConfig.OAK_LEAVES
                        || block == GameConfig.PINE_LEAVES
                        || block == GameConfig.BIRCH_LEAVES
                        || block == GameConfig.OAK_DOOR
                        || block == GameConfig.RED_BED
                        || block == GameConfig.TORCH;
                    IntVertexBuilder targetBuilder = (!stableOpaqueBlock && world.isTransparentBlock(block)) ? transparentBuilder : opaqueBuilder;
                    if (block == GameConfig.OAK_DOOR) {
                        emitDoorBlock(targetBuilder, localX, localY, localZ, x, y, z, blockState);
                        continue;
                    }
                    if (block == GameConfig.OAK_FENCE) {
                        emitFenceBlock(targetBuilder, localX, localY, localZ, x, y, z);
                        continue;
                    }
                    if (block == GameConfig.OAK_FENCE_GATE) {
                        emitFenceGateBlock(targetBuilder, localX, localY, localZ, x, y, z, blockState);
                        continue;
                    }
                    if (block == GameConfig.RED_BED) {
                        emitBedBlock(targetBuilder, localX, localY, localZ, x, y, z, blockState);
                        continue;
                    }
                    if (block == GameConfig.PARADISE_PORTAL) {
                        emitParadisePortalBlock(targetBuilder, localX, localY, localZ, x, y, z);
                        continue;
                    }
                    if (world.isStairBlock(block)) {
                        emitStairBlock(targetBuilder, localX, localY, localZ, x, y, z, block, blockState);
                        continue;
                    }
                    if (world.isCrossPlant(block)) {
                        if (isWaterloggedPlantForMesh(block)) {
                            emitWaterloggedPlantWater(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z);
                        }
                        emitCrossPlant(targetBuilder, localX, localY, localZ, x, y, z, block);
                        continue;
                    }
                    if (!world.isCubeBlock(block)) {
                        continue;
                    }

                    if (waterRenderer.isRenderableBlockFace(block, x, y, z, Face.WEST)) {
                        emitBlockFace(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z, Face.WEST, block);
                    }
                    if (waterRenderer.isRenderableBlockFace(block, x, y, z, Face.EAST)) {
                        emitBlockFace(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z, Face.EAST, block);
                    }
                    if (waterRenderer.isRenderableBlockFace(block, x, y, z, Face.TOP)) {
                        emitBlockFace(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z, Face.TOP, block);
                    }
                    if (waterRenderer.isRenderableBlockFace(block, x, y, z, Face.BOTTOM)) {
                        emitBlockFace(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z, Face.BOTTOM, block);
                    }
                    if (waterRenderer.isRenderableBlockFace(block, x, y, z, Face.NORTH)) {
                        emitBlockFace(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z, Face.NORTH, block);
                    }
                    if (waterRenderer.isRenderableBlockFace(block, x, y, z, Face.SOUTH)) {
                        emitBlockFace(sourceChunk, targetBuilder, localX, localY, localZ, startX, startY, startZ, x, y, z, Face.SOUTH, block);
                    }
                }
            }
        }
    }

    private void emitBlockFace(ChunkSectionSnapshot sourceChunk, IntVertexBuilder builder, int x, int y, int z,
                               int startX, int startY, int startZ, int worldX, int worldY, int worldZ, Face face, byte block) {
        emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, face, block, -1.0f);
    }

    private void emitBlockFace(ChunkSectionSnapshot sourceChunk, IntVertexBuilder builder, int x, int y, int z,
                               int startX, int startY, int startZ, int worldX, int worldY, int worldZ,
                               Face face, byte block, float forcedAlpha) {
        double minX = x;
        double minY = y;
        double minZ = z;
        double maxX = x + 1.0;
        double maxY = y + 1.0;
        double maxZ = z + 1.0;
        boolean transparent = world.isTransparentBlock(block);
        double offset = transparent ? 0.001 : 0.0;
        boolean liquid = world.isLiquidBlock(block);
        if (liquid) {
            maxY = y + waterRenderer.liquidRenderHeight(block, worldX, worldY, worldZ) - LIQUID_SURFACE_Z_OFFSET;
        } else if (block == GameConfig.SNOW_LAYER) {
            maxY = y + 0.125;
        } else if (block == GameConfig.RED_BED) {
            maxY = y + 0.56;
        }
        double sideMinY = world.isLiquidBlock(block) ? waterRenderer.liquidSideMinY(minY, block, face, worldX, worldY, worldZ) : minY;

        float shade = Settings.goodGraphics() ? world.getAmbientShade(worldX, worldY, worldZ) : 1.0f;
        int color = colorForFace(block, face, shade, worldX, worldY, worldZ);
        if (forcedAlpha >= 0.0f) {
            color = withAlpha(color, forcedAlpha);
        }
        int ao = (!transparent && !liquid && forcedAlpha < 0.0f)
            ? packUnitFaceAo(sourceChunk, startX, startY, startZ, worldX, worldY, worldZ, face)
            : AO_FULL_BRIGHT_PACKED;
        int vertexFlags = isWavingFoliageFace(block, face) ? VERTEX_FLAG_FOLIAGE : 0;

        switch (face) {
            case WEST:
                appendQuad(builder, minX - offset, sideMinY, minZ, minX - offset, maxY, minZ, minX - offset, maxY, maxZ, minX - offset, sideMinY, maxZ, color, ao, vertexFlags);
                break;
            case EAST:
                appendQuad(builder, maxX + offset, sideMinY, maxZ, maxX + offset, maxY, maxZ, maxX + offset, maxY, minZ, maxX + offset, sideMinY, minZ, color, ao, vertexFlags);
                break;
            case TOP:
                appendQuad(builder, minX, maxY + (liquid ? 0.0 : offset), maxZ, minX, maxY + (liquid ? 0.0 : offset), minZ, maxX, maxY + (liquid ? 0.0 : offset), minZ, maxX, maxY + (liquid ? 0.0 : offset), maxZ, color, ao, vertexFlags);
                break;
            case BOTTOM:
                appendQuad(builder, minX, minY - offset, minZ, minX, minY - offset, maxZ, maxX, minY - offset, maxZ, maxX, minY - offset, minZ, color, ao, vertexFlags);
                break;
            case NORTH:
                appendQuad(builder, maxX, sideMinY, minZ - offset, maxX, maxY, minZ - offset, minX, maxY, minZ - offset, minX, sideMinY, minZ - offset, color, ao, vertexFlags);
                break;
            case SOUTH:
                appendQuad(builder, minX, sideMinY, maxZ + offset, minX, maxY, maxZ + offset, maxX, maxY, maxZ + offset, maxX, sideMinY, maxZ + offset, color, ao, vertexFlags);
                break;
            default:
                break;
        }
        if (isOreBlock(block) && !liquid && forcedAlpha < 0.0f) {
            emitOreFaceOverlay(builder, minX, minY, minZ, maxX, maxY, maxZ, face, block, shade);
        }
        if (block == GameConfig.CACTUS && !liquid && forcedAlpha < 0.0f && face != Face.TOP && face != Face.BOTTOM) {
            emitCactusSpines(builder, minX, minY, minZ, maxX, maxY, maxZ, face, shade);
        }
    }

    private void emitCactusSpines(IntVertexBuilder builder, double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ, Face face, float ambientShade) {
        int color = packColor(0.84f * ambientShade, 0.92f * ambientShade, 0.62f * ambientShade, 1.0f);
        appendOrePatch(builder, minX, minY, minZ, maxX, maxY, maxZ, face, 0.20, 0.24, 0.05, 0.020, color);
        appendOrePatch(builder, minX, minY, minZ, maxX, maxY, maxZ, face, 0.62, 0.42, 0.05, 0.020, color);
        appendOrePatch(builder, minX, minY, minZ, maxX, maxY, maxZ, face, 0.34, 0.70, 0.05, 0.020, color);
    }

    private void emitOreFaceOverlay(IntVertexBuilder builder, double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ, Face face, byte block, float ambientShade) {
        int color = oreAccentColorPacked(block, face.brightness * ambientShade);
        double lift = 0.018;
        appendOrePatch(builder, minX, minY, minZ, maxX, maxY, maxZ, face, 0.18, 0.20, 0.20, lift, color);
        appendOrePatch(builder, minX, minY, minZ, maxX, maxY, maxZ, face, 0.56, 0.16, 0.16, lift, color);
        appendOrePatch(builder, minX, minY, minZ, maxX, maxY, maxZ, face, 0.34, 0.48, 0.24, lift, color);
    }

    private void appendOrePatch(IntVertexBuilder builder, double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, Face face,
                                double a, double b, double size, double lift, int color) {
        double a2 = Math.min(0.92, a + size);
        double b2 = Math.min(0.92, b + size);
        switch (face) {
            case WEST: {
                double x = minX - lift;
                appendQuad(builder, x, minY + a, minZ + b, x, minY + a2, minZ + b, x, minY + a2, minZ + b2, x, minY + a, minZ + b2, color);
                break;
            }
            case EAST: {
                double x = maxX + lift;
                appendQuad(builder, x, minY + a, minZ + b2, x, minY + a2, minZ + b2, x, minY + a2, minZ + b, x, minY + a, minZ + b, color);
                break;
            }
            case TOP: {
                double y = maxY + lift;
                appendQuad(builder, minX + a, y, minZ + b2, minX + a, y, minZ + b, minX + a2, y, minZ + b, minX + a2, y, minZ + b2, color);
                break;
            }
            case BOTTOM: {
                double y = minY - lift;
                appendQuad(builder, minX + a, y, minZ + b, minX + a, y, minZ + b2, minX + a2, y, minZ + b2, minX + a2, y, minZ + b, color);
                break;
            }
            case NORTH: {
                double z = minZ - lift;
                appendQuad(builder, minX + a2, minY + b, z, minX + a2, minY + b2, z, minX + a, minY + b2, z, minX + a, minY + b, z, color);
                break;
            }
            case SOUTH: {
                double z = maxZ + lift;
                appendQuad(builder, minX + a, minY + b, z, minX + a, minY + b2, z, minX + a2, minY + b2, z, minX + a2, minY + b, z, color);
                break;
            }
            default:
                break;
        }
    }

    private void emitDoorBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ, BlockState state) {
        int facing = Blocks.doorFacing(state);
        boolean open = Blocks.isDoorOpen(state);
        double thickness = 0.16;
        double minX = x;
        double maxX = x + 1.0;
        double minZ = z;
        double maxZ = z + 1.0;
        int visualFacing = open ? ((facing + 1) & 3) : facing;
        switch (visualFacing) {
            case 0:
                maxZ = z + thickness;
                break;
            case 1:
                minX = x + 1.0 - thickness;
                break;
            case 2:
                minZ = z + 1.0 - thickness;
                break;
            default:
                maxX = x + thickness;
                break;
        }
        int color = colorForFace(GameConfig.OAK_DOOR, Face.TOP, world.getAmbientShade(worldX, worldY, worldZ), worldX, worldY, worldZ);
        appendCuboid(builder, minX, y, minZ, maxX, y + 1.0, maxZ, color);
    }

    private void emitBedBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ, BlockState state) {
        float ambient = world.getAmbientShade(worldX, worldY, worldZ);
        int facing = Blocks.bedFacing(state);
        boolean head = Blocks.isBedHead(state);
        boolean metadataFootPointsAtHead = !head
            && world.getBlock(worldX + facingDx(facing), worldY, worldZ + facingDz(facing)) == GameConfig.RED_BED
            && Blocks.isBedHead(world.getBlockState(worldX + facingDx(facing), worldY, worldZ + facingDz(facing)));
        boolean renderPillow = head || (state != null && state.data == 0 && !metadataFootPointsAtHead);
        boolean eastWest = facing == 1 || facing == 3;
        int blanket = packColor(0.78f * ambient, 0.10f * ambient, 0.10f * ambient, 1.0f);
        int frame = packColor(0.46f * ambient, 0.27f * ambient, 0.11f * ambient, 1.0f);
        int pillow = packColor(0.92f, 0.90f, 0.82f, 1.0f);

        appendCuboid(builder, x + 0.05, y + 0.10, z + 0.05, x + 0.95, y + 0.32, z + 0.95, frame);
        appendCuboid(builder, x + 0.04, y + 0.30, z + 0.04, x + 0.96, y + 0.58, z + 0.96, blanket);
        appendBedLegs(builder, x, y, z, frame, eastWest);
        if (renderPillow) {
            appendBedPillow(builder, x, y, z, facing, pillow);
        }
    }

    private void appendBedPillow(IntVertexBuilder builder, int x, int y, int z, int facing, int color) {
        switch (facing) {
            case 1:
                appendCuboid(builder, x + 0.60, y + 0.59, z + 0.16, x + 0.92, y + 0.72, z + 0.84, color);
                break;
            case 2:
                appendCuboid(builder, x + 0.16, y + 0.59, z + 0.60, x + 0.84, y + 0.72, z + 0.92, color);
                break;
            case 3:
                appendCuboid(builder, x + 0.08, y + 0.59, z + 0.16, x + 0.40, y + 0.72, z + 0.84, color);
                break;
            default:
                appendCuboid(builder, x + 0.16, y + 0.59, z + 0.08, x + 0.84, y + 0.72, z + 0.40, color);
                break;
        }
    }

    private void appendBedLegs(IntVertexBuilder builder, int x, int y, int z, int color, boolean eastWest) {
        if (eastWest) {
            appendCuboid(builder, x + 0.10, y, z + 0.12, x + 0.24, y + 0.30, z + 0.26, color);
            appendCuboid(builder, x + 0.10, y, z + 0.74, x + 0.24, y + 0.30, z + 0.88, color);
            appendCuboid(builder, x + 0.76, y, z + 0.12, x + 0.90, y + 0.30, z + 0.26, color);
            appendCuboid(builder, x + 0.76, y, z + 0.74, x + 0.90, y + 0.30, z + 0.88, color);
        } else {
            appendCuboid(builder, x + 0.12, y, z + 0.10, x + 0.26, y + 0.30, z + 0.24, color);
            appendCuboid(builder, x + 0.74, y, z + 0.10, x + 0.88, y + 0.30, z + 0.24, color);
            appendCuboid(builder, x + 0.12, y, z + 0.76, x + 0.26, y + 0.30, z + 0.90, color);
            appendCuboid(builder, x + 0.74, y, z + 0.76, x + 0.88, y + 0.30, z + 0.90, color);
        }
    }

    private void emitFenceBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ) {
        float ambient = world.getAmbientShade(worldX, worldY, worldZ);
        int color = packColor(0.56f * ambient, 0.38f * ambient, 0.20f * ambient, 1.0f);
        boolean west = shouldFenceConnect(worldX - 1, worldY, worldZ);
        boolean east = shouldFenceConnect(worldX + 1, worldY, worldZ);
        boolean north = shouldFenceConnect(worldX, worldY, worldZ - 1);
        boolean south = shouldFenceConnect(worldX, worldY, worldZ + 1);
        appendCuboid(builder, x + 0.36, y, z + 0.36, x + 0.64, y + 1.0, z + 0.64, color);
        if (west) {
            appendCuboid(builder, x, y + 0.58, z + 0.40, x + 0.40, y + 0.76, z + 0.60, color);
            appendCuboid(builder, x, y + 0.26, z + 0.40, x + 0.40, y + 0.44, z + 0.60, color);
        }
        if (east) {
            appendCuboid(builder, x + 0.60, y + 0.58, z + 0.40, x + 1.0, y + 0.76, z + 0.60, color);
            appendCuboid(builder, x + 0.60, y + 0.26, z + 0.40, x + 1.0, y + 0.44, z + 0.60, color);
        }
        if (north) {
            appendCuboid(builder, x + 0.40, y + 0.58, z, x + 0.60, y + 0.76, z + 0.40, color);
            appendCuboid(builder, x + 0.40, y + 0.26, z, x + 0.60, y + 0.44, z + 0.40, color);
        }
        if (south) {
            appendCuboid(builder, x + 0.40, y + 0.58, z + 0.60, x + 0.60, y + 0.76, z + 1.0, color);
            appendCuboid(builder, x + 0.40, y + 0.26, z + 0.60, x + 0.60, y + 0.44, z + 1.0, color);
        }
    }

    private boolean shouldFenceConnect(int worldX, int worldY, int worldZ) {
        byte block = world.getBlock(worldX, worldY, worldZ);
        return block == GameConfig.OAK_FENCE
            || block == GameConfig.OAK_FENCE_GATE
            || (world.isSolidBlock(block) && world.isCubeBlock(block) && !world.isTransparentBlock(block));
    }

    private void emitFenceGateBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ, BlockState state) {
        float ambient = world.getAmbientShade(worldX, worldY, worldZ);
        int color = packColor(0.56f * ambient, 0.38f * ambient, 0.20f * ambient, 1.0f);
        int facing = Blocks.gateFacing(state);
        boolean open = Blocks.isGateOpen(state);
        if (facing == 0 || facing == 2) {
            appendCuboid(builder, x + 0.08, y, z + 0.36, x + 0.26, y + 1.0, z + 0.64, color);
            appendCuboid(builder, x + 0.74, y, z + 0.36, x + 0.92, y + 1.0, z + 0.64, color);
            if (open) {
                return;
            }
            appendCuboid(builder, x + 0.20, y + 0.58, z + 0.42, x + 0.80, y + 0.76, z + 0.58, color);
            appendCuboid(builder, x + 0.20, y + 0.26, z + 0.42, x + 0.80, y + 0.44, z + 0.58, color);
        } else {
            appendCuboid(builder, x + 0.36, y, z + 0.08, x + 0.64, y + 1.0, z + 0.26, color);
            appendCuboid(builder, x + 0.36, y, z + 0.74, x + 0.64, y + 1.0, z + 0.92, color);
            if (open) {
                return;
            }
            appendCuboid(builder, x + 0.42, y + 0.58, z + 0.20, x + 0.58, y + 0.76, z + 0.80, color);
            appendCuboid(builder, x + 0.42, y + 0.26, z + 0.20, x + 0.58, y + 0.44, z + 0.80, color);
        }
    }

    private void emitStairBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ, byte block, BlockState state) {
        float ambient = world.getAmbientShade(worldX, worldY, worldZ);
        int color = colorForFace(block, Face.TOP, ambient, worldX, worldY, worldZ);
        int facing = Blocks.stairFacing(state);
        appendCuboid(builder, x, y, z, x + 1.0, y + 0.50, z + 1.0, color);
        switch (facing) {
            case 1:
                appendCuboid(builder, x + 0.50, y + 0.50, z, x + 1.0, y + 1.0, z + 1.0, color);
                break;
            case 2:
                appendCuboid(builder, x, y + 0.50, z + 0.50, x + 1.0, y + 1.0, z + 1.0, color);
                break;
            case 3:
                appendCuboid(builder, x, y + 0.50, z, x + 0.50, y + 1.0, z + 1.0, color);
                break;
            default:
                appendCuboid(builder, x, y + 0.50, z, x + 1.0, y + 1.0, z + 0.50, color);
                break;
        }
    }

    private void appendCuboid(IntVertexBuilder builder, double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ, int color) {
        appendQuad(builder, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        appendQuad(builder, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        appendQuad(builder, minX, maxY, maxZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        appendQuad(builder, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, color);
        appendQuad(builder, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, minX, minY, minZ, color);
        appendQuad(builder, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
    }

    private boolean isNearSpectatorCutoutBlock(int worldX, int worldY, int worldZ, MeshBuildContext context) {
        int dx = worldX - context.cameraBlockX;
        int dy = worldY - context.cameraBlockY;
        int dz = worldZ - context.cameraBlockZ;
        return dx * dx + dy * dy + dz * dz <= 5;
    }

    private void emitCrossPlant(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ, byte block) {
        if (block == GameConfig.TORCH) {
            emitTorchBlock(builder, x, y, z, worldX, worldY, worldZ);
            return;
        }
        int color = baseColorPacked(block, Face.TOP);
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        if (block == GameConfig.TALL_GRASS) {
            float amount = Math.min(0.82f, world.getColdSurfaceTint(worldX, worldY, worldZ));
            red = lerp(red, 0.48f, amount);
            green = lerp(green, 0.62f, amount);
            blue = lerp(blue, 0.54f, amount);
        }
        float ambient = world.getAmbientShade(worldX, worldY, worldZ);
        float alpha = (block == GameConfig.SEAGRASS || block == GameConfig.KELP) ? 0.72f : (block == GameConfig.TALL_GRASS ? 0.92f : 0.96f);
        int tintedColor = packColor(red * ambient, green * ambient, blue * ambient, alpha);

        boolean smallFlower = block == GameConfig.RED_FLOWER || block == GameConfig.YELLOW_FLOWER;
        boolean crop = block == GameConfig.WHEAT_CROP || block == GameConfig.CARROT_CROP || block == GameConfig.POTATO_CROP;
        boolean torch = block == GameConfig.TORCH;
        int vertexFlags = (block == GameConfig.TALL_GRASS || block == GameConfig.KELP) ? VERTEX_FLAG_FOLIAGE : 0;
        double halfWidth = torch ? 0.12 : (smallFlower ? 0.14 : (block == GameConfig.DEAD_BUSH ? 0.30 : (block == GameConfig.SEAGRASS ? 0.24 : (block == GameConfig.KELP ? 0.20 : (crop ? 0.30 : 0.42)))));
        double height = torch ? 0.76 : (block == GameConfig.SEAGRASS ? 0.58 : (block == GameConfig.KELP ? 0.96 : (block == GameConfig.DEAD_BUSH ? 0.52 : (smallFlower ? 0.68 : (crop ? cropRenderHeight(block, worldX, worldY, worldZ) : 0.98)))));
        if (block == GameConfig.KELP) {
            BlockState state = world.getBlockState(worldX, worldY, worldZ);
            int segment = state == null ? 1 : state.data;
            halfWidth = segment == 2 ? 0.24 : 0.17;
            height = segment == 2 ? 0.86 : 1.02;
        }
        double minX = x + 0.5 - halfWidth;
        double minY = y;
        double minZ = z + 0.5 - halfWidth;
        double maxX = x + 0.5 + halfWidth;
        double maxY = y + height;
        double maxZ = z + 0.5 + halfWidth;

        appendQuad(builder, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, tintedColor, AO_FULL_BRIGHT_PACKED, vertexFlags);
        appendQuad(builder, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, tintedColor, AO_FULL_BRIGHT_PACKED, vertexFlags);
        appendQuad(builder, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, tintedColor, AO_FULL_BRIGHT_PACKED, vertexFlags);
        appendQuad(builder, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, minZ, maxX, minY, minZ, tintedColor, AO_FULL_BRIGHT_PACKED, vertexFlags);
        if (smallFlower) {
            int stem = packColor(0.18f * ambient, 0.50f * ambient, 0.16f * ambient, 0.96f);
            int bloom = packColor(red * ambient, green * ambient, blue * ambient, 0.98f);
            appendQuad(builder, x + 0.47, y, z + 0.50, x + 0.47, y + 0.48, z + 0.50, x + 0.53, y + 0.48, z + 0.50, x + 0.53, y, z + 0.50, stem, AO_FULL_BRIGHT_PACKED, vertexFlags);
            appendQuad(builder, x + 0.25, y + 0.44, z + 0.50, x + 0.25, y + 0.70, z + 0.50, x + 0.75, y + 0.70, z + 0.50, x + 0.75, y + 0.44, z + 0.50, bloom, AO_FULL_BRIGHT_PACKED, vertexFlags);
            appendQuad(builder, x + 0.50, y + 0.44, z + 0.25, x + 0.50, y + 0.70, z + 0.25, x + 0.50, y + 0.70, z + 0.75, x + 0.50, y + 0.44, z + 0.75, bloom, AO_FULL_BRIGHT_PACKED, vertexFlags);
        }
    }

    private double cropRenderHeight(byte block, int worldX, int worldY, int worldZ) {
        BlockState state = world.getBlockState(worldX, worldY, worldZ);
        int stage = state == null ? 7 : Math.max(0, Math.min(7, state.data));
        double matureHeight = block == GameConfig.CARROT_CROP || block == GameConfig.POTATO_CROP ? 0.58 : 0.78;
        return 0.22 + (matureHeight - 0.22) * (stage / 7.0);
    }

    private void emitTorchBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ) {
        float ambient = Math.max(0.82f, world.getAmbientShade(worldX, worldY, worldZ));
        int wood = packColor(0.50f * ambient, 0.32f * ambient, 0.16f * ambient, 1.0f);
        int flame = packColor(1.0f, 0.72f, 0.18f, 1.0f);
        double cx = x + 0.5;
        double cz = z + 0.5;
        appendCuboid(builder, cx - 0.055, y, cz - 0.055, cx + 0.055, y + 0.62, cz + 0.055, wood);
        appendCuboid(builder, cx - 0.105, y + 0.58, cz - 0.105, cx + 0.105, y + 0.82, cz + 0.105, flame);
    }

    private void emitParadisePortalBlock(IntVertexBuilder builder, int x, int y, int z, int worldX, int worldY, int worldZ) {
        float shade = Settings.goodGraphics() ? world.getAmbientShade(worldX, worldY, worldZ) : 1.0f;
        int color = colorForFace(GameConfig.PARADISE_PORTAL, Face.NORTH, shade, worldX, worldY, worldZ);
        double inset = (1.0 - GameConfig.PARADISE_PORTAL_RENDER_THICKNESS) * 0.5;
        double minX = x + inset;
        double maxX = x + 1.0 - inset;
        double minZ = z + inset;
        double maxZ = z + 1.0 - inset;
        appendQuad(builder, minX, y, z + 0.5, minX, y + 1.0, z + 0.5, maxX, y + 1.0, z + 0.5, maxX, y, z + 0.5, color);
        appendQuad(builder, x + 0.5, y, minZ, x + 0.5, y + 1.0, minZ, x + 0.5, y + 1.0, maxZ, x + 0.5, y, maxZ, color);
    }

    private void appendQuad(IntVertexBuilder builder,
                            double x1, double y1, double z1,
                            double x2, double y2, double z2,
                            double x3, double y3, double z3,
                            double x4, double y4, double z4,
                            int color) {
        appendQuad(builder, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, AO_FULL_BRIGHT_PACKED);
    }

    private void appendQuad(IntVertexBuilder builder,
                            double x1, double y1, double z1,
                            double x2, double y2, double z2,
                            double x3, double y3, double z3,
                            double x4, double y4, double z4,
                            int color, int ao) {
        appendQuad(builder, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, ao, 0);
    }

    private void appendQuad(IntVertexBuilder builder,
                            double x1, double y1, double z1,
                            double x2, double y2, double z2,
                            double x3, double y3, double z3,
                            double x4, double y4, double z4,
                            int color, int ao, int vertexFlags) {
        int ao0 = ao & 3;
        int ao1 = (ao >>> 2) & 3;
        int ao2 = (ao >>> 4) & 3;
        int ao3 = (ao >>> 6) & 3;
        double upperY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        int flags1 = vertexShaderFlags(vertexFlags, y1, upperY);
        int flags2 = vertexShaderFlags(vertexFlags, y2, upperY);
        int flags3 = vertexShaderFlags(vertexFlags, y3, upperY);
        int flags4 = vertexShaderFlags(vertexFlags, y4, upperY);
        if ((ao & AO_FLIP_DIAGONAL) == 0) {
            appendVertex(builder, x1, y1, z1, color, ao0, flags1);
            appendVertex(builder, x2, y2, z2, color, ao1, flags2);
            appendVertex(builder, x3, y3, z3, color, ao2, flags3);
            appendVertex(builder, x1, y1, z1, color, ao0, flags1);
            appendVertex(builder, x3, y3, z3, color, ao2, flags3);
            appendVertex(builder, x4, y4, z4, color, ao3, flags4);
        } else {
            appendVertex(builder, x2, y2, z2, color, ao1, flags2);
            appendVertex(builder, x3, y3, z3, color, ao2, flags3);
            appendVertex(builder, x4, y4, z4, color, ao3, flags4);
            appendVertex(builder, x1, y1, z1, color, ao0, flags1);
            appendVertex(builder, x2, y2, z2, color, ao1, flags2);
            appendVertex(builder, x4, y4, z4, color, ao3, flags4);
        }
    }

    private int vertexShaderFlags(int baseFlags, double y, double upperY) {
        if ((baseFlags & VERTEX_FLAG_FOLIAGE) == 0) {
            return 0;
        }
        return Math.abs(y - upperY) < 0.0001 ? baseFlags | VERTEX_FLAG_UPPER : baseFlags;
    }

    private void appendVertex(IntVertexBuilder builder, double x, double y, double z, int color, int ao, int vertexFlags) {
        builder.putPacked(x, y, z, encodeVertexShaderFlags(color, vertexFlags), ao);
    }

    private int encodeVertexShaderFlags(int color, int vertexFlags) {
        if ((vertexFlags & VERTEX_FLAG_FOLIAGE) == 0) {
            return color;
        }
        int encodedFlags = (vertexFlags & VERTEX_FLAG_UPPER) != 0 ? 1 : 0;
        int alpha = color & 0xFF;
        if (alpha >= 252) {
            return (color & 0xFFFFFF00) | OPAQUE_SHADER_ALPHA_BASE | encodedFlags;
        }
        if (alpha >= 220) {
            return (color & 0xFFFFFF00) | TRANSLUCENT_SHADER_ALPHA_BASE | encodedFlags;
        }
        return color;
    }

    private int packUnitFaceAo(ChunkSectionSnapshot chunk, int startX, int startY, int startZ,
                               int worldX, int worldY, int worldZ, Face face) {
        switch (face) {
            case WEST:
                return packAo(
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, 1)
                );
            case EAST:
                return packAo(
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, -1)
                );
            case TOP:
                return packAo(
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, 1)
                );
            case BOTTOM:
                return packAo(
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, -1)
                );
            case NORTH:
                return packAo(
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, -1)
                );
            case SOUTH:
                return packAo(
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, -1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, -1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, 1),
                    vertexAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face, 1, -1)
                );
            default:
                return AO_FULL_BRIGHT_PACKED;
        }
    }

    private int packGreedyFaceAo(ChunkSectionSnapshot chunk, int startX, int startY, int startZ,
                                 Face face, int layer, int u, int v, int height, int width) {
        int uMax = u + height - 1;
        int vMax = v + width - 1;
        switch (face) {
            case WEST:
                return packAo(
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, u, v, -1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, uMax, v, 1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, uMax, vMax, 1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, u, vMax, -1, 1)
                );
            case EAST:
                return packAo(
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, u, vMax, -1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, uMax, vMax, 1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, uMax, v, 1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, layer, u, v, -1, -1)
                );
            case TOP:
                return packAo(
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, layer, vMax, -1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, layer, v, -1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, layer, v, 1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, layer, vMax, 1, 1)
                );
            case BOTTOM:
                return packAo(
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, layer, v, -1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, layer, vMax, -1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, layer, vMax, 1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, layer, v, 1, -1)
                );
            case NORTH:
                return packAo(
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, v, layer, 1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, vMax, layer, 1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, vMax, layer, -1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, v, layer, -1, -1)
                );
            case SOUTH:
                return packAo(
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, v, layer, -1, -1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, u, vMax, layer, -1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, vMax, layer, 1, 1),
                    vertexAoLocal(chunk, startX, startY, startZ, face, uMax, v, layer, 1, -1)
                );
            default:
                return AO_FULL_BRIGHT_PACKED;
        }
    }

    private int vertexAoLocal(ChunkSectionSnapshot chunk, int startX, int startY, int startZ,
                              Face face, int localX, int localY, int localZ, int signU, int signV) {
        return vertexAo(chunk, startX, startY, startZ, startX + localX, startY + localY, startZ + localZ, face, signU, signV);
    }

    private int vertexAo(ChunkSectionSnapshot chunk, int startX, int startY, int startZ,
                         int worldX, int worldY, int worldZ, Face face, int signU, int signV) {
        int nx = 0;
        int ny = 0;
        int nz = 0;
        int ux = 0;
        int uy = 0;
        int uz = 0;
        int vx = 0;
        int vy = 0;
        int vz = 0;
        switch (face) {
            case WEST:
                nx = -1; uy = 1; vz = 1;
                break;
            case EAST:
                nx = 1; uy = 1; vz = 1;
                break;
            case TOP:
                ny = 1; ux = 1; vz = 1;
                break;
            case BOTTOM:
                ny = -1; ux = 1; vz = 1;
                break;
            case NORTH:
                nz = -1; ux = 1; vy = 1;
                break;
            case SOUTH:
                nz = 1; ux = 1; vy = 1;
                break;
            default:
                return 3;
        }

        int bx = worldX + nx;
        int by = worldY + ny;
        int bz = worldZ + nz;
        int side1 = isAoOccluder(chunk, startX, startY, startZ, bx + ux * signU, by + uy * signU, bz + uz * signU) ? 1 : 0;
        int side2 = isAoOccluder(chunk, startX, startY, startZ, bx + vx * signV, by + vy * signV, bz + vz * signV) ? 1 : 0;
        if ((side1 & side2) != 0) {
            return 0;
        }
        int corner = isAoOccluder(chunk, startX, startY, startZ,
            bx + ux * signU + vx * signV,
            by + uy * signU + vy * signV,
            bz + uz * signU + vz * signV) ? 1 : 0;
        return 3 - side1 - side2 - corner;
    }

    private boolean isAoOccluder(ChunkSectionSnapshot chunk, int startX, int startY, int startZ, int worldX, int worldY, int worldZ) {
        int localX = worldX - startX;
        int localY = worldY - startY;
        int localZ = worldZ - startZ;
        byte block = GameConfig.isChunkLocalCoordinateInside(localX, localY, localZ)
            ? chunk.getBlockLocal(localX, localY, localZ)
            : world.getBlock(worldX, worldY, worldZ);
        return block != GameConfig.AIR && Blocks.isOpaque(block) && world.isCubeBlock(block);
    }

    private int packAo(int ao0, int ao1, int ao2, int ao3) {
        int packed = (ao0 & 3) | ((ao1 & 3) << 2) | ((ao2 & 3) << 4) | ((ao3 & 3) << 6);
        return ao0 + ao2 > ao1 + ao3 ? packed | AO_FLIP_DIAGONAL : packed;
    }

    private int colorForFace(byte block, Face face, float ambientShade, int worldX, int worldY, int worldZ) {
        int color = baseColorPacked(block, face);
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        if (block == GameConfig.GRASS && face == Face.TOP) {
            float cold = Math.min(0.82f, world.getColdSurfaceTint(worldX, worldY, worldZ));
            red = lerp(red, 0.48f, cold);
            green = lerp(green, 0.62f, cold);
            blue = lerp(blue, 0.54f, cold);
            float taiga = Math.min(0.75f, world.getTaigaSurfaceTint(worldX, worldZ));
            red = lerp(red, 0.22f, taiga);
            green = lerp(green, 0.46f, taiga);
            blue = lerp(blue, 0.24f, taiga);
        }
        float brightness = face.brightness * ambientShade;
        if (block == GameConfig.OAK_LEAVES || block == GameConfig.PINE_LEAVES || block == GameConfig.BIRCH_LEAVES) {
            brightness = Math.max(0.82f, face.brightness * (0.90f + ambientShade * 0.10f));
        } else if (block == GameConfig.SNOW_LAYER) {
            brightness *= 0.88f;
        }
        float alpha = 1.0f;
        if (block == GameConfig.GLASS) {
            alpha = 0.36f;
        } else if (block == GameConfig.SEAGRASS || block == GameConfig.KELP) {
            alpha = 0.72f;
        } else if (GameConfig.isWaterBlock(block)) {
            alpha = 0.60f;
        } else if (GameConfig.isLavaBlock(block)) {
            alpha = 0.85f;
        } else if (block == GameConfig.PARADISE_PORTAL) {
            alpha = 0.62f;
            brightness = Math.max(1.08f, brightness);
        }
        return packColor(red * brightness, green * brightness, blue * brightness, alpha);
    }

    private int baseColorPacked(byte block, Face face) {
        switch (block) {
            case GameConfig.GRASS:
                return face == Face.TOP ? packColor(0.37f, 0.76f, 0.34f, 1.0f) : packColor(0.50f, 0.36f, 0.24f, 1.0f);
            case GameConfig.DIRT:
                return packColor(0.50f, 0.36f, 0.24f, 1.0f);
            case GameConfig.SAND:
                return packColor(0.84f, 0.77f, 0.51f, 1.0f);
            case GameConfig.GRAVEL:
                return packColor(0.44f, 0.45f, 0.43f, 1.0f);
            case GameConfig.CLAY:
                return packColor(0.58f, 0.66f, 0.68f, 1.0f);
            case GameConfig.STONE:
                return packColor(0.58f, 0.58f, 0.60f, 1.0f);
            case GameConfig.DEEPSLATE:
                return packColor(0.28f, 0.29f, 0.32f, 1.0f);
            case GameConfig.COBBLESTONE:
                return packColor(0.48f, 0.48f, 0.48f, 1.0f);
            case GameConfig.OBSIDIAN:
                return packColor(0.12f, 0.08f, 0.18f, 1.0f);
            case GameConfig.BEDROCK:
                return packColor(0.17f, 0.17f, 0.17f, 1.0f);
            case GameConfig.IRON_ORE:
            case GameConfig.DIAMOND_ORE:
            case GameConfig.COAL_ORE:
                return packColor(0.58f, 0.58f, 0.60f, 1.0f);
            case GameConfig.DEEPSLATE_IRON_ORE:
            case GameConfig.DEEPSLATE_DIAMOND_ORE:
            case GameConfig.DEEPSLATE_COAL_ORE:
                return packColor(0.28f, 0.29f, 0.32f, 1.0f);
            case GameConfig.OAK_LOG:
                return face == Face.TOP || face == Face.BOTTOM ? packColor(0.66f, 0.54f, 0.31f, 1.0f) : packColor(0.54f, 0.38f, 0.20f, 1.0f);
            case GameConfig.OAK_LEAVES:
                return packColor(0.30f, 0.58f, 0.24f, 1.0f);
            case GameConfig.PINE_LOG:
                return face == Face.TOP || face == Face.BOTTOM ? packColor(0.58f, 0.48f, 0.30f, 1.0f) : packColor(0.38f, 0.27f, 0.16f, 1.0f);
            case GameConfig.PINE_LEAVES:
                return packColor(0.14f, 0.38f, 0.18f, 1.0f);
            case GameConfig.BIRCH_LOG:
                return face == Face.TOP || face == Face.BOTTOM ? packColor(0.78f, 0.70f, 0.50f, 1.0f) : packColor(0.78f, 0.76f, 0.68f, 1.0f);
            case GameConfig.BIRCH_LEAVES:
                return packColor(0.44f, 0.68f, 0.25f, 1.0f);
            case GameConfig.SNOW_BLOCK:
                return packColor(0.86f, 0.91f, 0.95f, 1.0f);
            case GameConfig.SNOW_LAYER:
                return packColor(0.78f, 0.86f, 0.92f, 1.0f);
            case GameConfig.CACTUS:
                return face == Face.TOP || face == Face.BOTTOM ? packColor(0.38f, 0.62f, 0.25f, 1.0f) : packColor(0.22f, 0.54f, 0.20f, 1.0f);
            case GameConfig.DEAD_BUSH:
                return packColor(0.46f, 0.30f, 0.16f, 1.0f);
            case GameConfig.TALL_GRASS:
                return packColor(0.42f, 0.77f, 0.31f, 1.0f);
            case GameConfig.SEAGRASS:
                return packColor(0.18f, 0.55f, 0.36f, 1.0f);
            case GameConfig.KELP:
                return packColor(0.12f, 0.42f, 0.20f, 1.0f);
            case GameConfig.RED_FLOWER:
                return packColor(0.91f, 0.18f, 0.22f, 1.0f);
            case GameConfig.YELLOW_FLOWER:
                return packColor(0.95f, 0.82f, 0.20f, 1.0f);
            case InventoryItems.OAK_PLANKS:
            case GameConfig.OAK_STAIRS:
                return packColor(0.73f, 0.58f, 0.34f, 1.0f);
            case GameConfig.PINE_PLANKS:
            case GameConfig.PINE_STAIRS:
                return packColor(0.50f, 0.34f, 0.18f, 1.0f);
            case GameConfig.BIRCH_PLANKS:
            case GameConfig.BIRCH_STAIRS:
                return packColor(0.82f, 0.72f, 0.46f, 1.0f);
            case GameConfig.STONE_STAIRS:
                return packColor(0.58f, 0.58f, 0.60f, 1.0f);
            case GameConfig.COBBLESTONE_STAIRS:
                return packColor(0.48f, 0.48f, 0.48f, 1.0f);
            case GameConfig.OAK_FENCE:
            case GameConfig.OAK_FENCE_GATE:
                return packColor(0.56f, 0.38f, 0.20f, 1.0f);
            case GameConfig.CHEST:
                return packColor(0.62f, 0.39f, 0.17f, 1.0f);
            case GameConfig.CRAFTING_TABLE:
                return face == Face.TOP ? packColor(0.63f, 0.46f, 0.25f, 1.0f) : packColor(0.48f, 0.31f, 0.16f, 1.0f);
            case GameConfig.FURNACE:
                return face == Face.NORTH ? packColor(0.40f, 0.40f, 0.38f, 1.0f) : packColor(0.33f, 0.33f, 0.32f, 1.0f);
            case GameConfig.GLASS:
                return packColor(0.72f, 0.90f, 0.95f, 1.0f);
            case GameConfig.PARADISE_PORTAL:
                return packColor(0.56f, 0.88f, 1.00f, 1.0f);
            case GameConfig.WHEAT_CROP:
                return packColor(0.86f, 0.72f, 0.26f, 1.0f);
            case GameConfig.CARROT_CROP:
                return packColor(0.28f, 0.64f, 0.20f, 1.0f);
            case GameConfig.POTATO_CROP:
                return packColor(0.38f, 0.58f, 0.24f, 1.0f);
            case GameConfig.RAIL:
                return packColor(0.58f, 0.50f, 0.42f, 1.0f);
            case GameConfig.OAK_DOOR:
                return packColor(0.50f, 0.30f, 0.13f, 1.0f);
            case GameConfig.FARMLAND:
                return face == Face.TOP ? packColor(0.36f, 0.24f, 0.14f, 1.0f) : packColor(0.44f, 0.31f, 0.20f, 1.0f);
            case GameConfig.TORCH:
                return packColor(0.96f, 0.72f, 0.24f, 1.0f);
            case GameConfig.RED_BED:
                return face == Face.TOP ? packColor(0.78f, 0.12f, 0.12f, 1.0f) : packColor(0.76f, 0.62f, 0.38f, 1.0f);
            case GameConfig.STRUCTURE_BLOCK:
                return face == Face.TOP ? packColor(0.70f, 0.55f, 0.86f, 1.0f) : packColor(0.44f, 0.34f, 0.58f, 1.0f);
            case GameConfig.WATER:
            case GameConfig.WATER_SOURCE:
            case GameConfig.WATER_FLOWING:
                return packColor(0.35f, 0.55f, 0.90f, 1.0f);
            case GameConfig.LAVA:
            case GameConfig.LAVA_SOURCE:
            case GameConfig.LAVA_FLOWING:
                return packColor(0.92f, 0.42f, 0.12f, 1.0f);
            case GameConfig.ZOMBIE_SKIN:
                return packColor(0.38f, 0.69f, 0.33f, 1.0f);
            case GameConfig.ZOMBIE_SHIRT:
                return packColor(0.23f, 0.51f, 0.57f, 1.0f);
            case GameConfig.ZOMBIE_PANTS:
                return packColor(0.25f, 0.27f, 0.53f, 1.0f);
            case GameConfig.ZOMBIE_EYE:
                return packColor(0.08f, 0.08f, 0.08f, 1.0f);
            default:
                return packColor(1.0f, 0.0f, 1.0f, 1.0f);
        }
    }

    private int packColor(float red, float green, float blue, float alpha) {
        int r = Math.max(0, Math.min(255, Math.round(clampColor(red) * 255.0f)));
        int g = Math.max(0, Math.min(255, Math.round(clampColor(green) * 255.0f)));
        int b = Math.max(0, Math.min(255, Math.round(clampColor(blue) * 255.0f)));
        int a = Math.max(0, Math.min(255, Math.round(clampColor(alpha) * 255.0f)));
        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    private int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(clampColor(alpha) * 255.0f)));
        return (color & 0xFFFFFF00) | a;
    }

    private float red(int color) {
        return ((color >>> 24) & 0xFF) / 255.0f;
    }

    private float green(int color) {
        return ((color >>> 16) & 0xFF) / 255.0f;
    }

    private float blue(int color) {
        return ((color >>> 8) & 0xFF) / 255.0f;
    }

    private GpuMeshBuffer uploadChunkBuffer(int[] vertices, int intCount) {
        if (intCount == 0) {
            return GpuMeshBuffer.EMPTY;
        }
        int vertexCount = intCount / INTS_PER_VERTEX;
        IntBuffer buffer = BufferUtils.createIntBuffer(vertexCount * GPU_INTS_PER_VERTEX);
        for (int index = 0; index < intCount; index += INTS_PER_VERTEX) {
            buffer.put(vertices[index]);
            buffer.put(vertices[index + 1]);
            buffer.put(0);
        }
        buffer.flip();
        return uploadGpuBuffer(buffer, vertexCount, "stage chunk mesh buffer");
    }

    private GpuMeshBuffer uploadGpuBuffer(IntBuffer buffer, int vertexCount, String verificationStep) {
        if (vertexCount == 0) {
            return GpuMeshBuffer.EMPTY;
        }
        int vboId = glGenBuffers();
        if (vboId == 0) {
            throw new IllegalStateException("Failed to allocate mesh VBO");
        }
        int vaoId = 0;
        boolean uploaded = false;
        try {
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            vaoId = createVertexArrayObject();
            if (vaoId != 0) {
                bindVertexArrayObject(vaoId);
                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glEnableVertexAttribArray(0);
                glVertexAttribIPointer(0, GPU_INTS_PER_VERTEX, GL_UNSIGNED_INT, GPU_BYTES_PER_VERTEX, 0L);
                bindVertexArrayObject(0);
                glBindBuffer(GL_ARRAY_BUFFER, 0);
            }
            verifyOpenGl(verificationStep);
            uploaded = true;
            return new GpuMeshBuffer(vaoId, vboId, vertexCount);
        } finally {
            if (!uploaded) {
                glBindBuffer(GL_ARRAY_BUFFER, 0);
                if (vaoId != 0) {
                    deleteVertexArrayObject(vaoId);
                }
                glDeleteBuffers(vboId);
            }
        }
    }

    static boolean isWaterloggedPlantForMesh(byte block) {
        return block == GameConfig.SEAGRASS || block == GameConfig.KELP;
    }

    private void emitWaterloggedPlantWater(ChunkSectionSnapshot sourceChunk, IntVertexBuilder builder,
                                            int x, int y, int z, int startX, int startY, int startZ,
                                            int worldX, int worldY, int worldZ) {
        byte water = GameConfig.WATER_SOURCE;
        if (waterRenderer.isRenderableBlockFace(water, worldX, worldY, worldZ, Face.WEST)) {
            emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, Face.WEST, water);
        }
        if (waterRenderer.isRenderableBlockFace(water, worldX, worldY, worldZ, Face.EAST)) {
            emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, Face.EAST, water);
        }
        if (waterRenderer.isRenderableBlockFace(water, worldX, worldY, worldZ, Face.TOP)) {
            emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, Face.TOP, water);
        }
        if (waterRenderer.isRenderableBlockFace(water, worldX, worldY, worldZ, Face.BOTTOM)) {
            emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, Face.BOTTOM, water);
        }
        if (waterRenderer.isRenderableBlockFace(water, worldX, worldY, worldZ, Face.NORTH)) {
            emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, Face.NORTH, water);
        }
        if (waterRenderer.isRenderableBlockFace(water, worldX, worldY, worldZ, Face.SOUTH)) {
            emitBlockFace(sourceChunk, builder, x, y, z, startX, startY, startZ, worldX, worldY, worldZ, Face.SOUTH, water);
        }
    }

    private void drawChunkBuffer(ChunkMesh mesh, int vaoId, int vboId, int vertexCount) {
        glUniform3f(
            chunkShaderOriginLocation,
            (float) cameraRelativeChunkOrigin(mesh.chunkX, renderCameraX),
            (float) (GameConfig.sectionYForIndex(mesh.chunkY) - renderCameraY),
            (float) cameraRelativeChunkOrigin(mesh.chunkZ, renderCameraZ)
        );

        if (vaoId != 0) {
            drawGpuBuffer(vaoId, vboId, vertexCount);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glEnableVertexAttribArray(0);
            glVertexAttribIPointer(0, GPU_INTS_PER_VERTEX, GL_UNSIGNED_INT, GPU_BYTES_PER_VERTEX, 0L);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glDisableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
    }

    private void unloadChunkMesh(ChunkMesh mesh) {
        deleteMeshBuffer(mesh.opaqueVaoId, mesh.opaqueVboId);
        deleteMeshBuffer(mesh.transparentVaoId, mesh.transparentVboId);
        mesh.opaqueVaoId = 0;
        mesh.opaqueVboId = 0;
        mesh.opaqueVertexCount = 0;
        mesh.opaqueVertices = new int[0];
        mesh.opaqueIntCount = 0;
        mesh.transparentVaoId = 0;
        mesh.transparentVboId = 0;
        mesh.transparentVertexCount = 0;
        mesh.hasOpaqueGeometry = false;
        mesh.hasTransparentGeometry = false;
        mesh.resident = false;
    }

    private void dropChunkMesh(long meshKey) {
        ChunkMesh mesh = chunkMeshes.remove(meshKey);
        if (mesh != null) {
            markOpaqueRegionDirty(mesh);
            unloadChunkMesh(mesh);
        }
        dirtyChunkMeshes.remove(meshKey);
        immediateChunkMeshes.remove(meshKey);
        pendingDirtyMeshBuildKeys.remove(meshKey);
        pendingDirtyMeshBuilds.removeIf(key -> key.longValue() == meshKey);
        queuedMeshBuildKeys.remove(meshKey);
        meshBuildCooldownUntilNanos.remove(meshKey);
        meshBuildsReady.remove(meshKey);
        meshBuildRevisions.forget(meshKey);
    }

    private void clearOpaqueRenderRegions() {
        for (OpaqueRenderRegion region : opaqueRenderRegions.values()) {
            deleteMeshBuffer(region.vaoId, region.vboId);
        }
        opaqueRenderRegions.clear();
        pendingOpaqueRegionBuilds.clear();
        pendingOpaqueRegionBuildKeys.clear();
        opaqueRegionBuildsInFlight = 0;
    }

    private void pruneDistantMeshBookkeeping(int playerChunkX, int playerChunkZ, int unloadChunkRadius) {
        pruneMeshKeySet(dirtyChunkMeshes, playerChunkX, playerChunkZ, unloadChunkRadius);
        pruneMeshKeySet(immediateChunkMeshes, playerChunkX, playerChunkZ, unloadChunkRadius);
        pruneMeshKeySet(pendingDirtyMeshBuildKeys, playerChunkX, playerChunkZ, unloadChunkRadius);
        pendingDirtyMeshBuilds.removeIf(key -> shouldPruneMeshKey(key, playerChunkX, playerChunkZ, unloadChunkRadius));
        pruneMeshKeySet(queuedMeshBuildKeys, playerChunkX, playerChunkZ, unloadChunkRadius);
        pruneMeshKeyMap(meshBuildCooldownUntilNanos, playerChunkX, playerChunkZ, unloadChunkRadius);
        pruneMeshKeyMap(meshBuildsReady, playerChunkX, playerChunkZ, unloadChunkRadius);
        meshBuildRevisions.forgetIf(key -> shouldPruneMeshKey(key, playerChunkX, playerChunkZ, unloadChunkRadius));
    }

    private void pruneMeshKeySet(HashSet<Long> keys, int playerChunkX, int playerChunkZ, int unloadChunkRadius) {
        Iterator<Long> iterator = keys.iterator();
        while (iterator.hasNext()) {
            long key = iterator.next();
            if (shouldPruneMeshKey(key, playerChunkX, playerChunkZ, unloadChunkRadius)) {
                iterator.remove();
            }
        }
    }

    private void pruneMeshKeyMap(ConcurrentHashMap<Long, ?> keys, int playerChunkX, int playerChunkZ, int unloadChunkRadius) {
        for (Long key : keys.keySet()) {
            if (shouldPruneMeshKey(key, playerChunkX, playerChunkZ, unloadChunkRadius)) {
                keys.remove(key);
            }
        }
    }

    private boolean shouldPruneMeshKey(long key, int playerChunkX, int playerChunkZ, int unloadChunkRadius) {
        int chunkX = chunkXFromKey(key);
        int chunkZ = chunkZFromKey(key);
        int horizontalDistance = Math.max(Math.abs(chunkX - playerChunkX), Math.abs(chunkZ - playerChunkZ));
        return horizontalDistance > unloadChunkRadius || !world.isChunkLoaded(chunkX, chunkZ);
    }

    private void deleteMeshBuffer(int vaoId, int vboId) {
        if (vaoId != 0) {
            deleteVertexArrayObject(vaoId);
        }
        if (vboId != 0) {
            glDeleteBuffers(vboId);
        }
    }

    private int createVertexArrayObject() {
        if (!vaoSupported) {
            return 0;
        }
        if (GL.getCapabilities().OpenGL30) {
            return org.lwjgl.opengl.GL30.glGenVertexArrays();
        }
        return org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays();
    }

    private void bindVertexArrayObject(int vaoId) {
        if (!vaoSupported) {
            return;
        }
        if (GL.getCapabilities().OpenGL30) {
            org.lwjgl.opengl.GL30.glBindVertexArray(vaoId);
        } else {
            org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray(vaoId);
        }
    }

    private void deleteVertexArrayObject(int vaoId) {
        if (!vaoSupported || vaoId == 0) {
            return;
        }
        if (GL.getCapabilities().OpenGL30) {
            org.lwjgl.opengl.GL30.glDeleteVertexArrays(vaoId);
        } else {
            org.lwjgl.opengl.ARBVertexArrayObject.glDeleteVertexArrays(vaoId);
        }
    }

    private int uploadReadyChunkMeshes(int uploadBudget, int playerChunkX, int playerChunkZ, int unloadChunkRadius) {
        int readyBacklog = completedImmediateMeshBuilds.size() + completedMeshBuilds.size();
        int totalBudget = Math.min(MAX_CHUNK_UPLOADS_PER_FRAME, Math.max(0, uploadBudget));
        long uploadStartNanos = System.nanoTime();
        long uploadDeadlineNanos = uploadStartNanos + dynamicMeshUploadTimeBudgetNanos(readyBacklog);
        int immediateBudget = completedImmediateMeshBuilds.isEmpty()
            ? 0
            : Math.min(MAX_IMMEDIATE_CHUNK_UPLOADS_PER_FRAME, totalBudget);
        int immediateUploaded = uploadReadyChunkMeshQueue(completedImmediateMeshBuilds, immediateBudget, uploadDeadlineNanos, playerChunkX, playerChunkZ, unloadChunkRadius);
        int regularBudget = Math.max(0, totalBudget - immediateUploaded);
        int regularUploaded = uploadReadyChunkMeshQueue(completedMeshBuilds, regularBudget, uploadDeadlineNanos, playerChunkX, playerChunkZ, unloadChunkRadius);
        int uploaded = immediateUploaded + regularUploaded;
        debugMeshUploadNanosLastFrame = System.nanoTime() - uploadStartNanos;
        if (uploaded > 0) {
            verifyOpenGl("uploadReadyChunkMeshes");
        }
        return uploaded;
    }

    private long dynamicMeshUploadTimeBudgetNanos(int readyBacklog) {
        if (debugFps > 0.0 && debugFps < 45.0) {
            return LOW_FPS_MESH_UPLOAD_BUDGET_NANOS;
        }
        if (readyBacklog > 500 && (debugFps >= 45.0 || debugFps <= 0.0)) {
            return BACKLOG_MESH_UPLOAD_BUDGET_NANOS;
        }
        return NORMAL_MESH_UPLOAD_BUDGET_NANOS;
    }

    private int uploadReadyChunkMeshQueue(ConcurrentLinkedQueue<MeshBuildResult> queue, int uploadBudget, long uploadDeadlineNanos, int playerChunkX, int playerChunkZ, int unloadChunkRadius) {
        int uploaded = 0;
        while (uploaded < uploadBudget && System.nanoTime() < uploadDeadlineNanos) {
            MeshBuildResult result = queue.poll();
            if (result == null) {
                break;
            }
            meshBuildsReady.remove(result.meshKey);
            if (result.epoch != meshBuildEpoch) {
                continue;
            }
            if (!result.readyForUpload) {
                continue;
            }
            int horizontalDistance = Math.max(Math.abs(result.chunkX - playerChunkX), Math.abs(result.chunkZ - playerChunkZ));
            if (!world.isChunkLoaded(result.chunkX, result.chunkZ) || horizontalDistance > unloadChunkRadius) {
                dropChunkMesh(result.meshKey);
                continue;
            }
            ChunkSectionSnapshot current = world.getChunkSnapshot(result.chunkX, result.chunkY, result.chunkZ);
            if (current == null) {
                dropChunkMesh(result.meshKey);
                continue;
            }
            int currentVersion = current == null ? -1 : current.version;
            if (currentVersion != result.version || !meshBuildRevisions.isCurrent(result.meshKey, result.buildRevision)) {
                dirtyChunkMeshes.add(result.meshKey);
                immediateChunkMeshes.add(result.meshKey);
                enqueueDirtyMeshBuild(result.meshKey);
                meshBuildCooldownUntilNanos.remove(result.meshKey);
                continue;
            }

            ChunkMesh mesh = chunkMeshes.get(result.meshKey);
            if (result.empty) {
                dropChunkMesh(result.meshKey);
                uploaded++;
                continue;
            }
            if (mesh == null) {
                mesh = new ChunkMesh(result.chunkX, result.chunkY, result.chunkZ);
                mesh.revealStartNanos = System.nanoTime();
                chunkMeshes.put(result.meshKey, mesh);
            }
            boolean opaqueRegionWasRenderable = isOpaqueRegionRenderable(mesh);
            GpuMeshBuffer preparedOpaque = GpuMeshBuffer.EMPTY;
            GpuMeshBuffer preparedTransparent = GpuMeshBuffer.EMPTY;
            try {
                if (!opaqueRegionWasRenderable) {
                    preparedOpaque = uploadChunkBuffer(result.opaqueVertices, result.opaqueIntCount);
                }
                preparedTransparent = uploadChunkBuffer(result.transparentVertices, result.transparentIntCount);
            } catch (RuntimeException uploadFailure) {
                deleteMeshBuffer(preparedOpaque.vaoId, preparedOpaque.vboId);
                deleteMeshBuffer(preparedTransparent.vaoId, preparedTransparent.vboId);
                dirtyChunkMeshes.add(result.meshKey);
                enqueueDirtyMeshBuild(result.meshKey);
                meshBuildCooldownUntilNanos.put(
                    result.meshKey,
                    System.nanoTime() + CHUNK_MESH_REBUILD_COOLDOWN_NANOS
                );
                if (GameConfig.ENABLE_DEBUG_LOGS) {
                    System.out.println("OpenGlRenderer: mesh upload will retry: " + uploadFailure.getMessage());
                }
                continue;
            } catch (Error uploadFailure) {
                deleteMeshBuffer(preparedOpaque.vaoId, preparedOpaque.vboId);
                deleteMeshBuffer(preparedTransparent.vaoId, preparedTransparent.vboId);
                throw uploadFailure;
            }

            int oldOpaqueVaoId = mesh.opaqueVaoId;
            int oldOpaqueVboId = mesh.opaqueVboId;
            int oldTransparentVaoId = mesh.transparentVaoId;
            int oldTransparentVboId = mesh.transparentVboId;
            mesh.opaqueVertices = result.opaqueVertices;
            mesh.opaqueIntCount = result.opaqueIntCount;
            mesh.opaqueVertexCount = result.opaqueIntCount / INTS_PER_VERTEX;
            if (opaqueRegionWasRenderable) {
                mesh.opaqueVaoId = 0;
                mesh.opaqueVboId = 0;
            } else {
                mesh.opaqueVaoId = preparedOpaque.vaoId;
                mesh.opaqueVboId = preparedOpaque.vboId;
                mesh.opaqueVertexCount = preparedOpaque.vertexCount;
            }
            mesh.transparentVaoId = preparedTransparent.vaoId;
            mesh.transparentVboId = preparedTransparent.vboId;
            mesh.transparentVertexCount = preparedTransparent.vertexCount;
            markOpaqueRegionDirty(mesh);
            deleteMeshBuffer(oldOpaqueVaoId, oldOpaqueVboId);
            deleteMeshBuffer(oldTransparentVaoId, oldTransparentVboId);
            mesh.hasOpaqueGeometry = mesh.opaqueIntCount > 0;
            mesh.hasTransparentGeometry = mesh.transparentVertexCount > 0;
            mesh.resident = mesh.hasOpaqueGeometry || mesh.hasTransparentGeometry;
            dirtyChunkMeshes.remove(result.meshKey);
            immediateChunkMeshes.remove(result.meshKey);
            uploaded++;
        }
        return uploaded;
    }

    private void markOpaqueRegionDirty(ChunkMesh mesh) {
        long regionKey = opaqueRegionKey(mesh.chunkX, mesh.chunkY, mesh.chunkZ);
        OpaqueRenderRegion region = opaqueRenderRegions.get(regionKey);
        if (region == null) {
            region = new OpaqueRenderRegion(
                Math.floorDiv(mesh.chunkX, OPAQUE_RENDER_REGION_SIZE_CHUNKS) * OPAQUE_RENDER_REGION_SIZE_CHUNKS,
                mesh.chunkY,
                Math.floorDiv(mesh.chunkZ, OPAQUE_RENDER_REGION_SIZE_CHUNKS) * OPAQUE_RENDER_REGION_SIZE_CHUNKS
            );
            opaqueRenderRegions.put(regionKey, region);
        }
        region.version++;
        if (!region.buildInFlight) {
            enqueueOpaqueRegionBuild(regionKey);
        }
    }

    private long opaqueRegionKey(int chunkX, int chunkY, int chunkZ) {
        return chunkKey(
            Math.floorDiv(chunkX, OPAQUE_RENDER_REGION_SIZE_CHUNKS),
            chunkY,
            Math.floorDiv(chunkZ, OPAQUE_RENDER_REGION_SIZE_CHUNKS)
        );
    }

    private void enqueueOpaqueRegionBuild(long regionKey) {
        if (pendingOpaqueRegionBuildKeys.add(regionKey)) {
            pendingOpaqueRegionBuilds.addLast(regionKey);
        }
    }

    private void scheduleOpaqueRegionBuild() {
        if (opaqueRegionBuildsInFlight > 0) {
            return;
        }
        while (!pendingOpaqueRegionBuilds.isEmpty()) {
            long regionKey = pendingOpaqueRegionBuilds.removeFirst();
            pendingOpaqueRegionBuildKeys.remove(regionKey);
            OpaqueRenderRegion region = opaqueRenderRegions.get(regionKey);
            if (region == null || region.buildInFlight) {
                continue;
            }
            OpaqueRegionBuildSnapshot snapshot = snapshotOpaqueRegion(regionKey, region);
            region.buildInFlight = true;
            opaqueRegionBuildsInFlight++;
            opaqueRegionExecutor.execute(() -> {
                try {
                    completedOpaqueRegionBuilds.add(buildOpaqueRegion(snapshot));
                } catch (RuntimeException failure) {
                    completedOpaqueRegionBuilds.add(OpaqueRegionBuildResult.failed(snapshot, failure));
                }
            });
            return;
        }
    }

    private OpaqueRegionBuildSnapshot snapshotOpaqueRegion(long regionKey, OpaqueRenderRegion region) {
        int[][] vertices = new int[OPAQUE_RENDER_REGION_SIZE_CHUNKS * OPAQUE_RENDER_REGION_SIZE_CHUNKS][];
        int[] intCounts = new int[vertices.length];
        int[] localChunkXs = new int[vertices.length];
        int[] localChunkZs = new int[vertices.length];
        long[] meshKeys = new long[vertices.length];
        int entryCount = 0;
        for (int offsetX = 0; offsetX < OPAQUE_RENDER_REGION_SIZE_CHUNKS; offsetX++) {
            for (int offsetZ = 0; offsetZ < OPAQUE_RENDER_REGION_SIZE_CHUNKS; offsetZ++) {
                int chunkX = region.chunkX + offsetX;
                int chunkZ = region.chunkZ + offsetZ;
                ChunkMesh mesh = chunkMeshes.get(chunkKey(chunkX, region.chunkY, chunkZ));
                if (mesh == null || mesh.opaqueIntCount == 0) {
                    continue;
                }
                vertices[entryCount] = mesh.opaqueVertices;
                intCounts[entryCount] = mesh.opaqueIntCount;
                localChunkXs[entryCount] = offsetX;
                localChunkZs[entryCount] = offsetZ;
                meshKeys[entryCount] = chunkKey(chunkX, region.chunkY, chunkZ);
                entryCount++;
            }
        }
        return new OpaqueRegionBuildSnapshot(regionKey, region.version, meshBuildEpoch, vertices, intCounts, localChunkXs, localChunkZs, meshKeys, entryCount);
    }

    private static OpaqueRegionBuildResult buildOpaqueRegion(OpaqueRegionBuildSnapshot snapshot) {
        int totalVertices = 0;
        for (int index = 0; index < snapshot.entryCount; index++) {
            totalVertices += snapshot.intCounts[index] / INTS_PER_VERTEX;
        }
        int[] regionVertices = new int[totalVertices * GPU_INTS_PER_VERTEX];
        int output = 0;
        for (int entry = 0; entry < snapshot.entryCount; entry++) {
            int[] source = snapshot.vertices[entry];
            int xOffset = snapshot.localChunkXs[entry] * GameConfig.CHUNK_SIZE * (int) PACKED_VERTEX_SCALE;
            int zOffset = snapshot.localChunkZs[entry] * GameConfig.CHUNK_SIZE * (int) PACKED_VERTEX_SCALE;
            for (int input = 0; input < snapshot.intCounts[entry]; input += INTS_PER_VERTEX) {
                int packed = source[input];
                long regionPosition = packOpaqueRegionPosition(packed, xOffset, zOffset);
                regionVertices[output++] = (int) regionPosition;
                regionVertices[output++] = (int) (regionPosition >>> 32);
                regionVertices[output++] = source[input + 1];
            }
        }
        long[] includedMeshKeys = new long[snapshot.entryCount];
        System.arraycopy(snapshot.meshKeys, 0, includedMeshKeys, 0, snapshot.entryCount);
        return new OpaqueRegionBuildResult(snapshot.regionKey, snapshot.version, snapshot.epoch, regionVertices, includedMeshKeys);
    }

    private void uploadReadyOpaqueRegionMeshes() {
        int uploaded = 0;
        while (uploaded < MAX_OPAQUE_REGION_UPLOADS_PER_FRAME) {
            OpaqueRegionBuildResult result = completedOpaqueRegionBuilds.poll();
            if (result == null) {
                return;
            }
            if (result.epoch != meshBuildEpoch) {
                continue;
            }
            opaqueRegionBuildsInFlight = Math.max(0, opaqueRegionBuildsInFlight - 1);
            OpaqueRenderRegion region = opaqueRenderRegions.get(result.regionKey);
            if (region == null) {
                continue;
            }
            region.buildInFlight = false;
            if (result.failure != null) {
                enqueueOpaqueRegionBuild(result.regionKey);
                if (GameConfig.ENABLE_DEBUG_LOGS) {
                    System.out.println("OpenGlRenderer: opaque region build will retry: " + result.failure.getMessage());
                }
                continue;
            }
            if (result.version != region.version) {
                enqueueOpaqueRegionBuild(result.regionKey);
                continue;
            }
            boolean firstUpload = region.vboId == 0;
            try {
                uploadOpaqueRegionBuffer(region, result.vertices);
            } catch (RuntimeException uploadFailure) {
                enqueueOpaqueRegionBuild(result.regionKey);
                if (GameConfig.ENABLE_DEBUG_LOGS) {
                    System.out.println("OpenGlRenderer: opaque region upload will retry: " + uploadFailure.getMessage());
                }
                continue;
            }
            if (firstUpload) {
                region.revealStartNanos = System.nanoTime();
            }
            region.includedMeshKeys.clear();
            for (long meshKey : result.includedMeshKeys) {
                region.includedMeshKeys.add(meshKey);
            }
            deleteOpaqueFallbackBuffers(region);
            uploaded++;
        }
    }

    private void uploadOpaqueRegionBuffer(OpaqueRenderRegion region, int[] vertices) {
        int vertexCount = vertices.length / GPU_INTS_PER_VERTEX;
        GpuMeshBuffer prepared = GpuMeshBuffer.EMPTY;
        if (vertexCount > 0) {
            IntBuffer buffer = BufferUtils.createIntBuffer(vertices.length);
            buffer.put(vertices).flip();
            prepared = uploadGpuBuffer(buffer, vertexCount, "stage opaque region buffer");
        }
        int oldVaoId = region.vaoId;
        int oldVboId = region.vboId;
        region.vaoId = prepared.vaoId;
        region.vboId = prepared.vboId;
        region.vertexCount = prepared.vertexCount;
        deleteMeshBuffer(oldVaoId, oldVboId);
    }

    private void deleteOpaqueFallbackBuffers(OpaqueRenderRegion region) {
        for (long meshKey : region.includedMeshKeys) {
            ChunkMesh mesh = chunkMeshes.get(meshKey);
            if (mesh == null || mesh.opaqueVboId == 0) {
                continue;
            }
            deleteMeshBuffer(mesh.opaqueVaoId, mesh.opaqueVboId);
            mesh.opaqueVaoId = 0;
            mesh.opaqueVboId = 0;
            mesh.opaqueVertexCount = 0;
        }
    }

    private void ensureChunkMeshesAroundPlayer(double playerX, double playerY, double playerZ, int activeChunkRadius) {
        int playerChunkX = worldToChunk((int) Math.floor(playerX));
        int playerChunkZ = worldToChunk((int) Math.floor(playerZ));
        int unloadChunkRadius = activeChunkRadius + GameConfig.RENDER_CHUNK_UNLOAD_PADDING;
        int uploadBudget = dynamicMeshUploadBudget();
        int meshSubmissionBudget = dynamicChunkUploadBudget();
        long meshProfileStartNs = System.nanoTime();
        int uploadedMeshCount = uploadReadyChunkMeshes(uploadBudget, playerChunkX, playerChunkZ, unloadChunkRadius);
        uploadReadyOpaqueRegionMeshes();
        scheduleOpaqueRegionBuild();
        int submittedMeshCount = 0;
        refreshMeshBuildPriorities(playerChunkX, playerChunkZ);
        discoverLoadedChunkMeshes(playerChunkX, playerChunkZ, activeChunkRadius, unloadChunkRadius);
        int promotedCandidateCount = promoteDirtyMeshCandidates(playerX, playerY, playerZ, playerChunkX, playerChunkZ, activeChunkRadius);
        int availableBuildSlots = Math.max(0, dynamicMeshBuildInFlightLimit() - meshBuildsInFlight.size());
        meshSubmissionBudget = Math.min(meshSubmissionBudget, availableBuildSlots);
        while (meshSubmissionBudget > 0 && !meshBuildQueue.isEmpty()) {
            MeshBuildCandidate candidate = meshBuildQueue.poll();
            queuedMeshBuildKeys.remove(candidate.meshKey);
            if (!dirtyChunkMeshes.contains(candidate.meshKey) || meshBuildsInFlight.containsKey(candidate.meshKey) || meshBuildsReady.containsKey(candidate.meshKey)) {
                continue;
            }
            int horizontalDistance = Math.max(Math.abs(candidate.chunkX - playerChunkX), Math.abs(candidate.chunkZ - playerChunkZ));
            if (horizontalDistance > activeChunkRadius || !world.isChunkLoaded(candidate.chunkX, candidate.chunkZ)) {
                continue;
            }
            if (submitChunkMeshBuild(candidate, playerX, playerY, playerZ)) {
                submittedMeshCount++;
                meshSubmissionBudget--;
            } else if (dirtyChunkMeshes.contains(candidate.meshKey) && !meshBuildsInFlight.containsKey(candidate.meshKey) && !meshBuildsReady.containsKey(candidate.meshKey)) {
                enqueueDirtyMeshBuild(candidate.meshKey);
            }
        }
        logMeshProfile(uploadedMeshCount + submittedMeshCount, promotedCandidateCount, System.nanoTime() - meshProfileStartNs);

        staleMeshKeys.clear();
        for (ChunkMesh mesh : chunkMeshes.values()) {
            int horizontalDistance = Math.max(Math.abs(mesh.chunkX - playerChunkX), Math.abs(mesh.chunkZ - playerChunkZ));
            if (!world.isChunkLoaded(mesh.chunkX, mesh.chunkZ) || horizontalDistance > unloadChunkRadius) {
                unloadChunkMesh(mesh);
                staleMeshKeys.add(chunkKey(mesh.chunkX, mesh.chunkY, mesh.chunkZ));
            }
        }
        for (long meshKey : staleMeshKeys) {
            dropChunkMesh(meshKey);
        }
        staleMeshKeys.clear();
        pruneDistantMeshBookkeeping(playerChunkX, playerChunkZ, unloadChunkRadius);
    }

    private void refreshMeshBuildPriorities(int playerChunkX, int playerChunkZ) {
        if (playerChunkX == lastMeshPriorityChunkX && playerChunkZ == lastMeshPriorityChunkZ) return;
        lastMeshPriorityChunkX = playerChunkX;
        lastMeshPriorityChunkZ = playerChunkZ;
        while (!meshBuildQueue.isEmpty()) {
            MeshBuildCandidate candidate = meshBuildQueue.poll();
            queuedMeshBuildKeys.remove(candidate.meshKey);
            if (dirtyChunkMeshes.contains(candidate.meshKey)) enqueueDirtyMeshBuild(candidate.meshKey);
        }
    }

    private void drawGpuBuffer(int vaoId, int vboId, int vertexCount) {
        if (vaoId != 0) {
            bindVertexArrayObject(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            bindVertexArrayObject(0);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glEnableVertexAttribArray(0);
            glVertexAttribIPointer(0, GPU_INTS_PER_VERTEX, GL_UNSIGNED_INT, GPU_BYTES_PER_VERTEX, 0L);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glDisableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
    }

    private void renderOpaqueRegions(int playerChunkX, int playerChunkZ, int chunkRenderRadius, long nowNanos) {
        for (OpaqueRenderRegion region : opaqueRenderRegions.values()) {
            if (region.vboId == 0 || region.vertexCount == 0 || !isOpaqueRegionInRange(region, playerChunkX, playerChunkZ, chunkRenderRadius)
                || !isOpaqueRegionInFrustum(region)) {
                continue;
            }
            glUniform3f(
                chunkShaderOriginLocation,
                (float) cameraRelativeChunkOrigin(region.chunkX, renderCameraX),
                (float) (GameConfig.sectionYForIndex(region.chunkY) - renderCameraY),
                (float) cameraRelativeChunkOrigin(region.chunkZ, renderCameraZ)
            );
            setOpaqueRegionReveal(region, nowNanos);
            drawGpuBuffer(region.vaoId, region.vboId, region.vertexCount);
            debugOpaqueRegionDrawCallsLastFrame++;
        }
    }

    private boolean isOpaqueRegionRenderable(ChunkMesh mesh) {
        OpaqueRenderRegion region = opaqueRenderRegions.get(opaqueRegionKey(mesh.chunkX, mesh.chunkY, mesh.chunkZ));
        return region != null && region.vboId != 0 && region.includedMeshKeys.contains(chunkKey(mesh.chunkX, mesh.chunkY, mesh.chunkZ));
    }

    private boolean isOpaqueRegionInRange(OpaqueRenderRegion region, int playerChunkX, int playerChunkZ, int chunkRenderRadius) {
        int minX = region.chunkX;
        int minZ = region.chunkZ;
        int maxX = minX + OPAQUE_RENDER_REGION_SIZE_CHUNKS - 1;
        int maxZ = minZ + OPAQUE_RENDER_REGION_SIZE_CHUNKS - 1;
        int distanceX = playerChunkX < minX ? minX - playerChunkX : Math.max(0, playerChunkX - maxX);
        int distanceZ = playerChunkZ < minZ ? minZ - playerChunkZ : Math.max(0, playerChunkZ - maxZ);
        return Math.max(distanceX, distanceZ) <= chunkRenderRadius;
    }

    private boolean isOpaqueRegionInFrustum(OpaqueRenderRegion region) {
        double minX = cameraRelativeChunkOrigin(region.chunkX, renderCameraX);
        double minY = GameConfig.sectionYForIndex(region.chunkY) - renderCameraY;
        double minZ = cameraRelativeChunkOrigin(region.chunkZ, renderCameraZ);
        double size = OPAQUE_RENDER_REGION_SIZE_CHUNKS * GameConfig.CHUNK_SIZE;
        return chunkFrustum.intersectsAabb(minX, minY, minZ, minX + size, minY + GameConfig.CHUNK_SIZE, minZ + size);
    }

    static double cameraRelativeChunkOrigin(int chunkCoordinate, double cameraCoordinate) {
        return (double) chunkCoordinate * GameConfig.CHUNK_SIZE - cameraCoordinate;
    }

    static long packOpaqueRegionPosition(int chunkPositionAndAo, int packedXOffset, int packedZOffset) {
        int packedX = (chunkPositionAndAo & PACKED_VERTEX_MAX) + packedXOffset;
        int packedY = (chunkPositionAndAo >>> 10) & PACKED_VERTEX_MAX;
        int packedZ = ((chunkPositionAndAo >>> 20) & PACKED_VERTEX_MAX) + packedZOffset;
        if (packedX < 0 || packedX > PACKED_REGION_COORD_MAX || packedZ < 0 || packedZ > PACKED_REGION_COORD_MAX) {
            throw new IllegalArgumentException("Opaque region vertex is outside the packed coordinate range");
        }
        int ao = (chunkPositionAndAo >>> 30) & 3;
        int low = packedX | (packedY << 13) | ((packedZ & PACKED_REGION_Z_LOW_MAX) << 23);
        int high = (packedZ >>> 9) | (ao << 4);
        return (low & 0xFFFFFFFFL) | ((long) high << 32);
    }

    private void discoverLoadedChunkMeshes(int playerChunkX, int playerChunkZ, int activeChunkRadius, int unloadChunkRadius) {
        int loadedCount = loadedChunkSnapshot.size();
        if (loadedCount == 0) { loadedChunkDiscoveryCursor = 0; return; }
        if (loadedChunkDiscoveryCursor >= loadedCount) loadedChunkDiscoveryCursor = 0;
        int discoveryCount = Math.min(MAX_CHUNK_DISCOVERY_PER_FRAME, loadedCount);
        for (int i = 0; i < discoveryCount; i++) {
            Chunk chunk = loadedChunkSnapshot.get(loadedChunkDiscoveryCursor);
            loadedChunkDiscoveryCursor = (loadedChunkDiscoveryCursor + 1) % loadedCount;
            int horizontalDistance = Math.max(Math.abs(chunk.chunkX - playerChunkX), Math.abs(chunk.chunkZ - playerChunkZ));
            long meshKey = chunkKey(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
            ChunkMesh mesh = chunkMeshes.get(meshKey);
            if (chunk.isEmpty()) {
                if (mesh != null || dirtyChunkMeshes.contains(meshKey)) dropChunkMesh(meshKey);
            } else if (horizontalDistance <= activeChunkRadius) {
                if (mesh == null || !mesh.resident || dirtyChunkMeshes.contains(meshKey)) {
                if (dirtyChunkMeshes.add(meshKey)) {
                    meshBuildRevisions.invalidate(meshKey);
                }
                    enqueueDirtyMeshBuild(meshKey);
                }
            } else if (horizontalDistance > unloadChunkRadius && mesh != null) {
                dropChunkMesh(meshKey);
            }
        }
    }

    private int promoteDirtyMeshCandidates(double playerX, double playerY, double playerZ, int playerChunkX, int playerChunkZ, int activeChunkRadius) {
        int promotedCandidateCount = 0;
        int candidateCount = Math.min(MAX_DIRTY_CANDIDATES_PER_FRAME, pendingDirtyMeshBuilds.size());
        for (int i = 0; i < candidateCount; i++) {
            long meshKey = pendingDirtyMeshBuilds.removeFirst();
            pendingDirtyMeshBuildKeys.remove(meshKey);
            if (!dirtyChunkMeshes.contains(meshKey) || meshBuildsInFlight.containsKey(meshKey) || meshBuildsReady.containsKey(meshKey) || queuedMeshBuildKeys.contains(meshKey)) continue;
            int chunkX = chunkXFromKey(meshKey);
            int chunkY = chunkYFromKey(meshKey);
            int chunkZ = chunkZFromKey(meshKey);
            int horizontalDistance = Math.max(Math.abs(chunkX - playerChunkX), Math.abs(chunkZ - playerChunkZ));
            if (horizontalDistance > activeChunkRadius || !world.isChunkLoaded(chunkX, chunkZ)) continue;
            Chunk chunk = world.getChunk(chunkX, chunkY, chunkZ);
            if (chunk == null) continue;
            if (chunk.isEmpty()) { dropChunkMesh(meshKey); continue; }
            promotedCandidateCount++;
            double distanceSquared = chunkDistanceSquaredToPoint(chunkX, chunkY, chunkZ, playerX, playerY, playerZ);
            queueMeshBuildCandidate(new MeshBuildCandidate(chunkX, chunkY, chunkZ, meshKey, distanceSquared, immediateChunkMeshes.contains(meshKey)));
        }
        return promotedCandidateCount;
    }

    private void enqueueDirtyMeshBuild(long meshKey) {
        if (pendingDirtyMeshBuildKeys.contains(meshKey)) {
            return;
        }
        if (pendingDirtyMeshBuildKeys.size() >= MAX_PENDING_DIRTY_MESH_BUILDS
            && !immediateChunkMeshes.contains(meshKey)) {
            return;
        }
        pendingDirtyMeshBuildKeys.add(meshKey);
        pendingDirtyMeshBuilds.addLast(meshKey);
    }

    private void queueMeshBuildCandidate(MeshBuildCandidate candidate) {
        if (queuedMeshBuildKeys.contains(candidate.meshKey)) {
            return;
        }
        if (meshBuildQueue.size() < MAX_QUEUED_MESH_BUILD_CANDIDATES) {
            queuedMeshBuildKeys.add(candidate.meshKey);
            meshBuildQueue.add(candidate);
            return;
        }

        MeshBuildCandidate worstCandidate = null;
        for (MeshBuildCandidate queuedCandidate : meshBuildQueue) {
            if (worstCandidate == null || queuedCandidate.compareTo(worstCandidate) > 0) {
                worstCandidate = queuedCandidate;
            }
        }
        if (worstCandidate != null && candidate.compareTo(worstCandidate) < 0) {
            meshBuildQueue.remove(worstCandidate);
            queuedMeshBuildKeys.remove(worstCandidate.meshKey);
            queuedMeshBuildKeys.add(candidate.meshKey);
            meshBuildQueue.add(candidate);
        }
    }

    private void emitGreedyCubeFaces(ChunkSectionSnapshot sourceChunk, IntVertexBuilder builder, int startX, int startY, int startZ, MeshBuildContext context, long[] greedyMask) {
        emitGreedyFace(sourceChunk, builder, startX, startY, startZ, Face.WEST, context, greedyMask);
        emitGreedyFace(sourceChunk, builder, startX, startY, startZ, Face.EAST, context, greedyMask);
        emitGreedyFace(sourceChunk, builder, startX, startY, startZ, Face.BOTTOM, context, greedyMask);
        emitGreedyFace(sourceChunk, builder, startX, startY, startZ, Face.TOP, context, greedyMask);
        emitGreedyFace(sourceChunk, builder, startX, startY, startZ, Face.NORTH, context, greedyMask);
        emitGreedyFace(sourceChunk, builder, startX, startY, startZ, Face.SOUTH, context, greedyMask);
    }

    private void emitGreedyFace(ChunkSectionSnapshot chunk, IntVertexBuilder builder, int startX, int startY, int startZ, Face face, MeshBuildContext context, long[] greedyFaceMask) {
        final int size = GameConfig.CHUNK_SIZE;
        for (int layer = 0; layer < size; layer++) {
            for (int i = 0; i < greedyFaceMask.length; i++) {
                greedyFaceMask[i] = 0L;
            }

            for (int u = 0; u < size; u++) {
                for (int v = 0; v < size; v++) {
                    int localX;
                    int localY;
                    int localZ;
                    switch (face) {
                        case WEST:
                        case EAST:
                            localX = layer;
                            localY = u;
                            localZ = v;
                            break;
                        case TOP:
                        case BOTTOM:
                            localX = u;
                            localY = layer;
                            localZ = v;
                            break;
                        case NORTH:
                        case SOUTH:
                            localX = u;
                            localY = v;
                            localZ = layer;
                            break;
                        default:
                            localX = u;
                            localY = v;
                            localZ = layer;
                            break;
                    }

                    int worldX = startX + localX;
                    int worldY = startY + localY;
                    int worldZ = startZ + localZ;
                    byte block = chunk.getBlockLocal(localX, localY, localZ);
                    BlockState state = chunk.getBlockStateLocal(localX, localY, localZ);
                    if (!isGreedyMeshBlock(block, state, worldX, worldY, worldZ, context)) {
                        continue;
                    }
                    if (!waterRenderer.isRenderableBlockFace(block, worldX, worldY, worldZ, face)) {
                        continue;
                    }
                    int color = colorForFace(block, face, Settings.goodGraphics() ? world.getAmbientShade(worldX, worldY, worldZ) : 1.0f, worldX, worldY, worldZ);
                    int ao = packUnitFaceAo(chunk, startX, startY, startZ, worldX, worldY, worldZ, face);
                    greedyFaceMask[u * size + v] = greedyKey(state, color, ao);
                }
            }

            for (int u = 0; u < size; u++) {
                for (int v = 0; v < size; ) {
                    long key = greedyFaceMask[u * size + v];
                    if (key == 0L) {
                        v++;
                        continue;
                    }
                    int width = 1;
                    while (v + width < size && greedyFaceMask[u * size + v + width] == key) {
                        width++;
                    }
                    int height = 1;
                    boolean expand = true;
                    while (u + height < size && expand) {
                        for (int offset = 0; offset < width; offset++) {
                            if (greedyFaceMask[(u + height) * size + v + offset] != key) {
                                expand = false;
                                break;
                            }
                        }
                        if (expand) {
                            height++;
                        }
                    }
                    for (int du = 0; du < height; du++) {
                        for (int dv = 0; dv < width; dv++) {
                            greedyFaceMask[(u + du) * size + v + dv] = 0L;
                        }
                    }
                    appendGreedyQuad(chunk, builder, startX, startY, startZ, face, layer, u, v, height, width, (int) (key >>> 32));
                    v += width;
                }
            }
        }
    }

    private boolean isGreedyMeshBlock(byte block, BlockState state, int worldX, int worldY, int worldZ, MeshBuildContext context) {
        if (block == GameConfig.AIR
            || block == GameConfig.OAK_DOOR
            || block == GameConfig.OAK_FENCE
            || block == GameConfig.SNOW_LAYER
            || block == GameConfig.RED_BED
            || isOreBlock(block)
            || world.isCrossPlant(block)
            || !world.isCubeBlock(block)
            || world.isTransparentBlock(block)
            || world.isLiquidBlock(block)) {
            return false;
        }
        boolean cameraInsideSolidBlock = worldX == context.cameraBlockX
            && worldY == context.cameraBlockY
            && worldZ == context.cameraBlockZ
            && world.isSolidBlock(block);
        if (cameraInsideSolidBlock) {
            return false;
        }
        return !context.spectatorInsideBlock || !world.isSolidBlock(block) || !isNearSpectatorCutoutBlock(worldX, worldY, worldZ, context);
    }

    private boolean isWavingFoliageFace(byte block, Face face) {
        return block == GameConfig.OAK_LEAVES
            || block == GameConfig.PINE_LEAVES
            || block == GameConfig.BIRCH_LEAVES;
    }

    private long greedyKey(BlockState state, int color, int ao) {
        int numericId = state == null || state.type == null ? GameConfig.AIR & 0xFF : state.type.numericId & 0xFF;
        int data = state == null ? 0 : state.data & 0x7FFF;
        return ((long) color << 32) | ((long) (ao & 0x1FF) << 23) | ((long) numericId << 15) | data;
    }

    private byte greedyFaceBlock(ChunkSectionSnapshot chunk, Face face, int layer, int u, int v) {
        switch (face) {
            case WEST:
            case EAST:
                return chunk.getBlockLocal(layer, u, v);
            case TOP:
            case BOTTOM:
                return chunk.getBlockLocal(u, layer, v);
            case NORTH:
            case SOUTH:
                return chunk.getBlockLocal(u, v, layer);
            default:
                return GameConfig.AIR;
        }
    }

    private void appendGreedyQuad(ChunkSectionSnapshot chunk, IntVertexBuilder builder, int startX, int startY, int startZ,
                                  Face face, int layer, int u, int v, int height, int width, int color) {
        double x;
        double y;
        double z;
        int ao = packGreedyFaceAo(chunk, startX, startY, startZ, face, layer, u, v, height, width);
        byte block = greedyFaceBlock(chunk, face, layer, u, v);
        int vertexFlags = isWavingFoliageFace(block, face) ? VERTEX_FLAG_FOLIAGE : 0;
        switch (face) {
            case WEST:
                x = layer;
                appendQuad(builder, x, u, v, x, u + height, v, x, u + height, v + width, x, u, v + width, color, ao, vertexFlags);
                break;
            case EAST:
                x = layer + 1.0;
                appendQuad(builder, x, u, v + width, x, u + height, v + width, x, u + height, v, x, u, v, color, ao, vertexFlags);
                break;
            case BOTTOM:
                y = layer;
                appendQuad(builder, u, y, v, u, y, v + width, u + height, y, v + width, u + height, y, v, color, ao, vertexFlags);
                break;
            case TOP:
                y = layer + 1.0;
                appendQuad(builder, u, y, v + width, u, y, v, u + height, y, v, u + height, y, v + width, color, ao, vertexFlags);
                break;
            case NORTH:
                z = layer;
                appendQuad(builder, u + height, v, z, u + height, v + width, z, u, v + width, z, u, v, z, color, ao, vertexFlags);
                break;
            case SOUTH:
                z = layer + 1.0;
                appendQuad(builder, u, v, z, u, v + width, z, u + height, v + width, z, u + height, v, z, color, ao, vertexFlags);
                break;
            default:
                break;
        }
    }

    private void logMeshProfile(int rebuiltMeshCount, int promotedChunkCount, long elapsedNs) {
        if (!GameConfig.ENABLE_FRAME_PROFILING) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastMeshProfileLogNanos < 1_000_000_000L) {
            return;
        }
        lastMeshProfileLogNanos = now;
        if (rebuiltMeshCount <= 0) {
            return;
        }
        int residentCount = 0;
        for (ChunkMesh mesh : chunkMeshes.values()) {
            if (mesh.resident) {
                residentCount++;
            }
        }
        System.out.println(String.format(Locale.ROOT,
            "OpenGlRenderer mesh rebuilt=%d resident=%d promotedChunks=%d ms=%.3f",
            rebuiltMeshCount,
            residentCount,
            promotedChunkCount,
            elapsedNs / 1_000_000.0));
    }

    private int residentChunkMeshCount() {
        int count = 0;
        for (ChunkMesh mesh : chunkMeshes.values()) if (mesh.resident) count++;
        return count;
    }

    private boolean hasResidentChunkMesh() {
        for (ChunkMesh mesh : chunkMeshes.values()) {
            if (mesh.resident) {
                return true;
            }
        }
        return false;
    }

    private int getChunkRenderRadius(PlayerState player) {
        int radius = player.spectatorMode
            ? Math.max(currentRenderDistanceChunks, GameConfig.SPECTATOR_CHUNK_RENDER_DISTANCE)
            : currentRenderDistanceChunks;
        return clamp(radius + RENDER_EDGE_PADDING_CHUNKS, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
    }

    private int getMeshBuildRadius(PlayerState player) {
        int targetRadius = getChunkRenderRadius(player);
        int currentRadius = meshBuildRadiusController.current(targetRadius);
        int playerChunkX = worldToChunk((int) Math.floor(player.x));
        int playerChunkZ = worldToChunk((int) Math.floor(player.z));
        boolean currentRadiusReady = hasCompleteMeshCoverage(playerChunkX, playerChunkZ, currentRadius);
        return meshBuildRadiusController.update(targetRadius, currentRadiusReady);
    }

    private boolean hasCompleteMeshCoverage(int playerChunkX, int playerChunkZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!world.isChunkLoaded(playerChunkX + dx, playerChunkZ + dz)) {
                    return false;
                }
            }
        }
        for (Chunk chunk : loadedChunkSnapshot) {
            int horizontalDistance = Math.max(Math.abs(chunk.chunkX - playerChunkX), Math.abs(chunk.chunkZ - playerChunkZ));
            if (horizontalDistance > radius || chunk.isEmpty()) {
                continue;
            }
            ChunkMesh mesh = chunkMeshes.get(chunkKey(chunk.chunkX, chunk.chunkY, chunk.chunkZ));
            if (mesh == null || !mesh.resident) {
                return false;
            }
        }
        return true;
    }

    private int dynamicChunkUploadBudget() {
        if (currentRenderDistanceChunks > 16) {
            return Settings.goodGraphics() ? 8 : 6;
        }
        if (debugFps > 0.0) {
            double frameTimeMs = 1000.0 / debugFps;
            if (frameTimeMs > 14.0) {
                return 8;
            }
            if (frameTimeMs < 8.0) {
                return 48;
            }
        }
        if (!Settings.goodGraphics()) {
            if (debugFps >= 55.0 || debugFps <= 0.0) {
                return currentRenderDistanceChunks >= 24 ? 7 : 5;
            }
            if (debugFps >= 35.0) {
                return currentRenderDistanceChunks >= 24 ? 5 : 3;
            }
            return MIN_CHUNK_UPLOADS_PER_FRAME;
        }
        if (debugFps >= 58.0 || debugFps <= 0.0) {
            return MAX_CHUNK_UPLOADS_PER_FRAME;
        }
        if (debugFps >= 48.0) {
            return 8;
        }
        if (debugFps >= 36.0) {
            return 5;
        }
        if (debugFps >= 26.0) {
            return 3;
        }
        return MIN_CHUNK_UPLOADS_PER_FRAME;
    }

    private int dynamicMeshBuildInFlightLimit() {
        if (debugFps > 0.0 && debugFps < 45.0) {
            return Math.min(MAX_MESH_BUILDS_IN_FLIGHT, 2);
        }
        if (debugFps > 0.0 && debugFps < 55.0) {
            return Math.min(MAX_MESH_BUILDS_IN_FLIGHT, MESH_THREAD_COUNT);
        }
        return MAX_MESH_BUILDS_IN_FLIGHT;
    }

    private int dynamicMeshUploadBudget() {
        int backlog = completedImmediateMeshBuilds.size() + completedMeshBuilds.size();
        if (backlog > 500) {
            return MAX_BACKLOG_CHUNK_UPLOADS_PER_FRAME;
        }
        if (!completedImmediateMeshBuilds.isEmpty()) {
            return MAX_IMMEDIATE_CHUNK_UPLOADS_PER_FRAME;
        }
        if (backlog >= MAX_CATCH_UP_CHUNK_UPLOADS_PER_FRAME
            && debugMeshUploadNanosLastFrame < 1_000_000L) {
            return MAX_CATCH_UP_CHUNK_UPLOADS_PER_FRAME;
        }
        if (debugFps > 0.0 && debugFps < 45.0) {
            return 4;
        }
        return MAX_CHUNK_UPLOADS_PER_FRAME;
    }

    private void updateFpsCounter(double deltaTime) {
        fpsSampleFrames++;
        fpsSampleTime += deltaTime;
        if (fpsSampleTime >= 0.25) {
            debugFps = fpsSampleFrames / fpsSampleTime;
            fpsSampleFrames = 0;
            fpsSampleTime = 0.0;
        }
    }

    private long chunkKey(int chunkX, int chunkY, int chunkZ) {
        long packedX = ((long) chunkX) & 0x3FFFFFFL;
        long packedZ = ((long) chunkZ) & 0x3FFFFFFL;
        long packedY = chunkY & 0xFFFL;
        return (packedX << 38) | (packedZ << 12) | packedY;
    }

    private int chunkXFromKey(long key) {
        return (int) (key >> 38);
    }

    private int chunkZFromKey(long key) {
        int packed = (int) ((key >> 12) & 0x3FFFFFFL);
        return packed >= 0x2000000 ? packed - 0x4000000 : packed;
    }

    private int chunkYFromKey(long key) {
        return (int) (key & 0xFFFL);
    }

    private int worldToChunk(int coordinate) {
        return Math.floorDiv(coordinate, GameConfig.CHUNK_SIZE);
    }

    private void renderZombies(PlayerState player) {
        for (MobEntity mob : world.getMobs()) {
            if (!shouldRenderMob(player, mob)) {
                continue;
            }
            glPushMatrix();
            glTranslated(
                interpolatedX(mob) - renderCameraX,
                interpolatedY(mob) - renderCameraY,
                interpolatedZ(mob) - renderCameraZ
            );
            glRotated(-Math.toDegrees(mob.bodyYaw) - 90.0, 0.0, 1.0, 0.0);

            double mobSpeed = Math.sqrt(mob.velocityX * mob.velocityX + mob.velocityZ * mob.velocityZ);
            double swingScale = Math.min(1.0, mobSpeed / 0.55);
            double armSwing = Math.sin(mob.walkCycle) * 28.0 * swingScale;
            double legSwing = Math.sin(mob.walkCycle) * 34.0 * swingScale;
            double headTurn = Math.toDegrees(Math.atan2(
                Math.sin(mob.targetBodyYaw - mob.bodyYaw),
                Math.cos(mob.targetBodyYaw - mob.bodyYaw)
            ));
            headTurn = Math.max(-35.0, Math.min(35.0, headTurn));
            float[] skinColor = mobSkinColor(mob.kind);
            float[] torsoColor = mobTorsoColor(mob.kind);
            float[] limbColor = mobLimbColor(mob.kind);
            float[] legColor = mobLegColor(mob.kind);
            boolean skeleton = mob.kind == MobKind.SKELETON;

            if (mob.kind == MobKind.HERRING || mob.kind == MobKind.SALMON) {
                renderFishMob(mob.kind, legSwing);
            } else if (mob.kind == MobKind.PIG || mob.kind == MobKind.SHEEP || mob.kind == MobKind.COW) {
                if (mob.isBaby()) {
                    glScalef(0.58f, 0.58f, 0.58f);
                }
                renderQuadrupedMob(mob, legSwing, headTurn, skinColor, torsoColor, legColor);
            } else if (mob.kind == MobKind.VILLAGER) {
                renderVillagerMob(armSwing, legSwing, headTurn, skinColor, torsoColor, limbColor, legColor);
            } else {
                renderHumanoidMob(armSwing, legSwing, headTurn, skinColor, torsoColor, limbColor, legColor, skeleton);
            }

            if (mob.fireTimer > 0.0) {
                drawCuboid(-0.30, 0.02, -0.30, 0.30, 1.65, 0.30, 1.0f, 0.34f, 0.06f);
            }

            glPopMatrix();
        }
    }

    private void renderHumanoidMob(double armSwing, double legSwing, double headTurn, float[] skinColor, float[] torsoColor, float[] limbColor, float[] legColor, boolean skeleton) {
        glPushMatrix();
        glTranslated(0.0, 1.585, 0.0);
        glRotated(-headTurn, 0.0, 1.0, 0.0);
        drawCuboid(-0.23, -0.235, -0.23, 0.23, 0.235, 0.23, skinColor[0], skinColor[1], skinColor[2]);
        drawCuboid(-0.12, -0.015, -0.245, -0.04, 0.065, -0.205, 0.04f, 0.04f, 0.04f);
        drawCuboid(0.04, -0.015, -0.245, 0.12, 0.065, -0.205, 0.04f, 0.04f, 0.04f);
        glPopMatrix();
        drawCuboid(-0.25, 0.73, -0.17, 0.25, 1.35, 0.17, torsoColor[0], torsoColor[1], torsoColor[2]);

        glPushMatrix();
        glTranslated(-0.30, 1.23, 0.0);
        glRotated(armSwing, 1.0, 0.0, 0.0);
        drawCuboid(-0.08, -0.50, -0.08, 0.08, 0.16, 0.08, limbColor[0], limbColor[1], limbColor[2]);
        glPopMatrix();

        glPushMatrix();
        glTranslated(0.30, 1.23, 0.0);
        glRotated(-armSwing, 1.0, 0.0, 0.0);
        drawCuboid(-0.08, -0.50, -0.08, 0.08, 0.16, 0.08, limbColor[0], limbColor[1], limbColor[2]);
        glPopMatrix();

        glPushMatrix();
        glTranslated(-0.12, 0.74, 0.0);
        glRotated(-legSwing, 1.0, 0.0, 0.0);
        drawCuboid(-0.09, -0.74, -0.09, 0.09, 0.0, 0.09, legColor[0], legColor[1], legColor[2]);
        glPopMatrix();

        glPushMatrix();
        glTranslated(0.12, 0.74, 0.0);
        glRotated(legSwing, 1.0, 0.0, 0.0);
        drawCuboid(-0.09, -0.74, -0.09, 0.09, 0.0, 0.09, legColor[0], legColor[1], legColor[2]);
        glPopMatrix();

        if (skeleton) {
            drawCuboid(-0.02, 0.76, -0.03, 0.02, 1.60, 0.03, 0.88f, 0.88f, 0.90f);
        }
    }

    private void renderVillagerMob(double armSwing, double legSwing, double headTurn, float[] skinColor, float[] torsoColor, float[] limbColor, float[] legColor) {
        glPushMatrix();
        glTranslated(0.0, 1.62, 0.0);
        glRotated(-headTurn, 0.0, 1.0, 0.0);
        drawCuboid(-0.27, -0.28, -0.25, 0.27, 0.27, 0.25, skinColor[0], skinColor[1], skinColor[2]);
        drawCuboid(-0.06, -0.06, -0.38, 0.06, 0.08, -0.22, 0.58f, 0.42f, 0.34f);
        drawCuboid(-0.13, 0.02, -0.265, -0.05, 0.10, -0.235, 0.04f, 0.04f, 0.04f);
        drawCuboid(0.05, 0.02, -0.265, 0.13, 0.10, -0.235, 0.04f, 0.04f, 0.04f);
        glPopMatrix();

        drawCuboid(-0.28, 0.58, -0.20, 0.28, 1.36, 0.20, torsoColor[0], torsoColor[1], torsoColor[2]);
        drawCuboid(-0.33, 0.86, -0.22, 0.33, 1.05, -0.02, limbColor[0], limbColor[1], limbColor[2]);
        drawCuboid(-0.18, 0.66, -0.24, 0.18, 0.92, -0.06, limbColor[0], limbColor[1], limbColor[2]);

        glPushMatrix();
        glTranslated(-0.10, 0.72, 0.0);
        glRotated(-legSwing, 1.0, 0.0, 0.0);
        drawCuboid(-0.09, -0.72, -0.09, 0.09, 0.0, 0.09, legColor[0], legColor[1], legColor[2]);
        glPopMatrix();

        glPushMatrix();
        glTranslated(0.10, 0.72, 0.0);
        glRotated(legSwing, 1.0, 0.0, 0.0);
        drawCuboid(-0.09, -0.72, -0.09, 0.09, 0.0, 0.09, legColor[0], legColor[1], legColor[2]);
        glPopMatrix();
    }

    private void renderFishMob(MobKind kind, double tailSwing) {
        boolean salmon = kind == MobKind.SALMON;
        float bodyR = salmon ? 0.86f : 0.22f;
        float bodyG = salmon ? 0.42f : 0.58f;
        float bodyB = salmon ? 0.34f : 0.68f;
        double length = salmon ? 0.72 : 0.52;
        double height = salmon ? 0.24 : 0.18;
        double width = salmon ? 0.18 : 0.13;
        drawCuboid(-length * 0.42, height * 0.18, -width, length * 0.34, height * 1.18, width, bodyR, bodyG, bodyB);
        drawCuboid(length * 0.30, height * 0.34, -width * 0.72, length * 0.48, height * 1.02, width * 0.72, bodyR * 0.92f, bodyG * 0.92f, bodyB * 0.92f);
        double tail = Math.sin(Math.toRadians(tailSwing)) * 0.10;
        drawCuboid(-length * 0.60, height * 0.40, -0.025 + tail, -length * 0.40, height * 0.98, 0.025 + tail, bodyR * 0.82f, bodyG * 0.82f, bodyB * 0.82f);
        drawCuboid(length * 0.39, height * 0.72, -width - 0.008, length * 0.43, height * 0.86, -width + 0.020, 0.04f, 0.04f, 0.04f);
        drawCuboid(length * 0.39, height * 0.72, width - 0.020, length * 0.43, height * 0.86, width + 0.008, 0.04f, 0.04f, 0.04f);
    }

    private void renderQuadrupedMob(MobEntity mob, double legSwing, double headTurn, float[] skinColor, float[] torsoColor, float[] legColor) {
        double bodyMinY = mob.kind == MobKind.SHEEP ? 0.56 : 0.52;
        double bodyMaxY = mob.kind == MobKind.SHEEP ? 1.18 : 1.08;
        double bodyMinX = mob.kind == MobKind.COW ? -0.36 : -0.31;
        double bodyMaxX = mob.kind == MobKind.COW ? 0.36 : 0.31;
        double bodyMinZ = mob.kind == MobKind.PIG ? -0.52 : -0.58;
        double bodyMaxZ = mob.kind == MobKind.PIG ? 0.42 : 0.48;

        drawCuboid(bodyMinX, bodyMinY, bodyMinZ, bodyMaxX, bodyMaxY, bodyMaxZ, torsoColor[0], torsoColor[1], torsoColor[2]);
        if (mob.kind == MobKind.SHEEP) {
            drawCuboid(-0.40, 0.54, -0.62, 0.40, 1.23, 0.50, 0.95f, 0.95f, 0.92f);
        }

        glPushMatrix();
        glTranslated(0.0, mob.kind == MobKind.COW ? 0.98 : 0.94, mob.kind == MobKind.PIG ? -0.66 : -0.72);
        glRotated(-headTurn * 0.75, 0.0, 1.0, 0.0);
        if (mob.kind == MobKind.PIG) {
            drawCuboid(-0.22, -0.18, -0.20, 0.22, 0.18, 0.20, skinColor[0], skinColor[1], skinColor[2]);
            drawCuboid(-0.12, -0.08, -0.34, 0.12, 0.07, -0.19, 0.96f, 0.70f, 0.76f);
            drawCuboid(-0.11, -0.01, -0.35, -0.06, 0.04, -0.345, 0.54f, 0.22f, 0.28f);
            drawCuboid(0.06, -0.01, -0.35, 0.11, 0.04, -0.345, 0.54f, 0.22f, 0.28f);
        } else if (mob.kind == MobKind.SHEEP) {
            drawCuboid(-0.23, -0.22, -0.22, 0.23, 0.22, 0.23, 0.94f, 0.94f, 0.90f);
            drawCuboid(-0.15, -0.12, -0.31, 0.15, 0.12, -0.20, 0.25f, 0.23f, 0.20f);
        } else {
            drawCuboid(-0.23, -0.20, -0.22, 0.23, 0.20, 0.22, skinColor[0], skinColor[1], skinColor[2]);
            drawCuboid(-0.14, -0.09, -0.35, 0.14, 0.07, -0.18, 0.66f, 0.52f, 0.38f);
            drawCuboid(-0.19, 0.11, -0.11, -0.11, 0.25, 0.03, 0.84f, 0.82f, 0.76f);
            drawCuboid(0.11, 0.11, -0.11, 0.19, 0.25, 0.03, 0.84f, 0.82f, 0.76f);
            drawCuboid(-0.33, -0.10, -0.06, -0.21, 0.06, 0.10, 0.28f, 0.18f, 0.10f);
            drawCuboid(0.21, -0.10, -0.06, 0.33, 0.06, 0.10, 0.28f, 0.18f, 0.10f);
        }
        renderQuadrupedEyes(mob.kind);
        glPopMatrix();

        renderQuadrupedLeg(-0.21, 0.58, -0.38, -legSwing, legColor);
        renderQuadrupedLeg(0.21, 0.58, -0.38, legSwing, legColor);
        renderQuadrupedLeg(-0.21, 0.58, 0.28, legSwing, legColor);
        renderQuadrupedLeg(0.21, 0.58, 0.28, -legSwing, legColor);
    }

    private void renderQuadrupedLeg(double x, double y, double z, double swing, float[] color) {
        glPushMatrix();
        glTranslated(x, y, z);
        glRotated(swing, 1.0, 0.0, 0.0);
        drawCuboid(-0.075, -0.54, -0.075, 0.075, 0.0, 0.075, color[0], color[1], color[2]);
        glPopMatrix();
    }

    private void renderQuadrupedEyes(MobKind kind) {
        if (kind == MobKind.SHEEP) {
            drawCuboid(-0.115, 0.025, -0.325, -0.055, 0.085, -0.300, 0.03f, 0.03f, 0.03f);
            drawCuboid(0.055, 0.025, -0.325, 0.115, 0.085, -0.300, 0.03f, 0.03f, 0.03f);
        } else if (kind == MobKind.PIG) {
            drawCuboid(-0.145, 0.045, -0.205, -0.085, 0.115, -0.178, 0.03f, 0.03f, 0.035f);
            drawCuboid(0.085, 0.045, -0.205, 0.145, 0.115, -0.178, 0.03f, 0.03f, 0.035f);
        } else {
            drawCuboid(-0.155, 0.055, -0.225, -0.090, 0.130, -0.198, 0.025f, 0.025f, 0.02f);
            drawCuboid(0.090, 0.055, -0.225, 0.155, 0.130, -0.198, 0.025f, 0.025f, 0.02f);
        }
    }

    private float[] mobSkinColor(MobKind kind) {
        switch (kind) {
            case SKELETON:
                return new float[]{0.88f, 0.88f, 0.90f};
            case PIG:
                return new float[]{0.92f, 0.66f, 0.72f};
            case SHEEP:
                return new float[]{0.92f, 0.92f, 0.88f};
            case COW:
                return new float[]{0.40f, 0.28f, 0.16f};
            case VILLAGER:
                return new float[]{0.76f, 0.62f, 0.48f};
            case ZOMBIE:
            default:
                return new float[]{0.38f, 0.69f, 0.33f};
        }
    }

    private float[] mobTorsoColor(MobKind kind) {
        switch (kind) {
            case SKELETON:
                return new float[]{0.82f, 0.82f, 0.84f};
            case PIG:
                return new float[]{0.90f, 0.58f, 0.66f};
            case SHEEP:
                return new float[]{0.94f, 0.94f, 0.90f};
            case COW:
                return new float[]{0.32f, 0.20f, 0.12f};
            case VILLAGER:
                return new float[]{0.52f, 0.34f, 0.18f};
            case ZOMBIE:
            default:
                return new float[]{0.23f, 0.51f, 0.57f};
        }
    }

    private float[] mobLimbColor(MobKind kind) {
        switch (kind) {
            case SKELETON:
                return new float[]{0.84f, 0.84f, 0.86f};
            case VILLAGER:
                return new float[]{0.68f, 0.54f, 0.42f};
            default:
                return mobSkinColor(kind);
        }
    }

    private float[] mobLegColor(MobKind kind) {
        switch (kind) {
            case SKELETON:
                return new float[]{0.80f, 0.80f, 0.82f};
            case PIG:
                return new float[]{0.86f, 0.56f, 0.62f};
            case SHEEP:
                return new float[]{0.88f, 0.88f, 0.84f};
            case COW:
                return new float[]{0.24f, 0.16f, 0.10f};
            case VILLAGER:
                return new float[]{0.42f, 0.24f, 0.12f};
            case ZOMBIE:
            default:
                return new float[]{0.25f, 0.27f, 0.53f};
        }
    }

    private void renderDroppedItems(PlayerState player, double timeOfDay) {
        for (DroppedItem droppedItem : world.getDroppedItems()) {
            double renderX = interpolatedX(droppedItem);
            double renderY = interpolatedY(droppedItem);
            double renderZ = interpolatedZ(droppedItem);
            double dx = renderX - player.x;
            double dz = renderZ - player.z;
            if (dx * dx + dz * dz > GameConfig.MAX_RENDER_DISTANCE * GameConfig.MAX_RENDER_DISTANCE) {
                continue;
            }
            float[] color = getHeldBlockColor(droppedItem.itemId);
            double bob = Math.sin(timeOfDay * Math.PI * 12.0 + droppedItem.spinDegrees * 0.04) * 0.04;
            glPushMatrix();
            glTranslated(
                renderX - renderCameraX,
                renderY - renderCameraY + 0.14 + bob,
                renderZ - renderCameraZ
            );
            glRotated(droppedItem.spinDegrees, 0.0, 1.0, 0.0);
            drawCuboid(-0.12, -0.12, -0.12, 0.12, 0.12, 0.12, color[0], color[1], color[2]);
            glPopMatrix();
        }
    }

    private void renderFallingBlocks(PlayerState player) {
        for (FallingBlock fallingBlock : world.getFallingBlocks()) {
            double renderX = interpolatedX(fallingBlock);
            double renderY = interpolatedY(fallingBlock);
            double renderZ = interpolatedZ(fallingBlock);
            if (!isSphereVisible(
                player,
                renderX,
                renderY + 0.5,
                renderZ,
                0.9,
                GameConfig.MAX_RENDER_DISTANCE
            )) {
                continue;
            }

            float[] color = getHeldBlockColor(fallingBlock.blockId);
            glPushMatrix();
            glTranslated(
                renderX - renderCameraX,
                renderY - renderCameraY,
                renderZ - renderCameraZ
            );
            drawCuboid(-0.49, 0.0, -0.49, 0.49, 0.98, 0.49, color[0], color[1], color[2]);
            glPopMatrix();
        }
    }

    private double interpolatedX(Entity entity) {
        return lerpDouble(entity.previousX, entity.x, currentPartialTicks);
    }

    private double interpolatedY(Entity entity) {
        return lerpDouble(entity.previousY, entity.y, currentPartialTicks);
    }

    private double interpolatedZ(Entity entity) {
        return lerpDouble(entity.previousZ, entity.z, currentPartialTicks);
    }

    private void renderPlayerModel(PlayerState player, PlayerInventory inventory, byte selectedBlock, boolean thirdPersonView, boolean frontThirdPersonView) {
        if (!thirdPersonView) {
            return;
        }

        double walkSwing = Math.sin(player.cameraBobPhase) * 26.0 * player.cameraBobAmount;
        double attackTime = player.handSwingTimer <= 0.0 ? 0.0 : clamp(1.0 - player.handSwingTimer / 0.22, 0.0, 1.0);
        double attackSwing = attackTime <= 0.0 ? 0.0 : Math.sin(attackTime * Math.PI) * 46.0;
        glPushMatrix();
        glTranslated(player.x - renderCameraX, player.y - renderCameraY, player.z - renderCameraZ);
        glRotated(-Math.toDegrees(player.yaw) - 90.0, 0.0, 1.0, 0.0);
        drawPlayerModelParts(inventory, selectedBlock, walkSwing, attackSwing, player.pitch, !player.spectatorMode, player.sneaking);
        if (player.fireTimer > 0.0) {
            drawCuboid(-0.30, 0.02, -0.30, 0.30, 1.65, 0.30, 1.0f, 0.34f, 0.06f);
        }
        glPopMatrix();
    }

    private void renderRemotePlayers(PlayerState localPlayer) {
        for (RemotePlayerState remote : world.getRemotePlayers()) {
            double dx = remote.x - localPlayer.x;
            double dz = remote.z - localPlayer.z;
            if (dx * dx + dz * dz > GameConfig.MAX_RENDER_DISTANCE * GameConfig.MAX_RENDER_DISTANCE) {
                continue;
            }
            double walkSwing = Math.sin(remote.stepTimer) * 16.0;
            glPushMatrix();
            glTranslated(
                interpolatedX(remote) - renderCameraX,
                interpolatedY(remote) - renderCameraY,
                interpolatedZ(remote) - renderCameraZ
            );
            glRotated(-Math.toDegrees(remote.yaw) - 90.0, 0.0, 1.0, 0.0);
            drawPlayerModelParts(null, remote.heldItem, walkSwing, 0.0, remote.pitch, !remote.spectatorMode, remote.sneaking);
            glPopMatrix();
            renderNameTag(remote.name, interpolatedX(remote), interpolatedY(remote) + 2.18, interpolatedZ(remote));
        }
    }

    private void renderNameTag(String name, double x, double y, double z) {
        if (name == null || name.trim().isEmpty()) {
            name = "Player";
        } else {
            name = name.trim();
        }
        double dx = x - renderCameraX;
        double dy = y - renderCameraY;
        double dz = z - renderCameraZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 72.0) {
            return;
        }

        glPushMatrix();
        glTranslated(dx, dy, dz);
        glRotated(-Math.toDegrees(renderCameraYaw) + 90.0, 0.0, 1.0, 0.0);
        glRotated(Math.toDegrees(renderCameraPitch), 1.0, 0.0, 0.0);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);

        float scale = (float) clamp(0.032 + distance * 0.0012, 0.034, 0.075);
        glScalef(-scale, -scale, scale);
        String label = trimTextToWidth(name, 1.0f, 150.0f);
        float width = measureTextWidth(label, 1.0f);
        drawRect(-width * 0.5f - 7.0f, -13.0f, width + 14.0f, 20.0f, 0.0f, 0.0f, 0.0f, 0.72f);
        drawOutline(-width * 0.5f - 7.0f, -13.0f, width + 14.0f, 20.0f, 1.0f, 0.85f, 0.85f, 0.92f, 0.86f);
        drawShadowText(-width * 0.5f, 2.0f, 1.0f, label, 1.0f, 1.0f, 1.0f);

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glPopMatrix();
    }

    private void drawPlayerModelParts(PlayerInventory inventory, byte selectedBlock, double swing, double attackSwing, double pitch, boolean showHeldItem, boolean sneakingPose) {
        float[] helmet = inventory == null ? null : armorColor(inventory.getArmorStack(0));
        float[] chest = inventory == null ? null : armorColor(inventory.getArmorStack(1));
        float[] legs = inventory == null ? null : armorColor(inventory.getArmorStack(2));
        float[] boots = inventory == null ? null : armorColor(inventory.getArmorStack(3));
        double crouch = sneakingPose ? -0.23 : 0.0;
        double legTop = sneakingPose ? 0.62 : 0.74;
        double legBottom = sneakingPose ? -0.60 : -0.74;

        float bodyR = chest == null ? 0.22f : chest[0];
        float bodyG = chest == null ? 0.47f : chest[1];
        float bodyB = chest == null ? 0.85f : chest[2];
        double bodyMinY = 0.73 + crouch;
        double bodyMaxY = 1.36 + crouch;
        drawCuboid(-0.25, bodyMinY, -0.17, 0.25, bodyMaxY, 0.17, bodyR, bodyG, bodyB);

        glPushMatrix();
        glTranslated(0.0, bodyMaxY + 0.23, sneakingPose ? -0.04 : 0.0);
        glRotated(Math.toDegrees(pitch), 1.0, 0.0, 0.0);
        drawCuboid(-0.23, -0.23, -0.23, 0.23, 0.23, 0.23, 0.91f, 0.78f, 0.63f);
        drawPlayerEyes();
        if (helmet != null) {
            drawCuboid(-0.255, -0.255, -0.255, 0.255, 0.07, 0.255, helmet[0], helmet[1], helmet[2]);
        }
        glPopMatrix();

        glPushMatrix();
        glTranslated(-0.30, 1.23 + crouch, 0.0);
        glRotated(swing, 1.0, 0.0, 0.0);
        drawCuboid(-0.08, -0.50, -0.08, 0.08, 0.16, 0.08, bodyR, bodyG, bodyB);
        glPopMatrix();

        glPushMatrix();
        glTranslated(0.30, 1.23 + crouch, 0.0);
        glRotated(-swing + attackSwing, 1.0, 0.0, 0.0);
        glRotated(-attackSwing * 0.16, 0.0, 0.0, 1.0);
        drawCuboid(-0.08, -0.50, -0.08, 0.08, 0.16, 0.08, bodyR, bodyG, bodyB);
        if (showHeldItem && selectedBlock != GameConfig.AIR) {
            float[] heldColor = getHeldBlockColor(selectedBlock);
            glPushMatrix();
            glTranslated(0.0, -0.40, -0.08);
            drawCuboid(-0.06, -0.06, -0.06, 0.06, 0.06, 0.06, heldColor[0], heldColor[1], heldColor[2]);
            glPopMatrix();
        }
        glPopMatrix();

        float legR = legs == null ? 0.24f : legs[0];
        float legG = legs == null ? 0.30f : legs[1];
        float legB = legs == null ? 0.66f : legs[2];
        float footR = boots == null ? legR : boots[0];
        float footG = boots == null ? legG : boots[1];
        float footB = boots == null ? legB : boots[2];

        glPushMatrix();
        glTranslated(-0.12, legTop, 0.0);
        glRotated(-swing * 1.2, 1.0, 0.0, 0.0);
        drawCuboid(-0.09, legBottom, -0.09, 0.09, 0.0, 0.09, legR, legG, legB);
        drawCuboid(-0.095, legBottom, -0.095, 0.095, legBottom + 0.22, 0.095, footR, footG, footB);
        glPopMatrix();

        glPushMatrix();
        glTranslated(0.12, legTop, 0.0);
        glRotated(swing * 1.2, 1.0, 0.0, 0.0);
        drawCuboid(-0.09, legBottom, -0.09, 0.09, 0.0, 0.09, legR, legG, legB);
        drawCuboid(-0.095, legBottom, -0.095, 0.095, legBottom + 0.22, 0.095, footR, footG, footB);
        glPopMatrix();
    }

    private void drawPlayerEyes() {
        drawCuboid(-0.13, 0.00, -0.245, -0.055, 0.075, -0.225, 0.03f, 0.03f, 0.035f);
        drawCuboid(0.055, 0.00, -0.245, 0.13, 0.075, -0.225, 0.03f, 0.03f, 0.035f);
        drawCuboid(-0.12, 0.035, -0.248, -0.095, 0.060, -0.222, 0.95f, 0.95f, 0.92f);
        drawCuboid(0.065, 0.035, -0.248, 0.090, 0.060, -0.222, 0.95f, 0.95f, 0.92f);
    }

    private void drawCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue) {
        glBegin(GL_QUADS);
        shadeColor(red, green, blue, 0.80f);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);

        shadeColor(red, green, blue, 0.80f);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);

        shadeColor(red, green, blue, 1.00f);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, maxY, maxZ);

        shadeColor(red, green, blue, 0.56f);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, minY, minZ);

        shadeColor(red, green, blue, 0.72f);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, minY, minZ);

        shadeColor(red, green, blue, 0.72f);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glEnd();
    }

    private void drawFlatCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue) {
        glBegin(GL_QUADS);
        glColor3f(clampColor(red * currentSceneBrightness), clampColor(green * currentSceneBrightness), clampColor(blue * currentSceneBrightness));
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glEnd();
    }

    private void renderChunkBorders(PlayerState player) {
        if (!chunkBordersEnabled || player == null) {
            return;
        }

        int centerChunkX = Math.floorDiv((int) Math.floor(player.x), GameConfig.CHUNK_SIZE);
        int centerChunkZ = Math.floorDiv((int) Math.floor(player.z), GameConfig.CHUNK_SIZE);
        int centerSection = GameConfig.sectionIndexForY((int) Math.floor(player.y));
        int minSection = Math.max(0, centerSection - 1);
        int maxSection = Math.min(GameConfig.SECTION_COUNT - 1, centerSection + 1);
        int minY = GameConfig.sectionYForIndex(minSection);
        int maxY = Math.min(GameConfig.WORLD_MAX_Y + 1, GameConfig.sectionYForIndex(maxSection) + GameConfig.CHUNK_SIZE);

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glLineWidth(1.0f);
        glBegin(GL_LINES);

        for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {
            double minX = chunkX * GameConfig.CHUNK_SIZE;
            double maxX = minX + GameConfig.CHUNK_SIZE;
            for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {
                boolean currentChunk = chunkX == centerChunkX && chunkZ == centerChunkZ;
                double minZ = chunkZ * GameConfig.CHUNK_SIZE;
                double maxZ = minZ + GameConfig.CHUNK_SIZE;
                double drawMinX = minX - renderCameraX;
                double drawMaxX = maxX - renderCameraX;
                double drawMinZ = minZ - renderCameraZ;
                double drawMaxZ = maxZ - renderCameraZ;
                double drawMinY = minY - renderCameraY;
                double drawMaxY = maxY - renderCameraY;
                glColor4f(currentChunk ? 0.95f : 0.65f, currentChunk ? 0.82f : 0.62f, 0.18f, currentChunk ? 0.64f : 0.34f);
                emitChunkBorderVerticals(drawMinX, drawMinZ, drawMaxX, drawMaxZ, drawMinY, drawMaxY);
                for (int section = minSection; section <= maxSection + 1; section++) {
                    int y = section == maxSection + 1
                        ? maxY
                        : GameConfig.sectionYForIndex(section);
                    double drawY = y - renderCameraY;
                    glColor4f(0.16f, 0.38f, 0.78f, currentChunk ? 0.52f : 0.26f);
                    emitChunkBorderSquare(drawMinX, drawMinZ, drawMaxX, drawMaxZ, drawY);
                }
            }
        }

        glEnd();
        glDepthMask(true);
        glLineWidth(1.0f);
        glEnable(GL_DEPTH_TEST);
    }

    private void emitChunkBorderVerticals(double minX, double minZ, double maxX, double maxZ, double minY, double maxY) {
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
    }

    private void emitChunkBorderSquare(double minX, double minZ, double maxX, double maxZ, double y) {
        glVertex3d(minX, y, minZ);
        glVertex3d(maxX, y, minZ);
        glVertex3d(maxX, y, minZ);
        glVertex3d(maxX, y, maxZ);
        glVertex3d(maxX, y, maxZ);
        glVertex3d(minX, y, maxZ);
        glVertex3d(minX, y, maxZ);
        glVertex3d(minX, y, minZ);
    }

    private void renderHoveredOutline(RayHit hoveredBlock) {
        if (hoveredBlock == null) {
            return;
        }
        byte hoveredType = world.getBlock(hoveredBlock.x, hoveredBlock.y, hoveredBlock.z);
        if (world.isLiquidBlock(hoveredType)) {
            return;
        }

        glLineWidth(1.15f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.015f, 0.012f, 0.010f, 0.58f);

        double outlineOffset = 0.0025;
        BlockState state = world.getBlockState(hoveredBlock.x, hoveredBlock.y, hoveredBlock.z);
        if (hoveredType == GameConfig.OAK_DOOR) {
            int lowerY = Blocks.isDoorUpper(state) ? hoveredBlock.y - 1 : hoveredBlock.y;
            BlockState lower = world.getBlockState(hoveredBlock.x, lowerY, hoveredBlock.z);
            double[] bounds = doorSelectionBounds(lower);
            drawAabbOutline(
                hoveredBlock.x + bounds[0] - outlineOffset,
                lowerY - outlineOffset,
                hoveredBlock.z + bounds[2] - outlineOffset,
                hoveredBlock.x + bounds[3] + outlineOffset,
                lowerY + 2.0 + outlineOffset,
                hoveredBlock.z + bounds[5] + outlineOffset
            );
            finishHoveredOutline();
            return;
        }
        if (hoveredType == GameConfig.RED_BED) {
            int facing = Blocks.bedFacing(state);
            int footX = Blocks.isBedHead(state) ? hoveredBlock.x - facingDx(facing) : hoveredBlock.x;
            int footZ = Blocks.isBedHead(state) ? hoveredBlock.z - facingDz(facing) : hoveredBlock.z;
            int headX = footX + facingDx(facing);
            int headZ = footZ + facingDz(facing);
            drawAabbOutline(
                Math.min(footX, headX) - outlineOffset,
                hoveredBlock.y - outlineOffset,
                Math.min(footZ, headZ) - outlineOffset,
                Math.max(footX, headX) + 1.0 + outlineOffset,
                hoveredBlock.y + 0.56 + outlineOffset,
                Math.max(footZ, headZ) + 1.0 + outlineOffset
            );
            finishHoveredOutline();
            return;
        }
        double[] bounds = selectionBounds(hoveredType, state);
        double minX = hoveredBlock.x + bounds[0] - outlineOffset - renderCameraX;
        double minY = hoveredBlock.y + bounds[1] - outlineOffset - renderCameraY;
        double minZ = hoveredBlock.z + bounds[2] - outlineOffset - renderCameraZ;
        double maxX = hoveredBlock.x + bounds[3] + outlineOffset - renderCameraX;
        double maxY = hoveredBlock.y + bounds[4] + outlineOffset - renderCameraY;
        double maxZ = hoveredBlock.z + bounds[5] + outlineOffset - renderCameraZ;

        glBegin(GL_LINE_LOOP);
        glVertex3d(minX, minY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(minX, minY, maxZ);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glEnd();

        glBegin(GL_LINES);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glEnd();
        finishHoveredOutline();
    }

    private void finishHoveredOutline() {
        glLineWidth(1.0f);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawAabbOutline(double worldMinX, double worldMinY, double worldMinZ,
                                 double worldMaxX, double worldMaxY, double worldMaxZ) {
        double minX = worldMinX - renderCameraX;
        double minY = worldMinY - renderCameraY;
        double minZ = worldMinZ - renderCameraZ;
        double maxX = worldMaxX - renderCameraX;
        double maxY = worldMaxY - renderCameraY;
        double maxZ = worldMaxZ - renderCameraZ;

        glBegin(GL_LINE_LOOP);
        glVertex3d(minX, minY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(minX, minY, maxZ);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glEnd();

        glBegin(GL_LINES);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glEnd();
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
            case GameConfig.OAK_FENCE:
                return new double[]{0.375, 0.0, 0.375, 0.625, 1.5, 0.625};
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

    private int facingDx(int facing) {
        return facing == 1 ? 1 : (facing == 3 ? -1 : 0);
    }

    private int facingDz(int facing) {
        return facing == 2 ? 1 : (facing == 0 ? -1 : 0);
    }

    private void renderBreakingOverlay(RayHit breakingBlock, double breakingProgress) {
        if (breakingBlock == null || breakingProgress <= 0.0) {
            return;
        }

        double offset = 0.004 + breakingProgress * 0.004;
        byte block = world.getBlock(breakingBlock.x, breakingBlock.y, breakingBlock.z);
        BlockState state = world.getBlockState(breakingBlock.x, breakingBlock.y, breakingBlock.z);
        double[] bounds = selectionBounds(block, state);
        double baseX = breakingBlock.x;
        double baseY = breakingBlock.y;
        double baseZ = breakingBlock.z;
        if (block == GameConfig.OAK_DOOR) {
            if (Blocks.isDoorUpper(state)) {
                baseY -= 1.0;
            }
            bounds = doorSelectionBounds(world.getBlockState((int) baseX, (int) baseY, (int) baseZ));
            bounds[4] = 2.0;
        } else if (block == GameConfig.RED_BED) {
            int facing = Blocks.bedFacing(state);
            int footX = Blocks.isBedHead(state) ? breakingBlock.x - facingDx(facing) : breakingBlock.x;
            int footZ = Blocks.isBedHead(state) ? breakingBlock.z - facingDz(facing) : breakingBlock.z;
            int headX = footX + facingDx(facing);
            int headZ = footZ + facingDz(facing);
            baseX = Math.min(footX, headX);
            baseZ = Math.min(footZ, headZ);
            bounds = new double[]{0.0, 0.0, 0.0, Math.abs(headX - footX) + 1.0, 0.56, Math.abs(headZ - footZ) + 1.0};
        }
        double minX = baseX + bounds[0] - offset - renderCameraX;
        double minY = baseY + bounds[1] - offset - renderCameraY;
        double minZ = baseZ + bounds[2] - offset - renderCameraZ;
        double maxX = baseX + bounds[3] + offset - renderCameraX;
        double maxY = baseY + bounds[4] + offset - renderCameraY;
        double maxZ = baseZ + bounds[5] + offset - renderCameraZ;
        if (block == GameConfig.RED_BED) {
            minY = baseY + 0.02 - renderCameraY;
            maxY = baseY + 0.54 + offset - renderCameraY;
        }

        glColor4f(1.0f, 0.95f, 0.72f, (float) (0.05 + breakingProgress * 0.22));
        glBegin(GL_QUADS);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);

        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);

        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, maxY, maxZ);

        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, minY, minZ);

        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, minY, minZ);

        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glEnd();
    }

    private void renderOverlay(PlayerState player, PlayerInventory inventory, RayHit hoveredBlock, boolean paused, boolean inventoryOpen, int inventoryScreenMode, ContainerInventory chestContainer, FurnaceBlockEntity furnace, boolean deathScreenActive, int deathSelection, boolean mainMenuActive, int mainMenuScreen, boolean optionsOpenedFromPause, int mainMenuSelection, boolean mainMenuWorldActionsEnabled, String createWorldName, String createWorldSeed, int createWorldGameMode, boolean createWorldAllowCheats, int createWorldDifficulty, int createWorldTerrainPreset, int activeMenuTextField, String renameWorldName, String multiplayerName, String multiplayerHost, String multiplayerPort, String multiplayerStatus, int lanGameMode, boolean lanAllowCheats, List<WorldInfo> worlds, int selectedWorldIndex, int mainMenuScrollOffset, String loadedWorldName, boolean showDebugInfo, boolean hideHud, int pauseSelection, boolean gameModeSwitcherActive, int gameModeSelection, byte selectedBlock, int selectedSlot, int creativeTab, int creativeScrollOffset, boolean creativeMode, boolean thirdPersonView, int renderDistanceChunks, int fovDegrees, double timeOfDay, double mouseX, double mouseY, ChatSystem chat, boolean showPlayerList, List<PlayerListEntry> playerList) {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, framebufferWidth, framebufferHeight, 0.0, -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);

        boolean minimalHud = player.spectatorMode;
        boolean blockingOverlay = paused || inventoryOpen || deathScreenActive || mainMenuActive;
        if (!hideHud && !minimalHud && !inventoryOpen && !deathScreenActive && !mainMenuActive) {
            if (!paused) {
                drawCrosshair();
            }
            drawHotbar(inventory, selectedSlot);
            if (!player.creativeMode) {
                drawHealthBar(player.health, inventory);
                drawHungerBar(player.hunger);
                drawArmorBar(inventory);
                drawAirBar(player);
            }
        }
        if (inventoryOpen) {
            boolean creativeInventory = creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_PLAYER;
            renderInventoryScreen(player, inventory, creativeInventory, selectedSlot, creativeTab, creativeScrollOffset, inventoryScreenMode, chestContainer, furnace, mouseX, mouseY);
        }
        if (!blockingOverlay && player.fireTimer > 0.0) {
            renderFireOverlay(player.fireTimer);
        }
        if (!hideHud && showDebugInfo && !mainMenuActive && !deathScreenActive) {
            renderDebugOverlay(player, hoveredBlock, selectedBlock);
        }
        if (!mainMenuActive && !deathScreenActive) {
            renderChat(chat);
        }
        if (showPlayerList && !hideHud && !blockingOverlay) {
            renderPlayerListOverlay(playerList);
        }
        if (mainMenuActive) {
            renderMainMenu(mainMenuScreen, optionsOpenedFromPause, mainMenuSelection, mainMenuWorldActionsEnabled, createWorldName, createWorldSeed, createWorldGameMode, createWorldAllowCheats, createWorldDifficulty, createWorldTerrainPreset, activeMenuTextField, renameWorldName, multiplayerName, multiplayerHost, multiplayerPort, multiplayerStatus, lanGameMode, lanAllowCheats, worlds, selectedWorldIndex, mainMenuScrollOffset, loadedWorldName, renderDistanceChunks, fovDegrees);
        }
        if (deathScreenActive) {
            renderDeathScreen(deathSelection);
        }
        if (paused) {
            renderPauseMenu(pauseSelection);
        }
        if (gameModeSwitcherActive) {
            renderGameModeSwitcher(gameModeSelection);
        }

        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    private void renderChat(ChatSystem chat) {
        if (chat == null) {
            return;
        }
        List<ChatSystem.VisibleMessage> messages = chat.renderMessages();

        float uiScale = getChatUiScale();
        float lineHeight = 18.0f * uiScale;
        float x = 14.0f * uiScale;
        float textScale = uiScale * 0.78f;
        float maxTextWidth = Math.max(96.0f * uiScale, framebufferWidth - 36.0f * uiScale);
        int maxLines = Math.max(1, (int) ((framebufferHeight - 110.0f * uiScale) / lineHeight));
        List<ChatRenderLine> wrappedMessages = wrapChatMessages(messages, textScale, maxTextWidth, maxLines, chat.scrollOffset());
        float y = framebufferHeight - 78.0f * uiScale - wrappedMessages.size() * lineHeight;
        for (ChatRenderLine message : wrappedMessages) {
            float width = Math.min(framebufferWidth - 28.0f * uiScale, measureTextWidth(message.text, textScale) + 12.0f * uiScale);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            drawRect(x - 5.0f * uiScale, y - 12.0f * uiScale, width, 17.0f * uiScale, 0.02f, 0.02f, 0.03f, 0.44f * message.alpha);
            drawShadowText(x, y, textScale, message.text, 0.92f, 0.94f, 0.98f, message.alpha);
            y += lineHeight;
        }

        if (chat.isActive()) {
            String input = "> " + chat.inputText() + "_";
            float inputY = framebufferHeight - 34.0f * uiScale;
            List<ChatSystem.CommandSuggestion> suggestions = chat.commandSuggestions();
            if (!suggestions.isEmpty()) {
                float suggestionScale = uiScale * 0.72f;
                float suggestionLineHeight = 18.0f * uiScale;
                float suggestionWidth = 180.0f * uiScale;
                for (ChatSystem.CommandSuggestion suggestion : suggestions) {
                    suggestionWidth = Math.max(suggestionWidth, measureTextWidth(suggestion.text(), suggestionScale) + 14.0f * uiScale);
                }
                suggestionWidth = Math.min(framebufferWidth - 20.0f * uiScale, suggestionWidth);
                float suggestionY = inputY - 22.0f * uiScale - suggestions.size() * suggestionLineHeight;
                for (ChatSystem.CommandSuggestion suggestion : suggestions) {
                    glEnable(GL_BLEND);
                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    if (suggestion.selected()) {
                        drawRect(10.0f * uiScale, suggestionY, suggestionWidth, suggestionLineHeight,
                            0.28f, 0.34f, 0.44f, 0.94f);
                    } else {
                        drawRect(10.0f * uiScale, suggestionY, suggestionWidth, suggestionLineHeight,
                            0.02f, 0.02f, 0.03f, 0.88f);
                    }
                    String suggestionText = trimTextToWidth(
                        suggestion.text(), suggestionScale, suggestionWidth - 12.0f * uiScale
                    );
                    drawShadowText(16.0f * uiScale, suggestionY + 13.0f * uiScale, suggestionScale,
                        suggestionText, 0.94f, 0.96f, 1.0f);
                    suggestionY += suggestionLineHeight;
                }
            }
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            drawRect(10.0f * uiScale, inputY - 18.0f * uiScale, framebufferWidth - 20.0f * uiScale, 26.0f * uiScale, 0.02f, 0.02f, 0.03f, 0.74f);
            drawOutline(10.0f * uiScale, inputY - 18.0f * uiScale, framebufferWidth - 20.0f * uiScale, 26.0f * uiScale, uiScale, 0.50f, 0.64f, 0.82f, 0.88f);
            drawShadowText(18.0f * uiScale, inputY, uiScale * 0.86f, trimTextToWidth(input, uiScale * 0.86f, framebufferWidth - 36.0f * uiScale), 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderPlayerListOverlay(List<PlayerListEntry> players) {
        if (players == null || players.isEmpty()) {
            return;
        }
        float uiScale = getUiScale();
        int rows = Math.min(players.size(), 16);
        float width = Math.min(framebufferWidth - 40.0f * uiScale, 430.0f * uiScale);
        float rowHeight = 22.0f * uiScale;
        float headerHeight = 30.0f * uiScale;
        float height = headerHeight + (rows + 1) * rowHeight + 10.0f * uiScale;
        float x = (framebufferWidth - width) * 0.5f;
        float y = 26.0f * uiScale;
        float padding = 12.0f * uiScale;
        float textScale = uiScale * 0.72f;

        drawRect(x, y, width, height, 0.02f, 0.02f, 0.03f, 0.78f);
        drawOutline(x, y, width, height, uiScale, 0.50f, 0.64f, 0.82f, 0.80f);
        drawShadowText(x + padding, y + 20.0f * uiScale, uiScale * 0.86f, "Players", 1.0f, 1.0f, 1.0f);

        float tableY = y + headerHeight;
        float nameX = x + padding;
        float pingX = x + width - 166.0f * uiScale;
        float hpX = x + width - 100.0f * uiScale;
        float modeX = x + width - 58.0f * uiScale;
        drawShadowText(nameX, tableY + 14.0f * uiScale, textScale, "Name", 0.70f, 0.76f, 0.86f);
        drawShadowText(pingX, tableY + 14.0f * uiScale, textScale, "Ping", 0.70f, 0.76f, 0.86f);
        drawShadowText(hpX, tableY + 14.0f * uiScale, textScale, "HP", 0.70f, 0.76f, 0.86f);
        drawShadowText(modeX, tableY + 14.0f * uiScale, textScale, "Mode", 0.70f, 0.76f, 0.86f);

        for (int i = 0; i < rows; i++) {
            PlayerListEntry entry = players.get(i);
            float rowY = tableY + (i + 1) * rowHeight;
            if ((i & 1) == 0) {
                drawRect(x + 5.0f * uiScale, rowY - 12.0f * uiScale, width - 10.0f * uiScale, rowHeight, 0.12f, 0.14f, 0.18f, 0.36f);
            }
            String name = entry.name == null || entry.name.trim().isEmpty() ? "Player" : entry.name.trim();
            String ping = entry.pingMs < 0 ? "? ms" : entry.pingMs + " ms";
            String hp = Integer.toString((int) Math.round(Math.max(0.0, entry.health)));
            String mode = entry.gameMode == null || entry.gameMode.trim().isEmpty() ? "-" : entry.gameMode.trim();
            drawShadowText(nameX, rowY + 4.0f * uiScale, textScale, trimTextToWidth(name, textScale, pingX - nameX - 12.0f * uiScale), 0.94f, 0.96f, 1.0f);
            drawShadowText(pingX, rowY + 4.0f * uiScale, textScale, ping, 0.88f, 0.90f, 0.94f);
            drawShadowText(hpX, rowY + 4.0f * uiScale, textScale, hp, 0.95f, 0.72f, 0.72f);
            drawShadowText(modeX, rowY + 4.0f * uiScale, textScale, mode, 0.82f, 0.88f, 0.98f);
        }
    }

    private List<ChatRenderLine> wrapChatMessages(List<ChatSystem.VisibleMessage> messages, float scale, float maxWidth, int maxLines, int scrollOffset) {
        ArrayList<ChatRenderLine> wrapped = new ArrayList<>();
        for (ChatSystem.VisibleMessage message : messages) {
            ArrayList<String> lines = new ArrayList<>();
            wrapChatLine(message.text(), scale, maxWidth, lines);
            for (String line : lines) {
                wrapped.add(new ChatRenderLine(line, message.alpha()));
            }
        }
        int maxScrollOffset = Math.max(0, wrapped.size() - maxLines);
        int effectiveScrollOffset = Math.min(maxScrollOffset, Math.max(0, scrollOffset));
        int end = wrapped.size() - effectiveScrollOffset;
        int start = Math.max(0, end - maxLines);
        return new ArrayList<>(wrapped.subList(start, end));
    }

    private static final class ChatRenderLine {
        private final String text;
        private final float alpha;

        private ChatRenderLine(String text, float alpha) {
            this.text = text;
            this.alpha = alpha;
        }
    }

    private void wrapChatLine(String line, float scale, float maxWidth, ArrayList<String> output) {
        if (measureTextWidth(line, scale) <= maxWidth) {
            output.add(line);
            return;
        }
        String[] words = line.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (measureTextWidth(candidate, scale) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                output.add(current.toString());
                current.setLength(0);
            }
            if (measureTextWidth(word, scale) <= maxWidth) {
                current.append(word);
            } else {
                splitLongChatWord(word, scale, maxWidth, output);
            }
        }
        if (current.length() > 0) {
            output.add(current.toString());
        }
    }

    private void splitLongChatWord(String word, float scale, float maxWidth, ArrayList<String> output) {
        StringBuilder part = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String candidate = part.toString() + c;
            if (part.length() > 0 && measureTextWidth(candidate, scale) > maxWidth) {
                output.add(part.toString());
                part.setLength(0);
            }
            part.append(c);
        }
        if (part.length() > 0) {
            output.add(part.toString());
        }
    }

    private String trimTextToWidth(String text, float scale, float maxWidth) {
        if (measureTextWidth(text, scale) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = Math.max(0, text.length());
        while (end > 0 && measureTextWidth(text.substring(0, end) + ellipsis, scale) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private void drawHotbar(PlayerInventory inventory, int selectedSlot) {
        float uiScale = getHudInventoryScale();
        float slotSize = 31.0f * uiScale;
        float spacing = 0.0f;
        float totalWidth = slotSize * 9.0f + spacing * 8.0f;
        float startX = framebufferWidth * 0.5f - totalWidth * 0.5f;
        float y = framebufferHeight - 44.0f * uiScale;

        for (int slot = 0; slot < 9; slot++) {
            float x = startX + slot * (slotSize + spacing);
            drawItemSlot(x, y, slotSize, inventory.getHotbarStack(slot), slot == selectedSlot, false, null);
        }
    }

    private void drawCrosshair() {
        float uiScale = getUiScale();
        float centerX = framebufferWidth * 0.5f;
        float centerY = framebufferHeight * 0.5f;
        float halfSize = 9.0f * uiScale;
        glLineWidth(Math.max(1.4f, 1.8f * uiScale));
        glColor4f(1.0f, 1.0f, 1.0f, 0.95f);
        glBegin(GL_LINES);
        glVertex3d(centerX - halfSize, centerY, 0.0);
        glVertex3d(centerX + halfSize, centerY, 0.0);
        glVertex3d(centerX, centerY - halfSize, 0.0);
        glVertex3d(centerX, centerY + halfSize, 0.0);
        glEnd();
    }

    private void renderFireOverlay(double fireTimer) {
        float alpha = clampColor((float) Math.min(0.42, 0.16 + fireTimer * 0.04));
        float flameHeight = framebufferHeight * 0.30f;
        drawRect(0.0f, framebufferHeight - flameHeight, framebufferWidth, flameHeight, 1.0f, 0.24f, 0.02f, alpha);
        drawRect(0.0f, 0.0f, framebufferWidth * 0.08f, framebufferHeight, 0.95f, 0.08f, 0.01f, alpha * 0.55f);
        drawRect(framebufferWidth * 0.92f, 0.0f, framebufferWidth * 0.08f, framebufferHeight, 0.95f, 0.08f, 0.01f, alpha * 0.55f);
    }

    private void drawHealthBar(double health, PlayerInventory inventory) {
        float uiScale = getHudInventoryScale();
        float heartScale = 1.52f * uiScale;
        float spacing = 0.9f * uiScale;
        float slotSize = 31.0f * uiScale;
        float slotSpacing = 0.0f;
        float hotbarWidth = slotSize * 9.0f + slotSpacing * 8.0f;
        float startX = framebufferWidth * 0.5f - hotbarWidth * 0.5f;
        float y = framebufferHeight - 44.0f * uiScale - 14.0f * getUiScale();

        for (int heart = 0; heart < 10; heart++) {
            double threshold = (heart + 1) * 2.0;
            boolean filled = health >= threshold;
            boolean half = !filled && health >= threshold - 1.0;
            drawHeart(startX + heart * (heartScale * 8.0f + spacing), y, heartScale, filled, half);
        }
    }

    private void drawArmorBar(PlayerInventory inventory) {
        int armor = inventory.getArmorProtection();
        if (armor <= 0) {
            return;
        }

        float uiScale = getHudInventoryScale();
        float scale = 1.42f * uiScale;
        float spacing = 1.1f * uiScale;
        float slotSize = 31.0f * uiScale;
        float slotSpacing = 0.0f;
        float hotbarWidth = slotSize * 9.0f + slotSpacing * 8.0f;
        float startX = framebufferWidth * 0.5f - hotbarWidth * 0.5f;
        float y = framebufferHeight - 44.0f * uiScale - 32.0f * getUiScale();

        for (int shield = 0; shield < 10; shield++) {
            int point = (shield + 1) * 2;
            boolean filled = armor >= point;
            boolean half = !filled && armor == point - 1;
            drawArmorShield(startX + shield * (scale * 8.0f + spacing), y, scale, filled, half);
        }
    }

    private void drawHungerBar(double hunger) {
        float uiScale = getHudInventoryScale();
        float scale = 1.32f * uiScale;
        float spacing = 1.2f * uiScale;
        float slotSize = 31.0f * uiScale;
        float slotSpacing = 0.0f;
        float hotbarWidth = slotSize * 9.0f + slotSpacing * 8.0f;
        float startX = framebufferWidth * 0.5f + hotbarWidth * 0.5f - 10.0f * (scale * 7.0f + spacing);
        float y = framebufferHeight - 44.0f * uiScale - 14.0f * getUiScale();
        for (int food = 0; food < 10; food++) {
            double threshold = (food + 1) * 2.0;
            boolean filled = hunger >= threshold;
            boolean half = !filled && hunger >= threshold - 1.0;
            drawFoodIcon(startX + food * (scale * 7.0f + spacing), y, scale, filled, half);
        }
    }

    private void drawFoodIcon(float x, float y, float scale, boolean filled, boolean half) {
        drawFoodIconShape(x, y, scale, 0.24f, 0.24f, 0.26f, 0.72f, 1.0f);
        if (filled || half) {
            drawFoodIconShape(x, y, scale, 0.86f, 0.46f, 0.18f, 0.96f, filled ? 1.0f : 0.52f);
        }
    }

    private void renderFirstPersonHand3d(PlayerState player, byte selectedItem, boolean breakingActive) {
        double bob = Math.sin(player.cameraBobPhase * 1.7) * 0.045 * player.cameraBobAmount;
        double swingTime = player.handSwingTimer <= 0.0 ? 0.0 : clamp(1.0 - player.handSwingTimer / 0.22, 0.0, 1.0);
        double swing = swingTime <= 0.0 ? 0.0 : Math.sin(swingTime * Math.PI);
        double swingDip = swingTime <= 0.0 ? 0.0 : Math.sin(swingTime * Math.PI * 2.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_FOG);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glTranslated(0.76 + bob * 0.45 - swing * 0.05, -0.66 + (player.sneaking ? -0.05 : 0.0) + swing * 0.02 + swingDip * 0.025, -1.08 - swing * 0.06);
        glRotated(-8.0 - swing * 22.0, 1.0, 0.0, 0.0);
        glRotated(11.0 + swing * 12.0, 0.0, 1.0, 0.0);
        glRotated(2.0 + swingDip * 5.0, 0.0, 0.0, 1.0);

        drawFlatCuboid(-0.092, -0.48, -0.092, 0.092, 0.08, 0.092, 0.74f, 0.54f, 0.38f);
        drawFlatCuboid(-0.102, -0.52, -0.102, 0.102, -0.38, 0.102, 0.12f, 0.34f, 0.70f);
        drawFlatCuboid(-0.098, 0.06, -0.098, 0.098, 0.30, 0.098, 0.86f, 0.66f, 0.48f);

        if (selectedItem != GameConfig.AIR) {
            float[] heldColor = getHeldBlockColor(selectedItem);
            glPushMatrix();
            glTranslated(-0.05, 0.14, -0.16);
            glRotated(-10.0, 1.0, 0.0, 0.0);
            glRotated(-12.0, 0.0, 0.0, 1.0);
            if (isHeldToolItem(selectedItem)) {
                drawCuboid(-0.025, -0.18, -0.025, 0.025, 0.26, 0.025, 0.52f, 0.34f, 0.18f);
                drawCuboid(-0.105, 0.19, -0.030, 0.105, 0.29, 0.030, heldColor[0], heldColor[1], heldColor[2]);
            } else {
                drawCuboid(-0.09, -0.02, -0.09, 0.09, 0.16, 0.09, heldColor[0], heldColor[1], heldColor[2]);
            }
            glPopMatrix();
        }

        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glPopMatrix();
    }

    private boolean isHeldToolItem(byte item) {
        switch (item) {
            case InventoryItems.WOODEN_PICKAXE:
            case InventoryItems.STONE_PICKAXE:
            case InventoryItems.IRON_PICKAXE:
            case InventoryItems.DIAMOND_PICKAXE:
            case InventoryItems.NETHERITE_PICKAXE:
            case InventoryItems.WOODEN_SWORD:
            case InventoryItems.STONE_SWORD:
            case InventoryItems.IRON_SWORD:
            case InventoryItems.DIAMOND_SWORD:
            case InventoryItems.NETHERITE_SWORD:
            case InventoryItems.WOODEN_AXE:
            case InventoryItems.STONE_AXE:
            case InventoryItems.IRON_AXE:
            case InventoryItems.DIAMOND_AXE:
            case InventoryItems.NETHERITE_AXE:
            case InventoryItems.WOODEN_SHOVEL:
            case InventoryItems.STONE_SHOVEL:
            case InventoryItems.IRON_SHOVEL:
            case InventoryItems.DIAMOND_SHOVEL:
            case InventoryItems.NETHERITE_SHOVEL:
            case InventoryItems.WOODEN_HOE:
            case InventoryItems.STONE_HOE:
            case InventoryItems.IRON_HOE:
            case InventoryItems.DIAMOND_HOE:
            case InventoryItems.NETHERITE_HOE:
                return true;
            default:
                return false;
        }
    }

    private void drawAirBar(PlayerState player) {
        if (!player.headInWater && player.airUnits >= GameConfig.MAX_AIR_UNITS) {
            return;
        }

        float uiScale = getUiScale();
        float bubbleSize = 6.0f * uiScale;
        float spacing = 2.0f * uiScale;
        float slotSize = 31.0f * uiScale;
        float slotSpacing = 0.0f;
        float hotbarWidth = slotSize * 9.0f + slotSpacing * 8.0f;
        float startX = framebufferWidth * 0.5f - hotbarWidth * 0.5f;
        float y = framebufferHeight - 93.0f * uiScale;

        for (int bubble = 0; bubble < GameConfig.MAX_AIR_UNITS; bubble++) {
            boolean filled = bubble < player.airUnits;
            float x = startX + bubble * (bubbleSize + spacing);
            drawRect(x + bubbleSize * 0.15f, y, bubbleSize * 0.70f, bubbleSize, 0.30f, 0.74f, 0.94f, filled ? 0.96f : 0.24f);
            drawRect(x, y + bubbleSize * 0.20f, bubbleSize, bubbleSize * 0.58f, 0.36f, 0.84f, 1.0f, filled ? 0.96f : 0.24f);
        }
    }

    private void drawArmorShield(float x, float y, float scale, boolean filled, boolean half) {
        float alpha = filled || half ? 0.98f : 0.30f;
        float red = filled || half ? 0.72f : 0.36f;
        float green = filled || half ? 0.76f : 0.38f;
        float blue = filled || half ? 0.82f : 0.42f;
        drawRect(x + scale * 1.0f, y, scale * 5.0f, scale * 2.0f, red, green, blue, alpha);
        drawRect(x, y + scale * 2.0f, scale * 7.0f, scale * 3.0f, red * 0.92f, green * 0.92f, blue * 0.92f, alpha);
        drawRect(x + scale * 2.0f, y + scale * 5.0f, scale * 3.0f, scale * 2.0f, red * 0.82f, green * 0.82f, blue * 0.82f, alpha);
        if (half) {
            drawRect(x + scale * 4.0f, y, scale * 3.5f, scale * 7.0f, 0.05f, 0.05f, 0.06f, 0.45f);
        }
    }

    private void drawHeart(float x, float y, float scale, boolean filled, boolean half) {
        String[] pattern = {
            "0110110",
            "1111111",
            "1111111",
            "0111110",
            "0011100",
            "0001000"
        };
        for (int row = 0; row < pattern.length; row++) {
            for (int column = 0; column < pattern[row].length(); column++) {
                if (pattern[row].charAt(column) != '1') {
                    continue;
                }
                boolean redHalf = half && column <= 3;
                float[] color = (filled || redHalf)
                    ? new float[]{0.92f, 0.18f, 0.22f}
                    : new float[]{0.28f, 0.10f, 0.12f};
                drawRect(x + column * scale, y + row * scale, scale, scale, color[0], color[1], color[2], 0.98f);
            }
        }
    }

    private void drawFoodIconShape(float x, float y, float scale, float r, float g, float b, float alpha, float widthFactor) {
        float width = 5.0f * scale * Math.max(0.0f, Math.min(1.0f, widthFactor));
        if (width <= 0.0f) {
            return;
        }
        drawRect(x + 1.0f * scale, y + 1.0f * scale, width, 5.0f * scale, r, g, b, alpha);
        drawRect(x + 4.0f * scale, y - 1.0f * scale, Math.min(2.0f * scale, width), 3.0f * scale, 0.72f, 0.36f, 0.14f, alpha);
        drawRect(x + 2.0f * scale, y + 6.0f * scale, Math.min(3.0f * scale, width), 1.0f * scale, r * 0.75f, g * 0.75f, b * 0.75f, alpha);
    }

    private void renderDebugOverlay(PlayerState player, RayHit hoveredBlock, byte selectedBlock) {
        updateDebugInfo(player, hoveredBlock, selectedBlock);
        boolean russian = Settings.isRussian();
        ArrayList<DebugLine> left = new ArrayList<>();
        ArrayList<DebugLine> right = new ArrayList<>();

        String titleText = "TinyCraft v" + GameConfig.VERSION + " | " + format(debugInfo.fps)
            + " FPS | " + format(debugInfo.frameTimeMs) + " ms";
        left.add(debugInfo.hasLowFps() ? DebugLine.danger(titleText) : DebugLine.title(titleText));
        left.add(DebugLine.normal("XYZ: " + format(debugInfo.x) + " / " + format(debugInfo.y) + " / " + format(debugInfo.z)));
        left.add(DebugLine.muted((russian ? "Блок: " : "Block: ")
            + debugInfo.blockX + " / " + debugInfo.blockY + " / " + debugInfo.blockZ));
        left.add(DebugLine.normal((russian ? "Взгляд: " : "Facing: ") + debugInfo.facing
            + " | yaw " + format(debugInfo.yawDegrees) + " | pitch " + format(debugInfo.pitchDegrees)));
        left.add(DebugLine.normal((russian ? "Биом: " : "Biome: ") + debugInfo.biome));
        String lightText = (russian ? "Освещение: ~" : "Light: ~") + debugInfo.lightLevel + "/15 "
            + (debugInfo.skyVisible ? (russian ? "(небо)" : "(sky)") : (russian ? "(под землёй)" : "(underground)"));
        left.add(debugInfo.lightLevel <= 4 ? DebugLine.warning(lightText) : DebugLine.normal(lightText));
        left.add(DebugLine.muted((russian ? "Режим: " : "Mode: ") + debugInfo.gameMode
            + " | " + (russian ? "измерение " : "dimension ") + debugInfo.dimensionId));

        right.add(DebugLine.title((russian ? "Система" : "System")
            + " | " + framebufferWidth + "x" + framebufferHeight));
        right.add(DebugLine.muted("OS: " + compactDebugValue(debugInfo.osDescription, 58)));
        right.add(DebugLine.muted("Java: " + compactDebugValue(debugInfo.javaDescription, 58)));
        right.add(DebugLine.muted("CPU: " + compactDebugValue(debugInfo.cpuDescription, 52)
            + " | " + debugInfo.logicalProcessors + (russian ? " потоков" : " threads")));
        right.add(DebugLine.muted("GPU: " + compactDebugValue(debugInfo.gpuDescription, 58)));
        right.add(DebugLine.muted("OpenGL: " + compactDebugValue(debugInfo.openGlDescription, 58)));
        String memoryText = (russian ? "Память: " : "Memory: ") + debugInfo.usedMemoryMiB + " / "
            + debugInfo.allocatedMemoryMiB + " MiB (max " + debugInfo.maxMemoryMiB + ")";
        right.add(debugInfo.hasHighMemoryUsage() ? DebugLine.danger(memoryText) : DebugLine.normal(memoryText));

        right.add(DebugLine.title((russian ? "Чанк: " : "Chunk: ")
            + debugInfo.chunkX + " / " + debugInfo.chunkY + " / " + debugInfo.chunkZ));
        right.add(DebugLine.muted((russian ? "Локально: " : "Local: ")
            + debugInfo.localX + " / " + debugInfo.localY + " / " + debugInfo.localZ));
        String statusText = (russian ? "Статус: " : "Status: ") + debugInfo.chunkStatus;
        right.add("FULL".equals(debugInfo.chunkStatus) ? DebugLine.normal(statusText) : DebugLine.warning(statusText));
        right.add(DebugLine.muted((russian ? "Регион: " : "Region: ") + debugInfo.region));
        right.add(DebugLine.muted((russian ? "Поверхность: генератор " : "Surface: generated ")
            + debugInfo.generatedSurface + (russian ? " | мир " : " | actual ") + debugInfo.actualSurface));
        if (debugInfo.hasTargetBlock) {
            right.add(DebugLine.normal((russian ? "Цель: " : "Target: ") + debugInfo.targetName));
            right.add(DebugLine.muted(debugInfo.targetId + " @ " + debugInfo.targetX + ", "
                + debugInfo.targetY + ", " + debugInfo.targetZ + " data=" + debugInfo.targetStateData));
        } else {
            right.add(DebugLine.muted(russian ? "Цель: блока нет" : "Target: no block"));
        }
        right.add(DebugLine.normal((russian ? "В руке: " : "Held: ") + debugInfo.heldItemName));
        int queuedMeshes = pendingDirtyMeshBuildKeys.size() + queuedMeshBuildKeys.size();
        String meshText = (russian ? "Меши: " : "Meshes: ") + "resident=" + residentChunkMeshCount()
            + " queued=" + queuedMeshes + " active=" + meshBuildsInFlight.size();
        right.add(queuedMeshes > 256 ? DebugLine.warning(meshText) : DebugLine.muted(meshText));
        right.add(DebugLine.muted((russian ? "Отрисовка: " : "Draws: ")
            + "opaque=" + debugOpaqueRegionDrawCallsLastFrame
            + " transparent=" + debugTransparentChunkDrawCallsLastFrame));
        right.add(DebugLine.muted("Seed: " + debugInfo.seed));

        drawDebugPanels(left, right);
    }

    private void updateDebugInfo(PlayerState player, RayHit hoveredBlock, byte selectedBlock) {
        debugInfo.fps = debugFps;
        debugInfo.frameTimeMs = DebugInfo.frameTimeMs(debugFps);
        debugInfo.x = player.x;
        debugInfo.y = player.y;
        debugInfo.z = player.z;
        debugInfo.yawDegrees = Math.toDegrees(player.yaw);
        debugInfo.pitchDegrees = Math.toDegrees(player.pitch);
        debugInfo.blockX = (int) Math.floor(player.x);
        debugInfo.blockY = (int) Math.floor(player.y);
        debugInfo.blockZ = (int) Math.floor(player.z);
        debugInfo.chunkX = Math.floorDiv(debugInfo.blockX, GameConfig.CHUNK_SIZE);
        debugInfo.chunkY = GameConfig.sectionIndexForY(debugInfo.blockY);
        debugInfo.chunkZ = Math.floorDiv(debugInfo.blockZ, GameConfig.CHUNK_SIZE);
        debugInfo.localX = Math.floorMod(debugInfo.blockX, GameConfig.CHUNK_SIZE);
        debugInfo.localY = GameConfig.localYForWorldY(debugInfo.blockY);
        debugInfo.localZ = Math.floorMod(debugInfo.blockZ, GameConfig.CHUNK_SIZE);
        debugInfo.dimensionId = player.dimensionId;
        debugInfo.facing = facingName(player.yaw);
        debugInfo.gameMode = player.spectatorMode
            ? (Settings.isRussian() ? "Наблюдатель" : "Spectator")
            : player.creativeMode
                ? (Settings.isRussian() ? "Творческий" : "Creative")
                : (Settings.isRussian() ? "Выживание" : "Survival");
        debugInfo.heldItemName = selectedBlock == GameConfig.AIR
            ? (Settings.isRussian() ? "пусто" : "empty")
            : InventoryItems.name(selectedBlock);

        debugInfo.hasTargetBlock = hoveredBlock != null;
        if (hoveredBlock != null) {
            byte targetBlock = world.getBlock(hoveredBlock.x, hoveredBlock.y, hoveredBlock.z);
            BlockType targetType = Blocks.typeFromLegacyId(targetBlock);
            BlockState targetState = world.getBlockState(hoveredBlock.x, hoveredBlock.y, hoveredBlock.z);
            debugInfo.targetX = hoveredBlock.x;
            debugInfo.targetY = hoveredBlock.y;
            debugInfo.targetZ = hoveredBlock.z;
            debugInfo.targetName = InventoryItems.name(targetBlock);
            debugInfo.targetId = targetType.namespacedId;
            debugInfo.targetStateData = targetState.data;
        }

        if (debugInfo.shouldRefreshSlow(System.nanoTime(), debugInfo.chunkX, debugInfo.chunkY, debugInfo.chunkZ)) {
            debugInfo.biome = world.getBiomeName(debugInfo.blockX, debugInfo.blockZ);
            debugInfo.generatedSurface = world.getGeneratedSurfaceHeight(debugInfo.blockX, debugInfo.blockZ);
            debugInfo.actualSurface = world.getActualSurfaceHeight(debugInfo.blockX, debugInfo.blockZ);
            debugInfo.chunkStatus = world.getChunkStatus(debugInfo.chunkX, debugInfo.chunkZ).name();
            debugInfo.region = world.getRegionFileName(debugInfo.chunkX, debugInfo.chunkZ);
            debugInfo.seed = Long.toUnsignedString(world.getSeed(), 16);
            Runtime runtime = Runtime.getRuntime();
            debugInfo.refreshMemory(runtime);
            debugInfo.refreshSystemInfo(
                runtime,
                safeOpenGlString(GL_VENDOR),
                safeOpenGlString(GL_RENDERER),
                safeOpenGlString(GL_VERSION)
            );
        }
        debugInfo.skyVisible = debugInfo.blockY >= debugInfo.actualSurface;
        debugInfo.lightLevel = DebugInfo.approximateLightLevel(currentDaylight, debugInfo.skyVisible);
    }

    private void drawDebugPanels(List<DebugLine> left, List<DebugLine> right) {
        float scale = DebugInfo.overlayScale(getInventoryUiScale(), framebufferWidth);
        float margin = 8.0f * scale;
        float gap = 8.0f * scale;
        float leftWidth = debugPanelWidth(left, scale);
        float rightWidth = debugPanelWidth(right, scale);
        float leftHeight = debugPanelHeight(left, scale);
        boolean sideBySide = leftWidth + rightWidth + margin * 3.0f <= framebufferWidth;
        float rightX = framebufferWidth - margin - rightWidth;
        float rightY = sideBySide ? margin : margin + leftHeight + gap;
        drawDebugPanel(left, margin, margin, leftWidth, scale, false);
        drawDebugPanel(right, rightX, rightY, rightWidth, scale, true);
    }

    private float debugPanelWidth(List<DebugLine> lines, float scale) {
        float width = 0.0f;
        for (DebugLine line : lines) {
            width = Math.max(width, measureTextWidth(line.text, scale));
        }
        return width + 16.0f * scale;
    }

    private float debugPanelHeight(List<DebugLine> lines, float scale) {
        return 12.0f * scale + lines.size() * 18.0f * scale;
    }

    private void drawDebugPanel(List<DebugLine> lines, float x, float y, float width, float scale,
            boolean rightAligned) {
        float padding = 8.0f * scale;
        float textY = y + 7.0f * scale;
        for (DebugLine line : lines) {
            float textWidth = measureTextWidth(line.text, scale);
            float textX = DebugInfo.alignedTextX(x, width, padding, textWidth, rightAligned);
            drawShadowText(textX, textY, scale, line.text, line.red, line.green, line.blue);
            textY += 18.0f * scale;
        }
    }

    private String safeOpenGlString(int name) {
        String value = glGetString(name);
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    private String compactDebugValue(String value, int maxCharacters) {
        if (value == null || value.length() <= maxCharacters) {
            return value == null ? "unknown" : value;
        }
        return value.substring(0, Math.max(1, maxCharacters - 3)) + "...";
    }

    private String facingName(double yaw) {
        double degrees = Math.toDegrees(yaw);
        double normalized = ((degrees % 360.0) + 360.0) % 360.0;
        if (normalized >= 45.0 && normalized < 135.0) {
            return Settings.isRussian() ? "юг (+Z)" : "south (+Z)";
        }
        if (normalized >= 135.0 && normalized < 225.0) {
            return Settings.isRussian() ? "запад (-X)" : "west (-X)";
        }
        if (normalized >= 225.0 && normalized < 315.0) {
            return Settings.isRussian() ? "север (-Z)" : "north (-Z)";
        }
        return Settings.isRussian() ? "восток (+X)" : "east (+X)";
    }

    private static final class DebugLine {
        final String text;
        final float red;
        final float green;
        final float blue;

        DebugLine(String text, float red, float green, float blue) {
            this.text = text;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        static DebugLine title(String text) {
            return new DebugLine(text, 0.62f, 0.88f, 1.0f);
        }

        static DebugLine normal(String text) {
            return new DebugLine(text, 0.96f, 0.96f, 0.96f);
        }

        static DebugLine muted(String text) {
            return new DebugLine(text, 0.72f, 0.78f, 0.84f);
        }

        static DebugLine warning(String text) {
            return new DebugLine(text, 1.0f, 0.72f, 0.24f);
        }

        static DebugLine danger(String text) {
            return new DebugLine(text, 1.0f, 0.30f, 0.24f);
        }
    }

    private void renderPauseMenu(int pauseSelection) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.0f, 0.0f, 0.0f, 0.42f);
        drawCenteredShadowText(74.0f * uiScale, uiScale * 1.2f, Settings.isRussian() ? "\u041c\u0435\u043d\u044e \u0438\u0433\u0440\u044b" : "Game menu", 1.0f, 1.0f, 1.0f);

        float wideWidth = 398.0f * uiScale;
        float buttonHeight = 38.0f * uiScale;
        float centerX = framebufferWidth * 0.5f;
        float y = 142.0f * uiScale;
        String[] pauseOptions = GameConfig.pauseOptions();
        for (int i = 0; i < pauseOptions.length; i++) {
            drawMenuButton(centerX - wideWidth * 0.5f, y + i * 52.0f * uiScale,
                wideWidth, buttonHeight, pauseOptions[i], pauseSelection == i, true, uiScale * 0.82f);
        }
    }

    private void drawSettingsSlider(int optionIndex, float y, float uiScale, String label, int value, int min, int max, boolean selected) {
        double knobT = (value - min) / (double) Math.max(1, max - min);
        drawSettingsSlider(optionIndex, y, uiScale, label, knobT, Integer.toString(value), selected);
    }

    private void drawSettingsSlider(int optionIndex, float y, float uiScale, String label, double normalizedValue, String displayValue, boolean selected) {
        float sliderWidth = optionsControlWidth(uiScale);
        float sliderHeight = 8.0f * uiScale;
        float x = optionsControlX(optionIndex, uiScale);
        float knobT = (float) clamp(normalizedValue, 0.0, 1.0);
        float knobX = x + knobT * sliderWidth;
        String text = label + ": " + displayValue;
        float textScale = text.length() > 25 ? uiScale * 0.70f : uiScale * 0.78f;
        drawShadowText(x + sliderWidth * 0.5f - measureTextWidth(text, textScale) * 0.5f, y - 12.0f * uiScale, textScale, text, selected ? 1.0f : 0.86f, selected ? 0.94f : 0.86f, selected ? 0.68f : 0.86f);
        drawRect(x, y, sliderWidth, sliderHeight, 0.20f, 0.22f, 0.24f, 0.95f);
        drawRect(x, y, sliderWidth * knobT, sliderHeight, 0.82f, 0.78f, 0.52f, 0.98f);
        drawRect(knobX - 5.0f * uiScale, y - 7.0f * uiScale, 10.0f * uiScale, 22.0f * uiScale, 0.92f, 0.90f, 0.72f, 0.98f);
        drawOutline(x, y, sliderWidth, sliderHeight, uiScale, selected ? 0.94f : 0.0f, selected ? 0.86f : 0.0f, selected ? 0.55f : 0.0f, selected ? 0.98f : 0.9f);
    }

    private void renderDeathScreen(int deathSelection) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.09f, 0.0f, 0.0f, 0.72f);
        drawCenteredShadowText(118.0f * uiScale, uiScale * 2.0f, Settings.isRussian() ? "\u0412\u044b \u043f\u043e\u0433\u0438\u0431\u043b\u0438!" : "You Died!", 0.96f, 0.22f, 0.22f);
        drawCenteredShadowText(176.0f * uiScale, uiScale, Settings.isRussian() ? "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435, \u0447\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c \u0434\u0430\u043b\u044c\u0448\u0435." : "Survival run ended. Choose what to do next.", 0.95f, 0.95f, 0.95f);
        drawCenteredMenuButtons(GameConfig.deathOptions(), deathSelection, 236.0f, 0.88f, 0.88f, 0.88f);
    }

    private void renderMainMenu(int menuScreen, boolean optionsOpenedFromPause, int mainMenuSelection, boolean mainMenuWorldActionsEnabled, String createWorldName, String createWorldSeed, int createWorldGameMode, boolean createWorldAllowCheats, int createWorldDifficulty, int createWorldTerrainPreset, int activeMenuTextField, String renameWorldName, String multiplayerName, String multiplayerHost, String multiplayerPort, String multiplayerStatus, int lanGameMode, boolean lanAllowCheats, List<WorldInfo> worlds, int selectedWorldIndex, int scrollOffset, String loadedWorldName, int renderDistanceChunks, int fovDegrees) {
        float uiScale = Math.max(1.0f, getUiScale());
        if (menuPanoramaReady || optionsOpenedFromPause) {
            drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.02f, 0.025f, 0.03f,
                menuScreen == GameConfig.MENU_SCREEN_MAIN ? 0.36f : 0.58f);
        } else {
            drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.02f, 0.025f, 0.03f, 1.0f);
        }

        if (menuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER) {
            renderSingleplayerMenu(mainMenuSelection, mainMenuWorldActionsEnabled, worlds, selectedWorldIndex, scrollOffset);
        } else if (menuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD) {
            renderCreateWorldMenu(mainMenuSelection, createWorldName, createWorldSeed, createWorldGameMode, createWorldAllowCheats, createWorldDifficulty, createWorldTerrainPreset, activeMenuTextField);
        } else if (menuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
            renderRenameWorldMenu(mainMenuSelection, renameWorldName, activeMenuTextField);
        } else if (menuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
            renderMultiplayerMenu(mainMenuSelection, multiplayerName, multiplayerHost, multiplayerPort, multiplayerStatus, activeMenuTextField);
        } else if (menuScreen == GameConfig.MENU_SCREEN_OPEN_LAN) {
            renderOpenLanMenu(mainMenuSelection, lanGameMode, lanAllowCheats);
        } else if (menuScreen == GameConfig.MENU_SCREEN_DISCONNECTED) {
            renderDisconnectedMenu(mainMenuSelection, multiplayerStatus);
        } else if (menuScreen == GameConfig.MENU_SCREEN_OPTIONS) {
            renderOptionsMenu(mainMenuSelection, renderDistanceChunks, fovDegrees);
        } else {
            drawCenteredShadowText(116.0f * uiScale, uiScale * 2.4f, "TinyCraft", 1.0f, 1.0f, 1.0f);
            renderMainMenuHome(mainMenuSelection, worlds, selectedWorldIndex, loadedWorldName);
        }
    }

    private void renderMainMenuHome(int mainMenuSelection, List<WorldInfo> worlds, int selectedWorldIndex, String loadedWorldName) {
        float uiScale = Math.max(1.0f, getUiScale());
        float actionWidth = 280.0f * uiScale;
        float actionHeight = 46.0f * uiScale;
        float actionsX = framebufferWidth * 0.5f - actionWidth * 0.5f;
        float actionsY = framebufferHeight * 0.5f - 18.0f * uiScale;
        String[] actions = GameConfig.worldMenuActions();
        for (int i = 0; i < actions.length; i++) {
            float boxY = actionsY + i * 58.0f * uiScale;
            boolean selected = i == mainMenuSelection;
            drawRect(actionsX, boxY, actionWidth, actionHeight,
                selected ? 0.86f : 0.58f,
                selected ? 0.82f : 0.58f,
                selected ? 0.58f : 0.58f,
                0.98f);
            drawOutline(actionsX, boxY, actionWidth, actionHeight, uiScale, 0.0f, 0.0f, 0.0f, 1.0f);
            drawText(actionsX + actionWidth * 0.5f - measureTextWidth(actions[i], uiScale * 0.9f) * 0.5f, boxY + 29.0f * uiScale, uiScale * 0.9f, actions[i], 0.10f, 0.10f, 0.12f);
        }

    }

    private void renderSingleplayerMenu(int mainMenuSelection, boolean mainMenuWorldActionsEnabled, List<WorldInfo> worlds, int selectedWorldIndex, int scrollOffset) {
        float uiScale = Math.max(1.0f, getUiScale());
        float headerHeight = 66.0f * uiScale;
        float footerHeight = 82.0f * uiScale;
        drawRect(0.0f, 0.0f, framebufferWidth, headerHeight, 0.03f, 0.035f, 0.04f, 1.0f);
        drawRect(0.0f, headerHeight, framebufferWidth, framebufferHeight - headerHeight - footerHeight, 0.0f, 0.0f, 0.0f, 0.38f);
        drawRect(0.0f, framebufferHeight - footerHeight, framebufferWidth, footerHeight, 0.03f, 0.035f, 0.04f, 1.0f);
        drawCenteredShadowText(32.0f * uiScale, uiScale * 1.0f, Settings.isRussian() ? "\u0412\u044b\u0431\u043e\u0440 \u043c\u0438\u0440\u0430" : "Select World", 0.96f, 0.96f, 0.96f);

        float panelWidth = framebufferWidth - 160.0f * uiScale;
        float panelX = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float listX = panelX + 28.0f * uiScale;
        float listY = 96.0f * uiScale;
        float listWidth = panelWidth - 56.0f * uiScale;
        float rowHeight = 36.0f * uiScale;

        int visibleRows = Math.min(GameConfig.WORLD_MENU_VISIBLE_ROWS, Math.max(0, worlds.size() - scrollOffset));
        if (worlds.isEmpty()) {
            drawShadowText(listX, listY + 30.0f * uiScale, uiScale * 0.84f, Settings.isRussian() ? "\u041c\u0438\u0440\u043e\u0432 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442. \u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u0418\u0433\u0440\u0430\u0442\u044c, \u0447\u0442\u043e\u0431\u044b \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043d\u043e\u0432\u044b\u0439." : "No worlds yet. Play will create a new one.", 0.86f, 0.88f, 0.90f);
        }
        for (int row = 0; row < visibleRows; row++) {
            int worldIndex = scrollOffset + row;
            WorldInfo info = worlds.get(worldIndex);
            float rowY = listY + row * (rowHeight + 8.0f * uiScale);
            boolean selected = worldIndex == selectedWorldIndex;
            drawRect(listX, rowY, listWidth, rowHeight,
                selected ? 0.38f : 0.17f,
                selected ? 0.42f : 0.18f,
                selected ? 0.48f : 0.20f,
                selected ? 0.96f : 0.88f);
            drawOutline(listX, rowY, listWidth, rowHeight, uiScale, selected ? 0.92f : 0.38f, selected ? 0.88f : 0.40f, selected ? 0.68f : 0.44f, 0.92f);
            drawShadowText(listX + 12.0f * uiScale, rowY + 15.0f * uiScale, uiScale * 0.74f, info.name, 0.98f, 0.98f, 0.98f);
            drawShadowText(listX + 12.0f * uiScale, rowY + 30.0f * uiScale, uiScale * 0.50f, (Settings.isRussian() ? "\u0418\u0437\u043c\u0435\u043d\u0435\u043d: " : "Modified: ") + formatWorldDate(info.lastModifiedMillis) + "  " + gameModeName(info.gameMode), 0.72f, 0.76f, 0.80f);
        }

        float actionWidth = 118.0f * uiScale;
        float actionHeight = 38.0f * uiScale;
        float gap = 10.0f * uiScale;
        String[] singleplayerActions = GameConfig.singleplayerActions();
        float startX = framebufferWidth * 0.5f - (actionWidth * singleplayerActions.length + gap * (singleplayerActions.length - 1)) * 0.5f;
        float actionY = framebufferHeight - 58.0f * uiScale;
        for (int i = 0; i < singleplayerActions.length; i++) {
            float x = startX + i * (actionWidth + gap);
            boolean selected = i == mainMenuSelection;
            boolean enabled = i == 1 || i == 4 || (mainMenuWorldActionsEnabled && !worlds.isEmpty());
            drawRect(x, actionY, actionWidth, actionHeight,
                enabled ? (selected ? 0.86f : 0.58f) : 0.28f,
                enabled ? (selected ? 0.82f : 0.58f) : 0.29f,
                enabled ? (selected ? 0.58f : 0.58f) : 0.31f,
                enabled ? 0.98f : 0.70f);
            drawOutline(x, actionY, actionWidth, actionHeight, uiScale, enabled ? 0.0f : 0.12f, enabled ? 0.0f : 0.12f, enabled ? 0.0f : 0.14f, enabled ? 1.0f : 0.72f);
            String label = singleplayerActions[i];
            float labelScale = label.length() > 8 ? uiScale * 0.68f : uiScale * 0.78f;
            drawText(x + actionWidth * 0.5f - measureTextWidth(label, labelScale) * 0.5f, actionY + 24.0f * uiScale, labelScale, label, enabled ? 0.10f : 0.50f, enabled ? 0.10f : 0.52f, enabled ? 0.12f : 0.56f);
        }
    }

    private void renderOptionsMenu(int mainMenuSelection, int renderDistanceChunks, int fovDegrees) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(Math.max(42.0f * uiScale, optionsRenderDistanceSliderY(uiScale) - 82.0f * uiScale), uiScale * 1.05f, Settings.isRussian() ? "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438" : "Options", 0.96f, 0.96f, 0.96f);

        drawSettingsSlider(0, optionsRenderDistanceSliderY(uiScale), uiScale, Settings.isRussian() ? "\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u043f\u0440\u043e\u0440\u0438\u0441\u043e\u0432\u043a\u0438" : "Render Distance", renderDistanceChunks, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS, mainMenuSelection == 0);
        drawSettingsSlider(1, optionsFovSliderY(uiScale), uiScale, Settings.isRussian() ? "\u041f\u043e\u043b\u0435 \u0437\u0440\u0435\u043d\u0438\u044f" : "FOV", fovDegrees, 55, 100, mainMenuSelection == 1);
        drawSettingsSlider(2, optionsBrightnessSliderY(uiScale), uiScale, Settings.isRussian() ? "\u042f\u0440\u043a\u043e\u0441\u0442\u044c" : "Brightness", Settings.brightness, 0, 100, mainMenuSelection == 2);
        drawSettingsSlider(3, optionsVolumeSliderY(uiScale), uiScale, Settings.isRussian() ? "\u0417\u0432\u0443\u043a" : "Sound", Settings.masterVolume, 0, 100, mainMenuSelection == 3);
        drawSettingsSlider(4, optionsFpsSliderY(uiScale), uiScale,
            Settings.isRussian() ? "\u041c\u0430\u043a\u0441. \u0447\u0430\u0441\u0442\u043e\u0442\u0430 \u043a\u0430\u0434\u0440\u043e\u0432" : "Max Framerate",
            Settings.maxFpsSliderPercent(Settings.savedMaxFps), Settings.maxFpsDisplayValue(Settings.savedMaxFps), mainMenuSelection == 4);
        drawSettingsSlider(5, optionsGuiScaleSliderY(uiScale), uiScale,
            Settings.isRussian() ? "\u041c\u0430\u0441\u0448\u0442\u0430\u0431 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430" : "GUI Scale",
            Settings.guiScale / 4.0, Settings.guiScaleDisplayValue(), mainMenuSelection == 5);

        float actionWidth = optionsControlWidth(uiScale);
        float actionHeight = 46.0f * uiScale;
        float graphicsX = optionsControlX(6, uiScale);
        drawMenuButton(graphicsX, optionsGraphicsButtonY(uiScale), actionWidth, actionHeight,
            (Settings.isRussian() ? "\u0413\u0440\u0430\u0444\u0438\u043a\u0430: " : "Graphics: ") + (Settings.goodGraphics() ? (Settings.isRussian() ? "\u0425\u043e\u0440\u043e\u0448\u043e" : "Fancy") : (Settings.isRussian() ? "\u0411\u044b\u0441\u0442\u0440\u043e" : "Fast")),
            mainMenuSelection == 6, true, uiScale * 0.80f);
        float languageX = optionsControlX(7, uiScale);
        drawMenuButton(languageX, optionsLanguageButtonY(uiScale), actionWidth, actionHeight,
            (Settings.isRussian() ? "\u042f\u0437\u044b\u043a: \u0420\u0443\u0441\u0441\u043a\u0438\u0439" : "Language: English"),
            mainMenuSelection == 7, true, uiScale * 0.80f);

        float actionsX = framebufferWidth * 0.5f - actionWidth * 0.5f;
        float boxY = optionsBackButtonY(uiScale);
        boolean selected = mainMenuSelection == 8;
        drawRect(actionsX, boxY, actionWidth, actionHeight,
            selected ? 0.86f : 0.58f,
            selected ? 0.82f : 0.58f,
            selected ? 0.58f : 0.58f,
            0.98f);
        drawOutline(actionsX, boxY, actionWidth, actionHeight, uiScale, 0.0f, 0.0f, 0.0f, 1.0f);
        String backLabel = Settings.isRussian() ? "\u041d\u0430\u0437\u0430\u0434" : "Back";
        drawText(actionsX + actionWidth * 0.5f - measureTextWidth(backLabel, uiScale * 0.9f) * 0.5f, boxY + 29.0f * uiScale, uiScale * 0.9f, backLabel, 0.10f, 0.10f, 0.12f);
    }
    private void renderCreateWorldMenu(int mainMenuSelection, String worldName, String seedText, int gameMode, boolean allowCheats, int difficulty, int terrainPreset, int activeTextField) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(46.0f * uiScale, uiScale * 1.0f, Settings.isRussian() ? "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u043d\u043e\u0432\u044b\u0439 \u043c\u0438\u0440" : "Create New World", 0.96f, 0.96f, 0.96f);

        float panelWidth = Math.min(720.0f * uiScale, framebufferWidth - 120.0f * uiScale);
        float panelX = framebufferWidth * 0.5f - panelWidth * 0.5f;
        drawShadowText(panelX, 104.0f * uiScale, uiScale * 0.78f, Settings.isRussian() ? "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043c\u0438\u0440\u0430" : "World Name", 0.82f, 0.82f, 0.82f);
        drawTextField(panelX, 126.0f * uiScale, panelWidth, 36.0f * uiScale, worldName, activeTextField == 0);

        float buttonWidth = 340.0f * uiScale;
        float buttonHeight = 44.0f * uiScale;
        float gap = 18.0f * uiScale;
        float startX = framebufferWidth * 0.5f - (buttonWidth * 2.0f + gap) * 0.5f;
        drawMenuButton(startX, 224.0f * uiScale, buttonWidth, buttonHeight, (Settings.isRussian() ? "\u0420\u0435\u0436\u0438\u043c: " : "Game Mode: ") + gameModeName(gameMode), false, true, uiScale * 0.80f);
        drawMenuButton(startX + buttonWidth + gap, 224.0f * uiScale, buttonWidth, buttonHeight, (Settings.isRussian() ? "\u0421\u043b\u043e\u0436\u043d\u043e\u0441\u0442\u044c: " : "Difficulty: ") + GameConfig.difficultyOptions()[Math.max(0, Math.min(difficulty, GameConfig.difficultyOptions().length - 1))], false, true, uiScale * 0.80f);
        int terrainIndex = Math.max(0, Math.min(terrainPreset, GameConfig.terrainPresetOptions().length - 1));
        drawMenuButton(startX, 282.0f * uiScale, buttonWidth, buttonHeight, (Settings.isRussian() ? "\u0422\u0438\u043f \u043c\u0438\u0440\u0430: " : "World Type: ") + GameConfig.terrainPresetOptions()[terrainIndex], false, true, uiScale * 0.68f);
        String cheatsLabel = (Settings.isRussian() ? "\u0427\u0438\u0442\u044b: " : "Allow Cheats: ")
            + (allowCheats ? (Settings.isRussian() ? "\u0412\u043a\u043b" : "On") : (Settings.isRussian() ? "\u0412\u044b\u043a\u043b" : "Off"));
        drawMenuButton(startX + buttonWidth + gap, 282.0f * uiScale, buttonWidth, buttonHeight, cheatsLabel, false, true, uiScale * 0.72f);
        drawShadowText(startX, 340.0f * uiScale, uiScale * 0.62f, gameModeHelp(gameMode), 0.70f, 0.72f, 0.76f);

        drawShadowText(panelX, 364.0f * uiScale, uiScale * 0.78f, Settings.isRussian() ? "Seed \u0433\u0435\u043d\u0435\u0440\u0430\u0442\u043e\u0440\u0430 \u043c\u0438\u0440\u0430" : "Seed for the World Generator", 0.82f, 0.82f, 0.82f);
        drawTextField(panelX, 386.0f * uiScale, panelWidth, 36.0f * uiScale, seedText, activeTextField == 1);
        drawShadowText(panelX, 438.0f * uiScale, uiScale * 0.66f, Settings.isRussian() ? "\u041e\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u043f\u0443\u0441\u0442\u044b\u043c \u0434\u043b\u044f \u0441\u043b\u0443\u0447\u0430\u0439\u043d\u043e\u0433\u043e seed" : "Leave blank for a random seed", 0.72f, 0.72f, 0.74f);

        drawBottomButtons(GameConfig.createWorldActions(), mainMenuSelection, 240.0f * uiScale, 46.0f * uiScale, uiScale);
    }

    private void renderRenameWorldMenu(int mainMenuSelection, String renameWorldName, int activeTextField) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(76.0f * uiScale, uiScale * 1.0f, Settings.isRussian() ? "\u041f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c \u043c\u0438\u0440" : "Rename World", 0.96f, 0.96f, 0.96f);
        float panelWidth = Math.min(720.0f * uiScale, framebufferWidth - 120.0f * uiScale);
        float panelX = framebufferWidth * 0.5f - panelWidth * 0.5f;
        drawShadowText(panelX, 224.0f * uiScale, uiScale * 0.78f, Settings.isRussian() ? "\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043c\u0438\u0440\u0430" : "World Name", 0.82f, 0.82f, 0.82f);
        drawTextField(panelX, 246.0f * uiScale, panelWidth, 36.0f * uiScale, renameWorldName, activeTextField == 0);
        drawBottomButtons(GameConfig.renameWorldActions(), mainMenuSelection, 240.0f * uiScale, 46.0f * uiScale, uiScale);
    }

    private void renderMultiplayerMenu(int mainMenuSelection, String playerName, String host, String port, String status, int activeTextField) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(54.0f * uiScale, uiScale * 1.0f, Settings.isRussian() ? "\u041c\u0443\u043b\u044c\u0442\u0438\u043f\u043b\u0435\u0435\u0440" : "Multiplayer", 0.96f, 0.96f, 0.96f);
        float panelWidth = Math.min(720.0f * uiScale, framebufferWidth - 120.0f * uiScale);
        float panelX = framebufferWidth * 0.5f - panelWidth * 0.5f;
        drawShadowText(panelX, 120.0f * uiScale, uiScale * 0.78f, Settings.isRussian() ? "\u0418\u043c\u044f \u0438\u0433\u0440\u043e\u043a\u0430" : "Player Name", 0.82f, 0.82f, 0.82f);
        drawTextField(panelX, 142.0f * uiScale, panelWidth, 36.0f * uiScale, playerName, activeTextField == 0);
        drawShadowText(panelX, 210.0f * uiScale, uiScale * 0.78f, Settings.isRussian() ? "IP \u0445\u043e\u0441\u0442\u0430" : "Host IP", 0.82f, 0.82f, 0.82f);
        drawTextField(panelX, 232.0f * uiScale, panelWidth, 36.0f * uiScale, host, activeTextField == 1);
        drawShadowText(panelX, 300.0f * uiScale, uiScale * 0.78f, Settings.isRussian() ? "\u041f\u043e\u0440\u0442" : "Port", 0.82f, 0.82f, 0.82f);
        drawTextField(panelX, 322.0f * uiScale, panelWidth, 36.0f * uiScale, port, activeTextField == 2);
        drawShadowText(panelX, 388.0f * uiScale, uiScale * 0.72f, (Settings.isRussian() ? "\u0421\u0442\u0430\u0442\u0443\u0441: " : "Status: ") + status, 0.86f, 0.88f, 0.92f);
        drawBottomButtons(GameConfig.multiplayerActions(), mainMenuSelection, 240.0f * uiScale, 46.0f * uiScale, uiScale);
    }

    private void renderOpenLanMenu(int mainMenuSelection, int gameMode, boolean allowCheats) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(76.0f * uiScale, uiScale * 1.0f, Settings.isRussian() ? "\u041c\u0438\u0440 \u0432 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u043e\u0439 \u0441\u0435\u0442\u0438" : "LAN World", 0.96f, 0.96f, 0.96f);
        drawCenteredShadowText(124.0f * uiScale, uiScale * 0.72f, Settings.isRussian() ? "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0434\u043b\u044f \u0434\u0440\u0443\u0433\u0438\u0445 \u0438\u0433\u0440\u043e\u043a\u043e\u0432" : "Settings for other players", 0.86f, 0.86f, 0.88f);
        float buttonWidth = 240.0f * uiScale;
        float buttonHeight = 32.0f * uiScale;
        float gap = 12.0f * uiScale;
        float startX = framebufferWidth * 0.5f - (buttonWidth * 2.0f + gap) * 0.5f;
        String modeLabel = (Settings.isRussian() ? "\u0420\u0435\u0436\u0438\u043c \u0438\u0433\u0440\u044b: " : "Game Mode: ") + gameModeName(gameMode);
        String cheatsLabel = (Settings.isRussian() ? "\u0418\u0441\u043f. \u0447\u0438\u0442\u043e\u0432: " : "Allow Cheats: ") + (allowCheats ? (Settings.isRussian() ? "\u0412\u043a\u043b" : "On") : (Settings.isRussian() ? "\u0412\u044b\u043a\u043b" : "Off"));
        drawMenuButton(startX, 170.0f * uiScale, buttonWidth, buttonHeight, modeLabel, false, true, uiScale * 0.62f);
        drawMenuButton(startX + buttonWidth + gap, 170.0f * uiScale, buttonWidth, buttonHeight, cheatsLabel, false, true, uiScale * 0.62f);
        drawBottomButtons(GameConfig.lanActions(), mainMenuSelection, 240.0f * uiScale, 46.0f * uiScale, uiScale);
    }

    private void renderDisconnectedMenu(int mainMenuSelection, String reason) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawCenteredShadowText(120.0f * uiScale, uiScale * 1.1f,
            Settings.isRussian() ? "\u041e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e" : "Disconnected",
            0.98f, 0.86f, 0.76f);
        String message = reason == null || reason.trim().isEmpty() ? "Connection Lost" : reason.trim();
        float textScale = message.length() > 80 ? uiScale * 0.54f : uiScale * 0.66f;
        drawCenteredShadowText(190.0f * uiScale, textScale, message, 0.92f, 0.94f, 0.96f);
        drawBottomButtons(GameConfig.disconnectedActions(), mainMenuSelection, 240.0f * uiScale, 46.0f * uiScale, uiScale);
    }

    private void drawTextField(float x, float y, float width, float height, String text, boolean active) {
        float uiScale = Math.max(1.0f, getUiScale());
        drawRect(x, y, width, height, 0.02f, 0.02f, 0.02f, 0.92f);
        drawOutline(x, y, width, height, uiScale, active ? 0.94f : 0.58f, active ? 0.94f : 0.58f, active ? 0.96f : 0.60f, 0.95f);
        String display = text == null ? "" : text;
        if (active) {
            display += "_";
        }
        drawShadowText(x + 8.0f * uiScale, y + 23.0f * uiScale, uiScale * 0.78f, display, 0.96f, 0.96f, 0.96f);
    }

    private void drawBottomButtons(String[] actions, int selectedIndex, float buttonWidth, float buttonHeight, float uiScale) {
        float gap = 12.0f * uiScale;
        float startX = framebufferWidth * 0.5f - (buttonWidth * actions.length + gap * (actions.length - 1)) * 0.5f;
        float y = framebufferHeight - 78.0f * uiScale;
        for (int i = 0; i < actions.length; i++) {
            drawMenuButton(startX + i * (buttonWidth + gap), y, buttonWidth, buttonHeight, actions[i], i == selectedIndex, true, uiScale * 0.78f);
        }
    }

    private void drawMenuButton(float x, float y, float width, float height, String label, boolean selected, boolean enabled, float textScale) {
        drawRect(x, y, width, height,
            enabled ? (selected ? 0.86f : 0.58f) : 0.28f,
            enabled ? (selected ? 0.82f : 0.58f) : 0.29f,
            enabled ? (selected ? 0.58f : 0.58f) : 0.31f,
            enabled ? 0.98f : 0.70f);
        drawOutline(x, y, width, height, Math.max(1.0f, textScale), 0.0f, 0.0f, 0.0f, enabled ? 1.0f : 0.72f);
        drawText(x + width * 0.5f - measureTextWidth(label, textScale) * 0.5f, y + height * 0.63f, textScale, label, enabled ? 0.10f : 0.50f, enabled ? 0.10f : 0.52f, enabled ? 0.12f : 0.56f);
    }

    private String gameModeName(int gameMode) {
        return GameConfig.gameModeOptions()[Math.max(0, Math.min(gameMode, GameConfig.gameModeOptions().length - 1))];
    }

    private String gameModeHelp(int gameMode) {
        if (gameMode == 1) {
            return Settings.isRussian() ? "\u0411\u0435\u0441\u043a\u043e\u043d\u0435\u0447\u043d\u044b\u0435 \u0431\u043b\u043e\u043a\u0438, \u043f\u043e\u043b\u0435\u0442 \u0438 \u0431\u0435\u0437 \u0443\u0440\u043e\u043d\u0430 \u0432\u044b\u0436\u0438\u0432\u0430\u043d\u0438\u044f." : "Unlimited blocks, flying, and no survival damage.";
        }
        if (gameMode == 2) {
            return Settings.isRussian() ? "\u041f\u043e\u043b\u0435\u0442 \u0441\u043a\u0432\u043e\u0437\u044c \u0431\u043b\u043e\u043a\u0438 \u0438 \u0440\u0435\u0436\u0438\u043c \u043d\u0430\u0431\u043b\u044e\u0434\u0435\u043d\u0438\u044f." : "No clipping and observation only.";
        }
        return Settings.isRussian() ? "\u0418\u0449\u0438\u0442\u0435 \u0440\u0435\u0441\u0443\u0440\u0441\u044b, \u043a\u0440\u0430\u0444\u0442\u0438\u0442\u0435, \u0441\u043b\u0435\u0434\u0438\u0442\u0435 \u0437\u0430 \u0437\u0434\u043e\u0440\u043e\u0432\u044c\u0435\u043c \u0438 \u0433\u043e\u043b\u043e\u0434\u043e\u043c." : "Search for resources, craft, gain levels, health and hunger.";
    }

    private String formatWorldDate(long millis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(millis));
    }

    private void renderGameModeSwitcher(int gameModeSelection) {
        float uiScale = Math.max(1.0f, getUiScale());
        float boxWidth = 360.0f * uiScale;
        float boxHeight = 124.0f * uiScale;
        float boxX = framebufferWidth * 0.5f - boxWidth * 0.5f;
        float boxY = 74.0f * uiScale;
        drawRect(boxX, boxY, boxWidth, boxHeight, 0.04f, 0.05f, 0.06f, 0.82f);
        drawOutline(boxX, boxY, boxWidth, boxHeight, 2.0f * uiScale, 0.86f, 0.88f, 0.90f, 0.92f);
        drawCenteredShadowText(boxY + 28.0f * uiScale, uiScale, Settings.isRussian() ? "\u0420\u0435\u0436\u0438\u043c \u0438\u0433\u0440\u044b" : "Game Mode", 0.98f, 0.98f, 0.98f);

        float slotWidth = 104.0f * uiScale;
        float slotHeight = 42.0f * uiScale;
        float slotY = boxY + 62.0f * uiScale;
        float startX = framebufferWidth * 0.5f - (slotWidth * 3.0f + 14.0f * uiScale * 2.0f) * 0.5f;
        String[] gameModeOptions = GameConfig.gameModeOptions();
        for (int i = 0; i < gameModeOptions.length; i++) {
            float slotX = startX + i * (slotWidth + 14.0f * uiScale);
            boolean selected = i == gameModeSelection;
            drawRect(slotX, slotY, slotWidth, slotHeight,
                selected ? 0.92f : 0.22f,
                selected ? 0.88f : 0.24f,
                selected ? 0.62f : 0.27f,
                selected ? 0.98f : 0.92f);
            float labelScale = uiScale * 0.86f;
            float labelX = slotX + slotWidth * 0.5f - measureTextWidth(gameModeOptions[i], labelScale) * 0.5f;
            drawText(labelX, slotY + 26.0f * uiScale, labelScale, gameModeOptions[i],
                selected ? 0.08f : 0.94f,
                selected ? 0.08f : 0.94f,
                selected ? 0.08f : 0.94f);
        }
    }

    private void drawCenteredMenuButtons(String[] options, int selectedIndex, float firstOptionY, float selectedRed, float selectedGreen, float selectedBlue) {
        float uiScale = Math.max(1.0f, getUiScale());
        float boxWidth = 260.0f * uiScale;
        float boxHeight = 48.0f * uiScale;
        float boxX = framebufferWidth * 0.5f - boxWidth * 0.5f;
        for (int i = 0; i < options.length; i++) {
            float boxY = firstOptionY * uiScale + i * 64.0f * uiScale;
            boolean selected = i == selectedIndex;
            if (selected) {
                drawRect(boxX, boxY, boxWidth, boxHeight, 0.76f, 0.76f, 0.76f, 0.98f);
                drawOutline(boxX, boxY, boxWidth, boxHeight, uiScale, 0.0f, 0.0f, 0.0f, 1.0f);
                drawCenteredButtonLabel(boxY, boxHeight, uiScale, options[i], 0.10f, 0.10f, 0.10f);
            } else {
                drawRect(boxX, boxY, boxWidth, boxHeight, 0.62f, 0.62f, 0.62f, 0.95f);
                drawOutline(boxX, boxY, boxWidth, boxHeight, uiScale, 0.0f, 0.0f, 0.0f, 1.0f);
                drawCenteredButtonLabel(boxY, boxHeight, uiScale, options[i], 0.16f, 0.16f, 0.16f);
            }
        }
    }

    private void drawCenteredButtonLabel(float boxY, float boxHeight, float textScale, String text, float red, float green, float blue) {
        float textX = framebufferWidth * 0.5f - measureTextWidth(text, textScale) * 0.5f;
        float textY = boxY + boxHeight * 0.63f;
        drawText(textX, textY, textScale, text, red, green, blue);
    }

    private void renderInventoryScreen(PlayerState player, PlayerInventory inventory, boolean creativeMode, int selectedSlot, int creativeTab, int creativeScrollOffset, int inventoryScreenMode, ContainerInventory chestContainer, FurnaceBlockEntity furnace, double mouseX, double mouseY) {
        UiRenderer.InventoryUiLayout layout = buildInventoryLayout(creativeMode, creativeTab, creativeScrollOffset, inventoryScreenMode);
        UiRenderer.SlotBox hovered = layout.hitTest(mouseX, mouseY);
        float uiScale = getInventoryUiScale();

        drawRect(0.0f, 0.0f, framebufferWidth, framebufferHeight, 0.0f, 0.0f, 0.0f, 0.34f);
        drawRect(layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight, 0.09f, 0.09f, 0.10f, 0.78f);
        drawOutline(layout.panelX, layout.panelY, layout.panelWidth, layout.panelHeight, 2.4f * uiScale, 0.90f, 0.90f, 0.95f, 0.92f);
        if (!creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_PLAYER) {
            renderInventoryPlayerPreview(
                player,
                inventory,
                layout.armorX + layout.slotSize + 34.0f * uiScale,
                layout.armorY,
                layout.slotSize * 4.25f
            );
        } else if (creativeMode) {
            renderCreativeTabs(layout, creativeTab, mouseX, mouseY);
        }

        if (creativeMode) {
            for (UiRenderer.SlotBox slot : layout.slots) {
                if (slot.ref.group == InventorySlotGroup.TRASH) {
                    drawItemSlot(slot.x, slot.y, slot.size, null, false, hovered == slot, "X");
                    break;
                }
            }
            int[] tabIndices = InventoryItems.CREATIVE_TAB_INDICES[layout.activeCreativeTab];
            int visibleSlots = layout.creativeRows * layout.creativeColumns;
            int maxOffset = Math.max(0, tabIndices.length - visibleSlots);
            int scrollOffset = Math.max(0, Math.min(creativeScrollOffset, maxOffset));
            for (int row = 0; row < layout.creativeRows; row++) {
                for (int column = 0; column < layout.creativeColumns; column++) {
                    int visibleIndex = row * layout.creativeColumns + column + scrollOffset;
                    int globalIndex = visibleIndex < tabIndices.length ? tabIndices[visibleIndex] : -1;
                    ItemStack stack = globalIndex >= 0
                        ? new ItemStack(InventoryItems.CREATIVE_ITEMS[globalIndex], 1)
                        : null;
                    drawItemSlot(
                        layout.creativeX + column * (layout.slotSize + layout.slotGap),
                        layout.creativeY + row * (layout.slotSize + layout.slotGap),
                        layout.slotSize,
                        stack,
                        false,
                        hovered != null && hovered.ref.group == InventorySlotGroup.CREATIVE && hovered.ref.index == globalIndex,
                        null
                    );
                }
            }
            if (maxOffset > 0) {
                drawCreativeScrollbar(layout, scrollOffset, maxOffset);
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int index = row * 9 + column;
                drawItemSlot(
                    layout.storageX + column * (layout.slotSize + layout.slotGap),
                    layout.storageY + row * (layout.slotSize + layout.slotGap),
                    layout.slotSize,
                    inventory.getStorageStack(index),
                    false,
                    hovered != null && hovered.ref.group == InventorySlotGroup.STORAGE && hovered.ref.index == index,
                    null
                );
            }
        }

        for (int slot = 0; slot < 9; slot++) {
            drawItemSlot(
                layout.hotbarX + slot * (layout.slotSize + layout.slotGap),
                layout.hotbarY,
                layout.slotSize,
                inventory.getHotbarStack(slot),
                slot == selectedSlot,
                hovered != null && hovered.ref.group == InventorySlotGroup.HOTBAR && hovered.ref.index == slot,
                null
            );
        }

        if (!creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_PLAYER) {
            for (int slot = 0; slot < 4; slot++) {
                drawItemSlot(
                    layout.armorX,
                    layout.armorY + slot * (layout.slotSize + layout.slotGap),
                    layout.slotSize,
                    inventory.getArmorStack(slot),
                    false,
                    hovered != null && hovered.ref.group == InventorySlotGroup.ARMOR && hovered.ref.index == slot,
                    null
                );
            }

            drawItemSlot(
                layout.offhandX,
                layout.offhandY,
                layout.slotSize,
                inventory.getOffhandStack(),
                false,
                hovered != null && hovered.ref.group == InventorySlotGroup.OFFHAND,
                null
            );

            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 2; column++) {
                    int index = row * 2 + column;
                    drawItemSlot(
                        layout.craftX + column * (layout.slotSize + layout.slotGap),
                        layout.craftY + row * (layout.slotSize + layout.slotGap),
                        layout.slotSize,
                        inventory.getCraftStack(index),
                        false,
                        hovered != null && hovered.ref.group == InventorySlotGroup.CRAFT && hovered.ref.index == index,
                        null
                    );
                }
            }

            float arrowX = (layout.craftX + 2.0f * layout.slotSize + layout.slotGap + layout.resultX) * 0.5f - 10.0f * uiScale;
            float arrowY = layout.craftY + layout.slotSize + layout.slotGap * 0.5f - 8.0f * uiScale;
            drawShadowText(arrowX, arrowY, uiScale * 1.2f, "->", 0.90f, 0.90f, 0.90f);
            drawItemSlot(
                layout.resultX,
                layout.resultY,
                layout.slotSize,
                inventory.getCraftResultStack(),
                false,
                hovered != null && hovered.ref.group == InventorySlotGroup.CRAFT_RESULT,
                null
            );
        }

        if (!creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_WORKBENCH) {
            for (UiRenderer.SlotBox slot : layout.slots) {
                if (slot.ref.group == InventorySlotGroup.CRAFT_3X3) {
                    drawItemSlot(slot.x, slot.y, slot.size, inventory.getWorkbenchCraftStack(slot.ref.index), false,
                        hovered != null && hovered.ref.group == slot.ref.group && hovered.ref.index == slot.ref.index, null);
                } else if (slot.ref.group == InventorySlotGroup.CRAFT_3X3_RESULT) {
                    drawShadowText(slot.x - 30.0f * uiScale, slot.y + slot.size * 0.44f, uiScale * 1.2f, "->", 0.90f, 0.90f, 0.90f);
                    drawItemSlot(slot.x, slot.y, slot.size, inventory.getWorkbenchCraftResultStack(), false,
                        hovered != null && hovered.ref.group == InventorySlotGroup.CRAFT_3X3_RESULT, null);
                }
            }
        } else if (!creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_CHEST) {
            for (UiRenderer.SlotBox slot : layout.slots) {
                if (slot.ref.group == InventorySlotGroup.CHEST_CONTAINER) {
                    drawItemSlot(slot.x, slot.y, slot.size, chestContainer == null ? null : chestContainer.getStack(slot.ref.index), false,
                        hovered != null && hovered.ref.group == slot.ref.group && hovered.ref.index == slot.ref.index, null);
                }
            }
        } else if (!creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_FURNACE) {
            for (UiRenderer.SlotBox slot : layout.slots) {
                ItemStack stack = null;
                if (slot.ref.group == InventorySlotGroup.FURNACE_INPUT) {
                    stack = furnace == null ? null : furnace.input;
                } else if (slot.ref.group == InventorySlotGroup.FURNACE_FUEL) {
                    stack = furnace == null ? null : furnace.fuel;
                } else if (slot.ref.group == InventorySlotGroup.FURNACE_OUTPUT) {
                    stack = furnace == null ? null : furnace.output;
                } else {
                    continue;
                }
                drawItemSlot(slot.x, slot.y, slot.size, stack, false,
                    hovered != null && hovered.ref.group == slot.ref.group, null);
            }
            if (furnace != null) {
                float barX = layout.panelX + layout.panelWidth * 0.5f - 44.0f * uiScale;
                float barY = layout.panelY + 80.0f * uiScale;
                drawRect(barX, barY, 88.0f * uiScale, 7.0f * uiScale, 0.04f, 0.04f, 0.04f, 0.90f);
                float cook = furnace.cookTotal <= 0.0 ? 0.0f : (float) Math.min(1.0, furnace.cookProgress / furnace.cookTotal);
                drawRect(barX, barY, 88.0f * uiScale * cook, 7.0f * uiScale, 0.90f, 0.58f, 0.20f, 0.96f);
                float burn = furnace.burnTotal <= 0.0 ? 0.0f : (float) Math.min(1.0, furnace.burnRemaining / furnace.burnTotal);
                drawRect(barX, barY + 13.0f * uiScale, 88.0f * uiScale * burn, 5.0f * uiScale, 0.95f, 0.28f, 0.08f, 0.92f);
            }
        }

        if (!inventory.getCursorStack().isEmpty()) {
            drawItemSlot((float) mouseX - layout.slotSize * 0.32f, (float) mouseY - layout.slotSize * 0.32f, layout.slotSize, inventory.getCursorStack(), false, false, null);
        }

        if (hovered != null) {
            if (hovered.ref.group == InventorySlotGroup.TRASH) {
                drawTooltip((float) mouseX + 14.0f, (float) mouseY + 18.0f, Settings.isRussian() ? "\u0423\u0434\u0430\u043b\u0438\u0442\u044c" : "Delete");
            } else {
                ItemStack tooltipStack = inventory.peekSlot(hovered.ref, creativeMode, chestContainer, furnace);
                if (tooltipStack != null && !tooltipStack.isEmpty()) {
                    drawTooltip((float) mouseX + 14.0f, (float) mouseY + 18.0f, itemTooltipText(tooltipStack));
                }
            }
        }
    }

    private String uiText(String russian, String english) {
        return Settings.isRussian() ? russian : english;
    }

    private String itemTooltipText(ItemStack stack) {
        String text = InventoryItems.name(stack.itemId);
        if (InventoryItems.isDurableItem(stack.itemId)) {
            text += "\n" + uiText("\u041f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c: ", "Durability: ")
                + stack.remainingDurability() + "/" + InventoryItems.maxDurability(stack.itemId);
        }
        return text;
    }

    private void drawCreativeScrollbar(UiRenderer.InventoryUiLayout layout, int scrollOffset, int maxOffset) {
        float uiScale = getInventoryUiScale();
        float x = layout.creativeX + layout.creativeColumns * (layout.slotSize + layout.slotGap) + 5.0f * uiScale;
        float y = layout.creativeY;
        float height = layout.creativeRows * layout.slotSize + (layout.creativeRows - 1) * layout.slotGap;
        float width = 3.0f * uiScale;
        drawRect(x, y, width, height, 0.06f, 0.06f, 0.07f, 0.70f);
        float thumbHeight = Math.max(14.0f * uiScale, height * 36.0f / (36.0f + maxOffset));
        float thumbY = y + (height - thumbHeight) * (scrollOffset / (float) Math.max(1, maxOffset));
        drawRect(x, thumbY, width, thumbHeight, 0.80f, 0.80f, 0.84f, 0.88f);
    }

    private void renderCreativeTabs(UiRenderer.InventoryUiLayout layout, int creativeTab, double mouseX, double mouseY) {
        float uiScale = getInventoryUiScale();
        float gap = 4.0f * uiScale;
        String[] tabLabels = GameConfig.creativeTabs();
        float tabWidth = (layout.panelWidth - 32.0f * uiScale - gap * (tabLabels.length - 1)) / tabLabels.length;
        float tabHeight = 22.0f * uiScale;
        float totalWidth = tabWidth * tabLabels.length + gap * (tabLabels.length - 1);
        float startX = layout.panelX + layout.panelWidth * 0.5f - totalWidth * 0.5f;
        float y = layout.panelY + 10.0f * uiScale;
        int activeTab = Math.max(0, Math.min(creativeTab, tabLabels.length - 1));
        for (int i = 0; i < tabLabels.length; i++) {
            float x = startX + i * (tabWidth + gap);
            boolean active = i == activeTab;
            boolean hovered = mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight;
            drawRect(x, y, tabWidth, tabHeight,
                active ? 0.84f : (hovered ? 0.52f : 0.32f),
                active ? 0.78f : (hovered ? 0.54f : 0.34f),
                active ? 0.56f : (hovered ? 0.58f : 0.38f),
                0.96f);
            drawOutline(x, y, tabWidth, tabHeight, uiScale, active ? 0.96f : 0.18f, active ? 0.92f : 0.18f, active ? 0.72f : 0.20f, 0.92f);
            String label = tabLabels[i];
            float labelScale = Math.min(uiScale * 0.58f, tabWidth / Math.max(1.0f, measureTextWidth(label, 1.0f)) * 0.82f);
            drawText(x + tabWidth * 0.5f - measureTextWidth(label, labelScale) * 0.5f, y + 15.0f * uiScale, labelScale, label, active ? 0.08f : 0.94f, active ? 0.08f : 0.94f, active ? 0.08f : 0.94f);
        }
    }

    private void renderInventoryPlayerPreview(PlayerState player, PlayerInventory inventory, float x, float y, float height) {
        float centerX = x + height * 0.34f;
        float feetY = y + height;
        float scale = height * 0.43f;

        drawRect(x - height * 0.08f, y - height * 0.04f, height * 0.84f, height * 1.08f, 0.05f, 0.05f, 0.06f, 0.34f);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, framebufferWidth, framebufferHeight, 0.0, -1000.0, 1000.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glTranslated(centerX, feetY, 0.0);
        glScalef(scale, -scale, scale);
        glRotated(-Math.toDegrees(player.yaw) - 25.0, 0.0, 1.0, 0.0);
        drawPlayerModelParts(inventory, GameConfig.AIR, 0.0, 0.0, player.pitch, false, player.sneaking);
        glDisable(GL_DEPTH_TEST);
        glPopMatrix();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private float[] armorColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return getBlockFaceColor(stack.itemId, Face.TOP);
    }

    private void drawItemSlot(float x, float y, float size, ItemStack stack, boolean selected, boolean hovered, String placeholder) {
        float background = hovered ? 0.28f : 0.18f;
        drawRect(x, y, size, size, background, background, background + 0.03f, 0.92f);
        if (selected) {
            drawOutline(x, y, size, size, 2.4f, 1.0f, 0.92f, 0.52f, 0.98f);
        } else if (hovered) {
            drawOutline(x, y, size, size, 2.0f, 0.98f, 0.98f, 0.98f, 0.94f);
        } else {
            drawOutline(x, y, size, size, 1.3f, 0.58f, 0.58f, 0.60f, 0.90f);
        }

        if (stack != null && !stack.isEmpty()) {
            drawItemIcon(stack.itemId, x + size * 0.16f, y + size * 0.16f, size * 0.68f);
            if (stack.count > 1) {
                drawShadowText(x + size * 0.46f, y + size * 0.78f, getUiScale() * 0.62f, Integer.toString(stack.count), 1.0f, 1.0f, 1.0f);
            }
            if (InventoryItems.isDurableItem(stack.itemId) && stack.durabilityDamage > 0) {
                float durability = stack.remainingDurability() / (float) Math.max(1, InventoryItems.maxDurability(stack.itemId));
                float barX = x + size * 0.12f;
                float barY = y + size * 0.86f;
                float barWidth = size * 0.76f;
                drawRect(barX, barY, barWidth, Math.max(1.0f, size * 0.055f), 0.04f, 0.04f, 0.04f, 0.92f);
                drawRect(barX, barY, barWidth * durability, Math.max(1.0f, size * 0.055f),
                    1.0f - durability, 0.32f + durability * 0.62f, 0.12f, 0.98f);
            }
        } else if (placeholder != null) {
            float scale = Math.min(getUiScale() * 0.56f, size / Math.max(1.0f, measureTextWidth(placeholder, 1.0f)) * 0.70f);
            drawShadowText(x + size * 0.5f - measureTextWidth(placeholder, scale) * 0.5f,
                y + size * 0.62f, scale, placeholder, 0.66f, 0.66f, 0.70f);
        }
    }

    private void drawItemIcon(byte itemId, float x, float y, float size) {
        float[] color = getHeldBlockColor(itemId);
        switch (itemId) {
            case InventoryItems.STICK:
                drawRect(x + size * 0.42f, y + size * 0.08f, size * 0.16f, size * 0.84f, 0.56f, 0.38f, 0.21f, 0.98f);
                drawRect(x + size * 0.34f, y + size * 0.08f, size * 0.32f, size * 0.14f, 0.70f, 0.54f, 0.32f, 0.98f);
                break;
            case InventoryItems.IRON_INGOT:
                drawRect(x + size * 0.10f, y + size * 0.34f, size * 0.80f, size * 0.32f, 0.78f, 0.80f, 0.84f, 0.98f);
                drawOutline(x + size * 0.10f, y + size * 0.34f, size * 0.80f, size * 0.32f, 1.2f, 0.55f, 0.57f, 0.60f, 0.98f);
                break;
            case InventoryItems.COAL_ITEM:
                drawRect(x + size * 0.22f, y + size * 0.24f, size * 0.56f, size * 0.48f, 0.08f, 0.08f, 0.09f, 0.98f);
                drawRect(x + size * 0.34f, y + size * 0.34f, size * 0.30f, size * 0.22f, 0.18f, 0.18f, 0.19f, 0.98f);
                break;
            case InventoryItems.DIAMOND_ITEM:
                drawRect(x + size * 0.24f, y + size * 0.16f, size * 0.52f, size * 0.58f, 0.38f, 0.86f, 0.92f, 0.98f);
                drawRect(x + size * 0.36f, y + size * 0.26f, size * 0.28f, size * 0.24f, 0.70f, 0.97f, 1.00f, 0.98f);
                break;
            case InventoryItems.ITEM_BUCKET:
            case InventoryItems.ITEM_WATER_BUCKET:
            case InventoryItems.ITEM_LAVA_BUCKET:
                drawRect(x + size * 0.22f, y + size * 0.18f, size * 0.56f, size * 0.14f, 0.72f, 0.74f, 0.76f, 0.98f);
                drawRect(x + size * 0.28f, y + size * 0.30f, size * 0.44f, size * 0.48f, 0.48f, 0.50f, 0.54f, 0.98f);
                if (itemId == InventoryItems.ITEM_WATER_BUCKET) {
                    drawRect(x + size * 0.32f, y + size * 0.42f, size * 0.36f, size * 0.22f, 0.25f, 0.50f, 0.92f, 0.98f);
                } else if (itemId == InventoryItems.ITEM_LAVA_BUCKET) {
                    drawRect(x + size * 0.32f, y + size * 0.42f, size * 0.36f, size * 0.22f, 0.95f, 0.34f, 0.08f, 0.98f);
                }
                break;
            case InventoryItems.IRON_HELMET:
            case InventoryItems.DIAMOND_HELMET:
            case InventoryItems.NETHERITE_HELMET:
                drawRect(x + size * 0.12f, y + size * 0.18f, size * 0.76f, size * 0.34f, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.22f, y + size * 0.50f, size * 0.56f, size * 0.18f, color[0] * 0.82f, color[1] * 0.82f, color[2] * 0.82f, 0.98f);
                break;
            case InventoryItems.IRON_CHESTPLATE:
            case InventoryItems.DIAMOND_CHESTPLATE:
            case InventoryItems.NETHERITE_CHESTPLATE:
                drawRect(x + size * 0.18f, y + size * 0.14f, size * 0.64f, size * 0.72f, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.08f, y + size * 0.22f, size * 0.16f, size * 0.28f, color[0] * 0.82f, color[1] * 0.82f, color[2] * 0.82f, 0.98f);
                drawRect(x + size * 0.76f, y + size * 0.22f, size * 0.16f, size * 0.28f, color[0] * 0.82f, color[1] * 0.82f, color[2] * 0.82f, 0.98f);
                break;
            case InventoryItems.IRON_LEGGINGS:
            case InventoryItems.DIAMOND_LEGGINGS:
            case InventoryItems.NETHERITE_LEGGINGS:
                drawRect(x + size * 0.22f, y + size * 0.14f, size * 0.56f, size * 0.28f, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.24f, y + size * 0.42f, size * 0.18f, size * 0.40f, color[0] * 0.82f, color[1] * 0.82f, color[2] * 0.82f, 0.98f);
                drawRect(x + size * 0.58f, y + size * 0.42f, size * 0.18f, size * 0.40f, color[0] * 0.82f, color[1] * 0.82f, color[2] * 0.82f, 0.98f);
                break;
            case InventoryItems.IRON_BOOTS:
            case InventoryItems.DIAMOND_BOOTS:
            case InventoryItems.NETHERITE_BOOTS:
                drawRect(x + size * 0.18f, y + size * 0.54f, size * 0.22f, size * 0.26f, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.50f, y + size * 0.54f, size * 0.22f, size * 0.26f, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.14f, y + size * 0.72f, size * 0.34f, size * 0.10f, color[0] * 1.08f, color[1] * 1.08f, color[2] * 1.08f, 0.98f);
                drawRect(x + size * 0.46f, y + size * 0.72f, size * 0.34f, size * 0.10f, color[0] * 1.08f, color[1] * 1.08f, color[2] * 1.08f, 0.98f);
                break;
            case InventoryItems.SHIELD:
                drawRect(x + size * 0.24f, y + size * 0.08f, size * 0.52f, size * 0.74f, 0.60f, 0.42f, 0.22f, 0.98f);
                drawRect(x + size * 0.42f, y + size * 0.14f, size * 0.16f, size * 0.62f, 0.82f, 0.82f, 0.84f, 0.98f);
                break;
            case InventoryItems.TOTEM:
                drawRect(x + size * 0.38f, y + size * 0.10f, size * 0.24f, size * 0.72f, 0.88f, 0.70f, 0.22f, 0.98f);
                drawRect(x + size * 0.18f, y + size * 0.24f, size * 0.20f, size * 0.18f, 0.45f, 0.72f, 0.28f, 0.98f);
                drawRect(x + size * 0.62f, y + size * 0.24f, size * 0.20f, size * 0.18f, 0.45f, 0.72f, 0.28f, 0.98f);
                drawRect(x + size * 0.30f, y + size * 0.62f, size * 0.12f, size * 0.14f, 0.88f, 0.82f, 0.28f, 0.98f);
                drawRect(x + size * 0.58f, y + size * 0.62f, size * 0.12f, size * 0.14f, 0.88f, 0.82f, 0.28f, 0.98f);
                break;
            case InventoryItems.WOODEN_PICKAXE:
            case InventoryItems.STONE_PICKAXE:
            case InventoryItems.IRON_PICKAXE:
            case InventoryItems.DIAMOND_PICKAXE:
            case InventoryItems.NETHERITE_PICKAXE:
                drawToolIcon(itemId, x, y, size, 0);
                break;
            case InventoryItems.WOODEN_SWORD:
            case InventoryItems.STONE_SWORD:
            case InventoryItems.IRON_SWORD:
            case InventoryItems.DIAMOND_SWORD:
            case InventoryItems.NETHERITE_SWORD:
                drawToolIcon(itemId, x, y, size, 1);
                break;
            case InventoryItems.WOODEN_AXE:
            case InventoryItems.STONE_AXE:
            case InventoryItems.IRON_AXE:
            case InventoryItems.DIAMOND_AXE:
            case InventoryItems.NETHERITE_AXE:
                drawToolIcon(itemId, x, y, size, 2);
                break;
            case InventoryItems.WOODEN_SHOVEL:
            case InventoryItems.STONE_SHOVEL:
            case InventoryItems.IRON_SHOVEL:
            case InventoryItems.DIAMOND_SHOVEL:
            case InventoryItems.NETHERITE_SHOVEL:
                drawToolIcon(itemId, x, y, size, 3);
                break;
            case InventoryItems.WOODEN_HOE:
            case InventoryItems.STONE_HOE:
            case InventoryItems.IRON_HOE:
            case InventoryItems.DIAMOND_HOE:
            case InventoryItems.NETHERITE_HOE:
                drawToolIcon(itemId, x, y, size, 4);
                break;
            case InventoryItems.ZOMBIE_SPAWN_EGG:
            case InventoryItems.SKELETON_SPAWN_EGG:
            case InventoryItems.PIG_SPAWN_EGG:
            case InventoryItems.SHEEP_SPAWN_EGG:
            case InventoryItems.COW_SPAWN_EGG:
            case InventoryItems.VILLAGER_SPAWN_EGG:
            case InventoryItems.HERRING_SPAWN_EGG:
            case InventoryItems.SALMON_SPAWN_EGG:
                drawEggIcon(itemId, x, y, size);
                break;
            case InventoryItems.RAW_PORK:
            case InventoryItems.RAW_BEEF:
            case InventoryItems.RAW_MUTTON:
            case InventoryItems.RAW_HERRING:
            case InventoryItems.RAW_SALMON:
            case InventoryItems.COOKED_HERRING:
            case InventoryItems.COOKED_SALMON:
            case InventoryItems.LEATHER:
            case InventoryItems.WOOL:
            case InventoryItems.ROTTEN_FLESH:
            case InventoryItems.BONE:
                drawRect(x + size * 0.16f, y + size * 0.22f, size * 0.68f, size * 0.50f, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.28f, y + size * 0.34f, size * 0.44f, size * 0.24f, clampColor(color[0] * 1.12f), clampColor(color[1] * 1.12f), clampColor(color[2] * 1.12f), 0.98f);
                break;
            default:
                drawRect(x, y, size, size, color[0], color[1], color[2], 0.98f);
                drawRect(x + size * 0.14f, y + size * 0.14f, size * 0.72f, size * 0.72f, clampColor(color[0] * 0.82f), clampColor(color[1] * 0.82f), clampColor(color[2] * 0.82f), 0.98f);
                if (isOreBlock(itemId)) {
                    drawOreItemOverlay(itemId, x, y, size);
                }
                break;
        }
    }

    private void drawEggIcon(byte itemId, float x, float y, float size) {
        float[] color = getHeldBlockColor(itemId);
        drawRect(x + size * 0.24f, y + size * 0.14f, size * 0.52f, size * 0.70f, color[0], color[1], color[2], 0.98f);
        drawRect(x + size * 0.34f, y + size * 0.08f, size * 0.32f, size * 0.18f, color[0], color[1], color[2], 0.98f);
        drawRect(x + size * 0.36f, y + size * 0.32f, size * 0.14f, size * 0.12f, 0.96f, 0.96f, 0.90f, 0.98f);
        drawRect(x + size * 0.58f, y + size * 0.54f, size * 0.12f, size * 0.10f, 0.96f, 0.96f, 0.90f, 0.98f);
    }

    private void drawToolIcon(byte itemId, float x, float y, float size, int shape) {
        float[] head = getHeldBlockColor(itemId);
        float handleRed = 0.55f;
        float handleGreen = 0.36f;
        float handleBlue = 0.18f;
        drawRect(x + size * 0.45f, y + size * 0.34f, size * 0.12f, size * 0.52f, handleRed, handleGreen, handleBlue, 0.98f);
        if (shape == 1) {
            drawRect(x + size * 0.42f, y + size * 0.08f, size * 0.18f, size * 0.56f, head[0], head[1], head[2], 0.98f);
            drawRect(x + size * 0.37f, y + size * 0.16f, size * 0.28f, size * 0.10f, clampColor(head[0] * 1.18f), clampColor(head[1] * 1.18f), clampColor(head[2] * 1.18f), 0.98f);
        } else if (shape == 2) {
            drawRect(x + size * 0.28f, y + size * 0.14f, size * 0.38f, size * 0.22f, head[0], head[1], head[2], 0.98f);
            drawRect(x + size * 0.58f, y + size * 0.26f, size * 0.16f, size * 0.22f, head[0], head[1], head[2], 0.98f);
        } else if (shape == 3) {
            drawRect(x + size * 0.36f, y + size * 0.10f, size * 0.30f, size * 0.22f, head[0], head[1], head[2], 0.98f);
            drawRect(x + size * 0.42f, y + size * 0.28f, size * 0.18f, size * 0.10f, head[0], head[1], head[2], 0.98f);
        } else if (shape == 4) {
            drawRect(x + size * 0.28f, y + size * 0.12f, size * 0.48f, size * 0.13f, head[0], head[1], head[2], 0.98f);
            drawRect(x + size * 0.28f, y + size * 0.24f, size * 0.13f, size * 0.20f, head[0], head[1], head[2], 0.98f);
        } else {
            drawRect(x + size * 0.24f, y + size * 0.12f, size * 0.54f, size * 0.16f, head[0], head[1], head[2], 0.98f);
            drawRect(x + size * 0.22f, y + size * 0.26f, size * 0.16f, size * 0.20f, head[0], head[1], head[2], 0.98f);
            drawRect(x + size * 0.64f, y + size * 0.26f, size * 0.16f, size * 0.20f, head[0], head[1], head[2], 0.98f);
        }
    }

    private void drawOreItemOverlay(byte itemId, float x, float y, float size) {
        float[] accent = oreAccentColor(itemId, 1.0f);
        drawRect(x + size * 0.24f, y + size * 0.24f, size * 0.16f, size * 0.15f, accent[0], accent[1], accent[2], 0.98f);
        drawRect(x + size * 0.56f, y + size * 0.22f, size * 0.18f, size * 0.14f, accent[0], accent[1], accent[2], 0.98f);
        drawRect(x + size * 0.36f, y + size * 0.52f, size * 0.22f, size * 0.18f, accent[0], accent[1], accent[2], 0.98f);
        drawRect(x + size * 0.64f, y + size * 0.62f, size * 0.12f, size * 0.12f, accent[0], accent[1], accent[2], 0.98f);
    }

    private void drawTooltip(float x, float y, String text) {
        float uiScale = getUiScale();
        float scale = uiScale * 0.86f;
        String[] lines = text == null ? new String[0] : text.split("\\n", -1);
        float textWidth = 0.0f;
        for (String line : lines) {
            textWidth = Math.max(textWidth, measureTextWidth(line, scale));
        }
        float lineHeight = 15.0f * uiScale;
        float width = Math.max(84.0f * uiScale, textWidth + 16.0f * uiScale);
        float height = Math.max(24.0f * uiScale, 10.0f * uiScale + lineHeight * Math.max(1, lines.length));
        float clampedX = Math.min(x, framebufferWidth - width - 8.0f * uiScale);
        float clampedY = Math.min(y, framebufferHeight - height - 8.0f * uiScale);
        drawRect(clampedX, clampedY, width, height, 0.04f, 0.04f, 0.05f, 0.92f);
        drawOutline(clampedX, clampedY, width, height, 1.4f * uiScale, 0.94f, 0.94f, 0.98f, 0.94f);
        for (int i = 0; i < Math.max(1, lines.length); i++) {
            String line = lines.length == 0 ? "" : lines[i];
            drawShadowText(clampedX + 8.0f * uiScale, clampedY + 17.0f * uiScale + i * lineHeight,
                scale, line, 1.0f, 1.0f, 1.0f);
        }
    }

    private void drawOutline(float x, float y, float width, float height, float thickness, float red, float green, float blue, float alpha) {
        drawRect(x, y, width, thickness, red, green, blue, alpha);
        drawRect(x, y + height - thickness, width, thickness, red, green, blue, alpha);
        drawRect(x, y, thickness, height, red, green, blue, alpha);
        drawRect(x + width - thickness, y, thickness, height, red, green, blue, alpha);
    }

    private void drawRect(float x, float y, float width, float height, float red, float green, float blue, float alpha) {
        glColor4f(red, green, blue, alpha);
        glBegin(GL_QUADS);
        glVertex3d(x, y, 0.0);
        glVertex3d(x + width, y, 0.0);
        glVertex3d(x + width, y + height, 0.0);
        glVertex3d(x, y + height, 0.0);
        glEnd();
    }

    private void drawText(float x, float y, float scale, String text, float red, float green, float blue) {
        drawText(x, y, scale, text, red, green, blue, 1.0f);
    }

    private void drawText(float x, float y, float scale, String text, float red, float green, float blue, float alpha) {
        text = normalizeUiText(text);
        if (text == null || text.isEmpty()) {
            return;
        }
        drawTextTexture(x, y, scale, text, red, green, blue, alpha);
    }

    private float measureTextWidth(String text, float scale) {
        if (text == null) {
            return 0.0f;
        }
        text = normalizeUiText(text);
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        return getTextTexture(text, scale).width;
    }

    private String normalizeUiText(String text) {
        if (text == null || text.isEmpty() || !looksLikeUtf8DecodedAsCp1251(text)) {
            return text;
        }
        try {
            byte[] bytes = text.getBytes(java.nio.charset.Charset.forName("windows-1251"));
            String repaired = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
            return isBetterRussianText(text, repaired) ? repaired : text;
        } catch (CharacterCodingException | RuntimeException ignored) {
            return text;
        }
    }

    private boolean looksLikeUtf8DecodedAsCp1251(String text) {
        return mojibakeScore(text) >= 2;
    }

    private boolean isBetterRussianText(String original, String repaired) {
        if (repaired == null || repaired.isEmpty() || repaired.indexOf('\uFFFD') >= 0) {
            return false;
        }
        return mojibakeScore(repaired) + 2 < mojibakeScore(original) && countCyrillicLetters(repaired) > 0;
    }

    private int mojibakeScore(String text) {
        int score = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00D0' || c == '\u00D1' || c == '\u0420' || c == '\u0421' || c == '\u0413') {
                score++;
            } else if (c == '\uFFFD') {
                score += 3;
            }
        }
        return score;
    }


    private int countCyrillicLetters(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(text.charAt(i));
            if (block == Character.UnicodeBlock.CYRILLIC) {
                count++;
            }
        }
        return count;
    }

    private void drawTextTexture(float x, float y, float scale, String text, float red, float green, float blue, float alpha) {
        TextTexture texture = getTextTexture(text, scale);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture.textureId);
        glColor4f(red, green, blue, alpha);
        glBegin(GL_QUADS);
        float top = y - texture.baseline;
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(x, top, 0.0);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(x + texture.width, top, 0.0);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(x + texture.width, top + texture.height, 0.0);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(x, top + texture.height, 0.0);
        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }

    private TextTexture getTextTexture(String text, float scale) {
        int fontSize = Math.max(9, Math.round(9.0f * Math.max(0.75f, scale)));
        String key = fontSize + "\u0000" + text;
        TextTexture existing = textTextures.get(key);
        if (existing != null) {
            return existing;
        }

        Font font = new Font(UI_FONT_FAMILY, Font.BOLD, fontSize);
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measureGraphics = measureImage.createGraphics();
        measureGraphics.setFont(font);
        FontMetrics metrics = measureGraphics.getFontMetrics();
        int width = Math.max(1, metrics.stringWidth(text) + 2);
        int height = Math.max(1, metrics.getAscent() + metrics.getDescent() + 2);
        int baseline = metrics.getAscent() + 1;
        measureGraphics.dispose();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(font);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString(text, 1, baseline);
        graphics.dispose();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int argb = pixels[row * width + column];
                int alpha = (argb >>> 24) & 0xFF;
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) alpha);
            }
        }
        buffer.flip();

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        TextTexture texture = new TextTexture(textureId, width, height, baseline);
        textTextures.put(key, texture);
        trimTextTextureCache();
        return texture;
    }

    private void trimTextTextureCache() {
        Iterator<Map.Entry<String, TextTexture>> iterator = textTextures.entrySet().iterator();
        while (textTextures.size() > MAX_TEXT_TEXTURE_CACHE_SIZE && iterator.hasNext()) {
            TextTexture texture = iterator.next().getValue();
            glDeleteTextures(texture.textureId);
            iterator.remove();
        }
    }

    private void drawCenteredShadowText(float y, float scale, String text, float red, float green, float blue) {
        float x = framebufferWidth * 0.5f - measureTextWidth(text, scale) * 0.5f;
        drawShadowText(x, y, scale, text, red, green, blue);
    }

    private void drawShadowText(float x, float y, float scale, String text, float red, float green, float blue) {
        drawShadowText(x, y, scale, text, red, green, blue, 1.0f);
    }

    private void drawShadowText(float x, float y, float scale, String text, float red, float green, float blue, float alpha) {
        drawText(x + Math.max(1.0f, scale), y + Math.max(1.0f, scale), scale, text, 0.0f, 0.0f, 0.0f, alpha);
        drawText(x, y, scale, text, red, green, blue, alpha);
    }

    private boolean loadMenuPanoramaTextures() {
        for (int i = 0; i < MENU_PANORAMA_FACE_COUNT; i++) {
            File file = RuntimePaths.resolve("assets", "menu_panorama", "panorama_" + i + ".png").toFile();
            if (!file.isFile()) {
                deleteMenuPanoramaTextures();
                System.out.println("Menu panorama disabled: missing " + file.getPath());
                return false;
            }
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    deleteMenuPanoramaTextures();
                    System.out.println("Menu panorama disabled: unreadable " + file.getPath());
                    return false;
                }
                menuPanoramaTextureIds[i] = uploadTexture(image, GL_LINEAR, GL_CLAMP_TO_EDGE);
            } catch (IOException | RuntimeException exception) {
                deleteMenuPanoramaTextures();
                System.out.println("Menu panorama disabled: " + exception.getMessage());
                return false;
            }
        }
        verifyOpenGl("loadMenuPanoramaTextures");
        return true;
    }

    private void deleteMenuPanoramaTextures() {
        for (int i = 0; i < menuPanoramaTextureIds.length; i++) {
            if (menuPanoramaTextureIds[i] != 0) {
                glDeleteTextures(menuPanoramaTextureIds[i]);
                menuPanoramaTextureIds[i] = 0;
            }
        }
    }

    private int uploadTexture(BufferedImage image, int filter, int wrap) {
        int textureId = glGenTextures();
        if (textureId == 0) {
            throw new IllegalStateException("glGenTextures returned 0");
        }
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        return textureId;
    }

    private int loadTerrainTexture() {
        try {
            BufferedImage atlas = loadOrCreateTerrainAtlas();
            int textureId = uploadTexture(atlas, GL_NEAREST, GL_REPEAT);
            verifyOpenGl("loadTerrainTexture");
            return textureId;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load terrain atlas", exception);
        }
    }

    private int createUploadProbeVbo() {
        int vboId = glGenBuffers();
        if (vboId == 0) {
            throw new IllegalStateException("glGenBuffers returned 0");
        }

        ByteBuffer probe = BufferUtils.createByteBuffer(16);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, probe, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        verifyOpenGl("createUploadProbeVbo");
        return vboId;
    }

    private BufferedImage loadOrCreateTerrainAtlas() throws IOException {
        File file = RuntimePaths.resolve("terrain.png").toFile();
        if (file.exists()) {
            BufferedImage existing = ImageIO.read(file);
            if (existing != null) {
                return existing;
            }
        }

        BufferedImage atlas = new BufferedImage(ATLAS_TILE_SIZE * ATLAS_COLUMNS, ATLAS_TILE_SIZE * ATLAS_ROWS, BufferedImage.TYPE_INT_ARGB);
        fillSolidTile(atlas, 0, 0xFF4CB545);
        fillSolidTile(atlas, 1, 0xFF7D593B);
        fillGrassSideTile(atlas, 2);
        fillSpeckledTile(atlas, 3, 0xFF7B7B7B, 0xFF9A9A9A, 0xFF606060, 22);
        fillSpeckledTile(atlas, 4, 0xFF3D3D3D, 0xFF545454, 0xFF1B1B1B, 26);
        fillSpeckledTile(atlas, 5, 0xFF7B7B7B, 0xFFC09574, 0xFF5C5C5C, 16);
        fillSpeckledTile(atlas, 6, 0xFF7B7B7B, 0xFF4DD5DE, 0xFF5C5C5C, 16);
        fillSpeckledTile(atlas, 7, 0xFF7B7B7B, 0xFF1A1A1A, 0xFF5C5C5C, 18);
        fillSolidTile(atlas, 8, 0xFF61B056);
        fillSolidTile(atlas, 9, 0xFF3A8291);
        fillSolidTile(atlas, 10, 0xFF40458A);
        fillSolidTile(atlas, 11, 0xFF111111);
        fillWaterTile(atlas, 12, 0xA04887E6);
        fillLavaTile(atlas, 13);
        fillSolidTile(atlas, 14, 0xFFD1B286);
        fillSolidTile(atlas, 15, 0xFF8A6A43);
        ImageIO.write(atlas, "png", file);
        return atlas;
    }

    private void fillSolidTile(BufferedImage atlas, int tileIndex, int argb) {
        int startX = (tileIndex % ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        int startY = (tileIndex / ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        for (int y = 0; y < ATLAS_TILE_SIZE; y++) {
            for (int x = 0; x < ATLAS_TILE_SIZE; x++) {
                atlas.setRGB(startX + x, startY + y, argb);
            }
        }
    }

    private void fillSpeckledTile(BufferedImage atlas, int tileIndex, int baseArgb, int accentArgb, int shadowArgb, int accentCount) {
        fillSolidTile(atlas, tileIndex, baseArgb);
        int startX = (tileIndex % ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        int startY = (tileIndex / ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        for (int i = 0; i < accentCount; i++) {
            int x = (i * 7 + tileIndex * 3) % ATLAS_TILE_SIZE;
            int y = (i * 11 + tileIndex * 5) % ATLAS_TILE_SIZE;
            atlas.setRGB(startX + x, startY + y, accentArgb);
            atlas.setRGB(startX + ((x + 3) % ATLAS_TILE_SIZE), startY + ((y + 5) % ATLAS_TILE_SIZE), shadowArgb);
        }
    }

    private void fillGrassSideTile(BufferedImage atlas, int tileIndex) {
        fillSolidTile(atlas, tileIndex, 0xFF7D593B);
        int startX = (tileIndex % ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        int startY = (tileIndex / ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < ATLAS_TILE_SIZE; x++) {
                atlas.setRGB(startX + x, startY + y, 0xFF4CB545);
            }
        }
        for (int x = 0; x < ATLAS_TILE_SIZE; x += 2) {
            atlas.setRGB(startX + x, startY + 3, 0xFF63C95B);
        }
    }

    private void fillWaterTile(BufferedImage atlas, int tileIndex, int argb) {
        fillSolidTile(atlas, tileIndex, argb);
        int startX = (tileIndex % ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        int startY = (tileIndex / ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        for (int y = 0; y < ATLAS_TILE_SIZE; y += 4) {
            for (int x = 0; x < ATLAS_TILE_SIZE; x++) {
                atlas.setRGB(startX + x, startY + y, 0xB06AA6FF);
            }
        }
    }

    private void fillLavaTile(BufferedImage atlas, int tileIndex) {
        fillSolidTile(atlas, tileIndex, 0xFFE97122);
        int startX = (tileIndex % ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        int startY = (tileIndex / ATLAS_COLUMNS) * ATLAS_TILE_SIZE;
        for (int y = 0; y < ATLAS_TILE_SIZE; y++) {
            for (int x = 0; x < ATLAS_TILE_SIZE; x += 3) {
                atlas.setRGB(startX + x, startY + y, (y + x) % 2 == 0 ? 0xFFFFB347 : 0xFFD95519);
            }
        }
    }

    private float[] getHeldBlockColor(byte block) {
        return getBlockFaceColor(block, Face.TOP);
    }

    private float[] getBlockFaceColor(byte block, Face face) {
        switch (block) {
            case GameConfig.GRASS:
                if (face == Face.TOP) {
                    return new float[]{0.37f, 0.76f, 0.34f};
                }
                return new float[]{0.50f, 0.36f, 0.24f};
            case GameConfig.DIRT:
                return new float[]{0.50f, 0.36f, 0.24f};
            case GameConfig.SAND:
                return new float[]{0.84f, 0.77f, 0.51f};
            case GameConfig.GRAVEL:
                return new float[]{0.44f, 0.45f, 0.43f};
            case GameConfig.CLAY:
                return new float[]{0.58f, 0.66f, 0.68f};
            case GameConfig.STONE:
                return new float[]{0.58f, 0.58f, 0.60f};
            case GameConfig.DEEPSLATE:
                return new float[]{0.28f, 0.29f, 0.32f};
            case GameConfig.COBBLESTONE:
                return new float[]{0.48f, 0.48f, 0.48f};
            case GameConfig.OBSIDIAN:
                return new float[]{0.12f, 0.08f, 0.18f};
            case GameConfig.BEDROCK:
                return new float[]{0.17f, 0.17f, 0.17f};
            case GameConfig.IRON_ORE:
            case GameConfig.DIAMOND_ORE:
            case GameConfig.COAL_ORE:
                return new float[]{0.58f, 0.58f, 0.60f};
            case GameConfig.DEEPSLATE_IRON_ORE:
            case GameConfig.DEEPSLATE_DIAMOND_ORE:
            case GameConfig.DEEPSLATE_COAL_ORE:
                return new float[]{0.28f, 0.29f, 0.32f};
            case GameConfig.OAK_LOG:
                return face == Face.TOP || face == Face.BOTTOM
                    ? new float[]{0.66f, 0.54f, 0.31f}
                    : new float[]{0.54f, 0.38f, 0.20f};
            case GameConfig.OAK_LEAVES:
                return new float[]{0.30f, 0.58f, 0.24f};
            case GameConfig.PINE_LOG:
                return face == Face.TOP || face == Face.BOTTOM
                    ? new float[]{0.58f, 0.48f, 0.30f}
                    : new float[]{0.38f, 0.27f, 0.16f};
            case GameConfig.PINE_LEAVES:
                return new float[]{0.14f, 0.38f, 0.18f};
            case GameConfig.BIRCH_LOG:
                return face == Face.TOP || face == Face.BOTTOM
                    ? new float[]{0.78f, 0.70f, 0.50f}
                    : new float[]{0.78f, 0.76f, 0.68f};
            case GameConfig.BIRCH_LEAVES:
                return new float[]{0.44f, 0.68f, 0.25f};
            case GameConfig.SNOW_BLOCK:
                return new float[]{0.86f, 0.91f, 0.95f};
            case GameConfig.SNOW_LAYER:
                return new float[]{0.78f, 0.86f, 0.92f};
            case GameConfig.CACTUS:
                return face == Face.TOP || face == Face.BOTTOM
                    ? new float[]{0.38f, 0.62f, 0.25f}
                    : new float[]{0.22f, 0.54f, 0.20f};
            case GameConfig.TALL_GRASS:
                return new float[]{0.42f, 0.77f, 0.31f};
            case GameConfig.DEAD_BUSH:
                return new float[]{0.46f, 0.30f, 0.16f};
            case GameConfig.SEAGRASS:
                return new float[]{0.18f, 0.55f, 0.36f};
            case GameConfig.KELP:
                return new float[]{0.14f, 0.44f, 0.22f};
            case GameConfig.RED_FLOWER:
                return new float[]{0.91f, 0.18f, 0.22f};
            case GameConfig.YELLOW_FLOWER:
                return new float[]{0.95f, 0.82f, 0.20f};
            case InventoryItems.OAK_PLANKS:
            case GameConfig.OAK_STAIRS:
                return new float[]{0.73f, 0.58f, 0.34f};
            case GameConfig.PINE_PLANKS:
            case GameConfig.PINE_STAIRS:
                return new float[]{0.50f, 0.34f, 0.18f};
            case GameConfig.BIRCH_PLANKS:
            case GameConfig.BIRCH_STAIRS:
                return new float[]{0.82f, 0.72f, 0.46f};
            case GameConfig.STONE_STAIRS:
                return new float[]{0.58f, 0.58f, 0.60f};
            case GameConfig.COBBLESTONE_STAIRS:
                return new float[]{0.48f, 0.48f, 0.48f};
            case GameConfig.OAK_FENCE:
            case GameConfig.OAK_FENCE_GATE:
                return new float[]{0.56f, 0.38f, 0.20f};
            case GameConfig.CHEST:
                return new float[]{0.62f, 0.39f, 0.17f};
            case GameConfig.CRAFTING_TABLE:
                return face == Face.TOP ? new float[]{0.63f, 0.46f, 0.25f} : new float[]{0.48f, 0.31f, 0.16f};
            case GameConfig.FURNACE:
                return face == Face.NORTH ? new float[]{0.40f, 0.40f, 0.38f} : new float[]{0.33f, 0.33f, 0.32f};
            case GameConfig.GLASS:
                return new float[]{0.72f, 0.90f, 0.95f};
            case GameConfig.WHEAT_CROP:
                return new float[]{0.86f, 0.72f, 0.26f};
            case GameConfig.CARROT_CROP:
                return new float[]{0.26f, 0.64f, 0.22f};
            case GameConfig.POTATO_CROP:
                return new float[]{0.38f, 0.58f, 0.24f};
            case GameConfig.RAIL:
                return new float[]{0.58f, 0.50f, 0.42f};
            case GameConfig.OAK_DOOR:
                return new float[]{0.50f, 0.30f, 0.13f};
            case GameConfig.FARMLAND:
                return face == Face.TOP ? new float[]{0.36f, 0.24f, 0.14f} : new float[]{0.44f, 0.31f, 0.20f};
            case GameConfig.TORCH:
                return new float[]{0.96f, 0.72f, 0.24f};
            case GameConfig.RED_BED:
                return face == Face.TOP ? new float[]{0.78f, 0.12f, 0.12f} : new float[]{0.76f, 0.62f, 0.38f};
            case GameConfig.STRUCTURE_BLOCK:
                return face == Face.TOP ? new float[]{0.70f, 0.55f, 0.86f} : new float[]{0.44f, 0.34f, 0.58f};
            case InventoryItems.STICK:
                return new float[]{0.58f, 0.40f, 0.22f};
            case InventoryItems.BREAD:
                return new float[]{0.78f, 0.50f, 0.24f};
            case InventoryItems.POTATO:
                return new float[]{0.64f, 0.48f, 0.25f};
            case InventoryItems.CARROT:
                return new float[]{0.92f, 0.46f, 0.12f};
            case InventoryItems.WHEAT_SEEDS:
                return new float[]{0.44f, 0.62f, 0.18f};
            case InventoryItems.IRON_INGOT:
            case InventoryItems.IRON_PICKAXE:
            case InventoryItems.IRON_SWORD:
            case InventoryItems.IRON_AXE:
            case InventoryItems.IRON_SHOVEL:
            case InventoryItems.IRON_HOE:
            case InventoryItems.IRON_HELMET:
            case InventoryItems.IRON_CHESTPLATE:
            case InventoryItems.IRON_LEGGINGS:
            case InventoryItems.IRON_BOOTS:
                return new float[]{0.76f, 0.78f, 0.82f};
            case InventoryItems.COAL_ITEM:
                return new float[]{0.08f, 0.08f, 0.09f};
            case InventoryItems.DIAMOND_ITEM:
                return new float[]{0.38f, 0.86f, 0.92f};
            case InventoryItems.ITEM_BUCKET:
                return new float[]{0.58f, 0.60f, 0.64f};
            case InventoryItems.ITEM_WATER_BUCKET:
                return new float[]{0.30f, 0.52f, 0.88f};
            case InventoryItems.ITEM_LAVA_BUCKET:
                return new float[]{0.92f, 0.38f, 0.12f};
            case InventoryItems.WOODEN_PICKAXE:
            case InventoryItems.WOODEN_SWORD:
            case InventoryItems.WOODEN_AXE:
            case InventoryItems.WOODEN_SHOVEL:
            case InventoryItems.WOODEN_HOE:
                return new float[]{0.64f, 0.43f, 0.22f};
            case InventoryItems.STONE_PICKAXE:
            case InventoryItems.STONE_SWORD:
            case InventoryItems.STONE_AXE:
            case InventoryItems.STONE_SHOVEL:
            case InventoryItems.STONE_HOE:
                return new float[]{0.52f, 0.52f, 0.52f};
            case InventoryItems.DIAMOND_PICKAXE:
            case InventoryItems.DIAMOND_SWORD:
            case InventoryItems.DIAMOND_AXE:
            case InventoryItems.DIAMOND_SHOVEL:
            case InventoryItems.DIAMOND_HOE:
            case InventoryItems.DIAMOND_HELMET:
            case InventoryItems.DIAMOND_CHESTPLATE:
            case InventoryItems.DIAMOND_LEGGINGS:
            case InventoryItems.DIAMOND_BOOTS:
                return new float[]{0.38f, 0.86f, 0.92f};
            case InventoryItems.NETHERITE_PICKAXE:
            case InventoryItems.NETHERITE_SWORD:
            case InventoryItems.NETHERITE_AXE:
            case InventoryItems.NETHERITE_SHOVEL:
            case InventoryItems.NETHERITE_HOE:
            case InventoryItems.NETHERITE_HELMET:
            case InventoryItems.NETHERITE_CHESTPLATE:
            case InventoryItems.NETHERITE_LEGGINGS:
            case InventoryItems.NETHERITE_BOOTS:
                return new float[]{0.34f, 0.33f, 0.38f};
            case InventoryItems.SHIELD:
                return new float[]{0.58f, 0.42f, 0.22f};
            case InventoryItems.TOTEM:
                return new float[]{0.92f, 0.78f, 0.24f};
            case InventoryItems.PIG_SPAWN_EGG:
                return new float[]{0.95f, 0.58f, 0.70f};
            case InventoryItems.SHEEP_SPAWN_EGG:
                return new float[]{0.90f, 0.90f, 0.84f};
            case InventoryItems.COW_SPAWN_EGG:
                return new float[]{0.42f, 0.28f, 0.18f};
            case InventoryItems.VILLAGER_SPAWN_EGG:
                return new float[]{0.58f, 0.40f, 0.24f};
            case InventoryItems.ZOMBIE_SPAWN_EGG:
                return new float[]{0.38f, 0.69f, 0.33f};
            case InventoryItems.SKELETON_SPAWN_EGG:
                return new float[]{0.82f, 0.82f, 0.78f};
            case InventoryItems.HERRING_SPAWN_EGG:
                return new float[]{0.20f, 0.55f, 0.72f};
            case InventoryItems.SALMON_SPAWN_EGG:
                return new float[]{0.86f, 0.42f, 0.34f};
            case InventoryItems.RAW_PORK:
                return new float[]{0.86f, 0.46f, 0.50f};
            case InventoryItems.RAW_BEEF:
                return new float[]{0.58f, 0.18f, 0.14f};
            case InventoryItems.RAW_MUTTON:
                return new float[]{0.72f, 0.28f, 0.30f};
            case InventoryItems.RAW_HERRING:
                return new float[]{0.22f, 0.58f, 0.72f};
            case InventoryItems.RAW_SALMON:
                return new float[]{0.88f, 0.42f, 0.34f};
            case InventoryItems.COOKED_PORK:
                return new float[]{0.82f, 0.52f, 0.34f};
            case InventoryItems.COOKED_BEEF:
                return new float[]{0.48f, 0.23f, 0.12f};
            case InventoryItems.COOKED_MUTTON:
                return new float[]{0.58f, 0.30f, 0.18f};
            case InventoryItems.COOKED_HERRING:
                return new float[]{0.62f, 0.54f, 0.38f};
            case InventoryItems.COOKED_SALMON:
                return new float[]{0.90f, 0.56f, 0.34f};
            case InventoryItems.BAKED_POTATO:
                return new float[]{0.78f, 0.58f, 0.30f};
            case InventoryItems.LEATHER:
                return new float[]{0.50f, 0.28f, 0.14f};
            case InventoryItems.WOOL:
                return new float[]{0.88f, 0.88f, 0.82f};
            case InventoryItems.ROTTEN_FLESH:
                return new float[]{0.48f, 0.34f, 0.20f};
            case InventoryItems.BONE:
                return new float[]{0.86f, 0.84f, 0.74f};
            case GameConfig.ZOMBIE_SKIN:
                return new float[]{0.38f, 0.69f, 0.33f};
            case GameConfig.ZOMBIE_SHIRT:
                return new float[]{0.23f, 0.51f, 0.57f};
            case GameConfig.ZOMBIE_PANTS:
                return new float[]{0.25f, 0.27f, 0.53f};
            case GameConfig.ZOMBIE_EYE:
                return new float[]{0.08f, 0.08f, 0.08f};
            case GameConfig.WATER:
            case GameConfig.WATER_SOURCE:
            case GameConfig.WATER_FLOWING:
                return new float[]{0.35f, 0.55f, 0.90f};
            case GameConfig.LAVA:
            case GameConfig.LAVA_SOURCE:
            case GameConfig.LAVA_FLOWING:
                return new float[]{0.92f, 0.42f, 0.12f};
            default:
                return new float[]{1.0f, 0.0f, 1.0f};
        }
    }

    private boolean isOreBlock(byte block) {
        return block == GameConfig.IRON_ORE
            || block == GameConfig.DIAMOND_ORE
            || block == GameConfig.COAL_ORE
            || block == GameConfig.DEEPSLATE_IRON_ORE
            || block == GameConfig.DEEPSLATE_DIAMOND_ORE
            || block == GameConfig.DEEPSLATE_COAL_ORE;
    }

    private float[] oreAccentColor(byte block, float brightness) {
        float red;
        float green;
        float blue;
        switch (block) {
            case GameConfig.IRON_ORE:
            case GameConfig.DEEPSLATE_IRON_ORE:
                red = 0.86f;
                green = 0.70f;
                blue = 0.54f;
                break;
            case GameConfig.DIAMOND_ORE:
            case GameConfig.DEEPSLATE_DIAMOND_ORE:
                red = 0.28f;
                green = 0.92f;
                blue = 0.96f;
                break;
            case GameConfig.COAL_ORE:
            case GameConfig.DEEPSLATE_COAL_ORE:
                red = 0.06f;
                green = 0.06f;
                blue = 0.06f;
                break;
            default:
                red = 1.0f;
                green = 0.0f;
                blue = 1.0f;
                break;
        }
        return new float[]{
            clampColor(red * brightness),
            clampColor(green * brightness),
            clampColor(blue * brightness),
            1.0f
        };
    }

    private int oreAccentColorPacked(byte block, float brightness) {
        float red;
        float green;
        float blue;
        switch (block) {
            case GameConfig.IRON_ORE:
            case GameConfig.DEEPSLATE_IRON_ORE:
                red = 0.86f;
                green = 0.70f;
                blue = 0.54f;
                break;
            case GameConfig.DIAMOND_ORE:
            case GameConfig.DEEPSLATE_DIAMOND_ORE:
                red = 0.28f;
                green = 0.92f;
                blue = 0.96f;
                break;
            case GameConfig.COAL_ORE:
            case GameConfig.DEEPSLATE_COAL_ORE:
                red = 0.06f;
                green = 0.06f;
                blue = 0.06f;
                break;
            default:
                red = 1.0f;
                green = 0.0f;
                blue = 1.0f;
                break;
        }
        return packColor(red * brightness, green * brightness, blue * brightness, 1.0f);
    }

    private void shadeColor(float red, float green, float blue, float brightness) {
        glColor3f(
            clampColor(red * brightness * currentSceneBrightness),
            clampColor(green * brightness * currentSceneBrightness),
            clampColor(blue * brightness * currentSceneBrightness)
        );
    }

    private float clampColor(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private float lerp(float start, float end, float amount) {
        return start + (end - start) * Math.max(0.0f, Math.min(1.0f, amount));
    }

    private double lerpDouble(double start, double end, double amount) {
        return start + (end - start) * Math.max(0.0, Math.min(1.0, amount));
    }

    private void updateCameraEffects(boolean sprinting, int fovDegrees, double deltaTime) {
        double targetFov = clamp(fovDegrees, 55, 100) + (sprinting ? GameConfig.SPRINT_FOV_BOOST : 0.0);
        double blend = Math.min(1.0, deltaTime * 8.0);
        currentFovDegrees += (targetFov - currentFovDegrees) * blend;
    }

    private void markChunkColumnDirty(int chunkX, int chunkZ) {
        for (int chunkY = 0; chunkY < GameConfig.WORLD_CHUNKS_Y; chunkY++) {
            markChunkDirty(chunkX, chunkY, chunkZ);
        }
    }

    private void refreshCameraInsideBlockMesh() {
        boolean insideSolid = isCameraInsideSolidBlock();
        if (!insideSolid && !lastCameraInsideSolidBlock && !lastSpectatorInsideBlock && !spectatorInsideBlock) {
            return;
        }
        if (insideSolid
            && lastCameraInsideSolidBlock
            && cameraBlockX == lastCameraSolidBlockX
            && cameraBlockY == lastCameraSolidBlockY
            && cameraBlockZ == lastCameraSolidBlockZ
            && spectatorInsideBlock == lastSpectatorInsideBlock) {
            return;
        }

        if (lastCameraInsideSolidBlock) {
            markCameraNeighborMeshes(lastCameraSolidBlockX, lastCameraSolidBlockY, lastCameraSolidBlockZ);
        }
        if (insideSolid) {
            markCameraNeighborMeshes(cameraBlockX, cameraBlockY, cameraBlockZ);
            lastCameraSolidBlockX = cameraBlockX;
            lastCameraSolidBlockY = cameraBlockY;
            lastCameraSolidBlockZ = cameraBlockZ;
        } else {
            lastCameraSolidBlockX = Integer.MIN_VALUE;
            lastCameraSolidBlockY = Integer.MIN_VALUE;
            lastCameraSolidBlockZ = Integer.MIN_VALUE;
        }

        lastCameraInsideSolidBlock = insideSolid;
        lastSpectatorInsideBlock = spectatorInsideBlock;
    }

    private boolean isCameraInsideSolidBlock() {
        return world.isSolidBlock(world.getBlock(cameraBlockX, cameraBlockY, cameraBlockZ));
    }

    private void markCameraNeighborMeshes(int blockX, int blockY, int blockZ) {
        if (!GameConfig.isWorldYInside(blockY)) {
            return;
        }
        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_SIZE);
        int chunkY = GameConfig.sectionIndexForY(blockY);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_SIZE);
        markChunkDirty(chunkX, chunkY, chunkZ);
        markChunkDirty(Math.floorDiv(blockX + 1, GameConfig.CHUNK_SIZE), chunkY, chunkZ);
        markChunkDirty(Math.floorDiv(blockX - 1, GameConfig.CHUNK_SIZE), chunkY, chunkZ);
        if (GameConfig.isWorldYInside(blockY + 1)) {
            markChunkDirty(chunkX, GameConfig.sectionIndexForY(blockY + 1), chunkZ);
        }
        if (GameConfig.isWorldYInside(blockY - 1)) {
            markChunkDirty(chunkX, GameConfig.sectionIndexForY(blockY - 1), chunkZ);
        }
        markChunkDirty(chunkX, chunkY, Math.floorDiv(blockZ + 1, GameConfig.CHUNK_SIZE));
        markChunkDirty(chunkX, chunkY, Math.floorDiv(blockZ - 1, GameConfig.CHUNK_SIZE));
    }

    private void markChunkDirty(int chunkX, int chunkY, int chunkZ) {
        markChunkDirty(chunkX, chunkY, chunkZ, false);
    }

    private void markChunkDirty(int chunkX, int chunkY, int chunkZ, boolean immediate) {
        if (chunkY < 0 || chunkY >= GameConfig.WORLD_CHUNKS_Y) {
            return;
        }
        long key = chunkKey(chunkX, chunkY, chunkZ);
        meshBuildRevisions.invalidate(key);
        dirtyChunkMeshes.add(key);
        if (immediate) {
            immediateChunkMeshes.add(key);
            meshBuildCooldownUntilNanos.remove(key);
        }
        if (meshBuildsInFlight.containsKey(key) || meshBuildsReady.containsKey(key)) {
            return;
        }
        enqueueDirtyMeshBuild(key);
    }

    private UiRenderer.InventoryUiLayout buildInventoryLayout(boolean creativeMode, int creativeTab) {
        return buildInventoryLayout(creativeMode, creativeTab, GameConfig.INVENTORY_SCREEN_PLAYER);
    }

    private UiRenderer.InventoryUiLayout buildInventoryLayout(boolean creativeMode, int creativeTab, int inventoryScreenMode) {
        return buildInventoryLayout(creativeMode, creativeTab, 0, inventoryScreenMode);
    }

    private UiRenderer.InventoryUiLayout buildInventoryLayout(boolean creativeMode, int creativeTab, int creativeScrollOffset, int inventoryScreenMode) {
        return uiRenderer.buildInventoryLayout(creativeMode, creativeTab, framebufferWidth, framebufferHeight, getInventoryUiScale(), inventoryScreenMode, creativeScrollOffset);
    }

    private void verifyOpenGl(String step) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new IllegalStateException("OpenGL error at " + step + ": " + error);
        }
    }

    private void logOpenGlError(String step) {
        if (!GameConfig.ENABLE_DEBUG_LOGS) {
            return;
        }
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("OpenGL warning at " + step + ": " + error);
        }
    }

    private float getUiScale() {
        return Math.max(1.4f, framebufferHeight / 720.0f * 1.4f);
    }

    private float getHudInventoryScale() {
        return getUiScale() * Settings.guiScaleMultiplier(framebufferWidth, framebufferHeight) / 1.18f;
    }

    private float getChatUiScale() {
        return getUiScale() * Settings.guiScaleMultiplier(framebufferWidth, framebufferHeight) / 1.18f;
    }

    private float getInventoryUiScale() {
        float desired = Math.max(1.0f, framebufferHeight / 720.0f * 1.18f * Settings.guiScaleMultiplier(framebufferWidth, framebufferHeight));
        float maxByWidth = Math.max(0.85f, (framebufferWidth - 32.0f) / 470.0f);
        float maxByHeight = Math.max(0.85f, (framebufferHeight - 32.0f) / 388.0f);
        return Math.max(0.85f, Math.min(desired, Math.min(maxByWidth, maxByHeight)));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static final class OpaqueRenderRegion {
        final int chunkX;
        final int chunkY;
        final int chunkZ;
        int vaoId;
        int vboId;
        int vertexCount;
        int version;
        boolean buildInFlight;
        long revealStartNanos;
        final HashSet<Long> includedMeshKeys = new HashSet<>();

        OpaqueRenderRegion(int chunkX, int chunkY, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.chunkZ = chunkZ;
        }
    }

    private static final class OpaqueRegionBuildSnapshot {
        final long regionKey;
        final int version;
        final int epoch;
        final int[][] vertices;
        final int[] intCounts;
        final int[] localChunkXs;
        final int[] localChunkZs;
        final long[] meshKeys;
        final int entryCount;

        OpaqueRegionBuildSnapshot(long regionKey, int version, int epoch, int[][] vertices, int[] intCounts,
                                  int[] localChunkXs, int[] localChunkZs, long[] meshKeys, int entryCount) {
            this.regionKey = regionKey;
            this.version = version;
            this.epoch = epoch;
            this.vertices = vertices;
            this.intCounts = intCounts;
            this.localChunkXs = localChunkXs;
            this.localChunkZs = localChunkZs;
            this.meshKeys = meshKeys;
            this.entryCount = entryCount;
        }
    }

    private static final class OpaqueRegionBuildResult {
        final long regionKey;
        final int version;
        final int epoch;
        final int[] vertices;
        final long[] includedMeshKeys;
        final RuntimeException failure;

        OpaqueRegionBuildResult(long regionKey, int version, int epoch, int[] vertices, long[] includedMeshKeys) {
            this(regionKey, version, epoch, vertices, includedMeshKeys, null);
        }

        private OpaqueRegionBuildResult(long regionKey, int version, int epoch, int[] vertices,
                                        long[] includedMeshKeys, RuntimeException failure) {
            this.regionKey = regionKey;
            this.version = version;
            this.epoch = epoch;
            this.vertices = vertices;
            this.includedMeshKeys = includedMeshKeys;
            this.failure = failure;
        }

        static OpaqueRegionBuildResult failed(OpaqueRegionBuildSnapshot snapshot, RuntimeException failure) {
            return new OpaqueRegionBuildResult(
                snapshot.regionKey,
                snapshot.version,
                snapshot.epoch,
                new int[0],
                new long[0],
                failure
            );
        }
    }

    static final class MeshBuildRadiusController {
        private final int initialRadius;
        private int currentRadius;

        MeshBuildRadiusController(int initialRadius) {
            this.initialRadius = Math.max(1, initialRadius);
        }

        int current(int targetRadius) {
            int safeTargetRadius = Math.max(1, targetRadius);
            if (currentRadius <= 0 || currentRadius > safeTargetRadius) {
                currentRadius = Math.min(initialRadius, safeTargetRadius);
            }
            return currentRadius;
        }

        int update(int targetRadius, boolean currentRadiusReady) {
            int safeTargetRadius = Math.max(1, targetRadius);
            current(safeTargetRadius);
            if (currentRadiusReady && currentRadius < safeTargetRadius) {
                currentRadius++;
            }
            return currentRadius;
        }

        int value() {
            return currentRadius;
        }

        void reset() {
            currentRadius = 0;
        }
    }

    static final class MeshBuildRevisionTracker {
        private final ConcurrentHashMap<Long, Integer> revisions = new ConcurrentHashMap<>();

        int current(long meshKey) {
            Integer revision = revisions.get(meshKey);
            return revision == null ? 0 : revision.intValue();
        }

        int invalidate(long meshKey) {
            return revisions.compute(meshKey, (key, revision) -> {
                if (revision == null || revision.intValue() == Integer.MAX_VALUE) {
                    return Integer.valueOf(1);
                }
                return Integer.valueOf(revision.intValue() + 1);
            }).intValue();
        }

        boolean isCurrent(long meshKey, int buildRevision) {
            return current(meshKey) == buildRevision;
        }

        void forget(long meshKey) {
            revisions.remove(meshKey);
        }

        void forgetIf(java.util.function.LongPredicate predicate) {
            for (Long meshKey : revisions.keySet()) {
                if (predicate.test(meshKey.longValue())) {
                    revisions.remove(meshKey);
                }
            }
        }

        void clear() {
            revisions.clear();
        }
    }

    private static final class MeshBuildCandidate implements Comparable<MeshBuildCandidate> {
        final int chunkX;
        final int chunkY;
        final int chunkZ;
        final long meshKey;
        final double distanceSquared;
        final boolean immediate;

        MeshBuildCandidate(int chunkX, int chunkY, int chunkZ, long meshKey, double distanceSquared, boolean immediate) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.chunkZ = chunkZ;
            this.meshKey = meshKey;
            this.distanceSquared = distanceSquared;
            this.immediate = immediate;
        }

        @Override
        public int compareTo(MeshBuildCandidate other) {
            if (immediate != other.immediate) {
                return immediate ? -1 : 1;
            }
            int distanceCompare = Double.compare(distanceSquared, other.distanceSquared);
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return Long.compare(meshKey, other.meshKey);
        }
    }

    private static final class MeshBuildContext {
        final int cameraBlockX;
        final int cameraBlockY;
        final int cameraBlockZ;
        final boolean spectatorInsideBlock;
        final double sortCameraX;
        final double sortCameraY;
        final double sortCameraZ;
        final int epoch;

        MeshBuildContext(int cameraBlockX, int cameraBlockY, int cameraBlockZ, boolean spectatorInsideBlock,
                         double sortCameraX, double sortCameraY, double sortCameraZ, int epoch) {
            this.cameraBlockX = cameraBlockX;
            this.cameraBlockY = cameraBlockY;
            this.cameraBlockZ = cameraBlockZ;
            this.spectatorInsideBlock = spectatorInsideBlock;
            this.sortCameraX = sortCameraX;
            this.sortCameraY = sortCameraY;
            this.sortCameraZ = sortCameraZ;
            this.epoch = epoch;
        }
    }

    private static final class GpuMeshBuffer {
        static final GpuMeshBuffer EMPTY = new GpuMeshBuffer(0, 0, 0);

        final int vaoId;
        final int vboId;
        final int vertexCount;

        GpuMeshBuffer(int vaoId, int vboId, int vertexCount) {
            this.vaoId = vaoId;
            this.vboId = vboId;
            this.vertexCount = vertexCount;
        }
    }

    private static final class MeshBuildResult {
        final int chunkX;
        final int chunkY;
        final int chunkZ;
        final long meshKey;
        final int version;
        final int buildRevision;
        final int[] opaqueVertices;
        final int opaqueIntCount;
        final int[] transparentVertices;
        final int transparentIntCount;
        final boolean empty;
        final boolean readyForUpload;
        final int epoch;

        MeshBuildResult(int chunkX, int chunkY, int chunkZ, long meshKey, int version, int buildRevision,
                        int[] opaqueVertices, int opaqueIntCount,
                        int[] transparentVertices, int transparentIntCount,
                        boolean readyForUpload, int epoch) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.chunkZ = chunkZ;
            this.meshKey = meshKey;
            this.version = version;
            this.buildRevision = buildRevision;
            this.opaqueVertices = opaqueVertices;
            this.opaqueIntCount = opaqueIntCount;
            this.transparentVertices = transparentVertices;
            this.transparentIntCount = transparentIntCount;
            this.empty = opaqueIntCount == 0 && transparentIntCount == 0;
            this.readyForUpload = readyForUpload;
            this.epoch = epoch;
        }

        static MeshBuildResult empty(int chunkX, int chunkY, int chunkZ, long meshKey, int version, int buildRevision, boolean readyForUpload, int epoch) {
            return new MeshBuildResult(chunkX, chunkY, chunkZ, meshKey, version, buildRevision, new int[0], 0, new int[0], 0, readyForUpload, epoch);
        }
    }

    private static final class Frustum {
        private final Plane[] planes = {
            new Plane(), new Plane(), new Plane(), new Plane(), new Plane(), new Plane()
        };

        void set(double cameraX, double cameraY, double cameraZ,
                 double forwardX, double forwardY, double forwardZ,
                 double fovDegrees, double aspect, double near, double far) {
            double forwardLength = Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
            if (forwardLength <= 1e-8) {
                forwardX = 1.0;
                forwardY = 0.0;
                forwardZ = 0.0;
                forwardLength = 1.0;
            }
            forwardX /= forwardLength;
            forwardY /= forwardLength;
            forwardZ /= forwardLength;

            double rightX = -forwardZ;
            double rightZ = forwardX;
            double rightLength = Math.sqrt(rightX * rightX + rightZ * rightZ);
            if (rightLength <= 1e-8) {
                rightX = 1.0;
                rightZ = 0.0;
                rightLength = 1.0;
            }
            rightX /= rightLength;
            rightZ /= rightLength;
            double upX = -rightZ * forwardY;
            double upY = rightZ * forwardX - rightX * forwardZ;
            double upZ = rightX * forwardY;
            double upLength = Math.sqrt(upX * upX + upY * upY + upZ * upZ);
            if (upLength <= 1e-8) {
                upX = 0.0;
                upY = 1.0;
                upZ = 0.0;
                upLength = 1.0;
            }
            upX /= upLength;
            upY /= upLength;
            upZ /= upLength;

            double tanY = Math.tan(Math.toRadians(fovDegrees) * 0.5);
            double tanX = tanY * aspect;
            planes[0].set(forwardX, forwardY, forwardZ,
                cameraX + forwardX * near, cameraY + forwardY * near, cameraZ + forwardZ * near);
            planes[1].set(-forwardX, -forwardY, -forwardZ,
                cameraX + forwardX * far, cameraY + forwardY * far, cameraZ + forwardZ * far);
            planes[2].set(rightX + forwardX * tanX, forwardY * tanX, rightZ + forwardZ * tanX, cameraX, cameraY, cameraZ);
            planes[3].set(-rightX + forwardX * tanX, forwardY * tanX, -rightZ + forwardZ * tanX, cameraX, cameraY, cameraZ);
            planes[4].set(upX + forwardX * tanY, upY + forwardY * tanY, upZ + forwardZ * tanY, cameraX, cameraY, cameraZ);
            planes[5].set(-upX + forwardX * tanY, -upY + forwardY * tanY, -upZ + forwardZ * tanY, cameraX, cameraY, cameraZ);
        }

        boolean intersectsAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            for (Plane plane : planes) {
                double x = plane.x >= 0.0 ? maxX : minX;
                double y = plane.y >= 0.0 ? maxY : minY;
                double z = plane.z >= 0.0 ? maxZ : minZ;
                if (plane.distance(x, y, z) < 0.0) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class Plane {
        double x;
        double y;
        double z;
        double d;

        void set(double normalX, double normalY, double normalZ, double pointX, double pointY, double pointZ) {
            double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            if (length <= 1e-8) {
                x = 1.0;
                y = 0.0;
                z = 0.0;
                d = -pointX;
                return;
            }
            x = normalX / length;
            y = normalY / length;
            z = normalZ / length;
            d = -(x * pointX + y * pointY + z * pointZ);
        }

        double distance(double pointX, double pointY, double pointZ) {
            return x * pointX + y * pointY + z * pointZ + d;
        }
    }

    static final class IntVertexBuilder {
        private int[] values;
        private int[] sortScratch;
        private long[] sortOrder;
        private int size;

        IntVertexBuilder(int initialCapacity) {
            values = new int[Math.max(INTS_PER_VERTEX * 4, initialCapacity)];
        }

        void putPacked(double x, double y, double z, int color, int ao) {
            ensureCapacity(size + INTS_PER_VERTEX);
            values[size++] = packPositionAndAo(x, y, z, ao);
            values[size++] = color;
        }

        private void ensureCapacity(int required) {
            if (required > values.length) {
                int[] next = new int[Math.max(required, values.length * 2)];
                System.arraycopy(values, 0, next, 0, values.length);
                values = next;
            }
        }

        int intCount() {
            return size;
        }

        int[] toArray() {
            if (size == 0) {
                return new int[0];
            }
            int[] copy = new int[size];
            System.arraycopy(values, 0, copy, 0, size);
            return copy;
        }

        void sortQuadsFarToNear(double cameraX, double cameraY, double cameraZ) {
            int intsPerQuad = INTS_PER_VERTEX * VERTICES_PER_QUAD;
            int quadCount = size / intsPerQuad;
            if (quadCount <= 1) {
                return;
            }

            if (sortOrder == null || sortOrder.length < quadCount) {
                sortOrder = new long[quadCount];
            }
            for (int quad = 0; quad < quadCount; quad++) {
                int base = quad * intsPerQuad;
                double centerX = 0.0;
                double centerY = 0.0;
                double centerZ = 0.0;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int packed = values[base + vertex * INTS_PER_VERTEX];
                    centerX += unpackPackedCoord(packed & 1023);
                    centerY += unpackPackedCoord((packed >>> 10) & 1023);
                    centerZ += unpackPackedCoord((packed >>> 20) & 1023);
                }
                centerX *= 0.25;
                centerY *= 0.25;
                centerZ *= 0.25;
                double dx = centerX - cameraX;
                double dy = centerY - cameraY;
                double dz = centerZ - cameraZ;
                long distanceKey = Math.min(0x7FFFFFFFL, Math.max(0L, Math.round((dx * dx + dy * dy + dz * dz) * 1024.0)));
                sortOrder[quad] = ((0x7FFFFFFFL - distanceKey) << 32) | (quad & 0xFFFFFFFFL);
            }

            java.util.Arrays.sort(sortOrder, 0, quadCount);
            if (sortScratch == null || sortScratch.length < size) {
                sortScratch = new int[size];
            }
            for (int targetQuad = 0; targetQuad < quadCount; targetQuad++) {
                int sourceOffset = (int) sortOrder[targetQuad] * intsPerQuad;
                int targetOffset = targetQuad * intsPerQuad;
                System.arraycopy(values, sourceOffset, sortScratch, targetOffset, intsPerQuad);
            }
            int[] previous = values;
            values = sortScratch;
            sortScratch = previous;
        }

        private static int packPositionAndAo(double x, double y, double z, int ao) {
            int packedX = packCoord(x);
            int packedY = packCoord(y);
            int packedZ = packCoord(z);
            return packedX | (packedY << 10) | (packedZ << 20) | ((ao & 3) << 30);
        }

        private static int packCoord(double value) {
            int packed = (int) Math.round((value + PACKED_VERTEX_BIAS) * PACKED_VERTEX_SCALE);
            return Math.max(0, Math.min(PACKED_VERTEX_MAX, packed));
        }

        private static double unpackPackedCoord(int packed) {
            return packed / PACKED_VERTEX_SCALE - PACKED_VERTEX_BIAS;
        }
    }

    private static final class TextTexture {
        final int textureId;
        final int width;
        final int height;
        final int baseline;

        TextTexture(int textureId, int width, int height, int baseline) {
            this.textureId = textureId;
            this.width = width;
            this.height = height;
            this.baseline = baseline;
        }
    }
}
