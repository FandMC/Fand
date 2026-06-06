package io.fand.server.network;

import com.mojang.authlib.GameProfile;
import java.net.InetSocketAddress;

public record ForwardedPlayerInfo(GameProfile profile, InetSocketAddress address) {
}
