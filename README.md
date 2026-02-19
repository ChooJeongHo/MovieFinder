# MovieFinder

TMDB (The Movie Database) API를 활용한 영화 검색 Android 앱입니다.
Clean Architecture + MVVM 패턴 기반으로, 오프라인 캐시, 다크 모드, 페이징, 딥링크 등 실무 수준의 기능을 구현했습니다.

## Screenshots

| Home | Search | Detail | Favorite |
|:----:|:------:|:------:|:--------:|
| <img src="screenshots/home.png" width="200"/> | <img src="screenshots/search.png" width="200"/> | <img src="screenshots/detail.png" width="200"/> | <img src="screenshots/favorite.png" width="200"/> |

## Tech Stack

| Category | Stack |
|----------|-------|
| Language | Kotlin 2.2.10 |
| Build | AGP 9.0.1, Gradle 9.1.0, KSP 2.3.2 |
| UI | XML Layouts, ViewBinding, Material Components 1.13.0 |
| Architecture | Clean Architecture, MVVM |
| DI | Hilt 2.59.1 |
| Network | Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization 1.7.3 |
| Database | Room 2.8.4, DataStore Preferences 1.2.0 |
| Async | Coroutines, Flow, StateFlow, SharedFlow |
| Paging | Paging 3.4.0, RemoteMediator |
| Image | Coil 3.3.0 (메모리 25% + 디스크 5% 캐시) |
| Navigation | Navigation Component 2.9.7, Safe Args |
| Test | JUnit 4, MockK 1.14.9, Turbine 1.2.1 (54개 테스트) |
| CI/CD | GitHub Actions (Detekt + Lint + Build + Test) |
| Static Analysis | Detekt 2.0.0-alpha.2 + KtLint |
| Logging | Timber 5.0.1 |
| Performance | Baseline Profiles, Splash Screen API, App Startup |

## Architecture

### Clean Architecture + MVVM

```
API/DB  →  Repository  →  UseCase  →  ViewModel  →  Fragment (XML UI)
```

```
app/src/main/java/com/choo/moviefinder/
├── core/                  # 공유 유틸리티
│   ├── startup/           # App Startup (Timber, StrictMode)
│   └── util/              # ErrorMessageProvider, ImageUrlProvider, NetworkMonitor
├── data/                  # 데이터 레이어
│   ├── local/             # Room DB, DataStore, DAO, Entity
│   ├── paging/            # PagingSource (검색), RemoteMediator (홈)
│   ├── remote/            # Retrofit API Service, DTO
│   ├── repository/        # Repository 구현체
│   └── util/              # 상수 (PAGE_SIZE, 언어 코드)
├── domain/                # 도메인 레이어 (순수 Kotlin)
│   ├── model/             # Movie, MovieDetail, Cast, ThemeMode
│   ├── repository/        # Repository 인터페이스
│   └── usecase/           # UseCase 15개
├── presentation/          # 프레젠테이션 레이어
│   ├── adapter/           # RecyclerView 어댑터 6개
│   ├── common/            # CircularRatingView (커스텀 뷰)
│   ├── detail/            # 상세 화면 (DetailFragment, DetailViewModel)
│   ├── favorite/          # 즐겨찾기 화면
│   ├── home/              # 홈 화면
│   └── search/            # 검색 화면
├── di/                    # Hilt DI 모듈
├── MainActivity.kt        # NavHostFragment + 딥링크 + 네트워크 Snackbar
└── MovieFinderApp.kt      # Application (@HiltAndroidApp, Coil/테마 설정)
```

### 레이어 규칙

- **Domain 레이어**: 순수 Kotlin, Android 프레임워크 의존성 없음
- **Data 레이어**: API 통신, DB 접근, Domain Repository 인터페이스 구현
- **Presentation 레이어**: ViewModel + Fragment/XML UI, Domain UseCase에만 의존 (Data 레이어 직접 참조 금지)
- **Core 레이어**: 공유 유틸리티, 모든 레이어에서 접근 가능

## Features

### 1. 홈 화면

- **현재 상영작 / 인기 영화** TabLayout 전환
- **Paging 3 무한 스크롤** (페이지당 20개, GridLayoutManager 2열)
- **오프라인 지원**: RemoteMediator + Room 캐시 — 네트워크 오류 시에도 캐시 데이터 표시
- **Facebook Shimmer** 로딩 애니메이션
- **테마 설정**: 툴바 메뉴에서 라이트/다크/시스템 전환 (DataStore 저장)

### 2. 검색 화면

- **300ms debounce** 적용 실시간 검색 (`Flow.combine` → `debounce` → `distinctUntilChanged` → `flatMapLatest`)
- **연도 필터**: ChipGroup + MaterialAlertDialog로 개봉 연도 필터링
- **최근 검색어** Room DB 저장/삭제/전체삭제
- 상태별 UI 전환 (초기/검색중/결과없음/결과표시)

### 3. 영화 상세 화면

- **CoordinatorLayout + CollapsingToolbarLayout** 패럴랙스 배경 이미지
- 상세/출연진/비슷한 영화/예고편 **4개 API 병렬 호출** (`coroutineScope` + `async`)
- **부분 실패 처리**: 출연진/비슷한 영화/예고편 API 실패 시 빈 리스트로 대체 (상세 정보는 유지)
- **ChipGroup**으로 장르 칩 동적 추가
- **FAB**으로 즐겨찾기 토글 + 실패 시 Snackbar 피드백
- **예고편 재생**: TMDB Videos API → YouTube 앱/웹 브라우저 연결
- **Shared Element Transition**: 홈/검색/즐겨찾기 → 상세 화면 포스터 이미지 공유 전환

### 4. 즐겨찾기 화면

- **Room Flow** 기반 실시간 업데이트 (`repeatOnLifecycle`)
- **스와이프 삭제**: ItemTouchHelper 왼쪽 스와이프 + Snackbar 실행취소 (Undo)
- 빈 상태 UI

### 5. 딥링크

- **커스텀 스킴**: `moviefinder://movie/{movieId}` (Navigation Component 자동 처리)
- **TMDB 웹 URL**: `https://www.themoviedb.org/movie/{movieId}` (MainActivity에서 수동 파싱)

## 핵심 구현 상세

### 오프라인 지원 (RemoteMediator 패턴)

홈 화면의 현재 상영작/인기 영화는 Room DB를 Single Source of Truth로 사용합니다.

```
[온라인] API → RemoteMediator → Room 캐시 저장 → Room PagingSource → UI
[오프라인] Room 캐시 데이터 즉시 표시 (네트워크 오류 시에도 기존 데이터 유지)
```

- `CachedMovieEntity`: 복합 PK(`id` + `category`), 카테고리별 인덱스
- `RemoteKeyEntity`: 페이징 키 추적
- `MovieRemoteMediator`: API → Room 동기화, **`withTransaction`으로 원자적 캐시 갱신**
- 검색은 네트워크 전용 (`MoviePagingSource`)

### 에러 처리 구조

ViewModel이 Android Context에 의존하지 않도록 에러 타입을 분리했습니다.

```
예외 발생 → ErrorMessageProvider.getErrorType() → ErrorType (NETWORK/TIMEOUT/SERVER/UNKNOWN)
         → ViewModel: ErrorType만 보유 (Context 불필요)
         → Fragment: ErrorMessageProvider.getMessage(context, errorType) → 한국어 메시지 표시
```

- `CancellationException`은 catch하지 않고 rethrow (코루틴 취소 시 잘못된 에러 상태 방지)
- HomeFragment/MovieLoadStateAdapter 등 View 레이어에서는 기존 `getErrorMessage(context, throwable)` 사용

### 네트워크 보안

- **Certificate Pinning**: `api.themoviedb.org` leaf + intermediate SHA-256 핀 (OkHttp `CertificatePinner`)
- **Network Security Config**: `cleartextTrafficPermitted="false"` (HTTPS 강제)
- **API Key**: `local.properties` → `BuildConfig` 주입, OkHttp Interceptor로 모든 요청에 자동 추가

### 다크 모드

- `Theme.MaterialComponents.DayNight.NoActionBar` 기반
- `values-night/colors.xml`에 다크 모드 전용 색상 정의
- 11개 아이콘 drawable: `android:fillColor="@color/icon_default"` (라이트: 검정, 다크: 흰색)
- DataStore에 테마 설정 저장, `MovieFinderApp.onCreate()`에서 `runBlocking`으로 초기 테마 동기 적용 (깜빡임 방지)
- `ProcessLifecycleOwner`로 이후 테마 변경 실시간 반영

### 이미지 캐시 (Coil 3)

- 메모리 캐시: 앱 메모리의 25%
- 디스크 캐시: 디스크의 5%
- `ImageUrlProvider`를 통해 이미지 URL 중앙 관리 (`posterUrl`, `backdropUrl`, `profileUrl`)

### Shared Element Transition

```
FragmentNavigatorExtras(posterView to "poster_$movieId")
  → DetailFragment: postponeEnterTransition() + doOnPreDraw { startPostponedEnterTransition() }
  → TransitionSet: ChangeBounds + ChangeTransform + ChangeImageTransform
```

## Testing

총 **54개** 유닛 테스트를 작성하여 핵심 비즈니스 로직을 검증합니다.

```bash
./gradlew testDebugUnitTest
```

| 테스트 클래스 | 테스트 수 | 대상 |
|---|---|---|
| `MovieRepositoryImplTest` | 20 | 영화 상세, 출연진, 비슷한 영화, 예고편 키(5개), 즐겨찾기 토글/조회, 검색 기록 CRUD |
| `DetailViewModelTest` | 9 | 초기 상태, ErrorType 에러, 부분 실패, 즐겨찾기 토글, Snackbar 이벤트, 재시도 |
| `SearchViewModelTest` | 7 | 검색어 변경, 검색 저장, 빈 검색어, 삭제, 전체 삭제, 최근 검색어 |
| `ErrorMessageProviderTest` | 6 | 예외 타입별 ErrorType 매핑 (Network, Timeout, Server, Unknown) |
| `HomeViewModelTest` | 5 | UseCase 호출 검증, 테마 기본값/변경 |
| `PreferencesRepositoryImplTest` | 4 | 테마 기본값, DARK/LIGHT 저장, 테마 변경 (실제 DataStore 사용) |
| `FavoriteViewModelTest` | 3 | 즐겨찾기 목록, 빈 목록, 토글 |

### 테스트 패턴

- **MockK**: UseCase/Repository/DAO/API 모킹
- **Turbine**: StateFlow/SharedFlow 수집 및 검증
- **StandardTestDispatcher** + `Dispatchers.setMain()`: 코루틴 테스트
- **PreferenceDataStoreFactory**: 실제 DataStore를 사용한 통합 테스트

## CI/CD

### GitHub Actions (`.github/workflows/android-ci.yml`)

- **트리거**: `push` / `pull_request` → `main` 브랜치
- **빌드 환경**: Ubuntu + JDK 21 (Temurin) + Gradle 캐시
- **파이프라인**: Detekt → Lint → Debug Build → Unit Test
- **Artifact**: Lint 결과 HTML + 테스트 결과 HTML 업로드
- **Concurrency**: 동일 브랜치 중복 실행 시 이전 실행 자동 취소

### 정적 분석

```bash
./gradlew :app:detekt          # Detekt + KtLint 규칙
./gradlew jacocoTestReport     # 커버리지 리포트 (app/build/reports/jacoco/)
```

## 성능 최적화

- **Baseline Profiles**: `ProfileInstaller` + `:baselineprofile` 모듈로 앱 시작 성능 최적화
- **App Startup**: `TimberInitializer`로 초기화 최적화, `StrictModeInitializer`는 디버그 빌드에서만 등록
- **Coil 이미지 캐시**: 메모리 25% + 디스크 5% (네트워크 요청 최소화)
- **Paging 3**: 페이지 단위 로딩으로 메모리 효율적 무한 스크롤
- **Room 인덱스**: `CachedMovieEntity(category)`, `FavoriteMovieEntity(addedAt)`에 인덱스 추가
- **Gradle Configuration Cache**: 반복 빌드 속도 개선
- **HttpLoggingInterceptor**: 릴리스 빌드에서 객체 자체를 미생성하여 오버헤드 제거

## Setup

### 1. API Key 발급

[TMDB](https://www.themoviedb.org/settings/api)에서 API 키를 발급받습니다.

### 2. local.properties 설정

```properties
TMDB_API_KEY=your_api_key_here
```

### 3. 빌드

```bash
./gradlew assembleDebug
```

### 4. 딥링크 테스트 (adb)

```bash
# 커스텀 스킴
adb shell am start -a android.intent.action.VIEW -d "moviefinder://movie/550"

# TMDB 웹 URL
adb shell am start -a android.intent.action.VIEW -d "https://www.themoviedb.org/movie/550"
```

### GitHub Secrets (CI/CD)

Repository Settings > Secrets and variables > Actions:
- `TMDB_API_KEY`: TMDB API 키 값

## Requirements

- Android Studio (AGP 9.0.1+)
- JDK 21+
- minSdk 24 / targetSdk 36

## License

MIT License