package dev.hellgates.retekeyime;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the bundled Hanja assets once and caches them for the process: the reading↔Hanja
 * {@link HanjaTable} ({@code hanja.txt}) and the {@link HunumTable} of glosses
 * ({@code hanja_hunum.txt}). The files are small (~90 KB each) so a lazy first-use load is cheap;
 * {@code preload} warms both on a background thread so the first 한자 press has no parse latency.
 */
public final class HanjaDictionary {
    private static volatile HanjaTable table;
    private static volatile HunumTable hunum;

    private HanjaDictionary() {
    }

    public static HanjaTable get(Context context) {
        HanjaTable local = table;
        if (local == null) {
            synchronized (HanjaDictionary.class) {
                local = table;
                if (local == null) {
                    local = HanjaTable.parse(readLines(context, "hanja.txt"));
                    table = local;
                }
            }
        }
        return local;
    }

    public static HunumTable hunum(Context context) {
        HunumTable local = hunum;
        if (local == null) {
            synchronized (HanjaDictionary.class) {
                local = hunum;
                if (local == null) {
                    local = HunumTable.parse(readLines(context, "hanja_hunum.txt"));
                    hunum = local;
                }
            }
        }
        return local;
    }

    /** Warms both caches off the calling thread; safe to call more than once. */
    public static void preload(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            get(app);
            hunum(app);
        }, "hanja-preload").start();
    }

    private static List<String> readLines(Context context, String assetName) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(assetName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException failure) {
            return Collections.emptyList();
        }
        return lines;
    }
}
