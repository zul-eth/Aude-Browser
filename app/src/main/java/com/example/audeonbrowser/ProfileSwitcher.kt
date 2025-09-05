package com.example.audeonbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.audeonbrowser.profile.ProfileManager
import kotlin.system.exitProcess

object ProfileSwitcher {

  fun switchTo(ctx: Context, id: String) {
    // 1) Simpan aktif (sinkron di ProfileManager kamu)
    ProfileManager.setActive(ctx, id)

    // 2) Minta proses lain (relauncher) untuk buka EntryActivity
    val relaunch = Intent(RelaunchReceiver.ACTION).apply {
      setClass(ctx, RelaunchReceiver::class.java) // explicit broadcast
    }
    ctx.sendBroadcast(relaunch)

    // 3) Tutup task & kill proses → WebView re-init dengan suffix baru
    if (ctx is Activity) ctx.finishAffinity()
    android.os.Process.killProcess(android.os.Process.myPid())
    exitProcess(0)
  }
}