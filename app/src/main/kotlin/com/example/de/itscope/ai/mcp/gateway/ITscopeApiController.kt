package de.itscope.ai.mcp.gateway

import de.itscope.api.client.ProductsApi
import de.itscope.api.client.ProductsApi.TypeQueryProductById
import de.itscope.api.client.ProductsApi.TypeQueryProducts
import de.itscope.api.client.ProductsApi.ViewQueryProductById
import de.itscope.api.client.ProductsApi.ViewQueryProducts
import de.itscope.api.dto.ProductResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/itscope")
class ITscopeApiController(
    private val productsApi: ProductsApi
) {

    @GetMapping("/product/{id}")
    fun getProductByItscopeId(
        @PathVariable id: Long
    ): Mono<ProductResponse> {
        return productsApi.queryProductById(id.toString(), TypeQueryProductById.json, ViewQueryProductById.standard)
    }

    @GetMapping("/product/search")
    fun getProductByItscopeId(term: String
    ): Mono<ProductResponse> {
        return productsApi.queryProducts(term, TypeQueryProducts.json, ViewQueryProducts.standard)
    }
}