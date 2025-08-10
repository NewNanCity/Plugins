# Database 模块连接池配置

> 📋 **状态**: 文档规划中，内容正在完善

## 连接池概述

连接池是数据库性能优化的关键组件，通过复用数据库连接来减少连接建立和销毁的开销。

## HikariCP 配置 (推荐)

HikariCP 是目前性能最优的 Java 连接池：

```kotlin
data class ConnectionPoolConfig(
    // 连接池大小
    val maximumPoolSize: Int = 10,
    val minimumIdle: Int = 5,
    
    // 连接超时设置
    val connectionTimeout: Long = 30000, // 30秒
    val idleTimeout: Long = 600000,      // 10分钟
    val maxLifetime: Long = 1800000,     // 30分钟
    
    // 连接验证
    val connectionTestQuery: String = "SELECT 1",
    val validationTimeout: Long = 5000,
    
    // 连接池名称
    val poolName: String = "MinecraftDB-Pool"
)
```

## 连接池参数详解

### 核心参数

| 参数 | 说明 | 推荐值 | 注意事项 |
|------|------|--------|----------|
| maximumPoolSize | 最大连接数 | 10-20 | 根据服务器负载调整 |
| minimumIdle | 最小空闲连接 | 5-10 | 不超过最大连接数的一半 |
| connectionTimeout | 连接超时 | 30000ms | 避免过长等待 |
| idleTimeout | 空闲超时 | 600000ms | 平衡资源使用和性能 |
| maxLifetime | 连接最大生存时间 | 1800000ms | 防止连接过期 |

### 高级参数

```kotlin
data class AdvancedPoolConfig(
    // 连接泄漏检测
    val leakDetectionThreshold: Long = 60000, // 1分钟
    
    // 连接初始化SQL
    val connectionInitSql: String = "SET NAMES utf8mb4",
    
    // 只读连接
    val readOnly: Boolean = false,
    
    // 事务隔离级别
    val transactionIsolation: String = "TRANSACTION_READ_COMMITTED",
    
    // 自动提交
    val autoCommit: Boolean = true
)
```

## 性能调优

### 连接池大小计算

```kotlin
// 基于服务器规格的连接池大小计算
fun calculatePoolSize(
    cpuCores: Int,
    expectedConcurrentUsers: Int,
    averageQueryTime: Long
): Int {
    // 公式：CPU核心数 * 2 + 有效磁盘数
    val baseSize = cpuCores * 2 + 1
    
    // 根据并发用户数调整
    val concurrencyFactor = (expectedConcurrentUsers / 10).coerceAtMost(5)
    
    return (baseSize + concurrencyFactor).coerceAtMost(50)
}
```

### 监控指标

```kotlin
class ConnectionPoolMonitor(private val dataSource: HikariDataSource) {
    
    fun getPoolStats(): PoolStats {
        val poolMXBean = dataSource.hikariPoolMXBean
        
        return PoolStats(
            activeConnections = poolMXBean.activeConnections,
            idleConnections = poolMXBean.idleConnections,
            totalConnections = poolMXBean.totalConnections,
            threadsAwaitingConnection = poolMXBean.threadsAwaitingConnection
        )
    }
    
    fun logPoolStatus() {
        val stats = getPoolStats()
        logger.info("连接池状态 - 活跃: ${stats.activeConnections}, " +
                   "空闲: ${stats.idleConnections}, " +
                   "总计: ${stats.totalConnections}")
    }
}
```

## 连接池配置示例

### 开发环境配置

```yaml
database:
  connection-pool:
    maximum-pool-size: 5
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 300000
    max-lifetime: 900000
    pool-name: "Dev-Pool"
```

### 生产环境配置

```yaml
database:
  connection-pool:
    maximum-pool-size: 20
    minimum-idle: 10
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
    leak-detection-threshold: 60000
    pool-name: "Prod-Pool"
```

### 高负载环境配置

```yaml
database:
  connection-pool:
    maximum-pool-size: 50
    minimum-idle: 25
    connection-timeout: 20000
    idle-timeout: 300000
    max-lifetime: 1200000
    validation-timeout: 3000
    pool-name: "HighLoad-Pool"
```

## 故障排除

### 常见问题

1. **连接池耗尽**
   ```kotlin
   // 检查连接是否正确关闭
   try {
       connection.use { conn ->
           // 数据库操作
       }
   } catch (e: SQLException) {
       logger.error("数据库操作失败", e)
   }
   ```

2. **连接超时**
   ```kotlin
   // 增加连接超时时间或检查网络延迟
   connectionTimeout = 60000 // 增加到60秒
   ```

3. **连接泄漏**
   ```kotlin
   // 启用连接泄漏检测
   leakDetectionThreshold = 30000 // 30秒检测
   ```

### 性能优化建议

1. **批量操作**
   ```kotlin
   // 使用批量插入而不是单条插入
   connection.prepareStatement(sql).use { stmt ->
       for (item in items) {
           stmt.setString(1, item.name)
           stmt.addBatch()
       }
       stmt.executeBatch()
   }
   ```

2. **连接复用**
   ```kotlin
   // 在同一个事务中复用连接
   @Transactional
   fun performMultipleOperations() {
       // 多个数据库操作共享同一连接
   }
   ```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用方法
- [💡 基础概念](concepts.md) - 数据库系统概念
- [📊 性能监控](monitoring.md) - 监控和调优

---

**📝 注意**: 此文档正在完善中，更多配置选项请参考 [API 参考](api-reference.md)。
