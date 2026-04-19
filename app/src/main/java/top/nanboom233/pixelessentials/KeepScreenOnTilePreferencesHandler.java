package top.nanboom233.pixelessentials;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;

import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRouteActionDispatcher;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRouteResolution;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRouteResolver;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnTilePreferencesHandler {
    private final KeepScreenOnRouteActionDispatcher actionDispatcher = new KeepScreenOnRouteActionDispatcher();

    public boolean handle(TilePreferencesActivity activity, Intent intent) {
        ComponentName componentName = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        if (componentName == null
                || !KeepScreenOnTileService.class.getName().equals(componentName.getClassName())) {
            return false;
        }

        KeepScreenOnRouteResolver routeResolver = new KeepScreenOnRouteResolver(activity);
        dispatchLongClickImmediately(activity, routeResolver);
        return true;
    }

    private void dispatchLongClickImmediately(
            TilePreferencesActivity activity,
            KeepScreenOnRouteResolver routeResolver
    ) {
        KeepScreenOnRouteResolution resolution = routeResolver
                .resolveCurrentState(SystemClock.elapsedRealtime());
        actionDispatcher.dispatchSetInfinite(
                activity,
                resolution,
                () -> activity.startActivity(new Intent(activity, WriteSettingsGrantActivity.class))
        );
        routeResolver.requestStatusRefresh();
        YukiDiagnosticsLog.info(
                ShortcutEntryActivity.TAG,
                "QS tile preferences requested keep screen on infinite mode. route=" + resolution.getRoute()
        );
        activity.finishSilently();
    }
}
