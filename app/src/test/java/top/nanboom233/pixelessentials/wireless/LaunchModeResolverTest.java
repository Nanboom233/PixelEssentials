package top.nanboom233.pixelessentials.wireless;

import top.nanboom233.pixelessentials.ShortcutEntryActivity;
import top.nanboom233.pixelessentials.shortcuts.LaunchMode;
import top.nanboom233.pixelessentials.shortcuts.LaunchModeResolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LaunchModeResolverTest {
    @Test
    public void resolve_returnsDispatchWhenTokenMatches() {
        LaunchModeResolver resolver = new LaunchModeResolver();
        LaunchMode mode = resolver.resolve(
                ShortcutEntryActivity.ACTION_OPEN_WIRELESS_DEBUGGING,
                "token",
                "token",
                false
        );

        assertEquals(LaunchMode.DISPATCH, mode);
    }

    @Test
    public void resolve_returnsBootstrapForMainActionWhenAliasEnabled() {
        LaunchModeResolver resolver = new LaunchModeResolver();
        LaunchMode mode = resolver.resolve("android.intent.action.MAIN", null, "token", true);

        assertEquals(LaunchMode.BOOTSTRAP, mode);
    }

    @Test
    public void resolve_returnsIgnoreForUntrustedLaunch() {
        LaunchModeResolver resolver = new LaunchModeResolver();
        LaunchMode mode = resolver.resolve("android.intent.action.MAIN", null, "token", false);

        assertEquals(LaunchMode.IGNORE, mode);
    }
}
