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
        version = release(AndroidConfig.COMPILE_SDK)
    }

    defaultConfig {
        applicationId = "com.choo.moviefinder"
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = AndroidConfig.VERSION_CODE
        versionName = AndroidConfig.VERSION_NAME

        testInstrumentationRunner = "com.choo.moviefinder.HiltTestRunner"

        buildConfigField("String", "TMDB_API_KEY", "\"${localProperties.getProperty("TMDB_API_KEY", "")}\"")
        buildConfigField("String", "TMDB_BASE_URL", "\"https://api.themoviedb.org/3/\"")
        buildConfigField("String", "TMDB_IMAGE_BASE_URL", "\"https://image.tmdb.org/t/p/\"")

        resourceConfigurations += setOf("ko", "en")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // CI 환경에서는 디버그 서명으로 R8/ProGuard 검증 빌드 수행
            if (System.getenv("CI") != null) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = AndroidConfig.JAVA_VERSION
        targetCompatibility = AndroidConfig.JAVA_VERSION
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
    parallel = false
    source.setFrom(
        "src/main/java",
        "src/test/java"
    )
}


tasks.withType<Test> {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
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

    val jacocoExcludes = listOf(
        // Generated
        "**/R.class", "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*_Hilt*.*", "**/Hilt_*.*",
        "**/*_Factory.*", "**/*_MembersInjector.*",
        "**/*Directions*.*", "**/*Args*.*",
        "**/*Database_Impl*.*", "**/*Dao_Impl*.*",
        // UI — only testable via Espresso
        "**/presentation/**/*Fragment*.*",
        "**/presentation/**/*Adapter*.*",
        "**/presentation/**/*ViewHolder*.*",
        "**/presentation/widget/**",
        "**/presentation/common/**",
        // Application / Activity
        "**/MovieFinderApp*.*",
        "**/MainActivity*.*",
        // Debug-only utilities
        "**/core/startup/**",
        "**/core/util/DebugHealthCheck*.*",
        "**/core/util/AnrWatchdog*.*",
        "**/core/util/FileLoggingTree*.*",
        "**/core/util/DebugEventListener*.*",
        "**/core/util/StrictModeInitializer*.*",
        // WorkManager workers
        "**/core/notification/*Worker*.*"
    )
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoExcludes)
    }

    classDirectories.setFrom(files(kotlinClasses))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(files("${layout.buildDirectory.get()}/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("testDebugUnitTest")
    group = "Verification"
    description = "Verify minimum JaCoCo test coverage"

    val jacocoExcludes = listOf(
        // Generated
        "**/R.class", "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*_Hilt*.*", "**/Hilt_*.*",
        "**/*_Factory.*", "**/*_MembersInjector.*",
        "**/*Directions*.*", "**/*Args*.*",
        "**/*Database_Impl*.*", "**/*Dao_Impl*.*",
        // UI — only testable via Espresso
        "**/presentation/**/*Fragment*.*",
        "**/presentation/**/*Adapter*.*",
        "**/presentation/**/*ViewHolder*.*",
        "**/presentation/widget/**",
        "**/presentation/common/**",
        // Application / Activity
        "**/MovieFinderApp*.*",
        "**/MainActivity*.*",
        // Debug-only utilities
        "**/core/startup/**",
        "**/core/util/DebugHealthCheck*.*",
        "**/core/util/AnrWatchdog*.*",
        "**/core/util/FileLoggingTree*.*",
        "**/core/util/DebugEventListener*.*",
        "**/core/util/StrictModeInitializer*.*",
        // WorkManager workers
        "**/core/notification/*Worker*.*"
    )
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(jacocoExcludes)
    }

    classDirectories.setFrom(files(kotlinClasses))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(files("${layout.buildDirectory.get()}/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))

    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}

dependencies {
    baselineProfile(project(":baselineprofile"))

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
    implementation(libs.bundles.lifecycle)

    // Navigation
    implementation(libs.bundles.navigation)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime)

    // Retrofit + OkHttp
    implementation(libs.bundles.retrofit)
    debugImplementation(libs.okhttp.logging.interceptor)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil
    implementation(libs.bundles.coil)

    // Shimmer
    implementation(libs.facebook.shimmer)

    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // Baseline Profiles
    implementation(libs.androidx.profileinstaller)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore)

    // kotlinx-datetime
    implementation(libs.kotlinx.datetime)

    // App Startup
    implementation(libs.androidx.startup)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Window
    implementation(libs.androidx.window)

    // Timber
    implementation(libs.timber)

    // ML Kit Image Labeling unbundled (포스터 태그 자동 추천 — 모델은 Play Services에서 다운로드)
    implementation(libs.mlkit.image.labeling)

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
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
