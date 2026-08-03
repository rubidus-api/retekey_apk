package com.retekey;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The shipped indexes are generated from the curated tables, and generated data goes stale in
 * silence. This rebuilds what the generator should have produced and asserts the assets match it
 * line for line — so an edit to a source table that never reached the index fails here rather than
 * in someone's typing.
 */
public final class HanjaIndexBuildTest {
    private static Path file(String... candidates) {
        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        throw new AssertionError("not found from " + Paths.get("").toAbsolutePath());
    }

    private static List<String> read(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static List<String> source(String name) {
        return read(file("src/test/resources/hanja/" + name, "app/src/test/resources/hanja/" + name));
    }

    private static List<String> asset(String name) {
        return read(file("src/main/assets/" + name, "app/src/main/assets/" + name));
    }

    /** The data lines of an index, without its notice header or directives. */
    private static List<String> entries(List<String> lines) {
        List<String> data = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                data.add(line);
            }
        }
        return data;
    }

    private static List<String> sorted(Map<String, List<String>> table) {
        List<String> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : table.entrySet()) {
            rows.add(entry.getKey() + "\t" + String.join(",", entry.getValue()));
        }
        rows.sort(Comparator.comparing(row -> {
            String key = row.substring(0, row.indexOf('\t'));
            return new String(key.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }));
        return rows;
    }

    /** Reproduces HanjaTable.parse's grouping and ordering, which the indexes must preserve. */
    private static void build(Map<String, List<String>> forward, Map<String, List<String>> reverse) {
        for (String line : source("hanja.txt")) {
            String text = line.trim();
            if (text.isEmpty() || text.charAt(0) == '#') {
                continue;
            }
            int colon = text.indexOf(':');
            if (colon <= 0 || colon == text.length() - 1) {
                continue;
            }
            List<String> candidates = new ArrayList<>();
            for (String raw : text.substring(colon + 1).split(",")) {
                String candidate = raw.trim();
                if (!candidate.isEmpty() && !candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
            for (String rawKey : text.substring(0, colon).split(",")) {
                String key = rawKey.trim();
                if (key.isEmpty()) {
                    continue;
                }
                List<String> existing = forward.computeIfAbsent(key, ignored -> new ArrayList<>());
                for (String candidate : candidates) {
                    if (!existing.contains(candidate)) {
                        existing.add(candidate);
                    }
                    List<String> readings =
                        reverse.computeIfAbsent(candidate, ignored -> new ArrayList<>());
                    if (!readings.contains(key)) {
                        readings.add(key);
                    }
                }
            }
        }
    }

    @Test
    public void theShippedIndexesAreWhatTheCuratedTablesImply() {
        Map<String, List<String>> forward = new LinkedHashMap<>();
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        build(forward, reverse);

        assertEquals(sorted(forward), entries(asset("hanja_fwd.idx")));
        assertEquals(sorted(reverse), entries(asset("hanja_rev.idx")));

        Map<String, List<String>> glosses = new LinkedHashMap<>();
        for (String line : source("hanja_hunum.txt")) {
            int colon = line.indexOf(':');
            if (line.startsWith("#") || colon <= 0) {
                continue;
            }
            glosses.putIfAbsent(
                line.substring(0, colon),
                new ArrayList<>(List.of(line.substring(colon + 1).trim())));
        }
        assertEquals(sorted(glosses), entries(asset("hanja_hunum.idx")));
    }

    @Test
    public void everyIndexKeepsItsNoticeAndDeclaresItsLongestKey() {
        for (String name : new String[]{"hanja_fwd.idx", "hanja_rev.idx", "hanja_hunum.idx"}) {
            List<String> lines = asset(name);
            assertEquals(name + " opens with its notice", true, lines.get(0).startsWith("#"));
            String directive = null;
            int longest = 1;
            for (String line : lines) {
                if (line.startsWith("#!maxkey ")) {
                    directive = line.substring(9).trim();
                } else if (!line.startsWith("#") && line.indexOf('\t') > 0) {
                    longest = Math.max(longest, line.substring(0, line.indexOf('\t')).length());
                }
            }
            assertEquals(name + " declares its longest key", String.valueOf(longest), directive);
        }
    }
}
