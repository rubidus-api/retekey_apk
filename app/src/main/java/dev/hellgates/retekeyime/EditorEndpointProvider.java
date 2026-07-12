package dev.hellgates.retekeyime;

@FunctionalInterface
public interface EditorEndpointProvider {
    EditorEndpoint resolve();
}
