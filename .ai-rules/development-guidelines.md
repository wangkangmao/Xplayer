---
title: Xplayer开发规范
description: "Xplayer项目的代码规范、开发流程和质量标准"
inclusion: always
---

# Xplayer开发规范指南

## 代码规范标准

### 1. Kotlin编码规范

#### 命名约定
```kotlin
// ✅ 正确示例
class SmartPlayerManager           // 类名：大驼峰命名
fun generateSmartSubtitles()      // 函数名：小驼峰命名
private val smartFeaturesState   // 属性名：小驼峰命名
const val DEFAULT_BUFFER_SIZE    // 常量：全大写下划线分隔

// ❌ 错误示例  
class smartplayermanager         // 类名应大驼峰
fun GenerateSmartSubtitles()     // 函数名应小驼峰
val smart_features_state         // 属性名应小驼峰
const val defaultBufferSize      // 常量应全大写
```

#### 文件组织结构
```kotlin
// 文件头部结构
package com.wangkm.player.ai

import android.content.Context
import kotlinx.coroutines.*
// ... 其他导入

/**
 * 智能播放器管理器
 * 
 * 负责统一管理所有AI增强功能，包括：
 * - 智能字幕生成
 * - 视频画质增强  
 * - 音频智能优化
 * 
 * @author wangkm
 * @since 2.0.0
 */
class SmartPlayerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SmartPlayerManager"
        // 常量定义
    }
    
    // 私有属性
    private val subtitleProcessor by lazy { SubtitleAIProcessor(context) }
    
    // 公开属性
    val smartFeaturesState: StateFlow<SmartFeaturesState> = _smartFeaturesState.asStateFlow()
    
    // 构造函数和初始化
    init {
        // 初始化逻辑
    }
    
    // 公开方法
    suspend fun initialize() {
        // 实现
    }
    
    // 私有方法
    private fun updateState(update: SmartFeaturesState.() -> SmartFeaturesState) {
        // 实现
    }
    
    // 内部类和枚举
    enum class SmartFeature {
        SUBTITLE_GENERATION,
        VIDEO_ENHANCEMENT,
        AUDIO_ENHANCEMENT
    }
}
```

#### 函数设计原则
```kotlin
// ✅ 良好的函数设计
suspend fun generateSmartSubtitles(
    videoPath: String,
    sourceLanguage: String = "zh-CN",
    targetLanguage: String? = null,
    onProgress: ((String) -> Unit)? = null,
    onComplete: ((List<SubtitleItem>) -> Unit)? = null,
    onError: ((String) -> Unit)? = null
) {
    // 1. 参数验证
    require(videoPath.isNotEmpty()) { "视频路径不能为空" }
    
    // 2. 状态检查
    if (!smartFeaturesState.value.subtitleGenerationEnabled) {
        onError?.invoke("字幕生成功能未启用")
        return
    }
    
    // 3. 主要逻辑
    try {
        updateState { copy(isProcessingSubtitles = true) }
        // 处理逻辑...
    } catch (e: Exception) {
        Log.e(TAG, "字幕生成失败", e)
        onError?.invoke(e.message ?: "未知错误")
    } finally {
        updateState { copy(isProcessingSubtitles = false) }
    }
}

// ❌ 避免的做法
fun processVideo(path: String, mode: Int, callback: Any?) {
    // 1. 参数类型不明确
    // 2. 缺少参数验证
    // 3. 异常处理不完整
    // 4. 没有日志记录
}
```

### 2. Java编码规范 (兼容代码)

#### 类设计规范
```java
// ✅ 标准Java类结构
public class BasePlayer extends FrameLayout implements IVideoPlayer {
    
    private static final String TAG = "BasePlayer";
    private static final int DEFAULT_TIMEOUT = 30000;
    
    // 成员变量
    private VideoController mController;
    private AbstractMediaPlayer mMediaPlayer;
    
    // 构造函数
    public BasePlayer(Context context) {
        this(context, null);
    }
    
    public BasePlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public BasePlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }
    
    // 生命周期方法
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 实现
    }
    
    // 接口实现
    @Override
    public void setDataSource(String url) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException("数据源不能为空");
        }
        // 实现
    }
    
    // 私有方法
    private void initViews() {
        // 初始化视图
    }
}
```

### 3. Compose UI规范

#### 组件设计规范
```kotlin
// ✅ 标准Compose组件
@Composable
fun SmartPlayerScreen(
    modifier: Modifier = Modifier,
    onVideoSelected: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    var selectedVideoPath by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 组件内容
    }
}

// 预览函数
@Preview(showBackground = true)
@Composable
private fun SmartPlayerScreenPreview() {
    MaterialTheme {
        SmartPlayerScreen(
            onVideoSelected = {},
            onSettingsClick = {}
        )
    }
}
```

## 项目结构规范

### 1. 模块划分规则
```
app/                          # 应用主模块
├── ai/                      # AI功能模块 (business logic)
│   ├── SmartPlayerManager.kt      # 主管理器
│   ├── SubtitleAIProcessor.kt     # 字幕处理器
│   ├── VideoEnhanceProcessor.kt   # 视频增强处理器
│   └── AudioEnhanceProcessor.kt   # 音频增强处理器
├── ui/                      # 用户界面模块
│   ├── activity/           # Activity层
│   ├── compose/            # Compose UI组件
│   └── widget/             # 自定义View组件
├── manager/                 # 业务管理器
│   └── PlayerManager.java        # 播放器管理
├── utils/                   # 工具类
│   ├── Logger.kt                 # 日志工具
│   ├── ScreenUtils.kt            # 屏幕工具
│   └── SharedPreferencesUtil.java # 偏好设置工具
└── demo/                    # 演示功能
    └── VideoPlayerDemoActivity.kt
```

### 2. 资源文件规范
```
res/
├── layout/                  # 布局文件
│   ├── activity_*.xml              # Activity布局 
│   ├── fragment_*.xml              # Fragment布局
│   ├── view_*.xml                  # 自定义View布局
│   └── dialog_*.xml                # 对话框布局
├── drawable/                # 图标和背景
│   ├── ic_*                        # 图标前缀
│   ├── bg_*                        # 背景前缀  
│   └── selector_*                  # 选择器前缀
├── values/                  # 数值资源
│   ├── strings.xml                 # 字符串资源
│   ├── colors.xml                  # 颜色资源
│   ├── dimens.xml                  # 尺寸资源
│   └── styles.xml                  # 样式资源
└── mipmap-*/               # 启动图标
```

### 3. 包名规范
```kotlin
// 基础包结构
com.wangkm.player                    # 根包
├── ai                              # AI功能
├── ui                              # 用户界面
│   ├── activity                    # Activity
│   ├── compose                     # Compose组件
│   └── widget                      # 自定义View
├── manager                         # 管理器
├── utils                           # 工具类
├── demo                            # 演示功能
└── MainActivity                    # 主Activity

// SDK包结构  
com.wangkm.xplayer                  # 播放器SDK根包
├── base                            # 基础类
├── controller                      # 控制器
├── interfaces                      # 接口定义
├── listener                        # 监听器
├── manager                         # 管理器
├── media                           # 媒体处理
├── model                           # 数据模型
├── utils                           # 工具类
└── widget                          # UI组件
```

## 开发流程规范

### 1. Git工作流
```bash
# 分支命名规范
main                    # 主分支，稳定发布版本
develop                 # 开发分支，最新开发代码
feature/ai-subtitle     # 功能分支：新功能开发
bugfix/player-crash     # 修复分支：Bug修复
hotfix/v2.1.1          # 热修复分支：紧急修复
release/v2.2.0         # 发布分支：版本发布准备

# 提交信息规范
feat: 新增AI字幕生成功能
fix: 修复播放器崩溃问题
docs: 更新README文档
style: 代码格式调整
refactor: 重构视频处理逻辑
test: 添加单元测试
chore: 更新依赖版本
```

### 2. 代码审查检查清单
```markdown
## 功能性检查
- [ ] 功能是否按需求正确实现
- [ ] 边界条件是否正确处理
- [ ] 异常情况是否妥善处理
- [ ] 性能是否满足要求

## 代码质量检查  
- [ ] 代码风格是否符合规范
- [ ] 命名是否清晰易懂
- [ ] 注释是否完整准确
- [ ] 是否存在代码重复

## 架构设计检查
- [ ] 模块划分是否合理
- [ ] 接口设计是否清晰
- [ ] 依赖关系是否正确
- [ ] 是否遵循设计模式

## 测试覆盖检查
- [ ] 单元测试是否充分
- [ ] 集成测试是否完整
- [ ] 边界测试是否覆盖
- [ ] 性能测试是否满足
```

## 质量保证标准

### 1. 代码质量指标
```kotlin
// ✅ 良好的错误处理
class SmartPlayerManager {
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始初始化智能播放器")
            updateState { copy(processingStatus = "初始化中...") }
            
            // 初始化逻辑
            val initJobs = listOf(
                async { initVideoEnhancer() },
                async { initAudioEnhancer() }
            )
            
            initJobs.awaitAll()
            
            updateState { copy(isInitialized = true, processingStatus = "初始化完成") }
            Log.d(TAG, "智能播放器初始化成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "智能播放器初始化失败", e)
            updateState { copy(errorMessage = "初始化失败: ${e.message}") }
            throw e
        }
    }
}
```

### 2. 性能优化标准
```kotlin
// ✅ 资源管理规范
class VideoEnhanceProcessor {
    private var isInitialized = false
    private var tensorflowLite: Interpreter? = null
    
    suspend fun initialize() {
        if (isInitialized) return
        
        try {
            // 懒加载重资源
            tensorflowLite = loadModel()
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            throw e
        }
    }
    
    fun release() {
        tensorflowLite?.close()
        tensorflowLite = null
        isInitialized = false
    }
}

// ✅ 内存优化
class FrameProcessor {
    private val bufferPool = ArrayDeque<ByteArray>()
    
    fun processFrame(input: ByteArray): ByteArray {
        val buffer = bufferPool.pollFirst() ?: ByteArray(BUFFER_SIZE)
        try {
            // 处理逻辑
            return processWithBuffer(input, buffer)
        } finally {
            bufferPool.offerLast(buffer) // 回收buffer
        }
    }
}
```

### 3. 测试标准
```kotlin
// ✅ 单元测试示例
@RunWith(AndroidJUnit4::class)
class SmartPlayerManagerTest {
    
    private lateinit var context: Context
    private lateinit var smartPlayerManager: SmartPlayerManager
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        smartPlayerManager = SmartPlayerManager.getInstance(context)
    }
    
    @Test
    fun testInitialize_Success() = runTest {
        // Given
        val config = SmartPlayerManager.SmartPlayerConfig(
            autoGenerateSubtitles = true,
            enableVideoEnhance = false,
            enableAudioEnhance = true
        )
        
        // When
        smartPlayerManager.initialize(config)
        
        // Then
        val state = smartPlayerManager.smartFeaturesState.value
        assertTrue(state.isInitialized)
        assertTrue(state.subtitleGenerationEnabled)
        assertFalse(state.videoEnhancementEnabled)
        assertTrue(state.audioEnhancementEnabled)
    }
    
    @Test
    fun testGenerateSubtitles_EmptyVideoPath_ThrowsException() = runTest {
        // Given
        val emptyPath = ""
        
        // When & Then
        assertThrows<IllegalArgumentException> {
            smartPlayerManager.generateSmartSubtitles(emptyPath)
        }
    }
}
```

## 文档规范

### 1. 代码注释规范
```kotlin
/**
 * 智能播放器管理器
 * 
 * 这是Xplayer的核心AI功能管理类，负责统一调度和管理所有AI增强功能。
 * 支持以下功能：
 * - 智能字幕生成：基于语音识别的实时字幕生成
 * - 视频画质增强：使用AI算法提升视频质量
 * - 音频智能优化：3D音效和智能降噪处理
 * 
 * 使用示例：
 * ```kotlin
 * val manager = SmartPlayerManager.getInstance(context)
 * manager.initialize(SmartPlayerConfig())
 * manager.bindPlayer(videoPlayer, audioSessionId)
 * ```
 * 
 * @param context 应用上下文，用于初始化AI处理器
 * @author wangkm
 * @since 2.0.0
 * @see SubtitleAIProcessor
 * @see VideoEnhanceProcessor  
 * @see AudioEnhanceProcessor
 */
class SmartPlayerManager(private val context: Context) {
    
    /**
     * 初始化智能播放器管理器
     * 
     * 此方法会并行初始化所有启用的AI处理器，包括：
     * - 视频增强处理器（如果启用）
     * - 音频增强处理器（如果启用）
     * - 字幕生成处理器（默认启用）
     * 
     * @param config 智能播放器配置，包含各AI功能的开关设置
     * @throws IllegalStateException 如果在已初始化状态下重复调用
     * @throws RuntimeException 如果初始化过程中发生错误
     */
    suspend fun initialize(config: SmartPlayerConfig = SmartPlayerConfig()) {
        // 实现
    }
}
```

### 2. README文档规范
每个模块都应包含完整的README.md文档，包含：
- 模块功能说明
- 快速开始指南  
- API使用示例
- 配置选项说明
- 常见问题解答

这套开发规范相当全面，但我必须指出一个现实问题：你的AI功能实现和文档描述之间存在较大差距。建议在完善实际AI算法实现的同时，也要确保文档的准确性，避免过度承诺。代码规范很好，但执行的一致性需要通过代码审查和自动化工具来保证。