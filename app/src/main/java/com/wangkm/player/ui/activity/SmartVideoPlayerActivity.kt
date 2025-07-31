package com.wangkm.player.ui.activity

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        
        // 设置全屏显示支持
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
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
                    },
                    onFullScreenToggle = { isFullScreen ->
                        toggleFullScreen(isFullScreen)
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
    
    /**
     * 切换全屏模式
     */
    private fun toggleFullScreen(isFullScreen: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        if (isFullScreen) {
            // 进入全屏模式
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            windowInsetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            // 保持屏幕常亮
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            // 退出全屏模式
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            // 取消屏幕常亮
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        // 更新ViewModel状态
        viewModel.toggleFullScreen(isFullScreen)
    }
    
    override fun onBackPressed() {
        // 如果当前是全屏模式，先退出全屏
        if (viewModel.uiState.value.isFullScreen) {
            toggleFullScreen(false)
        } else {
            super.onBackPressed()
        }
    }
}

/**
 * 智能播放器主界面内容
 */
@Composable
private fun SmartVideoPlayerContent(
    viewModel: SmartVideoPlayerViewModel,
    smartPlayerManager: SmartPlayerManager,
    onVideoPlayerCreated: (VideoPlayer) -> Unit,
    onFullScreenToggle: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState
    val context = LocalContext.current
    
    var showPlayerView by remember { mutableStateOf(false) }
    var currentVideoPath by remember { mutableStateOf("") }
    
    if (uiState.isFullScreen) {
        // 全屏播放器界面
        FullScreenPlayerView(
            videoPath = currentVideoPath,
            smartPlayerManager = smartPlayerManager,
            onPlayerCreated = onVideoPlayerCreated,
            onExitFullScreen = { onFullScreenToggle(false) },
            onShowSettings = { viewModel.toggleSettings(true) },
            viewModel = viewModel
        )
    } else {
        // 普通界面
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 智能功能控制面板
            SmartPlayerScreen(
                modifier = Modifier.weight(1f),
                onVideoSelected = { videoPath ->
                    currentVideoPath = videoPath
                    showPlayerView = true
                    viewModel.updateVideoPath(videoPath)
                    
                    // 自动生成字幕
                    if (smartPlayerManager.smartFeaturesState.value.subtitleGenerationEnabled) {
                        smartPlayerManager.generateSmartSubtitles(
                            videoPath = videoPath,
                            onProgress = { progress ->
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
                    viewModel.toggleSettings(true)
                }
            )
            
            // 视频播放器视图（如果选择了视频）
            if (showPlayerView && currentVideoPath.isNotEmpty()) {
                SmartVideoPlayerView(
                    videoPath = currentVideoPath,
                    smartPlayerManager = smartPlayerManager,
                    onPlayerCreated = onVideoPlayerCreated,
                    onFullScreenRequest = { onFullScreenToggle(true) },
                    onShowSettings = { viewModel.toggleSettings(true) },
                    modifier = Modifier.height(200.dp)
                )
            }
        }
    }
    
    // 播放器设置对话框
    if (uiState.showSettings) {
        PlayerSettingsDialog(
            smartPlayerManager = smartPlayerManager,
            onDismiss = { viewModel.toggleSettings(false) }
        )
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
    onFullScreenRequest: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(modifier = modifier.fillMaxWidth()) {
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
                            return null
                        }
                        
                        override fun createRenderView(): com.wangkm.xplayer.interfaces.IRenderView? {
                            return null
                        }
                        
                        override fun onPlayerState(
                            state: com.wangkm.xplayer.model.PlayerState?,
                            message: String?
                        ) {
                            when (state) {
                                com.wangkm.xplayer.model.PlayerState.STATE_PREPARE -> {
                                    android.util.Log.d("SmartPlayer", "播放器准备中")
                                }
                                com.wangkm.xplayer.model.PlayerState.STATE_START -> {
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
            modifier = Modifier.fillMaxSize(),
            update = { videoPlayer ->
                videoPlayer.setDataSource(videoPath)
            }
        )
        
        // 播放器控制按钮覆盖层
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 设置按钮
            FloatingActionButton(
                onClick = onShowSettings,
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // 全屏按钮
            FloatingActionButton(
                onClick = onFullScreenRequest,
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "全屏",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 全屏播放器视图
 */
@Composable
private fun FullScreenPlayerView(
    videoPath: String,
    smartPlayerManager: SmartPlayerManager,
    onPlayerCreated: (VideoPlayer) -> Unit,
    onExitFullScreen: () -> Unit,
    onShowSettings: () -> Unit,
    viewModel: SmartVideoPlayerViewModel
) {
    var showControls by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // 全屏视频播放器
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                VideoPlayer(ctx).apply {
                    initController()
                    setDataSource(videoPath)
                    onPlayerCreated(this)
                    prepareAsync()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 控制界面（可隐藏）
        if (showControls) {
            // 顶部控制栏
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 返回按钮
                IconButton(onClick = onExitFullScreen) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "退出全屏",
                        tint = Color.White
                    )
                }
                
                // 设置按钮
                IconButton(onClick = onShowSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = Color.White
                    )
                }
            }
            
            // 底部控制栏
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放/暂停按钮
                IconButton(onClick = { /* 播放/暂停逻辑 */ }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // 进度条
                Slider(
                    value = 0.3f,
                    onValueChange = { /* 进度调整逻辑 */ },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
                
                // 全屏退出按钮
                IconButton(onClick = onExitFullScreen) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "退出全屏",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 播放器设置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsDialog(
    smartPlayerManager: SmartPlayerManager,
    onDismiss: () -> Unit
) {
    val smartFeaturesState by smartPlayerManager.smartFeaturesState.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "播放器设置",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // AI功能设置
                Text(
                    text = "AI智能功能",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 智能字幕开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "智能字幕生成")
                        Text(
                            text = "自动识别语音并生成字幕",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = smartFeaturesState.subtitleGenerationEnabled,
                        onCheckedChange = { enabled ->
                            // 更新状态的逻辑
                        }
                    )
                }
                
                // 画质增强开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "AI画质增强")
                        Text(
                            text = "提升视频清晰度和色彩",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = smartFeaturesState.videoEnhancementEnabled,
                        onCheckedChange = { enabled ->
                            // 更新状态的逻辑
                        }
                    )
                }
                
                // 音频增强开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "音频智能优化")
                        Text(
                            text = "3D音效和智能降噪",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = smartFeaturesState.audioEnhancementEnabled,
                        onCheckedChange = { enabled ->
                            // 更新状态的逻辑
                        }
                    )
                }
                
                Divider()
                
                // 播放器设置
                Text(
                    text = "播放器设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 解码器选择
                var selectedDecoder by remember { mutableStateOf(0) }
                Column {
                    Text(text = "解码器选择")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("系统默认", "IJKPlayer", "ExoPlayer").forEachIndexed { index, name ->
                            FilterChip(
                                onClick = { selectedDecoder = index },
                                label = { Text(name) },
                                selected = selectedDecoder == index
                            )
                        }
                    }
                }
                
                // 渲染模式选择
                var selectedRenderer by remember { mutableStateOf(0) }
                Column {
                    Text(text = "渲染模式")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("TextureView", "SurfaceView").forEachIndexed { index, name ->
                            FilterChip(
                                onClick = { selectedRenderer = index },
                                label = { Text(name) },
                                selected = selectedRenderer == index
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
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
        val showSettings: Boolean = false,
        val isFullScreen: Boolean = false
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
    
    fun toggleFullScreen(isFullScreen: Boolean) {
        _uiState.value = _uiState.value.copy(isFullScreen = isFullScreen)
    }
}

