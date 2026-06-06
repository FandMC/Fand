package io.fand.server.item;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.fand.api.item.component.ItemComponents;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;

final class ItemComponentBridge {

    private static volatile RegistryAccess registries = RegistryAccess.EMPTY;

    private ItemComponentBridge() {
    }

    static void useRegistries(RegistryAccess access) {
        registries = Objects.requireNonNull(access, "access");
    }

    static DataComponentPatch toVanilla(ItemComponents components) {
        Objects.requireNonNull(components, "components");
        if (components.isEmpty()) {
            return DataComponentPatch.EMPTY;
        }
        return DataComponentPatch.CODEC.parse(ops(), components.toJsonPatch())
                .getOrThrow(error -> new IllegalArgumentException("Invalid item components: " + error));
    }

    static ItemComponents fromVanilla(DataComponentPatch patch) {
        Objects.requireNonNull(patch, "patch");
        if (patch.isEmpty()) {
            return ItemComponents.EMPTY;
        }
        JsonElement json = DataComponentPatch.CODEC.encodeStart(ops(), patch)
                .getOrThrow(error -> new IllegalArgumentException("Could not encode item components: " + error));
        return ItemComponents.fromJsonPatch(json);
    }

    private static com.mojang.serialization.DynamicOps<JsonElement> ops() {
        return registries.createSerializationContext(JsonOps.INSTANCE);
    }
}
