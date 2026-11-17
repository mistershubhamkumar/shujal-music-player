package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class LrcLibResponse(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "plainLyrics") val plainLyrics: String? = null,
    @Json(name = "syncedLyrics") val syncedLyrics: String? = null
)

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null
    ): LrcLibResponse

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrcLibResponse>
}

@JsonClass(generateAdapter = true)
data class JamendoResponse(
    @Json(name = "results") val results: List<JamendoTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "artist_name") val artistName: String = "",
    @Json(name = "album_name") val albumName: String? = null,
    @Json(name = "duration") val durationSec: Long = 0L,
    @Json(name = "audio") val audioUrl: String = "",
    @Json(name = "image") val imageUrl: String? = null
)

interface JamendoApi {
    @GET("v3.0/tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 25,
        @Query("search") query: String? = null,
        @Query("order") order: String = "popularity_total"
    ): JamendoResponse
}

@JsonClass(generateAdapter = true)
data class AudiusHostsResponse(
    @Json(name = "data") val data: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusSearchResponse(
    @Json(name = "data") val data: List<AudiusTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusUserDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "handle") val handle: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusTrackDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "duration") val durationSec: Double? = null,
    @Json(name = "artwork") val artwork: Map<String, String>? = null,
    @Json(name = "user") val user: AudiusUserDto? = null,
    @Json(name = "is_streamable") val isStreamable: Boolean? = null
)

interface AudiusBootstrapApi {
    @GET("/")
    suspend fun getDiscoveryHosts(): AudiusHostsResponse
}

interface AudiusApi {
    @GET("v1/tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("limit") limit: Int = 25,
        @Query("app_name") appName: String = "ShujalMusicPlayer"
    ): AudiusSearchResponse

    @GET("v1/tracks/trending")
    suspend fun getTrendingTracks(
        @Query("time") time: String = "week",
        @Query("limit") limit: Int = 25,
        @Query("app_name") appName: String = "ShujalMusicPlayer"
    ): AudiusSearchResponse
}

interface HearThisApi {
    @GET("search/")
    suspend fun searchTracks(
        @Query("t") query: String,
        @Query("count") count: Int = 20,
        @Query("page") page: Int = 1
    ): ResponseBody

    @GET("feed/")
    suspend fun getFeed(
        @Query("type") type: String = "popular",
        @Query("count") count: Int = 20,
        @Query("page") page: Int = 1
    ): ResponseBody
}

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "response") val response: ArchiveSearchBody? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveSearchBody(
    @Json(name = "docs") val docs: List<ArchiveDocDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDocDto(
    @Json(name = "identifier") val identifier: String? = null,
    @Json(name = "title") val title: Any? = null,
    @Json(name = "creator") val creator: Any? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFilesResponse(
    @Json(name = "result") val result: List<ArchiveFileDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveFileDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "length") val length: String? = null,
    @Json(name = "title") val title: String? = null
)

interface ArchiveOrgApi {
    @GET("advancedsearch.php")
    suspend fun searchAudio(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "creator"),
        @Query("rows") rows: Int = 10,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}/files")
    suspend fun getItemFiles(
        @Path("identifier") identifier: String
    ): ArchiveFilesResponse
}
