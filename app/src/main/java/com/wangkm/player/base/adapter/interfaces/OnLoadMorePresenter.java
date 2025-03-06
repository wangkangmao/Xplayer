package com.wangkm.player.base.adapter.interfaces;

/**
 * 2022/7/7
 */

public interface OnLoadMorePresenter {
    //加载完成
    void onLoadComplete();
    //结束，一般为空调用
    void onLoadEnd();
    //加载失败
    void onLoadError();
}
