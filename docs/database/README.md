# Database 模块用户教程

欢迎使用现代化的数据库管理框架！本教程将带您从零开始学习如何使用 Database 模块构建强大的数据库应用。

## 📚 文档目录

### 基础教程
- [📖 介绍](intro.md) - 5分钟快速了解Database模块
- [🚀 快速开始](quick-start.md) - 第一个数据库连接
- [🎯 基础概念](concepts.md) - 核心概念详解

### 功能指南
- [🔗 连接池管理](connection-pool.md) - HikariCP连接池配置
- [🗄️ 多数据库支持](databases.md) - MySQL、PostgreSQL、SQLite、H2
- [🛠️ DSL配置](dsl.md) - 流畅的配置API
- [💼 事务管理](transactions.md) - 事务支持和自动回滚
- [⚡ 批量操作](batch-operations.md) - 高性能批量SQL执行
- [📊 监控统计](monitoring.md) - 连接池监控和健康检查

### 高级主题
- [🔧 性能优化](performance.md) - MySQL优化配置和最佳实践
- [🔄 生命周期管理](lifecycle.md) - 资源管理和自动清理
- [🏗️ 架构设计](architecture.md) - 模块架构和设计模式
- [⚙️ 自定义扩展](extensions.md) - 自定义数据库支持

### 参考资料
- [📋 API参考](api-reference.md) - 完整API文档
- [💡 最佳实践](best-practices.md) - 开发建议和模式
- [🔧 故障排除](troubleshooting.md) - 常见问题解决
- [📝 示例代码](examples.md) - 完整示例集合

## 🎯 快速导航

### 我想要...
- **连接第一个数据库** → [快速开始](quick-start.md)
- **配置连接池** → [连接池管理](connection-pool.md)
- **使用不同数据库** → [多数据库支持](databases.md)
- **使用DSL语法** → [DSL配置](dsl.md)
- **管理事务** → [事务管理](transactions.md)
- **批量操作** → [批量操作](batch-operations.md)
- **监控数据库** → [监控统计](monitoring.md)
- **优化性能** → [性能优化](performance.md)
- **解决问题** → [故障排除](troubleshooting.md)

## 🆕 最新特性

- **HikariCP连接池** - 高性能连接池管理，融合violet/sql优化配置
- **多数据库支持** - MySQL、PostgreSQL、SQLite、H2全面支持
- **DSL配置API** - 流畅的Kotlin DSL风格配置，支持infix函数
- **事务管理** - 内置事务支持，异常时自动回滚
- **批量操作** - 支持批量SQL执行，大幅提升性能
- **连接池监控** - 实时监控连接池状态和健康检查
- **MySQL优化** - 内置HikariCP官方推荐的MySQL性能优化配置
- **资源管理** - 实现Terminable接口，自动清理资源

## 🔧 支持的数据库

### 生产级数据库
- **MySQL** - 最流行的开源关系型数据库
- **PostgreSQL** - 功能强大的开源对象关系型数据库

### 轻量级数据库
- **SQLite** - 嵌入式数据库，适合小型应用
- **H2** - 内存/文件数据库，适合测试和开发

### 依赖配置
```kotlin
dependencies {
    // Database 模块（已包含 HikariCP）
    implementation(project(":modules:database"))

    // 根据需要添加具体的数据库驱动
    implementation("mysql:mysql-connector-java:8.0.33")      // MySQL
    implementation("org.postgresql:postgresql:42.7.1")       // PostgreSQL
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")        // SQLite
    implementation("com.h2database:h2:2.2.224")              // H2

    // 如果使用 ORM（推荐）
    implementation("org.ktorm:ktorm-core:3.6.0")             // Ktorm 核心
    implementation("org.ktorm:ktorm-support-mysql:3.6.0")    // MySQL 方言支持
    implementation("org.ktorm:ktorm-support-postgresql:3.6.0") // PostgreSQL 方言支持
}
```

## 🚀 快速预览

### Kotlin DSL风格（推荐）
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // MySQL数据库配置
        val db = mysql {
            host = "localhost"
            port = 3306
            database = "myserver"
            username = "minecraft"
            password = "secret123"

            // 连接池配置
            pool {
                maximumPoolSize = 10
                minimumIdle = 2
                connectionTimeout = 30000
                idleTimeout = 600000
            }

            // MySQL优化配置
            properties {
                "useSSL" to "false"
                "useUnicode" to "true"
                "characterEncoding" to "utf8mb4"
                "serverTimezone" to "Asia/Shanghai"
            }
        }

        // 使用数据库
        tasks {
            runAsync {
                db.useConnection { connection ->
                    val statement = connection.prepareStatement(
                        "SELECT * FROM players WHERE uuid = ?"
                    )
                    statement.setString(1, player.uniqueId.toString())
                    val result = statement.executeQuery()

                    while (result.next()) {
                        val playerName = result.getString("name")
                        logger.info("找到玩家: $playerName")
                    }
                }
            }
        }
    }
}
```

### Java兼容工厂函数
```kotlin
class JavaCompatiblePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用工厂函数创建数据库管理器
        val config = MySQLConfig(
            host = "localhost",
            port = 3306,
            database = "myserver",
            username = "minecraft",
            password = "secret123"
        )

        val databaseManager = DatabaseManager(this)
        databaseManager.initialize(config)
        bind(databaseManager)

        // 使用数据库
        runAsync {
            databaseManager.useConnection { connection ->
                // 数据库操作
            }
        }
    }
}
```

### 数据库操作方式

Database 模块支持两种数据库操作方式，开发者可以根据需求选择：

#### 方式一：ORM 操作（推荐）
使用 Ktorm 等现代 ORM 库，代码更简洁、类型安全：

```kotlin
// 定义表结构
object Players : Table<Nothing>("players") {
    val uuid = varchar("uuid").primaryKey()
    val name = varchar("name")
    val level = int("level")
}

override fun onPluginEnable() {
    val db = mysql { /* 配置 */ }

    // 创建 Ktorm Database 实例
    val database = Database.connect(
        dataSource = db.hikariDataSource!!,
        dialect = MySqlDialect()
    )

    tasks {
        runAsync {
            // 使用 Ktorm ORM 操作
            database.insert(Players) {
                set(it.uuid, player.uniqueId.toString())
                set(it.name, player.name)
                set(it.level, 1)
            }

            // 查询操作
            val playerData = database.from(Players)
                .select()
                .where { Players.uuid eq player.uniqueId.toString() }
                .map { row ->
                    PlayerData(
                        uuid = UUID.fromString(row[Players.uuid]!!),
                        name = row[Players.name]!!,
                        level = row[Players.level]!!
                    )
                }
                .firstOrNull()
        }
    }
}
```

#### 方式二：原生 SQL 操作
使用原生 SQL，提供最大的灵活性和控制：

```kotlin
override fun onPluginEnable() {
    val db = mysql { /* 配置 */ }

    tasks {
        runAsync {
            // 自动事务管理
            db.useTransaction { connection ->
                // 插入玩家数据
                val insertPlayer = connection.prepareStatement(
                    "INSERT INTO players (uuid, name, level) VALUES (?, ?, ?)"
                )
                insertPlayer.setString(1, player.uniqueId.toString())
                insertPlayer.setString(2, player.name)
                insertPlayer.setInt(3, 1)
                insertPlayer.executeUpdate()

                // 插入统计数据
                val insertStats = connection.prepareStatement(
                    "INSERT INTO player_stats (uuid, join_time) VALUES (?, ?)"
                )
                insertStats.setString(1, player.uniqueId.toString())
                insertStats.setTimestamp(2, Timestamp(System.currentTimeMillis()))
                insertStats.executeUpdate()

                // 如果任何操作失败，事务会自动回滚
            }
        }
    }
}
```

#### 选择建议

- **推荐使用 ORM（Ktorm）**：适合大多数常规 CRUD 操作，代码更简洁、类型安全
- **使用原生 SQL**：适合复杂查询、性能敏感场景、需要特定 SQL 功能的情况

### 批量操作

#### ORM 批量操作（推荐）
```kotlin
override fun onPluginEnable() {
    val db = mysql { /* 配置 */ }
    val database = Database.connect(db.hikariDataSource!!, MySqlDialect())

    tasks {
        runAsync {
            // Ktorm 批量插入
            database.batchInsert(PlayerActions) {
                repeat(1000) { i ->
                    item {
                        set(it.uuid, UUID.randomUUID().toString())
                        set(it.action, "action_$i")
                        set(it.timestamp, Timestamp(System.currentTimeMillis()))
                    }
                }
            }
            logger.info("批量插入了 1000 条记录")
        }
    }
}
```

#### 原生 SQL 批量操作
```kotlin
override fun onPluginEnable() {
    val db = mysql { /* 配置 */ }

    tasks {
        runAsync {
            // 原生 SQL 批量插入
            db.useBatch { batch ->
                val sql = "INSERT INTO player_actions (uuid, action, timestamp) VALUES (?, ?, ?)"

                // 添加多个批量操作
                repeat(1000) { i ->
                    batch.addBatch(sql) { statement ->
                        statement.setString(1, UUID.randomUUID().toString())
                        statement.setString(2, "action_$i")
                        statement.setTimestamp(3, Timestamp(System.currentTimeMillis()))
                    }
                }

                // 执行批量操作
                val results = batch.executeBatch()
                logger.info("批量插入了 ${results.sum()} 条记录")
            }
        }
    }
}
```

### 连接池监控
```kotlin
override fun onPluginEnable() {
    val db = mysql { /* 配置 */ }

    // 定期监控连接池状态
    runSyncRepeating(0L, 20L * 30) { // 每30秒检查一次
        val stats = db.getPoolStats()
        logger.info("""
            连接池状态:
            - 活跃连接: ${stats.activeConnections}
            - 空闲连接: ${stats.idleConnections}
            - 总连接数: ${stats.totalConnections}
            - 等待连接数: ${stats.threadsAwaitingConnection}
        """.trimIndent())

        // 健康检查
        if (db.isHealthy()) {
            logger.info("数据库连接正常")
        } else {
            logger.warning("数据库连接异常！")
        }
    }
}
```

## 🤝 贡献

如果您发现文档中的错误或有改进建议，欢迎提交Issue或Pull Request。

---

**开始您的Database开发之旅** → [📖 介绍](intro.md)
