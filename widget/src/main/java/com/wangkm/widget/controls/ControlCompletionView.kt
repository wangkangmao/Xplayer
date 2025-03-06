package com.wangkm.widget.controls

import android.content.Context
import com.wangkm.widget.R
import com.wangkm.xplayer.base.BaseControlWidget
import com.wangkm.xplayer.model.PlayerState

/**
 * created by wangkm
 * Desc:UI控制器-播放完成
 */
class ControlCompletionView(context: Context?) : BaseControlWidget(context) {
    override fun getLayoutId(): Int {
        return R.layout.player_control_completion
    }

    override fun initViews() {
        hide()
        setOnClickListener { if (null != mControlWrapper) mControlWrapper.togglePlay() }
    }

    override fun onPlayerState(state: PlayerState, message: String) {
        when (state) {
            PlayerState.STATE_COMPLETION -> if (!isWindowScene && !isPreViewScene) { //窗口播放模式/试看模式不显示
                show()
            }

            else -> hide()
        }
    }

    override fun onOrientation(direction: Int) {}
}