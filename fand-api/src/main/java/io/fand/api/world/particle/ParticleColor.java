package io.fand.api.world.particle;

/** Immutable particle color. RGB colors use alpha {@code 255}. */
public record ParticleColor(int argb) {

    public static ParticleColor rgb(int rgb) {
        if (rgb < 0 || rgb > 0xFFFFFF) {
            throw new IllegalArgumentException("rgb must be in 0x000000..0xFFFFFF");
        }
        return new ParticleColor(0xFF000000 | rgb);
    }

    public static ParticleColor argb(int argb) {
        return new ParticleColor(argb);
    }

    public static ParticleColor rgb(int red, int green, int blue) {
        return rgba(red, green, blue, 255);
    }

    public static ParticleColor rgba(int red, int green, int blue, int alpha) {
        return new ParticleColor((component(alpha, "alpha") << 24)
                | (component(red, "red") << 16)
                | (component(green, "green") << 8)
                | component(blue, "blue"));
    }

    public int rgb() {
        return argb & 0xFFFFFF;
    }

    public int alpha() {
        return (argb >>> 24) & 0xFF;
    }

    public int red() {
        return (argb >>> 16) & 0xFF;
    }

    public int green() {
        return (argb >>> 8) & 0xFF;
    }

    public int blue() {
        return argb & 0xFF;
    }

    private static int component(int value, String name) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException(name + " must be in 0..255");
        }
        return value;
    }
}
