package top.nanboom233.pixelessentials.keepscreenon;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnModuleBridge {
    public static final String BRIDGE_PERMISSION =
            "top.nanboom233.pixelessentials.permission.XPOSED_BRIDGE";
    public static final String ACTION_SYNC =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_SYNC";
    public static final String ACTION_STOP =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_XPOSED_STOP";
    public static final String ACTION_STATUS_REQUEST =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_XPOSED_STATUS_REQUEST";
    public static final String ACTION_STATUS_CALLBACK =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_XPOSED_STATUS_CALLBACK";
    public static final String ACTION_MODULE_STOP_CALLBACK =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_MODULE_STOP_CALLBACK";
    public static final String ACTION_HEARTBEAT_CALLBACK =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_XPOSED_HEARTBEAT_CALLBACK";

    public static final String EXTRA_ACTIVE = "extra_active";
    public static final String EXTRA_DURATION_INDEX = "extra_duration_index";
    public static final String EXTRA_EXPIRES_AT = "extra_expires_at";
    public static final String EXTRA_STOP_CALLBACK = "extra_stop_callback";
    public static final String EXTRA_STOP_REASON = "extra_stop_reason";
    public static final String EXTRA_STATUS_CALLBACK = "extra_status_callback";

    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final int STOP_CALLBACK_REQUEST_CODE = 1002;
    private static final int STATUS_CALLBACK_REQUEST_CODE = 1003;

    public void sendSync(Context context, KeepScreenOnState state) {
        PendingIntent stopCallback = buildStopCallback(context);
        Intent intent = new Intent(ACTION_SYNC)
                .setPackage(SYSTEM_UI_PACKAGE)
                .putExtra(EXTRA_ACTIVE, state.isActive())
                .putExtra(EXTRA_DURATION_INDEX, state.getDurationIndex())
                .putExtra(EXTRA_EXPIRES_AT, state.getExpiresAtElapsedRealtime())
                .putExtra(EXTRA_STOP_CALLBACK, stopCallback);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on bridge sending sync. active=" + state.isActive()
                        + ", durationIndex=" + state.getDurationIndex()
                        + ", expiresAt=" + state.getExpiresAtElapsedRealtime()
                        + ", hasStopCallback=" + (stopCallback != null)
        );
        context.sendBroadcast(intent);
    }

    public void sendStop(Context context) {
        YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on bridge sending stop");
        context.sendBroadcast(new Intent(ACTION_STOP).setPackage(SYSTEM_UI_PACKAGE));
    }

    public void requestStatus(Context context) {
        PendingIntent statusCallback = buildStatusCallback(context);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on bridge requesting Xposed status. hasStatusCallback=" + (statusCallback != null)
        );
        Intent intent = new Intent(ACTION_STATUS_REQUEST)
                .setPackage(SYSTEM_UI_PACKAGE)
                .putExtra(EXTRA_STATUS_CALLBACK, statusCallback);
        context.sendBroadcast(intent);
    }

    private PendingIntent buildStopCallback(Context context) {
        Intent intent = new Intent(context, KeepScreenOnXposedCallbackReceiver.class)
                .setAction(ACTION_MODULE_STOP_CALLBACK);
        return PendingIntent.getBroadcast(
                context,
                STOP_CALLBACK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }

    private PendingIntent buildStatusCallback(Context context) {
        Intent intent = new Intent(context, KeepScreenOnXposedCallbackReceiver.class)
                .setAction(ACTION_STATUS_CALLBACK);
        return PendingIntent.getBroadcast(
                context,
                STATUS_CALLBACK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }
}
