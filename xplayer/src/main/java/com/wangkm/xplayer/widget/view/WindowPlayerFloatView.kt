package com.wangkm.xplayer.widget.view

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import ccom.wangkm.xplayer.R
import com.wangkm.xplayer.base.BasePlayer
import com.wangkm.xplayer.listener.OnWindowActionListener
import com.wangkm.xplayer.utils.PlayerUtils
import kotlin.math.abs
import kotlin.math.max

/**
 * created by wangkm
 * Desc:Activity窗口和全局悬浮窗窗口播放器的容器包装，处理了手势操作
 * 1、解决了Activity级别和全局悬浮窗级别的窗口手势冲突
 * 2、当前View范围内拦截了ACTION_MOVE事件，点击事件不拦截
 * 3、内部根据activity window窗口和全局的悬浮窗窗口做了区别处理
 * 4、用户松手后自动吸附至屏幕最近的X轴边缘,距离边缘12dp位置悬停
 */
class WindowPlayerFloatView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(
    context!!, attrs, defStyleAttr
) {
    private var mGroupWidth = 0
    private var mGroupHeight = 0

    //手指按下此View在屏幕中X、Y坐标,偏移量X,Y
    private var xInView = 0f
    private var yInView = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var mPlayerViewGroup: ViewGroup? = null //Activity内的窗口模式下播放器父容器手势拖拽目标View
    var basePlayer: BasePlayer? = null //当全局悬浮窗启用时,此播放器实例不为空
        private set
    private var mStatusBarHeight = 0
    private var mHorMargin = 0
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mOldToPixelX = 0 //状态栏高度,吸附至屏幕边缘的边距,屏幕宽,屏幕高,实时的上一次平移偏移量X轴像素点
    private var isAutoSorption = false //是否自动吸附

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec) //如果是继承的viewgroup比如linearlayout时，可以先计算
        var widthResult = 0
        //view根据xml中layout_width和layout_height测量出对应的宽度和高度值，
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        when (widthSpecMode) {
            MeasureSpec.UNSPECIFIED -> widthResult = widthSpecSize
            MeasureSpec.AT_MOST -> widthResult = contentWidth
            MeasureSpec.EXACTLY ->                 //当xml布局中是准确的值，比如200dp是，判断一下当前view的宽度和准确值,取两个中大的，这样的好处是当view的宽度本事超过准确值不会出界
                widthResult = max(contentWidth.toDouble(), widthSpecSize.toDouble()).toInt()
        }
        var heightResult = 0
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        when (heightSpecMode) {
            MeasureSpec.UNSPECIFIED -> heightResult = heightSpecSize
            MeasureSpec.AT_MOST -> heightResult = contentHeight
            MeasureSpec.EXACTLY -> heightResult = max(contentHeight.toDouble(), heightSpecSize.toDouble())
                .toInt()
        }
        this.mGroupWidth = widthResult
        this.mGroupHeight = heightResult
        setMeasuredDimension(widthResult, heightResult) //测量宽度和高度
    }

    private val contentWidth: Int
        get() {
            val contentWidth = (width + paddingLeft + paddingRight).toFloat()
            return contentWidth.toInt()
        }

    private val parentViewHeight: Int
        get() = if (null != mPlayerViewGroup) mPlayerViewGroup!!.height else height

    private val parentViewWidth: Int
        get() = if (null != mPlayerViewGroup) mPlayerViewGroup!!.width else width

    private val contentHeight: Int
        get() {
            val contentHeight = (height + paddingTop + paddingBottom).toFloat()
            return contentHeight.toInt()
        }

    private val statusBarHeight: Float
        get() {
            if (mStatusBarHeight == 0) {
                mStatusBarHeight = PlayerUtils.getStatusBarHeight(context)
            }
            return mStatusBarHeight.toFloat()
        }

    /**
     * 处理拦截事件,是否拦截滑动事件
     * @param e
     * @return true:拦截,播放器将收不到ACTION_MOVE事件 false:不拦截,播放器可接收并处理ACTION_MOVE事件
     */
    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        var intercepted = false
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                intercepted = false
                //记录手指按下时手在父View中的位置
                xInView = e.x
                yInView = e.y
                xDownInScreen = e.rawX
                yDownInScreen = e.rawY
                if (null != mPlayerViewGroup) {
                    translationX = mPlayerViewGroup!!.translationX
                    translationY = mPlayerViewGroup!!.translationY
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val absDeltaX = abs((e.rawX - xDownInScreen).toDouble()).toFloat()
                val absDeltaY = abs((e.rawY - yDownInScreen).toDouble()).toFloat()
                intercepted = absDeltaX > ViewConfiguration.get(context).scaledTouchSlop ||
                        absDeltaY > ViewConfiguration.get(context).scaledTouchSlop
            }
        }
        return intercepted
    }

    /**
     * 当onInterceptTouchEvent返回为true:时,这里能接收到MotionEvent.ACTION_MOVE和ACTION_DOWN事件和
     * 当onInterceptTouchEvent返回false:时,这里将收不到任何手势事件
     * @param e
     * @return
     */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_MOVE ->                 //activity内窗口
                if (null != mPlayerViewGroup) {
                    var toX = x + (e.x - xInView)
                    var toY = y + (e.y - yInView)
                    //                    ILogger.d(TAG,"onTouchEvent-->getX():"+getX()+",e.getX():"+e.getX()+",toX:"+toX+",toY:"+toY);
                    if (toX <= -translationX) { //屏幕最左侧
                        toX = -translationX
                    } else if (toX >= (mGroupWidth - (translationX + parentViewWidth))) { //屏幕最右侧
                        toX = (mGroupWidth - (translationX + parentViewWidth))
                    }
                    if (toY <= -translationY) { //屏幕最顶部
                        toY = -translationY
                    } else if (toY >= (mGroupHeight - (translationY + parentViewHeight))) { //屏幕最底部
                        toY = (mGroupHeight - (translationY + parentViewHeight))
                    }
                    setTranslationX(toX)
                    setTranslationY(toY)
                    //全局悬浮窗口
                } else {
                    if (null != mWindowActionListener) {
                        val xInScreen = e.rawX
                        val yInScreen = e.rawY - statusBarHeight
                        mWindowActionListener!!.onMovie(
                            (xInScreen - xInView).toInt().toFloat(),
                            (yInScreen - yInView).toInt().toFloat()
                        )
                    }
                }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> adsorptionDisplay()
        }
        return super.onTouchEvent(e)
    }

    /**
     * 检测是否需要自动吸附到屏幕边缘
     */
    private fun adsorptionDisplay() {
        if (!isAutoSorption) return
        this.mOldToPixelX = 0
        //        xInView=0;yInView=0;
        val locations = IntArray(2)
        if (null != mPlayerViewGroup) {
            mPlayerViewGroup!!.getLocationInWindow(locations) //Activity悬浮窗口
        } else {
            getLocationOnScreen(locations) //全局悬浮窗口
        }
        val centerX = locations[0] + (parentViewWidth / 2)
        scrollToPixel(locations[0], centerX, 200)
    }

    /**
     * 自动滚动并吸附至屏幕边缘
     * @param startX 窗口当前在屏幕的X点
     * @param centerX 播放器位于屏幕的X中心点
     * @param scrollDurtion 滚动时间，单位：毫秒
     */
    private fun scrollToPixel(startX: Int, centerX: Int, scrollDurtion: Long) {
        try {
            var isLeft = true
            var toPixelX = horMargin //初始的默认停靠在左侧15dp处
            if (centerX > (screenWidth / 2)) { //检测是否在屏幕右侧
                //右边停靠最大X：屏幕宽-自身宽-边距大小
                isLeft = false
                toPixelX = (screenWidth - parentViewWidth - horMargin)
            }
            if (scrollDurtion <= 0) {
                moveToX(startX, toPixelX, isLeft)
                return
            }
            //            ILogger.d(TAG,"scrollToPixel,startX:"+startX+",toPixelX:"+toPixelX+",centerX:"+centerX);
            @SuppressLint("ObjectAnimatorBinding") val objectAnimator = ObjectAnimator.ofInt(this, "number", startX, toPixelX)
            objectAnimator.setDuration(scrollDurtion)
            objectAnimator.interpolator = LinearInterpolator()
            val finalIsLeft = isLeft
            objectAnimator.addUpdateListener { valueAnimator ->
                val animatedValue = valueAnimator.animatedValue as Int
                moveToX(startX, animatedValue, finalIsLeft)
            }
            objectAnimator.start()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 移动至某个位于屏幕的x点
     * @param startX 起点,位于屏幕的x点
     * @param toPixelX 终点,位于屏幕的x点
     * @param isLeft 是否往左边吸附悬停,true:往左边吸附悬停,false:往右边吸附悬停
     */
    private fun moveToX(startX: Int, toPixelX: Int, isLeft: Boolean) {
        //Activity级别悬浮窗口
        if (null != mPlayerViewGroup) {
            //根据实时偏移量计算当前应当偏移多少像素点
            val translationX = getTranslationX()
            var toTranslationX = 0f
            //            ILogger.d(TAG,"moveToX-->translationX:"+translationX+",toPixelX:"+toPixelX+",startX:"+startX+",leLeft:"+isLeft);
            if (isLeft) { //往左边越来越小，也就是TranslationX偏移量越来越大
                val offset = if (0 == mOldToPixelX) startX - toPixelX else mOldToPixelX - toPixelX //本次往左边偏移量
                toTranslationX = translationX - offset //最终递减往左边偏移量
                //                ILogger.d(TAG,"moveToLeftX-->offset:"+offset+",toTranslationX:"+toTranslationX);
            } else {
                val offset = if (0 == mOldToPixelX) toPixelX - startX else toPixelX - mOldToPixelX
                toTranslationX = translationX + offset //最终累加往右边偏移量
                //                ILogger.d(TAG,"moveToRightX-->offset:"+offset+",toTranslationX:"+toTranslationX);
            }
            setTranslationX(toTranslationX)
            this.mOldToPixelX = toPixelX
        } else {
            //全局悬浮窗口，交给WindowManager更新位置
            if (null != mWindowActionListener) mWindowActionListener!!.onMovie(toPixelX.toFloat(), -1f)
        }
    }

    private val horMargin: Int
        get() {
            if (0 == mHorMargin) {
                mHorMargin = PlayerUtils.dpToPxInt(12f)
            }
            return mHorMargin
        }

    private val screenWidth: Int
        get() {
            if (0 == mScreenWidth) {
                mScreenWidth = PlayerUtils.getScreenWidth(context)
            }
            return mScreenWidth
        }

    private val screenHeight: Int
        get() {
            if (0 == mScreenHeight) {
                mScreenHeight = PlayerUtils.getScreenHeight(context)
            }
            return mScreenHeight
        }

    /**
     * Activity类型窗口调用--将窗口View添加到可推拽的容器中
     * @param basePlayer
     * @param width 窗口组件宽
     * @param height 窗口组件高
     * @param startX 窗口X轴起始位置
     * @param startY 窗口Y轴起始位置
     * @param radius 窗口的圆角 单位:像素
     * @param bgColor 窗口的背景颜色
     * @param isAutoSorption 触摸松手后是否自动吸附到屏幕边缘
     */
    fun addPlayerView(
        basePlayer: BasePlayer?,
        width: Int,
        height: Int,
        startX: Float,
        startY: Float,
        radius: Float,
        bgColor: Int,
        isAutoSorption: Boolean
    ) {
        if (null == basePlayer) return
        this.isAutoSorption = isAutoSorption
        //被移动的View宽高确定
        mPlayerViewGroup = findViewById(R.id.player_window_group)
        val layoutParams = mPlayerViewGroup?.layoutParams as LayoutParams
        layoutParams.width = width
        layoutParams.height = height
        mPlayerViewGroup?.layoutParams = layoutParams
        //将播放器添加到容器里
        val playerContainer = findViewById<View>(R.id.player_window_container) as FrameLayout
        playerContainer.addView(basePlayer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        //初始位置确定
        mPlayerViewGroup?.setX(startX)
        mPlayerViewGroup?.setY(startY)
        if (radius > 0) PlayerUtils.setOutlineProvider(mPlayerViewGroup!!, radius)
        if (bgColor != 0) mPlayerViewGroup?.setBackgroundColor(bgColor)

        setListener(basePlayer)
        //        adsorptionDisplay();//防止参数调用意外，处理自动吸附
    }

    /**
     * 全局悬浮窗调用--将窗口播放器添加到可推拽的容器中,选举悬浮窗窗口的宽高被WindowManager.LayoutParams约束
     * @param basePlayer
     * @param width 窗口组件宽
     * @param height 窗口组件高
     * @param radius 窗口的圆角 单位:像素
     * @param bgColor 窗口的背景颜色
     * @param isAutoSorption 触摸松手后是否自动吸附到屏幕边缘
     */
    fun addPlayerView(basePlayer: BasePlayer?, width: Int, height: Int, radius: Float, bgColor: Int, isAutoSorption: Boolean) {
        if (null == basePlayer) return
        this.isAutoSorption = isAutoSorption
        //将播放器添加到容器里
        val windowGroup = findViewById<FrameLayout>(R.id.player_window_group)
        val layoutParams = windowGroup.layoutParams as LayoutParams
        layoutParams.width = LayoutParams.MATCH_PARENT
        layoutParams.height = LayoutParams.MATCH_PARENT
        windowGroup.layoutParams = layoutParams
        val playerContainer = findViewById<View>(R.id.player_window_container) as FrameLayout
        playerContainer.addView(basePlayer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        if (radius > 0) PlayerUtils.setOutlineProvider(playerContainer, radius)
        if (bgColor != 0) playerContainer.setBackgroundColor(bgColor)

        setListener(basePlayer)
        //        adsorptionDisplay();//防止参数调用意外，处理自动吸附
    }

    /**
     * 设置监听器
     */
    private fun setListener(basePlayer: BasePlayer) {
        /**
         * 关闭事件,优先通知给开发者处理,如果开发者未监听则直接销毁播放器
         */
        findViewById<View>(R.id.player_window_close).setOnClickListener { if (null != mWindowActionListener) mWindowActionListener!!.onClose() }
        this.basePlayer = basePlayer
    }

    private var mWindowActionListener: OnWindowActionListener? = null

    init {
        inflate(context, R.layout.player_window_float, this)
    }

    fun setOnWindowActionListener(listener: OnWindowActionListener?) {
        mWindowActionListener = listener
    }

    /**
     * 恢复播放
     */
    fun onResume() {
        if (null != basePlayer) {
            basePlayer!!.onResume()
        }
    }

    /**
     * 暂停播放
     */
    fun onPause() {
        if (null != basePlayer) {
            basePlayer!!.onPause()
        }
    }

    /**
     * 悬浮窗关闭需要调用此接口
     */
    fun onReset() {
        if (null != basePlayer) {
            PlayerUtils.removeViewFromParent(basePlayer)
            basePlayer!!.onReset()
            basePlayer!!.onDestroy()
            basePlayer = null
        }
        xDownInScreen = 0f
        yDownInScreen = 0f
        xInView = 0f
        yInView = 0f
        mStatusBarHeight = 0
    }

    /**
     * 这里一定要处理,可能存在开发者会在没有主动或者被动关闭悬浮窗窗口播放器的时候来添加一个播放器到窗口,所以必须释放此前的窗口播放器
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        xDownInScreen = 0f
        yDownInScreen = 0f
        xInView = 0f
        yInView = 0f
        translationX = 0f
        translationY = 0f
        mStatusBarHeight = 0
        if (null != mPlayerViewGroup) mPlayerViewGroup!!.removeAllViews()
    }

    companion object {
        private const val TAG = "WindowPlayerFloatView"

        //手指按下X、Y坐标
        private var xDownInScreen = 0f
        private var yDownInScreen = 0f
    }
}