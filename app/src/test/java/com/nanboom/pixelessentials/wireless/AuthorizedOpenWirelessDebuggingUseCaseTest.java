package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.RootAuthorizationUseCase;
import com.nanboom.pixelessentials.root.RootExecutor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AuthorizedOpenWirelessDebuggingUseCaseTest {
    @Test
    public void open_returnsAuthFailureWhenRootDenied() throws Exception {
        RootAuthorizationUseCase auth = new RootAuthorizationUseCase(command -> new ExecResult(false, 1, "", "denied"));
        OpenWirelessDebuggingUseCase open = new OpenWirelessDebuggingUseCase(
                new WirelessDebuggingCommandProvider(),
                command -> new ExecResult(true, 0, "should not run", "")
        );

        ExecResult result = new AuthorizedOpenWirelessDebuggingUseCase(auth, open).open();
        assertFalse(result.isSuccess());
        assertEquals("denied", result.getStderr());
    }

    @Test
    public void open_runsOpenCommandAfterSuccessfulRootAuth() throws Exception {
        RootExecutor authExecutor = command -> new ExecResult(true, 0, "uid=0(root)", "");
        RootAuthorizationUseCase auth = new RootAuthorizationUseCase(authExecutor);
        OpenWirelessDebuggingUseCase open = new OpenWirelessDebuggingUseCase(
                new WirelessDebuggingCommandProvider(),
                command -> new ExecResult(command.contains("AdbWirelessDebuggingFragment"), 0, "ok", "")
        );

        ExecResult result = new AuthorizedOpenWirelessDebuggingUseCase(auth, open).open();
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getStdout());
    }
}
