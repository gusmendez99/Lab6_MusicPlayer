package com.gustavomendez.lab6_musicplayer.Activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.database.Cursor;
import android.os.Binder
import android.support.v7.widget.LinearLayoutManager
import android.widget.ListView;
import com.gustavomendez.lab6_musicplayer.Adapters.SongAdapter
import com.gustavomendez.lab6_musicplayer.R
import com.gustavomendez.lab6_musicplayer.Models.Song
import kotlinx.android.synthetic.main.activity_main.*
import com.gustavomendez.lab6_musicplayer.Services.MusicService
import com.gustavomendez.lab6_musicplayer.Services.MusicService.MusicBinder
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.MediaController.MediaPlayerControl
import com.gustavomendez.lab6_musicplayer.Controllers.MusicController




class MainActivity : AppCompatActivity(), MediaPlayerControl {


    companion object {
        private const val EXTERNAL_WRITE_REQUEST_CODE = 112
    }

    private lateinit var songList: ArrayList<Song>
    private lateinit var adapter:SongAdapter
    private var musicSrv: MusicService? = null
    private var playIntent: Intent? = null
    private var musicBound = false
    private lateinit var controller: MusicController
    private val paused = false
    private val playbackPaused = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupPermissions()
        songList = ArrayList()

        getSongList()

        songList.sortedWith(compareBy { it.songTitle })

        // Creates a vertical Layout Manager
        recycler_songs.layoutManager = LinearLayoutManager(this)
        // Access the RecyclerView Adapter and load the data into it
        adapter = SongAdapter(songList, this) { song ->
            run {

                //Get a callback with the song info
                musicSrv!!.setSong(songList.indexOf(song))
                musicSrv!!.playSong()

                /*val intent = Intent(this, ContactInfoActivity::class.java)
                    intent.putExtra(SAVED_CONTACT_ID, contact._id)
                    startActivity(intent)
                    this.finish()*/
            }
        }

        //Setting the recycler adapter
        recycler_songs.adapter = adapter

        setController()

    }

    //connect to the service
    private val musicConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicBinder
            //get service
            musicSrv = binder.service
            //pass list
            musicSrv!!.setList(songList)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        if (playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_shuffle -> {
            }
            R.id.action_end -> {
                stopService(playIntent)
                musicSrv = null
                System.exit(0)
            }
            R.id.action_shuffle -> {
                musicSrv!!.setShuffle()
            }
        }//shuffle
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        stopService(playIntent)
        musicSrv=null
        super.onDestroy()
    }

    private fun setController() {
        //set the controller up
        controller = MusicController(this)

        controller.setPrevNextListeners(View.OnClickListener { playNext() }, View.OnClickListener { playPrev() })

        controller.setMediaPlayer(this)
        controller.setAnchorView(recycler_songs)
        controller.isEnabled = true
    }

    //play next
    private fun playNext() {
        musicSrv!!.playNext()
        controller.show(0)
    }

    //play previous
    private fun playPrev() {
        musicSrv!!.playPrev()
        controller.show(0)
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("SaveContactActivity", "Permission to write declined")
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            EXTERNAL_WRITE_REQUEST_CODE)
    }


    fun getSongList(){
        val musicResolver = contentResolver
        val musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor = musicResolver.query(musicUri, null, null, null, null)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            val titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
            //add songs to list
            do {
                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn)
                val thisArtist = musicCursor.getString(artistColumn)
                songList.add(Song(thisId, thisTitle, thisArtist))
            } while (musicCursor.moveToNext())
        }

    }

    /**
     * Method to accept/decline permission on real time
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            EXTERNAL_WRITE_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("MainActivity", "Permission has been denied by user")
                } else {
                    Log.i("MainActivity", "Permission has been granted by user")
                }
            }
        }
    }

    //From MediaPlayerControl
    override fun isPlaying(): Boolean {
        if(musicSrv!=null && musicBound)
            return musicSrv!!.isPng()
        return false;
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        if(musicSrv!=null && musicBound && musicSrv!!.isPng())
            return musicSrv!!.getDur()
        else return 0
    }

    override fun pause() {
        musicSrv!!.pausePlayer()
    }

    override fun getBufferPercentage(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun seekTo(pos: Int) {
        musicSrv!!.seek(pos)
    }

    override fun getCurrentPosition(): Int {
        return if(musicSrv != null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getPosn()
        else 0
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        musicSrv!!.go()
    }

    override fun getAudioSessionId(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canPause(): Boolean {
        return true
    }
}
