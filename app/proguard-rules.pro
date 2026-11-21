# ==============================================================================
# Rafiq Al-Muslim — Aggressive R8 / ProGuard Obfuscation & Protection Rules
# ==============================================================================

# 1. Aggressive class/method/field obfuscation & flattening
-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5

# 2. Strip all debug logging calls completely in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 3. Preserve Android entry points (Application, Activities, Services, Receivers, Providers)
-keep public class com.example.RafiqApplication
-keep public class com.example.MainActivity
-keep public class com.example.PermissionsOnboardingActivity
-keep public class com.example.service.** extends android.app.Service { *; }
-keep public class com.example.receiver.** extends android.content.BroadcastReceiver { *; }
-keep class androidx.core.content.FileProvider { *; }

# 4. Preserve Moshi JSON models & generated adapters so serialization never breaks
-keepattributes RuntimeVisibleAnnotations,Signature,InnerClasses,EnclosingMethod
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}
-keep class com.example.data.model.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}

# 5. Retrofit & OkHttp safe rules
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface com.example.data.remote.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**

# 6. Kotlin Coroutines & Compose runtime safety
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlin.reflect.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
