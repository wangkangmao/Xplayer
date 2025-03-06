package com.wangkm.xplayer.manager



/**
 * created by wangkm
 * Desc:播放器内部的公共设置等
 */
object IVideoManager {
    //是否支持在4G环境下播放
    var isMobileNetwork: Boolean = false
        private set

    /**
     * 是否监听并处理音频焦点事件
     * @return true:拦截，并在收到音频焦点失去后暂停播放 false:什么也不处理
     */
    //是否监听并处理音频焦点事件？
    var isInterceptTAudioFocus: Boolean = false //true:拦截，并在收到音频焦点失去后暂停播放 false:什么也不处理
        private set

    /**
     * 是否支持4G网络播放
     * @param mobileNetwork
     */
    fun setMobileNetwork(mobileNetwork: Boolean): IVideoManager? {
        isMobileNetwork = mobileNetwork
        return this
    }

    /**
     * 是否监听并处理音频焦点事件
     * @param interceptTAudioFocus true:拦截，并在收到音频焦点失去后暂停播放 false:什么也不处理
     */
    fun setInterceptTAudioFocus(interceptTAudioFocus: Boolean): IVideoManager? {
        isInterceptTAudioFocus = interceptTAudioFocus
        return this
    }

}