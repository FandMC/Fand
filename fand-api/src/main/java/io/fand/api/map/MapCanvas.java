package io.fand.api.map;

import java.util.List;

public interface MapCanvas {
    int WIDTH = 128;
    int HEIGHT = 128;

    default byte pixel(int x, int y) {
        throw new UnsupportedOperationException("Reading map pixels is not supported");
    }

    void pixel(int x, int y, byte color);

    default List<MapCursor> cursors() {
        return List.of();
    }

    default void cursors(List<MapCursor> cursors) {
        throw new UnsupportedOperationException("Map cursors are not supported");
    }

    default void clear(byte color) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                pixel(x, y, color);
            }
        }
    }
}
