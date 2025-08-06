# 租房管家 (Rental Manager)

一个现代化的Android租房管理应用程序，帮助房东轻松管理租户、账单和收据。

## 项目概述

租房管家是专为个人房东设计的移动应用，用于管理出租房屋的租户信息、账单生成和收据打印。应用采用了现代化的Material 3设计语言，支持深色模式，拥有流畅的动画效果和高性能的UI组件。

## 核心功能

- **租户管理**：添加、编辑和查看租户信息
- **账单生成**：为每个租户按月创建账单，支持水电费等费用明细
- **收据打印**：生成专业的收据图片，支持分享和保存
- **数据导入导出**：备份和恢复应用数据
- **自定义计量表名**：支持自定义水表、电表等计量表的显示名称
- **隐私保护**：支持在收据上屏蔽敏感信息

## 技术栈

- **开发语言**：Kotlin
- **UI框架**：Jetpack Compose
- **架构模式**：MVVM (Model-View-ViewModel)
- **数据存储**：Room Database
- **异步处理**：Kotlin Coroutines & Flow
- **依赖注入**：手动依赖注入
- **导航**：Compose Navigation
- **图片处理**：Coil
- **JSON处理**：Gson

## 系统要求

- Android 9.0 (API 28) 或更高版本
- 建议：4GB+ RAM，适中存储空间

## 安装指南

### 从APK安装

1. 下载最新版本的APK文件 (`app-release.apk`)
2. 在Android设备上点击APK文件
3. 按照屏幕提示完成安装
4. 首次运行时授予应用所需权限

### 从源代码构建

1. 克隆仓库：
   ```bash
   git clone https://github.com/your-username/rentalmanager.git
   ```

2. 使用Android Studio打开项目

3. 执行Gradle构建：
   ```bash
   ./gradlew assembleDebug
   ```

4. 生成的APK文件位于 `app/build/outputs/apk/debug/` 目录

## 使用说明

### 首次使用

1. 启动应用
2. 点击左下角"+"按钮，选择"添加租户"
3. 输入租户信息（房间号、姓名、租金等）
4. 保存租户信息

### 创建账单

1. 从主页或账单页面选择"添加账单"
2. 选择租户和账单月份
3. 输入水电表读数和其他费用
4. 保存账单

### 生成收据

1. 在主页租户卡片上点击"收据"按钮
2. 系统会自动生成当月收据
3. 点击分享按钮可以将收据分享给租户

### 数据备份和恢复

1. 从菜单中选择"备份与恢复"
2. 选择"导出数据"将数据保存到本地文件
3. 需要恢复时，选择"导入数据"并选择之前导出的文件

## 性能优化

应用采用了多项性能优化措施：

- 懒加载和预加载机制
- 组件级别的优化
- 智能UI渲染
- 数据库操作异步处理
- 资源占用监控

## 项目结构

```
app/
├── src/main/
│   ├── java/com/morgen/rentalmanager/
│   │   ├── myapplication/         # 数据模型和数据库
│   │   ├── ui/                    # UI组件和界面
│   │   │   ├── about/             # 关于页面
│   │   │   ├── addbillall/        # 添加账单页面
│   │   │   ├── billdetail/        # 账单详情页面
│   │   │   ├── billlist/          # 账单列表页面
│   │   │   ├── components/        # 可复用UI组件
│   │   │   ├── theme/             # 主题和样式
│   │   ├── utils/                 # 工具类
│   ├── res/                       # 资源文件
```

## 贡献

欢迎贡献代码、报告问题或提出功能建议。请通过GitHub Issues或Pull Requests参与项目开发。

## 许可证

[MIT License](LICENSE)

## 联系方式

如有问题或建议，请联系项目维护者：example@email.com 