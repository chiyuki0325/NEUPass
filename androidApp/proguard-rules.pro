# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Ktor only probes for the optional SLF4J 1.x binding on Android.
-dontwarn org.slf4j.impl.StaticLoggerBinder
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/pwroguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-obfuscationdictionary dingzhen.txt
-classobfuscationdictionary dingzhen.txt
-packageobfuscationdictionary dingzhen.txt

-dontwarn dalvik.**
-dontwarn com.tencent.smtt.**

-keep class com.tencent.smtt.** {
    *;
}

-keep class com.tencent.tbs.** {
    *;
}
