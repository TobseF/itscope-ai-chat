package de.itscope.api.client.util

import de.itscope.ai.mcp.config.WebClientConfig
import de.itscope.ai.mcp.config.WebClientFactory
import org.springframework.web.reactive.function.client.WebClient

object WebClientFactoryTestConfig {

    fun createWebClient(): WebClient {
        val baseUrl="https://api.itscope.com/2.1"
        val credentials = CredentialsFactory.getCredentials()
        val config = WebClientConfig(
            baseUrl = baseUrl,
            accountId =  credentials.accountID,
            apiKey =  credentials.apiKey,
            userAgent = "ITscope MCP Test Client"
            )
        return WebClientFactory().createWebClient(config)
    }
}