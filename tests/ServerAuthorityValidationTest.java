import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class ServerAuthorityValidationTest {
    @Test
    public void containerModesOnlyAcceptExpectedBlocks() {
        assertTrue(MultiplayerManager.isContainerBlockAllowed(GameConfig.INVENTORY_SCREEN_CHEST, GameConfig.CHEST));
        assertTrue(MultiplayerManager.isContainerBlockAllowed(GameConfig.INVENTORY_SCREEN_FURNACE, GameConfig.FURNACE));
        assertTrue(MultiplayerManager.isContainerBlockAllowed(GameConfig.INVENTORY_SCREEN_WORKBENCH, GameConfig.CRAFTING_TABLE));

        assertFalse(MultiplayerManager.isContainerBlockAllowed(GameConfig.INVENTORY_SCREEN_CHEST, GameConfig.FURNACE));
        assertFalse(MultiplayerManager.isContainerBlockAllowed(GameConfig.INVENTORY_SCREEN_FURNACE, GameConfig.CHEST));
        assertFalse(MultiplayerManager.isContainerBlockAllowed(GameConfig.INVENTORY_SCREEN_WORKBENCH, GameConfig.DIRT));
    }

    @Test
    public void serverReachRejectsFarAwayTargets() {
        PlayerState player = new PlayerState();
        player.setPosition(0.5, GameConfig.SURFACE_Y + 1.0, 0.5);

        assertTrue(MultiplayerManager.isWithinServerReach(player, 1, GameConfig.SURFACE_Y + 1, 1));
        assertFalse(MultiplayerManager.isWithinServerReach(player, 64, GameConfig.SURFACE_Y + 1, 64));
    }

    @Test
    public void slotValidationRejectsWrongScreenSlotsAndStaleIndices() {
        assertTrue(MultiplayerManager.isValidSlotRefForScreen(
            new InventorySlotRef(InventorySlotGroup.CHEST_CONTAINER, 26),
            GameConfig.INVENTORY_SCREEN_CHEST));
        assertFalse(MultiplayerManager.isValidSlotRefForScreen(
            new InventorySlotRef(InventorySlotGroup.CHEST_CONTAINER, 27),
            GameConfig.INVENTORY_SCREEN_CHEST));
        assertFalse(MultiplayerManager.isValidSlotRefForScreen(
            new InventorySlotRef(InventorySlotGroup.CHEST_CONTAINER, 0),
            GameConfig.INVENTORY_SCREEN_PLAYER));
        assertFalse(MultiplayerManager.isValidSlotRefForScreen(
            new InventorySlotRef(InventorySlotGroup.CRAFT_3X3, 0),
            GameConfig.INVENTORY_SCREEN_PLAYER));
        assertTrue(MultiplayerManager.isValidSlotRefForScreen(
            new InventorySlotRef(InventorySlotGroup.CRAFT_3X3_RESULT, 0),
            GameConfig.INVENTORY_SCREEN_WORKBENCH));
        assertFalse(MultiplayerManager.isValidSlotRefForScreen(
            new InventorySlotRef(InventorySlotGroup.CRAFT_3X3_RESULT, 1),
            GameConfig.INVENTORY_SCREEN_WORKBENCH));
    }

    @Test
    public void rateCounterAllowsOnlyConfiguredBurstPerWindow() {
        MultiplayerManager.RateCounter counter = new MultiplayerManager.RateCounter();

        assertTrue(counter.allow(2));
        assertTrue(counter.allow(2));
        assertFalse(counter.allow(2));
    }

    @Test
    public void serverActionsAreDeferredAndKeepFifoOrder() throws Exception {
        MultiplayerManager manager = new MultiplayerManager(null, null);
        java.util.List<Integer> applied = new java.util.ArrayList<>();
        Thread[] appliedOn = new Thread[1];
        Thread producer = new Thread(() -> {
            manager.queueServerAction(() -> applied.add(1));
            manager.queueServerAction(() -> {
                applied.add(2);
                appliedOn[0] = Thread.currentThread();
            });
        }, "test socket producer");

        producer.start();
        producer.join();
        assertTrue(applied.isEmpty());

        manager.drainEvents();

        assertEquals(java.util.Arrays.asList(1, 2), applied);
        assertSame(Thread.currentThread(), appliedOn[0]);
    }

    @Test
    public void survivalBlockBreakRequiresAuthoritativeToolForStoneLikeBlocks() {
        assertFalse(MultiplayerManager.canBreakBlockServer(GameConfig.STONE, GameConfig.AIR, false));
        assertFalse(MultiplayerManager.canBreakBlockServer(GameConfig.DIAMOND_ORE, InventoryItems.STONE_PICKAXE, false));
        assertFalse(MultiplayerManager.canBreakBlockServer(GameConfig.OBSIDIAN, InventoryItems.IRON_PICKAXE, false));

        assertTrue(MultiplayerManager.canBreakBlockServer(GameConfig.DIRT, GameConfig.AIR, false));
        assertTrue(MultiplayerManager.canBreakBlockServer(GameConfig.STONE, InventoryItems.WOODEN_PICKAXE, false));
        assertTrue(MultiplayerManager.canBreakBlockServer(GameConfig.DIAMOND_ORE, InventoryItems.IRON_PICKAXE, false));
        assertTrue(MultiplayerManager.canBreakBlockServer(GameConfig.OBSIDIAN, InventoryItems.DIAMOND_PICKAXE, false));
        assertTrue(MultiplayerManager.canBreakBlockServer(GameConfig.STONE, GameConfig.AIR, true));
    }

    @Test
    public void survivalCanStartBreakingMostBlocksButHarvestStillControlsDrops() {
        assertTrue(MultiplayerManager.canStartBreakBlockServer(GameConfig.DIRT, GameConfig.AIR, false));
        assertTrue(MultiplayerManager.canStartBreakBlockServer(GameConfig.STONE, GameConfig.AIR, false));
        assertFalse(MultiplayerManager.canStartBreakBlockServer(GameConfig.OBSIDIAN, InventoryItems.IRON_PICKAXE, false));
        assertFalse(MultiplayerManager.canStartBreakBlockServer(GameConfig.BEDROCK, InventoryItems.DIAMOND_PICKAXE, true));
    }

    @Test
    public void toolSpeedChangesServerBreakDuration() {
        assertTrue(MultiplayerManager.breakDurationSeconds(GameConfig.DIRT, InventoryItems.IRON_SHOVEL, false)
            < MultiplayerManager.breakDurationSeconds(GameConfig.DIRT, GameConfig.AIR, false));
        assertTrue(MultiplayerManager.breakDurationSeconds(GameConfig.OAK_LOG, InventoryItems.IRON_AXE, false)
            < MultiplayerManager.breakDurationSeconds(GameConfig.OAK_LOG, GameConfig.AIR, false));
        assertTrue(MultiplayerManager.breakDurationSeconds(GameConfig.STONE, InventoryItems.IRON_PICKAXE, false)
            < MultiplayerManager.breakDurationSeconds(GameConfig.STONE, GameConfig.AIR, false));
        assertTrue(MultiplayerManager.breakDurationSeconds(GameConfig.STONE, InventoryItems.IRON_PICKAXE, true) == 0.0);
    }

    @Test
    public void survivalBreakFinishRequiresMatchingStartedActionAndEnoughTime() {
        long startedMillis = 10_000L;

        assertFalse(MultiplayerManager.isServerBreakCompletionValid(false, 0L, 12_000L, 1_000.0));
        assertFalse(MultiplayerManager.isServerBreakCompletionValid(false, startedMillis, 12_000L, 1_000.0));
        assertFalse(MultiplayerManager.isServerBreakCompletionValid(true, startedMillis, 10_800L, 1_000.0));
        assertTrue(MultiplayerManager.isServerBreakCompletionValid(true, startedMillis, 10_875L, 1_000.0));
    }

    @Test
    public void serverCalculatesMeleeStrengthFromAuthoritativeHeldItem() {
        assertTrue(InventoryItems.meleeDamage(GameConfig.AIR) == 2);
        assertTrue(InventoryItems.meleeDamage(InventoryItems.WOODEN_SWORD) == 5);
        assertTrue(InventoryItems.meleeDamage(InventoryItems.DIAMOND_SWORD) == 8);
        assertTrue(InventoryItems.meleeDamage(InventoryItems.NETHERITE_AXE) == 10);

        assertTrue(InventoryItems.meleeKnockback(GameConfig.AIR) == 0.42);
        assertTrue(InventoryItems.meleeKnockback(InventoryItems.IRON_SWORD) == 0.82);
        assertTrue(InventoryItems.meleeKnockback(InventoryItems.NETHERITE_AXE) == 1.15);
    }

    @Test
    public void serverSelectsOneSimulationPlayerPerAdditionalDimension() {
        PlayerState primary = new PlayerState();
        PlayerState sameDimension = new PlayerState();
        PlayerState paradise = new PlayerState();
        paradise.dimensionId = GameConfig.DIMENSION_PARADISE;
        PlayerState secondParadisePlayer = new PlayerState();
        secondParadisePlayer.dimensionId = GameConfig.DIMENSION_PARADISE;

        java.util.List<PlayerState> selected = MultiplayerManager.selectAdditionalDimensionPlayers(
            primary,
            java.util.Arrays.asList(sameDimension, paradise, secondParadisePlayer)
        );

        assertEquals(1, selected.size());
        assertSame(paradise, selected.get(0));
    }

    @Test
    public void dedicatedAllowCheatsDoesNotGrantAdminCommands() throws Exception {
        ServerProperties properties = newServerProperties();
        properties.allowCheats = true;
        GameServer server = new GameServer(properties);

        String result = server.executeServerCommand(UUID.randomUUID(), "Guest", "kick Other");

        assertTrue(result.contains("do not have permission"));
    }

    @Test
    public void dedicatedCheatCommandsRequireAllowCheatsOrOp() throws Exception {
        ServerProperties properties = newServerProperties();
        properties.allowCheats = false;
        GameServer server = new GameServer(properties);

        String result = server.executeServerCommand(UUID.randomUUID(), "Guest", "give diamond 1");

        assertTrue(result.contains("do not have permission"));
    }

    @Test
    public void dedicatedNewGameplayCommandsRequireAllowCheatsOrOp() throws Exception {
        ServerProperties properties = newServerProperties();
        properties.allowCheats = false;
        GameServer server = new GameServer(properties);
        UUID guest = UUID.randomUUID();

        assertTrue(server.executeServerCommand(guest, "Guest", "tp 1 70 1").contains("do not have permission"));
        assertTrue(server.executeServerCommand(guest, "Guest", "kill").contains("do not have permission"));
        assertTrue(server.executeServerCommand(guest, "Guest", "summon zombie").contains("do not have permission"));
        assertTrue(server.executeServerCommand(guest, "Guest", "setblock 1 70 1 stone").contains("do not have permission"));
        assertTrue(server.executeServerCommand(guest, "Guest", "fill 1 70 1 2 70 2 stone").contains("do not have permission"));
    }

    @Test
    public void dedicatedCheatsDoNotAllowTargetingAnotherPlayerWithTeleport() throws Exception {
        ServerProperties properties = newServerProperties();
        properties.allowCheats = true;
        GameServer server = new GameServer(properties);

        String result = server.executeServerCommand(UUID.randomUUID(), "Guest", "tp Other 1 70 1");

        assertTrue(result.contains("do not have permission"));
    }

    @Test
    public void dedicatedServerDoesNotAcceptRemovedTeleportAlias() throws Exception {
        ServerProperties properties = newServerProperties();
        properties.allowCheats = true;
        GameServer server = new GameServer(properties);

        assertEquals(null, server.executeServerCommand(UUID.randomUUID(), "Guest", "teleport 1 70 1"));
    }

    @Test
    public void dedicatedPublicCommandsDoNotRequireCheatsOrOperator() throws Exception {
        ServerProperties properties = newServerProperties();
        properties.allowCheats = false;
        GameServer server = new GameServer(properties);

        String result = server.executeServerCommand(UUID.randomUUID(), "Guest", "help");

        assertTrue(result.startsWith("Commands:"));
    }

    private ServerProperties newServerProperties() throws Exception {
        java.lang.reflect.Constructor<ServerProperties> constructor =
            ServerProperties.class.getDeclaredConstructor(java.nio.file.Path.class);
        constructor.setAccessible(true);
        return constructor.newInstance(java.nio.file.Files.createTempFile("tinycraft-server", ".properties"));
    }
}
