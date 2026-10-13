import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'
import { mainLink } from '../blogUrl'

// SUB-01 구독: 누르면 구독, 다시 누르면 해제. 비회원은 로그인으로, 내 블로그에서는 숨긴다
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
      className={`subscribe${blog.subscribed ? ' subscribed' : ' primary'} ${className}`.trim()}>
      {blog.subscribed ? '✓ 구독중' : '+ 구독'}
    </button>
  )
}
