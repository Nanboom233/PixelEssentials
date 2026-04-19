package top.nanboom233.pixelessentials.keepscreenon;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class KeepScreenOnRouteSelectionModelTest {
    @Test
    public void shouldUseXposed_whenStatusCallbackIsFresh() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnXposedRouteSnapshot snapshot = new KeepScreenOnXposedRouteSnapshot(
                KeepScreenOnState.inactive(10_000L),
                19_500L
        );

        boolean usesXposed = model.shouldUseXposed(
                KeepScreenOnState.inactive(20_000L),
                snapshot,
                false,
                20_000L
        );

        assertTrue(usesXposed);
    }

    @Test
    public void shouldUseXposed_whenActiveStateAndStaleCallback_returnsTrue() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnXposedRouteSnapshot snapshot = new KeepScreenOnXposedRouteSnapshot(
                new KeepScreenOnState(true, 0, 300_000L, 10_000L),
                18_000L
        );

        boolean usesXposed = model.shouldUseXposed(
                new KeepScreenOnState(true, 0, 300_000L, 10_000L),
                snapshot,
                false,
                20_000L
        );

        assertTrue(usesXposed);
    }

    @Test
    public void shouldUseXposed_whenInactiveAndStatusCallbackIsStale_returnsFalse() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnXposedRouteSnapshot snapshot = new KeepScreenOnXposedRouteSnapshot(
                KeepScreenOnState.inactive(10_000L),
                10_000L
        );

        boolean usesXposed = model.shouldUseXposed(
                KeepScreenOnState.inactive(80_000L),
                snapshot,
                false,
                80_000L
        );

        assertFalse(usesXposed);
    }

    @Test
    public void shouldUseXposed_whenModuleActive_alwaysReturnsTrue() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnXposedRouteSnapshot snapshot = new KeepScreenOnXposedRouteSnapshot(
                KeepScreenOnState.inactive(10_000L),
                10_000L
        );

        boolean usesXposed = model.shouldUseXposed(
                KeepScreenOnState.inactive(80_000L),
                snapshot,
                true,
                80_000L
        );

        assertTrue(usesXposed);
    }

    @Test
    public void shouldUseXposedAfterStatusRequest_requiresCallbackAfterCurrentRequest() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnXposedRouteSnapshot staleSnapshot = new KeepScreenOnXposedRouteSnapshot(
                KeepScreenOnState.inactive(10_000L),
                10_000L
        );
        KeepScreenOnXposedRouteSnapshot freshSnapshot = new KeepScreenOnXposedRouteSnapshot(
                new KeepScreenOnState(true, 0, 300_000L, 10_000L),
                10_350L
        );

        assertFalse(model.shouldUseXposedAfterStatusRequest(
                staleSnapshot,
                KeepScreenOnState.inactive(10_500L),
                false,
                10_100L,
                10_500L
        ));
        assertTrue(model.shouldUseXposedAfterStatusRequest(
                freshSnapshot,
                freshSnapshot.getState(),
                false,
                10_100L,
                10_500L
        ));
    }

    @Test
    public void shouldUseXposedAfterStatusRequest_whenActiveStateIsStillFresh_returnsTrue() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnState activeState = new KeepScreenOnState(true, 0, 300_000L, 10_000L);
        KeepScreenOnXposedRouteSnapshot snapshot = new KeepScreenOnXposedRouteSnapshot(
                activeState,
                19_000L
        );

        assertTrue(model.shouldUseXposedAfterStatusRequest(
                snapshot,
                activeState,
                false,
                20_100L,
                20_200L
        ));
    }

    @Test
    public void shouldUseXposedAfterStatusRequest_whenActiveStateIsStale_returnsFalse() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnState activeState = new KeepScreenOnState(true, 0, 300_000L, 10_000L);
        KeepScreenOnXposedRouteSnapshot snapshot = new KeepScreenOnXposedRouteSnapshot(
                activeState,
                10_000L
        );

        assertFalse(model.shouldUseXposedAfterStatusRequest(
                snapshot,
                activeState,
                false,
                80_100L,
                80_500L
        ));
    }

    @Test
    public void shouldUseXposedAfterStatusRequest_whenModuleActive_alwaysReturnsTrue() {
        KeepScreenOnRouteSelectionModel model = new KeepScreenOnRouteSelectionModel();
        KeepScreenOnXposedRouteSnapshot staleSnapshot = new KeepScreenOnXposedRouteSnapshot(
                KeepScreenOnState.inactive(10_000L),
                10_000L
        );

        assertTrue(model.shouldUseXposedAfterStatusRequest(
                staleSnapshot,
                KeepScreenOnState.inactive(80_000L),
                true,
                80_100L,
                80_500L
        ));
    }
}
