package dev.hellgates.retekeyime;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * No-loss scaffold behavior used until the stateful Hangul composer lands.
 */
public final class ScaffoldInputProcessor implements StatelessInputProcessor {
    private static final String[] INITIALS = {
        "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
        "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };
    private static final String[] FINALS = {
        "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ",
        "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ",
        "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };
    private static final int VOWEL_BASE = 0x314f;

    private final Supplier<EditorProfile> editorProfile;

    public ScaffoldInputProcessor() {
        this(EditorProfile::unsupported);
    }

    public ScaffoldInputProcessor(Supplier<EditorProfile> editorProfile) {
        this.editorProfile = Objects.requireNonNull(editorProfile, "editorProfile");
    }

    @Override
    public DispatchResult process(SemanticInput input) {
        if (input == null) {
            throw new IllegalArgumentException("semantic input must not be null");
        }

        switch (input.kind()) {
            case TEXT:
                return DispatchResult.handled(KeyAction.commitText(input.text()));
            case JAMO:
                return DispatchResult.handled(KeyAction.commitText(
                    compatibilityJamo(input.jamo())
                ));
            case DELETE_BACKWARD:
                return DispatchResult.handled(KeyAction.deleteBackward());
            case FLUSH:
                return DispatchResult.handled();
            case PRIMARY_ACTION:
                return EditorActionPolicy.enter(
                    Objects.requireNonNull(editorProfile.get(), "editor profile")
                );
            default:
                throw new IllegalStateException("unsupported semantic input: " + input.kind());
        }
    }

    private static String compatibilityJamo(SemanticJamo jamo) {
        switch (jamo.role()) {
            case CONTEXTUAL_CONSONANT:
            case DIRECT_INITIAL:
                return INITIALS[jamo.index()];
            case VOWEL:
            case DIRECT_MEDIAL:
                return new String(Character.toChars(VOWEL_BASE + jamo.index()));
            case DIRECT_FINAL:
                return FINALS[jamo.index() - 1];
            default:
                throw new IllegalStateException("unsupported jamo role: " + jamo.role());
        }
    }
}
