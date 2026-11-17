package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object VideoThumbnailCache {
    val cache = LruCache<String, Bitmap>(32)
}

@Composable
fun VideoThumbnailImage(
    thumbnailUri: String?,
    isOnline: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bgBrush = Brush.linearGradient(
        listOf(primary.copy(alpha = 0.28f), secondary.copy(alpha = 0.18f), MaterialTheme.colorScheme.surface)
    )

    if (thumbnailUri.isNullOrBlank()) {
        Box(
            modifier = modifier.fillMaxSize().background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = contentDescription,
                tint = primary,
                modifier = Modifier.size(30.dp)
            )
        }
        return
    }

    if (isOnline || thumbnailUri.startsWith("http://") || thumbnailUri.startsWith("https://")) {
        SubcomposeAsyncImage(
            model = thumbnailUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
            loading = {
                Box(modifier = Modifier.fillMaxSize().background(bgBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = primary.copy(alpha = 0.8f))
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize().background(bgBrush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = primary)
                }
            }
        )
        return
    }

    val context = LocalContext.current
    var bitmap by remember(thumbnailUri) {
        mutableStateOf(VideoThumbnailCache.cache.get(thumbnailUri))
    }

    LaunchedEffect(thumbnailUri) {
        if (bitmap != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(thumbnailUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        context.contentResolver.loadThumbnail(uri, Size(480, 270), null)
                    }.getOrNull()
                } else {
                    null
                } ?: run {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                    } finally {
                        runCatching { retriever.release() }
                    }
                }
            }.getOrNull()
        }
        if (loaded != null) {
            VideoThumbnailCache.cache.put(thumbnailUri, loaded)
            bitmap = loaded
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize().background(bgBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Movie, contentDescription = contentDescription, tint = primary)
        }
    }
}
