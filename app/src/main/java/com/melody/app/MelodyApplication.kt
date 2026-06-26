package com.melody.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.melody.app.media.AudioCoverFetcher

/**
 * Application 入口
 *
 * 注册 Coil ImageLoader，支持从音频文件提取内嵌封面
 */
class MelodyApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        System.err.println("MELODY_COVER: MelodyApplication.newImageLoader 注册 AudioCoverFetcher")
        return ImageLoader.Builder(this)
            .components {
                add(AudioCoverFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .build()
    }
}
