package dev.hellgates.retekeyime;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Opens the bundled Hanja indexes once and keeps them for the process.
 *
 * <p>The files are memory-mapped straight out of the APK, which is why they are stored there
 * uncompressed: mapping costs no Java heap and no copy, the pages are file-backed and clean, and
 * the kernel is free to drop them when memory is short and fetch them again on the next lookup.
 * Parsing the same data into hash maps cost about 3.5 MB of heap for 190 KB of text.
 *
 * <p>A failure to open them leaves an empty table rather than a broken keyboard: 한자 conversion
 * simply finds nothing.
 */
public final class HanjaDictionary {
    private static final String FORWARD = "hanja_fwd.idx";
    private static final String REVERSE = "hanja_rev.idx";
    private static final String GLOSSES = "hanja_hunum.idx";

    private static volatile MappedHanjaTable table;

    private HanjaDictionary() {
    }

    public static MappedHanjaTable get(Context context) {
        MappedHanjaTable local = table;
        if (local == null) {
            synchronized (HanjaDictionary.class) {
                local = table;
                if (local == null) {
                    local = new MappedHanjaTable(
                        open(context, FORWARD), open(context, REVERSE), open(context, GLOSSES));
                    table = local;
                }
            }
        }
        return local;
    }

    /**
     * Warms the mapping off the calling thread. Mapping is cheap, but the first lookup still faults
     * pages in, and doing that on the thread a keystroke arrives on is how a keyboard stutters.
     */
    public static void preload(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> get(app), "hanja-preload").start();
    }

    private static SortedIndex open(Context context, String assetName) {
        try (AssetFileDescriptor descriptor = context.getAssets().openFd(assetName);
             FileInputStream stream = descriptor.createInputStream()) {
            FileChannel channel = stream.getChannel();
            ByteBuffer mapped = channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.getStartOffset(),
                descriptor.getLength());
            return SortedIndex.over(mapped);
        } catch (IOException | RuntimeException unavailable) {
            // openFd throws when the asset is stored compressed, which would be a build mistake;
            // either way the keyboard keeps working without Hanja rather than failing to start.
            return SortedIndex.empty();
        }
    }
}
