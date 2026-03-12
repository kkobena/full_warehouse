# ============================================================================
# ProGuard rules for Pharma Smart Sales Android App
# ============================================================================

# ----------------------------------------------------------------------------
# Kotlin
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ----------------------------------------------------------------------------
# Gson / JSON Serialization
# Keep all data classes used for API requests/responses
# ----------------------------------------------------------------------------

# Keep all data classes in the model package
-keep class com.kobe.warehouse.sales.data.model.** { *; }
-keepclassmembers class com.kobe.warehouse.sales.data.model.** { *; }

# Keep all data classes in the api package
-keep class com.kobe.warehouse.sales.data.api.** { *; }
-keepclassmembers class com.kobe.warehouse.sales.data.api.** { *; }

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances, and classes with @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ----------------------------------------------------------------------------
# Retrofit
# ----------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep Retrofit service interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ----------------------------------------------------------------------------
# OkHttp
# ----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ----------------------------------------------------------------------------
# Room Database
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# AndroidX / Jetpack
# ----------------------------------------------------------------------------
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ----------------------------------------------------------------------------
# Security Crypto (EncryptedSharedPreferences)
# ----------------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ----------------------------------------------------------------------------
# Sunmi Printer
# ----------------------------------------------------------------------------
-keep class com.sunmi.** { *; }
-keep class woyou.aidlservice.** { *; }
-dontwarn com.sunmi.**
-dontwarn woyou.aidlservice.**

# ----------------------------------------------------------------------------
# ZXing (QR Code / Barcode scanning)
# ----------------------------------------------------------------------------
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**

# ----------------------------------------------------------------------------
# Coil (Image Loading)
# ----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ----------------------------------------------------------------------------
# Navigation Component
# ----------------------------------------------------------------------------
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ----------------------------------------------------------------------------
# Paging 3
# ----------------------------------------------------------------------------
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

# ----------------------------------------------------------------------------
# DataBinding / ViewBinding
# ----------------------------------------------------------------------------
-keep class androidx.databinding.** { *; }
-dontwarn androidx.databinding.**
-keep class **databinding** { *; }
-keep class **Binding { *; }
-keep class **BR { *; }

# ----------------------------------------------------------------------------
# General Android
# ----------------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ----------------------------------------------------------------------------
# Debugging - Keep line numbers for crash reports
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
