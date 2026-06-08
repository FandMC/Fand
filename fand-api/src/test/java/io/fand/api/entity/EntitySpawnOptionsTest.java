package io.fand.api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.fand.api.world.Vector3;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class EntitySpawnOptionsTest {

    @Test
    void defaultsAreEmpty() {
        var options = EntitySpawnOptions.defaults();

        assertThat(options.velocity()).isNull();
        assertThat(options.projectileDirection()).isNull();
        assertThat(options.persistent()).isNull();
        assertThat(options.pickupDelay()).isNull();
        assertThat(options.unlimitedLifetime()).isFalse();
    }

    @Test
    void builderStoresRequestedValues() {
        var velocity = new Vector3(0.1, 0.2, 0.3);
        var direction = new Vector3(1.0, 0.0, 0.0);
        var name = Component.text("Demo");

        var options = EntitySpawnOptions.builder()
                .velocity(velocity)
                .projectile(direction, 2.0, 0.25)
                .persistent(true)
                .noAi(true)
                .pickupDelay(40)
                .unlimitedLifetime(true)
                .customName(name)
                .customNameVisible(true)
                .glowing(true)
                .silent(true)
                .gravity(false)
                .invulnerable(true)
                .fireTicks(100)
                .build();

        assertThat(options.velocity()).isSameAs(velocity);
        assertThat(options.projectileDirection()).isSameAs(direction);
        assertThat(options.projectilePower()).isEqualTo(2.0);
        assertThat(options.projectileUncertainty()).isEqualTo(0.25);
        assertThat(options.persistent()).isTrue();
        assertThat(options.noAi()).isTrue();
        assertThat(options.pickupDelay()).isEqualTo(40);
        assertThat(options.unlimitedLifetime()).isTrue();
        assertThat(options.customName()).isSameAs(name);
        assertThat(options.customNameVisible()).isTrue();
        assertThat(options.glowing()).isTrue();
        assertThat(options.silent()).isTrue();
        assertThat(options.gravity()).isFalse();
        assertThat(options.invulnerable()).isTrue();
        assertThat(options.fireTicks()).isEqualTo(100);
    }

    @Test
    void rejectsInvalidNumbers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().velocity(new Vector3(Double.NaN, 0, 0)).build());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().projectile(new Vector3(1, Double.POSITIVE_INFINITY, 0), 1.0, 0.0).build());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().projectile(new Vector3(1, 0, 0), -1.0, 0.0).build());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().projectile(new Vector3(1, 0, 0), Double.NaN, 0.0).build());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().projectile(new Vector3(1, 0, 0), 1.0, -1.0).build());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().pickupDelay(-1).build());
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EntitySpawnOptions.builder().fireTicks(-1).build());
    }
}
