package com.ramitsuri.repository

import com.ramitsuri.Constants
import com.ramitsuri.data.DatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

open class BaseRepositoryTest {
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        DatabaseFactory.init(Constants.DB_URL_TEST, Constants.DB_DRIVER)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
        DatabaseFactory.clear()
    }
}