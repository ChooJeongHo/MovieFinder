# Changelog

## [Unreleased] — 2026-09-16

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

### 버그 수정
- 딥링크(movie/stats) 진입 후 뒤로가기 시 온보딩 화면이 재등장하던 문제 3건 수정
- 딥링크로 앱을 시작할 때 온보딩 액션 호출로 인해 발생하던 크래시 수정
- WatchGoal 개봉일 알림의 stats 딥링크가 실기기에서 열리지 않던 문제 수정
- 계측 테스트(`connectedDebugAndroidTest`)가 아예 실행되지 않던 문제 수정
- KOFIC 박스오피스 UI 접근성 이슈 3건 수정

### 성능/보안
- 박스오피스-TMDB 매칭의 N+1 네트워크 호출 문제 해결 (로컬 캐시 우선 매칭)
- `image.tmdb.org` 인증서 핀 갱신 및 SSL 재시도 안전장치 추가
- OkHttpClient에 callTimeout 25초 추가
- OS 레벨 인증서 피닝 강화, 디버그 로그의 Authorization 헤더 redact 처리

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
