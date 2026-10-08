import { Navigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

// 글쓰기 화면은 2단계(POST-01)에서 만든다. 블로그가 없으면 개설로 보낸다 (AUTH-04)
export default function WritePage() {
  const { me } = useAuth()
  if (!me.blogAddress) return <Navigate to="/blog/new" replace />
  return <p>글쓰기 화면은 2단계에서 만들어요.</p>
}
