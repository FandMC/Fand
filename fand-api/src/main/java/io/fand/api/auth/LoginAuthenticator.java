package io.fand.api.auth;

/**
 * Authenticates a player during the network login path.
 *
 * <p>Implementations are called from Mojang's login authenticator thread, not
 * from the main server thread. They may perform blocking account service I/O,
 * but must not touch live world state directly.
 */
@FunctionalInterface
public interface LoginAuthenticator {

    LoginAuthenticationResult authenticate(LoginAuthenticationRequest request) throws Exception;
}
