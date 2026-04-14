# 需求文档：无线调试直达图标 App

## 目标

构建一个 Android 应用。应用本体不保留长期 launcher 入口；首次通过 launcher 入口启动时，先做 root 权限预检，再通过官方 pinned shortcut API 申请桌面直达图标。后续用户点击该桌面直达图标时，应用通过 root 执行已验证命令，直接打开系统“无线调试”界面。

## 技术约束

- 技术栈：Gradle + Java
- Android SDK 路径：`D:/Android SDK`
- 本机 Java：OpenJDK 21

## 功能范围

### 首版必须包含

- 一个临时 launcher 入口（用于首启 bootstrap）
- pinned shortcut 申请流程
- 一个无界面或透明分发 Activity
- 一个统一的 root 执行入口
- 一个 root 预检链路
- 一个统一的无线调试命令提供层
- 一个防滥用门禁（不可被外部无 token 触发）

### 首版不包含

- app 内按钮
- 设置页
- 历史记录
- Quick Settings tile
- Widget
- 通知入口

## 目标行为

首次点击 launcher 入口后：

1. 进入分发 Activity
2. Activity 先调用 root 预检
3. root 可用后申请 pinned shortcut
4. pinned shortcut 创建成功后隐藏 launcher 入口

之后点击桌面 pinned shortcut：

1. 进入分发 Activity
2. Activity 校验内部 token
3. token 通过后调用 root 执行层
4. 直接打开无线调试界面
5. Activity 立即退出，不在最近任务中保留

目标命令：

```sh
su -c 'am start -n com.android.settings/.SubSettings --es ":settings:show_fragment" "com.android.settings.development.AdbWirelessDebuggingFragment"'
```

## 验收标准

- 设计阶段给出 2-3 种实现方案并推荐其一
- 方案需说明组件清单、调用链、优缺点、后续扩展性
- 在用户批准设计前不进入实现
