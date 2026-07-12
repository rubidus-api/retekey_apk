package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ScaffoldKeyboardLayout {
    private static final List<List<SoftwareKeySpec>> ROWS = createRows();

    private ScaffoldKeyboardLayout() {
    }

    public static List<List<SoftwareKeySpec>> rows() {
        return ROWS;
    }

    public static SoftwareKeySpec findById(String stableKeyId) {
        if (stableKeyId == null) {
            return null;
        }
        for (List<SoftwareKeySpec> row : ROWS) {
            for (SoftwareKeySpec key : row) {
                if (stableKeyId.equals(key.stableKeyId())) {
                    return key;
                }
            }
        }
        return null;
    }

    private static List<List<SoftwareKeySpec>> createRows() {
        List<List<SoftwareKeySpec>> rows = new ArrayList<>();
        rows.add(row(
            consonant("bieup", "ㅂ", 7),
            consonant("jieut", "ㅈ", 12),
            consonant("digeut", "ㄷ", 3),
            consonant("giyeok", "ㄱ", 0),
            consonant("siot", "ㅅ", 9),
            vowel("yo", "ㅛ", 12),
            vowel("yeo", "ㅕ", 6),
            vowel("ya", "ㅑ", 2),
            vowel("ae", "ㅐ", 1),
            vowel("e", "ㅔ", 5)
        ));
        rows.add(row(
            consonant("mieum", "ㅁ", 6),
            consonant("nieun", "ㄴ", 2),
            consonant("ieung", "ㅇ", 11),
            consonant("rieul", "ㄹ", 5),
            consonant("hieuh", "ㅎ", 18),
            vowel("o", "ㅗ", 8),
            vowel("eo", "ㅓ", 4),
            vowel("a", "ㅏ", 0),
            vowel("i", "ㅣ", 20)
        ));
        rows.add(row(
            consonant("kieuk", "ㅋ", 15),
            consonant("tieut", "ㅌ", 16),
            consonant("chieut", "ㅊ", 14),
            consonant("pieup", "ㅍ", 17),
            vowel("yu", "ㅠ", 17),
            vowel("u", "ㅜ", 13),
            vowel("eu", "ㅡ", 18),
            SoftwareKeySpec.enabled(
                "touch.edit.backspace",
                "⌫",
                SemanticInput.deleteBackward()
            )
        ));
        rows.add(row(
            SoftwareKeySpec.disabled("touch.modifier.ctrl", "CTRL"),
            SoftwareKeySpec.disabled("touch.modifier.alt", "ALT"),
            SoftwareKeySpec.disabled("touch.layout.toggle", "한/영"),
            SoftwareKeySpec.disabled("touch.navigation.left", "←"),
            SoftwareKeySpec.disabled("touch.navigation.down", "↓"),
            SoftwareKeySpec.disabled("touch.navigation.up", "↑"),
            SoftwareKeySpec.disabled("touch.navigation.right", "→")
        ));
        return Collections.unmodifiableList(rows);
    }

    private static List<SoftwareKeySpec> row(SoftwareKeySpec... keys) {
        return Collections.unmodifiableList(Arrays.asList(keys.clone()));
    }

    private static SoftwareKeySpec consonant(String id, String label, int index) {
        return SoftwareKeySpec.enabled(
            "touch.ko2." + id,
            label,
            SemanticInput.jamo(SemanticJamo.contextualConsonant(index))
        );
    }

    private static SoftwareKeySpec vowel(String id, String label, int index) {
        return SoftwareKeySpec.enabled(
            "touch.ko2." + id,
            label,
            SemanticInput.jamo(SemanticJamo.vowel(index))
        );
    }
}
