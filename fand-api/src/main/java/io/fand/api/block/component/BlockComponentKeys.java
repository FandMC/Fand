package io.fand.api.block.component;

import com.google.gson.JsonObject;
import io.fand.api.component.DataComponentKey;
import java.util.UUID;
import net.kyori.adventure.key.Key;

/**
 * Common Fand block component keys. Plugins may define their own keys with
 * {@link DataComponentKey#of}.
 */
public final class BlockComponentKeys {

    public static final DataComponentKey<Key> CUSTOM_ID =
            DataComponentKey.key(Key.key("fand:custom_block"));
    public static final DataComponentKey<UUID> OWNER =
            DataComponentKey.uuid(Key.key("fand:owner"));
    public static final DataComponentKey<Boolean> TICKING =
            DataComponentKey.bool(Key.key("fand:ticking"));
    public static final DataComponentKey<JsonObject> CUSTOM_DATA =
            DataComponentKey.object(Key.key("fand:custom_data"));

    private BlockComponentKeys() {
    }
}
