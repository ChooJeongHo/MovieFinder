# MovieFinder 프로젝트 건강도 리포트

> 분석 일자: 2026-09-16
> 분석 도구: Bash(find/wc) + JaCoCo + Claude Code
> 분석 대상: `/Users/serveace/AndroidStudioProjects/MovieFinder`

---

## 파일별 크기 TOP 10

| 순위 | 파일 | 레이어 | 라인 수 | 비고 |
|:---:|------|--------|:-------:|------|
| 1 | `SearchFragment.kt` | Presentation | **825줄** | ⚠️ Fragment 분리 고려 (2026-05 777줄 → 계속 증가) |
| 2 | `DetailFragment.kt` | Presentation | **726줄** | ⚠️ Delegate 패턴 추가 여지 |
| 3 | `fragment_settings.xml` | Resources | **605줄** | ⚠️ 가장 큰 레이아웃 |
| 4 | `fragment_detail.xml` | Resources | **540줄** | 점진적 로딩 반영 |
| 5 | `FavoriteFragment.kt` | Presentation | **528줄** | ⚠️ 복잡한 필터/탭 로직 |
| 6 | `StatsFragment.kt` | Presentation | **455줄** | 통계 카드 9개 |
| 7 | `SettingsFragment.kt` | Presentation | **426줄** | TMDB 계정 연동 + i18n 대응 포함 |
| 8 | `DatabaseModule.kt` | DI | **411줄** | DAO 15개 Provider (2026-05 333줄에서 증가) |
| 9 | `fragment_stats.xml` | Resources | **403줄** | 커스텀 차트 5개 |
| 10 | `NetworkModule.kt` | DI | **347줄** | TMDB/KOFIC/KMRB 3계열 Retrofit + CertificatePinner |

---

## 레이어별 코드 구조

```
Kotlin 소스 코드(app 모듈) — 총 302개 파일 / 17,182줄
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Presentation  75파일  8,961줄  ████████████████████  52%
Data          80파일  3,645줄  █████████              21%
Domain       119파일  2,106줄  █████                  12%
Core          21파일  1,128줄  ███                     7%
DI             5파일    923줄  ██                      5%
Root           2파일    419줄  █                       2%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
XML Resources            115파일  6,661줄 (layout 37파일 4,467줄)
Build/Config               5파일    434줄
────────────────────────────────────────
app 모듈 총합             422파일  ~24,277줄

+ tools/codebase-rag (신규, 2026-09 추가) — 19파일 / 1,214줄, 별도 JVM 모듈이라 위 집계에서 제외
```

> 2026-05-01 대비 233파일/14,327줄 → 302파일/17,182줄로 증가 (약 +20%). Domain 레이어가 91→119파일로 가장 크게 늘었는데, UseCase가 61→75개로 늘면서도 파일당 평균 라인은 여전히 얇게 유지됨(추가 근거는 아래 "건강도 평가" 참고).

---

## 프로젝트 구조 (주요 디렉토리)

```
MovieFinder/
├── app/src/main/java/com/choo/moviefinder/
│   ├── presentation/        75파일  (13개 하위 패키지)
│   │   ├── search/          5파일  1,394줄  ← 가장 복잡한 화면
│   │   ├── detail/         10파일  1,291줄  ← Delegate 패턴 적용됨
│   │   ├── widget/         14파일  1,228줄  ← Popular/Favorite/WatchGoal 위젯 3종
│   │   ├── adapter/        14파일    870줄  ← PagingDataAdapter + ListAdapter 8종
│   │   ├── favorite/        4파일    799줄
│   │   ├── settings/        2파일    640줄  ← TMDB 계정 연동 추가로 증가
│   │   ├── common/          6파일    713줄  ← 커스텀 Canvas 뷰 5종
│   │   ├── stats/           3파일    508줄
│   │   ├── home/            5파일    501줄  ← 박스오피스 섹션 추가로 증가
│   │   ├── onboarding/      4파일    314줄
│   │   ├── person/          3파일    276줄
│   │   ├── reminder/        3파일    236줄  ← 신규(개봉일 알림)
│   │   └── collection/      2파일    191줄  ← 신규
│   ├── data/               80파일  (5개 하위 패키지)
│   │   ├── local/          35파일  1,314줄  ← DAO 15 + Entity 14, Room DB v24
│   │   ├── repository/     16파일  1,131줄
│   │   ├── remote/         21파일    912줄  ← TMDB/KOFIC/KMRB API 3계열
│   │   ├── paging/          6파일    221줄
│   │   └── util/            2파일     67줄
│   ├── domain/             119파일
│   │   ├── usecase/        75파일  1,345줄  ← 49→75개로 증가 (KMRB 등급, TMDB 계정 연동 등)
│   │   ├── repository/     20파일    422줄  ← 11→20개, ISP 세분화 계속됨
│   │   └── model/          24파일    339줄
│   ├── core/               21파일  1,128줄
│   │   ├── util/           14파일    683줄
│   │   ├── notification/    5파일    369줄
│   │   ├── shortcut/        1파일     59줄  ← 신규(App Shortcuts)
│   │   └── startup/         1파일     17줄
│   └── di/                  5파일    923줄
├── app/src/main/res/        115 XML파일
│   ├── layout/             37파일  4,467줄
│   └── values/strings.xml  447줄(KO) + 457줄(EN)
├── app/schemas/             Room DB 스키마 (v24)
├── tools/codebase-rag/      신규 — 19파일 1,214줄, RAG 기반 코드베이스 질의 도구(별도 JVM 모듈)
├── .github/                 CI/CD 워크플로우
└── buildSrc/                AndroidConfig 공유 상수
```

---

## Presentation 하위 화면별 상세

| 패키지 | 파일 수 | 총 라인 | 주요 내용 |
|--------|:------:|:-------:|----------|
| `search/` | 5 | 1,394 | Discover 모드, SavedStateHandle, 필터 9개 + KMRB 등급 필터 |
| `detail/` | 10 | 1,291 | Delegate 패턴 적용 (Memo, UserRating) |
| `widget/` | 14 | 1,228 | Popular/Favorite/WatchGoal/BoxOffice 위젯 |
| `adapter/` | 14 | 870 | PagingDataAdapter + ListAdapter 8종 |
| `favorite/` | 4 | 799 | 탭, 정렬, 태그 필터 |
| `settings/` | 2 | 640 | 테마, 캐시, 목표, 백업, 언어, TMDB 계정 연동 |
| `common/` | 6 | 713 | Canvas 뷰 5종 (Chart, Heatmap, Rating) |
| `stats/` | 3 | 508 | 통계 카드 9개 |
| `home/` | 5 | 501 | 3탭 + RemoteMediator + 박스오피스 섹션 |
| `onboarding/` | 4 | 314 | 온보딩 뷰페이저 |
| `person/` | 3 | 276 | 배우 상세 + CollapsingToolbar |
| `reminder/` | 3 | 236 | 개봉일 알림 (신규) |
| `collection/` | 2 | 191 | (신규) |

---

## 건강도 평가

| 항목 | 상태 | 평가 |
|------|:----:|------|
| **아키텍처 계층 분리** | ✅ | Clean Architecture 3계층 엄격 준수 |
| **의존성 방향** | ✅ | Presentation→Domain←Data, Data 직접 참조 금지 |
| **단일 책임 원칙** | ✅ | 75개 UseCase, 20개 Repository 인터페이스 (ISP) |
| **테스트 커버리지** | ✅ | 단위 테스트 856개(78파일) + UI 테스트 29개(5파일), JaCoCo **Line 75.31% / Branch 70.24%**(2026-09-16 `jacocoTestReport` 실측, CI 최소 기준 50% 상회) |
| **보안** | ✅ | Certificate Pinning(TMDB/Image), R8 Full Mode, KOFIC/KMRB는 쿼리 파라미터 키(핀 미적용, 의도적) |
| **오프라인 지원** | ✅ | RemoteMediator + 1시간 캐시 만료 정책 |
| **접근성 (a11y)** | ✅ | contentDescription, importantForAccessibility 전반 적용 (상세 항목은 `ACCESSIBILITY_REPORT.md` 참고) |
| **국제화 (i18n)** | ✅ | 한국어(447줄) + 영어(457줄) 문자열 리소스, 상세는 `I18N_REPORT.md` 참고 |
| **CI/CD** | ✅ | Detekt → Lint → Debug/Release Build(R8 검증) → Test → JaCoCo 파이프라인 |
| **대형 Fragment** | ⚠️ | SearchFragment 825줄(계속 증가 중), DetailFragment 726줄 — Delegate 추가 분리 여지 |
| **DatabaseModule 크기** | ⚠️ | 411줄(2026-05 333줄 대비 증가) — 도메인별 분할 고려 가능 |
| **settings.xml 크기** | ⚠️ | 605줄 — 여전히 최대 레이아웃, NestedScrollView 기반 분할 고려 가능 |
| **Kotlin/XML 비율** | ℹ️ | 17,182줄 Kotlin : 6,661줄 XML (약 2.6:1) |

---

## 종합 요약

- **총 규모(app 모듈)**: 422+ 파일, 약 24,277줄 (2026-05-01의 340파일/20,900줄 대비 +20% 성장) — 별도로 `tools/codebase-rag` 신규 모듈 19파일/1,214줄
- **강점**: Clean Architecture + MVVM 엄격 준수, 테스트 커버리지 상승(Line 75.31%), 보안/접근성/i18n 모두 양호, TMDB 계정 연동·KMRB 등급·박스오피스 등 신규 기능 편입 후에도 레이어 규칙 유지
- **개선 포인트**: `SearchFragment`(825줄)·`DetailFragment`(726줄)의 추가 Delegate 분리, `DatabaseModule.kt`(411줄) 도메인별 분할 검토
- **아키텍처 성숙도**: Domain 레이어가 119개 파일(가장 많음)이면서 라인은 12%에 불과 → 얇은 UseCase 원칙이 UseCase 수가 49→75개로 늘어난 뒤에도 유지됨
