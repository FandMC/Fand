package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.AttributeKey;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityAttributesTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void resolvesGeneratedAttributeKeys() {
        assertThat(EntityAttributes.holder(AttributeKey.MAX_HEALTH.key())).isPresent();
        assertThat(EntityAttributes.holder(AttributeKey.MOVEMENT_SPEED.key())).isPresent();
    }
}
