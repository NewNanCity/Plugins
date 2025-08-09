# GitHub Wiki 自动同步 PowerShell 工具
# 提供便捷的命令来管理 Wiki 文档

param(
    [Parameter(Position=0)]
    [string]$Command = "help"
)

function Show-Help {
    Write-Host "📚 GitHub Wiki 管理命令" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "可用命令:" -ForegroundColor Yellow
    Write-Host "  test-wiki     - 完整测试 Wiki 文档生成"
    Write-Host "  prepare-wiki  - 生成 Wiki 文档"
    Write-Host "  preview-wiki  - 预览生成的 Wiki 文档"
    Write-Host "  validate-wiki - 验证链接和文件"
    Write-Host "  clean-wiki    - 清理生成的文件"
    Write-Host "  sync-wiki     - 提交并触发同步"
    Write-Host "  status        - 检查状态"
    Write-Host "  stats         - 显示统计信息"
    Write-Host ""
    Write-Host "使用方法:" -ForegroundColor Yellow
    Write-Host "  .\wiki-tools.ps1 test-wiki"
    Write-Host "  .\wiki-tools.ps1 sync-wiki"
    Write-Host ""
    Write-Host "开发流程:" -ForegroundColor Green
    Write-Host "  1. .\wiki-tools.ps1 test-wiki    # 本地测试"
    Write-Host "  2. .\wiki-tools.ps1 sync-wiki    # 提交同步"
    Write-Host ""
}

function Test-Wiki {
    Write-Host "🧪 运行完整 Wiki 测试..." -ForegroundColor Cyan
    python scripts/test-wiki-locally.py
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 测试完成!" -ForegroundColor Green
    } else {
        Write-Host "❌ 测试失败!" -ForegroundColor Red
        exit 1
    }
}

function Prepare-Wiki {
    Write-Host "📝 生成 Wiki 文档..." -ForegroundColor Cyan
    python scripts/prepare-wiki-multi.py
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 文档生成完成!" -ForegroundColor Green
    } else {
        Write-Host "❌ 文档生成失败!" -ForegroundColor Red
        exit 1
    }
}

function Preview-Wiki {
    Write-Host "📖 预览 Wiki 文档..." -ForegroundColor Cyan
    python scripts/test-wiki-locally.py preview
}

function Validate-Wiki {
    Write-Host "🔍 验证 Wiki 文档..." -ForegroundColor Cyan
    python scripts/test-wiki-locally.py test
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ 验证完成!" -ForegroundColor Green
    } else {
        Write-Host "❌ 验证失败!" -ForegroundColor Red
        exit 1
    }
}

function Clean-Wiki {
    Write-Host "🧹 清理 Wiki 文件..." -ForegroundColor Cyan
    python scripts/test-wiki-locally.py clean
    Write-Host "✅ 清理完成!" -ForegroundColor Green
}

function Sync-Wiki {
    Write-Host "🚀 准备同步到 GitHub Wiki..." -ForegroundColor Cyan

    Write-Host "1. 运行本地测试..." -ForegroundColor Yellow
    Test-Wiki
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ 本地测试失败，取消同步" -ForegroundColor Red
        exit 1
    }

    Write-Host ""
    Write-Host "2. 添加文档更改..." -ForegroundColor Yellow
    git add docs/gui/

    Write-Host ""
    Write-Host "3. 提交更改..." -ForegroundColor Yellow
    $commitMsg = Read-Host "请输入提交信息"
    if ([string]::IsNullOrWhiteSpace($commitMsg)) {
        $commitMsg = "更新 GUI 文档"
    }

    git commit -m "📚 更新 GUI 文档: $commitMsg"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ℹ️ 没有更改需要提交" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "4. 推送到远程仓库..." -ForegroundColor Yellow
    git push origin main

    Write-Host ""
    Write-Host "✅ 同步完成! GitHub Action 将自动更新 Wiki" -ForegroundColor Green

    # 尝试获取仓库URL
    $repoUrl = git config --get remote.origin.url
    if ($repoUrl -match "github\.com[:/]([^.]+)") {
        $repoPath = $matches[1]
        Write-Host "🔗 查看 Actions: https://github.com/$repoPath/actions" -ForegroundColor Blue
    }
}

function Show-Status {
    Write-Host "📊 Wiki 状态检查" -ForegroundColor Cyan
    Write-Host ""

    Write-Host "📁 源文档文件:" -ForegroundColor Yellow
    $sourceFiles = Get-ChildItem "docs/**/*.md" -Recurse -ErrorAction SilentlyContinue
    if ($sourceFiles) {
        Write-Host "  文件数量: $($sourceFiles.Count)"
        $totalSize = ($sourceFiles | Measure-Object -Property Length -Sum).Sum
        Write-Host "  总大小: $([math]::Round($totalSize/1KB, 2)) KB"

        # 按模块统计
        $modules = $sourceFiles | Group-Object { $_.Directory.Name }
        Write-Host "  模块分布:"
        foreach ($module in $modules) {
            Write-Host "    - $($module.Name): $($module.Count) 个文件"
        }
    } else {
        Write-Host "  ❌ 未找到源文档文件" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "📄 生成的 Wiki 文件:" -ForegroundColor Yellow
    if (Test-Path "wiki") {
        $wikiFiles = Get-ChildItem "wiki/*.md" -ErrorAction SilentlyContinue
        if ($wikiFiles) {
            Write-Host "  文件数量: $($wikiFiles.Count)"
            $totalSize = ($wikiFiles | Measure-Object -Property Length -Sum).Sum
            Write-Host "  总大小: $([math]::Round($totalSize/1KB, 2)) KB"
        } else {
            Write-Host "  ❌ Wiki 目录为空"
        }
    } else {
        Write-Host "  📄 Wiki 文件: 未生成 (运行 prepare-wiki)"
    }

    Write-Host ""
    Write-Host "🔧 Git 状态:" -ForegroundColor Yellow
    $gitStatus = git status --porcelain docs/gui/ 2>$null
    if ($gitStatus) {
        Write-Host "  未提交更改: $($gitStatus.Count) 个文件"
    } else {
        Write-Host "  未提交更改: 0 个文件"
    }
    Write-Host ""
}

function Show-Stats {
    Write-Host "📈 Wiki 文档统计" -ForegroundColor Cyan
    Write-Host ""

    Write-Host "📚 文档概览:" -ForegroundColor Yellow
    $mdFiles = Get-ChildItem "docs/gui/*.md" -ErrorAction SilentlyContinue
    if ($mdFiles) {
        $totalLines = 0
        $totalWords = 0

        foreach ($file in $mdFiles) {
            $content = Get-Content $file.FullName -ErrorAction SilentlyContinue
            if ($content) {
                $totalLines += $content.Count
                $totalWords += ($content -join " ").Split(" ", [StringSplitOptions]::RemoveEmptyEntries).Count
            }
        }

        Write-Host "  文件数: $($mdFiles.Count)"
        Write-Host "  总行数: $totalLines"
        Write-Host "  总词数: $totalWords"
    } else {
        Write-Host "  ❌ 未找到文档文件" -ForegroundColor Red
    }

    Write-Host ""
    Write-Host "🔗 链接统计:" -ForegroundColor Yellow
    if ($mdFiles) {
        $totalLinks = 0
        $internalLinks = 0

        foreach ($file in $mdFiles) {
            $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
            if ($content) {
                $links = [regex]::Matches($content, '\[.*?\]\(.*?\)')
                $totalLinks += $links.Count

                $internalLinksInFile = [regex]::Matches($content, '\[.*?\]\([^)]*\.md\)')
                $internalLinks += $internalLinksInFile.Count
            }
        }

        Write-Host "  总链接数: $totalLinks"
        Write-Host "  内部链接: $internalLinks"
    }

    Write-Host ""
    Write-Host "📝 内容分布:" -ForegroundColor Yellow

    $basicFiles = @("README.md", "intro.md", "quick-start.md", "concepts.md")
    $basicCount = ($basicFiles | Where-Object { Test-Path "docs/gui/$_" }).Count
    Write-Host "  基础教程: $basicCount/4"

    $guideFiles = @("basic-gui.md", "paginated-gui.md", "scrolling-gui.md", "storage-gui.md",
                   "session-management.md", "task-system.md", "layout-schemes.md",
                   "event-handling.md", "chat-input.md")
    $guideCount = ($guideFiles | Where-Object { Test-Path "docs/gui/$_" }).Count
    Write-Host "  功能指南: $guideCount/9"

    $advancedFiles = @("lifecycle.md", "architecture.md", "configuration.md")
    $advancedCount = ($advancedFiles | Where-Object { Test-Path "docs/gui/$_" }).Count
    Write-Host "  高级主题: $advancedCount/3"

    $refFiles = @("api-reference.md", "best-practices.md", "troubleshooting.md", "examples.md")
    $refCount = ($refFiles | Where-Object { Test-Path "docs/gui/$_" }).Count
    Write-Host "  参考资料: $refCount/4"
}

function Install-Dependencies {
    Write-Host "📦 检查依赖..." -ForegroundColor Cyan

    # 检查 Python
    try {
        $pythonVersion = python --version 2>$null
        Write-Host "✅ Python 已安装: $pythonVersion" -ForegroundColor Green
    } catch {
        Write-Host "❌ 需要安装 Python 3.7+" -ForegroundColor Red
        exit 1
    }

    # 检查 Git
    try {
        $gitVersion = git --version 2>$null
        Write-Host "✅ Git 已安装: $gitVersion" -ForegroundColor Green
    } catch {
        Write-Host "❌ 需要安装 Git" -ForegroundColor Red
        exit 1
    }

    Write-Host "✅ 所有依赖已满足" -ForegroundColor Green
}

# 主逻辑
switch ($Command.ToLower()) {
    "help" { Show-Help }
    "test-wiki" { Test-Wiki }
    "prepare-wiki" { Prepare-Wiki }
    "preview-wiki" { Preview-Wiki }
    "validate-wiki" { Validate-Wiki }
    "clean-wiki" { Clean-Wiki }
    "sync-wiki" { Sync-Wiki }
    "status" { Show-Status }
    "stats" { Show-Stats }
    "install" { Install-Dependencies }
    default {
        Write-Host "❌ 未知命令: $Command" -ForegroundColor Red
        Write-Host ""
        Show-Help
        exit 1
    }
}
