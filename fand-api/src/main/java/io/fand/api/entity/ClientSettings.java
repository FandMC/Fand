package io.fand.api.entity;

import java.util.Objects;

/**
 * A snapshot of the client-side options a {@link Player} reported to the
 * server, as sent in their last settings packet.
 *
 * @param locale       the client's locale identifier, e.g. {@code "en_us"}.
 *                     Lower-cased; the client controls the exact value.
 * @param viewDistance the render distance, in chunks, the client requested.
 *                     The server may serve fewer chunks than this.
 */
public record ClientSettings(String locale, int viewDistance) {

    public ClientSettings {
        Objects.requireNonNull(locale, "locale");
    }
}
