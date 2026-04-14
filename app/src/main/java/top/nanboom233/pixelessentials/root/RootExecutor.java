package top.nanboom233.pixelessentials.root;

import java.io.IOException;

public interface RootExecutor {
    ExecResult execute(String command) throws IOException, InterruptedException;
}
