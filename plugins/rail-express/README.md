# RailExpress - 矿车速度控制插件

RailExpress是一个基于NewNanCity插件框架的现代化Minecraft插件，让矿车在不同方块上拥有不同的最大速度。

## 特性

### 🚀 现代化架构
- 基于项目标准架构，集成配置管理、日志记录等功能
- 使用Kotlin编写，提供类型安全和现代化的API
- 完整的生命周期管理和资源清理

### ⚡ 灵活的速度控制
- 根据铁轨下方的方块类型设置矿车速度
- 支持仅动力铁轨模式或所有铁轨类型
- 可配置是否允许非玩家实体（如动物）触发速度变化
- 红石块特殊处理：保持矿车当前最大速度

### 🌍 多世界支持
- 支持为不同世界组配置不同的速度规则
- 灵活的世界分组配置
- 自动处理世界加载和卸载

### ⚙️ 简单易用
- 直观的YAML配置文件
- 实时重载配置，无需重启服务器
- 详细的状态查询命令

## 快速开始

### 安装

1. 将`RailExpress.jar`放入服务器的`plugins`目录
2. 重启服务器或使用`/reload`命令
3. 编辑`plugins/RailExpress/config.yml`配置文件

### 基本配置

```yaml
groups:
  - worlds: ['world', 'world_nether', 'world_the_end']
    allow-non-player: false
    power-rail-only: true
    block-type:
      SOUL_SAND: 0.2        # 魂沙 - 减速
      STONE_BRICKS: 0.6     # 石砖 - 稍快
      EMERALD_BLOCK: 1.2    # 绿宝石块 - 很快
      BARRIER: 1.5          # 屏障方块 - 最快
```

### 基本命令

```bash
# 查看帮助
/railexpress help

# 重载配置
/railexpress reload

# 查看插件信息
/railexpress info

# 查看当前状态
/railexpress status
```

## 配置说明

### 世界组配置

- `worlds`: 应用此配置的世界列表
- `allow-non-player`: 是否允许非玩家实体（如动物）触发速度变化
- `power-rail-only`: 是否仅在动力铁轨上生效
- `block-type`: 方块类型与速度的映射

### 速度值说明

- 默认矿车速度：0.4
- 最大矿车速度：1.5
- 建议速度范围：0.1 - 1.5

### 特殊方块

- **红石块**: 保持矿车当前的最大速度，不改变速度值

## 权限

- `railexpress.use`: 使用基本功能（默认：所有玩家）
- `railexpress.reload`: 重载配置（默认：OP）
- `railexpress.admin`: 管理员权限（默认：OP）

## 开发信息

- **语言**: Kotlin
- **最低Java版本**: 21
- **Minecraft版本**: 1.21+
- **依赖**: Paper API
- **许可证**: MIT

## 从旧版本迁移

如果你之前使用的是旧版RailExpress，配置文件格式已经更新。新版本提供了更清晰的配置结构和更好的性能。

### 主要变化

1. 配置文件结构更加清晰
2. 支持多世界组配置
3. 更好的错误处理和日志记录
4. 现代化的命令系统

## 故障排除

### 常见问题

1. **矿车速度没有变化**
   - 检查世界是否在配置的世界列表中
   - 确认铁轨下方的方块类型是否在配置中
   - 检查是否启用了`power-rail-only`但使用的不是动力铁轨

2. **配置重载失败**
   - 检查YAML语法是否正确
   - 确认方块类型名称是否正确（使用Minecraft官方名称）

3. **权限问题**
   - 确认玩家拥有相应的权限节点
   - 检查权限插件配置

## 更新日志

### v2.0.0
- 完全重构为现代化架构
- 基于NewNanCity插件框架
- 改进的配置系统
- 更好的性能和稳定性
- 新的命令系统

### v1.0.2 (旧版本)
- 基于helper库的实现
- 基本的速度控制功能
