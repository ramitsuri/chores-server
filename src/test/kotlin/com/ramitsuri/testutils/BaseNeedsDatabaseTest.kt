package com.ramitsuri.testutils

import com.ramitsuri.Constants
import com.ramitsuri.data.DatabaseFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
abstract class BaseNeedsDatabaseTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")
    lateinit var testAppContainer: TestAppContainer

    @Before
    open fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        DatabaseFactory.init(Constants.DB_URL_TEST, Constants.DB_DRIVER_TEST)
        testAppContainer = TestAppContainer()
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
        DatabaseFactory.clear()
    }
}