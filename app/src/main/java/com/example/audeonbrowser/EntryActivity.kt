package com.example.audeonbrowser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class EntryActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startActivity(Intent(this, BrowserActivity::class.java))
    finish()
  }
}