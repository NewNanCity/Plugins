package city.newnan.guardian.sdk.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import city.newnan.guardian.sdk.exceptions.*
import city.newnan.guardian.sdk.models.ErrorResponse
import city.newnan.guardian.sdk.models.ServerSessionResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP客户端基类，提供同步HTTP请求功能
 */
class HttpClient(
    baseUrl: String,
    token: String,
    timeout: Long = 30L
) : AutoCloseable {

    internal val baseUrl = baseUrl
    internal val token = token
    internal val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Volatile
    private var serverSession: ServerSessionResponse? = null

    internal val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .build()

    companion object {
        internal val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    internal fun setServerSession(session: ServerSessionResponse) {
        require(session.sessionId.length in 32..64) { "Invalid NewNanManager session ID" }
        require(session.sessionEpoch > 0) { "Invalid NewNanManager session epoch" }
        serverSession = session
    }

    private fun authenticatedBuilder(url: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .addHeader("X-API-Token", token)
        serverSession?.let {
            builder.addHeader("X-NNM-Session-ID", it.sessionId)
                .addHeader("X-NNM-Session-Epoch", it.sessionEpoch.toString())
        }
        return builder
    }

    /**
     * 执行GET请求
     */
    fun <T> get(
        path: String,
        params: Map<String, Any?> = emptyMap(),
        responseType: Class<T>
    ): T {
        var url = "$baseUrl$path"

        // 添加查询参数
        if (params.isNotEmpty()) {
            val queryParams = params.filterValues { it != null }
                .map { "${it.key}=${it.value}" }
                .joinToString("&")
            url += if (url.contains("?")) "&$queryParams" else "?$queryParams"
        }

        val request = authenticatedBuilder(url)
            .get()
            .build()

        return executeRequest(request, responseType)
    }

    /**
     * 执行GET请求（reified版本）
     */
    inline fun <reified T> get(
        path: String,
        params: Map<String, Any?> = emptyMap()
    ): T {
        return get(path, params, T::class.java)
    }

    /**
     * 执行POST请求
     */
    fun <T> post(
        path: String,
        body: Any? = null,
        responseType: Class<T>
    ): T {
        val requestBody = if (body != null) {
            objectMapper.writeValueAsString(body).toRequestBody(JSON_MEDIA_TYPE)
        } else {
            "".toRequestBody(JSON_MEDIA_TYPE)
        }

        val request = authenticatedBuilder("$baseUrl$path")
            .post(requestBody)
            .build()

        return executeRequest(request, responseType)
    }

    /**
     * 执行POST请求（reified版本）
     */
    inline fun <reified T> post(
        path: String,
        body: Any? = null
    ): T {
        return post(path, body, T::class.java)
    }

    /**
     * 执行PUT请求
     */
    fun <T> put(
        path: String,
        body: Any? = null,
        responseType: Class<T>
    ): T {
        val requestBody = if (body != null) {
            objectMapper.writeValueAsString(body).toRequestBody(JSON_MEDIA_TYPE)
        } else {
            "".toRequestBody(JSON_MEDIA_TYPE)
        }

        val request = authenticatedBuilder("$baseUrl$path")
            .put(requestBody)
            .build()

        return executeRequest(request, responseType)
    }

    /**
     * 执行PUT请求（reified版本）
     */
    inline fun <reified T> put(
        path: String,
        body: Any? = null
    ): T {
        return put(path, body, T::class.java)
    }

    /**
     * 执行DELETE请求
     */
    fun delete(path: String) {
        val request = authenticatedBuilder("$baseUrl$path")
            .delete()
            .build()

        executeRequest(request, Unit::class.java)
    }

    /**
     * 执行HTTP请求并处理响应
     */
    fun <T> executeRequest(request: Request, responseType: Class<T>): T {
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    // 尝试解析错误响应
                    try {
                        val errorResponse = objectMapper.readValue(responseBody, ErrorResponse::class.java)
                        throw ApiException(errorResponse.detail)
                    } catch (e: ApiException) {
                        throw e
                    } catch (e: Exception) {
                        throw HttpException(response.code, responseBody)
                    }
                }

                // 处理空响应
                if (responseType == Unit::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return Unit as T
                }

                // 解析JSON响应
                try {
                    return objectMapper.readValue(responseBody, responseType)
                } catch (e: Exception) {
                    throw JsonParseException("Failed to parse response: $responseBody", e)
                }
            }
        } catch (e: IOException) {
            throw NetworkException("Network request failed", e)
        }
    }

    /**
     * 执行HTTP请求并处理响应（reified版本）
     */
    inline fun <reified T> executeRequest(request: Request): T {
        return executeRequest(request, T::class.java)
    }

    override fun close() {
        // OkHttpClient会自动管理连接池，无需手动关闭
    }
}
