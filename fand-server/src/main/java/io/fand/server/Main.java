package io.fand.server;

import io.fand.server.console.ConsoleLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Process entry point for the Fand runtime.
 */
public final class Main {

    private static volatile @Nullable FandServer runtime;
    private static final Pattern LOCALE_ARGUMENT = Pattern.compile("[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)+");

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
        var launchOptions = LaunchOptions.parse(args);
        ConsoleLanguage.configure(launchOptions.consoleLanguage());

        var server = new FandServer();
        bind(server);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "Fand-Shutdown"));
        try {
            server.start();
            net.minecraft.server.Main.main(launchOptions.minecraftArgs());
            server.awaitMinecraftServerStop();
        } finally {
            try {
                server.close();
            } finally {
                unbind(server);
            }
        }
    }

    record LaunchOptions(String consoleLanguage, String[] minecraftArgs) {
        static LaunchOptions parse(String[] args) {
            String language = ConsoleLanguage.DEFAULT_LOCALE;
            List<String> forwarded = new ArrayList<>(args.length);

            for (int index = 0; index < args.length; index++) {
                var arg = args[index];
                var inline = inlineLanguage(arg);
                if (inline != null) {
                    language = inline;
                    continue;
                }
                if (isLanguageOption(arg)) {
                    if (index + 1 < args.length && isLocaleArgument(args[index + 1])) {
                        language = args[++index];
                    }
                    continue;
                }
                forwarded.add(arg);
            }

            return new LaunchOptions(language, forwarded.toArray(String[]::new));
        }

        private static @Nullable String inlineLanguage(String arg) {
            if (arg.startsWith("-lang=")) {
                return arg.substring("-lang=".length());
            }
            if (arg.startsWith("--lang=")) {
                return arg.substring("--lang=".length());
            }
            return null;
        }

        private static boolean isLanguageOption(String arg) {
            return "-lang".equals(arg) || "--lang".equals(arg);
        }

        private static boolean isLocaleArgument(String arg) {
            return LOCALE_ARGUMENT.matcher(arg).matches();
        }
    }
}
