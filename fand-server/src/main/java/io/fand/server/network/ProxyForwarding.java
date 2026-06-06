package io.fand.server.network;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.net.InetAddresses;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.network.FriendlyByteBuf;

public final class ProxyForwarding {

    private static final int HMAC_LENGTH = 32;

    private ProxyForwarding() {
    }

    public static ForwardedPlayerInfo parseBungeeLegacy(String hostName, String playerName) {
        var parts = hostName.split("\u0000", 4);
        if (parts.length < 3) {
            throw new ForwardingParseException("BungeeCord forwarding data is missing from the handshake host");
        }
        var properties = parts.length >= 4 ? parseLegacyProperties(parts[3]) : new PropertyMap(ImmutableMultimap.of());
        var profile = new GameProfile(parseUuid(parts[2]), playerName, properties);
        return new ForwardedPlayerInfo(profile, parseAddress(parts[1]));
    }

    public static ForwardedPlayerInfo parseVelocityModern(String secret, VelocityForwardingQueryAnswerPayload payload) {
        var signedPayload = payload.forwardedData();
        if (signedPayload.length <= HMAC_LENGTH) {
            throw new ForwardingParseException("Velocity forwarding payload is too short");
        }

        var signature = new byte[HMAC_LENGTH];
        var forwardedData = new byte[signedPayload.length - HMAC_LENGTH];
        System.arraycopy(signedPayload, 0, signature, 0, HMAC_LENGTH);
        System.arraycopy(signedPayload, HMAC_LENGTH, forwardedData, 0, forwardedData.length);

        if (!MessageDigest.isEqual(signature, hmac(secret, forwardedData))) {
            throw new ForwardingParseException("Velocity forwarding signature is invalid");
        }

        var input = new FriendlyByteBuf(Unpooled.wrappedBuffer(forwardedData));
        try {
            int version = input.readVarInt();
            if (version < 1) {
                throw new ForwardingParseException("Unsupported Velocity forwarding version: " + version);
            }
            var address = parseAddress(input.readUtf());
            var profile = new GameProfile(input.readUUID(), input.readUtf(16), readProperties(input));
            return new ForwardedPlayerInfo(profile, address);
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            throw new ForwardingParseException("Velocity forwarding payload is malformed", ex);
        }
    }

    static PropertyMap readProperties(FriendlyByteBuf input) {
        int count = input.readVarInt();
        if (count < 0 || count > 16) {
            throw new ForwardingParseException("Forwarded profile property count is out of range");
        }
        var properties = ImmutableMultimap.<String, Property>builder();
        for (int i = 0; i < count; i++) {
            var property = readProperty(input);
            properties.put(property.name(), property);
        }
        return new PropertyMap(properties.build());
    }

    static void writeProperties(FriendlyByteBuf output, PropertyMap properties) {
        output.writeVarInt(properties.size());
        for (var property : properties.values()) {
            output.writeUtf(property.name());
            output.writeUtf(property.value());
            if (property.hasSignature()) {
                output.writeBoolean(true);
                output.writeUtf(property.signature());
            } else {
                output.writeBoolean(false);
            }
        }
    }

    static byte[] signVelocityPayload(String secret, byte[] forwardedData) {
        var signature = hmac(secret, forwardedData);
        var signed = new byte[signature.length + forwardedData.length];
        System.arraycopy(signature, 0, signed, 0, signature.length);
        System.arraycopy(forwardedData, 0, signed, signature.length, forwardedData.length);
        return signed;
    }

    private static Property readProperty(FriendlyByteBuf input) {
        var name = input.readUtf();
        var value = input.readUtf();
        var signature = input.readBoolean() ? input.readUtf() : null;
        return new Property(name, value, signature);
    }

    private static PropertyMap parseLegacyProperties(String json) {
        try {
            var properties = ImmutableMultimap.<String, Property>builder();
            var root = JsonParser.parseString(json);
            if (!root.isJsonArray()) {
                throw new ForwardingParseException("BungeeCord forwarded properties must be a JSON array");
            }
            for (var element : root.getAsJsonArray()) {
                var object = element.getAsJsonObject();
                var name = object.get("name").getAsString();
                var value = object.get("value").getAsString();
                var signature = object.has("signature") && !object.get("signature").isJsonNull()
                        ? object.get("signature").getAsString()
                        : null;
                var property = new Property(name, value, signature);
                properties.put(property.name(), property);
            }
            return new PropertyMap(properties.build());
        } catch (JsonSyntaxException | IllegalStateException | NullPointerException ex) {
            throw new ForwardingParseException("BungeeCord forwarded properties are malformed", ex);
        }
    }

    private static InetSocketAddress parseAddress(String address) {
        try {
            return new InetSocketAddress(InetAddresses.forString(address), 0);
        } catch (IllegalArgumentException ex) {
            throw new ForwardingParseException("Forwarded player address is not a valid IP address", ex);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            if (value.length() == 32) {
                return UUID.fromString(value.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"));
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ForwardingParseException("Forwarded player UUID is malformed", ex);
        }
    }

    private static byte[] hmac(String secret, byte[] data) {
        try {
            var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(data);
        } catch (InvalidKeyException ex) {
            throw new ForwardingParseException("Velocity forwarding secret is invalid", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("HmacSHA256 is not available", ex);
        }
    }
}
