# Billforce Owner Android App — Proguard Rules

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Gson model classes
-keepclassmembers class com.billforce.owner.data.model.** { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**
