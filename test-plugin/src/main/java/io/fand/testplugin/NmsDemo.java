package io.fand.testplugin;

import io.fand.api.nms.NmsAccess;
import io.fand.api.nms.NmsHookContext;
import io.fand.api.nms.NmsHookRegistration;
import io.fand.api.nms.NmsHookResult;
import io.fand.api.nms.NmsService;
import io.fand.api.plugin.PluginContext;
import io.fand.api.service.ServicePriority;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.key.Key;

final class NmsDemo {

    static final Key SERVER_STARTED_HOOK = Key.key("fand-test-plugin:server_started");
    static final Key MESSAGE_REWRITE_HOOK = Key.key("fand-test-plugin:message_rewrite");
    static final Key CANCEL_HOOK = Key.key("fand-test-plugin:cancel");
    static final Key TEMPORARY_HOOK = Key.key("fand-test-plugin:temporary");

    private final PluginContext context;
    private final List<NmsHookRegistration> registrations = new ArrayList<>();

    private volatile NmsDemoReport lastReport = NmsDemoReport.notRun();

    NmsDemo(PluginContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    void registerHooks() {
        var hooks = context.nms().hooks();
        registrations.add(hooks.register(SERVER_STARTED_HOOK, hook -> {
            context.logger().info(
                    "NMS hook {} observed instance={} args={}",
                    hook.hook().asString(),
                    typeName(hook.instance()),
                    hook.arguments());
            return NmsHookResult.pass();
        }, ServicePriority.NORMAL));
        registrations.add(hooks.register(MESSAGE_REWRITE_HOOK, hook ->
                NmsHookResult.replace(hook.argument(0, String.class).toUpperCase(Locale.ROOT)), ServicePriority.HIGH));
        registrations.add(hooks.register(CANCEL_HOOK, hook -> NmsHookResult.cancel(), ServicePriority.LOW));
        context.logger().info("Registered {} NMS demo hooks", registrations.size());
    }

    NmsDemoReport runStartupSelfTest(Object event) {
        var report = run("startup", event);
        lastReport = report;
        if (report.success()) {
            context.logger().info("NMS demo self-test passed: {}", report.summary());
        } else {
            context.logger().warn("NMS demo self-test failed: {}", report.summary());
            for (var failure : report.failures()) {
                context.logger().warn("NMS demo failure: {}", failure);
            }
        }
        return report;
    }

    NmsDemoReport runCommandSelfTest() {
        var report = run("command", null);
        lastReport = report;
        return report;
    }

    NmsDemoReport lastReport() {
        return lastReport;
    }

    List<NmsHookRegistration> registrations() {
        return List.copyOf(registrations);
    }

    private NmsDemoReport run(String mode, Object event) {
        var checks = new ArrayList<String>();
        var failures = new ArrayList<String>();
        var nms = context.nms();
        var access = nms.access();

        checkAccessServer(access, checks, failures);
        checkTypeLookup(access, checks, failures);
        checkConstructCallAndFields(access, checks, failures);
        checkHandleExtraction(access, event, checks, failures);
        checkLocalReflection(access, checks, failures);
        checkHookRegistration(nms, checks, failures);

        return new NmsDemoReport(mode, checks, failures);
    }

    private static void checkAccessServer(NmsAccess access, List<String> checks, List<String> failures) {
        try {
            var server = access.server();
            checks.add("server=" + server.getClass().getName());
        } catch (RuntimeException ex) {
            failures.add("server access failed: " + ex.getMessage());
        }
    }

    private static void checkTypeLookup(NmsAccess access, List<String> checks, List<String> failures) {
        try {
            var rawType = access.type("net.minecraft.server.MinecraftServer");
            var serverType = access.type("net.minecraft.server.MinecraftServer", Object.class);
            if (rawType != serverType) {
                failures.add("typed and raw NMS type lookup returned different classes");
                return;
            }
            checks.add("type=" + serverType.getName());
        } catch (RuntimeException ex) {
            failures.add("type lookup failed: " + ex.getMessage());
        }
    }

    private static void checkConstructCallAndFields(NmsAccess access, List<String> checks, List<String> failures) {
        try {
            var rawPos = access.construct("net.minecraft.core.BlockPos", 1, 2, 3);
            var pos = access.construct("net.minecraft.core.BlockPos", Object.class, 1, 2, 3);
            var posType = access.type("net.minecraft.core.BlockPos", Object.class);
            var x = access.get(pos, "x", Integer.class);
            var y = access.call(pos, "getY", Integer.class);
            var rawZ = access.call(rawPos, "getZ");
            var relative = access.call(pos, "relative", Object.class, access.get(access.type("net.minecraft.core.Direction"), "UP"));
            var relativeY = access.call(relative, "getY", Integer.class);
            if (x != 1 || y != 2 || !Integer.valueOf(3).equals(rawZ) || relativeY != 3) {
                failures.add("BlockPos reflection returned unexpected coordinates: x="
                        + x + " y=" + y + " z=" + rawZ + " upY=" + relativeY);
                return;
            }
            checks.add("construct/type/call/get=" + posType.getSimpleName() + "(1,2,3)->upY=" + relativeY);
        } catch (RuntimeException ex) {
            failures.add("construct/call/get failed: " + ex.getMessage());
        }
    }

    private static void checkHandleExtraction(NmsAccess access, Object event, List<String> checks, List<String> failures) {
        if (event == null) {
            checks.add("handle=skipped");
            return;
        }
        try {
            var maybeHandle = access.handleOrEmpty(event);
            if (maybeHandle.isPresent()) {
                checks.add("handle=" + maybeHandle.get().getClass().getName());
            } else {
                checks.add("handle=empty:" + event.getClass().getName());
            }
        } catch (RuntimeException ex) {
            failures.add("handle extraction failed: " + ex.getMessage());
        }
    }

    private static void checkLocalReflection(NmsAccess access, List<String> checks, List<String> failures) {
        try {
            var handle = new DemoHandle("old");
            var api = new DemoApi(handle);
            var extracted = access.handle(api);
            var optional = access.handleOrEmpty(api);
            access.set(extracted, "value", "new");
            var untyped = access.get(extracted, "value");
            var typed = access.get(extracted, "value", String.class);
            var joined = access.call(extracted, "join", String.class, "prefix", 2);
            var rawJoined = access.call(extracted, "join", "raw", 1);
            if (extracted != handle || optional.orElseThrow() != handle
                    || !"new".equals(untyped) || !"new".equals(typed)
                    || !"prefix:new:2".equals(joined) || !"raw:new:1".equals(rawJoined)) {
                failures.add("local handle/get/set/call returned unexpected values");
                return;
            }
            checks.add("handle/handleOrEmpty/get/set/call=" + joined);
        } catch (RuntimeException ex) {
            failures.add("local reflection failed: " + ex.getMessage());
        }
    }

    private void checkHookRegistration(NmsService nms, List<String> checks, List<String> failures) {
        try {
            var messageHooks = nms.hooks().hooks(MESSAGE_REWRITE_HOOK);
            var cancelHooks = nms.hooks().hooks(CANCEL_HOOK);
            var serverHooks = nms.hooks().hooks(SERVER_STARTED_HOOK);
            if (messageHooks.isEmpty() || cancelHooks.isEmpty() || serverHooks.isEmpty()) {
                failures.add("registered hooks are not visible through NmsHookService");
                return;
            }
            var messageHook = messageHooks.getFirst();
            if (!messageHook.hook().equals(MESSAGE_REWRITE_HOOK)
                    || messageHook.hookHandler() == null
                    || messageHook.owner().isBlank()
                    || messageHook.priority() != ServicePriority.HIGH
                    || !messageHook.active()) {
                failures.add("message hook registration metadata is invalid");
                return;
            }
            var rewrite = messageHooks.getFirst().hookHandler().invoke(new DemoHookContext(
                    MESSAGE_REWRITE_HOOK,
                    this,
                    List.of("nms api")));
            var cancel = cancelHooks.getFirst().hookHandler().invoke(new DemoHookContext(
                    CANCEL_HOOK,
                    this,
                    List.of()));
            var server = serverHooks.getFirst().hookHandler().invoke(new DemoHookContext(
                    SERVER_STARTED_HOOK,
                    this,
                    List.of("brand", "version")));
            if (rewrite.action() != NmsHookResult.Action.REPLACE
                    || !"NMS API".equals(rewrite.replacementOrNull())
                    || cancel.action() != NmsHookResult.Action.CANCEL
                    || server.action() != NmsHookResult.Action.PASS) {
                failures.add("registered hook handlers returned unexpected results");
                return;
            }
            var temporary = nms.hooks().register(TEMPORARY_HOOK, hook -> NmsHookResult.pass());
            if (nms.hooks().hooks(TEMPORARY_HOOK).isEmpty()) {
                failures.add("temporary hook was not visible after registration");
                return;
            }
            temporary.unregister();
            if (temporary.active() || !nms.hooks().hooks(TEMPORARY_HOOK).isEmpty()) {
                failures.add("temporary hook remained active after unregister");
                return;
            }
            checks.add("hooks=server:" + serverHooks.size()
                    + ",message:" + messageHooks.size()
                    + ",cancel:" + cancelHooks.size()
                    + ",rewrite=" + rewrite.replacementOrNull());
        } catch (RuntimeException ex) {
            failures.add("hook inspection failed: " + ex.getMessage());
        } catch (Exception ex) {
            failures.add("hook invocation failed: " + ex.getMessage());
        }
    }

    private static String typeName(Object instance) {
        return instance == null ? "null" : instance.getClass().getName();
    }

    private record DemoHookContext(Key hook, Object instance, List<Object> arguments) implements NmsHookContext {
        private DemoHookContext {
            arguments = List.copyOf(arguments);
        }
    }

    private static final class DemoApi {

        private final DemoHandle handle;

        private DemoApi(DemoHandle handle) {
            this.handle = handle;
        }

        @SuppressWarnings("unused")
        private DemoHandle handle() {
            return handle;
        }
    }

    private static final class DemoHandle {

        private String value;

        private DemoHandle(String value) {
            this.value = value;
        }

        @SuppressWarnings("unused")
        private String join(String prefix, int count) {
            return prefix + ":" + value + ":" + count;
        }
    }

    record NmsDemoReport(String mode, List<String> checks, List<String> failures) {
        NmsDemoReport {
            checks = List.copyOf(checks);
            failures = List.copyOf(failures);
        }

        static NmsDemoReport notRun() {
            return new NmsDemoReport("not-run", List.of(), List.of("NMS demo has not run yet"));
        }

        boolean success() {
            return failures.isEmpty();
        }

        String summary() {
            return "mode=" + mode + ", checks=" + checks.size() + ", failures=" + failures.size();
        }
    }
}
