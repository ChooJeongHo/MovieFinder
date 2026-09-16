# MovieFinder 라이브러리 현황 리포트

> 작성일: 2026-09-16 (2026-06-22 리포트 갱신)
> 조사 대상: Paging 3, Hilt, Coil, Room

---

## 요약

| 라이브러리 | 현재 버전 | 최신 안정 버전 | 업그레이드 권장 | 보안 이슈 |
|---|---|---|---|---|
| Paging 3 | 3.5.1 | 3.5.1 | — 최신 | 없음 |
| Hilt (Dagger) | 2.60.1 | 2.60.1 | — 최신 | 없음 |
| Coil | 3.6.2 | 3.6.2 | — 최신 | 없음 |
| Room | 2.8.5 | 2.8.5 (2.x) / 3.x 미출시 | — 최신 (2.x 계열) | 없음 |

> 4개 라이브러리 모두 2026-06-22 리포트 이후 Dependabot이 자동으로 최신 안정 버전까지 끌어올린 상태이며, 2026-09-16 기준 Google Maven / Maven Central 메타데이터로 재확인한 결과 **추가로 올릴 버전이 없다**(전부 최신).

---

## 1. Paging 3

### 현황
- **현재 버전**: `3.5.1`
- **최신 안정 버전**: `3.5.1` — **최신**
- 2026-06-22 리포트 시점 권장했던 `3.4.2 → 3.5.0` 업그레이드는 이미 적용됨(현재는 3.5.1까지 진행)

### MovieFinder 영향도
- 호환성 문제 보고된 바 없음, `MoviePagingSource`/`DiscoverPagingSource`/`TrendingPagingSource`/`MovieRemoteMediator` 정상 동작 중

### 보안 이슈
- 알려진 CVE 없음

---

## 2. Hilt (Dagger)

### 현황
- **현재 버전**: `2.60.1` (2026-06-22 리포트 시점 `2.59.2`에서 상승)
- **최신 안정 버전**: `2.60.1` — **최신**

### MovieFinder 영향도
- **Kotlin 버전 제약 여전히 유효**: `CLAUDE.md`에 따르면 Kotlin 2.4.x는 (1) Hilt 2.60.1이 `kotlin-metadata-jvm` 최대 2.3.0까지만 지원하는 문제와, (2) Kotlin 2.4.x + 현재 AGP/Lint 조합에서 `InferredThreadDetector` 초기화 실패로 `lintAnalyzeDebug` 계열 3개 태스크가 전부 실패하는 별개 문제가 동시에 있어 **이중으로 보류 중**(PR #92, #110, 2026-07-27 확인). Hilt 버전이 올라가도 Lint 쪽 이슈가 해결되지 않는 한 Kotlin 2.4.x 승급은 불가.
- 현재 프로젝트 Kotlin 버전: `2.3.21` (안정적으로 유지)

### 보안 이슈
- 알려진 CVE 없음

---

## 3. Coil

### 현황
- **현재 버전**: `3.6.2` (2026-06-22 리포트 시점 `3.4.0`에서 두 단계 상승)
- **최신 안정 버전**: `3.6.2` — **최신**

### 정정 사항
- 2026-06-22 리포트는 "Coil 3.5.0은 Kotlin 2.4.0을 필수로 요구해 즉시 업그레이드 불가"라고 기록했으나, 현재 프로젝트는 **Coil 3.6.2 + Kotlin 2.3.21** 조합으로 이미 정상 빌드·동작 중이다. 즉 실제로는 그 시점에 우려했던 만큼의 Kotlin 강제 요구사항이 (적어도 3.6.2 시점 기준) 적용되지 않았던 것으로 보인다 — 정확한 각 버전별 최소 Kotlin 요구사항은 필요 시 Coil 공식 릴리스 노트에서 재확인할 것(이 리포트에서는 실측 결과만 근거로 삼음).

### MovieFinder 영향도
- 메모리 25% / 디스크 50MB 캐시 설정, `@ImageOkHttpClient` Certificate Pinning 적용 등 기존 구성 그대로 호환

### 보안 이슈
- 알려진 CVE 없음

---

## 4. Room

### 현황
- **현재 버전**: `2.8.5` (2026-06-22 리포트 시점 `2.8.4`에서 패치 업)
- **최신 2.x 안정 버전**: `2.8.5` — **최신**
- **Room 3.x**: 2026-09-16 기준 Google Maven 메타데이터 재확인 결과 **여전히 stable 미출시**(2.x 계열 최신판까지만 존재) — 2026-06-22 리포트의 "3.0.0-rc01" 이후 진행 상황은 이번 조사에서 확인되지 않음, 3.x 마이그레이션 계획은 계속 보류 상태 유지가 타당

### MovieFinder 영향도
- `MovieDatabase`는 현재 **버전 24**로 진행 중(2026-06-22 리포트 작성 시점 대비 스키마 마이그레이션 다수 누적)
- DAO 15개 / Entity 14개로 증가(리포트 작성 시점 대비 항목 늘어남)
- `FavoriteMovieDao`/`WatchlistDao`/`WatchHistoryDao`의 `abstract class + @Transaction` 패턴, `withTransaction` 모킹 이슈 등 기존 대응 방식 그대로 유효 — Room 3.0이 stable 릴리스되기 전까지는 재검토 불필요

### 보안 이슈
- Room 라이브러리 자체 CVE 없음
- SQLite 기반이므로 SQL Injection 방어는 Room의 파라미터 바인딩으로 이미 처리됨

---

## 5. Android 플랫폼 보안 이슈

> 2026-06-30 리포트(`NETWORK_SECURITY_REPORT.md`)에서 네트워크 계층 보안은 별도로 다루므로, 이 섹션은 2026-06-22 시점 기록을 참고용으로 남긴다. OS 레벨 CVE 최신 현황은 이번 갱신에서 재조사하지 않았다(범위 밖) — 필요 시 별도 보안 리포트 갱신 시 재확인 권장.

| CVE | 심각도 | 영향 | 패치 |
|---|---|---|---|
| CVE-2025-48595 | High (활발히 악용 중) | Android Framework 정수 오버플로 → 권한 상승 | 2026-06-01 패치 |
| CVE-2026-0006 | Critical (RCE) | Android 16 대상 원격 코드 실행 | 2026-03 패치 |
| CVE-2026-0047 | Critical (EoP) | Framework 권한 상승 | 2026-03 패치 |

**MovieFinder 대응**:
- 앱 자체 취약점 아님. 사용자 기기의 보안 패치 수준에 의존

---

## 6. 업그레이드 로드맵 (2026-09-16 갱신)

### 즉시 가능
- 없음 — Paging/Hilt/Coil/Room 4종 모두 이미 최신 안정 버전

### Kotlin 2.4.x 승급 (중장기, 이중 블로커 해소 필요)
```toml
# 아래 두 문제가 모두 해소된 뒤에만 진행
# 1) Hilt의 kotlin-metadata-jvm 2.4.0 지원
# 2) AGP/Lint InferredThreadDetector 초기화 실패(lintAnalyzeDebug 계열) 해소
kotlin = "2.4.x"
```

### Room 3.x 마이그레이션 (장기, stable 미출시로 보류)
- Room 3.0 stable 릴리스 확인 후 재착수
- `androidx.room3` artifact 전환, DAO abstract class/`@Transaction` 패턴, 테스트 코드, 스키마 마이그레이션 콜백 일괄 검토

---

*조사 도구: Google Maven / Maven Central maven-metadata.xml 직접 조회(WebFetch) + `gradle/libs.versions.toml` 실측*
