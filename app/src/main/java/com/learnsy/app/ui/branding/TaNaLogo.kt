package com.learnsy.app.ui.branding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnsy.app.ui.theme.NunitoFontFamily
import kotlin.math.cos
import kotlin.math.sin

/**
 * Logo TA&NA: chữ "TA&NA" ở giữa (gradient tím-hồng, giống StudentLoginScreen.jsx)
 * kèm badge nhỏ hình "nguyên tử" (React-style) ở góc phải trên, có animation xoay.
 *
 * Dùng cho: launcher icon (bản PNG tĩnh ở drawable-*dpi/ic_launcher_foreground.png)
 * và các nơi hiển thị logo động trong app (màn hình login, splash...).
 */
@Composable
fun TaNaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    fontSize: TextUnit = 22.sp,
    animated: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "TA&NA",
            style = TextStyle(
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFF472B6))
                )
            )
        )

        // Badge logo react nhỏ ở góc phải trên
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            AtomBadge(size = size * 0.34f, badgeColor = Color(0xFFA855F7), animated = animated)
        }
    }
}

/**
 * Badge tròn nhỏ chứa logo "nguyên tử" 3 quỹ đạo — dùng độc lập cho icon app.
 * Khi [animated] = true: cả cụm quỹ đạo xoay chậm liên tục + 1 "electron" chạy dọc
 * mỗi quỹ đạo, giống hiệu ứng logo React quay.
 */
@Composable
fun AtomBadge(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    badgeColor: Color = Color(0xFFA855F7),
    backgroundColor: Color = Color.White,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "atomSpin")

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val electronAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "electron"
    )

    val rotationOffset = if (animated) spinAngle else 0f

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val rx = this.size.width * 0.31f
        val ry = this.size.height * 0.125f
        val nucleusR = this.size.width * 0.085f
        val electronR = this.size.width * 0.06f
        val stroke = Stroke(width = this.size.width * 0.055f)

        if (backgroundColor != Color.Transparent) {
            drawCircle(color = backgroundColor, radius = this.size.width / 2f, center = Offset(cx, cy))
        }

        listOf(0f, 60f, 120f).forEach { baseAngle ->
            rotate(degrees = baseAngle + rotationOffset, pivot = Offset(cx, cy)) {
                drawOval(
                    color = badgeColor,
                    topLeft = Offset(cx - rx, cy - ry),
                    size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f),
                    style = stroke
                )

                if (animated) {
                    // Electron chạy dọc theo quỹ đạo này
                    val rad = Math.toRadians(electronAngle.toDouble())
                    val ex = cx + rx * cos(rad).toFloat()
                    val ey = cy + ry * sin(rad).toFloat()
                    drawCircle(color = badgeColor, radius = electronR, center = Offset(ex, ey))
                }
            }
        }
        drawCircle(color = badgeColor, radius = nucleusR, center = Offset(cx, cy))
    }
}
