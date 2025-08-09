package city.newnan.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * 网络工具类
 *
 * 提供常用的网络操作和检测功能
 */
object NetworkUtils {

    /**
     * 检查网络连接性
     */
    suspend fun isNetworkAvailable(
        host: String = "8.8.8.8",
        timeout: Long = 5000
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(host)
            address.isReachable(timeout.toInt())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查 HTTP 服务可用性
     */
    suspend fun isHttpServiceAvailable(
        client: HttpClient,
        url: String,
        expectedStatus: HttpStatusCode = HttpStatusCode.OK
    ): Boolean {
        return try {
            val response = client.head(url)
            response.status == expectedStatus
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 测量网络延迟
     */
    suspend fun measureLatency(
        client: HttpClient,
        url: String,
        attempts: Int = 3
    ): NetworkLatency {
        val latencies = mutableListOf<Long>()
        var failures = 0

        repeat(attempts) {
            try {
                val startTime = System.currentTimeMillis()
                client.head(url)
                val endTime = System.currentTimeMillis()
                latencies.add(endTime - startTime)
            } catch (e: Exception) {
                failures++
            }
        }

        return if (latencies.isNotEmpty()) {
            NetworkLatency(
                average = latencies.average(),
                min = latencies.minOrNull() ?: 0L,
                max = latencies.maxOrNull() ?: 0L,
                attempts = attempts,
                failures = failures,
                successRate = (attempts - failures).toDouble() / attempts
            )
        } else {
            NetworkLatency(
                average = Double.MAX_VALUE,
                min = Long.MAX_VALUE,
                max = Long.MAX_VALUE,
                attempts = attempts,
                failures = failures,
                successRate = 0.0
            )
        }
    }

    /**
     * 下载文件到字节数组
     */
    suspend fun downloadToByteArray(
        client: HttpClient,
        url: String,
        maxSize: Long = 10 * 1024 * 1024 // 10MB 默认限制
    ): ByteArray {
        val response = client.get(url)
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()

        if (contentLength != null && contentLength > maxSize) {
            throw IllegalArgumentException("文件大小 ($contentLength bytes) 超过限制 ($maxSize bytes)")
        }

        return response.readBytes()
    }

    /**
     * 获取 URL 的响应头信息
     */
    suspend fun getHeaders(
        client: HttpClient,
        url: String
    ): Headers {
        val response = client.head(url)
        return response.headers
    }
    /**
     * 检查 URL 是否有效
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URI(url).toURL()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 解析 URL 组件
     */
    fun parseUrl(url: String): UrlComponents? {
        return try {
            val urlObj = URI(url)
            UrlComponents(
                protocol = urlObj.scheme ?: "http",
                host = urlObj.host ?: "localhost",
                port = if (urlObj.port == -1) null else urlObj.port,
                path = urlObj.path ?: "",
                query = urlObj.query,
                fragment = urlObj.fragment
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 构建查询字符串
     */
    fun buildQueryString(params: Map<String, String>): String {
        return params.entries.joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
    }

    /**
     * 解析查询字符串
     */
    fun parseQueryString(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()

        return query.split("&").mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0].decodeURLPart() to parts[1].decodeURLPart()
            } else null
        }.toMap()
    }

    /**
     * 获取 MIME 类型
     */
    fun getMimeType(filename: String): ContentType? {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "json" -> ContentType.Application.Json
            "xml" -> ContentType.Application.Xml
            "html", "htm" -> ContentType.Text.Html
            "txt" -> ContentType.Text.Plain
            "css" -> ContentType.Text.CSS
            "js" -> ContentType.Text.JavaScript
            "png" -> ContentType.Image.PNG
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            "svg" -> ContentType.Image.SVG
            "pdf" -> ContentType.Application.Pdf
            "zip" -> ContentType.Application.Zip
            "mp4" -> ContentType.Video.MP4
            "mp3" -> ContentType.Audio.MPEG
            else -> null
        }
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * 格式化持续时间
     */
    fun formatDuration(millis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis)

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}

/**
 * 网络延迟测量结果
 */
data class NetworkLatency(
    val average: Double,
    val min: Long,
    val max: Long,
    val attempts: Int,
    val failures: Int,
    val successRate: Double
) {
    val isHealthy: Boolean
        get() = successRate >= 0.8 && average < 1000 // 80% 成功率且平均延迟小于 1 秒
}

/**
 * URL 组件
 */
data class UrlComponents(
    val protocol: String,
    val host: String,
    val port: Int?,
    val path: String,
    val query: String?,
    val fragment: String?
) {
    fun toUrl(): String {
        val sb = StringBuilder()
        sb.append(protocol).append("://").append(host)

        port?.let { sb.append(":").append(it) }
        sb.append(path)
        query?.let { sb.append("?").append(it) }
        fragment?.let { sb.append("#").append(it) }

        return sb.toString()
    }
}

/**
 * 网络状态
 */
enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED,
    LIMITED,
    UNKNOWN
}

/**
 * 连接类型
 */
enum class ConnectionType {
    WIFI,
    ETHERNET,
    MOBILE,
    UNKNOWN
}
