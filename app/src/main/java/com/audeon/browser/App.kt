package com.audeon.browser

import android.app.Application
import android.os.Build
import android.webkit.WebView
import com.audeon.browser.managers.ProfileManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1) Ambil profil aktif (seed "default" jika belum ada).
        val active = ProfileManager.get(this).getActiveProfileIdOrDefault()

        // 2) Set suffix PALING AWAL (API 28+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(active)
        }
        // Setelah ini, aman bila Activity memanggil CookieManager/WebView.
    }
}