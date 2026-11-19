package ink.chyk.pass.ui

import androidx.window.core.layout.WindowSizeClass

/** 当前窗口使用手机宽度布局，而非判断设备类型。 */
val WindowSizeClass.isMobileWidth: Boolean
  get() = !isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

/** 当前窗口使用宽屏布局，而非判断设备类型或运行平台。 */
val WindowSizeClass.isWideWidth: Boolean
  get() = isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
