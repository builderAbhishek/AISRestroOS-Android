# AISRestroOS ProGuard / R8 Rules

# Keep Kotlin Metadata & Coroutines
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Gson & Retrofit DTOs / Domain Models so JSON serialization & deserialization never breaks
-keep class com.ais.restroos.data.remote.dtos.** { *; }
-keep class com.ais.restroos.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit2 & OkHttp3
-keepattributes Exceptions,Signature
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Strip debug logs in Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
