package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.VideoPlaybackProgressEntity
import com.example.model.VideoItem
import com.example.model.VideoSortOrder
import com.example.ui.AsyncLoadState
import com.example.ui.components.AdMobBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassmorphicSurface
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.formatTimeMs

enum class VideosSectionTab(val label: String) {
    OFFLINE("Offline Videos"),
    ONLINE("Online Videos"),
    SAVED("History & Saved")
}

private val ONLINE_VIDEO_SUGGESTIONS = listOf(
    "Trending India" to "trending hindi song video india",
    "Hindi Songs" to "new hindi song video 2025",
    "Bollywood Hits" to "bollywood hit video songs",
    "Punjabi Hits" to "latest punjabi video songs",
    "Arijit Singh" to "arijit singh video songs",
    "Bhakti & Sufi" to "hindi sufi bhajan video song"
)

@Composable
fun VideosScreen(
    isScanningVideos: Boolean,
    hasVideoPermission: Boolean,
    offlineVideos: List<VideoItem>,
    favoriteVideoIds: Set<String>,
    favoriteVideos: List<VideoItem>,
    videoWatchHistory: List<Pair<VideoItem, VideoPlaybackProgressEntity>>,
    progressList: List<VideoPlaybackProgressEntity>,
    onlineSearchState: AsyncLoadState<List<VideoItem>>,
    onlineTrendingState: AsyncLoadState<List<VideoItem>>,
    onInitOnlineLoad: () -> Unit,
    onRequestVideoPermission: () -> Unit,
    onRescanVideos: () -> Unit,
    onSearchOnlineVideos: (String) -> Unit,
    onPlayDirectUrl: (url: String, title: String) -> Unit,
    onPlayVideos: (List<VideoItem>, Int) -> Unit,
    onToggleFavoriteVideo: (VideoItem) -> Unit,
    onShareVideo: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onClearVideoHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(VideosSectionTab.OFFLINE) }
    var offlineSearchQuery by remember { mutableStateOf("") }
    var onlineSearchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(VideoSortOrder.NEWEST) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var infoDialogVideo by remember { mutableStateOf<VideoItem?>(null) }
    var confirmDeleteVideo by remember { mutableStateOf<VideoItem?>(null) }

    val progressMap = remember(progressList) { progressList.associateBy { it.videoId } }

    LaunchedEffect(Unit, hasVideoPermission) {
        if (hasVideoPermission) {
            onRescanVideos()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == VideosSectionTab.ONLINE) {
            onInitOnlineLoad()
        }
    }

    val folderNames = remember(offlineVideos) {
        listOf("All") + offlineVideos.map { it.subtitle.ifBlank { "Device Videos" } }.distinct().sorted()
    }

    val filteredOfflineVideos = remember(offlineVideos, offlineSearchQuery, selectedFolder, sortOrder) {
        val base = offlineVideos.filter { video ->
            val matchesFolder = selectedFolder == "All" || video.subtitle == selectedFolder
            val matchesQuery = offlineSearchQuery.isBlank() ||
                video.title.contains(offlineSearchQuery, ignoreCase = true) ||
                video.subtitle.contains(offlineSearchQuery, ignoreCase = true)
            matchesFolder && matchesQuery
        }
        when (sortOrder) {
            VideoSortOrder.NEWEST -> base.sortedByDescending { it.dateAddedSec }
            VideoSortOrder.TITLE -> base.sortedBy { it.title.lowercase() }
            VideoSortOrder.DURATION -> base.sortedByDescending { it.durationMs }
            VideoSortOrder.SIZE -> base.sortedByDescending { it.sizeBytes }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Videos", style = MaterialTheme.typography.headlineMedium)
                }
                Row {
                    IconButton(
                        onClick = { showUrlDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("videos_url_button")
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = "Stream video URL")
                    }
                    IconButton(
                        onClick = onRescanVideos,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("videos_rescan_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan videos")
                    }
                }
            }

            if (isScanningVideos) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VideosSectionTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("videos_tab_${tab.name.lowercase()}")
                    )
                }
            }

            if (selectedTab == VideosSectionTab.OFFLINE) {
                OutlinedTextField(
                    value = offlineSearchQuery,
                    onValueChange = { offlineSearchQuery = it },
                    placeholder = { Text("Search device videos, folders...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("offline_video_search")
                )
            } else if (selectedTab == VideosSectionTab.ONLINE) {
                OutlinedTextField(
                    value = onlineSearchQuery,
                    onValueChange = { onlineSearchQuery = it },
                    placeholder = { Text("Search YouTube India videos, Hindi songs...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        TextButton(onClick = { onSearchOnlineVideos(onlineSearchQuery) }) {
                            Text("Go")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchOnlineVideos(onlineSearchQuery) }),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("online_video_search")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "videos_ad_banner") {
                AdMobBanner()
            }

            when (selectedTab) {
                VideosSectionTab.OFFLINE -> {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(VideoSortOrder.entries) { order ->
                                FilterChip(
                                    selected = sortOrder == order,
                                    onClick = { sortOrder = order },
                                    label = { Text("Sort: ${order.label}") }
                                )
                            }
                        }
                    }

                    if (folderNames.size > 2) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(folderNames) { folder ->
                                    FilterChip(
                                        selected = selectedFolder == folder,
                                        onClick = { selectedFolder = folder },
                                        label = { Text(folder) }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredOfflineVideos.isEmpty()) {
                        item {
                            GlassmorphicSurface(cornerRadius = 24.dp, contentPadding = 20.dp) {
                                if (!hasVideoPermission) {
                                    Text("Allow Device Videos", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Grant access to automatically show all videos stored on your phone with previews and full player controls.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = onRequestVideoPermission,
                                        modifier = Modifier.testTag("grant_video_permission_btn")
                                    ) {
                                        Text("Allow Videos")
                                    }
                                } else {
                                    Text("Device Videos", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (isScanningVideos) "Scanning all videos on your device..."
                                        else "No videos found matching your filter on device storage.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(onClick = onRescanVideos) {
                                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Scan Again")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${filteredOfflineVideos.size} Device Video(s)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                TextButton(onClick = { onPlayVideos(filteredOfflineVideos, 0) }) {
                                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Play All")
                                }
                            }
                        }
                        itemsIndexed(filteredOfflineVideos, key = { _, v -> v.id }) { idx, video ->
                            VideoItemCard(
                                video = video,
                                isFavorite = video.id in favoriteVideoIds,
                                progress = progressMap[video.id],
                                onPlay = { onPlayVideos(filteredOfflineVideos, idx) },
                                onToggleFavorite = { onToggleFavoriteVideo(video) },
                                onShare = { onShareVideo(video) },
                                onInfo = { infoDialogVideo = video }
                            )
                        }
                    }
                }

                VideosSectionTab.ONLINE -> {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                AssistChip(
                                    onClick = { showUrlDialog = true },
                                    leadingIcon = { Icon(Icons.Rounded.Link, null, Modifier.size(16.dp)) },
                                    label = { Text("Open URL") }
                                )
                            }
                            items(ONLINE_VIDEO_SUGGESTIONS) { (label, keyword) ->
                                AssistChip(
                                    onClick = {
                                        onlineSearchQuery = keyword
                                        onSearchOnlineVideos(keyword)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    val activeResult = if (onlineSearchState is AsyncLoadState.Idle) onlineTrendingState else onlineSearchState
                    when (activeResult) {
                        is AsyncLoadState.Loading -> item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AsyncLoadState.Error -> item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Text("No matching video found", style = MaterialTheme.typography.titleMedium)
                                Text(activeResult.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onSearchOnlineVideos(onlineSearchQuery) }) {
                                        Icon(Icons.Rounded.Refresh, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                    OutlinedButton(onClick = { showUrlDialog = true }) {
                                        Text("Paste URL")
                                    }
                                }
                            }
                        }
                        is AsyncLoadState.Success -> {
                            itemsIndexed(activeResult.data, key = { _, v -> v.id }) { idx, video ->
                                VideoItemCard(
                                    video = video,
                                    isFavorite = video.id in favoriteVideoIds,
                                    progress = progressMap[video.id],
                                    onPlay = { onPlayVideos(activeResult.data, idx) },
                                    onToggleFavorite = { onToggleFavoriteVideo(video) },
                                    onShare = { onShareVideo(video) },
                                    onInfo = { infoDialogVideo = video }
                                )
                            }
                        }
                        AsyncLoadState.Idle -> Unit
                    }
                }

                VideosSectionTab.SAVED -> {
                    if (videoWatchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Continue Watching", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = onClearVideoHistory) { Text("Clear History") }
                            }
                        }
                        itemsIndexed(videoWatchHistory, key = { _, pair -> "hist_${pair.first.id}" }) { idx, (video, prog) ->
                            val allHistoryVideos = remember(videoWatchHistory) { videoWatchHistory.map { it.first } }
                            VideoItemCard(
                                video = video,
                                isFavorite = video.id in favoriteVideoIds,
                                progress = prog,
                                onPlay = { onPlayVideos(allHistoryVideos, idx) },
                                onToggleFavorite = { onToggleFavoriteVideo(video) },
                                onShare = { onShareVideo(video) },
                                onInfo = { infoDialogVideo = video }
                            )
                        }
                    }

                    item {
                        Text(
                            "Favourite Videos (${favoriteVideos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (favoriteVideos.isEmpty()) {
                        item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Text(
                                    "Tap the heart icon on any offline or online video to save it here.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(favoriteVideos, key = { _, v -> "fav_${v.id}" }) { idx, video ->
                            VideoItemCard(
                                video = video,
                                isFavorite = true,
                                progress = progressMap[video.id],
                                onPlay = { onPlayVideos(favoriteVideos, idx) },
                                onToggleFavorite = { onToggleFavoriteVideo(video) },
                                onShare = { onShareVideo(video) },
                                onInfo = { infoDialogVideo = video }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUrlDialog) {
        var urlInput by remember { mutableStateOf("") }
        var titleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Stream Online Video URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste any YouTube India link or direct .mp4 / .m3u8 / .webm link to stream inside the full video player.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("https://... video or YouTube link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("direct_video_url_input")
                    )
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Title (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUrlDialog = false
                        onPlayDirectUrl(urlInput, titleInput)
                    }
                ) { Text("Play Video") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
            }
        )
    }

    infoDialogVideo?.let { video ->
        val isFav = video.id in favoriteVideoIds
        AlertDialog(
            onDismissRequest = { infoDialogVideo = null },
            title = {
                Text(
                    video.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Source: ${if (video.youtubeId != null) "YouTube India" else if (video.isOnline) "Online Stream" else "Device Storage"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Channel / Folder: ${video.subtitle}", style = MaterialTheme.typography.bodyMedium)
                    Text("Resolution: ${video.resolutionLabel}", style = MaterialTheme.typography.bodyMedium)
                    if (video.durationMs > 0L) {
                        Text("Duration: ${formatTimeMs(video.durationMs)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (video.formattedSize.isNotBlank()) {
                        Text("File Size: ${video.formattedSize}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onToggleFavoriteVideo(video)
                                infoDialogVideo = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isFav) "Saved" else "Favourite")
                        }
                        OutlinedButton(
                            onClick = {
                                onShareVideo(video)
                                infoDialogVideo = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                    Button(
                        onClick = {
                            infoDialogVideo = null
                            confirmDeleteVideo = video
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_info_delete_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (video.isOnline) "Remove from Saved" else "Delete Video")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        infoDialogVideo = null
                        onPlayVideos(listOf(video), 0)
                    }
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play")
                }
            },
            dismissButton = {
                TextButton(onClick = { infoDialogVideo = null }) { Text("Close") }
            }
        )
    }

    confirmDeleteVideo?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteVideo = null },
            title = { Text(if (target.isOnline) "Remove Video?" else "Delete Video from Device?") },
            text = {
                Text(
                    if (target.isOnline) "Remove \"${target.title}\" from your saved list?"
                    else "Permanently delete \"${target.title}\" from your phone storage?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteVideo = null
                        onDeleteVideo(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (target.isOnline) "Remove" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteVideo = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VideoItemCard(
    video: VideoItem,
    isFavorite: Boolean,
    progress: VideoPlaybackProgressEntity?,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val dur = progress?.durationMs?.takeIf { it > 0L } ?: video.durationMs
    val watchedFraction = if (progress != null && dur > 0L && progress.positionMs > 1500L) {
        (progress.positionMs.toFloat() / dur.toFloat()).coerceIn(0.03f, 1f)
    } else 0f

    GlassmorphicSurface(
        cornerRadius = 22.dp,
        contentPadding = 12.dp,
        tintAlpha = 0.34f,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .testTag("video_card_${video.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(122.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                VideoThumbnailImage(
                    thumbnailUri = video.thumbnailUri,
                    isOnline = video.isOnline,
                    contentDescription = video.title
                )

                // Play indicator badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Duration badge
                if (dur > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatTimeMs(dur),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Resume progress indicator bar at bottom of thumbnail
                if (watchedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(watchedFraction)
                                .background(primary)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(video.subtitle)
                        if (video.formattedSize.isNotBlank()) {
                            append(" • ")
                            append(video.formattedSize)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (video.isOnline) secondary.copy(alpha = 0.18f)
                                else primary.copy(alpha = 0.18f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (video.isOnline) "ONLINE • ${video.resolutionLabel}" else video.resolutionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (video.isOnline) secondary else primary
                        )
                    }
                    if (progress != null && progress.positionMs > 2000L) {
                        Text(
                            text = "Resume ${formatTimeMs(progress.positionMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = primary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favourite video",
                    tint = if (isFavorite) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.IosShare, contentDescription = "Share video")
            }
            IconButton(
                onClick = onInfo,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("video_info_${video.id}")
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Video info & delete")
            }
        }
    }
}
