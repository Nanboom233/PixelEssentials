# 需求文档：仓库初始化与远程发布

## 目标

把当前 `PixelEssentials` Android/Java/Gradle 项目整理为可发布仓库：

- 建立本地 git 仓库并按逻辑批次提交
- 为仓库补齐忽略规则与 MIT License
- 排除 vibe 运行期工作文档目录，避免把运行收据纳入版本历史
- 使用 `gh` 创建 GitHub 远程仓库并推送现状

## 约束

- 所有 `git` 与 `gh` 命令都需要提权执行
- 不回滚、不覆盖现有工作区内容
- 跳过 `gpg` 二进制直接校验，改为依赖现有 git OpenPGP 配置
- 保持当前 Android 项目可构建

## 验收标准

- 根目录新增 `.gitignore`，至少排除：
  - `outputs/runtime/vibe-sessions/`
  - Gradle 构建产物与本机缓存
  - `local.properties`
- 根目录新增准确 MIT License 文本
- 当前项目文件按至少两个逻辑批次完成提交
- 提交阶段使用签名提交路径
- `gh` 成功创建远程仓库并完成首次推送
- 本轮若有仓库文件改动，输出一份总结性 Markdown 文档

## 非目标

- 不新增 Android 业务功能
- 不重写既有需求/设计文档内容
- 不做高风险设备侧“关闭无线调试”实机验证
