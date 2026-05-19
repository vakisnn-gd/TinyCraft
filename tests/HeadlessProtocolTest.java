import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import org.junit.Test;

public class HeadlessProtocolTest {
    @Test
    public void loopbackHandshakeAcceptsMatchingVersion() throws Exception {
        try (HandshakeServer server = new HandshakeServer()) {
            server.start();
            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                writeHello(output, MultiplayerProtocol.VERSION, UUID.randomUUID(), "PlayerOne");

                MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(input);
                assertEquals(MultiplayerProtocol.WELCOME, packet.type);
                assertEquals(1234L, packet.input.readLong());
                assertEquals("default", packet.input.readUTF());
            }
        }
    }

    @Test
    public void loopbackHandshakeRejectsMismatchedVersion() throws Exception {
        try (HandshakeServer server = new HandshakeServer()) {
            server.start();
            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                writeHello(output, MultiplayerProtocol.VERSION - 1, UUID.randomUUID(), "OldClient");

                MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(input);
                assertEquals(MultiplayerProtocol.DISCONNECT, packet.type);
                assertTrue(packet.input.readUTF().contains("Incompatible"));
            }
        }
    }

    @Test
    public void loopbackHandshakeRejectsDuplicateUuid() throws Exception {
        try (HandshakeServer server = new HandshakeServer()) {
            server.start();
            UUID uuid = UUID.randomUUID();

            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                writeHello(output, MultiplayerProtocol.VERSION, uuid, "First");
                assertEquals(MultiplayerProtocol.WELCOME, MultiplayerProtocol.readPacket(input).type);
            }

            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                writeHello(output, MultiplayerProtocol.VERSION, uuid, "Second");

                MultiplayerProtocol.Packet packet = MultiplayerProtocol.readPacket(input);
                assertEquals(MultiplayerProtocol.DISCONNECT, packet.type);
                assertTrue(packet.input.readUTF().contains("Duplicate"));
            }
        }
    }

    private void writeHello(DataOutputStream output, int version, UUID uuid, String name) throws Exception {
        MultiplayerProtocol.writePacket(output, MultiplayerProtocol.HELLO, packet -> {
            packet.writeInt(MultiplayerProtocol.MAGIC);
            packet.writeInt(version);
            MultiplayerProtocol.writeUuid(packet, uuid);
            packet.writeUTF(name);
        });
    }

    private static final class HandshakeServer implements AutoCloseable {
        private final java.util.HashSet<UUID> seen = new java.util.HashSet<>();
        private ServerSocket serverSocket;
        private Thread thread;
        private volatile boolean running;

        void start() throws Exception {
            serverSocket = new ServerSocket(0);
            running = true;
            thread = new Thread(this::acceptLoop, "test-handshake-server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    handle(socket);
                } catch (Exception ignored) {
                    if (running) {
                        throw new RuntimeException(ignored);
                    }
                }
            }
        }

        private void handle(Socket socket) throws Exception {
            try (Socket s = socket) {
                DataInputStream input = new DataInputStream(s.getInputStream());
                DataOutputStream output = new DataOutputStream(s.getOutputStream());
                MultiplayerProtocol.Packet hello = MultiplayerProtocol.readPacket(input);
                int magic = hello.input.readInt();
                int version = hello.input.readInt();
                UUID uuid = MultiplayerProtocol.readUuid(hello.input);
                hello.input.readUTF();
                if (hello.type != MultiplayerProtocol.HELLO || magic != MultiplayerProtocol.MAGIC || version != MultiplayerProtocol.VERSION) {
                    MultiplayerProtocol.writePacket(output, MultiplayerProtocol.DISCONNECT, packet -> packet.writeUTF("Incompatible multiplayer protocol."));
                    return;
                }
                if (!seen.add(uuid)) {
                    MultiplayerProtocol.writePacket(output, MultiplayerProtocol.DISCONNECT, packet -> packet.writeUTF("Duplicate player uuid."));
                    return;
                }
                MultiplayerProtocol.writePacket(output, MultiplayerProtocol.WELCOME, packet -> {
                    packet.writeLong(1234L);
                    packet.writeUTF("default");
                    packet.writeDouble(0.5);
                    packet.writeDouble(64.0);
                    packet.writeDouble(0.5);
                    packet.writeDouble(0.0);
                });
            }
        }

        @Override
        public void close() throws Exception {
            running = false;
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (thread != null) {
                thread.join(1000L);
            }
        }
    }
}
