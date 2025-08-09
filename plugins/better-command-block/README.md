# BetterCommandBlock Plugin

增强的命令方块插件，提供安全控制和扩展功能。

## 功能特性

### 🛡️ 安全控制
- **命令黑名单**：阻止命令方块执行危险命令（如op、ban、stop等）
- **违规日志**：记录被阻止的命令执行尝试，包含位置和时间信息
- **自动销毁**：执行被禁命令的命令方块会被自动销毁

### 👀 查看功能
- **右键查看**：有权限的玩家可以右键查看命令方块内容
- **权限控制**：通过`better-command-block.read`权限控制查看功能

### ⚡ 扩展命令

#### `/cb pick` - 随机物品选择
从容器中随机选择物品到另一个容器：
- `/cb pick random-slot <up|down>` - 随机选择一个非空格子的物品
- `/cb pick random-item <up|down>` - 按物品数量权重随机选择

#### `/cb scoreboard players random` - 随机计分板
随机设置计分板分数：
- `/cb scoreboard players random set <目标> <计分板> <最小值> <最大值>`
- `/cb scoreboard players random add <目标> <计分板> <最小值> <最大值>`
- `/cb scoreboard players random sub <目标> <计分板> <最小值> <最大值>`

#### `/cb execute` - 增强执行命令
支持选择器和相对坐标的execute命令：
- `/cb execute as <选择器> run <命令>`
- 支持`@`选择器和`~`相对坐标
- 支持`@@`转义字符

## 配置文件

### config.yml
```yaml
# 被禁止的命令列表
blocked-commands:
  - op
  - minecraft:op
  - deop
  - ban
  - stop
  # ... 更多危险命令

# 调试模式
debug: false

# 日志配置
logging:
  file-logging-enabled: true
  log-file-prefix: "BetterCommandBlock_"
  log-level: INFO

# 消息配置
message:
  player-prefix: "§7[§6命令方块§7] §f"
  console-prefix: "[BetterCommandBlock] "
```

## 权限节点

| 权限 | 描述 | 默认值 |
|------|------|--------|
| `better-command-block.admin` | 管理员权限（包含所有子权限） | op |
| `better-command-block.reload` | 重载插件配置 | op |
| `better-command-block.execute` | 使用增强execute命令 | op |
| `better-command-block.read` | 查看命令方块内容 | false |

## 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/cb reload` | 重载插件配置 | `better-command-block.reload` |
| `/cb help` | 显示帮助信息 | 无 |
| `/cb pick <type> <direction>` | 随机物品选择 | 仅命令方块 |
| `/cb scoreboard players random <mode> <target> <objective> <min> <max>` | 随机计分板 | 仅命令方块 |
| `/cb execute <args...>` | 增强执行命令 | `better-command-block.execute` |

## 安装和使用

1. 将插件放入服务器的`plugins`目录
2. 重启服务器或使用`/plugman load BetterCommandBlock`
3. 编辑`plugins/BetterCommandBlock/config.yml`配置文件
4. 使用`/cb reload`重载配置

## 开发信息

- **版本**: 2.0.0
- **API版本**: 1.20
- **依赖**: 无硬依赖
- **兼容性**: Paper 1.20.1+

## 更新日志

### v2.0.0
- 完全重写插件架构，基于现代化的BasePlugin框架
- 使用Cloud命令框架替代ACF
- 添加完整的国际化支持（中英文）
- 改进错误处理和日志记录
- 优化性能和资源管理
- 添加配置热重载功能

### v1.x.x
- 原始版本功能
