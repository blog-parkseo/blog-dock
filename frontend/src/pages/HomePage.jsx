import { useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import PostList from '../components/PostList'

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

  return (
    <div>
      <section className="hero">
        <h1>블로그독</h1>
        <p>{me ? `${me.nickname}님, 반가워요.` : '누구나 읽고, 로그인하면 내 블로그를 열 수 있어요.'}</p>
      </section>
      <h2>최신 글</h2>
      {error && <div className="error">{error}</div>}
      {items && <PostList items={items} showBlog empty="아직 올라온 글이 없어요." />}
      {cursor && <p><button onClick={() => load(cursor)} disabled={busy}>{busy ? '불러오는 중…' : '더보기'}</button></p>}
    </div>
  )
}
