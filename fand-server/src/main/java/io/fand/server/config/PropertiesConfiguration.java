package io.fand.server.config;

import io.fand.api.config.Configuration;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.Nullable;

/**
 * Java properties-backed {@link Configuration}. Nested sections are represented
 * by dot-separated keys. Lists are represented as indexed keys, for example
 * {@code flags.0=one} and {@code flags.1=two}.
 */
public final class PropertiesConfiguration extends FileBackedConfiguration {

    private static final String LINE_SEPARATOR = System.lineSeparator();

    public PropertiesConfiguration(Path file) {
        super(file, "properties");
    }

    public static PropertiesConfiguration load(Path file) {
        var config = new PropertiesConfiguration(file);
        config.reload();
        return config;
    }

    public static PropertiesConfiguration loadOrCopyDefault(Path file, @Nullable InputStream defaults) {
        ConfigurationFiles.materialiseDefault(file, defaults);
        return load(file);
    }

    @Override
    protected Map<String, Object> readFile(Path file) throws IOException {
        var properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        var root = new LinkedHashMap<String, Object>();
        var names = new ArrayList<>(properties.stringPropertyNames());
        Collections.sort(names);
        for (var name : names) {
            putPath(root, name, properties.getProperty(name));
        }
        return collapseChildren(root);
    }

    @Override
    protected void writeFile(Path file, Map<String, Object> values) throws IOException {
        var flattened = new LinkedHashMap<String, String>();
        flatten("", values, flattened);
        var entries = new ArrayList<>(flattened.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (var entry : entries) {
                writer.write(escape(entry.getKey(), true));
                writer.write("=");
                writer.write(escape(entry.getValue(), false));
                writer.write(LINE_SEPARATOR);
            }
        }
    }

    private static void putPath(Map<String, Object> root, String path, String value) {
        var segments = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < segments.length - 1; i++) {
            var segment = segments[i];
            var next = current.get(segment);
            if (next instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                var typed = (Map<String, Object>) map;
                current = typed;
            } else {
                var fresh = new LinkedHashMap<String, Object>();
                current.put(segment, fresh);
                current = fresh;
            }
        }
        current.put(segments[segments.length - 1], value);
    }

    private static Object collapseIndexedSections(Object value) {
        if (value instanceof Map<?, ?> map) {
            var collapsed = new LinkedHashMap<String, Object>(map.size());
            for (var entry : map.entrySet()) {
                collapsed.put(entry.getKey().toString(), collapseIndexedSections(entry.getValue()));
            }
            var list = asIndexedList(collapsed);
            return list == null ? collapsed : list;
        }
        return value;
    }

    private static Map<String, Object> collapseChildren(Map<String, Object> root) {
        var collapsed = new LinkedHashMap<String, Object>(root.size());
        for (var entry : root.entrySet()) {
            collapsed.put(entry.getKey(), collapseIndexedSections(entry.getValue()));
        }
        return collapsed;
    }

    private static @Nullable List<Object> asIndexedList(Map<String, Object> map) {
        if (map.isEmpty()) {
            return null;
        }
        var indexed = new ArrayList<Map.Entry<Integer, Object>>(map.size());
        for (var entry : map.entrySet()) {
            try {
                var index = Integer.parseInt(entry.getKey());
                if (index < 0) {
                    return null;
                }
                indexed.add(Map.entry(index, entry.getValue()));
            } catch (NumberFormatException notIndexed) {
                return null;
            }
        }
        indexed.sort(Comparator.comparingInt(Map.Entry::getKey));
        for (int i = 0; i < indexed.size(); i++) {
            if (indexed.get(i).getKey() != i) {
                return null;
            }
        }
        var list = new ArrayList<Object>(indexed.size());
        for (var entry : indexed) {
            list.add(entry.getValue());
        }
        return list;
    }

    private static void flatten(String prefix, Object value, Map<String, String> out) {
        if (value == null) {
            if (!prefix.isEmpty()) {
                out.put(prefix, "null");
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                var key = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey();
                flatten(key, entry.getValue(), out);
            }
            return;
        }
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                flatten(prefix + "." + i, list.get(i), out);
            }
            return;
        }
        out.put(prefix, value.toString());
    }

    private static String escape(String value, boolean key) {
        var out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            var ch = value.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '\t' -> out.append("\\t");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\f' -> out.append("\\f");
                case ' ', ':', '=', '#', '!' -> {
                    if (key || i == 0) {
                        out.append('\\');
                    }
                    out.append(ch);
                }
                default -> out.append(ch);
            }
        }
        return out.toString();
    }
}
