package com.example.data.scanner

import android.Manifest
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalVideoScanner(private val context: Context) {

    private val videoExtensions = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "3gp", "avi", "ts", "flv", "wmv"
    )

    suspend fun scanMediaStoreVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val canQueryMediaStore = ContextCompat.checkSelfPermission(
            context,
            perm
        ) == PackageManager.PERMISSION_GRANTED
        if (!canQueryMediaStore) return@withContext emptyList()

        val videos = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val title = cursor.getString(titleCol).orEmpty()
                        .ifBlank { displayName.substringBeforeLast('.', "Device Video") }
                    val folderPath = path.substringBeforeLast('/', "Device Videos")
                    val folderName = folderPath.substringAfterLast('/', "Device Videos").ifBlank { "Device Videos" }
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    videos += VideoItem(
                        id = "ms_video_$id",
                        title = title,
                        subtitle = folderName,
                        uri = contentUri,
                        thumbnailUri = contentUri,
                        durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                        width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                        height = if (heightCol >= 0) cursor.getInt(heightCol) else 0,
                        sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                        dateAddedSec = cursor.getLong(dateCol).coerceAtLeast(0L),
                        mimeType = cursor.getString(mimeCol).orEmpty().ifBlank { "video/mp4" },
                        folderPath = folderPath,
                        isOnline = false
                    )
                }
            }
        }
        videos
    }

    suspend fun extractVideoFromUri(
        uri: Uri,
        folderLabel: String = "Device Videos"
    ): VideoItem = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Video File"
        var sizeBytes = 0L

        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L)
                }
            }
        }

        var title = displayName.substringBeforeLast('.', displayName).ifBlank { "Video File" }
        var durationMs = 0L
        var width = 0
        var height = 0

        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()?.let { width = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()?.let { height = it }
        }
        runCatching { retriever.release() }

        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
            .ifBlank { "video/mp4" }

        VideoItem(
            id = "local_vid_${uri.toString().hashCode()}",
            title = title,
            subtitle = folderLabel,
            uri = uri.toString(),
            thumbnailUri = uri.toString(),
            durationMs = durationMs,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            dateAddedSec = System.currentTimeMillis() / 1000L,
            mimeType = mimeType,
            folderPath = folderLabel,
            isOnline = false
        )
    }

    suspend fun scanFolderTreeUri(treeUri: Uri): List<VideoItem> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return@withContext emptyList()
        val folderName = rootDocId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Video Folder" }
        val results = mutableListOf<VideoItem>()
        collectVideosInTree(treeUri, rootDocId, folderName, depth = 0, out = results)
        results
    }

    private suspend fun collectVideosInTree(
        treeUri: Uri,
        parentDocId: String,
        folderName: String,
        depth: Int,
        out: MutableList<VideoItem>
    ) {
        if (depth > 2 || out.size >= 120) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        val childDirs = mutableListOf<Pair<String, String>>()
        val videoDocs = mutableListOf<String>()

        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx).orEmpty() else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty().lowercase() else ""
                    val ext = name.substringAfterLast('.', "").lowercase()

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        childDirs += docId to name.ifBlank { folderName }
                    } else if (mime.startsWith("video/") || ext in videoExtensions) {
                        videoDocs += docId
                    }
                }
            }
        }

        for (docId in videoDocs) {
            if (out.size >= 120) break
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            out += extractVideoFromUri(docUri, folderName)
        }
        for ((subId, subName) in childDirs) {
            if (out.size >= 120) break
            collectVideosInTree(treeUri, subId, subName, depth + 1, out)
        }
    }

    fun createDeleteRequestIntent(video: VideoItem): PendingIntent? {
        if (video.isOnline || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val uri = runCatching { Uri.parse(video.uri) }.getOrNull() ?: return null
        return runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
        }.getOrNull()
    }

    suspend fun deleteVideoLegacy(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        if (video.isOnline) return@withContext false
        val uri = runCatching { Uri.parse(video.uri) }.getOrNull() ?: return@withContext false
        runCatching {
            context.contentResolver.delete(uri, null, null) > 0
        }.getOrElse { false }
    }

    fun buildShareIntent(video: VideoItem): Intent {
        return if (!video.isOnline) {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(video.uri))
                putExtra(Intent.EXTRA_SUBJECT, video.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, video.title)
                putExtra(Intent.EXTRA_TEXT, "${video.title} — ${video.subtitle}\n${video.uri}")
            }
        }
    }
}
