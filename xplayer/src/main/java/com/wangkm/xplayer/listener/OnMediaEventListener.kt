package com.wangkm.xplayer.listener

import com.wangkm.xplayer.media.IMediaPlayer

/**
 * created by wangkm
 * Desc:播放器内部各种事件回调
 */
interface OnMediaEventListener {
    /**
     * 播放器异步准备好了
     * @param mp 播放器
     */
    fun onPrepared(mp: IMediaPlayer?)

    /**
     * 播放器缓冲进度
     * @param mp 播放器
     * @param percent 缓冲进度，单位：百分比
     */
    fun onBufferUpdate(mp: IMediaPlayer?, percent: Int)

    /**
     * seek跳转播放成功
     * @param mp 播放器
     */
    fun onSeekComplete(mp: IMediaPlayer?)

    /**
     * 视频的宽高发生变化
     * @param mp 播放器
     * @param width 视频宽，单位：分辨率
     * @param height 视频高，单位：分辨率
     * @param sar_num 视频比例X
     * @param sar_den 视频比例Y
     */
    fun onVideoSizeChanged(mp: IMediaPlayer?, width: Int, height: Int, sar_num: Int, sar_den: Int)

    /**
     * 消息监听器,会将关于播放器的消息告知开发者,例如:视频渲染、音频渲染等
     * @param mp 播放器
     * @param what code码
     * @param extra 角度等其它参数
     * @return
     */
    fun onInfo(mp: IMediaPlayer?, what: Int, extra: Int): Boolean

    /**
     * 播放完成，仅当setLoop为false回调
     * @param mp 播放器
     */
    fun onCompletion(mp: IMediaPlayer?)

    /**
     * 错误监听器,播放器遇到错误时会将相应的错误码通过此回调接口告知开发者
     * @param mp 播放器
     * @param what 错误码
     * @param extra 错误信息
     * @return
     */
    fun onError(mp: IMediaPlayer?, what: Int, extra: Int): Boolean
}