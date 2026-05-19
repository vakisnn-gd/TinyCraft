import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TinyCraftServer {
    private TinyCraftServer() {
    }

    public static void main(String[] args) throws IOException {
        installServerLog();
        ServerProperties properties = ServerProperties.loadOrCreate();
        properties.applyArgs(args);
        properties.printEffective();

        GameServer server = new GameServer(properties);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "TinyCraft server shutdown"));
        if (!server.start()) {
            System.exit(1);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while (server.isRunning() && (line = reader.readLine()) != null) {
                server.handleConsoleCommand(line);
            }
        } finally {
            server.stop();
        }
    }

    private static void installServerLog() throws IOException {
        Path logs = RuntimePaths.resolve("logs");
        Files.createDirectories(logs);
        OutputStream logOutput = Files.newOutputStream(logs.resolve("latest.log"));
        System.setOut(new PrintStream(new TeeOutputStream(System.out, logOutput), true, "UTF-8"));
        System.setErr(new PrintStream(new TeeOutputStream(System.err, logOutput), true, "UTF-8"));
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream first;
        private final OutputStream second;

        TeeOutputStream(OutputStream first, OutputStream second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            first.write(b);
            second.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            first.write(b, off, len);
            second.write(b, off, len);
        }

        @Override
        public synchronized void flush() throws IOException {
            first.flush();
            second.flush();
        }
    }
}
