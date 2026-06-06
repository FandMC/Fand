package io.fand.server.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class PluginClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final List<PluginClassLoader> dependencies;

    public PluginClassLoader(URL jarUrl, ClassLoader parent, List<PluginClassLoader> dependencies) {
        super(new URL[] {jarUrl}, parent);
        this.dependencies = List.copyOf(dependencies);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            var loaded = findLoadedClass(name);
            if (loaded != null) {
                return resolveIfNeeded(loaded, resolve);
            }

            if (mustDelegateFirst(name)) {
                return resolveIfNeeded(getParent().loadClass(name), resolve);
            }

            try {
                return resolveIfNeeded(findClass(name), resolve);
            } catch (ClassNotFoundException ignored) {
            }

            for (var dependency : dependencies) {
                try {
                    var visited = Collections.newSetFromMap(new IdentityHashMap<PluginClassLoader, Boolean>());
                    visited.add(this);
                    return resolveIfNeeded(dependency.loadClassFromDependency(name, visited), resolve);
                } catch (ClassNotFoundException ignored) {
                }
            }

            return resolveIfNeeded(getParent().loadClass(name), resolve);
        }
    }

    private Class<?> loadClassFromDependency(String name, Set<PluginClassLoader> visited) throws ClassNotFoundException {
        if (!visited.add(this)) {
            throw new ClassNotFoundException(name);
        }
        synchronized (getClassLoadingLock(name)) {
            var loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }
            try {
                return findClass(name);
            } catch (ClassNotFoundException ignored) {
            }
            for (var dependency : dependencies) {
                try {
                    return dependency.loadClassFromDependency(name, visited);
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw new ClassNotFoundException(name);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    private static boolean mustDelegateFirst(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("io.fand.api.")
                || name.startsWith("io.fand.server.")
                || name.startsWith("org.slf4j.")
                || name.startsWith("org.jspecify.")
                || name.startsWith("net.kyori.")
                || name.startsWith("com.google.gson.");
    }

    private Class<?> resolveIfNeeded(Class<?> type, boolean resolve) {
        if (resolve) {
            resolveClass(type);
        }
        return type;
    }
}
