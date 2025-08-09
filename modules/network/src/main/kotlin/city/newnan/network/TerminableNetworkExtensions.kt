package city.newnan.network

import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 可终止的网络扩展
 * 
 * 提供与 TerminableConsumer 集成的网络功能
 */

/**
 * 可终止的 HTTP 客户端包装器
 */
class TerminableHttpClientWrapper(
    private val client: HttpClient
) : Terminable {
    
    private val closed = AtomicBoolean(false)
    private val activeJobs = mutableSetOf<Job>()
    
    /**
     * 执行可终止的 HTTP 请求
     */
    fun <T> executeRequest(
        scope: CoroutineScope,
        request: suspend () -> T,
        onResult: (Result<T>) -> Unit = {}
    ): Job {
        if (closed.get()) {
            onResult(Result.failure(IllegalStateException("HTTP 客户端已关闭")))
            return Job().apply { cancel() }
        }
        
        val job = scope.launch {
            try {
                val result = request()
                if (!closed.get()) {
                    onResult(Result.success(result))
                }
            } catch (e: Exception) {
                if (!closed.get()) {
                    onResult(Result.failure(e))
                }
            }
        }
        
        synchronized(activeJobs) {
            activeJobs.add(job)
        }
        
        job.invokeOnCompletion {
            synchronized(activeJobs) {
                activeJobs.remove(job)
            }
        }
        
        return job
    }
    
    /**
     * 获取底层客户端
     */
    fun getClient(): HttpClient = client
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            synchronized(activeJobs) {
                activeJobs.forEach { job ->
                    job.cancel()
                }
                activeJobs.clear()
            }
            
            try {
                client.close()
            } catch (e: Exception) {
                println("关闭 HTTP 客户端时出错: ${e.message}")
            }
        }
    }
    
    override fun isClosed(): Boolean = closed.get()
}

/**
 * 为 TerminableConsumer 绑定 HTTP 客户端管理器
 */
fun TerminableConsumer.bindHttpClientManager(name: String = "default"): HttpClientManager {
    val manager = getHttpClientManager(name)
    bind(manager)
    return manager
}

/**
 * 为 TerminableConsumer 创建可终止的 HTTP 客户端包装器
 */
fun TerminableConsumer.createTerminableHttpClient(
    name: String = "default",
    configure: HttpClientConfig.() -> Unit = {}
): TerminableHttpClientWrapper {
    val manager = bindHttpClientManager(name)
    val client = manager.getOrCreateClient(name, configure)
    val wrapper = TerminableHttpClientWrapper(client)
    bind(wrapper)
    return wrapper
}

/**
 * 为 TerminableConsumer 创建认证的可终止 HTTP 客户端
 */
fun TerminableConsumer.createTerminableAuthHttpClient(
    name: String,
    auth: AuthConfig,
    configure: HttpClientConfig.() -> Unit = {}
): TerminableHttpClientWrapper {
    return createTerminableHttpClient(name) {
        authConfig = auth
        configure()
    }
}

/**
 * 为 TerminableConsumer 创建 API Key 认证的可终止 HTTP 客户端
 */
fun TerminableConsumer.createTerminableApiKeyHttpClient(
    name: String,
    apiKey: String,
    headerName: String = "X-API-Key",
    configure: HttpClientConfig.() -> Unit = {}
): TerminableHttpClientWrapper {
    return createTerminableAuthHttpClient(name, AuthConfig.ApiKey(apiKey, headerName), configure)
}

/**
 * 为 TerminableConsumer 创建 Bearer Token 认证的可终止 HTTP 客户端
 */
fun TerminableConsumer.createTerminableBearerHttpClient(
    name: String,
    token: String,
    refreshToken: String? = null,
    configure: HttpClientConfig.() -> Unit = {}
): TerminableHttpClientWrapper {
    return createTerminableAuthHttpClient(name, AuthConfig.Bearer(token, refreshToken), configure)
}

/**
 * 为 TerminableConsumer 创建 Basic 认证的可终止 HTTP 客户端
 */
fun TerminableConsumer.createTerminableBasicAuthHttpClient(
    name: String,
    username: String,
    password: String,
    realm: String? = null,
    configure: HttpClientConfig.() -> Unit = {}
): TerminableHttpClientWrapper {
    return createTerminableAuthHttpClient(name, AuthConfig.Basic(username, password, realm), configure)
}

// 便捷的请求方法

/**
 * 执行可终止的 GET 请求
 */
fun TerminableConsumer.terminableGet(
    url: String,
    scope: CoroutineScope,
    clientName: String = "default",
    configure: HttpRequestBuilder.() -> Unit = {},
    onResult: (Result<HttpResponse>) -> Unit = {}
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        client.getClient().get(url, configure)
    }, onResult)
}

/**
 * 执行可终止的类型安全 GET 请求
 */
inline fun <reified T> TerminableConsumer.terminableGetTyped(
    url: String,
    scope: CoroutineScope,
    clientName: String = "default",
    noinline configure: HttpRequestBuilder.() -> Unit = {},
    noinline onResult: (Result<T>) -> Unit = {}
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        client.getClient().get(url, configure).body<T>()
    }, onResult)
}

/**
 * 执行可终止的 POST 请求
 */
fun TerminableConsumer.terminablePost(
    url: String,
    scope: CoroutineScope,
    clientName: String = "default",
    configure: HttpRequestBuilder.() -> Unit = {},
    onResult: (Result<HttpResponse>) -> Unit = {}
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        client.getClient().post(url, configure)
    }, onResult)
}

/**
 * 执行可终止的类型安全 POST 请求
 */
inline fun <reified T> TerminableConsumer.terminablePostTyped(
    url: String,
    scope: CoroutineScope,
    clientName: String = "default",
    noinline configure: HttpRequestBuilder.() -> Unit = {},
    noinline onResult: (Result<T>) -> Unit = {}
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        client.getClient().post(url, configure).body<T>()
    }, onResult)
}

/**
 * 执行可终止的 PUT 请求
 */
fun TerminableConsumer.terminablePut(
    url: String,
    scope: CoroutineScope,
    clientName: String = "default",
    configure: HttpRequestBuilder.() -> Unit = {},
    onResult: (Result<HttpResponse>) -> Unit = {}
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        client.getClient().put(url, configure)
    }, onResult)
}

/**
 * 执行可终止的 DELETE 请求
 */
fun TerminableConsumer.terminableDelete(
    url: String,
    scope: CoroutineScope,
    clientName: String = "default",
    configure: HttpRequestBuilder.() -> Unit = {},
    onResult: (Result<HttpResponse>) -> Unit = {}
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        client.getClient().delete(url, configure)
    }, onResult)
}

/**
 * 执行可终止的网络连接检查
 */
fun TerminableConsumer.terminableNetworkCheck(
    host: String = "8.8.8.8",
    timeout: Long = 5000,
    scope: CoroutineScope,
    onResult: (Boolean) -> Unit
): Job {
    return scope.launch {
        try {
            val isAvailable = NetworkUtils.isNetworkAvailable(host, timeout)
            onResult(isAvailable)
        } catch (e: Exception) {
            onResult(false)
        }
    }
}

/**
 * 执行可终止的文件下载
 */
fun TerminableConsumer.terminableDownload(
    url: String,
    maxSize: Long = 10 * 1024 * 1024,
    scope: CoroutineScope,
    clientName: String = "default",
    onResult: (Result<ByteArray>) -> Unit
): Job {
    val client = createTerminableHttpClient(clientName)
    return client.executeRequest(scope, {
        NetworkUtils.downloadToByteArray(client.getClient(), url, maxSize)
    }, onResult)
}
