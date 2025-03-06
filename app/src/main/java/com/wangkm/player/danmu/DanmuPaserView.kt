package com.wangkm.player.danmu

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.wangkm.player.R
import com.wangkm.xplayer.utils.PlayerUtils
import com.wangkm.xplayer.utils.PlayerUtils.dpToPxInt
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.controller.IDanmakuView
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import java.net.URLDecoder
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * created by wangkm
 * Desc:弹幕解析
 */
class DanmuPaserView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {
    private var mParser: DanmuParser? = null //解析器对象
    private var mDanmakuView: IDanmakuView? = null
    private var mDanmakuContext: DanmakuContext? = null
    private var mQueue: ConcurrentLinkedQueue<String>? = null
    private var isRuning = false

    /**
     * 弹幕是否已经初始化了
     * @return
     */
    var isInit: Boolean = false
        private set

    /**
     * 弹幕的消息处理
     */
    private val mDanmakuHandler: Handler? = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                WHAT_DISPLAY_SINGLE_DANMAKU -> {
                    this.removeMessages(WHAT_DISPLAY_SINGLE_DANMAKU)
                    displayDanmaku()
                }
            }
        }
    }

    init {
        inflate(context, R.layout.view_danmu_paser_layout, this)
    }

    /**
     * 自动任务
     */
    private fun displayDanmaku() {
        if (null != mDanmakuView) {
            isRuning = true
            val paused = mDanmakuView!!.isPaused
            //            VideoLogger.d(TAG,"displayDanmaku,paused:"+paused);
            //如果当前的弹幕由于Android生命周期的原因进入暂停状态，那么不应该不停的消耗弹幕数据
            //要知道，在这里发出一个handler消息，那么将会消费（删掉）ConcurrentLinkedQueue头部的数据
            if (null != mQueue && !paused) {
                val poll = mQueue!!.poll()
                if (!TextUtils.isEmpty(poll)) {
                    addDanmaku(poll, true) //从弹幕池中取出一条弹幕添加至屏幕中
                }
            }
            mDanmakuHandler!!.sendEmptyMessageDelayed(WHAT_DISPLAY_SINGLE_DANMAKU, DANMU_ADD_DURTAION) //1秒钟添加一条留言至字幕中
        }
    }

    /**
     * 添加一条自己发布的留言到字幕上
     * @param comment
     */
    private fun addInputDanmaku(comment: String) {
//        VideoLogger.d(TAG,"addInputDanmaku-->comment"+comment);
        val danmaku = danmaku
        if (null != danmaku) {
            try {
                val decode = URLDecoder.decode(comment, "utf-8")
                danmaku.text = decode
                danmaku.padding = 6
                danmaku.priority = 2 // 一定会显示, 一般用于本机发送的弹幕
                danmaku.isLive = true
                danmaku.time = mDanmakuView!!.currentTime //立即显示
                danmaku.textSize = dpToPxInt(16f).toFloat() //文本弹幕字体大小
                danmaku.textColor = Color.parseColor("#FF5000")
                mDanmakuView!!.addDanmaku(danmaku) //调用这个方法，添加字幕到控件，开始滚动
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 添加一条字幕到控件
     * @param cs
     * @param islive
     */
    private fun addDanmaku(cs: CharSequence, islive: Boolean) {
//        VideoLogger.d(TAG,"addDanmaku-->cs"+cs);
        if (TextUtils.isEmpty(cs)) return
        val danmaku = danmaku
        if (null != danmaku) {
            try {
                danmaku.text = cs
                danmaku.padding = 6
                danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
                danmaku.isLive = islive
                danmaku.time = mDanmakuView!!.currentTime + 2000 //多长时间后加入弹幕组合
                danmaku.textSize = PlayerUtils.dpToPxInt(16f).toFloat() //文本弹幕字体大小
                danmaku.textColor = Color.parseColor("#E6FFFFFF") //文本的颜色
                danmaku.textShadowColor = Color.parseColor("#80000000") //文本弹幕描边的颜色
                //danmaku.underlineColor = Color.DKGRAY; //文本弹幕下划线的颜色
                //danmaku.borderColor = Color.parseColor("#80000000"); //边框的颜色
                mDanmakuView!!.addDanmaku(danmaku) //调用这个方法，添加字幕到控件，开始滚动
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private val danmaku: BaseDanmaku?
        /**
         * 返回弹幕的可用状态
         * @return
         */
        get() {
//        VideoLogger.d(TAG,"getDanmaku");
            if (null == mDanmakuView) return null
            if (null == mParser) {
                mParser = DanmuParser()
            }
            if (null == mDanmakuContext) {
                mDanmakuContext = DanmakuContext.create()
            }
            val danmaku = mDanmakuContext!!.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
            return danmaku
        }

    /**
     * 初始化弹幕和设置并自动开始滚动显示
     */
    fun initDanmaku() {
//        VideoLogger.d(TAG,"initDanmaku-->init:"+isInit);
        if (null != mDanmakuView && isInit) {
//            VideoLogger.d(TAG,"initDanmaku-->已初始化,丢弃重复动作");
            return
        }
        //实例化
        mDanmakuView = findViewById<View>(R.id.sv_danmaku) as IDanmakuView
        mDanmakuView!!.show()
        if (null == mQueue) {
            mQueue = ConcurrentLinkedQueue()
        }
        //        VideoLogger.d(TAG,"initDanmaku-->mQueue:"+mQueue.size());
        if (null == mDanmakuContext) {
            mDanmakuContext = DanmakuContext.create()
        }
        // 设置滚动方向、最大显示行数
        val maxLinesPair = HashMap<Int, Int>()
        maxLinesPair[BaseDanmaku.TYPE_SCROLL_RL] = 3 // 设置从右向左滚动，最大同时显示2行
        // 设置是否禁止重叠
        val overlappingEnablePair = HashMap<Int, Boolean>()
        overlappingEnablePair[BaseDanmaku.TYPE_SCROLL_RL] = true
        overlappingEnablePair[BaseDanmaku.TYPE_FIX_TOP] = true

        //        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, false);//允许X坐标重叠
//        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        //普通文本弹幕描边设置样式
        mDanmakuContext!!.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f) //描边的厚度
            .setDuplicateMergingEnabled(false) //如果是图文混合编排编排，最后不要描边
            .setScrollSpeedFactor(2.2f) //弹幕的速度。注意！此值越小，速度越快！值越大，速度越慢。// by phil
            .setScaleTextSize(1.0f) //缩放的值
            //                    .setCacheStuffer(new BackgroundCacheStuffer(),)  // 绘制背景使用BackgroundCacheStuffer
            .setMaximumLines(maxLinesPair)
            .preventOverlapping(overlappingEnablePair)
        if (null == mParser) {
            mParser = DanmuParser()
        }
        mDanmakuView!!.setCallback(object : DrawHandler.Callback {
            override fun updateTimer(timer: DanmakuTimer) {
//                VideoLogger.d(TAG,"updateTimer:"+timer.currMillisecond);
            }

            override fun drawingFinished() {
//                VideoLogger.d(TAG,"drawingFinished");
            }

            override fun danmakuShown(danmaku: BaseDanmaku) {
//                VideoLogger.d(TAG,"danmakuShown:"+danmaku.obj);
            }

            override fun prepared() { //准备完成了
//                VideoLogger.d(TAG,"prepared");
                isInit = true
                if (null != mDanmakuView) {
                    mDanmakuView!!.start()
                }
            }
        })
        mDanmakuView!!.enableDanmakuDrawingCache(true) //保存绘制的缓存
        mDanmakuView!!.showFPS(false) //显示实时帧率，调试模式下开启
        mDanmakuView!!.prepare(mParser, mDanmakuContext)
    }

    /**
     * 添加弹幕数据
     * @param comments
     */
    fun addDanmuContent(comments: List<String>?) {
//        VideoLogger.d(TAG,"addDanmuContent");
        if (null == mQueue) {
            mQueue = ConcurrentLinkedQueue()
        }
        if (null != comments && comments.size > 0) {
            mQueue!!.addAll(comments)
        }
        //        VideoLogger.d(TAG,"addDanmuContent-->mQueue:"+mQueue.size());
    }

    /**
     * 添加单条弹幕数据
     * @param content 弹幕文本内容
     * @param isOneself 是否是自己发送的
     */
    fun addDanmuItem(content: String, isOneself: Boolean) {
        if (isOneself) {
            addInputDanmaku(content)
        } else {
            addDanmaku(content, false)
        }
    }

    fun onResume() {
//        VideoLogger.d(TAG,"onResume");
        if (null != mDanmakuView) {
            if (!isRuning) displayDanmaku()
            mDanmakuView!!.resume()
        }
    }

    fun onPause() {
//        VideoLogger.d(TAG,"onPause");
        if (null != mDanmakuView) {
            mDanmakuView!!.pause()
        }
    }

    fun onReset() {
//        VideoLogger.d(TAG,"onReset");
        releaseDanmaku()
    }

    /**
     * 销毁和释放弹幕相关所有资源资源
     */
    fun releaseDanmaku() {
//        VideoLogger.d(TAG,"releaseDanmaku");
        if (null != mDanmakuView) {
            if (null != mDanmakuHandler) {
                mDanmakuHandler.removeMessages(WHAT_DISPLAY_SINGLE_DANMAKU)
                mDanmakuHandler.removeCallbacksAndMessages(null)
            }
            if (mDanmakuView!!.isPrepared) {
                mDanmakuView!!.stop() //停止弹幕
            }
            mDanmakuView!!.stop()
            mDanmakuView!!.removeAllDanmakus(true)
            mDanmakuView!!.release() //释放弹幕资源
            if (null != mQueue) mQueue!!.clear()
            if (null != mParser) mParser!!.release()
            mDanmakuView!!.clearDanmakusOnScreen()
            mQueue = null
            mParser = null
        }
        isInit = false
    }

    fun onDestroy() {
//        VideoLogger.d(TAG,"onDestroy");
        releaseDanmaku()
        mDanmakuView = null
    }

    companion object {
        private const val TAG = "DanmuPaserView"
        private const val WHAT_DISPLAY_SINGLE_DANMAKU = 100
        private const val DANMU_ADD_DURTAION: Long = 1200 //添加留言道弹幕间隔时长
    }
}