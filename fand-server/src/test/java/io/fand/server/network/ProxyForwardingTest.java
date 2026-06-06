package io.fand.server.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.net.InetAddresses;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class ProxyForwardingTest {

    private static final UUID PLAYER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void parsesBungeeLegacyForwardingWithProperties() {
        var host = "play.example.net\u0000198.51.100.12\u0000"
                + "11111111222233334444555555555555\u0000"
                + "[{\"name\":\"textures\",\"value\":\"skin\",\"signature\":\"sig\"}]";

        var info = ProxyForwarding.parseBungeeLegacy(host, "BungeeUser");

        assertThat(info.profile().id()).isEqualTo(PLAYER_ID);
        assertThat(info.profile().name()).isEqualTo("BungeeUser");
        assertThat(info.profile().properties().get("textures"))
                .containsExactly(new Property("textures", "skin", "sig"));
        assertThat(InetAddresses.toAddrString(info.address().getAddress())).isEqualTo("198.51.100.12");
    }

    @Test
    void parsesBungeeLegacyForwardingWithoutProperties() {
        var host = "play.example.net\u0000203.0.113.9\u000011111111222233334444555555555555";

        var info = ProxyForwarding.parseBungeeLegacy(host, "BungeeUser");

        assertThat(info.profile().id()).isEqualTo(PLAYER_ID);
        assertThat(info.profile().properties().values()).isEmpty();
        assertThat(InetAddresses.toAddrString(info.address().getAddress())).isEqualTo("203.0.113.9");
    }

    @Test
    void parsesVelocityModernForwarding() {
        var properties = new PropertyMap(ImmutableMultimap.of(
                "textures", new Property("textures", "skin", "sig")));
        var signedPayload = ProxyForwarding.signVelocityPayload(
                "shared-secret",
                velocityForwardingPayload("2001:db8::5", PLAYER_ID, "VelocityUser", properties));

        var info = ProxyForwarding.parseVelocityModern(
                "shared-secret",
                new VelocityForwardingQueryAnswerPayload(signedPayload));

        assertThat(info.profile().id()).isEqualTo(PLAYER_ID);
        assertThat(info.profile().name()).isEqualTo("VelocityUser");
        assertThat(info.profile().properties().get("textures"))
                .containsExactly(new Property("textures", "skin", "sig"));
        assertThat(InetAddresses.toAddrString(info.address().getAddress())).isEqualTo("2001:db8::5");
    }

    @Test
    void rejectsVelocityModernForwardingWithWrongSecret() {
        var signedPayload = ProxyForwarding.signVelocityPayload(
                "shared-secret",
                velocityForwardingPayload(
                        "198.51.100.24",
                        PLAYER_ID,
                        "VelocityUser",
                        new PropertyMap(ImmutableMultimap.of())));

        assertThatThrownBy(() -> ProxyForwarding.parseVelocityModern(
                "wrong-secret",
                new VelocityForwardingQueryAnswerPayload(signedPayload)))
                .isInstanceOf(ForwardingParseException.class)
                .hasMessageContaining("signature");
    }

    private static byte[] velocityForwardingPayload(
            String address,
            UUID playerId,
            String playerName,
            PropertyMap properties
    ) {
        var output = new FriendlyByteBuf(Unpooled.buffer());
        output.writeVarInt(1);
        output.writeUtf(address);
        output.writeUUID(playerId);
        output.writeUtf(playerName);
        ProxyForwarding.writeProperties(output, properties);
        var bytes = new byte[output.readableBytes()];
        output.readBytes(bytes);
        return bytes;
    }
}
