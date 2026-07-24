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
    alias(libs.plugins.kotlin.compose)
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
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"${localProperties.getProperty("TMDB_READ_ACCESS_TOKEN", "")}\"")
        buildConfigField("String", "TMDB_V4_BASE_URL", "\"https://api.themoviedb.org/4/\"")
        buildConfigField("String", "KOFIC_API_KEY", "\"${localProperties.getProperty("KOFIC_API_KEY", "")}\"")
        buildConfigField("String", "KOFIC_BASE_URL", "\"https://www.kobis.or.kr/kobisopenapi/webservice/rest/\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        create("release") {
            // RELEASE_KEYSTORE_PATH 환경변수가 있을 때만 릴리즈 서명 활성화.
            // 로컬 개발 및 CI R8 검증 빌드에서는 디버그 서명으로 대체됨.
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
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
            val releaseConfig = signingConfigs.getByName("release")
            signingConfig = if (releaseConfig.storeFile != null) releaseConfig
                            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = AndroidConfig.JAVA_VERSION
        targetCompatibility = AndroidConfig.JAVA_VERSION
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    androidResources {
        localeFilters += setOf("ko", "en")
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

// Hilt의 kotlin-metadata-jvm이 Kotlin 메타데이터 2.3.0까지만 지원하므로,
// AGP 9.x가 강제하는 kotlin-stdlib:2.4.0 승격을 차단한다. (AGP 9.3.1에서도 여전히 발생 확인됨)
configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
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

    // ViewPager2
    implementation(libs.androidx.viewpager2)

    // YouTube Player (인앱 예고편 재생)
    implementation(libs.youtube.player)

    // Browser (Chrome Custom Tabs)
    implementation(libs.androidx.browser)

    // Security Crypto (EncryptedSharedPreferences for TMDB tokens)
    implementation(libs.androidx.security.crypto)

    // Timber
    implementation(libs.timber)

    // ML Kit Image Labeling unbundled (포스터 태그 자동 추천 — 모델은 Play Services에서 다운로드)
    implementation(libs.mlkit.image.labeling)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

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
