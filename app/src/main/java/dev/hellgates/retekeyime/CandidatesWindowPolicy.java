package dev.hellgates.retekeyime;

/**
 * Decides whether showing the candidate strip must also force the IME window open, and whether
 * hiding it must give that window back.
 *
 * <p>The platform only lays the candidates area out inside the IME window. When the on-screen
 * keyboard is suppressed — which {@link SoftKeyboardVisibilityPolicy} does as soon as a hardware
 * or Bluetooth keyboard is attached — that window is never displayed, so making the candidates
 * frame "visible" puts it on a surface nobody ever sees: the 한자 key looks dead. In that state the
 * service has to ask for its own window explicitly, and hand it back when the strip closes, so the
 * keyboard does not linger on screen after the conversion.
 *
 * <p>When the input view is already shown the window is up anyway, so neither request is made and
 * the strip simply appears above the keyboard.
 */
public final class CandidatesWindowPolicy {
    private CandidatesWindowPolicy() {
    }

    /**
     * <p>The signal is the editor's own request, not the visibility of our view: with a hardware
     * keyboard the app never asks for soft input, so the platform keeps the IME window hidden even
     * though the service considers its decor visible.
     *
     * @param softInputRequested whether the editor asked for the soft input window
     * @return whether the service must explicitly request its window to show the strip
     */
    public static boolean mustForceWindow(boolean softInputRequested) {
        return !softInputRequested;
    }

    /**
     * @param windowWasForced whether {@link #mustForceWindow} previously forced the window open
     * @return whether hiding the strip must also hide the window again
     */
    public static boolean mustReleaseWindow(boolean windowWasForced) {
        return windowWasForced;
    }
}
