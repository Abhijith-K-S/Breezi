package com.example.breezi

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ForegroundService: Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun isOnline(context: Context): Boolean {
        return StreamDetails.isOnline(context)
    }

    private var mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }

    private val streamList = StreamDetails.streamList


    data class CustomEvent(val eventCode: String)
    companion object EventObject {
        var EventBus = MutableLiveData(CustomEvent("LIVE_DATA_INITIAL"))
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //shared preferences
        val sharedPreferences = this.getSharedPreferences(StreamDetails.sharedPreferencesFileName,Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        var streamIndex = sharedPreferences.getInt(StreamDetails.streamIndex,0)
        val streamSize = StreamDetails.streamList.size
        val currentStream = intent?.getBooleanExtra("Current Stream",true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        when (intent?.getStringExtra("Control Status")) {
            StreamDetails.startStream -> {
                GlobalScope.launch(Dispatchers.IO) {
                    mediaPlayer.reset()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(streamList[streamIndex].streamURL)
                        setOnPreparedListener(this@ForegroundService)
                        setOnErrorListener(this@ForegroundService)
                        prepareAsync()
                    }
                }

                val notification = createNotification(streamIndex,true)

                if(currentStream!!)
                    startForeground(StreamDetails.notificationID, notification)
                else
                    notificationManager.notify(StreamDetails.notificationID, notification)
            }

            StreamDetails.stopStream -> {
                mediaPlayer.reset()
                val notification = createNotification(streamIndex,false)
                notificationManager.notify(StreamDetails.notificationID,notification)
                stopForeground(true)
                stopSelf(StreamDetails.notificationID)
            }

            //notification controls
            StreamDetails.notificationPrevious -> {
                if(isOnline(this)) {
                    val isPlaying = sharedPreferences.getBoolean(StreamDetails.isPlaying, true)
                    streamIndex = (streamIndex + streamSize - 1) % streamSize
                    if (!isPlaying)
                        editor.putBoolean(StreamDetails.isPlaying, true)
                    editor.putInt(StreamDetails.streamIndex, streamIndex)
                    editor.apply()
                    val notification = createNotification(streamIndex, true)
                    notificationManager.notify(StreamDetails.notificationID, notification)

                    if (isPlaying)
                        mediaPlayer.reset()

                    GlobalScope.launch(Dispatchers.IO) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(streamList[streamIndex].streamURL)
                            setOnPreparedListener(this@ForegroundService)
                            setOnErrorListener(this@ForegroundService)
                            prepareAsync()
                        }
                    }
                    uiSupportService(StreamDetails.notificationPrevious)
                }
            }

            StreamDetails.notificationPlayback -> {
                val isPlaying = sharedPreferences.getBoolean(StreamDetails.isPlaying,true)
                val notification = createNotification(streamIndex,!isPlaying)
                notificationManager.notify(StreamDetails.notificationID,notification)

                when(isPlaying) {
                    true -> {
                        mediaPlayer.reset()
                        stopForeground(true)
                        stopSelf(StreamDetails.notificationID)
                        editor.putBoolean(StreamDetails.isPlaying,!isPlaying)
                        editor.apply()
                        uiSupportService(StreamDetails.notificationPlayback)
                    }

                    false -> {
                        if(isOnline(this)) {
                            GlobalScope.launch(Dispatchers.IO) {
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(streamList[streamIndex].streamURL)
                                    setOnPreparedListener(this@ForegroundService)
                                    setOnErrorListener(this@ForegroundService)
                                    prepareAsync()
                                }
                            }
                        }
                        editor.putBoolean(StreamDetails.isPlaying,!isPlaying)
                        editor.apply()
                        uiSupportService(StreamDetails.notificationPlayback)
                    }
                }
            }

            StreamDetails.notificationNext -> {
                if (isOnline(this)) {
                    val isPlaying = sharedPreferences.getBoolean(StreamDetails.isPlaying, true)
                    streamIndex = (streamIndex + 1) % streamSize
                    if (!isPlaying)
                        editor.putBoolean(StreamDetails.isPlaying, true)
                    editor.putInt(StreamDetails.streamIndex, streamIndex)
                    editor.apply()
                    val notification = createNotification(streamIndex, true)
                    notificationManager.notify(StreamDetails.notificationID, notification)

                    if (isPlaying)
                        mediaPlayer.reset()

                    GlobalScope.launch(Dispatchers.IO) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(streamList[streamIndex].streamURL)
                            setOnPreparedListener(this@ForegroundService)
                            setOnErrorListener(this@ForegroundService)
                            prepareAsync()
                        }
                    }
                    uiSupportService(StreamDetails.notificationNext)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        stopSelf()
        val sharedPreferences = this.getSharedPreferences(StreamDetails.sharedPreferencesFileName,Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if(sharedPreferences.getBoolean(StreamDetails.isPlaying,true)) {
            editor.putBoolean(StreamDetails.isPlaying,false)
            editor.apply()
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(StreamDetails.notificationID)

        mediaPlayer.release()
        super.onDestroy()
    }


    override fun onPrepared(p0: MediaPlayer?) {
        mediaPlayer.start()
        uiSupportService(StreamDetails.prepSupport)
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        p0?.reset()
        uiSupportService(StreamDetails.errorSupport)
        val sharedPref = this.getSharedPreferences(StreamDetails.sharedPreferencesFileName,Context.MODE_PRIVATE)
        val notification = createNotification(sharedPref.getInt(StreamDetails.streamIndex,0),false)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(StreamDetails.notificationID, notification)
        stopForeground(true)
        stopSelf(StreamDetails.notificationID)
        return true
    }

    //function to update ui on error or media player prepare
    private fun uiSupportService(supportCode: String) {
        if(EventBus.hasActiveObservers()) {
            EventBus.postValue(CustomEvent(supportCode))
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification(streamIndex: Int, isPlaying: Boolean): Notification {
        lateinit var notificationChannel: NotificationChannel
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelID = "MainChannel"
        lateinit var builder: NotificationCompat.Builder

        val intent = Intent(this,MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName,R.layout.notification_layout)
        contentView.setTextViewText(R.id.notificationCurrentlyPlayingTitle,streamList[streamIndex].streamName)

        if(isPlaying)
            contentView.setImageViewResource(R.id.notification_playback_button,R.drawable.notification_pause)
        else
            contentView.setImageViewResource(R.id.notification_playback_button,R.drawable.notification_play)


        //setting control for notification buttons
        //previous button
        val previousIntent = Intent(this,ForegroundService::class.java)
                             .putExtra("Control Status", StreamDetails.notificationPrevious)
                             .putExtra("Current Stream",false)
        val previousPendingIntent = PendingIntent.getService(this,1,previousIntent,PendingIntent.FLAG_UPDATE_CURRENT)
        contentView.setOnClickPendingIntent(R.id.notification_previous_button,previousPendingIntent)

        //playback button*/
        val playbackIntent = Intent(this,ForegroundService::class.java)
                             .putExtra("Control Status",StreamDetails.notificationPlayback)
                             .putExtra("Current Stream",true)
        val playbackPendingIntent = PendingIntent.getService(this,2,playbackIntent,PendingIntent.FLAG_UPDATE_CURRENT)
        contentView.setOnClickPendingIntent(R.id.notification_playback_button,playbackPendingIntent)

        //previous button
        val nextIntent = Intent(this,ForegroundService::class.java)
                         .putExtra("Control Status",StreamDetails.notificationNext)
                         .putExtra("Current Stream",false)
        val nextPendingIntent = PendingIntent.getService(this,3,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT)
        contentView.setOnClickPendingIntent(R.id.notification_next_button,nextPendingIntent)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelID,"Playback", NotificationManager.IMPORTANCE_LOW)
            notificationChannel.lightColor = R.color.darkBlue
            notificationChannel.enableVibration(false)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(notificationChannel)

            builder = NotificationCompat.Builder(this,channelID)
                .setCustomContentView(contentView)
                .setSmallIcon(R.drawable.icon_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.icon_round))
                .setContentIntent(pendingIntent)
                .setOngoing(isPlaying)
        }

        else {
            @Suppress("DEPRECATION")
            builder = NotificationCompat.Builder(this)
                .setContent(contentView)
                .setSound(null)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.icon_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.icon_round))
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
        }

        return builder.build()
    }
}