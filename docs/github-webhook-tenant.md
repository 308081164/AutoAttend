# GitHub Webhook：按租户（组织 slug）配置

## 推荐 URL（无需 `X-Tenant-Id`）

在 GitHub 仓库 **Settings → Webhooks → Add webhook** 中，将 **Payload URL** 配置为：

```text
https://<你的后端域名>/api/webhooks/github/<组织 slug>
```

其中 `<组织 slug>` 与管理后台注册组织时填写的 **slug** 一致（小写字母、数字、连字符，与 `aa_tenant.slug` 对应）。

示例：组织 slug 为 `acme-corp` 时：

```text
https://api.example.com/api/webhooks/github/acme-corp
```

- **Content type**：`application/json`
- **Secret**：与报价项目开通 GitHub 时在仓库上配置的 Webhook secret 保持一致（若应用侧有校验）。
- **Events**：至少勾选 **Just the push event**（当前后端仅处理 `push`）。

若 slug 在系统中不存在，接口返回 **HTTP 404**，不会写入默认租户。

## 兼容旧版 URL

仍可使用：

```text
https://<你的后端域名>/api/webhooks/github
```

并额外设置请求头 **`X-Tenant-Id`** 为数字租户 ID（与 `aa_tenant.id` 一致）。未设置时行为与历史一致，默认为租户 `1`。

## 反向代理与超时

若经 Nginx 转发，需保证 Webhook 请求体与头不被剥离；大仓库 push 可能触发较长时间处理，可参考仓库内 `docs/nginx-api-timeout-snippet.conf` 适当调大 `proxy_read_timeout`（按运维策略调整）。
