package io.fand.api.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PermissionModelsTest {

    @Test
    void contextNormalizesKeysAndCopiesValues() {
        var context = new PermissionContext(Map.of(" World ", "overworld"));
        var expanded = context.with("SERVER", "lobby");

        assertThat(context.value("world")).contains("overworld");
        assertThat(context.value("WORLD")).contains("overworld");
        assertThat(context.contains("server")).isFalse();
        assertThat(expanded.values()).containsEntry("world", "overworld");
        assertThat(expanded.values()).containsEntry("server", "lobby");
        assertThat(expanded.without("world").value("world")).isEmpty();
    }

    @Test
    void contextRejectsBlankKeys() {
        assertThatThrownBy(() -> PermissionContext.of(" ", "world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key cannot be blank");
    }

    @Test
    void metaCarriesChatValuesAndGroups() {
        var meta = PermissionMeta.builder()
                .prefix("[Admin] ")
                .suffix(" *")
                .value("Vault:Balance", "100")
                .primaryGroup(" admin ")
                .group("vip")
                .group("admin")
                .build();

        assertThat(meta.prefix()).contains("[Admin] ");
        assertThat(meta.suffix()).contains(" *");
        assertThat(meta.value("vault:balance")).contains("100");
        assertThat(meta.primaryGroup()).contains("admin");
        assertThat(meta.groups()).containsExactly("admin", "vip");

        assertThat(meta.toBuilder().value("chat-color", "red").build().value("CHAT-COLOR"))
                .contains("red");
    }

    @Test
    void groupCarriesInheritanceAndGroupMeta() {
        var meta = PermissionMeta.builder().prefix("[Mod]").build();
        var group = PermissionGroup.builder(" moderator ")
                .displayName("Moderators")
                .parent("default")
                .parent("staff")
                .parent("default")
                .meta(meta)
                .build();

        assertThat(group.name()).isEqualTo("moderator");
        assertThat(group.displayName()).contains("Moderators");
        assertThat(group.parents()).containsExactly("default", "staff");
        assertThat(group.meta()).isSameAs(meta);
    }

    @Test
    void groupRejectsBlankNames() {
        assertThatThrownBy(() -> PermissionGroup.builder(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be blank");
        assertThatThrownBy(() -> PermissionGroup.builder("admin").parents(List.of("default", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be blank");
    }
}
