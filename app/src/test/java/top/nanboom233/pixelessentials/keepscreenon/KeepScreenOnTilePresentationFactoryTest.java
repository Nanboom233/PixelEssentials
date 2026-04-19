package top.nanboom233.pixelessentials.keepscreenon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class KeepScreenOnTilePresentationFactoryTest {
    @Test
    public void create_returnsPermissionRequiredInactiveWhenStandardRouteLacksPermission() {
        KeepScreenOnTilePresentationFactory factory = new KeepScreenOnTilePresentationFactory();
        KeepScreenOnRouteResolution resolution = new KeepScreenOnRouteResolution(
                KeepScreenOnRoute.STANDARD,
                KeepScreenOnState.inactive(10_000L)
        );

        KeepScreenOnTilePresentation presentation = factory.create(resolution, false, 10_000L);

        assertEquals(android.service.quicksettings.Tile.STATE_INACTIVE, presentation.getTileState());
        assertEquals(KeepScreenOnTilePresentation.SubtitleMode.PERMISSION_REQUIRED, presentation.getSubtitleMode());
        assertEquals(-1L, presentation.getNextRefreshDelayMs());
    }

    @Test
    public void create_returnsCountdownForActiveTimedState() {
        KeepScreenOnTilePresentationFactory factory = new KeepScreenOnTilePresentationFactory();
        KeepScreenOnRouteResolution resolution = new KeepScreenOnRouteResolution(
                KeepScreenOnRoute.XPOSED,
                new KeepScreenOnState(true, 0, 309_750L, 10_000L)
        );

        KeepScreenOnTilePresentation presentation = factory.create(resolution, true, 10_000L);

        assertEquals(KeepScreenOnTilePresentation.SubtitleMode.COUNTDOWN, presentation.getSubtitleMode());
        assertEquals(300L, presentation.getRemainingSeconds());
        assertEquals(750L, presentation.getNextRefreshDelayMs());
    }

    @Test
    public void create_returnsInfiniteWithoutNextRefresh() {
        KeepScreenOnTilePresentationFactory factory = new KeepScreenOnTilePresentationFactory();
        KeepScreenOnRouteResolution resolution = new KeepScreenOnRouteResolution(
                KeepScreenOnRoute.XPOSED,
                new KeepScreenOnState(true, KeepScreenOnInteractionModel.INFINITE_DURATION_INDEX, -1L, 10_000L)
        );

        KeepScreenOnTilePresentation presentation = factory.create(resolution, true, 10_000L);

        assertEquals(KeepScreenOnTilePresentation.SubtitleMode.INFINITE, presentation.getSubtitleMode());
        assertEquals(-1L, presentation.getNextRefreshDelayMs());
    }
}
