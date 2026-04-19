package top.nanboom233.pixelessentials.root;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProcessRootExecutor implements RootExecutor {
    private static final int UNKNOWN_EXIT_CODE = -1;
    private static final int MAX_CAPTURE_BYTES = 64 * 1024;
    private static final long PROCESS_TIMEOUT_MS = 10_000L;
    private static final long STREAM_JOIN_TIMEOUT_MS = 1_000L;
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "id",
            "settings get global adb_wifi_enabled",
            "settings put global adb_wifi_enabled 1",
            "settings put global adb_wifi_enabled 0",
            "logcat -c",
            "logcat -d -t 200 -v brief -s Settings:V SettingsActivity:V SubSettings:V"
    );
    private static final Pattern SETTINGS_OPEN_COMMAND_PATTERN = Pattern.compile(
            "^am start -W -f 0x10000000 -n (com\\.android\\.settings/[A-Za-z0-9_.$]+) "
                    + "--es \":settings:show_fragment\" \"([A-Za-z0-9_.$]+)\""
                    + "(?: --es \":settings:fragment_args_key\" \"([A-Za-z0-9_.$-]+)\")?$"
    );

    @Override
    public ExecResult execute(String command) throws IOException, InterruptedException {
        if (!isAllowedCommand(command)) {
            return new ExecResult(
                    false,
                    UNKNOWN_EXIT_CODE,
                    "",
                    "Rejected non-whitelisted root command"
            );
        }

        Process process = new ProcessBuilder("su", "-c", command).start();
        StreamCollector stdoutCollector = new StreamCollector(process.getInputStream());
        StreamCollector stderrCollector = new StreamCollector(process.getErrorStream());
        Thread stdoutThread = new Thread(stdoutCollector, "process-root-executor-stdout");
        Thread stderrThread = new Thread(stderrCollector, "process-root-executor-stderr");
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        try {
            boolean finished = process.waitFor(PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                joinCollector(stdoutThread);
                joinCollector(stderrThread);
                return new ExecResult(
                        false,
                        UNKNOWN_EXIT_CODE,
                        stdoutCollector.getCapturedText(),
                        mergeErrorMessage("Root command timed out", stderrCollector.getCapturedText())
                );
            }

            joinCollector(stdoutThread);
            joinCollector(stderrThread);
            IOException collectorFailure = stdoutCollector.getFailure();
            if (collectorFailure == null) {
                collectorFailure = stderrCollector.getFailure();
            }
            if (collectorFailure != null) {
                throw collectorFailure;
            }

            int exitCode = process.exitValue();
            return new ExecResult(
                    exitCode == 0,
                    exitCode,
                    stdoutCollector.getCapturedText(),
                    stderrCollector.getCapturedText()
            );
        } finally {
            process.destroy();
        }
    }

    static boolean isAllowedCommand(String command) {
        if (ALLOWED_COMMANDS.contains(command)) {
            return true;
        }

        Matcher matcher = SETTINGS_OPEN_COMMAND_PATTERN.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        String hostActivityClassName = matcher.group(1);
        if (!isAllowedSettingsHostActivity(hostActivityClassName)) {
            return false;
        }

        String fragmentClassName = matcher.group(2);
        if (!isAllowedWirelessDebuggingFragment(fragmentClassName)) {
            return false;
        }

        String fragmentArgsKey = matcher.group(3);
        return fragmentArgsKey == null || isSafeSettingsKey(fragmentArgsKey);
    }

    private static boolean isAllowedSettingsHostActivity(String hostActivityClassName) {
        return hostActivityClassName != null
                && hostActivityClassName.startsWith("com.android.settings/");
    }

    private static boolean isAllowedWirelessDebuggingFragment(String fragmentClassName) {
        String normalized = fragmentClassName.toLowerCase(Locale.ROOT);
        return normalized.startsWith("com.android.settings.")
                && (normalized.contains("wirelessdebuggingfragment")
                || normalized.contains("adbwirelessdebuggingfragment"));
    }

    private static boolean isSafeSettingsKey(String key) {
        return key != null && key.matches("[A-Za-z0-9_.$-]+");
    }

    private static void joinCollector(Thread thread) throws InterruptedException {
        thread.join(STREAM_JOIN_TIMEOUT_MS);
        if (thread.isAlive()) {
            thread.interrupt();
        }
    }

    private static String mergeErrorMessage(String primaryMessage, String capturedStderr) {
        if (capturedStderr == null || capturedStderr.isBlank()) {
            return primaryMessage;
        }
        return primaryMessage + "\n" + capturedStderr;
    }

    private static final class StreamCollector implements Runnable {
        private final InputStream inputStream;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private IOException failure;
        private boolean truncated;

        private StreamCollector(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int read;
            try {
                while ((read = inputStream.read(buffer)) != -1) {
                    int remaining = MAX_CAPTURE_BYTES - outputStream.size();
                    if (remaining > 0) {
                        outputStream.write(buffer, 0, Math.min(read, remaining));
                    }
                    if (read > remaining) {
                        truncated = true;
                    }
                }
            } catch (IOException exception) {
                failure = exception;
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    if (failure == null) {
                        failure = ignored;
                    }
                }
            }
        }

        private IOException getFailure() {
            return failure;
        }

        private String getCapturedText() {
            String captured = outputStream.toString(StandardCharsets.UTF_8);
            if (!truncated) {
                return captured;
            }
            if (captured.isEmpty()) {
                return "[truncated]";
            }
            return captured + "\n[truncated]";
        }
    }
}
