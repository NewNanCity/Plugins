# Database 模块示例代码

> 📋 **状态**: 文档规划中，内容正在完善

## 基础示例

### 简单实体定义

```kotlin
// 玩家实体
@Entity
@Table(name = "players")
data class Player(
    @Id
    val uuid: String,
    
    @Column(name = "display_name")
    val displayName: String,
    
    val level: Int = 1,
    val experience: Long = 0,
    
    @Column(name = "join_date")
    val joinDate: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "last_login")
    val lastLogin: LocalDateTime? = null
)

// 物品实体
@Entity
@Table(name = "player_items")
data class PlayerItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "player_uuid")
    val playerUuid: String,
    
    @Column(name = "item_type")
    val itemType: String,
    
    val amount: Int,
    
    @Column(name = "custom_data", columnDefinition = "TEXT")
    val customData: String? = null
)
```

### 基本数据库操作

```kotlin
class PlayerService(private val databaseManager: DatabaseManager) {
    
    // 创建玩家
    fun createPlayer(uuid: String, name: String): Player {
        val player = Player(
            uuid = uuid,
            displayName = name,
            joinDate = LocalDateTime.now()
        )
        return databaseManager.save(player)
    }
    
    // 查找玩家
    fun findPlayer(uuid: String): Player? {
        return databaseManager.findById(uuid, Player::class.java)
    }
    
    // 更新玩家等级
    fun updatePlayerLevel(uuid: String, level: Int) {
        val player = findPlayer(uuid) ?: return
        val updatedPlayer = player.copy(level = level)
        databaseManager.save(updatedPlayer)
    }
    
    // 获取排行榜
    fun getTopPlayers(limit: Int): List<Player> {
        return databaseManager.query(
            "SELECT * FROM players ORDER BY level DESC, experience DESC LIMIT ?",
            Player::class.java,
            limit
        )
    }
}
```

## 高级示例

### Repository 模式实现

```kotlin
// 玩家仓库接口
interface PlayerRepository : Repository<Player, String> {
    fun findByDisplayName(name: String): Player?
    fun findByLevelGreaterThan(level: Int): List<Player>
    fun findActivePlayersAfter(date: LocalDateTime): List<Player>
}

// 玩家仓库实现
class PlayerRepositoryImpl(
    private val databaseManager: DatabaseManager
) : PlayerRepository {
    
    override fun save(entity: Player): Player {
        return databaseManager.save(entity)
    }
    
    override fun findById(id: String): Player? {
        return databaseManager.findById(id, Player::class.java)
    }
    
    override fun findAll(): List<Player> {
        return databaseManager.query(
            "SELECT * FROM players",
            Player::class.java
        )
    }
    
    override fun findByDisplayName(name: String): Player? {
        return databaseManager.query(
            "SELECT * FROM players WHERE display_name = ?",
            Player::class.java,
            name
        ).firstOrNull()
    }
    
    override fun findByLevelGreaterThan(level: Int): List<Player> {
        return databaseManager.query(
            "SELECT * FROM players WHERE level > ? ORDER BY level DESC",
            Player::class.java,
            level
        )
    }
    
    override fun findActivePlayersAfter(date: LocalDateTime): List<Player> {
        return databaseManager.query(
            "SELECT * FROM players WHERE last_login > ?",
            Player::class.java,
            Timestamp.valueOf(date)
        )
    }
    
    override fun delete(entity: Player) {
        databaseManager.execute(
            "DELETE FROM players WHERE uuid = ?",
            entity.uuid
        )
    }
    
    override fun count(): Long {
        return databaseManager.query(
            "SELECT COUNT(*) as count FROM players",
            CountResult::class.java
        ).first().count
    }
}

data class CountResult(val count: Long)
```

### 事务管理示例

```kotlin
class EconomyService(
    private val databaseManager: DatabaseManager,
    private val playerRepository: PlayerRepository
) {
    
    // 转账操作（事务）
    @Transactional
    fun transferMoney(fromUuid: String, toUuid: String, amount: Double) {
        val fromPlayer = playerRepository.findById(fromUuid)
            ?: throw PlayerNotFoundException("发送方玩家不存在: $fromUuid")
        val toPlayer = playerRepository.findById(toUuid)
            ?: throw PlayerNotFoundException("接收方玩家不存在: $toUuid")
        
        // 检查余额
        val fromBalance = getPlayerBalance(fromUuid)
        if (fromBalance < amount) {
            throw InsufficientFundsException("余额不足")
        }
        
        // 执行转账
        updatePlayerBalance(fromUuid, fromBalance - amount)
        val toBalance = getPlayerBalance(toUuid)
        updatePlayerBalance(toUuid, toBalance + amount)
        
        // 记录交易
        recordTransaction(fromUuid, toUuid, amount)
        
        logger.info("转账成功: $fromUuid -> $toUuid, 金额: $amount")
    }
    
    // 批量发放奖励
    @Transactional
    fun distributeRewards(rewards: Map<String, Double>) {
        databaseManager.transaction { connection ->
            val updateStmt = connection.prepareStatement(
                "UPDATE player_economy SET balance = balance + ? WHERE player_uuid = ?"
            )
            
            for ((uuid, amount) in rewards) {
                updateStmt.setDouble(1, amount)
                updateStmt.setString(2, uuid)
                updateStmt.addBatch()
            }
            
            updateStmt.executeBatch()
            logger.info("批量发放奖励完成，共 ${rewards.size} 个玩家")
        }
    }
    
    private fun getPlayerBalance(uuid: String): Double {
        return databaseManager.query(
            "SELECT balance FROM player_economy WHERE player_uuid = ?",
            BalanceResult::class.java,
            uuid
        ).firstOrNull()?.balance ?: 0.0
    }
    
    private fun updatePlayerBalance(uuid: String, balance: Double) {
        databaseManager.execute(
            "UPDATE player_economy SET balance = ? WHERE player_uuid = ?",
            balance, uuid
        )
    }
    
    private fun recordTransaction(from: String, to: String, amount: Double) {
        databaseManager.execute(
            """INSERT INTO transactions (from_uuid, to_uuid, amount, transaction_time) 
               VALUES (?, ?, ?, ?)""",
            from, to, amount, Timestamp.from(Instant.now())
        )
    }
}

data class BalanceResult(val balance: Double)
```

### 分页查询示例

```kotlin
class PlayerListService(private val databaseManager: DatabaseManager) {
    
    fun getPlayersPage(page: Int, size: Int, sortBy: String = "level"): Page<Player> {
        val offset = page * size
        
        // 查询总数
        val totalCount = databaseManager.query(
            "SELECT COUNT(*) as count FROM players",
            CountResult::class.java
        ).first().count
        
        // 查询当前页数据
        val players = databaseManager.query(
            "SELECT * FROM players ORDER BY $sortBy DESC LIMIT ? OFFSET ?",
            Player::class.java,
            size, offset
        )
        
        return Page(
            content = players,
            totalElements = totalCount,
            totalPages = ((totalCount + size - 1) / size).toInt(),
            number = page,
            size = size
        )
    }
    
    fun searchPlayers(keyword: String, page: Int, size: Int): Page<Player> {
        val offset = page * size
        val searchPattern = "%$keyword%"
        
        val totalCount = databaseManager.query(
            "SELECT COUNT(*) as count FROM players WHERE display_name LIKE ?",
            CountResult::class.java,
            searchPattern
        ).first().count
        
        val players = databaseManager.query(
            """SELECT * FROM players 
               WHERE display_name LIKE ? 
               ORDER BY display_name 
               LIMIT ? OFFSET ?""",
            Player::class.java,
            searchPattern, size, offset
        )
        
        return Page(
            content = players,
            totalElements = totalCount,
            totalPages = ((totalCount + size - 1) / size).toInt(),
            number = page,
            size = size
        )
    }
}
```

### 数据库迁移示例

```kotlin
// 迁移管理器
class MigrationManager(private val databaseManager: DatabaseManager) {
    
    private val migrations = listOf(
        CreatePlayersTableMigration(),
        CreateEconomyTableMigration(),
        AddPlayerIndexesMigration(),
        UpdatePlayerSchemaV2Migration()
    )
    
    fun runMigrations() {
        // 创建迁移记录表
        createMigrationTable()
        
        val appliedMigrations = getAppliedMigrations()
        
        for (migration in migrations) {
            if (migration.version !in appliedMigrations) {
                logger.info("执行迁移: ${migration.version} - ${migration.description}")
                
                try {
                    databaseManager.transaction { connection ->
                        migration.up(connection)
                        recordMigration(migration)
                    }
                    logger.info("迁移完成: ${migration.version}")
                } catch (e: Exception) {
                    logger.error("迁移失败: ${migration.version}", e)
                    throw e
                }
            }
        }
    }
    
    private fun createMigrationTable() {
        databaseManager.execute("""
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version VARCHAR(255) PRIMARY KEY,
                description TEXT,
                applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
    }
    
    private fun getAppliedMigrations(): Set<String> {
        return databaseManager.query(
            "SELECT version FROM schema_migrations",
            MigrationRecord::class.java
        ).map { it.version }.toSet()
    }
    
    private fun recordMigration(migration: DatabaseMigration) {
        databaseManager.execute(
            "INSERT INTO schema_migrations (version, description) VALUES (?, ?)",
            migration.version, migration.description
        )
    }
}

data class MigrationRecord(val version: String)

// 具体迁移实现
class CreatePlayersTableMigration : DatabaseMigration {
    override val version = "001"
    override val description = "创建玩家表"
    
    override fun up(connection: Connection) {
        connection.createStatement().execute("""
            CREATE TABLE players (
                uuid VARCHAR(36) PRIMARY KEY,
                display_name VARCHAR(16) NOT NULL,
                level INT DEFAULT 1,
                experience BIGINT DEFAULT 0,
                join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP NULL,
                INDEX idx_level (level),
                INDEX idx_display_name (display_name)
            )
        """)
    }
    
    override fun down(connection: Connection) {
        connection.createStatement().execute("DROP TABLE players")
    }
}
```

## 实际应用示例

### 完整的插件集成

```kotlin
class DatabasePlugin : BasePlugin() {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var playerService: PlayerService
    private lateinit var economyService: EconomyService
    
    override fun onPluginEnable() {
        // 初始化数据库
        initializeDatabase()
        
        // 运行迁移
        runMigrations()
        
        // 初始化服务
        initializeServices()
        
        super.onPluginEnable()
    }
    
    private fun initializeDatabase() {
        val config = configManager.getPluginConfig<DatabasePluginConfig>()
        databaseManager = DatabaseManager(this, config.database)
    }
    
    private fun runMigrations() {
        val migrationManager = MigrationManager(databaseManager)
        migrationManager.runMigrations()
    }
    
    private fun initializeServices() {
        val playerRepository = PlayerRepositoryImpl(databaseManager)
        playerService = PlayerService(databaseManager)
        economyService = EconomyService(databaseManager, playerRepository)
    }
    
    override fun onPluginDisable() {
        // 关闭数据库连接
        databaseManager.close()
        super.onPluginDisable()
    }
}

data class DatabasePluginConfig(
    val database: DatabaseConfig = DatabaseConfig()
) : BasePluginConfig()
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用方法
- [💡 基础概念](concepts.md) - 数据库系统概念
- [📋 API 参考](api-reference.md) - 完整 API 文档
- [🔗 连接池配置](connection-pool.md) - 性能优化

---

**📝 注意**: 此文档正在完善中，更多示例请参考项目源码中的测试用例。
