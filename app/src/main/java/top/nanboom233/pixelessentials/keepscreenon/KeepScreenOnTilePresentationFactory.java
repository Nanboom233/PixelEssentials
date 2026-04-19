package top.nanboom233.pixelessentials.keepscreenon;

import android.service.quicksettings.Tile;

public final class KeepScreenOnTilePresentationFactory {
    private final KeepScreenOnInteractionModel interactionModel = new KeepScreenOnInteractionModel();

    public KeepScreenOnTilePresentation create(
            KeepScreenOnRouteResolution resolution,
            boolean hasWriteSettingsPermission,
            long nowElapsedRealtime
    ) {
        KeepScreenOnState state = resolution.getState();
        if (resolution.getRoute() == KeepScreenOnRoute.STANDARD
                && !hasWriteSettingsPermission
                && !state.isActive()) {
            return new KeepScreenOnTilePresentation(
                    Tile.STATE_INACTIVE,
                    KeepScreenOnTilePresentation.SubtitleMode.PERMISSION_REQUIRED,
                    0L,
                    -1L
            );
        }

        if (!state.isActive()) {
            return new KeepScreenOnTilePresentation(
                    Tile.STATE_INACTIVE,
                    KeepScreenOnTilePresentation.SubtitleMode.DISABLED,
                    0L,
                    -1L
            );
        }

        if (state.isInfinite()) {
            return new KeepScreenOnTilePresentation(
                    Tile.STATE_ACTIVE,
                    KeepScreenOnTilePresentation.SubtitleMode.INFINITE,
                    -1L,
                    -1L
            );
        }

        return new KeepScreenOnTilePresentation(
                Tile.STATE_ACTIVE,
                KeepScreenOnTilePresentation.SubtitleMode.COUNTDOWN,
                interactionModel.remainingSeconds(state, nowElapsedRealtime),
                nextRefreshDelayMs(state, nowElapsedRealtime)
        );
    }

    private long nextRefreshDelayMs(KeepScreenOnState state, long nowElapsedRealtime) {
        long remainingMs = Math.max(0L, state.getExpiresAtElapsedRealtime() - nowElapsedRealtime);
        if (remainingMs <= 0L) {
            return 0L;
        }
        long boundaryDelayMs = remainingMs % 1000L;
        return boundaryDelayMs == 0L ? 1000L : boundaryDelayMs;
    }
}
