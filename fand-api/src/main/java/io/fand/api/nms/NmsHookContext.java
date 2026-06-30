package io.fand.api.nms;

import java.util.List;
import net.kyori.adventure.key.Key;

public interface NmsHookContext {

    Key hook();

    Object instance();

    List<Object> arguments();

    default Object argument(int index) {
        return arguments().get(index);
    }

    default <T> T argument(int index, Class<T> type) {
        return type.cast(argument(index));
    }
}
