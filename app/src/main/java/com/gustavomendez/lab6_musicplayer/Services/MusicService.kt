package com.gustavomendez.lab6_musicplayer.Services

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.PRIORITY_MIN
import android.util.Log
import com.gustavomendez.lab6_musicplayer.Activities.MainActivity
import com.gustavomendez.lab6_musicplayer.Models.Song
import com.gustavomendez.lab6_musicplayer.R
import java.util.*

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    //media player
    private lateinit var player: MediaPlayer
    //song list
    private lateinit var songs: ArrayList<Song>
    //current position
    private var songPosn: Int = 0
    private var songTitle = ""
    private val musicBind = MusicBinder()
    private var shuffle = false
    private lateinit var rand:Random

    companion object {
        private const val NOTIFY_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? {
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player.stop()
        player.release()
        return false
    }

    override fun onCreate() {
        //create the service
        super.onCreate()
        //initialize position
        songPosn = 0
        initMusicPlayer()
        rand = Random()
    }

    fun setShuffle() {
        shuffle = !shuffle
    }

    fun initMusicPlayer() {
        //set player properties
        player = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }

        player.setOnPreparedListener(this)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)

    }

    fun playSong() {
        //play a song
        player.reset()
        //get song
        val playSong = songs[songPosn]
        //get id
        val currSong = playSong.songID
        songTitle = playSong.songTitle
        //set uri
        val trackUri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            currSong
        )

        try {
            player.setDataSource(applicationContext, trackUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error setting data source", e)
        }

        player.prepareAsync()

    }

    inner class MusicBinder : Binder() {
        internal val service: MusicService
            get() = this@MusicService
    }

    fun setList(theSongs: ArrayList<Song>) {
        songs = theSongs
    }


    override fun onPrepared(mp: MediaPlayer?) {
        mp!!.start()

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notIntent = Intent(this, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendInt = PendingIntent.getActivity(this, 0,
                                        notIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId )
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(PRIORITY_MIN)
            .setTicker(songTitle)
            .setContentText(songTitle)
            .setOngoing(true)
            .setContentIntent(pendInt)
            //.setTicker(songTitle)
            .setContentTitle("My Player")
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(NOTIFY_ID, notification)


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    fun setSong(songIndex: Int) {
        songPosn = songIndex
    }

    fun playPrev(){
        songPosn--
        if(songPosn == 0) songPosn = songs.size-1
        playSong()
    }

    //skip to next
    fun playNext(){
        if(shuffle){
            var newSong = songPosn
            while(newSong == songPosn){
                newSong = rand.nextInt(songs.size)
            }
            songPosn = newSong
        }
        else{
            songPosn++
            if(songPosn == songs.size) songPosn=0
        }
        playSong()
    }

    fun getPosn(): Int {
        return player.currentPosition
    }

    fun getDur(): Int {
        return player.duration
    }

    fun isPng(): Boolean {
        return player.isPlaying
    }

    fun pausePlayer() {
        player.pause()
    }

    fun seek(posn: Int) {
        player.seekTo(posn)
    }

    fun go() {
        player.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCompletion(mp: MediaPlayer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        stopForeground(true)
    }

}
