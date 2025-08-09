# createPluginMetadata 使用指南

`createPluginMetadata` 是在 `buildSrc/src/main/kotlin/PluginMetadataExtensions.kt` 中定义的便捷函数，用于简化插件元数据配置。

## 基本用法

在插件的 `build.gradle.kts` 文件中使用：

```kotlin
// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        name = "YourPluginName"
        mainClass = "city.newnan.yourplugin.YourPluginPlugin"
        description = "Your plugin description"
        authors = listOf("Author1", "Author2")
        dependencies = listOf("CommandAPI", "Vault")
        softDependencies = listOf("WorldEdit", "PlaceholderAPI")
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
```

## 默认值

如果不指定某些属性，函数会提供智能的默认值：

- `version`: 从 `project.version` 获取
- `group`: 从 `project.group` 获取  
- `name`: 默认为 `project.name`
- `mainClass`: 自动生成为 `city.newnan.{lowercase-project-name}.{PascalCase-Project-Name}Plugin`
- `apiVersion`: 从 `Versions.apiVersion` 获取
- `description`: 从 `project.description` 获取，如果为空则使用 "A NewNan city plugin"
- `website`: 默认为 "https://newnan.city"
- `authors`: 默认为 `listOf("NewNanCity")`
- `prefix`: 自动生成为 PascalCase 格式的项目名
- `load`: 默认为 `PluginLoadMode.POSTWORLD`
- `database`: 默认为 `false`
- `dependencies`: 默认为空列表
- `softDependencies`: 默认为空列表
- `loadBefore`: 默认为空列表

## 完整示例

### Foundation 插件示例
```kotlin
// plugins/foundation/build.gradle.kts
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        name = "Foundation"
        mainClass = "city.newnan.foundation.FoundationPlugin"
        description = "Modern foundation plugin for NewNan city economic management"
        authors = listOf("Sttot", "NSrank", "AI")
        prefix = "Foundation"
        load = PluginLoadMode.STARTUP
        dependencies = listOf("CommandAPI", "Vault")
        softDependencies = listOf("Essentials", "XConomy")
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
```

### External-Book 插件示例
```kotlin
// plugins/external-book/build.gradle.kts
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        name = "ExternalBook"
        mainClass = "city.newnan.externalbook.ExternalBookPlugin"
        authors = listOf("NewNanCity", "AI")
        prefix = "ExternalBook"
        load = PluginLoadMode.POSTWORLD
        dependencies = listOf("CommandAPI")
        database = true
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
```

### 简单插件示例
```kotlin
// 对于简单插件，只需要指定必要的属性
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        name = "SimplePlugin"
        dependencies = listOf("CommandAPI")
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
```

## 加载模式

可用的加载模式：
- `PluginLoadMode.STARTUP` - 服务器启动时加载
- `PluginLoadMode.POSTWORLD` - 世界加载后加载（默认）
- `PluginLoadMode.ON_DEMAND` - 按需加载

## 与旧方式的对比

### 旧方式（手动配置）
```kotlin
tasks.processResources {
    val pluginProperties = mapOf(
        "version" to project.version,
        "group" to project.group,
        "name" to "MyPlugin",
        "main" to "city.newnan.myplugin.MyPluginPlugin",
        "apiVersion" to "1.20",
        "description" to project.description,
        "website" to "https://newnan.city",
        "authors" to listOf("Author"),
        "prefix" to "MyPlugin",
        "load" to "POSTWORLD",
        "depend" to listOf("CommandAPI"),
        "softdepend" to emptyList<String>(),
        "loadbefore" to emptyList<String>()
    )

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
```

### 新方式（使用 createPluginMetadata）
```kotlin
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        name = "MyPlugin"
        dependencies = listOf("CommandAPI")
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
```

## 优势

1. **简洁性**: 大大减少了样板代码
2. **智能默认值**: 自动生成合理的默认配置
3. **类型安全**: 使用枚举和数据类提供类型安全
4. **一致性**: 确保所有插件使用相同的配置模式
5. **易维护**: 集中管理插件元数据配置逻辑

## 注意事项

- 确保 `mainClass` 路径与实际的插件主类路径匹配
- `dependencies` 中列出的插件必须在服务器中可用
- `softDependencies` 中的插件可选，插件会在这些插件加载后加载（如果存在）
- 项目名中的连字符会被自动处理以生成合法的包名和类名