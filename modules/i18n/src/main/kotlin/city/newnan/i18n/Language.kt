package city.newnan.i18n

import city.newnan.i18n.exceptions.LanguageFileFormatException
import city.newnan.i18n.exceptions.LanguageReloadException
import city.newnan.config.ConfigManager
import com.fasterxml.jackson.databind.JsonNode
import java.io.File
import java.util.*

/**
 * 单语言文件封装类
 *
 * 负责加载、解析和管理单个语言文件的内容
 *
 * @param locale 语言区域
 * @param getObjectNode 获取ObjectNode的函数
 * @author Gk0Wk
 * @since 1.0.0
 */
class Language(
    /**
     * 语言区域
     */
    val locale: Locale,

    /**
     * 获取配置对象的函数
     */
    private val getConfig: () -> city.newnan.config.Config
) {

    /**
     * 语言节点缓存
     */
    private val languageNodes = mutableMapOf<String, String>()

    /**
     * 配置文件路径（相对于插件数据目录）
     */
    var configPath: String? = null
        private set

    /**
     * 从文件构造Language实例
     *
     * @param locale 语言区域
     * @param languageFile 语言文件
     * @param configManager 配置管理器
     */
    constructor(locale: Locale, languageFile: File, configManager: ConfigManager) : this(
        locale,
        {
            try {
                // 计算相对于插件数据目录的路径
                val pluginDataFolder = configManager.plugin.dataFolder
                val relativePath = pluginDataFolder.toPath().relativize(languageFile.toPath()).toString().replace('\\', '/')
                configManager.get(relativePath, useCache = false)
            } catch (e: Exception) {
                throw LanguageFileFormatException(languageFile.absolutePath, e)
            }
        }
    ) {
        // 保存配置文件路径
        try {
            val pluginDataFolder = configManager.plugin.dataFolder
            this.configPath = pluginDataFolder.toPath().relativize(languageFile.toPath()).toString().replace('\\', '/')
        } catch (e: Exception) {
            // 如果无法计算相对路径，使用绝对路径
            this.configPath = languageFile.absolutePath
        }
    }

    init {
        reload()
    }

    /**
     * 重新加载语言文件
     *
     * @throws LanguageReloadException 重载失败时抛出
     */
    fun reload() {
        try {
            languageNodes.clear()
            val config = getConfig()
            // 使用预定义的路径列表来获取配置值
            loadKnownPaths(config)
        } catch (e: Exception) {
            throw LanguageReloadException(locale.toString(), e)
        }
    }

    /**
     * 递归加载所有配置路径
     * 遍历整个配置树，提取所有字符串值
     */
    private fun loadKnownPaths(config: city.newnan.config.Config) {
        // 递归遍历整个配置树
        loadAllPaths(config.node, "")
    }

    /**
     * 递归加载所有路径
     *
     * @param node 当前节点
     * @param prefix 路径前缀
     */
    private fun loadAllPaths(node: JsonNode, prefix: String) {
        when {
            node.isObject -> {
                // 对象节点：遍历所有字段
                val fieldNames = node.fieldNames()
                while (fieldNames.hasNext()) {
                    val fieldName = fieldNames.next()
                    val childNode = node.get(fieldName)
                    val currentPath = if (prefix.isEmpty()) fieldName else "$prefix.$fieldName"

                    // 递归处理子节点
                    loadAllPaths(childNode, currentPath)
                }
            }
            node.isArray -> {
                // 数组节点：遍历所有元素（使用索引作为键）
                for (i in 0 until node.size()) {
                    val childNode = node.get(i)
                    val currentPath = if (prefix.isEmpty()) i.toString() else "$prefix.$i"

                    // 递归处理数组元素
                    loadAllPaths(childNode, currentPath)
                }
            }
            node.isTextual -> {
                // 文本节点：保存到语言节点映射
                languageNodes[prefix] = node.asText()
            }
            node.isNumber -> {
                // 数字节点：转换为字符串保存
                languageNodes[prefix] = node.asText()
            }
            node.isBoolean -> {
                // 布尔节点：转换为字符串保存
                languageNodes[prefix] = node.asText()
            }
            // 其他类型（null等）忽略
        }
    }

    /**
     * 获取指定路径的字符串值
     *
     * @param path 节点路径（使用点分隔）
     * @return 对应的字符串值，如果不存在则返回null
     */
    fun getNodeString(path: String): String? {
        return languageNodes[path]
    }

    /**
     * 检查是否包含指定路径
     *
     * @param path 节点路径
     * @return 是否包含该路径
     */
    fun containsPath(path: String): Boolean {
        return languageNodes.containsKey(path)
    }

    /**
     * 获取所有可用的路径
     *
     * @return 所有路径的集合
     */
    fun getAllPaths(): Set<String> {
        return languageNodes.keys.toSet()
    }

    /**
     * 获取语言节点数量
     *
     * @return 节点数量
     */
    fun getNodeCount(): Int {
        return languageNodes.size
    }

    /**
     * 清空所有语言节点
     */
    fun clear() {
        languageNodes.clear()
    }

    override fun toString(): String {
        return "Language(locale=$locale, nodeCount=${getNodeCount()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Language) return false
        return locale == other.locale
    }

    override fun hashCode(): Int {
        return locale.hashCode()
    }


}
