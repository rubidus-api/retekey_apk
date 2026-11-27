package com.retekey;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;

/**
 * Isolated API 24+ direct-boot unlock helper so {@link ReteKeyImeService} carries no direct
 * {@link UserManager} reference on API 14–23.
 */
@TargetApi(24)
final class UserUnlockWatcher {
    private UserUnlockWatcher() {
    }

    static boolean isUserUnlocked(Context context) {
        try {
            UserManager users = Compat.systemService(
                context, Context.USER_SERVICE, UserManager.class);
            return users == null || users.isUserUnlocked();
        } catch (RuntimeException unknown) {
            return true;
        }
    }

    static BroadcastReceiver register(Context context, final Runnable onUnlocked) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                onUnlocked.run();
            }
        };
        try {
            context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
            return receiver;
        } catch (RuntimeException refused) {
            return null;
        }
    }
}
