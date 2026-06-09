package io.fand.server.network.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static java.util.Map.entry;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.view.ClientboundAddEntityPacketView;
import io.fand.api.packet.view.ClientboundBlockChangedAckPacketView;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DynamicPacketViewTest {

    @Test
    void generatedViewsReadPrimitiveFieldsThroughBoxedValues() {
        var view = new DynamicPacketView(
                PacketType.PLAY_CLIENTBOUND_ADD_ENTITY,
                Map.ofEntries(
                        entry("id", 42),
                        entry("uuid", java.util.UUID.randomUUID()),
                        entry("type", "minecraft:zombie"),
                        entry("x", 1.0D),
                        entry("y", 2.0D),
                        entry("z", 3.0D),
                        entry("movement", "still"),
                        entry("xRot", 4.5F),
                        entry("yRot", 5.5F),
                        entry("yHeadRot", 6.5F),
                        entry("data", 7)))
                .as(ClientboundAddEntityPacketView.class);

        assertThat(view.id()).isEqualTo(42);
        assertThat(view.xRot()).isEqualTo(4.5F);
    }

    @Test
    void generatedWithMethodsReturnTypedViews() {
        var view = new DynamicPacketView(
                PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK,
                Map.of("sequence", 1))
                .as(ClientboundBlockChangedAckPacketView.class);

        var replacement = view.withSequence(2);

        assertThat(replacement).isInstanceOf(ClientboundBlockChangedAckPacketView.class);
        assertThat(replacement.sequence()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownFields() {
        var view = new DynamicPacketView(PacketType.PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK, Map.of("sequence", 1));

        assertThatThrownBy(() -> view.value("missing"))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("missing");
    }
}
