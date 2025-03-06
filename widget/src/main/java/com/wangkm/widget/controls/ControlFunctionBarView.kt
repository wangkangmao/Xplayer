package com.wangkm.widget.controls

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.wangkm.widget.R
import com.wangkm.xplayer.base.BaseControlWidget
import com.wangkm.xplayer.controller.ControlWrapper
import com.wangkm.xplayer.media.IMediaPlayer
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.AnimationUtils
import com.wangkm.xplayer.utils.PlayerUtils

/**
 * created by wangkm
 * Desc:UI控制器-底部功能交互控制
 * 1、自定义seekbar相关的控制器需要实现[.isSeekBarShowing]方法，返回显示状态给Controller判断控制器是否正在显示中
 * 2、当单击BaseController空白区域时控制器需要处理显示\隐藏逻辑的情况下需要复写[.showControl]和[.hideControl]方法
 * 3、这个seekBar进度条组件还维护了底部的ProgressBar，SDK默认的UI交互是：当播放器处于列表模式时不显示，其它情况都显示
 */
class ControlFunctionBarView(context: Context?) : BaseControlWidget(context), View.OnClickListener {
    private var mController: View? = null //控制器
    private var mSeekBar: SeekBar? = null //seek调节控制器
    private var mProgressBar: ProgressBar? = null //底部进度条
    private var mCurrentDuration: TextView? = null
    private var mTotalDuration: TextView? = null //当前播放位置时间\总时间
    private var mPlayIcon: ImageView? = null //左下角的迷你播放状态按钮

    //用户手指是否持续拖动中
    private var isTouchSeekBar = false

    override fun getLayoutId(): Int {
        return R.layout.player_control_functionbar
    }

    override fun initViews() {
        hide()
        mPlayIcon = findViewById(R.id.controller_start)
        mSeekBar = findViewById(R.id.controller_seek_bar)
        mController = findViewById(R.id.controller_controller)
        mCurrentDuration = findViewById(R.id.controller_current_duration)
        mTotalDuration = findViewById(R.id.controller_total_duration)
        mProgressBar = findViewById(R.id.controller_bottom_progress)
        mPlayIcon?.setOnClickListener(this)
        findViewById<View>(R.id.controller_btn_mute).setOnClickListener(this)
        findViewById<View>(R.id.controller_btn_fullscreen).setOnClickListener(this)
        //seekBar监听
        mSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            /**
             * 用户持续拖动进度条,视频总长为虚拟时长时，用户不得滑动阈值超过限制
             * @param seekBar
             * @param progress
             * @param fromUser
             */
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                ILogger.d(TAG,"onProgressChanged-->progress:"+progress+",fromUser:"+fromUser+getOrientationStr());
                //视频虚拟总长度
                if (null != mCurrentDuration) mCurrentDuration!!.text = PlayerUtils.stringForAudioTime(progress.toLong())
                if (null != mProgressBar) mProgressBar!!.progress = progress
            }

            /**
             * 获得焦点-按住了
             * @param seekBar
             */
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isTouchSeekBar = true
                mControlWrapper.stopDelayedRunnable() //取消定时隐藏任务
            }

            /**
             * 失去焦点-松手了
             * @param seekBar
             */
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isTouchSeekBar = false
                mControlWrapper.startDelayedRunnable() //开启定时隐藏任务
                //当controller_deblocking设置了点击时间，试看结束的拦截都无效
//                ILogger.d(TAG,"onStopTrackingTouch-->,isCompletion:"+isCompletion+",preViewTotalTime:"+mPreViewTotalTime);
                if (null != mControlWrapper) {
                    if (mControlWrapper.isCompletion && mControlWrapper.preViewTotalDuration > 0) { //拦截是看结束,让用户解锁
                        if (null != mControlWrapper) mControlWrapper.onCompletion()
                        return
                    }
                    val seekBarProgress = seekBar.progress
                    //                    ILogger.d(TAG,"onStopTrackingTouch-->seekBarProgress:"+seekBarProgress+",ViewTotalTime:"+ mPreViewTotalTime +",duration:"+ mVideoPlayerControl.getDurtion()+getOrientationStr());
                    if (mControlWrapper.preViewTotalDuration > 0) { //跳转至某处,如果滑动的时长超过真实的试看时长,则直接播放完成需要解锁
                        val durtion = mControlWrapper.duration
                        if (0 == seekBarProgress) { //重新从头开始播放
                            //改变UI为缓冲状态
                            mControlWrapper.onPlayerState(PlayerState.STATE_BUFFER, "seek")
                            mControlWrapper.seekTo(0)
                        } else {
                            if (seekBarProgress >= durtion) { //试看片段,需要解锁
                                mControlWrapper.onCompletion()
                            } else {
                                //改变UI为缓冲状态
                                mControlWrapper.onPlayerState(PlayerState.STATE_BUFFER, "seek")
                                mControlWrapper.seekTo(seekBarProgress.toLong()) //试看片段内,允许跳转
                            }
                        }
                    } else {
                        //改变UI为缓冲状态
                        mControlWrapper.onPlayerState(PlayerState.STATE_BUFFER, "seek")
                        mControlWrapper.seekTo(seekBarProgress.toLong()) //真实时长,允许跳转
                    }
                }
            }
        })
    }

    override fun attachControlWrapper(controlWrapper: ControlWrapper) {
        super.attachControlWrapper(controlWrapper)
        if (null != mTotalDuration) mTotalDuration!!.text = PlayerUtils.stringForAudioTime(mControlWrapper.preViewTotalDuration)
    }

    override fun onCreate() {
        super.onCreate()
        updateMute()
    }

    override fun onClick(view: View) {
        if (view.id == R.id.controller_start) {
            reStartDelayedRunnable()
            togglePlay()
        } else if (view.id == R.id.controller_btn_mute) {
            reStartDelayedRunnable()
            toggleMute()
        } else if (view.id == R.id.controller_btn_fullscreen) {
            toggleFullScreen()
            reStartDelayedRunnable()
        }
    }

    override fun isSeekBarShowing(): Boolean {
        return null != mController && mController!!.visibility == VISIBLE
    }

    /**
     * @param isAnimation 控制器显示,是否开启动画
     */
    override fun showControl(isAnimation: Boolean) {
        if (null != mController) {
            if (mController!!.visibility != VISIBLE) {
                if (null != mProgressBar) mProgressBar!!.visibility = GONE
                if (isAnimation) {
                    AnimationUtils.instance!!.startTranslateBottomToLocat(mController, animationDuration, null)
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
        if (null != mController) {
            if (mController!!.visibility != GONE) {
                if (isAnimation) {
                    AnimationUtils.instance!!.startTranslateLocatToBottom(mController, animationDuration, object :
                        AnimationUtils.OnAnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {
                            mController!!.visibility = GONE
                            AnimationUtils.instance!!.startAlphaAnimatioFrom(mProgressBar, animationDuration, false, null)
                            //                            if(null!=mProgressBar) mProgressBar.setVisibility(View.VISIBLE);
                        }
                    })
                } else {
                    mController!!.visibility = GONE
                    if (null != mProgressBar) mProgressBar!!.visibility = VISIBLE
                }
            }
        }
    }

    override fun onPlayerState(state: PlayerState, message: String) {
        when (state) {
            PlayerState.STATE_RESET, PlayerState.STATE_STOP -> onReset()
            PlayerState.STATE_PREPARE -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_play)
                hide()
            }

            PlayerState.STATE_BUFFER, PlayerState.STATE_PAUSE, PlayerState.STATE_ON_PAUSE -> if (null != mPlayIcon) mPlayIcon!!.setImageResource(
                R.mipmap.ic_player_play
            )

            PlayerState.STATE_START -> {
                //渲染第一帧时，竖屏和横屏都显示
                if (isNoimalScene) {
                    show()
                }
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_pause)
                showControl(true)
            }

            PlayerState.STATE_PLAY, PlayerState.STATE_ON_PLAY -> if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_pause)
            PlayerState.STATE_COMPLETION -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_play)
                resetProgressBar()
            }

            PlayerState.STATE_MOBILE -> {}
            PlayerState.STATE_ERROR -> {
                if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_play)
                onReset()
            }

            PlayerState.STATE_DESTROY -> onDestroy()
        }
    }

    override fun onOrientation(direction: Int) {
        if (null == mController) return
        if (IMediaPlayer.ORIENTATION_LANDSCAPE == direction) {
            val margin22 = PlayerUtils.dpToPxInt(22f)
            //横屏下处理标题栏和控制栏的左右两侧缩放
            mController!!.setPadding(margin22, 0, margin22, 0)
            show()
            if (isPlaying) reStartDelayedRunnable()
        } else {
            val margin5 = PlayerUtils.dpToPxInt(5f)
            mController!!.setPadding(margin5, 0, margin5, 0)
            if (isNoimalScene) {
                show()
            } else {
                //非常规场景不处理
                hide()
            }
        }
    }

    override fun onPlayerScene(playerScene: Int) {
        findViewById<View>(R.id.controller_progress).visibility = if (isListPlayerScene) GONE else VISIBLE
        //当播放器和控制器在专场播放、场景发生变化时，仅当在常规模式下并且正在播放才显示控制器
        if (isNoimalScene) {
            show()
            if (isPlaying) {
                showControl(false)
                reStartDelayedRunnable()
            }
        } else {
            hide()
        }
    }

    override fun onProgress(currentDurtion: Long, totalDurtion: Long) {
        if (null != mSeekBar && null != mControlWrapper) {
            if (null != mProgressBar && mProgressBar!!.max == 0) {
                mProgressBar!!.max =
                    (if (mControlWrapper.preViewTotalDuration > 0) mControlWrapper.preViewTotalDuration else totalDurtion).toInt()
            }
            if (null != mSeekBar) {
                if (mSeekBar!!.max <= 0) { //总进度总时长只更新一次,如果是虚拟的总时长,则在setViewTotalDuration中更新总时长
                    mSeekBar!!.max =
                        (if (mControlWrapper.preViewTotalDuration > 0) mControlWrapper.preViewTotalDuration else totalDurtion).toInt()
                    if (null != mTotalDuration) mTotalDuration!!.text =
                        PlayerUtils.stringForAudioTime(if (mControlWrapper.preViewTotalDuration > 0) mControlWrapper.preViewTotalDuration else totalDurtion)
                }
                if (!isTouchSeekBar) mSeekBar!!.progress = currentDurtion.toInt()
            }
        }
    }

    override fun onBuffer(bufferPercent: Int) {
        if (null != mControlWrapper) {
            val percent = PlayerUtils.formatBufferPercent(bufferPercent, mControlWrapper.duration)
            if (null != mSeekBar && mSeekBar!!.secondaryProgress != percent) {
                mSeekBar!!.secondaryProgress = percent
            }
            if (null != mProgressBar && mProgressBar!!.secondaryProgress != percent) {
                mProgressBar!!.secondaryProgress = percent
            }
        }
    }

    override fun onMute(isMute: Boolean) {
        val muteImage = findViewById<View>(R.id.controller_btn_mute) as ImageView
        muteImage.setImageResource(if (isMute) R.mipmap.ic_player_mute_true else R.mipmap.ic_player_mute_false)
    }

    /**
     * 更新静音状态
     */
    private fun updateMute() {
        if (null != mControlWrapper) {
            val soundMute = mControlWrapper.isSoundMute
            val muteImge = findViewById<View>(R.id.controller_btn_mute) as ImageView
            muteImge.setImageResource(if (soundMute) R.mipmap.ic_player_mute_true else R.mipmap.ic_player_mute_false)
        }
    }

    /**
     * 是否显示静音按钮
     * @param showSound 是否显示静音按钮,true:显示 false:隐藏
     */
    fun showSoundMute(showSound: Boolean) {
        val muteImage = findViewById<View>(R.id.controller_btn_mute) as ImageView
        muteImage.visibility = if (showSound) VISIBLE else GONE
    }

    /**
     * 是否显示静音按钮
     * @param showSound 是否显示静音按钮,true:显示 false:隐藏
     * @param soundMute 是否静音,true:静音 false:系统原声
     */
    fun showSoundMute(showSound: Boolean, soundMute: Boolean) {
        val muteImage = findViewById<View>(R.id.controller_btn_mute) as ImageView
        muteImage.visibility = if (showSound) VISIBLE else GONE
        if (null != mControlWrapper) mControlWrapper.setSoundMute(soundMute) //UI状态将在onMute回调中处理
    }

    /**
     * 是否启用全屏按钮播放功能
     * @param enable true:启用 false:禁止 默认是开启的
     */
    fun enableFullScreen(enable: Boolean) {
        findViewById<View>(R.id.controller_btn_fullscreen).visibility =
            if (enable) VISIBLE else GONE
    }

    private fun resetProgressBar() {
        if (null != mProgressBar) {
            mProgressBar!!.progress = 0
            mProgressBar!!.secondaryProgress = 0
            mProgressBar!!.max = 0
        }
    }

    override fun onReset() {
        if (null != mSeekBar) {
            mSeekBar!!.progress = 0
            mSeekBar!!.secondaryProgress = 0
            mSeekBar!!.max = 0
        }
        resetProgressBar()
        hideControl(false)
        if (null != mTotalDuration) mTotalDuration!!.text = PlayerUtils.stringForAudioTime(0)
        if (null != mCurrentDuration) mCurrentDuration!!.text = PlayerUtils.stringForAudioTime(0)
        if (null != mPlayIcon) mPlayIcon!!.setImageResource(R.mipmap.ic_player_play)
    }
}