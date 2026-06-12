package com.aniplex.app.ui.main

import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DetailCompareTest {
    @Test
    fun compareDetails() {
        val ids = listOf("6245", "6467")
        for (id in ids) {
            val urlString = "https://aniplex-proxy.f1886391.workers.dev/api/v2/anime/$id"
            println("=== Querying details for '$id' ===")
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.setRequestProperty("Accept", "application/json")
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    response.append(line)
                    line = reader.readLine()
                }
                reader.close()
                println("Response for $id: ${response.toString()}")
            } catch (e: Exception) {
                println("Error querying $id: ${e.message}")
            }
        }
    }
}
