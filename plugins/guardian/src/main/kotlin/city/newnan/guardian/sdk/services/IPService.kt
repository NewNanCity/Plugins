package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * IP管理服务
 */
class IPService(httpClient: HttpClient) : BaseService(httpClient) {

    /**
     * 获取IP信息（包含风险信息）
     */
    fun getIPInfo(request: GetIPInfoRequest): IPInfo {
        return get("/api/v1/ips/${request.ip}")
    }

    /**
     * 封禁IP
     */
    fun banIP(request: BanIPRequest) {
        post<Unit>("/api/v1/ips/ban", request)
    }

    /**
     * 解封IP
     */
    fun unbanIP(request: UnbanIPRequest) {
        post<Unit>("/api/v1/ips/unban", request)
    }

    /**
     * 获取IP列表
     */
    fun listIPs(request: ListIPsRequest): ListIPsResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,           // 转换为snake_case
            "banned_only" to request.bannedOnly,       // 转换为snake_case
            "min_threat_level" to request.minThreatLevel?.value, // 转换为snake_case
            "min_risk_score" to request.minRiskScore   // 转换为snake_case
        ))
        return get("/api/v1/ips", params)
    }

    /**
     * 获取被封禁的IP列表
     */
    fun getBannedIPs(request: ListIPsRequest): ListIPsResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,           // 转换为snake_case
            "banned_only" to request.bannedOnly,       // 转换为snake_case
            "min_threat_level" to request.minThreatLevel?.value, // 转换为snake_case
            "min_risk_score" to request.minRiskScore   // 转换为snake_case
        ))
        return get("/api/v1/ips/banned", params)
    }

    /**
     * 获取可疑IP列表
     */
    fun getSuspiciousIPs(request: ListIPsRequest): ListIPsResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,           // 转换为snake_case
            "banned_only" to request.bannedOnly,       // 转换为snake_case
            "min_threat_level" to request.minThreatLevel?.value, // 转换为snake_case
            "min_risk_score" to request.minRiskScore   // 转换为snake_case
        ))
        return get("/api/v1/ips/suspicious", params)
    }

    /**
     * 获取高风险IP列表
     */
    fun getHighRiskIPs(request: ListIPsRequest): ListIPsResponse {
        val params = buildParams(mapOf(
            "page" to request.page,
            "page_size" to request.pageSize,           // 转换为snake_case
            "banned_only" to request.bannedOnly,       // 转换为snake_case
            "min_threat_level" to request.minThreatLevel?.value, // 转换为snake_case
            "min_risk_score" to request.minRiskScore   // 转换为snake_case
        ))
        return get("/api/v1/ips/high-risk", params)
    }

    /**
     * 获取IP统计信息
     */
    fun getIPStatistics(): IPStatistics {
        return get("/api/v1/ips/statistics")
    }
}
