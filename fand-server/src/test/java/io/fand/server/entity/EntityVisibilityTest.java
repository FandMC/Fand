package io.fand.server.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

final class EntityVisibilityTest {

    @Test
    void resolvesTrackedEntityMembers() {
        assertDoesNotThrow(() -> Class.forName(
                EntityVisibility.class.getName(),
                true,
                EntityVisibility.class.getClassLoader()));
    }
}
