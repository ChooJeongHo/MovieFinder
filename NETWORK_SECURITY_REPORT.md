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

---

## 2026-08-10 인증서 핀 불일치 조사 (091일차 부수 이슈)

### 증상

material3-design-validator 작업 중 실기기 테스트에서 캐스트/리뷰 아바타·포스터 이미지가 전부 회색
플레이스홀더로 표시되고, `DebugHealthCheck` 토스트가 `Image FAIL`을 보고. Logcat에
`DebugEventListener`가 다음을 대량(단일 화면 스크롤 1회에 수백 건) 기록:

```
javax.net.ssl.SSLHandshakeException: Pin verification failed
Caused by: java.security.cert.CertificateException: Pin verification failed
  at okhttp3.internal.connection.ConnectPlan.connectTls(ConnectPlan.kt:352)
```

처음엔 "간헐적"으로 보였음 — 같은 화면을 다시 열면 이미지가 로드되기도 했기 때문. 하지만 실제로는
**디버그 빌드임에도 실패가 발생**한 것 자체가 단서였음: `NetworkModule.kt`의 OkHttp
`CertificatePinner`는 `if (!BuildConfig.DEBUG)`로 디버그 빌드에서 비활성화되지만,
`network_security_config.xml`의 `<pin-set>`은 **OS/TLS 레벨**에서 별도로 평가되며,
`<debug-overrides overridePins="true">`는 "사용자 설치 CA(MITM 프록시 등)로 체인이 검증될 때만"
핀을 무시한다 — 실제 TMDB 서버(시스템 신뢰 앵커로 검증됨)와의 정상 연결에는 디버그 빌드에서도
`<pin-set>`이 그대로 적용된다. 즉 이 증상은 디버그/릴리스 무관하게 실사용자에게도 이미 발생 중이었음.

### 조사

`adb logcat`에서 실패 로그의 호스트명을 전수 확인(`🔴 연결 실패: <host>` 라인, 총 699건):

```bash
adb logcat -d | grep "연결 실패\|호출 실패" | sort -u
```

→ **100% `image.tmdb.org`, `api.themoviedb.org`는 0건.** "간헐적으로 보인" 이유는 화면 내 여러
이미지가 각각 비동기 로드되고 JSON 데이터(api.themoviedb.org, 정상)는 항상 성공해 텍스트는 뜨는데
포스터/아바타(image.tmdb.org)만 매번 실패했기 때문 — 실제로는 100% 재현되는 하드 실패였음.

두 도메인의 실제 서빙 인증서 체인을 직접 비교(`openssl s_client -showcerts`, api.themoviedb.org는
6회 재연결 + DNS로 얻은 IP 4개 전부에서 리프/중간 100% 동일 확인):

| 도메인 | 저장된 핀(067일차) | 실제 서빙 체인 (2026-08-10 측정) | 일치 |
|---|---|---|---|
| `api.themoviedb.org` | leaf `Qfyo...`, inter `G9LN...` (Amazon RSA 2048 M04) | leaf `Qfyo...`, inter `G9LN...` (Amazon RSA 2048 M04) | ✅ 완전 일치 |
| `image.tmdb.org` | leaf `D9+F...`, inter `LoMH...` | leaf `ev7y32IIBYuHsfRofMyLOE2lRz/O49x1HjkJ2Ea/9Y4=` (CN=image.tmdb.org), inter `nWN7PSep5XDQdge5zK24CnCRXHr3KvzhKEGxsdqCX9E=` (**Let's Encrypt YR2**), root ISRG Root X1 | ❌ **완전 불일치** |

**결론**: `image.tmdb.org`가 067일차 핀 설정 이후 발급 CA를 (추정컨대 Amazon 계열에서) **Let's
Encrypt로 이전**했음. 리프/중간 인증서 값이 통째로 바뀌어 저장된 핀과 아예 다른 체인 — 타이밍/로테이션
경합이 아니라 100% 재현되는 확정 불일치였음. `api.themoviedb.org`(Amazon 발급)는 문제 없음.

### 수정

- `NetworkModule.kt`: `PIN_IMAGE_LEAF`/`PIN_IMAGE_INTER`를 실측값으로 교체.
- `network_security_config.xml`: `image.tmdb.org`를 `themoviedb.org`/`tmdb.org`와 분리된
  `<domain-config>`로 이동, 별도 `<pin-set expiration="2026-11-10">` 부여(Amazon 블록은 기존
  `2027-06-30` 유지). Let's Encrypt 리프는 90일 이하 주기로 갱신되므로 Amazon보다 짧은 만료일을
  명시해, 다음 로테이션 때 갱신을 놓치더라도 Android가 만료된 pin-set을 조용히 비활성화(fail-open)
  하도록 함 — 무기한 고정된 오래된 핀으로 인한 재발을 방지.
- `PopularMoviesRemoteViewsFactory.kt`(위젯 OkHttpClient)는 `api.themoviedb.org`만 피닝하고
  있어 이번 이슈의 영향을 받지 않음 — 수정 불필요, 값도 이미 최신.

### 재발 방지 참고

`image.tmdb.org`는 Let's Encrypt 발급 특성상 `api.themoviedb.org`(Amazon)보다 핀이 훨씬 자주
깨진다. `cert-pin-check.yml` 주간 CI 알림에서 두 도메인 중 하나만 언급되면 대개
`image.tmdb.org` 쪽일 가능성이 높다.

---

---

## 2026-08-14 간헐적 핀 실패 재점검 (기기 재현 없이 코드 리뷰만)

091일차 조사(위 절)는 이미 원인을 특정하고 수정까지 마쳤지만, 그 결론("100% 재현되는 확정 불일치였다")
자체를 이번 세션에서 재검증하지는 않았다 — 그대로 인용한 것. 재현 없이 코드만으로 추가 확인 가능한
것과 불가능한 것을 구분해 아래만 처리:

- **백업(롤오버) 핀은 여전히 없음.** leaf+intermediate 2개는 같은 시점에 관측한 단일 체인의 상하위
  단계일 뿐, 다음 CA 로테이션에 대비한 독립 예비 핀이 아니다. 정확한 백업 값(예: 루트 CA 핀)을 넣으려면
  실서버 TLS 체인을 라이브로 조회해야 하는데, 이 개발 환경은 외부 네트워크가 차단되어 있어
  `openssl s_client`조차 접속 실패함(직접 확인). 검증 안 된 핀 값을 넣는 건 다음 로테이션 때 "백업이
  있으니 안전하다"는 착각만 주고 조용히 무력화될 위험이 있어 값 자체는 추가하지 않았다 — 필요 시
  네트워크 되는 환경에서 위 "인증서 핀 갱신 절차" 명령을 `-showcerts`로 확장해 루트까지 추출해야 함.
- **SSL/TLS 실패에 대한 재시도가 전혀 없었음** (기존 `withExponentialBackoff()`는 `MovieRemoteMediator`
  페이징 호출에만 적용, 이미지/일반 API 클라이언트는 미적용). `NetworkModule.kt`에
  `addSslRetryInterceptor()`를 추가해 `provideOkHttpClient`/`provideImageOkHttpClient` 양쪽에 적용 —
  `SSLException` 발생 시 최대 2회, 300ms/600ms 지연 후 재시도. 핀 값 자체가 서버 체인과 완전히
  불일치하는 경우(091일차 케이스)는 재시도해도 동일하게 실패하므로 근본 해결책이 아니라 CDN 엣지 간
  인증서 전파 지연 등 일시적 실패에 대한 안전장치일 뿐.

**결론**: "간헐적으로 보였다"는 현상의 정확한 메커니즘(CDN 엣지별 신/구 인증서 혼재 서빙인지, 단순
비동기 이미지 로딩 때문에 그렇게 보인 것인지)은 실제 트래픽 캡처나 기기 재현 없이 단정할 수 없다.
위 두 항목은 재현 여부와 무관하게 코드 검토만으로 확인·개선 가능했던 것이고, 원인 자체를 밝히는 조치는
아니다.

---

## 잔여 고려 사항

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 위젯 핀 상수 중복 | `PopularMoviesRemoteViewsFactory`의 핀이 `NetworkModule` 상수와 별도 관리 | 낮음 (기능 영향 없음) |
| API 키 query param 로그 | debug HEADERS 레벨에서 URL에 `api_key=xxx` 노출 (debug 전용, READ_LOGS 권한 필요) | 낮음 |
| `TMDB_READ_ACCESS_TOKEN` ProGuard | BuildConfig 필드로 release APK에 포함 — 서버 측 rate limiting으로 대응 권장 | 정보 |
