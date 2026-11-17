package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.local.SearchHistoryEntity
import com.example.model.Track
import com.example.ui.AsyncLoadState
import com.example.ui.components.AdMobBanner
import com.example.ui.components.GlassCard

enum class ExploreTab(val label: String) {
    SEARCH("Online Songs"),
    PLAYLISTS("Hindi & Mixes"),
    FAVOURITES("Saved Online")
}

private val HINDI_QUICK_SUGGESTIONS = listOf(
    "Hindi Hits" to "hindi bollywood hits",
    "Arijit Singh" to "arijit singh",
    "Bollywood Lofi" to "bollywood lofi",
    "Romantic Hindi" to "hindi romantic",
    "Punjabi Beats" to "punjabi hindi",
    "90s Hindi" to "90s hindi"
)

@Composable
fun ExploreOnlineScreen(
    onlineSearchState: AsyncLoadState<List<Track>>,
    trendingState: AsyncLoadState<List<Track>>,
    onlineFavorites: List<Track>,
    onlinePlayHistory: List<Track>,
    onlinePlaylists: List<Pair<String, String>>,
    searchHistory: List<SearchHistoryEntity>,
    currentTrackId: String?,
    onInitLoad: () -> Unit,
    onSearchOnline: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onInitLoad()
    }
    var selectedTab by remember { mutableStateOf(ExploreTab.SEARCH) }
    var query by remember { mutableStateOf("") }
    val favIds = remember(onlineFavorites) { onlineFavorites.map { it.id }.toSet() }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Online Songs", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search Hindi songs, artists, full tracks...") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    TextButton(onClick = {
                        selectedTab = ExploreTab.SEARCH
                        onSearchOnline(query)
                    }) { Text("Go") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    selectedTab = ExploreTab.SEARCH
                    onSearchOnline(query)
                }),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("online_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExploreTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("explore_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "online_ad_banner") {
                AdMobBanner()
            }
            when (selectedTab) {
                ExploreTab.SEARCH -> {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(HINDI_QUICK_SUGGESTIONS) { (label, searchKeyword) ->
                                AssistChip(
                                    onClick = {
                                        query = searchKeyword
                                        onSearchOnline(searchKeyword)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recently Searched", style = MaterialTheme.typography.labelLarge)
                                TextButton(onClick = onClearSearchHistory) { Text("Clear") }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchHistory) { item ->
                                    AssistChip(
                                        onClick = {
                                            query = item.query
                                            onSearchOnline(item.query)
                                        },
                                        label = { Text(item.query) }
                                    )
                                }
                            }
                        }
                    }

                    if (onlinePlayHistory.isNotEmpty() && onlineSearchState is AsyncLoadState.Idle) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Previously Played Online", style = MaterialTheme.typography.labelLarge)
                                TextButton(onClick = { onPlayTracks(onlinePlayHistory, 0) }) { Text("Play All") }
                            }
                        }
                        itemsIndexed(onlinePlayHistory.take(5), key = { _, t -> "hist_${t.id}" }) { idx, track ->
                            TrackItemRow(
                                track = track,
                                isActive = track.id == currentTrackId,
                                isFavorite = track.id in favIds,
                                onClick = { onPlayTracks(onlinePlayHistory, idx) },
                                onFavoriteClick = { onToggleFavorite(track) },
                                onMoreClick = { onOpenTrackOptions(track) }
                            )
                        }
                        item {
                            Text(
                                "Trending Hindi & Full Songs",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    val activeResult = if (onlineSearchState is AsyncLoadState.Idle) trendingState else onlineSearchState
                    when (activeResult) {
                        is AsyncLoadState.Loading -> item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AsyncLoadState.Error -> item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Text("No matching stream found", style = MaterialTheme.typography.titleMedium)
                                Text(activeResult.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onSearchOnline(query) }) {
                                    Icon(Icons.Rounded.Refresh, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                        is AsyncLoadState.Success -> {
                            itemsIndexed(activeResult.data, key = { _, t -> t.id }) { idx, track ->
                                TrackItemRow(
                                    track = track,
                                    isActive = track.id == currentTrackId,
                                    isFavorite = track.id in favIds,
                                    onClick = { onPlayTracks(activeResult.data, idx) },
                                    onFavoriteClick = { onToggleFavorite(track) },
                                    onMoreClick = { onOpenTrackOptions(track) }
                                )
                            }
                        }
                        AsyncLoadState.Idle -> Unit
                    }
                }

                ExploreTab.PLAYLISTS -> {
                    items(onlinePlaylists) { (title, searchTerm) ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                query = searchTerm
                                selectedTab = ExploreTab.SEARCH
                                onSearchOnline(searchTerm)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Tap to load matching & related full songs",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.PlayArrow, null)
                            }
                        }
                    }
                }

                ExploreTab.FAVOURITES -> {
                    if (onlineFavorites.isEmpty()) {
                        item {
                            GlassCard(Modifier.fillMaxWidth()) {
                                Text("No saved online tracks yet. Tap the heart on any online track to save it here.")
                            }
                        }
                    } else {
                        itemsIndexed(onlineFavorites, key = { _, t -> t.id }) { idx, track ->
                            TrackItemRow(
                                track = track,
                                isActive = track.id == currentTrackId,
                                isFavorite = true,
                                onClick = { onPlayTracks(onlineFavorites, idx) },
                                onFavoriteClick = { onToggleFavorite(track) },
                                onMoreClick = { onOpenTrackOptions(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}
