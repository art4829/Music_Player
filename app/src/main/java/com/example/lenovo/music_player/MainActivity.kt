package com.example.lenovo.music_player



import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.mtechviral.mplaylib.MusicFinder
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.util.*

class MainActivity : AppCompatActivity() {


    var albumArt: ImageView? = null

    var songTitle: TextView? = null
    var songArtist : TextView? = null

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){

            //Ask for permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),0)
        }else{
            createPlayer()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
            createPlayer()
        }else{
            longToast("Permission not granted. Shutting down.")
            finish()
        }
    }

    private fun createPlayer(){

        var songsJob = async {
            val songFinder = MusicFinder(contentResolver)
            songFinder.prepare()
            songFinder.allSongs
        }
        setContentView(R.layout.activity_main)

        launch(kotlinx.coroutines.experimental.android.UI){
            val songs = songsJob.await()
            albumArt = findViewById(R.id.albumArt)
            songTitle = findViewById(R.id.songId)
            songArtist = findViewById(R.id.songArtist)
            var playButton = findViewById<ImageButton>(R.id.playButton)
            var shuffleButton = findViewById<ImageButton>(R.id.shuffleButton)


                fun playRandom() {
                    Collections.shuffle(songs)
                    val song = songs[0]
                    mediaPlayer?.reset()
                    mediaPlayer = MediaPlayer.create(ctx,song.uri)
                    mediaPlayer?.setOnCompletionListener {
                        playRandom()
                    }
                    albumArt?.imageURI = song.albumArt
                    songTitle?.text = song.title
                    songArtist?.text = song.artist
                    mediaPlayer?.start()
                    playButton?.imageResource = R.drawable.ic_pause_black_24dp
                }

                fun playOrPause() {
                    var songPlaying:Boolean? = mediaPlayer?.isPlaying

                    if(songPlaying == true){
                        mediaPlayer?.pause()
                        playButton?.imageResource = R.drawable.ic_play_arrow_black_24dp
                    }
                    else{
                        mediaPlayer?.start()
                        playButton?.imageResource = R.drawable.ic_pause_black_24dp
                    }
                }
            shuffleButton.setOnClickListener{
                playRandom()
            }
            playButton.setOnClickListener{
                playOrPause()
            }
            playRandom()


        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
