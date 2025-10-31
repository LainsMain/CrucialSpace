package com.crucialspace.app.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiReminder(
	val event: String,
	val datetime: String,
)

@JsonClass(generateAdapter = true)
data class AiResult(
    val title: String? = null,
	val summary: String,
	val todos: List<String>,
    val reminders: List<AiReminder>,
    val urls: List<String> = emptyList(),
    val embedding: List<Double>? = null,
    val transcript: String? = null,
    val collections: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class EmbedRequest(val text: String)

@JsonClass(generateAdapter = true)
data class EmbeddingResponse(val embedding: List<Double>)
