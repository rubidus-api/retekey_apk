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
 * Loads the bundled {@code hanja.txt} asset once into a {@link HanjaTable} and caches it for the
 * process. The file is small (~90 KB) so a lazy first-use load is cheap; {@code preload} lets the
 * service warm it on a background thread so the first 한자 press has no parse latency.
 */
public final class HanjaDictionary {
    private static volatile HanjaTable table;

    private HanjaDictionary() {
    }

    public static HanjaTable get(Context context) {
        HanjaTable local = table;
        if (local == null) {
            synchronized (HanjaDictionary.class) {
                local = table;
                if (local == null) {
                    local = load(context);
                    table = local;
                }
            }
        }
        return local;
    }

    /** Warms the cache off the calling thread; safe to call more than once. */
    public static void preload(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> get(app), "hanja-preload").start();
    }

    private static HanjaTable load(Context context) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("hanja.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException failure) {
            return HanjaTable.parse(Collections.emptyList());
        }
        return HanjaTable.parse(lines);
    }
}
