package com.wangkm.player.video.contract;


import com.wangkm.player.base.BaseContract;
import com.wangkm.player.video.bean.OpenEyesIndexItemBean;

import java.util.List;

/**
 * created by wangkm
 * Index Video Contract
 */

public interface VideoListContract {

    interface View extends BaseContract.BaseView{

        /**
         * 显示视频列表
         * @param data 视频列表
         * @param isRestart 是否从第一页开始加载的
         */
        void showVideos(List<OpenEyesIndexItemBean> data, boolean isRestart);
    }

    interface Presenter<V> extends BaseContract.BasePresenter<V>{

        /**
         * 获取主页的视频列表
         * @param isRestart 是否重新开始？
         */
        void getIndexVideos(boolean isRestart);

        /**
         * 根据URL获取视频列表
         * @param url url
         * @param isRestart 是否从第一页开始加载的
         */
        void getVideosByUrl(String url, boolean isRestart);

        /**
         * 根据视频ID获取推荐列表
         * @param videoID 视频ID
         */
        void getVideosByVideo(String videoID);
    }
}