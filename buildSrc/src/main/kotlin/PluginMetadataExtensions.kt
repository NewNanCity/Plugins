import org.gradle.api.Project

/**
 * 插件加载模式
 */
enum class PluginLoadMode {
    STARTUP,
    POSTWORLD,
    ON_DEMAND
}

/**
 * 插件元数据配置
 */
data class PluginMetadata(
    var version: String,
    var group: String,
    var name: String,
    var mainClass: String,
    var apiVersion: String,
    var description: String,
    var website: String,
    var authors: List<String>,
    var prefix: String,
    var load: PluginLoadMode,
    var database: Boolean,
    var dependencies: List<String>,
    var softDependencies: List<String>,
    var loadBefore: List<String>
) {
    /**
     * 转换为插件属性Map
     */
    fun toPluginProperties(): Map<String, Any> {
        return mapOf(
            "version" to version,
            "group" to group,
            "name" to name,
            "main" to mainClass,
            "apiVersion" to apiVersion,
            "description" to description,
            "website" to website,
            "authors" to authors,
            "prefix" to prefix,
            "load" to load.name,
            "depend" to dependencies,
            "softdepend" to softDependencies,
            "loadbefore" to loadBefore
        )
    }
}

/**
 * 创建插件元数据配置
 * 
 * @param builder DSL构建器，用于自定义插件配置
 * @return 配置完成的插件属性Map
 */
fun Project.createPluginMetadata(builder: (PluginMetadata) -> Unit): Map<String, Any> {
    // 创建默认配置
    val metadata = PluginMetadata(
        version = project.version.toString(),
        group = project.group.toString(),
        name = project.name,
        mainClass = "${project.group}.${project.name}Plugin",
        apiVersion = Versions.apiVersion,
        description = project.description ?: "A NewNan city plugin",
        website = "https://newnan.city",
        authors = listOf("NewNanCity"),
        prefix = project.name,
        load = PluginLoadMode.POSTWORLD,
        database = false,
        dependencies = emptyList(),
        softDependencies = emptyList(),
        loadBefore = emptyList()
    )
    
    // 应用用户自定义配置
    metadata.apply(builder)
    
    return metadata.toPluginProperties()
}