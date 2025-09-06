package com.audeon.browser.managers

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

data class ProfileMeta(val id: String, val createdAt: Long, val lastUsedAt: Long)

class ProfileManager private constructor(ctx: Context) {
    private val prefs: SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getActiveProfileIdOrDefault(): String {
        seedIfNeeded()
        return prefs.getString(KEY_ACTIVE, DEFAULT) ?: DEFAULT
    }

    fun list(): List<ProfileMeta> {
        seedIfNeeded()
        val json = prefs.getString(KEY_LIST, "[]") ?: "[]"
        val arr = JSONArray(json)
        val out = ArrayList<ProfileMeta>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                ProfileMeta(
                    id = o.getString("id"),
                    createdAt = o.getLong("createdAt"),
                    lastUsedAt = o.optLong("lastUsedAt", o.getLong("createdAt"))
                )
            )
        }
        return out
    }

    fun create(id: String): Boolean {
        if (!validateId(id)) return false
        val items = list().toMutableList()
        if (items.any { it.id == id }) return false

        val now = System.currentTimeMillis()
        items.add(ProfileMeta(id, now, now))

        // Sinkron karena biasanya setelah create → switch → relaunch.
        save(items, sync = true)
        return true
    }

    fun switchTo(id: String): Boolean {
        val items = list()
        if (items.none { it.id == id }) return false

        val now = System.currentTimeMillis()
        val updated = items.map { if (it.id == id) it.copy(lastUsedAt = now) else it }

        // Sinkron agar timestamp dan list tersimpan sebelum kill process.
        save(updated, sync = true)

        // Simpan active ID secara sinkron juga.
        return prefs.edit().putString(KEY_ACTIVE, id).commit()
    }

    fun delete(id: String): Boolean {
        if (id == getActiveProfileIdOrDefault()) return false
        val items = list()
        val newItems = items.filter { it.id != id }
        if (newItems.size == items.size) return false

        // Sinkron agar perubahan terlihat setelah relaunch.
        save(newItems, sync = true)
        return true
    }

    private fun save(items: List<ProfileMeta>, sync: Boolean = false) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("createdAt", it.createdAt)
                put("lastUsedAt", it.lastUsedAt)
            })
        }
        val editor = prefs.edit().putString(KEY_LIST, arr.toString())
        if (sync) editor.commit() else editor.apply()
    }

    private fun seedIfNeeded() {
        if (!prefs.contains(KEY_LIST)) {
            val now = System.currentTimeMillis()
            val arr = JSONArray().put(JSONObject().apply {
                put("id", DEFAULT)
                put("createdAt", now)
                put("lastUsedAt", now)
            })
            // Tidak perlu sinkron di sini karena tidak langsung kill process.
            prefs.edit()
                .putString(KEY_LIST, arr.toString())
                .putString(KEY_ACTIVE, DEFAULT)
                .apply()
        }
    }

    companion object {
        private const val PREFS = "profiles_prefs"
        private const val KEY_LIST = "profiles_json"
        private const val KEY_ACTIVE = "active_id"
        private const val DEFAULT = "default"

        // kita pakai lowercase saja (id dibuat otomatis di UI)
        private val PATTERN = Pattern.compile("^profil-[a-z0-9_-]{1,32}$|^default$")

        fun validateId(id: String) = PATTERN.matcher(id).matches()

        @Volatile private var I: ProfileManager? = null
        fun get(ctx: Context) =
            I ?: synchronized(this) { I ?: ProfileManager(ctx).also { I = it } }
    }
}