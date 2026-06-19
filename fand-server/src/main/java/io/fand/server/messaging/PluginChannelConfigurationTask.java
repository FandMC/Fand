package io.fand.server.messaging;

import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.key.Key;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.server.network.ConfigurationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginChannelConfigurationTask implements ConfigurationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginChannelConfigurationTask.class);
    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("fand_plugin_channels");
    private static final int MODDED_CLIENT_PROBE_ID = 0xFAD71C;

    private final Collection<Key> serverboundChannels;
    private final Collection<Key> configurationServerboundChannels;
    private Step step = Step.PROBE;
    private boolean complete;
    private int negotiatedVersion = -1;
    private boolean supportsCommonRegister;
    private int ticks;

    public PluginChannelConfigurationTask(Collection<Key> serverboundChannels) {
        this(serverboundChannels, List.of());
    }

    public PluginChannelConfigurationTask(
            Collection<Key> serverboundChannels,
            Collection<Key> configurationServerboundChannels
    ) {
        this.serverboundChannels = PluginChannelAdvertiser.sorted(serverboundChannels);
        this.configurationServerboundChannels = PluginChannelAdvertiser.sorted(configurationServerboundChannels);
    }

    @Override
    public void start(Consumer<Packet<?>> connection) {
        if (serverboundChannels.isEmpty() && configurationServerboundChannels.isEmpty()) {
            complete = true;
            return;
        }
        connection.accept(PluginChannelAdvertiser.commonConfigurationRegisterPacket(configurationServerboundChannels));
        connection.accept(new ClientboundPingPacket(MODDED_CLIENT_PROBE_ID));
    }

    @Override
    public boolean tick() {
        if (!complete && ++ticks > 200) {
            LOGGER.debug("Timed out while negotiating Fand plugin channel common packets");
            complete = true;
        }
        return complete;
    }

    @Override
    public boolean handleCustomPayload(ServerboundCustomPayloadPacket packet, Consumer<Packet<?>> connection) {
        if (!(packet.payload() instanceof DiscardedPayload payload)) {
            return false;
        }
        if (PluginChannelAdvertiser.isLegacyRegister(payload.id())) {
            if (step == Step.PROBE) {
                List<String> channels = legacyChannels(payload.payload());
                if (channels.contains("c:version")) {
                    supportsCommonRegister = channels.contains("c:register");
                    sendVersion(connection);
                } else {
                    complete = true;
                }
            }
            return true;
        }
        if (PluginChannelAdvertiser.isCommonVersion(payload.id())) {
            if (step == Step.VERSION) {
                negotiatedVersion = negotiateVersion(payload);
                if (negotiatedVersion > 0 && supportsCommonRegister) {
                    connection.accept(PluginChannelAdvertiser.commonRegisterPacket(serverboundChannels, negotiatedVersion));
                    step = Step.REGISTER;
                } else {
                    complete = true;
                }
            }
            return true;
        }
        if (PluginChannelAdvertiser.isCommonRegister(payload.id())) {
            if (step == Step.REGISTER) {
                complete = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean handlePong(ServerboundPongPacket packet, Consumer<Packet<?>> connection) {
        if (packet.getId() != MODDED_CLIENT_PROBE_ID || step != Step.PROBE) {
            return false;
        }
        complete = true;
        return true;
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }

    private void sendVersion(Consumer<Packet<?>> connection) {
        connection.accept(PluginChannelAdvertiser.commonVersionPacket());
        step = Step.VERSION;
    }

    private static List<String> legacyChannels(byte[] payload) {
        var channels = new ArrayList<String>();
        for (String raw : new String(payload, StandardCharsets.US_ASCII).split("\0")) {
            if (!raw.isBlank()) {
                channels.add(raw);
            }
        }
        return channels;
    }

    private static int negotiateVersion(DiscardedPayload payload) {
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.payload()));
        int[] versions = buffer.readVarIntArray();
        for (int version : versions) {
            if (version == PluginChannelAdvertiser.COMMON_PACKET_VERSION) {
                return version;
            }
        }
        LOGGER.debug("Client does not support Fand plugin channel common packets");
        return -1;
    }

    private enum Step {
        PROBE,
        VERSION,
        REGISTER
    }
}
