package com.example.openrouterdialogue.api

import com.squareup.moshi.Json

data class OpenRouterChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenRouterMessage>,
    @Json(name = "temperature") val temperature: Double = 0.8,
    @Json(name = "max_tokens") val maxTokens: Int = 512,
    @Json(name = "provider") val provider: OpenRouterProvider = OpenRouterProvider(sort = "throughput")
)

data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

data class OpenRouterProvider(
    @Json(name = "sort") val sort: String = "throughput"
)

data class OpenRouterChatResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<OpenRouterChoice> = emptyList(),
    @Json(name = "error") val error: OpenRouterError? = null
)

data class OpenRouterChoice(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: OpenRouterMessage,
    @Json(name = "finish_reason") val finishReason: String? = null
)

data class OpenRouterError(
    @Json(name = "message") val message: String? = null
)
