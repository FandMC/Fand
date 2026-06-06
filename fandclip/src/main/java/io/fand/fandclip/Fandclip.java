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
 * Standalone launcher distributed to end users. Resolves the vanilla server
 * libraries from a Mojang bundler, builds a classpath, and hands control to the
 * Fand server entry point in the same JVM.
 *
 * <p>The classpath layout is intentionally flat: every library jar plus the
 * bundled {@code fand-server.jar} (extracted from this jar's resources). Nothing
 * is downloaded that the bundler itself does not list, and every download is
 * SHA-1 verified against the bundler's manifest.
 */
public final class Fandclip {

    private static final String FAND_MAIN_CLASS = "io.fand.server.Main";

    private Fandclip() {}

    public static void main(String[] args) throws Exception {
        Path home = resolveHome();
        Path libsDir = home.resolve("libraries");
        Files.createDirectories(libsDir);

        Path bundler = home.resolve("vanilla-bundler.jar");
        if (!Files.exists(bundler)) {
            String version = ClipManifest.minecraftVersion();
            System.out.println("[fandclip] no bundler cached, downloading vanilla " + version);
            new VanillaDownloader().download(version, bundler);
        }

        BundlerLayout layout = BundlerLayout.read(bundler);
        List<Path> libraries = layout.materialiseLibraries(libsDir);
        Path serverJar = home.resolve("fand-server.jar");
        ResourceExtractor.extract("/fand-server.jar", serverJar);

        List<Path> classpath = new java.util.ArrayList<>(libraries.size() + 1);
        classpath.add(serverJar);
        classpath.addAll(libraries);

        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < classpath.size(); i++) {
            urls[i] = classpath.get(i).toUri().toURL();
        }

        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        try (URLClassLoader cl = new URLClassLoader("fand", urls, parent)) {
            Thread.currentThread().setContextClassLoader(cl);
            Class<?> mainClass = Class.forName(FAND_MAIN_CLASS, true, cl);
            Method main = mainClass.getMethod("main", String[].class);
            try {
                main.invoke(null, (Object) args);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getTargetException();
                if (cause instanceof RuntimeException re) throw re;
                if (cause instanceof Error err) throw err;
                throw new RuntimeException(cause);
            }
        }
    }

    private static Path resolveHome() throws IOException {
        String override = System.getProperty("fand.home");
        Path base = override != null ? Paths.get(override) : Paths.get(".").toAbsolutePath().normalize().resolve(".fand");
        Files.createDirectories(base);
        return base;
    }
}
