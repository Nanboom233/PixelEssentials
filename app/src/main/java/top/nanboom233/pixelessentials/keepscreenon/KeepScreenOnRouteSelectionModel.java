package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnRouteSelectionModel {
    public static final long STATUS_CALLBACK_TTL_MS = 65_000L;

    public boolean shouldUseXposed(
            KeepScreenOnState xposedState,
            KeepScreenOnXposedRouteSnapshot snapshot,
            boolean isModuleActive,
            long nowElapsedRealtime
    ) {
        return isModuleActive
                || xposedState.isActive()
                || snapshot.hasFreshStatusCallback(nowElapsedRealtime, STATUS_CALLBACK_TTL_MS);
    }

    public boolean shouldUseXposedAfterStatusRequest(
            KeepScreenOnXposedRouteSnapshot snapshot,
            KeepScreenOnState xposedState,
            boolean isModuleActive,
            long requestedAtElapsedRealtime,
            long nowElapsedRealtime
    ) {
        return isModuleActive
                || snapshot.hasStatusCallbackSince(requestedAtElapsedRealtime)
                || (xposedState.isActive()
                && snapshot.hasFreshStatusCallback(nowElapsedRealtime, STATUS_CALLBACK_TTL_MS));
    }
}
