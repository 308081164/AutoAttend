# 快捷运维（Nexus）扩展任务清单：域名、OSS、备案、SMS

> 依据 `docs/快捷运维-功能设计文档.md` §13 候选能力及业务诉求整理。**本清单用于迭代规划**，实现时按租户隔离、最小权限 RAM、审计日志与安全评审执行。

---

## 1. 域名 / DNS

| # | 任务 | 说明 | 依赖 / 风险 |
|---|------|------|-------------|
| 1.1 | 数据模型 | 表：`nexus_dns_domain`（租户、云账号、域名、备注、同步时间）；可选 `nexus_dns_record` 缓存解析记录 | 与云账号 `region`、权限策略对齐 |
| 1.2 | 阿里云适配 | 封装 `alidns`：DescribeDomains、DescribeDomainRecords（只读优先） | RAM 仅开放只读 Action |
| 1.3 | 同步任务 | 按账号/全局间隔拉取域名与记录；失败重试与审计 | 与现有 `NexusSyncService` 调度模式一致 |
| 1.4 | 管理 API | `GET /api/admin/nexus/accounts/{id}/dns/domains`、`.../records?domain=` | 租户配额 |
| 1.5 | 前端 | Nexus 新 Tab「域名」：域名列表、展开解析记录、跳转阿里云控制台 | UX：大量记录分页与搜索 |

---

## 2. OSS（对象存储）

| # | 任务 | 说明 | 依赖 / 风险 |
|---|------|------|-------------|
| 2.1 | 数据模型 | `nexus_oss_bucket`（租户、账号、bucket、地域、创建时间、同步时间） | 与 §13.1 A24 一致 |
| 2.2 | 阿里云适配 | `ListBuckets`、可选 `GetBucketInfo` / 用量概览（若 API 与权限允许） | 只读；注意跨地域 endpoint |
| 2.3 | 同步与 API | 与 DNS 类似：`.../oss/buckets` | 配额与缓存 |
| 2.4 | 前端 | Tab「OSS」：Bucket 列表、地域、链接跳转控制台 | 不做对象列表（避免数据面过大）除非二期立项 |

---

## 3. 备案（ICP）

| # | 任务 | 说明 | 依赖 / 风险 |
|---|------|------|-------------|
| 3.1 | 结论 | **阿里云不提供在第三方平台内代开通/代查询备案的公开稳定 API**；本地登记重复控制台信息价值低 | — |
| 3.2 | 产品形态 | **仅提供跳转**：阿里云备案控制台、备案订单/进度（新窗口打开），由用户登录阿里云操作 | 需与云账号主体一致 |
| 3.3 | ~~数据模型~~ | ~~本地表~~ 已废弃；若历史环境曾建 `aa_nexus_icp_site`，可执行 `schema_nexus_drop_icp_site_migration.sql` 删除 | — |

---

## 4. SMS（短信服务）

| # | 任务 | 说明 | 依赖 / 风险 |
|---|------|------|-------------|
| 4.1 | 边界 | 明确：**非**替换平台已有「管理员/会员短信验证码」链路；此处指 **阿里云短信服务资源可观测**（签名、模板、发送量只读等） | 避免与 `AdminSmsService` 混淆 |
| 4.2 | 阿里云适配 | 查阅 `Dysmsapi` / 控制台 OpenAPI：签名与模板列表、发送统计（以官方为准） | RAM 最小权限 |
| 4.3 | 数据模型 | `nexus_sms_signature`、`nexus_sms_template` 缓存 + 同步时间 | — |
| 4.4 | API & UI | Tab「短信」：签名/模板只读列表、控制台跳转、可选近 24h 发送量（若 API 支持） | 费用与隐私提示 |

---

## 5. 横切事项（四模块共用）

- **租户隔离**：所有表带 `tenant_id`；Controller 使用管理员会话租户。
- **配额**：在 `TenantPlanCatalog` / `TenantResourceQuotaService` 中评估是否计入 Nexus 能力点。
- **审计**：同步失败、控制台跳转、未来写操作均记 `NexusAuditLog`。
- **前端**：在 `NexusConsoleView.vue` 增加与「实例与监控 / 告警 / 成本」同级 Tab 或子路由。

---

## 6. 建议迭代顺序

1. **域名 DNS（只读）** — API 稳定、与运维排障最近。  
2. **OSS Bucket 列表（只读）** — 实现路径与 DNS 类似。  
3. **短信签名/模板只读** — 依赖产品 API 清晰度。  
4. **备案** — **仅控制台跳转**（当前实现），不维护本地备案数据。

---

## 7. 实现状态

- **数据库**：`schema_nexus_extension_dns_oss_sms_icp.sql` 建表 `aa_nexus_dns_domain`、`aa_nexus_dns_record`、`aa_nexus_oss_bucket`、`aa_nexus_sms_signature`、`aa_nexus_sms_template`（**不含** `aa_nexus_icp_site`）。
- **清理旧表**：若曾创建 `aa_nexus_icp_site`，执行 `schema_nexus_drop_icp_site_migration.sql`。
- **同步**：在 `NexusSyncService.syncAccount` 成功后调用 `NexusExtensionSyncService` 拉取 DNS / OSS / 短信元数据；亦可 `POST /api/admin/nexus/accounts/{id}/extension-sync` 单独拉取。
- **API**：`dns/domains`、`dns/records`、`oss/buckets`、`sms/signatures`、`sms/templates`；**无** `icp/sites`。
- **前端**：`NexusConsoleView.vue` Tab：域名 DNS、OSS、**备案（跳转）**、短信。
- **RAM 建议**：云账号需授权云解析只读、OSS 列举、短信查询类 Action（以阿里云控制台策略为准）。

---

## 8. 结合《快捷运维-功能设计文档》§13，现阶段推荐优先做的能力

以下与现有 **ECS + 监控 + 成本 + 快捷入口** 主线契合度高、且多为 **只读**、风险可控：

| 优先级 | 能力 | 理由 |
|--------|------|------|
| **P1** | **安全组规则只读**（§13 A7） | 与 SSH/公网排障强相关；建议先做 Describe，写入端口走二期审批。 |
| **P1** | **EIP 列表与绑定关系只读**（§13 A8） | 与公网 IP、SSH 入口一致；便于核对「这台 ECS 绑了哪个 EIP」。 |
| **P2** | **云盘 / 快照列表只读**（§13 A3～A4） | 日常运维高频；先做列表与容量，创建快照需二次确认。 |
| **P2** | **云监控「系统事件」订阅或事件列表只读**（§13 A13） | 比阈值告警更贴近云平台运维事件；可与现有告警规则互补。 |
| **P3** | **标签 / 资源组只读**（§13 A17） | 利于多客户代维与成本分摊展示。 |

**建议暂缓**：云助手 RunCommand（A1）、OOS（A2）、日志 SLS 一体化（A14）— 权限与治理成本高，适合单独里程碑。

---

*文档版本：2026-04-17*
