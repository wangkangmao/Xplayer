package com.wangkm.xplayer.media.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.media.MediaPlayer.OnBufferingUpdateListener
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.OnSeekCompleteListener
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import com.wangkm.xplayer.base.AbstractMediaPlayer
import java.io.IOException

/**
 * created by wangkm
 * Desc:默认的多媒体解码器
 */
class MediaPlayer(context: Context?) : AbstractMediaPlayer(context!!), OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
    OnSeekCompleteListener, OnVideoSizeChangedListener, MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener {
    private var mMediaPlayer: MediaPlayer?
    private var mBuffer = 0 //缓冲进度

    init {
        mMediaPlayer = MediaPlayer()
        mMediaPlayer!!.setOnPreparedListener(this)
        mMediaPlayer!!.setOnBufferingUpdateListener(this)
        mMediaPlayer!!.setOnSeekCompleteListener(this)
        mMediaPlayer!!.setOnVideoSizeChangedListener(this)
        mMediaPlayer!!.setOnInfoListener(this)
        mMediaPlayer!!.setOnCompletionListener(this)
        mMediaPlayer!!.setOnErrorListener(this)
    }

    override fun setLooping(loop: Boolean) {
        if (null != mMediaPlayer) mMediaPlayer!!.isLooping = loop
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        if (null != mMediaPlayer) mMediaPlayer!!.setVolume(leftVolume, rightVolume)
    }

    override fun setBufferTimeMax(timeSecond: Float) {
//        if(null!=mMediaPlayer) mMediaPlayer.setBufferTimeMax(timeSecond);
    }

    override fun setSurface(surface: Surface) {
        if (null != mMediaPlayer) mMediaPlayer!!.setSurface(surface)
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder) {
        if (null != mMediaPlayer) mMediaPlayer!!.setDisplay(surfaceHolder)
    }

    @Throws(IOException::class, IllegalArgumentException::class, SecurityException::class, IllegalStateException::class)
    override fun setDataSource(dataSource: String) {
        if (null != mMediaPlayer) {
            try {
                val uri = Uri.parse(dataSource)
                (context).let { mMediaPlayer!!.setDataSource(it, uri) }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class, IllegalArgumentException::class, SecurityException::class, IllegalStateException::class)
    override fun setDataSource(path: String, headers: Map<String, String>) {
        if (null != mMediaPlayer) {
            try {
                mMediaPlayer!!.setDataSource(context, Uri.parse(path), headers)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
    override fun setDataSource(dataSource: AssetFileDescriptor) {
        if (null != mMediaPlayer) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mMediaPlayer!!.setDataSource(dataSource)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun setTimeout(prepareTimeout: Long, readTimeout: Long) {
//        if(null!=mMediaPlayer) mMediaPlayer.setTimeout(prepareTimeout,readTimeout);
    }

    override fun setSpeed(speed: Float) {}

    @Throws(IllegalStateException::class)
    override fun seekTo(msec: Long) {
        if (null != mMediaPlayer) mMediaPlayer!!.seekTo(msec.toInt())
    }

    @SuppressLint("WrongConstant", "NewApi")
    @Throws(IllegalStateException::class)
    override fun seekTo(msec: Long, accurate: Boolean) {
        if (null != mMediaPlayer) {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
                mMediaPlayer!!.seekTo(msec, if (accurate) 1 else 0)
            } else {
                mMediaPlayer!!.seekTo(msec.toInt())
            }
        }
    }

    override fun isPlaying(): Boolean {
        if (null != mMediaPlayer) {
            return mMediaPlayer!!.isPlaying
        }
        return false
    }

    override fun getCurrentPosition(): Long {
        if (null != mMediaPlayer) {
            return mMediaPlayer!!.currentPosition.toLong()
        }
        return 0
    }

    override fun getDuration(): Long {
        if (null != mMediaPlayer) {
            return mMediaPlayer!!.duration.toLong()
        }
        return 0
    }

    override fun getBuffer(): Int {
        return mBuffer
    }

    @Throws(IOException::class, IllegalStateException::class)
    override fun prepare() {
        if (null != mMediaPlayer) mMediaPlayer!!.prepare()
    }

    @Throws(IllegalStateException::class)
    override fun prepareAsync() {
        if (null != mMediaPlayer) mMediaPlayer!!.prepareAsync()
    }

    override fun start() {
        if (null != mMediaPlayer) mMediaPlayer!!.start()
    }

    override fun pause() {
        if (null != mMediaPlayer) mMediaPlayer!!.pause()
    }

    override fun stop() {
        if (null != mMediaPlayer) mMediaPlayer!!.stop()
    }

    override fun reset() {
        mBuffer = 0
        if (null != mMediaPlayer) {
            val mediaPlayer: MediaPlayer = mMediaPlayer as MediaPlayer //用于在列表播放时避免卡顿
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
        if (null != mMediaPlayer) {
            mMediaPlayer!!.setOnErrorListener(null)
            mMediaPlayer!!.setOnCompletionListener(null)
            mMediaPlayer!!.setOnInfoListener(null)
            mMediaPlayer!!.setOnBufferingUpdateListener(null)
            mMediaPlayer!!.setOnPreparedListener(null)
            mMediaPlayer!!.setOnVideoSizeChangedListener(null)
            mMediaPlayer!!.setSurface(null)
            mMediaPlayer!!.setDisplay(null)
            val mediaPlayer: MediaPlayer = mMediaPlayer as MediaPlayer //用于在列表播放时避免卡顿
            mMediaPlayer = null
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

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        if (null != mListener) mListener!!.onPrepared(this@MediaPlayer)
    }

    override fun onBufferingUpdate(mediaPlayer: MediaPlayer, percent: Int) {
        mBuffer = percent
        if (null != mListener) mListener!!.onBufferUpdate(this@MediaPlayer, percent)
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (null != mListener) mListener!!.onCompletion(this@MediaPlayer)
    }

    override fun onSeekComplete(mediaPlayer: MediaPlayer) {
        if (null != mListener) mListener!!.onSeekComplete(this@MediaPlayer)
    }

    override fun onVideoSizeChanged(mediaPlayer: MediaPlayer, width: Int, height: Int) {
        if (null != mListener) mListener!!.onVideoSizeChanged(this@MediaPlayer, width, height, 0, 0)
    }

    override fun onInfo(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
        if (null != mListener) {
            return mListener!!.onInfo(this@MediaPlayer, what, extra)
        }
        return true
    }

    override fun onError(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
        if (null != mListener) {
            return mListener!!.onError(this@MediaPlayer, what, extra)
        }
        return true
    }
}