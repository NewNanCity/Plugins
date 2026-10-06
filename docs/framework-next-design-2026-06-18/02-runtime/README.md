# Runtime Specs

本目录按深 Module 拆分下一代框架的 Runtime 草案。每份文档固定回答五件事：

1. Interface 是什么。
2. Implementation 藏了什么。
3. Seam 在哪里。
4. Adapter 如何接入。
5. Harness 如何验证。

## 已有草案

- [lifecycle-owner-runtime.md](lifecycle-owner-runtime.md): owner tree、Module state、close order、resource diagnostics。
- [task-runtime.md](task-runtime.md): affinity、task chain、outcome、budget、timeout、cancellation。
- [event-runtime.md](event-runtime.md): event scope、priority、ignoreCancelled、event-task bridge、backpressure。
- [command-runtime.md](command-runtime.md): 声明式命令、Cloud Adapter、权限、help、静态 yml 元数据。
- [gui-runtime.md](gui-runtime.md): GUI session、page、component、render diff、data provider、event routing。
- [plugin-spec-generation.md](plugin-spec-generation.md): 代码声明生成静态 `plugin.yml` / `paper-plugin.yml`。
- [config-runtime.md](config-runtime.md): config schema、snapshot、reload transaction、reload report。
- [text-runtime.md](text-runtime.md): message key、catalog、audience rendering、fallback、placeholder validation。
- [network-runtime.md](network-runtime.md): named client、policy、request outcome、redaction、service health。
- [persistence-cache-runtime.md](persistence-cache-runtime.md): repository executor、batch writer、flush lifecycle、cache invalidation。
- [diagnostics-runtime.md](diagnostics-runtime.md): owner/resource graph、health dump、structured report、diagnostic sink。
- [integration-runtime.md](integration-runtime.md): typed capability、provider state、soft dependency health。
- [test-strategy.md](test-strategy.md): harness、contract tests、adapter tests、golden samples。
- [development-slices.md](development-slices.md): 可执行开发切片、验收标准和暂停条件。

## 开发入口

后续实现优先从 [development-slices.md](development-slices.md) 进入。单个切片开始前，先阅读对应 Runtime spec 和 [../00-overview/api-surface-map.md](../00-overview/api-surface-map.md)。

## 结构约定

每份 Runtime 草案都应包含：

- `定位`: 这个 Module 解决什么问题。
- `Interface`: 调用者必须知道的类型、顺序、错误模式、性能特征。
- `Implementation`: 隐藏在 Module 内部的复杂性。
- `Seam 和 Adapter`: Bukkit/Paper/Folia 或第三方库如何接入。
- `协作关系`: 与其他 Runtime 的交互。
- `Diagnostics`: 可观察状态和 report。
- `Harness`: 测试入口和关键用例。
- `开放问题`: 需要后续拍板的点。
