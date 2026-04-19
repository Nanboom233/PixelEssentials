package top.nanboom233.pixelessentials.keepscreenon;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public final class KeepScreenOnAlarmScheduler {
    public static final String ACTION_EXPIRE =
            "top.nanboom233.pixelessentials.action.KEEP_SCREEN_ON_EXPIRE";

    private static final int REQUEST_CODE = 1001;

    public void schedule(Context context, KeepScreenOnState state, long nowElapsedRealtime) {
        cancel(context);
        if (!state.isActive() || state.isInfinite()) {
            return;
        }

        long triggerAtElapsedRealtime = nowElapsedRealtime
                + Math.max(0L, state.getExpiresAtElapsedRealtime() - nowElapsedRealtime);
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        PendingIntent pendingIntent = buildPendingIntent(context);
        if (alarmManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtElapsedRealtime,
                    pendingIntent
            );
            return;
        }

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedRealtime,
                pendingIntent
        );
    }

    public void cancel(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildPendingIntent(context));
    }

    private PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, KeepScreenOnEventReceiver.class).setAction(ACTION_EXPIRE);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}

