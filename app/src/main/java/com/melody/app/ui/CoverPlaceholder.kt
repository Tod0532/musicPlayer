package com.melody.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 封面占位组件（升级版：多层渐变 + 对角高光 + 音符图标）
 * 传入 coverUri 时用 Coil 加载真实封面，无值时降级渐变占位
 */
@Composable
fun CoverPlaceholder(
    modifier: Modifier = Modifier,
    startColor: Color,
    endColor: Color,
    cornerRadius: Dp = 8.dp,
    iconSize: Dp = 24.dp,
    coverUri: String? = null   // 真实封面 URI（有则用 Coil 加载，无则渐变占位）
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        startColor,
                        blendColor(startColor, endColor, 0.5f),
                        endColor
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 对角线高光（模拟封面光泽）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                    )
                )
        )
        // 底部暗影（增加立体感）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
        )
        // 真实封面（有 coverUri 时用 Coil 加载，覆盖渐变占位层）
        if (coverUri != null) {
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(coverUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 无封面时显示音符图标
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * 颜色混合工具
 */
private fun blendColor(c1: Color, c2: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = c1.red + (c2.red - c1.red) * r,
        green = c1.green + (c2.green - c1.green) * r,
        blue = c1.blue + (c2.blue - c1.blue) * r,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * r
    )
}
