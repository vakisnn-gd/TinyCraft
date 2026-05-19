import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex3d;

final class SkyRenderer {
    private final double[] starDirX = new double[GameConfig.STAR_COUNT];
    private final double[] starDirY = new double[GameConfig.STAR_COUNT];
    private final double[] starDirZ = new double[GameConfig.STAR_COUNT];
    private final double[] starSize = new double[GameConfig.STAR_COUNT];
    private final double[] starPhase = new double[GameConfig.STAR_COUNT];
    private double cloudDriftOffset;

    SkyRenderer() {
        initializeSkyDecor();
    }

    void render(PlayerState player, double timeOfDay, double deltaTime, float daylight,
                double renderCameraX, double renderCameraY, double renderCameraZ) {
        cloudDriftOffset = (cloudDriftOffset + deltaTime * GameConfig.CLOUD_DRIFT_SPEED)
            % (GameConfig.CLOUD_CELL_SIZE * 8192.0);

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        renderStars(player, timeOfDay, daylight, renderCameraX, renderCameraY, renderCameraZ);
        if (Settings.goodGraphics()) {
            renderCloudLayer(player, daylight, renderCameraX, renderCameraY, renderCameraZ);
        }
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderStars(PlayerState player, double timeOfDay, float daylight,
                             double renderCameraX, double renderCameraY, double renderCameraZ) {
        float starVisibility = clampColor((0.42f - daylight) / 0.34f);
        if (starVisibility <= 0.01f) {
            return;
        }

        double skyRotation = timeOfDay * Math.PI * 2.0;
        double rotationCos = Math.cos(skyRotation);
        double rotationSin = Math.sin(skyRotation);
        double anchorX = player.x - renderCameraX;
        double anchorY = player.y + GameConfig.EYE_HEIGHT - renderCameraY;
        double anchorZ = player.z - renderCameraZ;

        glBegin(GL_QUADS);
        for (int i = 0; i < GameConfig.STAR_COUNT; i++) {
            double rotatedY = starDirY[i] * rotationCos - starDirZ[i] * rotationSin;
            double rotatedZ = starDirY[i] * rotationSin + starDirZ[i] * rotationCos;
            if (rotatedY <= 0.02) {
                continue;
            }

            double twinkle = 0.70 + 0.30 * Math.sin(timeOfDay * Math.PI * 48.0 + starPhase[i]);
            float alpha = clampColor((float) (starVisibility * twinkle));
            if (alpha <= 0.02f) {
                continue;
            }

            double x = anchorX + starDirX[i] * GameConfig.STAR_RADIUS;
            double y = anchorY + rotatedY * GameConfig.STAR_RADIUS;
            double z = anchorZ + rotatedZ * GameConfig.STAR_RADIUS;
            double size = starSize[i];

            glColor4f(0.86f, 0.91f, 1.0f, alpha);
            glVertex3d(x - size, y - size, z);
            glVertex3d(x - size, y + size, z);
            glVertex3d(x + size, y + size, z);
            glVertex3d(x + size, y - size, z);
        }
        glEnd();
    }

    private void renderCloudLayer(PlayerState player, float daylight,
                                  double renderCameraX, double renderCameraY, double renderCameraZ) {
        float daylightFactor = 0.35f + daylight * 0.65f;
        float alpha = 0.14f + daylight * 0.20f;
        double anchorX = player.x;
        double anchorZ = player.z;
        int baseCellX = (int) Math.floor((anchorX + cloudDriftOffset) / GameConfig.CLOUD_CELL_SIZE);
        int baseCellZ = (int) Math.floor(anchorZ / GameConfig.CLOUD_CELL_SIZE);

        for (int cellX = baseCellX - GameConfig.CLOUD_RENDER_RADIUS; cellX <= baseCellX + GameConfig.CLOUD_RENDER_RADIUS; cellX++) {
            for (int cellZ = baseCellZ - GameConfig.CLOUD_RENDER_RADIUS; cellZ <= baseCellZ + GameConfig.CLOUD_RENDER_RADIUS; cellZ++) {
                double density = hash01(cellX, cellZ, 11);
                if (density < 0.58) {
                    continue;
                }

                double centerX = cellX * GameConfig.CLOUD_CELL_SIZE - cloudDriftOffset + (hash01(cellX, cellZ, 17) - 0.5) * 4.0 - renderCameraX;
                double centerZ = cellZ * GameConfig.CLOUD_CELL_SIZE + (hash01(cellX, cellZ, 23) - 0.5) * 4.0 - renderCameraZ;
                double centerY = GameConfig.CLOUD_LAYER_BASE_HEIGHT + hash01(cellX, cellZ, 29) * 5.0 - renderCameraY;
                double width = 18.0 + hash01(cellX, cellZ, 31) * 10.0;
                double depth = 12.0 + hash01(cellX, cellZ, 37) * 8.0;
                double thickness = 2.4 + hash01(cellX, cellZ, 41) * 2.4;

                renderCloudPuff(
                    centerX - width * 0.5,
                    centerY,
                    centerZ - depth * 0.5,
                    centerX + width * 0.5,
                    centerY + thickness,
                    centerZ + depth * 0.5,
                    daylightFactor,
                    alpha
                );

                if (density > 0.72) {
                    double offsetX = (hash01(cellX, cellZ, 43) - 0.5) * width * 0.24;
                    double offsetZ = (hash01(cellX, cellZ, 47) - 0.5) * depth * 0.24;
                    renderCloudPuff(
                        centerX - width * 0.30 + offsetX,
                        centerY + 0.3,
                        centerZ - depth * 0.28 + offsetZ,
                        centerX + width * 0.30 + offsetX,
                        centerY + thickness + 0.5,
                        centerZ + depth * 0.28 + offsetZ,
                        daylightFactor,
                        alpha * 0.86f
                    );
                }
            }
        }
    }

    private void renderCloudPuff(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ,
                                 float daylightFactor, float alpha) {
        float top = clampColor(0.84f * daylightFactor + 0.10f);
        float side = clampColor(top * 0.93f);
        float bottom = clampColor(top * 0.82f);

        glBegin(GL_QUADS);
        glColor4f(top, top, top, alpha);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, maxY, minZ);

        glColor4f(bottom, bottom, bottom, alpha * 0.92f);
        glVertex3d(minX, minY, minZ);
        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, minY, maxZ);
        glVertex3d(minX, minY, maxZ);

        glColor4f(side, side, side, alpha);
        glVertex3d(minX, minY, minZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, minY, minZ);

        glVertex3d(maxX, minY, maxZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, minY, maxZ);

        glColor4f(side * 0.97f, side * 0.97f, side, alpha);
        glVertex3d(minX, minY, maxZ);
        glVertex3d(minX, maxY, maxZ);
        glVertex3d(minX, maxY, minZ);
        glVertex3d(minX, minY, minZ);

        glVertex3d(maxX, minY, minZ);
        glVertex3d(maxX, maxY, minZ);
        glVertex3d(maxX, maxY, maxZ);
        glVertex3d(maxX, minY, maxZ);
        glEnd();
    }

    private void initializeSkyDecor() {
        for (int i = 0; i < GameConfig.STAR_COUNT; i++) {
            double azimuth = hash01(i, 0, 53) * Math.PI * 2.0;
            double z = hash01(i, 0, 59) * 2.0 - 1.0;
            double radial = Math.sqrt(Math.max(0.0, 1.0 - z * z));
            starDirX[i] = Math.cos(azimuth) * radial;
            starDirY[i] = 0.18 + hash01(i, 0, 61) * 0.82;
            starDirZ[i] = Math.sin(azimuth) * radial;
            double normalize = Math.sqrt(starDirX[i] * starDirX[i] + starDirY[i] * starDirY[i] + starDirZ[i] * starDirZ[i]);
            if (normalize > 1.0e-8) {
                starDirX[i] /= normalize;
                starDirY[i] /= normalize;
                starDirZ[i] /= normalize;
            }
            starSize[i] = GameConfig.STAR_BASE_SIZE + hash01(i, 0, 67) * 0.75;
            starPhase[i] = hash01(i, 0, 71) * Math.PI * 2.0;
        }
    }

    private double hash01(int x, int z, int salt) {
        long value = 0x9E3779B97F4A7C15L;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) z * 0x85157AF5L;
        value ^= (long) salt * 0x94D049BB133111EBL;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        long mantissa = value & ((1L << 53) - 1);
        return mantissa / (double) (1L << 53);
    }

    private float clampColor(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
