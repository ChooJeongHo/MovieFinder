#!/usr/bin/env node
/**
 * MovieFinder MCP Server
 * ADB 자동화 툴 4종을 Claude Code에 제공합니다.
 *
 * 등록 (~/.claude/settings.json 또는 claude_desktop_config.json):
 *   "mcpServers": {
 *     "moviefinder": {
 *       "command": "node",
 *       "args": ["/Users/serveace/AndroidStudioProjects/MovieFinder/moviefinder-mcp-server.js"]
 *     }
 *   }
 *
 * 의존성 설치: npm install (프로젝트 루트)
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { execFile, spawn } from 'child_process';
import { promisify } from 'util';
import { mkdir, createWriteStream } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const execFileAsync = promisify(execFile);
const mkdirAsync = promisify(mkdir);

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = __dirname;
const PACKAGE = 'com.choo.moviefinder';

// 하단 탭바 좌표 (Samsung Galaxy 1080×2340 기준)
const TAB_COORDS = {
  home:     { x: 135, y: 2150, label: '홈' },
  search:   { x: 405, y: 2150, label: '검색' },
  favorite: { x: 675, y: 2150, label: '즐겨찾기' },
  settings: { x: 945, y: 2150, label: '설정' },
};

const VALID_TABS = new Set(Object.keys(TAB_COORDS));

// folder 파라미터: 경로 순회 방지 (알파벳·숫자·하이픈·언더스코어만 허용)
function sanitizeFolder(raw) {
  const folder = typeof raw === 'string' && raw.trim() ? raw.trim() : 'latest';
  if (!/^[\w\-]+$/.test(folder)) {
    throw new Error(`folder 파라미터에 허용되지 않는 문자가 포함되어 있습니다: "${folder}"`);
  }
  return folder;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// ADB screencap 바이너리 출력을 파일에 직접 파이프 (쉘 리다이렉션 없이)
function captureScreencap(filePath) {
  return new Promise((resolve, reject) => {
    const adb = spawn('adb', ['exec-out', 'screencap', '-p']);
    const fileStream = createWriteStream(filePath);
    adb.stdout.pipe(fileStream);
    adb.stderr.on('data', (d) => process.stderr.write(`[adb] ${d}`));
    adb.on('error', reject);
    adb.on('close', (code) => {
      if (code === 0) resolve();
      else reject(new Error(`adb screencap 종료 코드: ${code}`));
    });
  });
}

// gfxinfo 텍스트에서 핵심 지표 파싱
function parseGfxinfo(raw) {
  const patterns = {
    totalFrames:         /Total frames rendered:\s*(\d+)/,
    jankyFrames:         /Janky frames:\s*(\d+)\s*\(([^)]+)\)/,
    p50:                 /50th percentile:\s*(\d+)ms/,
    p90:                 /90th percentile:\s*(\d+)ms/,
    p95:                 /95th percentile:\s*(\d+)ms/,
    p99:                 /99th percentile:\s*(\d+)ms/,
    slowUiThread:        /Number Slow UI thread:\s*(\d+)/,
    slowBitmapUploads:   /Number Slow bitmap uploads:\s*(\d+)/,
    missedVsync:         /Number Missed Vsync:\s*(\d+)/,
    frameDeadlineMissed: /Number Frame deadline missed:\s*(\d+)/,
    gpuP50:              /50th gpu percentile:\s*(\d+)ms/,
    gpuP99:              /99th gpu percentile:\s*(\d+)ms/,
  };
  const result = {};
  for (const line of raw.split('\n')) {
    for (const [key, pattern] of Object.entries(patterns)) {
      const m = line.match(pattern);
      if (m) result[key] = m[2] ? `${m[1]} (${m[2]})` : m[1];
    }
  }
  return result;
}

function formatPerfReport(data, scrollCount) {
  const p99Val = data.p99 ? parseInt(data.p99) : null;
  const verdict =
    p99Val === null ? '' :
    p99Val <= 16   ? '  ← 🟢 60fps 기준 통과' :
    p99Val <= 32   ? '  ← 🟡 경미한 지연' :
                     '  ← 🔴 성능 저하';

  return [
    `## gfxinfo 성능 측정 결과 (스크롤 ${scrollCount}회)`,
    '',
    '| 항목 | 값 |',
    '|------|-----|',
    `| 총 프레임 | ${data.totalFrames ?? 'N/A'} |`,
    `| 잰키 프레임 | ${data.jankyFrames ?? 'N/A'} |`,
    `| 50th percentile | ${data.p50 ?? 'N/A'}ms |`,
    `| 90th percentile | ${data.p90 ?? 'N/A'}ms |`,
    `| 95th percentile | ${data.p95 ?? 'N/A'}ms |`,
    `| 99th percentile | ${data.p99 ?? 'N/A'}ms${verdict} |`,
    `| Slow UI thread | ${data.slowUiThread ?? 'N/A'} |`,
    `| Slow bitmap uploads | ${data.slowBitmapUploads ?? 'N/A'} |`,
    `| Missed Vsync | ${data.missedVsync ?? 'N/A'} |`,
    `| Frame deadline missed | ${data.frameDeadlineMissed ?? 'N/A'} |`,
    `| GPU 50th | ${data.gpuP50 ?? 'N/A'}ms |`,
    `| GPU 99th | ${data.gpuP99 ?? 'N/A'}ms |`,
  ].join('\n');
}

// Gradle 빌드: spawn + cwd로 쉘 없이 실행
function runGradle(tasks) {
  return new Promise((resolve) => {
    const child = spawn('./gradlew', tasks, {
      cwd: PROJECT_ROOT,
      // PATH 상속 (java, android SDK 등)
      env: process.env,
    });

    let output = '';
    child.stdout.on('data', (d) => { output += d; });
    child.stderr.on('data', (d) => { output += d; });

    child.on('error', (err) => {
      resolve({ code: -1, output: `실행 오류: ${err.message}` });
    });
    child.on('close', (code) => {
      const tail = output.length > 2000
        ? `...(앞부분 생략)...\n${output.slice(-2000)}`
        : output;
      resolve({ code, output: tail });
    });
  });
}

// ─────────────────────────────────────────────
//  MCP Server 정의
// ─────────────────────────────────────────────

const server = new Server(
  { name: 'moviefinder-mcp', version: '1.0.0' },
  { capabilities: { tools: {} } },
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: 'capture_screenshot',
      description:
        '지정한 탭(홈/검색/즐겨찾기/설정)으로 이동 후 스크린샷을 캡처하여 저장합니다.\n' +
        '저장 경로: {프로젝트루트}/screenshot_test/{folder}/{tab}.png',
      inputSchema: {
        type: 'object',
        properties: {
          tab: {
            type: 'string',
            enum: ['home', 'search', 'favorite', 'settings'],
            description: '캡처할 탭 이름',
          },
          folder: {
            type: 'string',
            description:
              '저장 폴더명 (영숫자·하이픈·언더스코어만 허용, 기본값: "latest")',
          },
        },
        required: ['tab'],
      },
    },
    {
      name: 'tap_tab',
      description: '하단 탭바에서 지정한 탭으로 이동합니다.',
      inputSchema: {
        type: 'object',
        properties: {
          tab: {
            type: 'string',
            enum: ['home', 'search', 'favorite', 'settings'],
            description: '이동할 탭 이름',
          },
        },
        required: ['tab'],
      },
    },
    {
      name: 'get_performance',
      description:
        'gfxinfo로 프레임 성능을 측정합니다.\n' +
        '측정 전 reset → 스크롤 실행 → 결과 파싱 후 마크다운 표로 반환합니다.',
      inputSchema: {
        type: 'object',
        properties: {
          scroll_count: {
            type: 'number',
            description: '측정 중 실행할 스크롤 횟수 (기본값: 3)',
          },
        },
      },
    },
    {
      name: 'install_app',
      description:
        './gradlew installDebug를 실행하여 앱을 빌드하고 연결된 기기에 설치합니다.',
      inputSchema: {
        type: 'object',
        properties: {
          clean: {
            type: 'boolean',
            description: 'true이면 clean 후 installDebug 실행 (기본값: false)',
          },
        },
      },
    },
  ],
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args = {} } = request.params;

  // ── capture_screenshot ──────────────────────────────────────────────────
  if (name === 'capture_screenshot') {
    const tab = args.tab;
    if (!VALID_TABS.has(tab)) {
      throw new Error(`유효하지 않은 탭: "${tab}". home | search | favorite | settings 중 하나여야 합니다.`);
    }

    const folder = sanitizeFolder(args.folder);
    const coords = TAB_COORDS[tab];
    const outDir = path.join(PROJECT_ROOT, 'screenshot_test', folder);

    await mkdirAsync(outDir, { recursive: true });

    // execFile: 인자를 배열로 분리 → 쉘 인젝션 불가
    await execFileAsync('adb', [
      'shell', 'input', 'tap',
      String(coords.x), String(coords.y),
    ]);
    await sleep(1000);

    const filePath = path.join(outDir, `${tab}.png`);
    await captureScreencap(filePath);

    return {
      content: [
        {
          type: 'text',
          text: `✅ ${coords.label} 탭 스크린샷 저장 완료\n경로: ${filePath}`,
        },
      ],
    };
  }

  // ── tap_tab ─────────────────────────────────────────────────────────────
  if (name === 'tap_tab') {
    const tab = args.tab;
    if (!VALID_TABS.has(tab)) {
      throw new Error(`유효하지 않은 탭: "${tab}"`);
    }

    const coords = TAB_COORDS[tab];
    await execFileAsync('adb', [
      'shell', 'input', 'tap',
      String(coords.x), String(coords.y),
    ]);
    await sleep(800);

    return {
      content: [
        { type: 'text', text: `✅ ${coords.label} 탭으로 이동했습니다.` },
      ],
    };
  }

  // ── get_performance ─────────────────────────────────────────────────────
  if (name === 'get_performance') {
    const scrollCount =
      typeof args.scroll_count === 'number' && args.scroll_count > 0
        ? Math.min(Math.round(args.scroll_count), 20)
        : 3;

    // gfxinfo 카운터 리셋
    await execFileAsync('adb', [
      'shell', 'dumpsys', 'gfxinfo', PACKAGE, 'reset',
    ]);
    await sleep(300);

    // 스크롤 실행 (화면 중앙 아래→위 스와이프)
    for (let i = 0; i < scrollCount; i++) {
      await execFileAsync('adb', [
        'shell', 'input', 'swipe', '540', '1500', '540', '500', '400',
      ]);
      await sleep(600);
    }

    // framestats 수집
    const { stdout } = await execFileAsync('adb', [
      'shell', 'dumpsys', 'gfxinfo', PACKAGE, 'framestats',
    ], { maxBuffer: 4 * 1024 * 1024 });

    const summary = parseGfxinfo(stdout);
    return {
      content: [
        { type: 'text', text: formatPerfReport(summary, scrollCount) },
      ],
    };
  }

  // ── install_app ─────────────────────────────────────────────────────────
  if (name === 'install_app') {
    const clean = args.clean === true;
    const tasks = clean ? ['clean', 'installDebug'] : ['installDebug'];

    process.stderr.write(`[moviefinder-mcp] gradlew ${tasks.join(' ')} 시작\n`);
    const { code, output } = await runGradle(tasks);

    return {
      content: [
        {
          type: 'text',
          text: code === 0
            ? `✅ gradlew ${tasks.join(' ')} 성공\n\n${output}`
            : `❌ gradlew ${tasks.join(' ')} 실패 (exit ${code})\n\n${output}`,
        },
      ],
    };
  }

  throw new Error(`알 수 없는 툴: ${name}`);
});

const transport = new StdioServerTransport();
await server.connect(transport);
process.stderr.write('[moviefinder-mcp] 서버 시작됨\n');
