package com.wangkm.player.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wangkm.player.ai.AudioEnhanceProcessor
import com.wangkm.player.ai.SubtitleAIProcessor
import com.wangkm.player.ai.VideoEnhanceProcessor

/**
 * 智能播放器对话框集合
 * 简化版，减少复杂功能以避免编译错误
 */

@Composable
fun SubtitleSettingsDialog(
    currentConfig: SubtitleAIProcessor.SubtitleConfig,
    onConfigChange: (SubtitleAIProcessor.SubtitleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember { mutableStateOf(currentConfig) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "字幕设置",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("源语言: ${config.sourceLanguage}")
                Text("目标语言: ${config.targetLanguage ?: "无"}")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfigChange(config)
                            onDismiss()
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun VideoEnhanceSettingsDialog(
    currentConfig: VideoEnhanceProcessor.EnhanceConfig,
    onConfigChange: (VideoEnhanceProcessor.EnhanceConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember { mutableStateOf(currentConfig) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "视频增强设置",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SwitchSetting(
                    title = "超分辨率",
                    description = "提升视频清晰度",
                    checked = config.enableSuperResolution,
                    onCheckedChange = { config = config.copy(enableSuperResolution = it) }
                )
                
                SwitchSetting(
                    title = "降噪处理",
                    description = "减少视频噪点",
                    checked = config.enableDenoising,
                    onCheckedChange = { config = config.copy(enableDenoising = it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfigChange(config)
                            onDismiss()
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun AudioEnhanceSettingsDialog(
    currentConfig: AudioEnhanceProcessor.AudioEnhanceConfig,
    onConfigChange: (AudioEnhanceProcessor.AudioEnhanceConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember { mutableStateOf(currentConfig) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "音频增强设置",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SwitchSetting(
                    title = "3D音效",
                    description = "启用虚拟3D环绕声效果",
                    checked = config.enable3DAudio,
                    onCheckedChange = { config = config.copy(enable3DAudio = it) }
                )
                
                SwitchSetting(
                    title = "噪声抑制",
                    description = "减少背景噪声",
                    checked = config.enableNoiseReduction,
                    onCheckedChange = { config = config.copy(enableNoiseReduction = it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfigChange(config)
                            onDismiss()
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}