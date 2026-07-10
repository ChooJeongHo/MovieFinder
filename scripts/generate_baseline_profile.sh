#!/bin/bash
# Baseline Profile 자동 생성 스크립트
# 사용법: ./scripts/generate_baseline_profile.sh [OPTIONS]
#
# OPTIONS:
#   --emulator [AVD_NAME]   AVD를 새로 시작 (기본: Pixel_10)
#   --keep-emulator         생성 후 에뮬레이터 유지 (기본: 자동 종료)
#   --skip-clean            빌드 캐시 유지 (기본: clean 실행)
#   --help                  도움말 출력

set -euo pipefail

# ─── 색상 ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# ─── 상수 ────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PROFILE_PATH="$PROJECT_ROOT/app/src/main/baseline-prof.txt"
DEFAULT_AVD="Pixel_10"
EMULATOR_BIN="$HOME/Library/Android/sdk/emulator/emulator"
ADB_BIN="$HOME/Library/Android/sdk/platform-tools/adb"
EMULATOR_BOOT_TIMEOUT=180  # 초

# ─── 옵션 ────────────────────────────────────────────────────────────────────
USE_EMULATOR=false
AVD_NAME="$DEFAULT_AVD"
KEEP_EMULATOR=false
SKIP_CLEAN=false
STARTED_EMULATOR=false

# ─── 헬퍼 함수 ───────────────────────────────────────────────────────────────
info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERR]${NC}  $*" >&2; }
step()    { echo -e "\n${BOLD}▶ $*${NC}"; }
hr()      { echo -e "${BLUE}────────────────────────────────────────────────${NC}"; }

usage() {
    echo "사용법: $0 [OPTIONS]"
    echo ""
    echo "OPTIONS:"
    echo "  --emulator [AVD]   AVD를 새로 시작 (기본 AVD: $DEFAULT_AVD)"
    echo "  --keep-emulator    생성 후 에뮬레이터 유지"
    echo "  --skip-clean       Gradle clean 건너뜀"
    echo "  --help             이 도움말 출력"
    echo ""
    echo "예시:"
    echo "  $0                          # 연결된 기기 사용"
    echo "  $0 --emulator               # Pixel_10 에뮬레이터 자동 시작"
    echo "  $0 --emulator Pixel_10      # 특정 AVD 지정"
    echo "  $0 --emulator --keep-emulator  # 에뮬레이터 종료 안 함"
}

cleanup() {
    local exit_code=$?
    if $STARTED_EMULATOR && ! $KEEP_EMULATOR; then
        step "에뮬레이터 종료 중..."
        "$ADB_BIN" -s emulator-5554 emu kill 2>/dev/null || true
        info "에뮬레이터 종료됨"
    fi
    if [ $exit_code -ne 0 ]; then
        error "스크립트가 오류로 종료되었습니다 (exit code: $exit_code)"
    fi
    exit $exit_code
}
trap cleanup EXIT

# ─── 인수 파싱 ────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --emulator)
            USE_EMULATOR=true
            if [[ $# -gt 1 && "$2" != --* ]]; then
                AVD_NAME="$2"
                shift
            fi
            ;;
        --keep-emulator) KEEP_EMULATOR=true ;;
        --skip-clean)    SKIP_CLEAN=true ;;
        --help|-h)       usage; exit 0 ;;
        *) error "알 수 없는 옵션: $1"; usage; exit 1 ;;
    esac
    shift
done

# ─── 시작 배너 ────────────────────────────────────────────────────────────────
hr
echo -e "${BOLD}  MovieFinder Baseline Profile 생성기${NC}"
echo -e "  프로젝트: $PROJECT_ROOT"
hr

# ─── 1. 사전 요구사항 확인 ────────────────────────────────────────────────────
step "사전 요구사항 확인"

# local.properties에 TMDB_API_KEY 존재 여부
LOCAL_PROPS="$PROJECT_ROOT/local.properties"
if [ ! -f "$LOCAL_PROPS" ] || ! grep -q "TMDB_API_KEY" "$LOCAL_PROPS"; then
    error "local.properties에 TMDB_API_KEY가 없습니다."
    error "echo 'TMDB_API_KEY=your_key_here' >> $LOCAL_PROPS"
    exit 1
fi
success "local.properties 확인됨"

# ADB 확인
if [ ! -f "$ADB_BIN" ]; then
    ADB_BIN=$(command -v adb 2>/dev/null || echo "")
    if [ -z "$ADB_BIN" ]; then
        error "adb를 찾을 수 없습니다. Android SDK PATH를 확인하세요."
        exit 1
    fi
fi
success "adb 확인됨: $ADB_BIN"

# gradlew 확인
GRADLEW="$PROJECT_ROOT/gradlew"
if [ ! -f "$GRADLEW" ]; then
    error "gradlew를 찾을 수 없습니다: $GRADLEW"
    exit 1
fi
success "gradlew 확인됨"

# ─── 2. 기기/에뮬레이터 준비 ─────────────────────────────────────────────────
step "기기 연결 확인"

get_connected_device() {
    "$ADB_BIN" devices | grep -v "^List" | grep "device$" | awk '{print $1}' | head -1
}

DEVICE_SERIAL=""

if $USE_EMULATOR; then
    # 에뮬레이터 바이너리 확인
    if [ ! -f "$EMULATOR_BIN" ]; then
        error "에뮬레이터를 찾을 수 없습니다: $EMULATOR_BIN"
        exit 1
    fi

    # AVD 존재 여부 확인
    if ! "$EMULATOR_BIN" -list-avds 2>/dev/null | grep -q "^${AVD_NAME}$"; then
        error "AVD '$AVD_NAME'를 찾을 수 없습니다."
        info "사용 가능한 AVD 목록:"
        "$EMULATOR_BIN" -list-avds 2>/dev/null | sed 's/^/  - /'
        exit 1
    fi

    # 이미 실행 중인 에뮬레이터 확인
    EXISTING_EMULATOR=$(get_connected_device)
    if [[ "$EXISTING_EMULATOR" == emulator-* ]]; then
        info "이미 실행 중인 에뮬레이터 사용: $EXISTING_EMULATOR"
        DEVICE_SERIAL="$EXISTING_EMULATOR"
    else
        info "에뮬레이터 '$AVD_NAME' 시작 중..."
        "$EMULATOR_BIN" -avd "$AVD_NAME" -no-snapshot-save -no-audio -no-boot-anim &
        STARTED_EMULATOR=true

        # 부팅 완료 대기
        info "에뮬레이터 부팅 대기 중 (최대 ${EMULATOR_BOOT_TIMEOUT}초)..."
        ELAPSED=0
        while true; do
            BOOT_STATUS=$("$ADB_BIN" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || echo "0")
            if [ "$BOOT_STATUS" = "1" ]; then
                break
            fi
            if [ $ELAPSED -ge $EMULATOR_BOOT_TIMEOUT ]; then
                error "에뮬레이터 부팅 타임아웃 (${EMULATOR_BOOT_TIMEOUT}초 초과)"
                exit 1
            fi
            sleep 3
            ELAPSED=$((ELAPSED + 3))
            echo -n "."
        done
        echo ""
        # 추가 안정화 대기
        sleep 5
        "$ADB_BIN" shell input keyevent 82 2>/dev/null || true  # 화면 잠금 해제
        success "에뮬레이터 부팅 완료 (${ELAPSED}초 소요)"
        DEVICE_SERIAL=$(get_connected_device)
    fi
else
    # 연결된 실제 기기 사용
    DEVICE_SERIAL=$(get_connected_device)
    if [ -z "$DEVICE_SERIAL" ]; then
        error "연결된 기기가 없습니다."
        info "옵션 1: USB 디버깅 기기를 연결한 후 재실행"
        info "옵션 2: --emulator 플래그로 에뮬레이터 자동 시작"
        info "  $0 --emulator"
        exit 1
    fi
    success "기기 연결됨: $DEVICE_SERIAL"
fi

# ─── 3. 에뮬레이터 루트 권한 상승 ───────────────────────────────────────────────
# google_apis 에뮬레이터는 user 빌드라 adb root로 권한 상승해야
# /data/misc/profiles/ 경로에 직접 접근 가능 → "never flushed profiles" 오류 방지
if [[ "$DEVICE_SERIAL" == emulator-* ]]; then
    step "에뮬레이터 루트 권한 상승"
    ROOT_RESULT=$("$ADB_BIN" -s "$DEVICE_SERIAL" root 2>&1 | tr -d '\r')
    if echo "$ROOT_RESULT" | grep -q "cannot\|error\|failed"; then
        warn "adb root 실패: $ROOT_RESULT"
        warn "playstore 이미지는 root 불가 — 프로필 수집이 실패할 수 있습니다"
    else
        success "루트 권한 획득: $ROOT_RESULT"
        sleep 2  # root 재마운트 안정화 대기
        "$ADB_BIN" -s "$DEVICE_SERIAL" remount 2>/dev/null || true
    fi

    # 화면 켜기 + 잠금 해제 (screen-off 상태에서 profile flush 실패 방지)
    step "화면 활성화"
    "$ADB_BIN" -s "$DEVICE_SERIAL" shell settings put system screen_off_timeout 1800000 2>/dev/null || true
    "$ADB_BIN" -s "$DEVICE_SERIAL" shell svc power stayon usb 2>/dev/null || true
    "$ADB_BIN" -s "$DEVICE_SERIAL" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
    "$ADB_BIN" -s "$DEVICE_SERIAL" shell input keyevent 82 2>/dev/null || true  # KEYCODE_MENU → 잠금 해제
    success "화면 활성화 완료"
fi

# logcat 초기화 (테스트 중 앱 크래시 포착용)
"$ADB_BIN" -s "$DEVICE_SERIAL" logcat -c 2>/dev/null || true

# ─── 4. API 레벨 확인 (≥28 필수) ─────────────────────────────────────────────
step "기기 API 레벨 확인"

API_LEVEL=$("$ADB_BIN" -s "$DEVICE_SERIAL" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')
if [ -z "$API_LEVEL" ] || [ "$API_LEVEL" -lt 28 ]; then
    error "Baseline Profile 생성은 API 28 이상의 기기가 필요합니다."
    error "현재 기기 API: ${API_LEVEL:-'알 수 없음'}"
    exit 1
fi

DEVICE_MODEL=$("$ADB_BIN" -s "$DEVICE_SERIAL" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
success "기기: $DEVICE_MODEL (API $API_LEVEL)"

# ─── 5. 현재 프로필 통계 기록 ─────────────────────────────────────────────────
step "현재 Baseline Profile 통계"

PREV_LINE_COUNT=0
PREV_MTIME=""
if [ -f "$PROFILE_PATH" ]; then
    PREV_LINE_COUNT=$(wc -l < "$PROFILE_PATH")
    PREV_MTIME=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$PROFILE_PATH" 2>/dev/null || echo "알 수 없음")
    info "현재 프로필: ${PREV_LINE_COUNT}줄 (마지막 수정: $PREV_MTIME)"
else
    warn "baseline-prof.txt 없음. 신규 생성됩니다."
fi

# ─── 6. Gradle Clean (선택) ───────────────────────────────────────────────────
if ! $SKIP_CLEAN; then
    step "Gradle clean 실행"
    cd "$PROJECT_ROOT"
    ./gradlew :baselineprofile:clean :app:clean --quiet
    success "Clean 완료"
fi

# ─── 7. Baseline Profile 생성 ─────────────────────────────────────────────────
step "Baseline Profile 생성 시작"
info "이 작업은 5~15분 소요될 수 있습니다..."
info "Gradle task: :app:generateReleaseBaselineProfile"
echo ""

cd "$PROJECT_ROOT"

# ADB_SERIAL 환경변수로 기기 지정 (AGP가 ANDROID_SERIAL을 참조)
export ANDROID_SERIAL="$DEVICE_SERIAL"

GENERATE_START=$(date +%s)
if ./gradlew :app:generateReleaseBaselineProfile \
    --rerun-tasks \
    2>&1 | tee /tmp/baseline_profile_gen.log; then
    GENERATE_END=$(date +%s)
    ELAPSED_SECS=$((GENERATE_END - GENERATE_START))
    success "Gradle task 완료 (소요 시간: ${ELAPSED_SECS}초)"
else
    error "Baseline Profile 생성 실패"
    error "Gradle 로그: /tmp/baseline_profile_gen.log"
    # 앱 크래시 여부 확인을 위해 logcat 덤프
    LOGCAT_FILE="/tmp/baseline_profile_logcat_$(date +%Y%m%d_%H%M%S).txt"
    "$ADB_BIN" -s "$DEVICE_SERIAL" logcat -d \
        --pid="$("$ADB_BIN" -s "$DEVICE_SERIAL" shell pidof com.choo.moviefinder 2>/dev/null || echo '')" \
        -s AndroidRuntime:E System.err:W benchmarkRule:* 2>/dev/null \
        > "$LOGCAT_FILE" 2>&1 || \
    "$ADB_BIN" -s "$DEVICE_SERIAL" logcat -d -t 200 > "$LOGCAT_FILE" 2>&1
    error "logcat 덤프: $LOGCAT_FILE"
    if grep -q "FATAL\|AndroidRuntime\|EXCEPTION\|pin\|certificate" "$LOGCAT_FILE" 2>/dev/null; then
        warn "=== 관련 logcat (마지막 30줄) ==="
        grep -i "fatal\|exception\|pin\|certificate\|moviefinder" "$LOGCAT_FILE" | tail -30 | sed 's/^/  /'
    fi
    exit 1
fi

# ─── 8. 결과 검증 ─────────────────────────────────────────────────────────────
step "생성 결과 검증"

if [ ! -f "$PROFILE_PATH" ]; then
    error "baseline-prof.txt가 생성되지 않았습니다: $PROFILE_PATH"
    error "생성 로그를 확인하세요: /tmp/baseline_profile_gen.log"
    exit 1
fi

NEW_LINE_COUNT=$(wc -l < "$PROFILE_PATH")
NEW_MTIME=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$PROFILE_PATH" 2>/dev/null || echo "")
LINE_DIFF=$((NEW_LINE_COUNT - PREV_LINE_COUNT))
LINE_DIFF_STR=""
if [ $LINE_DIFF -gt 0 ]; then
    LINE_DIFF_STR=" (${GREEN}+${LINE_DIFF}${NC})"
elif [ $LINE_DIFF -lt 0 ]; then
    LINE_DIFF_STR=" (${RED}${LINE_DIFF}${NC})"
else
    LINE_DIFF_STR=" (변경 없음)"
fi

hr
echo -e "${BOLD}  생성 완료 요약${NC}"
hr
echo -e "  기기:         $DEVICE_MODEL (API $API_LEVEL)"
echo -e "  소요 시간:    ${ELAPSED_SECS}초"
echo -e "  프로필 위치:  $PROFILE_PATH"
echo -e "  이전 줄 수:   ${PREV_LINE_COUNT}"
echo -e "  현재 줄 수:   ${NEW_LINE_COUNT}${LINE_DIFF_STR}"
echo -e "  수정 시각:    $NEW_MTIME"
hr

# ─── 9. Git 상태 확인 ─────────────────────────────────────────────────────────
cd "$PROJECT_ROOT"
GIT_STATUS=$(git diff --stat -- "app/src/main/baseline-prof.txt" "app/src/release/generated/baselineProfiles/" 2>/dev/null || echo "")
if [ -n "$GIT_STATUS" ]; then
    echo ""
    info "변경된 파일:"
    echo "$GIT_STATUS" | sed 's/^/  /'
    echo ""
    echo -e "${YELLOW}커밋하려면:${NC}"
    echo -e "  git add app/src/main/baseline-prof.txt"
    echo -e "  git add app/src/release/generated/baselineProfiles/"
    echo -e "  git commit -m \"perf: regenerate baseline profile ($(date +%Y-%m-%d))\""
else
    info "프로필 변경 없음 (이미 최신 상태)"
fi

echo ""
success "Baseline Profile 생성 완료!"
