import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

final class ServerProperties {
    private static final String FILE_NAME = "server.properties";

    int port = MultiplayerProtocol.DEFAULT_PORT;
    String world = "server_world";
    String seed = "";
    String terrain = "default";
    int maxPlayers = 8;
    String motd = "TinyCraft v0.2.1 Server";
    boolean allowPvp = true;
    boolean allowCheats = false;
    boolean whitelist = false;
    int viewDistance = 8;

    private final Path path;

    private ServerProperties(Path path) {
        this.path = path;
    }

    static ServerProperties loadOrCreate() throws IOException {
        Path path = RuntimePaths.resolve(FILE_NAME);
        ServerProperties properties = new ServerProperties(path);
        if (Files.isRegularFile(path)) {
            properties.load();
        } else {
            properties.save();
        }
        return properties;
    }

    void applyArgs(String[] args) {
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            String value = null;
            int equals = arg.indexOf('=');
            if (equals >= 0) {
                value = arg.substring(equals + 1);
                arg = arg.substring(0, equals);
            } else if (i + 1 < args.length && args[i + 1] != null && !args[i + 1].startsWith("--")) {
                value = args[++i];
            }
            if (value == null) {
                continue;
            }
            if ("--port".equals(arg)) {
                port = clamp(parseInt(value, port), 1, 65535);
            } else if ("--world".equals(arg)) {
                world = sanitizeWorldName(value, world);
            } else if ("--seed".equals(arg)) {
                seed = value.trim();
            } else if ("--terrain".equals(arg)) {
                terrain = TerrainPreset.fromMetadata(value).metadataId();
            } else if ("--maxPlayers".equals(arg) || "--max-players".equals(arg)) {
                maxPlayers = clamp(parseInt(value, maxPlayers), 1, 64);
            } else if ("--motd".equals(arg)) {
                motd = value;
            } else if ("--allowPvp".equals(arg) || "--pvp".equals(arg)) {
                allowPvp = Boolean.parseBoolean(value);
            } else if ("--allowCheats".equals(arg) || "--cheats".equals(arg)) {
                allowCheats = Boolean.parseBoolean(value);
            } else if ("--whitelist".equals(arg) || "--white-list".equals(arg)) {
                whitelist = Boolean.parseBoolean(value);
            } else if ("--viewDistance".equals(arg) || "--view-distance".equals(arg)) {
                viewDistance = clamp(parseInt(value, viewDistance), GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
            }
        }
    }

    TerrainPreset terrainPreset() {
        TerrainPreset preset = TerrainPreset.fromMetadata(terrain);
        return preset == TerrainPreset.LEGACY ? TerrainPreset.DEFAULT : preset;
    }

    Path worldDirectory() {
        return RuntimePaths.resolve(GameConfig.SAVE_ROOT_DIRECTORY, sanitizeWorldName(world, "server_world"));
    }

    Long explicitSeed() {
        String trimmed = seed == null ? "" : seed.trim();
        return trimmed.isEmpty() ? null : Long.valueOf(parseSeed(trimmed));
    }

    long randomSeed() {
        return new Random(System.nanoTime() ^ System.currentTimeMillis()).nextLong();
    }

    void printEffective() {
        System.out.println("TinyCraft server properties:");
        System.out.println("  world=" + world);
        System.out.println("  port=" + port);
        System.out.println("  terrain=" + terrainPreset().metadataId());
        System.out.println("  maxPlayers=" + maxPlayers);
        System.out.println("  allowPvp=" + allowPvp);
        System.out.println("  allowCheats=" + allowCheats);
        System.out.println("  whitelist=" + whitelist);
        System.out.println("  viewDistance=" + viewDistance);
    }

    private void load() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        port = clamp(parseInt(properties.getProperty("port"), port), 1, 65535);
        world = sanitizeWorldName(properties.getProperty("world"), world);
        seed = properties.getProperty("seed", seed).trim();
        terrain = TerrainPreset.fromMetadata(properties.getProperty("terrain", terrain)).metadataId();
        maxPlayers = clamp(parseInt(properties.getProperty("maxPlayers"), maxPlayers), 1, 64);
        motd = properties.getProperty("motd", motd);
        allowPvp = Boolean.parseBoolean(properties.getProperty("allowPvp", Boolean.toString(allowPvp)));
        allowCheats = Boolean.parseBoolean(properties.getProperty("allowCheats", Boolean.toString(allowCheats)));
        whitelist = Boolean.parseBoolean(properties.getProperty("whitelist", Boolean.toString(whitelist)));
        viewDistance = clamp(parseInt(properties.getProperty("viewDistance"), viewDistance), GameConfig.MIN_RENDER_DISTANCE, GameConfig.MAX_RENDER_DISTANCE_CHUNKS);
    }

    void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("port", Integer.toString(port));
        properties.setProperty("world", world);
        properties.setProperty("seed", seed);
        properties.setProperty("terrain", terrain);
        properties.setProperty("maxPlayers", Integer.toString(maxPlayers));
        properties.setProperty("motd", motd);
        properties.setProperty("allowPvp", Boolean.toString(allowPvp));
        properties.setProperty("allowCheats", Boolean.toString(allowCheats));
        properties.setProperty("whitelist", Boolean.toString(whitelist));
        properties.setProperty("viewDistance", Integer.toString(viewDistance));
        try (OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "TinyCraft dedicated server settings");
        }
    }

    private static String sanitizeWorldName(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length() && builder.length() < 64; i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ' ') {
                builder.append(c);
            }
        }
        return builder.length() == 0 ? fallback : builder.toString().trim();
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static long parseSeed(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            try {
                String normalized = trimmed.toLowerCase(Locale.ROOT);
                if (normalized.startsWith("0x")) {
                    normalized = normalized.substring(2);
                }
                return Long.parseUnsignedLong(normalized, 16);
            } catch (NumberFormatException ignoredAgain) {
                long hash = 1125899906842597L;
                for (int i = 0; i < trimmed.length(); i++) {
                    hash = 31L * hash + trimmed.charAt(i);
                }
                return hash;
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
