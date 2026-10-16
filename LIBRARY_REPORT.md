# MovieFinder 라이브러리 현황 리포트

> 작성일: 2026-10-16 (2026-09-16 리포트 갱신, 최초 2026-06-22)
> 조사 대상: Paging 3, Hilt, Coil, Room (+ Room 3.x 배포 상태)

---

## 요약

| 라이브러리 | 현재 버전 | 최신 안정 버전 | 업그레이드 권장 | 보안 이슈 |
|---|---|---|---|---|
| Paging 3 | 3.5.2 | 3.5.3 (2026-10-12 게시) | 패치 업그레이드 가능 | 없음* |
| Hilt (Dagger) | 2.60.1 | 2.60.1 | — 최신 | 없음* |
| Coil | 3.6.3 | 3.6.3 | — 최신 | 없음* |
| Room | 2.8.5 | 2.8.5 (2.x) / 3.0.3 (3.x 정식, 별도 그룹 `androidx.room3`) | 2.x는 최신 / 3.x 이전은 미결정 | 없음* |

> 2026-10-16 Google Maven / Maven Central `maven-metadata.xml`을 `curl`로 직접 조회해 확인했다. Paging만 올릴 수 있는 새 버전이 있고(3.5.2 → 3.5.3), 나머지 3개는 최신이다. 열린 Dependabot PR은 #115(Kotlin 2.4.x) 하나뿐이다.
>
> \* 보안 이슈 열은 2026-09-16 조사 결과 그대로이며 **이번 갱신에서는 CVE를 재조사하지 않았다**.

---

## 1. Paging 3

### 현황
- **현재 버전**: `3.5.2`
- **최신 안정 버전**: `3.5.3` — 2026-10-12 게시(POM `Last-Modified` 기준). 3.5.3의 변경 내용(릴리스 노트)은 이번에 확인하지 않았다.
- 2026-09-16 판의 `3.5.1`에서 Dependabot이 3.5.2까지 올렸고, 그 뒤 3.5.3이 나왔다.

### MovieFinder 영향도
- 호환성 문제 보고된 바 없음, `MoviePagingSource`/`DiscoverPagingSource`/`TrendingPagingSource`/`UpcomingPagingSource`/`MovieRemoteMediator` 정상 동작 중
- 패치 업그레이드의 영향은 확인하지 않았다 — 올릴 때는 `PagingEmptyStateRegressionTest`(실제 Pager 파이프라인)를 포함한 유닛 테스트 전체로 회귀 확인이 필요하다.

### 보안 이슈
- 알려진 CVE 없음 (2026-09-16 조사 기준, 이번에 재조사하지 않음)

---

## 2. Hilt (Dagger)

### 현황
- **현재 버전**: `2.60.1` (2026-06-22 리포트 시점 `2.59.2`에서 상승)
- **최신 안정 버전**: `2.60.1` — **최신**

### MovieFinder 영향도
- **Kotlin 버전 제약**: `CLAUDE.md`에 따르면 Kotlin 2.4.x는 (1) Hilt 2.59.2 기준 `kotlin-metadata-jvm` 최대 2.3.0까지만 지원하는 문제(2.60.1에서도 같은지는 확인하지 못함)와, (2) Kotlin 2.4.x + 현재 AGP/Lint 조합에서 `InferredThreadDetector` 초기화 실패로 `lintAnalyzeDebug` 계열 3개 태스크가 전부 실패하는 별개 문제가 동시에 있어 **이중으로 보류 중**(Dependabot PR #92, #110, #115 — #115는 열린 채 보류). (2)는 AGP 9.4.1에서도 재현되는 것을 2026-09-28에 다시 확인했다. AGP 최신 정식판은 9.4.1이고 9.5.0은 알파뿐이라(2026-10-05) AGP 업그레이드로는 풀 수 없다. Hilt 버전이 올라가도 Lint 쪽 이슈가 해결되지 않는 한 Kotlin 2.4.x 승급은 불가.
- (1)의 `kotlin-metadata-jvm` 한도는 `CLAUDE.md`에 2.59.2 기준으로 기록된 값이며 2.60.1에서 유지되는지는 빌드로 확인하지 않았다. 참고로 Maven Central POM에서 `dagger-compiler` 2.59.2는 `kotlin-metadata-jvm` 2.2.20에, 2.60.1은 2.3.21에 의존한다(2026-10-16 조회) — 한도가 바뀌었을 수 있으나 이는 의존성 선언만 본 추정이다. Kotlin 2.4.x를 다시 시도할 때 (1)을 다시 측정할 것.
- 현재 프로젝트 Kotlin 버전: `2.3.21` (안정적으로 유지)

### 보안 이슈
- 알려진 CVE 없음 (2026-09-16 조사 기준, 이번에 재조사하지 않음)

---

## 3. Coil

### 현황
- **현재 버전**: `3.6.3` (2026-06-22 리포트 시점 `3.4.0`에서 세 단계 상승, 2026-09-28 Dependabot coil 그룹 업데이트로 3.6.2 → 3.6.3)
- **최신 안정 버전**: `3.6.3` — **최신**

### 정정 사항
- 2026-06-22 리포트는 "Coil 3.5.0은 Kotlin 2.4.0을 필수로 요구해 즉시 업그레이드 불가"라고 기록했으나, 현재 프로젝트는 **Coil 3.6.3 + Kotlin 2.3.21** 조합으로 정상 빌드·동작 중이다(2026-09-16 판은 3.6.2에서 같은 결과). 즉 실제로는 그 시점에 우려했던 만큼의 Kotlin 강제 요구사항이 (적어도 3.6.x 기준) 적용되지 않았던 것으로 보인다 — 정확한 각 버전별 최소 Kotlin 요구사항은 필요 시 Coil 공식 릴리스 노트에서 재확인할 것(이 리포트에서는 실측 결과만 근거로 삼음).

### MovieFinder 영향도
- 메모리 25% / 디스크 50MB 캐시 설정, `@ImageOkHttpClient` Certificate Pinning 적용 등 기존 구성 그대로 호환

### 보안 이슈
- 알려진 CVE 없음 (2026-09-16 조사 기준, 이번에 재조사하지 않음)

---

## 4. Room

### 현황
- **현재 버전**: `2.8.5` (2026-06-22 리포트 시점 `2.8.4`에서 패치 업)
- **최신 2.x 안정 버전**: `2.8.5` — **최신** (`androidx.room:room-runtime` 메타데이터, 2026-10-16)
- **Room 3.x: 정식판이 배포되어 있다.** 3.x는 아티팩트 그룹이 `androidx.room3`로 분리되어 있어 `androidx.room:room-runtime` 메타데이터에는 나타나지 않는다. 2026-10-16 `androidx.room3:room3-runtime` 메타데이터의 버전 목록은 `3.0.0-alpha01` ~ `alpha06`, `3.0.0-rc01`, `3.0.0`, `3.0.1`, `3.0.2`, `3.0.3`, `3.1.0-alpha01`이다. POM `Last-Modified`는 3.0.0이 2026-07-01, 3.0.3이 2026-09-09다.

### 정정 사항
- 2026-09-16 판은 "Room 3.x: 2026-09-16 기준 Google Maven 메타데이터 재확인 결과 **여전히 stable 미출시**(2.x 계열 최신판까지만 존재)"라고 적었으나, 위 날짜대로 3.0.0 정식판은 그보다 두 달 이상 앞선 2026-07-01부터 있었다. 그 판이 어떤 아티팩트를 조회했는지는 기록이 없어 확인하지 못했다. "2.x 계열 최신판까지만 존재"라는 서술은 `androidx.room` 그룹만 본 결과와 일치하지만, 그렇게 조회했는지는 추정일 뿐이다.

### MovieFinder 영향도
- `MovieDatabase`는 현재 **버전 25** (2026-09-16 판의 24에서 상승 — 2026-09-29 앱 언어 연동 때 한국어 제목 캐시 테이블 `movie_korean_title_cache` 추가)
- `@Dao` 14개 / `@Entity` 15개. 스키마 마이그레이션 객체는 12개(12→13 ~ 23→24)이고 **24→25 Migration은 없다**(자세한 내용은 `ONBOARDING.md` 실수 8 참고)
- `FavoriteMovieDao`/`WatchlistDao`/`WatchHistoryDao`의 `abstract class + @Transaction` 패턴, `withTransaction` 모킹 이슈 등 기존 대응 방식 그대로 유효 (Room 3.x로 옮길 경우 재검토 대상)
- 3.x 이전을 할지는 **미결정**이다. 이번 갱신에서는 Kotlin 2.3.21·KSP·`room-paging`·`sqlite-bundled`와의 호환성이나 API 차이를 조사하지 않았다.

### 보안 이슈
- Room 라이브러리 자체 CVE 없음 (2026-09-16 조사 기준, 이번에 재조사하지 않음)
- SQLite 기반이므로 SQL Injection 방어는 Room의 파라미터 바인딩으로 이미 처리됨

---

## 5. Android 플랫폼 보안 이슈

> 2026-06-30 리포트(`NETWORK_SECURITY_REPORT.md`)에서 네트워크 계층 보안은 별도로 다루므로, 이 섹션은 2026-06-22 시점 기록을 참고용으로 남긴다. OS 레벨 CVE 최신 현황은 2026-09-16과 2026-10-16 갱신 모두에서 재조사하지 않았다(범위 밖) — 필요 시 별도 보안 리포트 갱신 시 재확인 권장.

| CVE | 심각도 | 영향 | 패치 |
|---|---|---|---|
| CVE-2025-48595 | High (활발히 악용 중) | Android Framework 정수 오버플로 → 권한 상승 | 2026-06-01 패치 |
| CVE-2026-0006 | Critical (RCE) | Android 16 대상 원격 코드 실행 | 2026-03 패치 |
| CVE-2026-0047 | Critical (EoP) | Framework 권한 상승 | 2026-03 패치 |

**MovieFinder 대응**:
- 앱 자체 취약점 아님. 사용자 기기의 보안 패치 수준에 의존

---

## 6. 업그레이드 로드맵 (2026-10-16 갱신)

### 즉시 가능
- Paging `3.5.2` → `3.5.3` (패치, 릴리스 노트 미확인 — 유닛 테스트 전체로 회귀 확인 후 적용)
- 그 외 Hilt/Coil/Room(2.x)은 이미 최신 안정 버전

### Kotlin 2.4.x 승급 (중장기, 이중 블로커 해소 필요)
```toml
# 아래 두 문제가 모두 해소된 뒤에만 진행
# 1) Hilt의 kotlin-metadata-jvm 2.4.0 지원
# 2) AGP/Lint InferredThreadDetector 초기화 실패(lintAnalyzeDebug 계열) 해소
kotlin = "2.4.x"
```

### Room 3.x 마이그레이션 (정식판 `3.0.x` 존재 — 착수 여부는 호환성 조사 후 결정)
- 먼저 조사: Kotlin 2.3.21/KSP 2.3.12와의 호환성, `room-paging`·`sqlite-bundled` 대응 아티팩트, 2.x → 3.x API 차이
- 착수한다면: `androidx.room3` artifact 전환, DAO abstract class/`@Transaction` 패턴, 테스트 코드, 스키마 마이그레이션 콜백 일괄 검토
- 24→25 Migration 공백(위 4절)은 3.x 이전 여부와 무관하게 별도로 정리가 필요한 항목이다.

---

*조사 도구: Google Maven / Maven Central maven-metadata.xml 직접 조회(`curl`) + `gradle/libs.versions.toml` 실측. 게시일은 POM `Last-Modified` 헤더 기준(레지스트리 메타데이터에는 날짜가 없음).*
