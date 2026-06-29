# MovieFinder i18n 감사 보고서

**감사 일시:** 2026-06-26  
**대상:** `values/strings.xml` (KO, 기준) ↔ `values-en/strings.xml` (EN)  
**총 키:** KO 301개 / EN 301개 (감사 후 완전 일치)

---

## 수정 완료 항목

### 1. 누락 키 3개 추가

영어 로케일 기기에서 해당 시나리오 발생 시 한국어 원문이 그대로 노출되던 버그.

| 키 | 추가된 영어 번역 | 사용 위치 |
|----|-----------------|-----------|
| `import_empty` | `No data to restore.` | 데이터 복원 시 백업 파일 비어있을 때 |
| `tmdb_disconnected` | `TMDB account disconnected.` | TMDB 계정 연결 해제 후 확인 메시지 |
| `tmdb_no_browser` | `No browser found to open TMDB authentication.` | TMDB OAuth 브라우저 없을 때 |

---

### 2. 의미 오역 수정

#### `notification_channel_description`

| | 내용 |
|--|------|
| **KO (원문)** | `즐겨찾기한 영화의 개봉일을 알려줍니다` |
| **EN (수정 전)** | `Get notified when your **watchlist** movies are released` |
| **EN (수정 후)** | `Get notified of release dates for your **favorited** movies` |

한국어 원문은 **즐겨찾기(favorites)** 기반 개봉일 알림을 명시하는데, 영어가 **watchlist**로 잘못 번역되어 있었습니다. Android 설정 앱 → 알림 채널 설명란에 그대로 노출됩니다.

---

### 3. 품질 개선: `stats_chart_month_format`

| | 내용 |
|--|------|
| **KO** | `%1$d월` (예: "3월") |
| **EN (수정 전)** | `%1$d mo` ("3 mo" — 어색한 약어) |
| **EN (수정 후)** | `%1$d` ("3" — 차트 축 레이블로 충분) |

월 이름 표기(Jan/Feb…)는 문자열 리소스가 아닌 `java.time.Month.getDisplayName()` 또는 `DateFormatSymbols` API로 처리하는 것이 표준입니다. 정수 그대로 노출하는 것이 더 정확하며, 향후 코드 레벨에서 월 약자가 필요하면 포맷 변경으로 대응할 수 있습니다.

---

## 추가 개선 사항

### R-1 · 복수형 처리 — ✅ 완료

6개 키를 `<string>` → `<plurals>` 리소스로 전환하고 Kotlin 호출부 8곳을 `getQuantityString()`으로 수정했습니다.

| 키 | 수정 전 EN | 수정 후 (one / other) |
|----|-----------|----------------------|
| `stats_count_format` | `%d movies` | `%d movie` / `%d movies` |
| `stats_genre_item` | `%1$d. %2$s (%3$d movies)` | `…(%3$d movie)` / `…(%3$d movies)` |
| `stats_share_total` | `Total: %d movies` | `Total: %d movie` / `Total: %d movies` |
| `stats_share_monthly` | `This month: %d movies` | `This month: %d movie` / `This month: %d movies` |
| `genre_count` | `%d Genres` | `%d Genre` / `%d Genres` |
| `reminder_count` | `%d Reminders` | `%d Reminder` / `%d Reminders` |

**생성된 파일:** `values/plurals.xml` (KO — 단수/복수 동형), `values-en/plurals.xml` (EN — one/other)

**수정된 Kotlin 호출부 (8곳):**
- `StatsFragment.kt` — 5곳 (totalWatched, monthlyWatched, genreItem, shareTotal, shareMonthly)
- `SettingsFragment.kt` — 1곳 (watchGoal display)
- `FavoriteFragment.kt` — 1곳 (reminderChip)
- `SearchFragment.kt` — 1곳 (genreChip)

> `stats_goal_progress_format` (`%1$d / %2$d movies`)는 두 인자 구조로 pluralization 대상이 모호하여 `<string>` 유지.

---

### R-2 · `person_known_for_format` 의미 중복 — MINOR

| 키 | KO | EN (현재) |
|----|-----|-----------|
| `person_known_for_format` | `%s 분야` (검색 목록용 간단 표기) | `Known for: %s` |
| `person_known_for` | `분야: %s` (상세 화면 레이블) | `Known for: %s` |

두 키가 완전히 동일한 영어 번역을 갖고 있습니다. 한국어에서는 사용 맥락이 달라 차별화되어 있지만, 영어에서 동일 표현이어도 기능적으로는 문제없습니다. 다만 `PersonSearchAdapter`의 칩 레이블로 쓰이는 `person_known_for_format`은 `%s` 단독(부서명만)이 더 간결합니다.

```xml
<!-- 선택적 개선 -->
<string name="person_known_for_format">%s</string>  <!-- "Acting" (칩용) -->
<string name="person_known_for">Known for: %s</string>  <!-- "Known for: Acting" (상세용) -->
```

---

### R-3 · `filter_year_value` 단위 표시 없음 — INFO

| 키 | KO | EN |
|----|----|----|
| `filter_year_value` | `%d년` (예: "2024년") | `%d` (예: "2024") |

영어에서 연도 필터 칩에 숫자만 표시하는 것은 UI 맥락상 충분히 이해 가능합니다. 필요 시 `%d` 대신 `"%d"` 또는 맥락 표기를 추가할 수 있으나 현재 수준은 허용 범위입니다.

---

## 번역 품질 총평

| 카테고리 | 평가 | 비고 |
|---------|------|------|
| 네비게이션 / 탭 레이블 | ✅ 우수 | 표준 영어 표현 사용 |
| 에러 메시지 | ✅ 우수 | 자연스럽고 명확 |
| 접근성 contentDescription | ✅ 우수 | TalkBack 발화 기준 자연스러움 |
| 알림 채널 / 알림 본문 | ⚠️ 양호 | `notification_channel_description` 수정 완료 |
| 온보딩 문구 | ✅ 우수 | 자연스러운 마케팅 어조 |
| 통계 화면 | ⚠️ 요주의 | 복수형 처리 미흡 (R-1) |
| TMDB 연동 | ✅ 양호 | 누락 2개 추가 완료 |
| 위젯 | ✅ 우수 | — |

---

## 파일 변경 요약

| 파일 | 변경 내용 |
|------|-----------|
| `values-en/strings.xml` | 키 3개 추가 (`import_empty`, `tmdb_disconnected`, `tmdb_no_browser`) |
| `values-en/strings.xml` | 오역 수정 (`notification_channel_description`: watchlist → favorited) |
| `values-en/strings.xml` | 품질 개선 (`stats_chart_month_format`: `%1$d mo` → `%1$d`) |

**수정 전:** EN 298키 (KO 대비 3개 누락)  
**수정 후:** EN 301키 = KO 301키 (완전 일치)
