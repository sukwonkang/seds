package com.hgyu.seds.com.hgyu.seds

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hgyu.seds.Dinosaur
import com.hgyu.seds.MainActivity
import com.hgyu.seds.R
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

class SplashActivity : AppCompatActivity() {
    var responseSizeKB = 0f
    var tdd : TextView? = null
    private val client : OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Set connect timeout to 30 seconds
        .readTimeout(60, TimeUnit.SECONDS)     // Set read timeout to 30 seconds
        .writeTimeout(60, TimeUnit.SECONDS)    // Optional: Set write timeout to 30 seconds
        .build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)
        tdd = findViewById(R.id.loading_text)
        getDinos { result ->
            // Handle the list of results here
            if (result.isNotEmpty()) {
                // You can now use this list for UI updates, e.g., displaying in a TextView

               runOnUiThread {
                   val intent = Intent(this, MainActivity::class.java)
                   intent.putExtra("splash_result", arrayListOf(result))
                   intent.putExtra("sized",responseSizeKB)
                   startActivity(intent)
                   finish()
               }

            } else {
                // Handle empty or error result
            } }
    }
    private fun getDinos(callback: (List<Dinosaur>) -> Unit) {
        runOnUiThread {tdd?.text = "Getting dinos from wiki data ..."}
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
                runOnUiThread { tdd?.text = "Net error man ..."}


                //Toast.makeText(scopee,"$e",Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {tdd?.text = "Getting dinos ready for action..."}



                    val jsonResponse = response.body?.string()
                    val responseSizeBytes = jsonResponse?.toByteArray()?.size ?: 0
                    responseSizeKB =+ responseSizeBytes / (1024.0f * 1024.0f)
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
                    runOnUiThread {tdd?.text = "dinos goooo ..."}



                    // Return the list of dinosaurs through the callback
                    callback(dinosaursList)
                }
            }
        })
    }
}
