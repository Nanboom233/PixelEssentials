package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnState {
    private final boolean active;
    private final int durationIndex;
    private final long expiresAtElapsedRealtime;
    private final long lastClickElapsedRealtime;
    private final int originalTimeoutMs;
    private final int appliedTimeoutMs;

    public KeepScreenOnState(
            boolean active,
            int durationIndex,
            long expiresAtElapsedRealtime,
            long lastClickElapsedRealtime
    ) {
        this(active, durationIndex, expiresAtElapsedRealtime, lastClickElapsedRealtime, -1, -1);
    }

    public KeepScreenOnState(
            boolean active,
            int durationIndex,
            long expiresAtElapsedRealtime,
            long lastClickElapsedRealtime,
            int originalTimeoutMs,
            int appliedTimeoutMs
    ) {
        this.active = active;
        this.durationIndex = durationIndex;
        this.expiresAtElapsedRealtime = expiresAtElapsedRealtime;
        this.lastClickElapsedRealtime = lastClickElapsedRealtime;
        this.originalTimeoutMs = originalTimeoutMs;
        this.appliedTimeoutMs = appliedTimeoutMs;
    }

    public static KeepScreenOnState inactive(long nowElapsedRealtime) {
        return new KeepScreenOnState(false, -1, 0L, nowElapsedRealtime, -1, -1);
    }

    public boolean isActive() {
        return active;
    }

    public int getDurationIndex() {
        return durationIndex;
    }

    public long getExpiresAtElapsedRealtime() {
        return expiresAtElapsedRealtime;
    }

    public long getLastClickElapsedRealtime() {
        return lastClickElapsedRealtime;
    }

    public int getOriginalTimeoutMs() {
        return originalTimeoutMs;
    }

    public int getAppliedTimeoutMs() {
        return appliedTimeoutMs;
    }

    public boolean isInfinite() {
        return active && durationIndex == KeepScreenOnInteractionModel.INFINITE_DURATION_INDEX;
    }

    public KeepScreenOnState withTimeouts(int originalTimeoutMs, int appliedTimeoutMs) {
        return new KeepScreenOnState(
                active,
                durationIndex,
                expiresAtElapsedRealtime,
                lastClickElapsedRealtime,
                originalTimeoutMs,
                appliedTimeoutMs
        );
    }
}
