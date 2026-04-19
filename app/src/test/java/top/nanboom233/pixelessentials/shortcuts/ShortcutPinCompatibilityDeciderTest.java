package top.nanboom233.pixelessentials.shortcuts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ShortcutPinCompatibilityDeciderTest {
    @Test
    public void decide_returnsDisableUntilUsed_whenShortcutTargetsLauncherAlias() {
        ShortcutPinCompatibilityDecider decider = new ShortcutPinCompatibilityDecider();

        ShortcutPinAliasAction action = decider.decide(
                ShortcutTokenStore.SHORTCUT_ID,
                ShortcutTokenStore.LAUNCHER_ALIAS_NAME
        );

        assertEquals(ShortcutPinAliasAction.DISABLE_UNTIL_USED, action);
    }

    @Test
    public void decide_returnsDisable_whenShortcutTargetsNonBootstrapActivity() {
        ShortcutPinCompatibilityDecider decider = new ShortcutPinCompatibilityDecider();

        ShortcutPinAliasAction action = decider.decide(
                ShortcutTokenStore.SHORTCUT_ID,
                ShortcutTokenStore.PINNED_SHORTCUT_PROXY_ACTIVITY_NAME
        );

        assertEquals(ShortcutPinAliasAction.DISABLE, action);
    }

    @Test
    public void decide_returnsKeepEnabled_whenShortcutActivityIsUnknown() {
        ShortcutPinCompatibilityDecider decider = new ShortcutPinCompatibilityDecider();

        ShortcutPinAliasAction action = decider.decide(
                ShortcutTokenStore.SHORTCUT_ID,
                null
        );

        assertEquals(ShortcutPinAliasAction.KEEP_ENABLED, action);
    }

    @Test
    public void decide_returnsKeepEnabled_whenShortcutTargetsUnexpectedActivity() {
        ShortcutPinCompatibilityDecider decider = new ShortcutPinCompatibilityDecider();

        ShortcutPinAliasAction action = decider.decide(
                ShortcutTokenStore.SHORTCUT_ID,
                "top.nanboom233.pixelessentials.UnexpectedActivity"
        );

        assertEquals(ShortcutPinAliasAction.KEEP_ENABLED, action);
    }
}
