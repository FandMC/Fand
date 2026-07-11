package io.fand.api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.internal.FandRuntime;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class EntityTypesTest {

    private Server boundServer;

    @AfterEach
    void unbindServer() {
        if (boundServer != null) {
            FandRuntime.unbind(boundServer);
            boundServer = null;
        }
    }

    @Test
    void resolvesEntityTypesThroughBoundServer() {
        var zombie = new TestEntityType(EntityKey.ZOMBIE.key(), true, false);
        bindServer(key -> key.equals(EntityKey.ZOMBIE.key()) ? Optional.of(zombie) : Optional.empty());

        var found = EntityTypes.find(EntityKey.ZOMBIE);
        assertThat(found).isPresent();
        assertThat(found.get()).isSameAs(zombie);
        assertThat(EntityTypes.of(EntityKey.ZOMBIE)).isSameAs(zombie);
        assertThat(EntityTypes.of("minecraft:zombie")).isSameAs(zombie);
    }

    @Test
    void throwsForUnknownEntityType() {
        bindServer(key -> Optional.empty());

        assertThatThrownBy(() -> EntityTypes.of(Key.key("minecraft:not_real")))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("minecraft:not_real");
    }

    @Test
    void publicFandApiDoesNotExposeRuntimeBinding() {
        assertThat(Arrays.stream(Fand.class.getDeclaredMethods()).map(method -> method.getName()))
                .containsExactly("server");
    }

    private void bindServer(EntityTypeLookup lookup) {
        Object proxy = Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (instance, method, args) -> switch (method.getName()) {
                    case "entityType" -> lookup.find((Key) args[0]);
                    case "toString" -> "Server proxy";
                    case "hashCode" -> System.identityHashCode(instance);
                    case "equals" -> args != null && args.length == 1 && instance == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        boundServer = (Server) proxy;
        FandRuntime.bind(boundServer);
    }

    @FunctionalInterface
    private interface EntityTypeLookup {
        Optional<? extends EntityType> find(Key key);
    }

    private record TestEntityType(Key key, boolean spawnable, boolean player) implements EntityType {
    }
}
