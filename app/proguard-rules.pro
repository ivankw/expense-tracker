# Room Database Rules
-keepclassmembers class * extends androidx.room.RoomDatabase { public <methods>; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Model Data & Utilitas Proyek
-keep class com.example.pengeluaran.data.** { *; }
-keep class com.example.pengeluaran.util.** { *; }
-keep class com.example.pengeluaran.viewmodel.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# OkHttp & Okio
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines & Compose
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends kotlinx.coroutines.CoroutineScope { public <methods>; }
-keep class androidx.compose.material3.** { *; }
-dontwarn sun.misc.Unsafe
