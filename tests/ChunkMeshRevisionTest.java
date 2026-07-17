import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ChunkMeshRevisionTest {
    @Test
    public void meshRadiusExpandsAfterCurrentRingIsReadyWithoutPlayerMovement() {
        OpenGlRenderer.MeshBuildRadiusController controller = new OpenGlRenderer.MeshBuildRadiusController(12);

        assertTrue(controller.update(32, false) == 12);
        assertTrue(controller.update(32, true) == 13);
        assertTrue(controller.update(32, false) == 13);
        assertTrue(controller.update(32, true) == 14);
    }

    @Test
    public void neighborInvalidationRejectsBuildCapturedBeforeTheChange() {
        OpenGlRenderer.MeshBuildRevisionTracker revisions = new OpenGlRenderer.MeshBuildRevisionTracker();
        long meshKey = 42L;

        int capturedRevision = revisions.current(meshKey);
        assertTrue(revisions.isCurrent(meshKey, capturedRevision));

        revisions.invalidate(meshKey);

        assertFalse(revisions.isCurrent(meshKey, capturedRevision));
        assertTrue(revisions.isCurrent(meshKey, revisions.current(meshKey)));
    }

    @Test
    public void forgottenMeshStartsWithCleanRevision() {
        OpenGlRenderer.MeshBuildRevisionTracker revisions = new OpenGlRenderer.MeshBuildRevisionTracker();
        long meshKey = 99L;

        revisions.invalidate(meshKey);
        revisions.forget(meshKey);

        assertTrue(revisions.isCurrent(meshKey, 0));
    }

    @Test
    public void cornerBlockInvalidatesDiagonalChunkSections() {
        Set<String> affected = new HashSet<>();
        int blockY = GameConfig.sectionYForIndex(2) + GameConfig.CHUNK_SIZE - 1;

        ChunkSectionInvalidation.aroundBlock(
            GameConfig.CHUNK_SIZE - 1,
            blockY,
            GameConfig.CHUNK_SIZE - 1,
            (chunkX, chunkY, chunkZ) -> affected.add(chunkX + ":" + chunkY + ":" + chunkZ)
        );

        assertEquals(8, affected.size());
        assertTrue(affected.contains("0:2:0"));
        assertTrue(affected.contains("1:2:1"));
        assertTrue(affected.contains("1:3:1"));
    }

    @Test
    public void interiorBlockInvalidatesOnlyItsOwnSection() {
        Set<String> affected = new HashSet<>();
        int blockY = GameConfig.sectionYForIndex(2) + 3;

        ChunkSectionInvalidation.aroundBlock(
            3,
            blockY,
            3,
            (chunkX, chunkY, chunkZ) -> affected.add(chunkX + ":" + chunkY + ":" + chunkZ)
        );

        assertEquals(1, affected.size());
        assertTrue(affected.contains("0:2:0"));
    }

    @Test
    public void opaqueRegionPackingPreservesLastChunkCoordinatesAndAo() {
        int localPackedX = 520;
        int localPackedY = 264;
        int localPackedZ = 520;
        int ao = 3;
        int chunkPositionAndAo = localPackedX
            | (localPackedY << 10)
            | (localPackedZ << 20)
            | (ao << 30);
        int lastChunkOffset = 7 * GameConfig.CHUNK_SIZE * 32;

        long regionPosition = OpenGlRenderer.packOpaqueRegionPosition(
            chunkPositionAndAo,
            lastChunkOffset,
            lastChunkOffset
        );
        int low = (int) regionPosition;
        int high = (int) (regionPosition >>> 32);

        assertEquals(localPackedX + lastChunkOffset, low & 8191);
        assertEquals(localPackedY, (low >>> 13) & 1023);
        assertEquals(localPackedZ + lastChunkOffset, ((low >>> 23) & 511) | ((high & 15) << 9));
        assertEquals(ao, (high >>> 4) & 3);
    }

    @Test
    public void chunkOriginStaysCameraRelativeAtOneHundredMillionBlocks() {
        int chunkX = Math.floorDiv(100_000_007, GameConfig.CHUNK_SIZE);
        int negativeChunkX = Math.floorDiv(-100_000_007, GameConfig.CHUNK_SIZE);

        assertEquals(-7.25, OpenGlRenderer.cameraRelativeChunkOrigin(chunkX, 100_000_007.25), 0.0);
        assertEquals(-8.75, OpenGlRenderer.cameraRelativeChunkOrigin(negativeChunkX, -100_000_007.25), 0.0);
    }

    @Test
    public void vertexShaderUsesWideRegionVertexLayout() throws Exception {
        String shader = new String(Files.readAllBytes(Paths.get("ChunkShader.vsh")), StandardCharsets.UTF_8);

        assertTrue(shader.contains("layout(location = 0) in uvec3 aPackedVertex"));
        assertTrue(shader.contains("uint xi = low & 8191u"));
        assertTrue(shader.contains("((high & 15u) << 9)"));
        assertTrue(shader.contains("(aPackedVertex.y >> 4) & 3u"));
    }

    @Test
    public void aquaticPlantsKeepTheirWaterLayerInTheMesh() {
        assertTrue(OpenGlRenderer.isWaterloggedPlantForMesh(GameConfig.SEAGRASS));
        assertTrue(OpenGlRenderer.isWaterloggedPlantForMesh(GameConfig.KELP));
        assertFalse(OpenGlRenderer.isWaterloggedPlantForMesh(GameConfig.TALL_GRASS));
    }
}
