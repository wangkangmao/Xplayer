---
title: 项目结构
description: "Xplayer项目的目录结构、文件组织规范和代码模块划分详细说明"
inclusion: always
---

# Xplayer 项目结构文档

## 项目根目录结构

```
Xplayer/
├── .ai-rules/                 # AI规则配置目录
├── .claude/                   # Claude AI配置
├── .idea/                     # Android Studio配置
├── app/                       # 主应用模块
├── cache/                     # 视频缓存模块
├── docs/                      # 项目文档
├── exo/                       # ExoPlayer集成模块
├── gradle/                    # Gradle构建配置
├── ijk/                       # IJKPlayer集成模块
├── screenshots/               # 应用截图
├── widget/                    # UI组件库模块
├── xplayer/                   # 核心播放器模块
├── build.gradle              # 项目级构建配置
├── config.gradle             # 全局配置文件
├── gradle.properties         # Gradle属性配置
├── local.properties          # 本地环境配置
├── settings.gradle           # 项目设置
└── README.md                 # 项目说明文档
```

## 主应用模块 (app/)

### 目录结构
```
app/
├── build.gradle              # 应用模块构建配置
├── proguard-rules.pro        # ProGuard混淆规则
├── src/
│   ├── androidTest/          # Android集成测试
│   ├── main/                 # 主要源代码
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/wangkm/player/
│   │   │   ├── App.kt                    # 应用程序入口
│   │   │   ├── MainActivity.kt           # 主Activity
│   │   │   ├── ai/                       # AI功能模块
│   │   │   │   ├── SmartPlayerManager.kt      # AI总管理器
│   │   │   │   ├── SubtitleAIProcessor.kt     # 字幕AI处理器
│   │   │   │   ├── VideoEnhanceProcessor.kt   # 视频增强处理器
│   │   │   │   └── AudioEnhanceProcessor.kt   # 音频增强处理器
│   │   │   ├── base/                     # 基础架构
│   │   │   │   ├── BaseActivity.java          # Activity基类
│   │   │   │   ├── BaseContract.java          # MVP契约接口
│   │   │   │   ├── BaseFragment.java          # Fragment基类
│   │   │   │   ├── BasePresenter.java         # Presenter基类
│   │   │   │   └── adapter/                   # 适配器基类
│   │   │   ├── controller/               # 播放器控制器
│   │   │   ├── danmu/                    # 弹幕功能
│   │   │   ├── demo/                     # 演示功能
│   │   │   ├── manager/                  # 管理器类
│   │   │   ├── net/                      # 网络层
│   │   │   ├── render/                   # 渲染相关
│   │   │   ├── ui/                       # 用户界面
│   │   │   │   ├── activity/                  # Activity组件
│   │   │   │   ├── compose/                   # Compose UI组件
│   │   │   │   │   ├── SmartPlayerScreen.kt   # 智能播放器主界面
│   │   │   │   │   └── SmartPlayerDialogs.kt  # 对话框组件
│   │   │   │   └── widget/                    # 自定义View
│   │   │   ├── utils/                    # 工具类
│   │   │   └── video/                    # 视频相关
│   │   └── res/                          # 资源文件
│   │       ├── anim/                          # 动画资源
│   │       ├── color/                         # 颜色资源
│   │       ├── drawable/                      # 图片资源
│   │       ├── layout/                        # 布局资源
│   │       ├── mipmap-*/                      # 应用图标
│   │       ├── values/                        # 值资源
│   │       │   ├── attrs.xml                  # 自定义属性
│   │       │   ├── colors.xml                 # 颜色定义
│   │       │   ├── strings.xml                # 字符串资源
│   │       │   ├── styles.xml                 # 样式定义
│   │       │   └── themes.xml                 # 主题定义
│   │       └── xml/                           # XML配置
│   └── test/                 # 单元测试
└── build/                    # 构建输出目录
```

## 核心播放器模块 (xplayer/)

### 目录结构
```
xplayer/
├── build.gradle              # 模块构建配置
├── consumer-rules.pro        # 消费者ProGuard规则
├── src/main/java/com/wangkm/xplayer/
│   ├── base/                          # 基础抽象类
│   │   ├── AbstractMediaPlayer.kt          # 媒体播放器抽象
│   │   ├── BaseControlWidget.java          # 控制组件基类
│   │   ├── BaseController.kt               # 控制器基类
│   │   └── BasePlayer.java                # 播放器基类
│   ├── controller/                    # 控制器实现
│   │   ├── ControlWrapper.kt               # 控制器包装
│   │   ├── GestureController.kt            # 手势控制器
│   │   └── VideoController.kt              # 视频控制器
│   ├── interfaces/                    # 接口定义
│   │   ├── IBasePlayer.java               # 播放器接口
│   │   ├── IControllerView.java           # 控制器视图接口
│   │   ├── IGestureControl.java           # 手势控制接口
│   │   ├── IPlayerControl.kt              # 播放控制接口
│   │   ├── IRenderView.java               # 渲染视图接口
│   │   └── IVideoController.kt            # 视频控制器接口
│   ├── listener/                      # 监听器
│   │   ├── OnMediaEventListener.kt        # 媒体事件监听
│   │   ├── OnPlayerEventListener.kt       # 播放器事件监听
│   │   └── OnWindowActionListener.kt      # 窗口操作监听
│   ├── manager/                       # 管理器
│   │   ├── IVideoManager.kt               # 视频管理器接口
│   │   └── IWindowManager.kt              # 窗口管理器接口
│   ├── media/                         # 媒体相关
│   │   ├── IMediaPlayer.java              # 媒体播放器接口
│   │   ├── IVideoPlayer.kt                # 视频播放器接口  
│   │   ├── MediaFactory.kt                # 媒体工厂
│   │   └── core/                          # 核心实现
│   │       ├── MediaPlayer.kt             # 媒体播放器实现
│   │       └── MediaPlayerFactory.kt      # 播放器工厂
│   ├── model/                         # 数据模型
│   │   └── PlayerState.kt                 # 播放器状态
│   ├── utils/                         # 工具类
│   │   ├── AnimationUtils.kt              # 动画工具
│   │   ├── AudioFocus.java                # 音频焦点管理
│   │   ├── ILogger.kt                     # 日志工具
│   │   ├── PlayerUtils.kt                 # 播放器工具
│   │   └── ThreadPool.java                # 线程池管理
│   └── widget/                        # 播放器组件
│       ├── VideoPlayer.kt                 # 视频播放器实现
│       └── view/                          # 自定义视图
│           ├── LayoutProvider.java        # 布局提供者
│           ├── MediaTextureView.kt        # 媒体纹理视图
│           ├── ScreenOrientationRotate.java # 屏幕旋转
│           └── WindowPlayerFloatView.kt   # 悬浮窗视图
```

## UI组件库模块 (widget/)

### 目录结构
```
widget/
├── src/main/java/com/wangkm/widget/
│   ├── WidgetFactory.kt              # 组件工厂
│   ├── controls/                     # 控制组件
│   │   ├── ControWindowView.kt            # 窗口控制视图
│   │   ├── ControlCompletionView.kt       # 完成控制视图
│   │   ├── ControlFunctionBarView.kt      # 功能栏控制视图
│   │   ├── ControlGestureView.kt          # 手势控制视图
│   │   ├── ControlListView.kt             # 列表控制视图
│   │   ├── ControlLoadingView.kt          # 加载控制视图
│   │   ├── ControlStatusView.kt           # 状态控制视图
│   │   └── ControlToolBarView.kt          # 工具栏控制视图
│   └── view/                         # 自定义视图
│       └── BatteryView.kt                 # 电池视图
└── src/main/res/                     # UI资源
    ├── drawable/                          # 图片资源
    ├── layout/                            # 布局文件
    ├── mipmap-xxhdpi/                     # 高清图标
    ├── values/                            # 值资源
    └── values-zh-rCN/                     # 中文资源
```

## 播放器集成模块

### IJKPlayer模块 (ijk/)
```
ijk/
├── src/main/
│   ├── java/com/wangkm/ijk/media/
│   │   ├── RawDataSourceProvider.kt       # 原始数据源提供者
│   │   └── core/
│   │       ├── IJkMediaPlayer.kt          # IJK播放器实现
│   │       └── IjkPlayerFactory.java      # IJK播放器工厂
│   ├── java/tv/danmaku/ijk/media/player/  # IJK原生库
│   └── jniLibs/                           # 原生库文件
│       ├── arm64-v8a/                     # ARM64架构
│       ├── armeabi-v7a/                   # ARMv7架构
│       ├── armeabi/                       # ARM架构
│       ├── x86/                           # x86架构
│       └── x86_64/                        # x86_64架构
```

### ExoPlayer模块 (exo/)
```
exo/
└── src/main/java/com/wangkm/exo/media/
    ├── ExoMediaSourceHelper.java         # ExoPlayer数据源助手
    └── core/
        ├── ExoMediaPlayer.java            # ExoPlayer实现
        └── ExoPlayerFactory.java          # ExoPlayer工厂
```

## 缓存模块 (cache/)

### 目录结构
```
cache/
└── src/main/java/
    ├── com/danikula/videocache/           # 第三方缓存库
    │   ├── ByteArrayCache.java            # 字节数组缓存
    │   ├── HttpProxyCache.java            # HTTP代理缓存
    │   ├── HttpProxyCacheServer.java      # 缓存服务器
    │   ├── file/                          # 文件缓存
    │   ├── headers/                       # HTTP头处理
    │   └── sourcestorage/                 # 源存储
    └── com/wangkm/cache/
        ├── VideoCache.java                # 视频缓存主类
        └── task/
            └── PreloadTask.kt             # 预加载任务
```

## 资源文件组织

### 命名规范

#### 布局文件 (layout/)
- **Activity布局**: `activity_[功能名].xml`
- **Fragment布局**: `fragment_[功能名].xml`
- **自定义View**: `view_[组件名].xml`
- **Dialog布局**: `dialog_[对话框名].xml`
- **列表项布局**: `item_[列表项名].xml`

#### 图片资源 (drawable/)
- **图标**: `ic_[功能]_[状态].xml/png`
- **背景**: `bg_[用途].xml`
- **选择器**: `selector_[用途].xml`
- **形状**: `shape_[形状描述].xml`

#### 字符串资源 (values/strings.xml)
```xml
<!-- 功能模块分组 -->
<!-- 播放器相关 -->
<string name="player_play">播放</string>
<string name="player_pause">暂停</string>

<!-- AI功能相关 -->
<string name="ai_subtitle_generate">生成字幕</string>
<string name="ai_video_enhance">视频增强</string>

<!-- 错误信息 -->
<string name="error_network">网络连接失败</string>
<string name="error_file_not_found">文件未找到</string>
```

#### 颜色资源 (values/colors.xml)
```xml
<!-- 主题色 -->
<color name="primary">#6200EE</color>
<color name="primary_variant">#3700B3</color>

<!-- 功能色 -->
<color name="player_background">#000000</color>
<color name="subtitle_background">#80000000</color>
```

## 代码组织规范

### 包结构规范
```
com.wangkm.player/
├── ai/                    # AI功能包
├── base/                  # 基础架构包
├── controller/            # 控制器包
├── demo/                  # 演示功能包
├── manager/               # 管理器包
├── net/                   # 网络层包
├── ui/                    # 用户界面包
│   ├── activity/          # Activity组件
│   ├── compose/           # Compose UI
│   ├── fragment/          # Fragment组件
│   └── widget/            # 自定义View
├── utils/                 # 工具类包
└── video/                 # 视频功能包
```

### 文件命名规范

#### Kotlin/Java文件
- **Activity**: `[功能名]Activity.kt`
- **Fragment**: `[功能名]Fragment.kt`
- **Adapter**: `[数据类型]Adapter.kt`
- **Manager**: `[功能名]Manager.kt`
- **Utils**: `[功能名]Utils.kt`
- **Interface**: `I[接口名].kt`

#### 资源文件
- **布局**: 使用下划线分隔，全小写
- **图片**: 使用下划线分隔，全小写
- **ID**: 使用驼峰命名，首字母小写

### 模块依赖规则

#### 依赖层次
```
app (应用层)
 ↓
widget (UI组件层)
 ↓  
xplayer (核心播放器层)
 ↓
ijk/exo (播放器实现层)
 ↓
cache (缓存层)
```

#### 依赖原则
1. **单向依赖**: 上层可以依赖下层，下层不能依赖上层
2. **接口隔离**: 通过接口定义模块间的交互
3. **最小依赖**: 只引入必要的依赖
4. **循环检测**: 避免模块间循环依赖

## 配置文件管理

### Gradle配置分层
```
├── build.gradle (项目级)      # 全局构建配置
├── config.gradle             # 版本号等全局配置
├── gradle.properties         # Gradle属性
├── app/build.gradle          # 应用模块配置
├── xplayer/build.gradle      # 核心模块配置
└── gradle/libs.versions.toml # 版本目录文件
```

### 环境配置
- **local.properties**: 本地环境配置(SDK路径等)
- **gradle.properties**: Gradle属性配置
- **AndroidManifest.xml**: 应用清单配置

这种结构化的项目组织确保了代码的可维护性、可扩展性和团队协作的效率，为Xplayer项目的持续发展提供了坚实的基础。