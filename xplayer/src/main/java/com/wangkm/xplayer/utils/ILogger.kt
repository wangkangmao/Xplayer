package com.wangkm.xplayer.utils

import android.util.Log


/**
 * created by wangkm
 * Desc:
 */
object ILogger {
    var DEBUG: Boolean = true

    fun setDebug(debug: Boolean) {
        DEBUG = debug
    }

    val version: String
        get() = "1.0.0" // 直接返回版本号，避免BuildConfig引用

    fun pd(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.println(Log.DEBUG, TAG, message!!)
        }
    }

    fun pe(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.println(Log.ERROR, TAG, message!!)
        }
    }

    fun pw(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.println(Log.WARN, TAG, message!!)
        }
    }

    fun pi(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.println(Log.INFO, TAG, message!!)
        }
    }

    fun d(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.d(TAG, message!!)
        }
    }

    fun e(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.e(TAG, message!!)
        }
    }

    fun v(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.e(TAG, message!!)
        }
    }

    fun w(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.w(TAG, message!!)
        }
    }

    fun i(TAG: String?, message: String?) {
        if (DEBUG) {
            Log.i(TAG, message!!)
        }
    }
}