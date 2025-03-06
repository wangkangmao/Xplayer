package com.wangkm.widget.controls

import android.content.Context
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.wangkm.widget.R
import com.wangkm.widget.controls.ControlStatusView.OnStatusListener
import com.wangkm.xplayer.base.BaseControlWidget
import com.wangkm.xplayer.manager.IVideoManager
import com.wangkm.xplayer.manager.IWindowManager
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.AnimationUtils

/**
 * created by wangkm
 * Desc:UI控制器-窗口交互控制器,由于窗口有拖拽手势原因，窗口交互时独立的一套UI。其它例如加载中、网络提示、播放失败等会和这个组件排斥。
 */
class ControWindowView(context: Context?) : BaseControlWidget(context), View.OnClickListener {
    private var mController: View? = null
    private var mLoadingView: ProgressBar? = null
    private var mProgressBar: ProgressBar? = null //加载中

    //播放按钮,控制器,重新播放
    private var mControllerPlay: View? = null
    private var mControllerReplay: View? = null
    private var mPlayIcon: ImageView? = null //右下角的迷你播放状态按钮

    //失败\移动网络播放提示
    private var mControllerStatus: ControlStatusView? = null

    override fun getLayoutId(): Int {
        return R.layout.player_control_window
    }

    override fun initViews() {
        hide()
        mController = findViewById(R.id.window_controller)
        findViewById<View>(R.id.window_fullscreen).setOnClickListener(this)
        val rootView = findViewById<FrameLayout>(R.id.window_root_view)
        rootView.setOnClickListener(this)
        mControllerPlay = findViewById(R.id.window_play)
        mControllerPlay?.setOnClickListener(this)
        mLoadingView = findViewById(R.id.window_loading)
        mProgressBar = findViewById(R.id.window_progress)
        mControllerStatus = ControlStatusView(context)
        rootView.addView(mControllerStatus)
        mControllerStatus!!.visibility = GONE
        mControllerStatus!!.setSceneType(1) //窗口适用的UI样式
        mControllerStatus!!.setOnStatusListener(object : OnStatusListener {
            override fun onEvent(event: Int) {
                if (null != mControlWrapper) {
                    if (ControlStatusView.SCENE_MOBILE == event) { //移动网络播放
                        IVideoManager.setMobileNetwork(true)
                        if (null != mControlWrapper) mControlWrapper.togglePlay()
                    } else if (ControlStatusView.SCENE_ERROR == event) { //播放失败
                        if (null != mControlWrapper) mControlWrapper.togglePlay()
                    }
                }
            }
        })
        mControllerReplay = findViewById(R.id.window_replay)
        mControllerReplay?.setOnClickListener(this)
        mPlayIcon = findViewById(R.id.window_start)
        mPlayIcon?.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.window_fullscreen) {
            IWindowManager.instance!!.onClickWindow()
        } else if (id == R.id.window_start || id == R.id.window_replay || id == R.id.window_play) {
            if (null != mControlWrapper) mControlWrapper.togglePlay() //回调给播放器
        } else if (id == R.id.window_root_view) {
            toggleController()
        }
    }

    /**
     * @param isAnimation 控制器显示,是否开启动画
     */
    override fun showControl(isAnimation: Boolean) {
        if (isVisible && null != mController) {
            if (mController!!.visibility != VISIBLE) {
                if (isAnimation) {
                    AnimationUtils.instance!!.startAlphaAnimatioFrom(mController, animationDuration, false, null)
                } else {
                    mController!!.visibility = VISIBLE
                }
            }
        }
    }

    /**
     * @param isAnimation 控制器隐藏,是否开启动画
     */
    override fun hideControl(isAnimation: Boolean) {
        if (isVisible && null != mController) {
            if (mController!!.visibility != GONE) {
                if (isAnimation) {
                    AnimationUtils.instance!!.startAlphaAnimatioTo(mController, animationDuration, false, object :
                        AnimationUtils.OnAnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {
                            mController!!.visibility = GONE
                        }
                    })
                } else {
                    mController!!.visibility = GONE
                }
            }
        }
    }

    override fun onPlayerState(state: PlayerState, message: String) {
        when (state) {
            PlayerState.STATE_RESET, PlayerState.STATE_STOP -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_play)
                onReset()
            }

            PlayerState.STATE_PREPARE, PlayerState.STATE_BUFFER -> changedUIState(VISIBLE, GONE, GONE, GONE, 0, null)
            PlayerState.STATE_START -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_pause)
                showControl(false)
                changedUIState(GONE, GONE, GONE, GONE, 0, null)
            }

            PlayerState.STATE_PLAY, PlayerState.STATE_ON_PLAY -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_pause)
                changedUIState(GONE, GONE, GONE, GONE, 0, null)
            }

            PlayerState.STATE_PAUSE, PlayerState.STATE_ON_PAUSE -> {
                stopDelayedRunnable()
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_play)
                changedUIState(GONE, VISIBLE, GONE, GONE, 0, null)
            }

            PlayerState.STATE_COMPLETION -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_play)
                if (null != mProgressBar) mProgressBar!!.progress = 0
                changedUIState(GONE, GONE, VISIBLE, GONE, 0, null)
            }

            PlayerState.STATE_MOBILE -> changedUIState(GONE, GONE, GONE, VISIBLE, ControlStatusView.SCENE_MOBILE, null)
            PlayerState.STATE_ERROR -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_play)
                changedUIState(GONE, GONE, GONE, VISIBLE, ControlStatusView.SCENE_ERROR, null)
            }

            PlayerState.STATE_DESTROY -> onDestroy()
        }
    }

    override fun onOrientation(direction: Int) {}

    override fun onPlayerScene(playerScene: Int) {
        //仅当窗口模式时启用窗口控制器
        if (isWindowScene) {
            show()
            findViewById<View>(R.id.window_fullscreen).visibility =
                if (isWindowGlobalScene(playerScene)) VISIBLE else GONE
        } else {
            hide()
        }
    }

    override fun onProgress(currentDurtion: Long, totalDurtion: Long) {
        if (null != mProgressBar) {
            if (mProgressBar!!.max == 0) {
                mProgressBar!!.max = (if (isPreViewScene) mControlWrapper.preViewTotalDuration else totalDurtion).toInt()
            }
            mProgressBar!!.progress = currentDurtion.toInt()
        }
    }

    override fun onBuffer(percent: Int) {
        if (null != mProgressBar && mProgressBar!!.secondaryProgress != percent) {
            mProgressBar!!.secondaryProgress = percent
        }
    }

    /**
     * 改变UI状态
     * @param loadingView 加载状态
     * @param playerBtn 播放按钮状态
     * @param replayBtn 重新播放
     * @param statuView 移动网络播放\试看结束\播放失败 状态
     * @param scene 状态场景类型,提供给回调判断
     * @param errorMessage 当播放错误或scene==SCENE_ERROR时不为空
     */
    private fun changedUIState(loadingView: Int, playerBtn: Int, replayBtn: Int, statuView: Int, scene: Int, errorMessage: String?) {
        if (null != mLoadingView) mLoadingView!!.visibility = loadingView
        if (null != mControllerPlay) mControllerPlay!!.visibility = playerBtn
        if (null != mControllerReplay) mControllerReplay!!.visibility = replayBtn
        if (null != mControllerStatus) {
            mControllerStatus!!.visibility = statuView
            if (scene > 0) mControllerStatus!!.setScene(scene, errorMessage) //仅当需要处理状态场景时才更新交互UI
        }
    }

    /**
     * 显示\隐藏控制器
     */
    private fun toggleController() {
        stopDelayedRunnable()
        if (null == mController) return
        if (mController!!.visibility == VISIBLE) {
            hideControl(true)
        } else {
            showControl(true)
            startDelayedRunnable()
        }
    }

    /**
     * 根据消息通道结启动延时任务
     */
    /**
     * 结启动延时任务
     */
    private fun startDelayedRunnable(msg: Int = MESSAGE_HIDE_CONTROLLER) {
        if (null != mExHandel) {
            stopDelayedRunnable()
            val message = mExHandel.obtainMessage()
            message.what = msg
            mExHandel.sendMessageDelayed(message, DELAYED_INVISIBLE.toLong())
        }
    }

    /**
     * 根据消息通道取消延时任务
     * @param msg
     */
    /**
     * 结束延时任务
     */
    private fun stopDelayedRunnable(msg: Int = 0) {
        if (null != mExHandel) {
            if (0 == msg) {
                mExHandel.removeCallbacksAndMessages(null)
            } else {
                mExHandel.removeMessages(msg)
            }
        }
    }

    /**
     * 使用这个Handel替代getHandel(),避免多播放器同时工作的相互影响
     */
    private val mExHandel: BaseHandel? = object : BaseHandel(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (null != msg && MESSAGE_HIDE_CONTROLLER == msg.what) {
                hideControl(true)
            }
        }
    }

    /**
     * 重置内部状态
     */
    private fun reset() {
        stopDelayedRunnable()
        if (null != mProgressBar) {
            mProgressBar!!.progress = 0
            mProgressBar!!.secondaryProgress = 0
            mProgressBar!!.max = 0
        }
        mExHandel?.removeCallbacksAndMessages(null)
        if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_window_play)
    }

    override fun onReset() {
        reset()
        changedUIState(GONE, VISIBLE, GONE, GONE, 0, null)
    }

    override fun onDestroy() {
        reset()
        changedUIState(GONE, VISIBLE, GONE, GONE, 0, null)
    }

    companion object {
        private const val MESSAGE_HIDE_CONTROLLER = 20 //隐藏控制器
        private const val DELAYED_INVISIBLE = 3000 //延时隐藏锁时长
    }
}