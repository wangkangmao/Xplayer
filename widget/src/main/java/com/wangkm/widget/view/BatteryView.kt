package com.wangkm.widget.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.wangkm.widget.R
import com.wangkm.xplayer.utils.PlayerUtils

/**
 * created by wangkm
 * Desc:电池电量、实时时间显示控件
 */
class BatteryView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {
    private var mBatteryReceiver: BatteryReceiver? = null
    private val mBatteryText: TextView?
    private var mBatteryTime: TextView?
    private val mBatteryStatus: ImageView?

    init {
        inflate(context, R.layout.player_battery_view, this)
        mBatteryText = findViewById<View>(R.id.battery_text) as TextView
        mBatteryTime = findViewById<View>(R.id.battery_time) as TextView
        mBatteryStatus = findViewById<View>(R.id.battery_status) as ImageView
        updateSystemTime()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (null == mBatteryReceiver) {
            mBatteryReceiver = BatteryReceiver()
            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED) //电池电量变化、充电状态
            intentFilter.addAction(Intent.ACTION_TIME_TICK) //系统时间每分钟变化
            intentFilter.addAction(Intent.ACTION_TIME_CHANGED) //手动改变系统时间变化
            context.registerReceiver(mBatteryReceiver, intentFilter)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (null != mBatteryReceiver) {
            context.unregisterReceiver(mBatteryReceiver)
            mBatteryReceiver = null
        }
    }

    /**
     * 是否显示系统时间交互组件
     * @param showTime true:显示系统时间 false:不显示
     */
    fun showTime(showTime: Boolean) {
        if (!showTime) {
            if (null != mBatteryTime) {
                mBatteryTime!!.visibility = GONE
                mBatteryTime = null
            }
        }
    }

    /**
     * 更新系统时间
     */
    private fun updateSystemTime() {
        if (null != mBatteryTime) {
            mBatteryTime!!.text = PlayerUtils.currentTimeStr
        }
    }

    private inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //电池电量、充电状态变化
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                val extras = intent.extras
                if (null != mBatteryText && null != extras) {
                    //获取剩余电量
                    val current = extras.getInt("level") // 获得当前电量
                    val total = extras.getInt("scale") // 获得总电量
                    val percent = current * 100 / total
                    mBatteryText.text = "$percent%"
                    //检查充电状态
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    if (null != mBatteryStatus) mBatteryStatus.visibility = if (isCharging) VISIBLE else INVISIBLE
                }
                //系统时间每分钟变化\手动改变系统时间变化
            } else if (Intent.ACTION_TIME_TICK == intent.action || Intent.ACTION_TIME_CHANGED == intent.action) {
                updateSystemTime()
            }
        }
    }
}