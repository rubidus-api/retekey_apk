package dev.hellgates.retekeyime;

public final class ProjectKeyEvent {
    private final int keyCode;
    private final String text;
    private final boolean shift;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean meta;
    private final int repeatCount;

    public ProjectKeyEvent(
        int keyCode,
        String text,
        boolean shift,
        boolean ctrl,
        boolean alt,
        boolean meta,
        int repeatCount
    ) {
        this.keyCode = keyCode;
        this.text = text;
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
        this.meta = meta;
        this.repeatCount = repeatCount;
    }

    public int keyCode() {
        return keyCode;
    }

    public String text() {
        return text;
    }

    public boolean shift() {
        return shift;
    }

    public boolean ctrl() {
        return ctrl;
    }

    public boolean alt() {
        return alt;
    }

    public boolean meta() {
        return meta;
    }

    public int repeatCount() {
        return repeatCount;
    }
}
