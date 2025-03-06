package com.wangkm.xplayer.listener

import com.wangkm.xplayer.base.BasePlayer

/**
 * created by wangkm
 * Desc:全局的悬浮窗窗口播放器关闭\点击事件监听器
 */
interface OnWindowActionListener {
    /**
     * 持续移动中
     * @param x
     * @param y
     */
    fun onMovie(x: Float, y: Float)

    /**
     * 点击悬浮窗回调
     * @param basePlayer 播放器实例
     * @param customParams 自定义参数
     */
    fun onClick(basePlayer: BasePlayer?, customParams: Any?)


    /**
     * 关闭事件
     */
    fun onClose()
}