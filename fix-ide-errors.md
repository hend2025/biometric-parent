# 修复 IntelliJ IDEA 2025 编译错误

## 问题现象
- 命令行 `mvn install` 成功
- IDE 中显示 30 个编译错误
- 标记为"缺少符号"或"找不到类"

## 解决方案（按优先级）

### 方案1：刷新 Maven 项目 ⭐推荐
1. 打开 Maven 面板（右侧边栏）
2. 点击 🔄 按钮（Reload All Maven Projects）
3. 或者右键项目 → Maven → Reload Project
4. 快捷键：`Ctrl + Shift + O`

### 方案2：使缓存失效并重启
1. 菜单：File → Invalidate Caches...
2. 勾选以下选项：
   - ✅ Clear file system cache and Local History
   - ✅ Clear downloaded shared indexes
   - ✅ Clear VCS Log caches and indexes
3. 点击 "Invalidate and Restart"
4. 等待 IDE 重启并重新索引

### 方案3：重新导入 Maven 项目
1. 关闭项目：File → Close Project
2. 在欢迎页面，点击 "Open"
3. 选择 `D:\biometric-parent\pom.xml`
4. 选择 "Open as Project"
5. 等待 Maven 依赖下载和索引完成

### 方案4：手动删除 IDEA 缓存
1. 关闭 IntelliJ IDEA
2. 删除以下目录：
   - `D:\biometric-parent\.idea`
   - `D:\biometric-parent\biometric-algo\target`
   - `D:\biometric-parent\biometric-serv\target`
3. 重新用 IDEA 打开项目

### 方案5：检查 Maven 配置
1. 打开设置：File → Settings (Ctrl + Alt + S)
2. 导航到：Build, Execution, Deployment → Build Tools → Maven
3. 检查配置：
   - Maven home path: 指向你的 Maven 安装目录
   - User settings file: 使用默认或指定 settings.xml
   - Local repository: 检查是否正确
4. 点击 Apply → OK

### 方案6：使用 Maven 命令编译
在 IDE 底部的 Terminal 中执行：
```bash
mvn clean compile
```

然后点击 Maven 刷新按钮。

## 验证步骤

1. 检查右下角是否有 "Indexing..." 或 "Building..."
2. 等待所有后台任务完成
3. 查看 Problems 面板（Alt + 6）是否还有错误
4. 尝试运行主类 `BiometricAlgoApplication`

## 常见问题

### Q: 为什么命令行成功，IDE 失败？
A: IDE 有自己的编译器和缓存系统，可能与 Maven 不同步。

### Q: 刷新后仍然有错误怎么办？
A: 尝试方案2（使缓存失效）或方案4（手动删除缓存）。

### Q: 依赖下载很慢怎么办？
A: 配置国内 Maven 镜像（阿里云）。

## 预防措施

1. 每次修改 pom.xml 后，记得刷新 Maven 项目
2. 定期清理 IDE 缓存：File → Invalidate Caches...
3. 使用 IDE 自带的 Maven 功能，避免混用命令行

