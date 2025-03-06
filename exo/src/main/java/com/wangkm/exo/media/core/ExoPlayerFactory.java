package com.wangkm.exo.media.core;

import android.content.Context;

import com.wangkm.xplayer.media.MediaFactory;


/**
 * created by wangkm
 * Desc:ExoPlayer播放解码器的工厂类，{@link ExoMediaPlayer}
 */
public class ExoPlayerFactory extends MediaFactory<ExoMediaPlayer> {

    public static ExoPlayerFactory create() {
        return new ExoPlayerFactory();
    }

    @Override
    public ExoMediaPlayer createPlayer(Context context) {
        return new ExoMediaPlayer(context);
    }
}