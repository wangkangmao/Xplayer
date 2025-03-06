package com.wangkm.xplayer.media

import android.content.Context
import com.wangkm.xplayer.base.AbstractMediaPlayer

/**
 * created by wangkm
 * Desc:所有解码器的工厂构造可继承此类来实现创建自己的解码器
 */
abstract class MediaFactory<M : AbstractMediaPlayer?> {
    /**
     * 构造播放器解码器
     * @param context 上下文
     * @return 继承自AbstractMediaPlayer的解码器
     */
    abstract fun createPlayer(context: Context?): M
}