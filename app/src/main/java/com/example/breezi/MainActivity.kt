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
    private lateinit var label: Button

    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialise media player and buttons
        actionButton = findViewById(R.id.actionButton)
        label = findViewById(R.id.air)
        actionButton.setImageDrawable(getDrawable(R.drawable.play_to_stop_anim))

        var mediaPlayer : MediaPlayer? = null
        mediaPlayer?.setOnPreparedListener(this)

        //stream URL's
        val streamURL = arrayOf(
            "https://lfhh.radioca.st/stream",
            "http://66.228.41.10:8000/http://thirtythree-45.com:8000")

        //stream controls
        actionButton.setOnClickListener()
        {
            if(!isOnline(this))
                showToast("Please Check Your Network Connection")

            else if(mediaPlayer==null || !mediaPlayer!!.isPlaying) {
                GlobalScope.launch(Dispatchers.IO) {
                    this@MainActivity.runOnUiThread {
                        actionButton.isEnabled = false
                    }

                    mediaPlayer = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                        setDataSource(streamURL[0])
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
                    this@MainActivity.runOnUiThread {
                        label.text = mediaPlayer!!.isPlaying.toString()
                    }
                }

                buttonAnimation(actionButton,"stp")
            }
        }
    }

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.start()
        actionButton.isEnabled = true
        label.text = mediaPlayer?.isPlaying.toString()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        showToast("MediaPlayer Error: $p1")
        p0?.reset()
        buttonAnimation(actionButton,"stp")
        if(!actionButton.isEnabled)
            actionButton.isEnabled = true
        label.text = p0?.isPlaying.toString()
        return true
    }
}