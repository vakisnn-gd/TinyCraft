import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Test;

public class InventoryNetworkCodecTest {
    @Test
    public void playerInventoryRoundTripsThroughNetworkCodec() throws Exception {
        PlayerInventory inventory = new PlayerInventory();
        inventory.addItem(GameConfig.COBBLESTONE, 32);
        inventory.getArmorStack(0).set(InventoryItems.IRON_HELMET, 1);
        inventory.getCursorStack().set(InventoryItems.STICK, 4);

        PlayerInventory copy = new PlayerInventory();
        copy.readFrom(new DataInputStream(new ByteArrayInputStream(writeInventory(inventory))));

        assertEquals(GameConfig.COBBLESTONE, copy.getHotbarStack(0).itemId);
        assertEquals(32, copy.getHotbarStack(0).count);
        assertEquals(InventoryItems.IRON_HELMET, copy.getArmorStack(0).itemId);
        assertEquals(InventoryItems.STICK, copy.getCursorStack().itemId);
        assertEquals(4, copy.getCursorStack().count);
    }

    @Test
    public void chestAndFurnaceRoundTripThroughNetworkCodec() throws Exception {
        ContainerInventory chest = new ContainerInventory(27);
        chest.getStack(5).set(InventoryItems.DIAMOND_ITEM, 3);

        FurnaceBlockEntity furnace = new FurnaceBlockEntity();
        furnace.input.set(GameConfig.SAND, 2);
        furnace.fuel.set(InventoryItems.COAL_ITEM, 1);
        furnace.burnRemaining = 4.5;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        chest.writeTo(output);
        furnace.writeTo(output);
        output.flush();

        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        ContainerInventory chestCopy = ContainerInventory.readFrom(input, 27);
        FurnaceBlockEntity furnaceCopy = new FurnaceBlockEntity();
        furnaceCopy.readFrom(input);

        assertEquals(InventoryItems.DIAMOND_ITEM, chestCopy.getStack(5).itemId);
        assertEquals(3, chestCopy.getStack(5).count);
        assertEquals(GameConfig.SAND, furnaceCopy.input.itemId);
        assertEquals(InventoryItems.COAL_ITEM, furnaceCopy.fuel.itemId);
        assertEquals(4.5, furnaceCopy.burnRemaining, 0.0001);
        assertEquals(0, input.available());
    }

    @Test
    public void invalidContainerSizeIsRejected() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(999);
        output.flush();

        try {
            ContainerInventory.readFrom(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), 27);
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("invalid container size"));
            return;
        }
        throw new AssertionError("Expected invalid container size to be rejected");
    }

    @Test
    public void networkPlayerInventoryPersistsAcrossSaveLoad() throws Exception {
        Path directory = Files.createTempDirectory("tinycraft-network-player");
        VoxelWorld world = new VoxelWorld(1234L);
        world.configureWorld(directory, 1234L, TerrainPreset.DEFAULT);
        UUID uuid = UUID.randomUUID();

        PlayerState savedPlayer = new PlayerState();
        savedPlayer.setPosition(12.5, GameConfig.SURFACE_Y + 3.0, -4.5);
        PlayerInventory savedInventory = new PlayerInventory();
        savedInventory.getHotbarStack(2).set(InventoryItems.DIAMOND_ITEM, 3);
        savedInventory.getStorageStack(4).set(GameConfig.COBBLESTONE, 32);
        world.saveNetworkPlayerState(uuid, savedPlayer, savedInventory);

        PlayerState loadedPlayer = new PlayerState();
        PlayerInventory loadedInventory = new PlayerInventory();
        assertTrue(world.loadNetworkPlayerState(uuid, loadedPlayer, loadedInventory));
        assertEquals(12.5, loadedPlayer.x, 0.0001);
        assertEquals(InventoryItems.DIAMOND_ITEM, loadedInventory.getHotbarStack(2).itemId);
        assertEquals(3, loadedInventory.getHotbarStack(2).count);
        assertEquals(GameConfig.COBBLESTONE, loadedInventory.getStorageStack(4).itemId);
        assertEquals(32, loadedInventory.getStorageStack(4).count);
    }

    @Test
    public void malformedNetworkColumnRunLengthIsRejected() throws Exception {
        ByteArrayOutputStream sectionBytes = new ByteArrayOutputStream();
        DataOutputStream sectionOutput = new DataOutputStream(sectionBytes);
        sectionOutput.writeInt(Chunk.VOLUME - 1);
        sectionOutput.writeUTF("minecraft:air");
        sectionOutput.writeByte(-1);
        sectionOutput.flush();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(0);
        output.writeInt(0);
        output.writeInt(GameConfig.DIMENSION_OVERWORLD);
        output.writeBoolean(true);
        output.writeInt(ChunkGenerationStatus.FULL.ordinal());
        for (int i = 0; i < GameConfig.CHUNK_SIZE * GameConfig.CHUNK_SIZE; i++) {
            output.writeShort(GameConfig.SURFACE_Y);
        }
        output.writeInt(GameConfig.SECTION_COUNT);
        output.writeInt(0);
        output.writeInt(1);
        output.writeInt(sectionBytes.size());
        output.write(sectionBytes.toByteArray());
        output.flush();

        try {
            new VoxelWorld(1234L).readNetworkColumn(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("invalid network chunk run length"));
            return;
        }
        throw new AssertionError("Expected malformed network column to be rejected");
    }

    private byte[] writeInventory(PlayerInventory inventory) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        inventory.writeTo(output);
        output.flush();
        return bytes.toByteArray();
    }
}
