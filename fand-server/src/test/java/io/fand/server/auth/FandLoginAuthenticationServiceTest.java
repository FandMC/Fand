package io.fand.server.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.authlib.GameProfile;
import io.fand.api.auth.LoginAuthenticationRequest;
import io.fand.api.auth.LoginAuthenticationResult;
import io.fand.api.player.PlayerProfile;
import io.fand.api.service.ServicePriority;
import java.net.InetSocketAddress;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandLoginAuthenticationServiceTest {

    @Test
    void usesHighestPriorityAuthenticatorAndFallsBackOnPass() {
        var service = new FandLoginAuthenticationService();
        var request = request();
        var low = new DemoAuthenticator("low", LoginAuthenticationResult.allow(playerProfile("low")));
        var high = new DemoAuthenticator("high", LoginAuthenticationResult.allow(playerProfile("high")));

        service.register(Key.key("demo:low"), low, ServicePriority.LOW, "low");
        service.register(Key.key("demo:high"), high, ServicePriority.HIGH, "high");

        var attempt = service.authenticate(request);

        assertThat(attempt.status()).isEqualTo(FandLoginAuthenticationService.LoginAttempt.Status.ALLOW);
        assertThat(attempt.profile()).isNotNull();
        assertThat(attempt.profile().name()).isEqualTo("high");
    }

    @Test
    void samePriorityUsesNewestAuthenticator() {
        var service = new FandLoginAuthenticationService();
        var request = request();

        service.register(Key.key("demo:first"), new DemoAuthenticator("first", LoginAuthenticationResult.pass()), ServicePriority.NORMAL, "first");
        service.register(Key.key("demo:second"), new DemoAuthenticator("second", LoginAuthenticationResult.allow(playerProfile("second"))), ServicePriority.NORMAL, "second");

        var attempt = service.authenticate(request);

        assertThat(attempt.status()).isEqualTo(FandLoginAuthenticationService.LoginAttempt.Status.ALLOW);
        assertThat(attempt.profile()).isNotNull();
        assertThat(attempt.profile().name()).isEqualTo("second");
    }

    @Test
    void passFallsThroughToBuiltinAuthenticator() {
        var service = new FandLoginAuthenticationService();
        var request = request();
        service.register(Key.key("demo:pass"), new DemoAuthenticator("pass", LoginAuthenticationResult.pass()), ServicePriority.NORMAL, "pass");
        service.builtin(ignored -> FandLoginAuthenticationService.LoginAttempt.allow(gameProfile("builtin")));

        var attempt = service.authenticate(request);

        assertThat(attempt.status()).isEqualTo(FandLoginAuthenticationService.LoginAttempt.Status.ALLOW);
        assertThat(attempt.profile()).isNotNull();
        assertThat(attempt.profile().name()).isEqualTo("builtin");
    }

    @Test
    void pluginOnlyAuthenticationReturnsPluginDecision() {
        var service = new FandLoginAuthenticationService();
        var request = request();
        service.register(Key.key("demo:offline"), new DemoAuthenticator("offline", LoginAuthenticationResult.allow(playerProfile("offline"))));
        service.builtin(ignored -> FandLoginAuthenticationService.LoginAttempt.deny(net.minecraft.network.chat.Component.literal("should not run")));

        var attempt = service.authenticatePlugins(request);

        assertThat(attempt.status()).isEqualTo(FandLoginAuthenticationService.LoginAttempt.Status.ALLOW);
        assertThat(attempt.profile()).isNotNull();
        assertThat(attempt.profile().name()).isEqualTo("offline");
    }

    @Test
    void pluginOnlyAuthenticationDoesNotUseBuiltinFallback() {
        var service = new FandLoginAuthenticationService();
        var request = request();
        service.register(Key.key("demo:pass"), new DemoAuthenticator("pass", LoginAuthenticationResult.pass()));
        service.builtin(ignored -> FandLoginAuthenticationService.LoginAttempt.allow(gameProfile("builtin")));

        var attempt = service.authenticatePlugins(request);

        assertThat(attempt.status()).isEqualTo(FandLoginAuthenticationService.LoginAttempt.Status.PASS);
    }

    private static LoginAuthenticationRequest request() {
        return new LoginAuthenticationRequest(
                "Tester",
                "server-id",
                new InetSocketAddress("127.0.0.1", 25565),
                null
        );
    }

    private static PlayerProfile playerProfile(String name) {
        return new PlayerProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }

    private static GameProfile gameProfile(String name) {
        return new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }

    private record DemoAuthenticator(String name, LoginAuthenticationResult result) implements io.fand.api.auth.LoginAuthenticator {
        @Override
        public LoginAuthenticationResult authenticate(LoginAuthenticationRequest request) {
            return result;
        }
    }
}
