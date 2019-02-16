package com.gustavomendez.lab6_musicplayer.Activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.util.ArrayList
import android.support.v7.widget.LinearLayoutManager
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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.MediaController
import android.widget.MediaController.MediaPlayerControl
//import com.gustavomendez.lab6_musicplayer.Controllers.MusicController
import kotlinx.android.synthetic.main.toolbar.*


class MainActivity : AppCompatActivity(), MediaPlayerControl {

    //Request permission for external write
    companion object {
        private const val EXTERNAL_WRITE_REQUEST_CODE = 112
    }

    //Properties
    private lateinit var songList: ArrayList<Song>
    private lateinit var adapter:SongAdapter

    //Music Player properties
    private var musicSrv: MusicService? = null
    private var playIntent: Intent? = null
    private var musicBound = false
    private lateinit var controller: MediaController
    private var paused = false
    private var playbackPaused = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set the toolbar as support action bar
        setSupportActionBar(toolbar)

        // Now get the support action bar
        val actionBar = supportActionBar

        // Set toolbar title/app title
        actionBar!!.title = "PlayerApp"

        // Set action bar/toolbar sub title
        actionBar.subtitle = "Music Player"

        // Set action bar elevation
        actionBar.elevation = 4.0F

        // Display the app icon in action bar/toolbar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayUseLogoEnabled(true)

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

                if(playbackPaused){
                    setController()
                    playbackPaused = false
                }
                musicSrv!!.playSong()
                controller.show(0)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Connect to the music service
     */
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

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if(paused){
            setController()
            paused = false
        }
    }

    override fun onStop() {
        controller.hide()
        super.onStop()
    }

    override fun onDestroy() {
        stopService(playIntent)
        musicSrv=null
        super.onDestroy()
    }

    private fun setController() {
        //set the controller up
        controller = MediaController(this)

        controller.setPrevNextListeners(View.OnClickListener { playNext() }, View.OnClickListener { playPrev() })
        controller.setMediaPlayer(this)
        controller.setAnchorView(recycler_songs)
        controller.isEnabled = true
    }

    /**
     * Playing the next song
     */
    private fun playNext() {
        musicSrv!!.playNext()
        if(playbackPaused){
            //setting the controller
            setController()
            playbackPaused = false
        }
        controller.show(0)
    }

    /**
     * Play the prev song
     */
    private fun playPrev() {
        musicSrv!!.playPrev()
        if(playbackPaused){
            setController()
            playbackPaused=false
        }
        controller.show(0)
    }

    /**
     * For realtime permissions
     */
    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("MainActivity", "Permission to write declined")
            makeRequest()
        }
    }

    /**
     * For realtime permissions
     */
    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            EXTERNAL_WRITE_REQUEST_CODE)
    }

    /**
     * Getting the song list from internal storage
     */
    private fun getSongList(){
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
     * Method to accept/decline realtime permission
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
        return false
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        return if(musicSrv!=null && musicBound && musicSrv!!.isPng())
            musicSrv!!.getDur()
        else 0
    }

    override fun pause() {
        playbackPaused=true
        musicSrv!!.pausePlayer()
        //controller.show()
    }


    override fun getBufferPercentage(): Int {
        return 0
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
        controller.show()
    }

    override fun getAudioSessionId(): Int {
        return musicSrv!!.getAudioSession()
    }

    override fun canPause(): Boolean {
        return true
    }
}
