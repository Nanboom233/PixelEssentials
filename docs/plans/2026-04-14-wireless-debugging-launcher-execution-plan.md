# 执行计划：无线调试直达图标 App

## 内部执行等级

M：单 agent 设计与实现任务。

## 阶段

### Phase 1：设计冻结

- 基于空项目目录按 greenfield 方案推进
- 提出 2-3 个实现方案
- 明确推荐方案
- 等待用户批准

### Phase 2：项目初始化

- 初始化 Gradle + Java Android 项目
- 配置 SDK / wrapper / 基础 manifest

### Phase 3：核心实现

- 实现无界面 DispatchActivity
- 实现 RootExecutor
- 实现 CommandProvider
- 接通 launcher 图标入口

### Phase 4：验证

- 生成测试/验证脚本（如有代码改动）
- 构建项目
- 运行必要验证

### Phase 5：清理

- 写入 phase 收据
- 记录构建/运行结果
- 输出 cleanup receipt

## 关键约束

- 设计获批前不编码
- 不添加 app 内 UI
- 首版只保留桌面直达图标

## 回滚规则

- 若设计审批未通过，则仅保留文档工件
- 不在未经批准时创建超出首版范围的功能
