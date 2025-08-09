# DeathCost 死亡扣费插件

现代化的死亡扣费插件，基于 NewNanPlugins 框架开发，提供完整的基于经济系统的死亡惩罚功能。

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

DeathCost 是一个现代化的死亡扣费系统，让玩家在死亡时根据其财富状况扣除相应的费用。插件基于 NewNanPlugins 框架开发，具有以下核心特性：

**🆚 对比传统死亡惩罚插件**

| 特性       | 传统死亡插件 | DeathCost       |
| ---------- | ------------ | --------------- |
| 扣费模式   | 固定金额     | ✅ 阶梯扣费      |
| 经济集成   | 基础         | ✅ 深度Vault集成 |
| 权限控制   | 简单         | ✅ 细粒度权限    |
| 消息自定义 | 有限         | ✅ 多渠道消息    |
| 转账功能   | ❌            | ✅ 基金转账      |
| 配置灵活性 | 一般         | ✅ 双模式配置    |
| 国际化     | ❌            | ✅ 中英文双语    |

**💡 核心特性**
- **阶梯扣费**: 根据玩家财富状况进行阶梯式扣费
- **权限保护**: 支持 `deathcost.bypass` 死亡免费权限
- **转账功能**: 可将扣费转入指定基金账户
- **消息系统**: 支持玩家、广播、控制台多渠道消息
- **双模式**: 简单模式和复杂阶梯模式
- **热重载**: 支持配置文件热重载

**🎯 使用场景**
```yaml
# 示例：新手保护 + 阶梯扣费
complex-mode:
  - max: 5000.0    # 新手期（0-5000）不扣费
    cost: 0.0
    if-percent: false
  - max: 50000.0   # 中期（5000-50000）按0.1%扣费
    cost: 0.001
    if-percent: true
  - max: -1.0      # 后期（50000+）按0.24%扣费
    cost: 0.0024
    if-percent: true
```

## 🚀 快速开始

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
death-cost:
  use-simple-mode: true
  simple-mode:
    cost: 50.0
    if-percent: false
  target-account: null

death-message:
  player-enable: true
  broadcast-enable: false
  console-enable: true
```

### 快速使用

```bash
# 1. 查看插件状态
/deathcost status

# 2. 给予玩家免费死亡权限
/lp user <player> permission set deathcost.bypass true

# 3. 重载配置
/deathcost reload
```

## ⭐ 功能特性

### 总览

DeathCost提供两种扣费模式和完整的管理功能：

- **简单模式**: 固定金额或百分比扣费
- **复杂模式**: 多阶梯渐进式扣费
- **权限控制**: 支持死亡免费权限
- **转账功能**: 扣费可转入基金账户
- **消息系统**: 多渠道死亡消息通知
- **实时重载**: 支持配置热重载

### 简单用法

#### 基础扣费设置

```yaml
# 简单模式 - 固定扣费50金币
death-cost:
  use-simple-mode: true
  simple-mode:
    cost: 50.0
    if-percent: false
```

#### 百分比扣费

```yaml
# 简单模式 - 扣费1%
death-cost:
  use-simple-mode: true
  simple-mode:
    cost: 0.01
    if-percent: true
```

### 高级用法

#### 阶梯扣费配置

```yaml
# 复杂模式 - 阶梯扣费
death-cost:
  use-simple-mode: false
  complex-mode:
    # 新手保护：0-5000不扣费
    - max: 5000.0
      cost: 0.0
      if-percent: false
    # 初级：5000-10000固定扣20
    - max: 10000.0
      cost: 20.0
      if-percent: false
    # 中级：10000-50000按0.1%扣费
    - max: 50000.0
      cost: 0.001
      if-percent: true
    # 高级：50000+按0.24%扣费
    - max: -1.0
      cost: 0.0024
      if-percent: true
```

#### 基金转账设置

```yaml
# 将扣费转入基金账户
death-cost:
  target-account: "server_fund"
```

### API参考

#### 权限节点

```yaml
permissions:
  deathcost.bypass:      # 死亡不扣费
  deathcost.admin:       # 管理员权限
  deathcost.reload:      # 重载配置
  deathcost.status:      # 查看状态
```

#### 命令列表

```bash
/deathcost              # 显示插件信息
/deathcost reload       # 重载配置
/deathcost status       # 显示状态
/deathcost help         # 显示帮助
```

## 🎮 命令系统

### 可用命令

| 命令                     | 权限               | 描述                   |
| ------------------------ | ------------------ | ---------------------- |
| `/deathcost`             | 无                 | 显示插件帮助信息       |
| `/deathcost help [查询]` | 无                 | 显示命令帮助           |
| `/deathcost reload`      | `deathcost.reload` | 重载插件配置           |
| `/deathcost status`      | `deathcost.status` | 显示插件状态和配置信息 |

### 命令示例

```bash
# 查看插件状态
/deathcost status

# 重载配置
/deathcost reload

# 查看帮助
/deathcost help

# 查看特定命令的帮助
/deathcost help status
```

## 🔐 权限管理

### 权限节点详解

| 权限节点           | 描述               | 默认值  |
| ------------------ | ------------------ | ------- |
| `deathcost.bypass` | 死亡不会扣费的权限 | `false` |
| `deathcost.reload` | 重载插件配置的权限 | `op`    |
| `deathcost.status` | 查看插件状态的权限 | `op`    |

### LuckPerms 配置示例

```bash
# 给予玩家死亡免费权限
/lp user <player> permission set deathcost.bypass true

# 给予管理员权限
/lp user <admin> permission set deathcost.reload true
/lp user <admin> permission set deathcost.status true

# 创建VIP组免费死亡
/lp group vip permission set deathcost.bypass true

# 临时给新手7天免费死亡权限
/lp user <newbie> permission settemp deathcost.bypass true 7d
```

## ⚙️ 配置文件

### 完整配置示例

```yaml
# DeathCost 死亡扣费插件配置文件

# 消息前缀设置
player-message-prefix: "&7[&6牛腩小镇&7] &f"
console-message-prefix: "[DeathCost] "

# 死亡扣费配置
death-cost:
  # 目标转账账户（可选）
  # 设置后，扣除的费用会转入此账户
  # 支持玩家名或 UUID，留空则费用直接消失
  target-account: null

  # 扣费模式选择
  # true: 简单模式，false: 复杂阶梯模式
  use-simple-mode: false

  # 简单模式配置
  simple-mode:
    cost: 50.0        # 扣费数值
    if-percent: false # true=百分比，false=固定金额

  # 复杂模式配置（阶梯扣费）
  complex-mode:
    # 第一阶梯：0-5000 金币，不扣费（新手保护）
    - max: 5000.0
      cost: 0.0
      if-percent: false
    # 第二阶梯：5000-10000 金币，固定扣费 20
    - max: 10000.0
      cost: 20.0
      if-percent: false
    # 第三阶梯：10000-50000 金币，按 0.1% 扣费
    - max: 50000.0
      cost: 0.001
      if-percent: true
    # 第四阶梯：50000+ 金币，按 0.24% 扣费
    - max: -1          # -1 表示无上限
      cost: 0.0024
      if-percent: true

# 死亡消息配置
death-message:
  player-enable: true    # 向玩家发送扣费消息
  broadcast-enable: false # 全服广播死亡扣费
  console-enable: false   # 控制台记录扣费日志
```

### 配置说明

#### 扣费模式选择

- `use-simple-mode: true`: 使用简单模式
- `use-simple-mode: false`: 使用复杂阶梯模式

#### 阶梯配置说明

- `max`: 阶梯上限，`-1.0` 表示无上限
- `cost`: 扣费金额或百分比
- `if-percent`: `true` 为百分比，`false` 为固定金额

#### 目标账户设置

```yaml
# 使用玩家名
target-account: "server_fund"

# 使用 UUID
target-account: "550e8400-e29b-41d4-a716-446655440000"

# 不转账（费用直接消失）
target-account: null
```

## 🔧 故障排除

### 常见错误

#### 1. 经济系统未连接

**错误**: `Vault plugin not found!`

**解决方案**:
```bash
# 检查Vault插件
/plugins

# 确保安装了 Vault 和经济插件（如 EssentialsX）
```

#### 2. 配置文件错误

**错误**: `插件配置重新加载失败`

**解决方案**:
```bash
# 检查YAML语法
# 使用在线YAML验证器检查 config.yml

# 重新加载配置
/deathcost reload
```

#### 3. 权限问题

**错误**: `You don't have permission`

**解决方案**:
```bash
# 检查权限
/lp user <player> permission check deathcost.bypass

# 给予权限
/lp user <player> permission set deathcost.reload true
```

#### 4. 死亡时不扣费

**可能原因**:
- 玩家有 `deathcost.bypass` 权限
- 配置文件中扣费设置为 0
- 经济插件集成问题

**检查步骤**:
```bash
# 查看插件状态
/deathcost status

# 检查玩家权限
/lp user <player> permission check deathcost.bypass
```

### 调试建议

1. **查看插件状态**: 使用 `/deathcost status` 检查当前配置
2. **测试权限**: 给自己添加/移除 `deathcost.bypass` 权限测试
3. **观察控制台**: 开启控制台消息查看扣费记录
4. **分段测试**: 先使用简单模式测试，再配置复杂模式

## 📖 文档链接

- **详细使用指南**: [docs/plugins/deathcost/README.md](../../docs/plugins/deathcost/README.md)
- **项目主页**: [NewNanPlugins](https://github.com/NewNanCity/NewNanPlugins)
- **问题反馈**: [GitHub Issues](https://github.com/NewNanCity/NewNanPlugins/issues)

---

**DeathCost v2.0.0** - 让死亡更有意义的经济系统 💀💰
