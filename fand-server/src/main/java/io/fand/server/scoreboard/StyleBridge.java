package io.fand.server.scoreboard;

import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

final class StyleBridge {

    private StyleBridge() {
    }

    static net.minecraft.network.chat.Style toVanilla(Style style) {
        var element = GsonComponentSerializer.gson().serializer().toJsonTree(style);
        return net.minecraft.network.chat.Style.Serializer.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> {})
                .orElse(net.minecraft.network.chat.Style.EMPTY);
    }

    static Style fromVanilla(net.minecraft.network.chat.Style style) {
        var element = net.minecraft.network.chat.Style.Serializer.CODEC.encodeStart(JsonOps.INSTANCE, style)
                .resultOrPartial(error -> {})
                .orElseThrow(() -> new IllegalStateException("Failed to encode vanilla style"));
        return GsonComponentSerializer.gson().serializer().fromJson(element, Style.class);
    }
}
