package io.fand.fandclip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class FileHashes {

    private FileHashes() {}

    static String sha1(Path path) throws IOException {
        return hash(path, 40);
    }

    static String hash(Path path, int expectedLength) throws IOException {
        String algorithm = expectedLength == 64 ? "SHA-256" : "SHA-1";
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                }
            }
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(algorithm + " missing from JDK", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            out[i * 2] = hex[b >>> 4];
            out[i * 2 + 1] = hex[b & 0x0f];
        }
        return new String(out);
    }
}
