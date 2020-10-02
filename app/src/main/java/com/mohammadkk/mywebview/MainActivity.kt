package com.mohammadkk.mywebview

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslCertificate
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mohammadkk.mywebview.tab.Tab
import com.mohammadkk.mywebview.tab.TabRecyclerAdapter
import com.mohammadkk.mywebview.utils.MyToast
import com.mohammadkk.mywebview.utils.Services
import com.mohammadkk.mywebview.utils.Services.isTargetList
import com.monstertechno.adblocker.AdBlockerWebView
import com.monstertechno.adblocker.util.AdBlocker
import kotlinx.android.synthetic.main.action_main_bar.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_layout_drawer.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

@Suppress("DEPRECATION", "UNUSED_ANONYMOUS_PARAMETER")
class MainActivity : AppCompatActivity() {
    private lateinit var saveSetting: SaveSetting
    private lateinit var mainUrl: String
    private var isFinishApp:Boolean = false
    private lateinit var tabRecyclerAdapter: TabRecyclerAdapter
    private var iInterfaceInversed:String = ""
    private var fullScreenView = arrayOfNulls<View>(1)
    private var fullScreenCallbacks = arrayOfNulls<WebChromeClient.CustomViewCallback>(1)
    private fun createWebView():MyWebView{
        val webView = MyWebView(this)
        AdBlockerWebView.init(this).initializeWebView(webView)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url!!)
                if ("mailto:" in url!! || "sms:" in url || "tel:" in url){
                    webView.stopLoading()
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                return true
            }
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                view?.evaluateJavascript(UrlHelper.engineJsDesktopMode, null)
            }
            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                return if (AdBlockerWebView.blockAds(view, url))
                    AdBlocker.createEmptyResource()
                else
                    super.shouldInterceptRequest(view, url)
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBarWeb.visibility = View.VISIBLE
                progressBarWeb.progress = 0
                tabRecyclerAdapter.notifyDataSetChanged()
                if (view!! == getCurrentWebView()){
                    edtUrl.setText(url)
                    edtUrl.setSelection(0)
                    view.evaluateJavascript(iInterfaceInversed, null)
                }
                if (!Services.isInternetConnected(applicationContext))
                    Services.rootSnackBar(this@MainActivity, "لطفاً اتصال اینترنت را بررسی کنید!")
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webRefresher.isRefreshing = false
                progressBarWeb.visibility = View.GONE
                if (view == getCurrentWebView())
                    if (edtUrl.selectionStart == 0 && edtUrl.selectionEnd == 0 && edtUrl.text.toString() == view.url)
                        view.requestFocus()
            }
        }
        webView.webChromeClient = object : WebChromeClient(){
            private val originalSystemUiVisibility= window.decorView.systemUiVisibility
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBarWeb.progress = newProgress
            }
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                if (view == getCurrentWebView()){
                    edtUrl.setText(view.url)
                    edtUrl.setSelection(0)
                }
                actionMainBar.setBackgroundColor(getCurrentWebView().getColor(icon!!))
                tabs[currentTabIndex].bgColor = getCurrentWebView().getColor(icon)
            }
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                callback!!.invoke(origin, true, false)
            }
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                fullScreenView[0] = view
                fullScreenCallbacks[0] = callback
                mainWebLayout.visibility = View.INVISIBLE
                fullScreenLayout.addView(view)
                fullScreenLayout.visibility = View.VISIBLE
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE
            }
            override fun onHideCustomView() {
                if (fullScreenView[0] == null) return
                fullScreenLayout.removeView(fullScreenView[0])
                fullScreenLayout.visibility = View.GONE
                fullScreenView[0] = null
                fullScreenCallbacks[0] = null
                mainWebLayout.visibility = View.VISIBLE
                window.decorView.systemUiVisibility = originalSystemUiVisibility
            }
        }
        val onComplete:BroadcastReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                MyToast(applicationContext, "دانلود با موفقیت انجام شد", 3).show()
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("در حال دانلود فایل...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            MyToast(applicationContext, "شروع دانلود", 2).show()
            registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        return webView
    }
    private val tabs:ArrayList<Tab> = ArrayList()
    private var currentTabIndex by Delegates.notNull<Int>()
    private fun getCurrentTab():Tab = tabs[currentTabIndex]
    private fun getCurrentWebView():MyWebView = getCurrentTab().webView
    private fun newTab(url: String){
        val webView = createWebView()
        webView.visibility = View.GONE
        val tab = Tab(webView)
        tabs.add(tab)
        webViews.addView(webView)
        webView.loadUrl(url)
    }
    private fun switchToTab(tab: Int){
        getCurrentWebView().visibility = View.GONE
        currentTabIndex = tab
        getCurrentWebView().visibility = View.VISIBLE
        getCurrentWebView().requestFocus()
        edtUrl.setText(tabs[currentTabIndex].webView.url)
        actionMainBar.setBackgroundColor(tabs[currentTabIndex].bgColor)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        currentTabIndex = 0
        saveSetting = SaveSetting(this)
        iInterfaceInversed = if (saveSetting.inversionColorLoad()) {
            UrlHelper.jsInversesColor
        } else {
            ""
        }
        newTab(UrlHelper.googleUrl)
        getCurrentWebView().visibility = View.VISIBLE
        getCurrentWebView().requestFocus()
        if (saveSetting.desktopModeLoad()) {
            getCurrentWebView().desktopMode(true)
        } else {
            getCurrentWebView().desktopMode(false)
        }
        tabRecyclerAdapter = TabRecyclerAdapter(this,tabs,object : TabRecyclerAdapter.OnItemTabClick{
            override fun onTabClick(index: Int) {
                switchToTab(index)
            }

        })
        tabContainerRecycler.adapter = tabRecyclerAdapter
        tabContainerRecycler.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        pageInfo()
        setupDrawer()
        setSupportActionBar(actionMainBar)
        onActionbarTop()
        bottomNavigationManagement()
        registerForContextMenu(getCurrentWebView())
        confirmPermissions()
    }
    fun createPopupMenu(view: View) {
        val v = LayoutInflater.from(this).inflate(R.layout.popup_layout, findViewById(R.id.scrollPopup), false)
        val toggleDesktopMode = v.findViewById<SwitchMaterial>(R.id.toggleDesktopMode)
        val toggleNightMode = v.findViewById<SwitchMaterial>(R.id.toggleNightMode)
        val clearAllHistory = v.findViewById<TextView>(R.id.clearAllHistory)
        val findInPage = v.findViewById<TextView>(R.id.findInPage)
        val showListDownloads = v.findViewById<TextView>(R.id.showListDownloads)
        val printInPage = v.findViewById<TextView>(R.id.printInPage)
        val shareLinkCurrentUrl = v.findViewById<TextView>(R.id.shareLinkCurrentUrl)
        val mainPage = v.findViewById<TextView>(R.id.mainPage)
        val exitApplication = v.findViewById<TextView>(R.id.exitApplication)
        val popup = PopupWindow(v, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true)
        clearAllHistory.setOnClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("حدف تمام تاریخچه مرور")
            builder.setMessage("آیا از حذف تمام تاریخچه مرور خود مطمئن هستید ؟")
            builder.setCancelable(true)
            builder.setNegativeButton("نه") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            builder.setPositiveButton("بله") { dialog: DialogInterface?, which: Int ->
                getCurrentWebView().clearHistory()
                getCurrentWebView().clearFormData()
                getCurrentWebView().clearCache(true)
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
            }
            popup.dismiss()
            builder.show()
        }
        findInPage.setOnClickListener {
            layoutFindUrlTop.visibility = View.GONE
            layoutFindPanel.visibility = View.VISIBLE
            btnCloseDialogFindWord.setOnClickListener { v: View? ->
                layoutFindUrlTop.visibility = View.VISIBLE
                layoutFindPanel.visibility = View.GONE
                getCurrentWebView().clearMatches()
                getCurrentWebView().requestFocus()
                edtFindWord.text.clear()
            }
            edtFindWord.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    getCurrentWebView().findAllAsync(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            btnDownFindWord.setOnClickListener { v: View? ->
                getCurrentWebView().findNext(true)
            }
            btnUpFindWord.setOnClickListener { v: View? ->
                getCurrentWebView().findNext(false)
            }
            edtFindWord.setOnEditorActionListener { v, actionId, event ->
                val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                false
            }
            popup.dismiss()
        }
        showListDownloads.setOnClickListener {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        }
        printInPage.setOnClickListener {
            popup.dismiss()
            printPdf()
        }
        exitApplication.setOnClickListener {
            popup.dismiss()
            finish()
        }
        mainPage.setOnClickListener {
            getCurrentWebView().loadUrl(UrlHelper.googleUrl)
            popup.dismiss()
        }
        shareLinkCurrentUrl.setOnClickListener {
            val url = getCurrentWebView().url
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(shareIntent, "اشتراک لینک"))
            popup.dismiss()
        }
        if (saveSetting.desktopModeLoad())
            toggleDesktopMode.isChecked = true
        if (saveSetting.inversionColorLoad())
            toggleNightMode.isChecked = true
        toggleDesktopMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                getCurrentWebView().desktopMode(true)
            } else {
                getCurrentWebView().desktopMode(false)
            }
            saveSetting.desktopModeSave(isChecked)
            getCurrentWebView().requestFocus()
            popup.dismiss()
        }
        toggleNightMode.setOnCheckedChangeListener { buttonView, isChecked ->
            iInterfaceInversed = if (isChecked){
                UrlHelper.jsInversesColor
            } else {
                ""
            }
            getCurrentWebView().reload()
            getCurrentWebView().requestFocus()
            saveSetting.inversionColorSave(isChecked)
            popup.dismiss()
        }
        popup.elevation = 40F
        popup.animationStyle = R.style.AnimationPopupWindow
        popup.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background_round_layout))
        popup.showAtLocation(popupMenuBtn, Gravity.TOP or Gravity.START, 10, 100)
    }
    private fun setupDrawer() {
        var isDropEngineSearch = false
        when(saveSetting.loadPossession()){
            0 -> {
                radioGoogle.isChecked = true
                mainUrl = UrlHelper.googleSearchUrl
            }
            1 -> {
                radioBing.isChecked = true
                mainUrl = UrlHelper.bingSearchUrl
            }
            2 -> {
                radioYahoo.isChecked = true
                mainUrl = UrlHelper.yahooSearchUrl
            }
        }
        btnDropDrawer.setOnClickListener {
            if (!isDropEngineSearch){
                btnDropDrawer.animate().rotation(180F).setDuration(500).start()
                drawerEngineSearch.visibility = View.VISIBLE
                drawerEngineSearch.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down))
            } else {
                btnDropDrawer.animate().rotation(0F).setDuration(500).start()
                drawerEngineSearch.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
                drawerEngineSearch.visibility = View.GONE
            }
            radioGoogle.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    mainUrl = UrlHelper.googleSearchUrl
                    saveSetting.savePossession(0)
                    mainDrawer.closeDrawer(mainNav)
                }
            }
            radioBing.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    mainUrl = UrlHelper.bingSearchUrl
                    saveSetting.savePossession(1)
                    mainDrawer.closeDrawer(mainNav)
                }
            }
            radioYahoo.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked){
                    mainUrl = UrlHelper.yahooSearchUrl
                    saveSetting.savePossession(2)
                    mainDrawer.closeDrawer(mainNav)
                }
            }
            isDropEngineSearch = !isDropEngineSearch
        }
    }
    private fun printPdf() {
        val title: String = HelperUnit().fileName(getCurrentWebView().url!!)
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val documentAdapter = getCurrentWebView().createPrintDocumentAdapter(title)
        printManager.print(title, documentAdapter, PrintAttributes.Builder().build())
    }
    private fun bottomNavigationManagement() {
        bottomNavigationItems.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.removeTab -> {
                    closeCurrentTab()
                }
                R.id.addTab -> {
                    newTab(UrlHelper.googleUrl)
                    switchToTab(tabs.size -1)
                    tabRecyclerAdapter.notifyDataSetChanged()
                }
                R.id.goBackWebView -> if (getCurrentWebView().canGoBack()) {
                    getCurrentWebView().goBack()
                }
                R.id.goNextWebView -> if (getCurrentWebView().canGoForward()) {
                    getCurrentWebView().goForward()
                }
            }
            true
        }
    }
    private fun onActionbarTop() {
        webRefresher.setOnRefreshListener {
            getCurrentWebView().reload()
            isFinishApp = false
        }
        btnWebRefresher.setOnClickListener {
            getCurrentWebView().reload()
            isFinishApp = false
        }
        edtUrl.setOnEditorActionListener { v, actionId, event ->
            val query = v.text.toString()
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (query.startsWith("http") || query.startsWith("file")) {
                    getCurrentWebView().loadUrl(query)
                } else if (query.startsWith("www")){
                    getCurrentWebView().loadUrl("http://$query")
                } else {
                    getCurrentWebView().loadUrl(mainUrl + query)
                }
            }
            val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            true
        }
        edtUrl.onFocusChangeListener = View.OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            if (hasFocus) edtUrl.selectAll()
        }
    }
    private fun confirmPermissions() {
        val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasPermissions(this, permission.toString())){
            ActivityCompat.requestPermissions(this, permission, 100)
        }
    }
    @Suppress("SENSELESS_COMPARISON")
    private fun hasPermissions(context: Context, vararg permissions: String):Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context!= null && permissions != null){
            for (permission in permissions){
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED){
                    return false
                }
            }
        }
        return true
    }
    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webViewHitResult = getCurrentWebView().hitTestResult
        if (webViewHitResult.type == WebView.HitTestResult.IMAGE_TYPE || webViewHitResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu!!.setHeaderTitle("دانلود عکس...")
            menu.setHeaderIcon(R.drawable.ic_save)
            menu.add(0, 1, 0, "دانلود عکس")
                    .setOnMenuItemClickListener { item: MenuItem? ->
                        val imageUrl = webViewHitResult.extra
                        if (URLUtil.isValidUrl(imageUrl)) {
                            val request = DownloadManager.Request(Uri.parse(imageUrl))
                            request.allowScanningByMediaScanner()
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDescription("در حال دانلود عکس")
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imageUrl)
                            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                        }
                        false
                    }
        }
    }
    override fun onBackPressed() {
        if (getCurrentWebView().canGoBack()) {
            getCurrentWebView().goBack()
        } else {
            if (!isFinishApp){
                MyToast(applicationContext, "چیزی برای برگشت وجود ندارد!!", 1).show()
                isFinishApp = true
            } else {
                if (tabs.size > 1) {
                    closeCurrentTab()
                    isFinishApp = false
                } else {
                    super.onBackPressed()
                }
            }
        }

    }
    private fun pageInfo() {
        btnSecurityPageInfo.setOnClickListener {
            var s = "URL: " + getCurrentWebView().url + "\n";
            s += "Title: " + getCurrentWebView().title + "\n\n"
            val certificate:SslCertificate? = getCurrentWebView().certificate
            s += if (certificate == null) "Not secure" else "Certificate:\n" + certificateToStr(certificate)
            AlertDialog.Builder(this)
                    .setTitle("گواهی امنیت صفحه")
                    .setMessage(s)
                    .setNegativeButton("ok") { dialogInterface, i -> dialogInterface.dismiss() }
                    .show()
        }
    }
    private fun closeCurrentTab(){
        webViews.removeView(getCurrentWebView())
        getCurrentWebView().display
        tabs.removeAt(currentTabIndex)
        if (currentTabIndex >= tabs.size) currentTabIndex = tabs.size -1
        if (currentTabIndex == -1) {
            newTab(UrlHelper.googleUrl)
            currentTabIndex = 0
        }
        getCurrentWebView().visibility = View.VISIBLE
        getCurrentWebView().requestFocus()
        tabRecyclerAdapter.notifyDataSetChanged()
        edtUrl.setText(getCurrentWebView().url)
        actionMainBar.setBackgroundColor(getCurrentTab().bgColor)
    }
    private fun certificateToStr(certificate: SslCertificate?): String? {
        if (certificate == null) {
            return null
        }
        var s = ""
        val issuedTo = certificate.issuedTo
        if (issuedTo != null) {
            s += "Issued to: ${issuedTo.dName} \n"
        }
        val issuedBy = certificate.issuedBy
        if (issuedBy != null) {
            s += "Issued by: ${issuedBy.dName} \n"
        }
        val issueDate = certificate.validNotBeforeDate
        if (issueDate != null) {
            s += String.format("Issued on: %tF %tT %tz\n", issueDate, issueDate, issueDate)
        }
        val expiryDate = certificate.validNotAfterDate
        if (expiryDate != null) {
            s += String.format("Expires on: %tF %tT %tz\n", expiryDate, expiryDate, expiryDate)
        }
        return s
    }
}