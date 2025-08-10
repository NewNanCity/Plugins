# Database 模块 API 参考

> 📋 **状态**: 文档规划中，内容正在完善

## 核心 API

### DatabaseManager

数据库管理器是 Database 模块的核心 API：

```kotlin
interface DatabaseManager {
    // 连接管理
    fun getConnection(): Connection
    fun getDataSource(): DataSource
    
    // 实体操作
    fun <T> save(entity: T): T
    fun <T> findById(id: Any, entityClass: Class<T>): T?
    fun <T> findAll(entityClass: Class<T>): List<T>
    fun <T> delete(entity: T)
    
    // 查询操作
    fun <T> query(sql: String, entityClass: Class<T>, vararg params: Any): List<T>
    fun execute(sql: String, vararg params: Any): Int
    
    // 事务管理
    fun <T> transaction(block: (Connection) -> T): T
    fun beginTransaction(): Transaction
}
```

### Repository 接口

提供通用的数据访问模式：

```kotlin
interface Repository<T, ID> {
    fun save(entity: T): T
    fun saveAll(entities: Iterable<T>): List<T>
    
    fun findById(id: ID): T?
    fun findAll(): List<T>
    fun findAllById(ids: Iterable<ID>): List<T>
    
    fun existsById(id: ID): Boolean
    fun count(): Long
    
    fun delete(entity: T)
    fun deleteById(id: ID)
    fun deleteAll()
}
```

### 实体注解

用于实体类的 JPA 注解：

```kotlin
// 基本实体注解
@Entity
@Table(name = "players")
data class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "player_uuid", unique = true, nullable = false)
    val uuid: String,
    
    @Column(name = "player_name", length = 16)
    val name: String,
    
    @Column(name = "join_date")
    @Temporal(TemporalType.TIMESTAMP)
    val joinDate: Date = Date()
)
```

## 查询 API

### 原生 SQL 查询

```kotlin
class PlayerRepository(private val databaseManager: DatabaseManager) {
    
    fun findByName(name: String): Player? {
        return databaseManager.query(
            "SELECT * FROM players WHERE player_name = ?",
            Player::class.java,
            name
        ).firstOrNull()
    }
    
    fun findTopPlayers(limit: Int): List<Player> {
        return databaseManager.query(
            "SELECT * FROM players ORDER BY level DESC LIMIT ?",
            Player::class.java,
            limit
        )
    }
}
```

### 批量操作 API

```kotlin
interface BatchOperations {
    fun <T> batchSave(entities: List<T>): List<T>
    fun <T> batchUpdate(entities: List<T>): Int
    fun <T> batchDelete(entities: List<T>): Int
    
    fun executeBatch(sql: String, paramsList: List<Array<Any>>): IntArray
}
```

### 分页查询 API

```kotlin
data class PageRequest(
    val page: Int,
    val size: Int,
    val sort: Sort? = null
)

data class Page<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
)

interface PagingRepository<T, ID> : Repository<T, ID> {
    fun findAll(pageRequest: PageRequest): Page<T>
}
```

## 事务 API

### 声明式事务

```kotlin
@Transactional
class PlayerService(private val playerRepository: PlayerRepository) {
    
    @Transactional
    fun transferItems(fromPlayer: String, toPlayer: String, itemId: Long) {
        val from = playerRepository.findByName(fromPlayer)
            ?: throw PlayerNotFoundException(fromPlayer)
        val to = playerRepository.findByName(toPlayer)
            ?: throw PlayerNotFoundException(toPlayer)
            
        // 事务中的多个操作
        removeItemFromPlayer(from, itemId)
        addItemToPlayer(to, itemId)
    }
    
    @Transactional(readOnly = true)
    fun getPlayerStats(playerName: String): PlayerStats {
        // 只读事务
        return calculateStats(playerRepository.findByName(playerName))
    }
}
```

### 编程式事务

```kotlin
fun transferMoney(fromUuid: String, toUuid: String, amount: Double) {
    databaseManager.transaction { connection ->
        val fromBalance = getBalance(connection, fromUuid)
        if (fromBalance < amount) {
            throw InsufficientFundsException()
        }
        
        updateBalance(connection, fromUuid, fromBalance - amount)
        val toBalance = getBalance(connection, toUuid)
        updateBalance(connection, toUuid, toBalance + amount)
        
        // 记录交易日志
        insertTransaction(connection, fromUuid, toUuid, amount)
    }
}
```

## 连接池 API

### 连接池配置

```kotlin
data class ConnectionPoolConfig(
    val driverClassName: String,
    val jdbcUrl: String,
    val username: String,
    val password: String,
    
    // HikariCP 配置
    val maximumPoolSize: Int = 10,
    val minimumIdle: Int = 5,
    val connectionTimeout: Long = 30000,
    val idleTimeout: Long = 600000,
    val maxLifetime: Long = 1800000,
    
    // 连接验证
    val connectionTestQuery: String = "SELECT 1",
    val validationTimeout: Long = 5000
)
```

### 连接池监控

```kotlin
interface ConnectionPoolMonitor {
    fun getActiveConnections(): Int
    fun getIdleConnections(): Int
    fun getTotalConnections(): Int
    fun getThreadsAwaitingConnection(): Int
    
    fun getConnectionCreationRate(): Double
    fun getConnectionUsageRate(): Double
}
```

## 迁移 API

### 数据库迁移

```kotlin
interface DatabaseMigration {
    val version: String
    val description: String
    
    fun up(connection: Connection)
    fun down(connection: Connection)
}

class CreatePlayersTableMigration : DatabaseMigration {
    override val version = "001"
    override val description = "Create players table"
    
    override fun up(connection: Connection) {
        connection.createStatement().execute("""
            CREATE TABLE players (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) UNIQUE NOT NULL,
                player_name VARCHAR(16) NOT NULL,
                level INT DEFAULT 1,
                experience BIGINT DEFAULT 0,
                join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
    }
    
    override fun down(connection: Connection) {
        connection.createStatement().execute("DROP TABLE players")
    }
}
```

## 使用示例

### 基本 CRUD 操作

```kotlin
class PlayerService(private val databaseManager: DatabaseManager) {
    
    fun createPlayer(uuid: String, name: String): Player {
        val player = Player(uuid = uuid, name = name)
        return databaseManager.save(player)
    }
    
    fun getPlayer(uuid: String): Player? {
        return databaseManager.query(
            "SELECT * FROM players WHERE player_uuid = ?",
            Player::class.java,
            uuid
        ).firstOrNull()
    }
    
    fun updatePlayerLevel(uuid: String, level: Int) {
        databaseManager.execute(
            "UPDATE players SET level = ? WHERE player_uuid = ?",
            level, uuid
        )
    }
    
    fun deletePlayer(uuid: String) {
        databaseManager.execute(
            "DELETE FROM players WHERE player_uuid = ?",
            uuid
        )
    }
}
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用指南
- [💡 基础概念](concepts.md) - 核心概念说明
- [🔗 连接池配置](connection-pool.md) - 连接池优化

---

**📝 注意**: 此文档正在完善中，完整 API 说明请参考源码注释和 [示例代码](examples.md)。
