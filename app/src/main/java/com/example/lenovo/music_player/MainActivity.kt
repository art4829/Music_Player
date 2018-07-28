package com.example.lenovo.music_player



import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.*
import com.mtechviral.mplaylib.MusicFinder
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private var albumArt: ImageView? = null
    private var songTitle: TextView? = null
    private var songArtist : TextView? = null
    private var mediaPlayer: MediaPlayer? = null
    private val myHandler=Handler()
    
    
    private var seekBar:SeekBar?=null
    private var playButton:ImageButton?=null
    private var previousButton:ImageButton?=null
    private var nextButton:ImageButton?=null
    private var shuffleButton:ToggleButton?=null
    private var starttiming:TextView?=null
    private var endtiming:TextView?=null
    private var startTime=0.00
    private var finalTime=0.00

    private val updateSongTime=object:Runnable{
        override fun run() {
            startTime = mediaPlayer!!.currentPosition.toDouble()
            starttiming!!.text = String.format("%d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
            endtiming!!.text = String.format("%d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((finalTime-startTime).toLong()),
                    TimeUnit.MILLISECONDS.toSeconds((finalTime-startTime).toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((finalTime-startTime).toLong())))

            seekBar!!.progress=startTime.toInt()
                myHandler.postDelayed(this,100)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
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


    private fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) +  start


    private fun createPlayer(){
        val songsJob = async {
            val songFinder = MusicFinder(contentResolver)
            songFinder.prepare()
            songFinder.allSongs
        }
        val shuffledSongsJob =
                async {
                    val songFinder = MusicFinder(contentResolver)
                    songFinder.prepare()
                    songFinder.allSongs
                }
        setContentView(R.layout.activity_main)

        launch(kotlinx.coroutines.experimental.android.UI){
            albumArt = findViewById(R.id.albumArt)
            songTitle = findViewById(R.id.songId)
            songArtist = findViewById(R.id.songArtist)
            val songs = songsJob.await()
            playButton = findViewById<ImageButton>(R.id.playButton)
            shuffleButton= findViewById<ToggleButton>(R.id.shuffleButton)
            previousButton = findViewById<ImageButton>(R.id.previousButton)
            nextButton=findViewById<ImageButton>(R.id.nextButton)
            seekBar=findViewById<View>(R.id.seekBar) as SeekBar
            var song = songs[0]
            var idx=0
            var shuffle=false
            val shuffledSongs= shuffledSongsJob.await()
            starttiming=findViewById<View>(R.id.txt1) as TextView
            endtiming=findViewById<View>(R.id.txt2) as TextView

            Collections.shuffle(shuffledSongs)

                fun playOrPause() {
                    val songPlaying:Boolean? = mediaPlayer?.isPlaying
                    if(songPlaying == true){
                        mediaPlayer?.pause()
                        playButton?.imageResource = R.drawable.ic_play_arrow_black_24dp
                    }
                    else{
                        mediaPlayer?.start()
                        timing()
                        playButton?.imageResource = R.drawable.ic_pause_black_24dp
                    }
                }
                fun playPrevious(){
                    if(idx==0){
                        toast("No previous song")
                    }else {

                        idx--
                        if(shuffle){
                            song=shuffledSongs[idx]
                        }else {
                            song = songs[idx]
                        }
                        mediaPlayer?.reset()
                        mediaPlayer = MediaPlayer.create(ctx, song.uri)
                        mediaPlayer?.setOnCompletionListener {
                            playPrevious()
                        }
                        albumArt?.imageURI = song.albumArt
                        songTitle?.text = song.title
                        songArtist?.text = song.artist
                        mediaPlayer?.start()
                        timing()
                        playButton?.imageResource = R.drawable.ic_pause_black_24dp
                    }
                }
                fun playNext(){
                    idx++
                    if(shuffle){
                        song=shuffledSongs[idx]
                    }else {
                        song = songs[idx]
                    }
                    mediaPlayer?.reset()
                    mediaPlayer = MediaPlayer.create(ctx,song.uri)
                    mediaPlayer?.setOnCompletionListener {
                                                playNext()
                    }
                    albumArt?.imageURI = song.albumArt
                    songTitle?.text = song.title
                    songArtist?.text = song.artist
                    mediaPlayer?.start()
                    timing()
                    playButton?.imageResource = R.drawable.ic_pause_black_24dp
                }
//


            shuffleButton!!.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked){
                    Toast.makeText(this@MainActivity, "Shuffle On", Toast.LENGTH_SHORT).show()
                    shuffle=true
                }else{
                    Toast.makeText(this@MainActivity, "Shuffle Off", Toast.LENGTH_SHORT).show()
                    shuffle=false
                }


            }

            playButton!!.setOnClickListener{
                playOrPause()
            }
            nextButton!!.setOnClickListener{
                playNext()
            }
            previousButton!!.setOnClickListener{
                playPrevious()
            }
            seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    if(b){
                        mediaPlayer?.seekTo(i)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
        }
    }

    private fun timing(){
        finalTime=mediaPlayer!!.duration.toDouble()
        startTime=mediaPlayer!!.currentPosition.toDouble()
        if(oneTimeOnly==0){
            seekBar!!.max=finalTime.toInt()
            oneTimeOnly=1
        }
        seekBar!!.progress=startTime.toInt()
        myHandler.postDelayed(updateSongTime,100)
    }
    companion object {
        var oneTimeOnly=0
    }
    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}
