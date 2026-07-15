package dev.hellgates.retekeyime;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * Key-press feedback: a visual press highlight, a haptic tick, and a click sound. Each has its own
 * 0–1 strength read from the shared {@code retekey_view} preferences, so the settings screen and the
 * keyboard agree. Strength 0 turns that channel off.
 */
final class KeyFeedback {
    static final String KEY_HAPTIC = "haptic_strength";
    static final String KEY_SOUND = "sound_volume";
    static final String KEY_VISUAL = "visual_intensity";
    static final float DEFAULT_HAPTIC = 0.4f;
    static final float DEFAULT_SOUND = 0.3f;
    static final float DEFAULT_VISUAL = 0.6f;

    private final Vibrator vibrator;
    private final AudioManager audio;
    private float haptic = DEFAULT_HAPTIC;
    private float sound = DEFAULT_SOUND;
    private float visual = DEFAULT_VISUAL;

    KeyFeedback(Context context) {
        this.vibrator = resolveVibrator(context);
        this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /** Re-reads the three strengths; call when the keyboard is (re)shown so settings apply. */
    void reload(SharedPreferences prefs) {
        haptic = clamp(prefs.getFloat(KEY_HAPTIC, DEFAULT_HAPTIC));
        sound = clamp(prefs.getFloat(KEY_SOUND, DEFAULT_SOUND));
        visual = clamp(prefs.getFloat(KEY_VISUAL, DEFAULT_VISUAL));
    }

    /** Alpha (0–1) of the pressed-key overlay the view draws. */
    float visualIntensity() {
        return visual;
    }

    /**
     * Plays the haptic tick and click sound for a key press, honoring each strength. Feedback is
     * best-effort: device-specific vibrator/audio failures must never crash the keyboard, so every
     * call is guarded.
     */
    void playKeyDown() {
        if (sound > 0.0f && audio != null) {
            try {
                audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, sound);
            } catch (RuntimeException ignored) {
                // A device that refuses the sound effect must not break typing.
            }
        }
        if (haptic > 0.0f && vibrator != null) {
            try {
                if (vibrator.hasVibrator()) {
                    int durationMs = 15 + Math.round(haptic * 25.0f);
                    int amplitude = vibrator.hasAmplitudeControl()
                        ? Math.max(1, Math.round(haptic * 255.0f))
                        : VibrationEffect.DEFAULT_AMPLITUDE;
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
                }
            } catch (RuntimeException ignored) {
                // Some devices throw from the vibrator; feedback is optional, typing is not.
            }
        }
    }

    private static Vibrator resolveVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager =
                (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return manager == null ? null : manager.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
