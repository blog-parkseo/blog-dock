import { useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import { blogLink, mainLink } from '../blogUrl'
import Avatar from '../components/Avatar'
import PostList from '../components/PostList'
import SmartLink from '../components/SmartLink'
import ThemeFeed from '../components/ThemeFeed'

// HOME-01 홈 최신 글: 모든 블로그의 공개 글. 더보기로 이어 불러온다 (커서라 중복·누락이 없다)
export default function HomePage() {
  const { me } = useAuth()
  const [items, setItems] = useState(null)
  const [cursor, setCursor] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = async (from) => {
    setBusy(true)
    try {
      const data = await api('GET', `/api/home/posts${from ? `?cursor=${encodeURIComponent(from)}` : ''}`)
      setItems((prev) => (from ? [...(prev ?? []), ...data.items] : data.items))
      setCursor(data.nextCursor)
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => { load(null) }, [])

  // HOME-02 인기 글 (서버가 10분마다 새로 계산한다)
  const [popular, setPopular] = useState(null)
  useEffect(() => { api('GET', '/api/home/popular').then(setPopular).catch(() => setPopular([])) }, [])

  let cta = { to: mainLink('/login'), label: '로그인하고 시작하기' }
  if (me) cta = me.blogAddress ? { to: mainLink('/write'), label: '새 글 쓰기' } : { to: mainLink('/blog/new'), label: '내 블로그 만들기' }

  return (
    <div className="home">
      <section className="hero">
        <div className="hero-text">
          <p className="eyebrow">{me ? `${me.nickname}님, 반가워요` : '누구나 읽고, 누구나 쓰는'}</p>
          <h1>오늘의 이야기를<br />기록해 보세요</h1>
          <p className="hero-sub">산책 일기부터 개발 노트까지. 나만의 블로그를 열고 글을 모아 보세요.</p>
          <SmartLink to={cta.to} className="button primary large">{cta.label}</SmartLink>
        </div>
        <div className="hero-art" aria-hidden="true">
          <span className="paper p1" /><span className="paper p2" /><span className="paper p3" />
        </div>
      </section>
      <div className="section-head">
        <h2>인기 글</h2>
        <p className="hint">최근 7일 동안 공감과 댓글을 많이 받은 글이에요.</p>
      </div>
      {popular && (popular.length === 0
        ? <p className="empty popular-empty">아직 인기 글이 없어요. 아래 최신 글을 둘러보세요.</p>
        : (
          <ol className="popular-list">
            {popular.map((p, i) => (
              <li key={p.id}>
                <SmartLink to={blogLink(p.blog.address, p.slug)} className="popular-item">
                  <span className="rank">{i + 1}</span>
                  <span className="popular-text">
                    <strong>{p.title}</strong>
                    <small><Avatar name={p.blog.name} size={18} /> {p.blog.name}</small>
                  </span>
                  {p.thumbnailUrl && <img src={p.thumbnailUrl} alt="" loading="lazy" />}
                </SmartLink>
              </li>
            ))}
          </ol>
        ))}
      <div className="section-head">
        <h2>최신 글</h2>
        <p className="hint">모든 블로그의 공개 글을 새로 올라온 순서로 보여 줘요.</p>
      </div>
      {error && <div className="error">{error}</div>}
      {items && <PostList items={items} showBlog layout="grid" empty="아직 올라온 글이 없어요. 첫 글의 주인공이 되어 보세요." />}
      {cursor && <p className="center"><button onClick={() => load(cursor)} disabled={busy}>{busy ? '불러오는 중…' : '더보기'}</button></p>}
      <ThemeFeed />
    </div>
  )
}
