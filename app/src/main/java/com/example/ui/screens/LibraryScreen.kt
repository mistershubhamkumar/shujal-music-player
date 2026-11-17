package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackStatsEntity
import com.example.model.AlbumGroup
import com.example.model.ArtistGroup
import com.example.model.FolderGroup
import com.example.model.GenreGroup
import com.example.model.SmartPlaylist
import com.example.model.Track
import com.example.ui.components.AdMobBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.TrackArtworkImage

enum class LibraryTab(val label: String) {
    SONGS("All Songs"),
    ONLINE("Online Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    GENRES("Genres"),
    PLAYLISTS("Playlists"),
    SMART("Smart Mixes"),
    FAVOURITES("Favourites")
}

@Composable
fun LibraryScreen(
    isScanning: Boolean,
    localTracks: List<Track>,
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    folders: List<FolderGroup>,
    genres: List<GenreGroup>,
    playlists: List<PlaylistEntity>,
    smartPlaylists: List<SmartPlaylist>,
    playlistRefs: List<PlaylistTrackCrossRef>,
    trackStats: List<TrackStatsEntity>,
    currentTrackId: String?,
    onRescan: () -> Unit,
    onOpenSafFile: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onExportPlaylistM3u: (PlaylistEntity) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onShareSelectedTracks: (List<Track>) -> Unit,
    onDeleteSelectedTracks: (List<Track>) -> Unit,
    onlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Long-press multi-select state
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectionMode = selectedIds.isNotEmpty()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statMap = remember(trackStats) { trackStats.associateBy { it.trackId } }
    val favTrackIds = remember(trackStats) { trackStats.filter { it.isFavorite }.map { it.trackId }.toSet() }

    val filteredSongs = remember(localTracks, searchQuery) {
        if (searchQuery.isBlank()) localTracks
        else localTracks.filter {
            it.title.contains(searchQuery, true) ||
                it.artist.contains(searchQuery, true) ||
                it.album.contains(searchQuery, true)
        }
    }

    val favTracks = remember(localTracks, favTrackIds) { localTracks.filter { it.id in favTrackIds } }

    fun toggleSelection(trackId: String) {
        selectedIds = if (trackId in selectedIds) selectedIds - trackId else selectedIds + trackId
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Multi-select action bar when long-pressed
        AnimatedVisibility(visible = selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("multi_select_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel selection")
                    }
                    Text("${selectedIds.size} selected", style = MaterialTheme.typography.titleMedium)
                }
                Row {
                    IconButton(
                        onClick = {
                            val pool = if (selectedTab == LibraryTab.FAVOURITES) favTracks else filteredSongs
                            selectedIds = if (selectedIds.size == pool.size) emptySet() else pool.map { it.id }.toSet()
                        },
                        modifier = Modifier.testTag("select_all_button")
                    ) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                    }
                    IconButton(
                        onClick = {
                            val chosen = localTracks.filter { it.id in selectedIds }
                            onShareSelectedTracks(chosen)
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.testTag("share_selected_button")
                    ) {
                        Icon(Icons.Rounded.IosShare, contentDescription = "Share selected")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_selected_button")
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete selected",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (!selectionMode) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Library", style = MaterialTheme.typography.headlineMedium)
                    Row {
                        IconButton(onClick = onOpenSafFile) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "Open audio file")
                        }
                        IconButton(onClick = onRescan) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Rescan music")
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Create playlist")
                        }
                    }
                }

                if (isScanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (selectedTab != LibraryTab.ONLINE) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search your songs, artists, albums...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().testTag("library_search_input")
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibraryTab.entries) { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedIds = emptySet()
                                selectedTab = tab
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (selectedTab == LibraryTab.ONLINE) {
            onlineContent()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "library_ad_banner") {
                    AdMobBanner()
                }
                when (selectedTab) {
                    LibraryTab.SONGS -> {
                        if (filteredSongs.isEmpty()) {
                            item {
                                EmptyLibraryCard("No songs found on device. Tap the folder icon above to open an audio file or rescan.")
                            }
                        } else {
                            itemsIndexed(filteredSongs, key = { _, t -> t.id }) { idx, track ->
                                TrackItemRow(
                                    track = track,
                                    isActive = track.id == currentTrackId,
                                    isFavorite = track.id in favTrackIds,
                                    selectionMode = selectionMode,
                                    isSelected = track.id in selectedIds,
                                    onClick = {
                                        if (selectionMode) toggleSelection(track.id)
                                        else onPlayTracks(filteredSongs, idx)
                                    },
                                    onLongClick = { toggleSelection(track.id) },
                                    onFavoriteClick = { onToggleFavorite(track) },
                                    onMoreClick = { onOpenTrackOptions(track) }
                                )
                            }
                        }
                    }
                    LibraryTab.ALBUMS -> {
                        items(albums, key = { "${it.id}_${it.name}" }) { album ->
                            GlassCard(Modifier.fillMaxWidth(), onClick = { onPlayTracks(album.tracks, 0) }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))) {
                                        TrackArtworkImage(album.artworkUri, false, album.name)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(album.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${album.artist} • ${album.tracks.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.Album, null)
                                }
                            }
                        }
                    }
                    LibraryTab.ARTISTS -> {
                        items(artists, key = { it.name }) { artist ->
                            GlassCard(Modifier.fillMaxWidth(), onClick = { onPlayTracks(artist.tracks, 0) }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Rounded.Person, null, modifier = Modifier.size(34.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(artist.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${artist.albumCount} albums • ${artist.tracks.size} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    LibraryTab.FOLDERS -> {
                        items(folders, key = { it.path }) { folder ->
                            GlassCard(Modifier.fillMaxWidth(), onClick = { onPlayTracks(folder.tracks, 0) }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Rounded.Folder, null)
                                    Column(Modifier.weight(1f)) {
                                        Text(folder.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${folder.tracks.size} songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onExcludeFolder(folder.path) }) {
                                        Icon(Icons.Rounded.FolderOff, contentDescription = "Exclude folder")
                                    }
                                }
                            }
                        }
                    }
                    LibraryTab.GENRES -> {
                        items(genres, key = { it.name }) { genre ->
                            GlassCard(Modifier.fillMaxWidth(), onClick = { onPlayTracks(genre.tracks, 0) }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Rounded.MusicNote, null)
                                    Column(Modifier.weight(1f)) {
                                        Text(genre.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${genre.tracks.size} songs", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    LibraryTab.PLAYLISTS -> {
                        if (playlists.isEmpty()) {
                            item { EmptyLibraryCard("No playlists yet. Tap + at the top to create one.") }
                        } else {
                            items(playlists, key = { it.id }) { pl ->
                                val trackIds = playlistRefs.filter { it.playlistId == pl.id }.map { it.trackId }.toSet()
                                val tracks = localTracks.filter { it.id in trackIds }
                                GlassCard(Modifier.fillMaxWidth(), onClick = { if (tracks.isNotEmpty()) onPlayTracks(tracks, 0) }) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(pl.name, style = MaterialTheme.typography.titleMedium)
                                            Text("${tracks.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row {
                                            IconButton(onClick = { onExportPlaylistM3u(pl) }) {
                                                Icon(Icons.Rounded.FileUpload, contentDescription = "Export M3U8")
                                            }
                                            IconButton(onClick = { onDeletePlaylist(pl) }) {
                                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete playlist")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LibraryTab.SMART -> {
                        items(smartPlaylists) { smart ->
                            GlassCard(Modifier.fillMaxWidth(), onClick = { if (smart.tracks.isNotEmpty()) onPlayTracks(smart.tracks, 0) }) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(smart.kind.title, style = MaterialTheme.typography.titleMedium)
                                        Text("${smart.tracks.size} tracks • ${smart.kind.subtitle}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.PlayArrow, null)
                                }
                            }
                        }
                    }
                    LibraryTab.FAVOURITES -> {
                        if (favTracks.isEmpty()) {
                            item { EmptyLibraryCard("Tap the heart icon on any song to save it to Favourites.") }
                        } else {
                            itemsIndexed(favTracks, key = { _, t -> t.id }) { idx, track ->
                                TrackItemRow(
                                    track = track,
                                    isActive = track.id == currentTrackId,
                                    isFavorite = true,
                                    selectionMode = selectionMode,
                                    isSelected = track.id in selectedIds,
                                    onClick = {
                                        if (selectionMode) toggleSelection(track.id)
                                        else onPlayTracks(favTracks, idx)
                                    },
                                    onLongClick = { toggleSelection(track.id) },
                                    onFavoriteClick = { onToggleFavorite(track) },
                                    onMoreClick = { onOpenTrackOptions(track) }
                                )
                            }
                        }
                    }
                    LibraryTab.ONLINE -> Unit
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        val chosen = localTracks.filter { it.id in selectedIds }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${chosen.size} song(s)?") },
            text = { Text("Selected audio files will be permanently deleted from your device.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSelectedTracks(chosen)
                        selectedIds = emptySet()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyLibraryCard(message: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
