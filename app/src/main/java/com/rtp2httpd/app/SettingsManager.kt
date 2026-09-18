package com.rtp2httpd.app

import android.content.Context
import android.net.Uri

object SettingsManager {
    private const val PREFS = "rtp2httpd_prefs"
    private const val KEY_SERVER_URL = "server_url"

    fun getServerUrl(ctx: Context): String? {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_SERVER_URL, null) ?: return null
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "http://$trimmed"
        } else {
            trimmed
        }.trimEnd('/')
    }

    fun setServerUrl(ctx: Context, url: String) {
        val trimmed = url.trim().trimEnd('/')
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SERVER_URL, trimmed).apply()
    }

    fun isValid(url: String): Boolean {
        val u = url.trim()
        if (u.isBlank()) return false
        val uri = runCatching { Uri.parse(u) }.getOrNull() ?: return false
        if (uri.scheme != "http" && uri.scheme != "https") return false
        val host = uri.host
        if (host.isNullOrBlank()) return false
        return true
    }
}
