# 无线调试直达图标 App 设计文档

## 目标

创建一个最小 Android 应用。应用不提供任何 app 内界面。首次通过临时 launcher 入口启动时，先做 root 权限预检并申请 pinned shortcut；pinned shortcut 创建成功后隐藏 launcher 入口。此后用户只通过 pinned shortcut 进入应用，应用再通过 root 执行已验证命令打开系统“无线调试”界面。

## 设计结论

本次采用 **bootstrap + pinned shortcut + token 门禁 + 透明 EntryActivity**。

原因：

- 组件最少，最符合“只有一个图标”的目标
- 不需要引入 service、复杂状态或额外页面
- 便于先完成首版闭环，再考虑后续扩展

## 架构

调用链：

```text
Temporary launcher alias
  -> ShortcutEntryActivity (bootstrap mode)
  -> RootAuthorizationUseCase
  -> ShortcutManager.requestPinShortcut(...)
  -> ShortcutPinResultReceiver disables launcher alias

Pinned shortcut
  -> ShortcutEntryActivity (dispatch mode)
  -> LaunchModeResolver + token check
  -> OpenWirelessDebuggingUseCase
  -> RootExecutor
  -> WirelessDebuggingCommandProvider
  -> su -c am start ...
  -> com.android.settings/.SubSettings
```

### 组件

#### `ShortcutEntryActivity`

唯一的透明执行 Activity。

职责：

- bootstrap 模式下做 root 预检并申请 pinned shortcut
- dispatch 模式下校验 token 并触发 use case
- 不渲染业务 UI
- 执行完成后立即退出

约束：

- 透明主题
- `android:noHistory="true"`
- `android:excludeFromRecents="true"`

#### `RootAuthorizationUseCase`

root 预检层。

职责：

- 首启时执行 `id`
- 确认 `uid=0`
- 未通过时不创建 shortcut

#### `LaunchModeResolver`

启动模式裁决层。

职责：

- 区分 bootstrap / dispatch / ignore
- 只有合法 token 的 dispatch 才能触发 root 打开 Settings

#### `ShortcutTokenStore`

门禁令牌存储层。

职责：

- 首次创建 pinned shortcut 时生成随机 token
- 后续 dispatch 时校验 token
- 防止外部应用显式调用 exported Activity 触发 root 链路

#### `ShortcutPinResultReceiver`

pinned shortcut 创建结果回调。

职责：

- pinned shortcut 创建成功后隐藏 launcher alias

#### `OpenWirelessDebuggingUseCase`

统一的业务入口。

职责：

- 从 `WirelessDebuggingCommandProvider` 取得命令
- 交给 `RootExecutor` 执行
- 返回结构化结果给 Activity

#### `WirelessDebuggingCommandProvider`

命令集中管理层。

职责：

- 维护当前已验证命令
- 为后续 ROM / Android 版本差异留扩展点

首版命令：

```sh
am start -n com.android.settings/.SubSettings --es ":settings:show_fragment" "com.android.settings.development.AdbWirelessDebuggingFragment"
```

#### `RootExecutor`

root 执行层。

首版职责：

- 通过 `su -c` 执行命令
- 收集退出码、stdout、stderr

实现策略：

- 首版：`ProcessBuilder("su", "-c", command)`
- 后续可升级为 `libsu`

## 数据流

首次 bootstrap：

1. 用户点击临时 launcher 图标
2. `ShortcutEntryActivity` 进入 bootstrap 模式
3. `RootAuthorizationUseCase` 执行 `id`
4. root 通过后调用 `requestPinShortcut`
5. 回调 `ShortcutPinResultReceiver`
6. 隐藏 launcher alias

后续 dispatch：

1. 用户点击 pinned shortcut
2. `ShortcutEntryActivity` 校验 token
3. token 通过后调用 `OpenWirelessDebuggingUseCase`
4. Use case 获取命令并调用 `RootExecutor`
5. `RootExecutor` 运行 `su -c <command>`
6. 系统拉起无线调试界面
7. Activity 退出

## 错误处理

首版只处理必要错误：

- `su` 不存在
- root 授权被拒绝
- `am start` 返回非 0
- 目标 Activity/fragment 失效
- shortcut 不支持 pinned API
- 无 token 的外部显式调用

失败时：

- 不进入 app 内页面
- 允许使用 Toast 输出简短错误
- 记录日志到 Logcat

## 测试与验证

### 单元层

- `WirelessDebuggingCommandProvider` 返回命令是否正确
- `OpenWirelessDebuggingUseCase` 是否正确传播执行结果

### 手工验证

- 安装 app
- 真实 launcher 点击是否触发 root 预检
- root 通过后是否请求 pinned shortcut
- 合法 token 是否能打开无线调试界面
- 无 token 的显式调用是否被忽略
- pinned shortcut 成功后 launcher 入口是否被隐藏

## 默认假设

- 包名/namespace 默认采用 `com.nanboom.pixelessentials`
- 使用 Java + AndroidX
- compileSdk 使用本机已存在的 `36`
- minSdk 先取兼容性较好的 `26`

## 后续扩展位

本设计故意保留以下扩展点，但首版不实现：

- `TileService`
- Widget
- 通知 action
- 多入口 `activity-alias`
