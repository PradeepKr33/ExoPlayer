package com.saathi.exoplayer

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
class CustomPlayerView @OptIn(UnstableApi::class) @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {
    init {
        val aspectRatioFrameLayout =
            this.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
        aspectRatioFrameLayout.setAspectRatio(16f / 9f)
    }
}