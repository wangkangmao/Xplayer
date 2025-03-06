package com.wangkm.player.utils

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import com.wangkm.player.App
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * created by wangkm
 * Desc:
 */
object ScreenUtils {
    val screenWidth: Int
        /**
         * 获取屏幕宽度
         * @return
         */
        get() = App.context.getResources().getDisplayMetrics().widthPixels

    val screenWidthDP: Float
        get() {
            val dpInt = pxToDpInt(screenWidth.toFloat())
            return dpInt.toFloat()
        }

    val screenHeightDP: Float
        get() {
            val dpInt = pxToDpInt(screenHeight.toFloat())
            return dpInt.toFloat()
        }

    val screenHeight: Int
        /**
         * 获取屏幕高度
         * @return
         */
        get() = App.context.resources.displayMetrics.heightPixels

    /**
     * 将px转换成dp
     * @param pxValue
     * @return
     */
    fun pxToDpInt(pxValue: Float): Int {
        val scale: Float = App.context.resources.displayMetrics.density
        return (pxValue / (if (scale <= 0) 1f else scale) + 0.5f).toInt()
    }

    /**
     * 将dp转换成px
     * @param dipValue
     * @return
     */
    fun dpToPxInt(dipValue: Float): Int {
        val scale: Float = App.context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    /**
     * 将自己从父Parent中移除
     * @param view
     */
    fun removeParent(view: View?) {
        try {
            if (null != view && null != view.parent) {
                if (view.parent is ViewGroup) {
                    (view.parent as ViewGroup).removeView(view)
                } else if (view.parent is ViewParent) {
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun getActivity(context: Context?): Activity? {
        if (context == null) return null
        if (context is Activity) {
            return context
        } else if (context is ContextThemeWrapper) {
            return getActivity(context.baseContext)
        }
        return null
    }

    /**
     * 通过反射的方式获取状态栏高度
     * @return
     */
    fun getStatusBarHeight(context: Context): Int {
        var StatusBarHeight = 0
        try {
            val resourceId = context.applicationContext.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                StatusBarHeight = context.applicationContext.resources.getDimensionPixelSize(resourceId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            StatusBarHeight = dpToPxInt(25f)
        }
        return StatusBarHeight
    }

    /**
     * 将数据转换为万为单位
     * @param no
     * @return
     */
    fun formatWan(no: Long): String {
        val n = no.toDouble() / 10000
        return changeDouble(n).toString() + "万"
    }

    fun formatWan(no: String, round: Boolean): String {
        val parseInt = parseInt(no)
        return formatWan(parseInt.toLong(), round)
    }

    fun formatWan(no: Long, round: Boolean): String {
        if (round && no <= 10000) return no.toString()
        val n = no.toDouble() / 10000
        return changeDouble(n).toString() + "万"
    }

    fun changeDouble(dou: Double): Double {
        var dou = dou
        try {
            val nf: NumberFormat = DecimalFormat("0.0 ")
            dou = nf.format(dou).toDouble()
            return dou
        } catch (e: RuntimeException) {
        }
        return dou
    }

    fun changeDouble(num: Float): Double {
        var parseDouble = 0.0
        try {
            val nf: NumberFormat = DecimalFormat("0.00")
            parseDouble = nf.format(num.toDouble()).toDouble()
            return parseDouble
        } catch (e: Exception) {
            return parseDouble
        }
    }

    @JvmOverloads
    fun parseInt(content: String, defaultValue: Int = 0): Int {
        if (TextUtils.isEmpty(content)) return defaultValue
        try {
            return content.toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return 0
        }
    }

    @JvmOverloads
    fun parseLong(content: String, defaultValue: Int = 0): Long {
        if (TextUtils.isEmpty(content)) return defaultValue.toLong()
        try {
            return content.toLong()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return 0
        }
    }

}