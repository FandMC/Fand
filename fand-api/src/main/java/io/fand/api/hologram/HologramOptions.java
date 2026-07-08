package io.fand.api.hologram;

import io.fand.api.entity.Display;
import io.fand.api.entity.TextDisplay;

/**
 * Display tuning for a hologram.
 */
public record HologramOptions(
        double lineSpacing,
        int lineWidth,
        int textOpacity,
        int backgroundColor,
        boolean shadowed,
        boolean seeThrough,
        boolean defaultBackground,
        TextDisplay.Alignment alignment,
        Display.Billboard billboard,
        float viewRange
) {

    private static final HologramOptions DEFAULTS = builder().build();

    public HologramOptions {
        if (!Double.isFinite(lineSpacing) || lineSpacing <= 0.0D) {
            throw new IllegalArgumentException("lineSpacing must be finite and > 0");
        }
        if (lineWidth < 1) {
            throw new IllegalArgumentException("lineWidth must be >= 1");
        }
        if (textOpacity < -1 || textOpacity > 255) {
            throw new IllegalArgumentException("textOpacity must be between -1 and 255");
        }
        if (!Float.isFinite(viewRange) || viewRange < 0.0F) {
            throw new IllegalArgumentException("viewRange must be finite and >= 0");
        }
        alignment = java.util.Objects.requireNonNull(alignment, "alignment");
        billboard = java.util.Objects.requireNonNull(billboard, "billboard");
    }

    public static HologramOptions defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .lineSpacing(lineSpacing)
                .lineWidth(lineWidth)
                .textOpacity(textOpacity)
                .backgroundColor(backgroundColor)
                .shadowed(shadowed)
                .seeThrough(seeThrough)
                .defaultBackground(defaultBackground)
                .alignment(alignment)
                .billboard(billboard)
                .viewRange(viewRange);
    }

    public static final class Builder {

        private double lineSpacing = 0.25D;
        private int lineWidth = 200;
        private int textOpacity = -1;
        private int backgroundColor = 0x40000000;
        private boolean shadowed = true;
        private boolean seeThrough;
        private boolean defaultBackground;
        private TextDisplay.Alignment alignment = TextDisplay.Alignment.CENTER;
        private Display.Billboard billboard = Display.Billboard.CENTER;
        private float viewRange = 64.0F;

        private Builder() {
        }

        public Builder lineSpacing(double lineSpacing) {
            this.lineSpacing = lineSpacing;
            return this;
        }

        public Builder lineWidth(int lineWidth) {
            this.lineWidth = lineWidth;
            return this;
        }

        public Builder textOpacity(int textOpacity) {
            this.textOpacity = textOpacity;
            return this;
        }

        public Builder backgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder shadowed(boolean shadowed) {
            this.shadowed = shadowed;
            return this;
        }

        public Builder seeThrough(boolean seeThrough) {
            this.seeThrough = seeThrough;
            return this;
        }

        public Builder defaultBackground(boolean defaultBackground) {
            this.defaultBackground = defaultBackground;
            return this;
        }

        public Builder alignment(TextDisplay.Alignment alignment) {
            this.alignment = alignment == null ? TextDisplay.Alignment.CENTER : alignment;
            return this;
        }

        public Builder billboard(Display.Billboard billboard) {
            this.billboard = billboard == null ? Display.Billboard.CENTER : billboard;
            return this;
        }

        public Builder viewRange(float viewRange) {
            this.viewRange = viewRange;
            return this;
        }

        public HologramOptions build() {
            return new HologramOptions(
                    lineSpacing,
                    lineWidth,
                    textOpacity,
                    backgroundColor,
                    shadowed,
                    seeThrough,
                    defaultBackground,
                    alignment,
                    billboard,
                    viewRange);
        }
    }
}
