package dev.hellgates.retekeyime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * The punctuation keys on 천지인's bottom row work the way the Hangul keys around them do: the
 * label holds every character, a tap moves through them, and a drag picks one outright.
 */
public final class MultiTapCycleTest {
    @Test
    public void theLabelIsTheListOfCharacters() {
        assertEquals(java.util.Arrays.asList(".", ","), MultiTapCycle.charactersOf(".,"));
        assertEquals(java.util.Arrays.asList("!", "?"), MultiTapCycle.charactersOf("!?"));
    }

    @Test
    public void aLabelOutsideTheBasicPlaneIsStillOneCharacter() {
        assertEquals(1, MultiTapCycle.charactersOf("🌐").size());
    }

    @Test
    public void theFirstTapTypesTheFirstCharacterOutright() {
        List<String> characters = MultiTapCycle.charactersOf(".,");
        MultiTapCycle.Step step = MultiTapCycle.press(characters, -1, false);

        assertEquals(".", step.character);
        assertEquals(0, step.index);
        assertFalse("there is nothing on screen yet to take back", step.replacesPrevious);
    }

    @Test
    public void aTapInsideTheRunReplacesWhatItTyped() {
        List<String> characters = MultiTapCycle.charactersOf(".,");
        MultiTapCycle.Step step = MultiTapCycle.press(characters, 0, true);

        assertEquals(",", step.character);
        assertEquals(1, step.index);
        assertTrue(step.replacesPrevious);
    }

    @Test
    public void aTwoCharacterKeyFlipsBackAndForth() {
        List<String> characters = MultiTapCycle.charactersOf("!?");
        assertEquals("!", MultiTapCycle.press(characters, 1, true).character);
        assertEquals("?", MultiTapCycle.press(characters, 0, true).character);
    }

    @Test
    public void aRunThatHasEndedStartsTheLabelAgain() {
        List<String> characters = MultiTapCycle.charactersOf(".,");
        MultiTapCycle.Step step = MultiTapCycle.press(characters, 1, false);

        assertEquals("a pause means the next tap is a fresh period", ".", step.character);
        assertFalse(step.replacesPrevious);
    }

    @Test
    public void draggingPicksTheCharacterWrittenOnThatSide() {
        List<String> characters = MultiTapCycle.charactersOf(".,");
        assertEquals(".", MultiTapCycle.pick(characters, false).character);
        assertEquals(",", MultiTapCycle.pick(characters, true).character);
    }

    @Test
    public void aPickNeverTakesBackWhatCameBefore() {
        // A drag ends the run rather than continuing it, so there is nothing of its own to undo.
        assertFalse(MultiTapCycle.pick(MultiTapCycle.charactersOf("!?"), true).replacesPrevious);
    }

    @Test(expected = IllegalArgumentException.class)
    public void anEmptyLabelIsRejected() {
        MultiTapCycle.charactersOf("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void continuingFromACharacterThatIsNotThereIsRejected() {
        MultiTapCycle.press(MultiTapCycle.charactersOf(".,"), 5, true);
    }
}
