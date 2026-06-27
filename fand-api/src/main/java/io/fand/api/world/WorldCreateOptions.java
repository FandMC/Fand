package io.fand.api.world;

import io.fand.api.world.generation.WorldGeneratorSettings;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Options used when creating a dynamic world.
 */
public final class WorldCreateOptions {

    private final WorldTemplate template;
    private final @Nullable WorldGenerator generator;
    private final WorldGeneratorSettings generatorSettings;
    private final boolean voidWorld;

    private WorldCreateOptions(
            WorldTemplate template,
            @Nullable WorldGenerator generator,
            WorldGeneratorSettings generatorSettings,
            boolean voidWorld
    ) {
        this.template = Objects.requireNonNull(template, "template");
        this.generator = generator;
        this.generatorSettings = Objects.requireNonNull(generatorSettings, "generatorSettings");
        this.voidWorld = voidWorld;
        if (voidWorld && generator != null) {
            throw new IllegalArgumentException("voidWorld and custom generator are mutually exclusive");
        }
    }

    public static WorldCreateOptions of(WorldTemplate template) {
        return new WorldCreateOptions(template, null, WorldGeneratorSettings.template(), false);
    }

    public static WorldCreateOptions voidWorld() {
        return new WorldCreateOptions(WorldTemplate.OVERWORLD, null, WorldGeneratorSettings.empty(), true);
    }

    public static WorldCreateOptions generated(WorldGenerator generator) {
        return generated(generator, WorldGeneratorSettings.custom());
    }

    public static WorldCreateOptions generated(WorldGenerator generator, WorldGeneratorSettings settings) {
        return new WorldCreateOptions(
                WorldTemplate.OVERWORLD,
                Objects.requireNonNull(generator, "generator"),
                Objects.requireNonNull(settings, "settings"),
                false);
    }

    public WorldTemplate template() {
        return template;
    }

    public Optional<WorldGenerator> generator() {
        return Optional.ofNullable(generator);
    }

    public WorldGeneratorSettings generatorSettings() {
        return generatorSettings;
    }

    public boolean isVoidWorld() {
        return voidWorld;
    }

    public WorldCreateOptions template(WorldTemplate template) {
        return new WorldCreateOptions(template, generator, generatorSettings, voidWorld);
    }

    public WorldCreateOptions generator(@Nullable WorldGenerator generator) {
        return new WorldCreateOptions(template, generator, generatorSettings, voidWorld);
    }

    public WorldCreateOptions generator(
            @Nullable WorldGenerator generator,
            WorldGeneratorSettings generatorSettings
    ) {
        return new WorldCreateOptions(template, generator, generatorSettings, voidWorld);
    }

    public WorldCreateOptions generatorSettings(WorldGeneratorSettings generatorSettings) {
        return new WorldCreateOptions(template, generator, generatorSettings, voidWorld);
    }

    public WorldCreateOptions voidWorld(boolean voidWorld) {
        return new WorldCreateOptions(template, generator, generatorSettings, voidWorld);
    }
}
