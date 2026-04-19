package top.nanboom233.pixelessentials.keepscreenon;

import android.content.Context;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnRouteActionDispatcher {
    public interface WriteSettingsGrantLauncher {
        void launch();
    }

    public void dispatchToggle(
            Context context,
            KeepScreenOnRouteResolution resolution,
            WriteSettingsGrantLauncher writeSettingsGrantLauncher
    ) {
        if (resolution.getRoute() == KeepScreenOnRoute.XPOSED) {
            KeepScreenOnState currentState = resolution.getState();
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on dispatcher toggling via Xposed route. active="
                            + currentState.isActive()
                            + ", durationIndex=" + currentState.getDurationIndex()
                            + ", expiresAt=" + currentState.getExpiresAtElapsedRealtime()
            );
            new KeepScreenOnXposedSessionManager(context)
                    .handleToggle(context, SystemClock.elapsedRealtime());
            return;
        }

        dispatchStandard(
                context,
                resolution,
                KeepScreenOnService.ACTION_TOGGLE,
                writeSettingsGrantLauncher
        );
    }

    public void dispatchSetInfinite(
            Context context,
            KeepScreenOnRouteResolution resolution,
            WriteSettingsGrantLauncher writeSettingsGrantLauncher
    ) {
        if (resolution.getRoute() == KeepScreenOnRoute.XPOSED) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on dispatcher setting infinite mode via Xposed route"
            );
            new KeepScreenOnXposedSessionManager(context)
                    .handleSetInfinite(context, SystemClock.elapsedRealtime());
            return;
        }

        dispatchStandard(
                context,
                resolution,
                KeepScreenOnService.ACTION_SET_INFINITE,
                writeSettingsGrantLauncher
        );
    }

    private void dispatchStandard(
            Context context,
            KeepScreenOnRouteResolution resolution,
            String action,
            WriteSettingsGrantLauncher writeSettingsGrantLauncher
    ) {
        KeepScreenOnSessionManager sessionManager = new KeepScreenOnSessionManager(context);
        KeepScreenOnState currentState = resolution.getState();
        boolean hasWriteSettingsPermission = sessionManager.hasWriteSettingsPermission(context);
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "Keep screen on dispatcher using standard route. action=" + action
                        + ", hasWriteSettingsPermission=" + hasWriteSettingsPermission
                        + ", active=" + currentState.isActive()
                        + ", durationIndex=" + currentState.getDurationIndex()
                        + ", expiresAt=" + currentState.getExpiresAtElapsedRealtime()
        );
        if (!hasWriteSettingsPermission) {
            YukiDiagnosticsLog.info(
                    ShortcutEntryActivity.TAG,
                    "Keep screen on dispatcher requesting WRITE_SETTINGS grant"
            );
            writeSettingsGrantLauncher.launch();
            return;
        }
        KeepScreenOnService.startAction(context, action);
    }
}
