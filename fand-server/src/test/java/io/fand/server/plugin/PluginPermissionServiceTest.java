package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.server.permission.PermissionManager;
import org.junit.jupiter.api.Test;

final class PluginPermissionServiceTest {

    @Test
    void pluginCanOnlyRegisterOwnPermissionNodes() {
        var delegate = new PermissionManager();
        var permissions = new PluginPermissionService(delegate, "demo");

        permissions.register(new PermissionDescriptor("demo.command.use", PermissionDefault.FALSE));

        assertThat(delegate.lookup("demo.command.use")).isPresent();
        assertThatThrownBy(() -> permissions.register(new PermissionDescriptor("other.command.use", PermissionDefault.TRUE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demo");
        assertThat(delegate.lookup("other.command.use")).isEmpty();
    }
}
