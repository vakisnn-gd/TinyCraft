import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Test;

public class SurvivalGameplayTest {
    private static final double EPSILON = 0.0001;

    @Test
    public void zeroHealthRequiresDeathScreenInSurvival() {
        assertTrue(TinyCraft.shouldEnterSurvivalDeathScreen(false, false, false, 0.0));
        assertTrue(TinyCraft.shouldEnterSurvivalDeathScreen(false, false, false, -1.0));
        assertFalse(TinyCraft.shouldEnterSurvivalDeathScreen(true, false, false, 0.0));
        assertFalse(TinyCraft.shouldEnterSurvivalDeathScreen(false, true, false, 0.0));
        assertFalse(TinyCraft.shouldEnterSurvivalDeathScreen(false, false, true, 0.0));
        assertFalse(TinyCraft.shouldEnterSurvivalDeathScreen(false, false, false, 0.5));
    }

    @Test
    public void lethalControllerDamageNotifiesDeathScreen() throws Exception {
        VoxelWorld world = new VoxelWorld(1L);
        PlayerState player = new PlayerState();
        TestHost host = new TestHost();
        PlayerController controller = new PlayerController(world, player, null, host, () -> 0.0);
        Method applyPlayerDamage = PlayerController.class.getDeclaredMethod("applyPlayerDamage", double.class);
        applyPlayerDamage.setAccessible(true);

        try {
            player.health = 0.5;
            applyPlayerDamage.invoke(controller, 0.5);

            assertEquals(0.0, player.health, EPSILON);
            assertEquals(1, host.deathScreenCalls);
        } finally {
            world.cleanup();
        }
    }

    @Test
    public void hungerWaitsTwoMinutesThenDrainsEveryTwentySeconds() throws Exception {
        VoxelWorld world = new VoxelWorld(2L);
        PlayerState player = new PlayerState();
        TestHost host = new TestHost();
        PlayerController controller = new PlayerController(world, player, null, host, () -> 0.0);
        Method updatePlayerHunger = PlayerController.class.getDeclaredMethod("updatePlayerHunger", double.class, boolean.class);
        updatePlayerHunger.setAccessible(true);

        try {
            updatePlayerHunger.invoke(controller, GameConfig.HUNGER_GRACE_SECONDS, true);
            assertEquals(0.0, player.hungerGraceRemaining, EPSILON);
            assertEquals(GameConfig.MAX_HUNGER, player.hunger, EPSILON);

            updatePlayerHunger.invoke(controller, GameConfig.HUNGER_SPRINT_DRAIN_SECONDS - 0.01, true);
            assertEquals(GameConfig.MAX_HUNGER, player.hunger, EPSILON);

            updatePlayerHunger.invoke(controller, 0.01, true);
            assertEquals(GameConfig.MAX_HUNGER - 0.5, player.hunger, EPSILON);
        } finally {
            world.cleanup();
        }
    }

    @Test
    public void naturalHealingUsesHungerEvenDuringGracePeriod() throws Exception {
        VoxelWorld world = new VoxelWorld(5L);
        PlayerState player = new PlayerState();
        TestHost host = new TestHost();
        PlayerController controller = new PlayerController(world, player, null, host, () -> 0.0);
        Method updatePlayerHunger = PlayerController.class.getDeclaredMethod("updatePlayerHunger", double.class, boolean.class);
        updatePlayerHunger.setAccessible(true);

        try {
            player.health = GameConfig.MAX_HEALTH - 2.0;

            updatePlayerHunger.invoke(controller, GameConfig.HUNGER_REGEN_INTERVAL_SECONDS - 0.01, false);
            assertEquals(GameConfig.MAX_HEALTH - 2.0, player.health, EPSILON);
            assertEquals(GameConfig.MAX_HUNGER, player.hunger, EPSILON);

            updatePlayerHunger.invoke(controller, 0.01, false);
            assertEquals(GameConfig.MAX_HEALTH - 1.0, player.health, EPSILON);
            assertEquals(GameConfig.MAX_HUNGER, player.hunger, EPSILON);
            assertEquals(GameConfig.HUNGER_GRACE_SECONDS - GameConfig.HUNGER_REGEN_INTERVAL_SECONDS
                    - GameConfig.HUNGER_REGEN_GRACE_COST_SECONDS,
                player.hungerGraceRemaining, EPSILON);

            updatePlayerHunger.invoke(controller, GameConfig.HUNGER_REGEN_INTERVAL_SECONDS, false);
            assertEquals(GameConfig.MAX_HEALTH, player.health, EPSILON);
            assertEquals(GameConfig.MAX_HUNGER, player.hunger, EPSILON);
            assertEquals(GameConfig.HUNGER_GRACE_SECONDS - 2.0 * GameConfig.HUNGER_REGEN_INTERVAL_SECONDS
                    - 2.0 * GameConfig.HUNGER_REGEN_GRACE_COST_SECONDS,
                player.hungerGraceRemaining, EPSILON);

            player.health = GameConfig.MAX_HEALTH - 1.0;
            player.hungerGraceRemaining = 0.0;
            updatePlayerHunger.invoke(controller, GameConfig.HUNGER_REGEN_INTERVAL_SECONDS, false);
            assertEquals(GameConfig.MAX_HEALTH, player.health, EPSILON);
            assertEquals(GameConfig.MAX_HUNGER - GameConfig.HUNGER_REGEN_COST, player.hunger, EPSILON);
        } finally {
            world.cleanup();
        }
    }

    @Test
    public void hungerGraceIgnoresActionCostsAndSurvivesSaveLoad() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-hunger-grace");
        VoxelWorld world = new VoxelWorld(3L);
        world.configureWorld(directory, 3L, TerrainPreset.DEFAULT);
        PlayerState player = new PlayerState();
        TestHost host = new TestHost();
        PlayerController controller = new PlayerController(world, player, null, host, () -> 0.0);

        try {
            controller.spendHunger(1.0);
            assertEquals(GameConfig.MAX_HUNGER, player.hunger, EPSILON);

            player.hungerGraceRemaining = 37.5;
            world.savePlayerState(player, new PlayerInventory());
            PlayerState loaded = new PlayerState();
            assertTrue(world.loadPlayerState(loaded, new PlayerInventory()));
            assertEquals(37.5, loaded.hungerGraceRemaining, EPSILON);

            UUID uuid = UUID.randomUUID();
            player.hungerGraceRemaining = 12.25;
            world.saveNetworkPlayerState(uuid, player, new PlayerInventory());
            PlayerState networkLoaded = new PlayerState();
            assertTrue(world.loadNetworkPlayerState(uuid, networkLoaded, new PlayerInventory()));
            assertEquals(12.25, networkLoaded.hungerGraceRemaining, EPSILON);

            player.hungerGraceRemaining = 0.0;
            controller.spendHunger(1.0);
            assertEquals(GameConfig.MAX_HUNGER - 1.0, player.hunger, EPSILON);
        } finally {
            world.cleanup();
        }
    }

    @Test
    public void naturalSpawnLimitsSeparateLandMobsFromFishAndVillagers() {
        VoxelWorld world = new VoxelWorld(4L);
        try {
            int initialLandMobs = world.countNaturalLandMobs();
            int initialFish = world.countFishMobs();
            world.spawnMobAt(MobKind.ZOMBIE, 0.0, 70.0, 0.0);
            world.spawnMobAt(MobKind.PIG, 1.0, 70.0, 0.0);
            world.spawnMobAt(MobKind.VILLAGER, 2.0, 70.0, 0.0);
            world.spawnMobAt(MobKind.HERRING, 3.0, 60.0, 0.0);

            assertEquals(initialLandMobs + 2, world.countNaturalLandMobs());
            assertEquals(initialFish + 1, world.countFishMobs());
            assertEquals(10, VoxelWorld.naturalLandMobTargetCount());
            assertEquals(24, VoxelWorld.naturalFishMobTargetCount());
            assertEquals(10.0, VoxelWorld.naturalLandSpawnCooldownSeconds(true, 0.0), EPSILON);
            assertEquals(16.0, VoxelWorld.naturalLandSpawnCooldownSeconds(true, 1.0), EPSILON);
            assertEquals(6.0, VoxelWorld.naturalLandSpawnCooldownSeconds(false, 0.0), EPSILON);
            assertEquals(10.0, VoxelWorld.naturalLandSpawnCooldownSeconds(false, 1.0), EPSILON);
            assertEquals(8.0, VoxelWorld.naturalFishSpawnCooldownSeconds(0.0), EPSILON);
            assertEquals(14.0, VoxelWorld.naturalFishSpawnCooldownSeconds(1.0), EPSILON);
        } finally {
            world.cleanup();
        }
    }

    private static final class TestHost implements PlayerController.Host {
        int deathScreenCalls;

        @Override public boolean isCreativeMode() { return false; }
        @Override public boolean isSpectatorMode() { return false; }
        @Override public boolean isDeathScreenActive() { return false; }
        @Override public int currentWorldDifficulty() { return 2; }
        @Override public void enterDeathScreen() { deathScreenCalls++; }
    }
}
