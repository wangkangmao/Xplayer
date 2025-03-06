package com.wangkm.widget.controls

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.wangkm.widget.R
import com.wangkm.xplayer.base.BaseControlWidget
import com.wangkm.xplayer.media.IMediaPlayer
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.PlayerUtils

/**
 * created by wangkm
 * Desc:UI控制器-列表播放器场景定制UI
 */
class ControlListView(context: Context?) : BaseControlWidget(context), View.OnClickListener {
    private var mSurplusDuration: TextView? = null //剩余时间

    override fun getLayoutId(): Int {
        return R.layout.player_list_view
    }

    override fun initViews() {
        hide()
        mSurplusDuration = findViewById(R.id.controller_surplus_duration)
        findViewById<View>(R.id.controller_list_mute).setOnClickListener(this)
        findViewById<View>(R.id.controller_list_fullscreen).setOnClickListener(this)
        updateMute()
    }

    /**
     * 更新静音状态
     */
    private fun updateMute() {
        if (null != mControlWrapper) {
            val soundMute = mControlWrapper.isSoundMute
            val muteImge = findViewById<View>(R.id.controller_list_mute) as ImageView
            muteImge.setImageResource(if (soundMute) R.mipmap.ic_player_mute_true else R.mipmap.ic_player_mute_false)
        }
    }

    override fun onPlayerState(state: PlayerState, message: String) {
        when (state) {
            PlayerState.STATE_RESET, PlayerState.STATE_STOP, PlayerState.STATE_ERROR, PlayerState.STATE_COMPLETION -> onReset()
            PlayerState.STATE_PREPARE -> hide()
            PlayerState.STATE_START -> if (isListPlayerScene) {
                show()
            }
            else -> {}
        }
    }

    override fun onOrientation(direction: Int) {
        if (IMediaPlayer.ORIENTATION_LANDSCAPE == direction) {
            hide()
        } else {
            if (isListPlayerScene) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun onPlayerScene(playerScene: Int) {
        if (isOrientationPortrait) {
            if (isListPlayerScene(playerScene)) {
                if (isPlaying) {
                    show()
                }
            } else {
                hide()
            }
        } else {
            hide()
        }
    }

    override fun onProgress(currentDurtion: Long, totalDurtion: Long) {
        try {
            if (null != mSurplusDuration) mSurplusDuration!!.text = PlayerUtils.stringForAudioTime(totalDurtion - currentDurtion)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onMute(isMute: Boolean) {
        val muteImage = findViewById<View>(R.id.controller_list_mute) as ImageView
        muteImage.setImageResource(if (isMute) R.mipmap.ic_player_mute_true else R.mipmap.ic_player_mute_false)
    }

    override fun onClick(view: View) {
        if (null != mControlWrapper) {
            if (view.id == R.id.controller_list_mute) {
                mControlWrapper.toggleMute()
            } else if (view.id == R.id.controller_list_fullscreen) {
                mControlWrapper.toggleFullScreen()
            }
        }
    }

    /**
     * 是否显示静音按钮
     * @param showSound 是否显示静音按钮,true:显示 false:隐藏
     * @param soundMute 是否静音,true:静音 false:系统原声
     */
    fun showSoundMute(showSound: Boolean, soundMute: Boolean) {
        val muteImage = findViewById<View>(R.id.controller_list_mute) as ImageView
        muteImage.visibility = if (showSound) VISIBLE else GONE
        if (null != mControlWrapper) mControlWrapper.setSoundMute(soundMute) //UI状态将在onMute回调中处理
    }

    override fun onReset() {
        if (null != mSurplusDuration) mSurplusDuration!!.text = PlayerUtils.stringForAudioTime(0)
    }
}