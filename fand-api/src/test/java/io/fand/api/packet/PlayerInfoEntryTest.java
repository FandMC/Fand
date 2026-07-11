package io.fand.api.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.fand.api.entity.GameMode;
import io.fand.api.player.PlayerProfile;
import io.fand.api.tablist.TabListEntry;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerInfoEntryTest {

    @Test
    void convertsTabListEntryAndSupportsFocusedReplacement() {
        var id = UUID.randomUUID();
        var source = TabListEntry.builder(id, "Player")
                .latency(42)
                .gameMode(GameMode.CREATIVE)
                .order(7)
                .build();

        var entry = PlayerInfoEntry.from(source).withLatency(99);

        assertThat(entry.profileId()).isEqualTo(id);
        assertThat(entry.profile()).isEqualTo(source.profile());
        assertThat(entry.latency()).isEqualTo(99);
        assertThat(entry.gameMode()).isEqualTo(GameMode.CREATIVE);
        assertThat(entry.order()).isEqualTo(7);
    }

    @Test
    void rejectsMismatchedProfileIdentity() {
        var id = UUID.randomUUID();

        assertThatIllegalArgumentException().isThrownBy(() -> new PlayerInfoEntry(
                id,
                new PlayerProfile(UUID.randomUUID(), "Other"),
                true,
                0,
                GameMode.SURVIVAL,
                null,
                true,
                0));
    }
}
