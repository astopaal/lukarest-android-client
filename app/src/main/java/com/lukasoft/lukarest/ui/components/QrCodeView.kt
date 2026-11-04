package com.lukasoft.lukarest.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.lukasoft.lukarest.ui.theme.White
import java.util.EnumMap

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    qrColor: Int = android.graphics.Color.parseColor("#0B2956"),
    backgroundColor: Int = android.graphics.Color.WHITE
) {
    val bitmap = remember(content, size) {
        generateQrBitmap(content, 512, qrColor, backgroundColor)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Kodu",
                modifier = Modifier.size(size)
            )
        }
    }
}

private fun generateQrBitmap(
    text: String,
    sizePx: Int,
    qrColor: Int,
    backgroundColor: Int
): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1) // Kenar boşluğu
        }
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) qrColor else backgroundColor
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

