# 改进路线

本路线是方案，不是本轮实施清单。涉及公共 Interface 的步骤实施前需要单独确认。

## Phase 0：统一事实源

目标：先消除文档和接口契约冲突，避免后续修复继续扩大歧义。

建议：

- 定义生命周期词汇：BaseModule-managed module、plain manager、resource。
- 修改文档事实源：BaseModule-managed module 不再推荐 lazy；plain manager 可以 lazy + bind。
- 给 `BaseModule` 未初始化状态增加可观测性：reload/close 之外的关键方法遇到未初始化模块要记录明确 warning。

验收：

- `CLAUDE.md`、`docs/core/base-module.md`、`docs/development/plugin-conventions.md` 不再互相矛盾。
- 新插件示例只有一种推荐初始化方式。

## Phase 1：先修高风险逻辑缺陷

目标：修复不需要大改架构但会直接造成崩溃或资源泄漏的问题。

候选：

- GUI：`SessionStorage.performCleanup()` 离线玩家 NPE。
- GUI：`Session.close()` 与 `clearPages()` 的 closed 顺序。
- GUI：`BaseComponent.close()` 不应关闭 page scheduler。
- Scheduler：`thenRun*` 不再用 `getNow(null)!!` 伪装失败。
- Network：`NetworkDSL.createHttpClient()` 不应忽略 config。
- Config：非 ObjectNode 根节点 fail fast。

验收：

- 每个修复有最小单元测试或可运行 smoke。
- 不破坏现有公开调用方式。

## Phase 2：收敛 Module Interface

目标：把调用方必须知道的隐式规则收回 Module 内部，提高 Depth。

建议：

- BaseModule：新增 `installModule` seam，旧手动 `init()` 只保留兼容。
- Scheduler：新增 `TaskOutcome<T>` 内部模型和安全链式 Interface，旧 Interface 渐进 deprecate。
- Config：新增 `ConfigPath` 和 `ConfigCacheKey` 内部 Module，所有路径/缓存使用同一事实源。
- i18n：新增 `ReloadReport`，严格模式可抛聚合异常。
- Network：选定 `BasePlugin.networkManager` 为唯一归属入口，其他 DSL 只作为 thin convenience，不创建新 owner。

验收：

- 调用方不需要知道 addChild/bind/init 的顺序。
- 调用方不需要知道 path split 细节。
- 网络客户端不会跨插件共享，除非显式声明 global。

## Phase 3：重做 GUI 全局状态

目标：把过宽的 `SessionStorage` 拆成内部 Module，但保留 `GuiManager` 外部 Interface。

建议结构：

- `SessionStore`：只负责 player/session/page stack。
- `ChatInputRegistry`：只负责每玩家输入 handler。
- `GuiEventRouter`：从事件中的 inventory/session/page 找 owner，不使用全局 current manager。
- `GuiLifecycleOwner`：提供调度 owner，不 fallback 到任意 enabled plugin。

验收：

- 多插件同时注册 GuiManager 时，各自页面事件都能被路由。
- GuiManager close 只清自己的资源。
- 不再需要 `SessionStorage.currentGuiManager` 作为事件过滤条件。

## Phase 4：恢复 modules 级测试

目标：让架构约束变成测试 surface。

优先测试：

- `ConfigPath`：转义点、尾部反斜杠、空段。
- `ConfigManager`：非对象根节点、cache key、deepMerge changed 标记。
- `LanguageManager`：重复模板、缺失翻译、reload partial failure。
- `TaskOutcome`：success/failure/cancelled/null value 链式行为。
- Network ownership：同名 client 在不同 plugin 下不共享；wrapper close 不关闭 manager client。

验收：

- `modules:*:test` 至少对纯 JVM 测试启用。
- 根 build 可继续跳过 Paper integration，但不跳过 modules 单元测试。

## 不建议做的事

- 不建议直接大规模拆包。当前问题主要是 Interface 语义和 ownership，不是目录层级。
- 不建议用 wrapper 兼容所有错误输入。Config/i18n/network 的非法状态应 fail fast 或形成报告。
- 不建议在调用侧复制规则，例如每个插件自己处理网络 client 命名空间或 GUI event 过滤。
- 不建议继续扩展 DSL，除非它提供真实 Leverage，而不是简单转发到底层 Ktor。
