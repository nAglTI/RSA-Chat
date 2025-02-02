-keep class androidx.startup.AppInitializer
-keep class * extends androidx.startup.Initializer
# Needed by filekit
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
