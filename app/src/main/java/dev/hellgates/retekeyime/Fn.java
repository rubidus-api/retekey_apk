package dev.hellgates.retekeyime;

/**
 * The few function shapes this project passes around.
 *
 * <p>{@code java.util.function} only exists from API 24. Declaring the four shapes actually used
 * costs a dozen lines and lets the app run on Android 5 without pulling in library desugaring,
 * which would add more to the APK than the whole keyboard weighs.
 */
public final class Fn {
    private Fn() {
    }

    /** Supplies a value on demand. */
    public interface Supplier<T> {
        T get();
    }

    /** Answers a yes/no question about live state. */
    public interface BooleanSupplier {
        boolean getAsBoolean();
    }

    /** Accepts one value. */
    public interface Consumer<T> {
        void accept(T value);
    }

    /** Accepts one int, for the editor command ids. */
    public interface IntConsumer {
        void accept(int value);
    }
}
