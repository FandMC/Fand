package io.fand.server.auth;

import io.fand.api.auth.LoginAuthenticationRegistration;
import io.fand.api.auth.LoginAuthenticationRequest;
import io.fand.api.auth.LoginAuthenticationResult;
import io.fand.api.auth.LoginAuthenticationService;
import io.fand.api.auth.LoginAuthenticator;
import io.fand.api.auth.LoginAuthenticatorProvider;
import io.fand.api.player.PlayerProfile;
import io.fand.api.service.ServicePriority;
import io.fand.server.command.AdventureBridge;
import io.fand.server.player.PlayerProfiles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.key.Key;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandLoginAuthenticationService implements LoginAuthenticationService, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandLoginAuthenticationService.class);

    private final Object lock = new Object();
    private final LinkedHashMap<Key, Registration> registrations = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private volatile @Nullable BuiltinAuthenticator builtin;

    @Override
    public LoginAuthenticationRegistration register(Key key, LoginAuthenticator authenticator) {
        return register(key, authenticator, ServicePriority.NORMAL);
    }

    @Override
    public LoginAuthenticationRegistration register(
            Key key,
            LoginAuthenticator authenticator,
            ServicePriority priority
    ) {
        return register(key, authenticator, priority, "server");
    }

    public LoginAuthenticationRegistration register(
            Key key,
            LoginAuthenticator authenticator,
            ServicePriority priority,
            String owner
    ) {
        var registration = new Registration(
                this,
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(authenticator, "authenticator"),
                Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(priority, "priority"),
                sequence.incrementAndGet());
        Registration previous;
        synchronized (lock) {
            previous = registrations.put(key, registration);
        }
        if (previous != null) {
            previous.unregisterFromRegistry();
        }
        return registration;
    }

    @Override
    public List<LoginAuthenticatorProvider> authenticators() {
        synchronized (lock) {
            return orderedProviders();
        }
    }

    public void builtin(@Nullable BuiltinAuthenticator builtin) {
        this.builtin = builtin;
    }

    public LoginAttempt authenticate(LoginAuthenticationRequest request) {
        Objects.requireNonNull(request, "request");
        var pluginAttempt = authenticatePlugins(request);
        if (pluginAttempt.status() != LoginAttempt.Status.PASS) {
            return pluginAttempt;
        }

        var fallback = builtin;
        if (fallback == null) {
            return LoginAttempt.pass();
        }
        return fallback.authenticate(request);
    }

    public LoginAttempt authenticatePlugins(LoginAuthenticationRequest request) {
        Objects.requireNonNull(request, "request");
        for (var provider : authenticators()) {
            LoginAuthenticationResult result;
            try {
                result = Objects.requireNonNull(
                        provider.authenticator().authenticate(request),
                        "Login authenticator returned null");
            } catch (Exception failure) {
                LOGGER.warn("Login authenticator {} from {} failed", provider.key(), provider.owner(), failure);
                return LoginAttempt.error();
            }
            var attempt = toAttempt(result);
            if (attempt.status() != LoginAttempt.Status.PASS) {
                return attempt;
            }
        }

        return LoginAttempt.pass();
    }

    private LoginAttempt toAttempt(LoginAuthenticationResult result) {
        return switch (result.action()) {
            case PASS -> LoginAttempt.pass();
            case ALLOW -> LoginAttempt.allow(PlayerProfiles.toGameProfile(
                    Objects.requireNonNull(result.profileOrNull(), "Allowed login result is missing a profile")));
            case DENY -> LoginAttempt.deny(AdventureBridge.toVanillaOrFallback(
                    Objects.requireNonNull(result.reasonOrNull(), "Denied login result is missing a reason"),
                    Component.literal("Disconnected"),
                    null));
        };
    }

    private List<LoginAuthenticatorProvider> orderedProviders() {
        return registrations.values().stream()
                .filter(Registration::active)
                .sorted(FandLoginAuthenticationService::compare)
                .map(Registration::provider)
                .toList();
    }

    private static int compare(Registration left, Registration right) {
        int priority = Integer.compare(right.priority().ordinal(), left.priority().ordinal());
        if (priority != 0) {
            return priority;
        }
        return Long.compare(right.sequence(), left.sequence());
    }

    private void release(Registration registration) {
        synchronized (lock) {
            registrations.remove(registration.key(), registration);
        }
    }

    @Override
    public void close() {
        List<Registration> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(registrations.values());
            registrations.clear();
        }
        snapshot.forEach(Registration::unregisterFromRegistry);
        builtin = null;
    }

    @FunctionalInterface
    public interface BuiltinAuthenticator {
        LoginAttempt authenticate(LoginAuthenticationRequest request);
    }

    public record LoginAttempt(Status status, @Nullable GameProfile profile, @Nullable Component reason) {
        public static LoginAttempt pass() {
            return new LoginAttempt(Status.PASS, null, null);
        }

        public static LoginAttempt allow(GameProfile profile) {
            return new LoginAttempt(Status.ALLOW, Objects.requireNonNull(profile, "profile"), null);
        }

        public static LoginAttempt deny(Component reason) {
            return new LoginAttempt(Status.DENY, null, Objects.requireNonNull(reason, "reason"));
        }

        public static LoginAttempt error() {
            return deny(Component.translatable("multiplayer.disconnect.authservers_down"));
        }

        public enum Status {
            PASS,
            ALLOW,
            DENY
        }
    }

    private static final class Registration implements LoginAuthenticationRegistration {

        private final FandLoginAuthenticationService owner;
        private final Key key;
        private final LoginAuthenticator authenticator;
        private final String providerOwner;
        private final ServicePriority priority;
        private final long sequence;
        private volatile boolean active = true;

        private Registration(
                FandLoginAuthenticationService owner,
                Key key,
                LoginAuthenticator authenticator,
                String providerOwner,
                ServicePriority priority,
                long sequence
        ) {
            this.owner = owner;
            this.key = key;
            this.authenticator = authenticator;
            this.providerOwner = providerOwner;
            this.priority = priority;
            this.sequence = sequence;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public LoginAuthenticator authenticator() {
            return authenticator;
        }

        @Override
        public String owner() {
            return providerOwner;
        }

        @Override
        public ServicePriority priority() {
            return priority;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.release(this);
            }
        }

        private long sequence() {
            return sequence;
        }

        private LoginAuthenticatorProvider provider() {
            return new LoginAuthenticatorProvider(key, authenticator, providerOwner, priority);
        }

        private void unregisterFromRegistry() {
            active = false;
        }
    }
}
