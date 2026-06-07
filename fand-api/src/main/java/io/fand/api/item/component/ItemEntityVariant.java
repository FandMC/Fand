package io.fand.api.item.component;

import java.util.Locale;

/** String-backed vanilla entity variants used by item data components. */
public final class ItemEntityVariant {

    private ItemEntityVariant() {
    }

    public enum Fox {
        RED,
        SNOW;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Fox fromSerializedName(String value) {
            return Fox.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum SalmonSize {
        SMALL,
        MEDIUM,
        LARGE;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static SalmonSize fromSerializedName(String value) {
            return SalmonSize.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Parrot {
        RED_BLUE,
        BLUE,
        GREEN,
        YELLOW_BLUE,
        GRAY;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Parrot fromSerializedName(String value) {
            return Parrot.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum TropicalFishPattern {
        KOB,
        SUNSTREAK,
        SNOOPER,
        DASHER,
        BRINELY,
        SPOTTY,
        FLOPPER,
        STRIPEY,
        GLITTER,
        BLOCKFISH,
        BETTY,
        CLAYFISH;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static TropicalFishPattern fromSerializedName(String value) {
            return TropicalFishPattern.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Mooshroom {
        RED,
        BROWN;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Mooshroom fromSerializedName(String value) {
            return Mooshroom.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Rabbit {
        BROWN,
        WHITE,
        BLACK,
        WHITE_SPLOTCHED,
        GOLD,
        SALT,
        EVIL;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Rabbit fromSerializedName(String value) {
            return Rabbit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Horse {
        WHITE,
        CREAMY,
        CHESTNUT,
        BROWN,
        BLACK,
        GRAY,
        DARK_BROWN;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Horse fromSerializedName(String value) {
            return Horse.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Llama {
        CREAMY,
        WHITE,
        BROWN,
        GRAY;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Llama fromSerializedName(String value) {
            return Llama.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public enum Axolotl {
        LUCY,
        WILD,
        GOLD,
        CYAN,
        BLUE;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Axolotl fromSerializedName(String value) {
            return Axolotl.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }
}
