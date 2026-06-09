package io.fand.api.entity;

import java.util.Locale;
import net.kyori.adventure.text.Component;

/**
 * Text display entity.
 */
public interface TextDisplay extends Display {

    Component text();

    void setText(Component text);

    int lineWidth();

    void setLineWidth(int width);

    int textOpacity();

    void setTextOpacity(int opacity);

    int backgroundColor();

    void setBackgroundColor(int argb);

    boolean shadowed();

    void setShadowed(boolean shadowed);

    boolean seeThrough();

    void setSeeThrough(boolean seeThrough);

    boolean defaultBackground();

    void setDefaultBackground(boolean defaultBackground);

    Alignment alignment();

    void setAlignment(Alignment alignment);

    enum Alignment {
        CENTER,
        LEFT,
        RIGHT;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
