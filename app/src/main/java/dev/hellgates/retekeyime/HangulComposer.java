package dev.hellgates.retekeyime;

/**
 * A stateful 2-beolsik Hangul composition automaton, ported from the Jamotong FSM (revision
 * 90d6eb5, MIT; see THIRD_PARTY_NOTICES.md). It accumulates a choseong/jungseong/jongseong syllable
 * and, per input, reports the text to commit and the text still composing.
 *
 * <p>Platform-neutral and single-syllable: it holds one composing syllable at a time and reports
 * exactly what to commit when a syllable closes. The output model (a {@link Result} of commit +
 * preedit) and reversible backspace are ReteKey's; the transition rules and tables are Jamotong's.
 */
public final class HangulComposer {
    /** The commit text (may be empty) and the still-composing text (empty means nothing composing). */
    public static final class Result {
        private final String commit;
        private final String preedit;

        Result(String commit, String preedit) {
            this.commit = commit;
            this.preedit = preedit;
        }

        public String commit() {
            return commit;
        }

        public String preedit() {
            return preedit;
        }

        public boolean isComposing() {
            return !preedit.isEmpty();
        }
    }

    private enum State {
        EMPTY,
        CHO,
        JUNG,
        CHO_JUNG,
        CHO_JUNG_JONG
    }

    private static final Result NONE = new Result("", "");

    private State state = State.EMPTY;
    private int cho = -1;
    private int jung = -1;
    private int jong = -1;

    public boolean isComposing() {
        return state != State.EMPTY;
    }

    public void reset() {
        state = State.EMPTY;
        cho = -1;
        jung = -1;
        jong = -1;
    }

    /** Feeds one jamo. CONTEXTUAL_CONSONANT is a choseong index, VOWEL is a jungseong index. */
    public Result input(SemanticJamo jamo) {
        if (jamo == null) {
            throw new IllegalArgumentException("jamo must not be null");
        }
        switch (jamo.role()) {
            case CONTEXTUAL_CONSONANT:
            case DIRECT_INITIAL:
                return consonant(jamo.index());
            case VOWEL:
            case DIRECT_MEDIAL:
                return vowel(jamo.index());
            default:
                throw new IllegalArgumentException("2-beolsik composer takes cho/jung jamo only");
        }
    }

    private Result consonant(int c) {
        switch (state) {
            case EMPTY:
                cho = c;
                state = State.CHO;
                return preedit(HangulTables.choJamo(cho));
            case CHO: {
                // No 2-beolsik doubling by repeat; commit the old choseong and start a new one.
                String commit = HangulTables.choJamo(cho);
                cho = c;
                return new Result(commit, HangulTables.choJamo(cho));
            }
            case JUNG: {
                String commit = HangulTables.jungJamo(jung);
                jung = -1;
                cho = c;
                state = State.CHO;
                return new Result(commit, HangulTables.choJamo(cho));
            }
            case CHO_JUNG: {
                int asJong = HangulTables.choToJong(c);
                if (asJong != -1) {
                    // 도깨비불 setup: attach as a tentative batchim, commit nothing yet.
                    jong = asJong;
                    state = State.CHO_JUNG_JONG;
                    return preedit(syllable());
                }
                String commit = syllable();
                cho = c;
                jung = -1;
                state = State.CHO;
                return new Result(commit, HangulTables.choJamo(cho));
            }
            case CHO_JUNG_JONG: {
                int combined = HangulTables.combineJong(jong, c);
                if (combined != -1) {
                    jong = combined;
                    return preedit(syllable());
                }
                String commit = syllable();
                cho = c;
                jung = -1;
                jong = -1;
                state = State.CHO;
                return new Result(commit, HangulTables.choJamo(cho));
            }
            default:
                throw new IllegalStateException("unreachable state");
        }
    }

    private Result vowel(int v) {
        switch (state) {
            case EMPTY:
                jung = v;
                state = State.JUNG;
                return preedit(HangulTables.jungJamo(jung));
            case CHO:
                jung = v;
                state = State.CHO_JUNG;
                return preedit(syllable());
            case JUNG: {
                int combined = HangulTables.combineJung(jung, v);
                if (combined != -1) {
                    jung = combined;
                    return preedit(HangulTables.jungJamo(jung));
                }
                String commit = HangulTables.jungJamo(jung);
                jung = v;
                return new Result(commit, HangulTables.jungJamo(jung));
            }
            case CHO_JUNG: {
                int combined = HangulTables.combineJung(jung, v);
                if (combined != -1) {
                    jung = combined;
                    return preedit(syllable());
                }
                String commit = String.valueOf(HangulTables.compose(cho, jung, -1));
                cho = -1;
                jung = v;
                state = State.JUNG;
                return new Result(commit, HangulTables.jungJamo(jung));
            }
            case CHO_JUNG_JONG: {
                // 받침 이동: the batchim (or its tail) moves to the new syllable's choseong.
                int[] split = HangulTables.splitJong(jong);
                int stays = split[0];
                int moves = split[1];
                String commit = String.valueOf(
                    HangulTables.compose(cho, jung, stays > 0 ? stays : -1)
                );
                cho = moves;
                jung = v;
                jong = -1;
                state = State.CHO_JUNG;
                return new Result(commit, syllable());
            }
            default:
                throw new IllegalStateException("unreachable state");
        }
    }

    /**
     * Removes the last jamo, decomposing compounds rather than dropping them whole (ReteKey's
     * reversible behavior). Returns the new preedit, or null when there was nothing to compose (the
     * caller should let the editor handle the backspace).
     */
    public Result backspace() {
        switch (state) {
            case EMPTY:
                return null;
            case CHO:
            case JUNG:
                reset();
                return NONE;
            case CHO_JUNG: {
                int[] parts = HangulTables.splitJung(jung);
                if (parts != null) {
                    jung = parts[0];
                    return preedit(syllable());
                }
                jung = -1;
                state = State.CHO;
                return preedit(HangulTables.choJamo(cho));
            }
            case CHO_JUNG_JONG: {
                int[] parts = HangulTables.splitJong(jong);
                // A compound final drops its moved consonant and keeps the part that stays.
                if (parts[0] > 0) {
                    jong = parts[0];
                    return preedit(syllable());
                }
                jong = -1;
                state = State.CHO_JUNG;
                return preedit(syllable());
            }
            default:
                throw new IllegalStateException("unreachable state");
        }
    }

    /** Commits whatever is composing and resets. Returns the committed text (may be empty). */
    public String flush() {
        String text;
        switch (state) {
            case EMPTY:
                return "";
            case CHO:
                text = HangulTables.choJamo(cho);
                break;
            case JUNG:
                text = HangulTables.jungJamo(jung);
                break;
            default:
                text = syllable();
                break;
        }
        reset();
        return text;
    }

    private String syllable() {
        return String.valueOf(HangulTables.compose(cho, jung, jong < 0 ? -1 : jong));
    }

    private static Result preedit(String preedit) {
        return new Result("", preedit);
    }
}
