package com.hgyu.seds.util

object SineLUT {
    private const val TABLE_SIZE = 360
    private val table = FloatArray(TABLE_SIZE) { i ->
        kotlin.math.sin(Math.toRadians(i.toDouble())).toFloat()
    }

    fun get(angle: Float): Float {
        val index = ((angle % TABLE_SIZE + TABLE_SIZE) % TABLE_SIZE).toInt()
        return table[index]
    }
}
