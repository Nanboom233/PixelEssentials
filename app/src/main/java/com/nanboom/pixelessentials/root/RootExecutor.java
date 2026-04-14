package com.nanboom.pixelessentials.root;

import java.io.IOException;

public interface RootExecutor {
    ExecResult execute(String command) throws IOException, InterruptedException;
}
