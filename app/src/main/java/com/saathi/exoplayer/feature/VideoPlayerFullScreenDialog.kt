package com.saathi.exoplayer.feature

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.View
import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.ui.PlayerView
import com.saathi.exoplayer.CustomPlayerView
import com.saathi.exoplayer.R
import com.saathi.exoplayer.feature.controller.VideoPlayerControllerConfig
import com.saathi.exoplayer.feature.controller.applyToExoPlayerView
import com.saathi.exoplayer.feature.util.findActivity
import com.saathi.exoplayer.feature.util.setFullScreen


/**
 * ExoPlayer does not support full screen views by default.
 * So create a full screen modal that wraps the Compose Dialog.
 *
 * Delegate all functions of the video controller that were used just before
 * the full screen to the video controller managed by that component.
 * Conversely, if the full screen dismissed, it will restore all the functions it delegated
 * for synchronization with the video controller on the full screen and the video controller on the previous screen.
 *
 * @param player Exoplayer instance.
 * @param currentPlayerView [androidx.media3.ui.PlayerView] instance currently in use for playback.
 * @param fullScreenPlayerView Callback to return all features to existing video player controller.
 * @param controllerConfig Player controller config. You can customize the Video Player Controller UI.
 * @param repeatMode Sets the content repeat mode.
 * @param enablePip Enable PIP.
 * @param onDismissRequest Callback that occurs when modals are closed.
 * @param securePolicy Policy on setting [android.view.WindowManager.LayoutParams.FLAG_SECURE] on a full screen dialog window.
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoPlayerFullScreenDialog(
    player: Player,
    currentPlayerView: PlayerView,
    fullScreenPlayerView: PlayerView.() -> Unit,
    controllerConfig: VideoPlayerControllerConfig,
    repeatMode: RepeatMode,
    resizeMode: ResizeMode = ResizeMode.FILL,
    enablePip: Boolean,
    onDismissRequest: () -> Unit,
    securePolicy: SecureFlagPolicy,
) {
    val context = LocalContext.current
    val internalFullScreenPlayerView = remember {
        CustomPlayerView(context)
            .also(fullScreenPlayerView)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            securePolicy = securePolicy,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        LaunchedEffect(Unit) {
            PlayerView.switchTargetView(player, currentPlayerView, internalFullScreenPlayerView)
            val currentActivity = context.findActivity()
            currentActivity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            //View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            currentActivity.setFullScreen(true)

            (view.parent as DialogWindowProvider).window.setFullScreen(true)
        }

        LaunchedEffect(controllerConfig) {
            controllerConfig.applyToExoPlayerView(internalFullScreenPlayerView, context) {
                if (!it) {
                    onDismissRequest()
                }
            }
            internalFullScreenPlayerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
                .performClick()
        }

        LaunchedEffect(controllerConfig, repeatMode) {
            internalFullScreenPlayerView.setRepeatToggleModes(
                if (controllerConfig.showRepeatModeButton) {
                    RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL or RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
                } else {
                    RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            VideoPlayerSurface(
                defaultPlayerView = internalFullScreenPlayerView,
                player = player,
                usePlayerController = true,
                handleLifecycle = !enablePip,
                autoDispose = false,
                enablePip = enablePip,
                surfaceResizeMode = resizeMode,
                onPipEntered = { onDismissRequest() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
            )
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.back_icon),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clickable { onDismissRequest() })
        }
    }
}
