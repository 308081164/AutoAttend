# QuikUI Beta - Penpot 集成调通任务清单

## 任务 1：创建 quikUI_beta 分支

- [x] 1.1 提交当前未暂存的更改到 main 分支
- [x] 1.2 从 main 创建 quikUI_beta 分支
- [x] 1.3 切换到 quikUI_beta 分支
- [x] 1.4 验证分支状态

## 任务 2：环境准备与 Docker 栈验证

- [x] 2.1 检查 Docker Desktop 运行状态 ✅ Docker version 29.3.1 已安装
- [x] 2.2 创建/更新 .env 文件配置 Penpot 环境变量 ✅ 已创建完整配置
- [x] 2.3 验证 docker-compose.penpot.yml 配置 ✅ 配置正确
- [ ] 2.4 启动 Penpot Docker 栈 ⚠️ 需要手动执行 docker compose up -d
- [ ] 2.5 验证所有 Penpot 容器运行状态 ⚠️ 待启动后验证
- [ ] 2.6 检查端口 9001 可访问性 ⚠️ 待启动后验证
- [ ] 2.7 提交：Penpot Docker 栈环境验证完成

## 任务 3：后端 Penpot 连通性诊断与修复

- [ ] 3.1 调用诊断端点 `/admin/ui-prototype/penpot/diagnose`
- [ ] 3.2 检查功能启用状态
- [ ] 3.3 验证配置完整性
- [ ] 3.4 测试前端连通性
- [ ] 3.5 测试后端直连接通
- [ ] 3.6 验证 RPC 路径探测
- [ ] 3.7 检查租户凭证（如已配置）
- [ ] 3.8 修复发现的问题（如有）
- [ ] 3.9 提交：后端连通性诊断完成

## 任务 4：端到端 Penpot 生成链路验证

- [ ] 4.1 启动完整应用栈（backend + frontend）
- [ ] 4.2 登录管理端
- [ ] 4.3 进入快原型项目
- [ ] 4.4 切换到 Penpot Beta 标签页
- [ ] 4.5 填写需求 Prompt
- [ ] 4.6 点击"生成 Penpot 初稿"
- [ ] 4.7 验证 AI 规划阶段
- [ ] 4.8 验证文件创建阶段
- [ ] 4.9 验证内容写入阶段
- [ ] 4.10 验证预览链接生成
- [ ] 4.11 测试外链打开 Penpot 工作区
- [ ] 4.12 测试 .penpot 文件导出（如可用）
- [ ] 4.13 提交：端到端功能验证完成

## 任务 5：问题排查与修复

- [ ] 5.1 记录所有发现的问题
- [ ] 5.2 修复 Docker 网络问题（如有）
- [ ] 5.3 修复配置问题（如有）
- [ ] 5.4 修复后端 RPC 调用问题（如有）
- [ ] 5.5 修复前端交互问题（如有）
- [ ] 5.6 提交：问题修复完成

## 任务 6：最终验证与文档

- [ ] 6.1 完整流程再次验证
- [ ] 6.2 更新 README 或相关文档（如需要）
- [ ] 6.3 最终提交到 quikUI_beta 分支
- [ ] 6.4 验证分支已正确创建并包含所有更改

## 任务依赖关系

- 任务 2 依赖任务 1 完成 ✅
- 任务 3 依赖任务 2 完成（待完成启动）
- 任务 4 依赖任务 2 和 3 完成
- 任务 5 与任务 3、4 并行进行
- 任务 6 依赖任务 4 和 5 完成

## 验证标准

每项任务完成后应满足：
- 相关容器处于 running 状态
- 诊断端点返回成功状态
- 端到端流程无错误完成
- 预览链接可用
- Git commit 已创建

## 当前状态

✅ 已完成：
- quikui_beta 分支创建
- .env 配置文件创建
- 所有代码和文档提交

⚠️ 待完成：
- 需要手动启动 Docker 栈
- 需要验证容器运行
- 需要端到端功能测试

## 下一步操作

### 启动 Penpot Docker 栈

在项目根目录执行：

```bash
cd d:\Hui_Files\MyProjects\guangsha_technology\AutoAttend
docker compose up -d
```

这将启动：
- penpot-frontend (端口 9001)
- penpot-backend
- penpot-postgres
- penpot-valkey
- penpot-exporter
- penpot-mailcatch (端口 1080)
- autoattend-mysql
- autoattend-minio
- autoattend-backend (端口 8848)
- autoattend-frontend (端口 8849)
- autoattend-platform-monitor-frontend (端口 8850)
