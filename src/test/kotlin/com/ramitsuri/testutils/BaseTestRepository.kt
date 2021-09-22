package com.ramitsuri.testutils

open class BaseTestRepository<T> {
    private var intId: Int = 0
    val storage = mutableMapOf<String, T>()

    protected fun getNewId(): String {
        intId++
        return intId.toString()
    }
}