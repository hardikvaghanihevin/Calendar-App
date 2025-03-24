# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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

#-keep class com.squareup.** { *; }
#-keep interface com.squareup.** { *; }
#-keep class retrofit2.** { *; }
#-keep interface retrofit2.** { *;}
#-keep interface com.squareup.** { *; }
#
#
#-keepclasseswithmembers class * {
#    @retrofit2.http.* <methods>;
#}
#
#
#-dontwarn rx.**
#-dontwarn retrofit2.**
#-dontwarn okhttp3.**
#-dontwarn okio.**
#
#-keep class calendar.schedule.task.todo.event.reminder.data.database.entity.Event.* { *; }
#-keep class calendar.schedule.task.todo.event.reminder.domain.model.* { *; }

# General ProGuard settings
-keepattributes Signature, Exceptions, LineNumberTable, SourceFile

# Retrofit
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class com.squareup.okhttp3.** { *; }

# Gson (if using Gson for serialization/deserialization)
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# Application-specific classes
-keep class calendar.schedule.task.todo.event.reminder.data.remote.dto.HolidayApiDto { *; }
-keep class calendar.schedule.task.todo.event.reminder.data.database.entity.Event { *; }
-keep class calendar.schedule.task.todo.event.reminder.domain.model.** { *; }

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembers class kotlinx.coroutines.CoroutineScope { *; }

# Prevent coroutine obfuscation in stack traces
-keepclassmembers class * implements kotlin.coroutines.Continuation {
    public <init>(...);
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.jvm.internal.** { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.** { *; }

# Prevent warnings
-dontwarn rx.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Disable obfuscation (if needed for debugging)
-dontobfuscate
