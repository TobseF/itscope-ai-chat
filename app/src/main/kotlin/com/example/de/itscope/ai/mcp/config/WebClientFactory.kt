package de.itscope.ai.mcp.config

import org.openapitools.client.infrastructure.Serializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient


/**
 * Provides a configured [WebClient].
 */
@Configuration
class WebClientFactory {

    @Bean
    fun createWebClient(config: WebClientConfig): WebClient {
        return WebClient.builder()
            .baseUrl(config.baseUrl)
            .defaultCookie("cookieKey", "cookieValue")
            .defaultHeaders { it.setBasicAuth(config.accountId, config.apiKey) }
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT,config.apiKey)
            .codecs {
                it.defaultCodecs().jackson2JsonEncoder(
                    Jackson2JsonEncoder(
                        Serializer.jacksonObjectMapper,
                        MediaType.APPLICATION_JSON
                    )
                )
                it.defaultCodecs().jackson2JsonDecoder(
                    Jackson2JsonDecoder(
                        Serializer.jacksonObjectMapper,
                        MediaType.APPLICATION_JSON
                    )
                )
            }
            .build()
    }

    @Bean
    fun getObjectMapper() = Serializer.jacksonObjectMapper


}