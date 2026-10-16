# MovieFinder 접근성 감사 보고서

**감사 일시:** 2026-06-24 (최초) / 2026-09-16, 2026-10-16 재검증  
**대상:** app/src/main/res/layout/ (37개 XML) + layout-land/ (1개), app/src/main/java/ (프레젠테이션 레이어 전체)  
**기준:** WCAG 2.1 AA, Android Accessibility Guidelines, Google Material Design Accessibility

> **재검증 방식 (2026-09-16, 2026-10-16)**: 두 번 모두 TalkBack 실제 음성 낭독이나 실제 터치 판정 같은 런타임 검증은
> 수행하지 못했다(2026-09-16에는 `adb devices` 결과가 없었고, 2026-10-16은 문서 갱신 작업이라 기기를 쓰지 않았다).
> 아래 갱신 내용은 XML/Kotlin 소스 코드에 대한 **정적 재확인**(grep/Read)만 반영한 것이며, 런타임 검증이 필요한
> 항목은 기존과 동일하게 "실기기 재검증 필요"로 남겨둔다. 본문의 줄 번호는 2026-10-16 main(`f91e5b4`) 기준이고 편집에 따라 계속 밀린다.

---

## 요약

| 심각도 | 건수 |
|--------|------|
| CRITICAL | 0 |
| MAJOR | 4 |
| MINOR | 5 |
| INFO | 3 |

전반적으로 contentDescription 처리와 shimmer 레이아웃의 `importantForAccessibility="noHideDescendants"` 설정 등 기본기는 잘 갖춰져 있습니다. 최초 감사(2026-06-24) 시점에 **색상 대비 2건**, **LiveRegion 미사용**, **알파 텍스트 대비** 등 4개 MAJOR 이슈가 발견되었고, 2026-09-16 재검증 결과 M-3(LiveRegion)이 HomeFragment 한 곳에 한해 부분 해결됐을 뿐 나머지 3건(M-1, M-2, M-4)과 M-3의 나머지 범위는 모두 그대로 남아있습니다. MINOR/INFO 항목도 전부 미해결 상태입니다. 2026-10-16 정적 재확인에서도 이 상태는 그대로입니다(색상 값, 줄 위치, `AccessibilityDelegate`/`roleDescription` 부재를 다시 확인).

> **동적 색상(2026-09-23) 참고**: Android 12+에서는 `DynamicColors.applyIfAvailable()`(`MainActivity.kt:72`)가 XML 테마 색을 배경화면 기반 팔레트로 덮어쓰고, Compose 화면은 `MovieFinderTheme`(API 31+)가 동적 색상 스킴을 쓴다.
> 아래 M-1·M-2와 "색상 대비 전체 결과" 표의 수치는 **고정 팔레트(XML 테마) 기준**이며, 동적 팔레트에서의 대비는 측정하지 않았다. Android 11 이하와, 동적 팔레트가 아직 만들어지지 않은 기기에서는 고정 팔레트가 그대로 쓰인다(삼성 One UI는 배경화면을 바꿔도 팔레트가 자동 생성되지 않아 사용자가 컬러 팔레트를 직접 골라야 한다 — 실기기 SM-S926N).

---

## MAJOR 이슈

### M-1 · 색상 대비 실패: `colorSecondary` (#01B4E4) on white

| 항목 | 값 |
|------|-----|
| 전경색 | `#01B4E4` (colorSecondary) |
| 배경색 | `#FFFFFF` (colorSurface/white) |
| 대비 비율 | **2.43:1** |
| WCAG 기준 | 4.5:1 (일반 텍스트), 3:1 (대형 텍스트/UI 요소) |
| 판정 | **FAIL** |

**영향 범위:** `?attr/colorSecondary`를 텍스트 또는 아이콘 색상으로 사용하는 모든 뷰. Material Chip의 `chipStrokeColor`, progress indicator 등에서 흰 배경 위에 렌더링될 경우 저시력 사용자가 식별 불가.

**수정 방안:**
- 흰 배경 위에 `colorSecondary`를 전경색으로 단독 사용 금지
- 텍스트·아이콘에는 `colorOnSurface` / `colorOnBackground` 사용
- 보조 강조가 필요한 경우 `colorSecondary`를 배경으로, `colorOnSecondary` (#FFFFFF)를 전경으로 사용

---

### M-2 · 색상 대비 실패: 다크 모드 `colorPrimaryDark` (#90CEA1) 위 흰 텍스트

| 항목 | 값 |
|------|-----|
| 전경색 | `#FFFFFF` |
| 배경색 | `#90CEA1` (`values/colors.xml:21`의 `colorPrimaryDark`, 다크 테마가 `values-night/themes.xml`에서 `colorPrimary`로 참조) |
| 대비 비율 | **1.82:1** |
| WCAG 기준 | 4.5:1 |
| 판정 | **FAIL** |

**위치:** `app/src/main/res/values-night/themes.xml` — `colorPrimary`가 다크 모드에서 `#90CEA1`(밝은 녹색)으로 설정됨. Toolbar 배경, FAB 배경 등 `colorPrimary`를 배경으로 사용하는 컴포넌트에서 흰 텍스트/아이콘이 렌더링되면 대비 실패.

> 2026-10-16 정정: 이전 판은 `colorPrimaryDark`가 `values-night/colors.xml`에 있다고 적었으나, 그 파일에는 `icon_default`·`backdrop_overlay`·`shimmer_placeholder`만 있고 `colorPrimaryDark`의 정의는 `values/colors.xml`에 있다. 위 "위치"(`values-night/themes.xml`이 `colorPrimary`를 `#90CEA1`로 지정)는 맞다.

**수정 방안:**
```xml
<!-- values/colors.xml (values-night/themes.xml이 colorPrimary로 참조) -->
<!-- 현재 #90CEA1(흰 배경에 어두운 텍스트가 필요한 밝은 색) →
     어두운 primary로 교체하거나, colorOnPrimary를 어두운 색으로 조정 -->
<color name="colorPrimaryDark">#FF1B5E3B</color>  <!-- 더 어두운 녹색 -->
<!-- 또는 다크 모드 Toolbar에 colorSurface 사용 + colorOnSurface 텍스트 -->
```

---

### M-3 · LiveRegion / announceForAccessibility 부분 미사용 (2026-09-16 부분 해결 확인, 2026-10-16 재확인)

**위치:** `app/src/main/java/com/choo/moviefinder/presentation/` 전체

> **부분 해결됨**: `HomeFragment.kt:131`의 `binding.tvBoxOfficeLabel.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE`로
> 박스오피스 일별/주간 라벨 전환 시 TalkBack이 자동 발화하도록 처리됐다(코드 주석이 이 보고서의
> M-3 권고를 직접 인용하고 있음). 처음에는 `ViewCompat.setAccessibilityLiveRegion()`이었으나 2026-09-20 deprecated 경고 정리(`c46c038`)에서
> View 프로퍼티로 바꿨다(동작 변경 없음). 다만 이 패턴이 적용된 곳은 **이 한 곳뿐**이며, `announceForAccessibility`
> 실제 호출은 여전히 코드베이스 전체에 0건이다(소스에서 일치하는 한 줄은 `HomeFragment.kt:130`의 주석). 아래 표의 나머지 상황은 여전히 미해결.

**영향 범위:**

| 상황 | TalkBack 사용자 경험 | 상태 |
|------|---------------------|------|
| 박스오피스 일별/주간 라벨 전환 (`HomeFragment`) | 전환 사실 발화됨 | ✅ 해결됨 (2026-09) |
| 에러 뷰 표시 (`layout_error.xml`) | 에러 발생을 인지 불가 | 미해결 |
| 검색 결과 로딩 완료 | 결과가 나타났는지 알 수 없음 | 미해결 |
| FAB 상태 변경 (즐겨찾기 추가/제거) | contentDescription은 변경되나(`DetailFragment.kt:349,358`) 변경 사실이 발화되지 않음 | 미해결 |
| Snackbar 표시 | 스낵바 메시지 발화 안 됨 | 미해결 |
| 빈 상태/오류 → 정상 전환 | 전환 사실 미인지 | 미해결 |

**수정 방안:**
```kotlin
// 에러/빈 상태 표시 시
binding.errorView.visibility = View.VISIBLE
binding.errorView.announceForAccessibility(getString(R.string.error_load_failed))

// FAB 토글 후 (DetailFragment)
binding.fabFavorite.contentDescription = newDesc
binding.fabFavorite.announceForAccessibility(newDesc)

// 검색 결과 수신 시 (SearchFragment)
ViewCompat.setAccessibilityLiveRegion(
    binding.tvResultCount,
    ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
)
```

---

### M-4 · `alpha="0.5"` 텍스트 대비 실패 (일반 텍스트)

| 항목 | 값 |
|------|-----|
| 위치 | `item_person_search.xml:58` — `tv_known_for_titles` (2026-06-24 당시 56행에서 58행으로 이동, 내용 동일) |
| 설정 | `android:alpha="0.5"` |
| 유효 대비 비율 | **~3.95:1** (흰 배경 기준) |
| WCAG AA 기준 | 4.5:1 (일반 크기 텍스트) |
| 판정 | **FAIL** |

`android:alpha`는 View 전체(배경 포함)에 적용되므로, 실제 텍스트-배경 대비가 의도보다 낮아집니다. `textAppearanceCaption` 크기(12sp 미만)는 WCAG의 "소형 텍스트"로 분류되어 4.5:1 기준 적용.

```xml
<!-- 수정 전 -->
<TextView android:alpha="0.5" ... />

<!-- 수정 후: textColor에 ARGB 알파 직접 지정 -->
<TextView
    android:textColor="#80000000"  <!-- 50% alpha on black -->
    ... />
<!-- 또는 색상 리소스로 -->
<color name="text_hint">#80000000</color>
```

> **참고:** `alpha="0.7"` (`tv_character`, `tv_overview`)은 약 8.45:1로 통과, `alpha="0.6"` (`tv_vote_count`)은 약 6.0:1로 통과.

---

## MINOR 이슈

### N-1 · `CircularRatingView` 터치 타겟 40dp (기준 48dp 미달)

| 항목 | 값 |
|------|-----|
| 위치 | `item_movie_grid.xml:32`, `item_movie_list.xml:81` |
| 크기 | `@dimen/rating_view_size` = **40dp × 40dp** |
| WCAG 기준 | 48dp × 48dp (Android 접근성 가이드라인) |

현재 두 아이템 모두 `importantForAccessibility="no"`로 설정되어 TalkBack 직접 포커스는 받지 않지만, 시각장애는 없으나 운동 장애가 있는 사용자(스위치 접근, 큰 손가락)에게 터치 어려움 가능. 카드 전체가 클릭 단위이므로 치명적이지는 않음.

**수정 방안:**
```xml
<!-- dimens.xml -->
<dimen name="rating_view_size">48dp</dimen>  <!-- 40dp → 48dp -->
```

---

### N-2 · `fab_watchlist` — Mini FAB 시각적 크기 ~40dp

| 위치 | `fragment_detail.xml:521` (2026-06-24 당시 509행, 이후 라인 이동·내용 동일) |
|------|--------------------------|
| 설정 | `app:fabSize="mini"` |
| 시각적 크기 | ~40dp (기준 48dp 미달) |

Mini FAB은 Android 내부적으로 추가 터치 여백을 제공하나, 시각적 터치 타겟이 명시적으로 48dp를 보장하지 않음. 즐겨찾기 FAB은 기본 크기이므로 문제 없음.

**수정 방안:**
```xml
<!-- app:fabSize="mini" 제거 또는 -->
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:minWidth="48dp"
    android:minHeight="48dp"
    app:fabSize="auto" />
```

---

### N-3 · `rating_red` (#DB2360) 텍스트 대비 부족

| 항목 | 값 |
|------|-----|
| 전경색 | `#DB2360` (rating_red) |
| 배경색 | `#081C22` (rating_background) |
| 대비 비율 | **3.69:1** |
| 판정 | PASS(UI 요소 3:1) / **FAIL(일반 텍스트 4.5:1)** |

`CircularRatingView`에서 평점 텍스트를 `rating_red` 색상으로 렌더링하는 경우 일반 텍스트 기준 실패. `rating_background` 위 12sp 텍스트라면 4.5:1 필요.

**수정 방안:** `rating_red`를 `#FF3366` 이상의 밝은 값으로 조정하거나, 이 색상을 텍스트 색이 아닌 진행 링 색으로만 사용.

---

### N-4 · 커스텀 차트 뷰 — AccessibilityDelegate 미구현

| 위치 | `BarChartView.kt`, `PieChartView.kt`, `HistogramView.kt`, `CalendarHeatmapView.kt` |
|------|-----|

네 뷰 모두 `contentDescription`을 설정하고 `importantForAccessibility = YES`로 설정되어 있어 TalkBack이 읽어줍니다. 그러나 **AccessibilityDelegate가 없어** "더블탭으로 활성화" 같은 액션 정보 제공이 불가하며, 스위치 접근에서 상호작용 방법을 알 수 없습니다.

**수정 방안 (PieChartView 예시):**
```kotlin
ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
    override fun onInitializeAccessibilityNodeInfo(
        host: View, info: AccessibilityNodeInfoCompat
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        info.className = "android.widget.ImageView"
        // 인터랙티브하지 않은 경우 clickable=false 명시
        info.isClickable = false
    }
})
```

---

### N-5 · Settings 항목 접근성 역할(Role) 미지정

| 위치 | `fragment_settings.xml` — `item_theme`, `item_language` 등 10개 LinearLayout |
|------|-----|

클릭 가능한 설정 항목들이 LinearLayout으로 구현되어 있어 TalkBack이 "레이아웃"으로 인식합니다. 사용자에게 "이 항목을 누르면 선택 다이얼로그가 열린다"는 역할 정보가 전달되지 않습니다.

**수정 방안:**
```kotlin
// SettingsFragment.kt
listOf(binding.itemTheme, binding.itemLanguage, ...).forEach { view ->
    ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(
            host: View, info: AccessibilityNodeInfoCompat
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.roleDescription = getString(R.string.accessibility_role_menu_item)
        }
    })
}
```

---

## INFO

### I-1 · `progressGoal` LinearProgressIndicator에 `importantForAccessibility="no"`

| 위치 | `fragment_stats.xml:142` |
|------|--------------------------|

월 목표 진행 막대가 접근성 트리에서 제외되어 있습니다. 바로 아래 `tvGoalStatus` TextView가 진행 상태 텍스트를 제공하므로 중복 발화 방지 목적으로 보이며 **의도적이라면 허용 가능**합니다. 다만 `tvGoalStatus`에 `accessibilityLiveRegion`을 추가하면 목표 달성 시 자동 발화가 됩니다.

---

### I-2 · RecyclerView에 `contentDescription` 설정 (비표준 패턴)

| 위치 | `PersonDetailFragment.kt:122` — `binding.rvFilmography.contentDescription = ...` |
|------|-----|

TalkBack은 RecyclerView 컨테이너가 아닌 개별 아이템을 탐색하므로, RecyclerView 자체의 `contentDescription`은 거의 발화되지 않습니다. 헤더 역할로 사용하려는 의도라면 RecyclerView 상단에 별도 `TextView` 헤더를 두는 것이 권장됩니다.

---

### I-3 · `item_recent_search.xml` — 검색어 `TextView`에 `importantForAccessibility="no"`

| 위치 | `item_recent_search.xml:28` |
|------|-----|

루트 LinearLayout의 `selectableItemBackground`가 클릭 단위이나, 루트 자체의 `contentDescription`이 없어 TalkBack이 아무것도 발화하지 않을 수 있습니다. `RecentSearchAdapter`에서 `binding.root.contentDescription`을 설정하고 있으므로 현재는 정상 동작하나, 명시적으로 루트에 `contentDescription`을 XML에도 `tools:text` 수준으로라도 문서화하는 것이 권장됩니다.

---

## 색상 대비 전체 결과

| 색상 쌍 | 대비 비율 | 판정 |
|---------|----------|------|
| `colorPrimary` (#032541) on white | 15.62:1 | ✅ PASS |
| `colorSecondary` (#01B4E4) on white | **2.43:1** | ❌ **FAIL** |
| `colorSecondary` on `colorPrimary` | 6.43:1 | ✅ PASS |
| `colorOnSurface` (#1C1B1F) on white | 17.13:1 | ✅ PASS |
| `rating_green` (#21D07A) on `rating_background` | 8.64:1 | ✅ PASS |
| `rating_yellow` (#D2D531) on `rating_background` | 11.09:1 | ✅ PASS |
| `rating_red` (#DB2360) on `rating_background` | 3.69:1 | ⚠️ UI요소 PASS / 텍스트 FAIL |
| white on `colorPrimary` (#032541) | 15.62:1 | ✅ PASS |
| white on `colorPrimaryDark` (#90CEA1, 다크) | **1.82:1** | ❌ **FAIL** |
| `colorOnSurfaceDark` (#E6E1E5) on dark surface | 14.51:1 | ✅ PASS |
| `alpha="0.7"` 텍스트 on white (근사) | ~8.45:1 | ✅ PASS |
| `alpha="0.5"` 텍스트 on white (근사) | ~3.95:1 | ❌ **FAIL** (일반 텍스트) |
| `rating_star_color` (#0D47A1) on white | 8.63:1 | ✅ PASS |

---

## 잘 구현된 사항

- **Shimmer 레이아웃** 전체 `importantForAccessibility="noHideDescendants"` 처리 ✅
- **RecyclerView 어댑터** 9종 모두 `contentDescription`을 설정하는 코드가 있음 ✅ (`MovieAdapter`·`MoviePagingAdapter`는 `MovieGridViewHolder`/`MovieListViewHolder`에서) — 2026-10-16 `grep` 확인이며 각각 루트 뷰에 설정하는지는 개별 확인하지 않음
- **FAB contentDescription** 상태 변경 시 동적 업데이트 (`DetailFragment.kt:349,358`) ✅
- **커스텀 뷰** (CircularRatingView, PieChartView, BarChartView, HistogramView, CalendarHeatmapView) `contentDescription` 데이터 기반 생성 ✅
- **장식용 ImageView** 전체 `importantForAccessibility="no"` 처리 ✅
- **스트리밍 제공자 이미지** `contentDescription = provider.providerName` 설정 (56dp 크기) ✅
- **삭제/수정 버튼** (메모, 최근 검색) 48dp 터치 타겟 + contentDescription ✅
- **Toolbar** `navigationContentDescription` 설정 (`fragment_settings.xml:15`, 등) ✅
- **리뷰 "도움이 됨" 아이콘** 기본 상태 `contentDescription`을 `tools:`가 아닌 `android:`로 지정 (`item_review.xml`, 2026-09-20 `aae0654`) ✅
- **PersonSearchAdapter** 루트 contentDescription 통합 + 자식 뷰 `NO` 처리 (중복 발화 방지) ✅

---

## 수정 우선순위

| 순위 | 이슈 | 파일 |
|------|------|------|
| 1 | M-3 LiveRegion / announceForAccessibility (HomeFragment 박스오피스 라벨만 해결, 나머지 미해결) | DetailFragment, SearchFragment |
| 2 | M-1 colorSecondary 대비 | values/themes.xml, 관련 레이아웃 |
| 3 | M-2 다크 모드 colorPrimaryDark 대비 | values/colors.xml, values-night/themes.xml |
| 4 | M-4 alpha="0.5" 텍스트 | item_person_search.xml |
| 5 | N-5 Settings 역할 설명 | SettingsFragment.kt |
| 6 | N-4 차트 뷰 AccessibilityDelegate | BarChartView, PieChartView 등 |
| 7 | N-2 Mini FAB 터치 타겟 | fragment_detail.xml |
| 8 | N-3 rating_red 텍스트 대비 | colors.xml, CircularRatingView |
| 9 | N-1 CircularRatingView 40dp | dimens.xml |

---

## 2026-10-16 기준 점검하지 못한 새 화면·변경

2026-09-16 이후 추가·변경된 UI는 이 보고서의 항목 재확인 범위 밖이며, TalkBack·대비 측정을 **하지 않았다**.

- `layout-land/fragment_search.xml` (2026-10-12, 가로 모드 검색 2단 레이아웃) — 세로 레이아웃과 view id·클래스가 같음은 `SearchLayoutConsistencyTest`가 고정하지만 접근성 속성의 일치는 확인하지 않았다.
- 홈 `fragment_home.xml`의 접히는 `AppBarLayout`(2026-10-13) — 섹션이 접힌 상태에서 TalkBack 포커스 이동과 "맨 위로"가 섹션을 펼치는 동작.
- 상세 메모 입력란의 IME 스크롤(2026-10-14)과 메모 목록(2026-10-15).
- Material You 동적 색상(2026-09-23)에서의 색상 대비(위 "동적 색상 참고").
