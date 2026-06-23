package io.fand.server.tablist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.ReusablePacketEncoding;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

final class FandTabListPacketsTest {

    @Test
    void buildsPacketFromCustomEntries() {
        var id = UUID.randomUUID();
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                id,
                null,
                true,
                87,
                GameType.CREATIVE,
                null,
                true,
                12,
                null);

        var packet = FandTabListPackets.packet(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                List.of(entry));

        assertThat(packet.actions()).containsExactly(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY);
        assertThat(packet.entries()).containsExactly(entry);
        assertThat(packet).isInstanceOf(ReusablePacketEncoding.class);
        assertThat(packet.entries().getFirst().profileId()).isEqualTo(id);
        assertThat(packet.entries().getFirst().latency()).isEqualTo(87);
        assertThat(packet.entries().getFirst().listOrder()).isEqualTo(12);
    }

    @Test
    void rewritesLatencyAndFiltersHiddenEntries() {
        var visibleId = UUID.randomUUID();
        var hiddenId = UUID.randomUUID();
        var visible = entry(visibleId, 50);
        var hidden = entry(hiddenId, 60);
        var packet = FandTabListPackets.packet(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                List.of(visible, hidden));

        var rewritten = FandTabListPackets.rewrite(packet, Set.of(hiddenId), Map.of(visibleId, 135));

        assertThat(rewritten).isNotSameAs(packet);
        assertThat(rewritten.actions()).containsExactly(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY);
        assertThat(rewritten.entries()).hasSize(1);
        assertThat(rewritten.entries().getFirst().profileId()).isEqualTo(visibleId);
        assertThat(rewritten.entries().getFirst().latency()).isEqualTo(135);
    }

    @Test
    void keepsPacketWhenNoEntryChanges() {
        var packet = FandTabListPackets.packet(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE),
                List.of(entry(UUID.randomUUID(), 50)));

        assertThat(FandTabListPackets.rewrite(packet, Set.of(), Map.of())).isSameAs(packet);
    }

    private static ClientboundPlayerInfoUpdatePacket.Entry entry(UUID id, int latency) {
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                id,
                null,
                true,
                latency,
                GameType.CREATIVE,
                null,
                true,
                12,
                null);
    }
}
