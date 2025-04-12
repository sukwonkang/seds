package com.hgyu.seds

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class ShapeFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var blobsInCircle = 0
    private val counterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 58f
        textAlign = Paint.Align.CENTER
    }
    private var downX = 0f
    private var downY = 0f
    private var wasMoved = false

    private var centerCircleX = 0f
    private var centerCircleY = 0f
    private val centerCircleRadius = 333f
    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private var isMovingCenterCircle = false
    var selectedShape: AbstractShape? = null
    private val shapes = mutableListOf<AbstractShape>()
    var dinos = mutableListOf<Dinosaur>()
    private val colors = listOf(
        Color.rgb(189, 226, 255),
        Color.rgb(144, 238, 144),
        Color.rgb(255, 192, 203),
        Color.rgb(255, 222, 173),
        Color.rgb(176, 224, 230),
        Color.rgb(255, 250, 205),
        Color.rgb(186, 85, 211),
        Color.rgb(152, 251, 152),
        Color.rgb(230, 230, 250),
        Color.rgb(255, 239, 213),
        Color.rgb(119, 136, 153),
        Color.rgb(240, 255, 255)
    )
     var scale = 1f
    private val animator = object : Runnable {
        override fun run() {
            shapes.forEach { it.update(scale) }
            invalidate()
            postDelayed(this, 16L)
        }
    }

    var interactionCircleRadius: Float = 150f
        set(value) {
            field = value
            invalidate()
        }

    init {
        setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    wasMoved = false
                    val dx = event.x - centerCircleX
                    val dy = event.y - centerCircleY

                    var touchedShape: AbstractShape? = null
                    for (shape in shapes.reversed()) {
                        if (shape.checkHit(event.x, event.y)) {
                            touchedShape = shape
                            break
                        }
                    }

                    selectedShape = touchedShape
                    shapes.forEach { shape ->
                        if (shape == selectedShape) shape.onSelect() else {shape.onDeselect()
                        scale = 1f}
                    }


                    if ((dx * dx + dy * dy < interactionCircleRadius * interactionCircleRadius) && selectedShape == null) {
                        isMovingCenterCircle = true
                    }

                    invalidate()
                }

                MotionEvent.ACTION_MOVE -> {
                    val movedDist = (event.x - downX) * (event.x - downX) + (event.y - downY) * (event.y - downY)
                    if (movedDist > 100f) wasMoved = true
                    var shapeFollow = false
                    shapes.find { it.isSelected() }?.let { selected ->
                        if (selected.checkHit(event.x, event.y)) {
                            shapeFollow = true
                            selected.x = event.x
                            selected.y = event.y
                        }
                    }

                    if (isMovingCenterCircle && !shapeFollow) {
                        centerCircleX = event.x
                        centerCircleY = event.y
                    }

                    invalidate()
                }

                MotionEvent.ACTION_UP -> {
                    isMovingCenterCircle = false


                    // Only show image if shape was tapped (not dragged)
                    if (!wasMoved && selectedShape is RandomBlobShape) {
                        val shape = selectedShape as RandomBlobShape
                        if (shape.img.isNotEmpty()) {
                            showImagePopup(shape.img)
                        }
                    }
                }
            }

            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLayerType(LAYER_TYPE_HARDWARE, null)
        post(animator)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animator)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //centerCircleX = width / 2f
        //centerCircleY = height / 2f
        //generateShapes(w, h)
    }

    fun changeDirection() {
        selectedShape?.changeDirection()
    }

    fun speedUp(scope: CoroutineScope) {
        selectedShape?.speedUp(scope)
    }

    fun pause(scope: CoroutineScope) {
        selectedShape?.pause(scope)
    }

    fun generateShapes(width: Int, height: Int) {
        shapes.clear()
        val count = 5

        for (i in 0 until count) {
            val size = Random.nextFloat() * 80f + 30f
            val x = Random.nextFloat() * width
            val y = Random.nextFloat() * height
            val color = colors.random()
            var shape = RandomBlobShape(x, y, size, color, i.toString(),"", width, height)

            if (dinos.size == 5) {
                shape = RandomBlobShape(x, y, size, color, dinos[i].name,dinos[i].imageUrl, width, height)
            }

            shapes.add(shape)
        }
    }
    fun makeThumbnailUrl(ourl: String, width: Int): String {
        val url = URL(ourl.replaceFirst("http://", "https://"))
        val connection = url.openConnection() as HttpsURLConnection
        connection.instanceFollowRedirects = false
        connection.connect()

        var redirectedUrl = connection.getHeaderField("Location")
        connection.disconnect()
        redirectedUrl = resolveFinalImageUrl(redirectedUrl)
        if (redirectedUrl != null && redirectedUrl.contains("upload.wikimedia.org")) {
            val regex = Regex("""upload\.wikimedia\.org/wikipedia/commons/(\w)/(\w\w)/(.+?)$""")
            val match = regex.find(redirectedUrl)
            return if (match != null) {
                val (first, second, filename) = match.destructured
                "https://upload.wikimedia.org/wikipedia/commons/thumb/$first/$second/$filename/${width}px-$filename"
            } else {
                return ourl
            }
        }
        return ourl
    }
    fun resolveFinalImageUrl(startUrl: String): String {
        var currentUrl = startUrl
        var redirecting = true

        while (redirecting) {
            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connect()

            val redirect = connection.getHeaderField("Location")
            if (redirect != null) {
                currentUrl = redirect
            } else {
                redirecting = false
            }

            connection.disconnect()
        }

        return currentUrl
    }

    private fun showImagePopup(imageUrl: String) {

        var mm  = context as? Activity
        var progressBar: ProgressBar? = mm?.findViewById(R.id.progressBar)
        var ff:TextView? = mm?.findViewById(R.id.textView)
        progressBar?.visibility = View.VISIBLE
        val builder = android.app.AlertDialog.Builder(context)
        val imageView = android.widget.ImageView(context)

        val secureUrl = if (imageUrl.startsWith("http://")) {
            imageUrl.replaceFirst("http://", "https://")
        } else {
            imageUrl
        }

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = java.net.URL(makeThumbnailUrl(secureUrl,600))
                    val connection = url.openConnection() as javax.net.ssl.HttpsURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val byteArray = input.readBytes()
                    val sizeKB = byteArray.size / (1024.0 * 1024.0)
                    BitmapFactory.Options().apply {
                        inSampleSize = 4 // or 4, depending on image size
                    }
                    val bitmap =BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                        builder.setView(imageView)

                        mm?.runOnUiThread {
                            ff?.text = (ff?.text.toString().toFloat() + sizeKB).toString()
                            progressBar?.visibility = View.GONE
                        }

                        builder.setPositiveButton("Close", null)
                        builder.show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val snapshot = shapes.toList() // Prevent concurrent modification

        blobsInCircle = snapshot.count { shape ->
            val dx = shape.x - centerCircleX
            val dy = shape.y - centerCircleY
            val distance = sqrt(dx * dx + dy * dy)
            distance + shape.baseSize * 0.5f < interactionCircleRadius
        }

        canvas.drawCircle(centerCircleX, centerCircleY, interactionCircleRadius, centerCirclePaint)
        canvas.drawText("$blobsInCircle", width / 10f, height / 20f, counterPaint)

        for (shape in snapshot) {
            shape.draw(canvas)
        }
    }

}
