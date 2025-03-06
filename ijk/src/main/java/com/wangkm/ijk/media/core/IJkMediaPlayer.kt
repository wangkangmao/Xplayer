package com.wangkm.ijk.media.core

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.text.TextUtils
import android.view.Surface
import android.view.SurfaceHolder
import com.wangkm.ijk.media.RawDataSourceProvider
import com.wangkm.ijk.media.RawDataSourceProvider.Companion.create
import com.wangkm.xplayer.base.AbstractMediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException

/**
 * created by wangkm
 * Desc:基于ijkPlayer实现的解码器示例，此解码器自动开启硬件解码，如果硬件解码失败则自动切换至软件解码。
 * 原项目地址：https://github.com/bilibili/ijkplayer
 */
class IJkMediaPlayer @JvmOverloads constructor(context: Context?, isLive: Boolean = false) : AbstractMediaPlayer(
    context!!
), IMediaPlayer.OnPreparedListener, IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener,
    IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener {
    var mediaPlayer: IjkMediaPlayer? = null
        private set
    private var mBuffer = 0 //缓冲进度

    init {
        if (null != context) {
            mediaPlayer = IjkMediaPlayer()
            mediaPlayer!!.setOnPreparedListener(this)
            mediaPlayer!!.setOnBufferingUpdateListener(this)
            mediaPlayer!!.setOnSeekCompleteListener(this)
            mediaPlayer!!.setOnVideoSizeChangedListener(this)
            mediaPlayer!!.setOnInfoListener(this)
            mediaPlayer!!.setOnCompletionListener(this)
            mediaPlayer!!.setOnErrorListener(this)
            setOption(isLive)
        }
    }

    /**
     * 初始化Option设置
     * @param isLive 是否是应用于直播拉流模式
     */
    private fun setOption(isLive: Boolean) {
        //解决seek跳转时，可能出现跳转的位置和自己选择的进度不一致，是因为seek只支持关键帧，视频的关键帧比较少导致的。
        mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)
        // 播放前的探测Size，默认是1M, 改小一点会出画面更快
        mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 200) //1024L)
        //默认关闭硬件解码
        mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0)
        mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0)
        mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0)
        //播放重连次数
        mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "reconnect", 30)
        if (isLive) {
            //跳帧处理,放CPU处理较慢时，进行跳帧处理，保证播放流程，画面和声音同步
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5)
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp")
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp")
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0)
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48)
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
            //增加rtmp打开速度. 没有缓存会黑屏1s.
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "buffer_size", 1024) //1316

            //视频帧率
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fps", 30)
            //环路滤波
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48)
            //设置无packet缓存 是否开启预缓冲，通常直播项目会开启，达到秒开的效果，不过带来了播放丢帧卡顿的体验
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)
            //不限制拉流缓存大小
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1)
            //设置最大缓存数量
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 1024)
            //设置最小解码帧数
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 3)
            //启动预加载 须要准备好后自动播放
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1)
            //设置分析流时长
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", "2000000")
            /**
             * 播放延时的解决方案
             */
            // 每处理一个packet以后刷新io上下文
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1L)
            // 不额外优化（使能非规范兼容优化，默认值0 ）
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "fast", 1)
            // 自动旋屏
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0)
            // 处理分辨率变化
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0)
            // 最大缓冲大小,单位kb
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "max-buffer-size", 0)
            // 默认最小帧数2
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2)
            // 最大缓存时长
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 300)
            // 是否限制输入缓存数
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1)
            // 缩短播放的rtmp视频延迟在1s内
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer")
            // 设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48L)
            // 跳过帧 ？？
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0)
        }
    }

    /**
     * 用户可自定义设置option
     * @param category
     * @param name
     * @param value
     */
    fun setOption(category: Int, name: String?, value: Long) {
        if (null != mediaPlayer) mediaPlayer!!.setOption(category, name, value)
    }

    /**
     * 设置是否启用硬件解码，开启硬件解码可能由于各系统版本的兼容性导致黑屏、无声等问题，请慎用！
     * 启用之后，如果内部使用硬件解码失败，则自动切换到软件解码
     * @param hardwareDeCode true:开启硬件解码 false:关闭硬件解码
     */
    fun setHardwareDeCode(hardwareDeCode: Boolean) {
        if (null != mediaPlayer) {
            //开启硬解码 硬解码失败 再自动切换到软解码
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", (if (hardwareDeCode) 1 else 0).toLong())
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", (if (hardwareDeCode) 1 else 0).toLong())
            mediaPlayer!!.setOption(
                IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                "mediacodec-handle-resolution-change",
                (if (hardwareDeCode) 1 else 0).toLong()
            )
        }
    }

    override fun setLooping(loop: Boolean) {
        if (null != mediaPlayer) mediaPlayer!!.isLooping = loop
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        if (null != mediaPlayer) mediaPlayer!!.setVolume(leftVolume, rightVolume)
    }

    override fun setBufferTimeMax(timeSecond: Float) {
//        if(null!=mMediaPlayer) mMediaPlayer.setBufferTimeMax(timeSecond);
    }

    override fun setSurface(surface: Surface) {
        if (null != mediaPlayer) mediaPlayer!!.setSurface(surface)
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder) {
        if (null != mediaPlayer) mediaPlayer!!.setDisplay(surfaceHolder)
    }

    @Throws(IOException::class, IllegalArgumentException::class, SecurityException::class, IllegalStateException::class)
    override fun setDataSource(dataSource: String) {
        setDataSource(dataSource, null)
    }

    @Throws(IOException::class, IllegalArgumentException::class, SecurityException::class, IllegalStateException::class)
    override fun setDataSource(path: String, headers: MutableMap<String, String>?) {
        if (null != mediaPlayer) {
            try {
                val uri = Uri.parse(path)
                if (ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme) {
                    val rawDataSourceProvider = create(context, uri)
                    mediaPlayer!!.setDataSource(rawDataSourceProvider)
                } else {
                    //处理UA问题
                    if (headers != null && headers.size > 0) {
                        val userAgent = headers["User-Agent"]
                        if (!TextUtils.isEmpty(userAgent)) {
                            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", userAgent)
                            // 移除header中的User-Agent，防止重复
                            headers.remove("User-Agent")
                        }
                        mediaPlayer!!.setDataSource(context, uri, headers)
                    } else {
                        mediaPlayer!!.dataSource = path
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
    override fun setDataSource(dataSource: AssetFileDescriptor) {
        if (null != dataSource && null != mediaPlayer) {
            try {
                mediaPlayer!!.setDataSource(RawDataSourceProvider(dataSource))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun setTimeout(prepareTimeout: Long, readTimeout: Long) {
        //超时时间，timeout参数只对http设置有效。若果你用rtmp设置timeout，ijkplayer内部会忽略timeout参数。rtmp的timeout参数含义和http的不一样。
        if (null != mediaPlayer) {
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzemaxduration", readTimeout)
            mediaPlayer!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", prepareTimeout)
        }
    }

    override fun setSpeed(speed: Float) {
        if (null != mediaPlayer) mediaPlayer!!.setSpeed(speed)
    }

    @Throws(IllegalStateException::class)
    override fun seekTo(msec: Long) {
        if (null != mediaPlayer) mediaPlayer!!.seekTo(msec)
    }

    @Throws(IllegalStateException::class)
    override fun seekTo(msec: Long, accurate: Boolean) {
        if (null != mediaPlayer) mediaPlayer!!.seekTo(msec)
    }

    override fun isPlaying(): Boolean {
        if (null != mediaPlayer) {
            return mediaPlayer!!.isPlaying
        }
        return false
    }

    override fun getCurrentPosition(): Long {
        if (null != mediaPlayer) {
            return mediaPlayer!!.currentPosition
        }
        return 0
    }

    override fun getDuration(): Long {
        if (null != mediaPlayer) {
            return mediaPlayer!!.duration
        }
        return 0
    }

    override fun getBuffer(): Int {
        return mBuffer
    }

    @Throws(IOException::class, IllegalStateException::class)
    override fun prepare() {
    }

    @Throws(IllegalStateException::class)
    override fun prepareAsync() {
        if (null != mediaPlayer) mediaPlayer!!.prepareAsync()
    }

    override fun start() {
        if (null != mediaPlayer) mediaPlayer!!.start()
    }

    override fun pause() {
        if (null != mediaPlayer) mediaPlayer!!.pause()
    }

    override fun stop() {
        if (null != mediaPlayer) mediaPlayer!!.stop()
    }

    override fun reset() {
        mBuffer = 0
        if (null != mediaPlayer) {
            val mediaPlayer = mediaPlayer!! //用于在列表播放时避免卡顿
            object : Thread() {
                override fun run() {
                    try {
                        mediaPlayer.reset()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }

    override fun release() {
        mBuffer = 0
        if (null != mediaPlayer) {
            mediaPlayer!!.setOnErrorListener(null)
            mediaPlayer!!.setOnCompletionListener(null)
            mediaPlayer!!.setOnInfoListener(null)
            mediaPlayer!!.setOnBufferingUpdateListener(null)
            mediaPlayer!!.setOnPreparedListener(null)
            mediaPlayer!!.setOnVideoSizeChangedListener(null)
            mediaPlayer!!.setSurface(null)
            mediaPlayer!!.setDisplay(null)
            val mediaPlayer = mediaPlayer!! //用于在列表播放时避免卡顿
            this.mediaPlayer = null
            object : Thread() {
                override fun run() {
                    try {
                        mediaPlayer.release()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
        super.release()
    }

    override fun onPrepared(mp: IMediaPlayer) {
        if (null != mListener) mListener!!.onPrepared(this@IJkMediaPlayer)
    }

    override fun onCompletion(mp: IMediaPlayer) {
        if (null != mListener) mListener!!.onCompletion(this@IJkMediaPlayer)
    }

    override fun onBufferingUpdate(mp: IMediaPlayer, percent: Int) {
        mBuffer = percent
        if (null != mListener) mListener!!.onBufferUpdate(this@IJkMediaPlayer, percent)
    }

    override fun onSeekComplete(mp: IMediaPlayer) {
        if (null != mListener) mListener!!.onSeekComplete(this@IJkMediaPlayer)
    }

    override fun onVideoSizeChanged(mp: IMediaPlayer, width: Int, height: Int, sar_num: Int, sar_den: Int) {
        if (null != mListener) mListener!!.onVideoSizeChanged(this@IJkMediaPlayer, width, height, sar_num, sar_den)
    }

    override fun onError(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
        if (null != mListener) {
            return mListener!!.onError(this@IJkMediaPlayer, what, extra)
        }
        return true
    }

    override fun onInfo(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
        if (null != mListener) {
            return mListener!!.onInfo(this@IJkMediaPlayer, what, extra)
        }
        return true
    }
}