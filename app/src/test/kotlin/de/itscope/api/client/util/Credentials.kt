package de.itscope.api.client.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class Credentials(
    val accountID: String,
    val  apiKey: String)