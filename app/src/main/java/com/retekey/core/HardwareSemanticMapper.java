package com.retekey.core;

@FunctionalInterface
public interface HardwareSemanticMapper {
    HardwareSemanticMapper NONE = (stableKeyId, shift) -> null;

    SemanticInput map(String stableKeyId, boolean shift);

    default SemanticInput map(String stableKeyId, boolean shift, boolean capsLock) {
        return map(stableKeyId, shift);
    }

    static HardwareSemanticMapper none() {
        return NONE;
    }
}
