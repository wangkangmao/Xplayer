package com.wangkm.player.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.wangkm.player.ai.SmartPlayerManager
import com.wangkm.player.ui.compose.SmartPlayerScreen
import com.wangkm.xplayer.widget.VideoPlayer

/**
 * 智能视频播放器Activity
 * 整合了所有AI增强功能的现代化播放器界面
 */
class SmartVideoPlayerActivity : ComponentActivity() {
    
    private val viewModel: SmartVideoPlayerViewModel by viewModels()
    private lateinit var smartPlayerManager: SmartPlayerManager
    private var videoPlayer: VideoPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化智能播放器管理器
        smartPlayerManager = SmartPlayerManager.getInstance(this)
        
        setContent {
            MaterialTheme {
                SmartVideoPlayerContent(
                    viewModel = viewModel,
                    smartPlayerManager = smartPlayerManager,
                    onVideoPlayerCreated = { player ->
                        videoPlayer = player
                        // 绑定播放器到智能管理器
                        smartPlayerManager.bindPlayer(player, player.hashCode())
                    }
                )
            }
        }
        
        // 初始化智能功能
        lifecycleScope.launch {
            try {
                val config = SmartPlayerManager.SmartPlayerConfig(
                    autoGenerateSubtitles = true,
                    enableVideoEnhance = false, // 默认关闭，性能考虑
                    enableAudioEnhance = true,
                    enableRealtimeProcessing = false
                )
                smartPlayerManager.initialize(config)
            } catch (e: Exception) {
                // 处理初始化失败
                android.util.Log.e("SmartPlayer", "初始化失败", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        smartPlayerManager.cleanup()
        videoPlayer?.onDestroy()
    }
    
    override fun onPause() {
        super.onPause()
        videoPlayer?.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        videoPlayer?.onResume()
    }
}

/**
 * 智能播放器主界面内容
 */
@Composable
private fun SmartVideoPlayerContent(
    viewModel: SmartVideoPlayerViewModel,
    smartPlayerManager: SmartPlayerManager,
    onVideoPlayerCreated: (VideoPlayer) -> Unit
) {
    val uiState by viewModel.uiState
    val context = LocalContext.current
    
    var showPlayerView by remember { mutableStateOf(false) }
    var currentVideoPath by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 智能功能控制面板
        SmartPlayerScreen(
            modifier = Modifier.weight(1f),
            onVideoSelected = { videoPath ->
                currentVideoPath = videoPath
                showPlayerView = true
                
                // 自动生成字幕
                if (smartPlayerManager.smartFeaturesState.value.subtitleGenerationEnabled) {
                    smartPlayerManager.generateSmartSubtitles(
                        videoPath = videoPath,
                        onProgress = { progress ->
                            // 显示进度
                            android.util.Log.d("SmartPlayer", "字幕生成进度: $progress")
                        },
                        onComplete = { subtitles ->
                            android.util.Log.d("SmartPlayer", "字幕生成完成: ${subtitles.size}条")
                        },
                        onError = { error ->
                            android.util.Log.e("SmartPlayer", "字幕生成失败: $error")
                        }
                    )
                }
            },
            onSettingsClick = {
                // 打开设置界面
            }
        )
        
        // 视频播放器视图（如果选择了视频）
        if (showPlayerView && currentVideoPath.isNotEmpty()) {
            SmartVideoPlayerView(
                videoPath = currentVideoPath,
                smartPlayerManager = smartPlayerManager,
                onPlayerCreated = onVideoPlayerCreated,
                modifier = Modifier.height(200.dp)
            )
        }
    }
}

/**
 * 智能视频播放器视图
 */
@Composable
private fun SmartVideoPlayerView(
    videoPath: String,
    smartPlayerManager: SmartPlayerManager,
    onPlayerCreated: (VideoPlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 使用AndroidView包装原有的VideoPlayer
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            VideoPlayer(ctx).apply {
                // 初始化播放器
                initController()
                
                // 设置数据源
                setDataSource(videoPath)
                
                // 配置播放器事件监听
                setOnPlayerActionListener(object : com.wangkm.xplayer.listener.OnPlayerEventListener() {
                    override fun createMediaPlayer(): com.wangkm.xplayer.base.AbstractMediaPlayer? {
                        // 返回默认的媒体播放器
                        return null
                    }
                    
                    override fun createRenderView(): com.wangkm.xplayer.interfaces.IRenderView? {
                        // 返回默认的渲染视图
                        return null
                    }
                    
                    override fun onPlayerState(
                        state: com.wangkm.xplayer.model.PlayerState?,
                        message: String?
                    ) {
                        // 处理播放器状态变化
                        when (state) {
                            com.wangkm.xplayer.model.PlayerState.STATE_PREPARE -> {
                                // 播放器准备中
                                android.util.Log.d("SmartPlayer", "播放器准备中")
                            }
                            com.wangkm.xplayer.model.PlayerState.STATE_START -> {
                                // 开始播放，启动实时增强
                                android.util.Log.d("SmartPlayer", "开始播放")
                            }
                            else -> {
                                android.util.Log.d("SmartPlayer", "播放器状态: $state")
                            }
                        }
                    }
                })
                
                // 通知外部播放器已创建
                onPlayerCreated(this)
                
                // 开始播放
                prepareAsync()
            }
        },
        modifier = modifier.fillMaxWidth(),
        update = { videoPlayer ->
            // 更新播放器配置
            videoPlayer.setDataSource(videoPath)
        }
    )
}

/**
 * 智能播放器ViewModel
 */
class SmartVideoPlayerViewModel : androidx.lifecycle.ViewModel() {
    
    data class UiState(
        val isLoading: Boolean = false,
        val currentVideoPath: String = "",
        val errorMessage: String? = null,
        val showSettings: Boolean = false
    )
    
    private val _uiState = androidx.compose.runtime.mutableStateOf(UiState())
    val uiState: State<UiState> = _uiState
    
    fun updateVideoPath(path: String) {
        _uiState.value = _uiState.value.copy(currentVideoPath = path)
    }
    
    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun toggleSettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = show)
    }
}

