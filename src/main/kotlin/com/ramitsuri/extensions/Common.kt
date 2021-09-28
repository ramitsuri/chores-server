package com.ramitsuri.extensions

import com.google.api.core.ApiFuture
import kotlinx.coroutines.delay

suspend fun <T> ApiFuture<T>.wait(): T {
    while (!isDone)
        delay(1) // Polling frequency
    return get()
}