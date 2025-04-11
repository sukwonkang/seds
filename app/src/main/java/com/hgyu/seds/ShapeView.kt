package com.hgyu.seds

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.hgyu.seds.R

class ShapeView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint()
    private val path = Path()

    init {
        paint.color = Color.argb(255, 100, 200, 150) // Color for the abstract shape
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Abstract Shape: Pulsing Circle
        canvas.save()

        val radius = (width / 2).toFloat()
        val cx = (width / 2).toFloat()
        val cy = (height / 2).toFloat()

        path.reset()
        path.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.drawPath(path, paint)

        // Example of a subtle animation: Pulsing the shape
        paint.alpha = (Math.abs(Math.sin(System.currentTimeMillis() / 1000.0)) * 255).toInt()

        canvas.restore()
    }
}
