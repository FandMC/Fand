package io.fand.api.advancement;

import com.google.gson.JsonObject;
import java.util.Objects;

public final class AdvancementDistancePredicate {

    private final AdvancementRange x;
    private final AdvancementRange y;
    private final AdvancementRange z;
    private final AdvancementRange horizontal;
    private final AdvancementRange absolute;

    private AdvancementDistancePredicate(
            AdvancementRange x,
            AdvancementRange y,
            AdvancementRange z,
            AdvancementRange horizontal,
            AdvancementRange absolute
    ) {
        this.x = Objects.requireNonNull(x, "x");
        this.y = Objects.requireNonNull(y, "y");
        this.z = Objects.requireNonNull(z, "z");
        this.horizontal = Objects.requireNonNull(horizontal, "horizontal");
        this.absolute = Objects.requireNonNull(absolute, "absolute");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AdvancementDistancePredicate absolute(AdvancementRange absolute) {
        return builder().absolute(absolute).build();
    }

    public static AdvancementDistancePredicate horizontal(AdvancementRange horizontal) {
        return builder().horizontal(horizontal).build();
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        if (!x.any()) {
            json.add("x", x.toJson());
        }
        if (!y.any()) {
            json.add("y", y.toJson());
        }
        if (!z.any()) {
            json.add("z", z.toJson());
        }
        if (!horizontal.any()) {
            json.add("horizontal", horizontal.toJson());
        }
        if (!absolute.any()) {
            json.add("absolute", absolute.toJson());
        }
        return json;
    }

    public static final class Builder {
        private AdvancementRange x = AdvancementRange.ANY;
        private AdvancementRange y = AdvancementRange.ANY;
        private AdvancementRange z = AdvancementRange.ANY;
        private AdvancementRange horizontal = AdvancementRange.ANY;
        private AdvancementRange absolute = AdvancementRange.ANY;

        private Builder() {
        }

        public Builder x(AdvancementRange x) {
            this.x = Objects.requireNonNull(x, "x");
            return this;
        }

        public Builder y(AdvancementRange y) {
            this.y = Objects.requireNonNull(y, "y");
            return this;
        }

        public Builder z(AdvancementRange z) {
            this.z = Objects.requireNonNull(z, "z");
            return this;
        }

        public Builder horizontal(AdvancementRange horizontal) {
            this.horizontal = Objects.requireNonNull(horizontal, "horizontal");
            return this;
        }

        public Builder absolute(AdvancementRange absolute) {
            this.absolute = Objects.requireNonNull(absolute, "absolute");
            return this;
        }

        public AdvancementDistancePredicate build() {
            return new AdvancementDistancePredicate(x, y, z, horizontal, absolute);
        }
    }
}
