package com.spectro.app.features.library.data

// JSON helpers of the YouTube Music answers
import com.spectro.app.core.innertube.arrayAt
import com.spectro.app.core.innertube.findObjects
import com.spectro.app.core.innertube.objectAt
import com.spectro.app.core.innertube.objects
import com.spectro.app.core.innertube.stringAt
// JSON
import org.json.JSONObject
// Project resources
import com.spectro.app.features.collection.data.CollectionParser
import com.spectro.app.features.home.data.ChannelPrefix
import com.spectro.app.features.home.data.HomeItem
import com.spectro.app.features.home.data.HomeItemKind
import com.spectro.app.features.home.data.HomeParser
import com.spectro.app.features.home.data.PageAlbum
import com.spectro.app.features.home.data.PageArtist
import com.spectro.app.features.home.data.PageLibraryArtist
import com.spectro.app.features.home.data.PagePlaylist
import com.spectro.app.features.home.data.PlaylistPagePrefix

// The icon of the button that saves a song to the library
private const val LibrarySaveIcon = "BOOKMARK_BORDER"

// Turns the lists of the library of YouTube Music into the same cards the rest of the app draws, the list may be a grid of covers or rows
object LibraryParser {
    // A list of the library, the first page or the next ones of it
    fun parse(response: JSONObject): LibraryPage {
        val next = response.objectAt("continuationContents")
        val container = next?.objectAt("musicPlaylistShelfContinuation")
            ?: next?.objectAt("musicShelfContinuation")
            ?: next?.objectAt("gridContinuation")
            ?: response.findObjects("musicPlaylistShelfRenderer").firstOrNull { (it.arrayAt("contents")?.length() ?: 0) > 0 }
            ?: (response.findObjects("gridRenderer") + response.findObjects("musicShelfRenderer"))
                .firstOrNull { (it.arrayAt("contents")?.length() ?: 0) > 0 || (it.arrayAt("items")?.length() ?: 0) > 0 }
            ?: response.findObjects("musicPlaylistShelfRenderer").firstOrNull()
            ?: response.findObjects("gridRenderer").firstOrNull()
            ?: response.findObjects("musicShelfRenderer").firstOrNull()

        val rawEntries = (container?.arrayAt("contents").objects() + container?.arrayAt("items").objects())
            .ifEmpty {
                response.arrayAt("onResponseReceivedActions").objects().firstNotNullOfOrNull { action ->
                    action.arrayAt("reloadContinuationItemsCommand", "continuationItems")
                        ?: action.arrayAt("appendContinuationItemsAction", "continuationItems")
                }.objects()
            }
            .ifEmpty {
                response.findObjects("musicTwoRowItemRenderer").map { JSONObject().put("musicTwoRowItemRenderer", it) } +
                    response.findObjects("musicResponsiveListItemRenderer").map { JSONObject().put("musicResponsiveListItemRenderer", it) }
            }

        val continuation = container?.arrayAt("continuations").objects()
            .firstNotNullOfOrNull {
                it.stringAt("nextContinuationData", "continuation")
                    ?: it.stringAt("nextRadioContinuationData", "continuation")
            }
            ?: rawEntries.lastOrNull()?.stringAt("continuationItemRenderer", "continuationEndpoint", "continuationCommand", "token")
            ?: rawEntries.lastOrNull()?.stringAt("continuationItemRenderer", "continuationEndpoint", "continuationCommand", "continuation")
            ?: response.findObjects("nextContinuationData").firstOrNull()?.stringAt("continuation")

        val parsedItems = rawEntries.mapNotNull(::itemOf).ifEmpty {
            response.findObjects("musicTwoRowItemRenderer")
                .map { JSONObject().put("musicTwoRowItemRenderer", it) }
                .mapNotNull(::itemOf)
        }

        return LibraryPage(items = parsedItems, continuation = continuation, sortOptions = sortOptionsOf(response))
    }

    // The orders of the button above the list, only when more than one of them can be asked
    internal fun sortOptionsOf(response: JSONObject): List<LibrarySortOption> {
        val options = response.findObjects("musicMultiSelectMenuItemRenderer").mapNotNull { option ->
            val title = option.arrayAt("title", "runs").objects().joinToString("") { it.optString("text") }.trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val token = option.findObjects("reloadContinuationData").firstOrNull()?.stringAt("continuation")

            LibrarySortOption(title = title, token = token, isSelected = option.optBoolean("isSelected"))
        }

        return options.takeIf { list -> list.size > 1 && list.any { it.token != null } }.orEmpty()
    }

    // One entry of the list, a cover with two lines or a row
    private fun itemOf(entry: JSONObject): HomeItem? {
        entry.objectAt("musicTwoRowItemRenderer")?.let { row ->
            val item = HomeParser.parseTwoRow(row) ?: return null
            // The line under the cover is kept as YouTube Music writes it, with the kind of the item when the list says it
            return item.takeIf { it.kind != HomeItemKind.Playlist }
                ?: item.copy(countText = row.arrayAt("subtitle", "runs").objects().lastOrNull()?.optString("text")?.takeIf { it.isNotBlank() })
        }

        val row = entry.objectAt("musicResponsiveListItemRenderer") ?: return null
        val browse = row.objectAt("navigationEndpoint", "browseEndpoint")
            ?: row.arrayAt("flexColumns").objects().firstOrNull()?.objectAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "navigationEndpoint", "browseEndpoint")

        val browseId = browse?.stringAt("browseId")
        val pageType = browse?.let(HomeParser::pageTypeOf)

        // A row that opens the page of an artist
        if (pageType == PageArtist || pageType == PageLibraryArtist || (pageType == null && browseId?.startsWith(ChannelPrefix) == true)) {
            return HomeItem(
                kind = HomeItemKind.Artist,
                id = browseId ?: return null,
                title = row.stringAt("flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text") ?: return null,
                thumbnailUrl = HomeParser.coverOf(row.objectAt("thumbnail")),
                countText = secondLineOf(row),
                lineText = secondLineOf(row)
            )
        }

        // A row that is a playlist
        if (pageType == PagePlaylist || (browseId != null && (browseId.startsWith("VL") || browseId.startsWith("PL") || browseId.startsWith("RDCLAK")))) {
            return HomeItem(
                kind = HomeItemKind.Playlist,
                id = (browseId ?: return null).removePrefix(PlaylistPagePrefix),
                title = row.stringAt("flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text") ?: "Playlist",
                thumbnailUrl = HomeParser.coverOf(row.objectAt("thumbnail")),
                countText = secondLineOf(row),
                lineText = secondLineOf(row)
            )
        }

        // A row that is an album
        if (pageType == PageAlbum || (browseId != null && browseId.startsWith("MPRE"))) {
            return HomeItem(
                kind = HomeItemKind.Album,
                id = browseId ?: return null,
                title = row.stringAt("flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text") ?: "Album",
                thumbnailUrl = HomeParser.coverOf(row.objectAt("thumbnail")),
                countText = secondLineOf(row),
                lineText = secondLineOf(row)
            )
        }

        // Any other row is a song
        return CollectionParser.tracksOf(listOf(entry), null, emptyList()).firstOrNull()?.copy(lineText = secondLineOf(row), libraryRemoveToken = libraryRemoveTokenOf(row))
    }

    // What takes the song out of the library, the button of the menu that saves it and that says remove while it is saved
    private fun libraryRemoveTokenOf(row: JSONObject): String? =
        row.arrayAt("menu", "menuRenderer", "items").objects()
            .mapNotNull { it.objectAt("toggleMenuServiceItemRenderer") }
            .firstOrNull {
                val icon = it.stringAt("defaultIcon", "iconType")
                (icon == LibrarySaveIcon || icon == "LIKE" || icon == "FAVORITE") && it.optBoolean("isToggled")
            }
            ?.stringAt("toggledServiceEndpoint", "feedbackEndpoint", "feedbackToken")

    // The line under the title of a row, as it comes
    private fun secondLineOf(row: JSONObject): String? =
        row.arrayAt("flexColumns").objects().getOrNull(1)
            ?.arrayAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs").objects()
            .joinToString("") { it.optString("text") }.trim().takeIf { it.isNotEmpty() }
}
