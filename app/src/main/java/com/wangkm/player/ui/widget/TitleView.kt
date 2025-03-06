package com.wangkm.player.ui.widget

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.wangkm.player.R
import com.wangkm.player.utils.ScreenUtils
import com.wangkm.player.utils.StatusUtils
import com.wangkm.xplayer.utils.PlayerUtils

/**
 * created by wangkm
 * Desc:标题栏View 内部维护白色和头面给两套样式两套
 */
class TitleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {
    private var mTitleStyle = STYLE_LIGHT

    /**
     * 全屏
     */
    private fun fullScreen() {
        val activity: Activity = PlayerUtils.getActivity(context)!!
        if (null != activity) {
            findViewById<View>(R.id.view_status_bar).layoutParams.height = PlayerUtils.getNavigationHeight(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //系统版本大于19
                setTranslucentStatus(true, activity)
            }
            //Android5.0以上
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = activity.window
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                window.decorView.systemUiVisibility =
                    SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = Color.TRANSPARENT
            }
            StatusUtils.setStatusTextColor1(0 == mTitleStyle, activity)
        }
    }

    @TargetApi(19)
    private fun setTranslucentStatus(on: Boolean, activity: Activity) {
        val win = activity.window
        val winParams = win.attributes
        val bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    interface OnTitleActionListener {
        fun onBack()
    }

    private var mOnTitleActionListener: OnTitleActionListener? = null

    init {
        inflate(context, R.layout.view_title_view, this)
        if (null != attrs) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TitleView)
            mTitleStyle = typedArray.getInt(R.styleable.TitleView_titleStyle, STYLE_LIGHT)
            typedArray.recycle()
        }
        fullScreen()
        val btnBack = findViewById<View>(R.id.view_back) as ImageView
        val titleText = findViewById<View>(R.id.view_title) as TextView
        val titleBg = findViewById<View>(R.id.view_title_bg) as ImageView
        titleBg.layoutParams.height = PlayerUtils.getNavigationHeight(getContext()) + ScreenUtils.dpToPxInt(49f)
        if (0 == mTitleStyle) {
            btnBack.setColorFilter(Color.parseColor("#333333"))
            titleText.setTextColor(Color.parseColor("#333333"))
            titleBg.setImageResource(R.mipmap.ic_title_bg)
        } else {
            btnBack.setColorFilter(Color.parseColor("#FFFFFF"))
            titleText.setTextColor(Color.parseColor("#FFFFFF"))
            titleBg.setImageResource(0)
        }
        btnBack.setOnClickListener {
            if (null != mOnTitleActionListener) {
                mOnTitleActionListener!!.onBack()
            }
        }
    }

    fun setTitle(title: String?) {
        (findViewById<View>(R.id.view_title) as TextView).text = title
    }

    /**
     * 是否开启返回按钮
     * @param enable true:开启 false:禁用 默认是开启的
     */
    fun enableTitleBack(enable: Boolean) {
        findViewById<View>(R.id.view_back).visibility = if (enable) VISIBLE else GONE
    }

    fun setOnTitleActionListener(onTitleActionListener: OnTitleActionListener?) {
        mOnTitleActionListener = onTitleActionListener
    }

    companion object {
        const val STYLE_LIGHT: Int = 0 //白底
        const val STYLE_COLOR: Int = 1 //透明底
    }
}