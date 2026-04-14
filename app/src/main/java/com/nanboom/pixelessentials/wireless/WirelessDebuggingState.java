package com.nanboom.pixelessentials.wireless;

public final class WirelessDebuggingState {
    private final boolean authorized;
    private final boolean enabled;
    private final String errorMessage;

    public WirelessDebuggingState(boolean authorized, boolean enabled, String errorMessage) {
        this.authorized = authorized;
        this.enabled = enabled;
        this.errorMessage = errorMessage;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
