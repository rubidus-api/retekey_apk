package com.retekey;

public final class ProjectKeyEvent {
    private final InputSource source;
    private final InputAction action;
    private final String stableKeyId;
    private final int keyCode;
    private final int scanCode;
    private final int deviceId;
    private final int deviceSource;
    private final String text;
    private final boolean shift;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean meta;
    private final boolean capsLock;
    private final boolean function;
    private final boolean sym;
    private final int rawMetaState;
    private final int repeatCount;
    private final boolean canceled;
    private final boolean deadKey;
    private final int combiningAccentCodePoint;
    private final SemanticInput semanticInput;

    private ProjectKeyEvent(Builder builder) {
        this.source = builder.source;
        this.action = builder.action;
        this.stableKeyId = builder.stableKeyId;
        this.keyCode = builder.keyCode;
        this.scanCode = builder.scanCode;
        this.deviceId = builder.deviceId;
        this.deviceSource = builder.deviceSource;
        this.text = builder.text;
        this.shift = builder.shift;
        this.ctrl = builder.ctrl;
        this.alt = builder.alt;
        this.meta = builder.meta;
        this.capsLock = builder.capsLock;
        this.function = builder.function;
        this.sym = builder.sym;
        this.rawMetaState = builder.rawMetaState;
        this.repeatCount = builder.repeatCount;
        this.canceled = builder.canceled;
        this.deadKey = builder.deadKey || builder.combiningAccentCodePoint != 0;
        this.combiningAccentCodePoint = builder.combiningAccentCodePoint;
        this.semanticInput = builder.semanticInput;
    }

    public static Builder builder(InputSource source, InputAction action) {
        return new Builder(source, action);
    }

    public static ProjectKeyEvent softwareDown(String stableKeyId, SemanticInput input) {
        return builder(InputSource.SOFTWARE, InputAction.DOWN)
            .stableKeyId(stableKeyId)
            .semanticInput(input)
            .build();
    }

    public InputSource source() {
        return source;
    }

    public InputAction action() {
        return action;
    }

    public String stableKeyId() {
        return stableKeyId;
    }

    public int keyCode() {
        return keyCode;
    }

    public int scanCode() {
        return scanCode;
    }

    public int deviceId() {
        return deviceId;
    }

    public int deviceSource() {
        return deviceSource;
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

    public boolean capsLock() {
        return capsLock;
    }

    public boolean function() {
        return function;
    }

    public boolean sym() {
        return sym;
    }

    public int rawMetaState() {
        return rawMetaState;
    }

    public int repeatCount() {
        return repeatCount;
    }

    public boolean canceled() {
        return canceled;
    }

    public boolean hasCombiningAccent() {
        return deadKey;
    }

    public boolean hasDeadKey() {
        return deadKey;
    }

    public int combiningAccentCodePoint() {
        return combiningAccentCodePoint;
    }

    public boolean hasSemanticInput() {
        return semanticInput != null;
    }

    public SemanticInput semanticInput() {
        return semanticInput;
    }

    public static final class Builder {
        private final InputSource source;
        private final InputAction action;
        private String stableKeyId = "";
        private int keyCode;
        private int scanCode;
        private int deviceId = -1;
        private int deviceSource;
        private String text = "";
        private boolean shift;
        private boolean ctrl;
        private boolean alt;
        private boolean meta;
        private boolean capsLock;
        private boolean function;
        private boolean sym;
        private int rawMetaState;
        private int repeatCount;
        private boolean canceled;
        private boolean deadKey;
        private int combiningAccentCodePoint;
        private SemanticInput semanticInput;

        private Builder(InputSource source, InputAction action) {
            if (source == null || action == null) {
                throw new IllegalArgumentException("source and action are required");
            }
            this.source = source;
            this.action = action;
        }

        public Builder stableKeyId(String stableKeyId) {
            if (stableKeyId == null) {
                throw new IllegalArgumentException("stableKeyId must not be null");
            }
            this.stableKeyId = stableKeyId;
            return this;
        }

        public Builder keyCode(int keyCode) {
            this.keyCode = keyCode;
            return this;
        }

        public Builder scanCode(int scanCode) {
            this.scanCode = scanCode;
            return this;
        }

        public Builder deviceId(int deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder deviceSource(int deviceSource) {
            this.deviceSource = deviceSource;
            return this;
        }

        public Builder text(String text) {
            if (text == null) {
                throw new IllegalArgumentException("text must not be null");
            }
            this.text = text;
            return this;
        }

        public Builder shift(boolean shift) {
            this.shift = shift;
            return this;
        }

        public Builder ctrl(boolean ctrl) {
            this.ctrl = ctrl;
            return this;
        }

        public Builder alt(boolean alt) {
            this.alt = alt;
            return this;
        }

        public Builder meta(boolean meta) {
            this.meta = meta;
            return this;
        }

        public Builder capsLock(boolean capsLock) {
            this.capsLock = capsLock;
            return this;
        }

        public Builder function(boolean function) {
            this.function = function;
            return this;
        }

        public Builder sym(boolean sym) {
            this.sym = sym;
            return this;
        }

        public Builder rawMetaState(int rawMetaState) {
            this.rawMetaState = rawMetaState;
            return this;
        }

        public Builder repeatCount(int repeatCount) {
            if (repeatCount < 0) {
                throw new IllegalArgumentException("repeatCount must not be negative");
            }
            this.repeatCount = repeatCount;
            return this;
        }

        public Builder canceled(boolean canceled) {
            this.canceled = canceled;
            return this;
        }

        public Builder combiningAccentCodePoint(int codePoint) {
            if (codePoint != 0 && !UnicodeScalar.isValid(codePoint)) {
                throw new IllegalArgumentException("invalid combining accent code point");
            }
            this.combiningAccentCodePoint = codePoint;
            if (codePoint != 0) {
                this.deadKey = true;
            }
            return this;
        }

        public Builder deadKey(boolean deadKey) {
            this.deadKey = deadKey;
            return this;
        }

        public Builder semanticInput(SemanticInput semanticInput) {
            this.semanticInput = semanticInput;
            return this;
        }

        public ProjectKeyEvent build() {
            return new ProjectKeyEvent(this);
        }
    }
}
