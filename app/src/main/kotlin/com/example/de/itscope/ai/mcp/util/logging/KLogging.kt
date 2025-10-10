package de.itscope.ai.mcp.util.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass


internal interface Klogging {
    val log: Logger
        get() = LoggerFactory.getLogger(classNameOf(this::class)) as Logger;
}

fun classNameOf(ownerClass: KClass<*>): String? =
    if (ownerClass.isCompanion) ownerClass.java.enclosingClass.name else ownerClass.java.name
