-keep class com.arkivanov.decompose.extensions.compose.jetbrains.mainthread.SwingMainThreadChecker
# Needed by filekit
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# This is fine
-ignorewarnings
