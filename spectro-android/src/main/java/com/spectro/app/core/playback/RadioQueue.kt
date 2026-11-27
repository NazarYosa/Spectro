package com.spectro.app.core.playback

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Collections
import java.util.concurrent.ConcurrentHashMap
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONObject
// Project resources
import com.spectro.app.core.innertube.InnerTubeClient
import com.spectro.app.features.home.data.ArtistJoiners
import com.spectro.app.features.home.data.HomeParser
import com.spectro.app.features.home.data.VideoTypes
import com.spectro.app.features.settings.data.ContentPrefs
import com.spectro.app.core.innertube.MusicOrigin
import com.spectro.app.core.innertube.VisitorStore
import com.spectro.app.core.innertube.YouTubeSession
import com.spectro.app.core.innertube.arrayAt
import com.spectro.app.core.innertube.deviceLocale
import com.spectro.app.core.innertube.objectAt
import com.spectro.app.core.innertube.objects
import com.spectro.app.core.innertube.stringAt
import com.spectro.app.features.auth.data.Entry
import com.spectro.app.features.auth.data.EntryStore

// Prefix of the endless radio YouTube Music builds from a song
internal const val RadioPrefix = "RDAMVM"

// Units of a duration such as 3:45 or 1:02:30
private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3600L
private const val MillisPerSecond = 1000L

// The mark that ends the artists in the line under a song
private const val BylineDivider = "\u2022"

// How many more times a radio is asked when it comes back empty or fails, and how long it waits before each one
private const val RadioRetries = 2
private const val RadioRetryMillis = 600L

// Prefix of the id of the page of an album
private const val AlbumPagePrefix = "MPRE"

// The kind of a video that someone uploaded, not an artist, its line names the channel
private const val UploadType = "MUSIC_VIDEO_TYPE_UGC"

// Pages a song leads to, the ids of its artists and of its album when it has one, with the name of the album
class TrackLinks(
    // The artists in the order the song shows them, each with its name and its id, empty when the artist has no page
    val artists: List<Pair<String, String>>,
    val albumId: String?,
    val albumName: String? = null,
    // The name of the song as YouTube Music shows it
    val title: String? = null,
    // True for a video someone uploaded, whose artists are the channel and not the ones of the song
    val isUpload: Boolean = false
) {}

// One page of a radio, with what is needed to ask the next one
class RadioPage(
    val tracks: List<PlayableTrack>,
    val playlistId: String,
    val continuation: String?
)

private fun cleanTitle(title: String): String =
    title.lowercase()
        .replace(Regex("""\s*[\(\[](feat\.|ft\.|official|video|audio|lyrics|hd|remix).*?[\)\]]""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""[^a-z0-9]"""), "")
        .trim()

private val ExcludedKeywords = listOf(
    "cover", "karaoke", "instrumental", "acoustic", "slowed", "reverb",
    "sped up", "speed up", "tribute", "parody", "remake", "guitar cover",
    "piano cover", "drum cover", "vocal cover", "ai cover", "fancam",
    "reaction", "live at", "live from", "live in", "unreleased", "snippet",
    "teaser", "full album", "1 hour", "1hour", "loop", "bass boosted",
    "nightcore", "8d audio", "tiktok version", "live concert", "live performance",
    "mashup", "aesthetic", "edit", "tutorial", "fingerstyle", "short", "shorts",
    "synthesia", "chords", "backing track", "whatsapp status", "status video",
    "top nhạc", "kompilasi", "compilation", "kumpulan lagu"
)

private fun isDerivativeOrJunk(title: String, artist: String?, seedTitle: String?): Boolean {
    val lowerTitle = title.lowercase()
    val lowerArtist = (artist ?: "").lowercase()
    if (ExcludedKeywords.any { lowerTitle.contains(it) || lowerArtist.contains(it) }) return true
    if (seedTitle?.lowercase()?.contains("live") != true) {
        if (lowerTitle.contains("(live") || lowerTitle.contains("[live") || lowerTitle.contains("live version") || lowerTitle.contains("live audio")) {
            return true
        }
    }
    if (seedTitle != null) {
        val cSeed = cleanTitle(seedTitle)
        val cTarget = cleanTitle(title)
        if (cSeed.isNotEmpty() && (cSeed == cTarget || cSeed.contains(cTarget) || cTarget.contains(cSeed))) return true
    }
    return false
}

private fun organizePopularQueue(validTracks: List<PlayableTrack>, seedArtist: String?): List<PlayableTrack> {
    if (validTracks.size <= 1) return validTracks

    // 1. YouTube Music RDAMVM ranking puts the highest-engagement, most viral global hits in the top 20-25 positions.
    // Limit seed artist to at most 2 appearances in the top hit pool to avoid mono-artist flood.
    val normalizedSeedArtist = (seedArtist ?: "").lowercase().trim()
    var seedArtistCount = 0

    val topHitsPool = mutableListOf<PlayableTrack>()
    val secondaryPool = mutableListOf<PlayableTrack>()

    for (i in validTracks.indices) {
        val track = validTracks[i]
        val artistLower = (track.artist ?: "").lowercase().trim()
        val isSeedArtist = normalizedSeedArtist.isNotEmpty() &&
            (artistLower.contains(normalizedSeedArtist) || normalizedSeedArtist.contains(artistLower))

        if (isSeedArtist) {
            if (seedArtistCount < 2 && i < 25) {
                seedArtistCount++
                topHitsPool.add(track)
            } else {
                secondaryPool.add(track)
            }
        } else {
            if (topHitsPool.size < 20) {
                topHitsPool.add(track)
            } else {
                secondaryPool.add(track)
            }
        }
    }

    // 2. Dynamically randomize/shuffle top hits pool so every single play produces a completely fresh mix
    val shuffledTopPool = topHitsPool.shuffled()
    val shuffledSecondary = secondaryPool.shuffled()

    // 3. Interleave artists to guarantee variety (no 2 consecutive tracks from the same artist)
    fun interleave(tracks: List<PlayableTrack>, initialArtist: String?): List<PlayableTrack> {
        val result = mutableListOf<PlayableTrack>()
        val pool = tracks.toMutableList()
        var lastArtist = (initialArtist ?: "").lowercase().trim()

        while (pool.isNotEmpty()) {
            var pickIdx = pool.indexOfFirst { t ->
                val a = (t.artist ?: "").lowercase().trim()
                a.isNotEmpty() && !lastArtist.contains(a) && !a.contains(lastArtist)
            }
            if (pickIdx == -1) pickIdx = 0
            val chosen = pool.removeAt(pickIdx)
            result.add(chosen)
            lastArtist = (chosen.artist ?: "").lowercase().trim()
        }
        return result
    }

    val primaryCurated = interleave(shuffledTopPool, seedArtist)
    val lastArtist = primaryCurated.lastOrNull()?.artist ?: seedArtist
    val secondaryCurated = interleave(shuffledSecondary, lastArtist)

    val combined = mutableListOf<PlayableTrack>()
    combined.addAll(primaryCurated)
    combined.addAll(secondaryCurated)
    return combined
}

// The radio of a song, the queue YouTube Music plays after it, adapted from Metrolist (GPL-3.0)
internal object RadioQueue {
    // First page of the radio of a song, which starts with the song itself
    // YouTube sometimes answers with a radio that has only the song itself, or fails, so it is asked again a few times, as Metrolist (GPL-3.0) does
    suspend fun start(context: Context, videoId: String): Result<RadioPage> {
        var result = page(context, videoId, RadioPrefix + videoId, continuation = null)
        for (attempt in 0 until RadioRetries) {
            if (result.getOrNull()?.let { it.tracks.size > 1 } == true) break
            delay(RadioRetryMillis)
            result = page(context, videoId, RadioPrefix + videoId, continuation = null)
        }
        return result.map { originalPage ->
            val allTracks = originalPage.tracks
            val currentTrack = allTracks.firstOrNull { it.id == videoId } ?: allTracks.firstOrNull()
            val seedTitle = currentTrack?.title

            val seenKeys = mutableSetOf<String>()
            val seenIds = mutableSetOf<String>()
            if (currentTrack != null) {
                seenIds.add(currentTrack.id)
                val k = cleanTitle(currentTrack.title)
                if (k.isNotEmpty()) seenKeys.add(k)
            }

            // 1-Genre & Popular Hits Filter (Sama seperti di APK v1.0.0):
            // Saring konten sampah, cover, karaoke, slowed, reverb, dan duplikat
            val validRemaining = allTracks.filter { it.id != currentTrack?.id }
                .filter { track ->
                    if (track.id in seenIds) return@filter false
                    val key = cleanTitle(track.title)
                    if (key.isNotEmpty() && key in seenKeys) return@filter false
                    if (isDerivativeOrJunk(track.title, track.artist, seedTitle)) return@filter false
                    seenIds.add(track.id)
                    if (key.isNotEmpty()) seenKeys.add(key)
                    true
                }

            // Pengacakan Dinamis Populer Bertingkat (Tiered Popularity Dynamic Shuffle):
            // Mengelompokkan lagu ke dalam tier popularitas teratas (superhits) dan mengacak dinamis
            // dengan interleaving artis agar variasi selalu segar dan lagu berikutnya dijamin populer
            val organizedRemaining = organizePopularQueue(validRemaining, currentTrack?.artist)

            val curatedQueue = mutableListOf<PlayableTrack>()
            if (currentTrack != null) {
                curatedQueue.add(currentTrack)
            }
            curatedQueue.addAll(organizedRemaining)

            RadioPage(
                tracks = curatedQueue,
                playlistId = originalPage.playlistId,
                continuation = originalPage.continuation
            )
        }
    }

    // The page after the last one asked
    suspend fun more(context: Context, videoId: String, playlistId: String, continuation: String): Result<RadioPage> {
        var result = page(context, videoId, playlistId, continuation)
        for (attempt in 0 until RadioRetries) {
            if (result.isSuccess) break
            delay(RadioRetryMillis)
            result = page(context, videoId, playlistId, continuation)
        }
        return result.map { nextPage ->
            val seenKeys = mutableSetOf<String>()
            val filtered = nextPage.tracks.filter { track ->
                val key = cleanTitle(track.title)
                if (key.isNotEmpty() && key in seenKeys) return@filter false
                if (isDerivativeOrJunk(track.title, track.artist, null)) return@filter false
                if (key.isNotEmpty()) seenKeys.add(key)
                true
            }
            val organized = organizePopularQueue(filtered, null)
            RadioPage(
                tracks = organized,
                playlistId = nextPage.playlistId,
                continuation = nextPage.continuation
            )
        }
    }

    // Pages already worked out, so a second tap on the same song does not ask again
    private val linksCache = ConcurrentHashMap<String, TrackLinks>()

    // The pages of a song when they are already known, so a tap can open them at once
    fun cachedLinks(videoId: String): TrackLinks? = linksCache[videoId]

    // The artists and the album of a song, read from its own entry at the start of its radio
    suspend fun links(context: Context, videoId: String): Result<TrackLinks> {
        linksCache[videoId]?.let { return Result.success(it) }

        return InnerTubeClient.next(currentSession(context), videoId, RadioPrefix + videoId).mapCatching { response ->
            val renderer = response.objectAt(
                "contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer",
                "watchNextTabbedResultsRenderer", "tabs", 0, "tabRenderer", "content",
                "musicQueueRenderer", "content", "playlistPanelRenderer"
            )?.arrayAt("contents").objects()
                ?.mapNotNull { it.optJSONObject("playlistPanelVideoRenderer") ?: it.objectAt("playlistPanelVideoWrapperRenderer", "primaryRenderer", "playlistPanelVideoRenderer") }
                ?.firstOrNull { it.stringAt("videoId") == videoId }
                ?: throw IllegalStateException("Song not in the answer")

            val runs = renderer.arrayAt("longBylineText", "runs").objects()
            val albumRun = runs.firstOrNull { run ->
                run.stringAt("navigationEndpoint", "browseEndpoint", "browseId")?.startsWith(AlbumPagePrefix) == true
            }
            val albumId = albumRun?.stringAt("navigationEndpoint", "browseEndpoint", "browseId")

            // The artists come before the album, the words that join them are not artists, and one without a page keeps an empty id
            val artists = runs
                .takeWhile { run ->
                    run.optString("text").trim() != BylineDivider &&
                        run.stringAt("navigationEndpoint", "browseEndpoint", "browseId")?.startsWith(AlbumPagePrefix) != true
                }
                .mapNotNull { run ->
                    val text = run.optString("text")
                    val id = run.stringAt("navigationEndpoint", "browseEndpoint", "browseId")
                    if (id == null && text.trim().lowercase() in ArtistJoiners) null else text.trim() to id.orEmpty()
                }
                .filter { it.first.isNotEmpty() }
                .distinctBy { it.first }

            TrackLinks(
                artists = artists,
                albumId = albumId,
                albumName = albumRun?.stringAt("text")?.trim(),
                title = renderer.stringAt("title", "runs", 0, "text")?.trim(),
                isUpload = renderer.stringAt(
                    "navigationEndpoint", "watchEndpoint", "watchEndpointMusicSupportedConfigs",
                    "watchEndpointMusicConfig", "musicVideoType"
                ) == UploadType
            ).also { linksCache[videoId] = it }
        }
    }

    // Asks a page of the queue and reads its songs
    private suspend fun page(context: Context, videoId: String, playlistId: String, continuation: String?): Result<RadioPage> =
        InnerTubeClient.next(currentSession(context), videoId, playlistId, continuation).mapCatching { response ->
            val panel = response.objectAt("continuationContents", "playlistPanelContinuation")
                ?: response.objectAt(
                    "contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer",
                    "watchNextTabbedResultsRenderer", "tabs", 0, "tabRenderer", "content",
                    "musicQueueRenderer", "content", "playlistPanelRenderer"
                )
                ?: throw IllegalStateException("No queue in the answer")

            // A signed in answer wraps most rows with the other version of the same recording, so a video is swapped for its song when there is one
            val rows = panel.arrayAt("contents").objects().mapNotNull { row ->
                val wrapper = row.optJSONObject("playlistPanelVideoWrapperRenderer")
                val primary = (row.optJSONObject("playlistPanelVideoRenderer")
                    ?: wrapper?.objectAt("primaryRenderer", "playlistPanelVideoRenderer"))?.let(::trackOf) ?: return@mapNotNull null
                val song = wrapper?.arrayAt("counterpart").objects()
                    .mapNotNull { it.objectAt("counterpartRenderer", "playlistPanelVideoRenderer")?.let(::trackOf) }
                    .firstOrNull { !it.isVideo }
                if (primary.isVideo && primary.id != videoId && song != null) song else primary
            }.filter { !ContentPrefs.hideExplicit || !it.isExplicit || it.id == videoId }

            // The radio plays songs, the video the user chose stays, and a page with no song at all is kept as it came
            val songs = rows.filter { !it.isVideo || it.id == videoId }

            RadioPage(
                tracks = if (songs.any { it.id != videoId }) songs else rows,
                playlistId = panel.stringAt("playlistId") ?: playlistId,
                continuation = panel.arrayAt("continuations").objects().firstNotNullOfOrNull { next ->
                    next.stringAt("nextRadioContinuationData", "continuation")
                        ?: next.stringAt("nextContinuationData", "continuation")
                }
            )
        }

    // A song of the queue, with its largest picture
    private fun trackOf(renderer: JSONObject): PlayableTrack? {
        val id = renderer.stringAt("videoId") ?: return null
        val title = renderer.stringAt("title", "runs", 0, "text") ?: return null

        return PlayableTrack(
            id = id,
            title = title,
            artist = renderer.arrayAt("shortBylineText", "runs").objects()
                .joinToString("") { it.optString("text") }
                .ifBlank { null },
            artworkUrl = renderer.arrayAt("thumbnail", "thumbnails").objects().lastOrNull()?.stringAt("url"),
            durationMs = renderer.stringAt("lengthText", "runs", 0, "text")?.let(::parseTime) ?: 0L,
            isVideo = renderer.stringAt(
                "navigationEndpoint", "watchEndpoint", "watchEndpointMusicSupportedConfigs",
                "watchEndpointMusicConfig", "musicVideoType"
            ) in VideoTypes,
            isExplicit = HomeParser.hasExplicitBadge(renderer.arrayAt("badges"))
        )
    }

    // A duration such as 3:45 or 1:02:30 in milliseconds, empty when the text is not one
    private fun parseTime(text: String): Long? {
        val parts = text.trim().split(':').map { it.toLongOrNull() ?: return null }

        val seconds = when (parts.size) {
            2 -> parts[0] * SecondsPerMinute + parts[1]
            3 -> parts[0] * SecondsPerHour + parts[1] * SecondsPerMinute + parts[2]
            else -> return null
        }
        return seconds * MillisPerSecond
    }

    // The same identity the Home uses, with the account when the user signed in with Google
    private suspend fun currentSession(context: Context): YouTubeSession {
        val locale = deviceLocale()
        val cookies = if (EntryStore(context).entry.first() == Entry.Google) {
            withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
        } else {
            null
        }
        return YouTubeSession(cookies = cookies, visitorData = VisitorStore(context).get(locale), locale = locale)
    }
}
