package io.fand.server.auth;

import com.mojang.authlib.Environment;
import io.fand.server.config.FandConfig;
import java.util.Objects;

public final class FandAuthEnvironment {

    private FandAuthEnvironment() {
    }

    public static Environment fromConfig(FandConfig.Authentication config) {
        Objects.requireNonNull(config, "config");
        return new Environment(
                config.sessionHost.trim(),
                config.servicesHost.trim(),
                config.profilesHost.trim(),
                config.environmentName.trim()
        );
    }
}
