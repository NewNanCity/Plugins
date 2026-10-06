package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * 玩家服务器关系管理服务
 */
class PlayerServerService(httpClient: HttpClient) : BaseService(httpClient) {

    /**
     * 获取玩家的服务器关系
     */
    fun getPlayerServers(request: GetPlayerServersRequest): PlayerServersResponse {
        val params = buildParams(mapOf("online_only" to request.onlineOnly)) // 转换为snake_case
        return get("/api/v1/players/${request.playerId}/servers", params)
    }

    /**
     * 获取全局在线玩家
     */
    fun getServerPlayers(request: GetServerPlayersRequest): ServerPlayersResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,   // 转换为snake_case
            "search" to request.search,
            "server_id" to request.serverId,   // 转换为snake_case
            "online_only" to request.onlineOnly // 转换为snake_case
        ))
        return get("/api/v1/server-players", params)
    }

    /**
     * 设置玩家离线状态
     */
    fun setPlayersOffline(request: SetPlayersOfflineRequest) {
        post<Unit>("/api/v1/servers/players/offline", request)
    }
}
