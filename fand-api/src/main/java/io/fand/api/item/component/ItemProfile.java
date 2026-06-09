package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:profile}. */
public final class ItemProfile implements ItemComponentData {

    private final @Nullable String name;
    private final @Nullable UUID id;
    private final List<ItemProfile.Property> properties;
    private final @Nullable Key texture;
    private final @Nullable Key cape;
    private final @Nullable Key elytra;
    private final @Nullable String model;

    public ItemProfile(
            @Nullable String name,
            @Nullable UUID id,
            List<ItemProfile.Property> properties,
            @Nullable Key texture,
            @Nullable Key cape,
            @Nullable Key elytra,
            @Nullable String model) {
        this.name = name;
        this.id = id;
        this.properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
        this.texture = texture;
        this.cape = cape;
        this.elytra = elytra;
        this.model = model;
    }

    public static ItemProfile named(String name) {
        return new ItemProfile(Objects.requireNonNull(name, "name"), null, List.of(), null, null, null, null);
    }

    public static ItemProfile fromJson(JsonElement value) {
        Objects.requireNonNull(value, "value");
        if (value.isJsonPrimitive()) {
            return named(value.getAsString());
        }
        var object = ItemComponentJson.object(value, "profile");
        return new ItemProfile(
                ItemComponentJson.optionalString(object, "name").orElse(null),
                ItemComponentJson.optionalString(object, "id").map(UUID::fromString).orElse(null),
                propertiesFromJson(object.get("properties")),
                ItemComponentJson.optionalKey(object, "texture").orElse(null),
                ItemComponentJson.optionalKey(object, "cape").orElse(null),
                ItemComponentJson.optionalKey(object, "elytra").orElse(null),
                ItemComponentJson.optionalString(object, "model").orElse(null));
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public Optional<UUID> id() {
        return Optional.ofNullable(id);
    }

    public List<ItemProfile.Property> properties() {
        return properties;
    }

    public Optional<Key> texture() {
        return Optional.ofNullable(texture);
    }

    public Optional<Key> cape() {
        return Optional.ofNullable(cape);
    }

    public Optional<Key> elytra() {
        return Optional.ofNullable(elytra);
    }

    public Optional<String> model() {
        return Optional.ofNullable(model);
    }

    @Override
    public JsonElement toJson() {
        if (name != null && id == null && properties.isEmpty() && texture == null && cape == null && elytra == null && model == null) {
            return new JsonPrimitive(name);
        }
        var json = new JsonObject();
        if (name != null) {
            json.addProperty("name", name);
        }
        if (id != null) {
            json.addProperty("id", id.toString());
        }
        if (!properties.isEmpty()) {
            var propertyArray = new com.google.gson.JsonArray();
            properties.forEach(property -> propertyArray.add(property.toJson()));
            json.add("properties", propertyArray);
        }
        if (texture != null) {
            json.addProperty("texture", texture.asString());
        }
        if (cape != null) {
            json.addProperty("cape", cape.asString());
        }
        if (elytra != null) {
            json.addProperty("elytra", elytra.asString());
        }
        if (model != null) {
            json.addProperty("model", model);
        }
        return json;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemProfile that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && Objects.equals(id, that.id)
                && properties.equals(that.properties)
                && Objects.equals(texture, that.texture)
                && Objects.equals(cape, that.cape)
                && Objects.equals(elytra, that.elytra)
                && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, properties, texture, cape, elytra, model);
    }

    @Override
    public String toString() {
        return "ItemProfile[name=" + name()
                + ", id=" + id()
                + ", properties=" + properties
                + ", texture=" + texture()
                + ", cape=" + cape()
                + ", elytra=" + elytra()
                + ", model=" + model()
                + "]";
    }

    private static List<Property> propertiesFromJson(JsonElement value) {
        if (value == null) {
            return List.of();
        }
        var properties = new ArrayList<Property>();
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(property -> properties.add(Property.fromJson(property)));
            return List.copyOf(properties);
        }
        if (!value.isJsonObject()) {
            return List.of();
        }
        for (var entry : value.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                properties.add(Property.unsigned(entry.getKey(), entry.getValue().getAsString()));
            } else if (entry.getValue().isJsonArray()) {
                entry.getValue().getAsJsonArray().forEach(property -> {
                    if (property.isJsonPrimitive()) {
                        properties.add(Property.unsigned(entry.getKey(), property.getAsString()));
                    }
                });
            }
        }
        return List.copyOf(properties);
    }

    /** Mojang profile property entry, usually used for signed skin textures. */
    public static final class Property implements ItemComponentData {

        private final String name;
        private final String value;
        private final @Nullable String signature;

        public Property(String name, String value, @Nullable String signature) {
            this.name = Objects.requireNonNull(name, "name");
            this.value = Objects.requireNonNull(value, "value");
            this.signature = signature;
        }

        public static Property unsigned(String name, String value) {
            return new Property(name, value, null);
        }

        public static Property signed(String name, String value, String signature) {
            return new Property(name, value, Objects.requireNonNull(signature, "signature"));
        }

        public static Property fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "profile property");
            return new Property(
                    ItemComponentJson.optionalString(object, "name")
                            .orElseThrow(() -> new IllegalArgumentException("profile property name is required")),
                    ItemComponentJson.optionalString(object, "value")
                            .orElseThrow(() -> new IllegalArgumentException("profile property value is required")),
                    ItemComponentJson.optionalString(object, "signature").orElse(null));
        }

        public String name() {
            return name;
        }

        public String value() {
            return value;
        }

        public Optional<String> signature() {
            return Optional.ofNullable(signature);
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("name", name);
            json.addProperty("value", value);
            if (signature != null) {
                json.addProperty("signature", signature);
            }
            return json;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Property that)) {
                return false;
            }
            return name.equals(that.name)
                    && value.equals(that.value)
                    && Objects.equals(signature, that.signature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, signature);
        }

        @Override
        public String toString() {
            return "Property[name=" + name + ", value=" + value + ", signature=" + signature() + "]";
        }
    }
}
