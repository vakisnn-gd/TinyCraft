import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

final class NetworkConnection {
    final Socket socket;
    final DataInputStream input;
    final DataOutputStream output;
    volatile boolean open = true;

    NetworkConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    void closeQuietly() {
        open = false;
        try {
            socket.close();
        } catch (IOException exception) {
            System.err.println("Failed to close the network connection socket: " + exception);
        }
    }
}
