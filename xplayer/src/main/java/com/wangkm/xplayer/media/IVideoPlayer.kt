package com.wangkm.xplayer.media

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.FrameLayout
import ccom.wangkm.xplayer.R
import com.wangkm.xplayer.base.AbstractMediaPlayer
import com.wangkm.xplayer.base.BasePlayer
import com.wangkm.xplayer.interfaces.IBasePlayer
import com.wangkm.xplayer.interfaces.IRenderView
import com.wangkm.xplayer.listener.OnMediaEventListener
import com.wangkm.xplayer.manager.IVideoManager
import com.wangkm.xplayer.media.core.MediaPlayerFactory
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.AudioFocus
import com.wangkm.xplayer.utils.AudioFocus.OnAudioFocusListener
import com.wangkm.xplayer.utils.ILogger
import com.wangkm.xplayer.utils.PlayerUtils
import com.wangkm.xplayer.utils.ThreadPool
import com.wangkm.xplayer.widget.view.MediaTextureView
import java.util.Timer
import java.util.TimerTask

/**
 * created by wangkm
 * Desc:视频解码\播放\进度更新\特性功能等处理
 * 1、可支持用户自定义视频解码器，内部默认使用系统的MediaPlayer解码器。详细使用请参考BasePlayer文档描述
 */
class IVideoPlayer : OnMediaEventListener, OnAudioFocusListener {
    //播放器容器与播放器管理者绑定关系的监听器，必须实现监听
    private var mBasePlayer: IBasePlayer? = null

    //播放器画面渲染核心
    private var mMediaPlayer: AbstractMediaPlayer? = null //视频格式文件解码器
    private var mRenderView: IRenderView? = null //画面渲染
    private var mAudioFocusManager: AudioFocus? = null //多媒体焦点监听,失去焦点暂停播放

    //内部播放器状态,初始为默认/重置状态
    var playerState: PlayerState = PlayerState.STATE_RESET
        private set

    //是否循环播放/是否静音/是否镜像
    private var mLoop = false

    /**
     * 返回播放器是否启用了静音模式
     * @return true:静音 false:系统原生
     */
    var isSoundMute: Boolean = false
        private set
    private var mMirrors = false

    //左右声道设置
    private var mLeftVolume = 1.0f
    private var mRightVolume = 1.0f

    //裁剪缩放模式，默认为原始大小，定宽等高，可能高度会留有黑边
    private var mZoomMode = IMediaPlayer.MODE_ZOOM_TO_FIT

    //远程资源地址
    private var mDataSource: String? = null
    private var mAssetsSource: AssetFileDescriptor? = null //Assetss资产目录下的文件地址

    //进度计时器
    private var mPlayerTimerTask: PlayerTimerTask? = null
    private var mTimer: Timer? = null

    //播放进度回调间隔时间,默认的播放器进度间隔1秒回调
    private val DEFAULT_CALLBACK_TIME: Long = 1000
    private var mCallBackSpaceMilliss = DEFAULT_CALLBACK_TIME

    //需要跳转的进度位置
    private var mSeekDuration: Long = 0

    //视频宽、高
    var mVideoWidth = 0
    var mVideoHeight = 0

    //链接视频源超时时长\读取视频流超时时长
    private var mPrepareTimeout = 10 * 1000
    private var mReadTimeout = 15 * 1000

    //链接视频文件发生错误的重试次数
    private var mReCatenationCount = 3
    private var mReCount = 0 //重试的次数

    /**
     * 播放状态,回调给播放控制器宿主
     * @param playerState 播放状态
     * @param message 描述信息
     */
    private fun onPlayerState(playerState: PlayerState, message: String) {
//        ILogger.d(TAG,"onPlayerState-->playerState:"+playerState+",message:"+message);
        if (null != mBasePlayer) mBasePlayer!!.onPlayerState(playerState, message)
    }

    /**
     * 实时播放进度条,回调给播放控制器宿主
     * @param currentPosition 当前播放时长进度 毫秒
     * @param duration 总时长 毫秒
     */
    private fun onProgress(currentPosition: Long, duration: Long) {
        if (null != mBasePlayer) mBasePlayer!!.onProgress(currentPosition, duration)
    }

    //===========================================视频播放逻辑=========================================
    /**
     * 实例化一个播放器解码器,如果宿主自定义解码器则使用宿主自定义解码器,否则使用内部默认解码器
     * @return 返回一个自定义的MediaPlayer
     */
    private fun newInstanceMediaPlayer(): AbstractMediaPlayer {
        var mediaPlayer: AbstractMediaPlayer?
        mediaPlayer = mBasePlayer!!.mediaPlayer
        if (null == mediaPlayer) {
            val context = mBasePlayer!!.videoPlayer.context
            mediaPlayer = MediaPlayerFactory.create().createPlayer(context)
        }
        return mediaPlayer!!
    }

    /**
     * 实例化一个播放器画面渲染器,如果宿主自定义渲染器则使用宿主自定义渲染器,否则使用内部默认渲染器
     * @param context 上下文
     * @return 返回一个自定义的VideoRenderView
     */
    private fun newInstanceRenderView(context: Context): IRenderView {
        var renderView: IRenderView?
        renderView = mBasePlayer!!.renderView
        if (null == renderView) {
            renderView = MediaTextureView(context)
        }
        renderView.attachMediaPlayer(mMediaPlayer)
        return renderView
    }

    /**
     * 创建播放器
     */
    private fun initMediaPlayer(): Boolean {
        if (null != mBasePlayer) {
            mMediaPlayer = newInstanceMediaPlayer()
            val videoPlayer = mBasePlayer!!.videoPlayer
            ILogger.d(TAG, getString(R.string.player_core_name, "解码器内核：") + mMediaPlayer!!.javaClass.simpleName)
            mMediaPlayer!!.setMediaEventListener(this)
            mMediaPlayer!!.setLooping(mLoop)
            if (isSoundMute) {
                mMediaPlayer!!.setVolume(0f, 0f)
            } else {
                mMediaPlayer!!.setVolume(mLeftVolume, mRightVolume)
            }
            //设置播放参数
            mMediaPlayer!!.setBufferTimeMax(2.0f)
            mMediaPlayer!!.setTimeout(mPrepareTimeout.toLong(), mReadTimeout.toLong())
            initTextureView(videoPlayer.context)
            attachedVideoView(videoPlayer)
            return true
        }
        return false
    }

    private fun initTextureView(context: Context?) {
        if (null == context) return
        mRenderView = newInstanceRenderView(context)
        ILogger.d(TAG, getString(R.string.player_render_name, "渲染器内核：") + mRenderView!!.javaClass.simpleName)
    }

    //释放解码器\移除画面组件
    private fun releaseTextureView() {
//        ILogger.d(TAG,"releaseTextureView");
        if (null != mMediaPlayer) {
            try {
                if (mMediaPlayer!!.isPlaying) {
                    mMediaPlayer!!.stop()
                }
                //                mMediaPlayer.reset();//别重置了,直接销毁
                mMediaPlayer!!.release() //这个方法有点卡顿,请解码器内部做好处理
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                if (null != mRenderView) {
                    PlayerUtils.removeViewFromParent(mRenderView!!.view)
                }
                releaseSurfaceTexture()
                mRenderView = null
                mMediaPlayer = null
            }
        }
    }

    //释放渲染组件
    private fun releaseSurfaceTexture() {
//        ILogger.d(TAG,"releaseSurfaceTexture");
        if (null != mRenderView) mRenderView!!.release()
    }

    private fun firstPlay() {
        playerState = PlayerState.STATE_START
        onPlayerState(playerState, getString(R.string.player_media_start, "首帧渲染"))
        startTimer()
        if (mSeekDuration > 0) {
            val seekDuration = mSeekDuration
            mSeekDuration = 0
            seekTo(seekDuration)
        }
        listenerAudioFocus()
    }

    override fun onPrepared(mp: IMediaPlayer?) {
        ILogger.d(TAG, "onPrepared-->seek:$mSeekDuration")
        mReCount = 0 //重置重试次数
        if (null != mMediaPlayer) {
            mp!!.start()
            firstPlay()
        } else {
            mSeekDuration = 0
            onError(null, 0, 0)
        }
    }

    override fun onBufferUpdate(mp: IMediaPlayer?, percent: Int) {
//        ILogger.d(TAG,"onBufferingUpdate-->percent:"+percent);
        if (null != mBasePlayer) mBasePlayer!!.onBuffer(percent)
    }

    override fun onSeekComplete(mp: IMediaPlayer?) {
        ILogger.d(TAG, "onSeekComplete,buffer:")
        mSeekDuration = 0
        startTimer()
        playerState = PlayerState.STATE_PLAY
        onPlayerState(playerState, getString(R.string.player_media_seek, "快进快退恢复播放"))
    }

    override fun onVideoSizeChanged(mp: IMediaPlayer?, width: Int, height: Int, sar_num: Int, sar_den: Int) {
        ILogger.d(TAG, "onVideoSizeChanged,width:$width,height:$height")
        this.mVideoWidth = width
        this.mVideoHeight = height
        if (null != mRenderView) {
            mRenderView!!.setVideoSize(width, height)
            mRenderView!!.setZoomMode(mZoomMode)
            mRenderView!!.setMirror(mMirrors)
            mRenderView!!.setSarSize(sar_num, sar_den)
        }
        if (null != mBasePlayer) mBasePlayer!!.onVideoSizeChanged(width, height)
    }

    /**
     * @param mp
     * @param what
     * @param extra
     * @return
     */
    override fun onInfo(mp: IMediaPlayer?, what: Int, extra: Int): Boolean {
//        ILogger.d(TAG,"onInfo-->what:"+what+",extra:"+extra);
        when (what) {
            IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {}
            IMediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                playerState = PlayerState.STATE_BUFFER
                onPlayerState(playerState, getString(R.string.player_media_buffer_start, "缓冲开始"))
            }

            IMediaPlayer.MEDIA_INFO_BUFFERING_END, IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH -> {
                playerState = PlayerState.STATE_PLAY
                onPlayerState(playerState, getString(R.string.player_media_buffer_end, "缓冲结束"))
            }

            IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> if (null != mRenderView) mRenderView!!.setDegree(extra)
        }
        return true
    }

    override fun onCompletion(mp: IMediaPlayer?) {
        ILogger.d(TAG, "onCompletion：$mLoop,mp:$mp")
        mSeekDuration = 0
        stopTimer()
        playerState = PlayerState.STATE_COMPLETION
        onPlayerState(playerState, getString(R.string.player_media_completion, "播放完成"))
    }

    override fun onError(mp: IMediaPlayer?, what: Int, extra: Int): Boolean {
        ILogger.e(TAG, "onError,what:$what,extra:$extra,reCount:$mReCount") //直播拉流会有-38的错误
        if (-38 == what) return true
        if (-10000 == what && mReCount < mReCatenationCount && null != mMediaPlayer) {
            reCatenation()
            return true
        }
        stopTimer()
        playerState = PlayerState.STATE_ERROR
        onPlayerState(playerState, getErrorMessage(what))
        return true
    }

    /**
     * 内部重试
     */
    private fun reCatenation() {
        mReCount += 1
        startPlayer(dataSource)
    }

    /**
     * 返回错误描述
     * @param what
     * @return
     */
    private fun getErrorMessage(what: Int): String {
        return when (what) {
            IMediaPlayer.MEDIA_ERROR_IO, IMediaPlayer.MEDIA_ERROR_MALFORMED, IMediaPlayer.MEDIA_ERROR_UNSUPPORTED, IMediaPlayer.MEDIA_ERROR_TIMED_OUT -> getString(
                R.string.player_media_error_timeout,
                "播放失败,播放链接超时"
            )

            IMediaPlayer.MEDIA_ERROR_UNSUPPORT_PROTOCOL, IMediaPlayer.MEDIA_ERROR_UNSUPPORT_VIDEO_CODEC -> getString(
                R.string.player_media_error_file_invalid,
                "播放失败,不支持的视频文件格式"
            )

            IMediaPlayer.MEDIA_ERROR_DNS_PARSE_FAILED, IMediaPlayer.MEDIA_ERROR_CREATE_SOCKET_FAILED, IMediaPlayer.MEDIA_ERROR_CONNECT_SERVER_FAILED, IMediaPlayer.MEDIA_ERROR_UNSUPPORT_AUDIO_CODEC -> getString(
                R.string.player_media_error_dns,
                "播放失败,链接DNS失败"
            )

            IMediaPlayer.MEDIA_ERROR_BAD_REQUEST, IMediaPlayer.MEDIA_ERROR_UNAUTHORIZED_CLIENT, IMediaPlayer.MEDIA_ERROR_ACCESSS_FORBIDDEN, IMediaPlayer.MEDIA_ERROR_TARGET_NOT_FOUND, IMediaPlayer.MEDIA_ERROR_FILE_NOT_FOUND, IMediaPlayer.MEDIA_ERROR_OTHER_ERROR_CODE, IMediaPlayer.MEDIA_ERROR_SERVER_EXCEPTION, IMediaPlayer.MEDIA_ERROR_INVALID_DATA, IMediaPlayer.MEDIA_ERROR_INVALID_URL -> getString(
                R.string.player_media_error_path_invalid,
                "播放失败,请检查视频文件地址有效性"
            )

            IMediaPlayer.MEDIA_ERROR_VIDEO_DECODE_FAILED, IMediaPlayer.MEDIA_ERROR_AUDIO_DECODE_FAILED -> getString(
                R.string.player_media_error_core,
                "视频解码失败"
            )

            else -> what.toString() + ""
        }
    }

    /**
     * 提供给宿主调用
     * @param dataSource
     */
    private fun startPlayer(dataSource: Any?) {
        //检查播放地址
        if (!checkedDataSource()) {
            ILogger.d(TAG, "startPlayer-->地址为空")
            playerState = PlayerState.STATE_ERROR
            onPlayerState(playerState, getString(R.string.player_media_error_path_empty, "播放地址为空,请检查!"))
            return
        }
        //检查是网络地址还是本地的资源视频地址
        val hasNet = PlayerUtils.hasNet(mDataSource.toString(), mAssetsSource)
        //检查网络链接状态
        if (hasNet && !PlayerUtils.isCheckNetwork) {
            ILogger.d(TAG, "startPlayer-->网络未连接")
            playerState = PlayerState.STATE_ERROR
            onPlayerState(playerState, getString(R.string.player_media_error_net, "网络未连接"))
            return
        }
        //检查移动流量网络下是否允许播放
        val mobileNetwork = PlayerUtils.mobileNetwork(IVideoManager.isMobileNetwork)
        if (hasNet && !mobileNetwork) {
            ILogger.d(TAG, "startPlayer-->移动网络下")
            playerState = PlayerState.STATE_MOBILE
            onPlayerState(playerState, getString(R.string.player_media_mobile, "移动网络播放"))
            return
        }
        val result = createPlayer()
        if (result) {
            playerState = PlayerState.STATE_PREPARE
            onPlayerState(playerState, getString(R.string.player_media_reday, "播放准备中"))
            try {
                if (dataSource is String) {
                    this.mDataSource = dataSource
                    mMediaPlayer!!.setDataSource(mDataSource)
                } else if (dataSource is AssetFileDescriptor) {
                    this.mAssetsSource = dataSource
                    mMediaPlayer!!.setDataSource(mAssetsSource)
                }
                ILogger.d(TAG, "startPlayer-->source:" + (if (null != mAssetsSource) mAssetsSource else mDataSource))
                mMediaPlayer!!.prepareAsync()
            } catch (e: Throwable) {
                e.printStackTrace()
                playerState = PlayerState.STATE_ERROR
                onPlayerState(playerState, getString(R.string.player_media_play_error, "播放失败,error:") + e.message)
            }
        } else {
            playerState = PlayerState.STATE_ERROR
            onPlayerState(playerState, "ViewGroup is avail")
        }
    }

    /**
     * 音频焦点监听
     */
    private fun listenerAudioFocus() {
        if (IVideoManager.isInterceptTAudioFocus) {
            if (null == mAudioFocusManager) mAudioFocusManager = AudioFocus()
            mAudioFocusManager!!.requestAudioFocus(this)
        }
    }

    /**
     * 检查播放地址的有效性
     * @return
     */
    private fun checkedDataSource(): Boolean {
        if (!TextUtils.isEmpty(mDataSource)) {
            return true
        }
        if (null != mAssetsSource) {
            return true
        }
        return false
    }

    /**
     * 创建一个播放器
     */
    private fun createPlayer(): Boolean {
        releaseTextureView()
        val result = initMediaPlayer()
        return result
    }

    /**
     * 从资源string中获取文字返回
     * @param id 源字符串ID
     * @param defaultStr 源字符串
     * @return
     */
    private fun getString(id: Int, defaultStr: String): String {
        val context = PlayerUtils.context
        if (null != context) {
            return context.resources.getString(id)
        }
        return defaultStr
    }

    /**
     * 播放进度、闹钟倒计时进度 计时器
     */
    private inner class PlayerTimerTask : TimerTask() {
        override fun run() {
            try {
                if (null != mMediaPlayer && isPlaying) {
                    ThreadPool.getInstance().runOnUIThread {
                        try {
                            onProgress(mMediaPlayer!!.currentPosition, mMediaPlayer!!.duration)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 开始计时任务
     */
    private fun startTimer() {
        if (null == mPlayerTimerTask) {
            mTimer = Timer()
            mPlayerTimerTask = PlayerTimerTask()
            mTimer!!.schedule(mPlayerTimerTask, 0, mCallBackSpaceMilliss)
        }
    }

    /**
     * 结束计时任务
     */
    private fun stopTimer() {
        if (null != mPlayerTimerTask) {
            mPlayerTimerTask!!.cancel()
            mPlayerTimerTask = null
        }
        if (null != mTimer) {
            mTimer!!.cancel()
            mTimer = null
        }
    }

    /**
     * 视频解码及渲染View转场
     * @param basePlayer
     */
    private fun attachedVideoView(basePlayer: BasePlayer?) {
        if (null != mRenderView && null != basePlayer) {
            val viewGroup = basePlayer.findViewById<ViewGroup>(R.id.player_surface)
            if (null != viewGroup) {
                PlayerUtils.removeViewFromParent(mRenderView!!.view)
                viewGroup.removeAllViews()
                viewGroup.addView(
                    mRenderView!!.view,
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                )
                mRenderView!!.requestDrawLayout()
            }
        }
    }

    private val dataSource: Any?
        get() {
            if (!TextUtils.isEmpty(mDataSource)) {
                return mDataSource
            }
            if (null != mAssetsSource) {
                return mAssetsSource
            }
            return null
        }

    /**
     * 获得音频焦点
     */
    override fun onFocusStart() {}

    /**
     * 失去音频焦点
     */
    override fun onFocusStop() {
        if (isPlaying) {
            onPause()
        }
    }

    /**
     * 注册播放器监听器 必须实现
     * @param iBasePlayer
     */
    fun attachPlayer(iBasePlayer: IBasePlayer?) {
        this.mBasePlayer = iBasePlayer
    }

    /**
     * 在开始播放前设置播放地址
     * @param dataSource raw或net地址
     */
    fun setDateSource(dataSource: String?) {
        this.mAssetsSource = null
        this.mDataSource = dataSource
    }

    /**
     * 在开始播放前设置播放地址
     * @param dataSource assets目录下的文件地址
     */
    fun setDateSource(dataSource: AssetFileDescriptor?) {
        this.mAssetsSource = dataSource
        this.mDataSource = null
    }

    /**
     * 设置是否循环播放
     * @param loop
     */
    fun setLoop(loop: Boolean) {
        this.mLoop = loop
        if (null != mMediaPlayer) {
            try {
                mMediaPlayer!!.setLooping(loop)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 设置是否静音播放
     * @param soundMute true:无声 false:跟随系统音量
     * @return 是否静音,true:无声 false:跟随系统音量
     */
    fun setSoundMute(soundMute: Boolean): Boolean {
        this.isSoundMute = soundMute
        if (null != mMediaPlayer) {
            try {
                mMediaPlayer!!.setVolume(if (isSoundMute) 0f else 1.0f, if (isSoundMute) 0f else 1.0f)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
        return isSoundMute
    }

    /**
     * 静音、系统原生
     * @return true:静音 false:系统原生
     */
    fun toggleMute(): Boolean {
        val isMute = !isSoundMute
        setSoundMute(isMute)
        return isMute
    }

    /**
     * 设置缩放模式
     * @param zoomModel 请适用IMediaPlayer类中定义的常量值
     */
    fun setZoomModel(zoomModel: Int) {
        this.mZoomMode = zoomModel
        if (null != mRenderView) mRenderView!!.setZoomMode(zoomModel)
    }

    /**
     * 设置画面旋转角度
     * @param degree 画面的旋转角度
     */
    fun setDegree(degree: Int) {
        if (null != mRenderView) mRenderView!!.setDegree(degree)
    }

    /**
     * 是否监听并处理音频焦点事件
     * @param interceptTAudioFocus true:拦截，并在收到音频焦点失去后暂停播放 false:什么也不处理
     */
    fun setInterceptTAudioFocus(interceptTAudioFocus: Boolean) {
        IVideoManager.setInterceptTAudioFocus(interceptTAudioFocus)
    }

    /**
     * @param reCatenationCount 设置当播放器遇到链接视频文件失败时自动重试的次数，内部自动重试次数为3次
     */
    fun setReCatenationCount(reCatenationCount: Int) {
        this.mReCatenationCount = reCatenationCount
    }

    /**
     * 设置画面渲染是否镜像
     * @param mirror true:镜像 false:正常
     * @return true:镜像 false:正常
     */
    fun setMirror(mirror: Boolean): Boolean {
        this.mMirrors = mirror
        if (null != mRenderView) {
            return mRenderView!!.setMirror(mirror)
        }
        return false
    }

    /**
     * 画面渲染是否镜像
     * @return true:镜像 false:正常
     */
    fun toggleMirror(): Boolean {
        val isMirrors = !mMirrors
        return setMirror(isMirrors)
    }

    /**
     * 设置倍速播放
     * @param speed
     */
    fun setSpeed(speed: Float) {
        if (null != mMediaPlayer) mMediaPlayer!!.setSpeed(speed)
    }

    /**
     * 设置左右声道音量，从0.0f-1.0f
     * @param leftVolume 设置左声道音量，1.0f-1.0f
     * @param rightVolume 设置右声道音量，1.0f-1.0f
     */
    fun setVolume(leftVolume: Float, rightVolume: Float) {
        this.mLeftVolume = leftVolume
        this.mRightVolume = rightVolume
        if (null != mMediaPlayer) mMediaPlayer!!.setVolume(leftVolume, rightVolume)
    }

    /**
     * 设置View旋转角度
     * @param rotation
     */
    fun setRotation(rotation: Int) {
        if (null != mRenderView) mRenderView!!.setViewRotation(rotation)
    }

    /**
     * 设置进度回调间隔时间长 单位：毫秒
     * @param callBackSpaceMilliss
     */
    fun setCallBackSpaceMilliss(callBackSpaceMilliss: Int) {
        mCallBackSpaceMilliss = callBackSpaceMilliss.toLong()
    }

    /**
     * 是否支持4G网络播放
     * @param mobileNetwork
     */
    fun setMobileNetwork(mobileNetwork: Boolean) {
        IVideoManager.setMobileNetwork(mobileNetwork)
    }


    fun getVideoWidth(): Int {
        return mVideoWidth
    }

    fun getVideoHeight(): Int {
        return mVideoHeight
    }

    /**
     * 播放和暂停,推荐外部调用此方法
     */
    @JvmOverloads
    fun playOrPause(dataSource: Any? = this.dataSource) {
        if (null == dataSource) {
            playerState = PlayerState.STATE_ERROR
            onPlayerState(playerState, getString(R.string.player_media_error_path_empty, "播放地址为空,请检查!"))
            return
        }
        when (playerState) {
            PlayerState.STATE_RESET, PlayerState.STATE_STOP, PlayerState.STATE_MOBILE, PlayerState.STATE_COMPLETION, PlayerState.STATE_ERROR, PlayerState.STATE_DESTROY -> startPlayer(
                dataSource
            )

            PlayerState.STATE_PREPARE, PlayerState.STATE_BUFFER, PlayerState.STATE_START, PlayerState.STATE_PLAY, PlayerState.STATE_ON_PLAY -> onPause(
                true
            )

            PlayerState.STATE_ON_PAUSE, PlayerState.STATE_PAUSE -> {
                playerState = PlayerState.STATE_ON_PAUSE
                onResume()
            }
        }
    }

    val isPlaying: Boolean
        /**
         * 返回内部播放状态
         * @return
         */
        get() {
            try {
                return null != mMediaPlayer && (playerState == PlayerState.STATE_PREPARE || playerState == PlayerState.STATE_START || playerState == PlayerState.STATE_PLAY || playerState == PlayerState.STATE_ON_PLAY || playerState == PlayerState.STATE_BUFFER)
            } catch (e: RuntimeException) {
            }
            return false
        }

    val isWork: Boolean
        /**
         * 返回内部工作状态
         * @return
         */
        get() {
            try {
                return null != mMediaPlayer && (playerState == PlayerState.STATE_PREPARE || playerState == PlayerState.STATE_START || playerState == PlayerState.STATE_PLAY || playerState == PlayerState.STATE_ON_PLAY || playerState == PlayerState.STATE_PAUSE || playerState == PlayerState.STATE_ON_PAUSE || playerState == PlayerState.STATE_BUFFER)
            } catch (e: RuntimeException) {
            }
            return false
        }

    /**
     * 播放状态下允许快进、快退调节
     * @param msec 0:重新播放 其它:快进快退
     */
    fun seekTo(msec: Long) {
        if (msec < 0 || !checkedDataSource()) return
        if (0L == msec) {
            playOrPause()
            return
        }
        if (isWork) {
            try {
                if (null != mMediaPlayer) {
                    mMediaPlayer!!.seekTo(msec)
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        } else {
            //未开始播放情况下重新开始播放并立即跳转
            mSeekDuration = msec
            playOrPause()
        }
    }

    /**
     * 播放状态下允许快进、快退调节
     * @param msec 0:重新播放 其它:快进快退
     * @param accurate 是否精准seek
     */
    fun seekTo(msec: Long, accurate: Boolean) {
        if (msec < 0 || !checkedDataSource()) return
        if (isPlaying) {
            try {
                if (null != mMediaPlayer) {
                    mMediaPlayer!!.seekTo(msec, accurate)
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        } else {
            //未开始播放情况下重新开始播放并立即跳转到指定未知播放
            mSeekDuration = msec
            playOrPause()
        }
    }

    /**
     * 设置超时时间
     * @param prepareTimeout 设置准备和读数据超时阈值,之前调用方可生效 准备超时阈值,即播放器在建立链接、解析流媒体信息的超时阈值
     * @param readTimeout    读数据超时阈值
     */
    fun setTimeout(prepareTimeout: Int, readTimeout: Int) {
        this.mPrepareTimeout = prepareTimeout
        this.mReadTimeout = readTimeout
        if (null != mMediaPlayer) mMediaPlayer!!.setTimeout(prepareTimeout.toLong(), readTimeout.toLong())
    }

    val durtion: Long
        /**
         * 返回视频的总时长
         * @return 毫秒
         */
        get() {
            if (null != mMediaPlayer) {
                try {
                    return mMediaPlayer!!.duration
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
            return 0
        }

    val currentPosition: Long
        /**
         * 返回当前正在播放的位置
         * @return 毫秒
         */
        get() {
            if (null != mMediaPlayer) {
                try {
                    return mMediaPlayer!!.currentPosition
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
            return 0
        }

    val buffer: Int
        /**
         * 返回当前缓冲进度
         * @return 单位：百分比
         */
        get() {
            if (null != mMediaPlayer) {
                try {
                    return mMediaPlayer!!.buffer
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
            return 0
        }

    /**
     * 生命周期调用恢复播放,在用户主动暂停的情况下不主动恢复播放
     * 手动点击暂停\恢复请调用playOrPause
     */
    fun onResume() {
        if (checkedDataSource() && playerState == PlayerState.STATE_ON_PAUSE) {
            startTimer()
            try {
                if (null != mMediaPlayer) {
                    mMediaPlayer!!.start()
                }
                listenerAudioFocus()
                playerState = PlayerState.STATE_ON_PLAY
                onPlayerState(playerState, getString(R.string.player_media_resume, "恢复播放"))
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 生命周期调用暂停播放
     * 手动点击暂停\恢复请调用playOrPause
     */
    fun onPause() {
        onPause(false)
    }

    private fun onPause(isClick: Boolean) {
        if (checkedDataSource() && isPlaying) {
            stopTimer()
            try {
                if (null != mMediaPlayer) {
                    mMediaPlayer!!.pause()
                }
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            playerState = if (isClick) PlayerState.STATE_PAUSE else PlayerState.STATE_ON_PAUSE
            onPlayerState(playerState, getString(R.string.player_media_pause, "暂停播放"))
        }
    }

    /**
     * 主动完成播放,会回调COMPLETION状态给控制器和宿主
     */
    fun onCompletion() {
        stopTimer()
        releaseTextureView()
        playerState = PlayerState.STATE_COMPLETION
        onPlayerState(playerState, getString(R.string.player_media_completion, "播放结束"))
    }

    /**
     * 结束播放,不销毁内部播放地址,可能用户还会重新播放
     */
    fun onStop() {
        stopTimer()
        releaseTextureView()
        playerState = PlayerState.STATE_STOP
        onPlayerState(playerState, getString(R.string.player_media_stop, "停止播放"))
    }

    /**
     * 清除/还原播放器及播放状态
     */
    fun onReset() {
        stopTimer()
        releaseTextureView()
        mDataSource = null
        mAssetsSource = null
        mVideoWidth = 0
        mVideoHeight = 0
        mReCount = 0
        playerState = PlayerState.STATE_RESET
        onPlayerState(playerState, getString(R.string.player_media_reset, "结束播放并重置"))
    }

    /**
     * 销毁播放器,一旦销毁内部所有持有对象将被回收
     */
    fun onDestroy() {
        stopTimer()
        ThreadPool.getInstance().reset()
        releaseTextureView()
        playerState = PlayerState.STATE_DESTROY
        onPlayerState(playerState, getString(R.string.player_media_destroy, "播放器销毁"))
        if (null != mAudioFocusManager) {
            mAudioFocusManager!!.onDestroy()
            mAudioFocusManager = null
        }
        mLoop = false
        isSoundMute = false
        mMirrors = false
        mVideoWidth = 0
        mVideoHeight = 0
        mPrepareTimeout = 0
        mReadTimeout = 0
        mZoomMode = 0
        mReCount = 0
        mBasePlayer = null
        mDataSource = null
        mAssetsSource = null
        mLeftVolume = 1.0f
        mRightVolume = 1.0f
        mCallBackSpaceMilliss = DEFAULT_CALLBACK_TIME
    }

    companion object {
        private val TAG: String = IVideoPlayer::class.java.simpleName
    }
}