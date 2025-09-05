package com.example.audeonbrowser

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.webkit.WebView
import com.example.audeonbrowser.profile.ProfileManager

class WebViewInitProvider : ContentProvider() {
  override fun onCreate(): Boolean {
    val ctx = context ?: return true
    val active = ProfileManager.getActive(ctx) // default "default"
    // WAJIB: panggil sebelum ada WebView/CookieManager apa pun
    WebView.setDataDirectorySuffix(active)
    return true
  }
  override fun query(u: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
  override fun getType(u: Uri): String? = null
  override fun insert(u: Uri, v: ContentValues?): Uri? = null
  override fun delete(u: Uri, s: String?, a: Array<out String>?): Int = 0
  override fun update(u: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}