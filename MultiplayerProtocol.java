import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

final class MultiplayerProtocol {
    static final int MAGIC = 0x54434D50; // TCMP
    static final int VERSION = 7;
    static final int DEFAULT_PORT = 25566;
    static final int MAX_PACKET_BYTES = 512 * 1024;
    static final int MAX_CHUNK_PACKET_BYTES = 4 * 1024 * 1024;
    static final int MAX_TEXT_BYTES = 1024;
    static final int MAX_INVENTORY_PACKET_BYTES = 64 * 1024;

    static final byte HELLO = 1;
    static final byte WELCOME = 2;
    static final byte PLAYER_SPAWN = 3;
    static final byte PLAYER_STATE = 4;
    static final byte PLAYER_DESPAWN = 5;
    static final byte CHAT = 6;
    static final byte CHUNK_REQUEST = 7;
    static final byte CHUNK_DATA = 8;
    static final byte BLOCK_ACTION = 9;
    static final byte BLOCK_UPDATE = 10;
    static final byte WORLD_TIME = 11;
    static final byte MOB_SNAPSHOT = 12;
    static final byte DROPPED_ITEM_SNAPSHOT = 13;
    static final byte DISCONNECT = 14;
    static final byte PLAYER_ATTACK = 15;
    static final byte PLAYER_HEALTH = 16;
    static final byte PING = 17;
    static final byte PONG = 18;
    static final byte PLAYER_LIST = 19;
    static final byte COMMAND = 20;
    static final byte INVENTORY_ADD = 21;
    static final byte MOB_ATTACK = 22;
    static final byte SERVER_PLAYER_STATE = 23;
    static final byte INVENTORY_SYNC = 24;
    static final byte CONTAINER_OPEN_REQUEST = 25;
    static final byte CONTAINER_OPEN = 26;
    static final byte CONTAINER_CLICK = 27;
    static final byte CONTAINER_CLOSE = 28;
    static final byte CONTAINER_UPDATE = 29;
    static final byte ITEM_DROP = 30;

    static final byte BLOCK_BREAK = 1;
    static final byte BLOCK_PLACE = 2;
    static final byte BLOCK_BREAK_START = 3;
    static final byte BLOCK_BREAK_FINISH = 4;

    private MultiplayerProtocol() {
    }

    static void writePacket(DataOutputStream output, byte type, PacketWriter writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream packet = new DataOutputStream(bytes);
        packet.writeByte(type);
        if (writer != null) {
            writer.write(packet);
        }
        packet.flush();
        byte[] payload = bytes.toByteArray();
        output.writeInt(payload.length);
        output.write(payload);
        output.flush();
    }

    static Packet readPacket(DataInputStream input) throws IOException {
        int length;
        try {
            length = input.readInt();
        } catch (EOFException eof) {
            return null;
        }
        if (length <= 0 || length > MAX_CHUNK_PACKET_BYTES + 1) {
            throw new IOException("invalid packet length: " + length);
        }
        byte type = input.readByte();
        int payloadLength = length - 1;
        int maxPayloadBytes = maxPayloadBytesForType(type);
        if (maxPayloadBytes < 0) {
            throw new IOException("unknown packet type: " + type);
        }
        if (payloadLength > maxPayloadBytes) {
            throw new IOException("packet payload too large for type " + type + ": " + payloadLength);
        }
        byte[] payload = new byte[payloadLength];
        input.readFully(payload);
        DataInputStream packetInput = new DataInputStream(new ByteArrayInputStream(payload));
        return new Packet(type, packetInput, payloadLength);
    }

    static int maxPayloadBytesForType(byte type) {
        switch (type) {
            case CHUNK_DATA:
                return MAX_CHUNK_PACKET_BYTES;
            case MOB_SNAPSHOT:
            case DROPPED_ITEM_SNAPSHOT:
            case PLAYER_LIST:
                return MAX_PACKET_BYTES;
            case INVENTORY_SYNC:
            case CONTAINER_OPEN_REQUEST:
            case CONTAINER_OPEN:
            case CONTAINER_CLICK:
            case CONTAINER_CLOSE:
            case CONTAINER_UPDATE:
                return MAX_INVENTORY_PACKET_BYTES;
            case PLAYER_STATE:
                return 256;
            case CHUNK_REQUEST:
                return 12;
            case BLOCK_ACTION:
                return 40;
            case PLAYER_ATTACK:
                return 20;
            case MOB_ATTACK:
                return 12;
            case PING:
            case PONG:
                return 8;
            case SERVER_PLAYER_STATE:
                return 64;
            case ITEM_DROP:
                return 16;
            case WELCOME:
                return 128;
            case PLAYER_SPAWN:
            case PLAYER_DESPAWN:
            case BLOCK_UPDATE:
            case WORLD_TIME:
            case PLAYER_HEALTH:
            case INVENTORY_ADD:
                return 256;
            case HELLO:
            case CHAT:
            case COMMAND:
            case DISCONNECT:
                return MAX_TEXT_BYTES;
            default:
                return -1;
        }
    }

    static void writeUuid(DataOutputStream output, UUID uuid) throws IOException {
        output.writeLong(uuid.getMostSignificantBits());
        output.writeLong(uuid.getLeastSignificantBits());
    }

    static UUID readUuid(DataInputStream input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }

    interface PacketWriter {
        void write(DataOutputStream output) throws IOException;
    }

    static final class Packet {
        final byte type;
        final DataInputStream input;
        final int payloadLength;

        Packet(byte type, DataInputStream input, int payloadLength) {
            this.type = type;
            this.input = input;
            this.payloadLength = payloadLength;
        }
    }
}
