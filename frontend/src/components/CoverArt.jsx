// 블로그 위쪽 배경. 그림 4가지(노을빛·풀빛·하늘빛·밤빛)와 색 6가지, 고르지 않으면 주소로 정하는 자동 색
import { colorOf } from './Avatar'

export const COVER_SCENES = [
  { code: 'SUNSET', name: '노을빛' },
  { code: 'MEADOW', name: '풀빛' },
  { code: 'SKYFIELD', name: '하늘빛' },
  { code: 'NIGHT', name: '밤빛' },
]

export const COVER_COLORS = [
  { code: 'SKY', name: '하늘', color: '#4a90d9' },
  { code: 'MINT', name: '민트', color: '#2bb39b' },
  { code: 'LAVENDER', name: '라벤더', color: '#9b7fd4' },
  { code: 'LEMON', name: '레몬', color: '#e2b822' },
  { code: 'ROSE', name: '장미', color: '#e0607e' },
  { code: 'GRAY', name: '회색', color: '#7a7f87' },
]

export const isScene = (code) => COVER_SCENES.some((s) => s.code === code)

// 색 배경이면 그 색, 자동이면 주소로 정한 색
export function coverColor(cover, address) {
  return COVER_COLORS.find((c) => c.code === cover)?.color ?? colorOf(address)
}

// 늘 같은 자리에 찍히는 별·풀잎 (새로 그릴 때마다 바뀌지 않게)
function random(seed) {
  let s = seed
  return () => {
    s = (s * 16807) % 2147483647
    return (s - 1) / 2147483646
  }
}

const STARS = (() => {
  const r = random(7)
  return Array.from({ length: 70 }, () => ({ x: r() * 1200, y: 30 + r() * 150, size: 0.6 + r() * 1.6, o: 0.4 + r() * 0.6 }))
})()

function grass(seed, count, baseY, height, color) {
  const r = random(seed)
  let d = ''
  for (let i = 0; i < count; i++) {
    const x = (i / count) * 1220 - 10 + r() * 8
    const h = height * (0.5 + r() * 0.7)
    const lean = (r() - 0.5) * 14
    d += `M${x.toFixed(1)} ${baseY} q${(lean / 2).toFixed(1)} ${(-h / 2).toFixed(1)} ${lean.toFixed(1)} ${(-h).toFixed(1)} `
  }
  return <path d={d} stroke={color} strokeWidth="2" strokeLinecap="round" fill="none" />
}

// 나무 한 그루: 줄기 + 둥근 잎 세 덩이
function Tree({ x, y, s = 1, leaf, trunk }) {
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <rect x="-4" y="-34" width="8" height="34" rx="3" fill={trunk} />
      <circle cx="0" cy="-52" r="22" fill={leaf} />
      <circle cx="-16" cy="-40" r="15" fill={leaf} />
      <circle cx="16" cy="-40" r="15" fill={leaf} />
    </g>
  )
}

// 뾰족한 침엽수 (밤·노을 실루엣)
function Pine({ x, y, s = 1, color }) {
  return <path transform={`translate(${x} ${y}) scale(${s})`} d="M0 -70 L18 -30 L9 -30 L24 0 L-24 0 L-9 -30 L-18 -30 Z" fill={color} />
}

function Sunset() {
  return (
    <>
      <defs>
        <linearGradient id="sunset-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#ffe3c4" />
          <stop offset="0.45" stopColor="#ffb98a" />
          <stop offset="0.8" stopColor="#f2857a" />
          <stop offset="1" stopColor="#c8617d" />
        </linearGradient>
        <radialGradient id="sunset-glow" cx="0.69" cy="0.58" r="0.45">
          <stop offset="0" stopColor="#fff2c2" stopOpacity="0.9" />
          <stop offset="1" stopColor="#fff2c2" stopOpacity="0" />
        </radialGradient>
      </defs>
      <rect width="1200" height="300" fill="url(#sunset-sky)" />
      <rect width="1200" height="300" fill="url(#sunset-glow)" />
      <circle cx="830" cy="168" r="48" fill="#ffe7a3" />
      <circle cx="830" cy="168" r="62" fill="#ffe7a3" opacity="0.35" />
      <path d="M0 230 L120 190 L230 215 L360 170 L470 205 L560 180 L700 150 L820 195 L930 140 L1060 185 L1200 150 L1200 300 L0 300 Z" fill="#a8587a" opacity="0.75" />
      <path d="M0 250 L150 215 L300 240 L450 205 L620 235 L760 200 L900 230 L1040 205 L1200 225 L1200 300 L0 300 Z" fill="#7d4466" />
      <path d="M0 268 Q300 250 600 262 T1200 255 L1200 300 L0 300 Z" fill="#5a3350" />
      <Pine x={1010} y={262} s={0.9} color="#3e2340" />
      <Pine x={1050} y={265} s={1.2} color="#3e2340" />
      <Pine x={1100} y={262} s={0.8} color="#3e2340" />
      <Tree x={800} y={264} s={0.8} leaf="#3e2340" trunk="#3e2340" />
      {grass(11, 160, 300, 22, '#4a2b45')}
    </>
  )
}

function Meadow() {
  return (
    <>
      <defs>
        <linearGradient id="meadow-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#f6fbe8" />
          <stop offset="1" stopColor="#dcefc4" />
        </linearGradient>
      </defs>
      <rect width="1200" height="300" fill="url(#meadow-sky)" />
      <circle cx="1080" cy="82" r="30" fill="#fff6c9" />
      <path d="M0 215 Q250 170 520 200 T1200 170 L1200 300 L0 300 Z" fill="#b9dc8f" />
      <path d="M0 245 Q300 205 640 235 T1200 215 L1200 300 L0 300 Z" fill="#94c76c" />
      <path d="M0 270 Q350 245 700 262 T1200 250 L1200 300 L0 300 Z" fill="#73b04f" />
      <Tree x={1000} y={214} s={0.9} leaf="#6aa648" trunk="#7a5a3a" />
      <Tree x={1110} y={206} s={0.7} leaf="#5d9a40" trunk="#7a5a3a" />
      {grass(3, 220, 300, 30, '#5a9a3c')}
      {grass(5, 140, 300, 44, '#4c8a32')}
      {[[620, 272, '#ffffff'], [700, 280, '#ffd34d'], [820, 268, '#ffffff'], [905, 282, '#f7a8c4'], [1050, 276, '#ffd34d'], [1150, 284, '#ffffff']].map(([x, y, c]) => (
        <g key={x}>
          <circle cx={x} cy={y} r="5" fill={c} />
          <circle cx={x} cy={y} r="2" fill="#f2b321" />
        </g>
      ))}
    </>
  )
}

function SkyField() {
  return (
    <>
      <defs>
        <linearGradient id="skyfield-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#cfe8ff" />
          <stop offset="0.7" stopColor="#eaf5ff" />
          <stop offset="1" stopColor="#f6fbff" />
        </linearGradient>
      </defs>
      <rect width="1200" height="300" fill="url(#skyfield-sky)" />
      {[[800, 95, 1], [1030, 130, 0.8], [600, 70, 0.6], [1130, 75, 0.55]].map(([x, y, s]) => (
        <g key={x} transform={`translate(${x} ${y}) scale(${s})`} fill="#ffffff">
          <ellipse cx="0" cy="0" rx="60" ry="22" />
          <circle cx="-22" cy="-12" r="22" />
          <circle cx="16" cy="-20" r="28" />
        </g>
      ))}
      <path d="M0 240 Q320 205 640 228 T1200 210 L1200 300 L0 300 Z" fill="#a9d98a" />
      <path d="M0 265 Q380 240 760 258 T1200 245 L1200 300 L0 300 Z" fill="#86c46a" />
      <Tree x={960} y={232} s={0.75} leaf="#6fb257" trunk="#8a6a4a" />
      {grass(9, 160, 300, 22, '#6aae52')}
    </>
  )
}

function Night() {
  return (
    <>
      <defs>
        <linearGradient id="night-sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#141a3c" />
          <stop offset="0.65" stopColor="#2b2f63" />
          <stop offset="1" stopColor="#5a4376" />
        </linearGradient>
      </defs>
      <rect width="1200" height="300" fill="url(#night-sky)" />
      {STARS.map((s, i) => <circle key={i} cx={s.x} cy={s.y} r={s.size} fill="#fff" opacity={s.o} />)}
      <circle cx="1030" cy="88" r="28" fill="#fff4cf" />
      <circle cx="1042" cy="80" r="25" fill="#20264f" />
      <path d="M0 230 L120 190 L230 215 L360 170 L470 205 L560 180 L700 150 L820 195 L930 140 L1060 185 L1200 150 L1200 300 L0 300 Z" fill="#2a2550" />
      <path d="M0 250 L150 215 L300 240 L450 205 L620 235 L760 200 L900 230 L1040 205 L1200 225 L1200 300 L0 300 Z" fill="#1f1c40" />
      <path d="M0 268 Q300 250 600 262 T1200 255 L1200 300 L0 300 Z" fill="#161431" />
      <Pine x={1010} y={262} s={0.9} color="#0f0e24" />
      <Pine x={1050} y={265} s={1.2} color="#0f0e24" />
      <Pine x={1100} y={262} s={0.8} color="#0f0e24" />
      <Tree x={800} y={264} s={0.8} leaf="#0f0e24" trunk="#0f0e24" />
      {grass(11, 160, 300, 22, '#0f0e24')}
    </>
  )
}

const SCENES = { SUNSET: Sunset, MEADOW: Meadow, SKYFIELD: SkyField, NIGHT: Night }

// 블로그 위쪽(그리고 고르는 칸의 견본)에 깔리는 그림
export default function CoverArt({ code, className = 'cover-art' }) {
  const Scene = SCENES[code]
  if (!Scene) return null
  return (
    <svg className={className} viewBox="0 0 1200 300" preserveAspectRatio="xMaxYMax slice" aria-hidden="true">
      <Scene />
    </svg>
  )
}
