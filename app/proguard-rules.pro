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

# DataStore UserSettings serialization
-keep,includedescriptorclasses class com.choo.moviefinder.data.local.UserSettings$$serializer { *; }
-keepclassmembers class com.choo.moviefinder.data.local.UserSettings {
    *** Companion;
}
-keepclasseswithmembers class com.choo.moviefinder.data.local.UserSettings {
    kotlinx.serialization.KSerializer serializer(...);
}

# Domain backup model serialization (export/import)
-keep,includedescriptorclasses class com.choo.moviefinder.domain.model.UserDataBackup$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.domain.model.BackupMovie$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.domain.model.BackupRating$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.domain.model.BackupMemo$$serializer { *; }
-keepclassmembers class com.choo.moviefinder.domain.model.UserDataBackup {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.domain.model.BackupMovie {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.domain.model.BackupRating {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.domain.model.BackupMemo {
    *** Companion;
}
-keepclasseswithmembers class com.choo.moviefinder.domain.model.UserDataBackup {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.domain.model.BackupMovie {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.domain.model.BackupRating {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.domain.model.BackupMemo {
    kotlinx.serialization.KSerializer serializer(...);
}

# Person detail DTO serialization
-keep,includedescriptorclasses class com.choo.moviefinder.data.remote.dto.PersonDetailDto$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.data.remote.dto.PersonCreditsResponse$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.data.remote.dto.PersonSearchResponse$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.data.remote.dto.PersonSearchResult$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.data.remote.dto.KnownForMovie$$serializer { *; }
-keepclassmembers class com.choo.moviefinder.data.remote.dto.PersonDetailDto {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.data.remote.dto.PersonCreditsResponse {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.data.remote.dto.PersonSearchResponse {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.data.remote.dto.PersonSearchResult {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.data.remote.dto.KnownForMovie {
    *** Companion;
}
-keepclasseswithmembers class com.choo.moviefinder.data.remote.dto.PersonDetailDto {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.data.remote.dto.PersonCreditsResponse {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.data.remote.dto.PersonSearchResponse {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.data.remote.dto.PersonSearchResult {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.data.remote.dto.KnownForMovie {
    kotlinx.serialization.KSerializer serializer(...);
}

# Widget serialization classes (WidgetMovieListResponse, WidgetMovie only)
-keep,includedescriptorclasses class com.choo.moviefinder.presentation.widget.WidgetMovieListResponse$$serializer { *; }
-keep,includedescriptorclasses class com.choo.moviefinder.presentation.widget.WidgetMovie$$serializer { *; }
-keepclassmembers class com.choo.moviefinder.presentation.widget.WidgetMovieListResponse {
    *** Companion;
}
-keepclassmembers class com.choo.moviefinder.presentation.widget.WidgetMovie {
    *** Companion;
}
-keepclasseswithmembers class com.choo.moviefinder.presentation.widget.WidgetMovieListResponse {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.choo.moviefinder.presentation.widget.WidgetMovie {
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
