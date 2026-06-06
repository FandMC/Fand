package io.fand.server.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.resources.Identifier;

public enum VelocityForwardingQueryPayload implements CustomQueryPayload {
    INSTANCE;

    public static final int TRANSACTION_ID = 0;
    public static final Identifier ID = Identifier.fromNamespaceAndPath("velocity", "player_info");
    private static final int REQUESTED_VERSION = 1;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf output) {
        output.writeByte(REQUESTED_VERSION);
    }
}
