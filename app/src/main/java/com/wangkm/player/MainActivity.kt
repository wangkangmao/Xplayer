package com.wangkm.player

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wangkm.player.demo.VideoPlayerDemoActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 直接跳转到演示界面
        startActivity(Intent(this, VideoPlayerDemoActivity::class.java))
        finish()
    }
}