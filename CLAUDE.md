# CLAUDE.md

面向 AI 编码助手的项目索引。人类可读总览见 [README.md](README.md)，目录地图见 [project-tree.md](project-tree.md)。

## 项目简介

Kotlin + Gradle 多模块 Minecraft 插件生态系统，面向 Paper 1.20.1。
技术栈：Kotlin 2.2.0 / Java 21 编译 → Java 17 字节码 / Cloud 2.x 命令框架 / Jackson 配置 / Adventure API。

## 目录结构

```
buildSrc/          # Versions.kt / Dependencies.kt / newnancity-plugin 约定
modules/core/      # BasePlugin + Terminable + 事件 + 调度器 + 消息（必需）
modules/config/    # Jackson 多格式配置
modules/gui/       # 现代 GUI 框架（Session/Page/Component）
modules/i18n/      # 多语言模板
modules/network/   # Ktor HTTP 客户端
plugins/           # 15 个独立插件（详见 settings.gradle.kts）
docs/              # 完整文档中心
docs/framework-next/ # 下一代框架干净重写设计包
```

## 构建命令

```bash
./gradlew buildAllPlugins          # 构建全部（含 shadowJar）
./gradlew shadowJarAll             # 仅打包
./gradlew :plugins:<name>:build    # 构建单个插件
./gradlew :plugins:<name>:runServer # 启动测试服务器
./gradlew cleanAll                 # 清理全部
```

注意：测试已全局禁用（见 build.gradle.kts）。

## 核心架构要点

- **四层单向依赖**：基础层 → 逻辑层 → 事务层 → 主插件类，详见 [docs/core/architecture.md](docs/core/architecture.md)
- **资源管理**：所有资源通过 `bind()` / Terminable 自动清理，30 秒回收周期
- **Manager 模式**：`by lazy { Manager(this).also { bind(it) } }`，禁止 lateinit
- **⚠️ BaseModule v2.0**：子类必须在 init 块手动调用 `init()`，详见 [docs/core/base-module.md](docs/core/base-module.md)

## 编码规范要点

- **命令系统**：Cloud 注解驱动 + LanguageKeys i18n，详见 [docs/core/commands.md](docs/core/commands.md)
- **配置管理**：`touchWithMerge` + `parse<T>` 模式，`reloadPlugin()` 先 `clearCache`，详见 [docs/development/plugin-conventions.md](docs/development/plugin-conventions.md)
- **国际化**：语言设置前用英文日志；`{0}` StringFormat 语法；`<%key%>` 模板；五层分类，详见 [docs/i18n/README.md](docs/i18n/README.md)
- **GUI**：原生 i18n 集成，`name("<%key%>")`，详见 [docs/gui/README.md](docs/gui/README.md)
- **构建约定**：`Versions.kt → Dependencies.kt → build.gradle.kts`，详见 [docs/development/build-system.md](docs/development/build-system.md)

## 文档导航

| 类别      | 路径                                                                           | 说明                           |
| --------- | ------------------------------------------------------------------------------ | ------------------------------ |
| 文档中心  | [docs/README.md](docs/README.md)                                               | 总入口与模块导航               |
| Framework Next | [docs/framework-next/](docs/framework-next/README.md)                     | 下一代框架干净重写设计事实源   |
| Core 框架 | [docs/core/](docs/core/README.md)                                              | BasePlugin、事件、调度器、消息 |
| 命令系统  | [docs/core/commands.md](docs/core/commands.md)                                 | Cloud 框架详解                 |
| 最佳实践  | [docs/core/best-practices.md](docs/core/best-practices.md)                     | 四层架构、BaseModule 规范      |
| 开发指南  | [docs/development/](docs/development/README.md)                                | 插件规范、构建、迁移、质量清单 |
| 配置模块  | [docs/config/](docs/config/README.md)                                          | Jackson 多格式配置             |
| GUI 模块  | [docs/gui/](docs/gui/README.md)                                                | 现代 GUI 系统                  |
| 国际化    | [docs/i18n/](docs/i18n/README.md)                                              | 多语言模板系统                 |
| 网络模块  | [docs/network/](docs/network/README.md)                                        | Ktor HTTP 客户端               |
| 数据库    | [docs/database/](docs/database/README.md)                                      | HikariCP + Exposed ORM         |
| 迁移记录  | [docs/development/migration-records.md](docs/development/migration-records.md) | 各插件迁移状态                 |
| Wiki 同步 | [docs/WIKI-SETUP.md](docs/WIKI-SETUP.md)                                       | Wiki 自动发布流程              |

## 当前开发重点

下一代框架以 [docs/framework-next/README.md](docs/framework-next/README.md) 为主事实源，后续新项目从最佳实践开发者写法反推实现；旧 `modules/*` 和 `plugins/*` 仅作为需求考古素材。

## 工具脚本

```bash
python scripts/language-analyzer.py              # 语言键分析
python scripts/language-analyzer.py --check-best-practices  # i18n 合规检查
python scripts/prepare-wiki-multi.py              # 生成 Wiki 页面
python scripts/test-wiki-locally.py               # 本地 Wiki 测试
```
