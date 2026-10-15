import { useEffect, useState } from 'react'
import { api } from '../api'
import { blogLink, mainLink } from '../blogUrl'
import Avatar from '../components/Avatar'
import PostList from '../components/PostList'
import SmartLink from '../components/SmartLink'
import SubscribeButton from '../components/SubscribeButton'

// SUB-02 구독한 블로그, SUB-03 구독 피드: 구독한 블로그의 공개 글을 새로 올라온 순서로
export default function FeedPage() {
  const [blogs, setBlogs] = useState(null)
  const [items, setItems] = useState(null)
  const [cursor, setCursor] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = async (from) => {
    setBusy(true)
    try {
      const data = await api('GET', `/api/feed${from ? `?cursor=${encodeURIComponent(from)}` : ''}`)
      setItems((prev) => (from ? [...(prev ?? []), ...data.items] : data.items))
      setCursor(data.nextCursor)
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const loadBlogs = () => api('GET', '/api/me/subscriptions').then(setBlogs).catch((err) => setError(err.message))
  useEffect(() => {
    void loadBlogs()
    void load(null)
    api('GET', '/api/me/recommended-blogs').then(setRecommended).catch(() => setRecommended([]))
  }, [])

  // SUB-06 추천 블로그. 여기서 구독하면 버튼만 "구독중"으로 바꾸고, 구독 목록과 피드를 새로 불러온다
  const [recommended, setRecommended] = useState([])
  const onSubscribe = (address, r) => {
    setRecommended((list) => list.map((b) => (b.address === address ? { ...b, ...r } : b)))
    void loadBlogs()
    void load(null)
  }

  return (
    <div className="feed">
      <div className="section-head">
        <h2>구독 피드</h2>
        <p className="hint">구독한 블로그의 새 글을 모아 보여 줘요.</p>
      </div>
      {blogs && blogs.length > 0 && (
        <ul className="subscribed-blogs" aria-label="구독한 블로그">
          {blogs.map((b) => (
            <li key={b.address}>
              <SmartLink to={blogLink(b.address, '')} className="subscribed-blog">
                <Avatar name={b.name} src={b.profileImageUrl} size={28} />
                <span>{b.name}</span>
                {b.mutual && <span className="mutual-badge">맞구독</span>}
              </SmartLink>
            </li>
          ))}
        </ul>
      )}
      {error && <div className="error">{error}</div>}
      {/* 추천할 블로그가 없으면 영역을 숨긴다 */}
      {recommended.length > 0 && (
        <section className="recommend">
          <h3>추천 블로그</h3>
          <p className="hint">아직 구독하지 않은 인기 블로그예요.</p>
          <ul className="blogger-list">
            {recommended.map((b) => (
              <li key={b.address} className="blogger-card">
                <SmartLink to={blogLink(b.address, '')} className="blogger-link">
                  <Avatar name={b.name} src={b.profileImageUrl} size={56} />
                  <strong>{b.name}</strong>
                  <small>구독자 {b.subscriberCount}명</small>
                </SmartLink>
                <SubscribeButton blog={b} address={b.address} onChange={(r) => onSubscribe(b.address, r)} />
              </li>
            ))}
          </ul>
        </section>
      )}
      {blogs && blogs.length === 0 ? (
        <div className="empty">
          <p>아직 구독한 블로그가 없어요.</p>
          <p className="hint">{recommended.length > 0 ? '위의 추천 블로그를 구독해 보세요. 새 글이 여기에 모여요.' : '마음에 드는 블로그에서 "+ 구독"을 누르면 새 글이 여기에 모여요.'}</p>
          <SmartLink to={mainLink('/')} className="button primary">최신 글 둘러보기</SmartLink>
        </div>
      ) : (
        items && <PostList items={items} showBlog layout="grid" empty="구독한 블로그에 아직 공개 글이 없어요." />
      )}
      {cursor && <p className="center"><button onClick={() => load(cursor)} disabled={busy}>{busy ? '불러오는 중…' : '더보기'}</button></p>}
    </div>
  )
}
