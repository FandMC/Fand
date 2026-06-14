package io.fand.server;

import io.fand.server.world.LevelChunkInterceptor;
import io.fand.server.world.LevelChunkLoadedInterceptor;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicBoolean;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.jspecify.annotations.Nullable;

/**
 * Process entry point for the Fand runtime.
 */
public final class Main {

    private static volatile @Nullable FandServer runtime;

    private Main() {}

    public static FandServer runtime() {
        var local = runtime;
        if (local == null) {
            throw new IllegalStateException("Fand runtime has not been bootstrapped yet");
        }
        return local;
    }

    public static @Nullable FandServer runtimeOrNull() {
        return runtime;
    }

    static void bind(FandServer server) {
        synchronized (Main.class) {
            if (runtime != null) {
                throw new IllegalStateException("Fand runtime is already bootstrapped");
            }
            runtime = server;
        }
    }

    static void unbind(FandServer server) {
        synchronized (Main.class) {
            if (runtime == server) {
                runtime = null;
            }
        }
    }

    public static void main(String[] args) {
        initFandAgent();
        var server = new FandServer();
        bind(server);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "Fand-Shutdown"));
        try {
            server.start();
            net.minecraft.server.Main.main(args);
            server.awaitMinecraftServerStop();
        } finally {
            try {
                server.close();
            } finally {
                unbind(server);
            }
        }
    }

    private static void initFandAgent() {
        try {
            var instrumentation = ByteBuddyAgent.install();
            var serverLevelHooked = new AtomicBoolean(false);
            var levelChunkHooked = new AtomicBoolean(false);
            new AgentBuilder.Default()
                    .ignore(ElementMatchers.none())
                    .type(ElementMatchers.named("net.minecraft.server.level.ServerLevel"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                            .method(ElementMatchers.named("getChunk")
                                    .and(ElementMatchers.takesArguments(4))
                                    .and(ElementMatchers.takesArgument(0, int.class))
                                    .and(ElementMatchers.takesArgument(1, int.class))
                                    .and(ElementMatchers.takesArgument(
                                            2,
                                            ElementMatchers.named("net.minecraft.world.level.chunk.status.ChunkStatus")
                                    ))
                                    .and(ElementMatchers.takesArgument(3, boolean.class)))
                            .intercept(MethodDelegation.to(LevelChunkInterceptor.class)))
                    .with(new AgentInstallListener("net.minecraft.server.level.ServerLevel", serverLevelHooked))
                    .installOn(instrumentation);
            new AgentBuilder.Default()
                    .ignore(ElementMatchers.none())
                    .type(ElementMatchers.named("net.minecraft.world.level.chunk.LevelChunk"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                            .method(ElementMatchers.named("setLoaded")
                                    .and(ElementMatchers.takesArguments(boolean.class)))
                            .intercept(MethodDelegation.to(LevelChunkLoadedInterceptor.class)))
                    .with(new AgentInstallListener("net.minecraft.world.level.chunk.LevelChunk", levelChunkHooked))
                    .installOn(instrumentation);
            reportFandAgentStatus(instrumentation, serverLevelHooked, levelChunkHooked);
        } catch (Throwable failure) {
            System.err.println("[Fand] Failed to initialize ByteBuddy agent");
            failure.printStackTrace(System.err);
        }
    }

    private static void reportFandAgentStatus(
            Instrumentation instrumentation,
            AtomicBoolean serverLevelHooked,
            AtomicBoolean levelChunkHooked
    ) {
        boolean serverLevelLoaded = false;
        boolean levelChunkLoaded = false;
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            if (loadedClass.getName().equals("net.minecraft.server.level.ServerLevel")) {
                serverLevelLoaded = true;
            } else if (loadedClass.getName().equals("net.minecraft.world.level.chunk.LevelChunk")) {
                levelChunkLoaded = true;
            }
        }

        if (serverLevelHooked.get() && levelChunkHooked.get()) {
            System.out.println("[Fand] ByteBuddy agent initialized and chunk cache hooks installed");
            return;
        }
        if (!serverLevelLoaded && !levelChunkLoaded) {
            System.out.println("[Fand] ByteBuddy agent initialized; chunk cache hooks are pending class load");
            return;
        }
        System.err.println("[Fand] ByteBuddy agent initialized but chunk cache hook verification is incomplete: "
                + "ServerLevel=" + hookStatus(serverLevelLoaded, serverLevelHooked.get())
                + ", LevelChunk=" + hookStatus(levelChunkLoaded, levelChunkHooked.get()));
    }

    private static String hookStatus(boolean loaded, boolean transformed) {
        if (transformed) {
            return "transformed";
        }
        return loaded ? "loaded-not-transformed" : "pending-load";
    }

    private static final class AgentInstallListener implements AgentBuilder.Listener {

        private final String targetName;
        private final AtomicBoolean transformed;

        private AgentInstallListener(String targetName, AtomicBoolean transformed) {
            this.targetName = targetName;
            this.transformed = transformed;
        }

        @Override
        public void onDiscovery(String typeName, @Nullable ClassLoader classLoader, @Nullable JavaModule module, boolean loaded) {
        }

        @Override
        public void onTransformation(
                TypeDescription typeDescription,
                @Nullable ClassLoader classLoader,
                @Nullable JavaModule module,
                boolean loaded,
                DynamicType dynamicType
        ) {
            if (typeDescription.getName().equals(targetName)) {
                transformed.set(true);
            }
        }

        @Override
        public void onIgnored(
                TypeDescription typeDescription,
                @Nullable ClassLoader classLoader,
                @Nullable JavaModule module,
                boolean loaded
        ) {
        }

        @Override
        public void onError(
                String typeName,
                @Nullable ClassLoader classLoader,
                @Nullable JavaModule module,
                boolean loaded,
                Throwable throwable
        ) {
            if (typeName.equals(targetName)) {
                System.err.println("[Fand] Failed to transform " + targetName);
                throwable.printStackTrace(System.err);
            }
        }

        @Override
        public void onComplete(
                String typeName,
                @Nullable ClassLoader classLoader,
                @Nullable JavaModule module,
                boolean loaded
        ) {
        }
    }
}
