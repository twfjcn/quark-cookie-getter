package com.quarkcookie.getter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var tvStatus: TextView
    lateinit var etCookie: EditText
    lateinit var btnGetCookie: Button
    lateinit var btnCopy: Button
    lateinit var btnReload: Button
    private val quarkUrl = "https://pan.quark.cn"
    private val TAG = "QuarkCookieGetter"

    // PC版UA，Windows Chrome
    private val pcUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        tvStatus = findViewById(R.id.tvStatus)
        etCookie = findViewById(R.id.etCookie)
        btnGetCookie = findViewById(R.id.btnGetCookie)
        btnCopy = findViewById(R.id.btnCopy)
        btnReload = findViewById(R.id.btnReload)
        initWebView()
        btnReload.setOnClickListener {
            webView.loadUrl(quarkUrl)
            etCookie.setText("")
            tvStatus.text = "状态：页面重新加载，请扫码登录"
        }
        btnGetCookie.setOnClickListener {
            extractQuarkCookie()
        }
        btnCopy.setOnClickListener {
            val cookieText = etCookie.text.toString().trim()
            if (cookieText.isBlank()) {
                Toast.makeText(this, "Cookie为空，先提取！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("quark_cookie", cookieText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
        webView.loadUrl(quarkUrl)
    }

    private fun initWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.loadsImagesAutomatically = true
        // ========== 核心：设置电脑UA ==========
        webSettings.userAgentString = pcUA

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // PC标识请求头，防止网站识别移动端
        val pcHeaders = mapOf(
            "Sec-CH-UA" to "\"Not=A?Brand\";v=\"99\", \"Chromium\";v=\"126\"",
            "Sec-CH-UA-Mobile" to "?0",
            "Sec-CH-UA-Platform" to "\"Windows\""
        )

        webView.webViewClient = object : WebViewClient() {
            // 跳转链接都带上PC请求头
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                view?.loadUrl(request.url.toString(), pcHeaders)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "页面加载完成 $url")
                tvStatus.text = "页面加载完毕，登录成功后点【提取Cookie】"
            }
        }
        WebView.setWebContentsDebuggingEnabled(true)
    }

    private fun extractQuarkCookie() {
        val cookieManager = CookieManager.getInstance()
        val rawAllCookie = CookieManager.getInstance().getCookie("https://pan.quark.cn") ?: ""
        Log.d(TAG, "raw cookie: $rawAllCookie")
        if (rawAllCookie.isBlank()) {
            tvStatus.text = "❌ 没有获取到Cookie，请先扫码登录！"
            etCookie.setText("")
            return
        }
        if(rawAllCookie.contains("__pus=")){
            tvStatus.text = "✅ 检测到 __pus 凭证，登录有效"
        }else{
            tvStatus.text = "⚠️ 未找到 __pus，登录可能失效，请重新扫码"
        }
        etCookie.setText(rawAllCookie)
        Toast.makeText(this, "Cookie提取完成", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
