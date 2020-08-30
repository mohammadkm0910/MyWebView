package com.mohammadkk.mywebview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.view.MotionEventCompat
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.max

class MyWebView : WebView,NestedScrollingChild {
    private var mLastMotionY = 0
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private var mNestedYOffset = 0
    private var mChildHelper: NestedScrollingChildHelper? = null
    constructor(context: Context?) : this(context,null){
        initWebView()
    }
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs,R.attr.name){
        initWebView()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){
        initWebView()
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(){
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
        this.settings.javaScriptEnabled = true
        this.settings.javaScriptCanOpenWindowsAutomatically = true
        this.settings.setSupportZoom(true)
        this.settings.builtInZoomControls = true
        this.settings.displayZoomControls = false
        this.settings.allowContentAccess = true
        this.settings.allowFileAccess = true
        this.settings.allowUniversalAccessFromFileURLs = true
        this.settings.databaseEnabled = true
        this.settings.domStorageEnabled = true
        this.settings.setAppCacheEnabled(true)
        this.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        this.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        this.scrollBarStyle = SCROLLBARS_OUTSIDE_OVERLAY
        this.isScrollbarFadingEnabled = true
        this.isFocusable = true
    }
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
    fun desktopMode(enabled:Boolean){
        val webSettings = this.settings
        val newUserAgent:String
        newUserAgent = if (enabled) {
            webSettings.userAgentString.replace("Mobile", "eliboM").replace("Android", "diordnA")
        } else {
            webSettings.userAgentString.replace("eliboM", "Mobile").replace("diordnA", "Android")
        }
        webSettings.userAgentString = newUserAgent
        webSettings.useWideViewPort = enabled
        webSettings.loadWithOverviewMode = enabled
        this.reload()
    }
}