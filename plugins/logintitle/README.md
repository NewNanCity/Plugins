# LoginTitle 插件

这是一个最小的Minecraft插件示例，展示了如何使用NewNan插件框架的最佳实践。

## 功能

- 当玩家登录服务器时，自动显示欢迎标题
- 标题持续显示10秒钟
- 使用现代化的Adventure API显示标题

## 特点

- **最小化设计**：不使用配置文件和国际化系统
- **遵循最佳实践**：使用BasePlugin和BaseModule架构
- **简单易懂**：代码结构清晰，适合学习参考
- **现代化**：使用Adventure Component API和Title API

## 自定义标题内容

要修改标题内容，请编辑 `LoginTitleModule.kt` 文件中的以下变量：

```kotlin
// 标题内容 - 用户可以在这里修改标题内容
private val titleText = "欢迎来到服务器!"
private val subtitleText = "祝您游戏愉快"
```

## 构建

```bash
./gradlew :plugins:logintitle:shadowJar
```

生成的JAR文件位于：`plugins/logintitle/build/libs/LoginTitle-1.0.0.jar`

## 安装

1. 将生成的JAR文件复制到服务器的 `plugins` 目录
2. 重启服务器或使用插件管理器重载插件

## 依赖

- Paper 1.20.1+
- Java 17+

## 架构说明

这个插件展示了NewNan插件框架的核心架构：

- **LoginTitlePlugin**：主插件类，继承BasePlugin
- **LoginTitleModule**：功能模块，继承BaseModule，处理玩家登录事件
- **自动资源管理**：所有事件监听器和任务都会自动清理
- **硬编码配置**：getCoreConfig()返回硬编码的配置对象

## 许可证

本插件仅作为学习示例使用。
