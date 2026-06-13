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

    if (!response.ok) {
      const rawError = await response.text();
      const errorMessages: Record<number, string> = {
        401: 'Invalid OpenRouter API key or authorization failed.',
        403: 'OpenRouter rejected this request.',
        429: 'OpenRouter rate limit. Free models can throttle requests; wait before retrying.',
        408: 'Request timed out or OpenRouter was slow. Try again later.',
        504: 'Request timed out or OpenRouter was slow. Try again later.',
      };
      if (response.status >= 500 && response.status < 600) {
        return NextResponse.json({ error: 'OpenRouter server error. Try again later.' }, { status: 502 });
      }
      return NextResponse.json({ error: errorMessages[response.status] || `OpenRouter HTTP ${response.status}: ${rawError}` }, { status: response.status });
    }

    const body = await response.json();
    
    if (body.error) {
      return NextResponse.json({ error: body.error.message || 'OpenRouter returned an error' }, { status: 400 });
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