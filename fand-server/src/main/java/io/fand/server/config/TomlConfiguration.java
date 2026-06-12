package io.fand.server.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * TOML-backed {@link Configuration}. Comments and original formatting are not
 * preserved across {@link #save()}.
 */
public final class TomlConfiguration extends FileBackedConfiguration {

    public TomlConfiguration(Path file) {
        super(file, "TOML");
    }

    public static TomlConfiguration load(Path file) {
        var config = new TomlConfiguration(file);
        config.reload();
        return config;
    }

    public static TomlConfiguration loadOrCopyDefault(Path file, @Nullable InputStream defaults) {
        ConfigurationFiles.materialiseDefault(file, defaults);
        return load(file);
    }

    @Override
    protected Map<String, Object> readFile(Path file) throws IOException {
        if (Files.readString(file, StandardCharsets.UTF_8).isBlank()) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return configToMap(new TomlParser().parse(reader));
        }
    }

    @Override
    protected void writeFile(Path file, Map<String, Object> values) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new TomlWriter().write(toTomlConfig(values), writer);
        }
    }

    private static Map<String, Object> configToMap(UnmodifiableConfig config) {
        var out = new LinkedHashMap<String, Object>(config.size());
        for (var entry : config.entrySet()) {
            out.put(entry.getKey(), entry.isNull() ? null : normalise(entry.getRawValue()));
        }
        return out;
    }

    private static @Nullable Object normalise(@Nullable Object value) {
        if (value instanceof UnmodifiableConfig config) {
            return configToMap(config);
        }
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            var out = new ArrayList<Object>(list.size());
            for (var element : list) {
                out.add(normalise(element));
            }
            return out;
        }
        return value;
    }

    private static CommentedConfig toTomlConfig(Map<?, ?> values) {
        var config = TomlFormat.newConfig(LinkedHashMap::new);
        for (var entry : values.entrySet()) {
            var value = toTomlValue(entry.getValue(), false);
            if (value != null) {
                config.set(List.of(entry.getKey().toString()), value);
            }
        }
        return config;
    }

    private static @Nullable Object toTomlValue(@Nullable Object value, boolean insideList) {
        if (value == null) {
            if (insideList) {
                throw new ConfigurationException("TOML arrays cannot contain null values");
            }
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return toTomlConfig(map);
        }
        if (value instanceof List<?> list) {
            var out = new ArrayList<Object>(list.size());
            for (var element : list) {
                out.add(toTomlValue(element, true));
            }
            return out;
        }
        return value;
    }
}
