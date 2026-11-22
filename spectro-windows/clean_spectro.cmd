@echo off
chcp 65001 >nul
title Spectro - Pembersihan Sistem dan Cache

echo ======================================================================
echo                     SPECTRO WINDOWS CLEANER
echo ======================================================================
echo Script ini akan menutup semua proses Spectro yang sedang berjalan dan
echo membersihkan sisa instalasi serta cache lama sebelum memasang versi baru.
echo.

echo [1/3] Menutup semua proses Spectro yang sedang berjalan...
taskkill /F /IM "Spectro.exe" >nul 2>&1
taskkill /F /IM "spectro_core.exe" >nul 2>&1
taskkill /F /IM "Spectro-Setup.exe" >nul 2>&1
taskkill /F /IM "spectro-launcher.exe" >nul 2>&1
echo       - Selesai menutup proses aplikasi.

echo [2/3] Membersihkan direktori binary lama di LocalAppData...
if exist "%LOCALAPPDATA%\Spectro\bin" (
    rmdir /S /Q "%LOCALAPPDATA%\Spectro\bin" >nul 2>&1
    echo       - Berhasil menghapus %LOCALAPPDATA%\Spectro\bin
) else (
    echo       - Direktori binary sudah bersih.
)

echo [3/3] Membersihkan cache WebView2, Electron, dan file temporer...
if exist "%LOCALAPPDATA%\com.spectro.desktop\EBWebView" (
    rmdir /S /Q "%LOCALAPPDATA%\com.spectro.desktop\EBWebView\Default\Cache" >nul 2>&1
    rmdir /S /Q "%LOCALAPPDATA%\com.spectro.desktop\EBWebView\Default\Code Cache" >nul 2>&1
    rmdir /S /Q "%LOCALAPPDATA%\com.spectro.desktop\EBWebView\Default\GPUCache" >nul 2>&1
    echo       - Berhasil membersihkan cache WebView2.
)

if exist "%APPDATA%\Spectro\Cache" (
    rmdir /S /Q "%APPDATA%\Spectro\Cache" >nul 2>&1
    rmdir /S /Q "%APPDATA%\Spectro\Code Cache" >nul 2>&1
    rmdir /S /Q "%APPDATA%\Spectro\GPUCache" >nul 2>&1
    echo       - Berhasil membersihkan cache Electron.
)

if exist "%LOCALAPPDATA%\spectro-updater" (
    rmdir /S /Q "%LOCALAPPDATA%\spectro-updater" >nul 2>&1
)

del /Q "%TEMP%\spectro_*" >nul 2>&1

echo.
echo ======================================================================
echo [SUKSES] Seluruh sistem telah bersih tuntas!
echo Anda sekarang dapat menjalankan Spectro.exe atau memasang Spectro-Setup.exe yang baru.
echo ======================================================================
echo.
pause
