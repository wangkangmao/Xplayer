package com.wangkm.player.utils

import android.util.Log
import com.wangkm.player.BuildConfig

/**
 * created by wangkm
 * Desc:
 */
object Logger {
    fun pd(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.println(Log.DEBUG, TAG, message!!)
        }
    }

    fun pe(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.println(Log.ERROR, TAG, message!!)
        }
    }

    fun pw(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.println(Log.WARN, TAG, message!!)
        }
    }

    fun pi(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.println(Log.INFO, TAG, message!!)
        }
    }

    fun d(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message!!)
        }
    }

    fun e(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message!!)
        }
    }

    fun v(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message!!)
        }
    }

    fun w(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message!!)
        }
    }

    fun i(TAG: String?, message: String?) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message!!)
        }
    }
}