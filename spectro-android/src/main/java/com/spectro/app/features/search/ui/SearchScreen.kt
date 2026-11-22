package com.spectro.app.features.search.ui

// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import com.spectro.app.core.navigation.LocalNavBarInset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.spectro.app.core.playback.PlayerConnection
// Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.spectro.app.features.home.data.HomeItem
import com.spectro.app.features.home.data.HomeItemKind
import com.spectro.app.features.home.ui.rememberItemPlayer
import com.spectro.app.features.profile.ui.LocalProfile
import com.spectro.app.features.search.ui.components.SearchBar
import com.spectro.app.features.search.ui.components.SearchHistory
import com.spectro.app.features.search.ui.components.SearchResults
import com.spectro.app.features.search.ui.components.SearchSuggestionList
import com.spectro.app.features.settings.data.PrivacyPrefs

// Explore tab, the field on top and under it the history, the words that complete what is typed or the results
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    // Coming back from another tab starts from the empty search, before the first frame so the old results never show
    remember { viewModel.resetIfLeft() }
    val state by viewModel.state.collectAsState()

    // The searches of the account are taken out one by one while signed in, so there is no button to clear them all
    val isSynced = LocalProfile.current.isSignedIn && !PrivacyPrefs.pauseSearchHistory
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    // Songs and episodes play, the other cards do nothing until they have pages
    val play = rememberItemPlayer()

    // Searches the words and takes the keyboard away so the results fill the screen
    val search: (String) -> Unit = { text ->
        viewModel.submit(text)
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val onItemClick: (HomeItem) -> Unit = { item ->
        keyboardController?.hide()
        focusManager.clearFocus()
        play(item)
    }

    // Dismiss keyboard immediately as soon as music is played
    val currentTrack by PlayerConnection.track.collectAsState()
    LaunchedEffect(currentTrack) {
        if (currentTrack != null) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    // The history is asked again whenever the tab opens, so what changed in other places shows up
    LaunchedEffect(Unit) { viewModel.refreshAccountHistory() }

    val isActive = isFocused || state.query.isNotEmpty() || state.submitted != null

    // Back first leaves the results and the words, and only then leaves the tab
    BackHandler(enabled = isActive) {
        viewModel.back()
        focusManager.clearFocus()
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = { search(state.query) },
            // A tab has no arrow, the back gesture leaves the results and the words
            onBack = null,
            onFocusChange = { focus ->
                isFocused = focus.isFocused
                if (focus.isFocused && state.query.isEmpty()) viewModel.refreshAccountHistory()
                if (focus.isFocused && state.submitted != null) viewModel.openSuggestions()
            }
        )

        // The keyboard makes room for itself, less the bar of the tabs, which is under it already and would be counted twice
        Box(modifier = Modifier.weight(1f).windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets(bottom = LocalNavBarInset.current)))) {
            when {
                state.submitted != null -> SearchResults(
                    state = state,
                    onCategoryClick = viewModel::selectCategory,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                    onItemClick = onItemClick
                )

                state.query.isNotBlank() -> SearchSuggestionList(
                    suggestions = state.suggestions,
                    isLoading = state.isSuggesting,
                    onSearch = search,
                    onInsert = viewModel::onQueryChange,
                    onItemClick = onItemClick
                )

                // Pulled down, the history and the suggestions are asked again
                else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
                    SearchHistory(
                        history = state.history,
                        suggested = state.suggested,
                        onSearch = search,
                        onInsert = viewModel::onQueryChange,
                        onRemove = viewModel::removeFromHistory,
                        onClear = viewModel::clearHistory.takeUnless { isSynced }
                    )
                }
            }
        }
    }
}
