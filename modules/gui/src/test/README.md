# GUI模块测试文档

## 📋 概述

GUI模块的测试套件提供了全面的测试覆盖，确保模块的稳定性、性能和可靠性。测试分为五个主要类别：

- **单元测试** - 测试各个组件的独立功能
- **集成测试** - 测试组件之间的协作
- **性能测试** - 测试性能表现和资源使用
- **并发测试** - 测试多线程环境下的线程安全性
- **内存泄漏测试** - 测试资源清理和内存管理

## 🏗️ 测试结构

```
src/test/kotlin/city/newnan/gui/
├── unit/                    # 单元测试
│   ├── SessionStorageTest.kt
│   ├── SessionTest.kt
│   ├── GuiManagerTest.kt
│   └── PageComponentTest.kt
├── integration/             # 集成测试
│   └── GuiIntegrationTest.kt
├── performance/             # 性能测试
│   └── PerformanceTest.kt
├── concurrent/              # 并发测试
│   └── ConcurrencyTest.kt
├── memory/                  # 内存泄漏测试
│   └── MemoryLeakTest.kt
├── util/                    # 测试工具
│   └── TestUtils.kt
├── GuiTestSuite.kt        # 完整测试套件
└── README.md               # 本文档
```

## 🧪 测试类详细说明

### 单元测试 (Unit Tests)

#### SessionStorageTest
- **目的**: 测试SessionStorage的线程安全性和管理功能
- **覆盖内容**:
  - Session创建和获取
  - 命名Session管理
  - 玩家Session清理
  - GuiManager注册和管理
  - ChatInput处理器管理
  - 多玩家并发访问
  - 异常情况处理

#### SessionTest
- **目的**: 测试Session的栈管理和生命周期
- **覆盖内容**:
  - 栈操作（push/pop/replace）
  - 显示状态管理（show/hide）
  - 导航操作（goto）
  - 最大深度限制
  - Session关闭和清理
  - 并发访问安全性
  - 异常处理和状态一致性

#### GuiManagerTest
- **目的**: 测试GuiManager的初始化和管理功能
- **覆盖内容**:
  - 初始化和关闭
  - Page创建和验证
  - 生命周期绑定
  - SessionStorage集成
  - ChatInput功能
  - 多Manager协作

#### PageComponentTest
- **目的**: 测试Page和Component的生命周期和渲染
- **覆盖内容**:
  - Page生命周期管理
  - Component添加和管理
  - 渲染机制和缓存
  - 事件处理
  - 异常处理和恢复
  - 资源清理

### 集成测试 (Integration Tests)

#### GuiIntegrationTest
- **目的**: 测试各组件之间的协作和完整工作流程
- **覆盖内容**:
  - 完整GUI工作流程
  - 多页面导航
  - 跨插件GUI协作
  - 多玩家并发使用
  - ChatInput集成
  - 复杂场景状态一致性
  - 异常恢复能力

### 性能测试 (Performance Tests)

#### PerformanceTest
- **目的**: 测试性能表现和资源使用效率
- **覆盖内容**:
  - 大量组件渲染性能
  - 频繁更新性能
  - 内存使用效率
  - 并发访问性能
  - Session栈操作性能
  - 组件渲染缓存性能

### 并发测试 (Concurrency Tests)

#### ConcurrencyTest
- **目的**: 测试多线程环境下的线程安全性
- **覆盖内容**:
  - SessionStorage并发访问
  - Session并发栈操作
  - 多GuiManager并发注册
  - 并发ChatInput处理
  - 竞态条件检测
  - 状态一致性验证

### 内存泄漏测试 (Memory Leak Tests)

#### MemoryLeakTest
- **目的**: 测试资源清理和内存管理
- **覆盖内容**:
  - Page和Component资源清理
  - Session生命周期管理
  - GuiManager关闭后清理
  - ChatInput处理器清理
  - 长期运行稳定性
  - 循环引用检测

## 🚀 运行测试

### 运行所有测试
```bash
./gradlew :modules:gui:test
```

### 运行特定测试套件
```bash
# 快速测试（单元测试 + 集成测试）
./gradlew :modules:gui:test --tests "GuiQuickTestSuite"

# 性能测试套件
./gradlew :modules:gui:test --tests "GuiPerformanceTestSuite"

# 单元测试套件
./gradlew :modules:gui:test --tests "GuiUnitTestSuite"
```

### 运行特定测试类
```bash
# 运行SessionStorage测试
./gradlew :modules:gui:test --tests "SessionStorageTest"

# 运行性能测试
./gradlew :modules:gui:test --tests "PerformanceTest"
```

### 运行特定测试方法
```bash
./gradlew :modules:gui:test --tests "SessionStorageTest.testDefaultSessionCreation"
```

## 📊 测试报告

测试完成后，可以在以下位置查看详细报告：
- HTML报告: `modules/gui/build/reports/tests/test/index.html`
- XML报告: `modules/gui/build/test-results/test/`

## 🔧 测试工具

### TestUtils
提供了丰富的测试工具类：

- **Mock对象创建**: `createMockPlayer()`, `createMockPlugin()`, `createMockInventory()`
- **性能监控**: `PerformanceMonitor` - 测量操作执行时间
- **内存监控**: `MemoryMonitor` - 监控内存使用情况
- **数据生成**: `TestDataGenerator` - 生成测试数据
- **并发测试**: `ConcurrencyHelper` - 简化并发测试
- **断言助手**: `AssertionHelper` - 性能和内存断言

### 使用示例
```kotlin
// 性能监控
val monitor = TestUtils.PerformanceMonitor()
monitor.measure("page_creation") {
    guiManager.createPage(player, InventoryType.CHEST, 27, "Test")
}
monitor.printReport()

// 内存监控
val memoryMonitor = TestUtils.MemoryMonitor()
memoryMonitor.snapshot("before_test")
// ... 执行测试操作
memoryMonitor.snapshot("after_test")
memoryMonitor.printReport()

// 并发测试
val result = TestUtils.ConcurrencyHelper.runConcurrentTest(
    threadCount = 10,
    operationsPerThread = 100
) { threadIndex, operationIndex ->
    // 并发操作
}
result.printSummary()
```

## 📈 性能基准

### 预期性能指标
- **页面创建**: < 10ms
- **组件添加**: < 1ms
- **页面显示**: < 50ms
- **页面更新**: < 20ms
- **Session操作**: < 5ms

### 内存使用基准
- **单个页面**: < 1MB
- **100个组件**: < 10MB
- **长期运行增长**: < 100MB

### 并发性能
- **10线程并发**: 成功率 > 95%
- **竞态条件**: 0个检测到的不一致状态

## 🐛 故障排除

### 常见问题

1. **测试超时**
   - 检查系统资源使用情况
   - 增加超时时间配置
   - 检查是否有死锁

2. **内存不足**
   - 增加JVM堆内存: `-Xmx2g`
   - 检查是否有内存泄漏
   - 减少测试数据量

3. **并发测试失败**
   - 检查线程安全实现
   - 增加同步机制
   - 减少并发线程数

4. **Mock对象问题**
   - 确保MockK版本兼容
   - 检查Mock配置
   - 验证依赖注入

### 调试技巧

1. **启用详细日志**
   ```kotlin
   // 在测试中添加
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
   ```

2. **内存分析**
   ```kotlin
   // 强制垃圾回收
   System.gc()
   Thread.sleep(100)

   // 打印内存使用
   val runtime = Runtime.getRuntime()
   println("Memory: ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}MB")
   ```

3. **性能分析**
   ```kotlin
   // 使用性能监控器
   val monitor = TestUtils.PerformanceMonitor()
   // ... 测试代码
   monitor.printReport()
   ```

## 📝 贡献指南

### 添加新测试

1. **选择合适的测试类别**
2. **遵循命名约定**: `TestNameTest.kt`
3. **使用TestUtils工具类**
4. **添加详细的测试文档**
5. **更新测试套件**

### 测试最佳实践

1. **独立性**: 每个测试应该独立运行
2. **可重复性**: 测试结果应该一致
3. **清晰性**: 测试名称和断言应该清晰
4. **覆盖性**: 覆盖正常和异常情况
5. **性能**: 避免不必要的延迟

## 📚 参考资料

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [MockK 文档](https://mockk.io/)
- [Kotlin 测试最佳实践](https://kotlinlang.org/docs/jvm-test-using-junit.html)
- [GUI模块设计文档](../../new-gui.md)
- [GUI模块实现总结](../main/kotlin/city/newnan/gui/IMPLEMENTATION_SUMMARY.md)
