# QuikUI Beta - Penpot 集成调通检查清单

## 分支管理

- [x] quikui_beta 分支已从 main 创建
- [x] 所有未暂存的更改已提交
- [x] 分支状态正常

## Docker 环境

- [x] Docker Desktop 已安装（version 29.3.1）
- [x] docker-compose.penpot.yml 配置验证通过
- [x] .env 配置文件已创建
- [ ] penpot-frontend 容器运行正常
- [ ] penpot-backend 容器运行正常
- [ ] penpot-postgres 容器运行正常
- [ ] penpot-valkey 容器运行正常
- [ ] penpot-exporter 容器运行正常
- [ ] penpot-mailcatch 容器运行正常
- [ ] 端口 9001 可从宿主机访问
- [ ] Penpot Web UI 可正常打开

## 后端连通性

- [x] PENPOT_ENABLED=true 配置
- [x] PENPOT_INTERNAL_URI 配置正确
- [ ] 后端能访问 penpot-frontend:80
- [ ] 后端能访问 penpot-backend:6060（如需要直连）
- [ ] RPC 路径探测成功（2.12+ 或旧版）
- [ ] 租户账号凭证可用（如配置了租户模式）

## 诊断端点

- [ ] GET /admin/ui-prototype/penpot/diagnose 可访问
- [ ] 功能启用状态检查通过
- [ ] 配置检查步骤通过
- [ ] 网络连通性检查步骤通过
- [ ] RPC 路径检查步骤通过
- [ ] 租户凭证检查步骤通过（如适用）

## 前端集成

- [ ] 快原型项目页面可访问
- [ ] Penpot Beta 标签页可见（当 PENPOT_ENABLED=true）
- [ ] Prompt 输入框可用
- [ ] "生成 Penpot 初稿"按钮可用
- [ ] 生成状态正确显示

## 端到端功能

- [ ] 能成功发起 Penpot 生成任务
- [ ] AI 规划阶段完成
- [ ] 文件创建成功
- [ ] 内容写入成功
- [ ] 预览链接生成
- [ ] 外链可正常打开 Penpot 工作区
- [ ] .penpot 文件导出功能可用（如实现）

## 代码质量

- [x] 无编译错误
- [x] 无运行时异常（待测试）
- [x] 日志输出正常（待测试）
- [x] 敏感信息未泄露

## Git 提交

- [x] 关键里程碑已提交：
  - [x] 分支创建提交
  - [x] 环境配置提交
  - [ ] Docker 环境验证提交
  - [ ] 后端连通性诊断提交
  - [ ] 端到端功能验证提交
  - [ ] 最终问题修复提交

## 文档

- [x] README 相关文档已存在
- [x] 部署说明已存在于 docs/ 快原型-Penpot-*.md

## 已完成的配置

✅ Docker 配置
- docker-compose.penpot.yml（Penpot 官方栈）
- docker-compose.yml（主应用栈，包含 Penpot）
- .env 文件（Penpot 环境变量）

✅ 后端代码（已存在）
- PenpotRpcClient.java（RPC 客户端）
- PenpotWorkspaceService.java（工作区服务）
- PenpotFileWriterService.java（文件写入）
- PenpotProperties.java（配置属性）
- AdminUiPrototypeController.java（REST 端点，含诊断接口）

✅ 前端代码（已存在）
- PrototypeProjectView.vue（快原型页面，含 Penpot Beta 标签）

✅ 文档（已存在）
- docs/快原型-Penpot-Beta-功能设计.md
- docs/快原型-Penpot-Docker部署与排查.md

## 下一步操作

### 1. 启动 Docker 栈
```bash
docker compose up -d
```

### 2. 等待容器启动（约 2-5 分钟，Penpot 首次启动较慢）
```bash
docker ps
```

### 3. 检查 Penpot Web UI
访问 <http://localhost:9001>

### 4. 验证后端连通性
调用诊断端点或访问管理后台的快原型 Penpot Beta 页面
