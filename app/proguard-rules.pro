# Strip verbose and debug logcat logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Preserve Room database entities and DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}

# Preserve Retrofit interface methods and Enums
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers enum * { *; }
