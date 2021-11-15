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

    val artworkList = listOf(
        R.drawable.artwork1,
        R.drawable.artwork2,
        R.drawable.artwork3,
        R.drawable.artwork4,
        R.drawable.artwork5
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