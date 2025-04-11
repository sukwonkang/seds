package com.hgyu.seds

import android.graphics.*
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.*
import kotlin.random.Random

class RandomBlobShape(
    x: Float,
    y: Float,
    baseSize: Float,
    color: Int,
    id: String,
    img: String,
    width: Int,
    height: Int
) : AbstractShape(x, y, baseSize, color, id, width, height, img) {

    private val blobPath = Path()
    private val angles = List(12) { it * (2 * Math.PI / 12).toFloat() }
    private val radii = List(12) { baseSize * (0.8f + Random.nextFloat() * 0.4f) }

    private var bitmap: Bitmap? = null

    init {
        loadImageAsync(img)
    }

    private fun loadImageAsync(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val secureImg = if (url.startsWith("http://")) {
                    url.replaceFirst("http://", "https://")
                } else {
                    url
                }

                val connection = URL(secureImg).openConnection() as HttpsURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 16 // or 4, depending on image size
                }
                val bmp = BitmapFactory.decodeStream(inputStream,null,options)
                withContext(Dispatchers.Main) {
                    bitmap = bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun drawShape(canvas: Canvas, paint: Paint) {
        // Draw the blob shape
        blobPath.reset()
        angles.forEachIndexed { i, angle ->
            val r = radii[i]
            val px = cos(angle) * r
            val py = sin(angle) * r
            if (i == 0) {
                blobPath.moveTo(px, py)
            } else {
                blobPath.lineTo(px, py)
            }
        }
        blobPath.close()
        canvas.drawPath(blobPath, paint)

        // Draw the image if it's loaded
        bitmap?.let {
            val imgSize = baseSize * 1.2f
            val left = -imgSize / 2
            val top = -imgSize / 2
            val right = imgSize / 2
            val bottom = imgSize / 2
            val dstRect = RectF(left, top, right, bottom)
            canvas.drawBitmap(it, null, dstRect, null)
        }

        // Draw text in the center of the blob
        if (paint.style == Paint.Style.FILL_AND_STROKE) {
            // Glow effect (stroke)
            val glowPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 8f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                textSize = baseSize / 2.5f
            }

            // Main text paint
            val textPaint = Paint().apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                textSize = baseSize / 2.5f
            }

            val yOffset = (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(id, 0f, -yOffset, glowPaint)
            canvas.drawText(id, 0f, -yOffset, textPaint)
        }
    }
}
