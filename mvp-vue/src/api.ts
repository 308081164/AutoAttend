export type Design = {
  html: string;
  css: string;
};

export type ChatResponse = {
  content: string;
  design?: Design | null;
};

export async function chatWithDeepseek(prompt: string, model?: string): Promise<ChatResponse> {
  const res = await fetch('/api/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
    },
    body: JSON.stringify({
      prompt,
      model: model || undefined,
    }),
  });

  const data = (await res.json().catch(() => ({}))) as any;
  if (!res.ok) {
    throw new Error(data?.error || `Request failed: ${res.status}`);
  }

  return {
    content: data?.content || '',
    design: data?.design ?? null,
  };
}

