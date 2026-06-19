package io.fand.fandclip;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone launcher distributed to end users. It materialises the embedded
 * Fand server and libraries, downloads the vanilla bundled server when needed,
 * and starts the real server main class in an isolated class loader.
 *
 * <p>The on-disk layout mirrors Paperclip: {@code cache/} stores the downloaded
 * Mojang jar, {@code versions/} stores the Fand server jar, and
 * {@code libraries/} stores both Fand and vanilla library jars.
 */
public final class Fandclip {

    private static final String QUIET_JVM_WARNINGS_PROPERTY = "fandclip.quietJvmWarnings";
    private static final String QUIET_JVM_WARNINGS_RESTARTED_PROPERTY = "fandclip.quietJvmWarnings.restarted";
    private static final String ENABLE_NATIVE_ACCESS = "--enable-native-access=ALL-UNNAMED";
    private static final String ALLOW_SUN_MISC_UNSAFE = "--sun-misc-unsafe-memory-access=allow";

    private Fandclip() {}

    public static void main(String[] args) throws Exception {
        relaunchWithQuietJvmWarnings(args);

        if (Paths.get("").toAbsolutePath().toString().contains("!")) {
            System.err.println("Fandclip may not run in a directory containing '!'. Please rename the affected folder.");
            System.exit(1);
        }

        Path repoDir = resolveRepoDir();
        Path cacheDir = repoDir.resolve("cache");
        Path versionsDir = repoDir.resolve("versions");
        Path librariesDir = repoDir.resolve("libraries");

        String minecraftVersion = ClipManifest.minecraftVersion();
        Path bundler = cacheDir.resolve("mojang_" + minecraftVersion + ".jar");
        new VanillaDownloader().ensureCached(minecraftVersion, bundler);

        BundledLayout bundled = BundledLayout.read();
        List<Path> versions = bundled.materialiseVersions(versionsDir);
        List<Path> fandLibraries = bundled.materialiseLibraries(librariesDir);
        List<Path> vanillaLibraries = BundlerLayout.read(bundler).materialiseLibraries(librariesDir);

        List<Path> classpath = new ArrayList<>(versions.size() + fandLibraries.size() + vanillaLibraries.size());
        classpath.addAll(versions);
        classpath.addAll(fandLibraries);
        classpath.addAll(vanillaLibraries);

        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < classpath.size(); i++) {
            urls[i] = classpath.get(i).toUri().toURL();
        }

        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        URLClassLoader classLoader = new URLClassLoader("fand", urls, parent);
        String mainClassName = bundled.mainClass();
        System.out.println("Starting " + mainClassName);

        runServerMain(mainClassName, classLoader, args);
    }

    static void runServerMain(String mainClassName, ClassLoader classLoader, String[] args) throws InterruptedException {
        Thread runThread = new Thread(() -> {
            try {
                Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
                Method main = mainClass.getMethod("main", String[].class);
                main.invoke(null, (Object) args);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getTargetException();
                throw sneakyThrow(cause);
            } catch (Throwable t) {
                throw sneakyThrow(t);
            }
        }, "ServerMain");
        runThread.setContextClassLoader(classLoader);
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        runThread.setUncaughtExceptionHandler((thread, failure) -> serverFailure.set(failure));
        runThread.start();
        try {
            runThread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        var failure = serverFailure.get();
        if (failure != null) {
            throw sneakyThrow(failure);
        }
    }

    private static void relaunchWithQuietJvmWarnings(String[] args) throws IOException {
        if (!shouldRelaunchWithQuietJvmWarnings(ManagementFactory.getRuntimeMXBean().getInputArguments())) {
            return;
        }
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        List<String> quietArgs = missingQuietJvmArgs(jvmArgs);
        List<String> command = new ArrayList<>();
        command.add(javaExecutable().toString());
        command.addAll(jvmArgs);
        command.addAll(quietArgs);
        command.add("-D" + QUIET_JVM_WARNINGS_RESTARTED_PROPERTY + "=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Fandclip.class.getName());
        command.addAll(Arrays.asList(args));

        Process process = new ProcessBuilder(command)
                .inheritIO()
                .start();
        try {
            System.exit(process.waitFor());
        } catch (InterruptedException interrupted) {
            process.destroy();
            Thread.currentThread().interrupt();
            System.exit(130);
        }
    }

    static boolean shouldRelaunchWithQuietJvmWarnings(List<String> jvmArgs) {
        return Boolean.parseBoolean(System.getProperty(QUIET_JVM_WARNINGS_PROPERTY, "true"))
                && !Boolean.getBoolean(QUIET_JVM_WARNINGS_RESTARTED_PROPERTY)
                && !missingQuietJvmArgs(jvmArgs).isEmpty();
    }

    static List<String> missingQuietJvmArgs(List<String> jvmArgs) {
        return missingQuietJvmArgs(jvmArgs, Runtime.version().feature());
    }

    static List<String> missingQuietJvmArgs(List<String> jvmArgs, int javaFeature) {
        List<String> missing = new ArrayList<>(2);
        if (javaFeature >= 24 && !hasEnableNativeAccess(jvmArgs)) {
            missing.add(ENABLE_NATIVE_ACCESS);
        }
        if (javaFeature >= 24 && !hasSunMiscUnsafeAllow(jvmArgs)) {
            missing.add(ALLOW_SUN_MISC_UNSAFE);
        }
        return missing;
    }

    private static boolean hasEnableNativeAccess(List<String> jvmArgs) {
        return hasOptionValue(jvmArgs, "--enable-native-access", "ALL-UNNAMED");
    }

    private static boolean hasSunMiscUnsafeAllow(List<String> jvmArgs) {
        return hasOptionValue(jvmArgs, "--sun-misc-unsafe-memory-access", "allow");
    }

    private static boolean hasOptionValue(List<String> jvmArgs, String option, String expectedValue) {
        for (int i = 0; i < jvmArgs.size(); i++) {
            String arg = jvmArgs.get(i);
            if (arg.equals(option) && i + 1 < jvmArgs.size() && optionValueContains(jvmArgs.get(i + 1), expectedValue)) {
                return true;
            }
            if (arg.startsWith(option + "=") && optionValueContains(arg.substring(option.length() + 1), expectedValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean optionValueContains(String values, String expectedValue) {
        for (String value : values.split(",")) {
            if (value.trim().equals(expectedValue)) {
                return true;
            }
        }
        return false;
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe"
                : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }

    private static Path resolveRepoDir() throws IOException {
        String override = System.getProperty("bundlerRepoDir");
        if (override == null || override.isBlank()) {
            override = System.getProperty("fand.home");
        }
        Path base = override == null || override.isBlank()
                ? Paths.get("").toAbsolutePath().normalize()
                : Paths.get(override);
        Files.createDirectories(base);
        return base;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
