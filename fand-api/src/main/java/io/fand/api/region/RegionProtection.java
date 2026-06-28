package io.fand.api.region;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

/**
 * Protection metadata used when resolving overlapping regions.
 */
public record RegionProtection(
        int priority,
        Optional<Key> parent,
        Set<String> owners,
        Set<String> members
) {

    private static final RegionProtection EMPTY = new RegionProtection(
            0,
            Optional.empty(),
            Set.of(),
            Set.of());

    public RegionProtection {
        parent = Objects.requireNonNull(parent, "parent");
        owners = copySubjects(owners, "owners");
        members = copySubjects(members, "members");
    }

    public static RegionProtection empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean emptyMetadata() {
        return priority == 0 && parent.isEmpty() && owners.isEmpty() && members.isEmpty();
    }

    public boolean owner(String subject) {
        return owners.contains(normalizeSubject(subject));
    }

    public boolean member(String subject) {
        var normalized = normalizeSubject(subject);
        return owners.contains(normalized) || members.contains(normalized);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    static String normalizeSubject(String subject) {
        var normalized = Objects.requireNonNull(subject, "subject").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }
        return normalized;
    }

    private static Set<String> copySubjects(Collection<String> subjects, String name) {
        Objects.requireNonNull(subjects, name);
        if (subjects.isEmpty()) {
            return Set.of();
        }
        var copy = new LinkedHashSet<String>();
        for (var subject : subjects) {
            copy.add(normalizeSubject(subject));
        }
        return Set.copyOf(copy);
    }

    public static final class Builder {

        private int priority;
        private Optional<Key> parent = Optional.empty();
        private final LinkedHashSet<String> owners = new LinkedHashSet<>();
        private final LinkedHashSet<String> members = new LinkedHashSet<>();

        private Builder() {
        }

        private Builder(RegionProtection protection) {
            this.priority = protection.priority;
            this.parent = protection.parent;
            this.owners.addAll(protection.owners);
            this.members.addAll(protection.members);
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder parent(@Nullable Key parent) {
            this.parent = Optional.ofNullable(parent);
            return this;
        }

        public Builder parent(Optional<Key> parent) {
            this.parent = Objects.requireNonNull(parent, "parent");
            return this;
        }

        public Builder owner(String owner) {
            owners.add(normalizeSubject(owner));
            return this;
        }

        public Builder owners(Collection<String> owners) {
            this.owners.clear();
            this.owners.addAll(copySubjects(owners, "owners"));
            return this;
        }

        public Builder member(String member) {
            members.add(normalizeSubject(member));
            return this;
        }

        public Builder members(Collection<String> members) {
            this.members.clear();
            this.members.addAll(copySubjects(members, "members"));
            return this;
        }

        public RegionProtection build() {
            return new RegionProtection(priority, parent, owners, members);
        }
    }
}
