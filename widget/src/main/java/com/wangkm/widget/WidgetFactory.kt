package com.wangkm.widget

import com.wangkm.widget.controls.ControWindowView
import com.wangkm.widget.controls.ControlCompletionView
import com.wangkm.widget.controls.ControlFunctionBarView
import com.wangkm.widget.controls.ControlGestureView
import com.wangkm.widget.controls.ControlLoadingView
import com.wangkm.widget.controls.ControlStatusView
import com.wangkm.widget.controls.ControlToolBarView
import com.wangkm.xplayer.base.BaseController
import com.wangkm.xplayer.interfaces.IVideoController

/**
 * created by wangkm
 * Desc:UI交互组件工厂，为方便使用SDK提供的默认UI交互组件，仍然支持一键使用默认UI交互组件的使用
 */
object WidgetFactory {
    /**
     * 绑定默认UI交互组件到控制器
     * @param controller 控制器
     * @param showBack 竖屏状态下是否显示返回按钮，true：显示，false：不显示
     * @param addWindowWidget 是否添加window窗口UI交互组件
     */
    /**
     * 绑定默认UI交互组件到控制器
     * @param controller 控制器
     */
    @JvmOverloads
    fun bindDefaultControls(controller: BaseController?, showBack: Boolean = false, addWindowWidget: Boolean = false) {
        if (null != controller) {
            //顶部标题栏
            val toolBarView = ControlToolBarView(controller.context)
            toolBarView.target = IVideoController.TARGET_CONTROL_TOOL
            toolBarView.showBack(showBack)
            //底部播放时间进度、progressBar、seekBae、静音、全屏等功能栏
            val functionBarView = ControlFunctionBarView(controller.context)
            functionBarView.target = IVideoController.TARGET_CONTROL_FUNCTION
            //手势控制屏幕亮度、系统音量、快进、快退UI交互
            val gestureView = ControlGestureView(controller.context)
            gestureView.target = IVideoController.TARGET_CONTROL_GESTURE
            //播放完成、重试
            val completionView = ControlCompletionView(controller.context)
            completionView.target = IVideoController.TARGET_CONTROL_COMPLETION
            //移动网络播放提示、播放失败、试看完成
            val statusView = ControlStatusView(controller.context)
            statusView.target = IVideoController.TARGET_CONTROL_STATUS
            //加载中、开始播放
            val loadingView = ControlLoadingView(controller.context)
            loadingView.target = IVideoController.TARGET_CONTROL_LOADING
            //悬浮窗窗口播放器的窗口样式
            if (addWindowWidget) {
                val windowView = ControWindowView(controller.context)
                windowView.target = IVideoController.TARGET_CONTROL_WINDOW
                //将所有UI组件添加到控制器
                controller.addControllerWidget(
                    toolBarView,
                    functionBarView,
                    gestureView,
                    completionView,
                    statusView,
                    loadingView,
                    windowView
                )
            } else {
                //将所有UI组件添加到控制器
                controller.addControllerWidget(toolBarView, functionBarView, gestureView, completionView, statusView, loadingView)
            }
        }
    }
}