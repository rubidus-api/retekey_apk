package com.retekey;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

/**
 * The keyboard's colours, resolved to the device's theme following Android guidance:
 *
 * <ul>
 *   <li>honour the system light/dark mode ({@link Configuration#UI_MODE_NIGHT_MASK});</li>
 *   <li>map to Material colour roles — background ≈ surface, key ≈ elevated surface, label ≈
 *       onSurface, active key ≈ primary, press ≈ a primary state layer;</li>
 *   <li>on Android 12+ pull the user's Material You dynamic palette
 *       ({@code system_neutral*}/{@code system_accent*}) so the keyboard matches their theme,
 *       falling back to a hand-tuned light/dark palette on older versions.</li>
 * </ul>
 */
final class KeyboardPalette {
    final int background;
    final int keyFace;
    final int keyDisabled;
    /** A key held down until it is released again. The strongest state a key can be in. */
    final int keyAccent;
    /** A key armed for exactly one press. Distinct from held, and no longer a whisper. */
    final int keyAccentSoft;
    final int keyText;
    /** The label ink for fills the ordinary one would disappear into. */
    final int keyTextInverse;
    final int keyTextMuted;
    final int keyShadow;
    final int hint;
    final int pressTint;

    private KeyboardPalette(int background, int keyFace, int keyDisabled, int keyAccent,
            int keyAccentSoft, int keyText, int keyTextMuted, int keyShadow, int hint, int pressTint) {
        this(background, keyFace, keyDisabled, keyAccent, keyAccentSoft, keyText, keyTextMuted,
            keyShadow, hint, pressTint,
            KeyLabelContrast.prefersDarkInk(Color.red(keyText), Color.green(keyText),
                Color.blue(keyText)) ? Color.rgb(248, 250, 253) : Color.rgb(18, 20, 24));
    }

    private KeyboardPalette(int background, int keyFace, int keyDisabled, int keyAccent,
            int keyAccentSoft, int keyText, int keyTextMuted, int keyShadow, int hint, int pressTint,
            int keyTextInverse) {
        this.keyTextInverse = keyTextInverse;
        this.background = background;
        this.keyFace = keyFace;
        this.keyDisabled = keyDisabled;
        this.keyAccent = keyAccent;
        this.keyAccentSoft = keyAccentSoft;
        this.keyText = keyText;
        this.keyTextMuted = keyTextMuted;
        this.keyShadow = keyShadow;
        this.hint = hint;
        this.pressTint = pressTint;
    }

    /**
     * The label colour for a key filled with {@code fill}. A latched key is painted strongly, and
     * "strongly" is dark in the light theme and vivid in the dark one, so the ink cannot be fixed
     * per theme — it follows the fill's own luminance.
     */
    int inkOn(int fill) {
        boolean fillWantsDarkInk =
            KeyLabelContrast.prefersDarkInk(Color.red(fill), Color.green(fill), Color.blue(fill));
        boolean ordinaryInkIsDark = KeyLabelContrast.prefersDarkInk(
            Color.red(keyText), Color.green(keyText), Color.blue(keyText));
        return fillWantsDarkInk == ordinaryInkIsDark ? keyTextInverse : keyText;
    }

    /**
     * The colour for the small corner marks on a key filled with {@code fill}: the hint colour
     * where the face is the ordinary one, and the face's own ink softened where it is not. A
     * latched key is painted in the ink colour itself, and the fixed hint colour disappears into
     * it.
     */
    /**
     * The face of a key that is latched down, and the ink over it: the same pairing the echo popup
     * uses for every keystroke — the accent as a background, the keyboard's own background colour
     * as the ink. Borrowing it means the strongest state a key can be in is painted in the one
     * combination the user already reads several times a second, rather than in a private scheme
     * they have to learn.
     */
    int keyLatchedFace() {
        return keyAccent;
    }

    /** The ink over a latched face: the popup's text colour, so the two match exactly. */
    int keyLatchedInk() {
        return background;
    }

    int hintOn(int fill) {
        if (fill == keyFace || fill == keyDisabled) {
            return hint;
        }
        return KeyPressTint.mix(inkOn(fill), fill, 0.35f);
    }

    /**
     * Whether the keyboard is painted dark: the user's own choice where they made one, and the
     * device's light/dark setting where they left it on {@link ThemeMode#SYSTEM}.
     */
    static boolean isNight(Context context) {
        return ScreenTheme.mode(context).night(systemNight(context));
    }

    /** What the device itself is set to, ignoring the app's own setting. */
    static boolean systemNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    }

    static KeyboardPalette resolve(Context context) {
        boolean night = isNight(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return dynamic(context, night);
            } catch (RuntimeException useStatic) {
                // Dynamic colours unavailable; fall back to the hand-tuned palette.
            }
        }
        return night ? dark() : light();
    }

    private static int sys(Context context, int colorRes) {
        return Compat.getColor(context, colorRes);
    }

    private static KeyboardPalette dynamic(Context context, boolean night) {
        if (night) {
            return new KeyboardPalette(
                sys(context, android.R.color.system_neutral1_900),
                sys(context, android.R.color.system_neutral1_700),
                sys(context, android.R.color.system_neutral1_800),
                sys(context, android.R.color.system_accent1_300),
                sys(context, android.R.color.system_accent1_800),
                sys(context, android.R.color.system_neutral1_50),
                sys(context, android.R.color.system_neutral2_300),
                sys(context, android.R.color.system_neutral1_1000),
                sys(context, android.R.color.system_neutral2_300),
                sys(context, android.R.color.system_accent1_300));
        }
        return new KeyboardPalette(
            sys(context, android.R.color.system_neutral1_100),
            sys(context, android.R.color.system_neutral1_50),
            sys(context, android.R.color.system_neutral1_200),
            sys(context, android.R.color.system_accent1_700),
            sys(context, android.R.color.system_accent1_200),
            sys(context, android.R.color.system_neutral1_900),
            sys(context, android.R.color.system_neutral2_500),
            sys(context, android.R.color.system_neutral1_300),
            sys(context, android.R.color.system_neutral2_500),
            sys(context, android.R.color.system_accent1_600));
    }

    private static KeyboardPalette dark() {
        return new KeyboardPalette(
            Color.rgb(23, 24, 28),    // background (surface)
            Color.rgb(46, 49, 56),    // key face (elevated)
            Color.rgb(33, 35, 40),    // disabled
            Color.rgb(86, 150, 224),  // held: vivid, the strongest state
            Color.rgb(48, 78, 116),   // armed for one key: deep, plainly not held
            Color.rgb(230, 233, 239), // label (onSurface)
            Color.rgb(139, 147, 158), // muted label
            Color.rgb(10, 11, 14),    // raised lip
            Color.rgb(150, 158, 170), // long-press hint
            Color.rgb(120, 170, 235)); // press tint
    }

    private static KeyboardPalette light() {
        return new KeyboardPalette(
            Color.rgb(210, 214, 220), // background (surface)
            Color.rgb(252, 253, 255), // key face (near white, elevated)
            Color.rgb(230, 233, 238), // disabled
            Color.rgb(28, 90, 168),   // held: deep, the strongest state
            Color.rgb(138, 180, 232), // armed for one key: mid, plainly not held
            Color.rgb(28, 30, 34),    // label (onSurface)
            Color.rgb(120, 128, 138), // muted label
            Color.rgb(176, 182, 190), // raised lip
            Color.rgb(110, 120, 132), // long-press hint
            Color.rgb(80, 140, 220)); // press tint
    }
}
