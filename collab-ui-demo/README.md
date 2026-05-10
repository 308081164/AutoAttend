# 协作「开发与数据看板」静态 Demo（迁移级）

本目录为 **独立静态页面**，对照 `docs/项目协作UI优化方案.md` 与 `auto_attend_forntend/src/views/CollabTableView.vue` 的 **首页 / 表格 Tab / 管理员配置** 做布局与信息层次还原，便于评审通过后 **迁入主前端并删除本目录**。

## 本地预览

在仓库 **根目录** 执行其一：

```bash
npx --yes serve collab-ui-demo -l 5179
```

浏览器打开：<http://127.0.0.1:5179> 或 <http://localhost:5179>

若无 Node，可用 Python 3：

```bash
python -m http.server 5179 --directory collab-ui-demo
```

## 文件说明

| 文件 | 说明 |
|------|------|
| `index.html` | 页面结构：传送门、L1–L4、顶栏 Tab、表格工具栏、内嵌看板占位、传送门模态框 |
| `styles.css` | 样式（含 `admin-only`、双栏 L3、表格子 Tab 等） |
| `mock-data.js` | `window.COLLAB_DEMO` 集中 mock，迁移时可替换为接口 / props |
| `app.js` | 主 Tab、子 Tab、管理员开关、传送门列表与弹层同步、表格数据渲染 |

数据均为 **mock**，无后端、无构建步骤。

## 与生产页面对照（迁移时按块替换）

| Demo 区域 / 行为 | 生产参考（Vue） |
|------------------|-----------------|
| 顶栏项目名、副标题、环境 pill | `CollabTableView` 顶栏区 |
| 身份条 + 会话提示 | 当前用户 / JWT 角色展示 |
| **模拟管理员会话** 勾选 | `hasAdminSession` 为真时显示的配置块 |
| 传送门工具栏 + 链接列表 +「配置传送门」 | `home-portal-surface`、`openPortalModal` 等 |
| L1 项目摘要、风险与阻塞 | 首页摘要卡、风险列表数据源 |
| L2 关键指标 | KPI / 统计接口或 store |
| L3 每日总结横向滚动 | 每日总结接口 |
| L3 右侧「控制台数据看板」栅格占位 | 内嵌 `DashboardView`（`fixedRepoFullName`、`collab-data-board-only`、`embedded-compact` 等 props） |
| L4 邮件通知 / AI 联动 / 客户阅览看板 | 协作设置折叠区字段与保存、测试邮件、令牌逻辑 |
| Tab「项目调整表」「功能清单」 | 双表切换与路由/状态 |
| 表内工具栏：筛选、新建、AI 录入、CSV、本表数据看板 | 表头工具栏按钮组 |
| 表内子 Tab「表格 / 数据看板」 | `showDashboard` 与多 `canvas` 卡片区 |
| 列名：问题描述、归属模块… / 功能名称、开发进度… | 多维表默认列映射 |

按上表将对应 **HTML 区块** 替换为 Vue 组件并接 API 后，即可达到「迁移即可直接使用」的接线等级；本 Demo 不承担真实保存与权限校验。

## 键盘

- 传送门配置弹层打开时，可按 **Esc** 关闭（与保存同样会同步输入到内存中的 `COLLAB_DEMO.portalLinks` 并刷新列表）。
