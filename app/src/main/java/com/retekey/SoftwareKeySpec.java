package com.retekey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SoftwareKeySpec {
    private final String stableKeyId;
    private final String label;
    private final SemanticInput semanticInput;
    private final ControlKey control;
    private final int columnSpan;
    private final List<String> longPressTexts;
    private final ControlKey longPressControl;
    /** What to write in the corner for a long press that runs a control rather than typing. */
    private final String longPressHint;
    /**
     * What a directional drag off this key types: left, up, right, down — null where a direction
     * has nothing. Null (rather than four nulls) when the key has no flicks at all. Set after
     * construction by {@link #withFlicks}, which returns a copy like every other wither; kept
     * outside the constructors so the dozen existing call chains stay untouched.
     */
    private String[] flickTexts;

    private SoftwareKeySpec(
        String stableKeyId,
        String label,
        SemanticInput semanticInput,
        ControlKey control,
        int columnSpan,
        List<String> longPressTexts,
        ControlKey longPressControl
    ) {
        this(stableKeyId, label, semanticInput, control, columnSpan, longPressTexts,
            longPressControl, null);
    }

    private SoftwareKeySpec(
        String stableKeyId,
        String label,
        SemanticInput semanticInput,
        ControlKey control,
        int columnSpan,
        List<String> longPressTexts,
        ControlKey longPressControl,
        String longPressHint
    ) {
        this.longPressHint = longPressHint;
        if (stableKeyId == null || stableKeyId.isEmpty()) {
            throw new IllegalArgumentException("stable key id must not be empty");
        }
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("key label must not be empty");
        }
        if (columnSpan < 1) {
            throw new IllegalArgumentException("column span must be at least one column");
        }
        if (semanticInput != null && control != null) {
            throw new IllegalArgumentException("a key is either semantic or view-local, not both");
        }
        if (!longPressTexts.isEmpty() && longPressControl != null) {
            throw new IllegalArgumentException("a long press is either text or a control, not both");
        }
        this.stableKeyId = stableKeyId;
        this.label = label;
        this.semanticInput = semanticInput;
        this.control = control;
        this.columnSpan = columnSpan;
        this.longPressTexts = longPressTexts;
        this.longPressControl = longPressControl;
    }

    public static SoftwareKeySpec enabled(
        String stableKeyId,
        String label,
        SemanticInput semanticInput
    ) {
        if (semanticInput == null) {
            throw new IllegalArgumentException("enabled key requires semantic input");
        }
        return new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            null,
            1,
            Collections.emptyList(),
            null
        );
    }

    public static SoftwareKeySpec control(String stableKeyId, String label, ControlKey control) {
        if (control == null) {
            throw new IllegalArgumentException("control key requires a control command");
        }
        return new SoftwareKeySpec(
            stableKeyId,
            label,
            null,
            control,
            1,
            Collections.emptyList(),
            null
        );
    }

    public static SoftwareKeySpec disabled(String stableKeyId, String label) {
        return new SoftwareKeySpec(
            stableKeyId,
            label,
            null,
            null,
            1,
            Collections.emptyList(),
            null
        );
    }

    /**
     * The characters a drag off this key types, by direction; null skips a direction. A key with
     * flicks raises the four-way guide on a hold, the way a 12-key cell does, with its first
     * long-press character in the middle.
     */
    public SoftwareKeySpec withFlicks(String left, String up, String right, String down) {
        SoftwareKeySpec copy = copyOf(this);
        for (String text : new String[] {left, up, right, down}) {
            if (text != null) {
                SemanticInput.text(text); // the committable-text guard, as withLongPress uses
            }
        }
        copy.flickTexts = new String[] {left, up, right, down};
        return copy;
    }

    public boolean hasFlicks() {
        return flickTexts != null;
    }

    /** What a drag {@code direction} types, or null for nothing there (or no flicks at all). */
    public String flickText(CheonjiinInterpreter.Flick direction) {
        if (flickTexts == null || direction == null) {
            return null;
        }
        switch (direction) {
            case LEFT: return flickTexts[0];
            case UP: return flickTexts[1];
            case RIGHT: return flickTexts[2];
            case DOWN: return flickTexts[3];
            default: return null;
        }
    }

    private static SoftwareKeySpec copyOf(SoftwareKeySpec from) {
        SoftwareKeySpec copy = new SoftwareKeySpec(
            from.stableKeyId,
            from.label,
            from.semanticInput,
            from.control,
            from.columnSpan,
            from.longPressTexts,
            from.longPressControl,
            from.longPressHint
        );
        copy.flickTexts = from.flickTexts;
        return copy;
    }

    public SoftwareKeySpec withColumnSpan(int newColumnSpan) {
        SoftwareKeySpec next = new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            control,
            newColumnSpan,
            longPressTexts,
            longPressControl,
            longPressHint
        );
        next.flickTexts = flickTexts;
        return next;
    }

    /**
     * Alternate characters reachable by holding the key. They are plain text, in the order the
     * popup shows them, and only an enabled text key can carry them.
     */
    public SoftwareKeySpec withLongPress(String... texts) {
        if (!enabled()) {
            throw new IllegalStateException("only an enabled key can carry long-press characters");
        }
        if (texts == null || texts.length == 0) {
            throw new IllegalArgumentException("long-press characters must not be empty");
        }
        List<String> candidates = new ArrayList<>(texts.length);
        for (String text : texts) {
            // Reuse the semantic-input guard: a candidate must be committable text.
            SemanticInput.text(text);
            candidates.add(text);
        }
        SoftwareKeySpec next = new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            control,
            columnSpan,
            Collections.unmodifiableList(candidates),
            longPressControl
        );
        next.flickTexts = flickTexts;
        return next;
    }

    /**
     * A view-local command reached by holding the key, while a tap still commits the key's own
     * input. The period uses this to switch to the symbol layer without spending a second key.
     */
    public SoftwareKeySpec withLongPressControl(ControlKey longPressCommand) {
        if (longPressCommand == null) {
            throw new IllegalArgumentException("long-press control must not be null");
        }
        SoftwareKeySpec next = new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            control,
            columnSpan,
            longPressTexts,
            longPressCommand,
            longPressHint
        );
        next.flickTexts = flickTexts;
        return next;
    }

    public List<String> longPressTexts() {
        return longPressTexts;
    }

    public boolean hasLongPress() {
        return !longPressTexts.isEmpty();
    }

    public boolean hasLongPressControl() {
        return longPressControl != null;
    }

    /**
     * Marks what this key's long press reaches, in one or two characters drawn in the corner —
     * "m" for the menu, "p" for the pad. A control has no character of its own the way an
     * alternate letter does, so without this the corner can only say that a long press exists.
     */
    public SoftwareKeySpec withLongPressHint(String hint) {
        if (hint == null || hint.isEmpty()) {
            throw new IllegalArgumentException("long-press hint must not be empty");
        }
        if (longPressControl == null) {
            throw new IllegalStateException("a long-press hint needs a long press to describe");
        }
        SoftwareKeySpec next = new SoftwareKeySpec(
            stableKeyId,
            label,
            semanticInput,
            control,
            columnSpan,
            longPressTexts,
            longPressControl,
            hint
        );
        next.flickTexts = flickTexts;
        return next;
    }

    public boolean hasLongPressHint() {
        return longPressHint != null;
    }

    public String longPressHint() {
        if (longPressHint == null) {
            throw new IllegalStateException("key has no long-press hint");
        }
        return longPressHint;
    }

    public ControlKey longPressControl() {
        if (longPressControl == null) {
            throw new IllegalStateException("key has no long-press control");
        }
        return longPressControl;
    }

    /** The event for the alternate character at {@code index} of the long-press popup. */
    public ProjectKeyEvent longPressEvent(int index) {
        if (index < 0 || index >= longPressTexts.size()) {
            throw new IndexOutOfBoundsException("no long-press candidate at " + index);
        }
        String text = longPressTexts.get(index);
        return ProjectKeyEvent.softwareDown(
            stableKeyId + ".long." + index,
            SemanticInput.text(text)
        );
    }

    public String stableKeyId() {
        return stableKeyId;
    }

    public String label() {
        return label;
    }

    public int columnSpan() {
        return columnSpan;
    }

    public boolean enabled() {
        return semanticInput != null;
    }

    public boolean isControl() {
        return control != null;
    }

    public ControlKey control() {
        return control;
    }

    public SemanticInput semanticInput() {
        return semanticInput;
    }

    public ProjectKeyEvent pressEvent() {
        if (!enabled()) {
            throw new IllegalStateException("disabled key cannot emit input");
        }
        return ProjectKeyEvent.softwareDown(stableKeyId, semanticInput);
    }

    @Override
    public String toString() {
        return "SoftwareKeySpec{" +
            "stableKeyId='" + stableKeyId + '\'' +
            ", enabled=" + enabled() +
            ", control=" + control +
            ", columnSpan=" + columnSpan +
            '}';
    }
}
