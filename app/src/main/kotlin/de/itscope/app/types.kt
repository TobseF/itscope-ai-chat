@file:Suppress("ktlint:standard:filename")

package de.itscope.app

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

typealias ChatSessionId = String

object Generators {
    @OptIn(ExperimentalUuidApi::class)
    internal fun randomSessionId(): String = Uuid.random().toHexString()
}