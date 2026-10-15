// 블로그독 발자국 로고
export default function Logo({ size = 28 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" aria-hidden="true">
      <rect width="64" height="64" rx="16" fill="var(--accent)" />
      <g fill="#fff">
        <ellipse cx="32" cy="40" rx="12" ry="10" />
        <ellipse cx="18" cy="26" rx="5" ry="6.5" />
        <ellipse cx="46" cy="26" rx="5" ry="6.5" />
        <ellipse cx="26" cy="16" rx="4.5" ry="6" />
        <ellipse cx="38" cy="16" rx="4.5" ry="6" />
      </g>
    </svg>
  )
}
