# Decompose
-keep class com.arkivanov.decompose.extensions.compose.mainthread.SwingMainThreadChecker { *; }
# Coroutines break if we don't keep this
-keep class * implements kotlinx.coroutines.internal.MainDispatcherFactory
# Needed by filekit
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-keep class * implements com.sun.jna.** { *; }
-dontnote com.sun.**
# SLF4J uses service providers
-keep class org.slf4j.** { *; }
# And so does dbus for that matter
-keep class org.freedesktop.dbus.** { *; }
-keep class * implements org.freedesktop.dbus.** { *; }

-keepclasseswithmembers public class com.hypergonial.chat.MainKt {
    public static void main(java.lang.String[]);
}
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

# Compose
-assumenosideeffects public class androidx.compose.runtime.ComposerKt {
    void sourceInformation(androidx.compose.runtime.Composer,java.lang.String);
    void sourceInformationMarkerStart(androidx.compose.runtime.Composer,int,java.lang.String);
    void sourceInformationMarkerEnd(androidx.compose.runtime.Composer);
}

# Fix for VerifyError in androidx.compose.runtime
-keep class androidx.compose.runtime.State { *; }
-keep class androidx.compose.runtime.DerivedSnapshotState { *; }
-keep class androidx.compose.runtime.SnapshotStateKt { *; }
-keep class androidx.compose.runtime.SnapshotStateKt__DerivedStateKt { *; }

# Keep all implementations of State
-keep class * implements androidx.compose.runtime.State { *; }

# Keep derivedStateOf function and related classes
-keepclassmembers class androidx.compose.runtime.** {
    ** derivedStateOf(...);
}

# Keep all implementations of TextRangeScopeMeasurePolicy (including lambdas)
-keep class * implements androidx.compose.foundation.text.TextRangeScopeMeasurePolicy { *; }

# Keep the synthetic lambda classes that implement the interface
-keep class androidx.compose.foundation.text.TextLinkScope$$Lambda$* { *; }
-keep interface androidx.compose.foundation.text.TextRangeScopeMeasurePolicy { *; }
-keep class androidx.compose.foundation.text.TextRangeScopeMeasurePolicy$DefaultImpls { *; }



# kotlinx.serialization rules, taken from https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences

# disable optimisation for descriptor field because in some versions of ProGuard, optimization generates incorrect bytecode that causes a verification error
# see https://github.com/Kotlin/kotlinx.serialization/issues/2719
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

-keep,includedescriptorclasses class com.hypergonial.chat.**$$serializer { *; }
-keepclassmembers class com.hypergonial.chat.** {
    *** Companion;
}
-keepclasseswithmembers class com.hypergonial.chat.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# End of kotlinx.serialization rules

# ktor rules, taken from https://github.com/ktorio/ktor/blob/main/ktor-utils/jvm/resources/META-INF/proguard/ktor.pro

# Most of volatile fields are updated with AtomicFU and should not be mangled/removed
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}

-keepclassmembernames class io.ktor.** {
    volatile <fields>;
}

# client engines are loaded using ServiceLoader so we need to keep them
-keep class io.ktor.client.engine.** implements io.ktor.client.HttpClientEngineContainer
# Keep the json serviceloader for the love of god
-keep class * implements io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider { *; }

# end of ktor rules

# Stop murdering coil
-keep class okio.** { *; }
-keep class coil3.** { *; }

# Markdown also causes issues when optimized (or not?)
#-keep class com.mikepenz.markdown.compose.** { *; }

# Keep dbus & JNA
-keep class org.freedesktop.dbus.** { *; }
-keep class com.sun.jna.** { *; }


# OkHttp likes to spam warns
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# This is fine
-ignorewarnings
