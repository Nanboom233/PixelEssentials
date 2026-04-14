# Quick Settings Tile 扩展总结

## 本轮目标

在现有「root 预检 + pinned shortcut + token 门禁」链路之上，新增 Quick Settings tile 入口，并把它改成真正的无线调试开关；同时支持长按直接打开无线调试界面，并按指定语义重做 shortcut / tile 图标。

## 代码改动

### 新增

- `app/src/main/java/com/nanboom/pixelessentials/WirelessDebuggingTileService.java`
- `app/src/main/java/com/nanboom/pixelessentials/TilePreferencesActivity.java`
- `app/src/main/java/com/nanboom/pixelessentials/wireless/AuthorizedOpenWirelessDebuggingUseCase.java`
- `app/src/main/java/com/nanboom/pixelessentials/wireless/ReadWirelessDebuggingStateUseCase.java`
- `app/src/main/java/com/nanboom/pixelessentials/wireless/ToggleWirelessDebuggingUseCase.java`
- `app/src/main/java/com/nanboom/pixelessentials/wireless/WirelessDebuggingState.java`
- `app/src/main/java/com/nanboom/pixelessentials/wireless/WirelessDebuggingStateParser.java`
- `app/src/test/java/com/nanboom/pixelessentials/wireless/AuthorizedOpenWirelessDebuggingUseCaseTest.java`
- `app/src/test/java/com/nanboom/pixelessentials/wireless/ToggleWirelessDebuggingUseCaseTest.java`
- `app/src/test/java/com/nanboom/pixelessentials/wireless/WirelessDebuggingStateParserTest.java`

### 修改

- `app/src/main/AndroidManifest.xml`
  - 新增 `WirelessDebuggingTileService`
  - 新增受 `BIND_QUICK_SETTINGS_TILE` 保护的 `TilePreferencesActivity`
- `app/src/main/java/com/nanboom/pixelessentials/ShortcutEntryActivity.java`
  - 快捷图标 dispatch 也改走 `AuthorizedOpenWirelessDebuggingUseCase`
- `app/src/main/res/values/strings.xml`
  - 新增 tile 开/关/不可用文案
- `app/src/main/res/drawable/ic_tile_wireless_debugging.xml`
  - 新增更贴切的 tile 图标
- `app/src/main/res/drawable/ic_shortcut_wireless_debugging.xml`
- `app/src/main/res/drawable/ic_shortcut_wireless_debugging_foreground.xml`
  - 新增 pinned shortcut 图标
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - 恢复 app 图标前景为原通用样式

## 安全链路

### 快捷图标

- bootstrap：临时 launcher alias -> root 预检 -> 申请 pinned shortcut
- dispatch：合法 token -> root 预检 -> 打开无线调试页

### Quick Settings tile

- 系统通过 `TileService` 绑定
- tile 刷新时读取 `adb_wifi_enabled`
- tile 点击时切换 `adb_wifi_enabled`
- tile 长按时走 `QS_TILE_PREFERENCES`
- 长按入口受 `android.permission.BIND_QUICK_SETTINGS_TILE` 保护

### 开关行为

- 状态读取：`settings get global adb_wifi_enabled`
- 开启：`settings put global adb_wifi_enabled 1`
- 关闭：`settings put global adb_wifi_enabled 0`
- tile UI 根据读取结果切到 `STATE_ACTIVE / STATE_INACTIVE / STATE_UNAVAILABLE`

## 测试

### 自动测试

- `AuthorizedOpenWirelessDebuggingUseCaseTest`
- `ToggleWirelessDebuggingUseCaseTest`
- `WirelessDebuggingStateParserTest`
- 原有 command provider / launch mode / root auth 测试继续通过

### 构建

- `testDebugUnitTest`：通过
- `assembleDebug`：通过
- `installDebug`：通过

## 设备端验证

### QS tile 状态读取证据

```text
QS tile state refreshed. authorized=true, enabled=true, error=
```

### QS tile 长按打开界面证据

```text
QS tile preferences requested open. exitCode=0
Activity: com.android.settings/.SubSettings
```

### 权限保护证据

普通 shell 直接启动 `QS_TILE_PREFERENCES` 被系统拒绝：

```text
Permission Denial ... requires android.permission.BIND_QUICK_SETTINGS_TILE
```

### 已知运行边界

本轮**没有直接在设备上做“从开到关”的实测点击**，原因是当前会话依赖无线 adb，若 tile 真正把 `adb_wifi_enabled` 从 `1` 切到 `0`，将高度可能直接切断当前调试连接。\n\n因此本轮运行验证采取了更安全的组合：\n- 自动测试覆盖 toggle 逻辑\n- 设备上验证状态读取\n- 设备上验证长按打开界面\n- 设备上验证 preferences 入口的权限保护

### 旧版单击打开界面日志（上一轮）

```text
QS tile launched wireless debugging. exitCode=0
Activity: com.android.settings/.SubSettings
```

### 系统侧证据

```text
topResumedActivity=ActivityRecord{... com.android.settings/.SubSettings ...}
ResumedActivity: ActivityRecord{... com.android.settings/.SubSettings ...}
```

## 结论

Quick Settings tile 已落地，并且：

- 成为真正的无线调试开关
- 能通过长按打开无线调试界面
- 读取/切换都走 root 预检后的链路
- 长按入口受系统权限保护
- app 图标已恢复原样
- shortcut / tile 图标已更新为“bug 主体 + 右下角溢出 wifi”语义
