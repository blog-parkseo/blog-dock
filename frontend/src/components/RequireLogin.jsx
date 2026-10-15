import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../AuthContext'

// 로그인이 필요한 화면. 비회원이면 로그인으로 보내고 지금 주소를 기억한다
export default function RequireLogin({ children }) {
  const { me, loading } = useAuth()
  const location = useLocation()
  if (loading) return null
  if (!me) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return children
}
