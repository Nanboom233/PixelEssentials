package top.nanboom233.pixelessentials.wireless;

import java.util.Objects;

public final class SettingsLaunchSpec {
    private final String hostActivityClassName;
    private final String fragmentClassName;
    private final String fragmentArgsKey;
    private final String entryXmlName;
    private final String targetPageXmlName;
    private final int confidence;
    private final long settingsVersionCode;
    private final long settingsLastUpdateTime;
    private final int discoverySchemaVersion;

    public SettingsLaunchSpec(
            String hostActivityClassName,
            String fragmentClassName,
            String fragmentArgsKey,
            String entryXmlName,
            String targetPageXmlName,
            int confidence,
            long settingsVersionCode,
            long settingsLastUpdateTime,
            int discoverySchemaVersion
    ) {
        this.hostActivityClassName = hostActivityClassName;
        this.fragmentClassName = fragmentClassName;
        this.fragmentArgsKey = fragmentArgsKey;
        this.entryXmlName = entryXmlName;
        this.targetPageXmlName = targetPageXmlName;
        this.confidence = confidence;
        this.settingsVersionCode = settingsVersionCode;
        this.settingsLastUpdateTime = settingsLastUpdateTime;
        this.discoverySchemaVersion = discoverySchemaVersion;
    }

    public String getHostActivityClassName() {
        return hostActivityClassName;
    }

    public String getFragmentClassName() {
        return fragmentClassName;
    }

    public String getFragmentArgsKey() {
        return fragmentArgsKey;
    }

    public String getEntryXmlName() {
        return entryXmlName;
    }

    public String getTargetPageXmlName() {
        return targetPageXmlName;
    }

    public int getConfidence() {
        return confidence;
    }

    public long getSettingsVersionCode() {
        return settingsVersionCode;
    }

    public long getSettingsLastUpdateTime() {
        return settingsLastUpdateTime;
    }

    public int getDiscoverySchemaVersion() {
        return discoverySchemaVersion;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SettingsLaunchSpec)) {
            return false;
        }
        SettingsLaunchSpec that = (SettingsLaunchSpec) other;
        return confidence == that.confidence
                && settingsVersionCode == that.settingsVersionCode
                && settingsLastUpdateTime == that.settingsLastUpdateTime
                && discoverySchemaVersion == that.discoverySchemaVersion
                && Objects.equals(hostActivityClassName, that.hostActivityClassName)
                && Objects.equals(fragmentClassName, that.fragmentClassName)
                && Objects.equals(fragmentArgsKey, that.fragmentArgsKey)
                && Objects.equals(entryXmlName, that.entryXmlName)
                && Objects.equals(targetPageXmlName, that.targetPageXmlName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                hostActivityClassName,
                fragmentClassName,
                fragmentArgsKey,
                entryXmlName,
                targetPageXmlName,
                confidence,
                settingsVersionCode,
                settingsLastUpdateTime,
                discoverySchemaVersion
        );
    }

    @Override
    public String toString() {
        return "SettingsLaunchSpec{"
                + "hostActivityClassName='" + hostActivityClassName + '\''
                + ", fragmentClassName='" + fragmentClassName + '\''
                + ", fragmentArgsKey='" + fragmentArgsKey + '\''
                + ", entryXmlName='" + entryXmlName + '\''
                + ", targetPageXmlName='" + targetPageXmlName + '\''
                + ", confidence=" + confidence
                + ", settingsVersionCode=" + settingsVersionCode
                + ", settingsLastUpdateTime=" + settingsLastUpdateTime
                + ", discoverySchemaVersion=" + discoverySchemaVersion
                + '}';
    }
}
