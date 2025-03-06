package com.wangkm.xplayer.widget.view;

import android.annotation.SuppressLint;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * created by wangkm
 * Desc:View圆角设置
 */
@SuppressLint("NewApi")
public class LayoutProvider extends ViewOutlineProvider {

    private float mRadius;

    public LayoutProvider(float radius){
        this.mRadius = radius;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mRadius);
        view.setClipToOutline(true);
    }
}