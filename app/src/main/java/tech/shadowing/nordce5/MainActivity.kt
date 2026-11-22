package tech.shadowing.nordce5

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Native Android 15 (OxygenOS 15) WebView Wrapper tailored for OnePlus Nord CE5.
 * Target site: https://shadowing.tech
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TARGET_URL = "https://shadowing.tech"
        private const val TRUSTED_HOST = "shadowing.tech"
        private const val STATUS_BAR_HEX = "#0F172A"
        private const val NAV_BAR_HEX = "#0F172A"
    }

    private lateinit var rootContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var lastBackPressedMs: Long = 0L

    private val runtimePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val request = pendingWebPermissionRequest
            pendingWebPermissionRequest = null
            if (request != null) {
                val anyGranted = grants.values.any { it }
                if (anyGranted) {
                    request.grant(request.resources)
                } else {
                    request.deny()
                    Toast.makeText(
                        this,
                        "Microphone permission is required for voice shadowing.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.parseColor(STATUS_BAR_HEX)),
            navigationBarStyle = SystemBarStyle.dark(Color.parseColor(NAV_BAR_HEX))
        )

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(STATUS_BAR_HEX))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor(STATUS_BAR_HEX))
        }

        fullscreenContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        swipeRefresh = SwipeRefreshLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(webView)
            setOnRefreshListener { webView.reload() }
            setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 }
        }
        rootContainer.addView(swipeRefresh)
        rootContainer.addView(fullscreenContainer)
        setContentView(rootContainer)

        // Android 15 / OnePlus Nord CE5 punch-hole camera & gesture bar inset protection
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        configureWebView()
        registerBackNavigation()
        requestInitialPermissionsIfNeeded()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(TARGET_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            // Remove WebView "; wv" token so Google OAuth sign-in is permitted
            userAgentString = userAgentString.replace("; wv", "")
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val scheme = uri.scheme ?: return false
                if (scheme == "http" || scheme == "https") {
                    val host = uri.host ?: ""
                    val isTrusted = host == TRUSTED_HOST ||
                        host.endsWith(".$TRUSTED_HOST") ||
                        host.endsWith("google.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtu.be") ||
                        host.endsWith("accounts.google.com")
                    if (isTrusted) {
                        return false
                    }
                    // Open unrelated external websites in OxygenOS default browser
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                    return true
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    view.loadUrl("file:///android_asset/offline.html")
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val originHost = request.origin?.host ?: ""
                    val isTrustedOrigin = originHost == TRUSTED_HOST || originHost.endsWith(".$TRUSTED_HOST")
                    if (!isTrustedOrigin) {
                        request.deny()
                        return@runOnUiThread
                    }

                    val neededAndroidPerms = mutableListOf<String>()
                    val wantsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                    val wantsVideo = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)

                    if (wantsAudio && ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        neededAndroidPerms.add(Manifest.permission.RECORD_AUDIO)
                    }

                    

                    if (neededAndroidPerms.isEmpty()) {
                        request.grant(request.resources)
                    } else {
                        pendingWebPermissionRequest = request
                        runtimePermissionLauncher.launch(neededAndroidPerms.toTypedArray())
                    }
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            override fun onHideCustomView() {
                customView?.let { fullscreenContainer.removeView(it) }
                customView = null
                fullscreenContainer.visibility = View.GONE
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun requestInitialPermissionsIfNeeded() {
        val perms = arrayOf<String>(Manifest.permission.RECORD_AUDIO)
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            runtimePermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun registerBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    webView.webChromeClient?.onHideCustomView()
                    return
                }
                if (webView.canGoBack()) {
                    webView.goBack()
                    return
                }
                val now = System.currentTimeMillis()
                if (now - lastBackPressedMs < 2000L) {
                    finish()
                } else {
                    lastBackPressedMs = now
                    Toast.makeText(
                        this@MainActivity,
                        "Swipe or press back again to exit",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
