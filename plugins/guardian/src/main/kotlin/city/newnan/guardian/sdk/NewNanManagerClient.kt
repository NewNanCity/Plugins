package city.newnan.guardian.sdk

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.services.*
import city.newnan.guardian.sdk.models.ServerSessionResponse

/**
 * NewNanManager客户端SDK - 同步版本
 *
 * 基于OkHttp+Jackson的同步HTTP客户端，适用于Minecraft插件开发
 * 不依赖协程，所有API调用都是同步的，开发者可以自行决定在哪个线程调用
 *
 * 使用示例:
 * ```kotlin
 * val client = NewNanManagerClient(
 *     token = "your-api-token",
 *     baseUrl = "https://your-server.com"
 * )
 *
 * // 使用模块化服务
 * val servers = client.servers.listServers()
 * val players = client.players.listPlayers()
 * val towns = client.towns.listTowns()
 *
 * // 记得关闭客户端
 * client.close()
 * ```
 */
class NewNanManagerClient(
    private val token: String,
    private val baseUrl: String,
    private val timeout: Long = 30L
) : AutoCloseable {

    private val httpClient: HttpClient = HttpClient(baseUrl, token, timeout)

    /**
     * 玩家管理服务
     */
    val players: PlayerService = PlayerService(httpClient)

    /**
     * 城镇管理服务
     */
    val towns: TownService = TownService(httpClient)

    /**
     * 服务器管理服务
     */
    val servers: ServerService = ServerService(httpClient)

    /**
     * API Token管理服务
     */
    val tokens: TokenService = TokenService(httpClient)

    /**
     * IP管理服务
     */
    val ips: IPService = IPService(httpClient)

    /**
     * 玩家服务器关系管理服务
     */
    val playerServers: PlayerServerService = PlayerServerService(httpClient)

    /**
     * 监控服务
     */
    val monitor: MonitorService = MonitorService(httpClient)

    @Volatile
    private var sessionServerId: Int? = null
    @Volatile
    private var session: ServerSessionResponse? = null

    @Synchronized
    fun ensureServerSession(serverId: Int): ServerSessionResponse {
        require(serverId > 0) { "serverId must be positive" }
        val current = session
        if (sessionServerId == serverId && current != null) {
            return current
        }
        return monitor.openServerSession(serverId).also {
            sessionServerId = serverId
            session = it
        }
    }

    /**
     * 关闭客户端，释放资源
     */
    override fun close() {
        httpClient.close()
    }
}
