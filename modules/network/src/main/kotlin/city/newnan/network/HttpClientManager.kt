package city.newnan.network

import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import city.newnan.core.terminable.CompositeTerminable
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP 客户端管理器
 *
 * 提供现代化的 HTTP 客户端功能，基于 Ktor Client 构建
 * 支持协程、类型安全的 DSL、插件化配置
 * 实现 TerminableConsumer 接口，支持生命周期管理
 */
class HttpClientManager(
    private val config: HttpClientConfig = HttpClientConfig()
) : TerminableConsumer, Terminable {
    private val clients = ConcurrentHashMap<String, HttpClient>()
    private val terminableRegistry = CompositeTerminable.create()

    /**
     * 获取默认的 HTTP 客户端
     */
    val defaultClient: HttpClient by lazy {
        getOrCreateClient("default")
    }

    /**
     * 获取或创建指定名称的 HTTP 客户端
     */
    fun getOrCreateClient(
        name: String,
        configure: HttpClientConfig.() -> Unit = {}
    ): HttpClient {
        return clients.computeIfAbsent(name) {
            createClient(config.copy().apply(configure))
        }
    }



    /**
     * 创建新的 HTTP 客户端
     */
    private fun createClient(config: HttpClientConfig): HttpClient {
        return HttpClient(OkHttp) {
            // 基础配置
            expectSuccess = config.expectSuccess

            // 超时配置
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeout.toMillis()
                connectTimeoutMillis = config.connectTimeout.toMillis()
                socketTimeoutMillis = config.socketTimeout.toMillis()
            }

            // 内容协商 (序列化)
            install(ContentNegotiation) {
                when (config.serialization) {
                    SerializationType.KOTLINX_JSON -> {
                        json(Json {
                            prettyPrint = config.prettyPrint
                            isLenient = config.lenient
                            ignoreUnknownKeys = config.ignoreUnknownKeys
                            encodeDefaults = config.encodeDefaults
                        })
                    }
                    SerializationType.JACKSON -> {
                        jackson {
                            // 使用与 config 模块相同的 Jackson 配置
                        }
                    }
                    SerializationType.BOTH -> {
                        json(Json {
                            prettyPrint = config.prettyPrint
                            isLenient = config.lenient
                            ignoreUnknownKeys = config.ignoreUnknownKeys
                            encodeDefaults = config.encodeDefaults
                        })
                        jackson()
                    }
                }
            }

            // 日志配置
            if (config.enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            when (config.logLevel) {
                                LogLevel.ALL, LogLevel.HEADERS, LogLevel.BODY ->
                                    println("[HTTP] $message") // 临时使用 println
                                LogLevel.INFO ->
                                    println("[HTTP] $message") // 临时使用 println
                                LogLevel.NONE -> { /* 不记录 */ }
                            }
                        }
                    }
                    level = config.logLevel
                }
            }

            // 认证配置
            config.authConfig?.let { authConfig ->
                install(Auth) {
                    when (authConfig) {
                        is AuthConfig.Basic -> {
                            basic {
                                credentials {
                                    BasicAuthCredentials(authConfig.username, authConfig.password)
                                }
                                realm = authConfig.realm
                            }
                        }
                        is AuthConfig.Bearer -> {
                            bearer {
                                loadTokens {
                                    BearerTokens(authConfig.token, authConfig.refreshToken ?: "")
                                }
                            }
                        }
                        is AuthConfig.ApiKey -> {
                            // API Key 通过默认请求头处理
                        }
                    }
                }
            }

            // 默认请求配置
            install(DefaultRequest) {
                // 设置默认 User-Agent
                headers.append("User-Agent", config.userAgent)

                // API Key 认证
                if (config.authConfig is AuthConfig.ApiKey) {
                    val apiKey = config.authConfig as AuthConfig.ApiKey
                    headers.append(apiKey.headerName, apiKey.key)
                }

                // 自定义默认头
                config.defaultHeaders.forEach { (key, value) ->
                    headers.append(key, value)
                }
            }

            // 资源支持 (类型安全的 URL)
            if (config.enableResources) {
                install(Resources)
            }

            // OkHttp 引擎配置
            engine {
                config {
                    connectTimeout(config.connectTimeout)
                    readTimeout(config.socketTimeout)
                    writeTimeout(config.socketTimeout)
                }
            }
        }
    }

    /**
     * 关闭所有客户端
     */
    fun closeAll() {
        clients.values.forEach { client ->
            try {
                client.close()
            } catch (e: Exception) {
                println("关闭 HTTP 客户端时出错: ${e.message}")
            }
        }
        clients.clear()
    }

    /**
     * 关闭指定名称的客户端
     */
    fun closeClient(name: String) {
        clients.remove(name)?.let { client ->
            try {
                client.close()
            } catch (e: Exception) {
                println("关闭 HTTP 客户端 '$name' 时出错: ${e.message}")
            }
        }
    }

    // TerminableConsumer 接口实现
    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    // Terminable 接口实现
    override fun close() {
        terminableRegistry.close()
        closeAll()
    }

    override fun isClosed(): Boolean {
        return terminableRegistry.isClosed() && clients.isEmpty()
    }
}

/**
 * HTTP 客户端配置
 */
data class HttpClientConfig(
    var expectSuccess: Boolean = true,
    var requestTimeout: Duration = Duration.ofSeconds(30),
    var connectTimeout: Duration = Duration.ofSeconds(10),
    var socketTimeout: Duration = Duration.ofSeconds(30),
    var serialization: SerializationType = SerializationType.KOTLINX_JSON,
    var prettyPrint: Boolean = false,
    var lenient: Boolean = true,
    var ignoreUnknownKeys: Boolean = true,
    var encodeDefaults: Boolean = false,
    var enableLogging: Boolean = true,
    var logLevel: LogLevel = LogLevel.INFO,
    var enableResources: Boolean = true,
    var userAgent: String = "NewNanPlugins-HttpClient/1.0",
    var authConfig: AuthConfig? = null,
    var defaultHeaders: Map<String, String> = emptyMap()
) {
    fun copy(): HttpClientConfig = HttpClientConfig(
        expectSuccess, requestTimeout, connectTimeout, socketTimeout,
        serialization, prettyPrint, lenient, ignoreUnknownKeys, encodeDefaults,
        enableLogging, logLevel, enableResources, userAgent, authConfig, defaultHeaders
    )
}

/**
 * 序列化类型
 */
enum class SerializationType {
    KOTLINX_JSON,
    JACKSON,
    BOTH
}

/**
 * 认证配置
 */
sealed class AuthConfig {
    data class Basic(
        val username: String,
        val password: String,
        val realm: String? = null
    ) : AuthConfig()

    data class Bearer(
        val token: String,
        val refreshToken: String? = null
    ) : AuthConfig()

    data class ApiKey(
        val key: String,
        val headerName: String = "X-API-Key"
    ) : AuthConfig()
}
