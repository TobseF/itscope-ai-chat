package de.itscope.api.client.util

import de.itscope.api.client.util.Credentials
import org.junit.jupiter.api.fail

object CredentialsFactory {


     fun getCredentials(): Credentials {
        val accountID: String? = System.getenv("ITSCOPE_ACCOUNT_ID")
        if (accountID.isNullOrEmpty()) {
            fail { "Missing user-name environment variable: ITSCOPE_ACCOUNT_ID" }
        }
        val apiKey = System.getenv("ITSCOPE_API_KEY")
        if (apiKey.isNullOrEmpty()) {
            fail { "Missing api-Key environment variable: ITSCOPE_API_KEY" }
        }
        return Credentials(accountID, apiKey)
    }
}