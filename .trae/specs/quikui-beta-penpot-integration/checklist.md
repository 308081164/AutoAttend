# QuikUI Beta - Penpot 集成调通检查清单

## 分支管理

- [ ] quikUI_beta 分支已从 main 创建
- [ ] 所有未暂存的更改已提交
- [ ] 分支状态正常

## Docker 环境

- [ ] Docker Desktop 已运行
- [ ] docker-compose.penpot.yml 配置验证通过
- [ ] Penpot 前端容器运行正常
- [ ] Penpot 后端容器运行正常
- [ ] Penpot PostgreSQL 容器运行正常
- [ ] Penpot Exporter 容器运行正常
- [ ] 端口 9001 可从宿主机访问
- [ ] Penpot Web UI 可正常打开

## 后端连通性

- [ ] PENPOT_ENABLED=true 配置生效
- [ ] PENPOT_INTERNAL_URI 配置正确
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

- [ ] 无编译错误
- [ ] 无运行时异常
- [ ] 日志输出正常
- [ ] 敏感信息未泄露

## Git 提交

- [ ] 关键里程碑已提交：
  - [ ] 分支创建提交
  - [ ] Docker 环境验证提交
  - [ ] 后端连通性诊断提交
  - [ ] 端到端功能验证提交
  - [ ] 最终问题修复提交

## 文档

- [ ] README 或相关文档已更新（如需要）
- [ ] 部署说明已完善（如需要）
