package com.example.openrouterdialogue.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterService {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterChatRequest
    ): Response<OpenRouterChatResponse>
}
