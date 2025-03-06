package com.wangkm.cache.task

import android.util.Log
import com.wangkm.cache.VideoCache
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService

/**
 * created by wangkm
 * Desc:预缓存Task
 * 原理：主动去请求VideoCache生成的代理地址，触发VideoCache缓存机制
 * 缓存到 PreloadManager.PRELOAD_LENGTH 的数据之后停止请求，完成预加载
 * 播放器去播放VideoCache生成的代理地址的时候，VideoCache会直接返回缓存数据，
 * 从而提升播放速度
 */
class PreloadTask : Runnable {
    //原始视频地址
    var rawUrl: String? = null
        private set

    //列表中的位置,如果是非列表场景，则是-1
    var position: Int = -1
        private set

    //是否被取消
    private var mIsCanceled = false

    //是否正在预加载
    private var mIsExecuted = false

    //预缓存大小
    private var mPreloadLength = 1024 * 1024

    private constructor()

    @JvmOverloads
    constructor(rawUrl: String?, position: Int = -1) {
        this.rawUrl = rawUrl
        this.position = position
    }

    constructor(rawUrl: String?, position: Int, preloadLength: Int) {
        this.rawUrl = rawUrl
        this.position = position
        this.mPreloadLength = preloadLength
    }

    override fun run() {
        if (!mIsCanceled) {
            start()
        }
        mIsExecuted = false
        mIsCanceled = false
    }

    /**
     * 开始预加载
     */
    private fun start() {
        // 如果在小黑屋里不加载
        if (sBlackList.contains(rawUrl)) return
        //        Log.d(TAG,"start-->mPosition:"+mPosition);
        var connection: HttpURLConnection? = null
        try {
            //获取HttpProxyCacheServer的代理地址
            val proxyUrl = VideoCache.getInstance().proxy.getProxyUrl(rawUrl)
            val url = URL(proxyUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection!!.readTimeout = 5000
            val `in`: InputStream = BufferedInputStream(connection.inputStream)
            var length: Int
            var read = -1
            val bytes = ByteArray(8 * 1024)
            while ((`in`.read(bytes).also { length = it }) != -1) {
                read += length
                //预加载完成或者取消预加载
                if (mIsCanceled || read >= mPreloadLength) {
                    if (mIsCanceled) {
                        Log.d(TAG, "预缓存取消:position:" + position + ",Byte:" + read)
                    } else {
                        Log.d(TAG, "预缓存成功:position:" + position + ",Byte:" + read)
                    }
                    break
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Log.e(TAG, "预缓存异常:position:" + position + ",error:" + e.message)
            // 关入小黑屋
            sBlackList.add(rawUrl)
        } finally {
            connection?.disconnect()
            Log.d(TAG, "预缓存结束:position:" + position)
        }
    }

    /**
     * 将预加载任务提交到线程池，准备执行
     */
    fun executeOn(executorService: ExecutorService) {
        if (mIsExecuted) return
        mIsExecuted = true
        executorService.submit(this)
    }

    /**
     * 取消预加载任务
     */
    fun cancel() {
        if (mIsExecuted) {
            mIsCanceled = true
        }
    }

    companion object {
        private const val TAG = "PreloadTask"

        //暂停池子
        private val sBlackList: MutableList<String?> = ArrayList()
    }
}
