package com.example.breezi

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fun showToast(message: String)
        {
            val toast = Toast.makeText(this,message,Toast.LENGTH_SHORT)
            toast.show()
        }

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

        //function to play button animation
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

        val actionButton = findViewById<ImageView>(R.id.actionButton)
        val label = findViewById<Button>(R.id.air)
        actionButton.setImageDrawable(getDrawable(R.drawable.play_to_stop_anim))

        //initilaise media player and buttons
        var mediaPlayer : MediaPlayer? = null

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
                    try {
                        this@MainActivity.runOnUiThread{
                            actionButton.isEnabled = false
                        }
                        mediaPlayer = MediaPlayer.create(this@MainActivity, Uri.parse(streamURL[0]))
                        mediaPlayer?.start()

                        this@MainActivity.runOnUiThread{
                            actionButton.isEnabled = true
                            label.text = mediaPlayer!!.isPlaying.toString()
                        }
                    } catch (e: Exception) {
                        this@MainActivity.runOnUiThread {
                            showToast("An Error Occured! Please Try Later")
                            Log.d("bruh",e.toString())
                        }
                    }
                }
                buttonAnimation(actionButton,"pts")
            }

            else{
                GlobalScope.launch(Dispatchers.IO) {
                    mediaPlayer?.pause()
                    this@MainActivity.runOnUiThread {
                        label.text = mediaPlayer!!.isPlaying.toString()
                    }
                }

                buttonAnimation(actionButton,"stp")
            }
        }
    }
}