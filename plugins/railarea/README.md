# RailArea Plugin

一个现代化的 Minecraft 铁路区域管理插件，提供自动化矿车系统、音效管理和可视化功能。

## 功能特性

### 🚄 核心功能
- **智能区域检测**: 使用八叉树算法实现高效的 3D 空间区域检测
- **自动矿车管理**: 自动化的矿车召唤、等待和发车系统
- **音效系统**: 支持到站音乐、发车音乐和警告音效
- **可视化工具**: 粒子效果显示区域边界和范围
- **多世界支持**: 支持多个世界的独立铁路系统

### 🎵 音效管理
- 自定义到站音乐播放
- 发车音乐提醒
- 警告音效系统
- 支持多音符序列播放

### 🎮 用户交互
- 右键铁轨自动召唤矿车
- 实时 ActionBar 和 Title 提示
- 区域进入/离开事件处理
- 命令行管理界面

### 🔧 技术特性
- 基于现代化的 Kotlin 开发
- 使用协程进行异步处理
- 完整的生命周期管理
- 内存泄漏防护
- 高性能八叉树空间索引
- 完整的GUI管理界面
- 健壮的错误处理机制

## 快速开始

### 安装要求
- Minecraft 1.20.1+
- Paper 服务器
- CommandAPI 插件

### 安装步骤
1. 下载 `railarea-2.0.0.jar` 文件
2. 将文件放入服务器的 `plugins` 目录
3. 重启服务器
4. 配置 `config.yml` 和 `rails.yml` 文件

### 基本配置

#### config.yml
```yaml
# 世界大小配置
worldSize:
  world:
    x1: -1000
    z1: -1000
    x2: 1000
    z2: 1000

# 矿车配置
minecart:
  waitingSeconds: 100
  warningSeconds: 20
  maxSpeed: 0.4

# 音效配置
arriveMusic:
  - note: "C4"
    delay: 0
  - note: "E4"
    delay: 10

startMusic:
  - note: "G4"
    delay: 0
  - note: "C5"
    delay: 10

warningSound:
  sound: "BLOCK_NOTE_BLOCK_PLING"
  volume: 1.0
  pitch: 2.0
```

#### rails.yml
```yaml
# 线路配置
lines:
  line1:
    name: "1号线"
    color: "&c"
    stations:
      - "station1"
      - "station2"

# 站点配置
stations:
  station1:
    name: "中央车站"
    world: "world"
    x: 100
    y: 64
    z: 200

# 区域配置
areas:
  area1:
    station: "station1"
    line: "line1"
    world: "world"
    x1: 95
    y1: 60
    z1: 195
    x2: 105
    y2: 70
    z2: 205
```

## 命令使用

### 基本命令
- `/railarea` - 打开主GUI管理界面
- `/railarea info` - 显示插件信息和统计
- `/railarea reload` - 重载插件配置
- `/railarea status` - 显示插件状态信息

### 管理命令
- `/railarea help` - 显示命令帮助信息
- 所有功能都可通过GUI界面进行管理

## 开发信息

### 项目结构
```
src/main/kotlin/city/newnan/railarea/
├── RailAreaPlugin.kt          # 主插件类
├── config/                    # 配置相关
│   ├── RailAreaConfig.kt     # 主配置类
│   ├── Area.kt               # 区域配置
│   └── Station.kt            # 站点配置
├── manager/                   # 管理器
│   ├── RailAreaManager.kt    # 区域管理器
│   ├── MinecartManager.kt    # 矿车管理器
│   └── SoundManager.kt       # 音效管理器
├── spatial/                   # 空间算法
│   ├── Octree.kt             # 八叉树实现
│   ├── Point3D.kt            # 3D 点
│   └── Range3D.kt            # 3D 范围
├── events/                    # 事件处理
├── commands/                  # 命令处理
└── utils/                     # 工具类
```

### 技术栈
- **语言**: Kotlin 1.9+
- **构建工具**: Gradle 8.8
- **服务器**: Paper 1.20.1
- **依赖管理**: 基于项目模块化架构
- **配置**: Jackson JSON/YAML 处理
- **命令**: CommandAPI 框架

## 许可证

本项目采用 MIT 许可证。详见 LICENSE 文件。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 更新日志

### v2.0.0
- 完全重写为现代化 Kotlin 插件
- 新增八叉树空间索引算法
- 改进的音效系统
- 完整的生命周期管理
- 内存泄漏防护
- 模块化架构设计
- 完整的GUI管理界面
- 健壮的错误处理和异常恢复
- 完善的国际化支持
- 优化的配置管理系统
