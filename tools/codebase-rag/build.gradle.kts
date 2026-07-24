// MovieFinder 코드베이스 미니 RAG 개발 도구.
// 앱(:app)과는 완전히 분리된 순수 Kotlin/JVM 모듈이며, 릴리스/디버그 APK 어디에도 포함되지 않는다.
// 실행은 Gradle task(ragIndex, ragAsk)로만 가능 — 앱 사용자에게 노출되지 않음.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

dependencies {
    add("kspJvm", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val ragIndex by tasks.registering(JavaExec::class) {
    group = "codebase-rag"
    description = "domain/presentation/data .kt 파일 + JaCoCo 커버리지 + gfxinfo 성능 측정 결과를 청크 분할 " +
        "+ 임베딩 후 Room(SQLite)에 인덱싱한다. " +
        "-Ppaths=\"domain/repository,data/repository\" 로 특정 하위 경로만 샘플 인덱싱 가능(기본은 전체)."
    mainClass.set("com.choo.moviefinder.rag.IndexerKt")
    classpath = files(
        kotlin.jvm().compilations.getByName("main").output.allOutputs,
        configurations.getByName("jvmRuntimeClasspath"),
    )
    workingDir = rootProject.projectDir
    args = (project.findProperty("paths") as String?)
        ?.split(",")
        ?.map { it.trim() }
        ?: emptyList()
}

val ragIndexPerf by tasks.registering(JavaExec::class) {
    group = "codebase-rag"
    description = "performance-data/screen_performance.json(gfxinfo 측정 결과)만 다시 읽어 PERFORMANCE_METRIC " +
        "청크만 갱신한다 - 전체 재인덱싱(ragIndex) 없이 성능 데이터만 최신화할 때 사용."
    mainClass.set("com.choo.moviefinder.rag.PerfIndexerKt")
    classpath = files(
        kotlin.jvm().compilations.getByName("main").output.allOutputs,
        configurations.getByName("jvmRuntimeClasspath"),
    )
    workingDir = rootProject.projectDir
}

val ragAsk by tasks.registering(JavaExec::class) {
    group = "codebase-rag"
    description = "자연어 질문을 임베딩 -> 코사인 유사도 검색 -> Claude API 답변까지 실행한다. " +
        "-Pquestion=\"...\" 으로 질문 전달, -Ptypes=\"CLASS,FUNCTION\" 으로 청크 타입 필터링(기본은 전체 타입)."
    mainClass.set("com.choo.moviefinder.rag.QueryKt")
    classpath = files(
        kotlin.jvm().compilations.getByName("main").output.allOutputs,
        configurations.getByName("jvmRuntimeClasspath"),
    )
    workingDir = rootProject.projectDir
    args = listOfNotNull(
        project.findProperty("question") as String?,
        project.findProperty("types") as String?,
    )
}
