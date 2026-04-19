package top.nanboom233.pixelessentials.root;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RootAuthorizationUseCaseTest {
    @Test
    public void isAuthorized_returnsTrue_whenStdoutContainsRootUid() {
        RootAuthorizationUseCase useCase = new RootAuthorizationUseCase(command -> new ExecResult(true, 0, "", ""));

        boolean authorized = useCase.isAuthorized(new ExecResult(true, 0, "uid=0(root) gid=0(root)", ""));

        assertTrue(authorized);
    }

    @Test
    public void isAuthorized_returnsFalse_whenCommandFails() {
        RootAuthorizationUseCase useCase = new RootAuthorizationUseCase(command -> new ExecResult(true, 0, "", ""));

        boolean authorized = useCase.isAuthorized(new ExecResult(false, 1, "", "permission denied"));

        assertFalse(authorized);
    }
}
