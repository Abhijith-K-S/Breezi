package com.example.breezi

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

    const val notificationID = 101
    const val  customBroadcast = "CUSTOM_BROADCAST"

    const val startStream = "START_SERVICE"
    const val stopStream = "STOP_SERVICE"

    data class MainActivitySupport(val id: Int,val code:String)
    val prepSupport = MainActivitySupport(201,"ON_PREPARE_SUPPORT")
    val errorSupport = MainActivitySupport(202,"ON_ERROR_SUPPORT")
}