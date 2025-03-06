package com.wangkm.xplayer.widget

import android.content.Context
import android.util.AttributeSet
import com.wangkm.xplayer.base.BasePlayer
import com.wangkm.xplayer.controller.VideoController

/**
 * created by wangkm
 * desc:这是一个播放器的实现类，内部封装了如何使用SDK提供的交互组件的调用示例
 * 1、需要功能自定义，请复写父类的方法修改
 * 2、此播放器提供使用默认控制器作为播放器控制器方法，请调用：[.initController]
 * 3、如需使用默认UI交互，需集成implementation 'com.github.hty527.iPlayer:widget:lastversion'后，使用WidgetFactory.bindDefaultControls(controller);将UI交互组件绑定到控制器。
 */
class VideoPlayer : BasePlayer {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initViews() {}

    /**
     * 绑定默认的控制器到播放器
     * @return
     */
    fun initController(): VideoController {
        val controller = VideoController(context)
        setController(controller)
        return controller
    }
}