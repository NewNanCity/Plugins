# Framework Next

本目录是下一代 NewNan Minecraft 插件框架的全新设计包。它面向后续将要新开的干净项目，不以当前 `modules/*`、`plugins/*` 或任何已写过的 `framework-next` 实现为约束。

这里的目标是把“最终应该让插件开发者怎么写”先定清楚，再从这个目标反推架构、模块边界、构建工具、测试和运行时实现。

## 设计结论

下一代框架不是 Bukkit/Paper 的薄封装，也不是把 Cloud、Ktor、Caffeine、MockBukkit 等库拼在一起。它应该是一个 Minecraft plugin runtime framework：

- 开发者写自然、简洁、声明式的插件代码。
- 框架托管生命周期、owner、线程/区域归属、事件作用域、任务、GUI、命令、配置、文本、缓存、持久化、网络、集成和诊断。
- 构建期从代码声明生成静态 `plugin.yml` / `paper-plugin.yml`、命令描述、权限、配置 schema、文本 catalog 校验产物。
- 运行期只处理已经声明好的行为，不让业务代码维护多份事实源。

第一屏 API 必须接近下面的形态：

```kotlin
class MarketPlugin : PaperPlugin({
    id = "market"
    name = "Market"
    version = "1.0.0"
    apiVersion = "1.20.1+"

    config<MarketConfig>("config.yml") {
        preserveComments()
        unknownKeys.warn()
    }

    messages<MarketText>("lang/{locale}.yml") {
        fallbackLocale = "zh_CN"
        format = TextFormat.MiniMessage
        legacyInput = LegacyInput.Disabled
    }

    module<MarketModule>("market")
})
```

底层 `RuntimeServices`、`ConfigRuntime`、`MessageKey`、`TaskHandle`、adapter、provider、descriptor writer 都可以存在，但它们不是普通插件开发者首先应该看到的东西。

## 阅读顺序

1. [00-background.md](00-background.md): 为什么要重写，生产环境和不兼容边界是什么。
2. [01-pain-points-and-technical-analysis.md](01-pain-points-and-technical-analysis.md): Minecraft 插件开发的痛点和关键技术分析。
3. [02-architecture-and-best-practice.md](02-architecture-and-best-practice.md): 总架构、核心概念和目标最佳实践代码。
4. [03-new-project-bootstrap.md](03-new-project-bootstrap.md): 新干净项目如何开工。
5. [04-roadmap-outline.md](04-roadmap-outline.md): 从 0 到 1 的长期里程碑路线图、测试和验收门。
6. [api-contracts.md](api-contracts.md): 目标 public API 契约草案。
7. [implementation/README.md](implementation/README.md): 各部分具体实现文档入口。
8. [samples-policy.md](samples-policy.md): 样例治理和 first-screen 规则。
9. [research-and-tradeoffs.md](research-and-tradeoffs.md): 相关工作调研和取舍索引。
10. [verification-matrix.md](verification-matrix.md): 各模块完成证据矩阵。
11. [anti-patterns.md](anti-patterns.md): 防止再次跑偏的反模式清单。
12. [decisions.md](decisions.md): 当前已经拍板或推荐的设计决策。
13. [implementation-checklist.md](implementation-checklist.md): 新干净项目从零实现时的可执行检查清单。

## 目录结构

```text
docs/framework-next/
├── README.md
├── 00-background.md
├── 01-pain-points-and-technical-analysis.md
├── 02-architecture-and-best-practice.md
├── 03-new-project-bootstrap.md
├── 04-roadmap-outline.md
├── api-contracts.md
├── samples-policy.md
├── research-and-tradeoffs.md
├── verification-matrix.md
├── anti-patterns.md
├── decisions.md
├── implementation-checklist.md
└── implementation/
    ├── README.md
    ├── plugin-build.md
    ├── lifecycle-owner-module.md
    ├── config-text.md
    ├── command.md
    ├── event-task.md
    ├── gui.md
    ├── performance-cache-persistence-network.md
    └── diagnostics-testing-integration.md
```

## 事实源规则

后续新项目中，如果文档、样例和代码产生冲突，按这个顺序裁决：

1. `02-architecture-and-best-practice.md` 中的最终开发者写法。
2. `api-contracts.md` 中的 public API 契约。
3. `samples-policy.md` 中的 first-screen 样例治理规则。
4. `04-roadmap-outline.md` 中的阶段验收门。
5. `implementation/*` 中的模块级设计和取舍。
6. `decisions.md` 中已经拍板的边界。
7. `verification-matrix.md` 中定义的证据要求。
8. 未来新项目里的 contract tests 和 golden samples。
9. 旧仓库当前代码。旧代码只能作为需求考古素材，不能倒推新接口。

## 明确不做

- 不兼容旧 `BasePlugin` / `BaseModule` 入口。
- 不为了迁移旧插件保留旧命名或旧生命周期模型。
- 不让开发者维护 `plugin.yml`、命令描述、权限、配置 schema、文本 key 多份事实源。
- 不把第三方库类型作为公共业务接口。
- 不把 Folia 写成近期承诺；近期只把 affinity 模型设计正确。
