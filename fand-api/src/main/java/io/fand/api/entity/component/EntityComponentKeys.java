package io.fand.api.entity.component;

import com.google.gson.JsonObject;
import io.fand.api.component.DataComponentKey;
import java.util.UUID;
import net.kyori.adventure.key.Key;

/**
 * Common Fand entity component keys. Plugins may define their own keys with
 * {@link DataComponentKey#of}.
 */
public final class EntityComponentKeys {

    public static final DataComponentKey<Key> CUSTOM_ID =
            DataComponentKey.key(Key.key("fand:custom_entity"));
    public static final DataComponentKey<UUID> OWNER =
            DataComponentKey.uuid(Key.key("fand:owner"));
    public static final DataComponentKey<JsonObject> CUSTOM_DATA =
            DataComponentKey.object(Key.key("fand:custom_data"));

    private EntityComponentKeys() {
    }
}
