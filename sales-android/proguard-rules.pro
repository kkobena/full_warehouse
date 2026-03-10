# ===============================
# Attributes (merged)
# ===============================
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# ===============================
# Keep Parcelable implementations
# ===============================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
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
-dontwarn sun.misc.**

# ===============================
# Project Models / DTOs
# ===============================
-keep class com.kobe.warehouse.sales.data.model.** { *; }
-keep class com.kobe.warehouse.sales.service.dto.** { *; }

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

# ===============================
# ZXing (QR Code)
# ===============================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

# ===============================
# Sunmi Printer
# ===============================
-keep class com.sunmi.** { *; }
-keep class woyou.aidlservice.** { *; }
-dontwarn com.sunmi.**
-dontwarn woyou.aidlservice.**

# ===============================
# Coil (Image Loading)
# ===============================
-dontwarn coil.**
-keep class coil.** { *; }

# ===============================
# Room Database
# ===============================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ===============================
# Navigation Component
# ===============================
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ===============================
# Paging 3
# ===============================
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

# ===============================
# DataBinding / ViewBinding
# ===============================
-keep class androidx.databinding.** { *; }
-dontwarn androidx.databinding.**
-keep class **databinding** { *; }
-keep class **Binding { *; }
-keep class **BR { *; }

# ===============================
# Kotlin (Reflection / Metadata)
# ===============================
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

