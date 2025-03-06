package com.wangkm.xplayer.utils

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation

/**
 * created by wangkm
 * Desc:动画处理
 */
class AnimationUtils {
    interface OnAnimationListener {
        fun onAnimationEnd(animation: Animation?)
    }

    /**
     * 从所在位置往左边平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateLocatToLeft(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 0, false, durationMillis, listener)
    }

    /**
     * 从左边往所在位置平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateLeftToLocat(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 0, true, durationMillis, listener)
    }

    /**
     * 从所在位置往上方平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateLocatToTop(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 1, false, durationMillis, listener)
    }

    /**
     * 从上方往所在位置平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateTopToLocat(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 1, true, durationMillis, listener)
    }


    /**
     * 从所在位置往右边平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateLocatToRight(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 2, false, durationMillis, listener)
    }

    /**
     * 从右边往所在位置平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateRightToLocat(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 2, true, durationMillis, listener)
    }

    /**
     * 从所在位置往下方平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateLocatToBottom(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 3, false, durationMillis, listener)
    }

    /**
     * 从下方往所在位置平移
     * @param targetView 锚点View
     * @param durationMillis 动画持续时长
     * @param listener 监听器
     */
    fun startTranslateBottomToLocat(targetView: View?, durationMillis: Long, listener: OnAnimationListener?) {
        startTranslateAnimation(targetView, 3, true, durationMillis, listener)
    }

    /**
     * 开始播放 上\下\左\右 平移 进\出 动画
     * @param targetView 锚点View
     * @param direction 以锚点targetView真实所在位置为中心的方向(motion为true时表示从哪个方向来,为false时表示将要往哪个方向去) 0：左，1：上， 2：右，3：下
     * @param motion 动画相对自身targetView所在位置的意图，true:进 flase:出
     * @param durationMillis 动画时长
     * @param listener 监听器
     */
    private fun startTranslateAnimation(
        targetView: View?,
        direction: Int,
        motion: Boolean,
        durationMillis: Long,
        listener: OnAnimationListener?
    ) {
        if (null == targetView) {
            listener?.onAnimationEnd(null)
            return
        }
        TranslateYAnimation().startTranslateAnimation(targetView, direction, motion, durationMillis, listener)
    }

    private inner class TranslateYAnimation {
        private var mTargetView: View? = null
        private var mOnAnimationListener: OnAnimationListener? = null

        fun startTranslateAnimation(
            targetView: View?,
            direction: Int,
            motion: Boolean,
            durationMillis: Long,
            listener: OnAnimationListener?
        ) {
            this.mTargetView = targetView
            this.mOnAnimationListener = listener
            var animation: TranslateAnimation? = null
            when (direction) {
                0 -> animation = if (motion) { //进
                    moveFormLeftToLocat()
                } else { //出
                    moveFormLocatToLeft()
                }

                1 -> animation = if (motion) {
                    moveFormTopToLocat()
                } else {
                    moveFromLocatToTop()
                }

                2 -> animation = if (motion) {
                    moveFormRightToLocat()
                } else {
                    moveFormLocatToRight()
                }

                3 -> animation = if (motion) {
                    moveFormBottomToLocat()
                } else {
                    moveFormLocatToBottom()
                }
            }
            if (null != animation) {
                animation.duration = durationMillis
                animation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        if (null != mOnAnimationListener) mOnAnimationListener!!.onAnimationEnd(animation)
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                    }
                })
                mTargetView!!.visibility = View.VISIBLE
                mTargetView!!.startAnimation(animation)
            } else {
                if (null != mOnAnimationListener) mOnAnimationListener!!.onAnimationEnd(null)
            }
        }
    }

    /**
     * 从控件所在位置平移到控件所在位置左边
     * 从所在位置往左出场
     * @return
     */
    private fun moveFormLocatToLeft(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        return animation
    }

    /**
     * 从控件所在位置左边平移到控件所在位置
     * 从所在位置左边往所在位置进场
     * @return
     */
    private fun moveFormLeftToLocat(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, -1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        return animation
    }

    /**
     * 从控件所在位置平移到控件所在位置的顶部
     * 从所在位置往上出场
     * @return
     */
    private fun moveFromLocatToTop(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f
        )
        return animation
    }

    /**
     * 从控件所在位置顶部平移到控件所在位置
     * 从上方往所在位置进场
     * @return
     */
    private fun moveFormTopToLocat(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        return animation
    }

    /**
     * 从控件所在位置平移到控件所在位置右边
     * 从所在位置往右出场
     * @return
     */
    private fun moveFormLocatToRight(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        return animation
    }

    /**
     * 从控件所在位置右边平移到控件所在位置
     * 从所在位置右边往所在位置进场
     * @return
     */
    private fun moveFormRightToLocat(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        return animation
    }

    /**
     * 从控件所在位置平移到控件所在位置的底部
     * 从所在位置往下出场
     * @return
     */
    private fun moveFormLocatToBottom(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            0.0f, Animation.RELATIVE_TO_SELF, 1.0f
        )
        return animation
    }

    /**
     * 从控件所在位置底部平移到控件所在位置
     * 从下方往所在位置进场
     * @return
     */
    private fun moveFormBottomToLocat(): TranslateAnimation {
        val animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
            1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        return animation
    }


    /**
     * 播放透明动画
     * @param view 目标View
     * @param duration 动画时长
     * @param isFillAfter 是否停留在最后一帧
     * @param listener 状态监听器
     */
    fun startAlphaAnimatioTo(view: View?, duration: Long, isFillAfter: Boolean, listener: OnAnimationListener?) {
        if (null == view) return
        AnimationTask().start(view, duration, isFillAfter, listener)
    }

    /**
     * 动画执行
     */
    private inner class AnimationTask {
        private var mView: View? = null
        private var mOnAnimationListener: OnAnimationListener? = null

        fun start(view: View?, duration: Long, isFillAfter: Boolean, listener: OnAnimationListener?) {
            this.mView = view
            this.mOnAnimationListener = listener
            val alphaAnim = AlphaAnimation(1f, 0f)
            alphaAnim.duration = duration
            alphaAnim.fillAfter = isFillAfter
            alphaAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                }

                override fun onAnimationEnd(animation: Animation) {
                    if (null != mOnAnimationListener) mOnAnimationListener!!.onAnimationEnd(animation)
                }

                override fun onAnimationRepeat(animation: Animation) {
                }
            })
            mView!!.startAnimation(alphaAnim)
        }
    }

    /**
     * 播放透明动画
     * @param view 目标View
     * @param duration 动画时长
     * @param isFillAfter 是否停留在最后一帧
     * @param listener 状态监听器
     */
    fun startAlphaAnimatioFrom(view: View?, duration: Long, isFillAfter: Boolean, listener: OnAnimationListener?) {
        if (null == view) return
        AnimationTaskFrom().start(view, duration, isFillAfter, listener)
    }

    /**
     * 动画执行
     */
    private inner class AnimationTaskFrom {
        private var mView: View? = null
        private var mOnAnimationListener: OnAnimationListener? = null

        fun start(view: View?, duration: Long, isFillAfter: Boolean, listener: OnAnimationListener?) {
            this.mView = view
            this.mOnAnimationListener = listener
            mView!!.visibility = View.VISIBLE
            val alphaAnim = AlphaAnimation(0f, 1f)
            alphaAnim.duration = duration
            alphaAnim.fillAfter = isFillAfter
            alphaAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                }

                override fun onAnimationEnd(animation: Animation) {
                    if (null != mOnAnimationListener) mOnAnimationListener!!.onAnimationEnd(animation)
                }

                override fun onAnimationRepeat(animation: Animation) {
                }
            })
            mView!!.startAnimation(alphaAnim)
        }
    }

    companion object {
        private const val TAG = "AnimationUtils"
        private var mInstance: AnimationUtils? = null

        @get:Synchronized
        val instance: AnimationUtils?
            get() {
                synchronized(AnimationUtils::class.java) {
                    if (null == mInstance) {
                        mInstance = AnimationUtils()
                    }
                }
                return mInstance
            }
    }
}