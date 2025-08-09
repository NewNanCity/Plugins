# FeeFly 付费飞行插件使用指南

一个现代化的付费飞行插件，基于 NewNanPlugins 框架开发，提供基于经济系统的飞行功能。

## 📖 目录

- [插件介绍](#插件介绍)
- [功能特性](#功能特性)
- [安装配置](#安装配置)
- [权限系统](#权限系统)
- [命令系统](#命令系统)
- [配置详解](#配置详解)
- [使用示例](#使用示例)
- [API 接口](#api-接口)
- [故障排除](#故障排除)

## 🚀 插件介绍

FeeFly 是一个支持按时间收费的飞行插件，让玩家通过经济系统获得飞行能力。插件设计精巧，功能丰富，适合各种规模的服务器使用。

### 核心优势

- **💰 按时间计费**: 基于飞行时间的精确计费系统
- **🔒 权限控制**: 完善的权限系统，支持免费飞行权限
- **💸 资金转移**: 可将收取的费用转入指定账户
- **📊 实时监控**: 实时显示飞行状态和余额信息
- **🌐 国际化**: 完整的中英文双语支持
- **⚡ 热重载**: 支持配置文件热重载，无需重启服务器
- **🔧 丰富配置**: 飞行速度、计费间隔、冷却时间等多项可配置
- **🏗️ 现代架构**: 基于 NewNanPlugins 框架，代码结构清晰

## ⭐ 功能特性

### 飞行模式

**付费飞行**
- 按时间计费的飞行系统
- 可配置的计费间隔和费用
- 实时余额监控和警告

**免费飞行**
- `feefly.free` 权限提供免费飞行
- 不消耗经济资源
- 管理员和特权玩家专用

### 权限保护

- `feefly.self` - 为自己开启飞行权限
- `feefly.other` - 为他人开启飞行权限
- `feefly.free` - 免费飞行权限
- `feefly.list` - 查看飞行列表权限
- `feefly.reload` - 重载配置权限
- `feefly.bypass.cooldown` - 绕过冷却时间权限

### 智能管理

- **状态持久化**: 服务器重启后自动恢复飞行状态
- **事件监听**: 自动处理死亡、游戏模式切换、世界传送等事件
- **命令冷却**: 防止恶意频繁使用命令
- **余额警告**: 余额不足时自动提醒玩家

### 资金管理

- 支持将收费转入指定账户（如服务器基金）
- 支持 UUID 和玩家名两种账户指定方式
- 与 Vault 经济系统深度集成

## 🔧 安装配置

### 前置要求

1. **Bukkit/Spigot/Paper** 服务器
2. **Vault** 插件
3. **经济插件**（如 EssentialsX）

### 安装步骤

1. 下载 FeeFly 插件 jar 文件
2. 将文件放入服务器的 `plugins` 目录
3. 重启服务器或使用 PlugMan 等插件热加载
4. 插件会自动生成配置文件

### 基础配置

```yaml
# config.yml 最小可用配置
target-account: ""

flying:
  tick-per-count: 20        # 每秒扣费一次
  cost-per-count: 0.3       # 每次扣费 0.3 金币
  fly-speed: 0.05           # 飞行速度
  command-cooldown-seconds: 3 # 命令冷却3秒
```

## 🔐 权限系统

### 权限节点

| 权限节点                 | 描述                             | 默认值  |
| ------------------------ | -------------------------------- | ------- |
| `feefly.self`            | 为自己开启付费飞行的权限         | `false` |
| `feefly.other`           | 为别人开启付费飞行的权限         | `op`    |
| `feefly.free`            | 免费飞行的权限                   | `op`    |
| `feefly.list`            | 显示正在使用付费飞行的玩家的权限 | `op`    |
| `feefly.reload`          | 重载插件配置的权限               | `op`    |
| `feefly.bypass.cooldown` | 绕过命令冷却限制的权限           | `op`    |

### 权限配置示例

**使用 LuckPerms 设置权限**

```bash
# 给玩家设置基础飞行权限
/lp user <玩家名> permission set feefly.self true

# 给 VIP 玩家设置免费飞行权限
/lp user <VIP玩家> permission set feefly.free true

# 给管理员设置完整权限
/lp user <管理员> permission set feefly.other true
/lp user <管理员> permission set feefly.list true
/lp user <管理员> permission set feefly.reload true

# 给 VIP 组设置免费飞行权限
/lp group vip permission set feefly.free true

# 给普通玩家组设置基础权限
/lp group default permission set feefly.self true
```

## 🎮 命令系统

### 可用命令

| 命令                  | 权限要求                       | 描述                     |
| --------------------- | ------------------------------ | ------------------------ |
| `/fly [玩家]`         | `feefly.self` / `feefly.other` | 切换飞行状态             |
| `/feefly fly [玩家]`  | `feefly.self` / `feefly.other` | 切换飞行状态（完整命令） |
| `/feefly help [查询]` | `feefly.self`                  | 显示命令帮助             |
| `/feefly list`        | `feefly.list`                  | 显示当前飞行的玩家列表   |
| `/feefly reload`      | `feefly.reload`                | 重载插件配置             |

### 命令使用示例

```bash
# 为自己切换飞行状态
/fly

# 为指定玩家切换飞行状态（需要 feefly.other 权限）
/fly <玩家名>

# 查看当前飞行的玩家列表
/feefly list

# 重载配置文件
/feefly reload

# 查看帮助
/feefly help
```

## ⚙️ 配置详解

### 完整配置文件

```yaml
# FeeFly 付费飞行插件配置文件
# 版本: 2.0.0

# 玩家消息前缀
player-message-prefix: "&7[&6牛腩飞行&7] &f"

# 控制台消息前缀
console-message-prefix: "[FeeFly] "

# 目标转账账户（可选）
# 设置后，收取的费用会转入此账户
# 支持玩家名或 UUID，留空则费用直接消失
target-account: ""

# 飞行相关配置
flying:
  # 每次扣费的间隔（tick）
  # 20 tick = 1秒，建议值：10-40
  tick-per-count: 20

  # 每次扣费的金额
  # 建议根据服务器经济情况调整
  cost-per-count: 0.3

  # 飞行速度（0.0-1.0）
  # 默认飞行速度是0.1，这里设置为一半
  fly-speed: 0.05

  # 低余额警告阈值（秒）
  # 当剩余飞行时间少于此值时显示警告
  low-balance-warning-seconds: 60

  # 命令使用冷却时间（秒）
  # 防止玩家恶意频繁使用命令
  command-cooldown-seconds: 3

  # 状态验证间隔（秒）
  # 定期验证飞行玩家状态完整性的间隔
  state-validation-interval-seconds: 30
```

### 配置参数说明

#### 计费配置

- `tick-per-count`: 计费间隔，20 tick = 1秒
- `cost-per-count`: 每次计费金额
- **计费公式**: 每秒费用 = `cost-per-count × (20 ÷ tick-per-count)`

#### 飞行配置

- `fly-speed`: 飞行速度，范围 0.0-1.0，建议 0.05-0.1
- `low-balance-warning-seconds`: 余额警告阈值
- `command-cooldown-seconds`: 命令冷却时间
- `state-validation-interval-seconds`: 状态验证间隔

#### 目标账户配置

```yaml
# 方式1：使用玩家名
target-account: "server_bank"

# 方式2：使用 UUID
target-account: "550e8400-e29b-41d4-a716-446655440000"

# 方式3：不转账（费用直接消失）
target-account: ""
```

## 📚 使用示例

### 示例1：基础付费飞行配置

适合中型服务器，每秒扣费 0.3 金币：

```yaml
flying:
  tick-per-count: 20        # 每秒扣费
  cost-per-count: 0.3       # 每次0.3金币
  fly-speed: 0.05           # 适中的飞行速度
  command-cooldown-seconds: 3
```

### 示例2：高频低费配置

每0.5秒扣费，适合经济发达的服务器：

```yaml
flying:
  tick-per-count: 10        # 每0.5秒扣费
  cost-per-count: 0.15      # 每次0.15金币（相当于每秒0.3）
  fly-speed: 0.08           # 更快的飞行速度
```

### 示例3：低频高费配置

每2秒扣费，适合经济紧张的服务器：

```yaml
flying:
  tick-per-count: 40        # 每2秒扣费
  cost-per-count: 0.6       # 每次0.6金币（相当于每秒0.3）
  fly-speed: 0.04           # 较慢的飞行速度
```

### 示例4：VIP 特权配置

给不同等级的玩家不同权限：

```bash
# 普通玩家：只能为自己开启飞行
/lp group default permission set feefly.self true

# VIP 玩家：免费飞行
/lp group vip permission set feefly.free true

# 管理员：所有权限
/lp group admin permission set feefly.other true
/lp group admin permission set feefly.list true
/lp group admin permission set feefly.reload true
/lp group admin permission set feefly.bypass.cooldown true
```

## 🔌 API 接口

FeeFly 提供了完整的 API 接口供其他插件调用：

### 获取 API 实例

```java
// Java
FeeFlyService service = Bukkit.getServicesManager()
    .getRegistration(FeeFlyService.class)
    .getProvider();
```

```kotlin
// Kotlin
val service = Bukkit.getServicesManager()
    .getRegistration(FeeFlyService::class.java)
    ?.provider
```

### 主要 API 方法

```java
// 检查玩家是否在飞行
boolean isFlying(Player player);

// 开启玩家飞行
void startFly(Player player);

// 结束玩家飞行
void endFly(Player player);

// 切换玩家飞行状态
void toggleFly(Player player);

// 获取所有飞行玩家
Map<Player, FlyingPlayer> getFlyingPlayers();

// 获取玩家飞行数据
FlyingPlayer getFlyingPlayer(Player player);
```

### 事件监听

```java
// 飞行开始事件
@EventHandler
public void onFlyStart(FlyStartEvent event) {
    Player player = event.getPlayer();
    // 处理飞行开始
}

// 飞行结束事件
@EventHandler
public void onFlyEnd(FlyEndEvent event) {
    Player player = event.getPlayer();
    FlyEndEvent.EndReason reason = event.getReason();
    // 处理飞行结束
}
```

## 🔍 故障排除

### 常见问题

#### 1. 插件无法加载

**错误信息**: `Vault plugin not found!`

**解决方案**:
```bash
# 检查 Vault 插件是否安装
/plugins

# 如果没有，下载并安装 Vault 插件
# 同时确保安装了经济插件（如 EssentialsX）
```

#### 2. 无法开启飞行

**可能原因**:
- 玩家没有 `feefly.self` 权限
- 玩家在创造模式或观察者模式
- 玩家余额不足
- 命令冷却中

**检查步骤**:
```bash
# 1. 检查玩家权限
/lp user <玩家名> permission check feefly.self

# 2. 检查玩家游戏模式
/gamemode <玩家名>

# 3. 检查玩家余额
/bal <玩家名>

# 4. 检查插件状态
/feefly list
```

#### 3. 飞行状态异常

**现象**: 玩家掉线重连后飞行状态丢失

**解决方案**:
```yaml
# 检查状态验证间隔设置
flying:
  state-validation-interval-seconds: 30  # 建议30秒
```

#### 4. 扣费异常

**现象**: 扣费过快或过慢

**解决方案**:
```yaml
# 检查计费配置
flying:
  tick-per-count: 20      # 20 tick = 1秒
  cost-per-count: 0.3     # 每次扣费金额
```

#### 5. 目标账户设置失败

**错误信息**: `Player xxx not found`

**解决方案**:
```yaml
# 确保目标账户存在
# 方式1：使用确实存在的玩家名
target-account: "existing_player"

# 方式2：使用正确的 UUID
target-account: "550e8400-e29b-41d4-a716-446655440000"

# 方式3：不使用转账功能
target-account: ""
```

### 调试技巧

1. **查看飞行列表**: 使用 `/feefly list` 检查当前飞行状态
2. **测试权限**: 给自己添加/移除相关权限测试功能
3. **观察控制台**: 查看插件启动和运行日志
4. **分段测试**: 先给管理员权限测试，再配置普通玩家权限

### 性能优化建议

```yaml
# 生产环境推荐配置
flying:
  tick-per-count: 20                    # 保持1秒间隔
  state-validation-interval-seconds: 60  # 增加验证间隔
  command-cooldown-seconds: 5            # 增加冷却时间防滥用
```

### 常见配置错误

1. **飞行速度过高**: `fly-speed` 设置超过 0.2 可能导致玩家控制困难
2. **计费间隔过短**: `tick-per-count` 小于 10 可能影响服务器性能
3. **冷却时间过短**: `command-cooldown-seconds` 小于 1 可能被恶意利用

## 🤝 支持与反馈

- **项目地址**: [NewNanPlugins](https://github.com/NewNanCity/NewNanPlugins)
- **问题反馈**: [GitHub Issues](https://github.com/NewNanCity/NewNanPlugins/issues)
- **文档中心**: [插件文档](https://github.com/NewNanCity/NewNanPlugins/wiki)

---

**FeeFly v2.0.0** - 让飞行更有价值的经济系统 ✈️💰
