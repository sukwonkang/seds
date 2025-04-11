package com.hgyu.seds

import android.graphics.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.*
import kotlin.random.Random

abstract class AbstractShape(
    var x: Float,
    var y: Float,
    var baseSize: Float,
    var ccolor: Int,
    val id: String,
    var width: Int,
    val height: Int,
    val img: String
) {
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        this.color = ccolor
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 6f
        color = Color.WHITE
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    var scale = 1f
    private var t = 0f
    private var dx = Random.nextFloat() * 1f - 0.333f
    private var dy = Random.nextFloat() * 1f - 0.333f
    private var olddx = dx
    private var olddy = dy
    private var paused = false
    private var isSelected = false
    private var glowAlpha = 0

    fun update(i : Float) {
        // Breathing animation
        t += 0.05f


        // Slight drifting
        x += dx
        y += dy

        // Bounce off edges
        if (x < 0 || x > width) dx *= -1
        if (y < 0 || y > height) dy *= -1

        // Glow animation
        if (isSelected) {
            x += dx*1/3
            y += dy*1/3
            scale = (0.95f + 0.05f * sin(t))*i*2
            glowAlpha = (128 + 127 * sin(t * 2)).toInt()
        }else {
            scale = (0.95f + 0.05f * SineLUT.get(t))
        }
    }

    fun onSelect() {
        isSelected = true
    }
    fun changeDirection() {
        val d = dx
        dx = -dx
        dy= -dy
    }
     fun speedUp(scope: CoroutineScope) {
         dx *= 20/3
         dy *= 20/3
         scope.launch {
             delay(3000)
             dx =olddx
             dy = olddy
         }
    }
    fun speedDown() {
        dx /= (1 / 3)
        dy /= (1 / 3)
    }
    fun pause(scope: CoroutineScope) {
        dx = 0f
        dy = 0f
        scope.launch {
            delay(5000)
            dx =olddx
            dy = olddy
        }
    }
    fun isSelected(): Boolean {
        return isSelected
    }
    fun onDeselect() {
        isSelected = false
        glowAlpha = 0
    }

    fun checkHit(tapX: Float, tapY: Float): Boolean {
        val dx = tapX - x
        val dy = tapY - y
        val dist = sqrt(dx * dx + dy * dy)
        return dist < baseSize * scale
    }

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)

        if (isSelected) {
            glowPaint.alpha = glowAlpha
            drawShape(canvas, glowPaint)
        }

        drawShape(canvas, paint)
        canvas.restore()
    }

    protected abstract fun drawShape(canvas: Canvas, paint: Paint)
}
