package com.gustavomendez.lab6_musicplayer.Adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gustavomendez.lab6_musicplayer.Models.Song
import com.gustavomendez.lab6_musicplayer.R
import kotlinx.android.synthetic.main.song.view.*

class SongAdapter(
    private val items : ArrayList<Song>, private val context: Context,
    private val listener: (Song) -> Unit) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

    // Gets the number of contatcs in the list
    override fun getItemCount(): Int {
        return items.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.song, parent, false))
    }

    // Binds each song in the ArrayList to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(items[position], listener)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //Binding each item, with a listener
        fun bindItems(song: Song, listener: (Song) -> Unit) = with(itemView) {

            itemView.song_title.text = song.songTitle
            itemView.song_artist.text = song.songArtist

            //Listener return false if there's a single click
            setOnClickListener { listener(song) }
            //Listener return true if there's a long click

        }
    }


}