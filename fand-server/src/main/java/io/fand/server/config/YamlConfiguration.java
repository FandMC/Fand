package io.fand.server.config;

import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
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
public final class YamlConfiguration extends FileBackedConfiguration {

    public YamlConfiguration(Path file) {
        super(file, "YAML");
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
        ConfigurationFiles.materialiseDefault(file, defaults);
        return load(file);
    }

    @Override
    protected Map<String, Object> readFile(Path file) throws IOException {
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (loaded == null) {
                return new LinkedHashMap<>();
            }
            if (loaded instanceof Map<?, ?> map) {
                return copyMap(map);
            }
            throw new ConfigurationException("Configuration root must be a YAML mapping: " + file);
        }
    }

    @Override
    protected void writeFile(Path file, Map<String, Object> values) throws IOException {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        var yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options);
        try (var writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            yaml.dump(values, writer);
        }
    }
}
