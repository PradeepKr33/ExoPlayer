package com.saathi.exoplayer.feature

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import androidx.activity.compose.BackHandler
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.pallycon.widevine.model.ContentData
import com.pallycon.widevine.model.PallyConDrmConfigration
import com.pallycon.widevine.sdk.PallyConWvSDK
import com.saathi.exoplayer.CustomPlayerView
import com.saathi.exoplayer.feature.cache.VideoPlayerCacheManager
import com.saathi.exoplayer.feature.controller.VideoPlayerControllerConfig
import com.saathi.exoplayer.feature.controller.applyToExoPlayerView
import com.saathi.exoplayer.feature.pip.enterPIPMode
import com.saathi.exoplayer.feature.pip.isActivityStatePipMode
import com.saathi.exoplayer.feature.uri.VideoPlayerMediaItem
import com.saathi.exoplayer.feature.uri.toUri
import com.saathi.exoplayer.feature.util.findActivity
import com.saathi.exoplayer.feature.util.setFullScreen
import kotlinx.coroutines.delay
import java.util.UUID


/**
 * [VideoPlayer] is UI component that can play video in Jetpack Compose. It works based on ExoPlayer.
 * You can play local (e.g. asset files, resource files) files and all video files in the network environment.
 * For all video formats supported by the [VideoPlayer] component, see the ExoPlayer link below.
 *
 * If you rotate the screen, the default action is to reset the player state.
 * To prevent this happening, put the following options in the `android:configChanges` option of your app's AndroidManifest.xml to keep the settings.
 * ```
 * keyboard|keyboardHidden|orientation|screenSize|screenLayout|smallestScreenSize|uiMode
 * ```
 *
 * This component is linked with Compose [androidx.compose.runtime.DisposableEffect].
 * This means that it move out of the Composable Scope, the ExoPlayer instance will automatically be destroyed as well.
 *
 * @see <a href="https://exoplayer.dev/supported-formats.html">Exoplayer Support Formats</a>
 *
 * @param modifier Modifier to apply to this layout node.
 * @param mediaItems [VideoPlayerMediaItem] to be played by the video player. The reason for receiving media items as an array is to configure multi-track. If it's a single track, provide a single list (e.g. listOf(mediaItem)).
 * @param handleLifecycle Sets whether to automatically play/stop the player according to the activity lifecycle. Default is true.
 * @param autoPlay Autoplay when media item prepared. Default is true.
 * @param usePlayerController Using player controller. Default is true.
 * @param controllerConfig Player controller config. You can customize the Video Player Controller UI.
 * @param seekBeforeMilliSeconds The seek back increment, in milliseconds. Default is 10sec (10000ms). Read-only props (Changes in values do not take effect.)
 * @param seekAfterMilliSeconds The seek forward increment, in milliseconds. Default is 10sec (10000ms). Read-only props (Changes in values do not take effect.)
 * @param repeatMode Sets the content repeat mode.
 * @param volume Sets thie player volume. It's possible from 0.0 to 1.0.
 * @param onCurrentTimeChanged A callback that returned once every second for player current time when the player is playing.
 * @param fullScreenSecurePolicy Windows security settings to apply when full screen. Default is off. (For example, avoid screenshots that are not DRM-applied.)
 * @param onFullScreenEnter A callback that occurs when the player is full screen. (The [VideoPlayerControllerConfig.showFullScreenButton] must be true to trigger a callback.)
 * @param onFullScreenExit A callback that occurs when the full screen is turned off. (The [VideoPlayerControllerConfig.showFullScreenButton] must be true to trigger a callback.)
 * @param enablePip Enable PIP (Picture-in-Picture).
 * @param enablePipWhenBackPressed With [enablePip] is `true`, set whether to enable PIP mode even when you press Back. Default is false.
 * @param handleAudioFocus Set whether to handle the video playback control automatically when it is playing in PIP mode and media is played in another app. Default is true.
 * @param playerInstance Return exoplayer instance. This instance allows you to add [androidx.media3.exoplayer.analytics.AnalyticsListener] to receive various events from the player.
 */
@SuppressLint("SourceLockedOrientationActivity", "UnsafeOptInUsageError")
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    mediaItems: List<VideoPlayerMediaItem>,
    handleLifecycle: Boolean = true,
    autoPlay: Boolean = true,
    usePlayerController: Boolean = true,
    controllerConfig: VideoPlayerControllerConfig = VideoPlayerControllerConfig.Default,
    seekBeforeMilliSeconds: Long = 10000L,
    seekAfterMilliSeconds: Long = 10000L,
    repeatMode: RepeatMode = RepeatMode.NONE,
    resizeMode: ResizeMode = ResizeMode.FILL,
    @FloatRange(from = 0.0, to = 1.0) volume: Float = 1f,
    onCurrentTimeChanged: (Long) -> Unit = {},
    fullScreenSecurePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    onFullScreenEnter: () -> Unit = {},
    onFullScreenExit: () -> Unit = {},
    enablePip: Boolean = false,
    defaultFullScreen: Boolean = false,
    enablePipWhenBackPressed: Boolean = false,
    handleAudioFocus: Boolean = true,
    playerInstance: ExoPlayer.() -> Unit = {},
    playerView: PlayerView?.() -> Unit = {},
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
) {
    val context = LocalContext.current
    var currentTime by remember { mutableLongStateOf(0L) }
    var mediaSession = remember<MediaSession?> { null }
    val trackSelector = DefaultTrackSelector(context)
    fun getCurrentLifecycleState(): Lifecycle {
        return lifecycle
    }
    val config =  PallyConDrmConfigration(
        "DEMO",
        "eyJrZXlfcm90YXRpb24iOmZhbHNlLCJyZXNwb25zZV9mb3JtYXQiOiJvcmlnaW5hbCIsInVzZXJfaWQiOiJ0ZXN0VXNlciIsImRybV90eXBlIjoid2lkZXZpbmUiLCJzaXRlX2lkIjoiREVNTyIsImhhc2giOiJpSGlpQmM3U1QrWTR1T0h1VnVPQVNmNU1nTDVibDJMb1FuNzNHREtcLzltbz0iLCJjaWQiOiJtdWx0aXRyYWNrcyIsInBvbGljeSI6IjlXcUlXa2RocHhWR0s4UFNJWWNuSnNjdnVBOXN4Z3ViTHNkK2FqdVwvYm9tUVpQYnFJK3hhZVlmUW9jY2t2dUVmQWFxZFc1aFhnSk5nY1NTM2ZTN284TnNqd3N6ak11dnQrMFF6TGtaVlZObXgwa2VmT2Uyd0NzMlRJVGdkVTRCdk45YWJoZDByUWtNSXJtb0llb0pIcUllSGNSdlZmNlQxNFJtVEFERXBDWTQ2NHdxamNzWjA0Uk82Zm90Nm5yZjhXSGZ3QVNjek9kV1d6QStFRlRadDhRTWw5SFRueWVYK1g3YXp1Y2VmQjJBd2V0XC9hQm0rZXpmUERodFZuaUhsSiIsInRpbWVzdGFtcCI6IjIwMjItMDgtMDVUMDY6MDM6MjJaIn0="
    )
    val content =  ContentData(
        "https://contents.pallycon.com/DEV/sglee/multitracks/dash/stream.mpd",
        "",
        config
    )
    lateinit var agent:PallyConWvSDK
    val player = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        agent = PallyConWvSDK.createPallyConWvSDK(context, content)
        CustomForwardingExoPlayer(
            context,
            ExoPlayer.Builder(context)
                .setSeekBackIncrementMs(seekBeforeMilliSeconds)
                .setSeekForwardIncrementMs(seekAfterMilliSeconds)
                .setDeviceVolumeControlEnabled(true)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    handleAudioFocus,
                ).setTrackSelector(trackSelector)
                .apply {

                    // Enable/ disable player cache
                    val cache = VideoPlayerCacheManager.getCache()
                    if (cache != null) {
                        val cacheDataSourceFactory = CacheDataSource.Factory()
                            .setCache(cache)
                            .setUpstreamDataSourceFactory(
                                DefaultDataSource.Factory(
                                    context,
                                    httpDataSourceFactory
                                )
                            )
                        setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
                    }
                }
                .build()
                .apply {
                    this.trackSelectionParameters = this.trackSelectionParameters.buildUpon()
                        //.setMaxVideoSizeSd() // Set maximum allowed video size to standard definition.
                        //.setForceHighestSupportedBitrate(true) // Force selection of the highest bitrate track that the device is capable of playing.
                        //.setForceLowestBitrate(true) // Do not force selection of the lowest bitrate track.
                        .build()
                }
                .also(playerInstance),
            allowForwardSeeking = controllerConfig.allowForwardSeeking,
            allowBackwardSeeking = controllerConfig.allowBackwardSeeking
        )
    }

    val defaultPlayerView = remember {
        CustomPlayerView(context).also(playerView)
    }

    BackHandler(enablePip && enablePipWhenBackPressed) {
        getCurrentLifecycleState().let {
            if (it.currentState.isAtLeast(Lifecycle.State.RESUMED) && context.isActivityStatePipMode()) {
                enterPIPMode(context, defaultPlayerView)
            }
        }
        // enterPIPMode(context, defaultPlayerView)
        player.play()
    }
    val currentMediaItem: MediaItem? = player.currentMediaItem

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (currentTime != player.currentPosition) {
                onCurrentTimeChanged(currentTime)
            }
            currentTime = player.currentPosition
        }
    }

    LaunchedEffect(usePlayerController) {
        defaultPlayerView.useController = usePlayerController
    }

    LaunchedEffect(player) {
        defaultPlayerView.player = player
    }

    LaunchedEffect(mediaItems, player) {
        mediaSession?.release()
        mediaSession = MediaSession.Builder(context, ForwardingPlayer(player))
            .setId(
                "VideoPlayerMediaSession_${
                    UUID.randomUUID().toString().lowercase().split("-").first()
                }"
            )
            .build()

        val mediaSource:MediaItem? = agent?.getMediaItem()
       /* var exoPlayerMediaItem = mediaItems.map {
            mediaSource
        }*/
        mediaSource?.let {
            player.setMediaItem(mediaSource)

        }

        player.prepare()

        if (autoPlay) {
            player.play()
        }
    }

    var isFullScreenModeEntered by remember { mutableStateOf(defaultFullScreen) }

    LaunchedEffect(controllerConfig) {
        controllerConfig.applyToExoPlayerView(defaultPlayerView, context) {
            isFullScreenModeEntered = it

            if (it) {
                onFullScreenEnter()
            }
        }
    }

    LaunchedEffect(controllerConfig, repeatMode) {
        defaultPlayerView.setRepeatToggleModes(
            if (controllerConfig.showRepeatModeButton) {
                REPEAT_TOGGLE_MODE_ALL or REPEAT_TOGGLE_MODE_ONE
            } else {
                REPEAT_TOGGLE_MODE_NONE
            },
        )
        player.repeatMode = repeatMode.toExoPlayerRepeatMode()
    }

    LaunchedEffect(volume) {
        player.volume = volume
    }

    VideoPlayerSurface(
        modifier = modifier,
        defaultPlayerView = defaultPlayerView,
        player = player,
        usePlayerController = usePlayerController,
        handleLifecycle = handleLifecycle,
        enablePip = enablePip,
        surfaceResizeMode = resizeMode,
        lifecycle = getCurrentLifecycleState()
    )

    if (isFullScreenModeEntered) {
        var fullScreenPlayerView by remember { mutableStateOf<PlayerView?>(null) }

        VideoPlayerFullScreenDialog(
            player = player,
            currentPlayerView = defaultPlayerView,
            controllerConfig = controllerConfig,
            repeatMode = repeatMode,
            resizeMode = resizeMode,
            onDismissRequest = {
                fullScreenPlayerView?.let {
                    PlayerView.switchTargetView(player, it, defaultPlayerView)
                    defaultPlayerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
                        .performClick()
                    val currentActivity = context.findActivity()
                    currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    currentActivity.setFullScreen(false)
                    onFullScreenExit()
                }
                isFullScreenModeEntered = false
            },
            securePolicy = fullScreenSecurePolicy,
            enablePip = enablePip,
            fullScreenPlayerView = {
                fullScreenPlayerView = this
            },
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoPlayerSurface(
    modifier: Modifier = Modifier,
    defaultPlayerView: PlayerView,
    player: Player,
    usePlayerController: Boolean,
    handleLifecycle: Boolean,
    enablePip: Boolean,
    surfaceResizeMode: ResizeMode,
    onPipEntered: () -> Unit = {},
    autoDispose: Boolean = true,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
) {
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val context = LocalContext.current
    var isPendingPipMode by remember { mutableStateOf(false) }
    var surfaceHintRect: Rect
    val handler = Handler(Looper.getMainLooper())

    val updateInfoRunnable = object : Runnable {
        override fun run() {
            // Get the information you need from the player
            Log.d("inside runnable", "${player.currentPosition}")
            (player as CustomForwardingExoPlayer).changeTrackParam()
            // Schedule the next update in 10 second
            handler.postDelayed(this, 10000)
        }
    }

    fun startUpdatingInfo() {
        handler.post(updateInfoRunnable)
    }

    fun stopUpdatingInfo() {
        handler.removeCallbacks(updateInfoRunnable)
    }

    LaunchedEffect(player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("inside runnable", "onIsPlayingChanged $isPlaying")

                if (isPlaying) {
                    startUpdatingInfo()
                } else {
                    stopUpdatingInfo()
                }
            }
        })
    }
    DisposableEffect(
        Box(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                modifier = modifier.onGloballyPositioned {
                    surfaceHintRect = run {
                        val boundsInWindow = it.boundsInWindow()
                        Rect(
                            boundsInWindow.left.toInt(),
                            boundsInWindow.top.toInt(),
                            boundsInWindow.right.toInt(),
                            boundsInWindow.bottom.toInt()
                        )
                    }
                },
                factory = {
                    defaultPlayerView.apply {
                        useController = usePlayerController
                        resizeMode = surfaceResizeMode.toPlayerViewResizeMode()
                        setBackgroundColor(Color.BLACK)
                    }
                },
            )
        }
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (handleLifecycle) {
                        player.pause()
                        stopUpdatingInfo()
                    }

                    if (enablePip && player.playWhenReady) {
                        isPendingPipMode = true

                        Handler(Looper.getMainLooper()).post {
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                enterPIPMode(context, defaultPlayerView)
                                onPipEntered()
                            }
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            isPendingPipMode = false
                        }, 500)

                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (handleLifecycle) {
                        player.play()
                        startUpdatingInfo()
                    }

                    if (enablePip && player.playWhenReady) {
                        defaultPlayerView.useController = usePlayerController
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    val isPipMode = context.isActivityStatePipMode()

                    if (handleLifecycle || (enablePip && isPipMode && !isPendingPipMode)) {
                        player.stop()
                        stopUpdatingInfo()
                    }
                }

                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            if (autoDispose) {
                player.release()
                lifecycle.removeObserver(observer)
            }
        }
    }
}

