# ExternalBook Plugin

现代化的Minecraft书籍管理插件，提供书籍导入、导出、发布和管理功能。

## 功能特性

### 📚 书籍管理
- **导入书籍** - 将书与笔或成书导入到图书馆系统
- **导出原书** - 导出可编辑的原始书籍进行修改
- **发布成书** - 将书籍发布为不可编辑的成书
- **解绑书籍** - 将书籍从图书馆系统中解绑

### 🎮 用户界面
- **现代化GUI** - 基于项目GUI模块的直观界面
- **分页显示** - 支持大量书籍的分页浏览
- **实时编辑** - 在GUI中直接编辑书籍信息
- **玩家管理** - 管理员可查看所有玩家的书籍

### 🔧 高级功能
- **自动缓存** - 智能缓存系统提升性能
- **权限控制** - 细粒度的权限管理
- **多语言支持** - 支持中文和英文
- **自动备份** - 定期备份书籍数据

## 安装要求

- **Minecraft版本**: 1.20.1+
- **服务端**: Paper/Purpur
- **依赖插件**: CommandAPI

## 安装步骤

1. 下载插件jar文件
2. 将jar文件放入服务器的`plugins`文件夹
3. 确保已安装CommandAPI插件
4. 重启服务器
5. 插件将自动生成配置文件

## 命令使用

### 基础命令
```
/book                    # 打开书籍管理GUI
/book help              # 显示帮助信息
/book gui               # 打开书籍管理GUI
```

### 书籍操作
```
/book import            # 导入手中的书籍
/book export            # 导出书籍为可编辑版本
/book publish           # 发布书籍为成书
/book strip             # 解绑书籍
```

### 管理员命令
```
/book admin             # 打开管理员界面
/book reload            # 重载插件配置
/book open <玩家> <UUID> # 为指定玩家打开书籍
```

## 权限节点

### 基础权限
- `externalbook.use` - 使用基本功能 (默认: true)
- `externalbook.admin` - 管理员权限 (默认: op)
- `externalbook.reload` - 重载配置权限 (默认: op)

### 高级权限
- `externalbook.bypass` - 绕过作者权限检查 (默认: op)
- `externalbook.open` - 打开指定书籍给玩家 (默认: op)
- `externalbook.*` - 所有权限

## 配置文件

### 存储配置

插件支持两种存储方式：

#### JSON文件存储（默认）
适合小型服务器，配置简单：
```yaml
storage:
  mode: "json"
  json_storage:
    backup_enabled: true
    backup_interval_hours: 24
    max_backup_files: 7
```

#### MySQL数据库存储
适合大型服务器，支持高并发：
```yaml
storage:
  mode: "mysql"
  mysql_storage:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "your_password"
    # 自定义表名（可选，默认为 "external_books"）
    table_name: "custom_books_table"
    # 表前缀（最终表名 = table_prefix + table_name）
    table_prefix: "externalbook_"
    pool_settings:
      max_pool_size: 10
      min_idle: 2
      connection_timeout_ms: 30000
      idle_timeout_ms: 600000
      max_lifetime_ms: 1800000
```

#### MySQL表名配置
- `table_name`: 自定义数据表名称
  - 如果设置为 `null` 或留空，将使用默认表名 `external_books`
  - 支持自定义表名以适应不同的数据库命名规范
- `table_prefix`: 表名前缀
  - 默认为 `externalbook_`，用于避免与其他插件的表名冲突
  - 最终完整表名 = `table_prefix` + `table_name`
  - 例如：默认情况下最终表名为 `externalbook_external_books`
- 表名会自动加上反引号以支持特殊字符

### 其他配置选项

#### 图书馆设置
```yaml
library:
  cache_duration_minutes: 30        # 缓存持续时间
  auto_backup: true                 # 自动备份
  backup_interval_hours: 24         # 备份间隔
  max_books_per_player: 100         # 每个玩家最大书籍数
  publisher_name: "§8§l[牛腩书局出版社]§r"
```

#### 权限配置
```yaml
permissions:
  require_author_permission: true   # 需要作者权限
  bypass_permission: "externalbook.bypass"
  admin_permission: "externalbook.admin"
```

## 使用指南

### 1. 导入书籍
1. 手持书与笔或成书
2. 执行 `/book import` 命令
3. 书籍将被导入到图书馆系统

### 2. 编辑书籍
1. 打开GUI界面 `/book gui`
2. 右键点击要编辑的书籍
3. 在编辑界面中修改内容
4. 保存更改

### 3. 发布书籍
1. 手持已导入的书与笔
2. 执行 `/book publish` 命令
3. 获得发布的成书

### 4. 管理员功能
1. 使用 `/book admin` 打开管理界面
2. 选择要管理的玩家
3. 查看和管理该玩家的所有书籍

## 技术特性

### 架构设计
- 基于现代化的BasePlugin架构
- 使用CommandAPI进行命令处理
- 集成项目GUI模块
- 支持国际化和配置管理
- 完整的权限管理系统

### 性能优化
- 智能缓存系统
- 异步数据加载
- 分页显示大量数据
- 自动资源清理
- 数据完整性验证

### 数据存储
- **多种存储方式** - 支持JSON文件和MySQL数据库存储
- **JSON存储** - 适合小型服务器，数据存储在文件中
- **MySQL存储** - 适合大型服务器，支持高并发和分布式部署
- **自动备份机制** - 定期备份重要数据
- **数据完整性检查** - 自动验证数据一致性
- **支持数据迁移** - 在不同存储方式间迁移数据
- **安全的书籍删除** - 删除的书籍移动到备份目录

## 故障排除

### 常见问题

**Q: 书籍导入失败**
A: 确保手持的是书与笔或成书，且有相应权限。检查控制台是否有错误日志

**Q: GUI界面无法打开**
A: 检查是否安装了所有依赖模块，重启服务器。确认玩家有GUI使用权限

**Q: 权限问题**
A: 确认玩家有相应权限，管理员可使用bypass权限。查看权限节点列表

**Q: 数据丢失**
A: 检查`plugins/ExternalBook/removed`文件夹中的备份。插件会自动备份删除的书籍

**Q: 命令执行失败**
A: 检查CommandAPI插件是否正确安装，确认plugin.yml中的依赖声明

### 日志调试
启用调试模式：
```yaml
core:
  logging:
    enable_debug: true
```

## 开发信息

- **版本**: 1.0.0
- **作者**: NewNanCity
- **许可**: MIT License
- **源码**: [GitHub Repository]

## 更新日志

### v1.0.0
- 初始版本发布
- 完整的书籍管理功能
- 现代化GUI界面
- 多语言支持
- 权限系统

## 支持

如有问题或建议，请：
1. 查看本文档的故障排除部分
2. 检查服务器日志
3. 联系开发团队

---

**感谢使用ExternalBook插件！**
