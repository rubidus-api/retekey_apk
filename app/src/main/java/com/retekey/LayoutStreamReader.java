package com.retekey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Pure-Java stream reader and URI safety check for imported {@code .rkl} layout files, allowing
 * size-cap and path-traversal checks to be unit-tested on the JVM.
 */
final class LayoutStreamReader {
    enum Status {
        OK,
        TOO_BIG,
        UNREADABLE
    }

    static final class Result {
        final Status status;
        final String text;

        private Result(Status status, String text) {
            this.status = status;
            this.text = text;
        }

        static Result ok(String text) {
            return new Result(Status.OK, text);
        }

        static Result tooBig() {
            return new Result(Status.TOO_BIG, null);
        }

        static Result unreadable() {
            return new Result(Status.UNREADABLE, null);
        }
    }

    private LayoutStreamReader() {
    }

    static Result read(InputStream in, int maxBytes) {
        if (in == null || maxBytes <= 0) {
            return Result.unreadable();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (out.size() <= maxBytes) {
                int toRead = Math.min(buffer.length, maxBytes + 1 - out.size());
                int read = in.read(buffer, 0, toRead);
                if (read < 0) {
                    break;
                }
                if (read > 0) {
                    out.write(buffer, 0, read);
                }
            }
            if (out.size() > maxBytes) {
                return Result.tooBig();
            }
            return Result.ok(new String(out.toByteArray(), "UTF-8"));
        } catch (IOException | RuntimeException e) {
            return Result.unreadable();
        }
    }

    /**
     * Rejects {@code file://} URIs pointing inside ReteKey's own private application directory so an
     * external caller cannot trick the importer into reading internal files.
     */
    static boolean isOwnPrivateFile(String scheme, String path, String dataDir) {
        if (!"file".equalsIgnoreCase(scheme) || path == null || dataDir == null || dataDir.isEmpty()) {
            return false;
        }
        String canonPath = canonicalOrNormalized(path);
        String canonData = canonicalOrNormalized(dataDir);
        if (canonPath.equals(canonData)) {
            return true;
        }
        String prefix = canonData.endsWith(File.separator) ? canonData : canonData + File.separator;
        return canonPath.startsWith(prefix);
    }

    private static String canonicalOrNormalized(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException | RuntimeException ignored) {
            return new File(path).toURI().normalize().getPath();
        }
    }
}
