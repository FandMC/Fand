package io.fand.api.event.player;

/** Broad reason a player connection ended. */
public enum DisconnectReason {
    QUIT,
    KICK,
    TIMEOUT,
    SERVER_SHUTDOWN,
    DUPLICATE_LOGIN,
    FLYING,
    IDLING,
    INVALID_PACKET,
    INVALID_MOVEMENT,
    CHAT_VALIDATION_FAILED,
    SPAM,
    END_OF_STREAM,
    TRANSFER,
    UNKNOWN
}
