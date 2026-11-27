package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

/** Bounds checking and path-safety checks for importing {@code .rkl} layouts from URIs. */
public final class LayoutStreamReaderTest {
    @Test
    public void exactLimitSucceedsAndOneByteOverReportsTooBig() {
        byte[] exact = new byte[64];
        java.util.Arrays.fill(exact, (byte) 'a');
        LayoutStreamReader.Result ok = LayoutStreamReader.read(new ByteArrayInputStream(exact), 64);
        assertEquals(LayoutStreamReader.Status.OK, ok.status);
        assertEquals(64, ok.text.length());

        byte[] oversize = new byte[65];
        java.util.Arrays.fill(oversize, (byte) 'a');
        LayoutStreamReader.Result big = LayoutStreamReader.read(
            new ByteArrayInputStream(oversize), 64);
        assertEquals(LayoutStreamReader.Status.TOO_BIG, big.status);
        assertNull(big.text);
    }

    @Test
    public void ioErrorAndNullStreamReportUnreadable() {
        assertEquals(LayoutStreamReader.Status.UNREADABLE, LayoutStreamReader.read(null, 64).status);
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken pipe");
            }
        };
        assertEquals(LayoutStreamReader.Status.UNREADABLE,
            LayoutStreamReader.read(failing, 64).status);
    }

    @Test
    public void ownPrivateFilesAreBlockedWhileContentAndExternalFilesPass() {
        String dataDir = "/data/user/0/com.retekey";
        assertTrue(LayoutStreamReader.isOwnPrivateFile(
            "file", "/data/user/0/com.retekey/shared_prefs/retekey_clips.xml", dataDir));
        assertTrue(LayoutStreamReader.isOwnPrivateFile(
            "file", "/data/user/0/com.retekey/files/../shared_prefs/x.xml", dataDir));
        assertFalse(LayoutStreamReader.isOwnPrivateFile(
            "content", "/data/user/0/com.retekey/cache/foo.rkl", dataDir));
        assertFalse(LayoutStreamReader.isOwnPrivateFile(
            "file", "/sdcard/Download/custom.rkl", dataDir));
        assertFalse(LayoutStreamReader.isOwnPrivateFile(
            "file", "/data/user/0/com.retekey_other/custom.rkl", dataDir));
    }
}
