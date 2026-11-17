package com.example.data.network

import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OnlineVideoRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept-Language", "en-IN,en;q=0.9,hi;q=0.8")
                .build()
            chain.proceed(req)
        }
        .build()

    private val pipedMirrors = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee"
    )

    private val invidiousMirrors = listOf(
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.nerdvpn.de"
    )

    private fun parseDurationTextToMs(text: String): Long {
        val parts = text.trim().split(":").mapNotNull { it.toLongOrNull() }
        val seconds = when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0L
        }
        return (seconds * 1000L).coerceAtLeast(0L)
    }

    private fun JSONObject.extractRunsText(key: String): String {
        val obj = optJSONObject(key) ?: return ""
        obj.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = obj.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString().trim()
    }

    private fun collectVideoRenderers(node: Any?, out: MutableList<VideoItem>) {
        if (out.size >= 35 || node == null) return
        when (node) {
            is JSONObject -> {
                val vr = node.optJSONObject("videoRenderer")
                    ?: node.optJSONObject("videoWithContextRenderer")
                if (vr != null) {
                    val vid = vr.optString("videoId").trim()
                    if (vid.length == 11) {
                        val title = vr.extractRunsText("title")
                            .ifBlank { vr.extractRunsText("headline") }
                        val channel = vr.extractRunsText("ownerText")
                            .ifBlank { vr.extractRunsText("shortBylineText") }
                            .ifBlank { vr.extractRunsText("longBylineText") }
                            .ifBlank { "YouTube India" }
                        val durationStr = vr.extractRunsText("lengthText")
                        val viewsStr = vr.extractRunsText("shortViewCountText")
                            .ifBlank { vr.extractRunsText("viewCountText") }

                        if (title.isNotBlank()) {
                            val sub = buildString {
                                append(channel)
                                if (viewsStr.isNotBlank()) {
                                    append(" • ")
                                    append(viewsStr)
                                }
                            }
                            out += VideoItem(
                                id = "yt_$vid",
                                title = title,
                                subtitle = sub,
                                uri = "https://www.youtube.com/watch?v=$vid",
                                thumbnailUri = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                                durationMs = parseDurationTextToMs(durationStr),
                                width = 1280,
                                height = 720,
                                mimeType = "video/mp4",
                                folderPath = "YouTube India",
                                isOnline = true,
                                description = "YouTube India Video",
                                youtubeId = vid
                            )
                        }
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k != "videoRenderer" && k != "videoWithContextRenderer") {
                        collectVideoRenderers(node.opt(k), out)
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    collectVideoRenderers(node.opt(i), out)
                }
            }
        }
    }

    private fun searchYouTubeInnertube(query: String): List<VideoItem> {
        return runCatching {
            val payload = JSONObject().apply {
                put("query", query)
                put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject()
                            .put("clientName", "WEB")
                            .put("clientVersion", "2.20241126.01.00")
                            .put("hl", "en")
                            .put("gl", "IN")
                    )
                )
            }
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .post(body)
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return emptyList()
                val list = mutableListOf<VideoItem>()
                collectVideoRenderers(JSONObject(raw), list)
                list.distinctBy { it.youtubeId }
            }
        }.getOrElse { emptyList() }
    }

    private fun searchYouTubeHtmlFallback(query: String): List<VideoItem> {
        return runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://www.youtube.com/results?search_query=$encoded&gl=IN&hl=en")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val html = resp.body?.string().orEmpty()
                val marker = "var ytInitialData = "
                val startIdx = html.indexOf(marker)
                if (startIdx < 0) return emptyList()
                val jsonStart = startIdx + marker.length
                val endIdx = html.indexOf(";</script>", jsonStart).takeIf { it > jsonStart }
                    ?: return emptyList()
                val jsonStr = html.substring(jsonStart, endIdx)
                val list = mutableListOf<VideoItem>()
                collectVideoRenderers(JSONObject(jsonStr), list)
                list.distinctBy { it.youtubeId }
            }
        }.getOrElse { emptyList() }
    }

    private fun searchPipedFallback(query: String): List<VideoItem> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        for (mirror in pipedMirrors) {
            val res = runCatching {
                val req = Request.Builder()
                    .url("$mirror/search?q=$encoded&filter=videos")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList<VideoItem>()
                    val raw = resp.body?.string().orEmpty()
                    val arr = JSONObject(raw).optJSONArray("items") ?: return@use emptyList()
                    buildList {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val urlPath = obj.optString("url")
                            val vid = urlPath.substringAfter("v=", "").take(11)
                            if (vid.length != 11) continue
                            val title = obj.optString("title").trim()
                            if (title.isBlank()) continue
                            val uploader = obj.optString("uploaderName").ifBlank { "YouTube India" }
                            val durSec = obj.optLong("duration", 0L).coerceAtLeast(0L)
                            add(
                                VideoItem(
                                    id = "yt_$vid",
                                    title = title,
                                    subtitle = uploader,
                                    uri = "https://www.youtube.com/watch?v=$vid",
                                    thumbnailUri = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                                    durationMs = durSec * 1000L,
                                    width = 1280,
                                    height = 720,
                                    mimeType = "video/mp4",
                                    folderPath = "YouTube India",
                                    isOnline = true,
                                    youtubeId = vid
                                )
                            )
                        }
                    }
                }
            }.getOrNull().orEmpty()
            if (res.isNotEmpty()) return res
        }
        return emptyList()
    }

    suspend fun searchOnlineVideos(query: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            val effectiveQuery = if (trimmed.isBlank()) {
                "trending hindi song video india"
            } else {
                trimmed
            }

            var results = searchYouTubeInnertube(effectiveQuery)
            if (results.isEmpty()) {
                results = searchYouTubeHtmlFallback(effectiveQuery)
            }
            if (results.isEmpty()) {
                results = searchPipedFallback(effectiveQuery)
            }

            if (results.isEmpty()) {
                error("Could not load YouTube India videos for \"$effectiveQuery\". Check internet connection and retry.")
            }
            results.take(30)
        }
    }

    suspend fun fetchTrendingVideos(): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val queries = listOf(
                "trending hindi video songs india 2025",
                "new bollywood video song india"
            )
            val merged = mutableListOf<VideoItem>()
            for (q in queries) {
                val res = searchYouTubeInnertube(q).ifEmpty { searchYouTubeHtmlFallback(q) }
                merged += res
                if (merged.size >= 24) break
            }
            if (merged.isEmpty()) {
                merged += searchPipedFallback("hindi song video")
            }
            val unique = merged.distinctBy { it.youtubeId ?: it.id }.take(30)
            if (unique.isEmpty()) {
                error("Unable to load trending Indian YouTube videos. Check internet connection.")
            }
            unique
        }
    }

    suspend fun resolveDirectPlayableUrl(youtubeId: String): String? = withContext(Dispatchers.IO) {
        if (youtubeId.isBlank()) return@withContext null
        // 1. Try Piped HLS / muxed MP4 stream
        for (mirror in pipedMirrors) {
            val url = runCatching {
                val req = Request.Builder().url("$mirror/streams/$youtubeId").build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val hls = json.optString("hls").takeIf { it.startsWith("http") }
                    if (hls != null) return@use hls
                    val streams = json.optJSONArray("videoStreams") ?: return@use null
                    for (i in 0 until streams.length()) {
                        val s = streams.optJSONObject(i) ?: continue
                        val videoOnly = s.optBoolean("videoOnly", true)
                        val streamUrl = s.optString("url")
                        if (!videoOnly && streamUrl.startsWith("http")) {
                            return@use streamUrl
                        }
                    }
                    null
                }
            }.getOrNull()
            if (!url.isNullOrBlank()) return@withContext url
        }

        // 2. Try Invidious formatStreams (muxed MP4)
        for (mirror in invidiousMirrors) {
            val url = runCatching {
                val req = Request.Builder().url("$mirror/api/v1/videos/$youtubeId").build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val hls = json.optString("hlsUrl").takeIf { it.startsWith("http") }
                    if (hls != null) return@use hls
                    val formats = json.optJSONArray("formatStreams") ?: return@use null
                    for (i in 0 until formats.length()) {
                        val f = formats.optJSONObject(i) ?: continue
                        val streamUrl = f.optString("url")
                        if (streamUrl.startsWith("http")) return@use streamUrl
                    }
                    null
                }
            }.getOrNull()
            if (!url.isNullOrBlank()) return@withContext url
        }
        null
    }

    fun buildDirectUrlVideo(rawUrl: String, customTitle: String = ""): VideoItem {
        val cleanUrl = rawUrl.trim()
        val ytMatch = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{11})")
            .find(cleanUrl)
        val ytId = ytMatch?.groupValues?.getOrNull(1)
        if (ytId != null) {
            val title = customTitle.trim().ifBlank { "YouTube Video ($ytId)" }
            return VideoItem(
                id = "yt_$ytId",
                title = title,
                subtitle = "YouTube Link",
                uri = "https://www.youtube.com/watch?v=$ytId",
                thumbnailUri = "https://i.ytimg.com/vi/$ytId/hqdefault.jpg",
                mimeType = "video/mp4",
                folderPath = "YouTube India",
                isOnline = true,
                youtubeId = ytId
            )
        }

        val inferredTitle = customTitle.trim().ifBlank {
            cleanUrl.substringAfterLast('/').substringBefore('?').substringBeforeLast('.')
                .replace("%20", " ").replace("_", " ").ifBlank { "Online Stream Video" }
        }
        val mime = when {
            cleanUrl.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            cleanUrl.contains(".webm", ignoreCase = true) -> "video/webm"
            cleanUrl.contains(".mkv", ignoreCase = true) -> "video/x-matroska"
            else -> "video/mp4"
        }
        return VideoItem(
            id = "url_vid_${cleanUrl.hashCode()}",
            title = inferredTitle,
            subtitle = "Direct Stream URL",
            uri = cleanUrl,
            mimeType = mime,
            folderPath = "Direct URL",
            isOnline = true
        )
    }
}
