# Audio Cutter & Ringtone Maker Guide ✂️🎵

Zyvro features a built-in, hardware-accelerated **Audio Trimmer and Ringtone Maker**. This allows you to extract audio from any downloaded video or song, select your favorite segment (like the chorus or a punchline), and set it directly as your phone's ringtone or notification alert.

---

## 🚀 How It Works

Traditional apps re-encode the entire audio file through software decoders, degrading sound quality and consuming excessive battery. 

Zyvro uses Android's low-level **`MediaExtractor`** and **`MediaMuxer`** APIs:
* **Lossless Precision**: Audio samples are read, demuxed, and re-packeted without lossy transcode cycles.
* **Instant Processing**: Trimming a 30-second chorus from a 5-minute song takes **under 1 second**.
* **Zero Artifacts**: Preserves original sample rate, bit depth, and AAC/MP3 stereo channels.

---

## 📱 Step-by-Step Trimming Tutorial

### Step 1: Open Any Audio File
1. Go to the **Library** tab in Zyvro.
2. Select the **Audio** filter pill.
3. Tap on any audio track to open the playback player.
4. Tap the **✂️ Cut / Ringtone** icon in the player or the 3-dot menu.

### Step 2: Set Start and End Points
* An interactive waveform dialog appears showing the audio timeline.
* Drag the **Left Marker** to set the start timestamp (e.g. `00:45`).
* Drag the **Right Marker** to set the end timestamp (e.g. `01:15`).
* The total selection duration is displayed in real-time (e.g. `Duration: 30.0s`).

### Step 3: Preview the Clip
* Tap the **▶️ Play Preview** button.
* The player loops only the selected segment so you can ensure the cut begins and ends at the exact beat.

### Step 4: Export or Set as Ringtone
Choose from three direct actions:
1. **💾 Save to Ringtones**: Exports the trimmed clip to your phone's `/Ringtones` folder. It will immediately show up in your Android Sound Settings ringtone selector.
2. **🔔 Set as Notification**: Saves the clip to `/Notifications` so you can use it for SMS, WhatsApp, or app alerts.
3. **⭐ Set as Default Ringtone**: Sets the clip as your active phone ringtone immediately with 1 tap.

---

## 🔒 Permission Note for Setting System Ringtones

To set a sound as the active default ringtone without sending you to system menus, Android requires the `android.permission.WRITE_SETTINGS` permission.
* If you tap **Set as Default**, Zyvro will display a quick prompt.
* Toggle **Allow modifying system settings** ON once.
* Return to Zyvro, and the ringtone is applied instantly!
