import * as vscode from "vscode";
import { createHash, randomUUID } from "crypto";

export type CodingHeartbeatItem = {
  clientEventId: string;
  projectId: number;
  heartbeatAt: string;
  durationSeconds: number;
  language?: string;
  fileFingerprint?: string | null;
};

export interface CodingCollabPort {
  hasCollabToken(): Promise<boolean>;
  postCodingHeartbeats(items: CodingHeartbeatItem[]): Promise<number>;
}

const STATE_QUEUE = "autoattend.codingTime.queue";
const STATE_PROJECT_ID = "autoattend.codingTime.projectId";
const MAX_QUEUE = 200;

const IGNORE_SCHEMES = new Set([
  "output",
  "git",
  "vscode-scm",
  "debug",
  "vscode-terminal",
  "vscode-notebook-cell"
]);

function cfgEnabled(): boolean {
  return vscode.workspace.getConfiguration().get<boolean>("autoAttend.codingTime.enabled", false);
}

function cfgIntervalSec(): number {
  const v = vscode.workspace.getConfiguration().get<number>("autoAttend.codingTime.heartbeatSeconds", 60);
  return Math.min(120, Math.max(30, v | 0));
}

function cfgFingerprint(): boolean {
  return vscode.workspace.getConfiguration().get<boolean>("autoAttend.codingTime.sendFileFingerprint", false);
}

function isLikelyEditorDoc(doc: vscode.TextDocument | undefined): boolean {
  if (!doc) return false;
  if (IGNORE_SCHEMES.has(doc.uri.scheme)) return false;
  if (doc.uri.scheme === "vscode") return false;
  return true;
}

function isActiveCoding(): boolean {
  if (!vscode.window.state.focused) return false;
  const ed = vscode.window.activeTextEditor;
  if (!ed) return false;
  return isLikelyEditorDoc(ed.document);
}

function fingerprintFor(doc: vscode.TextDocument | undefined): string | null {
  if (!cfgFingerprint() || !doc) return null;
  try {
    const raw = doc.uri.toString();
    return createHash("sha256").update(raw, "utf8").digest("hex").slice(0, 32);
  } catch {
    return null;
  }
}

export class CodingTimeTracker implements vscode.Disposable {
  private disposables: vscode.Disposable[] = [];
  private tick?: NodeJS.Timeout;
  private flushTimer?: NodeJS.Timeout;
  private segmentStartMs: number | null = null;
  private lastDocLang: string | undefined;
  private lastFingerprint: string | null = null;
  private status?: vscode.StatusBarItem;

  constructor(
    private readonly context: vscode.ExtensionContext,
    private readonly port: CodingCollabPort,
    private readonly onSummaryTick?: () => void
  ) {}

  start(): void {
    this.disposeInner(false);
    if (!cfgEnabled()) {
      this.hideStatus();
      return;
    }
    this.status = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    this.status.command = "autoAttend.openPanel";
    this.disposables.push(this.status);

    const sampleMs = 5000;
    this.segmentStartMs = isActiveCoding() ? Date.now() : null;
    this.disposables.push(
      vscode.window.onDidChangeWindowState(() => this.onWindowState()),
      vscode.window.onDidChangeActiveTextEditor(() => this.onEditorSwitch()),
      vscode.workspace.onDidChangeTextDocument((e) => this.onDocChange(e)),
      vscode.workspace.onDidChangeConfiguration((e) => {
        if (e.affectsConfiguration("autoAttend.codingTime")) {
          this.start();
        }
      })
    );

    this.tick = setInterval(() => this.onSampleTick(sampleMs), sampleMs);
    const flushEvery = Math.max(cfgIntervalSec(), 30) * 1000;
    this.flushTimer = setInterval(() => void this.tryFlushQueue(), flushEvery);
    void this.tryFlushQueue();
    this.updateStatusText();
    this.status.show();
  }

  dispose(): void {
    this.disposeInner(true);
  }

  private disposeInner(flush: boolean): void {
    if (this.tick) {
      clearInterval(this.tick);
      this.tick = undefined;
    }
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = undefined;
    }
    if (flush) {
      void this.closeSegmentAndMaybeQueue();
    }
    for (const d of this.disposables) {
      d.dispose();
    }
    this.disposables = [];
    this.hideStatus();
  }

  private hideStatus(): void {
    this.status?.dispose();
    this.status = undefined;
  }

  private onWindowState(): void {
    if (!vscode.window.state.focused) {
      void this.closeSegmentAndMaybeQueue();
    } else if (this.segmentStartMs === null && isActiveCoding()) {
      this.segmentStartMs = Date.now();
    }
    this.updateStatusText();
  }

  private onEditorSwitch(): void {
    void this.closeSegmentAndMaybeQueue();
    if (isActiveCoding()) {
      this.segmentStartMs = Date.now();
      const ed = vscode.window.activeTextEditor;
      this.lastDocLang = ed?.document.languageId;
      this.lastFingerprint = fingerprintFor(ed?.document);
    }
    this.updateStatusText();
  }

  private onDocChange(e: vscode.TextDocumentChangeEvent): void {
    if (!isActiveCoding()) return;
    if (e.document !== vscode.window.activeTextEditor?.document) return;
    if (this.segmentStartMs === null) this.segmentStartMs = Date.now();
    this.lastDocLang = e.document.languageId;
    this.lastFingerprint = fingerprintFor(e.document);
  }

  private onSampleTick(_sampleMs: number): void {
    if (isActiveCoding()) {
      if (this.segmentStartMs === null) {
        this.segmentStartMs = Date.now();
      }
      const ed = vscode.window.activeTextEditor;
      if (ed) {
        this.lastDocLang = ed.document.languageId;
        this.lastFingerprint = fingerprintFor(ed.document);
      }
      void this.maybeSlicePeriodicSegment();
    } else {
      void this.closeSegmentAndMaybeQueue();
    }
    this.updateStatusText();
    this.onSummaryTick?.();
  }

  /** 达到心跳间隔仍在编辑态时，切一段上报（避免仅依赖切文件/失焦） */
  private async maybeSlicePeriodicSegment(): Promise<void> {
    if (this.segmentStartMs === null) return;
    const sec = Math.floor((Date.now() - this.segmentStartMs) / 1000);
    const need = cfgIntervalSec();
    if (sec < need) return;
    this.segmentStartMs = Date.now();
    await this.queueSegmentSeconds(Math.min(sec, 7200));
  }

  private async queueSegmentSeconds(sec: number): Promise<void> {
    if (sec < 1) return;
    const projectId = this.context.globalState.get<number>(STATE_PROJECT_ID);
    if (!projectId || projectId <= 0) return;
    const end = Date.now();
    const item: CodingHeartbeatItem = {
      clientEventId: `vscode-${randomUUID()}`,
      projectId,
      heartbeatAt: new Date(end).toISOString(),
      durationSeconds: sec,
      language: this.lastDocLang,
      fileFingerprint: this.lastFingerprint
    };
    await this.enqueue(item);
  }

  private async closeSegmentAndMaybeQueue(): Promise<void> {
    if (this.segmentStartMs === null) return;
    const sec = Math.floor((Date.now() - this.segmentStartMs) / 1000);
    this.segmentStartMs = null;
    await this.queueSegmentSeconds(sec);
  }

  private async enqueue(item: CodingHeartbeatItem): Promise<void> {
    const q = this.context.globalState.get<CodingHeartbeatItem[]>(STATE_QUEUE) || [];
    q.push(item);
    while (q.length > MAX_QUEUE) q.shift();
    await this.context.globalState.update(STATE_QUEUE, q);
    await this.tryFlushQueue();
  }

  async tryFlushQueue(): Promise<void> {
    if (!(await this.port.hasCollabToken())) return;
    const q = this.context.globalState.get<CodingHeartbeatItem[]>(STATE_QUEUE) || [];
    if (q.length === 0) return;
    try {
      await this.port.postCodingHeartbeats(q);
      await this.context.globalState.update(STATE_QUEUE, []);
    } catch {
      /* 保留队列稍后重试 */
    }
    this.updateStatusText();
  }

  bump(): void {
    this.updateStatusText();
    void this.tryFlushQueue();
  }

  private updateStatusText(): void {
    if (!this.status) return;
    const on = cfgEnabled();
    if (!on) return;
    const pid = this.context.globalState.get<number>(STATE_PROJECT_ID);
    const queued = this.context.globalState.get<CodingHeartbeatItem[]>(STATE_QUEUE)?.length || 0;
    const active = isActiveCoding();
    const parts = ["AutoAttend 编码"];
    if (!pid) parts.push("未绑定项目");
    else parts.push(`#${pid}`);
    parts.push(active ? "记录中" : "空闲");
    if (queued > 0) parts.push(`队列${queued}`);
    this.status.text = parts.join(" · ");
    this.status.tooltip = "点击打开 AutoAttend 侧栏；在设置中可开关编码统计";
  }

  static async getProjectId(context: vscode.ExtensionContext): Promise<number | undefined> {
    const v = context.globalState.get<number>(STATE_PROJECT_ID);
    return v && v > 0 ? v : undefined;
  }

  static async setProjectId(context: vscode.ExtensionContext, id: number | undefined): Promise<void> {
    if (id && id > 0) await context.globalState.update(STATE_PROJECT_ID, id);
    else await context.globalState.update(STATE_PROJECT_ID, undefined);
  }
}
