package com.example.audeonbrowser.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import com.example.audeonbrowser.R

object ThemeManager {
  private const val PREF = "theme_pref"
  private const val KEY = "is_dark"

  fun applySaved(context: Context) {
    val dark = isDark(context)
    AppCompatDelegate.setDefaultNightMode(
      if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    )
  }

  fun isDark(context: Context): Boolean =
    context.getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(KEY, false)

  fun toggle(context: Context) {
    val sp = context.getSharedPreferences(PREF, MODE_PRIVATE)
    val newVal = !sp.getBoolean(KEY, false)
    sp.edit().putBoolean(KEY, newVal).apply()
    AppCompatDelegate.setDefaultNightMode(
      if (newVal) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    )
  }

  fun menuTitle(context: Context): String =
    context.getString(if (isDark(context)) R.string.menu_theme_light else R.string.menu_theme_dark)

  fun menuIconRes(context: Context): Int =
    if (isDark(context)) R.drawable.ic_theme_light else R.drawable.ic_theme_dark
}