import { useCallback, useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { api } from '../api'
import { mainLink } from '../blogUrl'
import SmartLink from './SmartLink'

const POLL_MS = 60_000
// 알림 화면에서 읽음 처리하면 이 이벤트로 종 숫자를 바로 다시 센다
export const NOTIFICATIONS_CHANGED = 'notifications-changed'

// NOTI-02 위쪽 메뉴의 종. 읽지 않은 수를 60초마다 묻고, 다른 탭에 있으면 묻지 않는다
export default function NotificationBell() {
  const [count, setCount] = useState(0)
  const location = useLocation()

  const refresh = useCallback(() => {
    if (document.visibilityState !== 'visible') return
    api('GET', '/api/notifications/unread-count').then((r) => setCount(r.count)).catch(() => {})
  }, [])

  useEffect(() => {
    refresh()
    const timer = setInterval(refresh, POLL_MS)
    document.addEventListener('visibilitychange', refresh) // 탭으로 돌아오면 바로 한 번
    window.addEventListener(NOTIFICATIONS_CHANGED, refresh)
    return () => {
      clearInterval(timer)
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener(NOTIFICATIONS_CHANGED, refresh)
    }
  }, [refresh])
  useEffect(() => { refresh() }, [location.pathname, refresh])

  const label = count > 9 ? '9+' : String(count)
  return (
    <SmartLink to={mainLink('/notifications')} className="bell" aria-label={count ? `알림 ${count}개 읽지 않음` : '알림'}>
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
        <path d="M12 3a6 6 0 0 0-6 6v3.6L4.3 15.4A1 1 0 0 0 5.2 17h13.6a1 1 0 0 0 .9-1.6L18 12.6V9a6 6 0 0 0-6-6Z"
          fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
        <path d="M9.5 19.5a2.5 2.5 0 0 0 5 0" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
      {count > 0 && <span className="bell-count">{label}</span>}
    </SmartLink>
  )
}
