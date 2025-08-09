# FeeFly 付费飞行插件

现代化的付费飞行插件，基于 NewNanPlugins 框架开发，提供完整的基于经济系统的飞行功能。

## 📖 目录

- [介绍](#介绍)
- [快速开始](#快速开始)
- [功能特性](#功能特性)
- [命令系统](#命令系统)
- [权限管理](#权限管理)
- [配置文件](#配置文件)
- [故障排除](#故障排除)
- [文档链接](#文档链接)

## 🚀 介绍

FeeFly 是一个现代化的付费飞行系统，让玩家可以通过消费游戏内货币来获得飞行能力。插件基于 NewNanPlugins 框架开发，具有以下核心特性：

**🆚 对比传统飞行插件**

| 特性       | 传统飞行插件 | FeeFly          |
| ---------- | ------------ | --------------- |
| 经济集成   | 基础         | ✅ 深度Vault集成 |
| 状态持久化 | ❌            | ✅ 自动恢复      |
| 实时显示   | 简单         | ✅ Action Bar    |
| 多语言     | ❌            | ✅ 中英文支持    |
| 性能优化   | 一般         | ✅ 现代化架构    |
| 资源管理   | 手动         | ✅ 自动清理      |
| API 支持   | 有限         | ✅ 完整 API      |

**💡 核心特性**
- **按时间计费**: 精确的时间计费系统
- **智能警告**: 余额不足时自动提醒
- **安全检查**: 游戏模式和世界限制
- **状态恢复**: 服务器重启后自动恢复飞行状态
- **命令冷却**: 防止恶意频繁使用
- **事件处理**: 智能处理死亡、传送等事件

**🎯 使用场景**
- 生存服务器的付费飞行服务
- 经济服务器的增值功能
- 建筑服务器的便民工具
- 大型服务器的差异化服务

### 示例场景

```
玩家: /fly
系统: 已开启付费飞行！双击空格键开始飞行。
系统: [飞行中] 剩余时间: 5分30秒 (余额 15.60 ₦)

# 余额不足时
系统: 余额即将耗尽！
系统: 请尽快结束飞行
系统: 飞行已结束！
```

## 🏃 快速开始

### 前置要求

1. **Bukkit/Spigot/Paper** 服务器
2. **Vault** 插件
3. **经济插件**（如 EssentialsX）

### 安装步骤

1. **下载插件**: 从 Releases 页面下载最新版本
2. **安装依赖**: 确保服务器已安装 Vault 和经济插件
3. **放置插件**: 将 jar 文件放入 `plugins` 目录
4. **重启服务器**: 重启服务器以加载插件

### 基础配置

```yaml
# config.yml 最小配置
target-account: ""

flying:
  tick-per-count: 20        # 每次扣费间隔(tick)
  cost-per-count: 0.3       # 每次扣费金额
  fly-speed: 0.05           # 飞行速度
  command-cooldown-seconds: 3 # 命令冷却时间
```

### 快速使用

```bash
# 1. 给玩家基础权限
/lp user <player> permission set feefly.self true

# 2. 玩家使用飞行
/fly

# 3. 查看飞行列表
/feefly list
```

## 🎮 命令系统

### 可用命令

| 命令                  | 权限                           | 描述                     |
| --------------------- | ------------------------------ | ------------------------ |
| `/fly [玩家]`         | `feefly.self` / `feefly.other` | 切换飞行状态             |
| `/feefly fly [玩家]`  | `feefly.self` / `feefly.other` | 切换飞行状态（完整命令） |
| `/feefly help [查询]` | `feefly.self`                  | 显示命令帮助             |
| `/feefly list`        | `feefly.list`                  | 显示当前飞行的玩家列表   |
| `/feefly reload`      | `feefly.reload`                | 重载插件配置             |

### 命令示例

```bash
# 基础飞行命令
/fly                # 切换自己的飞行状态
/fly <玩家>         # 切换指定玩家的飞行状态（需要 feefly.other 权限）

# 管理命令
/feefly list        # 显示当前飞行的玩家
/feefly reload      # 重载插件配置
/feefly help        # 显示帮助信息
```

## ✨ 功能特性

### 核心功能总览

FeeFly 插件提供完整的付费飞行解决方案，包含以下主要功能模块：

- **飞行管理系统** - 启动、停止、状态监控
- **经济集成系统** - Vault集成、按时间扣费
- **权限控制系统** - 细粒度权限管理
- **状态持久化** - 服务器重启后自动恢复
- **事件处理** - 智能处理死亡、传送等事件
- **多语言支持** - 国际化语言系统

### 简单用法

#### 基础飞行操作

```bash
# 切换自己的飞行状态
/fly

# 为指定玩家切换飞行状态（需要权限）
/fly Steve

# 查看当前飞行的玩家
/feefly list

# 重载插件配置
/feefly reload
```

#### 飞行状态显示

```
# 正常飞行时的状态显示
[飞行中] 剩余时间: 3分45秒 (余额 12.50 ₦)

# 免费飞行时的显示
#免费飞行# 祝 Steve 白嫖快乐！

# 余额不足警告
余额即将耗尽！
请尽快结束飞行
```

### 高级用法

#### 权限配置

```bash
# 给予玩家基础飞行权限
/lp user <player> permission set feefly.self true

# 给予玩家免费飞行权限
/lp user <player> permission set feefly.free true

# 给予管理员完整权限
/lp user <admin> permission set feefly.other true
/lp user <admin> permission set feefly.list true
/lp user <admin> permission set feefly.reload true
```

#### 高级配置

```yaml
# 高频低费配置
flying:
  tick-per-count: 10          # 更频繁的扣费（0.5秒一次）
  cost-per-count: 0.15        # 更低的单次费用
  fly-speed: 0.1              # 更快的飞行速度
  low-balance-warning-seconds: 30  # 更早的余额警告
  command-cooldown-seconds: 1 # 更短的命令冷却

# 资金转账配置
target-account: "server_fund" # 将费用转入服务器账户
```

## 🔐 权限管理

### 权限节点详解

| 权限节点                 | 描述                             | 默认值  |
| ------------------------ | -------------------------------- | ------- |
| `feefly.self`            | 为自己开启付费飞行的权限         | `false` |
| `feefly.other`           | 为别人开启付费飞行的权限         | `op`    |
| `feefly.free`            | 免费飞行的权限                   | `op`    |
| `feefly.list`            | 显示正在使用付费飞行的玩家的权限 | `op`    |
| `feefly.reload`          | 重载插件配置的权限               | `op`    |
| `feefly.bypass.cooldown` | 绕过命令冷却限制的权限           | `op`    |

### LuckPerms 配置示例

```bash
# 给予玩家基础飞行权限
/lp user <player> permission set feefly.self true

# 给予 VIP 玩家免费飞行权限
/lp user <vip> permission set feefly.free true

# 给予管理员完整权限
/lp user <admin> permission set feefly.other true
/lp user <admin> permission set feefly.list true
/lp user <admin> permission set feefly.reload true

# 创建权限组
/lp group default permission set feefly.self true
/lp group vip permission set feefly.free true
/lp group admin permission set feefly.other true
```

## ⚙️ 配置文件

### 完整配置示例

```yaml
# FeeFly 付费飞行插件配置文件

# 消息前缀设置
player-message-prefix: "&7[&6牛腩飞行&7] &f"
console-message-prefix: "[FeeFly] "

# 目标转账账户（可选）
target-account: ""

# 飞行相关配置
flying:
  # 每次扣费的间隔（tick，20 tick = 1秒）
  tick-per-count: 20

  # 每次扣费的金额
  cost-per-count: 0.3

  # 飞行速度（0.0-1.0）
  fly-speed: 0.05

  # 低余额警告阈值（秒）
  low-balance-warning-seconds: 60

  # 命令使用冷却时间（秒）
  command-cooldown-seconds: 3

  # 状态验证间隔（秒）
  state-validation-interval-seconds: 30
```

### 配置说明

- `tick-per-count`: 计费间隔，建议保持20（1秒）
- `cost-per-count`: 每次计费金额，根据服务器经济调整
- `fly-speed`: 飞行速度，0.05是推荐值
- `target-account`: 费用转入的目标账户，留空则直接消失

## 🔧 故障排除

### 常见问题

#### 1. 插件无法启动

**错误**: `Vault plugin not found!`

**解决方案**:
```bash
# 检查 Vault 插件
/plugins

# 确保安装了 Vault 和经济插件
```

#### 2. 无法开启飞行

**可能原因**:
- 权限不足：需要 `feefly.self` 权限
- 余额不足：检查玩家经济余额
- 游戏模式：创造/观察者模式无法使用
- 命令冷却：等待冷却时间结束

**解决方案**:
```bash
# 检查权限
/lp user <player> permission check feefly.self

# 检查余额
/bal <player>

# 给予免费飞行权限测试
/lp user <player> permission set feefly.free true
```

#### 3. 飞行状态异常

**现象**: 服务器重启后飞行状态丢失

**解决方案**: 插件会自动恢复状态，如果仍有问题可重载配置：
```bash
/feefly reload
```

## 📖 文档链接

- **详细使用指南**: [docs/plugins/feefly/README.md](../../docs/plugins/feefly/README.md)
- **项目主页**: [NewNanPlugins](https://github.com/NewNanCity/NewNanPlugins)
- **问题反馈**: [GitHub Issues](https://github.com/NewNanCity/NewNanPlugins/issues)

---

**FeeFly v2.0.0** - 让飞行更有价值的经济系统 ✈️💰

## 🏃 快速开始

### 前置要求

1. **Bukkit/Spigot/Paper** 服务器
2. **Vault** 插件
3. **经济插件**（如 EssentialsX）

### 安装步骤

1. **下载插件**: 从 Releases 页面下载最新版本
2. **安装依赖**: 确保服务器已安装 Vault 和经济插件
3. **放置插件**: 将 jar 文件放入 `plugins` 目录
4. **重启服务器**: 重启服务器以加载插件

### 基础配置

```yaml
# config.yml 最小配置
target-account: ""

flying:
  tick-per-count: 20        # 每次扣费间隔(tick)
  cost-per-count: 0.3       # 每次扣费金额
  fly-speed: 0.05           # 飞行速度
  command-cooldown-seconds: 3 # 命令冷却时间
```

### 快速使用

```bash
# 1. 给玩家基础权限
/lp user <player> permission set feefly.self true

# 2. 玩家使用飞行
/fly

# 3. 查看飞行列表
/feefly list
```

### 最简使用示例

## 🎮 命令系统

### 可用命令

| 命令                  | 权限                           | 描述                     |
| --------------------- | ------------------------------ | ------------------------ |
| `/fly [玩家]`         | `feefly.self` / `feefly.other` | 切换飞行状态             |
| `/feefly fly [玩家]`  | `feefly.self` / `feefly.other` | 切换飞行状态（完整命令） |
| `/feefly help [查询]` | `feefly.self`                  | 显示命令帮助             |
| `/feefly list`        | `feefly.list`                  | 显示当前飞行的玩家列表   |
| `/feefly reload`      | `feefly.reload`                | 重载插件配置             |

### 命令示例

```bash
# 基础飞行命令
/fly                # 切换自己的飞行状态
/fly <玩家>         # 切换指定玩家的飞行状态（需要 feefly.other 权限）

# 管理命令
/feefly list        # 显示当前飞行的玩家
/feefly reload      # 重载插件配置
/feefly help        # 显示帮助信息
```

## ✨ 功能特性

### 核心功能总览

FeeFly 插件提供完整的付费飞行解决方案，包含以下主要功能模块：

- **飞行管理系统** - 启动、停止、状态监控
- **经济集成系统** - Vault集成、按时间扣费
- **权限控制系统** - 细粒度权限管理
- **状态持久化** - 服务器重启后自动恢复
- **事件处理** - 智能处理死亡、传送等事件
- **多语言支持** - 国际化语言系统

### 简单用法

#### 基础飞行操作

```bash
# 切换自己的飞行状态
/fly

# 为指定玩家切换飞行状态（需要权限）
/fly Steve

# 查看当前飞行的玩家
/feefly list

# 重载插件配置
/feefly reload
```

#### 飞行状态显示

```
# 正常飞行时的状态显示
[飞行中] 剩余时间: 3分45秒 (余额 12.50 ₦)

# 免费飞行时的显示
#免费飞行# 祝 Steve 白嫖快乐！

# 余额不足警告
余额即将耗尽！
请尽快结束飞行
```

### 高级用法

#### 权限配置

```bash
# 给予玩家基础飞行权限
/lp user <player> permission set feefly.self true

# 给予玩家免费飞行权限
/lp user <player> permission set feefly.free true

# 给予管理员完整权限
/lp user <admin> permission set feefly.other true
/lp user <admin> permission set feefly.list true
/lp user <admin> permission set feefly.reload true
```

#### 高级配置

```yaml
# 高频低费配置
flying:
  tick-per-count: 10          # 更频繁的扣费（0.5秒一次）
  cost-per-count: 0.15        # 更低的单次费用
  fly-speed: 0.1              # 更快的飞行速度
  low-balance-warning-seconds: 30  # 更早的余额警告
  command-cooldown-seconds: 1 # 更短的命令冷却

# 资金转账配置
target-account: "server_fund" # 将费用转入服务器账户
```

## 🔐 权限管理

### 权限节点详解

| 权限节点                 | 描述                             | 默认值  |
| ------------------------ | -------------------------------- | ------- |
| `feefly.self`            | 为自己开启付费飞行的权限         | `false` |
| `feefly.other`           | 为别人开启付费飞行的权限         | `op`    |
| `feefly.free`            | 免费飞行的权限                   | `op`    |
| `feefly.list`            | 显示正在使用付费飞行的玩家的权限 | `op`    |
| `feefly.reload`          | 重载插件配置的权限               | `op`    |
| `feefly.bypass.cooldown` | 绕过命令冷却限制的权限           | `op`    |

### LuckPerms 配置示例

```bash
# 给予玩家基础飞行权限
/lp user <player> permission set feefly.self true

# 给予 VIP 玩家免费飞行权限
/lp user <vip> permission set feefly.free true

# 给予管理员完整权限
/lp user <admin> permission set feefly.other true
/lp user <admin> permission set feefly.list true
/lp user <admin> permission set feefly.reload true

# 创建权限组
/lp group default permission set feefly.self true
/lp group vip permission set feefly.free true
/lp group admin permission set feefly.other true
```
飞行中: Steve, Alex, Notch
```

#### `/fly reload` - 重载配置

**功能**: 重新加载插件配置文件

**用法**:
```bash
/fly reload
```

**效果**:
- 重新读取config.yml
- 更新飞行参数
- 保持现有飞行状态

#### `/fly help` - 帮助信息

**功能**: 显示插件帮助信息

**用法**:
```bash
/fly help
```

## 🔐 权限管理

### 权限节点

FeeFly插件使用细粒度的权限控制系统，确保功能的安全使用。

#### 基础权限

| 权限节点        | 描述               | 默认值  | 适用对象 |
| --------------- | ------------------ | ------- | -------- |
| `feefly.self`   | 为自己开启付费飞行 | `false` | 普通玩家 |
| `feefly.other`  | 为别人开启付费飞行 | `op`    | 管理员   |
| `feefly.free`   | 免费飞行           | `op`    | VIP玩家  |
| `feefly.list`   | 查看飞行列表       | `op`    | 管理员   |
| `feefly.reload` | 重载配置           | `op`    | 管理员   |

### 权限配置示例

#### LuckPerms配置

```bash
# 给予基础飞行权限
/lp group default permission set feefly.self true

# 给予VIP玩家免费飞行权限
/lp group vip permission set feefly.free true

# 给予管理员完整权限
/lp group admin permission set feefly.* true
```

#### PermissionsEx配置

```yaml
groups:
  default:
    permissions:
      - feefly.self

  vip:
    permissions:
      - feefly.self
      - feefly.free

  admin:
    permissions:
      - feefly.*
```

## ⚙️ 配置文件

### 配置文件详解

#### 完整配置示例

```yaml
# 飞行相关配置
flying:
  # 每次扣费的间隔（tick）
  # 20 tick = 1秒，10 tick = 0.5秒
  tick-per-count: 20

  # 每次扣费的金额
  cost-per-count: 0.3

  # 飞行速度（0.0-1.0）
  # 默认飞行速度是0.1，这里设置为一半
  fly-speed: 0.05

  # 低余额警告阈值（秒）
  # 当剩余飞行时间少于此值时显示警告
  low-balance-warning-seconds: 60

# 经济相关配置
economy:
  # 目标转账账户
  # 如果设置，扣除的费用将转入此账户
  # 如果为空或注释掉，费用将直接消失
  target-account: ""

# 日志配置
logging:
  # 是否启用调试模式
  debug-enabled: false

  # 是否启用文件日志
  file-logging-enabled: true

  # 日志文件保留天数
  log-retention-days: 7

  # 日志文件名前缀
  log-file-prefix: "FeeFly"

# 消息配置
message:
  # 玩家消息前缀
  player-prefix: "&7[&6牛腩飞行&7] &f"

  # 是否启用国际化
  language-enabled: true

# 性能监控配置
performance:
  # 是否启用性能监控
  monitoring-enabled: true

  # 慢操作阈值（毫秒）
  slow-operation-threshold-ms: 100

# 资源清理配置
cleanup:
  # 是否启用自动清理
  auto-cleanup-enabled: true

  # 清理间隔（秒）
  cleanup-interval-seconds: 30
```

#### 配置项说明

**飞行配置 (flying)**
- `tick-per-count`: 扣费间隔，20 tick = 1秒
- `cost-per-count`: 每次扣费金额
- `fly-speed`: 飞行速度，范围0.0-1.0
- `low-balance-warning-seconds`: 低余额警告阈值

**经济配置 (economy)**
- `target-account`: 费用转账目标账户，可选

**性能配置 (performance)**
- `monitoring-enabled`: 是否启用性能监控
- `slow-operation-threshold-ms`: 慢操作阈值

**清理配置 (cleanup)**
- `auto-cleanup-enabled`: 是否启用自动清理
- `cleanup-interval-seconds`: 清理间隔

## ⚙️ 配置文件

### 完整配置示例

```yaml
# FeeFly 付费飞行插件配置文件

# 消息前缀设置
player-message-prefix: "&7[&6牛腩飞行&7] &f"
console-message-prefix: "[FeeFly] "

# 目标转账账户（可选）
target-account: ""

# 飞行相关配置
flying:
  # 每次扣费的间隔（tick，20 tick = 1秒）
  tick-per-count: 20

  # 每次扣费的金额
  cost-per-count: 0.3

  # 飞行速度（0.0-1.0）
  fly-speed: 0.05

  # 低余额警告阈值（秒）
  low-balance-warning-seconds: 60

  # 命令使用冷却时间（秒）
  command-cooldown-seconds: 3

  # 状态验证间隔（秒）
  state-validation-interval-seconds: 30
```

### 配置说明

- `tick-per-count`: 计费间隔，建议保持20（1秒）
- `cost-per-count`: 每次计费金额，根据服务器经济调整
- `fly-speed`: 飞行速度，0.05是推荐值
- `target-account`: 费用转入的目标账户，留空则直接消失

## 🔧 故障排除

### 常见问题

#### 1. 插件无法启动

**错误**: `Vault plugin not found!`

**解决方案**:
```bash
# 检查 Vault 插件
/plugins

# 确保安装了 Vault 和经济插件
```

#### 2. 无法开启飞行

**可能原因**:
- 权限不足：需要 `feefly.self` 权限
- 余额不足：检查玩家经济余额
- 游戏模式：创造/观察者模式无法使用
- 命令冷却：等待冷却时间结束

**解决方案**:
```bash
# 检查权限
/lp user <player> permission check feefly.self

# 检查余额
/bal <player>

# 给予免费飞行权限测试
/lp user <player> permission set feefly.free true
```

#### 3. 飞行状态异常

**现象**: 服务器重启后飞行状态丢失

**解决方案**: 插件会自动恢复状态，如果仍有问题可重载配置：
```bash
/feefly reload
```

## 📖 文档链接

- **详细使用指南**: [docs/plugins/feefly/README.md](../../docs/plugins/feefly/README.md)
- **项目主页**: [NewNanPlugins](https://github.com/NewNanCity/NewNanPlugins)
- **问题反馈**: [GitHub Issues](https://github.com/NewNanCity/NewNanPlugins/issues)

---

**FeeFly v2.0.0** - 让飞行更有价值的经济系统 ✈️💰
# config.yml
logging:
  debug-enabled: true
  file-logging-enabled: true
```

### 性能问题

如果遇到性能问题：

```yaml
# 调整性能配置
performance:
  monitoring-enabled: true
  slow-operation-threshold-ms: 50

cleanup:
  cleanup-interval-seconds: 60  # 增加清理间隔
```

## 🏆 最佳实践

### 服务器配置建议

#### 小型服务器（<50人）

```yaml
flying:
  tick-per-count: 20          # 1秒扣费一次
  cost-per-count: 0.5         # 较高费用
  fly-speed: 0.05             # 标准速度
  low-balance-warning-seconds: 30

economy:
  target-account: "server"    # 费用进入服务器账户

performance:
  monitoring-enabled: false   # 关闭性能监控

cleanup:
  cleanup-interval-seconds: 60
```

#### 中型服务器（50-200人）

```yaml
flying:
  tick-per-count: 20
  cost-per-count: 0.3         # 中等费用
  fly-speed: 0.05
  low-balance-warning-seconds: 60

economy:
  target-account: ""          # 费用消失

performance:
  monitoring-enabled: true    # 启用性能监控
  slow-operation-threshold-ms: 100

cleanup:
  cleanup-interval-seconds: 30
```

#### 大型服务器（200+人）

```yaml
flying:
  tick-per-count: 10          # 更频繁扣费
  cost-per-count: 0.15        # 更低单次费用
  fly-speed: 0.03             # 更慢速度
  low-balance-warning-seconds: 120

economy:
  target-account: "tax"       # 专门的税收账户

performance:
  monitoring-enabled: true
  slow-operation-threshold-ms: 50  # 更严格的性能要求

cleanup:
  cleanup-interval-seconds: 15     # 更频繁清理
```

### 权限配置建议

#### 基础权限组

```bash
# 普通玩家
/lp group default permission set feefly.self true

# VIP玩家（免费飞行）
/lp group vip permission set feefly.self true
/lp group vip permission set feefly.free true

# 管理员
/lp group admin permission set feefly.* true
```

#### 经济平衡建议

**费用计算公式**:
```
每分钟费用 = (20 / tick-per-count) * cost-per-count * 60
```

**推荐设置**:
- 新手服务器: 每分钟5-10货币
- 普通服务器: 每分钟15-25货币
- 困难服务器: 每分钟30-50货币

### 安全建议

1. **定期备份配置文件**
2. **监控异常飞行行为**
3. **设置合理的费用标准**
4. **定期检查权限配置**
5. **关注插件更新**

### 与其他插件集成

#### Essentials集成

FeeFly会自动检测Essentials并优先使用其飞行功能：

```yaml
# Essentials配置建议
essentials:
  fly-in-creative: true
  fly-in-spectator: true
```

#### 经济插件集成

支持所有Vault兼容的经济插件：
- EssentialsX Economy
- CMI Economy
- PlayerPoints
- TokenManager

#### 权限插件集成

推荐使用LuckPerms进行权限管理：

```bash
# 创建飞行权限组
/lp creategroup flyers
/lp group flyers permission set feefly.self true
/lp group flyers parent add default

# 批量给予权限
/lp group default permission set feefly.self true
```

## 🛠️ 开发信息

### 技术栈

- **语言**: Kotlin
- **架构**: BasePlugin + 模块化设计
- **依赖管理**: Gradle
- **配置系统**: Jackson + YAML
- **任务调度**: 协程 + Bukkit Scheduler
- **生命周期**: 自动资源管理

### 项目结构

```
plugins/feefly/
├── src/main/
│   ├── kotlin/city/newnan/feefly/
│   │   ├── FeeFlyPlugin.kt           # 主插件类
│   │   ├── config/                   # 配置相关
│   │   ├── commands/                 # 命令处理
│   │   └── manager/                  # 管理器
│   └── resources/
│       ├── paper-plugin.yml         # 插件描述
│       ├── config.yml               # 默认配置
│       └── lang/                    # 国际化文件
└── build.gradle.kts                 # 构建配置
```

## 📝 更新日志

### v2.0.0 (当前版本)

- 🔄 完全重构，基于新的项目架构
- ✨ 现代化的 Kotlin 实现
- 🛡️ 自动资源生命周期管理
- 🌍 完整的国际化支持
- ⚡ 性能优化和内存管理
- 🔧 灵活的配置系统
- 📊 实时状态显示
- 🎯 CommandAPI 集成

### 从 v1.x 迁移

旧版本的配置文件需要手动迁移到新格式。主要变化：

1. 配置文件结构调整
2. 权限节点保持兼容
3. 命令使用方式不变
4. 数据文件格式更新

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。

---

## 📚 完整文档

更多详细文档请查看：
- **[文档导航](../../docs/plugins/feefly/index.md)** - 完整的文档导航
- **[快速参考](../../docs/plugins/feefly/quick-reference.md)** - 常用命令和配置
- **[配置示例](../../docs/plugins/feefly/configuration-examples.md)** - 不同场景的配置模板
- **[API参考](../../docs/plugins/feefly/api-reference.md)** - 开发者API文档

**NewNanCity Development Team**
*现代化 Minecraft 插件开发*
