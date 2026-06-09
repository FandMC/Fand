package io.fand.api.world;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Options used when creating a dynamic world.
 */
public final class WorldCreateOptions {

    private final WorldTemplate template;
    private final @Nullable WorldGenerator generator;
    private final boolean voidWorld;

    private WorldCreateOptions(WorldTemplate template, @Nullable WorldGenerator generator, boolean voidWorld) {
        this.template = Objects.requireNonNull(template, "template");
        this.generator = generator;
        this.voidWorld = voidWorld;
        if (voidWorld && generator != null) {
            throw new IllegalArgumentException("voidWorld and custom generator are mutually exclusive");
        }
    }

    public static WorldCreateOptions of(WorldTemplate template) {
        return new WorldCreateOptions(template, null, false);
    }

    public static WorldCreateOptions voidWorld() {
        return new WorldCreateOptions(WorldTemplate.OVERWORLD, null, true);
    }

    public static WorldCreateOptions generated(WorldGenerator generator) {
        return new WorldCreateOptions(WorldTemplate.OVERWORLD, Objects.requireNonNull(generator, "generator"), false);
    }

    public WorldTemplate template() {
        return template;
    }

    public Optional<WorldGenerator> generator() {
        return Optional.ofNullable(generator);
    }

    public boolean isVoidWorld() {
        return voidWorld;
    }

    public WorldCreateOptions template(WorldTemplate template) {
        return new WorldCreateOptions(template, generator, voidWorld);
    }

    public WorldCreateOptions generator(@Nullable WorldGenerator generator) {
        return new WorldCreateOptions(template, generator, voidWorld);
    }

    public WorldCreateOptions voidWorld(boolean voidWorld) {
        return new WorldCreateOptions(template, generator, voidWorld);
    }
}
