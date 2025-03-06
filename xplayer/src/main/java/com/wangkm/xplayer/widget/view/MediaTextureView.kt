package com.wangkm.xplayer.widget.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import com.wangkm.xplayer.base.AbstractMediaPlayer
import com.wangkm.xplayer.interfaces.IRenderView
import com.wangkm.xplayer.media.IMediaPlayer
import kotlin.math.max
import kotlin.math.min

/**
 * created by wangkm
 * Desc:SDK提供的 支持三种缩放模式\画面翻转/角度 的默认自定义画面渲染
 */
class MediaTextureView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : TextureView(
    context!!, attrs, defStyleAttr
), IRenderView, SurfaceTextureListener {
    private var mMediaPlayer: AbstractMediaPlayer? = null
    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var mVideoSarNum = 0
    private var mVideoSarDen = 0
    var measureWidth: Int = 0
        private set
    var measureHeight: Int = 0
        private set
    protected var mScaleMode: Int = IMediaPlayer.MODE_NOZOOM_TO_FIT //默认是原始大小
    private var mDegree = 0
    private var mMirror = false
    private var mVerticalOrientation = false
    var mUseSettingRatio: Boolean = false
    private var mHOffset = 0.0f
    private var mVOffset = 0.0f
    private val mMatrix = Matrix()

    private var mLayoutWidth = 0
    private var mLayoutHeight = 0
    private var mCenterPointX = 0f
    private var mCenterPointY = 0f
    private var mDeltaX = 0f
    private var mDeltaY = 0f
    private var mCurrentVideoWidth = 0f
    private var mCurrentVideoHeight = 0f
    private var mTotalTranslateX = 0f
    private var mTotalTranslateY = 0f
    var videoScaleRatio: Float = 1.0f
        private set
    private var mScaledRatio = 0f
    private var mInitRatio = 0f

    private var mCurrentDispStatus = STATUS_NORMAL

    init {
        //自定义解码器注意这里的设置
        isSaveFromParentEnabled = true
        isDrawingCacheEnabled = false
        surfaceTextureListener = this
    }

    //======================================自定义解码器需要关心的回调===================================
    override fun attachMediaPlayer(mediaPlayer: AbstractMediaPlayer) {
        this.mMediaPlayer = mediaPlayer
    }

    override fun getView(): View {
        return this
    }

    override fun setVideoSize(width: Int, height: Int) {
        mVideoWidth = width
        mVideoHeight = height
    }

    override fun setZoomMode(zoomMode: Int) {
        mScaleMode = zoomMode
        mUseSettingRatio = false
        mCurrentDispStatus = STATUS_NORMAL
        requestLayout()
    }

    override fun setDegree(degree: Int) {
        mDegree = degree
        mCurrentDispStatus = STATUS_NORMAL
        requestLayout()
    }

    override fun setViewRotation(rotation: Int) {
        setRotation(rotation.toFloat())
    }

    override fun setSarSize(sarNum: Int, sarDen: Int) {
        mVideoSarNum = sarNum
        mVideoSarDen = sarDen
    }

    override fun setMirror(mirror: Boolean): Boolean {
        mMirror = mirror
        scaleX = if (mirror) -1.0f else 1.0f
        return mMirror
    }

    override fun toggleMirror(): Boolean {
        mMirror = !mMirror
        scaleX = if (mMirror) -1.0f else 1.0f
        return mMirror
    }

    override fun requestDrawLayout() {
        requestLayout()
    }

    override fun release() {
        try {
            if (null != mSurfaceTexture) {
                mSurfaceTexture!!.release()
            }
            if (null != mSurface) {
                mSurface!!.release()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            mSurfaceTexture = null
            mSurface = null
            mMediaPlayer = null
        }
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
//        ILogger.d(TAG,"onSurfaceTextureAvailable-->width:"+width+",height:"+height);
        if (null == mMediaPlayer) return
        if (null != mSurfaceTexture) {
            setSurfaceTexture(mSurfaceTexture!!)
        } else {
            mSurfaceTexture = surfaceTexture
            mSurface = Surface(surfaceTexture)
            mMediaPlayer!!.setSurface(mSurface)
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
//        ILogger.d(TAG,"onSurfaceTextureUpdated");
    }

    private fun Zoom() {
        val matrix = mMatrix
        var videoWidth = mVideoWidth
        val videoHeight = mVideoHeight

        if (mMirror) {
            mCenterPointX = mLayoutWidth - mCenterPointX
        }
        if (mVideoSarNum > 0 && mVideoSarDen > 0) {
            videoWidth = videoWidth * mVideoSarNum / mVideoSarDen
        }

        var scaleX = videoWidth.toFloat() / mLayoutWidth
        var scaleY = videoHeight.toFloat() / mLayoutHeight
        if (mScaleMode == IMediaPlayer.MODE_NOZOOM_TO_FIT) {
            if ((mDegree / 90) % 2 != 0) {
                scaleX = mLayoutHeight.toFloat() / mLayoutWidth
                scaleY = mLayoutWidth.toFloat() / mLayoutHeight
            } else {
                scaleX = 1.0f
                scaleY = 1.0f
            }
        }

        matrix.reset()
        matrix.postScale(videoScaleRatio * scaleX, videoScaleRatio * scaleY)
        matrix.postRotate(mDegree.toFloat())

        var scaledWidth = mLayoutWidth * videoScaleRatio * scaleX
        var scaledHeight = mLayoutHeight * videoScaleRatio * scaleY
        if ((mDegree / 90) % 2 != 0) {
            scaledWidth = mLayoutHeight * videoScaleRatio * scaleY
            scaledHeight = mLayoutWidth * videoScaleRatio * scaleX
        }
        var translateX = 0f
        var translateY = 0f

        translateX = mTotalTranslateX * mScaledRatio + mCenterPointX * (1 - mScaledRatio)
        translateY = mTotalTranslateY * mScaledRatio + mCenterPointY * (1 - mScaledRatio)

        when (mDegree) {
            0 -> {
                if (scaledWidth < mLayoutWidth) {
                    translateX = ((mLayoutWidth - scaledWidth) / 2f)
                } else {
                    if (translateX > 0) {
                        translateX = 0f
                    } else if (scaledWidth + translateX < mLayoutWidth) {
                        translateX = mLayoutWidth - scaledWidth
                    }
                }

                if (scaledHeight < mLayoutHeight) {
                    translateY = ((mLayoutHeight - scaledHeight) / 2f)
                } else {
                    if (translateY > 0) {
                        translateY = 0f
                    } else if (scaledHeight + translateY < mLayoutHeight) {
                        translateY = mLayoutHeight - scaledHeight
                    }
                }
            }

            -90 -> {
                if (scaledWidth < mLayoutWidth) {
                    translateX = ((mLayoutWidth - scaledWidth) / 2f)
                } else {
                    if (translateX > 0) {
                        translateX = 0f
                    } else if (scaledWidth + translateX < mLayoutWidth) {
                        translateX = mLayoutWidth - scaledWidth
                    }
                }

                if (scaledHeight < mLayoutHeight) {
                    translateY = ((mLayoutHeight + scaledHeight) / 2f)
                } else {
                    if (translateY > scaledHeight) {
                        translateY = scaledHeight
                    } else if (translateY < mLayoutHeight) {
                        translateY = mLayoutHeight.toFloat()
                    }
                }
            }

            -180 -> {
                if (scaledWidth < mLayoutWidth) {
                    translateX = ((mLayoutWidth + scaledWidth) / 2f)
                } else {
                    if (translateX > scaledWidth) {
                        translateX = scaledWidth
                    } else if (translateX < mLayoutWidth) {
                        translateX = mLayoutWidth.toFloat()
                    }
                }

                if (scaledHeight < mLayoutHeight) {
                    translateY = ((mLayoutHeight + scaledHeight) / 2f)
                } else {
                    if (translateY > scaledHeight) {
                        translateY = scaledHeight
                    } else if (translateY < mLayoutHeight) {
                        translateY = mLayoutHeight.toFloat()
                    }
                }
            }

            -270 -> {
                if (scaledWidth < mLayoutWidth) {
                    translateX = ((mLayoutWidth + scaledWidth) / 2f)
                } else {
                    if (translateX > scaledWidth) {
                        translateX = scaledWidth
                    } else if (translateX < mLayoutWidth) {
                        translateX = mLayoutWidth.toFloat()
                    }
                }

                if (scaledHeight < mLayoutHeight) {
                    translateY = ((mLayoutHeight - scaledHeight) / 2f)
                } else {
                    if (translateY > 0) {
                        translateY = 0f
                    } else if (scaledHeight + translateY < mLayoutHeight) {
                        translateY = mLayoutHeight - scaledHeight
                    }
                }
            }
        }
        //            translateX += mHOffset * mLayoutWidth/2;
//            translateY +=  - mVOffset* mLayoutHeight/2;
        matrix.postTranslate(translateX, translateY)

        mTotalTranslateX = translateX
        mTotalTranslateY = translateY
        mCurrentVideoWidth = scaledWidth
        mCurrentVideoHeight = scaledHeight
    }

    private fun Move() {
        val matrix = mMatrix
        var videoWidth = mVideoWidth
        val videoHeight = mVideoHeight

        if (mMirror) {
            mDeltaX = -mDeltaX
        }
        if (mVideoSarNum > 0 && mVideoSarDen > 0) {
            videoWidth = videoWidth * mVideoSarNum / mVideoSarDen
        }

        var scaleX = videoWidth.toFloat() / mLayoutWidth
        var scaleY = videoHeight.toFloat() / mLayoutHeight
        if (mScaleMode == IMediaPlayer.MODE_NOZOOM_TO_FIT) {
            if ((mDegree / 90) % 2 != 0) {
                scaleX = mLayoutHeight.toFloat() / mLayoutWidth
                scaleY = mLayoutWidth.toFloat() / mLayoutHeight
            } else {
                scaleX = 1.0f
                scaleY = 1.0f
            }
        }

        matrix.reset()
        matrix.postScale(videoScaleRatio * scaleX, videoScaleRatio * scaleY)
        matrix.postRotate(mDegree.toFloat())

        var xoffset = 0f
        var yoffset = 0f
        when (mDegree) {
            0 -> {
                xoffset = ((mLayoutWidth - mCurrentVideoWidth) / 2f)
                yoffset = ((mLayoutHeight - mCurrentVideoHeight) / 2f)
            }

            -90 -> {
                xoffset = (mLayoutWidth - mCurrentVideoWidth) / 2
                yoffset = (mLayoutHeight + mCurrentVideoHeight) / 2
            }

            -180 -> {
                xoffset = (mLayoutWidth + mCurrentVideoWidth) / 2
                yoffset = (mLayoutHeight + mCurrentVideoHeight) / 2
            }

            -270 -> {
                xoffset = ((mLayoutWidth + mCurrentVideoWidth) / 2f)
                yoffset = ((mLayoutHeight - mCurrentVideoHeight) / 2f)
            }
        }
        if (mTotalTranslateX + mDeltaX > xoffset + (mCurrentVideoWidth - mLayoutWidth) / 2) {
            mDeltaX = 0f
        } else if (mTotalTranslateX + mDeltaX < xoffset - (mCurrentVideoWidth - mLayoutWidth) / 2) {
            mDeltaX = 0f
        }

        if (mTotalTranslateY + mDeltaY > yoffset + (mCurrentVideoHeight - mLayoutHeight) / 2) {
            mDeltaY = 0f
        } else if (mTotalTranslateY + mDeltaY < yoffset - (mCurrentVideoHeight - mLayoutHeight) / 2) {
            mDeltaY = 0f
        }

        val translateX = mTotalTranslateX + mDeltaX
        val translateY = mTotalTranslateY + mDeltaY

        matrix.postTranslate(translateX, translateY)
        mTotalTranslateX = translateX
        mTotalTranslateY = translateY
    }

    private fun Normal(widthSpecMode: Int, heightSpecMode: Int) {
        var ratio = 1.0f
        var hOffset = 0.0f
        var vOffset = 0.0f
        var videoWidth = mVideoWidth
        var videoHeight = mVideoHeight
        val width = mLayoutWidth
        val height = mLayoutHeight
        val matrix = mMatrix

        if (mVideoSarNum > 0 && mVideoSarDen > 0) {
            videoWidth = videoWidth * mVideoSarNum / mVideoSarDen
        }

        var scaleX = videoWidth.toFloat() / mLayoutWidth
        var scaleY = videoHeight.toFloat() / mLayoutHeight

        if ((mDegree / 90) % 2 != 0) {
            videoHeight = mVideoWidth
            videoWidth = mVideoHeight

            if (mVideoSarNum > 0 && mVideoSarDen > 0) {
                videoHeight = videoHeight * mVideoSarNum / mVideoSarDen
            }
        }

        mInitRatio = min((width.toFloat() / videoWidth).toDouble(), (height.toFloat() / videoHeight).toDouble()).toFloat()

        when (mScaleMode) {
            IMediaPlayer.MODE_ZOOM_CROPPING -> {
                ratio = max((width.toFloat() / videoWidth).toDouble(), (height.toFloat() / videoHeight).toDouble()).toFloat()
                run {
                    vOffset = 0.0f
                    hOffset = vOffset
                }
                videoScaleRatio = ratio
            }

            IMediaPlayer.MODE_ZOOM_TO_FIT -> {
                ratio = min((width.toFloat() / videoWidth).toDouble(), (height.toFloat() / videoHeight).toDouble()).toFloat()
                hOffset = mHOffset
                vOffset = mVOffset
                videoScaleRatio = ratio
            }

            IMediaPlayer.MODE_NOZOOM_TO_FIT -> {
                if ((mDegree / 90) % 2 != 0) {
                    scaleX = height.toFloat() / width
                    scaleY = width.toFloat() / height
                } else {
                    scaleX = 1.0f
                    scaleY = 1.0f
                }
                run {
                    mInitRatio = ratio
                    this.videoScaleRatio = mInitRatio
                }
            }
        }
        if ((mDegree / 90) % 2 != 0) {
            mCurrentVideoWidth = height * scaleY * ratio
            mCurrentVideoHeight = width * scaleX * ratio
        } else {
            mCurrentVideoWidth = width * scaleX * ratio
            mCurrentVideoHeight = height * scaleY * ratio
        }

        matrix.reset()

        matrix.postScale(ratio * scaleX, ratio * scaleY)
        matrix.postRotate(mDegree.toFloat())

        var translateX = 0.0f
        var translateY = 0.0f


        when (mDegree) {
            0 -> {
                translateX = ((width - mCurrentVideoWidth) / 2f)
                translateY = ((height - mCurrentVideoHeight) / 2f)
            }

            -90 -> {
                translateX = (width - mCurrentVideoWidth) / 2
                translateY = (height + mCurrentVideoHeight) / 2
            }

            -180 -> {
                translateX = (width + mCurrentVideoWidth) / 2
                translateY = (height + mCurrentVideoHeight) / 2
            }

            -270 -> {
                translateX = ((width + mCurrentVideoWidth) / 2f)
                translateY = ((height - mCurrentVideoHeight) / 2f)
            }
        }
        mTotalTranslateX = translateX + hOffset * width / 2
        mTotalTranslateY = translateY - vOffset * height / 2

        matrix.postTranslate(mTotalTranslateX, mTotalTranslateY)

        //            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY)
//                matrix.postTranslate(mHOffset*width/2,-mVOffset* height/2);
//            else if (widthSpecMode == MeasureSpec.EXACTLY){
//                matrix.postTranslate(mHOffset*width/2,0);
//            }
//            else if (heightSpecMode == MeasureSpec.EXACTLY){
//                matrix.postTranslate(0,-mVOffset* height/2);
//            }
        measureWidth = (width * ratio * scaleX).toInt()
        measureHeight = (height * ratio * scaleY).toInt()
    }

    private fun Measure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        MideaUtils.getInstance().log(TAG,"Measure-->widthMeasureSpec:"+widthMeasureSpec+",heightMeasureSpec:"+heightMeasureSpec+",mVideoWidth:"+mVideoWidth+",mVideoHeight:"+mVideoHeight);
        if (mVideoWidth == 0 || mVideoHeight == 0) return
        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        mLayoutWidth = widthSpecSize
        mLayoutHeight = heightSpecSize

        if (mCurrentDispStatus == STATUS_NORMAL) {
            Normal(widthSpecMode, heightSpecMode)
        }
        setTransform(mMatrix)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Measure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    fun setVideoScaleRatio(ratio: Float, x: Float, y: Float) {
        //if ( (ratio < mTotalRatio && ratio < mInitRatio) || (ratio > mTotalRatio && ratio > 100*mInitRatio))
        if ((ratio < 0.25) || (ratio > 100)) return

        if ((mScaleMode == IMediaPlayer.MODE_ZOOM_TO_FIT) &&
            (mHOffset > 0.0f || mHOffset < 0.0f || mVOffset > 0.0f || mVOffset < 0.0f)
        ) return

        mScaledRatio = ratio / videoScaleRatio
        videoScaleRatio = ratio
        mCenterPointX = x
        mCenterPointY = y
        mCurrentDispStatus = STATUS_ZOOM
        Zoom()
        requestLayout()
    }

    fun setVerticalOrientation(vertical: Boolean) {
        mVerticalOrientation = vertical
        mCurrentDispStatus = STATUS_NORMAL
        requestLayout()
    }

    fun setVideoOffset(horizontal: Float, vertical: Float) {
        mHOffset = horizontal
        mVOffset = vertical
        mCurrentDispStatus = STATUS_NORMAL
        requestLayout()
    }

    fun moveVideo(deltaX: Float, deltaY: Float) {
        if ((mScaleMode == IMediaPlayer.MODE_ZOOM_TO_FIT) &&
            (mHOffset > 0.0f || mHOffset < 0.0f || mVOffset > 0.0f || mVOffset < 0.0f)
        ) return

        mDeltaX = deltaX
        mDeltaY = deltaY
        mCurrentDispStatus = STATUS_MOVE
        Move()
        requestLayout()
    }

    companion object {
        const val STATUS_NORMAL: Int = 1
        const val STATUS_ZOOM: Int = 2
        const val STATUS_MOVE: Int = 3
    }
}