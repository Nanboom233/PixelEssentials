package com.nanboom.pixelessentials.wireless;

import com.nanboom.pixelessentials.root.ExecResult;
import com.nanboom.pixelessentials.root.RootAuthorizationUseCase;
import com.nanboom.pixelessentials.root.RootExecutor;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RootAuthorizationUseCaseTest {
    @Test
    public void isAuthorized_returnsTrueForUidZero() throws Exception {
        RootExecutor executor = command -> new ExecResult(true, 0, "uid=0(root)", "");
        RootAuthorizationUseCase useCase = new RootAuthorizationUseCase(executor);

        assertTrue(useCase.isAuthorized(useCase.ensureRootReady()));
    }

    @Test
    public void isAuthorized_returnsFalseForDeniedRoot() throws Exception {
        RootExecutor executor = command -> new ExecResult(false, 1, "", "permission denied");
        RootAuthorizationUseCase useCase = new RootAuthorizationUseCase(executor);

        assertFalse(useCase.isAuthorized(useCase.ensureRootReady()));
    }
}
