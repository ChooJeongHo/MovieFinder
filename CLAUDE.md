# MovieFinder - 영화 검색 안드로이드 앱

## 프로젝트 개요
TMDB (The Movie Database) API를 활용한 영화 검색, 상세 정보 조회, 즐겨찾기 기능을 갖춘 안드로이드 앱입니다.

## 기술 스택

### 빌드 환경
- **AGP**: 9.2.1 (Android Gradle Plugin, Kotlin 2.3.21 내장)
- **Gradle**: 9.3.1
- **compileSdk**: 36 (Android 15)
- **minSdk**: 24 / **targetSdk**: 36
- **KSP**: 2.3.9 (Kotlin Symbol Processing)

### 핵심 라이브러리
| 라이브러리 | 용도 |
|---|---|
| AppCompat | Android 하위 호환성 |
| Material Components | Material Design XML 컴포넌트 |
| Navigation (Fragment) | Safe Args 기반 Fragment 네비게이션 |
| Fragment KTX | Fragment 확장 함수 |
| ConstraintLayout | 유연한 레이아웃 |
| RecyclerView | 리스트/그리드 표시 |
| Hilt | 의존성 주입 (DI) |
| Room | 로컬 SQLite 데이터베이스 |
| Room Paging | Room + Paging 3 통합 (RemoteMediator) |
| Paging 3 | 무한 스크롤 페이징 |
| Retrofit 3 | REST API 통신 |
| OkHttp 5 | HTTP 클라이언트 + 로깅 + 응답 캐시 (kotlin.time.Duration API) |
| kotlinx.serialization | JSON 직렬화/역직렬화 |
| Coil 3 | 이미지 로딩 (View 버전, 메모리+디스크 캐시) |
| Facebook Shimmer | 로딩 Shimmer 애니메이션 |
| Lifecycle | 생명주기 인식 컴포넌트 |
| Splash Screen API | 스플래시 화면 |
| SwipeRefreshLayout | 당겨서 새로고침 |
| DataStore (Typed) | kotlinx.serialization JSON 기반 타입 안전 설정 저장 |
| Lifecycle Process | ProcessLifecycleOwner (앱 수준 생명주기) |
| ProfileInstaller | Baseline Profiles 설치 |
| Benchmark Macro | Baseline Profile 생성 |
| Timber | 구조화 로깅 |
| App Startup | 초기화 최적화 (Timber 등) |
| WorkManager | 개봉일 알림 스케줄링 |
| kotlinx-datetime | 멀티플랫폼 날짜/시간 API |
| Window | 윈도우 레이아웃 API |
| android-youtube-player | YouTube 예고편 외부 연결 (임베드 제한으로 인해 앱 외부 실행) |

### 개발/디버그 도구
| 도구 | 용도 |
|---|---|
| Detekt | Kotlin 정적 분석 + KtLint 규칙 |
| JaCoCo | 테스트 코드 커버리지 리포트 (최소 50% 기준) |
| LeakCanary | 메모리 누수 감지 (debugImplementation) |

### 테스트 라이브러리
| 라이브러리 | 용도 |
|---|---|
| MockK | Kotlin 모킹 프레임워크 |
| Turbine | Flow 테스트 유틸리티 |
| Coroutines Test | 코루틴 테스트 디스패처 |
| Paging Testing | PagingData 테스트 유틸리티 |
| JUnit 4 | 유닛 테스트 프레임워크 |
| Espresso | UI 테스트 프레임워크 (core + contrib) |
| Hilt Testing | Hilt 의존성 주입 테스트 지원 |

## 아키텍처

### Clean Architecture + MVVM
```
app/src/main/java/com/choo/moviefinder/
├── core/                  # 공유 유틸리티
│   ├── startup/
│   │   ├── TimberInitializer.kt
│   │   └── StrictModeInitializer.kt
│   ├── notification/
│   │   ├── ReleaseNotificationScheduler.kt
│   │   ├── ReleaseNotificationWorker.kt
│   │   └── WatchGoalNotificationHelper.kt
│   └── util/
│       ├── DateUtils.kt
│       ├── ImageUrlProvider.kt
│       ├── ErrorMessageProvider.kt
│       ├── NetworkMonitor.kt
│       ├── CoroutineExt.kt
│       ├── ExponentialBackoff.kt
│       ├── RateLimiter.kt
│       ├── DebugHealthCheck.kt
│       ├── DebugEventListener.kt
│       ├── FileLoggingTree.kt
│       └── AnrWatchdog.kt
├── baselineprofile/       # Baseline Profile 생성 모듈
├── data/                  # 데이터 레이어
│   ├── local/
│   │   ├── MovieDatabase.kt       # Room DB (version 23)
│   │   ├── dao/                   # DAO 8개 (FavoriteMovieDao, WatchlistDao: abstract class)
│   │   └── entity/                # Room Entity 11개
│   ├── paging/
│   │   ├── MoviePagingSource.kt
│   │   ├── DiscoverPagingSource.kt
│   │   ├── TrendingPagingSource.kt
│   │   └── MovieRemoteMediator.kt
│   ├── remote/            # Retrofit API (Service, DTO)
│   ├── repository/        # Repository 구현체 (9개 도메인별 분리)
│   └── util/              # 상수 (PAGE_SIZE, DEFAULT_PAGING_CONFIG 등)
├── di/                    # Hilt DI 모듈
│   ├── DatabaseModule.kt  # Room DB + DAO (destructive migration fallback)
│   ├── DataStoreModule.kt # Typed DataStore<UserSettings>
│   ├── NetworkModule.kt   # Retrofit/OkHttp (API key interceptor, CertificatePinner, Cache 10MB, @ImageOkHttpClient)
│   └── RepositoryModule.kt # 9개 도메인 @Binds + Preferences
├── domain/                # 도메인 레이어 (순수 Kotlin)
│   ├── model/             # 도메인 모델
│   ├── repository/        # Repository 인터페이스 11개
│   └── usecase/           # UseCase 49개
├── presentation/          # 프레젠테이션 레이어
│   ├── adapter/           # RecyclerView 어댑터 8개 + MovieGridViewHolder + MovieListViewHolder
│   ├── common/            # CircularRatingView, PieChartView, BarChartView, HistogramView, CalendarHeatmapView
│   ├── detail/            # DetailFragment, DetailViewModel, MemoDelegate, UserRatingDelegate
│   ├── favorite/          # FavoriteFragment, FavoriteViewModel (탭, 정렬, 태그 필터)
│   ├── home/              # HomeFragment, HomeViewModel (3탭, RemoteMediator)
│   ├── person/            # PersonDetailFragment, PersonDetailViewModel
│   ├── search/            # SearchFragment, SearchViewModel (필터, SavedStateHandle)
│   ├── settings/          # SettingsFragment, SettingsViewModel (테마/캐시/목표/백업)
│   ├── stats/             # StatsFragment, StatsViewModel (통계 카드 9개)
│   └── widget/            # PopularMoviesWidget, RemoteViewsService/Factory
├── MainActivity.kt        # NavHostFragment + 딥링크 처리 + 네트워크 Snackbar
└── MovieFinderApp.kt      # @HiltAndroidApp, Coil 캐시, CertificatePinner, NotificationChannel 2개
```

### 레이어 규칙
- **Core**: 공유 유틸리티, 모든 레이어에서 접근 가능
- **Domain**: 순수 Kotlin, Android 프레임워크 의존성 없음 (Paging 예외)
- **Repository 인터페이스**: ISP 기반 9개 도메인별 분리 + Preferences
- **Data**: API 통신, DB 접근, Repository 구현
- **Presentation**: Domain UseCase + Core에만 의존, Data 레이어 직접 참조 금지

### 데이터 흐름
```
API/DB → Repository → UseCase → ViewModel → Fragment (XML UI)
```
- 홈 (오프라인): API → RemoteMediator → Room DB → PagingSource → PagingDataAdapter
- 검색 (네트워크 전용): API → MoviePagingSource → PagingDataAdapter
- 즐겨찾기: `Flow<List<Entity>>` → `stateIn()` → `repeatOnLifecycle` 수집
- UI 상태: 하나의 `repeatOnLifecycle(STARTED)` 블록 안에서 여러 `launch { flow.collect {} }` 병렬 수집 (DetailFragment 6개, SearchFragment 9개)
- 즐겨찾기/워치리스트 정렬: 앱 레벨 `sortedBy` 대신 Room DAO `ORDER BY` 쿼리로 DB 레벨 처리 (`FavoriteSortOrder` → domain 레이어로 이동)
- 홈 오프라인: Room 캐시 즉시 표시 (1시간 캐시 만료: `RemoteKeyEntity.lastUpdated`)

## API 설정

`local.properties`에 API 키 추가:
```properties
TMDB_API_KEY=여기에_API_키_입력
```

| 엔드포인트 | 용도 |
|---|---|
| GET /movie/now_playing | 현재 상영작 (RemoteMediator) |
| GET /movie/popular | 인기 영화 (RemoteMediator) |
| GET /trending/movie/day | 일별 트렌딩 |
| GET /search/movie | 검색 (year 필터) |
| GET /discover/movie | 장르/정렬 탐색 |
| GET /genre/movie/list | 장르 목록 |
| GET /movie/{id} | 영화 상세 |
| GET /movie/{id}/credits | 출연진 |
| GET /movie/{id}/similar | 비슷한 영화 |
| GET /movie/{id}/recommendations | 추천 영화 |
| GET /movie/{id}/videos | 예고편 (YouTube 키) |
| GET /movie/{id}/release_dates | 콘텐츠 등급 (KR→US 폴백) |
| GET /movie/{id}/reviews | 리뷰 |
| GET /person/{id} | 배우 상세 |
| GET /person/{id}/movie_credits | 배우 출연작 |

- API 키: `BuildConfig` 주입 → OkHttp Interceptor로 자동 추가
- HttpLoggingInterceptor: 디버그 빌드에서만 생성
- CertificatePinner: `api.themoviedb.org` + `image.tmdb.org` leaf + intermediate SHA-256 핀
- HTTP 응답 캐시: OkHttp Cache 10MB

## 테스트

### 유닛 테스트 (638개)
```bash
./gradlew testDebugUnitTest
```
주요 커버: ViewModel 5종, Repository 9종, UseCase 5종, PagingSource 3종, 유틸/워커 다수.

### Espresso UI 테스트 (23개)
```bash
./gradlew connectedDebugAndroidTest
```
- `MainActivityTest` (5): 하단 네비게이션, 화면 이동 검증
- HiltTestRunner 설정, `@HiltAndroidTest` + `HiltAndroidRule`

### 테스트 패턴
- MockK + Turbine + StandardTestDispatcher + `Dispatchers.setMain()`
- `coEvery`/`coVerify`: suspend 함수 모킹
- `testOptions.unitTests.isReturnDefaultValues = true`: Android API 스텁 기본값

## 빌드 및 실행

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :app:detekt
./gradlew jacocoTestReport   # 리포트: app/build/reports/jacoco/jacocoTestReport/html/
```

딥링크 테스트:
```bash
adb shell am start -a android.intent.action.VIEW -d "moviefinder://movie/550"
adb shell am start -a android.intent.action.VIEW -d "moviefinder://person/6193"
adb shell am start -a android.intent.action.VIEW -d "moviefinder://stats"
```

## CI/CD (GitHub Actions)

| 워크플로우 | 트리거 | 내용 |
|---|---|---|
| `android-ci.yml` | push/PR → main | Detekt → Lint → Debug Build → Release Build(R8 검증) → Test → JaCoCo (최소 50%); APK 업로드는 main push 전용 |
| `cert-pin-check.yml` | 매주 월요일 cron | 인증서 핀 검증, 불일치 시 Issue 생성 |
| `pr-coverage.yml` | PR → main | JaCoCo 커버리지 PR 코멘트 자동 게시 |

- Pre-commit Hook: Detekt + 컴파일 체크 (`.githooks/pre-commit`)
- Pre-push Hook: 유닛 테스트 전체 실행 (`.githooks/pre-push`)
- Hooks 활성화: `git config core.hooksPath .githooks`
- GitHub Secrets: `TMDB_API_KEY` 필요
- Dependabot: 라이브러리 자동 버전 업데이트

## Claude Code 훅 / 서브에이전트 자동화

> `.claude/`는 `.gitignore`로 전체 무시됨 — 아래 내용은 이 저장소를 사용하는 각자의 로컬 환경에만
> 존재하며 git으로 공유되지 않는다. 팀원이 동일하게 쓰려면 각자 셋업 필요.

### kotlin-architecture-reviewer 서브에이전트
- 위치: `.claude/agents/kotlin-architecture-reviewer.md` (model: opus, tools: Read/Grep/Glob/Bash)
- 스코프: Kotlin 코루틴/Flow 안전성(체크리스트 B) + Clean Architecture 레이어 규칙(체크리스트 A) +
  Paging/RemoteMediator(C) + UI 상태 설계(D)만 리뷰. 스타일/네이밍/OWASP 보안/접근성/테스트 커버리지는
  스코프 밖 — 관련 스킬(android-security-reviewer, accessibility-checker, testing-pyramid 등)의 몫.
- **오탐 방지 규칙**: `@OptIn`/experimental API 관련 지적을 하려면 보고 전에 반드시
  `./gradlew :app:compileDebugKotlin`으로 검증하고, 컴파일이 성공하면 그 지적은 보고서에서 완전히
  제외한다 (레이어 위반·코루틴 안전성 등 컴파일과 무관한 항목엔 적용 안 됨).
- 수동 호출: "아키텍처 리뷰", "코루틴 리뷰", "레이어 위반 확인해줘" 요청 시 자동 트리거.

### PreToolUse 사전 검토 훅
- 위치: `.claude/hooks/pre-edit-architecture-review.sh` + `.claude/hooks/lib/reconstruct_edit.py`,
  `.claude/settings.json`의 `PreToolUse`(matcher: `Edit|Write`)에 등록.
- 트리거 조건: `domain/*.kt` 또는 `presentation/**/*ViewModel.kt` (`src/main` 한정) 파일에 Edit/Write
  시도 시.
- 동작: `tool_input`(old_string/new_string 또는 content)으로 "적용 예정" 파일 내용을 임시 경로에
  재구성 → `claude --agent kotlin-architecture-reviewer -p`로 헤드리스 호출 → 결과를
  `hookSpecificOutput.additionalContext`로 Claude Code에 전달.
- **비-차단 설계**: `permissionDecision`은 항상 `"allow"` — 이 훅은 어떤 경우에도 편집을 막지 않는다.
  이유: 이 리뷰는 opus의 판단(의견)이지 `domain-android-import-guard.sh`(android/androidx import
  차단, 결정론적 grep) 같은 100% 확정적 규칙이 아니다. 판단에는 오판 가능성이 있고, 리뷰 자체가
  2분 이상 걸릴 수 있어 매 Edit마다 강제로 차단하면 개발 흐름이 망가진다. 결정론적·항상-참 규칙만
  차단(`exit 2`)하고, 판단이 필요한 리뷰는 정보 제공(`allow` + `additionalContext`)으로 남긴다.
- Bash 권한은 `--allowedTools "Read,Grep,Glob,Bash(./gradlew :app:compileDebugKotlin:*)"`로 제한 —
  프롬프트 규칙이 깨져도 툴 레이어에서 이중으로 막도록 함.
- **알려진 제약**: 훅은 세션 시작 시에만 로드된다. 훅 설정/스크립트를 수정하면 Claude Code를 재시작해야
  반영된다. macOS 기본 환경엔 GNU `timeout`이 없어 순수 bash(`sleep N && kill`)로 타임아웃을 구현함.

### Stop 테스트 페어링 강제 훅
- 위치: `.claude/hooks/stop-test-coverage-guard.sh` + `.claude/hooks/lib/check_test_pairing.py`,
  `.claude/settings.json`의 `Stop`(matcher: `*`)에 등록.
- 트리거 조건: 이번 세션 transcript에서 `domain/*.kt` 또는 `presentation/**/*ViewModel.kt` (`src/main`
  한정) 파일을 Edit/Write/MultiEdit 했는데, 대응 테스트 파일(`app/src/test/.../XxxTest.kt`)이 같은
  세션에서 한 번도 Edit/Write/MultiEdit 되지 않은 경우.
- **차단 설계**: exit 2로 세션 종료를 막는다 — PreToolUse 훅과 정반대. 이유: "같은 세션에서 테스트
  파일이 건드려졌는가"는 transcript를 기계적으로 훑는 사실 확인이라 100% 결정론적이고 오탐 가능성이
  없다. 판단이 필요 없는 항상-참 규칙이므로 위 PreToolUse 훅의 원칙("결정론적 규칙만 차단")을 그대로
  따르면 차단이 맞다.
- 무한 루프 방지: 훅 입력의 `stop_hook_active`가 true면(이미 한 번 이 훅에 막혔다가 재개했다는 뜻)
  다시 막지 않고 통과시킨다 — 한 번은 스스로 고치거나 사유를 설명할 기회를 주되 영원히 막지는 않는다.
- **알려진 제약**: 서브에이전트(Agent 도구로 위임한 작업)가 만든 Edit/Write는 메인 세션 transcript에
  안 잡힌다 — 서브에이전트 자신의 별도 transcript에만 기록되기 때문. 즉 이 훅은 **메인 루프가 직접
  수정한 파일만** 감지하고, "구현자 서브에이전트에게 위임해서 만든 파일"은 놓친다. 서브에이전트가
  주도하는 워크플로우(예: feature-implementer 파이프라인)에서는 이 한계를 감안할 것.

## 주의사항

### AGP 9.x 특이사항
- `kotlinOptions` 블록 사용 불가 (Kotlin이 AGP에 내장)
- `android.disallowKotlinSourceSets=false` 필요 (KSP 호환)
- `Theme.MaterialComponents.DayNight.NoActionBar` 사용
- Hilt 2.59.2 이상 필요 (2.59.2 기준 Kotlin 2.4.0 미지원 — kotlin-metadata-jvm max 2.3.0)
- **Dependabot 호환성**: core-ktx 1.19.0은 compileSdk 37 요구; Kotlin 2.4.0은 Hilt 호환 이슈로 보류

### Navigation
- `nav_graph.xml`에 destination + argument + deepLink 정의
- `FragmentDirections`로 타입 안전 action, `navArgs()`로 Fragment 수신
- ViewModel에서: `savedStateHandle.get<Int>("movieId")`
- `NavigationUI.setupWithNavController()` + Detail에서 BottomNav 자동 숨김
- Detail self-navigation: `launchSingleTop="true"` (백스택 중복 방지)
- 딥링크: `moviefinder://movie/{id}`, `moviefinder://person/{id}`, `moviefinder://stats`

### Room DB
- 버전: 23, 이름: `movie_finder_db`, 스키마: `app/schemas/`
- Entity 11개: FavoriteMovieEntity, RecentSearchEntity, CachedMovieEntity, RemoteKeyEntity, WatchHistoryEntity, WatchlistEntity, UserRatingEntity, MemoEntity, ScheduledReminderEntity 외
- `FavoriteMovieDao`, `WatchlistDao`, `WatchHistoryDao`: `abstract class` — `@Transaction` wrapper 메서드 (toggleFavorite/toggleWatchlist, insertWithGenres/clearAllWithGenres)
- Destructive migration fallback 적용 (개발 환경)
- Room의 `withTransaction`은 MockK로 모킹 어려움 → `@Transaction` on abstract DAO 사용

### Coil 이미지 캐시
- 메모리 25%, 디스크 50MB 고정 (`cacheDir/image_cache`, `maxSizeBytes(50L * 1024 * 1024)`)
- `@ImageOkHttpClient`로 `image.tmdb.org` Certificate Pinning 적용
- 모든 어댑터: `onViewRecycled()` → `dispose()`
- 모든 어댑터 `load()` 블록: `size(ViewSizeResolver(imageView))` 명시 → 실제 표시 크기로 다운샘플링

### 다크 모드
- `values-night/colors.xml`: `icon_default`, `backdrop_overlay`
- 11개 아이콘 drawable: `android:fillColor="@color/icon_default"`

### Shared Element Transition
- `FragmentNavigatorExtras(posterView to "poster_$movieId")`
- DetailFragment `onCreate()`: sharedElementEnterTransition + ReturnTransition 설정
- `postponeEnterTransition(500, TimeUnit.MILLISECONDS)` + Coil `listener(onSuccess, onError)`
- nav_graph.xml action에서 enterAnim/exitAnim 제거 (Shared Element와 충돌)

### Fragment ViewBinding 패턴
- `_binding` nullable + `binding` non-null getter
- `onDestroyView()`: `_binding = null` + Shimmer `stopShimmer()`

### DataStore
- Typed `DataStore<UserSettings>` (kotlinx.serialization JSON)
- `Application.onCreate()`에서 `runBlocking { .first() }` — 초기 테마 동기 적용 (깜빡임 방지)
- `ProcessLifecycleOwner.lifecycleScope`로 이후 테마 변경 반영
- Application에서 DI 없이 접근: `@EntryPoint` + `EntryPointAccessors.fromApplication()`

### Certificate Pinning
- `NetworkModule.kt`의 `CertificatePinner` 설정 — 핀 상수 4개(`PIN_API_LEAF/INTER`, `PIN_IMAGE_LEAF/INTER`) + `buildApiCertPinner()` / `buildImageCertPinner()` helper로 3곳 중복 제거
- 디버그 빌드에서는 핀 비활성화 (`if (!BuildConfig.DEBUG)`)
- 인증서 갱신 시 핀 업데이트:
  ```bash
  echo | openssl s_client -connect api.themoviedb.org:443 2>/dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
  ```
- 위젯 OkHttp 싱글턴에도 CertificatePinner 적용 (Hilt 미사용)

### 네트워크 복원력
- **ExponentialBackoff**: `withExponentialBackoff()` (1s→2s→4s, CancellationException 안전)
- **RateLimiter**: 재시도 버튼 2초 쿨다운 (`AtomicLong.compareAndSet`)

### DetailViewModel 패턴
- `Mutex.tryLock()`: 반드시 `viewModelScope.launch {}` 내부에서 호출 — 스코프 외부 호출 시 취소 시 영구 잠금 위험
- `toggleMutex.withLock()`: FAB 즐겨찾기/워치리스트 연타 방지; `isInWatchlist(movieId).first()` 사용 (`.value` stale read 방지)
- `loadOptional`/`loadOptionalNullable`: 출연진/추천 등 부분 실패 graceful degradation
- **점진적 로딩**: 핵심 데이터(상세+등급) 먼저 `DetailUiState.Success` emit → 이후 출연진/비슷한 영화/리뷰/예고편/추천 각각 완료 시 `_uiState.update { it.copy(...) }` — `DetailUiState.Success`의 secondary 필드는 nullable (null = 로딩 중)
- `saveWatchHistory`: 별도 `coroutineScope` fire-and-forget (UI 상태 영향 없음)
- `launchWithErrorHandler`: 공통 에러 처리 (`core/util/CoroutineExt.kt`)
- **MemoDelegate** / **UserRatingDelegate**: 생성자 의존성 19개 → 13개 (`snackbarChannel` 공유)
- `Channel.CONFLATED` + `receiveAsFlow()`: Snackbar 일회성 이벤트 (모든 ViewModel 통일)

### FavoriteViewModel / FavoriteFragment 패턴
- `FavoriteViewModel`: `SavedStateHandle`로 선택 탭(`_selectedTab`) 프로세스 종료 후 복원 — `savedStateHandle["selectedTab"] = tab.name`
- `StatsViewModel` 포함 전체 stats/favorite StateFlow: `SharingStarted.Lazily` 사용 (재진입 시 Loading 플래시 방지; `WhileSubscribed`는 백스택 복귀 시 재구독 트리거)
- **탭 태그 패턴**: `TabLayout` 탭에 `tab.tag = HomeTab/FavoriteTab` 설정 → `onTabSelected`에서 `tab.tag as HomeTab` 캐스팅 (enum `entries[position]` 인덱스 의존 제거)
- **FavoriteFragment retry loop**: `Channel<Unit>` + `while(true)` 패턴으로 collect 재시작 — `CancellationException`은 반드시 rethrow
- **resume 스크롤 방지**: `initialEmit` 플래그로 StateFlow 첫 emit(resume 재방출) 시 `scrollToPosition(0)` 스킵

### StatsFragment 공유 이미지
- `createStatsCardBitmap()` 내 Canvas 크기/패딩/텍스트는 px 하드코딩 금지 — `resources.displayMetrics.density` 곱해 dp→px 변환

### 기타 주의사항
- **POST_NOTIFICATIONS 권한**: Android 13+ (TIRAMISU) 런타임 권한 필요 — `MainActivity.onCreate()`에서 `ActivityCompat.requestPermissions()` 호출 (시스템이 자동 팝업 불가)
- **PersonDetailFragment 로딩**: `layout_shimmer_person_detail.xml` Shimmer 레이아웃 `<include>` — ViewBinding에서 `binding.shimmerView.shimmerLayout`으로 접근 (중첩 바인딩 생성)
- **Widget 에러 상태**: `loadFailed` 플래그로 제어 — 에러 시 `getCount()=1`, `getViewAt()`에서 `R.string.widget_empty` 오류 행 반환
- **FavoriteSortOrder 문자열 매핑**: 도메인 모델 순수성 유지 — `FavoriteFragment.kt` 파일 레벨 private 확장함수 `labelRes()` / `subtitleRes()`로 `@StringRes` 변환 (Domain 레이어에 Android 의존성 미추가)
- **Widget**: OkHttp 싱글턴 + kotlinx.serialization 직접 호출, `response.use {}` 패턴
- **WatchGoalNotificationHelper**: `Mutex.withLock()` 원자적 달성 체크, `lastGoalNotifiedMonth`로 월 1회 제한
- **BackupRepository**: `UserDataBackup` @Serializable — favorites/watchlist/ratings/memos/tags/watchHistory 6종; `WatchHistoryDao.getAllHistoryOnce()` 백업용 일회성 쿼리; `restoreWatchHistory()`에서 `watchedAt.toYearMonth()` (kotlinx-datetime) 재계산
- **StrictMode**: `src/debug/java/` 소스셋 분리 — release 빌드에 클래스 자체 미포함, debug manifest에 meta-data 등록
- **디버그 도구**: DebugHealthCheck/FileLoggingTree/AnrWatchdog — `BuildConfig.DEBUG` 가드; `DebugEventListener` — `src/debug/` 소스셋 분리 (릴리스 바이너리에 로깅 코드 미포함, `src/release/`에 no-op 존재); `StrictModeInitializer` — `src/debug/` 소스셋 분리 (release 빌드에 클래스 자체 미포함, lintVitalRelease 통과)
- **R8 Full Mode**: `android.enableR8.fullMode=true` — serialization 클래스 ProGuard keep 필수
- **Predictive Back**: `android:enableOnBackInvokedCallback="true"` (Android 14+)
- **Edge-to-Edge**: `enableEdgeToEdge()` (Android 15 필수)
- **buildSrc**: `AndroidConfig` 객체로 compileSdk/minSdk/targetSdk/VERSION 공유
- **Per-App Language**: `locales_config.xml` + `AppCompatDelegate.setApplicationLocales()`
- **Baseline Profiles**: `:baselineprofile` 모듈 (minSdk 28), 플러그인 `1.5.0-alpha02` (AGP 9 호환)
