const { app, BrowserWindow, ipcMain, shell, session } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');
const crypto = require('crypto');
const os = require('os');

// Flags to ensure background audio plays smoothly without throttling
app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');
app.commandLine.appendSwitch('disable-background-timer-throttling');
app.commandLine.appendSwitch('disable-backgrounding-occluded-windows');
app.commandLine.appendSwitch('disable-renderer-backgrounding');

let mainWindow = null;
let internalServer = null;
let internalPort = 0;

// Session storage helpers
function getSessionDir() {
  const base = process.env.LOCALAPPDATA || os.tmpdir();
  const dir = path.join(base, 'Spectro');
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
  return dir;
}

function getSessionFile() {
  return path.join(getSessionDir(), 'session.json');
}

function getOAuthConfigFile() {
  return path.join(getSessionDir(), 'oauth_config.json');
}

function loadSession() {
  try {
    const f = getSessionFile();
    if (fs.existsSync(f)) {
      return JSON.parse(fs.readFileSync(f, 'utf8'));
    }
  } catch (_) {}
  return null;
}

function saveSessionData(data) {
  try {
    fs.writeFileSync(getSessionFile(), JSON.stringify(data, null, 2), 'utf8');
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('session_updated', JSON.stringify(data));
    }
  } catch (e) {
    console.error('Failed to save session:', e);
  }
}

// InnerTube context
function getClientContext() {
  return {
    client: {
      clientName: "WEB_REMIX",
      clientVersion: "1.20241001.01.00",
      hl: "id",
      gl: "ID"
    },
    request: {
      useSsl: true,
      internalExperimentFlags: []
    },
    user: {
      lockedSafetyMode: false
    }
  };
}

// InnerTube POST Helper with SAPISIDHASH authentication support
async function fetchInnerTube(endpoint, body) {
  const url = `https://music.youtube.com/youtubei/v1/${endpoint}`;
  const sess = loadSession();
  const cookies = sess?.cookies || "";

  const headers = {
    "Content-Type": "application/json",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
    "Origin": "https://music.youtube.com",
    "Referer": "https://music.youtube.com/",
    "X-Origin": "https://music.youtube.com",
    "X-Goog-AuthUser": "0"
  };

  if (cookies) {
    if (cookies.startsWith("oauth_token=")) {
      headers["Authorization"] = `Bearer ${cookies.replace("oauth_token=", "")}`;
    } else {
      headers["Cookie"] = cookies;
      const sapisidMatch = cookies.match(/(?:SAPISID|__Secure-3PAPISID)=([^;]+)/);
      if (sapisidMatch) {
        const sapisid = sapisidMatch[1].trim();
        const time = Math.floor(Date.now() / 1000);
        const hash = crypto.createHash('sha1').update(`${time} ${sapisid} https://music.youtube.com`).digest('hex');
        headers["Authorization"] = `SAPISIDHASH ${time}_${hash}`;
      }
    }
  }

  const res = await fetch(url, {
    method: "POST",
    headers,
    body: JSON.stringify(body)
  });
  return await res.text();
}

// Embedded HTTP Server to serve dist/ over standard http://127.0.0.1
// This resolves the YouTube Iframe API origin restriction with file://
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.svg': 'image/svg+xml'
};

function startInternalServer() {
  return new Promise((resolve, reject) => {
    const distPath = path.join(__dirname, 'dist');
    internalServer = http.createServer((req, res) => {
      let reqPath = (req.url || '/').split('?')[0];
      if (reqPath === '/' || !reqPath) reqPath = '/index.html';

      const safePath = path.normalize(path.join(distPath, reqPath));
      if (!safePath.startsWith(distPath)) {
        res.writeHead(403);
        res.end('Forbidden');
        return;
      }

      fs.readFile(safePath, (err, data) => {
        if (err) {
          // Fallback to index.html for SPA routes if not found
          fs.readFile(path.join(distPath, 'index.html'), (err2, data2) => {
            if (err2) {
              res.writeHead(404);
              res.end('Not Found');
            } else {
              res.writeHead(200, {
                'Content-Type': 'text/html; charset=utf-8',
                'Access-Control-Allow-Origin': '*'
              });
              res.end(data2);
            }
          });
          return;
        }

        const ext = path.extname(safePath).toLowerCase();
        res.writeHead(200, {
          'Content-Type': MIME_TYPES[ext] || 'application/octet-stream',
          'Access-Control-Allow-Origin': '*'
        });
        res.end(data);
      });
    });

    internalServer.listen(0, '127.0.0.1', () => {
      internalPort = internalServer.address().port;
      console.log(`Spectro internal server running on http://127.0.0.1:${internalPort}`);
      resolve(internalPort);
    });

    internalServer.on('error', (err) => {
      console.error('Failed to start internal server:', err);
      reject(err);
    });
  });
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1240,
    height: 820,
    minWidth: 900,
    minHeight: 620,
    frame: false,
    backgroundColor: '#0a0a0c',
    icon: path.join(__dirname, 'dist', 'icon.ico'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: false,
      webSecurity: false,
      allowRunningInsecureContent: true
    }
  });

  // Intercept YouTube headers to match nocookie origin
  session.defaultSession.webRequest.onBeforeSendHeaders((details, callback) => {
    const u = details.url;
    if (u.includes('youtube') || u.includes('googlevideo.com') || u.includes('ytimg.com')) {
      details.requestHeaders['Origin'] = 'https://www.youtube-nocookie.com';
      details.requestHeaders['Referer'] = 'https://www.youtube-nocookie.com/';
    }
    callback({ cancel: false, requestHeaders: details.requestHeaders });
  });

  // Strip restrictive headers and allow nocookie origin
  session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
    const responseHeaders = Object.assign({}, details.responseHeaders);
    delete responseHeaders['x-frame-options'];
    delete responseHeaders['X-Frame-Options'];
    delete responseHeaders['content-security-policy'];
    delete responseHeaders['Content-Security-Policy'];
    if (details.url.includes('youtube') || details.url.includes('googlevideo.com')) {
      responseHeaders['access-control-allow-origin'] = ['https://www.youtube-nocookie.com'];
      responseHeaders['access-control-allow-credentials'] = ['true'];
    }
    callback({ cancel: false, responseHeaders });
  });

  session.defaultSession.webRequest.onErrorOccurred((details) => {
    if (details.url.includes('googlevideo') || details.url.includes('youtube')) {
      console.log(`[Net Error] ${details.error} on ${details.url.substring(0, 100)}`);
    }
  });

  session.defaultSession.webRequest.onResponseStarted((details) => {
    if (details.statusCode >= 400 && (details.url.includes('googlevideo') || details.url.includes('youtube'))) {
      console.log(`[Net Status ${details.statusCode}] on ${details.url.substring(0, 100)}`);
    }
  });

  mainWindow.webContents.on('console-message', (event, level, message) => {
    console.log(`[Renderer] ${message}`);
  });

  mainWindow.loadURL(`http://127.0.0.1:${internalPort}`);

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// Single instance lock
const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });

  app.whenReady().then(async () => {
    try {
      await startInternalServer();
    } catch (_) {
      // Fallback if local server failed
    }
    createWindow();

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
  });
}

app.on('window-all-closed', () => {
  if (internalServer) {
    internalServer.close();
  }
  app.quit();
});

// --- IPC HANDLERS ---

// 1. Window Controls
let lastToggleMaximizeTime = 0;
ipcMain.handle('window_minimize', () => {
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.minimize();
  return true;
});

ipcMain.handle('window_toggle_maximize', () => {
  const now = Date.now();
  if (now - lastToggleMaximizeTime < 350) return true;
  lastToggleMaximizeTime = now;

  if (mainWindow && !mainWindow.isDestroyed()) {
    if (mainWindow.isMaximized()) {
      mainWindow.unmaximize();
    } else {
      mainWindow.maximize();
    }
  }
  return true;
});

ipcMain.handle('window_close', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.close();
  }
  app.quit();
  return true;
});

ipcMain.handle('window_is_maximized', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    return mainWindow.isMaximized();
  }
  return false;
});

// Equalizer & Audio Processing IPC: Injects Web Audio BiquadFilters directly inside YouTube Iframe
ipcMain.handle('set_equalizer_gains', async (_, args = {}) => {
  const bands = args.bands || [0, 0, 0, 0, 0];
  const bass = args.bass || 0;
  if (!mainWindow || mainWindow.isDestroyed()) return false;

  const script = `
    (function() {
      try {
        const video = document.querySelector('video');
        if (!video) return;

        if (!window._spectroAudioCtx) {
          const AudioContextClass = window.AudioContext || window.webkitAudioContext;
          if (!AudioContextClass) return;
          const ctx = new AudioContextClass();
          const source = ctx.createMediaElementSource(video);

          const freqs = [60, 230, 910, 3600, 14000];
          const types = ["lowshelf", "peaking", "peaking", "peaking", "highshelf"];
          const filters = freqs.map((f, i) => {
            const filter = ctx.createBiquadFilter();
            filter.type = types[i];
            filter.frequency.value = f;
            return filter;
          });

          const bassFilter = ctx.createBiquadFilter();
          bassFilter.type = "lowshelf";
          bassFilter.frequency.value = 100;

          let lastNode = source;
          lastNode.connect(bassFilter);
          lastNode = bassFilter;

          filters.forEach(f => {
            lastNode.connect(f);
            lastNode = f;
          });

          lastNode.connect(ctx.destination);

          window._spectroAudioCtx = ctx;
          window._spectroBass = bassFilter;
          window._spectroFilters = filters;
        }

        if (window._spectroAudioCtx && window._spectroAudioCtx.state === 'suspended') {
          window._spectroAudioCtx.resume();
        }

        const bList = ${JSON.stringify(bands)};
        const bVal = ${bass};

        if (window._spectroFilters && window._spectroFilters.length === 5) {
          bList.forEach((val, idx) => {
            if (window._spectroFilters[idx]) {
              window._spectroFilters[idx].gain.setTargetAtTime(val, window._spectroAudioCtx.currentTime, 0.05);
            }
          });
        }

        if (window._spectroBass) {
          window._spectroBass.gain.setTargetAtTime(bVal * 1.5, window._spectroAudioCtx.currentTime, 0.05);
        }
      } catch (err) {
        console.warn('[Spectro Frame EQ Error]', err);
      }
    })();
  `;

  try {
    if (mainWindow.webContents?.mainFrame?.frames) {
      for (const frame of mainWindow.webContents.mainFrame.frames) {
        if (frame.url && (frame.url.includes('youtube') || frame.url.includes('embed'))) {
          try {
            await frame.executeJavaScript(script);
          } catch (_) {}
        }
      }
    }
  } catch (_) {}
  return true;
});

// 2. Music Search & Catalog
ipcMain.handle('search_music', async (_, args = {}) => {
  const query = args.query || "";
  return await fetchInnerTube('search', {
    context: getClientContext(),
    query
  });
});

ipcMain.handle('get_radio', async (_, args = {}) => {
  const videoId = args.videoId || args.video_id || "";
  const continuation = args.continuation || null;

  const body = continuation ? {
    context: getClientContext(),
    continuation
  } : {
    context: getClientContext(),
    videoId,
    playlistId: `RDAMVM${videoId}`,
    isAudioOnly: true
  };

  return await fetchInnerTube('next', body);
});

ipcMain.handle('get_home', async () => {
  return await fetchInnerTube('browse', {
    context: getClientContext(),
    browseId: 'FEmusic_home'
  });
});

ipcMain.handle('get_explore', async () => {
  return await fetchInnerTube('browse', {
    context: getClientContext(),
    browseId: 'FEmusic_explore'
  });
});

ipcMain.handle('get_playlist_details', async (_, args = {}) => {
  const rawId = args.browseId || args.browse_id || "";
  const browseId = rawId.startsWith('VL') || rawId === 'LM' || rawId.startsWith('MPRE') ? rawId : `VL${rawId}`;
  return await fetchInnerTube('browse', {
    context: getClientContext(),
    browseId
  });
});

ipcMain.handle('get_liked_songs', async () => {
  return await fetchInnerTube('browse', {
    context: getClientContext(),
    browseId: 'LM'
  });
});

ipcMain.handle('get_user_library', async () => {
  return await fetchInnerTube('browse', {
    context: getClientContext(),
    browseId: 'FEmusic_liked_playlists'
  });
});

ipcMain.handle('rate_song', async (_, args = {}) => {
  const videoId = args.videoId || args.video_id || "";
  const liked = args.liked !== false;
  const action = liked ? "like" : "removelike";
  try {
    return await fetchInnerTube(`like/${action}`, {
      context: getClientContext(),
      target: { videoId }
    });
  } catch (err) {
    console.warn("rate_song error:", err);
    return JSON.stringify({ error: err.message });
  }
});

ipcMain.handle('edit_playlist', async (_, args = {}) => {
  const playlistId = args.playlistId || "";
  const videoId = args.videoId || "";
  const action = args.action || "ACTION_ADD_VIDEO";
  try {
    return await fetchInnerTube('browse/edit_playlist', {
      context: getClientContext(),
      playlistId,
      actions: [{
        action,
        addedVideoId: videoId
      }]
    });
  } catch (err) {
    console.warn("edit_playlist error:", err);
    return JSON.stringify({ error: err.message });
  }
});

ipcMain.handle('create_playlist', async (_, args = {}) => {
  const title = args.title || "Playlist Baru";
  const videoIds = args.videoIds || [];
  try {
    return await fetchInnerTube('playlist/create', {
      context: getClientContext(),
      title,
      privacyStatus: "PUBLIC",
      videoIds
    });
  } catch (err) {
    console.warn("create_playlist error:", err);
    return JSON.stringify({ error: err.message });
  }
});

// 3. Lyrics APIs (7 waterfall providers matching APK v1.0.0 & Windows guidelines)
ipcMain.handle('get_lyrics', async (_, args = {}) => {
  const encTitle = encodeURIComponent(args.title || "");
  const encArtist = encodeURIComponent(args.artist || "");
  const url = `https://lrclib.net/api/get?track_name=${encTitle}&artist_name=${encArtist}`;
  const res = await fetch(url, {
    headers: { "User-Agent": "Spectro Desktop (https://github.com/NazarYosa/Spectro)" }
  });
  return await res.text();
});

ipcMain.handle('get_betterlyrics', async (_, args = {}) => {
  const encTitle = encodeURIComponent(args.title || "");
  const encArtist = encodeURIComponent(args.artist || "");
  let url = `https://lyrics-api.boidu.dev/getLyrics?s=${encTitle}&a=${encArtist}`;
  if (args.duration && args.duration > 0) url += `&d=${args.duration}`;
  const res = await fetch(url, {
    headers: {
      "User-Agent": "Mozilla/5.0",
      "Accept": "application/json"
    }
  });
  return await res.text();
});

ipcMain.handle('get_lyricsplus', async (_, args = {}) => {
  const encTitle = encodeURIComponent(args.title || "");
  const encArtist = encodeURIComponent(args.artist || "");
  const servers = [
    "https://lyricsplus.binimum.org",
    "https://lyricsplus.atomix.one",
    "https://lyricsplus.prjktla.my.id"
  ];
  for (const s of servers) {
    try {
      let url = `${s}/v2/lyrics/get?title=${encTitle}&artist=${encArtist}`;
      if (args.duration && args.duration > 0) url += `&duration=${args.duration}`;
      const res = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0" }, signal: AbortSignal.timeout(4000) });
      const txt = await res.text();
      if (txt && txt.includes('lyrics')) return txt;
    } catch (_) {}
  }
  throw new Error("LyricsPlus unavailable");
});

ipcMain.handle('get_kugou_lyrics', async (_, args = {}) => {
  const kw = encodeURIComponent(`${args.title || ""} ${args.artist || ""}`);
  const url = `http://mobileservice.kugou.com/api/v3/search/song?version=9108&plat=0&pagesize=8&showtype=0&keyword=${kw}`;
  const res = await fetch(url);
  const data = await res.json();
  const infoArr = data?.data?.info || [];
  for (const song of infoArr) {
    if (!song.hash) continue;
    if (args.duration && song.duration && Math.abs(song.duration - args.duration) > 8) continue;
    const sUrl = `https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&hash=${song.hash}`;
    const sRes = await fetch(sUrl);
    const sData = await sRes.json();
    const cand = sData?.candidates?.[0];
    if (cand?.id && cand?.accesskey) {
      const dlUrl = `https://lyrics.kugou.com/download?fmt=lrc&charset=utf8&client=pc&ver=1&id=${cand.id}&accesskey=${cand.accesskey}`;
      const dlRes = await fetch(dlUrl);
      const dlData = await dlRes.json();
      if (dlData?.content) {
        return Buffer.from(dlData.content, 'base64').toString('utf8');
      }
    }
  }
  throw new Error("KuGou lyrics not found");
});

ipcMain.handle('get_paxsenix_lyrics', async (_, args = {}) => {
  const encTitle = encodeURIComponent(args.title || "");
  const encArtist = encodeURIComponent(args.artist || "");
  let url = `https://lyrics.paxsenix.org/apple-music/lyrics?title=${encTitle}&artist=${encArtist}`;
  if (args.duration && args.duration > 0) url += `&duration=${args.duration}`;
  const res = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0" } });
  return await res.text();
});

ipcMain.handle('get_ytm_lyrics', async (_, args = {}) => {
  const videoId = args.videoId || args.video_id || "";
  const nextRaw = await fetchInnerTube('next', {
    context: getClientContext(),
    videoId
  });
  const nextJson = JSON.parse(nextRaw);
  const tabs = nextJson?.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs || [];
  let browseId = null;
  for (const t of tabs) {
    const pageType = t?.tabRenderer?.endpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType;
    if (pageType === "MUSIC_PAGE_TYPE_TRACK_LYRICS") {
      browseId = t?.tabRenderer?.endpoint?.browseEndpoint?.browseId;
      break;
    }
  }
  if (!browseId) throw new Error("Track lyrics not available on YouTube Music");
  const browseRaw = await fetchInnerTube('browse', {
    context: getClientContext(),
    browseId
  });
  const browseJson = JSON.parse(browseRaw);
  const runs = browseJson?.contents?.sectionListRenderer?.contents?.[0]?.musicDescriptionShelfRenderer?.description?.runs || [];
  const text = runs.map(r => r.text || "").join("");
  if (!text.trim()) throw new Error("Empty lyrics");
  return text;
});

ipcMain.handle('get_captions_lyrics', async (_, args = {}) => {
  const videoId = args.videoId || args.video_id || "";
  const paramsBytes = Buffer.concat([Buffer.from([0x0a, videoId.length]), Buffer.from(videoId)]);
  const paramsB64 = paramsBytes.toString('base64');
  const ytClientApiKey = Buffer.from("QUl6YVN5QzlYTDNaaldkZFh5YTZYNzRkSm9DVEwtV0VZRkROWDMw", "base64").toString("utf-8");
  const url = `https://music.youtube.com/youtubei/v1/get_transcript?key=${ytClientApiKey}`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "User-Agent": "Mozilla/5.0",
      "Origin": "https://music.youtube.com",
      "Referer": "https://music.youtube.com/"
    },
    body: JSON.stringify({
      context: getClientContext(),
      params: paramsB64
    })
  });
  const parsed = await res.json();
  const cueGroups = parsed?.actions?.[0]?.updateEngagementPanelAction?.content?.transcriptRenderer?.body?.transcriptBodyRenderer?.cueGroups || [];
  const lines = [];
  for (const g of cueGroups) {
    const cue = g?.transcriptCueGroupRenderer?.cues?.[0]?.transcriptCueRenderer;
    if (cue) {
      const startMs = parseInt(cue.startOffsetMs || "0", 10);
      const text = (cue.cue?.simpleText || "").replace(/[♪ ]/g, '').trim();
      if (text) {
        const totalSec = Math.floor(startMs / 1000);
        const min = Math.floor(totalSec / 60);
        const sec = totalSec % 60;
        const ms = startMs % 1000;
        lines.push(`[${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}.${String(ms).padStart(3, '0')}]${text}`);
      }
    }
  }
  if (lines.length === 0) throw new Error("Empty transcript");
  return lines.join("\n");
});

// 4. Session & Authentication
ipcMain.handle('get_session', () => {
  const sess = loadSession();
  return JSON.stringify(sess || { is_logged_in: false });
});

ipcMain.handle('save_session', async (_, args = {}) => {
  const cookies = args.cookies || "";
  if (!cookies) return "error";

  let name = "Akun Google";
  let handle = null;
  let photo_url = null;

  try {
    const resp = await fetchInnerTube('account/account_menu', { context: getClientContext() });
    const json = JSON.parse(resp);
    const header = json?.actions?.[0]?.openPopupAction?.popup?.multiPageMenuRenderer?.header?.activeAccountHeaderRenderer;
    if (header) {
      name = header.accountName?.runs?.[0]?.text || name;
      handle = header.channelHandle?.runs?.[0]?.text || handle;
      const thumbs = header.accountPhoto?.thumbnails || [];
      if (thumbs.length > 0) photo_url = thumbs[thumbs.length - 1].url;
    }
  } catch (_) {}

  const sessionObj = {
    is_logged_in: true,
    name,
    handle,
    photo_url,
    cookies
  };
  saveSessionData(sessionObj);
  return "saved";
});

ipcMain.handle('logout', () => {
  const f = getSessionFile();
  if (fs.existsSync(f)) fs.unlinkSync(f);
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('session_updated', JSON.stringify({ is_logged_in: false }));
  }
  return true;
});

// Google OAuth
let oauthServer = null;
ipcMain.handle('start_google_oauth', async () => {
  return new Promise((resolve) => {
    try {
      if (oauthServer) {
        oauthServer.close();
        oauthServer = null;
      }

      oauthServer = http.createServer(async (req, res) => {
        const u = new URL(req.url, 'http://127.0.0.1:8089');
        if (u.pathname === '/callback') {
          const code = u.searchParams.get('code');
          if (code) {
            res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
            res.end(`<html><body style="background:#121212;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;text-align:center;"><div><h2>Login Berhasil!</h2><p>Silakan tutup tab ini dan kembali ke Spectro.</p></div><script>setTimeout(()=>window.close(),2000)</script></body></html>`);
            if (oauthServer) { oauthServer.close(); oauthServer = null; }

            // Exchange token
            try {
              const cfgFile = getOAuthConfigFile();
              let clientId = '282683342361-vktvvamcgldsm803mvhscflhh8iuhb22.apps.googleusercontent.com';
              let clientSecret = '';
              if (fs.existsSync(cfgFile)) {
                try {
                  const cfg = JSON.parse(fs.readFileSync(cfgFile, 'utf8'));
                  if (cfg.client_id) clientId = cfg.client_id;
                  if (cfg.client_secret) clientSecret = cfg.client_secret;
                } catch (_) {}
              }

              const bodyParams = {
                code,
                client_id: clientId,
                redirect_uri: 'http://127.0.0.1:8089/callback',
                grant_type: 'authorization_code'
              };
              if (clientSecret) bodyParams.client_secret = clientSecret;

              const tokenRes = await fetch('https://oauth2.googleapis.com/token', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(bodyParams)
              });
              const tokens = await tokenRes.json();
              if (tokens.access_token) {
                let name = "Akun Google";
                let photo = null;
                let email = null;
                try {
                  const infoRes = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
                    headers: { 'Authorization': `Bearer ${tokens.access_token}` }
                  });
                  const info = await infoRes.json();
                  name = info.name || name;
                  photo = info.picture || null;
                  email = info.email || null;
                } catch (_) {}

                const sessionObj = {
                  is_logged_in: true,
                  name,
                  handle: email,
                  photo_url: photo,
                  cookies: `oauth_token=${tokens.access_token}`
                };
                saveSessionData(sessionObj);
                resolve({ success: true, name, email, picture: photo });
                return;
              }
            } catch (err) {
              resolve({ success: false, error: String(err) });
              return;
            }
          }
        }
        res.writeHead(404);
        res.end();
      });

      oauthServer.listen(8089, '127.0.0.1', () => {
        const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=282683342361-vktvvamcgldsm803mvhscflhh8iuhb22.apps.googleusercontent.com&redirect_uri=http://127.0.0.1:8089/callback&response_type=code&scope=https://www.googleapis.com/auth/youtube.readonly%20https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email`;
        shell.openExternal(authUrl);
      });
    } catch (e) {
      resolve({ success: false, error: String(e) });
    }
  });
});

ipcMain.handle('cancel_google_oauth', () => {
  if (oauthServer) {
    oauthServer.close();
    oauthServer = null;
  }
  return true;
});

ipcMain.handle('get_oauth_config', () => {
  try {
    const f = getOAuthConfigFile();
    if (fs.existsSync(f)) {
      return JSON.parse(fs.readFileSync(f, 'utf8'));
    }
  } catch (_) {}
  return {
    client_id: '282683342361-vktvvamcgldsm803mvhscflhh8iuhb22.apps.googleusercontent.com',
    client_secret: ''
  };
});

ipcMain.handle('save_oauth_config', (_, args = {}) => {
  try {
    const f = getOAuthConfigFile();
    fs.writeFileSync(f, JSON.stringify({
      client_id: (args.client_id || args.clientId || '').trim(),
      client_secret: (args.client_secret || args.clientSecret || '').trim()
    }, null, 2), 'utf8');
    return true;
  } catch (_) {
    return false;
  }
});

ipcMain.handle('open_browser_url', (_, args = {}) => {
  const url = typeof args === 'string' ? args : (args.url || "");
  if (url) shell.openExternal(url);
  return true;
});

ipcMain.handle('open_google_login', () => {
  // Seamless popup window for YouTube login
  const authWin = new BrowserWindow({
    width: 500,
    height: 650,
    parent: mainWindow,
    modal: true,
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  authWin.loadURL('https://accounts.google.com/ServiceLogin?service=youtube&continue=https://music.youtube.com');

  const checkCookies = async () => {
    try {
      const cookies = await authWin.webContents.session.cookies.get({ domain: '.youtube.com' });
      const cookieStr = cookies.map(c => `${c.name}=${c.value}`).join('; ');
      if (cookieStr.includes('SAPISID=') || cookieStr.includes('__Secure-3PAPISID=')) {
        await ipcMain.emit('save_session', null, { cookies: cookieStr });
        if (!authWin.isDestroyed()) authWin.close();
        return true;
      }
    } catch (_) {}
    return false;
  };

  authWin.webContents.on('did-finish-load', () => {
    checkCookies();
  });
  return true;
});

ipcMain.handle('sync_browser_session', () => {
  throw new Error("Gunakan kode pairing Spectro Connect 6-digit untuk sinkronisasi instan");
});
