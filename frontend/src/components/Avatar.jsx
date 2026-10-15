// 프로필 이미지. 없으면 이름 첫 글자를 색 동그라미에 넣는다
const COLORS = ['#d9622b', '#2f7d6d', '#5b6ee1', '#c2410c', '#7c5cbf', '#b45309', '#0f766e', '#be185d']

export function colorOf(text = '') {
  let h = 0
  for (const ch of text) h = (h * 31 + ch.codePointAt(0)) >>> 0
  return COLORS[h % COLORS.length]
}

export default function Avatar({ name = '', src, size = 40, className = '' }) {
  const style = { width: size, height: size, fontSize: Math.round(size * 0.42) }
  if (src) return <img className={`avatar ${className}`} src={src} alt="" style={style} />
  return (
    <span className={`avatar avatar-initial ${className}`} style={{ ...style, background: colorOf(name) }} aria-hidden="true">
      {[...name.trim()][0] ?? '?'}
    </span>
  )
}
