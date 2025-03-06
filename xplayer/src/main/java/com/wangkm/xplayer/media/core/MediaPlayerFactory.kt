package com.wangkm.xplayer.media.core

import android.content.Context
import com.wangkm.xplayer.media.MediaFactory

/**
 * created by wangkm
 * Desc:系统MediaPlayer的工厂类，[MediaPlayer]
 */
class MediaPlayerFactory : MediaFactory<MediaPlayer?>() {

    companion object {
        @JvmStatic
        fun create(): MediaPlayerFactory {
            return MediaPlayerFactory()
        }
    }

    override fun createPlayer(context: Context?): MediaPlayer? {
        return MediaPlayer(context)
    }
}