package de.itscope.ai.mcp.config

import de.itscope.api.client.BusinesscartsApi
import de.itscope.api.client.BusinessdealsApi
import de.itscope.api.client.BusinessdealsPurchaseApi
import de.itscope.api.client.BusinessdealsSalesApi
import de.itscope.api.client.BusinessdocumentsApi
import de.itscope.api.client.BusinessquotesApi
import de.itscope.api.client.CompanyApi
import de.itscope.api.client.InfoApi
import de.itscope.api.client.ProductsApi
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ApiAccess(
    private val client: WebClient,
) {
    @Bean
    fun getProductsApi() = ProductsApi(client)

    @Bean
    fun getBusinesscartsApi() = BusinesscartsApi(client)

    @Bean
    fun getBusinessdealsApi() = BusinessdealsApi(client)

    @Bean
    fun getBusinessdealsPurchaseApi() = BusinessdealsPurchaseApi(client)

    @Bean
    fun getBusinessdealsSalesApi() = BusinessdealsSalesApi(client)

    @Bean
    fun getBusinessdocumentsApi() = BusinessdocumentsApi(client)

    @Bean
    fun getBusinessquotesApi() = BusinessquotesApi(client)

    @Bean
    fun getCompanyApi() = CompanyApi(client)

    @Bean
    fun getInfoApi() = InfoApi(client)
}