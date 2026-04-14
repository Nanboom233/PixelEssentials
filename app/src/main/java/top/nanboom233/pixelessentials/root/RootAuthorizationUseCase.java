package top.nanboom233.pixelessentials.root;

import java.io.IOException;

public final class RootAuthorizationUseCase {
    private final RootExecutor rootExecutor;

    public RootAuthorizationUseCase(RootExecutor rootExecutor) {
        this.rootExecutor = rootExecutor;
    }

    public ExecResult ensureRootReady() throws IOException, InterruptedException {
        return rootExecutor.execute("id");
    }

    public boolean isAuthorized(ExecResult result) {
        return result.isSuccess() && result.getStdout() != null && result.getStdout().contains("uid=0");
    }
}
