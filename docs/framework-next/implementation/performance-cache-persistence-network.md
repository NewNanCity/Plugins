# Performance, Cache, Persistence, And Network

## 是什么

这些能力都和运行成本有关。它们不一定都进 core，但都必须服从同一套 owner、policy、snapshot、report、diagnostics 模型。

## 性能原则

- 主线程 handler 默认目标 < 1ms。
- main/entity/region task warning 2ms，error 5ms，可配置。
- 文件、数据库、网络、大解析、大排序、大量 MiniMessage 解析不在主线程。
- 任何长期 executor、cache maintainer、batch writer 都必须属于 owner。
- 关闭后 active resource 为 0。

## CacheRuntime

### 相关工作

Caffeine 是生产 cache adapter 候选，支持 size/weight、time expiration、async loading、refresh。它适合作为 adapter，不应决定公共接口。

### 用法

```kotlin
val itemCache = cache<ShopItemId, ShopItem>("shop-items") {
    maximumSize = 10_000
    expireAfterAccess = 10.minutes
    invalidateOnConfigReload()
    loader { id -> shops.loadItem(id) }
}
```

### 内部方案

每个 cache 声明：

- owner。
- key/value type。
- maximum size/weight。
- ttl/refresh。
- invalidation policy。
- loader affinity。
- value mutability。
- diagnostics。

不长期缓存 Player、Entity、Inventory、可变 ItemStack 或跨线程 Bukkit 对象。

## PersistenceRuntime

### 相关工作

HikariCP 适合 JDBC pool，Exposed/Ktorm 适合 Kotlin SQL，但框架不应强制 ORM。PersistenceRuntime 只统一运行时约束：owner、transaction、batch、flush、backpressure、report。

### 用法

```kotlin
persistence.writeBehind("wealth-cache") {
    key(playerId)
    value(delta)
    merge(::mergeDelta)
    flush(::saveDelta)
    maxDelay = 3.seconds
    maxBatchSize = 100
}
```

### 内部方案

Write policy：

- `SyncCommit`: 关键交易，立即提交。
- `WriteThrough`: 写入并更新 cache。
- `WriteBehind`: 合并、延迟、批量 flush。
- `DerivedCache`: 可重建派生数据。

DB driver 由插件自己引入；`runtime-persistence` 是 optional module，不进入 core。

文件型数据可以提供 `FileSnapshotStore`：

- atomic write。
- version。
- checksum。
- backup。
- report。

## NetworkRuntime

### 相关工作

Ktor Client 适合 Kotlin HTTP，OkHttp engine 适合 JVM/Paper，Resilience4j 的 timeout/retry/rate limit/bulkhead/circuit breaker 说明 policy 可组合。

### 用法

```kotlin
val api = network.client("market-api") {
    baseUrl(config.api.baseUrl)
    timeout(config.api.timeout)
    retry(config.api.retry)
    rateLimit(config.api.rateLimit)
    redactHeaders("Authorization", "X-API-Key")
}

val profile = api.get<PlayerProfile>("/players/$playerId") {
    owner = currentOwner
}
```

### 内部方案

- Ktor Client 作为默认 implementation，JVM/Paper 默认 OkHttp engine。
- circuit breaker 不进 core 必需路径，作为 optional policy provider。
- 默认不记录 body，只记录 method、redacted URL、safe headers、content type、size。
- debug 且 allowlist content type 时最多记录 4KB redacted body preview。
- 外部服务默认不阻止 plugin enable；只有 `requiredAtStartup` 才 fail module。

## 测试

- cache ttl/max size/invalidation。
- cache owner close cleanup。
- batch writer merge/drain/timeout/failure。
- FileSnapshotStore atomic write and checksum。
- request timeout/retry/cancel/redaction。
- network owner close cancels request。

