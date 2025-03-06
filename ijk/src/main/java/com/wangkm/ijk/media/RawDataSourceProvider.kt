package com.wangkm.ijk.media

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import tv.danmaku.ijk.media.player.misc.IMediaDataSource
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * created by wangkm
 * Desc:本地资源地址转换处理
 */
class RawDataSourceProvider(private var mDescriptor: AssetFileDescriptor?) : IMediaDataSource {
    private var mMediaBytes: ByteArray? = null

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position + 1 >= mMediaBytes!!.size) {
            return -1
        }

        var length: Int
        if (position + size < mMediaBytes!!.size) {
            length = size
        } else {
            length = (mMediaBytes!!.size - position).toInt()
            if (length > buffer.size) length = buffer.size

            length--
        }
        System.arraycopy(mMediaBytes, position.toInt(), buffer, offset, length)

        return length
    }

    @Throws(IOException::class)
    override fun getSize(): Long {
        val length = mDescriptor!!.length
        if (mMediaBytes == null) {
            val inputStream: InputStream = mDescriptor!!.createInputStream()
            mMediaBytes = readBytes(inputStream)
        }


        return length
    }

    @Throws(IOException::class)
    override fun close() {
        if (mDescriptor != null) mDescriptor!!.close()

        mDescriptor = null
        mMediaBytes = null
    }

    @Throws(IOException::class)
    private fun readBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()

        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)

        var len: Int
        while ((inputStream.read(buffer).also { len = it }) != -1) {
            byteBuffer.write(buffer, 0, len)
        }

        return byteBuffer.toByteArray()
    }

    companion object {
        fun create(context: Context, uri: Uri?): RawDataSourceProvider? {
            try {
                val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri!!, "r")
                return RawDataSourceProvider(fileDescriptor)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            return null
        }
    }
}