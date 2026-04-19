# MovieFinder - 영화 검색 안드로이드 앱

## 프로젝트 개요
TMDB (The Movie Database) API를 활용한 영화 검색, 상세 정보 조회, 즐겨찾기 기능을 갖춘 안드로이드 앱입니다.

## 기술 스택

### 빌드 환경
- **AGP**: 9.1.0 (Android Gradle Plugin, Kotlin 2.2.10 내장)
- **Gradle**: 9.3.1
- **compileSdk**: 36 (Android 15)
- **minSdk**: 24 / **targetSdk**: 36
- **KSP**: 2.3.2 (Kotlin Symbol Processing)

### 핵심 라이브러리
| 라이브러리 | 버전 | 용도 |
|---|---|---|
| AppCompat | 1.7.1 | Android 하위 호환성 |
| Material Components | 1.13.0 | Material Design XML 컴포넌트 |
| Navigation (Fragment) | 2.9.7 | Safe Args 기반 Fragment 네비게이션 |
| Fragment KTX | 1.8.9 | Fragment 확장 함수 |
| ConstraintLayout | 2.2.1 | 유연한 레이아웃 |
| RecyclerView | 1.4.0 | 리스트/그리드 표시 |
| Hilt | 2.59.2 | 의존성 주입 (DI) |
| Room | 2.8.4 | 로컬 SQLite 데이터베이스 |
| Room Paging | 2.8.4 | Room + Paging 3 통합 (RemoteMediator) |
| Paging 3 | 3.4.2 | 무한 스크롤 페이징 |
| Retrofit 3 | 3.0.0 | REST API 통신 |
| OkHttp 5 | 5.3.2 | HTTP 클라이언트 + 로깅 + 응답 캐시 (kotlin.time.Duration API) |
| kotlinx.serialization | 1.10.0 | JSON 직렬화/역직렬화 |
| Coil 3 | 3.4.0 | 이미지 로딩 (View 버전, 메모리+디스크 캐시) |
| Facebook Shimmer | 0.5.0 | 로딩 Shimmer 애니메이션 |
| Lifecycle | 2.10.0 | 생명주기 인식 컴포넌트 |
| Splash Screen API | 1.2.0 | 스플래시 화면 |
| SwipeRefreshLayout | 1.2.0 | 당겨서 새로고침 |
| DataStore (Typed) | 1.2.1 | kotlinx.serialization JSON 기반 타입 안전 설정 저장 |
| Lifecycle Process | 2.10.0 | ProcessLifecycleOwner (앱 수준 생명주기) |
| ProfileInstaller | 1.4.1 | Baseline Profiles 설치 |
| Benchmark Macro | 1.5.0-alpha05 | Baseline Profile 생성 |
| Timber | 5.0.1 | 구조화 로깅 |
| App Startup | 1.2.0 | 초기화 최적화 (Timber 등) |
| WorkManager | 2.11.2 | 개봉일 알림 스케줄링 |
| kotlinx-datetime | 0.7.1 | 멀티플랫폼 날짜/시간 API |
| Window | 1.3.0 | 윈도우 레이아웃 API |

### 개발/디버그 도구
| 도구 | 버전 | 용도 |
|---|---|---|
| Detekt | 2.0.0-alpha.2 | Kotlin 정적 분석 + KtLint 규칙 |
| JaCoCo | (Gradle 내장) | 테스트 코드 커버리지 리포트 (최소 50% 기준) |
| LeakCanary | 2.14 | 메모리 누수 감지 (debugImplementation) |

### 테스트 라이브러리
| 라이브러리 | 버전 | 용도 |
|---|---|---|
| MockK | 1.14.9 | Kotlin 모킹 프레임워크 |
| Turbine | 1.2.1 | Flow 테스트 유틸리티 |
| Coroutines Test | 1.10.2 | 코루틴 테스트 디스패처 |
| Paging Testing | 3.4.0 | PagingData 테스트 유틸리티 |
| JUnit 4 | 4.13.2 | 유닛 테스트 프레임워크 |
| Espresso | 3.7.0 | UI 테스트 프레임워크 (core + contrib) |
| Hilt Testing | 2.59.2 | Hilt 의존성 주입 테스트 지원 |

## 아키텍처

### Clean Architecture + MVVM
```
app/src/main/java/com/choo/moviefinder/
├── core/                  # 공유 유틸리티
│   ├── startup/
│   │   ├── TimberInitializer.kt   # App Startup Timber 초기화
│   │   └── StrictModeInitializer.kt # App Startup StrictMode (src/debug 소스셋 — release 빌드에 클래스 미포함)
│   ├── notification/
│   │   ├── ReleaseNotificationScheduler.kt # 개봉일 알림 스케줄링 (WorkManager)
│   │   ├── ReleaseNotificationWorker.kt    # 개봉일 알림 Worker
│   │   └── WatchGoalNotificationHelper.kt  # 시청 목표 달성 알림 (Mutex 중복 방지)
│   └── util/
│       ├── DateUtils.kt           # currentMonthStartMillis(), currentYearMonth()
│       ├── ImageUrlProvider.kt    # 이미지 URL 빌더 (posterUrl, backdropUrl, profileUrl)
│       ├── ErrorMessageProvider.kt # 예외 → ErrorType 매핑 + 사용자 메시지 변환
│       ├── NetworkMonitor.kt      # 실시간 네트워크 상태 모니터링 (StateFlow)
│       ├── CoroutineExt.kt        # launchWithErrorHandler 공통 에러 처리 헬퍼
│       ├── ExponentialBackoff.kt  # 재시도 간격 점진 증가 (1s→2s→4s, 최대 3회)
│       ├── RateLimiter.kt         # 재시도 버튼 2초 쿨다운
│       ├── DebugHealthCheck.kt    # 앱 시작 시 API/이미지/DB 헬스체크 (디버그 전용)
│       ├── DebugEventListener.kt  # OkHttp SSL/연결 실패 로깅 (src/debug 소스셋, src/release는 no-op)
│       ├── FileLoggingTree.kt     # Timber 파일 로깅 2MB 로테이션 (디버그 전용)
│       └── AnrWatchdog.kt         # 메인 스레드 5초+ 블로킹 감지 (디버그 전용)
├── baselineprofile/       # Baseline Profile 생성 모듈
├── data/                  # 데이터 레이어
│   ├── local/
│   │   ├── MovieDatabase.kt       # Room DB (version 14)
│   │   ├── dao/                   # DAO 8개 (FavoriteMovieDao, WatchlistDao: abstract class)
│   │   └── entity/                # Room Entity 8개
│   ├── paging/
│   │   ├── MoviePagingSource.kt       # 네트워크 전용 PagingSource (검색용)
│   │   ├── DiscoverPagingSource.kt    # 장르/정렬 기반 탐색
│   │   ├── TrendingPagingSource.kt    # 일별 트렌딩
│   │   └── MovieRemoteMediator.kt     # 오프라인 지원 (홈 화면, 1시간 캐시 만료)
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
│   └── usecase/           # UseCase 48개
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

## RecyclerView 어댑터
| 어댑터 | 부모 클래스 | 용도 |
|---|---|---|
| `MoviePagingAdapter` | `PagingDataAdapter` | 홈/검색 (무한 스크롤, ViewMode.GRID/LIST) |
| `MovieAdapter` | `ListAdapter` | 즐겨찾기/워치리스트 목록 |
| `CastAdapter` | `ListAdapter` | 상세 출연진 (onCastClick 콜백 → PersonDetailFragment) |
| `HorizontalMovieAdapter` | `ListAdapter` | 가로 스크롤 (비슷한 영화, 추천 영화, 시청 기록) |
| `ReviewAdapter` | `ListAdapter` | 상세 리뷰 (클릭 시 확장/축소) |
| `RecentSearchAdapter` | `ListAdapter` | 검색 최근 검색어 |
| `MemoAdapter` | `ListAdapter` | 상세 메모 목록 (수정/삭제, Undo) |
| `MovieLoadStateAdapter` | `LoadStateAdapter` | 페이징 로딩/에러 footer |

- 모든 이미지 어댑터: `onViewRecycled()` → Coil `dispose()`

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

## 오프라인 지원 (RemoteMediator 패턴)

- 홈 (현재 상영작/인기 영화): Room DB를 Single Source of Truth
- **온라인**: API → RemoteMediator → Room 캐시 → Room PagingSource → UI
- **오프라인**: Room 캐시 즉시 표시 (1시간 캐시 만료: `RemoteKeyEntity.lastUpdated`)
- 검색: 네트워크 전용 (MoviePagingSource)

## 테스트

### 유닛 테스트 (453개)
```bash
./gradlew testDebugUnitTest
```

| 테스트 클래스 | 수 | 대상 |
|---|---|---|
| `SearchViewModelTest` | 27 | 검색, 필터, SavedStateHandle 복원, 뷰모드 토글, Discover 모드 |
| `DetailViewModelTest` | 24 | 초기 상태, 에러, 부분 실패, FAB 토글, Snackbar, 재시도, 중복 방지 |
| `MovieUseCasesTest` | 23 | 영화 관련 UseCase 위임 검증 |
| `PreferencesTagUseCasesTest` | 21 | 테마/시청목표 UseCase + 태그 UseCase 위임 검증 |
| `MemoRatingPersonUseCasesTest` | 20 | 메모/평점/배우 UseCase 위임 검증 |
| `MovieRepositoryImplTest` | 25 | 영화 API 호출 (상세, 출연진, 비슷한 영화, 예고편, 리뷰 등) + 페이징 메서드 스모크 테스트 |
| `SettingsViewModelTest` | 18 | 테마, 시청기록 삭제, 시청 목표 |
| `FavoriteViewModelTest` | 16 | 즐겨찾기/워치리스트 목록·토글·정렬, 태그 추가/삭제 |
| `SearchWatchHistoryUseCasesTest` | 15 | 검색기록/시청기록 UseCase 위임 검증 |
| `TagRepositoryImplTest` | 13 | 태그 Repository 구현체 |
| `FavoriteWatchlistUseCasesTest` | 12 | 즐겨찾기/워치리스트 UseCase 위임 검증 |
| `GetWatchStatsUseCaseTest` | 10 | combine 로직, 장르 빈도, 월별/일별 카운트, 평점 분포 |
| `ErrorMessageProviderTest` | 27 | 예외 타입별 ErrorType 매핑 + getMessage() Context 위임 검증 |
| `PreferencesRepositoryImplTest` | 9 | 테마, 시청 목표, 알림 월 |
| `DelegateTest` | 8 | MemoDelegate + UserRatingDelegate 로직 |
| `StatsViewModelTest` | 7 | Loading/Success/Error, 목표 달성률 |
| `SplitRepositoryImplTest` | 35 | 분리된 Repository 구현체 (Favorite, Watchlist, SearchHistory 등, MemoRepository 포함, getFavoriteMoviesSorted 3분기 검증) |
| `MovieRemoteMediatorTest` | 10 | REFRESH/APPEND/SKIP, 캐시 만료, 오프라인 REFRESH, POPULAR 카테고리, 빈 응답 |
| `MoviePagingSourceTest` | 7 | 페이지 로드, 에러, nextKey/prevKey |
| `CachedMovieEntityMapperTest` | 7 | Entity ↔ 도메인 모델 변환 |
| `WatchGoalNotificationHelperTest` | 7 | 달성/미달/중복 알림 방지 |
| `ReleaseNotificationSchedulerTest` | 7 | WorkManager 스케줄/취소, 과거날짜/API 레벨 가드 |
| `PersonDetailViewModelTest` | 6 | 배우 상세 상태, 병렬 호출 |
| `HomeViewModelTest` | 8 | UseCase 호출, 시청기록 |
| `ExponentialBackoffTest` | 10 | 첫 성공, 재시도 후 성공, 전체 실패, CancellationException 처리, require 가드 (maxRetries/initialDelayMs/factor 유효성), maxDelayMs 상한 검증 |
| `TrendingPagingSourceTest` | 8 | 트렌딩 페이징, getRefreshKey |
| `DiscoverPagingSourceTest` | 8 | Discover 페이징, getRefreshKey |
| `BackupRepositoryImplTest` | 9 | exportUserData 매핑, importUserData insertAll 호출·타임스탬프 |
| `WatchHistoryRepositoryImplTest` | 7 | saveWatchHistoryWithGenres @Transaction 원자적 저장 에지 케이스, clearWatchHistory @Transaction 원자적 삭제 |
| `RateLimiterTest` | 5 | 2초 쿨다운, 동시 호출 차단 |
| `ExportImportUseCaseTest` | 4 | 내보내기/가져오기 검증 |
| `PersonRepositoryImplTest` | 10 | getPersonDetail, getPersonMovieCredits, searchPerson + 유효성 검사 |
| `NetworkMonitorTest` | 8 | 초기 상태, 콜백, SecurityException 처리 |
| `DateUtilsTest` | 7 | currentMonthStartMillis, currentYearMonth (고정 Clock) |
| `ImageUrlProviderTest` | 6 | posterUrl, backdropUrl, profileUrl URL 생성 |
| `CoroutineExtTest` | 7 | launchWithErrorHandler 성공/에러/CancellationException, suspendRunCatching 성공/에러/CancellationException 재전파 |
| `ReleaseNotificationWorkerTest` | 3 | 유효하지 않은 입력 실패 경로 |

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

## 주의사항

### AGP 9.x 특이사항
- `kotlinOptions` 블록 사용 불가 (Kotlin이 AGP에 내장)
- `android.disallowKotlinSourceSets=false` 필요 (KSP 호환)
- `Theme.MaterialComponents.DayNight.NoActionBar` 사용
- Hilt 2.59.1 이상 필요

### Navigation
- `nav_graph.xml`에 destination + argument + deepLink 정의
- `FragmentDirections`로 타입 안전 action, `navArgs()`로 Fragment 수신
- ViewModel에서: `savedStateHandle.get<Int>("movieId")`
- `NavigationUI.setupWithNavController()` + Detail에서 BottomNav 자동 숨김
- Detail self-navigation: `launchSingleTop="true"` (백스택 중복 방지)
- 딥링크: `moviefinder://movie/{id}`, `moviefinder://person/{id}`, `moviefinder://stats`

### Room DB
- 버전: 14, 이름: `movie_finder_db`, 스키마: `app/schemas/`
- Entity 8개: FavoriteMovieEntity, RecentSearchEntity, CachedMovieEntity, RemoteKeyEntity, WatchHistoryEntity, WatchlistEntity, UserRatingEntity, MemoEntity
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
- `NetworkModule.kt`의 `CertificatePinner` 설정
- 인증서 갱신 시 핀 업데이트:
  ```bash
  echo | openssl s_client -connect api.themoviedb.org:443 2>/dev/null | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
  ```
- 위젯 OkHttp 싱글턴에도 CertificatePinner 적용 (Hilt 미사용)

### 네트워크 복원력
- **ExponentialBackoff**: `withExponentialBackoff()` (1s→2s→4s, CancellationException 안전)
- **RateLimiter**: 재시도 버튼 2초 쿨다운 (`AtomicLong.compareAndSet`)

### DetailViewModel 패턴
- `Mutex.tryLock()`: 중복 API 호출 차단, `finally`에서 unlock (에러 후 재시도 가능)
- `toggleMutex.withLock()`: FAB 즐겨찾기/워치리스트 연타 방지
- `loadOptional`/`loadOptionalNullable`: 출연진/추천 등 부분 실패 graceful degradation
- **점진적 로딩**: 핵심 데이터(상세+등급) 먼저 `DetailUiState.Success` emit → 이후 출연진/비슷한 영화/리뷰/예고편/추천 각각 완료 시 `_uiState.update { it.copy(...) }` — `DetailUiState.Success`의 secondary 필드는 nullable (null = 로딩 중)
- `saveWatchHistory`: 별도 `coroutineScope` fire-and-forget (UI 상태 영향 없음)
- `launchWithErrorHandler`: 공통 에러 처리 (`core/util/CoroutineExt.kt`)
- **MemoDelegate** / **UserRatingDelegate**: 생성자 의존성 19개 → 13개 (`snackbarChannel` 공유)
- `Channel.CONFLATED` + `receiveAsFlow()`: Snackbar 일회성 이벤트 (모든 ViewModel 통일)

### 기타 주의사항
- **POST_NOTIFICATIONS 권한**: Android 13+ (TIRAMISU) 런타임 권한 필요 — `MainActivity.onCreate()`에서 `ActivityCompat.requestPermissions()` 호출 (시스템이 자동 팝업 불가)
- **PersonDetailFragment 로딩**: `layout_shimmer_person_detail.xml` Shimmer 레이아웃 `<include>` — ViewBinding에서 `binding.shimmerView.shimmerLayout`으로 접근 (중첩 바인딩 생성)
- **Widget 에러 상태**: `loadFailed` 플래그로 제어 — 에러 시 `getCount()=1`, `getViewAt()`에서 `R.string.widget_empty` 오류 행 반환
- **FavoriteSortOrder 문자열 매핑**: 도메인 모델 순수성 유지 — `FavoriteFragment.kt` 파일 레벨 private 확장함수 `labelRes()` / `subtitleRes()`로 `@StringRes` 변환 (Domain 레이어에 Android 의존성 미추가)
- **Widget**: OkHttp 싱글턴 + kotlinx.serialization 직접 호출, `response.use {}` 패턴
- **WatchGoalNotificationHelper**: `Mutex.withLock()` 원자적 달성 체크, `lastGoalNotifiedMonth`로 월 1회 제한
- **StrictMode**: `src/debug/java/` 소스셋 분리 — release 빌드에 클래스 자체 미포함, debug manifest에 meta-data 등록
- **디버그 도구**: DebugHealthCheck/FileLoggingTree/AnrWatchdog — `BuildConfig.DEBUG` 가드; `DebugEventListener` — `src/debug/` 소스셋 분리 (릴리스 바이너리에 로깅 코드 미포함, `src/release/`에 no-op 존재); `StrictModeInitializer` — `src/debug/` 소스셋 분리 (release 빌드에 클래스 자체 미포함, lintVitalRelease 통과)
- **R8 Full Mode**: `android.enableR8.fullMode=true` — serialization 클래스 ProGuard keep 필수
- **Predictive Back**: `android:enableOnBackInvokedCallback="true"` (Android 14+)
- **Edge-to-Edge**: `enableEdgeToEdge()` (Android 15 필수)
- **buildSrc**: `AndroidConfig` 객체로 compileSdk/minSdk/targetSdk/VERSION 공유
- **Per-App Language**: `locales_config.xml` + `AppCompatDelegate.setApplicationLocales()`
- **Baseline Profiles**: `:baselineprofile` 모듈 (minSdk 28), 플러그인 `1.5.0-alpha02` (AGP 9 호환)
