package com.wangkm.xplayer.listener

import com.wangkm.xplayer.base.AbstractMediaPlayer
import com.wangkm.xplayer.interfaces.IRenderView
import com.wangkm.xplayer.model.PlayerState

/**
 * created by wangkm
 * Desc:简单的播放器内部事件回调,提供给播放器容器的宿主实现监听
 */
abstract class OnPlayerEventListener {
    /**
     * 如需自定义解码器,可复写此方法并返回一个继承自AbstractMediaPlayer的自定义多媒体解码器,如果返回为空,则适用内部默认的DefaultMediaPlayer解码器
     * @return 一个自定义的多媒体解码器
     */
    open fun createMediaPlayer(): AbstractMediaPlayer? {
        return null
    }

    /**
     * 如需自定义解码器,可复写此方法并返回一个实现了IRenderView接口的自定义画面渲染器,如果返回为空,则适用内部默认的MediaTextureView解码器
     * @return
     */
    open fun createRenderView(): IRenderView? {
        return null
    }

    /**
     * 播放器内部各状态
     * @param state 状态请参考PlayerState中定义的状态
     * @param message 状态描述
     */
    open fun onPlayerState(state: PlayerState?, message: String?) {}

    /**
     * 视频宽高
     * @param width 视频宽
     * @param height 视频高
     */
    fun onVideoSizeChanged(width: Int, height: Int) {}

    /**
     * 播放进度实时回调,回调到主线程
     * @param currentDurtion 当前播放进度,单位:毫秒时间戳
     * @param totalDurtion 总时长,单位:毫秒时间戳
     */
    fun onProgress(currentDurtion: Long, totalDurtion: Long) {}

    /**
     * @param isMute 当静音状态发生了变化回调，true:处于静音状态 false:处于非静音状态
     */
    open fun onMute(isMute: Boolean) {}

    /**
     * @param isMirror 当播放器的内部画面渲染镜像状态发生了变化回调， true:处于镜像状态 false:处于非镜像状态
     */
    fun onMirror(isMirror: Boolean) {}

    /**
     * @param zoomModel 当播放器内部渲染缩放模式发生了变化回调，，当初始化和播放器缩放模式设置发生变化时回调，参考IMediaPlayer类
     */
    fun onZoomModel(zoomModel: Int) {}
}