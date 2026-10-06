package city.newnan.guardian.sdk.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

// ==================== 枚举类型 ====================

/**
 * 封禁模式枚举
 */
enum class BanMode(@JsonValue val value: Int) {
    NORMAL(0),      // 正常
    TEMPORARY(1),   // 临时封禁
    PERMANENT(2)    // 永久封禁
}

/**
 * 登录动作枚举
 */
enum class LoginAction(@JsonValue val value: Int) {
    LOGIN(1),
    LOGOUT(2)
}

/**
 * IP威胁等级枚举
 */
enum class ThreatLevel(@JsonValue val value: Int) {
    LOW(0),         // 低威胁
    MEDIUM(1),      // 中等威胁
    HIGH(2),        // 高威胁
    CRITICAL(3)     // 严重威胁
}

/**
 * IP查询状态枚举
 */
enum class QueryStatus(@JsonValue val value: Int) {
    PENDING(0),     // 待查询
    COMPLETED(1),   // 已完成
    FAILED(2)       // 查询失败
}

// ==================== 基础数据结构 ====================

/**
 * 空请求
 */
data class EmptyRequest(
    val dummy: String? = null
)

/**
 * 空响应
 */
data class EmptyResponse(
    val dummy: String? = null
)

// ==================== 通用响应模型 ====================

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
    @JsonProperty("request_id")
    val requestId: String
)

data class ErrorResponse(
    val detail: String
)

// ==================== 玩家相关模型 ====================

/**
 * 玩家信息
 */
data class Player(
    val id: Int,                                    // 玩家ID
    val name: String,                               // 游戏ID
    @JsonProperty("town_id") val townId: Int? = null, // 所属城镇ID
    val qq: String? = null,                         // QQ号
    val qqguild: String? = null,                    // QQ频道ID
    val discord: String? = null,                    // Discord ID
    @JsonProperty("in_qq_group") val inQqGroup: Boolean,  // 是否在QQ群
    @JsonProperty("in_qq_guild") val inQqGuild: Boolean,  // 是否在QQ频道
    @JsonProperty("in_discord") val inDiscord: Boolean,   // 是否在Discord群
    @JsonProperty("ban_mode") val banMode: BanMode,       // 封禁模式
    @JsonProperty("ban_expire") val banExpire: String? = null,  // 封禁到期时间
    @JsonProperty("ban_reason") val banReason: String? = null,  // 封禁原因
    @JsonProperty("created_at") val createdAt: String,    // 创建时间
    @JsonProperty("updated_at") val updatedAt: String     // 更新时间
)

/**
 * 创建玩家请求
 */
data class CreatePlayerRequest(
    val name: String,                               // 游戏ID
    @JsonProperty("town_id") val townId: Int? = null,     // 所属城镇ID
    val qq: String? = null,                         // QQ号
    val qqguild: String? = null,                    // QQ频道ID
    val discord: String? = null,                    // Discord ID
    @JsonProperty("in_qq_group") val inQqGroup: Boolean = false,  // 是否在QQ群
    @JsonProperty("in_qq_guild") val inQqGuild: Boolean = false,  // 是否在QQ频道
    @JsonProperty("in_discord") val inDiscord: Boolean = false    // 是否在Discord群
)

/**
 * 获取玩家请求
 */
data class GetPlayerRequest(
    val id: Int                                     // 玩家ID
)

/**
 * 删除玩家请求
 */
data class DeletePlayerRequest(
    val id: Int                                     // 玩家ID
)

/**
 * 解封玩家请求
 */
data class UnbanPlayerRequest(
    @JsonProperty("player_id") val playerId: Int    // 玩家ID
)

/**
 * 玩家列表请求
 */
data class ListPlayersRequest(
    val page: Int = 1,                              // 页码
    @JsonProperty("page_size") val pageSize: Int = 20,  // 每页数量
    val search: String? = null,                     // 搜索关键词
    @JsonProperty("town_id") val townId: Int? = null,   // 城镇ID过滤
    @JsonProperty("ban_mode") val banMode: BanMode? = null,  // 封禁状态过滤
    val name: String? = null,                       // 精确游戏名过滤
    val qq: String? = null,                         // QQ号过滤
    val qqguild: String? = null,                    // QQ频道ID过滤
    val discord: String? = null                     // Discord ID过滤
)

/**
 * 更新玩家请求
 */
data class UpdatePlayerRequest(
    val id: Int,                                    // 玩家ID
    val name: String? = null,                       // 游戏ID
    @JsonProperty("town_id") val townId: Int? = null,     // 所属城镇ID
    val qq: String? = null,                         // QQ号
    val qqguild: String? = null,                    // QQ频道ID
    val discord: String? = null,                    // Discord ID
    @JsonProperty("in_qq_group") val inQqGroup: Boolean? = null,  // 是否在QQ群
    @JsonProperty("in_qq_guild") val inQqGuild: Boolean? = null,  // 是否在QQ频道
    @JsonProperty("in_discord") val inDiscord: Boolean? = null    // 是否在Discord群
)

/**
 * 封禁玩家请求
 */
data class BanPlayerRequest(
    @JsonProperty("player_id") val playerId: Int,         // 玩家ID
    @JsonProperty("ban_mode") val banMode: BanMode,       // 封禁模式
    @JsonProperty("duration_seconds") val durationSeconds: Long? = null,  // 封禁时长(秒)
    val reason: String                              // 封禁原因
)

/**
 * 玩家列表响应
 */
data class ListPlayersResponse(
    val players: List<Player>,                      // 玩家列表
    val total: Long,                                // 总数
    val page: Int,                                  // 当前页码
    @JsonProperty("page_size") val pageSize: Int    // 每页数量
)

// ==================== 城镇相关模型 ====================

/**
 * 城镇信息
 */
data class Town(
    val id: Int,                                          // 城镇ID
    val name: String,                                     // 城镇名称
    val level: Int,                                       // 城镇等级(0-5星级)
    @JsonProperty("leader_id") val leaderId: Int? = null, // 镇长玩家ID
    @JsonProperty("qq_group") val qqGroup: String? = null,// 绑定QQ群号
    val description: String? = null,                      // 城镇描述
    @JsonProperty("created_at") val createdAt: String,    // 创建时间
    @JsonProperty("updated_at") val updatedAt: String     // 更新时间
)

/**
 * 创建城镇请求
 */
data class CreateTownRequest(
    val name: String,                                     // 城镇名称
    val level: Int = 0,                                   // 城镇等级，默认0
    @JsonProperty("leader_id") val leaderId: Int? = null, // 镇长玩家ID
    @JsonProperty("qq_group") val qqGroup: String? = null,// 绑定QQ群号
    val description: String? = null                       // 城镇描述
)

/**
 * 获取城镇请求
 */
data class GetTownRequest(
    val id: Int,                                          // 城镇ID
    val detail: Boolean = false                           // 是否返回详细信息
)

/**
 * 删除城镇请求
 */
data class DeleteTownRequest(
    val id: Int                                           // 城镇ID
)

/**
 * 城镇列表请求
 */
data class ListTownsRequest(
    val page: Int = 1,                                    // 页码
    @JsonProperty("page_size") val pageSize: Int = 20,    // 每页数量
    val name: String? = null,                             // 名称
    val search: String? = null,                           // 搜索关键词
    @JsonProperty("min_level") val minLevel: Int? = null, // 最小等级
    @JsonProperty("max_level") val maxLevel: Int? = null  // 最大等级
)

/**
 * 更新城镇请求
 */
data class UpdateTownRequest(
    val id: Int,                                          // 城镇ID
    val name: String? = null,                             // 城镇名称
    val level: Int? = null,                               // 城镇等级
    @JsonProperty("leader_id") val leaderId: Int? = null, // 镇长玩家ID
    @JsonProperty("qq_group") val qqGroup: String? = null,// 绑定QQ群号
    val description: String? = null,                      // 城镇描述
    @JsonProperty("add_players") val addPlayers: List<Int>? = null,     // 待添加的玩家ID列表
    @JsonProperty("remove_players") val removePlayers: List<Int>? = null // 待移除的玩家ID列表
)

/**
 * 城镇列表响应
 */
data class ListTownsResponse(
    val towns: List<Town>,                                // 城镇列表
    val total: Long,                                      // 总数
    val page: Int,                                        // 当前页码
    @JsonProperty("page_size") val pageSize: Int          // 每页数量
)

/**
 * 城镇详细信息响应
 */
data class TownDetailResponse(
    val town: Town,                                       // 城镇基本信息
    val leader: Int? = null,                              // 镇长信息
    val members: List<Player>                             // 成员列表
)

// ==================== 服务器相关模型 ====================

/**
 * 服务器注册信息
 */
data class ServerRegistry(
    val id: Int,                                          // 服务器ID
    val name: String,                                     // 服务器名称
    val address: String,                                  // 服务器地址
    val description: String? = null,                      // 服务器描述
    val active: Boolean,                                  // 是否激活
    @JsonProperty("created_at") val createdAt: String,    // 创建时间
    @JsonProperty("updated_at") val updatedAt: String     // 更新时间
)

/**
 * 服务器状态
 */
data class ServerStatus(
    @JsonProperty("server_id") val serverId: Int,         // 服务器ID
    val online: Boolean,                                  // 是否在线
    @JsonProperty("current_players") val currentPlayers: Int,  // 当前在线人数
    @JsonProperty("max_players") val maxPlayers: Int,     // 最大玩家数
    @JsonProperty("latency_ms") val latencyMs: Int? = null,    // 延迟毫秒
    val tps: Double? = null,                              // 服务器TPS
    val version: String? = null,                          // 服务器版本
    val motd: String? = null,                             // 服务器描述
    @JsonProperty("expire_at") val expireAt: String,      // 状态失效时间
    @JsonProperty("last_heartbeat") val lastHeartbeat: String  // 最后心跳时间
)

/**
 * 服务器创建请求
 */
data class CreateServerRequest(
    val name: String,                                     // 服务器名称
    val address: String,                                  // 服务器地址
    val description: String? = null                       // 服务器描述
)

/**
 * 获取服务器请求
 */
data class GetServerRequest(
    val id: Int,                                          // 服务器ID
    val detail: Boolean = false                           // 是否返回详细信息
)

/**
 * 删除服务器请求
 */
data class DeleteServerRequest(
    val id: Int                                           // 服务器ID
)

/**
 * 服务器列表请求
 */
data class ListServersRequest(
    val page: Int = 1,                                    // 页码
    @JsonProperty("page_size") val pageSize: Int = 20,    // 每页数量
    val search: String? = null,                           // 搜索关键词
    @JsonProperty("online_only") val onlineOnly: Boolean = false  // 仅显示在线服务器
)

/**
 * 更新服务器请求
 */
data class UpdateServerRequest(
    val id: Int,                                          // 服务器ID
    val name: String? = null,                             // 服务器名称
    val address: String? = null,                          // 服务器地址
    val description: String? = null                       // 服务器描述
)

/**
 * 服务器列表响应
 */
data class ListServersResponse(
    val servers: List<ServerRegistry>,                    // 服务器列表
    val total: Long,                                      // 总数
    val page: Int,                                        // 当前页码
    @JsonProperty("page_size") val pageSize: Int          // 每页数量
)

/**
 * 服务器详细信息响应
 */
data class ServerDetailResponse(
    val server: ServerRegistry,                           // 服务器信息
    val status: ServerStatus? = null                      // 服务器状态
)

// ==================== 监控相关模型 ====================

/**
 * 心跳请求
 */
data class HeartbeatRequest(
    @JsonProperty("current_players") val currentPlayers: Int,  // 当前在线人数
    @JsonProperty("max_players") val maxPlayers: Int,     // 最大玩家数
    val tps: Double? = null,                              // 服务器TPS
    val version: String? = null,                          // 服务器版本
    val motd: String? = null,                             // 服务器描述
    @JsonProperty("rtt_ms") val rttMs: Long? = null       // RTT
)

/**
 * 心跳响应
 */
data class HeartbeatResponse(
    @JsonProperty("received_at") val receivedAt: Long,    // 服务端接收时间戳
    @JsonProperty("response_at") val responseAt: Long,    // 服务端响应时间戳
    @JsonProperty("expire_duration_ms") val expireDurationMs: Long  // 状态过期时间(毫秒)
)

/** Server session used to reject stale plugin writes after a restart or takeover. */
data class ServerSessionResponse(
    @JsonProperty("session_id") val sessionId: String,
    @JsonProperty("session_epoch") val sessionEpoch: Long
)

/**
 * 监控统计项目
 */
data class MonitorStatRecord(
    val timestamp: Long,                                  // 统计时间戳
    @JsonProperty("current_players") val currentPlayers: Int,  // 当前在线人数
    val tps: Double? = null,                              // 服务器TPS
    @JsonProperty("latency_ms") val latencyMs: Long? = null    // 延迟毫秒
)

/**
 * 监控统计响应
 */
data class GetMonitorStatsResponse(
    @JsonProperty("server_id") val serverId: Int,         // 服务器ID
    val stats: List<MonitorStatRecord>                    // 监控统计信息列表
)

/**
 * 获取监控统计请求
 */
data class GetMonitorStatsRequest(
    @JsonProperty("server_id") val serverId: Int,         // 服务器ID
    val since: Long = 0,                                  // 起始时间戳
    val duration: Long = 3600                             // 持续时间
)

// ==================== API Token相关模型 ====================

/**
 * API Token信息
 */
data class ApiToken(
    val id: Int,                                          // Token ID
    val name: String,                                     // Token名称
    val role: String,                                     // 角色
    val description: String? = null,                      // Token描述
    val active: Boolean,                                  // 是否激活
    @JsonProperty("expire_at") val expireAt: String? = null,   // 过期时间
    @JsonProperty("last_used_at") val lastUsedAt: String? = null,  // 最后使用时间
    @JsonProperty("last_used_ip") val lastUsedIp: String? = null,  // 最后使用IP
    @JsonProperty("created_at") val createdAt: String,    // 创建时间
    @JsonProperty("updated_at") val updatedAt: String     // 更新时间
)

/**
 * 创建API Token请求
 */
data class CreateApiTokenRequest(
    val name: String,                                     // Token名称
    val role: String,                                     // 角色
    val description: String? = null,                      // Token描述
    @JsonProperty("expire_days") val expireDays: Long? = null  // 过期天数
)

/**
 * 获取API Token请求
 */
data class GetApiTokenRequest(
    val id: Int                                           // Token ID
)

/**
 * 更新API Token请求
 */
data class UpdateApiTokenRequest(
    val id: Int,                                          // Token ID
    val name: String? = null,                             // Token名称
    val role: String? = null,                             // 角色
    val description: String? = null,                      // Token描述
    val active: Boolean? = null                           // 是否激活
)

/**
 * 删除API Token请求
 */
data class DeleteApiTokenRequest(
    val id: Int                                           // Token ID
)

/**
 * API Token列表请求
 */
data class ListApiTokensRequest(
    val page: Int = 1,                                    // 页码
    @JsonProperty("page_size") val pageSize: Int = 20     // 每页数量
)

/**
 * 创建API Token响应
 */
data class CreateApiTokenResponse(
    @JsonProperty("token_info") val tokenInfo: ApiToken, // Token信息
    @JsonProperty("token_value") val tokenValue: String  // Token明文值
)

/**
 * API Token列表响应
 */
data class ListApiTokensResponse(
    val tokens: List<ApiToken>,                           // Token列表
    val total: Long,                                      // 总数
    val page: Int,                                        // 当前页码
    @JsonProperty("page_size") val pageSize: Int          // 每页数量
)

// ==================== IP管理相关模型 ====================

/**
 * IP信息 - 包含基础信息和风险信息
 */
data class IPInfo(
    val ip: String,                                       // IP地址
    @JsonProperty("ip_type") val ipType: String,          // IP类型：ipv4/ipv6
    val country: String? = null,                          // 国家/地区
    @JsonProperty("country_code") val countryCode: String? = null,  // 国家代码
    val region: String? = null,                           // 省份/州
    val city: String? = null,                             // 城市
    val latitude: Double? = null,                         // 纬度
    val longitude: Double? = null,                        // 经度
    val timezone: String? = null,                         // 时区
    val isp: String? = null,                              // 网络服务提供商
    val organization: String? = null,                     // 组织名称
    val asn: String? = null,                              // ASN号码
    @JsonProperty("is_bogon") val isBogon: Boolean,       // 是否为Bogon IP
    @JsonProperty("is_mobile") val isMobile: Boolean,     // 是否为移动网络
    @JsonProperty("is_satellite") val isSatellite: Boolean,  // 是否为卫星网络
    @JsonProperty("is_crawler") val isCrawler: Boolean,   // 是否为爬虫
    @JsonProperty("is_datacenter") val isDatacenter: Boolean,  // 是否为数据中心IP
    @JsonProperty("is_tor") val isTor: Boolean,           // 是否为Tor出口节点
    @JsonProperty("is_proxy") val isProxy: Boolean,       // 是否为代理IP
    @JsonProperty("is_vpn") val isVpn: Boolean,           // 是否为VPN
    @JsonProperty("is_abuser") val isAbuser: Boolean,     // 是否为滥用者
    val banned: Boolean,                                  // 是否被封禁
    @JsonProperty("ban_reason") val banReason: String? = null,  // 封禁原因
    @JsonProperty("threat_level") val threatLevel: ThreatLevel,  // 威胁等级
    @JsonProperty("risk_score") val riskScore: Int,       // 风险评分（0-100）
    @JsonProperty("query_status") val queryStatus: QueryStatus,  // 查询状态
    @JsonProperty("last_query_at") val lastQueryAt: String? = null,  // 最后查询时间
    @JsonProperty("created_at") val createdAt: String,    // 创建时间
    @JsonProperty("updated_at") val updatedAt: String,    // 更新时间
    @JsonProperty("risk_level") val riskLevel: String,    // 风险等级
    @JsonProperty("risk_description") val riskDescription: String  // 风险描述
)

/**
 * 获取IP信息请求
 */
data class GetIPInfoRequest(
    val ip: String                                        // IP地址
)

/**
 * IP列表请求
 */
data class ListIPsRequest(
    val page: Int = 1,                                    // 页码
    @JsonProperty("page_size") val pageSize: Int = 20,    // 每页数量
    @JsonProperty("banned_only") val bannedOnly: Boolean = false,  // 仅显示被封禁的IP
    @JsonProperty("min_threat_level") val minThreatLevel: ThreatLevel? = null,  // 最小威胁等级
    @JsonProperty("min_risk_score") val minRiskScore: Int? = null   // 最小风险评分
)

/**
 * 封禁IP请求
 */
data class BanIPRequest(
    val ips: List<String>,                                // IP地址列表
    val reason: String                                    // 封禁原因
)

/**
 * 解封IP请求
 */
data class UnbanIPRequest(
    val ips: List<String>                                 // IP地址列表
)

/**
 * IP列表响应
 */
data class ListIPsResponse(
    val ips: List<IPInfo>,                                // IP列表
    val total: Long,                                      // 总数
    val page: Int,                                        // 当前页码
    @JsonProperty("page_size") val pageSize: Int          // 每页数量
)

/**
 * IP统计信息
 */
data class IPStatistics(
    @JsonProperty("total_ips") val totalIps: Long,        // 总IP数
    @JsonProperty("completed_ips") val completedIps: Long,// 已完成查询的IP数
    @JsonProperty("pending_ips") val pendingIps: Long,    // 待查询IP数
    @JsonProperty("failed_ips") val failedIps: Long,      // 查询失败IP数
    @JsonProperty("banned_ips") val bannedIps: Long,      // 被封禁IP数
    @JsonProperty("proxy_ips") val proxyIps: Long,        // 代理IP数
    @JsonProperty("vpn_ips") val vpnIps: Long,            // VPN IP数
    @JsonProperty("tor_ips") val torIps: Long,            // Tor IP数
    @JsonProperty("datacenter_ips") val datacenterIps: Long,  // 数据中心IP数
    @JsonProperty("high_risk_ips") val highRiskIps: Long  // 高风险IP数
)

// ==================== 玩家服务器关系相关模型 ====================

/**
 * 玩家服务器关系信息
 */
data class PlayerServer(
    @JsonProperty("player_id") val playerId: Int,         // 玩家ID
    @JsonProperty("server_id") val serverId: Int,         // 服务器ID
    val online: Boolean,                                  // 是否在线
    @JsonProperty("joined_at") val joinedAt: String,      // 加入时间
    @JsonProperty("created_at") val createdAt: String,    // 创建时间
    @JsonProperty("updated_at") val updatedAt: String     // 更新时间
)

/**
 * 在线玩家信息
 */
data class OnlinePlayer(
    @JsonProperty("player_id") val playerId: Int,         // 玩家ID
    @JsonProperty("player_name") val playerName: String,  // 玩家名
    @JsonProperty("server_id") val serverId: Int,         // 服务器ID
    @JsonProperty("server_name") val serverName: String,  // 服务器名
    @JsonProperty("joined_at") val joinedAt: String       // 加入时间
)

/**
 * 获取玩家服务器关系请求
 */
data class GetPlayerServersRequest(
    @JsonProperty("player_id") val playerId: Int,         // 玩家ID
    @JsonProperty("online_only") val onlineOnly: Boolean = false  // 仅显示在线服务器
)

/**
 * 玩家服务器关系列表响应
 */
data class PlayerServersResponse(
    val servers: List<PlayerServer>,                      // 服务器关系列表
    val total: Long                                       // 总数
)

/**
 * 服务器在线玩家响应
 */
data class ServerPlayersResponse(
    val players: List<OnlinePlayer>,                      // 在线玩家列表
    val total: Long,                                      // 总数
    val page: Int,                                        // 当前页码
    @JsonProperty("page_size") val pageSize: Int          // 每页数量
)

/**
 * 获取全局在线玩家请求
 */
data class GetServerPlayersRequest(
    val page: Int = 1,                                    // 页码
    @JsonProperty("page_size") val pageSize: Int = 50,    // 每页数量
    val search: String? = null,                           // 搜索玩家名
    @JsonProperty("server_id") val serverId: Int? = null, // 服务器ID过滤
    @JsonProperty("online_only") val onlineOnly: Boolean = false  // 仅显示在线玩家
)

/**
 * 设置玩家离线请求
 */
data class SetPlayersOfflineRequest(
    @JsonProperty("server_id") val serverId: Int,         // 服务器ID
    @JsonProperty("player_ids") val playerIds: List<Int>  // 玩家ID列表
)

// ==================== 玩家验证相关模型 ====================

/**
 * 单个玩家验证信息
 */
data class PlayerValidateInfo(
    @JsonProperty("player_name") val playerName: String,   // 玩家名：1-100字符
    val ip: String,                                        // IP地址：1-45字符
    @JsonProperty("client_version") val clientVersion: String? = null,    // 客户端版本
    @JsonProperty("protocol_version") val protocolVersion: String? = null // 协议版本
)

/**
 * 玩家验证请求（支持批处理）
 */
data class ValidateRequest(
    val players: List<PlayerValidateInfo>,                 // 玩家列表（1-100个）
    @JsonProperty("server_id") val serverId: Int,   // 服务器ID：1-999999
    val login: Boolean                                     // 是否为登录验证
)

/**
 * 单个玩家验证结果 - 包含IP风险信息
 */
data class PlayerValidateResult(
    @JsonProperty("player_name") val playerName: String,   // 玩家名
    val allowed: Boolean,                                  // 是否允许登录
    @JsonProperty("player_id") val playerId: Int? = null,  // 玩家ID
    val reason: String? = null,                            // 拒绝原因
    val newbie: Boolean = false,                           // 是否为新玩家
    @JsonProperty("ban_mode") val banMode: BanMode? = null,          // 封禁模式
    @JsonProperty("ban_expire") val banExpire: String? = null,       // 封禁到期时间(ISO8601格式)
    @JsonProperty("ban_reason") val banReason: String? = null        // 封禁原因
)

/**
 * 玩家验证响应（支持批处理）
 */
data class ValidateResponse(
    val results: List<PlayerValidateResult>,               // 验证结果列表
    @JsonProperty("processed_at") val processedAt: Long    // 处理时间戳
)
