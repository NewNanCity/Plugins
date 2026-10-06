package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient
import city.newnan.guardian.sdk.models.*

/**
 * 监控服务 - 处理服务器心跳和监控统计
 */
class MonitorService(httpClient: HttpClient) : BaseService(httpClient) {

    /** Establish the server session used to fence heartbeat and presence writes. */
    fun openServerSession(serverId: Int): ServerSessionResponse {
        val session: ServerSessionResponse = post("/api/v1/monitor/${serverId}/session", EmptyRequest())
        httpClient.setServerSession(session)
        return session
    }

    /**
     * 发送心跳
     */
    fun heartbeat(serverId: Int, request: HeartbeatRequest): HeartbeatResponse {
        return post("/api/v1/monitor/${serverId}/heartbeat", request)
    }

    /**
     * 获取监控统计
     */
    fun getMonitorStats(request: GetMonitorStatsRequest): GetMonitorStatsResponse {
        val params = buildParams(mapOf(
            "since" to request.since,
            "duration" to request.duration
        ))
        return get("/api/v1/monitor/${request.serverId}/stats", params)
    }
}
