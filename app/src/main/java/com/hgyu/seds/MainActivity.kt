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
    var responseSizeKB = 0f
    private lateinit var timerText: TextView
    private var secondsElapsed = 0
    private val client : OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60,TimeUnit.SECONDS)  // Set connect timeout to 30 seconds
        .readTimeout(60, TimeUnit.SECONDS)     // Set read timeout to 30 seconds
        .writeTimeout(60, TimeUnit.SECONDS)    // Optional: Set write timeout to 30 seconds
        .build()
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

        progressBar = findViewById(R.id.progressBar)
        progressBar?.visibility = View.VISIBLE
        getDinos { result ->
            // Handle the list of results here
            if (result.isNotEmpty()) {
                // You can now use this list for UI updates, e.g., displaying in a TextView
                shapeFieldView.dinos = result.toMutableList()
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels
                val width = displayMetrics.widthPixels
                shapeFieldView.generateShapes(width,height)
                runOnUiThread {
                    ff.text = (ff.text.toString().toFloat() + responseSizeKB).toString()
                    progressBar?.visibility = View.GONE
                }
            } else {
                // Handle empty or error result
            } }

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
    private fun getDinos(callback: (List<Dinosaur>) -> Unit) {
        val query = """
            SELECT ?dinosaur ?dinosaurLabel ?image WHERE {
              ?dinosaur wdt:P31 wd:Q23038290;          # Instance of Dinosaur
                       wdt:P18 ?image.             # Image property
              SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en". }
            }
            ORDER BY RAND()  # Randomize the results
            LIMIT 5
        """
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://query.wikidata.org/sparql?query=$encodedQuery&format=json"


        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "DinoExplorer/1.0")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
                //Toast.makeText(scopee,"$e",Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    val responseSizeBytes = jsonResponse?.toByteArray()?.size ?: 0
                    responseSizeKB =+ responseSizeBytes / 1024.0f
                    val jsonObject = JSONObject(jsonResponse)
                    val results = jsonObject.getJSONObject("results").getJSONArray("bindings")
                    val dinosaursList = mutableListOf<Dinosaur>()
                    for (i in 0 until results.length()) {
                        val result = results.getJSONObject(i)

                        // Extract dinosaur label (name)
                        val dinosaurLabel = result.getJSONObject("dinosaurLabel").getString("value")

                        // Extract image URL (if available)
                        val imageUrl = result.getJSONObject("image").getString("value")

                        // Add the dinosaur data to the list
                        dinosaursList.add(Dinosaur(dinosaurLabel, imageUrl))
                    }

                    // Return the list of dinosaurs through the callback
                    callback(dinosaursList)
                }
            }
        })
    }
}
