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
 * @param description short human-readable plugin description; may be blank
 * @param website   plugin website or documentation URL; may be blank
 * @param license   license identifier or name; may be blank
 * @param apiVersion Fand API version targeted by this plugin
 * @param authors   author names; may be empty
 * @param depends   plugin ids that must be loaded before this one
 * @param loadAfter plugin ids that should be loaded before this one when present and whose classes are visible when available
 * @param loadBefore plugin ids that should be loaded after this one when present; soft ordering only
 * @param permissions static permissions declared by this plugin
 */
public record PluginDescriptor(
        String id,
        String version,
        String mainClass,
        String description,
        String website,
        String license,
        String apiVersion,
        List<String> authors,
        List<String> depends,
        List<String> loadAfter,
        List<String> loadBefore,
        List<PermissionDescriptor> permissions
) {
    public static final String CURRENT_API_VERSION = "0.1.1";

    public PluginDescriptor(String id, String version, String mainClass, List<String> authors, List<String> depends) {
        this(id, version, mainClass, "", "", "", CURRENT_API_VERSION, authors, depends, List.of(), List.of(), List.of());
    }

    public PluginDescriptor(
            String id,
            String version,
            String mainClass,
            List<String> authors,
            List<String> depends,
            List<PermissionDescriptor> permissions
    ) {
        this(id, version, mainClass, "", "", "", CURRENT_API_VERSION, authors, depends, List.of(), List.of(), permissions);
    }

    public PluginDescriptor(
            String id,
            String version,
            String mainClass,
            List<String> authors,
            List<String> depends,
            List<String> loadAfter,
            List<String> loadBefore,
            List<PermissionDescriptor> permissions
    ) {
        this(id, version, mainClass, "", "", "", CURRENT_API_VERSION, authors, depends, loadAfter, loadBefore, permissions);
    }

    public PluginDescriptor(
            String id,
            String version,
            String mainClass,
            String description,
            String website,
            String license,
            String apiVersion,
            List<String> authors,
            List<String> depends,
            List<PermissionDescriptor> permissions
    ) {
        this(id, version, mainClass, description, website, license, apiVersion, authors, depends, List.of(), List.of(), permissions);
    }

    public PluginDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(mainClass, "mainClass");
        description = Objects.requireNonNull(description, "description").trim();
        website = Objects.requireNonNull(website, "website").trim();
        license = Objects.requireNonNull(license, "license").trim();
        apiVersion = Objects.requireNonNull(apiVersion, "apiVersion").trim();
        if (apiVersion.isBlank()) {
            throw new IllegalArgumentException("apiVersion cannot be blank");
        }
        authors = List.copyOf(Objects.requireNonNull(authors, "authors"));
        depends = List.copyOf(Objects.requireNonNull(depends, "depends"));
        loadAfter = List.copyOf(Objects.requireNonNull(loadAfter, "loadAfter"));
        loadBefore = List.copyOf(Objects.requireNonNull(loadBefore, "loadBefore"));
        permissions = List.copyOf(Objects.requireNonNull(permissions, "permissions"));
    }
}
