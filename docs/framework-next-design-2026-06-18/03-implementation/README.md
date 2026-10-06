# Implementation Planning

本目录放真正进入开发前的执行计划。它不是 Runtime 设计说明，而是把设计拆成可以落代码、写测试、验收的阶段。

## 文档

- [phase-1-plan.md](phase-1-plan.md): 第一阶段计划，建立 isolated `framework-next/`、最小 sample、Owner/Diagnostics/Task/Event/PluginSpec/Command/Config/Text 的最小闭环。

## 使用规则

- 每个阶段计划必须明确目标、非目标、目录边界、artifact 拆分、任务顺序和验收命令。
- 计划不得要求修改旧 `modules/*` 作为前置条件。
- 真正执行前，需要基于当时的 Gradle/Paper/Kotlin 版本再做一次依赖复核。
