import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class ServerAccessList {
    private final Path opsPath = RuntimePaths.resolve("ops.json");
    private final Path whitelistPath = RuntimePaths.resolve("whitelist.json");
    private final Path bannedPath = RuntimePaths.resolve("banned-players.json");

    private final LinkedHashSet<String> ops = new LinkedHashSet<>();
    private final LinkedHashSet<String> whitelist = new LinkedHashSet<>();
    private final LinkedHashSet<String> banned = new LinkedHashSet<>();

    void loadOrCreate() throws IOException {
        loadList(opsPath, ops);
        loadList(whitelistPath, whitelist);
        loadList(bannedPath, banned);
        saveAll();
    }

    void saveAll() throws IOException {
        saveList(opsPath, ops);
        saveList(whitelistPath, whitelist);
        saveList(bannedPath, banned);
    }

    boolean isOperator(UUID uuid, String name) {
        return containsPlayer(ops, uuid, name);
    }

    boolean isWhitelisted(UUID uuid, String name) {
        return containsPlayer(whitelist, uuid, name);
    }

    boolean isBanned(UUID uuid, String name) {
        return containsPlayer(banned, uuid, name);
    }

    boolean addOperator(String token, UUID uuid, String displayName) throws IOException {
        boolean changed = addPlayerTokens(ops, token, uuid, displayName);
        if (changed) {
            saveList(opsPath, ops);
        }
        return changed;
    }

    boolean removeOperator(String token, UUID uuid, String displayName) throws IOException {
        boolean changed = removePlayerTokens(ops, token, uuid, displayName);
        if (changed) {
            saveList(opsPath, ops);
        }
        return changed;
    }

    boolean addWhitelist(String token, UUID uuid, String displayName) throws IOException {
        boolean changed = addPlayerTokens(whitelist, token, uuid, displayName);
        if (changed) {
            saveList(whitelistPath, whitelist);
        }
        return changed;
    }

    boolean removeWhitelist(String token, UUID uuid, String displayName) throws IOException {
        boolean changed = removePlayerTokens(whitelist, token, uuid, displayName);
        if (changed) {
            saveList(whitelistPath, whitelist);
        }
        return changed;
    }

    boolean addBan(String token, UUID uuid, String displayName) throws IOException {
        boolean changed = addPlayerTokens(banned, token, uuid, displayName);
        if (changed) {
            saveList(bannedPath, banned);
        }
        return changed;
    }

    boolean removeBan(String token, UUID uuid, String displayName) throws IOException {
        boolean changed = removePlayerTokens(banned, token, uuid, displayName);
        if (changed) {
            saveList(bannedPath, banned);
        }
        return changed;
    }

    String describeOps() {
        return describe(ops);
    }

    String describeWhitelist() {
        return describe(whitelist);
    }

    String describeBanned() {
        return describe(banned);
    }

    private boolean addPlayerTokens(Set<String> target, String token, UUID uuid, String displayName) {
        boolean changed = false;
        String normalizedToken = normalizeToken(token);
        if (!normalizedToken.isEmpty()) {
            changed |= target.add(normalizedToken);
        }
        if (uuid != null) {
            changed |= target.add("uuid:" + uuid.toString().toLowerCase(Locale.ROOT));
        }
        String normalizedName = normalizeName(displayName);
        if (!normalizedName.isEmpty()) {
            changed |= target.add("name:" + normalizedName);
        }
        return changed;
    }

    private boolean removePlayerTokens(Set<String> target, String token, UUID uuid, String displayName) {
        boolean changed = false;
        String normalizedToken = normalizeToken(token);
        if (!normalizedToken.isEmpty()) {
            changed |= target.remove(normalizedToken);
            if (!normalizedToken.startsWith("name:") && !normalizedToken.startsWith("uuid:")) {
                changed |= target.remove("name:" + normalizedToken);
                changed |= target.remove("uuid:" + normalizedToken);
            }
        }
        if (uuid != null) {
            changed |= target.remove("uuid:" + uuid.toString().toLowerCase(Locale.ROOT));
        }
        String normalizedName = normalizeName(displayName);
        if (!normalizedName.isEmpty()) {
            changed |= target.remove("name:" + normalizedName);
        }
        return changed;
    }

    private boolean containsPlayer(Set<String> source, UUID uuid, String name) {
        if (uuid != null && source.contains("uuid:" + uuid.toString().toLowerCase(Locale.ROOT))) {
            return true;
        }
        String normalizedName = normalizeName(name);
        return !normalizedName.isEmpty()
            && (source.contains("name:" + normalizedName) || source.contains(normalizedName));
    }

    private void loadList(Path path, LinkedHashSet<String> target) throws IOException {
        target.clear();
        if (!Files.isRegularFile(path)) {
            return;
        }
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        for (String value : parseJsonStringArray(text)) {
            String normalized = normalizeToken(value);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private void saveList(Path path, LinkedHashSet<String> source) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (!source.isEmpty()) {
            builder.append(System.lineSeparator());
            int index = 0;
            for (String entry : source) {
                builder.append("  \"").append(jsonEscape(entry)).append("\"");
                if (++index < source.size()) {
                    builder.append(',');
                }
                builder.append(System.lineSeparator());
            }
        }
        builder.append("]").append(System.lineSeparator());
        Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private List<String> parseJsonStringArray(String text) {
        ArrayList<String> values = new ArrayList<>();
        boolean inString = false;
        boolean escaping = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; text != null && i < text.length(); i++) {
            char c = text.charAt(i);
            if (!inString) {
                if (c == '"') {
                    inString = true;
                    current.setLength(0);
                }
                continue;
            }
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                values.add(current.toString());
                inString = false;
            } else {
                current.append(c);
            }
        }
        return values;
    }

    private String describe(Set<String> values) {
        if (values.isEmpty()) {
            return "(empty)";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String normalizeToken(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("uuid:") || trimmed.startsWith("name:")) {
            return trimmed;
        }
        try {
            UUID uuid = UUID.fromString(trimmed);
            return "uuid:" + uuid.toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ignored) {
            return "name:" + trimmed;
        }
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
