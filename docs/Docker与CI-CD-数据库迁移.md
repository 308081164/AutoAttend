# Docker 与 CI/CD 数据库迁移说明

本文说明在 **Docker Compose** 与 **GitHub Actions CI/CD** 下，数据库如何初始化、如何**在每次部署时自动执行迁移**，以及如何新增迁移脚本而无需手动上机操作。

---

## 1. 建表脚本 vs 迁移脚本

| 类型 | 目录/命名 | 执行时机 | 用途 |
|------|-----------|----------|------|
| **建表脚本** | `db/schema_*.sql`（如 `schema_mysql.sql`、`schema_collab_mysql.sql`、`schema_ai_mysql.sql`） | 仅 **MySQL 首次初始化**（数据卷为空时，`docker-entrypoint-initdb.d` 只跑一次） | 新环境一次性建库、建表 |
| **迁移脚本** | `db/*migration*.sql`（如 `schema_collab_team_migration.sql`、`schema_collab_attachment_project_id_migration.sql`） | **每次 CI/CD 部署时** 对已有库执行 | 已有库升级：加列、加表、改结构，无需手动上机 |

- **新部署**：只用建表脚本即可（通过 Compose 挂载到 `docker-entrypoint-initdb.d`）。
- **已有库升级**：建表脚本不会再次执行，因此通过 **迁移脚本** 在 CI/CD 里自动执行，无需 SSH 登录服务器手工跑 SQL。

---

## 2. CI/CD 中如何自动执行迁移

### 2.1 流程概览

GitHub Actions 工作流 `.github/workflows/deploy.yml` 在 **push 到 main/master** 时：

1. 构建 backend / frontend 镜像并推送到 ghcr.io。
2. 将 `docker-compose.prod.yml` 和 `atuo_attend_backend` 目录 SCP 到服务器。
3. 在服务器上执行：
   - `docker compose -f docker-compose.prod.yml pull && up -d` 拉取并启动容器。
   - **等待约 5 秒** 确保 MySQL 已就绪。
   - **按文件名顺序** 执行 `atuo_attend_backend/src/main/resources/db/` 下所有 **`*migration*.sql`**。

### 2.2 迁移执行方式

在服务器上使用临时 MySQL 客户端容器，挂载当前仓库中的迁移文件，对已运行的 `autoattend-mysql` 容器内的库执行 SQL：

```bash
for f in atuo_attend_backend/src/main/resources/db/*migration*.sql; do
  [ -f "$f" ] || continue
  echo "Running migration: $f"
  docker run --rm --network container:autoattend-mysql \
    -v "$(pwd)/$f:/migrate.sql:ro" \
    mysql:8.4 sh -c "mysql -h 127.0.0.1 -u autoattend -pautoattend_pwd autoattend < /migrate.sql" || true
done
```

- `--network container:autoattend-mysql`：使用与 MySQL 容器相同的网络，`127.0.0.1` 即数据库。
- 单条迁移报错用 `|| true` 忽略（例如“列已存在”等），避免因重复执行或环境差异导致整段部署失败。

因此：**只要把新的 `*migration*.sql` 放到 `atuo_attend_backend/src/main/resources/db/` 并推送到 main，下次 CI/CD 部署时会自动执行，无需手动配置或上机操作。**

---

## 3. 迁移脚本规范与顺序

- **命名**：文件名需包含 `migration`，且放在 `atuo_attend_backend/src/main/resources/db/` 下，例如：
  - `schema_collab_team_migration.sql`
  - `schema_collab_attachment_project_id_migration.sql`
  - `schema_system_config_migration.sql`
- **执行顺序**：当前按 **文件名排序** 依次执行；若有强依赖顺序，可通过文件名前缀控制（如 `01_xxx_migration.sql`、`02_xxx_migration.sql`）。
- **内容约定**：
  - 迁移应为**幂等**或**可重复执行**：如 `ADD COLUMN ...` 在列已存在时会报错，脚本内或文档中说明“可忽略”即可（CI 已用 `|| true`）。
  - 在文件头部用注释说明用途、适用场景（如“仅对已有库执行，新库无需执行”）。

---

## 4. 如何新增一条迁移

1. 在 `atuo_attend_backend/src/main/resources/db/` 下新增 SQL 文件，文件名包含 `migration`，例如 `schema_xxx_feature_migration.sql`。
2. 在文件开头写清注释，例如：
   - 本迁移做什么（加列、加表等）；
   - 仅对已有库执行、新库由建表脚本已包含时可跳过或忽略报错。
3. 若主建表脚本（如 `schema_collab_mysql.sql`）已包含新表结构，**同时更新建表脚本**，以便新部署环境从零起就具备新结构。
4. 提交并推送到 main/master；CI/CD 部署时会自动执行该迁移，**无需手动在服务器上执行 SQL**。

---

## 5. Docker Compose 与迁移的关系

- **docker-compose.yml**（本地/开发）  
  - MySQL 通过 `docker-entrypoint-initdb.d` 挂载建表脚本；**不会**自动执行迁移。  
  - 若本地库是新建的，用建表脚本即可；若本地复用了旧数据卷需要升级，可手动执行迁移（见下节）。

- **docker-compose.prod.yml**（生产）  
  - 同样只挂载建表脚本；生产库多为已有库，升级依赖 CI/CD 中的迁移步骤。  
  - 部署时 SCP 会带上 `atuo_attend_backend` 整个目录，因此服务器上有 `atuo_attend_backend/src/main/resources/db/*migration*.sql`，由 workflow 的脚本依次执行。

---

## 6. 本地或生产手动执行迁移（可选）

若需要在**本地**或**某次未走 CI 的生产环境**手动跑迁移：

```bash
# 进入项目根目录（或服务器上的部署目录）
cd /path/to/AutoAttend

# 单条迁移示例
docker run --rm --network container:autoattend-mysql \
  -v "$(pwd)/atuo_attend_backend/src/main/resources/db/schema_collab_attachment_project_id_migration.sql:/migrate.sql:ro" \
  mysql:8.4 sh -c "mysql -h 127.0.0.1 -u autoattend -pautoattend_pwd autoattend < /migrate.sql"

# 或按顺序执行所有迁移
for f in atuo_attend_backend/src/main/resources/db/*migration*.sql; do
  [ -f "$f" ] || continue
  echo "Running: $f"
  docker run --rm --network container:autoattend-mysql \
    -v "$(pwd)/$f:/migrate.sql:ro" \
    mysql:8.4 sh -c "mysql -h 127.0.0.1 -u autoattend -pautoattend_pwd autoattend < /migrate.sql" || true
done
```

确保 MySQL 容器名称为 `autoattend-mysql`，且当前目录下存在 `atuo_attend_backend/src/main/resources/db/`。

---

## 7. 小结

| 场景 | 操作 |
|------|------|
| 新环境首次部署 | 仅需 `docker compose up`，建表脚本由 initdb 执行 |
| 已有库升级（加列/加表等） | 在 `db/` 下新增或修改 `*migration*.sql`，推送到 main，CI/CD 部署时**自动执行** |
| 不想等 CI，手动升级 | 在部署目录执行上述 `docker run` 循环，对 `*migration*.sql` 执行一遍 |

按上述规范新增迁移并推送后，**无需再手动配置或上机执行 SQL**，CI/CD 会自动完成数据库迁移。
