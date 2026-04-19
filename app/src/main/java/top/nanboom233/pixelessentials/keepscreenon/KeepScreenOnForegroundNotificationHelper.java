package top.nanboom233.pixelessentials.keepscreenon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.Locale;

import top.nanboom233.pixelessentials.R;

public final class KeepScreenOnForegroundNotificationHelper {
    private static final String CHANNEL_ID = "keep_screen_on_active";
    private static final int NOTIFICATION_ID = 1001;

    public void show(Context context, KeepScreenOnState state, long nowElapsedRealtime) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        ensureChannel(notificationManager, context);
        notificationManager.notify(
                NOTIFICATION_ID,
                createNotification(context, state, nowElapsedRealtime)
        );
    }

    public android.app.Notification createNotification(
            Context context,
            KeepScreenOnState state,
            long nowElapsedRealtime
    ) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        ensureChannel(notificationManager, context);
        return buildNotification(context, state, nowElapsedRealtime);
    }

    public void cancel(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public int getNotificationId() {
        return NOTIFICATION_ID;
    }

    private android.app.Notification buildNotification(
            Context context,
            KeepScreenOnState state,
            long nowElapsedRealtime
    ) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_tile_keep_screen_on)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(context.getString(R.string.keep_screen_on_tile_label));

        if (state.isInfinite()) {
            builder.setContentText(context.getString(R.string.keep_screen_on_notification_infinite));
        } else {
            long remainingMs = Math.max(0L, state.getExpiresAtElapsedRealtime() - nowElapsedRealtime);
            builder.setContentText(
                    context.getString(
                            R.string.keep_screen_on_notification_remaining,
                            formatRemaining(remainingMs)
                    )
            );
        }

        if (!isIgnoringBatteryOptimizations(context)) {
            builder.addAction(new Notification.Action.Builder(
                    0,
                    context.getString(R.string.keep_screen_on_notification_battery_action),
                    buildBatterySettingsPendingIntent(context)
            ).build());
        }

        return builder.build();
    }

    private PendingIntent buildBatterySettingsPendingIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:" + context.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(
                context,
                1004,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    private void ensureChannel(NotificationManager notificationManager, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.keep_screen_on_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);
    }

    private String formatRemaining(long remainingMs) {
        long remainingSeconds = (remainingMs + 999L) / 1000L;
        long minutes = remainingSeconds / 60L;
        long seconds = remainingSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
