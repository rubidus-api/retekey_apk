package com.retekey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The guide that tells somebody else how to write a layout (issue #11 asked for it) is only worth
 * having while every example in it is a layout this app reads. So the examples are run, not
 * proofread: each fenced {@code rkl} block in {@code docs/user-layouts.md} is parsed here, and the
 * Korean guide has to carry the same blocks byte for byte — a translation that quietly edits an
 * example teaches a format the parser does not accept.
 */
public final class UserLayoutDocExamplesTest {
    private static final String FENCE = "```rkl";

    @Test
    public void everyExampleInTheGuideIsALayoutTheAppReads() throws IOException {
        List<String> examples = examplesIn("docs/user-layouts.md");
        assertFalse("the guide has no examples at all", examples.isEmpty());
        for (String example : examples) {
            UserLayout layout = UserLayout.parse(example);
            assertNotNull("this example does not parse:\n" + example, layout);
            assertEquals("a layout has three rows", UserLayout.ROWS, layout.rows().size());
            assertEquals("the cap is three characters", 3, layout.cap().length());
        }
    }

    @Test
    public void theTranslationShowsTheSameExamples() throws IOException {
        assertEquals("the two guides must show the same layout files",
            examplesIn("docs/user-layouts.md"), examplesIn("docs/user-layouts.ko.md"));
    }

    @Test
    public void theGuideAndTheSettingsScreenAgree() throws IOException {
        // The first example in the guide is the one the settings screen shows, so a reader who
        // starts on the phone and a reader who starts on the web are given the same file.
        assertEquals(UserLayout.EXAMPLE, examplesIn("docs/user-layouts.md").get(0));
    }

    /** Every fenced rkl block in a documentation file, in the order it appears. */
    private static List<String> examplesIn(String path) throws IOException {
        // Unit tests run with the app module as the working directory; the docs are the project's.
        File file = new File("../" + path);
        assertTrue(file.getPath() + " is missing", file.isFile());
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        List<String> examples = new ArrayList<>();
        int from = 0;
        while (true) {
            int start = text.indexOf(FENCE, from);
            if (start < 0) {
                return examples;
            }
            int body = text.indexOf('\n', start) + 1;
            int end = text.indexOf("```", body);
            assertTrue("an unclosed code fence in " + path, end > 0);
            examples.add(text.substring(body, end).trim());
            from = end + 3;
        }
    }
}
