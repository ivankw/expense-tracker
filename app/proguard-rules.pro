# ==========================================
# Room Database Rules
# ==========================================
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ==========================================
# Model Data & Utilitas Proyek
# ==========================================
-keep class com.example.pengeluaran.data.** { *; }
-keep class com.example.pengeluaran.util.** { *; }
-keep class com.example.pengeluaran.viewmodel.** { *; }

# ==========================================
# Kotlin Coroutines & DataStore
# ==========================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class * extends kotlinx.coroutines.CoroutineScope {
    public <methods>;
}
-dontwarn sun.misc.Unsafe

# ==========================================
# Jetpack Compose
# ==========================================
-keep class androidx.compose.material3.** { *; }
