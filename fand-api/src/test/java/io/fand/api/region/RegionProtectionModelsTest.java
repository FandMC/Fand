package io.fand.api.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.world.BlockRegion;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class RegionProtectionModelsTest {

    @Test
    void protectionNormalizesMembershipSubjects() {
        var protection = RegionProtection.builder()
                .priority(20)
                .parent(Key.key("fand:spawn"))
                .owner(" User:ABC ")
                .member("Group:Builders")
                .build();

        assertThat(protection.priority()).isEqualTo(20);
        assertThat(protection.parent()).contains(Key.key("fand:spawn"));
        assertThat(protection.owner("user:abc")).isTrue();
        assertThat(protection.member("user:abc")).isTrue();
        assertThat(protection.member("group:builders")).isTrue();
        assertThat(protection.emptyMetadata()).isFalse();
    }

    @Test
    void regionDefinitionBuilderCarriesProtectionMetadata() {
        var region = RegionDefinition.builder(
                        Key.key("fand:spawn"),
                        Key.key("minecraft:overworld"),
                        new BlockRegion(0, 0, 0, 10, 10, 10))
                .priority(5)
                .owner("user:alice")
                .member("group:builders")
                .build();

        assertThat(region.protection().priority()).isEqualTo(5);
        assertThat(region.protection().owner("user:alice")).isTrue();
        assertThat(region.protection().member("group:builders")).isTrue();
        assertThat(region.toBuilder().priority(8).build().protection().priority()).isEqualTo(8);
    }

    @Test
    void protectionRejectsBlankSubjects() {
        assertThatThrownBy(() -> RegionProtection.builder().owner(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject cannot be blank");
    }
}
