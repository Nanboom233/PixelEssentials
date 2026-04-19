package top.nanboom233.pixelessentials.keepscreenon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class KeepScreenOnInteractionModelTest {
    @Test
    public void handleClick_activatesFiveMinuteModeFromInactive() {
        KeepScreenOnInteractionModel model = new KeepScreenOnInteractionModel();

        KeepScreenOnState nextState = model.handleClick(KeepScreenOnState.inactive(10_000L), 20_000L);

        assertTrue(nextState.isActive());
        assertEquals(0, nextState.getDurationIndex());
        assertEquals(320_000L, nextState.getExpiresAtElapsedRealtime());
        assertEquals(20_000L, nextState.getLastClickElapsedRealtime());
    }

    @Test
    public void handleClick_cyclesToNextDurationInsideClickWindow() {
        KeepScreenOnInteractionModel model = new KeepScreenOnInteractionModel();
        KeepScreenOnState currentState = new KeepScreenOnState(true, 0, 320_000L, 18_000L);

        KeepScreenOnState nextState = model.handleClick(currentState, 20_000L);

        assertTrue(nextState.isActive());
        assertEquals(1, nextState.getDurationIndex());
        assertEquals(620_000L, nextState.getExpiresAtElapsedRealtime());
    }

    @Test
    public void handleClick_turnsOffAfterInfiniteCycle() {
        KeepScreenOnInteractionModel model = new KeepScreenOnInteractionModel();
        KeepScreenOnState currentState = new KeepScreenOnState(true, 3, -1L, 19_500L);

        KeepScreenOnState nextState = model.handleClick(currentState, 20_000L);

        assertFalse(nextState.isActive());
    }

    @Test
    public void handleLongPress_entersInfiniteMode() {
        KeepScreenOnInteractionModel model = new KeepScreenOnInteractionModel();

        KeepScreenOnState nextState = model.handleLongPress(20_000L);

        assertTrue(nextState.isActive());
        assertEquals(KeepScreenOnInteractionModel.INFINITE_DURATION_INDEX, nextState.getDurationIndex());
        assertEquals(-1L, nextState.getExpiresAtElapsedRealtime());
    }

    @Test
    public void normalize_expiresTimedOutState() {
        KeepScreenOnInteractionModel model = new KeepScreenOnInteractionModel();
        KeepScreenOnState currentState = new KeepScreenOnState(true, 1, 19_999L, 18_000L);

        KeepScreenOnState nextState = model.normalize(currentState, 20_000L);

        assertFalse(nextState.isActive());
    }
}
