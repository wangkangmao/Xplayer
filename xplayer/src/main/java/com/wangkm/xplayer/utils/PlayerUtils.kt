package com.wangkm.xplayer.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.wangkm.xplayer.widget.view.LayoutProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Formatter
import java.util.Locale

/**
 * created by wangkm
 * Desc:工具类集合
 */
object PlayerUtils {
    fun stringForAudioTime(timeMs: Long): String {
        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
            return "00:00"
        }
        val totalSeconds = timeMs / 1000
        val seconds = (totalSeconds % 60).toInt()
        val minutes = ((totalSeconds / 60) % 60).toInt()
        val hours = (totalSeconds / 3600).toInt()
        val stringBuilder = StringBuilder()
        val mFormatter = Formatter(stringBuilder, Locale.getDefault())
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    /**
     * 返回设备是否连接至WIFI网络
     * @param context context
     * @return if wifi is connected,return true
     */
    fun isWifiConnected(context: Context?): Boolean {
        if (existPermission(this.context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            try {
                val connectivityManager = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                @SuppressLint("MissingPermission") val networkInfo = connectivityManager.activeNetworkInfo
                return networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return true
    }

    /**
     * 返回设备是否连接至移动网络
     * @param context context
     * @return if wifi is connected,return true
     */
    fun isMobileConnected(context: Context?): Boolean {
        if (isCheckNetwork && !isWifiConnected(context)) {
            return true
        }
        return false
    }

    val isCheckNetwork: Boolean
        /**
         * 检查设备是否已连接至可用网络
         * @return
         */
        get() {
            val context = context
            if (null != context && existPermission(this.context, Manifest.permission.ACCESS_NETWORK_STATE)) {
                try {
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    @SuppressLint("MissingPermission") val networkInfo = cm.activeNetworkInfo ?: return false
                    val type = networkInfo.type
                    if (type == ConnectivityManager.TYPE_MOBILE || type == ConnectivityManager.TYPE_WIFI) {
                        return true
                    }
                    return false
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                return true
            }
            return true
        }

    fun mobileNetwork(isMobileNetwork: Boolean): Boolean {
        if (isMobileConnected(context)) {
            return isMobileNetwork
        }
        return true
    }

    /**
     * 获取上下文所在的Activity
     * 写播放器时遇到的问题：播放器被转场时，获取播放器的上下文是上一个Activity的上下文。
     * 解决办法:在接收转场的Activityg或Fragment或ViewGroup中调用BasePlayer的setTempContext(Context context)手动设置上下文,并在界面销毁时释放TempContext
     * @param context
     * @return
     */
    fun getActivity(context: Context?): Activity? {
        try {
            if (context == null) return null
            if (context is Activity) {
                return context
            } else if (context is ContextWrapper) {
                return getActivity(context.baseContext)
            } else if (context is ContextThemeWrapper) {
                return getActivity(context.baseContext)
            }
            return null
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    //设备屏幕宽度
    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    //设备屏幕高度
    fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    /**
     * 将dp转换成px
     * @param dp
     * @return
     */
    fun dpToPx(context: Context?, dp: Float): Float {
        return dp * context!!.applicationContext.resources.displayMetrics.density
    }

    fun dpToPxInt(dp: Float): Int {
        return (dpToPx(context, dp) + 0.5f).toInt()
    }

    /**
     * 获取应用的包名
     * @param context
     * @return
     */
    fun getPackageName(context: Context): String {
        //当前应用pid
        val pid = Process.myPid()
        //任务管理类
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //遍历所有应用
        val infos = manager.runningAppProcesses
        for (info in infos) {
            if (info.pid == pid) //得到当前应用
                return info.processName //返回包名
        }
        return ""
    }

    /**
     * 获取状态栏高度
     * @param context
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
     * 获取底部虚拟按键的高度
     * @param context
     * @return
     */
    fun getNavigationHeight(context: Context): Int {
        var result = 0
        if (hasNavBar(context)) {
            val res = context.resources
            val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = res.getDimensionPixelSize(resourceId)
            }
        }
        if (result <= 0) {
            result = dpToPxInt(24f)
        }
        return result
    }

    /**
     * 检查是否存在虚拟按键
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun hasNavBar(context: Context): Boolean {
        val res = context.resources
        val resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android")
        if (resourceId != 0) {
            var hasNav = res.getBoolean(resourceId)
            val sNavBarOverride = navBarOverride
            if ("1" == sNavBarOverride) {
                hasNav = false
            } else if ("0" == sNavBarOverride) {
                hasNav = true
            }
            return hasNav
        } else {
            return !ViewConfiguration.get(context).hasPermanentMenuKey()
        }
    }

    val context: Context?
        /**
         * 反射获取Context
         * @return
         */
        get() {
            try {
                val ActivityThread = Class.forName("android.app.ActivityThread")
                val method = ActivityThread.getMethod("currentActivityThread")
                val currentActivityThread = method.invoke(ActivityThread) //获取currentActivityThread 对象
                val method2 = currentActivityThread.javaClass.getMethod("getApplication")
                return method2.invoke(currentActivityThread) as Context //获取 Context对象
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    /**
     * 检测资源地址是否是直播流
     * @param dataSource
     * @return
     */
    fun isLiveStream(dataSource: String): Boolean {
        if (dataSource.isEmpty()) return false
        if (dataSource.startsWith("htpp") || dataSource.startsWith("htpps")) {
            if (dataSource.endsWith(".m3u8") || dataSource.endsWith(".hks") || dataSource.endsWith(".rtmp")) {
                return true
            }
        }
        return false
    }

    /**
     * 将自己从父Parent中移除
     * @param view
     */
    fun removeViewFromParent(view: View?) {
        if (null != view && null != view.parent && view.parent is ViewGroup) {
            try {
                (view.parent as ViewGroup).removeView(view)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
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

    fun parseLong(content: String): Long {
        if (TextUtils.isEmpty(content)) return 0
        try {
            return content.toLong()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            return 0
        }
    }

    fun parseFloat(content: String): Float {
        try {
            val parseFloat = content.toFloat()
            return parseFloat
        } catch (e: RuntimeException) {
            e.printStackTrace()
            return 0F
        }
    }

    fun parseDouble(progressStr: String, defaultvalue: Double): Double {
        try {
            return progressStr.toDouble()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            return defaultvalue
        }
    }

    /**
     * 根据包名检测是否申明某个权限
     * @param context
     * @param permission
     * @return
     */
    fun existPermission(context: Context?, permission: String): Boolean {
        if (TextUtils.isEmpty(permission)) {
            return false
        }
        val packageManager = context!!.packageManager
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions
            if (null != requestedPermissions && requestedPermissions.size > 0) {
                for (requestedPermission in requestedPermissions) {
                    if (requestedPermission == permission) {
                        return true
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    fun formatHtml(content: String?): Spanned {
//        Logger.d(TAG,"content"+content);
        if (TextUtils.isEmpty(content)) return SpannableString("")
        try {
            return Html.fromHtml(content)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return SpannableString(content)
    }

    /**
     * 给View设置圆角
     * @param view
     * @param radius
     */
    fun setOutlineProvider(view: View, radius: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.outlineProvider = LayoutProvider(radius)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 检查是否需要网络播放
     * @param dataSource
     * @param assetsSource
     * @return
     */
    fun hasNet(dataSource: String, assetsSource: AssetFileDescriptor?): Boolean {
        if (null != assetsSource) {
            return false
        }
        if (TextUtils.isEmpty(dataSource)) {
            return false
        }
        if (dataSource.startsWith("file") || dataSource.startsWith("android")) {
            return false
        }
        if (dataSource.contains("127.0.0.1")) {
            return false
        }
        return true
    }

    val currentTimeStr: String
        /**
         * 获取当前系统时间
         * @return
         */
        get() {
            val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = Date()
            return simpleDateFormat.format(date)
        }

    /**
     * 根据百分比计算出实际占总进度的进度值
     * @param bufferPercent 百分比
     * @param durtion 总数
     * @return
     */
    fun formatBufferPercent(bufferPercent: Int, durtion: Long): Int {
        if (bufferPercent <= 0) return 0
        if (durtion <= 0) return 100
        try {
            return (durtion / 100).toInt() * bufferPercent
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * 判断 悬浮窗口权限是否打开
     * @param context
     * @return true 允许  false禁止
     */
    fun checkWindowsPermission(context: Context): Boolean {
        if (!existPermission(context, Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    return true
                }
                return false
            }
            val `object` = context.getSystemService(Context.APP_OPS_SERVICE) ?: return false
            val localClass: Class<*> = `object`.javaClass
            val arrayOfClass: Array<Class<*>?> = arrayOfNulls(3)
            arrayOfClass[0] = Integer.TYPE
            arrayOfClass[1] = Integer.TYPE
            arrayOfClass[2] = String::class.java
            val method = localClass.getMethod("checkOp", *arrayOfClass) ?: return false
            val arrayOfObject1 = arrayOfNulls<Any>(3)
            arrayOfObject1[0] = 24
            arrayOfObject1[1] = Binder.getCallingUid()
            arrayOfObject1[2] = context.packageName
            val m = (method.invoke(`object`, *arrayOfObject1) as Int)
            return m == AppOpsManager.MODE_ALLOWED
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return false
    }

    /**
     * 用户手指在屏幕边缘检测
     * @param context 上下文
     * @param e 触摸事件
     * @return
     */
    fun isEdge(context: Context, e: MotionEvent): Boolean {
        val edgeSize = dpToPx(context, 40f)
        //        ILogger.d(TAG,"isEdge-->eX:"+e.getRawX()+",eY:"+e.getRawY()+",screenWidt:"+getScreenWidth(context));
        return e.rawX < edgeSize || e.rawX > getScreenWidth(context) - edgeSize || e.rawY < edgeSize || e.rawY > getScreenHeight(context) - edgeSize
    }

    private val navBarOverride: String?
        /**
         * 检查虚拟按键是否被重写
         * @return
         */
        get() {
            var sNavBarOverride: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    val c = Class.forName("android.os.SystemProperties")
                    val m = c.getDeclaredMethod("get", String::class.java)
                    m.isAccessible = true
                    sNavBarOverride = m.invoke(null, "qemu.hw.mainkeys") as String
                } catch (e: Throwable) {
                }
            }
            return sNavBarOverride
        }



}