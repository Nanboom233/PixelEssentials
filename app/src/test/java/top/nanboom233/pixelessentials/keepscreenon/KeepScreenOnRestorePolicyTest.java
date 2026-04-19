package top.nanboom233.pixelessentials.keepscreenon;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class KeepScreenOnRestorePolicyTest {
    @Test
    public void shouldRestoreTimeout_whenCurrentTimeoutMatchesAppliedTimeout() {
        KeepScreenOnRestorePolicy policy = new KeepScreenOnRestorePolicy();
        KeepScreenOnState state = new KeepScreenOnState(true, 0, 120_000L, 10_000L, 30_000, Integer.MAX_VALUE);

        assertTrue(policy.shouldRestoreTimeout(state, Integer.MAX_VALUE));
    }

    @Test
    public void shouldNotRestoreTimeout_whenCurrentTimeoutWasChangedExternally() {
        KeepScreenOnRestorePolicy policy = new KeepScreenOnRestorePolicy();
        KeepScreenOnState state = new KeepScreenOnState(true, 0, 120_000L, 10_000L, 30_000, Integer.MAX_VALUE);

        assertFalse(policy.shouldRestoreTimeout(state, 60_000));
    }
}
