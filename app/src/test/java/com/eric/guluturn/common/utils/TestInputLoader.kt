package com.eric.guluturn.common.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable
data class UserTestInput(val input: String)

object TestInputLoader {
    fun loadJsonInputs(resourceName: String): List<String> {
        val stream: InputStream = Thread.currentThread().contextClassLoader
            .getResourceAsStream(resourceName)
            ?: throw IllegalStateException("Missing test input file: $resourceName")

        val jsonString = stream.bufferedReader().use { it.readText() }
        val parser = Json { ignoreUnknownKeys = true }
        return parser.decodeFromString<List<UserTestInput>>(jsonString).map { it.input }
    }
}
