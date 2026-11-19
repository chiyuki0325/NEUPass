# Navigation3迁移与版本升级

记录日期：2026-11-10。

## 本次变更

主窗口的四个Tab及页面内多级导航由手写导航迁移到Navigation3，设置页归入“我的”子栈。点击任意Tab仍返回目标Tab的首页，重复点击当前Tab也遵循此行为。页面层级见[页面导航](页面导航.md)。

迁移同时调整了以下依赖，版本配置位于`gradle/libs.versions.toml`：

| 依赖 | 迁移前 | 迁移后 |
| --- | --- | --- |
| Compose Multiplatform | `1.9.2` | `1.10.0` |
| JetBrains Lifecycle | `2.9.0` | `2.10.0` |
| 导航 | Android侧的Navigation Compose `2.8.8`，主窗口使用手写导航 | common侧使用Navigation3 runtime及多平台UI `1.1.0` |
| Navigation3的ViewModel集成 | 未引入 | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0` |

Kotlin仍为`2.2.21`，Material3仍为单独配置的`1.9.0-beta03`，本次没有随Compose一起升级。

## 升级原因与依赖约束

本次采用的`org.jetbrains.androidx.navigation3:navigation3-ui:1.1.0`发布元数据声明依赖Compose `1.10.0`系列组件，其Desktop产物声明的Compose runtime版本为`1.10.1`。这里的Compose `1.10.0`是项目直接配置的版本，不代表所有传递依赖最终都解析为`1.10.0`。

因此，仅把版本目录中的Compose改回`1.9.2`，仍会受到Navigation3传递依赖的影响，不能视为恢复了Compose `1.9.2`技术栈。强制压低依赖版本也未经兼容性验证。

JetBrains将跨平台Navigation3支持列为Compose Multiplatform 1.10开始提供的能力，见[官方说明](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)。这说明本次选用的依赖组合有升级要求，不代表已验证所有早期Navigation3版本都无法搭配Compose `1.9.2`。

本次迁移在引入Navigation3的同时直接升级了Compose和Lifecycle，没有事先单独确认鸿蒙适配约束；这项平台兼容性影响需要与导航迁移一并记录。

## 对鸿蒙适配的影响

本次评估按项目维护者提供的信息，以[CPF-KMP-CMP](https://atomgit.com/CPF-KMP-CMP)目前仍停留在Compose `1.9.2`为鸿蒙适配基线。该版本状态是本次记录的前提，后续应随其上游进展重新核对。

**本次升级打破了沿用CPF-KMP-CMP现有Compose `1.9.2`基线适配鸿蒙的可行性前提。**目前不能再把鸿蒙适配看作仅补齐平台入口和已有平台能力即可完成的工作，新增了两类阻塞：

- **Compose版本不一致。**NEUPass的共享UI已升级到`1.10.0`系列，而所依据的鸿蒙Compose基线仍为`1.9.2`。
- **Navigation3需要平台移植。**Navigation3 UI并非全部都是平台无关的common代码。已核对的`1.1.0`源码包含默认转场的`expect/actual`声明及iOS、Web平台实现，并依赖Navigation Event处理返回事件。业务代码写在common中，不意味着这些底层库自动支持鸿蒙；当前接入没有提供或验证鸿蒙目标产物及相应平台实现。

鸿蒙开工时，需要重新评估依赖移植成本。

## 后续恢复适配的条件

后续若继续推进鸿蒙适配，需要确认以下路线之一：

- CPF-KMP-CMP跟进所需Compose版本，并补齐Navigation3及相关依赖的鸿蒙支持。
- 在相应基础设施仓库完成版本跟进与平台实现，发布可用依赖后再接入NEUPass。
- 重新选择与鸿蒙基线兼容的导航及UI依赖组合；退回版本或更换导航方案都需要另行决策与验证。

仅回退Compose版本号，或仅为应用添加鸿蒙入口，都不足以消除上述阻塞。
