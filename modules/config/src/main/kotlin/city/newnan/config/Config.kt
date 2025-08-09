@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package city.newnan.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

/**
 * 配置文件对象
 *
 * 提供类型安全的配置操作，支持链式调用和 JsonNode 操作
 *
 * @param file 配置文件
 * @param configPath 配置文件路径（相对于插件数据目录）
 * @param format 配置格式
 * @param node Jackson ObjectNode
 * @param manager 配置管理器实例
 * @author Gk0Wk
 * @since 1.0.0
 */
class Config(
    /**
     * 配置文件
     */
    val file: File,

    /**
     * 配置文件路径（相对于插件数据目录）
     */
    val configPath: String,

    /**
     * 配置格式
     */
    val format: String,

    /**
     * Jackson ObjectNode
     */
    val node: ObjectNode,

    /**
     * 配置管理器实例
     */
    val manager: ConfigManager
) {
    
    companion object {
        private const val PATH_SEPARATOR = "."
        private const val ESCAPE_CHAR = "\\"
        
        /**
         * 解析支持转义的路径
         * 例如: "user\\.name.password" -> ["user.name", "password"]
         */
        private fun parsePath(path: String): List<String> {
            val parts = mutableListOf<String>()
            val currentPart = StringBuilder()
            var escaped = false
            
            for (char in path) {
                when {
                    escaped -> {
                        currentPart.append(char)
                        escaped = false
                    }
                    char == ESCAPE_CHAR[0] -> {
                        escaped = true
                    }
                    char == PATH_SEPARATOR[0] -> {
                        if (currentPart.isNotEmpty()) {
                            parts.add(currentPart.toString())
                            currentPart.clear()
                        }
                    }
                    else -> {
                        currentPart.append(char)
                    }
                }
            }
            
            if (currentPart.isNotEmpty()) {
                parts.add(currentPart.toString())
            }
            
            return parts
        }
    }

    /**
     * 获取根节点并执行操作
     *
     * @param block 操作块
     * @return 当前配置实例（支持链式调用）
     */
    fun root(block: ObjectNode.() -> Unit): Config {
        node.block()
        return this
    }

    /**
     * 获取指定路径的节点并执行操作
     *
     * @param path 节点路径（支持点分隔）
     * @param block 操作块
     * @return 当前配置实例（支持链式调用）
     */
    fun path(path: String, block: JsonNode.() -> Unit): Config {
        val targetNode = getNodeByPath(path)
        targetNode?.block()
        return this
    }

    /**
     * 获取指定路径的 ObjectNode 并执行操作
     *
     * @param path 节点路径（支持点分隔）
     * @param block 操作块
     * @return 当前配置实例（支持链式调用）
     */
    fun objectPath(path: String, block: ObjectNode.() -> Unit): Config {
        val targetNode = getOrCreateObjectNode(path)
        targetNode.block()
        return this
    }

    /**
     * 获取指定路径的 ArrayNode 并执行操作
     *
     * @param path 节点路径（支持点分隔）
     * @param block 操作块
     * @return 当前配置实例（支持链式调用）
     */
    fun arrayPath(path: String, block: ArrayNode.() -> Unit): Config {
        val targetNode = getOrCreateArrayNode(path)
        targetNode.block()
        return this
    }

    /**
     * 获取配置值
     *
     * @param T 值类型
     * @param path 配置路径（支持点分隔）
     * @param default 默认值
     * @return 配置值
     */
    inline fun <reified T> get(path: String, default: T): T {
        val targetNode = getNodeByPathPublic(path) ?: return default

        return try {
            when (T::class) {
                String::class -> (targetNode.asText() ?: default) as T
                Int::class -> targetNode.asInt(default as Int) as T
                Long::class -> targetNode.asLong(default as Long) as T
                Double::class -> targetNode.asDouble(default as Double) as T
                Float::class -> targetNode.asDouble(default as Double).toFloat() as T
                Boolean::class -> targetNode.asBoolean(default as Boolean) as T
                else -> {
                    val mapper = manager.getMapper(format)
                    val typeRef = object : TypeReference<T>() {}
                    mapper.convertValue(targetNode, typeRef) ?: default
                }
            }
        } catch (e: Exception) {
            default
        }
    }

    /**
     * 获取配置值（可能为 null）
     *
     * @param T 值类型
     * @param path 配置路径（支持点分隔）
     * @return 配置值或 null
     */
    inline fun <reified T> getOrNull(path: String): T? {
        val targetNode = getNodeByPathPublic(path) ?: return null

        return try {
            when (T::class) {
                String::class -> targetNode.asText() as? T
                Int::class -> targetNode.asInt() as? T
                Long::class -> targetNode.asLong() as? T
                Double::class -> targetNode.asDouble() as? T
                Float::class -> targetNode.asDouble().toFloat() as? T
                Boolean::class -> targetNode.asBoolean() as? T
                else -> {
                    val mapper = manager.getMapper(format)
                    val typeRef = object : TypeReference<T>() {}
                    mapper.convertValue(targetNode, typeRef)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置配置值
     *
     * @param path 配置路径（支持点分隔和转义）
     * @param value 配置值
     * @return 当前配置实例（支持链式调用）
     */
    fun set(path: String, value: Any?): Config {
        val pathParts = parsePath(path)
        var currentNode = node

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
            else -> {
                val mapper = manager.getMapper(format)
                val valueNode = mapper.valueToTree<JsonNode>(value)
                currentNode.set<JsonNode>(finalKey, valueNode)
            }
        }

        return this
    }

    /**
     * 检查路径是否存在
     *
     * @param path 配置路径（支持点分隔）
     * @return true 如果路径存在
     */
    fun has(path: String): Boolean {
        return getNodeByPathPublic(path) != null
    }

    /**
     * 删除指定路径的配置
     *
     * @param path 配置路径（支持点分隔和转义）
     * @return 当前配置实例（支持链式调用）
     */
    fun remove(path: String): Config {
        val pathParts = parsePath(path)
        var currentNode = node

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
     * 保存配置文件
     *
     * @param block 保存前的修改操作
     * @return 当前配置实例（支持链式调用）
     */
    fun save(block: (ObjectNode.() -> Unit)? = null): Config {
        block?.invoke(node)
        manager.save(this, configPath, format)
        return this
    }

    /**
     * 重新加载配置文件
     *
     * @return 新的配置实例
     */
    fun reload(): Config {
        return manager.get(configPath, format, useCache = false)
    }

    /**
     * 克隆配置到新文件
     *
     * @param newConfigPath 新配置文件路径
     * @param newFormat 新格式，null 则使用当前格式
     * @return 新的配置实例
     */
    fun clone(newConfigPath: String, newFormat: String? = null): Config {
        val targetFormat = newFormat ?: format
        val newNode = node.deepCopy()
        val newFile = File(manager.plugin.dataFolder, newConfigPath)

        val newConfig = Config(
            file = newFile,
            configPath = newConfigPath,
            format = targetFormat,
            node = newNode,
            manager = manager
        )

        newConfig.save()
        return newConfig
    }

    /**
     * 转换为字符串
     *
     * @param targetFormat 目标格式，null 则使用当前格式
     * @return 配置字符串
     */
    fun toString(targetFormat: String? = null): String {
        val mapper = manager.getMapper(targetFormat ?: format)
        return mapper.writeValueAsString(node)
    }

    /**
     * 根据路径获取节点（公共方法）
     */
    fun getNodeByPathPublic(path: String): JsonNode? {
        return getNodeByPath(path)
    }

    /**
     * 根据路径获取节点
     */
    internal fun getNodeByPath(path: String): JsonNode? {
        val pathParts = parsePath(path)
        var currentNode: JsonNode = node

        for (part in pathParts) {
            currentNode = currentNode.get(part) ?: return null
        }

        return currentNode
    }

    /**
     * 获取或创建 ObjectNode
     */
    internal fun getOrCreateObjectNode(path: String): ObjectNode {
        val pathParts = parsePath(path)
        var currentNode = node

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
     * 获取或创建 ArrayNode
     */
    internal fun getOrCreateArrayNode(path: String): ArrayNode {
        val pathParts = parsePath(path)
        var currentNode = node

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
}
