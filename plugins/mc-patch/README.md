# MCPatch Plugin

全面的 Minecraft 服务器安全与稳定性保护插件。

## 功能特性

### 🛡️ 安全防护模块

#### AntiWorldDownload 模块
- **功能**：防止玩家使用 World Downloader 模组下载服务器地图
- **检测方式**：监听插件消息通道 `WDL|INIT` 和 `WDL|REQUEST`
- **处理方式**：检测到使用时立即踢出玩家并记录日志

#### Contraband 模块
- **功能**：检测和清理违禁物品
- **检测范围**：玩家背包、箱子、末影箱、潜影盒等容器
- **违禁物品**：基岩、屏障方块、末地传送门、命令方块等系统物品
- **处理方式**：自动删除违禁物品并通知玩家和管理员

#### AntiCrash 模块
- **功能**：防止各种崩服漏洞
- **保护内容**：
  - 发射器边界漏洞（Y=0 和 Y=255）
  - 恶意 NBT 数据
  - 超长字符串攻击
  - 大量实体生成

#### AntiDuplication 模块 (新增)
- **功能**：检测和防止物品复制漏洞
- **监控内容**：
  - 异常物品数量增加
  - 可疑的容器操作模式
  - 快速重复操作
- **记录功能**：详细的物品流转日志

### ⚡ 性能保护模块

#### PerformanceProtection 模块 (新增)
- **红石保护**：限制高频红石电路，防止卡顿机器
- **实体控制**：限制区域内实体数量，防止实体过载
- **区块保护**：限制区块加载频率，防止内存消耗攻击

#### NetworkSecurity 模块 (新增)
- **连接保护**：限制单IP连接频率，防止连接洪水攻击
- **数据包检测**：检测异常数据包，防止恶意客户端攻击
- **IP管理**：支持黑名单和白名单功能

### 📊 监控和日志

#### SecurityLogger 系统
- **事件记录**：记录所有安全相关事件
- **严重程度分类**：INFO、WARN、ERROR、CRITICAL
- **实时监控**：支持实时威胁检测和自动响应
- **管理员通知**：重要安全事件实时通知在线管理员

## 配置文件

### config.yml
```yaml
# 模块启用配置
modules:
  anti-world-download:
    enabled: true
    kick-message: "请勿使用WDL插件"
    log-violations: true
  
  contraband:
    enabled: true
    check-inventories:
      - CHEST
      - ENDER_CHEST
      - PLAYER
      - SHULKER_BOX
    blocked-materials:
      - BEDROCK
      - BARRIER
      - END_PORTAL
      - COMMAND_BLOCK
    auto-remove: true
  
  anti-crash:
    enabled: true
    dispenser-protection: true
    nbt-protection: true
    string-length-limit: 1000
  
  anti-duplication:
    enabled: true
    monitor-item-increase: true
    suspicious-operation-threshold: 5
    log-item-transfers: true
  
  performance-protection:
    enabled: true
    redstone-frequency-limit: 20
    entity-limit-per-chunk: 100
    chunk-load-rate-limit: 10
  
  network-security:
    enabled: true
    connection-rate-limit: 5
    packet-inspection: true
    ip-blacklist: []

# 日志配置
logging:
  file-logging-enabled: true
  log-file-prefix: "MCPatch_Security_"
  log-level: INFO
  real-time-monitoring: true

# 消息配置
message:
  player-prefix: "§7[§cMCPatch§7] §f"
  console-prefix: "[MCPatch] "
```

## 权限节点

| 权限 | 描述 | 默认值 |
|------|------|--------|
| `mc-patch.admin` | 管理员权限（包含所有子权限） | op |
| `mc-patch.reload` | 重载插件配置 | op |
| `mc-patch.bypass.all` | 绕过所有安全检查 | false |
| `mc-patch.bypass.contraband` | 绕过违禁物品检查 | false |
| `mc-patch.bypass.anti-crash` | 绕过崩服防护 | false |
| `mc-patch.bypass.performance` | 绕过性能限制 | false |

## 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/mcpatch reload` | 重载插件配置 | `mc-patch.reload` |
| `/mcpatch status` | 查看插件状态 | `mc-patch.admin` |
| `/mcpatch logs [lines]` | 查看安全日志 | `mc-patch.admin` |
| `/mcpatch blacklist <add/remove> <ip>` | 管理IP黑名单 | `mc-patch.admin` |

## 安装和使用

1. 将插件放入服务器的 `plugins` 目录
2. 重启服务器或使用 `/plugman load MCPatch`
3. 编辑 `plugins/MCPatch/config.yml` 配置文件
4. 使用 `/mcpatch reload` 重载配置

## 兼容性

- **服务器版本**: Paper 1.20.1+
- **Java版本**: Java 17+
- **依赖**: 无硬依赖
- **冲突**: 与其他安全插件可能存在功能重叠

## 性能影响

- **CPU使用**: 轻微增加（主要在事件处理）
- **内存使用**: 约 10-20MB（取决于监控数据量）
- **网络影响**: 几乎无影响
- **磁盘使用**: 日志文件可能较大，建议定期清理

## 开发信息

- **版本**: 2.0.0
- **架构**: BasePlugin + BaseModule
- **技术栈**: Kotlin 2.2.0, Paper API 1.20.1
- **构建工具**: Gradle 8.8

## 更新日志

### v2.0.0
- 完全重写插件架构，基于现代化的BasePlugin框架
- 从Addon架构迁移到BaseModule架构
- 添加反物品复制检测模块
- 添加性能保护模块
- 添加网络安全模块
- 实现完整的安全日志系统
- 添加完整的国际化支持（中英文）
- 改进错误处理和资源管理

### v1.x.x
- 原始版本功能（基于ExtendedJavaPlugin和Addon架构）

## 贡献指南

- 遵循 Kotlin 官方代码风格
- 使用 BasePlugin 和 BaseModule 架构模式
- 添加完整的国际化支持
- 编写单元测试
- 更新文档

## 许可证

本项目采用 MIT 许可证。
