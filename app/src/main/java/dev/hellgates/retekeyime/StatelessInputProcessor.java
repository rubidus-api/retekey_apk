package dev.hellgates.retekeyime;

/**
 * Stateless P1A lowering from semantic input to ordered editor actions.
 * Stateful composition must use the later immutable transition-plan boundary.
 */
@FunctionalInterface
public interface StatelessInputProcessor {
    DispatchResult process(SemanticInput input);
}
