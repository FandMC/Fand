package io.fand.server.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.fand.api.command.CommandRegistry;
import io.fand.api.event.EventBus;
import io.fand.api.packet.PacketRegistry;
import io.fand.api.permission.PermissionService;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginDescriptor;
import io.fand.api.plugin.PluginManager;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.scheduler.Scheduler;
import io.fand.server.recipe.FandRecipeRegistry;
import io.fand.server.network.packet.PacketRegistryImpl;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginRuntime implements PluginManager, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRuntime.class);
    private static final Gson GSON = new Gson();
    private static final Pattern PLUGIN_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final String DESCRIPTOR_PATH = "fand-plugin.json";

    private final Path pluginsDirectory;
    private final Path dataDirectoryRoot;
    private final ClassLoader parentClassLoader;
    private final CommandRegistry commandRegistry;
    private final EventBus eventBus;
    private final PermissionService permissions;
    private final RecipeRegistry recipeRegistry;
    private final PacketRegistry packetRegistry;
    private final Scheduler scheduler;
    private volatile Options options;
    private final ConcurrentHashMap<String, LoadedPlugin> loadedPlugins = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> loadOrder = new CopyOnWriteArrayList<>();
    private volatile boolean loaded;
    private volatile boolean enabled;
    private volatile boolean closed;

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                new FandRecipeRegistry(),
                new PacketRegistryImpl(),
                Options.defaults()
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                new FandRecipeRegistry(),
                new PacketRegistryImpl(),
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            Options options
    ) {
        this(
                pluginsDirectory,
                dataDirectoryRoot,
                parentClassLoader,
                commandRegistry,
                eventBus,
                permissions,
                scheduler,
                recipeRegistry,
                new PacketRegistryImpl(),
                options
        );
    }

    public PluginRuntime(
            Path pluginsDirectory,
            Path dataDirectoryRoot,
            ClassLoader parentClassLoader,
            CommandRegistry commandRegistry,
            EventBus eventBus,
            PermissionService permissions,
            Scheduler scheduler,
            RecipeRegistry recipeRegistry,
            PacketRegistry packetRegistry,
            Options options
    ) {
        this.pluginsDirectory = pluginsDirectory;
        this.dataDirectoryRoot = dataDirectoryRoot;
        this.parentClassLoader = parentClassLoader;
        this.commandRegistry = commandRegistry;
        this.eventBus = eventBus;
        this.permissions = permissions;
        this.recipeRegistry = recipeRegistry;
        this.packetRegistry = packetRegistry;
        this.scheduler = scheduler;
        this.options = options;
    }

    public void reconfigure(Options options) {
        this.options = options;
    }

    public void loadPlugins() {
        ensureOpen();
        if (loaded) {
            throw new IllegalStateException("Plugins are already loaded");
        }
        try {
            Files.createDirectories(pluginsDirectory);
            Files.createDirectories(dataDirectoryRoot);
            var artifacts = discoverArtifacts();
            var ordered = sortArtifacts(artifacts);
            var skipped = 0;
            for (var artifact : ordered) {
                var unavailableDependency = firstUnavailableDependency(artifact.descriptor.depends());
                if (unavailableDependency != null) {
                    if (!options.continueOnLoadFailure()) {
                        close();
                        throw new PluginLoadException("Plugin '" + artifact.descriptor.id() + "' depends on unavailable plugin '" + unavailableDependency + "'");
                    }
                    skipped++;
                    LOGGER.warn("Skipping plugin {} because dependency {} is unavailable", artifact.descriptor.id(), unavailableDependency);
                    continue;
                }

                var dependencies = dependencyClassLoaders(artifact.descriptor.depends());
                var classLoader = new PluginClassLoader(toJarUrl(artifact.jarPath), parentClassLoader, dependencies);
                var resources = new PluginResourceTracker();
                var context = new RuntimePluginContext(
                        artifact.descriptor,
                        LoggerFactory.getLogger(artifact.descriptor.id()),
                        new PluginEventBus(eventBus, resources),
                        permissions,
                        new PluginCommandRegistry(commandRegistry, resources, artifact.descriptor.id()),
                        new PluginRecipeRegistry(recipeRegistry, resources, artifact.descriptor.id()),
                        new PluginPacketRegistry(packetRegistry, resources),
                        new PluginScheduler(scheduler, resources),
                        dataDirectoryRoot.resolve(artifact.descriptor.id()),
                        resources,
                        classLoader
                );
                try {
                    var plugin = instantiatePlugin(artifact.descriptor, classLoader);
                    plugin.onLoad(context);
                    loadedPlugins.put(artifact.descriptor.id(), new LoadedPlugin(artifact.descriptor, plugin, context, classLoader));
                    loadOrder.add(artifact.descriptor.id());
                } catch (Throwable failure) {
                    context.close();
                    closeQuietly(classLoader, artifact.descriptor.id());
                    if (!options.continueOnLoadFailure()) {
                        close();
                        throw new PluginLoadException("Failed to load plugin '" + artifact.descriptor.id() + "'", failure);
                    }
                    skipped++;
                    LOGGER.warn("Skipping plugin {} after load failure", artifact.descriptor.id(), failure);
                }
            }
            loaded = true;
            if (options.logSummary()) {
                LOGGER.info("Plugin load summary: discovered={}, loaded={}, skipped={}", artifacts.size(), loadedPlugins.size(), skipped);
            }
        } catch (IOException failure) {
            close();
            throw new PluginLoadException("Failed to scan plugins directory", failure);
        } catch (RuntimeException failure) {
            close();
            throw failure;
        } catch (Error failure) {
            close();
            throw failure;
        }
    }

    public void enablePlugins() {
        ensureOpen();
        if (!loaded) {
            throw new IllegalStateException("Plugins must be loaded before enabling");
        }
        if (enabled) {
            throw new IllegalStateException("Plugins are already enabled");
        }
        var enabledThisRun = new ArrayDeque<LoadedPlugin>();
        var skipped = 0;
        try {
            for (var pluginId : loadOrder) {
                var loadedPlugin = loadedPlugins.get(pluginId);
                if (loadedPlugin == null) {
                    continue;
                }
                var unavailableDependency = firstDisabledDependency(loadedPlugin.descriptor.depends());
                if (unavailableDependency != null) {
                    if (!options.continueOnEnableFailure()) {
                        throw new PluginLoadException("Plugin '" + loadedPlugin.descriptor.id() + "' depends on disabled plugin '" + unavailableDependency + "'");
                    }
                    skipped++;
                    LOGGER.warn("Skipping plugin {} because dependency {} is disabled", loadedPlugin.descriptor.id(), unavailableDependency);
                    discardLoadedPlugin(loadedPlugin);
                    continue;
                }
                try {
                    loadedPlugin.plugin.onEnable(loadedPlugin.context);
                    loadedPlugin.enabled = true;
                    enabledThisRun.push(loadedPlugin);
                } catch (Throwable failure) {
                    discardLoadedPlugin(loadedPlugin);
                    if (!options.continueOnEnableFailure()) {
                        throw new PluginLoadException("Failed to enable plugin '" + loadedPlugin.descriptor.id() + "'", failure);
                    }
                    skipped++;
                    LOGGER.warn("Skipping plugin {} after enable failure", loadedPlugin.descriptor.id(), failure);
                }
            }
            enabled = true;
            if (options.logSummary()) {
                LOGGER.info("Plugin enable summary: loaded={}, enabled={}, skipped={}", loadedPlugins.size(), enabledThisRun.size(), skipped);
            }
        } catch (Throwable failure) {
            while (!enabledThisRun.isEmpty()) {
                disablePlugin(enabledThisRun.pop());
            }
            throw failure;
        }
    }

    public void disablePlugins() {
        if (!enabled) {
            return;
        }
        for (int i = loadOrder.size() - 1; i >= 0; i--) {
            var loadedPlugin = loadedPlugins.get(loadOrder.get(i));
            if (loadedPlugin != null) {
                disablePlugin(loadedPlugin);
            }
        }
        enabled = false;
    }

    @Override
    public Collection<Plugin> loaded() {
        var plugins = new ArrayList<Plugin>(loadOrder.size());
        for (var pluginId : loadOrder) {
            var loadedPlugin = loadedPlugins.get(pluginId);
            if (loadedPlugin != null) {
                plugins.add(loadedPlugin.plugin);
            }
        }
        return List.copyOf(plugins);
    }

    @Override
    public Optional<Plugin> byId(String id) {
        var loadedPlugin = loadedPlugins.get(id);
        return loadedPlugin == null ? Optional.empty() : Optional.of(loadedPlugin.plugin);
    }

    @Override
    public boolean isEnabled(String id) {
        var loadedPlugin = loadedPlugins.get(id);
        return loadedPlugin != null && loadedPlugin.enabled;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        disablePlugins();
        for (int i = loadOrder.size() - 1; i >= 0; i--) {
            var pluginId = loadOrder.get(i);
            var loadedPlugin = loadedPlugins.get(pluginId);
            if (loadedPlugin == null) {
                continue;
            }
            loadedPlugin.context.close();
            closeQuietly(loadedPlugin.classLoader, loadedPlugin.descriptor.id());
        }
        loadedPlugins.clear();
        loadOrder.clear();
        loaded = false;
        enabled = false;
    }

    private List<PluginArtifact> discoverArtifacts() throws IOException {
        try (var stream = Files.list(pluginsDirectory)) {
            var jars = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            var artifacts = new ArrayList<PluginArtifact>(jars.size());
            var seenIds = new LinkedHashMap<String, Path>();
            for (var jar : jars) {
                var descriptor = readDescriptor(jar);
                var existing = seenIds.putIfAbsent(descriptor.id(), jar);
                if (existing != null) {
                    throw new PluginLoadException("Duplicate plugin id '" + descriptor.id() + "' in " + existing + " and " + jar);
                }
                artifacts.add(new PluginArtifact(jar, descriptor));
            }
            return artifacts;
        }
    }

    private PluginDescriptor readDescriptor(Path jarPath) {
        try (var jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry(DESCRIPTOR_PATH);
            if (entry == null) {
                throw new PluginLoadException("Plugin jar " + jarPath + " is missing " + DESCRIPTOR_PATH);
            }
            try (var reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                var file = GSON.fromJson(reader, DescriptorFile.class);
                if (file == null) {
                    throw new PluginLoadException("Plugin jar " + jarPath + " contains an empty " + DESCRIPTOR_PATH);
                }
                return validateDescriptor(jarPath, file.toDescriptor());
            }
        } catch (IOException | JsonParseException failure) {
            throw new PluginLoadException("Failed to read descriptor from " + jarPath, failure);
        }
    }

    private PluginDescriptor validateDescriptor(Path jarPath, PluginDescriptor descriptor) {
        if (!PLUGIN_ID.matcher(descriptor.id()).matches()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " has invalid id '" + descriptor.id() + "'");
        }
        if (!descriptor.id().equals(descriptor.id().toLowerCase(Locale.ROOT))) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must use a lowercase id");
        }
        if (descriptor.version().isBlank()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must declare a non-empty version");
        }
        if (descriptor.mainClass().isBlank()) {
            throw new PluginLoadException("Plugin jar " + jarPath + " must declare a non-empty mainClass");
        }
        for (var dependency : descriptor.depends()) {
            if (!PLUGIN_ID.matcher(dependency).matches()) {
                throw new PluginLoadException("Plugin jar " + jarPath + " has invalid dependency id '" + dependency + "'");
            }
        }
        return descriptor;
    }

    private List<PluginArtifact> sortArtifacts(List<PluginArtifact> artifacts) {
        var byId = new LinkedHashMap<String, PluginArtifact>();
        var incomingEdges = new LinkedHashMap<String, Integer>();
        var dependents = new LinkedHashMap<String, List<String>>();
        for (var artifact : artifacts) {
            byId.put(artifact.descriptor.id(), artifact);
            incomingEdges.put(artifact.descriptor.id(), 0);
            dependents.put(artifact.descriptor.id(), new ArrayList<>());
        }
        for (var artifact : artifacts) {
            for (var dependency : artifact.descriptor.depends()) {
                if (!byId.containsKey(dependency)) {
                    throw new PluginLoadException("Plugin '" + artifact.descriptor.id() + "' depends on missing plugin '" + dependency + "'");
                }
                dependents.get(dependency).add(artifact.descriptor.id());
                incomingEdges.put(artifact.descriptor.id(), incomingEdges.get(artifact.descriptor.id()) + 1);
            }
        }
        var ready = new ArrayDeque<PluginArtifact>();
        for (var artifact : artifacts) {
            if (incomingEdges.get(artifact.descriptor.id()) == 0) {
                ready.addLast(artifact);
            }
        }
        var ordered = new ArrayList<PluginArtifact>(artifacts.size());
        while (!ready.isEmpty()) {
            var artifact = ready.removeFirst();
            ordered.add(artifact);
            for (var dependent : dependents.get(artifact.descriptor.id())) {
                var remaining = incomingEdges.get(dependent) - 1;
                incomingEdges.put(dependent, remaining);
                if (remaining == 0) {
                    ready.addLast(byId.get(dependent));
                }
            }
        }
        if (ordered.size() != artifacts.size()) {
            throw new PluginLoadException("Plugin dependency graph contains a cycle");
        }
        return ordered;
    }

    private List<PluginClassLoader> dependencyClassLoaders(List<String> dependencyIds) {
        var dependencies = new ArrayList<PluginClassLoader>(dependencyIds.size());
        for (var dependencyId : dependencyIds) {
            var loadedPlugin = loadedPlugins.get(dependencyId);
            if (loadedPlugin == null) {
                throw new PluginLoadException("Plugin '" + dependencyId + "' must be loaded before its dependents");
            }
            dependencies.add(loadedPlugin.classLoader);
        }
        return dependencies;
    }

    private String firstUnavailableDependency(List<String> dependencyIds) {
        for (var dependencyId : dependencyIds) {
            if (!loadedPlugins.containsKey(dependencyId)) {
                return dependencyId;
            }
        }
        return null;
    }

    private String firstDisabledDependency(List<String> dependencyIds) {
        for (var dependencyId : dependencyIds) {
            var loadedPlugin = loadedPlugins.get(dependencyId);
            if (loadedPlugin == null || !loadedPlugin.enabled) {
                return dependencyId;
            }
        }
        return null;
    }

    private Plugin instantiatePlugin(PluginDescriptor descriptor, PluginClassLoader classLoader) {
        try {
            var type = classLoader.loadClass(descriptor.mainClass());
            if (!Plugin.class.isAssignableFrom(type)) {
                throw new PluginLoadException("Plugin '" + descriptor.id() + "' main class does not implement Plugin");
            }
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Plugin) constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException ex) {
            throw new PluginLoadException("Failed to construct plugin '" + descriptor.id() + "'", ex);
        } catch (InvocationTargetException ex) {
            throw new PluginLoadException("Plugin '" + descriptor.id() + "' constructor failed", ex.getTargetException());
        }
    }

    private static java.net.URL toJarUrl(Path jarPath) {
        try {
            return jarPath.toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new PluginLoadException("Invalid plugin path " + jarPath, ex);
        }
    }

    private void disablePlugin(LoadedPlugin loadedPlugin) {
        try {
            if (loadedPlugin.enabled) {
                loadedPlugin.plugin.onDisable(loadedPlugin.context);
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("Plugin {} failed during disable", loadedPlugin.descriptor.id(), failure);
        } finally {
            loadedPlugin.enabled = false;
            loadedPlugin.context.close();
        }
    }

    private void discardLoadedPlugin(LoadedPlugin loadedPlugin) {
        loadedPlugin.enabled = false;
        loadedPlugins.remove(loadedPlugin.descriptor.id(), loadedPlugin);
        loadOrder.remove(loadedPlugin.descriptor.id());
        loadedPlugin.context.close();
        closeQuietly(loadedPlugin.classLoader, loadedPlugin.descriptor.id());
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Plugin runtime is closed");
        }
    }

    private static void closeQuietly(PluginClassLoader classLoader, String pluginId) {
        try {
            classLoader.close();
        } catch (IOException failure) {
            LOGGER.warn("Failed to close classloader for plugin {}", pluginId, failure);
        }
    }

    private record PluginArtifact(Path jarPath, PluginDescriptor descriptor) {
    }

    private static final class LoadedPlugin {

        private final PluginDescriptor descriptor;
        private final Plugin plugin;
        private final RuntimePluginContext context;
        private final PluginClassLoader classLoader;
        private volatile boolean enabled;

        private LoadedPlugin(
                PluginDescriptor descriptor,
                Plugin plugin,
                RuntimePluginContext context,
                PluginClassLoader classLoader
        ) {
            this.descriptor = descriptor;
            this.plugin = plugin;
            this.context = context;
            this.classLoader = classLoader;
        }
    }

    private record DescriptorFile(
            String id,
            String version,
            String mainClass,
            List<String> authors,
            List<String> depends
    ) {
        private PluginDescriptor toDescriptor() {
            return new PluginDescriptor(
                    required(id, "id"),
                    required(version, "version"),
                    required(mainClass, "mainClass"),
                    authors == null ? List.of() : authors,
                    depends == null ? List.of() : depends
            );
        }

        private static String required(String value, String name) {
            if (value == null) {
                throw new PluginLoadException("Plugin descriptor is missing '" + name + "'");
            }
            return value.trim();
        }
    }

    public record Options(
            boolean continueOnLoadFailure,
            boolean continueOnEnableFailure,
            boolean logSummary
    ) {
        public static Options defaults() {
            return new Options(false, false, true);
        }
    }
}
