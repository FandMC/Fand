package io.fand.api.resourcepack;

/**
 * A file stored inside a managed resource pack.
 */
public record ResourcePackFile(String packId, String path, long size) {

    public ResourcePackFile {
        packId = ResourcePack.normalizeId(packId);
        path = ResourcePack.normalizeRelativePath(path);
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
    }
}
