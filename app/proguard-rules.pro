# Retrofit 3.x (bundles own consumer rules, keep annotations and service interfaces)
-keepattributes Signature
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp 5.x (bundles own consumer rules via okhttp3.internal.publicsuffix)
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.choo.moviefinder.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.choo.moviefinder.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.choo.moviefinder.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Widget serialization classes
-keep,includedescriptorclasses class com.choo.moviefinder.presentation.widget.**$$serializer { *; }
-keepclassmembers class com.choo.moviefinder.presentation.widget.** {
    *** Companion;
}
-keepclasseswithmembers class com.choo.moviefinder.presentation.widget.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coil 3.x
-dontwarn coil3.**

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}

# Paging
-keep class androidx.paging.** { *; }

# WorkManager (notification worker)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
