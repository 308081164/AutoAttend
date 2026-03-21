# Docker 与 CI/CD 数据库迁移说明

本文说明在 **Docker Compose** 与 **GitHub Actions CI/CD** 下，数据库如何初始化、如何在 **每次部署/启动后端容器时自动执行迁移**，以及如何新增迁移而无需手工连库执行 SQL。

---

## 1. 三种机制（协同工作）

| 机制 | 作用 | 执行时机 |
|------|------|----------|
| **MySQL `docker-entrypoint-initdb.d`** | 新数据卷首次启动时建库表（`schema_mysql` / `collab` / `ai` / `quote`） | **仅第一次**（数据卷为空时） |
| **后端镜像 `docker-entrypoint.sh`** | 按 `migrate_manifest.txt` 顺序执行增量 SQL（报价表、迁移脚本等） | **每次 `backend` 容器启动**（含 CI 部署 `up -d` 后容器重启） |
| **清单文件** `db/migrate_manifest.txt` | 声明迁移顺序与文件名 | 随代码维护，打进后端镜像 |

推送 `main`/`master` 后，CI 构建新 backend 镜像并 `docker compose up -d`：**新起的 backend 会在启动 Java 前自动跑完清单中的 SQL**，一般**无需再 SSH 手工执行**数据库命令。

---

## 2. 后端启动时迁移（推荐主路径）

`atuo_attend_backend/Dockerfile` 将 `src/main/resources/db/` 复制到镜像内 `/app/db/`，入口为 `docker-entrypoint.sh`：

1. 使用 `mysqladmin ping` 等待 `DB_HOST:DB_PORT` 上的 MySQL 就绪（最长约 3 分钟）。
2. 读取 `/app/db/migrate_manifest.txt`（忽略 `#` 注释与空行），按行将 `文件名` 解析为 `/app/db/<文件名>` 并执行：  
   `mysql -h... -u... -p... $DB_NAME < 文件`
3. 单条脚本若报错（例如列已存在），会打印警告并**继续**下一条，避免历史环境差异卡死部署。
4. 最后 `exec java -jar app.jar` 启动 Spring Boot。

调试时可设置环境变量 **`SKIP_DB_MIGRATE=1`** 跳过迁移（仅排障用）。

---

## 3. `migrate_manifest.txt` 规范

- 路径：`atuo_attend_backend/src/main/resources/db/migrate_manifest.txt`
- 每行一个**文件名**（相对 `db/` 目录），顺序即执行顺序；**依赖关系**通过调整行顺序保证。
- 以 `#` 开头的行与空行忽略。
- 新增迁移：在 `db/` 下增加 SQL 文件，并把文件名**追加到清单合适位置**（通常加在末尾，除非有强依赖）。
- 适合放入清单的脚本：
  - 全量可重复：`CREATE TABLE IF NOT EXISTS`、与报价相关的 `schema_quote_mysql.sql` 等。
  - 增量一次性：`ALTER TABLE ...`（重复执行可能报错，依赖「报错继续」策略）。

---

## 4. Compose 与 CI/CD

### 4.1 `docker-compose.yml` / `docker-compose.prod.yml`

- MySQL 挂载 `01`–`04` 建表脚本（含 **quote**），供**全新数据卷**初始化。
- MySQL 增加 **`healthcheck`**（`mysqladmin ping`），**backend** 使用 `depends_on: mysql: condition: service_healthy`，减少「库未就绪就启动应用」的竞态。

### 4.2 GitHub Actions（`.github/workflows/deploy.yml`）

- `docker compose pull && up -d` 后，**不再**在 SSH 里循环执行 `*migration*.sql`；迁移统一由 **backend 容器 entrypoint** 完成。
- 仍会 SCP `atuo_attend_backend` 目录到服务器，以便 **compose 挂载 initdb 脚本**（新卷或重建 MySQL 卷时需要主机上的 SQL 文件）。

---

## 5. 与旧文档的差异（摘要）

| 过去 | 现在 |
|------|------|
| CI 在服务器上用临时容器对 `*migration*.sql` 循环执行 | 由 **backend 镜像启动脚本**按 **`migrate_manifest.txt`** 执行（含 `schema_quote_mysql.sql` 等，不再依赖文件名必须含 `migration`） |
| 仅 initdb 挂载 3 个 schema | initdb 增加 **`04_schema_quote.sql`**；清单负责已有库升级 |

---

## 6. 本地手动执行迁移（可选）

若未使用 Docker 后端入口，仍可在部署目录对单文件执行：

```bash
docker run --rm --network container:autoattend-mysql \
  -v "$(pwd)/atuo_attend_backend/src/main/resources/db/某文件.sql:/migrate.sql:ro" \
  mysql:8.4 sh -c "mysql -h 127.0.0.1 -u autoattend -pautoattend_pwd autoattend < /migrate.sql"
```

---

## 7. 小结

| 场景 | 操作 |
|------|------|
| 新环境首次部署 | `docker compose up`，initdb 执行 `01`–`04`；backend 启动时再跑清单（多为 IF NOT EXISTS，无害） |
| 已有库升级 | 在 `db/` 新增 SQL + 更新 **`migrate_manifest.txt`**，推送后 CI 部署，**backend 重启即自动迁移** |
| 临时跳过迁移 | backend 环境变量 `SKIP_DB_MIGRATE=1` |

按上述方式维护清单并推送后，**无需再手工在服务器执行 SQL**，即可完成数据库迁移。
