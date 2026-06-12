package io.fand.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.fand.api.config.Configuration;
import io.fand.api.config.ConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * JSON-backed {@link Configuration}. Comments are not supported by JSON and
 * object formatting is normalised on {@link #save()}.
 */
public final class JsonConfiguration extends FileBackedConfiguration {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public JsonConfiguration(Path file) {
        super(file, "JSON");
    }

    public static JsonConfiguration load(Path file) {
        var config = new JsonConfiguration(file);
        config.reload();
        return config;
    }

    public static JsonConfiguration loadOrCopyDefault(Path file, @Nullable InputStream defaults) {
        ConfigurationFiles.materialiseDefault(file, defaults);
        return load(file);
    }

    @Override
    protected Map<String, Object> readFile(Path file) throws IOException {
        var content = Files.readString(file, StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            var element = JsonParser.parseString(content);
            if (element == null || element instanceof JsonNull) {
                return new LinkedHashMap<>();
            }
            if (!element.isJsonObject()) {
                throw new ConfigurationException("Configuration root must be a JSON object: " + file);
            }
            return jsonObjectToMap(element.getAsJsonObject());
        } catch (JsonParseException ex) {
            throw new ConfigurationException("Failed to parse JSON config " + file + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void writeFile(Path file, Map<String, Object> values) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(values, writer);
            writer.write(System.lineSeparator());
        }
    }

    private static Map<String, Object> jsonObjectToMap(JsonObject object) {
        var out = new LinkedHashMap<String, Object>(object.size());
        for (var entry : object.entrySet()) {
            out.put(entry.getKey(), jsonToValue(entry.getValue()));
        }
        return out;
    }

    private static @Nullable Object jsonToValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            return jsonObjectToMap(element.getAsJsonObject());
        }
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            var out = new ArrayList<Object>(array.size());
            for (var child : array) {
                out.add(jsonToValue(child));
            }
            return out;
        }
        if (element.isJsonPrimitive()) {
            return primitiveToValue(element.getAsJsonPrimitive());
        }
        return element.toString();
    }

    private static Object primitiveToValue(JsonPrimitive primitive) {
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isString()) {
            return primitive.getAsString();
        }
        if (primitive.isNumber()) {
            var decimal = primitive.getAsBigDecimal();
            return decimal.stripTrailingZeros().scale() <= 0 ? integralNumber(decimal) : primitive.getAsDouble();
        }
        return primitive.getAsString();
    }

    private static Number integralNumber(BigDecimal value) {
        try {
            var number = value.longValueExact();
            return number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE ? (int) number : number;
        } catch (ArithmeticException outOfRange) {
            return value;
        }
    }
}
