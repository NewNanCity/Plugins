# Verification Matrix

这份矩阵定义什么证据能证明一个模块完成。绿色测试本身不够，必须证明测试覆盖了对应契约。

## Evidence Types

| Evidence | 证明什么 | 不证明什么 |
| --- | --- | --- |
| unit test | 单个类型或纯函数行为 | adapter 和真实 Paper 行为 |
| contract test | runtime 语义和 public API 稳定性 | 平台 glue |
| golden artifact | 生成格式稳定 | 运行期行为正确 |
| MockBukkit test | Paper/Bukkit glue 可加载 | Folia、真实 tick、复杂 inventory 全部边界 |
| source hygiene test | first-screen API 没有泄漏低层 primitive | 内部实现质量 |
| smoke test | 打包插件能在目标 Paper 启动 | 全量业务正确性 |
| docs link check | 文档可发现 | 内容正确 |

## Matrix

| Area | Required evidence |
| --- | --- |
| Plugin/build | Gradle functional test, golden `paper-plugin.yml`, jar entry verification, source hygiene test |
| Lifecycle/owner | owner close order contract test, leak detection test, close timeout test |
| Module graph | required/optional dependency tests, degraded module report test |
| Config | data class schema test, comment-preserving file test, reload failure retention test, restart-required diff test |
| Text | typed catalog generation/proxy test, missing/unused key tests, MiniMessage validation test |
| Command | usage parser tests, sender policy tests, argument/options tests, descriptor golden, error rendering tests, suggestion timeout tests |
| Event | priority/ignoreCancelled tests, filter order test, once-by-handled test, throttle/debounce tests, owner cleanup test |
| Task | affinity dispatch tests, timeout/cancel/failure tests, join/race cleanup tests, owner close cancellation test |
| GUI | slot policy tests, stale generation tests, input/output commit tests, Paper inventory adapter tests |
| Cache | ttl/size/invalidation tests, owner cleanup test, loader failure diagnostics |
| Persistence | batch merge/flush/drain tests, failure report tests, file snapshot atomic write tests |
| Network | timeout/retry/cancel tests, redaction tests, owner close cancellation |
| Integration | required/optional capability tests, provider state change tests, missing dependency report |
| Diagnostics | JSON schema test, ring buffer test, source registration test, admin command permission test |
| Samples | best-practice source hygiene, generated artifact verification, MockBukkit load, dogfood integration tests |
| Release | `check`, release gate task, docs link check, API boundary scan, Paper 1.20.1+ smoke evidence |

## Completion Rule

一个模块标记完成必须同时满足：

- public API 写入 docs。
- best-practice sample 或明确的 advanced sample 展示用法。
- contract tests 覆盖语义。
- adapter tests 覆盖平台 glue。
- diagnostics 能解释失败。
- generated artifacts 有 golden 或 jar verification。

如果只完成 runtime kernel，但 first-screen sample 仍然暴露底层 API，状态是 `dx-drift`，不是 done。

## Release Gate Shape

建议新项目提供：

```powershell
.\gradlew.bat check
.\gradlew.bat frameworkNextReleaseCheck
.\gradlew.bat :samples:best-practice-plugin:verifyGeneratedArtifacts
```

`frameworkNextReleaseCheck` 至少聚合：

- all unit/contract tests。
- best-practice sample tests。
- generated artifact verification。
- docs link check。
- source hygiene scan。
- API boundary scan。

真实 Paper smoke 不一定每次本地运行，但 release candidate 必须附证据。

