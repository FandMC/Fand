package io.fand.server.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class LoginThreadingSourceTest {

    @Test
    void pluginAuthenticationAndPreLoginEventsLeaveTheNettyEventLoop() throws IOException {
        var listener = Files.readString(
                Path.of("src/minecraft/java/net/minecraft/server/network/ServerLoginPacketListenerImpl.java"),
                StandardCharsets.UTF_8);

        assertThat(listener).contains(
                "this.startPluginAuthentication();",
                "this.startClientVerificationAsync(UUIDUtil.createOfflineProfile(this.requestedUsername));",
                "this.startClientVerificationAsync(profile);",
                "this.runAuthenticator(() -> this.startClientVerification(profile));",
                "new Thread(task, \"User Authenticator #\"");
    }
}
