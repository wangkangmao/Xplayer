package com.wangkm.xplayer.manager

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import ccom.wangkm.xplayer.R
import com.wangkm.xplayer.base.BasePlayer
import com.wangkm.xplayer.listener.OnWindowActionListener
import com.wangkm.xplayer.utils.ILogger
import com.wangkm.xplayer.utils.PlayerUtils
import com.wangkm.xplayer.widget.view.WindowPlayerFloatView

/**
 * created by wangkm
 * Desc:全局悬浮窗口播放管理者,开发者可自行调用api来添加自定义的VideoPlayer到窗口中
 */
class IWindowManager() {
    private var mLayoutParams: WindowManager.LayoutParams? = null

    private var mPlayerContainer: WindowPlayerFloatView? = null
    var windowActionListener: OnWindowActionListener? = null
        private set
    var customParams: Any? = null //自定义参数
        private set

    private val windowManager: WindowManager?
        get() = getWindowManager(PlayerUtils.context!!)

    private fun getWindowManager(context: Context): WindowManager? {
        if (mWindowManager == null) {
            mWindowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
        return mWindowManager
    }

    /**
     * 初始化全局悬浮窗初始化参数
     * @param context 播放器上下文
     * @param basePlayer 播放器
     * @param width 悬浮窗的宽，默认为：屏幕宽度/2+30dp
     * @param height 悬浮窗的高，默认为：width*9/16
     * @param startX 位于屏幕的X起始位置，如果为0第一次渲染全局悬浮窗时：屏幕宽度/2-30dp-12dp；非初次渲染全局悬浮窗：使用最后一次关闭窗口前的位置
     * @param startY 位于屏幕的Y起始位置，如果为0第一次渲染全局悬浮窗时：播放器位于屏幕的Y轴+播放器高度+边距(12dp)；非初次渲染全局悬浮窗：使用最后一次关闭窗口前的位置
     */
    private fun initParams(context: Context, basePlayer: BasePlayer, width: Int, height: Int, startX: Float, startY: Float) {
        var width = width
        var height = height
        var startX = startX
        var startY = startY
        if (null == mLayoutParams) {
            mLayoutParams = WindowManager.LayoutParams()
            //WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            } else {
                mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_TOAST
            }
            //不拦截焦点、使焦点穿透到底层
            mLayoutParams!!.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            //背景透明
            mLayoutParams!!.format = PixelFormat.RGBA_8888
            //需要默认位于屏幕的左上角，具体定位用x,y轴
            mLayoutParams!!.gravity = Gravity.LEFT or Gravity.TOP

            val screenLocation = IntArray(2)
            var parent: ViewGroup? = null
            //1.从原有竖屏窗口移除自己前保存自己的Parent,直接开启全屏是不存在宿主ViewGroup的,可直接窗口转场
            if (null != basePlayer.parent && basePlayer.parent is ViewGroup) {
                parent = basePlayer.parent as ViewGroup
                parent.getLocationInWindow(screenLocation)
            }
            //2.获取宿主的View属性和startX、Y轴
            if (width <= 0 || height <= 0) {
                width = PlayerUtils.getScreenWidth(context) / 2 + PlayerUtils.dpToPxInt(30f)
                height = width * 9 / 16
                //                ILogger.d(TAG,"initParams-->未传入宽或高,width:"+width+",height:"+height);
            }
            //如果传入的startX不存在，则startX起点位于屏幕宽度1/2-距离右侧15dp位置，startY起点位于宿主View的下方12dp处
            if (startX <= 0 && null != parent) {
                startX = ((PlayerUtils.getScreenWidth(context) / 2 - PlayerUtils.dpToPxInt(30f)) - PlayerUtils.dpToPxInt(12f)).toFloat()
                startY = (screenLocation[1] + parent.height + PlayerUtils.dpToPxInt(12f)).toFloat()
                //                ILogger.d(TAG,"initParams-->未传入X,Y轴,取父容器位置,startX:"+startX+",startY:"+startY);
            }
            //如果宿主也不存在，则startX起点位于屏幕宽度1/2-距离右侧12dp位置，startY起点位于屏幕高度-Window View 高度+12dp位置处
            if (startX <= 0) {
                startX = ((PlayerUtils.getScreenWidth(context) / 2 - PlayerUtils
                    .dpToPxInt(30f)) - PlayerUtils.dpToPxInt(12f)).toFloat()
                startY = PlayerUtils.dpToPxInt(60f).toFloat()
                //                ILogger.d(TAG,"initParams-->未传入X,Y轴或取父容器位置失败,startX:"+startX+",startY:"+startY);
            }
            //            ILogger.d(TAG,"initParams-->final:width:"+width+",height:"+height+",startX:"+startX+",startY:"+startY);
            mLayoutParams!!.width = width
            mLayoutParams!!.height = height
            mLayoutParams!!.x = startX.toInt()
            mLayoutParams!!.y = startY.toInt()
            mLayoutParams!!.windowAnimations = R.style.WindowAnimation //悬浮窗开启动画
        } else {
            if (width > 0 || height > 0) {
                mLayoutParams!!.width = width
                mLayoutParams!!.height = height
            }
        }
    }

    /**
     * 将播放器添加到全局窗口中
     * @param context context 上下文
     * @param basePlayer 继承自BasePlayer的播放器实例
     * @param width 悬浮窗的宽，默认为：屏幕宽度/2+30dp
     * @param height 悬浮窗的高，默认为：width*9/16
     * @param startX 位于屏幕的X起始位置，如果为0第一次渲染全局悬浮窗时：屏幕宽度/2-30dp-12dp；非初次渲染全局悬浮窗：使用最后一次关闭窗口前的位置
     * @param startY 位于屏幕的Y起始位置，如果为0第一次渲染全局悬浮窗时：播放器位于屏幕的Y轴+播放器高度+边距(12dp)；非初次渲染全局悬浮窗：使用最后一次关闭窗口前的位置
     * @param radius 窗口的圆角 单位:像素
     * @param bgColor 窗口的背景颜色
     * @param isAutoSorption 触摸松手后是否自动吸附到屏幕边缘
     */
    fun addGlobalWindow(
        context: Context,
        basePlayer: BasePlayer,
        width: Int,
        height: Int,
        startX: Float,
        startY: Float,
        radius: Float,
        bgColor: Int,
        isAutoSorption: Boolean
    ): Boolean {
        quitGlobalWindow() //清除可能存在的窗口播放器
        try {
            //悬浮窗口准备
            val windowManager = getWindowManager(context)
            initParams(context, basePlayer, width, height, startX, startY)
            //从原宿主中移除播放器
            PlayerUtils.removeViewFromParent(basePlayer)
            //初始化一个装载播放器的手势容器
            mPlayerContainer = WindowPlayerFloatView(context)
            mPlayerContainer!!.setOnWindowActionListener(object : OnWindowActionListener {
                override fun onMovie(x: Float, y: Float) {
                    if (null != mLayoutParams) {
                        mLayoutParams!!.x = x.toInt()
                        if (-1f != y) { //过滤自动吸附事件
                            mLayoutParams!!.y = y.toInt()
                        }
                        windowManager?.updateViewLayout(mPlayerContainer, mLayoutParams)
                    }
                }

                override fun onClick(basePlayer: BasePlayer?, customParams: Any?) {
                    ILogger.d(TAG, "onClick-->customParams:$customParams")
                    if (null != windowActionListener) {
                        windowActionListener!!.onClick(basePlayer, customParams)
                    }
                }

                override fun onClose() {
                    if (null != windowActionListener) {
                        windowActionListener!!.onClose()
                    } else {
                        quitGlobalWindow()
                    }
                }
            })
            windowManager!!.addView(mPlayerContainer, mLayoutParams)
            mPlayerContainer!!.addPlayerView(basePlayer, width, height, radius, bgColor, isAutoSorption) //先将播放器包装到可托拽的容器中
            return true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 清除悬浮窗所有View
     */
    fun quitGlobalWindow() {
        if (null != mPlayerContainer) {
            getWindowManager(mPlayerContainer!!.context)!!.removeViewImmediate(mPlayerContainer)
            //销毁此前的播放器
            mPlayerContainer!!.onReset()
            mPlayerContainer = null
        }
        customParams = null
    }

    val basePlayer: BasePlayer?
        get() {
            if (null != mPlayerContainer) {
                return mPlayerContainer!!.basePlayer
            }
            return null
        }

    /**
     * 设置自定义参数，在收到
     * @param coustomParams
     */
    fun setCustomParams(coustomParams: Any?): IWindowManager? {
        customParams = coustomParams
        return mInstance
    }

    /**
     * 注册监听器,监听点击悬浮窗口播放器事件
     * @param listener
     */
    fun setOnWindowActionListener(listener: OnWindowActionListener?) {
        windowActionListener = listener
    }

    /**
     * 提供给窗口控制器调用的,方便将点击事件抛给开发者
     * 为什么要这样写:因为悬浮窗播放器的宿主界面已经不存在了,只能抛给全局的关心点击悬浮窗的监听器来处理
     */
    fun onClickWindow() {
        if (null != windowActionListener && null != mPlayerContainer) {
            val basePlayer = mPlayerContainer!!.basePlayer
            if (null != basePlayer) {
                windowActionListener!!.onClick(basePlayer, customParams)
            }
        }
    }

    /**
     * 恢复播放
     */
    fun onResume() {
        if (null != mPlayerContainer) mPlayerContainer!!.onResume()
    }

    /**
     * 暂停播放
     */
    fun onPause() {
        if (null != mPlayerContainer) mPlayerContainer!!.onPause()
    }

    /**
     * 清除悬浮窗播放器及其容器&&将其还原到常规模式,但不销毁播放器
     */
    fun onClean() {
        val basePlayer = basePlayer
        PlayerUtils.removeViewFromParent(basePlayer) //从原有全局悬浮窗口移除
        basePlayer?.onRecover()
        if (null != mPlayerContainer) {
            mPlayerContainer!!.removeAllViews()
            getWindowManager(mPlayerContainer!!.context)!!.removeViewImmediate(mPlayerContainer)
            mPlayerContainer = null
        }
    }

    /**
     * 清除悬浮窗及所有设置
     */
    fun onReset() {
        quitGlobalWindow()
        mLayoutParams = null
        mWindowManager = null
    }

    companion object {
        private const val TAG = "IWindowManager"

        @Volatile
        private var mInstance: IWindowManager? = null

        //以下二个变量将时常驻内存，只初始化一次
        private var mWindowManager: WindowManager? = null

        @JvmStatic
        @get:Synchronized
        val instance: IWindowManager?
            get() {
                synchronized(IWindowManager::class.java) {
                    if (null == mInstance) {
                        mInstance = IWindowManager()
                    }
                }
                return mInstance
            }
    }
}