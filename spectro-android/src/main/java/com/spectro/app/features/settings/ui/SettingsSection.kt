package com.spectro.app.features.settings.ui

// Android resources and icons
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.spectro.app.R
import com.spectro.app.core.designsystem.icons.SpectroIcons

// A part of the settings, with the words that name it, the line that says what is in it and its icon
enum class SettingsSection(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    // False while the screen of the section is not built, so its row only stands in its place
    val isReady: Boolean = false
) {
    Appearance(R.string.settings_appearance, R.string.settings_appearance_subtitle, SpectroIcons.Palette, isReady = true),
    Player(R.string.settings_player, R.string.settings_player_subtitle, SpectroIcons.MusicNote, isReady = true),
    Content(R.string.settings_content, R.string.settings_content_subtitle, SpectroIcons.Checklist, isReady = true),
    Storage(R.string.settings_storage, R.string.settings_storage_subtitle, SpectroIcons.Storage, isReady = true),
    Privacy(R.string.settings_privacy, R.string.settings_privacy_subtitle, SpectroIcons.History, isReady = true),
    DesktopSync(R.string.settings_sync_desktop, R.string.settings_sync_desktop_desc, SpectroIcons.Integrations, isReady = true),
    Links(R.string.settings_links, R.string.settings_links_subtitle, SpectroIcons.Link, isReady = true),
    About(R.string.settings_about, R.string.settings_about_subtitle, SpectroIcons.Info, isReady = true)
}

// The sections that sit together in a card under a name
enum class SettingsGroup(@StringRes val titleRes: Int, val sections: List<SettingsSection>) {
    App(R.string.settings_group_app, listOf(SettingsSection.Appearance, SettingsSection.DesktopSync)),
    Playback(R.string.settings_group_playback, listOf(SettingsSection.Player, SettingsSection.Content)),
    Data(R.string.settings_group_data, listOf(SettingsSection.Storage, SettingsSection.Privacy)),
    More(R.string.settings_group_more, listOf(SettingsSection.About))
}
