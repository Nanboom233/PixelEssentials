# 仓库初始化与远程发布执行计划

## 内部执行等级

- `M`：单 agent 执行，按批次提交并保留验证证据

## 批次规划

### 批次 1：仓库元数据

范围：

- `.gitignore`
- `LICENSE`
- 本轮仓库初始化需求 / 计划 / 总结文档

目标：

- 建立仓库发布所需的最小元数据
- 排除 vibe 运行期文档与本地生成物

### 批次 2：当前产品快照

范围：

- Android 工程源码、资源、脚本、既有需求/设计/总结文档
- 不纳入 `.gradle`、`app/build`、`local.properties`

目标：

- 以当前已验证产品形态入库
- 保留 root 预检、pinned shortcut、QS tile 与测试脚本

## 执行步骤

1. 检查当前目录是否已初始化 git；若未初始化则创建 `main` 分支
2. 写入仓库元数据文件与本轮 vibe 工件
3. 运行现有测试脚本做本轮验证
4. 按批次暂存并生成签名提交
5. 使用 `gh` 创建远程仓库、配置 `origin`、推送 `main`
6. 写入 phase receipt 与 cleanup receipt

## 验证命令

- `./scripts/test-launcher-flow.ps1`
- `git status --short`
- `git log --oneline --show-signature -n 5`
- `gh repo view --json name,nameWithOwner,url,defaultBranchRef`

## 回滚规则

- 若签名提交失败，先保留暂存区并修正 git/OpenPGP 配置后重试
- 若远程仓库创建失败，不销毁本地仓库；保留本地提交并记录失败原因

## 清理要求

- 保留 `.gradle-user-home` 以便后续构建
- 不提交 `outputs/runtime/vibe-sessions/` 下的运行收据
