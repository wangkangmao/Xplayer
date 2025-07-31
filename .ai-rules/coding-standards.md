---
title: 开发规范
description: "Xplayer项目的编码标准、代码风格、最佳实践和开发工作流程"
inclusion: always
---

# Xplayer 开发规范文档

## 编码标准

### Kotlin编码规范

#### 命名规范
```kotlin
// 类名：大驼峰命名法
class SmartPlayerManager
class VideoEnhanceProcessor

// 函数名：小驼峰命名法
fun generateSmartSubtitles()
fun enhanceVideoFrame()

// 常量：全大写，下划线分隔
const val MAX_RETRY_COUNT = 3
const val DEFAULT_TIMEOUT = 5000L

// 变量：小驼峰命名法
private var currentPlayer: VideoPlayer? = null
private val smartFeaturesState = MutableStateFlow(SmartFeaturesState())

// 包名：全小写，点分隔
package com.wangkm.player.ai
package com.wangkm.xplayer.base
```

#### 代码格式化
```kotlin
// 类声明格式
class SmartPlayerManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "SmartPlayerManager"
        
        @Volatile
        private var INSTANCE: SmartPlayerManager? = null
        
        fun getInstance(context: Context): SmartPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartPlayerManager(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }
}

// 函数声明格式
suspend fun generateSmartSubtitles(
    videoPath: String,
    sourceLanguage: String = "zh-CN",
    targetLanguage: String? = null,
    onProgress: ((String) -> Unit)? = null,
    onComplete: ((List<SubtitleItem>) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    // 函数实现
}

// 链式调用格式
val config = SmartPlayerConfig(
    autoGenerateSubtitles = true,
    enableVideoEnhance = false,
    enableAudioEnhance = true
)
```

#### 注释规范
```kotlin
/**
 * 智能播放器总管理器
 * 统一管理所有AI增强功能
 * 
 * @param context 应用上下文
 * @author wangkm
 * @since 2.1.26
 */
class SmartPlayerManager(private val context: Context) {
    
    /**
     * 智能字幕生成
     * 
     * @param videoPath 视频文件路径
     * @param sourceLanguage 源语言，默认中文
     * @param targetLanguage 目标语言，为空则不翻译
     * @param onProgress 进度回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    fun generateSmartSubtitles(
        videoPath: String,
        sourceLanguage: String = "zh-CN",
        targetLanguage: String? = null,
        onProgress: ((String) -> Unit)? = null,
        onComplete: ((List<SubtitleItem>) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // TODO: 实现字幕生成逻辑
    }
}
```

### Java编码规范

#### 命名规范
```java
// 类名：大驼峰命名法
public class BaseActivity extends AppCompatActivity
public abstract class BasePlayer extends FrameLayout

// 方法名：小驼峰命名法
public void setController(BaseController controller)
protected abstract void initViews()

// 变量名：小驼峰命名法
private BaseController mController;
protected OnPlayerEventListener mOnPlayerActionListener;

// 常量：全大写，下划线分隔
public static final String MP4_URL0 = "https://example.com/video.mp4";
private static final String TAG = "BaseActivity";
```

#### 类结构顺序
```java
public class BaseActivity extends AppCompatActivity {
    // 1. 常量
    protected static final String TAG = "BaseActivity";
    public static final String MP4_URL0 = "https://example.com/video.mp4";
    
    // 2. 静态变量
    private static boolean isInitialized = false;
    
    // 3. 成员变量
    protected VideoPlayer mVideoPlayer;
    private PlayerMenuDialog mMenuDialog;
    
    // 4. 构造函数
    public BaseActivity() {
        super();
    }
    
    // 5. 生命周期方法
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    // 6. 公开方法
    public void showMenuDialog() {
        // 实现
    }
    
    // 7. 保护方法
    protected abstract P createPresenter();
    
    // 8. 私有方法
    private void setTranslucentStatus(boolean on) {
        // 实现
    }
    
    // 9. 内部类
    private static class ViewHolder {
        // 实现
    }
}
```

## 架构设计规范

### MVP模式实现
```kotlin
// Contract接口定义
interface VideoPlayerContract {
    interface View : BaseContract.BaseView {
        fun showVideo(videoUrl: String)
        fun showLoading()
        fun showError(message: String)
    }
    
    interface Presenter : BaseContract.BasePresenter<View> {
        fun loadVideo(videoId: String)
        fun playVideo()
        fun pauseVideo()
    }
}

// Presenter实现
class VideoPlayerPresenter : BasePresenter<VideoPlayerContract.View>(), 
    VideoPlayerContract.Presenter {
    
    override fun loadVideo(videoId: String) {
        mView?.showLoading()
        // 加载视频逻辑
    }
}

// Activity实现
class VideoPlayerActivity : BaseActivity<VideoPlayerPresenter>(), 
    VideoPlayerContract.View {
    
    override fun createPresenter(): VideoPlayerPresenter {
        return VideoPlayerPresenter()
    }
    
    override fun showVideo(videoUrl: String) {
        mVideoPlayer.setDataSource(videoUrl)
        mVideoPlayer.prepareAsync()
    }
}
```

### 接口设计规范
```kotlin
// 接口命名：I开头
interface IPlayerControl {
    fun play()
    fun pause()
    fun seekTo(position: Long)
}

// 监听器接口：Listener结尾
interface OnPlayerEventListener {
    fun onPlayerState(state: PlayerState, message: String)
    fun onProgress(currentDuration: Long, totalDuration: Long)
    fun onVideoSizeChanged(width: Int, height: Int)
}

// 回调接口：Callback结尾
interface OnResultCallback<T> {
    fun onSuccess(result: T)
    fun onError(errorCode: Int, errorMessage: String)
}
```

### 工厂模式实现
```kotlin
// 抽象工厂
interface MediaPlayerFactory {
    fun createPlayer(context: Context): AbstractMediaPlayer
}

// 具体工厂
class IjkPlayerFactory : MediaPlayerFactory {
    override fun createPlayer(context: Context): AbstractMediaPlayer {
        return IjkMediaPlayer(context)
    }
}

class ExoPlayerFactory : MediaPlayerFactory {
    override fun createPlayer(context: Context): AbstractMediaPlayer {
        return ExoMediaPlayer(context)
    }
}

// 工厂管理器
object MediaPlayerFactoryManager {
    fun getFactory(type: PlayerType): MediaPlayerFactory {
        return when (type) {
            PlayerType.IJK -> IjkPlayerFactory()
            PlayerType.EXO -> ExoPlayerFactory()
            PlayerType.SYSTEM -> SystemPlayerFactory()
        }
    }
}
```

## 异步编程规范

### Kotlin Coroutines使用
```kotlin
class SmartPlayerManager {
    // 协程作用域
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 异步函数标准格式
    suspend fun generateSmartSubtitles(
        videoPath: String
    ): Result<List<SubtitleItem>> = withContext(Dispatchers.IO) {
        try {
            updateState { copy(processingStatus = "生成字幕中...") }
            
            val result = subtitleProcessor.generateSubtitles(videoPath)
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "字幕生成失败", e)
            Result.failure(e)
        }
    }
    
    // 启动协程的标准方式
    fun startAsyncTask() {
        managerScope.launch {
            try {
                val result = generateSmartSubtitles("video.mp4")
                result.fold(
                    onSuccess = { subtitles ->
                        // 处理成功结果
                    },
                    onFailure = { exception ->
                        // 处理错误
                    }
                )
            } catch (e: Exception) {
                // 处理异常
            }
        }
    }
}
```

### StateFlow/Flow使用规范
```kotlin
class SmartPlayerManager {
    // 私有可变状态
    private val _smartFeaturesState = MutableStateFlow(SmartFeaturesState())
    
    // 公开只读状态
    val smartFeaturesState: StateFlow<SmartFeaturesState> = _smartFeaturesState.asStateFlow()
    
    // 状态更新函数
    private fun updateState(update: SmartFeaturesState.() -> SmartFeaturesState) {
        _smartFeaturesState.value = _smartFeaturesState.value.update()
    }
    
    // Flow转换示例
    val isProcessing: Flow<Boolean> = smartFeaturesState
        .map { it.isProcessingSubtitles || it.isProcessingVideo }
        .distinctUntilChanged()
}
```

## UI开发规范

### Jetpack Compose规范
```kotlin
// Composable函数命名：大驼峰命名法
@Composable
fun SmartPlayerScreen(
    modifier: Modifier = Modifier,
    onVideoSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    // 状态管理
    val context = LocalContext.current
    val smartPlayerManager = remember { SmartPlayerManager.getInstance(context) }
    val state by smartPlayerManager.smartFeaturesState.collectAsStateWithLifecycle()
    
    // 本地状态
    var showDialog by remember { mutableStateOf(false) }
    
    // UI结构
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // UI内容
    }
}

// 私有Composable函数
@Composable
private fun SmartFeatureCard(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        // 卡片内容
    }
}
```

### 传统View规范
```xml
<!-- 布局文件命名规范 -->
<!-- activity_smart_video_player.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- ID命名：功能_类型_描述 -->
    <TextView
        android:id="@+id/tv_title_main"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/app_name" />
        
    <com.wangkm.xplayer.widget.VideoPlayer
        android:id="@+id/video_player_main"
        android:layout_width="match_parent"
        android:layout_height="200dp" />
        
</LinearLayout>
```

## 资源管理规范

### 字符串资源
```xml
<!-- strings.xml -->
<resources>
    <!-- 应用基础信息 -->
    <string name="app_name">Xplayer</string>
    <string name="app_version">2.1.26.1</string>
    
    <!-- 播放器功能 -->
    <string name="player_play">播放</string>
    <string name="player_pause">暂停</string>
    <string name="player_fullscreen">全屏</string>
    <string name="player_window">窗口播放</string>
    
    <!-- AI功能 -->
    <string name="ai_subtitle_generate">生成字幕</string>
    <string name="ai_video_enhance">视频增强</string>
    <string name="ai_audio_enhance">音频增强</string>
    
    <!-- 错误信息 -->
    <string name="error_network_unavailable">网络不可用</string>
    <string name="error_file_not_found">文件未找到</string>
    <string name="error_ai_processing_failed">AI处理失败</string>
    
    <!-- 提示信息 -->
    <string name="tip_generating_subtitle">正在生成字幕，请稍候...</string>
    <string name="tip_enhancing_video">正在增强视频质量...</string>
</resources>
```

### 颜色资源
```xml
<!-- colors.xml -->
<resources>
    <!-- Material Design 3 主题色 -->
    <color name="md_theme_light_primary">#6750A4</color>
    <color name="md_theme_light_on_primary">#FFFFFF</color>
    <color name="md_theme_light_primary_container">#EADDFF</color>
    
    <!-- 播放器专用色彩 -->
    <color name="player_background">#000000</color>
    <color name="player_control_background">#80000000</color>
    <color name="subtitle_background">#CC000000</color>
    <color name="subtitle_text">#FFFFFF</color>
    
    <!-- AI功能色彩 -->
    <color name="ai_processing">#2196F3</color>
    <color name="ai_success">#4CAF50</color>
    <color name="ai_error">#F44336</color>
</resources>
```

## 错误处理规范

### 异常处理标准
```kotlin
class SmartPlayerManager {
    
    // 标准异常处理模式
    suspend fun generateSmartSubtitles(videoPath: String): Result<List<SubtitleItem>> {
        return try {
            // 参数验证
            require(videoPath.isNotEmpty()) { "视频路径不能为空" }
            require(File(videoPath).exists()) { "视频文件不存在" }
            
            // 执行处理
            val result = subtitleProcessor.generateSubtitles(videoPath)
            
            Result.success(result)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "参数错误: ${e.message}", e)
            Result.failure(AIProcessingException("参数错误", e))
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "文件未找到: ${e.message}", e)
            Result.failure(AIProcessingException("文件未找到", e))
        } catch (e: Exception) {
            Log.e(TAG, "AI处理失败: ${e.message}", e)
            Result.failure(AIProcessingException("AI处理失败", e))
        }
    }
    
    // 自定义异常类
    class AIProcessingException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)
}
```

### 日志记录规范
```kotlin
object Logger {
    private const val TAG = "Xplayer"
    
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG-$tag", message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG-$tag", message, throwable)
        // 可以添加崩溃报告
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG-$tag", message)
    }
}

// 使用示例
class SmartPlayerManager {
    companion object {
        private const val TAG = "SmartPlayerManager"
    }
    
    fun initialize() {
        Logger.d(TAG, "开始初始化智能播放器管理器")
        try {
            // 初始化逻辑
            Logger.i(TAG, "智能播放器管理器初始化完成")
        } catch (e: Exception) {
            Logger.e(TAG, "智能播放器管理器初始化失败", e)
        }
    }
}
```

## 性能优化规范

### 内存管理
```kotlin
class SmartPlayerManager {
    
    // 使用WeakReference避免内存泄漏
    private var currentPlayerRef: WeakReference<VideoPlayer>? = null
    
    // 及时释放资源
    fun cleanup() {
        try {
            managerScope.cancel()
            videoEnhanceProcessor.release()
            audioEnhanceProcessor.release()
            currentPlayerRef?.clear()
            currentPlayerRef = null
        } catch (e: Exception) {
            Logger.e(TAG, "资源清理失败", e)
        }
    }
    
    // 使用对象池减少内存分配
    private val subtitleItemPool = object : Pools.SynchronizedPool<SubtitleItem>(10) {
        override fun newInstance(): SubtitleItem {
            return SubtitleItem()
        }
    }
}
```

### 网络请求优化
```kotlin
class NetworkManager {
    
    // 使用单例OkHttpClient
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .build()
    
    // 请求重试机制
    suspend fun downloadVideo(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(3) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()
                    
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    return@withContext Result.success(response.body?.bytes() ?: ByteArray(0))
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) {
                    delay(1000 * (attempt + 1)) // 指数退避
                }
            }
        }
        
        Result.failure(lastException ?: Exception("下载失败"))
    }
}
```

## 测试规范

### 单元测试
```kotlin
class SmartPlayerManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSubtitleProcessor: SubtitleAIProcessor
    
    private lateinit var smartPlayerManager: SmartPlayerManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        smartPlayerManager = SmartPlayerManager(mockContext)
    }
    
    @Test
    fun `当视频路径为空时，字幕生成应该失败`() = runTest {
        // Given
        val emptyVideoPath = ""
        
        // When
        val result = smartPlayerManager.generateSmartSubtitles(emptyVideoPath)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
    
    @Test
    fun `当字幕生成成功时，应该更新状态`() = runTest {
        // Given
        val videoPath = "test_video.mp4"
        val expectedSubtitles = listOf(
            SubtitleItem(0, 1000, "测试字幕", 1.0f)
        )
        
        // When
        val result = smartPlayerManager.generateSmartSubtitles(videoPath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedSubtitles, result.getOrNull())
    }
}
```

### UI测试
```kotlin
@RunWith(AndroidJUnit4::class)
class SmartPlayerScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testSmartPlayerScreenDisplaysCorrectly() {
        composeTestRule.setContent {
            SmartPlayerScreen()
        }
        
        // 验证界面元素显示
        composeTestRule
            .onNodeWithText("智能播放器")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("AI智能字幕")
            .assertIsDisplayed()
    }
    
    @Test
    fun testVideoSelectionTriggersCallback() {
        var selectedVideo = ""
        
        composeTestRule.setContent {
            SmartPlayerScreen(
                onVideoSelected = { videoUrl ->
                    selectedVideo = videoUrl
                }
            )
        }
        
        // 执行点击操作
        composeTestRule
            .onNodeWithText("开始视频播放演示")
            .performClick()
            
        // 验证回调被触发
        assertNotEquals("", selectedVideo)
    }
}
```

这些开发规范确保了Xplayer项目代码的一致性、可维护性和高质量，为团队协作和项目长期发展提供了坚实的基础。