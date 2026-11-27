package com.retekey.core;

@FunctionalInterface
public interface EditorEndpointProvider {
    EditorEndpoint resolve();
}
