const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

const dotenv = require('dotenv');

function loadEnvFile() {
  const candidates = [
    'env.local',
    '.env.local',
    '.env',
    '.env copy',
    '.env copy.example',
    '.env.example',
  ];

  const cwd = process.cwd();
  for (const filename of candidates) {
    const p = path.join(cwd, filename);
    if (fs.existsSync(p) && fs.statSync(p).isFile()) {
      // IMPORTANT: override ensures file values replace any pre-set environment variables.
      dotenv.config({ path: p, override: true });
      // Some environments still keep pre-set env vars. For DEEPSEEK_* we force-apply from file.
      forceApplyDeepseekEnvFromFile(p);
      // eslint-disable-next-line no-console
      console.log(`[mvp-vue server] loaded env from ${filename} (${p})`);
      return filename;
    }
  }

  // Fall back to default behavior (reads process.env only)
  dotenv.config({ override: true });
  return null;
}

function forceApplyDeepseekEnvFromFile(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    const lines = content.split(/\r?\n/);

    /** @type {Record<string, string>} */
    const kv = {};
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const eq = trimmed.indexOf('=');
      if (eq <= 0) continue;
      const k = trimmed.slice(0, eq).trim();
      const v = trimmed.slice(eq + 1).trim();
      if (k) kv[k] = v;
    }

    // Only force-apply for this MVP.
    if (kv.DEEPSEEK_API_KEY) process.env.DEEPSEEK_API_KEY = kv.DEEPSEEK_API_KEY;
    if (kv.DEEPSEEK_BASE_URL) process.env.DEEPSEEK_BASE_URL = kv.DEEPSEEK_BASE_URL;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.warn(`[mvp-vue server] failed to force-apply env from file: ${e?.message || e}`);
  }
}

loadEnvFile();

const PORT = process.env.PORT ? Number(process.env.PORT) : 3001;
const HOST = process.env.HOST || '127.0.0.1';

function normalizeApiKey(raw) {
  let v = String(raw || '');
  // Remove BOM / surrounding whitespace
  v = v.replace(/^\uFEFF/, '').trim();
  // Strip surrounding quotes
  v = v.replace(/^['"]/, '').replace(/['"]$/, '');
  // Strip common trailing separators accidentally pasted
  v = v.replace(/[;,]+$/, '').trim();
  return v;
}

const DEEPSEEK_API_KEY = normalizeApiKey(process.env.DEEPSEEK_API_KEY);
const DEEPSEEK_BASE_URL = process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com/v1';

// Only output non-sensitive debug info.
// Helps confirm whether env.local/.env.example is being loaded as expected.
const keyTail4 = DEEPSEEK_API_KEY ? DEEPSEEK_API_KEY.slice(-4) : '';
console.log(
  `[mvp-vue server] DeepSeek config: keyLoaded=${!!DEEPSEEK_API_KEY}, keyLength=${DEEPSEEK_API_KEY.length}, keyTail4=${keyTail4}, baseUrl=${DEEPSEEK_BASE_URL}`
);

function setJson(res, statusCode, payload) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  });
  res.end(JSON.stringify(payload));
}

async function readJsonBody(req) {
  const chunks = [];
  for await (const chunk of req) {
    chunks.push(chunk);
  }
  const raw = Buffer.concat(chunks).toString('utf8');
  if (!raw) return {};
  return JSON.parse(raw);
}

async function callDeepseekChatCompletions({ prompt, model }) {
  if (!DEEPSEEK_API_KEY) {
    throw new Error('DEEPSEEK_API_KEY is not set');
  }
  if (!DEEPSEEK_API_KEY.startsWith('sk-') || DEEPSEEK_API_KEY.length < 30) {
    throw new Error(
      `DEEPSEEK_API_KEY looks invalid (length=${DEEPSEEK_API_KEY.length}). Please re-copy the full key from DeepSeek console and ensure no quotes/newlines are included.`
    );
  }

  const response = await fetch(`${DEEPSEEK_BASE_URL.replace(/\/$/, '')}/chat/completions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      Authorization: `Bearer ${DEEPSEEK_API_KEY}`,
    },
    body: JSON.stringify({
      model: model || 'deepseek-chat',
      messages: [{ role: 'user', content: prompt }],
      stream: false,
      temperature: 0.4,
      max_tokens: 4096,
    }),
  });

  const rawText = await response.text();
  const data = (() => {
    try {
      return JSON.parse(rawText);
    } catch {
      return { _raw: rawText };
    }
  })();
  if (!response.ok) {
    const msg = data?.error?.message || JSON.stringify(data).slice(0, 500);
    throw new Error(`DeepSeek request failed: HTTP ${response.status}: ${msg}`);
  }

  const content = data?.choices?.[0]?.message?.content ?? '';
  return { content };
}

function safeExtractJsonObject(text) {
  const trimmed = (text || '').trim();
  if (!trimmed) return null;

  // 1) Try direct JSON
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      return JSON.parse(trimmed);
    } catch {
      // ignore
    }
  }

  // 1.5) Try "first { ... last }" slice (handles extra leading/trailing text)
  const firstBrace = trimmed.indexOf('{');
  const lastBrace = trimmed.lastIndexOf('}');
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    try {
      const sliced = trimmed.slice(firstBrace, lastBrace + 1);
      return JSON.parse(sliced);
    } catch {
      // ignore
    }
  }

  // 2) Try fenced json
  const fenced = trimmed.match(/```json\s*([\s\S]*?)\s*```/i);
  if (fenced?.[1]) {
    try {
      return JSON.parse(fenced[1]);
    } catch {
      // ignore
    }
  }

  return null;
}

function stripScriptTags(html) {
  return String(html || '').replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
}

function extractCodeFence(text, lang) {
  const re = new RegExp('```' + lang + '\\s*([\\\\s\\\\S]*?)\\s*```', 'i');
  const m = String(text || '').match(re);
  return m?.[1] ? m[1].trim() : null;
}

function removeStyleTags(html) {
  return String(html || '').replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '');
}

function parseDesignFromText(text) {
  const json = safeExtractJsonObject(text);
  if (json && typeof json === 'object') {
    const html = typeof json.html === 'string' ? json.html : '';
    const css = typeof json.css === 'string' ? json.css : '';
    if (html || css) return { html, css };
  }

  const html = extractCodeFence(text, 'html');
  const css = extractCodeFence(text, 'css');
  if (html && css) return { html, css };

  // Fallback: if we got raw HTML with embedded <style>, try to extract CSS
  const maybeHtml = String(text || '').trim();
  if (maybeHtml.includes('<style')) {
    const styleMatch = maybeHtml.match(/<style[\s\S]*?>([\s\S]*?)<\/style>/i);
    const extractedCss = styleMatch?.[1]?.trim() || '';
    const extractedHtml = removeStyleTags(maybeHtml);
    if (extractedHtml || extractedCss) return { html: extractedHtml, css: extractedCss };
  }

  return null;
}

async function generateDesignWithRepair({ prompt, model }) {
  const first = await callDeepseekChatCompletions({ prompt, model });
  const firstText = first.content || '';
  const firstDesign = parseDesignFromText(firstText);
  if (firstDesign && (firstDesign.html || firstDesign.css)) {
    return { content: firstText, design: firstDesign, repaired: false };
  }

  // If parsing failed (often due to truncation / invalid JSON), ask the model to repair strictly.
  const repairPrompt = `你刚才的输出无法被 JSON.parse 解析（通常是因为输出被截断或字符串未闭合）。
请你【只输出一个 JSON 对象】并确保 100% 可解析，结构固定为：
{ "html": "...", "css": "..." }

约束：
1) 不要输出 \`\`\` 代码块标记，不要输出任何解释文字
2) html/css 字符串必须闭合，不能被截断
3) CSS 要简洁（控制在 120 行以内），避免冗长
4) html 只包含 body 内元素，不要包含 <html>/<head>/<body>/<style>/<script>
5) 不要包含 <script> 或 on* 事件属性

这是你上一次的原始输出（请基于它修复并压缩）：
${firstText}`;

  const second = await callDeepseekChatCompletions({ prompt: repairPrompt, model });
  const secondText = second.content || '';
  const secondDesign = parseDesignFromText(secondText);
  return { content: secondText, design: secondDesign, repaired: true, original: firstText };
}

const server = http.createServer(async (req, res) => {
  try {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;

    if (req.method === 'OPTIONS') {
      // Preflight
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      });
      res.end();
      return;
    }

    if (req.method === 'POST' && pathname === '/api/chat') {
      const body = await readJsonBody(req);
      const prompt = body?.prompt;
      const model = body?.model;

      if (!prompt || typeof prompt !== 'string') {
        setJson(res, 400, { error: 'prompt is required (string)' });
        return;
      }

      const result = await generateDesignWithRepair({ prompt, model });
      setJson(res, 200, result);
      return;
    }

    setJson(res, 404, { error: 'Not Found' });
  } catch (err) {
    setJson(res, 500, { error: err instanceof Error ? err.message : String(err) });
  }
});

server.listen(PORT, HOST, () => {
  // eslint-disable-next-line no-console
  console.log(`[mvp-vue server] http://${HOST}:${PORT}`);
});

