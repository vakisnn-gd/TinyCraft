import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.junit.Test;

public class PanoramaCaptureTest {
    private static final double EPSILON = 0.000001;

    @Test
    public void faceDirectionsMatchMenuCubeOrder() {
        assertDirection(0, 180.0, 0.0);
        assertDirection(1, 0.0, 0.0);
        assertDirection(2, -90.0, 90.0);
        assertDirection(3, -90.0, -90.0);
        assertDirection(4, -90.0, 0.0);
        assertDirection(5, 90.0, 0.0);
        assertEquals("panorama_5.png", OpenGlRenderer.panoramaFaceFileName(5));
    }

    @Test
    public void openGlPixelsAreFlippedIntoTopDownPngOrder() {
        ByteBuffer rgba = ByteBuffer.allocateDirect(2 * 2 * 4);
        putPixel(rgba, 255, 0, 0);
        putPixel(rgba, 0, 255, 0);
        putPixel(rgba, 0, 0, 255);
        putPixel(rgba, 255, 255, 255);

        BufferedImage image = OpenGlRenderer.panoramaImageFromRgba(rgba, 2);

        assertEquals(0x0000FF, image.getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0xFFFFFF, image.getRGB(1, 0) & 0xFFFFFF);
        assertEquals(0xFF0000, image.getRGB(0, 1) & 0xFFFFFF);
        assertEquals(0x00FF00, image.getRGB(1, 1) & 0xFFFFFF);
    }

    private static void assertDirection(int face, double yaw, double pitch) {
        assertEquals(yaw, Math.toDegrees(OpenGlRenderer.panoramaFaceYawRadians(face)), EPSILON);
        assertEquals(pitch, Math.toDegrees(OpenGlRenderer.panoramaFacePitchRadians(face)), EPSILON);
    }

    private static void putPixel(ByteBuffer buffer, int red, int green, int blue) {
        buffer.put((byte) red);
        buffer.put((byte) green);
        buffer.put((byte) blue);
        buffer.put((byte) 255);
    }
}
