package com.wangkm.xplayer.base

import android.content.Context
import com.wangkm.xplayer.listener.OnMediaEventListener
import com.wangkm.xplayer.media.IMediaPlayer

/**
 * Created by wangkm
 * Desc:多媒体解码器基类，自定义多媒体解码器必须继承此类重写&&赋值所关心的监听器
 * 1、自定义解码器实现请参考com.android.iplayer.media.core.MediaPlayer类
 * 2、所有子类必须将OnMediaEventListener事件回调给mListener
 */
 abstract class AbstractMediaPlayer(protected var context: Context) : IMediaPlayer {
    protected val TAG: String = AbstractMediaPlayer::class.java.simpleName
    @JvmField
    protected var mListener: OnMediaEventListener? = null //播放器监听器

    override fun setMediaEventListener(listener: OnMediaEventListener) {
        this.mListener = listener
    }

    override fun release() {
        mListener = null
    }
}