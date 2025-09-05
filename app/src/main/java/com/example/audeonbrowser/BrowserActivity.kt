package com.example.audeonbrowser

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.example.audeonbrowser.profile.ProfileManager
import com.example.audeonbrowser.util.ThemeManager

data class Tab(val web: WebView, var url: String = "")

class BrowserActivity : AppCompatActivity() {

  private lateinit var toolbar: MaterialToolbar
  private lateinit var swipe: SwipeRefreshLayout
  private lateinit var webContainer: FrameLayout
  private lateinit var urlInput: EditText
  private lateinit var urlLayout: TextInputLayout
  private lateinit var tabCountView: TextView

  private val tabs = mutableListOf<Tab>()
  private var currentTab = -1

  private var desktopMode = false
  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private var ntpView: View? = null

  private val recentHosts = ArrayDeque<String>()
  private val MAX_RECENT = 12

  private val pickLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      val result = WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data)
      filePathCallback?.onReceiveValue(result)
      filePathCallback = null
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    ThemeManager.applySaved(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_browser)

    toolbar      = findViewById(R.id.toolbar)
    swipe        = findViewById(R.id.swipe)
    webContainer = findViewById(R.id.webContainer)
    urlInput     = findViewById(R.id.urlInput)
    urlLayout    = findViewById(R.id.urlLayout)

    toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_home)
    toolbar.setNavigationOnClickListener { showNewTab() }
    toolbar.setOnMenuItemClickListener { onToolbarAction(it) }

    // set ikon & judul menu tema sesuai state
    toolbar.menu.findItem(R.id.action_theme)?.apply {
      icon  = ContextCompat.getDrawable(this@BrowserActivity, ThemeManager.menuIconRes(this@BrowserActivity))
      title = ThemeManager.menuTitle(this@BrowserActivity)
    }

    val tabsItem = toolbar.menu.findItem(R.id.action_tabs)
    val actionView = requireNotNull(tabsItem.actionView) {
      "Menu item action_tabs harus pakai actionLayout=@layout/action_tab_counter"
    }
    tabCountView = actionView.findViewById(R.id.tabCount)
    tabCountView.setOnClickListener { showTabsSheet() }

    swipe.setOnRefreshListener { currentTab().web.reload() }
    swipe.setOnChildScrollUpCallback { _, _ ->
      tabs.isNotEmpty() && ntpView == null && currentTab().web.canScrollVertically(-1)
    }

    urlInput.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
        openFromAddressBar(); true
      } else false
    }

    urlLayout.setStartIconOnClickListener { showSiteInfo() }

    addNewTab(initialUrl = null)
    showNewTab()
  }

  private fun buildWebView(): WebView {
    val wv = WebView(this)
    wv.setOnScrollChangeListener { _, _, y, _, _ -> swipe.isEnabled = (y == 0 && ntpView == null) }
    wv.layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    )

    with(wv.settings) {
      javaScriptEnabled = true
      domStorageEnabled = true
      mediaPlaybackRequiresUserGesture = true
      mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
      setSupportMultipleWindows(true)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        forceDark = if (ThemeManager.isDark(this@BrowserActivity))
          WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
      }
    }
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

    wv.webViewClient = object : WebViewClient() {
      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        hideNewTabIfShown()
        urlLayout.visibility = View.VISIBLE
        swipe.isRefreshing = true
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        swipe.isRefreshing = false
        if (!url.isNullOrEmpty() && url != "about:blank") {
          urlInput.setText(url)
          currentTab().url = url

          val host = try { Uri.parse(url).host ?: "" } catch (_: Exception) { "" }
          if (host.isNotBlank()) {
            recentHosts.remove(host)
            recentHosts.addFirst(host)
            while (recentHosts.size > MAX_RECENT) recentHosts.removeLast()
          }
        }

        val iconRes = when {
          ntpView != null -> R.drawable.ic_google
          url?.startsWith("https://") == true -> R.drawable.ic_lock
          url.isNullOrEmpty() -> R.drawable.ic_search
          else -> R.drawable.ic_warning
        }
        urlLayout.startIconDrawable = ContextCompat.getDrawable(this@BrowserActivity, iconRes)
      }

      override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
        val u = r?.url ?: return false
        return if (u.scheme == "http" || u.scheme == "https") false
               else { startActivity(Intent(Intent.ACTION_VIEW, u)); true }
      }

      override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
        h?.cancel()
      }
    }

    wv.webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        swipe.isRefreshing = (ntpView == null) && newProgress < 100
      }

      override fun onShowFileChooser(
        wv: WebView?,
        filePath: ValueCallback<Array<Uri>>?,
        params: FileChooserParams?
      ): Boolean {
        filePathCallback = filePath
        val intent = params?.createIntent() ?: return false.also {
          filePathCallback?.onReceiveValue(null); filePathCallback = null
        }
        return try { pickLauncher.launch(intent); true }
        catch (_: ActivityNotFoundException) {
          filePathCallback?.onReceiveValue(null); filePathCallback = null; false
        }
      }

      override fun onCreateWindow(
        view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
      ): Boolean {
        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
        transport.webView = currentTab().web
        resultMsg.sendToTarget()
        return true
      }
    }

    wv.setDownloadListener { url, userAgent, _, mimeType, _ ->
      ensureStoragePermissionIfNeeded()
      val req = DownloadManager.Request(Uri.parse(url))
        .setMimeType(mimeType)
        .addRequestHeader("User-Agent", userAgent)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      val fileName = Uri.parse(url).lastPathSegment ?: "download"
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        req.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
      } else {
        @Suppress("DEPRECATION")
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
      }
      (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
    }

    return wv
  }

  private fun showNewTab() {
    webContainer.removeAllViews()
    swipe.isEnabled = false
    ntpView = layoutInflater.inflate(R.layout.view_new_tab, webContainer, false)
    webContainer.addView(ntpView)

    urlLayout.visibility = View.GONE
    urlInput.setText("")
    urlLayout.startIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_google)

    val ntpInput = ntpView!!.findViewById<EditText>(R.id.ntpSearchInput)
    val btnGo = ntpView!!.findViewById<View>(R.id.ntpBtnGo)

    ntpInput.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        btnGo.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
      }
      override fun afterTextChanged(s: Editable?) {}
    })

    ntpInput.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO) {
        triggerNtpSearch(ntpInput); true
      } else false
    }
    btnGo.setOnClickListener { triggerNtpSearch(ntpInput) }

    renderRecents()
  }

  private fun triggerNtpSearch(ntpInput: EditText) {
    val text = ntpInput.text?.toString().orEmpty().trim()
    if (text.isEmpty()) return
    val url = normalize(text)
    hideNewTabIfShown()
    currentTab().web.loadUrl(url)
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
      .hideSoftInputFromWindow(ntpInput.windowToken, 0)
    currentTab().web.requestFocus()
  }

  private fun renderRecents() {
    val label = ntpView!!.findViewById<TextView>(R.id.labelRecents)
    val container = ntpView!!.findViewById<android.widget.LinearLayout>(R.id.ntpRecents)
    container.removeAllViews()

    if (recentHosts.isEmpty()) {
      label.visibility = View.GONE
      return
    }
    label.visibility = View.VISIBLE

    val pad = (8 * resources.displayMetrics.density).toInt()
    recentHosts.forEach { host ->
      val v = TextView(this).apply {
        text = host
        setPadding(pad * 2, pad, pad * 2, pad)
        setBackgroundResource(R.drawable.bg_search_chip)
        setTextColor(0xFF000000.toInt())
        textSize = 13f
      }
      val lp = android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
      )
      lp.rightMargin = pad
      v.layoutParams = lp
      v.setOnClickListener {
        hideNewTabIfShown()
        currentTab().web.loadUrl("https://$host")
      }
      container.addView(v)
    }
  }

  private fun hideNewTabIfShown() {
    if (ntpView != null) {
      webContainer.removeView(ntpView)
      ntpView = null
      swipe.isEnabled = true
      urlLayout.visibility = View.VISIBLE
      attachWeb(currentTab().web)
    }
  }

  private fun attachWeb(web: WebView) {
    if (ntpView == null) {
      webContainer.removeAllViews()
      webContainer.addView(web)
    }
  }

  private fun currentTab(): Tab = tabs[currentTab]

  private fun addNewTab(initialUrl: String?) {
    val wv = buildWebView()
    val tab = Tab(wv)
    tabs.add(tab)
    currentTab = tabs.lastIndex
    updateTabCounter()

    if (initialUrl.isNullOrEmpty()) {
      showNewTab()
    } else {
      hideNewTabIfShown()
      attachWeb(wv)
      wv.loadUrl(initialUrl)
    }
  }

  private fun switchTo(index: Int) {
    if (index == currentTab || index !in tabs.indices) return
    currentTab = index
    attachWeb(currentTab().web)
    urlInput.setText(currentTab().url)
    updateTabCounter()
  }

  private fun closeCurrentTab() {
    if (tabs.isEmpty()) return
    val idx = currentTab
    tabs[idx].web.destroy()
    tabs.removeAt(idx)
    if (tabs.isEmpty()) {
      addNewTab(null)
    } else {
      currentTab = (idx - 1).coerceAtLeast(0)
      attachWeb(currentTab().web)
    }
    updateTabCounter()
  }

  private fun updateTabCounter() {
    tabCountView.text = tabs.size.toString()
  }

  private fun showTabsSheet() {
    val titles = tabs.mapIndexed { i, t ->
      val u = t.url.ifBlank { "about:blank" }
      "${if (i == currentTab) "• " else ""}${u.take(45)}"
    }.toTypedArray()

    AlertDialog.Builder(this)
      .setTitle("Tabs")
      .setItems(titles) { _, which -> switchTo(which) }
      .setPositiveButton("Tab Baru") { _, _ -> addNewTab(null) }
      .setNegativeButton("Tutup Tab Ini") { _, _ -> closeCurrentTab() }
      .show()
  }

  private fun normalize(input: String): String {
    val t = input.trim()
    if (t.isEmpty()) return "about:blank"
    if (t.startsWith("http://") || t.startsWith("https://")) return t
    val looksLikeHost = Regex("""^[\w.-]+\.[a-z]{2,}(/.*)?$""", RegexOption.IGNORE_CASE)
    if (looksLikeHost.matches(t)) return "https://$t"
    return "https://www.google.com/search?hl=id&q=" + Uri.encode(t)
  }

  private fun openFromAddressBar() {
    val text = urlInput.text?.toString().orEmpty().trim()
    if (text.isEmpty()) return
    val url = normalize(text)
    hideNewTabIfShown()
    currentTab().web.loadUrl(url)
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
      .hideSoftInputFromWindow(urlInput.windowToken, 0)
    currentTab().web.requestFocus()
  }

  private fun onToolbarAction(item: MenuItem): Boolean = when (item.itemId) {
    R.id.action_back -> { val w = currentTab().web; if (w.canGoBack()) w.goBack(); true }
    R.id.action_forward -> { val w = currentTab().web; if (w.canGoForward()) w.goForward(); true }
    R.id.action_reload -> { currentTab().web.reload(); true }
    R.id.action_share -> { shareCurrent(); true }
    R.id.action_open_external -> { openExternal(); true }
    R.id.action_desktop -> { toggleDesktopMode(); true }
    R.id.action_profiles -> { showProfilesDialog(); true }
    R.id.action_tabs -> { showTabsSheet(); true }
    R.id.action_theme -> {
      ThemeManager.toggle(this)
      toolbar.menu.findItem(R.id.action_theme)?.apply {
        setIcon(ThemeManager.menuIconRes(this@BrowserActivity))
        title = ThemeManager.menuTitle(this@BrowserActivity)
      }
      recreate()
      true
    }
    else -> false
  }

  private fun shareCurrent() {
    val u = currentTab().web.url ?: return
    startActivity(Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"; putExtra(Intent.EXTRA_TEXT, u)
    })
  }

  private fun openExternal() {
    val u = currentTab().web.url ?: return
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
  }

  private fun toggleDesktopMode() {
    desktopMode = !desktopMode
    val w = currentTab().web
    val ua = w.settings.userAgentString
    val newUA = if (desktopMode) ua.replace("Mobile", "") else null
    w.settings.userAgentString = newUA ?: WebSettings.getDefaultUserAgent(this)
    w.reload()
  }

  private fun ensureStoragePermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
      if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(perm), 42)
      }
    }
  }

  private fun showProfilesDialog() {
    val profiles = ProfileManager.list(this).sorted()
    val active = ProfileManager.getActive(this)
    AlertDialog.Builder(this)
      .setTitle("Profiles (aktif: $active)")
      .setItems(profiles.toTypedArray()) { _, which ->
        val picked = profiles[which]
        if (picked != active) ProfileSwitcher.switchTo(this, picked)
      }
      .setPositiveButton("Tambah") { _, _ ->
        val input = android.widget.EditText(this).apply { hint = "profil_id" }
        AlertDialog.Builder(this)
          .setTitle("Buat Profil")
          .setView(input)
          .setPositiveButton("OK") { _, _ ->
            val id = input.text.toString().trim()
            // ProfileManager.create + switch seperti sebelumnya
            // (biarkan sesuai implementasi kamu)
            if (ProfileManager.create(this, id)) {
              ProfileSwitcher.switchTo(this, id)
            } else toast("ID tidak valid / sudah ada")
          }
          .setNegativeButton("Batal", null)
          .show()
      }
      .setNegativeButton("Batal", null)
      .show()
  }

  private fun toast(s: String) =
    android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show()

  private fun showSiteInfo() {
    val url = currentTab().web.url.orEmpty()
    if (url.isEmpty()) return
    val isHttps = url.startsWith("https://")
    val host = try { Uri.parse(url).host ?: url } catch (_: Exception) { url }
    val cookies = try { CookieManager.getInstance().getCookie(url).orEmpty() } catch (_: Exception) { "" }
    val cookieCount = if (cookies.isEmpty()) 0 else cookies.split(";").size
    val msg = "Situs: $host\nKoneksi: ${if (isHttps) "Aman (HTTPS)" else "Tidak aman (HTTP)"}\nCookies: $cookieCount item"
    AlertDialog.Builder(this)
      .setTitle("Info Situs")
      .setMessage(msg)
      .setPositiveButton("OK", null)
      .setNeutralButton("Salin URL") { _, _ ->
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("URL", url))
        toast("URL tersalin")
      }
      .setNegativeButton("Buka eksternal") { _, _ -> openExternal() }
      .show()
  }

  override fun onBackPressed() {
    if (ntpView != null) {
      hideNewTabIfShown()
      attachWeb(currentTab().web)
      return
    }
    if (tabs.isNotEmpty() && currentTab().web.canGoBack()) {
      currentTab().web.goBack()
    } else {
      super.onBackPressed()
    }
  }
}