'use client';

import { useState, useRef, useEffect } from 'react';

const FREE_MODELS = [
  'google/gemma-4-26b-a4b-it:free',
  'meta-llama/llama-3.3-70b-instruct:free',
  'qwen/qwen3-next-80b-a3b-instruct:free',
  'openai/gpt-oss-20b:free',
  'nvidia/nemotron-3-nano-30b-a3b:free',
];

type ModelRole = 'A' | 'B';

interface ChatMessage {
  id: string;
  role: ModelRole;
  model: string;
  content: string;
  timestamp: number;
}

type ConnectionStatus = 
  | { type: 'idle' }
  | { type: 'loading' }
  | { type: 'running' }
  | { type: 'stopped' }
  | { type: 'error'; message: string };

export default function Home() {
  const [apiKey, setApiKey] = useState('');
  const [modelA, setModelA] = useState('google/gemma-4-26b-a4b-it:free');
  const [modelB, setModelB] = useState('meta-llama/llama-3.3-70b-instruct:free');
  const [systemPrompt, setSystemPrompt] = useState(
    'Engage in a candid, imaginative, and direct dialogue. Be creative, concise, and responsive to the other model.'
  );
  const [maxMessages, setMaxMessages] = useState(100);
  const [turnDelay, setTurnDelay] = useState(1000);
  const [temperature, setTemperature] = useState(0.8);
  const [maxTokens, setMaxTokens] = useState(512);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState<ConnectionStatus>({ type: 'idle' });
  const [abortController, setAbortController] = useState<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleStart = async () => {
    if (!apiKey.trim()) {
      setStatus({ type: 'error', message: 'Enter an OpenRouter API key.' });
      return;
    }
    if (!modelA || !modelB) {
      setStatus({ type: 'error', message: 'Choose both models.' });
      return;
    }
    if (modelA === modelB) {
      setStatus({ type: 'error', message: 'Choose two different models.' });
      return;
    }

    setMessages([]);
    setStatus({ type: 'loading' });
    
    const controller = new AbortController();
    setAbortController(controller);

    try {
      const initialInput = 'Begin a creative dialogue.';
      const newMessages: ChatMessage[] = [];
      let currentInput = initialInput;
      let currentModel = modelA;

      for (let i = 0; i < maxMessages; i++) {
        if (controller.signal.aborted) {
          setStatus({ type: 'stopped' });
          return;
        }

        const response = await fetch('/api/dialogue/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            apiKey,
            model: currentModel,
            systemPrompt,
            input: currentInput,
            temperature,
            maxTokens,
          }),
        });

        if (!response.ok) {
          const error = await response.json();
          throw new Error(error.error || 'Request failed');
        }

        const data = await response.json();
        
        newMessages.push({
          id: `${Date.now()}-${i}`,
          role: currentModel === modelA ? 'A' : 'B',
          model: currentModel,
          content: data.content,
          timestamp: Date.now(),
        });
        setMessages([...newMessages]);

        currentInput = data.content;
        currentModel = currentModel === modelA ? modelB : modelA;

        await new Promise(r => setTimeout(r, turnDelay));
      }

      setStatus({ type: 'idle' });
    } catch (error) {
      if (controller.signal.aborted) return;
      const message = error instanceof Error ? error.message : 'Unknown error';
      setStatus({ type: 'error', message });
    } finally {
      setAbortController(null);
    }
  };

  const handleStop = () => {
    abortController?.abort();
  };

  const handleClear = () => {
    if (abortController) {
      abortController.abort();
    }
    setMessages([]);
    setStatus({ type: 'idle' });
  };

  const statusText = () => {
    switch (status.type) {
      case 'idle': return 'Idle';
      case 'loading': return 'Preparing...';
      case 'running': return 'Running';
      case 'stopped': return 'Stopped';
      case 'error': return `Error: ${status.message}`;
    }
  };

  const canStart = status.type !== 'loading' && status.type !== 'running';

  return (
    <main className="min-h-screen bg-neutral-900 text-white flex flex-col">
      <header className="p-4 border-b border-neutral-800">
        <h1 className="text-2xl font-bold">OpenRouter Dialogue</h1>
      </header>

      <div className="flex-1 overflow-y-auto p-4 max-w-4xl mx-auto w-full">
        <div className="space-y-4 mb-4">
          <input
            type="password"
            placeholder="OpenRouter API key"
            value={apiKey}
            onChange={e => setApiKey(e.target.value)}
            disabled={!canStart}
            className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded"
          />

          <div className="flex gap-4">
            <ModelSelector
              label="Model A"
              models={FREE_MODELS}
              value={modelA}
              onChange={setModelA}
              disabled={!canStart}
            />
            <ModelSelector
              label="Model B"
              models={FREE_MODELS}
              value={modelB}
              onChange={setModelB}
              disabled={!canStart}
            />
          </div>

          <textarea
            placeholder="System Prompt"
            value={systemPrompt}
            onChange={e => setSystemPrompt(e.target.value)}
            disabled={!canStart}
            className="w-full px-3 py-2 bg-neutral-800 border border-neutral-700 rounded min-h-[80px]"
          />

          <div className="grid grid-cols-4 gap-4">
            <input
              type="number"
              placeholder="Max messages"
              value={maxMessages}
              onChange={e => setMaxMessages(parseInt(e.target.value) || 100)}
              disabled={!canStart}
              className="px-3 py-2 bg-neutral-800 border border-neutral-700 rounded"
            />
            <input
              type="number"
              placeholder="Delay ms"
              value={turnDelay}
              onChange={e => setTurnDelay(parseInt(e.target.value) || 1000)}
              disabled={!canStart}
              className="px-3 py-2 bg-neutral-800 border border-neutral-700 rounded"
            />
            <input
              type="number"
              step="0.1"
              placeholder="Temperature"
              value={temperature}
              onChange={e => setTemperature(parseFloat(e.target.value) || 0.8)}
              disabled={!canStart}
              className="px-3 py-2 bg-neutral-800 border border-neutral-700 rounded"
            />
            <input
              type="number"
              placeholder="Max tokens"
              value={maxTokens}
              onChange={e => setMaxTokens(parseInt(e.target.value) || 512)}
              disabled={!canStart}
              className="px-3 py-2 bg-neutral-800 border border-neutral-700 rounded"
            />
          </div>

          <div className="flex gap-4">
            <button
              onClick={handleStart}
              disabled={!canStart}
              className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded"
            >
              Start
            </button>
            <button
              onClick={handleStop}
              disabled={status.type !== 'running'}
              className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 rounded"
            >
              Stop
            </button>
          </div>

          <button
            onClick={handleClear}
            disabled={messages.length === 0 && status.type !== 'error' && status.type !== 'stopped'}
            className="w-full px-4 py-2 bg-neutral-700 hover:bg-neutral-600 disabled:opacity-50 rounded"
          >
            Clear
          </button>

          <p className={`text-sm ${status.type === 'error' ? 'text-red-400' : 'text-neutral-400'}`}>
            {statusText()}
          </p>
        </div>

        <div className="space-y-3">
          {messages.length === 0 ? (
            <p className="text-neutral-500 text-center">Start the loop to see the conversation.</p>
          ) : (
            messages.map(msg => (
              <ModelBubble key={msg.id} message={msg} />
            ))
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>
    </main>
  );
}

function ModelSelector({
  label,
  models,
  value,
  onChange,
  disabled,
}: {
  label: string;
  models: string[];
  value: string;
  onChange: (v: string) => void;
  disabled: boolean;
}) {
  return (
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      disabled={disabled}
      className="flex-1 px-3 py-2 bg-neutral-800 border border-neutral-700 rounded"
    >
      {models.map(m => (
        <option key={m} value={m}>{m}</option>
      ))}
    </select>
  );
}

function ModelBubble({ message }: { message: ChatMessage }) {
  const isModelA = message.role === 'A';
  const bgColor = isModelA ? 'bg-blue-900/50' : 'bg-green-900/50';
  const align = isModelA ? 'justify-start' : 'justify-end';

  return (
    <div className={`flex ${align}`}>
      <div className={`max-w-xs p-3 rounded-lg ${bgColor}`}>
        <p className="text-xs opacity-70">{message.model}</p>
        <p className="text-sm mt-1">{message.content}</p>
        <p className="text-xs opacity-50 mt-2">{new Date(message.timestamp).toLocaleTimeString()}</p>
      </div>
    </div>
  );
}