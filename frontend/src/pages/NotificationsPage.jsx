import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { blogLink } from '../blogUrl'
import { formatDateTime } from '../format'
import { NOTIFICATIONS_CHANGED } from '../components/NotificationBell'

const ICON = { COMMENT: '💬', LIKE: '♥', SUBSCRIBE: '➕', NEW_POST: '📝', MUTUAL: '🤝' }
const changed = () => window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED))

// SUB-05 구독 알림 옆 맞구독 버튼. 상대 블로그가 아직 없으면 예약 맞구독(SUB-07)
function SubscribeBack({ n, onClick }) {
  if (n.address) {
    if (n.subscribed) return <button className="notification-side subscribe subscribed mutual" disabled>✓ 맞구독중</button>
    return <button className="notification-side subscribe primary" onClick={() => onClick(false)}>+ 맞구독하기</button>
  }
  if (n.reserved) {
    return (
      <span className="notification-side reserve">
        <button className="subscribe subscribed" onClick={() => onClick(true)} title="누르면 예약을 취소해요">✓ 맞구독 예약됨</button>
        <small>블로그를 만들면 자동으로 구독돼요</small>
      </span>
    )
  }
  return (
    <span className="notification-side reserve">
      <button className="subscribe primary" onClick={() => onClick(false)}>+ 맞구독 예약</button>
      <small>아직 블로그가 없어요</small>
    </span>
  )
}

// NOTI-02 알림 화면. 최신순, 더보기는 커서 방식
export default function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState(null)
  const [cursor, setCursor] = useState(null)
  const [busy, setBusy] = useState(false)
  const [gone, setGone] = useState({}) // 눌렀는데 볼 수 없는 글이었던 알림

  const load = useCallback(async (from) => {
    setBusy(true)
    try {
      const data = await api('GET', `/api/notifications${from ? `?cursor=${from}` : ''}`)
      setItems((prev) => (from ? [...prev, ...data.items] : data.items))
      setCursor(data.nextCursor)
    } finally {
      setBusy(false)
    }
  }, [])
  useEffect(() => { load(null) }, [load])

  const open = async (n) => {
    const r = await api('POST', `/api/notifications/${n.id}/read`)
    setItems((prev) => prev.map((x) => (x.id === n.id ? r : x)))
    changed()
    if (!r.available) {
      setGone((g) => ({ ...g, [n.id]: true }))
      return
    }
    if (!r.address) return
    const to = blogLink(r.address, r.slug ?? '')
    if (to.external) window.location.href = to.href
    else navigate(to.href)
  }

  // 구독 알림에서 바로 맞구독. 상대 블로그가 없으면 예약해 두고, 상대가 블로그를 만들 때 자동으로 구독된다 (SUB-07)
  const subscribeBack = async (n, cancel = false) => {
    if (cancel && !window.confirm('맞구독 예약을 취소할까요?')) return
    try {
      const r = await api(cancel ? 'DELETE' : 'POST', `/api/notifications/${n.id}/subscribe-back`)
      setItems((prev) => prev.map((x) => (x.id === n.id ? r : x)))
      changed()
    } catch (err) {
      window.alert(err.message)
    }
  }

  const readAll = async () => {
    await api('POST', '/api/notifications/read-all')
    setItems((prev) => prev.map((x) => ({ ...x, read: true })))
    changed()
  }

  if (!items) return null
  return (
    <div className="panel">
      <div className="notifications-head">
        <h1>알림</h1>
        {items.some((n) => !n.read) && <button onClick={readAll}>모두 읽음</button>}
      </div>
      {items.length === 0 ? (
        <p className="empty">아직 알림이 없어요. 누가 댓글을 달거나 구독하면 여기에 보여요.</p>
      ) : (
        <ul className="notification-list">
          {items.map((n) => (
            <li key={n.id} className={`notification-row${n.read ? '' : ' unread'}`}>
              <button className="notification" onClick={() => open(n)}>
                <span className="notification-icon" aria-hidden="true">{ICON[n.type]}</span>
                <span className="notification-main">
                  <span className="notification-text">{n.message}</span>
                  <time>{formatDateTime(n.createdAt)}</time>
                  {gone[n.id] && <span className="notification-gone">볼 수 없는 글이에요</span>}
                </span>
                {!n.read && <span className="unread-dot" aria-label="읽지 않음" />}
              </button>
              {n.type === 'SUBSCRIBE' && <SubscribeBack n={n} onClick={(cancel) => subscribeBack(n, cancel)} />}
            </li>
          ))}
        </ul>
      )}
      {cursor && <p className="center"><button onClick={() => load(cursor)} disabled={busy}>{busy ? '불러오는 중…' : '더보기'}</button></p>}
    </div>
  )
}
