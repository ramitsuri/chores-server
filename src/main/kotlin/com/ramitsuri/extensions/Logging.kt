package com.ramitsuri.extensions

import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject

fun <R: Any> R.logger(): Lazy<Logger> {
    return lazy {Logger.getLogger(unwrapCompanionClass(this.javaClass).name)}
}

fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

// unwrap companion class to enclosing class given a Kotlin Class
fun <T: Any> unwrapCompanionClass(ofClass: KClass<T>): KClass<*> {
    return unwrapCompanionClass(ofClass.java).kotlin
}

interface Loggable {
    val log: Logger
    fun logger(): Logger {
        return Logger.getLogger(unwrapCompanionClass(this.javaClass).name)
    }
}