import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

final class LocalProfile {
    static final int MAX_NAME_LENGTH = 16;
    private static final String PROFILE_FILE = "profile.properties";

    final UUID uuid;
    String name;

    private LocalProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = sanitizeName(name, defaultName(uuid));
    }

    static LocalProfile loadOrCreate() {
        Path path = RuntimePaths.resolve(PROFILE_FILE);
        Properties properties = new Properties();
        UUID uuid = null;
        String name = null;
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
                uuid = UUID.fromString(properties.getProperty("uuid", ""));
                name = properties.getProperty("name");
            } catch (Exception ignored) {
                uuid = null;
                name = null;
            }
        }

        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        LocalProfile profile = new LocalProfile(uuid, name == null ? defaultName(uuid) : name);
        profile.save();
        return profile;
    }

    void setName(String nextName) {
        name = sanitizeName(nextName, defaultName(uuid));
        save();
    }

    void save() {
        Path path = RuntimePaths.resolve(PROFILE_FILE);
        Properties properties = new Properties();
        properties.setProperty("uuid", uuid.toString());
        properties.setProperty("name", name);
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "TinyCraft local multiplayer profile");
        } catch (IOException exception) {
            System.err.println("Failed to save local multiplayer profile to " + path + ": " + exception);
        }
    }

    static String sanitizeName(String raw, String fallback) {
        String trimmed = raw == null ? "" : raw.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < trimmed.length() && builder.length() < MAX_NAME_LENGTH; i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                builder.append(c);
            }
        }
        if (builder.length() == 0) {
            return fallback == null || fallback.trim().isEmpty() ? "Player0000" : fallback;
        }
        return builder.toString();
    }

    private static String defaultName(UUID uuid) {
        Random random = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        int suffix = Math.abs(random.nextInt()) % 10000;
        return String.format(Locale.ROOT, "Player%04d", suffix);
    }
}
