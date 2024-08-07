package com.saathi.exoplayer.feature.controller

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Immutable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.saathi.exoplayer.feature.pip.isActivityStatePipMode

/**
 * Sets the detailed properties of the VideoPlayer Controller.
 *
 * @param showSpeedAndPitchOverlay Visible speed, audio track select button.
 * @param showSubtitleButton Visible subtitle (CC) button.
 * @param showCurrentTimeAndTotalTime Visible currentTime, totalTime text.
 * @param showBufferingProgress Visible buffering progress.
 * @param showForwardIncrementButton Show forward increment button.
 * @param showBackwardIncrementButton Show backward increment button.
 * @param showBackTrackButton Show back track button.
 * @param showNextTrackButton Show next track button.
 * @param showRepeatModeButton Show repeat mode toggle button.
 * @param controllerShowTimeMilliSeconds Sets the playback controls timeout.
 *  The playback controls are automatically hidden after this duration of time has elapsed without user input and with playback or buffering in progress.
 *  (The timeout in milliseconds. A non-positive value will cause the controller to remain visible indefinitely.)
 * @param controllerAutoShow Sets whether the playback controls are automatically shown when playback starts, pauses, ends, or fails.
 *  If set to false, the playback controls can be manually operated with {@link #showController()} and {@link #hideController()}.
 *  (Whether the playback controls are allowed to show automatically.)
 * @param showFullScreenButton Show full screen button.
 */
@Immutable
data class VideoPlayerControllerConfig(
    val showSpeedAndPitchOverlay: Boolean,
    val showSubtitleButton: Boolean,
    val showCurrentTimeAndTotalTime: Boolean,
    val showBufferingProgress: Boolean,
    val showForwardIncrementButton: Boolean,
    val showBackwardIncrementButton: Boolean,
    val showBackTrackButton: Boolean,
    val showNextTrackButton: Boolean,
    val showRepeatModeButton: Boolean,
    val showFullScreenButton: Boolean,
    val controllerShowTimeMilliSeconds: Int,
    val controllerAutoShow: Boolean,
    val gestureEnable: Boolean,
    val allowForwardSeeking: Boolean,
    val allowBackwardSeeking: Boolean,
) {

    companion object {
        /**
         * Default config for Controller.
         */
        val Default = VideoPlayerControllerConfig(
            showSpeedAndPitchOverlay = false,
            showSubtitleButton = true,
            showCurrentTimeAndTotalTime = true,
            showBufferingProgress = false,
            showForwardIncrementButton = false,
            showBackwardIncrementButton = false,
            showBackTrackButton = true,
            showNextTrackButton = true,
            showRepeatModeButton = false,
            controllerShowTimeMilliSeconds = 5_000,
            controllerAutoShow = true,
            showFullScreenButton = true,
            gestureEnable = false,
            allowForwardSeeking = true,
            allowBackwardSeeking = true,
        )
    }
}


/**
 * Apply the [VideoPlayerControllerConfig] to the ExoPlayer StyledViewPlayer.
 *
 * @param playerView [PlayerView] to which you want to apply settings.
 * @param onFullScreenStatusChanged Callback that occurs when the full screen status changes.
 */
@SuppressLint("UnsafeOptInUsageError")
internal fun VideoPlayerControllerConfig.applyToExoPlayerView(
    playerView: PlayerView,
    context: Context,
    onFullScreenStatusChanged: (Boolean) -> Unit,
) {
    val controllerView = playerView.rootView
    fun seekForward(playerView: PlayerView) {
        // Implement seeking forward logic
        // This could involve calling your media player's seekTo() method
        // with an offset forward from the current playback position
        val seekPosition = playerView.player?.currentPosition?.plus(controllerShowTimeMilliSeconds)
        seekPosition?.let { maxOf(0, it) }?.let { playerView.player?.seekTo(it) }
    }

    fun seekBackward(playerView: PlayerView) {
        // Implement seeking backward logic
        // This could involve calling your media player's seekTo() method
        // with an offset backward from the current playback position
        val seekPosition = playerView.player?.currentPosition?.minus(controllerShowTimeMilliSeconds)
        seekPosition?.let { maxOf(0, it) }?.let { playerView.player?.seekTo(it) }
    }

    val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.d("CustomPlayerControlView", "onDoubleTap")
                if (e.x < playerView.width / 2) {
                    // Seek backward
                    seekBackward(playerView)
                } else {
                    // Seek forward
                    seekForward(playerView)
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val x = e1!!.x
                if (x < playerView.width / 2) {
                    Log.d("CustomPlayerControlView", "on brightness change")

                    // Adjust brightness on the left side
                    //   adjustBrightness(if (distanceY > 0) BRIGHTNESS_CHANGE_DELTA else -BRIGHTNESS_CHANGE_DELTA)
                } else {
                    // Adjust volume on the right side
                    Log.d("CustomPlayerControlView", "on volume change")

                    // adjustVolume(if (distanceY > 0) VOLUME_CHANGE_DELTA else -VOLUME_CHANGE_DELTA)
                }
                return true
            }
        })
    val aspectRatioFrameLayout =
        controllerView.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
    aspectRatioFrameLayout.setAspectRatio(16f / 9f)
    val isInPipMode = context.isActivityStatePipMode()
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_settings).isVisible =
        showSpeedAndPitchOverlay
    playerView.setShowSubtitleButton(showSubtitleButton)
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_time).isVisible =
        showCurrentTimeAndTotalTime
    playerView.setShowBuffering(
        if (!showBufferingProgress) PlayerView.SHOW_BUFFERING_NEVER else PlayerView.SHOW_BUFFERING_ALWAYS,
    )
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_ffwd_with_amount).isInvisible =
        !showForwardIncrementButton
    controllerView.findViewById<View>(androidx.media3.ui.R.id.exo_rew_with_amount).isVisible =
        showBackwardIncrementButton

    playerView.setShowNextButton(showNextTrackButton && !isInPipMode)
    playerView.setShowPreviousButton(showBackTrackButton && !isInPipMode)
    playerView.showController()
    if (gestureEnable) {
        playerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
        }
    }
    playerView.setShowFastForwardButton(showForwardIncrementButton)
    playerView.setShowRewindButton(showBackwardIncrementButton)
    playerView.controllerShowTimeoutMs = controllerShowTimeMilliSeconds
    playerView.controllerAutoShow = controllerAutoShow

    @Suppress("DEPRECATION")
    if (showFullScreenButton) {
        playerView.setControllerOnFullScreenModeChangedListener {
            onFullScreenStatusChanged(it)
        }
    } else {
        playerView.setControllerOnFullScreenModeChangedListener(null)
    }

}
