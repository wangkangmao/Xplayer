package com.wangkm.xplayer.controller

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.wangkm.xplayer.base.BaseController
import com.wangkm.xplayer.interfaces.IGestureControl
import com.wangkm.xplayer.media.IMediaPlayer
import com.wangkm.xplayer.utils.PlayerUtils
import kotlin.math.abs

/**
 * created by wangkm
 * Desc:带有手势交互的基础控制器,需要实现手势交互的控制器可继承此类
 * 1、如果需要自定义处理手势交互改变屏幕亮度、系统音量、快进、快退等UI交互，请实现[IGestureControl]接口
 */
abstract class GestureController(context: Context?) : BaseController(context), OnTouchListener {
    //GestureDetector.OnGestureListener
    private var mGestureDetector: GestureDetector? = null
    private var mAudioManager: AudioManager? = null

    //设置相关
    private var mCanTouchPosition = true //是否可以滑动调节进度，默认可以
    private var mCanTouchInPortrait = true //是否在竖屏模式下开启手势控制，默认开启
    private var mIsGestureEnabled = true //是否开启手势控制，默认开启，关闭之后，手势调节进度，音量，亮度功能将关闭
    private var mIsDoubleTapTogglePlayEnabled = false //是否开启双击播放/暂停，默认关闭

    //逻辑相关
    private var mChangePosition = false //是否允许滑动seek播放
    private var mChangeBrightness = false //是否允许滑动更改屏幕亮度
    private var mChangeVolume = false //是否允许滑动更改系统音量
    private var mStreamVolume = 0
    private var mBrightness = 0f
    private var mSeekPosition = -1
    private var mFirstTouch = false
    private var mCanSlide = mCanTouchPosition

    /**
     * 是否锁住了屏幕
     * @return
     */
    protected open var isLocked: Boolean = false //屏幕锁是否启用
        set

    override val layoutId: Int
        get() = 0

    override fun initViews() {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mGestureDetector = GestureDetector(context, SimpleOnGesture())
        this.setOnTouchListener(this)
    }

    //单击
    protected abstract fun onSingleTap()

    //双击
    protected abstract fun onDoubleTap()

    /**
     * 设置是否可以滑动调节进度，默认可以
     * @param canTouchPosition true:允许滑动快进快退 false:不允许滑动快进快退
     */
    fun setCanTouchPosition(canTouchPosition: Boolean) {
        mCanTouchPosition = canTouchPosition
    }

    /**
     * 是否在竖屏模式下开始手势控制，默认开启
     * @param canTouchInPortrait true:开始竖屏状态下的手势交互 false:关闭竖屏状态下的手势交互
     */
    fun setCanTouchInPortrait(canTouchInPortrait: Boolean) {
        mCanTouchInPortrait = canTouchInPortrait
        mCanSlide = mCanTouchInPortrait
    }

    /**
     * 是否开启手势控制，默认开启，关闭之后，手势调节进度，音量，亮度功能将关闭
     * @param gestureEnabled true:允许手势交互 false:不允许手势交互
     */
    fun setGestureEnabled(gestureEnabled: Boolean) {
        mIsGestureEnabled = gestureEnabled
    }

    /**
     * 是否开启双击播放/暂停，默认关闭
     * @param enabled true:允许双击播放\暂停 false:不允许双击播放\暂停
     */
    fun setDoubleTapTogglePlayEnabled(enabled: Boolean) {
        mIsDoubleTapTogglePlayEnabled = enabled
    }

    /**
     * 接管处理手势识别
     */
    private inner class SimpleOnGesture : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            val edge = parentContext?.let { PlayerUtils.isEdge(it, e) }
            //            ILogger.d(TAG,"onDown-->isPlayering:"+isPlayering()+",edge:"+edge+",mIsGestureEnabled:"+mIsGestureEnabled+",e:"+e.getAction());
            if (!isPlayering //不处于播放状态
                || !mIsGestureEnabled //关闭了手势
                || edge == true
            ) //处于屏幕边沿
                return true
            mStreamVolume = mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
            val activity: Activity? = activity
            mBrightness = activity?.window?.attributes?.screenBrightness ?: 0f
            mFirstTouch = true
            mChangePosition = false
            mChangeBrightness = false
            mChangeVolume = false
            return true
        }

        /**
         * 单机
         * @param e
         * @return
         */
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTap()
            return true
        }

        /**
         * 双击，双击事件消费后单击事件也消费了
         * @param e
         * @return
         */
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mIsDoubleTapTogglePlayEnabled) {
                this@GestureController.onDoubleTap()
            }
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
//            boolean edge1 = PlayerUtils.getInstance().isEdge(getParentContext(), e1);
//            boolean edge2 = PlayerUtils.getInstance().isEdge(getParentContext(), e2);
//        ILogger.d(TAG,"onScroll-->IsGestureEnabled:"+mIsGestureEnabled+",mCanSlide:"+mCanSlide+",mFirstTouch:"+mFirstTouch);
            if (!isPlayering //不处于播放状态
                || !mIsGestureEnabled //关闭了手势
                || !mCanSlide //关闭了滑动手势
                || isLocked //锁住了屏幕
                || (e1 != null && PlayerUtils.isEdge(parentContext!!, e1))
            ) { // //处于屏幕边沿
                return true
            }
            if (e1 == null) return true
            val deltaX = e1.x - e2.x
            val deltaY = e1.y - e2.y
            //手指按下首次处理,通知UI交互组件处理手势按下事件
            if (mFirstTouch) {
                mChangePosition = abs(distanceX.toDouble()) >= abs(distanceY.toDouble())
                if (!mChangePosition) {
                    //半屏宽度
                    val halfScreen = PlayerUtils.getScreenWidth(context) / 2
                    if (e2.x > halfScreen) {
                        mChangeVolume = true
                    } else {
                        mChangeBrightness = true
                    }
                }
                if (mChangePosition) {
                    //根据用户设置是否可以滑动调节进度来决定最终是否可以滑动调节进度
                    mChangePosition = mCanTouchPosition
                }
                //                ILogger.d(TAG,"onScroll-->mChangePosition:"+mChangePosition+",mChangeBrightness:"+mChangeBrightness+",mChangeVolume:"+mChangeVolume);
                if (mChangePosition || mChangeBrightness || mChangeVolume) {
                    for (iControllerView in mIControllerViews!!) {
                        if (iControllerView is IGestureControl) {
                            (iControllerView as IGestureControl).onStartSlide()
                        }
                    }
                }
                mFirstTouch = false
            }
            if (mChangePosition) { //seek播放进度
                slideToChangePosition(deltaX)
            } else if (mChangeBrightness) { //更改屏幕亮度
                slideToChangeBrightness(deltaY)
            } else if (mChangeVolume) { //更改系统音量
                slideToChangeVolume(deltaY)
            }
            return true
        }
    }

    override fun onScreenOrientation(orientation: Int) {
        super.onScreenOrientation(orientation)
        mCanSlide = if (IMediaPlayer.ORIENTATION_PORTRAIT == orientation) {
            mCanTouchInPortrait //竖屏使用用户配置的
        } else {
            true //横屏强制开启手势交互
        }
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (null != mGestureDetector) {
            return mGestureDetector!!.onTouchEvent(event)
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (null != mGestureDetector) {
            //滑动结束时事件处理
            if (!mGestureDetector!!.onTouchEvent(event)) {
                val action = event.action
                when (action) {
                    MotionEvent.ACTION_UP -> {
                        stopSlide()
                        if (mSeekPosition > -1) {
                            if (null != mVideoPlayerControl) mVideoPlayerControl!!.seekTo(mSeekPosition.toLong())
                            mSeekPosition = -1
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        stopSlide()
                        mSeekPosition = -1
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 改变播放进度
     * @param deltaX
     */
    private fun slideToChangePosition(deltaX: Float) {
        var deltaX = deltaX
        if (null != mVideoPlayerControl) {
            deltaX = -deltaX
            val width = measuredWidth
            val duration = mVideoPlayerControl!!.getDuration().toInt()
            val currentPosition = mVideoPlayerControl!!.getCurrentPosition().toInt()
            var position = (deltaX / width * 120000 + currentPosition).toInt()
            if (position > duration) position = duration
            if (position < 0) position = 0
            for (iControllerView in mIControllerViews!!) {
                if (iControllerView is IGestureControl) {
                    (iControllerView as IGestureControl).onPositionChange(position, currentPosition, duration)
                }
            }
            mSeekPosition = position
        }
    }

    /**
     * 改变屏幕亮度
     * @param deltaY
     */
    private fun slideToChangeBrightness(deltaY: Float) {
        val activity = activity ?: return
        val window = activity.window
        val attributes = window.attributes
        val height = measuredHeight
        if (mBrightness == -1.0f) mBrightness = 0.5f
        var brightness = deltaY * 2 / height * 1.0f + mBrightness
        if (brightness < 0) {
            brightness = 0f
        }
        if (brightness > 1.0f) brightness = 1.0f
        val percent = (brightness * 100).toInt()
        attributes.screenBrightness = brightness
        window.attributes = attributes
        for (iControllerView in mIControllerViews!!) {
            if (iControllerView is IGestureControl) {
                (iControllerView as IGestureControl).onBrightnessChange(percent)
            }
        }
    }


    /**
     * 改变音量
     * @param deltaY
     */
    private fun slideToChangeVolume(deltaY: Float) {
        val streamMaxVolume = mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val height = measuredHeight
        val deltaV = deltaY * 2 / height * streamMaxVolume
        var index = mStreamVolume + deltaV
        if (index > streamMaxVolume) index = streamMaxVolume.toFloat()
        if (index < 0) index = 0f
        val percent = (index / streamMaxVolume * 100).toInt()
        mAudioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)
        for (iControllerView in mIControllerViews!!) {
            if (iControllerView is IGestureControl) {
                (iControllerView as IGestureControl).onVolumeChange(percent)
            }
        }
    }

    /**
     * 手势操作取消
     */
    private fun stopSlide() {
        for (iControllerView in mIControllerViews!!) {
            if (iControllerView is IGestureControl) {
                (iControllerView as IGestureControl).onStopSlide()
            }
        }
    }

    protected fun setLocker(locker: Boolean) {
        isLocked = locker
    }
}