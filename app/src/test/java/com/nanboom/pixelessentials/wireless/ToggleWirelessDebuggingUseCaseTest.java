package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.RootAuthorizationUseCase;
import com.nanboom.pixelessentials.root.RootExecutor;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ToggleWirelessDebuggingUseCaseTest {
    @Test
    public void toggle_flipsOffStateToOn() throws Exception {
        AtomicInteger state = new AtomicInteger(0);
        RootExecutor executor = command -> {
            if ("id".equals(command)) {
                return new ExecResult(true, 0, "uid=0(root)", "");
            }
            if (command.contains("settings get global adb_wifi_enabled")) {
                return new ExecResult(true, 0, Integer.toString(state.get()), "");
            }
            if (command.contains("settings put global adb_wifi_enabled 1")) {
                state.set(1);
                return new ExecResult(true, 0, "", "");
            }
            if (command.contains("settings put global adb_wifi_enabled 0")) {
                state.set(0);
                return new ExecResult(true, 0, "", "");
            }
            return new ExecResult(false, 1, "", "unexpected");
        };

        ToggleWirelessDebuggingUseCase useCase = new ToggleWirelessDebuggingUseCase(
                new RootAuthorizationUseCase(executor),
                executor,
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser()
        );

        WirelessDebuggingState result = useCase.toggle();
        assertTrue(result.isAuthorized());
        assertTrue(result.isEnabled());
    }

    @Test
    public void toggle_returnsUnauthorizedWhenRootDenied() throws Exception {
        RootExecutor executor = command -> new ExecResult(false, 1, "", "denied");
        ToggleWirelessDebuggingUseCase useCase = new ToggleWirelessDebuggingUseCase(
                new RootAuthorizationUseCase(executor),
                executor,
                new WirelessDebuggingCommandProvider(),
                new WirelessDebuggingStateParser()
        );

        WirelessDebuggingState result = useCase.toggle();
        assertFalse(result.isAuthorized());
    }
}
