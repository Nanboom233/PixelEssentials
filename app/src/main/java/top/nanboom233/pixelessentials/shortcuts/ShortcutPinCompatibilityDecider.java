package top.nanboom233.pixelessentials.shortcuts;

public final class ShortcutPinCompatibilityDecider {
    public ShortcutPinAliasAction decide(String shortcutId, String activityClassName) {
        if (!ShortcutTokenStore.SHORTCUT_ID.equals(shortcutId)) {
            return ShortcutPinAliasAction.KEEP_ENABLED;
        }
        if (activityClassName == null || activityClassName.isEmpty()) {
            return ShortcutPinAliasAction.KEEP_ENABLED;
        }
        if (ShortcutTokenStore.PINNED_SHORTCUT_PROXY_ACTIVITY_NAME.equals(activityClassName)) {
            return ShortcutPinAliasAction.DISABLE;
        }
        if (ShortcutTokenStore.LAUNCHER_ALIAS_NAME.equals(activityClassName)) {
            return ShortcutPinAliasAction.DISABLE_UNTIL_USED;
        }
        return ShortcutPinAliasAction.KEEP_ENABLED;
    }
}
