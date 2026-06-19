package io.fand.server.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

final class PluginChannelAdvertiserTest {

    @Test
    void legacyRegisterPayloadUsesNullSeparatedAsciiChannels() {
        var payload = PluginChannelAdvertiser.legacyPayload(List.of(
                Key.key("jei:give_item_stack"),
                Key.key("roughlyenoughitems:create_item")));

        assertThat(new String(payload, StandardCharsets.US_ASCII))
                .isEqualTo("jei:give_item_stack\0roughlyenoughitems:create_item");
    }

    @Test
    void legacyUnregisterPayloadUsesSameNullSeparatedFormat() {
        var payload = PluginChannelAdvertiser.legacyPayload(List.of(
                Key.key("example:old"),
                Key.key("example:gone")));

        assertThat(new String(payload, StandardCharsets.US_ASCII))
                .isEqualTo("example:old\0example:gone");
    }

    @Test
    void commonRegisterPayloadUsesPlayProtocolAndIdentifierList() {
        var payload = PluginChannelAdvertiser.commonRegisterPayload(List.of(
                Key.key("jei:give_item_stack"),
                Key.key("roughlyenoughitems:create_item")));
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));

        assertThat(buffer.readVarInt()).isEqualTo(1);
        assertThat(buffer.readUtf()).isEqualTo("play");
        assertThat(buffer.readVarInt()).isEqualTo(2);
        assertThat(buffer.readIdentifier().toString()).isEqualTo("jei:give_item_stack");
        assertThat(buffer.readIdentifier().toString()).isEqualTo("roughlyenoughitems:create_item");
        assertThat(buffer.readableBytes()).isZero();
    }
}
