package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * 服务器管理服务
 */
class ServerService(httpClient: HttpClient) : BaseService(httpClient) {

    /**
     * 注册服务器
     */
    fun createServer(request: CreateServerRequest): ServerRegistry {
        return post("/api/v1/servers", request)
    }

    /**
     * 获取服务器信息
     */
    fun getServer(request: GetServerRequest): ServerDetailResponse {
        val params = buildParams(mapOf("detail" to request.detail))
        return get("/api/v1/servers/${request.id}", params)
    }

    /**
     * 更新服务器信息
     */
    fun updateServer(request: UpdateServerRequest): ServerRegistry {
        return put("/api/v1/servers/${request.id}", request)
    }

    /**
     * 删除服务器
     */
    fun deleteServer(request: DeleteServerRequest) {
        delete("/api/v1/servers/${request.id}")
    }

    /**
     * 获取服务器列表
     */
    fun listServers(request: ListServersRequest): ListServersResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,   // 转换为snake_case
            "search" to request.search,
            "online_only" to request.onlineOnly // 转换为snake_case
        ))
        return get("/api/v1/servers", params)
    }
}
