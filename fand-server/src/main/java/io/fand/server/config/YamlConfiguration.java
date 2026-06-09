package io.fand.server.config;

import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationException;
import io.fand.api.config.ConfigurationSection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * YAML-backed {@link Configuration}. Comments and original key order from the
 * source file are <em>not</em> preserved across {@link #save()}; the file is
 * round-tripped through SnakeYAML's representer.
 */
public final class YamlConfiguration implements Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlConfiguration.class);

    private final Path file;
    private MapSection root;

    public YamlConfiguration(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        this.root = new MapSection(new LinkedHashMap<>());
    }

    /**
     * Loads {@code file} into a new instance. If the file does not exist the
     * returned configuration is empty.
     */
    public static YamlConfiguration load(Path file) {
        var config = new YamlConfiguration(file);
        config.reload();
        return config;
    }

    /**
     * Like {@link #load(Path)}, but if {@code file} does not exist, copies
     * {@code defaults} into place first (when non-null).
     */
    public static YamlConfiguration loadOrCopyDefault(Path file, @Nullable InputStream defaults) {
        if (!Files.exists(file)) {
            try {
                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }
                if (defaults != null) {
                    Files.copy(defaults, file);
                } else {
                    Files.createFile(file);
                }
            } catch (IOException ex) {
                throw new ConfigurationException("Failed to materialise default config at " + file, ex);
            }
        }
        return load(file);
    }

    @Override
    public Path file() {
        return file;
    }

    @Override
    public void reload() {
        if (!Files.exists(file)) {
            this.root = new MapSection(new LinkedHashMap<>());
            return;
        }
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (loaded == null) {
                this.root = new MapSection(new LinkedHashMap<>());
            } else if (loaded instanceof Map<?, ?> map) {
                this.root = new MapSection(copyMap(map));
            } else {
                throw new ConfigurationException("Configuration root must be a YAML mapping: " + file);
            }
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to read config " + file, ex);
        } catch (RuntimeException ex) {
            throw new ConfigurationException("Failed to parse config " + file + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public void save() {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options);
        Path tmp;
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            tmp = Files.createTempFile(
                    file.getParent() == null ? Path.of(".") : file.getParent(),
                    file.getFileName().toString(),
                    ".tmp");
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to allocate temp file for " + file, ex);
        }
        try {
            try (OutputStream out = Files.newOutputStream(tmp);
                 var writer = new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                yaml.dump(root.backing, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException cleanupFailure) {
                LOGGER.warn("Failed to delete temp config file {}", tmp, cleanupFailure);
            }
            throw new ConfigurationException("Failed to save config " + file, ex);
        }
    }

    @Override
    public boolean contains(String path) {
        return root.contains(path);
    }

    @Override
    public Set<String> keys() {
        return root.keys();
    }

    @Override
    public @Nullable Object get(String path) {
        return root.get(path);
    }

    @Override
    public String getString(String path, String defaultValue) {
        return root.getString(path, defaultValue);
    }

    @Override
    public @Nullable String getString(String path) {
        return root.getString(path);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        return root.getInt(path, defaultValue);
    }

    @Override
    public long getLong(String path, long defaultValue) {
        return root.getLong(path, defaultValue);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        return root.getDouble(path, defaultValue);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return root.getBoolean(path, defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
        return root.getStringList(path);
    }

    @Override
    public ConfigurationSection getSection(String path) {
        return root.getSection(path);
    }

    @Override
    public void set(String path, @Nullable Object value) {
        root.set(path, value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Map<?, ?> source) {
        var copy = new LinkedHashMap<String, Object>(source.size());
        for (var entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            var key = entry.getKey().toString();
            var value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(key, copyMap(nested));
            } else if (value instanceof List<?> list) {
                copy.put(key, copyList(list));
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }

    private static List<Object> copyList(List<?> source) {
        var copy = new ArrayList<Object>(source.size());
        for (var element : source) {
            if (element instanceof Map<?, ?> nested) {
                copy.add(copyMap(nested));
            } else if (element instanceof List<?> list) {
                copy.add(copyList(list));
            } else {
                copy.add(element);
            }
        }
        return copy;
    }

    private static final class MapSection implements ConfigurationSection {

        final Map<String, Object> backing;

        MapSection(Map<String, Object> backing) {
            this.backing = backing;
        }

        @Override
        public boolean contains(String path) {
            var resolved = resolve(path, false);
            return resolved != null && resolved.parent.containsKey(resolved.leaf);
        }

        @Override
        public Set<String> keys() {
            return Set.copyOf(backing.keySet());
        }

        @Override
        public @Nullable Object get(String path) {
            var resolved = resolve(path, false);
            if (resolved == null) {
                return null;
            }
            var value = resolved.parent.get(resolved.leaf);
            if (value instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                var typed = (Map<String, Object>) map;
                return new MapSection(typed);
            }
            return value;
        }

        @Override
        public String getString(String path, String defaultValue) {
            var v = rawValue(path);
            return v == null ? defaultValue : v.toString();
        }

        @Override
        public @Nullable String getString(String path) {
            var v = rawValue(path);
            return v == null ? null : v.toString();
        }

        @Override
        public int getInt(String path, int defaultValue) {
            var v = rawValue(path);
            if (v instanceof Number n) {
                return n.intValue();
            }
            if (v instanceof String s) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException invalidNumber) {
                }
            }
            return defaultValue;
        }

        @Override
        public long getLong(String path, long defaultValue) {
            var v = rawValue(path);
            if (v instanceof Number n) {
                return n.longValue();
            }
            if (v instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException invalidNumber) {
                }
            }
            return defaultValue;
        }

        @Override
        public double getDouble(String path, double defaultValue) {
            var v = rawValue(path);
            if (v instanceof Number n) {
                return n.doubleValue();
            }
            if (v instanceof String s) {
                try {
                    return Double.parseDouble(s);
                } catch (NumberFormatException invalidNumber) {
                }
            }
            return defaultValue;
        }

        @Override
        public boolean getBoolean(String path, boolean defaultValue) {
            var v = rawValue(path);
            if (v instanceof Boolean b) {
                return b;
            }
            if (v instanceof String s) {
                if (s.equalsIgnoreCase("true")) {
                    return true;
                }
                if (s.equalsIgnoreCase("false")) {
                    return false;
                }
            }
            return defaultValue;
        }

        @Override
        public List<String> getStringList(String path) {
            var v = rawValue(path);
            if (v instanceof List<?> list) {
                var out = new ArrayList<String>(list.size());
                for (var element : list) {
                    out.add(element == null ? "null" : element.toString());
                }
                return List.copyOf(out);
            }
            return List.of();
        }

        @Override
        public ConfigurationSection getSection(String path) {
            var resolved = resolve(path, true);
            assert resolved != null;
            var existing = resolved.parent.get(resolved.leaf);
            if (existing instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                var typed = (Map<String, Object>) map;
                return new MapSection(typed);
            }
            var fresh = new LinkedHashMap<String, Object>();
            resolved.parent.put(resolved.leaf, fresh);
            return new MapSection(fresh);
        }

        @Override
        public void set(String path, @Nullable Object value) {
            var resolved = resolve(path, true);
            assert resolved != null;
            if (value == null) {
                resolved.parent.remove(resolved.leaf);
                return;
            }
            if (value instanceof MapSection nested) {
                resolved.parent.put(resolved.leaf, nested.backing);
            } else if (value instanceof Map<?, ?> map) {
                resolved.parent.put(resolved.leaf, copyMap(map));
            } else if (value instanceof List<?> list) {
                resolved.parent.put(resolved.leaf, copyList(list));
            } else {
                resolved.parent.put(resolved.leaf, value);
            }
        }

        private @Nullable Object rawValue(String path) {
            var resolved = resolve(path, false);
            if (resolved == null) {
                return null;
            }
            return resolved.parent.get(resolved.leaf);
        }

        private @Nullable Resolved resolve(String path, boolean createMissing) {
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("path must be non-empty");
            }
            var segments = path.split("\\.");
            Map<String, Object> current = backing;
            for (int i = 0; i < segments.length - 1; i++) {
                var segment = segments[i];
                var next = current.get(segment);
                if (next instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    var typed = (Map<String, Object>) map;
                    current = typed;
                } else if (createMissing) {
                    var fresh = new LinkedHashMap<String, Object>();
                    current.put(segment, fresh);
                    current = fresh;
                } else {
                    return null;
                }
            }
            return new Resolved(current, segments[segments.length - 1]);
        }

        private record Resolved(Map<String, Object> parent, String leaf) {}
    }
}
