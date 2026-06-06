package io.fand.server.network;

import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

public record VelocityForwardingQueryAnswerPayload(byte[] forwardedData) implements CustomQueryAnswerPayload {

    public VelocityForwardingQueryAnswerPayload {
        forwardedData = Arrays.copyOf(forwardedData, forwardedData.length);
    }

    public static VelocityForwardingQueryAnswerPayload read(FriendlyByteBuf input) {
        var data = new byte[input.readableBytes()];
        input.readBytes(data);
        return new VelocityForwardingQueryAnswerPayload(data);
    }

    @Override
    public byte[] forwardedData() {
        return Arrays.copyOf(forwardedData, forwardedData.length);
    }

    @Override
    public void write(FriendlyByteBuf output) {
        output.writeBytes(forwardedData);
    }
}
