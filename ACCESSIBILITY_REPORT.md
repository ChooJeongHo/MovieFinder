# MovieFinder 접근성 감사 보고서

**감사 일시:** 2026-06-24  
**대상:** app/src/main/res/layout/ (35개 XML), app/src/main/java/ (프레젠테이션 레이어 전체)  
**기준:** WCAG 2.1 AA, Android Accessibility Guidelines, Google Material Design Accessibility

---

## 요약

| 심각도 | 건수 |
|--------|------|
| CRITICAL | 0 |
| MAJOR | 4 |
| MINOR | 5 |
| INFO | 3 |

전반적으로 contentDescription 처리와 shimmer 레이아웃의 `importantForAccessibility="noHideDescendants"` 설정 등 기본기는 잘 갖춰져 있습니다. 그러나 **색상 대비 2건**, **LiveRegion 미사용**, **알파 텍스트 대비** 등 4개 MAJOR 이슈가 발견되었습니다.

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
| 배경색 | `#90CEA1` (values-night/colors.xml의 colorPrimaryDark) |
| 대비 비율 | **1.82:1** |
| WCAG 기준 | 4.5:1 |
| 판정 | **FAIL** |

**위치:** `app/src/main/res/values-night/themes.xml` — `colorPrimary`가 다크 모드에서 `#90CEA1`(밝은 녹색)으로 설정됨. Toolbar 배경, FAB 배경 등 `colorPrimary`를 배경으로 사용하는 컴포넌트에서 흰 텍스트/아이콘이 렌더링되면 대비 실패.

**수정 방안:**
```xml
<!-- values-night/colors.xml -->
<!-- 현재 #90CEA1(흰 배경에 어두운 텍스트가 필요한 밝은 색) →
     어두운 primary로 교체하거나, colorOnPrimary를 어두운 색으로 조정 -->
<color name="colorPrimaryDark">#FF1B5E3B</color>  <!-- 더 어두운 녹색 -->
<!-- 또는 다크 모드 Toolbar에 colorSurface 사용 + colorOnSurface 텍스트 -->
```

---

### M-3 · LiveRegion / announceForAccessibility 전면 미사용

**위치:** `app/src/main/java/com/choo/moviefinder/presentation/` 전체

`announceForAccessibility`, `accessibilityLiveRegion`, `ViewCompat.setAccessibilityLiveRegion()` 호출이 코드베이스 전체에 **0건**입니다.

**영향 범위:**

| 상황 | TalkBack 사용자 경험 |
|------|---------------------|
| 에러 뷰 표시 (`layout_error.xml`) | 에러 발생을 인지 불가 |
| 검색 결과 로딩 완료 | 결과가 나타났는지 알 수 없음 |
| FAB 상태 변경 (즐겨찾기 추가/제거) | contentDescription은 변경되나 변경 사실이 발화되지 않음 |
| Snackbar 표시 | 스낵바 메시지 발화 안 됨 |
| 빈 상태/오류 → 정상 전환 | 전환 사실 미인지 |

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
| 위치 | `item_person_search.xml:56` — `tv_known_for_titles` |
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

| 위치 | `fragment_detail.xml:509` |
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

| 위치 | `fragment_settings.xml` — `item_theme`, `item_language` 등 11개 LinearLayout |
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
- **RecyclerView 어댑터** 8종 전체 루트 `contentDescription` 동적 설정 ✅
- **FAB contentDescription** 상태 변경 시 동적 업데이트 (`DetailFragment.kt:337,346`) ✅
- **커스텀 뷰** (CircularRatingView, PieChartView, BarChartView, HistogramView, CalendarHeatmapView) `contentDescription` 데이터 기반 생성 ✅
- **장식용 ImageView** 전체 `importantForAccessibility="no"` 처리 ✅
- **스트리밍 제공자 이미지** `contentDescription = provider.providerName` 설정 (56dp 크기) ✅
- **삭제/수정 버튼** (메모, 최근 검색) 48dp 터치 타겟 + contentDescription ✅
- **Toolbar** `navigationContentDescription` 설정 (`fragment_settings.xml:16`, 등) ✅
- **PersonSearchAdapter** 루트 contentDescription 통합 + 자식 뷰 `NO` 처리 (중복 발화 방지) ✅

---

## 수정 우선순위

| 순위 | 이슈 | 파일 |
|------|------|------|
| 1 | M-3 LiveRegion / announceForAccessibility | DetailFragment, SearchFragment, HomeFragment |
| 2 | M-1 colorSecondary 대비 | values/themes.xml, 관련 레이아웃 |
| 3 | M-2 다크 모드 colorPrimaryDark 대비 | values-night/colors.xml |
| 4 | M-4 alpha="0.5" 텍스트 | item_person_search.xml |
| 5 | N-5 Settings 역할 설명 | SettingsFragment.kt |
| 6 | N-4 차트 뷰 AccessibilityDelegate | BarChartView, PieChartView 등 |
| 7 | N-2 Mini FAB 터치 타겟 | fragment_detail.xml |
| 8 | N-3 rating_red 텍스트 대비 | colors.xml, CircularRatingView |
| 9 | N-1 CircularRatingView 40dp | dimens.xml |
