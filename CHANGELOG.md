# Changelog

## [Unreleased] — 2026-10-16

### 새 기능
- KOFIC 박스오피스 일별/주간 TOP 10 연동 (TMDB 매칭, 홈 화면 표시)
- KMRB(영상물등급위원회) 국내 관람등급 연동 — 영화 상세 표시, 검색 필터, 홈 박스오피스 배지
- 박스오피스 홈 화면 위젯 추가 (Jetpack Glance 기반, 기존 인기 영화 위젯과 별도)
- App Shortcuts 추가 — 검색/즐겨찾기 정적 단축키 + 최근 시청 영화 동적 단축키
- Predictive Back Gesture 지원 — Fragment 전환 애니메이션을 Animator 리소스로 전환해 스크러빙 미리보기 지원
- 검색 결과 화면을 Jetpack Compose로 일부 마이그레이션
- 리뷰 "도움이 됨" 표시 및 상단 고정 기능
- 트레일러 재생 위치 기억 및 재개 안내
- `tools/codebase-rag` 개발 도구 추가 (사내 코드베이스 RAG 질의 도구, 앱 런타임과 무관)
- GitHub Actions 자동 릴리즈 파이프라인 추가
- 박스오피스 위젯에 일별/주간 전환 배지 추가 — 탭하면 기간이 바뀌고 기간별 스냅샷을 따로 보관해 즉시 새로고침 (2026-09-18)
- Material You 동적 색상 적용 — XML 화면과 Compose 화면(검색 결과·온보딩) 모두, Android 12(API 31) 이상에서 적용되고 그 미만은 기존 팔레트. 삼성 One UI(실기기 SM-S926N)는 배경화면을 바꿔도 팔레트를 자동으로 만들지 않아, 설정에서 컬러 팔레트를 직접 골라야 앱 색이 바뀜 (2026-09-23)
- TMDB 영화 정보가 앱 언어(Per-App Language)를 따름 — English로 바꾸면 상세·배우·트렌딩·discover·개봉예정·홈 목록의 제목/줄거리가 영어로 표시. 검색 결과는 데이터를 한국어 기준으로 유지(KMRB 등급 필터가 한국어 제목으로 매칭)하고 화면에 보이는 카드의 제목만 앱 언어로 표시. 언어를 바꾸면 홈 캐시를 즉시 무효화 (2026-09-29)

### 버그 수정
- 딥링크(movie/stats) 진입 후 뒤로가기 시 온보딩 화면이 재등장하던 문제 3건 수정
- 딥링크로 앱을 시작할 때 온보딩 액션 호출로 인해 발생하던 크래시 수정
- WatchGoal 개봉일 알림의 stats 딥링크가 실기기에서 열리지 않던 문제 수정
- 계측 테스트(`connectedDebugAndroidTest`)가 아예 실행되지 않던 문제 수정
- KOFIC 박스오피스 UI 접근성 이슈 3건 수정
- TMDB 계정 연동이 완료된 적이 없었을 정도의 결함 3건 수정 — `redirect_to`를 보내는 위치, v4 콜백에 쿼리 파라미터가 없는 점, `MainActivity`와 `SettingsFragment`가 서로 다른 `SettingsViewModel` 인스턴스를 써서 `pendingRequestToken`이 공유되지 않던 점 (2026-09-16)
- 검색: 등급 필터로 전부 걸러지면 이어 로드가 멈추던 문제와, 로딩 중에 "검색 결과 없음"이 잘못 표시되던 문제 수정 — 등급이 바뀌면 재검색하고 마지막 페이지까지 확인한 뒤에만 "결과 없음"을 표시 (2026-09-30)
- 검색: 엔터·칩·장르 확인 같은 즉시 검색과 타이핑 debounce 검색이 같은 검색을 두 번 내던 문제 수정(실기기 2건 → 1건). 같은 조건으로 다시 엔터를 누르면 재검색하지 않음 (2026-10-02)
- 검색: 검색어를 지운 뒤 최근 검색어 목록이 비거나 일부만 보이던 문제 수정 (2026-10-08)
- 검색: 가로 모드에서 콘텐츠 영역이 173px로 줄어 최근 검색어와 결과가 보이지 않던 문제 수정 — 가로 전용 2단 레이아웃(왼쪽 입력·필터, 오른쪽 콘텐츠)으로 1139px (2026-10-12)
- 홈: 시청기록이 있을 때 메인 목록 높이가 세로·가로 모두 0px이던 문제 수정 — 박스오피스·시청기록 섹션을 위로 밀어 접으면 목록이 보임. 첫 화면에서는 아직 목록이 보이지 않아 별도 결정이 필요 (2026-10-13)
- 상세: 메모 입력란이 키보드에 가려지고 스크롤로도 닿지 않던 문제 수정 (2026-10-14)
- 상세: 메모를 저장해도 메모 목록이 그려지지 않던 문제 수정 — `rv_memos`의 `setHasFixedSize(true)` + `wrap_content` 조합이 원인 (2026-10-15)

### 성능/보안
- 박스오피스-TMDB 매칭의 N+1 네트워크 호출 문제 해결 (로컬 캐시 우선 매칭)
- `image.tmdb.org` 인증서 핀 갱신 및 SSL 재시도 안전장치 추가
- OkHttpClient에 callTimeout 25초 추가
- OS 레벨 인증서 피닝 강화, 디버그 로그의 Authorization 헤더 redact 처리
- 디버그 로그에 API 키(KMRB `serviceKey`, KOFIC `key`, TMDB `api_key`)와 `session_id`가 쿼리스트링 그대로 남던 문제 수정 — 요청/응답/호출 실패 줄에서 값을 마스킹. 디버그 빌드 한정이며 release는 로깅이 없음 (2026-10-07)

### 개발/CI
- `pre-push` 훅에 `:app:lintDebug` 추가 — Lint가 CI에서만 돌아 push 후에야 실패가 드러났음. 이 클론에서 훅이 한 번도 실행되지 않던 `core.hooksPath` 설정 오류도 확인·복구 (2026-09-29)
- `cert-pin-check`: leaf 핀만 비교해 매주 오탐을 내던 판정을 "서버 체인의 인증서 중 하나라도 코드의 핀과 일치하면 정상"으로 변경하고, 같은 제목의 열린 이슈가 있으면 새로 만들지 않도록 수정 (2026-10-07)
- `Claude Code Review` 워크플로우에서 Dependabot PR 제외 (`claude-pr-review.yml`이 이미 리뷰) (2026-09-29)
- Kotlin 컴파일러/Lint 경고 정리 — `kotlinx.datetime.Instant` → `kotlin.time.Instant` 등, 컴파일 경고 15 → 11개 (남은 11개는 `SecureTokenStore`의 `EncryptedSharedPreferences`/`MasterKey` deprecated로 의도적 유지) (2026-09-20)
- 테스트 추가: 언어 선택 다이얼로그 Espresso 3개, 동적 색상 Espresso 2개(`MainActivityDynamicColorTest`), `MovieFinderTheme` 색상 분기 유닛 테스트, WorkManager `ExistingWorkPolicy` 회귀 테스트. 유닛 테스트 856 → 987개 (2026-09-16 → 2026-10-16)

## [1.0.0] — 2026-06-29

### 새 기능
- TMDB API 기반 영화 검색 및 상세 정보 조회
- 즐겨찾기 / 워치리스트 관리
- 시청 기록 및 통계 (월별 시청, 장르 분포, 별점 분포)
- 개봉일 알림 (WorkManager 기반)
- 영화 메모 및 개인 별점 기록
- 데이터 내보내기 / 가져오기 (JSON 백업)
- 홈 위젯 (인기 영화 / 시청 목표)
- 영어 / 한국어 다국어 지원
- 다크 모드 지원
- Baseline Profile 적용 (콜드 스타트 최적화)
