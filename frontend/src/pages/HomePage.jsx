import { useAuth } from '../AuthContext'

export default function HomePage() {
  const { me } = useAuth()
  return (
    <div>
      <h1>블로그독</h1>
      <p>{me ? `${me.nickname}님, 반가워요.` : '로그인하고 내 블로그를 열어 보세요.'}</p>
      <p className="hint">홈 최신 글(HOME-01)은 3단계에서 만들어요.</p>
    </div>
  )
}
