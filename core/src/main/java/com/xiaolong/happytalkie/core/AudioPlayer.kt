package com.xiaolong.happytalkie.core

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

object AudioPlayer {
    private var player: MediaPlayer? = null

    @Synchronized
    fun play(context: Context, file: File, deleteAfter: Boolean = false) {
        stop()
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer

        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnCompletionListener {
                synchronized(this) {
                    runCatching { it.release() }
                    if (player === it) player = null
                    if (deleteAfter) file.delete()
                }
            }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                synchronized(this) {
                    runCatching { mp.release() }
                    if (player === mp) player = null
                    if (deleteAfter) file.delete()
                }
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (_: Exception) {
            runCatching { mediaPlayer.release() }
            if (player === mediaPlayer) player = null
            if (deleteAfter) file.delete()
        }
    }

    @Synchronized
    fun stop() {
        val current = player ?: return
        player = null
        runCatching { current.stop() }
        runCatching { current.release() }
    }
}
