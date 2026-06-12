package io.fand.server.config;

import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationFormat;
import io.fand.api.config.ConfigurationService;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class FandConfigurationService implements ConfigurationService {

    public static final FandConfigurationService INSTANCE = new FandConfigurationService();

    private FandConfigurationService() {
    }

    @Override
    public Configuration load(Path file) {
        return load(file, ConfigurationFormat.detect(file));
    }

    @Override
    public Configuration load(Path file, ConfigurationFormat format) {
        Objects.requireNonNull(format, "format");
        return switch (format) {
            case YAML -> YamlConfiguration.load(file);
            case JSON -> JsonConfiguration.load(file);
            case TOML -> TomlConfiguration.load(file);
            case PROPERTIES -> PropertiesConfiguration.load(file);
        };
    }

    @Override
    public Configuration loadOrCopyDefault(Path file, @Nullable InputStream defaults) {
        return loadOrCopyDefault(file, ConfigurationFormat.detect(file), defaults);
    }

    @Override
    public Configuration loadOrCopyDefault(Path file, ConfigurationFormat format, @Nullable InputStream defaults) {
        Objects.requireNonNull(format, "format");
        return switch (format) {
            case YAML -> YamlConfiguration.loadOrCopyDefault(file, defaults);
            case JSON -> JsonConfiguration.loadOrCopyDefault(file, defaults);
            case TOML -> TomlConfiguration.loadOrCopyDefault(file, defaults);
            case PROPERTIES -> PropertiesConfiguration.loadOrCopyDefault(file, defaults);
        };
    }
}
