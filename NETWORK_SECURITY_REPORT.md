# Network Security Report — MovieFinder

> Generated: 2026-06-30

---

## 현황 요약

MovieFinder는 TMDB API(`api.themoviedb.org`) 및 이미지 서버(`image.tmdb.org`)와 통신하는 Android 앱입니다.
이 리포트는 네트워크 보안 전반을 점검하고, 이번 세션에서 적용된 개선 사항을 기록합니다.

---

## 기존 보안 조치 (이미 구현됨)

| 항목 | 구현 위치 | 상태 |
|---|---|---|
| HTTPS 강제 적용 | `network_security_config.xml` `cleartextTrafficPermitted="false"` | ✅ |
| OkHttp Certificate Pinning (API) | `NetworkModule.kt` — leaf + intermediate SHA-256 | ✅ |
| OkHttp Certificate Pinning (이미지) | `NetworkModule.kt` `@ImageOkHttpClient` — leaf + intermediate | ✅ |
| Widget Certificate Pinning | `PopularMoviesRemoteViewsFactory.kt` singleton OkHttpClient | ✅ |
| 디버그 빌드 핀 비활성화 | `if (!BuildConfig.DEBUG) certificatePinner(...)` | ✅ |
| HTTP 로깅 디버그 전용 | `src/debug/` vs `src/release/` 소스셋 분리 (`OkHttpDebugPlugin.kt`) | ✅ |
| API 키 URL 직접 포함 방지 | OkHttp Interceptor로 query param 주입 | ✅ |
| 429 Rate-Limit 재시도 | `Retry-After` 헤더 존중, 최대 5초 대기 후 1회 재시도 | ✅ |
| 지수 백오프 | `RemoteMediator` API 호출에 `withExponentialBackoff()` 적용 | ✅ |
| 인증서 핀 만료 모니터링 | `cert-pin-check.yml` CI 워크플로우 (매주 월요일 cron) | ✅ |

---

## 이번 세션 개선 사항

### 1. NSC OS 레벨 Certificate Pinning 추가

**파일**: `app/src/main/res/xml/network_security_config.xml`

**변경 전**: base-config에 cleartext 차단만 설정. 핀 검증은 앱 레벨(OkHttp)에만 의존.

**변경 후**: `<domain-config>`에 `<pin-set>` 추가 — Android OS/TLS 레벨에서 핀 검증 수행.

```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">themoviedb.org</domain>
    <domain includeSubdomains="true">tmdb.org</domain>
    <pin-set expiration="2027-06-30">
        <pin digest="SHA-256">QfyoR20v8hyYX7L+ikLzM/euPGSDl67gFFcor/sROMs=</pin> <!-- api leaf -->
        <pin digest="SHA-256">G9LNNAql897egYsabashkzUCTEJkWBzgoEtk8X/678c=</pin> <!-- api inter -->
        <pin digest="SHA-256">D9+FUQAcRTKvnv4RFbvEOfxIdAaqGJVOtOKBUZPFlak=</pin> <!-- image leaf -->
        <pin digest="SHA-256">LoMHBotttiDko50Gi13uXW71eIy7LAttI+rYT8wXF4w=</pin> <!-- image inter -->
    </pin-set>
</domain-config>

<!-- 디버그 빌드: 핀 검증 비활성화 (Charles Proxy 등 사용 가능) -->
<debug-overrides overridePins="true">
    <trust-anchors>
        <certificates src="user"/>
    </trust-anchors>
</debug-overrides>
```

**효과**: 루팅 기기에서 OkHttp 레이어를 우회하더라도 OS 레벨에서 MITM 공격 차단.

---

### 2. Bearer 토큰 로그 노출 차단

**파일**: `app/src/debug/java/com/choo/moviefinder/core/util/OkHttpDebugPlugin.kt`

**변경 전**: `HttpLoggingInterceptor.Level.HEADERS`가 `Authorization: Bearer <token>` 헤더를 Logcat에 평문 출력.

**변경 후**: `redactHeader("Authorization")` 추가 — Authorization 헤더값이 `██` 으로 마스킹.

```kotlin
.apply {
    level = HttpLoggingInterceptor.Level.HEADERS
    redactHeader("Authorization")
}
```

**효과**: TMDB v4 Read Access Token이 디버그 로그(`adb logcat`)에 노출되지 않음.

---

### 3. TMDB v4 API 디버그 로깅 추가

**파일**: `app/src/main/java/com/choo/moviefinder/di/NetworkModule.kt`

**변경 전**: `TmdbV4OkHttpClient`에 `addDebugLogging()` 누락 — v4 API 요청/응답이 디버그에서 보이지 않음.

**변경 후**: `.addDebugLogging()` 추가 (Authorization 헤더는 위 변경으로 자동 마스킹).

---

## 아키텍처: 다층 보안 모델

```
[ Android OS / TLS Layer ]
  └─ network_security_config.xml
       ├─ cleartextTrafficPermitted="false"  → HTTP 전면 차단
       └─ pin-set (themoviedb.org, tmdb.org) → OS 레벨 MITM 차단

[ Application Layer / OkHttp ]
  ├─ CertificatePinner (api.themoviedb.org)  → 앱 레벨 핀 검증
  ├─ CertificatePinner (image.tmdb.org)      → 이미지 서버 핀 검증
  ├─ CertificatePinner (widget singleton)    → 위젯 OkHttpClient 핀 검증
  └─ ApiKey Interceptor                      → 쿼리 파라미터 주입 (URL 직접 포함 금지)

[ Logging Layer ]
  ├─ Debug: HEADERS 레벨, Authorization 마스킹
  └─ Release: 로깅 없음 (no-op OkHttpDebugPlugin)
```

---

## 인증서 핀 갱신 절차

핀 만료(`expiration: 2027-06-30`) 또는 `cert-pin-check.yml` CI 알림 발생 시:

```bash
# api.themoviedb.org 신규 핀 추출
echo | openssl s_client -connect api.themoviedb.org:443 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64

# image.tmdb.org 신규 핀 추출
echo | openssl s_client -connect image.tmdb.org:443 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

갱신 시 수정 파일:
- `NetworkModule.kt` — `PIN_API_LEAF`, `PIN_API_INTER`, `PIN_IMAGE_LEAF`, `PIN_IMAGE_INTER` 상수
- `PopularMoviesRemoteViewsFactory.kt` — 위젯 OkHttpClient 내 인라인 핀 문자열
- `network_security_config.xml` — `<pin-set>` 항목 및 `expiration` 날짜

---

## 잔여 고려 사항

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 위젯 핀 상수 중복 | `PopularMoviesRemoteViewsFactory`의 핀이 `NetworkModule` 상수와 별도 관리 | 낮음 (기능 영향 없음) |
| API 키 query param 로그 | debug HEADERS 레벨에서 URL에 `api_key=xxx` 노출 (debug 전용, READ_LOGS 권한 필요) | 낮음 |
| `TMDB_READ_ACCESS_TOKEN` ProGuard | BuildConfig 필드로 release APK에 포함 — 서버 측 rate limiting으로 대응 권장 | 정보 |
