package io.fand.api.entity;

import io.fand.api.tag.Tag;
import io.fand.api.tag.Tags;
import java.util.Collection;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Vanilla entity type exposed without leaking server implementation classes.
 */
public interface EntityType {

    Key key();

    default boolean is(Tag<EntityType> tag) {
        return Objects.requireNonNull(tag, "tag").contains(this);
    }

    default boolean is(EntityTypeTagKey tag) {
        return Tags.entityType(tag).map(candidate -> candidate.contains(this)).orElse(false);
    }

    default Collection<? extends Tag<EntityType>> tags() {
        return Tags.entityTypes().stream()
                .filter(tag -> tag.contains(this))
                .toList();
    }

    boolean spawnable();

    boolean player();
}
