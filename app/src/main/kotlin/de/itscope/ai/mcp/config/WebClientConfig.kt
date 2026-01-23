package de.itscope.ai.mcp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Config for a [WebClient].
 */
@Configuration
data class WebClientConfig(
    @Value("\${de.itscope.api.url}")
    val baseUrl: String,
    @Value("\${de.itscope.api.account.id}")
    val accountId: String,
    @Value("\${de.itscope.api.key}")
    val apiKey: String,
    @Value("\${de.itscope.api.useragent}")
    val userAgent: String
)

