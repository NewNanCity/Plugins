package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * 城镇管理服务
 */
class TownService(httpClient: HttpClient) : BaseService(httpClient) {

    /**
     * 创建城镇
     */
    fun createTown(request: CreateTownRequest): Town {
        return post("/api/v1/towns", request)
    }

    /**
     * 获取城镇详情
     */
    fun getTown(request: GetTownRequest): TownDetailResponse {
        val params = buildParams(mapOf("detail" to request.detail))
        return get("/api/v1/towns/${request.id}", params)
    }

    /**
     * 更新城镇信息
     */
    fun updateTown(request: UpdateTownRequest): Town {
        return put("/api/v1/towns/${request.id}", request)
    }

    /**
     * 删除城镇
     */
    fun deleteTown(request: DeleteTownRequest) {
        delete("/api/v1/towns/${request.id}")
    }

    /**
     * 获取城镇列表
     */
    fun listTowns(request: ListTownsRequest): ListTownsResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,  // 转换为snake_case
            "name" to request.name,
            "search" to request.search,
            "min_level" to request.minLevel,  // 转换为snake_case
            "max_level" to request.maxLevel   // 转换为snake_case
        ))
        return get("/api/v1/towns", params)
    }
}
