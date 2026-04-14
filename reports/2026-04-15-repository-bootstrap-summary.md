# 仓库初始化与发布总结

## 本轮目标

为当前 `PixelEssentials` 项目补齐仓库发布基础设施，并把现有产品快照按逻辑批次纳入版本管理。

## 本轮改动

- 新增 `.gitignore`，排除 Gradle 构建产物、本地 SDK 配置与 vibe 运行收据
- 新增 `LICENSE`，使用标准 MIT License 文本
- 新增本轮仓库初始化需求与执行计划文档
- 复用并加固 `scripts/test-launcher-flow.ps1`，固定 `GRADLE_USER_HOME`

## 提交策略

1. 仓库元数据
2. 当前 Android 产品快照

## 验证策略

- 复用 `scripts/test-launcher-flow.ps1`
- 提交前确认工作区分组
- 远程创建后检查 `origin` 与默认分支

## 注意事项

- `local.properties` 仅保留本地，不纳入版本库
- `outputs/runtime/vibe-sessions/` 仅保留本地运行证据
- 本轮不追加 Android 业务功能修改
