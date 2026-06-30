package io.fand.server.nms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.nms.NmsHookResult;
import io.fand.api.service.ServicePriority;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandNmsServiceTest {

    @Test
    void exposesServerHandleFromSupplier() {
        var server = new Object();
        var nms = new FandNmsService(() -> server);

        assertThat(nms.access().server()).isSameAs(server);
    }

    @Test
    void readsWritesAndCallsPrivateMembers() {
        var nms = new FandNmsService(() -> new Object()).access();
        var target = new DemoHandle("old");

        assertThat(nms.get(target, "value", String.class)).isEqualTo("old");

        nms.set(target, "value", "new");

        assertThat(nms.get(target, "value", String.class)).isEqualTo("new");
        assertThat(nms.call(target, "join", String.class, "pre", 3)).isEqualTo("pre:new:3");
    }

    @Test
    void extractsHandleMethodFromApiWrapper() {
        var nms = new FandNmsService(() -> new Object()).access();
        var handle = new DemoHandle("value");

        assertThat(nms.handle(new DemoApi(handle))).isSameAs(handle);
    }

    @Test
    void missingHandleThrowsClearException() {
        var nms = new FandNmsService(() -> new Object()).access();

        assertThatThrownBy(() -> nms.handle("plain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not expose an NMS handle");
    }

    @Test
    void dispatchUsesPriorityAndStopsOnReplacement() {
        var service = new FandNmsService(() -> new Object());
        var hook = Key.key("demo:hook");
        var calls = new AtomicInteger();
        service.registerHook(hook, context -> {
            calls.incrementAndGet();
            return NmsHookResult.replace(context.argument(0, String.class).toUpperCase());
        }, ServicePriority.HIGH, "high");
        service.registerHook(hook, context -> {
            calls.incrementAndGet();
            return NmsHookResult.replace("low");
        }, ServicePriority.LOW, "low");

        var result = service.dispatch(hook, this, "value");

        assertThat(result.action()).isEqualTo(NmsHookResult.Action.REPLACE);
        assertThat(result.replacementOrNull()).isEqualTo("VALUE");
        assertThat(calls).hasValue(1);
    }

    @Test
    void unregisterRemovesHook() {
        var service = new FandNmsService(() -> new Object());
        var hook = Key.key("demo:hook");
        var registration = service.registerHook(hook, ignored -> NmsHookResult.cancel(), ServicePriority.NORMAL, "demo");

        registration.unregister();

        assertThat(service.dispatch(hook, this).action()).isEqualTo(NmsHookResult.Action.PASS);
    }

    private static final class DemoApi {

        private final DemoHandle handle;

        private DemoApi(DemoHandle handle) {
            this.handle = handle;
        }

        private DemoHandle handle() {
            return handle;
        }
    }

    private static final class DemoHandle {

        private String value;

        private DemoHandle(String value) {
            this.value = value;
        }

        private String join(String prefix, int count) {
            return prefix + ":" + value + ":" + count;
        }
    }
}
