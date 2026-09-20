# Cookies & Authentication Guide 🍪

Certain online content requires authentication to be downloaded, including:
* Age-restricted YouTube videos.
* Private or followers-only Instagram accounts.
* Member-only community posts or livestreams.
* Higher quality streams restricted to logged-in users.

Zyvro includes built-in support for importing standard Netscape-formatted `cookies.txt` files.

---

## 🔒 Privacy & Local Security

* **100% Local**: Your cookies are stored exclusively inside Zyvro's sandboxed private app storage (`/data/user/0/com.zyvro.app/files/cookies.txt`).
* **No Telemetry**: Cookies are never uploaded to any remote server or third-party service. They are only passed locally to the sandboxed `yt-dlp` executable.
* **Instant Deletion**: You can delete the stored cookies file at any time from **Settings > Clear Cookies**.

---

## 🛠️ How to Export and Import `cookies.txt`

### Step 1: Export Cookies from Your Browser
1. On your PC (or mobile browser that supports extensions like Kiwi or Firefox Mobile), install a reputable cookie export extension such as:
   - **"Get cookies.txt LOCALLY"** (Open Source, Chrome Web Store & Firefox Add-ons).
2. Visit [YouTube.com](https://www.youtube.com) or [Instagram.com](https://www.instagram.com) and ensure you are logged into your account.
3. Open the extension and click **Export Cookies** / **Download as cookies.txt**.

### Step 2: Transfer to Your Phone
* Send the downloaded `cookies.txt` file to your phone via USB cable, Google Drive, Telegram Saved Messages, or local Wi-Fi transfer.

### Step 3: Import into Zyvro
1. Open **Zyvro**.
2. Tap on the **Settings** gear icon.
3. Scroll down to **Advanced / Authentication**.
4. Tap **Import Cookies File**.
5. Select your `cookies.txt` file using the Android system document picker.
6. A success snackbar will display: *"Cookies imported successfully (X KB)"*.

---

## ⚠️ Best Practices

1. **Do Not Share**: Never share your `cookies.txt` file with anyone. Anyone with your cookies can access your account session.
2. **Session Expiry**: If you log out of the account in your browser, the session token will become invalid. If downloads fail with an authentication error, re-export fresh cookies.
