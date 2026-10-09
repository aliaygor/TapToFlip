package com.aliaygor.taptoflip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.cos
import kotlin.math.sin

/** A native hue rotation preserves sprite shading, white eyes and transparency. */
internal enum class FrogStyle(val tr: String, val en: String, val degrees: Float) {
    GREEN("Yeşil", "Green", 0f), BLUE("Mavi", "Blue", 120f),
    PINK("Pembe", "Pink", 240f), GOLD("Altın", "Gold", -55f),
    SPOTTED("Benekli", "Spotted", 120f), STRIPED("Çizgili", "Striped", -55f);
    private val matrix: FloatArray get() {
        val a = Math.toRadians(degrees.toDouble()); val c = cos(a).toFloat(); val s = sin(a).toFloat()
        return floatArrayOf(
            .213f + c*.787f - s*.213f, .715f - c*.715f - s*.715f, .072f - c*.072f + s*.928f, 0f, 0f,
            .213f - c*.213f + s*.143f, .715f + c*.285f + s*.140f, .072f - c*.072f - s*.283f, 0f, 0f,
            .213f - c*.213f - s*.787f, .715f - c*.715f + s*.715f, .072f + c*.928f + s*.072f, 0f, 0f,
            0f,0f,0f,1f,0f)
    }
    fun filter(): ColorFilter? = if (this == GREEN) null else ColorFilter.colorMatrix(ColorMatrix(matrix))
    fun color(source: Color): Color {
        val m = matrix
        fun channel(i: Int) = (m[i]*source.red + m[i+1]*source.green + m[i+2]*source.blue).coerceIn(0f,1f)
        return Color(channel(0), channel(5), channel(10), source.alpha)
    }
}

@androidx.compose.runtime.Composable
internal fun styledFrogBitmap(source: androidx.compose.ui.graphics.ImageBitmap, style: FrogStyle): androidx.compose.ui.graphics.ImageBitmap {
    return androidx.compose.runtime.remember(source, style) {
        if (style != FrogStyle.SPOTTED && style != FrogStyle.STRIPED) source
        else {
            val bitmap = source.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(170, 30, 75, 35)
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP)
            }
            val w = bitmap.width.toFloat(); val h = bitmap.height.toFloat()
            canvas.save()
            canvas.clipPath(android.graphics.Path().apply { addOval(w*.29f, h*.55f, w*.71f, h*.82f, android.graphics.Path.Direction.CW) })
            if (style == FrogStyle.SPOTTED) {
                for (row in 0..2) for (col in 0..3)
                    canvas.drawCircle(w*(.32f+col*.115f+(row%2)*.035f), h*(.59f+row*.085f), w*.022f, paint)
            } else {
                paint.strokeWidth = w*.025f
                for (i in 0..4) canvas.drawLine(w*(.25f+i*.12f), h*.54f, w*(.38f+i*.12f), h*.83f, paint)
            }
            canvas.restore()
            bitmap.asImageBitmap()
        }
    }
}
