package com.hgyu.seds.util

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

    }
}