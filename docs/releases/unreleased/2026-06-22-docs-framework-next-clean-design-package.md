---
type: docs
scope: docs
audience: developer
summary: 重写 framework-next 干净项目设计文档包
breaking: false
demo_ready: false
tests:
  - powershell link check for docs/framework-next markdown links
  - git diff --check -- docs/framework-next docs/README.md CLAUDE.md TODO.md docs/releases/unreleased/2026-06-22-docs-framework-next-clean-design-package.md
artifacts:
  - docs/framework-next/README.md
  - docs/framework-next/00-background.md
  - docs/framework-next/01-pain-points-and-technical-analysis.md
  - docs/framework-next/02-architecture-and-best-practice.md
  - docs/framework-next/03-new-project-bootstrap.md
  - docs/framework-next/04-roadmap-outline.md
  - docs/framework-next/api-contracts.md
  - docs/framework-next/samples-policy.md
  - docs/framework-next/research-and-tradeoffs.md
  - docs/framework-next/verification-matrix.md
  - docs/framework-next/anti-patterns.md
  - docs/framework-next/implementation/
  - docs/framework-next/decisions.md
  - docs/framework-next/implementation-checklist.md
---

## What changed

Added a new `docs/framework-next` design package for a future clean rewrite. The package starts from the desired best-practice developer surface, then documents background, pain points, architecture, a 0-to-1 milestone roadmap, module-level implementation plans, public API contracts, sample governance, research trade-offs, verification evidence, anti-patterns, decisions, and an implementation checklist.

## Why it matters

The new docs deliberately do not treat current implementation work as the source of truth. They define what the next clean project should build toward: lower cognitive load for plugin authors, typed config/text, unified command declarations, scoped event/task behavior, GUI runtime semantics, build-time metadata generation, and diagnostics-first operation.

## Demo posture / limitations

This is a documentation and design artifact only. It does not mean the current code implements the final API, and it does not decide whether existing experimental code should be kept, deleted, or rewritten.
