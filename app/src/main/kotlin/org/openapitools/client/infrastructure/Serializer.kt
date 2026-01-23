package org.openapitools.client.infrastructure

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonSetter.Value
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Serializer {
    @JvmStatic
    val jacksonObjectMapper: ObjectMapper = jacksonObjectMapper()
        .findAndRegisterModules()
        .setSerializationInclusion(Include.NON_ABSENT)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .apply {
            configOverride(List::class.java)
                .setSetterInfo(Value.forValueNulls(Nulls.AS_EMPTY))
        }
}
