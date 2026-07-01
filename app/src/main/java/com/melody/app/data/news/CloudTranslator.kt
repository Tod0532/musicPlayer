package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * 云端翻译器（MyMemory 免费 API）
 *
 * 无需 API key，免费额度 5000 词/天。
 * 国内可达，翻译质量优于 MLKit（逐句翻译 vs 逐词翻译）。
 */
object CloudTranslator {

    private const val API_URL = "https://api.mymemory.translated.net/get"

    /**
     * 翻译英文文本为中文
     * @return 翻译后的中文；失败返回原文
     */
    suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        // MyMemory 限制 500 字符/次，超长截断
        val toTranslate = if (text.length > 450) text.take(450) else text

        try {
            val encoded = URLEncoder.encode(toTranslate, "UTF-8")
            val urlStr = "$API_URL?q=$encoded&langpair=en|zh"

            val conn = (java.net.URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 Melody/1.0")
                connectTimeout = 5000
                readTimeout = 5000
            }

            // 5秒超时保护
            val result = withTimeoutOrNull(5000L) {
                if (conn.responseCode != 200) return@withTimeoutOrNull null
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val translated = json.optString("responseData", "")
                    .let { JSONObject(it).optString("translatedText", "") }
                if (translated.isNotBlank() && translated != toTranslate) translated else null
            }

            conn.disconnect()
            result ?: text  // 超时或失败返回原文
        } catch (_: Exception) {
            text  // 任何异常返回原文
        }
    }

    /**
     * 判断是否需要翻译（英文文本）
     */
    fun needsTranslation(text: String): Boolean {
        if (text.isBlank()) return false
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val cjk = text.count { it in '\u4e00'..'\u9fff' }
        return latin > cjk * 3 && latin > 10
    }
}
