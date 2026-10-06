# TODO

## 进行中

当前无进行中的文档拆细项。下一步可按 `02-runtime/development-slices.md` 拆开发任务。

## 已完成

- [x] 补充 `01-research/related-work-other-capabilities.md`：scheduler、lifecycle、config、i18n、network、persistence/cache、diagnostics、integration 的相关工作与设计启发。
- [x] 补充 `01-research/research-assets.md`：把调研工作整理为未来技术博客、推广说明和外部对比可复用素材。
- [x] 补充 `00-overview/unified-framework-design.md`：把已有设计、隐含需求、性能目标、event/gui/command 讨论和相关工作重塑成一体化框架设计。
- [x] 重组设计包目录：根目录只保留入口和追踪，overview/research/runtime 分层放文档，删除已被统一设计吸收的早期骨架。
- [x] 补强 `02-runtime/event-runtime.md` 和 `02-runtime/command-runtime.md`，新增 `02-runtime/gui-runtime.md`。
- [x] 补充 `00-overview/api-surface-map.md`：把当前 core/modules public surface 映射到下一代 Runtime。
- [x] 补充 `01-research/framework-comparison-followups.md`：把调研收敛为设计取舍准则和对外写作角度。
- [x] 补充 `02-runtime/plugin-spec-generation.md`：代码声明生成静态 yml 的构建期设计。
- [x] 补充 `02-runtime/config-runtime.md` 和 `02-runtime/text-runtime.md`：snapshot、reload report、message catalog、audience rendering。
- [x] 补充 `02-runtime/network-runtime.md`：named client、policy、request outcome、redaction、service health。
- [x] 补充 `02-runtime/persistence-cache-runtime.md`：repository executor、batch writer、flush lifecycle、cache policy。
- [x] 补充 `02-runtime/diagnostics-runtime.md` 和 `02-runtime/integration-runtime.md`：health dump、resource graph、typed capability、provider health。
- [x] 补充 `02-runtime/test-strategy.md`：harness、contract tests、adapter tests、golden samples。
- [x] 补充 `02-runtime/development-slices.md`：把整体方案拆成 12 个可执行开发切片。
- [x] 补充 `00-overview/decision-recommendations.md`：记录 2026-06-19 拍板结果和框架推荐方案。
- [x] 补充 `03-implementation/phase-1-plan.md`：记录 `Minecraft/Paper 1.20.1+` 基线、`framework-next/` 隔离策略和第一阶段开发计划。

## 待确认

当前无阻塞文档拆细的待确认项。各 Runtime 原文中的 `开放问题` 已由 `00-overview/decision-recommendations.md` 覆盖；实现前只需复核第三方库版本和平台细节。

## 后续文档

- [x] `00-overview/api-surface-map.md`: 当前 public Interface 清单和设计候选。
- [x] `02-runtime/lifecycle-owner-runtime.md`: owner tree、module state、resource close order。
- [x] `02-runtime/plugin-spec-generation.md`: 代码声明生成静态 yml 的设计。
- [x] `02-runtime/task-runtime.md`: affinity、outcome、budget、timeout、cancellation。
- [x] `02-runtime/gui-runtime.md`: GUI session/page/render/data/event routing 设计。
- [x] `01-research/framework-comparison-followups.md`: 框架比较后的细化取舍。
- [x] `02-runtime/config-runtime.md`: PluginSpec、ConfigSchema、ConfigSnapshot、ConfigReloadReport。
- [x] `02-runtime/text-runtime.md`: MessageKey、AudiencePolicy、CatalogReport、MiniMessage adapter。
- [x] `02-runtime/network-runtime.md`: NamedHttpClient、NetworkPolicy、RequestDiagnostics。
- [x] `02-runtime/persistence-cache-runtime.md`: DataSourceScope、TransactionScope、BatchWriter、CacheRuntime。
- [x] `02-runtime/diagnostics-runtime.md`: health dump、resource graph、test harness、profiling hooks。
- [x] `02-runtime/integration-runtime.md`: typed capability、soft dependency state、provider health。
- [x] `02-runtime/test-strategy.md`: root 测试恢复和模块 harness 设计。
- [x] `02-runtime/development-slices.md`: 可执行开发切片和验收矩阵。
- [x] `00-overview/decision-recommendations.md`: 拍板结果和框架推荐方案。
- [x] `03-implementation/phase-1-plan.md`: 第一阶段开发计划。
