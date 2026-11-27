package com.retekey;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipDescription;
import android.os.PersistableBundle;

/**
 * Isolated API 24+ {@link PersistableBundle} reader for Android clipboard sensitivity extras so
 * pre-API 24 builds never verify {@code ClipDescription.getExtras()} directly inside the service.
 */
@TargetApi(24)
final class ClipSensitivityCompat {
    private ClipSensitivityCompat() {
    }

    static boolean isMarkedSensitive(ClipData data) {
        ClipDescription description = data == null ? null : data.getDescription();
        if (description == null) {
            return false;
        }
        PersistableBundle extras = description.getExtras();
        return extras != null && (extras.getBoolean("android.content.extra.IS_SENSITIVE", false)
            || extras.getBoolean("android.clipboard.extra.IS_SENSITIVE", false));
    }
}
