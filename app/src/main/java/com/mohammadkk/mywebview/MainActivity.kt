package com.mohammadkk.mywebview

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslCertificate
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.mohammadkk.mywebview.adapter.SpinnerIconAdapter
import com.mohammadkk.mywebview.utils.EssentialMethod
import com.mohammadkk.mywebview.utils.MyToast
import com.monstertechno.adblocker.AdBlockerWebView
import com.monstertechno.adblocker.util.AdBlocker
import kotlinx.android.synthetic.main.action_main_bar.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toggle_layout.*

@Suppress("DEPRECATION", "UNUSED_ANONYMOUS_PARAMETER")
class MainActivity : AppCompatActivity(),EssentialMethod {
    private lateinit var cci: CheckConnectionInternet
    private lateinit var saveSetting: SaveSetting
    private lateinit var mainUrl: String
    private var isFinishApp:Boolean = false
    private var iInterfaceInversed:String = ""
    private lateinit var behavior:BottomSheetBehavior<View>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cci = CheckConnectionInternet(this)
        behavior = BottomSheetBehavior.from(toggleLayout)
        saveSetting = SaveSetting(this)
        if (saveSetting.desktopModeLoad()) {
            scrollWeb.desktopMode(true)
        } else {
            scrollWeb.desktopMode(false)
        }
        iInterfaceInversed = if (saveSetting.inversionColorLoad()) {
            UrlHelper.jsInversesColor
        } else {
            ""
        }
        initWebView()
        refreshPage()
        pageInfo()
        setSupportActionBar(actionMainBar)
        onActionbarTop()
        toggleButtonManagement()
        bottomNavigationManagement()
        initSpinner()
        downloadManager()
        registerForContextMenu(scrollWeb)
        confirmPermissions()

    }
    fun createPopupMenu(view: View) {
        val wrapper = ContextThemeWrapper(this@MainActivity, R.style.customPopupMenuStyleOne)
        val popup = PopupMenu(wrapper, popupMenuBtn)
        popup.menuInflater.inflate(R.menu.popup_menu_top, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.allClearHistoryWeb -> {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setTitle("حدف تمام تاریخچه مرور")
                    builder.setMessage("آیا از حذف تمام تاریخچه مرور خود مطمئن هستید ؟")
                    builder.setCancelable(true)
                    builder.setNegativeButton("نه") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                    builder.setPositiveButton("بله") { dialog: DialogInterface?, which: Int ->
                        scrollWeb.clearHistory()
                        scrollWeb.clearCache(true)
                    }
                    builder.show()
                }
                R.id.findWordToPage -> {
                    layoutFindUrlTop.visibility = View.GONE
                    layoutFindPanel.visibility = View.VISIBLE
                    btnCloseDialogFindWord.setOnClickListener { v: View? ->
                        layoutFindUrlTop.visibility = View.VISIBLE
                        layoutFindPanel.visibility = View.GONE
                        scrollWeb.findAllAsync("")
                        edtFindWord.text.clear()
                    }
                    edtFindWord.setOnEditorActionListener { v, actionId, event ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            scrollWeb.findAllAsync(v.text.toString())
                            val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(v.windowToken, 0)
                        }
                        false
                    }
                    btnDownFindWord.setOnClickListener { v: View? ->
                        findControllerWebView(true)
                    }
                    btnUpFindWord.setOnClickListener { v: View? ->
                        findControllerWebView(false)
                    }
                }
                R.id.shareUrlWebView -> {
                    val url = scrollWeb.url
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                    startActivity(Intent.createChooser(shareIntent, "اشتراک لینک"))
                }
                R.id.listDownloads -> startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                R.id.pintPdfPage -> printPdf()
                R.id.exitApp -> finish()
            }
            true
        }
        popup.show()
    }

    private fun findControllerWebView(next: Boolean) {
        try {
            if (next) {
                scrollWeb.findNext(true)
            } else {
                scrollWeb.findNext(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun printPdf() {
        val title: String = HelperUnit().fileName(scrollWeb.url!!)
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val documentAdapter = scrollWeb.createPrintDocumentAdapter(title)
        printManager.print(title, documentAdapter, PrintAttributes.Builder().build())
    }
    private fun downloadManager() {
        val onComplete:BroadcastReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                MyToast(applicationContext, "دانلود با موفقیت انجام شد", 3).show()
            }
        }
        scrollWeb.setDownloadListener { url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long ->
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
    }
    private fun initSpinner() {
        val icons = arrayOf(R.drawable.ic_google,R.drawable.ic_bing,R.drawable.ic_yahoo)
        val spinnerIconAdapter = SpinnerIconAdapter(this,icons)
        searchEngineSpinner.adapter = spinnerIconAdapter
        searchEngineSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        mainUrl = UrlHelper.googleSearchUrl
                        saveSetting.savePossession(position)
                    }
                    1 -> {
                        mainUrl = UrlHelper.bingSearchUrl
                        saveSetting.savePossession(position)
                    }
                    2 -> {
                        mainUrl = UrlHelper.yahooSearchUrl
                        saveSetting.savePossession(position)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        searchEngineSpinner.setSelection(saveSetting.loadPossession())
        when(saveSetting.loadPossession()){
            0 -> MyToast(this,"موتور جست و جو گوگل",2).show()
            1 -> MyToast(this,"موتور جست و جو بینگ",2).show()
            2 -> MyToast(this,"موتور جست و جو یاهو",2).show()
        }
    }
    @SuppressLint("InflateParams")
    private fun toggleButtonManagement() {
        if (saveSetting.desktopModeLoad()) {
            btnSwitchDesktopMode.isChecked = true
        }
        if (saveSetting.inversionColorLoad()) {
            btnSwitchInversionColor.isChecked = true
        }
        btnSwitchDesktopMode.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                scrollWeb.desktopMode(true)
            } else {
                scrollWeb.desktopMode(false)
            }
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            saveSetting.desktopModeSave(isChecked)
        }
        btnSwitchInversionColor.setOnCheckedChangeListener { buttonView, isChecked ->
            iInterfaceInversed = if (isChecked){
                UrlHelper.jsInversesColor
            } else {
                ""
            }
            scrollWeb.reload()
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            saveSetting.inversionColorSave(isChecked)
        }
    }
    private fun bottomNavigationManagement() {
        bottomNavigationItems.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.goToHomePage -> scrollWeb.loadUrl("https://www.google.com/")
                R.id.addTabActivity -> {
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    startActivity(intent)
                }
                R.id.goBackWebView -> if (scrollWeb.canGoBack()) {
                    scrollWeb.goBack()
                }
                R.id.goNextWebView -> if (scrollWeb.canGoForward()) {
                    scrollWeb.goForward()
                }
                R.id.openDrawer -> {
                    if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED){
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }
            true
        }
    }

    private fun onActionbarTop() {
        edtUrl.setOnEditorActionListener { v, actionId, event ->
            val query = v.text.toString()
            if (actionId == EditorInfo.IME_ACTION_GO) {
                if (query.startsWith("http") || query.startsWith("file")) {
                    scrollWeb.loadUrl(query)
                } else {
                    if (query.startsWith("www")) {
                        scrollWeb.loadUrl("http://$query")
                    } else {
                        val urlQuery: String = mainUrl + query
                        scrollWeb.loadUrl(urlQuery)
                    }
                }
            }
            val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
            true
        }
        edtUrl.onFocusChangeListener = View.OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            if (hasFocus) {
                edtUrl.selectAll()
            }
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
        val webViewHitResult = scrollWeb.hitTestResult
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
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (scrollWeb.canGoBack()) {
                scrollWeb.goBack()
            } else {
                if (!isFinishApp) {
                    MyToast(applicationContext, "چیزی برای برگشت وجود ندارد!!", 1).show()
                    isFinishApp = true
                } else {
                    finish()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun initWebView() {
        AdBlockerWebView.init(this).initializeWebView(scrollWeb)
        scrollWeb.loadUrl(UrlHelper.googleUrl)
        scrollWeb.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view!!.loadUrl(url!!)
                if (url.contains("mailto:") || url.contains("sms:") || url.contains("tel:")){
                    scrollWeb.stopLoading()
                    val i = Intent()
                    i.action = Intent.ACTION_VIEW
                    i.data = Uri.parse(url)
                    startActivity(i)
                }
                return true
            }
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                view!!.evaluateJavascript(UrlHelper.engineJsDesktopMode, null)
            }
            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                return if (AdBlockerWebView.blockAds(view, url)) AdBlocker.createEmptyResource() else super.shouldInterceptRequest(view, url)
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBarWeb.visibility = View.VISIBLE
                if (!cci.checkedConnectionInternet()) {
                    Snackbar.make(mainDrawer, "اتصال اینترنت را بررسی کنید.", Snackbar.LENGTH_LONG).show()
                    actionMainBar.setBackgroundResource(R.color.blueThree)
                }
                edtUrl.setText(url)
                view!!.evaluateJavascript(iInterfaceInversed, null)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBarWeb.visibility = View.GONE
                webRefresher.isRefreshing = false
            }
        }
        scrollWeb.webChromeClient = object : WebChromeClient() {
            private var mCustomView: View? = null
            private var mOriginalSystemUiVisibility = 0
            private var mOriginalOrientation = 0
            private var mCustomViewCallback: CustomViewCallback? = null
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBarWeb.max = newProgress
            }
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                if (cci.checkedConnectionInternet()){
                    actionMainBar.setBackgroundColor(scrollWeb.getColor(icon!!))
                }
                edtUrl.setText(view!!.url)
            }
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                callback!!.invoke(origin, true, false)
            }
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (mCustomView != null) {
                    onHideCustomView()
                    return
                }
                mCustomView = view
                mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
                mOriginalOrientation = requestedOrientation
                mCustomViewCallback = callback
                val decor = window.decorView as FrameLayout
                decor.addView(mCustomView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                bottomNavigationItems.visibility = View.GONE
            }
            override fun onHideCustomView() {
                val decor = window.decorView as FrameLayout
                decor.removeView(mCustomView)
                mCustomView = null
                window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                requestedOrientation = mOriginalOrientation
                mCustomViewCallback!!.onCustomViewHidden()
                mCustomViewCallback = null
                bottomNavigationItems.visibility = View.VISIBLE
            }
        }
    }
    override fun refreshPage() {
        webRefresher.setOnRefreshListener {
            scrollWeb.reload()
            isFinishApp = false
        }
        btnWebRefresher.setOnClickListener {
            scrollWeb.reload()
            isFinishApp = false
        }
    }
    override fun pageInfo() {
        btnSecurityPageInfo.setOnClickListener {
            var s = "URL: " + scrollWeb.url + "\n";
            s += "Title: " + scrollWeb.title + "\n\n"
            val certificate:SslCertificate? = scrollWeb.certificate
            s += if (certificate == null) "Not secure" else "Certificate:\n" + certificateToStr(certificate)
            AlertDialog.Builder(this)
                    .setTitle("گواهی امنیت صفحه")
                    .setMessage(s)
                    .setNegativeButton("ok", DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.dismiss() })
                    .show()
        }
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