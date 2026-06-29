# MovieFinder 라이브러리 현황 리포트

> 작성일: 2026-06-22  
> 조사 대상: Paging 3, Hilt, Coil, Room

---

## 요약

| 라이브러리 | 현재 버전 | 최신 안정 버전 | 업그레이드 권장 | 보안 이슈 |
|---|---|---|---|---|
| Paging 3 | 3.4.2 | **3.5.0** | ✅ 가능 | 없음 |
| Hilt (Dagger) | 2.59.2 | 2.59.2 | — 최신 | 없음 |
| Coil | 3.4.0 | **3.5.0** | ⚠️ Kotlin 제약 | 없음 |
| Room | 2.8.4 | 2.8.4 (2.x) / 3.0.0-rc01 (3.x) | ⚠️ 3.x는 Breaking | 없음 |

---

## 1. Paging 3

### 현황
- **현재 버전**: `3.4.2`
- **최신 안정 버전**: `3.5.0` (2026-05-06 릴리스)
- **업데이트 권장도**: ✅ 즉시 업그레이드 가능

### 버전별 변경 내용

#### 3.5.0 (2026-05-06)
- 안정화 릴리스. 3.5.0-alpha01 ~ rc01 사이클에서 수렴된 버그 수정 포함
- KMP(Kotlin Multiplatform) 타겟 추가: JVM(Android + Desktop), Native(Linux, iOS, watchOS, tvOS, macOS, MinGW), Web(JS/Wasm)
- macosX64, iosX64, watchosX64, tvosX64 타겟 제거 (JetBrains deprecation에 맞춤)

#### 3.4.2 (현재, 2026-03-11)
- 버그 수정 릴리스

#### 3.4.0 (2026-01-28)
- `paging-common`, `paging-testing`, `paging-compose`에 멀티플랫폼 타겟 추가

### MovieFinder 영향도
- **호환성**: 완전 호환. API 변경 없음
- **업그레이드 방법**: `libs.versions.toml`에서 `paging = "3.5.0"` 변경

```toml
# libs.versions.toml
paging = "3.5.0"
```

### 보안 이슈
- 알려진 CVE 없음

---

## 2. Hilt (Dagger)

### 현황
- **현재 버전**: `2.59.2`
- **최신 안정 버전**: `2.59.2` — **이미 최신**
- **androidx.hilt**: `1.3.0` (stable) / `1.4.0-rc01` (RC, 2026-06-03)

### 최근 변경 내용

#### Dagger 2.59.2 (2026-02-20)
- `HiltSyncTask`가 AGP 9에서 느린 증분 빌드를 유발하는 문제 수정 (`DefaultTask`로 변경)
- Hilt의 "android-classes" 변환을 attribute rules로 교체
- nullability 지원 개선 (Switching Providers)

#### Dagger 2.59.1 (2026-02)
- `jetifierEnabled=true` + AGP 9 조합 컴파일 에러 수정

#### Dagger 2.59 (2026-01-21)
- **Breaking**: Hilt Gradle Plugin 사용 시 AGP 9 필수 요구
- AGP 9 공식 지원 추가

#### androidx.hilt 1.4.0-rc01 (2026-06-03)
- `rememberHiltViewModelFactory()` API 단순화 (ViewModelStoreOwner 파라미터 제거)
- Compose compileSdk 업데이트

### MovieFinder 영향도
- **현재 상태 양호**: `2.59.2`는 AGP 9.2.1과 호환 확인됨
- **⚠️ Kotlin 제약**: Hilt 2.59.2는 `kotlin-metadata-jvm` 최대 2.3.0 지원 → Kotlin 2.4.0 업그레이드 불가

### 보안 이슈
- 알려진 CVE 없음

---

## 3. Coil

### 현황
- **현재 버전**: `3.4.0`
- **최신 안정 버전**: `3.5.0` (2026-06-10 릴리스)
- **업데이트 권장도**: ⚠️ Kotlin 2.4.0 의존성으로 **즉시 업그레이드 불가**

### 버전별 변경 내용

#### 3.5.0 (2026-06-10) — 주요 변경
- **Breaking**: Android minSdk 21 → **23** 상향 (MovieFinder minSdk 24이므로 호환)
- **Breaking**: iosX64, macosX64 타겟 제거
- **의존성 업데이트**: Kotlin **2.4.0**, Compose 1.11.1, Okio 3.17.0, Skiko 0.144.6
- WebP 크기 추출 JS/WASM 지원 (전체 Skia 디코딩 없이)
- `memoryCacheMaxSizePercentWhileInBackground` API 실험적 annotation 제거 (안정화)
- `CacheStrategy` 구현의 캐시된 실패 응답(예: 만료된 캐시 401) 처리 수정
- `CoroutineDispatcher` 대신 `ContinuationInterceptor` 사용으로 코루틴 통합 개선

#### 3.4.0 (2026-02-24)
- 안정화 릴리스

#### 3.3.0 (2025-07-22)
- `memoryCacheMaxSizePercentWhileInBackground`: 앱 백그라운드 시 메모리 캐시 크기 제한 API 추가 (기본 비활성화)

### MovieFinder 영향도
- **즉시 업그레이드 차단 요인**: Coil 3.5.0은 Kotlin 2.4.0 필수 → Hilt 2.59.2가 Kotlin 2.3.x까지만 지원하므로 동시 업그레이드 불가
- **Hilt가 Kotlin 2.4.0을 지원하는 버전 출시 후 함께 업그레이드** 필요
- minSdk 24 (현재) ≥ 23 (3.5.0 요구) → 조건 충족

```kotlin
// Hilt가 Kotlin 2.4.0 지원 시 함께 업그레이드
coil = "3.5.0"
kotlin = "2.4.0"  // Hilt 호환 버전 확인 필요
```

### 보안 이슈
- 알려진 CVE 없음

---

## 4. Room

### 현황
- **현재 버전**: `2.8.4`
- **최신 2.x 안정 버전**: `2.8.4` — **이미 최신**
- **Room 3.x**: `3.0.0-rc01` (2026-06-17, 별도 artifact `androidx.room3`)
- **업데이트 권장도**: ⚠️ 2.x는 최신 유지 중 / 3.x는 Breaking Changes로 단기 마이그레이션 불권장

### Room 3.0 주요 변경사항 (참고용)

Room 3.0은 `androidx.room3`라는 **새 artifact**로 분리되어 발표됨:

| 항목 | Room 2.x | Room 3.0 |
|---|---|---|
| Artifact | `androidx.room` | `androidx.room3` |
| 어노테이션 처리 | KAPT + KSP | **KSP 전용** (KAPT 폐지) |
| 생성 코드 | Java | **Kotlin** |
| 기본 API | Sync + RxJava + Coroutines | **Coroutines-first** |
| DB 드라이버 | SupportSQLiteDatabase | **SQLiteDriver** (SupportSQLite 제거) |
| KMP 타겟 | Android, iOS, JVM, Native | **+ Web (Wasm/JS)** |
| minSdk | 21 | **23** |

**3.0.0-rc01 (2026-06-17) 신규 기능**:
- DAO 쿼리 결과 data class에서 기본값 파라미터 지원
- `@TypeConverter` → `@ColumnTypeConverter` 이름 변경
- `@ProvidedDaoReturnTypeConverter` API 추가
- `PrimaryKey.algorithm` 프로퍼티 추가 (자동 생성 PK 알고리즘 지정)
- `SupportSQLiteDatabase` 호환 래퍼: `room3-sqlite-wrapper` artifact (마이그레이션 편의)

### MovieFinder 영향도 (Room 3.x 마이그레이션 시)
- **KAPT → KSP**: 이미 KSP 사용 중 ✅
- **SupportSQLiteDatabase 제거**: `database.withTransaction {}` 패턴은 `mockkStatic` 방식으로 테스트 중 → SQLiteDriver 기반으로 재작성 필요
- **`@Transaction` DAO abstract class 패턴**: Room 3.x에서 호환 여부 검토 필요
- **DB 버전 21 스키마**: 마이그레이션 콜백 API 변경으로 재검토 필요
- **결론**: Room 3.0 stable 릴리스 후 별도 마이그레이션 작업 계획 수립 권장

### 보안 이슈
- Room 라이브러리 자체 CVE 없음
- SQLite 기반이므로 SQL Injection 방어는 Room의 파라미터 바인딩으로 이미 처리됨

---

## 5. Android 플랫폼 보안 이슈

Jetpack 라이브러리 자체의 CVE는 발견되지 않았으나, 최근 Android OS 수준 취약점이 확인됨:

| CVE | 심각도 | 영향 | 패치 |
|---|---|---|---|
| CVE-2025-48595 | High (활발히 악용 중) | Android Framework 정수 오버플로 → 권한 상승 | 2026-06-01 패치 |
| CVE-2026-0006 | Critical (RCE) | Android 16 대상 원격 코드 실행 | 2026-03 패치 |
| CVE-2026-0047 | Critical (EoP) | Framework 권한 상승 | 2026-03 패치 |

**MovieFinder 대응**:
- 앱 자체 취약점 아님. 사용자 기기의 보안 패치 수준에 의존
- API 레벨별 조건부 코드에서 추가 입력 검증이 필요한 부분은 없음

---

## 6. 업그레이드 로드맵 권장사항

### 즉시 가능 (단기)
```toml
# libs.versions.toml
paging = "3.5.0"  # 3.4.2 → 3.5.0, 호환성 문제 없음
```

### Hilt Kotlin 2.4.0 지원 시 (중기)
```toml
# Hilt가 Kotlin 2.4.0과 호환되는 버전 출시 후
kotlin = "2.4.0"
coil = "3.5.0"
hilt = "<kotlin-2.4.0-compatible-version>"
```

### Room 3.x 마이그레이션 (장기, 별도 계획 필요)
- Room 3.0 stable 릴리스 확인
- `androidx.room3` artifact로 전환
- DAO abstract class, `@Transaction` 패턴, 테스트 코드 일괄 검토
- 스키마 마이그레이션 콜백 재작성

---

*조사 도구: Exa Web Search + Android Developers 공식 릴리스 노트*
