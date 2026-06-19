package io.fand.api.map;

import java.util.Optional;

public interface MapService {

    Optional<MapView> map(int id);

    default MapView create(MapRenderer renderer) {
        throw new UnsupportedOperationException("Custom map rendering is not supported");
    }

    default MapView create(PlayerMapRenderer renderer) {
        return create((MapRenderer) renderer);
    }

    static MapService empty() {
        return id -> Optional.empty();
    }
}
