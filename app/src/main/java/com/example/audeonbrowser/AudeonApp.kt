package com.example.audeonbrowser

import android.app.Application
import android.webkit.WebView
import com.example.audeonbrowser.profile.ProfileManager

class AudeonApp : Application() {
  override fun onCreate() {
    super.onCreate()
    // BACA profil aktif & set suffix SEBELUM ada WebView/CookieManager disentuh
    val active = ProfileManager.getActive(this) // default "default" kalau belum ada
    WebView.setDataDirectorySuffix(active)
  }
}