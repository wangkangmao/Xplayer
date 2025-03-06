package com.wangkm.xplayer.base

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import com.wangkm.xplayer.controller.ControlWrapper
import com.wangkm.xplayer.interfaces.IControllerView
import com.wangkm.xplayer.interfaces.IPlayerControl
import com.wangkm.xplayer.interfaces.IVideoController
import com.wangkm.xplayer.media.IMediaPlayer
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.PlayerUtils.getActivity
import java.util.LinkedList

/**
 * Created by wangkm
 * desc: 视频播放器UI控制器交互基类
 * 1、此控制器维护所有自定义UI组件，负责传达和处理播放器以及UI组件的事件
 * 2、控制器的所有UI组件都支持自定义，调用[.addControllerWidget]添加你的自定义UI组件
 * 3、播放器只能有一个控制器，但一个控制器可以有多个UI交互组件
 * 4、这个基类封装了一些播放器常用的功能方法，请阅读此类的method
 */
abstract class BaseController @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(
        context!!, attrs, defStyleAttr
    ), IVideoController {
    protected var mVideoPlayerControl: IPlayerControl<*>? = null //播放器

    /**
     * 返回播放器交互方向
     * @return
     */
    protected var orientation: Int = IMediaPlayer.ORIENTATION_PORTRAIT
    protected var mPlayerScene: Int = IVideoController.SCENE_NOIMAL //当前控制器(播放器)方向\当前控制器(播放器)场景
    protected var mIControllerViews: LinkedList<IControllerView?>? = LinkedList() //所有自定义UI控制器组件
    private var mControlWrapper: ControlWrapper? = null

    //设置控制器的各UI组件显示、隐藏动画持续时间戳，单位：毫秒
    //返回控制器的各UI组件显示、隐藏动画持续时间戳，单位：毫秒
    override var animationDuration: Long = IVideoController.MATION_DRAUTION //控制交互组件显示|隐藏的动画时长

    /**
     * 是否播放完成
     * @return true:播放完成 false:未播放完成
     */
    override var isCompletion: Boolean = false //是否播放(试看)完成
        protected set
    /**
     * 返回试看模式下的虚拟总时长
     * @return
     */
    /**
     * 设置给用户看的虚拟的视频总时长
     * @param preTotalDuration 单位：毫秒
     */
    override var preViewTotalDuration: Long = 0 //试看模式下总时长

    protected open inner class ExHandel(looper: Looper?) : Handler(looper!!)

    init {
        val layoutId = layoutId
        if (0 != layoutId) {
            val inflate = inflate(context, this.layoutId, null)
            addView(inflate, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        initViews()
    }

    abstract val layoutId: Int

    abstract fun initViews()

    /**
     * 提供给播放器解码器来绑定播放器代理人
     * @param playerControl
     */
    fun attachedPlayer(playerControl: IPlayerControl<*>?) {
        this.mVideoPlayerControl = playerControl
    }

    //=================下列方法由播放器内部回调，请不要随意调用！！！子类复写方法请重载super方法=================
    /**
     * 组件初始化完成，组件已被添加到播放器
     */
    override fun onCreate() {}

    /**
     * 播放器的内部状态发生变化
     * @param state 播放器的内部状态 状态码 参考:PlayerState
     * @param message 描述信息
     */
    override fun onPlayerState(state: PlayerState?, message: String?) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onPlayerState(state, message)
        }
    }

    /**
     * 子类如需关心此回调请复写处理
     * @param currentDurtion 播放进度 主线程回调：当前播放位置,单位：总进度的毫秒进度
     * @param totalDurtion 总时长,单位：毫秒
     */
    override fun onProgress(currentDurtion: Long, totalDurtion: Long) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onProgress(currentDurtion, totalDurtion)
        }
    }

    /**
     * 子类如需关心此回调请复写处理
     * @param bufferPercent 缓冲进度 主线程回调,单位:百分比
     */
    override fun onBuffer(bufferPercent: Int) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onBuffer(bufferPercent)
        }
    }

    /**
     * 竖屏状态下,如果用户设置返回按钮可见仅显示返回按钮,切换到横屏模式下播放时初始都不显示
     * @param orientation 更新控制器方向状态 0:竖屏 1:横屏
     */
    override fun onScreenOrientation(orientation: Int) {
        this.orientation = orientation
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onOrientation(orientation)
        }
    }

    /**
     * 播放器/控制器的场景变化
     * @param playerScene 播放器/控制器的场景变化 0：常规状态(包括竖屏、横屏)，1：activity小窗口，2：全局悬浮窗窗口，3：Android8.0的画中画，4：列表 其它：自定义场景
     */
    override fun onPlayerScene(playerScene: Int) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onPlayerScene(playerScene)
        }
    }

    /**
     * 当静音状态发生了变化回调
     * @param isMute 当静音状态发生了变化回调，true:处于静音状态 false:处于非静音状态
     */
    override fun onMute(isMute: Boolean) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onMute(isMute)
        }
    }

    /**
     * 当播放器的内部画面渲染镜像状态发生了变化回调
     * @param isMirror 当播放器的内部画面渲染镜像状态发生了变化回调， true:处于镜像状态 false:处于非镜像状态
     */
    override fun onMirror(isMirror: Boolean) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onMirror(isMirror)
        }
    }

    /**
     * 当播放器内部渲染缩放模式发生了变化回调
     * @param zoomModel 当播放器内部渲染缩放模式发生了变化回调，，当初始化和播放器缩放模式设置发生变化时回调，参考IMediaPlayer类
     */
    override fun onZoomModel(zoomModel: Int) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onZoomModel(zoomModel)
        }
    }

    /**
     * 生命周期可见,在播放器宿主调用播放的onResume方法后回调
     */
    override fun onResume() {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onResume()
        }
    }

    /**
     * 生命周期不可见,在播放器宿主调用播放的onPause方法后回调
     */
    override fun onPause() {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onPause()
        }
    }

    /**
     * 控制器所有状态重置(由播放器内部回调,与播放器生命周期无关)
     */
    override fun onReset() {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onReset()
        }
    }

    /**
     * 播放器被销毁(由播放器内部回调)
     */
    override fun onDestroy() {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.onDestroy()
        }
        removeAllControllerWidget()
    }

    /**
     * 是否处于列表播放模式，在开始播放和开启\退出全屏时都需要设置
     * @param listPlayerScene 是否处于列表播放模式(需要在开始播放之前设置),列表播放模式下首次渲染不会显示控制器,否则首次渲染会显示控制器 true:处于列表播放模式 false:不处于列表播放模式
     */
    override fun setListPlayerMode(listPlayerScene: Boolean) {
        playerScene = if (listPlayerScene) IVideoController.SCENE_LISTS else IVideoController.SCENE_NOIMAL
    }

    /**
     * 进入画中画模式
     */
    override fun enterPipWindow() {
        playerScene = IVideoController.SCENE_PIP_WINDOW
    }

    /**
     * 退出画中画模式
     */
    override fun quitPipWindow() {
        playerScene = IVideoController.SCENE_NOIMAL
    }

    /**
     * 当切换至小窗口模式播放,取消可能存在的定时器隐藏控制器任务,强制隐藏控制器
     * @param isActivityWindow 控制器是否处于Activity级别窗口模式中
     * @param isGlobalWindow 控制器是否处于全局悬浮窗窗口模式中
     */
    fun setWindowPropertyPlayer(isActivityWindow: Boolean, isGlobalWindow: Boolean) {
        playerScene = if (isActivityWindow) {
            IVideoController.SCENE_ACTIVITY_WINDOW
        } else if (isGlobalWindow) {
            IVideoController.SCENE_GLOBAL_WINDOW
        } else {
            IVideoController.SCENE_NOIMAL
        }
    }

    //===================================控制器提供给宿主或子类调用======================================
    /**
     * 向视频播放器控制器添加自定义UI组件
     * @param controllerView 添加自定义UI组件，必须是实现[IControllerView]接口的UI组件
     */
    override fun addControllerWidget(controllerView: IControllerView?) {
        addControllerWidget(controllerView, -1)
    }

    /**
     * 向视频播放器控制器添加自定义UI组件
     * @param controllerView 添加自定义UI组件，必须是实现[IControllerView]接口的UI组件
     * @param target 唯一的标识，设置此值后可在不同的场景下找到此值对应的Widget组件
     */
    override fun addControllerWidget(controllerView: IControllerView?, target: String?) {
        addControllerWidget(controllerView, target, -1)
    }

    /**
     * 向视频播放器控制器添加自定义UI组件
     * @param controllerView 添加自定义UI组件，必须是实现[IControllerView]接口的UI组件
     * @param index 添加的层级位置,默认是将UI控制组件添加到控制器上层
     */
    override fun addControllerWidget(controllerView: IControllerView?, index: Int) {
        addControllerWidget(controllerView, null, index)
    }

    /**
     * 向视频播放器控制器添加自定义UI组件
     * @param controllerView 添加自定义UI组件，必须是实现[IControllerView]接口的UI组件
     * @param target 唯一的标识，设置此值后可在不同的场景下找到此值对应的Widget组件
     * @param index 添加的层级位置,默认是将UI控制组件添加到控制器上层
     */
    override fun addControllerWidget(controllerView: IControllerView?, target: String?, index: Int) {
        if (null == controllerView) return
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        if (null == mControlWrapper) {
            mControlWrapper = ControlWrapper(this, mVideoPlayerControl)
        }
        controllerView.attachControlWrapper(mControlWrapper)
        if (TextUtils.isEmpty(controllerView.target)) { //未设置target情况下才主动为IControllerView添加target
            controllerView.target = target
        }
        mIControllerViews!!.add(controllerView)
        if (-1 == index) {
            addView(controllerView.view, layoutParams)
        } else {
            addView(controllerView.view, index, layoutParams)
        }
        //组件创建完成，各自定义UI组件可在这里初始化自己的逻辑
        controllerView.onCreate()
        controllerView.onOrientation(orientation) //初始化播放器横竖屏状态
        controllerView.onPlayerScene(playerScene) //初始化播放器应用场景
    }

    /**
     * 向视频播放器控制器批量添加自定义UI组件
     * @param iControllerViews 添加多个自定义UI组件，必须是实现IControllerView接口的UI组件
     */
    override fun addControllerWidget(vararg iControllerViews: IControllerView?) {
        if (iControllerViews.isNotEmpty()) {
            for (iControllerView in iControllerViews) {
                addControllerWidget(iControllerView)
            }
        }
    }

    /**
     * 根据组件tag标识寻找组件实例
     * @param target 标识
     * @return 组件实例化的对象
     */
    override fun findControlWidgetByTag(target: String?): IControllerView? {
        for (iControllerView in mIControllerViews!!) {
            if (target == iControllerView!!.target) {
                return iControllerView
            }
        }
        return null
    }

    /**
     * 移除已添加的自定义UI组件
     * @param controllerView 移除这个实例的控制器
     */
    override fun removeControllerWidget(controllerView: IControllerView?) {
        if (null != controllerView) removeView(controllerView.view)
        if (null != mIControllerViews) mIControllerViews!!.remove(controllerView)
    }

    /**
     * 移除所有已添加的自定义UI组件
     */
    override fun removeAllControllerWidget() {
        if (null != mIControllerViews) {
            for (iControllerView in mIControllerViews!!) {
                removeView(iControllerView!!.view)
            }
            mIControllerViews!!.clear()
        }
    }

    override val isOrientationPortrait: Boolean
        /**
         * 返回是否是竖屏状态
         * @return true:竖屏状态 false:非竖屏状态
         */
        get() = orientation == IMediaPlayer.ORIENTATION_PORTRAIT

    override val isOrientationLandscape: Boolean
        /**
         * 返回控制器是否处于竖屏状态
         * @return true:处于竖屏状态 false:非竖屏状态
         */
        get() = orientation == IMediaPlayer.ORIENTATION_LANDSCAPE

    override var playerScene: Int
        /**
         * 返回控制器当前正处于什么场景，各UI组件初始化后会收到回调：onPlayerScene
         * @return 播放器\控制器场景 0：常规状态(包括竖屏、横屏)，1：activity小窗口，2：全局悬浮窗窗口，3：列表，4：Android8.0的画中画 其它：自定义场景
         */
        get() = mPlayerScene
        /**
         * 更新播放器场景
         * @param playerScene 更新播放器场景，自定义场景可调用此方法设置，设置后会同步通知到所有实现IControllerView接口的UI组件中的onPlayerScene方法
         */
        set(playerScene) {
            this.mPlayerScene = playerScene
            onPlayerScene(mPlayerScene)
        }

    protected val activity: Activity?
        get() {
            if (null != mVideoPlayerControl && null != mVideoPlayerControl!!.getParentContext()) {
                return getActivity(mVideoPlayerControl!!.getParentContext())
            }
            return getActivity(context)
        }

    protected val parentContext: Context?
        get() {
            if (null != mVideoPlayerControl && null != mVideoPlayerControl!!.getParentContext()) {
                return mVideoPlayerControl!!.getParentContext()
            }
            return context
        }

    protected val isPlayering: Boolean
        /**
         * 返回播放器内部是否正在播放
         * @return 是否正处于播放中(准备\开始播放\播放中\缓冲\) true:播放中 false:不处于播放中状态
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.isPlaying()
            }
            return false
        }

    protected val isWorking: Boolean
        /**
         * 播放器是否正处于工作状态(准备\开始播放\缓冲\手动暂停\生命周期暂停) true:工作中 false:空闲状态
         * @return 是否正处于播放中(准备\开始播放\播放中\缓冲\) true:播放中 false:不处于播放中状态
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.isWorking()
            }
            return false
        }


    protected val duration: Long
        /**
         * 返回视频文件总时长
         * @return 单位：毫秒
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.getDuration()
            }
            return 0
        }

    protected val currentPosition: Long
        /**
         * 返回正在播放的位置
         * @return 单位：毫秒
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.getCurrentPosition()
            }
            return 0
        }

    protected val videoWidth: Int
        /**
         * 返回视频分辨率-宽
         * @return 单位：像素
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.getVideoWidth()
            }
            return 0
        }

    protected val videoHeight: Int
        /**
         * 返回视频分辨率-高
         * @return 单位：像素
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.getVideoHeight()
            }
            return 0
        }

    protected val buffer: Int
        /**
         * 返回当前视频缓冲的进度
         * @return 单位：百分比
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.getBuffer()
            }
            return 0
        }

    /**
     * 快进\快退
     * @param msec 毫秒进度条
     */
    protected fun seekTo(msec: Long) {
        if (null != mVideoPlayerControl) {
            mVideoPlayerControl!!.seekTo(msec)
        }
    }

    /**
     * 开始\暂停播放
     */
    protected fun togglePlay() {
        if (null != mVideoPlayerControl) mVideoPlayerControl!!.togglePlay()
    }

    /**
     * 结束播放
     */
    protected fun stopPlay() {
        if (null != mVideoPlayerControl) mVideoPlayerControl!!.onStop()
    }

    /**
     * 开启全屏播放
     */
    protected fun startFullScreen() {
        if (null != mVideoPlayerControl) mVideoPlayerControl!!.startFullScreen()
    }

    /**
     * 开启\退出全屏播放
     */
    protected fun toggleFullScreen() {
        if (null != mVideoPlayerControl) mVideoPlayerControl!!.toggleFullScreen()
    }

//    fun startWindow() {
//        startWindow(true)
//    }
//
//    fun startWindow(isAutoSorption: Boolean) {
//        startWindow(0, 0, 0f, 0f, 0f, 0, isAutoSorption)
//    }
//
//    fun startWindow(radius: Float, bgColor: Int) {
//        startWindow(radius, bgColor, true)
//    }
//
//    fun startWindow(radius: Float, bgColor: Int, isAutoSorption: Boolean) {
//        startWindow(0, 0, 0f, 0f, radius, bgColor, isAutoSorption)
//    }
//
//    fun startWindow(width: Int, height: Int, startX: Float, startY: Float) {
//        startWindow(width, height, startX, startY, true)
//    }
//
//    fun startWindow(width: Int, height: Int, startX: Float, startY: Float, isAutoSorption: Boolean) {
//        startWindow(width, height, startX, startY, 0f, 0, true)
//    }
//
//    fun startWindow(width: Int, height: Int, startX: Float, startY: Float, radius: Float) {
//        startWindow(width, height, startX, startY, radius, true)
//    }
//
//    fun startWindow(width: Int, height: Int, startX: Float, startY: Float, radius: Float, isAutoSorption: Boolean) {
//        startWindow(width, height, startX, startY, radius, Color.parseColor("#99000000"), isAutoSorption)
//    }
//
//    fun startWindow(width: Int, height: Int, startX: Float, startY: Float, radius: Float, bgColor: Int) {
//        startWindow(width, height, startX, startY, radius, bgColor, true)
//    }

    protected val isSoundMute: Boolean
        /**
         * 是否开启了静音
         * @return true:已开启静音 false:系统音量
         */
        get() {
            if (null != mVideoPlayerControl) {
                return mVideoPlayerControl!!.isSoundMute()
            }
            return false
        }

    /**
     * 设置\取消静音
     * @param soundMute true:静音 false:系统音量
     * @return true:已开启静音 false:系统音量
     */
    protected fun setSoundMute(soundMute: Boolean): Boolean {
        if (null != mVideoPlayerControl) {
            return mVideoPlayerControl!!.setSoundMute(soundMute)
        }
        return false
    }

    /**
     * 静音、取消静音
     */
    protected fun toggleMute(): Boolean {
        if (null != mVideoPlayerControl) {
            return mVideoPlayerControl!!.toggleMute()
        }
        return false
    }

    /**
     * 镜像、取消镜像
     */
    protected fun toggleMirror(): Boolean {
        if (null != mVideoPlayerControl) {
            return mVideoPlayerControl!!.toggleMirror()
        }
        return false
    }

    protected val orientationStr: String
        get() = ",Orientation:" + orientation

    protected val isActivityWindow: Boolean
        /**
         * 返回是否是Activity悬浮窗窗口模式
         * @return true:当前正处于Activity窗口模式 false:当前不处于Activity窗口模式
         */
        get() = IVideoController.SCENE_ACTIVITY_WINDOW == playerScene

    protected val isGlobalWindow: Boolean
        /**
         * 返回是否是全局悬浮窗窗口模式
         * @return true:当前正处于全局悬浮窗窗口模式 false:当前不处于全局悬浮窗窗口模式
         */
        get() = IVideoController.SCENE_GLOBAL_WINDOW == playerScene

    protected val isPipWindow: Boolean
        /**
         * 返回是否是画中画窗口模式
         * @return true:当前正处于画中画窗口模式 false:当前不处于画中画窗口模式
         */
        get() = IVideoController.SCENE_PIP_WINDOW == playerScene

    protected val isListPlayerScene: Boolean
        /**
         * 是否处于列表模式下播放
         * @return true:是 false:否
         */
        get() = IVideoController.SCENE_LISTS == playerScene

    /**
     * 单击事件下-控制器组件显示
     * @param isAnimation 是否启用动画
     */
    protected fun showWidget(isAnimation: Boolean) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.showControl(isAnimation)
        }
    }

    /**
     * 单击事件下-控制器组件隐藏
     * @param isAnimation 是否启用动画
     */
    protected fun hideWidget(isAnimation: Boolean) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.hideControl(isAnimation)
        }
    }

    /**
     * 返回ID对应的字符串
     * @param resId 资源ID
     * @return
     */
    protected fun getString(resId: Int): String {
        return context.resources.getString(resId)
    }


    //==================下面这些方法时不常用的，子类如果需要处理下列方法,请复写实现自己的逻辑====================
    override val isControllerShowing: Boolean
        /**
         * 返回seek控制器是否正在显示中
         * @return
         */
        get() {
            var isShowing = false
            for (iControllerView in mIControllerViews!!) {
                if (iControllerView!!.isSeekBarShowing) {
                    isShowing = true
                    break
                }
            }
            return isShowing
        }

    /**
     * 请求其它所有UI组件隐藏自己的控制器,是否开启动画
     * @param isAnimation 请求其它所有UI组件隐藏自己的控制器,是否开启动画
     */
    override fun hideAllController(isAnimation: Boolean) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.hideControl(isAnimation)
        }
    }

    //设置视频标题内容
    override fun setTitle(videoTitle: String?) {
        for (iControllerView in mIControllerViews!!) {
            iControllerView!!.setTitle(videoTitle)
        }
    }

    //开始延时任务
    override fun startDelayedRunnable() {}

    //取消延时任务
    override fun stopDelayedRunnable() {}

    //重新开始延时任务。适用于：当有组件产生了交互后，需要重新开始倒计时关闭控制任务时的场景
    override fun reStartDelayedRunnable() {}

    companion object {
        protected val TAG: String = BaseController::class.java.simpleName
    }
}