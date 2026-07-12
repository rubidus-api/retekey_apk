package dev.hellgates.retekeyime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DispatchResult {
    public enum Disposition {
        HANDLED,
        DELEGATE
    }

    private static final DispatchResult HANDLED_EMPTY =
        new DispatchResult(Disposition.HANDLED, Collections.emptyList());
    private static final DispatchResult DELEGATE_EMPTY =
        new DispatchResult(Disposition.DELEGATE, Collections.emptyList());

    private final Disposition disposition;
    private final List<KeyAction> actions;

    private DispatchResult(Disposition disposition, List<KeyAction> actions) {
        this.disposition = Objects.requireNonNull(disposition, "disposition");
        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        ArrayList<KeyAction> copy = new ArrayList<>(actions);
        if (copy.contains(null)) {
            throw new IllegalArgumentException("actions must not contain null");
        }
        this.actions = Collections.unmodifiableList(copy);
    }

    public static DispatchResult handled() {
        return HANDLED_EMPTY;
    }

    public static DispatchResult handled(KeyAction... actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        return handled(Arrays.asList(actions));
    }

    public static DispatchResult handled(List<KeyAction> actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        return actions.isEmpty() ? HANDLED_EMPTY : new DispatchResult(Disposition.HANDLED, actions);
    }

    public static DispatchResult delegate() {
        return DELEGATE_EMPTY;
    }

    public static DispatchResult delegate(KeyAction... actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        return delegate(Arrays.asList(actions));
    }

    public static DispatchResult delegate(List<KeyAction> actions) {
        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        return actions.isEmpty() ? DELEGATE_EMPTY : new DispatchResult(Disposition.DELEGATE, actions);
    }

    public Disposition disposition() {
        return disposition;
    }

    public boolean isHandled() {
        return disposition == Disposition.HANDLED;
    }

    public List<KeyAction> actions() {
        return actions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DispatchResult)) {
            return false;
        }
        DispatchResult that = (DispatchResult) other;
        return disposition == that.disposition && actions.equals(that.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disposition, actions);
    }

    @Override
    public String toString() {
        return "DispatchResult{" + "disposition=" + disposition + ", actions=" + actions + '}';
    }
}
