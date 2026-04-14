# MovieFinder

> TMDB API를 활용한 영화 검색 Android 앱

![CI](https://github.com/ChooJeongHo/MovieFinder/actions/workflows/android-ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)
![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?logo=kotlin&logoColor=white)
![AGP](https://img.shields.io/badge/AGP-9.1.0-green.svg)

Clean Architecture + MVVM 패턴 기반으로, 오프라인 캐시, 다크 모드, 페이징, 딥링크 등 실무 수준의 기능을 구현했습니다.

---

## Screenshots

| Home | Search (Grid) | Search (List) | Detail | Favorite | Settings |
|:----:|:-------------:|:-------------:|:------:|:--------:|:--------:|
| <img src="screenshots/home.png" width="160"/> | <img src="screenshots/search.png" width="160"/> | <img src="screenshots/search_list.png" width="160"/> | <img src="screenshots/detail.png" width="160"/> | <img src="screenshots/favorite.png" width="160"/> | <img src="screenshots/settings.png" width="160"/> |

---

## Tech Stack

| Category | Stack |
|----------|-------|
| Language | Kotlin 2.2.10 |
| Build | AGP 9.1.0, Gradle 9.3.1, KSP 2.3.2 |
| UI | XML Layouts, ViewBinding, Material Components 1.13.0 |
| Architecture | Clean Architecture, MVVM |
| DI | Hilt 2.59.2 |
| Network | Retrofit 3.0.0, OkHttp 5.0.0-alpha.14, kotlinx.serialization 1.8.1 |
| Database | Room 2.8.4, Typed DataStore 1.2.0 |
| Async | Coroutines, Flow, StateFlow, Channel |
| Paging | Paging 3.4.0, RemoteMediator |
| Image | Coil 3.4.0 |
| Navigation | Navigation Component 2.9.7, Safe Args |
| Test | JUnit 4, MockK 1.14.9, Turbine 1.2.1 (359개 유닛 + 23개 Espresso) |
| CI/CD | GitHub Actions (Build / Test / Coverage / Cert Pinning) |
| Static Analysis | Detekt 2.0.0-alpha.2 + KtLint |

---

## Architecture

```
API/DB  →  Repository  →  UseCase  →  ViewModel  →  Fragment (XML UI)
```

```
app/src/main/java/com/choo/moviefinder/
├── core/           # 공유 유틸리티 (ErrorMessageProvider, NetworkMonitor, 알림)
├── data/           # 데이터 레이어 (Room, Retrofit, PagingSource, RemoteMediator)
├── domain/         # 도메인 레이어 (Model, Repository 인터페이스, UseCase 48개)
├── presentation/   # 프레젠테이션 레이어 (Fragment, ViewModel, Adapter)
├── di/             # Hilt DI 모듈
├── MainActivity.kt
└── MovieFinderApp.kt
```

| 레이어 | 규칙 |
|--------|------|
| Domain | 순수 Kotlin, Android 프레임워크 의존성 없음 |
| Repository | ISP 원칙 기반 도메인별 분리 (10개 인터페이스) |
| Presentation | Domain UseCase에만 의존, Data 레이어 직접 참조 금지 |

---

## Features

| 화면 | 주요 기능 |
|------|-----------|
| 홈 | 현재 상영작 / 인기 영화 / 트렌딩 탭, Paging 3 무한 스크롤, 오프라인 캐시 (RemoteMediator), 최근 본 영화 |
| 검색 | 실시간 검색 (debounce), 연도 / 장르 / 정렬 필터, Discover 모드, 그리드 ↔ 리스트 전환 |
| 영화 상세 | 7개 API 병렬 호출, 출연진 / 추천 영화 / 리뷰 / 예고편, 즐겨찾기 / 워치리스트, 평점, 메모, Shared Element Transition |
| 배우 상세 | 프로필, 바이오그래피, 필모그래피 |
| 즐겨찾기 | 즐겨찾기 / 워치리스트 탭, 스와이프 삭제 + Undo, 정렬 |
| 설정 | 테마 / 언어 전환, 시청 목표, 데이터 백업 (JSON) |
| 통계 | 시청 편수, 평균 별점, 장르 파이차트, 월별 바차트, 히스토그램, 캘린더 히트맵 |

**기타**
- 홈 화면 위젯 (인기 영화 Top 10, 1시간 자동 갱신)
- 개봉일 알림 / 시청 목표 달성 알림 (WorkManager)
- 딥링크: `moviefinder://movie/{id}`, `moviefinder://person/{id}`, `moviefinder://stats`
- 한국어 + 영어 다국어 지원

---

## Testing

유닛 테스트 **390개** + Espresso UI 테스트 **23개**

```bash
# 유닛 테스트
./gradlew testDebugUnitTest

# UI 테스트 (에뮬레이터/실기기 필요)
./gradlew connectedDebugAndroidTest

# 커버리지 리포트 (최소 50% 기준)
./gradlew jacocoTestReport
```

---

## CI/CD

| 워크플로우 | 트리거 | 내용 |
|-----------|--------|------|
| `android-ci.yml` | push / PR → main | Detekt → Lint → Build → Test → JaCoCo |
| `cert-pin-check.yml` | 매주 월요일 | 인증서 핀 검증, 불일치 시 Issue 생성 |
| `pr-coverage.yml` | PR → main | 커버리지 리포트 PR 코멘트 자동 게시 |
| Pre-commit Hook | git commit | Detekt + 컴파일 체크 |

---

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
adb shell am start -a android.intent.action.VIEW -d "moviefinder://movie/550"
adb shell am start -a android.intent.action.VIEW -d "https://www.themoviedb.org/movie/550"
```

> CI/CD 사용 시 GitHub Secrets에 `TMDB_API_KEY` 등록 필요

---

## Requirements

- Android Studio (AGP 9.1.0+)
- JDK 21+
- minSdk 24 / targetSdk 36

## License

MIT License
