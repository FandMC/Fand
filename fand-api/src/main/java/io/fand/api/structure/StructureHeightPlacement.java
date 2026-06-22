package io.fand.api.structure;

/**
 * Vertical placement policy for generated template structures.
 */
public record StructureHeightPlacement(StructureHeightmap heightmap, int offset) {

    public StructureHeightPlacement {
        java.util.Objects.requireNonNull(heightmap, "heightmap");
    }

    public static StructureHeightPlacement worldSurface() {
        return new StructureHeightPlacement(StructureHeightmap.WORLD_SURFACE_WG, 0);
    }

    public static StructureHeightPlacement oceanFloor() {
        return new StructureHeightPlacement(StructureHeightmap.OCEAN_FLOOR_WG, 0);
    }

    public StructureHeightPlacement withOffset(int offset) {
        return new StructureHeightPlacement(heightmap, offset);
    }
}
