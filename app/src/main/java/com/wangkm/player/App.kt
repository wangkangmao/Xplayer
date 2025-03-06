package com.wangkm.player

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.wangkm.player.utils.SharedPreferencesUtil
import com.wangkm.xplayer.manager.IVideoManager
import com.wangkm.xplayer.utils.ILogger

/**
 * created by wangkm
 * Desc:
 */
class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SharedPreferencesUtil.init(this, "$packageName.sp", MODE_MULTI_PROCESS)
        ILogger.DEBUG = true
        //设置播放器是否拦截音频焦点丢失事件,如果设置了检测到音频焦点丢失会自动暂停播放
        IVideoManager.setInterceptTAudioFocus(true)

        /**
         * SDK内部会在使用缓存相关功能时自动初始化。如果需要自行定义缓存目录、缓存目录最大长度大小可自行调用初始化。必须在使用缓存功能之前初始化
         */
        //返回的路径是SD卡包名下的内部缓存路径，无需存储权限。位于/storage/emulated/0/Android/data/包名/files/video/cache下，会随着应用卸载被删除
        //其它路径请注意申请动态权限！！！
//        File cachePath = getExternalFilesDir("video/cache/");
        //参数2：缓存大小(单位：字节),参数3：缓存路径,不设置默认在sd_card/Android/data/[app_package_name]/cache中
//        VideoCache.getInstance().initCache(getApplicationContext(),1024*1024*1024,cachePath);//缓存大小为1024M，路径为SD卡下的cachePath。请注意SD卡权限状态。
    }



    companion object {
        private const val TAG = "App"
        val context: Context
            get() = instance!!.applicationContext
        var instance: App? = null
            private set
    }
}