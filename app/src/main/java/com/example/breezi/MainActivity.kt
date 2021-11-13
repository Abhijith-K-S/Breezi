package com.example.breezi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val streamList = StreamDetails.streamList
    private val artworkList = StreamDetails.artworkList

    private val streamDataSize = streamList.size
    private var streamIndex = 0

    private lateinit var resultReceiver: ResultReceiver

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


    @SuppressLint("UseCompatLoadingForDrawables", "UnspecifiedImmutableFlag")
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
        backgroundArt.setBackgroundColor(ContextCompat.getColor(applicationContext,R.color.darkBlue))


        //hiding loadView
        loadView.visibility = View.GONE
        loadView.startAnimation(fadeOut)
        loadView.visibility = View.VISIBLE

        resetArtwork()

        //getting stream data from preferences
        loadStream()

        //function to control playback
        fun playStream(playBtnAnim: Boolean)
        {
            if(!isOnline(this))
                showToast("Please Check Your Network Connection")

            else if(!isPlaying) {
                label.startAnimation(fadeOut)
                loadView.startAnimation(fadeIn)
                GlobalScope.launch(Dispatchers.IO) {
                    isPlaying = true
                    musicServiceControl(StreamDetails.startStream,!playBtnAnim)
                }

                if (playBtnAnim)
                    buttonAnimation(actionButton, "pts")
            }

            else{
                GlobalScope.launch(Dispatchers.IO) {
                    isPlaying = false
                    musicServiceControl(StreamDetails.stopStream,true)
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
            playStream(true)
        }

        prevButtonCardView.setOnClickListener()
        {
            if(isOnline(this)) {
                GlobalScope.launch(Dispatchers.IO) {
                    streamIndex = (streamDataSize + streamIndex - 1) % streamDataSize
                }

                val playBtnAnim  = !isPlaying
                prevButton.startAnimation(bounce)
                buttonAnimation(actionButton, "stp")
                isPlaying = false
                playStream(playBtnAnim)
            }
            else
                showToast("No Connection")
        }

        nextButtonCardView.setOnClickListener()
        {
            if(isOnline(this)) {
                GlobalScope.launch(Dispatchers.IO) {
                    streamIndex = (streamIndex + 1) % streamDataSize
                }
                val playBtnAnim = !isPlaying
                nextButton.startAnimation(bounce)
                isPlaying = false
                playStream(playBtnAnim)
            }
            else
                showToast("No Connection")
        }
    }


    //function to start the foreground service
    private fun musicServiceControl(controlCode: String, currentStream: Boolean) {
        val intent = Intent(this,ForegroundService::class.java)
        intent.putExtra("Stream Index",streamIndex)
        intent.putExtra("Control Status",controlCode)
        intent.putExtra("Current Stream",currentStream)
        intent.putExtra("resultReceiver",resultReceiver)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)
    }

    //function to save data to shared preferences
    private fun saveStream() {
        val preferences = this.getPreferences(Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt("streamIndex",streamIndex)
        editor.putBoolean("isPlaying",isPlaying)
        editor.apply()
    }


    //function to load data from shared preferences
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun loadStream() {
        val preferences = this.getPreferences(Context.MODE_PRIVATE)
        streamIndex = preferences.getInt("streamIndex",0)
        isPlaying = preferences.getBoolean("isPlaying",false)

        if(isPlaying) {
            actionButton.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.play_to_stop_anim))
            artworkWindow.setImageResource(artworkList[streamIndex])
            backgroundArt.setImageResource(artworkList[streamIndex])
        }

        else {
            actionButton.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.stop_to_play_anim))
            resetArtwork()
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        loadStream()
        loadView.visibility = View.GONE
        loadView.startAnimation(fadeOut)
        loadView.visibility = View.VISIBLE


        //receives
        resultReceiver = object: ResultReceiver(Handler(Looper.myLooper()!!)) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                super.onReceiveResult(resultCode, resultData)
                Log.d("tag","working")
                when(resultCode) {
                    StreamDetails.prepSupport.id -> onPreparedSupport()
                    StreamDetails.errorSupport.id -> onErrorSupport()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveStream()
    }


    fun onPreparedSupport() {
        label.text = streamList[streamIndex].streamName
        loadView.startAnimation(fadeOut)
        label.startAnimation(fadeIn)
        artworkWindow.setImageResource(artworkList[streamIndex])
        backgroundArt.setImageResource(artworkList[streamIndex])
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onErrorSupport() {
        showToast("MediaPlayer Error")
        isPlaying = false
        buttonAnimation(actionButton,"stp")
        label.text = getString(R.string.now_playing_header_text)
        resetArtwork()
    }
}