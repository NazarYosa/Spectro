const { contextBridge, ipcRenderer } = require('electron');

const windowControls = {
  minimize: () => ipcRenderer.invoke('window_minimize'),
  toggleMaximize: () => ipcRenderer.invoke('window_toggle_maximize'),
  close: () => ipcRenderer.invoke('window_close'),
  isMaximized: () => ipcRenderer.invoke('window_is_maximized')
};

const tauriBridge = {
  core: {
    invoke: (command, args = {}) => ipcRenderer.invoke(command, args)
  },
  invoke: (command, args = {}) => ipcRenderer.invoke(command, args),
  event: {
    listen: (eventName, cb) => {
      const listener = (_event, data) => cb({ event: eventName, payload: data });
      ipcRenderer.on(eventName, listener);
      return Promise.resolve(() => ipcRenderer.removeListener(eventName, listener));
    }
  },
  window: {
    getCurrentWindow: () => windowControls
  },
  webviewWindow: {
    getCurrentWebviewWindow: () => windowControls
  }
};

try {
  contextBridge.exposeInMainWorld('__TAURI__', tauriBridge);
  contextBridge.exposeInMainWorld('spectroWindow', windowControls);
} catch (_) {}

window.__TAURI__ = tauriBridge;
window.spectroWindow = windowControls;

window.__spectro_minimize = (e) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  return ipcRenderer.invoke('window_minimize');
};

window.__spectro_toggle_maximize = (e) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  return ipcRenderer.invoke('window_toggle_maximize');
};

window.__spectro_close = (e) => {
  if (e) { e.preventDefault(); e.stopPropagation(); }
  return ipcRenderer.invoke('window_close');
};

ipcRenderer.on('session_updated', (_event, data) => {
  window.dispatchEvent(new CustomEvent('session_updated', { detail: data }));
});
