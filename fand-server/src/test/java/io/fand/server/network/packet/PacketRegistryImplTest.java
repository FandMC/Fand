package io.fand.server.network.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.packet.CustomPacketDefinition;
import io.fand.api.packet.PacketDirection;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.Vec3d;
import io.fand.api.packet.view.ClientboundSetEntityMotionView;
import io.fand.api.packet.view.ServerboundChatView;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.permission.PermissionManager;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PacketRegistryImplTest {

    private PacketRegistryImpl newRegistry() {
        return new PacketRegistryImpl(new PlayerRegistry(new PermissionManager()));
    }

    private static ServerboundChatPacket chat(String message) {
        return new ServerboundChatPacket(message, Instant.EPOCH, 7L, null, null);
    }

    @Test
    void interceptorIsInvokedWithTypedView() {
        var registry = newRegistry();
        var seen = new AtomicReference<String>();
        registry.<ServerboundChatView>intercept(PacketType.C2S_CHAT, (view, controller) -> seen.set(view.message()));

        Packet<?> result = registry.interceptInbound(chat("hello"), null);

        assertThat(seen.get()).isEqualTo("hello");
        assertThat(result).isInstanceOf(ServerboundChatPacket.class);
    }

    @Test
    void cancelStopsDownstreamInterceptors() {
        var registry = newRegistry();
        var downstreamCalled = new AtomicBoolean(false);
        registry.<ServerboundChatView>intercept(PacketType.C2S_CHAT, (view, controller) -> controller.cancel());
        registry.<ServerboundChatView>intercept(PacketType.C2S_CHAT, (view, controller) -> downstreamCalled.set(true));

        Packet<?> result = registry.interceptInbound(chat("blocked"), null);

        assertThat(result).isNull();
        assertThat(downstreamCalled).isFalse();
    }

    @Test
    void replaceRewritesTheVanillaPacketViaTypedView() {
        var registry = newRegistry();
        registry.<ServerboundChatView>intercept(PacketType.C2S_CHAT,
                (view, controller) -> controller.replace(view.with("message", view.message().toUpperCase())));

        Packet<?> result = registry.interceptInbound(chat("hi"), null);

        assertThat(result).isInstanceOf(ServerboundChatPacket.class);
        assertThat(((ServerboundChatPacket) result).message()).isEqualTo("HI");
    }

    @Test
    void readsAndReplacesRealMarshalledFields() {
        var registry = newRegistry();
        var seen = new AtomicReference<Vec3d>();
        registry.<ClientboundSetEntityMotionView>intercept(PacketType.S2C_SET_ENTITY_MOTION, (view, controller) -> {
            seen.set(view.velocity());
            controller.replace(view.with("movement", new Vec3d(0.0, 2.0, 0.0)));
        });

        var original = new ClientboundSetEntityMotionPacket(42, new Vec3(1.0, 1.0, 1.0));
        var result = (ClientboundSetEntityMotionPacket) registry.interceptOutbound(original, null);

        assertThat(seen.get()).isEqualTo(new Vec3d(1.0, 1.0, 1.0));
        assertThat(result.id()).isEqualTo(42);
        assertThat(result.movement()).isEqualTo(new Vec3(0.0, 2.0, 0.0));
    }

    @Test
    void dynamicGetExposesFieldsWithoutATypedView() {
        var registry = newRegistry();
        var captured = new AtomicReference<String>();
        // C2S_CHAT_COMMAND has a typed view, but read it purely dynamically here.
        registry.intercept(PacketType.C2S_CHAT_COMMAND,
                (view, controller) -> captured.set(view.get("command", String.class).orElse("?")));

        registry.interceptInbound(new net.minecraft.network.protocol.game.ServerboundChatCommandPacket("help"), null);

        assertThat(captured.get()).isEqualTo("help");
    }

    @Test
    void closingRegistrationRemovesInterceptor() {
        var registry = newRegistry();
        var called = new AtomicBoolean(false);
        var registration = registry.<ServerboundChatView>intercept(
                PacketType.C2S_CHAT, (view, controller) -> called.set(true));
        registration.close();

        registry.interceptInbound(chat("hi"), null);

        assertThat(called).isFalse();
    }

    @Test
    void registeringCustomDefinitionDoesNotThrow() {
        var registry = newRegistry();
        var inbound = new CustomPacketDefinition<>("fand-test:in", PacketDirection.INBOUND, SamplePayload.class);
        var outbound = new CustomPacketDefinition<>("fand-test:out", PacketDirection.OUTBOUND, SamplePayload.class);

        registry.register(inbound, (sender, payload) -> { });
        registry.register(outbound, null);
    }

    @Test
    void reservedNamespacesAreRejected() {
        var registry = newRegistry();
        var bungee = new CustomPacketDefinition<>("bungeecord:main", PacketDirection.INBOUND, SamplePayload.class);
        var velocity = new CustomPacketDefinition<>("velocity:main", PacketDirection.OUTBOUND, SamplePayload.class);

        assertThatThrownBy(() -> registry.register(bungee, (sender, payload) -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
        assertThatThrownBy(() -> registry.register(velocity, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
    }

    record SamplePayload(long value) {
    }
}
