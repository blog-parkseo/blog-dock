import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import { mainLink } from '../blogUrl'

// SUB-01 구독, SUB-05 맞구독. 비회원은 로그인으로, 내 블로그에서는 숨긴다
// 네 가지 상태: 서로 안 함 / 상대만 나를 구독 / 나만 구독 / 서로 구독(맞구독)
function label(blog) {
  if (blog.subscribed) return blog.subscribesMe ? '✓ 맞구독중' : '✓ 구독중'
  return blog.subscribesMe ? '+ 맞구독하기' : '+ 구독'
}

export default function SubscribeButton({ blog, address, onChange, className = '' }) {
  const { me } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [busy, setBusy] = useState(false)
  if (blog.isOwner) return null

  const click = async () => {
    if (!me) {
      const to = mainLink(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`)
      if (to.external) window.location.href = to.href
      else navigate(to.href)
      return
    }
    // 해제는 확인을 받는다. 내 구독만 취소되고 상대에게 알리지 않는다
    if (blog.subscribed && !window.confirm(`'${blog.name}' 구독을 해제할까요?`)) return
    setBusy(true)
    try {
      onChange(await api(blog.subscribed ? 'DELETE' : 'POST', `/api/blogs/${address}/subscription`))
    } catch (err) {
      window.alert(err.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <button onClick={click} disabled={busy} aria-pressed={blog.subscribed}
      className={`subscribe${blog.subscribed ? ' subscribed' : ' primary'}${blog.subscribed && blog.subscribesMe ? ' mutual' : ''} ${className}`.trim()}
      title={!blog.subscribed && blog.subscribesMe ? '이 블로그 주인이 내 블로그를 구독하고 있어요' : undefined}>
      {label(blog)}
    </button>
  )
}
