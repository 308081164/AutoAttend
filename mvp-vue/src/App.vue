<script setup lang="ts">
import { computed, ref } from 'vue';
import { chatWithDeepseek } from './api';

type Role = 'user' | 'assistant';
type Design = { html: string; css: string };
type Message = { role: Role; content: string; design?: Design | null };

const prompt = ref('');
const selectedModel = ref('deepseek-chat');
const isLoading = ref(false);

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content:
      '你好！我是 SuperDesign MVP（DeepSeek 版）。\n你可以直接输入中文需求（例如：生成一个登录页 mockup）。我会自动解析为 HTML + CSS 并预览。',
    design: null,
  },
]);

const canSend = computed(() => !isLoading.value && prompt.value.trim().length > 0);

const latestDesign = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const m = messages.value[i];
    if (m?.role === 'assistant' && m.design && (m.design.html || m.design.css)) return m.design;
  }
  return null;
});

const iframeSrcdoc = computed(() => {
  const design = latestDesign.value;
  if (!design) return '';
  const css = design.css || '';
  const html = design.html || '';

  // 用 sandbox 禁用脚本执行，降低风险（主要用于本地 MVP 演示）。
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>${css}</style>
</head>
<body>
${html}
</body>
</html>`;
});

function buildDesignPrompt(userInput: string) {
  return `你是一名专业前端工程师。请生成一个“可直接在浏览器预览”的前端 mockup，用于本地 MVP 演示。

要求：
1. 只允许输出一个 JSON 对象，不要输出任何多余文字。
2. JSON 结构固定：{ "html": "...", "css": "..." }
3. html：只包含 body 内所需的元素（可以包含 class/id），不要包含 <html>, <head>, <body>，不要包含 <style> 标签。
4. css：只包含 CSS 内容（不要包含 <style> 标签）。
5. 需要支持中文显示（使用系统字体栈：sans-serif 即可），不要引用外部图片/外链资源。
6. 不要包含 <script>，不要包含事件处理属性（如 onclick=...）。
7. 尽量保证响应式（移动端也要好看）。

用户需求（中文）：
${userInput}`;
}

function downloadLatestMockup() {
  const design = latestDesign.value;
  if (!design) return;

  const css = design.css || '';
  const html = design.html || '';
  const doc = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>${css}</style>
</head>
<body>
${html}
</body>
</html>`;

  const blob = new Blob([doc], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'superdesign-mockup.html';
  a.click();
  URL.revokeObjectURL(url);
}

async function onSend() {
  const content = prompt.value.trim();
  if (!content) return;

  messages.value.push({ role: 'user', content });
  prompt.value = '';
  isLoading.value = true;

  try {
    const designPrompt = buildDesignPrompt(content);
    const res = await chatWithDeepseek(designPrompt, selectedModel.value || undefined);

    const design = res.design || null;
    const raw = res.content || '(空回复)';
    const hasDesign = !!design && (!!design.html || !!design.css);

    const assistantContent = hasDesign
      ? `已生成 mockup（HTML/CSS），已尝试解析并在下方预览。\n\n原始输出（节选）：\n${raw.substring(0, 500)}${
          raw.length > 500 ? '...' : ''
        }`
      : raw;
    messages.value.push({
      role: 'assistant',
      content: assistantContent,
      design,
    });
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    messages.value.push({ role: 'assistant', content: `调用 DeepSeek 失败：${msg}`, design: null });
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="container">
    <div class="panel">
      <h1 style="margin: 0 0 12px 0; font-size: 20px; letter-spacing: 0.2px">SuperDesign MVP（中文对话）</h1>
      <div class="hint">本地后端会调用 DeepSeek（OpenAI 兼容接口）。请先设置环境变量 <code>DEEPSEEK_API_KEY</code>。</div>

      <div style="height: 16px"></div>

      <div class="row">
        <div class="chat-log" role="log" aria-live="polite">
          <div v-for="(m, idx) in messages" :key="idx" :class="['msg', m.role === 'user' ? 'msg-user' : 'msg-assistant']">
            <b style="display: block; margin-bottom: 6px; font-size: 13px; color: rgba(231, 238, 252, 0.85)">
              {{ m.role === 'user' ? '你' : 'DeepSeek' }}
            </b>
            {{ m.content }}
          </div>
          <div v-if="isLoading" class="msg msg-assistant">
            正在生成中……
          </div>
        </div>
      </div>

      <div style="height: 14px"></div>

      <div class="row" style="align-items: stretch">
        <div style="flex: 1">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px">
            <div style="font-weight: 800; letter-spacing: 0.2px">Mockup 预览</div>
            <div style="color: rgba(231, 238, 252, 0.7); font-size: 13px">
              {{ latestDesign ? '已解析 HTML/CSS' : '等待模型输出可预览内容' }}
            </div>
          </div>

          <iframe
            v-if="latestDesign"
            :srcdoc="iframeSrcdoc"
            sandbox=""
            style="width: 100%; height: 420px; border-radius: 12px; border: 1px solid var(--border); background: #fff"
          />
          <div v-else class="panel" style="padding: 14px; border-radius: 12px; background: rgba(231, 238, 252, 0.03)">
            <div style="font-weight: 800; margin-bottom: 6px">暂无可预览产物</div>
            <div style="color: rgba(231, 238, 252, 0.75); font-size: 13px; line-height: 1.5">
              DeepSeek 返回的内容未被解析为 HTML/CSS。
              你可以稍微调整你的需求，或我会在后端解析逻辑上继续增强。
            </div>
          </div>
        </div>

        <div style="width: 360px; display: flex; flex-direction: column; gap: 10px">
          <button
            :disabled="!latestDesign"
            style="background: transparent; border: 1px solid var(--border); color: var(--text)"
            @click="downloadLatestMockup"
          >
            下载 mockup（HTML+CSS）
          </button>

          <div class="panel" style="padding: 12px; border-radius: 12px">
            <div style="font-weight: 800; margin-bottom: 8px">已解析代码</div>
            <div style="color: rgba(231, 238, 252, 0.7); font-size: 13px; margin-bottom: 6px">HTML（只包含 body 内容）</div>
            <pre
              v-if="latestDesign"
              style="max-height: 150px; overflow: auto; white-space: pre-wrap; border: 1px solid var(--border); border-radius: 12px; padding: 10px; margin: 0; font-size: 12px"
            >{{ latestDesign.html }}</pre>
            <div v-else style="color: rgba(231, 238, 252, 0.7); font-size: 13px; line-height: 1.5">—</div>

            <div style="height: 10px"></div>
            <div style="color: rgba(231, 238, 252, 0.7); font-size: 13px; margin-bottom: 6px">CSS</div>
            <pre
              v-if="latestDesign"
              style="max-height: 150px; overflow: auto; white-space: pre-wrap; border: 1px solid var(--border); border-radius: 12px; padding: 10px; margin: 0; font-size: 12px"
            >{{ latestDesign.css }}</pre>
          </div>
        </div>
      </div>

      <div style="height: 12px"></div>

      <div class="row">
        <div style="flex: 1">
          <div style="display: flex; gap: 12px; margin-bottom: 8px; align-items: center">
            <label style="display: flex; align-items: center; gap: 10px; color: rgba(231, 238, 252, 0.9)">
              模型
              <input
                v-model="selectedModel"
                style="width: 220px; background: transparent; color: var(--text); border: 1px solid var(--border); border-radius: 12px; padding: 10px 12px; outline: none"
                placeholder="deepseek-chat"
              />
            </label>
          </div>

          <textarea v-model="prompt" placeholder="输入你的中文需求，比如：生成一个现代登录页（包含按钮、输入框、布局说明）" />
          <div class="hint">
            提示建议：给出页面类型 + 风格关键词 + 输出格式（HTML/CSS/结构说明）。
          </div>
        </div>

        <div style="width: 180px; display: flex; flex-direction: column; gap: 10px">
          <button :disabled="!canSend" @click="onSend">发送</button>
          <button
            :disabled="isLoading || messages.length <= 1"
            style="background: transparent; border: 1px solid var(--border); color: var(--text)"
            @click="
              () => {
                messages.value = [
                  {
                    role: 'assistant',
                    content:
                      '已清空对话。你可以继续输入中文需求，我会继续调用 DeepSeek。',
                  },
                ];
              }
            "
          >
            清空
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

