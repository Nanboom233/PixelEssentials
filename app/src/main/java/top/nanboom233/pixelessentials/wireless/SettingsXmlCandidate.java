package top.nanboom233.pixelessentials.wireless;

final class SettingsXmlCandidate {
    private final String sourceXmlName;
    private final String key;
    private final String fragmentClassName;
    private final int signalScore;
    private final boolean toggleEntryKey;

    SettingsXmlCandidate(
            String sourceXmlName,
            String key,
            String fragmentClassName,
            int signalScore,
            boolean toggleEntryKey
    ) {
        this.sourceXmlName = sourceXmlName;
        this.key = key;
        this.fragmentClassName = fragmentClassName;
        this.signalScore = signalScore;
        this.toggleEntryKey = toggleEntryKey;
    }

    String getSourceXmlName() {
        return sourceXmlName;
    }

    String getKey() {
        return key;
    }

    String getFragmentClassName() {
        return fragmentClassName;
    }

    int getSignalScore() {
        return signalScore;
    }

    boolean isToggleEntryKey() {
        return toggleEntryKey;
    }
}
