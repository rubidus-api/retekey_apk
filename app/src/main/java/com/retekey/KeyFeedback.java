package com.retekey;

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
    // Enough to feel and hear that a key went in, without the keyboard buzzing at every letter.
    static final float DEFAULT_HAPTIC = 0.10f;
    static final float DEFAULT_SOUND = 0.10f;
    static final float DEFAULT_VISUAL = 0.30f;

    private final Vibrator vibrator;
    private final AudioManager audio;
    /**
     * The click is ours rather than the platform's. {@code playSoundEffect} takes a volume, but
     * plenty of devices ignore it and play the system's own keypress sound at the system's own
     * level — which is why the setting appeared to do nothing. A sample we play ourselves obeys
     * the number the user set.
     */
    private final android.media.SoundPool pool;
    private int clickId;
    private boolean clickReady;
    private float haptic = DEFAULT_HAPTIC;
    private float sound = DEFAULT_SOUND;
    private float visual = DEFAULT_VISUAL;

    KeyFeedback(Context context) {
        this.vibrator = resolveVibrator(context);
        this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.pool = createPool();
        if (pool != null) {
            pool.setOnLoadCompleteListener(new android.media.SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(android.media.SoundPool p, int id, int status) {
                    clickReady = status == 0;
                }
            });
            try {
                clickId = pool.load(context, R.raw.key_click, 1);
            } catch (RuntimeException ignored) {
                clickId = 0;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static android.media.SoundPool createPool() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new android.media.SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .build();
            }
            return new android.media.SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Frees the sample; the keyboard view calls this when it goes away. */
    void release() {
        if (pool != null) {
            try {
                pool.release();
            } catch (RuntimeException ignored) {
                // Nothing to do if the pool is already gone.
            }
        }
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
        if (sound > 0.0f) {
            try {
                if (pool != null && clickReady) {
                    pool.play(clickId, sound, sound, 1, 0, 1.0f);
                } else if (audio != null) {
                    // Until the sample is loaded, or if this device would not give us a pool.
                    audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, sound);
                }
            } catch (RuntimeException ignored) {
                // A device that refuses to make the sound must not break typing.
            }
        }
        if (haptic > 0.0f && vibrator != null) {
            try {
                if (vibrator.hasVibrator()) {
                    int durationMs = 15 + Math.round(haptic * 25.0f);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        int amplitude = vibrator.hasAmplitudeControl()
                            ? Math.max(1, Math.round(haptic * 255.0f))
                            : VibrationEffect.DEFAULT_AMPLITUDE;
                        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
                    } else {
                        // Before VibrationEffect the only dial is how long it buzzes.
                        vibrator.vibrate(durationMs);
                    }
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
