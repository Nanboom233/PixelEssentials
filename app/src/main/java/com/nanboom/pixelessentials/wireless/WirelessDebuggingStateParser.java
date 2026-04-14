package com.nanboom.pixelessentials.wireless;

public final class WirelessDebuggingStateParser {
    public boolean parseEnabled(String stdout) {
        if (stdout == null) {
            return false;
        }

        String normalized = stdout.trim();
        return "1".equals(normalized) || "true".equalsIgnoreCase(normalized);
    }
}
