package city.newnan.network

import city.newnan.core.base.BasePlugin
import city.newnan.core.terminable.terminable
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Job

/**
 * 网络DSL构建器
 *
 * 提供Kotlin DSL风格的网络请求API，基于工厂函数实现。
 * 这是推荐的网络请求方式，提供更简洁和类型安全的API。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class NetworkDSLBuilder(private val plugin: BasePlugin) {
    private var client: HttpClient? = null

    /**
     * 配置HTTP客户端
     */
    fun client(name: String = "default", block: HttpClientConfigBuilder.() -> Unit = {}): HttpClient {
        val builder = HttpClientConfigBuilder()
        builder.block()

        client = createHttpClient(name, builder.build())

        // 绑定到插件生命周期
        plugin.bind(terminable { client?.close() })

        return client!!
    }

    /**
     * GET请求
     */
    suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val httpClient = client ?: getHttpClient()
        return httpClient.get(url, block)
    }

    /**
     * POST请求
     */
    suspend fun post(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val httpClient = client ?: getHttpClient()
        return httpClient.post(url, block)
    }

    /**
     * PUT请求
     */
    suspend fun put(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val httpClient = client ?: getHttpClient()
        return httpClient.put(url, block)
    }

    /**
     * DELETE请求
     */
    suspend fun delete(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val httpClient = client ?: getHttpClient()
        return httpClient.delete(url, block)
    }

    /**
     * 批量请求
     */
    suspend fun batch(block: BatchRequestBuilder.() -> Unit): List<HttpResponse> {
        val builder = BatchRequestBuilder(client ?: getHttpClient())
        builder.block()
        return builder.execute()
    }

    /**
     * 网络监控（简化实现）
     */
    fun monitor(block: NetworkMonitorBuilder.() -> Unit): SimpleNetworkMonitor {
        val builder = NetworkMonitorBuilder()
        builder.block()

        val monitor = builder.build()

        // 绑定到插件生命周期
        plugin.bind(monitor)

        return monitor
    }

    /**
     * 延迟测量
     */
    suspend fun latency(url: String, attempts: Int = 3): NetworkLatency {
        val httpClient = client ?: getHttpClient()
        return measureLatency(httpClient, url, attempts)
    }
}

/**
 * HTTP客户端配置构建器
 */
class HttpClientConfigBuilder {
    private var userAgent: String = "NewNanPlugins/1.0"
    private val headers = mutableMapOf<String, String>()
    private var enableLogging: Boolean = false
    private var authConfig: AuthConfig? = null

    fun userAgent(userAgent: String) { this.userAgent = userAgent }
    fun header(key: String, value: String) { headers[key] = value }
    fun enableLogging(enabled: Boolean) { this.enableLogging = enabled }
    fun auth(authConfig: AuthConfig) { this.authConfig = authConfig }
    fun bearerAuth(token: String) { this.authConfig = AuthConfig.Bearer(token) }
    fun apiKeyAuth(key: String, headerName: String = "X-API-Key") {
        this.authConfig = AuthConfig.ApiKey(key, headerName)
    }

    internal fun build(): HttpClientConfig {
        return HttpClientConfig(
            userAgent = userAgent,
            defaultHeaders = headers.toMap(),
            enableLogging = enableLogging,
            authConfig = authConfig
        )
    }
}

/**
 * 批量请求构建器
 */
class BatchRequestBuilder(private val client: HttpClient) {
    private val requests = mutableListOf<suspend () -> HttpResponse>()

    fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}) {
        requests.add { client.get(url, block) }
    }

    fun post(url: String, block: HttpRequestBuilder.() -> Unit = {}) {
        requests.add { client.post(url, block) }
    }

    fun put(url: String, block: HttpRequestBuilder.() -> Unit = {}) {
        requests.add { client.put(url, block) }
    }

    fun delete(url: String, block: HttpRequestBuilder.() -> Unit = {}) {
        requests.add { client.delete(url, block) }
    }

    suspend fun execute(): List<HttpResponse> {
        return requests.map { it() }
    }
}

/**
 * 简单的网络监控器
 */
class SimpleNetworkMonitor(
    private val urls: List<String>,
    private val intervalSeconds: Long,
    private val onStatusChange: ((String, Boolean) -> Unit)?
) : AutoCloseable {

    override fun close() {
        // 简单实现，实际应该停止监控任务
    }
}

/**
 * 网络监控构建器
 */
class NetworkMonitorBuilder {
    private val urls = mutableListOf<String>()
    private var intervalSeconds: Long = 30
    private var onStatusChange: ((String, Boolean) -> Unit)? = null

    fun url(url: String) { urls.add(url) }
    fun urls(vararg urls: String) { this.urls.addAll(urls) }
    fun interval(seconds: Long) { this.intervalSeconds = seconds }
    fun onStatusChange(callback: (String, Boolean) -> Unit) { this.onStatusChange = callback }

    internal fun build(): SimpleNetworkMonitor {
        return SimpleNetworkMonitor(urls.toList(), intervalSeconds, onStatusChange)
    }
}

/**
 * BasePlugin的网络DSL扩展函数
 */
fun BasePlugin.network(block: NetworkDSLBuilder.() -> Unit): NetworkDSLBuilder {
    val builder = NetworkDSLBuilder(this)
    builder.block()
    return builder
}

/**
 * 简化的HTTP请求DSL
 */
suspend fun BasePlugin.httpGet(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    val builder = NetworkDSLBuilder(this)
    return builder.get(url, block)
}

suspend fun BasePlugin.httpPost(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    val builder = NetworkDSLBuilder(this)
    return builder.post(url, block)
}

suspend fun BasePlugin.httpPut(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    val builder = NetworkDSLBuilder(this)
    return builder.put(url, block)
}

suspend fun BasePlugin.httpDelete(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
    val builder = NetworkDSLBuilder(this)
    return builder.delete(url, block)
}

/**
 * 简化的网络监控DSL
 */
fun BasePlugin.networkMonitor(block: NetworkMonitorBuilder.() -> Unit): SimpleNetworkMonitor {
    val builder = NetworkDSLBuilder(this)
    return builder.monitor(block)
}

/**
 * 简化的延迟测量DSL
 */
suspend fun BasePlugin.networkLatency(url: String, attempts: Int = 3): NetworkLatency {
    val builder = NetworkDSLBuilder(this)
    return builder.latency(url, attempts)
}

// 实际的函数实现
private fun createHttpClient(name: String, config: HttpClientConfig): HttpClient {
    // 调用实际的HTTP客户端创建函数
    return getHttpClient(name)
}

private suspend fun measureLatency(client: HttpClient, url: String, attempts: Int): NetworkLatency {
    // 调用实际的延迟测量函数
    return NetworkUtils.measureLatency(client, url, attempts)
}
