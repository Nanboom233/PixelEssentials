package top.nanboom233.pixelessentials.keepscreenon;

public final class KeepScreenOnTilePresentation {
    public enum SubtitleMode {
        PERMISSION_REQUIRED,
        DISABLED,
        INFINITE,
        COUNTDOWN
    }

    private final int tileState;
    private final SubtitleMode subtitleMode;
    private final long remainingSeconds;
    private final long nextRefreshDelayMs;

    public KeepScreenOnTilePresentation(
            int tileState,
            SubtitleMode subtitleMode,
            long remainingSeconds,
            long nextRefreshDelayMs
    ) {
        this.tileState = tileState;
        this.subtitleMode = subtitleMode;
        this.remainingSeconds = remainingSeconds;
        this.nextRefreshDelayMs = nextRefreshDelayMs;
    }

    public int getTileState() {
        return tileState;
    }

    public SubtitleMode getSubtitleMode() {
        return subtitleMode;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public long getNextRefreshDelayMs() {
        return nextRefreshDelayMs;
    }
}
