package com.saathi.exoplayer.feature.uri

import android.net.Uri
import androidx.annotation.RawRes
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.MediaMetadata
import com.saathi.exoplayer.feature.uri.VideoPlayerMediaItem.AssetFileMediaItem
import com.saathi.exoplayer.feature.uri.VideoPlayerMediaItem.NetworkMediaItem
import com.saathi.exoplayer.feature.uri.VideoPlayerMediaItem.RawResourceMediaItem
import com.saathi.exoplayer.feature.uri.VideoPlayerMediaItem.StorageMediaItem

interface BaseVideoPlayerMediaItem {
    val mediaMetadata: MediaMetadata
    val mimeType: String
    val startPosition: Long
}

/**
 * Representation of a media item for [VideoPlayer].
 *
 * @see RawResourceMediaItem
 * @see AssetFileMediaItem
 * @see StorageMediaItem
 * @see NetworkMediaItem
 */
sealed interface VideoPlayerMediaItem : BaseVideoPlayerMediaItem {

    /**
     * A media item in the raw resource.
     * @param resourceId R.raw.xxxxx resource id
     * @param mediaMetadata Media Metadata. Default is empty.
     * @param mimeType Media mime type.
     */
    data class RawResourceMediaItem(
        @RawRes val resourceId: Int,
        override val mediaMetadata: MediaMetadata = MediaMetadata.EMPTY,
        override val mimeType: String = "",
        override val startPosition: Long = 0,
    ) : VideoPlayerMediaItem

    /**
     * A media item in the assets folder.
     * @param assetPath asset media file path (e.g If there is a test.mp4 file in the assets folder, test.mp4 becomes the assetPath.)
     * @throws androidx.media3.datasource.AssetDataSource.AssetDataSourceException asset file is not exist or load failed.
     * @param mediaMetadata Media Metadata. Default is empty.
     * @param mimeType Media mime type.
     */
    data class AssetFileMediaItem(
        val assetPath: String,
        override val mediaMetadata: MediaMetadata = MediaMetadata.EMPTY,
        override val mimeType: String = "",
        override val startPosition: Long = 0,
    ) : VideoPlayerMediaItem

    /**
     * A media item in the device internal / external storage.
     * @param storageUri storage file uri
     * @param mediaMetadata Media Metadata. Default is empty.
     * @param mimeType Media mime type.
     * @throws androidx.media3.datasource.FileDataSource.FileDataSourceException
     */
    data class StorageMediaItem(
        val storageUri: Uri,
        override val mediaMetadata: MediaMetadata = MediaMetadata.EMPTY,
        override val mimeType: String = "",
        override val startPosition: Long = 0,
    ) : VideoPlayerMediaItem

    /**
     * A media item in the internet
     * @param url network video url'
     * @param mediaMetadata Media Metadata. Default is empty.
     * @param mimeType Media mime type.
     * @param drmConfiguration Drm configuration for media. (Default is null)
     */
    data class NetworkMediaItem(
        val url: String,
        override val mediaMetadata: MediaMetadata = MediaMetadata.EMPTY,
        override val mimeType: String = "",
        override val startPosition: Long = 0,
        val drmConfiguration: DrmConfiguration? = null,
    ) : VideoPlayerMediaItem
}
