import { useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import { mainLink } from '../blogUrl'
import PostList from '../components/PostList'
import SmartLink from '../components/SmartLink'

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
        <h2>최신 글</h2>
        <p className="hint">모든 블로그의 공개 글을 새로 올라온 순서로 보여 줘요.</p>
      </div>
      {error && <div className="error">{error}</div>}
      {items && <PostList items={items} showBlog layout="grid" empty="아직 올라온 글이 없어요. 첫 글의 주인공이 되어 보세요." />}
      {cursor && <p className="center"><button onClick={() => load(cursor)} disabled={busy}>{busy ? '불러오는 중…' : '더보기'}</button></p>}
    </div>
  )
}
