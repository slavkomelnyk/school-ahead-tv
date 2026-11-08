# Keep JS interface methods
-keepclassmembers class com.ahead.school.bridge.** {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.ahead.school.bridge.** { *; }

# Keep WebView JS integration
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Standard AndroidX (safe defaults)
-keep class androidx.core.app.CoreComponentFactory { *; }