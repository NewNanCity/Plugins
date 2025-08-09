# Database 模块介绍

## 🎯 什么是 Database 模块？

Database 模块是一个基于 HikariCP 的高性能数据库管理器，为 Minecraft 插件提供现代化、类型安全的数据库操作解决方案。它融合了 violet/sql 的优化配置，支持多种数据库，并与 BasePlugin 完美集成。

**5分钟快速了解：** Database 模块解决了传统数据库操作中连接管理困难、性能问题、事务处理复杂等痛点，通过 HikariCP 连接池、DSL 配置、事务管理和批量操作，让数据库操作变得简单而高效。

## 🔍 解决的问题

### 传统数据库操作的痛点

1. **连接管理困难** - 手动管理数据库连接，容易出现连接泄漏
2. **性能问题** - 频繁创建连接影响性能，缺乏连接池
3. **事务处理复杂** - 手动管理事务，容易出现数据不一致
4. **配置繁琐** - 数据库配置复杂，缺乏最佳实践
5. **监控困难** - 缺乏连接池状态监控和健康检查

### Database 模块的解决方案

✅ **HikariCP连接池** - 业界最快的连接池，自动管理连接生命周期
✅ **DSL配置API** - 流畅的Kotlin DSL风格配置，简化数据库设置
✅ **事务管理** - 内置事务支持，异常时自动回滚
✅ **批量操作** - 支持批量SQL执行，大幅提升性能
✅ **连接池监控** - 实时监控连接池状态和健康检查
✅ **MySQL优化** - 内置HikariCP官方推荐的MySQL性能优化配置

## 🆚 技术对比

### 与原生 JDBC 对比

| 特性     | 原生 JDBC     | Database 模块    |
| -------- | ------------- | ---------------- |
| 连接管理 | 手动管理      | HikariCP自动管理 |
| 配置方式 | 代码硬编码    | DSL配置          |
| 事务处理 | 手动try-catch | 自动事务管理     |
| 性能优化 | 需要手动优化  | 内置最佳实践     |
| 监控支持 | 无            | 完整监控         |
| 批量操作 | 复杂实现      | 简单API          |

### 与其他数据库框架对比

| 框架             | 优势                 | 劣势               |
| ---------------- | -------------------- | ------------------ |
| **Database模块** | 轻量级、高性能、易用 | 功能相对简单       |
| MyBatis          | 功能丰富、SQL映射    | 复杂度高、配置繁琐 |
| Hibernate        | ORM完整、自动映射    | 重量级、学习成本高 |
| JDBI             | 轻量级、SQL友好      | 缺乏连接池集成     |

## 🚀 快速示例

### 传统 JDBC 操作
```java
public class OldDatabaseManager {
    private Connection connection;

    public void connect() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/mydb";
        connection = DriverManager.getConnection(url, "user", "pass");
    }

    public void insertPlayer(UUID uuid, String name) throws SQLException {
        // 手动事务管理
        connection.setAutoCommit(false);
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO players (uuid, name) VALUES (?, ?)"
            );
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
```

### Database 模块操作
```kotlin
class ModernDatabaseManager : BasePlugin() {
    override fun onPluginEnable() {
        // 简单的DSL配置
        val db = mysql {
            host = "localhost"
            port = 3306
            database = "mydb"
            username = "user"
            password = "pass"

            // 连接池配置
            pool {
                maximumPoolSize = 10
                minimumIdle = 2
            }
        }

        // 使用异步调度器进行数据库操作
        runAsync {
            try {
                // 自动事务管理
                db.useTransaction { connection ->
                    val stmt = connection.prepareStatement(
                        "INSERT INTO players (uuid, name) VALUES (?, ?)"
                    )
                    stmt.setString(1, player.uniqueId.toString())
                    stmt.setString(2, player.name)
                    stmt.executeUpdate()

                    // 如果出现异常，事务自动回滚
                    // 无需手动管理
                }

                // 在主线程通知操作完成
                runSync {
                    logger.info("玩家数据保存成功")
                }
            } catch (e: Exception) {
                runSync {
                    logger.error("数据库操作失败", e)
                }
            }
        }

        // 连接池自动管理，插件禁用时自动关闭
    }
}
```

## 🏗️ 核心架构

### 1. HikariCP 连接池
- **高性能** - 业界最快的连接池实现
- **自动管理** - 连接创建、复用、销毁全自动
- **配置优化** - 内置MySQL等数据库的最佳实践配置
- **监控支持** - 完整的连接池状态监控

### 2. DSL 配置系统
- **类型安全** - Kotlin DSL提供编译时类型检查
- **流畅API** - 链式调用，配置简洁明了
- **infix函数** - 支持自然语言风格的配置
- **Java兼容** - 提供Java兼容的工厂函数

### 3. 事务管理
- **自动事务** - useTransaction自动开启和提交事务
- **异常回滚** - 异常时自动回滚，保证数据一致性
- **嵌套支持** - 支持嵌套事务和保存点
- **隔离级别** - 可配置的事务隔离级别

### 4. 批量操作
- **高性能** - 批量SQL执行，减少网络往返
- **简单API** - useBatch提供简洁的批量操作接口
- **内存优化** - 分批处理大量数据，避免内存溢出
- **错误处理** - 批量操作的错误处理和部分成功支持

## 📊 支持的数据库

### 生产级数据库
```kotlin
// MySQL - 最流行的开源关系型数据库
val mysql = mysql {
    host = "localhost"
    port = 3306
    database = "myserver"
    username = "minecraft"
    password = "secret123"
}

// PostgreSQL - 功能强大的开源对象关系型数据库
val postgresql = postgresql {
    host = "localhost"
    port = 5432
    database = "myserver"
    username = "postgres"
    password = "secret123"
}
```

### 轻量级数据库
```kotlin
// SQLite - 嵌入式数据库，适合小型应用
val sqlite = sqlite {
    file = "data/database.db"
    enableWAL = true // 启用WAL模式提升性能
}

// H2 - 内存/文件数据库，适合测试和开发
val h2 = h2 {
    mode = H2Mode.FILE
    file = "data/h2database"
    enableMVCC = true
}
```

## 📈 性能优势

### 连接池性能
- **零开销** - HikariCP的零开销设计
- **快速连接** - 平均连接获取时间 < 1ms
- **智能调度** - 自适应的连接池大小调整
- **内存优化** - 最小的内存占用

### 批量操作性能
```kotlin
// 传统方式：1000次单独插入 ≈ 10秒
repeat(1000) { i ->
    connection.prepareStatement("INSERT INTO data VALUES (?)").use { stmt ->
        stmt.setInt(1, i)
        stmt.executeUpdate()
    }
}

// Database模块：1000条批量插入 ≈ 0.1秒
db.useBatch { batch ->
    repeat(1000) { i ->
        batch.addBatch("INSERT INTO data VALUES (?)") { stmt ->
            stmt.setInt(1, i)
        }
    }
    batch.executeBatch()
}
```

## 🎯 适用场景

### ✅ 推荐使用
- 需要数据库操作的Minecraft插件
- 高并发的数据读写需求
- 需要事务保证的业务逻辑
- 大量数据的批量处理
- 需要连接池监控的生产环境

### ⚠️ 考虑因素
- 学习HikariCP配置的成本
- 数据库驱动的依赖大小
- 团队对数据库操作的熟悉度

## 🔄 迁移路径

### 从原生 JDBC 迁移
1. **添加依赖** - 引入Database模块和数据库驱动
2. **配置连接池** - 使用DSL替换手动连接管理
3. **重构事务** - 使用useTransaction替换手动事务
4. **优化批量操作** - 使用useBatch提升性能

### 从其他ORM迁移
1. **分析现有查询** - 了解当前的数据访问模式
2. **逐步替换** - 从简单查询开始逐步迁移
3. **性能测试** - 对比迁移前后的性能差异
4. **监控调优** - 使用连接池监控优化配置

## 🚀 异步数据库操作最佳实践

### 1. 基础异步操作
```kotlin
class PlayerDataManager : BasePlugin() {

    private lateinit var database: MySQLDatabase

    override fun onPluginEnable() {
        database = mysql {
            host = "localhost"
            database = "playerdata"
            username = "root"
            password = "password"
        }

        // 异步加载玩家数据
        loadPlayerDataAsync(player)
    }

    private fun loadPlayerDataAsync(player: Player) {
        runAsync {
            try {
                // 数据库查询操作
                val playerData = database.useConnection { conn ->
                    val stmt = conn.prepareStatement(
                        "SELECT * FROM players WHERE uuid = ?"
                    )
                    stmt.setString(1, player.uniqueId.toString())
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        PlayerData(
                            uuid = rs.getString("uuid"),
                            name = rs.getString("name"),
                            level = rs.getInt("level"),
                            exp = rs.getInt("exp")
                        )
                    } else null
                }

                // 回到主线程处理结果
                runSync {
                    if (playerData != null) {
                        applyPlayerData(player, playerData)
                        player.sendMessage("欢迎回来，${playerData.name}！")
                    } else {
                        createNewPlayerData(player)
                    }
                }
            } catch (e: Exception) {
                runSync {
                    logger.error("加载玩家数据失败: ${player.name}", e)
                    player.sendMessage("加载数据失败，请联系管理员")
                }
            }
        }
    }
}
```

### 2. 批量操作优化
```kotlin
private fun saveBatchPlayerStats() {
    runAsync {
        try {
            val playerStats = getAllOnlinePlayerStats()

            database.useBatch { batch ->
                val sql = "INSERT INTO player_stats (uuid, stat_type, value, timestamp) VALUES (?, ?, ?, ?)"

                playerStats.forEach { stat ->
                    batch.addBatch(sql) { stmt ->
                        stmt.setString(1, stat.uuid)
                        stmt.setString(2, stat.type)
                        stmt.setInt(3, stat.value)
                        stmt.setLong(4, System.currentTimeMillis())
                    }
                }
            }

            runSync {
                logger.info("批量保存玩家统计完成: ${playerStats.size} 条记录")
            }
        } catch (e: Exception) {
            runSync {
                logger.error("批量保存失败", e)
            }
        }
    }
}
```

### 3. 定期数据同步
```kotlin
private fun scheduleDataBackup() {
    // 每小时备份一次玩家数据
    runSyncRepeating(0L, 72000L) { task ->
        runAsync {
            try {
                backupPlayerData()
                runSync {
                    logger.info("定期数据备份完成")
                }
            } catch (e: Exception) {
                runSync {
                    logger.error("定期备份失败", e)
                }
            }
        }
    }
}

private fun backupPlayerData() {
    database.useTransaction { conn ->
        // 备份玩家数据到备份表
        val stmt = conn.prepareStatement("""
            INSERT INTO player_backup
            SELECT *, NOW() as backup_time FROM players
        """)
        stmt.executeUpdate()

        // 清理旧备份（保留最近7天）
        val cleanupStmt = conn.prepareStatement("""
            DELETE FROM player_backup
            WHERE backup_time < DATE_SUB(NOW(), INTERVAL 7 DAY)
        """)
        cleanupStmt.executeUpdate()
    }
}
```

### 数据库 + 网络集成示例
参考网络模块文档中的 [数据库集成指导](../network/intro.md#🗄️-数据库集成指导) 部分，了解如何将数据库操作与网络请求结合使用。

---

**准备开始？** → [🚀 快速开始](README.md)
