package city.newnan.guardian.sdk.services

import city.newnan.guardian.sdk.http.HttpClient

/**
 * 基础服务类，提供通用的HTTP请求功能
 */
abstract class BaseService(
    protected val httpClient: HttpClient
) {

    /**
     * 执行GET请求
     */
    protected inline fun <reified T> get(
        path: String,
        params: Map<String, Any?> = emptyMap()
    ): T {
        return httpClient.get<T>(path, params)
    }

    /**
     * 执行POST请求
     */
    protected inline fun <reified T> post(
        path: String,
        body: Any? = null
    ): T {
        return httpClient.post<T>(path, body)
    }

    /**
     * 执行PUT请求
     */
    protected inline fun <reified T> put(
        path: String,
        body: Any? = null
    ): T {
        return httpClient.put<T>(path, body)
    }

    /**
     * 执行DELETE请求
     */
    protected fun delete(path: String) {
        httpClient.delete(path)
    }

    /**
     * 构建查询参数，过滤掉null值
     */
    protected fun buildParams(params: Map<String, Any?>): Map<String, Any?> {
        return params.filterValues { it != null }
    }
}
