package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * 玩家管理服务
 */
class PlayerService(httpClient: HttpClient) : BaseService(httpClient) {

    /**
     * 创建玩家
     */
    fun createPlayer(request: CreatePlayerRequest): Player {
        return post("/api/v1/players", request)
    }

    /**
     * 获取玩家详情
     */
    fun getPlayer(request: GetPlayerRequest): Player {
        return get("/api/v1/players/${request.id}")
    }

    /**
     * 更新玩家信息
     */
    fun updatePlayer(request: UpdatePlayerRequest): Player {
        return put("/api/v1/players/${request.id}", request)
    }

    /**
     * 删除玩家
     */
    fun deletePlayer(request: DeletePlayerRequest) {
        delete("/api/v1/players/${request.id}")
    }

    /**
     * 获取玩家列表
     */
    fun listPlayers(request: ListPlayersRequest): ListPlayersResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,  // 转换为snake_case
            "search" to request.search,
            "town_id" to request.townId,      // 转换为snake_case
            "ban_mode" to request.banMode?.value,  // 转换为snake_case
            "name" to request.name,
            "qq" to request.qq,
            "qqguild" to request.qqguild,
            "discord" to request.discord
        ))
        return get("/api/v1/players", params)
    }

    /**
     * 封禁玩家
     */
    fun banPlayer(request: BanPlayerRequest) {
        post<Unit>("/api/v1/players/${request.playerId}/ban", request)
    }

    /**
     * 解封玩家
     */
    fun unbanPlayer(request: UnbanPlayerRequest) {
        post<Unit>("/api/v1/players/${request.playerId}/unban")
    }

    /**
     * 玩家验证（支持批处理）
     */
    fun validate(request: ValidateRequest): ValidateResponse {
        return post("/api/v1/players/validate", request)
    }
}
