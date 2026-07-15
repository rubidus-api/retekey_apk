package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lowers semantic input to editor actions through the stateful {@link HangulComposer}. A jamo
 * updates the composing syllable and commits any syllable that closes; other input flushes the
 * composition first so nothing is lost. Its state is the composer, reset at each session boundary
 * by the service.
 */
public final class HangulInputProcessor implements StatelessInputProcessor {
    private final HangulComposer composer = new HangulComposer();
    private final Supplier<EditorProfile> editorProfile;

    public HangulInputProcessor(Supplier<EditorProfile> editorProfile) {
        this.editorProfile = Objects.requireNonNull(editorProfile, "editorProfile");
    }

    /** Clears the composing syllable at a session boundary. */
    public void reset() {
        composer.reset();
    }

    public boolean isComposing() {
        return composer.isComposing();
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
                return flushThen(KeyAction.commitText(input.text()));
            case DELETE_BACKWARD:
                return delete();
            case FLUSH:
                return flushOnly();
            case PRIMARY_ACTION:
                return primaryAction();
            case RAW_KEY:
                return flushThen(KeyAction.rawKey(input.rawKey(), input.modifiers()));
            default:
                throw new IllegalStateException("unsupported semantic input: " + input.kind());
        }
    }

    private DispatchResult jamo(SemanticJamo jamo) {
        HangulComposer.Result result = composer.input(jamo);
        List<KeyAction> actions = new ArrayList<>(2);
        if (!result.commit().isEmpty()) {
            actions.add(KeyAction.commitText(result.commit()));
        }
        // A jamo always leaves something composing.
        actions.add(KeyAction.setComposingText(result.preedit()));
        return DispatchResult.handled(actions);
    }

    private DispatchResult delete() {
        HangulComposer.Result result = composer.backspace();
        if (result == null) {
            // Nothing composing: let the editor delete a real character.
            return DispatchResult.handled(KeyAction.deleteBackward());
        }
        // The composer decomposed a jamo; show the shorter composition (empty clears it).
        return DispatchResult.handled(KeyAction.setComposingText(result.preedit()));
    }

    private DispatchResult flushOnly() {
        String flushed = composer.flush();
        return flushed.isEmpty()
            ? DispatchResult.handled()
            : DispatchResult.handled(KeyAction.commitText(flushed));
    }

    private DispatchResult flushThen(KeyAction trailing) {
        String flushed = composer.flush();
        if (flushed.isEmpty()) {
            return DispatchResult.handled(trailing);
        }
        return DispatchResult.handled(KeyAction.commitText(flushed), trailing);
    }

    private DispatchResult primaryAction() {
        String flushed = composer.flush();
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
