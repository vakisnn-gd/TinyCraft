import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_8;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_COMPAT_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RAW_MOUSE_MOTION;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TinyCraft implements MultiplayerManager.Listener {
    private final long bootstrapSeed = System.nanoTime() ^ new Random().nextLong();
    private final PlayerState player = new PlayerState();
    private final VoxelWorld world = new VoxelWorld(bootstrapSeed);
    private final OpenGlRenderer renderer = new OpenGlRenderer(world);
    private final AudioEngine audio = new AudioEngine();
    private final PlayerInventory inventory = new PlayerInventory();
    private final PlayerController playerController = new PlayerController(
        world,
        player,
        audio,
        new PlayerController.Host() {
            @Override
            public boolean isCreativeMode() {
                return creativeMode;
            }

            @Override
            public boolean isSpectatorMode() {
                return spectatorMode;
            }

            @Override
            public boolean isDeathScreenActive() {
                return deathScreenActive;
            }

            @Override
            public int currentWorldDifficulty() {
                return currentWorldDifficulty;
            }

            @Override
            public void enterDeathScreen() {
                TinyCraft.this.enterDeathScreen();
            }
        },
        TinyCraft::currentGlfwTime
    );
    private final InputController inputController = new InputController(
        playerController,
        player,
        inventory,
        world,
        () -> TinyCraft.this.paused
            || TinyCraft.this.inventoryOpen
            || TinyCraft.this.deathScreenActive
            || TinyCraft.this.mainMenuActive
            || TinyCraft.this.chat.isActive()
    );
    private final PlayerState renderPlayer = new PlayerState();
    private final Random ambientRandom = new Random();
    private final ArrayList<WorldInfo> availableWorlds = new ArrayList<>();
    private final FrameProfiler frameProfiler = new FrameProfiler();
    private final ChatSystem chat = new ChatSystem();
    private final LocalProfile localProfile = LocalProfile.loadOrCreate();
    private final MultiplayerManager multiplayer = new MultiplayerManager(localProfile, this);

    private GLFWErrorCallback errorCallback;
    private long window;

    private boolean creativeMode;
    private boolean spectatorMode;
    private boolean thirdPersonView;
    private boolean frontThirdPersonView;
    private boolean paused;
    private boolean deathScreenActive;
    private boolean inventoryDroppedOnDeath;
    private boolean mainMenuActive;
    private boolean inventoryOpen;
    private int inventoryScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
    private ContainerInventory activeChestContainer;
    private FurnaceBlockEntity activeFurnace;
    private boolean showDebugInfo;
    private boolean showChunkBorders;
    private boolean hideHud;
    private boolean screenshotCaptureRequested;
    private boolean panoramaCaptureRequested;
    private boolean worldLoaded;
    private boolean multiplayerWorldLoading;
    private long multiplayerWorldLoadingStartedMillis;
    private boolean mainMenuWorldActionsEnabled;
    private int pauseSelection;
    private int deathSelection;
    private int mainMenuScreen;
    private int mainMenuSelection;
    private int selectedWorldIndex;
    private int mainMenuScrollOffset;
    private int selectedSlot;
    private int createWorldGameMode;
    private boolean createWorldAllowCheats;
    private int createWorldDifficulty = 2;
    private int createWorldTerrainPreset = TerrainPreset.NEW_WORLD_DEFAULT.createWorldOptionIndex();
    private int currentWorldDifficulty = 2;
    private boolean currentWorldAllowCheats;
    private int renderDistanceChunks = GameConfig.CHUNK_RENDER_DISTANCE;
    private int fovDegrees = (int) GameConfig.FOV_DEGREES;
    private int maxFps = 144;
    private boolean optionsOpenedFromPause;
    private boolean lanOpenedFromPause;
    private byte selectedBlock = GameConfig.GRASS;
    private String createWorldName = "New World";
    private String createWorldSeed = "";
    private String renameWorldName = "";
    private String multiplayerHost = "127.0.0.1";
    private String multiplayerPort = Integer.toString(MultiplayerProtocol.DEFAULT_PORT);
    private String multiplayerName = localProfile.name;
    private String multiplayerStatus = "Offline";
    private int lanGameMode = 0;
    private boolean lanAllowCheats = false;
    private RayHit hoveredBlock;
    private int breakingBlockX = -1;
    private int breakingBlockY = -1;
    private int breakingBlockZ = -1;
    private double breakingTimer;
    private double breakingDuration;
    private RayHit breakingHit;
    private boolean fullscreen;
    private int windowedX;
    private int windowedY;
    private int windowedWidth = GameConfig.WINDOW_WIDTH;
    private int windowedHeight = GameConfig.WINDOW_HEIGHT;
    private double lastWorldSlotClickTime = -1.0;
    private double simulationAccumulator;
    private double previousPlayerX;
    private double previousPlayerY;
    private double previousPlayerZ;
    private boolean interpolationInitialized;
    private String loadedWorldName;
    private int lastWorldSlotClickIndex = -1;

    public static void main(String[] args) {
        if (args.length > 0 && "--profile-world".equals(args[0])) {
            runWorldProfile(args);
            return;
        }
        new TinyCraft().run();
    }

    private static void runWorldProfile(String[] args) {
        long profileSeed = args.length > 1 ? parseProfileSeed(args[1]) : 0x5cfa085fdacb6033L;
        int radius = args.length > 2 ? Math.max(1, parseProfileInt(args[2], 6)) : 6;
        TerrainPreset profilePreset = args.length > 3 ? TerrainPreset.fromMetadata(args[3]) : TerrainPreset.NEW_WORLD_DEFAULT;
        if (profilePreset == TerrainPreset.LEGACY && args.length <= 3) {
            profilePreset = TerrainPreset.NEW_WORLD_DEFAULT;
        }
        WorldGenerator generator = new WorldGenerator(profileSeed, profilePreset);
        int columns = 0;
        ArrayList<Long> generationSamples = new ArrayList<>();
        long generationNs = 0L;
        long copyNs = 0L;
        long generationMaxNs = 0L;
        long copyMaxNs = 0L;
        generator.generateChunk(radius + 1000, radius + 1000);
        generator.generateChunk(-radius - 1000, radius + 1001);
        for (int chunkX = -radius; chunkX <= radius; chunkX++) {
            for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                long generationStartNs = System.nanoTime();
                GeneratedChunkColumn generated = generator.generateChunk(chunkX, chunkZ);
                long generationElapsedNs = System.nanoTime() - generationStartNs;
                generationSamples.add(generationElapsedNs);
                long copyStartNs = System.nanoTime();
                VoxelWorld.ChunkColumn column = new VoxelWorld.ChunkColumn(chunkX, chunkZ, false);
                Chunk[] generatedSections = generated.sections();
                for (int chunkY = 0; chunkY < GameConfig.SECTION_COUNT; chunkY++) {
                    column.sections[chunkY] = generatedSections[chunkY];
                }
                for (int localX = 0; localX < GameConfig.CHUNK_SIZE; localX++) {
                    for (int localZ = 0; localZ < GameConfig.CHUNK_SIZE; localZ++) {
                        column.setSurfaceHeightLocal(localX, localZ, generated.getSurfaceHeight(localX, localZ));
                    }
                }
                long copyElapsedNs = System.nanoTime() - copyStartNs;
                generationNs += generationElapsedNs;
                copyNs += copyElapsedNs;
                generationMaxNs = Math.max(generationMaxNs, generationElapsedNs);
                copyMaxNs = Math.max(copyMaxNs, copyElapsedNs);
                columns++;
            }
        }
        generationSamples.sort(Long::compare);
        long generationP95Ns = percentileNs(generationSamples, 0.95);
        long heavyThresholdNs = Math.max(50_000_000L, generationP95Ns);
        int heavyColumns = 0;
        for (long sampleNs : generationSamples) {
            if (sampleNs >= heavyThresholdNs) {
                heavyColumns++;
            }
        }
        double totalMs = (generationNs + copyNs) / 1_000_000.0;
        System.out.printf(
            Locale.ROOT,
            "World profile: seed=%016x preset=%s radius=%d columns=%d height=%d..%d generationAvg=%.3fms generationP95=%.3fms generationMax=%.3fms heavyColumns=%d@%.1fms integrationCopyAvg=%.3fms integrationCopyMax=%.3fms total=%.3fms%n",
            profileSeed,
            profilePreset.metadataId(),
            radius,
            columns,
            GameConfig.WORLD_MIN_Y,
            GameConfig.WORLD_MAX_Y,
            generationNs / 1_000_000.0 / Math.max(1, columns),
            generationP95Ns / 1_000_000.0,
            generationMaxNs / 1_000_000.0,
            heavyColumns,
            heavyThresholdNs / 1_000_000.0,
            copyNs / 1_000_000.0 / Math.max(1, columns),
            copyMaxNs / 1_000_000.0,
            totalMs
        );
    }

    private static long percentileNs(ArrayList<Long> sortedSamples, double percentile) {
        if (sortedSamples.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile * sortedSamples.size()) - 1;
        index = Math.max(0, Math.min(sortedSamples.size() - 1, index));
        return sortedSamples.get(index);
    }

    private static long parseProfileSeed(String value) {
        try {
            return Long.parseUnsignedLong(value, 16);
        } catch (NumberFormatException exception) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return 0x5cfa085fdacb6033L;
            }
        }
    }

    private static int parseProfileInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void run() {
        try {
            Settings.load();
            renderDistanceChunks = Settings.savedRenderDistance;
            fovDegrees = Settings.savedFovDegrees;
            maxFps = Settings.savedMaxFps;
            world.setRenderDistanceChunks(renderDistanceChunks);
            initWindow();
            renderer.init();
            audio.init();
            refreshAvailableWorlds();
            mainMenuActive = true;
            mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
            mainMenuSelection = 0;
            optionsOpenedFromPause = false;
            syncPlayerModeState();
            syncSelectedHotbarItem();
            player.isGrounded = true;
            inputController.updateCursorMode();
            loop();
        } finally {
            cleanup();
        }
    }

    private void setGrassBlockWindowIcon() {
        final int size = 32;
        ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                int a = 0;
                boolean border = x < 3 || x >= size - 3 || y < 3 || y >= size - 3;
                if (!border) {
                    a = 255;
                    if (y < 11) {
                        r = x < 15 ? 78 : 61;
                        g = x < 15 ? 170 : 145;
                        b = 61;
                    } else if (y < 14) {
                        r = 39;
                        g = 112;
                        b = 46;
                    } else {
                        r = 122;
                        g = 78;
                        b = 43;
                        if ((x >= 8 && x <= 12 && y >= 17 && y <= 21)
                            || (x >= 20 && x <= 24 && y >= 20 && y <= 24)) {
                            r = 153;
                            g = 101;
                            b = 55;
                        } else if (x >= 13 && x <= 17 && y >= 25) {
                            r = 66;
                            g = 42;
                            b = 28;
                        }
                    }
                }
                pixels.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
        }
        pixels.flip();

        GLFWImage.Buffer icons = GLFWImage.malloc(1);
        try {
            icons.position(0).width(size).height(size).pixels(pixels);
            glfwSetWindowIcon(window, icons);
        } finally {
            icons.free();
        }
    }

    private void initWindow() {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        long primaryMonitor = glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = glfwGetVideoMode(primaryMonitor);
        int initialWidth = videoMode != null ? videoMode.width() : GameConfig.WINDOW_WIDTH;
        int initialHeight = videoMode != null ? videoMode.height() : GameConfig.WINDOW_HEIGHT;
        long initialMonitor = videoMode != null ? primaryMonitor : NULL;
        fullscreen = videoMode != null;

        window = glfwCreateWindow(initialWidth, initialHeight, "TinyCraft", initialMonitor, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Unable to create window");
        }
        setGrassBlockWindowIcon();

        if (videoMode != null) {
            windowedWidth = GameConfig.WINDOW_WIDTH;
            windowedHeight = GameConfig.WINDOW_HEIGHT;
            windowedX = (videoMode.width() - windowedWidth) / 2;
            windowedY = (videoMode.height() - windowedHeight) / 2;
        }

        glfwMakeContextCurrent(window);
        if (window == NULL) {
            throw new IllegalStateException("Window handle is invalid");
        }
        glfwSwapInterval(maxFps == Settings.FPS_VSYNC ? 1 : 0);
        glfwShowWindow(window);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (org.lwjgl.glfw.GLFW.glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }

        inputController.setCursorPositionHandler((xpos, ypos) -> {
            if (mainMenuActive) {
                int hoveredWorld = renderer.getMainMenuWorldAt(xpos, ypos, mainMenuScreen, availableWorlds.size(), mainMenuScrollOffset);
                if (hoveredWorld != -1) {
                    selectedWorldIndex = hoveredWorld;
                }
                int hoveredSlider = renderer.getOptionsSliderAt(xpos, ypos, mainMenuScreen);
                if (hoveredSlider != -1) {
                    mainMenuSelection = hoveredSlider;
                    inputController.setMouseInitialized(false);
                    return;
                }
                int hoveredOption = renderer.getMainMenuOptionAt(xpos, ypos, mainMenuScreen);
                if (hoveredOption != -1) {
                    mainMenuSelection = hoveredOption;
                }
                inputController.setMouseInitialized(false);
                return;
            }

            if (deathScreenActive) {
                int hoveredOption = renderer.getDeathOptionAt(xpos, ypos);
                if (hoveredOption != -1) {
                    deathSelection = hoveredOption;
                }
                inputController.setMouseInitialized(false);
                return;
            }

            if (paused) {
                int hoveredOption = renderer.getPauseOptionAt(xpos, ypos);
                if (hoveredOption != -1) {
                    pauseSelection = hoveredOption;
                }
                inputController.setMouseInitialized(false);
                return;
            }

            if (inventoryOpen) {
                inputController.setMouseInitialized(false);
                return;
            }

            if (chat.isActive()) {
                inputController.setMouseInitialized(false);
                return;
            }

            if (!inputController.isMouseInitialized()) {
                inputController.setLastMouseX(xpos);
                inputController.setLastMouseY(ypos);
                inputController.setMouseInitialized(true);
                return;
            }

            double deltaX = xpos - inputController.getLastMouseX();
            double deltaY = ypos - inputController.getLastMouseY();
            inputController.setLastMouseX(xpos);
            inputController.setLastMouseY(ypos);

            player.yaw = wrapAngle(player.yaw + deltaX * Settings.mouseSensitivity);
            player.pitch -= deltaY * Settings.mouseSensitivity * Settings.mouseVerticalFactor;
            player.pitch = clamp(player.pitch, -GameConfig.MAX_PITCH, GameConfig.MAX_PITCH);
        });

        inputController.setFramebufferSizeHandler((width, height) -> {
            if (width <= 0 || height <= 0) {
                return;
            }
            if (!fullscreen) {
                windowedWidth = width;
                windowedHeight = height;
            }
            inputController.setMouseInitialized(false);
        });

        inputController.setScrollHandler((xoffset, yoffset) -> {
            if (chat.isActive()) {
                chat.scroll((int) Math.signum(yoffset));
                return;
            }
            if (inventoryOpen && creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_PLAYER) {
                inputController.scrollCreativeInventory((int) Math.signum(yoffset));
                return;
            }
            if (!mainMenuActive || mainMenuScreen != GameConfig.MENU_SCREEN_SINGLEPLAYER || availableWorlds.isEmpty()) {
                return;
            }
            int maxOffset = Math.max(0, availableWorlds.size() - GameConfig.WORLD_MENU_VISIBLE_ROWS);
            mainMenuScrollOffset = Math.max(0, Math.min(maxOffset, mainMenuScrollOffset - (int) Math.signum(yoffset)));
            selectedWorldIndex = Math.max(0, Math.min(availableWorlds.size() - 1, selectedWorldIndex));
        });

        inputController.setCharacterHandler(codepoint -> {
            if (mainMenuActive && handleMainMenuCharacter(codepoint)) {
                return;
            }
            chat.appendCharacter(codepoint);
        });

        inputController.setKeyHandler((key, scancode, action, mods) -> {
            boolean pressed = action != GLFW_RELEASE;

            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
                return;
            }

            if (chat.isActive()) {
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    if (key == GLFW_KEY_C && (mods & GLFW_MOD_CONTROL) != 0) {
                        copyChatToClipboard();
                        resetMovement();
                        return;
                    }
                    if (key == GLFW_KEY_UP) {
                        chat.previousInput();
                    } else if (key == GLFW_KEY_DOWN) {
                        chat.nextInput();
                    } else if (key == GLFW_KEY_TAB) {
                        chat.cycleSuggestion((mods & GLFW_MOD_SHIFT) != 0);
                    } else if (key == GLFW_KEY_ENTER) {
                        chat.submit(new ChatSystem.CommandTarget() {
                            @Override
                            public void teleportPlayer(double x, double y, double z) {
                                teleportPlayerTo(x, y, z);
                            }

                            @Override
                            public void setWorldTime(double worldTime) {
                                world.setWorldTime(worldTime);
                            }

                            @Override
                            public void setGameMode(String mode) {
                                TinyCraft.this.setGameMode(commandGameModeIndex(mode));
                                saveLocalPlayerStateIfWorldLoaded();
                            }

                            @Override
                            public void clearInventory() {
                                inventory.clearAll();
                                updatePlayerArmorProtection();
                                syncSelectedHotbarItem();
                            }

                            @Override
                            public boolean giveItem(byte itemId, int amount) {
                                if (!InventoryItems.isCollectible(itemId) && !InventoryItems.isPlaceable(itemId)) {
                                    return false;
                                }
                                boolean added = inventory.addItem(itemId, amount);
                                syncSelectedHotbarItem();
                                return added;
                            }

                            @Override
                            public void killPlayer() {
                                enterDeathScreen();
                            }

                            @Override
                            public boolean summonMob(MobKind kind) {
                                if (kind == null) {
                                    return false;
                                }
                                world.spawnMobAt(kind, player.x, player.y, player.z);
                                return true;
                            }

                            @Override
                            public int setBlock(int x, int y, int z, BlockState state) {
                                int changed = world.setBlockStateForCommand(x, y, z, state);
                                if (changed > 0) {
                                    renderer.forceRebuildBlockEdit(x, y, z);
                                    broadcastCommandBlockUpdates();
                                }
                                return changed;
                            }

                            @Override
                            public int fillBlocks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
                                int changed = world.fillBlockStateForCommand(minX, minY, minZ, maxX, maxY, maxZ, state);
                                if (changed > 0) {
                                    renderer.buildAllChunkMeshes();
                                    broadcastCommandBlockUpdates();
                                }
                                return changed;
                            }

                            @Override
                            public String currentSeed() {
                                return Long.toUnsignedString(world.getSeed(), 16);
                            }

                            @Override
                            public String locateBiome(String biomeName) {
                                return TinyCraft.this.locateBiome(biomeName);
                            }

                            @Override
                            public String locateStructure(String structureName) {
                                return TinyCraft.this.locateStructureAndTeleport(structureName);
                            }

                            @Override
                            public String placeStructure(String structureName, int rotation) {
                                return TinyCraft.this.placeStructureNearPlayer(structureName, rotation);
                            }

                            @Override
                            public String listStructures() {
                                return world.structureTemplateList();
                            }

                            @Override
                            public String currentDebugLocation() {
                                int blockX = (int) Math.floor(player.x);
                                int blockY = (int) Math.floor(player.y);
                                int blockZ = (int) Math.floor(player.z);
                                int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_SIZE);
                                int chunkY = GameConfig.sectionIndexForY(blockY);
                                int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_SIZE);
                                int underY = (int) Math.floor(player.y - 0.12);
                                byte underBlock = world.getBlock(blockX, underY, blockZ);
                                BlockType underType = Blocks.typeFromLegacyId(underBlock);
                                int actualSurface = world.getActualSurfaceHeight(blockX, blockZ);
                                return String.format(
                                    Locale.ROOT,
                                    "seed=%s xyz=%.2f %.2f %.2f chunk=%d %d %d section=%d%nbiome=%s region=%s status=%s under=%s@%d%nactualSurface=%d %s%n%s%n%s",
                                    Long.toUnsignedString(world.getSeed(), 16),
                                    player.x,
                                    player.y,
                                    player.z,
                                    chunkX,
                                    chunkY,
                                    chunkZ,
                                    chunkY,
                                    world.getBiomeName(blockX, blockZ),
                                    world.getRegionFileName(chunkX, chunkZ),
                                    world.getChunkStatus(chunkX, chunkZ),
                                    underType.namespacedId,
                                    underY,
                                    actualSurface,
                                    world.getDensityDebugInfo(blockX, blockY, blockZ),
                                    world.getTerrainDebugInfo(blockX, blockZ),
                                    nearestStructureDebug(blockX, blockZ)
                                );
                            }

                            @Override
                            public String terrainDebugAt(int x, int z) {
                                int chunkX = Math.floorDiv(x, GameConfig.CHUNK_SIZE);
                                int chunkZ = Math.floorDiv(z, GameConfig.CHUNK_SIZE);
                                return String.format(
                                    Locale.ROOT,
                                    "seed=%s xz=%d %d chunk=%d %d %s",
                                    Long.toUnsignedString(world.getSeed(), 16),
                                    x,
                                    z,
                                    chunkX,
                                    chunkZ,
                                    world.getTerrainDebugInfo(x, z)
                                );
                            }

                            @Override
                            public String heightTest() {
                                boolean pass = GameConfig.isWorldYInside(-64)
                                    && GameConfig.isWorldYInside(319)
                                    && !GameConfig.isWorldYInside(-65)
                                    && !GameConfig.isWorldYInside(320)
                                    && GameConfig.sectionIndexForY(-64) == 0
                                    && GameConfig.sectionIndexForY(-49) == 0
                                    && GameConfig.sectionIndexForY(-48) == 1
                                    && GameConfig.sectionIndexForY(0) == 4
                                    && GameConfig.sectionIndexForY(319) == 23;
                                return pass
                                    ? "Height test PASS (-64..319, sections OK)"
                                    : "Height test FAIL";
                            }

                            @Override
                            public String blockInfo() {
                                RayHit hit = hoveredBlock != null ? hoveredBlock : world.raycastBlock(player);
                                int blockX;
                                int blockY;
                                int blockZ;
                                if (hit != null) {
                                    blockX = hit.x;
                                    blockY = hit.y;
                                    blockZ = hit.z;
                                } else {
                                    blockX = (int) Math.floor(player.x);
                                    blockY = (int) Math.floor(player.y - 0.05);
                                    blockZ = (int) Math.floor(player.z);
                                }
                                byte legacyId = world.getBlock(blockX, blockY, blockZ);
                                BlockType type = Blocks.typeFromLegacyId(legacyId);
                                BlockState state = world.getBlockState(blockX, blockY, blockZ);
                                String stateInfo = "";
                                if (legacyId == GameConfig.OAK_DOOR) {
                                    stateInfo = " doorOpen=" + Blocks.isDoorOpen(state)
                                        + " upper=" + Blocks.isDoorUpper(state)
                                        + " facing=" + Blocks.doorFacing(state);
                                } else if (legacyId == GameConfig.STRUCTURE_BLOCK) {
                                    int templateIndex = state.data & 0xFF;
                                    int rotation = (state.data >>> 8) & 3;
                                    stateInfo = " template=" + StructureTemplates.nameAt(templateIndex)
                                        + " rotation=" + rotation
                                        + " (right-click cycle, shift-right rotate, top-click place)";
                                }
                                return String.format(
                                    Locale.ROOT,
                                    "block %d %d %d legacy=%d id=%s name=%s solid=%s opaque=%s liquid=%s replaceable=%s plant=%s gravity=%s%s",
                                    blockX,
                                    blockY,
                                    blockZ,
                                    legacyId & 0xFF,
                                    type.namespacedId,
                                    type.displayName,
                                    type.solid,
                                    type.opaque,
                                    type.liquid,
                                    type.replaceable,
                                    type.plant,
                                    type.gravityAffected,
                                    stateInfo
                                );
                            }

                            @Override
                            public void sendChat(String message) {
                                TinyCraft.this.sendChatMessage(message);
                            }

                            @Override
                            public boolean canUseCheatCommands() {
                                return TinyCraft.this.canAttemptCheatCommands();
                            }

                            @Override
                            public boolean shouldHandleCommandRemotely(String command) {
                                return multiplayer.isClient() && command != null && command.trim().startsWith("/");
                            }
                        });
                    } else if (key == GLFW_KEY_BACKSPACE) {
                        chat.backspace();
                    } else if (key == GLFW_KEY_ESCAPE) {
                        chat.close();
                    }
                }
                resetChatInputState();
                return;
            }

            if (mainMenuActive) {
                if (action == GLFW_PRESS) {
                    if (handleMainMenuTextKey(key)) {
                        return;
                    }
                    handleMainMenuKey(key);
                }
                return;
            }

            if (deathScreenActive) {
                if (action == GLFW_PRESS) {
                    if (key == GLFW_KEY_W || key == GLFW_KEY_UP) {
                        deathSelection = (deathSelection + GameConfig.DEATH_OPTIONS.length - 1) % GameConfig.DEATH_OPTIONS.length;
                    } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN) {
                        deathSelection = (deathSelection + 1) % GameConfig.DEATH_OPTIONS.length;
                    } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                        activateDeathOption();
                    }
                }
                return;
            }

            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                if (inventoryOpen) {
                    toggleInventory();
                    return;
                }
                togglePause();
                return;
            }

            if (key == GLFW_KEY_E && action == GLFW_PRESS && !paused && !spectatorMode) {
                toggleInventory();
                return;
            }

            if (paused) {
                if (action == GLFW_PRESS) {
                    if (key == GLFW_KEY_W || key == GLFW_KEY_UP) {
                        pauseSelection = (pauseSelection + GameConfig.PAUSE_OPTIONS.length - 1) % GameConfig.PAUSE_OPTIONS.length;
                    } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN) {
                        pauseSelection = (pauseSelection + 1) % GameConfig.PAUSE_OPTIONS.length;
                    } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                        activatePauseOption();
                    }
                }
                return;
            }

            if (key == GLFW_KEY_T && action == GLFW_PRESS && !inventoryOpen) {
                chat.setCheatSuggestionsEnabled(canAttemptCheatCommands());
                chat.open();
                resetChatInputState();
                return;
            }

            if (key == GLFW_KEY_TAB) {
                inputController.setTabPlayerListHeld(
                    pressed && !inventoryOpen && !paused && !mainMenuActive && !deathScreenActive && multiplayer.isMultiplayerActive()
                );
                resetMovement();
                return;
            }

            switch (key) {
                case GLFW_KEY_F1:
                    if (action == GLFW_PRESS) {
                        hideHud = !hideHud;
                    }
                    break;
                case GLFW_KEY_F2:
                    if (action == GLFW_PRESS) {
                        screenshotCaptureRequested = true;
                    }
                    break;
                case GLFW_KEY_F3:
                    if (action == GLFW_PRESS) {
                        inputController.setF3Held(true);
                        inputController.setF3ComboConsumed(false);
                    } else if (action == GLFW_RELEASE) {
                        if (inputController.isGameModeSwitcherActive()) {
                            applySelectedGameMode();
                        } else if (!inputController.isF3ComboConsumed()) {
                            showDebugInfo = !showDebugInfo;
                        }
                        inputController.setF3Held(false);
                        inputController.setF3ComboConsumed(false);
                    }
                    break;
                case GLFW_KEY_F4:
                    if (action == GLFW_PRESS && inputController.isF3Held()) {
                        inputController.setF3ComboConsumed(true);
                        if (canAttemptCheatCommands()) {
                            cycleGameModeSelection();
                        } else {
                            inputController.setGameModeSwitcherActive(false);
                            chat.addMessage("Cheats are not enabled in this world.");
                        }
                    }
                    break;
                case GLFW_KEY_G:
                    if (action == GLFW_PRESS && inputController.isF3Held()) {
                        showChunkBorders = !showChunkBorders;
                        inputController.setF3ComboConsumed(true);
                    }
                    break;
                case GLFW_KEY_F5:
                    if (action == GLFW_PRESS) {
                        if (!thirdPersonView) {
                            thirdPersonView = true;
                            frontThirdPersonView = false;
                        } else if (!frontThirdPersonView) {
                            frontThirdPersonView = true;
                        } else {
                            thirdPersonView = false;
                            frontThirdPersonView = false;
                        }
                    }
                    break;
                case GLFW_KEY_F6:
                    if (action == GLFW_PRESS) {
                        panoramaCaptureRequested = true;
                    }
                    break;
                case GLFW_KEY_1:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(0);
                    }
                    break;
                case GLFW_KEY_2:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(1);
                    }
                    break;
                case GLFW_KEY_3:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(2);
                    }
                    break;
                case GLFW_KEY_4:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(3);
                    }
                    break;
                case GLFW_KEY_5:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(4);
                    }
                    break;
                case GLFW_KEY_6:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(5);
                    }
                    break;
                case GLFW_KEY_7:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(6);
                    }
                    break;
                case GLFW_KEY_8:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(7);
                    }
                    break;
                case GLFW_KEY_9:
                    if (action == GLFW_PRESS) {
                        selectHotbarSlot(8);
                    }
                    break;
                case GLFW_KEY_Q:
                    if (action == GLFW_PRESS && !inventoryOpen && !spectatorMode) {
                        dropSelectedHotbarItem();
                    }
                    break;
                default:
                    break;
            }

            if (inventoryOpen) {
                return;
            }

            switch (key) {
                case GLFW_KEY_W:
                    playerController.setForward(pressed);
                    break;
                case GLFW_KEY_S:
                    playerController.setBackward(pressed);
                    break;
                case GLFW_KEY_A:
                    playerController.setLeft(pressed);
                    break;
                case GLFW_KEY_D:
                    playerController.setRight(pressed);
                    break;
                case GLFW_KEY_LEFT_CONTROL:
                    playerController.setSprint(pressed);
                    break;
                case GLFW_KEY_LEFT_SHIFT:
                    playerController.setSneak(pressed);
                    playerController.setDescend(pressed);
                    break;
                case GLFW_KEY_SPACE:
                    playerController.setJumpHeld(pressed);
                    if (action == GLFW_PRESS) {
                        handleJumpPress();
                    }
                    if (!spectatorMode && action == GLFW_PRESS && (!creativeMode || !playerController.isCreativeFlightEnabled())) {
                        playerController.setJumpQueued(true);
                    }
                    break;
                default:
                    break;
            }
        });

        inputController.setMouseButtonHandler((button, action, mods) -> {
            double mouseX = inputController.getMouseX();
            double mouseY = inputController.getMouseY();
            if (mainMenuActive) {
                if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                    int hoveredWorld = renderer.getMainMenuWorldAt(mouseX, mouseY, mainMenuScreen, availableWorlds.size(), mainMenuScrollOffset);
                    if (hoveredWorld != -1) {
                        double clickTime = glfwGetTime();
                        boolean doubleClick = hoveredWorld == lastWorldSlotClickIndex
                            && lastWorldSlotClickTime >= 0.0
                            && clickTime - lastWorldSlotClickTime <= 0.35;
                        selectedWorldIndex = hoveredWorld;
                        mainMenuWorldActionsEnabled = true;
                        lastWorldSlotClickIndex = hoveredWorld;
                        lastWorldSlotClickTime = clickTime;
                        if (doubleClick) {
                            mainMenuSelection = 0;
                            activateMainMenuOption();
                            return;
                        }
                    }
                    int hoveredOption = renderer.getMainMenuOptionAt(mouseX, mouseY, mainMenuScreen);
                    if (hoveredOption != -1) {
                        if (mainMenuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER && !isSingleplayerActionEnabled(hoveredOption)) {
                            return;
                        }
                        mainMenuSelection = hoveredOption;
                        activateMainMenuOption();
                        return;
                    }
                    int hoveredSlider = renderer.getOptionsSliderAt(mouseX, mouseY, mainMenuScreen);
                    if (hoveredSlider != -1) {
                        mainMenuSelection = hoveredSlider;
                        applyOptionsSlider(hoveredSlider, renderer.sliderPercentAt(mouseX, hoveredSlider));
                        return;
                    }
                    if (mainMenuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD) {
                        int field = renderer.getCreateWorldFieldAt(mouseX, mouseY);
                        if (field != -1) {
                            inputController.setActiveMenuTextField(field);
                            return;
                        }
                        int toggle = renderer.getCreateWorldToggleAt(mouseX, mouseY);
                        if (toggle == 0) {
                            createWorldGameMode = (createWorldGameMode + 1) % GameConfig.GAME_MODE_OPTIONS.length;
                            createWorldAllowCheats = WorldAccessRules.defaultAllowCheats(createWorldGameMode);
                            return;
                        }
                        if (toggle == 1) {
                            createWorldDifficulty = (createWorldDifficulty + 1) % GameConfig.DIFFICULTY_OPTIONS.length;
                            return;
                        }
                        if (toggle == 2) {
                            createWorldTerrainPreset = (createWorldTerrainPreset + 1) % GameConfig.TERRAIN_PRESET_OPTIONS.length;
                            return;
                        }
                        if (toggle == 3) {
                            createWorldAllowCheats = !createWorldAllowCheats;
                            return;
                        }
                    } else if (mainMenuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
                        int field = renderer.getMultiplayerFieldAt(mouseX, mouseY);
                        if (field != -1) {
                            inputController.setActiveMenuTextField(field);
                            return;
                        }
                    } else if (mainMenuScreen == GameConfig.MENU_SCREEN_OPEN_LAN) {
                        int toggle = renderer.getLanToggleAt(mouseX, mouseY);
                        if (toggle == 0) {
                            lanGameMode = (lanGameMode + 1) % GameConfig.GAME_MODE_OPTIONS.length;
                            return;
                        }
                        if (toggle == 1) {
                            lanAllowCheats = !lanAllowCheats;
                            return;
                        }
                    } else if (mainMenuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
                        int field = renderer.getRenameWorldFieldAt(mouseX, mouseY);
                        if (field != -1) {
                            inputController.setActiveMenuTextField(field);
                            return;
                        }
                    }
                }
                return;
            }

            if (deathScreenActive) {
                if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                    int hoveredOption = renderer.getDeathOptionAt(mouseX, mouseY);
                    if (hoveredOption != -1) {
                        deathSelection = hoveredOption;
                        activateDeathOption();
                    }
                }
                return;
            }

            if (paused) {
                if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                    int hoveredOption = renderer.getPauseOptionAt(mouseX, mouseY);
                    if (hoveredOption != -1) {
                        pauseSelection = hoveredOption;
                        activatePauseOption();
                    }
                }
                return;
            }

            if (chat.isActive()) {
                inputController.setLeftMouseHeld(false);
                resetBreakingProgress();
                return;
            }

            if (inventoryOpen) {
                handleInventoryMouseClick(button, action, mods);
                return;
            }

            if (spectatorMode) {
                inputController.setLeftMouseHeld(false);
                resetBreakingProgress();
                return;
            }

            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    player.handSwingTimer = Math.max(player.handSwingTimer, 0.22);
                    hoveredBlock = world.raycastBlock(player);
                    if (multiplayer.isClient()) {
                        if (hoveredBlock == null || !canBreak(hoveredBlock)) {
                            multiplayer.sendMobAttack(attackDamageForHeldItem(), knockbackForHeldItem());
                            inputController.setLeftMouseHeld(false);
                            resetBreakingProgress();
                            return;
                        }
                    }
                }
                if (action == GLFW_PRESS && world.attackMobInReach(player, attackDamageForHeldItem(), knockbackForHeldItem())) {
                    player.handSwingTimer = 0.22;
                    playerController.spendHunger(0.12);
                    if (!creativeMode) {
                        inventory.damageSelectedItem(selectedSlot, 1);
                        syncSelectedHotbarItem();
                    }
                    inputController.setLeftMouseHeld(false);
                    resetBreakingProgress();
                    return;
                }
                if (action == GLFW_PRESS && multiplayer.isMultiplayerActive()) {
                    java.util.UUID targetUuid = world.findRemotePlayerInReach(player);
                    if (targetUuid != null) {
                        multiplayer.sendPlayerAttack(targetUuid, attackDamageForHeldItem());
                        player.handSwingTimer = 0.22;
                        playerController.spendHunger(0.12);
                        if (!creativeMode) {
                            inventory.damageSelectedItem(selectedSlot, 1);
                            syncSelectedHotbarItem();
                        }
                        inputController.setLeftMouseHeld(false);
                        resetBreakingProgress();
                        return;
                    }
                }
                if (creativeMode) {
                    if (action == GLFW_PRESS) {
                        breakHoveredInstantly();
                    }
                    inputController.setLeftMouseHeld(false);
                    return;
                }
                inputController.setLeftMouseHeld(action != GLFW_RELEASE);
                if (action == GLFW_PRESS) {
                    inputController.setLeftMousePressQueued(true);
                }
                if (!inputController.isLeftMouseHeld()) {
                    if (!inputController.isLeftMousePressQueued()) {
                        resetBreakingProgress();
                    }
                }
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
                RayHit hit = world.raycastBlock(player);
                boolean shiftDown = (mods & GLFW_MOD_SHIFT) != 0;
                if (hit != null && !shiftDown) {
                    byte targetBlock = world.getBlock(hit.x, hit.y, hit.z);
                    if (targetBlock == GameConfig.CRAFTING_TABLE) {
                        openContainerScreen(GameConfig.INVENTORY_SCREEN_WORKBENCH, hit.x, hit.y, hit.z);
                        return;
                    }
                    if (targetBlock == GameConfig.CHEST) {
                        openContainerScreen(GameConfig.INVENTORY_SCREEN_CHEST, hit.x, hit.y, hit.z);
                        return;
                    }
                    if (targetBlock == GameConfig.FURNACE) {
                        openContainerScreen(GameConfig.INVENTORY_SCREEN_FURNACE, hit.x, hit.y, hit.z);
                        return;
                    }
                }
                byte heldItem = inventory.getSelectedItemId(selectedSlot);
                if (multiplayer.isClient()) {
                    if (InventoryItems.isPlaceable(heldItem)) {
                        multiplayer.sendBlockPlace(hit, heldItem, selectedSlot);
                        player.handSwingTimer = Math.max(player.handSwingTimer, 0.16);
                    }
                    return;
                }
                if (world.interactBlock(hit, player, shiftDown)) {
                    renderer.forceRebuildBlockEdit(hit.x, hit.y, hit.z);
                    return;
                }
                MobKind spawnKind = InventoryItems.mobKindForSpawnEgg(heldItem);
                if (spawnKind != null) {
                    useSpawnEgg(spawnKind, hit);
                    return;
                }
                if (world.feedPassiveMobInReach(player, heldItem)) {
                    if (!creativeMode) {
                        inventory.consumeSelectedItem(selectedSlot);
                        syncSelectedHotbarItem();
                    }
                    return;
                }
                if (useHeldFood(heldItem)) {
                    return;
                }
                if (useHeldBucket(heldItem, hit)) {
                    return;
                }
                if (!InventoryItems.isPlaceable(heldItem)) {
                    return;
                }
                if (world.placeBlock(hit, heldItem, player)) {
                    if (multiplayer.isHosting()) {
                        multiplayer.broadcastBlockNeighborhood(world, hit.previousX, hit.previousY, hit.previousZ);
                    }
                    if (!creativeMode) {
                        inventory.consumeSelectedItem(selectedSlot);
                        syncSelectedHotbarItem();
                    }
                    renderer.forceRebuildBlockEdit(hit.previousX, hit.previousY, hit.previousZ);
                }
            }
        });
        inputController.setupCallbacks(window);
    }

    private void loop() {
        double lastTime = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            double deltaTime = Math.min(0.05, now - lastTime);
            lastTime = now;
            long updateStartNs = System.nanoTime();

            glfwPollEvents();
            multiplayer.drainEvents();
            renderer.updateFramebufferSize(window);
            capturePanoramaIfRequested();

            boolean simulateWorld = shouldRunWorldSimulation();
            long simulationStartNs = System.nanoTime();
            long simulationNs = 0L;
            if (simulateWorld) {
                simulationAccumulator = Math.min(simulationAccumulator + deltaTime, GameConfig.GAME_TICK_SECONDS * 4.0);
                int ticksThisFrame = 0;
                while (simulationAccumulator >= GameConfig.GAME_TICK_SECONDS && ticksThisFrame < 3) {
                    runWorldSimulationTick(GameConfig.GAME_TICK_SECONDS);
                    simulationAccumulator -= GameConfig.GAME_TICK_SECONDS;
                    ticksThisFrame++;
                }
                if (ticksThisFrame == 3 && simulationAccumulator >= GameConfig.GAME_TICK_SECONDS) {
                    simulationAccumulator = 0.0;
                }
            }
            player.handSwingTimer = Math.max(0.0, player.handSwingTimer - deltaTime);
            if (!simulateWorld) {
                simulationAccumulator = 0.0;
                hoveredBlock = null;
                resetBreakingProgress();
            }
            simulationNs = System.nanoTime() - simulationStartNs;
            PlayerState framePlayer = interpolatedPlayerForRender(simulateWorld);

            long renderStartNs = System.nanoTime();
            audio.updateListener(player);
            boolean sprinting = shouldRunWorldSimulation()
                && playerController.isSprint()
                && player.hunger > 6
                && (playerController.isForward() || playerController.isBackward() || playerController.isLeft() || playerController.isRight());
            renderer.setChunkBordersEnabled(showChunkBorders);
            renderer.render(
                framePlayer,
                inventory,
                hoveredBlock,
                currentBreakingHit(),
                getBreakingProgress(),
                paused,
                inventoryOpen,
                inventoryScreenMode,
                activeChestContainer,
                activeFurnace,
                deathScreenActive,
                deathSelection,
                mainMenuActive,
                mainMenuScreen,
                optionsOpenedFromPause || lanOpenedFromPause,
                mainMenuSelection,
                mainMenuWorldActionsEnabled,
                createWorldName,
                createWorldSeed,
                createWorldGameMode,
                createWorldAllowCheats,
                createWorldDifficulty,
                createWorldTerrainPreset,
                inputController.getActiveMenuTextField(),
                renameWorldName,
                multiplayerName,
                multiplayerHost,
                multiplayerPort,
                multiplayerStatus,
                lanGameMode,
                lanAllowCheats,
                availableWorlds,
                selectedWorldIndex,
                mainMenuScrollOffset,
                loadedWorldName,
                showDebugInfo,
                hideHud,
                pauseSelection,
                inputController.isGameModeSwitcherActive(),
                inputController.getGameModeSelection(),
                selectedBlock,
                selectedSlot,
                inputController.getCreativeTab(),
                inputController.getCreativeScrollOffset(),
                creativeMode,
                thirdPersonView,
                frontThirdPersonView,
                sprinting,
                renderDistanceChunks,
                fovDegrees,
                world.getWorldTime(),
                inputController.getMouseX(),
                inputController.getMouseY(),
                deltaTime,
                currentPartialTicks(simulateWorld),
                chat,
                inputController.isTabPlayerListHeld() && multiplayer.isMultiplayerActive(),
                multiplayer.playerListSnapshot()
            );
            captureScreenshotIfRequested();
            long frameEndNs = System.nanoTime();
            glfwSwapBuffers(window);
            frameProfiler.recordFrame(
                deltaTime,
                simulationStartNs - updateStartNs,
                simulationNs,
                frameEndNs - renderStartNs,
                0L
            );
            limitFrameRate(now);
        }
    }

    private void capturePanoramaIfRequested() {
        if (!panoramaCaptureRequested) {
            return;
        }
        panoramaCaptureRequested = false;
        if (!worldLoaded || multiplayerWorldLoading) {
            chat.addMessage(Settings.isRussian()
                ? "Панораму можно снять после полной загрузки мира."
                : "Panorama capture is available after the world finishes loading.");
            return;
        }
        try {
            Path directory = renderer.captureMenuPanorama(player, renderDistanceChunks, world.getWorldTime());
            addCaptureSavedMessage(true, directory);
        } catch (IOException | RuntimeException exception) {
            System.err.println("Failed to capture menu panorama: " + exception);
            chat.addMessage(Settings.isRussian()
                ? "Не удалось сохранить панораму. Подробности в консоли."
                : "Failed to save panorama. See the console for details.");
        }
    }

    private void captureScreenshotIfRequested() {
        if (!screenshotCaptureRequested) {
            return;
        }
        screenshotCaptureRequested = false;
        try {
            Path output = renderer.captureScreenshot();
            addCaptureSavedMessage(false, output);
        } catch (IOException | RuntimeException exception) {
            System.err.println("Failed to capture screenshot: " + exception);
            chat.addMessage(Settings.isRussian()
                ? "Не удалось сохранить скриншот. Подробности в консоли."
                : "Failed to save screenshot. See the console for details.");
        }
    }

    private void addCaptureSavedMessage(boolean panorama, Path output) {
        String displayPath;
        try {
            displayPath = RuntimePaths.PROJECT_ROOT.relativize(output).toString();
        } catch (IllegalArgumentException exception) {
            displayPath = output.toString();
        }
        if (Settings.isRussian()) {
            chat.addMessage((panorama ? "Панорама сохранена: " : "Скриншот сохранён: ") + displayPath);
        } else {
            chat.addMessage((panorama ? "Panorama saved: " : "Screenshot saved: ") + displayPath);
        }
    }

    private void limitFrameRate(double frameStartSeconds) {
        if (maxFps <= 0) {
            return;
        }
        double targetFrameSeconds = 1.0 / maxFps;
        while (true) {
            double remaining = targetFrameSeconds - (glfwGetTime() - frameStartSeconds);
            if (remaining <= 0.0005) {
                break;
            }
            long sleepMillis = Math.max(0L, (long) ((remaining - 0.0003) * 1000.0));
            if (sleepMillis > 0L) {
                sleepQuietly(sleepMillis);
            } else {
                Thread.yield();
            }
        }
    }

    private void runWorldSimulationTick(double tickDelta) {
        if (multiplayerWorldLoading && multiplayer.isClient()) {
            capturePreviousPlayerForInterpolation();
            world.setRenderDistanceChunks(renderDistanceChunks);
            multiplayer.tickClient(world, player, inventory.getSelectedItemId(selectedSlot), selectedSlot, renderDistanceChunks, tickDelta);
            if (System.currentTimeMillis() - multiplayerWorldLoadingStartedMillis > 15000L) {
                multiplayerWorldLoading = false;
                chat.addMessage("[MP] Chunk loading timeout; continuing with streaming.");
                resetRenderInterpolation();
            }
            hoveredBlock = null;
            resetBreakingProgress();
            return;
        }
        capturePreviousPlayerForInterpolation();
        capturePreviousEntityPositions();
        world.setRenderDistanceChunks(renderDistanceChunks);
        if (multiplayer.isClient()) {
            updatePlayerArmorProtection();
            playerController.update(tickDelta);
            multiplayer.tickClient(world, player, inventory.getSelectedItemId(selectedSlot), selectedSlot, renderDistanceChunks, tickDelta);
            if (shouldEnterSurvivalDeathScreen(deathScreenActive, creativeMode, spectatorMode, player.health)) {
                enterDeathScreen();
            }
            return;
        }
        world.advanceWorldTime(tickDelta);
        world.prepareForPlayer(player);
        updatePlayerArmorProtection();
        playerController.update(tickDelta);
        if (world.updateParadisePortalTravel(player, tickDelta)) {
            showLoadingScreen(paradiseTravelText(), world.isParadiseArea(player.x, player.z) ? "Paradise" : loadedWorldName);
            renderer.clearWorldMeshes();
            world.prepareForPlayer(player);
            world.primeStreamingAround(player);
            renderer.buildAllChunkMeshes();
            resetRenderInterpolation();
            resetMovement();
            resetBreakingProgress();
        }
        world.updateDroppedItems(player, inventory, tickDelta);
        world.updateMobs(player, tickDelta);
        if (multiplayer.isHosting()) {
            multiplayer.tickHost(world, player, inventory.getSelectedItemId(selectedSlot), tickDelta);
        } else if (multiplayer.isClient()) {
            multiplayer.tickClient(world, player, inventory.getSelectedItemId(selectedSlot), selectedSlot, renderDistanceChunks, tickDelta);
        }
        if (shouldEnterSurvivalDeathScreen(deathScreenActive, creativeMode, spectatorMode, player.health)) {
            enterDeathScreen();
        }
        for (MobEntity mob : world.getMobs()) {
            if (mob.growlQueued) {
                audio.playMobAmbient(mob.kind, mob.x, mob.y + 1.4, mob.z);
                mob.growlQueued = false;
            }
            if (mob.splashQueued) {
                audio.playSplash(mob.x, mob.y + 0.3, mob.z);
                mob.splashQueued = false;
            }
        }
        ColumnUpdateList dirtyColumns = world.updateWorldTicks(player, tickDelta);
        for (int i = 0; i < dirtyColumns.size(); i++) {
            renderer.rebuildChunkSectionAroundBlockThrottled(dirtyColumns.xAt(i), dirtyColumns.yAt(i), dirtyColumns.zAt(i));
        }
        if (multiplayer.isHosting()) {
            multiplayer.broadcastBlockUpdates(world, world.drainNetworkDirtyBlocks());
        } else {
            world.clearNetworkDirtyBlocks();
        }
        updateWaterAmbientAudio(tickDelta);
        if (!inventoryOpen && !chat.isActive() && !deathScreenActive && !mainMenuActive && !spectatorMode) {
            hoveredBlock = world.raycastBlock(player);
            updateBlockInteraction(tickDelta);
        } else {
            hoveredBlock = null;
            resetBreakingProgress();
        }
    }

    private void capturePreviousEntityPositions() {
        for (MobEntity mob : world.getMobs()) {
            mob.capturePreviousPosition();
        }
        for (DroppedItem droppedItem : world.getDroppedItems()) {
            droppedItem.capturePreviousPosition();
        }
        for (FallingBlock fallingBlock : world.getFallingBlocks()) {
            fallingBlock.capturePreviousPosition();
        }
    }

    private void capturePreviousPlayerForInterpolation() {
        previousPlayerX = player.x;
        previousPlayerY = player.y;
        previousPlayerZ = player.z;
        interpolationInitialized = true;
    }

    private PlayerState interpolatedPlayerForRender(boolean simulateWorld) {
        if (!interpolationInitialized || !simulateWorld) {
            resetRenderInterpolation();
        }
        copyPlayerState(player, renderPlayer);
        double partialTicks = currentPartialTicks(simulateWorld);
        renderPlayer.x = lerp(previousPlayerX, player.x, partialTicks);
        renderPlayer.y = lerp(previousPlayerY, player.y, partialTicks);
        renderPlayer.z = lerp(previousPlayerZ, player.z, partialTicks);
        return renderPlayer;
    }

    private double currentPartialTicks(boolean simulateWorld) {
        return simulateWorld ? clamp(simulationAccumulator / GameConfig.GAME_TICK_SECONDS, 0.0, 1.0) : 1.0;
    }

    private void resetRenderInterpolation() {
        previousPlayerX = player.x;
        previousPlayerY = player.y;
        previousPlayerZ = player.z;
        interpolationInitialized = true;
    }

    private void copyPlayerState(PlayerState source, PlayerState target) {
        target.x = source.x;
        target.y = source.y;
        target.z = source.z;
        target.velocityX = source.velocityX;
        target.velocityZ = source.velocityZ;
        target.verticalVelocity = source.verticalVelocity;
        target.fallDistance = source.fallDistance;
        target.isGrounded = source.isGrounded;
        target.stepTimer = source.stepTimer;
        target.health = source.health;
        target.airUnits = source.airUnits;
        target.airUnitTimer = source.airUnitTimer;
        target.drowningTimer = source.drowningTimer;
        target.lavaDamageTimer = source.lavaDamageTimer;
        target.fireTimer = source.fireTimer;
        target.fireDamageTimer = source.fireDamageTimer;
        target.wasInWater = source.wasInWater;
        target.yaw = source.yaw;
        target.pitch = source.pitch;
        target.cameraBobPhase = source.cameraBobPhase;
        target.cameraBobAmount = source.cameraBobAmount;
        target.handSwingTimer = source.handSwingTimer;
        target.suffocationTimer = source.suffocationTimer;
        target.hungerDrainTimer = source.hungerDrainTimer;
        target.hungerDamageTimer = source.hungerDamageTimer;
        target.hungerRegenTimer = source.hungerRegenTimer;
        target.hungerGraceRemaining = source.hungerGraceRemaining;
        target.hunger = source.hunger;
        target.armorProtection = source.armorProtection;
        target.headInWater = source.headInWater;
        target.wasInLiquid = source.wasInLiquid;
        target.creativeMode = source.creativeMode;
        target.spectatorMode = source.spectatorMode;
        target.flightEnabled = source.flightEnabled;
        target.sneaking = source.sneaking;
        target.hasCustomSpawn = source.hasCustomSpawn;
        target.spawnX = source.spawnX;
        target.spawnY = source.spawnY;
        target.spawnZ = source.spawnZ;
        target.hasParadiseReturn = source.hasParadiseReturn;
        target.paradiseReturnX = source.paradiseReturnX;
        target.paradiseReturnY = source.paradiseReturnY;
        target.paradiseReturnZ = source.paradiseReturnZ;
        target.paradiseReturnDimensionId = source.paradiseReturnDimensionId;
        target.paradisePortalTimer = source.paradisePortalTimer;
        target.paradisePortalCooldown = source.paradisePortalCooldown;
        target.dimensionId = source.dimensionId;
    }

    private void copyChatToClipboard() {
        String text = chat.copyText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        glfwSetClipboardString(window, text);
    }

    private void teleportPlayerTo(double x, double y, double z) {
        player.x = x;
        player.y = clamp(y, GameConfig.WORLD_MIN_Y + 1.0, GameConfig.WORLD_MAX_Y - 1.0);
        player.z = z;
        player.verticalVelocity = 0.0;
        player.isGrounded = false;
        playerController.resetPlayerFluidState();
        resetBreakingProgress();
        world.prepareForPlayer(player);
        resetRenderInterpolation();
        renderer.rebuildChunksAroundBlock((int) Math.floor(player.x), (int) Math.floor(player.z));
    }

    private void broadcastCommandBlockUpdates() {
        if (multiplayer.isHosting()) {
            multiplayer.broadcastBlockUpdates(world, world.drainNetworkDirtyBlocks());
        }
    }

    private String locateBiome(String biomeName) {
        String query = normalizeBiomeName(biomeName);
        if (query.isEmpty()) {
            return "Usage: /locatebiome <name>";
        }

        int startX = (int) Math.floor(player.x);
        int startZ = (int) Math.floor(player.z);
        int step = GameConfig.CHUNK_SIZE;
        int maxRadius = Math.max(512, renderDistanceChunks * GameConfig.CHUNK_SIZE * 10);
        for (int radius = 0; radius <= maxRadius; radius += step) {
            for (int offsetX = -radius; offsetX <= radius; offsetX += step) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ += step) {
                    if (radius != 0 && Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) {
                        continue;
                    }
                    int sampleX = startX + offsetX;
                    int sampleZ = startZ + offsetZ;
                    String candidate = world.getBiomeName(sampleX, sampleZ);
                    if (!matchesBiomeQuery(candidate, query)) {
                        continue;
                    }
                    int surfaceY = world.getGeneratedSurfaceHeight(sampleX, sampleZ);
                    return "Nearest " + candidate + ": " + sampleX + " " + surfaceY + " " + sampleZ + ".";
                }
            }
        }
        return "Could not find biome '" + biomeName + "' within " + maxRadius + " blocks.";
    }

    private String locateStructureAndTeleport(String structureName) {
        String query = normalizeBiomeName(structureName);
        if (query.isEmpty()) {
            return "Usage: /locate structure <village|mineshaft>";
        }
        if (query.contains("village") || query.contains("деревн")) {
            return locateVillageAndTeleport();
        }
        if (query.contains("mineshaft") || query.contains("shaft") || query.contains("шахт")) {
            return locateMineshaftAndTeleport();
        }
        return "Unknown structure '" + structureName + "'. Try village or mineshaft.";
    }

    private String placeStructureNearPlayer(String structureName, int rotation) {
        if ("list".equalsIgnoreCase(structureName)) {
            return "Structures: " + world.structureTemplateList();
        }
        double horizontalLength = Math.cos(player.pitch);
        int forwardX = (int) Math.round(Math.cos(player.yaw) * horizontalLength * 6.0);
        int forwardZ = (int) Math.round(Math.sin(player.yaw) * horizontalLength * 6.0);
        if (forwardX == 0 && forwardZ == 0) {
            forwardX = 4;
        }
        int originX = (int) Math.floor(player.x) + forwardX;
        int originZ = (int) Math.floor(player.z) + forwardZ;
        int originY = world.getSurfaceHeight(originX, originZ) + 1;
        String result = world.placeStructureTemplate(structureName, originX, originY, originZ, rotation);
        renderer.rebuildChunksAroundBlock(originX, originZ);
        return result;
    }

    private String nearestStructureDebug(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_SIZE);
        String village = nearestVillageDebug(chunkX, chunkZ, blockX, blockZ);
        String mineshaft = nearestMineshaftDebug(chunkX, chunkZ, blockX, blockZ);
        return "nearestStructure " + village + " " + mineshaft;
    }

    private String nearestVillageDebug(int chunkX, int chunkZ, int blockX, int blockZ) {
        int villageCellSize = world.getVillageCellSizeChunks();
        int cellX = Math.floorDiv(chunkX, villageCellSize);
        int cellZ = Math.floorDiv(chunkZ, villageCellSize);
        long bestDistance = Long.MAX_VALUE;
        int bestX = 0;
        int bestZ = 0;
        int bestY = 0;
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                int[] center = world.getVillageCenterForCell(cellX + dx, cellZ + dz);
                if (center == null) {
                    continue;
                }
                int x = center[0];
                int z = center[2];
                long distance = squaredDistance2d(blockX, blockZ, x, z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestZ = z;
                    bestY = center[1];
                }
            }
        }
        if (bestDistance == Long.MAX_VALUE) {
            return "village=none";
        }
        return "village=" + bestX + "," + bestY + "," + bestZ + " d=" + (int) Math.round(Math.sqrt(bestDistance));
    }

    private String nearestMineshaftDebug(int chunkX, int chunkZ, int blockX, int blockZ) {
        int cellX = Math.floorDiv(chunkX, 7);
        int cellZ = Math.floorDiv(chunkZ, 7);
        long bestDistance = Long.MAX_VALUE;
        int bestX = 0;
        int bestZ = 0;
        int bestY = 0;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                MineshaftCenter center = sampleMineshaftCenter(cellX + dx, cellZ + dz);
                if (center == null) {
                    continue;
                }
                int x = center.chunkX * GameConfig.CHUNK_SIZE + 8;
                int z = center.chunkZ * GameConfig.CHUNK_SIZE + 8;
                long distance = squaredDistance2d(blockX, blockZ, x, z);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestZ = z;
                    bestY = center.y;
                }
            }
        }
        if (bestDistance == Long.MAX_VALUE) {
            return "mineshaft=none";
        }
        return "mineshaft=" + bestX + "," + bestY + "," + bestZ + " d=" + (int) Math.round(Math.sqrt(bestDistance));
    }

    private long squaredDistance2d(int ax, int az, int bx, int bz) {
        long dx = (long) ax - bx;
        long dz = (long) az - bz;
        return dx * dx + dz * dz;
    }

    private String locateVillageAndTeleport() {
        int playerChunkX = Math.floorDiv((int) Math.floor(player.x), GameConfig.CHUNK_SIZE);
        int playerChunkZ = Math.floorDiv((int) Math.floor(player.z), GameConfig.CHUNK_SIZE);
        int villageCellSize = world.getVillageCellSizeChunks();
        int playerCellX = Math.floorDiv(playerChunkX, villageCellSize);
        int playerCellZ = Math.floorDiv(playerChunkZ, villageCellSize);
        int maxRadiusCells = 96;
        for (int radius = 0; radius <= maxRadiusCells; radius++) {
            for (int offsetCellX = -radius; offsetCellX <= radius; offsetCellX++) {
                for (int offsetCellZ = -radius; offsetCellZ <= radius; offsetCellZ++) {
                    if (radius != 0 && Math.abs(offsetCellX) != radius && Math.abs(offsetCellZ) != radius) {
                        continue;
                    }
                    int cellX = playerCellX + offsetCellX;
                    int cellZ = playerCellZ + offsetCellZ;
                    int[] center = world.getVillageCenterForCell(cellX, cellZ);
                    if (center == null) {
                        continue;
                    }
                    int worldX = center[0];
                    int surfaceY = center[1];
                    int worldZ = center[2];
                    teleportPlayerTo(worldX + 0.5, surfaceY + 2.0, worldZ + 0.5);
                    return "Located village at " + worldX + " " + surfaceY + " " + worldZ + ".";
                }
            }
        }
        return "Could not find structure 'village' nearby.";
    }

    private String locateMineshaftAndTeleport() {
        int playerChunkX = Math.floorDiv((int) Math.floor(player.x), GameConfig.CHUNK_SIZE);
        int playerChunkZ = Math.floorDiv((int) Math.floor(player.z), GameConfig.CHUNK_SIZE);
        int playerCellX = Math.floorDiv(playerChunkX, 7);
        int playerCellZ = Math.floorDiv(playerChunkZ, 7);
        int maxRadiusCells = 128;
        for (int radius = 0; radius <= maxRadiusCells; radius++) {
            for (int offsetCellX = -radius; offsetCellX <= radius; offsetCellX++) {
                for (int offsetCellZ = -radius; offsetCellZ <= radius; offsetCellZ++) {
                    if (radius != 0 && Math.abs(offsetCellX) != radius && Math.abs(offsetCellZ) != radius) {
                        continue;
                    }
                    MineshaftCenter center = sampleMineshaftCenter(playerCellX + offsetCellX, playerCellZ + offsetCellZ);
                    if (center == null) {
                        continue;
                    }
                    int worldX = center.chunkX * GameConfig.CHUNK_SIZE + 8;
                    int worldZ = center.chunkZ * GameConfig.CHUNK_SIZE + 8;
                    int y = center.y;
                    teleportPlayerTo(worldX + 0.5, y + 1.5, worldZ + 0.5);
                    return "Located mineshaft at " + worldX + " " + y + " " + worldZ + ".";
                }
            }
        }
        return "Could not find structure 'mineshaft' nearby.";
    }

    private MineshaftCenter sampleMineshaftCenter(int cellX, int cellZ) {
        long cellSeed = mix64(world.getSeed() ^ ((long) cellX * 73428767L) ^ ((long) cellZ * 912931L));
        if (randomUnit(cellSeed) > 0.24) {
            return null;
        }
        int centerChunkX = cellX * 7 + 2 + (int) (randomUnit(cellSeed ^ 0x31337L) * 3.0);
        int centerChunkZ = cellZ * 7 + 2 + (int) (randomUnit(cellSeed ^ 0x51515L) * 3.0);
        int y = GameConfig.SEA_LEVEL - 26 - (int) Math.round(randomUnit(cellSeed ^ 0x51EEDL) * 42.0);
        y = clamp(y, GameConfig.WORLD_MIN_Y + 12, GameConfig.SEA_LEVEL - 12);
        return new MineshaftCenter(centerChunkX, centerChunkZ, y);
    }

    private double randomUnit(long bits) {
        long mantissa = (bits >>> 11) & ((1L << 53) - 1L);
        return mantissa / (double) (1L << 53);
    }

    private long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static final class MineshaftCenter {
        final int chunkX;
        final int chunkZ;
        final int y;

        MineshaftCenter(int chunkX, int chunkZ, int y) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.y = y;
        }
    }

    private String normalizeBiomeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }

    private boolean matchesBiomeQuery(String candidate, String normalizedQuery) {
        String normalized = normalizeBiomeName(candidate);
        if (normalized.isEmpty() || normalizedQuery.isEmpty()) {
            return false;
        }
        if (normalized.equals(normalizedQuery)) {
            return true;
        }
        if ("desert".equals(normalizedQuery)) {
            return normalized.equals("desert");
        }
        if ("mountain".equals(normalizedQuery) || "mountains".equals(normalizedQuery)) {
            return normalized.contains("mountain");
        }
        if ("meadow".equals(normalizedQuery) || "plain".equals(normalizedQuery) || "plains".equals(normalizedQuery)) {
            return normalized.equals("meadow") || normalized.equals("plains");
        }
        return normalized.contains(normalizedQuery);
    }

    private void sendChatMessage(String message) {
        if (message != null && message.startsWith("/")
            && (multiplayer.isClient() || isMultiplayerCommand(message))) {
            if (multiplayer.isMultiplayerActive()) {
                multiplayer.sendCommand(message);
            } else {
                chat.addMessage("This command is only available in multiplayer.");
            }
            return;
        }
        if (multiplayer.isMultiplayerActive()) {
            multiplayer.sendChat(message);
            return;
        }
        chat.addMessage("<" + localProfile.name + "> " + message);
    }

    private boolean isMultiplayerCommand(String message) {
        String trimmed = message == null ? "" : message.trim();
        if (!trimmed.startsWith("/")) {
            return false;
        }
        String command = trimmed.substring(1).trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return "list".equals(command)
            || "help".equals(command)
            || "ping".equals(command)
            || "msg".equals(command)
            || "kick".equals(command)
            || "give".equals(command)
            || "clear".equals(command)
            || "gamemode".equals(command)
            || "gm".equals(command)
            || "gmc".equals(command)
            || "gms".equals(command)
            || "gmsp".equals(command)
            || "tp".equals(command)
            || "kill".equals(command)
            || "summon".equals(command)
            || "setblock".equals(command)
            || "fill".equals(command)
            || "time".equals(command)
            || "say".equals(command)
            || "spawnzombie".equals(command)
            || "seed".equals(command)
            || "locate".equals(command)
            || "locatebiome".equals(command)
            || "place".equals(command)
            || "whereami".equals(command)
            || "locatebug".equals(command)
            || "probe".equals(command)
            || "terrain".equals(command)
            || "heighttest".equals(command)
            || "blockinfo".equals(command);
    }

    private boolean canAttemptCheatCommands() {
        if (multiplayer.isClient()) {
            return true;
        }
        if (multiplayer.isHosting()) {
            return lanAllowCheats;
        }
        return currentWorldAllowCheats;
    }

    @Override
    public void onMultiplayerStatus(String status) {
        multiplayerStatus = status == null ? "Offline" : status;
        if (chat != null && multiplayer.isMultiplayerActive()) {
            chat.addMessage("[MP] " + multiplayerStatus);
        }
    }

    @Override
    public void onMultiplayerChat(String message) {
        chat.addMessage(message);
    }

    @Override
    public void onClientWelcome(long seed, TerrainPreset terrainPreset, int dimensionId, double x, double y, double z, double worldTime) {
        resetClientMirrorWorldState();
        multiplayerWorldLoading = true;
        multiplayerWorldLoadingStartedMillis = System.currentTimeMillis();
        world.configureWorld(RuntimePaths.resolve(GameConfig.SAVE_ROOT_DIRECTORY, "Multiplayer Client"), seed, terrainPreset);
        world.setNetworkMirrorMode(true);
        world.setActiveDimension(dimensionId);
        world.initializeNoise();
        world.setWorldTime(worldTime);
        player.dimensionId = dimensionId;
        player.setPosition(x, y, z);
        player.yaw = PlayerState.DEFAULT_YAW;
        player.pitch = PlayerState.DEFAULT_PITCH;
        resetPlayerStateForNewWorld();
        player.dimensionId = dimensionId;
        player.setPosition(x, y, z);
        currentWorldDifficulty = 2;
        worldLoaded = true;
        loadedWorldName = "Multiplayer";
        chat.clearMessages();
        mainMenuActive = false;
        paused = false;
        inventoryOpen = false;
        deathScreenActive = false;
        multiplayer.requestInitialClientChunks(player.dimensionId, x, z);
        inputController.updateCursorMode();
    }

    @Override
    public void onClientChunkColumn(int chunkX, int chunkZ) {
        int readyRadius = Math.min(renderDistanceChunks, 6);
        int readyColumns = world.loadedColumnCountAround(player, readyRadius);
        int targetColumns = world.targetColumnCountForRadius(readyRadius);
        if (multiplayerWorldLoading && readyColumns >= targetColumns) {
            multiplayerWorldLoading = false;
            resetRenderInterpolation();
        }
    }

    @Override
    public void onClientBlockUpdate(int x, int y, int z) {
        renderer.forceRebuildBlockEdit(x, y, z);
    }

    @Override
    public void onClientDisconnected(String reason) {
        String message = reason == null || reason.trim().isEmpty() ? "Connection Lost" : reason;
        resetClientMirrorWorldState();
        multiplayerWorldLoading = false;
        multiplayerStatus = message;
        mainMenuActive = true;
        mainMenuScreen = GameConfig.MENU_SCREEN_DISCONNECTED;
        mainMenuSelection = 0;
        paused = false;
        optionsOpenedFromPause = false;
        inventoryOpen = false;
        deathScreenActive = false;
        inputController.setActiveMenuTextField(-1);
        chat.addMessage("[MP] " + message);
        inputController.updateCursorMode();
    }

    @Override
    public void onClientHealth(double health) {
        player.health = clamp(health, 0.0, GameConfig.MAX_HEALTH);
        if (player.health <= 0.0) {
            enterDeathScreen();
        }
    }

    @Override
    public void onClientInventoryAdd(byte itemId, int count, int durabilityDamage) {
        if (count <= 0) {
            return;
        }
        if (!inventory.addItem(itemId, count, durabilityDamage)) {
            world.spawnDroppedItem(itemId, count, durabilityDamage, player.x, player.y + 0.8, player.z);
        }
    }

    @Override
    public void onClientServerPlayerState(int dimensionId, double x, double y, double z, double yaw, double pitch, boolean creativeMode, boolean spectatorMode, double health) {
        boolean changingWorld = player.dimensionId != dimensionId;
        if (changingWorld) {
            renderer.clearWorldMeshes();
            world.setActiveDimension(dimensionId);
        }
        player.capturePreviousPosition();
        player.dimensionId = dimensionId;
        player.setPosition(x, y, z);
        player.yaw = yaw;
        player.pitch = pitch;
        player.creativeMode = creativeMode;
        player.spectatorMode = spectatorMode;
        player.flightEnabled = creativeMode || spectatorMode;
        TinyCraft.this.creativeMode = creativeMode;
        TinyCraft.this.spectatorMode = spectatorMode;
        playerController.setCreativeFlightEnabled(player.flightEnabled && creativeMode && !spectatorMode);
        player.health = clamp(health, 0.0, GameConfig.MAX_HEALTH);
        if (changingWorld) {
            showLoadingScreen(paradiseTravelText(), GameConfig.isParadiseDimension(dimensionId) ? "Paradise" : "Overworld");
        }
        multiplayer.requestInitialClientChunks(player.dimensionId, x, z);
        resetRenderInterpolation();
        inputController.updateCursorMode();
    }

    @Override
    public void onClientInventorySync(PlayerInventory authoritativeInventory) {
        inventory.copyFrom(authoritativeInventory);
        syncSelectedHotbarItem();
    }

    @Override
    public void onClientContainerOpen(int screenMode, int x, int y, int z, int windowId, ContainerInventory chest, FurnaceBlockEntity furnace) {
        inventoryScreenMode = screenMode;
        activeChestContainer = chest;
        activeFurnace = furnace;
        inventoryOpen = true;
        resetMovement();
        resetBreakingProgress();
        inputController.setLeftMouseHeld(false);
        inputController.updateCursorMode();
        syncSelectedHotbarItem();
    }

    @Override
    public void onClientContainerUpdate(int screenMode, int x, int y, int z, int windowId, ContainerInventory chest, FurnaceBlockEntity furnace) {
        inventoryScreenMode = screenMode;
        if (screenMode == GameConfig.INVENTORY_SCREEN_CHEST) {
            if (activeChestContainer == null || chest == null || activeChestContainer.slots.length != chest.slots.length) {
                activeChestContainer = chest;
            } else {
                activeChestContainer.copyFrom(chest);
            }
            activeFurnace = null;
        } else if (screenMode == GameConfig.INVENTORY_SCREEN_FURNACE) {
            if (activeFurnace == null) {
                activeFurnace = new FurnaceBlockEntity();
            }
            activeFurnace.copyFrom(furnace);
            activeChestContainer = null;
        }
        syncSelectedHotbarItem();
    }

    @Override
    public void onClientContainerClose(int windowId) {
        activeChestContainer = null;
        activeFurnace = null;
        inventoryScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
        inventoryOpen = false;
        inputController.updateCursorMode();
        syncSelectedHotbarItem();
    }

    private boolean useHeldFood(byte heldItem) {
        int foodValue = InventoryItems.foodValue(heldItem);
        if (foodValue <= 0 || creativeMode || spectatorMode || player.hunger >= GameConfig.MAX_HUNGER) {
            return false;
        }
        player.hunger = Math.min(GameConfig.MAX_HUNGER, player.hunger + foodValue);
        player.hungerDamageTimer = 0.0;
        if (!creativeMode) {
            inventory.consumeSelectedItem(selectedSlot);
            syncSelectedHotbarItem();
        }
        return true;
    }

    private boolean useHeldBucket(byte heldItem, RayHit hit) {
        if (heldItem == InventoryItems.ITEM_BUCKET) {
            byte filledBucket = world.scoopLiquid(hit);
            if (filledBucket == GameConfig.AIR) {
                return false;
            }
            if (!creativeMode) {
                inventory.consumeSelectedItem(selectedSlot);
                inventory.addItem(filledBucket, 1);
                syncSelectedHotbarItem();
            }
            renderer.forceRebuildBlockEdit(hit.x, hit.y, hit.z);
            return true;
        }
        if (heldItem != InventoryItems.ITEM_WATER_BUCKET && heldItem != InventoryItems.ITEM_LAVA_BUCKET) {
            return false;
        }
        if (multiplayer.isClient()) {
            multiplayer.sendBlockPlace(hit, heldItem, selectedSlot);
            return true;
        }
        if (world.placeBlock(hit, heldItem, player)) {
            if (multiplayer.isHosting()) {
                multiplayer.broadcastBlockNeighborhood(world, hit.previousX, hit.previousY, hit.previousZ);
            }
            if (!creativeMode) {
                inventory.consumeSelectedItem(selectedSlot);
                inventory.addItem(InventoryItems.ITEM_BUCKET, 1);
                syncSelectedHotbarItem();
            }
            renderer.forceRebuildBlockEdit(hit.previousX, hit.previousY, hit.previousZ);
        }
        return true;
    }

    private void useSpawnEgg(MobKind kind, RayHit hit) {
        double horizontalLength = Math.cos(player.pitch);
        double dirX = Math.cos(player.yaw) * horizontalLength;
        double dirZ = Math.sin(player.yaw) * horizontalLength;
        double spawnX = hit == null ? player.x + dirX * 1.8 : hit.previousX + 0.5;
        double spawnY = hit == null ? player.y : hit.previousY + 0.05;
        double spawnZ = hit == null ? player.z + dirZ * 1.8 : hit.previousZ + 0.5;
        world.spawnMobAt(kind, spawnX, spawnY, spawnZ);
        if (!creativeMode) {
            inventory.consumeSelectedItem(selectedSlot);
            syncSelectedHotbarItem();
        }
    }

    private void updateBlockInteraction(double deltaTime) {
        if (creativeMode || spectatorMode) {
            resetBreakingProgress();
            return;
        }

        boolean consumeQueuedPress = inputController.isLeftMousePressQueued();
        inputController.setLeftMousePressQueued(false);

        if (!inputController.isLeftMouseHeld() && !consumeQueuedPress) {
            resetBreakingProgress();
            return;
        }

        if (!canBreak(hoveredBlock)) {
            resetBreakingProgress();
            return;
        }

        byte targetBlock = world.getBlock(hoveredBlock.x, hoveredBlock.y, hoveredBlock.z);
        double targetDuration = getBreakDuration(targetBlock);
        if (!isBreakingSameBlock(hoveredBlock)) {
            breakingBlockX = hoveredBlock.x;
            breakingBlockY = hoveredBlock.y;
            breakingBlockZ = hoveredBlock.z;
            breakingTimer = 0.0;
            breakingDuration = targetDuration;
            breakingHit = new RayHit(breakingBlockX, breakingBlockY, breakingBlockZ, breakingBlockX, breakingBlockY, breakingBlockZ);
            if (multiplayer.isClient()) {
                multiplayer.sendBlockBreakStart(breakingBlockX, breakingBlockY, breakingBlockZ,
                    inventory.getSelectedItemId(selectedSlot), selectedSlot);
            }
        }

        breakingTimer += deltaTime;
        if (player.handSwingTimer <= 0.03) {
            player.handSwingTimer = 0.16;
        }
        if (breakingTimer < breakingDuration) {
            return;
        }

        int blockX = hoveredBlock.x;
        int blockY = hoveredBlock.y;
        int blockZ = hoveredBlock.z;
        BlockState targetState = world.getBlockState(blockX, blockY, blockZ);
        if (multiplayer.isClient()) {
            multiplayer.sendBlockBreak(blockX, blockY, blockZ, inventory.getSelectedItemId(selectedSlot), selectedSlot);
            player.handSwingTimer = 0.18;
            playerController.spendHunger(isCorrectToolForBlock(inventory.getSelectedItemId(selectedSlot), targetBlock) ? 0.025 : 0.045);
            resetBreakingProgress();
            inputController.setLeftMousePressQueued(false);
            return;
        }
        if (world.breakBlock(hoveredBlock)) {
            if (multiplayer.isHosting()) {
                multiplayer.broadcastBlockNeighborhood(world, blockX, blockY, blockZ);
            }
            player.handSwingTimer = 0.18;
            if (!multiplayer.isClient()) {
                BlockDropRules.DropPlan dropPlan = BlockDropRules.forBrokenBlock(
                    targetBlock,
                    targetState == null ? 0 : targetState.data,
                    inventory.getSelectedItemId(selectedSlot),
                    creativeMode
                );
                world.spawnBlockDrops(dropPlan, blockX, blockY, blockZ);
            }
            playerController.spendHunger(isCorrectToolForBlock(inventory.getSelectedItemId(selectedSlot), targetBlock) ? 0.025 : 0.045);
            damageSelectedToolForBlock(targetBlock);
            audio.playBreak(targetBlock, blockX + 0.5, blockY + 0.5, blockZ + 0.5);
            renderer.forceRebuildBlockEdit(blockX, blockY, blockZ);
            hoveredBlock = world.raycastBlock(player);
        }
        resetBreakingProgress();
    }

    private int attackDamageForHeldItem() {
        byte heldItem = inventory.getSelectedItemId(selectedSlot);
        switch (heldItem) {
            case InventoryItems.NETHERITE_SWORD:
                return 9;
            case InventoryItems.DIAMOND_SWORD:
                return 8;
            case InventoryItems.IRON_SWORD:
                return 7;
            case InventoryItems.STONE_SWORD:
                return 6;
            case InventoryItems.WOODEN_SWORD:
                return 5;
            case InventoryItems.NETHERITE_AXE:
                return 10;
            case InventoryItems.DIAMOND_AXE:
                return 9;
            case InventoryItems.IRON_AXE:
                return 8;
            case InventoryItems.STONE_AXE:
                return 7;
            case InventoryItems.WOODEN_AXE:
                return 6;
            case InventoryItems.NETHERITE_PICKAXE:
                return 7;
            case InventoryItems.DIAMOND_PICKAXE:
                return 6;
            case InventoryItems.IRON_PICKAXE:
                return 5;
            case InventoryItems.STONE_PICKAXE:
            case InventoryItems.WOODEN_PICKAXE:
                return 4;
            case InventoryItems.NETHERITE_SHOVEL:
            case InventoryItems.NETHERITE_HOE:
                return 5;
            case InventoryItems.DIAMOND_SHOVEL:
            case InventoryItems.DIAMOND_HOE:
                return 4;
            case InventoryItems.IRON_SHOVEL:
            case InventoryItems.IRON_HOE:
                return 3;
            default:
                return 2;
        }
    }

    private void damageSelectedToolForBlock(byte block) {
        byte heldItem = inventory.getSelectedItemId(selectedSlot);
        if (!isCorrectToolForBlock(heldItem, block)) {
            return;
        }
        inventory.damageSelectedItem(selectedSlot, 1);
        syncSelectedHotbarItem();
    }

    private boolean isCorrectToolForBlock(byte heldItem, byte block) {
        boolean stoneLike = block == GameConfig.COBBLESTONE
            || block == GameConfig.STONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.COAL_ORE
            || block == GameConfig.IRON_ORE
            || block == GameConfig.DIAMOND_ORE
            || block == GameConfig.DEEPSLATE_COAL_ORE
            || block == GameConfig.DEEPSLATE_IRON_ORE
            || block == GameConfig.DEEPSLATE_DIAMOND_ORE
            || block == GameConfig.OBSIDIAN
            || block == GameConfig.STONE_STAIRS
            || block == GameConfig.COBBLESTONE_STAIRS;
        boolean dirtLike = block == GameConfig.GRASS
            || block == GameConfig.DIRT
            || block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.CLAY
            || block == GameConfig.FARMLAND
            || block == GameConfig.SNOW_BLOCK
            || block == GameConfig.SNOW_LAYER;
        boolean woodLike = block == GameConfig.OAK_LOG
            || block == GameConfig.PINE_LOG
            || block == GameConfig.BIRCH_LOG
            || block == InventoryItems.OAK_PLANKS
            || block == GameConfig.PINE_PLANKS
            || block == GameConfig.BIRCH_PLANKS
            || block == GameConfig.OAK_STAIRS
            || block == GameConfig.PINE_STAIRS
            || block == GameConfig.BIRCH_STAIRS
            || block == GameConfig.CHEST
            || block == GameConfig.CRAFTING_TABLE
            || block == GameConfig.OAK_FENCE
            || block == GameConfig.OAK_FENCE_GATE
            || block == GameConfig.OAK_DOOR;
        boolean hoeLike = block == GameConfig.WHEAT_CROP
            || block == GameConfig.CARROT_CROP
            || block == GameConfig.POTATO_CROP
            || block == GameConfig.OAK_LEAVES
            || block == GameConfig.PINE_LEAVES
            || block == GameConfig.BIRCH_LEAVES;
        return (stoneLike && isPickaxe(heldItem))
            || (dirtLike && isShovel(heldItem))
            || (woodLike && isAxe(heldItem))
            || (hoeLike && isHoe(heldItem));
    }

    private boolean isPickaxe(byte item) {
        return item == InventoryItems.WOODEN_PICKAXE || item == InventoryItems.STONE_PICKAXE
            || item == InventoryItems.IRON_PICKAXE || item == InventoryItems.DIAMOND_PICKAXE
            || item == InventoryItems.NETHERITE_PICKAXE;
    }

    private boolean isShovel(byte item) {
        return item == InventoryItems.WOODEN_SHOVEL || item == InventoryItems.STONE_SHOVEL
            || item == InventoryItems.IRON_SHOVEL || item == InventoryItems.DIAMOND_SHOVEL
            || item == InventoryItems.NETHERITE_SHOVEL;
    }

    private boolean isAxe(byte item) {
        return item == InventoryItems.WOODEN_AXE || item == InventoryItems.STONE_AXE
            || item == InventoryItems.IRON_AXE || item == InventoryItems.DIAMOND_AXE
            || item == InventoryItems.NETHERITE_AXE;
    }

    private boolean isHoe(byte item) {
        return item == InventoryItems.WOODEN_HOE || item == InventoryItems.STONE_HOE
            || item == InventoryItems.IRON_HOE || item == InventoryItems.DIAMOND_HOE
            || item == InventoryItems.NETHERITE_HOE;
    }

    private double knockbackForHeldItem() {
        byte heldItem = inventory.getSelectedItemId(selectedSlot);
        switch (heldItem) {
            case InventoryItems.NETHERITE_SWORD:
            case InventoryItems.NETHERITE_AXE:
                return 1.15;
            case InventoryItems.DIAMOND_SWORD:
            case InventoryItems.DIAMOND_AXE:
                return 0.95;
            case InventoryItems.IRON_SWORD:
            case InventoryItems.IRON_AXE:
                return 0.82;
            case InventoryItems.STONE_SWORD:
            case InventoryItems.STONE_AXE:
                return 0.72;
            case InventoryItems.WOODEN_SWORD:
            case InventoryItems.WOODEN_AXE:
                return 0.62;
            case InventoryItems.NETHERITE_PICKAXE:
            case InventoryItems.NETHERITE_SHOVEL:
            case InventoryItems.NETHERITE_HOE:
                return 0.78;
            case InventoryItems.DIAMOND_PICKAXE:
            case InventoryItems.DIAMOND_SHOVEL:
            case InventoryItems.DIAMOND_HOE:
                return 0.64;
            default:
                return 0.42;
        }
    }

    private void updateWaterAmbientAudio(double deltaTime) {
        if (creativeMode || spectatorMode || paused || inventoryOpen || deathScreenActive || mainMenuActive) {
            playerController.setWaterAmbientCooldown(0.0);
            return;
        }
        boolean nearbyWater = world.hasNearbyWater(player.x, player.y + 0.8, player.z, 4);
        if (!player.headInWater && !nearbyWater) {
            playerController.setWaterAmbientCooldown(0.0);
            return;
        }

        double waterAmbientCooldown = playerController.getWaterAmbientCooldown() - deltaTime;
        playerController.setWaterAmbientCooldown(waterAmbientCooldown);
        if (waterAmbientCooldown > 0.0) {
            return;
        }

        if (player.headInWater) {
            audio.playWaterBubble(player.x, player.y + 1.0, player.z);
            playerController.setWaterAmbientCooldown(0.7 + ambientRandom.nextDouble() * 1.1);
            return;
        }

        audio.playWaterAmbient(player.x, player.y + 1.0, player.z);
        playerController.setWaterAmbientCooldown(
            GameConfig.WATER_AMBIENT_MIN_INTERVAL
                + ambientRandom.nextDouble() * (GameConfig.WATER_AMBIENT_MAX_INTERVAL - GameConfig.WATER_AMBIENT_MIN_INTERVAL)
        );
    }

    private boolean canBreak(RayHit hit) {
        if (hit == null) {
            return false;
        }
        byte block = world.getBlock(hit.x, hit.y, hit.z);
        if (block == GameConfig.BEDROCK) {
            return false;
        }
        if (block == GameConfig.OBSIDIAN) {
            byte heldItem = inventory.getSelectedItemId(selectedSlot);
            return heldItem == InventoryItems.DIAMOND_PICKAXE || heldItem == InventoryItems.NETHERITE_PICKAXE;
        }
        return true;
    }

    private boolean isBreakingSameBlock(RayHit hit) {
        return hit != null
            && hit.x == breakingBlockX
            && hit.y == breakingBlockY
            && hit.z == breakingBlockZ
            && breakingDuration > 0.0;
    }

    private double getBreakDuration(byte block) {
        if (isInstantBreakBlock(block)) {
            return 0.0;
        }
        double baseDuration;
        switch (block) {
            case GameConfig.GRASS:
            case GameConfig.DIRT:
            case GameConfig.FARMLAND:
                baseDuration = 0.75;
                break;
            case GameConfig.COBBLESTONE:
            case GameConfig.STONE:
                baseDuration = 1.1;
                break;
            case GameConfig.DEEPSLATE:
                baseDuration = 1.7;
                break;
            case GameConfig.COAL_ORE:
            case GameConfig.IRON_ORE:
            case GameConfig.DIAMOND_ORE:
                baseDuration = 1.6;
                break;
            case GameConfig.DEEPSLATE_COAL_ORE:
            case GameConfig.DEEPSLATE_IRON_ORE:
            case GameConfig.DEEPSLATE_DIAMOND_ORE:
                baseDuration = 2.0;
                break;
            case GameConfig.OBSIDIAN:
                baseDuration = 8.5;
                break;
            default:
                baseDuration = 1.5;
                break;
        }
        return baseDuration / breakSpeedMultiplierForHeldItem(block);
    }

    private boolean isInstantBreakBlock(byte block) {
        return block == GameConfig.TORCH
            || block == GameConfig.WHEAT_CROP
            || block == GameConfig.CARROT_CROP
            || block == GameConfig.POTATO_CROP
            || block == GameConfig.TALL_GRASS
            || block == GameConfig.SEAGRASS
            || block == GameConfig.KELP
            || block == GameConfig.RED_FLOWER
            || block == GameConfig.YELLOW_FLOWER
            || block == GameConfig.RAIL;
    }

    private double breakSpeedMultiplierForHeldItem(byte block) {
        byte heldItem = inventory.getSelectedItemId(selectedSlot);
        boolean stoneLike = block == GameConfig.COBBLESTONE
            || block == GameConfig.STONE
            || block == GameConfig.DEEPSLATE
            || block == GameConfig.COAL_ORE
            || block == GameConfig.IRON_ORE
            || block == GameConfig.DIAMOND_ORE
            || block == GameConfig.DEEPSLATE_COAL_ORE
            || block == GameConfig.DEEPSLATE_IRON_ORE
            || block == GameConfig.DEEPSLATE_DIAMOND_ORE
            || block == GameConfig.OBSIDIAN
            || block == GameConfig.STONE_STAIRS
            || block == GameConfig.COBBLESTONE_STAIRS;
        boolean dirtLike = block == GameConfig.GRASS
            || block == GameConfig.DIRT
            || block == GameConfig.SAND
            || block == GameConfig.GRAVEL
            || block == GameConfig.CLAY
            || block == GameConfig.FARMLAND
            || block == GameConfig.SNOW_BLOCK
            || block == GameConfig.SNOW_LAYER;
        boolean woodLike = block == GameConfig.OAK_LOG
            || block == GameConfig.PINE_LOG
            || block == GameConfig.BIRCH_LOG
            || block == InventoryItems.OAK_PLANKS
            || block == GameConfig.PINE_PLANKS
            || block == GameConfig.BIRCH_PLANKS
            || block == GameConfig.OAK_STAIRS
            || block == GameConfig.PINE_STAIRS
            || block == GameConfig.BIRCH_STAIRS
            || block == GameConfig.CHEST
            || block == GameConfig.CRAFTING_TABLE
            || block == GameConfig.OAK_FENCE
            || block == GameConfig.OAK_FENCE_GATE
            || block == GameConfig.OAK_DOOR;
        if ((heldItem == InventoryItems.DIAMOND_PICKAXE || heldItem == InventoryItems.NETHERITE_PICKAXE) && stoneLike) {
            return heldItem == InventoryItems.NETHERITE_PICKAXE ? 7.0 : 6.2;
        }
        if ((heldItem == InventoryItems.IRON_PICKAXE || heldItem == InventoryItems.STONE_PICKAXE || heldItem == InventoryItems.WOODEN_PICKAXE) && stoneLike) {
            return heldItem == InventoryItems.IRON_PICKAXE ? 5.2 : (heldItem == InventoryItems.STONE_PICKAXE ? 3.8 : 2.4);
        }
        if ((heldItem == InventoryItems.DIAMOND_SHOVEL || heldItem == InventoryItems.NETHERITE_SHOVEL) && dirtLike) {
            return heldItem == InventoryItems.NETHERITE_SHOVEL ? 6.0 : 5.0;
        }
        if ((heldItem == InventoryItems.IRON_SHOVEL || heldItem == InventoryItems.STONE_SHOVEL || heldItem == InventoryItems.WOODEN_SHOVEL) && dirtLike) {
            return heldItem == InventoryItems.IRON_SHOVEL ? 4.2 : (heldItem == InventoryItems.STONE_SHOVEL ? 3.0 : 1.8);
        }
        if ((heldItem == InventoryItems.DIAMOND_AXE || heldItem == InventoryItems.NETHERITE_AXE) && woodLike) {
            return heldItem == InventoryItems.NETHERITE_AXE ? 6.0 : 5.0;
        }
        if ((heldItem == InventoryItems.IRON_AXE || heldItem == InventoryItems.STONE_AXE || heldItem == InventoryItems.WOODEN_AXE) && woodLike) {
            return heldItem == InventoryItems.IRON_AXE ? 4.2 : (heldItem == InventoryItems.STONE_AXE ? 3.0 : 1.8);
        }
        if ((heldItem == InventoryItems.DIAMOND_HOE || heldItem == InventoryItems.NETHERITE_HOE) && (block == GameConfig.WHEAT_CROP || block == GameConfig.CARROT_CROP || block == GameConfig.POTATO_CROP || block == GameConfig.OAK_LEAVES || block == GameConfig.PINE_LEAVES || block == GameConfig.BIRCH_LEAVES)) {
            return heldItem == InventoryItems.NETHERITE_HOE ? 5.0 : 4.0;
        }
        if ((heldItem == InventoryItems.IRON_HOE || heldItem == InventoryItems.STONE_HOE || heldItem == InventoryItems.WOODEN_HOE) && (block == GameConfig.WHEAT_CROP || block == GameConfig.CARROT_CROP || block == GameConfig.POTATO_CROP || block == GameConfig.OAK_LEAVES || block == GameConfig.PINE_LEAVES || block == GameConfig.BIRCH_LEAVES)) {
            return heldItem == InventoryItems.IRON_HOE ? 3.4 : (heldItem == InventoryItems.STONE_HOE ? 2.6 : 1.6);
        }
        if (stoneLike) {
            return 0.22;
        }
        return 1.0;
    }

    private void resetBreakingProgress() {
        breakingBlockX = -1;
        breakingBlockY = -1;
        breakingBlockZ = -1;
        breakingTimer = 0.0;
        breakingDuration = 0.0;
        breakingHit = null;
        inputController.setLeftMousePressQueued(false);
    }

    private RayHit currentBreakingHit() {
        return breakingHit;
    }

    private double getBreakingProgress() {
        if (breakingDuration <= 0.0) {
            return 0.0;
        }
        return clamp(breakingTimer / breakingDuration, 0.0, 1.0);
    }

    private void breakHoveredInstantly() {
        RayHit hit = creativeMode ? findPlayerIntersectingSolidBlock() : null;
        if (hit == null) {
            hit = hoveredBlock != null ? hoveredBlock : world.raycastBlock(player);
        }
        byte targetBlock = hit == null ? GameConfig.AIR : world.getBlock(hit.x, hit.y, hit.z);
        if (multiplayer.isClient() && hit != null) {
            multiplayer.sendBlockBreak(hit.x, hit.y, hit.z, inventory.getSelectedItemId(selectedSlot), selectedSlot);
            player.handSwingTimer = 0.18;
            resetBreakingProgress();
            return;
        }
        if (world.breakBlock(hit)) {
            if (multiplayer.isHosting()) {
                multiplayer.broadcastBlockNeighborhood(world, hit.x, hit.y, hit.z);
            }
            player.handSwingTimer = 0.18;
            audio.playBreak(targetBlock, hit.x + 0.5, hit.y + 0.5, hit.z + 0.5);
            renderer.forceRebuildBlockEdit(hit.x, hit.y, hit.z);
            hoveredBlock = world.raycastBlock(player);
        }
        resetBreakingProgress();
    }

    private RayHit findPlayerIntersectingSolidBlock() {
        double minX = player.x - player.radius();
        double maxX = player.x + player.radius();
        double minY = player.y;
        double maxY = player.y + player.height();
        double minZ = player.z - player.radius();
        double maxZ = player.z + player.radius();

        int startX = (int) Math.floor(minX);
        int endX = (int) Math.floor(Math.max(minX, Math.nextDown(maxX - 1.0e-7)));
        int startY = (int) Math.floor(minY);
        int endY = (int) Math.floor(Math.max(minY, Math.nextDown(maxY - 1.0e-7)));
        int startZ = (int) Math.floor(minZ);
        int endZ = (int) Math.floor(Math.max(minZ, Math.nextDown(maxZ - 1.0e-7)));

        for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int x = startX; x <= endX; x++) {
                    if (world.isInside(x, y, z) && world.isSolidBlock(world.getBlock(x, y, z))) {
                        return new RayHit(x, y, z, x, y, z);
                    }
                }
            }
        }
        return null;
    }

    private void cycleGameModeSelection() {
        int gameModeSelection = inputController.getGameModeSelection();
        if (!inputController.isGameModeSwitcherActive()) {
            gameModeSelection = currentGameModeIndex();
        }
        gameModeSelection = (gameModeSelection + 1) % GameConfig.GAME_MODE_OPTIONS.length;
        inputController.setGameModeSelection(gameModeSelection);
        inputController.setGameModeSwitcherActive(true);
    }

    private int currentGameModeIndex() {
        if (spectatorMode) {
            return 2;
        }
        return creativeMode ? 1 : 0;
    }

    private int commandGameModeIndex(String mode) {
        if ("creative".equals(mode)) {
            return 1;
        }
        if ("spectator".equals(mode)) {
            return 2;
        }
        return 0;
    }

    private void applySelectedGameMode() {
        int gameModeSelection = inputController.getGameModeSelection();
        if (multiplayer.isClient()) {
            multiplayer.sendCommand("/gamemode " + gameModeCommandName(gameModeSelection));
        } else {
            setGameMode(gameModeSelection);
            saveLocalPlayerStateIfWorldLoaded();
        }
        inputController.setGameModeSwitcherActive(false);
    }

    private static double currentGlfwTime() {
        return glfwGetTime();
    }

    private String gameModeCommandName(int mode) {
        if (mode == 1) {
            return "creative";
        }
        if (mode == 2) {
            return "spectator";
        }
        return "survival";
    }

    private void setGameMode(int mode) {
        creativeMode = mode == 1;
        spectatorMode = mode == 2;
        playerController.setCreativeFlightEnabled(spectatorMode);
        player.verticalVelocity = 0.0;
        player.isGrounded = spectatorMode ? false : playerController.isStandingOnGround();
        player.cameraBobAmount = 0.0;
        player.stepTimer = 0.0;
        resetRenderInterpolation();
        playerController.resetPlayerFluidState();
        playerController.resetPlayerDamageTimers();
        playerController.setJumpQueued(false);
        inputController.setLeftMouseHeld(false);
        if (spectatorMode) {
            inventoryOpen = false;
            resetMovement();
        }
        resetBreakingProgress();
        syncPlayerModeState();
        syncSelectedHotbarItem();
        inputController.updateCursorMode();
    }

    private void saveLocalPlayerStateIfWorldLoaded() {
        if (!worldLoaded || multiplayer.isClient()) {
            return;
        }
        syncPlayerModeState();
        saveCurrentWorldMetadata();
        world.savePlayerState(player, inventory);
    }

    private void selectHotbarSlot(int slot) {
        if (slot < 0 || slot >= PlayerInventory.HOTBAR_SIZE) {
            return;
        }
        selectedSlot = slot;
        syncSelectedHotbarItem();
    }

    private void togglePause() {
        if (deathScreenActive || mainMenuActive) {
            return;
        }
        paused = !paused;
        if (paused) {
            resetMovement();
            resetBreakingProgress();
            inputController.updateCursorMode();
        } else {
            inputController.updateCursorMode();
        }
    }

    private void toggleFullscreen() {
        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (videoMode == null) {
            return;
        }

        if (!fullscreen) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer xBuffer = stack.mallocInt(1);
                IntBuffer yBuffer = stack.mallocInt(1);
                IntBuffer widthBuffer = stack.mallocInt(1);
                IntBuffer heightBuffer = stack.mallocInt(1);
                glfwGetWindowPos(window, xBuffer, yBuffer);
                glfwGetWindowSize(window, widthBuffer, heightBuffer);
                windowedX = xBuffer.get(0);
                windowedY = yBuffer.get(0);
                windowedWidth = widthBuffer.get(0);
                windowedHeight = heightBuffer.get(0);
            }

            glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, videoMode.width(), videoMode.height(), videoMode.refreshRate());
        } else {
            glfwSetWindowMonitor(
                window,
                NULL,
                windowedX,
                windowedY,
                windowedWidth,
                windowedHeight,
                videoMode.refreshRate()
            );
        }

        fullscreen = !fullscreen;
        inputController.setMouseInitialized(false);
    }

    private void toggleInventory() {
        if (inventoryOpen) {
            closeContainerScreen();
            inventoryOpen = false;
        } else {
            if (multiplayer.isClient()) {
                multiplayer.requestContainerOpen(GameConfig.INVENTORY_SCREEN_PLAYER, 0, 0, 0);
                resetMovement();
                resetBreakingProgress();
                inputController.setLeftMouseHeld(false);
                inputController.returnCursorToInventory();
                inputController.updateCursorMode();
                syncSelectedHotbarItem();
                return;
            }
            inventoryScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
            inputController.clampCreativeScrollOffset();
            activeChestContainer = null;
            activeFurnace = null;
            inventoryOpen = true;
        }
        resetMovement();
        resetBreakingProgress();
        inputController.setLeftMouseHeld(false);

        inputController.returnCursorToInventory();

        inputController.updateCursorMode();
        syncSelectedHotbarItem();
    }

    private void openContainerScreen(int mode, int x, int y, int z) {
        if (multiplayer.isClient()) {
            multiplayer.requestContainerOpen(mode, x, y, z);
            resetMovement();
            resetBreakingProgress();
            inputController.setLeftMouseHeld(false);
            inputController.updateCursorMode();
            return;
        }
        inventory.returnTransientCraftingToInventory();
        inputController.returnCursorToInventory();
        inventoryScreenMode = mode;
        activeChestContainer = mode == GameConfig.INVENTORY_SCREEN_CHEST ? world.chestContainerAt(x, y, z) : null;
        activeFurnace = mode == GameConfig.INVENTORY_SCREEN_FURNACE ? world.furnaceAt(x, y, z) : null;
        inventoryOpen = true;
        resetMovement();
        resetBreakingProgress();
        inputController.setLeftMouseHeld(false);
        inputController.updateCursorMode();
    }

    private void closeContainerScreen() {
        if (multiplayer.isClient()) {
            multiplayer.sendContainerClose();
        }
        inventory.returnTransientCraftingToInventory();
        inputController.returnCursorToInventory();
        activeChestContainer = null;
        activeFurnace = null;
        inventoryScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
    }

    private void activatePauseOption() {
        if (pauseSelection == 0) {
            togglePause();
            return;
        }
        if (pauseSelection == 1) {
            paused = false;
            mainMenuActive = true;
            mainMenuScreen = GameConfig.MENU_SCREEN_OPTIONS;
            mainMenuSelection = 0;
            optionsOpenedFromPause = true;
            inputController.updateCursorMode();
            return;
        }
        if (pauseSelection == 2) {
            openLanScreenFromPause();
            return;
        }
        if (pauseSelection == 3) {
            enterMainMenu();
        }
    }

    private void openLanScreenFromPause() {
        if (multiplayer.isClient()) {
            chat.addMessage("[MP] Client cannot open this world to LAN.");
            togglePause();
            return;
        }
        if (multiplayer.isHosting()) {
            chat.addMessage("[MP] " + multiplayer.status());
            togglePause();
            return;
        }
        lanGameMode = creativeMode ? 1 : (spectatorMode ? 2 : 0);
        lanAllowCheats = creativeMode || spectatorMode;
        paused = false;
        mainMenuActive = true;
        mainMenuScreen = GameConfig.MENU_SCREEN_OPEN_LAN;
        mainMenuSelection = 0;
        lanOpenedFromPause = true;
        inputController.setActiveMenuTextField(-1);
        inputController.updateCursorMode();
    }

    private void applyOptionsSlider(int slider, double percent) {
        if (slider == 0) {
            renderDistanceChunks = (int) Math.round(lerp(GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS, percent));
            world.setRenderDistanceChunks(renderDistanceChunks);
        } else if (slider == 1) {
            fovDegrees = (int) Math.round(lerp(55.0, 100.0, percent));
        } else if (slider == 2) {
            Settings.brightness = clamp((int) Math.round(percent * 100.0), 0, 100);
        } else if (slider == 3) {
            Settings.masterVolume = clamp((int) Math.round(percent * 100.0), 0, 100);
        } else if (slider == 4) {
            maxFps = Settings.maxFpsFromSliderPercent(percent);
            glfwSwapInterval(maxFps == Settings.FPS_VSYNC ? 1 : 0);
        } else if (slider == 5) {
            Settings.guiScale = clamp((int) Math.round(percent * 4.0), 0, 4);
        }
        Settings.save(renderDistanceChunks, fovDegrees, maxFps);
    }

    private void nudgeOptionsSelection(int direction) {
        if (mainMenuSelection == 0) {
            renderDistanceChunks = clamp(renderDistanceChunks + direction, GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
            world.setRenderDistanceChunks(renderDistanceChunks);
        } else if (mainMenuSelection == 1) {
            fovDegrees = clamp(fovDegrees + direction * 2, 55, 100);
        } else if (mainMenuSelection == 2) {
            Settings.brightness = clamp(Settings.brightness + direction * 5, 0, 100);
        } else if (mainMenuSelection == 3) {
            Settings.masterVolume = clamp(Settings.masterVolume + direction * 5, 0, 100);
        } else if (mainMenuSelection == 4) {
            maxFps = Settings.nudgeMaxFps(maxFps, direction);
            glfwSwapInterval(maxFps == Settings.FPS_VSYNC ? 1 : 0);
        } else if (mainMenuSelection == 5) {
            Settings.guiScale = clamp(Settings.guiScale + direction, 0, 4);
        } else if (mainMenuSelection == 6) {
            Settings.graphicsQuality = 1 - Settings.graphicsQuality;
        } else if (mainMenuSelection == 7) {
            Settings.toggleLanguage();
        }
        Settings.save(renderDistanceChunks, fovDegrees, maxFps);
    }

    private void closeOptionsMenu() {
        inputController.setActiveMenuTextField(-1);
        if (optionsOpenedFromPause) {
            mainMenuActive = false;
            paused = true;
            mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
            pauseSelection = 1;
            optionsOpenedFromPause = false;
            inputController.updateCursorMode();
            return;
        }
        mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
        mainMenuSelection = 2;
        optionsOpenedFromPause = false;
    }

    private void resetMovement() {
        inputController.resetMovementInput();
    }

    private void resetChatInputState() {
        resetMovement();
        resetBreakingProgress();
        inputController.updateCursorMode();
    }

    private void handleInventoryMouseClick(int button, int action, int mods) {
        if (action != GLFW_PRESS
            || (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT && button != GLFW_MOUSE_BUTTON_MIDDLE)) {
            return;
        }

        boolean creativeInventory = creativeMode && inventoryScreenMode == GameConfig.INVENTORY_SCREEN_PLAYER;
        double mouseX = inputController.getMouseX();
        double mouseY = inputController.getMouseY();
        int creativeTab = inputController.getCreativeTab();
        int creativeScrollOffset = inputController.getCreativeScrollOffset();
        if (creativeInventory) {
            int clickedTab = renderer.getCreativeTabAt(mouseX, mouseY, creativeTab);
            if (clickedTab != -1) {
                inputController.setCreativeTab(clickedTab);
                inputController.setCreativeScrollOffset(0);
                return;
            }
        }

        InventorySlotRef slot = renderer.getInventorySlotAt(inventory, creativeInventory, creativeTab, creativeScrollOffset, inventoryScreenMode, mouseX, mouseY);
        boolean insideInventory = renderer.isInventoryPointInside(creativeInventory, creativeTab, inventoryScreenMode, mouseX, mouseY);
        if (slot == null && !insideInventory) {
            if (multiplayer.isClient()) {
                return;
            }
            dropCursorStack();
            syncSelectedHotbarItem();
            return;
        }

        if (slot != null) {
            boolean rightClick = button == GLFW_MOUSE_BUTTON_RIGHT;
            boolean middleClick = button == GLFW_MOUSE_BUTTON_MIDDLE;
            boolean shiftDown = (mods & GLFW_MOD_SHIFT) != 0;
            if (multiplayer.isClient()) {
                multiplayer.sendContainerClick(slot, rightClick, shiftDown, middleClick);
                return;
            }
            inventory.handleClick(slot, creativeInventory, rightClick, shiftDown, middleClick, activeChestContainer, activeFurnace);
            syncSelectedHotbarItem();
        }
    }

    private void syncSelectedHotbarItem() {
        selectedBlock = inventory.getSelectedItemId(selectedSlot);
    }

    private void dropCursorStack() {
        ItemStack cursor = inventory.getCursorStack();
        if (cursor == null || cursor.isEmpty()) {
            return;
        }
        double yaw = player.yaw;
        double pitch = player.pitch;
        double forwardX = Math.cos(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.sin(yaw) * Math.cos(pitch);
        world.spawnThrownItem(cursor.itemId, cursor.count, cursor.durabilityDamage,
            player.x + forwardX * 0.72,
            player.y + player.eyeHeight() * 0.78,
            player.z + forwardZ * 0.72,
            forwardX * 3.5,
            forwardY * 3.5 + 0.8,
            forwardZ * 3.5);
        inventory.clearCursor();
    }

    private void dropInventoryOnDeath() {
        if (inventoryDroppedOnDeath) {
            return;
        }
        inventoryDroppedOnDeath = true;
        for (int i = 0; i < PlayerInventory.STORAGE_SIZE; i++) {
            dropInventoryStack(inventory.getStorageStack(i));
        }
        for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
            dropInventoryStack(inventory.getHotbarStack(i));
        }
        for (int i = 0; i < PlayerInventory.ARMOR_SIZE; i++) {
            dropInventoryStack(inventory.getArmorStack(i));
        }
        for (int i = 0; i < PlayerInventory.CRAFT_SIZE; i++) {
            dropInventoryStack(inventory.getCraftStack(i));
        }
        for (int i = 0; i < PlayerInventory.WORKBENCH_CRAFT_SIZE; i++) {
            dropInventoryStack(inventory.getWorkbenchCraftStack(i));
        }
        dropInventoryStack(inventory.getOffhandStack());
        dropInventoryStack(inventory.getCursorStack());
        inventory.clearAll();
        syncSelectedHotbarItem();
    }

    private void dropInventoryStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        world.spawnDroppedItem(stack.itemId, stack.count, stack.durabilityDamage,
            player.x, player.y + 0.55, player.z);
    }

    private void dropSelectedHotbarItem() {
        ItemStack stack = inventory.getHotbarStack(selectedSlot);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (multiplayer.isMultiplayerActive()) {
            multiplayer.sendDropSelectedHotbarItem(selectedSlot);
            return;
        }
        double yaw = player.yaw;
        double pitch = player.pitch;
        double forwardX = Math.cos(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.sin(yaw) * Math.cos(pitch);
        double spawnX = player.x + forwardX * 0.72;
        double spawnY = player.y + player.eyeHeight() * 0.78;
        double spawnZ = player.z + forwardZ * 0.72;
        world.spawnThrownItem(stack.itemId, 1, stack.durabilityDamage, spawnX, spawnY, spawnZ, forwardX * 4.2, forwardY * 4.2 + 1.0, forwardZ * 4.2);
        if (!creativeMode) {
            stack.count--;
            if (stack.count <= 0) {
                stack.clear();
            }
            syncSelectedHotbarItem();
        }
    }

    private void syncPlayerModeState() {
        player.creativeMode = creativeMode;
        player.spectatorMode = spectatorMode;
        player.flightEnabled = spectatorMode || (creativeMode && playerController.isCreativeFlightEnabled());
        player.sneaking = playerController.isSneak() && !spectatorMode;
    }

    private void syncLocalModeStateFromPlayer() {
        creativeMode = player.creativeMode;
        spectatorMode = player.spectatorMode;
        playerController.setCreativeFlightEnabled(player.flightEnabled && creativeMode && !spectatorMode);
        if (spectatorMode) {
            playerController.setCreativeFlightEnabled(true);
            playerController.setSneak(false);
        }
    }

    private void updatePlayerArmorProtection() {
        player.armorProtection = inventory.getArmorProtection();
    }

    private void handleJumpPress() {
        playerController.handleJumpPress();
    }

    private boolean shouldRunWorldSimulation() {
        return worldLoaded && !mainMenuActive && (!paused || multiplayer.isMultiplayerActive());
    }

    static boolean shouldEnterSurvivalDeathScreen(boolean deathScreenActive, boolean creativeMode,
                                                   boolean spectatorMode, double health) {
        return !deathScreenActive && !creativeMode && !spectatorMode && health <= 0.0;
    }

    private void enterDeathScreen() {
        if (deathScreenActive) {
            player.health = 0.0;
            return;
        }
        deathScreenActive = true;
        player.health = 0.0;
        dropInventoryOnDeath();
        inputController.setGameModeSwitcherActive(false);
        inputController.setF3Held(false);
        deathSelection = 0;
        paused = false;
        inventoryOpen = false;
        mainMenuActive = false;
        player.verticalVelocity = 0.0;
        player.isGrounded = false;
        player.stepTimer = 0.0;
        playerController.resetPlayerFluidState();
        playerController.resetPlayerDamageTimers();
        hoveredBlock = null;
        resetMovement();
        resetBreakingProgress();
        syncPlayerModeState();
        syncSelectedHotbarItem();
        inputController.updateCursorMode();
    }

    private void activateDeathOption() {
        if (deathSelection == 0) {
            respawnPlayer();
            return;
        }
        enterMainMenu();
    }

    private void handleMainMenuKey(int key) {
        if (key == GLFW_KEY_ESCAPE) {
            if (mainMenuScreen == GameConfig.MENU_SCREEN_MAIN) {
                return;
            }
            if (mainMenuScreen == GameConfig.MENU_SCREEN_OPTIONS) {
                closeOptionsMenu();
                return;
            }
            if (mainMenuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD || mainMenuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
                mainMenuScreen = GameConfig.MENU_SCREEN_SINGLEPLAYER;
                mainMenuSelection = 1;
                inputController.setActiveMenuTextField(-1);
            } else if (mainMenuScreen == GameConfig.MENU_SCREEN_OPEN_LAN) {
                closeOpenLanMenu();
            } else if (mainMenuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
                mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
                mainMenuSelection = 1;
                inputController.setActiveMenuTextField(-1);
            } else if (mainMenuScreen == GameConfig.MENU_SCREEN_DISCONNECTED) {
                mainMenuScreen = GameConfig.MENU_SCREEN_MULTIPLAYER;
                mainMenuSelection = 1;
                inputController.setActiveMenuTextField(-1);
            } else {
                mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
                mainMenuSelection = 0;
                inputController.setActiveMenuTextField(-1);
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER) {
            if (key == GLFW_KEY_A) {
                mainMenuSelection = (mainMenuSelection + GameConfig.SINGLEPLAYER_ACTIONS.length - 1) % GameConfig.SINGLEPLAYER_ACTIONS.length;
            } else if (key == GLFW_KEY_D) {
                mainMenuSelection = (mainMenuSelection + 1) % GameConfig.SINGLEPLAYER_ACTIONS.length;
            } else if ((key == GLFW_KEY_W || key == GLFW_KEY_UP) && !availableWorlds.isEmpty()) {
                selectedWorldIndex = (selectedWorldIndex + availableWorlds.size() - 1) % availableWorlds.size();
                mainMenuWorldActionsEnabled = false;
                keepSelectedWorldVisible();
            } else if ((key == GLFW_KEY_S || key == GLFW_KEY_DOWN) && !availableWorlds.isEmpty()) {
                selectedWorldIndex = (selectedWorldIndex + 1) % availableWorlds.size();
                mainMenuWorldActionsEnabled = false;
                keepSelectedWorldVisible();
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                activateMainMenuOption();
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD) {
            if (inputController.getActiveMenuTextField() >= 0) {
                if (key == GLFW_KEY_TAB) {
                    inputController.setActiveMenuTextField(inputController.getActiveMenuTextField() == 0 ? 1 : 0);
                } else if (key == GLFW_KEY_ENTER) {
                    activateMainMenuOption();
                }
                return;
            }
            if (key == GLFW_KEY_TAB) {
                inputController.setActiveMenuTextField(0);
            } else if (key == GLFW_KEY_A) {
                mainMenuSelection = (mainMenuSelection + GameConfig.CREATE_WORLD_ACTIONS.length - 1) % GameConfig.CREATE_WORLD_ACTIONS.length;
            } else if (key == GLFW_KEY_D) {
                mainMenuSelection = (mainMenuSelection + 1) % GameConfig.CREATE_WORLD_ACTIONS.length;
            } else if (key == GLFW_KEY_W || key == GLFW_KEY_UP) {
                inputController.setActiveMenuTextField(inputController.getActiveMenuTextField() == 1 ? 0 : 1);
            } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN) {
                inputController.setActiveMenuTextField(inputController.getActiveMenuTextField() == 0 ? 1 : 0);
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                activateMainMenuOption();
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
            if (inputController.getActiveMenuTextField() >= 0) {
                if (key == GLFW_KEY_TAB) {
                    inputController.setActiveMenuTextField(-1);
                } else if (key == GLFW_KEY_ENTER) {
                    activateMainMenuOption();
                }
                return;
            }
            if (key == GLFW_KEY_TAB) {
                inputController.setActiveMenuTextField(0);
            } else if (key == GLFW_KEY_A) {
                mainMenuSelection = (mainMenuSelection + GameConfig.RENAME_WORLD_ACTIONS.length - 1) % GameConfig.RENAME_WORLD_ACTIONS.length;
            } else if (key == GLFW_KEY_D) {
                mainMenuSelection = (mainMenuSelection + 1) % GameConfig.RENAME_WORLD_ACTIONS.length;
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                activateMainMenuOption();
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
            if (inputController.getActiveMenuTextField() >= 0) {
                if (key == GLFW_KEY_TAB) {
                    inputController.setActiveMenuTextField((inputController.getActiveMenuTextField() + 1) % 3);
                } else if (key == GLFW_KEY_ENTER) {
                    activateMainMenuOption();
                }
                return;
            }
            if (key == GLFW_KEY_TAB) {
                inputController.setActiveMenuTextField(0);
            } else if (key == GLFW_KEY_A) {
                mainMenuSelection = (mainMenuSelection + GameConfig.MULTIPLAYER_ACTIONS.length - 1) % GameConfig.MULTIPLAYER_ACTIONS.length;
            } else if (key == GLFW_KEY_D) {
                mainMenuSelection = (mainMenuSelection + 1) % GameConfig.MULTIPLAYER_ACTIONS.length;
            } else if (key == GLFW_KEY_W || key == GLFW_KEY_UP) {
                inputController.setActiveMenuTextField(
                    inputController.getActiveMenuTextField() <= 0 ? 2 : inputController.getActiveMenuTextField() - 1
                );
            } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN) {
                inputController.setActiveMenuTextField((inputController.getActiveMenuTextField() + 1) % 3);
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                activateMainMenuOption();
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_OPEN_LAN) {
            if (key == GLFW_KEY_A) {
                mainMenuSelection = (mainMenuSelection + GameConfig.LAN_ACTIONS.length - 1) % GameConfig.LAN_ACTIONS.length;
            } else if (key == GLFW_KEY_D) {
                mainMenuSelection = (mainMenuSelection + 1) % GameConfig.LAN_ACTIONS.length;
            } else if (key == GLFW_KEY_W || key == GLFW_KEY_UP) {
                lanGameMode = (lanGameMode + GameConfig.GAME_MODE_OPTIONS.length - 1) % GameConfig.GAME_MODE_OPTIONS.length;
            } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN) {
                lanAllowCheats = !lanAllowCheats;
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                activateMainMenuOption();
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_DISCONNECTED) {
            if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                mainMenuScreen = GameConfig.MENU_SCREEN_MULTIPLAYER;
                mainMenuSelection = 1;
                inputController.setActiveMenuTextField(-1);
            }
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_OPTIONS) {
            if (key == GLFW_KEY_W || key == GLFW_KEY_UP) {
                mainMenuSelection = (mainMenuSelection + 8) % 9;
            } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN) {
                mainMenuSelection = (mainMenuSelection + 1) % 9;
            } else if (key == GLFW_KEY_A || key == GLFW_KEY_LEFT) {
                nudgeOptionsSelection(-1);
            } else if (key == GLFW_KEY_D || key == GLFW_KEY_RIGHT) {
                nudgeOptionsSelection(1);
            } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
                if (mainMenuSelection == 6) {
                    Settings.graphicsQuality = 1 - Settings.graphicsQuality;
                    Settings.save(renderDistanceChunks, fovDegrees, maxFps);
                } else if (mainMenuSelection == 7) {
                    Settings.toggleLanguage();
                    Settings.save(renderDistanceChunks, fovDegrees, maxFps);
                } else if (mainMenuSelection == 8) {
                    closeOptionsMenu();
                }
            }
            return;
        }

        if (key == GLFW_KEY_W || key == GLFW_KEY_UP || key == GLFW_KEY_A) {
            mainMenuSelection = (mainMenuSelection + GameConfig.WORLD_MENU_ACTIONS.length - 1) % GameConfig.WORLD_MENU_ACTIONS.length;
        } else if (key == GLFW_KEY_S || key == GLFW_KEY_DOWN || key == GLFW_KEY_D) {
            mainMenuSelection = (mainMenuSelection + 1) % GameConfig.WORLD_MENU_ACTIONS.length;
        } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
            activateMainMenuOption();
        }
    }

    private boolean handleMainMenuTextKey(int key) {
        int activeMenuTextField = inputController.getActiveMenuTextField();
        if (!mainMenuActive || activeMenuTextField < 0 || key != GLFW_KEY_BACKSPACE) {
            return false;
        }
        if (mainMenuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD) {
            if (activeMenuTextField == 0 && !createWorldName.isEmpty()) {
                createWorldName = createWorldName.substring(0, createWorldName.length() - 1);
            } else if (activeMenuTextField == 1 && !createWorldSeed.isEmpty()) {
                createWorldSeed = createWorldSeed.substring(0, createWorldSeed.length() - 1);
            }
            return true;
        }
        if (mainMenuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
            if (!renameWorldName.isEmpty()) {
                renameWorldName = renameWorldName.substring(0, renameWorldName.length() - 1);
            }
            return true;
        }
        if (mainMenuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
            if (activeMenuTextField == 0 && !multiplayerName.isEmpty()) {
                multiplayerName = multiplayerName.substring(0, multiplayerName.length() - 1);
            } else if (activeMenuTextField == 1 && !multiplayerHost.isEmpty()) {
                multiplayerHost = multiplayerHost.substring(0, multiplayerHost.length() - 1);
            } else if (activeMenuTextField == 2 && !multiplayerPort.isEmpty()) {
                multiplayerPort = multiplayerPort.substring(0, multiplayerPort.length() - 1);
            }
            return true;
        }
        return false;
    }

    private boolean handleMainMenuCharacter(int codepoint) {
        int activeMenuTextField = inputController.getActiveMenuTextField();
        if (!mainMenuActive || activeMenuTextField < 0 || codepoint < 32 || codepoint > Character.MAX_VALUE) {
            return false;
        }
        char character = (char) codepoint;
        if (mainMenuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD) {
            if (activeMenuTextField == 0 && createWorldName.length() < 32 && isSafeWorldNameCharacter(character)) {
                createWorldName += character;
                return true;
            }
            if (activeMenuTextField == 1 && createWorldSeed.length() < 32 && isSeedCharacter(character)) {
                createWorldSeed += character;
                return true;
            }
            return true;
        }
        if (mainMenuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
            if (renameWorldName.length() < 32 && isSafeWorldNameCharacter(character)) {
                renameWorldName += character;
            }
            return true;
        }
        if (mainMenuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
            if (activeMenuTextField == 0 && multiplayerName.length() < LocalProfile.MAX_NAME_LENGTH && isProfileNameCharacter(character)) {
                multiplayerName += character;
            } else if (activeMenuTextField == 1 && multiplayerHost.length() < 64 && isHostCharacter(character)) {
                multiplayerHost += character;
            } else if (activeMenuTextField == 2 && multiplayerPort.length() < 5 && Character.isDigit(character)) {
                multiplayerPort += character;
            }
            return true;
        }
        return false;
    }

    private boolean isSafeWorldNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == ' ' || character == '_' || character == '-';
    }

    private boolean isSeedCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '-' || character == '_';
    }

    private boolean isProfileNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '-';
    }

    private boolean isHostCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '.' || character == '-' || character == '_' || character == ':';
    }

    private void activateMainMenuOption() {
        if (mainMenuScreen == GameConfig.MENU_SCREEN_MAIN) {
            switch (mainMenuSelection) {
                case 0:
                    refreshAvailableWorlds();
                    mainMenuScreen = GameConfig.MENU_SCREEN_SINGLEPLAYER;
                    mainMenuSelection = 0;
                    mainMenuWorldActionsEnabled = false;
                    lastWorldSlotClickIndex = -1;
                    lastWorldSlotClickTime = -1.0;
                    return;
                case 1:
                    openMultiplayerScreen();
                    return;
                case 2:
                    mainMenuScreen = GameConfig.MENU_SCREEN_OPTIONS;
                    mainMenuSelection = 0;
                    optionsOpenedFromPause = false;
                    return;
                default:
                    glfwSetWindowShouldClose(window, true);
                    return;
            }
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_SINGLEPLAYER) {
            if (!isSingleplayerActionEnabled(mainMenuSelection)) {
                return;
            }
            switch (mainMenuSelection) {
                case 0:
                    ensureWorldLoaded();
                    mainMenuActive = false;
                    deathScreenActive = false;
                    paused = false;
                    inventoryOpen = false;
                    inputController.updateCursorMode();
                    return;
                case 1:
                    openCreateWorldScreen();
                    return;
                case 2:
                    openRenameWorldScreen();
                    return;
                case 3:
                    deleteSelectedWorld();
                    mainMenuWorldActionsEnabled = false;
                    lastWorldSlotClickIndex = -1;
                    lastWorldSlotClickTime = -1.0;
                    return;
                default:
                    mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
                    mainMenuSelection = 0;
                    return;
            }
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_MULTIPLAYER) {
            activateMultiplayerOption();
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_OPEN_LAN) {
            activateOpenLanOption();
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_DISCONNECTED) {
            mainMenuScreen = GameConfig.MENU_SCREEN_MULTIPLAYER;
            mainMenuSelection = 1;
            inputController.setActiveMenuTextField(-1);
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_CREATE_WORLD) {
            if (mainMenuSelection == 0) {
                createNewWorld(createWorldName, createWorldSeed, createWorldGameMode, createWorldAllowCheats, createWorldDifficulty, createWorldTerrainPreset);
                showLoadingScreen(loadingTerrainText(), buildingWorldText());
                ensureWorldLoaded();
                mainMenuActive = false;
                deathScreenActive = false;
                paused = false;
                inventoryOpen = false;
                inputController.setActiveMenuTextField(-1);
                inputController.updateCursorMode();
                return;
            }
            mainMenuScreen = GameConfig.MENU_SCREEN_SINGLEPLAYER;
            mainMenuSelection = 1;
            inputController.setActiveMenuTextField(-1);
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_RENAME_WORLD) {
            if (mainMenuSelection == 0) {
                renameSelectedWorld(renameWorldName);
                mainMenuWorldActionsEnabled = true;
            }
            mainMenuScreen = GameConfig.MENU_SCREEN_SINGLEPLAYER;
            mainMenuSelection = 2;
            inputController.setActiveMenuTextField(-1);
            return;
        }

        if (mainMenuScreen == GameConfig.MENU_SCREEN_OPTIONS) {
            if (mainMenuSelection == 6) {
                Settings.graphicsQuality = 1 - Settings.graphicsQuality;
                Settings.save(renderDistanceChunks, fovDegrees, maxFps);
                return;
            }
            if (mainMenuSelection == 7) {
                Settings.toggleLanguage();
                Settings.save(renderDistanceChunks, fovDegrees, maxFps);
                return;
            }
            if (mainMenuSelection == 8) {
                closeOptionsMenu();
            }
            return;
        }

        mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
        mainMenuSelection = 0;
    }

    private boolean isSingleplayerActionEnabled(int actionIndex) {
        if (actionIndex == 1 || actionIndex == 4) {
            return true;
        }
        return mainMenuWorldActionsEnabled && !availableWorlds.isEmpty();
    }

    private void showLoadingScreen(String message, String detail) {
        renderer.renderLoadingScreen(message, detail);
        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    private void ensureWorldLoaded() {
        if (availableWorlds.isEmpty()) {
            createNewWorld(nextWorldName(), "", 0, false, 2, TerrainPreset.NEW_WORLD_DEFAULT.createWorldOptionIndex());
        }
        WorldInfo worldInfo = selectedWorld();
        if (worldInfo == null) {
            return;
        }
        currentWorldDifficulty = worldInfo.difficulty;
        currentWorldAllowCheats = worldInfo.allowCheats;

        if (worldLoaded && worldInfo.name.equals(loadedWorldName)) {
            return;
        }

        if (worldLoaded) {
            syncPlayerModeState();
            saveCurrentWorldMetadata();
            world.savePlayerState(player, inventory);
            world.saveAllLoadedColumns();
        }
        renderer.clearWorldMeshes();
        showLoadingScreen(loadingTerrainText(), worldInfo.name);
        long worldGenerationStart = System.nanoTime();
        world.configureWorld(worldInfo.directory, worldInfo.seed, worldInfo.terrainPreset);
        world.initializeNoise();
        world.generateWorld();
        FrameProfiler.logTask("world.generateWorld", System.nanoTime() - worldGenerationStart);
        resetInventoryScreenStateForWorldLoad();
        inventory.clearAll();
        if (!world.loadPlayerState(player, inventory)) {
            resetPlayerStateForNewWorld();
            world.placePlayerAtSpawn(player);
            setGameMode(worldInfo.gameMode);
        } else {
            syncLocalModeStateFromPlayer();
        }
        world.prepareForPlayer(player);
        world.primeStreamingAround(player);
        warmupWorldStreaming(worldInfo.name);
        world.clearNetworkDirtyBlocks();
        long meshBuildStart = System.nanoTime();
        renderer.buildAllChunkMeshes();
        FrameProfiler.logTask("renderer.buildAllChunkMeshes", System.nanoTime() - meshBuildStart);
        loadedWorldName = worldInfo.name;
        worldLoaded = true;
        chat.clearMessages();
        deathScreenActive = false;
        inventoryDroppedOnDeath = false;
        player.health = clamp(player.health, 0.0, GameConfig.MAX_HEALTH);
        player.hunger = clamp(player.hunger, 0.0, GameConfig.MAX_HUNGER);
        player.verticalVelocity = 0.0;
        player.isGrounded = true;
        resetRenderInterpolation();
        playerController.resetPlayerDamageTimers();
        playerController.resetPlayerFluidState();
        resetMovement();
        resetBreakingProgress();
        syncPlayerModeState();
        syncSelectedHotbarItem();
        if (shouldEnterSurvivalDeathScreen(deathScreenActive, creativeMode, spectatorMode, player.health)) {
            enterDeathScreen();
        }
    }

    private void resetInventoryScreenStateForWorldLoad() {
        inventoryOpen = false;
        activeChestContainer = null;
        activeFurnace = null;
        inventoryScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
    }

    private void resetPlayerStateForNewWorld() {
        creativeMode = false;
        spectatorMode = false;
        playerController.setCreativeFlightEnabled(false);
        deathScreenActive = false;
        inventoryDroppedOnDeath = false;
        player.health = GameConfig.MAX_HEALTH;
        player.hunger = GameConfig.MAX_HUNGER;
        player.hungerDrainTimer = 0.0;
        player.hungerDamageTimer = 0.0;
        player.hungerRegenTimer = 0.0;
        player.hungerGraceRemaining = GameConfig.HUNGER_GRACE_SECONDS;
        player.hasCustomSpawn = false;
        player.spawnX = 0.0;
        player.spawnY = 0.0;
        player.spawnZ = 0.0;
        player.hasParadiseReturn = false;
        player.paradiseReturnX = 0.0;
        player.paradiseReturnY = 0.0;
        player.paradiseReturnZ = 0.0;
        player.paradiseReturnDimensionId = GameConfig.DIMENSION_OVERWORLD;
        player.paradisePortalTimer = 0.0;
        player.paradisePortalCooldown = 0.0;
        player.dimensionId = GameConfig.DIMENSION_OVERWORLD;
        player.yaw = PlayerState.DEFAULT_YAW;
        player.pitch = PlayerState.DEFAULT_PITCH;
        player.verticalVelocity = 0.0;
        player.cameraBobPhase = 0.0;
        player.cameraBobAmount = 0.0;
        player.stepTimer = 0.0;
        playerController.resetLastCreativeJumpTapTime();
        syncPlayerModeState();
    }

    private void warmupWorldStreaming(String worldName) {
        int warmupRadius = Math.min(renderDistanceChunks, 14);
        int targetColumns = world.targetColumnCountForRadius(warmupRadius);
        int minimumReadyColumns = Math.max(9, (int) Math.ceil(targetColumns * 0.70));
        long deadlineNs = System.nanoTime() + 4_000_000_000L;
        int loadedColumns = world.loadedColumnCountAround(player, warmupRadius);
        while (loadedColumns < minimumReadyColumns && System.nanoTime() < deadlineNs && !glfwWindowShouldClose(window)) {
            world.primeStreamingAround(player);
            String detail = worldName + " " + loadedColumns + "/" + targetColumns
                + " chunks, queue " + world.pendingColumnCount();
            showLoadingScreen(loadingTerrainText(), detail);
            sleepQuietly(25L);
            loadedColumns = world.loadedColumnCountAround(player, warmupRadius);
        }
        world.primeStreamingAround(player);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void enterMainMenu() {
        multiplayer.stop();
        world.clearRemotePlayers();
        if (worldLoaded) {
            syncPlayerModeState();
            saveCurrentWorldMetadata();
            world.savePlayerState(player, inventory);
            world.saveAllLoadedColumns();
        }
        refreshAvailableWorlds();
        deathScreenActive = false;
        inputController.setGameModeSwitcherActive(false);
        inputController.setF3Held(false);
        paused = false;
        inventoryOpen = false;
        mainMenuActive = true;
        mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
        mainMenuSelection = 0;
        optionsOpenedFromPause = false;
        lanOpenedFromPause = false;
        mainMenuWorldActionsEnabled = false;
        creativeMode = false;
        spectatorMode = false;
        playerController.setCreativeFlightEnabled(false);
        player.health = GameConfig.MAX_HEALTH;
        player.verticalVelocity = 0.0;
        player.isGrounded = true;
        player.cameraBobPhase = 0.0;
        player.cameraBobAmount = 0.0;
        player.stepTimer = 0.0;
        playerController.resetPlayerFluidState();
        playerController.resetPlayerDamageTimers();
        hoveredBlock = null;
        resetMovement();
        resetBreakingProgress();
        syncPlayerModeState();
        syncSelectedHotbarItem();
        inputController.updateCursorMode();
    }

    private void refreshAvailableWorlds() {
        availableWorlds.clear();
        Path savesRoot = RuntimePaths.resolve(GameConfig.SAVE_ROOT_DIRECTORY);
        try {
            Files.createDirectories(savesRoot);
            try (Stream<Path> stream = Files.list(savesRoot)) {
                stream.filter(Files::isDirectory)
                    .filter(TinyCraft::hasWorldMetadata)
                    .forEach(directory -> {
                        try {
                            WorldMetadata metadata = readWorldMetadata(directory);
                            long lastModified = worldLastModified(directory);
                            availableWorlds.add(new WorldInfo(directory.getFileName().toString(), directory, metadata.seed, lastModified, metadata.gameMode, metadata.difficulty, metadata.terrainPreset, metadata.allowCheats));
                        } catch (IOException | NumberFormatException exception) {
                            System.err.println("Failed to read world metadata from " + directory + ": " + exception);
                        }
                    });
            }
        } catch (IOException exception) {
            System.err.println("Failed to list saved worlds in " + savesRoot + ": " + exception);
        }

        availableWorlds.sort((left, right) -> compareWorldInfoByName(right, left));
        if (availableWorlds.isEmpty()) {
            selectedWorldIndex = 0;
            mainMenuScrollOffset = 0;
            return;
        }
        if (loadedWorldName != null) {
            for (int i = 0; i < availableWorlds.size(); i++) {
                if (availableWorlds.get(i).name.equals(loadedWorldName)) {
                    selectedWorldIndex = i;
                    keepSelectedWorldVisible();
                    return;
                }
            }
        }
        selectedWorldIndex = Math.max(0, Math.min(selectedWorldIndex, availableWorlds.size() - 1));
        clampMainMenuScrollOffset();
    }

    static boolean hasWorldMetadata(Path directory) {
        return Files.isRegularFile(directory.resolve(GameConfig.SAVE_LEVEL_FILE))
            || Files.isRegularFile(directory.resolve(GameConfig.SAVE_METADATA_FILE));
    }

    private void keepSelectedWorldVisible() {
        if (selectedWorldIndex < mainMenuScrollOffset) {
            mainMenuScrollOffset = selectedWorldIndex;
        } else if (selectedWorldIndex >= mainMenuScrollOffset + GameConfig.WORLD_MENU_VISIBLE_ROWS) {
            mainMenuScrollOffset = selectedWorldIndex - GameConfig.WORLD_MENU_VISIBLE_ROWS + 1;
        }
        clampMainMenuScrollOffset();
    }

    private void clampMainMenuScrollOffset() {
        int maxOffset = Math.max(0, availableWorlds.size() - GameConfig.WORLD_MENU_VISIBLE_ROWS);
        mainMenuScrollOffset = Math.max(0, Math.min(maxOffset, mainMenuScrollOffset));
    }

    private static int compareWorldInfoByName(WorldInfo left, WorldInfo right) {
        int nameCompare = compareNatural(left.name, right.name);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return left.directory.toString().compareToIgnoreCase(right.directory.toString());
    }

    private static int compareNatural(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        int ia = 0;
        int ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia);
            char cb = b.charAt(ib);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int startA = ia;
                int startB = ib;
                while (ia < a.length() && Character.isDigit(a.charAt(ia))) {
                    ia++;
                }
                while (ib < b.length() && Character.isDigit(b.charAt(ib))) {
                    ib++;
                }
                String numberA = a.substring(startA, ia);
                String numberB = b.substring(startB, ib);
                String trimmedA = trimLeadingZeroes(numberA);
                String trimmedB = trimLeadingZeroes(numberB);
                int lengthCompare = Integer.compare(trimmedA.length(), trimmedB.length());
                if (lengthCompare != 0) {
                    return lengthCompare;
                }
                int valueCompare = trimmedA.compareTo(trimmedB);
                if (valueCompare != 0) {
                    return valueCompare;
                }
                int zeroCompare = Integer.compare(numberA.length(), numberB.length());
                if (zeroCompare != 0) {
                    return zeroCompare;
                }
                continue;
            }
            int charCompare = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
            if (charCompare != 0) {
                return charCompare;
            }
            ia++;
            ib++;
        }
        return Integer.compare(a.length(), b.length());
    }

    private static String trimLeadingZeroes(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    private WorldMetadata readWorldMetadata(Path directory) throws IOException {
        Path levelPath = directory.resolve(GameConfig.SAVE_LEVEL_FILE);
        if (Files.isRegularFile(levelPath)) {
            return readLevelMetadata(levelPath);
        }

        Path seedPath = directory.resolve(GameConfig.SAVE_METADATA_FILE);
        String[] lines = readUtf8(seedPath).split("\\R");
        long seed = lines.length == 0 ? 0L : Long.parseUnsignedLong(lines[0].trim(), 16);
        int gameMode = 0;
        int difficulty = 2;
        TerrainPreset terrainPreset = TerrainPreset.LEGACY;
        String allowCheatsValue = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("mode=")) {
                gameMode = clamp(Integer.parseInt(line.substring(5)), 0, GameConfig.GAME_MODE_OPTIONS.length - 1);
            } else if (line.startsWith("difficulty=")) {
                difficulty = clamp(Integer.parseInt(line.substring(11)), 0, GameConfig.DIFFICULTY_OPTIONS.length - 1);
            } else if (line.startsWith("worldType=")) {
                terrainPreset = TerrainPreset.fromMetadata(line.substring(10));
            } else if (line.startsWith("terrainPreset=")) {
                terrainPreset = TerrainPreset.fromMetadata(line.substring(14));
            } else if (line.startsWith("allowCheats=")) {
                allowCheatsValue = line.substring("allowCheats=".length());
            }
        }
        return new WorldMetadata(seed, gameMode, difficulty, terrainPreset,
            WorldAccessRules.allowCheatsFromMetadata(allowCheatsValue, gameMode));
    }

    private String readUtf8(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private void writeUtf8(Path path, String text) throws IOException {
        AtomicFiles.writeUtf8(path, text);
    }

    private WorldMetadata readLevelMetadata(Path levelPath) throws IOException {
        String json = readUtf8(levelPath);
        String seedText = jsonValue(json, "seed");
        long seed = parseStoredSeed(seedText);
        int gameMode = clamp(jsonIntValue(json, "gameMode", 0), 0, GameConfig.GAME_MODE_OPTIONS.length - 1);
        int difficulty = clamp(jsonIntValue(json, "difficulty", 2), 0, GameConfig.DIFFICULTY_OPTIONS.length - 1);
        TerrainPreset terrainPreset = TerrainPreset.fromMetadata(jsonValue(json, "worldType"));
        boolean allowCheats = WorldAccessRules.allowCheatsFromMetadata(jsonValue(json, "allowCheats"), gameMode);
        return new WorldMetadata(seed, gameMode, difficulty, terrainPreset, allowCheats);
    }

    private void writeWorldMetadata(Path directory, long seed, int gameMode, boolean allowCheats, int difficulty, TerrainPreset terrainPreset) throws IOException {
        Files.createDirectories(directory);
        Files.createDirectories(directory.resolve(GameConfig.SAVE_REGION_DIRECTORY));
        int clampedGameMode = clamp(gameMode, 0, GameConfig.GAME_MODE_OPTIONS.length - 1);
        int clampedDifficulty = clamp(difficulty, 0, GameConfig.DIFFICULTY_OPTIONS.length - 1);
        TerrainPreset storedPreset = terrainPreset == null ? TerrainPreset.LEGACY : terrainPreset;
        String seedHex = Long.toUnsignedString(seed, 16);
        String levelJson = "{"
            + System.lineSeparator() + "  \"version\": 1,"
            + System.lineSeparator() + "  \"seed\": \"" + jsonEscape(seedHex) + "\","
            + System.lineSeparator() + "  \"gameMode\": " + clampedGameMode + ","
            + System.lineSeparator() + "  \"allowCheats\": " + allowCheats + ","
            + System.lineSeparator() + "  \"difficulty\": " + clampedDifficulty + ","
            + System.lineSeparator() + "  \"worldType\": \"" + jsonEscape(storedPreset.metadataId()) + "\","
            + System.lineSeparator() + "  \"minY\": " + GameConfig.WORLD_MIN_Y + ","
            + System.lineSeparator() + "  \"height\": " + GameConfig.WORLD_HEIGHT + ","
            + System.lineSeparator() + "  \"seaLevel\": " + GameConfig.SEA_LEVEL + ","
            + System.lineSeparator() + "  \"createdWith\": \"TinyCraft mcrx-1\""
            + System.lineSeparator() + "}"
            + System.lineSeparator();
        writeUtf8(directory.resolve(GameConfig.SAVE_LEVEL_FILE), levelJson);

        String metadata = seedHex
            + System.lineSeparator() + "mode=" + clampedGameMode
            + System.lineSeparator() + "allowCheats=" + allowCheats
            + System.lineSeparator() + "difficulty=" + clampedDifficulty
            + System.lineSeparator() + "worldType=" + storedPreset.metadataId()
            + System.lineSeparator();
        writeUtf8(directory.resolve(GameConfig.SAVE_METADATA_FILE), metadata);
    }

    private void saveCurrentWorldMetadata() {
        WorldInfo worldInfo = selectedWorld();
        if (worldInfo == null) {
            return;
        }
        try {
            writeWorldMetadata(worldInfo.directory, worldInfo.seed, currentGameModeIndex(), currentWorldAllowCheats, currentWorldDifficulty, worldInfo.terrainPreset);
        } catch (IOException exception) {
            System.err.println("Failed to save world metadata for " + worldInfo.directory + ": " + exception);
        }
    }

    private long parseStoredSeed(String seedText) {
        if (seedText == null || seedText.trim().isEmpty()) {
            return 0L;
        }
        String trimmed = seedText.trim();
        try {
            return Long.parseUnsignedLong(trimmed, 16);
        } catch (NumberFormatException ignored) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignoredAgain) {
                return trimmed.hashCode();
            }
        }
    }

    private int jsonIntValue(String json, String key, int defaultValue) {
        String value = jsonValue(json, key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String jsonValue(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) {
            return null;
        }
        if (json.charAt(valueStart) == '"') {
            StringBuilder value = new StringBuilder();
            boolean escaped = false;
            for (int i = valueStart + 1; i < json.length(); i++) {
                char character = json.charAt(i);
                if (escaped) {
                    value.append(character);
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    return value.toString();
                } else {
                    value.append(character);
                }
            }
            return null;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char character = json.charAt(valueEnd);
            if (character == ',' || character == '}' || Character.isWhitespace(character)) {
                break;
            }
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd);
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void openCreateWorldScreen() {
        createWorldName = nextWorldName();
        createWorldSeed = "";
        createWorldGameMode = 0;
        createWorldAllowCheats = false;
        createWorldDifficulty = 2;
        createWorldTerrainPreset = TerrainPreset.NEW_WORLD_DEFAULT.createWorldOptionIndex();
        inputController.setActiveMenuTextField(0);
        mainMenuScreen = GameConfig.MENU_SCREEN_CREATE_WORLD;
        mainMenuSelection = 0;
    }

    private void openRenameWorldScreen() {
        WorldInfo worldInfo = selectedWorld();
        if (worldInfo == null) {
            return;
        }
        renameWorldName = worldInfo.name;
        inputController.setActiveMenuTextField(0);
        mainMenuScreen = GameConfig.MENU_SCREEN_RENAME_WORLD;
        mainMenuSelection = 0;
    }

    private void openMultiplayerScreen() {
        multiplayerName = localProfile.name;
        if (multiplayerHost == null || multiplayerHost.trim().isEmpty()) {
            multiplayerHost = "127.0.0.1";
        }
        if (multiplayerPort == null || multiplayerPort.trim().isEmpty()) {
            multiplayerPort = Integer.toString(MultiplayerProtocol.DEFAULT_PORT);
        }
        multiplayerStatus = multiplayer.status();
        lanOpenedFromPause = false;
        inputController.setActiveMenuTextField(0);
        mainMenuScreen = GameConfig.MENU_SCREEN_MULTIPLAYER;
        mainMenuSelection = 1;
    }

    private void activateMultiplayerOption() {
        saveMultiplayerProfileName();
        if (mainMenuSelection == 0) {
            lanGameMode = creativeMode ? 1 : (spectatorMode ? 2 : 0);
            lanAllowCheats = creativeMode || spectatorMode;
            lanOpenedFromPause = false;
            mainMenuScreen = GameConfig.MENU_SCREEN_OPEN_LAN;
            mainMenuSelection = 0;
            inputController.setActiveMenuTextField(-1);
            return;
        }
        if (mainMenuSelection == 1) {
            resetClientMirrorWorldState();
            multiplayer.connect(multiplayerHost.trim(), parseMultiplayerPort());
            return;
        }
        mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
        mainMenuSelection = 1;
        inputController.setActiveMenuTextField(-1);
    }

    private void activateOpenLanOption() {
        if (mainMenuSelection == 0) {
            if (!worldLoaded) {
                if (!createFreshLanHostWorld()) {
                    multiplayerStatus = "Host failed: cannot create LAN world";
                    return;
                }
            }
            setGameMode(lanGameMode);
            multiplayer.startHost(world, player, parseMultiplayerPort(), lanAllowCheats);
            mainMenuActive = false;
            paused = false;
            inventoryOpen = false;
            deathScreenActive = false;
            lanOpenedFromPause = false;
            inputController.setActiveMenuTextField(-1);
            inputController.updateCursorMode();
            return;
        }
        closeOpenLanMenu();
    }

    private void closeOpenLanMenu() {
        inputController.setActiveMenuTextField(-1);
        if (lanOpenedFromPause) {
            mainMenuActive = false;
            paused = true;
            mainMenuScreen = GameConfig.MENU_SCREEN_MAIN;
            pauseSelection = 2;
            lanOpenedFromPause = false;
            inputController.updateCursorMode();
            return;
        }
        mainMenuScreen = GameConfig.MENU_SCREEN_MULTIPLAYER;
        mainMenuSelection = 0;
    }

    private void resetClientMirrorWorldState() {
        renderer.clearWorldMeshes();
        world.clearRuntimeWorld();
        multiplayerWorldLoading = false;
        worldLoaded = false;
        loadedWorldName = null;
        hoveredBlock = null;
        activeChestContainer = null;
        activeFurnace = null;
        inventoryOpen = false;
        inventoryScreenMode = GameConfig.INVENTORY_SCREEN_PLAYER;
        resetMovement();
        resetBreakingProgress();
        resetRenderInterpolation();
        playerController.resetPlayerFluidState();
    }

    private boolean createFreshLanHostWorld() {
        if (worldLoaded) {
            return true;
        }
        String worldName = uniqueWorldName("world_lan_" + System.currentTimeMillis());
        long seed = System.nanoTime() ^ new Random().nextLong();
        TerrainPreset terrainPreset = TerrainPreset.NEW_WORLD_DEFAULT;
        Path worldDirectory = RuntimePaths.resolve(GameConfig.SAVE_ROOT_DIRECTORY, worldName);
        try {
            writeWorldMetadata(worldDirectory, seed, lanGameMode, lanAllowCheats, 2, terrainPreset);
        } catch (IOException ignored) {
            return false;
        }
        renderer.clearWorldMeshes();
        world.clearRuntimeWorld();
        showLoadingScreen(loadingTerrainText(), worldName);
        long worldGenerationStart = System.nanoTime();
        world.configureWorld(worldDirectory, seed, terrainPreset);
        world.initializeNoise();
        world.generateWorld();
        FrameProfiler.logTask("world.generateWorld", System.nanoTime() - worldGenerationStart);
        resetInventoryScreenStateForWorldLoad();
        inventory.clearAll();
        resetPlayerStateForNewWorld();
        world.placePlayerAtSpawn(player);
        currentWorldDifficulty = 2;
        currentWorldAllowCheats = lanAllowCheats;
        setGameMode(lanGameMode);
        world.prepareForPlayer(player);
        world.primeStreamingAround(player);
        warmupWorldStreaming(worldName);
        world.clearNetworkDirtyBlocks();
        renderer.buildAllChunkMeshes();
        loadedWorldName = worldName;
        worldLoaded = true;
        chat.clearMessages();
        deathScreenActive = false;
        inventoryDroppedOnDeath = false;
        player.health = Math.max(0.5, player.health);
        player.hunger = clamp(player.hunger, 0.0, GameConfig.MAX_HUNGER);
        player.verticalVelocity = 0.0;
        player.isGrounded = true;
        resetRenderInterpolation();
        playerController.resetPlayerDamageTimers();
        playerController.resetPlayerFluidState();
        resetMovement();
        resetBreakingProgress();
        syncPlayerModeState();
        syncSelectedHotbarItem();
        refreshAvailableWorlds();
        selectWorldByName(worldName);
        return true;
    }

    private void saveMultiplayerProfileName() {
        localProfile.setName(multiplayerName);
        multiplayerName = localProfile.name;
    }

    private int parseMultiplayerPort() {
        try {
            int parsed = Integer.parseInt(multiplayerPort.trim());
            return clamp(parsed, 1, 65535);
        } catch (Exception ignored) {
            multiplayerPort = Integer.toString(MultiplayerProtocol.DEFAULT_PORT);
            return MultiplayerProtocol.DEFAULT_PORT;
        }
    }

    private String loadingTerrainText() {
        return Settings.isRussian() ? "Генерация мира" : "Generating terrain";
    }

    private String paradiseTravelText() {
        return Settings.isRussian() ? "\u041f\u0435\u0440\u0435\u0445\u043e\u0434 \u043c\u0435\u0436\u0434\u0443 \u043c\u0438\u0440\u0430\u043c\u0438" : "Changing worlds";
    }

    private String buildingWorldText() {
        return Settings.isRussian() ? "Подготовка чанков" : "Building world";
    }

    private void createNewWorld(String requestedName, String requestedSeed, int gameMode, boolean allowCheats, int difficulty, int terrainPresetOption) {
        String worldName = uniqueWorldName(sanitizeWorldName(requestedName, nextWorldName()));
        long seed = parseSeed(requestedSeed);
        Path worldDirectory = RuntimePaths.resolve(GameConfig.SAVE_ROOT_DIRECTORY, worldName);
        try {
            writeWorldMetadata(worldDirectory, seed, gameMode, allowCheats, difficulty, TerrainPreset.fromCreateWorldOption(terrainPresetOption));
            currentWorldDifficulty = difficulty;
        } catch (IOException ignored) {
            return;
        }
        refreshAvailableWorlds();
        selectWorldByName(worldName);
    }

    private long parseSeed(String seedText) {
        String trimmed = seedText == null ? "" : seedText.trim();
        if (trimmed.isEmpty()) {
            return System.nanoTime() ^ new Random().nextLong();
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            return trimmed.hashCode();
        }
    }

    private void renameSelectedWorld(String requestedName) {
        WorldInfo worldInfo = selectedWorld();
        if (worldInfo == null) {
            return;
        }
        String newName = uniqueWorldName(sanitizeWorldName(requestedName, worldInfo.name), worldInfo.name);
        if (newName.equals(worldInfo.name)) {
            return;
        }
        Path target = worldInfo.directory.resolveSibling(newName);
        try {
            if (worldInfo.name.equals(loadedWorldName)) {
                syncPlayerModeState();
                saveCurrentWorldMetadata();
                world.savePlayerState(player, inventory);
                world.saveAllLoadedColumns();
                world.discardLoadedWorld();
                loadedWorldName = null;
                worldLoaded = false;
                hoveredBlock = null;
            }
            Files.move(worldInfo.directory, target);
        } catch (IOException ignored) {
            return;
        }
        refreshAvailableWorlds();
        selectWorldByName(newName);
    }

    private void deleteSelectedWorld() {
        WorldInfo worldInfo = selectedWorld();
        if (worldInfo == null) {
            return;
        }
        try {
            if (worldInfo.name.equals(loadedWorldName)) {
                world.discardLoadedWorld();
                loadedWorldName = null;
                worldLoaded = false;
                hoveredBlock = null;
            }
            deleteDirectory(worldInfo.directory);
        } catch (IOException exception) {
            System.err.println("Failed to delete world directory " + worldInfo.directory + ": " + exception);
        }
        refreshAvailableWorlds();
    }

    private long worldLastModified(Path directory) throws IOException {
        long lastModified = Files.getLastModifiedTime(directory).toMillis();
        Path levelPath = directory.resolve(GameConfig.SAVE_LEVEL_FILE);
        if (Files.exists(levelPath)) {
            lastModified = Math.max(lastModified, Files.getLastModifiedTime(levelPath).toMillis());
        }
        Path seedPath = directory.resolve(GameConfig.SAVE_METADATA_FILE);
        if (Files.exists(seedPath)) {
            lastModified = Math.max(lastModified, Files.getLastModifiedTime(seedPath).toMillis());
        }
        Path regionPath = directory.resolve(GameConfig.SAVE_REGION_DIRECTORY);
        if (Files.exists(regionPath)) {
            lastModified = Math.max(lastModified, Files.getLastModifiedTime(regionPath).toMillis());
        }
        Path chunksPath = directory.resolve(GameConfig.SAVE_CHUNKS_DIRECTORY);
        if (Files.exists(chunksPath)) {
            lastModified = Math.max(lastModified, Files.getLastModifiedTime(chunksPath).toMillis());
        }
        return lastModified;
    }

    private void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    System.err.println("Failed to delete world path " + path + ": " + exception);
                }
            });
        }
    }

    private String nextWorldName() {
        HashSet<String> names = new HashSet<>();
        for (WorldInfo worldInfo : availableWorlds) {
            names.add(worldInfo.name);
        }
        int index = 1;
        while (true) {
            String candidate = "World " + index;
            if (!names.contains(candidate)) {
                return candidate;
            }
            index++;
        }
    }

    private String sanitizeWorldName(String requestedName, String fallbackName) {
        String source = requestedName == null ? "" : requestedName.trim();
        if (source.isEmpty()) {
            source = fallbackName;
        }
        String cleaned = source.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        if (cleaned.isEmpty()) {
            cleaned = fallbackName;
        }
        return cleaned.length() > GameConfig.WORLD_MENU_NAME_LIMIT ? cleaned.substring(0, GameConfig.WORLD_MENU_NAME_LIMIT) : cleaned;
    }

    private String uniqueWorldName(String baseName) {
        return uniqueWorldName(baseName, null);
    }

    private String uniqueWorldName(String baseName, String allowedExistingName) {
        HashSet<String> names = new HashSet<>();
        for (WorldInfo worldInfo : availableWorlds) {
            if (!worldInfo.name.equals(allowedExistingName)) {
                names.add(worldInfo.name);
            }
        }
        if (!names.contains(baseName)) {
            return baseName;
        }
        int index = 2;
        while (true) {
            String candidate = baseName + " (" + index + ")";
            if (!names.contains(candidate)) {
                return candidate;
            }
            index++;
        }
    }

    private void selectWorldByName(String worldName) {
        for (int i = 0; i < availableWorlds.size(); i++) {
            if (availableWorlds.get(i).name.equals(worldName)) {
                selectedWorldIndex = i;
                keepSelectedWorldVisible();
                return;
            }
        }
    }

    private WorldInfo selectedWorld() {
        if (availableWorlds.isEmpty() || selectedWorldIndex < 0 || selectedWorldIndex >= availableWorlds.size()) {
            return null;
        }
        return availableWorlds.get(selectedWorldIndex);
    }

    private void respawnPlayer() {
        deathScreenActive = false;
        inventoryDroppedOnDeath = false;
        mainMenuActive = false;
        inputController.setGameModeSwitcherActive(false);
        inputController.setF3Held(false);
        paused = false;
        inventoryOpen = false;
        creativeMode = false;
        spectatorMode = false;
        playerController.setCreativeFlightEnabled(false);
        player.health = GameConfig.MAX_HEALTH;
        player.hunger = GameConfig.MAX_HUNGER;
        player.hungerDrainTimer = 0.0;
        player.hungerDamageTimer = 0.0;
        player.hungerRegenTimer = 0.0;
        player.hungerGraceRemaining = GameConfig.HUNGER_GRACE_SECONDS;
        if (player.hasCustomSpawn) {
            player.x = player.spawnX;
            player.y = player.spawnY;
            player.z = player.spawnZ;
        } else {
            world.placePlayerAtSpawn(player);
        }
        player.verticalVelocity = 0.0;
        player.isGrounded = true;
        player.yaw = PlayerState.DEFAULT_YAW;
        player.pitch = PlayerState.DEFAULT_PITCH;
        player.cameraBobPhase = 0.0;
        player.cameraBobAmount = 0.0;
        player.stepTimer = 0.0;
        playerController.resetPlayerFluidState();
        playerController.resetPlayerDamageTimers();
        hoveredBlock = null;
        playerController.resetLastCreativeJumpTapTime();
        resetMovement();
        resetBreakingProgress();
        syncPlayerModeState();
        syncSelectedHotbarItem();
        inputController.updateCursorMode();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double lerp(double start, double end, double amount) {
        return start + (end - start) * clamp(amount, 0.0, 1.0);
    }

    private double wrapAngle(double value) {
        while (value > Math.PI) {
            value -= Math.PI * 2.0;
        }
        while (value < -Math.PI) {
            value += Math.PI * 2.0;
        }
        return value;
    }

    private void cleanup() {
        closeContainerScreen();
        Settings.save(renderDistanceChunks, fovDegrees, maxFps);
        if (worldLoaded) {
            syncPlayerModeState();
            saveCurrentWorldMetadata();
            world.savePlayerState(player, inventory);
        }
        world.cleanup();
        renderer.cleanup();
        audio.cleanup();
        if (window != NULL) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private static final class WorldMetadata {
        final long seed;
        final int gameMode;
        final int difficulty;
        final TerrainPreset terrainPreset;
        final boolean allowCheats;

        WorldMetadata(long seed, int gameMode, int difficulty, TerrainPreset terrainPreset, boolean allowCheats) {
            this.seed = seed;
            this.gameMode = gameMode;
            this.difficulty = difficulty;
            this.terrainPreset = terrainPreset == null ? TerrainPreset.LEGACY : terrainPreset;
            this.allowCheats = allowCheats;
        }
    }
}
