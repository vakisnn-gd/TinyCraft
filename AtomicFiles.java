import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

final class AtomicFiles {
    interface DataWriter {
        void write(DataOutputStream output) throws IOException;
    }

    private interface StreamWriter {
        void write(OutputStream output) throws IOException;
    }

    private AtomicFiles() {
    }

    static void writeData(Path target, DataWriter writer) throws IOException {
        write(target, output -> {
            DataOutputStream dataOutput = new DataOutputStream(output);
            writer.write(dataOutput);
            dataOutput.flush();
        });
    }

    static void writeUtf8(Path target, String text) throws IOException {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        write(target, output -> output.write(bytes));
    }

    private static void write(Path target, StreamWriter writer) throws IOException {
        if (target == null || writer == null) {
            throw new IllegalArgumentException("target and writer are required");
        }
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent == null) {
            throw new IOException("save path has no parent: " + target);
        }
        Files.createDirectories(parent);
        Path tempPath = Files.createTempFile(parent, "." + absoluteTarget.getFileName() + ".", ".tmp");
        boolean replaced = false;
        try {
            try (FileChannel channel = FileChannel.open(
                tempPath,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )) {
                BufferedOutputStream output = new BufferedOutputStream(Channels.newOutputStream(channel));
                writer.write(output);
                output.flush();
                channel.force(true);
            }
            try {
                Files.move(tempPath, absoluteTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(tempPath, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            replaced = true;
        } finally {
            if (!replaced) {
                Files.deleteIfExists(tempPath);
            }
        }
    }
}
