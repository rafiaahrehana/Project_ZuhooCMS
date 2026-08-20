# R8 rules for the release build.
#
# Every DTO field carries @SerializedName, so R8 renaming fields does NOT break JSON mapping —
# that is the main reason this file can stay small. What remains below is about generic type
# information and reflection, which annotations don't solve.

# ---------------------------------------------------------------------------
# Gson
# ---------------------------------------------------------------------------
# Signatures must survive: PageResponse<T> and Map<String, String> are resolved from the generic
# signature at runtime. Strip it and every paged response deserialises to the wrong type.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod

# TypeToken relies on reflecting over its own generic superclass.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Gson instantiates model classes without calling a constructor. Keep the no-arg constructors it
# looks for, and the fields it writes into.
-keepclassmembers class com.raf.zuhoo.data.model.** {
    <init>();
    <fields>;
}

# Belt and braces: keep any field explicitly annotated for serialisation, wherever it lives.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---------------------------------------------------------------------------
# Retrofit / OkHttp
# ---------------------------------------------------------------------------
# Retrofit builds its calls by reflecting over the interface's annotations and generic return
# types, so the service interface itself must keep both.
-keep,allowobfuscation interface com.raf.zuhoo.data.api.ApiService
-keepclassmembers,allowobfuscation interface com.raf.zuhoo.data.api.ApiService { *; }

# Retrofit's own reflection needs; these mirror the rules Retrofit ships.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# OkHttp references these optional platform pieces that aren't on Android.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
# The generated implementation is found by name at runtime.
-keep class com.raf.zuhoo.data.local.db.** { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Firebase Cloud Messaging
# ---------------------------------------------------------------------------
# The service is instantiated by the framework from the manifest entry.
-keep class com.raf.zuhoo.push.ZuhooMessagingService { *; }
-dontwarn com.google.firebase.**

# ---------------------------------------------------------------------------
# App
# ---------------------------------------------------------------------------
# Named in AndroidManifest, so it must keep its name.
-keep class com.raf.zuhoo.ZuhooApplication { *; }

# ViewModels are constructed reflectively by ViewModelProvider via their (Application) constructor.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(android.app.Application);
}

# Keep source file and line numbers in stack traces, but rename the file so the original names
# aren't handed out. Without this a crash report is unreadable.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
