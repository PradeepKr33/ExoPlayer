package com.saathi.exoplayer

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import com.pallycon.widevine.model.ContentData
import com.pallycon.widevine.model.PallyConDrmConfigration
import com.saathi.exoplayer.feature.RepeatMode
import com.saathi.exoplayer.feature.ResizeMode
import com.saathi.exoplayer.feature.VideoPlayer
import com.saathi.exoplayer.feature.controller.VideoPlayerControllerConfig
import com.saathi.exoplayer.feature.pip.enterPIPMode
import com.saathi.exoplayer.feature.toRepeatMode
import com.saathi.exoplayer.feature.uri.VideoPlayerMediaItem
import com.saathi.exoplayer.feature.util.MIME_TYPE_DASH
import com.saathi.exoplayer.feature.util.MIME_TYPE_HLS
import com.saathi.exoplayer.ui.theme.ExoPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@AndroidEntryPoint
class MainActivity : ComponentActivity() {



    // TODO: 2. initialize PallyconWVM SDK

    var player: PlayerView? = null
    var videoList: PersistentList<VideoPlayerMediaItem.NetworkMediaItem> = persistentListOf(
        VideoPlayerMediaItem.NetworkMediaItem(
            url = "https://contents.pallycon.com/DEV/sglee/multitracks/dash/stream.mpd",
            mediaMetadata = MediaMetadata.Builder().build(),
            mimeType = MIME_TYPE_DASH,

        ),
        VideoPlayerMediaItem.NetworkMediaItem(
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            mediaMetadata = MediaMetadata.Builder().build(),
            mimeType = MIME_TYPE_HLS,
        ),
        VideoPlayerMediaItem.NetworkMediaItem(
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            mediaMetadata = MediaMetadata.Builder().build(),
            mimeType = MIME_TYPE_HLS,
        )

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExoPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ExoPlayerView()
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            // Enter PIP mode
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                // The activity is in the resumed state, you can enter PIP mode
                enterPIPMode(this, defaultPlayerView = player!!)
            } else {
                // The activity is not in the resumed state, you cannot enter PIP mode
            }
        }
    }

    @Composable
    private fun ExoPlayerView() {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                val context = LocalContext.current
                var repeatMode by remember { mutableStateOf(RepeatMode.NONE) }
                VideoPlayer(
                    mediaItems = videoList,
                    handleLifecycle = false,
                    autoPlay = true,
                    usePlayerController = true,
                    enablePipWhenBackPressed = true,
                    enablePip = true,
                    defaultFullScreen = false,
                    controllerConfig = VideoPlayerControllerConfig.Default.copy(
                        showSpeedAndPitchOverlay = true,
                        showSubtitleButton = false,
                        showNextTrackButton = false,
                        showBackTrackButton = false,
                        showBackwardIncrementButton = true,
                        showForwardIncrementButton = true,
                        showRepeatModeButton = false,
                        showFullScreenButton = true,
                        allowForwardSeeking = false,
                        allowBackwardSeeking = true
                    ),
                    repeatMode = repeatMode,
                    resizeMode = ResizeMode.FIT,
                    onCurrentTimeChanged = {
                        Log.e("CurrentTime", it.toString())
                    },
                    handleAudioFocus = true,
                    playerInstance = {
                        Log.e("VOLUME", volume.toString())
                        addAnalyticsListener(object : AnalyticsListener {
                            @SuppressLint("UnsafeOptInUsageError")
                            override fun onRepeatModeChanged(
                                eventTime: AnalyticsListener.EventTime,
                                rMode: Int,
                            ) {
                                repeatMode = rMode.toRepeatMode()
                                Toast.makeText(
                                    context,
                                    "RepeatMode changed = ${rMode.toRepeatMode()}",
                                    Toast.LENGTH_LONG,
                                )
                                    .show()
                            }

                            @SuppressLint("UnsafeOptInUsageError")
                            override fun onPlayWhenReadyChanged(
                                eventTime: AnalyticsListener.EventTime,
                                playWhenReady: Boolean,
                                reason: Int,
                            ) {
                            }

                            @SuppressLint("UnsafeOptInUsageError")
                            override fun onVolumeChanged(
                                eventTime: AnalyticsListener.EventTime,
                                volume: Float,
                            ) {
                                Toast.makeText(
                                    context,
                                    "Player volume changed = $volume",
                                    Toast.LENGTH_LONG,
                                )
                                    .show()
                            }

                            @SuppressLint("UnsafeOptInUsageError")
                            override fun onSeekForwardIncrementChanged(
                                eventTime: AnalyticsListener.EventTime,
                                seekForwardIncrementMs: Long
                            ) {

                            }
                        })
                        addListener(object : Player.Listener {
                            @OptIn(UnstableApi::class)
                            override fun onTracksChanged(tracks: Tracks) {
                                tracks.groups.forEachIndexed { index, trackGroup ->
                                    for (j in 0 until trackGroup.length) {
                                        val format = trackGroup.getTrackFormat(j)
                                        Log.d("TrackFormat", format.sampleMimeType.toString())
                                        if (format.sampleMimeType?.startsWith("video") == true) {
                                            // This is a video track
                                        }
                                    }
                                }
                                super.onTracksChanged(tracks)
                            }
                        })

                    },
                    playerView = {
                        player = this
                    },
                    lifecycle = lifecycle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                )
            }
        }
    }
}
