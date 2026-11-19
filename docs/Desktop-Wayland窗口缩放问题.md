# Desktop 在 Wayland 下的窗口缩放问题

结论（2026-11-05 实机复现，niri + xwayland-satellite + Compose Desktop 1.9.2 + JDK 21）：

1. **NEUPass Desktop 不支持原生 Wayland，只能经 XWayland 运行。** 实际链路是 `Compose Desktop → AWT/XToolkit → XWayland → niri`。当前 JDK 只有 `libawt_xawt.so`，没有原生 Wayland 所需的 `libawt_wlawt.so`；OpenJDK 的纯 Wayland toolkit 仍停留在 Wakefield 原型阶段。

2. **“窗口拉伸后出现黑边、Compose 感知不到尺寸”的根因是尺寸事件断在“xwayland-satellite 顶层 X11 窗口 → OpenJDK AWT 子窗口”之间。** 复现中 niri 已把顶层窗口从初始 800×600 配置为约 1272×1375 物理像素，但 AWT 的“Content window”仍停在 800×600、Skiko 画布仍是 790×570。黑边是未被重绘覆盖的窗口区域，不是 Compose 布局不响应，也不是业务 UI 绘制的黑色背景。

3. **最小修复：启动时设置 `_JAVA_AWT_WM_NONREPARENTING=1`。** 这是 Java/AWT 在非 reparenting 合成器（如 niri）下的标准兼容开关。验证中设置该变量后，AWT“Content window”和画布随顶层窗口同步变为 1272×1375。项目代码无需改动。

4. **这是 XWayland + AWT 的通用兼容问题，不是 NEUPass 业务代码缺陷。** niri 的 super+右键能拉小窗口但 Compose 感知不到，正是同一断点的表现。
