# Supported Platforms & Formats Guide 🌐

**Zyvro** harnesses `yt-dlp` underneath, which inherently supports over **1,000+ video and audio portals**. In addition, Zyvro includes custom optimizations, regex query stripping, and headers tailored for popular social media and video sharing networks.

---

## 📱 Social & Video Platforms Matrix

| Platform | URL Patterns | Videos / Reels | Photos / Carousels | Audio Only | Special Engine Features |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **YouTube** | `youtube.com`, `youtu.be` | ✅ (Up to 4K/8K) | ❌ | ✅ (MP3/M4A/Opus) | Smart player client selection (`android`), thumbnail embedding, subtitle extraction. |
| **Instagram** | `instagram.com/reel/`, `/p/`, `/stories/` | ✅ | ✅ (Single & Multi-slide) | ✅ | Anti-redirect Chrome User-Agent, auto-stripping of `?igsh=` and `?xmt=`, progressive stream selector (`b/best`). |
| **Facebook** | `facebook.com`, `fb.watch` | ✅ (HD/SD) | ❌ | ✅ | Strips tracking queries (`fbclid`, `mibextid`, `sfnsn`), preserves `?v=` for Watch URLs. |
| **Threads** | `threads.net` | ✅ | ✅ | ✅ | Progressive media extraction without login requirement. |
| **Pinterest** | `pinterest.com`, `pin.it` | ✅ (Original MP4) | ✅ (Full HD) | ✅ | Short link expansion (`pin.it`) & query parameter clean up. |
| **TikTok** | `tiktok.com`, `vm.tiktok.com` | ✅ (Watermark-free) | ❌ | ✅ | Direct audio extractor & high-definition MP4. |
| **Twitter / X** | `twitter.com`, `x.com` | ✅ (Highest bitrate) | ❌ | ✅ | Video stream resolution selection (`720p`, `1080p`). |
| **Reddit** | `reddit.com/r/...` | ✅ | ❌ | ✅ | Seamless audio + video track merging via FFmpeg. |
| **SoundCloud** | `soundcloud.com` | ❌ | ❌ | ✅ (Original/HQ) | High quality audio extraction with album art metadata. |
| **Twitch** | `twitch.tv/videos/`, `clips` | ✅ | ❌ | ✅ | VODs and clips download with customizable speed limit. |
| **Bilibili** | `bilibili.com` | ✅ | ❌ | ✅ | Multi-resolution stream support with custom headers. |

---

## 🎚️ Format & Quality Selection

When you paste a link into Zyvro or share a link to Zyvro via the Android system Share sheet, Zyvro automatically parses available streams:

### 1. Photo / Image Tab
* Displayed when the target post contains photos, posters, or image carousels (e.g. Instagram Photo posts, Pinterest pins).
* Directly saves the full-resolution JPG/PNG image without converting or merging into video formats.

### 2. Video & Audio Tab
* **Best Quality (Auto)**: Automatically merges the highest available video stream (`bv*[height<=1080]`) with the highest available audio stream (`ba`), or uses progressive single-stream (`b/best`) where separated streams are not provided.
* **Granular Resolutions**: Choose between `4K (2160p)`, `2K (1440p)`, `1080p Full HD`, `720p HD`, `480p`, or `360p`.
* **Audio Extraction**:
  - Extracts only the audio track.
  - Choose between **MP3**, **M4A (AAC)**, **Opus**, or **FLAC**.
  - Automatically embeds metadata (Track Title, Artist, and Cover Artwork).

---

## ⚡ How to Download Using the Android Share Sheet

You don't need to manually copy and paste URLs:
1. In Instagram, YouTube, TikTok, or Twitter, tap the **Share** icon on any post.
2. Select **Zyvro** from the share list.
3. Zyvro will open automatically, normalize the link, fetch stream details, and display the **Format Selection Sheet**.
4. Tap your preferred format to immediately start downloading in the background.
