package com.example.openrouterdialogue.ui

import androidx.lifecycle.ViewModel
import com.example.openrouterdialogue.api.OpenRouterChatRequest
import com.example.openrouterdialogue.api.OpenRouterChatResponse
import com.example.openrouterdialogue.api.OpenRouterMessage
import com.example.openrouterdialogue.api.OpenRouterProvider
import com.example.openrouterdialogue.api.OpenRouterService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class OpenRouterViewModel(
    private val service: OpenRouterService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private companion object {
        const val DEFAULT_SYSTEM_PROMPT = "Engage in a candid, imaginative, and direct dialogue. Be creative, concise, and responsive to the other model."
    }

    data class DialogueConfig(
        val apiKey: String,
        val modelA: String = "openchat/openchat-7b:free",
        val modelB: String = "huggingfaceh4/zephyr-7b-beta:free",
        val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        val seedPrompt: String = "Begin a creative dialogue.",
        val maxMessages: Int = 100,
        val turnDelayMillis: Long = 1_000L,
        val temperature: Double = 0.8,
        val maxTokens: Int = 512
    )

    sealed class ConnectionStatus {
        data object Idle : ConnectionStatus()
        data object Loading : ConnectionStatus()
        data object Running : ConnectionStatus()
        data object Stopped : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }

    enum class ModelRole {
        A,
        B
    }

    data class ChatMessageUi(
        val id: Long = System.currentTimeMillis() * 1_000L + (System.nanoTime() and 0x3FFL),
        val role: ModelRole,
        val model: String,
        val content: String,
        val timestampMillis: Long = System.currentTimeMillis()
    ) {
        fun formattedTime(): String {
            return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(timestampMillis)
        }
    }

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(emptyList())
    val messages: StateFlow<List<ChatMessageUi>> = _messages.asStateFlow()

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private var loopJob: Job? = null

    fun start(config: DialogueConfig) {
        if (config.apiKey.isBlank()) {
            _status.value = ConnectionStatus.Error("Enter an OpenRouter API key.")
            return
        }

        if (config.modelA.isBlank() || config.modelB.isBlank()) {
            _status.value = ConnectionStatus.Error("Choose both models.")
            return
        }

        if (config.modelA == config.modelB) {
            _status.value = ConnectionStatus.Error("Choose two different models.")
            return
        }

        loopJob?.cancel()
        _messages.value = emptyList()
        _status.value = ConnectionStatus.Loading

        loopJob = viewModelScope.launch {
            runDialogueLoop(config)
        }
    }

    fun stop() {
        loopJob?.cancel()
        _status.value = ConnectionStatus.Stopped
    }

    fun clear() {
        loopJob?.cancel()
        _messages.value = emptyList()
        _status.value = ConnectionStatus.Idle
    }

    override fun onCleared() {
        loopJob?.cancel()
        super.onCleared()
    }

    private suspend fun runDialogueLoop(config: DialogueConfig) {
        val safeConfig = config.copy(
            maxMessages = config.maxMessages.coerceAtLeast(1)
        )

        val systemPrompt = safeConfig.systemPrompt.ifBlank {
            DEFAULT_SYSTEM_PROMPT
        }

        var input = safeConfig.seedPrompt.ifBlank {
            "Begin a creative dialogue."
        }

        var model = safeConfig.modelA
        var role = ModelRole.A

        _status.value = ConnectionStatus.Running

        try {
            while (currentCoroutineContext().isActive) {
                val responseText = callModel(
                    config = safeConfig,
                    model = model,
                    systemPrompt = systemPrompt,
                    input = input
                )

                appendMessage(
                    role = role,
                    model = model,
                    content = responseText,
                    maxMessages = safeConfig.maxMessages
                )

                input = responseText

                model = if (model == safeConfig.modelA) {
                    safeConfig.modelB
                } else {
                    safeConfig.modelA
                }

                role = if (role == ModelRole.A) {
                    ModelRole.B
                } else {
                    ModelRole.A
                }

                delay(safeConfig.turnDelayMillis.coerceAtLeast(250L))
            }
        } catch (e: CancellationException) {
            if (_status.value == ConnectionStatus.Running) {
                _status.value = ConnectionStatus.Stopped
            }
        } catch (e: Throwable) {
            _status.value = ConnectionStatus.Error(e.userMessage())
        }
    }

    private fun appendMessage(
        role: ModelRole,
        model: String,
        content: String,
        maxMessages: Int
    ) {
        _messages.value = (
            _messages.value + ChatMessageUi(
                role = role,
                model = model,
                content = content
            )
            ).takeLast(maxMessages)
    }

    private suspend fun callModel(
        config: DialogueConfig,
        model: String,
        systemPrompt: String,
        input: String
    ): String {
        val request = OpenRouterChatRequest(
            model = model,
            messages = listOf(
                OpenRouterMessage(
                    role = "system",
                    content = systemPrompt
                ),
                OpenRouterMessage(
                    role = "user",
                    content = input
                )
            ),
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            provider = OpenRouterProvider(sort = "throughput")
        )

        val response = withContext(ioDispatcher) {
            service.chat(
                authorization = "Bearer ${config.apiKey.trim()}",
                request = request
            )
        }

        response.ensureSuccessful()

        val body = response.body()

        body?.error?.let { error ->
            throw ApiException(error.message ?: "OpenRouter returned an error")
        }

        return body?.firstChoiceText()
            ?: throw ApiException("OpenRouter returned an empty response")
    }

    private fun Response<OpenRouterChatResponse>.ensureSuccessful() {
        if (isSuccessful) return

        val rawError = errorBody()?.string().orEmpty()

        val message = when (code()) {
            401 -> "Invalid OpenRouter API key or authorization failed."
            403 -> "OpenRouter rejected this request."
            429 -> "OpenRouter rate limit. Free models can throttle requests; wait before retrying."
            408, 504 -> "Request timed out or OpenRouter was slow. Try again later."
            in 500..599 -> "OpenRouter server error. Try again later."
            else -> "OpenRouter HTTP ${code()}: $rawError"
        }

        throw ApiException(message)
    }

    private fun OpenRouterChatResponse.firstChoiceText(): String? {
        return choices
            .firstOrNull()
            ?.message
            ?.content
            ?.takeIf { it.isNotBlank() }
    }

    private fun Throwable.userMessage(): String {
        return when (this) {
            is ApiException -> message ?: "OpenRouter request failed."
            is IOException -> "Network error or timeout. Check connection and retry."
            else -> localizedMessage ?: javaClass.simpleName
        }
    }

    private class ApiException(message: String) : Exception(message)
}
