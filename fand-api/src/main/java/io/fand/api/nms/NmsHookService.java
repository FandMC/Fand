package io.fand.api.nms;

import io.fand.api.service.ServicePriority;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public interface NmsHookService {

    NmsHookRegistration register(Key hook, NmsHook handler);

    NmsHookRegistration register(Key hook, NmsHook handler, ServicePriority priority);

    List<NmsHookRegistration> hooks(Key hook);

    static NmsHookService empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements NmsHookService {
        INSTANCE;

        @Override
        public NmsHookRegistration register(Key hook, NmsHook handler) {
            Objects.requireNonNull(hook, "hook");
            Objects.requireNonNull(handler, "handler");
            throw new UnsupportedOperationException("NMS hook registration is not supported");
        }

        @Override
        public NmsHookRegistration register(Key hook, NmsHook handler, ServicePriority priority) {
            Objects.requireNonNull(hook, "hook");
            Objects.requireNonNull(handler, "handler");
            Objects.requireNonNull(priority, "priority");
            throw new UnsupportedOperationException("NMS hook registration is not supported");
        }

        @Override
        public List<NmsHookRegistration> hooks(Key hook) {
            Objects.requireNonNull(hook, "hook");
            return List.of();
        }
    }
}
