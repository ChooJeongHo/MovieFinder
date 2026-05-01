# MovieFinder 프로젝트 건강도 리포트

> 분석 일자: 2026-05-01  
> 분석 도구: Filesystem MCP + Claude Code  
> 분석 대상: `/Users/serveace/AndroidStudioProjects/MovieFinder`

---

## 파일별 크기 TOP 10

| 순위 | 파일 | 레이어 | 라인 수 | 비고 |
|:---:|------|--------|:-------:|------|
| 1 | `SearchFragment.kt` | Presentation | **777줄** | ⚠️ Fragment 분리 고려 |
| 2 | `DetailFragment.kt` | Presentation | **688줄** | ⚠️ Delegate 패턴 추가 여지 |
| 3 | `fragment_settings.xml` | Resources | **605줄** | ⚠️ 가장 큰 레이아웃 |
| 4 | `FavoriteFragment.kt` | Presentation | **545줄** | ⚠️ 복잡한 필터/탭 로직 |
| 5 | `fragment_detail.xml` | Resources | **528줄** | 점진적 로딩 반영 |
| 6 | `StatsFragment.kt` | Presentation | **444줄** | 9개 통계 카드 |
| 7 | `SettingsFragment.kt` | Presentation | **416줄** | i18n 대응 포함 |
| 8 | `fragment_stats.xml` | Resources | **403줄** | 커스텀 차트 5개 |
| 9 | `SearchViewModel.kt` | Presentation | **337줄** | SavedStateHandle 복원 포함 |
| 10 | `DatabaseModule.kt` | DI | **333줄** | DAO 28개 Provider |

---

## 레이어별 코드 구조

```
Kotlin 소스 코드 — 총 233개 파일 / 14,327줄
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Presentation  59파일  7,758줄  ████████████████████  54%
Data          62파일  2,892줄  ████████              20%
Domain        91파일  1,631줄  █████                 11%
Core          17파일    955줄  ███                    7%
DI             4파일    716줄  ██                     5%
Root           2파일    388줄  █                      3%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
XML Resources            102파일  5,967줄
Build/Config               5파일    562줄
────────────────────────────────────────
총합                      340파일  ~20,900줄
```

---

## 프로젝트 구조 (주요 디렉토리)

```
MovieFinder/
├── app/src/main/java/com/choo/moviefinder/
│   ├── presentation/        59파일  (13개 하위 패키지)
│   │   ├── detail/          9파일  1,135줄  ← Delegate 패턴 적용됨
│   │   ├── search/          2파일  1,114줄  ← 가장 복잡한 화면
│   │   ├── adapter/        12파일    741줄  ← 8종 어댑터
│   │   ├── favorite/        3파일    810줄
│   │   ├── widget/          8파일    776줄  ← 위젯 전담 패키지
│   │   ├── common/          6파일    713줄  ← 커스텀 Canvas 뷰
│   │   ├── settings/        2파일    634줄
│   │   ├── stats/           3파일    505줄
│   │   ├── onboarding/      4파일    301줄
│   │   ├── person/          3파일    276줄
│   │   └── home/            2파일    321줄
│   ├── data/               62파일  (5개 하위 패키지)
│   │   ├── local/          28파일  1,095줄  ← DAO 12 + Entity 11
│   │   ├── remote/         16파일    658줄  ← Service 3 + DTO 13
│   │   ├── repository/     12파일    844줄
│   │   └── paging/          5파일    272줄
│   ├── domain/             91파일
│   │   ├── usecase/        61파일  1,034줄  ← 49 UseCases
│   │   ├── repository/     13파일    350줄  ← ISP 기반
│   │   └── model/           ~파일    247줄
│   ├── core/               17파일    955줄
│   │   ├── util/           11파일    620줄
│   │   └── notification/    3파일    318줄
│   └── di/                  4파일    716줄
├── app/src/main/res/        102 XML파일
│   ├── layout/             ~70파일  5,967줄
│   └── values/strings.xml  404줄 (KO+EN)
├── app/schemas/             Room DB 스키마
├── .github/                 CI/CD 워크플로우
└── buildSrc/                AndroidConfig 공유 상수
```

---

## Presentation 하위 화면별 상세

| 패키지 | 파일 수 | 총 라인 | 주요 내용 |
|--------|:------:|:-------:|----------|
| `detail/` | 9 | 1,135 | Delegate 패턴 적용 (Memo, UserRating) |
| `search/` | 2 | 1,114 | Discover 모드, SavedStateHandle, 필터 9개 |
| `favorite/` | 3 | 810 | 탭, 정렬, 태그 필터 |
| `widget/` | 8 | 776 | Popular/Favorite/WatchGoal 위젯 3종 |
| `common/` | 6 | 713 | Canvas 뷰 5종 (Chart, Heatmap, Rating) |
| `settings/` | 2 | 634 | 테마, 캐시, 목표, 백업, 언어 |
| `adapter/` | 12 | 741 | PagingDataAdapter + ListAdapter 8종 |
| `stats/` | 3 | 505 | 통계 카드 9개 |
| `home/` | 2 | 321 | 3탭 + RemoteMediator |
| `onboarding/` | 4 | 301 | 온보딩 뷰페이저 |
| `person/` | 3 | 276 | 배우 상세 + CollapsingToolbar |

---

## 건강도 평가

| 항목 | 상태 | 평가 |
|------|:----:|------|
| **아키텍처 계층 분리** | ✅ | Clean Architecture 3계층 엄격 준수 |
| **의존성 방향** | ✅ | Presentation→Domain←Data, Data 직접 참조 금지 |
| **단일 책임 원칙** | ✅ | 61개 UseCase, 13개 Repository 인터페이스 (ISP) |
| **테스트 커버리지** | ✅ | 509 단위 + 23 UI 테스트, JaCoCo 50%+ |
| **보안** | ✅ | Certificate Pinning, R8 Full Mode, SecureTokenStore |
| **오프라인 지원** | ✅ | RemoteMediator + 1시간 캐시 만료 정책 |
| **접근성 (a11y)** | ✅ | contentDescription, importantForAccessibility 전반 적용 |
| **국제화 (i18n)** | ✅ | 한국어 + 영어 문자열 리소스 완비 |
| **CI/CD** | ✅ | Detekt → Lint → Build → Test → JaCoCo 파이프라인 |
| **대형 Fragment** | ⚠️ | SearchFragment 777줄, DetailFragment 688줄 — Delegate 추가 분리 여지 |
| **DatabaseModule 크기** | ⚠️ | 333줄 단일 파일 — 도메인별 분할 고려 가능 |
| **settings.xml 크기** | ⚠️ | 605줄 — NestedScrollView 기반 분할 고려 가능 |
| **Kotlin/XML 비율** | ℹ️ | 14,327줄 Kotlin : 5,967줄 XML (약 2.4:1) |

---

## 종합 요약

- **총 규모**: 340+ 파일, 약 20,900줄 (프로덕션급)
- **강점**: Clean Architecture + MVVM 엄격 준수, 풍부한 테스트, 보안/접근성/i18n 모두 양호
- **개선 포인트**: `SearchFragment`, `DetailFragment`의 추가 Delegate 분리로 유지보수성 향상 가능
- **아키텍처 성숙도**: Domain 레이어가 91개 파일(가장 많음)이면서 라인은 11%에 불과 → 얇은 UseCase 원칙 잘 적용됨
