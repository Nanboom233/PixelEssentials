package top.nanboom233.pixelessentials;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.Locale;

import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnInteractionModel;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRoute;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRouteActionDispatcher;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRouteResolution;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnRouteResolver;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnSessionManager;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnState;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnTilePresentation;
import top.nanboom233.pixelessentials.keepscreenon.KeepScreenOnTilePresentationFactory;
import top.nanboom233.pixelessentials.logging.YukiDiagnosticsLog;

public final class KeepScreenOnTileService extends TileService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final KeepScreenOnRouteActionDispatcher actionDispatcher = new KeepScreenOnRouteActionDispatcher();
    private final KeepScreenOnTilePresentationFactory presentationFactory =
            new KeepScreenOnTilePresentationFactory();
    private static final long STATUS_REQUEST_DEBOUNCE_MS = 2000L;
    private String lastLoggedTileSnapshot;
    private long lastStatusRequestElapsedRealtime;
    private KeepScreenOnRouteResolver cachedRouteResolver;
    private KeepScreenOnSessionManager cachedSessionManager;
    private final Runnable refreshRunnable = this::refreshTileState;

    @Override
    public void onStartListening() {
        super.onStartListening();
        YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on tile started listening");
        handler.removeCallbacks(refreshRunnable);
        refreshRunnable.run();
        long now = SystemClock.elapsedRealtime();
        if (now - lastStatusRequestElapsedRealtime > STATUS_REQUEST_DEBOUNCE_MS) {
            lastStatusRequestElapsedRealtime = now;
            requestRouteStatusRefresh();
            handler.postDelayed(
                    () -> reconcileTileStateAfterStatusRequest(now),
                    KeepScreenOnRouteResolver.STATUS_REQUEST_GRACE_PERIOD_MS
            );
        }
    }

    @Override
    public void onStopListening() {
        YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on tile stopped listening");
        handler.removeCallbacks(refreshRunnable);
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        unlockAndRun(() -> {
            handler.removeCallbacks(refreshRunnable);
            dispatchToggleImmediately();
        });
    }

    private void refreshTileState() {
        long nowElapsedRealtime = SystemClock.elapsedRealtime();
        KeepScreenOnRouteResolution resolution = routeResolver()
                .resolveCurrentState(nowElapsedRealtime);
        boolean hasWriteSettingsPermission = sessionManager().hasWriteSettingsPermission(this);
        updateTile(resolution, hasWriteSettingsPermission, nowElapsedRealtime);
    }

    private void dispatchToggleImmediately() {
        long nowElapsedRealtime = SystemClock.elapsedRealtime();
        KeepScreenOnRouteResolution resolution = routeResolver()
                .resolveCurrentState(nowElapsedRealtime);
        boolean hasWriteSettingsPermission = sessionManager().hasWriteSettingsPermission(this);
        KeepScreenOnRouteResolution previewResolution = previewToggleResolution(resolution, hasWriteSettingsPermission, nowElapsedRealtime);
        actionDispatcher.dispatchToggle(this, resolution, this::launchWriteSettingsGrant);
        if (previewResolution != null) {
            updateTile(previewResolution, hasWriteSettingsPermission, nowElapsedRealtime);
        }
    }

    private void reconcileTileStateAfterStatusRequest(long requestedAtElapsedRealtimeMs) {
        long nowElapsedRealtime = SystemClock.elapsedRealtime();
        KeepScreenOnRouteResolution resolution = routeResolver()
                .resolveAfterStatusRequest(requestedAtElapsedRealtimeMs, nowElapsedRealtime);
        boolean hasWriteSettingsPermission = sessionManager().hasWriteSettingsPermission(this);
        updateTile(resolution, hasWriteSettingsPermission, nowElapsedRealtime);
    }

    private void updateTile(KeepScreenOnRouteResolution resolution, boolean hasWriteSettingsPermission, long nowElapsedRealtime) {
        Tile tile = getQsTile();
        if (tile == null) {
            YukiDiagnosticsLog.warn(ShortcutEntryActivity.TAG, "Keep screen on tile unavailable: getQsTile returned null");
            return;
        }

        tile.setLabel(getString(R.string.keep_screen_on_tile_label));
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_keep_screen_on));

        KeepScreenOnTilePresentation presentation = presentationFactory.create(
                resolution,
                hasWriteSettingsPermission,
                nowElapsedRealtime
        );
        tile.setState(presentation.getTileState());
        switch (presentation.getSubtitleMode()) {
            case PERMISSION_REQUIRED:
                tile.setSubtitle(getString(R.string.keep_screen_on_tile_permission_subtitle));
                break;
            case INFINITE:
                tile.setSubtitle(getString(R.string.keep_screen_on_tile_infinite_subtitle));
                break;
            case COUNTDOWN:
                tile.setSubtitle(formatRemaining(presentation.getRemainingSeconds()));
                break;
            case DISABLED:
            default:
                tile.setSubtitle(getString(R.string.keep_screen_on_tile_disabled_subtitle));
                break;
        }

        logTileStateIfChanged(
                resolution.getRoute(),
                tile.getState(),
                resolution.getState(),
                hasWriteSettingsPermission
        );
        tile.updateTile();
        scheduleNextRefresh(presentation);
    }

    private KeepScreenOnRouteResolution previewToggleResolution(
            KeepScreenOnRouteResolution resolution,
            boolean hasWriteSettingsPermission,
            long nowElapsedRealtime
    ) {
        if (resolution.getRoute() == KeepScreenOnRoute.STANDARD
                && !hasWriteSettingsPermission) {
            return null;
        }
        KeepScreenOnState previewState = new KeepScreenOnInteractionModel()
                .handleClick(resolution.getState(), nowElapsedRealtime);
        return new KeepScreenOnRouteResolution(resolution.getRoute(), previewState);
    }

    private String formatRemaining(long remainingSeconds) {
        long minutes = remainingSeconds / 60L;
        long seconds = remainingSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private KeepScreenOnRouteResolver routeResolver() {
        if (cachedRouteResolver == null) {
            cachedRouteResolver = new KeepScreenOnRouteResolver(this);
        }
        return cachedRouteResolver;
    }

    private KeepScreenOnSessionManager sessionManager() {
        if (cachedSessionManager == null) {
            cachedSessionManager = new KeepScreenOnSessionManager(this);
        }
        return cachedSessionManager;
    }

    private void requestRouteStatusRefresh() {
        routeResolver().requestStatusRefresh();
    }

    private void scheduleNextRefresh(KeepScreenOnTilePresentation presentation) {
        handler.removeCallbacks(refreshRunnable);
        long nextDelayMs = presentation.getNextRefreshDelayMs();
        if (nextDelayMs >= 0L) {
            handler.postDelayed(refreshRunnable, nextDelayMs);
        }
    }

    private void launchWriteSettingsGrant() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, WriteSettingsGrantActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        startActivityAndCollapse(pendingIntent);
    }

    private void logTileStateIfChanged(
            KeepScreenOnRoute route,
            int tileState,
            KeepScreenOnState keepScreenOnState,
            boolean hasWriteSettingsPermission
    ) {
        String snapshot = "route=" + route
                + ", tileState=" + tileState
                + ", hasWriteSettingsPermission=" + hasWriteSettingsPermission
                + ", active=" + keepScreenOnState.isActive()
                + ", infinite=" + keepScreenOnState.isInfinite()
                + ", durationIndex=" + keepScreenOnState.getDurationIndex()
                + ", expiresAt=" + keepScreenOnState.getExpiresAtElapsedRealtime();
        if (!snapshot.equals(lastLoggedTileSnapshot)) {
            lastLoggedTileSnapshot = snapshot;
            YukiDiagnosticsLog.info(ShortcutEntryActivity.TAG, "Keep screen on tile state updated. " + snapshot);
        }
    }
}
