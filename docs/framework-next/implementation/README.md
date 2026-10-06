# Implementation Specs

本目录按框架组成部分拆分具体实现文档。每份文档固定回答三类问题：

1. 是什么：这个部分解决什么问题，公共概念是什么。
2. 相关工作调研和取舍：参考了哪些生态方案，吸收什么，避免什么。
3. 怎么做：开发者用法规范和内部实现方案。

这些文档是给后续新干净项目使用的实施蓝图。它们不描述当前旧仓库已经实现了什么。

## 文档列表

| 文档 | 范围 |
| --- | --- |
| [plugin-build.md](plugin-build.md) | 插件声明、静态 yml、Gradle/KSP、descriptor、样例策略 |
| [lifecycle-owner-module.md](lifecycle-owner-module.md) | Lifecycle、Owner、Module、scope、dependency graph |
| [config-text.md](config-text.md) | typed config、comment-preserving YAML、typed text catalog、MiniMessage |
| [command.md](command.md) | command DSL、参数、sender、权限、补全、help、metadata |
| [event-task.md](event-task.md) | scoped event、backpressure、task affinity、join/race、interactive scope |
| [gui.md](gui.md) | screen、session、page、slot policy、render diff、Paper inventory safety |
| [performance-cache-persistence-network.md](performance-cache-persistence-network.md) | 性能、cache、persistence、network optional runtimes |
| [diagnostics-testing-integration.md](diagnostics-testing-integration.md) | diagnostics、harness、MockBukkit、integration capability |

## 开发顺序建议

1. `runtime-core`: owner、report、diagnostics 基础。
2. `runtime-plugin`: `PaperPlugin` / `Module` facade 和 build descriptor facts。
3. `framework-gradle-plugin`: 静态 yml、descriptor、schema、catalog 生成。
4. `runtime-config` + `runtime-text`: 第一屏 typed config/text。
5. `runtime-command`: 一体化 command block。
6. `runtime-task` + `runtime-event`: scope、affinity、event-task bridge。
7. `runtime-gui` + `adapter-paper`: screen/page/slot policy。
8. optional runtimes: cache、persistence、network、integration adapters。
9. diagnostics and release gates。

