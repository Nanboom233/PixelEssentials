package top.nanboom233.pixelessentials.root;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ProcessRootExecutor implements RootExecutor {
    @Override
    public ExecResult execute(String command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("su", "-c", command).start();
        String stdout = readFully(process.getInputStream());
        String stderr = readFully(process.getErrorStream());
        int exitCode = process.waitFor();
        return new ExecResult(exitCode == 0, exitCode, stdout, stderr);
    }

    private String readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
