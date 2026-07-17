import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

final class InputController {
    interface Host {
        boolean shouldReleaseCursor();
    }

    interface CursorPositionHandler {
        void handle(double x, double y);
    }

    interface FramebufferSizeHandler {
        void handle(int width, int height);
    }

    interface ScrollHandler {
        void handle(double xOffset, double yOffset);
    }

    interface CharacterHandler {
        void handle(int codepoint);
    }

    interface KeyHandler {
        void handle(int key, int scancode, int action, int mods);
    }

    interface MouseButtonHandler {
        void handle(int button, int action, int mods);
    }

    private final PlayerController playerController;
    private final PlayerState player;
    private final PlayerInventory inventory;
    private final VoxelWorld world;
    private final Host host;

    private long window;
    private double mouseX;
    private double mouseY;
    private double lastMouseX;
    private double lastMouseY;
    private boolean mouseInitialized;
    private boolean leftMouseHeld;
    private boolean leftMousePressQueued;
    private boolean tabPlayerListHeld;
    private boolean f3Held;
    private boolean f3ComboConsumed;
    private boolean gameModeSwitcherActive;
    private int gameModeSelection;
    private int creativeTab;
    private int creativeScrollOffset;
    private int activeMenuTextField = -1;
    private CursorPositionHandler cursorPositionHandler;
    private FramebufferSizeHandler framebufferSizeHandler;
    private ScrollHandler scrollHandler;
    private CharacterHandler characterHandler;
    private KeyHandler keyHandler;
    private MouseButtonHandler mouseButtonHandler;

    InputController(PlayerController playerController, PlayerState player, PlayerInventory inventory, VoxelWorld world, Host host) {
        this.playerController = playerController;
        this.player = player;
        this.inventory = inventory;
        this.world = world;
        this.host = host;
    }

    void setupCallbacks(long window) {
        this.window = window;
        glfwSetCursorPosCallback(window, (handle, x, y) -> {
            mouseX = x;
            mouseY = y;
            if (cursorPositionHandler != null) {
                cursorPositionHandler.handle(x, y);
            }
        });
        glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
            if (framebufferSizeHandler != null) {
                framebufferSizeHandler.handle(width, height);
            }
        });
        glfwSetScrollCallback(window, (handle, xOffset, yOffset) -> {
            if (scrollHandler != null) {
                scrollHandler.handle(xOffset, yOffset);
            }
        });
        glfwSetCharCallback(window, (handle, codepoint) -> {
            if (characterHandler != null) {
                characterHandler.handle(codepoint);
            }
        });
        glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
            if (keyHandler != null) {
                keyHandler.handle(key, scancode, action, mods);
            }
        });
        glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> {
            if (mouseButtonHandler != null) {
                mouseButtonHandler.handle(button, action, mods);
            }
        });
    }

    void setCursorPositionHandler(CursorPositionHandler cursorPositionHandler) {
        this.cursorPositionHandler = cursorPositionHandler;
    }

    void setFramebufferSizeHandler(FramebufferSizeHandler framebufferSizeHandler) {
        this.framebufferSizeHandler = framebufferSizeHandler;
    }

    void setScrollHandler(ScrollHandler scrollHandler) {
        this.scrollHandler = scrollHandler;
    }

    void setCharacterHandler(CharacterHandler characterHandler) {
        this.characterHandler = characterHandler;
    }

    void setKeyHandler(KeyHandler keyHandler) {
        this.keyHandler = keyHandler;
    }

    void setMouseButtonHandler(MouseButtonHandler mouseButtonHandler) {
        this.mouseButtonHandler = mouseButtonHandler;
    }

    void updateCursorMode() {
        int cursorMode = host.shouldReleaseCursor() ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_DISABLED;
        glfwSetInputMode(window, GLFW_CURSOR, cursorMode);
        mouseInitialized = false;
    }

    void returnCursorToInventory() {
        ItemStack cursor = inventory.getCursorStack();
        if (!cursor.isEmpty()) {
            if (!inventory.addItem(cursor.itemId, cursor.count, cursor.durabilityDamage)) {
                world.spawnDroppedItem(cursor.itemId, cursor.count, cursor.durabilityDamage, player.x, player.y + 0.8, player.z);
            }
            cursor.clear();
        }
    }

    void scrollCreativeInventory(int direction) {
        if (direction == 0) {
            return;
        }
        int step = 9;
        creativeScrollOffset = clamp(creativeScrollOffset - direction * step, 0, creativeMaxScrollOffset());
    }

    void clampCreativeScrollOffset() {
        creativeScrollOffset = clamp(creativeScrollOffset, 0, creativeMaxScrollOffset());
    }

    int creativeMaxScrollOffset() {
        int tab = clamp(creativeTab, 0, InventoryItems.CREATIVE_TAB_INDICES.length - 1);
        int visibleSlots = 36;
        return Math.max(0, InventoryItems.CREATIVE_TAB_INDICES[tab].length - visibleSlots);
    }

    void resetMovementInput() {
        playerController.resetMovement();
        leftMouseHeld = false;
        leftMousePressQueued = false;
    }

    double getMouseX() {
        return mouseX;
    }

    double getMouseY() {
        return mouseY;
    }

    double getLastMouseX() {
        return lastMouseX;
    }

    void setLastMouseX(double lastMouseX) {
        this.lastMouseX = lastMouseX;
    }

    double getLastMouseY() {
        return lastMouseY;
    }

    void setLastMouseY(double lastMouseY) {
        this.lastMouseY = lastMouseY;
    }

    boolean isMouseInitialized() {
        return mouseInitialized;
    }

    void setMouseInitialized(boolean mouseInitialized) {
        this.mouseInitialized = mouseInitialized;
    }

    boolean isLeftMouseHeld() {
        return leftMouseHeld;
    }

    void setLeftMouseHeld(boolean leftMouseHeld) {
        this.leftMouseHeld = leftMouseHeld;
    }

    boolean isLeftMousePressQueued() {
        return leftMousePressQueued;
    }

    void setLeftMousePressQueued(boolean leftMousePressQueued) {
        this.leftMousePressQueued = leftMousePressQueued;
    }

    boolean isTabPlayerListHeld() {
        return tabPlayerListHeld;
    }

    void setTabPlayerListHeld(boolean tabPlayerListHeld) {
        this.tabPlayerListHeld = tabPlayerListHeld;
    }

    boolean isF3Held() {
        return f3Held;
    }

    void setF3Held(boolean f3Held) {
        this.f3Held = f3Held;
    }

    boolean isF3ComboConsumed() {
        return f3ComboConsumed;
    }

    void setF3ComboConsumed(boolean f3ComboConsumed) {
        this.f3ComboConsumed = f3ComboConsumed;
    }

    boolean isGameModeSwitcherActive() {
        return gameModeSwitcherActive;
    }

    void setGameModeSwitcherActive(boolean gameModeSwitcherActive) {
        this.gameModeSwitcherActive = gameModeSwitcherActive;
    }

    int getGameModeSelection() {
        return gameModeSelection;
    }

    void setGameModeSelection(int gameModeSelection) {
        this.gameModeSelection = gameModeSelection;
    }

    int getCreativeTab() {
        return creativeTab;
    }

    void setCreativeTab(int creativeTab) {
        this.creativeTab = creativeTab;
    }

    int getCreativeScrollOffset() {
        return creativeScrollOffset;
    }

    void setCreativeScrollOffset(int creativeScrollOffset) {
        this.creativeScrollOffset = creativeScrollOffset;
    }

    int getActiveMenuTextField() {
        return activeMenuTextField;
    }

    void setActiveMenuTextField(int activeMenuTextField) {
        this.activeMenuTextField = activeMenuTextField;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
