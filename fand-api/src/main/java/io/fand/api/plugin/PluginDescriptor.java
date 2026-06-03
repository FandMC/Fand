package io.fand.api.plugin;

import java.util.List;

/**
 * Static metadata loaded from a plugin's {@code fand-plugin.json}.
 *
 * @param id        unique lowercase identifier (e.g. {@code my-plugin})
 * @param version   semantic version string
 * @param mainClass fully qualified class name implementing {@link Plugin}
 * @param authors   author names; may be empty
 * @param depends   plugin ids that must be loaded before this one
 */
public record PluginDescriptor(
        String id,
        String version,
        String mainClass,
        List<String> authors,
        List<String> depends
) {
    public PluginDescriptor {
        authors = List.copyOf(authors);
        depends = List.copyOf(depends);
    }
}
