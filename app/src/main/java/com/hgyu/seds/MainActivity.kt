package com.hgyu.seds


import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var shapeFieldView: ShapeFieldView
    private var responseSizeKB : Float = 0f;
    private lateinit var timerText: TextView
    private var secondsElapsed = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var radiusSeekBar: SeekBar
    private val timerRunnable = object : Runnable {
        override fun run() {
            val hours = secondsElapsed / 3600
            val minutes = (secondsElapsed % 3600) / 60
            val seconds = secondsElapsed % 60

            // Make last two digits of each group larger and colorful
            val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            val styled = SpannableString(time)

            styled.setSpan(
                ForegroundColorSpan(Color.GREEN),
                6, 8, // seconds
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            styled.setSpan(
                ForegroundColorSpan(Color.YELLOW),
                3, 5, // minutes
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            styled.setSpan(
                ForegroundColorSpan(Color.CYAN),
                0, 2, // hours
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            styled.setSpan(
                RelativeSizeSpan(1.5f),
                6, 8,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            timerText.text = styled

            secondsElapsed++
            handler.postDelayed(this, 1000)
        }
    }
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.setDecorFitsSystemWindows(false)}
        }else {
            // Fullscreen immersive
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)
        shapeFieldView = findViewById(R.id.shapeField)
        var ff :TextView = findViewById(R.id.textView)

        responseSizeKB = intent.getFloatExtra("sized",0f)
        val sresult = intent.getSerializableExtra("splash_result") as ArrayList<List<Dinosaur>>
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        shapeFieldView.dinos = sresult.toList()[0].toMutableList()
        ff.text = (ff.text.toString().toFloat() + responseSizeKB).toString()

        shapeFieldView.generateShapes(width,height)

        progressBar = findViewById(R.id.progressBar)

        timerText = findViewById(R.id.timerText)

        handler.post(timerRunnable)

        radiusSeekBar = findViewById(R.id.radiusSeekBar)
        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //shapeFieldView.interactionCircleRadius = progress.toFloat()
                if(shapeFieldView.selectedShape != null)
                    shapeFieldView.scale = progress / 100f
                else shapeFieldView.interactionCircleRadius = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        findViewById<Button>(R.id.btnAdd).setOnClickListener {
                shapeFieldView.changeDirection()
        }
        findViewById<Button>(R.id.btnRemove).setOnClickListener {
            shapeFieldView.speedUp(lifecycleScope)
        }
        findViewById<Button>(R.id.btnfreez).setOnClickListener {
            shapeFieldView.pause(lifecycleScope)
        }
    }

}
