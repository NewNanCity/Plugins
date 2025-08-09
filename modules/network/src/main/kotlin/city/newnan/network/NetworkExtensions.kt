package city.newnan.network

import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 网络扩展
 *
 * 提供简单的 HTTP 客户端管理功能
 * 注意：这个文件将在与 BasePlugin 集成时被替换
 */

private val httpClientManagers = ConcurrentHashMap<String, HttpClientManager>()

/**
 * 获取默认的 HTTP 客户端管理器
 */
fun getHttpClientManager(name: String = "default"): HttpClientManager {
    return httpClientManagers.computeIfAbsent(name) {
        HttpClientManager()
    }
}

/**
 * 获取默认的 HTTP 客户端
 */
fun getHttpClient(name: String = "default"): HttpClient {
    return getHttpClientManager(name).defaultClient
}



/**
 * 创建配置了 JSON 序列化的 HTTP 客户端
 */
fun createJsonHttpClient(name: String = "json"): HttpClient {
    return getHttpClientManager(name).getOrCreateClient(name) {
        serialization = SerializationType.KOTLINX_JSON
        prettyPrint = false
        ignoreUnknownKeys = true
    }
}

/**
 * 创建配置了认证的 HTTP 客户端
 */
fun createAuthenticatedHttpClient(
    name: String,
    auth: AuthConfig
): HttpClient {
    return getHttpClientManager(name).getOrCreateClient(name) {
        authConfig = auth
    }
}

/**
 * 创建配置了 API Key 认证的 HTTP 客户端
 */
fun createApiKeyHttpClient(
    name: String,
    apiKey: String,
    headerName: String = "X-API-Key"
): HttpClient {
    return createAuthenticatedHttpClient(name, AuthConfig.ApiKey(apiKey, headerName))
}

/**
 * 创建配置了 Bearer Token 认证的 HTTP 客户端
 */
fun createBearerHttpClient(
    name: String,
    token: String,
    refreshToken: String? = null
): HttpClient {
    return createAuthenticatedHttpClient(name, AuthConfig.Bearer(token, refreshToken))
}

/**
 * 创建配置了基础认证的 HTTP 客户端
 */
fun createBasicAuthHttpClient(
    name: String,
    username: String,
    password: String,
    realm: String? = null
): HttpClient {
    return createAuthenticatedHttpClient(name, AuthConfig.Basic(username, password, realm))
}



/**
 * 异步执行 HTTP 请求
 */
fun asyncHttpRequest(
    scope: CoroutineScope,
    block: suspend CoroutineScope.() -> Unit
) {
    scope.launch {
        try {
            block()
        } catch (e: Exception) {
            println("异步 HTTP 请求执行失败: ${e.message}")
        }
    }
}

/**
 * 检查网络连接性
 */
suspend fun checkNetworkConnectivity(
    host: String = "8.8.8.8",
    timeout: Long = 5000
): Boolean {
    return NetworkUtils.isNetworkAvailable(host, timeout)
}

/**
 * 检查 HTTP 服务可用性
 */
suspend fun checkHttpService(
    url: String,
    clientName: String = "default"
): Boolean {
    val client = getHttpClient(clientName)
    return NetworkUtils.isHttpServiceAvailable(client, url)
}

/**
 * 测量网络延迟
 */
suspend fun measureNetworkLatency(
    url: String,
    attempts: Int = 3,
    clientName: String = "default"
): NetworkLatency {
    val client = getHttpClient(clientName)
    return NetworkUtils.measureLatency(client, url, attempts)
}

/**
 * 下载文件到字节数组
 */
suspend fun downloadFile(
    url: String,
    maxSize: Long = 10 * 1024 * 1024, // 10MB
    clientName: String = "default"
): ByteArray {
    val client = getHttpClient(clientName)
    return NetworkUtils.downloadToByteArray(client, url, maxSize)
}

/**
 * 批量 HTTP 请求
 */
suspend fun <T> batchHttpRequests(
    requests: List<suspend HttpClient.() -> T>,
    clientName: String = "default"
): List<T> {
    val client = getHttpClient(clientName)
    return client.batchRequests(requests)
}

/**
 * 带重试的 HTTP 请求
 */
suspend fun <T> httpRequestWithRetry(
    maxRetries: Int = 3,
    delayMillis: Long = 1000,
    clientName: String = "default",
    request: suspend HttpClient.() -> T
): T {
    val client = getHttpClient(clientName)
    return client.requestWithRetry(maxRetries, delayMillis, request)
}

/**
 * 清理所有 HTTP 客户端
 */
fun closeAllHttpClients() {
    httpClientManagers.values.forEach { manager ->
        manager.closeAll()
    }
    httpClientManagers.clear()
}

/**
 * 清理指定的 HTTP 客户端管理器
 */
fun closeHttpClientManager(name: String) {
    httpClientManagers.remove(name)?.closeAll()
}
