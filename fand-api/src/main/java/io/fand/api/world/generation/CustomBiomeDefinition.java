package io.fand.api.world.generation;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Full biome definition supplied by a custom {@link BiomeProvider}.
 */
public record CustomBiomeDefinition(
        Key key,
        float temperature,
        float downfall,
        Precipitation precipitation,
        BiomeColors colors,
        List<BiomeFeatureReference> features,
        JsonObject extraJson
) {

    public CustomBiomeDefinition {
        Objects.requireNonNull(key, "key");
        if (temperature < -2.0F || temperature > 2.0F) {
            throw new IllegalArgumentException("temperature must be between -2.0 and 2.0");
        }
        if (downfall < 0.0F || downfall > 1.0F) {
            throw new IllegalArgumentException("downfall must be between 0.0 and 1.0");
        }
        precipitation = Objects.requireNonNull(precipitation, "precipitation");
        colors = Objects.requireNonNull(colors, "colors");
        features = List.copyOf(Objects.requireNonNull(features, "features"));
        extraJson = extraJson == null ? new JsonObject() : extraJson.deepCopy();
    }

    public static Builder builder(Key key) {
        return new Builder(key);
    }

    public JsonObject extraJson() {
        return extraJson.deepCopy();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public enum Precipitation {
        NONE,
        RAIN,
        SNOW
    }

    public static final class Builder {
        private Key key;
        private float temperature = 0.8F;
        private float downfall = 0.4F;
        private Precipitation precipitation = Precipitation.RAIN;
        private BiomeColors colors = BiomeColors.defaults();
        private List<BiomeFeatureReference> features = List.of();
        private JsonObject extraJson = new JsonObject();

        private Builder(Key key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        private Builder(CustomBiomeDefinition definition) {
            this.key = definition.key;
            this.temperature = definition.temperature;
            this.downfall = definition.downfall;
            this.precipitation = definition.precipitation;
            this.colors = definition.colors;
            this.features = definition.features;
            this.extraJson = definition.extraJson.deepCopy();
        }

        public Builder key(Key key) {
            this.key = Objects.requireNonNull(key, "key");
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }

        public Builder precipitation(Precipitation precipitation) {
            this.precipitation = Objects.requireNonNull(precipitation, "precipitation");
            return this;
        }

        public Builder colors(BiomeColors colors) {
            this.colors = Objects.requireNonNull(colors, "colors");
            return this;
        }

        public Builder features(List<BiomeFeatureReference> features) {
            this.features = List.copyOf(Objects.requireNonNull(features, "features"));
            return this;
        }

        public Builder addFeature(DecorationStep step, Key placedFeature) {
            var copy = new java.util.ArrayList<>(features);
            copy.add(new BiomeFeatureReference(step, placedFeature));
            this.features = List.copyOf(copy);
            return this;
        }

        public Builder extraJson(@Nullable JsonObject extraJson) {
            this.extraJson = extraJson == null ? new JsonObject() : extraJson.deepCopy();
            return this;
        }

        public CustomBiomeDefinition build() {
            return new CustomBiomeDefinition(key, temperature, downfall, precipitation, colors, features, extraJson);
        }
    }
}
