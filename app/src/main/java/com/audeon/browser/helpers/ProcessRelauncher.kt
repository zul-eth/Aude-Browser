package com.audeon.browser.helpers

import android.app.Activity
import android.content.Intent
import android.os.Process

object ProcessRelauncher {
    fun relaunch(activity: Activity) {
        val ctx = activity.applicationContext
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.finishAffinity()
        if (launch != null) ctx.startActivity(launch)
        Process.killProcess(Process.myPid())
    }
}