package com.melody.app.media

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * ML Kit 翻译器（英文 → 中文，离线模型）
 *
 * 首次使用自动下载英中翻译模型（约30MB），之后离线工作。
 * 无需 API key，完全免费，数据不出设备。
 */
object NewsTranslator {

    private var translator: com.google.mlkit.nl.translate.Translator? = null
    private var isModelReady = false

    /**
     * 初始化翻译器并确保模型已下载
     * @return true 表示模型就绪可翻译
     */
    suspend fun ensureReady(): Boolean = withContext(Dispatchers.IO) {
        if (isModelReady && translator != null) return@withContext true

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.CHINESE)
            .build()
        translator = Translation.getClient(options)

        // 确保模型已下载（允许移动网络下载，不强制 WiFi）
        val conditions = DownloadConditions.Builder()
            .build()

        suspendCancellableCoroutine<Unit> { cont ->
            translator?.downloadModelIfNeeded(conditions)
                ?.addOnSuccessListener {
                    isModelReady = true
                    if (cont.isActive) cont.resume(Unit)
                }
                ?.addOnFailureListener {
                    // 模型下载失败（无 GMS / 无网络）
                    isModelReady = false
                    if (cont.isActive) cont.resume(Unit)
                }
        }
        isModelReady
    }

    /**
     * 翻译文本（英文 → 中文）
     * @return 翻译后的中文；如果模型未就绪或失败，返回原文
     */
    suspend fun translate(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        if (!isModelReady) {
            val ready = ensureReady()
            if (!ready) return@withContext text  // 模型不可用，返回原文
        }

        val t = translator ?: return@withContext text
        suspendCancellableCoroutine { cont ->
            t.translate(text)
                .addOnSuccessListener { translated ->
                    if (cont.isActive) cont.resume(translated)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(text)  // 翻译失败返回原文
                }
        }
    }

    /**
     * 判断文本是否可能是英文（简单启发式：含较多拉丁字母）
     */
    fun isLikelyEnglish(text: String): Boolean {
        if (text.isBlank()) return false
        val latinChars = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val cjkChars = text.count { it in '\u4e00'..'\u9fff' }
        // 拉丁字母多于中文字符 → 认为是英文
        return latinChars > cjkChars * 2 && latinChars > 5
    }

    /**
     * 释放资源
     */
    fun release() {
        translator?.close()
        translator = null
        isModelReady = false
    }
}
