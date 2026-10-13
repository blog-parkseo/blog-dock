import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { blogLink } from '../blogUrl'
import { timeAgo } from '../format'
import Avatar from './Avatar'
import SmartLink from './SmartLink'

// HOME-05 홈 테마 피드. 글이 있는 테마만 버튼으로 보여 주고, 처음엔 글이 가장 많은 테마를 연다.
// 주소에 ?theme=TRAVEL 이 있으면 그 테마를 열고 이 영역으로 내려온다 (글 화면의 테마 표시에서 들어올 때)
export default function ThemeFeed() {
  const [params, setParams] = useSearchParams()
  const [themes, setThemes] = useState(null)
  const [feed, setFeed] = useState(null)
  const [busy, setBusy] = useState(false)
  const ref = useRef(null)
  const asked = params.get('theme')

  useEffect(() => { api('GET', '/api/home/themes').then(setThemes).catch(() => setThemes([])) }, [])

  const available = (themes ?? []).filter((t) => t.count > 0)
  const busiest = [...available].sort((a, b) => b.count - a.count)[0]?.code
  const current = available.some((t) => t.code === asked) ? asked : busiest

  const load = useCallback(async (code, cursor) => {
    setBusy(true)
    try {
      const data = await api('GET', `/api/home/themes/${code}/posts${cursor ? `?cursor=${encodeURIComponent(cursor)}` : ''}`)
      setFeed((prev) => (cursor && prev ? { ...data, items: [...prev.items, ...data.items] } : data))
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => { if (current) load(current, null) }, [current, load])
  useEffect(() => {
    if (asked && current === asked && ref.current) ref.current.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [asked, current])

  // 테마가 달린 공개 글이 하나도 없으면 영역을 숨긴다
  if (!themes || !available.length || !feed) return null
  const pick = (code) => setParams((p) => { const next = new URLSearchParams(p); next.set('theme', code); return next }, { replace: true })

  return (
    <section className="theme-feed" ref={ref}>
      <div className="theme-tabs" role="tablist" aria-label="테마">
        {available.map((t) => (
          <button key={t.code} role="tab" aria-selected={t.code === current}
            className={t.code === current ? 'active' : ''} onClick={() => pick(t.code)}>
            {t.label}
          </button>
        ))}
      </div>
      <header className="theme-head">
        <p className="theme-eyebrow">THEME</p>
        <h2>{feed.theme.title} <span aria-hidden="true">{feed.theme.emoji}</span></h2>
        <p className="theme-desc">{feed.theme.description}</p>
      </header>
      <ul className="theme-list">
        {feed.items.map((p) => (
          <li key={p.id}>
            <SmartLink to={blogLink(p.blog.address, p.slug)} className="theme-item">
              <div className="theme-text">
                <p className="theme-blog"><Avatar name={p.blog.name} src={p.blogImageUrl} size={26} /> {p.blog.name}</p>
                <h3>{p.title}</h3>
                <p className="theme-excerpt">{p.excerpt}</p>
                <p className="theme-meta">
                  <span aria-label={`공감 ${p.likeCount}`}>♡ {p.likeCount}</span>
                  <span aria-label={`댓글 ${p.commentCount}`}>💬 {p.commentCount}</span>
                  <time>{timeAgo(p.publishedAt)}</time>
                </p>
              </div>
              {p.thumbnailUrl && <img className="theme-thumb" src={p.thumbnailUrl} alt="" loading="lazy" />}
            </SmartLink>
          </li>
        ))}
      </ul>
      {feed.nextCursor && (
        <p className="center"><button onClick={() => load(current, feed.nextCursor)} disabled={busy}>{busy ? '불러오는 중…' : '더보기'}</button></p>
      )}
    </section>
  )
}
