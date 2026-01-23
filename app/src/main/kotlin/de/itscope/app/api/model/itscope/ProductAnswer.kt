package de.itscope.app.api.model.itscope

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class ProductAnswer(
    /**
     * Eindeutiger Key
     * */
    val puid: Long,
    // Konfigurierbarer Produktbezeichner
    @get:JsonProperty("productName")
    val productName: String,
    /**
     *  Bezeichner des Produkttyps. Kann als 2. <a href=\"https://guide.itscope.com/kb/produktkategorien/\">Kategorieebene</a> verwendet werden. */
    val productTypeName: String,
    // Name der Gruppe von Produkttypen, z.B. Netzwerktechnik. Kann als 1. <a href=\"https://guide.itscope.com/kb/produktkategorien/\">Kategorieebene</a> verwendet werden.
    @get:JsonProperty("productTypeGroupName")
    val productTypeGroupName: String,
    // EAN
    @get:JsonProperty("ean")
    val ean: String,
    /**
     /* Preisbasis für den <a href=\"https://guide.itscope.com/kb/herkunft-der-preis-und-bestandsinformation/\">kalkulierten Preis</a> */
     */
    @get:JsonProperty("price")
    val price: String,
    // Bestandsmenge des in dieser Struktur angegebenen Lieferstatus
    @get:JsonProperty("stock")
    val stock: Int? = null,
    /** <a href=\"https://guide.itscope.com/kb/technische-eigenschaften-im-html-format-htmlspecs/\">Technische Eigenschaften</a> des Produktes, im HTML Format */
    @get:JsonProperty("htmlSpecs")
    val htmlSpecs: String? = null,
    /** <a href=\"https://guide.itscope.com/kb/technische-eigenschaften-im-html-format-htmlspecs/\">Technische Eigenschaften</a> des Produktes in Kurzform, getrennt mit br-Tags */
    @get:JsonProperty("htmlMainSpecs")
    val htmlMainSpecs: String = "",
    /** <a href=\"https://guide.itscope.com/kb/produktbezeichner-und-produkttexte/\">Marketingtext</a> für das Produkt */
    @get:JsonProperty("marketingText")
    val marketingText: String = "",
)