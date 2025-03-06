package com.wangkm.xplayer.controller

import com.wangkm.xplayer.interfaces.IPlayerControl
import com.wangkm.xplayer.interfaces.IVideoController
import com.wangkm.xplayer.model.PlayerState


/**
 * created by wangkm
 * Desc:一个在[IVideoController](BaseController)和[IPlayerControl](BasePlayer)之间通信交互的桥梁
 */
class ControlWrapper(//控制器
    val controller: IVideoController?, //播放器
    val videoPlayer: IPlayerControl<*>?
) {
    //========================================控制器常用功能方法========================================
    val isCompletion: Boolean
        /**
         * 是否播放完成
         * @return true:播放完成 false:未播放完成
         */
        get() {
            if (null != controller) {
                return controller.isCompletion
            }
            return false
        }

    val isOrientationPortrait: Boolean
        /**
         * 返回播放器\控制器是否处于竖屏状态
         * @return true:处于竖屏状态 false:非竖屏状态
         */
        get() {
            if (null != controller) {
                return controller.isOrientationPortrait
            }
            return true
        }

    val isOrientationLandscape: Boolean
        /**
         * 返回播放器\控制器是否处于横屏状态
         * @return true:处于竖屏状态 false:非竖屏状态
         */
        get() {
            if (null != controller) {
                return controller.isOrientationLandscape
            }
            return false
        }

    val playerScene: Int
        /**
         * 返回控制器当前正处于什么场景
         * @return 返回值参考IVideoController，0：常规状态(包括竖屏、横屏)，1：activity小窗口，2：全局悬浮窗窗口，3：Android8.0的画中画，4：列表 也可自定义更多场景
         */
        get() {
            if (null != controller) {
                return controller.playerScene
            }
            return 0
        }

    val preViewTotalDuration: Long
        /**
         * 返回试看模式下的虚拟总时长
         * @return 单位：毫秒
         */
        get() {
            if (null != controller) {
                return controller.preViewTotalDuration
            }
            return 0
        }

    /**
     * 有些特殊场景，比如seek后立即改变内部播放器为缓冲状态时，可以手动更改播放器内部的播放状态,慎用！！！
     * @param state 播放器内部状态,请参阅PlayerState
     * @param message 描述信息
     */
    fun onPlayerState(state: PlayerState?, message: String?) {
        if (null != controller) controller.onPlayerState(state, message)
    }

    /**
     * 开始延时任务
     */
    fun startDelayedRunnable() {
        if (null != controller) controller.startDelayedRunnable()
    }

    /**
     * 结束延时任务
     */
    fun stopDelayedRunnable() {
        if (null != controller) controller.stopDelayedRunnable()
    }

    /**
     * 重新开始延时任务
     */
    fun reStartDelayedRunnable() {
        if (null != controller) controller.reStartDelayedRunnable()
    }

    /**
     * @param isAnimation 请求其它所有UI组件隐藏自己的控制器,是否开启动画
     */
    fun hideAllController(isAnimation: Boolean) {
        if (null != controller) controller.hideAllController(isAnimation)
    }

    val animationDuration: Long
        /**
         * @return 返回控制器的各UI组件显示、隐藏动画持续时间戳，单位：毫秒
         */
        get() {
            if (null != controller) {
                return controller.animationDuration
            }
            return IVideoController.MATION_DRAUTION
        }

    //========================================播放器常用功能方法========================================
    /**
     * 开始\暂停播放
     */
    fun togglePlay() {
        if (null != videoPlayer) videoPlayer.togglePlay()
    }

    /**
     * 结束播放
     */
    fun stopPlay() {
        if (null != videoPlayer) videoPlayer.onStop()
    }

    /**
     * 开启全屏播放
     */
    fun startFullScreen() {
        if (null != videoPlayer) videoPlayer.startFullScreen()
    }

    /**
     * 退出全屏播放
     */
    fun quitFullScreen() {
        if (null != videoPlayer) videoPlayer.quitFullScreen()
    }

    /**
     * 开启\退出全屏播放
     */
    fun toggleFullScreen() {
        if (null != videoPlayer) videoPlayer.toggleFullScreen()
    }

    /**
     * 镜像、取消镜像
     */
    fun toggleMirror(): Boolean {
        if (null != videoPlayer) {
            return videoPlayer.toggleMirror()
        }
        return false
    }

    val isPlaying: Boolean
        /**
         * 播放器是否正在播放中
         * @return 是否正处于播放中(准备\开始播放\播放中\缓冲\) true:播放中 false:不处于播放中状态
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.isPlaying()
            }
            return false
        }

    val isWorking: Boolean
        /**
         * 播放器是否正在工作中
         * @return 播放器是否正处于工作状态(准备\开始播放\缓冲\手动暂停\生命周期暂停) true:工作中 false:空闲状态
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.isWorking()
            }
            return false
        }

    val isSoundMute: Boolean
        /**
         * 是否开启了静音
         * @return true:已开启静音 false:系统音量
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.isSoundMute()
            }
            return false
        }

    /**
     * 设置\取消静音
     * @param soundMute true:静音 false:系统音量
     * @return true:已开启静音 false:系统音量
     */
    fun setSoundMute(soundMute: Boolean): Boolean {
        if (null != videoPlayer) {
            return videoPlayer.setSoundMute(soundMute)
        }
        return false
    }

    /**
     * 静音、取消静音
     */
    fun toggleMute(): Boolean {
        if (null != videoPlayer) {
            return videoPlayer.toggleMute()
        }
        return false
    }

    val videoWidth: Int
        /**
         * 返回视频分辨率-宽
         * @return 单位：像素
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.getVideoWidth()
            }
            return 0
        }

    val videoHeight: Int
        /**
         * 返回视频分辨率-高
         * @return 单位：像素
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.getVideoHeight()
            }
            return 0
        }

    /**
     * 试看模式下调用此方法结束播放
     */
    fun onCompletion() {
        if (null != videoPlayer) videoPlayer.onCompletion()
    }

    val duration: Long
        /**
         * 返回视频文件总时长
         * @return 单位：毫秒
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.getDuration()
            }
            return 0
        }

    val currentPosition: Long
        /**
         * 返回正在播放的位置
         * @return 单位：毫秒
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.getCurrentPosition()
            }
            return 0
        }

    val buffer: Int
        /**
         * 返回当前视频缓冲进度
         * @return 单位：百分比
         */
        get() {
            if (null != videoPlayer) {
                return videoPlayer.getBuffer()
            }
            return 0
        }

    /**
     * 快进\快退
     * @param msec 毫秒进度条
     */
    fun seekTo(msec: Long) {
        if (null != videoPlayer) videoPlayer.seekTo(msec)
    }
}