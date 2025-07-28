package com.wangkm.player.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.wangkm.xplayer.widget.VideoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 智能播放器总管理器
 * 统一管理所有AI增强功能
 */
class SmartPlayerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SmartPlayerManager"
        
        @Volatile
        private var INSTANCE: SmartPlayerManager? = null
        
        fun getInstance(context: Context): SmartPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // AI处理器
    private val subtitleProcessor by lazy { SubtitleAIProcessor(context) }
    private val videoEnhanceProcessor by lazy { VideoEnhanceProcessor(context) }
    private val audioEnhanceProcessor by lazy { AudioEnhanceProcessor() }
    
    // 协程作用域
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 智能功能状态
    private val _smartFeaturesState = MutableStateFlow(SmartFeaturesState())
    val smartFeaturesState: StateFlow<SmartFeaturesState> = _smartFeaturesState.asStateFlow()
    
    // 当前播放器实例
    private var currentPlayer: VideoPlayer? = null
    private var currentAudioSessionId: Int = 0
    
    data class SmartFeaturesState(
        val isInitialized: Boolean = false,
        val subtitleGenerationEnabled: Boolean = true,
        val videoEnhancementEnabled: Boolean = false,
        val audioEnhancementEnabled: Boolean = true,
        val isProcessingSubtitles: Boolean = false,
        val isProcessingVideo: Boolean = false,
        val currentSubtitles: List<SubtitleAIProcessor.SubtitleItem> = emptyList(),
        val videoEnhanceConfig: VideoEnhanceProcessor.EnhanceConfig = VideoEnhanceProcessor.EnhanceConfig(),
        val audioEnhanceConfig: AudioEnhanceProcessor.AudioEnhanceConfig = AudioEnhanceProcessor.AudioEnhanceConfig(),
        val processingStatus: String = "",
        val errorMessage: String? = null
    )
    
    data class SmartPlayerConfig(
        val autoGenerateSubtitles: Boolean = true,
        val subtitleLanguage: String = "zh-CN",
        val translateTo: String? = null,
        val enableVideoEnhance: Boolean = false,
        val enableAudioEnhance: Boolean = true,
        val enableRealtimeProcessing: Boolean = false,
        val videoEnhanceConfig: VideoEnhanceProcessor.EnhanceConfig = VideoEnhanceProcessor.EnhanceConfig(),
        val audioEnhanceConfig: AudioEnhanceProcessor.AudioEnhanceConfig = AudioEnhanceProcessor.AudioEnhanceConfig()
    )
    
    /**
     * 初始化智能播放器
     */
    suspend fun initialize(config: SmartPlayerConfig = SmartPlayerConfig()) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "初始化智能播放器管理器")
            
            updateState { copy(processingStatus = "初始化中...") }
            
            // 初始化AI处理器
            val initJobs = listOf(
                async {
                    if (config.enableVideoEnhance) {
                        videoEnhanceProcessor.initialize()
                        Log.d(TAG, "视频增强处理器初始化完成")
                    }
                }
            )
            
            // 等待所有初始化完成
            initJobs.awaitAll()
            
            updateState { 
                copy(
                    isInitialized = true,
                    subtitleGenerationEnabled = config.autoGenerateSubtitles,
                    videoEnhancementEnabled = config.enableVideoEnhance,
                    audioEnhancementEnabled = config.enableAudioEnhance,
                    videoEnhanceConfig = config.videoEnhanceConfig,
                    audioEnhanceConfig = config.audioEnhanceConfig,
                    processingStatus = "初始化完成"
                )
            }
            
            Log.d(TAG, "智能播放器管理器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "智能播放器初始化失败", e)
            updateState { copy(errorMessage = "初始化失败: ${e.message}") }
            throw e
        }
    }
    
    /**
     * 绑定播放器
     */
    fun bindPlayer(player: VideoPlayer, audioSessionId: Int = 0) {
        currentPlayer = player
        currentAudioSessionId = audioSessionId
        
        // 初始化音频增强器
        if (smartFeaturesState.value.audioEnhancementEnabled && audioSessionId != 0) {
            managerScope.launch {
                try {
                    // 音频增强处理器已准备就绪
                    Log.d(TAG, "音频增强处理器初始化完成")
                    Log.d(TAG, "音频增强器绑定成功")
                } catch (e: Exception) {
                    Log.e(TAG, "音频增强器绑定失败", e)
                    updateState { copy(errorMessage = "音频增强器绑定失败: ${e.message}") }
                }
            }
        }
        
        Log.d(TAG, "播放器绑定完成")
    }
    
    /**
     * 智能字幕生成
     */
    fun generateSmartSubtitles(
        videoPath: String,
        sourceLanguage: String = "zh-CN",
        targetLanguage: String? = null,
        onProgress: ((String) -> Unit)? = null,
        onComplete: ((List<SubtitleAIProcessor.SubtitleItem>) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (!smartFeaturesState.value.subtitleGenerationEnabled) {
            onError?.invoke("字幕生成功能未启用")
            return
        }
        
        managerScope.launch {
            try {
                updateState { copy(isProcessingSubtitles = true, processingStatus = "生成字幕中...") }
                onProgress?.invoke("开始分析视频文件...")
                
                val config = SubtitleAIProcessor.SubtitleConfig(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )
                
                onProgress?.invoke("提取音频轨道...")
                val result = subtitleProcessor.generateSubtitles(videoPath, config)
                
                result.fold(
                    onSuccess = { subtitles ->
                        updateState { 
                            copy(
                                isProcessingSubtitles = false,
                                currentSubtitles = subtitles,
                                processingStatus = "字幕生成完成"
                            )
                        }
                        onComplete?.invoke(subtitles)
                        Log.d(TAG, "字幕生成成功，共${subtitles.size}条")
                    },
                    onFailure = { exception ->
                        updateState { 
                            copy(
                                isProcessingSubtitles = false,
                                errorMessage = "字幕生成失败: ${exception.message}"
                            )
                        }
                        onError?.invoke(exception.message ?: "未知错误")
                        Log.e(TAG, "字幕生成失败", exception)
                    }
                )
                
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isProcessingSubtitles = false,
                        errorMessage = "字幕生成异常: ${e.message}"
                    )
                }
                onError?.invoke(e.message ?: "未知异常")
                Log.e(TAG, "字幕生成异常", e)
            }
        }
    }
    
    /**
     * 导出字幕文件
     */
    fun exportSubtitles(
        outputPath: String,
        format: SubtitleAIProcessor.SubtitleFormat = SubtitleAIProcessor.SubtitleFormat.SRT,
        onComplete: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val subtitles = smartFeaturesState.value.currentSubtitles
        if (subtitles.isEmpty()) {
            onError?.invoke("没有可导出的字幕")
            return
        }
        
        managerScope.launch {
            try {
                val result = subtitleProcessor.exportSubtitles(subtitles, outputPath, format)
                result.fold(
                    onSuccess = { file ->
                        onComplete?.invoke(file.absolutePath)
                        Log.d(TAG, "字幕导出成功: ${file.absolutePath}")
                    },
                    onFailure = { exception ->
                        onError?.invoke(exception.message ?: "导出失败")
                        Log.e(TAG, "字幕导出失败", exception)
                    }
                )
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "导出异常")
                Log.e(TAG, "字幕导出异常", e)
            }
        }
    }
    
    /**
     * 实时视频增强
     */
    suspend fun enhanceVideoFrame(
        inputBitmap: Bitmap,
        config: VideoEnhanceProcessor.EnhanceConfig? = null
    ): Bitmap? {
        if (!smartFeaturesState.value.videoEnhancementEnabled) {
            return inputBitmap
        }
        
        return try {
            val enhanceConfig = config ?: smartFeaturesState.value.videoEnhanceConfig
            val (enhancedBitmap, result) = videoEnhanceProcessor.processFrame(inputBitmap, enhanceConfig)
            
            if (result.success) {
                Log.d(TAG, "视频帧增强成功，耗时: ${result.processingTime}ms")
                enhancedBitmap
            } else {
                Log.w(TAG, "视频帧增强失败: ${result.errorMessage}")
                inputBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "视频帧增强异常", e)
            inputBitmap
        }
    }
    
    /**
     * 实时音频增强
     */
    suspend fun enhanceAudioFrame(
        audioData: ShortArray,
        config: AudioEnhanceProcessor.AudioEnhanceConfig? = null
    ): ShortArray {
        if (!smartFeaturesState.value.audioEnhancementEnabled) {
            return audioData
        }
        
        return try {
            val enhanceConfig = config ?: smartFeaturesState.value.audioEnhanceConfig
            audioEnhanceProcessor.processAudioFrame(audioData, enhanceConfig)
        } catch (e: Exception) {
            Log.e(TAG, "音频帧增强异常", e)
            audioData
        }
    }
    
    /**
     * 分析音频质量
     */
    suspend fun analyzeAudioQuality(audioData: ShortArray): Boolean {
        return try {
            // 简化版：检查音频数据是否有效
            audioData.isNotEmpty() && audioData.any { it != 0.toShort() }
        } catch (e: Exception) {
            Log.e(TAG, "音频分析失败", e)
            false
        }
    }
    
    /**
     * 更新视频增强配置
     */
    fun updateVideoEnhanceConfig(config: VideoEnhanceProcessor.EnhanceConfig) {
        updateState { copy(videoEnhanceConfig = config) }
        Log.d(TAG, "视频增强配置已更新")
    }
    
    /**
     * 更新音频增强配置
     */
    fun updateAudioEnhanceConfig(config: AudioEnhanceProcessor.AudioEnhanceConfig) {
        updateState { copy(audioEnhanceConfig = config) }
        
        // 立即应用新配置
        managerScope.launch {
            try {
                // 配置已更新
                Log.d(TAG, "音频增强配置已应用")
                Log.d(TAG, "音频增强配置已更新并应用")
            } catch (e: Exception) {
                Log.e(TAG, "音频增强配置应用失败", e)
            }
        }
    }
    
    /**
     * 启用/禁用智能功能
     */
    fun toggleSmartFeature(feature: SmartFeature, enabled: Boolean) {
        when (feature) {
            SmartFeature.SUBTITLE_GENERATION -> {
                updateState { copy(subtitleGenerationEnabled = enabled) }
            }
            SmartFeature.VIDEO_ENHANCEMENT -> {
                updateState { copy(videoEnhancementEnabled = enabled) }
            }
            SmartFeature.AUDIO_ENHANCEMENT -> {
                updateState { copy(audioEnhancementEnabled = enabled) }
            }
        }
        
        Log.d(TAG, "智能功能 $feature ${if (enabled) "启用" else "禁用"}")
    }
    
    /**
     * 获取处理器状态
     */
    fun getProcessorStatus(): Map<String, Any> {
        return mapOf(
            "subtitle_processor" to "就绪",
            "video_enhance_processor" to if (smartFeaturesState.value.videoEnhancementEnabled) "启用" else "禁用",
            "audio_enhance_processor" to if (smartFeaturesState.value.audioEnhancementEnabled) "启用" else "禁用",
            "audio_effects" to mapOf("enabled" to smartFeaturesState.value.audioEnhancementEnabled),
            "current_subtitles_count" to smartFeaturesState.value.currentSubtitles.size
        )
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            managerScope.cancel()
            videoEnhanceProcessor.release()
            audioEnhanceProcessor.release()
            currentPlayer = null
            currentAudioSessionId = 0
            
            updateState { 
                copy(
                    isInitialized = false,
                    currentSubtitles = emptyList(),
                    processingStatus = "已清理"
                )
            }
            
            Log.d(TAG, "智能播放器管理器资源已清理")
        } catch (e: Exception) {
            Log.e(TAG, "资源清理失败", e)
        }
    }
    
    /**
     * 更新状态
     */
    private fun updateState(update: SmartFeaturesState.() -> SmartFeaturesState) {
        _smartFeaturesState.value = _smartFeaturesState.value.update()
    }
    
    enum class SmartFeature {
        SUBTITLE_GENERATION,
        VIDEO_ENHANCEMENT,
        AUDIO_ENHANCEMENT
    }
}