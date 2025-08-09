package city.newnan.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * HTTP 请求 DSL 扩展
 *
 * 提供简洁、类型安全的 HTTP 请求 API
 */

/**
 * GET 请求扩展
 */
suspend inline fun <reified T> HttpClient.getTyped(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = get(urlString, block).body()

suspend inline fun <reified T> HttpClient.getTyped(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = get(url, block).body()

/**
 * POST 请求扩展
 */
suspend inline fun <reified T> HttpClient.postTyped(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = post(urlString, block).body()

suspend inline fun <reified T> HttpClient.postTyped(
    url: Url,
    block: HttpRequestBuilder.() -> Unit = {}
): T = post(url, block).body()

/**
 * PUT 请求扩展
 */
suspend inline fun <reified T> HttpClient.putTyped(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = put(urlString, block).body()

/**
 * DELETE 请求扩展
 */
suspend inline fun <reified T> HttpClient.deleteTyped(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = delete(urlString, block).body()

/**
 * PATCH 请求扩展
 */
suspend inline fun <reified T> HttpClient.patchTyped(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): T = patch(urlString, block).body()

/**
 * JSON 请求体设置扩展
 */
fun HttpRequestBuilder.jsonBody(body: Any) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

/**
 * 表单数据设置扩展
 */
fun HttpRequestBuilder.formData(vararg pairs: Pair<String, String>) {
    contentType(ContentType.Application.FormUrlEncoded)
    val parameters = Parameters.build {
        pairs.forEach { (key, value) ->
            append(key, value)
        }
    }
    setBody(parameters.formUrlEncode())
}

/**
 * 查询参数设置扩展
 */
fun HttpRequestBuilder.queryParams(vararg pairs: Pair<String, String>) {
    url {
        pairs.forEach { (key, value) ->
            parameters.append(key, value)
        }
    }
}

fun HttpRequestBuilder.queryParams(params: Map<String, String>) {
    url {
        params.forEach { (key, value) ->
            parameters.append(key, value)
        }
    }
}

/**
 * 认证头设置扩展
 */
fun HttpRequestBuilder.bearerAuth(token: String) {
    header(HttpHeaders.Authorization, "Bearer $token")
}

fun HttpRequestBuilder.basicAuth(username: String, password: String) {
    header(HttpHeaders.Authorization, "Basic ${"$username:$password".toByteArray().encodeBase64()}")
}

fun HttpRequestBuilder.apiKey(key: String, headerName: String = "X-API-Key") {
    header(headerName, key)
}

/**
 * 流式请求扩展
 */
suspend fun HttpClient.getStream(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {}
): Flow<ByteArray> = flow {
    val response = get(urlString, block)
    val bytes = response.readBytes()
    emit(bytes)
}

/**
 * 批量请求扩展
 */
suspend fun <T> HttpClient.batchRequests(
    requests: List<suspend HttpClient.() -> T>
): List<T> {
    return requests.map { request ->
        request()
    }
}

/**
 * 重试请求扩展
 */
suspend fun <T> HttpClient.requestWithRetry(
    maxRetries: Int = 3,
    delayMillis: Long = 1000,
    request: suspend HttpClient.() -> T
): T {
    var lastException: Exception? = null

    repeat(maxRetries) { attempt ->
        try {
            return request()
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                kotlinx.coroutines.delay(delayMillis * (attempt + 1))
            }
        }
    }

    throw lastException ?: RuntimeException("请求失败，已重试 $maxRetries 次")
}

/**
 * 响应处理扩展
 */
suspend inline fun <reified T> HttpResponse.bodyOrNull(): T? {
    return try {
        body<T>()
    } catch (e: Exception) {
        null
    }
}

suspend fun HttpResponse.isSuccessful(): Boolean {
    return status.isSuccess()
}

suspend fun HttpResponse.errorMessage(): String? {
    return if (!status.isSuccess()) {
        "HTTP ${status.value}: ${status.description}"
    } else null
}

/**
 * URL 构建扩展
 */
fun URLBuilder.pathSegments(vararg segments: String) {
    segments.forEach { segment ->
        appendPathSegments(segment)
    }
}

fun URLBuilder.pathSegments(segments: List<String>) {
    appendPathSegments(*segments.toTypedArray())
}

/**
 * 常用的响应数据类
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val code: Int? = null
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasNext: Boolean
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val code: Int? = null,
    val details: Map<String, String>? = null
)

/**
 * 常用的 Content-Type 常量
 */
object ContentTypes {
    val JSON = ContentType.Application.Json
    val XML = ContentType.Application.Xml
    val FORM = ContentType.Application.FormUrlEncoded
    val MULTIPART = ContentType.MultiPart.FormData
    val TEXT = ContentType.Text.Plain
    val HTML = ContentType.Text.Html
}

/**
 * HTTP 状态码常量
 */
object HttpStatusCodes {
    const val OK = 200
    const val CREATED = 201
    const val ACCEPTED = 202
    const val NO_CONTENT = 204
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val METHOD_NOT_ALLOWED = 405
    const val CONFLICT = 409
    const val INTERNAL_SERVER_ERROR = 500
    const val BAD_GATEWAY = 502
    const val SERVICE_UNAVAILABLE = 503
}
