package io.fand.server.region;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fand.api.region.Region;
import io.fand.api.region.RegionDefinition;
import io.fand.api.region.RegionFlag;
import io.fand.api.region.RegionFlagRegistration;
import io.fand.api.region.RegionProtection;
import io.fand.api.region.RegionRegistration;
import io.fand.api.region.RegionService;
import io.fand.api.world.BlockRegion;
import io.fand.api.world.Location;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public final class FandRegionService implements RegionService {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path rootDirectory;
    private final Object lock = new Object();
    private final LinkedHashMap<Key, RegisteredRegion> regions = new LinkedHashMap<>();
    private final LinkedHashMap<Key, RegisteredFlag<?>> flags = new LinkedHashMap<>();
    private long sequence;

    public FandRegionService(Path rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory")
                .toAbsolutePath()
                .normalize();
        ensureRoot();
        loadExistingRegions();
    }

    @Override
    public Path rootDirectory() {
        ensureRoot();
        return rootDirectory;
    }

    @Override
    public Collection<Region> regions() {
        synchronized (lock) {
            return regions.values().stream()
                    .map(RegisteredRegion::region)
                    .toList();
        }
    }

    @Override
    public Optional<Region> region(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return Optional.ofNullable(regions.get(key)).map(RegisteredRegion::region);
        }
    }

    @Override
    public boolean remove(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            if (regions.remove(key) == null) {
                return false;
            }
        }
        deleteRegionFile(key);
        return true;
    }

    @Override
    public Collection<Region> applicableRegions(Location location) {
        Objects.requireNonNull(location, "location");
        synchronized (lock) {
            return regions.values().stream()
                    .filter(entry -> entry.region().contains(location))
                    .sorted(Comparator
                            .comparingInt((RegisteredRegion entry) -> entry.region().protection().priority())
                            .reversed()
                            .thenComparingLong(entry -> entry.region().region().cappedVolume())
                            .thenComparing(Comparator.comparingLong(RegisteredRegion::token).reversed()))
                    .map(RegisteredRegion::region)
                    .toList();
        }
    }

    @Override
    public Optional<Region> applicableRegion(Location location) {
        return applicableRegions(location).stream().findFirst();
    }

    @Override
    public Collection<RegionFlag<?>> flags() {
        synchronized (lock) {
            var result = new ArrayList<RegionFlag<?>>(flags.size());
            for (var entry : flags.values()) {
                result.add(entry.flag());
            }
            return result;
        }
    }

    @Override
    public Optional<RegionFlag<?>> flag(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return Optional.ofNullable(flags.get(key)).map(RegisteredFlag::flag);
        }
    }

    @Override
    public boolean unregisterFlag(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            return flags.remove(key) != null;
        }
    }

    @Override
    public RegionRegistration register(RegionDefinition region) {
        Objects.requireNonNull(region, "region");
        var copy = new Region(region.key(), region.world(), region.region(), region.flags(), region.protection());
        long token;
        synchronized (lock) {
            token = ++sequence;
            regions.put(copy.key(), new RegisteredRegion(copy, token));
        }
        persistRegion(copy);
        return new Registration(this, copy.key(), token);
    }

    @Override
    public <T> RegionFlagRegistration registerFlag(RegionFlag<T> flag) {
        Objects.requireNonNull(flag, "flag");
        long token;
        synchronized (lock) {
            token = ++sequence;
            flags.put(flag.key(), new RegisteredFlag<>(flag, token));
        }
        return new FlagRegistration<>(this, flag.key(), token);
    }

    private void loadExistingRegions() {
        try {
            if (!Files.exists(rootDirectory)) {
                return;
            }
            try (var stream = Files.walk(rootDirectory)) {
                var files = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .toList();
                for (var file : files) {
                    loadRegionFile(file);
                }
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to scan region store " + rootDirectory, failure);
        }
    }

    private void loadRegionFile(Path file) {
        try {
            var element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Region file must contain a JSON object: " + file);
            }
            var definition = readDefinition(file, element.getAsJsonObject());
            synchronized (lock) {
                regions.put(definition.key(), new RegisteredRegion(
                        new Region(
                                definition.key(),
                                definition.world(),
                                definition.region(),
                                definition.flags(),
                                definition.protection()),
                        ++sequence));
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to read region file " + file, failure);
        }
    }

    private RegionDefinition readDefinition(Path file, JsonObject object) {
        var fileKey = keyFromFile(file);
        var key = readKey(object, "key").orElse(fileKey);
        if (!key.equals(fileKey)) {
            throw new IllegalStateException("Region file key mismatch: " + file + " declares " + key + " but is stored as " + fileKey);
        }
        var world = readKey(object, "world").orElseThrow(() -> new IllegalStateException("Region file is missing world: " + file));
        var region = readRegion(object, file);
        var flags = readFlags(object, file);
        var protection = readProtection(object, file);
        return new RegionDefinition(key, world, region, flags, protection);
    }

    private static Optional<Key> readKey(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return Optional.empty();
        }
        var value = object.get(name);
        if (!value.isJsonPrimitive()) {
            throw new IllegalStateException(name + " must be a string");
        }
        return Optional.of(Key.key(value.getAsString()));
    }

    private static BlockRegion readRegion(JsonObject object, Path file) {
        if (!object.has("region") || !object.get("region").isJsonObject()) {
            throw new IllegalStateException("Region file is missing region bounds: " + file);
        }
        var region = object.getAsJsonObject("region");
        return new BlockRegion(
                requiredInt(region, "minX", file),
                requiredInt(region, "minY", file),
                requiredInt(region, "minZ", file),
                requiredInt(region, "maxX", file),
                requiredInt(region, "maxY", file),
                requiredInt(region, "maxZ", file));
    }

    private static Map<Key, JsonElement> readFlags(JsonObject object, Path file) {
        if (!object.has("flags") || object.get("flags").isJsonNull()) {
            return Map.of();
        }
        if (!object.get("flags").isJsonObject()) {
            throw new IllegalStateException("Region file flags must be a JSON object: " + file);
        }
        var flags = new LinkedHashMap<Key, JsonElement>();
        for (var entry : object.getAsJsonObject("flags").entrySet()) {
            flags.put(Key.key(entry.getKey()), entry.getValue().deepCopy());
        }
        return Map.copyOf(flags);
    }

    private static RegionProtection readProtection(JsonObject object, Path file) {
        if (!object.has("protection") || object.get("protection").isJsonNull()) {
            return RegionProtection.empty();
        }
        if (!object.get("protection").isJsonObject()) {
            throw new IllegalStateException("Region file protection must be a JSON object: " + file);
        }
        var protection = object.getAsJsonObject("protection");
        var builder = RegionProtection.builder();
        if (protection.has("priority") && !protection.get("priority").isJsonNull()) {
            builder.priority(protection.get("priority").getAsInt());
        }
        readKey(protection, "parent").ifPresent(builder::parent);
        readSubjects(protection, "owners", file).forEach(builder::owner);
        readSubjects(protection, "members", file).forEach(builder::member);
        return builder.build();
    }

    private static Collection<String> readSubjects(JsonObject object, String name, Path file) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return java.util.List.of();
        }
        if (!object.get(name).isJsonArray()) {
            throw new IllegalStateException("Region file protection " + name + " must be an array: " + file);
        }
        var result = new ArrayList<String>();
        for (var element : object.getAsJsonArray(name)) {
            if (!element.isJsonPrimitive()) {
                throw new IllegalStateException("Region file protection " + name + " must contain strings: " + file);
            }
            result.add(element.getAsString());
        }
        return result;
    }

    private void persistRegion(Region region) {
        var file = regionFile(region.key());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(toJson(region)) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to write region file " + file, failure);
        }
    }

    private void deleteRegionFile(Key key) {
        var file = regionFile(key);
        try {
            Files.deleteIfExists(file);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to delete region file " + file, failure);
        }
    }

    private JsonObject toJson(Region region) {
        var object = new JsonObject();
        object.addProperty("key", region.key().asString());
        object.addProperty("world", region.world().asString());
        object.add("region", regionToJson(region.region()));
        var flagsObject = new JsonObject();
        region.flags().forEach((key, value) -> flagsObject.add(key.asString(), value));
        object.add("flags", flagsObject);
        if (!region.protection().emptyMetadata()) {
            object.add("protection", protectionToJson(region.protection()));
        }
        return object;
    }

    private static JsonObject protectionToJson(RegionProtection protection) {
        var object = new JsonObject();
        if (protection.priority() != 0) {
            object.addProperty("priority", protection.priority());
        }
        protection.parent().ifPresent(parent -> object.addProperty("parent", parent.asString()));
        if (!protection.owners().isEmpty()) {
            object.add("owners", subjectsToJson(protection.owners()));
        }
        if (!protection.members().isEmpty()) {
            object.add("members", subjectsToJson(protection.members()));
        }
        return object;
    }

    private static JsonArray subjectsToJson(Collection<String> subjects) {
        var array = new JsonArray();
        for (var subject : subjects) {
            array.add(subject);
        }
        return array;
    }

    private static JsonObject regionToJson(BlockRegion region) {
        var object = new JsonObject();
        object.addProperty("minX", region.minX());
        object.addProperty("minY", region.minY());
        object.addProperty("minZ", region.minZ());
        object.addProperty("maxX", region.maxX());
        object.addProperty("maxY", region.maxY());
        object.addProperty("maxZ", region.maxZ());
        return object;
    }

    private Path regionFile(Key key) {
        var relative = Path.of(key.value()).normalize();
        if (relative.isAbsolute() || relative.startsWith("..") || relative.toString().equals("..")) {
            throw new IllegalArgumentException("Region key path escapes the region root: " + key.asString());
        }
        var file = rootDirectory.resolve(key.namespace()).resolve(relative.toString() + ".json").normalize();
        if (!file.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("Region file escapes the region root: " + key.asString());
        }
        return file;
    }

    private Key keyFromFile(Path file) {
        var relative = rootDirectory.relativize(file.toAbsolutePath().normalize());
        if (relative.getNameCount() < 2) {
            throw new IllegalStateException("Invalid region file location: " + file);
        }
        var namespace = relative.getName(0).toString();
        var nameCount = relative.getNameCount();
        var path = new StringBuilder();
        for (int i = 1; i < nameCount; i++) {
            if (i > 1) {
                path.append('/');
            }
            path.append(relative.getName(i));
        }
        var value = path.toString();
        if (!value.endsWith(".json")) {
            throw new IllegalStateException("Region file must end with .json: " + file);
        }
        return Key.key(namespace, value.substring(0, value.length() - 5));
    }

    private void ensureRoot() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException failure) {
            throw new UncheckedIOException("Failed to create region root " + rootDirectory, failure);
        }
    }

    private static int requiredInt(JsonObject object, String name, Path file) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()) {
            throw new IllegalStateException("Region file is missing integer field '" + name + "': " + file);
        }
        return object.get(name).getAsInt();
    }

    private boolean active(Key key, long token) {
        synchronized (lock) {
            var entry = regions.get(key);
            return entry != null && entry.token() == token;
        }
    }

    private boolean unregister(Key key, long token) {
        synchronized (lock) {
            var entry = regions.get(key);
            if (entry == null || entry.token() != token) {
                return false;
            }
            regions.remove(key);
        }
        deleteRegionFile(key);
        return true;
    }

    private boolean activeFlag(Key key, long token) {
        synchronized (lock) {
            var entry = flags.get(key);
            return entry != null && entry.token() == token;
        }
    }

    private boolean unregisterFlag(Key key, long token) {
        synchronized (lock) {
            var entry = flags.get(key);
            if (entry == null || entry.token() != token) {
                return false;
            }
            flags.remove(key);
            return true;
        }
    }

    private record RegisteredRegion(Region region, long token) {
    }

    private record RegisteredFlag<T>(RegionFlag<T> flag, long token) {
    }

    private static final class Registration implements RegionRegistration {

        private final FandRegionService owner;
        private final Key key;
        private final long token;
        private volatile boolean active = true;

        private Registration(FandRegionService owner, Key key, long token) {
            this.owner = owner;
            this.key = key;
            this.token = token;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active && owner.active(key, token);
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.unregister(key, token);
            }
        }
    }

    private static final class FlagRegistration<T> implements RegionFlagRegistration {

        private final FandRegionService owner;
        private final Key key;
        private final long token;
        private volatile boolean active = true;

        private FlagRegistration(FandRegionService owner, Key key, long token) {
            this.owner = owner;
            this.key = key;
            this.token = token;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active && owner.activeFlag(key, token);
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.unregisterFlag(key, token);
            }
        }
    }
}
