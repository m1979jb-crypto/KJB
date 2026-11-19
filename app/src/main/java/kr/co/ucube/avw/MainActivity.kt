package kr.co.ucube.avw

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    @Volatile private var atTop = true

    private val pickFiles = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val cb = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        cb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data))
    }
    private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        web = findViewById(R.id.web)
        swipe = findViewById(R.id.swipe)
        progress = findViewById(R.id.progress)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            setSupportZoom(false)
            userAgentString = "$userAgentString AVWApp/2.0"
        }
        web.addJavascriptInterface(Bridge(), "AVWApp")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val base = Uri.parse(Prefs.server(this@MainActivity))
                val u = req.url
                if (u.host == base.host && u.port == base.port) return false
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, u)) }
                return true
            }
            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                view.evaluateJavascript(
                    """
                    window.AVW_NATIVE = true;
                    window.print = function(){ AVWApp.print(document.title || 'AI VISION WORKS'); };
                    (function(){
                      var report = function(){
                        var c = document.querySelector('.content');
                        var busy = /messenger|mail/.test(location.hash) || document.querySelector('.overlay,.drawer');
                        AVWApp.setAtTop(!busy && (!c || c.scrollTop <= 0) && window.scrollY <= 0);
                      };
                      document.addEventListener('scroll', report, true);
                      window.addEventListener('hashchange', function(){ setTimeout(report, 300); });
                      setInterval(report, 1000); report();
                    })();
                    """.trimIndent(), null
                )
                CookieManager.getInstance().flush()
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (req.isForMainFrame) {
                    progress.visibility = View.GONE
                    swipe.isRefreshing = false
                    showOffline("서버(${Prefs.server(this@MainActivity)})에 연결할 수 없어요.\n회사 와이파이/인터넷 연결이나 서버 주소를 확인해 주세요.")
                }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = cb
                return try {
                    pickFiles.launch(params.createIntent().apply { putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == FileChooserParams.MODE_OPEN_MULTIPLE) })
                    true
                } catch (e: Exception) {
                    fileCallback = null; false
                }
            }
        }
        web.setDownloadListener { url, ua, disposition, mime, _ -> download(url, ua, disposition, mime) }

        swipe.setColorSchemeColors(0xFF5B5BF7.toInt(), 0xFF8B5CF6.toInt())
        swipe.setOnChildScrollUpCallback { _, _ -> !atTop }
        swipe.setOnRefreshListener { web.reload() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        NotifyWorker.ensureChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "avw-notify", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NotifyWorker>(15, TimeUnit.MINUTES).build()
        )

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState)
        } else if (Prefs.server(this).isBlank()) {
            askServer(first = true)
        } else {
            open(intent?.getStringExtra("route"))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("route")?.let { open(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    private fun open(route: String?) {
        progress.visibility = View.VISIBLE
        web.loadUrl(Prefs.server(this).trimEnd('/') + "/" + (route ?: ""))
    }

    private fun askServer(first: Boolean = false) {
        val input = EditText(this).apply {
            setText(Prefs.server(this@MainActivity).ifBlank { "http://192.168.0.56:8100" })
            setSingleLine()
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(if (first) "AI VISION WORKS 서버 주소" else "서버 주소 변경")
            .setMessage("회사에서 안내한 주소를 입력하세요.\n사내: http://192.168.0.56:8100\n외부: https://works.ucube.co.kr")
            .setView(input)
            .setCancelable(!first)
            .setPositiveButton("연결") { _, _ ->
                var u = input.text.toString().trim()
                if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
                Prefs.setServer(this, u.trimEnd('/'))
                web.clearHistory()
                open(null)
            }
            .apply { if (!first) setNegativeButton("취소", null) }
            .show()
    }

    private fun showOffline(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("연결 안 됨")
            .setMessage(msg)
            .setPositiveButton("다시 시도") { _, _ -> open(null) }
            .setNeutralButton("서버 주소 변경") { _, _ -> askServer() }
            .setCancelable(true)
            .show()
    }

    private fun download(url: String, ua: String, disposition: String?, mime: String?) {
        try {
            var name = URLUtil.guessFileName(url, disposition, mime)
            val star = disposition?.let { Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(it) }
            if (star != null) name = URLDecoder.decode(star.groupValues[1], "UTF-8")
            val req = DownloadManager.Request(Uri.parse(url))
                .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                .addRequestHeader("User-Agent", ua)
                .setTitle(name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            if (mime != null) req.setMimeType(mime)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(this, "내려받는 중: $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "내려받기 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class Bridge {
        @JavascriptInterface fun setAtTop(top: Boolean) { atTop = top }
        @JavascriptInterface fun setBadge(n: Int) { Prefs.setLastTotal(this@MainActivity, n) }
        @JavascriptInterface fun changeServer() { runOnUiThread { askServer() } }
        @JavascriptInterface fun print(title: String) {
            runOnUiThread {
                val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
                pm.print(title, web.createPrintDocumentAdapter(title), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
            }
        }
    }
}
