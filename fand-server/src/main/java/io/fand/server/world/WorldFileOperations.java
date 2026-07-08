package io.fand.server.world;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public final class WorldFileOperations {

    private static final String SESSION_LOCK = "session.lock";

    private WorldFileOperations() {
    }

    public static void copyWorldDirectory(Path source, Path target) throws IOException {
        var normalizedSource = source.toAbsolutePath().normalize();
        var normalizedTarget = target.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedSource)) {
            throw new IOException("World directory does not exist: " + normalizedSource);
        }
        if (normalizedTarget.getParent() == null) {
            throw new IOException("Target world directory cannot be a filesystem root: " + normalizedTarget);
        }
        if (normalizedTarget.startsWith(normalizedSource)) {
            throw new IOException("Target world directory cannot be inside source directory: " + normalizedTarget);
        }
        if (normalizedSource.startsWith(normalizedTarget)) {
            throw new IOException("Target world directory cannot contain source directory: " + normalizedTarget);
        }
        deleteRecursively(normalizedTarget);
        Files.createDirectories(normalizedTarget);
        Files.walkFileTree(normalizedSource, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = normalizedSource.relativize(dir);
                Files.createDirectories(normalizedTarget.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (SESSION_LOCK.equals(file.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Path relative = normalizedSource.relativize(file);
                Files.copy(file, normalizedTarget.resolve(relative), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                if (ex != null) {
                    throw ex;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
