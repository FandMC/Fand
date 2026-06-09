package io.fand.api.packet;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.packet.view.ClientboundAddEntityPacketView;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PacketMetadataTest {

    @Test
    void generatedPacketTypesBindGeneratedViews() {
        assertThat(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY.protocol()).isEqualTo(PacketProtocol.PLAY);
        assertThat(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY.direction()).isEqualTo(PacketDirection.CLIENTBOUND);
        assertThat(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY.key()).isEqualTo(Key.key("minecraft:add_entity"));
        assertThat(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY.viewType()).isEqualTo(ClientboundAddEntityPacketView.class);
        assertThat(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY.vanillaClassName())
                .isEqualTo("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
    }

    @Test
    void generatedPacketTypesCanBeLookedUpByProtocolDirectionAndKey() {
        assertThat(PacketType.find(PacketProtocol.PLAY, PacketDirection.SERVERBOUND, Key.key("minecraft:use_item_on")))
                .contains(PacketType.PLAY_SERVERBOUND_USE_ITEM_ON);
    }

    @Test
    void generatedViewsUsePrimitiveReturnTypes() throws NoSuchMethodException {
        assertThat(ClientboundAddEntityPacketView.class.getMethod("id").getReturnType()).isEqualTo(int.class);
        assertThat(ClientboundAddEntityPacketView.class.getMethod("xRot").getReturnType()).isEqualTo(float.class);
    }
}
