# MovieFinder - 영화 검색 안드로이드 앱

## 프로젝트 개요
TMDB (The Movie Database) API를 활용한 영화 검색, 상세 정보 조회, 즐겨찾기 기능을 갖춘 안드로이드 앱입니다.

## 기술 스택

### 빌드 환경
- **AGP**: 9.4.1 (Android Gradle Plugin, Kotlin 2.3.21 내장)
- **Gradle**: 9.7.1
- **compileSdk**: 37 (Android 17)
- **minSdk**: 24 / **targetSdk**: 36
- **KSP**: 2.3.12 (Kotlin Symbol Processing)

### 핵심 라이브러리
| 라이브러리 | 용도 |
|---|---|
| AppCompat, Material Components, Fragment KTX, ConstraintLayout, RecyclerView, Lifecycle(+Process), Splash Screen API, SwipeRefreshLayout, Facebook Shimmer, Timber, App Startup, kotlinx-datetime, Window, ProfileInstaller, Benchmark Macro | 기본 UI/생명주기/유틸 — 버전은 `gradle/libs.versions.toml` |
| Navigation (Fragment) | Safe Args 기반 Fragment 네비게이션 |
| Hilt | 의존성 주입 (DI) |
| Room + Room Paging + Paging 3 | 로컬 SQLite, RemoteMediator, 무한 스크롤 페이징 |
| Retrofit 3 + OkHttp 5 + kotlinx.serialization | REST 통신, 로깅 + 응답 캐시 (kotlin.time.Duration API), JSON 직렬화 |
| Coil 3 | 이미지 로딩 (View 버전, 메모리+디스크 캐시) |
| DataStore (Typed) | kotlinx.serialization JSON 기반 타입 안전 설정 저장 |
| WorkManager | 개봉일 알림 스케줄링 |
| android-youtube-player | YouTube 예고편 외부 연결 (임베드 제한으로 인해 앱 외부 실행) |
| Jetpack Compose (부분 도입) | 검색 결과·온보딩 두 곳뿐, 나머지는 XML/ViewBinding — 아래 "Compose 부분 도입 & Glance 위젯" 참고 |
| Jetpack Glance | `BoxOfficeWidget`(2x2) — `PopularMoviesWidget`(RemoteViews)과 위젯 2종이 서로 다른 기술로 공존 |

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
│   ├── startup/           # TimberInitializer, StrictModeInitializer
│   ├── notification/      # ReleaseNotificationScheduler/Worker, WatchGoalNotificationHelper
│   └── util/              # DateUtils, ImageUrlProvider, ErrorMessageProvider, NetworkMonitor, CoroutineExt,
│                          # ExponentialBackoff, RateLimiter, DebugHealthCheck/DebugEventListener/FileLoggingTree/AnrWatchdog
├── baselineprofile/       # Baseline Profile 생성 모듈
├── data/                  # 데이터 레이어
│   ├── local/
│   │   ├── MovieDatabase.kt       # Room DB (version 24)
│   │   ├── dao/                   # DAO 15개 (FavoriteMovieDao, WatchlistDao: abstract class)
│   │   └── entity/                # Room Entity 14개
│   ├── paging/
│   │   ├── MoviePagingSource.kt
│   │   ├── DiscoverPagingSource.kt
│   │   ├── TrendingPagingSource.kt
│   │   └── MovieRemoteMediator.kt
│   ├── remote/            # Retrofit API (Service, DTO)
│   ├── repository/        # Repository 구현체 (17개, ISP 기반 도메인별 분리)
│   └── util/              # 상수 (PAGE_SIZE, DEFAULT_PAGING_CONFIG 등)
├── di/                    # Hilt DI 모듈
│   ├── DatabaseModule.kt  # Room DB + DAO (destructive migration fallback)
│   ├── DataStoreModule.kt # Typed DataStore<UserSettings>
│   ├── NetworkModule.kt   # Retrofit/OkHttp (API key interceptor, CertificatePinner, Cache 10MB, @ImageOkHttpClient)
│   └── RepositoryModule.kt # 17개 도메인 @Binds + Preferences
├── domain/                # 도메인 레이어 (순수 Kotlin)
│   ├── model/             # 도메인 모델
│   ├── repository/        # Repository 인터페이스 21개
│   └── usecase/           # UseCase 78개
├── presentation/          # 프레젠테이션 레이어
│   ├── adapter/           # RecyclerView 어댑터 10개 + MovieGridViewHolder + MovieListViewHolder
│   ├── common/            # CircularRatingView, PieChartView, BarChartView, HistogramView, CalendarHeatmapView
│   ├── detail/            # DetailFragment, DetailViewModel, MemoDelegate, UserRatingDelegate
│   ├── favorite/          # FavoriteFragment, FavoriteViewModel (탭, 정렬, 태그 필터)
│   ├── home/              # HomeFragment, HomeViewModel (3탭, RemoteMediator)
│   ├── onboarding/        # OnboardingFragment(전체 Compose, ComposeView) + OnboardingScreen/OnboardingPage
│   ├── person/            # PersonDetailFragment, PersonDetailViewModel
│   ├── search/            # SearchFragment(XML+ComposeView 2곳), SearchViewModel (필터, SavedStateHandle), SearchComposables.kt, SearchResultsPane.kt (결과 영역 판정), RecentSearchesPane.kt (최근 검색어 영역 판정)
│   ├── settings/          # SettingsFragment, SettingsViewModel (테마/캐시/목표/백업/TMDB 계정 연동)
│   ├── stats/             # StatsFragment, StatsViewModel (통계 카드 9개)
│   └── widget/            # PopularMoviesWidget(RemoteViewsService/Factory) + BoxOfficeWidget(Glance, 2x2) — 위젯 2종이 서로 다른 기술 스택
├── MainActivity.kt        # NavHostFragment + 딥링크 처리 + 네트워크 Snackbar
└── MovieFinderApp.kt      # @HiltAndroidApp, Coil 캐시, CertificatePinner, NotificationChannel 2개
```

### 레이어 규칙
- **Core**: 공유 유틸리티, 모든 레이어에서 접근 가능
- **Domain**: 순수 Kotlin, Android 프레임워크 의존성 없음 (Paging 예외)
- **Repository 인터페이스**: ISP 기반 20개 도메인별 분리 + Preferences
- **Data**: API 통신, DB 접근, Repository 구현
- **Presentation**: Domain UseCase + Core에만 의존, Data 레이어 직접 참조 금지

### 데이터 흐름
```
API/DB → Repository → UseCase → ViewModel → Fragment (XML UI, 일부 Compose)
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

### KOFIC 박스오피스 API
`local.properties`에 `KOFIC_API_KEY` 추가 필요 (영화진흥위원회 Open API, TMDB와 별개 발급).

| 엔드포인트 | 용도 |
|---|---|
| GET boxoffice/searchDailyBoxOfficeList.json | 일별 박스오피스 TOP 10 (`targetDt`, `itemPerPage`) |
| GET boxoffice/searchWeeklyBoxOfficeList.json | 주간 박스오피스 TOP 10 (`targetDt`, `weekGb`(0=주간/1=주말/2=주중), `itemPerPage`) — 개별 아이템 필드는 일별과 100% 동일, `KoficBoxOfficeItemDto` 공용 |

- TMDB와 완전히 분리된 `@KoficOkHttpClient`/`@KoficRetrofit`/`KoficApiService` (`NetworkModule.kt`) — 별도 baseUrl(`BuildConfig.KOFIC_BASE_URL`), 인증 키를 헤더가 아닌 쿼리 파라미터(`key`)로 주입, TMDB `CertificatePinner` 미적용
- `MatchBoxOfficeWithTmdbUseCase`: KOFIC `movieName`을 TMDB와 제목 문자열 매칭(ID 체계가 달라 직접 연결 불가) — 항목마다 TMDB 검색을 날리면 N+1이므로 로컬 캐시(`MovieQueryRepository.getCachedMoviesSnapshot()`, now_playing+popular 스냅샷)를 요청당 1회만 조회해 우선 매칭하고 캐시 미스만 `searchMoviesOnce()` 네트워크 폴백(TOP 50 기준 약 1.6s → 0.3s, `MatchBoxOfficeWithTmdbPerformanceTest`). `GetDailyBoxOfficeWithTmdbMatchUseCase`/`GetWeeklyBoxOfficeWithTmdbMatchUseCase`가 이 매처를 공유
- 홈 화면 박스오피스 섹션은 ChipGroup(일별/주간)으로 기간 전환, 선택은 `SavedStateHandle`에 보존, 전환 시 이전 `Job` 취소로 늦게 도착한 응답이 최신 상태를 덮지 않음 (`HomeViewModel.loadBoxOffice()`)

## 테스트

### 유닛 테스트 (987개)
```bash
./gradlew testDebugUnitTest
```
주요 커버: ViewModel 5종, Repository 9종, UseCase 5종, PagingSource 3종, 유틸/워커 다수.

### Espresso UI 테스트 (29개)
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
- Pre-push Hook: 유닛 테스트 전체 + `:app:lintDebug` (`.githooks/pre-push`) — Lint가 CI에서만 돌면 push 후에야 실패가 드러나 push 전 검사로 이동(2026-09-29, NewApi로 main CI 연속 실패)
- Hooks 활성화: `git config core.hooksPath .githooks` — **클론마다 필요하고 조용히 풀릴 수 있다**(2026-09-29에 `.git/hooks`로 잡혀 훅이 전혀 안 돌던 것을 발견). 훅이 돈다고 가정하지 말고 `git config core.hooksPath`로 확인할 것
- `Claude Code Review`(claude-code-review.yml)는 Dependabot PR 제외 — `allowed_bots` 미설정이라 봇이 시작한 워크플로우가 항상 실패했고, Dependabot PR은 `claude-pr-review.yml`이 이미 리뷰함
- GitHub Secrets: `TMDB_API_KEY` 필요 — `KOFIC_API_KEY`는 CI에 미주입(로컬 전용); 없어도 빌드/유닛 테스트는
  통과하지만 실기기 박스오피스 API는 인증 오류 (`ONBOARDING.md` 참고)
- Dependabot: 라이브러리 자동 버전 업데이트

## Claude Code 훅 / 서브에이전트 자동화

> `.claude/`는 `.gitignore`로 전체 무시됨 — 아래 내용은 이 저장소를 사용하는 각자의 로컬 환경에만
> 존재하며 git으로 공유되지 않는다. 팀원이 동일하게 쓰려면 각자 셋업 필요.

### kotlin-architecture-reviewer 서브에이전트
- 위치: `.claude/agents/kotlin-architecture-reviewer.md` (model: opus, tools: Read/Grep/Glob/Bash)
- 스코프: 코루틴/Flow 안전성(체크리스트 B) + Clean Architecture 레이어 규칙(A) + Paging/RemoteMediator(C) + UI 상태 설계(D)만. 스타일/네이밍/OWASP 보안/접근성/테스트 커버리지는 스코프 밖(관련 스킬의 몫).
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
- **비-차단 설계**: `permissionDecision`은 항상 `"allow"` — 어떤 경우에도 편집을 막지 않는다. 이 리뷰는 opus의 판단(오판 가능)이고 2분 이상 걸릴 수 있어 매 Edit마다 차단하면 개발 흐름이 망가진다. 결정론적·항상-참 규칙(`domain-android-import-guard.sh`의 android/androidx import grep 등)만 차단(`exit 2`)하고, 판단이 필요한 리뷰는 정보 제공(`allow` + `additionalContext`)으로 남긴다.
- Bash 권한은 `--allowedTools "Read,Grep,Glob,Bash(./gradlew :app:compileDebugKotlin:*)"`로 제한 —
  프롬프트 규칙이 깨져도 툴 레이어에서 이중으로 막도록 함.
- **알려진 제약**: 훅 *설정*(`settings.json`)은 세션 시작 시에만 로드되어 수정하면 Claude Code를 재시작해야
  반영된다(스크립트 파일 자체는 매 호출마다 새로 실행되므로 수정이 재시작 없이 바로 반영됨 — 2026-09-30 확인).
  macOS 기본 환경엔 GNU `timeout`이 없어 순수 bash(`sleep N && kill`)로 타임아웃을 구현함.
- **워처 stdio 누수(2026-09-30 수정)**: 타임아웃 워처 `( sleep 240; kill ... ) &`가 훅 stdout(하네스 파이프)을 물려받고 `kill $watcher_pid`는 서브셸만 죽여, 고아 `sleep 240`이 파이프를 최대 240초 붙잡아 하네스가 `stdio went quiet before end-of-stream`으로 훅 출력을 거부했다. 워처에 `>/dev/null 2>&1`을 붙이고 `stop_watcher`가 `pkill -P`로 자식까지 정리하도록 수정. 같은 패턴(백그라운드 서브셸 + 물려받은 stdout)은 다른 훅에도 주의할 것.

### Stop 테스트 페어링 강제 훅
- 위치: `.claude/hooks/stop-test-coverage-guard.sh` + `.claude/hooks/lib/check_test_pairing.py`,
  `.claude/settings.json`의 `Stop`(matcher: `*`)에 등록.
- 트리거 조건: 이번 세션 transcript에서 `domain/*.kt` 또는 `presentation/**/*ViewModel.kt` (`src/main`
  한정) 파일을 Edit/Write/MultiEdit 했는데, 대응 테스트 파일(`app/src/test/.../XxxTest.kt`)이 같은
  세션에서 한 번도 Edit/Write/MultiEdit 되지 않은 경우.
- **차단 설계**: exit 2로 세션 종료를 막는다 — PreToolUse 훅과 정반대. "같은 세션에서 테스트 파일이 건드려졌는가"는 transcript를 기계적으로 훑는 100% 결정론적 사실 확인이라 오탐이 없고, "결정론적 규칙만 차단" 원칙에 따르면 차단이 맞다.
- 무한 루프 방지: 훅 입력의 `stop_hook_active`가 true면(이미 한 번 이 훅에 막혔다가 재개했다는 뜻)
  다시 막지 않고 통과시킨다 — 한 번은 스스로 고치거나 사유를 설명할 기회를 주되 영원히 막지는 않는다.
- **알려진 제약**: 서브에이전트(Agent 도구 위임)의 Edit/Write는 자신의 별도 transcript에만 기록돼 메인 세션 transcript에 안 잡힌다 — 이 훅은 **메인 루프가 직접 수정한 파일만** 감지하고 구현자 서브에이전트가 만든 파일은 놓친다(feature-implementer 파이프라인 같은 서브에이전트 주도 워크플로우에서 감안할 것).

## 주의사항

### AGP 9.x 특이사항
- `kotlinOptions` 블록 사용 불가 (Kotlin이 AGP에 내장)
- `android.disallowKotlinSourceSets=false` 필요 (KSP 호환)
- `Theme.MaterialComponents.DayNight.NoActionBar` 사용
- Hilt 2.59.2 이상 필요 (2.59.2 기준 Kotlin 2.4.0 미지원 — kotlin-metadata-jvm max 2.3.0)
- **Dependabot 호환성**: core-ktx 1.19.0은 compileSdk 37 요구 → 37로 올라가 해소(현재 적용 중). Kotlin 2.4.x는 보류: 2.4.0은 Hilt 호환 이슈(위 참고), 2.4.10·2.4.20은 AGP/Lint의 `Can't initialize detector com.android.tools.lint.checks.InferredThreadDetector` 오류로 lintAnalyzeDebug 계열 3개 태스크가 전부 실패(PR #92·#110·#115, AGP 9.4.1에서도 재현 확인 2026-09-28). AGP 9.4.1이 최신 정식판(9.5.0은 알파뿐, 2026-10-05)이라 AGP 업그레이드로는 못 푼다 — 정식 신버전이 나오면 lintAnalyzeDebug 재실행으로 재검증

### Navigation
- `nav_graph.xml`에 destination + argument + deepLink 정의
- `FragmentDirections`로 타입 안전 action, `navArgs()`로 Fragment 수신
- ViewModel에서: `savedStateHandle.get<Int>("movieId")`
- `NavigationUI.setupWithNavController()` + Detail에서 BottomNav 자동 숨김
- Detail self-navigation: `launchSingleTop="true"` (백스택 중복 방지)
- 딥링크: `moviefinder://movie/{id}`, `moviefinder://person/{id}`, `moviefinder://stats`
- **Predictive Back Gesture**: `enableOnBackInvokedCallback="true"` + targetSdk 36 + Fragment 1.9.0/Navigation 2.9.8로 플러밍은 갖춰져 있음. 앱 종료(홈으로 나가기)는 OS 레벨이라 스크러빙 애니메이션이 자동으로 나오지만, **인앱 Fragment 전환은 `nav_graph.xml` 액션의 애니메이션 리소스가 `res/animator/`(Animator, seek 가능)여야만 스와이프 진행률에 맞춰 미리보기가 나온다** — `res/anim/`(레거시 View Animation)은 손을 뗄 때까지 변화 없다가 순간이동처럼 보임(2026-09-01 실기기 SM-S926N, 커스텀 `OnBackPressedCallback` 없이 이 차이만으로 재현). `slide_in_right`/`slide_out_right`/`fade_in`/`fade_out` 4개를 `res/animator/`로 전환 완료(translationX는 `@dimen/nav_transition_slide_distance` 480dp 고정 근사치). **DetailFragment 공유 요소 전환(플랫폼 `android.transition.TransitionSet`)은 레거시라 predictive back 미리보기가 여전히 안 됨** — androidx.transition 마이그레이션은 500ms `postponeEnterTransition` + Coil 리스너 흐름을 건드려 리스크 대비 보류.
- MaterialAlertDialogBuilder 다이얼로그도 predictive back 스와이프로 닫히긴 하나 진행률 미리보기는
  없음(실기기 검증, 원인 미조사) — 다이얼로그 predictive back 애니메이션이 필요하면 별도 조사 필요.

### Room DB
- 버전: 24, 이름: `movie_finder_db`, 스키마: `app/schemas/`
- Entity 14개: FavoriteMovieEntity, RecentSearchEntity, CachedMovieEntity, RemoteKeyEntity, WatchHistoryEntity, WatchlistEntity, UserRatingEntity, MemoEntity, ScheduledReminderEntity 외
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
- 29개 아이콘 drawable: `android:fillColor="@color/icon_default"`

### Shared Element Transition
- `FragmentNavigatorExtras(posterView to "poster_$movieId")`
- DetailFragment `onCreate()`: sharedElementEnterTransition + ReturnTransition 설정
- `postponeEnterTransition(500, TimeUnit.MILLISECONDS)` + Coil `listener(onSuccess, onError)`
- nav_graph.xml action에서 enterAnim/exitAnim 제거 (Shared Element와 충돌)

### Fragment ViewBinding 패턴
- `_binding` nullable + `binding` non-null getter
- `onDestroyView()`: `_binding = null` + Shimmer `stopShimmer()`

### Compose 부분 도입 & Glance 위젯
- Compose는 `OnboardingFragment`(Fragment 전체가 `ComposeView`)와 `SearchFragment`(XML 안에 `composeSearchInput`/`composeSearchResults` 2개 `ComposeView` 임베드) 두 곳뿐이고 나머지는 XML/ViewBinding — 새 화면을 Compose로 만들지는 케이스 바이 케이스이므로 임의로 전체 마이그레이션 시도 금지
- `BoxOfficeWidget`(Glance, 2x2)은 `PopularMoviesWidget`(RemoteViews)과 기술 스택이 다름 — `provideGlance()`는 재구성마다 호출될 수 있어 여기서 네트워크/DB I/O 금지. 데이터 획득은 `BoxOfficeWidgetWorker`가 맡고, `provideGlance()`는 `PreferencesGlanceStateDefinition`에 저장된 스냅샷을 읽어 그리기만 함

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
- `cert-pin-check.yml`은 OkHttp처럼 체인 중 하나라도 코드 핀과 일치하면 정상. `PIN_IMAGE_LEAF`는 LE 갱신마다 죽어서 중간 핀이 지탱

### 디버그 로그 키 마스킹 (2026-10-07, 실기기 SM-S926N 검증)
- 쿼리로 나가는 비밀값(`api_key`/`key`/`serviceKey`/`session_id`)은 `SecretQueryParams` 상수로만 이름을 쓴다 — 주입 인터셉터·`@Query`와 `addDebugLogging()`의 `redactQueryParams`가 같은 상수를 공유해야 마스킹이 조용히 풀리지 않는다. **새 쿼리 비밀값을 추가하면 `ALL`에도 넣을 것.**
- **인터셉터 순서를 바꿔도 안 막힌다**: `HttpLoggingInterceptor`의 응답 줄은 키가 주입된 최종 URL(`response.request.url`)을 찍어, 로깅을 키 주입 앞에 둔 TMDB v3도 응답 줄에서 노출됐다. `DebugEventListener.callFailed`는 원본 URL이라 주입 키는 없지만 `@Query`로 붙는 `session_id`는 있다(별도 `redactSecrets()`).
- 테스트 `NetworkLogRedactionTest`(실제 `build*OkHttpClient` + 로컬 HttpServer, `testDebugUnitTest` 전용 — release는 로깅이 no-op이라 `assumeTrue`로 건너뜀). release 로깅 부재는 `src/release` no-op + `debugImplementation`으로 유지.

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
- **PopularMoviesWidget 에러 상태**: `loadFailed` 플래그로 제어 — 에러 시 `getCount()=1`, `getViewAt()`에서 `R.string.widget_empty` 오류 행 반환
- **FavoriteSortOrder 문자열 매핑**: 도메인 모델 순수성 유지 — `FavoriteFragment.kt` 파일 레벨 private 확장함수 `labelRes()` / `subtitleRes()`로 `@StringRes` 변환 (Domain 레이어에 Android 의존성 미추가)
- **PopularMoviesWidget**: OkHttp 싱글턴 + kotlinx.serialization 직접 호출, `response.use {}` 패턴 (RemoteViews 방식 — Glance 기반 `BoxOfficeWidget`은 위 "Compose 부분 도입 & Glance 위젯" 절 참고)
- **WatchGoalNotificationHelper**: `Mutex.withLock()` 원자적 달성 체크, `lastGoalNotifiedMonth`로 월 1회 제한
- **BackupRepository**: `UserDataBackup` @Serializable — favorites/watchlist/ratings/memos/tags/watchHistory 6종; `WatchHistoryDao.getAllHistoryOnce()` 백업용 일회성 쿼리; `restoreWatchHistory()`에서 `watchedAt.toYearMonth()` (kotlinx-datetime) 재계산
- **디버그 도구**: DebugHealthCheck/FileLoggingTree/AnrWatchdog — `BuildConfig.DEBUG` 가드; `DebugEventListener`/`StrictModeInitializer` — `src/debug/java/` 소스셋 분리(release 바이너리에 클래스 미포함, debug manifest에 meta-data 등록, lintVitalRelease 통과; `DebugEventListener`는 `src/release/`에 no-op 존재)
- **R8 Full Mode**: `android.enableR8.fullMode=true` — serialization 클래스 ProGuard keep 필수
- **Predictive Back**: `android:enableOnBackInvokedCallback="true"` (Android 14+)
- **Edge-to-Edge**: `enableEdgeToEdge()` (Android 15 필수)
- **buildSrc**: `AndroidConfig` 객체로 compileSdk/minSdk/targetSdk/VERSION 공유
- **Per-App Language**: `locales_config.xml` + `AppCompatDelegate.setApplicationLocales()`
  - **알려진 한계(API 24~32)**: `AppLocalesMetadataHolderService`(`autoStoreLocales`)를 의도적으로 추가하지 않음 →
    API 33 미만에서는 프로세스 종료 시 언어 선택이 유지되지 않을 수 있음(API 36 실기기로만 검증, 24~32 미검증).
    추가하면 API 33+에서 이미 고른 언어가 업데이트 후 첫 실행에 `[]`로 초기화되는 회귀를 실기기(SM-S926N)에서
    재현·확인함(2026-09-21, 서비스 유무 통제 실험). 해결하려면 33+ 초기화를 피하는 방법(예: 저장소 값 사전 시딩)을 별도 검토.
  - **TMDB 언어 연동(2026-09-28, 실기기 SM-S926N 검증 완료)**: `AppLanguageProvider`(core/util)가 앱 언어를
    TMDB `language` 코드로 변환해 제공. 상세/배우/트렌딩/discover/개봉예정/홈(현재상영작·인기) 목록은 앱 언어를
    따르도록 전환 완료 — 영어 모드에서 제목/줄거리가 영어로 표시됨.
    - **검색 결과 데이터는 ko-KR 고정, 표시 제목만 앱 언어를 따름** (2026-09-29, 실기기 검증): KMRB 등급 필터(`FilterMoviesByKoreanRatingUseCase`)가
      `movie.title`로 매칭하므로 `MoviePagingSource`/discover는 ko-KR을 유지한다. 영어일 때만 **화면에 컴포즈된 카드의 제목**을
      `GetLocalizedTitleUseCase`로 지연 조회한다(`SearchComposables.kt`의 `rememberDisplayTitle`). 필터 판정은 목록 멤버십을 정해 전체 항목이 필요하므로
      "보이는 항목만"으로 지연할 수 없다(순환). 필터 안에서 제목을 추가 조회하던 방식은 60건 약 68초라 롤백. `GetKoreanTitleUseCase`는 상세 화면 KMRB 매칭 전용.
      한계: 표시 제목이 약 0.5초 뒤 교체, 포스터·매칭·정렬은 ko-KR 기준.
    - **KMRB 필터 자체의 한계(언어와 무관, 미해결)**: 항목당 순차 호출(콜드 0.4~0.55초/건), `PagingData.filter`가 페이지 단위 원자적이라 첫 결과까지 약 10초.
      통과 항목이 적은 조합은 페이지를 연쇄 로드한다(최대 실측 138초). 연쇄 상한 없음, 필터를 PagingSource 레벨로 옮기는 근본 해결은 미실시.
      **KMRB 장애/한도 소진 중 등급 칩을 고르면 전 페이지를 연쇄 로드하며 KMRB를 계속 호출한다**(`GetKoreanRatingUseCase`가 429를 null로 흡수 → "전부 걸러짐";
      칩을 `전체`로 되돌리면 멈춤, 캐시 오염 없음).
    - **"페이지 기아" 해결(2026-09-30, 실기기 검증 10/01)**: 필터가 `cachedIn` *뒤*(`combine`)에 있으면 2페이지 이상 로드된 뒤 등급 칩으로 전부 걸러질 때 Paging이 이어 로드를 멈췄다.
      이제 `SearchViewModel.searchResults`는 `combine(searchParams, 등급)` → `filter` → `flatMapLatest { 검색 + 필터 }` → `cachedIn` 순서라 등급이 바뀌면 재검색(새 Pager)한다
      (대가: 등급 변경마다 재검색 + shimmer). `PagingEmptyStateRegressionTest`·`SearchViewModelTest`가 고정한다 — **필터를 다시 cachedIn 뒤로 옮기지 말 것.**
    - **"결과 없음" 판정**: `resolveSearchResultsPane`(`SearchResultsPane.kt`)이 `append.endOfPaginationReached` 이후에만 "결과 없음", 그 전엔 shimmer, append 실패 시 오류 화면(재시도)을 보인다.
      "Paging이 빈 페이지 뒤에도 이어 로드한다"는 전제에 기대므로 깨지면 오판 대신 무한 shimmer가 된다.
    - **즉시 검색/debounce 중복 호출 제거(2026-10-02)**: `searchParams`의 `distinctUntilChanged`를 merge *뒤*로 옮기고 타이핑 경로도 `trim()`(2건→1건 실측).
      같은 조건으로 이미 검색한 뒤의 엔터는 재검색하지 않는다(재시도는 화면 버튼). **dedupe를 가지별로 되돌리지 말 것.**
    - 박스오피스 매칭(`MatchBoxOfficeWithTmdbUseCase`)의 TMDB 폴백 검색은 앱 언어와 무관하게 ko-KR 고정(KOFIC 한국어 영화명 매칭 정확도). 앱 언어 변경 시 홈 캐시는
      `InvalidateHomeMovieCacheUseCase`로 즉시 무효화. 즐겨찾기/워치리스트/시청기록의 저장 제목은 추가 시점 스냅샷이라 소급 갱신되지 않는다(의도).
- **검색 "최근 검색어" 목록이 비어 보이던 현상(10/08 수정, 실기기 SM-S926N)**: UI 층 문제(Room/Flow 정상) — X로 지운 뒤에도 *필터된* 목록이 어댑터에 남았다.
  `resolveRecentSearchesPane`(`RecentSearchesPane.kt`, 순수 함수, `RecentSearchesPaneTest`)이 목록·영역 처리를 정하고 collector와 `updateVisibility`의 blank 분기가 같은 판정을 쓴다.
  **연결부(apply 호출, `submitList`)는 JVM 테스트로 못 잡으니 `SearchFragment`를 고칠 땐 실기기 시나리오로 확인할 것.** 필터본→전체본 교체 시 스크롤 고정으로 최신 검색어가 가려지므로
  `pendingScrollToTop`을 non-blank→blank 전이에서만 세우고 모든 제출의 commit 콜백이 소비한다(콜백 인자가 아닌 필드: `AsyncListDiffer`는 세대가 밀린 제출의 콜백을 버린다).
  - **남은 한계(미해결)**: 배우 모드 가드 없음(코드 추론, 재현 안 됨) / 제출 직후 `Filtered`가 목록을 다시 보이게 할 수 있음 / 장르 칩 해제가 `updateVisibility`를 안 불러 전환이 안 될 수 있음 /
    초기 안내 깜빡임과 `star wars` 제출→X 11회 중 1회 `OTHER`는 **확인 못 함**.
- **가로 모드 검색(123일차, 실기기 1440x3120/560dpi/3버튼)**: 가로 콘텐츠 영역이 173px(고정 스택이 높이의 86%)라 최근 검색어 `rv`=0·결과도 173px였다.
  `layout-land/fragment_search.xml`(왼쪽 320dp 스크롤 패널에 toolbar·입력창·칩, 오른쪽 콘텐츠)로 1139px/`rv` 915px. **가로 루트 `baselineAligned="false"` 필수**: 기본값이면 weight 자식이 UNSPECIFIED
  높이로 선측정돼 결과 `LazyVerticalGrid`가 가로 검색어 입력 시 크래시. id·뷰 클래스·루트는 세로와 같게, 구조용 컨테이너엔 id 금지(`SearchLayoutConsistencyTest`; 속성 차이는 못 잡으니 세로를 고치면 가로도).
  **한계**: 결과 그리드 4열 유지(창 폭 기준); 키보드 올림 시 `fitsSystemWindows`가 `bottom_nav` 높이만큼 과다 패딩(세로 `rv` 113px); 다른 탭 가로·다른 크기/태블릿/제스처 내비 미측정.
- **다른 탭 가로 점검과 홈 1단계 수정(124일차, 실기기 SM-S926N 1440x3120/560dpi/3버튼)**:
  - **측정(가로 콘텐츠 영역 px / 세로)**: 사용 불가는 홈뿐 — `rv_movies`가 세로·가로 모두 0px(스크롤 없는 LinearLayout 루트 + wrap_content 섹션이 높이를 다 씀, 시청기록 유무에 이진 의존). 통계·설정·알림 기록 943/2265, 검색 1139/1495 정상. 상세·인물·컬렉션·즐겨찾기는 가로에서 좁지만 도달 가능(즐겨찾기 가로 목록은 자연 높이의 41%만 보임).
  - **홈 수정 = 1단계(섹션 접기)**: 섹션을 `CoordinatorLayout` + `AppBarLayout`(`app_bar_home_sections`, `scroll`)으로 감싸고 목록에 `appbar_scrolling_view_behavior`. 루트는 LinearLayout 유지(`baseline-prof.txt`가 `FragmentHomeBinding.getRoot()`를 고정). "맨 위로"(FAB·탭 재선택)는 섹션도 펼침(`HomeFragment.scrollHomeToTop`, `MainActivity.scrollCurrentTabToTop`). `HomeLayoutStructureTest`가 구조를 고정하나 **높이 값은 JVM이 못 잡으니 레이아웃을 고치면 실기기로 확인.** 접은 뒤 목록 가시 높이 세로 2265px·가로 775px.
  - **첫 화면에서는 목록이 여전히 안 보이고(시청기록 있으면 0px) 밀어야 한다는 단서가 없다 — 별도 제품 결정 필요.**
  - **R1 재정의**: 변경 전 시청기록 937px·가로 박스오피스 775px는 마지막 wrap_content 자식이 남은 높이에 맞춰 **잘린 값**이었다. 접히는 앱바 안에서는 자연 높이(1024/1328px). 불변은 툴바·탭 491px와 세로 박스오피스 1328px.
  - **동작 변화**: 섹션이 접힌 상태에서 아래로 당기면 첫 당김은 섹션을 펼치고(스피너 없음) 새로고침은 그다음부터.
  - **`HomeFragmentTest`(androidTest)는 `rv_movies` `isDisplayed`를 검사해 이 기기에서 이미 실패할 가능성이 크다 — 미수정·미실행, CI도 androidTest 안 돌림.**
  - **후보(미수정)**: 상세·인물 가로 backdrop 280dp 고정, 그리드 열 수가 너비만 기준(가로 4열), 상세 FAB가 가로 3버튼 내비와 겹침, 컬렉션 가로 앱바 700px, 오프라인+트렌딩 빈 화면, 홈 가로 왼쪽 컷아웃 인셋 126px 미적용, 온보딩 "다음" 버튼이 내비 바와 겹침(AVD만), `baseline-prof.txt`의 `FragmentHomeBinding.<init>` 줄 stale. **미측정**: 다크 모드·TalkBack·다른 크기/태블릿·프로세스 종료 후 접힘 복원.
- **상세 메모 입력란 키보드 가림 수정(125일차, 실기기 SM-S926N 3버튼)**: 키보드를 올리면 입력란이 IME 뒤로 가렸고(25회 이상 가시 0px) 메모 0개면 스크롤로도 못 닿았다(입력란 아래 콘텐츠 1068px < IME 1326px).
  - **원인(설정 아닌 구조)**: `DetailFragment`의 `nestedScroll` 인셋 리스너가 `navigationBars`만 반영하고 IME를 무시, `enableEdgeToEdge`라 창이 안 줄어든다. `windowSoftInputMode`는 이미 `adjust=resize`라 원인 아님.
  - **수정(`DetailFragment.kt` 1개, +22/-2)**: 패딩=`max(nav, ime)`, IME가 올라오고 `et_memo` 포커스면 `scrollMemoAboveIme()`가 입력란 아래 끝을 IME 위(+8dp)로 스크롤, 입력란 높이 변화(1→4줄)도 재보정. IME가 없으면 수정 전과 같다.
  - **R6 재정의**: "닫힘은 수정 전과 같을 것"은 포커스 해제·키보드 닫힘 동작이고 닫힌 뒤 스크롤 위치는 대상 아님 — 닫히면 스크롤 끝으로 clamp(A −147~−161px, C −875px, 프레임상 깜빡임·빈 화면 없음).
  - **한계**: 키보드 연 채 세로→가로→세로 회전 시 657px 과다 스크롤(전부 보임), 프로세스 복원+키보드 복원 시 자동 스크롤 안 됨(스크롤하면 닿음), 저장 직후 메모 목록 미표시(0/6, 별개 결함·원인 미확인, `setHasFixedSize` 가설 지지 안 됨). 상세 외 화면 IME는 미측정.
- **Baseline Profiles**: `:baselineprofile` 모듈 (minSdk 28), 플러그인 `1.5.0-alpha02` (AGP 9 호환)
