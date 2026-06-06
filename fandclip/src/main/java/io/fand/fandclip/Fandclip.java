package io.fand.fandclip;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    private Fandclip() {}

    public static void main(String[] args) throws Exception {
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
        if (!Files.exists(bundler)) {
            System.out.println("[fandclip] no vanilla cache, downloading mojang_" + minecraftVersion + ".jar");
            new VanillaDownloader().download(minecraftVersion, bundler);
        }

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
        runThread.start();
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
