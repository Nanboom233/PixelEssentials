package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnRestorePolicy {
    public static final int APPLIED_TIMEOUT_MS = Integer.MAX_VALUE;

    public int resolveAppliedTimeoutMs(int currentTimeoutMs) {
        return Math.max(currentTimeoutMs, APPLIED_TIMEOUT_MS);
    }

    public boolean shouldRestoreTimeout(KeepScreenOnState state, int currentTimeoutMs) {
        return state.isActive()
                && state.getOriginalTimeoutMs() >= 0
                && state.getAppliedTimeoutMs() >= 0
                && currentTimeoutMs == state.getAppliedTimeoutMs();
    }
}

