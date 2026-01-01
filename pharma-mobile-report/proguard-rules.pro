# ============================================================================
# ProGuard rules for Pharma Report Mobile App
# ============================================================================

# ----------------------------------------------------------------------------
# Kotlin
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ----------------------------------------------------------------------------
# Gson / JSON Serialization
# Keep all data classes used for API requests/responses
# ----------------------------------------------------------------------------

# Keep all data classes in the model package
-keep class com.kobe.warehouse.reports.data.model.** { *; }
-keepclassmembers class com.kobe.warehouse.reports.data.model.** { *; }

# Keep all data classes in the api package (DTOs like LoginRequest, JwtTokenResponse, etc.)
-keep class com.kobe.warehouse.reports.data.api.** { *; }
-keepclassmembers class com.kobe.warehouse.reports.data.api.** { *; }

# Keep all data classes in offline package
-keep class com.kobe.warehouse.reports.data.offline.** { *; }
-keepclassmembers class com.kobe.warehouse.reports.data.offline.** { *; }

# Keep all Room entities
-keep class com.kobe.warehouse.reports.data.local.entity.** { *; }
-keepclassmembers class com.kobe.warehouse.reports.data.local.entity.** { *; }

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from stripping interface information
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ----------------------------------------------------------------------------
# Retrofit
# ----------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

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

# ----------------------------------------------------------------------------
# Coroutines
# ----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# Firebase
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ----------------------------------------------------------------------------
# Room Database
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
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

# Keep MasterKey and related classes
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# ----------------------------------------------------------------------------
# MPAndroidChart
# ----------------------------------------------------------------------------
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ----------------------------------------------------------------------------
# ZXing (Barcode scanning)
# ----------------------------------------------------------------------------
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keep class com.journeyapps.barcodescanner.** { *; }

# ----------------------------------------------------------------------------
# Shimmer
# ----------------------------------------------------------------------------
-keep class com.facebook.shimmer.** { *; }

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

# ----------------------------------------------------------------------------
# Debugging - Keep line numbers for crash reports
# ----------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
