package com.rtp2httpd.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.rtp2httpd.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentUrl: String? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var actionBarWasShown = false

    private val openSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // 设置页返回后，可能地址变了，重新加载
        loadOrShowEmpty()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(null) // 用 MaterialComponents 透明状态栏更自然

        binding.btnGoSettings.setOnClickListener { openSettingsPage() }
        binding.btnRetry.setOnClickListener { loadOrShowEmpty() }

        configureWebView(binding.webView)
        binding.webView.webViewClient = RtpWebViewClient(binding)
        binding.webView.webChromeClient = RtpChromeClient(this)

        loadOrShowEmpty()
    }

    private fun loadOrShowEmpty() {
        val url = SettingsManager.getServerUrl(this)
        if (url == null) {
            binding.emptyView.visibility = View.VISIBLE
            binding.errorView.visibility = View.GONE
            binding.webView.visibility = View.GONE
            return
        }
        currentUrl = url
        binding.emptyView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(true)
        s.builtInZoomControls = false  // rtp2httpd 内置播放器自己管缩放
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.userAgentString = "${s.userAgentString} Rtp2HttpdApp/1.0"

        // WebView 文件下载 → 交给系统
        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(Intent.createChooser(i, "下载文件"))
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 全屏播放时退出全屏
            if (customView != null) {
                customViewCallback?.onCustomViewHidden()
                return true
            }
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
        binding.webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        binding.webView.resumeTimers()
    }

    override fun onDestroy() {
        binding.webView.stopLoading()
        binding.webView.settings.javaScriptEnabled = false
        binding.webView.removeAllViews()
        binding.webView.destroy()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_refresh -> {
            binding.webView.reload()
            true
        }
        R.id.action_settings -> {
            openSettingsPage()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openSettingsPage() {
        openSettings.launch(Intent(this, SettingsActivity::class.java))
    }

    /** 处理 WebViewClient 的页面加载 / 错误 */
    private inner class RtpWebViewClient(private val b: ActivityMainBinding) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            // 只让 WebView 处理 http(s)
            return if (url.startsWith("http://") || url.startsWith("https://")) {
                false
            } else {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                true
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            b.webView.visibility = View.VISIBLE
            b.errorView.visibility = View.GONE
        }

        @Deprecated("Deprecated in Java")
        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            showError(description ?: "未知错误")
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (request?.isForMainFrame == true) {
                super.onReceivedError(view, request, error)
                showError(error?.description?.toString() ?: "网络错误")
            }
        }

        private fun showError(msg: String) {
            b.webView.visibility = View.GONE
            b.emptyView.visibility = View.GONE
            b.errorText.text = getString(R.string.server_unreachable) + "\n($msg)"
            b.errorView.visibility = View.VISIBLE
        }
    }

    /** 处理全屏视频播放 */
    private inner class RtpChromeClient(private val activity: MainActivity) : WebChromeClient() {
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
            actionBarWasShown = activity.supportActionBar?.isShowing == true
            activity.supportActionBar?.hide()
            view?.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    customViewCallback?.onCustomViewHidden()
                    true
                } else false
            }
            activity.setContentView(view)
        }

        override fun onHideCustomView() {
            if (customView == null) return
            activity.setContentView(binding.root)
            if (actionBarWasShown) activity.supportActionBar?.show()
            customView = null
            customViewCallback = null
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
        }
    }
}
