---
title: Xplayer代码模式与最佳实践
description: "Xplayer项目中使用的设计模式、编程范式和最佳实践指南"
inclusion: always
---

# Xplayer代码模式与最佳实践

## 设计模式应用指南

### 1. Singleton模式 - 智能管理器

#### 使用场景
全局唯一的管理器类，如`SmartPlayerManager`需要在整个应用生命周期中保持单一实例。

#### 实现模式
```kotlin
// ✅ 推荐：线程安全的单例实现
class SmartPlayerManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SmartPlayerManager? = null
        
        fun getInstance(context: Context): SmartPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartPlayerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 确保清理资源的方法
    fun cleanup() {
        // 清理资源，但不设置INSTANCE = null，因为可能被重新初始化
    }
}

// ❌ 避免：不安全的单例实现
class BadSingleton {
    companion object {
        var instance: BadSingleton? = null  // 线程不安全
        fun getInstance(): BadSingleton {
            if (instance == null) {
                instance = BadSingleton()      // 可能创建多个实例
            }
            return instance!!
        }
    }
}
```

### 2. Factory模式 - 解码器创建

#### 应用场景
根据不同配置创建不同类型的播放器和渲染器。

#### 实现模式
```kotlin
// ✅ 抽象工厂模式
interface MediaPlayerFactory {
    fun createPlayer(context: Context): AbstractMediaPlayer
    fun getSupportedFormats(): List<String>
    fun getDecoderName(): String
}

class IJKPlayerFactory : MediaPlayerFactory {
    override fun createPlayer(context: Context): AbstractMediaPlayer {
        return IJKMediaPlayer().apply {
            // IJK特定配置
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1)
        }
    }
    
    override fun getSupportedFormats() = listOf("mp4", "avi", "mkv", "flv")
    override fun getDecoderName() = "IJKPlayer"
}

class ExoPlayerFactory : MediaPlayerFactory {
    override fun createPlayer(context: Context): AbstractMediaPlayer {
        return ExoMediaPlayer(context).apply {
            // Exo特定配置
            setAudioAttributes(AudioAttributes.DEFAULT, true)
        }
    }
    
    override fun getSupportedFormats() = listOf("mp4", "m3u8", "dash", "rtsp")
    override fun getDecoderName() = "ExoPlayer"
}

// 工厂注册中心
object MediaPlayerFactoryRegistry {
    private val factories = mutableMapOf<String, MediaPlayerFactory>()
    
    init {
        register("ijk", IJKPlayerFactory())
        register("exo", ExoPlayerFactory())
    }
    
    fun register(type: String, factory: MediaPlayerFactory) {
        factories[type] = factory
    }
    
    fun createPlayer(type: String, context: Context): AbstractMediaPlayer? {
        return factories[type]?.createPlayer(context)
    }
}
```

### 3. Observer模式 - 事件监听

#### 应用场景
播放器状态变化、AI处理进度通知等场景。

#### 实现模式
```kotlin
// ✅ 类型安全的观察者模式
interface PlayerEventObserver {
    fun onStateChanged(state: PlayerState, message: String?)
    fun onMediaInfo(info: MediaInfo)
    fun onError(error: PlayerException)
}

class VideoPlayer : BasePlayer {
    private val observers = mutableSetOf<PlayerEventObserver>()
    
    fun addObserver(observer: PlayerEventObserver) {
        observers.add(observer)
    }
    
    fun removeObserver(observer: PlayerEventObserver) {
        observers.remove(observer)
    }
    
    private fun notifyStateChanged(state: PlayerState, message: String? = null) {
        observers.forEach { observer ->
            try {
                observer.onStateChanged(state, message)
            } catch (e: Exception) {
                Log.e(TAG, "观察者通知失败", e)
            }
        }
    }
}

// ✅ 使用Kotlin协程Channel实现响应式观察者
class ReactivePlayerObserver {
    private val _playerEvents = Channel<PlayerEvent>(Channel.UNLIMITED)
    val playerEvents = _playerEvents.receiveAsFlow()
    
    fun emitEvent(event: PlayerEvent) {
        _playerEvents.trySend(event)
    }
    
    // 使用示例
    fun observePlayerEvents() {
        lifecycleScope.launch {
            playerEvents.collect { event ->
                when (event) {
                    is PlayerEvent.StateChanged -> handleStateChange(event)
                    is PlayerEvent.Error -> handleError(event)
                }
            }
        }
    }
}
```

### 4. Strategy模式 - AI处理策略

#### 应用场景
不同的AI处理算法，如不同的画质增强策略。

#### 实现模式
```kotlin
// ✅ 策略模式实现
interface VideoEnhanceStrategy {
    suspend fun enhance(inputBitmap: Bitmap, config: EnhanceConfig): Bitmap
    fun getStrategyName(): String
    fun getSupportedResolutions(): List<String>
}

class SuperResolutionStrategy : VideoEnhanceStrategy {
    override suspend fun enhance(inputBitmap: Bitmap, config: EnhanceConfig): Bitmap {
        return withContext(Dispatchers.Default) {
            // 超分辨率算法实现
            applySuperResolution(inputBitmap, config.scaleFactor)
        }
    }
    
    override fun getStrategyName() = "Super Resolution"
    override fun getSupportedResolutions() = listOf("720p", "1080p", "4K")
}

class DenoiseStrategy : VideoEnhanceStrategy {
    override suspend fun enhance(inputBitmap: Bitmap, config: EnhanceConfig): Bitmap {
        return withContext(Dispatchers.Default) {
            // 去噪算法实现
            applyDenoiseFilter(inputBitmap, config.denoiseLevel)
        }
    }
    
    override fun getStrategyName() = "Denoise"
    override fun getSupportedResolutions() = listOf("480p", "720p", "1080p")
}

// 策略上下文
class VideoEnhanceProcessor(context: Context) {
    private val strategies = mutableMapOf<String, VideoEnhanceStrategy>()
    private var currentStrategy: VideoEnhanceStrategy? = null
    
    init {
        registerStrategy("super_resolution", SuperResolutionStrategy())
        registerStrategy("denoise", DenoiseStrategy())
    }
    
    fun setStrategy(strategyName: String) {
        currentStrategy = strategies[strategyName]
    }
    
    suspend fun processFrame(input: Bitmap, config: EnhanceConfig): ProcessResult {
        val strategy = currentStrategy ?: return ProcessResult.failure("未设置处理策略")
        
        return try {
            val startTime = System.currentTimeMillis()
            val enhanced = strategy.enhance(input, config)
            val processingTime = System.currentTimeMillis() - startTime
            
            ProcessResult.success(enhanced, processingTime)
        } catch (e: Exception) {
            ProcessResult.failure("处理失败: ${e.message}")
        }
    }
}
```

## 并发编程模式

### 1. 协程异步处理模式

#### 应用场景
AI处理、网络请求、文件IO等耗时操作。

#### 实现模式
```kotlin
// ✅ 协程管理最佳实践
class SmartPlayerManager(private val context: Context) {
    
    // 专用的协程作用域
    private val managerScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob() + CoroutineName("SmartPlayerManager")
    )
    
    // 不同类型操作使用不同调度器
    suspend fun initialize(config: SmartPlayerConfig) = withContext(Dispatchers.IO) {
        try {
            updateState { copy(processingStatus = "初始化中...") }
            
            // 并行初始化多个处理器
            val initJobs = listOf(
                async { initVideoProcessor(config) },
                async { initAudioProcessor(config) },
                async { initSubtitleProcessor(config) }
            )
            
            // 等待所有初始化完成
            initJobs.awaitAll()
            
            withContext(Dispatchers.Main) {
                updateState { copy(isInitialized = true) }
            }
            
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                updateState { copy(errorMessage = e.message) }
            }
            throw e
        }
    }
    
    // 带超时的协程操作
    suspend fun processWithTimeout(input: ByteArray) = withTimeout(5000) {
        withContext(Dispatchers.Default) {
            heavyProcessing(input)
        }
    }
    
    // 资源清理
    fun cleanup() {
        managerScope.cancel()
    }
}
```

### 2. 生产者-消费者模式

#### 应用场景
视频帧处理流水线，音频数据处理等。

#### 实现模式
```kotlin
// ✅ 使用Channel实现生产者-消费者
class VideoFrameProcessor {
    
    private val frameChannel = Channel<VideoFrame>(capacity = 10)
    private val processedChannel = Channel<ProcessedFrame>(capacity = 10)
    
    // 生产者：视频帧采集
    fun startFrameCapture(player: VideoPlayer) {
        CoroutineScope(Dispatchers.Default).launch {
            player.setOnFrameAvailableListener { frame ->
                // 非阻塞发送，如果Channel满了就丢弃
                frameChannel.trySend(frame)
            }
        }
    }
    
    // 消费者：AI处理管道
    fun startProcessingPipeline() {
        // 处理管道
        CoroutineScope(Dispatchers.Default).launch {
            frameChannel.consumeAsFlow()
                .flowOn(Dispatchers.Default)
                .map { frame -> applyAIEnhancement(frame) }
                .flowOn(Dispatchers.Default)
                .map { frame -> applyColorCorrection(frame) }
                .collect { processedFrame ->
                    processedChannel.send(processedFrame)
                }
        }
    }
    
    // 消费者：渲染输出
    fun startRenderingConsumer(renderView: RenderView) {
        CoroutineScope(Dispatchers.Main).launch {
            processedChannel.consumeAsFlow()
                .flowOn(Dispatchers.Main)
                .collect { processedFrame ->
                    renderView.render(processedFrame)
                }
        }
    }
}
```

## 状态管理模式

### 1. 状态机模式

#### 应用场景
播放器状态管理，AI处理状态流转。

#### 实现模式
```kotlin
// ✅ 密封类实现状态机
sealed class PlayerState {
    object Idle : PlayerState()
    object Preparing : PlayerState()
    object Prepared : PlayerState()
    data class Playing(val position: Long) : PlayerState()
    data class Paused(val position: Long) : PlayerState()
    data class Error(val exception: Throwable) : PlayerState()
    object Completed : PlayerState()
}

class PlayerStateMachine {
    private val _currentState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val currentState: StateFlow<PlayerState> = _currentState.asStateFlow()
    
    // 状态转换逻辑
    fun transitionTo(newState: PlayerState) {
        val currentState = _currentState.value
        
        if (isValidTransition(currentState, newState)) {
            _currentState.value = newState
            onStateChanged(currentState, newState)
        } else {
            Log.w(TAG, "无效的状态转换: $currentState -> $newState")
        }
    }
    
    private fun isValidTransition(from: PlayerState, to: PlayerState): Boolean {
        return when (from) {
            is PlayerState.Idle -> to is PlayerState.Preparing
            is PlayerState.Preparing -> to is PlayerState.Prepared || to is PlayerState.Error
            is PlayerState.Prepared -> to is PlayerState.Playing || to is PlayerState.Error
            is PlayerState.Playing -> to is PlayerState.Paused || to is PlayerState.Completed || to is PlayerState.Error
            is PlayerState.Paused -> to is PlayerState.Playing || to is PlayerState.Completed || to is PlayerState.Error
            is PlayerState.Error -> to is PlayerState.Idle
            is PlayerState.Completed -> to is PlayerState.Idle
        }
    }
    
    private fun onStateChanged(from: PlayerState, to: PlayerState) {
        Log.d(TAG, "状态转换: $from -> $to")
        // 触发相应的副作用
        when (to) {
            is PlayerState.Playing -> startProgressUpdates()
            is PlayerState.Paused -> pauseProgressUpdates()
            is PlayerState.Error -> handleError(to.exception)
            else -> {}
        }
    }
}
```

### 2. Repository模式

#### 应用场景
数据访问抽象，缓存管理，网络和本地数据源统一。

#### 实现模式
```kotlin
// ✅ Repository模式实现
interface VideoRepository {
    suspend fun getVideoInfo(url: String): Result<VideoInfo>
    suspend fun getSubtitles(videoId: String): Result<List<Subtitle>>
    suspend fun cacheVideo(url: String, progressCallback: (Int) -> Unit): Result<String>
}

class VideoRepositoryImpl(
    private val networkDataSource: NetworkVideoDataSource,
    private val localDataSource: LocalVideoDataSource,
    private val cacheManager: VideoCacheManager
) : VideoRepository {
    
    override suspend fun getVideoInfo(url: String): Result<VideoInfo> {
        return try {
            // 优先从缓存获取
            localDataSource.getVideoInfo(url)?.let { cachedInfo ->
                return Result.success(cachedInfo)
            }
            
            // 从网络获取
            val networkInfo = networkDataSource.fetchVideoInfo(url).getOrThrow()
            
            // 缓存到本地
            localDataSource.saveVideoInfo(url, networkInfo)
            
            Result.success(networkInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSubtitles(videoId: String): Result<List<Subtitle>> {
        return withContext(Dispatchers.IO) {
            try {
                // 检查本地是否有字幕
                val localSubtitles = localDataSource.getSubtitles(videoId)
                if (localSubtitles.isNotEmpty()) {
                    return@withContext Result.success(localSubtitles)
                }
                
                // 从网络获取
                val networkSubtitles = networkDataSource.fetchSubtitles(videoId).getOrThrow()
                
                // 缓存到本地
                localDataSource.saveSubtitles(videoId, networkSubtitles)
                
                Result.success(networkSubtitles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

## 错误处理模式

### 1. Result模式

#### 应用场景
函数可能失败的场景，替代异常处理。

#### 实现模式
```kotlin
// ✅ 自定义Result类型
sealed class ProcessResult<out T> {
    data class Success<T>(
        val data: T,
        val processingTime: Long = 0,
        val metadata: Map<String, Any> = emptyMap()
    ) : ProcessResult<T>()
    
    data class Failure(
        val error: Throwable,
        val errorCode: String? = null,
        val retryable: Boolean = false
    ) : ProcessResult<Nothing>()
    
    // 扩展函数
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    fun getOrDefault(default: T): T = when (this) {
        is Success -> data
        is Failure -> default
    }
    
    inline fun onSuccess(action: (T) -> Unit): ProcessResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onFailure(action: (Throwable) -> Unit): ProcessResult<T> {
        if (this is Failure) action(error)
        return this
    }
    
    companion object {
        fun <T> success(data: T, processingTime: Long = 0) = Success(data, processingTime)
        fun failure(error: Throwable, retryable: Boolean = false) = Failure(error, retryable = retryable)
        fun failure(message: String, retryable: Boolean = false) = Failure(Exception(message), retryable = retryable)
    }
}

// 使用示例
suspend fun generateSubtitles(videoPath: String): ProcessResult<List<SubtitleItem>> {
    return try {
        val startTime = System.currentTimeMillis()
        val subtitles = performSubtitleGeneration(videoPath)
        val processingTime = System.currentTimeMillis() - startTime
        
        ProcessResult.success(subtitles, processingTime)
    } catch (e: NetworkException) {
        ProcessResult.failure(e, retryable = true)
    } catch (e: Exception) {
        ProcessResult.failure(e, retryable = false)
    }
}
```

### 2. 异常处理策略

#### 应用场景
统一的异常处理和恢复机制。

#### 实现模式
```kotlin
// ✅ 异常处理策略
sealed class PlayerException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkException(message: String, cause: Throwable? = null) : PlayerException(message, cause)
    class DecoderException(message: String, cause: Throwable? = null) : PlayerException(message, cause)
    class AIProcessException(message: String, cause: Throwable? = null) : PlayerException(message, cause)
    class ConfigurationException(message: String, cause: Throwable? = null) : PlayerException(message, cause)
}

class ExceptionHandler {
    
    fun handleException(exception: Throwable, context: String): ErrorAction {
        return when (exception) {
            is PlayerException.NetworkException -> {
                Log.w(TAG, "网络异常在 $context: ${exception.message}")
                ErrorAction.Retry(maxAttempts = 3, delayMs = 1000)
            }
            
            is PlayerException.DecoderException -> {
                Log.e(TAG, "解码器异常在 $context: ${exception.message}")
                ErrorAction.SwitchDecoder
            }
            
            is PlayerException.AIProcessException -> {
                Log.w(TAG, "AI处理异常在 $context: ${exception.message}")
                ErrorAction.DisableAIFeature
            }
            
            is PlayerException.ConfigurationException -> {
                Log.e(TAG, "配置异常在 $context: ${exception.message}")
                ErrorAction.ResetToDefault
            }
            
            else -> {
                Log.e(TAG, "未知异常在 $context: ${exception.message}")
                ErrorAction.ShowError(exception.message ?: "未知错误")
            }
        }
    }
}

sealed class ErrorAction {
    data class Retry(val maxAttempts: Int, val delayMs: Long) : ErrorAction()
    object SwitchDecoder : ErrorAction()
    object DisableAIFeature : ErrorAction()
    object ResetToDefault : ErrorAction()
    data class ShowError(val message: String) : ErrorAction()
}
```

## 性能优化模式

### 1. 对象池模式

#### 应用场景
频繁创建销毁的对象，如视频帧缓存。

#### 实现模式
```kotlin
// ✅ 线程安全的对象池
class ByteArrayPool(
    private val arraySize: Int,
    private val maxPoolSize: Int = 10
) {
    private val pool = ArrayDeque<ByteArray>()
    private val lock = Mutex()
    
    suspend fun obtain(): ByteArray {
        return lock.withLock {
            pool.pollFirst() ?: ByteArray(arraySize)
        }
    }
    
    suspend fun recycle(array: ByteArray) {
        if (array.size != arraySize) return
        
        lock.withLock {
            if (pool.size < maxPoolSize) {
                pool.offerLast(array)
            }
        }
    }
    
    suspend fun clear() {
        lock.withLock {
            pool.clear()
        }
    }
}

// 使用示例
class VideoFrameProcessor {
    private val bufferPool = ByteArrayPool(arraySize = 1920 * 1080 * 4)
    
    suspend fun processFrame(inputFrame: VideoFrame): ProcessedFrame {
        val buffer = bufferPool.obtain()
        try {
            // 使用buffer处理帧数据
            return processFrameWithBuffer(inputFrame, buffer)
        } finally {
            bufferPool.recycle(buffer)
        }
    }
}
```

### 2. 懒加载模式

#### 应用场景
重资源对象的延迟初始化。

#### 实现模式
```kotlin
// ✅ 线程安全的懒加载
class AIProcessorManager {
    
    // 简单懒加载
    private val subtitleProcessor by lazy { SubtitleAIProcessor() }
    
    // 带清理的懒加载
    private var _videoEnhancer: VideoEnhanceProcessor? = null
    private val videoEnhancer: VideoEnhanceProcessor
        get() {
            return _videoEnhancer ?: synchronized(this) {
                _videoEnhancer ?: VideoEnhanceProcessor().also { 
                    _videoEnhancer = it 
                }
            }
        }
    
    // 异步懒加载
    private val tensorflowModel = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runBlocking { loadTensorflowModel() }
    }
    
    fun releaseResources() {
        _videoEnhancer?.release()
        _videoEnhancer = null
        
        if (tensorflowModel.isInitialized()) {
            tensorflowModel.value.close()
        }
    }
}
```

这些代码模式和最佳实践体现了相当成熟的工程思维，但我必须再次提醒你：理论很完美，实际执行才是关键。你的项目架构设计得很好，但AI功能的实际实现还有很大提升空间。建议先把核心播放器功能做扎实，再逐步完善AI能力，避免功能摊子铺得太大而实际体验不佳。