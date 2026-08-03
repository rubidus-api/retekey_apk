package com.retekey;

@FunctionalInterface
public interface HardwareSemanticMapper {
    HardwareSemanticMapper NONE = (stableKeyId, shift) -> null;

    SemanticInput map(String stableKeyId, boolean shift);

    static HardwareSemanticMapper none() {
        return NONE;
    }
}
