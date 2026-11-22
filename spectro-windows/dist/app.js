// --- Spectro Desktop Core Application (Complete Port of Android Spectro Features) ---

(function() {
  // Core Application State
  const state = {
    currentTrackIndex: -1,
    isPlaying: false,
    queue: [],
    isShuffle: false,
    isRepeat: false,
    activePage: "home",
    activeLyrics: [],
    currentLyricIndex: -1,
    searchResults: [],
    searchLoading: false,
    homeShelves: [],
    homeLoading: false,
    userPlaylists: [],
    likedTracks: JSON.parse(localStorage.getItem("spectro_liked_songs") || "[]"),
    customPlaylists: JSON.parse(localStorage.getItem("spectro_custom_playlists") || "[]"),
    currentPlaylist: null,
    currentVolume: parseFloat(localStorage.getItem("spectro_volume") || "0.8"),
    radioContinuation: null,
    isExtendingQueue: false
  };

  // Auth State
  let currentSession = {
    is_logged_in: false,
    name: "Pengguna",
    handle: null,
    photo_url: null
  };

  // Navigation History Stack
  const navHistory = [{ page: "home", data: null }];
  let navHistoryIdx = 0;

  // --- Multi-Language & User-Friendly Localization Engine ---
  const I18N = {
    id: {
      nav_home: "Beranda",
      nav_search: "Pencarian",
      nav_library: "Koleksi Saya",
      nav_queue: "Daftar Putar",
      nav_settings: "Pengaturan",
      search_placeholder: "Cari lagu, artis, atau album...",
      section_charts: "Paling Populer Saat Ini",
      section_new_releases: "Rilis Terbaru & Terpopuler",
      section_quick_picks: "Rekomendasi Spesial Buat Kamu",
      section_queue: "Daftar Putar Lagu",
      queue_empty: "Daftar putar masih kosong",
      queue_count: "Lagu berikutnya:",
      btn_pair_device: "Hubungkan HP",
      btn_create_playlist: "Buat Playlist",
      btn_clear_queue: "Bersihkan Antrean",
      btn_smart_queue: "Rekomendasi Serupa",
      lyrics_source_label: "Sumber Lirik",
      lyrics_empty: "Lirik belum tersedia untuk lagu ini",
      status_online: "Terhubung",
      status_ready: "Siap Menghubungkan",
      settings_title: "Pengaturan",
      settings_group_sync: "Sinkronisasi Perangkat",
      settings_sync_desc: "Layanan Sinkronisasi Akun",
      settings_sync_paired: "Akun ini terhubung dengan:",
      settings_sync_none: "Belum ada HP yang terhubung.",
      settings_btn_pair: "Hubungkan HP",
      settings_btn_reconnect: "Hubungkan Ulang",
      settings_group_lyrics: "Sumber Lirik",
      settings_lyrics_desc: "Atur urutan dan aktifkan penyedia lirik sesuai pilihan Anda.",
      settings_lyrics_active: "Aktif digunakan",
      settings_lyrics_inactive: "Dinonaktifkan",
      settings_group_audio: "Kualitas Suara",
      settings_audio_quality: "Kualitas Audio",
      settings_audio_quality_desc: "Pilih kualitas kejernihan suara saat memutar musik",
      settings_audio_auto: "Otomatis (Sesuai Jaringan)",
      settings_audio_high: "Tinggi (256 kbps)",
      settings_audio_normal: "Normal (128 kbps)",
      settings_audio_low: "Hemat Data (64 kbps)",
      settings_audio_normalize: "Penyelarasan Volume Otomatis",
      settings_audio_normalize_desc: "Menjaga volume suara tetap seimbang di setiap lagu",
      settings_group_appearance: "Tampilan & Bahasa",
      settings_lang_label: "Bahasa Tampilan",
      settings_lang_desc: "Pilih bahasa tampilan aplikasi Spectro",
      settings_lang_system: "Otomatis (Ikuti Sistem)",
      settings_lyrics_size: "Ukuran Teks Lirik",
      settings_lyrics_size_desc: "Atur ukuran teks lirik agar nyaman dibaca",
      settings_lyrics_medium: "Sedang (Standar)",
      settings_lyrics_large: "Besar (Fokus Lirik)",
      settings_lyrics_xlarge: "Ekstra Besar (Karaoke)",
      settings_group_storage: "Penyimpanan & Ruang",
      settings_storage_clear: "Hapus Berkas Sementara",
      settings_storage_clear_desc: "Membersihkan cache untuk menyegarkan aplikasi",
      settings_btn_clear: "Bersihkan",
      settings_group_about: "Tentang Aplikasi",
      settings_about_app: "Spectro Music Player for Windows",
      settings_about_version: "Versi 1.0.0 (Official Release)",
      settings_about_license: "Lisensi Bebas GPL-3.0",
      eq_title: "Pengatur Nada (Equalizer)",
      eq_preset: "Pilihan Suara",
      eq_bass: "Penguat Bass",
      pl_modal_title: "Simpan ke Playlist",
      pl_create_title: "Nama Playlist Baru",
      pl_create_btn: "Buat & Simpan",
      pl_cancel: "Batal",
      toast_liked: "Ditambahkan ke Lagu Favorit",
      toast_unliked: "Dihapus dari Lagu Favorit",
      toast_added_pl: "Berhasil disimpan ke playlist",
      toast_pl_created: "Playlist berhasil dibuat"
    },
    en: {
      nav_home: "Home",
      nav_search: "Search",
      nav_library: "Your Library",
      nav_queue: "Up Next",
      nav_settings: "Settings",
      search_placeholder: "Search songs, artists, or albums...",
      section_charts: "Top Charts Right Now",
      section_new_releases: "New & Popular Releases",
      section_quick_picks: "Recommended For You",
      section_queue: "Playback Queue",
      queue_empty: "Queue is currently empty",
      queue_count: "Upcoming songs:",
      btn_pair_device: "Pair Phone",
      btn_create_playlist: "New Playlist",
      btn_clear_queue: "Clear Queue",
      btn_smart_queue: "Similar Picks",
      lyrics_source_label: "Lyrics Source",
      lyrics_empty: "Lyrics not available for this song",
      status_online: "Connected",
      status_ready: "Ready to Pair",
      settings_title: "Settings",
      settings_group_sync: "Device Pairing",
      settings_sync_desc: "Account Synchronization",
      settings_sync_paired: "Connected to device:",
      settings_sync_none: "No smartphone paired yet.",
      settings_btn_pair: "Pair Phone",
      settings_btn_reconnect: "Reconnect",
      settings_group_lyrics: "Lyrics Providers",
      settings_lyrics_desc: "Reorder and toggle your preferred lyrics sources.",
      settings_lyrics_active: "Active",
      settings_lyrics_inactive: "Disabled",
      settings_group_audio: "Audio Playback",
      settings_audio_quality: "Audio Quality",
      settings_audio_quality_desc: "Choose sound clarity and streaming quality",
      settings_audio_auto: "Automatic (Adaptive)",
      settings_audio_high: "High Quality (256 kbps)",
      settings_audio_normal: "Standard (128 kbps)",
      settings_audio_low: "Data Saver (64 kbps)",
      settings_audio_normalize: "Volume Normalization",
      settings_audio_normalize_desc: "Keep volume levels balanced between tracks",
      settings_group_appearance: "Appearance & Language",
      settings_lang_label: "Display Language",
      settings_lang_desc: "Select the display language for Spectro",
      settings_lang_system: "System Default",
      settings_lyrics_size: "Lyrics Font Size",
      settings_lyrics_size_desc: "Adjust lyrics font size for comfortable reading",
      settings_lyrics_medium: "Medium (Standard)",
      settings_lyrics_large: "Large (Lyrics Focus)",
      settings_lyrics_xlarge: "Extra Large (Karaoke)",
      settings_group_storage: "Storage & Cache",
      settings_storage_clear: "Clear Temporary Files",
      settings_storage_clear_desc: "Free up disk space and refresh cached data",
      settings_btn_clear: "Clear Now",
      settings_group_about: "About Spectro",
      settings_about_app: "Spectro Music Player for Windows",
      settings_about_version: "Version 1.0.0 (Official Release)",
      settings_about_license: "Free & Open Source GPL-3.0",
      eq_title: "Equalizer & Tone",
      eq_preset: "Sound Preset",
      eq_bass: "Bass Boost",
      pl_modal_title: "Add to Playlist",
      pl_create_title: "New Playlist Name",
      pl_create_btn: "Create & Add",
      pl_cancel: "Cancel",
      toast_liked: "Added to Liked Songs",
      toast_unliked: "Removed from Liked Songs",
      toast_added_pl: "Saved to playlist",
      toast_pl_created: "Playlist created"
    }
  };

  function getCurrentLanguage() {
    const saved = localStorage.getItem("spectro_language");
    if (saved === "en" || saved === "id") return saved;
    const sys = (navigator.language || navigator.userLanguage || "id").toLowerCase();
    if (sys.startsWith("en")) return "en";
    return "id";
  }

  function t(key) {
    const lang = getCurrentLanguage();
    return (I18N[lang] && I18N[lang][key]) || (I18N.id && I18N.id[key]) || key;
  }

  function applyLanguageToUI() {
    const lang = getCurrentLanguage();
    document.querySelectorAll("[data-i18n]").forEach(el => {
      const key = el.getAttribute("data-i18n");
      if (key && I18N[lang] && I18N[lang][key]) {
        el.textContent = I18N[lang][key];
      }
    });
    document.querySelectorAll("[data-i18n-ph]").forEach(el => {
      const key = el.getAttribute("data-i18n-ph");
      if (key && I18N[lang] && I18N[lang][key]) {
        el.setAttribute("placeholder", I18N[lang][key]);
      }
    });
    document.querySelectorAll("[data-i18n-title]").forEach(el => {
      const key = el.getAttribute("data-i18n-title");
      if (key && I18N[lang] && I18N[lang][key]) {
        el.setAttribute("title", I18N[lang][key]);
      }
    });
  }

  function updateNavArrows() {
    const btnBack = document.getElementById("nav-back-btn");
    const btnFwd = document.getElementById("nav-forward-btn");
    if (btnBack) {
      btnBack.style.opacity = navHistoryIdx > 0 ? "1" : "0.35";
      btnBack.style.cursor = navHistoryIdx > 0 ? "pointer" : "default";
    }
    if (btnFwd) {
      btnFwd.style.opacity = navHistoryIdx < navHistory.length - 1 ? "1" : "0.35";
      btnFwd.style.cursor = navHistoryIdx < navHistory.length - 1 ? "pointer" : "default";
    }
  }

  function pushNav(page, data = null) {
    if (navHistory[navHistoryIdx]?.page === page && JSON.stringify(navHistory[navHistoryIdx]?.data) === JSON.stringify(data)) return;
    navHistory.splice(navHistoryIdx + 1);
    navHistory.push({ page, data });
    navHistoryIdx = navHistory.length - 1;
    updateNavArrows();
  }

  function navigateFromHistory(entry) {
    if (!entry) return;
    const page = entry.page;
    if (page === "home") renderHome(false);
    else if (page === "explore") renderExplore(false);
    else if (page === "radios") renderRadios(false);
    else if (page === "settings") renderSettings(false);
    else if (page === "playlist") openPlaylist(entry.data?.id, entry.data?.title, entry.data?.cover, false);
    updateNavArrows();
  }

  function showToast(msg) {
    let toast = document.getElementById("spectro-toast");
    if (!toast) {
      toast = document.createElement("div");
      toast.id = "spectro-toast";
      toast.style.cssText = "position: fixed; bottom: 95px; left: 50%; transform: translateX(-50%); background: rgba(18, 18, 18, 0.95); color: #fff; border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 500px; padding: 10px 22px; font-size: 0.86rem; font-weight: 600; box-shadow: 0 8px 30px rgba(0,0,0,0.7); z-index: 9999; pointer-events: none; opacity: 0; transition: opacity 0.25s, transform 0.25s;";
      document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.style.opacity = "1";
    toast.style.transform = "translateX(-50%) translateY(0)";
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transform = "translateX(-50%) translateY(10px)";
    }, 2500);
  }

  function updateLikedCountSidebar() {
    const el = document.getElementById("liked-count-meta");
    if (el) {
      el.textContent = `${state.likedTracks.length} lagu`;
    }
  }

  function updateLikeButtonState(track) {
    const btn = document.getElementById("player-like-btn");
    if (!btn || !track) return;
    const isLiked = state.likedTracks.some(t => (t.videoId || t.id) === (track.videoId || track.id));
    if (isLiked) {
      btn.classList.add("liked");
      btn.innerHTML = `<svg viewBox="0 0 24 24" fill="#ff4b72" stroke="#ff4b72"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`;
      btn.title = "Dihapus dari Lagu Disukai";
    } else {
      btn.classList.remove("liked");
      btn.innerHTML = `<svg viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`;
      btn.title = "Sukai Lagu";
    }
  }

  async function toggleLikeCurrentTrack() {
    const track = state.queue[state.currentTrackIndex];
    if (!track) {
      showToast("Putar lagu terlebih dahulu");
      return;
    }
    const vid = track.videoId || track.id;
    const idx = state.likedTracks.findIndex(t => (t.videoId || t.id) === vid);
    const wasLiked = idx !== -1;

    if (wasLiked) {
      state.likedTracks.splice(idx, 1);
      showToast("Dihapus dari Lagu Disukai");
    } else {
      state.likedTracks.unshift(track);
      showToast("Ditambahkan ke Lagu Disukai");
    }

    localStorage.setItem("spectro_liked_songs", JSON.stringify(state.likedTracks));
    updateLikeButtonState(track);
    updateLikedCountSidebar();

    if (state.activePage === "playlist" && state.currentPlaylist === "liked") {
      renderPlaylistTracks("Lagu Disukai", state.likedTracks);
    }

    try {
      invokeTauri("rate_song", { videoId: vid, liked: !wasLiked });
    } catch (_) {}
  }

  let activeModalTargetTrack = null;

  function openSaveToPlaylistModal(targetTrack = null) {
    const track = targetTrack || state.queue[state.currentTrackIndex];
    if (!track) {
      showToast("Pilih atau putar lagu terlebih dahulu");
      return;
    }
    activeModalTargetTrack = track;
    const modal = document.getElementById("playlist-modal");
    if (!modal) return;

    const modalTitle = document.getElementById("playlist-modal-title");
    const modalArtist = document.getElementById("playlist-modal-artist");
    const modalArt = document.getElementById("playlist-modal-art");

    if (modalTitle) modalTitle.textContent = cleanDisplayTitle(track.title);
    if (modalArtist) modalArtist.textContent = track.artist || "";
    if (modalArt) modalArt.src = track.art || (track.videoId ? `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg` : "logo.png");

    renderPlaylistModalList(track);
    modal.classList.remove("hidden");
  }

  function renderPlaylistModalList(track) {
    const listEl = document.getElementById("playlist-modal-list");
    if (!listEl) return;

    const allPlaylists = [
      ...(state.customPlaylists || []).map(p => ({ ...p, isCustom: true })),
      ...(state.userPlaylists || []).map(p => ({ ...p, isCustom: false }))
    ];

    if (allPlaylists.length === 0) {
      listEl.innerHTML = `
        <div style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 0.82rem;">
          Belum ada playlist. Buat nama playlist di atas lalu tekan Buat.
        </div>
      `;
      return;
    }

    const vid = track ? (track.videoId || track.id) : null;

    listEl.innerHTML = allPlaylists.map(pl => {
      const alreadyIn = pl.tracks ? pl.tracks.some(t => (t.videoId || t.id) === vid) : false;
      return `
        <div class="playlist-modal-item" data-modal-pl-id="${pl.id}">
          <div style="display: flex; align-items: center; gap: 10px; overflow: hidden;">
            <img src="${pl.cover || 'logo.png'}" onerror="this.src='logo.png'" style="width: 36px; height: 36px; border-radius: 4px; object-fit: cover;">
            <div style="overflow: hidden;">
              <div style="font-size: 0.85rem; font-weight: 600; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtml(pl.title)}</div>
              <div style="font-size: 0.74rem; color: var(--text-muted);">${pl.isCustom ? 'Playlist Lokal' : 'YouTube Music'}</div>
            </div>
          </div>
          <button class="btn-add-to-pl-action ${alreadyIn ? 'added' : ''}" data-pl-action-id="${pl.id}">
            ${alreadyIn ? '&#10003; Ditambahkan' : '+ Simpan'}
          </button>
        </div>
      `;
    }).join("");

    listEl.querySelectorAll("[data-pl-action-id]").forEach(btn => {
      btn.addEventListener("click", async (e) => {
        e.stopPropagation();
        const plId = btn.getAttribute("data-pl-action-id");
        const chosenPl = allPlaylists.find(p => p.id === plId);
        if (chosenPl && activeModalTargetTrack) {
          await addTrackToPlaylist(chosenPl, activeModalTargetTrack);
          btn.classList.add("added");
          btn.innerHTML = "&#10003; Ditambahkan";
        }
      });
    });
  }

  async function addTrackToPlaylist(playlistObj, track) {
    const vid = track.videoId || track.id;
    if (playlistObj.isCustom) {
      if (!playlistObj.tracks) playlistObj.tracks = [];
      if (!playlistObj.tracks.some(t => (t.videoId || t.id) === vid)) {
        playlistObj.tracks.unshift(track);
        playlistObj.cover = track.art || playlistObj.cover;
        localStorage.setItem("spectro_custom_playlists", JSON.stringify(state.customPlaylists));
      }
    } else {
      try {
        await invokeTauri("edit_playlist", {
          playlistId: playlistObj.id,
          videoId: vid,
          action: "ACTION_ADD_VIDEO"
        });
      } catch (_) {}
    }
    showToast(`Ditambahkan ke "${playlistObj.title}"`);
    renderUserPlaylistsInSidebar(state.userPlaylists);
  }

  async function createAndAddToPlaylist(title, videoIds = []) {
    const newId = `custom_${Date.now()}`;
    const firstTrack = activeModalTargetTrack;
    const newPl = {
      id: newId,
      title,
      cover: firstTrack ? (firstTrack.art || "logo.png") : "logo.png",
      tracks: firstTrack ? [firstTrack] : [],
      isCustom: true
    };
    state.customPlaylists.unshift(newPl);
    localStorage.setItem("spectro_custom_playlists", JSON.stringify(state.customPlaylists));

    if (currentSession.is_logged_in && videoIds.length > 0) {
      try {
        await invokeTauri("create_playlist", { title, videoIds });
        loadUserPlaylists();
      } catch (_) {}
    }

    showToast(`Playlist "${title}" berhasil dibuat!`);
    renderPlaylistModalList(activeModalTargetTrack);
    renderUserPlaylistsInSidebar(state.userPlaylists);
  }

  // YouTube Player Instance & Status
  let ytPlayer = null;
  let ytReady = false;
  let ytSyncTimer = null;
  let pendingPlayId = null;

  // DOM Elements
  const audio = document.getElementById("audio-engine");
  const pageContent = document.getElementById("page-content");
  const navItems = document.querySelectorAll(".nav-item");
  const globalSearchInput = document.getElementById("global-search-input");
  
  // Player Controls
  const btnPlayPause = document.getElementById("btn-play-pause");
  const iconPlay = document.getElementById("icon-play");
  const iconPause = document.getElementById("icon-pause");
  const btnNext = document.getElementById("btn-next");
  const btnPrev = document.getElementById("btn-prev");
  const btnShuffle = document.getElementById("btn-shuffle");
  const btnRepeat = document.getElementById("btn-repeat");
  const playerArt = document.getElementById("player-art");
  const playerTitle = document.getElementById("player-title");
  const playerArtist = document.getElementById("player-artist");
  const timeCurrent = document.getElementById("time-current");
  const timeTotal = document.getElementById("time-total");
  const seekbarContainer = document.getElementById("seekbar-container");
  const seekbarProgress = document.getElementById("seekbar-progress");
  const seekbarThumb = document.getElementById("seekbar-thumb");
  const btnVolume = document.getElementById("btn-volume");
  const volumeContainer = document.getElementById("volume-container");
  const volumeProgress = document.getElementById("volume-progress");
  const volumeThumb = document.getElementById("volume-thumb");
  
  // Sidebar Left (Spotify Style Collapsible)
  const sidebarLeft = document.getElementById("sidebar-left");
  const btnToggleSidebar = document.getElementById("btn-toggle-sidebar");
  const userCustomPlaylists = document.getElementById("user-custom-playlists");
  const likedCountMeta = document.getElementById("liked-count-meta");

  // Main Canvas & Dedicated Music Player View
  const mainCanvas = document.getElementById("main-canvas");
  const musicPlayerView = document.getElementById("music-player-view");
  const btnBackToBrowse = document.getElementById("btn-back-to-browse");
  const pvArtwork = document.getElementById("pv-artwork");
  const pvTitle = document.getElementById("pv-title");
  const pvArtist = document.getElementById("pv-artist");
  const pvAlbum = document.getElementById("pv-album");
  const lyricsContainer = document.getElementById("lyrics-container");

  // Right Sidebar (Dedicated Queue Panel)
  const sidebarRight = document.getElementById("sidebar-right");
  const btnToggleQueue = document.getElementById("btn-toggle-queue");
  const btnTogglePlayerView = document.getElementById("btn-toggle-player-view");
  const queueCountBadge = document.getElementById("queue-count-badge");
  const nowPlayingQueueItem = document.getElementById("now-playing-queue-item");
  const queueActiveTitle = document.getElementById("queue-active-title");
  const queueActiveArtist = document.getElementById("queue-active-artist");
  const queueActiveArt = document.getElementById("queue-active-art");
  const queueList = document.getElementById("queue-list");

  // Device Sync Badge (Spotify Connect Style)
  const deviceSyncBadge = document.getElementById("device-sync-badge");
  const deviceSyncName = document.getElementById("device-sync-name");

  // Helper: Tauri invoke wrapper with bridge retry for early app initialization
  async function invokeTauri(command, args = {}) {
    for (let attempt = 0; attempt < 30; attempt++) {
      const invoker = window.__TAURI__?.core?.invoke 
                   || window.__TAURI__?.invoke 
                   || window.__TAURI_INTERNALS__?.invoke;
      if (typeof invoker === "function") {
        try {
          return await invoker(command, args);
        } catch (err) {
          console.warn(`Tauri command '${command}' returned error:`, err);
          return null;
        }
      }
      await new Promise(res => setTimeout(res, 40));
    }
    return null;
  }

  // --- 1. YouTube Iframe Engine Initialization ---
  let isInitializingPlayer = false;
  function ensureYouTubePlayer() {
    if (ytPlayer && ytReady) return;
    if (isInitializingPlayer) return;
    if (window.YT && window.YT.Player) {
      try {
        const hostEl = document.getElementById("yt-player");
        if (!hostEl) return;
        isInitializingPlayer = true;
        const initialVideoId = pendingPlayId || "JGwWNGJdvx8";
        ytPlayer = new YT.Player("yt-player", {
          height: "240",
          width: "240",
          videoId: initialVideoId,
          host: "https://www.youtube-nocookie.com",
          playerVars: {
            autoplay: pendingPlayId ? 1 : 0,
            controls: 0,
            disablekb: 1,
            fs: 0,
            modestbranding: 1,
            playsinline: 1,
            rel: 0,
            enablejsapi: 1,
            origin: "https://www.youtube-nocookie.com"
          },
          events: {
            onReady: () => {
              console.log("[YT Player] onReady triggered!");
              ytReady = true;
              isInitializingPlayer = false;
              try {
                if (state.currentVolume > 0) {
                  if (typeof ytPlayer.unMute === "function") ytPlayer.unMute();
                  if (typeof ytPlayer.setVolume === "function") ytPlayer.setVolume(Math.round(state.currentVolume * 100));
                } else {
                  if (typeof ytPlayer.mute === "function") ytPlayer.mute();
                }
              } catch (_) {}
              if (pendingPlayId) {
                const vid = pendingPlayId;
                pendingPlayId = null;
                ytPlayer.loadVideoById(vid);
                ytPlayer.playVideo();
                onPlaybackStarted();
              }
            },
            onStateChange: (event) => {
              console.log("[YT Player] onStateChange:", event.data);
              if (event.data === YT.PlayerState.PLAYING) {
                onPlaybackStarted();
              } else if (event.data === YT.PlayerState.PAUSED) {
                onPlaybackPaused();
              } else if (event.data === YT.PlayerState.ENDED) {
                onPlaybackEnded();
              }
            },
            onError: (e) => {
              console.warn("[YT Player] onError:", e.data);
              isInitializingPlayer = false;
              const elapsed = Date.now() - lastTrackLoadTime;
              if (elapsed > 2500) {
                playNext();
              }
            }
          }
        });
      } catch (e) {
        isInitializingPlayer = false;
        console.warn("YT.Player init failed:", e);
      }
    } else {
      if (!document.querySelector('script[src*="iframe_api"]')) {
        const tag = document.createElement("script");
        tag.src = "https://www.youtube.com/iframe_api";
        document.head.appendChild(tag);
      }
    }
  }

  window.onYouTubeIframeAPIReady = function() {
    ensureYouTubePlayer();
  };

  // --- 2. Playback State Synchronization ---
  let lastTrackLoadTime = 0;

  function onPlaybackStarted() {
    state.isPlaying = true;
    iconPlay.classList.add("hidden");
    iconPause.classList.remove("hidden");
    const waveAnim = document.getElementById("queue-wave-anim");
    if (waveAnim) waveAnim.classList.remove("hidden");
    updateDeviceBadge("Komputer Ini (Windows)");
    broadcastPlaybackState(true);
    startSyncTimer();
    setupWebAudioEQ();
    updateEQFilterGains();

    // Enforce configured volume level on the YouTube player
    if (ytPlayer) {
      try {
        if (state.currentVolume > 0) {
          if (typeof ytPlayer.unMute === "function") ytPlayer.unMute();
          if (typeof ytPlayer.setVolume === "function") ytPlayer.setVolume(Math.round(state.currentVolume * 100));
        } else {
          if (typeof ytPlayer.mute === "function") ytPlayer.mute();
        }
      } catch (_) {}
    }
  }

  function onPlaybackPaused() {
    state.isPlaying = false;
    iconPlay.classList.remove("hidden");
    iconPause.classList.add("hidden");
    const waveAnim = document.getElementById("queue-wave-anim");
    if (waveAnim) waveAnim.classList.add("hidden");
    broadcastPlaybackState(false);
    stopSyncTimer();
  }

  function onPlaybackEnded() {
    // Guard against premature ENDED events emitted by YouTube player during video transitions
    const elapsed = Date.now() - lastTrackLoadTime;
    if (elapsed < 3000) {
      console.log("Ignored premature ENDED event during track switch (elapsed:", elapsed, "ms)");
      return;
    }
    // Also verify that current playback actually reached near the end
    if (ytPlayer && typeof ytPlayer.getCurrentTime === "function" && typeof ytPlayer.getDuration === "function") {
      try {
        const cur = ytPlayer.getCurrentTime();
        const dur = ytPlayer.getDuration();
        if (dur > 10 && cur < dur - 4) {
          console.log(`Ignored spurious ENDED event: cur=${cur}, dur=${dur}`);
          return;
        }
      } catch (_) {}
    }

    if (state.isRepeat) {
      if (ytPlayer && ytReady && typeof ytPlayer.seekTo === "function") {
        ytPlayer.seekTo(0);
        ytPlayer.playVideo();
      }
    } else {
      playNext();
    }
  }

  function startSyncTimer() {
    stopSyncTimer();
    ytSyncTimer = setInterval(() => {
      if (!state.isPlaying) return;

      if (ytPlayer && ytReady && typeof ytPlayer.getCurrentTime === "function") {
        const cur = ytPlayer.getCurrentTime() || 0;
        const dur = ytPlayer.getDuration() || (state.queue[state.currentTrackIndex]?.duration || 0);

        timeCurrent.textContent = formatTime(cur);
        if (dur > 0) {
          timeTotal.textContent = formatTime(dur);
          const pct = Math.min(100, (cur / dur) * 100);
          seekbarProgress.style.width = `${pct}%`;
          seekbarThumb.style.left = `${pct}%`;
        }
        syncLyricsWithTime(cur);
      }
    }, 250);
  }

  function stopSyncTimer() {
    if (ytSyncTimer) {
      clearInterval(ytSyncTimer);
      ytSyncTimer = null;
    }
  }

  // --- 3. Pure v1.0.0 YouTube Music Queue Algorithm (1 Genre & Popular Hits) ---
  function shuffleArray(array) {
    const arr = [...array];
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  function cleanDisplayTitle(str) {
    if (!str) return "";
    return str
      .replace(/\s*[\(\[](official\s*(music\s*)?video|video\s*klip|lyric\s*video|official\s*audio|audio\s*only|lirik\s*lagu|audio|hd|4k|hq|visualizer)[\)\]]/gi, '')
      .replace(/\s*\|\s*Official\s*Video/gi, '')
      .trim();
  }

  function cleanTitle(str) {
    if (!str) return "";
    return str.toLowerCase()
      .replace(/\s*[\(\[](feat\.|ft\.|official|video|audio|lyrics|hd|remix).*?[\)\]]/gi, '')
      .replace(/[^a-z0-9]/g, '')
      .trim();
  }

  const ExcludedKeywords = [
    "cover", "karaoke", "instrumental", "acoustic", "slowed", "reverb",
    "sped up", "speed up", "tribute", "parody", "remake", "guitar cover",
    "piano cover", "drum cover", "vocal cover", "ai cover", "fancam",
    "reaction", "live at", "live from", "live in", "unreleased", "snippet",
    "teaser", "full album", "1 hour", "1hour", "loop", "bass boosted",
    "nightcore", "8d audio", "tiktok version", "live concert", "live performance",
    "mashup", "aesthetic", "edit", "tutorial", "fingerstyle"
  ];

  function parsePopularity(bylineRuns) {
    if (!Array.isArray(bylineRuns)) return { views: 0, likes: 0, score: 0 };
    let views = 0, likes = 0;
    for (const r of bylineRuns) {
      const text = (r.text || "").trim().toLowerCase();
      if (text.includes("ditonton") || text.includes("view")) {
        const cleanNum = text.replace(/[^0-9,\.]/g, '').replace(',', '.');
        const val = parseFloat(cleanNum) || 0;
        if (text.includes('m x') || text.includes('b view') || text.includes('miliar')) views = val * 1000000000;
        else if (text.includes('jt') || text.includes('m view') || text.includes('juta')) views = val * 1000000;
        else if (text.includes('rb') || text.includes('k view') || text.includes('ribu')) views = val * 1000;
        else views = val;
      }
      if (text.includes("suka") || text.includes("like")) {
        const cleanNum = text.replace(/[^0-9,\.]/g, '').replace(',', '.');
        const val = parseFloat(cleanNum) || 0;
        if (text.includes('jt') || text.includes('m like') || text.includes('juta')) likes = val * 1000000;
        else if (text.includes('rb') || text.includes('k like') || text.includes('ribu')) likes = val * 1000;
        else likes = val;
      }
    }
    const score = (views / 1000000) * 1.0 + (likes / 100000) * 2.0;
    return { views, likes, score };
  }

  function isDerivativeOrJunk(title, artist, seedTitle, pop) {
    const raw = `${title} ${artist}`.toLowerCase();
    if (ExcludedKeywords.some(b => raw.includes(b))) return true;

    // Filter low engagement / obscure uploads (< 500k views or < 3000 likes if available)
    if (pop) {
      if ((pop.views > 0 && pop.views < 500000) || (pop.likes > 0 && pop.likes < 3000)) {
        return true;
      }
    }

    // Reject live versions unless seed song was already live
    if (seedTitle && !seedTitle.toLowerCase().includes("live")) {
      if (raw.includes("(live") || raw.includes("[live") || raw.includes("live version") || raw.includes("live audio")) {
        return true;
      }
    }

    // Reject alternate versions of the same seed song
    const cleanT = cleanTitle(title);
    const cleanS = cleanTitle(seedTitle);
    if (cleanT && cleanS && (cleanT === cleanS || cleanT.includes(cleanS) || cleanS.includes(cleanT))) {
      return true;
    }
    return false;
  }

  function organizePopularQueue(validTracks, seedArtist) {
    if (!validTracks || validTracks.length <= 1) return validTracks;

    // Ambil kolam lagu-lagu hits teratas (30 lagu teratas dari stasiun radio)
    // dan kolam lanjutan (sisa lagu untuk perpanjangan antrean)
    const topHitsPool = validTracks.slice(0, 30);
    const extendedPool = validTracks.slice(30);

    // Pengacakan dinamis dari seluruh kolam hits teratas setiap pemutaran lagu
    // Hal ini menjamin ketika lagu A diputar ulang (misal A -> B -> A), antrean yang dihasilkan
    // SELALU BERBEDA baik dari variasi pilihan lagu hits maupun urutannya.
    const shuffledTopPool = shuffleArray(topHitsPool);

    const tier1 = shuffledTopPool.slice(0, 14);
    const tier2 = shuffledTopPool.slice(14);
    const tier3 = shuffleArray(extendedPool);

    // Interleave artists to prevent 3+ consecutive songs from the same artist
    function interleaveArtists(tracks, initialArtist) {
      const result = [];
      const pool = [...tracks];
      let lastArtist = (initialArtist || "").toLowerCase().trim();

      while (pool.length > 0) {
        let pickIdx = pool.findIndex(t => {
          const a = (t.artist || "").toLowerCase().trim();
          return a && !lastArtist.includes(a) && !a.includes(lastArtist);
        });
        if (pickIdx === -1) pickIdx = 0;
        const chosen = pool.splice(pickIdx, 1)[0];
        result.push(chosen);
        lastArtist = (chosen.artist || "").toLowerCase().trim();
      }
      return result;
    }

    const finalT1 = interleaveArtists(tier1, seedArtist);
    const lastT1Artist = finalT1.length > 0 ? finalT1[finalT1.length - 1].artist : seedArtist;
    const finalT2 = interleaveArtists(tier2, lastT1Artist);
    const lastT2Artist = finalT2.length > 0 ? finalT2[finalT2.length - 1].artist : lastT1Artist;
    const finalT3 = interleaveArtists(tier3, lastT2Artist);

    return [...finalT1, ...finalT2, ...finalT3];
  }

  function parseRadioPage(responseJson) {
    const tracks = [];
    let continuation = null;
    const seenIds = new Set();
    const badges = new Set(["Lagu", "Song", "Video", "Single", "Album", "EP", "Artis", "Artist"]);

    try {
      const data = typeof responseJson === "string" ? JSON.parse(responseJson) : responseJson;
      let panel = null;
      let contents = null;
      if (data?.continuationContents?.playlistPanelContinuation) {
        panel = data.continuationContents.playlistPanelContinuation;
        contents = panel.contents;
      } else if (data?.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer) {
        panel = data.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content.musicQueueRenderer.content.playlistPanelRenderer;
        contents = panel.contents;
      }

      if (panel) {
        const conts = panel.continuations;
        if (Array.isArray(conts) && conts.length > 0) {
          continuation = conts[0]?.nextRadioContinuationData?.continuation || conts[0]?.nextContinuationData?.continuation || null;
        }
      }

      if (Array.isArray(contents)) {
        for (let i = 0; i < contents.length; i++) {
          const row = contents[i];
          const wrapper = row.playlistPanelVideoWrapperRenderer;
          let item = row.playlistPanelVideoRenderer || wrapper?.primaryRenderer?.playlistPanelVideoRenderer;
          // Prefer audio counterpart over video wrapper
          if (wrapper && Array.isArray(wrapper.counterpart)) {
            const audioCp = wrapper.counterpart.find(c => c?.counterpartRenderer?.playlistPanelVideoRenderer);
            if (audioCp) {
              item = audioCp.counterpartRenderer.playlistPanelVideoRenderer;
            }
          }
          if (!item || !item.videoId) continue;
          const videoId = item.videoId;
          if (seenIds.has(videoId)) continue;
          seenIds.add(videoId);

          const title = item.title?.runs?.[0]?.text || "Lagu";
          let artist = "Artis YouTube";
          let album = "Single";
          const subRuns = item.longBylineText?.runs || item.shortBylineText?.runs || [];
          const texts = subRuns.map(r => r.text).filter(t => t !== " • " && t.trim().length > 0);
          const filtered = texts.filter(t => !badges.has(t));
          if (filtered.length > 0) artist = filtered[0];
          if (filtered.length > 1) album = filtered[1];

          let art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
          const thumbs = item.thumbnail?.thumbnails;
          if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;

          const pop = parsePopularity(subRuns);

          tracks.push({
            id: videoId,
            videoId,
            title,
            artist,
            album,
            duration: 0,
            art,
            rawRank: i,
            views: pop.views,
            likes: pop.likes,
            score: pop.score
          });
        }
      } else {
        function walk(obj) {
          if (!obj || typeof obj !== "object") return;
          if (obj.playlistPanelVideoRenderer) {
            const item = obj.playlistPanelVideoRenderer;
            const videoId = item.videoId;
            if (videoId && !seenIds.has(videoId)) {
              seenIds.add(videoId);
              const title = item.title?.runs?.[0]?.text || "Lagu";
              let artist = "Artis YouTube";
              let album = "Single";
              const subRuns = item.longBylineText?.runs || item.shortBylineText?.runs || [];
              const texts = subRuns.map(r => r.text).filter(t => t !== " • " && t.trim().length > 0);
              const filtered = texts.filter(t => !badges.has(t));
              if (filtered.length > 0) artist = filtered[0];
              if (filtered.length > 1) album = filtered[1];

              let art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
              const thumbs = item.thumbnail?.thumbnails;
              if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;

              const pop = parsePopularity(subRuns);

              tracks.push({
                id: videoId,
                videoId,
                title,
                artist,
                album,
                duration: 0,
                art,
                rawRank: tracks.length,
                views: pop.views,
                likes: pop.likes,
                score: pop.score
              });
            }
          }
          if ((obj.nextRadioContinuationData?.continuation || obj.nextContinuationData?.continuation) && !continuation) {
            continuation = obj.nextRadioContinuationData?.continuation || obj.nextContinuationData?.continuation;
          }
          for (const k in obj) {
            if (Object.prototype.hasOwnProperty.call(obj, k)) walk(obj[k]);
          }
        }
        walk(data);
      }
    } catch (_) {}
    return { tracks, continuation };
  }

  function parseRadioTracks(responseJson) {
    return parseRadioPage(responseJson).tracks;
  }

  async function playTrackWithRadio(track, preserveHistory = false, clickedIdx = null) {
    if (!track) return;
    
    // YouTube Music Queue System:
    // When playing a song from within the queue, retain earlier history and anchor fresh radio recommendations
    if (preserveHistory && clickedIdx !== null && clickedIdx >= 0 && clickedIdx < state.queue.length) {
      const history = state.queue.slice(0, clickedIdx).slice(-15);
      state.queue = [...history, track];
      state.currentTrackIndex = state.queue.length - 1;
    } else {
      state.queue = [track];
      state.currentTrackIndex = 0;
    }
    
    loadTrack(state.currentTrackIndex, true);
    state.radioContinuation = null;

    // Show loading indicator in queue list while fetching radio tracks
    if (queueList) {
      queueList.innerHTML = `
        <div style="padding: 30px 16px; text-align: center; color: var(--text-secondary);">
          <svg style="width: 24px; height: 24px; animation: spin 1s linear infinite; margin-bottom: 8px;" viewBox="0 0 24 24"><path fill="currentColor" d="M12 4V2A10 10 0 0 0 2 12h2a8 8 0 0 1 8-8z"/></svg>
          <div style="font-size: 0.85rem; font-weight: 500;">Menyiapkan rekomendasi lagu terbaik...</div>
        </div>
      `;
    }

    // 2. Query YouTube Music Radio with 1-Genre & Popular Hits Algorithm (Sama seperti di APK v1.0.0)
    const seedId = track.videoId || track.id;
    const seedTitle = track.title;

    try {
      const rawJson = await invokeTauri("get_radio", { videoId: seedId, continuation: null });
      if (rawJson) {
        const { tracks: allRadioTracks, continuation } = parseRadioPage(rawJson);
        state.radioContinuation = continuation;

        const seenKeys = new Set();
        const seenIds = new Set();
        state.queue.forEach(t => {
          const id = t.videoId || t.id;
          if (id) seenIds.add(id);
          const k = cleanTitle(t.title);
          if (k) seenKeys.add(k);
        });

        const validRemaining = allRadioTracks.filter(t => {
          if (!t.videoId || seenIds.has(t.videoId)) return false;
          const k = cleanTitle(t.title);
          if (k && seenKeys.has(k)) return false;
          if (isDerivativeOrJunk(t.title, t.artist, seedTitle, { views: t.views, likes: t.likes })) return false;
          seenIds.add(t.videoId);
          if (k) seenKeys.add(k);
          return true;
        });

        // Pengacakan Dinamis Populer Bertingkat (Tiered Popularity Dynamic Shuffle):
        // Mengelompokkan lagu ke dalam tier popularitas teratas (superhits) dan mengacak dinamis
        // dengan interleaving artis agar variasi selalu segar dan lagu berikutnya dijamin populer
        const organizedRemaining = organizePopularQueue(validRemaining, track.artist);

        state.queue = [...state.queue, ...organizedRemaining];
        renderQueueList();

        // If initial batch has fewer than 25 tracks and continuation exists, extend dynamically right away
        if (state.queue.length - state.currentTrackIndex < 25 && state.radioContinuation) {
          extendQueueFromRadio();
        }
      }
    } catch (e) {
      console.warn("get_radio error:", e);
      renderQueueList();
    }
  }

  // YouTube Music Infinite Queue Auto-Extension:
  // Dynamically extends the queue with more popular 1-genre hits so playback never runs dry or loops back
  async function extendQueueFromRadio() {
    if (state.isExtendingQueue) return;
    state.isExtendingQueue = true;
    try {
      const lastTrack = state.queue[state.queue.length - 1] || state.queue[state.currentTrackIndex];
      if (!lastTrack) return;
      const vid = lastTrack.videoId || lastTrack.id;
      const rawJson = await invokeTauri("get_radio", {
        videoId: vid,
        continuation: state.radioContinuation || null
      });
      if (rawJson) {
        const { tracks: newTracks, continuation } = parseRadioPage(rawJson);
        state.radioContinuation = continuation;

        const seenIds = new Set(state.queue.map(t => t.videoId || t.id));
        const seenKeys = new Set(state.queue.map(t => cleanTitle(t.title)).filter(Boolean));

        const validNew = newTracks.filter(t => {
          if (!t.videoId || seenIds.has(t.videoId)) return false;
          const k = cleanTitle(t.title);
          if (k && seenKeys.has(k)) return false;
          if (isDerivativeOrJunk(t.title, t.artist, lastTrack.title, { views: t.views, likes: t.likes })) return false;
          seenIds.add(t.videoId);
          if (k) seenKeys.add(k);
          return true;
        });

        if (validNew.length > 0) {
          const organizedNew = organizePopularQueue(validNew, lastTrack.artist);
          state.queue = [...state.queue, ...organizedNew];
          renderQueueList();
        }
      }
    } catch (e) {
      console.warn("extendQueueFromRadio error:", e);
    } finally {
      state.isExtendingQueue = false;
    }
  }

  // --- 4. Track Loading & Playback Controls ---
  function saveRecentlyPlayed(track) {
    if (!track || !track.title) return;
    const vid = track.videoId || track.id;
    if (!vid) return;
    let list = [];
    try {
      list = JSON.parse(localStorage.getItem("spectro_recently_played") || "[]");
    } catch (_) {}
    list = list.filter(t => (t.videoId || t.id) !== vid);
    list.unshift({
      id: vid,
      videoId: vid,
      title: track.title,
      artist: track.artist || "Artis YouTube",
      album: track.album || "Single",
      art: track.art || `https://i.ytimg.com/vi/${vid}/hqdefault.jpg`,
      duration: track.duration || 0
    });
    if (list.length > 20) list = list.slice(0, 20);
    try {
      localStorage.setItem("spectro_recently_played", JSON.stringify(list));
    } catch (_) {}
  }

  function getRecentlyPlayed() {
    try {
      return JSON.parse(localStorage.getItem("spectro_recently_played") || "[]");
    } catch (_) {
      return [];
    }
  }

  function loadTrack(index, autoplay = true) {
    if (index < 0 || index >= state.queue.length) return;
    lastTrackLoadTime = Date.now();
    state.currentTrackIndex = index;

    // Reset per-song lyrics source override for the new track (revert to Settings default)
    state.songLyricsOverride = null;
    const selectTrackLyrics = document.getElementById("select-track-lyrics-source");
    if (selectTrackLyrics) {
      selectTrackLyrics.innerHTML = '<option value="">Memuat sumber...</option>';
      selectTrackLyrics.disabled = true;
    }

    const track = state.queue[index];
    saveRecentlyPlayed(track);
    const safeArt = track.art || (track.videoId ? `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg` : "logo.png");

    // Update Bottom Mini Player Bar
    playerTitle.textContent = cleanDisplayTitle(track.title);
    playerArtist.textContent = track.artist;
    playerArt.src = safeArt;
    playerArt.onerror = () => { playerArt.src = "logo.png"; };

    // Update Dedicated Music Player View in Main Canvas
    if (pvTitle) pvTitle.textContent = cleanDisplayTitle(track.title);
    if (pvArtist) pvArtist.textContent = track.artist;
    if (pvAlbum) pvAlbum.textContent = track.album || "Spectro Pure Audio";
    if (pvArtwork) {
      pvArtwork.src = safeArt;
      pvArtwork.onerror = () => { pvArtwork.src = "logo.png"; };
    }

    // Update Dedicated Queue Panel
    if (queueActiveTitle) queueActiveTitle.textContent = cleanDisplayTitle(track.title);
    if (queueActiveArtist) queueActiveArtist.textContent = track.artist;
    if (queueActiveArt) {
      queueActiveArt.src = safeArt;
      queueActiveArt.onerror = () => { queueActiveArt.src = "logo.png"; };
    }

    timeCurrent.textContent = "0:00";
    timeTotal.textContent = formatTime(track.duration || 0);
    seekbarProgress.style.width = "0%";
    seekbarThumb.style.left = "0%";

    renderQueueList();
    updateLikeButtonState(track);
    fetchAndRenderLyrics(track);
    updateDeviceBadge("Komputer Ini (Windows)");

    // Auto-extend queue when nearing end (<= 5 tracks left)
    if (state.queue.length - state.currentTrackIndex <= 5) {
      extendQueueFromRadio();
    }

    if (autoplay) {
      playCurrentTrack();
    }
  }

  function playCurrentTrack() {
    const track = state.queue[state.currentTrackIndex];
    if (!track) return;
    const vid = track.videoId || track.id;
    if (!vid) return;

    if (!ytPlayer || !ytReady) {
      pendingPlayId = vid;
      ensureYouTubePlayer();
      return;
    }

    try {
      ytPlayer.loadVideoById(vid);
      ytPlayer.playVideo();
      onPlaybackStarted();
    } catch (e) {
      console.warn("playCurrentTrack error:", e);
      pendingPlayId = vid;
      ensureYouTubePlayer();
    }
  }

  function togglePlayPause() {
    if (state.queue.length === 0 || state.currentTrackIndex < 0) {
      return;
    }
    if (state.isPlaying) {
      if (ytPlayer && ytReady && typeof ytPlayer.pauseVideo === "function") {
        ytPlayer.pauseVideo();
      }
      onPlaybackPaused();
    } else {
      if (ytPlayer && ytReady && typeof ytPlayer.playVideo === "function") {
        ytPlayer.playVideo();
      } else {
        playCurrentTrack();
      }
      onPlaybackStarted();
    }
  }

  const DEFAULT_INITIAL_HITS = [
    { id: "JGwWNGJdvx8", videoId: "JGwWNGJdvx8", title: "Shape of You", artist: "Ed Sheeran", art: "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg" },
    { id: "kffacxfA7G4", videoId: "kffacxfA7G4", title: "Baby", artist: "Justin Bieber", art: "https://i.ytimg.com/vi/kffacxfA7G4/hqdefault.jpg" },
    { id: "09R8_2nJtjg", videoId: "09R8_2nJtjg", title: "Sugar", artist: "Maroon 5", art: "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg" },
    { id: "fJ9rUzIMcZQ", videoId: "fJ9rUzIMcZQ", title: "Bohemian Rhapsody", artist: "Queen", art: "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg" },
    { id: "k4V3Ui68k7Q", videoId: "k4V3Ui68k7Q", title: "Wide Awake", artist: "Katy Perry", art: "https://i.ytimg.com/vi/k4V3Ui68k7Q/hqdefault.jpg" },
    { id: "fLexgOxsZu0", videoId: "fLexgOxsZu0", title: "Call Me Maybe", artist: "Carly Rae Jepsen", art: "https://i.ytimg.com/vi/fLexgOxsZu0/hqdefault.jpg" },
    { id: "7PCkvCPvDXk", videoId: "7PCkvCPvDXk", title: "Somebody To You", artist: "The Vamps", art: "https://i.ytimg.com/vi/7PCkvCPvDXk/hqdefault.jpg" },
    { id: "AByfaY8IDd8", videoId: "AByfaY8IDd8", title: "Heart Attack", artist: "Demi Lovato", art: "https://i.ytimg.com/vi/AByfaY8IDd8/hqdefault.jpg" },
    { id: "kJQP7kiw5Fk", videoId: "kJQP7kiw5Fk", title: "Despacito", artist: "Luis Fonsi", art: "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg" },
    { id: "OPf0YbXqDm0", videoId: "OPf0YbXqDm0", title: "Uptown Funk", artist: "Mark Ronson", art: "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg" }
  ];

  async function playNext() {
    if (state.queue.length === 0) return;
    if (state.queue.length - state.currentTrackIndex <= 5) {
      extendQueueFromRadio();
    }
    let nextIndex = state.currentTrackIndex + 1;
    if (state.isShuffle) {
      nextIndex = Math.floor(Math.random() * state.queue.length);
    } else if (nextIndex >= state.queue.length) {
      // YouTube Music queue: extend dynamically instead of looping back to song A
      await extendQueueFromRadio();
      if (state.currentTrackIndex + 1 < state.queue.length) {
        nextIndex = state.currentTrackIndex + 1;
      } else {
        const cur = state.queue[state.currentTrackIndex];
        if (cur) {
          playTrackWithRadio(cur, true, state.currentTrackIndex);
          return;
        }
        nextIndex = 0;
      }
    }
    loadTrack(nextIndex, true);
  }

  function playPrev() {
    if (state.queue.length === 0) return;
    let prevIndex = state.currentTrackIndex - 1;
    if (prevIndex < 0) prevIndex = state.queue.length - 1;
    loadTrack(prevIndex, true);
  }

  function updateVolumeIcon(vol) {
    if (!btnVolume) return;
    if (vol <= 0.01) {
      btnVolume.innerHTML = `<svg viewBox="0 0 24 24"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>`;
      btnVolume.title = "Bisukan (Muted)";
    } else if (vol < 0.5) {
      btnVolume.innerHTML = `<svg viewBox="0 0 24 24"><path d="M18.5 12c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM5 9v6h4l5 5V4L9 9H5z"/></svg>`;
      btnVolume.title = `Volume: ${Math.round(vol * 100)}%`;
    } else {
      btnVolume.innerHTML = `<svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L9 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>`;
      btnVolume.title = `Volume: ${Math.round(vol * 100)}%`;
    }
  }

  let lastNonZeroVolume = 0.8;

  function setVolume(val) {
    state.currentVolume = Math.max(0, Math.min(1, val));
    if (state.currentVolume > 0) {
      lastNonZeroVolume = state.currentVolume;
    }
    localStorage.setItem("spectro_volume", state.currentVolume.toString());
    if (volumeProgress) volumeProgress.style.width = `${state.currentVolume * 100}%`;
    if (volumeThumb) volumeThumb.style.left = `${state.currentVolume * 100}%`;
    updateVolumeIcon(state.currentVolume);
    if (ytPlayer) {
      try {
        if (state.currentVolume > 0) {
          if (typeof ytPlayer.unMute === "function") ytPlayer.unMute();
          if (typeof ytPlayer.setVolume === "function") ytPlayer.setVolume(Math.round(state.currentVolume * 100));
        } else {
          if (typeof ytPlayer.mute === "function") ytPlayer.mute();
        }
      } catch (err) {
        console.warn("ytPlayer volume error:", err);
      }
    }
  }

  // --- 5. Dedicated Queue Panel Rendering ---
  function renderQueueList() {
    if (!queueList) return;

    if (state.queue.length === 0 || state.currentTrackIndex < 0) {
      const waveAnim = document.getElementById("queue-wave-anim");
      if (waveAnim) waveAnim.classList.add("hidden");
      if (queueCountBadge) queueCountBadge.textContent = "0 lagu";
      if (queueActiveTitle) queueActiveTitle.textContent = "Tidak ada lagu diputar";
      if (queueActiveArtist) queueActiveArtist.textContent = "-";
      if (queueActiveArt) queueActiveArt.src = "logo.png";
      queueList.innerHTML = `
        <div style="padding: 40px 16px; text-align: center; color: var(--text-muted); font-size: 0.9rem;">
          <svg viewBox="0 0 24 24" width="36" height="36" fill="currentColor" style="opacity: 0.25; margin-bottom: 10px;"><path d="M15 6H3v2h12V6zm0 4H3v2h12v-2zM3 16h8v-2H3v2zM17 6v8.18c-.31-.11-.65-.18-1-.18-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3V8h3V6h-5z"/></svg>
          <div style="font-weight: 600; color: var(--text-secondary); margin-bottom: 4px;">Antrean Kosong</div>
          <div style="font-size: 0.8rem; color: var(--text-muted); line-height: 1.4;">Pilih dan putar lagu dari Beranda atau Pencarian untuk memulai antrean radio otomatis.</div>
        </div>
      `;
      return;
    }

    if (queueCountBadge) {
      queueCountBadge.textContent = `${state.queue.length} lagu`;
    }

    const currentTrack = state.queue[state.currentTrackIndex];
    if (currentTrack) {
      if (queueActiveTitle) queueActiveTitle.textContent = cleanDisplayTitle(currentTrack.title);
      if (queueActiveArtist) queueActiveArtist.textContent = currentTrack.artist;
      if (queueActiveArt) {
        queueActiveArt.src = currentTrack.art || (currentTrack.videoId ? `https://i.ytimg.com/vi/${currentTrack.videoId}/hqdefault.jpg` : "logo.png");
      }
      const waveAnim = document.getElementById("queue-wave-anim");
      if (waveAnim && state.isPlaying) waveAnim.classList.remove("hidden");
    }

    const upNext = state.queue.slice(state.currentTrackIndex + 1);
    if (upNext.length === 0) {
      queueList.innerHTML = `
        <div style="padding: 24px 16px; text-align: center; color: var(--text-muted); font-size: 0.85rem;">
          Antrean selesai. Putar lagu lain untuk memulai rekomendasi radio baru.
        </div>
      `;
      return;
    }

    queueList.innerHTML = upNext.map((track, i) => {
      const actualIdx = state.currentTrackIndex + 1 + i;
      const art = track.art || (track.videoId ? `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg` : "logo.png");
      return `
        <div class="queue-item" data-queue-idx="${actualIdx}">
          <span class="queue-idx">${i + 1}</span>
          <img src="${art}" onerror="this.src='logo.png'" alt="${track.title}" class="queue-thumb">
          <div class="queue-text">
            <span class="q-title">${cleanDisplayTitle(track.title)}</span>
            <span class="q-artist">${track.artist}</span>
          </div>
          <button class="queue-play-hover-btn" title="Putar Sekarang">
            <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
          </button>
        </div>
      `;
    }).join('');

    queueList.querySelectorAll("[data-queue-idx]").forEach(item => {
      item.addEventListener("click", (e) => {
        e.stopPropagation();
        const idx = parseInt(item.getAttribute("data-queue-idx"));
        if (!isNaN(idx) && state.queue[idx]) {
          playTrackWithRadio(state.queue[idx], true, idx);
        }
      });
    });
  }

  const DEFAULT_LYRICS_SOURCES = [
    { id: "betterlyrics", name: "BetterLyrics (Sinkron Kata demi Kata)", enabled: true },
    { id: "paxsenix", name: "Paxsenix (Apple Music)", enabled: true },
    { id: "lyricsplus", name: "LyricsPlus (Musixmatch)", enabled: true },
    { id: "lrclib", name: "LrcLib (Komunitas Global)", enabled: true },
    { id: "kugou", name: "KuGou (Musik Asia & Global)", enabled: true },
    { id: "ytm", name: "YouTube Music (Lirik Resmi)", enabled: true },
    { id: "captions", name: "YouTube Captions (Teks Otomatis)", enabled: true }
  ];

  function getLyricsSettings() {
    let settings = null;
    try {
      settings = JSON.parse(localStorage.getItem("spectro_lyrics_settings") || "null");
    } catch (_) {}
    if (!settings || !Array.isArray(settings.sources)) {
      settings = { sources: DEFAULT_LYRICS_SOURCES };
    } else {
      // Auto-migrate: ensure all 7 sources are present
      const existingIds = new Set(settings.sources.map(s => s.id));
      for (const def of DEFAULT_LYRICS_SOURCES) {
        if (!existingIds.has(def.id)) {
          settings.sources.push(def);
        }
      }
    }
    return settings;
  }

  // --- 6. Synced Lyrics Engine (Parallel Probe + Dynamic Dropdown of Available Sources) ---
  let currentLyricsProbeAbort = null;
  let trackAvailableLyrics = []; // Array of { id, name, lines, plain, isSynced, isWordByWord }

  async function fetchAndRenderLyrics(track) {
    if (!lyricsContainer || !track) return;

    if (currentLyricsProbeAbort) {
      currentLyricsProbeAbort.abort();
    }
    const abortCtrl = new AbortController();
    currentLyricsProbeAbort = abortCtrl;

    const selectSourceEl = document.getElementById("select-track-lyrics-source");
    if (selectSourceEl) {
      selectSourceEl.innerHTML = '<option value="">Mencari sumber lirik...</option>';
      selectSourceEl.disabled = true;
    }

    lyricsContainer.innerHTML = `
      <div style="padding: 60px 0; text-align: center; color: var(--text-secondary);">
        <p style="font-size: 1.1rem; margin-bottom: 6px;">Memuat lirik...</p>
        <span style="font-size: 0.85rem; color: var(--text-muted);">Mencari lirik tersinkron terbaik...</span>
      </div>
    `;
    state.activeLyrics = [];
    state.currentLyricIndex = -1;
    trackAvailableLyrics = [];

    const cleanArtist = (track.artist || "").split(/[,&•]|feat\.|ft\./)[0]?.trim();
    const cleanT = cleanDisplayTitle(track.title);
    const duration = track.duration || 0;
    const videoId = track.videoId || track.id || "";

    const lyricsSettings = getLyricsSettings();

    // Parallel fetchers for all 7 providers
    const probes = [
      // 1. BetterLyrics
      (async () => {
        try {
          const raw = await invokeTauri("get_betterlyrics", { title: cleanT, artist: cleanArtist, duration });
          if (raw && !abortCtrl.signal.aborted) {
            const json = JSON.parse(raw);
            if (json.ttml) {
              const lines = parseBetterLyricsTTML(json.ttml);
              if (lines && lines.length > 0) {
                return {
                  id: "betterlyrics",
                  name: "BetterLyrics (Kata demi Kata TTML)",
                  lines,
                  isSynced: true,
                  isWordByWord: true
                };
              }
            }
          }
        } catch (_) {}
        return null;
      })(),

      // 2. Paxsenix (Apple Music)
      (async () => {
        try {
          const raw = await invokeTauri("get_paxsenix_lyrics", { title: cleanT, artist: cleanArtist, duration });
          if (raw && !abortCtrl.signal.aborted) {
            const json = JSON.parse(raw);
            if (json.ttmlContent) {
              const lines = parseBetterLyricsTTML(json.ttmlContent);
              if (lines && lines.length > 0) {
                return {
                  id: "paxsenix",
                  name: "Paxsenix (Apple Music TTML)",
                  lines,
                  isSynced: true,
                  isWordByWord: true
                };
              }
            }
            const text = json.elrcMultiPerson || json.elrc || json.plain;
            if (text) {
              const parsed = parseLrc(text);
              if (parsed && parsed.length > 0) {
                return {
                  id: "paxsenix",
                  name: "Paxsenix (Apple Music Tersinkron)",
                  lines: parsed,
                  isSynced: true
                };
              }
            }
          }
        } catch (_) {}
        return null;
      })(),

      // 3. LyricsPlus (Musixmatch)
      (async () => {
        try {
          const raw = await invokeTauri("get_lyricsplus", { title: cleanT, artist: cleanArtist, duration });
          if (raw && !abortCtrl.signal.aborted) {
            const json = JSON.parse(raw);
            if (json.lyrics && Array.isArray(json.lyrics)) {
              const lines = json.lyrics.map(l => ({
                time: (l.time || 0) / 1000,
                text: l.text || ""
              })).filter(l => l.text.trim().length > 0);
              if (lines.length > 0) {
                return {
                  id: "lyricsplus",
                  name: "LyricsPlus (Musixmatch Tersinkron)",
                  lines: lines.sort((a, b) => a.time - b.time),
                  isSynced: true
                };
              }
            }
          }
        } catch (_) {}
        return null;
      })(),

      // 4. LrcLib
      (async () => {
        try {
          const raw = await invokeTauri("get_lyrics", { title: cleanT, artist: cleanArtist });
          if (raw && !abortCtrl.signal.aborted) {
            const json = JSON.parse(raw);
            if (json.syncedLyrics) {
              const lines = parseLrc(json.syncedLyrics);
              if (lines && lines.length > 0) {
                return {
                  id: "lrclib",
                  name: "LrcLib (Tersinkron)",
                  lines,
                  isSynced: true
                };
              }
            } else if (json.plainLyrics && json.plainLyrics.trim().length > 0) {
              return {
                id: "lrclib",
                name: "LrcLib (Teks Statis)",
                plain: json.plainLyrics,
                isSynced: false
              };
            }
          }
        } catch (_) {}
        return null;
      })(),

      // 5. KuGou
      (async () => {
        try {
          const raw = await invokeTauri("get_kugou_lyrics", { title: cleanT, artist: cleanArtist, duration });
          if (raw && !abortCtrl.signal.aborted) {
            const lines = parseLrc(raw);
            if (lines && lines.length > 0) {
              return {
                id: "kugou",
                name: "KuGou (Asia & Global)",
                lines,
                isSynced: true
              };
            }
          }
        } catch (_) {}
        return null;
      })(),

      // 6. YouTube Music
      (async () => {
        try {
          if (!videoId) return null;
          const raw = await invokeTauri("get_ytm_lyrics", { videoId });
          if (raw && raw.trim().length > 0 && !abortCtrl.signal.aborted) {
            return {
              id: "ytm",
              name: "YouTube Music (Lirik Resmi)",
              plain: raw,
              isSynced: false
            };
          }
        } catch (_) {}
        return null;
      })(),

      // 7. YouTube Captions
      (async () => {
        try {
          if (!videoId) return null;
          const raw = await invokeTauri("get_captions_lyrics", { videoId });
          if (raw && raw.trim().length > 0 && !abortCtrl.signal.aborted) {
            const lines = parseLrc(raw);
            if (lines && lines.length > 0) {
              return {
                id: "captions",
                name: "YouTube Captions (Transkrip)",
                lines,
                isSynced: true
              };
            }
          }
        } catch (_) {}
        return null;
      })()
    ];

    const results = await Promise.allSettled(probes);
    if (abortCtrl.signal.aborted) return;

    const available = results
      .filter(r => r.status === "fulfilled" && r.value != null)
      .map(r => r.value);

    trackAvailableLyrics = available;

    if (available.length === 0) {
      if (selectSourceEl) {
        selectSourceEl.innerHTML = '<option value="">Lirik Belum Tersedia</option>';
        selectSourceEl.disabled = true;
      }
      lyricsContainer.innerHTML = `
        <div style="padding: 60px 0; text-align: center; color: var(--text-muted);">
          <p style="font-size: 1.1rem; margin-bottom: 8px;">Instrumental atau Lirik Belum Tersedia</p>
          <span style="font-size: 0.85rem;">Nikmati alunan musik murni Spectro</span>
        </div>
      `;
      return;
    }

    // Sort available sources by user settings priority order
    const priorityOrder = lyricsSettings.sources.map(s => s.id);
    available.sort((a, b) => {
      const idxA = priorityOrder.indexOf(a.id);
      const idxB = priorityOrder.indexOf(b.id);
      return (idxA === -1 ? 999 : idxA) - (idxB === -1 ? 999 : idxB);
    });

    // Populate dropdown ONLY with available sources!
    if (selectSourceEl) {
      selectSourceEl.disabled = false;
      selectSourceEl.innerHTML = available.map(s => `<option value="${s.id}">${s.name}</option>`).join("");
      
      selectSourceEl.onchange = (e) => {
        const selectedId = e.target.value;
        const chosen = trackAvailableLyrics.find(s => s.id === selectedId);
        if (chosen) {
          state.songLyricsOverride = chosen.id;
          activateLyricSource(chosen);
        }
      };
    }

    // Choose initial active source:
    let initialSource = null;
    if (state.songLyricsOverride) {
      initialSource = available.find(s => s.id === state.songLyricsOverride);
    }
    if (!initialSource) {
      initialSource = available[0];
    }

    if (selectSourceEl) {
      selectSourceEl.value = initialSource.id;
    }
    activateLyricSource(initialSource);
  }

  function activateLyricSource(sourceObj) {
    if (!sourceObj) return;
    if (sourceObj.isSynced && sourceObj.lines) {
      state.activeLyrics = sourceObj.lines;
      state.currentLyricIndex = -1;
      renderLyricsLines(state.activeLyrics);
    } else if (sourceObj.plain) {
      state.activeLyrics = [];
      state.currentLyricIndex = -1;
      renderPlainLyrics(sourceObj.plain);
    }
  }

  function renderPlainLyrics(text) {
    if (!lyricsContainer) return;
    lyricsContainer.innerHTML = `
      <div class="plain-lyrics" style="white-space: pre-line; line-height: 2; font-size: 1.25rem; color: #ffffff; font-weight: 600; padding: 10px 0;">
        ${text}
      </div>
    `;
  }

  function parseBetterLyricsTTML(ttml) {
    const lines = [];
    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(ttml, "text/xml");
      const ps = doc.querySelectorAll("p");
      ps.forEach(p => {
        const begin = p.getAttribute("begin") || "";
        const text = p.textContent.trim();
        if (begin && text) {
          const time = parseTimeString(begin);
          lines.push({ time, text });
        }
      });
    } catch (_) {}
    return lines.sort((a, b) => a.time - b.time);
  }

  function parseTimeString(str) {
    if (!str) return 0;
    const parts = str.split(":");
    if (parts.length === 2) {
      return parseFloat(parts[0]) * 60 + parseFloat(parts[1]);
    } else if (parts.length === 3) {
      return parseFloat(parts[0]) * 3600 + parseFloat(parts[1]) * 60 + parseFloat(parts[2]);
    }
    return parseFloat(str) || 0;
  }

  function parseLrc(lrcText) {
    const lines = lrcText.split("\n");
    const result = [];
    const timeReg = /\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]/;
    for (const line of lines) {
      const match = timeReg.exec(line);
      if (match) {
        const min = parseInt(match[1]);
        const sec = parseInt(match[2]);
        const ms = match[3] ? parseInt(match[3].padEnd(3, '0').substring(0, 3)) : 0;
        const time = min * 60 + sec + ms / 1000;
        const text = line.replace(timeReg, "").trim();
        if (text) {
          result.push({ time, text });
        }
      }
    }
    return result.sort((a, b) => a.time - b.time);
  }

  function renderLyricsLines(lyrics) {
    if (!lyricsContainer) return;
    lyricsContainer.innerHTML = lyrics.map((l, i) => `
      <div class="lyric-line" data-line-idx="${i}" data-time="${l.time}">
        ${l.text}
      </div>
    `).join('');

    lyricsContainer.querySelectorAll(".lyric-line").forEach(lineEl => {
      lineEl.addEventListener("click", () => {
        const t = parseFloat(lineEl.getAttribute("data-time"));
        if (!isNaN(t) && ytPlayer && ytReady && typeof ytPlayer.seekTo === "function") {
          ytPlayer.seekTo(t, true);
        }
      });
    });
  }

  function syncLyricsWithTime(currentTime) {
    if (!lyricsContainer || state.activeLyrics.length === 0) return;
    let activeIdx = -1;
    for (let i = 0; i < state.activeLyrics.length; i++) {
      if (currentTime >= state.activeLyrics[i].time - 0.15) {
        activeIdx = i;
      } else {
        break;
      }
    }

    if (activeIdx !== state.currentLyricIndex) {
      state.currentLyricIndex = activeIdx;
      const lines = lyricsContainer.querySelectorAll(".lyric-line");
      let activeEl = null;
      lines.forEach((el, idx) => {
        if (idx === activeIdx) {
          el.classList.add("active");
          activeEl = el;
        } else {
          el.classList.remove("active");
        }
      });

      if (activeEl) {
        const containerH = lyricsContainer.clientHeight;
        const targetScroll = activeEl.offsetTop - (containerH / 2) + (activeEl.offsetHeight / 2);
        lyricsContainer.scrollTo({
          top: Math.max(0, targetScroll),
          behavior: "smooth"
        });
      }
    }
  }

  // --- 7. View Toggles (Player View & Sidebar) ---
  function showPlayerView() {
    if (!musicPlayerView || !mainCanvas) return;
    musicPlayerView.classList.remove("hidden");
    pageContent.classList.add("hidden");
    if (btnTogglePlayerView) btnTogglePlayerView.classList.add("active");
  }

  function hidePlayerView() {
    if (!musicPlayerView || !mainCanvas) return;
    musicPlayerView.classList.add("hidden");
    pageContent.classList.remove("hidden");
    if (btnTogglePlayerView) btnTogglePlayerView.classList.remove("active");
  }

  function togglePlayerView() {
    if (!musicPlayerView) return;
    if (musicPlayerView.classList.contains("hidden")) {
      showPlayerView();
    } else {
      hidePlayerView();
    }
  }

  function toggleSidebar() {
    if (!sidebarLeft) return;
    const isCollapsed = sidebarLeft.classList.toggle("collapsed");
    localStorage.setItem("spectro_sidebar_collapsed", isCollapsed ? "true" : "false");
  }

  function toggleQueueSidebar() {
    if (!sidebarRight) return;
    const isHidden = sidebarRight.classList.toggle("hidden");
    if (btnToggleQueue) btnToggleQueue.classList.toggle("active", !isHidden);
  }

  // --- 8. InnerTube Parsing Utilities ---
  function extractInnerTubeTracks(response) {
    const results = [];
    const seenIds = new Set();
    const badges = new Set(["Lagu", "Song", "Video", "Single", "Album", "EP", "Artis", "Artist"]);

    function walk(obj) {
      if (!obj || typeof obj !== "object") return;

      // 1. Top result card (musicCardShelfRenderer)
      if (obj.musicCardShelfRenderer) {
        const card = obj.musicCardShelfRenderer;
        const videoId = card.title?.runs?.[0]?.navigationEndpoint?.watchEndpoint?.videoId ||
                        card.onTap?.watchEndpoint?.videoId ||
                        card.buttons?.[0]?.buttonRenderer?.navigationEndpoint?.watchEndpoint?.videoId;
        if (videoId && !seenIds.has(videoId)) {
          seenIds.add(videoId);
          const title = card.title?.runs?.[0]?.text || "Top Result";
          const subtitleRuns = card.subtitle?.runs || [];
          const texts = subtitleRuns.map(r => r.text).filter(t => t !== " • " && t.trim().length > 0);
          const filtered = texts.filter(t => !badges.has(t));
          const artist = filtered[0] || "Artis YouTube";
          const album = filtered[1] || "Single";
          let art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
          const thumbs = card.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails;
          if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;
          results.push({ id: videoId, videoId, title, artist, album, duration: 0, art });
        }
      }

      // 2. List items (musicResponsiveListItemRenderer)
      if (obj.musicResponsiveListItemRenderer) {
        const item = obj.musicResponsiveListItemRenderer;
        const videoId = item.playlistItemData?.videoId || 
                        item.navigationEndpoint?.watchEndpoint?.videoId ||
                        item.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;

        if (videoId && !seenIds.has(videoId)) {
          seenIds.add(videoId);
          const flexCols = item.flexColumns || [];
          const title = flexCols[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || "Lagu";
          
          let artist = "Artis YouTube";
          let album = "Single";
          if (flexCols.length > 1) {
            const runs = flexCols[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
            const texts = runs.map(r => r.text).filter(t => t !== " • " && t.trim().length > 0);
            const filtered = texts.filter(t => !badges.has(t));
            if (filtered.length > 0) artist = filtered[0];
            if (filtered.length > 1) album = filtered[1];
          }

          let art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
          const thumbs = item.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails;
          if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;

          results.push({ id: videoId, videoId, title, artist, album, duration: 0, art });
        }
      }

      // 3. Two row items (musicTwoRowItemRenderer)
      if (obj.musicTwoRowItemRenderer) {
        const item = obj.musicTwoRowItemRenderer;
        const videoId = item.navigationEndpoint?.watchEndpoint?.videoId ||
                        item.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;

        if (videoId && !seenIds.has(videoId)) {
          seenIds.add(videoId);
          const title = item.title?.runs?.[0]?.text || "Lagu";
          let artist = item.subtitle?.runs?.[0]?.text || "Artis YouTube";
          let album = item.subtitle?.runs?.[2]?.text || "Single";
          let art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
          const thumbs = item.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails;
          if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;

          results.push({ id: videoId, videoId, title, artist, album, duration: 0, art });
        }
      }

      for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) walk(obj[key]);
      }
    }

    walk(response);
    return results;
  }

  // --- 9. Beranda / Home Page (Real Live YouTube Music Browse) ---
  function getGreeting() {
    const hr = new Date().getHours();
    if (hr >= 4 && hr < 11) return "Selamat Pagi";
    if (hr >= 11 && hr < 15) return "Selamat Siang";
    if (hr >= 15 && hr < 18) return "Selamat Sore";
    return "Selamat Malam";
  }

  async function renderHome(pushHistory = true) {
    hidePlayerView();
    state.activePage = "home";
    if (pushHistory) pushNav("home");

    const recentlyPlayed = getRecentlyPlayed();
    const recentHtml = recentlyPlayed.length > 0 ? `
      <div class="section-header" style="margin-top: 14px; margin-bottom: 12px;">
        <h2 class="section-title">Baru Saja Diputar</h2>
      </div>
      <div class="section-grid" style="grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; margin-bottom: 24px;" id="recent-tracks-container">
        ${recentlyPlayed.slice(0, 6).map(t => `
          <div class="music-card recent-track-card" data-video-id="${t.videoId || t.id}" title="${escapeHtml(t.title)} - ${escapeHtml(t.artist)}">
            <div class="card-img-wrapper">
              <img src="${t.art || 'logo.png'}" alt="${escapeHtml(t.title)}" onerror="this.src='logo.png'">
              <div class="card-play-overlay">
                <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
              </div>
            </div>
            <div class="card-title">${escapeHtml(cleanDisplayTitle(t.title))}</div>
            <div class="card-subtitle">${escapeHtml(t.artist)}</div>
          </div>
        `).join('')}
      </div>
    ` : '';

    // Immediate initial layout with greeting and skeletons (100% pure live YouTube Music)
    pageContent.innerHTML = `
      <h1 class="page-greeting">${getGreeting()}</h1>
      ${recentHtml}
      
      <!-- Quick Picks Grid with Shimmer Skeleton -->
      <div class="quick-picks-grid" id="home-quick-picks">
        ${Array(6).fill(0).map(() => `
          <div class="quick-card skeleton-card" style="opacity: 0.55; pointer-events: none;">
            <div style="width: 56px; height: 56px; background: rgba(255,255,255,0.08); border-radius: 4px;"></div>
            <div style="width: 55%; height: 14px; background: rgba(255,255,255,0.08); border-radius: 4px;"></div>
          </div>
        `).join('')}
      </div>

      <!-- Live Dynamic Sections Container -->
      <div id="home-dynamic-sections">
        <div class="section-header">
          <h2 class="section-title">Memuat Rekomendasi Musik...</h2>
        </div>
        <div class="section-grid">
          ${Array(6).fill(0).map(() => `
            <div class="music-card" style="opacity: 0.55; pointer-events: none;">
              <div class="card-img-wrapper" style="background: rgba(255,255,255,0.08); aspect-ratio: 1/1;"></div>
              <div style="width: 70%; height: 14px; background: rgba(255,255,255,0.08); border-radius: 4px; margin-top: 10px;"></div>
              <div style="width: 45%; height: 12px; background: rgba(255,255,255,0.05); border-radius: 4px; margin-top: 6px;"></div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    // Attach click listener to recently played cards
    document.querySelectorAll(".recent-track-card").forEach(card => {
      card.addEventListener("click", () => {
        const vid = card.getAttribute("data-video-id");
        const found = recentlyPlayed.find(t => (t.videoId || t.id) === vid);
        if (found) {
          playTrackWithRadio(found);
        }
      });
    });

    // Fetch real live browse data from YouTube Music FEmusic_home & FEmusic_explore in parallel
    try {
      const [rawHome, rawExplore] = await Promise.all([
        invokeTauri("get_home").catch(() => null),
        invokeTauri("get_explore").catch(() => null)
      ]);

      let homeData = null;
      let exploreData = null;
      try { if (rawHome) homeData = JSON.parse(rawHome); } catch (_) {}
      try { if (rawExplore) exploreData = JSON.parse(rawExplore); } catch (_) {}

      if (homeData || exploreData) {
        try {
          localStorage.setItem("spectro_home_cache", JSON.stringify({ home: rawHome, explore: rawExplore }));
        } catch (_) {}
        parseAndRenderLiveHome(homeData, exploreData);
        return;
      }
    } catch (e) {
      console.warn("Live get_home error:", e);
    }

    // Fallback 1: Try cached home data if available
    try {
      const stored = localStorage.getItem("spectro_home_cache");
      if (stored) {
        const cached = JSON.parse(stored);
        const hData = cached.home ? JSON.parse(cached.home) : null;
        const eData = cached.explore ? JSON.parse(cached.explore) : null;
        if (hData || eData) {
          parseAndRenderLiveHome(hData, eData);
          return;
        }
      }
    } catch (_) {}

    // Fallback 2: Guaranteed search fallback for popular hits so songs are ALWAYS shown
    try {
      const rawSearch = await invokeTauri("search_music", { query: "Top Hits Indonesia Populer" });
      if (rawSearch) {
        const searchJson = JSON.parse(rawSearch);
        const songs = extractInnerTubeTracks(searchJson);
        if (songs && songs.length > 0) {
          renderFallbackHome(songs);
          return;
        }
      }
    } catch (_) {}

    // Fallback 3: Hardcoded popular hits so user NEVER sees a failed or empty screen
    if (typeof DEFAULT_INITIAL_HITS !== "undefined" && DEFAULT_INITIAL_HITS.length > 0) {
      renderFallbackHome(DEFAULT_INITIAL_HITS);
      return;
    }

    showHomeOfflineNotice();
  }

  function renderFallbackHome(songs) {
    const quickPicks = document.getElementById("home-quick-picks");
    if (quickPicks) {
      quickPicks.innerHTML = songs.slice(0, 6).map(t => {
        const art = t.art || (t.videoId ? `https://i.ytimg.com/vi/${t.videoId}/hqdefault.jpg` : "logo.png");
        return `
          <div class="quick-card home-fallback-card" data-video-id="${t.videoId || t.id}">
            <img src="${art}" alt="${escapeHtml(t.title)}" onerror="this.src='logo.png'">
            <span>${escapeHtml(cleanDisplayTitle(t.title))}</span>
          </div>
        `;
      }).join('');
    }

    const dynamicSections = document.getElementById("home-dynamic-sections");
    if (dynamicSections) {
      const popularTracks = songs.slice(0, 10);
      const recommendedTracks = songs.slice(10, 20);

      const renderSection = (title, list) => `
        <div class="section-header" style="margin-top: 20px;">
          <h2 class="section-title">${title}</h2>
        </div>
        <div class="section-grid">
          ${list.map(t => {
            const art = t.art || (t.videoId ? `https://i.ytimg.com/vi/${t.videoId}/hqdefault.jpg` : "logo.png");
            return `
              <div class="music-card home-fallback-card" data-video-id="${t.videoId || t.id}" title="${escapeHtml(t.title)} - ${escapeHtml(t.artist)}">
                <div class="card-img-wrapper">
                  <img src="${art}" alt="${escapeHtml(t.title)}" onerror="this.src='logo.png'">
                  <div class="card-play-overlay">
                    <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  </div>
                </div>
                <div class="card-title">${escapeHtml(cleanDisplayTitle(t.title))}</div>
                <div class="card-subtitle">${escapeHtml(t.artist)}</div>
              </div>
            `;
          }).join('')}
        </div>
      `;

      dynamicSections.innerHTML = `
        ${renderSection("Lagu Populer & Viral", popularTracks)}
        ${recommendedTracks.length > 0 ? renderSection("Rekomendasi Teratas", recommendedTracks) : ""}
      `;
    }

    document.querySelectorAll(".home-fallback-card").forEach(el => {
      el.addEventListener("click", () => {
        const vid = el.getAttribute("data-video-id");
        const found = songs.find(t => (t.videoId || t.id) === vid);
        if (found) {
          playTrackWithRadio(found);
        }
      });
    });
  }

  function showHomeOfflineNotice() {
    const dynamicContainer = document.getElementById("home-dynamic-sections");
    if (dynamicContainer) {
      dynamicContainer.innerHTML = `
        <div style="padding: 40px 16px; text-align: center; color: var(--text-secondary);">
          <p style="font-size: 1.1rem; color: #fff; margin-bottom: 8px;">Gagal memuat rekomendasi YouTube Music</p>
          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 16px;">Pastikan koneksi internet aktif atau coba muat ulang.</p>
          <button id="btn-retry-home" class="playlist-play-all-btn" style="width: auto; padding: 10px 24px; border-radius: 500px; font-size: 0.85rem; cursor: pointer;">Coba Lagi</button>
        </div>
      `;
      const btnRetry = document.getElementById("btn-retry-home");
      if (btnRetry) btnRetry.addEventListener("click", () => renderHome());
    }
  }

  function parseAndRenderLiveHome(homeData, exploreData) {
    const sectionsHtml = [];
    const allTracksCollected = [];
    const trackMap = new Map();
    const playlistMap = new Map();

    function processShelf(shelf, defaultTitle) {
      if (!shelf) return;
      const title = shelf.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.[0]?.text ||
                    shelf.title?.runs?.[0]?.text || defaultTitle;
      const items = shelf.contents || [];
      const sectionTracks = [];
      const sectionPlaylists = [];

      items.forEach(it => {
        const itemTwoRow = it.musicTwoRowItemRenderer;
        const itemResp = it.musicResponsiveListItemRenderer;

        if (itemTwoRow) {
          const videoId = itemTwoRow.navigationEndpoint?.watchEndpoint?.videoId ||
                          itemTwoRow.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;
          const browseId = itemTwoRow.navigationEndpoint?.browseEndpoint?.browseId;
          const t = itemTwoRow.title?.runs?.[0]?.text || "Lagu";
          const a = itemTwoRow.subtitle?.runs?.[0]?.text || "YouTube Music";
          let art = "logo.png";
          const thumbs = itemTwoRow.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails ||
                         itemTwoRow.thumbnail?.thumbnails ||
                         itemTwoRow.thumbnailRenderer?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails;
          if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;
          if ((!art || art === "logo.png") && videoId) art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;

          if (videoId) {
            sectionTracks.push({ id: videoId, videoId, title: t, artist: a, album: "Album", art });
          } else if (browseId) {
            sectionPlaylists.push({ browseId, title: t, subtitle: a, art });
          }
        } else if (itemResp) {
          const videoId = itemResp.playlistItemData?.videoId ||
                          itemResp.navigationEndpoint?.watchEndpoint?.videoId ||
                          itemResp.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;
          if (videoId) {
            const t = itemResp.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || "Lagu";
            const a = itemResp.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || "Artis YouTube";
            let art = `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
            const thumbs = itemResp.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails ||
                           itemResp.thumbnail?.thumbnails;
            if (thumbs && thumbs.length > 0) art = thumbs[thumbs.length - 1].url;
            sectionTracks.push({ id: videoId, videoId, title: t, artist: a, album: "Album", art });
          }
        }
      });

      if (sectionTracks.length > 0) {
        sectionTracks.forEach(t => {
          allTracksCollected.push(t);
          trackMap.set(t.videoId, t);
        });
        sectionsHtml.push(`
          <div class="section-header">
            <h2 class="section-title">${title}</h2>
          </div>
          <div class="section-grid">
            ${sectionTracks.slice(0, 10).map(t => `
              <div class="music-card" data-live-id="${t.videoId}">
                <div class="card-img-wrapper">
                  <img src="${t.art}" onerror="this.src='logo.png'" alt="${t.title}">
                  <button class="card-play-btn" title="Putar">
                    <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  </button>
                </div>
                <div class="card-title">${cleanDisplayTitle(t.title)}</div>
                <div class="card-subtitle">${t.artist}</div>
              </div>
            `).join('')}
          </div>
        `);
      } else if (sectionPlaylists.length > 0) {
        sectionPlaylists.forEach(p => playlistMap.set(p.browseId, p));
        sectionsHtml.push(`
          <div class="section-header">
            <h2 class="section-title">${title}</h2>
          </div>
          <div class="section-grid">
            ${sectionPlaylists.slice(0, 10).map(p => `
              <div class="music-card" data-live-browse-id="${p.browseId}">
                <div class="card-img-wrapper">
                  <img src="${p.art}" onerror="this.src='logo.png'" alt="${p.title}">
                  <button class="card-play-btn" title="Buka Playlist">
                    <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  </button>
                </div>
                <div class="card-title">${cleanDisplayTitle(p.title)}</div>
                <div class="card-subtitle">${p.subtitle}</div>
              </div>
            `).join('')}
          </div>
        `);
      }
    }

    // Process Home Sections
    if (homeData) {
      const homeTabs = homeData?.contents?.singleColumnBrowseResultsRenderer?.tabs;
      const homeContents = homeTabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];
      homeContents.forEach((sec, idx) => {
        const shelf = sec.musicCarouselShelfRenderer || sec.musicShelfRenderer;
        processShelf(shelf, idx === 0 ? "Pilihan Cepat" : "Rekomendasi YouTube Music");
      });
    }

    // Process Explore Sections (Trending, Album & Single Baru)
    if (exploreData) {
      const exploreTabs = exploreData?.contents?.singleColumnBrowseResultsRenderer?.tabs;
      const exploreContents = exploreTabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];
      exploreContents.forEach(sec => {
        const shelf = sec.musicCarouselShelfRenderer || sec.musicShelfRenderer;
        processShelf(shelf, "Trending & Rilisan Baru");
      });
    }

    const dynamicContainer = document.getElementById("home-dynamic-sections");
    const quickPicksContainer = document.getElementById("home-quick-picks");

    // Populate Top Quick Picks Grid
    if (quickPicksContainer && allTracksCollected.length > 0) {
      const top6 = allTracksCollected.slice(0, 6);
      quickPicksContainer.innerHTML = top6.map(t => `
        <div class="quick-card" data-live-id="${t.videoId}">
          <img src="${t.art}" onerror="this.src='logo.png'" alt="${t.title}" class="quick-card-art">
          <span class="quick-card-title">${cleanDisplayTitle(t.title)}</span>
          <button class="card-play-btn" title="Putar">
            <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
          </button>
        </div>
      `).join('');

      quickPicksContainer.querySelectorAll("[data-live-id]").forEach(el => {
        el.addEventListener("click", () => {
          const id = el.getAttribute("data-live-id");
          const trk = trackMap.get(id);
          if (trk) playTrackWithRadio(trk);
        });
      });
    }

    // Populate Dynamic Section Grids
    if (dynamicContainer && sectionsHtml.length > 0) {
      dynamicContainer.innerHTML = sectionsHtml.join('');

      dynamicContainer.querySelectorAll("[data-live-id]").forEach(el => {
        el.addEventListener("click", () => {
          const id = el.getAttribute("data-live-id");
          const trk = trackMap.get(id);
          if (trk) playTrackWithRadio(trk);
        });
      });

      dynamicContainer.querySelectorAll("[data-live-browse-id]").forEach(el => {
        el.addEventListener("click", () => {
          const browseId = el.getAttribute("data-live-browse-id");
          const p = playlistMap.get(browseId);
          if (p) openPlaylist(browseId, p.title, p.art);
        });
      });
    }
  }

  function bindHomeCardEvents(tracks) {
    pageContent.querySelectorAll("[data-home-idx]").forEach(el => {
      el.addEventListener("click", () => {
        const idx = parseInt(el.getAttribute("data-home-idx"));
        if (tracks[idx]) playTrackWithRadio(tracks[idx]);
      });
    });
  }

  // --- 10. Search Execution & Results Rendering ---
  let activeSearchRequestId = 0;

  async function performSearch(query) {
    if (!query || !query.trim()) return;
    query = query.trim();
    hidePlayerView();
    state.activePage = "search";
    const thisRequestId = ++activeSearchRequestId;

    pageContent.innerHTML = `
      <div style="padding: 24px;">
        <h2 class="section-title mb-3">Mencari: "${query}"...</h2>
        <div style="display: flex; align-items: center; gap: 12px; color: var(--text-secondary); margin-top: 16px;">
          <svg style="width: 24px; height: 24px; animation: spin 1s linear infinite;" viewBox="0 0 24 24"><path fill="currentColor" d="M12 4V2A10 10 0 0 0 2 12h2a8 8 0 0 1 8-8z"/></svg>
          <span>Mengambil katalog resmi YouTube Music...</span>
        </div>
      </div>
      <style>@keyframes spin { 100% { transform: rotate(360deg); } }</style>
    `;

    try {
      const searchPromise = invokeTauri("search_music", { query });
      const timeoutPromise = new Promise((_, reject) => setTimeout(() => reject(new Error("Timeout")), 8000));
      const rawJson = await Promise.race([searchPromise, timeoutPromise]);

      if (thisRequestId !== activeSearchRequestId) return;

      if (rawJson) {
        const parsed = JSON.parse(rawJson);
        const songs = extractInnerTubeTracks(parsed);
        state.searchResults = songs;
        renderSearchResults(query, songs);
        return;
      }
    } catch (e) {
      console.warn("Search error:", e);
    }

    if (thisRequestId === activeSearchRequestId) {
      renderSearchResults(query, []);
    }
  }

  function renderSearchResults(query, tracks) {
    if (!tracks || tracks.length === 0) {
      pageContent.innerHTML = `
        <div style="padding: 24px;">
          <h2 class="section-title mb-3">Hasil Pencarian: "${query}"</h2>
          <p style="color: var(--text-secondary); margin-top: 12px;">Tidak ada musik yang ditemukan untuk "${query}". Coba kata kunci lain.</p>
        </div>
      `;
      return;
    }

    const topResult = tracks[0];

    pageContent.innerHTML = `
      <div style="padding-bottom: 32px;">
        <h2 class="section-title mb-3">Hasil Pencarian: "${query}"</h2>
        
        <!-- Top Result Card -->
        <div style="margin-bottom: 24px; background: rgba(255,255,255,0.05); padding: 20px; border-radius: 8px; display: flex; align-items: center; gap: 20px; cursor: pointer;" id="top-result-card">
          <img src="${topResult.art}" onerror="this.src='logo.png'" alt="${topResult.title}" style="width: 80px; height: 80px; border-radius: 6px; object-fit: cover;">
          <div style="flex: 1; overflow: hidden;">
            <div style="font-size: 0.8rem; font-weight: 700; text-transform: uppercase; color: #1db954; margin-bottom: 4px;">Hasil Teratas</div>
            <div style="font-size: 1.4rem; font-weight: 800; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${cleanDisplayTitle(topResult.title)}</div>
            <div style="font-size: 0.9rem; color: var(--text-secondary); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${topResult.artist} • ${topResult.album}</div>
          </div>
          <button class="playlist-play-all-btn" title="Putar Sekarang" style="width: 44px; height: 44px; flex-shrink: 0;">
            <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
          </button>
        </div>

        <h3 class="section-title" style="font-size: 1.1rem; margin-bottom: 12px;">Lagu Terkait</h3>
        <table class="track-table">
          <thead>
            <tr>
              <th class="track-num">#</th>
              <th>Judul</th>
              <th>Album</th>
              <th style="text-align: right;">Aksi</th>
            </tr>
          </thead>
          <tbody>
            ${tracks.map((t, i) => `
              <tr class="track-row" data-search-idx="${i}">
                <td class="track-num">${i + 1}</td>
                <td>
                  <div class="track-title-cell">
                    <img src="${t.art}" onerror="this.src='logo.png'" alt="${t.title}" class="track-thumb">
                    <div class="track-cell-info">
                      <span class="track-cell-title">${cleanDisplayTitle(t.title)}</span>
                      <span class="track-cell-artist">${t.artist}</span>
                    </div>
                  </div>
                </td>
                <td>${t.album}</td>
                <td style="text-align: right; white-space: nowrap;">
                  <button class="btn-row-add-pl" data-search-row-pl-idx="${i}" style="background:none; border:none; color:var(--text-secondary); cursor:pointer; padding:6px; margin-right:4px;" title="Simpan ke Playlist">
                    <svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M14 10H3v2h11v-2zm0-4H3v2h11V6zm4 8v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM3 16h7v-2H3v2z"/></svg>
                  </button>
                  <button class="card-play-btn" style="position: static; opacity: 1; transform: none; width: 32px; height: 32px;" title="Putar">
                    <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                  </button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

    const topCard = document.getElementById("top-result-card");
    if (topCard) {
      topCard.addEventListener("click", () => playTrackWithRadio(topResult));
    }

    pageContent.querySelectorAll("[data-search-row-pl-idx]").forEach(btn => {
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        const idx = parseInt(btn.getAttribute("data-search-row-pl-idx"));
        if (tracks[idx]) openSaveToPlaylistModal(tracks[idx]);
      });
    });

    pageContent.querySelectorAll("[data-search-idx]").forEach(row => {
      row.addEventListener("click", () => {
        const idx = parseInt(row.getAttribute("data-search-idx"));
        if (tracks[idx]) playTrackWithRadio(tracks[idx]);
      });
    });
  }

  // --- 11. Playlist & Library Viewing (Full YouTube Music Integration) ---
  async function openPlaylist(playlistId, playlistTitle = "Daftar Putar", playlistCover = null, pushHistory = true) {
    hidePlayerView();
    state.activePage = "playlist";
    state.currentPlaylist = playlistId;
    if (pushHistory) pushNav("playlist", { id: playlistId, title: playlistTitle, cover: playlistCover });

    pageContent.innerHTML = `
      <div style="padding: 24px 0;">
        <div class="playlist-hero">
          <img src="${playlistCover || 'logo.png'}" onerror="this.src='logo.png'" alt="${playlistTitle}" class="playlist-hero-cover" id="pl-hero-cover">
          <div class="playlist-hero-info">
            <span class="playlist-hero-type">Daftar Putar</span>
            <h1 class="playlist-hero-title">${playlistTitle}</h1>
            <span class="playlist-hero-meta" id="pl-hero-meta">Memuat lagu...</span>
          </div>
        </div>
        <div class="playlist-actions-bar">
          <button class="playlist-play-all-btn" id="btn-playlist-play-all" title="Putar Semua">
            <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
          </button>
        </div>
        <div id="playlist-tracks-container">
          <p style="color: var(--text-secondary); padding: 20px 0;">Mengambil daftar lagu...</p>
        </div>
      </div>
    `;

    try {
      if (playlistId === "liked" || playlistId === "LM") {
        let tracks = state.likedTracks || [];
        try {
          if (currentSession && currentSession.is_logged_in) {
            const raw = await invokeTauri("get_liked_songs");
            if (raw) {
              const data = JSON.parse(raw);
              const ytTracks = extractInnerTubeTracks(data);
              if (ytTracks && ytTracks.length > 0) {
                tracks = ytTracks;
                state.likedTracks = ytTracks;
                localStorage.setItem("spectro_liked_songs", JSON.stringify(ytTracks));
                updateLikedCountSidebar();
              }
            }
          }
        } catch (_) {}
        renderPlaylistTracks(playlistTitle, tracks);
        return;
      }

      const customPl = (state.customPlaylists || []).find(p => p.id === playlistId);
      if (customPl) {
        renderPlaylistTracks(playlistTitle, customPl.tracks || []);
        return;
      }

      const raw = await invokeTauri("get_playlist_details", { browseId: playlistId });
      if (raw) {
        const data = JSON.parse(raw);
        const tracks = extractInnerTubeTracks(data);
        renderPlaylistTracks(playlistTitle, tracks);
        return;
      }
    } catch (e) {
      console.warn("openPlaylist error:", e);
    }

    renderPlaylistTracks(playlistTitle, []);
  }

  function renderPlaylistTracks(title, tracks) {
    const metaEl = document.getElementById("pl-hero-meta");
    const container = document.getElementById("playlist-tracks-container");
    const btnPlayAll = document.getElementById("btn-playlist-play-all");

    if (metaEl) {
      metaEl.textContent = `Spectro Library • ${tracks.length} lagu`;
    }

    if (btnPlayAll) {
      btnPlayAll.onclick = () => {
        if (tracks.length > 0) {
          state.queue = [...tracks];
          state.currentTrackIndex = 0;
          loadTrack(0, true);
        }
      };
    }

    if (!container) return;

    if (!tracks || tracks.length === 0) {
      container.innerHTML = `
        <div style="padding: 40px 16px; text-align: center; color: var(--text-muted);">
          <p style="font-size: 1rem; color: var(--text-secondary); margin-bottom: 8px;">Daftar putar ini masih kosong atau belum disinkronkan.</p>
          <p style="font-size: 0.85rem;">Gunakan tombol "Simpan ke Playlist" pada lagu yang Anda sukai untuk menambahkannya ke sini.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = `
      <table class="track-table">
        <thead>
          <tr>
            <th class="track-num">#</th>
            <th>Judul</th>
            <th>Album</th>
            <th style="text-align: right;">Aksi</th>
          </tr>
        </thead>
        <tbody>
          ${tracks.map((t, i) => `
            <tr class="track-row" data-pl-track-idx="${i}">
              <td class="track-num">${i + 1}</td>
              <td>
                <div class="track-title-cell">
                  <img src="${t.art}" onerror="this.src='logo.png'" alt="${t.title}" class="track-thumb">
                  <div class="track-cell-info">
                    <span class="track-cell-title">${cleanDisplayTitle(t.title)}</span>
                    <span class="track-cell-artist">${t.artist}</span>
                  </div>
                </div>
              </td>
              <td>${t.album || 'Spectro Music'}</td>
              <td style="text-align: right; white-space: nowrap;">
                <button class="btn-pl-row-add" data-pl-row-idx="${i}" style="background:none; border:none; color:var(--text-secondary); cursor:pointer; padding:6px; margin-right:4px;" title="Simpan ke Playlist">
                  <svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M14 10H3v2h11v-2zm0-4H3v2h11V6zm4 8v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM3 16h7v-2H3v2z"/></svg>
                </button>
                <button class="card-play-btn" style="position: static; opacity: 1; transform: none; width: 32px; height: 32px;" title="Putar">
                  <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                </button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;

    container.querySelectorAll("[data-pl-row-idx]").forEach(btn => {
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        const idx = parseInt(btn.getAttribute("data-pl-row-idx"));
        if (tracks[idx]) openSaveToPlaylistModal(tracks[idx]);
      });
    });

    container.querySelectorAll("[data-pl-track-idx]").forEach(row => {
      row.addEventListener("click", () => {
        const idx = parseInt(row.getAttribute("data-pl-track-idx"));
        if (tracks[idx]) {
          state.queue = [...tracks];
          loadTrack(idx, true);
        }
      });
    });
  }

  // --- 12. User Library & Playlists Sync ---
  async function loadUserPlaylists() {
    renderUserPlaylistsInSidebar(state.userPlaylists || []);

    if (!currentSession.is_logged_in) {
      return;
    }

    try {
      const raw = await invokeTauri("get_user_library");
      if (raw) {
        const data = JSON.parse(raw);
        const playlists = [];
        const seenIds = new Set();

        function walk(obj) {
          if (!obj || typeof obj !== "object") return;
          if (obj.musicTwoRowItemRenderer) {
            const item = obj.musicTwoRowItemRenderer;
            const browseId = item.navigationEndpoint?.browseEndpoint?.browseId;
            if (browseId && (browseId.startsWith("VL") || browseId.startsWith("PL") || browseId.startsWith("RDCLAK")) && !seenIds.has(browseId)) {
              seenIds.add(browseId);
              const title = item.title?.runs?.[0]?.text || "Playlist";
              const count = item.subtitle?.runs?.[item.subtitle?.runs?.length - 1]?.text || "Daftar Putar";
              let cover = "logo.png";
              const thumbs = item.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails;
              if (thumbs && thumbs.length > 0) cover = thumbs[thumbs.length - 1].url;
              playlists.push({ id: browseId, title, count, cover });
            }
          }
          for (const k in obj) {
            if (Object.prototype.hasOwnProperty.call(obj, k)) walk(obj[k]);
          }
        }

        walk(data);
        state.userPlaylists = playlists;
        renderUserPlaylistsInSidebar(playlists);
      }
    } catch (e) {
      console.warn("loadUserPlaylists error:", e);
    }
  }

  function renderUserPlaylistsInSidebar(playlists = []) {
    if (!userCustomPlaylists) return;
    const combined = [
      ...(state.customPlaylists || []).map(p => ({
        id: p.id,
        title: p.title,
        count: `${(p.tracks || []).length} lagu`,
        cover: p.cover || 'logo.png'
      })),
      ...(playlists || [])
    ];

    if (combined.length === 0) {
      userCustomPlaylists.innerHTML = `
        <div id="btn-sidebar-login-prompt" style="padding: 12px; font-size: 0.8rem; color: var(--text-muted); line-height: 1.4; cursor: pointer; border-radius: 6px; transition: all 0.2s;" onmouseover="this.style.color='#00e5ff'; this.style.background='rgba(0,229,255,0.06)'" onmouseout="this.style.color='var(--text-muted)'; this.style.background='transparent'">
          ${currentSession.is_logged_in ? 'Belum ada playlist di akun kamu.' : 'Masuk Akun Google untuk sinkron playlist.'}
        </div>
      `;
      const prompt = document.getElementById("btn-sidebar-login-prompt");
      if (prompt && !currentSession.is_logged_in) {
        prompt.addEventListener("click", () => {
          const modal = document.getElementById("login-modal");
          if (modal) modal.classList.remove("hidden");
        });
      }
      return;
    }

    userCustomPlaylists.innerHTML = combined.map(pl => `
      <div class="playlist-item" data-custom-pl-id="${pl.id}" title="${escapeHtml(pl.title)}">
        <div class="playlist-icon vinyl-gradient">
          <img src="${pl.cover}" onerror="this.src='logo.png'" alt="${escapeHtml(pl.title)}" style="width: 100%; height: 100%; object-fit: cover; border-radius: 4px;">
        </div>
        <div class="playlist-info">
          <span class="playlist-name">${escapeHtml(pl.title)}</span>
          <span class="playlist-meta">${pl.count}</span>
        </div>
      </div>
    `).join('');

    userCustomPlaylists.querySelectorAll("[data-custom-pl-id]").forEach(el => {
      el.addEventListener("click", () => {
        const id = el.getAttribute("data-custom-pl-id");
        const pl = combined.find(p => p.id === id);
        if (pl) openPlaylist(pl.id, pl.title, pl.cover);
      });
    });
  }

  // --- 13. Explore & Radio Pages ---
  async function renderExplore(pushHistory = true) {
    hidePlayerView();
    state.activePage = "explore";
    if (pushHistory) pushNav("explore");

    pageContent.innerHTML = `
      <div style="padding: 24px;">
        <h2 class="section-title mb-3">Jelajahi Genre & Suasana</h2>
        <div class="section-grid">
          ${[
            { name: "Pop Indonesia", bg: "#4776E6" },
            { name: "Indie Pop", bg: "#8E54E9" },
            { name: "Hits Viral Top 50", bg: "#FF416C" },
            { name: "R&B / Soul", bg: "#F09819" },
            { name: "Akustik Santai", bg: "#11998e" },
            { name: "Rock & Alternatif", bg: "#ED213A" },
            { name: "K-Pop Terkini", bg: "#93291E" },
            { name: "Lo-Fi & Chillhop", bg: "#38ef7d" }
          ].map(g => `
            <div class="music-card" data-genre="${g.name}" style="cursor: pointer; background: ${g.bg}; padding: 24px; border-radius: 8px; min-height: 100px; display: flex; flex-direction: column; justify-content: flex-end;">
              <h3 style="font-size: 1.25rem; font-weight: 800; color: #fff; line-height: 1.2;">${g.name}</h3>
              <p style="font-size: 0.8rem; color: rgba(255,255,255,0.85); margin-top: 4px;">Putar radio rekomendasi</p>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    pageContent.querySelectorAll("[data-genre]").forEach(el => {
      el.addEventListener("click", () => {
        const g = el.getAttribute("data-genre");
        globalSearchInput.value = g;
        performSearch(g);
      });
    });
  }

  const RADIO_GENRES = [
    { title: "Pop Indonesia", desc: "Hits pop terbaik dan terpopuler tanah air", query: "Hits Pop Indonesia", icon: "🇮🇩" },
    { title: "Hits Global & Barat", desc: "Tangga lagu top global terhangat saat ini", query: "Top Global Hits", icon: "🌍" },
    { title: "Indie Nusantara", desc: "Alunan musik indie peneduh suasana", query: "Indie Folk Indonesia", icon: "☕" },
    { title: "Akustik Santai", desc: "Lagu-lagu santai untuk fokus dan relaksasi", query: "Acoustic Pop Relaxing", icon: "🎸" },
    { title: "Dangdut Koplo Terbaru", desc: "Irama dangdut koplo hits dan viral", query: "Dangdut Koplo Viral", icon: "🥁" },
    { title: "Jazz & Coffee Vibes", desc: "Melodi jazz elegan pendamping hari", query: "Smooth Jazz Relaxing", icon: "🎷" },
    { title: "Rock Classics & Modern", desc: "Energi rock bertenaga lintas generasi", query: "Rock Hits Classic Modern", icon: "⚡" },
    { title: "Hip-Hop & R&B", desc: "Aliran beat hip-hop & r&b terkini", query: "Hip Hop R&B Hits", icon: "🎧" }
  ];

  function renderRadios(pushHistory = true) {
    hidePlayerView();
    state.activePage = "radios";
    if (pushHistory) pushNav("radios");

    pageContent.innerHTML = `
      <div style="padding: 24px;">
        <h2 class="section-title mb-3">Radio Online & Infinite Flow</h2>
        <p style="color: var(--text-secondary); margin-bottom: 24px;">Pilih stasiun radio untuk mendengarkan aliran lagu YouTube Music tanpa henti secara instan.</p>
        <div class="section-grid">
          ${RADIO_GENRES.map((g, idx) => `
            <div class="music-card" data-radio-genre-idx="${idx}">
              <div class="card-img-wrapper" style="background: linear-gradient(135deg, rgba(0, 240, 255, 0.15), rgba(123, 44, 191, 0.25)); display: flex; align-items: center; justify-content: center; font-size: 2.8rem;">
                <span>${g.icon}</span>
                <button class="card-play-btn" title="Mulai Radio">
                  <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z"/></svg>
                </button>
              </div>
              <div class="card-title">${g.title}</div>
              <div class="card-subtitle">${g.desc}</div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    pageContent.querySelectorAll("[data-radio-genre-idx]").forEach(el => {
      el.addEventListener("click", () => {
        const idx = parseInt(el.getAttribute("data-radio-genre-idx"));
        const g = RADIO_GENRES[idx];
        if (g) {
          if (globalSearchInput) globalSearchInput.value = g.query;
          performSearch(g.query);
        }
      });
    });
  }

  // --- 14. Auth Handling ---
  async function initAuth() {
    try {
      const raw = await invokeTauri("get_session");
      if (raw) {
        const data = JSON.parse(raw);
        applySession(data);
      }
    } catch (e) {
      console.warn("initAuth error:", e);
    }

    if (window.__TAURI__ && window.__TAURI__.event) {
      window.__TAURI__.event.listen("session_updated", (event) => {
        try {
          const data = typeof event.payload === "string" ? JSON.parse(event.payload) : event.payload;
          applySession(data);
        } catch (_) {}
      });
    }
  }

  function applySession(session) {
    currentSession = session || { is_logged_in: false };
    const btnLogin = document.getElementById("btn-login");
    const userBadge = document.getElementById("user-badge");
    const userAvatarImg = document.getElementById("user-avatar-img");
    const userAvatarInitial = document.getElementById("user-avatar-initial");
    const userName = document.getElementById("user-name");
    const dropdownName = document.getElementById("dropdown-user-name");
    const dropdownHandle = document.getElementById("dropdown-user-handle");

    if (currentSession.is_logged_in) {
      if (btnLogin) btnLogin.classList.add("hidden");
      if (userBadge) userBadge.classList.remove("hidden");
      if (userName) userName.textContent = currentSession.name || "Akun Google";
      if (dropdownName) dropdownName.textContent = currentSession.name || "Akun Google";
      if (dropdownHandle) dropdownHandle.textContent = currentSession.handle || "@google";

      if (currentSession.photo_url && userAvatarImg) {
        userAvatarImg.src = currentSession.photo_url;
        userAvatarImg.classList.remove("hidden");
        if (userAvatarInitial) userAvatarInitial.classList.add("hidden");
      } else if (userAvatarInitial) {
        userAvatarInitial.textContent = (currentSession.name || "U").substring(0, 2).toUpperCase();
        userAvatarInitial.classList.remove("hidden");
        if (userAvatarImg) userAvatarImg.classList.add("hidden");
      }

      loadUserPlaylists();
    } else {
      if (btnLogin) btnLogin.classList.remove("hidden");
      if (userBadge) userBadge.classList.add("hidden");
      loadUserPlaylists();
    }
  }

  // --- 15. Spectro Connect & Cross-Device Account Pairing ---
  let mqttClient = null;
  const MQTT_TOPIC = "spectro/connect/sync";
  const MQTT_CLIENT_ID = "spectro_win_" + Math.random().toString(16).substring(2, 10);
  let activePairingCode = null;

  function escapeHtml(str) {
    return (str || "").replace(/[&<>'"]/g, tag => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;"
    }[tag] || tag));
  }

  async function handlePairingSyncSuccess(payload) {
    const statusBox = document.getElementById("sync-status-indicator");
    const statusText = document.getElementById("sync-status-text");
    if (statusBox) {
      statusBox.style.background = "rgba(46, 213, 115, 0.12)";
      statusBox.style.borderColor = "rgba(46, 213, 115, 0.4)";
      statusBox.style.color = "#2ed573";
    }
    if (statusText) {
      const uName = payload.name || "Akun Google";
      statusText.innerHTML = `&#10003; Berhasil tersambung dengan <b>${escapeHtml(uName)}</b>! Menyimpan...`;
    }

    try {
      if (payload.device) {
        localStorage.setItem("spectro_connected_device", payload.device);
      }
      await invokeTauri("save_session", { cookies: payload.cookies });
      await initAuth();

      if (mqttClient && mqttClient.isConnected() && payload.code) {
        try {
          const ack = new Paho.MQTT.Message(JSON.stringify({
            status: "ok",
            device: "Spectro Windows (Desktop PC)",
            timestamp: Date.now()
          }));
          ack.destinationName = `spectro/sync/ack/${payload.code}`;
          mqttClient.send(ack);
        } catch (_) {}
      }

      if (state.activePage === "settings") {
        renderSettings();
      }
    } catch (e) {
      console.warn("Error saving paired session:", e);
    }

    setTimeout(() => {
      const modal = document.getElementById("login-modal");
      if (modal) modal.classList.add("hidden");
    }, 1200);
  }

  function initSpectroConnect() {
    if (typeof Paho === "undefined" || !Paho.MQTT) {
      console.warn("Paho MQTT not loaded");
      return;
    }

    try {
      mqttClient = new Paho.MQTT.Client("broker.emqx.io", 8084, "/mqtt", MQTT_CLIENT_ID);
      mqttClient.onConnectionLost = () => {
        setTimeout(initSpectroConnect, 5000);
      };
      mqttClient.onMessageArrived = async (message) => {
        try {
          const topic = message.destinationName;
          const payload = JSON.parse(message.payloadString);

          // 1. Account Sync from Spectro Android
          if (topic && topic.startsWith("spectro/sync/pair/")) {
            if (payload && payload.cookies) {
              await handlePairingSyncSuccess(payload);
              return;
            }
          }

          // 2. Cross-Device Playback Sync
          if (payload.source === "android" && payload.track) {
            updateDeviceBadge(`Memutar di: ${payload.device || "Android"}`);
            // If playing on Android, sync player bar metadata
            if (payload.isPlaying && (!state.isPlaying || payload.track.videoId !== state.queue[state.currentTrackIndex]?.videoId)) {
              playerTitle.textContent = payload.track.title;
              playerArtist.textContent = payload.track.artist;
              if (payload.track.art) playerArt.src = payload.track.art;
              if (payload.currentTime && payload.track.duration) {
                timeCurrent.textContent = formatTime(payload.currentTime);
                timeTotal.textContent = formatTime(payload.track.duration);
                const pct = (payload.currentTime / payload.track.duration) * 100;
                seekbarProgress.style.width = `${pct}%`;
                seekbarThumb.style.left = `${pct}%`;
              }
            }
          }
        } catch (_) {}
      };

      mqttClient.connect({
        useSSL: true,
        timeout: 5,
        onSuccess: () => {
          mqttClient.subscribe(MQTT_TOPIC, { qos: 0 });
          if (activePairingCode) {
            try {
              mqttClient.subscribe(`spectro/sync/pair/${activePairingCode}`, { qos: 0 });
            } catch (_) {}
          }
        },
        onFailure: () => {
          setTimeout(initSpectroConnect, 5000);
        }
      });
    } catch (e) {
      console.warn("Spectro Connect init error:", e);
    }
  }

  function broadcastPlaybackState(isPlaying) {
    if (!mqttClient || !mqttClient.isConnected()) return;
    const track = state.queue[state.currentTrackIndex];
    if (!track) return;

    try {
      const cur = ytPlayer && ytReady && typeof ytPlayer.getCurrentTime === "function" ? ytPlayer.getCurrentTime() : 0;
      const dur = ytPlayer && ytReady && typeof ytPlayer.getDuration === "function" ? ytPlayer.getDuration() : (track.duration || 0);

      const payload = {
        source: "windows",
        device: "Windows Desktop",
        isPlaying: isPlaying,
        currentTime: cur,
        timestamp: Date.now(),
        track: {
          id: track.videoId || track.id,
          videoId: track.videoId || track.id,
          title: track.title,
          artist: track.artist,
          art: track.art,
          duration: dur
        }
      };

      const message = new Paho.MQTT.Message(JSON.stringify(payload));
      message.destinationName = MQTT_TOPIC;
      mqttClient.send(message);
    } catch (_) {}
  }

  function updateDeviceBadge(name) {
    if (deviceSyncName) deviceSyncName.textContent = name;
  }

  // --- 16. Equalizer & Audio Effects Controller ---
  const EQ_PRESETS = {
    flat: { name: "Standar (Datar)", bands: [0, 0, 0, 0, 0], bass: 0, surround: 0 },
    bass: { name: "Penguat Bass", bands: [7, 5, -1, 1, 2], bass: 7, surround: 2 },
    vocal: { name: "Vokal Jernih", bands: [-2, 1, 5, 4, 1], bass: 1, surround: 3 },
    rock: { name: "Musik Rock", bands: [5, 3, -1, 3, 5], bass: 4, surround: 4 },
    pop: { name: "Musik Pop", bands: [2, 3, 4, 3, 2], bass: 3, surround: 3 },
    acoustic: { name: "Musik Akustik", bands: [3, 2, 1, 3, 4], bass: 2, surround: 4 },
    edm: { name: "Elektronik / EDM", bands: [6, 4, 0, 4, 6], bass: 8, surround: 5 }
  };

  const btnToggleEqualizer = document.getElementById("btn-toggle-equalizer");
  const equalizerModal = document.getElementById("equalizer-modal");
  const btnCloseEqualizerModal = document.getElementById("btn-close-equalizer-modal");
  const eqPresetSelect = document.getElementById("eq-preset-select");
  const eqBands = [0, 1, 2, 3, 4].map(i => document.getElementById(`eq-band-${i}`));
  const eqVals = [0, 1, 2, 3, 4].map(i => document.getElementById(`eq-val-${i}`));
  const eqBassBoost = document.getElementById("eq-bass-boost");
  const eqBassVal = document.getElementById("eq-bass-val");
  const eqSurround = document.getElementById("eq-surround");
  const eqSurroundVal = document.getElementById("eq-surround-val");

  let audioCtx = null;
  let eqFilters = [];
  let bassFilter = null;
  let mediaSourceNode = null;

  function setupWebAudioEQ() {
    try {
      const iframe = document.querySelector("#yt-player-host iframe");
      let mediaEl = null;
      try {
        mediaEl = iframe?.contentDocument?.querySelector("video") || iframe?.contentWindow?.document?.querySelector("video");
      } catch (_) {}
      if (!mediaEl) {
        mediaEl = document.getElementById("audio-engine");
      }
      if (!mediaEl) return;
      if (mediaEl._eqConnected) return;

      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextClass) return;
      if (!audioCtx) {
        audioCtx = new AudioContextClass();
      }
      if (audioCtx.state === "suspended") {
        audioCtx.resume();
      }

      const freqs = [60, 230, 910, 3600, 14000];
      const types = ["lowshelf", "peaking", "peaking", "peaking", "highshelf"];

      eqFilters = freqs.map((f, i) => {
        const filter = audioCtx.createBiquadFilter();
        filter.type = types[i];
        filter.frequency.value = f;
        filter.gain.value = state.equalizer?.bands?.[i] || 0;
        return filter;
      });

      bassFilter = audioCtx.createBiquadFilter();
      bassFilter.type = "lowshelf";
      bassFilter.frequency.value = 100;
      const bassVal = state.equalizer?.bass || 0;
      bassFilter.gain.value = bassVal * 1.5;

      mediaSourceNode = audioCtx.createMediaElementSource(mediaEl);
      mediaEl._eqConnected = true;

      let lastNode = mediaSourceNode;
      lastNode.connect(bassFilter);
      lastNode = bassFilter;

      eqFilters.forEach(f => {
        lastNode.connect(f);
        lastNode = f;
      });

      lastNode.connect(audioCtx.destination);
    } catch (err) {
      console.warn("Could not attach Web Audio EQ:", err);
    }
  }

  function updateEQFilterGains() {
    const bands = state.equalizer?.bands || [0, 0, 0, 0, 0];
    const bass = state.equalizer?.bass || 0;

    // 1. Inject real-time EQ & Bass Boost gains into YouTube IFrame in Electron backend
    invokeTauri("set_equalizer_gains", { bands, bass }).catch(() => {});

    // 2. Also update local AudioContext if attached
    if (audioCtx) {
      if (audioCtx.state === "suspended") audioCtx.resume();
      if (eqFilters.length === 5) {
        bands.forEach((val, idx) => {
          if (eqFilters[idx]) {
            eqFilters[idx].gain.setTargetAtTime(val, audioCtx.currentTime, 0.05);
          }
        });
      }
      if (bassFilter) {
        const bassGain = bass * 1.5;
        bassFilter.gain.setTargetAtTime(bassGain, audioCtx.currentTime, 0.05);
      }
    }
  }

  function initEqualizer() {
    let saved = null;
    try {
      saved = JSON.parse(localStorage.getItem("spectro_equalizer") || "null");
    } catch (_) {}

    if (!saved) {
      saved = { preset: "flat", bands: [0, 0, 0, 0, 0], bass: 0, surround: 0 };
    }

    state.equalizer = saved;
    applyEqualizerToUI(state.equalizer);
    setupEqualizerListeners();
  }

  function applyEqualizerToUI(eq) {
    if (eqPresetSelect) eqPresetSelect.value = eq.preset || "flat";
    (eq.bands || [0, 0, 0, 0, 0]).forEach((val, idx) => {
      if (eqBands[idx]) eqBands[idx].value = val;
      if (eqVals[idx]) eqVals[idx].textContent = `${val > 0 ? '+' : ''}${val} dB`;
    });
    if (eqBassBoost) eqBassBoost.value = eq.bass || 0;
    if (eqBassVal) eqBassVal.textContent = `${(eq.bass || 0) * 10}%`;
    if (eqSurround) eqSurround.value = eq.surround || 0;
    if (eqSurroundVal) eqSurroundVal.textContent = `${(eq.surround || 0) * 10}%`;
    updateEQFilterGains();
  }

  function saveEqualizerState() {
    localStorage.setItem("spectro_equalizer", JSON.stringify(state.equalizer));
    updateEQFilterGains();
  }

  function setupEqualizerListeners() {
    if (btnToggleEqualizer && equalizerModal) {
      btnToggleEqualizer.addEventListener("click", () => {
        const isHidden = equalizerModal.classList.toggle("hidden");
        btnToggleEqualizer.classList.toggle("active", !isHidden);
      });
    }

    if (btnCloseEqualizerModal && equalizerModal) {
      btnCloseEqualizerModal.addEventListener("click", () => {
        equalizerModal.classList.add("hidden");
        if (btnToggleEqualizer) btnToggleEqualizer.classList.remove("active");
      });
    }

    if (equalizerModal) {
      equalizerModal.addEventListener("click", (e) => {
        if (e.target === equalizerModal) {
          equalizerModal.classList.add("hidden");
          if (btnToggleEqualizer) btnToggleEqualizer.classList.remove("active");
        }
      });
    }

    if (eqPresetSelect) {
      eqPresetSelect.addEventListener("change", (e) => {
        const val = e.target.value;
        if (EQ_PRESETS[val]) {
          const p = EQ_PRESETS[val];
          state.equalizer = {
            preset: val,
            bands: [...p.bands],
            bass: p.bass,
            surround: p.surround
          };
          applyEqualizerToUI(state.equalizer);
          saveEqualizerState();
        }
      });
    }

    eqBands.forEach((slider, idx) => {
      if (slider) {
        slider.addEventListener("input", (e) => {
          const val = parseInt(e.target.value, 10);
          state.equalizer.bands[idx] = val;
          if (eqVals[idx]) eqVals[idx].textContent = `${val > 0 ? '+' : ''}${val} dB`;
          state.equalizer.preset = "custom";
          if (eqPresetSelect && !eqPresetSelect.querySelector('option[value="custom"]')) {
            const opt = document.createElement("option");
            opt.value = "custom";
            opt.textContent = "Pengaturan Kustom";
            eqPresetSelect.appendChild(opt);
          }
          if (eqPresetSelect) eqPresetSelect.value = "custom";
          saveEqualizerState();
        });
      }
    });

    if (eqBassBoost) {
      eqBassBoost.addEventListener("input", (e) => {
        const val = parseInt(e.target.value, 10);
        state.equalizer.bass = val;
        if (eqBassVal) eqBassVal.textContent = `${val * 10}%`;
        saveEqualizerState();
      });
    }

    if (eqSurround) {
      eqSurround.addEventListener("input", (e) => {
        const val = parseInt(e.target.value, 10);
        state.equalizer.surround = val;
        if (eqSurroundVal) eqSurroundVal.textContent = `${val * 10}%`;
        saveEqualizerState();
      });
    }
  }

  // --- 17. Settings Page Full View & Typography ---
  function applyLyricsSize(size) {
    if (!lyricsContainer) return;
    lyricsContainer.classList.remove("lyrics-size-medium", "lyrics-size-large", "lyrics-size-xlarge");
    lyricsContainer.classList.add(`lyrics-size-${size}`);
  }

  function initSettings() {
    const lSize = localStorage.getItem("spectro_lyrics_size") || "large";
    applyLyricsSize(lSize);
  }

  function renderSettings(pushHistory = true) {
    hidePlayerView();
    state.activePage = "settings";
    if (pushHistory) pushNav("settings");

    navItems.forEach(item => {
      item.classList.toggle("active", item.getAttribute("data-page") === "settings");
    });

    const connectedDevice = localStorage.getItem("spectro_connected_device") || null;
    const lyricsSettings = getLyricsSettings();
    const currentAudioQuality = localStorage.getItem("spectro_audio_quality") || "high";
    const currentAudioNormalize = localStorage.getItem("spectro_audio_normalize") !== "false";
    const currentLyricsSize = localStorage.getItem("spectro_lyrics_size") || "large";
    const currentLang = localStorage.getItem("spectro_language") || "auto";

    pageContent.innerHTML = `
      <div class="settings-page-container">
        <div class="settings-page-header">
          <svg viewBox="0 0 24 24" width="28" height="28" fill="#00F0FF">
            <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
          </svg>
          <h1>${t("settings_title")}</h1>
        </div>

        <!-- Section 1: Sinkronisasi Akun & Perangkat -->
        <div class="settings-group">
          <div class="settings-group-title">${t("settings_group_sync")}</div>
          <div class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-label">${t("settings_sync_desc")}</span>
              <span class="settings-item-desc">${connectedDevice ? `${t("settings_sync_paired")} <b>${escapeHtml(connectedDevice)}</b>` : t("settings_sync_none")}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 10px;">
              <span class="badge-status-online">${connectedDevice ? t("status_online") : t("status_ready")}</span>
              <button id="btn-settings-pair-device" class="btn-secondary-action">${connectedDevice ? t("settings_btn_reconnect") : t("settings_btn_pair")}</button>
            </div>
          </div>
        </div>

        <!-- Section 2: Pengaturan Sumber Lirik (Priority & Toggles) -->
        <div class="settings-group">
          <div class="settings-group-title">${t("settings_group_lyrics")}</div>
          <p style="font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 6px;">
            ${t("settings_lyrics_desc")}
          </p>
          <div id="lyrics-sources-list">
            ${lyricsSettings.sources.map((src, idx) => `
              <div class="lyrics-source-item" data-id="${src.id}" data-index="${idx}">
                <div class="lyrics-source-left">
                  <span class="lyrics-source-order-num">${idx + 1}</span>
                  <div class="settings-item-info">
                    <span class="settings-item-label">${src.name}</span>
                    <span class="settings-item-desc">${src.enabled ? t("settings_lyrics_active") : t("settings_lyrics_inactive")}</span>
                  </div>
                </div>
                <div class="lyrics-source-actions">
                  <div class="lyrics-order-btn-group">
                    <button class="lyrics-order-btn btn-lyrics-up" data-index="${idx}" ${idx === 0 ? 'disabled' : ''} title="Geser ke Atas">▲</button>
                    <button class="lyrics-order-btn btn-lyrics-down" data-index="${idx}" ${idx === lyricsSettings.sources.length - 1 ? 'disabled' : ''} title="Geser ke Bawah">▼</button>
                  </div>
                  <label class="switch-toggle">
                    <input type="checkbox" class="toggle-lyrics-source" data-index="${idx}" ${src.enabled ? 'checked' : ''}>
                    <span class="slider-toggle"></span>
                  </label>
                </div>
              </div>
            `).join('')}
          </div>
        </div>

        <!-- Section 3: Pemutaran & Kualitas Suara -->
        <div class="settings-group">
          <div class="settings-group-title">${t("settings_group_audio")}</div>
          <div class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-label">${t("settings_audio_quality")}</span>
              <span class="settings-item-desc">${t("settings_audio_quality_desc")}</span>
            </div>
            <select id="setting-audio-quality" class="eq-select">
              <option value="auto" ${currentAudioQuality === 'auto' ? 'selected' : ''}>${t("settings_audio_auto")}</option>
              <option value="high" ${currentAudioQuality === 'high' ? 'selected' : ''}>${t("settings_audio_high")}</option>
              <option value="normal" ${currentAudioQuality === 'normal' ? 'selected' : ''}>${t("settings_audio_normal")}</option>
              <option value="low" ${currentAudioQuality === 'low' ? 'selected' : ''}>${t("settings_audio_low")}</option>
            </select>
          </div>
          <div class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-label">${t("settings_audio_normalize")}</span>
              <span class="settings-item-desc">${t("settings_audio_normalize_desc")}</span>
            </div>
            <label class="switch-toggle">
              <input type="checkbox" id="setting-audio-normalize" ${currentAudioNormalize ? 'checked' : ''}>
              <span class="slider-toggle"></span>
            </label>
          </div>
        </div>

        <!-- Section 4: Tampilan & Bahasa -->
        <div class="settings-group">
          <div class="settings-group-title">${t("settings_group_appearance")}</div>
          <div class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-label">${t("settings_lang_label")}</span>
              <span class="settings-item-desc">${t("settings_lang_desc")}</span>
            </div>
            <select id="setting-app-language" class="eq-select">
              <option value="auto" ${currentLang === 'auto' ? 'selected' : ''}>${t("settings_lang_system")}</option>
              <option value="id" ${currentLang === 'id' ? 'selected' : ''}>Bahasa Indonesia</option>
              <option value="en" ${currentLang === 'en' ? 'selected' : ''}>English</option>
            </select>
          </div>
          <div class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-label">${t("settings_lyrics_size")}</span>
              <span class="settings-item-desc">${t("settings_lyrics_size_desc")}</span>
            </div>
            <select id="setting-lyrics-size" class="eq-select">
              <option value="medium" ${currentLyricsSize === 'medium' ? 'selected' : ''}>${t("settings_lyrics_medium")}</option>
              <option value="large" ${currentLyricsSize === 'large' ? 'selected' : ''}>${t("settings_lyrics_large")}</option>
              <option value="xlarge" ${currentLyricsSize === 'xlarge' ? 'selected' : ''}>${t("settings_lyrics_xlarge")}</option>
            </select>
          </div>
        </div>

        <!-- Section 5: Penyimpanan & Cache -->
        <div class="settings-group">
          <div class="settings-group-title">${t("settings_group_storage")}</div>
          <div class="settings-item">
            <div class="settings-item-info">
              <span class="settings-item-label">${t("settings_storage_clear")}</span>
              <span class="settings-item-desc">${t("settings_storage_clear_desc")}</span>
            </div>
            <button id="btn-clear-cache" class="btn-secondary-action">${t("settings_btn_clear")}</button>
          </div>
        </div>

        <!-- Section 6: Tentang Aplikasi -->
        <div class="settings-group">
          <div class="settings-group-title">${t("settings_group_about")}</div>
          <div class="settings-about-row">
            <div>
              <div class="settings-about-app">${t("settings_about_app")}</div>
              <div class="settings-about-version">${t("settings_about_version")}</div>
            </div>
            <div class="settings-about-license">${t("settings_about_license")}</div>
          </div>
        </div>
      </div>
    `;

    setupSettingsPageListeners();
  }

  function setupSettingsPageListeners() {
    const btnPair = document.getElementById("btn-settings-pair-device");
    if (btnPair) {
      btnPair.addEventListener("click", () => {
        const loginModal = document.getElementById("login-modal");
        if (loginModal) {
          loginModal.classList.remove("hidden");
          const code = generatePairingCode();
          updatePairingUI(code);
        }
      });
    }

    const settingAppLang = document.getElementById("setting-app-language");
    if (settingAppLang) {
      settingAppLang.addEventListener("change", (e) => {
        const val = e.target.value;
        if (val === "auto") {
          localStorage.removeItem("spectro_language");
        } else {
          localStorage.setItem("spectro_language", val);
        }
        applyLanguageToUI();
        renderSettings(false);
      });
    }

    // Lyrics source reorder (Up / Down)
    document.querySelectorAll(".btn-lyrics-up").forEach(btn => {
      btn.addEventListener("click", () => {
        const idx = parseInt(btn.getAttribute("data-index"), 10);
        if (idx > 0) {
          const cfg = getLyricsSettings();
          const temp = cfg.sources[idx - 1];
          cfg.sources[idx - 1] = cfg.sources[idx];
          cfg.sources[idx] = temp;
          localStorage.setItem("spectro_lyrics_settings", JSON.stringify(cfg));
          renderSettings();
          // Instantly re-fetch lyrics for currently playing song with the new priority order
          if (state.queue && state.queue[state.currentTrackIndex]) {
            fetchAndRenderLyrics(state.queue[state.currentTrackIndex]);
          }
        }
      });
    });

    document.querySelectorAll(".btn-lyrics-down").forEach(btn => {
      btn.addEventListener("click", () => {
        const idx = parseInt(btn.getAttribute("data-index"), 10);
        const cfg = getLyricsSettings();
        if (idx < cfg.sources.length - 1) {
          const temp = cfg.sources[idx + 1];
          cfg.sources[idx + 1] = cfg.sources[idx];
          cfg.sources[idx] = temp;
          localStorage.setItem("spectro_lyrics_settings", JSON.stringify(cfg));
          renderSettings();
          // Instantly re-fetch lyrics for currently playing song with the new priority order
          if (state.queue && state.queue[state.currentTrackIndex]) {
            fetchAndRenderLyrics(state.queue[state.currentTrackIndex]);
          }
        }
      });
    });

    // Lyrics source toggle active / inactive
    document.querySelectorAll(".toggle-lyrics-source").forEach(toggle => {
      toggle.addEventListener("change", (e) => {
        const idx = parseInt(toggle.getAttribute("data-index"), 10);
        const cfg = getLyricsSettings();
        if (cfg.sources[idx]) {
          cfg.sources[idx].enabled = e.target.checked;
          localStorage.setItem("spectro_lyrics_settings", JSON.stringify(cfg));
          const descEl = toggle.closest(".lyrics-source-item")?.querySelector(".settings-item-desc");
          if (descEl) descEl.textContent = e.target.checked ? "Aktif digunakan" : "Dinonaktifkan";
          // Instantly re-fetch lyrics for currently playing song with the updated enabled sources
          if (state.queue && state.queue[state.currentTrackIndex]) {
            fetchAndRenderLyrics(state.queue[state.currentTrackIndex]);
          }
        }
      });
    });

    const settingAudioQuality = document.getElementById("setting-audio-quality");
    if (settingAudioQuality) {
      settingAudioQuality.addEventListener("change", (e) => {
        localStorage.setItem("spectro_audio_quality", e.target.value);
      });
    }

    const settingAudioNormalize = document.getElementById("setting-audio-normalize");
    if (settingAudioNormalize) {
      settingAudioNormalize.addEventListener("change", (e) => {
        localStorage.setItem("spectro_audio_normalize", e.target.checked.toString());
      });
    }

    const settingLyricsSize = document.getElementById("setting-lyrics-size");
    if (settingLyricsSize) {
      settingLyricsSize.addEventListener("change", (e) => {
        const val = e.target.value;
        localStorage.setItem("spectro_lyrics_size", val);
        applyLyricsSize(val);
      });
    }

    const btnClearCache = document.getElementById("btn-clear-cache");
    if (btnClearCache) {
      btnClearCache.addEventListener("click", () => {
        localStorage.removeItem("spectro_volume");
        localStorage.removeItem("spectro_equalizer");
        localStorage.removeItem("spectro_sidebar_collapsed");
        btnClearCache.textContent = "Berhasil Dibersihkan!";
        btnClearCache.style.color = "#1db954";
        setTimeout(() => {
          btnClearCache.textContent = "Bersihkan";
          btnClearCache.style.color = "#ffffff";
        }, 2000);
      });
    }
  }

  const getTauriWin = () => {
    try {
      return window.__TAURI__?.window?.getCurrentWindow?.()
          || window.__TAURI__?.webviewWindow?.getCurrentWebviewWindow?.();
    } catch (_) { return null; }
  };

  let lastToggleMaxTime = 0;
  window.__spectro_minimize = function(e) {
    if (e) { e.preventDefault(); e.stopPropagation(); }
    invokeTauri("window_minimize");
  };

  window.__spectro_toggle_maximize = function(e) {
    if (e) { e.preventDefault(); e.stopPropagation(); }
    const now = Date.now();
    if (now - lastToggleMaxTime < 350) return;
    lastToggleMaxTime = now;
    invokeTauri("window_toggle_maximize");
  };

  window.__spectro_close = function(e) {
    if (e) { e.preventDefault(); e.stopPropagation(); }
    invokeTauri("window_close");
  };

  function initWindowControls() {
    const btnMin = document.getElementById("win-btn-minimize");
    const btnMax = document.getElementById("win-btn-maximize");
    const btnClose = document.getElementById("win-btn-close");

    if (btnMin) {
      btnMin.onclick = window.__spectro_minimize;
      btnMin.onmousedown = (e) => e.stopPropagation();
    }
    if (btnMax) {
      btnMax.onclick = window.__spectro_toggle_maximize;
      btnMax.onmousedown = (e) => e.stopPropagation();
    }
    if (btnClose) {
      btnClose.onclick = window.__spectro_close;
      btnClose.onmousedown = (e) => e.stopPropagation();
    }
  }
  initWindowControls();

  // --- 17. Utility Functions ---
  function formatTime(seconds) {
    const s = Math.floor(seconds || 0);
    const m = Math.floor(s / 60);
    const rem = s % 60;
    return `${m}:${rem.toString().padStart(2, '0')}`;
  }

  // --- 17. Setup Event Listeners ---
  function setupEventListeners() {
    // Navigation items
    navItems.forEach(item => {
      item.addEventListener("click", () => {
        navItems.forEach(n => n.classList.remove("active"));
        item.classList.add("active");
        const page = item.getAttribute("data-page");
        if (page === "home") renderHome();
        else if (page === "search") {
          hidePlayerView();
          state.activePage = "search";
          globalSearchInput.focus();
        } else if (page === "explore") renderExplore();
        else if (page === "radios") renderRadios();
        else if (page === "settings") {
          renderSettings();
        }
      });
    });

    // Global Search Input with live debouncing (like Android APK)
    if (globalSearchInput) {
      let searchDebounceTimer = null;
      globalSearchInput.addEventListener("input", (e) => {
        clearTimeout(searchDebounceTimer);
        const query = e.target.value.trim();
        if (query.length >= 2) {
          searchDebounceTimer = setTimeout(() => {
            performSearch(query);
          }, 350);
        }
      });

      globalSearchInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
          clearTimeout(searchDebounceTimer);
          performSearch(globalSearchInput.value);
        }
      });
    }

    // Liked Songs Library Item in Sidebar
    const likedLibraryItem = document.querySelector('.playlist-item[data-id="liked"]');
    if (likedLibraryItem) {
      likedLibraryItem.addEventListener("click", () => {
        openPlaylist("liked", "Lagu Disukai", null);
      });
    }

    // Playback buttons
    if (btnPlayPause) btnPlayPause.addEventListener("click", togglePlayPause);
    if (btnNext) btnNext.addEventListener("click", playNext);
    if (btnPrev) btnPrev.addEventListener("click", playPrev);

    if (btnShuffle) {
      btnShuffle.addEventListener("click", () => {
        state.isShuffle = !state.isShuffle;
        btnShuffle.style.color = state.isShuffle ? "#1db954" : "var(--text-secondary)";
      });
    }

    if (btnRepeat) {
      btnRepeat.addEventListener("click", () => {
        state.isRepeat = !state.isRepeat;
        btnRepeat.style.color = state.isRepeat ? "#1db954" : "var(--text-secondary)";
      });
    }

    // Seekbar scrubbing
    if (seekbarContainer) {
      let isSeeking = false;
      const handleSeek = (e) => {
        const rect = seekbarContainer.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const pct = Math.max(0, Math.min(1, clickX / rect.width));
        seekbarProgress.style.width = `${pct * 100}%`;
        seekbarThumb.style.left = `${pct * 100}%`;
        if (ytPlayer && ytReady && typeof ytPlayer.getDuration === "function") {
          const dur = ytPlayer.getDuration();
          if (dur > 0) {
            ytPlayer.seekTo(dur * pct, true);
          }
        }
      };
      seekbarContainer.addEventListener("click", handleSeek);
    }

    // Volume scrubbing & dragging
    if (volumeContainer) {
      let isDraggingVolume = false;
      const handleVolFromEvent = (e) => {
        const rect = volumeContainer.getBoundingClientRect();
        const clickX = Math.max(0, Math.min(rect.width, e.clientX - rect.left));
        setVolume(clickX / rect.width);
      };

      volumeContainer.addEventListener("mousedown", (e) => {
        isDraggingVolume = true;
        handleVolFromEvent(e);
      });

      window.addEventListener("mousemove", (e) => {
        if (isDraggingVolume) {
          handleVolFromEvent(e);
        }
      });

      window.addEventListener("mouseup", () => {
        if (isDraggingVolume) {
          isDraggingVolume = false;
        }
      });

      volumeContainer.addEventListener("click", handleVolFromEvent);
    }

    if (btnVolume) {
      btnVolume.addEventListener("click", () => {
        if (state.currentVolume > 0) {
          setVolume(0);
        } else {
          setVolume(lastNonZeroVolume || 0.8);
        }
      });
    }

    // Sidebar Left Collapsible Toggle (Spotify style)
    if (btnToggleSidebar) {
      btnToggleSidebar.addEventListener("click", toggleSidebar);
    }

    // Dedicated Player View Toggles
    if (btnTogglePlayerView) {
      btnTogglePlayerView.addEventListener("click", togglePlayerView);
    }
    if (btnBackToBrowse) {
      btnBackToBrowse.addEventListener("click", hidePlayerView);
    }

    // Navigation History Controls
    const btnNavBack = document.getElementById("nav-back-btn");
    const btnNavForward = document.getElementById("nav-forward-btn");
    if (btnNavBack) {
      btnNavBack.addEventListener("click", () => {
        if (navHistoryIdx > 0) {
          navHistoryIdx--;
          navigateFromHistory(navHistory[navHistoryIdx]);
        }
      });
    }
    if (btnNavForward) {
      btnNavForward.addEventListener("click", () => {
        if (navHistoryIdx < navHistory.length - 1) {
          navHistoryIdx++;
          navigateFromHistory(navHistory[navHistoryIdx]);
        }
      });
    }
    updateNavArrows();

    // Player Like & Playlist Actions
    const btnPlayerLike = document.getElementById("player-like-btn");
    if (btnPlayerLike) {
      btnPlayerLike.addEventListener("click", toggleLikeCurrentTrack);
    }

    const btnPlayerPlaylist = document.getElementById("player-playlist-btn");
    if (btnPlayerPlaylist) {
      btnPlayerPlaylist.addEventListener("click", () => openSaveToPlaylistModal());
    }

    // Playlist Modal Controls
    const btnClosePlaylistModal = document.getElementById("btn-close-playlist-modal");
    const playlistModal = document.getElementById("playlist-modal");
    if (btnClosePlaylistModal && playlistModal) {
      btnClosePlaylistModal.addEventListener("click", () => {
        playlistModal.classList.add("hidden");
      });
      playlistModal.addEventListener("click", (e) => {
        if (e.target === playlistModal) playlistModal.classList.add("hidden");
      });
    }

    const btnCreatePlaylist = document.getElementById("btn-create-playlist");
    const inputNewPlaylistTitle = document.getElementById("input-new-playlist-title");
    if (btnCreatePlaylist && inputNewPlaylistTitle) {
      btnCreatePlaylist.addEventListener("click", async () => {
        const title = inputNewPlaylistTitle.value.trim();
        if (!title) return;
        const currentTrack = activeModalTargetTrack || state.queue[state.currentTrackIndex];
        const videoId = currentTrack ? (currentTrack.videoId || currentTrack.id) : null;
        await createAndAddToPlaylist(title, videoId ? [videoId] : []);
        inputNewPlaylistTitle.value = "";
      });
    }

    // Queue Sidebar Toggle
    if (btnToggleQueue) {
      btnToggleQueue.addEventListener("click", toggleQueueSidebar);
    }

    // Spectro Sync: Android to Windows Pairing Modal
    const btnLogin = document.getElementById("btn-login");
    const loginModal = document.getElementById("login-modal");
    const btnCloseModal = document.getElementById("btn-close-login-modal");
    const btnCopyPairCode = document.getElementById("btn-copy-pair-code");
    const btnCopyText = document.getElementById("btn-copy-text");
    const btnRefreshPairCode = document.getElementById("btn-refresh-pair-code");
    const userBadge = document.getElementById("user-badge");
    const userMenuDropdown = document.getElementById("user-menu-dropdown");
    const btnLogout = document.getElementById("btn-logout");

    function generatePairingCode() {
      return Math.floor(100000 + Math.random() * 900000).toString();
    }

    function updatePairingUI(code) {
      activePairingCode = code;
      const pairDisplay = document.getElementById("sync-pair-code");
      if (pairDisplay) {
        pairDisplay.textContent = `${code.substring(0, 3)} ${code.substring(3)}`;
      }
      const statusBox = document.getElementById("sync-status-indicator");
      const statusText = document.getElementById("sync-status-text");
      if (statusBox) {
        statusBox.style.background = "rgba(255, 255, 255, 0.03)";
        statusBox.style.borderColor = "rgba(255, 255, 255, 0.08)";
        statusBox.style.color = "#aaa";
      }
      if (statusText) {
        statusText.textContent = "Menunggu sambungan dari Spectro HP...";
      }

      if (mqttClient && mqttClient.isConnected()) {
        try {
          mqttClient.subscribe(`spectro/sync/pair/${code}`, { qos: 0 });
        } catch (_) {}
      } else {
        initSpectroConnect();
      }
    }

    function openLoginModal() {
      if (loginModal) {
        loginModal.classList.remove("hidden");
        const code = generatePairingCode();
        updatePairingUI(code);
      }
    }

    if (btnLogin) {
      btnLogin.addEventListener("click", openLoginModal);
    }

    if (btnCloseModal && loginModal) {
      btnCloseModal.addEventListener("click", () => {
        loginModal.classList.add("hidden");
        if (activePairingCode && mqttClient && mqttClient.isConnected()) {
          try {
            mqttClient.unsubscribe(`spectro/sync/pair/${activePairingCode}`);
          } catch (_) {}
        }
      });
    }

    if (btnCopyPairCode) {
      btnCopyPairCode.addEventListener("click", () => {
        if (activePairingCode) {
          navigator.clipboard.writeText(activePairingCode);
          if (btnCopyText) {
            btnCopyText.textContent = "Tersalin!";
            setTimeout(() => { btnCopyText.textContent = "Salin Kode"; }, 2000);
          }
        }
      });
    }

    if (btnRefreshPairCode) {
      btnRefreshPairCode.addEventListener("click", () => {
        if (activePairingCode && mqttClient && mqttClient.isConnected()) {
          try {
            mqttClient.unsubscribe(`spectro/sync/pair/${activePairingCode}`);
          } catch (_) {}
        }
        const newCode = generatePairingCode();
        updatePairingUI(newCode);
      });
    }

    // User Profile Dropdown
    if (userBadge && userMenuDropdown) {
      userBadge.addEventListener("click", (e) => {
        e.stopPropagation();
        userMenuDropdown.classList.toggle("hidden");
      });
      document.addEventListener("click", () => {
        userMenuDropdown.classList.add("hidden");
      });
    }

    const btnDropdownSettings = document.getElementById("btn-dropdown-settings");
    if (btnDropdownSettings) {
      btnDropdownSettings.addEventListener("click", () => {
        if (userMenuDropdown) userMenuDropdown.classList.add("hidden");
        renderSettings();
      });
    }

    if (btnLogout) {
      btnLogout.addEventListener("click", async () => {
        await invokeTauri("logout");
        applySession({ is_logged_in: false });
      });
    }
  }

  const APP_VERSION = "1.0.0";
  function checkVersionMigration() {
    try {
      const storedVer = localStorage.getItem("spectro_version");
      if (storedVer !== APP_VERSION) {
        localStorage.removeItem("spectro_home_cache");
        localStorage.setItem("spectro_version", APP_VERSION);
      }
    } catch (_) {}
  }

  // --- 18. Initialize Application ---
  function init() {
    checkVersionMigration();
    applyLanguageToUI();
    initWindowControls();
    window.playTrackWithRadio = playTrackWithRadio;
    ensureYouTubePlayer();

    // Restore sidebar collapsed state
    if (localStorage.getItem("spectro_sidebar_collapsed") === "true" && sidebarLeft) {
      sidebarLeft.classList.add("collapsed");
    }

    setupEventListeners();
    initSettings();
    initEqualizer();
    setVolume(state.currentVolume);
    renderQueueList();
    updateLikedCountSidebar();
    renderHome();
    initAuth();
    initSpectroConnect();
  }

  // Run on DOM ready or immediately if already loaded
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
