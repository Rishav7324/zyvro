package com.zyvro.app.engine

/**
 * Shared yt-dlp error mapper used by BOTH metadata fetch and actual
 * downloads, so Home errors and Queue/Library FAILED cards speak the same
 * language. Messages carry a machine prefix (LOGIN_REQUIRED:, RATE_LIMITED:,
 * UNSUPPORTED:, NETWORK:, FETCH_FAILED:) followed by a Hinglish explanation
 * the UI can show directly.
 */
object DownloadErrors {

    fun isLoginWall(msg: String): Boolean {
        val m = msg.lowercase()
        return m.contains("login") || m.contains("log in") ||
                m.contains("cookies") || m.contains("private") ||
                m.contains("rate-limit") || m.contains("redirect") ||
                m.contains("challenge") || m.contains("checkpoint")
    }

    fun friendlyMessage(rawInput: String?, hasCookies: Boolean): String {
        val raw = rawInput?.trim().orEmpty()
        val lower = raw.lowercase()
        return when {
            raw.isBlank() ->
                "FETCH_FAILED: Could not read this media link. URL verify karke retry karo."
            lower.contains("login") || lower.contains("log in") ||
                    lower.contains("cookies") || lower.contains("challenge") ||
                    lower.contains("checkpoint") -> {
                if (hasCookies) {
                    "LOGIN_REQUIRED: Cookies ke baad bhi platform login maang raha hai. " +
                            "Link private/deleted ho sakta hai, ya cookies expire ho gaye. " +
                            "Fresh cookies.txt import karke retry karo. Detail: ${raw.take(200)}"
                } else {
                    "LOGIN_REQUIRED: Ye post login maang raha hai (private/gated). " +
                            "Public link try karo, ya Settings me cookies.txt import karke retry karo. " +
                            "Detail: ${raw.take(200)}"
                }
            }
            lower.contains("rate-limit") || lower.contains("too many requests") ||
                    lower.contains("429") || lower.contains("slow down") ->
                "RATE_LIMITED: Platform ne request limit lagayi hai. 1-2 min ruk kar retry karo. Detail: ${raw.take(200)}"
            lower.contains("unsupported url") || lower.contains("no video") ||
                    lower.contains("not available") || lower.contains("no media found") ||
                    lower.contains("empty playlist") || lower.contains("private video") ->
                "UNSUPPORTED: Ye link public video/photo nahi lag raha (deleted/private ya galat URL). Detail: ${raw.take(200)}"
            lower.contains("network") || lower.contains("timeout") ||
                    lower.contains("unknownhost") || lower.contains("unable to resolve") ||
                    lower.contains("connection") ->
                "NETWORK: Internet slow/unstable hai. Wi-Fi check karke retry karo. Detail: ${raw.take(200)}"
            lower.contains("ffmpeg") || lower.contains("merge") || lower.contains("mux") ->
                "FETCH_FAILED: Download ho gaya tha lekin file merge nahi hui (FFmpeg error). Retry karo. Detail: ${raw.take(200)}"
            else -> "FETCH_FAILED: ${raw.take(350)}"
        }
    }

    /** Short hint shown under the error, chosen from the typed prefix. */
    fun hintFor(message: String): String = when {
        message.startsWith("LOGIN_REQUIRED:") ->
            "Settings me cookies.txt import karo, phir Retry dabao."
        message.startsWith("RATE_LIMITED:") ->
            "1-2 min ruk kar Retry dabao — baar-baar tap karne se limit badhegi."
        message.startsWith("NETWORK:") ->
            "Wi-Fi/data check karke Retry dabao."
        message.startsWith("UNSUPPORTED:") ->
            "Link browser me khol kar verify karo ki post public hai."
        else ->
            "Retry dabao — aksar dusri attempt me stream mil jata hai."
    }
}
