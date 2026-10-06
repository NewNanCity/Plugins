# NetworkRuntime

## 定位

NetworkRuntime 管命名 HTTP client、外部服务 policy、request lifecycle、retry、rate limit、bulkhead、streaming、credential redaction、health check 和 diagnostics。

它不是对 Ktor/OkHttp 的薄封装。Minecraft 插件里的网络请求经常由 command、GUI、event 或周期任务触发，如果没有 owner、timeout、背压和可观察性，就会变成关闭时悬挂、主线程卡顿、错误重试风暴和日志泄漏。

## Interface

核心概念：

- `NamedHttpClient`: 按用途命名的外部服务 client。
- `NetworkPolicy`: timeout、retry、rate limit、bulkhead、payload limit、redaction。
- `RequestSpec<T>`: method、url、headers、body、decoder、owner、policy。
- `RequestHandle<T>`: 可取消、可等待、可诊断的 request 引用。
- `NetworkOutcome<T>`: 成功、失败、取消、超时、限流、熔断、解码失败。
- `ServiceHealthSnapshot`: 外部服务健康状态。

```kotlin
val github = ctx.network.client("github") {
    baseUrl("https://api.github.com")
    timeout(3.seconds)
    rateLimit(10.perSecond())
    retry(RetryPolicy.exponential(maxAttempts = 3))
    redactHeader("Authorization")
}

val result = github.get<UserProfile>("/users/{name}") {
    owner(ctx.owner)
    path("name", playerName)
    cacheKey("github-user:$playerName")
    decodeAsJson<UserProfile>()
}
```

Interface 不变量：

- 每个 request 必须有 owner。
- 每个 client 必须有名称和 policy。
- timeout 必须显式存在，不能无限等待。
- retry 只允许在声明为幂等的 request 上启用。
- request body、header、query 中的 secret 必须经过 redaction 后才能进入日志和 diagnostics。
- 网络回调不直接触碰 Bukkit mutable state，必须经 TaskRuntime 切回正确 affinity。
- owner close 后，未完成 request 必须取消或按 policy drain。

## Policy

`NetworkPolicy` 建议包含：

```kotlin
data class NetworkPolicy(
    val requestTimeout: Duration,
    val connectTimeout: Duration,
    val retry: RetryPolicy,
    val rateLimit: RateLimitPolicy,
    val bulkhead: BulkheadPolicy,
    val circuitBreaker: CircuitBreakerPolicy?,
    val payloadLimit: PayloadLimit,
    val redaction: RedactionPolicy,
    val failureMode: NetworkFailureMode
)
```

关键 policy：

- `requestTimeout`: 单次请求总耗时上限。
- `connectTimeout`: 建连上限，防止卡在连接阶段。
- `retry`: 幂等请求的失败重试和 backoff。
- `rateLimit`: 面向外部服务的速率控制。
- `bulkhead`: 单个 client 最大并发，避免拖垮全局 I/O。
- `circuitBreaker`: 外部服务异常时快速失败。
- `payloadLimit`: 下载和响应大小上限。
- `redaction`: header、query、body path 的脱敏规则。

## Request Flow

```text
request declared
-> resolve named client
-> attach owner and correlation id
-> apply rate limit and bulkhead
-> execute on AsyncIo
-> decode response
-> map to NetworkOutcome
-> update service health
-> emit request diagnostics
```

错误策略：

- DNS/connect/read timeout 返回结构化 outcome。
- HTTP 4xx/5xx 是否视为 failure 由 decoder/policy 决定。
- decoder failure 必须保留 status、content type 和安全摘要。
- circuit open 时返回 rejected，不进入真实请求。
- owner close 时按 `CancellationReason.OwnerClosed` 记录。

## Streaming 和下载

大文件和流式响应不能进入普通 `ByteArray` 路径：

```kotlin
ctx.network.download("resource-pack") {
    owner(ctx.owner)
    url(packUrl)
    maxSize(64.megabytes)
    checksum(sha256)
    destination(cacheDir.resolve("pack.zip"))
    progress { bytesRead, total -> ctx.diagnostics.progress("pack-download", bytesRead, total) }
}
```

约束：

- 必须声明最大大小。
- 必须支持取消。
- 目标文件写入应使用临时文件加原子替换。
- checksum/etag/last-modified 可以驱动 CacheRuntime。
- progress event 不能高频刷屏，应做节流。

## Implementation

Implementation 隐藏：

- Ktor/OkHttp client lifecycle。
- coroutine dispatcher。
- retry/backoff timer。
- rate limiter。
- bulkhead semaphore。
- circuit state。
- response decode。
- redaction。
- request metrics。
- service health aggregation。

业务 Module 不直接保存裸 `HttpClient`，也不直接把 bearer token、cookie 或 request body 写入日志。

## Seam 和 Adapter

外部 Seam：

- `NetworkRuntime` Interface -> Ktor/OkHttp/Java HTTP Implementation。
- `NamedHttpClient` -> external service provider。
- `NetworkOutcome` -> CommandRuntime/TextRuntime 用户反馈。
- `ServiceHealthSnapshot` -> DiagnosticsRuntime。

Adapter：

- `KtorNetworkAdapter`: 复用现有 network module 经验。
- `OkHttpNetworkAdapter`: 适合高可控连接池和拦截器场景。
- `ResilienceAdapter`: retry、rate limit、bulkhead、circuit policy。
- `TestNetworkAdapter`: deterministic fake response、timeout、stream。

Adapter 要求：

- 不改变 `NetworkOutcome` 语义。
- 不把所有错误折叠成 `Exception`。
- 不在 Adapter 内静默无限重试。
- 默认关闭时取消未完成 request，除非 request 明确声明 drain。

## 协作关系

- TaskRuntime: 网络请求运行在 AsyncIo，完成后按需要切回 Main/Entity/Region。
- ConfigRuntime: client policy、endpoint、feature flag 来自 snapshot。
- CacheRuntime: response cache、etag、negative cache 和失效。
- TextRuntime: request failure 渲染给 sender/player。
- DiagnosticsRuntime: request trace、client health、redaction audit。
- IntegrationRuntime: 外部服务作为 capability 暴露给业务 Module。

## Diagnostics

Network diagnostics 应包含：

- client name。
- owner path。
- method。
- redacted url。
- timeout。
- retry attempt。
- queue wait。
- execution duration。
- status code。
- payload size。
- outcome。
- circuit state。
- rate limit state。
- last error summary。

示例：

```text
network github GET /users/{name}
owner=plugin:tpa/module:profile outcome=timedOut timeout=3s attempts=3
queueWait=12ms duration=3010ms circuit=closed
suggestion=increase timeout only if provider p95 proves it is needed
```

## Harness

`NetworkHarness` 需要覆盖：

- named client policy 生效。
- timeout 返回 `NetworkOutcome.TimedOut`。
- owner close 取消 request。
- retry 只作用于幂等 request。
- rate limit 拒绝或排队符合 policy。
- circuit open 后快速失败。
- redaction 不泄漏 token。
- decoder failure 保留安全摘要。
- streaming download 超过 max size 会停止。
- response cache 和 etag 与 CacheRuntime 协作。

## 开放问题

1. 初版是否直接以 Ktor 为默认 Implementation。
2. circuit breaker 是否放在 core，还是作为 optional policy provider。
3. request body diagnostics 默认保留多大安全摘要。
4. 外部服务健康状态是否参与插件 enable 判定。
