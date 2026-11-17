package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.example.model.Track
import com.example.model.TrackSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalMusicScanner(private val context: Context) {

    private val supportedMimePrefixes = setOf(
        "audio/mpeg", "audio/mp3", "audio/mp4", "audio/aac", "audio/x-aac",
        "audio/flac", "audio/x-flac", "audio/wav", "audio/x-wav", "audio/wave",
        "audio/ogg", "application/ogg", "audio/opus", "audio/m4a", "audio/x-m4a"
    )

    private val supportedExtensions = setOf(
        "mp3", "aac", "flac", "wav", "ogg", "m4a", "opus", "wma", "amr", "webm", "3gp", "alac", "aiff"
    )

    suspend fun scanDeviceAudio(excludedFolders: Set<String> = emptySet()): List<Track> =
        withContext(Dispatchers.IO) {
            val tracks = mutableListOf<Track>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val genreMap = loadGenreMap()

            val projection = buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.DATA)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATE_ADDED)
                add(MediaStore.Audio.Media.MIME_TYPE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(MediaStore.Audio.Media.GENRE)
                }
            }.toTypedArray()

            // Query all audio files on device (including downloads where IS_MUSIC may be 0)
            val selection = "${MediaStore.Audio.Media.DURATION} >= 3000 OR ${MediaStore.Audio.Media.SIZE} >= 30000"
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            runCatching {
                context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                    val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    } else -1

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val path = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                        if (path.contains("/system/media/audio/", ignoreCase = true) ||
                            path.contains("/product/media/audio/", ignoreCase = true)
                        ) {
                            continue
                        }

                        val title = cursor.getString(titleCol).orEmpty().ifBlank {
                            path.substringAfterLast('/').substringBeforeLast('.', "Untitled Track")
                        }
                        val artist = cursor.getString(artistCol).orEmpty()
                            .let { if (it.isBlank() || it == "<unknown>") "Unknown Artist" else it }
                        val album = cursor.getString(albumCol).orEmpty()
                            .let { if (it.isBlank() || it == "<unknown>") "Single" else it }
                        val albumId = cursor.getLong(albumIdCol)
                        val duration = cursor.getLong(durationCol)
                        val folderPath = path.substringBeforeLast('/', "")
                        val size = cursor.getLong(sizeCol)
                        val dateAdded = cursor.getLong(dateCol)
                        val mimeType = cursor.getString(mimeCol).orEmpty().lowercase()
                        val ext = path.substringAfterLast('.', "").lowercase()

                        val isAudio = mimeType.startsWith("audio/") ||
                            mimeType == "application/ogg" ||
                            ext in supportedExtensions
                        if (!isAudio) continue

                        if (excludedFolders.any { excluded ->
                                folderPath.equals(excluded, ignoreCase = true) ||
                                    folderPath.startsWith("$excluded/", ignoreCase = true)
                            }
                        ) {
                            continue
                        }

                        val genre = (if (genreCol >= 0) cursor.getString(genreCol) else null)
                            ?.takeIf { it.isNotBlank() }
                            ?: genreMap[id]
                            ?: "Music"

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val artworkUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()

                        tracks.add(
                            Track(
                                id = "local_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                albumId = albumId,
                                genre = genre,
                                durationMs = duration,
                                uri = contentUri.toString(),
                                artworkUri = artworkUri,
                                folderPath = folderPath,
                                sizeBytes = size,
                                dateAddedSec = dateAdded,
                                mimeType = mimeType.ifBlank { "audio/mpeg" },
                                sourceType = TrackSourceType.LOCAL
                            )
                        )
                    }
                }
            }
            tracks
        }

    private fun loadGenreMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        runCatching {
            val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                genresUri,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null,
                null,
                null
            )?.use { genreCursor ->
                val idIdx = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val nameIdx = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(idIdx)
                    val genreName = genreCursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null
                    )?.use { memberCursor ->
                        val audioIdx = memberCursor.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        if (audioIdx >= 0) {
                            while (memberCursor.moveToNext()) {
                                map[memberCursor.getLong(audioIdx)] = genreName
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    suspend fun extractTrackFromUri(uri: Uri): Track = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Audio File"
        var artist = "Unknown Artist"
        var album = "External Audio"
        var genre = "Music"
        var duration = 0L

        runCatching {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }?.let { genre = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { duration = it }
        }
        runCatching { retriever.release() }

        Track(
            id = "saf_${uri.toString().hashCode()}",
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            durationMs = duration,
            uri = uri.toString(),
            folderPath = uri.path?.let { File(it).parent ?: "SAF" } ?: "SAF",
            dateAddedSec = System.currentTimeMillis() / 1000L,
            sourceType = TrackSourceType.EXTERNAL_URI
        )
    }

    fun detectDuplicates(tracks: List<Track>): List<List<Track>> {
        return tracks
            .filter { it.isLocal }
            .groupBy { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}|${it.durationMs / 2000L}" }
            .values
            .filter { it.size > 1 }
    }

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun setAsRingtone(track: Track): Result<String> {
        return runCatching {
            if (!track.isLocal) error("Only local audio tracks can be assigned as ringtone.")
            if (!Settings.System.canWrite(context)) {
                error("Settings modification is required to assign ringtone.")
            }
            val uri = Uri.parse(track.uri)
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                uri
            )
            "${track.title} set as default ringtone"
        }
    }

    fun createDeleteRequestIntent(tracks: List<Track>): android.app.PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = tracks.filter { it.isLocal }.map { Uri.parse(it.uri) }
            if (uris.isEmpty()) return null
            return MediaStore.createDeleteRequest(context.contentResolver, uris)
        }
        return null
    }

    suspend fun deleteTracksLegacy(tracks: List<Track>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        tracks.filter { it.isLocal }.forEach { track ->
            runCatching {
                val count = context.contentResolver.delete(Uri.parse(track.uri), null, null)
                if (count > 0) deleted++
            }
        }
        deleted
    }

    fun buildShareIntent(tracks: List<Track>): android.content.Intent? {
        if (tracks.isEmpty()) return null
        val localUris = tracks.filter { it.isLocal }.map { Uri.parse(it.uri) }
        return if (localUris.size == 1 && tracks.size == 1) {
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(android.content.Intent.EXTRA_STREAM, localUris.first())
                putExtra(android.content.Intent.EXTRA_SUBJECT, "${tracks.first().title} - ${tracks.first().artist}")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else if (localUris.size > 1) {
            android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, ArrayList(localUris))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val summary = tracks.joinToString("\n") { "${it.title} — ${it.artist}\n${it.uri}" }
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, summary)
            }
        }
    }
}
