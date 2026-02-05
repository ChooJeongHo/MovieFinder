# MovieFinder - 영화 검색 안드로이드 앱

## 프로젝트 개요
TMDB (The Movie Database) API를 활용한 영화 검색, 상세 정보 조회, 즐겨찾기 기능을 갖춘 안드로이드 앱입니다.

## 기술 스택

### 빌드 환경
- **AGP**: 9.0.0 (Android Gradle Plugin, Kotlin 2.2.10 내장)
- **Gradle**: 9.1.0
- **compileSdk**: 36 (Android 15)
- **minSdk**: 24 / **targetSdk**: 36
- **KSP**: 2.2.10-2.0.2 (Kotlin Symbol Processing)

### 핵심 라이브러리
| 라이브러리 | 버전 | 용도 |
|---|---|---|
| AppCompat | 1.7.0 | Android 하위 호환성 |
| Material Components | 1.12.0 | Material Design XML 컴포넌트 |
| Navigation (Fragment) | 2.9.7 | Safe Args 기반 Fragment 네비게이션 |
| Fragment KTX | 1.8.6 | Fragment 확장 함수 |
| ConstraintLayout | 2.2.1 | 유연한 레이아웃 |
| RecyclerView | 1.4.0 | 리스트/그리드 표시 |
| Hilt | 2.59.1 | 의존성 주입 (DI) |
| Room | 2.8.4 | 로컬 SQLite 데이터베이스 |
| Room Paging | 2.8.4 | Room + Paging 3 통합 (RemoteMediator) |
| Paging 3 | 3.3.6 | 무한 스크롤 페이징 |
| Retrofit 2 | 2.11.0 | REST API 통신 |
| OkHttp | 4.12.0 | HTTP 클라이언트 + 로깅 |
| kotlinx.serialization | 1.7.3 | JSON 직렬화/역직렬화 |
| Coil 3 | 3.3.0 | 이미지 로딩 (View 버전, 메모리+디스크 캐시) |
| Facebook Shimmer | 0.5.0 | 로딩 Shimmer 애니메이션 |
| Lifecycle | 2.10.0 | 생명주기 인식 컴포넌트 |
| android-youtube-player | 12.1.1 | YouTube IFrame Player (인앱 예고편 재생) |
| Splash Screen API | 1.0.1 | 스플래시 화면 |
| SwipeRefreshLayout | 1.1.0 | 당겨서 새로고침 |

### 테스트 라이브러리
| 라이브러리 | 버전 | 용도 |
|---|---|---|
| MockK | 1.13.16 | Kotlin 모킹 프레임워크 |
| Turbine | 1.2.0 | Flow 테스트 유틸리티 |
| Coroutines Test | 1.10.1 | 코루틴 테스트 디스패처 |
| Paging Testing | 3.3.6 | PagingData 테스트 유틸리티 |
| JUnit 4 | 4.13.2 | 유닛 테스트 프레임워크 |

## 아키텍처

### Clean Architecture + MVVM
```
app/src/main/java/com/choo/moviefinder/
├── core/                  # 공유 유틸리티
│   └── util/
│       ├── ImageUrlProvider.kt    # 이미지 URL 빌더
│       └── ErrorMessageProvider.kt # 예외 → 사용자 메시지 매핑
├── data/                  # 데이터 레이어
│   ├── local/
│   │   ├── MovieDatabase.kt       # Room DB (version 3)
│   │   ├── dao/                   # DAO 인터페이스 (4개)
│   │   │   ├── FavoriteMovieDao.kt
│   │   │   ├── RecentSearchDao.kt
│   │   │   ├── CachedMovieDao.kt  # 오프라인 캐시 (PagingSource 반환)
│   │   │   └── RemoteKeyDao.kt    # 페이징 키 관리
│   │   └── entity/                # Room Entity (4개)
│   │       ├── FavoriteMovieEntity.kt  # 인덱스: addedAt
│   │       ├── RecentSearchEntity.kt
│   │       ├── CachedMovieEntity.kt    # 인덱스: category, 복합PK: id+category
│   │       └── RemoteKeyEntity.kt
│   ├── paging/
│   │   ├── MoviePagingSource.kt       # 네트워크 전용 PagingSource (검색용)
│   │   └── MovieRemoteMediator.kt     # 오프라인 지원 RemoteMediator (홈 화면)
│   ├── remote/            # Retrofit API (Service, DTO)
│   ├── repository/        # Repository 구현체
│   └── util/              # 상수 정의 (PAGE_SIZE)
├── di/                    # Hilt DI 모듈
│   ├── DatabaseModule.kt  # Room DB + DAO 제공 (destructive migration fallback)
│   ├── NetworkModule.kt   # Retrofit/OkHttp 제공 (API key interceptor)
│   └── RepositoryModule.kt # Repository 바인딩
├── domain/                # 도메인 레이어
│   ├── model/             # 도메인 모델 (Movie, MovieDetail, Cast)
│   ├── repository/        # Repository 인터페이스
│   └── usecase/           # UseCase 클래스 (13개)
├── presentation/          # 프레젠테이션 레이어
│   ├── adapter/           # RecyclerView 어댑터 (6개)
│   ├── common/            # CircularRatingView (커스텀 뷰)
│   ├── detail/            # 영화 상세 화면 (DetailFragment, DetailViewModel, TrailerDialogFragment)
│   ├── favorite/          # 즐겨찾기 화면 (FavoriteFragment, FavoriteViewModel)
│   ├── home/              # 홈 화면 (HomeFragment, HomeViewModel)
│   └── search/            # 검색 화면 (SearchFragment, SearchViewModel)
├── MainActivity.kt        # 진입점 (AppCompatActivity + NavHostFragment + 딥링크 처리)
└── MovieFinderApp.kt      # Application 클래스 (@HiltAndroidApp, Coil 캐시 설정)
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
- TabLayout으로 "현재 상영작" / "인기 영화" 전환
- Paging 3 무한 스크롤 (페이지당 20개)
- **오프라인 지원**: RemoteMediator + Room 캐시 (네트워크 오류 시 캐시 데이터 표시)
- Facebook Shimmer 로딩 애니메이션
- GridLayoutManager (spanCount=2)
- MoviePagingAdapter + MovieLoadStateAdapter (withLoadStateFooter)

### 2. 검색 화면 (SearchFragment)
- 300ms debounce 적용 검색
- Flow combine(query, year) → debounce → distinctUntilChanged → flatMapLatest
- TextInputLayout + TextInputEditText (둥근 모서리)
- **연도 필터**: ChipGroup + MaterialAlertDialog로 개봉 연도 필터링 (TMDB API `year` 파라미터)
- 최근 검색어 Room DB 저장/삭제/전체삭제 (RecentSearchAdapter)
- 상태별 UI 전환 (초기/검색중/결과없음/결과표시)

### 3. 영화 상세 화면 (DetailFragment)
- CoordinatorLayout + CollapsingToolbarLayout (배경 이미지 패럴랙스)
- 배경 이미지, 제목, 평점, 개봉일, 런타임, 태그라인
- ChipGroup으로 장르 칩 동적 추가
- 출연진 (Cast) 가로 스크롤 RecyclerView (CastAdapter)
- 비슷한 영화 추천 가로 스크롤 RecyclerView (SimilarMovieAdapter)
- FAB으로 즐겨찾기 토글
- **영화 예고편 재생**: TMDB Videos API로 YouTube 키 획득 → TrailerDialogFragment에서 인앱 재생 (android-youtube-player 라이브러리, API 키 불필요)
- **Shared Element Transition**: 홈/검색/즐겨찾기 → 상세 화면 포스터 이미지 공유 전환 (ChangeBounds + ChangeTransform + ChangeImageTransform)
- 상세/출연진/비슷한 영화/예고편 4개 API를 async로 병렬 호출
- **부분 실패 처리**: 출연진/비슷한 영화/예고편 API 실패 시 빈 리스트/null로 대체 (상세 정보는 유지)
- **즐겨찾기 토글 실패 피드백**: SharedFlow → Snackbar로 에러 메시지 표시
- NestedScrollView 기반 스크롤

### 4. 즐겨찾기 화면 (FavoriteFragment)
- Room DB 기반 오프라인 조회
- Flow를 통한 실시간 업데이트 (repeatOnLifecycle)
- **스와이프 삭제**: ItemTouchHelper 왼쪽 스와이프로 삭제 + Snackbar 실행취소 (Undo)
- 빈 상태 UI (EmptyState layout include)
- MovieAdapter (ListAdapter) 사용

### 5. 딥링크 지원
- **커스텀 스킴**: `moviefinder://movie/{movieId}` (Navigation Component 자동 처리)
- **TMDB 웹 URL**: `https://www.themoviedb.org/movie/{movieId}` (MainActivity에서 수동 파싱)
- `onNewIntent()` 처리로 기존 Activity에서도 딥링크 수신

### 6. 에러 처리
- **ErrorMessageProvider**: 예외 타입별 한국어 사용자 메시지 매핑
  - `UnknownHostException` / `ConnectException` → 네트워크 연결 오류
  - `SocketTimeoutException` → 서버 응답 지연
  - `HttpException` → 서버 오류
  - 기타 → 알 수 없는 오류
- 네트워크 오류 시 재시도 버튼 (layout_error.xml include)
- Shimmer 로딩 애니메이션
- Empty State UI 구현 (layout_empty_state.xml include)
- Paging append 에러 처리 (MovieLoadStateAdapter)

## RecyclerView 어댑터 (6개)
| 어댑터 | 부모 클래스 | 용도 |
|---|---|---|
| `MoviePagingAdapter` | `PagingDataAdapter` | 홈/검색 그리드 (무한 스크롤) |
| `MovieAdapter` | `ListAdapter` | 즐겨찾기 목록 |
| `CastAdapter` | `ListAdapter` | 상세 화면 출연진 |
| `SimilarMovieAdapter` | `ListAdapter` | 상세 화면 비슷한 영화 |
| `RecentSearchAdapter` | `ListAdapter` | 검색 화면 최근 검색어 |
| `MovieLoadStateAdapter` | `LoadStateAdapter` | 페이징 로딩/에러 footer |

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

### 사용 엔드포인트
| 엔드포인트 | 용도 |
|---|---|
| GET /movie/now_playing | 현재 상영작 (RemoteMediator 페이징) |
| GET /movie/popular | 인기 영화 (RemoteMediator 페이징) |
| GET /search/movie | 영화 검색 (네트워크 페이징, year 필터 지원) |
| GET /movie/{id} | 영화 상세 정보 |
| GET /movie/{id}/credits | 출연진 정보 |
| GET /movie/{id}/similar | 비슷한 영화 |
| GET /movie/{id}/videos | 예고편 영상 (YouTube 키) |

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
| `RemoteKeyEntity` | 페이징 키 추적 Entity (PK: category) |
| `CachedMovieDao` | Room PagingSource 반환, insertAll, clearByCategory |
| `RemoteKeyDao` | 원격 키 조회/저장/삭제 |
| `MovieRemoteMediator` | API → Room 캐시 동기화 (REFRESH/APPEND 처리) |

## 테스트

### 유닛 테스트 (42개)
```bash
./gradlew testDebugUnitTest
```

| 테스트 클래스 | 테스트 수 | 대상 |
|---|---|---|
| `MovieRepositoryImplTest` | 20 | 영화 상세, 출연진, 비슷한 영화, 예고편 키(5개), 즐겨찾기 토글/조회, 검색 기록 CRUD |
| `DetailViewModelTest` | 9 | 초기 상태, 에러, 부분 실패, 즐겨찾기 토글, Snackbar, 재시도 |
| `SearchViewModelTest` | 7 | 검색어 변경, 검색 저장, 빈 검색어, 삭제, 전체 삭제, 최근 검색어 |
| `HomeViewModelTest` | 3 | UseCase 호출 검증 (nowPlaying, popular, 동시 호출) |
| `FavoriteViewModelTest` | 3 | 즐겨찾기 목록, 빈 목록, 삭제 |

### 테스트 패턴
- `MockK`: UseCase/Repository/DAO/API 모킹
- `Turbine`: StateFlow/SharedFlow 수집 및 검증
- `StandardTestDispatcher` + `Dispatchers.setMain()`: 코루틴 테스트
- `coEvery`/`coVerify`: suspend 함수 모킹 및 검증

## 빌드 및 실행

### 사전 요구사항
- Android Studio (AGP 9.0.0 지원 버전)
- JDK 17+
- TMDB API Key

### 빌드 명령어
```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리스 빌드
./gradlew assembleRelease

# 유닛 테스트
./gradlew testDebugUnitTest
```

### 딥링크 테스트 (adb)
```bash
# 커스텀 스킴
adb shell am start -a android.intent.action.VIEW -d "moviefinder://movie/550"

# TMDB 웹 URL
adb shell am start -a android.intent.action.VIEW -d "https://www.themoviedb.org/movie/550"
```

## 주의사항

### AGP 9.0.0 특이사항
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

### Room DB
- 스키마 export 위치: `app/schemas/`
- Entity: FavoriteMovieEntity, RecentSearchEntity, CachedMovieEntity, RemoteKeyEntity
- 데이터베이스 이름: `movie_finder_db`
- 데이터베이스 버전: 3
- Destructive migration fallback 적용 (개발 환경)
- 인덱스: CachedMovieEntity(category), FavoriteMovieEntity(addedAt)

### 이미지 URL 관리
- `ImageUrlProvider` (core/util)를 통해 이미지 URL 생성
- Presentation 레이어에서 BuildConfig/Constants 직접 참조하지 않음
- posterUrl(), backdropUrl(), profileUrl() 헬퍼 함수 제공

### Coil 이미지 캐시
- 메모리 캐시: 앱 메모리의 25% (`MemoryCache.Builder().maxSizePercent(0.25)`)
- 디스크 캐시: 디스크의 5% (`DiskCache.Builder().maxSizePercent(0.05)`)
- 캐시 디렉토리: `cacheDir/image_cache`
- `MovieFinderApp`에서 `SingletonImageLoader.Factory` 구현

### 다크 모드
- `Theme.MaterialComponents.DayNight.NoActionBar` 기반
- `values-night/colors.xml`에 다크 모드 전용 색상 정의 (`icon_default`, `backdrop_overlay`)
- 11개 아이콘 drawable: `android:fillColor="@color/icon_default"` (라이트: 검정, 다크: 흰색)
- `fragment_detail.xml`: 배경 오버레이 `@color/backdrop_overlay`, FAB 아이콘 `?attr/colorOnPrimary`

### Shared Element Transition
- 홈/검색/즐겨찾기 → 상세 화면 포스터 이미지 공유 전환
- `FragmentNavigatorExtras(posterView to "poster_$movieId")` 사용
- DetailFragment `onCreate()`에서 `sharedElementEnterTransition` + `sharedElementReturnTransition` 설정 (TransitionSet: ChangeBounds + ChangeTransform + ChangeImageTransform)
- `postponeEnterTransition()` + `doOnPreDraw { startPostponedEnterTransition() }` 패턴
- nav_graph.xml의 상세 화면 이동 action에서 enterAnim/exitAnim 제거 (애니메이션이 Shared Element 전환을 덮어쓰는 문제 방지)

### 영화 예고편 재생
- TMDB `/movie/{id}/videos` API로 YouTube 키 획득
- 우선순위: 공식 Trailer > 비공식 Trailer > YouTube 영상
- `TrailerDialogFragment`: `DialogFragment` + `android-youtube-player` 라이브러리
- YouTube IFrame Player API 사용 (API 키 불필요, TMDB에서 제공하는 video key만 사용)

### Fragment ViewBinding 패턴
- `_binding` nullable + `binding` non-null getter 패턴
- `onDestroyView()`에서 `_binding = null` 필수 (메모리 누수 방지)

### ProGuard / R8
- `proguard-rules.pro`에 Retrofit, OkHttp, kotlinx.serialization, Room, Coil 규칙 포함

## XML 레이아웃 구조
### 주요 레이아웃 파일
| 파일 | 용도 |
|---|---|
| `activity_main.xml` | NavHostFragment + BottomNavigationView |
| `fragment_home.xml` | Toolbar + TabLayout + RecyclerView + Shimmer |
| `fragment_search.xml` | Toolbar + SearchInput + YearFilter ChipGroup + RecentSearches + Results |
| `fragment_detail.xml` | CoordinatorLayout + CollapsingToolbar + NestedScrollView + FAB |
| `fragment_favorite.xml` | Toolbar + RecyclerView + EmptyState |
| `item_movie_grid.xml` | 그리드용 영화 카드 |
| `item_movie_horizontal.xml` | 가로 스크롤용 영화 카드 |
| `item_cast.xml` | 출연진 아이템 |
| `item_recent_search.xml` | 최근 검색어 아이템 |
| `item_load_state.xml` | Paging footer (로딩/에러) |
| `layout_shimmer_grid.xml` | Shimmer placeholder 그리드 |
| `layout_error.xml` | 에러 뷰 (include용) |
| `layout_empty_state.xml` | 빈 상태 뷰 (include용) |
| `dialog_trailer.xml` | YouTube 예고편 재생 다이얼로그 |

## QA 완료 사항
- [x] Navigation: Safe Args 기반 타입 안전 네비게이션
- [x] API Key: OkHttp Interceptor로 중앙 관리
- [x] Clean Architecture: Presentation → Data 직접 의존 제거
- [x] DB Migration: fallbackToDestructiveMigration 적용
- [x] Locale: CircularRatingView에서 Locale.US 사용
- [x] Genre Chips: ChipGroup으로 동적 추가
- [x] toggleFavorite: try-catch 에러 처리 + Snackbar 피드백
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
- [x] 유닛 테스트: 42개 (ViewModel 22개 + Repository 20개, MockK + Turbine)
- [x] 다크 모드 아이콘: `@color/icon_default` + `values-night/colors.xml` 테마 대응
- [x] Shared Element Transition: 포스터 이미지 공유 전환 (postponeEnterTransition 패턴)
- [x] YouTube 예고편 재생: TrailerDialogFragment + android-youtube-player (인앱 재생)

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
- [x] Unit Test 작성 (42개: ViewModel 22개 + Repository 20개)
- [x] 영화 예고편 재생 (android-youtube-player 인앱 재생, TMDB Videos API 연동)
- [x] Shared Element Transition (포스터 이미지 공유 전환)
