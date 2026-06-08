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
            seen.set((Vec3d) view.movement());
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

    @Test
    void typedWithMethodsWorkForHighFrequencyPackets() {
        var registry = newRegistry();

        // Test ServerboundChatView.withMessage()
        registry.intercept(PacketType.C2S_CHAT, (view, controller) -> {
            ServerboundChatView chatView = (ServerboundChatView) view;
            controller.replace(chatView.withMessage(chatView.message().toUpperCase()));
        });

        Packet<?> rewrittenChat = registry.interceptInbound(chat("hello"), null);
        assertThat(rewrittenChat).isInstanceOf(ServerboundChatPacket.class);
        assertThat(((ServerboundChatPacket) rewrittenChat).message()).isEqualTo("HELLO");

        // Test ClientboundSetEntityMotionView.withMovement()
        registry.intercept(PacketType.S2C_SET_ENTITY_MOTION, (view, controller) -> {
            ClientboundSetEntityMotionView motionView = (ClientboundSetEntityMotionView) view;
            controller.replace(motionView.withMovement(new Vec3d(1.0, 2.0, 3.0)));
        });

        var motionPacket = new ClientboundSetEntityMotionPacket(42, new Vec3(0.0, 0.0, 0.0));
        Packet<?> rewrittenMotion = registry.interceptOutbound(motionPacket, null);
        assertThat(rewrittenMotion).isInstanceOf(ClientboundSetEntityMotionPacket.class);
        Vec3 newMovement = ((ClientboundSetEntityMotionPacket) rewrittenMotion).movement();
        assertThat(newMovement.x).isEqualTo(1.0);
        assertThat(newMovement.y).isEqualTo(2.0);
        assertThat(newMovement.z).isEqualTo(3.0);
    }

    @Test
    void classPacketsCanBeReplacedViaSnapshot() {
        var registry = newRegistry();

        // Test ClientboundSetHealthPacket (Class type, now replaceable)
        registry.intercept(PacketType.S2C_SET_HEALTH, (view, controller) -> {
            float currentHealth = view.require("health", Float.class);
            controller.replace(view.with("health", currentHealth * 2.0f));
        });

        var healthPacket = new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(10.0f, 15, 5.0f);
        Packet<?> rewrittenHealth = registry.interceptOutbound(healthPacket, null);
        assertThat(rewrittenHealth).isInstanceOf(net.minecraft.network.protocol.game.ClientboundSetHealthPacket.class);
        assertThat(((net.minecraft.network.protocol.game.ClientboundSetHealthPacket) rewrittenHealth).getHealth())
                .isEqualTo(20.0f);
        assertThat(((net.minecraft.network.protocol.game.ClientboundSetHealthPacket) rewrittenHealth).getFood())
                .isEqualTo(15);

        // Test ClientboundContainerSetSlotPacket (Class type, now replaceable)
        registry.intercept(PacketType.S2C_CONTAINER_SET_SLOT, (view, controller) -> {
            controller.replace(view.with("slot", 5));
        });

        var slotPacket = new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                1, 2, 3, net.minecraft.world.item.ItemStack.EMPTY);
        Packet<?> rewrittenSlot = registry.interceptOutbound(slotPacket, null);
        assertThat(rewrittenSlot).isInstanceOf(net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket.class);
        assertThat(((net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket) rewrittenSlot).getSlot())
                .isEqualTo(5);
        assertThat(((net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket) rewrittenSlot).getContainerId())
                .isEqualTo(1);
    }

    @Test
    void genericClassPacketsCanBeReplaced() {
        var registry = newRegistry();

        // Test a Class packet without a dedicated snapshot (should use generic codec)
        registry.intercept(PacketType.S2C_KEEP_ALIVE, (view, controller) -> {
            long currentId = view.require("id", Long.class);
            controller.replace(view.with("id", currentId + 100L));
        });

        var keepAlivePacket = new net.minecraft.network.protocol.common.ClientboundKeepAlivePacket(123L);
        Packet<?> rewritten = registry.interceptOutbound(keepAlivePacket, null);
        assertThat(rewritten).isInstanceOf(net.minecraft.network.protocol.common.ClientboundKeepAlivePacket.class);
        assertThat(((net.minecraft.network.protocol.common.ClientboundKeepAlivePacket) rewritten).getId())
                .isEqualTo(223L);
    }

    record SamplePayload(long value) {
    }
}
