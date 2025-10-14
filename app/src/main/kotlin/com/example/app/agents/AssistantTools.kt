package com.example.app.agents

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.example.app.api.model.itscope.ProductAnswer
import de.itscope.ai.mcp.config.ApiAccess
import de.itscope.api.client.ProductsApi.TypeQueryProductByEan
import de.itscope.api.client.ProductsApi.TypeQueryProductById
import de.itscope.api.client.ProductsApi.TypeQueryProducts
import de.itscope.api.client.ProductsApi.ViewQueryProductByEan
import de.itscope.api.client.ProductsApi.ViewQueryProductById
import de.itscope.api.client.ProductsApi.ViewQueryProducts
import de.itscope.api.dto.Product
import de.itscope.api.dto.ProductResponse
import reactor.core.publisher.Mono

@Suppress("unused")
class AssistantTools(
    itscopeAPI: ApiAccess,
) : ToolSet {
    val productsApi = itscopeAPI.getProductsApi()

    @Tool
    @LLMDescription("Gibt das beliebteste Notebook auf ITscope zurück")
    fun getBestNotebook(): String = "Dell Pro 16 PC16250 - Intel Core 5 (ITscope-iD: 20764936000)"

    @Tool
    @LLMDescription(
        "Sucht ein Produkt auf der ITscope Platform anhand seiner ITscope-ID. " +
            "Im Ergebnis sind Produktinformationen und Preis enthalten.",
    )
    fun getProductByItscopeId(
        @LLMDescription("ITscope ID")
        id: Long,
    ): ProductAnswer? =
        productsApi
            .queryProductById(
                id.toString(),
                TypeQueryProductById.json,
                ViewQueryProductById.standard,
            ).getProductAnswer()

    @Tool
    @LLMDescription(
        "Sucht ein Produkt auf ITscope über Freitext. " +
            "Im Ergebnis sind Produktinformationen und Preis enthalten.",
    )
    fun searchProduct(
        @LLMDescription("Freitext Suchbegriffe")
        term: String,
    ): ProductAnswer? =
        productsApi.queryProducts(term, TypeQueryProducts.json, ViewQueryProducts.standard).getProductAnswer()

    @Tool
    @LLMDescription(
        "Sucht ein Produkt auf ITscope anhand seiner EAN." +
            "Im Ergebnis sind Produktinformationen und Preis enthalten.",
    )
    fun getProductByProductEAN(
        @LLMDescription("Produkt EAN")
        ean: String,
    ): ProductAnswer? =
        productsApi
            .queryProductByEan(ean, TypeQueryProductByEan.json, ViewQueryProductByEan.standard)
            .getProductAnswer()

    fun Mono<ProductResponse>.getProduct(): Product? = this.block()?.product?.first()

    fun Mono<ProductResponse>.getProductAnswer(): ProductAnswer? = this.getProduct()?.mapToAnswer()

    fun Product.mapToAnswer(): ProductAnswer =
        ProductAnswer(
            puid = this.puid,
            ean = this.ean ?: "",
            price = this.price.toString() + " " + this.currencyCode,
            stock = this.stock,
            productName = this.productName,
            productTypeName = this.productTypeName,
            productTypeGroupName = this.productTypeGroupName,
            htmlSpecs = this.htmlSpecs,
            htmlMainSpecs = this.htmlMainSpecs ?: "",
            marketingText = this.marketingText ?: "",
        )
}