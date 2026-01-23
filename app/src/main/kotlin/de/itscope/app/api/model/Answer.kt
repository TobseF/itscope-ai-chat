package de.itscope.app.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

/**
 *
 * @param message The AI agent's response
 * @param chatSessionId Chat Session identifier  for the conversation
 * @param completed Is response complete
 * @param chatRequestId Chat Request identifier\\
 */
@Serializable
data class Answer(
    @get:JsonProperty("message", required = true) val message: String,
    // @get:Pattern(regexp = "^[_0-9a-f-]{16,64}$")
    @get:JsonProperty("chatSessionId", required = false) val chatSessionId: String,
    @get:JsonProperty("completed", required = true) val completed: Boolean,
    @get:JsonProperty("chatRequestId") val chatRequestId: String? = null,
)