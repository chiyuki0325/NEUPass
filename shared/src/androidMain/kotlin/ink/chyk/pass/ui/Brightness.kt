package ink.chyk.pass.ui

import android.view.Window

fun setScreenBrightness(window: Window, brightness: Float) {
    val layoutParams = window.attributes
    layoutParams.screenBrightness = brightness
    window.attributes = layoutParams
}