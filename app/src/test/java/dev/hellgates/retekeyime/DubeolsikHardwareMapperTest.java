package dev.hellgates.retekeyime;

import org.junit.Assert;
import org.junit.Test;

public final class DubeolsikHardwareMapperTest {
    @Test
    public void coversEveryUnshiftedStandardLetterKey() {
        String[] keys = {
            "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m"
        };
        SemanticJamo[] jamo = {
            SemanticJamo.contextualConsonant(7),
            SemanticJamo.contextualConsonant(12),
            SemanticJamo.contextualConsonant(3),
            SemanticJamo.contextualConsonant(0),
            SemanticJamo.contextualConsonant(9),
            SemanticJamo.vowel(12),
            SemanticJamo.vowel(6),
            SemanticJamo.vowel(2),
            SemanticJamo.vowel(1),
            SemanticJamo.vowel(5),
            SemanticJamo.contextualConsonant(6),
            SemanticJamo.contextualConsonant(2),
            SemanticJamo.contextualConsonant(11),
            SemanticJamo.contextualConsonant(5),
            SemanticJamo.contextualConsonant(18),
            SemanticJamo.vowel(8),
            SemanticJamo.vowel(4),
            SemanticJamo.vowel(0),
            SemanticJamo.vowel(20),
            SemanticJamo.contextualConsonant(15),
            SemanticJamo.contextualConsonant(16),
            SemanticJamo.contextualConsonant(14),
            SemanticJamo.contextualConsonant(17),
            SemanticJamo.vowel(17),
            SemanticJamo.vowel(13),
            SemanticJamo.vowel(18)
        };

        for (int i = 0; i < keys.length; i++) {
            Assert.assertEquals(
                keys[i],
                SemanticInput.jamo(jamo[i]),
                DubeolsikHardwareMapper.INSTANCE.map("hardware.key." + keys[i], false)
            );
        }
    }

    @Test
    public void mapsStandardPhysicalKeysToStructuredJamo() {
        DubeolsikHardwareMapper mapper = DubeolsikHardwareMapper.INSTANCE;

        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(0)),
            mapper.map("hardware.key.r", false)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(1)),
            mapper.map("hardware.key.r", true)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(8)),
            mapper.map("hardware.key.q", true)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(13)),
            mapper.map("hardware.key.w", true)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(4)),
            mapper.map("hardware.key.e", true)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.contextualConsonant(10)),
            mapper.map("hardware.key.t", true)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.vowel(0)),
            mapper.map("hardware.key.k", false)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.vowel(3)),
            mapper.map("hardware.key.o", true)
        );
        Assert.assertEquals(
            SemanticInput.jamo(SemanticJamo.vowel(7)),
            mapper.map("hardware.key.p", true)
        );
    }

    @Test
    public void leavesNonDubeolsikKeysUnmapped() {
        DubeolsikHardwareMapper mapper = DubeolsikHardwareMapper.INSTANCE;

        Assert.assertNull(mapper.map("hardware.key.space", false));
        Assert.assertNull(mapper.map("hardware.key.unknown", true));
        Assert.assertNull(mapper.map(null, false));
    }
}
