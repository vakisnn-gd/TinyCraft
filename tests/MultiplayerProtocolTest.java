import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

public class MultiplayerProtocolTest {
    @Test
    public void packetRoundTripPreservesTypeAndPayload() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);

        MultiplayerProtocol.writePacket(output, MultiplayerProtocol.CHAT, packet -> packet.writeUTF("hello"));

        MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(
            new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(MultiplayerProtocol.CHAT, packet.type);
        assertEquals("hello", packet.input.readUTF());
        assertEquals(0, packet.input.available());
    }

    @Test
    public void eofBeforePacketReturnsNull() throws Exception {
        MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(
            new DataInputStream(new ByteArrayInputStream(new byte[0])));

        assertNull(packet);
    }

    @Test(expected = IOException.class)
    public void zeroLengthPacketIsRejected() throws Exception {
        readLengthOnly(0);
    }

    @Test(expected = IOException.class)
    public void negativeLengthPacketIsRejected() throws Exception {
        readLengthOnly(-1);
    }

    @Test(expected = IOException.class)
    public void oversizedLengthPacketIsRejected() throws Exception {
        readLengthOnly(MultiplayerProtocol.MAX_CHUNK_PACKET_BYTES + 2);
    }

    @Test
    public void oversizedNonChunkPacketIsRejectedBeforePayloadRead() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(MultiplayerProtocol.MAX_CHUNK_PACKET_BYTES);
        output.writeByte(MultiplayerProtocol.PLAYER_STATE);
        output.flush();

        try {
            MultiplayerProtocol.readPacket(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        } catch (IOException expected) {
            assertEquals("packet payload too large for type " + MultiplayerProtocol.PLAYER_STATE + ": " + (MultiplayerProtocol.MAX_CHUNK_PACKET_BYTES - 1), expected.getMessage());
            return;
        }
        throw new AssertionError("Expected oversized PLAYER_STATE to be rejected");
    }

    @Test
    public void unknownPacketTypeIsRejectedBeforePayloadRead() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(MultiplayerProtocol.MAX_PACKET_BYTES);
        output.writeByte(99);
        output.flush();

        try {
            MultiplayerProtocol.readPacket(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        } catch (IOException expected) {
            assertEquals("unknown packet type: 99", expected.getMessage());
            return;
        }
        throw new AssertionError("Expected unknown packet type to be rejected");
    }

    private void readLengthOnly(int length) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(length);
        output.flush();
        MultiplayerProtocol.readPacket(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }
}
