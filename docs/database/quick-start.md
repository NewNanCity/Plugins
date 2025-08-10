# Database 模块快速开始

> 📋 **状态**: 文档规划中，内容正在完善

## 概述

Database 模块提供了强大的数据库集成功能，支持多种数据库类型和 ORM 框架。本页面将指导您快速上手使用 Database 模块。

## 快速开始步骤

### 1. 添加依赖

```kotlin
// 在您的插件中添加 database 模块依赖
dependencies {
    implementation(project(":modules:database"))
}
```

### 2. 配置数据库连接

```kotlin
// 在插件配置中添加数据库配置
data class MyPluginConfig(
    val database: DatabaseConfig = DatabaseConfig()
) : BasePluginConfig()

data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "minecraft",
    val username: String = "root",
    val password: String = ""
)
```

### 3. 在插件中使用

```kotlin
class MyPlugin : BasePlugin() {
    private lateinit var databaseManager: DatabaseManager
    
    override fun onPluginEnable() {
        // 初始化数据库管理器
        databaseManager = DatabaseManager(this)
        super.onPluginEnable()
    }
}
```

### 4. 定义数据模型

```kotlin
@Entity
@Table(name = "players")
data class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true)
    val uuid: String,
    
    val name: String,
    val level: Int = 1,
    val experience: Long = 0
)
```

## 支持的数据库

- **MySQL** - 推荐用于生产环境
- **PostgreSQL** - 高性能关系型数据库
- **SQLite** - 轻量级本地数据库
- **H2** - 内存数据库，适合测试

## 相关文档

- [📖 模块介绍](intro.md) - 了解 Database 模块的核心概念
- [💡 基础概念](concepts.md) - 深入了解数据库系统设计
- [⚠️ 故障排除](troubleshooting.md) - 常见问题解决方案

## 下一步

- [连接池配置](connection-pool.md) - 优化数据库连接性能
- [事务管理](transactions.md) - 了解事务处理机制
- [API 参考](api-reference.md) - 完整的 API 文档

---

**📝 注意**: 此文档正在完善中，如有疑问请参考 [README](README.md) 或查看 [示例代码](examples.md)。
