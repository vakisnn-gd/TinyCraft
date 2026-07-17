import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DebugInfoTest {
    private static final double EPSILON = 0.000001;

    @Test
    public void frameTimeAndWarningsUseStableThresholds() {
        DebugInfo info = new DebugInfo();
        assertEquals(20.0, DebugInfo.frameTimeMs(50.0), EPSILON);
        assertEquals(0.0, DebugInfo.frameTimeMs(0.0), EPSILON);

        info.fps = DebugInfo.LOW_FPS_THRESHOLD - 0.1;
        assertTrue(info.hasLowFps());
        info.fps = DebugInfo.LOW_FPS_THRESHOLD;
        assertFalse(info.hasLowFps());

        info.usedMemoryMiB = 850;
        info.maxMemoryMiB = 1000;
        assertTrue(info.hasHighMemoryUsage());
        info.usedMemoryMiB = 849;
        assertFalse(info.hasHighMemoryUsage());
    }

    @Test
    public void slowDataRefreshesOncePerSecondOrAfterChangingChunkSection() {
        DebugInfo info = new DebugInfo();
        long start = 5_000_000_000L;

        assertTrue(info.shouldRefreshSlow(start, 10, 64, -3));
        assertFalse(info.shouldRefreshSlow(start + DebugInfo.SLOW_REFRESH_NANOS - 1, 10, 64, -3));
        assertTrue(info.shouldRefreshSlow(start + DebugInfo.SLOW_REFRESH_NANOS, 10, 64, -3));
        assertTrue(info.shouldRefreshSlow(start + DebugInfo.SLOW_REFRESH_NANOS + 1, 11, 64, -3));
    }

    @Test
    public void approximateLightDistinguishesSkyFromUnderground() {
        assertEquals(15, DebugInfo.approximateLightLevel(1.0f, true));
        assertEquals(3, DebugInfo.approximateLightLevel(1.0f, false));
        assertEquals(0, DebugInfo.approximateLightLevel(0.0f, true));
    }

    @Test
    public void overlayScaleFollowsInventoryScaleAndScreenWidth() {
        assertTrue(DebugInfo.overlayScale(2.0f, 1920) > DebugInfo.overlayScale(1.0f, 1920));
        assertEquals(0.72, DebugInfo.overlayScale(4.0f, 500), EPSILON);
        assertEquals("0.2.1", GameConfig.VERSION);
    }

    @Test
    public void alignsDebugTextAgainstRequestedPanelEdge() {
        assertEquals(110.0, DebugInfo.alignedTextX(100.0f, 300.0f, 10.0f, 80.0f, false), EPSILON);
        assertEquals(310.0, DebugInfo.alignedTextX(100.0f, 300.0f, 10.0f, 80.0f, true), EPSILON);
    }
}
