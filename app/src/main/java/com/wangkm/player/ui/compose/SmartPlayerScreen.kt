package com.wangkm.player.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wangkm.player.ai.SmartPlayerManager
import com.wangkm.player.ai.SubtitleAIProcessor
import com.wangkm.player.ai.VideoEnhanceProcessor
import com.wangkm.player.ai.AudioEnhanceProcessor

/**
 * 智能播放器主界面
 * 展示所有AI增强功能和控制面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlayerScreen(
    modifier: Modifier = Modifier,
    onVideoSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val smartPlayerManager = remember { SmartPlayerManager.getInstance(context) }
    val state by smartPlayerManager.smartFeaturesState.collectAsStateWithLifecycle()
    
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showVideoEnhanceDialog by remember { mutableStateOf(false) }
    var showAudioEnhanceDialog by remember { mutableStateOf(false) }
    var showVideoLibrary by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { 
                Text(
                    "智能播放器",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            actions = {
                IconButton(onClick = { showVideoLibrary = true }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "视频库")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        
        // 状态指示器
        SmartPlayerStatusCard(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // 快速播放按钮
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showVideoLibrary = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "开始视频播放演示",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "选择示例视频或输入自定义链接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // 功能卡片列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI字幕生成
            item {
                SmartFeatureCard(
                    title = "AI智能字幕",
                    description = "自动识别语音并生成多语言字幕",
                    icon = Icons.Default.Settings,
                    enabled = state.subtitleGenerationEnabled,
                    isProcessing = state.isProcessingSubtitles,
                    onToggle = { enabled ->
                        smartPlayerManager.toggleSmartFeature(
                            SmartPlayerManager.SmartFeature.SUBTITLE_GENERATION,
                            enabled
                        )
                    },
                    onSettings = { showSubtitleDialog = true },
                    statusText = if (state.currentSubtitles.isNotEmpty()) 
                        "已生成 ${state.currentSubtitles.size} 条字幕" else null
                )
            }
            
            // 视频画质增强
            item {
                SmartFeatureCard(
                    title = "AI画质增强",
                    description = "超分辨率、去噪、HDR等视频增强",
                    icon = Icons.Default.Settings,
                    enabled = state.videoEnhancementEnabled,
                    isProcessing = state.isProcessingVideo,
                    onToggle = { enabled ->
                        smartPlayerManager.toggleSmartFeature(
                            SmartPlayerManager.SmartFeature.VIDEO_ENHANCEMENT,
                            enabled
                        )
                    },
                    onSettings = { showVideoEnhanceDialog = true },
                    statusText = if (state.videoEnhancementEnabled) "实时增强中" else null
                )
            }
            
            // 音频智能增强
            item {
                SmartFeatureCard(
                    title = "音频智能增强",
                    description = "3D音效、降噪、均衡器优化",
                    icon = Icons.Default.Settings,
                    enabled = state.audioEnhancementEnabled,
                    isProcessing = false,
                    onToggle = { enabled ->
                        smartPlayerManager.toggleSmartFeature(
                            SmartPlayerManager.SmartFeature.AUDIO_ENHANCEMENT,
                            enabled
                        )
                    },
                    onSettings = { showAudioEnhanceDialog = true },
                    statusText = if (state.audioEnhancementEnabled) "音效增强激活" else null
                )
            }
            
            // 字幕列表
            if (state.currentSubtitles.isNotEmpty()) {
                item {
                    SubtitleListCard(
                        subtitles = state.currentSubtitles,
                        onExport = { format ->
                            // 导出字幕逻辑
                        }
                    )
                }
            }
        }
    }
    
    // 视频选择对话框
    if (showVideoLibrary) {
        VideoLibraryDialog(
            onVideoSelected = { videoUrl ->
                onVideoSelected(videoUrl)
                showVideoLibrary = false
            },
            onDismiss = { showVideoLibrary = false }
        )
    }
    
    // 对话框
    if (showSubtitleDialog) {
        SubtitleSettingsDialog(
            currentConfig = com.wangkm.player.ai.SubtitleAIProcessor.SubtitleConfig(),
            onConfigChange = { config -> 
                // 应用配置
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }
    
    if (showVideoEnhanceDialog) {
        VideoEnhanceSettingsDialog(
            currentConfig = state.videoEnhanceConfig,
            onDismiss = { showVideoEnhanceDialog = false },
            onConfigChange = { config ->
                smartPlayerManager.updateVideoEnhanceConfig(config)
                showVideoEnhanceDialog = false
            }
        )
    }
    
    if (showAudioEnhanceDialog) {
        AudioEnhanceSettingsDialog(
            currentConfig = state.audioEnhanceConfig,
            onDismiss = { showAudioEnhanceDialog = false },
            onConfigChange = { config ->
                smartPlayerManager.updateAudioEnhanceConfig(config)
                showAudioEnhanceDialog = false
            }
        )
    }
}

/**
 * 状态卡片
 */
@Composable
private fun SmartPlayerStatusCard(
    state: SmartPlayerManager.SmartFeaturesState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "系统状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isInitialized) Color.Green else Color.Yellow
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = state.processingStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 智能功能卡片
 */
@Composable
private fun SmartFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    isProcessing: Boolean,
    onToggle: (Boolean) -> Unit,
    onSettings: () -> Unit,
    statusText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = !isProcessing
                )
            }
            
            if (statusText != null || isProcessing) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "处理中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (statusText != null) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    TextButton(
                        onClick = onSettings,
                        enabled = enabled && !isProcessing
                    ) {
                        Text("设置")
                    }
                }
            }
        }
    }
}

/**
 * 字幕列表卡片
 */
@Composable
private fun SubtitleListCard(
    subtitles: List<SubtitleAIProcessor.SubtitleItem>,
    onExport: (SubtitleAIProcessor.SubtitleFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "生成的字幕 (${subtitles.size}条)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    TextButton(
                        onClick = { onExport(SubtitleAIProcessor.SubtitleFormat.SRT) }
                    ) {
                        Text("导出SRT")
                    }
                    
                    TextButton(
                        onClick = { onExport(SubtitleAIProcessor.SubtitleFormat.VTT) }
                    ) {
                        Text("导出VTT")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 显示前几条字幕预览
            subtitles.take(3).forEach { subtitle ->
                SubtitlePreviewItem(subtitle = subtitle)
            }
            
            if (subtitles.size > 3) {
                Text(
                    text = "... 还有 ${subtitles.size - 3} 条字幕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * 字幕预览项
 */
@Composable
private fun SubtitlePreviewItem(
    subtitle: SubtitleAIProcessor.SubtitleItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "${formatTime(subtitle.startTime)} → ${formatTime(subtitle.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            if (subtitle.confidence < 1.0f) {
                Text(
                    text = "置信度: ${(subtitle.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * 视频库对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoLibraryDialog(
    onVideoSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customUrl by remember { mutableStateOf("") }
    
    // 示例视频列表
    val sampleVideos = listOf(
        VideoItem(
            title = "惊奇队长2预告片",
            description = "高清1080p示例视频",
            url = "https://vd3.bdstatic.com/mda-pd736fppd4m1muub/sc/cae_h264/1680924082856978562/mda-pd736fppd4m1muub.mp4",
            duration = "2:30",
            thumbnail = "🎬"
        ),
        VideoItem(
            title = "4K风景视频",
            description = "适合测试高分辨率播放",
            url = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
            duration = "0:30",
            thumbnail = "🌄"
        ),
        VideoItem(
            title = "音乐MV演示",
            description = "测试音频增强功能",
            url = "https://vjs.zencdn.net/v/oceans.mp4",
            duration = "1:45",
            thumbnail = "🎵"
        ),
        VideoItem(
            title = "本地测试视频",
            description = "Android assets中的示例视频",
            url = "android.resource://com.wangkm.player/raw/sample_video",
            duration = "1:00",
            thumbnail = "📱"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择视频",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 示例视频列表
                item {
                    Text(
                        text = "示例视频",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(sampleVideos) { video ->
                    VideoItemCard(
                        video = video,
                        onClick = { onVideoSelected(video.url) }
                    )
                }
                
                // 自定义URL输入
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "或输入自定义URL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("视频URL") },
                        placeholder = { Text("https://example.com/video.mp4") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    if (customUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onVideoSelected(customUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("播放自定义视频")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 视频项卡片
 */
@Composable
private fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            Text(
                text = video.thumbnail,
                fontSize = 32.sp,
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .wrapContentSize(Alignment.Center)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 视频信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "时长: ${video.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 视频项数据类
 */
private data class VideoItem(
    val title: String,
    val description: String,
    val url: String,
    val duration: String,
    val thumbnail: String
)

/**
 * 格式化时间显示
 */
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}