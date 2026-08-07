package com.retekey;

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
    /**
     * The syllable the last consonant closed and pushed out, kept for exactly one input. A 12-key
     * automaton types a consonant and only then learns what it becomes: ㅇ, then 획추가, is ㅎ. By
     * the time the stroke arrives the ㅇ has already started a new syllable and 만 is committed, so
     * without this the ㄶ of 많 can never be assembled. Null whenever there is nothing to re-open.
     */
    private int[] closedSyllable;

    public boolean isComposing() {
        return state != State.EMPTY;
    }

    public void reset() {
        state = State.EMPTY;
        cho = -1;
        jung = -1;
        jong = -1;
        closedSyllable = null;
    }

    /** Feeds one jamo. CONTEXTUAL_CONSONANT is a choseong index, VOWEL is a jungseong index. */
    public Result input(SemanticJamo jamo) {
        if (jamo == null) {
            throw new IllegalArgumentException("jamo must not be null");
        }
        // Only the input immediately after a syllable break may re-open it.
        closedSyllable = null;
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
                closedSyllable = new int[] {cho, jung, -1};
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
                closedSyllable = new int[] {cho, jung, jong};
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
        closedSyllable = null;
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

    /**
     * Takes back the syllable the consonant now composing pushed out, discarding that consonant.
     * The caller is left holding a syllable that is composing again, and must remove the one
     * character it had already committed for it. Returns null when there is nothing to re-open,
     * which is every case except the input right after a consonant closed a syllable.
     *
     * <p>This is what lets 나랏글 spell 많: ㅁㅏㄴ closes as 만 when ㅇ arrives, 획추가 asks for the
     * ㅇ back, and the ㅎ that replaces it then has a 만 to attach to.
     */
    public Result reopenClosedSyllable() {
        if (closedSyllable == null || state != State.CHO) {
            return null;
        }
        cho = closedSyllable[0];
        jung = closedSyllable[1];
        jong = closedSyllable[2];
        state = jong > 0 ? State.CHO_JUNG_JONG : State.CHO_JUNG;
        closedSyllable = null;
        return preedit(syllable());
    }

    /**
     * The last consonant of the composing syllable as a choseong index — the bare choseong, or the
     * batchim (a compound final answers its tail, the letter typed last) — or -1 when the syllable
     * does not end in a consonant. This is what a transformation key acts on when the interpreter
     * that would normally remember it has lost its run.
     */
    public int trailingConsonant() {
        switch (state) {
            case CHO:
                return cho;
            case CHO_JUNG_JONG:
                return HangulTables.splitJong(jong)[1];
            default:
                return -1;
        }
    }

    /** The composing syllable's vowel when it is the last jamo typed, or -1. */
    public int trailingVowel() {
        switch (state) {
            case JUNG:
            case CHO_JUNG:
                return jung;
            default:
                return -1;
        }
    }

    /**
     * Starts composing an already-written syllable, replacing whatever was composing. This is how
     * a transformation re-opens a character it found before the cursor: the caller removes the
     * character from the editor and shows the returned preedit in its place, and typing continues
     * as if the syllable had never been closed. Indices follow the standard tables; jung -1 for a
     * bare consonant, jong -1 (or 0) for none.
     */
    public Result seed(int cho, int jung, int jong) {
        if (cho < 0 && jung < 0) {
            throw new IllegalArgumentException("seed needs a choseong or a jungseong");
        }
        reset();
        if (cho < 0) {
            this.jung = jung;
            state = State.JUNG;
            return preedit(HangulTables.jungJamo(jung));
        }
        this.cho = cho;
        if (jung < 0) {
            state = State.CHO;
            return preedit(HangulTables.choJamo(cho));
        }
        this.jung = jung;
        if (jong <= 0) {
            state = State.CHO_JUNG;
        } else {
            this.jong = jong;
            state = State.CHO_JUNG_JONG;
        }
        return preedit(syllable());
    }

    /** The text currently composing — what the editor's preedit shows — without changing state. */
    public String preeditText() {
        switch (state) {
            case EMPTY:
                return "";
            case CHO:
                return HangulTables.choJamo(cho);
            case JUNG:
                return HangulTables.jungJamo(jung);
            default:
                return syllable();
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
