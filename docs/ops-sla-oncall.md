# 运维 SLA 与 On-Call（MVP / 人工流程）

本文档描述 **阶段 3 监测后台** 上线后的初版约定：以人工响应为主，后续可接入告警与值班轮换系统。

## 服务范围

- **监测 API**：`/api/platform/*`（需平台运维账号登录，见 `application.properties` 中平台密码配置说明）。
- **租户报表**：`GET /api/platform/ops/reports/tenants` — 租户列表、成员数、GitHub 绑定数、近 24h/7d 提交量、近 24h 活跃邮箱数（日活近似）、diff 存储汇总近似。

## SLA（建议目标，非合同承诺）

| 场景 | 目标 |
|------|------|
| 生产不可用（全站 5xx 持续） | 工作时间内 **4 小时内** 响应并尝试恢复；非工作时间 **下一工作日优先处理** |
| 单租户功能异常 | **1 个工作日内** 回复进展 |
| 数据问题（提交/diff 缺失） | 先查租户 ID、仓库名与 Webhook 投递记录，按个案处理 |

实际 SLA 以团队与客户合同为准；上表仅供内部对齐。

## On-Call（人工）

1. **入口**：告警渠道（邮件/IM，后续可接 Prometheus/钉钉）→ 当值人员。
2. **处理步骤**：
   - 确认影响面（全站 / 单租户）。
   - 查应用日志与数据库只读报表（`ops/reports/tenants`）。
   - 若为 Webhook 问题，核对 `docs/github-webhook-tenant.md` 中 URL、slug、GitHub 投递历史。
3. **升级**：超过约定响应时间仍无结论 → 升级至技术负责人。

## 改进 backlog

- 将 `ops/reports/tenants` 扩展为可视化仪表盘与趋势图。
- 接入统一告警与排班（PagerDuty、阿里云等）。
- 明确「暂停租户」`status=suspended` 时的对外话术与恢复流程。
