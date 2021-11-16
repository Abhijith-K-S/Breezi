package com.example.breezi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object StreamDetails {
    data class StreamData(val id: Int, val streamName: String, val streamURL: String)

    val streamList = listOf(
        StreamData(0, "ChillSky", "https://lfhh.radioca.st/stream"),
        StreamData(1, "Lauft FM", "https://lofi.stream.laut.fm/lofi"),
        StreamData(2, "Planet Lofi", "http://198.245.60.88:8080"),
        StreamData(3, "KauteMusik FM","http://de-hz-fal-stream07.rautemusik.fm/study"),
        StreamData(4, "BFlash", "http://bardia.cloud:8000/stream/1/")
    )

    val artworkUrl = listOf(
        "https://images.unsplash.com/photo-1620926987470-8b07366a88b0?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=687&q=80",
        "https://images.pexels.com/photos/9901513/pexels-photo-9901513.jpeg?cs=srgb&dl=pexels-erik-mclean-9901513.jpg&fm=jpg",
        "https://images.pexels.com/photos/2289236/pexels-photo-2289236.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=650&w=940",
        "https://images.pexels.com/photos/3052361/pexels-photo-3052361.jpeg",
        "https://images.unsplash.com/photo-1619301694814-1528ad10b5c1?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=685&q=80"
    )

    const val sharedPreferencesFileName = "SHARED_PREFERENCES"
    const val streamIndex = "STREAM_INDEX"
    const val isPlaying = "IS_PLAYING"

    const val notificationID = 101

    const val startStream = "START_SERVICE"
    const val stopStream = "STOP_SERVICE"

    const val prepSupport = "ON_PREPARE_SUPPORT"
    const val errorSupport ="ON_ERROR_SUPPORT"

    const val notificationPrevious = "NOTIFICATION_PREVIOUS"
    const val notificationPlayback = "NOTIFICATION_PLAY_OR_PAUSE"
    const val notificationNext = "NOTIFICATION_NEXT"


    //function to check for network connectivity
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        }
        else
        {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}