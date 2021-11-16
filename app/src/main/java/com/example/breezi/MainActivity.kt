package com.example.breezi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val streamList = StreamDetails.streamList

    private val streamDataSize = streamList.size
    private var streamIndex = 0
    private var isPlaying = false


    //shared preferences values
    private lateinit var preferences : SharedPreferences
    private lateinit var editor : SharedPreferences.Editor


    private fun showToast(message: String)
    {
        val toast = Toast.makeText(this,message,Toast.LENGTH_SHORT)
        toast.show()
    }

    //function to check for network connectivity
    private fun isOnline(context: Context): Boolean {
        return StreamDetails.isOnline(context)
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
                    saveStream()
                    musicServiceControl(StreamDetails.startStream,!playBtnAnim)
                }

                if (playBtnAnim)
                    buttonAnimation(actionButton, "pts")
            }

            else{
                GlobalScope.launch(Dispatchers.IO) {
                    isPlaying = false
                    saveStream()
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
        intent.putExtra("Control Status",controlCode)
        intent.putExtra("Current Stream",currentStream)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)
    }

    //function to save data to shared preferences
    private fun saveStream() {
        editor.putInt(StreamDetails.streamIndex,streamIndex)
        editor.putBoolean(StreamDetails.isPlaying,isPlaying)
        editor.apply()
    }


    //function to load data from shared preferences
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun loadStream() {
        streamIndex = preferences.getInt(StreamDetails.streamIndex,0)
        isPlaying = preferences.getBoolean(StreamDetails.isPlaying,false)


        if(isPlaying) {
            label.text = streamList[streamIndex].streamName
            actionButton.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.play_to_stop_anim))
            glideImageLoader()
        }

        else {
            label.text = getString(R.string.app_name)
            actionButton.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.stop_to_play_anim))
            resetArtwork()
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()

        preferences = this.getSharedPreferences(StreamDetails.sharedPreferencesFileName,Context.MODE_PRIVATE)!!
        editor = preferences.edit()

        loadView.visibility = View.GONE
        loadView.startAnimation(fadeOut)
        loadView.visibility = View.VISIBLE

        loadStream()

        var initialRun = true
        ForegroundService.EventBus.observe(
            this, { customEvent ->
                if(initialRun)
                    initialRun = false
                else {
                    when (customEvent.eventCode) {
                        StreamDetails.prepSupport -> onPreparedSupport()
                        StreamDetails.errorSupport -> onErrorSupport()

                        StreamDetails.notificationPrevious,StreamDetails.notificationNext -> {
                            streamIndex = preferences.getInt(StreamDetails.streamIndex,0)

                            if(!isPlaying) {
                                buttonAnimation(actionButton, "pts")
                                isPlaying = true
                            }

                            label.startAnimation(fadeOut)
                            loadView.startAnimation(fadeIn)
                        }

                        StreamDetails.notificationPlayback -> {
                            loadStream()
                            when (isPlaying) {
                                false -> {
                                    if (loadView.alpha > 0) {
                                        loadView.visibility = View.GONE
                                        loadView.startAnimation(fadeOut)
                                        loadView.visibility = View.VISIBLE
                                    }
                                    label.startAnimation(fadeOut)
                                    label.text = getString(R.string.now_playing_header_text)
                                    label.startAnimation(fadeIn)
                                    resetArtwork()
                                    buttonAnimation(actionButton, "stp")
                                }

                                true -> {
                                    label.startAnimation(fadeOut)
                                    loadView.startAnimation(fadeIn)
                                    buttonAnimation(actionButton, "pts")
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        saveStream()
    }


    //function to load images
    private fun glideImageLoader() {
        Glide.with(applicationContext).load(StreamDetails.artworkUrl[streamIndex])
            .placeholder(R.drawable.icon)
            .error(R.drawable.icon)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(artworkWindow.currentView as ImageView)

        Glide.with(applicationContext).load(StreamDetails.artworkUrl[streamIndex])
            .placeholder(android.R.color.transparent)
            .error(android.R.color.transparent)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(backgroundArt.currentView as ImageView)
    }


    fun onPreparedSupport() {
        label.text = streamList[streamIndex].streamName
        loadView.startAnimation(fadeOut)
        label.startAnimation(fadeIn)
        glideImageLoader()
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