package com.saathi.exoplayer.feature

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class CustomForwardingExoPlayer(
    private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val allowForwardSeeking: Boolean = false,
    private val allowBackwardSeeking: Boolean = false
) : ForwardingPlayer(exoPlayer) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var lastMaxPlayedPosition = 0L
    override fun seekTo(positionMs: Long) {
        // Prevent seeking forward
        if (!allowForwardSeeking) {
            if (lastMaxPlayedPosition < currentPosition) {
                Log.d("inside lastMaxPlayedPosition", "$lastMaxPlayedPosition, $positionMs")
                lastMaxPlayedPosition = currentPosition
            }
            if (positionMs > lastMaxPlayedPosition) {
                seekTo(lastMaxPlayedPosition)
                Toast.makeText(context, "Cannot seek forward", Toast.LENGTH_SHORT).show()
                return
            }
        } else if (!allowBackwardSeeking) {
            if (positionMs < currentPosition) {
                Toast.makeText(context, "Cannot seek backward", Toast.LENGTH_SHORT).show()
                return
            }
        }
        super.seekTo(positionMs)
    }

    fun allowSeekToManually(positionMs: Long) {
        super.seekTo(positionMs)
    }

    fun changeTrackParam() {
        // if (exoPlayer.playbackState == Player.STATE_READY) {
        val mediaTrackSelectionParameters = if (exoPlayer.currentPosition >= 15000) {
            Log.d("inside medium ", "${exoPlayer.currentPosition}")
            TrackSelectionParameters.Builder(context)
                .setMaxVideoSize(960, 540)
                .build()
        } else {
            Log.d("inside low ", "${exoPlayer.currentPosition}")

            TrackSelectionParameters.Builder(context)
                .setMaxVideoSize(426, 240)
                .build()
        }
        coroutineScope.launch {
            exoPlayer.trackSelector?.parameters = mediaTrackSelectionParameters
            Log.d(
                "inside trackSelector",
                "trackSelector changed ${exoPlayer.trackSelector?.parameters}"
            )
        }
    }


}