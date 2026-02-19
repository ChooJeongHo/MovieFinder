import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.baseline.profile)
    alias(libs.plugins.detekt)
    jacoco
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.choo.moviefinder"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.choo.moviefinder"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TMDB_API_KEY", "\"${localProperties.getProperty("TMDB_API_KEY", "")}\"")
        buildConfigField("String", "TMDB_BASE_URL", "\"https://api.themoviedb.org/3/\"")
        buildConfigField("String", "TMDB_IMAGE_BASE_URL", "\"https://image.tmdb.org/t/p/\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

baselineProfile {
    dexLayoutOptimization = true
}

detekt {
    config.setFrom("$rootDir/detekt.yml")
    buildUponDefaultConfig = true
    parallel = true
    source.setFrom(
        "src/main/java",
        "src/test/java"
    )
}


tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "Reporting"
    description = "Generate JaCoCo test coverage report"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/classes") {
        exclude(
            "**/R.class", "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*_Hilt*.*", "**/Hilt_*.*",
            "**/*_Factory.*", "**/*_MembersInjector.*",
            "**/*Directions*.*", "**/*Args*.*",
            "**/*Database_Impl*.*", "**/*Dao_Impl*.*"
        )
    }
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class", "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*_Hilt*.*", "**/Hilt_*.*",
            "**/*_Factory.*", "**/*_MembersInjector.*",
            "**/*Directions*.*", "**/*Args*.*",
            "**/*Database_Impl*.*", "**/*Dao_Impl*.*"
        )
    }

    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(files("${layout.buildDirectory.get()}/jacoco/testDebugUnitTest.exec"))
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Activity & Fragment
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Material Components
    implementation(libs.google.material)

    // ConstraintLayout
    implementation(libs.androidx.constraintlayout)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // SwipeRefreshLayout
    implementation(libs.androidx.swiperefreshlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Shimmer
    implementation(libs.facebook.shimmer)

    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // Baseline Profiles
    implementation(libs.androidx.profileinstaller)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // App Startup
    implementation(libs.androidx.startup)

    // Timber
    implementation(libs.timber)

    // LeakCanary (debug only)
    debugImplementation(libs.leakcanary)

    // Detekt (KtLint rules)
    detektPlugins(libs.detekt.rules.ktlint)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.paging.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
