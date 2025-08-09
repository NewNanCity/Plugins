@file:Suppress("unused")

package city.newnan.config.extensions

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * JsonNode 扩展函数
 * 
 * 提供便捷的 JsonNode 操作方法，整合了原有的 JsonObjectExtension 功能
 * 
 * @author Gk0Wk
 * @since 1.0.0
 */

/**
 * 获取指定路径的节点（支持点分隔路径）
 * 
 * @param path 节点路径，如 "user.profile.name"
 * @return 目标节点，如果路径不存在则返回 null
 */
operator fun JsonNode.get(path: String): JsonNode? {
    val pathParts = path.split(".")
    var currentNode: JsonNode = this
    
    for (part in pathParts) {
        currentNode = currentNode.get(part) ?: return null
    }
    
    return currentNode
}

/**
 * 检查指定路径是否存在
 * 
 * @param path 节点路径，如 "user.profile.name"
 * @return true 如果路径存在
 */
fun JsonNode.hasPath(path: String): Boolean {
    return this[path] != null
}

/**
 * 获取字符串值，支持路径访问
 * 
 * @param path 节点路径
 * @param default 默认值
 * @return 字符串值
 */
fun JsonNode.getString(path: String, default: String = ""): String {
    return this[path]?.asText() ?: default
}

/**
 * 获取整数值，支持路径访问
 * 
 * @param path 节点路径
 * @param default 默认值
 * @return 整数值
 */
fun JsonNode.getInt(path: String, default: Int = 0): Int {
    return this[path]?.asInt() ?: default
}

/**
 * 获取长整数值，支持路径访问
 * 
 * @param path 节点路径
 * @param default 默认值
 * @return 长整数值
 */
fun JsonNode.getLong(path: String, default: Long = 0L): Long {
    return this[path]?.asLong() ?: default
}

/**
 * 获取双精度浮点数值，支持路径访问
 * 
 * @param path 节点路径
 * @param default 默认值
 * @return 双精度浮点数值
 */
fun JsonNode.getDouble(path: String, default: Double = 0.0): Double {
    return this[path]?.asDouble() ?: default
}

/**
 * 获取布尔值，支持路径访问
 * 
 * @param path 节点路径
 * @param default 默认值
 * @return 布尔值
 */
fun JsonNode.getBoolean(path: String, default: Boolean = false): Boolean {
    return this[path]?.asBoolean() ?: default
}

/**
 * 获取列表值，支持路径访问
 * 
 * @param T 列表元素类型
 * @param path 节点路径
 * @param mapper ObjectMapper 实例
 * @param default 默认值
 * @return 列表值
 */
inline fun <reified T> JsonNode.getList(
    path: String, 
    mapper: ObjectMapper, 
    default: List<T> = emptyList()
): List<T> {
    val node = this[path] ?: return default
    if (!node.isArray) return default
    
    return try {
        val typeRef = object : TypeReference<List<T>>() {}
        mapper.convertValue(node, typeRef) ?: default
    } catch (e: Exception) {
        default
    }
}

/**
 * 获取映射值，支持路径访问
 * 
 * @param T 映射值类型
 * @param path 节点路径
 * @param mapper ObjectMapper 实例
 * @param default 默认值
 * @return 映射值
 */
inline fun <reified T> JsonNode.getMap(
    path: String, 
    mapper: ObjectMapper, 
    default: Map<String, T> = emptyMap()
): Map<String, T> {
    val node = this[path] ?: return default
    if (!node.isObject) return default
    
    return try {
        val typeRef = object : TypeReference<Map<String, T>>() {}
        mapper.convertValue(node, typeRef) ?: default
    } catch (e: Exception) {
        default
    }
}

/**
 * 获取对象值，支持路径访问
 * 
 * @param T 对象类型
 * @param path 节点路径
 * @param mapper ObjectMapper 实例
 * @param default 默认值
 * @return 对象值
 */
inline fun <reified T> JsonNode.getObject(
    path: String, 
    mapper: ObjectMapper, 
    default: T? = null
): T? {
    val node = this[path] ?: return default
    
    return try {
        val typeRef = object : TypeReference<T>() {}
        mapper.convertValue(node, typeRef) ?: default
    } catch (e: Exception) {
        default
    }
}

/**
 * ObjectNode 扩展函数
 */

/**
 * 设置指定路径的值（支持点分隔路径）
 * 
 * @param path 节点路径，如 "user.profile.name"
 * @param value 要设置的值
 * @return 当前 ObjectNode（支持链式调用）
 */
fun ObjectNode.setPath(path: String, value: Any?): ObjectNode {
    val pathParts = path.split(".")
    var currentNode = this
    
    // 导航到目标节点的父节点
    for (i in 0 until pathParts.size - 1) {
        val part = pathParts[i]
        val childNode = currentNode.get(part)
        
        currentNode = if (childNode != null && childNode.isObject) {
            childNode as ObjectNode
        } else {
            val newNode = currentNode.objectNode()
            currentNode.set<JsonNode>(part, newNode)
            newNode
        }
    }
    
    // 设置最终值
    val finalKey = pathParts.last()
    when (value) {
        null -> currentNode.putNull(finalKey)
        is String -> currentNode.put(finalKey, value)
        is Int -> currentNode.put(finalKey, value)
        is Long -> currentNode.put(finalKey, value)
        is Double -> currentNode.put(finalKey, value)
        is Float -> currentNode.put(finalKey, value)
        is Boolean -> currentNode.put(finalKey, value)
        is JsonNode -> currentNode.set<JsonNode>(finalKey, value)
        else -> {
            // 对于复杂对象，需要外部提供 ObjectMapper
            throw IllegalArgumentException("复杂对象需要使用 setPath(path, value, mapper) 方法")
        }
    }
    
    return this
}

/**
 * 设置指定路径的值（支持复杂对象）
 * 
 * @param path 节点路径，如 "user.profile.name"
 * @param value 要设置的值
 * @param mapper ObjectMapper 实例
 * @return 当前 ObjectNode（支持链式调用）
 */
fun ObjectNode.setPath(path: String, value: Any?, mapper: ObjectMapper): ObjectNode {
    val pathParts = path.split(".")
    var currentNode = this
    
    // 导航到目标节点的父节点
    for (i in 0 until pathParts.size - 1) {
        val part = pathParts[i]
        val childNode = currentNode.get(part)
        
        currentNode = if (childNode != null && childNode.isObject) {
            childNode as ObjectNode
        } else {
            val newNode = currentNode.objectNode()
            currentNode.set<JsonNode>(part, newNode)
            newNode
        }
    }
    
    // 设置最终值
    val finalKey = pathParts.last()
    when (value) {
        null -> currentNode.putNull(finalKey)
        is String -> currentNode.put(finalKey, value)
        is Int -> currentNode.put(finalKey, value)
        is Long -> currentNode.put(finalKey, value)
        is Double -> currentNode.put(finalKey, value)
        is Float -> currentNode.put(finalKey, value)
        is Boolean -> currentNode.put(finalKey, value)
        is JsonNode -> currentNode.set<JsonNode>(finalKey, value)
        else -> {
            val valueNode = mapper.valueToTree<JsonNode>(value)
            currentNode.set<JsonNode>(finalKey, valueNode)
        }
    }
    
    return this
}

/**
 * 删除指定路径的节点
 * 
 * @param path 节点路径，如 "user.profile.name"
 * @return 当前 ObjectNode（支持链式调用）
 */
fun ObjectNode.removePath(path: String): ObjectNode {
    val pathParts = path.split(".")
    var currentNode = this
    
    // 导航到目标节点的父节点
    for (i in 0 until pathParts.size - 1) {
        val part = pathParts[i]
        val childNode = currentNode.get(part)
        
        if (childNode == null || !childNode.isObject) {
            return this // 路径不存在
        }
        
        currentNode = childNode as ObjectNode
    }
    
    // 删除最终节点
    currentNode.remove(pathParts.last())
    return this
}

/**
 * 获取或创建指定路径的 ObjectNode
 * 
 * @param path 节点路径，如 "user.profile"
 * @return ObjectNode
 */
fun ObjectNode.getOrCreateObject(path: String): ObjectNode {
    val pathParts = path.split(".")
    var currentNode = this
    
    for (part in pathParts) {
        val childNode = currentNode.get(part)
        
        currentNode = if (childNode != null && childNode.isObject) {
            childNode as ObjectNode
        } else {
            val newNode = currentNode.objectNode()
            currentNode.set<JsonNode>(part, newNode)
            newNode
        }
    }
    
    return currentNode
}

/**
 * 获取或创建指定路径的 ArrayNode
 * 
 * @param path 节点路径，如 "user.hobbies"
 * @return ArrayNode
 */
fun ObjectNode.getOrCreateArray(path: String): ArrayNode {
    val pathParts = path.split(".")
    var currentNode = this
    
    // 导航到目标节点的父节点
    for (i in 0 until pathParts.size - 1) {
        val part = pathParts[i]
        val childNode = currentNode.get(part)
        
        currentNode = if (childNode != null && childNode.isObject) {
            childNode as ObjectNode
        } else {
            val newNode = currentNode.objectNode()
            currentNode.set<JsonNode>(part, newNode)
            newNode
        }
    }
    
    // 获取或创建最终的 ArrayNode
    val finalKey = pathParts.last()
    val finalNode = currentNode.get(finalKey)
    
    return if (finalNode != null && finalNode.isArray) {
        finalNode as ArrayNode
    } else {
        val newArrayNode = currentNode.arrayNode()
        currentNode.set<JsonNode>(finalKey, newArrayNode)
        newArrayNode
    }
}
