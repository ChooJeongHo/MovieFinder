# MovieFinder 프로젝트 건강도 리포트

> 분석 일자: 2026-10-16 (이전 판: 2026-09-16)
> 분석 도구: git 객체 기반 줄 수 집계 + JaCoCo + Claude Code
> 분석 대상: `/Users/serveace/AndroidStudioProjects/MovieFinder` (main `f91e5b4`)
> 집계 검증: 이전 판의 값(Kotlin 302파일/17,182줄, XML 115파일/6,661줄, 문자열 키 336개, 유닛 테스트 856개 등)이
> 9/16 문서 갱신 커밋(`725af5f`)에 같은 방식을 적용하면 그대로 재현되는 것을 확인한 뒤 현재 값을 계산했다.
> 집계 범위: Kotlin = `app/src/main/java/**/*.kt`, XML = `app/src/main/res/**/*.xml`(레이아웃 표는 `res/layout`만, `layout-land`는 따로 표기),
> Build/Config = `build.gradle.kts` 4개(app·baselineprofile·루트·buildSrc) + `settings.gradle.kts` (이전 판에 정의가 없어 합이 같은 이 구성으로 해석, 9/16 대비 변화 없음).

---

## 2026-09-16 대비 변화

| 항목 | 2026-09-16 | 2026-10-16 |
|------|:----------:|:----------:|
| Kotlin 소스(app) | 302파일 / 17,182줄 | 315파일 / 17,946줄 |
| XML 리소스 | 115파일 / 6,661줄 | 118파일 / 7,058줄 |
| UseCase / Repository 인터페이스 | 75 / 20 | 78 / 21 |
| Room DB 버전 / `@Dao` / `@Entity` | v24 / 13 / 14 | v25 / 14 / 15 |
| 유닛 테스트 | 856개 (78파일) | 987개 (92파일) |
| JaCoCo Line / Branch | 75.31% / 70.24% | 78.48% / 72.15% |
| 문자열 키 (KO / EN) | 336 / 336 | 338 / 338 |
| `SearchFragment` / `DetailFragment` | 825 / 726줄 | 845 / 746줄 |

> 이전 판은 Room 항목을 "DAO 15 + Entity 14"로 적었는데 이는 `dao/` 폴더의 파일 수(쿼리 결과 클래스 `GenreCountResult`·`MonthlyCount` 2개 포함)였다. `@Dao` 어노테이션 기준은 9/16에 13개, 지금 14개다.

---

## 파일별 크기 TOP 10

> Kotlin 소스와 레이아웃 XML만 순위에 넣었다(이전 판과 같은 범위). 문자열 리소스 `values-en/strings.xml`(459줄)·`values/strings.xml`(449줄)은 순위에서 제외했다.

| 순위 | 파일 | 레이어 | 라인 수 | 비고 |
|:---:|------|--------|:-------:|------|
| 1 | `SearchFragment.kt` | Presentation | **845줄** | ⚠️ Fragment 분리 고려 (2026-05 777줄 → 825줄(09-16) → 계속 증가) |
| 2 | `DetailFragment.kt` | Presentation | **746줄** | ⚠️ Delegate 패턴 추가 여지 (IME 인셋·메모 목록 수정으로 +20) |
| 3 | `fragment_settings.xml` | Resources | **605줄** | ⚠️ 가장 큰 레이아웃 |
| 4 | `fragment_detail.xml` | Resources | **540줄** | 점진적 로딩 반영 |
| 5 | `FavoriteFragment.kt` | Presentation | **528줄** | ⚠️ 복잡한 필터/탭 로직 |
| 6 | `StatsFragment.kt` | Presentation | **455줄** | 통계 카드 9개 |
| 7 | `SettingsFragment.kt` | Presentation | **430줄** | TMDB 계정 연동 + i18n 대응 포함 |
| 8 | `DatabaseModule.kt` | DI | **419줄** | DAO 14개 Provider + 마이그레이션 12개 (2026-05 333줄에서 증가) |
| 9 | `fragment_stats.xml` | Resources | **403줄** | 커스텀 차트 5개 |
| 10 | `NetworkModule.kt` | DI | **368줄** | TMDB/KOFIC/KMRB 3계열 Retrofit + CertificatePinner + 디버그 로그 마스킹 |

---

## 레이어별 코드 구조

```
Kotlin 소스 코드(app 모듈) — 총 315개 파일 / 17,946줄
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Presentation  79파일  9,348줄  ████████████████████  52%
Data          83파일  3,808줄  █████████              21%
Domain       123파일  2,217줄  █████                  12%
Core          23파일  1,168줄  ███                     7%
DI             5파일    958줄  ██                      5%
Root           2파일    447줄  █                       2%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
XML Resources            118파일  7,058줄 (layout 37파일 4,498줄 + layout-land 1파일 349줄)
Build/Config               5파일    434줄
────────────────────────────────────────
app 모듈 총합             438파일  ~25,438줄

+ tools/codebase-rag — 19파일 / 1,214줄 (Kotlin 기준, 9/16과 같음), 별도 JVM 모듈이라 위 집계에서 제외
```

> 2026-05-01 대비 233파일/14,327줄 → 315파일/17,946줄로 증가 (Kotlin 줄 기준 약 +25%). Domain 레이어가 91→123파일로 가장 크게 늘었는데, UseCase가 61→78개로 늘면서도 파일당 평균 라인은 여전히 얇게 유지됨(추가 근거는 아래 "건강도 평가" 참고).

---

## 프로젝트 구조 (주요 디렉토리)

```
MovieFinder/
├── app/src/main/java/com/choo/moviefinder/
│   ├── presentation/        79파일  (13개 하위 패키지)
│   │   ├── search/          7파일  1,542줄  ← 가장 복잡한 화면 (결과/최근 검색어 영역 판정을 순수 함수로 분리)
│   │   ├── widget/         15파일  1,371줄  ← Popular/Favorite/WatchGoal/BoxOffice 위젯 4종
│   │   ├── detail/         10파일  1,318줄  ← Delegate 패턴 적용됨
│   │   ├── adapter/        14파일    871줄  ← PagingDataAdapter + ListAdapter 9종 + LoadStateAdapter + ViewHolder 2종
│   │   ├── favorite/        4파일    799줄
│   │   ├── common/          7파일    759줄  ← 커스텀 Canvas 뷰 5종 + Compose 공용 테마(`MovieFinderTheme`) + `GridLayoutManagerFactory`
│   │   ├── settings/        2파일    659줄  ← TMDB 계정 연동 추가로 증가
│   │   ├── stats/           3파일    508줄
│   │   ├── home/            5파일    504줄  ← 박스오피스 섹션 추가로 증가
│   │   ├── onboarding/      4파일    314줄
│   │   ├── person/          3파일    276줄
│   │   ├── reminder/        3파일    236줄  ← 신규(개봉일 알림)
│   │   └── collection/      2파일    191줄  ← 신규
│   ├── data/               83파일  (5개 하위 패키지)
│   │   ├── local/          37파일  1,354줄  ← DAO 14 + 결과 클래스 2 + Entity 15, Room DB v25
│   │   ├── repository/     17파일  1,239줄
│   │   ├── remote/         21파일    914줄  ← TMDB/KOFIC/KMRB API 3계열
│   │   ├── paging/          6파일    234줄
│   │   └── util/            2파일     67줄
│   ├── domain/            123파일
│   │   ├── usecase/        78파일  1,431줄  ← 49→78개로 증가 (KMRB 등급, TMDB 계정 연동, 표시 제목 지연 조회 등)
│   │   ├── repository/     21파일    447줄  ← 11→21개, ISP 세분화 계속됨
│   │   └── model/          24파일    339줄
│   ├── core/               23파일  1,168줄
│   │   ├── util/           16파일    723줄
│   │   ├── notification/    5파일    369줄
│   │   ├── shortcut/        1파일     59줄  ← App Shortcuts
│   │   └── startup/         1파일     17줄
│   └── di/                  5파일    958줄
├── app/src/main/res/        118 XML파일
│   ├── layout/             37파일  4,498줄
│   ├── layout-land/         1파일    349줄  ← 가로 모드 검색 화면 (123일차)
│   └── values/strings.xml  449줄(KO) + 459줄(EN)
├── app/schemas/             Room DB 스키마 (v25)
├── tools/codebase-rag/      19파일 1,214줄, RAG 기반 코드베이스 질의 도구(별도 JVM 모듈)
├── .github/                 CI/CD 워크플로우
└── buildSrc/                AndroidConfig 공유 상수
```

---

## Presentation 하위 화면별 상세

| 패키지 | 파일 수 | 총 라인 | 주요 내용 |
|--------|:------:|:-------:|----------|
| `search/` | 7 | 1,542 | Discover 모드, SavedStateHandle, 필터 9개 + KMRB 등급 필터, 가로 전용 레이아웃 |
| `widget/` | 15 | 1,371 | Popular/Favorite/WatchGoal/BoxOffice 위젯 (BoxOffice는 Glance, 일별/주간 토글) |
| `detail/` | 10 | 1,318 | Delegate 패턴 적용 (Memo, UserRating), IME 인셋 처리 |
| `adapter/` | 14 | 871 | PagingDataAdapter + ListAdapter 9종, LoadStateAdapter, ViewHolder 2종 |
| `favorite/` | 4 | 799 | 탭, 정렬, 태그 필터 |
| `common/` | 7 | 759 | Canvas 뷰 5종 (Chart, Heatmap, Rating) + `MovieFinderTheme` |
| `settings/` | 2 | 659 | 테마, 캐시, 목표, 백업, 언어, TMDB 계정 연동 |
| `stats/` | 3 | 508 | 통계 카드 9개 |
| `home/` | 5 | 504 | 3탭 + RemoteMediator + 박스오피스 섹션 (섹션 접기 AppBarLayout) |
| `onboarding/` | 4 | 314 | 온보딩 (전체 Compose) |
| `person/` | 3 | 276 | 배우 상세 + CollapsingToolbar |
| `reminder/` | 3 | 236 | 개봉일 알림 (신규) |
| `collection/` | 2 | 191 | (신규) |

---

## 건강도 평가

| 항목 | 상태 | 평가 |
|------|:----:|------|
| **아키텍처 계층 분리** | ✅ | Clean Architecture 3계층 엄격 준수 |
| **의존성 방향** | ✅ | Presentation→Domain←Data, Data 직접 참조 금지 |
| **단일 책임 원칙** | ✅ | 78개 UseCase, 21개 Repository 인터페이스 (ISP) |
| **테스트 커버리지** | ✅ | 단위 테스트 987개(92파일) + UI 테스트 34개(7파일, `@Test` 개수·CI 미실행), JaCoCo **Line 78.48% / Branch 72.15%**(Instruction 73.91%, 2026-10-16 `jacocoTestReport` 실측·유닛 테스트 기준, CI 최소 기준 50% 상회) |
| **보안** | ✅ | Certificate Pinning(TMDB/Image), R8 Full Mode, KOFIC/KMRB는 쿼리 파라미터 키(핀 미적용, 의도적), 디버그 로그의 쿼리 비밀값 마스킹(2026-10-07) |
| **오프라인 지원** | ✅ | RemoteMediator + 1시간 캐시 만료 정책 |
| **접근성 (a11y)** | ✅ | contentDescription, importantForAccessibility 전반 적용 (상세 항목은 `ACCESSIBILITY_REPORT.md` 참고) |
| **국제화 (i18n)** | ✅ | 한국어(449줄) + 영어(459줄) 문자열 리소스, 키 338개 양쪽 일치, 상세는 `I18N_REPORT.md` 참고 |
| **CI/CD** | ✅ | Detekt → Lint → Debug/Release Build(R8 검증) → Test → JaCoCo 파이프라인, pre-push에 Lint 포함 |
| **대형 Fragment** | ⚠️ | SearchFragment 845줄(계속 증가 중), DetailFragment 746줄 — Delegate 추가 분리 여지 |
| **DatabaseModule 크기** | ⚠️ | 419줄(2026-05 333줄 대비 증가) — 도메인별 분할 고려 가능 |
| **settings.xml 크기** | ⚠️ | 605줄 — 여전히 최대 레이아웃, NestedScrollView 기반 분할 고려 가능 |
| **Room 마이그레이션** | ⚠️ | DB v25인데 `ALL_MIGRATIONS`는 23→24까지 — 24→25(테이블 `movie_korean_title_cache` 추가)는 Migration 없이 올라감(코드로 확인, 실제 업그레이드는 실행하지 않음) |
| **Kotlin/XML 비율** | ℹ️ | 17,946줄 Kotlin : 7,058줄 XML (약 2.5:1) |

---

## 종합 요약

- **총 규모(app 모듈)**: 438 파일, 약 25,438줄 (2026-05-01의 340파일/20,900줄 대비 줄 기준 약 +22% 성장) — 별도로 `tools/codebase-rag` 모듈 19파일/1,214줄
- **강점**: Clean Architecture + MVVM 엄격 준수, 테스트 커버리지 상승(Line 75.31% → 78.48%), 보안/접근성/i18n 모두 양호, TMDB 계정 연동·KMRB 등급·박스오피스·앱 언어 연동·동적 색상 등 신규 기능 편입 후에도 레이어 규칙 유지
- **개선 포인트**: `SearchFragment`(845줄)·`DetailFragment`(746줄)의 추가 Delegate 분리, `DatabaseModule.kt`(419줄) 도메인별 분할 검토, v24→v25 마이그레이션 공백 정리
- **아키텍처 성숙도**: Domain 레이어가 123개 파일(가장 많음)이면서 라인은 12%에 불과 → 얇은 UseCase 원칙이 UseCase 수가 49→78개로 늘어난 뒤에도 유지됨
