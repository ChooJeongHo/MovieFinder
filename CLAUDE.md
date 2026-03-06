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
| Paging 3 | 3.4.0 | 무한 스크롤 페이징 |
| Retrofit 3 | 3.0.0 | REST API 통신 |
| OkHttp 5 | 5.0.0-alpha.14 | HTTP 클라이언트 + 로깅 (kotlin.time.Duration API) |
| kotlinx.serialization | 1.8.1 | JSON 직렬화/역직렬화 |
| Coil 3 | 3.4.0 | 이미지 로딩 (View 버전, 메모리+디스크 캐시) |
| Facebook Shimmer | 0.5.0 | 로딩 Shimmer 애니메이션 |
| Lifecycle | 2.10.0 | 생명주기 인식 컴포넌트 |
| Splash Screen API | 1.2.0 | 스플래시 화면 |
| SwipeRefreshLayout | 1.2.0 | 당겨서 새로고침 |
| DataStore Preferences | 1.2.0 | 사용자 설정 저장 (테마 등) |
| Lifecycle Process | 2.10.0 | ProcessLifecycleOwner (앱 수준 생명주기) |
| ProfileInstaller | 1.4.1 | Baseline Profiles 설치 |
| Benchmark Macro | 1.5.0-alpha02 | Baseline Profile 생성 |
| Timber | 5.0.1 | 구조화 로깅 |
| App Startup | 1.2.0 | 초기화 최적화 (Timber 등) |
| WorkManager | 2.10.1 | 개봉일 알림 스케줄링 |
| kotlinx-datetime | 0.6.2 | 멀티플랫폼 날짜/시간 API (Calendar/SimpleDateFormat 대체) |
| DataStore (Typed) | 1.2.0 | kotlinx.serialization JSON 기반 타입 안전 설정 저장 |

### 개발/디버그 도구
| 도구 | 버전 | 용도 |
|---|---|---|
| Detekt | 2.0.0-alpha.2 | Kotlin 정적 분석 + KtLint 규칙 |
| JaCoCo | (Gradle 내장) | 테스트 코드 커버리지 리포트 |
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
│   │   └── StrictModeInitializer.kt # App Startup StrictMode (디버그 전용)
│   ├── notification/
│   │   ├── ReleaseNotificationScheduler.kt # 개봉일 알림 스케줄링 (WorkManager)
│   │   └── ReleaseNotificationWorker.kt    # 개봉일 알림 Worker
│   └── util/
│       ├── ImageUrlProvider.kt    # 이미지 URL 빌더
│       ├── ErrorMessageProvider.kt # 예외 → ErrorType 매핑 + 사용자 메시지 변환
│       └── NetworkMonitor.kt      # 실시간 네트워크 상태 모니터링 (StateFlow)
├── baselineprofile/       # Baseline Profile 생성 모듈
│   └── BaselineProfileGenerator.kt # 앱 시작 시나리오
├── data/                  # 데이터 레이어
│   ├── local/
│   │   ├── MovieDatabase.kt       # Room DB (version 9)
│   │   ├── PreferencesRepositoryImpl.kt # DataStore 기반 설정 저장소
│   │   ├── dao/                   # DAO (6개)
│   │   │   ├── FavoriteMovieDao.kt  # abstract class (@Transaction toggleFavorite)
│   │   │   ├── RecentSearchDao.kt
│   │   │   ├── CachedMovieDao.kt  # 오프라인 캐시 (PagingSource 반환)
│   │   │   ├── RemoteKeyDao.kt    # 페이징 키 관리
│   │   │   ├── UserRatingDao.kt    # 사용자 평점 (CRUD)
│   │   │   ├── WatchHistoryDao.kt # 시청 기록 (최근 20개)
│   │   │   └── WatchlistDao.kt    # abstract class (@Transaction toggleWatchlist)
│   │   └── entity/                # Room Entity (7개)
│   │       ├── FavoriteMovieEntity.kt  # 인덱스: addedAt
│   │       ├── RecentSearchEntity.kt  # 인덱스: timestamp
│   │       ├── CachedMovieEntity.kt    # 인덱스: category, 복합PK: id+category
│   │       ├── RemoteKeyEntity.kt      # lastUpdated 타임스탬프 포함
│   │       ├── UserRatingEntity.kt      # 사용자 영화 평점 (PK: movieId)
│   │       ├── WatchHistoryEntity.kt   # 인덱스: watchedAt
│   │       └── WatchlistEntity.kt      # 인덱스: addedAt
│   ├── paging/
│   │   ├── MoviePagingSource.kt       # 네트워크 전용 PagingSource (검색용)
│   │   ├── DiscoverPagingSource.kt    # 장르/정렬 기반 탐색 PagingSource
│   │   └── MovieRemoteMediator.kt     # 오프라인 지원 RemoteMediator (홈 화면, 1시간 캐시 만료)
│   ├── remote/            # Retrofit API (Service, DTO)
│   ├── repository/        # Repository 구현체
│   └── util/              # 상수 정의 (PAGE_SIZE, PREFETCH_DISTANCE, DEFAULT_PAGING_CONFIG, API 언어 코드)
├── di/                    # Hilt DI 모듈
│   ├── DatabaseModule.kt  # Room DB + DAO 제공 (destructive migration fallback)
│   ├── DataStoreModule.kt # DataStore Preferences 제공
│   ├── NetworkModule.kt   # Retrofit/OkHttp 제공 (API key interceptor, Certificate Pinning, @ImageOkHttpClient, NetworkMonitor)
│   └── RepositoryModule.kt # Repository 바인딩 (Movie + Preferences)
├── domain/                # 도메인 레이어
│   ├── model/             # 도메인 모델 (Movie, MovieDetail, Cast, Review, ThemeMode)
│   ├── repository/        # Repository 인터페이스
│   └── usecase/           # UseCase 클래스 (30개, 테마/시청기록/워치리스트/장르/등급/리뷰/사용자평점 포함)
├── presentation/          # 프레젠테이션 레이어
│   ├── adapter/           # RecyclerView 어댑터 (7개) + MovieGridViewHolder + MovieListViewHolder (뷰 모드별 ViewHolder)
│   ├── common/            # CircularRatingView (커스텀 뷰), GridLayoutManagerFactory (LoadState-aware 그리드)
│   ├── detail/            # 영화 상세 화면 (DetailFragment, DetailViewModel, Mutex 중복 호출 방지)
│   ├── favorite/          # 즐겨찾기/워치리스트 화면 (FavoriteFragment, FavoriteViewModel, TabLayout 탭 전환, 정렬, FavoriteSortOrder)
│   ├── home/              # 홈 화면 (HomeFragment, HomeViewModel, 시청 기록, 탭 상태 저장)
│   ├── search/            # 검색 화면 (SearchFragment, SearchViewModel, 장르/정렬 필터, SavedStateHandle)
│   ├── settings/          # 설정 화면 (SettingsFragment, SettingsViewModel, 테마/캐시/시청기록 관리)
│   └── widget/            # 홈 화면 위젯 (PopularMoviesWidget, RemoteViewsService/Factory)
├── MainActivity.kt        # 진입점 (AppCompatActivity + NavHostFragment + 딥링크 처리 + 네트워크 Snackbar)
└── MovieFinderApp.kt      # Application 클래스 (@HiltAndroidApp, Coil 캐시 설정 + Certificate Pinning + NotificationChannel)
```

### UI 프레임워크
- **XML Layouts + ViewBinding**: 전통적 Android View 시스템
- **Fragments**: 각 화면을 Fragment로 구현
- **RecyclerView + PagingDataAdapter**: 리스트/그리드 표시
- **Navigation Component + Safe Args**: 타입 안전 Fragment 간 전환

### 레이어 규칙
- **Core 레이어**: 공유 유틸리티 (ImageUrlProvider, ErrorMessageProvider 등), 모든 레이어에서 접근 가능
- **Domain 레이어**: 순수 Kotlin, Android 프레임워크 의존성 없음 (Paging은 예외)
- **Data 레이어**: API 통신, DB 접근, Domain 레이어의 Repository 인터페이스 구현
- **Presentation 레이어**: ViewModel + Fragment/XML UI, Domain UseCase와 Core에만 의존 (Data 레이어 직접 참조 금지)

### 데이터 흐름
```
API/DB → Repository → UseCase → ViewModel → Fragment (XML UI)
```
- 홈 화면 (오프라인 지원): API → RemoteMediator → Room DB → PagingSource → PagingDataAdapter
- 검색 (네트워크 전용): API → MoviePagingSource → PagingDataAdapter
- 즐겨찾기: Room `Flow<List<Entity>>` → Repository 매핑 → `stateIn()` → `repeatOnLifecycle` 수집
- UI 상태 수집: `viewLifecycleOwner.lifecycleScope.launch { repeatOnLifecycle(STARTED) { flow.collect {} } }`

## 주요 기능

### 1. 홈 화면 (HomeFragment)
- **시청 기록**: 최근 본 영화 가로 스크롤 RecyclerView (HorizontalMovieAdapter, 최근 20개)
- TabLayout으로 "현재 상영작" / "인기 영화" 전환
- **탭 상태 저장**: `savedInstanceState`로 화면 회전 시 현재 탭 복원
- Paging 3 무한 스크롤 (페이지당 20개)
- **Pull-to-Refresh**: SwipeRefreshLayout으로 당겨서 새로고침 (colorPrimary 색상, LoadState 연동)
- **오프라인 지원**: RemoteMediator + Room 캐시 (네트워크 오류 시 캐시 데이터 표시)
- Facebook Shimmer 로딩 애니메이션
- GridLayoutManager (spanCount=2)
- MoviePagingAdapter + MovieLoadStateAdapter (withLoadStateFooter)
- **RecyclerView 아이템 애니메이션**: staggered slide-up layoutAnimation
- **맨 위로 FAB**: 스크롤 시 mini FAB 표시, 탭하면 최상단 이동

### 2. 검색 화면 (SearchFragment)
- 300ms debounce 적용 검색 + 즉시 검색 (merge 패턴: debounced flow + immediate SharedFlow)
- Flow combine(query, year, genres, sortBy) → debounce → distinctUntilChanged → merge(immediate) → flatMapLatest
- TextInputLayout + TextInputEditText (둥근 모서리)
- **연도 필터**: ChipGroup + `setSingleChoiceItems` MaterialAlertDialog (현재 선택 상태 표시)
- **장르 필터**: ChipGroup + `setMultiChoiceItems` MaterialAlertDialog (다중 선택, TMDB genre/movie/list API)
- **정렬 필터**: ChipGroup + `setSingleChoiceItems` (인기순/평점순/개봉일순/수익순)
- **Discover 모드**: 검색어 없이 장르/정렬 필터만으로 영화 탐색 (TMDB discover/movie API), **Discover 모드 Chip 표시**
- **SavedStateHandle**: 검색어 + 연도 + 장르 + 정렬 필터 프로세스 사망 시 복원 (IntArray/String으로 저장)
- **검색 자동완성**: 입력 중 최근 검색어 기반 필터링 제안 (기존 RecentSearchAdapter 활용)
- **검색어 trim 처리**: `onSearch` 시 공백 제거하여 중복 저장 방지
- 최근 검색어 Room DB 저장/삭제/전체삭제 (RecentSearchAdapter)
- 상태별 UI 전환 (초기/검색중/결과없음/결과표시)
- **검색 추천**: 결과 없을 때 추천 검색어 칩 표시 (마블, 스파이더맨, 배트맨, 스타워즈, 해리포터)
- **맨 위로 FAB**: 스크롤 시 mini FAB 표시, 탭하면 최상단 이동
- **보기 모드 전환**: Toolbar 메뉴 토글로 그리드 ↔ 리스트 뷰 전환 (SavedStateHandle 저장)
- **장르 칩 복원 개선**: 장르 API 미로드 시 개수만 표시, 로드 완료 시 이름으로 갱신
- **다이얼로그 생명주기 관리**: `onDestroyView()`에서 dismiss

### 3. 영화 상세 화면 (DetailFragment)
- CoordinatorLayout + CollapsingToolbarLayout (배경 이미지 패럴랙스)
- 배경 이미지, 제목, 평점, 개봉일, 런타임, 태그라인
- **콘텐츠 등급 배지**: TMDB release_dates API로 KR/US 등급 조회 (Chip 표시)
- ChipGroup으로 장르 칩 동적 추가
- 출연진 (Cast) 가로 스크롤 RecyclerView (CastAdapter)
- 비슷한 영화 추천 가로 스크롤 RecyclerView (HorizontalMovieAdapter)
- **확장 상세 정보**: 제작비, 수익, 원어, 상태, IMDb 링크
- **영화 공유**: Toolbar 메뉴에서 Intent.ACTION_SEND로 영화 정보 공유
- FAB으로 즐겨찾기 토글 + **워치리스트 FAB**: 보고 싶은 영화 토글
- **FAB 바운스 애니메이션**: 토글 시 scale bounce 효과
- **영화 예고편 재생**: TMDB Videos API로 YouTube 키 획득 → YouTube 앱/웹 브라우저로 연결 (Intent.ACTION_VIEW)
- **Shared Element Transition**: 홈/검색/즐겨찾기 → 상세 화면 포스터 이미지 공유 전환 (ChangeBounds + ChangeTransform + ChangeImageTransform)
- **영화 리뷰**: TMDB reviews API로 사용자 리뷰 표시 (ReviewAdapter, 클릭 시 확장/축소)
- 상세/출연진/비슷한 영화/예고편/등급/리뷰 6개 API를 async로 병렬 호출
- **시청 기록 자동 저장**: 상세 화면 진입 시 Room DB에 저장
- **부분 실패 처리**: 출연진/비슷한 영화/예고편/등급/리뷰 API 실패 시 빈 리스트/null로 대체 (상세 정보는 유지, `loadOptional`/`loadOptionalNullable` 헬퍼)
- **중복 호출 방지**: `Mutex.tryLock()` 기반 원자적 로딩 가드로 중복 API 호출 차단 (에러 후 재시도 가능)
- **재시도 UI**: 에러 시 재시도 버튼 비활성화 → 완료 후 재활성화
- **즐겨찾기 토글 실패 피드백**: Channel → `receiveAsFlow()` → Snackbar로 에러 메시지 표시 (이벤트 유실 방지)
- **출연진 정렬**: `order` 필드 기준 오름차순 정렬
- **사용자 평점**: RatingBar로 0.5~5.0 별점 매기기 (Room DB 저장), 삭제 버튼
- NestedScrollView 기반 스크롤

### 4. 즐겨찾기/워치리스트 화면 (FavoriteFragment)
- **TabLayout 탭 전환**: "즐겨찾기" / "보고 싶은 영화" 탭
- Room DB 기반 오프라인 조회
- Flow를 통한 실시간 업데이트 (repeatOnLifecycle)
- **정렬 옵션**: Toolbar 메뉴 → MaterialAlertDialog (추가 날짜순/제목순/평점순, FavoriteSortOrder enum)
- **스와이프 삭제**: ItemTouchHelper 왼쪽 스와이프로 삭제 + Snackbar 실행취소 (Undo, 양 탭 모두 지원)
- **토글 에러 피드백**: Channel → `receiveAsFlow()` → Snackbar로 에러 메시지 표시 (DetailViewModel과 동일 패턴)
- 빈 상태 UI (EmptyState layout include, 탭별 다른 아이콘/메시지)
- MovieAdapter (ListAdapter) 사용
- **탭 상태 저장**: `savedInstanceState`로 화면 회전 시 현재 탭 복원

### 5. 설정 화면 (SettingsFragment)
- **테마 설정**: 라이트/다크/시스템 전환 (MaterialAlertDialog 단일 선택)
- **캐시 삭제**: Coil 메모리 + 디스크 캐시 클리어
- **시청 기록 삭제**: WatchHistoryDao.clearAll() + Channel 기반 성공/에러 이벤트 분리
- **앱 정보**: `getString(R.string.settings_version, VERSION_NAME)` 포맷으로 표시
- BottomNavigationView 4번째 탭으로 접근
- **에러 처리**: setThemeMode/clearWatchHistory에 try-catch + CancellationException rethrow + Snackbar 에러 피드백

### 6. 딥링크 지원
- **커스텀 스킴**: `moviefinder://movie/{movieId}` (Navigation Component 자동 처리)
- **TMDB 웹 URL**: `https://www.themoviedb.org/movie/{movieId}` (MainActivity에서 수동 파싱)
- `onNewIntent()` 처리로 기존 Activity에서도 딥링크 수신

### 7. 홈 화면 위젯 (PopularMoviesWidget)
- **AppWidgetProvider**: 인기 영화 Top 10 목록 표시
- **RemoteViewsService + RemoteViewsFactory**: ListView로 영화 제목 + 평점 표시
- **자동 갱신**: 1시간 주기 업데이트 (`updatePeriodMillis`)
- **새로고침 버튼**: 수동 갱신 (ACTION_REFRESH 브로드캐스트)
- **클릭 → 상세 화면**: 딥링크 (`moviefinder://movie/{movieId}`) PendingIntent
- **다크 모드 지원**: `widget_background.xml` + `drawable-night/` 다크 배경
- OkHttp + kotlinx.serialization으로 직접 API 호출 (Hilt DI 미사용)

### 8. 개봉일 알림 (WorkManager)
- **ReleaseNotificationScheduler**: 워치리스트 추가 시 개봉일 알림 예약, 삭제 시 취소
- **ReleaseNotificationWorker**: 개봉일 당일 오전 9시 알림 표시
- **NotificationChannel**: `release_date_channel` (API 26+)
- **딥링크 연동**: 알림 탭 → 영화 상세 화면 이동
- **POST_NOTIFICATIONS 권한**: API 33+ 런타임 권한 체크
- `ExistingWorkPolicy.KEEP`: 중복 예약 방지

### 9. 다국어 지원 (i18n)
- **한국어** (기본): `values/strings.xml`
- **영어**: `values-en/strings.xml` (161개 문자열 완전 번역)
- 포맷 문자열 (%s, %d, %,d 등) 모든 specifier 보존

### 10. 에러 처리
- **ErrorMessageProvider + ErrorType**: 예외 → ErrorType 열거형 매핑 + Context 기반 한국어 메시지 변환
  - `UnknownHostException` / `ConnectException` / `IOException` → `ErrorType.NETWORK`
  - `SocketTimeoutException` → `ErrorType.TIMEOUT`
  - `HttpException` → `ErrorType.SERVER`
  - `SSLException` / `SSLHandshakeException` → `ErrorType.SSL`
  - `SerializationException` → `ErrorType.PARSE`
  - 기타 → `ErrorType.UNKNOWN`
  - ViewModel은 ErrorType만 보유 (Context 불필요), Fragment에서 문자열 변환
- 네트워크 오류 시 재시도 버튼 (layout_error.xml include)
- Shimmer 로딩 애니메이션
- Empty State UI 구현 (layout_empty_state.xml include)
- Paging append 에러 처리 (MovieLoadStateAdapter)

## RecyclerView 어댑터 (7개 + 공유 ViewHolder)
| 어댑터 | 부모 클래스 | 용도 |
|---|---|---|
| `MoviePagingAdapter` | `PagingDataAdapter` | 홈/검색 그리드 (무한 스크롤) |
| `MovieAdapter` | `ListAdapter` | 즐겨찾기/워치리스트 목록 |
| `CastAdapter` | `ListAdapter` | 상세 화면 출연진 |
| `HorizontalMovieAdapter` | `ListAdapter` | 가로 스크롤 영화 카드 (비슷한 영화 + 시청 기록, transitionPrefix 파라미터) |
| `ReviewAdapter` | `ListAdapter` | 상세 화면 리뷰 (클릭 시 확장/축소) |
| `RecentSearchAdapter` | `ListAdapter` | 검색 화면 최근 검색어 |
| `MovieLoadStateAdapter` | `LoadStateAdapter` | 페이징 로딩/에러 footer |
| `MovieGridViewHolder` | `ViewHolder` | MoviePagingAdapter/MovieAdapter 공유 ViewHolder (그리드 뷰) |
| `MovieListViewHolder` | `ViewHolder` | MoviePagingAdapter 리스트 뷰 ViewHolder |

- 모든 이미지 어댑터에 `onViewRecycled()` 구현 → Coil 이미지 로드 취소 (`dispose()`)

## API 설정

### TMDB API Key 설정
`local.properties` 파일에 API 키를 추가:
```properties
TMDB_API_KEY=여기에_API_키_입력
```
API 키 발급: https://www.themoviedb.org/settings/api

### API Key 관리
- API 키는 `local.properties`에서 읽어 `BuildConfig`에 주입
- OkHttp Interceptor를 통해 모든 요청에 자동으로 `api_key` 쿼리 파라미터 추가
- API Service 메서드에서는 API 키 파라미터 불필요
- **HttpLoggingInterceptor**: 디버그 빌드에서만 생성/추가 (릴리스 빌드 시 객체 자체 미생성)
- **CertificatePinner**: `api.themoviedb.org` + `image.tmdb.org` leaf + intermediate 인증서 SHA-256 핀 적용 (중간자 공격 방지)

### 사용 엔드포인트
| 엔드포인트 | 용도 |
|---|---|
| GET /movie/now_playing | 현재 상영작 (RemoteMediator 페이징) |
| GET /movie/popular | 인기 영화 (RemoteMediator 페이징) |
| GET /search/movie | 영화 검색 (네트워크 페이징, year 필터 지원) |
| GET /discover/movie | 장르/정렬 기반 영화 탐색 (네트워크 페이징) |
| GET /genre/movie/list | 장르 목록 조회 |
| GET /movie/{id} | 영화 상세 정보 |
| GET /movie/{id}/credits | 출연진 정보 |
| GET /movie/{id}/similar | 비슷한 영화 |
| GET /movie/{id}/videos | 예고편 영상 (YouTube 키) |
| GET /movie/{id}/release_dates | 콘텐츠 등급 (KR 우선, US 폴백) |
| GET /movie/{id}/reviews | 사용자 리뷰 |

## 오프라인 지원 (RemoteMediator 패턴)

### 동작 방식
- 홈 화면 (현재 상영작/인기 영화): Room DB를 Single Source of Truth로 사용
- **온라인**: API → RemoteMediator → Room 캐시 저장 → Room PagingSource → UI 표시
- **오프라인**: Room 캐시 데이터 즉시 표시 (네트워크 오류 시에도 기존 데이터 유지)
- 검색은 네트워크 전용 (MoviePagingSource)

### 관련 파일
| 파일 | 역할 |
|---|---|
| `CachedMovieEntity` | 캐시 영화 Entity (복합 PK: id + category, 인덱스: category) |
| `RemoteKeyEntity` | 페이징 키 추적 Entity (PK: category, lastUpdated 타임스탬프) |
| `CachedMovieDao` | Room PagingSource 반환, insertAll, clearByCategory |
| `RemoteKeyDao` | 원격 키 조회/저장/삭제 |
| `MovieRemoteMediator` | API → Room 캐시 동기화 (REFRESH/APPEND 처리, 1시간 캐시 만료) |

## 테스트

### 유닛 테스트 (145개)
```bash
./gradlew testDebugUnitTest
```

| 테스트 클래스 | 테스트 수 | 대상 |
|---|---|---|
| `MovieRepositoryImplTest` | 36 | 영화 상세, 출연진, 비슷한 영화, 예고편 키(5개), 즐겨찾기 DAO 위임/조회, 검색 기록 CRUD, 인증등급(4개), 장르(1개), 시청기록(3개), 워치리스트(4개), 리뷰(2개), 사용자 평점(3개) |
| `DetailViewModelTest` | 24 | 초기 상태, ErrorType 에러, 부분 실패(credits/similar/trailer/reviews), 즐겨찾기 토글, Snackbar 이벤트, 재시도, 중복 호출 방지, 워치리스트 토글/실패/상태, 인증등급 로드, 리뷰 로드/실패, 알림 스케줄/취소, 사용자 평점 설정/삭제/에러/조회(5개) |
| `SearchViewModelTest` | 27 | 검색어 변경, 검색 저장, 빈 검색어, 삭제, 전체 삭제, 최근 검색어, 연도 필터, SavedStateHandle 복원(query/year/genres/sort), 장르 선택/초기화, 정렬 선택/초기값, SortOption 매핑, onSearch trim, SavedStateHandle 저장 검증, 뷰모드 토글(4개), Discover 필터(1개), 장르 재시도(2개) |
| `ErrorMessageProviderTest` | 10 | 예외 타입별 ErrorType 매핑 (Network, Timeout, Server, SSL, Parse, IOException, Unknown) |
| `SettingsViewModelTest` | 10 | 테마 기본값/DARK/LIGHT 반영, 테마 설정(2개), 테마 설정 에러 처리, 시청기록 삭제, 시청기록 삭제 에러 처리, 삭제 성공 이벤트, 삭제 에러 Snackbar 이벤트 |
| `FavoriteViewModelTest` | 10 | 즐겨찾기 목록, 빈 목록, 토글, 토글 에러 Snackbar 이벤트, 워치리스트 목록, 워치리스트 토글, 정렬 순서 변경, 제목순 정렬, 평점순 정렬 |
| `MoviePagingSourceTest` | 7 | 첫 페이지 로드, 에러 처리, 다음 페이지 로드, 마지막 페이지 nextKey null, 첫 페이지 prevKey null, 연도 파라미터 전달, getRefreshKey null |
| `DiscoverPagingSourceTest` | 5 | 첫 페이지 로드, 에러 처리, 파라미터 전달 검증, 마지막 페이지 nextKey null, 다음 페이지 키 |
| `MovieRemoteMediatorTest` | 7 | 캐시 없을 때 REFRESH, 캐시 만료 시 REFRESH, 캐시 유효 시 SKIP, PREPEND 즉시 성공, APPEND 키 없음, APPEND nextKey null, API 에러 처리 |
| `HomeViewModelTest` | 5 | UseCase 호출 검증 (nowPlaying, popular, 동시 호출), 시청기록 목록, 시청기록 빈 목록 |
| `PreferencesRepositoryImplTest` | 4 | 테마 기본값, DARK/LIGHT 저장, 테마 변경 |

### Espresso UI 테스트 (5개)
```bash
./gradlew connectedDebugAndroidTest
```

| 테스트 클래스 | 테스트 수 | 대상 |
|---|---|---|
| `MainActivityTest` | 5 | 하단 네비게이션 표시, 홈 TabLayout 표시, 검색 화면 이동, 즐겨찾기 화면 이동, 설정 화면 이동 |

### 테스트 패턴
- `MockK`: UseCase/Repository/DAO/API 모킹
- `Turbine`: StateFlow/SharedFlow 수집 및 검증
- `StandardTestDispatcher` + `Dispatchers.setMain()`: 코루틴 테스트
- `coEvery`/`coVerify`: suspend 함수 모킹 및 검증

## 빌드 및 실행

### 사전 요구사항
- Android Studio (AGP 9.1.0 지원 버전)
- JDK 21+
- TMDB API Key

### 빌드 명령어
```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리스 빌드
./gradlew assembleRelease

# 유닛 테스트
./gradlew testDebugUnitTest

# 정적 분석 (Detekt + KtLint)
./gradlew :app:detekt

# 테스트 커버리지 리포트
./gradlew jacocoTestReport
# 리포트: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### 딥링크 테스트 (adb)
```bash
# 커스텀 스킴
adb shell am start -a android.intent.action.VIEW -d "moviefinder://movie/550"

# TMDB 웹 URL
adb shell am start -a android.intent.action.VIEW -d "https://www.themoviedb.org/movie/550"
```

## CI/CD (GitHub Actions)

### 워크플로우: `.github/workflows/android-ci.yml`
- **트리거**: `push` / `pull_request` → `main` 브랜치
- **빌드 환경**: Ubuntu + JDK 21 (Temurin) + Gradle 캐시
- **실행 순서**: Detekt → Lint → Debug Build → Unit Test → JaCoCo 리포트
- **Artifact 업로드**: Lint 결과 HTML + 테스트 결과 HTML + Detekt 결과 + JaCoCo 커버리지 리포트 + Debug APK
- **API Key**: GitHub Secrets에서 `TMDB_API_KEY` 주입 → `local.properties` 생성
- **Concurrency**: 동일 브랜치 중복 실행 시 이전 실행 자동 취소

### GitHub Secrets 설정 필요
Repository Settings > Secrets and variables > Actions에서:
- `TMDB_API_KEY`: TMDB API 키 값 추가

## 주의사항

### AGP 9.x 특이사항
- `kotlinOptions` 블록 사용 불가 (Kotlin이 AGP에 내장)
- `android.disallowKotlinSourceSets=false` 설정 필요 (KSP 호환)
- XML 테마는 `Theme.MaterialComponents.DayNight.NoActionBar` 사용
- Hilt 2.59.1 이상 필요 (AGP 9 공식 지원)

### Navigation
- Safe Args 플러그인 기반 타입 안전 네비게이션
- `nav_graph.xml`에 Fragment destination + argument + deepLink 정의
- `FragmentDirections` 클래스로 타입 안전 action 생성
- `navArgs()`로 Fragment에서 argument 수신
- `savedStateHandle.get<Int>("movieId")` 방식으로 ViewModel에서 argument 추출
- BottomNavigationView + `NavigationUI.setupWithNavController()` 연동
- Detail 화면에서 BottomNavigationView 자동 숨김
- Detail self-navigation에 `launchSingleTop="true"` 적용 (백스택 중복 방지)

### Room DB
- 스키마 export 위치: `app/schemas/`
- Entity: FavoriteMovieEntity, RecentSearchEntity, CachedMovieEntity, RemoteKeyEntity, WatchHistoryEntity, WatchlistEntity, UserRatingEntity
- 데이터베이스 이름: `movie_finder_db`
- 데이터베이스 버전: 9
- Destructive migration fallback 적용 (개발 환경)
- 인덱스: CachedMovieEntity(category), FavoriteMovieEntity(addedAt), RecentSearchEntity(timestamp), WatchHistoryEntity(watchedAt), WatchlistEntity(addedAt)

### 이미지 URL 관리
- `ImageUrlProvider` (core/util)를 통해 이미지 URL 생성
- Presentation 레이어에서 BuildConfig/Constants 직접 참조하지 않음
- posterUrl(), backdropUrl(), profileUrl() 헬퍼 함수 제공

### Coil 이미지 캐시
- 메모리 캐시: 앱 메모리의 25% (`MemoryCache.Builder().maxSizePercent(0.25)`)
- 디스크 캐시: 디스크의 5% (`DiskCache.Builder().maxSizePercent(0.05)`)
- 캐시 디렉토리: `cacheDir/image_cache`
- `MovieFinderApp`에서 `SingletonImageLoader.Factory` 구현
- `@ImageOkHttpClient` OkHttpClient로 `image.tmdb.org` Certificate Pinning 적용 (`OkHttpNetworkFetcherFactory`)
- 모든 이미지 어댑터에서 `onViewRecycled()` 시 `dispose()` 호출하여 불필요한 이미지 로드 취소

### 다크 모드
- `Theme.MaterialComponents.DayNight.NoActionBar` 기반
- `values-night/colors.xml`에 다크 모드 전용 색상 정의 (`icon_default`, `backdrop_overlay`)
- 11개 아이콘 drawable: `android:fillColor="@color/icon_default"` (라이트: 검정, 다크: 흰색)
- `fragment_detail.xml`: 배경 오버레이 `@color/backdrop_overlay`, FAB 아이콘 `?attr/colorOnPrimary`

### Shared Element Transition
- 홈/검색/즐겨찾기 → 상세 화면 포스터 이미지 공유 전환
- `FragmentNavigatorExtras(posterView to "poster_$movieId")` 사용
- DetailFragment `onCreate()`에서 `sharedElementEnterTransition` + `sharedElementReturnTransition` 설정 (TransitionSet: ChangeBounds + ChangeTransform + ChangeImageTransform)
- `postponeEnterTransition(500, TimeUnit.MILLISECONDS)` + Coil `listener(onSuccess, onError)` 패턴 (이미지 로드 완료 시 전환 시작, 500ms 타임아웃은 안전장치)
- nav_graph.xml의 상세 화면 이동 action에서 enterAnim/exitAnim 제거 (애니메이션이 Shared Element 전환을 덮어쓰는 문제 방지)

### 영화 예고편 재생
- TMDB `/movie/{id}/videos` API로 YouTube 키 획득
- 우선순위: 공식 Trailer > 비공식 Trailer > YouTube 영상
- `Intent.ACTION_VIEW`로 YouTube 앱/웹 브라우저 연결 (`https://www.youtube.com/watch?v={key}`)

### Fragment ViewBinding 패턴
- `_binding` nullable + `binding` non-null getter 패턴
- `onDestroyView()`에서 `_binding = null` 필수 (메모리 누수 방지)
- `onDestroyView()`에서 Shimmer `stopShimmer()` 호출 (CPU/배터리 절약)

### DataStore
- `preferencesDataStore(name = "settings")` — 파일 위치: `data/data/com.choo.moviefinder/files/datastore/settings.preferences_pb`
- `DataStoreModule`에서 `@Singleton`으로 제공
- `PreferencesRepository` 인터페이스 → `PreferencesRepositoryImpl` 구현
- 테마 설정: `ThemeMode.LIGHT` / `DARK` / `SYSTEM` → `AppCompatDelegate.setDefaultNightMode()` 적용
- `MovieFinderApp.onCreate()`에서 `runBlocking`으로 초기 테마 동기 적용 (깜빡임 방지)
- `ProcessLifecycleOwner.lifecycleScope`로 이후 테마 변경 실시간 반영

### Certificate Pinning
- `NetworkModule.kt`에서 `CertificatePinner` 설정
- `api.themoviedb.org`: leaf + intermediate SHA-256 핀 (API용 OkHttpClient)
- `image.tmdb.org`: leaf + intermediate SHA-256 핀 (`@ImageOkHttpClient` → Coil `OkHttpNetworkFetcherFactory`에 주입)
- 인증서 갱신 시 핀 업데이트 필요 (openssl 명령으로 새 해시 획득)

### Baseline Profiles
- `:baselineprofile` 모듈 (`com.android.test` 타입, minSdk 28)
- `profileinstaller` 라이브러리가 앱에 포함되어 Play Store 설치 시 자동 최적화
- 프로필 생성: 에뮬레이터/실기기 연결 후 `./gradlew :baselineprofile:connectedBenchmarkAndroidTest` 실행
- Benchmark 플러그인 버전 `1.5.0-alpha02` (AGP 9.0.0 호환)

### 네트워크 상태 모니터링
- `NetworkMonitor` (core/util): `ConnectivityManager.registerDefaultNetworkCallback()` 기반
- `isConnected: StateFlow<Boolean>` 노출 → `MainActivity`에서 `repeatOnLifecycle` 수집
- 오프라인 시 indefinite Snackbar 표시, 온라인 복구 시 자동 dismiss
- `NetworkModule`에서 `@Singleton`으로 제공
- `ACCESS_NETWORK_STATE` 권한 필요 (AndroidManifest.xml)

### StrictMode (디버그 전용)
- `StrictModeInitializer` (core/startup): App Startup `Initializer` 패턴
- **디버그 매니페스트에서만 등록** (`app/src/debug/AndroidManifest.xml`) — 릴리스 빌드 시 클래스 미로드
- **ThreadPolicy**: `detectDiskReads()`, `detectDiskWrites()`, `detectNetwork()` + `penaltyLog()`
- **VmPolicy**: `detectLeakedClosableObjects()`, `detectLeakedSqlLiteObjects()` + `penaltyLog()`
- `penaltyLog()` 전용 (앱 크래시 방지), Logcat에서 `StrictMode` 태그로 확인
- `TimberInitializer` 의존 (Timber 초기화 후 실행)

### Gradle Configuration Cache
- `gradle.properties`에 `org.gradle.configuration-cache=true` 설정
- 빌드 설정 단계를 캐시하여 반복 빌드 속도 개선

### ProGuard / R8
- `proguard-rules.pro`에 Retrofit, OkHttp, kotlinx.serialization, Room, Coil, Hilt, Paging, ViewModel, WorkManager, Widget 규칙 포함

## XML 레이아웃 구조
### 주요 레이아웃 파일
| 파일 | 용도 |
|---|---|
| `activity_main.xml` | NavHostFragment + BottomNavigationView |
| `fragment_home.xml` | Toolbar + TabLayout + RecyclerView + Shimmer |
| `fragment_search.xml` | Toolbar + SearchInput + YearFilter ChipGroup + RecentSearches + Results |
| `fragment_detail.xml` | CoordinatorLayout + CollapsingToolbar + NestedScrollView + FAB |
| `fragment_favorite.xml` | Toolbar + TabLayout + RecyclerView + EmptyState |
| `fragment_settings.xml` | Toolbar + NestedScrollView + 설정 항목 목록 |
| `item_movie_grid.xml` | 그리드용 영화 카드 |
| `item_movie_horizontal.xml` | 가로 스크롤용 영화 카드 |
| `item_cast.xml` | 출연진 아이템 |
| `item_recent_search.xml` | 최근 검색어 아이템 |
| `item_load_state.xml` | Paging footer (로딩/에러) |
| `layout_shimmer_grid.xml` | Shimmer placeholder 그리드 |
| `layout_error.xml` | 에러 뷰 (include용) |
| `layout_empty_state.xml` | 빈 상태 뷰 (include용) |
| `item_review.xml` | 리뷰 아이템 (확장/축소) |
| `widget_popular_movies.xml` | 위젯 레이아웃 (인기 영화 목록) |
| `item_movie_list.xml` | 리스트 뷰용 영화 카드 (포스터 + 제목 + 줄거리 + 평점) |
| `widget_movie_item.xml` | 위젯 영화 아이템 |
| `menu_search.xml` | 검색 화면 툴바 메뉴 (보기 모드 토글) |
| `menu_detail.xml` | 상세 화면 툴바 메뉴 (공유) |

## QA 완료 사항
- [x] Navigation: Safe Args 기반 타입 안전 네비게이션
- [x] API Key: OkHttp Interceptor로 중앙 관리
- [x] Clean Architecture: Presentation → Data 직접 의존 제거
- [x] DB Migration: fallbackToDestructiveMigration 적용
- [x] Locale: CircularRatingView에서 Locale.US 사용
- [x] Genre Chips: ChipGroup으로 동적 추가
- [x] toggleFavorite: @Transaction 원자적 처리 (abstract DAO) + try-catch 에러 처리 + Channel Snackbar 피드백
- [x] DAO providers: @Singleton 스코프 추가
- [x] ViewBinding: _binding null 처리로 메모리 누수 방지
- [x] repeatOnLifecycle: 생명주기 안전한 Flow 수집
- [x] 부분 API 실패 처리: credits/similar 실패 시 graceful degradation
- [x] 에러 메시지 로컬라이징: ErrorMessageProvider로 한국어 메시지 매핑
- [x] 오프라인 지원: RemoteMediator + Room 캐시
- [x] 딥링크: 커스텀 스킴 + TMDB 웹 URL
- [x] 스와이프 삭제: 즐겨찾기 ItemTouchHelper + Undo
- [x] 검색 필터: 개봉 연도 필터 (TMDB API year 파라미터)
- [x] Coil 캐시: 메모리 25% + 디스크 5% 설정
- [x] Room 인덱스: 자주 쿼리되는 컬럼에 인덱스 추가
- [x] LoggingInterceptor: 릴리스 빌드에서 객체 미생성
- [x] 유닛 테스트: 145개 (ViewModel 76개 + Repository 36개 + Paging 19개 + ErrorMessageProvider 10개 + PreferencesRepository 4개)
- [x] Espresso UI 테스트: 5개 (네비게이션 + 화면 표시 검증, HiltTestRunner)
- [x] 접근성 강화: 영화 카드 contentDescription, 리뷰 stateDescription, 등급 배지 접근성, ProgressBar/overlay 접근성
- [x] 다크 모드 아이콘: `@color/icon_default` + `values-night/colors.xml` 테마 대응
- [x] Shared Element Transition: 포스터 이미지 공유 전환 (postponeEnterTransition 패턴)
- [x] YouTube 예고편 재생: YouTube 앱/웹 브라우저 연결 (Intent.ACTION_VIEW)
- [x] Certificate Pinning: OkHttp CertificatePinner (api.themoviedb.org + image.tmdb.org leaf + intermediate 핀)
- [x] DataStore: 다크모드 설정 저장 (Preferences DataStore)
- [x] 테마 전환 UI: SettingsFragment MaterialAlertDialog (라이트/다크/시스템)
- [x] Baseline Profiles: ProfileInstaller + 생성 모듈 구성
- [x] GitHub Actions CI/CD: Lint + Build + Test 자동화 워크플로우
- [x] Detekt + KtLint: 정적 분석 (Detekt 2.0.0-alpha.2 + KtLint 규칙)
- [x] JaCoCo: 테스트 코드 커버리지 리포트 (HTML + XML)
- [x] App Startup: Timber 초기화 최적화 (TimberInitializer)
- [x] LeakCanary: 디버그 빌드 메모리 누수 감지
- [x] Timber: 구조화 로깅 (OkHttp 로깅 통합)
- [x] 네트워크 상태 모니터링: ConnectivityManager + StateFlow + Snackbar (오프라인 감지)
- [x] StrictMode: 디버그 전용 디스크/네트워크 접근 감지 (App Startup)
- [x] Gradle Configuration Cache: 빌드 속도 최적화
- [x] ErrorType 확장: SSL, PARSE 에러 타입 추가 (SSLException, SerializationException, IOException 매핑)
- [x] Channel 기반 일회성 이벤트: DetailViewModel Snackbar 이벤트 유실 방지 (SharedFlow → Channel)
- [x] 검색 즉시 실행: merge(debounced, immediate) 패턴으로 검색 버튼 즉시 반응
- [x] CircularRatingView 접근성: contentDescription으로 평점 값 음성 출력
- [x] postponeEnterTransition 타임아웃: 500ms (이미지 로딩 실패 시 무한 대기 방지)
- [x] 이미지 Certificate Pinning: Coil에 @ImageOkHttpClient 주입 (image.tmdb.org)
- [x] 어댑터 코드 중복 제거: MovieGridViewHolder 공유 ViewHolder 추출, HorizontalMovieAdapter 통합 (SimilarMovieAdapter + WatchHistoryAdapter)
- [x] PagingConfig 중복 제거: DEFAULT_PAGING_CONFIG 상수 추출 (4곳 → 1곳)
- [x] GridLayoutManager 중복 제거: createMovieGridLayoutManager 유틸 함수 추출 (Home + Search)
- [x] DetailFragment bindExtendedInfo 간략화: bindOptionalField 헬퍼 추출
- [x] Shimmer 생명주기: onDestroyView()에서 stopShimmer() 호출
- [x] Coil 이미지 해제: 모든 이미지 어댑터 onViewRecycled()에서 dispose() 호출
- [x] 스와이프 안정성: NO_POSITION 체크 + getOrNull 안전 접근
- [x] 상세 화면 빈 상태: 에러 시 출연진/비슷한 영화 어댑터 초기화
- [x] 접근성 레이블: 에러 아이콘, Shimmer, ProgressBar contentDescription 추가
- [x] 하드코딩 치수 제거: @dimen 리소스로 통합 (icon_size_small, icon_size_error 등)
- [x] 연도 필터 캐싱: lazy 프로퍼티로 배열 재생성 방지
- [x] PagingConfig 최적화: prefetchDistance=5, initialLoadSize=PAGE_SIZE
- [x] 캐시 만료: RemoteMediator initialize() 1시간 캐시 만료 (RemoteKeyEntity.lastUpdated)
- [x] Detail self-navigation: launchSingleTop으로 백스택 중복 방지
- [x] ProGuard 규칙 보강: Hilt, Paging, ViewModel, WorkManager 규칙 추가
- [x] 영화 공유: Toolbar 메뉴 + Intent.ACTION_SEND
- [x] 확장 상세 정보: 제작비/수익/원어/상태/IMDb 링크 (MovieDetail 필드 확장)
- [x] 콘텐츠 등급 배지: release_dates API (KR→US 폴백), Chip 표시
- [x] 시청 기록: WatchHistoryEntity + Room DB v6 (v8에서 overview/releaseDate/voteCount 필드 확장), 상세 화면 진입 시 자동 저장, 홈 화면 가로 스크롤
- [x] 워치리스트: WatchlistEntity + Room DB v7, @Transaction toggleWatchlist, FAB + FavoriteFragment 탭
- [x] 고급 검색 필터: 장르 다중 선택 (genre/movie/list API), 정렬 (4가지), Discover API 연동
- [x] 설정 화면: 테마/캐시/시청기록 관리, BottomNav 4번째 탭, HomeFragment 테마 메뉴 통합
- [x] Material 애니메이션: RecyclerView layoutAnimation (staggered slide-up), FAB bounce 효과
- [x] FAB 애니메이션 스태킹 수정: animate().cancel() + scale 리셋으로 연타 시 겹침 방지
- [x] 장르 칩 오버플로우 수정: 3개 이상 선택 시 "장르 N개" 카운트 표시
- [x] 장르 로드 실패 피드백: Snackbar + retryLoadGenres() 재시도
- [x] SettingsViewModel 에러 처리: setThemeMode/clearWatchHistory try-catch + CancellationException rethrow
- [x] 캐시 삭제 개선: Coil 메모리 + 디스크 캐시 동시 클리어
- [x] FavoriteFragment 탭 전환: submitList(emptyList())로 stale data 방지
- [x] 딥링크 대소문자 수정: TMDB host 비교 시 ignoreCase 적용
- [x] WATCH_HISTORY_LIMIT 상수 추출: WatchHistoryDao 파라미터화
- [x] Import 순서 정렬: DetailFragment, SearchFragment (android→androidx→java→3rd party)
- [x] OkHttp 5.x 업그레이드: kotlin.time.Duration API, toResponseBody() 마이그레이션
- [x] Retrofit 3.x 업그레이드: 바이너리 호환, retrofit2 패키지 유지
- [x] ProGuard 규칙 업데이트: OkHttp 5.x/Retrofit 3.x consumer rules 기반, 과도한 keep 규칙 정리
- [x] Gradle 빌드 최적화: parallel=true, caching=true, shrinkResources, Version Catalog bundles
- [x] 홈 화면 Pull-to-Refresh: SwipeRefreshLayout, LoadState 기반 초기 로딩/새로고침 분리
- [x] 즐겨찾기 정렬 옵션: FavoriteSortOrder enum (추가 날짜순/제목순/평점순), Toolbar 메뉴 다이얼로그
- [x] 즐겨찾기 에러 피드백: Channel → Snackbar (DetailViewModel과 동일 패턴)
- [x] SearchViewModel 필터 SavedStateHandle 복원: genres (IntArray), sortBy (String name)
- [x] Discover 모드 표시: 검색어 없이 필터 탐색 시 Chip 표시
- [x] 검색 자동완성: 입력 중 최근 검색어 필터링 제안
- [x] 검색어 trim 처리: onSearch 시 공백 제거
- [x] DetailViewModel Mutex 로딩 가드: @Volatile isLoading → Mutex.tryLock() 원자적 처리
- [x] 장르 칩 복원 개선: API 미로드 시 개수 표시, 로드 완료 시 이름 갱신
- [x] FavoriteFragment 탭 전환 스크롤 초기화: scrollToPosition(0)으로 이전 탭 스크롤 위치 제거
- [x] FavoriteFragment 다이얼로그 생명주기: activeDialog 추가, onDestroyView() dismiss
- [x] FavoriteFragment SwipeHelper 분리: onDestroyView()에서 attachToRecyclerView(null)
- [x] FavoriteFragment 정렬 상태 표시: Toolbar subtitle로 현재 정렬 옵션 시각적 피드백
- [x] SearchFragment 키보드 자동 숨김: RecyclerView 스크롤 시 InputMethodManager로 키보드 닫기
- [x] SettingsFragment 버전 문자열 포맷: getString(format, arg) 방식으로 변경
- [x] SettingsViewModel 삭제 이벤트 분리: Channel 기반 성공/에러 분리 (watchHistoryCleared + snackbarEvent)
- [x] WatchHistoryEntity 데이터 보존: overview/releaseDate/voteCount 필드 추가 (Room DB v8)
- [x] 어댑터 null 안전성: MovieAdapter/HorizontalMovieAdapter getItem()?.let 패턴 적용
- [x] Shared Element Transition 이미지 연동: Coil listener(onSuccess, onError)로 이미지 로드 완료 후 전환 시작
- [x] 영화 리뷰 표시: TMDB reviews API + ReviewAdapter (클릭 시 확장/축소), DetailUiState.Success에 reviews 필드 추가
- [x] 맨 위로 FAB: HomeFragment/SearchFragment 스크롤 시 mini FAB 표시 (canScrollVertically 기반)
- [x] 검색 추천: 결과 없을 때 추천 검색어 칩 (마블, 스파이더맨, 배트맨, 스타워즈, 해리포터)
- [x] 홈 화면 위젯: PopularMoviesWidget + RemoteViewsService/Factory (인기 영화 Top 10, 1시간 갱신, 딥링크 클릭)
- [x] 개봉일 알림: WorkManager + ReleaseNotificationScheduler/Worker (워치리스트 추가 시 예약, 삭제 시 취소)
- [x] 다국어 지원: values-en/strings.xml 영어 번역 (161개 문자열)
- [x] DetailViewModel 부분 실패 리팩토링: loadOptional/loadOptionalNullable 헬퍼 추출
- [x] 사용자 영화 평점: RatingBar (0.5~5.0 별점), Room DB UserRatingEntity 저장, 삭제 버튼
- [x] 검색 결과 보기 모드 전환: 그리드 ↔ 리스트 토글 (Toolbar 메뉴, SavedStateHandle 저장)
- [x] Predictive Back Gesture: `android:enableOnBackInvokedCallback="true"` (Android 14+ 뒤로가기 미리보기)
- [x] kotlinx-datetime: Calendar/SimpleDateFormat → kotlinx.datetime.Clock, LocalDate, TimeZone (멀티플랫폼)
- [x] Typed DataStore: Preferences DataStore → `DataStore<UserSettings>` (kotlinx.serialization JSON, 타입 안전)
- [x] Per-App Language: `locales_config.xml` + `AppCompatDelegate.setApplicationLocales()` (시스템 설정 연동)
- [x] Gradle Convention Plugins: `buildSrc/AndroidConfig` 공유 상수 (compileSdk, minSdk, targetSdk, Java 버전)
- [x] R8 Full Mode: `android.enableR8.fullMode=true` (최적화 강화, ProGuard 규칙 보강)
- [x] 검색어 저장 trim 순서 수정: `trim()` 후 `isNotBlank()` 검증 (공백만 있는 쿼리 방어)
- [x] 딥링크 movieId 검증 강화: `movieId <= 0` 방어 추가 (음수/0 ID 네비게이션 차단)
- [x] 캐시 삭제 안정성: `try-finally`로 memoryCache 실패해도 diskCache 클리어 보장
- [x] DetailViewModel 보일러플레이트 제거: `launchWithSnackbar` 헬퍼 추출 (4개 함수 중복 try-catch 통합)
- [x] 위젯 리소스 누수 수정: OkHttp `response.use {}` 패턴으로 Response 자동 닫힘 보장

## 보너스 기능 구현 현황
- [x] 다크 모드 지원 (MaterialComponents.DayNight 테마 + 테마 대응 아이콘/색상)
- [x] 최근 검색어 저장 및 표시
- [x] 애니메이션 전환 효과 (Navigation anim + Shared Element Transition)
- [x] Splash Screen API
- [x] CollapsingToolbarLayout 패럴랙스 효과
- [x] Facebook Shimmer 로딩 애니메이션
- [x] 오프라인 캐시 지원 (RemoteMediator)
- [x] 딥링크 지원 (커스텀 스킴 + TMDB URL)
- [x] 스와이프 삭제 + 실행취소
- [x] 검색 연도 필터
- [x] Unit Test 작성 (145개: ViewModel 76개 + Repository 36개 + Paging 19개 + ErrorMessageProvider 10개 + PreferencesRepository 4개)
- [x] 영화 예고편 재생 (YouTube 앱/웹 연결, TMDB Videos API 연동)
- [x] Shared Element Transition (포스터 이미지 공유 전환)
- [x] DataStore 기반 다크모드 설정 (라이트/다크/시스템 전환)
- [x] OkHttp Certificate Pinning (TMDB API + 이미지 서버 보안 강화)
- [x] Baseline Profiles (앱 시작 성능 최적화)
- [x] GitHub Actions CI/CD (Detekt + Lint + Build + Test 자동화)
- [x] Detekt + KtLint 정적 분석 (Kotlin 코드 품질 검사)
- [x] JaCoCo 테스트 커버리지 리포트
- [x] App Startup 초기화 최적화 (Timber)
- [x] LeakCanary 메모리 누수 감지 (디버그 전용)
- [x] Timber 구조화 로깅
- [x] 네트워크 상태 모니터링 (ConnectivityManager 콜백 + 오프라인 Snackbar)
- [x] StrictMode 디버그 감지 (App Startup, penaltyLog)
- [x] Gradle Configuration Cache (빌드 속도 최적화)
- [x] RemoteMediator 캐시 만료 (1시간 자동 새로고침)
- [x] PagingConfig 최적화 (prefetchDistance, initialLoadSize 튜닝)
- [x] 어댑터 이미지 메모리 관리 (onViewRecycled + Coil dispose)
- [x] 접근성 개선 (CircularRatingView, 에러/로딩 상태 contentDescription)
- [x] Channel 기반 일회성 UI 이벤트 (Snackbar 이벤트 유실 방지)
- [x] 검색 즉시 실행 (merge 패턴: debounced + immediate flow)
- [x] 홈 화면 탭 상태 저장 (savedInstanceState로 화면 회전 시 복원)
- [x] DetailViewModel 중복 호출 방지 (isLoading 플래그)
- [x] 재시도 버튼 debounce (로딩 중 비활성화)
- [x] 부분 API 실패 Timber 로깅 (credits/similar/trailer)
- [x] CircularRatingView draw 캐싱 (onSizeChanged)
- [x] 연도 필터 현재 선택 표시 (setSingleChoiceItems)
- [x] 다이얼로그 생명주기 관리 (HomeFragment/SearchFragment onDestroyView dismiss)
- [x] RecentSearchEntity 타임스탬프 인덱스 (ORDER BY 최적화)
- [x] HorizontalMovieAdapter Shared Element (transitionName 설정, transitionPrefix 파라미터)
- [x] SearchViewModel SavedStateHandle (프로세스 사망 시 검색어/연도 복원)
- [x] ImageOkHttpClient writeTimeout (30초)
- [x] 하드코딩 마진 제거 (rating_view_overlap_margin dimen)
- [x] CI/CD Artifact 강화 (JaCoCo + Detekt + APK 업로드)
- [x] Repository 입력 검증 (require guards: movieId > 0, query.isNotBlank)
- [x] 출연진 정렬 (order 필드 기준 오름차순)
- [x] FavoriteViewModel 에러 처리 (toggleFavorite try-catch + Timber 로깅)
- [x] 영화 공유 (Intent.ACTION_SEND, Toolbar 메뉴)
- [x] 확장 상세 정보 (제작비/수익/원어/상태/IMDb 링크)
- [x] 콘텐츠 등급 배지 (release_dates API, KR→US 폴백)
- [x] 시청 기록 (WatchHistoryEntity, Room DB, 홈 화면 가로 스크롤)
- [x] 워치리스트 (WatchlistEntity, @Transaction toggleWatchlist, FAB + FavoriteFragment 탭)
- [x] 고급 검색 필터 (장르 다중 선택, 정렬, Discover API)
- [x] 설정 화면 (테마/캐시/시청기록 관리, BottomNav 4번째 탭)
- [x] Material 애니메이션 (RecyclerView layoutAnimation, FAB bounce)
- [x] OkHttp 5.x + Retrofit 3.x 업그레이드 (kotlin.time.Duration timeout API)
- [x] 홈 화면 Pull-to-Refresh (SwipeRefreshLayout, LoadState 연동)
- [x] 즐겨찾기/워치리스트 정렬 옵션 (추가 날짜순/제목순/평점순)
- [x] 즐겨찾기 에러 Snackbar 피드백 (Channel 기반 일회성 이벤트)
- [x] SearchViewModel 필터 SavedStateHandle 전체 복원 (query/year/genres/sort)
- [x] Discover 모드 Chip 표시 (검색어 없이 필터 탐색 시 시각적 피드백)
- [x] 검색 자동완성 (입력 중 최근 검색어 필터링 제안)
- [x] Gradle 빌드 최적화 (parallel, caching, shrinkResources, Version Catalog bundles)
- [x] ProGuard 규칙 현대화 (OkHttp 5.x/Retrofit 3.x consumer rules 기반)
- [x] 영화 리뷰 표시 (TMDB reviews API, ReviewAdapter 확장/축소)
- [x] 맨 위로 FAB (홈/검색 화면, mini FAB, canScrollVertically 기반)
- [x] 검색 추천 (결과 없을 때 추천 검색어 칩)
- [x] 홈 화면 위젯 (AppWidgetProvider, RemoteViewsService, 인기 영화 Top 10)
- [x] 개봉일 알림 (WorkManager, 워치리스트 연동, NotificationChannel)
- [x] 다국어 지원 (values-en/strings.xml 영어 번역 165개)
- [x] Espresso UI 테스트 (HiltTestRunner + 네비게이션/화면 검증 5개)
- [x] PagingSource/RemoteMediator 유닛 테스트 (19개: MoviePagingSource 7 + DiscoverPagingSource 5 + MovieRemoteMediator 7)
- [x] 접근성 강화 (영화 카드 contentDescription, 리뷰 stateDescription, 등급 배지, ProgressBar, decorative overlay)
- [x] 사용자 영화 평점 (RatingBar 0.5~5.0, Room DB UserRatingEntity, 삭제 버튼)
- [x] 검색 결과 보기 모드 전환 (그리드 ↔ 리스트 토글, MovieListViewHolder, SavedStateHandle 저장)
- [x] Predictive Back Gesture (Android 14+ 뒤로가기 미리보기 애니메이션)
- [x] kotlinx-datetime (Calendar/SimpleDateFormat 대체, 멀티플랫폼 날짜/시간 API)
- [x] Typed DataStore (Preferences → kotlinx.serialization JSON 기반 타입 안전 저장)
- [x] Per-App Language (locales_config.xml, AppCompatDelegate.setApplicationLocales)
- [x] Gradle Convention Plugins (buildSrc/AndroidConfig 공유 빌드 상수)
- [x] R8 Full Mode (최적화 강화, ProGuard 규칙 보강)
- [x] 검색어 저장 trim 순서 수정 (공백 쿼리 방어)
- [x] 딥링크 movieId 검증 강화 (음수/0 ID 차단)
- [x] 캐시 삭제 안정성 (try-finally memoryCache/diskCache 보장)
- [x] DetailViewModel launchWithSnackbar 헬퍼 (중복 try-catch 4곳 통합)
- [x] 위젯 OkHttp Response 리소스 누수 수정 (response.use 패턴)
