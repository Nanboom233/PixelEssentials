package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.ShortcutEntryActivity;
import com.nanboom.pixelessentials.shortcuts.LaunchMode;
import com.nanboom.pixelessentials.shortcuts.LaunchModeResolver;

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
