# MovieFinder 신규 개발자 온보딩 가이드

> 이 문서는 MovieFinder 프로젝트에 처음 합류한 개발자가  
> 환경 셋업부터 첫 번째 PR 병합까지 막힘 없이 진행할 수 있도록 작성되었습니다.

---

## 목차

1. [개발 환경 셋업](#1-개발-환경-셋업)
2. [프로젝트 구조 설명](#2-프로젝트-구조-설명)
3. [첫 번째 PR 내기까지의 흐름](#3-첫-번째-pr-내기까지의-흐름)
4. [자주 하는 실수 및 주의사항](#4-자주-하는-실수-및-주의사항)
5. [핵심 코드 패턴 설명](#5-핵심-코드-패턴-설명)

---

## 1. 개발 환경 셋업

### 1-1. 필수 도구

| 도구 | 버전 | 비고 |
|------|------|------|
| **Android Studio** | Meerkat 이상 권장 | |
| **JDK** | **21** (필수) | AGP 9.x는 JDK 17로는 빌드 안 됨 |
| **Gradle** | 9.3.1 | Wrapper로 자동 설치됨 |
| **AGP** | 9.2.1 | Kotlin 2.3.21 내장 |

> Android Studio → Settings → Build → Gradle JDK 를 **JDK 21**로 설정하세요.

---

### 1-2. 저장소 클론

```bash
git clone <repo-url>
cd MovieFinder
```

---

### 1-3. `local.properties` 설정 (필수)

`local.properties` 파일은 `.gitignore`에 포함되어 있어 직접 생성해야 합니다.

```properties
# local.properties (프로젝트 루트)
sdk.dir=/Users/<your-username>/Library/Android/sdk

# TMDB API 키 (https://www.themoviedb.org/settings/api 에서 발급)
TMDB_API_KEY=여기에_v3_API_키_입력
TMDB_READ_ACCESS_TOKEN=여기에_v4_Read_Access_Token_입력
```

> **팀 리드에게 키를 요청하세요.** CI에서는 GitHub Secret `TMDB_API_KEY`가 자동 주입됩니다.  
> 키가 없으면 빌드는 성공하지만 앱 실행 시 API 인증 오류가 발생합니다.

---

### 1-4. Git Hooks 활성화

프로젝트에는 커밋/푸시 품질 게이트가 내장되어 있습니다. **한 번만 실행하면 됩니다.**

```bash
git config core.hooksPath .githooks
```

| Hook | 실행 시점 | 내용 |
|------|----------|------|
| `pre-commit` | `git commit` 직전 | Detekt 정적 분석 + Kotlin 컴파일 체크 |
| `pre-push` | `git push` 직전 | 전체 유닛 테스트 실행 |

> `pre-push`는 509개 테스트를 실행하므로 **1~3분** 소요됩니다. 정상입니다.

---

### 1-5. 첫 빌드 및 실행

```bash
# 디버그 빌드
./gradlew assembleDebug

# 유닛 테스트
./gradlew testDebugUnitTest

# Detekt 정적 분석
./gradlew :app:detekt

# JaCoCo 커버리지 리포트 (app/build/reports/jacoco/.../html/index.html)
./gradlew jacocoTestReport
```

> 첫 빌드는 Gradle 캐시가 없어 5~10분 걸릴 수 있습니다.

---

## 2. 프로젝트 구조 설명

### 2-1. 아키텍처 개요

**Clean Architecture + MVVM** 패턴을 따릅니다. 레이어 간 의존 방향은 단방향입니다.

```
Presentation  →  Domain  ←  Data
     ↑              ↑
  (ViewModel)   (UseCase / Repository Interface)
```

- **Domain 레이어**: 순수 Kotlin. Android 프레임워크 미사용. 49개 UseCase.
- **Data 레이어**: API 통신(Retrofit), 로컬 DB(Room), Repository 구현체.
- **Presentation 레이어**: Fragment + ViewModel. Domain UseCase만 참조. Data 레이어 직접 참조 금지.
- **Core**: 모든 레이어에서 사용하는 유틸리티.

### 2-2. 디렉토리 구조

```
app/src/main/java/com/choo/moviefinder/
├── core/
│   ├── notification/     # 개봉일·워치리스트·목표 알림
│   ├── startup/          # App Startup (Timber, StrictMode 초기화)
│   └── util/             # 공통 유틸 (RateLimiter, ExponentialBackoff, CoroutineExt 등)
├── data/
│   ├── local/            # Room DB (version 21), DAO 8개, Entity 11개
│   ├── paging/           # PagingSource 4개 + RemoteMediator
│   ├── remote/           # Retrofit API Service + DTO
│   └── repository/       # Repository 구현체 9개
├── di/                   # Hilt 모듈 (Database, DataStore, Network, Repository)
├── domain/
│   ├── model/            # 도메인 모델 (순수 Kotlin data class)
│   ├── repository/       # Repository 인터페이스 11개
│   └── usecase/          # UseCase 49개 (각 파일 1개 UseCase)
└── presentation/
    ├── adapter/          # RecyclerView 어댑터 8개
    ├── common/           # 커스텀 뷰 (CircularRatingView, PieChartView 등)
    ├── detail/           # 영화 상세 (DetailFragment + ViewModel + Delegates)
    ├── favorite/         # 즐겨찾기·워치리스트 탭
    ├── home/             # 홈 (현재 상영작·인기·트렌딩 탭)
    ├── search/           # 영화·배우 검색 + 필터
    ├── settings/         # 테마·언어·목표·백업 설정
    ├── stats/            # 시청 통계 (차트 4종)
    └── widget/           # 홈 화면 위젯 3종
```

### 2-3. 핵심 파일 빠른 참조

| 파일 | 역할 |
|------|------|
| `buildSrc/AndroidConfig.kt` | compileSdk, minSdk, 버전 상수 한 곳 관리 |
| `di/NetworkModule.kt` | Retrofit, OkHttp, CertificatePinner 설정 |
| `di/DatabaseModule.kt` | Room DB 초기화, DAO 바인딩 |
| `core/util/CoroutineExt.kt` | `launchWithErrorHandler`, `suspendRunCatching`, `WhileSubscribed5s` |
| `core/util/ErrorMessageProvider.kt` | 예외 → `ErrorType` 변환 (UI 메시지 중앙 관리) |
| `data/util/Constants.kt` | `DEFAULT_PAGING_CONFIG`, `PAGE_SIZE` 등 상수 |

---

## 3. 첫 번째 PR 내기까지의 흐름

### 3-1. 브랜치 전략

```bash
# main 브랜치 최신화
git pull origin main

# 기능 브랜치 생성 (예시)
git checkout -b feat/your-feature-name
```

> `main` 브랜치에 직접 push하면 CI가 동작하지만 보호 규칙에 걸립니다.  
> 반드시 브랜치를 만들고 PR을 통해 병합하세요.

---

### 3-2. 코드 작성 후 커밋

```bash
# 스테이징
git add app/src/...

# 커밋 — pre-commit hook 자동 실행 (Detekt + 컴파일 체크)
git commit -m "feat: 새 기능 설명"
```

Detekt가 실패하면 커밋이 차단됩니다. 오류 메시지를 확인하고 수정 후 재커밋하세요.

```bash
# Detekt 결과 HTML 리포트 확인
open app/build/reports/detekt/detekt.html
```

---

### 3-3. 푸시

```bash
# pre-push hook 자동 실행 (전체 유닛 테스트)
git push origin feat/your-feature-name
```

테스트 실패 시 push가 차단됩니다.

---

### 3-4. PR 생성 및 CI

PR을 열면 GitHub Actions CI가 자동 실행됩니다:

```
Detekt → Lint → Debug Build → Release Build (R8/ProGuard 검증) → 유닛 테스트 → JaCoCo 커버리지
```

**커버리지 최소 기준: 50%** — 새 코드에는 테스트를 함께 작성해야 합니다.

> **Release Build가 실패하는 경우**: R8 Full Mode가 활성화되어 있으므로 직렬화 클래스에  
> `proguard-rules.pro`의 keep 규칙이 없으면 릴리스 빌드만 실패합니다.

---

### 3-5. PR 체크리스트

PR 전에 직접 확인하세요:

- [ ] `./gradlew :app:detekt` 통과
- [ ] `./gradlew testDebugUnitTest` 통과
- [ ] 새 기능에 유닛 테스트 추가
- [ ] Presentation 레이어에서 Data 레이어를 직접 import하지 않음
- [ ] `CancellationException` 재전파 처리
- [ ] ViewBinding `_binding = null` on `onDestroyView()`

---

## 4. 자주 하는 실수 및 주의사항

### ❌ 실수 1: `local.properties` 키 누락

**증상**: 빌드는 성공하지만 앱 실행 시 `401 Unauthorized`.  
**해결**: `local.properties`에 `TMDB_API_KEY`와 `TMDB_READ_ACCESS_TOKEN` 모두 추가.

---

### ❌ 실수 2: Git Hooks 미활성화

**증상**: 동료의 커밋은 Detekt를 통과하는데 내 커밋은 CI에서만 실패.  
**해결**: `git config core.hooksPath .githooks` 한 번만 실행.

---

### ❌ 실수 3: Presentation에서 Data 레이어 직접 import

```kotlin
// ❌ 절대 금지
import com.choo.moviefinder.data.repository.MovieRepositoryImpl

// ✅ 올바른 방법 — Domain UseCase 사용
class MyViewModel @Inject constructor(
    private val getMoviesUseCase: GetNowPlayingMoviesUseCase
)
```

---

### ❌ 실수 4: `CancellationException` 삼키기

```kotlin
// ❌ 코루틴 취소가 무시됨
try {
    apiCall()
} catch (e: Exception) {
    _error.value = e.message
}

// ✅ CancellationException은 반드시 재전파
try {
    apiCall()
} catch (e: CancellationException) {
    throw e          // ← 이 줄이 없으면 코루틴 취소가 동작하지 않음
} catch (e: Exception) {
    _error.value = e.message
}
```

PagingSource, catch 블록 어디서든 동일하게 적용됩니다.

---

### ❌ 실수 5: ViewBinding 메모리 누수

```kotlin
// ❌ _binding을 null로 만들지 않으면 Fragment가 소멸된 후에도 View 참조 유지
override fun onDestroyView() {
    super.onDestroyView()
    // 빠뜨리면 LeakCanary가 경고를 발생시킴
}

// ✅ 올바른 패턴
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!

override fun onDestroyView() {
    super.onDestroyView()
    binding.shimmerLayout.stopShimmer()   // Shimmer가 있으면 먼저 중지
    _binding = null
}
```

---

### ❌ 실수 6: `kotlinOptions` 블록 사용 (AGP 9.x)

```kotlin
// ❌ AGP 9.x에서는 컴파일 오류
android {
    kotlinOptions { jvmTarget = "17" }
}

// ✅ AGP 9.x는 Kotlin이 내장되어 있어 별도 설정 불필요
// compileOptions의 sourceCompatibility / targetCompatibility만 설정
```

---

### ❌ 실수 7: 새 직렬화 클래스에 ProGuard 규칙 누락

`@Serializable` data class를 추가하면 `proguard-rules.pro`에 keep 규칙을 추가해야 합니다.  
릴리스 빌드만 실패하므로 디버그 테스트만 하면 놓치기 쉽습니다.

```proguard
# proguard-rules.pro 예시
-keepclassmembers class com.choo.moviefinder.data.remote.dto.YourNewDto {
    <fields>;
}
```

---

### ❌ 실수 8: Room 새 Entity 추가 시 DB 버전 미증가

현재 DB 버전: **21**. Entity나 컬럼을 추가/변경하면:

1. `MovieDatabase.kt`의 `version` 증가
2. `migrations/` 에 `Migration(old, new)` 객체 추가
3. `app/schemas/` 에 스키마 파일 자동 생성 확인 (`./gradlew generateDebugRoomSchemas`)

> 마이그레이션 없이 버전만 올리면 `fallbackToDestructiveMigration()`으로 데이터가 초기화됩니다.  
> 개발 중에는 괜찮지만 릴리스 빌드에서는 사용자 데이터 손실이 발생합니다.

---

## 5. 핵심 코드 패턴 설명

### 패턴 1: Flow 수집 — `repeatOnLifecycle`

모든 Fragment의 Flow 수집은 이 구조를 따릅니다.

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // 독립적인 Flow는 병렬로 수집
            launch { viewModel.uiState.collect { render(it) } }
            launch { viewModel.snackbarEvent.collect { showSnackbar(it) } }
        }
    }
}
```

> `repeatOnLifecycle(STARTED)`: 화면이 보일 때만 수집, 백그라운드에서 자동 중단됩니다.  
> 여러 `launch { }` 블록을 하나의 `repeatOnLifecycle` 안에 두면 생명주기를 공유합니다.

---

### 패턴 2: 에러 처리 — `launchWithErrorHandler`

ViewModel에서 코루틴 에러 처리의 표준 패턴입니다.

```kotlin
// core/util/CoroutineExt.kt
fun CoroutineScope.launchWithErrorHandler(
    onError: suspend (ErrorType) -> Unit,
    block: suspend () -> Unit
) {
    launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e   // 코루틴 취소는 반드시 재전파
        } catch (e: Exception) {
            onError(ErrorMessageProvider.getErrorType(e))
        }
    }
}
```

```kotlin
// ViewModel에서 사용 예시
fun loadData() {
    viewModelScope.launchWithErrorHandler(
        onError = { errorType -> _snackbarEvent.trySend(errorType) }
    ) {
        val result = repository.getData()
        _uiState.value = UiState.Success(result)
    }
}
```

---

### 패턴 3: 일회성 이벤트 — `Channel.CONFLATED`

Snackbar, Toast 같은 일회성 이벤트는 모든 ViewModel에서 이 패턴을 씁니다.

```kotlin
// ViewModel
private val _snackbarEvent = Channel<ErrorType>(Channel.CONFLATED)
val snackbarEvent = _snackbarEvent.receiveAsFlow()

// 이벤트 발송
_snackbarEvent.trySend(ErrorType.Network)

// Fragment에서 수집
launch { viewModel.snackbarEvent.collect { showSnackbar(it) } }
```

> `Channel.CONFLATED`: 최신 이벤트만 유지합니다. 화면 회전 시 같은 Snackbar가 두 번 뜨지 않습니다.  
> `StateFlow`가 아닌 이유: 한 번 소비된 이벤트가 재구독 시 다시 발행되면 안 되기 때문입니다.

---

### 패턴 4: 중복 API 호출 방지 — `Mutex.tryLock()`

```kotlin
private val loadingMutex = Mutex()

fun loadMovieDetail() {
    if (!loadingMutex.tryLock()) return   // 이미 로딩 중이면 즉시 리턴
    viewModelScope.launch {
        try {
            _uiState.value = UiState.Loading
            // ... API 호출
        } finally {
            loadingMutex.unlock()   // 에러 후에도 재시도 가능하도록 항상 해제
        }
    }
}
```

> `if (isLoading) return` 방식은 코루틴 환경에서 체크-후-세트 경쟁 조건이 생깁니다.  
> `Mutex.tryLock()`은 원자적으로 잠금을 획득하므로 안전합니다.

---

### 패턴 5: FAB 연타 방지 — `toggleMutex.withLock()`

```kotlin
private val toggleMutex = Mutex()

fun toggleFavorite() {
    viewModelScope.launch {
        toggleMutex.withLock {   // 이전 토글 완료까지 대기 후 실행
            toggle.toggleFavorite(movieId)
        }
    }
}
```

> `tryLock()`과의 차이: `tryLock()`은 획득 실패 시 즉시 리턴, `withLock()`은 대기 후 순서대로 실행.  
> FAB 토글은 "무시"가 아닌 "순서 보장"이 필요하므로 `withLock()`을 씁니다.

---

### 패턴 6: StateFlow의 `stateIn` 설정

```kotlin
// core/util/CoroutineExt.kt
val WhileSubscribed5s = SharingStarted.WhileSubscribed(5_000)

// ViewModel에서 사용
val isFavorite: StateFlow<Boolean> = toggle.isFavorite(movieId)
    .stateIn(viewModelScope, WhileSubscribed5s, false)
    //                        ↑ 구독자가 없어도 5초간 업스트림 유지 (화면 회전 대응)
```

> 항상 `WhileSubscribed5s`를 사용하세요. `Eagerly`는 ViewModel 생존 기간 내내 Flow를 구독해  
> 불필요한 DB 쿼리가 계속 실행됩니다.

---

### 패턴 7: 점진적 로딩 (DetailViewModel)

상세 화면은 핵심 데이터를 먼저 표시하고, 보조 데이터를 순차적으로 업데이트합니다.

```kotlin
// 1단계: 핵심 데이터(영화 상세 + 등급) → Success 상태로 전환
_uiState.value = DetailUiState.Success(detail = movieDetail, certification = cert)

// 2단계: 보조 데이터 완료 시마다 update (null = 아직 로딩 중)
_uiState.update { current ->
    if (current is DetailUiState.Success) current.copy(credits = credits)
    else current
}
```

> `DetailUiState.Success`의 `credits`, `similar`, `reviews` 등은 nullable입니다.  
> Fragment에서 null 체크로 로딩 중/완료를 구분합니다.

---

### 패턴 8: Abstract DAO + `@Transaction`

`FavoriteMovieDao`, `WatchlistDao`, `WatchHistoryDao`는 `interface`가 아닌 `abstract class`입니다.

```kotlin
// 여러 DAO 작업을 원자적으로 실행 가능
@Dao
abstract class FavoriteMovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: FavoriteMovieEntity)

    @Transaction
    open suspend fun toggleFavorite(entity: FavoriteMovieEntity) {
        if (isFavoriteNow(entity.id)) delete(entity.id)
        else insert(entity)
    }
}
```

> `interface`에서는 `@Transaction` 래퍼 메서드를 구현할 수 없습니다.  
> `database.withTransaction {}` 대신 이 패턴을 쓰면 MockK 테스트도 단순해집니다.

---

### 패턴 9: 오프라인 지원 — RemoteMediator vs PagingSource

| 화면 | 방식 | 이유 |
|------|------|------|
| 홈 (현재 상영작, 인기) | `RemoteMediator` + Room | 오프라인에서도 캐시 표시 |
| 검색 | `MoviePagingSource` (네트워크 전용) | 검색 결과는 캐시 불필요 |
| 트렌딩 | `TrendingPagingSource` (네트워크 전용) | 실시간 데이터 캐시 부적합 |

```kotlin
// RemoteMediator: 오프라인이면 API 호출 없이 즉시 성공 반환
override suspend fun load(...): MediatorResult {
    if (loadType == LoadType.REFRESH && !networkMonitor.isConnected.value) {
        return MediatorResult.Success(endOfPaginationReached = false)
    }
    // ...
}
```

---

## 추가 참고 자료

- **CLAUDE.md** (프로젝트 루트): 전체 기술 스택, 아키텍처 규칙, 주의사항 총망라
- **CI 결과**: GitHub Actions 탭 → `Android CI` 워크플로우
- **커버리지 리포트**: PR 댓글에 자동 게시 (`pr-coverage.yml`)
- **인증서 핀 갱신**: `cert-pin-check.yml` 매주 월요일 자동 검증, 불일치 시 Issue 생성

---

*최종 업데이트: 2026-05-27 | 프로젝트 기준 버전: Room v21, AGP 9.2.1*
