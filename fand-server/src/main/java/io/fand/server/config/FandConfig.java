package io.fand.server.config;

import io.fand.server.network.ProxyForwardingMode;
import java.nio.file.Path;

public final class FandConfig {

    @ConfigComment("Public-facing identity settings.")
    public final Identity identity = new Identity();

    @ConfigComment("Plugin runtime settings.")
    public final Plugins plugins = new Plugins();

    @ConfigComment("Async scheduler settings.")
    public final Scheduler scheduler = new Scheduler();

    @ConfigComment("Server console and GUI settings.")
    public final Console console = new Console();

    @ConfigComment("Network and proxy settings.")
    public final Network network = new Network();

    @ConfigComment("Chunk loading and player chunk-send scheduling settings.")
    public final Chunks chunks = new Chunks();

    @ConfigComment("Game-path performance optimizations. All entries preserve vanilla behaviour unless noted.")
    public final Performance performance = new Performance();

    public static FandConfig load(Path path) {
        var config = new YamlConfigLoader<>(FandConfig.class).load(path);
        validate(config);
        return config;
    }

    private static void validate(FandConfig config) {
        var mode = ProxyForwardingMode.fromConfig(config.network.forwarding.mode);
        if (mode.requiresSecret() && config.network.forwarding.secret.isBlank()) {
            throw new ConfigException("network.forwarding.secret must be set when network.forwarding.mode is " + mode.configValue());
        }
        io.fand.server.console.gui.GuiTheme.fromConfig(config.console.gui.theme);
    }

    public static final class Identity {

        @ConfigComment("Brand reported by the server and plugin API.")
        public String brand = "Fand";
    }

    public static final class Plugins {

        @ConfigComment("Directory scanned for plugin jars and plugin data folders.")
        public String directory = "plugins";

        @ConfigComment("Continue boot if a plugin fails during discovery, construction, or onLoad.")
        public boolean continueOnLoadFailure = true;

        @ConfigComment("Continue boot if a plugin fails during onEnable.")
        public boolean continueOnEnableFailure = true;

        @ConfigComment("Log a summary after plugin load and enable phases.")
        public boolean logSummary = true;
    }

    public static final class Scheduler {

        @ConfigComment({
                "Number of async scheduler threads.",
                "Set to 0 to derive the value from available processors."
        })
        @ConfigRange(min = 0, max = 1024)
        public int asyncThreads = 0;
    }

    public static final class Chunks {

        @ConfigComment({
                "Worker threads used for safe chunk tracking diff calculations.",
                "Set to 0 to derive the value from available processors."
        })
        @ConfigRange(min = 0, max = 64)
        public int workerThreads = 0;

        @ConfigComment({
                "Maximum player chunk tracking diff jobs completed per server tick.",
                "Set to 0 to apply every completed job in the same tick."
        })
        @ConfigRange(min = 0, max = 4096)
        public int trackingDiffApplyBudget = 256;
    }

    public static final class Performance {

        @ConfigComment({
                "Cache explosion line-of-sight exposure per (center, entity bounding box)",
                "within a single level tick. The vanilla algorithm re-traces ~27 rays per",
                "nearby entity per explosion through the same blocks, which is quadratic",
                "when many explosions share a tick (TNT chains, cannons).",
                "Behaviour note: explosions in the same tick share cached results, so an",
                "entity's cached exposure may predate block changes made by an earlier",
                "explosion in that tick. This matches Paper's optimize-explosions trade-off."
        })
        public boolean explosionDensityCache = true;

        @ConfigComment({
                "Cache each entity's scoreboard team lookup, invalidated when team",
                "membership changes. Entity collision checks resolve the pusher's and",
                "every candidate's team once per pair; with hundreds of entities stacked",
                "together that is hundreds of thousands of scoreboard hash lookups per",
                "tick. Results are identical to vanilla."
        })
        public boolean collisionTeamCache = true;

        @ConfigComment({
                "Cache block state, fluid resistance, and world-bounds checks per block",
                "position within a single explosion's ray pass. The 1352 explosion rays",
                "revisit each block in the blast sphere ~5 times on average, and no block",
                "mutates until after all rays finish, so results are identical to vanilla."
        })
        public boolean explosionBlockCache = true;

        @ConfigComment({
                "Maximum TNT detonations processed per level tick. Set to 0 for vanilla",
                "behaviour (all fused TNT detonates in the same tick). When positive,",
                "TNT past the budget keeps its primed state and detonates on a following",
                "tick instead - nothing is cancelled, large chains are spread out so a",
                "15k-TNT detonation becomes a wave over a few seconds instead of a",
                "single multi-minute tick that trips the watchdog.",
                "Recommended starting point for cannon/stress servers: 200-500."
        })
        @ConfigRange(min = 0, max = 1_000_000)
        public int tntDetonationBudget = 0;

        @ConfigComment({
                "Use a hash map for explosion drop merging instead of linear search.",
                "Vanilla loops over all collected drops to find merge targets (O(n²)).",
                "This switches to a hash-keyed lookup (O(n)), strictly vanilla-equivalent."
        })
        public boolean explosionDropHashMerge = true;

        @ConfigComment({
                "Route explosion entity-exposure sight rays through the explosion block",
                "cache instead of chunk lookups. Each nearby entity costs ~27 line-of-",
                "sight rays of ~10 block steps; vanilla pays two chunk lookups per step.",
                "The blast sphere is already cached by the ray pass, blocks cannot",
                "change mid-explosion, and collision shapes are still resolved per",
                "entity - results are identical to vanilla."
        })
        public boolean explosionExposureClipCache = true;

        @ConfigComment("Cache entity queries during explosions (avoids repeated AABB scans).")
        public boolean explosionEntityCache = true;
    }

    public static final class Console {

        @ConfigComment("Graphical server console window settings.")
        public final Gui gui = new Gui();
    }

    public static final class Gui {

        @ConfigComment("Show the graphical console window when a display is available and --nogui is not set.")
        public boolean enabled = true;

        @ConfigComment({
                "Initial colour theme for the GUI.",
                "Supported values: dark, light, system. Unknown values are rejected.",
                "The theme can also be switched at runtime from the GUI itself."
        })
        public String theme = "system";
    }

    public static final class Network {

        @ConfigComment("Proxy player information forwarding settings.")
        public final Forwarding forwarding = new Forwarding();
    }

    public static final class Forwarding {

        @ConfigComment({
                "Proxy forwarding mode.",
                "Supported values: none, bungee-legacy, velocity-modern."
        })
        public String mode = "none";

        @ConfigComment("Shared secret used by velocity-modern forwarding.")
        public String secret = "";
    }
}
