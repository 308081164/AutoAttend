# QuikUI Beta - Penpot 集成调通规范

## 为什么
用户需要将现有的 Penpot Beta 功能彻底调通，确保在本地环境中能够完整运行快原型的 Penpot 集成功能，包括后端 RPC 调用、Docker 部署、以及前后端联调。

## 什么变化

- **分支创建**：从 main 创建 quikUI_beta 分支用于开发
- **环境验证**：验证 Docker Compose Penpot 栈能否正常启动
- **连通性诊断**：确认后端到 Penpot 的 RPC 调用链路
- **功能调通**：确保完整的生成链路（Prompt → AI → Penpot 文件）可用
- **版本控制**：在关键里程碑处进行本地 commit

## 影响

- **影响的规格**：
  - 快原型 Penpot Beta 功能（docs/快原型-Penpot-Beta-功能设计.md）
  - Docker 部署配置（docker-compose.penpot.yml）
  - 后端 Penpot 服务层（prototype/penpot/）
  - 前端集成（PrototypeProjectView.vue）

- **影响的代码**：
  - `atuo_attend_backend/src/main/java/org/example/atuo_attend_backend/prototype/penpot/` - Penpot 核心服务
  - `atuo_attend_backend/src/main/java/org/example/atuo_attend_backend/prototype/config/` - 配置类
  - `atuo_attend_backend/src/main/java/org/example/atuo_attend_backend/prototype/controller/` - REST 端点
  - `docker-compose.penpot.yml` - Penpot Docker 栈
  - `auto_attend_forntend/src/views/PrototypeProjectView.vue` - 前端界面

## 新增需求

### 需求：Penpot Docker 栈验证
系统必须在本地能够通过 Docker Compose 成功启动完整的 Penpot 服务栈

#### 场景：Docker 启动验证
- **当** 执行 `docker compose up -d` 包含 penpot 相关服务
- **那么** 所有 Penpot 容器应处于 running 状态，端口 9001 可访问

### 需求：后端 Penpot RPC 连通性
后端必须能够成功调用 Penpot RPC API（create-file、update-file 等）

#### 场景：RPC 调用验证
- **当** 调用诊断端点 `/admin/ui-prototype/penpot/diagnose`
- **那么** 应返回配置检查、网络连通性、RPC 路径探测等全部通过

### 需求：完整的 Penpot 生成链路
从用户输入 Prompt 到生成 Penpot 文件的完整流程必须可用

#### 场景：生成链路验证
- **当** 用户在前端填写需求并点击"生成 Penpot 初稿"
- **那么** 后端应完成：AI 规划布局 → 创建文件 → 写入内容 → 返回预览链接

### 需求：每租户独立账号（可选验证）
如配置了租户自动开户，应能验证每租户的独立 Penpot 账号和加密 Token

## 修改需求

### 需求：Penpot 诊断端点增强
现有诊断端点应能够逐步检查配置、网络、RPC 路径、租户凭证等

#### 场景：诊断端点检查项
- 功能启用状态
- 配置完整性（脱敏）
- 网络连通性（前端 + 后端直连）
- RPC 路径探测（自动识别 2.12+ 或旧版）
- 租户凭证有效性

## 移除需求

无

## 调通步骤

1. **分支创建**
   - 从 main 创建 quikUI_beta 分支
   - 提交当前未暂存的更改

2. **环境准备**
   - 检查 Docker Desktop 运行状态
   - 确认 docker-compose.penpot.yml 配置正确
   - 准备 .env 配置文件

3. **Docker 栈验证**
   - 启动 Penpot 相关容器
   - 验证容器健康状态
   - 检查端口 9001 可访问性

4. **后端连通性诊断**
   - 调用诊断端点
   - 逐步排查问题
   - 修复发现的配置或代码问题

5. **端到端功能验证**
   - 从前端发起 Penpot 生成请求
   - 验证完整的生成链路
   - 确保预览链接可用

6. **清理与文档**
   - 确保代码质量
   - 更新必要的文档
   - 里程碑提交
