package top.nanboom233.pixelessentials.wireless;

import top.nanboom233.pixelessentials.root.ExecResult;
import top.nanboom233.pixelessentials.root.RootAuthorizationUseCase;
import top.nanboom233.pixelessentials.root.RootExecutor;

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
