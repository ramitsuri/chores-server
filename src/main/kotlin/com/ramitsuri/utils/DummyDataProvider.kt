package com.ramitsuri.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DummyDataProvider() {

    fun setup() {
        val sampleDb = File("data/sample.db")
        val realDb = File("data/data.db")
        try {
            Files.delete(realDb.toPath())
            Files.copy(sampleDb.toPath(), realDb.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            // Do nothing, file probably doesn't exist
        }
    }
}