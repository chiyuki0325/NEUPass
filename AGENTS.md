# NEUPass KMP 开发规则

## 项目概况

NEUPass 是支持 Android、JVM Desktop 和 iOS 的 Kotlin Multiplatform 应用。应用根节点、导航、UI、ViewModel 和业务逻辑均以 common 代码为主体，三端共享同一套产品实现。

## 构建与入口

- Android：`./gradlew :androidApp:assembleDebug` / `:androidApp:assembleRelease`。
- Desktop：`./gradlew :desktopApp:run`。
- iOS：`iosApp/iosApp.xcodeproj`，最终构建需要 macOS/Xcode；原生侧仅保留 UIKit 启动壳。
- Android、Desktop 和 iOS 入口必须启动同一个 common `App()`，平台入口不携带业务逻辑。

## 共享应用架构

- NEUPass 的主要部分是跨平台代码。跨平台根节点、导航、页面、对话框、组件、状态模型和 ViewModel 位于 `shared/src/commonMain`。
- 平台源码集不携带业务逻辑，只填补平台特定缺口。
- common 中的 `App()` 持有根导航，并直接渲染原有四个主区域：课表、一码通、应用、我的。
- AndroidX Lifecycle ViewModel、StateFlow、协程等已有 KMP 支持的 API 必须直接在 common 使用。不得为把 ViewModel 留在 Android 而增加 Presenter 等过渡层。
- 仓库、协议解析和业务规则位于 common。平台服务只提供无法消除的原生操作、引擎或存储初始化。
- 新增业务代码放在 common 对应业务包（`features/*`、`repositories`、`viewmodels` 等），feature 专属代码与其业务归属放在一起，共享 UI 基础件放 `ui` 包。

## 平台边界

只有确实依赖平台的能力可以使用 `expect/actual` 或平台服务，包括：

- Android 腾讯 X5 WebView；
- 屏幕亮度、防截屏和其他原生窗口控制；
- 文件/图片选择器、外部应用启动；
- 认证所需的平台设备描述；
- 平台入口以及网络、资源底层引擎。

平台能力对业务 UI 必须透明。例如 common 业务只调用 `CustomWebView`，不得判断当前是 Desktop、Android 还是 iOS，也不得在业务页面中渲染平台专属提示。

Desktop 和 iOS 的 WebView 可以是空实现或统一的不支持实现。除非用户明确要求，不得为 Desktop 引入 JCEF，也不得设计另一套 Desktop WebView 体验。

## UI 设计

- 采用 Material 3 Compose 组件和主题
- 采用 Fluent system icons

## 基础设施优先

- 当 KmmResource、mmkv-kotlin 或本仓库维护的其他跨平台基建缺少所需能力时，必须直接修改对应基建仓库，补齐所有目标平台实现，发布新 tag，再让 NEUPass 升级依赖。
- 禁止为了绕过基建缺陷，在 NEUPass 业务代码中编写基建临时实现。

## 网络

- NEUPass 网络层统一使用 Ktor Client。业务 API 位于 `ink.chyk.pass.api`，在 common 中通过 `createApiClient` 创建客户端。
- 协议级的横切逻辑需要写成 Ktor 插件或客户端配置，不得在每个接口方法里手写。这类逻辑包括加解密、信封解析、签名请求头、Cookie 持久化、异常归类和固定请求头。
- Json 实例由各协议插件的配置持有。

## API 文档

- 接口文档放在 `docs/api/` 下按业务分目录
- 文档描述实际验证过的协议行为；与旧代码不一致时以实际验证结论为准，并注明差异

## 验证

- 每个有意义的变更后必须至少确保一个平台能编译通过。
- 重要提交前根据变更范围运行 Android debug/release、Desktop 测试 / Android 单元测试和 R8 release 验证。
- Desktop 视觉验证只启动一个 Gradle session。运行中的 Desktop 应用会让 `run` task 持续存活，不能等待它自然结束。
- 截图完成后立即结束同一个 Desktop session。另一个构建尚未结束时禁止重复启动 Gradle。
- iOS 最终构建需要 macOS/Xcode；Linux 上至少验证源码集、Gradle 模型和 framework 配置。

## 关于测试编写

维护大量测试文件是非常昂贵的工作，测试文件的维护成本可能远高于它们带来的收益。
你可以编写测试，但我们通常建议你跑一次，确保你本次新增的代码（或牵扯到的旧有代码）行为正确后，删掉大部分/所有测试。

真正能留着的测试是那些能在未来帮助我们发现问题的测试，如证明登录/会话恢复正确、课表解析正确等的测试。
测试是用来约束业务行为的，而不是固定代码行为的。

跑完测试，准备交付需求时，重新在内部思考中解释本次试图新增的所有测试。
通常对于一条测试，如果你解释不出「它在什么情况下会失败/它为什么可能会失败」，通常不建议保留。

## Git 与维护

见 docs/agents/Git提交规范.md

## 注释编写风格

见 docs/agents/注释编写风格.md
