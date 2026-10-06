# Persistence/CacheRuntime

## 定位

PersistenceRuntime 管数据源、transaction、repository executor、batch writer、flush lifecycle 和数据一致性边界。CacheRuntime 管本地缓存、TTL、容量、版本失效、negative cache、预热和统计。

这两者放在一份规格中，是因为 Minecraft 插件常把“内存缓存 + 配置文件/数据库写回 + 事件驱动更新”混在业务 Module 里。设计目标是让业务代码声明读写意图、刷新策略和缓存 policy，而不是手动维护 `Map`、定时 save、关闭时补救写盘。

## Interface

Persistence 核心概念：

- `DataSourceDescriptor`: 数据源声明。
- `DataSourceScope`: owner-bound 数据源生命周期。
- `TransactionScope`: transaction 边界。
- `RepositoryExecutor`: 执行 repository 操作的入口。
- `BatchWriter<T>`: 合并、排队、刷新和关闭 drain。
- `PersistenceOutcome<T>`: 成功、失败、取消、超时、拒绝、冲突。

Cache 核心概念：

- `CacheSpec<K, V>`: 缓存名称、owner、容量、TTL、loader、invalidation。
- `CacheHandle<K, V>`: get/put/invalidate/stats。
- `CacheVersion`: snapshot/report 驱动的失效版本。
- `CacheInvalidation`: key、tag、owner、version、global。

```kotlin
val profiles = ctx.cache.loading<PlayerId, Profile>("profiles") {
    owner(ctx.owner)
    maxSize(10_000)
    expireAfterWrite(10.minutes)
    negativeTtl(30.seconds)
    load { id -> ctx.persistence.read("load-profile") { repo.profiles.find(id) } }
}

val writer = ctx.persistence.batchWriter<PlayerProfileChange>("profile-writer") {
    owner(ctx.owner)
    flushEvery(5.seconds)
    maxBatchSize(100)
    onFlush { changes -> repo.profiles.applyChanges(changes) }
}
```

Interface 不变量：

- 数据源、transaction、writer、cache 都必须归属 owner。
- 阻塞 I/O 默认运行在 AsyncIo。
- transaction 内不能直接跨到 Bukkit main thread。
- batch writer 必须声明 flush policy。
- owner close 时 writer 必须按 policy drain 或取消，并输出 report。
- cache key/value policy 必须可诊断，不能无限增长作为默认。
- config/text/catalog reload 必须能触发明确的 cache invalidation。

## Persistence Flow

普通读写：

```text
operation declared
-> resolve data source
-> run on AsyncIo
-> open transaction if needed
-> execute repository action
-> map outcome
-> record duration and error
```

批量写：

```text
enqueue change
-> coalesce or append by key
-> flush by size/time/explicit request
-> execute transaction
-> retry only if operation is idempotent
-> emit BatchFlushReport
```

关闭顺序：

```text
owner closing
-> stop accepting writes
-> flush or cancel queued writes
-> close repository resources
-> close data source
-> emit close report
```

## Cache Flow

```text
get key
-> lookup local entry
-> check version and ttl
-> return hit or run loader
-> protect concurrent same-key load
-> store success or negative entry
-> update stats
```

失效入口：

- config version changed。
- text catalog version changed。
- external service etag changed。
- event indicates domain object changed。
- command/admin explicit invalidate。
- owner/module close。

## Policy

Persistence policy：

- `timeout`: 单次 repository operation 上限。
- `transactionMode`: read-only、read-write、none。
- `retry`: 只用于幂等操作。
- `writeCoalescing`: 按 key 合并写入。
- `flushOnClose`: drain、drop、fail-fast。
- `failureMode`: fail operation、queue for retry、report-only。

Cache policy：

- `maxSize`: 默认必须存在。
- `ttl`: write/access 过期。
- `negativeTtl`: miss/error 短暂缓存，防击穿。
- `refreshAhead`: 后台刷新。
- `versionBinding`: 绑定 config/text/domain version。
- `weight`: 按内存估算控制容量。
- `stats`: hit/miss/load/error/eviction。

## Implementation

Implementation 隐藏：

- Hikari/DataSource lifecycle。
- SQL transaction。
- ORM/query library。
- repository dispatcher。
- batch queue。
- flush timer。
- cache storage。
- single-flight loader。
- eviction。
- stats aggregation。
- close drain。

业务 Module 不直接把 `ConcurrentHashMap` 当长期缓存，也不在多个事件处理器里复制 save/flush 规则。

## Seam 和 Adapter

外部 Seam：

- `PersistenceRuntime` Interface -> Hikari/JDBC/Exposed/Ktorm/SQLite/file Implementation。
- `CacheRuntime` Interface -> Caffeine/internal cache Implementation。
- `BatchWriter` -> repository transaction。
- `CacheInvalidation` -> ConfigRuntime/EventRuntime/CommandRuntime。

Adapter：

- `HikariDataSourceAdapter`。
- `JdbcTransactionAdapter`。
- `ExposedRepositoryAdapter`。
- `FileSnapshotStoreAdapter`。
- `CaffeineCacheAdapter`。
- `TestPersistenceAdapter`。
- `TestCacheAdapter`。

Adapter 要求：

- 不把 driver/ORM 类型泄漏到业务 Interface。
- 不把 SQL exception 直接暴露给玩家反馈。
- cache Adapter 必须能导出 stats 和 entries summary。
- test Adapter 必须支持注入 timeout、deadlock、duplicate key、flush failure。

## 协作关系

- TaskRuntime: repository operation、flush、cache refresh 都是可诊断 task。
- EventRuntime: event handler 可以 enqueue write 或 invalidate cache。
- CommandRuntime: admin command 可以触发 flush、health、invalidate。
- ConfigRuntime: 数据源和 cache policy 来自 snapshot。
- NetworkRuntime: external response cache 和 persistence 写入统一统计。
- DiagnosticsRuntime: datasource health、writer queue、cache stats。

## Diagnostics

Persistence diagnostics：

- data source name。
- owner path。
- connection pool state。
- active transaction count。
- slow query summary。
- writer queue size。
- last flush duration。
- last flush outcome。
- pending drain count。

Cache diagnostics：

- cache name。
- owner path。
- size/maxSize。
- hit/miss/load/error/eviction。
- entry ttl summary。
- version binding。
- top hot keys 的安全摘要。

示例：

```text
cache profiles size=842/10000 hitRate=91.2% loadError=3
version=config:42 invalidations=17 owner=plugin:tpa/module:profiles
```

## Harness

`PersistenceHarness` 覆盖：

- operation 在 AsyncIo 执行。
- transaction success commit。
- transaction failure rollback。
- timeout 返回结构化 outcome。
- batch writer 按 size flush。
- batch writer 按 time flush。
- owner close drain 完成后关闭 datasource。
- duplicate key 或 conflict 映射成结构化错误。

`CacheHarness` 覆盖：

- hit/miss 统计。
- ttl 过期。
- negative cache 生效。
- single-flight 防同 key 击穿。
- config version changed 触发失效。
- owner close 清理 cache。
- maxSize eviction。
- loader failure 不污染长期 entry。

## 开放问题

1. PersistenceRuntime 是否作为 core 默认能力，还是拆成 optional runtime。
2. 默认 cache Implementation 使用 Caffeine 还是内部最小实现。
3. batch writer 关闭时默认 drain 超时时间是多少。
4. 是否为文件型数据提供统一 snapshot store。
