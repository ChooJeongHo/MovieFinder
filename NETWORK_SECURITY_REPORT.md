# Network Security Report — MovieFinder

> Generated: 2026-06-30 (최종 갱신: 2026-10-16 — 아래 "2026-09-16", "2026-10-07" 절과 "잔여 고려 사항" 참고)

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
| 디버그 로그 쿼리 비밀값 마스킹 | `SecretQueryParams` + `addDebugLogging()`의 `redactQueryParams` + `DebugEventListener` (2026-10-07, 아래 절) | ✅ |
| 인증서 핀 만료 모니터링 | `cert-pin-check.yml` CI 워크플로우 (매주 월요일 cron) — 2026-10-07부터 "서버 체인의 인증서 중 하나라도 코드 핀과 일치하면 정상" 기준(OkHttp `CertificatePinner`와 같음), 같은 제목의 열린 이슈가 있으면 새로 만들지 않음 | ✅ |

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
  ├─ Debug: HEADERS 레벨, Authorization 헤더 + 쿼리 비밀값(api_key/key/serviceKey/session_id) 마스킹
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

# image.tmdb.org 서버 체인의 모든 인증서 핀 추출 (cert-pin-check.yml과 같은 방식; 리프만 보면 안 된다)
echo | openssl s_client -connect image.tmdb.org:443 -servername image.tmdb.org -showcerts 2>/dev/null |
  awk -v cmd="openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64" '
    /BEGIN CERTIFICATE/ { buf = "" }
    { buf = buf $0 "\n" }
    /END CERTIFICATE/ { printf "%s", buf | cmd; close(cmd) }'
```

**image.tmdb.org 핀 선택 원칙 (131일차)**: 리프는 핀하지 않는다(발급마다 키가 바뀐다). 루트 공개키(ISRG Root X1·Root YR)와 활성 중간 CA(YR1·YR2), 백업 중간 CA(YR3)를 핀한다. 서버가 보낸 체인에서 계산한 값만 믿지 말고 Let's Encrypt 공식 PEM(letsencrypt.org/certificates 페이지의 `pem` 링크)의 SPKI 값과 전체 일치하는지 대조한다. 같은 페이지의 활성/백업 중간 CA 목록이 바뀌면(YR3 승격, 새 세대) 새 중간 CA 핀을 **추가**하고 기존 핀은 유지한다. 핀 5개는 OS 층(`network_security_config.xml`)과 앱 층(`NetworkModule.kt`)에서 같아야 하며 `cert-pin-check.yml`이 두 층의 일치와 서버 체인 일치를 함께 점검한다.

갱신 시 수정 파일:
- `NetworkModule.kt` — `PIN_API_LEAF`, `PIN_API_INTER`, `PIN_IMAGE_ROOT_X1`, `PIN_IMAGE_ROOT_YR`, `PIN_IMAGE_INTER_YR1`, `PIN_IMAGE_INTER_YR2`, `PIN_IMAGE_INTER_YR3` 상수
- `PopularMoviesRemoteViewsFactory.kt` — 위젯 OkHttpClient 내 인라인 핀 문자열
- `network_security_config.xml` — `<pin-set>` 항목 및 `expiration` 날짜(image 블록 `2027-06-30`, api 블록 `2027-06-30`)

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

## 2026-09-16 정기 재검증 (실측)

이 개발 환경에서 처음으로 외부 네트워크 접속이 가능해져(091/094일차 당시엔 차단되어 있었음),
`openssl s_client -showcerts`로 두 도메인의 실제 서빙 체인을 직접 재측정했다.

| 도메인 | 저장된 핀(코드 기준) | 실측 체인 (2026-09-16) | 일치 |
|---|---|---|---|
| `api.themoviedb.org` leaf | `Qfyo...` | `Qfyo...` (CN=`*.themoviedb.org`) | ✅ 일치 |
| `api.themoviedb.org` inter | `G9LN...` (Amazon RSA 2048 M04) | `G9LN...` (Amazon RSA 2048 M04) | ✅ 일치 |
| `image.tmdb.org` leaf | `ev7y...` (091일차 갱신값) | `x9aA...` (CN=`image.tmdb.org`) | ❌ **불일치(로테이션됨)** |
| `image.tmdb.org` inter | `nWN7...` (Let's Encrypt YR2) | `nWN7...` (Let's Encrypt YR2) | ✅ 일치 |

**결론**: 091일차 예측대로 `image.tmdb.org`의 leaf 인증서가 다시 로테이션됐다(Let's Encrypt 90일
주기). 하지만 이번엔 **장애로 이어지지 않았다** — `CertificatePinner.add()`/`<pin-set>` 둘 다
"핀셋 중 하나라도 체인과 일치하면 통과"이고, intermediate(YR2)는 그대로라 leaf만 바뀌어도 정상
연결된다. **leaf+intermediate 동시 피닝 전략(091일차 수정)이 실전에서 처음으로 검증된 사례.**
leaf만 피닝했다면 이번에도 091일차와 동일한 전면 이미지 로드 실패가 재발했을 것이다.

다만 저장된 `PIN_IMAGE_LEAF` 값(`ev7y...`) 자체는 이제 stale하다. 당장 깨지진 않지만, 다음
로테이션에서 Let's Encrypt가 intermediate CA(YR2)까지 교체하면(수년 주기로 발생 가능) leaf/inter
둘 다 불일치해 091일차와 같은 장애가 재발한다. `image.tmdb.org`의 `<pin-set expiration>`도
2026-11-10으로 2달 이내 만료 예정이므로, 다음 인증서 핀 갱신 절차(위 "인증서 핀 갱신 절차" 참고)
때 leaf 값도 함께 최신화하는 것을 권장 — 이번 재검증 자체가 코드를 수정하지는 않았음(문서 갱신
세션 범위, 실측만 진행).

---

## 2026-10-07 디버그 로그 쿼리 비밀값 마스킹

위 "이번 세션 개선 사항 2"는 `Authorization` 헤더만 가렸고, 쿼리스트링으로 나가는 비밀값은 디버그 로그에 그대로 남아 있었다(아래 "잔여 고려 사항"에 있던 항목). 2026-10-07(PR #158)에 수정했고 실기기(SM-S926N)에서 검증했다.

### 증상
- `HttpLoggingInterceptor`가 쿼리스트링의 키(KMRB `serviceKey`, KOFIC `key`, TMDB `api_key`)와 `session_id`를 요청/응답 줄에 그대로 출력했다.
- **인터셉터 순서를 바꿔도 막히지 않는다**: TMDB v3는 로깅이 키 주입보다 먼저 등록돼 있어도, 응답 줄이 키가 주입된 최종 URL(`response.request.url`)을 찍어 노출됐다.
- `DebugEventListener.callFailed`도 `@Query`로 붙는 `session_id`를 원본 URL 그대로 출력했다(주입 키는 원본 URL에 없다).

### 수정
- `SecretQueryParams`(`core/util`): 비밀 쿼리 파라미터 이름 상수. 키 주입 인터셉터·`@Query`와 마스킹 목록이 같은 상수를 공유한다. **새 쿼리 비밀값을 추가하면 `ALL`에도 넣어야 마스킹이 조용히 풀리지 않는다.**
- `addDebugLogging()`(debug 소스셋): `redactQueryParams(*SecretQueryParams.ALL)`로 요청/응답/실패 줄의 값을 가린다. release는 로깅이 no-op이라 그대로.
- `DebugEventListener`: 호출 실패 로그 URL에서 같은 목록을 같은 마스크(`██`)로 가린다(`redactSecrets()`).
- 테스트: `NetworkLogRedactionTest` — 실제 `build*OkHttpClient` + 로컬 HttpServer로 조립 순서까지 검증하며 `testDebugUnitTest` 전용이다(release 변형은 `assumeTrue`로 건너뜀).

---

## 2026-10-22 image.tmdb.org 핀 재구성 (131일차)

### 증상
- `image.tmdb.org`의 리프 인증서가 2026-10-14 08:02 GMT에 재발급됐다(리프 `7p4VXQnn…`, 만료 2027-01-12). 중간 CA는 YR2(`nWN7PSep…`)에서 **YR1**(`LoMHBott…`)로 바뀌었다. 미국 GitHub 러너의 `cert-pin-check`는 10/12에는 YR2 체인(통과), 10/19에는 YR1 체인(불일치 → 이슈 #173)을 봤다.
- 2026-10-22 리졸버 8종(시스템·1.1.1.1·8.8.8.8·9.9.9.9·OpenDNS·KT·LG U+·SK브로드밴드)이 알려 준 엣지 IP 10곳이 모두 같은 체인을 보냈고 앱 핀(리프 `ev7y32II…`, YR2)과는 **0/10 불일치**였다. 도메인은 `tmdb-image-prod.b-cdn.net`으로 CNAME된다(Bunny CDN으로 추정).
- 변경 전(main@45a40df) 기기 실측: 에뮬레이터(API 37)와 폰(SM-S926N, API 36) 모두 디버그·릴리스에서 이미지 호스트 TLS 핸드셰이크 성공이 0이었고, 디버그 로그의 핀 검증 실패는 폰 약 300줄/회, 에뮬레이터 약 120~140줄/회였다.
- 정정: 10/16 기록의 "중간 인증서는 경로마다 다름 / 폰은 YR2라 정상"은 이번 실측으로 지지되지 않는다. 8종 리졸버·엣지 10곳, 폰 직접 측정, 10/12→10/19 CI 순서가 모두 10/14 재발급(시간차)으로 설명된다. 다만 10/16경 일부 엣지에 옛 인증서가 남았을 가능성은 배제하지 못한다(판정 불가).

### 원인
- Let's Encrypt 공식 "Chains of Trust"(2026-07-08 갱신): RSA 중간 CA는 YR1·YR2가 교대로 발급하고 **YR3은 예고 없이 발급을 시작할 수 있는 백업**이다. 기본 체인은 `리프 ← YR1 ← Root YR ← ISRG Root X1`이다. 그래서 리프 핀은 발급마다, 중간 CA 핀 1개는 중간 CA가 바뀔 때마다 깨진다(8/10, 9/14, 10/14 세 번째).
- 핀은 두 층에서 따로 검사된다. OS 층(`network_security_config.xml`의 `pin-set`)과 앱 층(`NetworkModule.kt`의 `CertificatePinner`, release에서만 켜짐). **디버그 빌드도 OS 층 핀은 적용된다**: 공식 문서상 `debug-overrides`의 `overridePins`는 user CA로 서명된 체인에만 핀을 면제하고, 양성 대조 a(아래)에서 실측으로 확인했다. 앱 층에는 만료 개념이 없다.

### 수정
- **핀 5개**(두 층에서 같은 집합, 값은 공식 PEM의 SPKI SHA-256과 전체 일치):

| 핀 | 앞 8자 | 처리 |
|---|---|---|
| ISRG Root X1 | `C5+lpZ7t…` | 추가 |
| ISRG Root YR | `fk6IOKit…` | 추가 |
| 중간 YR1 | `LoMHBott…` | 추가 |
| 중간 YR2 | `nWN7PSep…` | 기존 값 유지 |
| 중간 YR3(백업) | `UaqofZhL…` | 추가 |
| 리프 | `ev7y32II…` | 제거(발급마다 키가 바뀜) |

- `network_security_config.xml`: image 블록 `expiration` 2026-11-10 → 2027-06-30(api 블록과 같은 날).
- `cert-pin-check.yml`: image 단계가 `PIN_IMAGE_*` 상수 전체를 읽고, OS 층 XML과 앱 층 코드의 핀 집합이 다르면 불일치로 보고한다. 로컬 시험: 정상 ok / XML 핀 1개 제거 → 불일치 / 체인 불일치 → 불일치 / 옛 스크립트는 지금 서버에서 불일치.
- 변경하지 않은 것: `api.themoviedb.org` 핀·위젯 핀(아래 후속 후보), CLAUDE.md.

### 검증 (2026-10-22, 같은 시나리오·같은 기기)
시나리오: 앱 내 이미지 캐시 삭제 → 콜드 시작 → 홈 5화면 스크롤 → `iv_poster` 슬롯 판정(중앙 50% 영역의 95% 이상이 placeholder 색이면 실패). 이 판정은 오버레이 때문에 **가짜 OK를 4~14%** 냈다(기준선 디버그 로그의 진짜 성공은 0). 명확한 성공(비율 <0.5)은 기준선 0, 수정 후 전부.

| 그룹 | 기준선 성공률 | 수정 후 성공률 |
|---|---|---|
| 에뮬레이터 디버그 | 4.7% (7/148, 3회) | 100% (144/144, 3회) |
| 에뮬레이터 릴리스 | 10.7% (16/150, 3회) | 100% (150/150, 3회) |
| 폰 디버그 | 10.8% (11/102, 2회) | 100% (99/99, 2회) |
| 폰 릴리스 | 11.8% (12/102, 2회) | 100% (102/102, 2회) |

- 디버그 로그: 핸드셰이크 성공 0 → 회당 5, 핀 검증 실패 줄 약 120~300 → 0, 디스크 캐시 파일 1 → 53~61. 폰 디버그의 이미지 호출 실패 2건은 `Canceled`(스크롤 취소)이며 핀과 무관하다.
- 서버 체인 재실측: 수정 직전(12:21)·직후(12:22) 모두 엣지 10곳에서 새 핀 중 Root YR·Root X1·YR1이 일치했다.
- 양성 대조(에뮬레이터, 임시 폴더 복사본): (a) 틀린 핀+미래 만료 디버그 → 성공률 4.2%, 핀 검증 실패 줄 134·116 / (b) 같은 틀린 핀+과거 만료 → 100%, 핀 실패 0(**만료 후 핀 검사가 꺼짐을 실측**, 에뮬레이터 API 37 디버그) / (c) NSC는 수정본, OkHttp 핀만 틀린 릴리스 → 10.4%(가짜 OK 포함), 앱 층만으로도 실패.
- 빌드·테스트: 유닛 987개 0 실패, detekt 통과, lint 89건(Warning 88 + Hint 1). 기준선 lint는 83으로 알려졌으나 그것은 `lintAnalyzeDebug` 등이 `UP-TO-DATE`인 캐시 결과였고, 같은 조건 강제 재실행(`--rerun`)은 main 89·수정 후 89로 항목(규칙 id·파일·줄) 차이가 0이다. 83→89의 6건은 `NewerVersionAvailable` 6건과 개수가 같다는 추정이며 확인하지 못했다.
- 다른 도메인: 핀 검증 실패 줄 0, api·KOFIC·KMRB 호출 실패 0. 기준선 2회차 이후와 비교하면 api·KOFIC 성공 수는 같고 KMRB는 양쪽 0이라 판정 불가(캐시, 확인 못 함).

### 한계
- 기기는 에뮬레이터 1대(API 37)와 폰 1대(API 36, 한국 경로)뿐이다. 다른 지역·망·OS 버전은 확인하지 못했다.
- 릴리스 변형은 debug 서명 폴백 빌드이고 화면 판정으로만 쟀다(로그 없음). 서버 체인 일치는 SPKI 계산으로 확인했지만 실제 사용자 기기의 신뢰 저장소가 만드는 체인(Root YR 직접 신뢰 여부)은 확인하지 못했다.
- CDN이 Let's Encrypt를 떠나면 이 핀 5개도 전부 불일치한다. 앱 층에는 만료가 없어 그 경우 앱 업데이트가 필요하다.

### 다음 로테이션 일정
| 일정 | 내용 |
|---|---|
| 2026-10-26 | 병합 후 첫 주간 `cert-pin-check`(통과 기대) |
| 약 2026-12-13 | 현재 리프(만료 2027-01-12) 갱신 예상 — LE 권고(만료 30일 전) 기준 추정, CDN 구현은 불명. 갱신 후 첫 월요일 점검 확인 |
| 2027-05-01 | `expiration` 갱신 착수 권장(제안) |
| 2027-06-30 | NSC `expiration`(이후 OS 층은 핀 검사 꺼짐, 앱 층은 유지) |
| 2028-09-02 | LE 중간 CA YR1·YR2·YR3 유효 종료 |
| 2030-06-04 | LE 문서상 ISRG Root X1 신뢰 종료 예상(루트 프로그램 정책, 인증서 notAfter는 2035-06-04) |

---

## 잔여 고려 사항

| 항목 | 내용 | 우선순위 |
|---|---|---|
| 위젯 핀 상수 중복 | `PopularMoviesRemoteViewsFactory`의 핀이 `NetworkModule` 상수와 별도 관리 | 낮음 (기능 영향 없음) |
| API 키 query param 로그 | **해결됨(2026-10-07)** — 위 절. 이전 상태: debug HEADERS 레벨에서 URL에 `api_key=xxx` 노출(debug 전용, READ_LOGS 권한 필요) | — |
| `TMDB_READ_ACCESS_TOKEN` ProGuard | BuildConfig 필드로 release APK에 포함 — 서버 측 rate limiting으로 대응 권장 | 정보 |
| `image.tmdb.org` leaf 핀 stale · pin-set 만료 임박 | **해결됨(2026-10-22, 131일차)** — 리프 핀 제거, 루트 2개+중간 CA 3개로 재구성, `expiration` 2027-06-30. 위 "2026-10-22 image.tmdb.org 핀 재구성" 절 | — |
| 앱 층 `CertificatePinner`에 만료 없음 | NSC `pin-set`은 만료 후 핀 검사가 꺼지지만(실측, 위 절 양성 대조 b) 앱 층 핀은 계속 남아, CDN이 인증서 체계를 바꾸면 앱 업데이트 전까지 이미지가 실패한다 | 중간(후속 후보) |
| `api.themoviedb.org` 핀 | 10/19 재발급으로 리프 핀은 이미 불일치이고 Amazon 중간 CA(M04) 핀 1개로 버틴다. Amazon Trust Services는 핀닝을 권장하지 않으며 한다면 루트 공개키에 하라고 안내한다. 이번 범위 밖이라 0줄 변경 | 중간(후속 후보) |
| `cert-pin-check` image 점검 | 2026-10-22부터 서버 체인 일치와 함께 OS 층 XML·앱 층 코드의 핀 집합 일치를 점검한다. api·위젯은 기존 방식 | — |
