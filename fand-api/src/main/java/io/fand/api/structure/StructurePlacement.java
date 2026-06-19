package io.fand.api.structure;

import java.util.Objects;

public record StructurePlacement(
        StructureRotation rotation,
        StructureMirror mirrorMode,
        boolean includeEntities,
        boolean ignoreStructureVoid,
        boolean knownShape,
        float integrity,
        long seed,
        int updateFlags
) {

    public StructurePlacement(float rotationDegrees, boolean mirror, boolean includeEntities) {
        this(StructureRotation.fromDegrees(rotationDegrees), mirror ? StructureMirror.FRONT_BACK : StructureMirror.NONE, includeEntities);
    }

    public StructurePlacement(StructureRotation rotation, StructureMirror mirror, boolean includeEntities) {
        this(rotation, mirror, includeEntities, true, true, 1.0F, 0L, StructureUpdateFlags.DEFAULT);
    }

    public StructurePlacement {
        Objects.requireNonNull(rotation, "rotation");
        Objects.requireNonNull(mirrorMode, "mirrorMode");
        if (integrity < 0.0F || integrity > 1.0F) {
            throw new IllegalArgumentException("integrity must be in 0.0..1.0");
        }
    }

    public static StructurePlacement defaults() {
        return new StructurePlacement(StructureRotation.NONE, StructureMirror.NONE, true);
    }

    public float rotationDegrees() {
        return rotation.degrees();
    }

    public boolean mirror() {
        return mirrorMode != StructureMirror.NONE;
    }

    public StructurePlacement withRotation(StructureRotation rotation) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withMirror(StructureMirror mirror) {
        return new StructurePlacement(rotation, mirror, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withIncludeEntities(boolean includeEntities) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withIgnoreStructureVoid(boolean ignoreStructureVoid) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withKnownShape(boolean knownShape) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withIntegrity(float integrity) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withSeed(long seed) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }

    public StructurePlacement withUpdateFlags(int updateFlags) {
        return new StructurePlacement(rotation, mirrorMode, includeEntities, ignoreStructureVoid, knownShape, integrity, seed, updateFlags);
    }
}
