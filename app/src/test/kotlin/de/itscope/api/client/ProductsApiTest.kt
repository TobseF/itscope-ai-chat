package de.itscope.api.client

import de.itscope.api.client.ProductsApi.TypeQueryProductById
import de.itscope.api.client.ProductsApi.TypeQueryProducts
import de.itscope.api.client.ProductsApi.ViewQueryProductById
import de.itscope.api.client.ProductsApi.ViewQueryProducts
import de.itscope.api.client.util.WebClientFactoryTestConfig
import de.itscope.api.dto.Product
import de.itscope.api.dto.ProductResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Test for [ProductsApi]
 */
class ProductsApiTest {
    @Test
    fun `test queryProductById`() {
        val client = WebClientFactoryTestConfig.createWebClient()
        val productsApi = ProductsApi(client)
        val productID = 20480406000

        val productResponse: ProductResponse? =
            productsApi
                .queryProductById(
                    productID.toString(),
                    TypeQueryProductById.json,
                    ViewQueryProductById.standard,
                ).block()
        val product: Product = productResponse?.product?.first()!!

        assertEquals(product.puid, productID)
        assertEquals(product.ean, "0198156944993")
        assertContains(product.productName, "Lenovo ThinkPad")
    }

    @Test
    fun `test queryProducts`() {
        val client = WebClientFactoryTestConfig.createWebClient()
        val productsApi = ProductsApi(client)
        val ean = "0198156944993"
        val searchTerm = "ean=$ean"

        val productResponse: ProductResponse? =
            productsApi.queryProducts(searchTerm, TypeQueryProducts.json, ViewQueryProducts.standard).block()
        val product: Product = productResponse?.product?.first()!!

        assertEquals(product.puid, 20480406000)
        assertEquals(product.ean, "0198156944993")
        assertContains(product.productName, "Lenovo ThinkPad")
    }
}