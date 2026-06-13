# Product Context: OpenRouter Dialogue

## Why This Exists

OpenRouter Dialogue is a web application that demonstrates AI-to-AI conversation. It connects two different language models via the OpenRouter API and lets them have a back-and-forth dialogue, providing an interactive way to explore AI behavior and creativity.

## User Flow

1. User enters their OpenRouter API key
2. Selects two different models from the free model list
3. Configures the dialogue parameters (system prompt, max messages, temperature, etc.)
4. Clicks "Start" to begin the AI conversation loop
5. Watches the models exchange messages in real-time
6. Can "Stop" or "Clear" at any time

## Key UX Goals

- **Simple Setup**: Just enter API key and select models
- **Full Control**: Adjustable parameters for experimentation
- **Visual Distinction**: Each model's messages are clearly differentiated by color
- **Error Handling**: Clear error messages for API issues

## Integration Points

- **OpenRouter API**: Connects to chat/completions endpoint for model responses
- **Styling**: Tailwind CSS for clean, responsive UI