package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnInteractionModel {
    public static final long CLICK_WINDOW_MS = 5000L;
    public static final int[] DURATIONS_SECONDS = new int[] {
            5 * 60,
            10 * 60,
            30 * 60,
            -1,
    };
    public static final int INFINITE_DURATION_INDEX = DURATIONS_SECONDS.length - 1;

    public KeepScreenOnState handleClick(KeepScreenOnState currentState, long nowElapsedRealtime) {
        KeepScreenOnState normalized = normalize(currentState, nowElapsedRealtime);
        if (normalized.isActive()
                && normalized.getLastClickElapsedRealtime() > 0
                && nowElapsedRealtime - normalized.getLastClickElapsedRealtime() < CLICK_WINDOW_MS) {
            int nextDurationIndex = normalized.getDurationIndex() + 1;
            if (nextDurationIndex >= DURATIONS_SECONDS.length) {
                return KeepScreenOnState.inactive(nowElapsedRealtime);
            }
            return activeState(nextDurationIndex, nowElapsedRealtime);
        }

        if (normalized.isActive()) {
            return KeepScreenOnState.inactive(nowElapsedRealtime);
        }

        return activeState(0, nowElapsedRealtime);
    }

    public KeepScreenOnState handleLongPress(long nowElapsedRealtime) {
        return activeState(INFINITE_DURATION_INDEX, nowElapsedRealtime);
    }

    public KeepScreenOnState normalize(KeepScreenOnState state, long nowElapsedRealtime) {
        if (!state.isActive()) {
            return KeepScreenOnState.inactive(nowElapsedRealtime);
        }

        if (state.isInfinite()) {
            return state;
        }

        if (state.getExpiresAtElapsedRealtime() <= nowElapsedRealtime) {
            return KeepScreenOnState.inactive(nowElapsedRealtime);
        }

        return state;
    }

    public long remainingSeconds(KeepScreenOnState state, long nowElapsedRealtime) {
        if (!state.isActive()) {
            return 0L;
        }

        if (state.isInfinite()) {
            return -1L;
        }

        long remainingMs = Math.max(0L, state.getExpiresAtElapsedRealtime() - nowElapsedRealtime);
        return (remainingMs + 999L) / 1000L;
    }

    private KeepScreenOnState activeState(int durationIndex, long nowElapsedRealtime) {
        long expiresAtElapsedRealtime = durationIndex == INFINITE_DURATION_INDEX
                ? -1L
                : nowElapsedRealtime + (DURATIONS_SECONDS[durationIndex] * 1000L);
        return new KeepScreenOnState(true, durationIndex, expiresAtElapsedRealtime, nowElapsedRealtime);
    }
}
