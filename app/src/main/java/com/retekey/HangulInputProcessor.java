package com.retekey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lowers semantic input to editor actions through the stateful {@link HangulComposer}. A jamo
 * updates the composing syllable and commits any syllable that closes; other input flushes the
 * composition first so nothing is lost. Its state is the composer, reset at each session boundary
 * by the service.
 */
public final class HangulInputProcessor implements StatelessInputProcessor {
    private final HangulComposer composer = new HangulComposer();
    /** A Latin-script composer (Vietnamese Telex) for the current layout, or null for none. */
    private TelexComposer latin;
    private final Fn.Supplier<EditorProfile> editorProfile;
    private final Fn.Supplier<CharSequence> textBeforeCursor;

    public HangulInputProcessor(Fn.Supplier<EditorProfile> editorProfile) {
        this(editorProfile, () -> null);
    }

    /**
     * @param textBeforeCursor answers the character before the cursor (or null when the editor
     *     cannot say), read only when a 나랏글 transformation arrives with nothing composing.
     */
    public HangulInputProcessor(
        Fn.Supplier<EditorProfile> editorProfile,
        Fn.Supplier<CharSequence> textBeforeCursor
    ) {
        this.editorProfile = Objects.requireNonNull(editorProfile, "editorProfile");
        this.textBeforeCursor = Objects.requireNonNull(textBeforeCursor, "textBeforeCursor");
    }

    /** Clears the composing syllable at a session boundary. */
    public void reset() {
        composer.reset();
        if (latin != null) {
            latin.reset();
        }
    }

    /**
     * Gives the letter keys to a Latin composer — Vietnamese Telex — or takes them back with null.
     * Switching flushes nothing by itself; the caller commits what was composing first.
     */
    public void setLatinComposer(TelexComposer composer) {
        this.latin = composer;
    }

    public TelexComposer latinComposer() {
        return latin;
    }

    public boolean isComposing() {
        return composer.isComposing() || (latin != null && latin.isComposing());
    }

    /** The text currently composing, for comparing against what sits before the editor's cursor. */
    public String composingText() {
        if (latin != null && latin.isComposing()) {
            return latin.preeditText();
        }
        return composer.preeditText();
    }

    @Override
    public DispatchResult process(SemanticInput input) {
        if (input == null) {
            throw new IllegalArgumentException("semantic input must not be null");
        }
        switch (input.kind()) {
            case JAMO:
                return jamo(input.jamo());
            case TEXT:
                if (latin != null && latin.accepts(input.text())) {
                    return latinLetter(input.text());
                }
                return flushThen(KeyAction.commitText(input.text()));
            case DELETE_BACKWARD:
                return delete(input.isCorrection());
            case FLUSH:
                return flushOnly();
            case PRIMARY_ACTION:
                return primaryAction();
            case RAW_KEY:
                return flushThen(
                    KeyAction.rawKey(input.rawKey(), input.modifiers(), input.rawKeyPhase()));
            case TRANSFORM:
                return transform(input.transform());
            default:
                throw new IllegalStateException("unsupported semantic input: " + input.kind());
        }
    }

    private DispatchResult jamo(SemanticJamo jamo) {
        List<KeyAction> actions = new ArrayList<>(3);
        if (latin != null && latin.isComposing()) {
            actions.add(KeyAction.commitText(latin.flush()));
        }
        HangulComposer.Result result = composer.input(jamo);
        if (!result.commit().isEmpty()) {
            actions.add(KeyAction.commitText(result.commit()));
        }
        // A jamo always leaves something composing.
        actions.add(KeyAction.setComposingText(result.preedit()));
        return DispatchResult.handled(actions);
    }

    /** A letter for the Latin composer: the Hangul syllable, if any, commits first. */
    private DispatchResult latinLetter(String text) {
        List<KeyAction> actions = new ArrayList<>(2);
        String hangul = composer.flush();
        if (!hangul.isEmpty()) {
            actions.add(KeyAction.commitText(hangul));
        }
        TelexComposer.Result result = latin.input(text);
        actions.add(KeyAction.setComposingText(result.preedit));
        return DispatchResult.handled(actions);
    }

    private DispatchResult delete(boolean correction) {
        if (latin != null && latin.isComposing()) {
            TelexComposer.Result result = latin.backspace();
            return DispatchResult.handled(
                KeyAction.setComposingText(result == null ? "" : result.preedit));
        }
        if (correction) {
            HangulComposer.Result reopened = composer.reopenClosedSyllable();
            if (reopened != null) {
                // Drop the consonant being corrected, take back the syllable it closed, and put
                // that syllable back into composition so the replacement can join it.
                return DispatchResult.handled(
                    KeyAction.setComposingText(""),
                    KeyAction.deleteBackward(),
                    KeyAction.setComposingText(reopened.preedit())
                );
            }
        }
        HangulComposer.Result result = composer.backspace();
        if (result == null) {
            // Nothing composing: let the editor delete a real character.
            return DispatchResult.handled(KeyAction.deleteBackward());
        }
        // The composer decomposed a jamo; show the shorter composition (empty clears it).
        return DispatchResult.handled(KeyAction.setComposingText(result.preedit()));
    }

    /**
     * A 나랏글 transformation whose interpreter has lost its run — the letter to act on is found
     * on screen instead. Tier one: the composing syllable's trailing jamo. Tier two, when nothing
     * is composing: the character before the cursor, which is replaced and re-opened as a
     * composing syllable so typing continues as if the run had never broken. When neither names a
     * transformable letter the press does nothing, exactly like the interpreter's own no-answer.
     */
    private DispatchResult transform(SemanticInput.Transform kind) {
        int consonant = composer.trailingConsonant();
        if (consonant >= 0) {
            int next = apply(kind, consonant);
            if (next < 0) {
                return DispatchResult.handled();
            }
            return concat(
                delete(true),
                jamo(SemanticJamo.contextualConsonant(next))
            );
        }
        int vowel = composer.trailingVowel();
        if (vowel >= 0) {
            if (kind != SemanticInput.Transform.STROKE) {
                return DispatchResult.handled();
            }
            int next = NaratgeulTransforms.vowelStrokeOf(vowel);
            if (next < 0) {
                return DispatchResult.handled();
            }
            return concat(delete(true), jamo(SemanticJamo.vowel(next)));
        }
        if (composer.isComposing()) {
            return DispatchResult.handled();
        }
        return coldTransform(kind);
    }

    /** Nothing is composing: transform the character before the cursor, and re-open it. */
    private DispatchResult coldTransform(SemanticInput.Transform kind) {
        CharSequence before = textBeforeCursor.get();
        if (before == null || before.length() == 0) {
            return DispatchResult.handled();
        }
        char last = before.charAt(before.length() - 1);

        // A bare consonant jamo: ㄱ becomes ㅋ, and is left composing.
        int bareCho = choIndexOfJamo(last);
        if (bareCho >= 0) {
            int next = apply(kind, bareCho);
            if (next < 0) {
                return DispatchResult.handled();
            }
            HangulComposer.Result reopened = composer.seed(next, -1, -1);
            return DispatchResult.handled(
                KeyAction.deleteBackward(),
                KeyAction.setComposingText(reopened.preedit())
            );
        }

        // A bare vowel jamo: ㅏ becomes ㅑ under 획추가; 쌍자음 has nothing to say to it.
        if (last >= 0x314F && last < 0x314F + HangulTables.JUNG_COUNT) {
            if (kind != SemanticInput.Transform.STROKE) {
                return DispatchResult.handled();
            }
            int next = NaratgeulTransforms.vowelStrokeOf(last - 0x314F);
            if (next < 0) {
                return DispatchResult.handled();
            }
            HangulComposer.Result reopened = composer.seed(-1, next, -1);
            return DispatchResult.handled(
                KeyAction.deleteBackward(),
                KeyAction.setComposingText(reopened.preedit())
            );
        }

        if (last < HangulTables.HANGUL_BASE || last >= HangulTables.HANGUL_BASE
            + HangulTables.CHO_COUNT * HangulTables.JUNG_COUNT * HangulTables.JONG_COUNT) {
            return DispatchResult.handled();
        }
        int offset = last - HangulTables.HANGUL_BASE;
        int cho = offset / (HangulTables.JUNG_COUNT * HangulTables.JONG_COUNT);
        int jung = (offset / HangulTables.JONG_COUNT) % HangulTables.JUNG_COUNT;
        int jong = offset % HangulTables.JONG_COUNT;

        if (jong > 0) {
            // The last letter is the batchim (a compound final's tail): transform it in place.
            int[] split = HangulTables.splitJong(jong);
            int next = apply(kind, split[1]);
            if (next < 0) {
                return DispatchResult.handled();
            }
            int stays = split[0];
            int asJong = stays > 0
                ? HangulTables.combineJong(stays, next)
                : HangulTables.choToJong(next);
            if (asJong > 0) {
                HangulComposer.Result reopened = composer.seed(cho, jung, asJong);
                return DispatchResult.handled(
                    KeyAction.deleteBackward(),
                    KeyAction.setComposingText(reopened.preedit())
                );
            }
            // The transformed letter cannot be a batchim (ㄸ ㅃ ㅉ, or no compound): the syllable
            // keeps what stays, and the new letter starts the next one — as it does live.
            String base = String.valueOf(
                HangulTables.compose(cho, jung, stays > 0 ? stays : -1)
            );
            HangulComposer.Result reopened = composer.seed(next, -1, -1);
            return DispatchResult.handled(
                KeyAction.deleteBackward(),
                KeyAction.commitText(base),
                KeyAction.setComposingText(reopened.preedit())
            );
        }

        // No batchim: the last letter is the vowel, and only 획추가 speaks to a vowel.
        if (kind != SemanticInput.Transform.STROKE) {
            return DispatchResult.handled();
        }
        int next = NaratgeulTransforms.vowelStrokeOf(jung);
        if (next < 0) {
            return DispatchResult.handled();
        }
        HangulComposer.Result reopened = composer.seed(cho, next, -1);
        return DispatchResult.handled(
            KeyAction.deleteBackward(),
            KeyAction.setComposingText(reopened.preedit())
        );
    }

    private static int apply(SemanticInput.Transform kind, int cho) {
        return kind == SemanticInput.Transform.STROKE
            ? NaratgeulTransforms.strokeOf(cho)
            : NaratgeulTransforms.twinOf(cho);
    }

    /** The choseong index of a bare consonant compatibility jamo, or -1. */
    private static int choIndexOfJamo(char c) {
        for (int i = 0; i < HangulTables.CHO_COUNT; i++) {
            if (HangulTables.choJamo(i).charAt(0) == c) {
                return i;
            }
        }
        return -1;
    }

    private static DispatchResult concat(DispatchResult first, DispatchResult second) {
        List<KeyAction> actions = new ArrayList<>(first.actions().size() + second.actions().size());
        actions.addAll(first.actions());
        actions.addAll(second.actions());
        return DispatchResult.handled(actions);
    }

    /** Everything composing, in either script, as one committed string. */
    private String flushAll() {
        String hangul = composer.flush();
        String word = latin == null ? "" : latin.flush();
        return hangul + word;
    }

    private DispatchResult flushOnly() {
        String flushed = flushAll();
        return flushed.isEmpty()
            ? DispatchResult.handled()
            : DispatchResult.handled(KeyAction.commitText(flushed));
    }

    private DispatchResult flushThen(KeyAction trailing) {
        String flushed = flushAll();
        if (flushed.isEmpty()) {
            return DispatchResult.handled(trailing);
        }
        return DispatchResult.handled(KeyAction.commitText(flushed), trailing);
    }

    private DispatchResult primaryAction() {
        String flushed = flushAll();
        DispatchResult enter = EditorActionPolicy.enter(
            Objects.requireNonNull(editorProfile.get(), "editor profile")
        );
        if (flushed.isEmpty()) {
            return enter;
        }
        List<KeyAction> actions = new ArrayList<>(enter.actions().size() + 1);
        actions.add(KeyAction.commitText(flushed));
        actions.addAll(enter.actions());
        return enter.isHandled()
            ? DispatchResult.handled(actions)
            : DispatchResult.delegate(actions);
    }
}
