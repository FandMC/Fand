package io.fand.api.event.entity;

import io.fand.api.entity.LivingEntity;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread when a living entity enters vanilla's death
 * sequence, before loot and experience are dropped.
 */
public final class EntityDeathEvent implements Event {

    private final LivingEntity entity;
    private final String cause;
    private final @Nullable LivingEntity directEntity;
    private final @Nullable LivingEntity attacker;

    public EntityDeathEvent(LivingEntity entity, String cause) {
        this(entity, cause, null, null);
    }

    public EntityDeathEvent(
            LivingEntity entity,
            String cause,
            @Nullable LivingEntity directEntity,
            @Nullable LivingEntity attacker) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.cause = Objects.requireNonNull(cause, "cause");
        this.directEntity = directEntity;
        this.attacker = attacker;
    }

    public LivingEntity entity() {
        return entity;
    }

    /** Vanilla damage-type identifier that caused the death. */
    public String cause() {
        return cause;
    }

    /** Direct living entity involved in the fatal damage, if vanilla exposes one. */
    public Optional<LivingEntity> directEntity() {
        return Optional.ofNullable(directEntity);
    }

    /** Living entity credited as causing the fatal damage, if vanilla exposes one. */
    public Optional<LivingEntity> attacker() {
        return Optional.ofNullable(attacker);
    }
}
