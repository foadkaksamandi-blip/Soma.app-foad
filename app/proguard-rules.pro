# ProGuard rules for soma.app.foad

# Keep models for Room (SQLite)
-keep class androidx.room.** { *; }
-keep class ir.soma.app.foad.data.** { *; }

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }

# Keep QR code generator
-keep class com.journeyapps.** { *; }

# General
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
