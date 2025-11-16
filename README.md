# MCPanel-Server - 构建与启动指南

## 环境要求

- **Java 21**：确保项目的 Java 版本设置为 **Java 21**。

## 构建 MOD 文件

1. 打开 IntelliJ IDEA，并确保已正确导入 **MCPanel-Server** 项目。
2. 确保项目的 **Java 版本** 设置为 **Java 21**，并且 Gradle 已成功加载项目。
3. 在 **Gradle** 工具窗口中，展开 **Tasks**：
    - `build`
4. 运行 **build** 任务以构建项目，最终的 **MOD 文件** 会生成在 `build/libs/` 目录下。

## 启动 MCPanel-Server

1. 在 IntelliJ IDEA 中，选择 **MCPanel-Server** 项目。
2. 点击 **Tasks-mod development**，然后选择 **runServer** 或 **runClient** 启动。

### 启动说明：
- **runServer**：启动服务器端，自动下载服务端文件并运行。
- **runClient**：启动客户端，自动下载客户端文件并运行。