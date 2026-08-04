# Papirus Office ProGuard / R8 Rules

# Preserve stack trace line numbers for crash logging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve JNI Native Method Names and Classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Papirus Office Engine & Domain Models
-keep class com.makerandreas.papirusoffice.** { *; }
-keep class com.example.** { *; }

# Keep LibreOffice UNO / Java SDK Stubs
-keep class com.sun.star.** { *; }

# Moshi / Retrofit / JSON Serialization rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retain Compose runtime annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

