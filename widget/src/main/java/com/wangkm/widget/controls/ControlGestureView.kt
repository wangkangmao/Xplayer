package com.wangkm.widget.controls

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.wangkm.widget.R
import com.wangkm.xplayer.base.BaseControlWidget
import com.wangkm.xplayer.interfaces.IGestureControl
import com.wangkm.xplayer.media.IMediaPlayer
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.PlayerUtils

/**
 * created by wangkm
 * Desc:手势交互声音、亮度、快进、快退等UI交互
 */
class ControlGestureView(context: Context?) : BaseControlWidget(context),
    IGestureControl {
    private var mController: View? = null //UI交互区域
    private var mPresentIcon: ImageView? = null //ICON
    private var mPresentText: TextView? = null //进度文字
    private var mPresentProgress: ProgressBar? = null //亮度、声音进度

    override fun getLayoutId(): Int {
        return R.layout.player_control_gesture
    }

    override fun initViews() {
        hide()
        mController = findViewById(R.id.gesture_present)
        mPresentIcon = findViewById<View>(R.id.gesture_present_icon) as ImageView
        mPresentText = findViewById<View>(R.id.gesture_present_text) as TextView
        mPresentProgress = findViewById<View>(R.id.gesture_present_progress) as ProgressBar
    }

    override fun onStartSlide() {
        //请求其它控制器处于不可见状态
        hideAllController(true)
        show()
        alpha = 1.0f
    }

    override fun onStopSlide() {
        animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    hide()
                }
            })
            .start()
    }

    /**
     * 播放进度调节
     * @param slidePosition 滑动进度
     * @param currentPosition 当前播放进度
     * @param duration 视频总长度
     */
    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
        if (null != mPresentProgress) mPresentProgress!!.visibility = GONE
        if (null != mPresentText) mPresentText!!.visibility = VISIBLE
        if (null != mPresentIcon) mPresentIcon!!.setImageResource(if (slidePosition > currentPosition) R.mipmap.ic_player_gesture_next else R.mipmap.ic_player_gesture_last)
        if (null != mPresentText) mPresentText!!.text =
            String.format("%s/%s", PlayerUtils.stringForAudioTime(slidePosition.toLong()), PlayerUtils.stringForAudioTime(duration.toLong()))
    }

    /**
     * 屏幕亮度调节
     * @param percent 亮度百分比
     */
    override fun onBrightnessChange(percent: Int) {
        if (null != mPresentText) mPresentText!!.visibility = GONE
        if (null != mPresentIcon) mPresentIcon!!.setImageResource(R.mipmap.ic_player_brightness)
        if (null != mPresentProgress) {
            mPresentProgress!!.visibility = VISIBLE
            mPresentProgress!!.progress = percent
        }
    }

    /**
     * 声音调节
     * @param percent 音量百分比
     */
    override fun onVolumeChange(percent: Int) {
        if (null != mPresentText) mPresentText!!.visibility = GONE
        if (null != mPresentIcon) mPresentIcon!!.setImageResource(if (0 == percent) R.mipmap.ic_player_sound_off else R.mipmap.ic_player_sound)
        if (null != mPresentProgress) {
            mPresentProgress!!.visibility = VISIBLE
            mPresentProgress!!.progress = percent
        }
    }

    override fun onPlayerState(state: PlayerState, message: String) {}

    override fun onPlayerScene(playerScene: Int) {
    }

    override fun onOrientation(direction: Int) {
        if (IMediaPlayer.ORIENTATION_LANDSCAPE == direction) {
            enterLandscape()
        } else {
            enterPortrait()
        }
    }

    /**
     * 进入竖屏模式 默认竖屏模式
     */
    private fun enterPortrait() {
        if (null != mController) {
            mController!!.layoutParams.width = PlayerUtils.dpToPxInt(146f)
            mController!!.layoutParams.height = PlayerUtils.dpToPxInt(79f)
            mController!!.setBackgroundResource(R.drawable.player_gesture_content_portrait_bg)
        }
        if (null != mPresentProgress) {
            val layoutParams = mPresentProgress!!.layoutParams as LinearLayout.LayoutParams
            val toPxInt10 = PlayerUtils.dpToPxInt(12f)
            val toPxInt16 = PlayerUtils.dpToPxInt(16f)
            layoutParams.setMargins(toPxInt10, toPxInt16, toPxInt10, 0)
        }
        if (null != mPresentText) mPresentText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, PlayerUtils.dpToPxInt(14f).toFloat())
    }

    /**
     * 进入横屏模式
     */
    private fun enterLandscape() {
        if (null != mController) {
            mController!!.layoutParams.width = PlayerUtils.dpToPxInt(168f)
            mController!!.layoutParams.height = PlayerUtils.dpToPxInt(99f)
            mController!!.setBackgroundResource(R.drawable.player_gesture_content_bg)
        }
        if (null != mPresentProgress) {
            val layoutParams = mPresentProgress!!.layoutParams as LinearLayout.LayoutParams
            val toPxInt12 = PlayerUtils.dpToPxInt(16f)
            val toPxInt20 = PlayerUtils.dpToPxInt(20f)
            layoutParams.setMargins(toPxInt12, toPxInt20, toPxInt12, 0)
        }
        if (null != mPresentText) mPresentText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, PlayerUtils.dpToPxInt(15f).toFloat())
    }
}