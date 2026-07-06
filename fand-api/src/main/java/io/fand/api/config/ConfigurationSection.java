package io.fand.api.config;

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A view onto a hierarchical configuration document. Paths are dot-separated
 * (e.g. {@code "server.host"}). Lookups never throw on type mismatch — typed
 * getters return their default instead. Mutating methods are allowed but only
 * persisted by {@link Configuration#save()} on the root view.
 */
public interface ConfigurationSection {

    /** Whether this section contains a value (or sub-section) at {@code path}. */
    boolean contains(String path);

    /** Top-level keys directly under this section. The returned set is a snapshot. */
    Set<String> keys();

    /**
     * Returns the value at {@code path}, or {@code null} if it does not exist.
     * The runtime type matches whatever the YAML parser produced (String,
     * Long/Integer, Double, Boolean, List, or {@link ConfigurationSection}).
     */
    @Nullable Object value(String path);

    String string(String path, String defaultValue);

    @Nullable String string(String path);

    int intValue(String path, int defaultValue);

    long longValue(String path, long defaultValue);

    double doubleValue(String path, double defaultValue);

    boolean booleanValue(String path, boolean defaultValue);

    /**
     * Returns the list at {@code path}, coerced element-wise to {@code String}
     * via {@code toString()}. Returns an empty list when the path is missing
     * or the value is not a list.
     */
    List<String> stringList(String path);

    /**
     * Returns the sub-section at {@code path}, creating an empty one if it
     * does not exist. Mutations on the returned view affect this section.
     */
    ConfigurationSection section(String path);

    /**
     * Sets {@code value} at {@code path}. Pass {@code null} to remove. Maps
     * are stored as nested sections; lists are stored as-is. The change is
     * not persisted until {@link Configuration#save()} runs on the root.
     */
    void set(String path, @Nullable Object value);
}
