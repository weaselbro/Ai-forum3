import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const { apiKey, model, systemPrompt, input, temperature, maxTokens } = await request.json();

    if (!apiKey || apiKey.trim() === '') {
      return NextResponse.json({ error: 'Enter an OpenRouter API key.' }, { status: 400 });
    }

    const response = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey.trim()}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model,
        messages: [
          { role: 'system', content: systemPrompt || 'Engage in a candid, imaginative, and direct dialogue. Be creative, concise, and responsive to the other model.' },
          { role: 'user', content: input },
        ],
        temperature: Math.min(2, Math.max(0, temperature ?? 0.8)),
        max_tokens: Math.max(64, maxTokens || 512),
        provider: { sort: 'throughput' },
      }),
    });

    const body = await response.json();

    // Handle OpenRouter returning error in body (even with 200 status)
    if (body.error) {
      const msg = body.error.message || 'OpenRouter returned an error';
      const code = body.error.code || response.status || 400;
      if (code === 404 || msg.includes('No endpoints')) {
        return NextResponse.json({ error: 'Model unavailable or privacy restrictions. Configure: https://openrouter.ai/settings/privacy' }, { status: 404 });
      }
      return NextResponse.json({ error: msg }, { status: response.status || 400 });
    }

    // Handle non-OK responses
    if (!response.ok) {
      const rawError = body?.error?.message || `HTTP ${response.status}`;
      const errorMessages: Record<number, string> = {
        401: 'Invalid OpenRouter API key or authorization failed.',
        403: 'OpenRouter rejected this request.',
        429: 'OpenRouter rate limit. Free models can throttle requests; wait before retrying.',
        408: 'Request timed out or OpenRouter was slow. Try again later.',
        504: 'Request timed out or OpenRouter was slow. Try again later.',
      };
      return NextResponse.json({ error: errorMessages[response.status] || `OpenRouter HTTP ${response.status}: ${rawError}` }, { status: response.status });
    }

    const firstChoice = body.choices?.[0];
    const content = firstChoice?.message?.content;

    if (!content || content.trim() === '') {
      return NextResponse.json({ error: 'OpenRouter returned an empty response' }, { status: 502 });
    }

    return NextResponse.json({ content });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unknown error occurred';
    return NextResponse.json({ error: message }, { status: 500 });
  }
}