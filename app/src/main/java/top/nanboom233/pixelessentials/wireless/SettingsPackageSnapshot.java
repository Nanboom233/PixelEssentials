package top.nanboom233.pixelessentials.wireless;

public final class SettingsPackageSnapshot {
    private final String sourceDir;
    private final long versionCode;
    private final long lastUpdateTime;

    public SettingsPackageSnapshot(String sourceDir, long versionCode, long lastUpdateTime) {
        this.sourceDir = sourceDir;
        this.versionCode = versionCode;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public long getVersionCode() {
        return versionCode;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
}
