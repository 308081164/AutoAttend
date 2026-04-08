import * as vscode from "vscode";

type ApiEnvelope<T> = { code: number; message?: string; data?: T };
type LoginResponse = { token?: string; collabToken?: string };
type ProjectItem = { id: number; name?: string };

const SECRET_ADMIN = "autoattend.admin.token";
const SECRET_COLLAB = "autoattend.collab.token";
const STATE_BASE_URL = "autoattend.baseUrl";

class AutoAttendApiClient {
  constructor(private readonly context: vscode.ExtensionContext) {}

  async getBaseUrl(): Promise<string> {
    const cfg = vscode.workspace.getConfiguration();
    const confUrl = cfg.get<string>("autoAttend.baseUrl", "").trim();
    const stateUrl = (this.context.globalState.get<string>(STATE_BASE_URL) || "").trim();
    const base = confUrl || stateUrl || "http://localhost:8080";
    return base.replace(/\/+$/, "");
  }

  async setBaseUrl(url: string): Promise<void> {
    await this.context.globalState.update(STATE_BASE_URL, url.trim().replace(/\/+$/, ""));
  }

  async clearTokens(): Promise<void> {
    await this.context.secrets.delete(SECRET_ADMIN);
    await this.context.secrets.delete(SECRET_COLLAB);
  }

  async hasCollabToken(): Promise<boolean> {
    const t = await this.context.secrets.get(SECRET_COLLAB);
    return !!t;
  }

  async getAdminToken(): Promise<string> {
    return (await this.context.secrets.get(SECRET_ADMIN)) || "";
  }

  async getCollabToken(): Promise<string> {
    return (await this.context.secrets.get(SECRET_COLLAB)) || "";
  }

  async login(account: string, password: string): Promise<void> {
    const base = await this.getBaseUrl();
    const json = await this.request<LoginResponse>(`${base}/api/admin/auth/login`, {
      method: "POST",
      body: JSON.stringify({ account, password }),
      headers: { "Content-Type": "application/json" }
    });
    if (!json?.data?.token || !json?.data?.collabToken) {
      throw new Error(json?.message || "登录失败，返回 token 缺失");
    }
    await this.context.secrets.store(SECRET_ADMIN, json.data.token);
    await this.context.secrets.store(SECRET_COLLAB, json.data.collabToken);
  }

  async ensureCollabToken(): Promise<string> {
    const current = await this.context.secrets.get(SECRET_COLLAB);
    if (current) return current;
    const admin = await this.context.secrets.get(SECRET_ADMIN);
    if (!admin) throw new Error("未登录");
    const base = await this.getBaseUrl();
    const r = await this.request<LoginResponse>(`${base}/api/admin/auth/collab-token`, {
      headers: { Authorization: `Bearer ${admin}` }
    });
    const collabToken = r?.data?.collabToken;
    if (!collabToken) throw new Error(r?.message || "获取协作 token 失败");
    await this.context.secrets.store(SECRET_COLLAB, collabToken);
    return collabToken;
  }

  async listProjects(): Promise<ProjectItem[]> {
    const data = await this.collabGet<{ items?: ProjectItem[] }>("/api/collab/projects");
    return data?.items || [];
  }

  private async collabGet<T>(path: string): Promise<T | undefined> {
    const result = await this.collabRequest(path, { method: "GET" });
    return result?.data as T | undefined;
  }

  private async collabRequest(path: string, init: RequestInit): Promise<ApiEnvelope<unknown>> {
    const base = await this.getBaseUrl();
    const token = await this.ensureCollabToken();
    return this.request(`${base}${path}`, {
      ...init,
      headers: {
        ...(init.headers || {}),
        Authorization: `Bearer ${token}`
      }
    });
  }

  private async request<T>(url: string, init: RequestInit): Promise<ApiEnvelope<T>> {
    let res: Response;
    try {
      res = await fetch(url, init);
    } catch (e) {
      throw new Error(`网络请求失败: ${String((e as Error)?.message || e)}`);
    }
    let json: ApiEnvelope<T>;
    try {
      json = (await res.json()) as ApiEnvelope<T>;
    } catch {
      throw new Error(`接口返回非 JSON: ${url}`);
    }
    if (res.status === 401) {
      await this.context.secrets.delete(SECRET_COLLAB);
    }
    if (!res.ok || json.code !== 0) {
      throw new Error(json?.message || `请求失败(${res.status})`);
    }
    return json;
  }
}

class SidebarProvider implements vscode.WebviewViewProvider {
  public static readonly viewType = "autoAttend.sidebarView";
  private _view?: vscode.WebviewView;

  constructor(private readonly context: vscode.ExtensionContext, private readonly api: AutoAttendApiClient) {}

  resolveWebviewView(webviewView: vscode.WebviewView): void {
    this._view = webviewView;
    webviewView.webview.options = { enableScripts: true };
    webviewView.webview.html = getWebviewHtml(webviewView.webview);
    webviewView.webview.onDidReceiveMessage(async (msg) => {
      try {
        await this.onMessage(msg);
      } catch (e) {
        this.post({ type: "error", message: String((e as Error)?.message || e) });
      }
    });
  }

  reveal(): void {
    this._view?.show?.(true);
  }

  async refreshState(): Promise<void> {
    const baseUrl = await this.api.getBaseUrl();
    const loggedIn = await this.api.hasCollabToken();
    const projects = loggedIn ? await this.api.listProjects().catch(() => []) : [];
    this.post({ type: "state", payload: { baseUrl, loggedIn, projects } });
  }

  private post(msg: unknown): void {
    this._view?.webview.postMessage(msg);
  }

  private async onMessage(msg: any): Promise<void> {
    const type = msg?.type;
    switch (type) {
      case "init": {
        await this.refreshState();
        return;
      }
      case "setBaseUrl": {
        const baseUrl = String(msg?.baseUrl || "").trim();
        await this.api.setBaseUrl(baseUrl);
        await this.refreshState();
        return;
      }
      case "login": {
        await this.api.login(String(msg.account || ""), String(msg.password || ""));
        vscode.window.showInformationMessage("AutoAttend 登录成功");
        await this.refreshState();
        return;
      }
      case "logout": {
        await this.api.clearTokens();
        vscode.window.showInformationMessage("AutoAttend 已退出登录");
        await this.refreshState();
        return;
      }
      case "loadProjects": {
        const projects = await this.api.listProjects();
        this.post({ type: "projects", payload: projects });
        return;
      }
      case "openEmbed": {
        const loggedIn = await this.api.hasCollabToken();
        if (!loggedIn) throw new Error("请先登录");
        const baseUrl = await this.api.getBaseUrl();
        const adminToken = await this.api.getAdminToken();
        const collabToken = await this.api.getCollabToken();
        const projectId = Number(msg?.projectId || 0);
        const path = projectId
          ? `/collab/projects/${projectId}/table`
          : "/collab/projects";
        const url =
          `${baseUrl}${path}?embed=1&from=vscode` +
          `&adminToken=${encodeURIComponent(adminToken)}` +
          `&collabToken=${encodeURIComponent(collabToken)}` +
          `&username=${encodeURIComponent(msg?.username || "")}`;
        this.post({ type: "embedUrl", payload: { url } });
        return;
      }
      case "openLink": {
        const url = String(msg?.url || "");
        if (!/^https?:\/\//i.test(url)) throw new Error("仅支持 http/https 链接");
        await vscode.env.openExternal(vscode.Uri.parse(url));
        return;
      }
      default:
        break;
    }
  }
}

export function activate(context: vscode.ExtensionContext): void {
  const api = new AutoAttendApiClient(context);
  const provider = new SidebarProvider(context, api);
  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider(SidebarProvider.viewType, provider),
    vscode.commands.registerCommand("autoAttend.openPanel", () => provider.reveal()),
    vscode.commands.registerCommand("autoAttend.logout", async () => {
      await api.clearTokens();
      await provider.refreshState();
      vscode.window.showInformationMessage("AutoAttend 已退出登录");
    })
  );
}

export function deactivate(): void {}

function getWebviewHtml(webview: vscode.Webview): string {
  const nonce = String(Date.now());
  const csp = `default-src 'none'; img-src ${webview.cspSource} https: http: data:; frame-src https: http:; style-src 'unsafe-inline'; script-src 'nonce-${nonce}';`;
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta http-equiv="Content-Security-Policy" content="${csp}">
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>AutoAttend</title>
  <style>
    body{font-family:var(--vscode-font-family);padding:10px;color:var(--vscode-editor-foreground)}
    input,select,button,textarea{width:100%;box-sizing:border-box;margin:4px 0;padding:6px;background:var(--vscode-input-background);color:var(--vscode-input-foreground);border:1px solid var(--vscode-input-border)}
    button{cursor:pointer}
    .row{display:flex;gap:8px}
    .row>*{flex:1}
    .card{border:1px solid var(--vscode-panel-border);border-radius:6px;padding:8px;margin:8px 0}
    .muted{opacity:.75;font-size:12px}
    .error{color:#f87171}
    .ok{color:#34d399}
    .iframe-wrap{margin-top:8px;border:1px solid var(--vscode-panel-border);border-radius:6px;overflow:hidden;height:70vh}
    iframe{width:100%;height:100%;border:0;background:#fff}
  </style>
</head>
<body>
  <div class="card">
    <div><b>连接配置</b></div>
    <input id="baseUrl" placeholder="http://localhost:8080" />
    <button id="saveBaseUrl">保存地址</button>
    <div class="muted" id="stateLine">未初始化</div>
  </div>

  <div class="card">
    <div><b>登录</b></div>
    <input id="account" placeholder="手机号/邮箱/账号" />
    <input id="password" type="password" placeholder="密码" />
    <div class="row">
      <button id="loginBtn">登录</button>
      <button id="logoutBtn">退出</button>
    </div>
  </div>

  <div class="card">
    <div><b>加载网页工作台（Webview）</b></div>
    <div class="row">
      <button id="loadProjectsBtn">加载项目</button>
      <button id="openEmbedBtn">打开嵌入页</button>
    </div>
    <select id="projectSelect"><option value="">请选择项目...</option></select>
    <div class="muted">项目为空时将打开项目列表页</div>
    <div class="iframe-wrap">
      <iframe id="embedFrame" referrerpolicy="no-referrer"></iframe>
    </div>
  </div>

  <div id="message" class="muted"></div>

  <script nonce="${nonce}">
    const vscode = acquireVsCodeApi();
    const el = (id) => document.getElementById(id);
    function msg(text, kind) {
      const n = el("message");
      n.textContent = text || "";
      n.className = kind === "error" ? "error" : (kind === "ok" ? "ok" : "muted");
    }

    function renderProjects(items) {
      const s = el("projectSelect");
      s.innerHTML = '<option value="">请选择项目...</option>';
      (items || []).forEach(p => {
        const o = document.createElement("option");
        o.value = String(p.id);
        o.textContent = p.name ? p.name + " (#" + p.id + ")" : ("项目#" + p.id);
        s.appendChild(o);
      });
    }

    window.addEventListener("message", (event) => {
      const msgIn = event.data || {};
      switch (msgIn.type) {
        case "state":
          el("baseUrl").value = msgIn.payload.baseUrl || "";
          el("stateLine").textContent = msgIn.payload.loggedIn ? "已登录" : "未登录";
          renderProjects(msgIn.payload.projects || []);
          break;
        case "projects":
          renderProjects(msgIn.payload || []);
          msg("项目加载完成", "ok");
          break;
        case "embedUrl":
          el("embedFrame").src = msgIn.payload.url;
          msg("已打开 Webview 嵌入页面", "ok");
          break;
        case "error":
          msg(msgIn.message || "操作失败", "error");
          break;
        default:
          break;
      }
    });

    el("saveBaseUrl").onclick = () => vscode.postMessage({ type: "setBaseUrl", baseUrl: el("baseUrl").value });
    el("loginBtn").onclick = () => vscode.postMessage({ type: "login", account: el("account").value, password: el("password").value });
    el("logoutBtn").onclick = () => vscode.postMessage({ type: "logout" });
    el("loadProjectsBtn").onclick = () => vscode.postMessage({ type: "loadProjects" });
    el("openEmbedBtn").onclick = () => {
      const projectId = Number(el("projectSelect").value || 0);
      vscode.postMessage({
        type: "openEmbed",
        projectId,
        username: el("account").value || ""
      });
    };

    vscode.postMessage({ type: "init" });
  </script>
</body>
</html>`;
}

