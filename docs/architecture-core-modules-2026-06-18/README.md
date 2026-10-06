# Core & Modules 架构审查

日期：2026-06-18

范围：`modules/core`、`modules/config`、`modules/gui`、`modules/i18n`、`modules/network`，以及这些 Module 的文档和插件调用模式。

## Outcome

本轮只做架构理解和方案输出，不改业务代码。完成后应成立：

- core/modules 的主要逻辑问题、过度设计、设计不足有代码证据。
- 每个候选问题都说明当前 Module 的 Interface 为什么浅、宽或不稳定。
- 改进方案集中写在本目录，后续可以按优先级拆成实现任务。

## Success Criteria

- 文档集中在 `docs/architecture-core-modules-2026-06-18/`。
- 结论引用当前代码路径和关键行号。
- 区分立即修复、架构正修、暂不做。
- 不修改公共契约、数据库 schema、发布配置或业务实现。

## Constraints

- 当前工作树已有大量未提交改动，本轮只新增本目录文件。
- 不把历史 `docs/reviews/modules-review-2026-03-09.md` 当作结论，必须重新核对当前代码。
- 建议涉及公共 Interface 变更时，只记录方案，实施前需单独确认。
- 测试在根 `build.gradle.kts` 中被禁用，本轮无法用测试通过率证明行为正确性。

## Non-goals

- 不修复代码。
- 不重构 Module。
- 不新增依赖。
- 不做发布、打包、部署。

## Stop Rules

实施下列变更前必须停下确认：

- 修改 `BaseModule`、`ITaskHandler`、`ConfigManager`、`GuiManager`、`LanguageManager`、`HttpClientManager` 等公共 Interface。
- 改变插件生命周期、配置文件读取/写入语义、网络客户端归属或 GUI 全局事件分发规则。
- 为了兼容旧行为引入 wrapper/adapter，但无法说明移除条件。

## 结论摘要

当前 core/modules 的主要问题不是“缺少抽象”，而是几个关键 Module 的 Interface 没有把调用方必须知道的约束封装起来：

1. `BaseModule` 的初始化 Interface 太浅：调用方必须记住 `init { init() }`，文档还同时传播 `lateinit` 和 `by lazy` 两套互斥规范。
2. scheduler 的链式任务 Interface 把失败、取消和可空结果折叠成 `getNow(null)!!`，导致真实错误模式丢失。
3. GUI 的 `SessionStorage` 是过宽的全局 Module：Session、GuiManager 选择、事件责任链、主线程调度、聊天输入都集中在一个单例里，导致死锁、串插件事件丢失和生命周期泄漏。
4. Config 的路径语义有多个事实源：`Config.parsePath()` 私有实现、`JsonExtensions.path.split(".")`、merge 读取失败时静默降级，导致同一 Interface 下行为不一致。
5. i18n 的模板替换 Implementation 直接修改正在匹配的字符串，且 reload 错误只记录不传播，调用方无法通过 Interface 得知语言层是否健康。
6. Network 同时存在全局 `name -> HttpClientManager`、`BasePlugin -> HttpClientManager`、临时 DSL builder 三套归属模型，是设计不足和过度设计叠加。

## 文档导航

- [findings.md](findings.md)：详细问题、证据和风险。
- [improvement-plan.md](improvement-plan.md)：建议的分阶段正修路线。
- [TODO.md](TODO.md)：本轮任务追踪和后续待办。
