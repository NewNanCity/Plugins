package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * API Token管理服务
 */
class TokenService(httpClient: HttpClient) : BaseService(httpClient) {

    /**
     * 创建API Token
     */
    fun createApiToken(request: CreateApiTokenRequest): CreateApiTokenResponse {
        return post("/api/v1/tokens", request)
    }

    /**
     * 获取API Token详情
     */
    fun getApiToken(request: GetApiTokenRequest): ApiToken {
        return get("/api/v1/tokens/${request.id}")
    }

    /**
     * 更新API Token
     */
    fun updateApiToken(request: UpdateApiTokenRequest): ApiToken {
        return put("/api/v1/tokens/${request.id}", request)
    }

    /**
     * 删除API Token
     */
    fun deleteApiToken(request: DeleteApiTokenRequest) {
        delete("/api/v1/tokens/${request.id}")
    }

    /**
     * 获取API Token列表
     */
    fun listApiTokens(request: ListApiTokensRequest): ListApiTokensResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize  // 转换为snake_case
        ))
        return get("/api/v1/tokens", params)
    }
}
