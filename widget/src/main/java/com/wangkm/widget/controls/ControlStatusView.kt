package com.wangkm.widget.controls

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import android.widget.TextView
import com.wangkm.widget.R
import com.wangkm.xplayer.base.BaseControlWidget
import com.wangkm.xplayer.manager.IVideoManager
import com.wangkm.xplayer.model.PlayerState
import com.wangkm.xplayer.utils.PlayerUtils

/**
 * created by wangkm
 * Desc:播放器试看结束\移动网络提示\播放失败提示
 */
class ControlStatusView : BaseControlWidget {
    private var mScene = 0

    constructor(context: Context?) : super(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun getLayoutId(): Int {
        return R.layout.player_control_status
    }

    override fun initViews() {
        hide()
        val btnContinue = findViewById<View>(R.id.player_status_btn)
        PlayerUtils.setOutlineProvider(findViewById(R.id.player_status_btn), PlayerUtils.dpToPxInt(18f).toFloat())
        btnContinue.setOnClickListener(OnClickListener { //点击事件优先回调给监听器处理
            if (null != mOnStatusListener) {
                mOnStatusListener!!.onEvent(mScene)
                return@OnClickListener
            }
            if (null != mControlWrapper) {
                when (mScene) {
                    SCENE_MOBILE -> {
                        IVideoManager.setMobileNetwork(true)
                        mControlWrapper.togglePlay()
                    }

                    SCENE_COMPLETION -> mControlWrapper.onCompletion()
                    SCENE_ERROR -> mControlWrapper.togglePlay()
                }
            }
        })
    }

    /**
     * 改变场景
     * @param scene 0:移动网络播放提示 1:试看结束 2:播放失败
     */
    fun setScene(scene: Int) {
        setScene(scene, null)
    }

    /**
     * 改变场景
     * @param scene 1:移动网络播放提示 2:试看结束 3:播放失败
     * @param tipsStr 提示文字
     */
    fun setScene(scene: Int, tipsStr: String?) {
        setScene(scene, tipsStr, null)
    }

    /**
     * 改变场景
     * @param scene 1:移动网络播放提示 2:试看结束 3:播放失败
     * @param tipsStr 提示文字
     * @param btnStr 按钮文字
     */
    fun setScene(scene: Int, tipsStr: String?, btnStr: String?) {
        this.mScene = scene
        val tips = findViewById<View>(R.id.player_status_tips) as TextView
        val btn = findViewById<View>(R.id.player_status_btn) as TextView
        tips.text = PlayerUtils.formatHtml(getTipsStr(scene, tipsStr))
        btn.text = PlayerUtils.formatHtml(getBtnStr(scene, btnStr))
    }

    /**
     * 返回按钮文案
     * @param scene
     * @param btnStr
     * @return
     */
    private fun getBtnStr(scene: Int, btnStr: String?): String? {
        if (!TextUtils.isEmpty(btnStr)) return btnStr
        when (scene) {
            SCENE_MOBILE -> return context.resources.getString(R.string.player_btn_continue_play)
            SCENE_COMPLETION -> return context.resources.getString(R.string.player_btn_yes)
            SCENE_ERROR -> return context.resources.getString(R.string.player_btn_try)
        }
        return context.resources.getString(R.string.player_btn_unknown)
    }

    /**
     * 返回提示文案
     * @param scene
     * @param tipsStr
     * @return
     */
    private fun getTipsStr(scene: Int, tipsStr: String?): String? {
        if (!TextUtils.isEmpty(tipsStr)) return tipsStr
        when (scene) {
            SCENE_MOBILE -> return context.resources.getString(R.string.player_tips_mobile)
            SCENE_COMPLETION -> return context.resources.getString(R.string.player_tips_preview_finish)
            SCENE_ERROR -> return context.resources.getString(R.string.player_tips_play_error)
        }
        return context.resources.getString(R.string.player_tips_unknown)
    }

    /**
     * 设置场景类型以改变交互样式来适应窗口模式
     * @param sceneType 0:默认的视频控制器场景 1:窗口模式场景
     */
    fun setSceneType(sceneType: Int) {
        val textSize14 = PlayerUtils.dpToPxInt(14f)
        val textSize13 = PlayerUtils.dpToPxInt(13f)
        val textSize16 = PlayerUtils.dpToPxInt(16f)
        val tips = findViewById<View>(R.id.player_status_tips) as TextView
        tips.setTextSize(TypedValue.COMPLEX_UNIT_PX, (if (1 == sceneType) textSize14 else textSize16).toFloat())

        val paddingLeft12 = PlayerUtils.dpToPxInt(12f)
        val paddingLeft22 = PlayerUtils.dpToPxInt(22f)
        val btn = findViewById<View>(R.id.player_status_btn) as TextView
        btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, (if (1 == sceneType) textSize13 else textSize16).toFloat())
        btn.setPadding(if (1 == sceneType) paddingLeft12 else paddingLeft22, 0, if (1 == sceneType) paddingLeft12 else paddingLeft22, 0)
        val layoutParams = btn.layoutParams as LinearLayout.LayoutParams
        layoutParams.height = PlayerUtils.dpToPxInt(if (1 == sceneType) 26f else 36f)
        layoutParams.setMargins(0, PlayerUtils.dpToPxInt(if (1 == sceneType) 15f else 20f), 0, 0)
        btn.layoutParams = layoutParams
        PlayerUtils.setOutlineProvider(btn, PlayerUtils.dpToPxInt(if (1 == sceneType) 13f else 18f).toFloat())
    }

    override fun show() {
        if (visibility != VISIBLE) {
            visibility = VISIBLE
        }
    }

    override fun hide() {
        if (visibility != GONE) {
            visibility = GONE
        }
    }

    override fun onPlayerState(state: PlayerState, message: String) {
        when (state) {
            PlayerState.STATE_COMPLETION -> if (isPreViewScene) { //预览场景
                show()
                setScene(SCENE_COMPLETION)
            } else {
                hide()
            }

            PlayerState.STATE_MOBILE -> if (!isWindowScene) {
                show()
                setScene(SCENE_MOBILE)
            }

            PlayerState.STATE_ERROR -> if (!isWindowScene) {
                setScene(SCENE_ERROR, message)
                show()
            }

            else -> hide()
        }
    }

    override fun onOrientation(direction: Int) {}

    /**
     * 适配常规场景和窗口场景样式
     * @param playerScene
     */
    override fun onPlayerScene(playerScene: Int) {
//        if(IControllerView.SCENE_GLOBAL_WINDOW==scene||IControllerView.SCENE_WINDOW==scene){
//            setSceneType(1);
//        }else{
//            setSceneType(0);
//        }
    }

    interface OnStatusListener {
        fun onEvent(event: Int)
    }

    private var mOnStatusListener: OnStatusListener? = null

    fun setOnStatusListener(onStatusListener: OnStatusListener?) {
        mOnStatusListener = onStatusListener
    }

    companion object {
        const val SCENE_MOBILE: Int = 1 //移动网络播放提示
        const val SCENE_COMPLETION: Int = 2 //试看结束
        const val SCENE_ERROR: Int = 3 //播放失败
    }
}