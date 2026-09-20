# Welcome to the Zyvro Wiki 🌟

**Zyvro** is a next-generation, high-performance Android media downloader, audio extractor, and offline media player built with **Jetpack Compose**, **Material 3 (Liquid Glass UI)**, **yt-dlp**, **FFmpeg**, **Aria2c**, and **AndroidX Media3**.

Whether you want to download 4K videos, save Instagram reels and carousels, extract high-bitrate audio, trim songs into custom ringtones, or listen to music in the background with lockscreen controls — Zyvro is built to handle it cleanly and privately.

---

## 📚 Table of Contents

| Section | Description |
| :--- | :--- |
| 🚀 **[Installation & Setup](Installation-&-Setup)** | How to install Zyvro APK, grant Android permissions, and configure storage. |
| 🌐 **[Supported Platforms & Formats](Supported-Platforms-&-Formats)** | Detailed breakdown of supported platforms (YouTube, Instagram, Facebook, Threads, Pinterest, TikTok, Reddit, etc.) and format selectors. |
| ✂️ **[Audio Cutter & Ringtone Guide](Audio-Cutter-&-Ringtone-Guide)** | How to trim audio losslessly and set tracks as system ringtones or notifications. |
| 🍪 **[Cookies & Authentication](Cookies-&-Authentication)** | How to import `cookies.txt` for age-restricted or member-only videos. |
| 🏗️ **[Architecture & Internals](Architecture-&-Internals)** | In-depth technical architecture: Compose M3, YtDlpEngine, Aria2c turbo, and MediaStore storage. |
| ❓ **[FAQ & Troubleshooting](FAQ-&-Troubleshooting)** | Solutions to common download failures, network errors, and background restrictions. |

---

## ⚡ Core Capabilities at a Glance

* **🎬 Universal Downloader**: Powered by `yt-dlp` supporting 1000+ sites with special optimizations for YouTube, Instagram, Facebook, Threads, and Pinterest.
* **🖼️ Instagram Media & Photos**: Fixes 302 login redirects, strips tracking IDs (`?igsh=`, `?mibextid=`), and supports full photo/carousel downloads.
* **🚀 Aria2c Multi-Connection Turbo**: Speeds up downloads up to 8x using multi-threaded fragment chunking.
* **✂️ Lossless Audio Cutter**: Hardware-accelerated trimming via `MediaExtractor` + `MediaMuxer` with 1-tap "Set as Ringtone".
* **🎧 Background Lockscreen Player**: Full playback controls with Android Media3 ExoPlayer notification service.
* **📂 Android 10–15 MediaStore Integration**: Instant gallery and music library indexing without file manager scans.
* **🎨 Liquid Glass Material 3 UI**: Clean, glass-morphic surfaces, dynamic color accents, and responsive animations.

---

## 🤝 Community & Support

* **Source Code**: [GitHub Repository](https://github.com/Rishav7324/zyvro)
* **Latest Releases**: [GitHub Releases](https://github.com/Rishav7324/zyvro/releases)
* **Issue Tracker**: [Report a Bug or Request a Feature](https://github.com/Rishav7324/zyvro/issues)
* **Maintainer**: [@Rishav7324](https://github.com/Rishav7324)
