package io.fand.server;

import io.fand.server.world.LevelChunkInterceptor;
import io.fand.server.world.LevelChunkLoadedInterceptor;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
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
                    .installOn(instrumentation);
            new AgentBuilder.Default()
                    .ignore(ElementMatchers.none())
                    .type(ElementMatchers.named("net.minecraft.world.level.chunk.LevelChunk"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                            .method(ElementMatchers.named("setLoaded")
                                    .and(ElementMatchers.takesArguments(boolean.class)))
                            .intercept(MethodDelegation.to(LevelChunkLoadedInterceptor.class)))
                    .installOn(instrumentation);
            System.out.println("[Fand] ByteBuddy agent initialized and chunk cache hooks installed");
        } catch (Throwable failure) {
            System.err.println("[Fand] Failed to initialize ByteBuddy agent");
            failure.printStackTrace(System.err);
        }
    }
}
