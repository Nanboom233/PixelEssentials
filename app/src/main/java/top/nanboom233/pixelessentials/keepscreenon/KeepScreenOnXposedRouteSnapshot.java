package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnXposedRouteSnapshot {
    private final KeepScreenOnState state;
    private final long lastStatusCallbackElapsedRealtime;

    public KeepScreenOnXposedRouteSnapshot(
            KeepScreenOnState state,
            long lastStatusCallbackElapsedRealtime
    ) {
        this.state = state;
        this.lastStatusCallbackElapsedRealtime = lastStatusCallbackElapsedRealtime;
    }

    public static KeepScreenOnXposedRouteSnapshot empty(long nowElapsedRealtime) {
        return new KeepScreenOnXposedRouteSnapshot(
                KeepScreenOnState.inactive(nowElapsedRealtime),
                0L
        );
    }

    public KeepScreenOnState getState() {
        return state;
    }

    public long getLastStatusCallbackElapsedRealtime() {
        return lastStatusCallbackElapsedRealtime;
    }

    public boolean hasStatusCallbackSince(long elapsedRealtime) {
        return lastStatusCallbackElapsedRealtime >= elapsedRealtime;
    }

    public boolean hasFreshStatusCallback(long nowElapsedRealtime, long ttlMs) {
        return lastStatusCallbackElapsedRealtime > 0L
                && nowElapsedRealtime - lastStatusCallbackElapsedRealtime <= ttlMs;
    }
}
