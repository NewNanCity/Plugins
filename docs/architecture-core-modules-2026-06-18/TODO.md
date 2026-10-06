# TODO

## 本轮已完成

- [x] 阅读项目索引：`CLAUDE.md`、`README.md`、`docs/core/architecture.md`、`docs/core/base-module.md`、`docs/development/plugin-conventions.md`。
- [x] 阅读历史审查：`docs/reviews/modules-review-2026-03-09.md`。
- [x] 抽样核对 core/config/gui/i18n/network 当前实现。
- [x] 归类逻辑问题、过度设计和设计不足。
- [x] 输出集中审查文档到本目录。

## 后续建议拆分

- [ ] Phase 0：统一 BaseModule/Manager 生命周期文档事实源。
- [ ] Phase 1：修复 GUI 清理、scheduler 链式 NPE、network config ignored、config 非对象根节点等直接逻辑缺陷。
- [ ] Phase 2：设计并确认 BaseModule install seam、TaskOutcome、ConfigPath/CacheKey、i18n ReloadReport、network ownership。
- [ ] Phase 3：重做 GUI 事件路由和 SessionStorage 内部结构。
- [ ] Phase 4：恢复 modules 层纯 JVM 测试。

## 需要用户拍板

- [ ] 是否允许对 `BaseModule` 增加新的推荐创建 Interface，并把手动 `init()` 标为 legacy。
- [ ] 是否允许对 `ITaskHandler` 新增 Result/Outcome 风格 Interface，并逐步 deprecate 旧 `thenRun*`。
- [ ] Network 外部入口选择：以 `BasePlugin.networkManager` 为唯一主 Interface，还是保留 `plugin.network {}` DSL 为主 Interface。
- [ ] GUI 多插件场景是否必须支持多个 GuiManager 同时处理自己的页面事件。
