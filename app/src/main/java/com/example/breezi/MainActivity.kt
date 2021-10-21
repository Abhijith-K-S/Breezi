package com.example.breezi

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
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
        StreamData(3,"KauteMusik FM","http://de-hz-fal-stream07.rautemusik.fm/study"),
        StreamData(4,"BFlash","http://bardia.cloud:8000/stream/1/")
    )

    private val artworkList = listOf(
        R.drawable.artwork1,
        R.drawable.artwork2,
        R.drawable.artwork3,
        R.drawable.artwork4,
        R.drawable.artwork5
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

    //function to reset artwork
    private fun resetArtwork() {
        artworkWindow.setImageResource(R.drawable.icon)
        backgroundArt.setImageResource(android.R.color.transparent)
    }

    private lateinit var actionButtonCardView: CardView
    private lateinit var actionButton: ImageView
    private lateinit var prevButtonCardView: CardView
    private lateinit var prevButton: ImageView
    private lateinit var nextButtonCardView: CardView
    private lateinit var nextButton: ImageView
    private lateinit var label: TextView
    private lateinit var loadView: ProgressBar
    private lateinit var artworkWindow: ImageSwitcher
    private lateinit var backgroundArt: ImageSwitcher

    private var isPlaying = false
    private lateinit var fadeIn: Animation
    private lateinit var fadeOut: Animation
    private lateinit var bounce: Animation

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        //initialise media player, buttons and views
        actionButtonCardView = findViewById(R.id.playBackCardView)
        actionButton = findViewById(R.id.actionButton)
        prevButtonCardView = findViewById(R.id.prevButtonCardView)
        prevButton = findViewById(R.id.prevButton)
        nextButtonCardView = findViewById(R.id.nextButtonCardView)
        nextButton = findViewById(R.id.nextButton)
        label = findViewById(R.id.nowPlayingHeader)
        loadView = findViewById(R.id.loadRing)
        artworkWindow = findViewById(R.id.artwork)
        backgroundArt = findViewById(R.id.backgroundArt)

        var mediaPlayer: MediaPlayer? = null

        //setting initial icon
        actionButton.setImageDrawable(getDrawable(R.drawable.play_to_stop_anim))

        //Initialising animations
        fadeIn = AnimationUtils.loadAnimation(this,R.anim.fade_in)
        fadeOut = AnimationUtils.loadAnimation(this,R.anim.fade_out)
        bounce = AnimationUtils.loadAnimation(this,R.anim.bounce)

        //Initialise artwork window and backgroundWindow
        artworkWindow.setFactory {
            val imgView = ImageView(applicationContext)
            imgView.scaleType = ImageView.ScaleType.CENTER_CROP
            imgView
        }
        artworkWindow.setInAnimation(applicationContext,R.anim.fade_in)
        artworkWindow.setOutAnimation(applicationContext,R.anim.fade_out)

        backgroundArt.setFactory {
            val imgView = ImageView(applicationContext)
            imgView.alpha = 0.5F
            imgView.adjustViewBounds = true
            imgView.scaleType = ImageView.ScaleType.FIT_XY
            imgView
        }
        backgroundArt.setInAnimation(applicationContext,R.anim.fade_in)
        backgroundArt.setOutAnimation(applicationContext,R.anim.fade_out)
        backgroundArt.setBackgroundColor(Color.parseColor("#102A43"))


        //hiding loadView
        loadView.visibility = View.GONE
        loadView.startAnimation(fadeOut)
        loadView.visibility = View.VISIBLE

        resetArtwork()

        //function to control playback
        fun playStream()
        {
            if(!isOnline(this))
                showToast("Please Check Your Network Connection")

            else if(!isPlaying) {
                label.startAnimation(fadeOut)
                loadView.startAnimation(fadeIn)
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
                }
                if(loadView.alpha>0) {
                    loadView.visibility = View.GONE
                    loadView.startAnimation(fadeOut)
                    loadView.visibility = View.VISIBLE
                }
                label.startAnimation(fadeOut)
                label.text = getString(R.string.now_playing_header_text)
                label.startAnimation(fadeIn)
                resetArtwork()
                buttonAnimation(actionButton,"stp")
            }
        }

        //stream controls
        actionButtonCardView.setOnClickListener()
        {
            playStream()
        }

        prevButtonCardView.setOnClickListener()
        {
            if(isOnline(this)) {
                GlobalScope.launch(Dispatchers.IO)
                {
                    streamIndex = (streamDataSize + streamIndex - 1) % streamDataSize
                    mediaPlayer?.reset()
                }
                prevButton.startAnimation(bounce)
                buttonAnimation(actionButton, "stp")
                isPlaying = false
                playStream()
            }
            else
                showToast("No Connection")
        }

        nextButtonCardView.setOnClickListener()
        {
            if(isOnline(this)) {
                GlobalScope.launch(Dispatchers.IO)
                {
                    streamIndex = (streamIndex + 1) % streamDataSize
                    mediaPlayer?.reset()
                }
                nextButton.startAnimation(bounce)
                buttonAnimation(actionButton, "stp")
                isPlaying = false
                playStream()
            }
            else
                showToast("No Connection")
        }
    }

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.start()
        label.text = streamList[streamIndex].streamName
        loadView.startAnimation(fadeOut)
        label.startAnimation(fadeIn)
        artworkWindow.setImageResource(artworkList[streamIndex])
        backgroundArt.setImageResource(artworkList[streamIndex])
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        showToast("MediaPlayer Error: $p1")
        p0?.reset()
        isPlaying = false
        buttonAnimation(actionButton,"stp")
        label.text = getString(R.string.now_playing_header_text)
        resetArtwork()
        return true
    }
}