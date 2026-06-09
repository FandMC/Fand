package io.fand.api.entity;

import java.util.Set;
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
public record ClientSettings(
        String locale,
        int viewDistance,
        ClientChatVisibility chatVisibility,
        boolean chatColors,
        Set<ClientSkinPart> skinParts,
        ClientMainHand mainHand,
        boolean textFilteringEnabled,
        boolean serverListingAllowed,
        ClientParticleStatus particleStatus) {

    public ClientSettings(String locale, int viewDistance) {
        this(
                locale,
                viewDistance,
                ClientChatVisibility.FULL,
                true,
                Set.of(),
                ClientMainHand.RIGHT,
                false,
                false,
                ClientParticleStatus.ALL);
    }

    public ClientSettings {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(chatVisibility, "chatVisibility");
        skinParts = Set.copyOf(Objects.requireNonNull(skinParts, "skinParts"));
        Objects.requireNonNull(mainHand, "mainHand");
        Objects.requireNonNull(particleStatus, "particleStatus");
    }
}
