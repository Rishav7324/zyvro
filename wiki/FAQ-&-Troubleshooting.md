# Frequently Asked Questions & Troubleshooting ❓

Find quick answers to common questions and troubleshooting steps for Zyvro.

---

## 🔍 Common Issues & Solutions

### 1. "Download Failed / Extractor Error"
* **Cause**: Platforms like YouTube and Instagram frequently change their internal video player APIs.
* **Fix**:
  1. Open Zyvro **Settings**.
  2. Tap **Update yt-dlp Engine**.
  3. Wait for the update confirmation snackbar.
  4. Retry your download.

### 2. Instagram Reels or Posts Fail to Download
* **Cause**: The post may be private, age-restricted, or blocked by Meta's login wall.
* **Fix**:
  1. Verify the link is from a **public account**.
  2. If the link contains tracking tags (e.g. `?igsh=...`), Zyvro strips them automatically, but ensure you copied the direct reel/post URL.
  3. Import your `cookies.txt` in **Settings > Import Cookies** to access private or follower-only media.

### 3. Downloads Stop When Screen Turns Off
* **Cause**: Android OEM battery optimization (common on Xiaomi/MIUI, Samsung OneUI, OnePlus/OxygenOS) aggressively terminates background foreground services.
* **Fix**:
  1. Open Android **Settings > Apps > Zyvro**.
  2. Tap **Battery** or **App Battery Usage**.
  3. Select **Unrestricted** / **No restrictions**.
  4. Ensure Zyvro is allowed to run in the background.

### 4. Downloaded Files Don't Appear in Gallery / Music Player
* **Cause**: On Android 10+, files saved without MediaStore indexing may not be immediately scanned by third-party gallery apps.
* **Fix**:
  - In Zyvro v3.0.0, all downloads are automatically registered with Android's system `MediaStore`.
  - If using a custom download directory on an SD card, grant storage access permissions when prompted.

### 5. "Cannot set as default ringtone"
* **Cause**: Android requires explicit system settings write permission for changing system ringtones.
* **Fix**:
  - In the prompt, tap **Allow modifying system settings**. If already granted, ensure your device has a default SIM card configured.

---

## 💬 Frequently Asked Questions

#### Q: Is Zyvro completely free and open source?
**A:** Yes! Zyvro is 100% free, open source, and licensed under the GNU General Public License v3 (GPLv3). There are no ads, subscriptions, tracking, or telemetry.

#### Q: Where are downloaded files saved?
**A:** By default:
* **Videos**: `Movies/Zyvro` or `Download/Zyvro`
* **Audio**: `Music/Zyvro`
* **Photos**: `Pictures/Zyvro`
* **Trimmed Ringtones**: `Ringtones/`
You can customize the download directory in **Settings > Storage**.

#### Q: Can I limit download speeds?
**A:** Yes! Navigate to **Settings > Speed Limiter** and select from 500 KB/s, 2 MB/s, 5 MB/s, 10 MB/s, or Unlimited.
