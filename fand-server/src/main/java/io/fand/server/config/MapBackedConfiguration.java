package io.fand.server.config;

import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationSection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

abstract class MapBackedConfiguration implements Configuration {

    private MapSection root = new MapSection(new LinkedHashMap<>());

    protected final Map<String, Object> rootValues() {
        return root.backing;
    }

    protected final void replaceRoot(Map<?, ?> values) {
        this.root = new MapSection(copyMap(values));
    }

    protected final void clearRoot() {
        this.root = new MapSection(new LinkedHashMap<>());
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
    public @Nullable Object value(String path) {
        return root.value(path);
    }

    @Override
    public String string(String path, String defaultValue) {
        return root.string(path, defaultValue);
    }

    @Override
    public @Nullable String string(String path) {
        return root.string(path);
    }

    @Override
    public int intValue(String path, int defaultValue) {
        return root.intValue(path, defaultValue);
    }

    @Override
    public long longValue(String path, long defaultValue) {
        return root.longValue(path, defaultValue);
    }

    @Override
    public double doubleValue(String path, double defaultValue) {
        return root.doubleValue(path, defaultValue);
    }

    @Override
    public boolean booleanValue(String path, boolean defaultValue) {
        return root.booleanValue(path, defaultValue);
    }

    @Override
    public List<String> stringList(String path) {
        return root.stringList(path);
    }

    @Override
    public ConfigurationSection section(String path) {
        return root.section(path);
    }

    @Override
    public void set(String path, @Nullable Object value) {
        root.set(path, value);
    }

    static Map<String, Object> copyMap(Map<?, ?> source) {
        var copy = new LinkedHashMap<String, Object>(source.size());
        for (var entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            copy.put(entry.getKey().toString(), copyValue(entry.getValue()));
        }
        return copy;
    }

    static List<Object> copyList(List<?> source) {
        var copy = new ArrayList<Object>(source.size());
        for (var element : source) {
            copy.add(copyValue(element));
        }
        return copy;
    }

    private static @Nullable Object copyValue(@Nullable Object value) {
        if (value instanceof MapSection section) {
            return copyMap(section.backing);
        }
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            return copyList(list);
        }
        return value;
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
        public @Nullable Object value(String path) {
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
        public String string(String path, String defaultValue) {
            var value = rawValue(path);
            return value == null ? defaultValue : value.toString();
        }

        @Override
        public @Nullable String string(String path) {
            var value = rawValue(path);
            return value == null ? null : value.toString();
        }

        @Override
        public int intValue(String path, int defaultValue) {
            var value = rawValue(path);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException invalidNumber) {
                }
            }
            return defaultValue;
        }

        @Override
        public long longValue(String path, long defaultValue) {
            var value = rawValue(path);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException invalidNumber) {
                }
            }
            return defaultValue;
        }

        @Override
        public double doubleValue(String path, double defaultValue) {
            var value = rawValue(path);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String text) {
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException invalidNumber) {
                }
            }
            return defaultValue;
        }

        @Override
        public boolean booleanValue(String path, boolean defaultValue) {
            var value = rawValue(path);
            if (value instanceof Boolean flag) {
                return flag;
            }
            if (value instanceof String text) {
                if (text.equalsIgnoreCase("true")) {
                    return true;
                }
                if (text.equalsIgnoreCase("false")) {
                    return false;
                }
            }
            return defaultValue;
        }

        @Override
        public List<String> stringList(String path) {
            var value = rawValue(path);
            if (value instanceof List<?> list) {
                var out = new ArrayList<String>(list.size());
                for (var element : list) {
                    out.add(element == null ? "null" : element.toString());
                }
                return List.copyOf(out);
            }
            return List.of();
        }

        @Override
        public ConfigurationSection section(String path) {
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
            resolved.parent.put(resolved.leaf, copyValue(value));
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
