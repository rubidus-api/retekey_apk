package com.retekey;

@FunctionalInterface
public interface EditorEndpointProvider {
    EditorEndpoint resolve();
}
