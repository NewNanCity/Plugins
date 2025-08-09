#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
语言模板分析脚本

该脚本用于分析项目中的语言模板使用情况，检查:
1. 代码中使用的语言键
2. 语言文件中定义的键
3. 缺失的键（代码中使用但语言文件中未定义）
4. 冗余的键（语言文件中定义但代码中未使用）
5. i18n最佳实践合规性检查

@author Gk0Wk
@since 1.0.0
"""

import re
import yaml
from dataclasses import dataclass
from pathlib import Path
import argparse
import shutil
from datetime import datetime
import sys
import os

# 设置Windows控制台编码为UTF-8
if sys.platform == "win32":
    os.system("chcp 65001 > nul")


class MultilineDumper(yaml.SafeDumper):
    """自定义YAML Dumper，多行字符串使用|格式"""

    def represent_str(self, data):
        # 如果字符串包含换行符，使用 literal 样式 (|)
        if "\n" in data:
            return self.represent_scalar("tag:yaml.org,2002:str", data, style="|")
        return self.represent_scalar("tag:yaml.org,2002:str", data)


# 注册自定义的字符串表示器
MultilineDumper.add_representer(str, MultilineDumper.represent_str)


@dataclass
class LanguageAnalysisResult:
    """语言分析结果"""

    plugin_name: str
    language_file: str
    used_keys: set[str]
    defined_keys: set[str]
    missing_keys: set[str]
    redundant_keys: set[str]


@dataclass
class I18nBestPracticesResult:
    """i18n最佳实践检查结果"""

    plugin_name: str
    has_language_keys_file: bool
    language_keys_file_path: str | None
    direct_template_usage: list[str]  # 直接使用<%xxx%>的文件
    best_practices_violations: list[str]  # 最佳实践违规项
    score: float  # 合规性评分 (0-100)


class LanguageAnalyzer:
    """语言模板分析器"""

    def __init__(self):
        # 语言模板匹配模式：<%path.path.key%>
        # 修正正则表达式，匹配字母、数字、下划线和点号
        self.template_pattern = re.compile(r"<%([a-zA-Z0-9_.]+)%>")

        # 支持的代码文件扩展名
        self.code_extensions = {".kt", ".java"}

        # 支持的语言文件扩展名
        self.lang_extensions = {".yml", ".yaml"}

    def analyze_project(
        self, project_root: Path, target_plugins: list[str] | None = None
    ) -> tuple[list[LanguageAnalysisResult], list[I18nBestPracticesResult]]:
        """分析整个项目"""
        results = []
        best_practices_results = []

        print(f"开始分析项目: {project_root.absolute()}")
        if target_plugins:
            print(f"仅分析指定插件: {', '.join(target_plugins)}")
        print("=" * 80)

        # 找到所有插件目录
        plugins_dir = project_root / "plugins"
        if not plugins_dir.exists() or not plugins_dir.is_dir():
            print("错误: 未找到 plugins 目录")
            return results, best_practices_results

        # 遍历每个插件
        for plugin_dir in plugins_dir.iterdir():
            if plugin_dir.is_dir() and (plugin_dir.name != "build"):
                # 如果指定了插件列表，只分析指定的插件
                if target_plugins and plugin_dir.name not in target_plugins:
                    continue

                plugin_results = self._analyze_plugin(plugin_dir)
                results.extend(plugin_results)

                # 进行i18n最佳实践检查
                best_practices_result = self._check_i18n_best_practices(plugin_dir)
                best_practices_results.append(best_practices_result)

        return results, best_practices_results

    def _check_i18n_best_practices(self, plugin_dir: Path) -> I18nBestPracticesResult:
        """检查i18n最佳实践合规性"""
        plugin_name = plugin_dir.name
        violations = []
        score = 100.0

        # 检查是否有LanguageKeys.kt文件
        language_keys_file = None
        i18n_dir = plugin_dir / "src" / "main" / "kotlin"
        language_keys_files = list(i18n_dir.rglob("**/i18n/LanguageKeys.kt"))

        has_language_keys_file = len(language_keys_files) > 0
        if has_language_keys_file:
            language_keys_file = str(language_keys_files[0])
        else:
            violations.append("缺少 i18n/LanguageKeys.kt 文件")
            score -= 40

        # 检查直接使用<%xxx%>的文件
        direct_template_files = self._find_direct_template_usage(plugin_dir)
        if direct_template_files:
            violations.append(
                f"发现 {len(direct_template_files)} 个文件直接使用 <%xxx%> 而不是 LanguageKeys 常量"
            )
            score -= len(direct_template_files) * 10

        # 检查是否有LanguageKeys的import但没有使用
        if has_language_keys_file:
            language_keys_usage = self._check_language_keys_usage(plugin_dir)
            if not language_keys_usage:
                violations.append("有 LanguageKeys.kt 文件但没有在代码中使用")
                score -= 20

        # 检查LanguageKeys文件的结构规范
        if has_language_keys_file:
            structure_violations = self._check_language_keys_structure(
                Path(language_keys_file)
            )
            violations.extend(structure_violations)
            score -= len(structure_violations) * 5

        # 确保评分不小于0
        score = max(0, score)

        return I18nBestPracticesResult(
            plugin_name=plugin_name,
            has_language_keys_file=has_language_keys_file,
            language_keys_file_path=language_keys_file,
            direct_template_usage=direct_template_files,
            best_practices_violations=violations,
            score=score,
        )

    def _find_direct_template_usage(self, plugin_dir: Path) -> list[str]:
        """查找直接使用<%xxx%>模板的文件"""
        direct_usage_files = []

        def traverse_code_files(directory: Path):
            try:
                for item in directory.iterdir():
                    if item.is_dir():
                        traverse_code_files(item)
                    elif item.is_file() and item.suffix.lower() in self.code_extensions:
                        # 跳过LanguageKeys.kt文件本身
                        if item.name == "LanguageKeys.kt":
                            continue

                        if self._file_has_direct_template_usage(item):
                            direct_usage_files.append(str(item.relative_to(plugin_dir)))
            except PermissionError:
                pass

        src_dir = plugin_dir / "src"
        if src_dir.exists():
            traverse_code_files(src_dir)

        return direct_usage_files

    def _file_has_direct_template_usage(self, file_path: Path) -> bool:
        """检查文件是否直接使用<%xxx%>模板"""
        try:
            with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()

            # 查找<%xxx%>模式
            template_matches = self.template_pattern.findall(content)
            if not template_matches:
                return False

            # 检查是否有LanguageKeys的import
            has_language_keys_import = "LanguageKeys" in content

            # 如果有模板但没有LanguageKeys import，认为是直接使用
            return not has_language_keys_import

        except Exception:
            return False

    def _check_language_keys_usage(self, plugin_dir: Path) -> bool:
        """检查LanguageKeys是否在代码中被使用"""

        def traverse_code_files(directory: Path) -> bool:
            try:
                for item in directory.iterdir():
                    if item.is_dir():
                        if traverse_code_files(item):
                            return True
                    elif item.is_file() and item.suffix.lower() in self.code_extensions:
                        # 跳过LanguageKeys.kt文件本身
                        if item.name == "LanguageKeys.kt":
                            continue

                        try:
                            with open(
                                item, "r", encoding="utf-8", errors="ignore"
                            ) as f:
                                content = f.read()
                                if "LanguageKeys." in content:
                                    return True
                        except Exception:
                            pass
            except PermissionError:
                pass
            return False

        src_dir = plugin_dir / "src"
        if src_dir.exists():
            return traverse_code_files(src_dir)
        return False

    def _check_language_keys_structure(self, language_keys_file: Path) -> list[str]:
        """检查LanguageKeys文件的结构规范"""
        violations = []

        try:
            with open(language_keys_file, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()

            # 检查是否有五层架构注释
            if "五层架构" not in content:
                violations.append("LanguageKeys.kt 缺少五层架构分类说明")

            # 检查是否有object LanguageKeys声明
            if "object LanguageKeys" not in content:
                violations.append("LanguageKeys.kt 应该使用 object LanguageKeys 声明")

            # 检查是否有标准的分层对象
            expected_objects = ["Core", "Commands", "Gui", "Events", "Log"]
            for obj in expected_objects:
                if f"object {obj}" not in content:
                    violations.append(f"LanguageKeys.kt 缺少 {obj} 对象分类")

            # 检查常量值格式
            const_pattern = re.compile(r'const val \w+ = "(.*?)"')
            const_matches = const_pattern.findall(content)

            for const_value in const_matches:
                if const_value.startswith("<%") and const_value.endswith("%>"):
                    continue  # 正确的格式
                elif const_value == "Reloading ExternalBook plugin...":
                    continue  # 允许的英文常量
                else:
                    violations.append(f"常量值 '{const_value}' 应该使用 <%xxx%> 格式")

        except Exception as e:
            violations.append(f"读取 LanguageKeys.kt 文件失败: {e}")

        return violations

    def _analyze_plugin(self, plugin_dir: Path) -> list[LanguageAnalysisResult]:
        """分析单个插件"""
        results = []
        plugin_name = plugin_dir.name

        # 查找代码文件中使用的语言键
        used_keys = self._find_used_keys(plugin_dir)
        # print(f"在代码中找到 {len(used_keys)} 个语言键")

        # if used_keys:
        #     print(f"使用的键: {', '.join(sorted(used_keys))}")

        # 查找语言文件
        lang_dir = plugin_dir / "src" / "main" / "resources" / "lang"
        if not lang_dir.exists() or not lang_dir.is_dir():
            print(f"警告: 未找到语言文件目录 {lang_dir}")
            if used_keys:
                results.append(
                    LanguageAnalysisResult(
                        plugin_name=plugin_name,
                        language_file="无语言文件",
                        used_keys=used_keys,
                        defined_keys=set(),
                        missing_keys=used_keys,
                        redundant_keys=set(),
                    )
                )
            return results

        # 分析每个语言文件
        for lang_file in lang_dir.iterdir():
            if (
                lang_file.is_file()
                and lang_file.suffix.lower() in self.lang_extensions
                and not lang_file.name.startswith(".")
                and "backup" not in lang_file.name.lower()
            ):
                print(f"\n分析语言文件: {lang_file.name}")

                defined_keys = self._parse_lang_file(lang_file)
                print(f"语言文件中定义了 {len(defined_keys)} 个键")

                missing_keys = used_keys - defined_keys
                redundant_keys = defined_keys - used_keys

                print(f"缺失的键: {len(missing_keys)} 个")
                if missing_keys:
                    for key in sorted(missing_keys):
                        print(f"  - {key}")

                print(f"冗余的键: {len(redundant_keys)} 个")
                if redundant_keys:
                    for key in sorted(redundant_keys):
                        print(f"  - {key}")

                results.append(
                    LanguageAnalysisResult(
                        plugin_name=plugin_name,
                        language_file=lang_file.name,
                        used_keys=used_keys,
                        defined_keys=defined_keys,
                        missing_keys=missing_keys,
                        redundant_keys=redundant_keys,
                    )
                )

        return results

    def _find_used_keys(self, plugin_dir: Path) -> set[str]:
        """在插件的所有代码文件中查找使用的语言键"""
        used_keys = set()

        def traverse_code_files(directory: Path):
            """递归遍历代码文件"""
            try:
                for item in directory.iterdir():
                    if item.is_dir():
                        traverse_code_files(item)
                    elif item.is_file() and item.suffix.lower() in self.code_extensions:
                        keys = self._extract_keys_from_file(item)
                        used_keys.update(keys)
            except PermissionError:
                print(f"警告: 无权限访问目录 {directory}")

        # 从 src 目录开始遍历
        src_dir = plugin_dir / "src"
        if src_dir.exists():
            traverse_code_files(src_dir)

        # 也检查 bin 目录（编译后的代码）
        bin_dir = plugin_dir / "bin"
        if bin_dir.exists():
            traverse_code_files(bin_dir)

        # 去掉一些在注释中可能出现的键
        used_keys -= {
            "key",
            "...",
            "..",
            "xxx",
            "xx",
        }

        return used_keys

    def _extract_keys_from_file(self, file_path: Path) -> set[str]:
        """从单个代码文件中提取语言键"""
        keys = set()

        try:
            with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                matches = self.template_pattern.findall(content)
                keys.update(matches)
        except Exception as e:
            print(f"警告: 读取文件失败 {file_path}: {e}")

        return keys

    def _parse_lang_file(self, file_path: Path) -> set[str]:
        """解析语言文件，提取所有定义的键"""
        keys = set()

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = yaml.safe_load(f)
                if data:
                    self._extract_keys_from_yaml(data, "", keys)
        except Exception as e:
            print(f"警告: 解析语言文件失败 {file_path}: {e}")

        return keys

    def _extract_keys_from_yaml(self, data, prefix: str, keys: set[str]):
        """递归提取 YAML 中的所有键路径"""
        if isinstance(data, dict):
            for key, value in data.items():
                key_str = str(key)
                full_key = key_str if not prefix else f"{prefix}.{key_str}"

                if isinstance(value, (dict, list)):
                    self._extract_keys_from_yaml(value, full_key, keys)
                else:
                    keys.add(full_key)
        elif isinstance(data, list):
            for index, value in enumerate(data):
                self._extract_keys_from_yaml(value, f"{prefix}[{index}]", keys)

    def remove_redundant_keys(
        self, results: list[LanguageAnalysisResult], backup: bool = True
    ):
        """删除冗余的键"""
        if not results:
            print("没有找到任何结果，无需删除")
            return

        total_removed = 0

        for result in results:
            if not result.redundant_keys:
                continue

            # 构建语言文件路径
            plugin_dir = Path("plugins") / result.plugin_name
            lang_dir = plugin_dir / "src" / "main" / "resources" / "lang"
            lang_file = lang_dir / result.language_file

            if not lang_file.exists():
                print(f"警告: 语言文件不存在 {lang_file}")
                continue

            print(f"\n处理文件: {lang_file}")
            print(f"将删除 {len(result.redundant_keys)} 个冗余键")

            # 备份原文件
            if backup:
                backup_file = lang_file.with_suffix(
                    f".backup.{datetime.now().strftime('%Y%m%d_%H%M%S')}.yml"
                )
                shutil.copy2(lang_file, backup_file)
                print(f"已备份到: {backup_file}")

            # 读取并解析YAML文件
            try:
                with open(lang_file, "r", encoding="utf-8") as f:
                    data = yaml.safe_load(f)

                if not data:
                    print(f"警告: 文件为空或无法解析 {lang_file}")
                    continue

                # 删除冗余键
                removed_count = self._remove_keys_from_yaml(data, result.redundant_keys)

                # 写回文件
                with open(lang_file, "w", encoding="utf-8") as f:
                    yaml.dump(
                        data,
                        f,
                        Dumper=MultilineDumper,
                        default_flow_style=False,
                        allow_unicode=True,
                        sort_keys=False,
                        indent=2,
                        width=120,
                    )

                print(f"成功删除 {removed_count} 个键")
                total_removed += removed_count

            except Exception as e:
                print(f"错误: 处理文件失败 {lang_file}: {e}")

        print(f"\n总计删除了 {total_removed} 个冗余键")

    def _remove_keys_from_yaml(self, data: dict, keys_to_remove: set[str]) -> int:
        """从YAML数据中删除指定的键"""
        removed_count = 0

        for key_path in keys_to_remove:
            if self._remove_key_by_path(data, key_path):
                removed_count += 1
                print(f"  - 删除键: {key_path}")

        return removed_count

    def _remove_key_by_path(self, data: dict, key_path: str) -> bool:
        """根据路径删除键"""
        parts = key_path.split(".")
        current = data

        # 导航到父级
        for part in parts[:-1]:
            if not isinstance(current, dict) or part not in current:
                return False
            current = current[part]

        # 删除最后一级键
        last_key = parts[-1]
        if isinstance(current, dict) and last_key in current:
            del current[last_key]

            # 如果父级字典变空了，递归删除空的父级
            self._cleanup_empty_dicts(data, parts[:-1])
            return True

        return False

    def _cleanup_empty_dicts(self, data: dict, path_parts: list[str]):
        """清理空的字典"""
        if not path_parts:
            return

        current = data
        for part in path_parts[:-1]:
            if not isinstance(current, dict) or part not in current:
                return
            current = current[part]

        last_key = path_parts[-1]
        if (
            isinstance(current, dict)
            and last_key in current
            and isinstance(current[last_key], dict)
            and not current[last_key]
        ):
            del current[last_key]
            # 递归清理上级
            self._cleanup_empty_dicts(data, path_parts[:-1])

    def generate_report(
        self,
        results: list[LanguageAnalysisResult],
        best_practices_results: list[I18nBestPracticesResult],
    ):
        """生成分析报告"""
        print("\n" + "=" * 80)
        print("语言分析报告")
        print("=" * 80)

        if not results:
            print("没有找到任何结果")
            return

        total_missing = 0
        total_redundant = 0

        # 按插件分组
        plugins = {}
        for result in results:
            if result.plugin_name not in plugins:
                plugins[result.plugin_name] = []
            plugins[result.plugin_name].append(result)

        for plugin_name, plugin_results in plugins.items():
            print(f"\n插件: {plugin_name}")
            print("-" * 60)

            for result in plugin_results:
                print(f"\n语言文件: {result.language_file}")
                print(f"  使用的键: {len(result.used_keys)}")
                print(f"  定义的键: {len(result.defined_keys)}")

                if result.missing_keys:
                    print(f"  ❌ 缺失的键 ({len(result.missing_keys)}):")
                    for key in sorted(result.missing_keys):
                        print(f"    - {key}")
                    total_missing += len(result.missing_keys)

                if result.redundant_keys:
                    print(f"  ⚠️ 冗余的键 ({len(result.redundant_keys)}):")
                    for key in sorted(result.redundant_keys):
                        print(f"    - {key}")
                    total_redundant += len(result.redundant_keys)

                if not result.missing_keys and not result.redundant_keys:
                    print("  [OK] 没有发现问题")

        print("\n" + "=" * 80)
        print("总结:")
        print(f"  分析的插件数: {len(plugins)}")
        print(f"  分析的语言文件数: {len(results)}")
        print(f"  总缺失键数: {total_missing}")
        print(f"  总冗余键数: {total_redundant}")

        if total_missing == 0 and total_redundant == 0:
            print("  [OK] 所有语言文件都完美匹配!")
        else:
            print("  [--] 建议修复上述问题以完善国际化支持")

        # 生成i18n最佳实践报告
        self.generate_best_practices_report(best_practices_results)

        print("=" * 80)

    def generate_best_practices_report(
        self, best_practices_results: list[I18nBestPracticesResult]
    ):
        """生成i18n最佳实践报告"""
        print("\n" + "=" * 80)
        print("i18n 最佳实践合规性报告")
        print("=" * 80)

        if not best_practices_results:
            print("没有找到任何插件")
            return

        total_plugins = len(best_practices_results)
        compliant_plugins = sum(1 for r in best_practices_results if r.score >= 90)
        average_score = sum(r.score for r in best_practices_results) / total_plugins

        print(f"总插件数: {total_plugins}")
        print(f"合规插件数 (≥90分): {compliant_plugins}")
        print(f"平均合规分数: {average_score:.1f}")
        print()

        # 按分数排序
        sorted_results = sorted(
            best_practices_results, key=lambda x: x.score, reverse=True
        )

        for result in sorted_results:
            print(f"插件: {result.plugin_name}")
            print(f"  合规分数: {result.score:.1f}/100")

            if result.has_language_keys_file:
                print(
                    f"  [OK] 有 LanguageKeys.kt 文件: {result.language_keys_file_path}"
                )
            else:
                print("  [NO] 缺少 LanguageKeys.kt 文件")

            if result.direct_template_usage:
                print(
                    f"  [!!] 直接使用 <%xxx%> 的文件 ({len(result.direct_template_usage)} 个):"
                )
                for file in result.direct_template_usage:
                    print(f"    - {file}")

            if result.best_practices_violations:
                print(f"  [--] 违规项 ({len(result.best_practices_violations)} 个):")
                for violation in result.best_practices_violations:
                    print(f"    - {violation}")

            if result.score >= 90:
                print("  [++] 符合最佳实践!")
            elif result.score >= 70:
                print("  [~~] 基本符合，建议改进")
            elif result.score >= 50:
                print("  [--] 需要改进")
            else:
                print("  [XX] 严重不符合最佳实践")
            print()

        # 总结建议
        print("=" * 40)
        print("改进建议:")

        plugins_without_keys = [
            r for r in best_practices_results if not r.has_language_keys_file
        ]
        if plugins_without_keys:
            print(
                f"1. 以下 {len(plugins_without_keys)} 个插件缺少 LanguageKeys.kt 文件:"
            )
            for result in plugins_without_keys:
                print(f"   - {result.plugin_name}")

        plugins_with_direct_usage = [
            r for r in best_practices_results if r.direct_template_usage
        ]
        if plugins_with_direct_usage:
            print(
                f"2. 以下 {len(plugins_with_direct_usage)} 个插件直接使用 <%xxx%> 模板:"
            )
            for result in plugins_with_direct_usage:
                print(
                    f"   - {result.plugin_name} ({len(result.direct_template_usage)} 个文件)"
                )

        print("3. 参考 external-book 插件的 i18n 实现作为最佳实践模板")
        print("4. 使用 LanguageKeys 常量类统一管理所有语言键")
        print(
            "5. 在代码中使用 LanguageKeys.Core.Error.NO_PERMISSION 而不是 <%core.error.no_permission%>"
        )
        print("6. 确保 LanguageKeys.kt 文件包含五层架构分类说明")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description="语言模板分析器 - 分析和清理项目中的语言模板"
    )
    parser.add_argument(
        "--remove-redundant",
        action="store_true",
        help="删除冗余的语言键",
        default=False,
    )
    parser.add_argument("--no-backup", action="store_true", help="删除时不创建备份文件")
    parser.add_argument("--confirm", action="store_true", help="删除前不询问确认")
    parser.add_argument(
        "--check-best-practices", action="store_true", help="只检查i18n最佳实践合规性"
    )
    parser.add_argument(
        "--score-threshold",
        type=float,
        default=80.0,
        help="最佳实践合规性评分阈值(默认80.0)",
    )
    parser.add_argument(
        "--plugins", nargs="*", help="指定要分析的插件名称，如果不指定则分析所有插件"
    )

    args = parser.parse_args()

    print("语言模板分析器")
    print("分析项目中的语言模板使用情况...")

    # 获取当前脚本所在目录作为项目根目录
    project_root = Path.cwd()
    print(f"项目根目录: {project_root}")

    analyzer = LanguageAnalyzer()
    results, best_practices_results = analyzer.analyze_project(
        project_root, args.plugins
    )

    # 如果只检查最佳实践
    if args.check_best_practices:
        analyzer.generate_best_practices_report(best_practices_results)

        # 检查是否有不符合阈值的插件
        failing_plugins = [
            r for r in best_practices_results if r.score < args.score_threshold
        ]
        if failing_plugins:
            print(
                f"\n[XX] 有 {len(failing_plugins)} 个插件的合规性评分低于阈值 {args.score_threshold}:"
            )
            for result in failing_plugins:
                print(f"  - {result.plugin_name}: {result.score:.1f}/100")
            exit(1)
        else:
            print(
                f"\n[OK] 所有插件的合规性评分都达到或超过阈值 {args.score_threshold}!"
            )
            exit(0)

    # 生成完整报告
    analyzer.generate_report(results, best_practices_results)

    # 如果指定了删除冗余键
    if args.remove_redundant:
        # 统计要删除的键数量
        total_redundant = sum(len(result.redundant_keys) for result in results)

        if total_redundant == 0:
            print("\n[OK] 没有发现冗余键，无需删除！")
            return

        print(f"\n发现 {total_redundant} 个冗余键")

        # 如果没有指定确认标志，询问用户
        if not args.confirm:
            response = input("是否确认删除所有冗余键？(y/N): ").strip().lower()
            if response not in ["y", "yes", "是"]:
                print("操作已取消")
                return

        # 执行删除
        backup = not args.no_backup
        analyzer.remove_redundant_keys(results, backup=backup)

        print("\n[OK] 冗余键删除完成！")
        if backup:
            print("[++] 提示: 如需恢复，可以使用备份文件")
    else:
        if any(result.redundant_keys for result in results):
            print("\n[++] 提示: 使用 --remove-redundant 参数可以自动删除冗余键")
            print("   例如: python scripts/language-analyzer.py --remove-redundant")


if __name__ == "__main__":
    main()
