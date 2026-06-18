package io.fand.server.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class YamlConfigLoader<T> {

    private final Class<T> rootType;
    private final Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    public YamlConfigLoader(Class<T> rootType) {
        this.rootType = rootType;
    }

    public T load(Path path) {
        var config = instantiate(rootType);
        if (Files.exists(path)) {
            try {
                Object loaded = yaml.load(Files.readString(path));
                if (loaded instanceof Map<?, ?> map) {
                    bindObject(config, map, rootType.getSimpleName());
                } else if (loaded != null) {
                    throw new ConfigException("Config root must be a YAML mapping");
                }
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to read config from " + path, ex);
            }
        }
        write(path, config);
        return config;
    }

    public void save(Path path, T config) {
        write(path, config);
    }

    private void write(Path path, T config) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            var lines = new ArrayList<String>();
            writeObject(lines, config, 0);
            Files.writeString(path, String.join(System.lineSeparator(), lines) + System.lineSeparator());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write config to " + path, ex);
        }
    }

    private void bindObject(Object target, Map<?, ?> values, String path) {
        for (var field : configFields(target.getClass())) {
            Object rawValue = values.get(field.getName());
            if (rawValue == null) {
                continue;
            }
            var fieldPath = path + "." + field.getName();
            try {
                if (isSection(field.getType())) {
                    if (!(rawValue instanceof Map<?, ?> nested)) {
                        throw new ConfigException(fieldPath + " must be a section");
                    }
                    var section = field.get(target);
                    if (section == null) {
                        section = instantiate(field.getType());
                        field.set(target, section);
                    }
                    bindObject(section, nested, fieldPath);
                } else {
                    var converted = convertValue(field, rawValue, fieldPath);
                    validateRange(field, converted, fieldPath);
                    field.set(target, converted);
                }
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Failed to access config field " + fieldPath, ex);
            }
        }
    }

    private Object convertValue(Field field, Object rawValue, String path) {
        var type = field.getType();
        try {
            if (type == String.class) {
                return String.valueOf(rawValue);
            }
            if (type == int.class || type == Integer.class) {
                return toNumber(rawValue, path).intValueExact();
            }
            if (type == long.class || type == Long.class) {
                return toNumber(rawValue, path).longValueExact();
            }
            if (type == boolean.class || type == Boolean.class) {
                if (rawValue instanceof Boolean value) {
                    return value;
                }
                throw new ConfigException(path + " must be a boolean");
            }
        } catch (ArithmeticException ex) {
            throw new ConfigException(path + " is out of range", ex);
        }
        throw new ConfigException("Unsupported config field type: " + type.getName());
    }

    private static java.math.BigDecimal toNumber(Object rawValue, String path) {
        if (rawValue instanceof Number value) {
            return new java.math.BigDecimal(value.toString());
        }
        throw new ConfigException(path + " must be a number");
    }

    private void validateRange(Field field, Object converted, String path) {
        var range = field.getAnnotation(ConfigRange.class);
        if (range == null) {
            return;
        }
        long value;
        if (converted instanceof Integer number) {
            value = number.longValue();
        } else if (converted instanceof Long number) {
            value = number;
        } else {
            throw new ConfigException(path + " cannot use @ConfigRange on non-integer values");
        }
        if (value < range.min() || value > range.max()) {
            throw new ConfigException(path + " must be between " + range.min() + " and " + range.max());
        }
    }

    private void writeObject(List<String> lines, Object target, int indent) {
        for (var field : configFields(target.getClass())) {
            var comment = field.getAnnotation(ConfigComment.class);
            if (comment != null) {
                for (var line : comment.value()) {
                    lines.add(indent(indent) + "# " + line);
                }
            }
            try {
                var value = field.get(target);
                if (value == null) {
                    continue;
                }
                if (isSection(field.getType())) {
                    lines.add(indent(indent) + field.getName() + ":");
                    writeObject(lines, value, indent + 2);
                } else {
                    lines.add(indent(indent) + field.getName() + ": " + formatScalar(value));
                }
                lines.add("");
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Failed to access config field " + field.getName(), ex);
            }
        }
        while (!lines.isEmpty() && lines.getLast().isBlank()) {
            lines.removeLast();
        }
    }

    private static String formatScalar(Object value) {
        if (value instanceof String text) {
            if (text.isEmpty() || text.matches(".*[:#\\s].*")) {
                return "'" + text.replace("'", "''") + "'";
            }
            return text;
        }
        return String.valueOf(value);
    }

    private static boolean isSection(Class<?> type) {
        return !type.isPrimitive()
                && type != String.class
                && type != Integer.class
                && type != Long.class
                && type != Boolean.class
                && !Number.class.isAssignableFrom(type)
                && !type.isEnum();
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Config type must have a no-arg constructor: " + type.getName(), ex);
        }
    }

    private static List<Field> configFields(Class<?> type) {
        var fields = new ArrayList<Field>();
        for (var field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
        }
        return fields;
    }

    private static String indent(int spaces) {
        return " ".repeat(spaces);
    }
}
