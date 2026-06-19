package io.fand.api.plugin;

import io.fand.api.permission.PermissionDescriptor;
import java.util.List;
import java.util.Objects;

/**
 * Static metadata loaded from a plugin's {@code fand-plugin.json}.
 *
 * @param id        unique lowercase identifier (e.g. {@code my-plugin})
 * @param version   semantic version string
 * @param mainClass fully qualified class name implementing {@link Plugin}
 * @param authors   author names; may be empty
 * @param depends   plugin ids that must be loaded before this one
 * @param permissions static permissions declared by this plugin
 */
public record PluginDescriptor(
        String id,
        String version,
        String mainClass,
        List<String> authors,
        List<String> depends,
        List<PermissionDescriptor> permissions
) {
    public PluginDescriptor(String id, String version, String mainClass, List<String> authors, List<String> depends) {
        this(id, version, mainClass, authors, depends, List.of());
    }

    public PluginDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(mainClass, "mainClass");
        authors = List.copyOf(authors);
        depends = List.copyOf(depends);
        permissions = List.copyOf(permissions);
    }
}
