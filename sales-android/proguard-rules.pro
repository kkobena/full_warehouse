# ===============================
# Keep generic info (fix ParameterizedType crash)
# ===============================
-keepattributes Signature, InnerClasses, EnclosingMethod

# Keep ALL app classes (prevents broken generics / reflection issues)
-keep class com.kobe.warehouse.sales.** { *; }

# Keep all inner and anonymous classes
-keep class **$* { *; }

# Keep all class members for reflection
-keepclassmembers class * {
    *;
}

# ===============================
# Retrofit
# ===============================
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.**
-keep class retrofit2.** { *; }

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepclasseswithmembers interface * {
    @retrofit2.* <methods>;
}

# Keep the AuthApiService and its methods
-keep,allowobfuscation,allowshrinking class com.kobe.warehouse.sales.data.api.AuthApiService {
    <methods>;
}

# ===============================
# OkHttp
# ===============================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ===============================
# Gson
# ===============================
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepattributes Signature, *Annotation*
-dontwarn sun.misc.**

# ===============================
# Project Models / DTOs
# ===============================
-keep class com.kobe.warehouse.sales.data.model.** { *; }
-keep class com.kobe.warehouse.sales.service.dto.** { *; }
-keep class com.kobe.warehouse.sales.data.model.auth.** { *; }
-keep class com.kobe.warehouse.sales.data.model.sales.** { *; }

-keepclassmembers class com.kobe.warehouse.sales.data.model.** {
    <init>(...);
    *;
}
-keepclassmembers class com.kobe.warehouse.sales.service.dto.** {
    <init>(...);
    *;
}
-keepclassmembers class com.kobe.warehouse.sales.data.model.auth.** {
    <init>(...);
    *;
}
-keepclassmembers class com.kobe.warehouse.sales.data.model.sales.** {
    <init>(...);
    *;
}

# ===============================
# Coroutines
# ===============================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ===============================
# Lifecycle / LiveData
# ===============================
-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keep class * implements androidx.lifecycle.GeneratedAdapter {
    <init>(...);
}
-keepclassmembers class ** {
    @androidx.lifecycle.OnLifecycleEvent *;
}

# ===============================
# ViewModels
# ===============================
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ===============================
# EncryptedSharedPreferences
# ===============================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ===============================
# Remove logging in release builds
# ===============================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===============================
# Google HTTP client / Joda
# ===============================
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant
