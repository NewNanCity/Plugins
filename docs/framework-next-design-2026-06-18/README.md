# 下一代框架设计包

本目录记录 NewNanPlugins core/modules 下一代框架设计。当前阶段只做文档、Interface 草案和验证方案，不进入代码实现。

## 阅读入口

1. [00-overview/unified-framework-design.md](00-overview/unified-framework-design.md): 当前主设计，解释整套框架如何围绕 owner、scope、affinity、policy、snapshot、report、diagnostics 组织。
2. [00-overview/api-surface-map.md](00-overview/api-surface-map.md): 当前 core/modules 暴露能力到下一代 Runtime 的归属图。
3. [00-overview/decision-recommendations.md](00-overview/decision-recommendations.md): 2026-06-19 拍板结果和框架推荐方案。
4. [03-implementation/phase-1-plan.md](03-implementation/phase-1-plan.md): 第一阶段开发计划，覆盖 `framework-next/` 隔离子树和最小 sample。
5. [02-runtime/development-slices.md](02-runtime/development-slices.md): 可直接进入开发排期的切片、验收标准和暂停条件。
6. [02-runtime/README.md](02-runtime/README.md): Runtime 细化设计入口，所有深 Module 草案都放这里。
7. [01-research/research-assets.md](01-research/research-assets.md): 调研资产库，保留未来技术博客、推广说明和外部对比可复用素材。
8. [TODO.md](TODO.md): 当前拆细进度和待确认问题。

## 目录结构

```text
docs/framework-next-design-2026-06-18/
├── README.md
├── TODO.md
├── 00-overview/
│   ├── README.md
│   ├── api-surface-map.md
│   ├── decision-recommendations.md
│   ├── unified-framework-design.md
│   ├── current-decisions.md
│   ├── inferred-requirements.md
│   ├── performance-and-cost.md
│   └── iteration-roadmap.md
├── 01-research/
│   ├── README.md
│   ├── research-assets.md
│   ├── external-pain-points.md
│   ├── framework-comparison.md
│   ├── framework-comparison-followups.md
│   └── related-work-other-capabilities.md
├── 02-runtime/
    ├── README.md
    ├── lifecycle-owner-runtime.md
    ├── task-runtime.md
    ├── event-runtime.md
    ├── command-runtime.md
    ├── gui-runtime.md
    ├── plugin-spec-generation.md
    ├── config-runtime.md
    ├── text-runtime.md
    ├── network-runtime.md
    ├── persistence-cache-runtime.md
    ├── diagnostics-runtime.md
    ├── integration-runtime.md
    ├── test-strategy.md
    └── development-slices.md
└── 03-implementation/
    ├── README.md
    └── phase-1-plan.md
```

## 当前设计立场

下一代框架不是更短的 Bukkit wrapper，而是 Minecraft plugin runtime framework。业务插件只表达业务意图；框架托管生命周期、任务、事件、GUI、命令、配置、文本、网络、持久化、缓存、集成和诊断。

设计主线：

- 所有长期资源必须属于 owner tree。
- 所有触碰 Bukkit/Paper/Folia 的行为必须表达 affinity。
- timeout、retry、cache ttl、batch flush、event backpressure 等 policy 进入 Interface。
- 配置、消息、capability、reload、task 都以 snapshot/report 作为事实源。
- 第三方库只作为 Adapter 或参考，不能倒推业务 Interface。

## 文档治理

- 根目录只保留入口和追踪文件，避免继续堆平铺文档。
- `00-overview/` 放主设计、需求和路线。
- `01-research/` 放证据、来源和可复用传播素材。
- `02-runtime/` 放按深 Module 拆细的 Runtime 草案。
- `03-implementation/` 放开发阶段计划和验收入口。
- 过时骨架如果已被统一设计吸收，直接删除；有证据价值的内容保留在 overview 或 research 中。

## 适用边界

本目录不代表已经实现或运行验证通过。当前只记录设计决策、Interface 草案、事实调查和验证方案。
