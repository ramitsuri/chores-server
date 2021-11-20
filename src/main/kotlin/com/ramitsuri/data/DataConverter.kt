package com.ramitsuri.data

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class InstantConverter: Converter<Instant, String> {
    override fun toStorage(main: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(main)
    }

    override fun toMain(storage: String): Instant {
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(storage))
    }
}

class UuidConverter: Converter<String, UUID> {
    override fun toStorage(main: String): UUID {
        return UUID.fromString(main)
    }

    override fun toMain(storage: UUID): String {
        return storage.toString()
    }
}

interface Converter<Main, Storage> {
    fun toStorage(main: Main): Storage
    fun toMain(storage: Storage): Main
}