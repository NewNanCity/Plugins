# i18n Key命名规范和最佳实践

## 🎯 概述

本文档定义了项目中i18n键名的统一命名规范，确保所有插件的多语言支持保持一致性和可维护性。

## 📋 核心分类体系

### 五层架构设计

```yaml
# 1. 核心系统层 (Core System Layer)
core:
  plugin:     # 插件生命周期
  config:     # 配置管理
  error:      # 系统级错误
  success:    # 系统级成功消息
  validation: # 数据验证

# 2. 命令系统层 (Command System Layer)
commands:
  help:             # 帮助系统
  validation:       # 命令参数验证
  [command_name]:   # 具体命令
    success:        # 命令成功
    error:          # 命令错误
    info:           # 命令信息
    confirm:        # 确认消息

# 3. 图形界面层 (GUI Layer)
gui:
  common:           # 通用GUI元素
  [gui_name]:       # 具体GUI
    title:          # 标题
    button:         # 按钮文本
    item:           # 物品名称和描述
    hint:           # 提示信息

# 4. 业务领域层 (Business Domain Layer)
[business_domain]:  # 具体业务（如 death, fly, tpa, book 等）
  [action]:         # 具体动作
    success:        # 成功
    error:          # 错误
    info:           # 信息
    broadcast:      # 广播消息

# 5. 日志系统层 (Logging Layer)
log:
  info:             # 信息日志
  warn:             # 警告日志
  error:            # 错误日志
  debug:            # 调试日志
```

## 🔧 命名约定规范

### 1. 分隔符规则
- **层级分隔**: 使用点号(`.`)分隔层级
  ```yaml
  core.plugin.enabled
  commands.help.header
  gui.common.back
  ```

- **单词分隔**: 使用下划线(`_`)分隔单词
  ```yaml
  reload_success
  no_permission
  player_not_found
  ```

### 2. 命名格式
- **全小写字母**: 所有键名使用小写字母
- **语义化命名**: 键名能清晰表达含义
- **一致性词汇**: 使用统一的词汇，如：
  - `success`/`error`
  - `start`/`stop`
  - `enable`/`disable`
  - `online`/`offline`

### 3. 常用后缀约定
```yaml
# 消息类型后缀
.success          # 成功消息
.error            # 错误消息
.info             # 信息消息
.confirm          # 确认消息
.broadcast        # 广播消息
.warning          # 警告消息

# 界面元素后缀
.title            # 标题
.description      # 描述
.hint             # 提示
.header           # 头部
.footer           # 尾部

# 状态后缀
.enabled          # 已启用
.disabled         # 已禁用
.started          # 已开始
.stopped          # 已停止
.completed        # 已完成
.failed           # 已失败
```

### 4. 常用前缀约定
```yaml
# 功能前缀
error.            # 错误相关
success.          # 成功相关
validation.       # 验证相关
log.              # 日志相关
gui.              # 界面相关
commands.         # 命令相关
core.             # 核心相关
```

## 🌟 MiniMessage高级功能

### 使用MiniMessage的优势

由于我们使用MiniMessage格式，可以利用其丰富的高级功能来创建更加交互性和视觉吸引力的用户界面：

#### 1. 悬浮提示(Hover)
```yaml
gui:
  common:
    help_button: "<yellow><hover:show_text:'<gray>点击查看详细帮助信息<br><gold>包含所有可用命令'>帮助</hover></yellow>"
    settings_button: "<blue><hover:show_text:'<gray>打开设置面板<br><yellow>可以自定义插件行为'>设置</hover></blue>"
```

#### 2. 点击行为(Click Events)
```yaml
commands:
  help:
    command_list: "<click:run_command:'/myplugin help {0}'><yellow>▶ {0}</yellow></click> <gray>- {1}</gray>"
    reload_hint: "<click:run_command:'/myplugin reload'><green>[点击重载]</green></click>"
    url_link: "<click:open_url:'https://docs.example.com'><blue>[查看文档]</blue></click>"
```

#### 3. 物品展示(Item Display)
```yaml
gui:
  inventory:
    item_info: "<hover:show_item:'{item_json}'><yellow>{item_name}</yellow></hover>"
    reward_preview: "<gold>奖励: </gold><hover:show_item:'{reward_json}'><yellow>{reward_name}</yellow></hover>"
```

#### 4. 渐变色彩(Gradients)
```yaml
core:
  plugin:
    welcome_banner: "<gradient:gold:yellow:gold>=== 欢迎使用 {0} ===</gradient>"
    success_message: "<gradient:green:lime>操作成功完成！</gradient>"

gui:
  common:
    title_bar: "<gradient:blue:cyan:blue>{title}</gradient>"
```

#### 5. 复杂交互组合
```yaml
gui:
  player_list:
    player_entry: |
      <click:run_command:'/tp {0}'>
        <hover:show_text:'<gray>玩家信息:<br><yellow>名称: {0}<br><green>在线时间: {1}<br><blue>点击传送到该玩家'>
          <gradient:green:yellow>{0}</gradient>
        </hover>
      </click>

commands:
  help:
    interactive_help: |
      <gold>===== 插件帮助 =====</gold>
      <click:run_command:'/myplugin reload'>
        <hover:show_text:'<gray>重新加载插件配置'>
          <green>• 重载配置</green>
        </hover>
      </click>
      <click:run_command:'/myplugin settings'>
        <hover:show_text:'<gray>打开设置GUI'>
          <blue>• 插件设置</blue>
        </hover>
      </click>
```

#### 6. 动态内容展示
```yaml
economy:
  balance_display: |
    <gradient:gold:yellow>当前余额: {0}</gradient>
    <click:run_command:'/shop'>
      <hover:show_text:'<gray>点击打开商店<br><green>购买各种物品'>
        <green>[前往商店]</green>
      </hover>
    </click>

death:
  death_message: |
    <red>死亡扣费: {0} 金币</red>
    <click:run_command:'/balance'>
      <hover:show_text:'<gray>查看当前余额<br><yellow>剩余: {1} 金币'>
        <yellow>[查看余额]</yellow>
      </hover>
    </click>
```

### 最佳实践建议

**✅ 推荐使用场景**：
- **GUI交互**：按钮、菜单项、提示信息
- **命令帮助**：可点击的命令示例
- **信息展示**：悬浮详情、物品预览
- **用户引导**：交互式教程、提示链接

**❌ 避免过度使用**：
- 不要在日志消息中使用交互功能
- 避免在频繁发送的消息中使用复杂格式
- 保持可读性，不要过度装饰

**🎯 性能考虑**：
- 复杂的MiniMessage格式会增加解析开销
- 建议在非频繁更新的界面中使用高级功能
- 对于高频消息，使用简单的颜色格式即可

## 🌟 最佳实践

### 1. 按功能模块分组

**✅ 推荐做法**:
```yaml
# 按功能逻辑分组
death:
  cost_deducted: "<red>死亡扣除 {0} 金币</red>"
  insufficient_balance: "<red>余额不足</red>"
  broadcast_message: "<gray>玩家 {0} 死亡</gray>"

fly:
  started: "<green>飞行开始</green>"
  ended: "<red>飞行结束</red>"
  time_remaining: "<gray>剩余时间: {0}</gray>"
```

**❌ 不推荐做法**:
```yaml
# 随意命名，无逻辑分组
death_cost_message: "扣除金币"
fly_start_msg: "开始飞行"
some_error: "出错了"
```

### 2. 使用语义化键名

**✅ 推荐做法**:
```yaml
core:
  error:
    no_permission: "<red>无权限</red>"
    player_not_found: "<red>玩家不存在</red>"
    operation_failed: "<red>操作失败</red>"
```

**❌ 不推荐做法**:
```yaml
error:
  err1: "无权限"
  err2: "玩家不存在"
  msg3: "操作失败"
```

### 3. 保持一致的层级结构

**✅ 推荐做法**:
```yaml
commands:
  donate:
    success: "<green>捐赠成功</green>"
    error: "<red>捐赠失败</red>"
    info: "<gray>捐赠信息</gray>"

  transfer:
    success: "<green>转账成功</green>"
    error: "<red>转账失败</red>"
    info: "<gray>转账信息</gray>"
```

**❌ 不推荐做法**:
```yaml
commands:
  donate:
    ok: "成功"
    fail: "失败"

  transfer:
    success_message: "转账成功"
    error_info: "转账失败"
```

### 4. 合理使用MiniMessage格式

**✅ 推荐做法**:
```yaml
# 使用语义化的颜色
core:
  success:
    operation_completed: "<green>操作完成</green>"
  error:
    operation_failed: "<red>操作失败</red>"
  info:
    processing: "<gray>正在处理...</gray>"
```

**❌ 不推荐做法**:
```yaml
# 过度使用格式化
core:
  success:
    operation_completed: "<bold><green><underlined>操作完成</underlined></green></bold>"
```

### 5. 日志消息不使用颜色

**✅ 推荐做法**:
```yaml
log:
  info:
    plugin_loaded: "插件已加载"
    config_loaded: "配置已加载"
  error:
    database_error: "数据库连接失败: {0}"
```

**❌ 不推荐做法**:
```yaml
log:
  info:
    plugin_loaded: "<green>插件已加载</green>"
  error:
    database_error: "<red>数据库连接失败</red>"
```

## 📝 实际应用示例

### 完整的插件i18n结构示例

```yaml
# ============================================================================
# ExamplePlugin 多语言文件 (zh_CN.yml)
# ============================================================================

# 1. 核心系统层
core:
  plugin:
    enabled: "<green>示例插件已启用</green>"
    disabled: "<red>示例插件已禁用</red>"
    reload_completed: "<green>插件重载完成</green>"

  config:
    reloaded: "<green>配置已重载</green>"
    reload_failed: "<red>配置重载失败: {0}</red>"

  error:
    no_permission: "<red>您没有权限执行此操作</red>"
    player_not_found: "<red>找不到玩家: {0}</red>"
    operation_failed: "<red>操作失败: {0}</red>"

  success:
    operation_completed: "<green>操作成功完成</green>"

  validation:
    amount_invalid: "<red>金额无效，必须大于0</red>"
    parameter_missing: "<red>缺少参数: {0}</red>"

# 2. 命令系统层
commands:
  help:
    header: "<gold>===== 示例插件帮助 =====</gold>"
    usage: "<yellow>用法: {0}</yellow>"

  validation:
    command_not_found: "<red>命令不存在: {0}</red>"
    invalid_usage: "<red>用法错误，请使用 {0}</red>"

  reload:
    success: "<green>重载成功</green>"
    error: "<red>重载失败: {0}</red>"

  example:
    success: "<green>示例命令执行成功</green>"
    error: "<red>示例命令执行失败: {0}</red>"
    info: "<gray>示例命令信息: {0}</gray>"

# 3. 图形界面层
gui:
  common:
    back: "<white>返回</white>"
    close: "<red>关闭</red>"
    confirm: "<green>确认</green>"
    cancel: "<red>取消</red>"

  main_menu:
    title: "<gold>主菜单</gold>"
    button:
      settings: "<yellow>设置</yellow>"
      help: "<blue>帮助</blue>"
    hint:
      click_to_open: "<gray>点击打开</gray>"

# 4. 业务领域层
example:
  feature:
    enabled: "<green>功能已启用</green>"
    disabled: "<red>功能已禁用</red>"
    configured: "<green>功能已配置</green>"

  action:
    started: "<green>操作开始</green>"
    completed: "<green>操作完成</green>"
    failed: "<red>操作失败: {0}</red>"

  notification:
    player_joined: "<yellow>玩家 {0} 加入了游戏</yellow>"
    player_left: "<gray>玩家 {0} 离开了游戏</gray>"

# 5. 日志系统层
log:
  info:
    plugin_loaded: "示例插件已加载"
    feature_initialized: "功能已初始化: {0}"

  warn:
    config_outdated: "配置文件版本过旧"

  error:
    initialization_failed: "初始化失败: {0}"
    service_error: "服务错误: {0}"
```

## 🔧 LanguageKeys常量类模板

```kotlin
/**
 * 语言键常量类
 * 使用 <%key%> 格式，便于模板替换
 */
object LanguageKeys {

    // ==================== 核心系统 ====================
    object Core {
        object Plugin {
            // LOADING ENABLING 等应当使用英文硬编码，因为此时i18n模块还未加载
            const val ENABLED = "<%core.plugin.enabled%>"
            const val DISABLED = "<%core.plugin.disabled%>"
            const val RELOAD_COMPLETED = "<%core.plugin.reload_completed%>"
        }

        object Error {
            const val NO_PERMISSION = "<%core.error.no_permission%>"
            const val PLAYER_NOT_FOUND = "<%core.error.player_not_found%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
        }

        object Success {
            const val OPERATION_COMPLETED = "<%core.success.operation_completed%>"
        }

        object Validation {
            const val AMOUNT_INVALID = "<%core.validation.amount_invalid%>"
            const val PARAMETER_MISSING = "<%core.validation.parameter_missing%>"
        }
    }

    // ==================== 命令系统 ====================
    object Commands {
        object Help {
            const val HEADER = "<%commands.help.header%>"
            const val USAGE = "<%commands.help.usage%>"
        }

        object Validation {
            const val COMMAND_NOT_FOUND = "<%commands.validation.command_not_found%>"
            const val INVALID_USAGE = "<%commands.validation.invalid_usage%>"
        }

        object Reload {
            const val SUCCESS = "<%commands.reload.success%>"
            const val ERROR = "<%commands.reload.error%>"
        }
    }

    // ==================== 图形界面 ====================
    object Gui {
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val CLOSE = "<%gui.common.close%>"
            const val CONFIRM = "<%gui.common.confirm%>"
            const val CANCEL = "<%gui.common.cancel%>"
        }

        object MainMenu {
            const val TITLE = "<%gui.main_menu.title%>"

            object Button {
                const val SETTINGS = "<%gui.main_menu.button.settings%>"
                const val HELP = "<%gui.main_menu.button.help%>"
            }

            object Hint {
                const val CLICK_TO_OPEN = "<%gui.main_menu.hint.click_to_open%>"
            }
        }
    }

    // ==================== 业务领域 ====================
    object Example {
        object Feature {
            const val ENABLED = "<%example.feature.enabled%>"
            const val DISABLED = "<%example.feature.disabled%>"
            const val CONFIGURED = "<%example.feature.configured%>"
        }

        object Action {
            const val STARTED = "<%example.action.started%>"
            const val COMPLETED = "<%example.action.completed%>"
            const val FAILED = "<%example.action.failed%>"
        }

        object Notification {
            const val PLAYER_JOINED = "<%example.notification.player_joined%>"
            const val PLAYER_LEFT = "<%example.notification.player_left%>"
        }
    }

    // ==================== 日志系统 ====================
    object Log {
        object Info {
            const val PLUGIN_LOADED = "<%log.info.plugin_loaded%>"
            const val FEATURE_INITIALIZED = "<%log.info.feature_initialized%>"
        }

        object Warn {
            const val CONFIG_OUTDATED = "<%log.warn.config_outdated%>"
        }

        object Error {
            const val INITIALIZATION_FAILED = "<%log.error.initialization_failed%>"
            const val SERVICE_ERROR = "<%log.error.service_error%>"
        }
    }
}
```

## 🚀 使用示例

### 在插件中使用常量

```kotlin
class ExamplePlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用常量而非硬编码字符串
        logger.info(LanguageKeys.Log.Info.PLUGIN_LOADED)

        // 带参数的消息
        messager.info(player, LanguageKeys.Example.Notification.PLAYER_JOINED, player.name)

        // 错误处理
        try {
            // 某些操作
        } catch (e: Exception) {
            messager.error(player, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
        }
    }
}
```

### 在命令中使用

```kotlin
class ExampleCommand : BaseCommand {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("example.use")) {
            messager.error(sender, LanguageKeys.Core.Error.NO_PERMISSION)
            return
        }

        messager.success(sender, LanguageKeys.Commands.Example.SUCCESS)
    }
}
```

## 📊 检查清单

在实现i18n时，请确保：

- [ ] 遵循五层架构分类
- [ ] 使用语义化键名
- [ ] 保持一致的命名约定
- [ ] 创建LanguageKeys常量类
- [ ] 使用MiniMessage格式
- [ ] 日志消息不使用颜色
- [ ] 提供中英文两套完整翻译
- [ ] 使用位置参数传递动态值
- [ ] 定期检查和更新翻译文件
- [ ] 测试所有键值是否正确显示

## 🔗 相关资源

- [通用模板文件](template.yml) - 中文模板
- [英文模板文件](template_en.yml) - 英文模板
- [Core模块最佳实践](../core/best-practices.md) - 核心模块规范
- [i18n模块文档](README.md) - 完整i18n文档

---

遵循这些规范，您的插件将拥有一致、清晰、易维护的多语言支持系统。