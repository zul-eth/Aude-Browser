package com.example.audeonbrowser.profile

import android.content.Context

object ProfileManager {
  private const val PREF = "profiles"
  private const val KEY_ACTIVE = "active"
  private const val KEY_LIST = "list"
  private val ID_REGEX = Regex("^[a-z0-9_-]{1,32}$")

  private fun prefs(ctx: Context) =
    ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

  private fun ensureDefault(ctx: Context) {
    val p = prefs(ctx)
    val set = p.getStringSet(KEY_LIST, null)?.toMutableSet() ?: mutableSetOf()
    if (!set.contains("default")) {
      set += "default"
      // gunakan commit() agar tersimpan sebelum proses mati
      p.edit()
        .putStringSet(KEY_LIST, set)
        .putString(KEY_ACTIVE, p.getString(KEY_ACTIVE, "default") ?: "default")
        .commit()
    }
  }

  fun list(ctx: Context): List<String> {
    ensureDefault(ctx)
    return prefs(ctx).getStringSet(KEY_LIST, setOf("default"))!!.toList()
  }

  fun getActive(ctx: Context): String {
    ensureDefault(ctx)
    return prefs(ctx).getString(KEY_ACTIVE, "default")!!
  }

  fun setActive(ctx: Context, id: String) {
    ensureDefault(ctx)
    val set = prefs(ctx).getStringSet(KEY_LIST, setOf("default"))!!
    require(set.contains(id)) { "Profil tidak ada" }
    // commit sinkron
    prefs(ctx).edit().putString(KEY_ACTIVE, id).commit()
  }

  fun validate(id: String) = ID_REGEX.matches(id)

  fun create(ctx: Context, id: String): Boolean {
    if (!validate(id)) return false
    ensureDefault(ctx)
    val set = prefs(ctx).getStringSet(KEY_LIST, setOf("default"))!!.toMutableSet()
    if (!set.add(id)) return false
    // commit sinkron
    return prefs(ctx).edit().putStringSet(KEY_LIST, set).commit()
  }

  fun delete(ctx: Context, id: String): Boolean {
    ensureDefault(ctx)
    if (id == getActive(ctx)) return false
    val set = prefs(ctx).getStringSet(KEY_LIST, setOf("default"))!!.toMutableSet()
    val ok = set.remove(id)
    if (!ok) return false
    // commit sinkron
    return prefs(ctx).edit().putStringSet(KEY_LIST, set).commit()
  }
}