package dev.hellgates.retekeyime;

public final class RawHardwareKeyEvent {
    private final InputAction action;
    private final String stableKeyId;
    private final int keyCode;
    private final int scanCode;
    private final int deviceId;
    private final int deviceSource;
    private final int unicodeValue;
    private final String characters;
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
    private final SemanticInput mappedInput;

    private RawHardwareKeyEvent(Builder builder) {
        this.action = builder.action;
        this.stableKeyId = builder.stableKeyId;
        this.keyCode = builder.keyCode;
        this.scanCode = builder.scanCode;
        this.deviceId = builder.deviceId;
        this.deviceSource = builder.deviceSource;
        this.unicodeValue = builder.unicodeValue;
        this.characters = builder.characters;
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
        this.mappedInput = builder.mappedInput;
    }

    public static Builder builder(InputAction action, int keyCode) {
        return new Builder(action, keyCode);
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

    public int unicodeValue() {
        return unicodeValue;
    }

    public String characters() {
        return characters;
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

    public SemanticInput mappedInput() {
        return mappedInput;
    }

    public static final class Builder {
        private final InputAction action;
        private final int keyCode;
        private String stableKeyId = "";
        private int scanCode;
        private int deviceId = -1;
        private int deviceSource;
        private int unicodeValue;
        private String characters = "";
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
        private SemanticInput mappedInput;

        private Builder(InputAction action, int keyCode) {
            if (action == null) {
                throw new IllegalArgumentException("action must not be null");
            }
            this.action = action;
            this.keyCode = keyCode;
        }

        public Builder stableKeyId(String stableKeyId) {
            if (stableKeyId == null) {
                throw new IllegalArgumentException("stableKeyId must not be null");
            }
            this.stableKeyId = stableKeyId;
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

        public Builder unicodeValue(int unicodeValue) {
            this.unicodeValue = unicodeValue;
            return this;
        }

        public Builder characters(String characters) {
            if (characters == null) {
                throw new IllegalArgumentException("characters must not be null");
            }
            this.characters = characters;
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

        public Builder mappedInput(SemanticInput mappedInput) {
            this.mappedInput = mappedInput;
            return this;
        }

        public RawHardwareKeyEvent build() {
            return new RawHardwareKeyEvent(this);
        }
    }
}
