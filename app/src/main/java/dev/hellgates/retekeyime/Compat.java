package dev.hellgates.retekeyime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * The handful of places where a newer API is nicer and an older one still works.
 *
 * <p>Each method takes the modern path when the device has it and the long-standing path when it
 * does not, so the rest of the code can be written once. They are collected here rather than
 * scattered because they are the whole reason the app can run on Android 5 as well as 16 — a list
 * worth being able to read in one sitting.
 */
public final class Compat {
    private Compat() {
    }

    /** Cuts a rectangle out of what will be drawn. {@code clipOutRect} is API 26. */
    @SuppressWarnings("deprecation")
    public static void clipOut(Canvas canvas, float left, float top, float right, float bottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutRect(left, top, right, bottom);
        } else {
            canvas.clipRect(left, top, right, bottom, Region.Op.DIFFERENCE);
        }
    }

    /** {@code SeekBar.setMin} is API 26; below it the caller offsets the progress itself. */
    public static boolean canSetSeekBarMin() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /** Applies a text appearance. The one-argument form is API 23. */
    @SuppressWarnings("deprecation")
    public static void setTextAppearance(TextView view, int styleResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setTextAppearance(styleResId);
        } else {
            view.setTextAppearance(view.getContext(), styleResId);
        }
    }

    /** Reads a colour resource against the context's theme. The theme overload is API 23. */
    @SuppressWarnings("deprecation")
    public static int getColor(Context context, int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(colorResId, context.getTheme());
        }
        return context.getResources().getColor(colorResId);
    }

    /** A rounded rectangle. The all-float overload is API 21; the RectF one has always existed. */
    public static void drawRoundRect(Canvas canvas, float left, float top, float right,
            float bottom, float radius, Paint paint) {
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    /** A drop shadow under a popup. {@code setElevation} is API 21; below it there is none. */
    public static void setElevation(PopupWindow popup, float elevation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(elevation);
        }
    }

    /**
     * The language of a BCP-47 tag. {@code Locale.forLanguageTag} is API 21, and all this code
     * needs is the part before the first separator — "ko" out of "ko-KR".
     */
    public static String languageOf(String languageTag) {
        if (languageTag == null || languageTag.isEmpty()) {
            return "";
        }
        int end = 0;
        while (end < languageTag.length()) {
            char c = languageTag.charAt(end);
            if (c == '-' || c == '_') {
                break;
            }
            end++;
        }
        return languageTag.substring(0, end).toLowerCase(Locale.US);
    }

    /** UTF-8 without {@code StandardCharsets}, which is API 19. */
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /** {@code getSystemService(Class)} is API 23; the name-based form has always been there. */
    @SuppressWarnings("unchecked")
    public static <T> T systemService(Context context, String name, Class<T> type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getSystemService(type);
        }
        Object service = context.getSystemService(name);
        return type.isInstance(service) ? (T) service : null;
    }
}
