package com.mohammadkk.mywebview

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.palette.graphics.Palette
import kotlin.math.max

@Suppress("DEPRECATION")
class MyWebView : WebView,NestedScrollingChild {
    private var mLastMotionY = 0
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private var mNestedYOffset = 0
    private var mChildHelper: NestedScrollingChildHelper? = null
    constructor(context: Context?) : this(context, null){
        initWebView()
    }
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0){
        initWebView()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr){
        initWebView()
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(){
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
        this.settings.javaScriptEnabled = true
        this.settings.javaScriptCanOpenWindowsAutomatically = true
        this.settings.allowContentAccess = true
        this.settings.allowFileAccess = true
        this.settings.allowUniversalAccessFromFileURLs = true
        this.settings.databaseEnabled = true
        this.settings.setSupportZoom(true)
        this.settings.builtInZoomControls = true
        this.settings.displayZoomControls = false
        this.settings.domStorageEnabled = true
        this.settings.setAppCacheEnabled(true)
        this.settings.cacheMode = WebSettings.LOAD_DEFAULT
        this.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        this.scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
        this.isScrollbarFadingEnabled = true
        this.isFocusable = true
        this.clearSslPreferences()
    }
    @Suppress("DEPRECATION")
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var result = false
        val trackedEvent = MotionEvent.obtain(event)
        val action = MotionEventCompat.getActionMasked(event)
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0
        }
        val y = event.y.toInt()
        event.offsetLocation(0f, mNestedYOffset.toFloat())
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mLastMotionY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                result = super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = mLastMotionY - y
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1]
                    trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedYOffset += mScrollOffset[1]
                }
                mLastMotionY = y - mScrollOffset[1]
                val oldY = scrollY
                val newScrollY = max(0, oldY + deltaY)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed
                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                    mLastMotionY -= mScrollOffset[1]
                    trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedYOffset += mScrollOffset[1]
                }
                result = super.onTouchEvent(trackedEvent)
                trackedEvent.recycle()
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll()
                result = super.onTouchEvent(event)
            }
        }
        return result
    }
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper!!.isNestedScrollingEnabled = enabled
    }
    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper!!.isNestedScrollingEnabled
    }
    override fun startNestedScroll(axes: Int): Boolean {
        return mChildHelper!!.startNestedScroll(axes)
    }
    override fun stopNestedScroll() {
        mChildHelper!!.stopNestedScroll()
    }
    override fun hasNestedScrollingParent(): Boolean {
        return mChildHelper!!.hasNestedScrollingParent()
    }
    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return mChildHelper!!.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }
    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return mChildHelper!!.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }
    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return mChildHelper!!.dispatchNestedFling(velocityX, velocityY, consumed)
    }
    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper!!.dispatchNestedPreFling(velocityX, velocityY)
    }
    fun getColor(bitmap: Bitmap):Int{
        val palette = Palette.from(bitmap).generate()
        val default = ContextCompat.getColor(context, R.color.blueTwo)
        val muted= palette.getDominantColor(default)
        return palette.getVibrantColor(muted)
    }
    fun setDesktopMode(enabled: Boolean) {
        var newUserAgent = this.settings.userAgentString
        if (enabled) {
            try {
                val ua = this.settings.userAgentString
                val androidOSString = this.settings.userAgentString.substring(ua.indexOf("("), ua.indexOf(")") + 1)
                newUserAgent = this.settings.userAgentString.replace(androidOSString, "(X11; Linux x86_64)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            newUserAgent = null
        }
        this.settings.userAgentString = newUserAgent
        this.settings.useWideViewPort = enabled
        this.settings.loadWithOverviewMode = enabled
        this.reload()
    }
}