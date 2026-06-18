import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

final class LanServerTransport {
    interface AcceptHandler {
        void onAccept(NetworkConnection connection);

        void onError(IOException exception);
    }

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    boolean start(String bindAddress, int port, AcceptHandler handler, String threadName) throws IOException {
        stop();
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindAddress, port));
        running = true;
        acceptThread = new Thread(() -> acceptLoop(handler), threadName == null ? "TinyCraft LAN server" : threadName);
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException exception) {
                System.err.println("Failed to close the LAN server socket: " + exception);
            }
            serverSocket = null;
        }
    }

    private void acceptLoop(AcceptHandler handler) {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                handler.onAccept(new NetworkConnection(socket));
            } catch (IOException exception) {
                if (running && handler != null) {
                    handler.onError(exception);
                }
            }
        }
    }
}
