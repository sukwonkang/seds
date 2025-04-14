package com.hgyu.seds.util

import com.google.firebase.firestore.FirebaseFirestore
import com.hgyu.seds.RandomBlobShape
import com.hgyu.seds.data.Dinosaur
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class Tools {
    companion object {
        @JvmStatic
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
        @JvmStatic
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
        @JvmStatic
        fun fetchShapesFromFirebase(db : FirebaseFirestore,onResult: (List<Dinosaur>) -> Unit) {
            db.collection("shapes").get()
                .addOnSuccessListener { snapshot ->
                    val shapes = snapshot.documents.mapNotNull {
                        it.toObject(Dinosaur::class.java)?.toDino()
                    }
                    onResult(shapes)
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    onResult(emptyList())
                }
        }
        @JvmStatic
        fun uploadShapesToFirebase(shapes: List<Dinosaur>, db : FirebaseFirestore) {
            val collection = db.collection("shapes")

            shapes.forEach { shape ->
                collection.document(shape.id).set(shape.toDTO())
            }
        }

    }
}