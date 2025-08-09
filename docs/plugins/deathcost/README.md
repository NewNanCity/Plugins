# DeathCost 死亡扣费插件使用指南

一个现代化的死亡扣费插件，基于 NewNanPlugins 框架开发，提供灵活的死亡经济惩罚系统。

## 📖 目录

- [插件介绍](#插件介绍)
- [功能特性](#功能特性)
- [安装配置](#安装配置)
- [权限系统](#权限系统)
- [命令系统](#命令系统)
- [配置详解](#配置详解)
- [使用示例](#使用示例)
- [故障排除](#故障排除)

## 🚀 插件介绍

DeathCost 是一个支持阶梯式死亡扣费的经济插件，让玩家在死亡时根据其财富状况缴纳相应的费用。插件设计精巧，功能丰富，适合各种规模的服务器使用。

### 核心优势

- **🎯 阶梯扣费**: 支持简单固定扣费和复杂阶梯扣费两种模式
- **🔒 权限控制**: 完善的权限系统，支持死亡免费权限
- **💸 资金转移**: 可将扣除的费用转入指定账户
- **📢 消息系统**: 多渠道死亡消息通知（玩家/广播/控制台）
- **🌐 国际化**: 完整的中英文双语支持
- **⚡ 热重载**: 支持配置文件热重载，无需重启服务器
- **🏗️ 现代架构**: 基于 NewNanPlugins 框架，代码结构清晰

## ⭐ 功能特性

### 扣费模式

**简单模式**
- 固定金额扣费（如每次死亡扣 50 金币）
- 百分比扣费（如每次死亡扣余额的 1%）

**复杂模式（阶梯扣费）**
- 根据玩家财富分阶梯扣费
- 支持新手保护（低余额不扣费）
- 灵活的阶梯配置，可设置无限阶梯

### 权限保护

- `deathcost.bypass` 权限让玩家死亡时不扣费
- 完善的管理权限系统
- 支持 LuckPerms 等权限插件

### 资金管理

- 可选择将扣费转入指定账户（如服务器基金）
- 支持 UUID 和玩家名两种账户指定方式
- 与 Vault 经济系统深度集成

### 消息系统

- 玩家个人消息：通知玩家扣费金额
- 全服广播：可选的死亡扣费广播
- 控制台日志：记录所有死亡扣费事件
- 支持 Adventure 组件的现代化消息格式

## 🔧 安装配置

### 前置要求

1. **Bukkit/Spigot/Paper** 服务器
2. **Vault** 插件
3. **经济插件**（如 EssentialsX）

### 安装步骤

1. 下载 DeathCost 插件 jar 文件
2. 将文件放入服务器的 `plugins` 目录
3. 重启服务器或使用 PlugMan 等插件热加载
4. 插件会自动生成配置文件

### 基础配置

```yaml
# config.yml 最小可用配置
death-cost:
  use-simple-mode: true
  simple-mode:
    cost: 50.0
    if-percent: false

death-message:
  player-enable: true
  broadcast-enable: false
  console-enable: true
```

## 🔐 权限系统

### 权限节点

| 权限节点           | 描述           | 默认值  |
| ------------------ | -------------- | ------- |
| `deathcost.bypass` | 死亡不扣费权限 | `false` |
| `deathcost.reload` | 重载配置权限   | `op`    |
| `deathcost.status` | 查看状态权限   | `op`    |

### 权限配置示例

**使用 LuckPerms 设置权限**

```bash
# 给玩家设置死亡免费权限
/lp user <玩家名> permission set deathcost.bypass true

# 给管理员设置管理权限
/lp user <管理员> permission set deathcost.reload true
/lp user <管理员> permission set deathcost.status true

# 给 VIP 组设置死亡免费权限
/lp group vip permission set deathcost.bypass true

# 临时给新手7天死亡免费权限
/lp user <新手> permission settemp deathcost.bypass true 7d
```

## 🎮 命令系统

### 可用命令

| 命令                     | 权限要求           | 描述               |
| ------------------------ | ------------------ | ------------------ |
| `/deathcost`             | 无                 | 显示插件帮助信息   |
| `/deathcost help [查询]` | 无                 | 显示命令帮助       |
| `/deathcost reload`      | `deathcost.reload` | 重载插件配置       |
| `/deathcost status`      | `deathcost.status` | 显示插件状态和配置 |

### 命令使用示例

```bash
# 查看插件状态
/deathcost status

# 重载配置文件
/deathcost reload

# 查看帮助
/deathcost help

# 查看特定命令帮助
/deathcost help status
```

## ⚙️ 配置详解

### 完整配置文件

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

### 配置参数说明

#### 扣费阶梯配置

- `max`: 阶梯上限金额，`-1` 表示无上限
- `cost`: 扣费数值，根据 `if-percent` 决定是金额还是百分比
- `if-percent`:
  - `true`: `cost` 为百分比（0.001 = 0.1%）
  - `false`: `cost` 为固定金额

#### 目标账户配置

```yaml
# 方式1：使用玩家名
target-account: "server_bank"

# 方式2：使用 UUID
target-account: "550e8400-e29b-41d4-a716-446655440000"

# 方式3：不转账（费用直接消失）
target-account: null
```

## 📚 使用示例

### 示例1：简单固定扣费

适合小型服务器，每次死亡固定扣除 100 金币：

```yaml
death-cost:
  use-simple-mode: true
  simple-mode:
    cost: 100.0
    if-percent: false
  target-account: "server_fund"
```

### 示例2：简单百分比扣费

每次死亡扣除玩家余额的 2%：

```yaml
death-cost:
  use-simple-mode: true
  simple-mode:
    cost: 0.02        # 2%
    if-percent: true
```

### 示例3：新手友好的阶梯扣费

为新手提供保护，富有玩家承担更多：

```yaml
death-cost:
  use-simple-mode: false
  complex-mode:
    # 新手期：不扣费
    - max: 1000.0
      cost: 0.0
      if-percent: false

    # 成长期：固定扣费
    - max: 10000.0
      cost: 50.0
      if-percent: false

    # 发展期：低百分比
    - max: 100000.0
      cost: 0.005       # 0.5%
      if-percent: true

    # 富豪期：高百分比
    - max: -1
      cost: 0.02        # 2%
      if-percent: true
```

### 示例4：VIP 保护配置

给 VIP 玩家死亡免费权限：

```bash
# 创建 VIP 组并设置权限
/lp creategroup vip
/lp group vip permission set deathcost.bypass true

# 将玩家加入 VIP 组
/lp user <玩家名> parent add vip
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

#### 2. 死亡时不扣费

**可能原因**:
- 玩家有 `deathcost.bypass` 权限
- 配置文件设置错误
- 经济插件集成问题

**检查步骤**:
```bash
# 1. 检查玩家权限
/lp user <玩家名> permission check deathcost.bypass

# 2. 查看插件状态
/deathcost status

# 3. 检查配置文件语法
# 使用 YAML 验证器检查 config.yml
```

#### 3. 配置重载失败

**错误信息**: `插件配置重新加载失败`

**解决方案**:
```bash
# 1. 检查配置文件语法
# 2. 查看控制台错误信息
# 3. 恢复默认配置文件
/deathcost reload
```

#### 4. 目标账户设置失败

**错误信息**: `Player xxx not found`

**解决方案**:
```yaml
# 确保目标账户存在
# 方式1：使用确实存在的玩家名
target-account: "existing_player"

# 方式2：使用正确的 UUID
target-account: "550e8400-e29b-41d4-a716-446655440000"

# 方式3：不使用转账功能
target-account: null
```

### 调试技巧

1. **查看插件状态**: 使用 `/deathcost status` 检查当前配置
2. **测试权限**: 给自己添加/移除 `deathcost.bypass` 权限测试
3. **观察日志**: 开启控制台消息查看扣费记录
4. **分段测试**: 先使用简单模式测试，再配置复杂模式

### 性能优化建议

```yaml
# 生产环境推荐配置
death-message:
  player-enable: true      # 通知玩家
  broadcast-enable: false  # 关闭广播避免刷屏
  console-enable: true     # 保留日志记录

# 合理的阶梯设计
complex-mode:
  # 避免过多阶梯，建议不超过 5 个
  - max: 1000.0
    cost: 0.0
    if-percent: false
  # ... 其他阶梯
```

## 🤝 支持与反馈

- **项目地址**: [NewNanPlugins](https://github.com/NewNanCity/NewNanPlugins)
- **问题反馈**: [GitHub Issues](https://github.com/NewNanCity/NewNanPlugins/issues)
- **文档中心**: [插件文档](https://github.com/NewNanCity/NewNanPlugins/wiki)

---

**DeathCost v2.0.0** - 让死亡更有意义的经济系统 💀💰
