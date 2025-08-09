package city.newnan.network.examples

import city.newnan.core.terminable.CompositeTerminable
import city.newnan.core.terminable.TerminableConsumer
import city.newnan.network.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

/**
 * 可终止网络请求示例
 * 
 * 展示如何使用 Terminable 网络功能，确保所有网络请求都能被正确管理和清理
 */

@Serializable
data class ExampleUser(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class ExamplePost(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String
)

/**
 * 示例服务类，展示如何在实际应用中使用可终止网络请求
 */
class ExampleApiService : TerminableConsumer {
    
    private val terminableRegistry = CompositeTerminable.create()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * 获取用户信息（可终止请求）
     */
    fun getUserAsync(
        userId: Int,
        onResult: (Result<ExampleUser>) -> Unit
    ): Job {
        return terminableGetTyped<ExampleUser>(
            "https://jsonplaceholder.typicode.com/users/$userId",
            scope,
            onResult = onResult
        )
    }
    
    /**
     * 获取用户的所有帖子（可终止请求）
     */
    fun getUserPostsAsync(
        userId: Int,
        onResult: (Result<List<ExamplePost>>) -> Unit
    ): Job {
        return terminableGetTyped<List<ExamplePost>>(
            "https://jsonplaceholder.typicode.com/posts",
            scope,
            configure = {
                url {
                    parameters.append("userId", userId.toString())
                }
            },
            onResult = onResult
        )
    }
    
    /**
     * 创建新帖子（可终止请求）
     */
    fun createPostAsync(
        post: ExamplePost,
        onResult: (Result<ExamplePost>) -> Unit
    ): Job {
        return terminablePostTyped<ExamplePost>(
            "https://jsonplaceholder.typicode.com/posts",
            scope,
            configure = {
                jsonBody(post)
            },
            onResult = onResult
        )
    }
    
    /**
     * 检查网络连接
     */
    fun checkNetworkAsync(onResult: (Boolean) -> Unit): Job {
        return terminableNetworkCheck(
            scope = scope,
            onResult = onResult
        )
    }
    
    /**
     * 下载文件
     */
    fun downloadFileAsync(
        url: String,
        onResult: (Result<ByteArray>) -> Unit
    ): Job {
        return terminableDownload(
            url = url,
            scope = scope,
            onResult = onResult
        )
    }
    
    // TerminableConsumer 实现
    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }
    
    /**
     * 关闭服务，清理所有资源
     */
    fun close() {
        scope.cancel()
        terminableRegistry.close()
    }
}

/**
 * 使用示例
 */
fun main() {
    runBlocking {
        val apiService = ExampleApiService()
        
        try {
            println("=== 可终止网络请求示例 ===")
            
            // 示例1: 检查网络连接
            println("\n1. 检查网络连接:")
            val networkJob = apiService.checkNetworkAsync { isConnected ->
                println("网络连接状态: ${if (isConnected) "已连接" else "未连接"}")
            }
            networkJob.join()
            
            // 示例2: 获取单个用户
            println("\n2. 获取用户信息:")
            val userJob = apiService.getUserAsync(1) { result ->
                result.onSuccess { user ->
                    println("用户: ${user.name} (${user.email})")
                }.onFailure { error ->
                    println("获取用户失败: ${error.message}")
                }
            }
            userJob.join()
            
            // 示例3: 获取用户帖子
            println("\n3. 获取用户帖子:")
            val postsJob = apiService.getUserPostsAsync(1) { result ->
                result.onSuccess { posts ->
                    println("用户有 ${posts.size} 篇帖子")
                    posts.take(2).forEach { post ->
                        println("- ${post.title}")
                    }
                }.onFailure { error ->
                    println("获取帖子失败: ${error.message}")
                }
            }
            postsJob.join()
            
            // 示例4: 创建新帖子
            println("\n4. 创建新帖子:")
            val newPost = ExamplePost(
                id = 0,
                userId = 1,
                title = "测试帖子",
                body = "这是一个测试帖子的内容"
            )
            
            val createJob = apiService.createPostAsync(newPost) { result ->
                result.onSuccess { createdPost ->
                    println("帖子创建成功: ID=${createdPost.id}")
                }.onFailure { error ->
                    println("创建帖子失败: ${error.message}")
                }
            }
            createJob.join()
            
            // 示例5: 下载文件
            println("\n5. 下载文件:")
            val downloadJob = apiService.downloadFileAsync("https://httpbin.org/bytes/1024") { result ->
                result.onSuccess { data ->
                    println("下载成功，大小: ${NetworkUtils.formatFileSize(data.size.toLong())}")
                }.onFailure { error ->
                    println("下载失败: ${error.message}")
                }
            }
            downloadJob.join()
            
            println("\n=== 示例完成 ===")
            
        } finally {
            // 清理所有资源
            apiService.close()
            println("所有网络资源已清理")
        }
    }
}

/**
 * 模拟插件环境中的使用
 */
class ExamplePlugin : TerminableConsumer {
    private val terminableRegistry = CompositeTerminable.create()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun onEnable() {
        println("插件启用，开始网络操作...")
        
        // 创建认证客户端
        val apiClient = createTerminableApiKeyHttpClient(
            "api", 
            "plugin-api-key"
        )
        
        // 执行可终止请求
        terminableGetTyped<ExampleUser>(
            "https://jsonplaceholder.typicode.com/users/1",
            scope
        ) { result ->
            result.onSuccess { user ->
                println("插件获取用户成功: ${user.name}")
            }.onFailure { error ->
                println("插件获取用户失败: ${error.message}")
            }
        }
        
        // 定期检查网络状态
        scope.launch {
            while (isActive) {
                terminableNetworkCheck(scope = this) { isConnected ->
                    println("网络状态检查: ${if (isConnected) "正常" else "异常"}")
                }
                delay(30000) // 30秒检查一次
            }
        }
    }
    
    fun onDisable() {
        println("插件禁用，清理网络资源...")
        // 关闭所有资源，包括正在进行的请求
        scope.cancel()
        terminableRegistry.close()
        println("网络资源清理完成")
    }
    
    // TerminableConsumer 委托
    override fun <T : AutoCloseable> bind(terminable: T): T = terminableRegistry.bind(terminable)
}

/**
 * 插件使用示例
 */
fun pluginExample() {
    val plugin = ExamplePlugin()
    
    try {
        plugin.onEnable()
        
        // 模拟插件运行一段时间
        Thread.sleep(5000)
        
    } finally {
        plugin.onDisable()
    }
}
