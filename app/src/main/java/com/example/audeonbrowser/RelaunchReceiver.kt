package com.example.audeonbrowser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RelaunchReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION = "com.example.audeonbrowser.ACTION_RELAUNCH"
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION) return
    Log.i("Audeon", "RelaunchReceiver: launching EntryActivity")

    val launch = Intent(context, EntryActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(launch)
  }
}