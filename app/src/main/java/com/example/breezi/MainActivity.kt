package com.example.breezi

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    data class StreamData(val id: Int, val streamName: String, val streamURL: String)

    private val streamList = listOf(
        StreamData(0,"ChillSky", "https://lfhh.radioca.st/stream"),
        StreamData(1,"Lauft FM", "https://lofi.stream.laut.fm/lofi"),
        StreamData(2,"Planet Lofi","http://198.245.60.88:8080"),
        StreamData(3,"KauteMusik FM Study","http://de-hz-fal-stream07.rautemusik.fm/study"),
        StreamData(4,"BFlash","http://bardia.cloud:8000/stream/1/")
    )

    private val streamDataSize = streamList.size
    private var streamIndex = 0

    private fun showToast(message: String)
    {
        val toast = Toast.makeText(this,message,Toast.LENGTH_SHORT)
        toast.show()
    }

    //function to check for network connectivity
    private fun isOnline(context: Context): Boolean {
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

    //function to play button animation
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("UseCompatLoadingForDrawables")
    fun buttonAnimation(actionButton: ImageView,action: String)
    {
        if(action=="stp")
            actionButton.setImageDrawable(getDrawable(R.drawable.stop_to_play_anim))
        else
            actionButton.setImageDrawable(getDrawable(R.drawable.play_to_stop_anim))

        val drawable = actionButton.drawable

        if(drawable is AnimatedVectorDrawableCompat)
            drawable.start()

        else{
            val anim = drawable as AnimatedVectorDrawable
            anim.start()
        }
    }

    private lateinit var actionButton: ImageView
    private lateinit var prevButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var label: Button
    private var isPlaying = false

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialise media player and buttons
        actionButton = findViewById(R.id.actionButton)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        label = findViewById(R.id.air)
        actionButton.setImageDrawable(getDrawable(R.drawable.play_to_stop_anim))

        var mediaPlayer: MediaPlayer? = null

        //function to control playback
        fun playStream()
        {
            if(!isOnline(this))
                showToast("Please Check Your Network Connection")

            else if(!isPlaying) {
                GlobalScope.launch(Dispatchers.IO) {
                    isPlaying = true
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(streamList[streamIndex].streamURL)
                        setOnPreparedListener(this@MainActivity)
                        setOnErrorListener(this@MainActivity)
                        prepareAsync()
                    }
                }
                buttonAnimation(actionButton,"pts")
            }

            else{
                GlobalScope.launch(Dispatchers.IO) {
                    if(mediaPlayer!=null)
                        mediaPlayer?.reset()
                    isPlaying = false
                    this@MainActivity.runOnUiThread {
                        label.text = mediaPlayer!!.isPlaying.toString()
                    }
                }

                buttonAnimation(actionButton,"stp")
            }
        }

        //stream controls
        actionButton.setOnClickListener()
        {
            playStream()
        }

        prevButton.setOnClickListener()
        {
            if(isOnline(this)) {
                GlobalScope.launch(Dispatchers.IO)
                {
                    streamIndex = (streamDataSize + streamIndex - 1) % streamDataSize
                    mediaPlayer?.reset()
                }
                buttonAnimation(actionButton, "stp")
                playStream()
            }
            else
                showToast("No Connection")
        }

        nextButton.setOnClickListener()
        {
            if(isOnline(this)) {
                GlobalScope.launch(Dispatchers.IO)
                {
                    streamIndex = (streamIndex + 1) % streamDataSize
                    mediaPlayer?.reset()
                }
                buttonAnimation(actionButton, "stp")
                playStream()
            }
            else
                showToast("No Connection")
        }
    }

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.start()
        label.text = streamList[streamIndex].streamName
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        showToast("MediaPlayer Error: $p1")
        p0?.reset()
        isPlaying = false
        buttonAnimation(actionButton,"stp")
        label.text = p0?.isPlaying.toString()
        return true
    }
}