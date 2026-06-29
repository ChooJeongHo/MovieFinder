#!/bin/bash
# MovieFinder 릴리즈 자동화 스크립트
# 사용법: ./scripts/release.sh [patch|minor|major] [OPTIONS]
#
# OPTIONS:
#   --dry-run    변경 없이 결과만 미리 보기
#   --no-push    커밋/태그 생성 후 push 생략
#   --build      로컬에서 release AAB 빌드 (TMDB_API_KEY 필요)
#   --help       도움말

set -euo pipefail

# ─── 색상 ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_CONFIG="$PROJECT_ROOT/buildSrc/src/main/kotlin/AndroidConfig.kt"
CHANGELOG="$PROJECT_ROOT/CHANGELOG.md"

info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC}   $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
die()     { echo -e "${RED}[ERR]${NC}  $*" >&2; exit 1; }
step()    { echo -e "\n${BOLD}▶ $*${NC}"; }

usage() {
    cat <<EOF
사용법: $0 [patch|minor|major] [OPTIONS]

  patch    패치 버전 증가: 1.0.0 → 1.0.1  (기본값)
  minor    마이너 버전 증가: 1.0.0 → 1.1.0
  major    메이저 버전 증가: 1.0.0 → 2.0.0

OPTIONS:
  --dry-run    변경 없이 결과만 미리 보기
  --no-push    커밋/태그 생성 후 push 생략
  --build      로컬에서 release AAB 빌드
  --help       도움말

예시:
  $0 patch                    # 패치 릴리즈
  $0 minor --dry-run          # 마이너 릴리즈 시뮬레이션
  $0 major --build --no-push  # 메이저 릴리즈 + 로컬 빌드, push 없음
EOF
}

# ─── 옵션 파싱 ────────────────────────────────────────────────────────────────
BUMP_TYPE="patch"
DRY_RUN=false
NO_PUSH=false
BUILD_AAB=false

for arg in "$@"; do
    case $arg in
        patch|minor|major) BUMP_TYPE="$arg" ;;
        --dry-run)  DRY_RUN=true ;;
        --no-push)  NO_PUSH=true ;;
        --build)    BUILD_AAB=true ;;
        --help)     usage; exit 0 ;;
        *) die "알 수 없는 옵션: $arg\n$(usage)" ;;
    esac
done

cd "$PROJECT_ROOT"

# ─── Step 1: 사전 검사 ────────────────────────────────────────────────────────
step "사전 검사"

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
[[ "$CURRENT_BRANCH" == "main" ]] || die "릴리즈는 main 브랜치에서만 실행 가능합니다. (현재: $CURRENT_BRANCH)"

if ! git diff --quiet || ! git diff --cached --quiet; then
    die "커밋되지 않은 변경사항이 있습니다. 먼저 커밋하거나 stash 하세요."
fi

[[ -f "$ANDROID_CONFIG" ]] || die "AndroidConfig.kt를 찾을 수 없습니다: $ANDROID_CONFIG"
grep -q "TMDB_API_KEY" local.properties 2>/dev/null || die "local.properties에 TMDB_API_KEY가 없습니다."

success "사전 검사 통과"

# ─── Step 2: 현재 버전 읽기 ───────────────────────────────────────────────────
step "현재 버전 읽기"

CURRENT_VERSION_NAME=$(grep 'VERSION_NAME' "$ANDROID_CONFIG" | grep -oE '"[^"]+"' | tr -d '"')
CURRENT_VERSION_CODE=$(grep 'VERSION_CODE' "$ANDROID_CONFIG" | grep -oE '[0-9]+' | head -1)

# 2-part 버전을 3-part semver로 정규화 (예: "1.0" → "1.0.0")
IFS='.' read -ra PARTS <<< "$CURRENT_VERSION_NAME"
MAJOR="${PARTS[0]:-1}"; MINOR="${PARTS[1]:-0}"; PATCH="${PARTS[2]:-0}"

info "현재: v${MAJOR}.${MINOR}.${PATCH} (versionCode: ${CURRENT_VERSION_CODE})"

# ─── Step 3: 버전 증가 ────────────────────────────────────────────────────────
step "버전 증가 ($BUMP_TYPE)"

case $BUMP_TYPE in
    major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
    patch) PATCH=$((PATCH + 1)) ;;
esac

NEW_VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}"
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
TAG_NAME="v${NEW_VERSION_NAME}"

# 태그 중복 확인
if git tag -l "$TAG_NAME" | grep -q .; then
    die "태그 ${TAG_NAME}이 이미 존재합니다."
fi

info "새 버전: ${TAG_NAME} (versionCode: ${NEW_VERSION_CODE})"

# ─── Step 4: 릴리즈 노트 생성 ────────────────────────────────────────────────
step "릴리즈 노트 생성"

LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
LOG_RANGE="${LAST_TAG:+${LAST_TAG}..}HEAD"

if [[ -n "$LAST_TAG" ]]; then
    info "범위: ${LAST_TAG} → HEAD ($(git rev-list "${LAST_TAG}..HEAD" --count)개 커밋)"
else
    info "범위: 전체 히스토리"
fi

# Conventional commit 그룹 파싱
_parse_commits() {
    local pattern="$1" prefix="$2"
    git log $LOG_RANGE --no-merges --format="%s" \
        | grep -E "^${pattern}(\(.+\))?:" \
        | sed "s/^${pattern}[^:]*: /- /" \
        || true
}

FEATURES=$(_parse_commits "feat" "feat")
FIXES=$(_parse_commits "fix" "fix")
PERF=$(_parse_commits "perf" "perf")
CHORES=$(git log $LOG_RANGE --no-merges --format="%s" \
    | grep -E "^(chore|refactor|build|ci)(\(.+\))?:" \
    | sed 's/^[^:]*: /- /' || true)

# 빌드 릴리즈 노트 (GitHub Release body 용)
RELEASE_BODY=""
[[ -n "$FEATURES" ]] && RELEASE_BODY+="### 새 기능"$'\n'"$FEATURES"$'\n\n'
[[ -n "$FIXES"    ]] && RELEASE_BODY+="### 버그 수정"$'\n'"$FIXES"$'\n\n'
[[ -n "$PERF"     ]] && RELEASE_BODY+="### 성능 개선"$'\n'"$PERF"$'\n\n'
[[ -n "$CHORES"   ]] && RELEASE_BODY+="### 기타 변경"$'\n'"$CHORES"$'\n\n'

if [[ -z "$RELEASE_BODY" ]]; then
    RELEASE_BODY="- 버그 수정 및 안정성 개선"$'\n\n'
fi

[[ -n "$LAST_TAG" ]] && \
    RELEASE_BODY+="**Full Changelog:** https://github.com/ChooJeongHo/MovieFinder/compare/${LAST_TAG}...${TAG_NAME}"

echo ""
echo "────────────────────────── 릴리즈 노트 미리보기 ──────────────────────────"
echo -e "$RELEASE_BODY"
echo "──────────────────────────────────────────────────────────────────────────"

if [[ "$DRY_RUN" == "true" ]]; then
    warn "[DRY-RUN] 실제 변경 없이 종료합니다."
    exit 0
fi

# ─── Step 5: AndroidConfig.kt 버전 업데이트 ──────────────────────────────────
step "AndroidConfig.kt 업데이트"

sed -i '' \
    "s/const val VERSION_CODE = .*/const val VERSION_CODE = ${NEW_VERSION_CODE}/" \
    "$ANDROID_CONFIG"
sed -i '' \
    "s/const val VERSION_NAME = .*/const val VERSION_NAME = \"${NEW_VERSION_NAME}\"/" \
    "$ANDROID_CONFIG"

# 검증
grep "VERSION_CODE\|VERSION_NAME" "$ANDROID_CONFIG"
success "AndroidConfig.kt 업데이트 완료"

# ─── Step 6: CHANGELOG.md 업데이트 ───────────────────────────────────────────
step "CHANGELOG.md 업데이트"

TODAY=$(date +%Y-%m-%d)
CHANGELOG_ENTRY="## [${NEW_VERSION_NAME}] — ${TODAY}"$'\n\n'"${RELEASE_BODY}"

if [[ -f "$CHANGELOG" ]]; then
    TMP=$(mktemp)
    {
        head -1 "$CHANGELOG"
        echo ""
        echo "$CHANGELOG_ENTRY"
        echo ""
        tail -n +2 "$CHANGELOG"
    } > "$TMP"
    mv "$TMP" "$CHANGELOG"
else
    printf "# Changelog\n\n%s\n" "$CHANGELOG_ENTRY" > "$CHANGELOG"
fi

success "CHANGELOG.md 업데이트 완료"

# ─── Step 7: Git 커밋 & 태그 생성 ────────────────────────────────────────────
step "Git 커밋 & 태그"

git add "$ANDROID_CONFIG" "$CHANGELOG"
git commit -m "chore(release): bump version to ${TAG_NAME}"

# 태그 annotation에 릴리즈 노트 포함 (GitHub Actions에서 추출하여 Release body로 사용)
git tag -a "${TAG_NAME}" -m "${RELEASE_BODY}"

success "태그 생성: ${TAG_NAME}"

# ─── Step 8: Push ─────────────────────────────────────────────────────────────
if [[ "$NO_PUSH" == "false" ]]; then
    step "Push"
    git push origin main
    git push origin "${TAG_NAME}"
    success "Push 완료 — GitHub Actions 릴리즈 워크플로우가 시작됩니다."
    info "진행 확인: https://github.com/ChooJeongHo/MovieFinder/actions"
    info "릴리즈:   https://github.com/ChooJeongHo/MovieFinder/releases/tag/${TAG_NAME}"
else
    warn "--no-push: push를 건너뜁니다."
    info "나중에 실행:\n  git push origin main && git push origin ${TAG_NAME}"
fi

# ─── Step 9: 로컬 AAB 빌드 (선택) ────────────────────────────────────────────
if [[ "$BUILD_AAB" == "true" ]]; then
    step "릴리즈 AAB 로컬 빌드"
    ./gradlew bundleRelease
    AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
    if [[ -f "$AAB_PATH" ]]; then
        AAB_SIZE=$(du -sh "$AAB_PATH" | cut -f1)
        success "AAB 생성 완료 (${AAB_SIZE}): ${AAB_PATH}"
    else
        die "AAB 파일을 찾을 수 없습니다: $AAB_PATH"
    fi
fi

echo ""
echo -e "${GREEN}${BOLD}✓ 릴리즈 ${TAG_NAME} 완료!${NC}"
