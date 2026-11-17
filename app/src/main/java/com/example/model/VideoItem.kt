package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoItem(
    val id: String,
    val title: String,
    val subtitle: String = "Device Video",
    val uri: String,
    val thumbnailUri: String? = null,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val dateAddedSec: Long = 0L,
    val mimeType: String = "video/mp4",
    val folderPath: String = "Device Videos",
    val isOnline: Boolean = false,
    val description: String = "",
    val youtubeId: String? = null
) {
    val resolutionLabel: String
        get() = when {
            maxOf(width, height) >= 3840 -> "4K UHD"
            minOf(width, height) >= 1080 -> "1080p HD"
            minOf(width, height) >= 720 -> "720p HD"
            minOf(width, height) >= 480 -> "480p"
            width > 0 && height > 0 -> "${width}x${height}"
            isOnline -> "HD Stream"
            else -> "Video"
        }

    val formattedSize: String
        get() = when {
            sizeBytes <= 0L -> if (isOnline) "Online" else ""
            sizeBytes >= 1_073_741_824L -> "%.1f GB".format(sizeBytes / 1_073_741_824.0)
            sizeBytes >= 1_048_576L -> "%.1f MB".format(sizeBytes / 1_048_576.0)
            else -> "${(sizeBytes / 1024L).coerceAtLeast(1L)} KB"
        }
}

enum class VideoResizeOption(val label: String) {
    FIT("Fit"),
    FILL("Stretch"),
    ZOOM("Zoom")
}

enum class VideoSortOrder(val label: String) {
    NEWEST("Newest"),
    TITLE("Name"),
    DURATION("Duration"),
    SIZE("Size")
}
