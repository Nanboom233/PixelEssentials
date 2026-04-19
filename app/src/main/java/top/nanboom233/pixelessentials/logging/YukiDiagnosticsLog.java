package top.nanboom233.pixelessentials.logging;

import androidx.annotation.Nullable;

import com.highcapable.yukihookapi.hook.log.YLog;

public final class YukiDiagnosticsLog {
    private YukiDiagnosticsLog() {
    }

    public static void debug(String tag, Object message) {
        YLog.INSTANCE.debug(message, null, tag, YLog.EnvType.SCOPE);
    }

    public static void info(String tag, Object message) {
        YLog.INSTANCE.info(message, null, tag, YLog.EnvType.SCOPE);
    }

    public static void warn(String tag, Object message) {
        YLog.INSTANCE.warn(message, null, tag, YLog.EnvType.SCOPE);
    }

    public static void warn(String tag, Object message, @Nullable Throwable throwable) {
        YLog.INSTANCE.warn(message, throwable, tag, YLog.EnvType.SCOPE);
    }

    public static void error(String tag, Object message) {
        YLog.INSTANCE.error(message, null, tag, YLog.EnvType.SCOPE);
    }

    public static void error(String tag, Object message, @Nullable Throwable throwable) {
        YLog.INSTANCE.error(message, throwable, tag, YLog.EnvType.SCOPE);
    }
}
