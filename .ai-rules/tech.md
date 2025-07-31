---
title: 技术架构
description: "Xplayer项目的技术栈、架构模式、依赖管理和构建配置详细说明"
inclusion: always
---

# Xplayer 技术架构文档

## 技术栈概览

### 核心技术栈
- **开发语言**: Kotlin 1.9.0 + Java
- **最低SDK**: Android API 21 (Android 5.0)
- **目标SDK**: Android API 34 (Android 14)
- **构建工具**: Gradle 8.6.0
- **JDK版本**: Java 17

### UI框架
- **Jetpack Compose**: 1.5.2 (现代化声明式UI)
- **Material Design 3**: 最新设计规范
- **传统View系统**: XML布局（兼容性考虑）
- **ViewBinding**: 类型安全的视图绑定

### AI与机器学习
- **TensorFlow Lite**: 2.13.0 (边缘AI推理)
- **TensorFlow Lite GPU**: 2.13.0 (GPU加速)
- **TensorFlow Lite Support**: 0.4.3 (支持库)

### 媒体播放框架
- **ExoPlayer**: 2.19.1 (主要播放引擎)
  - exoplayer-dash: DASH协议支持
  - exoplayer-hls: HLS协议支持  
  - exoplayer-rtsp: RTSP协议支持
- **IJKPlayer**: 集成版本 (备用播放引擎)
- **Android MediaPlayer**: 系统默认播放器

### 网络与数据处理
- **OkHttp**: 4.11.0 (HTTP客户端)
- **Retrofit**: 2.9.0 (网络请求封装)
- **Gson**: 2.8.6 (JSON解析)

### 图像处理
- **Glide**: 4.16.0 (图像加载和缓存)
- **GPUImage**: 2.1.0 (GPU图像处理)

### 其他核心库
- **Kotlin Coroutines**: 1.7.3 (异步编程)
- **AndroidX库**: 最新稳定版本
- **DanmakuFlameMaster**: 0.9.25 (弹幕功能)

## 模块架构

### 项目模块结构
```
Xplayer/
├── app/                    # 主应用模块 (Android Application)
├── xplayer/               # 核心播放器模块 (Android Library)
├── widget/                # UI组件库模块 (Android Library)
├── ijk/                   # IJKPlayer集成模块 (Android Library)
├── exo/                   # ExoPlayer集成模块 (Android Library)
└── cache/                 # 视频缓存模块 (Android Library)
```

### 模块依赖关系
```
app
├── depends on: xplayer
├── depends on: widget
├── depends on: cache
├── depends on: ijk
└── depends on: exo

xplayer (核心模块)
├── 提供: 基础播放器抽象
├── 提供: 播放器接口定义
└── 提供: 媒体管理器

widget
├── 提供: UI控制组件
├── 提供: 播放器控制器
└── 提供: 自定义View

cache
├── 提供: 视频缓存功能
├── 提供: 预加载机制
└── 基于: DaniKula VideoCache

ijk/exo
├── 提供: 具体播放器实现
├── 实现: 媒体解码接口
└── 封装: 原生播放器库
```

## 架构模式

### 整体架构
项目采用**模块化分层架构**，结合**MVP模式**和**现代Android架构组件**：

```
┌─────────────────────────────────────┐
│            Presentation Layer        │
│  ┌─────────────┐  ┌─────────────────┐│
│  │   Compose   │  │  Traditional    ││
│  │     UI      │  │     Views       ││
│  └─────────────┘  └─────────────────┘│
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│            Business Layer           │
│  ┌─────────────┐  ┌─────────────────┐│
│  │ AI Managers │  │   Presenters    ││
│  └─────────────┘  └─────────────────┘│
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│             Data Layer              │
│  ┌─────────────┐  ┌─────────────────┐│
│  │   Players   │  │     Cache       ││
│  └─────────────┘  └─────────────────┘│
└─────────────────────────────────────┘
```

### 核心设计模式

#### 1. 工厂模式 (Factory Pattern)
```kotlin
// MediaPlayerFactory - 播放器工厂
interface MediaPlayerFactory {
    fun createPlayer(context: Context): AbstractMediaPlayer
}

// 具体工厂实现
class IjkPlayerFactory : MediaPlayerFactory
class ExoPlayerFactory : MediaPlayerFactory
```

#### 2. 观察者模式 (Observer Pattern)
```kotlin
// 播放状态监听
interface OnPlayerEventListener {
    fun onPlayerState(state: PlayerState, message: String)
    fun onProgress(currentDuration: Long, totalDuration: Long)
}
```

#### 3. 策略模式 (Strategy Pattern)
```kotlin
// AI处理策略
interface AIProcessor {
    suspend fun process(input: Any): Result<Any>
}

class SubtitleAIProcessor : AIProcessor
class VideoEnhanceProcessor : AIProcessor
class AudioEnhanceProcessor : AIProcessor
```

#### 4. 单例模式 (Singleton Pattern)
```kotlin
// 全局管理器
object IVideoManager {
    fun getInstance(): IVideoManager
}

class SmartPlayerManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: SmartPlayerManager? = null
        
        fun getInstance(context: Context): SmartPlayerManager
    }
}
```

## AI功能架构

### AI处理流水线
```
Input (Video/Audio)
        ↓
┌─────────────────┐
│  Data Prepare   │ (数据预处理)
└─────────────────┘
        ↓
┌─────────────────┐
│  TF Lite Model  │ (AI模型推理)
└─────────────────┘
        ↓
┌─────────────────┐
│ Post Processing │ (后处理优化)
└─────────────────┘
        ↓
Output (Enhanced Media)
```

### AI模块设计
```kotlin
// AI功能总管理器
class SmartPlayerManager {
    // AI处理器
    private val subtitleProcessor: SubtitleAIProcessor
    private val videoEnhanceProcessor: VideoEnhanceProcessor  
    private val audioEnhanceProcessor: AudioEnhanceProcessor
    
    // 协程作用域
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 状态管理
    private val _smartFeaturesState = MutableStateFlow(SmartFeaturesState())
    val smartFeaturesState: StateFlow<SmartFeaturesState>
}
```

## 数据流架构

### 播放器数据流
```
User Action → Controller → BasePlayer → IVideoPlayer → MediaPlayer
     ↓              ↓           ↓            ↓            ↓
UI Update ← Presenter ← Listener ← Callback ← Native Player
```

### AI处理数据流
```
Media Source → AI Processor → Enhanced Output → Display/Playback
     ↓              ↓              ↓              ↓
Status Update ← Progress ← Processing ← Real-time Feedback
```

## 性能优化策略

### 内存管理
- **播放器复用**: 全局单例管理播放器实例
- **资源释放**: 及时释放AI模型和媒体资源
- **缓存策略**: LRU缓存机制管理AI处理结果

### 异步处理
- **Kotlin Coroutines**: 所有耗时操作异步执行
- **线程池管理**: 自定义线程池处理AI计算
- **流式处理**: StateFlow/Flow响应式数据流

### AI优化
- **模型量化**: TensorFlow Lite模型压缩
- **GPU加速**: 支持GPU推理加速
- **批处理**: 批量处理提升效率

## 构建配置

### Gradle配置结构
```gradle
// 项目级build.gradle
buildscript {
    ext.kotlin_version = "1.9.0"
    dependencies {
        classpath "com.android.tools.build:gradle:8.6.0"
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

// config.gradle - 全局配置
ext {
    sdk = [
        versionCode: 20126,
        versionName: "2.1.26.1"
    ]
}
```

### 模块配置
每个模块都配置为Android Library，支持：
- **ProGuard混淆**: 发布版本代码保护
- **多架构支持**: ARM64, ARMv7支持
- **构建变体**: Debug/Release配置

### 依赖管理
- **版本目录**: 使用libs.versions.toml统一管理版本
- **强制版本**: 解决依赖冲突问题
- **按需依赖**: 模块化依赖，支持按需引入

## 测试架构

### 测试策略
- **单元测试**: JUnit + Mockito
- **UI测试**: Espresso测试框架  
- **集成测试**: 模块间集成测试
- **AI测试**: TensorFlow Lite模型测试

### 测试覆盖
- 核心播放功能测试
- AI功能准确性测试
- 性能基准测试
- 兼容性测试

## 安全考虑

### 代码安全
- **混淆配置**: ProGuard代码混淆
- **权限管理**: 最少权限原则
- **数据加密**: 敏感数据加密存储

### AI模型安全
- **模型保护**: 模型文件加密
- **输入验证**: AI输入数据验证
- **隐私保护**: 本地AI处理，数据不上传

## 扩展性设计

### 插件化架构
- **播放器插件**: 支持新的播放器引擎
- **AI插件**: 支持新的AI功能模块
- **UI插件**: 支持自定义UI组件

### 配置化设计
- **功能开关**: 运行时功能启用/禁用
- **参数配置**: AI处理参数可配置
- **主题系统**: 支持动态主题切换

这个架构设计确保了Xplayer的可维护性、可扩展性和高性能，为未来的功能扩展和技术演进奠定了坚实的基础。