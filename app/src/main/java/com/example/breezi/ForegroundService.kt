package com.example.breezi

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ForegroundService: Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private var mediaPlayer: MediaPlayer? = null
    private val streamList = StreamDetails.streamList
    private lateinit var resultReceiver: ResultReceiver


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val streamIndex = intent?.getIntExtra("Stream Index", 0)
        val currentStream = intent?.getBooleanExtra("Current Stream",true)
        resultReceiver = intent?.getParcelableExtra("resultReceiver")!!

        when (intent.getStringExtra("Control Status")) {
            StreamDetails.startStream -> {
                GlobalScope.launch(Dispatchers.IO) {
                    mediaPlayer?.reset()
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(streamList[streamIndex!!].streamURL)
                        setOnPreparedListener(this@ForegroundService)
                        setOnErrorListener(this@ForegroundService)
                        prepareAsync()
                    }
                }

                val notification = createNotification(streamIndex!!)

                if(currentStream!!)
                    startForeground(StreamDetails.notificationID, notification)
                else {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(StreamDetails.notificationID, notification)
                }
            }

            StreamDetails.stopStream -> {
                mediaPlayer?.reset()
                stopForeground(false)
                stopSelf(StreamDetails.notificationID)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    override fun onPrepared(p0: MediaPlayer?) {
        mediaPlayer?.start()
        uiSupportService(StreamDetails.prepSupport)
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        p0?.reset()
        uiSupportService(StreamDetails.errorSupport)
        stopForeground(false)
        stopSelf(StreamDetails.notificationID)
        return true
    }

    //function to update ui on error or media player prepare
    private fun uiSupportService(supportCode: StreamDetails.MainActivitySupport) {
        val bundle = Bundle()
        resultReceiver.send(supportCode.id,bundle)
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification(streamIndex: Int): Notification {
        lateinit var notificationChannel: NotificationChannel
        val channelID = "MainChannel"
        lateinit var builder: Notification.Builder
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this,MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName,R.layout.notification_layout)
        contentView.setTextViewText(R.id.notificationCurrentlyPlayingTitle,streamList[streamIndex].streamName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelID,"Playback", NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.lightColor = R.color.darkBlue
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this,channelID)
                .setCustomContentView(contentView)
                .setSmallIcon(R.drawable.icon_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.icon_round))
                .setContentIntent(pendingIntent)
        }

        else {
            builder = Notification.Builder(this)
                .setContent(contentView)
                .setSmallIcon(R.drawable.icon_round)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.drawable.icon_round))
                .setContentIntent(pendingIntent)
        }

        return builder.build()
    }
}