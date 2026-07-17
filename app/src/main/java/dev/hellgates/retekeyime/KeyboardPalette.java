package dev.hellgates.retekeyime;

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
    final int keyAccent;
    final int keyAccentSoft;
    final int keyText;
    final int keyTextMuted;
    final int keyShadow;
    final int hint;
    final int pressTint;

    private KeyboardPalette(int background, int keyFace, int keyDisabled, int keyAccent,
            int keyAccentSoft, int keyText, int keyTextMuted, int keyShadow, int hint, int pressTint) {
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

    static boolean isNight(Context context) {
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
        return context.getResources().getColor(colorRes, context.getTheme());
    }

    private static KeyboardPalette dynamic(Context context, boolean night) {
        if (night) {
            return new KeyboardPalette(
                sys(context, android.R.color.system_neutral1_900),
                sys(context, android.R.color.system_neutral1_700),
                sys(context, android.R.color.system_neutral1_800),
                sys(context, android.R.color.system_accent1_300),
                sys(context, android.R.color.system_accent2_700),
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
            sys(context, android.R.color.system_accent1_600),
            sys(context, android.R.color.system_accent2_100),
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
            Color.rgb(90, 140, 205),  // active key (primary)
            Color.rgb(55, 72, 96),    // one-shot active (soft)
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
            Color.rgb(120, 160, 215), // active key (primary)
            Color.rgb(196, 214, 240), // one-shot active (soft)
            Color.rgb(28, 30, 34),    // label (onSurface)
            Color.rgb(120, 128, 138), // muted label
            Color.rgb(176, 182, 190), // raised lip
            Color.rgb(110, 120, 132), // long-press hint
            Color.rgb(80, 140, 220)); // press tint
    }
}
