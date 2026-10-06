# 当前设计决策

本页记录已经拍板的方向。当前阶段只迭代文档和方案，不讨论插件切换路径。

## D1: Folia 作为调度模型预留，不作为近期承诺

Folia 可以理解为 Paper 面向多区域并行 tick 的服务器形态。传统 Paper/Bukkit 的插件心智模型是“多数 Bukkit API 回主线程”，而 Folia 不再只有一个万能主线程，世界、实体和区域有更细的线程归属。

本框架当前只需要把调度模型设计对：

- 公共 API 不只暴露 `sync/async` 两档。
- 使用 `ThreadAffinity.Main`、`AsyncIo`、`AsyncCpu`、`Region`、`Entity` 表达归属。
- Paper 下 `Region/Entity` 可以先映射回主线程。
- Folia adapter 留作后续设计验证，不把 `folia-supported: true` 当成能力承诺。

## D2: plugin 配置文件由代码声明生成静态 yml

方向：开发者不再手写维护多份 `plugin.yml` / `paper-plugin.yml`。框架提供代码声明，Gradle 在构建期生成静态 yml，并在 CI 或构建任务中校验产物。

示意：

```kotlin
pluginSpec {
    id("tpa")
    name("TPA")
    mainClass("city.newnan.tpa.TPAPlugin")
    apiVersion("1.21")

    command("tpa") {
        permission("newnan.tpa.use")
        descriptionKey(Keys.Command.TPA.DESCRIPTION)
    }

    permission("newnan.tpa.admin") {
        default(PermissionDefault.OP)
    }

    softDepend("Vault")
}
```

约束：

- 生成必须发生在 build-time，因为 Paper 加载插件前就要读取静态 yml。
- 运行时只读取已经打包进 jar 的结果，不运行时写 plugin 元数据。
- 代码声明是事实源；静态 yml 是构建产物。
- 构建校验要检查命令、权限、依赖、libraries、api-version 和主类一致性。

## D3: PersistenceRuntime 不进入 framework core

PersistenceRuntime 不是 ORM，也不是要统一所有数据库模型。它是 optional runtime module，不进入 framework core。DB driver 由插件自己引入，避免框架核心依赖臃肿。

它主要服务这些场景：

- Foundation: 数据库/CSV、排行榜、余额、批量保存。
- DynamicEconomy: `economy-cache.yml`、财富统计、商品价格、频繁保存。
- ExternalBook: 数据库或 JSON/YAML 书籍存储。
- RailArea/FeeFly/TPA: 玩家状态、缓存文件、周期性落盘或清理。

设计边界：

- optional runtime 提供 write policy、batch queue、flush lifecycle、failure report、backpressure、metrics。
- 业务仍决定数据模型、事务语义和具体 storage backend。
- driver 不由 framework core 传递引入。
- 高价值交易类写入必须能选择更强一致性的策略。
- 派生缓存和统计类写入可以选择合并、延迟或可重建策略。

## D4: 测试禁用先按历史遗留处理，但新框架必须有 harness

root build 禁用 Test 的原因暂时未知。当前不直接恢复，也不删除相关设置。

判断：禁用原因大概率来自旧框架问题或 project 配置不稳定。新框架不能继续依赖只启动真实 server 测试。MockBukkit/Testcontainers 是刚需，具体引入点在实现前复核。

下一步只做事实调查：

- 记录 root build 禁用 Test 的位置。
- 分别尝试 core/config/i18n/gui 的关键测试命令。
- 记录真实失败点、依赖缺口和可恢复范围。
- 先建立设计验证所需的 harness 清单，再决定是否改构建配置。
- 测试用 golden sample 提交仓库；生产构建生成的 descriptor 不提交。

## D5: 当前阶段只做文档迭代

当前不进入代码实现，不讨论插件切换路径。文档拆细已经完成，后续进入开发前应从这些入口读取：

- [api-surface-map.md](api-surface-map.md): 当前公开 Interface、内部泄漏和设计候选。
- [decision-recommendations.md](decision-recommendations.md): 已拍板结果和框架推荐方案。
- [../02-runtime/development-slices.md](../02-runtime/development-slices.md): 可执行开发切片。
- [../02-runtime/test-strategy.md](../02-runtime/test-strategy.md): harness、contract tests、adapter tests、golden samples。

## D6: 统一设计成为当前主入口，调研资产单独保留

前面形成的调研、对比和痛点分析不丢弃，但它们不再作为框架方案的主要阅读入口。当前主入口是 `00-overview/unified-framework-design.md`，它把已有目标架构、隐含需求、性能目标、EventRuntime、CommandRuntime、GUI 比较和其他相关工作重新整合为一套完整设计。

调研材料整理到 `01-research/research-assets.md`，后续可用于：

- 技术博客。
- 设计说明。
- 框架推广材料。
- 外部方案优劣对比。

约束：

- 对外材料不能宣称尚未实现的能力。
- 第三方项目只作为参考和 Adapter 候选，不作为业务 Interface 的事实源。
- 后续具体草案要以 `00-overview/unified-framework-design.md` 的 owner、scope、affinity、policy、snapshot、report、diagnostics 为主线。

## D7: 2026-06-19 拍板结果进入决策表

本轮对旧入口、Folia、测试、Persistence、build-time descriptor、command、runtime 语义、event/gui、config/text、network/cache、diagnostics/integration 等问题做了集中拍板。

权威表见 [decision-recommendations.md](decision-recommendations.md)。后续实现优先按该文件执行；Runtime 文档中的 `开放问题` 保留为上下文，不再代表未决状态。
