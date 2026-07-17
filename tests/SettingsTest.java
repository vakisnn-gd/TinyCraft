import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsTest {
    @Test
    public void fpsSliderHasVsyncAndUnlimitedEndpoints() {
        assertEquals(Settings.FPS_VSYNC, Settings.maxFpsFromSliderPercent(0.0));
        assertEquals(30, Settings.maxFpsFromSliderPercent(1.0 / 23.0));
        assertEquals(240, Settings.maxFpsFromSliderPercent(22.0 / 23.0));
        assertEquals(Settings.FPS_UNLIMITED, Settings.maxFpsFromSliderPercent(1.0));
    }

    @Test
    public void keyboardFpsChangesCrossSpecialEndpoints() {
        assertEquals(30, Settings.nudgeMaxFps(Settings.FPS_VSYNC, 1));
        assertEquals(Settings.FPS_VSYNC, Settings.nudgeMaxFps(30, -1));
        assertEquals(150, Settings.nudgeMaxFps(144, 1));
        assertEquals(140, Settings.nudgeMaxFps(144, -1));
        assertEquals(Settings.FPS_UNLIMITED, Settings.nudgeMaxFps(240, 1));
        assertEquals(240, Settings.nudgeMaxFps(Settings.FPS_UNLIMITED, -1));
    }

    @Test
    public void automaticGuiScaleUsesCurrentFramebufferSize() {
        int previousGuiScale = Settings.guiScale;
        try {
            Settings.guiScale = 0;
            assertEquals(1.34f, Settings.guiScaleMultiplier(1280, 720), 0.001f);

            Settings.guiScale = 4;
            assertEquals(1.18f, Settings.guiScaleMultiplier(640, 480), 0.001f);
        } finally {
            Settings.guiScale = previousGuiScale;
        }
    }

    @Test
    public void pauseWorldMenusUseWorldBlurWhileMainMenuUsesPanorama() {
        assertTrue(OpenGlRenderer.shouldBlurWorldBackground(
            true, false, GameConfig.MENU_SCREEN_MAIN, false));
        assertTrue(OpenGlRenderer.shouldBlurWorldBackground(
            false, true, GameConfig.MENU_SCREEN_OPTIONS, true));
        assertTrue(OpenGlRenderer.shouldBlurWorldBackground(
            false, true, GameConfig.MENU_SCREEN_OPEN_LAN, true));
        assertFalse(OpenGlRenderer.shouldBlurWorldBackground(
            false, true, GameConfig.MENU_SCREEN_OPTIONS, false));

        assertTrue(OpenGlRenderer.shouldRenderMenuPanorama(
            true, GameConfig.MENU_SCREEN_MAIN, false));
        assertFalse(OpenGlRenderer.shouldRenderMenuPanorama(
            true, GameConfig.MENU_SCREEN_OPTIONS, true));
        assertFalse(OpenGlRenderer.shouldRenderMenuPanorama(
            true, GameConfig.MENU_SCREEN_OPEN_LAN, true));
        assertTrue(OpenGlRenderer.shouldRenderMenuPanorama(
            true, GameConfig.MENU_SCREEN_OPEN_LAN, false));
        assertTrue(OpenGlRenderer.shouldRenderMenuPanorama(
            true, GameConfig.MENU_SCREEN_DISCONNECTED, true));
        assertFalse(OpenGlRenderer.shouldRenderMenuPanorama(
            false, GameConfig.MENU_SCREEN_MAIN, false));

        assertTrue(OpenGlRenderer.shouldPrepareWorldMeshes(false, false));
        assertTrue(OpenGlRenderer.shouldPrepareWorldMeshes(true, true));
        assertFalse(OpenGlRenderer.shouldPrepareWorldMeshes(true, false));

        assertTrue(OpenGlRenderer.shouldUsePanoramaFallbackForWorldBlur(true, false, true));
        assertFalse(OpenGlRenderer.shouldUsePanoramaFallbackForWorldBlur(true, true, true));
        assertFalse(OpenGlRenderer.shouldUsePanoramaFallbackForWorldBlur(true, false, false));
    }
}
