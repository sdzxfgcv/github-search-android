# GitHub 搜索（Android）

一个用 Kotlin + Jetpack Compose 开发的 GitHub 仓库搜索 App。

## 功能

- **关键词搜索**：仓库名 / 描述 / README 全文检索
- **分类搜索**（核心功能）
  - 快捷分类：Android·Kotlin、Android·Java、AI/LLM、高星精选、近期活跃
  - 编程语言：Kotlin / Java / Python / JavaScript / TypeScript / Go / Rust / C++ / Swift / Dart / PHP
  - Topic 主题：android、kotlin、ai、llm、flutter、jetpack-compose、mvvm、game、tool
  - Star 数：不限 / 100+ / 500+ / 1000+ / 5000+
  - 更新时间：不限 / 近一周 / 近一月 / 近一年
  - 排序：最佳匹配 / 最多 Star / 最近更新
- **一键复制仓库网址**：每个结果卡片都有「复制网址」按钮，复制到系统剪贴板
- **打开 GitHub**：调用系统浏览器打开仓库页面
- **可选 Token**：在设置里填写 GitHub Personal Access Token，提高 API 频率限制（匿名 10 次/小时，认证后 30 次/分钟）。Token 只存在本机。
- 跟随系统的深色 / 浅色主题

## 构建

项目自带 Gradle Wrapper 与 GitHub Actions 工作流。推送到 `main` 分支会自动在云端构建 APK 并发布到 Releases 页面。

本地构建（需 JDK 17 + Android SDK）：

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 技术栈

Kotlin 2.0 · Jetpack Compose (Material 3) · OkHttp · GitHub Search REST API · AGP 8.6 · Gradle 8.7
